<template>
  <div class="page-card">
    <h3 style="margin-top:0">运维工作台</h3>
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="8"><div class="stat-card">待处理工单<br><b>{{ data.processingCount || 0 }}</b></div></el-col>
      <el-col :span="8"><div class="stat-card">本月完成<br><b>{{ data.monthDoneCount || 0 }}</b></div></el-col>
      <el-col :span="8"><div class="stat-card">临近维保设备<br><b>{{ (data.maintainList || []).length }}</b></div></el-col>
    </el-row>
    <h4>负责范围维保提醒（30天内）</h4>
    <div class="table-wrap">
      <table class="data-table" v-if="(data.maintainList || []).length">
        <thead>
          <tr>
            <th>设备</th>
            <th>冷藏间</th>
            <th>下次维保</th>
            <th>剩余天数</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in data.maintainList" :key="row.deviceId" :class="{ urgent: row.urgent }">
            <td>{{ row.deviceName }}</td>
            <td>{{ row.roomName }}</td>
            <td>{{ row.nextMaintainDate }}</td>
            <td :style="{color: row.urgent ? '#f56c6c' : '#606266'}">{{ row.daysLeft }} 天</td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无临近维保</div>
    </div>
    <el-button type="primary" style="margin-top:12px" @click="$router.push('/orders')">去处理工单</el-button>
  </div>
</template>

<script>
import http from '../../utils/http'

export default {
  data() {
    return { data: {} }
  },
  created() { this.load() },
  methods: {
    async load() {
      const res = await http.get('/stats/ops-dashboard')
      this.data = res.data || {}
    }
  }
}
</script>

<style scoped>
.stat-card {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 16px;
  text-align: center;
  color: #606266;
}
.stat-card b { font-size: 24px; color: #0b3a4a; }
.urgent td { background: #fff1f0; }
</style>
