package com.air.aiagent.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库管理实体类
 * @author AI Agent
 * @since 2025/3/12
 */
@Data
@TableName(value = "knowledge_base")
public class KnowledgeBase implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 向量数据库表名
     */
    private String tableName;

    /**
     * 状态：0-停用，1-启用
     */
    private Integer status;

    /**
     * 启动时自动加载：0-否，1-是
     */
    private Integer autoLoad;

    /**
     * 文档数量
     */
    private Integer documentCount;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除：0-否，1-是
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
