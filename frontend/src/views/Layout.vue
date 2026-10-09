<template>
  <el-container class="layout">
    <el-header class="layout-header" height="56px">
      <div class="layout-title">冷库设备运维工单管理系统</div>
      <div class="header-right">
        <el-badge :value="unread" :hidden="!unread" class="msg-badge">
          <el-button type="text" style="color:#fff" @click="$router.push('/messages')">
            <i class="el-icon-bell" /> 消息
          </el-button>
        </el-badge>
        <span style="margin:0 12px">{{ user.realName || user.username }}（{{ roleText }}）</span>
        <el-button type="text" style="color:#fff" @click="$router.push('/profile')">个人中心</el-button>
        <el-button type="text" style="color:#fff" @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="layout-side">
        <el-menu :default-active="$route.path" background-color="#112233" text-color="#cfd8dc" active-text-color="#4fc3f7" router>
          <el-menu-item v-if="user.role === 'OPS' || user.role === 'ADMIN'" index="/dashboard"><i class="el-icon-s-home" />运维工作台</el-menu-item>
          <el-menu-item index="/orders"><i class="el-icon-s-order" />运维工单</el-menu-item>
          <el-menu-item index="/storages"><i class="el-icon-house" />冷库管理</el-menu-item>
          <el-menu-item index="/devices"><i class="el-icon-cpu" />设备台账</el-menu-item>
          <el-menu-item v-if="user.role !== 'CLIENT'" index="/device-health"><i class="el-icon-data-line" />设备健康</el-menu-item>
          <el-menu-item index="/rooms"><i class="el-icon-office-building" />冷藏间</el-menu-item>
          <el-menu-item v-if="user.role === 'ADMIN'" index="/users"><i class="el-icon-user" />用户管理</el-menu-item>
          <el-menu-item v-if="user.role === 'ADMIN'" index="/logs"><i class="el-icon-document" />操作日志</el-menu-item>
          <el-menu-item v-if="user.role !== 'CLIENT'" index="/faults"><i class="el-icon-warning-outline" />故障记录</el-menu-item>
          <el-menu-item v-if="user.role !== 'CLIENT'" index="/fault-cases"><i class="el-icon-notebook-2" />故障案例库</el-menu-item>
          <el-menu-item v-if="user.role === 'ADMIN' || user.role === 'OPS'" index="/maintain-plans"><i class="el-icon-date" />维保计划</el-menu-item>
          <el-menu-item v-if="user.role !== 'CLIENT'" index="/maintain-tasks"><i class="el-icon-s-claim" />维保任务</el-menu-item>
          <el-menu-item v-if="user.role !== 'CLIENT'" index="/maintain"><i class="el-icon-bell" />周期提醒</el-menu-item>
          <el-menu-item v-if="user.role !== 'CLIENT'" index="/spare-parts"><i class="el-icon-box" />备件库存</el-menu-item>
          <el-menu-item v-if="user.role !== 'CLIENT'" index="/suppliers"><i class="el-icon-office-building" />供应商</el-menu-item>
          <el-menu-item v-if="user.role === 'ADMIN'" index="/stats"><i class="el-icon-data-analysis" />数据统计</el-menu-item>
          <el-menu-item index="/messages">
            <i class="el-icon-message" />消息中心
            <el-badge v-if="unread" :value="unread" class="side-badge" />
          </el-menu-item>
          <el-menu-item index="/profile"><i class="el-icon-setting" />个人中心</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { getUser, getToken, clearAuth, roleLabel } from '../utils/auth'
import http from '../utils/http'

export default {
  data() {
    return {
      user: getUser() || {},
      unread: 0,
      ws: null,
      pollTimer: null
    }
  },
  computed: {
    roleText() {
      return roleLabel(this.user.role)
    }
  },
  created() {
    this.refreshUser()
    this.refreshUnread()
    this.connectWs()
    this.pollTimer = setInterval(this.refreshUnread, 60000)
    window.addEventListener('focus', this.refreshUser)
    this.$root.$on('msg-unread-refresh', this.refreshUnread)
  },
  beforeDestroy() {
    window.removeEventListener('focus', this.refreshUser)
    this.$root.$off('msg-unread-refresh', this.refreshUnread)
    if (this.pollTimer) clearInterval(this.pollTimer)
    if (this.ws) {
      try { this.ws.close() } catch (e) { /* ignore */ }
    }
  },
  methods: {
    refreshUser() {
      this.user = getUser() || {}
    },
    async refreshUnread() {
      try {
        const res = await http.get('/messages/unread-count')
        this.unread = (res.data && res.data.count) || 0
      } catch (e) {
        // ignore
      }
    },
    connectWs() {
      const token = getToken()
      if (!token) return
      const proto = location.protocol === 'https:' ? 'wss' : 'ws'
      const url = proto + '://' + location.host + '/ws/messages?token=' + encodeURIComponent(token)
      try {
        this.ws = new WebSocket(url)
        this.ws.onmessage = (ev) => {
          try {
            const data = JSON.parse(ev.data)
            if (data && data.type === 'MESSAGE') {
              this.unread = (this.unread || 0) + 1
              this.$notify({
                title: data.title || '新消息',
                message: data.content || '',
                type: 'warning',
                duration: 4500
              })
            }
          } catch (e) { /* ignore */ }
        }
        this.ws.onclose = () => {
          setTimeout(() => {
            if (getToken()) this.connectWs()
          }, 8000)
        }
      } catch (e) {
        // WS 失败时依赖轮询
      }
    },
    async logout() {
      try {
        await http.post('/auth/logout')
      } catch (e) { /* ignore */ }
      if (this.ws) {
        try { this.ws.close() } catch (e2) { /* ignore */ }
      }
      clearAuth()
      this.$router.replace('/login')
    }
  }
}
</script>

<style scoped>
.header-right { display: flex; align-items: center; }
.msg-badge { margin-right: 4px; }
.side-badge { margin-left: 6px; }
</style>
