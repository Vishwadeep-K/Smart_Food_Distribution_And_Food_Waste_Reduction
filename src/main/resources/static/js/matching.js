// ============================================================================
// SMART FOOD MATCHING VIEW SCRIPT
// ============================================================================

async function loadMatchingView() {
    await populateMatchingDropdowns();
}

async function populateMatchingDropdowns() {
    try {
        const [recipientsRes, vehiclesRes] = await Promise.all([
            fetch(`${API_BASE}/recipients`),
            fetch(`${API_BASE}/vehicles`)
        ]);

        if (recipientsRes.ok) {
            const recipients = await recipientsRes.json();
            const select = document.getElementById('match-recipient-select');
            select.innerHTML = '<option value="">-- Choose Recipient Organization --</option>' +
                recipients.map(r => `
                    <option value="${r.recipientId}">
                        ${r.name} (${r.recipientType}) | Rem Demand: ${r.remainingDemandPortions} | @ ${r.locationName}
                    </option>
                `).join('');
        }

        if (vehiclesRes.ok) {
            const vehicles = await vehiclesRes.json();
            const select = document.getElementById('match-vehicle-select');
            select.innerHTML = '<option value="">Auto-Assign Optimal Available Vehicle</option>' +
                vehicles.map(v => `
                    <option value="${v.vehicleId}">
                        ${v.name} [Cap: ${v.portionCapacity}] @ ${v.currentLocationName} (${v.isAvailable ? 'AVAILABLE' : 'BUSY'})
                    </option>
                `).join('');
        }
    } catch (err) {
        console.error('Failed to populate matching dropdowns:', err);
    }
}

async function runMatchingEvaluation() {
    const recipientId = document.getElementById('match-recipient-select').value;
    const category = document.getElementById('match-category-select').value;
    const vehicleId = document.getElementById('match-vehicle-select').value;

    if (!recipientId) {
        showAlert('warning', 'Please select a beneficiary recipient to evaluate surplus food matches.');
        return;
    }

    const tbody = document.getElementById('matching-table-body');
    tbody.innerHTML = '<tr><td colspan="12" class="text-center py-4"><div class="spinner-border text-success spinner-border-sm me-2"></div>Evaluating candidate batches against Dijkstra routes and deadlines...</td></tr>';

    try {
        let url = `${API_BASE}/matching/candidates?recipientId=${encodeURIComponent(recipientId)}`;
        if (category && category !== 'ALL') url += `&category=${encodeURIComponent(category)}`;
        if (vehicleId) url += `&vehicleId=${encodeURIComponent(vehicleId)}`;

        const res = await fetch(url);
        if (!res.ok) throw new Error('Evaluation request failed');
        const candidates = await res.json();

        document.getElementById('match-candidate-count').innerText = `${candidates.length} candidate batches evaluated`;

        if (candidates.length === 0) {
            tbody.innerHTML = '<tr><td colspan="12" class="text-center text-muted py-4">No eligible food batches currently available in PriorityQueue.</td></tr>';
            return;
        }

        tbody.innerHTML = candidates.map(c => {
            const isAccepted = c.status === 'ACCEPTED';
            const statusBadge = isAccepted
                ? `<span class="badge badge-status badge-approved"><i class="bi bi-check-circle"></i> ACCEPTED</span>`
                : `<span class="badge badge-status badge-rejected" title="${c.rejectionReason}"><i class="bi bi-x-circle"></i> REJECTED</span><br><small class="text-danger" style="font-size:0.75rem;">${c.rejectionReason}</small>`;

            const actionBtn = isAccepted
                ? `<button class="btn btn-sm btn-primary-green" onclick="executeReservation('${c.batchId}', '${c.recipientId}', '${c.vehicleId}')"><i class="bi bi-bookmark-plus"></i> Reserve</button>`
                : `<button class="btn btn-sm btn-outline-secondary" disabled>Ineligible</button>`;

            return `
                <tr class="${isAccepted ? 'table-success-subtle' : ''}">
                    <td><strong>${c.batchId}</strong></td>
                    <td><span class="badge-category">${c.category}</span></td>
                    <td>${c.donorName}<br><small class="text-muted">@ ${c.pickupLocation}</small></td>
                    <td>${c.availablePortions}</td>
                    <td><span class="badge ${isAccepted ? 'bg-success' : 'bg-secondary'} fs-6">${c.allocatedPortions}</span></td>
                    <td>${c.vehicleName}<br><small class="text-muted">Cap: ${c.vehicleCapacity}</small></td>
                    <td><small class="font-monospace">${c.route}</small></td>
                    <td>${c.travelTimeMinutes} mins</td>
                    <td><strong>${c.expectedCompletionTime}</strong></td>
                    <td><span class="text-danger fw-bold">${c.useByDeadline}</span></td>
                    <td>${statusBadge}</td>
                    <td>${actionBtn}</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="12" class="text-center text-danger py-4">Failed to run matching evaluation. Please check server connection.</td></tr>';
        showAlert('danger', 'Error executing matching algorithm.');
    }
}

async function executeReservation(batchId, recipientId, vehicleId) {
    if (!confirm(`Confirm atomic reservation of batch ${batchId} for recipient ${recipientId}?`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/allocations/reserve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ batchId, recipientId, vehicleId })
        });
        const data = await res.json();

        if (res.ok && data.success) {
            showAlert('success', `Success: Allocated ${data.allocatedPortions} portions under Allocation ID ${data.allocationId}! Remaining batch portions: ${data.remainingBatchPortions}.`);
            // Refresh matching table to update available portions
            runMatchingEvaluation();
            populateMatchingDropdowns();
        } else {
            showAlert('danger', `Reservation Rejected: ${data.message || 'Validation error'}`);
        }
    } catch (err) {
        showAlert('danger', 'Failed to communicate with reservation service.');
    }
}
