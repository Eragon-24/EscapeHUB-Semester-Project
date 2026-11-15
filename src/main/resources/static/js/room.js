(function () {
    'use strict';
    var button = document.getElementById('select-object-btn');
    var card = button && button.closest('.room-object-card');
    var panel = document.getElementById('clue-panel');
    if (!button || !panel) {
        return;
    }

    // US-002: selecting the object reveals its clue in place (no page change, no inventory).
    button.addEventListener('click', function () {
        var reveal = panel.hidden;
        panel.hidden = !reveal;
        button.setAttribute('aria-expanded', String(reveal));
        button.textContent = reveal ? 'Απόκρυψη ένδειξης' : 'Επιλογή αντικειμένου';
        card.classList.toggle('selected', reveal);
    });
})();
