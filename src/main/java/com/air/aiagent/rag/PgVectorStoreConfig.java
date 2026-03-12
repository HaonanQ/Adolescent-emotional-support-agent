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
                    "CREATE INDEX IF NOT EXISTS idx_%s_embedding ON %s USING ivfflat (embedding vector_cosine_ops)",
                    tableName, tableName);
                pgJdbcTemplate.execute(indexSql);

                log.info("向量表 {} 创建成功", tableName);
            } else {
                log.debug("向量表 {} 已存在", tableName);
            }
        } catch (Exception e) {
            log.error("创建向量表 {} 失败", tableName, e);
            throw new RuntimeException("创建向量表失败: " + tableName, e);
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
}
