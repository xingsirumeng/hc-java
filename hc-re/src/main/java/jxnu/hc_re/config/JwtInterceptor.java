package jxnu.hc_re.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 放行管理后台接口（无需登录）
        if (request.getRequestURI().contains("/admin/")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "未登录或 Token 缺失");
            return false;
        }

        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            sendUnauthorized(response, "Token 无效或已过期");
            return false;
        }

        Claims claims = jwtUtils.parseClaims(token);
        String userId = claims.getSubject();
        String name = (String) claims.get("name");
        String role = (String) claims.get("role");

        if (role == null) {
            sendUnauthorized(response, "Token 缺少角色信息");
            return false;
        }

        // 通用属性：所有已认证请求均可使用
        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentUserRole", role);

        // 向后兼容：教师角色设置旧属性
        if ("teacher".equals(role)) {
            request.setAttribute("currentTeacherId", userId);
            request.setAttribute("currentTeacherName", name);
        }

        // 学生角色设置学生属性
        if ("student".equals(role)) {
            request.setAttribute("currentStudentId", userId);
        }

        // 路径级角色授权
        String path = request.getRequestURI();

        // 教师专属路径
        if (path.matches(".*/(teacher|assignment|submission)(/.*)?$")
                && !path.matches(".*/student(/.*)?$")) {
            if (!"teacher".equals(role)) {
                sendForbidden(response, "需要教师权限");
                return false;
            }
        }

        // 学生专属路径（教师查看学生列表的接口除外）
        if (path.matches(".*/student(/.*)?$")
                && !path.matches(".*/student/by-course/.*$")) {
            if (!"student".equals(role)) {
                sendForbidden(response, "需要学生权限");
                return false;
            }
        }

        return true;
    }

    private void sendUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        String json = "{\"code\":0,\"msg\":\"" + msg + "\",\"data\":null}";
        response.getWriter().write(json);
    }

    private void sendForbidden(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        String json = "{\"code\":0,\"msg\":\"" + msg + "\",\"data\":null}";
        response.getWriter().write(json);
    }
}
