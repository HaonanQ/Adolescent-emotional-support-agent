package com.air.aiagent.controller;

import com.air.aiagent.annotation.LoginCheck;
import com.air.aiagent.common.BaseResponse;
import com.air.aiagent.common.ResultUtils;
import com.air.aiagent.domain.dto.EmotionArticleAddRequest;
import com.air.aiagent.domain.dto.EmotionArticleQueryRequest;
import com.air.aiagent.domain.dto.EmotionArticleUpdateRequest;
import com.air.aiagent.domain.entity.User;
import com.air.aiagent.domain.vo.EmotionArticleVO;
import com.air.aiagent.service.EmotionArticleService;
import com.air.aiagent.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/emotionArticle")
@Tag(name = "情感课堂接口", description = "情感课堂文章管理")
public class EmotionArticleController {

    @Autowired
    private EmotionArticleService emotionArticleService;

    @Resource
    private UserService userService;

    /**
     * 用户端：获取可见文章列表
     */
    @LoginCheck
    @PostMapping("/list")
    public BaseResponse<List<EmotionArticleVO>> getVisibleArticleList() {
        List<EmotionArticleVO> list = emotionArticleService.getVisibleArticleList();
        return ResultUtils.success(list);
    }

    /**
     * 用户端：查看文章详情（增加阅读次数）
     */
    @LoginCheck
    @PostMapping("/detail")
    public BaseResponse<EmotionArticleVO> getArticleDetail(@RequestBody EmotionArticleQueryRequest request) {
        EmotionArticleVO vo = emotionArticleService.getArticleByIdForUser(request.getId());
        emotionArticleService.incrementReadCount(request.getId());
        vo.setReadCount(vo.getReadCount() + 1);
        return ResultUtils.success(vo);
    }

    /**
     * 管理员：获取所有文章列表（含不可见）
     */
    @LoginCheck
    @PostMapping("/admin/list")
    public BaseResponse<List<EmotionArticleVO>> getAllArticleList(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        checkAdmin(loginUser);
        List<EmotionArticleVO> list = emotionArticleService.getAllArticleList();
        return ResultUtils.success(list);
    }

    /**
     * 管理员：查看文章详情（含不可见）
     */
    @LoginCheck
    @PostMapping("/admin/detail")
    public BaseResponse<EmotionArticleVO> getArticleDetailForAdmin(@RequestBody EmotionArticleQueryRequest request,
                                                                    HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        checkAdmin(loginUser);
        EmotionArticleVO vo = emotionArticleService.getArticleByIdForAdmin(request.getId());
        return ResultUtils.success(vo);
    }

    /**
     * 管理员：创建新文章
     */
    @LoginCheck
    @PostMapping("/admin/add")
    public BaseResponse<Long> addArticle(@RequestBody EmotionArticleAddRequest request,
                                         HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        checkAdmin(loginUser);
        request.setAuthorId(loginUser.getId());
        request.setAuthorName(loginUser.getUsername());
        Long articleId = emotionArticleService.addArticle(request);
        return ResultUtils.success(articleId);
    }

    /**
     * 管理员：更新文章
     */
    @LoginCheck
    @PostMapping("/admin/update")
    public BaseResponse<Boolean> updateArticle(@RequestBody EmotionArticleUpdateRequest request,
                                               HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        checkAdmin(loginUser);
        Boolean result = emotionArticleService.updateArticle(request);
        return ResultUtils.success(result);
    }

    /**
     * 管理员：删除文章
     */
    @LoginCheck
    @PostMapping("/admin/delete")
    public BaseResponse<Boolean> deleteArticle(@RequestBody EmotionArticleQueryRequest request,
                                               HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        checkAdmin(loginUser);
        Boolean result = emotionArticleService.deleteArticle(request.getId());
        return ResultUtils.success(result);
    }

    /**
     * 校验用户是否为管理员
     */
    private void checkAdmin(User user) {
        if (user.getIsAdmin() == null || user.getIsAdmin() != 1) {
            throw new com.air.aiagent.exception.BusinessException(
                    com.air.aiagent.exception.ErrorCode.NO_AUTH_ERROR, "无管理员权限"
            );
        }
    }
}
