package com.papergen.generate.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.papergen.generate.domain.entity.SysUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 生成与校验。
 * 载荷包含用户 ID（subject）、账号、角色，签发后由前端以 Cookie 保存。
 */
@Component
public class JwtUtil {

    private final String secret;
    private final String issuer;
    private final long expireMinutes;

    public JwtUtil(@Value("${papergen.jwt.secret}") String secret,
                   @Value("${papergen.jwt.issuer:papergen}") String issuer,
                   @Value("${papergen.jwt.expire-minutes:10080}") long expireMinutes) {
        this.secret = secret;
        this.issuer = issuer;
        this.expireMinutes = expireMinutes;
    }

    /**
     * 为登录用户签发令牌
     */
    public String generateToken(SysUser user) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expireMinutes * 60_000L);
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(String.valueOf(user.getSysUserId()))
                .withClaim("username", user.getUsername())
                .withClaim("role", user.getRole())
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 校验令牌，失败时抛出 JWTVerificationException
     */
    public DecodedJWT verify(String token) {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                .withIssuer(issuer)
                .build();
        return verifier.verify(token);
    }

    public long getExpireSeconds() {
        return expireMinutes * 60L;
    }
}
