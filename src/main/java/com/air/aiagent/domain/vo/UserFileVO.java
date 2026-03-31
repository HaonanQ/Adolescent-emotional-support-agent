package com.air.aiagent.domain.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * @author Qiuhaonan
 * @since 2025/10/10 20:19
 */
@Data
@Builder
public class UserFileVO {
    /**
     * 文件主键ID
     */
    private Long id;

    /**
     * 文件访问URL
     */
    private String fileUrl;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
