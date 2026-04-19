package com.air.aiagent.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 情绪日记响应类
 */
@Data
public class EmotionDiaryVO {
    /**
     * 日记ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 日记标题
     */
    private String title;

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

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date updateTime;
}
