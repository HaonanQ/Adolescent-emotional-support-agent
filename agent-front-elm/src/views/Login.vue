<template>
  <div class="login-container">
    <el-button class="back-home" type="primary" text @click="goToHome">
      <el-icon><arrow-left /></el-icon>
      返回首页
    </el-button>
    <el-card class="login-card" shadow="hover">
      <div class="logo">
        <span class="heart-icon">❤️</span>
        <h1>心灵港湾</h1>
        <p>青少年情感陪伴智能体</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登 录" name="login">
          <el-form :model="form" label-width="0" class="login-form">
            <el-form-item>
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                size="large"
                clearable
                prefix-icon="User"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                show-password
                prefix-icon="Lock"
                @keyup.enter="handleSubmit"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                @click="handleSubmit"
                style="width: 100%"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注 册" name="register">
          <el-form :model="form" label-width="0" class="login-form">
            <el-form-item>
              <el-input
                v-model="form.username"
                placeholder="请输入用户名（4-10个字符）"
                size="large"
                clearable
                prefix-icon="User"
                maxlength="10"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码（6-16个字符）"
                size="large"
                show-password
                prefix-icon="Lock"
                maxlength="16"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="请确认密码"
                size="large"
                show-password
                prefix-icon="Lock"
                maxlength="16"
                @keyup.enter="handleSubmit"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                @click="handleSubmit"
                style="width: 100%"
              >
                注 册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
        style="margin-top: 16px"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowLeft, User, Lock } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { login as apiLogin, register as apiRegister } from '../api';

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

  // 前端校验
  if (!form.value.username || !form.value.username.trim()) {
    errorMessage.value = '用户名不能为空';
    return;
  }
  
  if (!form.value.password || !form.value.password.trim()) {
    errorMessage.value = '密码不能为空';
    return;
  }

  // 注册时的额外校验
  if (activeTab.value === 'register') {
    if (form.value.username.trim().length < 4 || form.value.username.trim().length > 10) {
      errorMessage.value = '用户名长度必须在4-20个字符之间';
      return;
    }
    
    if (form.value.password.trim().length < 6 || form.value.password.trim().length > 16) {
      errorMessage.value = '密码长度必须在6-20个字符之间';
      return;
    }
    
    if (form.value.password !== form.value.confirmPassword) {
      errorMessage.value = '两次输入的密码不一致';
      return;
    }
  }

  loading.value = true;

  try {
    if (activeTab.value === 'login') {
      const response = await apiLogin(form.value.username, form.value.password);
      if (response.code === 0 && response.data) {
        const user = response.data;
        localStorage.setItem('user', JSON.stringify(user));
        ElMessage.success('登录成功');
        router.push('/chat');
      } else {
        errorMessage.value = response.message || '登录失败';
      }
    } else {
      const response = await apiRegister(form.value.username, form.value.password);
      if (response.code === 0) {
        ElMessage.success('注册成功，请登录');
        activeTab.value = 'login';
      } else {
        errorMessage.value = response.message || '注册失败';
      }
    }
  } catch (error) {
    console.error('Error:', error);
    errorMessage.value = '网络错误，请稍后重试';
  } finally {
    loading.value = false;
  }
};

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
  background-image: url('../image/bg.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  position: relative;
}

.back-home {
  position: absolute;
  top: 20px;
  left: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
}

.logo {
  text-align: center;
  margin-bottom: 24px;
}

.heart-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 8px;
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

.login-tabs {
  margin-top: 24px;
}

.login-tabs :deep(.el-tabs__header) {
  display: flex;
  justify-content: center;
}

.login-tabs :deep(.el-tabs__nav-wrap) {
  width: 100%;
}

.login-tabs :deep(.el-tabs__nav) {
  width: 100%;
  display: flex;
}

.login-tabs :deep(.el-tabs__item) {
  flex: 1;
  text-align: center;
  font-size: 16px;
  font-weight: 500;
}

.login-form {
  margin-top: 24px;
}
</style>
