/**
 * FoodRescue AI — Donation Request Workflow
 *
 * Reuses window.api (api-service.js) for all HTTP calls.
 * Do NOT duplicate authentication logic here.
 *
 * Provides:
 *   - loadAvailableNgos()     → GET /api/ngo/available
 *   - createRequest()         → POST /api/requests
 *   - loadDonorRequests()     → GET /api/requests/donor
 *   - loadNgoRequests()       → GET /api/requests/ngo
 *   - acceptRequest(id)       → PATCH /api/requests/{id}/accept
 *   - rejectRequest(id, reason) → PATCH /api/requests/{id}/reject
 *   - assignDelivery(id, name, phone) → PATCH /api/requests/{id}/assign
 *   - startPickup(id)         → PATCH /api/requests/{id}/pickup-start
 *   - collectFood(id)         → PATCH /api/requests/{id}/collect
 *   - receiveFood(id, qty, notes) → PATCH /api/requests/{id}/receive
 *   - deliverFood(id, notes)  → PATCH /api/requests/{id}/deliver
 *   - cancelRequest(id)       → PATCH /api/requests/{id}/cancel
 */
class DonationRequestService {

    // ── NGO Discovery ─────────────────────────────────────────────────────────

    async loadAvailableNgos() {
        return window.api.get('/ngo/available');
    }

    // ── Create Request ────────────────────────────────────────────────────────

    async createRequest(donationId, ngoId) {
        return window.api.post('/requests', { donationId, ngoId });
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    async loadDonorRequests() {
        return window.api.get('/requests/donor');
    }

    async loadNgoRequests() {
        return window.api.get('/requests/ngo');
    }

    async getRequestById(id) {
        return window.api.get(`/requests/${id}`);
    }

    // ── Status Transitions ────────────────────────────────────────────────────

    async acceptRequest(id) {
        return window.api.patch(`/requests/${id}/accept`);
    }

    async rejectRequest(id, reason) {
        return window.api.patch(`/requests/${id}/reject`, { reason });
    }

    async assignDelivery(id, assignedToName, assignedToPhone) {
        return window.api.patch(`/requests/${id}/assign`, { assignedToName, assignedToPhone });
    }

    async startPickup(id) {
        return window.api.patch(`/requests/${id}/pickup-start`);
    }

    async collectFood(id) {
        return window.api.patch(`/requests/${id}/collect`);
    }

    async receiveFood(id, receivedQuantityKg, notes) {
        return window.api.patch(`/requests/${id}/receive`, { receivedQuantityKg, notes });
    }

    async deliverFood(id, notes) {
        return window.api.patch(`/requests/${id}/deliver`, { notes });
    }

    async cancelRequest(id) {
        return window.api.patch(`/requests/${id}/cancel`);
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    /** Returns a Bootstrap badge class + label for a given status string. */
    statusBadge(status) {
        const map = {
            'PENDING':            ['bg-warning bg-opacity-15 text-warning border border-warning border-opacity-25', 'PENDING'],
            'ACCEPTED':           ['bg-primary bg-opacity-15 text-primary border border-primary border-opacity-25', 'ACCEPTED'],
            'REJECTED':           ['bg-danger bg-opacity-15 text-danger border border-danger border-opacity-25', 'REJECTED'],
            'ASSIGNED':           ['bg-info bg-opacity-15 text-info border border-info border-opacity-25', 'ASSIGNED'],
            'PICKUP_IN_PROGRESS': ['bg-secondary bg-opacity-15 text-secondary border border-secondary border-opacity-25', 'PICKUP IN PROGRESS'],
            'COLLECTED':          ['bg-success bg-opacity-15 text-success border border-success border-opacity-25', 'COLLECTED'],
            'RECEIVED':           ['bg-success bg-opacity-20 text-success border border-success border-opacity-50', 'RECEIVED'],
            'DELIVERED':          ['bg-success text-white', 'DELIVERED ✓'],
            'EXPIRED':            ['bg-danger text-white', 'EXPIRED'],
            'CANCELLED':          ['bg-secondary text-white', 'CANCELLED'],
        };
        const s = (status || 'UNKNOWN').toUpperCase();
        const [cls, label] = map[s] || ['bg-secondary bg-opacity-10 text-secondary', s];
        return `<span class="badge ${cls} px-3 py-2 rounded-pill fw-semibold">${label}</span>`;
    }

    /** Returns action buttons for the NGO based on current request status. */
    ngoActionButtons(request) {
        const id = request.id;
        const s = request.status;
        switch (s) {
            case 'PENDING':
                return `
                    <button class="btn btn-sm btn-success rounded-pill px-3 me-1" onclick="window.drService.handleAccept(${id})">
                        <i class="fa-solid fa-check me-1"></i>Accept
                    </button>
                    <button class="btn btn-sm btn-outline-danger rounded-pill px-3" onclick="window.drService.handleReject(${id})">
                        <i class="fa-solid fa-x me-1"></i>Reject
                    </button>`;
            case 'ACCEPTED':
                return `<button class="btn btn-sm btn-primary rounded-pill px-3" onclick="window.drService.handleAssign(${id})">
                    <i class="fa-solid fa-person-walking me-1"></i>Assign Delivery</button>`;
            case 'ASSIGNED':
                return `<button class="btn btn-sm btn-warning rounded-pill px-3" onclick="window.drService.handlePickup(${id})">
                    <i class="fa-solid fa-truck me-1"></i>Start Pickup</button>`;
            case 'PICKUP_IN_PROGRESS':
                return `<button class="btn btn-sm btn-info rounded-pill px-3" onclick="window.drService.handleCollect(${id})">
                    <i class="fa-solid fa-box me-1"></i>Mark Collected</button>`;
            case 'COLLECTED':
                return `<button class="btn btn-sm btn-success rounded-pill px-3" onclick="window.drService.handleReceive(${id})">
                    <i class="fa-solid fa-warehouse me-1"></i>Confirm Received</button>`;
            case 'RECEIVED':
                return `<button class="btn btn-sm btn-success rounded-pill px-3" onclick="window.drService.handleDeliver(${id})">
                    <i class="fa-solid fa-flag-checkered me-1"></i>Mark Delivered</button>`;
            default:
                return `<span class="text-muted small">No actions available</span>`;
        }
    }

    /** Formats a ISO datetime string for display. */
    formatDateTime(iso) {
        if (!iso) return '—';
        const d = new Date(iso);
        return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
             + ' ' + d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
    }

    // ── UI Action Handlers (used by NGO pages) ────────────────────────────────

    /** Helper to find the button triggered for this request and action. */
    _getButton(id, actionName) {
        const selector = `button[onclick*="handle${actionName}(${id})"], button[onclick*="handle${actionName}(${id},"]`;
        return document.querySelector(selector);
    }

    /** Helper to refresh the request lists on the page dynamically without full reload. */
    async _refreshLists() {
        if (typeof window.loadRequests === 'function') {
            await window.loadRequests();
        } else if (typeof window.loadIncomingRequests === 'function') {
            await window.loadIncomingRequests();
        } else {
            setTimeout(() => window.location.reload(), 800);
        }
    }

    async handleAccept(id) {
        if (!confirm('Accept this food rescue request?')) return;
        const btn = this._getButton(id, 'Accept');
        let originalHtml = '';
        if (btn) {
            originalHtml = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Processing...';
        }
        try {
            await this.acceptRequest(id);
            if (window.showToast) window.showToast('Request accepted successfully!', 'success');
            await this._refreshLists();
        } catch (e) {
            if (window.showToast) window.showToast('Error: ' + e.message, 'error');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalHtml;
            }
        }
    }

    async handleReject(id) {
        const reason = prompt('Rejection reason (optional):') || '';
        if (!confirm('Reject this request?')) return;
        const btn = this._getButton(id, 'Reject');
        let originalHtml = '';
        if (btn) {
            originalHtml = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Processing...';
        }
        try {
            await this.rejectRequest(id, reason);
            if (window.showToast) window.showToast('Request rejected.', 'info');
            await this._refreshLists();
        } catch (e) {
            if (window.showToast) window.showToast('Error: ' + e.message, 'error');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalHtml;
            }
        }
    }

    async handleAssign(id) {
        const name  = prompt('Delivery person name:') || 'NGO Team';
        const phone = prompt('Delivery person phone (optional):') || '';
        if (!confirm(`Assign delivery to "${name}"?`)) return;
        const btn = this._getButton(id, 'Assign');
        let originalHtml = '';
        if (btn) {
            originalHtml = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Processing...';
        }
        try {
            await this.assignDelivery(id, name, phone);
            if (window.showToast) window.showToast('Delivery assigned!', 'success');
            await this._refreshLists();
        } catch (e) {
            if (window.showToast) window.showToast('Error: ' + e.message, 'error');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalHtml;
            }
        }
    }

    async handlePickup(id) {
        if (!confirm('Start pickup for this request?')) return;
        const btn = this._getButton(id, 'Pickup');
        let originalHtml = '';
        if (btn) {
            originalHtml = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Processing...';
        }
        try {
            await this.startPickup(id);
            if (window.showToast) window.showToast('Pickup started!', 'success');
            await this._refreshLists();
        } catch (e) {
            if (window.showToast) window.showToast('Error: ' + e.message, 'error');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalHtml;
            }
        }
    }

    async handleCollect(id) {
        if (!confirm('Mark food as collected?')) return;
        const btn = this._getButton(id, 'Collect');
        let originalHtml = '';
        if (btn) {
            originalHtml = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Processing...';
        }
        try {
            await this.collectFood(id);
            if (window.showToast) window.showToast('Food marked as collected!', 'success');
            await this._refreshLists();
        } catch (e) {
            if (window.showToast) window.showToast('Error: ' + e.message, 'error');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalHtml;
            }
        }
    }

    async handleReceive(id) {
        const qty   = parseFloat(prompt('Quantity received (kg):') || '0') || null;
        const notes = prompt('Receiving notes (optional):') || '';
        if (!confirm('Confirm food received?')) return;
        const btn = this._getButton(id, 'Receive');
        let originalHtml = '';
        if (btn) {
            originalHtml = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Processing...';
        }
        try {
            await this.receiveFood(id, qty, notes);
            if (window.showToast) window.showToast('Food received confirmed!', 'success');
            await this._refreshLists();
        } catch (e) {
            if (window.showToast) window.showToast('Error: ' + e.message, 'error');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalHtml;
            }
        }
    }

    async handleDeliver(id) {
        const notes = prompt('Delivery notes (optional):') || '';
        if (!confirm('Mark food as delivered/distributed?')) return;
        const btn = this._getButton(id, 'Deliver');
        let originalHtml = '';
        if (btn) {
            originalHtml = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Processing...';
        }
        try {
            await this.deliverFood(id, notes);
            if (window.showToast) window.showToast('🎉 Food delivered! Mission complete.', 'success');
            await this._refreshLists();
        } catch (e) {
            if (window.showToast) window.showToast('Error: ' + e.message, 'error');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalHtml;
            }
        }
    }
}

// Single global instance reused across all pages
window.drService = new DonationRequestService();
