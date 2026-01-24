package com.lhamacorp.knotes.context;

public class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    public static void set(UserContext serviceContext) {
        CONTEXT.set(serviceContext);
    }

    public static UserContext get() {
        return CONTEXT.get();
    }

    public static boolean isAuthenticated() {
        return CONTEXT.get().roles().contains("USER");
    }

    public static void clear() {
        CONTEXT.remove();
    }

}
