<template>
  <div class="login-container">
    <div class="back-home" @click="goToHome">
      ← 返回首页
    </div>
    <div class="login-card">
      <div class="logo">
        <span class="heart-icon">❤️</span>
        <h1>心灵港湾</h1>
        <p>青少年情感陪伴智能体</p>
      </div>
      
      <div class="tabs">
        <button 
          :class="['tab', { active: activeTab === 'login' }]"
          @click="activeTab = 'login'"
        >
          登 录
        </button>
        <button 
          :class="['tab', { active: activeTab === 'register' }]"
          @click="activeTab = 'register'"
        >
          注 册
        </button>
      </div>

      <form @submit.prevent="handleSubmit" class="login-form">
        <div class="form-group">
          <input 
            type="text" 
            v-model="form.username" 
            placeholder="请输入用户名"
            required
          />
        </div>
        
        <div class="form-group">
          <input 
            type="password" 
            v-model="form.password" 
            placeholder="请输入密码"
            required
          />
        </div>

        <div v-if="activeTab === 'register'" class="form-group">
          <input 
            type="password" 
            v-model="form.confirmPassword" 
            placeholder="请确认密码"
            required
          />
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '加载中...' : (activeTab === 'login' ? '登 录' : '注 册') }}
        </button>
      </form>

      <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { login, register } from '../api';

const router = useRouter();
const activeTab = ref('login');
const loading = ref(false);
const errorMessage = ref('');

const form = ref({
  username: '',
  password: '',
  confirmPassword: '',
});

const handleSubmit = async () => {
  errorMessage.value = '';
  
  if (activeTab.value === 'register' && form.value.password !== form.value.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致';
    return;
  }

  loading.value = true;

  try {
    if (activeTab.value === 'login') {
      const response = await login(form.value.username, form.value.password);
      if (response.data.code === 0 && response.data.data) {
        const user = response.data.data;
        localStorage.setItem('user', JSON.stringify(user));
        router.push('/chat');
      } else {
        errorMessage.value = response.data.message || '登录失败';
      }
    } else {
      const response = await register(form.value.username, form.value.password);
      if (response.data.code === 0) {
        alert('注册成功，请登录');
        activeTab.value = 'login';
      } else {
        errorMessage.value = response.data.message || '注册失败';
      }
    }
  } catch (error) {
    console.error('Error:', error);
    errorMessage.value = '网络错误，请稍后重试';
  } finally {
    loading.value = false;
  }
};

/**
 * 跳转到首页
 */
const goToHome = () => {
  router.push('/');
};
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #f0f4f8 0%, #d9e2ec 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  position: relative;
}

.back-home {
  position: absolute;
  top: 20px;
  left: 20px;
  padding: 8px 16px;
  background: white;
  color: #3b82f6;
  border: 2px solid #3b82f6;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 500;
}

.back-home:hover {
  background: #3b82f6;
  color: white;
}

.login-card {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.logo {
  text-align: center;
  margin-bottom: 30px;
}

.heart-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 10px;
}

.logo h1 {
  font-size: 28px;
  color: #334155;
  margin: 0 0 8px 0;
}

.logo p {
  color: #64748b;
  font-size: 14px;
  margin: 0;
}

.tabs {
  display: flex;
  margin-bottom: 24px;
  border-bottom: 2px solid #e2e8f0;
}

.tab {
  flex: 1;
  padding: 12px;
  border: none;
  background: none;
  font-size: 16px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  position: relative;
  transition: all 0.3s;
}

.tab:hover {
  color: #3b82f6;
}

.tab.active {
  color: #3b82f6;
}

.tab.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  height: 2px;
  background: #3b82f6;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.submit-btn {
  padding: 14px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error-message {
  margin-top: 16px;
  padding: 12px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 14px;
  text-align: center;
}
</style>
