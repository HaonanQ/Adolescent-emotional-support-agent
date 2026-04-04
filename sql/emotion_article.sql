-- 情感课堂文章表
CREATE TABLE emotion_article (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    title VARCHAR(255) NOT NULL COMMENT '文章标题',
    content LONGTEXT NOT NULL COMMENT '文章内容（HTML富文本，支持图片）',
    cover_image VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
    summary VARCHAR(1000) DEFAULT NULL COMMENT '文章摘要（列表展示用）',
    author_id BIGINT NOT NULL COMMENT '作者用户ID',
    author_name VARCHAR(100) DEFAULT NULL COMMENT '作者名称',
    category VARCHAR(100) DEFAULT NULL COMMENT '文章分类',
    status TINYINT DEFAULT 1 COMMENT '发布状态：0-不可见（下架），1-可见（已发布）',
    read_count INT DEFAULT 0 COMMENT '阅读次数',
    sort_order INT DEFAULT 0 COMMENT '排序权重（越大越靠前）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_sort (sort_order),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情感课堂文章表';
