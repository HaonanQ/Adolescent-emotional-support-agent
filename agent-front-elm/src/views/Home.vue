<template>
  <div class="home-container">
    <!-- 装饰性背景元素 -->
    <div class="decorative-circle circle-1" style="top: -100px; right: -100px;"></div>
    <div class="decorative-circle circle-2" style="bottom: 20%; left: -150px;"></div>
    <div class="decorative-circle circle-3" style="top: 40%; right: 10%;"></div>
    
    <!-- 漂浮的装饰元素 -->
    <div class="floating-elements">
      <div class="floating-item" style="top: 15%; left: 10%; animation-delay: 0s;">✨</div>
      <div class="floating-item" style="top: 30%; right: 15%; animation-delay: 1s;">💫</div>
      <div class="floating-item" style="bottom: 25%; left: 20%; animation-delay: 2s;">🌟</div>
      <div class="floating-item" style="bottom: 35%; right: 10%; animation-delay: 0.5s;">💖</div>
      <div class="floating-item" style="top: 50%; left: 5%; animation-delay: 1.5s;">🌸</div>
    </div>

    <el-container>
      <!-- 导航栏 -->
      <el-header class="navbar" style="animation: fadeInDown 0.6s ease-out;">
        <div class="nav-left">
          <span class="nav-logo">❤️</span>
          <span class="nav-title">青少年情感陪伴智能体</span>
        </div>
        <div class="nav-right">
          <template v-if="user">
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
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button class="nav-btn-secondary" @click="goToLogin">登录</el-button>
            <el-button class="nav-btn-primary" @click="goToLogin">注册</el-button>
          </template>
        </div>
      </el-header>

      <el-main class="main-content">
        <!-- 首页英雄区域 -->
        <section class="hero-section">
          <div class="hero-content" style="animation: fadeInUp 0.8s ease-out 0.2s both;">
            <div class="hero-badge">
              <span class="badge-icon">✨</span>
              <span>温暖你的每一刻</span>
            </div>
            <h1 class="hero-title">
              找到属于你的
              <br>
              <span class="highlight">心灵港湾</span>
            </h1>
            <p class="hero-description">
              我们是专为青少年打造的情感陪伴智能体，用温暖的语言陪伴你度过成长中的每一个时刻。
              无论你是开心、难过还是迷茫，这里永远是你可以倾诉的地方。
            </p>
            <div class="hero-buttons">
              <el-button v-if="user" class="btn-hero-primary" size="large" @click="goToChat">
                <span class="btn-text">开始对话</span>
                <span class="btn-icon">→</span>
              </el-button>
              <el-button v-else class="btn-hero-primary" size="large" @click="goToLogin">
                <span class="btn-text">立即体验</span>
                <span class="btn-icon">→</span>
              </el-button>
              <el-button class="btn-hero-secondary" size="large">了解更多</el-button>
            </div>
            
            <!-- 数据统计 -->
            <div class="hero-stats">
              <div class="stat-item">
                <div class="stat-number">10K+</div>
                <div class="stat-label">用户陪伴</div>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <div class="stat-number">100K+</div>
                <div class="stat-label">对话次数</div>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <div class="stat-number">98%</div>
                <div class="stat-label">满意度</div>
              </div>
            </div>
          </div>
          
          <!-- 右侧视觉区域 -->
          <div class="hero-image" style="animation: float 6s ease-in-out infinite;">
            <div class="image-wrapper">
              <div class="big-icon-main">🤖</div>
              <div class="floating-bubbles">
                <div class="bubble bubble-1">💬</div>
                <div class="bubble bubble-2">💕</div>
                <div class="bubble bubble-3">🌟</div>
                <div class="bubble bubble-4">🎨</div>
                <div class="bubble bubble-5">📝</div>
              </div>
            </div>
          </div>
        </section>

        <!-- 功能展示区域 -->
        <section class="features-section">
          <div class="section-header" style="animation: fadeInUp 0.6s ease-out 0.3s both;">
            <span class="section-subtitle">我们的功能</span>
            <h2 class="section-title">全方位的情感陪伴</h2>
          </div>
          
          <el-row :gutter="24" class="features-grid">
            <el-col 
              :xs="24" 
              :sm="12" 
              :md="8" 
              v-for="(feature, index) in features" 
              :key="feature.icon"
              :style="{ animation: `fadeInUp 0.6s ease-out ${0.4 + index * 0.1}s both` }"
            >
              <el-card class="feature-card" shadow="never">
                <div class="feature-icon-wrapper">
                  <span class="feature-icon">{{ feature.icon }}</span>
                </div>
                <h3>{{ feature.title }}</h3>
                <p>{{ feature.description }}</p>
                <div class="feature-arrow">
                  <el-icon><arrow-right /></el-icon>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </section>

        <!-- 关于我们 -->
        <section class="about-section">
          <div class="about-content" style="animation: fadeInUp 0.6s ease-out 0.6s both;">
            <div class="about-header">
              <span class="about-subtitle">关于我们</span>
              <h2 class="about-title">用心陪伴，温暖成长</h2>
            </div>
            
            <div class="about-grid">
              <div class="about-text-block">
                <div class="about-icon">🌱</div>
                <h4>理解你的每一刻</h4>
                <p>青少年时期是人生中最美好也最困惑的阶段。我们希望通过人工智能技术，为每一位青少年提供一个安全、温暖、可以自由倾诉的空间。</p>
              </div>
              
              <div class="about-text-block">
                <div class="about-icon">💝</div>
                <h4>温暖的情感陪伴</h4>
                <p>在这里，你可以畅所欲言，分享你的快乐与烦恼。我们的AI会用理解和包容的态度，倾听你的每一句话，给予你温暖的回应和建议。</p>
              </div>
              
              <div class="about-text-block">
                <div class="about-icon">🌈</div>
                <h4>与你共同成长</h4>
                <p>愿每一位青少年都能在成长的道路上，找到属于自己的心灵港湾，我们会一直陪伴着你。</p>
              </div>
            </div>
          </div>
        </section>
      </el-main>

      <!-- 页脚 -->
      <el-footer class="footer">
        <div class="footer-content">
          <div class="footer-logo">
            <span>❤️</span>
            <span>青少年情感陪伴智能体</span>
          </div>
          <p class="footer-text">用AI温暖每一颗年轻的心</p>
          <div class="footer-divider"></div>
          <p class="copyright">© 2024 青少年情感陪伴智能体 版权所有</p>
        </div>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowDown, ArrowRight } from '@element-plus/icons-vue';
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
  background: var(--color-background);
  position: relative;
  overflow: hidden;
}

/* 漂浮装饰元素 */
.floating-elements {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.floating-item {
  position: absolute;
  font-size: 28px;
  opacity: 0.6;
  animation: floatSlow 6s ease-in-out infinite;
}

/* 导航栏 */
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 48px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: var(--shadow-soft);
  height: 72px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-logo {
  font-size: 32px;
  animation: pulse 3s ease-in-out infinite;
}

.nav-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-btn-primary {
  background: var(--gradient-1);
  color: white;
  border: none;
  border-radius: var(--radius-full);
  padding: 10px 24px;
  font-weight: 600;
  transition: all var(--transition-base);
}

.nav-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

.nav-btn-secondary {
  background: transparent;
  color: var(--color-text-primary);
  border: 2px solid var(--color-primary-light);
  border-radius: var(--radius-full);
  padding: 10px 24px;
  font-weight: 500;
  transition: all var(--transition-base);
}

.nav-btn-secondary:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
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
  font-size: 15px;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  padding: 0;
  z-index: 2;
}

/* 首页英雄区域 */
.hero-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 80px 48px;
  max-width: 1400px;
  margin: 0 auto;
  gap: 64px;
  min-height: calc(100vh - 72px);
}

.hero-content {
  flex: 1;
  max-width: 600px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(167, 139, 250, 0.1));
  border: 1px solid rgba(255, 107, 157, 0.2);
  padding: 8px 16px;
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-primary-dark);
  margin-bottom: 24px;
}

.badge-icon {
  font-size: 16px;
}

.hero-title {
  font-size: 4rem;
  font-weight: 700;
  line-height: 1.1;
  margin-bottom: 24px;
  color: var(--color-text-primary);
  letter-spacing: -0.03em;
}

.highlight {
  background: var(--gradient-1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  position: relative;
}

.hero-description {
  font-size: 1.125rem;
  line-height: 1.8;
  margin-bottom: 32px;
  color: var(--color-text-secondary);
}

.hero-buttons {
  display: flex;
  gap: 16px;
  margin-bottom: 48px;
}

.btn-hero-primary {
  background: var(--gradient-1);
  color: white;
  border: none;
  border-radius: var(--radius-full);
  padding: 16px 32px;
  font-size: 1rem;
  font-weight: 600;
  box-shadow: var(--shadow-medium);
  transition: all var(--transition-base);
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  position: relative;
}

.btn-hero-primary::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
  transition: left var(--transition-slow);
}

.btn-hero-primary:hover::before {
  left: 100%;
}

.btn-hero-primary:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-large);
}

.btn-hero-primary .btn-icon {
  transition: transform var(--transition-base);
}

.btn-hero-primary:hover .btn-icon {
  transform: translateX(4px);
}

.btn-hero-secondary {
  background: white;
  color: var(--color-text-primary);
  border: 2px solid var(--color-primary-light);
  border-radius: var(--radius-full);
  padding: 16px 32px;
  font-size: 1rem;
  font-weight: 600;
  transition: all var(--transition-base);
}

.btn-hero-secondary:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
  transform: translateY(-3px);
}

/* 数据统计 */
.hero-stats {
  display: flex;
  align-items: center;
  gap: 32px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.stat-number {
  font-size: 2rem;
  font-weight: 700;
  background: var(--gradient-1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
}

.stat-label {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 48px;
  background: linear-gradient(180deg, transparent, var(--color-primary-light), transparent);
}

/* 右侧视觉区域 */
.hero-image {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.image-wrapper {
  position: relative;
  width: 400px;
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.big-icon-main {
  font-size: 180px;
  z-index: 2;
  filter: drop-shadow(0 20px 60px rgba(255, 107, 157, 0.3));
}

.floating-bubbles {
  position: absolute;
  width: 100%;
  height: 100%;
}

.bubble {
  position: absolute;
  font-size: 48px;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 50%;
  box-shadow: var(--shadow-medium);
  animation: floatSlow 5s ease-in-out infinite;
}

.bubble-1 {
  top: 10%;
  left: -20%;
  animation-delay: 0s;
}

.bubble-2 {
  top: 0%;
  right: -10%;
  animation-delay: 1s;
}

.bubble-3 {
  bottom: 20%;
  left: -25%;
  animation-delay: 2s;
}

.bubble-4 {
  bottom: 0%;
  right: -20%;
  animation-delay: 0.5s;
}

.bubble-5 {
  top: 50%;
  right: -30%;
  animation-delay: 1.5s;
}

/* 功能展示区域 */
.features-section {
  background: white;
  padding: 96px 48px;
  position: relative;
  z-index: 2;
}

.section-header {
  text-align: center;
  margin-bottom: 64px;
}

.section-subtitle {
  display: inline-block;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-primary);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 12px;
}

.section-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

.features-grid {
  max-width: 1200px;
  margin: 0 auto;
}

.feature-card {
  text-align: left;
  margin-bottom: 24px;
  transition: all var(--transition-base);
  border: 1px solid rgba(255, 107, 157, 0.1);
  border-radius: var(--radius-xl);
  padding: 32px;
  background: white;
  position: relative;
  overflow: hidden;
}

.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--gradient-1);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform var(--transition-base);
}

.feature-card:hover::before {
  transform: scaleX(1);
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: var(--shadow-large);
  border-color: rgba(255, 107, 157, 0.2);
}

.feature-icon-wrapper {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(167, 139, 250, 0.1));
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  transition: all var(--transition-base);
}

.feature-card:hover .feature-icon-wrapper {
  transform: scale(1.1) rotate(5deg);
  background: var(--gradient-1);
}

.feature-icon {
  font-size: 40px;
  transition: all var(--transition-base);
}

.feature-card h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
}

.feature-card p {
  color: var(--color-text-secondary);
  line-height: 1.7;
  font-size: 0.9375rem;
}

.feature-arrow {
  margin-top: 20px;
  opacity: 0;
  transform: translateX(-10px);
  transition: all var(--transition-base);
}

.feature-card:hover .feature-arrow {
  opacity: 1;
  transform: translateX(0);
}

.feature-arrow .el-icon {
  font-size: 24px;
  color: var(--color-primary);
}

/* 关于我们 */
.about-section {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05) 0%, rgba(167, 139, 250, 0.05) 100%);
  padding: 96px 48px;
  position: relative;
  z-index: 2;
}

.about-content {
  max-width: 1200px;
  margin: 0 auto;
}

.about-header {
  text-align: center;
  margin-bottom: 64px;
}

.about-subtitle {
  display: inline-block;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-primary);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 12px;
}

.about-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

.about-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
}

.about-text-block {
  background: white;
  padding: 32px;
  border-radius: var(--radius-xl);
  border: 1px solid rgba(255, 107, 157, 0.1);
  transition: all var(--transition-base);
}

.about-text-block:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-medium);
}

.about-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.about-text-block h4 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
}

.about-text-block p {
  color: var(--color-text-secondary);
  line-height: 1.7;
  font-size: 0.9375rem;
}

/* 页脚 */
.footer {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  padding: 48px;
  color: white;
  height: auto;
  position: relative;
  z-index: 2;
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
  gap: 12px;
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 12px;
}

.footer-text {
  opacity: 0.8;
  margin-bottom: 24px;
  font-size: 1rem;
}

.footer-divider {
  width: 100px;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--color-primary), transparent);
  margin: 24px auto;
}

.copyright {
  opacity: 0.6;
  font-size: 0.875rem;
}

/* 移动端响应式 */
@media (max-width: 1024px) {
  .hero-section {
    flex-direction: column;
    text-align: center;
    padding: 60px 24px;
    min-height: auto;
  }
  
  .hero-content {
    max-width: 100%;
  }
  
  .hero-title {
    font-size: 2.5rem;
  }
  
  .hero-buttons {
    justify-content: center;
  }
  
  .hero-stats {
    justify-content: center;
  }
  
  .about-grid {
    grid-template-columns: 1fr;
  }
  
  .image-wrapper {
    width: 300px;
    height: 300px;
  }
  
  .big-icon-main {
    font-size: 120px;
  }
}

@media (max-width: 640px) {
  .navbar {
    padding: 0 16px;
  }
  
  .nav-title {
    display: none;
  }
  
  .features-section,
  .about-section {
    padding: 60px 16px;
  }
  
  .hero-title {
    font-size: 2rem;
  }
}
</style>
