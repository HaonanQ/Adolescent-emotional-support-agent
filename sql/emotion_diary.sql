-- 情绪日记表
CREATE TABLE `emotion_diary` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日记ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `mood` VARCHAR(50) NOT NULL COMMENT '情绪（开心/难过/愤怒/焦虑/平静/其他）',
    `mood_score` INT NOT NULL COMMENT '情绪分数（1-10）',
    `content` TEXT COMMENT '日记内容（支持Markdown）',
    `image_url` VARCHAR(500) COMMENT '图片URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情绪日记表';

-- 插入示例数据
INSERT INTO `emotion_diary` (`user_id`, `mood`, `mood_score`, `content`, `image_url`) VALUES
(2025475557931888642, '开心', 9, '今天考试考了全班第一！真的很开心，这段时间的努力没有白费。感谢老师和同学的帮助。\n\n> 努力终会有回报！', NULL),
(2025475557931888642, '难过', 3, '今天和最好的朋友吵架了，心里很难过。我应该主动找他和好吗？', NULL),
(2025475557931888642, '平静', 7, '今天天气很好，下午去公园散步了，感觉很放松。生活就是这样，平平淡淡才是真。', NULL);
