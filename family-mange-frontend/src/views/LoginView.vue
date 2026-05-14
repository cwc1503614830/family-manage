<template>
  <div v-if="!authReady" class="login-shell">
    <div class="login-card">
      <div class="brand login-brand">
        <div class="brand-mark">H</div>
        <div>
          <strong>HomeGuard</strong>
          <span>家庭资产</span>
        </div>
      </div>
      <p class="eyebrow">Family Asset Console</p>
      <h1>家庭管理登录</h1>
    </div>
  </div>

  <div v-else class="login-shell">
    <form class="login-card" @submit.prevent="$emit('login', { ...form })">
      <div class="brand login-brand">
        <div class="brand-mark">H</div>
        <div>
          <strong>HomeGuard</strong>
          <span>家庭资产</span>
        </div>
      </div>
      <p class="eyebrow">Family Asset Console</p>
      <h1>家庭管理登录</h1>
      <label>
        <span>账号</span>
        <input v-model.trim="form.username" autocomplete="username" required />
      </label>
      <label>
        <span>密码</span>
        <input v-model="form.password" autocomplete="current-password" required type="password" />
      </label>
      <p v-if="error" class="form-error">{{ error }}</p>
      <button class="primary-btn" type="submit" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>
      <span class="login-hint">默认账号：admin / admin123</span>
    </form>
  </div>
</template>

<script>
export default {
  name: 'LoginView',
  props: {
    authReady: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    error: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      form: {
        username: 'admin',
        password: 'admin123'
      }
    }
  }
}
</script>
