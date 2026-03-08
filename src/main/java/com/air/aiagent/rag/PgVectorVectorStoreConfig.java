package com.air.aiagent.rag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

import java.util.List;

/**
 * @author Qiuhaonan
 * @since 2025/8/11 下午8:45
 */
@Slf4j
@Configuration
public class PgVectorVectorStoreConfig {

    // 引入文档加载器
    @Resource
    private TeenSupportDocumentLoader loader;

    // 引入文本切分器
    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    // 初始化Bean
    @Bean
    public VectorStore pgVectorVectorStore(@Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbcTemplate,
            EmbeddingModel dashscopeEmbeddingModel) {
        // 最终得到一个PgVectorStore对象
        PgVectorStore pgVectorStore = PgVectorStore.builder(pgJdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1536) // Optional: defaults to model dimensions or 1536
                .distanceType(COSINE_DISTANCE) // Optional: defaults to COSINE_DISTANCE
                .indexType(HNSW) // Optional: defaults to HNSW
                .initializeSchema(true) // Optional: defaults to false 自动初始化建表，这里设置了 true ，但是它并不会建表，得要在虚拟机中手动建表
                .schemaName("public") // Optional: defaults to "public" 向量数据库的名称
                .vectorTableName("Teens_rag") // Optional: defaults to "vector_store" 表的名称
                .maxDocumentBatchSize(10000) // Optional: defaults to 10000 最大批量插入的文档数
                .build();
        
        // 加载并处理知识库文档
        try {
            log.info("开始加载知识库文档...");
            // 1. 加载 Markdown 文档
            List<Document> documentList = loader.loadMarkdown();
            log.info("加载知识库文档数量: {}", documentList.size());
            
            if (!documentList.isEmpty()) {
                // 2. 切分文档
                List<Document> splitDocumentList = myTokenTextSplitter.splitCustomized(documentList);
                log.info("切分后文档数量: {}", splitDocumentList.size());
                
                // 3. 添加到向量数据库
                pgVectorStore.add(splitDocumentList);
                log.info("知识库文档已成功加载到 pgvector 数据库");
            } else {
                log.warn("未加载到任何知识库文档，可能会影响 RAG 功能");
            }
        } catch (Exception e) {
            log.error("加载知识库文档到 pgvector 数据库失败", e);
            log.warn("知识库文档加载失败，RAG 功能可能会受到影响，但应用会继续启动");
        }
        
        return pgVectorStore;
    }
}
