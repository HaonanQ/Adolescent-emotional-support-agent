package com.air.aiagent.controller;

import com.air.aiagent.annotation.LoginCheck;
import com.air.aiagent.common.BaseResponse;
import com.air.aiagent.common.ResultUtils;
import com.air.aiagent.domain.dto.UserQueryRequest;
import com.air.aiagent.domain.entity.EmotionDiary;
import com.air.aiagent.domain.entity.User;
import com.air.aiagent.domain.vo.EmotionHistoryVO;
import com.air.aiagent.domain.vo.UserManageVO;
import com.air.aiagent.exception.BusinessException;
import com.air.aiagent.exception.ErrorCode;
import com.air.aiagent.service.EmotionDiaryService;
import com.air.aiagent.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员接口
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "管理员接口", description = "管理员用户管理相关接口")
public class AdminController {

    @Resource
    private UserService userService;

    @Resource
    private EmotionDiaryService emotionDiaryService;

    /**
     * 获取所有用户列表（含最新情绪分数）
     */
    @LoginCheck
    @PostMapping("/user/list")
    public BaseResponse<List<UserManageVO>> getUserList(@RequestBody(required = false) UserQueryRequest queryRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser.getIsAdmin() == null || loginUser.getIsAdmin() != 1) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (queryRequest != null) {
            if (queryRequest.getKeyword() != null && !queryRequest.getKeyword().isEmpty()) {
                queryWrapper.and(w -> w
                        .like(User::getUsername, queryRequest.getKeyword())
                        .or()
                        .like(User::getNickname, queryRequest.getKeyword())
                );
            }
            if (queryRequest.getIsDeleted() != null) {
                queryWrapper.eq(User::getIsDeleted, queryRequest.getIsDeleted());
            }
        }
        if (queryRequest == null || queryRequest.getIsDeleted() == null) {
            queryWrapper.eq(User::getIsDeleted, 0);
        }
        queryWrapper.orderByDesc(User::getCreateTime);
        List<User> userList = userService.list(queryWrapper);
        List<UserManageVO> voList = userList.stream().map(user -> {
            UserManageVO vo = new UserManageVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setCreateTime(user.getCreateTime());
            vo.setIsAdmin(user.getIsAdmin());
            vo.setIsDeleted(user.getIsDeleted());
            EmotionDiary latest = emotionDiaryService.getLatestEmotionByUserId(user.getId());
            if (latest != null) {
                vo.setLatestMoodScore(latest.getMoodScore());
                vo.setLatestMood(latest.getMood());
                vo.setLatestDiaryTime(latest.getCreateTime());
            }
            long count = emotionDiaryService.count(
                    new LambdaQueryWrapper<EmotionDiary>().eq(EmotionDiary::getUserId, user.getId())
            );
            vo.setDiaryCount((int) count);
            return vo;
        }).toList();
        return ResultUtils.success(voList);
    }

    /**
     * 启用/停用用户账号
     */
    @LoginCheck
    @PostMapping("/user/toggleStatus")
    public BaseResponse<Boolean> toggleUserStatus(@RequestParam("userId") Long userId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser.getIsAdmin() == null || loginUser.getIsAdmin() != 1) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }
        User targetUser = userService.getById(userId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (targetUser.getIsAdmin() != null && targetUser.getIsAdmin() == 1) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无法操作管理员账号");
        }
        int newStatus = (targetUser.getIsDeleted() == null || targetUser.getIsDeleted() == 0) ? 1 : 0;
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId).set(User::getIsDeleted, newStatus);
        boolean success = userService.update(updateWrapper);
        return ResultUtils.success(success);
    }

    /**
     * 获取指定用户的情绪历史记录（按时间正序）
     */
    @LoginCheck
    @PostMapping("/user/emotion/history")
    public BaseResponse<List<EmotionHistoryVO>> getEmotionHistory(@RequestParam("userId") Long userId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser.getIsAdmin() == null || loginUser.getIsAdmin() != 1) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }
        List<EmotionHistoryVO> history = emotionDiaryService.getEmotionHistoryByUserId(userId);
        return ResultUtils.success(history);
    }
}
