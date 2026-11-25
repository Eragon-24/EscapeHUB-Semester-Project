'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');
const componentPath = path.join(__dirname, '../../main/resources/static/js/nearby-object-inspection.js');
const componentSource = fs.readFileSync(componentPath, 'utf8');

function createHarness(fetchImplementation, initialPosition) {
    const objectName = { textContent: '' };
    const objectDescription = { textContent: '' };
    const prompt = { hidden: true, textContent: '', className: '' };
    let keydownHandler, intervalHandler, closeHandler, dialogClickHandler;
    let playerPosition = initialPosition || { x: 293, y: 223 };
    const canvas = { insertAdjacentElement(position, element) {
        assert.equal(position, 'afterend'); assert.equal(element, prompt);
    }};
    const dialog = {
        open: false,
        addEventListener(name, handler) { assert.equal(name, 'click'); dialogClickHandler = handler; },
        showModal() { this.open = true; },
        close() { this.open = false; }
    };
    const closeButton = { addEventListener(name, handler) {
        assert.equal(name, 'click'); closeHandler = handler;
    }};
    const elements = {
        'room-canvas': canvas, 'object-inspection-dialog': dialog,
        'close-inspection-dialog': closeButton,
        'inspection-object-name': objectName,
        'inspection-object-description': objectDescription
    };
    const document = {
        getElementById(id) { return elements[id] || null; },
        createElement(tag) { assert.equal(tag, 'div'); return prompt; }
    };
    const window = {
        escapeHubGetPlayerPosition() { return playerPosition; },
        addEventListener(name, handler) { assert.equal(name, 'keydown'); keydownHandler = handler; },
        setInterval(handler, delay) { assert.equal(delay, 100); intervalHandler = handler; return 1; }
    };
    vm.runInNewContext(componentSource, { document, window, fetch: fetchImplementation, encodeURIComponent, Math });
    const instance = new window.NearbyObjectInspection();
    return {
        dialog, instance, objectName, objectDescription, prompt,
        keydown(code, target) {
            let prevented = false;
            keydownHandler({ code, target: target || null, preventDefault() { prevented = true; } });
            return prevented;
        },
        setPlayerPosition(position) { playerPosition = position; },
        updatePrompt() { intervalHandler(); },
        closeButtonClick() { closeHandler(); },
        dialogClick(target) { dialogClickHandler({ target }); }
    };
}

test('shows the interaction prompt when the player is near the notebook', () => {
    const h = createHarness(async () => { throw new Error('unexpected fetch'); });
    assert.equal(h.prompt.hidden, false);
    assert.equal(h.prompt.textContent, 'E — Εξέταση Σημειωματάριο');
});
test('hides the prompt when the player moves away', () => {
    const h = createHarness(async () => { throw new Error('unexpected fetch'); });
    h.setPlayerPosition({ x: 480, y: 490 }); h.updatePrompt();
    assert.equal(h.prompt.hidden, true);
});
test('pressing E near the notebook fetches and displays the inspection result', async () => {
    let url;
    const h = createHarness(async (requestedUrl) => {
        url = requestedUrl;
        return { ok: true, json: async () => ({ name: 'Notebook', description: 'A weathered notebook.' }) };
    });
    assert.equal(h.keydown('KeyE'), true);
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(url, '/api/room/objects/' + encodeURIComponent('Παλιό Σημειωματάριο') + '/inspection');
    assert.equal(h.dialog.open, true);
    assert.equal(h.objectName.textContent, 'Notebook');
    assert.equal(h.objectDescription.textContent, 'A weathered notebook.');
    assert.equal(h.prompt.hidden, true);
    assert.equal(h.instance.pending, false);
});
test('pressing E away from the notebook does not send a request', () => {
    let count = 0;
    const h = createHarness(async () => { count++; return { ok: true, json: async () => ({}) }; }, { x: 480, y: 490 });
    h.keydown('KeyE');
    assert.equal(count, 0); assert.equal(h.dialog.open, false);
});
test('pressing E while typing in an input does not inspect the notebook', () => {
    let count = 0;
    const h = createHarness(async () => { count++; return { ok: true, json: async () => ({}) }; });
    h.keydown('KeyE', { tagName: 'INPUT' });
    assert.equal(count, 0); assert.equal(h.dialog.open, false);
});
test('a rejected inspection displays an error in the modal', async () => {
    const h = createHarness(async () => ({ ok: false }));
    h.keydown('KeyE');
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(h.dialog.open, true);
    assert.equal(h.objectName.textContent, 'Δεν ήταν δυνατή η εξέταση');
    assert.equal(h.objectDescription.textContent, 'Δεν ήταν δυνατή η εξέταση αυτού του αντικειμένου.');
    assert.equal(h.instance.pending, false);
});
test('the close button closes the inspection dialog', () => {
    const h = createHarness(async () => { throw new Error('unexpected fetch'); });
    h.dialog.open = true; h.closeButtonClick();
    assert.equal(h.dialog.open, false);
});
test('clicking the dialog backdrop closes the inspection dialog', () => {
    const h = createHarness(async () => { throw new Error('unexpected fetch'); });
    h.dialog.open = true; h.dialogClick(h.dialog);
    assert.equal(h.dialog.open, false);
});
