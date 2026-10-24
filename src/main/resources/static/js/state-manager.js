/**
 * FoodRescue AI - Global State Manager
 * Handles user profile, permissions, theme, and offline capabilities.
 *
 * B-06 FIX: Role comparison now strips ROLE_ prefix so 'ROLE_DONOR' matches path guard for '/donor'
 */
class StateManager {
    constructor() {
        this.state = {
            currentUser: null,
            role: null,
            theme: localStorage.getItem('theme') || 'light',
            isOffline: !navigator.onLine
        };
        this.listeners = {};

        this._initOfflineDetection();
        this._applyTheme();
    }

    async init() {
        if (!window.api || !window.api.getToken()) return; // Not logged in — skip silently

        try {
            // Check sessionStorage cache first to avoid redundant API calls
            const cachedUser = sessionStorage.getItem('fr_user');
            if (cachedUser) {
                this.setUser(JSON.parse(cachedUser));
            } else {
                // B-02: calls the new /api/auth/me endpoint
                const user = await window.api.get('/auth/me');
                if (user) {
                    this.setUser(user);
                    sessionStorage.setItem('fr_user', JSON.stringify(user));
                }
            }
            this.checkPermissions();
        } catch (e) {
            console.warn('[StateManager] Session init failed:', e.message);
            sessionStorage.removeItem('fr_user');
            window.api.clearTokens();
            const path = window.location.pathname;
            if (path.startsWith('/donor') || path.startsWith('/ngo') || path.startsWith('/admin')) {
                window.location.href = '/login?session_expired=true';
            }
        }
    }

    setUser(user) {
        this.state.currentUser = user;
        // B-06 FIX: Normalize role by stripping ROLE_ prefix for path comparisons
        this.state.role = this._normalizeRole(user.role);
        this.emit('userChanged', user);
        this._updateUIWithUser(user);
    }

    // B-06 FIX: Strip "ROLE_" prefix so guards work with 'DONOR', 'NGO', 'ADMIN'
    _normalizeRole(role) {
        if (!role) return null;
        return role.startsWith('ROLE_') ? role.substring(5) : role;
    }

    getUser() { return this.state.currentUser; }
    getRole()  { return this.state.role; } // Returns 'DONOR', 'NGO', or 'ADMIN'

    // ── Permission Guard ──────────────────────────────────────────────────────

    checkPermissions() {
        const path = window.location.pathname;
        const role = this.getRole(); // Already normalized (no ROLE_ prefix)

        if (!role) return;

        if (path.startsWith('/admin') && role !== 'ADMIN') {
            window.location.href = '/error/403';
        } else if (path.startsWith('/ngo') && role !== 'NGO' && role !== 'ADMIN') {
            window.location.href = '/error/403';
        } else if (path.startsWith('/donor') && role !== 'DONOR' && role !== 'ADMIN') {
            window.location.href = '/error/403';
        }
    }

    // ── UI Updates ────────────────────────────────────────────────────────────

    _updateUIWithUser(user) {
        // Populate any navbar user name/email elements
        document.querySelectorAll('[data-user-name]').forEach(el => {
            el.textContent = user.name || user.email || '';
        });
        document.querySelectorAll('[data-user-email]').forEach(el => {
            el.textContent = user.email || '';
        });
        document.querySelectorAll('[data-user-role]').forEach(el => {
            el.textContent = this.state.role || '';
        });
        // Unread notification badge
        this._loadUnreadCount();
    }

    async _loadUnreadCount() {
        try {
            const data = await window.api.get('/notifications/unread-count');
            const count = data?.count || 0;
            document.querySelectorAll('[data-unread-count]').forEach(el => {
                el.textContent = count;
                el.style.display = count > 0 ? '' : 'none';
            });
        } catch (e) {
            // Not authenticated yet — skip silently
        }
    }

    // ── Theme Management ──────────────────────────────────────────────────────

    toggleTheme() {
        this.state.theme = this.state.theme === 'light' ? 'dark' : 'light';
        localStorage.setItem('theme', this.state.theme);
        this._applyTheme();
        this.emit('themeChanged', this.state.theme);
    }

    _applyTheme() {
        if (this.state.theme === 'dark') {
            document.body.classList.add('dark-mode');
        } else {
            document.body.classList.remove('dark-mode');
        }
    }

    // ── Offline Detection ─────────────────────────────────────────────────────

    _initOfflineDetection() {
        window.addEventListener('online',  () => this._handleOfflineState(false));
        window.addEventListener('offline', () => this._handleOfflineState(true));
    }

    _handleOfflineState(isOffline) {
        this.state.isOffline = isOffline;
        this.emit('offlineStatus', isOffline);
        if (isOffline) {
            this._showOfflineUI();
        } else {
            this._hideOfflineUI();
        }
    }

    _showOfflineUI() {
        let banner = document.getElementById('offline-banner');
        if (!banner) {
            banner = document.createElement('div');
            banner.id = 'offline-banner';
            banner.className = 'bg-danger text-white text-center py-2 px-4 shadow fixed-top';
            banner.style.zIndex = '9999';
            banner.innerHTML = '<i class="fa-solid fa-wifi-slash me-2"></i> You are offline. Live data unavailable.';
            document.body.prepend(banner);
        }
    }

    _hideOfflineUI() {
        const banner = document.getElementById('offline-banner');
        if (banner) banner.remove();
    }

    // ── Event Emitter ─────────────────────────────────────────────────────────

    on(event, callback) {
        if (!this.listeners[event]) this.listeners[event] = [];
        this.listeners[event].push(callback);
    }

    emit(event, data) {
        (this.listeners[event] || []).forEach(cb => cb(data));
    }
}

window.stateManager = new StateManager();
document.addEventListener('DOMContentLoaded', () => window.stateManager.init());
