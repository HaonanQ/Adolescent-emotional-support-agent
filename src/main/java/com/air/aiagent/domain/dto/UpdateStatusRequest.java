package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * 修改性别请求
 */
@Data
public class UpdateStatusRequest {
    /**
     * 性别：0-女生，1-男生
     */
    private Integer sex;
}
