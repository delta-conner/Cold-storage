<template>
  <div class="page-card" v-if="storage">
    <div class="filter-row" style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <el-button type="text" icon="el-icon-arrow-left" @click="$router.push('/storages')">返回列表</el-button>
        <span style="font-size:18px;font-weight:600;margin-left:8px">{{ storage.name }} · 库区详情</span>
        <el-tag type="info" size="mini" style="margin-left:10px">办事台</el-tag>
      </div>
      <div>
        <el-button size="mini" @click="goRooms">管理冷藏间</el-button>
        <el-button size="mini" @click="goDevices">查看设备</el-button>
        <el-button size="mini" type="primary" @click="goOrders">去处理工单</el-button>
        <el-button size="mini" @click="goMaintain">维保提醒</el-button>
      </div>
    </div>
    <p class="hint">本页只列<strong>这座库当下要办的事</strong>；趋势、完成率、类型分布请到「数据统计」。</p>

    <el-row :gutter="16">
      <el-col :span="8">
        <h4>基础档案</h4>
        <p><b>地址：</b>{{ storage.address || '-' }}</p>
        <p><b>联系人：</b>{{ storage.contactName || '-' }} / {{ storage.contactPhone || '-' }}</p>
        <p><b>投用时间：</b>{{ storage.commissionDate || '-' }}</p>
        <p><b>状态：</b>{{ storage.status === 'ENABLED' ? '启用' : '停用' }}</p>
        <p><b>备注：</b>{{ storage.remark || '-' }}</p>

        <h4 style="margin-top:20px">下属冷藏间</h4>
        <div class="table-wrap">
          <table class="data-table" v-if="rooms.length">
            <thead>
              <tr><th>名称</th><th>编码</th><th>状态</th><th>温度范围</th></tr>
            </thead>
            <tbody>
              <tr v-for="r in rooms" :key="r.id">
                <td>{{ r.name }}</td>
                <td>{{ r.code }}</td>
                <td>{{ r.status === 'DISABLED' ? '停用' : '启用' }}</td>
                <td>{{ r.tempMin != null ? (r.tempMin + ' ~ ' + r.tempMax + '℃') : '-' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty-tip">暂无冷藏间</div>
        </div>
      </el-col>

      <el-col :span="16">
        <h4>
          待办 · 未完成工单
          <el-button type="text" size="mini" @click="goOrders">全部工单</el-button>
        </h4>
        <div class="table-wrap">
          <table class="data-table" v-if="(overview.openOrders || []).length">
            <thead><tr><th>工单号</th><th>设备</th><th>冷藏间</th><th>状态</th></tr></thead>
            <tbody>
              <tr v-for="o in overview.openOrders" :key="o.id" class="clickable" @click="goOrders">
                <td>{{ o.orderNo }}</td>
                <td>{{ o.deviceName }}</td>
                <td>{{ o.roomName || '-' }}</td>
                <td>{{ orderStatus(o.status) }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty-tip">暂无未完成工单</div>
        </div>

        <h4 style="margin-top:16px">
          待办 · 异常设备（故障/维修中）
          <el-button type="text" size="mini" @click="goDevices">设备台账</el-button>
        </h4>
        <div class="table-wrap">
          <table class="data-table" v-if="(overview.attentionDevices || []).length">
            <thead><tr><th>设备</th><th>编号</th><th>冷藏间</th><th>状态</th></tr></thead>
            <tbody>
              <tr v-for="d in overview.attentionDevices" :key="d.deviceId" class="clickable" @click="goDevices">
                <td>{{ d.deviceName }}</td>
                <td>{{ d.deviceNo || '-' }}</td>
                <td>{{ d.roomName }}</td>
                <td>{{ statusLabels[d.status] || d.status }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty-tip">当前无异常设备</div>
        </div>

        <h4 style="margin-top:16px">
          待办 · 临近维保（30天内）
          <el-button type="text" size="mini" @click="goMaintain">维保列表</el-button>
        </h4>
        <div class="table-wrap">
          <table class="data-table" v-if="(overview.maintainList || []).length">
            <thead><tr><th>设备</th><th>冷藏间</th><th>下次维保</th><th>剩余</th></tr></thead>
            <tbody>
              <tr
                v-for="m in overview.maintainList"
                :key="m.deviceId"
                :class="{ urgent: m.urgent, clickable: true }"
                @click="goMaintain"
              >
                <td>{{ m.deviceName }}</td>
                <td>{{ m.roomName || '-' }}</td>
                <td>{{ m.nextMaintainDate }}</td>
                <td :style="{color: m.urgent ? '#f56c6c' : '#606266'}">{{ m.daysLeft }}天</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty-tip">近期无临近维保</div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import http from '../../utils/http'

export default {
  data() {
    return {
      overview: {},
      statusLabels: {
        NORMAL: '正常', FAULT: '故障', REPAIRING: '维修中', ACCEPTING: '待验收', SCRAPPED: '报废'
      }
    }
  },
  computed: {
    storage() { return this.overview.storage || null },
    rooms() { return this.overview.rooms || [] }
  },
  created() { this.load() },
  watch: { '$route.params.id'() { this.load() } },
  methods: {
    orderStatus(s) {
      return ({ PENDING: '待受理', ASSIGNED: '已派单', PROCESSING: '处理中', ACCEPTING: '待验收', DONE: '已完成', EVALUATED: '已评价', ARCHIVED: '已归档' })[s] || s
    },
    async load() {
      const res = await http.get('/storages/' + this.$route.params.id + '/overview')
      this.overview = res.data || {}
    },
    goRooms() { this.$router.push({ path: '/rooms', query: { storageId: this.storage.id } }) },
    goDevices() { this.$router.push({ path: '/devices' }) },
    goOrders() { this.$router.push({ path: '/orders' }) },
    goMaintain() { this.$router.push({ path: '/maintain-tasks' }) }
  }
}
</script>

<style scoped>
.hint { color: #909399; font-size: 13px; margin: 0 0 12px; }
.urgent td { background: #fff1f0; }
.clickable { cursor: pointer; }
.clickable:hover td { background: #f0f9ff; }
.urgent.clickable:hover td { background: #ffe7e5; }
h4 { margin: 0 0 10px; color: #303133; display: flex; align-items: center; justify-content: space-between; }
</style>
