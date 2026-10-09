<template>
  <div class="page-card">
    <div class="filter-row" style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <el-select v-model="query.isRead" clearable placeholder="已读状态" style="width:120px;margin-right:8px" @change="load">
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
        <el-button type="primary" @click="load">刷新</el-button>
        <el-button @click="readAll">全部已读</el-button>
      </div>
      <el-tag type="warning" size="small">未读 {{ unread }}</el-tag>
    </div>
    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th style="width:70px">状态</th>
            <th style="width:160px">标题</th>
            <th>内容</th>
            <th style="width:120px">类型</th>
            <th style="width:160px">时间</th>
            <th style="width:100px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id" :class="{ unread: row.isRead === 0 }">
            <td>
              <el-tag :type="row.isRead === 0 ? 'danger' : 'info'" size="mini">{{ row.isRead === 0 ? '未读' : '已读' }}</el-tag>
            </td>
            <td>{{ row.title }}</td>
            <td>{{ row.content }}</td>
            <td>{{ typeText(row.msgType) }}</td>
            <td>{{ row.createTime }}</td>
            <td>
              <el-button v-if="row.isRead === 0" type="text" @click="markRead(row)">标为已读</el-button>
              <span v-else style="color:#c0c4cc">—</span>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty">暂无消息</div>
    </div>
    <el-pagination
      style="margin-top:12px;text-align:right"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page.sync="query.page"
      @current-change="load"
    />
  </div>
</template>

<script>
import http from '../../utils/http'

export default {
  data() {
    return {
      list: [],
      total: 0,
      unread: 0,
      query: { page: 1, size: 10, isRead: null }
    }
  },
  created() {
    this.load()
    this.loadUnread()
  },
  methods: {
    typeText(t) {
      return ({
        ORDER_NEW: '新报修',
        ORDER_ASSIGN: '工单派单',
        ORDER_ACCEPTING: '待验收',
        ORDER_DONE: '工单完成',
        STOCK_LOW: '库存预警',
        MAINTAIN_ASSIGN: '维保指派',
        MAINTAIN_DUE: '维保到期'
      })[t] || t || '-'
    },
    async load() {
      const params = { page: this.query.page, size: this.query.size }
      if (this.query.isRead !== null && this.query.isRead !== '') {
        params.isRead = this.query.isRead
      }
      const res = await http.get('/messages/page', { params })
      const page = res.data || {}
      this.list = page.records || []
      this.total = page.total || 0
      this.loadUnread()
    },
    async loadUnread() {
      const res = await http.get('/messages/unread-count')
      this.unread = (res.data && res.data.count) || 0
    },
    async markRead(row) {
      await http.post('/messages/' + row.id + '/read')
      this.$message.success('已标为已读')
      this.load()
      this.$root.$emit('msg-unread-refresh')
    },
    async readAll() {
      await http.post('/messages/read-all')
      this.$message.success('全部已读')
      this.load()
      this.$root.$emit('msg-unread-refresh')
    }
  }
}
</script>

<style scoped>
.unread td { font-weight: 600; }
.empty { padding: 40px; text-align: center; color: #909399; }
</style>
