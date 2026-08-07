const entries = [];
const seenIds = new Set();

function addEntry(entry) {
    if (seenIds.has(entry.id)) return;
    seenIds.add(entry.id);
    entries.push(entry);
    entries.sort((a, b) => a.createdAt.localeCompare(b.createdAt));
    renderTimeline();
    updateCurrentStatus();
}

function stepHtml(status, entry, state) {
    const symbol = state === 'done' ? '&check;' : state === 'failed' ? '&times;' : '';
    return `
        <li class="step ${state}">
            <span class="step-dot">${symbol}</span>
            <div class="step-body">
                <p class="step-label">${statusLabel(status)}</p>
                ${entry ? `<p class="step-time">${formatDateTime(entry.createdAt)}</p>` : ''}
            </div>
        </li>
    `;
}

// Reconstruit le parcours complet (etapes passees, en cours, a venir) a partir
// du dernier evenement connu pour chaque statut, plutot que d'afficher un simple
// journal brut - c'est ce qui donne l'effet "suivi de livraison".
function renderTimeline() {
    const byStatus = new Map(entries.map((entry) => [entry.status, entry]));
    const failed = byStatus.get('FAILED');
    const lastStatus = entries[entries.length - 1]?.status;

    let html = '';
    for (const status of STATUS_SEQUENCE) {
        const entry = byStatus.get(status);
        if (entry) {
            const state = status === lastStatus && !failed ? 'current' : 'done';
            html += stepHtml(status, entry, state);
        } else if (failed) {
            break; // une etape jamais atteinte a cause d'un echec n'est pas affichee
        } else {
            html += stepHtml(status, null, 'pending');
        }
    }
    if (failed) {
        html += stepHtml('FAILED', failed, 'failed');
    }

    document.getElementById('timeline').innerHTML = html;
}

function updateCurrentStatus() {
    if (entries.length === 0) return;
    const last = entries[entries.length - 1];
    const pill = document.getElementById('current-status');
    pill.textContent = statusLabel(last.status);
    pill.className = 'status-pill status-pill-lg status-' + last.status;
}

function showMissing(message) {
    document.getElementById('order-panel').classList.add('hidden');
    const missing = document.getElementById('order-missing');
    missing.textContent = message;
    missing.classList.remove('hidden');
}

document.addEventListener('DOMContentLoaded', () => {
    const orderId = new URLSearchParams(window.location.search).get('orderId');
    if (!orderId) {
        showMissing("Aucune commande specifiee. Ajoutez ?orderId=... a l'URL.");
        return;
    }

    document.getElementById('order-panel').classList.remove('hidden');
    document.getElementById('order-id').textContent = orderId;

    fetch(`/api/orders/${orderId}`)
        .then(async (response) => {
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Commande introuvable');
            }
            return response.json();
        })
        .then((order) => {
            document.getElementById('order-customer').textContent = order.customerId;
        })
        .catch((err) => showMissing(err.message));

    fetch(`/api/orders/${orderId}/history`)
        .then((response) => (response.ok ? response.json() : []))
        .then((history) => history.forEach(addEntry));

    connectStomp({
        onConnect: (client) => {
            client.subscribe('/topic/orders/' + orderId, (message) => {
                addEntry(JSON.parse(message.body));
            });
        },
    });
});
