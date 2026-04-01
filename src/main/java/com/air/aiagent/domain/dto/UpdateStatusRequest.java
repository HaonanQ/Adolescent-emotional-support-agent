package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * 修改状态请求
 */
@Data
public class UpdateStatusRequest {
    /**
     * 恋爱状态：0-单身，1-恋爱中
     */
    private Integer relationshipStatus;
}
