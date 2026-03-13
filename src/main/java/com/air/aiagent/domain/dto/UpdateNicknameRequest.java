package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * @author Qiuhaonan
 * @since 2026/3/12
 * 修改昵称请求类
 */
@Data
public class UpdateNicknameRequest {
    /**
     * 用户昵称
     */
    private String nickname;
}
