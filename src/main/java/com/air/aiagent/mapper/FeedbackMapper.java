package com.air.aiagent.mapper;

import com.air.aiagent.domain.entity.Feedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 反馈 Mapper 接口
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
}
