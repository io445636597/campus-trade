package com.campustrade.security;

import com.campustrade.common.BusinessException;

public class LoginUser {

    private static final ThreadLocal<Long> USER_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_HOLDER.get();
    }

    /**
     * Get current login user ID, throw exception if not logged in
     */
    public static Long getRequiredUserId() {
        Long userId = USER_HOLDER.get();
        if (userId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        return userId;
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}
