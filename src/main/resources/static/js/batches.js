// ============================================================================
// FOOD BATCHES MANAGEMENT & APPROVALS VIEW SCRIPT
// ============================================================================

let allBatchesCache = [];
let pendingRejectBatchId = null;
let rejectModalInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    const modalEl = document.getElementById('modal-reject-batch');
    if (modalEl && window.bootstrap) {
        rejectModalInstance = new bootstrap.Modal(modalEl);
    }
});

// ----------------------------------------------------------------------------
// Dedicated Approvals View Loader
// ----------------------------------------------------------------------------
async function loadApprovalsView() {
    const tbody = document.getElementById('approvals-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span> Loading pending donations...</td></tr>';

    try {
        const res = await fetch(`${API_BASE}/batches/pending`);
        if (!res.ok) throw new Error('Failed to load pending batches');
        const pending = await res.json();

        // Also update sidebar badge
        const badge = document.getElementById('sidebar-pending-badge');
        if (badge) {
            badge.innerText = pending.length;
            badge.className = pending.length > 0 ? 'badge bg-danger rounded-pill' : 'badge bg-secondary rounded-pill';
        }

        if (pending.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-5 text-muted">
                        <i class="bi bi-check2-circle text-success" style="font-size: 2.2rem;"></i>
                        <p class="mt-2 mb-1 fw-bold text-dark">All Donations Verified</p>
                        <small>No food donations currently awaiting approval.</small>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = pending.map(b => {
            const submittedDate = b.createdAt ? b.createdAt.replace('T', ' ').substring(0, 16) : '--';

            return `
                <tr>
                    <td>
                        <strong class="font-monospace text-dark">${b.batchId}</strong>
                    </td>
                    <td>
                        <div class="fw-bold text-dark">${escapeHtml(b.donorName)}</div>
                        <small class="text-muted font-monospace">${b.donorId}</small>
                    </td>
                    <td>
                        <div class="fw-semibold">${escapeHtml(b.foodName || b.category)}</div>
                        <span class="badge-category">${b.category}</span>
                    </td>
                    <td class="text-center">
                        <span class="badge bg-success-subtle text-success fs-6 px-3 py-1 border border-success-subtle">
                            ${b.totalPortions}
                        </span>
                        <div class="text-muted" style="font-size: 0.72rem;">portions</div>
                    </td>
                    <td>
                        <div><span class="text-muted" style="font-size: 0.75rem;">Prep:</span> ${b.preparationTime || '--'}</div>
                        <div><span class="text-danger fw-bold" style="font-size: 0.75rem;">Use-By:</span> <strong>${b.declaredUseByTime}</strong></div>
                    </td>
                    <td>
                        <div class="fw-semibold">${escapeHtml(b.pickupLocationName)}</div>
                        <small class="text-muted font-monospace">${b.pickupLocationId}</small>
                    </td>
                    <td>
                        <span class="text-muted small">${submittedDate}</span>
                    </td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-success me-1" onclick="approveBatch('${b.batchId}')" title="Approve and queue for Smart Matching">
                            <i class="bi bi-check-lg"></i> Approve
                        </button>
                        <button class="btn btn-sm btn-outline-danger" onclick="openRejectModal('${b.batchId}')" title="Reject with reason">
                            <i class="bi bi-x-lg"></i> Reject
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-4">Failed to fetch pending approval queue.</td></tr>';
    }
}

// ----------------------------------------------------------------------------
// General Food Batches View Loader
// ----------------------------------------------------------------------------
async function loadBatchesView(priorityQueueMode = false) {
    const tbody = document.getElementById('batches-table-body');
    if (!tbody) return;

    try {
        const endpoint = priorityQueueMode ? `${API_BASE}/batches/queue` : `${API_BASE}/batches`;
        const res = await fetch(endpoint);
        if (!res.ok) throw new Error('Failed to load batches');
        allBatchesCache = await res.json();

        if (priorityQueueMode) {
            showAlert('info', 'Displaying approved batches strictly in Earliest-Use-By-First (PriorityQueue) order.');
        }

        renderBatchesTable(allBatchesCache);
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-danger py-4">Failed to fetch food batches.</td></tr>';
    }
}

function renderBatchesTable(batches) {
    const tbody = document.getElementById('batches-table-body');
    if (!tbody) return;

    if (batches.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-4">No matching food batches found.</td></tr>';
        return;
    }

    tbody.innerHTML = batches.map(b => {
        let apprBadge = 'badge-not-approved';
        if (b.approvalStatus === 'APPROVED') apprBadge = 'badge-approved';
        if (b.approvalStatus === 'REJECTED') apprBadge = 'badge-rejected';

        const expBadge = b.expired
            ? '<span class="badge badge-status badge-expired"><i class="bi bi-x-circle"></i> EXPIRED</span>'
            : '<span class="badge badge-status badge-approved"><i class="bi bi-check2"></i> Active</span>';

        let actionBtns = '';
        if (b.approvalStatus === 'NOT_APPROVED' && !b.expired) {
            actionBtns = `
                <button class="btn btn-sm btn-outline-success" onclick="approveBatch('${b.batchId}')" title="Approve for Matching">
                    <i class="bi bi-check-lg"></i> Approve
                </button>
                <button class="btn btn-sm btn-outline-danger ms-1" onclick="openRejectModal('${b.batchId}')" title="Reject Batch">
                    <i class="bi bi-x-lg"></i>
                </button>
            `;
        } else if (b.approvalStatus === 'APPROVED') {
            actionBtns = `<span class="badge bg-success-subtle text-success border border-success-subtle">Ready</span>`;
        } else if (b.approvalStatus === 'REJECTED') {
            actionBtns = `<span class="badge bg-danger-subtle text-danger border border-danger-subtle" title="${escapeHtml(b.rejectionReason || 'Rejected')}">Rejected</span>`;
        } else {
            actionBtns = `<span class="text-muted small">--</span>`;
        }

        const rejectionSnippet = (b.approvalStatus === 'REJECTED' && b.rejectionReason) 
            ? `<div class="text-danger small mt-1" style="font-size: 0.72rem;"><i class="bi bi-info-circle me-1"></i>${escapeHtml(b.rejectionReason)}</div>`
            : '';

        return `
            <tr class="${b.expired ? 'table-secondary opacity-75' : ''}">
                <td><strong>${b.batchId}</strong></td>
                <td>
                    <span class="badge-category">${b.category}</span>
                    <div class="fw-bold small text-dark mt-1">${escapeHtml(b.foodName || b.category)}</div>
                    ${rejectionSnippet}
                </td>
                <td>${escapeHtml(b.donorName)}</td>
                <td>
                    <span class="badge bg-success">${b.availablePortions} avail</span> / 
                    <span class="badge bg-primary">${b.reservedPortions} res</span> / 
                    <span class="badge bg-info">${b.deliveredPortions} del</span>
                </td>
                <td><strong class="${b.expired ? 'text-decoration-line-through text-muted' : 'text-danger'}">${b.declaredUseByTime}</strong></td>
                <td>${escapeHtml(b.pickupLocationName)}</td>
                <td><span class="badge badge-status ${apprBadge}">${b.approvalStatus}</span></td>
                <td>${expBadge}</td>
                <td>${actionBtns}</td>
            </tr>
        `;
    }).join('');
}

function filterBatches() {
    const cat = document.getElementById('batch-filter-category').value;
    const appr = document.getElementById('batch-filter-approval').value;
    const exp = document.getElementById('batch-filter-expiry').value;
    const search = document.getElementById('batch-search-input').value.toLowerCase();

    const filtered = allBatchesCache.filter(b => {
        if (cat !== 'ALL' && b.category !== cat) return false;
        if (appr !== 'ALL' && b.approvalStatus !== appr) return false;
        if (exp === 'ACTIVE' && b.expired) return false;
        if (exp === 'EXPIRED' && !b.expired) return false;
        if (search) {
            const matchesId = b.batchId.toLowerCase().includes(search);
            const matchesDonor = (b.donorName || '').toLowerCase().includes(search);
            const matchesCategory = b.category.toLowerCase().includes(search);
            const matchesFoodName = (b.foodName || '').toLowerCase().includes(search);
            if (!matchesId && !matchesDonor && !matchesCategory && !matchesFoodName) return false;
        }
        return true;
    });

    renderBatchesTable(filtered);
}

// ----------------------------------------------------------------------------
// Batch Actions: Approve & Reject with Reason
// ----------------------------------------------------------------------------
async function approveBatch(batchId) {
    try {
        const res = await fetch(`${API_BASE}/batches/${batchId}/approve`, { method: 'PUT' });
        const data = await res.json();
        if (res.ok && data.success) {
            showAlert('success', `Batch ${batchId} approved. Queued in Earliest-Use-By-First PriorityQueue.`);
            loadBatchesView();
            loadApprovalsView();
            if (typeof loadPendingApprovalsBadge === 'function') loadPendingApprovalsBadge();
        } else {
            showAlert('danger', `Approval Failed: ${data.message || 'Error'}`);
        }
    } catch (err) {
        showAlert('danger', 'Failed to approve batch.');
    }
}

function openRejectModal(batchId) {
    pendingRejectBatchId = batchId;
    const label = document.getElementById('reject-modal-batch-id');
    const input = document.getElementById('reject-reason-input');
    if (label) label.innerText = batchId;
    if (input) {
        input.value = '';
        input.classList.remove('is-invalid');
    }
    if (rejectModalInstance) {
        rejectModalInstance.show();
    }
}

async function confirmRejectBatchAction() {
    if (!pendingRejectBatchId) return;

    const input = document.getElementById('reject-reason-input');
    const reason = (input?.value || '').trim();

    if (!reason) {
        input.classList.add('is-invalid');
        return;
    }
    input.classList.remove('is-invalid');

    try {
        const res = await fetch(`${API_BASE}/batches/${pendingRejectBatchId}/reject`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reason })
        });
        const data = await res.json();
        if (res.ok && data.success) {
            showAlert('warning', `Batch ${pendingRejectBatchId} rejected. Reason recorded for donor view.`);
            if (rejectModalInstance) rejectModalInstance.hide();
            loadBatchesView();
            loadApprovalsView();
            if (typeof loadPendingApprovalsBadge === 'function') loadPendingApprovalsBadge();
        } else {
            showAlert('danger', `Failed to reject batch: ${data.message || 'Error'}`);
        }
    } catch (err) {
        showAlert('danger', 'Error rejecting batch.');
    } finally {
        pendingRejectBatchId = null;
    }
}

// ----------------------------------------------------------------------------
// Form Submission for Adding Batch
// ----------------------------------------------------------------------------
const addBatchForm = document.getElementById('form-add-batch');
if (addBatchForm) {
    addBatchForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const batchId = document.getElementById('new-batch-id').value.trim();
        const donorId = document.getElementById('new-batch-donor').value;
        const category = document.getElementById('new-batch-category').value;
        const totalPortions = parseInt(document.getElementById('new-batch-portions').value);
        const useByTime = document.getElementById('new-batch-useby').value;
        const pickupLocationId = document.getElementById('new-batch-location').value;
        const approvalStatus = document.getElementById('new-batch-approval').value;

        try {
            const res = await fetch(`${API_BASE}/batches`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ batchId, donorId, category, totalPortions, useByTime, pickupLocationId, approvalStatus })
            });
            const data = await res.json();
            if (res.ok && data.success) {
                showAlert('success', `Batch ${batchId} registered successfully.`);
                const modalEl = document.getElementById('modal-add-batch');
                const modal = bootstrap.Modal.getInstance(modalEl);
                if (modal) modal.hide();
                addBatchForm.reset();
                loadBatchesView();
                loadApprovalsView();
                if (typeof loadPendingApprovalsBadge === 'function') loadPendingApprovalsBadge();
            } else {
                showAlert('danger', `Failed to create batch: ${data.message}`);
            }
        } catch (err) {
            showAlert('danger', 'Error submitting batch form.');
        }
    });
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.innerText = text;
    return div.innerHTML;
}
