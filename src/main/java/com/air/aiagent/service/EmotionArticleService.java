package com.air.aiagent.service;

import com.air.aiagent.domain.dto.EmotionArticleAddRequest;
import com.air.aiagent.domain.dto.EmotionArticleUpdateRequest;
import com.air.aiagent.domain.entity.EmotionArticle;
import com.air.aiagent.domain.vo.EmotionArticleVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 针对表【emotion_article(情感课堂文章表)】的数据库操作Service
 */
public interface EmotionArticleService extends IService<EmotionArticle> {

    /**
     * Entity转换为VO
     */
    EmotionArticleVO entityToVO(EmotionArticle emotionArticle);

    /**
     * 管理员创建文章（仅管理员可调用）
     */
    Long addArticle(EmotionArticleAddRequest request);

    /**
     * 管理员更新文章（仅管理员可调用）
     */
    Boolean updateArticle(EmotionArticleUpdateRequest request);

    /**
     * 根据ID查询文章详情（用户端，仅可见文章）
     */
    EmotionArticleVO getArticleByIdForUser(Long id);

    /**
     * 根据ID查询文章详情（管理员端，包含不可见文章）
     */
    EmotionArticleVO getArticleByIdForAdmin(Long id);

    /**
     * 查询用户可见的文章列表（按排序权重和创建时间倒序）
     */
    List<EmotionArticleVO> getVisibleArticleList();

    /**
     * 查询所有文章列表（管理员端，包含不可见文章）
     */
    List<EmotionArticleVO> getAllArticleList();

    /**
     * 管理员删除文章（逻辑删除）
     */
    Boolean deleteArticle(Long id);

    /**
     * 增加阅读次数
     */
    void incrementReadCount(Long id);
}
