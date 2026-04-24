package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * 反馈查询请求DTO
 */
@Data
public class FeedbackQueryRequest {

    /**
     * 当前页码
     */
    private Integer current = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 处理状态（可选）
     */
    private String status;

    /**
     * 反馈类型（可选）
     */
    private String type;
}
