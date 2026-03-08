<template>
  <div class="chat-container">
    <nav class="navbar">
      <div class="nav-left">
        <span class="nav-logo">❤️</span>
        <span class="nav-title">青少年情感陪伴智能体</span>
      </div>
      <div class="nav-center">
        <span class="nav-item">首页</span>
        <span class="nav-item active">情感陪伴</span>
        <span class="nav-item">情绪日记</span>
        <!-- 以下菜单项暂时隐藏 -->
        <!-- <span class="nav-item">心理FM</span>
        <span class="nav-item">心理学堂</span>
        <span class="nav-item">心理阅读</span>
        <span class="nav-item">心理论坛</span>
        <span class="nav-item">心理测试</span>
        <span class="nav-item">心理医生</span> -->
        <span class="nav-item">个人中心</span>
        <span class="nav-item">反馈与建议</span>
      </div>
      <div class="nav-right">
        <div class="user-avatar">{{ user.username?.charAt(0) || 'U' }}</div>
        <span class="user-name">{{ user.username }}</span>
        <button @click="handleLogout" class="logout-btn">退出</button>
      </div>
    </nav>

    <div class="chat-main">
      <aside class="sidebar">
        <div class="sidebar-section">
          <div class="sidebar-header">
            <h3>历史会话</h3>
            <button @click="createNewSession" class="new-session-btn" title="新建会话">
              ➕
            </button>
          </div>
          <div class="session-list">
            <div 
              v-for="session in sessions" 
              :key="session.id"
              :class="['session-item', { active: currentSessionId === session.id }]"
              @click="selectSession(session.id)"
            >
              <div class="session-title">{{ session.sessionName || '新会话' }}</div>
              <div class="session-time">{{ formatDate(session.updatedAt) }}</div>
              <button 
                @click.stop="deleteSession(session.id)"
                class="delete-session-btn"
                title="删除会话"
              >
                🗑️
              </button>
            </div>
          </div>
        </div>
        
        <div class="sidebar-section">
          <div class="sidebar-header">
            <h3>我的文档</h3>
          </div>
          <div class="file-list">
            <div v-if="userFiles.length === 0" class="empty-files">
              <span class="empty-files-icon">📄</span>
              <span class="empty-files-text">暂无文档</span>
            </div>
            <div 
              v-for="file in userFiles" 
              :key="file.id"
              class="file-item"
            >
              <a :href="file.fileUrl" target="_blank" download class="file-download-link">
                <span class="file-icon">📄</span>
                <span class="file-name">{{ file.fileName }}</span>
                <span class="file-download-icon">⬇️</span>
              </a>
              <div class="file-time">{{ formatDate(file.createTime) }}</div>
            </div>
          </div>
        </div>
      </aside>

      <main class="chat-area">
        <div class="messages-container" ref="messagesContainer">
          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">🤖</div>
            <p>你好！我是你的情感陪伴助手，有什么想和我聊聊的吗？</p>
          </div>
          <div 
            v-for="message in messages" 
            :key="message.id"
            :class="['message', message.isAiResponse ? 'ai-message' : 'user-message']"
          >
            <div class="message-avatar">
              {{ message.isAiResponse ? '🤖' : '👤' }}
            </div>
            <div class="message-content">
              <div v-if="message.imageFileUrl" class="image-container">
                <img :src="message.imageFileUrl" :alt="message.imageFileName || '图片'" class="message-image" />
              </div>
              <div v-if="message.audioFileUrl" class="audio-container">
                <audio :src="message.audioFileUrl" controls class="message-audio"></audio>
              </div>
              <div v-if="message.content" class="message-text markdown-content" v-html="formatMessage(message.content)"></div>
              <div v-if="message.recommendedProducts && message.recommendedProducts.length > 0" class="products-container">
                <div class="products-title">推荐内容：</div>
                <div class="products-list">
                  <div v-for="product in message.recommendedProducts" :key="product.productId" class="product-card">
                    <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.productName" />
                    <div class="product-name">{{ product.productName }}</div>
                    <div v-if="product.description" class="product-desc">{{ product.description }}</div>
                  </div>
                </div>
              </div>
              <div v-if="message.pdfFileUrl" class="pdf-container">
                <div class="pdf-title">📄 生成的文档：</div>
                <a :href="message.pdfFileUrl" target="_blank" download class="pdf-download-btn">
                  <span class="pdf-icon">⬇️</span>
                  <span class="pdf-name">{{ message.pdfFileName || '下载PDF' }}</span>
                </a>
              </div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <div class="input-tools">
            <input 
              type="file" 
              ref="imageInput" 
              accept="image/*" 
              style="display: none"
              @change="handleImageSelect"
            />
            <button @click="handleImageClick" class="tool-btn" title="上传图片">
              📷
            </button>
            <button 
              @click="handleAudioClick" 
              :class="['tool-btn', { recording: isRecording }]" 
              :title="isRecording ? '停止录音' : '语音输入'"
            >
              🎤
            </button>
          </div>
          <div class="input-main">
            <div v-if="selectedImage" class="selected-image-preview">
              <img :src="selectedImage.preview" :alt="selectedImage.name" class="preview-image" />
              <button @click="removeSelectedImage" class="remove-image-btn">×</button>
            </div>
            <textarea 
              v-model="inputMessage"
              placeholder="请输入您的问题..."
              @keydown.enter.prevent="handleSendMessage"
              rows="3"
            ></textarea>
          </div>
          <button 
            @click="handleSendMessage" 
            :disabled="isLoading || (!inputMessage.trim() && !selectedImage)" 
            class="send-btn"
          >
            发送
          </button>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { 
  getChatSessionList, 
  getChatMessageBySessionId, 
  getLatestChatHistory,
  createChatSession,
  chatWithRagStream,
  deleteChatSession,
  logout,
  getUserFileList,
  chatWithImage,
  transcribeSpeech,
  chatWithAudio
} from '../api';

const router = useRouter();
const user = ref(JSON.parse(localStorage.getItem('user')) || {});
const chatId = String(user.value.id);

const sessions = ref([]);
const currentSessionId = ref(null);
const messages = ref([]);
const userFiles = ref([]);
const inputMessage = ref('');
const isLoading = ref(false);
const messagesContainer = ref(null);
const selectedImage = ref(null);
const isRecording = ref(false);
const imageInput = ref(null);

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now - date;
  
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
  return date.toLocaleDateString();
};

// /**
//  * 格式化消息内容，支持Markdown渲染
//  * @param {string} content - 原始消息内容
//  * @returns {string} 渲染后的HTML内容
//  */
// const formatMessage = (content) => {
//   if (!content) return '';
//   try {
//     const htmlContent = marked.parse(content);
//     return DOMPurify.sanitize(htmlContent);
//   } catch (error) {
//     console.error('Markdown渲染错误:', error);
//     return content.replace(/\n/g, '<br>');
//   }
// };
/**
 * 格式化消息内容，支持Markdown渲染
 * @param {string} content - 原始消息内容
 * @returns {string} 渲染后的HTML内容
 */
const formatMessage = (content) => {
  if (!content) return '';
  try {
    // 关键修改：配置 marked 解析选项
    marked.setOptions({
      breaks: true, // 解析 \n 为 <br> 换行符
      gfm: true,   // 支持 GitHub Flavored Markdown（如表格、任务列表等）
      smartypants: true // 自动转换引号为智能引号，优化排版
    });
    const htmlContent = marked.parse(content);
    return DOMPurify.sanitize(htmlContent);
  } catch (error) {
    console.error('Markdown渲染错误:', error);
    // 降级处理：至少保证换行生效
    return content.replace(/\n/g, '<br>');
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
};

const loadSessions = async () => {
  try {
    const response = await getChatSessionList(chatId);
    if (response.data.code === 0 && response.data.data) {
      sessions.value = response.data.data;
    }
  } catch (error) {
    console.error('加载会话列表失败:', error);
  }
};

const loadUserFiles = async () => {
  try {
    const response = await getUserFileList(chatId);
    if (response.data.code === 0 && response.data.data) {
      userFiles.value = response.data.data;
    }
  } catch (error) {
    console.error('加载用户文件失败:', error);
  }
};

const loadMessages = async (sessionId) => {
  try {
    const response = await getChatMessageBySessionId(sessionId, chatId);
    if (response.data.code === 0 && response.data.data) {
      messages.value = response.data.data.chatMessageVOList || [];
    }
  } catch (error) {
    console.error('加载消息失败:', error);
  }
  scrollToBottom();
};

const selectSession = async (sessionId) => {
  currentSessionId.value = sessionId;
  await loadMessages(sessionId);
};

const createNewSession = async () => {
  try {
    const response = await createChatSession(chatId);
    if (response.data.code === 0 && response.data.data) {
      await loadSessions();
      currentSessionId.value = response.data.data;
      messages.value = [];
    }
  } catch (error) {
    console.error('创建会话失败:', error);
  }
};

const deleteSession = async (sessionId) => {
  if (!confirm('确定要删除这个会话吗？删除后将无法恢复！')) return;
  
  try {
    const response = await deleteChatSession(sessionId, chatId);
    if (response.data.code === 0) {
      await loadSessions();
      if (currentSessionId.value === sessionId) {
        if (sessions.value.length > 0) {
          selectSession(sessions.value[0].id);
        } else {
          currentSessionId.value = null;
          messages.value = [];
        }
      }
    }
  } catch (error) {
    console.error('删除会话失败:', error);
  }
};

const handleImageClick = () => {
  imageInput.value.click();
};

const handleImageSelect = (event) => {
  const file = event.target.files[0];
  if (file) {
    const reader = new FileReader();
    reader.onload = (e) => {
      selectedImage.value = {
        file: file,
        preview: e.target.result,
        name: file.name,
      };
    };
    reader.readAsDataURL(file);
  }
};

const removeSelectedImage = () => {
  selectedImage.value = null;
  if (imageInput.value) {
    imageInput.value.value = '';
  }
};

let mediaRecorder = null;
let audioChunks = [];
let recordedAudioUrl = null;

const handleAudioClick = async () => {
  if (isRecording.value) {
    stopRecording();
  } else {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      mediaRecorder = new MediaRecorder(stream);
      audioChunks = [];
      
      mediaRecorder.ondataavailable = (event) => {
        audioChunks.push(event.data);
      };
      
      mediaRecorder.onstop = async () => {
        const audioBlob = new Blob(audioChunks, { type: 'audio/mp3' });
        const audioFile = new File([audioBlob], 'audio.mp3', { type: 'audio/mp3' });
        recordedAudioUrl = URL.createObjectURL(audioBlob);
        
        await handleSendAudioMessage(audioFile, recordedAudioUrl);
        
        stream.getTracks().forEach(track => track.stop());
      };
      
      mediaRecorder.start();
      isRecording.value = true;
    } catch (error) {
      console.error('无法访问麦克风:', error);
      alert('无法访问麦克风，请检查权限设置');
    }
  }
};

const handleSendAudioMessage = async (audioFile, audioPreviewUrl) => {
  console.log('开始发送音频消息，预览URL:', audioPreviewUrl);
  if (!currentSessionId.value) {
    await createNewSession();
    if (!currentSessionId.value) return;
  }

  const userMessage = {
    id: Date.now().toString(),
    content: '',
    audioFileUrl: audioPreviewUrl,
    isAiResponse: false,
  };
  
  console.log('创建用户音频消息:', userMessage);
  messages.value.push(userMessage);
  inputMessage.value = '';
  scrollToBottom();
  
  isLoading.value = true;

  const aiMessageId = (Date.now() + 1).toString();
  const aiMessage = {
    id: aiMessageId,
    content: '',
    isAiResponse: true,
    recommendedProducts: [],
    pdfFileUrl: null,
    pdfFileName: null,
  };
  messages.value.push(aiMessage);

  try {
    await chatWithAudio(audioFile, '', chatId, currentSessionId.value, (chunk) => {
      const lastMessage = messages.value[messages.value.length - 1];
      if (lastMessage && lastMessage.id === aiMessageId) {
        lastMessage.content = chunk;
        scrollToBottom();
      }
    });
    
    await loadSessions();
    await loadUserFiles();
  } catch (error) {
    console.error('发送音频消息失败:', error);
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage && lastMessage.id === aiMessageId) {
      lastMessage.content = '抱歉，发生了一些错误，请稍后再试。';
    }
  } finally {
    isLoading.value = false;
    scrollToBottom();
  }
};

const stopRecording = () => {
  if (mediaRecorder && isRecording.value) {
    mediaRecorder.stop();
    isRecording.value = false;
  }
};

const handleSendMessage = async () => {
  if ((!inputMessage.value.trim() && !selectedImage.value) || isLoading.value) return;
  
  if (!currentSessionId.value) {
    await createNewSession();
    if (!currentSessionId.value) return;
  }

  if (selectedImage.value) {
    await handleSendImageMessage();
  } else {
    await handleSendTextMessage();
  }
};

const handleSendTextMessage = async () => {
  const userMessage = {
    id: Date.now().toString(),
    content: inputMessage.value,
    isAiResponse: false,
  };
  
  messages.value.push(userMessage);
  const messageToSend = inputMessage.value;
  inputMessage.value = '';
  scrollToBottom();
  
  isLoading.value = true;

  const aiMessageId = (Date.now() + 1).toString();
  const aiMessage = {
    id: aiMessageId,
    content: '',
    isAiResponse: true,
    recommendedProducts: [],
    pdfFileUrl: null,
    pdfFileName: null,
  };
  messages.value.push(aiMessage);

  try {
    await chatWithRagStream(messageToSend, chatId, currentSessionId.value, (chunk) => {
      const lastMessage = messages.value[messages.value.length - 1];
      if (lastMessage && lastMessage.id === aiMessageId) {
        lastMessage.content = chunk;
        scrollToBottom();
      }
    });
    
    await loadSessions();
    await loadUserFiles();
  } catch (error) {
    console.error('发送消息失败:', error);
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage && lastMessage.id === aiMessageId) {
      lastMessage.content = '抱歉，发生了一些错误，请稍后再试。';
    }
  } finally {
    isLoading.value = false;
    scrollToBottom();
  }
};

const handleSendImageMessage = async () => {
  const userMessage = {
    id: Date.now().toString(),
    content: inputMessage.value,
    imageFileUrl: selectedImage.value.preview,
    isAiResponse: false,
  };
  
  messages.value.push(userMessage);
  const messageToSend = inputMessage.value;
  inputMessage.value = '';
  const imageFile = selectedImage.value.file;
  removeSelectedImage();
  scrollToBottom();
  
  isLoading.value = true;

  const aiMessageId = (Date.now() + 1).toString();
  const aiMessage = {
    id: aiMessageId,
    content: '',
    isAiResponse: true,
    recommendedProducts: [],
    pdfFileUrl: null,
    pdfFileName: null,
  };
  messages.value.push(aiMessage);

  try {
    await chatWithImage(imageFile, messageToSend, chatId, currentSessionId.value, (chunk) => {
      const lastMessage = messages.value[messages.value.length - 1];
      if (lastMessage && lastMessage.id === aiMessageId) {
        lastMessage.content = chunk;
        scrollToBottom();
      }
    });
    
    await loadSessions();
    await loadUserFiles();
  } catch (error) {
    console.error('发送图片消息失败:', error);
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage && lastMessage.id === aiMessageId) {
      lastMessage.content = '抱歉，发生了一些错误，请稍后再试。';
    }
  } finally {
    isLoading.value = false;
    scrollToBottom();
  }
};

const handleLogout = async () => {
  try {
    await logout();
  } catch (error) {
    console.error('登出失败:', error);
  }
  localStorage.removeItem('user');
  router.push('/login');
};

onMounted(async () => {
  await loadSessions();
  await loadUserFiles();
  if (sessions.value.length > 0) {
    const response = await getLatestChatHistory(chatId);
    if (response.data.code === 0 && response.data.data) {
      currentSessionId.value = response.data.data.sessionId;
      messages.value = response.data.data.chatMessageVOList || [];
    } else {
      selectSession(sessions.value[0].id);
    }
  } else {
    await createNewSession();
  }
});
</script>

<style scoped>
/* Markdown内容样式 */
.markdown-content h1,
.markdown-content h2,
.markdown-content h3,
.markdown-content h4,
.markdown-content h5,
.markdown-content h6 {
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
}

.markdown-content h1 { font-size: 1.5em; }
.markdown-content h2 { font-size: 1.3em; }
.markdown-content h3 { font-size: 1.15em; }
.markdown-content h4 { font-size: 1em; }
.markdown-content h5 { font-size: 0.9em; }
.markdown-content h6 { font-size: 0.8em; }

.markdown-content p {
  margin: 0.5em 0;
  line-height: 1.6;
}

.markdown-content ul,
.markdown-content ol {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.markdown-content li {
  margin: 0.25em 0;
}

.markdown-content code {
  background: #f3f4f6;
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

.markdown-content pre {
  background: #1f2937;
  color: #e5e7eb;
  padding: 1em;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0.5em 0;
}

.markdown-content pre code {
  background: transparent;
  padding: 0;
  color: inherit;
}

.markdown-content blockquote {
  border-left: 4px solid #3b82f6;
  padding-left: 1em;
  margin: 0.5em 0;
  color: #64748b;
  font-style: italic;
}

.markdown-content a {
  color: #3b82f6;
  text-decoration: underline;
}

.markdown-content a:hover {
  color: #2563eb;
}

.markdown-content strong,
.markdown-content b {
  font-weight: 600;
}

.markdown-content em,
.markdown-content i {
  font-style: italic;
}

.markdown-content hr {
  border: none;
  border-top: 1px solid #e2e8f0;
  margin: 1em 0;
}

.markdown-content table {
  width: 100%;
  border-collapse: collapse;
  margin: 0.5em 0;
}

.markdown-content th,
.markdown-content td {
  border: 1px solid #e2e8f0;
  padding: 0.5em;
  text-align: left;
}

.markdown-content th {
  background: #f8fafc;
  font-weight: 600;
}

.chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f0f4f8 0%, #d9e2ec 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  /* 原高度设置可能影响浏览器最小化，改为最小高度 */
}

.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-logo {
  font-size: 32px;
}

.nav-title {
  font-size: 18px;
  font-weight: 600;
  color: #334155;
}

.nav-center {
  display: flex;
  gap: 16px;
}

.nav-item {
  padding: 8px 12px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.nav-item:hover {
  color: #3b82f6;
  background: #f1f5f9;
}

.nav-item.active {
  color: #3b82f6;
  background: #eff6ff;
  font-weight: 500;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
}

.user-name {
  font-size: 14px;
  color: #334155;
}

.logout-btn {
  padding: 6px 16px;
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: #fee2e2;
}

.chat-main {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 16px;
  gap: 16px;
}

.sidebar {
  width: 280px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  border-radius: 12px;
  overflow: hidden;
}

.sidebar-section:first-child {
  flex: 1;
  min-height: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #334155;
}

.new-session-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #eff6ff;
  color: #3b82f6;
  border-radius: 8px;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s;
}

.new-session-btn:hover {
  background: #dbeafe;
  transform: scale(1.05);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.session-item {
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  margin-bottom: 8px;
  position: relative;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.session-item:hover {
  background: #f8fafc;
}

.session-item.active {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  font-size: 12px;
  color: #94a3b8;
}

.delete-session-btn {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  border: none;
  background: #fef2f2;
  color: #dc2626;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.session-item:hover .delete-session-btn {
  opacity: 1;
}

.delete-session-btn:hover {
  background: #fee2e2;
}

.file-list {
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.empty-files {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #94a3b8;
  gap: 8px;
}

.empty-files-icon {
  font-size: 32px;
}

.empty-files-text {
  font-size: 13px;
}

.file-item {
  background: white;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 8px;
  transition: all 0.2s;
  border: 1px solid #e2e8f0;
}

.file-item:hover {
  border-color: #bfdbfe;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
}

.file-download-link {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: inherit;
  cursor: pointer;
}

.file-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.file-name {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-download-icon {
  font-size: 16px;
  flex-shrink: 0;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.file-item:hover .file-download-icon {
  opacity: 1;
}

.file-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}

.chat-area {
  flex: 1;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #64748b;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

/* .message {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  align-items: flex-start;
} */
/* 1. 基础消息容器改为相对定位，为头像绝对定位做准备 */
.message {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  align-items: flex-start;
  position: relative; /* 新增：相对定位 */
}
.message-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
  background: #f8fafc;
  border: 2px solid #e2e8f0;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}

.message-text {
  padding: 16px 20px;
  border-radius: 20px;
  font-size: 15px;
  line-height: 1.7;
  word-wrap: break-word;
  display: inline-block;
}

/* .user-message {
  flex-direction: row-reverse;
} */
/* 3. 用户消息布局调整（内容居右，头像在右下方） */
.user-message {
  flex-direction: row-reverse;
  justify-content: flex-start; /* 确保内容靠右对齐 */
  margin-bottom: 16px;
  padding-bottom: 20px;
}
/* 4. 用户头像绝对定位到右下方 */
.user-message .message-avatar {
  position: absolute;
  bottom: 0; /* 贴底 */
  right: 0; /* 靠右 */
  margin-left: 0;
  margin-right: 0;
  /* 可选：缩小头像，避免占用过多空间 */
  /* width: 36px;
  height: 36px;
  font-size: 18px; */
}

/* .user-message .message-content {
  align-items: flex-end;
} */
/* 5. 调整用户消息内容的右边距，避免被头像遮挡 */
.user-message .message-content {
  align-items: flex-end;
  margin-right: 58px; /* 留出头像宽度+间距（36px+12px） */
}
.user-message .message-text {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border-bottom-right-radius: 8px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
}
/* 7. 修复音频/图片消息与头像的间距问题 */
.user-message .audio-container,
.user-message .image-container {
  margin-right: 0;
}
/* 6. 保持AI头像原有样式（左侧上方） */
.ai-message .message-avatar {
  position: absolute; /* 取消绝对定位 */
  width: 44px;
  height: 44px;
  font-size: 22px;
  bottom: 0; /* 贴底 */
  left: 0; /* 靠右 */
  margin-left: 0;
  margin-right: 0;
}
.ai-message {
  flex-direction: row;
  margin-bottom: 12px;
  padding-bottom: 20px;
}
.ai-message .message-content {
  align-items: flex-end;
  margin-left: 58px; /* 留出头像宽度+间距（36px+12px） */
}
.ai-message .message-text {
  background: #ffffff;
  color: #334155;
  border-bottom-left-radius: 8px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.products-container {
  margin-top: 12px;
}

.products-title {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}

.products-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.product-card {
  width: 140px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  font-size: 12px;
}

.product-card img {
  width: 100%;
  height: 80px;
  object-fit: cover;
}

.product-name {
  padding: 8px;
  font-weight: 500;
  color: #334155;
}

.product-desc {
  padding: 0 8px 8px;
  color: #64748b;
  font-size: 11px;
}

.pdf-container {
  margin-top: 12px;
}

.pdf-title {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}

.pdf-download-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  text-decoration: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);
}

.pdf-download-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35);
}

.pdf-icon {
  font-size: 16px;
}

.pdf-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.send-btn {
  padding: 12px 28px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  align-self: flex-end;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.input-area {
  display: flex;
  gap: 12px;
  padding: 20px 24px 24px;
  border-top: 1px solid #e2e8f0;
  align-items: flex-end;
}

.input-area textarea {
  flex: 1;
  padding: 16px 20px;
  border: 1px solid #d1d5db;
  border-radius: 16px;
  font-size: 15px;
  resize: vertical;
  min-height: 80px;
  max-height: 200px;
  font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
  line-height: 1.6;
}

.input-area textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.input-tools {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.tool-btn {
  width: 44px;
  height: 44px;
  border: 1px solid #d1d5db;
  background: white;
  border-radius: 12px;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tool-btn:hover {
  background: #f1f5f9;
  border-color: #3b82f6;
  transform: translateY(-1px);
}

.tool-btn.recording {
  background: #fef2f2;
  border-color: #dc2626;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(220, 38, 38, 0.4);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(220, 38, 38, 0);
  }
}

.input-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.selected-image-preview {
  position: relative;
  display: inline-block;
}

.preview-image {
  max-width: 200px;
  max-height: 150px;
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  object-fit: contain;
}

.remove-image-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #dc2626;
  color: white;
  border: none;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  padding: 0;
}

.remove-image-btn:hover {
  background: #b91c1c;
}

.image-container {
  margin-bottom: 10px;
}

.message-image {
  max-width: 100%;
  max-height: 400px;
  border-radius: 16px;
  cursor: pointer;
  transition: transform 0.2s;
}

.message-image:hover {
  transform: scale(1.02);
}

.user-message .message-image {
  border-bottom-right-radius: 4px;
}

.ai-message .message-image {
  border-bottom-left-radius: 4px;
}

/* 音频容器样式 - 适配聊天布局 */
.audio-container {
  margin-bottom: 10px;
  display: block;
  width: 100%;
  max-width: 400px;
}
/* 通用音频播放器基础样式 */
.message-audio {
  width: 100%;
  min-width: 400px;
  height: 48px;
  border-radius: 24px;
  border: none;
  outline: none;
  transition: all 0.3s ease;
  display: block;
}
/* 用户消息的音频播放器（和用户文字消息同渐变配色） */
.user-message .message-audio {
  background: #f2f2f2;
  border-bottom-right-radius: 8px; /* 和用户消息气泡圆角统一 */
}
/* 悬浮动效 - 放大+阴影加深 */
.message-audio:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}
/* 隐藏浏览器默认控件的多余样式（部分浏览器适配） */
.message-audio::-webkit-media-controls-panel {
  background: transparent !important;
  color: white !important;
  border-radius: 24px;
}
.user-message .message-audio::-webkit-media-controls-panel {
  color: white !important;
}
.ai-message .message-audio::-webkit-media-controls-panel {
  color: #334155 !important;
}
/* 适配Firefox浏览器 */
.message-audio::-moz-media-controls-panel {
  background: transparent !important;
  border-radius: 24px;
}
</style>
