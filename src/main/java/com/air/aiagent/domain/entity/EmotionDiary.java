package com.air.aiagent.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 情绪日记表
 * @TableName emotion_diary
 */
@TableName(value ="emotion_diary")
@Data
@Builder
public class EmotionDiary implements Serializable {
    /**
     * 日记ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
