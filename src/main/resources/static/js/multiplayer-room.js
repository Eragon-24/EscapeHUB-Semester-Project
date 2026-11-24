(function () {
    'use strict';

    var canvas = document.getElementById('room-canvas');
    if (!canvas) {
        return;
    }

    var context = canvas.getContext('2d');
    if (!context) {
        return;
    }

    var storageKey = 'escapehub.activeSessionId';
    var sessionLeft = document.body.getAttribute('data-session-left') === 'true';
    var playerRadius = 18;
    var moveStep = 12;
    var localPlayer = { x: 480, y: 490 };

    window.escapeHubGetPlayerPosition = function () {
        return {
            x: localPlayer.x,
            y: localPlayer.y
        };
    };

    var otherPlayers = [];
    var pollIntervalMs = 300;
    var obstacles = [
        { minX: 150, minY: 180, maxX: 420, maxY: 290 },
        { minX: 680, minY: 195, maxX: 820, maxY: 305 },
        { minX: 430, minY: 92, maxX: 530, maxY: 142 }
    ];

    function resolveSessionId() {
        if (sessionLeft) {
            try {
                window.sessionStorage.removeItem(storageKey);
            } catch (e) {
                // Ignore storage restrictions
            }
            return null;
        }
        var fromAttr = canvas.getAttribute('data-session-id');
        var fromUrl = new URLSearchParams(window.location.search).get('sessionId');
        var resolved = fromAttr || fromUrl;

        if (resolved) {
            try {
                window.sessionStorage.setItem(storageKey, resolved);
            } catch (e) {
                // Ignore storage restrictions
            }
            if (!fromUrl && window.history && window.history.replaceState) {
                var url = new URL(window.location.href);
                url.searchParams.set('sessionId', resolved);
                window.history.replaceState(null, '', url.toString());
            }
            return resolved;
        }

        try {
            return window.sessionStorage.getItem(storageKey);
        } catch (e) {
            return null;
        }
    }

    var sessionId = resolveSessionId();

    function clearStoredSession() {
        sessionId = null;
        otherPlayers = [];
        try {
            window.sessionStorage.removeItem(storageKey);
        } catch (e) {
            // Ignore storage restrictions
        }
        render();
    }

    function getCsrfHeaders() {
        var headers = { 'Content-Type': 'application/json' };
        var csrfInput = document.querySelector('input[name="_csrf"]');
        if (csrfInput && csrfInput.value) {
            headers['X-CSRF-TOKEN'] = csrfInput.value;
        }
        return headers;
    }

    function drawGrid() {
        context.strokeStyle = '#3a4448';
        context.lineWidth = 1;

        for (var x = 95; x < 885; x += 48) {
            context.beginPath();
            context.moveTo(x, 145);
            context.lineTo(x, 570);
            context.stroke();
        }

        for (var y = 160; y < 570; y += 48) {
            context.beginPath();
            context.moveTo(75, y);
            context.lineTo(885, y);
            context.stroke();
        }
    }

    function drawRoom() {
        context.fillStyle = '#0c1017';
        context.fillRect(0, 0, 960, 640);

        context.fillStyle = '#161d26';
        context.beginPath();
        context.roundRect(35, 58, 890, 540, 18);
        context.fill();

        context.strokeStyle = '#5d6f7d';
        context.lineWidth = 5;
        context.stroke();

        context.fillStyle = '#30393e';
        context.fillRect(75, 95, 810, 475);
        drawGrid();

        context.strokeStyle = '#69777c';
        context.lineWidth = 3;
        context.strokeRect(75, 95, 810, 475);

        context.fillStyle = '#dce1dc';
        context.font = 'bold 17px sans-serif';
        context.fillText('ΕΓΚΑΤΑΛΕΛΕΙΜΜΕΝΟ ΕΡΓΑΣΤΗΡΙΟ', 95, 125);

        drawDesk();
        drawNotebook();
        drawChest();
        drawExit();
    }

    function drawDesk() {
        context.fillStyle = '#695038';
        context.fillRect(150, 180, 270, 110);
        context.strokeStyle = '#9b774f';
        context.lineWidth = 4;
        context.strokeRect(150, 180, 270, 110);

        context.fillStyle = '#eedebe';
        context.font = 'bold 14px sans-serif';
        context.fillText('ΠΑΓΚΟΣ', 245, 174);
    }

    function drawNotebook() {
        context.fillStyle = '#cfc6a4';
        context.fillRect(272, 207, 42, 32);
        context.strokeStyle = '#5f523c';
        context.lineWidth = 2;
        context.beginPath();
        context.moveTo(293, 209);
        context.lineTo(293, 237);
        context.stroke();

        context.fillStyle = '#ebe6d6';
        context.font = '12px sans-serif';
        context.fillText('Σημειωματάριο', 260, 265);
    }

    function drawChest() {
        context.fillStyle = '#704227';
        context.fillRect(680, 195, 140, 110);
        context.strokeStyle = '#ae7a44';
        context.lineWidth = 4;
        context.strokeRect(680, 195, 140, 110);

        context.strokeStyle = '#e2ba5d';
        context.lineWidth = 3;
        context.beginPath();
        context.moveTo(750, 198);
        context.lineTo(750, 302);
        context.stroke();

        context.fillStyle = '#e2ba5d';
        context.fillRect(738, 238, 24, 30);
        context.fillStyle = '#ebe6d6';
        context.font = '12px sans-serif';
        context.fillText('Σεντούκι', 718, 329);
    }

    function drawExit() {
        context.fillStyle = '#584136';
        context.fillRect(430, 92, 100, 50);
        context.strokeStyle = '#b1956a';
        context.lineWidth = 3;
        context.strokeRect(430, 92, 100, 50);

        context.fillStyle = '#e0be62';
        context.beginPath();
        context.arc(514, 118, 4, 0, Math.PI * 2);
        context.fill();

        context.fillStyle = '#ebe6d6';
        context.font = 'bold 12px sans-serif';
        context.fillText('ΕΞΟΔΟΣ', 458, 78);
    }

    function drawPlayerAvatar(x, y, label, fillColor, strokeColor) {
        context.fillStyle = 'rgba(15, 18, 20, 0.55)';
        context.beginPath();
        context.ellipse(x, y + 14, 18, 7, 0, 0, Math.PI * 2);
        context.fill();

        context.fillStyle = fillColor;
        context.beginPath();
        context.arc(x, y, playerRadius, 0, Math.PI * 2);
        context.fill();

        context.strokeStyle = strokeColor;
        context.lineWidth = 3;
        context.stroke();

        context.fillStyle = '#f4f7f5';
        context.font = 'bold 12px sans-serif';
        context.textAlign = 'center';
        context.fillText(label, x, y - 24);
        context.textAlign = 'start';
    }

    function render() {
        drawRoom();
        if (!sessionId) {
            return;
        }
        otherPlayers.forEach(function (player) {
            if (player && player.position) {
                drawPlayerAvatar(
                    player.position.x,
                    player.position.y,
                    player.username,
                    '#d97757',
                    '#f6c2a8'
                );
            }
        });
        drawPlayerAvatar(localPlayer.x, localPlayer.y, 'Εσύ', '#2a9d8f', '#a8e6cf');
    }

    function canMoveTo(x, y) {
        if (x < 75 + playerRadius || x > 885 - playerRadius
            || y < 95 + playerRadius || y > 570 - playerRadius) {
            return false;
        }
        return obstacles.every(function (obstacle) {
            return !(x + playerRadius > obstacle.minX
                && x - playerRadius < obstacle.maxX
                && y + playerRadius > obstacle.minY
                && y - playerRadius < obstacle.maxY);
        });
    }

    function pushLocalPosition() {
        if (!sessionId) {
            return;
        }
        fetch('/api/v1/game-sessions/' + encodeURIComponent(sessionId) + '/player-position', {
            method: 'PUT',
            headers: getCsrfHeaders(),
            body: JSON.stringify({ x: localPlayer.x, y: localPlayer.y })
        })
            .then(function (response) {
                if (response.status === 403 || response.status === 404 || response.status === 409) {
                    clearStoredSession();
                }
            })
            .catch(function () {
                // Ignore transient network errors during live updates
            });
    }

    function fetchOtherPlayers() {
        if (!sessionId) {
            return;
        }
        fetch('/api/v1/game-sessions/' + encodeURIComponent(sessionId) + '/other-players', {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        })
            .then(function (response) {
                if (response.status === 403 || response.status === 404 || response.status === 409) {
                    clearStoredSession();
                    return null;
                }
                if (!response.ok) {
                    return null;
                }
                return response.json();
            })
            .then(function (players) {
                if (Array.isArray(players)) {
                    otherPlayers = players;
                    render();
                }
            })
            .catch(function () {
                // Ignore transient polling errors
            });
    }

    if (sessionId) {
        window.addEventListener('keydown', function (event) {
            if (event.target && (event.target.tagName === 'INPUT'
                || event.target.tagName === 'TEXTAREA')) {
                return;
            }
            var dx = 0;
            var dy = 0;
            if (event.code === 'ArrowUp' || event.code === 'KeyW') {
                dy = -moveStep;
            } else if (event.code === 'ArrowDown' || event.code === 'KeyS') {
                dy = moveStep;
            } else if (event.code === 'ArrowLeft' || event.code === 'KeyA') {
                dx = -moveStep;
            } else if (event.code === 'ArrowRight' || event.code === 'KeyD') {
                dx = moveStep;
            } else {
                return;
            }
            event.preventDefault();
            var moved = false;
            if (dx !== 0 && canMoveTo(localPlayer.x + dx, localPlayer.y)) {
                localPlayer.x += dx;
                moved = true;
            }
            if (dy !== 0 && canMoveTo(localPlayer.x, localPlayer.y + dy)) {
                localPlayer.y += dy;
                moved = true;
            }
            if (moved) {
                render();
                pushLocalPosition();
            }
        });

        pushLocalPosition();
        fetchOtherPlayers();
        window.setInterval(fetchOtherPlayers, pollIntervalMs);
    }

    render();
})();