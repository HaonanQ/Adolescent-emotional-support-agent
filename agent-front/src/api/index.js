import axios from 'axios';

const API_BASE_URL = 'http://localhost:8123/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 用户登录
export const login = (username, password) => {
  return api.post('/emotionagent/user/login', {
    username,
    password,
  });
};

// 用户注册
export const register = (username, password) => {
  return api.post('/emotionagent/user/register', {
    username,
    password,
  });
};

// 用户登出
export const logout = () => {
  return api.get('/emotionagent/user/logout');
};

// 获取当前登录用户
export const getLoginUser = () => {
  return api.get('/emotionagent/user/getLoginUser');
};

// 创建聊天会话
export const createChatSession = (chatId) => {
  return api.post('/support/createChatSession', {
    chatId,
  });
};

// 获取聊天会话列表
export const getChatSessionList = (chatId) => {
  return api.post('/support/getChatSessionList', {
    chatId,
  });
};

// 获取聊天消息
export const getChatMessageBySessionId = (sessionId, chatId) => {
  return api.post('/support/getChatMessageBySessionId', {
    sessionId,
    chatId,
  });
};

// 获取最新聊天记录
export const getLatestChatHistory = (chatId) => {
  return api.post('/support/getLatestChatHistory', {
    chatId,
  });
};

// AI对话（普通响应）
export const chatWithRag = (message, chatId, sessionId) => {
  return api.post('/support/chat/rag', {
    message,
    chatId,
    sessionId,
  }, {
    responseType: 'text',
  });
};

/**
 * AI对话（流式响应）
 * @param {string} message - 用户消息
 * @param {string} chatId - 用户ID
 * @param {string} sessionId - 会话ID
 * @param {function} onChunk - 收到数据块时的回调函数
 * @returns {Promise<string>} 完整的AI回复
 */
export const chatWithRagStream = async (message, chatId, sessionId, onChunk) => {
  try {
    const response = await fetch(`${API_BASE_URL}/support/chat/rag`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        message,
        chatId,
        sessionId,
      }),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let fullText = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      
      const chunk = decoder.decode(value, { stream: true });
      fullText += chunk;
      onChunk(fullText);
    }

    return fullText;
  } catch (error) {
    console.error('流式响应错误:', error);
    throw error;
  }
};

// 删除聊天会话（完全删除会话和所有消息）
export const deleteChatSession = (sessionId, chatId) => {
  return api.post('/support/deleteChatSession', {
    sessionId,
    chatId,
  });
};

// 清空会话消息（只删除消息，保留会话）
export const deleteChatSessionBySessionId = (sessionId, chatId) => {
  return api.post('/support/deleteChatSessionBySessionId', {
    sessionId,
    chatId,
  });
};

// 获取用户文件列表
export const getUserFileList = (chatId) => {
  return api.post('/support/getUserFile', {
    chatId,
  });
};

// 上传图片
export const uploadImage = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/support/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

// 上传语音
export const uploadAudio = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/support/upload/audio', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

// 发送包含图片的消息（流式响应）
export const chatWithImage = async (file, message, chatId, sessionId, onChunk) => {
  const formData = new FormData();
  formData.append('file', file);
  if (message) {
    formData.append('message', message);
  }
  formData.append('chatId', chatId);
  formData.append('sessionId', sessionId);
  
  try {
    const response = await fetch(`${API_BASE_URL}/support/chat/image`, {
      method: 'POST',
      credentials: 'include',
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let fullText = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      
      const chunk = decoder.decode(value, { stream: true });
      fullText += chunk;
      onChunk(fullText);
    }

    return fullText;
  } catch (error) {
    console.error('发送图片消息错误:', error);
    throw error;
  }
};

// 语音转文字
export const transcribeSpeech = (file, chatId, sessionId) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('chatId', chatId);
  formData.append('sessionId', sessionId);
  return api.post('/support/speech/transcribe', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

// 发送包含音频的消息（流式响应）
export const chatWithAudio = async (file, message, chatId, sessionId, onChunk) => {
  const formData = new FormData();
  formData.append('file', file);
  if (message) {
    formData.append('message', message);
  }
  formData.append('chatId', chatId);
  formData.append('sessionId', sessionId);
  
  try {
    const response = await fetch(`${API_BASE_URL}/support/chat/audio`, {
      method: 'POST',
      credentials: 'include',
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let fullText = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      
      const chunk = decoder.decode(value, { stream: true });
      fullText += chunk;
      onChunk(fullText);
    }

    return fullText;
  } catch (error) {
    console.error('发送音频消息错误:', error);
    throw error;
  }
};

// 添加情绪日记
export const addEmotionDiary = (data) => {
  return api.post('/emotionDiary/add', data);
};

// 获取情绪日记列表
export const getEmotionDiaryList = () => {
  return api.post('/emotionDiary/list');
};

// 获取单条情绪日记
export const getEmotionDiaryById = (id) => {
  return api.post('/emotionDiary/getById', { id });
};

// 删除情绪日记
export const deleteEmotionDiary = (data) => {
  return api.post('/emotionDiary/delete', data);
};

export default api;
