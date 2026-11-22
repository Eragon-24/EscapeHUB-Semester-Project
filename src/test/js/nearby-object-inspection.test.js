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
        dataset: { inspectObjectId: 'notebook' },
        disabled: false
    };
    let clickHandler;
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

    assert.equal(requestedUrl, '/api/room/objects/notebook/inspection');
    assert.equal(harness.name.textContent, 'Notebook');
    assert.equal(harness.description.textContent, 'A weathered notebook.');
    assert.equal(harness.details.hidden, false);
    assert.equal(harness.button.disabled, false);
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
