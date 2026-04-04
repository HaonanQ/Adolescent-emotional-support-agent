package com.air.aiagent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.air.aiagent.domain.dto.EmotionArticleAddRequest;
import com.air.aiagent.domain.dto.EmotionArticleUpdateRequest;
import com.air.aiagent.domain.entity.EmotionArticle;
import com.air.aiagent.domain.vo.EmotionArticleVO;
import com.air.aiagent.exception.BusinessException;
import com.air.aiagent.exception.ErrorCode;
import com.air.aiagent.mapper.EmotionArticleMapper;
import com.air.aiagent.service.EmotionArticleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 针对表【emotion_article(情感课堂文章表)】的数据库操作Service实现
 */
@Service
public class EmotionArticleServiceImpl extends ServiceImpl<EmotionArticleMapper, EmotionArticle> implements EmotionArticleService {

    /**
     * Entity转换为VO
     */
    @Override
    public EmotionArticleVO entityToVO(EmotionArticle emotionArticle) {
        return BeanUtil.copyProperties(emotionArticle, EmotionArticleVO.class);
    }

    /**
     * 管理员创建文章
     */
    @Override
    public Long addArticle(EmotionArticleAddRequest request) {
        if (request.getTitle() == null || request.getTitle().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文章标题不能为空");
        }
        if (request.getContent() == null || request.getContent().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文章内容不能为空");
        }

        EmotionArticle article = EmotionArticle.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .coverImage(request.getCoverImage())
                .summary(request.getSummary())
                .authorId(request.getAuthorId())
                .authorName(request.getAuthorName())
                .category(request.getCategory())
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .readCount(0)
                .sortOrder(0)
                .createTime(new Date())
                .build();

        boolean saved = save(article);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建文章失败");
        }

        return article.getId();
    }

    /**
     * 管理员更新文章
     */
    @Override
    public Boolean updateArticle(EmotionArticleUpdateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文章ID不能为空");
        }

        EmotionArticle existingArticle = getById(request.getId());
        if (existingArticle == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        }

        EmotionArticle updateArticle = new EmotionArticle();
        updateArticle.setId(request.getId());
        if (request.getTitle() != null) {
            updateArticle.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            updateArticle.setContent(request.getContent());
        }
        if (request.getCoverImage() != null) {
            updateArticle.setCoverImage(request.getCoverImage());
        }
        if (request.getSummary() != null) {
            updateArticle.setSummary(request.getSummary());
        }
        if (request.getCategory() != null) {
            updateArticle.setCategory(request.getCategory());
        }
        if (request.getStatus() != null) {
            updateArticle.setStatus(request.getStatus());
        }
        if (request.getSortOrder() != null) {
            updateArticle.setSortOrder(request.getSortOrder());
        }
        updateArticle.setUpdateTime(new Date());

        return updateById(updateArticle);
    }

    /**
     * 用户端查询文章详情（仅可见文章）
     */
    @Override
    public EmotionArticleVO getArticleByIdForUser(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文章ID不能为空");
        }

        EmotionArticle article = getById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        }

        if (article.getStatus() == null || article.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在或已下架");
        }

        return entityToVO(article);
    }

    /**
     * 管理员端查询文章详情（包含不可见文章）
     */
    @Override
    public EmotionArticleVO getArticleByIdForAdmin(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文章ID不能为空");
        }

        EmotionArticle article = getById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        }

        return entityToVO(article);
    }

    /**
     * 查询用户可见的文章列表
     */
    @Override
    public List<EmotionArticleVO> getVisibleArticleList() {
        LambdaQueryWrapper<EmotionArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmotionArticle::getStatus, 1)
                .orderByDesc(EmotionArticle::getSortOrder)
                .orderByDesc(EmotionArticle::getCreateTime);

        List<EmotionArticle> list = list(queryWrapper);
        return list.stream().map(this::entityToVO).collect(Collectors.toList());
    }

    /**
     * 查询所有文章列表（管理员端）
     */
    @Override
    public List<EmotionArticleVO> getAllArticleList() {
        LambdaQueryWrapper<EmotionArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(EmotionArticle::getSortOrder)
                .orderByDesc(EmotionArticle::getCreateTime);

        List<EmotionArticle> list = list(queryWrapper);
        return list.stream().map(this::entityToVO).collect(Collectors.toList());
    }

    /**
     * 删除文章（逻辑删除）
     */
    @Override
    public Boolean deleteArticle(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文章ID不能为空");
        }

        EmotionArticle article = getById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        }

        return removeById(id);
    }

    /**
     * 增加阅读次数
     */
    @Override
    public void incrementReadCount(Long id) {
        if (id == null) {
            return;
        }
        EmotionArticle article = getById(id);
        if (article != null) {
            article.setReadCount((article.getReadCount() == null ? 0 : article.getReadCount()) + 1);
            updateById(article);
        }
    }
}
