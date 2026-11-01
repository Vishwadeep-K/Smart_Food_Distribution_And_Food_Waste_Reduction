package com.annam.config;

import com.annam.model.User;
import com.annam.model.UserRole;
import com.annam.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 1. Always allow static resources and HTML pages
        if (uri.endsWith(".html") || uri.endsWith(".css") || uri.endsWith(".js") || uri.endsWith(".ico")
                || uri.endsWith(".png") || uri.endsWith(".jpg") || uri.endsWith(".svg")
                || uri.startsWith("/css/") || uri.startsWith("/js/")) {
            return true;
        }

        // 2. Always allow public authentication and lookup endpoints
        if (uri.startsWith("/api/auth/") || uri.equals("/api/routing/locations")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        User currentUser = session != null ? (User) session.getAttribute(AuthService.SESSION_USER_KEY) : null;

        // 3. Donor-specific endpoints: /api/donor/**
        if (uri.startsWith("/api/donor/")) {
            if (currentUser == null) {
                sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required. Please log in.");
                return false;
            }
            if (currentUser.getRole() != UserRole.DONOR && currentUser.getRole() != UserRole.ADMIN) {
                sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Access Denied: Donor privileges required.");
                return false;
            }
            return true;
        }

        // 4. Batch creation, approval, rejection, and pending review: strictly ADMIN only
        if (uri.startsWith("/api/batches")) {
            boolean batchAdminAction = uri.endsWith("/approve") 
                    || uri.endsWith("/reject") 
                    || uri.equals("/api/batches/pending")
                    || method.equals("POST")
                    || method.equals("PUT")
                    || method.equals("DELETE");

            if (batchAdminAction) {
                if (currentUser == null) {
                    sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Admin authentication required.");
                    return false;
                }
                if (currentUser.getRole() != UserRole.ADMIN) {
                    sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Access Denied: Only Administrator can manage batch approvals and allocations.");
                    return false;
                }
                return true;
            }
        }

        // 5. Protected Admin-only endpoints
        boolean isAdminOnly = uri.startsWith("/api/dashboard")
                || uri.startsWith("/api/matching")
                || uri.startsWith("/api/allocations")
                || uri.startsWith("/api/vehicles")
                || uri.startsWith("/api/recipients")
                || uri.startsWith("/api/clock")
                || uri.startsWith("/api/reports")
                || uri.startsWith("/api/donors");

        if (isAdminOnly) {
            if (currentUser == null) {
                sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required. Please log in as Admin.");
                return false;
            }
            if (currentUser.getRole() != UserRole.ADMIN) {
                sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Access Denied: Admin privileges required.");
                return false;
            }
            return true;
        }

        // Default: allow other endpoints (e.g. GET /api/batches)
        return true;
    }

    private void sendJsonError(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format("{\"success\":false,\"error\":\"%s\",\"message\":\"%s\"}",
                statusCode == HttpServletResponse.SC_UNAUTHORIZED ? "Unauthorized" : "Forbidden",
                message.replace("\"", "\\\"")));
    }
}
