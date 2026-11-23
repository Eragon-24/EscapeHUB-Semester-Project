/*
 * US-015: browser side of the real-time shared game state.
 *
 * Opens one Server-Sent Events connection for the player's game session and keeps a local COPY of the state.
 * The server stays authoritative: send() only asks the server to change the state, and the local copy changes
 * only when the server publishes an update. Later gameplay stories use window.EscapeHubSharedState.
 */
(function () {
    'use strict';

    var card = document.getElementById('shared-state-card');
    if (!card) {
        return;
    }

    var sessionId = card.getAttribute('data-session-id');
    var statusElement = document.getElementById('shared-state-status');
    var listElement = document.getElementById('shared-state-list');
    var csrfToken = metaContent('_csrf');
    var csrfHeader = metaContent('_csrf_header');

    var state = {};
    var currentRoomId = null;
    var gameStatus = null;
    var version = 0;
    var listeners = [];
    var source = null;
    var objectMessage = document.getElementById('object-interaction-message');

    function renderItems() {
        var controls = document.querySelectorAll('.object-interaction');
        var inventoryList = document.getElementById('shared-inventory-list');
        Array.prototype.forEach.call(controls, function (control) {
            var itemId = control.getAttribute('data-object-id');
            var pickedUp = state['inventory.' + itemId] === 'true';
            control.querySelector('[data-object-action="pickup"]').disabled = pickedUp;
        });
        if (inventoryList) {
            inventoryList.textContent = '';
            Object.keys(state).filter(function (key) {
                return key.indexOf('inventory.') === 0 && state[key] === 'true';
            }).forEach(function (key) {
                var item = document.createElement('li');
                item.textContent = key.substring('inventory.'.length);
                inventoryList.appendChild(item);
            });
        }
    }

    function metaContent(name) {
        var meta = document.querySelector('meta[name="' + name + '"]');
        return meta ? meta.getAttribute('content') : null;
    }

    function setStatus(text) {
        if (statusElement) {
            statusElement.textContent = text;
        }
    }

    function render() {
        if (!listElement) {
            return;
        }
        listElement.textContent = '';
        Object.keys(state).forEach(function (key) {
            var item = document.createElement('li');
            item.textContent = key + ' = ' + state[key];
            listElement.appendChild(item);
        });
    }

    function notify(update) {
        listeners.forEach(function (listener) {
            listener(update, state);
        });
    }

    function onSnapshot(event) {
        var snapshot = JSON.parse(event.data);
        var snapshotVersion = Number(snapshot.version) || 0;
        if (snapshotVersion < version) {
            return; // a newer update arrived before this snapshot
        }
        state = Object.assign({}, snapshot.attributes || {});
        renderItems();
        currentRoomId = snapshot.currentRoomId || null;
        gameStatus = snapshot.status || null;
        version = snapshotVersion;
        render();
        setStatus('Συγχρονισμός: συνδεδεμένος (έκδοση ' + version + ')');
    }

    function onUpdate(event) {
        var update = JSON.parse(event.data);
        if (update.version <= version) {
            return; // already included in the snapshot or already applied
        }
        version = update.version;
        if (update.key === 'game.currentRoomId') {
            currentRoomId = update.value;
        } else if (update.key === 'game.status') {
            gameStatus = update.value;
        } else {
            state[update.key] = update.value;
            if (update.key.indexOf('inventory.') === 0) renderItems();
        }
        render();
        setStatus('Συγχρονισμός: συνδεδεμένος (έκδοση ' + version + ')');
        notify(update);
    }

    function connect() {
        if (!sessionId || typeof EventSource === 'undefined') {
            setStatus('Συγχρονισμός: μη διαθέσιμος');
            return;
        }
        source = new EventSource('/game-sessions/' + encodeURIComponent(sessionId) + '/events');
        source.addEventListener('snapshot', onSnapshot);
        source.addEventListener('update', onUpdate);
        source.onerror = function () {
            // The browser reconnects automatically and the server sends a fresh snapshot.
            setStatus('Συγχρονισμός: επανασύνδεση...');
        };
    }

    function send(key, value) {
        var headers = {'Content-Type': 'application/x-www-form-urlencoded'};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }
        return fetch('/game-sessions/' + encodeURIComponent(sessionId) + '/state', {
            method: 'POST',
            headers: headers,
            body: 'key=' + encodeURIComponent(key) + '&value=' + encodeURIComponent(value)
        }).then(function (response) {
            return response.json().then(function (body) {
                if (!response.ok) {
                    throw new Error(body.error || 'Η αλλαγή απορρίφθηκε.');
                }
                return body;
            });
        });
    }

    window.EscapeHubSharedState = {
        send: send,
        onUpdate: function (listener) {
            listeners.push(listener);
        },
        current: function () {
            return {
                version: version,
                currentRoomId: currentRoomId,
                status: gameStatus,
                values: Object.assign({}, state)
            };
        }
    };

    document.addEventListener('click', function (event) {
        var control = event.target.closest('.object-interaction');
        if (control) {
            var action = event.target.getAttribute('data-object-action');
            if (!action) return;
            sendObjectInteraction(control.getAttribute('data-object-id'), action)
                .catch(function (error) { if (objectMessage) objectMessage.textContent = error.message; });
        }
    });

    function sendObjectInteraction(objectId, action) {
        var headers = {'Content-Type': 'application/x-www-form-urlencoded'};
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
        return fetch('/game-sessions/' + encodeURIComponent(sessionId) + '/objects/'
            + encodeURIComponent(objectId) + '/interactions', {
            method: 'POST', headers: headers, body: 'action=' + encodeURIComponent(action)
        }).then(function (response) {
            return response.json().then(function (body) {
                if (!response.ok) throw new Error(body.error || 'Interaction rejected');
                if (objectMessage) objectMessage.textContent = 'Interaction accepted';
                return body;
            });
        });
    }

    connect();
})();
