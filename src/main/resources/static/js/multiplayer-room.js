(function () {
    'use strict';

    var canvas = document.getElementById('room-canvas');
    var context = canvas ? canvas.getContext('2d') : null;

    var objectList = document.getElementById('inspectable-objects');
    var inspectionStatus = document.getElementById('inspection-status');
    var inspectionDetails = document.getElementById('inspection-details');
    var inspectionName = document.getElementById('inspection-name');
    var inspectionDescription = document.getElementById('inspection-description');
    var pendingObjectId = null;

    if (objectList && inspectionStatus && inspectionDetails
            && inspectionName && inspectionDescription) {
        objectList.addEventListener('click', function (event) {
            var button = event.target.closest('.inspect-object-button');
            if (!button || !objectList.contains(button) || pendingObjectId !== null) {
                return;
            }

            var objectId = button.getAttribute('data-object-id');
            if (!objectId) {
                inspectionStatus.textContent = 'Το αντικείμενο δεν είναι διαθέσιμο για εξέταση.';
                return;
            }

            pendingObjectId = objectId;
            inspectionDetails.hidden = true;
            inspectionStatus.textContent = 'Έλεγχος δυνατότητας εξέτασης...';
            setInspectionButtonsDisabled(true);
            window.dispatchEvent(new CustomEvent('room:inspect-object', {
                detail: { objectId: objectId }
            }));
        });

        window.addEventListener('room:inspection-result', function (event) {
            var result = event.detail;
            if (!result || pendingObjectId === null
                    || String(result.objectId) !== pendingObjectId) {
                return;
            }

            pendingObjectId = null;
            setInspectionButtonsDisabled(false);
            inspectionDetails.hidden = true;

            if (result.success === true
                    && typeof result.name === 'string'
                    && typeof result.description === 'string') {
                inspectionName.textContent = result.name;
                inspectionDescription.textContent = result.description;
                inspectionDetails.hidden = false;
                inspectionStatus.textContent = 'Οι πληροφορίες του αντικειμένου εμφανίζονται παρακάτω.';
                return;
            }

            inspectionStatus.textContent = typeof result.message === 'string'
                ? result.message
                : 'Το αντικείμενο δεν μπορεί να εξεταστεί αυτή τη στιγμή.';
        });
    }

    function setInspectionButtonsDisabled(disabled) {
        if (!objectList) {
            return;
        }
        objectList.querySelectorAll('.inspect-object-button').forEach(function (button) {
            button.disabled = disabled;
        });
    }

    if (!context) {
        return;
    }

    function drawGrid() {
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
        drawGrid();

        context.strokeStyle = '#69777c';
        context.lineWidth = 3;
        context.strokeRect(75, 95, 810, 475);

        context.fillStyle = '#dce1dc';
        context.font = 'bold 17px sans-serif';
        context.fillText('ΕΓΚΑΤΑΛΕΛΕΙΜΜΕΝΟ ΕΡΓΑΣΤΗΡΙΟ', 95, 125);

        drawDesk();
        drawNotebook();
        drawChest();
        drawExit();
    }

    function drawDesk() {
        context.fillStyle = '#695038';
        context.fillRect(150, 180, 270, 110);
        context.strokeStyle = '#9b774f';
        context.lineWidth = 4;
        context.strokeRect(150, 180, 270, 110);

        context.fillStyle = '#eedebe';
        context.font = 'bold 14px sans-serif';
        context.fillText('ΠΑΓΚΟΣ', 245, 174);
    }

    function drawNotebook() {
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
    }

    function drawChest() {
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
        context.fillStyle = '#ebe6d6';
        context.font = '12px sans-serif';
        context.fillText('Σεντούκι', 718, 329);
    }

    function drawExit() {
        context.fillStyle = '#584136';
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
        context.fillText('ΕΞΟΔΟΣ', 458, 78);
    }

    drawRoom();
})();
