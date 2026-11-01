// ============================================================================
// VEHICLES MANAGEMENT VIEW SCRIPT
// ============================================================================

async function loadVehiclesView() {
    const tbody = document.getElementById('vehicles-table-body');
    if (!tbody) return;

    try {
        const res = await fetch(`${API_BASE}/vehicles`);
        if (!res.ok) throw new Error('Failed to load vehicles');
        const vehicles = await res.json();

        if (vehicles.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No vehicles registered.</td></tr>';
            return;
        }

        tbody.innerHTML = vehicles.map(v => {
            const availBadge = v.isAvailable
                ? `<span class="badge badge-status badge-approved"><i class="bi bi-check-circle"></i> Available</span>`
                : `<span class="badge badge-status badge-not-approved"><i class="bi bi-clock"></i> In Transit / Busy</span>`;

            const toggleBtn = `
                <button class="btn btn-sm ${v.isAvailable ? 'btn-outline-warning' : 'btn-outline-success'}" 
                        onclick="toggleVehicleAvailability('${v.vehicleId}', ${!v.isAvailable})">
                    <i class="bi ${v.isAvailable ? 'bi-pause-circle' : 'bi-play-circle'}"></i> 
                    ${v.isAvailable ? 'Set Busy' : 'Set Available'}
                </button>
            `;

            return `
                <tr>
                    <td><strong>${v.vehicleId}</strong></td>
                    <td>${v.name}</td>
                    <td><i class="bi bi-geo-alt text-success me-1"></i>${v.currentLocationName} (${v.currentLocationId})</td>
                    <td><span class="badge bg-primary fs-6">${v.portionCapacity} portions</span></td>
                    <td>${availBadge}</td>
                    <td>${toggleBtn}</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger py-4">Failed to fetch vehicles.</td></tr>';
    }
}

async function toggleVehicleAvailability(vehicleId, newStatus) {
    try {
        const res = await fetch(`${API_BASE}/vehicles/${vehicleId}/availability`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ isAvailable: newStatus })
        });
        const data = await res.json();
        if (res.ok && data.success) {
            showAlert('success', `Vehicle ${vehicleId} marked as ${newStatus ? 'AVAILABLE' : 'BUSY'}.`);
            loadVehiclesView();
        } else {
            showAlert('danger', `Failed: ${data.message}`);
        }
    } catch (err) {
        showAlert('danger', 'Error updating vehicle availability.');
    }
}

document.getElementById('form-add-vehicle').addEventListener('submit', async (e) => {
    e.preventDefault();
    const vehicleId = document.getElementById('new-vehicle-id').value.trim();
    const name = document.getElementById('new-vehicle-name').value.trim();
    const portionCapacity = parseInt(document.getElementById('new-vehicle-capacity').value);
    const currentLocationId = document.getElementById('new-vehicle-location').value;

    try {
        const res = await fetch(`${API_BASE}/vehicles`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ vehicleId, name, portionCapacity, currentLocationId })
        });
        const data = await res.json();
        if (res.ok && data.success) {
            showAlert('success', `Vehicle ${vehicleId} registered.`);
            const modalEl = document.getElementById('modal-add-vehicle');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
            document.getElementById('form-add-vehicle').reset();
            loadVehiclesView();
        } else {
            showAlert('danger', `Failed to create vehicle: ${data.message}`);
        }
    } catch (err) {
        showAlert('danger', 'Error submitting vehicle form.');
    }
});
