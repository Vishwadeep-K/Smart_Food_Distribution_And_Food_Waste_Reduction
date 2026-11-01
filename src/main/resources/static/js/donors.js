// ============================================================================
// DONORS MANAGEMENT VIEW SCRIPT
// ============================================================================

let allDonorsCache = [];
let donorHistoryModalInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    const modalEl = document.getElementById('modal-donor-history');
    if (modalEl && window.bootstrap) {
        donorHistoryModalInstance = new bootstrap.Modal(modalEl);
    }
});

async function loadDonorsView() {
    const tbody = document.getElementById('donors-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="9" class="text-center py-3 text-muted"><span class="spinner-border spinner-border-sm me-2"></span> Loading registered donors...</td></tr>';

    try {
        const res = await fetch(`${API_BASE}/donors`);
        if (!res.ok) throw new Error('Failed to load donors');
        allDonorsCache = await res.json();

        const badge = document.getElementById('donor-count-badge');
        if (badge) badge.innerText = `${allDonorsCache.length} Donors`;

        renderDonorsTable(allDonorsCache);
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-danger py-4">Failed to fetch donors list.</td></tr>';
    }
}

function renderDonorsTable(donors) {
    const tbody = document.getElementById('donors-table-body');
    if (!tbody) return;

    if (donors.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-4">No donors match your search.</td></tr>';
        return;
    }

    tbody.innerHTML = donors.map(d => {
        const donorTypeBadge = `<span class="badge bg-light text-dark border">${escapeHtml(d.donorType || 'Individual')}</span>`;
        const cityDisplay = d.city ? `<span class="text-muted"><i class="bi bi-geo me-1"></i>${escapeHtml(d.city)}</span>` : '--';

        return `
            <tr>
                <td><strong class="font-monospace">${d.donorId}</strong></td>
                <td>
                    <div class="fw-bold text-dark">${escapeHtml(d.name)}</div>
                </td>
                <td>${escapeHtml(d.organization || d.name)}</td>
                <td>${donorTypeBadge}</td>
                <td>${cityDisplay}</td>
                <td>
                    <div><i class="bi bi-telephone text-muted me-1"></i>${escapeHtml(d.phone || '--')}</div>
                </td>
                <td>
                    <div class="fw-semibold">${escapeHtml(d.locationName)}</div>
                    <small class="text-muted font-monospace">${d.locationId}</small>
                </td>
                <td>
                    <span class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1">
                        ${d.batchCount} batches
                    </span>
                </td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-primary me-1" onclick="viewDonorHistory('${d.donorId}')" title="View Donor Profile and Donation History">
                        <i class="bi bi-clock-history"></i> History
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteDonor('${d.donorId}')" title="Delete Donor">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterDonors() {
    const search = (document.getElementById('donor-search-input')?.value || '').toLowerCase().trim();
    const typeFilter = document.getElementById('donor-filter-type')?.value || 'ALL';

    const filtered = allDonorsCache.filter(d => {
        if (typeFilter !== 'ALL' && (d.donorType || 'Individual') !== typeFilter) return false;
        if (search) {
            const matchId = d.donorId.toLowerCase().includes(search);
            const matchName = (d.name || '').toLowerCase().includes(search);
            const matchOrg = (d.organization || '').toLowerCase().includes(search);
            const matchCity = (d.city || '').toLowerCase().includes(search);
            if (!matchId && !matchName && !matchOrg && !matchCity) return false;
        }
        return true;
    });

    const badge = document.getElementById('donor-count-badge');
    if (badge) badge.innerText = `${filtered.length} Donors`;

    renderDonorsTable(filtered);
}

// ----------------------------------------------------------------------------
// View Donor Donation History Modal
// ----------------------------------------------------------------------------
async function viewDonorHistory(donorId) {
    const modalTitle = document.getElementById('donor-history-modal-title');
    const modalBody = document.getElementById('donor-history-modal-body');
    if (!modalBody) return;

    modalTitle.innerText = `Donor Profile & Donation Records - ${donorId}`;
    modalBody.innerHTML = '<div class="text-center py-4"><span class="spinner-border spinner-border-sm me-2"></span> Loading donor history...</div>';

    if (donorHistoryModalInstance) {
        donorHistoryModalInstance.show();
    }

    try {
        const res = await fetch(`${API_BASE}/donors/${donorId}/donations`);
        if (!res.ok) throw new Error('Failed to load donor history');
        const data = await res.json();
        const donor = data.donor;
        const batches = data.donations || [];

        let batchesHtml = '';
        if (batches.length === 0) {
            batchesHtml = '<div class="alert alert-light border text-muted small text-center my-3">No food batches registered by this donor yet.</div>';
        } else {
            batchesHtml = `
                <div class="table-responsive mt-3">
                    <table class="table table-sm table-hover align-middle mb-0">
                        <thead class="table-light small">
                            <tr>
                                <th>Batch ID</th>
                                <th>Food Item & Category</th>
                                <th class="text-center">Portions</th>
                                <th>Use-By Time</th>
                                <th>Approval</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody class="small">
                            ${batches.map(b => {
                                let apprBadge = '<span class="badge bg-warning text-dark">Pending</span>';
                                if (b.approvalStatus === 'APPROVED') apprBadge = '<span class="badge bg-success">Approved</span>';
                                if (b.approvalStatus === 'REJECTED') apprBadge = '<span class="badge bg-danger">Rejected</span>';

                                let delBadge = `<span class="badge bg-light text-dark border">${b.deliveryStatus || 'Pending'}</span>`;
                                if (b.deliveryStatus === 'Delivered') delBadge = '<span class="badge bg-success">Delivered</span>';
                                if (b.deliveryStatus === 'Expired') delBadge = '<span class="badge bg-secondary">Expired</span>';

                                return `
                                    <tr>
                                        <td><strong class="font-monospace">${b.batchId}</strong></td>
                                        <td>
                                            <strong>${escapeHtml(b.foodName || b.category)}</strong>
                                            <span class="badge bg-secondary-subtle text-dark ms-1">${b.category}</span>
                                        </td>
                                        <td class="text-center">
                                            <strong>${b.totalPortions}</strong>
                                            <div class="text-muted" style="font-size: 0.72rem;">${b.availablePortions} avail / ${b.reservedPortions} res / ${b.deliveredPortions} del</div>
                                        </td>
                                        <td><strong class="text-danger">${b.declaredUseByTime}</strong></td>
                                        <td>${apprBadge}</td>
                                        <td>${delBadge}</td>
                                    </tr>
                                `;
                            }).join('')}
                        </tbody>
                    </table>
                </div>
            `;
        }

        modalBody.innerHTML = `
            <div class="row g-3 mb-3">
                <div class="col-md-6">
                    <div class="p-2 border rounded bg-light">
                        <small class="text-muted d-block">Donor Name / Organization</small>
                        <h6 class="fw-bold text-dark mb-0">${escapeHtml(donor.name)} (${escapeHtml(donor.organization || donor.name)})</h6>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="p-2 border rounded bg-light">
                        <small class="text-muted d-block">Donor Type</small>
                        <span class="badge bg-success-subtle text-success border border-success-subtle">${escapeHtml(donor.donorType || 'Individual')}</span>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="p-2 border rounded bg-light">
                        <small class="text-muted d-block">City / Locality</small>
                        <strong class="text-dark">${escapeHtml(donor.city || 'Central')}</strong>
                    </div>
                </div>
            </div>

            <div class="row g-3 mb-3">
                <div class="col-md-6">
                    <div class="p-2 border rounded bg-light small">
                        <span class="text-muted">Contact Phone:</span> <strong>${escapeHtml(donor.phone)}</strong>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="p-2 border rounded bg-light small">
                        <span class="text-muted">Pickup Zone:</span> <strong>${escapeHtml(donor.locationName)} (${donor.locationId})</strong>
                    </div>
                </div>
            </div>

            <h6 class="fw-bold text-dark mt-4 mb-2"><i class="bi bi-box-seam text-success me-2"></i>Donation History (${batches.length} Batches)</h6>
            ${batchesHtml}
        `;
    } catch (err) {
        modalBody.innerHTML = '<div class="alert alert-danger">Failed to load donor history.</div>';
    }
}

// ----------------------------------------------------------------------------
// Delete Donor Action
// ----------------------------------------------------------------------------
async function deleteDonor(donorId) {
    if (!confirm(`Are you sure you want to delete Donor ${donorId}? (Deletions are prevented if linked batches exist).`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/donors/${donorId}`, { method: 'DELETE' });
        const data = await res.json();
        if (res.ok && data.success) {
            showAlert('success', `Donor ${donorId} deleted.`);
            loadDonorsView();
        } else {
            showAlert('danger', `Cannot Delete: ${data.message || 'Integrity constraint violation'}`);
        }
    } catch (err) {
        showAlert('danger', 'Failed to delete donor.');
    }
}

// ----------------------------------------------------------------------------
// Add Donor Form Submission
// ----------------------------------------------------------------------------
const addDonorForm = document.getElementById('form-add-donor');
if (addDonorForm) {
    addDonorForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const donorId = document.getElementById('new-donor-id').value.trim();
        const name = document.getElementById('new-donor-name').value.trim();
        const organization = document.getElementById('new-donor-org').value.trim();
        const donorType = document.getElementById('new-donor-type').value;
        const city = document.getElementById('new-donor-city').value.trim();
        const phone = document.getElementById('new-donor-phone').value.trim();
        const locationId = document.getElementById('new-donor-location').value;

        try {
            const res = await fetch(`${API_BASE}/donors`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ donorId, name, organization, donorType, city, phone, locationId })
            });
            const data = await res.json();
            if (res.ok && data.success) {
                showAlert('success', `Donor ${donorId} added successfully.`);
                const modalEl = document.getElementById('modal-add-donor');
                const modal = bootstrap.Modal.getInstance(modalEl);
                if (modal) modal.hide();
                addDonorForm.reset();
                loadDonorsView();
                if (typeof populateDonorDropdowns === 'function') populateDonorDropdowns();
            } else {
                showAlert('danger', `Failed to create donor: ${data.message}`);
            }
        } catch (err) {
            showAlert('danger', 'Error submitting donor form.');
        }
    });
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.innerText = text;
    return div.innerHTML;
}
