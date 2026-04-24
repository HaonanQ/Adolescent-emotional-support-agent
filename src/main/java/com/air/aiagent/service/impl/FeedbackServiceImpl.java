package com.air.aiagent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.air.aiagent.common.BaseResponse;
import com.air.aiagent.exception.ErrorCode;
import com.air.aiagent.common.ResultUtils;
import com.air.aiagent.domain.dto.FeedbackAddRequest;
import com.air.aiagent.domain.dto.FeedbackQueryRequest;
import com.air.aiagent.domain.dto.FeedbackReplyRequest;
import com.air.aiagent.domain.entity.Feedback;
import com.air.aiagent.domain.entity.User;
import com.air.aiagent.domain.vo.FeedbackVO;
import com.air.aiagent.exception.BusinessException;
import com.air.aiagent.mapper.FeedbackMapper;
import com.air.aiagent.service.FeedbackService;
import com.air.aiagent.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 反馈服务实现类
 */
@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {

    @Resource
    private UserService userService;

    @Override
    public Long addFeedback(FeedbackAddRequest request, Long userId) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题不能为空");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容不能为空");
        }

        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        Feedback feedback = Feedback.builder()
                .userId(userId)
                .userNickname(user.getNickname())
                .title(request.getTitle())
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : "suggestion")
                .status("pending")
                .createTime(new Date())
                .updateTime(new Date())
                .isDeleted(0)
                .build();

        this.save(feedback);
        return feedback.getId();
    }

    @Override
    public BaseResponse<?> listFeedback(FeedbackQueryRequest request) {
        QueryWrapper<Feedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", 0);
        
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            queryWrapper.eq("status", request.getStatus());
        }
        if (request.getType() != null && !request.getType().isEmpty()) {
            queryWrapper.eq("type", request.getType());
        }
        
        queryWrapper.orderByDesc("create_time");

        Page<Feedback> page = new Page<>(request.getCurrent(), request.getPageSize());
        Page<Feedback> feedbackPage = this.page(page, queryWrapper);

        Page<FeedbackVO> voPage = new Page<>();
        voPage.setCurrent(feedbackPage.getCurrent());
        voPage.setSize(feedbackPage.getSize());
        voPage.setTotal(feedbackPage.getTotal());
        voPage.setRecords(feedbackPage.getRecords().stream()
                .map(feedback -> BeanUtil.copyProperties(feedback, FeedbackVO.class))
                .toList());

        return ResultUtils.success(voPage);
    }

    @Override
    public BaseResponse<?> listUserFeedback(Long userId, FeedbackQueryRequest request) {
        QueryWrapper<Feedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("is_deleted", 0);
        queryWrapper.orderByDesc("create_time");

        Page<Feedback> page = new Page<>(request.getCurrent(), request.getPageSize());
        Page<Feedback> feedbackPage = this.page(page, queryWrapper);

        Page<FeedbackVO> voPage = new Page<>();
        voPage.setCurrent(feedbackPage.getCurrent());
        voPage.setSize(feedbackPage.getSize());
        voPage.setTotal(feedbackPage.getTotal());
        voPage.setRecords(feedbackPage.getRecords().stream()
                .map(feedback -> BeanUtil.copyProperties(feedback, FeedbackVO.class))
                .toList());

        return ResultUtils.success(voPage);
    }

    @Override
    public Boolean replyFeedback(FeedbackReplyRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "反馈ID不能为空");
        }

        Feedback feedback = this.getById(request.getId());
        if (feedback == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "反馈不存在");
        }

        feedback.setAdminReply(request.getAdminReply());
        feedback.setStatus(request.getStatus() != null ? request.getStatus() : "processed");
        feedback.setUpdateTime(new Date());

        return this.updateById(feedback);
    }

    @Override
    public Boolean deleteFeedback(Long id, Long userId) {
        Feedback feedback = this.getById(id);
        if (feedback == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "反馈不存在");
        }
        if (!feedback.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除");
        }

        feedback.setIsDeleted(1);
        feedback.setUpdateTime(new Date());
        return this.updateById(feedback);
    }
}
