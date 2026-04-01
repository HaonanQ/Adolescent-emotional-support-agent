package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * 修改头像请求
 */
@Data
public class UpdateAvatarRequest {
    /**
     * 头像URL
     */
    private String avatar;
}
