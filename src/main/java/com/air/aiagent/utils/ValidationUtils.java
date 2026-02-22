package com.air.aiagent.utils;

import com.air.aiagent.domain.dto.AddUserRequest;
import com.air.aiagent.exception.BusinessException;
import com.air.aiagent.exception.ErrorCode;
import org.apache.commons.lang3.StringUtils;
/**
 * 工具类
 * 验证用户名、密码是否有效
 */
public class ValidationUtils {

    /**
     * 验证请求参数合法性
     * @param request 用户请求
     */
    public static void validateAddUserRequest(AddUserRequest request) {
        // 验证整体参数
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数不能为空");
        }

        // 验证用户名是否符合要求
        if (StringUtils.isBlank(request.getUsername())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名不能为空");
        }
        if (request.getUsername().length() < 3 || request.getUsername().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名长度应在3-20个字符之间");
        }

        // 验证密码是否正确
        if (StringUtils.isBlank(request.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码不能为空");
        }
        if (request.getPassword().length() < 6 || request.getPassword().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码长度应在6-20个字符之间");
        }
    }
}