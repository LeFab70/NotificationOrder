const STATUS_LABELS = {
    PAYMENT_PENDING: 'Paiement en attente',
    PAYMENT_COMPLETED: 'Paiement confirme',
    PREPARING: 'En preparation',
    IN_PROGRESS: 'En cours',
    READY: 'Prete',
    DELIVERED: 'Livree',
    FAILED: 'Echouee',
};

const TERMINAL_STATUSES = new Set(['DELIVERED', 'FAILED']);

// Ordre d'avancement "normal" utilise par le bouton Suivant de l'admin.
// FAILED n'y figure pas : c'est une sortie d'urgence, pas une etape du parcours.
const NEXT_STATUS = {
    PAYMENT_PENDING: 'PAYMENT_COMPLETED',
    PAYMENT_COMPLETED: 'PREPARING',
    PREPARING: 'IN_PROGRESS',
    IN_PROGRESS: 'READY',
    READY: 'DELIVERED',
};

function statusLabel(status) {
    return STATUS_LABELS[status] || status;
}

// Les LocalDateTime Java arrivent avec jusqu'a 6 decimales (microsecondes) ;
// Date() de JS ne gere fiablement que 3 decimales (millisecondes).
function formatDateTime(isoString) {
    const truncated = isoString.replace(/(\.\d{3})\d*$/, '$1');
    const date = new Date(truncated);
    return date.toLocaleString('fr-FR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit', second: '2-digit',
    });
}
