import axios from 'axios';

const API_BASE_URL = '/api';

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
  withCredentials: true,
});

request.interceptors.request.use(
  (config) => {
    const user = JSON.parse(localStorage.getItem('user'));
    if (user && user.token) {
      config.headers.Authorization = user.token;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 0) {
      console.error('请求错误:', res.message);
      if (res.code === 401) {
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
    }
    return res;
  },
  (error) => {
    console.error('响应错误:', error);
    return Promise.reject(error);
  }
);

export const login = (username, password) => {
  return request.post('/emotionagent/user/login', { username, password });
};

export const register = (username, password) => {
  return request.post('/emotionagent/user/register', { username, password });
};

export const logout = () => {
  return request.get('/emotionagent/user/logout');
};

export const getLoginUser = () => {
  return request.get('/emotionagent/user/getLoginUser');
};

export const createChatSession = (chatId) => {
  return request.post('/support/createChatSession', { chatId });
};

export const getChatSessionList = (chatId) => {
  return request.post('/support/getChatSessionList', { chatId });
};

export const getChatMessageBySessionId = (sessionId, chatId) => {
  return request.post('/support/getChatMessageBySessionId', { sessionId, chatId });
};

export const getLatestChatHistory = (chatId) => {
  return request.post('/support/getLatestChatHistory', { chatId });
};

export const chatWithRagStream = async (message, chatId, sessionId, nickname, sex, onChunk) => {
  try {
    const response = await fetch(`${API_BASE_URL}/support/chat/rag`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ message, chatId, sessionId, nickname, sex }),
    });

    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

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

export const deleteChatSession = (sessionId, chatId) => {
  return request.post('/support/deleteChatSession', { sessionId, chatId });
};

export const getUserFileList = (chatId) => {
  return request.post('/support/getUserFile', { chatId });
};

export const uploadImage = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/support/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const chatWithImage = async (file, message, chatId, sessionId, nickname, sex, onChunk) => {
  const formData = new FormData();
  formData.append('file', file);
  if (message) formData.append('message', message);
  formData.append('chatId', chatId);
  formData.append('sessionId', sessionId);
  if (nickname) formData.append('nickname', nickname);
  if (sex !== undefined && sex !== null) formData.append('sex', sex);

  try {
    const response = await fetch(`${API_BASE_URL}/support/chat/image`, {
      method: 'POST',
      credentials: 'include',
      body: formData,
    });

    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

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

export const chatWithAudio = async (file, message, chatId, sessionId, nickname, sex, onChunk) => {
  const formData = new FormData();
  formData.append('file', file);
  if (message) formData.append('message', message);
  formData.append('chatId', chatId);
  formData.append('sessionId', sessionId);
  if (nickname) formData.append('nickname', nickname);
  if (sex !== undefined && sex !== null) formData.append('sex', sex);

  try {
    const response = await fetch(`${API_BASE_URL}/support/chat/audio`, {
      method: 'POST',
      credentials: 'include',
      body: formData,
    });

    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

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

export const addEmotionDiary = (data) => {
  return request.post('/emotionDiary/add', data);
};

export const getEmotionDiaryList = () => {
  return request.post('/emotionDiary/list');
};

export const deleteEmotionDiary = (data) => {
  return request.post('/emotionDiary/delete', data);
};

export const getCurrentUserInfo = () => {
  return request.get('/emotionagent/user/getCurrentUserInfo');
};

export const updateNickname = (nickname) => {
  return request.post('/emotionagent/user/updateNickname', { nickname });
};

export const updatePassword = (oldPassword, newPassword) => {
  return request.post('/emotionagent/user/updatePassword', { oldPassword, newPassword });
};

export const updateAvatar = (avatar) => {
  return request.post('/emotionagent/user/updateAvatar', { avatar });
};

export const updateSex = (sex) => {
  return request.post('/emotionagent/user/updateSex', { sex });
};

/**
 * 情感课堂相关接口
 */
export const getArticleList = () => {
  return request.post('/emotionArticle/list');
};

export const getArticleDetail = (id) => {
  return request.post('/emotionArticle/detail', { id });
};

export const getAdminArticleList = () => {
  return request.post('/emotionArticle/admin/list');
};

export const getAdminArticleDetail = (id) => {
  return request.post('/emotionArticle/admin/detail', { id });
};

export const addArticle = (data) => {
  return request.post('/emotionArticle/admin/add', data);
};

export const updateArticle = (data) => {
  return request.post('/emotionArticle/admin/update', data);
};

export const deleteArticle = (id) => {
  return request.post('/emotionArticle/admin/delete', { id });
};

/**
 * 管理员用户管理相关接口
 */
export const getUserList = (params) => {
  return request.post('/admin/user/list', params || {});
};

export const getEmotionHistory = (userId) => {
  return request.post('/admin/user/emotion/history', null, { params: { userId } });
};

export const toggleUserStatus = (userId) => {
  return request.post('/admin/user/toggleStatus', null, { params: { userId } });
};

/**
 * 知识库管理相关接口
 */
export const getKnowledgeBaseList = () => {
  return request.post('/knowledgeBase/list');
};

export const addKnowledgeBase = (data) => {
  return request.post('/knowledgeBase/add', data);
};

export const updateKnowledgeBase = (data) => {
  return request.post('/knowledgeBase/update', data);
};

export const deleteKnowledgeBase = (id) => {
  return request.post('/knowledgeBase/delete', null, { params: { id } });
};

export const updateKnowledgeBaseStatus = (id, status) => {
  return request.post('/knowledgeBase/updateStatus', null, { params: { id, status } });
};

export const updateKnowledgeBaseAutoLoad = (id, autoLoad) => {
  return request.post('/knowledgeBase/updateAutoLoad', null, { params: { id, autoLoad } });
};

export const hotReloadKnowledgeBase = (id) => {
  return request.post('/knowledgeBase/hotReload', null, { params: { id } });
};

export const getDocumentList = (knowledgeBaseId) => {
  return request.post('/knowledgeBase/document/list', null, { params: { knowledgeBaseId } });
};

export const deleteDocument = (id) => {
  return request.post('/knowledgeBase/document/delete', null, { params: { id } });
};

export const updateDocumentStatus = (id, status) => {
  return request.post('/knowledgeBase/document/updateStatus', null, { params: { id, status } });
};

export const downloadDocument = (id) => {
  return `/api/knowledgeBase/document/download?id=${id}`;
};

export const batchUpdateDocumentStatus = (ids, status) => {
  return request.post('/knowledgeBase/document/batchUpdateStatus', { ids, status });
};

export const batchDeleteDocuments = (ids) => {
  return request.post('/knowledgeBase/document/batchDelete', { ids });
};

export default request;
