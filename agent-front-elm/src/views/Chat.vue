<template>
  <div class="chat-container">
    <nav class="navbar">
      <div class="nav-left" @click="goToHome" style="cursor: pointer;">
        <span class="nav-logo">❤️</span>
        <span class="nav-title">青少年情感陪伴智能体</span>
      </div>
      <div class="nav-center">
        <el-button text type="primary">情感陪伴</el-button>
        <el-button text @click="goToEmotionDiary">情绪日记</el-button>
        <el-button text @click="goToEmotionClassroom">情感课堂</el-button>
        <el-button text @click="goToKnowledgeManagement" v-if="user?.isAdmin === 1">知识库管理</el-button>
        <el-button text>反馈与建议</el-button>
      </div>
      <div class="nav-right">
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            <el-avatar :size="36" :src="user.avatar" v-if="user.avatar"></el-avatar>
            <el-avatar :size="36" v-else>{{ user.username?.charAt(0) || 'U' }}</el-avatar>
            <span class="user-name">{{ user.username }}</span>
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="userManage" v-if="user?.isAdmin === 1">用户管理</el-dropdown-item>
              <el-dropdown-item command="knowledgeManage" v-if="user?.isAdmin === 1">知识库管理</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </nav>

    <div class="chat-main">
      <aside class="sidebar">
        <div class="sidebar-section">
          <div class="sidebar-header">
            <h3>历史会话</h3>
            <el-button type="primary" circle @click="createNewSession" title="新建会话">
              <el-icon><plus /></el-icon>
            </el-button>
          </div>
          <div class="session-list">
            <el-empty v-if="sessions.length === 0" description="暂无会话" :image-size="60" />
            <div
              v-for="session in sessions"
              :key="session.id"
              :class="['session-item', { active: currentSessionId === session.id }]"
              @click="selectSession(session.id)"
            >
              <div class="session-title">{{ session.sessionName || '新会话' }}</div>
              <div class="session-time">{{ formatDate(session.updatedAt) }}</div>
              <el-button
                type="danger"
                text
                circle
                size="large"
                @click.stop="deleteSession(session.id)"
                title="删除会话"
                class="delete-session-btn"
              >
                <el-icon><delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>

        <div class="sidebar-section">
          <div class="sidebar-header2">
            <h3>我的文档</h3>
          </div>
          <div class="file-list">
            <el-empty v-if="userFiles.length === 0" description="暂无文档" :image-size="40" />
            <div v-for="file in userFiles" :key="file.id" class="file-item">
              <el-link :href="file.fileUrl" target="_blank" type="primary" class="file-download-link">
                <el-icon><document /></el-icon>
                <span class="file-name">{{ file.fileName }}</span>
                <!-- <el-icon class="file-download-icon"><download /></el-icon> -->
              </el-link>
               <div class="file-time">{{ formatDate(file.createTime) }}</div>
            </div>
          </div>
        </div>
      </aside>

      <main class="chat-area">
        <div class="messages-container" ref="messagesContainer">
          <el-empty v-if="messages.length === 0" description="你好！我是你的情感陪伴助手，有什么想和我聊聊的吗？">
            <template #image>
              <div class="empty-icon">🤖</div>
            </template>
          </el-empty>
          <div
            v-for="message in messages"
            :key="message.id"
            :class="['message', message.isAiResponse ? 'ai-message' : 'user-message']"
          >
            <div class="message-avatar">
              <template v-if="message.isAiResponse">
                🤖
              </template>
              <template v-else>
                <el-avatar :size="44" :src="user.avatar" v-if="user.avatar"></el-avatar>
                <span v-else>👤</span>
              </template>
            </div>
            <div class="message-content">
              <div v-if="message.imageFileUrl" class="image-container">
                <el-image 
                  :src="message.imageFileUrl" 
                  :alt="message.imageFileName || '图片'" 
                  fit="contain" 
                  class="message-image"
                  @click="previewImage(message.imageFileUrl)"
                />
              </div>
              <div v-if="message.audioFileUrl" class="audio-container">
                <audio :src="message.audioFileUrl" controls class="message-audio"></audio>
              </div>
              <div v-if="message.content" class="message-text markdown-content" v-html="formatMessage(message.content)"></div>
              <div v-if="message.pdfFileUrl" class="pdf-container">
                <div class="pdf-title">📄 生成的文档：</div>
                <el-link :href="message.pdfFileUrl" target="_blank" type="success" class="pdf-download-btn">
                  <!-- <el-icon><download /></el-icon> -->
                  <span>{{ message.pdfFileName || '下载PDF' }}</span>
                </el-link>
              </div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <div class="input-main">
            <input
              type="file"
              ref="imageInput"
              accept="image/*"
              style="display: none"
              @change="handleImageSelect"
            />
            <div v-if="selectedImage" class="selected-image-preview">
              <el-image :src="selectedImage.preview" :alt="selectedImage.name" fit="contain" class="preview-image" />
              <el-button type="danger" circle size="medium" @click="removeSelectedImage" class="remove-image-btn">
                <el-icon><close /></el-icon>
              </el-button>
            </div>
            <div class="textarea-wrapper">
              <el-input
                v-model="inputMessage"
                type="textarea"
                :rows="3"
                placeholder="请输入您的问题..."
                @keydown.enter.prevent="handleSendMessage"
                resize="none"
              />
              <div class="input-tools">
                <el-button circle size="medium" @click="handleImageClick" title="上传图片">
                  <img src="../image/picture-icon.svg" alt="上传图片" style="width: 18px; height: 18px;" />
                </el-button>
                <el-button
                  circle
                  size="medium"
                  :type="isRecording ? 'danger' : 'default'"
                  @click="handleAudioClick"
                  :title="isRecording ? '停止录音' : '语音输入'"
                >
                  <img src="../image/microphone-icon.svg" alt="语音输入" style="width: 18px; height: 18px;" />
                </el-button>
              </div>
            </div>
          </div>
          <el-button
            type="primary"
            :loading="isLoading"
            :disabled="!inputMessage.trim() && !selectedImage"
            @click="handleSendMessage"
            class="send-btn"
          >
            发送
          </el-button>
        </div>
      </main>
    </div>

    <!-- 图片预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="图片预览"
      width="80%"
      top="5vh"
      append-to-body
    >
      <div class="dialog-image-container">
        <el-image
          :src="previewImageUrl"
          fit="contain"
          class="dialog-image"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  ArrowDown, Plus, Delete, Document, Download, Close
} from '@element-plus/icons-vue';
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
const previewDialogVisible = ref(false);
const previewImageUrl = ref('');

let mediaRecorder = null;
let audioChunks = [];

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

const formatMessage = (content) => {
  if (!content) return '';
  try {
    marked.setOptions({
      breaks: true,
      gfm: true,
      smartypants: true
    });
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
      messagesContainer.value.wrap.scrollTop = messagesContainer.value.wrap.scrollHeight;
    }
  });
};

const loadSessions = async () => {
  try {
    const response = await getChatSessionList(chatId);
    if (response.code === 0 && response.data) {
      sessions.value = response.data;
    }
  } catch (error) {
    console.error('加载会话列表失败:', error);
  }
};

const loadUserFiles = async () => {
  try {
    const response = await getUserFileList(chatId);
    if (response.code === 0 && response.data) {
      userFiles.value = response.data;
    }
  } catch (error) {
    console.error('加载用户文件失败:', error);
  }
};

const loadMessages = async (sessionId) => {
  try {
    const response = await getChatMessageBySessionId(sessionId, chatId);
    if (response.code === 0 && response.data) {
      messages.value = response.data.chatMessageVOList || [];
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
    if (response.code === 0 && response.data) {
      await loadSessions();
      currentSessionId.value = response.data;
      messages.value = [];
    }
  } catch (error) {
    console.error('创建会话失败:', error);
  }
};

const deleteSession = async (sessionId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个会话吗？删除后将无法恢复！', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }

  try {
    const response = await deleteChatSession(sessionId, chatId);
    if (response.code === 0) {
      ElMessage.success('删除成功');
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
    ElMessage.error('删除失败');
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
        const audioPreviewUrl = URL.createObjectURL(audioBlob);

        await handleSendAudioMessage(audioFile, audioPreviewUrl);

        stream.getTracks().forEach(track => track.stop());
      };

      mediaRecorder.start();
      isRecording.value = true;
    } catch (error) {
      console.error('无法访问麦克风:', error);
      ElMessage.error('无法访问麦克风，请检查权限设置');
    }
  }
};

const handleSendAudioMessage = async (audioFile, audioPreviewUrl) => {
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

  messages.value.push(userMessage);
  inputMessage.value = '';
  scrollToBottom();

  isLoading.value = true;

  const aiMessageId = (Date.now() + 1).toString();
  const aiMessage = {
    id: aiMessageId,
    content: '',
    isAiResponse: true,
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
  ElMessage.success('退出登录成功');
  router.push('/');
};

const goToHome = () => {
  router.push('/');
};

const goToEmotionDiary = () => {
  router.push('/emotion-diary');
};

const goToEmotionClassroom = () => {
  router.push('/emotion-classroom');
};

const goToKnowledgeManagement = () => {
  router.push('/knowledge-management');
};

const goToProfile = () => {
  router.push('/profile');
};

const handleCommand = (command) => {
  if (command === 'profile') {
    goToProfile();
  } else if (command === 'userManage') {
    router.push('/user-management');
  } else if (command === 'knowledgeManage') {
    router.push('/knowledge-management');
  } else if (command === 'logout') {
    handleLogout();
  }
};

const previewImage = (imageUrl) => {
  previewImageUrl.value = imageUrl;
  previewDialogVisible.value = true;
};

onMounted(async () => {
  await loadSessions();
  await loadUserFiles();
  if (sessions.value.length > 0) {
    const response = await getLatestChatHistory(chatId);
    if (response.code === 0 && response.data) {
      currentSessionId.value = response.data.sessionId;
      messages.value = response.data.chatMessageVOList || [];
    } else {
      selectSession(sessions.value[0].id);
    }
  } else {
    await createNewSession();
  }
});
</script>

<style scoped>
.chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f0f4f8 0%, #d9e2ec 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
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

.nav-center .el-button {
  font-size: 16px;
  font-weight: 500;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 15px;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #333;
}

.user-name {
  font-weight: 500;
  font-size: 16px;
}

.chat-main {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 16px;
  gap: 16px;
}

.chat-content {
  height: 100%;
  display: flex;
  overflow: hidden;
}

.sidebar {
  width: 300px;
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

.sidebar-section:last-child {
  max-height: 300px;
  min-height: 150px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid #e2e8f0;
}
.sidebar-header2 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #e2e8f0;
}
.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #334155;
}
.sidebar-header2 h3 {
  padding-top: 4px;
  margin: 0;
  font-size: 16px;
  color: #334155;
}
.session-list {
  flex: 1;
  padding: 8px;
  overflow-y: auto;
}

.file-list {
  padding: 8px;
  overflow-y: auto;
}

.session-item {
  position: relative;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  margin-bottom: 8px;
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
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .delete-session-btn {
  opacity: 1;
}

.file-item {
  background: white;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 8px;
  transition: all 0.2s;
  border: 1px solid #e2e8f0;
  text-align: left;
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
  justify-content: flex-start;
}

.file-name {
  /* flex: 1; */
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

/* .file-download-icon {
  font-size: 16px;
  flex-shrink: 0;
  opacity: 0.7;
  transition: opacity 0.2s;
} */

/* .file-item:hover .file-download-icon {
  opacity: 1;
} */

.file-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
  text-align: left;
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
  padding: 24px;
  overflow-y: auto;
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
  position: relative;
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
  overflow: hidden;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}

.message-text {
  padding: 10px 18px;
  border-radius: 20px;
  font-size: 15px;
  line-height: 1.6;
  word-wrap: break-word;
  display: inline-block;
}

.user-message {
  flex-direction: row-reverse;
  justify-content: flex-start;
  margin-bottom: 16px;
  padding-bottom: 20px;
}

.user-message .message-avatar {
  position: absolute;
  bottom: 0;
  right: 0;
}

.user-message .message-content {
  align-items: flex-end;
  margin-right: 58px;
}

.user-message .message-text {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
  border-bottom-right-radius: 8px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.25);
}

.ai-message {
  flex-direction: row;
  margin-bottom: 12px;
  padding-bottom: 20px;
}

.ai-message .message-avatar {
  position: absolute;
  bottom: 0;
  left: 0;
}

.ai-message .message-content {
  align-items: flex-start;
  margin-left: 58px;
}

.ai-message .message-text {
  background: #ffffff;
  color: #334155;
  border-bottom-left-radius: 8px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.image-container {
  margin-bottom: 10px;
  max-width: 100%;
  overflow: hidden;
}

.message-image {
  width: 100%;
  height: 100%;
  max-width: 300px;
  max-height: 400px;
  border-radius: 16px;
  cursor: pointer;
  transition: transform 0.2s;
  object-fit: contain;
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

.audio-container {
  margin-bottom: 10px;
  display: block;
  width: 100%;
  max-width: 400px;
}

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

.user-message .message-audio {
  background: #f2f2f2;
  border-bottom-right-radius: 8px;
}

.pdf-container {
  margin-top: 12px;
  padding: 16px 20px;
  background: #ffffff;
  border-radius: 20px;
  border-bottom-left-radius: 8px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
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
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
}

.input-area {
  display: flex;
  gap: 12px;
  padding: 20px 24px 24px;
  border-top: 1px solid #e2e8f0;
  align-items: flex-end;
}

.input-tools {
  position: absolute;
  right: 7px;
  bottom: 8px;
  display: flex;
  gap: 0px;
  z-index: 10;
}

.input-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.textarea-wrapper {
  position: relative;
  width: 100%;
}

.textarea-wrapper :deep(.el-textarea__inner) {
  padding-bottom: 40px;
  border-radius: 16px;
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
}

.send-btn {
  align-self: flex-end;
  padding: 12px 28px;
  font-size: 15px;
  font-weight: 600;
}

:deep(.markdown-content) h1,
:deep(.markdown-content) h2,
:deep(.markdown-content) h3,
:deep(.markdown-content) h4,
:deep(.markdown-content) h5,
:deep(.markdown-content) h6 {
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
}

:deep(.markdown-content) h1 { font-size: 1.5em; }
:deep(.markdown-content) h2 { font-size: 1.3em; }
:deep(.markdown-content) h3 { font-size: 1.15em; }
:deep(.markdown-content) h4 { font-size: 1em; }
:deep(.markdown-content) h5 { font-size: 0.9em; }
:deep(.markdown-content) h6 { font-size: 0.8em; }

:deep(.markdown-content) p {
  margin: 0.5em 0;
  line-height: 1.6;
}

:deep(.markdown-content) ul,
:deep(.markdown-content) ol {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

:deep(.markdown-content) li {
  margin: 0.25em 0;
}

:deep(.markdown-content) code {
  background: #f3f4f6;
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

:deep(.markdown-content) pre {
  background: #1f2937;
  color: #e5e7eb;
  padding: 1em;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0.5em 0;
}

:deep(.markdown-content) pre code {
  background: transparent;
  padding: 0;
  color: inherit;
}

:deep(.markdown-content) blockquote {
  border-left: 4px solid #409eff;
  padding-left: 1em;
  margin: 0.5em 0;
  color: #64748b;
  font-style: italic;
}

:deep(.markdown-content) a {
  color: #409eff;
  text-decoration: underline;
}

:deep(.markdown-content) a:hover {
  color: #66b1ff;
}

:deep(.markdown-content) strong,
:deep(.markdown-content) b {
  font-weight: 600;
}

:deep(.markdown-content) em,
:deep(.markdown-content) i {
  font-style: italic;
}

:deep(.markdown-content) hr {
  border: none;
  border-top: 1px solid #e2e8f0;
  margin: 1em 0;
}

:deep(.markdown-content) table {
  width: 100%;
  border-collapse: collapse;
  margin: 0.5em 0;
}

:deep(.markdown-content) th,
:deep(.markdown-content) td {
  border: 1px solid #e2e8f0;
  padding: 0.5em;
  text-align: left;
}

:deep(.markdown-content) th {
  background: #f8fafc;
  font-weight: 600;
}

.dialog-image-container {
  width: 100%;
  height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  border-radius: 8px;
}

.dialog-image {
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
</style>
