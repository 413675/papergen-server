package com.papergen.context;

/**
 * 当前请求的仓库上下文，由 WarehouseInterceptor 写入、请求结束清理。
 * 空值表示系统级调用或 admin 未选择仓库（不过滤）；-1 为非 admin 未带头的哨兵值（过滤为空）。
 */
public final class PapergenContext {

    /** 非 admin 未携带仓库请求头时的哨兵值，保证查询结果为空，防止越权。 */
    public static final long NONE = -1L;

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private PapergenContext() {
    }

    public static void set(Long warehouseId) {
        HOLDER.set(warehouseId);
    }

    public static Long get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}