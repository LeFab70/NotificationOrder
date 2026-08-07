# notificationOrderLive

Demo de suivi de commande en temps reel : un back-office (`admin.html`) fait evoluer le
statut d'une commande, et le client qui suit cette commande (`client.html`) voit
l'evolution s'afficher **instantanement**, avec l'heure de chaque etape, sans recharger
la page. Le transport temps reel est du WebSocket (STOMP + SockJS), le backend est en
Spring Boot.

## Sommaire

- [Architecture generale](#architecture-generale)
- [Pourquoi separer WebSocket et REST](#pourquoi-separer-websocket-et-rest)
- [Modele de donnees](#modele-de-donnees)
- [Base de donnees H2](#base-de-donnees-h2)
- [Migrations Flyway](#migrations-flyway)
- [API REST](#api-rest)
- [WebSocket](#websocket)
- [Gestion des erreurs](#gestion-des-erreurs)
- [Frontend (admin.html / client.html)](#frontend-adminhtml--clienthtml)
- [Tests effectues](#tests-effectues)
- [Lancer le projet](#lancer-le-projet)
- [Structure du projet](#structure-du-projet)

## Architecture generale

```
Admin (admin.html)                          Client (client.html?orderId=...)
      |                                                 ^
      | 1. clic "Suivant"                               | 5. push temps reel
      | 2. POST /api/orders/{id}/status                 |    /topic/orders/{id}
      v                                                  |
              OrderController (REST)
                      |
                      v
              OrderService (transactionnel)
                 |            |
      3. ecrit en base   4. SimpMessagingTemplate.convertAndSend
       (Order + OrderStatusHistory)   (broadcast STOMP)
```

Le clic de l'admin ne parle jamais directement au client : il passe par une route REST
classique qui persiste le changement, puis c'est le serveur qui pousse l'evenement aux
abonnes du topic WebSocket concerne. C'est ce qui permet a n'importe quel nombre de
clients de suivre une commande sans que l'admin ait a savoir qui regarde.

## Pourquoi separer WebSocket et REST

Les **ecritures** (creer une commande, changer un statut) passent par des routes REST
classiques (`POST /api/orders`, `POST /api/orders/{id}/status`), **pas** par des
messages STOMP entrants (`@MessageMapping`). Le WebSocket ne sert que dans un seul sens,
serveur -> client, pour la diffusion.

Deux raisons a ce choix :

1. **Bande passante / cout de diffusion.** Chaque commande a son propre topic
   (`/topic/orders/{orderId}`), pas un topic global partage. Un client qui suit la
   commande A n'est jamais notifie des evenements de la commande B - le serveur
   n'envoie du trafic qu'aux clients reellement concernes, au lieu d'un broadcast
   global que chaque navigateur devrait filtrer lui-meme. A l'echelle, c'est la
   difference entre "pousser vers 3 abonnes concernes" et "pousser vers tous les
   clients connectes, tout le temps".
2. **Separation des responsabilites.** Une mutation a besoin de validation, de codes
   d'erreur HTTP normalises (404, 409, 400 - voir plus bas), et d'une reponse
   synchrone pour l'appelant. Rien de tout ca n'est naturel en STOMP entrant. Le
   WebSocket, lui, est **best-effort** : si la diffusion echoue (`OrderNotificationException`,
   voir plus bas), la donnee est deja persistee en base - le client la recuperera au
   prochain `GET /history`. Une commande qui a change de statut ne doit jamais echouer
   juste parce que la notification live a eu un probleme reseau.

## Modele de donnees

- **`Order`** : etat courant de la commande (`id`, `status`, `customerId`, `createdAt`).
- **`OrderStatusHistory`** : journal append-only, une ligne par changement de statut,
  avec son propre horodatage (`createdAt`). Relation `@ManyToOne` vers `Order`
  (unidirectionnelle - pas de `List<OrderStatusHistory>` sur `Order`, pour eviter un
  cycle `toString()`/`equals()` avec Lombok `@Data`).

C'est ce journal qui donne "l'evolution de l'heure de la commande" : chaque etape du
stepper cote client vient d'une ligne `OrderStatusHistory`, pas d'un simple champ
`status` ecrase a chaque fois.

## Base de donnees H2

H2 embarquee **en fichier local** (`./data/notificationOrderLivedb.mv.db`) : aucun
serveur de base de donnees a installer, tout tourne dans le process Java. A noter :
ce n'est pas de l'in-memory pur (`jdbc:h2:mem:...`) - les donnees survivent aux
redemarrages de l'appli, contrairement a une vraie base in-memory qui repartirait de
zero a chaque lancement. Pour un mode reellement in-memory (utile par exemple pour des
tests automatises isoles), il suffirait de changer l'URL JDBC dans
`application.properties`. La console H2 est activee sur `/h2-console` pour inspecter
les donnees pendant le dev.

## Migrations Flyway

- **`V1__create_orders_and_order_status_history.sql`** : cree les tables `orders` (le
  mot `order` etant reserve en SQL) et `order_status_history`, avec la FK et un index
  sur `order_id`.
- **`V2__seed_sample_orders.sql`** : insere 5 commandes de demo a differents stades
  (paiement en attente, en preparation, prete, livree, echouee), chacune avec son
  historique complet et des horodatages echelonnes (`DATEADD` relatif a
  `CURRENT_TIMESTAMP`, pour que la demo reste credible peu importe quand l'app demarre).

Ce sont des migrations Flyway **versionnees** : elles ne s'executent qu'une seule fois,
au premier demarrage suivant leur ajout - pas a chaque redemarrage.

## API REST

| Methode | Route | Description |
|---|---|---|
| `POST` | `/api/orders` | Cree une commande (`{ customerId }`). Statut initial et date fixes par le serveur. |
| `GET` | `/api/orders` | Liste toutes les commandes. |
| `GET` | `/api/orders/{id}` | Detail d'une commande. |
| `GET` | `/api/orders/{id}/history` | Historique complet, trie chronologiquement. |
| `POST` | `/api/orders/{id}/status` | Change le statut (`{ status }`) -> ecrit l'historique + diffuse en WebSocket. |

## WebSocket

- Endpoint de connexion : `/ws` (STOMP over SockJS, avec fallback pour les navigateurs
  sans WebSocket natif).
- Chaque commande a son propre topic : `/topic/orders/{orderId}`.
- Payload pousse a chaque changement de statut (identique au corps retourne par
  `POST /api/orders/{id}/status`) :

```json
{
  "id": "b1a2...",
  "orderId": "a0000000-0000-0000-0000-000000000002",
  "status": "PREPARING",
  "createdAt": "2026-08-07T03:22:35.5"
}
```

## Gestion des erreurs

Deux familles distinctes dans `exception/` :

- **Metier** (remontent au client REST via `GlobalExceptionHandler`,
  `@RestControllerAdvice`) : `OrderNotFoundException` -> 404,
  `InvalidOrderStatusTransitionException` -> 409 (commande deja dans un statut final),
  erreurs de validation `@Valid` -> 400. Reponse JSON uniforme
  `{ timestamp, status, error, message }`.
- **WebSocket** (ne remontent jamais au client REST, juste logguees) :
  `OrderNotificationException` (echec de `convertAndSend`),
  `WebSocketEventException` (echec dans les listeners connect/subscribe/disconnect).
  Une commande deja persistee ne doit jamais echouer a cause d'un probleme de
  diffusion live.

## Frontend (admin.html / client.html)

Deux pages HTML statiques, sans framework, connectees en STOMP/SockJS via
`sockjs-client` et `stomp.js` (CDN) :

- **`admin.html`** : grille de cartes, une par commande, avec bouton "Suivant" (avance
  dans le parcours normal) et "Marquer en echec". S'abonne au topic de **chaque**
  commande affichee, donc reflete aussi les changements faits ailleurs (un autre
  onglet admin, par exemple).
- **`client.html?orderId=...`** : charge l'historique existant au chargement (REST),
  puis s'abonne au topic de cette commande. Rendu en **stepper vertical** façon suivi
  de livraison : etapes passees cochees, etape en cours avec pastille pulsante, etapes
  futures grisees, cas dedie pour un echec.
- Theme clair/sombre avec bouton de bascule manuelle (persiste dans `localStorage`,
  l'emporte sur la preference systeme).

## Tests effectues

- **Postman** : creation de commande, lecture (liste / detail / historique), changement
  de statut (cas normal, transition sur commande deja terminale -> 409, commande
  inconnue -> 404, corps invalide -> 400).

  > 🎥 Video de demo (Loom) : https://www.loom.com/share/7753cbd241b74f07bc58aea075eed64b

- **Navigateur, avec le vrai WebSocket** (pas de mock) : `admin.html` et
  `client.html` ouverts simultanement dans deux onglets, clic sur "Suivant" cote admin
  et verification que le stepper cote client se met a jour tout seul, avec le bon
  horodatage, sans rechargement de page. Verifie aussi en theme clair et sombre, et sur
  le cas "commande echouee".

## Lancer le projet

```bash
./mvnw spring-boot:run
```

- Back-office : http://localhost:8080/admin.html
- Suivi client : http://localhost:8080/client.html?orderId=a0000000-0000-0000-0000-000000000001
- Console H2 : http://localhost:8080/h2-console (JDBC URL : `jdbc:h2:file:./data/notificationOrderLivedb`)

## Structure du projet

```
src/main/java/org/fab/notificationorderlive/
├── config/            WebSocketConfig, WebSocketEventListener
├── controller/         OrderController (REST)
├── dto/                 OrderDto, OrderCreateRequest, UpdateOrderStatusRequest, OrderStatusHistoryDto
├── entities/            Order, OrderStatusHistory
├── enums/               Status
├── exception/           exceptions metier + WebSocket, GlobalExceptionHandler, ErrorResponse
├── mapper/              OrderMapper (entite -> DTO)
├── repositories/        OrderRepository, OrderStatusHistoryRepository
└── service/             IOrderService (interface), OrderService (implementation)

src/main/resources/
├── db/migration/        V1 (schema), V2 (donnees de demo)
└── static/
    ├── admin.html / client.html
    ├── css/styles.css
    └── js/               status-labels.js, ws.js, theme.js, admin.js, client.js
```
