package com.air.aiagent.controller;

import com.air.aiagent.annotation.LoginCheck;
import com.air.aiagent.common.BaseResponse;
import com.air.aiagent.common.ResultUtils;
import com.air.aiagent.domain.dto.FeedbackAddRequest;
import com.air.aiagent.domain.dto.FeedbackQueryRequest;
import com.air.aiagent.domain.dto.FeedbackReplyRequest;
import com.air.aiagent.domain.entity.User;
import com.air.aiagent.service.FeedbackService;
import com.air.aiagent.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 反馈控制器
 */
@Slf4j
@RestController
@RequestMapping("/feedback")
@Tag(name = "反馈接口", description = "用户反馈与建议")
public class FeedbackController {

    @Resource
    private FeedbackService feedbackService;

    @Resource
    private UserService userService;

    /**
     * 添加反馈
     */
    @LoginCheck
    @PostMapping("/add")
    public BaseResponse<Long> addFeedback(@RequestBody FeedbackAddRequest request, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        Long feedbackId = feedbackService.addFeedback(request, loginUser.getId());
        return ResultUtils.success(feedbackId);
    }

    /**
     * 查询用户的反馈列表
     */
    @LoginCheck
    @PostMapping("/list/user")
    public BaseResponse<?> listUserFeedback(@RequestBody FeedbackQueryRequest request, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return feedbackService.listUserFeedback(loginUser.getId(), request);
    }

    /**
     * 删除反馈
     */
    @LoginCheck
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteFeedback(@RequestBody FeedbackReplyRequest request, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        Boolean result = feedbackService.deleteFeedback(request.getId(), loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 查询所有反馈列表（管理员）
     */
    @LoginCheck
    @PostMapping("/admin/list")
    public BaseResponse<?> listFeedback(@RequestBody FeedbackQueryRequest request, HttpServletRequest httpServletRequest) {
        userService.getLoginUser(httpServletRequest);
        return feedbackService.listFeedback(request);
    }

    /**
     * 回复反馈（管理员）
     */
    @LoginCheck
    @PostMapping("/admin/reply")
    public BaseResponse<Boolean> replyFeedback(@RequestBody FeedbackReplyRequest request, HttpServletRequest httpServletRequest) {
        userService.getLoginUser(httpServletRequest);
        Boolean result = feedbackService.replyFeedback(request);
        return ResultUtils.success(result);
    }
}
