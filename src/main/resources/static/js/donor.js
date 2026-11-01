// ============================================================================
// ANNAM DONOR PORTAL CONTROLLER
// ============================================================================

let currentDonorProfile = null;
let allDonorDonationsCache = [];
let donationDetailsModalInstance = null;

document.addEventListener('DOMContentLoaded', async () => {
    const authenticated = await verifyDonorAuth();
    if (authenticated) {
        await loadLocationsDropdowns();
        await loadDonorProfile();
        await loadDonorDonations();
        initFormHandlers();
        initModalInstance();
        handlePeriodPresetChange();
    }
});

function initModalInstance() {
    const modalEl = document.getElementById('modal-donation-details');
    if (modalEl && window.bootstrap) {
        donationDetailsModalInstance = new bootstrap.Modal(modalEl);
    }
}

function animateDonorCounter(elementId, targetValue, duration = 650) {
    const el = document.getElementById(elementId);
    if (!el) return;

    const target = parseInt(targetValue) || 0;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || target === 0) {
        el.innerText = target.toLocaleString();
        return;
    }

    const currentText = el.innerText.replace(/,/g, '');
    const start = parseInt(currentText) || 0;
    if (start === target) return;

    const startTime = performance.now();

    function update(now) {
        const elapsed = now - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const ease = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);
        const current = Math.round(start + (target - start) * ease);
        el.innerText = current.toLocaleString();

        if (progress < 1) {
            requestAnimationFrame(update);
        } else {
            el.innerText = target.toLocaleString();
        }
    }

    requestAnimationFrame(update);
}

// ----------------------------------------------------------------------------
// Authentication Verification
// ----------------------------------------------------------------------------
async function verifyDonorAuth() {
    try {
        const res = await fetch('/api/auth/me');
        if (!res.ok) {
            window.location.href = '/login.html?error=session_expired';
            return false;
        }

        const data = await res.json();
        if (!data.authenticated || !data.user) {
            window.location.href = '/login.html?error=session_expired';
            return false;
        }

        if (data.user.role === 'ADMIN') {
            showDonorAlert('info', 'Viewing Donor Portal with Administrator privileges.');
        }

        document.getElementById('donor-topbar-name').innerText = data.user.fullName || 'Donor';
        document.getElementById('donor-topbar-email').innerText = data.user.email || '';
        return true;
    } catch (err) {
        window.location.href = '/login.html?error=session_expired';
        return false;
    }
}

// ----------------------------------------------------------------------------
// Load Locations Dropdowns
// ----------------------------------------------------------------------------
async function loadLocationsDropdowns() {
    try {
        const res = await fetch('/api/routing/locations');
        if (!res.ok) return;
        const locations = await res.json();

        const submitSelect = document.getElementById('don-pickup-loc');
        const profileSelect = document.getElementById('profile-location');

        const optionsHtml = '<option value="" disabled selected>-- Select Pickup Zone --</option>' +
            locations.map(l => `<option value="${l.locationId}">${l.locationId} - ${l.name} (${l.zoneType})</option>`).join('');

        if (submitSelect) submitSelect.innerHTML = optionsHtml;
        if (profileSelect) profileSelect.innerHTML = optionsHtml;
    } catch (err) {
        console.error('Failed to load locations dropdowns:', err);
    }
}

// ----------------------------------------------------------------------------
// Load Donor Profile & Key 5 Dashboard Metrics
// ----------------------------------------------------------------------------
async function loadDonorProfile() {
    try {
        const res = await fetch('/api/donor/profile');
        if (!res.ok) {
            if (res.status === 401) {
                window.location.href = '/login.html?error=session_expired';
            }
            return;
        }

        const profile = await res.json();
        currentDonorProfile = profile;

        // Topbar
        const orgOrName = profile.organization || profile.name || 'Donor';
        document.getElementById('donor-topbar-name').innerText = orgOrName;
        document.getElementById('donor-topbar-email').innerText = profile.email || '';

        // Summary 5 Metrics (Smooth Counter Animations)
        animateDonorCounter('stat-total-batches', profile.totalDonationsCount || 0);
        animateDonorCounter('stat-total-portions', profile.totalPortionsDonated || 0);
        animateDonorCounter('stat-pending-batches', profile.pendingApprovalCount || 0);
        animateDonorCounter('stat-delivered-batches', profile.deliveredDonationsCount || 0);
        animateDonorCounter('stat-delivered-portions', profile.deliveredPortionsCount || 0);
        animateDonorCounter('stat-expired-rejected-batches', profile.expiredOrRejectedCount || 0);

        // Populate Profile Form
        document.getElementById('profile-donor-id-badge').innerText = `Donor ID: ${profile.donorId}`;
        document.getElementById('profile-name').value = profile.name || '';
        document.getElementById('profile-email').value = profile.email || '';
        document.getElementById('profile-phone').value = profile.phone || '';
        document.getElementById('profile-address').value = profile.pickupAddress || '';

        const donorTypeEl = document.getElementById('profile-donor-type');
        if (donorTypeEl) donorTypeEl.value = profile.donorType || 'Individual';

        const cityEl = document.getElementById('profile-city');
        if (cityEl) cityEl.value = profile.city || 'Central';

        if (profile.locationId) {
            const locSelect = document.getElementById('profile-location');
            if (locSelect) locSelect.value = profile.locationId;
            const submitLocSelect = document.getElementById('don-pickup-loc');
            if (submitLocSelect) submitLocSelect.value = profile.locationId;
        }
    } catch (err) {
        console.error('Error loading donor profile:', err);
    }
}

// ----------------------------------------------------------------------------
// Load Donations History
// ----------------------------------------------------------------------------
async function loadDonorDonations() {
    const tbody = document.getElementById('donor-donations-tbody');
    if (!tbody) return;

    try {
        const res = await fetch('/api/donor/donations');
        if (!res.ok) throw new Error('Failed to load donations');
        allDonorDonationsCache = await res.json();
        renderDonorDonationsTable(allDonorDonationsCache);
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-4">Failed to fetch donation records.</td></tr>';
    }
}

function renderDonorDonationsTable(donations) {
    const tbody = document.getElementById('donor-donations-tbody');
    if (!tbody) return;

    if (!donations || donations.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center py-5 text-muted">
                    <i class="bi bi-box2-heart text-success" style="font-size: 2.2rem;"></i>
                    <p class="mt-2 mb-1 fw-semibold">No food donations match the filter criteria</p>
                    <small>Submit a surplus food batch or adjust your filters above.</small>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = donations.map(d => {
        // Approval Badge
        let apprBadge = '';
        if (d.approvalStatus === 'APPROVED') {
            apprBadge = '<span class="badge-status-pill badge-approved-state"><i class="bi bi-check2-circle"></i> Approved</span>';
        } else if (d.approvalStatus === 'REJECTED') {
            apprBadge = '<span class="badge-status-pill badge-rejected-state"><i class="bi bi-x-circle"></i> Rejected</span>';
        } else {
            apprBadge = '<span class="badge-status-pill badge-pending"><i class="bi bi-clock"></i> Pending Approval</span>';
        }

        // Delivery Status Badge
        let delBadge = '';
        switch (d.deliveryStatus) {
            case 'Pending Approval':
                delBadge = '<span class="badge-status-pill badge-pending"><i class="bi bi-hourglass"></i> Pending Approval</span>';
                break;
            case 'Approved':
                delBadge = '<span class="badge-status-pill badge-approved-state"><i class="bi bi-check2"></i> Approved</span>';
                break;
            case 'Partially Allocated':
                delBadge = '<span class="badge-status-pill badge-reserved-state" style="background:#e0e7ff; color:#3730a3; border-color:#c7d2fe;"><i class="bi bi-pie-chart"></i> Partially Allocated</span>';
                break;
            case 'Reserved':
                delBadge = '<span class="badge-status-pill badge-reserved-state"><i class="bi bi-truck"></i> Reserved</span>';
                break;
            case 'Delivered':
                delBadge = '<span class="badge-status-pill badge-delivered-state"><i class="bi bi-gift-fill"></i> Delivered</span>';
                break;
            case 'Rejected':
                delBadge = '<span class="badge-status-pill badge-rejected-state"><i class="bi bi-x-octagon"></i> Rejected</span>';
                break;
            case 'Expired':
                delBadge = '<span class="badge-status-pill badge-expired-state"><i class="bi bi-x-circle"></i> Expired</span>';
                break;
            default:
                delBadge = `<span class="badge bg-secondary">${escapeHtml(d.deliveryStatus)}</span>`;
        }

        const rejectionNote = (d.approvalStatus === 'REJECTED' && d.rejectionReason) 
            ? `<div class="text-danger small mt-1"><i class="bi bi-info-circle me-1"></i>Reason: ${escapeHtml(d.rejectionReason)}</div>` 
            : '';

        const submittedDateFormatted = d.createdAt ? d.createdAt.split('T')[0] : '--';

        return `
            <tr class="${d.expired ? 'table-secondary opacity-75' : ''}">
                <td>
                    <span class="badge bg-dark-subtle text-dark border font-monospace">${d.batchId}</span>
                </td>
                <td>
                    <div class="fw-bold text-dark">${escapeHtml(d.foodName || d.categoryDisplayName || d.category)}</div>
                    <span class="badge-category">${d.category}</span>
                    ${rejectionNote}
                </td>
                <td>
                    <div><strong>${d.totalPortions}</strong> portions</div>
                    <div style="font-size: 0.75rem;">
                        <span class="text-success fw-bold">${d.availablePortions} avail</span> / 
                        <span class="text-primary fw-semibold">${d.reservedPortions} res</span> / 
                        <span class="text-info fw-semibold">${d.deliveredPortions} del</span>
                    </div>
                </td>
                <td>
                    <div><span class="text-danger fw-bold" style="font-size: 0.78rem;">Use-By:</span> <strong>${d.declaredUseByTime}</strong></div>
                    <div class="text-muted" style="font-size: 0.72rem;">Zone: ${d.pickupLocationId}</div>
                </td>
                <td>
                    <span class="text-muted">${submittedDateFormatted}</span>
                </td>
                <td>${apprBadge}</td>
                <td>${delBadge}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-success" onclick="openDonationDetails('${d.batchId}')" title="View Portions & Delivery Details">
                        <i class="bi bi-eye"></i> Details
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

// ----------------------------------------------------------------------------
// View Detailed Breakdown Modal (IDOR Protected API)
// ----------------------------------------------------------------------------
async function openDonationDetails(batchId) {
    const modalTitle = document.getElementById('modal-detail-title');
    const modalBody = document.getElementById('modal-detail-body');
    if (!modalBody) return;

    modalTitle.innerText = `Donation Details - Batch ${batchId}`;
    modalBody.innerHTML = '<div class="text-center py-4"><span class="spinner-border spinner-border-sm me-2"></span> Loading batch & delivery breakdown...</div>';

    if (donationDetailsModalInstance) {
        donationDetailsModalInstance.show();
    }

    try {
        const res = await fetch(`/api/donor/donations/${batchId}`);
        if (!res.ok) {
            if (res.status === 403 || res.status === 401) {
                modalBody.innerHTML = '<div class="alert alert-danger">Access Denied: You cannot view donations belonging to other accounts.</div>';
            } else {
                modalBody.innerHTML = '<div class="alert alert-danger">Failed to load donation details.</div>';
            }
            return;
        }

        const d = await res.json();

        let rejectionBanner = '';
        if (d.approvalStatus === 'REJECTED') {
            rejectionBanner = `
                <div class="alert alert-danger border d-flex align-items-start gap-2 mb-3">
                    <i class="bi bi-x-octagon-fill fs-5 flex-shrink-0"></i>
                    <div>
                        <strong>Donation Rejected by Administrator</strong>
                        <div class="mt-1">${escapeHtml(d.rejectionReason || 'No specific reason provided by administrator.')}</div>
                    </div>
                </div>
            `;
        }

        let allocationsHtml = '';
        if (d.allocations && d.allocations.length > 0) {
            allocationsHtml = `
                <h6 class="fw-bold text-dark mt-4 mb-2"><i class="bi bi-truck text-primary me-2"></i>Matched Recipient & Vehicle Deliveries</h6>
                <div class="table-responsive">
                    <table class="table table-sm table-bordered align-middle mb-0">
                        <thead class="table-light small">
                            <tr>
                                <th>Alloc ID</th>
                                <th>Recipient Organization</th>
                                <th>Delivery Zone</th>
                                <th>Vehicle</th>
                                <th class="text-center">Quantity</th>
                                <th>Delivery Status</th>
                            </tr>
                        </thead>
                        <tbody class="small">
                            ${d.allocations.map(a => `
                                <tr>
                                    <td><strong class="font-monospace">${a.allocationId}</strong></td>
                                    <td>${escapeHtml(a.recipientName)} <span class="text-muted">(${a.recipientType})</span></td>
                                    <td>${escapeHtml(a.recipientLocationName)} <span class="text-muted font-monospace">(${a.recipientLocationId})</span></td>
                                    <td><i class="bi bi-truck text-secondary me-1"></i>${escapeHtml(a.vehicleName)}</td>
                                    <td class="text-center fw-bold text-success">${a.quantity}</td>
                                    <td>
                                        <span class="badge ${a.status === 'DELIVERED' ? 'bg-success' : 'bg-primary'}">${a.status}</span>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            `;
        } else {
            allocationsHtml = `
                <div class="p-3 bg-light rounded border text-muted small mt-4 text-center">
                    <i class="bi bi-info-circle me-1"></i> No portions have been allocated to recipients yet. Once approved, available portions are matched automatically.
                </div>
            `;
        }

        modalBody.innerHTML = `
            ${rejectionBanner}
            <div class="row g-3 mb-3">
                <div class="col-md-6">
                    <div class="p-2 border rounded bg-light">
                        <small class="text-muted d-block">Food Item & Category</small>
                        <strong class="text-dark">${escapeHtml(d.foodName || d.categoryDisplayName || d.category)}</strong> 
                        <span class="badge bg-secondary ms-1">${d.category}</span>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="p-2 border rounded bg-light">
                        <small class="text-muted d-block">Declared Use-By</small>
                        <strong class="text-danger">${d.declaredUseByTime}</strong>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="p-2 border rounded bg-light">
                        <small class="text-muted d-block">Pickup Zone</small>
                        <strong>${escapeHtml(d.pickupLocationName)}</strong> (${d.pickupLocationId})
                    </div>
                </div>
            </div>

            <div class="row g-2 text-center mb-3">
                <div class="col-3">
                    <div class="border rounded p-2 bg-white">
                        <small class="text-muted d-block">Total Donated</small>
                        <h4 class="fw-bold text-dark mb-0">${d.totalPortions}</h4>
                    </div>
                </div>
                <div class="col-3">
                    <div class="border rounded p-2 bg-white">
                        <small class="text-muted d-block">Remaining Avail</small>
                        <h4 class="fw-bold text-success mb-0">${d.availablePortions}</h4>
                    </div>
                </div>
                <div class="col-3">
                    <div class="border rounded p-2 bg-white">
                        <small class="text-muted d-block">Reserved / In-Transit</small>
                        <h4 class="fw-bold text-primary mb-0">${d.reservedPortions}</h4>
                    </div>
                </div>
                <div class="col-3">
                    <div class="border rounded p-2 bg-white">
                        <small class="text-muted d-block">Delivered Portions</small>
                        <h4 class="fw-bold text-info mb-0">${d.deliveredPortions}</h4>
                    </div>
                </div>
            </div>

            ${d.notes ? `
                <div class="p-2 border rounded bg-light small mb-3">
                    <span class="fw-bold text-muted">Notes:</span> ${escapeHtml(d.notes)}
                </div>
            ` : ''}

            ${allocationsHtml}
        `;
    } catch (err) {
        modalBody.innerHTML = '<div class="alert alert-danger">Error retrieving batch details.</div>';
    }
}

// ----------------------------------------------------------------------------
// Filter & Search
// ----------------------------------------------------------------------------
function filterDonorDonations() {
    const search = (document.getElementById('search-donations')?.value || '').toLowerCase().trim();
    const statusFilter = document.getElementById('filter-delivery-status')?.value || 'ALL';
    const catFilter = document.getElementById('filter-category')?.value || 'ALL';
    const dateFilter = document.getElementById('filter-submission-date')?.value || '';

    const filtered = allDonorDonationsCache.filter(d => {
        if (statusFilter !== 'ALL' && d.deliveryStatus !== statusFilter) return false;
        if (catFilter !== 'ALL' && d.category !== catFilter) return false;
        if (dateFilter) {
            const itemDate = d.createdAt ? d.createdAt.split('T')[0] : '';
            if (itemDate !== dateFilter) return false;
        }
        if (search) {
            const matchId = d.batchId.toLowerCase().includes(search);
            const matchName = (d.foodName || '').toLowerCase().includes(search);
            const matchCategory = d.category.toLowerCase().includes(search);
            if (!matchId && !matchName && !matchCategory) return false;
        }
        return true;
    });

    renderDonorDonationsTable(filtered);
}

// ----------------------------------------------------------------------------
// Tab Switching
// ----------------------------------------------------------------------------
function switchDonorTab(tab) {
    const tabs = ['donations', 'submit', 'reports', 'profile'];
    tabs.forEach(t => {
        const view = document.getElementById(`tab-view-${t}`);
        const pill = document.getElementById(`pill-${t === 'donations' ? 'my-donations' : (t === 'submit' ? 'submit-donation' : (t === 'reports' ? 'my-reports' : 'my-profile'))}`);
        const mobPill = document.getElementById(`mob-pill-${t}`);

        if (t === tab) {
            if (view) view.classList.remove('d-none');
            if (pill) pill.classList.add('active');
            if (mobPill) mobPill.classList.add('active');
        } else {
            if (view) view.classList.add('d-none');
            if (pill) pill.classList.remove('active');
            if (mobPill) mobPill.classList.remove('active');
        }
    });

    if (tab === 'donations') {
        loadDonorDonations();
    } else if (tab === 'reports') {
        loadDonorReport();
    }
}

// ----------------------------------------------------------------------------
// My Reports Section Logic
// ----------------------------------------------------------------------------
function handlePeriodPresetChange() {
    const preset = document.getElementById('report-period-preset').value;
    const startInput = document.getElementById('report-start-date');
    const endInput = document.getElementById('report-end-date');

    const today = new Date();
    const todayStr = today.toISOString().split('T')[0];

    if (preset === 'ALL') {
        startInput.value = '';
        endInput.value = '';
        startInput.disabled = true;
        endInput.disabled = true;
    } else if (preset === 'THIS_WEEK') {
        const day = today.getDay();
        const diff = today.getDate() - day + (day === 0 ? -6 : 1); // Monday
        const monday = new Date(today.setDate(diff));
        startInput.value = monday.toISOString().split('T')[0];
        endInput.value = todayStr;
        startInput.disabled = false;
        endInput.disabled = false;
    } else if (preset === 'THIS_MONTH') {
        const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
        startInput.value = firstDay.toISOString().split('T')[0];
        endInput.value = todayStr;
        startInput.disabled = false;
        endInput.disabled = false;
    } else if (preset === 'LAST_3_MONTHS') {
        const threeMonthsAgo = new Date();
        threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3);
        startInput.value = threeMonthsAgo.toISOString().split('T')[0];
        endInput.value = todayStr;
        startInput.disabled = false;
        endInput.disabled = false;
    } else {
        // CUSTOM
        startInput.disabled = false;
        endInput.disabled = false;
    }
}

async function loadDonorReport() {
    const startDate = document.getElementById('report-start-date').value;
    const endDate = document.getElementById('report-end-date').value;

    let url = '/api/donor/reports/summary';
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    if (params.toString()) url += `?${params.toString()}`;

    try {
        const res = await fetch(url);
        if (!res.ok) throw new Error('Failed to load report summary');
        const rep = await res.json();

        // Populate Headers
        document.getElementById('report-heading-title').innerText = rep.reportTitle || 'ANNAM Donor Contribution Report';
        document.getElementById('report-period-label').innerText = `Period: ${rep.dateRangeLabel || 'All Time'}`;
        document.getElementById('report-timestamp-label').innerText = `Generated: ${rep.generatedTimestamp || '--'}`;

        if (rep.donor) {
            document.getElementById('report-donor-metadata').innerText = 
                `Donor: ${rep.donor.name || ''} | ${rep.donor.organization || ''} (${rep.donor.donorType || 'Individual'}) | ${rep.donor.city || ''}`;
        }

        // KPIs (Smooth Counter Animations)
        animateDonorCounter('rep-stat-total-donations', rep.totalDonations || 0);
        animateDonorCounter('rep-stat-total-portions', rep.totalPortionsDonated || 0);
        animateDonorCounter('rep-stat-delivered-portions', rep.portionsSuccessfullyDelivered || 0);
        animateDonorCounter('rep-stat-expired-rejected', rep.portionsExpiredOrRejected || 0);
        document.getElementById('rep-stat-success-rate').innerText = `${(rep.deliverySuccessRate || 0).toFixed(1)}%`;

        // Category Breakdown Table
        const catTbody = document.getElementById('report-category-tbody');
        if (rep.categoryBreakdown && rep.categoryBreakdown.length > 0) {
            catTbody.innerHTML = rep.categoryBreakdown.map(c => `
                <tr>
                    <td><strong>${escapeHtml(c.category)}</strong></td>
                    <td class="text-center">${c.batchCount}</td>
                    <td class="text-center fw-bold text-success">${c.portionCount}</td>
                    <td class="text-end fw-semibold">${c.percentageOfTotal.toFixed(1)}%</td>
                </tr>
            `).join('');
        } else {
            catTbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">No category data recorded in this period.</td></tr>';
        }

        // Status Summary List
        document.getElementById('rep-status-delivered').innerText = rep.deliveredCount || 0;
        document.getElementById('rep-status-pending').innerText = rep.pendingApprovalCount || 0;
        document.getElementById('rep-status-reserved').innerText = rep.reservedCount || 0;
        document.getElementById('rep-status-rejected').innerText = rep.rejectedCount || 0;
        document.getElementById('rep-status-expired').innerText = rep.expiredCount || 0;

        // Donations List
        const itemsTbody = document.getElementById('report-items-tbody');
        if (rep.donations && rep.donations.length > 0) {
            itemsTbody.innerHTML = rep.donations.map(item => `
                <tr>
                    <td><strong class="font-monospace">${item.batchId}</strong></td>
                    <td>${escapeHtml(item.foodName || item.categoryDisplayName || item.category)}</td>
                    <td><span class="badge bg-secondary-subtle text-dark">${item.category}</span></td>
                    <td><strong>${item.totalPortions}</strong> portions</td>
                    <td class="text-muted">${item.createdAt ? item.createdAt.split('T')[0] : '--'}</td>
                    <td><strong class="text-danger">${item.declaredUseByTime}</strong></td>
                    <td><span class="badge bg-light text-dark border">${item.deliveryStatus}</span></td>
                </tr>
            `).join('');
        } else {
            itemsTbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No donation records found in the specified date range.</td></tr>';
        }
    } catch (err) {
        showDonorAlert('danger', 'Failed to generate contribution report.');
    }
}

function exportDonorReport(format) {
    const startDate = document.getElementById('report-start-date').value;
    const endDate = document.getElementById('report-end-date').value;

    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);

    const queryString = params.toString() ? `?${params.toString()}` : '';
    const exportUrl = `/api/donor/reports/export/${format}${queryString}`;

    window.open(exportUrl, '_blank');
}

// ----------------------------------------------------------------------------
// Form Handlers
// ----------------------------------------------------------------------------
function initFormHandlers() {
    // 1. Submit Donation Form
    const submitForm = document.getElementById('form-submit-donation');
    if (submitForm) {
        submitForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearDonorAlert();

            const foodName = document.getElementById('don-food-name').value.trim();
            const category = document.getElementById('don-category').value;
            const portions = parseInt(document.getElementById('don-portions').value);
            const prepTime = document.getElementById('don-prep-time').value;
            const useByTime = document.getElementById('don-use-by').value;
            const pickupLocationId = document.getElementById('don-pickup-loc').value;
            const notes = document.getElementById('don-notes').value.trim();

            let valid = true;
            if (!foodName) {
                document.getElementById('don-food-name').classList.add('is-invalid');
                valid = false;
            } else {
                document.getElementById('don-food-name').classList.remove('is-invalid');
            }

            if (!category) {
                document.getElementById('don-category').classList.add('is-invalid');
                valid = false;
            } else {
                document.getElementById('don-category').classList.remove('is-invalid');
            }

            if (!portions || portions <= 0) {
                document.getElementById('don-portions').classList.add('is-invalid');
                valid = false;
            } else {
                document.getElementById('don-portions').classList.remove('is-invalid');
            }

            if (!useByTime) {
                document.getElementById('don-use-by').classList.add('is-invalid');
                valid = false;
            } else {
                document.getElementById('don-use-by').classList.remove('is-invalid');
            }

            if (!pickupLocationId) {
                document.getElementById('don-pickup-loc').classList.add('is-invalid');
                valid = false;
            } else {
                document.getElementById('don-pickup-loc').classList.remove('is-invalid');
            }

            if (!valid) return;

            const btn = document.getElementById('btn-submit-donation-action');
            btn.disabled = true;

            try {
                const res = await fetch('/api/donor/donations', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        foodName,
                        category,
                        totalPortions: portions,
                        preparationTime: prepTime,
                        useByTime,
                        pickupLocationId,
                        notes
                    })
                });

                const data = await res.json();
                if (res.ok && data.success) {
                    showDonorAlert('success', `Donation ${data.donation.batchId} submitted successfully! Status: Pending Admin Approval.`);
                    submitForm.reset();
                    if (currentDonorProfile && currentDonorProfile.locationId) {
                        document.getElementById('don-pickup-loc').value = currentDonorProfile.locationId;
                    }
                    await loadDonorProfile();
                    switchDonorTab('donations');
                } else {
                    showDonorAlert('danger', data.message || 'Failed to submit donation.');
                }
            } catch (err) {
                showDonorAlert('danger', 'Error submitting donation. Please check server connectivity.');
            } finally {
                btn.disabled = false;
            }
        });
    }

    // 2. Profile Update Form
    const profileForm = document.getElementById('form-update-profile');
    if (profileForm) {
        profileForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearDonorAlert();

            const name = document.getElementById('profile-name').value.trim();
            const phone = document.getElementById('profile-phone').value.trim();
            const pickupAddress = document.getElementById('profile-address').value.trim();
            const locationId = document.getElementById('profile-location').value;

            try {
                const res = await fetch('/api/donor/profile', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name, phone, pickupAddress, locationId })
                });

                const data = await res.json();
                if (res.ok && data.success) {
                    showDonorAlert('success', 'Profile details updated successfully.');
                    await loadDonorProfile();
                } else {
                    showDonorAlert('danger', data.message || 'Failed to update profile.');
                }
            } catch (err) {
                showDonorAlert('danger', 'Error updating profile.');
            }
        });
    }
}

// ----------------------------------------------------------------------------
// Logout
// ----------------------------------------------------------------------------
async function handleDonorLogout() {
    if (!confirm('Are you sure you want to log out of the Donor Portal?')) return;
    try {
        await fetch('/api/auth/logout', { method: 'POST' });
        window.location.href = '/login.html?message=logged_out';
    } catch (err) {
        window.location.href = '/login.html';
    }
}

// ----------------------------------------------------------------------------
// Alerts
// ----------------------------------------------------------------------------
function showDonorAlert(type, message) {
    const container = document.getElementById('donor-alert-container');
    if (!container) return;

    let icon = 'bi-info-circle-fill';
    if (type === 'success') icon = 'bi-check-circle-fill';
    if (type === 'danger') icon = 'bi-exclamation-triangle-fill';
    if (type === 'warning') icon = 'bi-exclamation-circle-fill';

    container.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show shadow-sm py-2 px-3 small d-flex align-items-center gap-2 mb-3" role="alert">
            <i class="bi ${icon} flex-shrink-0"></i>
            <div class="flex-grow-1">${escapeHtml(message)}</div>
            <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
        </div>
    `;

    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function clearDonorAlert() {
    const container = document.getElementById('donor-alert-container');
    if (container) container.innerHTML = '';
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.innerText = text;
    return div.innerHTML;
}
