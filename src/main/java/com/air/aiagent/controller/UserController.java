package com.air.aiagent.controller;
import cn.hutool.core.util.StrUtil;
import com.air.aiagent.common.BaseResponse;
import com.air.aiagent.common.ResultUtils;
import com.air.aiagent.domain.dto.AddUserRequest;
import com.air.aiagent.domain.dto.UserLoginRequest;
import com.air.aiagent.domain.entity.User;
import com.air.aiagent.domain.vo.UserVO;
import com.air.aiagent.exception.BusinessException;
import com.air.aiagent.exception.ErrorCode;
import com.air.aiagent.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import static com.air.aiagent.constant.Constant.LOGIN_USER;


/**
 * @author Qiuhaonan
 * @since 2025/9/12 下午12:59
 */
@RestController
@RequestMapping("/emotionagent/user")
@Tag(name = "用户接口", description = "用于用户登陆相关的基础接口")
public class UserController {

    @Resource
    private UserService userService;


    /**
     * 注册账号
     */
    @PostMapping("/register")
    public BaseResponse<Boolean> register(@RequestBody AddUserRequest request) {
        // 1.调用注册方法，返回结果
        if(userService.register(request)){
            return ResultUtils.success(true);
        }
        // 2.未注册成功，抛异常
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
    }


    /**
     * 登录
     */
    @PostMapping("/login")
    public BaseResponse<UserVO> login(@RequestBody UserLoginRequest request, HttpServletRequest httpServletRequest){
        // 1.判断传过来的参数
        if(request == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        // 2.调用登录的方法
        UserVO userVO = userService.login(request,httpServletRequest);
        // 3.返回结果
        if(userVO != null){
            return ResultUtils.success(userVO);
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败");
    }


    /**
     * 退出登录
     */
    @GetMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request){
        // 2.用户已登录，消除登录态
        request.getSession().removeAttribute(LOGIN_USER);
        return ResultUtils.success(true);
    }


    /**
     * 获取当前登录用户，也就是存储到后端中的用户信息过期了，前端就清理内存，将保存的用户信息释放掉
     */
    @GetMapping("/getLoginUser")
    public BaseResponse<Boolean> getLoginUser(HttpServletRequest request){
        // 1.从 session 中获取用户
        User user = (User)request.getSession().getAttribute(LOGIN_USER);
        // 2.若存在就返回 true ，不存在就返回 false
        if(user != null)
            return ResultUtils.success(true);
        return ResultUtils.success(false);
    }
}
