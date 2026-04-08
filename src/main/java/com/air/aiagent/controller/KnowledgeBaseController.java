package com.air.aiagent.controller;

import com.air.aiagent.annotation.LoginCheck;
import com.air.aiagent.common.BaseResponse;
import com.air.aiagent.common.ResultUtils;
import com.air.aiagent.domain.entity.KnowledgeBase;
import com.air.aiagent.domain.entity.KnowledgeDocument;
import com.air.aiagent.domain.entity.User;
import com.air.aiagent.exception.BusinessException;
import com.air.aiagent.exception.ErrorCode;
import com.air.aiagent.service.KnowledgeBaseService;
import com.air.aiagent.service.KnowledgeDocumentService;
import com.air.aiagent.service.UserService;
import com.air.aiagent.utils.KnowledgeFileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库管理接口（管理端）
 * 提供知识库的增删改查和热更新功能
 * 需要管理员权限才能操作
 * @author AI Agent
 * @since 2025/3/12
 */
@Slf4j
@RestController
@RequestMapping("/knowledgeBase")
@Tag(name = "知识库管理接口", description = "管理端知识库管理，支持热更新（需要管理员权限）")
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private UserService userService;

    /**
     * 检查当前用户是否为管理员
     * @param request HTTP请求
     * @return 登录用户信息
     * @throws BusinessException 如果不是管理员或未登录
     */
    private User checkAdminPermission(HttpServletRequest request) {
        // 1. 获取登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        // 2. 检查是否为管理员
        Integer isAdmin = loginUser.getIsAdmin();
        if (isAdmin == null || isAdmin != 1) {
            log.warn("用户 [{}] 尝试访问管理员接口，权限不足", loginUser.getUsername());
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有管理员才能操作知识库");
        }

        log.info("管理员 [{}] 执行知识库操作", loginUser.getUsername());
        return loginUser;
    }

    /**
     * 获取所有知识库列表
     * @param request HTTP请求
     * @return 知识库列表
     */
    @LoginCheck
    @Operation(summary = "获取所有知识库列表", description = "查询所有知识库，包括启用和停用的（需要管理员权限）")
    @PostMapping("/list")
    public BaseResponse<List<KnowledgeBase>> listKnowledgeBases(HttpServletRequest request) {
        checkAdminPermission(request);
        List<KnowledgeBase> list = knowledgeBaseService.list();
        return ResultUtils.success(list);
    }

    /**
     * 获取启用的知识库列表
     * @param request HTTP请求
     * @return 启用的知识库列表
     */
    @LoginCheck
    @Operation(summary = "获取启用的知识库列表", description = "只查询状态为启用的知识库（需要管理员权限）")
    @PostMapping("/listActive")
    public BaseResponse<List<KnowledgeBase>> listActiveKnowledgeBases(HttpServletRequest request) {
        checkAdminPermission(request);
        List<KnowledgeBase> list = knowledgeBaseService.getActiveKnowledgeBaseList();
        return ResultUtils.success(list);
    }

    /**
     * 根据ID获取知识库详情
     * @param id 知识库ID
     * @param request HTTP请求
     * @return 知识库详情
     */
    @LoginCheck
    @Operation(summary = "获取知识库详情", description = "根据ID查询知识库详细信息（需要管理员权限）")
    @Parameter(name = "id", description = "知识库ID", required = true)
    @PostMapping("/getById")
    public BaseResponse<KnowledgeBase> getKnowledgeBaseById(@RequestParam Long id, HttpServletRequest request) {
        checkAdminPermission(request);
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(id);
        return ResultUtils.success(knowledgeBase);
    }

    /**
     * 创建知识库
     * @param knowledgeBase 知识库信息
     * @param request HTTP请求
     * @return 创建后的知识库ID
     */
    @LoginCheck
    @Operation(summary = "创建知识库", description = "创建新的知识库（需要管理员权限），会自动创建文件夹")
    @PostMapping("/add")
    public BaseResponse<Long> addKnowledgeBase(@RequestBody KnowledgeBase knowledgeBase, HttpServletRequest request) {
        User admin = checkAdminPermission(request);
        
        // 校验表名
        String tableName = knowledgeBase.getTableName();
        if (tableName == null || tableName.trim().isEmpty()) {
            return (BaseResponse<Long>) ResultUtils.error(ErrorCode.PARAMS_ERROR, "表名不能为空");
        }
        // 校验表名长度
        if (tableName.length() > 30) {
            return (BaseResponse<Long>) ResultUtils.error(ErrorCode.PARAMS_ERROR, "表名长度不能超过30个字符");
        }
        // 校验表名格式：只允许小写字母、数字和下划线，以字母开头
        if (!tableName.matches("^[a-z][a-z0-9_]*$")) {
            return (BaseResponse<Long>) ResultUtils.error(ErrorCode.PARAMS_ERROR, "表名格式不正确，只能包含小写字母、数字和下划线，且必须以字母开头");
        }
        
        try {
            Long id = knowledgeBaseService.createKnowledgeBase(knowledgeBase);
            if (id != null) {
                log.info("管理员 [{}] 创建知识库成功，ID: {}", admin.getUsername(), id);
                return ResultUtils.success(id);
            } else {
                return (BaseResponse<Long>) ResultUtils.error(ErrorCode.OP_RAG_ERROR);
            }
        } catch (RuntimeException e) {
            log.error("创建知识库失败: {}", e.getMessage());
            return (BaseResponse<Long>) ResultUtils.error(ErrorCode.OP_RAG_ERROR, e.getMessage());
        }
    }

    /**
     * 更新知识库
     * @param knowledgeBase 知识库信息
     * @param request HTTP请求
     * @return 是否成功
     */
    @LoginCheck
    @Operation(summary = "更新知识库", description = "更新知识库信息（需要管理员权限），名称不能重复")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateKnowledgeBase(@RequestBody KnowledgeBase knowledgeBase, HttpServletRequest request) {
        checkAdminPermission(request);

        // 检查名称是否与其他知识库重复
        if (knowledgeBase.getName() != null && !knowledgeBase.getName().trim().isEmpty()) {
            if (knowledgeBaseService.isNameExists(knowledgeBase.getName(), knowledgeBase.getId())) {
                return (BaseResponse<Boolean>) ResultUtils.error(ErrorCode.OP_RAG_ERROR, "知识库名称已存在");
            }
        }

        boolean updated = knowledgeBaseService.updateById(knowledgeBase);
        return ResultUtils.success(updated);
    }

    /**
     * 批量更新文档状态
     * @param ids 文档ID列表
     * @param status 状态：0-停用，1-启用
     * @param request HTTP请求
     * @return 是否成功
     */
    @LoginCheck
    @Operation(summary = "批量更新文档状态", description = "批量设置文档的启用状态（需要管理员权限）")
    @PostMapping("/document/batchUpdateStatus")
    public BaseResponse<Boolean> batchUpdateDocumentStatus(@RequestBody java.util.Map<String, Object> params, HttpServletRequest request) {
        checkAdminPermission(request);
        
        @SuppressWarnings("unchecked")
        java.util.List<?> idList = (java.util.List<?>) params.get("ids");
        Integer status = (Integer) params.get("status");
        
        if (idList == null || idList.isEmpty()) {
            return (BaseResponse<Boolean>) ResultUtils.error(ErrorCode.PARAMS_ERROR, "文档ID列表不能为空");
        }
        
        for (Object idObj : idList) {
            Long id;
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                id = Long.parseLong((String) idObj);
            } else {
                continue;
            }
            KnowledgeDocument document = new KnowledgeDocument();
            document.setId(id);
            document.setStatus(status);
            document.setUpdateTime(LocalDateTime.now());
            knowledgeDocumentService.updateById(document);
            
            // 更新知识库的文档数量
            KnowledgeDocument doc = knowledgeDocumentService.getById(id);
            if (doc != null) {
                updateKnowledgeBaseDocumentCount(doc.getKnowledgeBaseId());
            }
        }
        
        return ResultUtils.success(true);
    }

    /**
     * 批量删除文档
     * @param ids 文档ID列表
     * @param request HTTP请求
     * @return 是否成功
     */
    @LoginCheck
    @Operation(summary = "批量删除文档", description = "批量删除文档（需要管理员权限）")
    @PostMapping("/document/batchDelete")
    public BaseResponse<Boolean> batchDeleteDocuments(@RequestBody java.util.Map<String, Object> params, HttpServletRequest request) {
        User admin = checkAdminPermission(request);
        
        @SuppressWarnings("unchecked")
        java.util.List<?> idList = (java.util.List<?>) params.get("ids");
        
        if (idList == null || idList.isEmpty()) {
            return (BaseResponse<Boolean>) ResultUtils.error(ErrorCode.PARAMS_ERROR, "文档ID列表不能为空");
        }
        
        int count = 0;
        for (Object idObj : idList) {
            Long id;
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                id = Long.parseLong((String) idObj);
            } else {
                continue;
            }
            KnowledgeDocument doc = knowledgeDocumentService.getById(id);
            if (doc != null) {
                // 删除文件
                if (doc.getFilePath() != null) {
                    java.io.File file = new java.io.File(doc.getFilePath());
                    if (file.exists()) {
                        file.delete();
                    }
                }
                
                // 删除数据库记录
                knowledgeDocumentService.removeById(id);
                
                // 更新知识库的文档数量
                updateKnowledgeBaseDocumentCount(doc.getKnowledgeBaseId());
                count++;
            }
        }
        
        log.info("管理员 [{}] 批量删除文档成功，数量: {}", admin.getUsername(), count);
        return ResultUtils.success(true);
    }

    /**
     * 下载文档
     * @param id 文档ID
     * @param request HTTP请求
     * @param response HTTP响应
     */
    @LoginCheck
    @Operation(summary = "下载文档", description = "下载知识库文档（需要管理员权限）")
    @Parameter(name = "id", description = "文档ID", required = true)
    @GetMapping("/document/download")
    public void downloadDocument(@RequestParam Long id, HttpServletRequest request, HttpServletResponse response) {
        checkAdminPermission(request);
        
        KnowledgeDocument document = knowledgeDocumentService.getById(id);
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        }
        
        String filePath = document.getFilePath();
        if (filePath == null || filePath.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件路径不存在");
        }
        
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在");
        }
        
        try {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + 
                java.net.URLEncoder.encode(document.getFileName(), "UTF-8") + "\"");
            response.setContentLengthLong(file.length());
            
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                 java.io.OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } catch (Exception e) {
            log.error("下载文档失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载文档失败");
        }
    }

    /**
     * 删除知识库
     * @param id 知识库ID
     * @param request HTTP请求
     * @return 是否成功
     */
    @LoginCheck
    @Operation(summary = "删除知识库", description = "删除知识库（需要管理员权限），会同时删除文件夹和所有文档")
    @Parameter(name = "id", description = "知识库ID", required = true)
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteKnowledgeBase(@RequestParam Long id, HttpServletRequest request) {
        User admin = checkAdminPermission(request);
        boolean removed = knowledgeBaseService.deleteKnowledgeBase(id);
        if (removed) {
            log.info("管理员 [{}] 删除知识库成功，ID: {}", admin.getUsername(), id);
        }
        return ResultUtils.success(removed);
    }

    /**
     * 启用/停用知识库
     * @param id 知识库ID
     * @param status 状态：0-停用，1-启用
     * @param request HTTP请求
     * @return 是否成功
     */
    @LoginCheck
    @Operation(summary = "启用/停用知识库", description = "设置知识库的启用状态（需要管理员权限）")
    @Parameter(name = "id", description = "知识库ID", required = true)
    @Parameter(name = "status", description = "状态：0-停用，1-启用", required = true)
    @PostMapping("/updateStatus")
    public BaseResponse<Boolean> updateStatus(@RequestParam Long id, @RequestParam Integer status, HttpServletRequest request) {
        User admin = checkAdminPermission(request);
        boolean updated = knowledgeBaseService.updateStatus(id, status);
        if (updated) {
            log.info("管理员 [{}] 更新知识库状态成功，ID: {}, 状态: {}", admin.getUsername(), id, status);
        }
        return ResultUtils.success(updated);
    }

    /**
     * 设置启动时自动加载
     * @param id 知识库ID
     * @param autoLoad 是否自动加载：0-否，1-是
     * @param request HTTP请求
     * @return 是否成功
     */
    @LoginCheck
    @Operation(summary = "设置自动加载", description = "设置知识库是否在启动时自动加载（需要管理员权限）")
    @Parameter(name = "id", description = "知识库ID", required = true)
    @Parameter(name = "autoLoad", description = "是否自动加载：0-否，1-是", required = true)
    @PostMapping("/updateAutoLoad")
    public BaseResponse<Boolean> updateAutoLoad(@RequestParam Long id, @RequestParam Integer autoLoad, HttpServletRequest request) {
        User admin = checkAdminPermission(request);
        boolean updated = knowledgeBaseService.updateAutoLoad(id, autoLoad);
        if (updated) {
            log.info("管理员 [{}] 更新知识库自动加载设置成功，ID: {}, autoLoad: {}", admin.getUsername(), id, autoLoad);
        }
        return ResultUtils.success(updated);
    }

    /**
     * 热更新知识库（重新加载文档到向量数据库）
     * @param id 知识库ID
     * @param request HTTP请求
     * @return 是否成功
     */
    @LoginCheck
    @Operation(summary = "热更新知识库", description = "重新加载知识库文档到向量数据库，会清空原有数据（需要管理员权限）")
    @Parameter(name = "id", description = "知识库ID", required = true)
    @PostMapping("/hotReload")
    public BaseResponse<Boolean> hotReloadKnowledgeBase(@RequestParam Long id, HttpServletRequest request) {
        User admin = checkAdminPermission(request);
        log.info("管理员 [{}] 发起热更新知识库请求，ID: {}", admin.getUsername(), id);
        boolean result = knowledgeBaseService.hotReloadKnowledgeBase(id);
        if (result) {
            log.info("管理员 [{}] 热更新知识库成功，ID: {}", admin.getUsername(), id);
            return ResultUtils.success(true);
        } else {
            return (BaseResponse<Boolean>) ResultUtils.error(ErrorCode.OP_RAG_ERROR);
        }
    }

    // ==================== 知识库文档管理接口 ====================

    /**
     * 获取知识库的文档列表
     * @param knowledgeBaseId 知识库ID
     * @param request HTTP请求
     * @return 文档列表
     */
    @LoginCheck
    @Operation(summary = "获取知识库文档列表", description = "查询指定知识库下的所有文档（需要管理员权限）")
    @Parameter(name = "knowledgeBaseId", description = "知识库ID", required = true)
    @PostMapping("/document/list")
    public BaseResponse<List<KnowledgeDocument>> listDocuments(@RequestParam Long knowledgeBaseId, HttpServletRequest request) {
        checkAdminPermission(request);
        QueryWrapper<KnowledgeDocument> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("knowledge_base_id", knowledgeBaseId);
        List<KnowledgeDocument> list = knowledgeDocumentService.list(queryWrapper);
        return ResultUtils.success(list);
    }

    /**
     * 上传文档到知识库
     * @param knowledgeBaseId 知识库ID
     * @param file 上传的文件
     * @param request HTTP请求
     * @return 创建后的文档ID
     */
    @LoginCheck
    @Operation(summary = "上传文档", description = "向知识库上传新文档（需要管理员权限），文件会保存到知识库对应文件夹")
    @Parameter(name = "knowledgeBaseId", description = "知识库ID", required = true)
    @PostMapping("/document/upload")
    public BaseResponse<Long> uploadDocument(
            @RequestParam Long knowledgeBaseId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        User admin = checkAdminPermission(request);

        // 1. 检查知识库是否存在
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(knowledgeBaseId);
        if (knowledgeBase == null) {
            return (BaseResponse<Long>) ResultUtils.error(ErrorCode.OP_RAG_ERROR, "知识库不存在");
        }

        // 2. 检查文件是否为空
        if (file == null || file.isEmpty()) {
            return (BaseResponse<Long>) ResultUtils.error(ErrorCode.OP_RAG_ERROR, "上传文件不能为空");
        }

        // 3. 检查文件类型（目前只支持markdown）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".md")) {
            return (BaseResponse<Long>) ResultUtils.error(ErrorCode.OP_RAG_ERROR, "目前只支持上传Markdown(.md)文件");
        }

        try {
            // 4. 保存文件到知识库文件夹
            String filePath = KnowledgeFileUtil.saveDocument(knowledgeBase.getName(), file);
            if (filePath == null) {
                return (BaseResponse<Long>) ResultUtils.error(ErrorCode.OP_RAG_ERROR, "保存文件失败");
            }

            // 5. 创建文档记录
            KnowledgeDocument document = new KnowledgeDocument();
            document.setKnowledgeBaseId(knowledgeBaseId);
            document.setFileName(originalFilename);
            document.setFilePath(filePath);
            document.setFileType("markdown");
            document.setFileSize(file.getSize());
            document.setStatus(1); // 默认启用
            document.setCreateTime(LocalDateTime.now());
            document.setUpdateTime(LocalDateTime.now());

            boolean saved = knowledgeDocumentService.save(document);
            if (saved) {
                // 更新知识库的文档数量
                updateKnowledgeBaseDocumentCount(knowledgeBaseId);
                log.info("管理员 [{}] 上传文档成功，ID: {}，路径: {}", admin.getUsername(), document.getId(), filePath);
                return ResultUtils.success(document.getId());
            } else {
                // 删除已保存的文件
                KnowledgeFileUtil.deleteDocument(filePath);
                return (BaseResponse<Long>) ResultUtils.error(ErrorCode.OP_RAG_ERROR, "保存文档记录失败");
            }
        } catch (Exception e) {
            log.error("上传文档失败", e);
            return (BaseResponse<Long>) ResultUtils.error(ErrorCode.OP_RAG_ERROR, "上传文档失败: " + e.getMessage());
        }
    }

    /**
     * 更新文档信息
     * @param document 文档信息
     * @param request HTTP请求
     * @return 是否成功
     */
    @LoginCheck
    @Operation(summary = "更新文档", description = "更新文档信息（需要管理员权限）")
    @PostMapping("/document/update")
    public BaseResponse<Boolean> updateDocument(@RequestBody KnowledgeDocument document, HttpServletRequest request) {
        checkAdminPermission(request);
        boolean updated = knowledgeDocumentService.updateById(document);
        return ResultUtils.success(updated);
    }

    /**
     * 删除文档
     * @param id 文档ID
     * @param request HTTP请求
     * @return 是否成功
     */
    @LoginCheck
    @Operation(summary = "删除文档", description = "删除文档（需要管理员权限），会同时删除文件")
    @Parameter(name = "id", description = "文档ID", required = true)
    @PostMapping("/document/delete")
    public BaseResponse<Boolean> deleteDocument(@RequestParam Long id, HttpServletRequest request) {
        User admin = checkAdminPermission(request);

        KnowledgeDocument document = knowledgeDocumentService.getById(id);
        if (document == null) {
            return ResultUtils.success(true); // 文档不存在，视为删除成功
        }

        Long knowledgeBaseId = document.getKnowledgeBaseId();
        String filePath = document.getFilePath();

        // 1. 删除文件
        if (filePath != null && !filePath.isEmpty()) {
            KnowledgeFileUtil.deleteDocument(filePath);
        }

        // 2. 删除数据库记录
        boolean removed = knowledgeDocumentService.removeById(id);
        if (removed) {
            // 更新知识库的文档数量
            updateKnowledgeBaseDocumentCount(knowledgeBaseId);
            log.info("管理员 [{}] 删除文档成功，ID: {}", admin.getUsername(), id);
        }
        return ResultUtils.success(removed);
    }

    /**
     * 启用/停用文档
     * @param id 文档ID
     * @param status 状态：0-停用，1-启用
     * @param request HTTP请求
     * @return 是否成功
     */
    @LoginCheck
    @Operation(summary = "启用/停用文档", description = "设置文档的启用状态（需要管理员权限）")
    @Parameter(name = "id", description = "文档ID", required = true)
    @Parameter(name = "status", description = "状态：0-停用，1-启用", required = true)
    @PostMapping("/document/updateStatus")
    public BaseResponse<Boolean> updateDocumentStatus(@RequestParam Long id, @RequestParam Integer status, HttpServletRequest request) {
        checkAdminPermission(request);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setStatus(status);
        document.setUpdateTime(LocalDateTime.now());
        boolean updated = knowledgeDocumentService.updateById(document);

        if (updated) {
            // 更新知识库的文档数量
            KnowledgeDocument doc = knowledgeDocumentService.getById(id);
            if (doc != null) {
                updateKnowledgeBaseDocumentCount(doc.getKnowledgeBaseId());
            }
        }
        return ResultUtils.success(updated);
    }

    /**
     * 更新知识库的文档数量
     * @param knowledgeBaseId 知识库ID
     */
    private void updateKnowledgeBaseDocumentCount(Long knowledgeBaseId) {
        try {
            int count = knowledgeDocumentService.countActiveByKnowledgeBaseId(knowledgeBaseId);
            KnowledgeBase knowledgeBase = new KnowledgeBase();
            knowledgeBase.setId(knowledgeBaseId);
            knowledgeBase.setDocumentCount(count);
            knowledgeBase.setUpdateTime(LocalDateTime.now());
            knowledgeBaseService.updateById(knowledgeBase);
        } catch (Exception e) {
            log.error("更新知识库文档数量失败，knowledgeBaseId: {}", knowledgeBaseId, e);
        }
    }
}
