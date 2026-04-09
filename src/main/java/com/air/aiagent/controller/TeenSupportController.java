package com.air.aiagent.controller;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.air.aiagent.annotation.ClearContext;
import com.air.aiagent.annotation.LoginCheck;
import com.air.aiagent.annotation.CheckLoginwithChat;
import com.air.aiagent.app.TeenSupportApp;
import com.air.aiagent.common.BaseResponse;
import com.air.aiagent.common.ResultUtils;
import com.air.aiagent.context.UserContext;
import com.air.aiagent.domain.dto.ChatRequest;
import com.air.aiagent.domain.entity.ChatMessage;
import com.air.aiagent.domain.entity.ChatSession;
import com.air.aiagent.domain.entity.MessageMetadata;
import com.air.aiagent.domain.entity.MessageType;
import com.air.aiagent.domain.entity.User;
import com.air.aiagent.domain.vo.*;
import com.air.aiagent.exception.BusinessException;
import com.air.aiagent.exception.ErrorCode;
import com.air.aiagent.manage.CosManager;
import com.air.aiagent.service.UserFileService;
import com.air.aiagent.service.UserService;
import com.air.aiagent.service.impl.ChatMessageService;
import com.air.aiagent.service.impl.ChatSessionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

import com.air.aiagent.utils.SessionIdGenerator;

/**
 * 青少年情感陪伴应用控制器
 * 
 * @author AI Assistant
 */
@Slf4j
@RestController
@RequestMapping("/support")
@Tag(name = "智能体对话接口", description = "用于用大模型对话")
public class TeenSupportController {

    @Resource
    private TeenSupportApp teenSupportApp;

    @Resource
    private UserService userService;

    @Resource
    private UserFileService userFileService;
    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ChatMessageService chatMessageService;


    @Resource
    private CosManager cosManager;


    /**
     * RAG知识库对话，支持工具调用
     */
    @CheckLoginwithChat
    @PostMapping(value = "/chat/rag", produces = "text/html;charset=UTF-8")
    @ClearContext
    public Flux<String> chatWithRag(@RequestBody ChatRequest request, HttpServletRequest httpServletRequest) {
        log.info("收到RAG知识库对话请求: {}", request);
        User loginUser = userService.getLoginUser(httpServletRequest);
        UserContext.setUserId(request.getChatId());
        UserContext.setSessionId(request.getSessionId());
        Optional<ChatSession> session = chatSessionService.findById(request.getSessionId());
        if (!session.isPresent()) {
            // sessionId 不存在
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的 sessionId");
        }
        request.setNickname(loginUser.getNickname());
        return teenSupportApp.smartChat(request, MessageType.TEXT);
    }

    @CheckLoginwithChat
//    @PostMapping("/game/emo")
    public String gameEmo(@RequestBody ChatRequest request) {
        log.info("收到判断情绪请求: {}", request);
        UserContext.setUserId(request.getChatId());
        UserContext.setSessionId(request.getSessionId());
        return teenSupportApp.doChatWithEmo(request.getMessage(), request.getChatId());
    }

    @CheckLoginwithChat
//    @PostMapping(value = "/game/chat", produces = "text/html;charset=UTF-8")
    public Flux<String> gameChat(@RequestBody ChatRequest request) {
        log.info("收到游戏请求: {}", request);
        UserContext.setUserId(request.getChatId());
        UserContext.setSessionId(request.getSessionId());
        return teenSupportApp.gameStreamChat(request.getMessage(), request.getChatId());
    }

    /**
     * 获取用户文件
     */
    @CheckLoginwithChat
    @PostMapping("/getUserFile")
    public BaseResponse<List<UserFileVO>> getUserFileList(@RequestBody ChatRequest request, HttpServletRequest httpServletRequest){
        User loginUser = userService.getLoginUser(httpServletRequest);
        List<UserFileVO> userFileList = userFileService.getUserFileList(loginUser.getId());
        return ResultUtils.success(userFileList);
    }

    /**
     * 查询最新的会话历史，也就是进入聊天页面之后，默认进行展示的聊天历史
     */
    @CheckLoginwithChat
    @PostMapping("/getLatestChatHistory")
    public BaseResponse<ChatHistory> getLatestChatSession(@RequestBody ChatRequest request, HttpServletRequest httpServletRequest) {
        // 1.获取当前登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);

        // 2.创建返回对象
        ChatHistory chatHistory = new ChatHistory();

        // 3.获取当前用户的最新会话记录
        ChatSession latestChatSession = chatSessionService.getLatestSessionByUserId(loginUser.getId().toString());
        if (latestChatSession == null) {
            // 如果不存在最新的会话历史，直接返回一个空对象
            return ResultUtils.success(chatHistory);
        }

        // 4.存在最新的会话，设置会话Id
        chatHistory.setSessionId(latestChatSession.getId());

        // 5.根据会话Id 查找该记录中的聊天历史，包含最新消息在内的最近10条记录，转换成 VO
        List<ChatMessage> latestChatMessageList = chatMessageService
                .findHistoryExcludingLatest(latestChatSession.getId(), 10, 0);
        List<ChatMessageVO> latestChatMessageVOList = latestChatMessageList.stream().map(chatMessage -> {
            ChatMessageVO chatMessageVO = ChatMessageVO.builder()
                    .id(chatMessage.getId())
                    .chatId(chatMessage.getChatId())
                    .sessionId(chatMessage.getSessionId())
                    .messageType(chatMessage.getMessageType())
                    .content(chatMessage.getContent())
                    .isAiResponse(chatMessage.getIsAiResponse())
                    .build();
            if(chatMessage.getMetadata() != null) {

                if(chatMessage.getMetadata().getPdfFileUrl() != null){
                    chatMessageVO.setPdfFileUrl(chatMessage.getMetadata().getPdfFileUrl());
                    chatMessageVO.setPdfFileName(chatMessage.getMetadata().getPdfFileName());
                }
                if(chatMessage.getMetadata().getImageFileUrl() != null){
                    chatMessageVO.setImageFileUrl(chatMessage.getMetadata().getImageFileUrl());
                    chatMessageVO.setImageFileName(chatMessage.getMetadata().getImageFileName());
                }
                if(chatMessage.getMetadata().getAudioFileUrl() != null){
                    chatMessageVO.setAudioFileUrl(chatMessage.getMetadata().getAudioFileUrl());
                    chatMessageVO.setAudioFileName(chatMessage.getMetadata().getAudioFileName());
                }
            }
            return chatMessageVO;
        }).toList();

        // 6.进行封装返回
        chatHistory.setChatMessageVOList(latestChatMessageVOList);
        return ResultUtils.success(chatHistory);
    }


    /**
     * 根据 sessionId 查询该会话的聊天记录
     */
    @CheckLoginwithChat
    @PostMapping("/getChatMessageBySessionId")
    public BaseResponse<ChatHistory> getChatMessageBySessionId(@RequestBody ChatRequest request,
            HttpServletRequest httpServletRequest) {
        // 1.获取当前登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);

        // 2.创建返回对象
        ChatHistory chatHistory = new ChatHistory();

        // 3.判断当前用户是否存在该 会话Id
        String sessionId = request.getSessionId();
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话Id为空，请选择会话");
        }

        // 4.判断当前用户是否存在该会话Id，不存在的话抛出异常
        ChatSession chatSession = chatSessionService.findByIdAndChatId(sessionId, loginUser.getId().toString())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "当前用户不存在该会话"));

        // 5.存在最新的会话，设置会话Id
        chatHistory.setSessionId(chatSession.getId());

        // 6.根据会话Id 查找该记录中的聊天历史，包含最新消息在内的最近100条记录，转换成 VO
        List<ChatMessage> latestChatMessageList = chatMessageService
                .findHistoryExcludingLatest(chatSession.getId(), 100, 0);
        List<ChatMessageVO> latestChatMessageVOList = latestChatMessageList.stream().map(chatMessage -> {
            ChatMessageVO chatMessageVO = ChatMessageVO.builder()
                    .id(chatMessage.getId())
                    .chatId(chatMessage.getChatId())
                    .sessionId(chatMessage.getSessionId())
                    .messageType(chatMessage.getMessageType())
                    .content(chatMessage.getContent())
                    .isAiResponse(chatMessage.getIsAiResponse())
                    .build();
            if(chatMessage.getMetadata() != null) {

                if(chatMessage.getMetadata().getPdfFileUrl() != null){
                    chatMessageVO.setPdfFileUrl(chatMessage.getMetadata().getPdfFileUrl());
                    chatMessageVO.setPdfFileName(chatMessage.getMetadata().getPdfFileName());
                }
                if(chatMessage.getMetadata().getImageFileUrl() != null){
                    chatMessageVO.setImageFileUrl(chatMessage.getMetadata().getImageFileUrl());
                    chatMessageVO.setImageFileName(chatMessage.getMetadata().getImageFileName());
                }
                if(chatMessage.getMetadata().getAudioFileUrl() != null){
                    chatMessageVO.setAudioFileUrl(chatMessage.getMetadata().getAudioFileUrl());
                    chatMessageVO.setAudioFileName(chatMessage.getMetadata().getAudioFileName());
                }
            }
            return chatMessageVO;
        }).toList();

        // 7.进行封装返回
        chatHistory.setChatMessageVOList(latestChatMessageVOList);
        return ResultUtils.success(chatHistory);
    }


    /**
     * 创建会话返回 sessionId ，并将会话记录保存到数据库中，之后的用户发送的消息必须携带这个生成的 sessionId
     */
    @CheckLoginwithChat
    @PostMapping("/createChatSession")
    public BaseResponse<String> createChatSession(@RequestBody ChatRequest request, HttpServletRequest httpServletRequest) {
        // 1.获取当前登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);

        // 2.创建会话Id (时间戳 + "_" + 用户Id)
        String sessionId = SessionIdGenerator.generateSessionId(loginUser.getId());

        // 3.保存会话记录到 MongoDB 中
        ChatSession chatSession = ChatSession.builder()
                .id(sessionId)
                .chatId(loginUser.getId().toString())
                .sessionName("新对话")
                .build();
        chatSessionService.save(chatSession);

        // 3.返回会话Id
        return ResultUtils.success(sessionId);
    }


    /**
     * 查询用户的所有会话历史，返回 sessionList ，用于在页面左侧列表展示会话历史
     * 后面可以完善一下，例如只查询一些比较活跃的 session
     */
    @CheckLoginwithChat
    @PostMapping("/getChatSessionList")
    public BaseResponse<List<ChatSessionVO>> getChatSessionList(@RequestBody ChatRequest request,
                                                                HttpServletRequest httpServletRequest) {
        // 1.根据 用户Id 查询所有会话记录
        List<ChatSession> chatSessionList = chatSessionService.findByChatId(request.getChatId());
        if (chatSessionList.isEmpty()) {
            return ResultUtils.success(List.of());
        }
        // 2.转换成 ChatSessionVO 集合
        List<ChatSessionVO> list = chatSessionList.stream()
                .map(chatSession -> BeanUtil.copyProperties(chatSession, ChatSessionVO.class)).toList();
        return ResultUtils.success(list);
    }


    /**
     * 删除会话（包括会话记录和所有聊天消息）
     */
    @CheckLoginwithChat
    @PostMapping("/deleteChatSession")
    public BaseResponse<Boolean> deleteChatSession(@RequestBody ChatRequest request,
            HttpServletRequest httpServletRequest) {
        // 1.获取当前登录用户
        Long chatId = userService.getLoginUser(httpServletRequest).getId();
        String sessionId = request.getSessionId();

        // 2.判断是否有传 sessionId 参数
        if (StrUtil.isBlank(sessionId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择会话");
        }

        // 3.验证会话是否存在且属于当前用户
        ChatSession currentChatSession = chatSessionService.getSessionByChatIdAndSessionId(chatId.toString(),
                sessionId);
        if (currentChatSession == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前用户不存在该会话");
        }

        // 4.删除该会话中的所有聊天记录
        long deletedMessages = chatMessageService.deleteBySessionId(sessionId);
        log.info("删除会话消息，sessionId={}，删除消息数量={}", sessionId, deletedMessages);

        // 5.删除该会话
        long deletedSession = chatSessionService.deleteSession(sessionId, chatId.toString());

        // 6.判断删除结果
        if (deletedSession > 0) {
            log.info("会话删除成功，sessionId={}，删除消息数={}，删除会话数={}",
                    sessionId, deletedMessages, deletedSession);
            return ResultUtils.success(true);
        } else {
            log.warn("会话删除失败，sessionId={}，未找到对应会话", sessionId);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败，会话可能已被删除");
        }
    }


    /**
     * 清空会话中的所有聊天记录（保留会话本身）
     */
    @CheckLoginwithChat
    @PostMapping("/deleteChatSessionBySessionId")
    public BaseResponse<Boolean> deleteChatSessionBySessionId(@RequestBody ChatRequest request,
            HttpServletRequest httpServletRequest) {
        // 1.获取当前登录用户
        Long chatId = userService.getLoginUser(httpServletRequest).getId();
        String sessionId = request.getSessionId();

        // 2.判断是否有传 sessionId 参数
        if (StrUtil.isBlank(sessionId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择会话");
        }

        // 3.验证会话是否存在且属于当前用户
        ChatSession currentChatSession = chatSessionService.getSessionByChatIdAndSessionId(chatId.toString(),
                sessionId);
        if (currentChatSession == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前用户不存在该会话");
        }

        // 4.删除该会话中的所有聊天记录（保留会话本身）
        long deletedMessages = chatMessageService.deleteBySessionId(sessionId);

        // 5.判断删除结果并返回
        if (deletedMessages > 0) {
            log.info("清空会话消息成功，sessionId={}，删除消息数={}", sessionId, deletedMessages);
            return ResultUtils.success(true);
        } else {
            log.info("会话消息为空，无需清空，sessionId={}", sessionId);
            return ResultUtils.success(true);
        }
    }
    /**
     * 以下注释为备用接口，不能删除或改动！
     */
    // POST http://localhost:8123/api/love/game/emo
    // 请求体：{ message: 用户输入, chatId: '23034480211' }
    // @PostMapping("/game/emo")
    // public String gameEmo(@RequestBody ChatRequest request) {
    // log.info("收到判断情绪请求: {}", request);
    // UserContext.setUserId(request.getChatId());
    // return loveApp.doChatWithEmo(request.getMessage(), request.getChatId());
    // }
    // 这个接口返回的是一个情绪
    // 你现在添加一个逻辑，在进入游戏页面之前可以有一个符合当前主题的弹窗可以给用户输入女朋友不开心的原因，然后后端会返回的是一个情绪
    // 你根据返回的情绪渲染不同的颜色效果的游戏界面

     /**
     * 工具调用对话接口
     */
//     @ClearContext //方法执行后清理内存中 ThreadLocal ，防止内存泄露
//     @PostMapping("/chat/tools")
//     public ChatResponse chatWithTools(@RequestBody ChatRequest request) {
//     log.info("收到工具调用对话请求: {}", request);
//     UserContext.setUserId(request.getChatId());
//     String response = TeenSupportApp.doChatWithTools(request.getMessage(),
//     request.getChatId());
//     return new ChatResponse(response);
//     }
    //
    //
    //
    // /**
    // * 基础对话接口
    // */
    // @PostMapping("/chat")
    // public ChatResponse chat(@RequestBody ChatRequest request) {
    // log.info("收到对话请求: {}", request);
    // String response = loveApp.doChat(request.getMessage(), request.getChatId());
    // return new ChatResponse(response);
    // }
    //
    //
    // /**
    // * 恋爱报告生成接口
    // */
    // @PostMapping("/report")
    // public LoveReportResponse generateReport(@RequestBody ChatRequest request) {
    // log.info("收到恋爱报告生成请求: {}", request);
    // LoveApp.LoveReport report = loveApp.doChatWithReport(request.getMessage(),
    // request.getChatId());
    // return new LoveReportResponse(report.title(), report.suggestions());
    // }
    //
    //
    // /**
    //  * MCP服务调用接口
    //  */
    // @PostMapping("/chat/mcp")
    // public ChatResponse chatWithMCP(@RequestBody ChatRequest request) {
    // log.info("收到MCP服务调用请求: {}", request);
    // String response = loveApp.doChatWithMCP(request.getMessage(),
    // request.getChatId());
    // return new ChatResponse(response);
    // }

    /**
     * 上传图片文件
     */
    @LoginCheck
    @PostMapping("/upload/image")
    public BaseResponse<UploadFileVO> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);

        String fileName = IdUtil.simpleUUID() + "_" + file.getOriginalFilename();
        String objectPath = "public/images/" + loginUser.getId() + '/' + fileName;

        String fileUrl = cosManager.uploadImage(file, objectPath);

        UploadFileVO vo = UploadFileVO.builder()
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .objectPath(objectPath)
                .build();

        return ResultUtils.success(vo);
    }

    /**
     * 上传语音文件
     */
//    @LoginCheck
//    @PostMapping("/upload/audio")
//    public BaseResponse<UploadFileVO> uploadAudio(
//            @RequestParam("file") MultipartFile file,
//            HttpServletRequest httpServletRequest) {
//        User loginUser = userService.getLoginUser(httpServletRequest);
//
//        String fileName = IdUtil.simpleUUID() + "_" + file.getOriginalFilename();
//        String objectPath = "public/audio/" + fileName;
//
//        String fileUrl = cosManager.uploadAudio(file, objectPath);
//
//        UploadFileVO vo = UploadFileVO.builder()
//                .fileUrl(fileUrl)
//                .fileName(file.getOriginalFilename())
//                .objectPath(objectPath)
//                .build();
//
//        return ResultUtils.success(vo);
//    }

    /**
     * 发送包含图片的消息
     */
    @CheckLoginwithChat
    @PostMapping(value = "/chat/image", produces = "text/html;charset=UTF-8")
    @ClearContext
    public Flux<String> chatWithImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam("sessionId") String sessionId,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        String chatId = String.valueOf(loginUser.getId());
        
        // 上传图片
        String fileName = IdUtil.simpleUUID() + "_" + file.getOriginalFilename();
        String objectPath = "public/images/" + loginUser.getId() + '/' + fileName;
        String imageUrl = cosManager.uploadImage(file, objectPath);
        
        // 构建请求
        ChatRequest request = new ChatRequest();
        request.setChatId(chatId);
        request.setSessionId(sessionId);
        request.setMessage(message != null ? message : "");
        request.setImageUrl(imageUrl);
        request.setImageFileName(file.getOriginalFilename());
        request.setNickname(loginUser.getNickname());
        
        UserContext.setUserId(chatId);
        UserContext.setSessionId(sessionId);
        Optional<ChatSession> session = chatSessionService.findById(sessionId);
        if (!session.isPresent()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的 sessionId");
        }
        
        // 保存用户消息（带图片）
        String userMessageId = UUID.randomUUID().toString();
        ChatMessage userMessage = ChatMessage.builder()
                .id(userMessageId)
                .chatId(chatId)
                .sessionId(sessionId)
                .messageType(MessageType.IMAGE)
                .content(message != null ? message : "")
                .isAiResponse(false)
                .metadata(MessageMetadata.builder()
                        .imageFileUrl(imageUrl)
                        .imageFileName(file.getOriginalFilename())
                        .build())
                .build();
        chatMessageService.save(userMessage);
        log.info("用户图片消息已保存，sessionId={}, imageUrl={}", sessionId, imageUrl);
        
        // 使用图像对话方法，图片信息已保存到数据库
        return teenSupportApp.smartChat(request,MessageType.IMAGE);
    }

    /**
     * 语音转文字
     */
//    @LoginCheck
//    @PostMapping("/speech/transcribe")
//    public BaseResponse<String> transcribeSpeech(
//            @RequestParam("file") MultipartFile file,
//            HttpServletRequest httpServletRequest) {
//        try {
//            User loginUser = userService.getLoginUser(httpServletRequest);
//
//            // 1. 定义临时文件存储目录（jar包同级的tmpaudio文件夹）
//            String tempDirPath = System.getProperty("user.dir") + File.separator + "tmpaudio";
//            File tempDir = new File(tempDirPath);
//            // 2. 确保目录存在，不存在则创建
//            if (!tempDir.exists()) {
//                boolean mkdirSuccess = tempDir.mkdirs(); // 递归创建目录
//                if (!mkdirSuccess) {
//                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建临时目录失败");
//                }
//            }
//
//            // 3. 在指定目录下创建临时文件（前缀+后缀，指定目录）
//            File tempFile = File.createTempFile("speech_", ".mp3", tempDir);
//            file.transferTo(tempFile);
//
//            // 4. 语音转文字（保留你的业务逻辑）
//            String text = speechToText.transcribeAudio(tempFile);
//            // text = "hello"; // 测试用代码，可注释
//
//            // 5. 用完删除临时文件（可选：也可定时清理，避免文件堆积）
////            tempFile.deleteOnExit(); // JVM退出时删除，兜底保障
////            boolean deleteSuccess = tempFile.delete();
////            if (!deleteSuccess) {
////                log.warn("临时文件删除失败，路径：{}", tempFile.getAbsolutePath());
////            }
//
//            return ResultUtils.success(text);
//        } catch (Exception e) {
//            log.error("语音识别失败", e);
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "语音识别失败: " + e.getMessage());
//        }
//    }

    /**
     * 发送包含音频的消息
     */
    @CheckLoginwithChat
    @PostMapping(value = "/chat/audio", produces = "text/html;charset=UTF-8")
    @ClearContext
    public Flux<String> chatWithAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam("sessionId") String sessionId,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        String chatId = String.valueOf(loginUser.getId());
        
        // 上传音频
        String fileName = IdUtil.simpleUUID() + "_" + file.getOriginalFilename();
        String objectPath = "public/audio/" + loginUser.getId() + '/' + fileName;
        String audioUrl = cosManager.uploadAudio(file, objectPath);
        
        // 构建请求
        ChatRequest request = new ChatRequest();
        request.setChatId(chatId);
        request.setSessionId(sessionId);
        request.setMessage(message != null ? message : "");
        request.setAudioUrl(audioUrl);
        request.setAudioFileName(file.getOriginalFilename());
        request.setNickname(loginUser.getNickname());
        
        UserContext.setUserId(chatId);
        UserContext.setSessionId(sessionId);
        Optional<ChatSession> session = chatSessionService.findById(sessionId);
        if (!session.isPresent()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的 sessionId");
        }
        
        // 保存用户消息（带音频）
        String userMessageId = UUID.randomUUID().toString();
        ChatMessage userMessageObj = ChatMessage.builder()
                .id(userMessageId)
                .chatId(chatId)
                .sessionId(sessionId)
                .messageType(MessageType.AUDIO)
                .content(message != null ? message : "")
                .isAiResponse(false)
                .metadata(MessageMetadata.builder()
                        .audioFileUrl(audioUrl)
                        .audioFileName(fileName)
                        .build())
                .build();
        chatMessageService.save(userMessageObj);
        log.info("用户音频消息已保存，sessionId={}, audioUrl={}", sessionId, audioUrl);
        
        // 使用音频对话方法，音频信息已保存到数据库
        return teenSupportApp.smartChat(request, MessageType.AUDIO);
    }


}
