/**
 * FoodRescue AI - Enterprise API Service
 * Handles JWT injection, refresh, and centralized fetch logic.
 *
 * B-03 FIX: Changed baseUrl from '/api/v1' to '/api' to match backend routes.
 */
class ApiService {
    constructor() {
        this.baseUrl = '/api';          // ← FIXED (was /api/v1)
        this.tokenKey = 'jwt_token';
        this.userKey  = 'fr_user';
    }

    // ── Token Management ──────────────────────────────────────────────────────

    getToken() {
        return localStorage.getItem(this.tokenKey);
    }

    setToken(token) {
        localStorage.setItem(this.tokenKey, token);
    }

    clearTokens() {
        localStorage.removeItem(this.tokenKey);
        sessionStorage.removeItem(this.userKey);
    }

    // ── Core Fetch Wrapper ────────────────────────────────────────────────────

    async fetch(endpoint, options = {}) {
        const url = `${this.baseUrl}${endpoint}`;
        const token = this.getToken();

        const headers = new Headers(options.headers || {});
        if (!options.isMultipart) {
            headers.set('Content-Type', 'application/json');
        }
        if (token) {
            headers.set('Authorization', `Bearer ${token}`);
        }

        const config = { ...options, headers };

        try {
            const response = await window.fetch(url, config);

            if (response.status === 401) {
                // Token invalid/expired — force re-login only on protected pages
                this.clearTokens();
                const path = window.location.pathname;
                if (path.startsWith('/donor') || path.startsWith('/ngo') || path.startsWith('/admin')) {
                    window.location.href = '/login?session_expired=true';
                }
                throw new Error('Not authenticated.');
            }

            if (response.status === 403) {
                if (token) {
                    window.location.href = '/error/403';
                }
                throw new Error('Permission denied.');
            }

            if (!response.ok) {
                const errData = await response.json().catch(() => ({}));
                throw new Error(errData.message || `HTTP Error: ${response.status}`);
            }

            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            }
            return await response.text();

        } catch (error) {
            console.error('[API Service Error]', endpoint, error.message);
            throw error;
        }
    }

    // ── Common HTTP Methods ───────────────────────────────────────────────────

    get(endpoint)           { return this.fetch(endpoint, { method: 'GET' }); }
    post(endpoint, data)    { return this.fetch(endpoint, { method: 'POST',   body: JSON.stringify(data) }); }
    put(endpoint, data)     { return this.fetch(endpoint, { method: 'PUT',    body: JSON.stringify(data) }); }
    patch(endpoint, data)   { return this.fetch(endpoint, { method: 'PATCH',  body: JSON.stringify(data) }); }
    delete(endpoint)        { return this.fetch(endpoint, { method: 'DELETE' }); }

    postForm(endpoint, formData) {
        return this.fetch(endpoint, { method: 'POST', body: formData, isMultipart: true });
    }
}

window.api = new ApiService();
