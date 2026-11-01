// ============================================================================
// REPORTS & DATA STRUCTURES SHOWCASE VIEW SCRIPT
// ============================================================================

async function loadHistoryView() {
    const tbody = document.getElementById('history-table-body');
    if (!tbody) return;

    try {
        const res = await fetch(`${API_BASE}/allocations/history`);
        if (!res.ok) throw new Error('Failed to load history');
        const history = await res.json();

        if (history.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-4">No audit events recorded yet.</td></tr>';
            return;
        }

        tbody.innerHTML = history.map(h => {
            const isReserved = h.eventType === 'RESERVED';
            const isDelivered = h.eventType === 'DELIVERED';
            const badgeClass = isDelivered ? 'badge-approved' : (isReserved ? 'badge-reserved' : 'badge-not-approved');

            return `
                <tr>
                    <td>#${h.historyId}</td>
                    <td><strong>${h.allocationId}</strong></td>
                    <td><span class="badge badge-status ${badgeClass}">${h.eventType}</span></td>
                    <td>${h.batchId}</td>
                    <td>${h.recipientId}</td>
                    <td>${h.vehicleId}</td>
                    <td><span class="badge bg-secondary">${h.allocatedPortions}</span></td>
                    <td><small class="text-muted">${new Date(h.timestamp).toLocaleTimeString()}</small></td>
                    <td><small>${h.details}</small></td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-danger py-4">Failed to fetch audit history.</td></tr>';
    }
}

async function loadReportsView() {
    try {
        const res = await fetch(`${API_BASE}/reports/data-structures`);
        if (!res.ok) throw new Error('Failed to load data structures demonstration');
        const data = await res.json();

        // 1. PriorityQueue demonstration list
        const pqList = document.getElementById('dsa-pq-list');
        if (pqList) {
            pqList.innerHTML = data.priorityQueueOrder.map(b => `
                <li class="py-1">
                    <strong>${b.batchId}</strong> (${b.category}) &mdash; 
                    <span class="text-danger fw-bold">Use-By: ${b.useByTime}</span> | 
                    <span>Avail: ${b.availablePortions} portions</span> @ ${b.pickupLocation}
                </li>
            `).join('');
        }

        // 2. Adjacency List graph view
        const graphView = document.getElementById('dsa-graph-view');
        if (graphView) {
            let text = '';
            for (const [node, edges] of Object.entries(data.weightedGraphAdjacencyList)) {
                text += `${node}:\n  ` + edges.join(', ') + '\n';
            }
            graphView.innerText = text;
        }
    } catch (err) {
        console.error('Failed to load DSA demonstration:', err);
    }
}

function loadClockView() {
    initClockSync();
}
