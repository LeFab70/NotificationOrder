let stompClient = null;
const orders = new Map();
const subscribed = new Set();

function ensureSubscription(orderId) {
    if (!stompClient || subscribed.has(orderId)) return;
    subscribed.add(orderId);
    stompClient.subscribe('/topic/orders/' + orderId, (message) => {
        const history = JSON.parse(message.body);
        const order = orders.get(history.orderId);
        if (!order) return;
        order.status = history.status;
        renderRow(order);
    });
}

function rowHtml(order) {
    const next = NEXT_STATUS[order.status];
    const terminal = TERMINAL_STATUSES.has(order.status);
    return `
        <td>${order.customerId}</td>
        <td><span class="status-badge status-${order.status}">${statusLabel(order.status)}</span></td>
        <td>${formatDateTime(order.createdAt)}</td>
        <td class="actions">
            ${next ? `<button data-action="advance" data-order="${order.id}">Suivant : ${statusLabel(next)}</button>` : ''}
            ${!terminal ? `<button data-action="fail" data-order="${order.id}" class="danger">Marquer en echec</button>` : ''}
            <a href="/client.html?orderId=${order.id}" target="_blank">Lien client</a>
        </td>
    `;
}

function renderRow(order) {
    let row = document.getElementById('row-' + order.id);
    if (!row) {
        row = document.createElement('tr');
        row.id = 'row-' + order.id;
        document.getElementById('orders-body').appendChild(row);
    }
    row.innerHTML = rowHtml(order);
    ensureSubscription(order.id);
}

function loadOrders() {
    fetch('/api/orders')
        .then((response) => response.json())
        .then((list) => {
            list.forEach((order) => orders.set(order.id, order));
            orders.forEach(renderRow);
        });
}

function updateStatus(orderId, status) {
    fetch(`/api/orders/${orderId}/status`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status }),
    })
        .then(async (response) => {
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Erreur inconnue');
            }
            return response.json();
        })
        .then((history) => {
            const order = orders.get(orderId);
            if (order) {
                order.status = history.status;
                renderRow(order);
            }
        })
        .catch((err) => alert('Impossible de mettre a jour la commande : ' + err.message));
}

document.getElementById('orders-body').addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const orderId = button.dataset.order;
    if (button.dataset.action === 'advance') {
        updateStatus(orderId, NEXT_STATUS[orders.get(orderId).status]);
    } else if (button.dataset.action === 'fail') {
        updateStatus(orderId, 'FAILED');
    }
});

document.getElementById('create-order-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const input = document.getElementById('customer-id-input');
    const customerId = input.value.trim();
    if (!customerId) return;

    fetch('/api/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ customerId }),
    })
        .then(async (response) => {
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Erreur inconnue');
            }
            return response.json();
        })
        .then((order) => {
            orders.set(order.id, order);
            renderRow(order);
            input.value = '';
        })
        .catch((err) => alert('Impossible de creer la commande : ' + err.message));
});

document.addEventListener('DOMContentLoaded', () => {
    loadOrders();
    connectStomp({
        onConnect: (client) => {
            stompClient = client;
            orders.forEach((_, orderId) => ensureSubscription(orderId));
        },
    });
});
