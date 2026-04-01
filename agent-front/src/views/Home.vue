<template>
  <div class="home-container">
    <nav class="navbar">
      <div class="nav-left">
        <span class="nav-logo">❤️</span>
        <span class="nav-title">青少年情感陪伴智能体</span>
      </div>
      <div class="nav-right">
        <template v-if="user">
          <!-- <button @click="goToChat" class="chat-btn">开始对话</button> -->
          <div class="user-dropdown">
            <div class="user-info" @mouseenter="handleMouseEnter" @mouseleave="handleMouseLeave">
              <div v-if="user.avatar" class="user-avatar">
                <img :src="user.avatar" alt="用户头像" class="user-avatar-img" />
              </div>
              <div v-else class="user-avatar">
                {{ user.username?.charAt(0) || 'U' }}
              </div>
              <span class="user-name">{{ user.username }}</span>
            </div>
            <transition name="dropdown">
              <div v-if="showDropdown" class="dropdown-menu" @mouseenter="handleMouseEnter" @mouseleave="handleMouseLeave">
                <div class="dropdown-item" @click="goToProfile">个人中心</div>
                <div class="dropdown-item" @click="handleLogout">退出登录</div>
              </div>
            </transition>
          </div>
        </template>
        <template v-else>
          <button @click="goToLogin" class="login-btn">登录</button>
          <button @click="goToLogin" class="register-btn">注册</button>
        </template>
      </div>
    </nav>

    <main class="main-content">
      <section class="hero-section">
        <div class="hero-content">
          <h1 class="hero-title">
            找到属于你的<br/>
            <span class="highlight">心灵港湾</span>
          </h1>
          <p class="hero-description">
            我们是专为青少年打造的情感陪伴智能体，用温暖的语言陪伴你度过成长中的每一个时刻。
            无论你是开心、难过还是迷茫，这里永远是你可以倾诉的地方。
          </p>
          <div class="hero-buttons">
            <button v-if="user" @click="goToChat" class="primary-btn">开始对话</button>
            <button v-else @click="goToLogin" class="primary-btn">立即体验</button>
            <button class="secondary-btn">了解更多</button>
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
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon">🤖</div>
            <h3>智能对话</h3>
            <p>24小时在线的AI情感陪伴，随时倾听你的心声，给予温暖回应</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🎤</div>
            <h3>语音交流</h3>
            <p>支持语音输入和输出，让对话更自然，更贴近真实陪伴</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">📷</div>
            <h3>图像识别</h3>
            <p>上传图片与AI分享，让AI理解你的情感状态，给出更贴切的建议</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">📝</div>
            <h3>情绪日记</h3>
            <p>记录你的情绪变化，追踪心理健康，见证成长的每一步</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🔒</div>
            <h3>隐私保护</h3>
            <p>所有对话内容严格保密，你的隐私是我们最重视的事情</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🌈</div>
            <h3>温暖陪伴</h3>
            <p>理解、包容、不评判，用温暖的语言陪伴你度过每一个时刻</p>
          </div>
        </div>
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
    </main>

    <footer class="footer">
      <div class="footer-content">
        <div class="footer-logo">
          <span>❤️</span>
          <span>青少年情感陪伴智能体</span>
        </div>
        <p class="footer-text">用AI温暖每一颗年轻的心</p>
        <p class="copyright">© 2024 青少年情感陪伴智能体 版权所有</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const user = ref(null);
const showDropdown = ref(false);
let hideTimeout = null;

/**
 * 初始化用户状态
 */
onMounted(() => {
  const storedUser = localStorage.getItem('user');
  if (storedUser) {
    user.value = JSON.parse(storedUser);
  }
});

/**
 * 清除定时器
 */
onUnmounted(() => {
  if (hideTimeout) {
    clearTimeout(hideTimeout);
  }
});

/**
 * 鼠标进入 - 显示下拉框
 */
const handleMouseEnter = () => {
  if (hideTimeout) {
    clearTimeout(hideTimeout);
    hideTimeout = null;
  }
  showDropdown.value = true;
};

/**
 * 鼠标离开 - 延迟隐藏下拉框
 */
const handleMouseLeave = () => {
  hideTimeout = setTimeout(() => {
    showDropdown.value = false;
  }, 200);
};

/**
 * 跳转到登录页面
 */
const goToLogin = () => {
  router.push('/login');
};

/**
 * 跳转到聊天页面
 */
const goToChat = () => {
  router.push('/chat');
};

/**
 * 跳转到个人中心
 */
const goToProfile = () => {
  showDropdown.value = false;
  router.push('/profile');
};

/**
 * 处理退出登录
 */
const handleLogout = () => {
  showDropdown.value = false;
  localStorage.removeItem('user');
  user.value = null;
  router.push('/');
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
  padding: 1rem 2rem;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.nav-logo {
  font-size: 1.5rem;
}

.nav-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-right: 15px; /* 让整个用户区域向左移动 */
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.9rem;
  overflow: hidden;
}

.user-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-dropdown {
  position: relative;
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.user-name {
  color: #333;
  font-weight: 500;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-top: 8px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  min-width: 150px;
  z-index: 1000;
  overflow: hidden;
}

/* 下拉框过渡动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}

.dropdown-item {
  padding: 0.75rem 1.25rem;
  cursor: pointer;
  transition: all 0.2s;
  color: #333;
  font-size: 0.95rem;
}

.dropdown-item:hover {
  background: #f0f0f0;
  color: #667eea;
}

.login-btn,
.register-btn,
.chat-btn,
.logout-btn {
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.3s;
}

.login-btn {
  background: transparent;
  border: 2px solid #667eea;
  color: #667eea;
}

.login-btn:hover {
  background: #667eea;
  color: white;
}

.register-btn,
.chat-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
}

.register-btn:hover,
.chat-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.logout-btn {
  background: transparent;
  border: 2px solid #ff6b6b;
  color: #ff6b6b;
}

.logout-btn:hover {
  background: #ff6b6b;
  color: white;
}

.main-content {
  flex: 1;
}

.hero-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4rem 2rem;
  max-width: 1200px;
  margin: 0 auto;
  gap: 2rem;
}

.hero-content {
  flex: 1;
  color: white;
}

.hero-title {
  font-size: 3rem;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 1.5rem;
}

.highlight {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-description {
  font-size: 1.1rem;
  line-height: 1.8;
  margin-bottom: 2rem;
  opacity: 0.9;
}

.hero-buttons {
  display: flex;
  gap: 1rem;
}

.primary-btn,
.secondary-btn {
  padding: 0.875rem 2rem;
  border-radius: 25px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.primary-btn {
  background: white;
  border: none;
  color: #667eea;
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.secondary-btn {
  background: transparent;
  border: 2px solid white;
  color: white;
}

.secondary-btn:hover {
  background: white;
  color: #667eea;
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
  font-size: 8rem;
}

.features-section {
  background: white;
  padding: 4rem 2rem;
}

.section-title {
  text-align: center;
  font-size: 2.5rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 3rem;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.feature-card {
  background: #f8f9fa;
  padding: 2rem;
  border-radius: 16px;
  text-align: center;
  transition: all 0.3s;
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.feature-card h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.75rem;
}

.feature-card p {
  color: #666;
  line-height: 1.6;
}

.about-section {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  padding: 4rem 2rem;
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
  font-size: 1.1rem;
  line-height: 1.8;
  margin-bottom: 1.5rem;
  opacity: 0.95;
}

.footer {
  background: #1a1a2e;
  padding: 2rem;
  color: white;
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
  gap: 0.5rem;
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.footer-text {
  opacity: 0.8;
  margin-bottom: 0.5rem;
}

.copyright {
  opacity: 0.6;
  font-size: 0.9rem;
}
</style>
