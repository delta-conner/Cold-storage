<template>
  <div class="page-card">
    <p class="hint">按设备「投用日期 + 维保周期」推算的临近提醒；正式维保请走「维保计划 → 生成任务」。</p>
    <div class="filter-row">
      <span style="margin-right:8px">临近天数</span>
      <el-input-number v-model="daysAhead" :min="7" :max="180" />
      <el-button type="primary" style="margin-left:8px" @click="load">刷新</el-button>
      <el-button style="margin-left:8px" @click="$router.push('/maintain-tasks')">去维保任务</el-button>
    </div>
    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>设备编号</th>
            <th>设备名称</th>
            <th>类型</th>
            <th>冷藏间</th>
            <th>投用日期</th>
            <th>维保周期(天)</th>
            <th>下次维保</th>
            <th>剩余天数</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.deviceId" :class="{ urgent: row.urgent }">
            <td>{{ row.deviceNo }}</td>
            <td>{{ row.deviceName }}</td>
            <td>{{ row.deviceType }}</td>
            <td>{{ row.roomName }}</td>
            <td>{{ row.installDate }}</td>
            <td>{{ row.maintainCycleDays }}</td>
            <td>{{ row.nextMaintainDate }}</td>
            <td :style="{color: row.urgent ? '#f56c6c' : '#606266', fontWeight: row.urgent ? 700 : 400}">
              {{ row.daysLeft }} 天
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">近期无临近维保设备</div>
    </div>
  </div>
</template>

<script>
import http from '../../utils/http'

export default {
  data() {
    return { daysAhead: 30, list: [] }
  },
  created() { this.load() },
  methods: {
    async load() {
      const res = await http.get('/stats/maintain', { params: { daysAhead: this.daysAhead } })
      this.list = res.data || []
    }
  }
}
</script>

<style scoped>
.hint { color: #909399; font-size: 12px; margin: 0 0 10px; }
.urgent td { background: #fff1f0; }
</style>
