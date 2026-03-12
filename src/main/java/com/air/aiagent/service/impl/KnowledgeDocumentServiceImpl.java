package com.air.aiagent.service.impl;

import com.air.aiagent.domain.entity.KnowledgeDocument;
import com.air.aiagent.mapper.KnowledgeDocumentMapper;
import com.air.aiagent.service.KnowledgeDocumentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 知识库文档服务实现类
 * @author AI Agent
 * @since 2025/3/12
 */
@Service
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument> implements KnowledgeDocumentService {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public int countActiveByKnowledgeBaseId(Long knowledgeBaseId) {
        return knowledgeDocumentMapper.countByKnowledgeBaseId(knowledgeBaseId);
    }
}
