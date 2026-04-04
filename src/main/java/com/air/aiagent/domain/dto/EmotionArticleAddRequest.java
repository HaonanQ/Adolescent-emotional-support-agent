package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * 添加文章请求类
 */
@Data
public class EmotionArticleAddRequest {
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
     * 作者用户ID
     */
    private Long authorId;

    /**
     * 作者名称
     */
    private String authorName;

    /**
     * 文章分类
     */
    private String category;

    /**
     * 发布状态：0-不可见，1-可见
     */
    private Integer status;
}
