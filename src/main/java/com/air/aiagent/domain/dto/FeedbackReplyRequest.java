package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * 反馈回复请求DTO
 */
@Data
public class FeedbackReplyRequest {

    /**
     * 反馈ID
     */
    private Long id;

    /**
     * 管理员回复
     */
    private String adminReply;

    /**
     * 处理状态
     */
    private String status;
}
