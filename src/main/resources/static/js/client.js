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

function renderTimeline() {
    document.getElementById('timeline').innerHTML = entries.map((entry) => `
        <li>
            <span class="status-badge status-${entry.status}">${statusLabel(entry.status)}</span>
            <span class="timeline-time">${formatDateTime(entry.createdAt)}</span>
        </li>
    `).join('');
}

function updateCurrentStatus() {
    if (entries.length === 0) return;
    const last = entries[entries.length - 1];
    const badge = document.getElementById('current-status');
    badge.textContent = statusLabel(last.status);
    badge.className = 'status-badge status-' + last.status;
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
