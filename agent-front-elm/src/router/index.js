import { createRouter, createWebHistory } from 'vue-router';
import Home from '../views/Home.vue';
import Login from '../views/Login.vue';
import Chat from '../views/Chat.vue';
import EmotionDiary from '../views/EmotionDiary.vue';
import Profile from '../views/Profile.vue';
import EmotionClassroom from '../views/EmotionClassroom.vue';
import ArticleDetail from '../views/ArticleDetail.vue';
import ArticleEditor from '../views/ArticleEditor.vue';
import UserManagement from '../views/UserManagement.vue';
import KnowledgeManagement from '../views/KnowledgeManagement.vue';

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
  },
  {
    path: '/chat',
    name: 'Chat',
    component: Chat,
    meta: { requiresAuth: true },
  },
  {
    path: '/emotion-diary',
    name: 'EmotionDiary',
    component: EmotionDiary,
    meta: { requiresAuth: true },
  },
  {
    path: '/emotion-classroom',
    name: 'EmotionClassroom',
    component: EmotionClassroom,
    meta: { requiresAuth: true },
  },
  {
    path: '/article-detail',
    name: 'ArticleDetail',
    component: ArticleDetail,
    meta: { requiresAuth: true },
  },
  {
    path: '/article-editor',
    name: 'ArticleEditor',
    component: ArticleEditor,
    meta: { requiresAuth: true },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
    meta: { requiresAuth: true },
  },
  {
    path: '/user-management',
    name: 'UserManagement',
    component: UserManagement,
    meta: { requiresAuth: true },
  },
  {
    path: '/knowledge-management',
    name: 'KnowledgeManagement',
    component: KnowledgeManagement,
    meta: { requiresAuth: true },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const user = JSON.parse(localStorage.getItem('user'));
  if (to.meta.requiresAuth && !user) {
    next('/login');
  } else {
    next();
  }
});

export default router;
