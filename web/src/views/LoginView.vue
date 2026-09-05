<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'
import { clearPermissions, loadPermissions } from '../auth'

const router = useRouter()
const user = ref('')
const pass = ref('')
const loading = ref(false)

const query = new URLSearchParams(location.search)
if (query.get('timeout') === '1' || query.get('reason') === 'expired') {
  ElMessage.warning('会话已超时，请重新登录')
}

const login = async () => {
  if (!user.value || !pass.value) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  loading.value = true
  clearPermissions()

  try {
    const credentials = new URLSearchParams({ username: user.value, password: pass.value })
    await http.post('/auth/login', credentials, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
    await http.get('/system/me')
    await loadPermissions()
    const redirect = query.get('redirect')
    await router.replace(redirect?.startsWith('/') && !redirect.startsWith('//') ? redirect : '/dashboard')
  } catch (error) {
    clearPermissions()
    ElMessage.error(error instanceof Error ? error.message : '账号或密码错误，或账号已被锁定')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card" aria-labelledby="login-title">
      <header class="login-heading">
        <div class="login-mark" aria-hidden="true">康</div>
        <h1 id="login-title">康复医院运营管理系统</h1>
        <p>欠费通报 · 预出院管理</p>
      </header>

      <form class="login-form" @submit.prevent="login">
        <label for="login-account">账号</label>
        <el-input id="login-account" v-model="user" name="username" autocomplete="username" placeholder="请输入登录名" size="large" autofocus />

        <label for="login-password">密码</label>
        <el-input id="login-password" v-model="pass" name="password" type="password" autocomplete="current-password" placeholder="请输入密码" show-password size="large" />

        <el-button class="login-button" type="primary" native-type="submit" :loading="loading">登 录</el-button>
      </form>

      <p class="login-tip">请使用院内系统账号登录，登录后将根据账号权限展示可用功能。</p>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 32px 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 60%, #3b82f6 100%);
}

.login-card {
  width: min(400px, 100%);
  padding: 36px;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 20px 50px rgba(0, 0, 0, .25);
}

.login-heading { text-align: center; }

.login-mark {
  width: 46px;
  height: 46px;
  margin: 0 auto 14px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  color: #fff;
  background: linear-gradient(145deg, #3578f4, #2256c7);
  box-shadow: 0 8px 18px rgba(37, 99, 235, .24);
  font-size: 20px;
  font-weight: 700;
}

.login-heading h1 {
  margin: 0 0 6px;
  color: #1f2937;
  font-size: 22px;
  line-height: 1.45;
}

.login-heading p {
  margin: 0 0 28px;
  color: #6b7280;
  font-size: 13px;
}

.login-form label {
  display: block;
  margin: 14px 0 6px;
  color: #374151;
  font-size: 13px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #d1d5db inset;
}

.login-form :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #94a3b8 inset; }
.login-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #2563eb inset; }

.login-button {
  width: 100%;
  height: 42px;
  margin-top: 26px;
  border-color: #2563eb;
  border-radius: 8px;
  background: #2563eb;
  font-size: 15px;
}

.login-button:hover,
.login-button:focus {
  border-color: #1d4ed8;
  background: #1d4ed8;
}

.login-tip {
  margin: 18px 0 0;
  padding: 10px 12px;
  border-radius: 8px;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 768px) {
  .login-page { padding: 24px 16px; }
  .login-card { padding: 30px 22px; }
}
</style>
