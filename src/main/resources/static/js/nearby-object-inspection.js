(function () {
    'use strict';

    class NearbyObjectInspection {
        constructor(sidebar) {
            this.sidebar = sidebar;
            this.pendingObjectId = null;
            this.card = sidebar.querySelector('.nearby-object-inspection');
            if (!this.card) {
                return;
            }

            this.card.addEventListener('click', (event) => {
                const button = event.target.closest('[data-inspect-object-id]');
                if (button && this.card.contains(button)) {
                    this.requestInspection(button.dataset.inspectObjectId);
                }
            });
        }

        requestInspection(objectId) {
            if (!objectId || this.pendingObjectId !== null) {
                return;
            }

            this.pendingObjectId = objectId;
            this.hideDetails();
            this.setStatus('Έλεγχος δυνατότητας εξέτασης...');
            this.setButtonsDisabled(true);
            fetch('/api/room/objects/' + encodeURIComponent(objectId) + '/inspection')
                .then((response) => {
                    if (!response.ok) {
                        throw new Error('Το αντικείμενο δεν μπορεί να εξεταστεί αυτή τη στιγμή.');
                    }
                    return response.json();
                })
                .then((result) => this.renderResult(objectId, result))
                .catch((error) => this.renderFailure(objectId, error));
        }

        renderResult(objectId, result) {
            if (this.pendingObjectId !== objectId) {
                return;
            }

            this.pendingObjectId = null;
            this.setButtonsDisabled(false);
            this.hideDetails();

            if (result && typeof result.name === 'string'
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

            this.setStatus('Ο διακομιστής επέστρεψε μη έγκυρες πληροφορίες αντικειμένου.');
        }

        renderFailure(objectId, error) {
            if (this.pendingObjectId !== objectId) {
                return;
            }

            this.pendingObjectId = null;
            this.setButtonsDisabled(false);
            this.hideDetails();
            this.setStatus(error.message);
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
