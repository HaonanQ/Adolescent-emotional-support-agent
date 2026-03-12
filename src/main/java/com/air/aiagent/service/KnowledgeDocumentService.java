package com.air.aiagent.service;

import com.air.aiagent.domain.entity.KnowledgeDocument;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 知识库文档服务接口
 * @author AI Agent
 * @since 2025/3/12
 */
public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {

    /**
     * 根据知识库ID统计启用的文档数量
     * @param knowledgeBaseId 知识库ID
     * @return 文档数量
     */
    int countActiveByKnowledgeBaseId(Long knowledgeBaseId);
}
