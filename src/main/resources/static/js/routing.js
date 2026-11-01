// ============================================================================
// DELIVERY ROUTING & DIJKSTRA CALCULATOR VIEW SCRIPT
// ============================================================================

async function loadRoutingView() {
    await loadRoadsTable();
}

async function loadRoadsTable() {
    const tbody = document.getElementById('roads-table-body');
    if (!tbody) return;

    try {
        const res = await fetch(`${API_BASE}/routing/roads`);
        if (!res.ok) throw new Error('Failed to load roads');
        const roads = await res.json();

        document.getElementById('road-count-badge').innerText = `${roads.length} Roads in Network`;

        if (roads.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-3">No roads loaded.</td></tr>';
            return;
        }

        tbody.innerHTML = roads.map(r => `
            <tr>
                <td><strong>#${r.roadId}</strong></td>
                <td><span class="badge bg-light text-dark border">${r.sourceId}</span> ${r.sourceName}</td>
                <td><span class="badge bg-light text-dark border">${r.destId}</span> ${r.destName}</td>
                <td><span class="badge bg-success-subtle text-success fs-6">${r.travelTimeMinutes} mins</span></td>
                <td><small class="text-muted">${r.bidirectional ? 'Two-way (Bidirectional)' : 'One-way'}</small></td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger py-3">Failed to load roads.</td></tr>';
    }
}

async function calculateRouteDijkstra() {
    const sourceId = document.getElementById('route-source-select').value;
    const destId = document.getElementById('route-dest-select').value;

    if (!sourceId || !destId) {
        showAlert('warning', 'Please choose both source and destination locations.');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/routing/shortest-path?sourceId=${encodeURIComponent(sourceId)}&destId=${encodeURIComponent(destId)}`);
        const data = await res.json();

        const box = document.getElementById('route-result-box');
        box.classList.remove('d-none');

        if (data.reachable) {
            document.getElementById('route-time-badge').innerText = `${data.travelTimeMinutes} Minutes Travel Time`;
            document.getElementById('route-path-text').innerHTML = data.pathNodes.map(name => 
                `<span class="badge-route-node">${name}</span>`
            ).join('<span class="route-arrow">&rarr;</span>');
        } else {
            document.getElementById('route-time-badge').innerText = 'UNREACHABLE';
            document.getElementById('route-path-text').innerText = 'No path found in current road network graph.';
        }
    } catch (err) {
        showAlert('danger', 'Failed to calculate Dijkstra route.');
    }
}
