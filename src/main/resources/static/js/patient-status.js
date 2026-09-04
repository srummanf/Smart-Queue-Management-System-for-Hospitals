(function () {
    function setText(id, value) {
        document.getElementById(id).textContent = value;
    }

    function render(status) {
        setText('doctor-name', status.doctorName);
        setText('token-number', status.tokenNumber);
        setText('priority', status.priority);
        setText('status', status.status);

        const positionLine = document.getElementById('position-line');
        if (status.position != null) {
            setText('position', status.position);
            positionLine.style.display = '';
        } else {
            positionLine.style.display = 'none';
        }

        const etaLine = document.getElementById('eta-line');
        if (status.estimatedWaitMinutes != null) {
            setText('eta', status.estimatedWaitMinutes);
            etaLine.style.display = '';
        } else {
            etaLine.style.display = 'none';
        }

        document.getElementById('turn-near-banner').style.display = status.turnNear ? 'block' : 'none';
    }

    const socket = new SockJS('/ws');
    const stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, function () {
        stompClient.subscribe('/topic/patient/' + tokenId, function (message) {
            render(JSON.parse(message.body));
        });
    });
})();
