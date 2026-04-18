package com.air.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * PgVector向量数据库配置类
 * 负责创建和管理VectorStore实例
 * 每个知识库使用独立的向量表
 * @author Qiuhaonan
 * @since 2025/8/11 下午8:45
 */
@Slf4j
@Configuration
public class PgVectorStoreConfig {

    /**
     * 默认知识库表名
     */
    @Value("${knowledge.base.default-table:teens_rag}")
    private String defaultTableName;

    // 缓存知识库对应的VectorStore，避免重复创建
    private final Map<String, VectorStore> vectorStoreCache = new ConcurrentHashMap<>();

    /**
     * 初始化默认的PgVectorStore Bean
     * 用于兼容旧代码和默认知识库
     *
     * @param pgJdbcTemplate pg向量数据库JdbcTemplate
     * @param dashscopeEmbeddingModel 嵌入模型
     * @return VectorStore实例
     */
    @Bean
    public VectorStore pgVectorVectorStore(@Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbcTemplate,
            EmbeddingModel dashscopeEmbeddingModel) {

        log.info("创建默认PgVectorStore Bean，表名: {}", defaultTableName);

        // 创建默认的PgVectorStore
        PgVectorStore pgVectorStore = createPgVectorStore(pgJdbcTemplate, dashscopeEmbeddingModel, defaultTableName);

        log.info("PgVectorStore Bean创建完成，表名: {}", defaultTableName);
        log.info("提示：知识库文档加载请通过管理接口调用热更新，或设置 knowledge.base.auto-load=true");

        return pgVectorStore;
    }

    /**
     * 创建指定表名的PgVectorStore
     *
     * @param pgJdbcTemplate pg向量数据库JdbcTemplate
     * @param embeddingModel 嵌入模型
     * @param tableName 向量表名
     * @return PgVectorStore实例
     */
    public PgVectorStore createPgVectorStore(JdbcTemplate pgJdbcTemplate,
                                              EmbeddingModel embeddingModel,
                                              String tableName) {
        log.info("创建PgVectorStore，表名: {}", tableName);

        // 先创建表（如果不存在）
        createVectorTableIfNotExists(pgJdbcTemplate, tableName);

        return PgVectorStore.builder(pgJdbcTemplate, embeddingModel)
                .dimensions(1536)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(false) // 我们手动创建表
                .schemaName("public")
                .vectorTableName(tableName)
                .maxDocumentBatchSize(10000)
                .build();
    }

    /**
     * 创建向量表（如果不存在）
     *
     * @param pgJdbcTemplate pg向量数据库JdbcTemplate
     * @param tableName 表名
     */
    private void createVectorTableIfNotExists(JdbcTemplate pgJdbcTemplate, String tableName) {
        try {
            // 检查表是否存在
            String checkSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?";
            Integer count = pgJdbcTemplate.queryForObject(checkSql, Integer.class, tableName);

            if (count == null || count == 0) {
                log.info("向量表 {} 不存在，开始创建...", tableName);
                // ========== 新增：先创建必要的扩展 ==========
                // 创建uuid-ossp扩展（解决uuid_generate_v4()函数缺失）
                pgJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
                // 创建pgvector扩展（解决vector类型缺失）
                pgJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
                log.info("PostgreSQL扩展 uuid-ossp 和 vector 创建/验证成功");
                // 创建向量表
                String createSql = String.format(
                    "CREATE TABLE IF NOT EXISTS %s (" +
                    "id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, " +
                    "content TEXT, " +
                    "metadata JSON, " +
                    "embedding vector(1536)" +
                    ")", tableName);

                pgJdbcTemplate.execute(createSql);

                // 创建向量索引
                String indexSql = String.format(
                    "CREATE INDEX IF NOT EXISTS idx_%s_embedding ON %s USING hnsw (embedding vector_cosine_ops)",
                    tableName, tableName);
                pgJdbcTemplate.execute(indexSql);

                log.info("向量表 {} 创建成功", tableName);
            } else {
                log.debug("向量表 {} 已存在，检查并升级索引", tableName);
                upgradeIndexToHnsw(pgJdbcTemplate, tableName);
            }
        } catch (Exception e) {
            log.error("创建向量表 {} 失败", tableName, e);
            throw new RuntimeException("创建向量表失败: " + tableName, e);
        }
    }

    /**
     * 将已有的ivfflat索引升级为hnsw索引
     *
     * @param pgJdbcTemplate pg向量数据库JdbcTemplate
     * @param tableName 表名
     */
    private void upgradeIndexToHnsw(JdbcTemplate pgJdbcTemplate, String tableName) {
        try {
            String indexName = "idx_" + tableName + "_embedding";
            String checkIndexSql = "SELECT indexdef FROM pg_indexes WHERE tablename = ? AND indexname = ?";
            String indexDef = pgJdbcTemplate.queryForObject(checkIndexSql, String.class, tableName, indexName);

            if (indexDef != null && indexDef.toLowerCase().contains("ivfflat")) {
                log.info("检测到表 {} 使用ivfflat索引，正在升级为hnsw索引...", tableName);
                pgJdbcTemplate.execute("DROP INDEX IF EXISTS " + indexName);
                String indexSql = String.format(
                    "CREATE INDEX %s ON %s USING hnsw (embedding vector_cosine_ops)",
                    indexName, tableName);
                pgJdbcTemplate.execute(indexSql);
                log.info("表 {} 索引已从ivfflat升级为hnsw", tableName);
            }
        } catch (Exception e) {
            log.warn("检查/升级索引时出错（表: {}）: {}", tableName, e.getMessage());
        }
    }

    /**
     * 获取或创建指定知识库的VectorStore
     *
     * @param pgJdbcTemplate pg向量数据库JdbcTemplate
     * @param embeddingModel 嵌入模型
     * @param tableName 向量表名
     * @return VectorStore实例
     */
    public VectorStore getOrCreateVectorStore(JdbcTemplate pgJdbcTemplate,
                                               EmbeddingModel embeddingModel,
                                               String tableName) {
        return vectorStoreCache.computeIfAbsent(tableName, k -> {
            log.info("创建新的VectorStore缓存，表名: {}", tableName);
            return createPgVectorStore(pgJdbcTemplate, embeddingModel, tableName);
        });
    }

    /**
     * 清空指定知识库的向量表
     *
     * @param pgJdbcTemplate pg向量数据库JdbcTemplate
     * @param tableName 向量表名
     */
    public void clearVectorTable(JdbcTemplate pgJdbcTemplate, String tableName) {
        try {
            log.info("清空向量表: {}", tableName);
            pgJdbcTemplate.execute("TRUNCATE TABLE " + tableName);
            log.info("向量表 {} 已清空", tableName);
        } catch (Exception e) {
            log.error("清空向量表 {} 失败", tableName, e);
            throw new RuntimeException("清空向量表失败: " + tableName, e);
        }
    }

    /**
     * 批量获取多个知识库的VectorStore
     *
     * @param pgJdbcTemplate pg向量数据库JdbcTemplate
     * @param embeddingModel 嵌入模型
     * @param tableNames 向量表名列表
     * @return VectorStore列表
     */
    public List<VectorStore> getVectorStoresByTableNames(JdbcTemplate pgJdbcTemplate,
                                                          EmbeddingModel embeddingModel,
                                                          List<String> tableNames) {
        List<VectorStore> vectorStores = new ArrayList<>();
        for (String tableName : tableNames) {
            try {
                VectorStore vectorStore = getOrCreateVectorStore(pgJdbcTemplate, embeddingModel, tableName);
                vectorStores.add(vectorStore);
                log.debug("成功获取知识库向量存储: {}", tableName);
            } catch (Exception e) {
                log.error("获取知识库向量存储失败: {}", tableName, e);
            }
        }
        log.info("成功获取 {} 个知识库的向量存储", vectorStores.size());
        return vectorStores;
    }
}
