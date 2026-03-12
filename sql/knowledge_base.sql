-- 知识库管理表（MySQL版本）
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '知识库名称',
    description TEXT COMMENT '知识库描述',
    table_name VARCHAR(100) NOT NULL COMMENT '向量数据库表名',
    status TINYINT DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    auto_load TINYINT DEFAULT 0 COMMENT '启动时自动加载：0-否，1-是',
    document_count INT DEFAULT 0 COMMENT '文档数量',
    last_sync_time TIMESTAMP NULL COMMENT '最后同步时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库管理表';

-- 知识库文档表（MySQL版本）
CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    knowledge_base_id BIGINT NOT NULL COMMENT '所属知识库ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名称',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_type VARCHAR(50) COMMENT '文件类型：markdown/pdf/txt等',
    file_size BIGINT COMMENT '文件大小（字节）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    chunk_count INT DEFAULT 0 COMMENT '切分后的文档块数量',
    last_sync_time TIMESTAMP NULL COMMENT '最后同步时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

-- 创建索引
CREATE INDEX idx_kb_status ON knowledge_base(status);
CREATE INDEX idx_kb_auto_load ON knowledge_base(auto_load);
CREATE INDEX idx_kd_knowledge_base_id ON knowledge_document(knowledge_base_id);
CREATE INDEX idx_kd_status ON knowledge_document(status);

-- 插入默认知识库（青少年情感陪伴）
INSERT IGNORE INTO knowledge_base (id, name, description, table_name, status, auto_load, create_time, update_time)
VALUES (1, '青少年情感陪伴知识库', '用于青少年情感陪伴的RAG知识库', 'Teens_rag', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入示例数据
INSERT IGNORE INTO knowledge_document (knowledge_base_id, file_name, file_path, file_type, status, create_time, update_time)
VALUES 
    (1, 'teen_support_guide.md', '/knowledge/teen_support_guide.md', 'markdown', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'emotional_support.md', '/knowledge/emotional_support.md', 'markdown', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
