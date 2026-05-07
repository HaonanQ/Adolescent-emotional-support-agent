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
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "添加反馈", description = "用户提交反馈或建议")
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
    @Operation(summary = "查询用户反馈列表", description = "获取当前用户提交的所有反馈记录")
    @PostMapping("/list/user")
    public BaseResponse<?> listUserFeedback(@RequestBody FeedbackQueryRequest request, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return feedbackService.listUserFeedback(loginUser.getId(), request);
    }

    /**
     * 删除反馈
     */
    @LoginCheck
    @Operation(summary = "删除反馈", description = "用户删除自己提交的反馈")
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
    @Operation(summary = "查询所有反馈列表", description = "管理员查询所有用户的反馈记录")
    @PostMapping("/admin/list")
    public BaseResponse<?> listFeedback(@RequestBody FeedbackQueryRequest request, HttpServletRequest httpServletRequest) {
        userService.getLoginUser(httpServletRequest);
        return feedbackService.listFeedback(request);
    }

    /**
     * 回复反馈（管理员）
     */
    @LoginCheck
    @Operation(summary = "回复反馈", description = "管理员回复用户反馈")
    @PostMapping("/admin/reply")
    public BaseResponse<Boolean> replyFeedback(@RequestBody FeedbackReplyRequest request, HttpServletRequest httpServletRequest) {
        userService.getLoginUser(httpServletRequest);
        Boolean result = feedbackService.replyFeedback(request);
        return ResultUtils.success(result);
    }
}
