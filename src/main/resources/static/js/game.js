(function () {
    'use strict';

    var canvas = document.getElementById('game-canvas');
    var context = canvas.getContext('2d');
    var connectionStatus = document.getElementById('connection-status');
    var gameStatus = document.getElementById('game-status');
    var cluePanel = document.getElementById('clue-panel');
    var playerCount = document.getElementById('player-count');
    var playerList = document.getElementById('player-list');
    var objectiveNotebook = document.getElementById('objective-notebook');
    var objectiveChest = document.getElementById('objective-chest');
    var objectiveExit = document.getElementById('objective-exit');

    var state = null;
    var pressedKeys = new Set();
    var interactionHeld = false;
    var socket = null;
    var moveTimer = null;
    var directions = {
        w: [0, -1], arrowup: [0, -1],
        s: [0, 1], arrowdown: [0, 1],
        a: [-1, 0], arrowleft: [-1, 0],
        d: [1, 0], arrowright: [1, 0]
    };

    function connect() {
        var protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        socket = new WebSocket(protocol + '//' + window.location.host + '/ws/game');

        socket.addEventListener('open', function () {
            connectionStatus.textContent = 'Συνδεδεμένος στον game server';
            connectionStatus.className = 'connection-status connected';
        });

        socket.addEventListener('message', function (event) {
            try {
                state = JSON.parse(event.data);
                render();
            } catch (error) {
                gameStatus.textContent = 'Λήφθηκε μη έγκυρη κατάσταση παιχνιδιού.';
            }
        });

        socket.addEventListener('close', function () {
            connectionStatus.textContent = 'Η σύνδεση διακόπηκε — επανασύνδεση…';
            connectionStatus.className = 'connection-status disconnected';
            window.setTimeout(connect, 2000);
        });

        socket.addEventListener('error', function () {
            connectionStatus.textContent = 'Σφάλμα σύνδεσης με τον server';
            connectionStatus.className = 'connection-status disconnected';
        });
    }

    function send(message) {
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(message);
        }
    }

    function movementTick() {
        if (!state || state.gameWon || pressedKeys.size === 0) {
            return;
        }

        var horizontal = 0;
        var vertical = 0;
        pressedKeys.forEach(function (key) {
            var direction = directions[key];
            if (direction) {
                horizontal += direction[0];
                vertical += direction[1];
            }
        });

        horizontal = Math.max(-1, Math.min(1, horizontal));
        vertical = Math.max(-1, Math.min(1, vertical));
        if (horizontal !== 0 || vertical !== 0) {
            send('MOVE|' + horizontal + '|' + vertical);
        }
    }

    function nearestTarget() {
        if (!state) {
            return null;
        }
        var player = state.players.find(function (item) {
            return item.id === state.you;
        });
        if (!player) {
            return null;
        }

        var targets = [
            { name: 'NOTEBOOK', x: 290, y: 230, label: 'Σημειωματάριο' },
            { name: 'CHEST', x: 750, y: 250, label: 'Σεντούκι' },
            { name: 'EXIT', x: 480, y: 117, label: 'Έξοδος' }
        ];
        var nearest = null;
        var nearestDistance = 95;
        targets.forEach(function (target) {
            var distance = Math.hypot(player.x - target.x, player.y - target.y);
            if (distance <= nearestDistance) {
                nearest = target;
                nearestDistance = distance;
            }
        });
        return nearest;
    }

    function interact() {
        if (!state || state.gameWon) {
            return;
        }
        var target = nearestTarget();
        if (target && target.name === 'CHEST'
                && state.notebookFound && !state.chestSolved) {
            var answer = window.prompt('Πληκτρολόγησε τον εξαψήφιο κωδικό του σεντουκιού:');
            if (answer !== null) {
                send('ANSWER|' + answer);
            }
            return;
        }
        send('INTERACT');
    }

    function handleKeyDown(event) {
        var key = event.key.toLowerCase();
        if (directions[key]) {
            event.preventDefault();
            pressedKeys.add(key);
        } else if (key === 'e' && !interactionHeld) {
            event.preventDefault();
            interactionHeld = true;
            interact();
        }
    }

    function handleKeyUp(event) {
        var key = event.key.toLowerCase();
        pressedKeys.delete(key);
        if (key === 'e') {
            interactionHeld = false;
        }
    }

    function bindTouchControls() {
        document.querySelectorAll('[data-direction]').forEach(function (button) {
            var direction = button.getAttribute('data-direction');
            var aliases = { up: 'arrowup', down: 'arrowdown', left: 'arrowleft', right: 'arrowright' };
            var key = aliases[direction];

            button.addEventListener('pointerdown', function (event) {
                event.preventDefault();
                pressedKeys.add(key);
                button.setPointerCapture(event.pointerId);
            });
            ['pointerup', 'pointercancel', 'lostpointercapture'].forEach(function (name) {
                button.addEventListener(name, function () {
                    pressedKeys.delete(key);
                });
            });
        });

        document.getElementById('interact-button').addEventListener('click', interact);
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
        context.strokeStyle = '#69777c';
        context.lineWidth = 3;
        context.strokeRect(75, 95, 810, 475);

        context.fillStyle = '#dce1dc';
        context.font = 'bold 17px sans-serif';
        context.fillText('ΕΓΚΑΤΑΛΕΛΕΙΜΜΕΝΟ ΕΡΓΑΣΤΗΡΙΟ', 95, 125);

        context.fillStyle = '#695038';
        context.fillRect(150, 180, 270, 110);
        context.strokeStyle = '#9b774f';
        context.lineWidth = 4;
        context.strokeRect(150, 180, 270, 110);
        context.fillStyle = '#eedebe';
        context.font = 'bold 14px sans-serif';
        context.fillText('ΠΑΓΚΟΣ', 245, 174);

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
        if (state && state.chestSolved) {
            context.fillStyle = '#ae7a44';
            context.fillRect(690, 174, 120, 28);
        }
        context.fillStyle = '#ebe6d6';
        context.font = '12px sans-serif';
        context.fillText('Σεντούκι', 718, 329);

        context.fillStyle = state && state.chestSolved ? '#487051' : '#584136';
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
        context.fillText(state && state.chestSolved ? 'ΕΞΟΔΟΣ - ΞΕΚΛΕΙΔΩΤΗ' : 'ΕΞΟΔΟΣ - ΚΛΕΙΔΩΜΕΝΗ', 405, 78);
    }

    function drawPlayer(player, index) {
        var palette = ['#48b2be', '#d28b55', '#9c83d4', '#75ad78',
            '#d36d83', '#78a9d6', '#d0bd65', '#8fb8a8'];
        var color = palette[index % palette.length];

        context.fillStyle = 'rgba(15, 18, 20, 0.55)';
        context.beginPath();
        context.ellipse(player.x, player.y + 10, 19, 8, 0, 0, Math.PI * 2);
        context.fill();
        context.fillStyle = color;
        context.beginPath();
        context.arc(player.x, player.y, 16, 0, Math.PI * 2);
        context.fill();
        context.strokeStyle = player.id === state.you ? '#fff3b0' : '#18353c';
        context.lineWidth = player.id === state.you ? 4 : 2;
        context.stroke();

        context.fillStyle = '#f4f0e8';
        context.font = 'bold 12px sans-serif';
        context.textAlign = 'center';
        context.fillText(player.name + (player.id === state.you ? ' (εσύ)' : ''), player.x, player.y - 24);
        context.textAlign = 'left';
    }

    function renderPlayers() {
        var players = state ? state.players : [];
        players.forEach(drawPlayer);

        playerCount.textContent = players.length + ' συνδεδεμένοι';
        playerList.replaceChildren();
        players.forEach(function (player) {
            var item = document.createElement('li');
            item.textContent = player.name + (player.id === state.you ? ' — εσύ' : '');
            playerList.appendChild(item);
        });
    }

    function renderObjectives() {
        objectiveNotebook.classList.toggle('completed', state.notebookFound);
        objectiveChest.classList.toggle('completed', state.chestSolved);
        objectiveExit.classList.toggle('completed', state.gameWon);

        cluePanel.hidden = !state.notebookFound;
        cluePanel.textContent = state.clueText;
    }

    function render() {
        if (!state) {
            return;
        }
        drawRoom();
        renderPlayers();
        renderObjectives();
        gameStatus.textContent = state.statusMessage;
    }

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    window.addEventListener('blur', function () {
        pressedKeys.clear();
        interactionHeld = false;
    });

    bindTouchControls();
    connect();
    moveTimer = window.setInterval(movementTick, 55);
    window.addEventListener('beforeunload', function () {
        if (moveTimer !== null) {
            window.clearInterval(moveTimer);
        }
        if (socket) {
            socket.close();
        }
    });
})();
