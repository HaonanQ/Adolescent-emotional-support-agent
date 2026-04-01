<template>
  <div class="profile-container">
    <nav class="navbar">
      <div class="nav-left">
        <span class="nav-logo">❤️</span>
        <span class="nav-title">青少年情感陪伴智能体</span>
      </div>
      <div class="nav-right">
        <span class="back-link" @click="goBack">← 返回</span>
      </div>
    </nav>

    <main class="main-content">
      <div class="profile-card">
        <h2 class="profile-title">个人中心</h2>
        
        <!-- 头像区域 -->
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <img v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar-img" alt="用户头像" />
            <div v-else class="avatar-placeholder">
              {{ userInfo.username?.charAt(0) || 'U' }}
            </div>
            <label class="avatar-upload-btn" for="avatar-input">
              更换头像
            </label>
            <input 
              type="file" 
              id="avatar-input" 
              accept="image/*" 
              style="display: none" 
              @change="handleAvatarChange"
            />
          </div>
        </div>

        <!-- 信息编辑区 -->
        <div class="form-section">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <input type="text" class="form-input" :value="userInfo.username" disabled />
          </div>

          <div class="form-group">
            <label class="form-label">昵称</label>
            <div class="input-wrapper">
              <input 
                type="text" 
                class="form-input" 
                v-model="editNickname" 
                placeholder="请输入昵称"
              />
              <button class="save-btn" @click="handleUpdateNickname">保存</button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">当前状态</label>
            <div class="status-options">
              <div 
                class="status-option" 
                :class="{ active: userInfo.relationshipStatus === 0 }"
                @click="handleUpdateStatus(0)"
              >
                <span class="status-icon">💔</span>
                <span class="status-text">单身</span>
              </div>
              <div 
                class="status-option" 
                :class="{ active: userInfo.relationshipStatus === 1 }"
                @click="handleUpdateStatus(1)"
              >
                <span class="status-icon">💕</span>
                <span class="status-text">恋爱中</span>
              </div>
            </div>
          </div>

          <div class="password-section">
            <h3 class="section-title">修改密码</h3>
            <div class="form-group">
              <label class="form-label">旧密码</label>
              <input 
                type="password" 
                class="form-input" 
                v-model="oldPassword" 
                placeholder="请输入旧密码"
              />
            </div>
            <div class="form-group">
              <label class="form-label">新密码</label>
              <input 
                type="password" 
                class="form-input" 
                v-model="newPassword" 
                placeholder="请输入新密码"
              />
            </div>
            <div class="form-group">
              <label class="form-label">确认新密码</label>
              <input 
                type="password" 
                class="form-input" 
                v-model="confirmPassword" 
                placeholder="请再次输入新密码"
              />
            </div>
            <button class="password-btn" @click="handleUpdatePassword">修改密码</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getCurrentUserInfo, updateNickname, updatePassword, updateAvatar, updateStatus, uploadImage } from '../api';

const router = useRouter();
const userInfo = ref({});
const editNickname = ref('');
const oldPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');

/**
 * 初始化用户信息
 */
onMounted(async () => {
  const user = JSON.parse(localStorage.getItem('user'));
  if (!user) {
    router.push('/login');
    return;
  }
  
  userInfo.value = user;
  editNickname.value = user.nickname || '';
  
  try {
    const response = await getCurrentUserInfo();
    if (response.data.code === 0 && response.data.data) {
      userInfo.value = response.data.data;
      editNickname.value = response.data.data.nickname || '';
      localStorage.setItem('user', JSON.stringify(response.data.data));
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
  }
});

/**
 * 返回上一页
 */
const goBack = () => {
  router.back();
};

/**
 * 处理头像更换
 */
const handleAvatarChange = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  try {
    // 先上传图片到云存储
    const uploadResponse = await uploadImage(file);
    if (uploadResponse.data.code === 0 && uploadResponse.data.data) {
      const avatarUrl = uploadResponse.data.data.fileUrl;
      // 更新用户头像
      const response = await updateAvatar(avatarUrl);
      if (response.data.code === 0 && response.data.data) {
        userInfo.value = response.data.data;
        localStorage.setItem('user', JSON.stringify(response.data.data));
        alert('头像更新成功！');
      }
    }
  } catch (error) {
    console.error('更新头像失败:', error);
    alert('更新头像失败，请重试');
  }
};

/**
 * 处理昵称更新
 */
const handleUpdateNickname = async () => {
  if (!editNickname.value.trim()) {
    alert('昵称不能为空');
    return;
  }

  try {
    const response = await updateNickname(editNickname.value);
    if (response.data.code === 0 && response.data.data) {
      userInfo.value = response.data.data;
      localStorage.setItem('user', JSON.stringify(response.data.data));
      alert('昵称更新成功！');
    }
  } catch (error) {
    console.error('更新昵称失败:', error);
    alert('更新昵称失败，请重试');
  }
};

/**
 * 处理状态更新
 */
const handleUpdateStatus = async (status) => {
  try {
    const response = await updateStatus(status);
    if (response.data.code === 0 && response.data.data) {
      userInfo.value = response.data.data;
      localStorage.setItem('user', JSON.stringify(response.data.data));
      alert('状态更新成功！');
    }
  } catch (error) {
    console.error('更新状态失败:', error);
    alert('更新状态失败，请重试');
  }
};

/**
 * 处理密码更新
 */
const handleUpdatePassword = async () => {
  if (!oldPassword.value || !newPassword.value || !confirmPassword.value) {
    alert('请填写完整的密码信息');
    return;
  }

  if (newPassword.value !== confirmPassword.value) {
    alert('两次输入的新密码不一致');
    return;
  }

  try {
    const response = await updatePassword(oldPassword.value, newPassword.value);
    if (response.data.code === 0) {
      alert('密码修改成功！');
      oldPassword.value = '';
      newPassword.value = '';
      confirmPassword.value = '';
    }
  } catch (error) {
    console.error('修改密码失败:', error);
    alert('修改密码失败，请检查旧密码是否正确');
  }
};
</script>

<style scoped>
.profile-container {
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

.back-link {
  color: #667eea;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
}

.back-link:hover {
  color: #764ba2;
}

.main-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 2rem;
}

.profile-card {
  background: white;
  border-radius: 20px;
  padding: 2.5rem;
  width: 100%;
  max-width: 600px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.profile-title {
  text-align: center;
  font-size: 2rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 2rem;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 2.5rem;
}

.avatar-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.avatar-img {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #667eea;
}

.avatar-placeholder {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
  font-weight: 600;
}

.avatar-upload-btn {
  padding: 0.5rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.3s;
}

.avatar-upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-size: 0.95rem;
  font-weight: 600;
  color: #555;
}

.form-input {
  padding: 0.75rem 1rem;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  font-size: 1rem;
  transition: all 0.3s;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
}

.form-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.input-wrapper {
  display: flex;
  gap: 0.75rem;
}

.input-wrapper .form-input {
  flex: 1;
}

.save-btn {
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.3s;
  white-space: nowrap;
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.status-options {
  display: flex;
  gap: 1rem;
}

.status-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1.5rem;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.status-option:hover {
  border-color: #667eea;
}

.status-option.active {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.1);
}

.status-icon {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
}

.status-text {
  font-size: 1rem;
  font-weight: 500;
  color: #555;
}

.password-section {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 2px solid #f0f0f0;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 1.5rem;
}

.password-btn {
  width: 100%;
  padding: 0.875rem;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  transition: all 0.3s;
  margin-top: 0.5rem;
}

.password-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(240, 147, 251, 0.4);
}
</style>
