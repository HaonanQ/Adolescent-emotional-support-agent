package com.air.aiagent.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户查询请求DTO
 */
@Data
public class UserQueryRequest implements Serializable {

    /**
     * 搜索关键词（用户名/昵称）
     */
    private String keyword;

    /**
     * 账号状态：0-已启用，1-已停用，不传则查全部
     */
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
