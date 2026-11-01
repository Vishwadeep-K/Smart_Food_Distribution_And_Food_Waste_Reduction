// ============================================================================
// DASHBOARD VIEW SCRIPT - MODERN SAAS CHARTS & ANIMATED METRICS
// ============================================================================

let chartCategory = null;
let chartPortions = null;

function animateNumberCounter(elementId, targetValue, duration = 650) {
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
        // Exponential ease-out
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

async function loadDashboardView() {
    try {
        const res = await fetch(`${API_BASE}/dashboard/stats`);
        if (!res.ok) throw new Error('Failed to fetch dashboard stats');
        const data = await res.json();

        // Row 1 Cards: Animate numbers
        animateNumberCounter('stat-donors', data.totalDonors);
        animateNumberCounter('stat-total-batches', data.totalBatches);

        const activeCount = Math.max(0, (data.totalBatches || 0) - (data.expiredBatches || 0));
        animateNumberCounter('stat-active-batches', activeCount);

        animateNumberCounter('stat-pending-approvals', data.pendingApprovals || 0);
        animateNumberCounter('stat-expiring-batches', data.expiringBatches || 0);

        // Row 2 Cards: Animate numbers
        animateNumberCounter('stat-avail-portions', data.availablePortions || 0);
        animateNumberCounter('stat-reserved-portions', data.reservedPortions || 0);
        animateNumberCounter('stat-delivered-portions', data.deliveredPortions || 0);
        animateNumberCounter('stat-active-allocations', data.activeAllocations || 0);
        animateNumberCounter('stat-expired-batches', data.expiredBatches || 0);
        animateNumberCounter('stat-waste-avoided', data.foodWasteAvoidedPortions || 0);

        // Render or Update Chart.js Charts
        renderCategoryChart(data.categoryPortions || {});
        renderPortionsChart(data.availablePortions || 0, data.reservedPortions || 0, data.deliveredPortions || 0);
    } catch (err) {
        console.error('Error loading dashboard stats:', err);
    }
}

function renderCategoryChart(categoryPortions) {
    const ctx = document.getElementById('chart-category-portions');
    if (!ctx) return;

    const labels = Object.keys(categoryPortions);
    const counts = Object.values(categoryPortions);

    const colors = [
        '#059669', '#10b981', '#34d399', '#6ee7b7',
        '#0284c7', '#38bdf8', '#f59e0b', '#ec4899'
    ];

    if (chartCategory) {
        chartCategory.data.labels = labels;
        chartCategory.data.datasets[0].data = counts;
        chartCategory.update();
        return;
    }

    chartCategory = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: counts,
                backgroundColor: colors,
                borderWidth: 2,
                borderColor: '#ffffff',
                hoverOffset: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: {
                duration: 750,
                easing: 'easeOutQuart'
            },
            plugins: {
                legend: {
                    position: 'right',
                    labels: {
                        font: { family: 'Inter', size: 12, weight: 500 },
                        boxWidth: 14,
                        padding: 12
                    }
                }
            },
            cutout: '68%'
        }
    });
}

function renderPortionsChart(available, reserved, delivered) {
    const ctx = document.getElementById('chart-portion-distribution');
    if (!ctx) return;

    if (chartPortions) {
        chartPortions.data.datasets[0].data = [available, reserved, delivered];
        chartPortions.update();
        return;
    }

    chartPortions = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Available for Match', 'Reserved (In Transit)', 'Confirmed Delivered'],
            datasets: [{
                label: 'Portions',
                data: [available, reserved, delivered],
                backgroundColor: ['#10b981', '#3b82f6', '#06b6d4'],
                borderRadius: 8,
                borderSkipped: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: {
                duration: 750,
                easing: 'easeOutQuart'
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: '#f1f5f9' },
                    ticks: { font: { family: 'Inter', size: 11 } }
                },
                x: {
                    grid: { display: false },
                    ticks: { font: { family: 'Inter', size: 11, weight: 500 } }
                }
            },
            plugins: {
                legend: { display: false }
            }
        }
    });
}
