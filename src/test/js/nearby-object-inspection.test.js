'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const componentPath = path.join(
    __dirname,
    '../../main/resources/static/js/nearby-object-inspection.js'
);
const componentSource = fs.readFileSync(componentPath, 'utf8');

function createHarness(fetchImplementation) {
    const displayedName = { textContent: '' };
    const displayedDescription = { textContent: '' };
    const status = { textContent: '' };
    const details = {
        hidden: true,
        querySelector(selector) {
            return selector === 'h3' ? displayedName : displayedDescription;
        }
    };
    const button = {
        dataset: { inspectObjectId: 'Παλιό Σημειωματάριο' },
        disabled: false
    };
    let clickHandler;
    let canvasClickHandler;
    const canvas = {
        width: 960,
        height: 640,
        addEventListener(eventName, handler) {
            assert.equal(eventName, 'click');
            canvasClickHandler = handler;
        },
        getBoundingClientRect() {
            return { left: 0, top: 0, width: 960, height: 640 };
        }
    };
    const card = {
        addEventListener(eventName, handler) {
            assert.equal(eventName, 'click');
            clickHandler = handler;
        },
        contains(element) {
            return element === button;
        },
        querySelector(selector) {
            if (selector === '.nearby-object-inspection__status') {
                return status;
            }
            if (selector === '.nearby-object-inspection__details') {
                return details;
            }
            if (selector === '[data-inspect-object-id]') {
                return button;
            }
            throw new Error('Unexpected selector: ' + selector);
        },
        querySelectorAll() {
            return [button];
        }
    };
    const sidebar = {
        querySelector() {
            return card;
        }
    };
    const window = {};
    const document = {
        querySelector() {
            return sidebar;
        },
        getElementById(id) {
            return id === 'room-canvas' ? canvas : null;
        }
    };

    vm.runInNewContext(componentSource, {
        document: document,
        fetch: fetchImplementation,
        window: window
    });

    return {
        button: button,
        click() {
            clickHandler({
                target: {
                    closest() {
                        return button;
                    }
                }
            });
        },
        clickCanvas(x, y) {
            canvasClickHandler({ clientX: x, clientY: y });
        },
        description: displayedDescription,
        details: details,
        name: displayedName,
        status: status
    };
}

test('selecting an object fetches and displays its server inspection result', async () => {
    let requestedUrl;
    const harness = createHarness(async (url) => {
        requestedUrl = url;
        return {
            ok: true,
            json: async () => ({
                name: 'Notebook',
                description: 'A weathered notebook.'
            })
        };
    });

    harness.click();
    await new Promise((resolve) => setImmediate(resolve));

    assert.equal(
        requestedUrl,
        '/api/room/objects/' + encodeURIComponent('Παλιό Σημειωματάριο') + '/inspection'
    );
    assert.equal(harness.name.textContent, 'Notebook');
    assert.equal(harness.description.textContent, 'A weathered notebook.');
    assert.equal(harness.details.hidden, false);
    assert.equal(harness.button.disabled, false);
});

test('clicking the notebook in the room canvas requests its inspection', async () => {
    let requestedUrl;
    const harness = createHarness(async (url) => {
        requestedUrl = url;
        return {
            ok: true,
            json: async () => ({
                name: 'Παλιό Σημειωματάριο',
                description: 'Η ένδειξη του σημειωματαρίου.'
            })
        };
    });

    harness.clickCanvas(293, 223);
    await new Promise((resolve) => setImmediate(resolve));

    assert.equal(
        requestedUrl,
        '/api/room/objects/' + encodeURIComponent('Παλιό Σημειωματάριο') + '/inspection'
    );
    assert.equal(harness.details.hidden, false);
});

test('clicking the nearby bench does not inspect the notebook', () => {
    let requestCount = 0;
    const harness = createHarness(async () => {
        requestCount += 1;
        return { ok: true, json: async () => ({}) };
    });

    harness.clickCanvas(260, 220);

    assert.equal(requestCount, 0);
    assert.equal(harness.details.hidden, true);
    assert.equal(harness.button.disabled, false);
});

test('clicking outside the notebook bounds does not inspect it', () => {
    let requestCount = 0;
    const harness = createHarness(async () => {
        requestCount += 1;
        return { ok: true, json: async () => ({}) };
    });

    harness.clickCanvas(320, 220);

    assert.equal(requestCount, 0);
    assert.equal(harness.details.hidden, true);
});

test('a rejected inspection shows an error without exposing details', async () => {
    const harness = createHarness(async () => ({ ok: false }));

    harness.click();
    await new Promise((resolve) => setImmediate(resolve));

    assert.equal(
        harness.status.textContent,
        'Το αντικείμενο δεν μπορεί να εξεταστεί αυτή τη στιγμή.'
    );
    assert.equal(harness.details.hidden, true);
    assert.equal(harness.button.disabled, false);
});
