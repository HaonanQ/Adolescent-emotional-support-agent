package com.air.aiagent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.air.aiagent.domain.dto.EmotionDiaryAddRequest;
import com.air.aiagent.domain.entity.EmotionDiary;
import com.air.aiagent.domain.vo.EmotionDiaryVO;
import com.air.aiagent.domain.vo.EmotionHistoryVO;
import com.air.aiagent.exception.BusinessException;
import com.air.aiagent.exception.ErrorCode;
import com.air.aiagent.mapper.EmotionDiaryMapper;
import com.air.aiagent.service.EmotionDiaryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 针对表【emotion_diary(情绪日记表)】的数据库操作Service实现
 */
@Service
public class EmotionDiaryServiceImpl extends ServiceImpl<EmotionDiaryMapper, EmotionDiary> implements EmotionDiaryService {

    /**
     * Entity转换为VO
     */
    @Override
    public EmotionDiaryVO entityToVO(EmotionDiary emotionDiary) {
        return BeanUtil.copyProperties(emotionDiary, EmotionDiaryVO.class);
    }

    /**
     * 添加情绪日记
     */
    @Override
    public Long addEmotionDiary(EmotionDiaryAddRequest request) {
        if (request.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题不能为空");
        }
        if (request.getMood() == null || request.getMood().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "情绪不能为空");
        }
        if (request.getMoodScore() == null || request.getMoodScore() < 1 || request.getMoodScore() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "情绪分数必须在1-10之间");
        }

        EmotionDiary emotionDiary = EmotionDiary.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .mood(request.getMood())
                .moodScore(request.getMoodScore())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .createTime(new Date())
                .build();

        boolean saved = save(emotionDiary);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加情绪日记失败");
        }

        return emotionDiary.getId();
    }

    /**
     * 查询单条情绪日记（需验证userId）
     */
    @Override
    public EmotionDiaryVO getEmotionDiaryById(Long id, Long userId) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日记ID不能为空");
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        EmotionDiary emotionDiary = getById(id);
        if (emotionDiary == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "日记不存在");
        }

        if (!emotionDiary.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该日记");
        }

        return entityToVO(emotionDiary);
    }

    /**
     * 查询用户的所有情绪日记
     */
    @Override
    public List<EmotionDiaryVO> getEmotionDiaryListByUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        LambdaQueryWrapper<EmotionDiary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmotionDiary::getUserId, userId)
                .orderByDesc(EmotionDiary::getCreateTime);

        List<EmotionDiary> list = list(queryWrapper);
        return list.stream().map(this::entityToVO).collect(Collectors.toList());
    }

    /**
     * 删除情绪日记
     */
    @Override
    public Boolean deleteEmotionDiary(Long id, Long userId) {
        if (id == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        EmotionDiary emotionDiary = getById(id);
        if (emotionDiary == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "日记不存在");
        }

        if (!emotionDiary.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该日记");
        }

        return removeById(id);
    }

    /**
     * 获取用户最新一条情绪记录
     */
    @Override
    public EmotionDiary getLatestEmotionByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        LambdaQueryWrapper<EmotionDiary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmotionDiary::getUserId, userId)
                .orderByDesc(EmotionDiary::getCreateTime)
                .last("LIMIT 1");
        return getOne(queryWrapper);
    }

    /**
     * 获取用户情绪历史记录（按时间正序，用于展示变化趋势）
     */
    @Override
    public List<EmotionHistoryVO> getEmotionHistoryByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        LambdaQueryWrapper<EmotionDiary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmotionDiary::getUserId, userId)
                .orderByAsc(EmotionDiary::getCreateTime);

        List<EmotionDiary> list = list(queryWrapper);
        return list.stream().map(diary -> {
            EmotionHistoryVO vo = new EmotionHistoryVO();
            vo.setId(diary.getId());
            vo.setMood(diary.getMood());
            vo.setMoodScore(diary.getMoodScore());
            vo.setDiaryDate(diary.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }
}
