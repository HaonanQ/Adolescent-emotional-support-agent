package com.air.aiagent.domain.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * @author Qiuhaonan
 * @since 2025/9/27 13:17
 * 添加用户响应类
 */
@Data
public class UserVO {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 性别：0-女生，1-男生
     */
    private Integer sex;

    /**
     * 用户头像URL
     */
    private String avatar;

    /**
     * 是否为管理员：0-普通用户，1-管理员
     */
    private Integer isAdmin;
}
