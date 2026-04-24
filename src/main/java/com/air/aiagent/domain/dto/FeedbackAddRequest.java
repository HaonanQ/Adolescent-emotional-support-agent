package com.air.aiagent.domain.dto;

import lombok.Data;

/**
 * 反馈添加请求DTO
 */
@Data
public class FeedbackAddRequest {

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
}
