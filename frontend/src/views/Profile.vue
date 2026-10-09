<template>
  <div class="page-card">
    <h3 style="margin-top:0">个人中心</h3>
    <el-form label-width="90px" style="max-width:480px">
      <el-form-item label="用户名"><el-input :value="form.username" disabled /></el-form-item>
      <el-form-item label="角色"><el-input :value="roleText" disabled /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
      <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="新密码"><el-input v-model="form.password" show-password placeholder="不修改请留空" /></el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存</el-button>
      </el-form-item>
    </el-form>

    <div v-if="isClient" style="margin-top:20px">
      <h4>已绑定冷藏间</h4>
      <el-tag v-for="r in summary.rooms || []" :key="r.id" style="margin-right:8px;margin-bottom:8px">{{ r.name }}</el-tag>
      <span v-if="!(summary.rooms || []).length" style="color:#999">暂无绑定</span>

      <h4 style="margin-top:20px">报修工单汇总</h4>
      <el-row :gutter="12">
        <el-col :span="6"><div class="mini-stat">全部<br><b>{{ summary.orderTotal || 0 }}</b></div></el-col>
        <el-col :span="6"><div class="mini-stat">待受理<br><b>{{ summary.orderPending || 0 }}</b></div></el-col>
        <el-col :span="6"><div class="mini-stat">处理中<br><b>{{ summary.orderProcessing || 0 }}</b></div></el-col>
        <el-col :span="6"><div class="mini-stat">已完成<br><b>{{ summary.orderDone || 0 }}</b></div></el-col>
      </el-row>
      <p style="margin-top:12px">工单完成率：<b style="color:#0b3a4a">{{ summary.completionRate || 0 }}%</b></p>
    </div>
  </div>
</template>

<script>
import http from '../utils/http'
import { getUser, setUser, roleLabel } from '../utils/auth'

export default {
  data() {
    return {
      form: { username: '', realName: '', phone: '', password: '', role: '' },
      summary: {}
    }
  },
  computed: {
    isClient() { return this.form.role === 'CLIENT' },
    roleText() { return roleLabel(this.form.role) }
  },
  created() { this.load() },
  methods: {
    async load() {
      const res = await http.get('/auth/profile')
      this.form = { ...res.data, password: '' }
      if (this.isClient) {
        const s = await http.get('/users/client-summary')
        this.summary = s.data || {}
      }
    },
    async save() {
      await http.put('/auth/profile', {
        realName: this.form.realName,
        phone: this.form.phone,
        password: this.form.password || null
      })
      const user = getUser()
      if (user) {
        user.realName = this.form.realName
        user.phone = this.form.phone
        setUser(user)
      }
      this.$message.success('已保存')
      this.form.password = ''
    }
  }
}
</script>

<style scoped>
.mini-stat {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px;
  text-align: center;
  color: #606266;
}
.mini-stat b { font-size: 20px; color: #0b3a4a; }
</style>
