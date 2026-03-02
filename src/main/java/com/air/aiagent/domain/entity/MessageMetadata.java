package com.air.aiagent.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 消息元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageMetadata {

    /**
     * token 消耗数
     */
    private Integer tokenCount;

    /**
     * 响应时间
     */
    private Integer responseTimeMs;

    /**
     * 推荐的商品ID列表
     */
    private List<Long> recommendedProductIds;

    /**
     * 生成的PDF文件URL
     */
    private String pdfFileUrl;

    /**
     * PDF文件名
     */
    private String pdfFileName;
}
