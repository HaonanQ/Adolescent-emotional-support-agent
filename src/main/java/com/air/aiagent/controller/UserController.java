package com.air.aiagent.controller;
import cn.hutool.core.util.StrUtil;
import com.air.aiagent.annotation.LoginCheck;
import com.air.aiagent.common.BaseResponse;
import com.air.aiagent.common.ResultUtils;
import com.air.aiagent.domain.dto.AddUserRequest;
import com.air.aiagent.domain.dto.UpdateNicknameRequest;
import com.air.aiagent.domain.dto.UpdatePasswordRequest;
import com.air.aiagent.domain.dto.UpdateAvatarRequest;
import com.air.aiagent.domain.dto.UpdateStatusRequest;
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

    /**
     * 修改用户昵称
     */
    @LoginCheck
    @PostMapping("/updateNickname")
    public BaseResponse<UserVO> updateNickname(@RequestBody UpdateNicknameRequest request, HttpServletRequest httpServletRequest){
        // 1.获取当前登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 2.调用更新昵称方法
        boolean success = userService.updateNickname(loginUser.getId(), request.getNickname());
        if(!success){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改昵称失败");
        }
        // 3.更新session中的用户信息
        User updatedUser = userService.getById(loginUser.getId());
        httpServletRequest.getSession().setAttribute(LOGIN_USER, updatedUser);
        // 4.返回更新后的用户信息
        return ResultUtils.success(userService.entityToVO(updatedUser));
    }

    /**
     * 修改用户密码
     */
    @LoginCheck
    @PostMapping("/updatePassword")
    public BaseResponse<Boolean> updatePassword(@RequestBody UpdatePasswordRequest request, HttpServletRequest httpServletRequest){
        // 1.获取当前登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 2.调用更新密码方法
        boolean success = userService.updatePassword(loginUser.getId(), request.getOldPassword(), request.getNewPassword());
        if(!success){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改密码失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 修改用户头像
     */
    @LoginCheck
    @PostMapping("/updateAvatar")
    public BaseResponse<UserVO> updateAvatar(@RequestBody UpdateAvatarRequest request, HttpServletRequest httpServletRequest){
        // 1.获取当前登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 2.调用更新头像方法
        boolean success = userService.updateAvatar(loginUser.getId(), request.getAvatar());
        if(!success){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改头像失败");
        }
        // 3.更新session中的用户信息
        User updatedUser = userService.getById(loginUser.getId());
        httpServletRequest.getSession().setAttribute(LOGIN_USER, updatedUser);
        // 4.返回更新后的用户信息
        return ResultUtils.success(userService.entityToVO(updatedUser));
    }

    /**
     * 修改用户状态
     */
    @LoginCheck
    @PostMapping("/updateStatus")
    public BaseResponse<UserVO> updateStatus(@RequestBody UpdateStatusRequest request, HttpServletRequest httpServletRequest){
        // 1.获取当前登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 2.调用更新状态方法
        boolean success = userService.updateStatus(loginUser.getId(), request.getRelationshipStatus());
        if(!success){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改状态失败");
        }
        // 3.更新session中的用户信息
        User updatedUser = userService.getById(loginUser.getId());
        httpServletRequest.getSession().setAttribute(LOGIN_USER, updatedUser);
        // 4.返回更新后的用户信息
        return ResultUtils.success(userService.entityToVO(updatedUser));
    }

    /**
     * 获取当前登录用户信息
     */
    @LoginCheck
    @GetMapping("/getCurrentUserInfo")
    public BaseResponse<UserVO> getCurrentUserInfo(HttpServletRequest httpServletRequest){
        // 1.获取当前登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 2.获取用户信息
        UserVO userVO = userService.getCurrentUserInfo(loginUser.getId());
        // 3.返回用户信息
        return ResultUtils.success(userVO);
    }
}
