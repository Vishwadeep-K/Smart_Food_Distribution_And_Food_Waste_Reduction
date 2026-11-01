// ============================================================================
// ALLOCATIONS & RESERVATIONS VIEW SCRIPT
// ============================================================================

async function loadAllocationsView() {
    const tbody = document.getElementById('active-allocations-table-body');
    if (!tbody) return;

    try {
        const res = await fetch(`${API_BASE}/allocations`);
        if (!res.ok) throw new Error('Failed to load allocations');
        const allocations = await res.json();

        if (allocations.length === 0) {
            tbody.innerHTML = '<tr><td colspan="10" class="text-center text-muted py-4">No active allocations recorded yet. Go to Smart Matching to reserve surplus food.</td></tr>';
            return;
        }

        tbody.innerHTML = allocations.map(a => {
            const isReserved = a.status === 'RESERVED';
            const isDelivered = a.status === 'DELIVERED';

            let badgeClass = 'badge-not-approved';
            if (isReserved) badgeClass = 'badge-reserved';
            if (isDelivered) badgeClass = 'badge-approved';

            const actionBtn = isReserved
                ? `<button class="btn btn-sm btn-success" onclick="confirmPhysicalDelivery('${a.allocationId}')"><i class="bi bi-check2-all"></i> Confirm Delivery</button>`
                : `<span class="badge bg-light text-muted border">Completed</span>`;

            return `
                <tr>
                    <td><strong>${a.allocationId}</strong></td>
                    <td><span class="badge bg-light text-dark border">${a.batchId}</span> (${a.category})</td>
                    <td>${a.recipientName}</td>
                    <td>${a.vehicleName}</td>
                    <td><span class="badge bg-success fs-6">${a.allocatedPortions}</span></td>
                    <td><small class="font-monospace">${a.route}</small></td>
                    <td>${a.expectedCompletionTime}</td>
                    <td><span class="text-danger fw-bold">${a.useByDeadline}</span></td>
                    <td><span class="badge badge-status ${badgeClass}">${a.status}</span></td>
                    <td>${actionBtn}</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="10" class="text-center text-danger py-4">Failed to fetch allocations.</td></tr>';
    }
}

async function confirmPhysicalDelivery(allocationId) {
    if (!confirm(`Confirm delivery for Allocation ${allocationId}? This will update delivered food portions and free the delivery vehicle.`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/allocations/${allocationId}/confirm-delivery`, {
            method: 'POST'
        });
        const data = await res.json();

        if (res.ok && data.success) {
            showAlert('success', `Allocation ${allocationId} marked as DELIVERED! Vehicle is now freed at destination.`);
            loadAllocationsView();
        } else {
            showAlert('danger', `Delivery Confirmation Failed: ${data.message || 'Error'}`);
        }
    } catch (err) {
        showAlert('danger', 'Failed to confirm delivery.');
    }
}
