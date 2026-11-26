(function () {
    'use strict';

    var POLL_INTERVAL_MS = 3000;
    var endpoint = window.JOURNAL_ENDPOINT;
    var list = document.getElementById('journal-list');
    var search = document.getElementById('journal-search');
    var status = document.getElementById('journal-status');
    var lastRendered = null;

    if (!endpoint || !list || !search) {
        return;
    }

    function render(entries) {
        var signature = JSON.stringify(entries);
        if (signature === lastRendered) {
            return;
        }
        lastRendered = signature;
        list.textContent = '';
        entries.forEach(function (entry) {
            var item = document.createElement('li');
            var title = document.createElement('strong');
            title.textContent = entry.title;
            var description = document.createElement('p');
            description.textContent = entry.description;
            item.appendChild(title);
            item.appendChild(description);
            list.appendChild(item);
        });
        status.textContent = entries.length === 0
            ? 'Δεν βρέθηκαν ενδείξεις.'
            : entries.length + ' ένδειξη(εις).';
    }

    function load() {
        var url = endpoint + '?q=' + encodeURIComponent(search.value);
        fetch(url, { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(render)
            .catch(function () {
                status.textContent = 'Αδυναμία φόρτωσης του ημερολογίου.';
            });
    }

    search.addEventListener('input', load);
    load();
    window.setInterval(load, POLL_INTERVAL_MS);
}());
