package com.air.aiagent.mapper;

import com.air.aiagent.domain.entity.KnowledgeDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库文档Mapper接口
 * @author AI Agent
 * @since 2025/3/12
 */
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {

    /**
     * 根据知识库ID查询启用的文档列表
     * @param knowledgeBaseId 知识库ID
     * @return 文档列表
     */
    @Select("SELECT * FROM knowledge_document WHERE knowledge_base_id = #{knowledgeBaseId} AND status = 1 AND is_delete = 0")
    List<KnowledgeDocument> selectActiveByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

    /**
     * 统计知识库的文档数量
     * @param knowledgeBaseId 知识库ID
     * @return 文档数量
     */
    @Select("SELECT COUNT(*) FROM knowledge_document WHERE knowledge_base_id = #{knowledgeBaseId} AND status = 1 AND is_delete = 0")
    int countByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);
}
