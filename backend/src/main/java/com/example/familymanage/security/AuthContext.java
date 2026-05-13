package com.example.familymanage.security;

public final class AuthContext {

    private static final ThreadLocal<AuthUser> CURRENT = new ThreadLocal<AuthUser>();

    private AuthContext() {
    }

    public static void set(AuthUser user) {
        CURRENT.set(user);
    }

    public static AuthUser get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
