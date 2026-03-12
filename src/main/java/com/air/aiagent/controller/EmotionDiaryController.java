package com.air.aiagent.controller;

import com.air.aiagent.annotation.LoginCheck;
import com.air.aiagent.common.BaseResponse;
import com.air.aiagent.common.ResultUtils;
import com.air.aiagent.domain.dto.EmotionDiaryAddRequest;
import com.air.aiagent.domain.dto.EmotionDiaryQueryRequest;
import com.air.aiagent.domain.entity.User;
import com.air.aiagent.domain.vo.EmotionDiaryVO;
import com.air.aiagent.manage.CosManager;
import com.air.aiagent.service.EmotionDiaryService;
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
@RequestMapping("/emotionDiary")
@Tag(name = "情绪日记接口", description = "用户端编写情绪日记")
public class EmotionDiaryController {
    @Autowired
    private EmotionDiaryService emotionDiaryService;

    @Resource
    private UserService userService;
    /**
     * 添加情绪日记
     */
    @LoginCheck
    @PostMapping("/add")
    public BaseResponse<Long> addEmotionDiary(@RequestBody EmotionDiaryAddRequest request, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        request.setUserId(loginUser.getId());
        Long diaryId = emotionDiaryService.addEmotionDiary(request);
        return ResultUtils.success(diaryId);
    }

    /**
     * 查询单条情绪日记（需验证userId）
     */
    @LoginCheck
    @PostMapping("/getById")
    public BaseResponse<EmotionDiaryVO> getEmotionDiaryById(@RequestBody EmotionDiaryQueryRequest request, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        EmotionDiaryVO diaryVO = emotionDiaryService.getEmotionDiaryById(request.getId(), loginUser.getId());
        return ResultUtils.success(diaryVO);
    }

    /**
     * 查询用户的所有情绪日记
     */
    @LoginCheck
    @PostMapping("/list")
    public BaseResponse<List<EmotionDiaryVO>> getEmotionDiaryList(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        List<EmotionDiaryVO> list = emotionDiaryService.getEmotionDiaryListByUserId(loginUser.getId());
        return ResultUtils.success(list);
    }

    /**
     * 删除情绪日记
     */
    @LoginCheck
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteEmotionDiary(@RequestBody EmotionDiaryQueryRequest request, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        Boolean result = emotionDiaryService.deleteEmotionDiary(request.getId(), loginUser.getId());
        return ResultUtils.success(result);
    }
}
