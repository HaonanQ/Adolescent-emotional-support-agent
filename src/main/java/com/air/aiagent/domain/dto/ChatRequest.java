package com.air.aiagent.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 聊天请求DTO
 */
@Data
public class ChatRequest {
    /**
     * 用户提示词
     */
    private String message;

    /**
     * 用户id
     */
    private String chatId;
    /**
     * 会话id
     */
    private String sessionId;

    /**
     * 图片URL（用于多模态对话）
     */
    private String imageUrl;

    /**
     * 图片文件名
     */
    private String imageFileName;

}
