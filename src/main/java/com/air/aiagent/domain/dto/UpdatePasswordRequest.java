package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class UpdatePasswordRequest {
    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;
}
