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
    var exitStatusElement = document.getElementById('exit-status');
    var csrfToken = metaContent('_csrf');
    var csrfHeader = metaContent('_csrf_header');

    var state = {};
    var currentRoomId = null;
    var gameStatus = null;
    var exitUnlocked = false;
    var completedPuzzleIds = [];
    var collectedItems = [];
    var version = 0;
    var listeners = [];
    var source = null;

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
        if (exitStatusElement) {
            exitStatusElement.textContent = exitUnlocked
                ? 'Η έξοδος ξεκλειδώθηκε για όλη την ομάδα!'
                : 'Η έξοδος είναι κλειδωμένη. Λύστε το σεντούκι για να αποκτήσετε το κλειδί.';
        }
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

    function publishExitState() {
        window.dispatchEvent(new CustomEvent('escapehub:exit-state', {
            detail: {exitUnlocked: exitUnlocked}
        }));
        window.dispatchEvent(new CustomEvent('escapehub:progress'));
    }

    function onSnapshot(event) {
        var snapshot = JSON.parse(event.data);
        var snapshotVersion = Number(snapshot.version) || 0;
        if (snapshotVersion < version) {
            return; // a newer update arrived before this snapshot
        }
        state = Object.assign({}, snapshot.attributes || {});
        currentRoomId = snapshot.currentRoomId || null;
        gameStatus = snapshot.status || null;
        exitUnlocked = snapshot.exitUnlocked === true;
        completedPuzzleIds = Array.isArray(snapshot.completedPuzzleIds) ? snapshot.completedPuzzleIds : [];
        collectedItems = Array.isArray(snapshot.collectedItems) ? snapshot.collectedItems : [];
        version = snapshotVersion;
        render();
        publishExitState();
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
        } else if (update.key === 'game.exitUnlocked') {
            exitUnlocked = update.value === 'true';
        } else {
            state[update.key] = update.value;
            if (update.key.indexOf('progress.puzzles.') === 0 && update.value === 'true') {
                var puzzleId = update.key.substring('progress.puzzles.'.length);
                if (completedPuzzleIds.indexOf(puzzleId) === -1) {
                    completedPuzzleIds.push(puzzleId);
                }
            } else if (update.key.indexOf('progress.items.') === 0 && update.value === 'true') {
                var item = update.key.substring('progress.items.'.length);
                if (collectedItems.indexOf(item) === -1) {
                    collectedItems.push(item);
                }
            }
        }
        render();
        publishExitState();
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
                exitUnlocked: exitUnlocked,
                completedPuzzleIds: completedPuzzleIds.slice(),
                collectedItems: collectedItems.slice(),
                values: Object.assign({}, state)
            };
        }
    };

    connect();
})();
