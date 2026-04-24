package com.air.aiagent.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * 反馈VO
 */
@Data
public class FeedbackVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String userNickname;

    /**
     * 反馈标题
     */
    private String title;

    /**
     * 反馈内容
     */
    private String content;

    /**
     * 反馈类型
     */
    private String type;

    /**
     * 处理状态
     */
    private String status;

    /**
     * 管理员回复
     */
    private String adminReply;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
