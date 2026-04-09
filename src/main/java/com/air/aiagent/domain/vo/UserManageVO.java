package com.air.aiagent.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户管理信息VO（管理员查看用户列表用）
 */
@Data
public class UserManageVO implements Serializable {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 注册时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 最新情绪分数（无日记时为null）
     */
    private Integer latestMoodScore;

    /**
     * 最新情绪状态（无日记时为null）
     */
    private String latestMood;

    /**
     * 最新情绪日记日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date latestDiaryTime;

    /**
     * 情绪日记总数
     */
    private Integer diaryCount;

    /**
     * 是否为管理员：0-普通用户，1-管理员
     */
    private Integer isAdmin;

    /**
     * 性别：0-女生，1-男生
     */
    private Integer sex;

    /**
     * 账号状态：0-已启用，1-已停用
     */
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
