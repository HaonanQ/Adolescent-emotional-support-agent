package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * 查询情绪日记请求类
 */
@Data
public class EmotionDiaryQueryRequest {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 日记ID（用于查询单条）
     */
    private Long id;
}
