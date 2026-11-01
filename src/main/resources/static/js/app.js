// ============================================================================
// ANNAM FOOD DISTRIBUTION - CORE APP CONTROLLER & NAVIGATION
// ============================================================================

const API_BASE = '/api';

document.addEventListener('DOMContentLoaded', async () => {
    const isAdmin = await checkAdminSession();
    if (!isAdmin) return;
    initNavigation();
    initClockSync();
    loadDashboardView();
    loadPendingApprovalsBadge();
    populateLocationDropdowns();
    populateDonorDropdowns();
});

async function checkAdminSession() {
    try {
        const res = await fetch(`${API_BASE}/auth/me`);
        if (!res.ok) {
            window.location.href = '/login.html?error=session_expired';
            return false;
        }
        const data = await res.json();
        if (!data.authenticated || !data.user) {
            window.location.href = '/login.html?error=session_expired';
            return false;
        }
        if (data.user.role !== 'ADMIN') {
            window.location.href = '/donor-dashboard.html?error=unauthorized_admin_access';
            return false;
        }
        const displayEl = document.getElementById('admin-user-display');
        if (displayEl) {
            displayEl.innerText = data.user.fullName || 'Admin';
        }
        return true;
    } catch (err) {
        window.location.href = '/login.html?error=session_expired';
        return false;
    }
}

// ----------------------------------------------------------------------------
// Sidebar Badge for Pending Approvals
// ----------------------------------------------------------------------------
async function loadPendingApprovalsBadge() {
    try {
        const res = await fetch(`${API_BASE}/batches/pending`);
        if (!res.ok) return;
        const pending = await res.json();
        const badge = document.getElementById('sidebar-pending-badge');
        if (badge) {
            badge.innerText = pending.length;
            if (pending.length > 0) {
                badge.className = 'badge bg-danger rounded-pill';
            } else {
                badge.className = 'badge bg-secondary rounded-pill';
            }
        }
    } catch (err) {
        console.error('Failed to update pending badge:', err);
    }
}

// ----------------------------------------------------------------------------
// Navigation Handler
// ----------------------------------------------------------------------------
function initNavigation() {
    const navLinks = document.querySelectorAll('.nav-link-custom');
    const pageTitle = document.getElementById('page-title');

    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            const page = link.getAttribute('data-page');
            if (!page) return; // e.g. logout link
            e.preventDefault();

            navLinks.forEach(l => l.classList.remove('active'));
            link.classList.add('active');

            document.querySelectorAll('.page-view').forEach(view => view.classList.add('d-none'));

            const targetView = document.getElementById(`view-${page}`);
            if (targetView) {
                targetView.classList.remove('d-none');
            }

            // Set Title & Trigger view loaders
            switch (page) {
                case 'dashboard':
                    pageTitle.innerText = 'Admin Dashboard Overview';
                    loadDashboardView();
                    loadPendingApprovalsBadge();
                    break;
                case 'approvals':
                    pageTitle.innerText = 'Donation Approvals Queue';
                    loadApprovalsView();
                    break;
                case 'matching':
                    pageTitle.innerText = 'Time-Sensitive Smart Food Matching';
                    loadMatchingView();
                    break;
                case 'allocations':
                    pageTitle.innerText = 'Food Allocations & Active Deliveries';
                    loadAllocationsView();
                    break;
                case 'routing':
                    pageTitle.innerText = 'Road Network Graph & Dijkstra Routing';
                    loadRoutingView();
                    break;
                case 'batches':
                    pageTitle.innerText = 'Food Batch Management';
                    loadBatchesView();
                    break;
                case 'donors':
                    pageTitle.innerText = 'Donor Management';
                    loadDonorsView();
                    break;
                case 'recipients':
                    pageTitle.innerText = 'Beneficiary Organizations (Shelters & Centres)';
                    loadRecipientsView();
                    break;
                case 'vehicles':
                    pageTitle.innerText = 'Volunteer Delivery Vehicles';
                    loadVehiclesView();
                    break;
                case 'clock':
                    pageTitle.innerText = 'Simulated Timeline & Expiry Control';
                    loadClockView();
                    break;
                case 'history':
                    pageTitle.innerText = 'Audit Trail & Allocation History';
                    loadHistoryView();
                    break;
                case 'reports':
                    pageTitle.innerText = 'Reports & Data Structures Demonstration';
                    loadReportsView();
                    break;
                case 'settings':
                    pageTitle.innerText = 'System Configuration & Security Settings';
                    break;
            }
        });
    });

    const sidebarToggle = document.getElementById('sidebar-toggle');
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', () => {
            const sidebar = document.getElementById('sidebar');
            sidebar.classList.toggle('d-none');
        });
    }
}

// ----------------------------------------------------------------------------
// Simulated Clock Helpers
// ----------------------------------------------------------------------------
async function initClockSync() {
    try {
        const res = await fetch(`${API_BASE}/clock`);
        if (res.ok) {
            const data = await res.json();
            updateClockDisplay(data.simulatedTime, data.handoverAllowanceMinutes);
        }
    } catch (err) {
        console.error('Failed to sync simulated clock:', err);
    }
}

function updateClockDisplay(timeStr, handover) {
    const topbarClock = document.getElementById('topbar-clock-display');
    const clockPageDisplay = document.getElementById('clock-page-display');
    const customInput = document.getElementById('custom-clock-input');
    const handoverInput = document.getElementById('handover-input');

    if (topbarClock) topbarClock.innerText = timeStr;
    if (clockPageDisplay) clockPageDisplay.innerText = timeStr;
    if (customInput) customInput.value = timeStr;
    if (handoverInput && handover) handoverInput.value = handover;
}

async function quickAdvanceClock(minutes) {
    try {
        const res = await fetch(`${API_BASE}/clock/advance`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ minutes })
        });
        const data = await res.json();
        if (data.success) {
            updateClockDisplay(data.simulatedTime);
            showAlert('success', `Simulated clock advanced by +${minutes} mins to ${data.simulatedTime}.`);
            if (data.expiredBatches && data.expiredBatches.length > 0) {
                showAlert('warning', `Notice: ${data.expiredBatches.length} batch(es) reached use-by deadline and were marked EXPIRED: ${data.expiredBatches.join(', ')}.`);
            }
            refreshCurrentView();
            loadPendingApprovalsBadge();
        }
    } catch (err) {
        showAlert('danger', 'Failed to advance clock.');
    }
}

async function applyCustomClock() {
    const input = document.getElementById('custom-clock-input').value;
    if (!input) return;

    try {
        const res = await fetch(`${API_BASE}/clock/set`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ time: input })
        });
        const data = await res.json();
        if (data.success) {
            updateClockDisplay(data.simulatedTime);
            showAlert('success', `Simulated time set to ${data.simulatedTime}.`);
            if (data.expiredBatches && data.expiredBatches.length > 0) {
                showAlert('warning', `Expired batch scan: ${data.expiredBatches.length} batch(es) invalidated: ${data.expiredBatches.join(', ')}.`);
            }
            refreshCurrentView();
            loadPendingApprovalsBadge();
        }
    } catch (err) {
        showAlert('danger', 'Failed to update simulated time.');
    }
}

async function applyHandoverMinutes() {
    const handover = parseInt(document.getElementById('handover-input').value);
    const clockTime = document.getElementById('topbar-clock-display').innerText;

    try {
        const res = await fetch(`${API_BASE}/clock/set`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ time: clockTime, handover })
        });
        const data = await res.json();
        if (data.success) {
            showAlert('success', `Fixed handover safety allowance updated to ${handover} minutes.`);
        }
    } catch (err) {
        showAlert('danger', 'Failed to update handover time.');
    }
}

async function triggerExpiryCheck() {
    try {
        const res = await fetch(`${API_BASE}/clock/run-expiry`, { method: 'POST' });
        const data = await res.json();
        const logBox = document.getElementById('expiry-result-log');

        if (data.expiredCount > 0) {
            showAlert('warning', `Expiry Check Complete: ${data.expiredCount} batch(es) marked EXPIRED: ${data.expiredBatchIds.join(', ')}.`);
            if (logBox) {
                logBox.innerHTML = `<span class="text-danger fw-bold"><i class="bi bi-x-circle"></i> Invalidated ${data.expiredCount} batch(es):</span><br>` +
                    data.expiredBatchIds.map(id => `<span class="badge bg-secondary m-1">${id}</span>`).join('');
            }
        } else {
            showAlert('success', `Expiry Check Complete: All batches remain within valid use-by times at ${data.currentTime}.`);
            if (logBox) {
                logBox.innerHTML = `<span class="text-success fw-bold"><i class="bi bi-check-circle"></i> No batches expired at ${data.currentTime}. All available food is within safe use-by window.</span>`;
            }
        }
        refreshCurrentView();
        loadPendingApprovalsBadge();
    } catch (err) {
        showAlert('danger', 'Failed to run expiry check.');
    }
}

// ----------------------------------------------------------------------------
// Notification Alerts & UI Utilities
// ----------------------------------------------------------------------------
function showAlert(type, message) {
    const container = document.getElementById('alert-container');
    if (!container) return;

    const alert = document.createElement('div');
    alert.className = `alert alert-${type} alert-dismissible fade show shadow-sm`;
    alert.role = 'alert';
    alert.innerHTML = `
        <div>${message}</div>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;
    container.appendChild(alert);

    setTimeout(() => {
        alert.classList.remove('show');
        setTimeout(() => alert.remove(), 200);
    }, 6000);
}

function refreshCurrentView() {
    const activeLink = document.querySelector('.nav-link-custom.active');
    if (!activeLink) return;
    const page = activeLink.getAttribute('data-page');
    switch (page) {
        case 'dashboard': loadDashboardView(); break;
        case 'approvals': loadApprovalsView(); break;
        case 'matching': loadMatchingView(); break;
        case 'allocations': loadAllocationsView(); break;
        case 'batches': loadBatchesView(); break;
        case 'reports': loadReportsView(); break;
    }
}

async function populateLocationDropdowns() {
    try {
        const res = await fetch(`${API_BASE}/routing/locations`);
        if (!res.ok) return;
        const locations = await res.json();

        const selects = [
            'route-source-select',
            'route-dest-select',
            'new-batch-location',
            'new-donor-location',
            'new-recipient-location',
            'new-vehicle-location'
        ];

        selects.forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                el.innerHTML = locations.map(l => `<option value="${l.locationId}">${l.name} (${l.locationId}) - ${l.zoneType}</option>`).join('');
            }
        });
    } catch (err) {
        console.error('Failed to load locations for dropdowns:', err);
    }
}

async function populateDonorDropdowns() {
    try {
        const res = await fetch(`${API_BASE}/donors`);
        if (!res.ok) return;
        const donors = await res.json();
        const el = document.getElementById('new-batch-donor');
        if (el) {
            el.innerHTML = donors.map(d => `<option value="${d.donorId}">${d.name} (${d.donorId})</option>`).join('');
        }
    } catch (err) {
        console.error('Failed to load donors for dropdowns:', err);
    }
}

async function handleAdminLogout() {
    if (!confirm('Are you sure you want to log out of the Admin Dashboard?')) return;
    try {
        await fetch(`${API_BASE}/auth/logout`, { method: 'POST' });
        window.location.href = '/login.html?message=logged_out';
    } catch (err) {
        window.location.href = '/login.html';
    }
}
