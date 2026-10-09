<template>
  <div class="page-card">
    <div class="filter-row" style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <el-tag type="success" size="mini">{{ data.scopeLabel || '全系统总结分析' }}</el-tag>
        <el-date-picker v-model="month" type="month" value-format="yyyy-MM" placeholder="选择月份" style="margin-left:12px" />
        <el-button type="primary" style="margin-left:8px" @click="load">刷新统计</el-button>
      </div>
    </div>
    <p class="hint">本页是<strong>全平台</strong>趋势与对比分析（跨全部冷库）；单座库当下待办请到「冷库管理 → 进入办事台」。</p>

    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="4"><div class="stat-card">工单总量<br><b>{{ data.orderTotal || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="stat-card">已完成<br><b>{{ data.orderDone || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="stat-card">未完成<br><b>{{ data.orderIncomplete || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="stat-card">完成率<br><b>{{ data.orderDoneRate || 0 }}%</b></div></el-col>
      <el-col :span="4"><div class="stat-card">平均响应(时)<br><b>{{ data.avgResponseHours || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="stat-card">平均维修(时)<br><b>{{ data.avgRepairHours || 0 }}</b></div></el-col>
    </el-row>
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="4"><div class="stat-card soft">待受理<br><b>{{ data.orderPending || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="stat-card soft">处理中<br><b>{{ data.orderProcessing || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="stat-card soft">临近维保<br><b>{{ data.maintainNearCount || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="stat-card soft">紧急维保<br><b style="color:#f56c6c">{{ data.maintainUrgentCount || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="stat-card soft">库存不足<br><b style="color:#e6a23c">{{ data.sparePartLowCount || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="stat-card soft">维保待办<br><b>{{ data.maintainTaskPending || 0 }}</b></div></el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12"><div ref="bar" class="chart-box"></div></el-col>
      <el-col :span="12"><div ref="pie" class="chart-box"></div></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12"><div ref="compare" class="chart-box"></div></el-col>
      <el-col :span="12"><div ref="roomRank" class="chart-box"></div></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12"><div ref="stockPie" class="chart-box"></div></el-col>
      <el-col :span="12"><div ref="maintainPie" class="chart-box"></div></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12"><div ref="lowStock" class="chart-box"></div></el-col>
      <el-col :span="12"><div ref="rank" class="chart-box"></div></el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import http from '../../utils/http'

export default {
  data() {
    return { month: '', data: {}, charts: [] }
  },
  mounted() {
    const now = new Date()
    this.month = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0')
    this.load()
    window.addEventListener('resize', this.resize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resize)
    this.charts.forEach(c => c.dispose())
  },
  methods: {
    resize() { this.charts.forEach(c => c.resize()) },
    async load() {
      const res = await http.get('/stats/admin', { params: { month: this.month } })
      this.data = res.data || {}
      this.$nextTick(() => this.renderCharts())
    },
    renderCharts() {
      this.charts.forEach(c => c.dispose())
      this.charts = []
      const bar = echarts.init(this.$refs.bar)
      bar.setOption({
        title: { text: '全系统近6个月：新建 vs 完成', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'axis' },
        legend: { bottom: 0, data: ['新建工单', '完成工单'] },
        xAxis: { type: 'category', data: this.data.monthLabels || [] },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          { name: '新建工单', type: 'bar', data: this.data.monthCreatedCounts || [], itemStyle: { color: '#909399' } },
          { name: '完成工单', type: 'bar', data: this.data.monthDoneCounts || [], itemStyle: { color: '#1a6b7c' } }
        ]
      })
      const pie = echarts.init(this.$refs.pie)
      pie.setOption({
        title: { text: '全系统故障类型分布', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'item' },
        series: [{ type: 'pie', radius: '55%', data: this.data.faultTypePie || [], label: { formatter: '{b}: {c}' } }]
      })
      const compare = echarts.init(this.$refs.compare)
      const sc = this.data.storageCompare || []
      compare.setOption({
        title: { text: '各冷库对比（设备/未完成工单/故障）', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'axis' },
        legend: { bottom: 0 },
        xAxis: { type: 'category', data: sc.map(i => i.storageName) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          { name: '设备数', type: 'bar', data: sc.map(i => i.deviceCount), itemStyle: { color: '#409EFF' } },
          { name: '未完成工单', type: 'bar', data: sc.map(i => i.openOrderCount), itemStyle: { color: '#E6A23C' } },
          { name: '故障次数', type: 'bar', data: sc.map(i => i.faultCount), itemStyle: { color: '#F56C6C' } }
        ]
      })
      const roomRank = echarts.init(this.$refs.roomRank)
      const rr = this.data.roomFaultRank || []
      roomRank.setOption({
        title: { text: '全系统冷藏间故障排名', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'axis' },
        grid: { left: 100 },
        xAxis: { type: 'value', minInterval: 1 },
        yAxis: { type: 'category', data: rr.map(i => i.roomName).reverse() },
        series: [{ type: 'bar', data: rr.map(i => i.count).reverse(), itemStyle: { color: '#67C23A' } }]
      })
      const stockPie = echarts.init(this.$refs.stockPie)
      stockPie.setOption({
        title: { text: '备件库存按类型分布', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'item' },
        series: [{ type: 'pie', radius: ['35%', '60%'], data: this.data.stockTypePie || [], label: { formatter: '{b}: {c}' } }]
      })
      const maintainPie = echarts.init(this.$refs.maintainPie)
      maintainPie.setOption({
        title: { text: '维保任务完成情况', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie',
          radius: '55%',
          data: this.data.maintainTaskPie || [],
          label: { formatter: '{b}: {c}' },
          itemStyle: { color: (p) => (p.name === '已完成' ? '#67C23A' : '#E6A23C') }
        }]
      })
      const lowStock = echarts.init(this.$refs.lowStock)
      const ls = this.data.lowStockRank || []
      lowStock.setOption({
        title: { text: '库存不足备件（当前/安全）', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'axis' },
        legend: { bottom: 0 },
        grid: { left: 100 },
        xAxis: { type: 'value', minInterval: 1 },
        yAxis: { type: 'category', data: ls.map(i => i.partName).reverse() },
        series: [
          { name: '当前库存', type: 'bar', data: ls.map(i => i.stockQty).reverse(), itemStyle: { color: '#F56C6C' } },
          { name: '安全库存', type: 'bar', data: ls.map(i => i.safetyStock).reverse(), itemStyle: { color: '#909399' } }
        ]
      })
      const rank = echarts.init(this.$refs.rank)
      const rankData = this.data.deviceFaultRank || []
      rank.setOption({
        title: { text: '全系统设备故障频次排名', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'axis' },
        grid: { left: 140 },
        xAxis: { type: 'value', minInterval: 1 },
        yAxis: { type: 'category', data: rankData.map(i => i.deviceName).reverse() },
        series: [{ type: 'bar', data: rankData.map(i => i.count).reverse(), itemStyle: { color: '#e6a23c' } }]
      })
      this.charts = [bar, pie, compare, roomRank, stockPie, maintainPie, lowStock, rank]
    }
  }
}
</script>

<style scoped>
.hint { color: #909399; font-size: 13px; margin: 0 0 12px; }
.stat-card {
  background: #f5f7fa; border-radius: 6px; padding: 14px; text-align: center; color: #606266;
}
.stat-card.soft { background: #fafafa; }
.stat-card b { font-size: 22px; color: #0b3a4a; }
.chart-box { height: 320px; background: #fff; border: 1px solid #ebeef5; border-radius: 4px; }
</style>
