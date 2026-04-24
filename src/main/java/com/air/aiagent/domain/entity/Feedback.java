package com.air.aiagent.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

/**
 * 用户反馈实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("feedback")
public class Feedback implements Serializable {

    /**
     * 主键ID
     */
    @Id
    @TableId(type = IdType.AUTO)
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
     * 反馈类型：suggestion-建议，problem-问题，other-其他
     */
    private String type;

    /**
     * 处理状态：pending-待处理，processed-已处理，ignored-已忽略
     */
    private String status;

    /**
     * 管理员回复
     */
    private String adminReply;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer isDeleted;
}
