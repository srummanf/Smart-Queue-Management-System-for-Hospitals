(function () {
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfParameter = document.querySelector('meta[name="_csrf_parameter"]').content;
    const container = document.getElementById('queue-container');

    function escapeHtml(value) {
        const div = document.createElement('div');
        div.textContent = value;
        return div.innerHTML;
    }

    function csrfInput() {
        return '<input type="hidden" name="' + escapeHtml(csrfParameter) + '" value="' + escapeHtml(csrfToken) + '">';
    }

    function renderCurrentlyServing(snapshot) {
        if (snapshot.currentlyServing) {
            const cs = snapshot.currentlyServing;
            return '<h2>Currently serving</h2>' +
                '<p>Token <strong>' + cs.tokenNumber + '</strong> — ' + escapeHtml(cs.patientName) +
                ' (' + cs.priority + ')</p>' +
                '<form method="post" action="/tokens/' + cs.tokenId + '/complete">' + csrfInput() +
                '<button type="submit">Complete consultation</button></form>';
        }
        let html = '<h2>Currently serving</h2><p>No patient currently being served.</p>';
        if (snapshot.waitingTokens.length > 0) {
            html += '<form method="post" action="/doctors/' + snapshot.doctorId + '/call-next">' + csrfInput() +
                '<button type="submit">Call next patient</button></form>';
        }
        return html;
    }

    function renderWaitingQueue(snapshot) {
        if (snapshot.waitingTokens.length === 0) {
            return '<h2>Waiting queue</h2><p>No patients waiting.</p>';
        }
        let rows = '';
        snapshot.waitingTokens.forEach(function (t) {
            rows += '<tr>' +
                '<td>' + t.position + '</td>' +
                '<td>' + t.tokenNumber + '</td>' +
                '<td>' + escapeHtml(t.patientName) + '</td>' +
                '<td>' + t.priority + '</td>' +
                '<td>' + t.estimatedWaitMinutes + ' min</td>' +
                '<td><form method="post" action="/tokens/' + t.tokenId + '/escalate">' + csrfInput() +
                '<select name="priority">' +
                ['NORMAL', 'PRIORITY', 'EMERGENCY'].map(function (p) {
                    return '<option value="' + p + '"' + (p === t.priority ? ' selected' : '') + '>' + p + '</option>';
                }).join('') +
                '</select><button type="submit">Set</button></form></td>' +
                '<td><form method="post" action="/tokens/' + t.tokenId + '/cancel">' + csrfInput() +
                '<button type="submit">Cancel</button></form></td>' +
                '</tr>';
        });
        return '<h2>Waiting queue</h2><table><thead><tr>' +
            '<th>Position</th><th>Token</th><th>Patient</th><th>Priority</th><th>Est. wait</th><th>Escalate</th><th>Cancel</th>' +
            '</tr></thead><tbody>' + rows + '</tbody></table>';
    }

    function render(snapshot) {
        container.innerHTML =
            '<div id="currently-serving">' + renderCurrentlyServing(snapshot) + '</div>' +
            renderWaitingQueue(snapshot);
    }

    const socket = new SockJS('/ws');
    const stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, function () {
        stompClient.subscribe('/topic/doctor/' + doctorId + '/queue', function (message) {
            render(JSON.parse(message.body));
        });
    });
})();
