package com.air.aiagent.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传文件响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadFileVO {

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件在MinIO中的存储路径
     */
    private String objectPath;
}
