(function () {
    'use strict';

    class NearbyObjectInspection {
        constructor(sidebar) {
            this.sidebar = sidebar;
            this.pendingObjectId = null;
            this.card = this.createCard();
            this.sidebar.appendChild(this.card);

            this.card.addEventListener('click', (event) => {
                const button = event.target.closest('[data-inspect-object-id]');
                if (button && this.card.contains(button)) {
                    this.requestInspection(button.dataset.inspectObjectId);
                }
            });

            window.addEventListener('room:inspectable-objects', (event) => {
                this.renderObjects(event.detail && event.detail.objects);
            });

            window.addEventListener('room:inspection-result', (event) => {
                this.renderResult(event.detail);
            });

            this.renderObjects(this.readObjectElements());
        }

        createCard() {
            const card = document.createElement('section');
            card.className = 'room-card nearby-object-inspection';
            card.setAttribute('aria-labelledby', 'nearby-object-inspection-heading');
            card.innerHTML =
                '<h2 id="nearby-object-inspection-heading">Αντικείμενα προς εξέταση</h2>'
                + '<p class="muted-message">Επίλεξε ένα αντικείμενο κοντά στον χαρακτήρα σου.</p>'
                + '<ul class="nearby-object-inspection__objects"></ul>'
                + '<p class="nearby-object-inspection__status" role="status" aria-live="polite">'
                + 'Δεν υπάρχουν διαθέσιμες πληροφορίες επιλογής.</p>'
                + '<article class="nearby-object-inspection__details" aria-live="polite" hidden>'
                + '<h3></h3><p></p></article>';
            return card;
        }

        readObjectElements() {
            return Array.from(document.querySelectorAll(
                '[data-inspectable-object-id][data-inspectable-object-name]'
            )).map((element) => ({
                id: element.dataset.inspectableObjectId,
                name: element.dataset.inspectableObjectName
            }));
        }

        renderObjects(objects) {
            const list = this.card.querySelector('.nearby-object-inspection__objects');
            list.replaceChildren();

            if (!Array.isArray(objects) || objects.length === 0) {
                this.setStatus('Δεν υπάρχουν διαθέσιμα αντικείμενα προς εξέταση.');
                return;
            }

            objects.forEach((object) => {
                if (!object || typeof object.id !== 'string'
                        || typeof object.name !== 'string') {
                    return;
                }

                const item = document.createElement('li');
                const button = document.createElement('button');
                button.type = 'button';
                button.className = 'nearby-object-inspection__button';
                button.dataset.inspectObjectId = object.id;
                button.textContent = object.name;
                item.appendChild(button);
                list.appendChild(item);
            });

            if (list.children.length > 0) {
                this.setStatus('Διάλεξε ένα αντικείμενο για εξέταση.');
            }
        }

        requestInspection(objectId) {
            if (!objectId || this.pendingObjectId !== null) {
                return;
            }

            this.pendingObjectId = objectId;
            this.hideDetails();
            this.setStatus('Έλεγχος δυνατότητας εξέτασης...');
            this.setButtonsDisabled(true);

            window.dispatchEvent(new CustomEvent('room:inspect-object', {
                detail: { objectId: objectId }
            }));
        }

        renderResult(result) {
            if (!result || this.pendingObjectId === null
                    || String(result.objectId) !== this.pendingObjectId) {
                return;
            }

            this.pendingObjectId = null;
            this.setButtonsDisabled(false);
            this.hideDetails();

            if (result.success === true
                    && typeof result.name === 'string'
                    && typeof result.description === 'string') {
                const details = this.card.querySelector(
                    '.nearby-object-inspection__details'
                );
                details.querySelector('h3').textContent = result.name;
                details.querySelector('p').textContent = result.description;
                details.hidden = false;
                this.setStatus('Οι πληροφορίες του αντικειμένου εμφανίζονται παρακάτω.');
                return;
            }

            this.setStatus(typeof result.message === 'string'
                ? result.message
                : 'Το αντικείμενο δεν μπορεί να εξεταστεί αυτή τη στιγμή.');
        }

        setButtonsDisabled(disabled) {
            this.card.querySelectorAll('[data-inspect-object-id]').forEach((button) => {
                button.disabled = disabled;
            });
        }

        setStatus(message) {
            this.card.querySelector('.nearby-object-inspection__status').textContent = message;
        }

        hideDetails() {
            this.card.querySelector('.nearby-object-inspection__details').hidden = true;
        }
    }

    window.NearbyObjectInspection = NearbyObjectInspection;

    const sidebar = document.querySelector('.room-sidebar');
    if (sidebar) {
        new NearbyObjectInspection(sidebar);
    }
})();
