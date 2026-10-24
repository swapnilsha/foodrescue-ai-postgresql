/**
 * FoodRescue AI - Advanced Tables Module
 * Enhances generic HTML tables with Sorting, Filtering, and Client-Side Pagination.
 */
class EnterpriseTables {
    constructor() {
        this.cache = new Map();
    }

    init(tableId, options = {}) {
        const table = document.getElementById(tableId);
        if (!table) return;

        // Extract data into JS array
        const headers = Array.from(table.querySelectorAll('thead th')).map(th => th.innerText);
        const rows = Array.from(table.querySelectorAll('tbody tr')).map(tr => {
            return {
                element: tr,
                cells: Array.from(tr.querySelectorAll('td')).map(td => td.innerText.trim())
            };
        });

        this.cache.set(tableId, { headers, rows, originalRows: [...rows] });
        
        // Wrap table and build controls if not already exists
        this._buildControls(tableId, table, options);
    }

    _buildControls(tableId, table, options) {
        if (document.getElementById(`dt-controls-${tableId}`)) return;

        const wrapper = document.createElement('div');
        wrapper.className = 'dt-wrapper d-flex justify-content-between align-items-center mb-3';
        wrapper.id = `dt-controls-${tableId}`;

        // Search Input
        const searchInput = document.createElement('input');
        searchInput.type = 'text';
        searchInput.className = 'form-control form-control-sm w-auto bg-light border-0';
        searchInput.placeholder = 'Search table...';
        
        // Debounce search
        let timeout = null;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(timeout);
            timeout = setTimeout(() => this.filter(tableId, e.target.value), 300);
        });

        // Pagination Container
        const pagination = document.createElement('div');
        pagination.className = 'small text-muted';
        pagination.id = `dt-pagination-${tableId}`;

        wrapper.appendChild(searchInput);
        wrapper.appendChild(pagination);

        table.parentNode.insertBefore(wrapper, table);
    }

    filter(tableId, query) {
        const data = this.cache.get(tableId);
        if (!data) return;

        const lowerQuery = query.toLowerCase();
        let visibleCount = 0;

        data.originalRows.forEach(row => {
            const matches = row.cells.some(cell => cell.toLowerCase().includes(lowerQuery));
            row.element.style.display = matches ? '' : 'none';
            if (matches) visibleCount++;
        });

        const pagInfo = document.getElementById(`dt-pagination-${tableId}`);
        if (pagInfo) {
            pagInfo.innerText = `Showing ${visibleCount} results`;
        }
    }

    // Export module integration
    exportToCSV(tableId) {
        const data = this.cache.get(tableId);
        if (!data) return;

        let csv = data.headers.join(',') + '\n';
        data.originalRows.forEach(row => {
            // Basic CSV escaping
            const escapedCells = row.cells.map(c => `"${c.replace(/"/g, '""')}"`);
            csv += escapedCells.join(',') + '\n';
        });

        this._downloadBlob(csv, `${tableId}_export.csv`, 'text/csv;charset=utf-8;');
    }

    _downloadBlob(content, fileName, mimeType) {
        const blob = new Blob([content], { type: mimeType });
        const link = document.createElement("a");
        const url = URL.createObjectURL(blob);
        link.setAttribute("href", url);
        link.setAttribute("download", fileName);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
}

window.enterpriseTables = new EnterpriseTables();
