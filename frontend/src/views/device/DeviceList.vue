<template>
  <div class="page-card">
    <div class="filter-row">
      <el-input v-model="query.deviceNo" placeholder="设备编号" clearable style="width:140px;margin-right:8px" />
      <el-input v-model="query.deviceName" placeholder="设备名称" clearable style="width:140px;margin-right:8px" />
      <el-select v-model="query.roomId" clearable placeholder="冷藏间" style="width:150px;margin-right:8px">
        <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
      </el-select>
      <el-select v-model="query.deviceType" clearable placeholder="设备类型" style="width:170px;margin-right:8px">
        <el-option v-for="t in types" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="query.status" clearable placeholder="设备状态" style="width:130px;margin-right:8px">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="canEdit" type="success" @click="openEdit()">新增设备</el-button>
      <el-button v-if="canEdit" @click="exportExcel">导出Excel</el-button>
      <el-upload
        v-if="canEdit"
        style="display:inline-block;margin-left:8px"
        action="/api/devices/import"
        :headers="uploadHeaders"
        :show-file-list="false"
        accept=".xlsx,.xls"
        :on-success="onImportOk"
        :on-error="onImportErr"
      >
        <el-button type="warning">导入Excel</el-button>
      </el-upload>
    </div>
    <p class="hint">生命周期：正常 → 故障 → 维修中 → 待验收 → 正常；可报废。工单流转会自动写状态历史。</p>
    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>设备编号</th>
            <th>设备名称</th>
            <th>类型</th>
            <th>品牌/型号</th>
            <th>冷藏间</th>
            <th>冷库</th>
            <th>公共</th>
            <th>状态</th>
            <th>投用日期</th>
            <th v-if="canEdit">维保周期(天)</th>
            <th>故障次数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.deviceNo }}</td>
            <td>{{ row.deviceName }}</td>
            <td>{{ row.deviceType }}</td>
            <td>{{ (row.brand || '-') + ' / ' + (row.model || '-') }}</td>
            <td>{{ row.roomName }}</td>
            <td>{{ row.storageName }}</td>
            <td>{{ row.isPublic === 1 ? '是' : '否' }}</td>
            <td>{{ statusText(row.status) }}</td>
            <td>{{ row.installDate }}</td>
            <td v-if="canEdit">{{ row.maintainCycleDays }}</td>
            <td>{{ row.faultCount || 0 }}</td>
            <td class="ops">
              <el-button type="text" @click="showDetail(row)">详情/生命周期</el-button>
              <el-button type="text" @click="openArchive(row)">维修档案</el-button>
              <el-button v-if="canEdit" type="text" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="canEdit" type="text" style="color:#f56c6c" @click="remove(row)">删除</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无设备数据</div>
    </div>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total" :page-size="query.size" :current-page.sync="query.page" @current-change="load" />

    <el-dialog :title="form.id ? '编辑设备' : '新增设备'" :visible.sync="visible" width="560px">
      <el-form label-width="100px">
        <el-form-item label="设备编号"><el-input v-model="form.deviceNo" /></el-form-item>
        <el-form-item label="设备名称"><el-input v-model="form.deviceName" /></el-form-item>
        <el-form-item label="设备类型">
          <el-select v-model="form.deviceType" style="width:100%">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="冷藏间">
          <el-select v-model="form.roomId" style="width:100%">
            <el-option v-for="r in allRooms" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="公共设备">
          <el-switch v-model="form.isPublic" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="设备状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <div class="form-tip">状态变更受生命周期规则约束；日常流转优先走工单。</div>
        </el-form-item>
        <el-form-item label="投用日期">
          <el-date-picker v-model="form.installDate" type="date" value-format="yyyy-MM-dd" style="width:100%" />
        </el-form-item>
        <el-form-item label="维保周期">
          <el-input-number v-model="form.maintainCycleDays" :min="1" />
        </el-form-item>
        <el-form-item label="额定参数"><el-input v-model="form.ratedParams" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog title="设备详情 · 生命周期" :visible.sync="detailVisible" width="680px">
      <div v-if="detail">
        <el-row :gutter="16">
          <el-col :span="12">
            <p><b>编号：</b>{{ detail.deviceNo }}</p>
            <p><b>名称：</b>{{ detail.deviceName }}</p>
            <p><b>类型：</b>{{ detail.deviceType }}</p>
            <p><b>品牌/型号：</b>{{ (detail.brand || '-') + ' / ' + (detail.model || '-') }}</p>
            <p><b>冷藏间：</b>{{ detail.roomName }}</p>
            <p><b>冷库：</b>{{ detail.storageName }}</p>
          </el-col>
          <el-col :span="12">
            <p><b>公共设备：</b>{{ detail.isPublic === 1 ? '是' : '否' }}</p>
            <p><b>当前状态：</b><el-tag size="mini">{{ statusText(detail.status) }}</el-tag></p>
            <p><b>投用日期：</b>{{ detail.installDate || '-' }}</p>
            <p><b>维保周期：</b>{{ detail.maintainCycleDays || '-' }} 天</p>
            <p><b>故障次数：</b>{{ detail.faultCount || 0 }}</p>
            <p v-if="detail.remark"><b>备注：</b>{{ detail.remark }}</p>
          </el-col>
        </el-row>
        <div v-if="isAdmin" style="margin:8px 0 12px">
          <el-button v-if="detail.status !== 'SCRAPPED'" size="mini" type="danger" plain @click="scrapDevice">报废</el-button>
          <el-button v-if="detail.status === 'SCRAPPED'" size="mini" type="warning" plain @click="restoreDevice">恢复为正常</el-button>
        </div>
        <h4 style="margin:12px 0 8px">状态变更时间线</h4>
        <el-timeline v-if="statusLogs.length">
          <el-timeline-item
            v-for="log in statusLogs"
            :key="log.id"
            :timestamp="log.createTime"
            placement="top"
          >
            <div>
              <b>{{ statusText(log.fromStatus) || '—' }}</b>
              →
              <b>{{ statusText(log.toStatus) }}</b>
              <el-tag size="mini" style="margin-left:8px">{{ sourceText(log.source) }}</el-tag>
            </div>
            <div class="log-meta">{{ log.reason || '-' }} · {{ log.operatorName || '-' }}</div>
          </el-timeline-item>
        </el-timeline>
        <div v-else class="empty-tip">暂无状态历史</div>
      </div>
    </el-dialog>

    <el-dialog title="维修档案摘要" :visible.sync="archiveVisible" width="640px">
      <div v-if="archiveSummary">
        <p>
          <b>健康：</b>
          <el-tag size="mini" :type="healthTag(archiveSummary.healthLevel)">{{ archiveSummary.healthLabel }}</el-tag>
        </p>
        <p><b>近一年故障：</b>{{ archiveSummary.yearFaultCount }} · <b>累计故障：</b>{{ archiveSummary.faultCountTotal }}</p>
        <p><b>维修次数：</b>{{ archiveSummary.repairCount }} · <b>维保次数：</b>{{ archiveSummary.maintainCount }} · <b>备件件数：</b>{{ archiveSummary.partsUsedCount }}</p>
        <p><b>平均维修：</b>{{ archiveSummary.avgRepairMinutes || 0 }} 分钟 · <b>平均故障间隔：</b>{{ archiveSummary.avgFaultIntervalDays || 0 }} 天</p>
        <p><b>最近故障：</b>{{ archiveSummary.lastFaultTime || '-' }}</p>
        <p class="hint">{{ archiveSummary.healthRule }}</p>
        <el-button type="primary" size="mini" @click="$router.push('/device-health')">打开健康分析全页</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import http from '../../utils/http'
import { getUser, getToken } from '../../utils/auth'

export default {
  data() {
    return {
      query: { page: 1, size: 10, deviceNo: '', deviceName: '', roomId: null, deviceType: '', status: '' },
      list: [],
      total: 0,
      rooms: [],
      allRooms: [],
      types: [],
      statusOptions: [
        { label: '正常', value: 'NORMAL' },
        { label: '故障', value: 'FAULT' },
        { label: '维修中', value: 'REPAIRING' },
        { label: '待验收', value: 'ACCEPTING' },
        { label: '报废', value: 'SCRAPPED' }
      ],
      visible: false,
      form: {},
      detailVisible: false,
      detail: null,
      statusLogs: [],
      archiveVisible: false,
      archiveSummary: null
    }
  },
  computed: {
    user() { return getUser() || {} },
    canEdit() {
      return this.user.role === 'ADMIN' || this.user.role === 'OPS'
    },
    isAdmin() { return this.user.role === 'ADMIN' },
    uploadHeaders() {
      return { Authorization: 'Bearer ' + (getToken() || '') }
    }
  },
  created() {
    this.init()
  },
  methods: {
    statusText(s) {
      return ({ NORMAL: '正常', FAULT: '故障', REPAIRING: '维修中', ACCEPTING: '待验收', SCRAPPED: '报废' })[s] || s || '正常'
    },
    sourceText(s) {
      return ({ WORK_ORDER: '工单联动', MANUAL: '手工', SYSTEM: '系统' })[s] || s || '-'
    },
    async init() {
      const [rooms, types] = await Promise.all([
        http.get('/rooms/list'),
        http.get('/devices/types')
      ])
      this.rooms = rooms.data || []
      this.allRooms = rooms.data || []
      this.types = types.data || []
      this.load()
    },
    async load() {
      const res = await http.get('/devices/page', { params: this.query })
      this.list = res.data.records || []
      this.total = res.data.total || 0
    },
    openEdit(row) {
      this.form = row ? { ...row } : {
        deviceNo: '', deviceName: '', deviceType: this.types[0], brand: '', model: '',
        roomId: null, isPublic: 0, status: 'NORMAL', maintainCycleDays: 90, ratedParams: '', remark: ''
      }
      this.visible = true
    },
    async save() {
      await http.post('/devices', this.form)
      this.$message.success('保存成功')
      this.visible = false
      this.load()
    },
    async showDetail(row) {
      const [detailRes, logRes] = await Promise.all([
        http.get('/devices/' + row.id),
        http.get('/devices/' + row.id + '/status-logs')
      ])
      this.detail = detailRes.data
      this.statusLogs = logRes.data || []
      this.detailVisible = true
    },
    healthTag(lv) {
      return ({ NORMAL: 'success', WATCH: 'warning', WARN: 'danger' })[lv] || 'info'
    },
    async openArchive(row) {
      const res = await http.get('/devices/' + row.id + '/archive')
      this.archiveSummary = (res.data && res.data.summary) || null
      this.archiveVisible = true
    },
    async scrapDevice() {
      const { value } = await this.$prompt('请输入报废原因', '设备报废', { inputPlaceholder: '报废原因' })
      await http.post('/devices/' + this.detail.id + '/status', { status: 'SCRAPPED', reason: value })
      this.$message.success('已报废')
      this.showDetail(this.detail)
      this.load()
    },
    async restoreDevice() {
      await this.$confirm('确认将设备恢复为正常？', '提示')
      await http.post('/devices/' + this.detail.id + '/status', { status: 'NORMAL', reason: '报废恢复' })
      this.$message.success('已恢复')
      this.showDetail(this.detail)
      this.load()
    },
    remove(row) {
      this.$confirm('确认删除该设备？', '提示').then(async () => {
        await http.delete('/devices/' + row.id)
        this.$message.success('已删除')
        this.load()
      }).catch(() => {})
    },
    async exportExcel() {
      try {
        const res = await fetch('/api/devices/export', {
          headers: { Authorization: 'Bearer ' + (getToken() || '') }
        })
        if (!res.ok) {
          this.$message.error('导出失败')
          return
        }
        const blob = await res.blob()
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = '设备台账.xlsx'
        a.click()
        URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败')
      }
    },
    onImportOk(res) {
      if (!res || res.code !== 0) {
        this.$message.error((res && res.message) || '导入失败')
        return
      }
      const d = res.data || {}
      this.$message.success('导入完成：成功 ' + (d.success || 0) + '，失败 ' + (d.fail || 0))
      if (d.errors && d.errors.length) {
        this.$alert(d.errors.join('<br/>'), '部分行失败', { dangerouslyUseHTMLString: true })
      }
      this.load()
    },
    onImportErr() {
      this.$message.error('导入失败')
    }
  }
}
</script>

<style scoped>
.hint { color: #909399; font-size: 12px; margin: 0 0 10px; }
.form-tip { color: #909399; font-size: 12px; line-height: 1.4; margin-top: 4px; }
.log-meta { color: #909399; font-size: 12px; margin-top: 4px; }
</style>
