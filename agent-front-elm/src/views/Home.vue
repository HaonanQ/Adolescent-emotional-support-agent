<template>
  <div class="home-container">
    <el-container>
      <el-header class="navbar">
        <div class="nav-left">
          <span class="nav-logo">❤️</span>
          <span class="nav-title">青少年情感陪伴智能体</span>
        </div>
        <div class="nav-right">
          <template v-if="user">
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
          </template>
          <template v-else>
            <el-button type="primary" @click="goToLogin">登录</el-button>
            <el-button @click="goToLogin">注册</el-button>
          </template>
        </div>
      </el-header>

      <el-main class="main-content">
        <section class="hero-section">
          <div class="hero-content">
            <h1 class="hero-title">
              找到属于你的<br />
              <span class="highlight">心灵港湾</span>
            </h1>
            <p class="hero-description">
              我们是专为青少年打造的情感陪伴智能体，用温暖的语言陪伴你度过成长中的每一个时刻。
              无论你是开心、难过还是迷茫，这里永远是你可以倾诉的地方。
            </p>
            <div class="hero-buttons">
              <el-button v-if="user" type="primary" size="large" @click="goToChat">开始对话</el-button>
              <el-button v-else type="primary" size="large" @click="goToLogin">立即体验</el-button>
              <el-button size="large">了解更多</el-button>
            </div>
          </div>
          <div class="hero-image">
            <div class="image-placeholder">
              <span class="big-icon">💫</span>
            </div>
          </div>
        </section>

        <section class="features-section">
          <h2 class="section-title">我们的功能</h2>
          <el-row :gutter="24">
            <el-col :xs="24" :sm="12" :md="8" v-for="feature in features" :key="feature.icon">
              <el-card class="feature-card" shadow="hover">
                <div class="feature-icon">{{ feature.icon }}</div>
                <h3>{{ feature.title }}</h3>
                <p>{{ feature.description }}</p>
              </el-card>
            </el-col>
          </el-row>
        </section>

        <section class="about-section">
          <div class="about-content">
            <h2 class="section-title">关于我们</h2>
            <p class="about-text">
              青少年时期是人生中最美好也最困惑的阶段。我们希望通过人工智能技术，
              为每一位青少年提供一个安全、温暖、可以自由倾诉的空间。
            </p>
            <p class="about-text">
              在这里，你可以畅所欲言，分享你的快乐与烦恼。我们的AI会用理解和包容的态度，
              倾听你的每一句话，给予你温暖的回应和建议。
            </p>
            <p class="about-text">
              愿每一位青少年都能在成长的道路上，找到属于自己的心灵港湾。
            </p>
          </div>
        </section>
      </el-main>

      <el-footer class="footer">
        <div class="footer-content">
          <div class="footer-logo">
            <span>❤️</span>
            <span>青少年情感陪伴智能体</span>
          </div>
          <p class="footer-text">用AI温暖每一颗年轻的心</p>
          <p class="copyright">© 2024 青少年情感陪伴智能体 版权所有</p>
        </div>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowDown } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

const router = useRouter();
const user = ref(null);

const features = [
  { icon: '🤖', title: '智能对话', description: '24小时在线的AI情感陪伴，随时倾听你的心声，给予温暖回应' },
  { icon: '🎤', title: '语音交流', description: '支持语音输入和输出，让对话更自然，更贴近真实陪伴' },
  { icon: '📷', title: '图像识别', description: '上传图片与AI分享，让AI理解你的情感状态，给出更贴切的建议' },
  { icon: '📝', title: '情绪日记', description: '记录你的情绪变化，追踪心理健康，见证成长的每一步' },
  { icon: '🔒', title: '隐私保护', description: '所有对话内容严格保密，你的隐私是我们最重视的事情' },
  { icon: '🌈', title: '温暖陪伴', description: '理解、包容、不评判，用温暖的语言陪伴你度过每一个时刻' },
];

onMounted(() => {
  const storedUser = localStorage.getItem('user');
  if (storedUser) {
    user.value = JSON.parse(storedUser);
  }
});

const goToLogin = () => {
  router.push('/login');
};

const goToChat = () => {
  router.push('/chat');
};

const goToProfile = () => {
  router.push('/profile');
};

const handleLogout = () => {
  localStorage.removeItem('user');
  user.value = null;
  ElMessage.success('退出登录成功');
  router.push('/');
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
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  height: 64px;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-logo {
  font-size: 24px;
}

.nav-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
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

.main-content {
  flex: 1;
  padding: 0;
}

.hero-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 64px 24px;
  max-width: 1200px;
  margin: 0 auto;
  gap: 32px;
}

.hero-content {
  flex: 1;
  color: white;
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 24px;
}

.highlight {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-description {
  font-size: 18px;
  line-height: 1.8;
  margin-bottom: 32px;
  opacity: 0.9;
}

.hero-buttons {
  display: flex;
  gap: 16px;
}

.hero-image {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.image-placeholder {
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}

.big-icon {
  font-size: 128px;
}

.features-section {
  background: white;
  padding: 64px 24px;
}

.section-title {
  text-align: center;
  font-size: 40px;
  font-weight: 700;
  color: #333;
  margin-bottom: 48px;
}

.feature-card {
  text-align: center;
  margin-bottom: 24px;
  transition: all 0.3s;
}

.feature-card:hover {
  transform: translateY(-8px);
}

.feature-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.feature-card h3 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.feature-card p {
  color: #666;
  line-height: 1.6;
}

.about-section {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  padding: 64px 24px;
}

.about-content {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
  color: white;
}

.about-content .section-title {
  color: white;
}

.about-text {
  font-size: 18px;
  line-height: 1.8;
  margin-bottom: 24px;
  opacity: 0.95;
}

.footer {
  background: #1a1a2e;
  padding: 32px 24px;
  color: white;
  height: auto;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}

.footer-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}

.footer-text {
  opacity: 0.8;
  margin-bottom: 8px;
}

.copyright {
  opacity: 0.6;
  font-size: 14px;
}
</style>
