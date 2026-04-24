package com.air.aiagent.service;

import com.air.aiagent.common.BaseResponse;
import com.air.aiagent.domain.dto.FeedbackAddRequest;
import com.air.aiagent.domain.dto.FeedbackQueryRequest;
import com.air.aiagent.domain.dto.FeedbackReplyRequest;
import com.air.aiagent.domain.entity.Feedback;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 反馈服务接口
 */
public interface FeedbackService extends IService<Feedback> {

    /**
     * 添加反馈
     */
    Long addFeedback(FeedbackAddRequest request, Long userId);

    /**
     * 查询反馈列表（管理员）
     */
    BaseResponse<?> listFeedback(FeedbackQueryRequest request);

    /**
     * 查询用户的反馈列表
     */
    BaseResponse<?> listUserFeedback(Long userId, FeedbackQueryRequest request);

    /**
     * 回复反馈（管理员）
     */
    Boolean replyFeedback(FeedbackReplyRequest request);

    /**
     * 删除反馈
     */
    Boolean deleteFeedback(Long id, Long userId);
}
