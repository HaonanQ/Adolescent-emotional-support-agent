package com.air.aiagent.service;

import com.air.aiagent.domain.dto.EmotionDiaryAddRequest;
import com.air.aiagent.domain.dto.EmotionDiaryQueryRequest;
import com.air.aiagent.domain.entity.EmotionDiary;
import com.air.aiagent.domain.vo.EmotionDiaryVO;
import com.air.aiagent.domain.vo.EmotionHistoryVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 针对表【emotion_diary(情绪日记表)】的数据库操作Service
 */
public interface EmotionDiaryService extends IService<EmotionDiary> {

    /**
     * Entity转换为VO
     */
    EmotionDiaryVO entityToVO(EmotionDiary emotionDiary);

    /**
     * 添加情绪日记
     */
    Long addEmotionDiary(EmotionDiaryAddRequest request);

    /**
     * 查询单条情绪日记（需验证userId）
     */
    EmotionDiaryVO getEmotionDiaryById(Long id, Long userId);

    /**
     * 查询用户的所有情绪日记
     */
    List<EmotionDiaryVO> getEmotionDiaryListByUserId(Long userId);

    /**
     * 删除情绪日记
     */
    Boolean deleteEmotionDiary(Long id, Long userId);

    /**
     * 获取用户最新一条情绪记录
     */
    EmotionDiary getLatestEmotionByUserId(Long userId);

    /**
     * 获取用户情绪历史记录（按时间正序，用于展示变化趋势）
     */
    List<EmotionHistoryVO> getEmotionHistoryByUserId(Long userId);
}
