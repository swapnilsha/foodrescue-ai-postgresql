/**
 * FoodRescue AI - Real-time WebSocket Service
 * Subscribes to backend topics to push live updates to the UI without refreshing.
 * Requires SockJS and StompJS libraries.
 */
class WebSocketService {
    constructor() {
        this.stompClient = null;
        this.isConnected = false;
        this.eventListeners = {};
    }

    connect() {
        if (!window.SockJS || !window.Stomp) {
            console.error('SockJS or StompJS missing. Cannot initialize WebSockets.');
            return;
        }
        
        const token = window.api ? window.api.getToken() : null;
        if (!token) return; // Prevent anonymous connection attempts

        // Using standard Spring Boot SockJS endpoint
        const socket = new SockJS('/ws-notifications'); 
        this.stompClient = Stomp.over(socket);

        // Turn off noisy debug logging unless set in preferences
        this.stompClient.debug = null;

        const headers = { 'Authorization': `Bearer ${token}` };

        this.stompClient.connect(headers, (frame) => {
            this.isConnected = true;
            console.log('Connected to FoodRescue Real-time Network.');
            this._setupSubscriptions();
        }, (error) => {
            console.error('WebSocket connection error: ', error);
            this.isConnected = false;
            // Basic retry logic
            setTimeout(() => this.connect(), 5000); 
        });
    }

    disconnect() {
        if (this.stompClient !== null) {
            this.stompClient.disconnect();
        }
        this.isConnected = false;
    }

    _setupSubscriptions() {
        const role = window.stateManager ? window.stateManager.getRole() : null;

        // 1. Global System Alerts
        this.stompClient.subscribe('/topic/system', (message) => {
            this.dispatchMessage('system_alert', JSON.parse(message.body));
        });

        // 2. Role-specific topics
        if (role === 'NGO' || role === 'ADMIN') {
            this.stompClient.subscribe('/topic/public.donations', (message) => {
                this.dispatchMessage('new_donation', JSON.parse(message.body));
            });
        }

        if (role === 'DONOR' || role === 'ADMIN') {
            this.stompClient.subscribe('/user/queue/donations/updates', (message) => {
                this.dispatchMessage('donation_update', JSON.parse(message.body));
            });
        }
        
        // 3. User-specific private queue (Notifications)
        const user = window.stateManager ? window.stateManager.getUser() : null;
        if (user && user.email) {
            this.stompClient.subscribe(`/queue/private-${user.email}`, (message) => {
                this.dispatchMessage('notification', JSON.parse(message.body));
            });
        } else {
            this.stompClient.subscribe('/user/queue/notifications', (message) => {
                this.dispatchMessage('notification', JSON.parse(message.body));
            });
        }
    }

    on(eventType, callback) {
        if (!this.eventListeners[eventType]) {
            this.eventListeners[eventType] = [];
        }
        this.eventListeners[eventType].push(callback);
    }

    dispatchMessage(eventType, data) {
        // Broadcast locally
        if (this.eventListeners[eventType]) {
            this.eventListeners[eventType].forEach(cb => cb(data));
        }

        // Optional: show generic toast notification if unhandled UI
        this._showToast(data.message || 'New live event occurred.');
    }

    _showToast(message) {
        // Integrate with Bootstrap Toast or custom notification center logic
        const event = new CustomEvent('app-toast', { detail: { message } });
        window.dispatchEvent(event);
    }
}

window.wsService = new WebSocketService();
document.addEventListener('DOMContentLoaded', () => {
    // Connect via stateManager event so we always have a user before connecting,
    // regardless of how long /api/auth/me takes to respond.
    if (window.stateManager) {
        window.stateManager.on('userChanged', () => {
            if (!window.wsService.isConnected) {
                window.wsService.connect();
            }
        });
    }

    // Fallback: if stateManager already has a user (e.g. cached from sessionStorage), connect now
    setTimeout(() => {
        if (window.stateManager && window.stateManager.getUser() && !window.wsService.isConnected) {
            window.wsService.connect();
        }
    }, 1500);
});

