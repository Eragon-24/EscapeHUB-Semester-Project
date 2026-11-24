(function () {
    'use strict';
    class NearbyObjectInspection {
        constructor() {
            this.canvas = document.getElementById('room-canvas');
            this.dialog = document.getElementById('object-inspection-dialog');
            this.closeButton = document.getElementById('close-inspection-dialog');
            this.objectName = document.getElementById('inspection-object-name');
            this.objectDescription = document.getElementById('inspection-object-description');
            this.pending = false;
            this.interactionDistance = 75;
            this.notebook = { x: 293, y: 223, id: 'Παλιό Σημειωματάριο', name: 'Σημειωματάριο' };
            if (!this.canvas || !this.dialog) return;
            this.createInteractionPrompt();
            this.closeButton.addEventListener('click', () => this.dialog.close());
            this.dialog.addEventListener('click', (event) => {
                if (event.target === this.dialog) this.dialog.close();
            });
            window.addEventListener('keydown', (event) => {
                if (event.target && (event.target.tagName === 'INPUT' ||
                    event.target.tagName === 'TEXTAREA')) return;
                if (event.code !== 'KeyE') return;
                event.preventDefault();
                if (this.isPlayerNearNotebook()) this.requestInspection(this.notebook.id);
            });
            this.updateInterval = window.setInterval(() => this.updateInteractionPrompt(), 100);
            this.updateInteractionPrompt();
        }
        createInteractionPrompt() {
            this.prompt = document.createElement('div');
            this.prompt.className = 'interaction-prompt';
            this.prompt.textContent = 'E — Εξέταση ' + this.notebook.name;
            this.prompt.hidden = true;
            this.canvas.insertAdjacentElement('afterend', this.prompt);
        }
        isPlayerNearNotebook() {
            if (typeof window.escapeHubGetPlayerPosition !== 'function') return false;
            const player = window.escapeHubGetPlayerPosition();
            const left = 272, right = 314, top = 207, bottom = 239;
            const closestX = Math.max(left, Math.min(player.x, right));
            const closestY = Math.max(top, Math.min(player.y, bottom));
            const dx = player.x - closestX, dy = player.y - closestY;
            return Math.sqrt(dx * dx + dy * dy) <= this.interactionDistance;
        }
        updateInteractionPrompt() {
            if (this.dialog.open || this.pending) {
                this.prompt.hidden = true;
                return;
            }
            this.prompt.hidden = !this.isPlayerNearNotebook();
        }
        requestInspection(objectId) {
            if (this.pending || this.dialog.open || !this.isPlayerNearNotebook()) return;
            this.pending = true;
            this.prompt.hidden = true;
            this.objectName.textContent = 'Εξέταση αντικειμένου';
            this.objectDescription.textContent = 'Έλεγχος...';
            this.dialog.showModal();
            fetch('/api/room/objects/' + encodeURIComponent(objectId) + '/inspection')
                .then((response) => {
                    if (!response.ok) throw new Error('Δεν ήταν δυνατή η εξέταση αυτού του αντικειμένου.');
                    return response.json();
                })
                .then((result) => {
                    if (typeof result.name !== 'string' || typeof result.description !== 'string')
                        throw new Error('Μη έγκυρη απάντηση από τον διακομιστή.');
                    this.objectName.textContent = result.name;
                    this.objectDescription.textContent = result.description;
                })
                .catch((error) => {
                    this.objectName.textContent = 'Δεν ήταν δυνατή η εξέταση';
                    this.objectDescription.textContent = error.message;
                })
                .finally(() => { this.pending = false; });
        }
    }
    window.NearbyObjectInspection = NearbyObjectInspection;
    // Browser bootstrapping is optional so the component can be loaded by Node tests.
    if (typeof document.addEventListener === 'function') {
        document.addEventListener('DOMContentLoaded', () => new NearbyObjectInspection());
    }
})();
