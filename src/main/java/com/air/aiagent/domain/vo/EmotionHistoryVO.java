package com.air.aiagent.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户情绪历史记录VO（展示情绪变化趋势）
 */
@Data
public class EmotionHistoryVO implements Serializable {

    /**
     * 日记ID
     */
    private Long id;

    /**
     * 情绪状态名称
     */
    private String mood;

    /**
     * 情绪分数
     */
    private Integer moodScore;

    /**
     * 日记日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date diaryDate;

    private static final long serialVersionUID = 1L;
}
