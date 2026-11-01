// ============================================================================
// RECIPIENTS MANAGEMENT VIEW SCRIPT
// ============================================================================

async function loadRecipientsView() {
    const tbody = document.getElementById('recipients-table-body');
    if (!tbody) return;

    try {
        const res = await fetch(`${API_BASE}/recipients`);
        if (!res.ok) throw new Error('Failed to load recipients');
        const recipients = await res.json();

        if (recipients.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">No recipients registered.</td></tr>';
            return;
        }

        tbody.innerHTML = recipients.map(r => {
            const isShelter = r.recipientType === 'Shelter';
            const typeBadge = isShelter
                ? `<span class="badge bg-primary-subtle text-primary border border-primary-subtle"><i class="bi bi-house-heart"></i> Shelter</span>`
                : `<span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle"><i class="bi bi-shop"></i> Community Centre</span>`;

            const categoryBadges = r.acceptedCategories.map(c => 
                `<span class="badge-category">${c}</span>`
            ).join(' ');

            return `
                <tr>
                    <td><strong>${r.recipientId}</strong></td>
                    <td>${r.name}</td>
                    <td>${typeBadge}</td>
                    <td><i class="bi bi-geo-alt text-success me-1"></i>${r.locationName} (${r.locationId})</td>
                    <td>${r.totalRequiredPortions}</td>
                    <td>
                        <span class="badge ${r.remainingDemandPortions > 0 ? 'bg-success' : 'bg-secondary'} fs-6">
                            ${r.remainingDemandPortions} portions
                        </span>
                    </td>
                    <td>${categoryBadges}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-success" onclick="promptUpdateDemand('${r.recipientId}', ${r.remainingDemandPortions})" title="Adjust Remaining Demand">
                            <i class="bi bi-pencil-square"></i> Update Demand
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-4">Failed to fetch recipients.</td></tr>';
    }
}

async function promptUpdateDemand(recipientId, currentDemand) {
    const val = prompt(`Enter new remaining demand portions for ${recipientId}:`, currentDemand);
    if (val === null) return;
    const newDemand = parseInt(val);
    if (isNaN(newDemand) || newDemand < 0) {
        showAlert('warning', 'Please enter a valid non-negative number.');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/recipients/${recipientId}/demand`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ remainingDemand: newDemand })
        });
        const data = await res.json();
        if (res.ok && data.success) {
            showAlert('success', `Demand for ${recipientId} updated to ${newDemand} portions.`);
            loadRecipientsView();
        } else {
            showAlert('danger', `Failed to update demand: ${data.message}`);
        }
    } catch (err) {
        showAlert('danger', 'Error updating demand.');
    }
}

document.getElementById('form-add-recipient').addEventListener('submit', async (e) => {
    e.preventDefault();
    const recipientId = document.getElementById('new-recipient-id').value.trim();
    const name = document.getElementById('new-recipient-name').value.trim();
    const recipientType = document.getElementById('new-recipient-type').value;
    const totalRequiredPortions = parseInt(document.getElementById('new-recipient-portions').value);
    const locationId = document.getElementById('new-recipient-location').value;

    try {
        const res = await fetch(`${API_BASE}/recipients`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ recipientId, name, recipientType, totalRequiredPortions, locationId })
        });
        const data = await res.json();
        if (res.ok && data.success) {
            showAlert('success', `Recipient ${recipientId} registered.`);
            const modalEl = document.getElementById('modal-add-recipient');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
            document.getElementById('form-add-recipient').reset();
            loadRecipientsView();
        } else {
            showAlert('danger', `Failed to create recipient: ${data.message}`);
        }
    } catch (err) {
        showAlert('danger', 'Error submitting recipient form.');
    }
});
