package com.release.rr.global.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    private SecurityUtil() {}

    public static Claims getCurrentClaims() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Claims claims) {
            return claims;
        }

        return null;
    }

    public static Long getCurrentUserId() {
        Claims claims = getCurrentClaims();
        return (claims != null) ? claims.get("userId", Long.class) : null;
    }

    public static String getCurrentNickname() {
        Claims claims = getCurrentClaims();
        return (claims != null) ? claims.get("nickname", String.class) : null;
    }
}
