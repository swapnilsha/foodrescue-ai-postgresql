/**
 * FoodRescue AI - Vision Diagnostics Workflow
 * Handles camera permissions, capture, and mock AI payload simulation.
 */
class AIWorkflow {
    constructor() {
        this.stream = null;
        this.videoElement = null;
        this.canvasElement = null;
        this.isAnalyzing = false;
    }

    async startCamera(videoElementId) {
        this.videoElement = document.getElementById(videoElementId);
        if (!this.videoElement) return false;

        try {
            this.stream = await navigator.mediaDevices.getUserMedia({ 
                video: { facingMode: 'environment' } 
            });
            this.videoElement.srcObject = this.stream;
            return true;
        } catch (err) {
            console.error('Camera access denied or unavailable:', err);
            window.stateManager?.emit('error', 'Camera access blocked by device settings.');
            return false;
        }
    }

    stopCamera() {
        if (this.stream) {
            this.stream.getTracks().forEach(track => track.stop());
            this.stream = null;
        }
    }

    captureFrame() {
        if (!this.videoElement) return null;
        
        if (!this.canvasElement) {
            this.canvasElement = document.createElement('canvas');
        }

        this.canvasElement.width = this.videoElement.videoWidth;
        this.canvasElement.height = this.videoElement.videoHeight;
        
        const ctx = this.canvasElement.getContext('2d');
        ctx.drawImage(this.videoElement, 0, 0);
        
        // Return base64 JPEG
        return this.canvasElement.toDataURL('image/jpeg', 0.8);
    }

    dataURLtoBlob(dataurl) {
        var arr = dataurl.split(','), mime = arr[0].match(/:(.*?);/)[1],
            bstr = atob(arr[1]), n = bstr.length, u8arr = new Uint8Array(n);
        while(n--){
            u8arr[n] = bstr.charCodeAt(n);
        }
        return new Blob([u8arr], {type:mime});
    }

    async analyzeImage(base64Image, category = 'OTHER') {
        if (this.isAnalyzing) return;
        this.isAnalyzing = true;
        
        try {
            const blob = this.dataURLtoBlob(base64Image);
            const formData = new FormData();
            formData.append('image', blob, 'capture.jpg');
            formData.append('category', category);

            // Fetch token from localStorage or api wrapper
            const token = window.api ? window.api.getToken() : sessionStorage.getItem('jwt_token');
            const headers = {};
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }

            const response = await fetch('/api/ai/analyze', {
                method: 'POST',
                headers: headers,
                body: formData
            });

            if (!response.ok) {
                throw new Error('Server returned error status ' + response.status);
            }

            const result = await response.json();
            
            // Map backend fields to the format frontend expects
            return {
                confidence: result.confidenceScore,
                category: category.toLowerCase(),
                description: result.freshnessLabel + " surplus (" + category + ")",
                estimatedWeightKg: result.estimatedQuantityKg,
                freshnessScore: result.freshnessScore,
                allergens: [],
                isSafe: result.freshnessScore > 40,
                urgency: result.freshnessScore > 75 ? "LOW" : "HIGH",
                recommendation: result.recommendation,
                predictedExpiryTime: result.predictedExpiryTime
            };

        } catch (err) {
            console.error('AI Analysis failed:', err);
            throw err;
        } finally {
            this.isAnalyzing = false;
        }
    }
}

window.aiWorkflow = new AIWorkflow();
