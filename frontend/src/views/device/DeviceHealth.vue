<template>
  <div class="page-card">
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="6"><div class="mini">参与分析设备<br><b>{{ summary.total || 0 }}</b></div></el-col>
      <el-col :span="6"><div class="mini">正常<br><b style="color:#67c23a">{{ summary.normalCount || 0 }}</b></div></el-col>
      <el-col :span="6"><div class="mini">关注<br><b style="color:#e6a23c">{{ summary.watchCount || 0 }}</b></div></el-col>
      <el-col :span="6"><div class="mini">预警<br><b style="color:#f56c6c">{{ summary.warnCount || 0 }}</b></div></el-col>
    </el-row>
    <p class="hint">规则（非 AI）：近一年故障 ≤2 正常；3～5 关注；&gt;5 预警。报废设备不参与。</p>

    <div class="filter-row">
      <el-input v-model="query.deviceName" placeholder="设备名称" clearable style="width:160px;margin-right:8px" />
      <el-select v-model="query.roomId" clearable placeholder="冷藏间" style="width:150px;margin-right:8px">
        <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
      </el-select>
      <el-select v-model="query.healthLevel" clearable placeholder="健康等级" style="width:130px;margin-right:8px">
        <el-option label="正常" value="NORMAL" />
        <el-option label="关注" value="WATCH" />
        <el-option label="预警" value="WARN" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>设备</th>
            <th>类型</th>
            <th>冷藏间</th>
            <th>健康等级</th>
            <th>近一年故障</th>
            <th>维修次数</th>
            <th>维保次数</th>
            <th>平均维修(分)</th>
            <th>平均故障间隔(天)</th>
            <th>最近故障</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.deviceId" :class="rowClass(row.healthLevel)">
            <td>{{ row.deviceName }}（{{ row.deviceNo }}）</td>
            <td>{{ row.deviceType }}</td>
            <td>{{ row.roomName || '-' }}</td>
            <td><el-tag size="mini" :type="tagType(row.healthLevel)">{{ row.healthLabel }}</el-tag></td>
            <td>{{ row.yearFaultCount }}</td>
            <td>{{ row.repairCount }}</td>
            <td>{{ row.maintainCount }}</td>
            <td>{{ row.avgRepairMinutes || 0 }}</td>
            <td>{{ row.avgFaultIntervalDays || 0 }}</td>
            <td>{{ row.lastFaultTime || '-' }}</td>
            <td class="ops">
              <el-button type="text" @click="openArchive(row.deviceId)">维修档案</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无数据</div>
    </div>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
                   :page-size="query.size" :current-page.sync="query.page" @current-change="load" />

    <el-dialog title="设备维修档案" :visible.sync="archiveVisible" width="860px" top="5vh">
      <div v-if="archive">
        <el-row :gutter="12" style="margin-bottom:12px">
          <el-col :span="12">
            <p><b>设备：</b>{{ archive.device.deviceName }}（{{ archive.device.deviceNo }}）</p>
            <p><b>位置：</b>{{ archive.device.storageName }} / {{ archive.device.roomName }}</p>
            <p><b>当前状态：</b>{{ statusText(archive.device.status) }}</p>
          </el-col>
          <el-col :span="12">
            <p><b>健康：</b>
              <el-tag size="mini" :type="tagType(archive.summary.healthLevel)">{{ archive.summary.healthLabel }}</el-tag>
              <span class="hint" style="margin-left:8px">{{ archive.summary.healthRule }}</span>
            </p>
            <p><b>近一年故障 / 累计故障：</b>{{ archive.summary.yearFaultCount }} / {{ archive.summary.faultCountTotal }}</p>
            <p><b>维修 / 维保 / 备件件数：</b>{{ archive.summary.repairCount }} / {{ archive.summary.maintainCount }} / {{ archive.summary.partsUsedCount }}</p>
            <p><b>平均维修时长：</b>{{ archive.summary.avgRepairMinutes || 0 }} 分钟 · <b>平均故障间隔：</b>{{ archive.summary.avgFaultIntervalDays || 0 }} 天</p>
          </el-col>
        </el-row>

        <el-tabs>
          <el-tab-pane label="故障工单">
            <table class="data-table" v-if="(archive.orders || []).length">
              <thead><tr><th>工单号</th><th>状态</th><th>描述</th><th>提交时间</th><th>完工时间</th></tr></thead>
              <tbody>
                <tr v-for="o in archive.orders" :key="o.id">
                  <td>{{ o.orderNo }}</td>
                  <td>{{ orderStatus(o.status) }}</td>
                  <td>{{ o.faultDesc }}</td>
                  <td>{{ o.createTime }}</td>
                  <td>{{ o.finishTime || '-' }}</td>
                </tr>
              </tbody>
            </table>
            <div v-else class="empty-tip">无关联工单</div>
          </el-tab-pane>
          <el-tab-pane label="故障记录">
            <table class="data-table" v-if="(archive.faults || []).length">
              <thead><tr><th>编号</th><th>类型</th><th>等级</th><th>现象</th><th>耗时</th><th>时间</th></tr></thead>
              <tbody>
                <tr v-for="f in archive.faults" :key="f.id">
                  <td>{{ f.recordNo }}</td>
                  <td>{{ f.faultType }}</td>
                  <td>{{ f.faultLevel || '-' }}</td>
                  <td>{{ f.faultPhenomenon || '-' }}</td>
                  <td>{{ f.durationMinutes != null ? f.durationMinutes : '-' }}</td>
                  <td>{{ f.handleTime || '-' }}</td>
                </tr>
              </tbody>
            </table>
            <div v-else class="empty-tip">无故障记录</div>
          </el-tab-pane>
          <el-tab-pane label="维保任务">
            <table class="data-table" v-if="(archive.maintainTasks || []).length">
              <thead><tr><th>任务号</th><th>状态</th><th>到期</th><th>完成时间</th><th>摘要</th></tr></thead>
              <tbody>
                <tr v-for="t in archive.maintainTasks" :key="t.id">
                  <td>{{ t.taskNo }}</td>
                  <td>{{ t.status === 'DONE' ? '已完成' : '未完成' }}</td>
                  <td>{{ t.dueDate }}</td>
                  <td>{{ t.finishTime || '-' }}</td>
                  <td>{{ t.resultSummary || '-' }}</td>
                </tr>
              </tbody>
            </table>
            <div v-else class="empty-tip">无维保任务</div>
          </el-tab-pane>
          <el-tab-pane label="备件消耗">
            <table class="data-table" v-if="(archive.parts || []).length">
              <thead><tr><th>工单</th><th>备件</th><th>数量</th><th>时间</th></tr></thead>
              <tbody>
                <tr v-for="(p,i) in archive.parts" :key="i">
                  <td>{{ p.orderNo || '-' }}</td>
                  <td>{{ p.partName }}（{{ p.partNo }}）</td>
                  <td>{{ p.quantity }} {{ p.unit || '' }}</td>
                  <td>{{ p.createTime || '-' }}</td>
                </tr>
              </tbody>
            </table>
            <div v-else class="empty-tip">无备件消耗</div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import http from '../../utils/http'
import { statusLabel } from '../../utils/auth'

export default {
  data() {
    return {
      query: { page: 1, size: 10, deviceName: '', roomId: null, healthLevel: '' },
      list: [],
      total: 0,
      summary: {},
      rooms: [],
      archiveVisible: false,
      archive: null
    }
  },
  created() { this.init() },
  methods: {
    statusText(s) { return ({ NORMAL: '正常', FAULT: '故障', REPAIRING: '维修中', ACCEPTING: '待验收', SCRAPPED: '报废' })[s] || s },
    orderStatus(s) { return statusLabel(s) },
    tagType(lv) {
      return ({ NORMAL: 'success', WATCH: 'warning', WARN: 'danger' })[lv] || 'info'
    },
    rowClass(lv) {
      return ({ WARN: 'warn-row', WATCH: 'watch-row' })[lv] || ''
    },
    async init() {
      const rooms = await http.get('/rooms/list')
      this.rooms = rooms.data || []
      this.loadSummary()
      this.load()
    },
    async loadSummary() {
      const res = await http.get('/devices/health/summary')
      this.summary = res.data || {}
    },
    async load() {
      const res = await http.get('/devices/health/page', { params: this.query })
      this.list = (res.data && res.data.records) || []
      this.total = (res.data && res.data.total) || 0
    },
    async openArchive(deviceId) {
      const res = await http.get('/devices/' + deviceId + '/archive')
      this.archive = res.data
      this.archiveVisible = true
    }
  }
}
</script>

<style scoped>
.hint { color: #909399; font-size: 12px; margin: 0 0 10px; }
.mini { background:#f5f7fa; border-radius:6px; padding:10px; text-align:center; color:#606266; }
.mini b { font-size:18px; color:#0b3a4a; }
.warn-row td { background: #fff1f0; }
.watch-row td { background: #fdf6ec; }
</style>
