//package com.air.aiagent.aop;
//import com.air.aiagent.annotation.LoginCheck;
//import com.air.aiagent.domain.dto.ChatRequest;
//import com.air.aiagent.domain.entity.User;
//import com.air.aiagent.exception.BusinessException;
//import com.air.aiagent.exception.ErrorCode;
//import com.air.aiagent.service.UserService;
//import jakarta.annotation.Resource;
//import jakarta.servlet.http.HttpServletRequest;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.springframework.stereotype.Component;
//import org.springframework.web.context.request.RequestAttributes;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
///**
// *
// * @since 2025/10/10 19:01
// */
//@Aspect  // 声明这是一个切面类
//@Component  // 由 Spring 容器管理
//public class LoginCheckInterceptor {
//
//    @Resource
//    private UserService userService;  // 可注入其他依赖
//
//    /**
//     * 执行拦截，用户登录了才可以访问，且前端传来的 userId 与 session 存的 userId 一致
//     *
//     * @param joinPoint 切入点
//     * 这是一个切点，就是你想要在哪些地方去执行这里面的代码
//     */
//    @Around("@annotation(loginCheck)")
//    public Object checkLogin(ProceedingJoinPoint joinPoint, LoginCheck loginCheck) throws Throwable {
//        // 1.直接获取当前 HTTP 请求的 HttpServletRequest 对象
//        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
//        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
//
//        // 2.获取存储到 session 中的用户信息，这个方法里面如果未获取到是会报错的
//        User user = userService.getLoginUser(request);
//
//        // 3.判断获取到的 userId 与前端传过来的 userId 是否一致
//        Object[] args = joinPoint.getArgs();
//        ChatRequest chatRequest =(ChatRequest)args[0];
//        if(chatRequest == null || chatRequest.getChatId() == null){
//            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数错误");
//        }
//        if(!Long.valueOf(chatRequest.getChatId()).equals(user.getId())){
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户登录信息错误");
//        }
//
//        // 4.放行
//        return joinPoint.proceed();
//    }
//}
package com.air.aiagent.aop;

import com.air.aiagent.annotation.LoginCheck;
import com.air.aiagent.domain.dto.ChatRequest;
import com.air.aiagent.domain.entity.User;
import com.air.aiagent.exception.BusinessException;
import com.air.aiagent.exception.ErrorCode;
import com.air.aiagent.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Enumeration;
import java.util.Map;

/**
 * 登录校验切面
 *
 * @since 2025/10/10 19:01
 */
@Slf4j
@Aspect  // 声明这是一个切面类
@Component  // 由 Spring 容器管理
public class LoginCheckInterceptor {

    @Resource
    private UserService userService;  // 可注入其他依赖

    // JSON解析器，用于解析请求中的ChatRequest参数
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行拦截，用户登录了才可以访问，且前端传来的 userId 与 session 存的 userId 一致
     *
     * @param joinPoint 切入点
     * @param loginCheck 自定义注解
     */
    @Around("@annotation(loginCheck)")
    public Object checkLogin(ProceedingJoinPoint joinPoint, LoginCheck loginCheck) throws Throwable {
        // 1. 获取当前 HTTP 请求对象
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 2. 获取登录用户信息
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        // 3. 灵活提取 chatId（核心修复：兼容不同参数类型）
        String chatId = extractChatId(joinPoint, request);
        if (chatId == null || chatId.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误：缺少chatId");
        }

        // 4. 校验 chatId 与登录用户ID是否一致
        try {
            Long chatUserId = Long.valueOf(chatId);
            if (!chatUserId.equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户登录信息错误：chatId与登录用户不匹配");
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误：chatId必须是数字");
        }

        // 5. 放行，执行原方法
        return joinPoint.proceed();
    }

    /**
     * 从请求中灵活提取 chatId
     * 兼容场景：
     * 1. 参数是 ChatRequest 对象
     * 2. 文件上传请求（Multipart）
     * 3. 请求参数/表单参数中包含 chatId
     * 4. JSON请求体中包含 chatId
     */
    private String extractChatId(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        // 场景1：先尝试从切入点参数中获取 ChatRequest
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof ChatRequest chatRequest) {
                log.debug("从ChatRequest参数中提取chatId: {}", chatRequest.getChatId());
                return chatRequest.getChatId();
            }
        }

        // 场景2：文件上传请求（Multipart）
        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            // 从表单参数中获取 chatId
            String chatId = multipartRequest.getParameter("chatId");
            if (chatId != null) {
                log.debug("从Multipart请求参数中提取chatId: {}", chatId);
                return chatId;
            }
            // 从表单参数中获取 ChatRequest JSON 字符串并解析
            String chatRequestJson = multipartRequest.getParameter("chatRequest");
            if (chatRequestJson != null) {
                try {
                    ChatRequest chatRequest = objectMapper.readValue(chatRequestJson, ChatRequest.class);
                    log.debug("从Multipart请求的chatRequest参数中提取chatId: {}", chatRequest.getChatId());
                    return chatRequest.getChatId();
                } catch (Exception e) {
                    log.warn("解析Multipart请求中的chatRequest参数失败", e);
                }
            }
        }

        // 场景3：普通请求参数/表单参数
        String chatId = request.getParameter("chatId");
        if (chatId != null) {
            log.debug("从请求参数中提取chatId: {}", chatId);
            return chatId;
        }

        // 场景4：其他参数类型（可根据实际业务扩展）
        for (Object arg : args) {
            // 如果有其他包含chatId的参数类型，可在此处处理
            log.debug("未识别的参数类型：{}", arg.getClass().getName());
        }

        // 所有场景都未找到chatId
        return null;
    }
}