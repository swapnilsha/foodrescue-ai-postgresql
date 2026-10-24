/**
 * FoodRescue AI - Geospatial Intelligence
 * Wrapper for Google Maps API, managing routing and clustered markers.
 */
class MapIntegration {
    constructor() {
        this.map = null;
        this.markers = [];
        this.directionsService = null;
        this.directionsRenderer = null;
    }

    // Must be called from the Google Maps callback
    initMap(containerId, options = {}) {
        if (!window.google) {
            console.error('Google Maps API not loaded.');
            return null;
        }

        const defaultOptions = {
            center: { lat: 38.9072, lng: -77.0369 }, // Default: DC
            zoom: 12,
            styles: this._getEnterpriseMapStyle(),
            disableDefaultUI: true,
            zoomControl: true,
        };

        this.map = new window.google.maps.Map(
            document.getElementById(containerId), 
            { ...defaultOptions, ...options }
        );

        this.directionsService = new window.google.maps.DirectionsService();
        this.directionsRenderer = new window.google.maps.DirectionsRenderer({
            map: this.map,
            suppressMarkers: false,
            polylineOptions: { strokeColor: '#10b981', strokeWeight: 5 }
        });

        return this.map;
    }

    addMarker(lat, lng, type = 'DONOR', title = '', payload = {}) {
        if (!this.map) return;

        // Custom icon based on type
        let iconColor = type === 'URGENT' ? '#ef4444' : (type === 'NGO' ? '#3b82f6' : '#10b981');
        
        const marker = new window.google.maps.Marker({
            position: { lat, lng },
            map: this.map,
            title: title,
            icon: {
                path: window.google.maps.SymbolPath.CIRCLE,
                scale: 10,
                fillColor: iconColor,
                fillOpacity: 1,
                strokeWeight: 2,
                strokeColor: '#ffffff'
            }
        });

        marker.addListener('click', () => {
            window.stateManager?.emit('markerClicked', payload);
        });

        this.markers.push(marker);
        return marker;
    }

    clearMarkers() {
        this.markers.forEach(m => m.setMap(null));
        this.markers = [];
    }

    async calculateRoute(origin, destination) {
        if (!this.directionsService) return;

        return new Promise((resolve, reject) => {
            this.directionsService.route({
                origin: origin,
                destination: destination,
                travelMode: 'DRIVING'
            }, (response, status) => {
                if (status === 'OK') {
                    this.directionsRenderer.setDirections(response);
                    resolve(response);
                } else {
                    reject(status);
                }
            });
        });
    }

    _getEnterpriseMapStyle() {
        // Minimal gray Google Maps styling to fit the premium theme
        return [
            { "elementType": "geometry", "stylers": [{"color": "#f5f5f5"}] },
            { "elementType": "labels.icon", "stylers": [{"visibility": "off"}] },
            { "elementType": "labels.text.fill", "stylers": [{"color": "#616161"}] },
            { "elementType": "labels.text.stroke", "stylers": [{"color": "#f5f5f5"}] },
            { "featureType": "administrative.land_parcel", "elementType": "labels.text.fill", "stylers": [{"color": "#bdbdbd"}] },
            { "featureType": "poi", "elementType": "geometry", "stylers": [{"color": "#eeeeee"}] },
            { "featureType": "poi", "elementType": "labels.text.fill", "stylers": [{"color": "#757575"}] },
            { "featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#e5e5e5"}] },
            { "featureType": "road", "elementType": "geometry", "stylers": [{"color": "#ffffff"}] },
            { "featureType": "road.arterial", "elementType": "labels.text.fill", "stylers": [{"color": "#757575"}] },
            { "featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#dadada"}] },
            { "featureType": "water", "elementType": "geometry", "stylers": [{"color": "#c9c9c9"}] },
            { "featureType": "water", "elementType": "labels.text.fill", "stylers": [{"color": "#9e9e9e"}] }
        ];
    }
}

window.mapIntegration = new MapIntegration();
