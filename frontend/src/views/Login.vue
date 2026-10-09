<template>
  <div class="login-wrap">
    <div class="login-box">
      <h2>冷库运维工单系统</h2>
      <div class="login-tip">广西龙门港冷库 · 演示初版</div>
      <el-form :model="form" @keyup.enter.native="onLogin">
        <el-form-item>
          <el-input v-model="form.username" prefix-icon="el-icon-user" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" prefix-icon="el-icon-lock" show-password placeholder="密码" />
        </el-form-item>
        <el-button type="primary" style="width:100%" :loading="loading" @click="onLogin">登 录</el-button>
      </el-form>
      <div class="login-tip" style="margin-top:16px;line-height:1.6">
        admin / ops01 / client01<br />密码均为 123456
      </div>
    </div>
  </div>
</template>

<script>
import http from '../utils/http'
import { setAuth } from '../utils/auth'

export default {
  data() {
    return {
      loading: false,
      form: { username: 'admin', password: '123456' }
    }
  },
  methods: {
    async onLogin() {
      this.loading = true
      try {
        const res = await http.post('/auth/login', this.form)
        setAuth(res.data)
        this.$message.success('登录成功')
        this.$router.replace('/orders')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>
