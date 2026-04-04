package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * 更新文章请求类
 */
@Data
public class EmotionArticleUpdateRequest {
    /**
     * 文章ID
     */
    private Long id;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章内容（HTML富文本）
     */
    private String content;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 文章分类
     */
    private String category;

    /**
     * 发布状态：0-不可见，1-可见
     */
    private Integer status;

    /**
     * 排序权重
     */
    private Integer sortOrder;
}
