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
              <div class="message-text markdown-content" v-html="formatMessage(message.content)"></div>
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
            </div>
          </div>
        </div>

        <div class="quick-tags">
          <span class="tag">新会话</span>
          <span class="tag">焦虑</span>
          <span class="tag">睡眠</span>
          <span class="tag">情绪低落</span>
          <span class="tag">人际</span>
        </div>

        <div class="input-area">
          <textarea 
            v-model="inputMessage"
            placeholder="请输入您的问题..."
            @keydown.enter.prevent="handleSendMessage"
            rows="1"
          ></textarea>
          <button @click="handleSendMessage" :disabled="isLoading || !inputMessage.trim()" class="send-btn">
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
  logout 
} from '../api';

const router = useRouter();
const user = ref(JSON.parse(localStorage.getItem('user')) || {});
const chatId = String(user.value.id);

const sessions = ref([]);
const currentSessionId = ref(null);
const messages = ref([]);
const inputMessage = ref('');
const isLoading = ref(false);
const messagesContainer = ref(null);

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

/**
 * 格式化消息内容，支持Markdown渲染
 * @param {string} content - 原始消息内容
 * @returns {string} 渲染后的HTML内容
 */
const formatMessage = (content) => {
  if (!content) return '';
  try {
    const htmlContent = marked.parse(content);
    return DOMPurify.sanitize(htmlContent);
  } catch (error) {
    console.error('Markdown渲染错误:', error);
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

const handleSendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value) return;
  
  if (!currentSessionId.value) {
    await createNewSession();
    if (!currentSessionId.value) return;
  }

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

.message {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  align-items: flex-start;
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

.user-message {
  flex-direction: row-reverse;
}

.user-message .message-content {
  align-items: flex-end;
}

.user-message .message-text {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border-bottom-right-radius: 8px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
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

.quick-tags {
  display: flex;
  gap: 10px;
  padding: 12px 24px;
  border-top: 1px solid #e2e8f0;
}

.tag {
  padding: 6px 14px;
  background: #f8fafc;
  color: #64748b;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.tag:hover {
  background: #eff6ff;
  color: #3b82f6;
  border-color: #bfdbfe;
}

.input-area {
  display: flex;
  gap: 12px;
  padding: 16px 24px 24px;
  border-top: 1px solid #e2e8f0;
}

.input-area textarea {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 12px;
  font-size: 15px;
  resize: none;
  max-height: 120px;
  font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-area textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
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
</style>
