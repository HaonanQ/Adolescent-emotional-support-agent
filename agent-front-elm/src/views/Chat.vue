<template>
  <div class="chat-container">
    <!-- 装饰性背景 -->
    <div class="decorative-circle circle-1" style="top: -150px; right: -150px; opacity: 0.3;"></div>
    <div class="decorative-circle circle-2" style="bottom: -100px; left: -100px; opacity: 0.3;"></div>
    
    <nav class="navbar" style="animation: fadeInDown 0.4s ease-out;">
      <div class="nav-left" @click="goToHome" style="cursor: pointer;">
        <span class="nav-logo">❤️</span>
        <span class="nav-title">青少年情感陪伴智能体</span>
      </div>
      <div class="nav-center">
        <el-button class="nav-btn" @click="goToChat">情感陪伴</el-button>
        <el-button class="nav-btn" @click="goToEmotionDiary">情绪日记</el-button>
        <el-button class="nav-btn" @click="goToEmotionClassroom">情感课堂</el-button>
        <el-button class="nav-btn" @click="goToKnowledgeManagement" v-if="user?.isAdmin === 1">知识库管理</el-button>
        <el-button class="nav-btn" @click="goToFeedback">反馈与建议</el-button>
      </div>
      <div class="nav-right">
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            <el-avatar :size="36" :src="user.avatar" v-if="user.avatar" class="user-avatar"></el-avatar>
            <el-avatar :size="36" v-else class="user-avatar">{{ user.username?.charAt(0) || 'U' }}</el-avatar>
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
      <!-- 侧边栏 -->
      <aside class="sidebar" style="animation: fadeInUp 0.4s ease-out 0.1s both;">
        <div class="sidebar-section">
          <div class="sidebar-header">
            <h3>历史会话</h3>
            <el-button type="primary" circle @click="createNewSession" title="新建会话" class="new-session-btn">
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
              <div class="session-icon">💬</div>
              <div class="session-info">
                <div class="session-title">{{ session.sessionName || '新会话' }}</div>
                <div class="session-time">{{ formatDate(session.updatedAt) }}</div>
              </div>
              <el-button
                type="danger"
                text
                circle
                size="small"
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
          <div class="sidebar-header">
            <h3>我的文档</h3>
          </div>
          <div class="file-list">
            <el-empty v-if="userFiles.length === 0" description="暂无文档" :image-size="40" />
            <div v-for="file in userFiles" :key="file.id" class="file-item">
              <el-link :href="file.fileUrl" target="_blank" type="primary" class="file-download-link">
                <el-icon class="file-icon"><document /></el-icon>
                <span class="file-name">{{ file.fileName }}</span>
              </el-link>
              <div class="file-time">{{ formatDate(file.createTime) }}</div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 聊天区域 -->
      <main class="chat-area" style="animation: fadeInUp 0.4s ease-out 0.2s both;">
        <div class="messages-container" ref="messagesContainer">
          <div v-if="messages.length === 0" class="welcome-section">
            <div class="welcome-icon">🤖</div>
            <h2 class="welcome-title">你好！我是你的情感陪伴助手</h2>
            <p class="welcome-text">有什么想和我聊聊的吗？无论是开心、烦恼还是困惑，我都会耐心倾听。</p>
          </div>
          <div
            v-for="message in messages"
            :key="message.id"
            :class="['message', message.isAiResponse ? 'ai-message' : 'user-message']"
          >
            <div class="message-avatar">
              <template v-if="message.isAiResponse">
                <div class="ai-avatar">
                  <span>🤖</span>
                </div>
              </template>
              <template v-else>
                <el-avatar :size="44" :src="user.avatar" v-if="user.avatar" class="user-message-avatar"></el-avatar>
                <div v-else class="user-avatar-fallback">
                  <span>👤</span>
                </div>
              </template>
            </div>
            <div class="message-content">
              <div v-if="message.imageFileUrl" class="image-container" @click="previewImage(message.imageFileUrl)">
                <el-image 
                  :src="message.imageFileUrl" 
                  :alt="message.imageFileName || '图片'" 
                  fit="contain" 
                  class="message-image"
                  :preview-src-list="[message.imageFileUrl]"
                />
              </div>
              <div v-if="message.audioFileUrl" class="audio-container">
                <audio :src="message.audioFileUrl" controls class="message-audio"></audio>
              </div>
              <div v-if="message.content" class="message-text markdown-content" v-html="formatMessage(message.content)"></div>
              <div v-if="message.pdfFileUrl" class="pdf-container">
                <div class="pdf-header">
                  <span class="pdf-icon">📄</span>
                  <span class="pdf-label">生成的文档</span>
                </div>
                <el-link :href="message.pdfFileUrl" target="_blank" type="primary" class="pdf-download-btn">
                  <span>{{ message.pdfFileName || '下载PDF' }}</span>
                </el-link>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <div class="input-main">
            <input
              type="file"
              ref="imageInput"
              accept="image/*"
              style="display: none"
              @change="handleImageSelect"
            />
            
            <!-- 预览区域 -->
            <div v-if="selectedImage" class="selected-preview">
              <div class="selected-image-container">
                <el-image :src="selectedImage.preview" :alt="selectedImage.name" fit="contain" class="preview-image" />
                <el-button type="danger" circle size="small" @click="removeSelectedImage" class="remove-btn">
                  <el-icon><close /></el-icon>
                </el-button>
              </div>
            </div>
            <div v-if="selectedDiaries.length > 0" class="selected-diaries-preview">
              <div class="diaries-preview-header">
                <span class="diaries-preview-title">📔 已选择 {{ selectedDiaries.length }} 篇日记</span>
                <el-button type="text" size="small" @click="clearSelectedDiaries" class="clear-btn">
                  <el-icon><close /></el-icon>
                </el-button>
              </div>
              <div class="diaries-preview-list">
                <el-tag
                  v-for="diary in selectedDiaries"
                  :key="diary.id"
                  closable
                  @close="removeDiaryFromSelection(diary.id)"
                  class="diary-tag"
                >
                  {{ diary.title || '无标题' }}
                </el-tag>
              </div>
            </div>
            
            <!-- 输入框 -->
            <div class="textarea-wrapper">
              <el-input
                v-model="inputMessage"
                type="textarea"
                :rows="3"
                placeholder="请输入您想聊的内容..."
                @keydown.enter.prevent="handleEnter"
                resize="none"
                class="chat-textarea"
              />
              <div class="input-tools">
                <el-button class="tool-btn" circle size="medium" @click="handleDiaryClick" title="选择日记">
                  📔
                </el-button>
                <el-button class="tool-btn" circle size="medium" @click="handleImageClick" title="上传图片">
                  <span class="tool-icon">🖼️</span>
                </el-button>
                <el-button
                  class="tool-btn"
                  circle
                  size="medium"
                  :type="isRecording ? 'danger' : 'default'"
                  @click="handleAudioClick"
                  :title="isRecording ? '停止录音' : '语音输入'"
                >
                  <span class="tool-icon" v-if="!isRecording">🎤</span>
                  <span class="recording-indicator" v-else>
                    <span class="recording-dot"></span>
                    <span class="recording-dot"></span>
                    <span class="recording-dot"></span>
                  </span>
                </el-button>
                <div class="tool-divider"></div>
                <el-button
                  class="send-btn"
                  circle
                  size="medium"
                  type="primary"
                  :loading="isLoading"
                  :disabled="!inputMessage.trim() && !selectedImage && selectedDiaries.length === 0"
                  @click="handleSendMessage"
                  title="发送消息"
                >
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 4L12 20M12 4L6 10M12 4L18 10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- 日记选择对话框 -->
    <el-dialog
      v-model="diaryDialogVisible"
      title="选择日记"
      width="600px"
      top="15vh"
      append-to-body
      class="diary-dialog"
    >
      <div class="diary-select-container">
        <el-empty v-if="availableDiaries.length === 0" description="暂无日记" :image-size="60" />
        <div v-else class="diary-select-list">
          <div
            v-for="diary in availableDiaries"
            :key="diary.id"
            :class="['diary-select-item', { selected: selectedDiaryIds.includes(diary.id) }]"
            @click="toggleDiarySelection(diary.id)"
          >
            <div class="diary-select-checkbox">
              <el-checkbox :model-value="selectedDiaryIds.includes(diary.id)" @click.stop />
            </div>
            <div class="diary-select-content">
              <div class="diary-select-title">{{ diary.title || '无标题' }}</div>
              <div class="diary-select-info">
                <span class="diary-select-mood">{{ diary.mood }}</span>
                <span class="diary-select-time">{{ formatDate(diary.createTime) }}</span>
              </div>
            </div>
            <div class="diary-select-check">
              <el-icon v-if="selectedDiaryIds.includes(diary.id)"><Check /></el-icon>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="diaryDialogVisible = false" class="dialog-cancel-btn">取消</el-button>
          <el-button type="primary" @click="confirmDiarySelection" class="dialog-confirm-btn">
            确定 (已选择 {{ selectedDiaryIds.length }} 篇)
          </el-button>
        </div>
      </template>
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
  ArrowDown, Plus, Delete, Document, Close, Check
} from '@element-plus/icons-vue';
import {
  getChatSessionList,
  getChatMessageBySessionId,
  getLatestChatHistory,
  createChatSession,
  chatWithRagStream,
  deleteChatSession,
  logout,
  getUserFiles,
  chatWithImage,
  chatWithAudio,
  getEmotionDiaryList
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

const diaryDialogVisible = ref(false);
const availableDiaries = ref([]);
const selectedDiaryIds = ref([]);
const selectedDiaries = ref([]);

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
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
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
    const response = await getUserFiles(chatId);
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
  clearSelectedDiaries();
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

const handleDiaryClick = async () => {
  clearSelectedImage();
  try {
    const response = await getEmotionDiaryList(user.value.id);
    if (response.code === 0 && response.data) {
      availableDiaries.value = response.data || [];
      selectedDiaryIds.value = [...selectedDiaries.value.map(d => d.id)];
      diaryDialogVisible.value = true;
    }
  } catch (error) {
    console.error('加载日记列表失败:', error);
    ElMessage.error('加载日记列表失败');
  }
};

const toggleDiarySelection = (diaryId) => {
  const index = selectedDiaryIds.value.indexOf(diaryId);
  if (index > -1) {
    selectedDiaryIds.value.splice(index, 1);
  } else {
    selectedDiaryIds.value.push(diaryId);
  }
};

const confirmDiarySelection = () => {
  selectedDiaries.value = availableDiaries.value.filter(diary => 
    selectedDiaryIds.value.includes(diary.id)
  );
  diaryDialogVisible.value = false;
};

const clearSelectedDiaries = () => {
  selectedDiaries.value = [];
  selectedDiaryIds.value = [];
};

const clearSelectedImage = () => {
  selectedImage.value = null;
  if (imageInput.value) {
    imageInput.value.value = '';
  }
};

const removeDiaryFromSelection = (diaryId) => {
  const index = selectedDiaries.value.findIndex(d => d.id === diaryId);
  if (index > -1) {
    selectedDiaries.value.splice(index, 1);
  }
  const idIndex = selectedDiaryIds.value.indexOf(diaryId);
  if (idIndex > -1) {
    selectedDiaryIds.value.splice(idIndex, 1);
  }
};

const handleAudioClick = async () => {
  clearSelectedDiaries();
  clearSelectedImage();
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

        await handleSendAudioMessage(audioFile);

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

const handleSendAudioMessage = async (audioFile) => {
  if (!currentSessionId.value) {
    await createNewSession();
    if (!currentSessionId.value) return;
  }

  const audioPreviewUrl = URL.createObjectURL(audioFile);
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
    await chatWithAudio(audioFile, '', chatId, currentSessionId.value, user.value.nickname, user.value.sex, (chunk) => {
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

const handleEnter = (event) => {
  if (!event.shiftKey) {
    handleSendMessage();
  }
};

const handleSendMessage = async () => {
  if ((!inputMessage.value.trim() && !selectedImage.value && selectedDiaries.value.length === 0) || isLoading.value) return;

  if (!currentSessionId.value) {
    await createNewSession();
    if (!currentSessionId.value) return;
  }

  if (selectedImage.value) {
    await handleSendImageMessage();
  } else if (selectedDiaries.value.length > 0) {
    await handleSendDiaryMessage();
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
    await chatWithRagStream(messageToSend, chatId, currentSessionId.value, user.value.nickname, user.value.sex, (chunk) => {
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
      let errorMessage = '抱歉，发生了一些错误，请稍后再试。';
      
      if (error.response) {
        const status = error.response.status;
        const data = error.response.data;
        
        if (status === 429) {
          errorMessage = '抱歉，当前请求过于频繁，请稍后再试。';
        } else if (status === 500) {
          errorMessage = '抱歉，服务器遇到了一些问题，请稍后再试。';
        } else if (status === 503) {
          errorMessage = '抱歉，AI服务暂时不可用，请稍后再试。';
        } else if (data && data.message) {
          if (data.message.includes('AI') || data.message.includes('模型') || data.message.includes('API')) {
            errorMessage = '抱歉，AI服务暂时无法响应，请稍后再试。';
          }
        }
      } else if (error.message) {
        if (error.message.includes('timeout') || error.message.includes('超时')) {
          errorMessage = '抱歉，请求超时了，请检查网络后重试。';
        } else if (error.message.includes('network') || error.message.includes('网络')) {
          errorMessage = '抱歉，网络连接出现问题，请检查网络后重试。';
        }
      }
      
      lastMessage.content = errorMessage;
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
    await chatWithImage(imageFile, messageToSend, chatId, currentSessionId.value, user.value.nickname, user.value.sex, (chunk) => {
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
      let errorMessage = '抱歉，发生了一些错误，请稍后再试。';
      
      if (error.response) {
        const status = error.response.status;
        const data = error.response.data;
        
        if (status === 429) {
          errorMessage = '抱歉，当前请求过于频繁，请稍后再试。';
        } else if (status === 500) {
          errorMessage = '抱歉，服务器遇到了一些问题，请稍后再试。';
        } else if (status === 503) {
          errorMessage = '抱歉，AI服务暂时不可用，请稍后再试。';
        } else if (data && data.message) {
          if (data.message.includes('AI') || data.message.includes('模型') || data.message.includes('API')) {
            errorMessage = '抱歉，AI服务暂时无法响应，请稍后再试。';
          }
        }
      } else if (error.message) {
        if (error.message.includes('timeout') || error.message.includes('超时')) {
          errorMessage = '抱歉，请求超时了，请检查网络后重试。';
        } else if (error.message.includes('network') || error.message.includes('网络')) {
          errorMessage = '抱歉，网络连接出现问题，请检查网络后重试。';
        }
      }
      
      lastMessage.content = errorMessage;
    }
  } finally {
    isLoading.value = false;
    scrollToBottom();
  }
};

const handleSendDiaryMessage = async () => {
  const diaryContent = selectedDiaries.value.map(diary => {
    return `【${diary.title || '无标题'}】\n情绪: ${diary.mood}\n情绪分数: ${diary.moodScore}/10\n内容: ${diary.content || '无内容'}`;
  }).join('\n\n---\n\n');

  const fullMessage = inputMessage.value 
    ? `${inputMessage.value}\n\n---以下是日记内容---\n\n${diaryContent}`
    : `以下是日记内容:\n\n${diaryContent}`;

  const userMessage = {
    id: Date.now().toString(),
    content: `已选择 ${selectedDiaries.value.length} 篇日记:\n${selectedDiaries.value.map(d => '• ' + (d.title || '无标题')).join('\n')}${inputMessage.value ? '\n\n' + inputMessage.value : ''}`,
    isAiResponse: false,
  };

  messages.value.push(userMessage);
  inputMessage.value = '';
  const diariesToSend = [...selectedDiaries.value];
  clearSelectedDiaries();
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
    await chatWithRagStream(fullMessage, chatId, currentSessionId.value, user.value.nickname, user.value.sex, (chunk) => {
      const lastMessage = messages.value[messages.value.length - 1];
      if (lastMessage && lastMessage.id === aiMessageId) {
        lastMessage.content = chunk;
        scrollToBottom();
      }
    });

    await loadSessions();
    await loadUserFiles();
  } catch (error) {
    console.error('发送日记消息失败:', error);
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage && lastMessage.id === aiMessageId) {
      let errorMessage = '抱歉，发生了一些错误，请稍后再试。';
      
      if (error.response) {
        const status = error.response.status;
        const data = error.response.data;
        
        if (status === 429) {
          errorMessage = '抱歉，当前请求过于频繁，请稍后再试。';
        } else if (status === 500) {
          errorMessage = '抱歉，服务器遇到了一些问题，请稍后再试。';
        } else if (status === 503) {
          errorMessage = '抱歉，AI服务暂时不可用，请稍后再试。';
        } else if (data && data.message) {
          if (data.message.includes('AI') || data.message.includes('模型') || data.message.includes('API')) {
            errorMessage = '抱歉，AI服务暂时无法响应，请稍后再试。';
          }
        }
      } else if (error.message) {
        if (error.message.includes('timeout') || error.message.includes('超时')) {
          errorMessage = '抱歉，请求超时了，请检查网络后重试。';
        } else if (error.message.includes('network') || error.message.includes('网络')) {
          errorMessage = '抱歉，网络连接出现问题，请检查网络后重试。';
        }
      }
      
      lastMessage.content = errorMessage;
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

const goToChat = () => {
  router.push('/chat');
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

const goToFeedback = () => {
  if (user.value?.isAdmin === 1) {
    router.push('/feedback-management');
  } else {
    router.push('/feedback');
  }
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
  // Element Plus 的 el-image 组件已经支持预览功能
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
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-background);
  position: relative;
  overflow: hidden;
}

/* 导航栏 */
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 32px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: var(--shadow-soft);
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 12px;
  transition: transform var(--transition-fast);
}

.nav-left:hover {
  transform: translateX(4px);
}

.nav-logo {
  font-size: 32px;
  animation: pulse 3s ease-in-out infinite;
}

.nav-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.nav-center {
  display: flex;
  gap: 8px;
}

.nav-btn {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  border: none;
  background: transparent;
  border-radius: var(--radius-full);
  padding: 8px 16px;
  transition: all var(--transition-base);
}

.nav-btn:hover {
  color: var(--color-primary);
  background: rgba(255, 107, 157, 0.1);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  color: var(--color-text-primary);
  padding: 8px 16px;
  border-radius: var(--radius-full);
  transition: all var(--transition-base);
}

.el-dropdown-link:hover {
  background: var(--color-surface-soft);
}

.user-avatar {
  border: 2px solid var(--color-primary-light);
}

.user-name {
  font-weight: 500;
  font-size: 14px;
}

/* 主聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 24px;
  gap: 24px;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
}

/* 侧边栏 */
.sidebar {
  width: 320px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-soft);
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  border: 1px solid rgba(255, 107, 157, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  background: transparent;
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.sidebar-section:first-child {
  flex: 1;
  min-height: 0;
}

.sidebar-section:last-child {
  max-height: 320px;
  min-height: 160px;
  border-top: 1px solid rgba(255, 107, 157, 0.1);
  padding-top: 16px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px 16px;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.new-session-btn {
  background: var(--gradient-1);
  border: none;
  box-shadow: var(--shadow-soft);
  transition: all var(--transition-base);
}

.new-session-btn:hover {
  transform: scale(1.1) rotate(90deg);
  box-shadow: var(--shadow-medium);
}

.session-list {
  flex: 1;
  padding: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.session-item {
  position: relative;
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-base);
  border: 1px solid transparent;
  display: flex;
  align-items: center;
  gap: 12px;
}

.session-item:hover {
  background: rgba(255, 107, 157, 0.05);
  border-color: rgba(255, 107, 157, 0.15);
  transform: translateX(4px);
}

.session-item.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1) 0%, rgba(167, 139, 250, 0.1) 100%);
  border-color: var(--color-primary-light);
}

.session-icon {
  font-size: 20px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 107, 157, 0.1);
  border-radius: var(--radius-md);
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.delete-session-btn {
  position: relative;
  opacity: 0;
  transition: all var(--transition-base);
}

.session-item:hover .delete-session-btn {
  opacity: 1;
}

.delete-session-btn:hover {
  background: rgba(245, 101, 101, 0.1);
}

.file-list {
  padding: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-md);
  padding: 12px;
  transition: all var(--transition-base);
  border: 1px solid rgba(255, 107, 157, 0.08);
  text-align: left;
}

.file-item:hover {
  border-color: var(--color-primary-light);
  background: rgba(255, 107, 157, 0.05);
  transform: translateX(4px);
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

.file-icon {
  font-size: 20px;
  color: var(--color-primary);
}

.file-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
  flex: 1;
}

.file-time {
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-top: 6px;
  text-align: left;
}

/* 聊天区域 */
.chat-area {
  flex: 1;
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-soft);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(255, 107, 157, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.messages-container {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
}

/* 欢迎区域 */
.welcome-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.welcome-icon {
  font-size: 80px;
  margin-bottom: 20px;
  animation: floatSlow 4s ease-in-out infinite;
  filter: drop-shadow(0 8px 24px rgba(255, 107, 157, 0.2));
}

.welcome-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
}

.welcome-text {
  font-size: 15px;
  color: var(--color-text-secondary);
  max-width: 480px;
  line-height: 1.6;
}

/* 消息样式 */
.message {
  display: flex;
  gap: 16px;
  margin-bottom: 28px;
  align-items: flex-start;
  position: relative;
  animation: fadeInUp 0.3s ease-out;
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
  position: absolute;
  bottom: 0;
}

.ai-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-soft);
}

.ai-avatar span {
  font-size: 24px;
}

.user-message-avatar {
  border: 2px solid var(--color-primary-light);
}

.user-avatar-fallback {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-accent-light) 0%, var(--color-primary-light) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 72%;
}

.message-text {
  padding: 14px 18px;
  border-radius: var(--radius-lg);
  font-size: 15px;
  line-height: 1.7;
  word-wrap: break-word;
  display: inline-block;
}

/* 用户消息 */
.user-message {
  flex-direction: row-reverse;
  justify-content: flex-start;
  margin-bottom: 24px;
  padding-right: 60px;
}

.user-message .message-avatar {
  right: 0;
}

.user-message .message-content {
  align-items: flex-end;
}

.user-message .message-text {
  background: var(--gradient-1);
  color: white;
  border-bottom-right-radius: 6px;
  box-shadow: var(--shadow-medium);
}

/* AI消息 */
.ai-message {
  flex-direction: row;
  margin-bottom: 24px;
  padding-left: 60px;
}

.ai-message .message-avatar {
  left: 0;
}

.ai-message .message-content {
  align-items: flex-start;
}

.ai-message .message-text {
  background: white;
  color: var(--color-text-primary);
  border-bottom-left-radius: 6px;
  border: 1px solid rgba(255, 107, 157, 0.15);
  box-shadow: var(--shadow-soft);
}

/* 图片消息 */
.image-container {
  margin-bottom: 12px;
  max-width: 100%;
  overflow: hidden;
  cursor: pointer;
  border-radius: var(--radius-lg);
}

.message-image {
  width: 100%;
  height: 100%;
  max-width: 320px;
  max-height: 400px;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: transform var(--transition-base);
  object-fit: contain;
  box-shadow: var(--shadow-soft);
}

.message-image:hover {
  transform: scale(1.02);
}

.user-message .message-image {
  border-bottom-right-radius: 6px;
}

.ai-message .message-image {
  border-bottom-left-radius: 6px;
}

/* 音频消息 */
.audio-container {
  margin-bottom: 12px;
  display: block;
  width: 100%;
  max-width: 420px;
}

.message-audio {
  width: 100%;
  min-width: 320px;
  height: 48px;
  border-radius: var(--radius-full);
  border: none;
  outline: none;
  transition: all var(--transition-base);
  display: block;
}

.user-message .message-audio {
  background: #f2f2f2;
  border-bottom-right-radius: 6px;
}

/* PDF文档 */
.pdf-container {
  margin-top: 16px;
  padding: 16px 20px;
  background: white;
  border-radius: var(--radius-lg);
  border-bottom-left-radius: 6px;
  border: 1px solid rgba(255, 107, 157, 0.15);
  box-shadow: var(--shadow-soft);
}

.pdf-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.pdf-icon {
  font-size: 20px;
}

.pdf-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.pdf-download-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1) 0%, rgba(167, 139, 250, 0.1) 100%);
  border: 1px solid var(--color-primary-light);
  color: var(--color-primary);
  transition: all var(--transition-base);
}

.pdf-download-btn:hover {
  background: var(--gradient-1);
  color: white;
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}

/* 输入区域 */
.input-area {
  display: flex;
  gap: 12px;
  padding: 20px;
  border-top: 1px solid rgba(255, 107, 157, 0.1);
  align-items: flex-end;
  background: linear-gradient(180deg, transparent 0%, rgba(255, 107, 157, 0.03) 100%);
}

.input-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

/* 预览区域 */
.selected-preview {
  width: 100%;
}

.selected-image-container {
  position: relative;
  display: inline-block;
}

.preview-image {
  max-width: 160px;
  max-height: 120px;
  border-radius: var(--radius-md);
  border: 2px solid var(--color-primary-light);
  object-fit: contain;
  box-shadow: var(--shadow-soft);
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  background: rgba(245, 101, 101, 1);
  border: none;
  box-shadow: var(--shadow-soft);
}

.remove-btn:hover {
  background: rgba(220, 76, 76, 1);
}

.selected-diaries-preview {
  width: 100%;
  padding: 12px 16px;
  background: rgba(255, 107, 157, 0.05);
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 107, 157, 0.15);
}

.diaries-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.diaries-preview-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.clear-btn {
  color: var(--color-text-secondary);
}

.clear-btn:hover {
  color: var(--color-primary);
}

.diaries-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.diary-tag {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: white;
  border-color: var(--color-primary-light);
  color: var(--color-text-primary);
}

.diary-tag:hover {
  border-color: var(--color-primary);
}

/* 输入框 */
.textarea-wrapper {
  position: relative;
  width: 100%;
}

.chat-textarea {
  border-radius: var(--radius-xl);
}

.chat-textarea :deep(.el-textarea__inner) {
  padding: 16px 16px 56px;
  border-radius: var(--radius-xl);
  border: 2px solid rgba(255, 107, 157, 0.15);
  background: rgba(255, 255, 255, 0.9);
  font-size: 15px;
  line-height: 1.6;
  transition: all var(--transition-base);
  resize: none;
}

.chat-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.1);
}

.input-tools {
  position: absolute;
  right: 12px;
  bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 10;
}

.tool-btn {
  border: none;
  background: rgba(255, 107, 157, 0.08);
  transition: all var(--transition-base);
}

.tool-btn:hover {
  background: rgba(255, 107, 157, 0.15);
  transform: scale(1.1);
}

.tool-icon {
  font-size: 18px;
}

/* 录音指示器 */
.recording-indicator {
  display: flex;
  gap: 3px;
  align-items: center;
  padding: 4px;
}

.recording-dot {
  width: 6px;
  height: 6px;
  background: var(--color-primary);
  border-radius: 50%;
  animation: pulse 0.8s ease-in-out infinite;
}

.recording-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.recording-dot:nth-child(3) {
  animation-delay: 0.4s;
}

.tool-divider {
  width: 1px;
  height: 32px;
  background: rgba(255, 107, 157, 0.15);
  margin: 0 4px;
}

.send-btn {
  background: var(--gradient-1);
  border: none;
  box-shadow: var(--shadow-soft);
  transition: all var(--transition-base);
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.1);
  box-shadow: var(--shadow-medium);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.95);
}

/* Markdown 内容样式 */
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
  line-height: 1.7;
}

:deep(.markdown-content) ul,
:deep(.markdown-content) ol {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

:deep(.markdown-content) li {
  margin: 0.3em 0;
}

:deep(.markdown-content) code {
  background: rgba(255, 107, 157, 0.1);
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

:deep(.markdown-content) pre {
  background: #1f2937;
  color: #e5e7eb;
  padding: 1em;
  border-radius: var(--radius-md);
  overflow-x: auto;
  margin: 0.5em 0;
}

:deep(.markdown-content) pre code {
  background: transparent;
  padding: 0;
  color: inherit;
}

:deep(.markdown-content) blockquote {
  border-left: 3px solid var(--color-primary);
  padding-left: 1em;
  margin: 0.5em 0;
  color: var(--color-text-secondary);
  font-style: italic;
  background: rgba(255, 107, 157, 0.05);
  padding: 12px 16px;
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
}

:deep(.markdown-content) a {
  color: var(--color-primary);
  text-decoration: none;
  transition: all var(--transition-fast);
}

:deep(.markdown-content) a:hover {
  color: var(--color-primary-dark);
  text-decoration: underline;
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
  border-top: 1px solid rgba(255, 107, 157, 0.15);
  margin: 1em 0;
}

:deep(.markdown-content) table {
  width: 100%;
  border-collapse: collapse;
  margin: 0.5em 0;
}

:deep(.markdown-content) th,
:deep(.markdown-content) td {
  border: 1px solid rgba(255, 107, 157, 0.15);
  padding: 0.5em;
  text-align: left;
}

:deep(.markdown-content) th {
  background: rgba(255, 107, 157, 0.05);
  font-weight: 600;
}

/* 日记选择对话框 */
:deep(.diary-dialog .el-dialog) {
  border-radius: var(--radius-xl);
  overflow: hidden;
}

:deep(.diary-dialog .el-dialog__header) {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05) 0%, rgba(167, 139, 250, 0.05) 100%);
  padding: 20px 24px;
  margin: 0;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
}

:deep(.diary-dialog .el-dialog__title) {
  font-weight: 600;
  color: var(--color-text-primary);
}

.diary-select-container {
  border-radius: var(--radius-md);
  max-height: 60vh;
  overflow-y: auto;
  padding: 8px;
}

.diary-select-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.diary-select-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  border: 2px solid rgba(255, 107, 157, 0.1);
  cursor: pointer;
  transition: all var(--transition-base);
}

.diary-select-item:hover {
  border-color: var(--color-primary-light);
  background: rgba(255, 107, 157, 0.05);
  transform: translateX(4px);
}

.diary-select-item.selected {
  border-color: var(--color-primary);
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1) 0%, rgba(167, 139, 250, 0.1) 100%);
}

.diary-select-checkbox {
  flex-shrink: 0;
}

.diary-select-content {
  flex: 1;
  min-width: 0;
}

.diary-select-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.diary-select-info {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
  align-items: center;
}

.diary-select-mood {
  padding: 3px 10px;
  background: rgba(255, 107, 157, 0.1);
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
  color: var(--color-primary);
}

.diary-select-time {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.diary-select-check {
  flex-shrink: 0;
  color: var(--color-primary);
  font-size: 20px;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 16px;
}

.dialog-cancel-btn {
  border-color: rgba(255, 107, 157, 0.2);
  color: var(--color-text-secondary);
  border-radius: var(--radius-full);
  padding: 10px 24px;
  transition: all var(--transition-base);
}

.dialog-cancel-btn:hover {
