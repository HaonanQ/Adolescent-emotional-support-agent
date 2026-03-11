package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * 添加情绪日记请求类
 */
@Data
public class EmotionDiaryAddRequest {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 情绪（开心/难过/愤怒/焦虑/平静/其他）
     */
    private String mood;

    /**
     * 情绪分数（1-10）
     */
    private Integer moodScore;

    /**
     * 日记内容（支持Markdown）
     */
    private String content;

    /**
     * 图片URL
     */
    private String imageUrl;
}
