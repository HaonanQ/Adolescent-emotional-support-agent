package com.air.aiagent.service;

import com.air.aiagent.domain.entity.KnowledgeBase;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 知识库管理服务接口
 * @author AI Agent
 * @since 2025/3/12
 */
public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    /**
     * 获取所有启用的知识库
     * @return 知识库列表
     */
    List<KnowledgeBase> getActiveKnowledgeBaseList();

    /**
     * 获取启动时需要自动加载的知识库
     * @return 知识库列表
     */
    List<KnowledgeBase> getAutoLoadKnowledgeBaseList();

    /**
     * 启用/停用知识库
     * @param id 知识库ID
     * @param status 状态：0-停用，1-启用
     * @return 是否成功
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 设置是否启动时自动加载
     * @param id 知识库ID
     * @param autoLoad 是否自动加载：0-否，1-是
     * @return 是否成功
     */
    boolean updateAutoLoad(Long id, Integer autoLoad);

    /**
     * 热更新知识库（重新加载文档到向量数据库）
     * 每个知识库有独立的向量表，互不影响
     * @param id 知识库ID
     * @return 是否成功
     */
    boolean hotReloadKnowledgeBase(Long id);

    /**
     * 更新知识库同步时间和文档数量
     * @param id 知识库ID
     * @param chunkCount 文档块数量
     * @return 是否成功
     */
    boolean updateSyncTime(Long id, int chunkCount);

    /**
     * 检查知识库名称是否已存在
     * @param name 知识库名称
     * @param excludeId 排除的ID（用于更新时排除自身）
     * @return 是否存在
     */
    boolean isNameExists(String name, Long excludeId);

    /**
     * 创建知识库（带文件夹创建和名称校验）
     * @param knowledgeBase 知识库信息
     * @return 创建后的知识库ID，失败返回null
     */
    Long createKnowledgeBase(KnowledgeBase knowledgeBase);

    /**
     * 删除知识库（同时删除文件夹）
     * @param id 知识库ID
     * @return 是否成功
     */
    boolean deleteKnowledgeBase(Long id);
}
