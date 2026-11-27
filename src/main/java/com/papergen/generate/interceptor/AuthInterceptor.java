package com.papergen.generate.interceptor;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papergen.generate.constant.AuthConstant;
import com.papergen.generate.constant.HttpStatus;
import com.papergen.generate.constant.UserConstant;
import com.papergen.generate.context.PapergenContext;
import com.papergen.generate.domain.entity.SysUser;
import com.papergen.generate.domain.model.R;
import com.papergen.generate.service.SysUserService;
import com.papergen.generate.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 登录拦截器：从 Authorization 头或 Cookie 解析 JWT，校验通过后把用户写入 PapergenContext。
 * 令牌无效、过期、用户不存在或被禁用时返回 code=401 的统一响应。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${papergen.jwt.cookie-name:papergen_token}")
    private String cookieName;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return reject(response, "未登录或登录已过期");
        }

        SysUser user;
        try {
            DecodedJWT jwt = jwtUtil.verify(token);
            Long userId = Long.valueOf(jwt.getSubject());
            user = sysUserService.getById(userId);
        } catch (JWTVerificationException | IllegalArgumentException e) {
            return reject(response, "登录凭证无效或已过期");
        }

        if (user == null) {
            return reject(response, "账号不存在");
        }
        if (!Integer.valueOf(UserConstant.STATUS_ENABLED).equals(user.getStatus())) {
            return reject(response, "账号已被禁用");
        }

        PapergenContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        PapergenContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(AuthConstant.BEARER_PREFIX)) {
            return header.substring(AuthConstant.BEARER_PREFIX.length()).trim();
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                R.fail(HttpStatus.UNAUTHORIZED, message)));
        return false;
    }
}
