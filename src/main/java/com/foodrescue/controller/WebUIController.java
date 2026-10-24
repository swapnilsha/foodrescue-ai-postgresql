package com.foodrescue.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WebUIController {

    /** Returns the role prefix ("donor", "ngo", "admin") for the authenticated user, defaulting to "donor". */
    private String rolePrefix(Authentication auth) {
        if (auth == null) return "donor";
        return auth.getAuthorities().stream()
                .filter(a -> a != null && a.getAuthority() != null && a.getAuthority().startsWith("ROLE_"))
                .map(a -> a.getAuthority().substring(5).toLowerCase())
                .findFirst()
                .orElse("donor");
    }

    @GetMapping("/")
    public String index() {
        return "auth/landing";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    // --- DONOR ROUTES ---
    @GetMapping("/donor/dashboard")
    public String donorDashboard(Model model) {
        model.addAttribute("userRole", "DONOR");
        return "donor/dashboard";
    }

    @GetMapping("/donor/upload")
    public String donorUpload(Model model) {
        model.addAttribute("userRole", "DONOR");
        return "donor/upload";
    }

    @GetMapping("/donor/history")
    public String donorHistory(Model model) {
        model.addAttribute("userRole", "DONOR");
        return "donor/history";
    }

    @GetMapping("/donor/tracking")
    public String donorTracking(Model model) {
        model.addAttribute("userRole", "DONOR");
        return "donor/tracking";
    }

    @GetMapping("/donor/analytics")
    public String donorAnalytics(Model model) {
        model.addAttribute("userRole", "DONOR");
        return "donor/analytics";
    }

    @GetMapping("/donor/impact")
    public String donorImpact(Model model) {
        model.addAttribute("userRole", "DONOR");
        return "donor/impact";
    }

    @GetMapping("/donor/notifications")
    public String donorNotifs(Model model) {
        model.addAttribute("userRole", "DONOR");
        return "donor/notifications";
    }

    @GetMapping("/donor/profile")
    public String donorProfile(Model model) {
        model.addAttribute("userRole", "DONOR");
        return "donor/profile";
    }

    // --- NGO ROUTES ---
    @GetMapping("/ngo/dashboard")
    public String ngoDashboard(Model model) {
        model.addAttribute("userRole", "NGO");
        return "ngo/dashboard";
    }

    @GetMapping("/ngo/nearby")
    public String ngoNearby(Model model) {
        model.addAttribute("userRole", "NGO");
        return "ngo/nearby";
    }

    @GetMapping("/ngo/details/{id}")
    public String ngoDetails(@PathVariable String id, Model model) {
        model.addAttribute("userRole", "NGO");
        model.addAttribute("donationId", id);
        return "ngo/details";
    }

    @GetMapping("/ngo/mission")
    public String ngoMission(Model model) {
        model.addAttribute("userRole", "NGO");
        return "ngo/mission";
    }

    @GetMapping("/ngo/map")
    public String ngoMap(Model model) {
        model.addAttribute("userRole", "NGO");
        return "ngo/map";
    }

    @GetMapping("/ngo/history")
    public String ngoHistory(Model model) {
        model.addAttribute("userRole", "NGO");
        return "ngo/history";
    }

    @GetMapping("/ngo/requests")
    public String ngoRequests(Model model) {
        model.addAttribute("userRole", "NGO");
        return "ngo/requests";
    }

    @GetMapping("/ngo/notifications")
    public String ngoNotifs(Model model) {
        model.addAttribute("userRole", "NGO");
        return "ngo/notifications";
    }

    @GetMapping("/ngo/analytics")
    public String ngoAnalytics(Model model) {
        model.addAttribute("userRole", "NGO");
        return "ngo/analytics";
    }

    @GetMapping("/ngo/profile")
    public String ngoProfile(Model model) {
        model.addAttribute("userRole", "NGO");
        return "ngo/profile";
    }

    // --- ADMIN ROUTES ---
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("userRole", "ADMIN");
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String adminUsers(Model model) {
        model.addAttribute("userRole", "ADMIN");
        return "admin/users";
    }

    @GetMapping("/admin/verification")
    public String adminVerification(Model model) {
        model.addAttribute("userRole", "ADMIN");
        return "admin/verification";
    }

    @GetMapping("/admin/monitoring")
    public String adminMonitoring(Model model) {
        model.addAttribute("userRole", "ADMIN");
        return "admin/monitoring";
    }

    @GetMapping("/admin/reports")
    public String adminReports(Model model) {
        model.addAttribute("userRole", "ADMIN");
        return "admin/reports";
    }

    @GetMapping("/admin/analytics")
    public String adminAnalytics(Model model) {
        model.addAttribute("userRole", "ADMIN");
        return "admin/analytics";
    }

    @GetMapping("/admin/audit")
    public String adminAudit(Model model) {
        model.addAttribute("userRole", "ADMIN");
        return "admin/audit";
    }

    @GetMapping("/admin/settings")
    public String adminSettings(Model model) {
        model.addAttribute("userRole", "ADMIN");
        return "admin/settings";
    }

    @GetMapping("/admin/notifications")
    public String adminNotifications(Model model) {
        model.addAttribute("userRole", "ADMIN");
        return "admin/notifications";
    }

    // --- COMMON ROUTES ---
    @GetMapping("/notifications")
    public String globalNotifs(Authentication auth, Model model) {
        String role = rolePrefix(auth).toUpperCase();
        model.addAttribute("userRole", role);
        return "notifications";
    }

    @GetMapping("/chat")
    public String chat(Authentication auth, Model model) {
        String role = rolePrefix(auth).toUpperCase();
        model.addAttribute("userRole", role);
        return "chat";
    }

    /**
     * Smart redirect: /profile → /donor/profile, /ngo/profile, or /admin/dashboard
     * depending on the authenticated user's role.
     */
    @GetMapping("/profile")
    public String profile(Authentication auth) {
        String prefix = rolePrefix(auth);
        if ("admin".equals(prefix)) return "redirect:/admin/dashboard";
        return "redirect:/" + prefix + "/profile";
    }

    /**
     * Smart redirect: /settings → /admin/settings or /donor/profile (donor/ngo have no settings page)
     */
    @GetMapping("/settings")
    public String settings(Authentication auth) {
        String prefix = rolePrefix(auth);
        if ("admin".equals(prefix)) return "redirect:/admin/settings";
        // Donors and NGOs: redirect to their profile as settings equivalent
        return "redirect:/" + prefix + "/profile";
    }

    /**
     * Help center: simple informational page (or redirect to a future help template).
     */
    @GetMapping("/help")
    public String help(Authentication auth, Model model) {
        String role = rolePrefix(auth).toUpperCase();
        model.addAttribute("userRole", role);
        return "help";
    }

    @GetMapping("/ai/center")
    public String aiCenter(Authentication auth, Model model) {
        String role = rolePrefix(auth).toUpperCase();
        model.addAttribute("userRole", role);
        return "ai/center";
    }

    // --- ERROR PAGES ---
    @GetMapping("/error/403")
    public String error403() {
        return "error/403";
    }
}