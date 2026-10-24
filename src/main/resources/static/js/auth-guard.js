/**
 * FoodRescue AI - Client-Side Authentication & Permission Guard
 * Provides a client-side UX layer on top of server security.
 * NOTE: The Spring Boot backend is always the authoritative source of truth.
 */
class AuthGuard {
    constructor() {
        this.sessionTimeoutWarningMs = 5 * 60 * 1000; // 5 min before expiry
        this.sessionTimeoutMs = 30 * 60 * 1000; // 30 min max inactivity
        this.lastActivity = Date.now();
        this.warningTimer = null;
        this.logoutTimer = null;
        this.warningShown = false;

        this._initActivityListeners();
        this._startSessionTimer();
    }

    // ── Session Timeout Logic ─────────────────────────────────────────────────

    _initActivityListeners() {
        const resetActivity = () => {
            this.lastActivity = Date.now();
            this.warningShown = false;
            this._hideSessionWarning();
            this._startSessionTimer();
        };

        ['mousemove', 'keydown', 'scroll', 'click', 'touchstart'].forEach(evt => {
            window.addEventListener(evt, resetActivity, { passive: true });
        });
    }

    _startSessionTimer() {
        clearTimeout(this.warningTimer);
        clearTimeout(this.logoutTimer);

        // Only run timers if a user session exists
        if (!window.api || !window.api.getToken()) return;

        // Show warning 5 min before timeout
        this.warningTimer = setTimeout(() => {
            this._showSessionWarning();
        }, this.sessionTimeoutMs - this.sessionTimeoutWarningMs);

        // Auto logout after full inactivity period
        this.logoutTimer = setTimeout(() => {
            this._forceLogout('Your session expired due to inactivity.');
        }, this.sessionTimeoutMs);
    }

    _showSessionWarning() {
        if (this.warningShown) return;
        this.warningShown = true;

        let modal = document.getElementById('session-timeout-modal');
        if (!modal) {
            modal = document.createElement('div');
            modal.id = 'session-timeout-modal';
            modal.setAttribute('role', 'alertdialog');
            modal.setAttribute('aria-modal', 'true');
            modal.setAttribute('aria-labelledby', 'session-timeout-title');
            modal.style.cssText = `
                position: fixed; inset: 0; z-index: 10000;
                background: rgba(0,0,0,0.5); backdrop-filter: blur(4px);
                display: flex; align-items: center; justify-content: center;
            `;
            modal.innerHTML = `
                <div class="bg-white rounded-4 shadow-2xl p-5" style="max-width: 420px; width: 90%;">
                    <div class="text-center mb-4">
                        <div class="bg-warning bg-opacity-10 text-warning p-3 rounded-circle d-inline-flex mb-3">
                            <i class="fa-solid fa-clock-rotate-left fs-3"></i>
                        </div>
                        <h5 class="fw-bold" id="session-timeout-title">Session Expiring Soon</h5>
                        <p class="text-muted small mb-0">Your session will automatically expire in <span id="session-countdown" class="fw-bold text-danger">5:00</span> due to inactivity.</p>
                    </div>
                    <div class="d-flex gap-3">
                        <button onclick="window.authGuard.extendSession()" class="btn btn-enterprise btn-enterprise-primary flex-fill">
                            <i class="fa-solid fa-refresh me-2"></i>Stay Logged In
                        </button>
                        <button onclick="window.authGuard._forceLogout()" class="btn btn-outline-danger flex-fill">
                            Log Out
                        </button>
                    </div>
                </div>
            `;
            document.body.appendChild(modal);
            this._startCountdown(this.sessionTimeoutWarningMs / 1000);
        }
        modal.style.display = 'flex';
    }

    _startCountdown(seconds) {
        const countdownEl = document.getElementById('session-countdown');
        if (!countdownEl) return;

        const interval = setInterval(() => {
            seconds--;
            if (seconds <= 0) { clearInterval(interval); return; }
            const mins = Math.floor(seconds / 60);
            const secs = seconds % 60;
            countdownEl.textContent = `${mins}:${secs.toString().padStart(2, '0')}`;
        }, 1000);
    }

    _hideSessionWarning() {
        const modal = document.getElementById('session-timeout-modal');
        if (modal) modal.style.display = 'none';
    }

    extendSession() {
        this.lastActivity = Date.now();
        this.warningShown = false;
        this._hideSessionWarning();
        this._startSessionTimer();
    }

    _forceLogout(message = 'You have been logged out.') {
        if (window.api) window.api.clearTokens();
        if (window.sessionStorage) sessionStorage.clear();
        const url = new URL('/login', window.location.origin);
        url.searchParams.set('message', message);
        window.location.href = url.toString();
    }

    // ── Role-Based Permission Guard ────────────────────────────────────────────

    restrictAreas(restrictedSelectors) {
        // Called after state manager initializes with user role
        const role = window.stateManager ? window.stateManager.getRole() : null;
        if (!role) return;

        restrictedSelectors.forEach(({ selector, allowedRoles }) => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(el => {
                if (!allowedRoles.includes(role)) {
                    el.style.display = 'none';
                    el.setAttribute('aria-hidden', 'true');
                }
            });
        });
    }
}

window.authGuard = new AuthGuard();
