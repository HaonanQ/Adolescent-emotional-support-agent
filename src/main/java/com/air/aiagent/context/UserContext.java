package com.air.aiagent.context;

import java.util.Optional;

/**
 * 用户上下文管理器（基于ThreadLocal）
 */
public class UserContext {

    private static final ThreadLocal<String> currentUserId = new ThreadLocal<>();

    private static final ThreadLocal<String> currentSessionId = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     */
    public static void setUserId(String userId) {
        currentUserId.set(userId);
    }

    /**
     * 获取当前用户ID
     */
    public static String getUserId() {
        return currentUserId.get();
    }

    /**
     * 获取当前用户ID（安全方式）
     */
    public static String getSafeUserId() {
        return Optional.ofNullable(currentUserId.get()).orElse("SYSTEM");
    }

    /**
     * 设置当前会话ID
     */
    public static void setSessionId(String sessionId) {
        currentSessionId.set(sessionId);
    }

    /**
     * 获取当前会话ID
     */
    public static String getSessionId() {
        return currentSessionId.get();
    }

    /**
     * 获取当前会话ID（安全方式）
     */
    public static String getSafeSessionId() {
        return Optional.ofNullable(currentSessionId.get()).orElse("");
    }

    /**
     * 清除所有ThreadLocal数据
     */
    public static void clear() {
        currentUserId.remove();
        currentSessionId.remove();
    }
}