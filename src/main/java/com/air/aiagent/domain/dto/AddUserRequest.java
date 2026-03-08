package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * @author Qiuhaonan
 * @since 2025/9/12 下午1:09
 * 注册用户请求类
 */
@Data
public class AddUserRequest {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
