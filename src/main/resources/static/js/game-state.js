(function () {
    'use strict';

    var container = document.getElementById('game-state');
    var text = document.getElementById('game-state-text');
    if (!container || !text) {
        return;
    }
    var endpoint = container.getAttribute('data-endpoint');

    fetch(endpoint, { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
        .then(function (response) {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        })
        .then(function (state) {
            text.textContent = 'δωμάτιο ' + state.currentRoomId
                + ' (έκδοση ' + state.version + ')';
        })
        .catch(function () {
            text.textContent = 'δεν ήταν δυνατή η φόρτωση.';
        });
}());