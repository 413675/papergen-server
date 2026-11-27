package com.papergen.generate.context;

import com.papergen.generate.domain.entity.SysUser;

/**
 * 当前请求的登录用户上下文。
 * 由登录 / JWT 过滤器解析用户信息后写入，请求结束时调用 {@link #clear()} 清理，避免线程复用导致串号。
 * 未登录或系统级调用时 {@link #get()} 为 null。
 */
public final class PapergenContext {

    private static final ThreadLocal<SysUser> HOLDER = new ThreadLocal<>();

    private PapergenContext() {
    }

    public static void set(SysUser user) {
        HOLDER.set(user);
    }

    public static SysUser get() {
        return HOLDER.get();
    }

    /**
     * 当前登录用户 ID，未登录返回 null
     */
    public static Long getUserId() {
        SysUser user = HOLDER.get();
        return user == null ? null : user.getSysUserId();
    }

    /**
     * 当前登录用户名，未登录返回 null
     */
    public static String getUsername() {
        SysUser user = HOLDER.get();
        return user == null ? null : user.getUsername();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
