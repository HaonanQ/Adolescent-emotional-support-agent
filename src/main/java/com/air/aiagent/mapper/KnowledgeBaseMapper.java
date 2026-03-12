package com.air.aiagent.mapper;

import com.air.aiagent.domain.entity.KnowledgeBase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库管理Mapper接口
 * @author AI Agent
 * @since 2025/3/12
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    /**
     * 查询所有启用的知识库
     * @return 知识库列表
     */
    @Select("SELECT * FROM knowledge_base WHERE status = 1 AND is_delete = 0")
    List<KnowledgeBase> selectAllActive();

    /**
     * 查询启动时需要自动加载的知识库
     * @return 知识库列表
     */
    @Select("SELECT * FROM knowledge_base WHERE status = 1 AND auto_load = 1 AND is_delete = 0")
    List<KnowledgeBase> selectAutoLoadList();
}
