package com.air.aiagent.service.impl;

import com.air.aiagent.domain.entity.KnowledgeBase;
import com.air.aiagent.domain.entity.KnowledgeDocument;
import com.air.aiagent.mapper.KnowledgeBaseMapper;
import com.air.aiagent.mapper.KnowledgeDocumentMapper;
import com.air.aiagent.rag.PgVectorStoreConfig;
import com.air.aiagent.rag.TeenSupportDocumentLoader;
import com.air.aiagent.rag.MyTokenTextSplitter;
import com.air.aiagent.service.KnowledgeBaseService;
import com.air.aiagent.utils.KnowledgeFileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库管理服务实现类
 * 支持多知识库，每个知识库使用独立的向量表
 * @author AI Agent
 * @since 2025/3/12
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private JdbcTemplate pgJdbcTemplate;

    @Resource
    private EmbeddingModel dashscopeEmbeddingModel;

    // 使用@Lazy延迟加载，避免与PgVectorStoreConfig的循环依赖
    @Lazy
    @Resource
    private PgVectorStoreConfig pgVectorStoreConfig;

    @Resource
    private TeenSupportDocumentLoader documentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Override
    public List<KnowledgeBase> getActiveKnowledgeBaseList() {
        return knowledgeBaseMapper.selectAllActive();
    }

    @Override
    public List<KnowledgeBase> getAutoLoadKnowledgeBaseList() {
        return knowledgeBaseMapper.selectAutoLoadList();
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setStatus(status);
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        return updateById(knowledgeBase);
    }

    @Override
    public boolean updateAutoLoad(Long id, Integer autoLoad) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setAutoLoad(autoLoad);
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        return updateById(knowledgeBase);
    }

    @Override
    public boolean updateSyncTime(Long id, int chunkCount) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setLastSyncTime(LocalDateTime.now());
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        // 可以在这里更新其他统计信息
        return updateById(knowledgeBase);
    }

    @Override
    public boolean isNameExists(String name, Long excludeId) {
        QueryWrapper<KnowledgeBase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name);
        queryWrapper.eq("is_delete", 0);
        if (excludeId != null) {
            queryWrapper.ne("id", excludeId);
        }
        return count(queryWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createKnowledgeBase(KnowledgeBase knowledgeBase) {
        // 1. 校验名称长度
        if (knowledgeBase.getName() == null || knowledgeBase.getName().trim().isEmpty()) {
            log.error("知识库名称不能为空");
            throw new RuntimeException("知识库名称不能为空");
        }
        String name = knowledgeBase.getName().trim();
        if (name.length() > 10) {
            log.error("知识库名称长度不能超过10个字符: {}", name);
            throw new RuntimeException("知识库名称长度不能超过10个字符");
        }
        
        // 2. 检查名称是否已存在
        if (isNameExists(name, null)) {
            log.error("知识库名称已存在: {}", name);
            throw new RuntimeException("知识库名称已存在: " + name);
        }

        // 3. 设置默认值
        if (knowledgeBase.getTableName() == null || knowledgeBase.getTableName().trim().isEmpty()) {
            // 根据名称生成表名
            String tableName = "teens_" + name.toLowerCase()
                    .replaceAll("[^a-z0-9]", "_")
                    .replaceAll("_+", "_");
            knowledgeBase.setTableName(tableName);
        }
        
        // 4. 校验表名长度
        String tableName = knowledgeBase.getTableName().trim();
        if (tableName.length() > 15) {
            log.error("表名长度不能超过15个字符: {}", tableName);
            throw new RuntimeException("表名长度不能超过15个字符");
        }
        knowledgeBase.setName(name);
        knowledgeBase.setTableName(tableName);

        knowledgeBase.setStatus(1);
        knowledgeBase.setAutoLoad(0);
        knowledgeBase.setDocumentCount(0);
        knowledgeBase.setCreateTime(LocalDateTime.now());
        knowledgeBase.setUpdateTime(LocalDateTime.now());

        // 5. 保存到数据库
        boolean saved = save(knowledgeBase);
        if (!saved) {
            log.error("创建知识库失败: {}", name);
            return null;
        }

        // 6. 创建知识库文件夹
        boolean dirCreated = KnowledgeFileUtil.createKnowledgeBaseDir(name);
        if (!dirCreated) {
            log.error("创建知识库文件夹失败: {}", name);
            // 删除已创建的数据库记录
            removeById(knowledgeBase.getId());
            throw new RuntimeException("创建知识库文件夹失败");
        }

        // 7. 创建向量数据库表（提前创建，避免热更新时出错）
        try {
            pgVectorStoreConfig.createPgVectorStore(pgJdbcTemplate, dashscopeEmbeddingModel, knowledgeBase.getTableName());
            log.info("创建知识库向量表成功: {}", knowledgeBase.getTableName());
        } catch (Exception e) {
            log.error("创建知识库向量表失败: {}", knowledgeBase.getTableName(), e);
            // 删除已创建的文件夹和数据库记录
            KnowledgeFileUtil.deleteKnowledgeBaseDir(name);
            removeById(knowledgeBase.getId());
            throw new RuntimeException("创建知识库向量表失败: " + e.getMessage());
        }

        log.info("创建知识库成功: {} (ID: {}), 表名: {}, 文件夹和向量表已创建",
                name, knowledgeBase.getId(), knowledgeBase.getTableName());

        return knowledgeBase.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteKnowledgeBase(Long id) {
        KnowledgeBase knowledgeBase = getById(id);
        if (knowledgeBase == null) {
            log.warn("知识库不存在，ID: {}", id);
            return false;
        }

        String knowledgeBaseName = knowledgeBase.getName();

        // 1. 删除知识库下的所有文档记录
        QueryWrapper<KnowledgeDocument> docWrapper = new QueryWrapper<>();
        docWrapper.eq("knowledge_base_id", id);
        knowledgeDocumentMapper.delete(docWrapper);
        log.info("删除知识库 [{}] 的所有文档记录", knowledgeBaseName);

        // 2. 清空向量表
        try {
            pgVectorStoreConfig.clearVectorTable(pgJdbcTemplate, knowledgeBase.getTableName());
            log.info("清空知识库 [{}] 的向量表", knowledgeBaseName);
        } catch (Exception e) {
            log.warn("清空知识库 [{}] 的向量表失败: {}", knowledgeBaseName, e.getMessage());
        }

        // 3. 删除数据库记录
        boolean removed = removeById(id);
        if (!removed) {
            log.error("删除知识库数据库记录失败: {}", knowledgeBaseName);
            return false;
        }

        // 4. 删除知识库文件夹
        boolean dirDeleted = KnowledgeFileUtil.deleteKnowledgeBaseDir(knowledgeBaseName);
        if (!dirDeleted) {
            log.warn("删除知识库文件夹失败: {}", knowledgeBaseName);
            // 不抛出异常，因为数据库记录已删除
        }

        log.info("删除知识库成功: {} (ID: {})", knowledgeBaseName, id);
        return true;
    }

    @Override
    public boolean hotReloadKnowledgeBase(Long id) {
        log.info("开始热更新知识库，ID: {}", id);

        KnowledgeBase knowledgeBase = getById(id);
        if (knowledgeBase == null) {
            log.error("知识库不存在，ID: {}", id);
            return false;
        }

        if (knowledgeBase.getStatus() != 1) {
            log.error("知识库已停用，无法热更新，ID: {}", id);
            return false;
        }

        String tableName = knowledgeBase.getTableName();
        String knowledgeBaseName = knowledgeBase.getName();
        log.info("知识库 [{}] 使用向量表: {}", knowledgeBaseName, tableName);

        try {
            // 1. 清空该知识库的独立向量表（不会影响其他知识库）
            pgVectorStoreConfig.clearVectorTable(pgJdbcTemplate, tableName);
            log.info("已清空知识库 [{}] 的向量表: {}", knowledgeBaseName, tableName);

            // 2. 从数据库查询该知识库启用的文档列表（status=1）
            List<KnowledgeDocument> activeDocuments = knowledgeDocumentMapper.selectActiveByKnowledgeBaseId(id);
            log.info("知识库 [{}] 有 {} 个启用状态的文档", knowledgeBaseName, activeDocuments.size());

            if (activeDocuments.isEmpty()) {
                log.warn("知识库 [{}] 没有启用状态的文档，ID: {}", knowledgeBaseName, id);
                // 更新同步时间
                updateSyncTime(id, 0);
                return true;
            }

            // 3. 根据数据库记录的文件路径加载实际文档内容
            List<Document> documentList = documentLoader.loadDocumentsByFileList(activeDocuments);

            if (documentList.isEmpty()) {
                log.warn("知识库 [{}] 的文档文件加载失败或不存在", knowledgeBaseName);
                // 更新同步时间
                updateSyncTime(id, 0);
                return true;
            }

            log.info("知识库 [{}] 从文件加载到 {} 个文档块", knowledgeBaseName, documentList.size());

            // 4. 为该知识库创建独立的PgVectorStore
            PgVectorStore vectorStore = pgVectorStoreConfig.createPgVectorStore(
                    pgJdbcTemplate, dashscopeEmbeddingModel, tableName);
            log.info("已为知识库 [{}] 创建向量存储", knowledgeBaseName);

            // 5. 智能切分文档（自动识别问答格式或文章格式）
            List<Document> splitDocumentList = myTokenTextSplitter.splitSmart(documentList);
            log.info("知识库 [{}] 智能切分后文档块数量: {}", knowledgeBaseName, splitDocumentList.size());

            // 6. 分批添加到向量数据库（阿里云嵌入模型限制每批最多10个）
            int batchSize = 10;
            int totalDocs = splitDocumentList.size();
            int addedDocs = 0;
            for (int i = 0; i < totalDocs; i += batchSize) {
                int endIndex = Math.min(i + batchSize, totalDocs);
                List<Document> batch = splitDocumentList.subList(i, endIndex);
                vectorStore.add(batch);
                addedDocs += batch.size();
                log.info("知识库 [{}] 已添加 {}/{} 个文档块到向量表 [{}]", 
                        knowledgeBaseName, addedDocs, totalDocs, tableName);
            }
            log.info("知识库 [{}] 文档已成功加载到向量表 [{}]", knowledgeBaseName, tableName);

            // 7. 更新知识库同步时间和统计信息
            updateSyncTime(id, splitDocumentList.size());
            log.info("知识库 [{}] 热更新完成，共加载 {} 个文档块", knowledgeBaseName, splitDocumentList.size());

            return true;
        } catch (Exception e) {
            log.error("知识库 [{}] 热更新失败，ID: {}, 表名: {}", knowledgeBaseName, id, tableName, e);
            return false;
        }
    }
}
