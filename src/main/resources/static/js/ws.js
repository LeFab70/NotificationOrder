function connectStomp({ onConnect, onDisconnect }) {
    const statusEl = document.getElementById('connection-status');

    function setStatus(connected) {
        if (!statusEl) return;
        statusEl.textContent = connected ? 'Connecte' : 'Deconnecte - reconnexion...';
        statusEl.className = 'connection-status ' + (connected ? 'connected' : 'disconnected');
    }

    function open() {
        const socket = new SockJS('/ws');
        const stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, () => {
            setStatus(true);
            onConnect(stompClient);
        }, () => {
            setStatus(false);
            if (onDisconnect) onDisconnect();
            setTimeout(open, 3000);
        });
    }

    open();
}
