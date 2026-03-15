package com.air.aiagent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.air.aiagent.domain.dto.AddUserRequest;
import com.air.aiagent.domain.dto.UserLoginRequest;
import com.air.aiagent.domain.vo.UserVO;
import com.air.aiagent.exception.BusinessException;
import com.air.aiagent.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.air.aiagent.domain.entity.User;
import com.air.aiagent.service.UserService;
import com.air.aiagent.mapper.UserMapper;
import com.air.aiagent.utils.BCryptUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.air.aiagent.constant.Constant.LOGIN_USER;
import static com.air.aiagent.constant.Constant.USER_NICK_NAME_PREFIX;

/**
* @author 30280
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2025-09-12 12:44:39
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{

    /**
     * 用户登录
     */
    @Override
    public UserVO login(UserLoginRequest request, HttpServletRequest httpServletRequest) {
        // 1.判断用户名是否为空
        if(StrUtil.isBlank(request.getUsername())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        // 2.通过用户名，判断用户是否存在
        User user = null;
        user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if(user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不存在，请前往注册");
        }
        // 3.密码登录
        if(StrUtil.isBlank(request.getPassword())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码不能为空");
        }
        if(!BCryptUtils.verify(request.getPassword(), user.getPassword())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码错误");
        }
        // 4.登录成功，存储登录态到 session 中
        httpServletRequest.getSession().setAttribute(LOGIN_USER, user);

        // 5.返回脱敏的用户信息
        return entityToVO(user);
    }

    /**
     * 用户注册
     */
    @Override
    public Boolean register(AddUserRequest request) {
        // 1.判断用户名是否为空
        if(StrUtil.isBlank(request.getUsername())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名不能为空");
        }
        // 2.判断密码是否为空
        if(StrUtil.isBlank(request.getPassword())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        // 3.判断该用户是否存在
        User user = null;
        user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if(user != null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户名已注册过账户，请前往登录");
        }

        // 4.创建用户，密码使用 BCrypt 加密
        String encryptedPassword = BCryptUtils.encrypt(request.getPassword());
        user = User.builder()
                .username(request.getUsername())
                .password(encryptedPassword)
                .build();
        return this.save(user);
    }

    /**
     * 获取当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1.从 session 中获取用户
        User user = (User)request.getSession().getAttribute(LOGIN_USER);
        // 2.判断用户是否登录
        if(user == null || user.getId() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        // 3.判断是否可以查询到所记录的用户
        User currentUser = getById(user.getId());
        if(currentUser == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        return currentUser;
    }

    /**
     * 脱敏，Entity 转换为 VO
     */
    @Override
    public UserVO entityToVO(User user){
        return BeanUtil.copyProperties(user, UserVO.class);
    }

    /**
     * 随机获取用户昵称
     */
    @Override
    public String getRandomNickName(){
        List<String> userNickName = USER_NICK_NAME_PREFIX;
        int index = RandomUtil.randomInt(0, userNickName.size());
        return userNickName.get(index);
    }

    /**
     * 更新用户昵称
     */
    @Override
    public Boolean updateNickname(Long userId, String nickname){
        // 1.校验参数
        if(userId == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        if(StrUtil.isBlank(nickname)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称不能为空");
        }
        // 2.校验昵称长度
        if(nickname.length() > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称长度不能超过20个字符");
        }
        // 3.查询用户是否存在
        User user = this.getById(userId);
        if(user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        // 4.更新昵称
        user.setNickname(nickname);
        return this.updateById(user);
    }
}




