package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * @author WyH524
 * @since 2025/9/30 13:38
 */
@Data
public class UserLoginRequest {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
