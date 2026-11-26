/*
 * US-022: submit an answer to a puzzle of the current game session.
 *
 * The browser only sends the answer. The server validates it and decides whether the puzzle is solved; the page
 * never marks a puzzle as solved on its own. The solved puzzles come from the real-time shared state (US-015):
 * both from the snapshot after a (re)load and from the updates caused by other players.
 */
(function () {
    'use strict';

    var card = document.getElementById('puzzle-card');
    var container = document.getElementById('puzzle-list');
    if (!card || !container) {
        return;
    }

    var sessionId = card.getAttribute('data-session-id');
    var csrfToken = metaContent('_csrf');
    var csrfHeader = metaContent('_csrf_header');
    var forms = {};

    function metaContent(name) {
        var meta = document.querySelector('meta[name="' + name + '"]');
        return meta ? meta.getAttribute('content') : null;
    }

    function base() {
        return '/game-sessions/' + encodeURIComponent(sessionId) + '/puzzles';
    }

    function markSolved(puzzleId) {
        var parts = forms[puzzleId];
        if (!parts) {
            return;
        }
        parts.input.disabled = true;
        parts.button.disabled = true;
        parts.form.classList.add('puzzle-solved');
    }

    function submit(puzzleId, answer, feedback) {
        var headers = {'Content-Type': 'application/x-www-form-urlencoded'};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }
        fetch(base() + '/' + encodeURIComponent(puzzleId) + '/answer', {
            method: 'POST',
            headers: headers,
            body: 'answer=' + encodeURIComponent(answer)
        }).then(function (response) {
            return response.json().then(function (body) {
                if (!response.ok) {
                    throw new Error(body.error || 'Η υποβολή απέτυχε.');
                }
                return body;
            });
        }).then(function (result) {
            feedback.textContent = result.message;
            if (result.status === 'ACCEPTED' || result.status === 'ALREADY_SOLVED') {
                markSolved(puzzleId);
            }
        }).catch(function (error) {
            feedback.textContent = error.message;
        });
    }

    function render(puzzle) {
        var form = document.createElement('form');
        var prompt = document.createElement('p');
        var input = document.createElement('input');
        var button = document.createElement('button');
        var feedback = document.createElement('p');

        prompt.textContent = puzzle.prompt;
        input.type = 'text';
        input.name = 'answer';
        input.required = true;
        input.setAttribute('aria-label', 'Απάντηση για ' + puzzle.id);
        button.type = 'submit';
        button.textContent = 'Υποβολή απάντησης';
        feedback.className = 'muted-message';
        feedback.setAttribute('role', 'status');

        form.appendChild(prompt);
        form.appendChild(input);
        form.appendChild(button);
        form.appendChild(feedback);
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            submit(puzzle.id, input.value, feedback);
        });
        container.appendChild(form);
        forms[puzzle.id] = {form: form, input: input, button: button};
    }

    fetch(base(), {headers: {'Accept': 'application/json'}})
        .then(function (response) {
            return response.ok ? response.json() : [];
        })
        .then(function (puzzles) {
            puzzles.forEach(render);
            applyProgress(); // the snapshot may have arrived before the forms existed
        });

    function applyProgress() {
        if (!window.EscapeHubSharedState) {
            return;
        }
        window.EscapeHubSharedState.current().completedPuzzleIds.forEach(markSolved);
    }

    window.addEventListener('escapehub:progress', applyProgress);
})();
