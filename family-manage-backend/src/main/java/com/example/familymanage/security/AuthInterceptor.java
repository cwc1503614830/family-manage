package com.example.familymanage.security;

import com.example.familymanage.common.ForbiddenException;
import com.example.familymanage.common.UnauthorizedException;
import com.example.familymanage.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || request.getRequestURI().startsWith("/api/auth/login")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().contains("/api/files/view/")) {
            return true;
        }
        try {
            AuthUser user = authService.requireByToken(resolveToken(request));
            AuthContext.set(user);
            RequirePermission permission = resolvePermission(handler);
            if (permission != null && !user.getPermissions().contains(permission.value())) {
                throw new ForbiddenException("没有权限访问该功能");
            }
            return true;
        } catch (UnauthorizedException exception) {
            writeError(response, 401, exception.getMessage());
            return false;
        } catch (ForbiddenException exception) {
            writeError(response, 403, exception.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private RequirePermission resolvePermission(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return null;
        }
        HandlerMethod method = (HandlerMethod) handler;
        RequirePermission permission = method.getMethodAnnotation(RequirePermission.class);
        return permission == null ? method.getBeanType().getAnnotation(RequirePermission.class) : permission;
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return request.getHeader("X-Auth-Token");
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(200);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
