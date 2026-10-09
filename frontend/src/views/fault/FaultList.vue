<template>
  <div class="page-card">
    <div class="filter-row">
      <el-select v-model="query.deviceId" clearable filterable placeholder="设备" style="width:200px;margin-right:8px">
        <el-option v-for="d in devices" :key="d.id" :label="d.deviceName + '（' + d.deviceNo + '）'" :value="d.id" />
      </el-select>
      <el-select v-model="query.faultType" clearable placeholder="故障类型" style="width:160px;margin-right:8px">
        <el-option v-for="t in types" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="query.faultLevel" clearable placeholder="等级" style="width:110px;margin-right:8px">
        <el-option v-for="l in levels" :key="l" :label="l" :value="l" />
      </el-select>
      <el-date-picker v-model="dateRange" type="datetimerange" value-format="yyyy-MM-dd HH:mm:ss"
                      start-placeholder="开始时间" end-placeholder="结束时间" style="margin-right:8px" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openEdit()">新增记录</el-button>
    </div>
    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>记录编号</th>
            <th>设备</th>
            <th>冷藏间</th>
            <th>关联工单</th>
            <th>故障类型</th>
            <th>等级</th>
            <th>现象</th>
            <th>耗时(分)</th>
            <th>处理人</th>
            <th>处理时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.recordNo }}</td>
            <td>{{ row.deviceName }}</td>
            <td>{{ row.roomName }}</td>
            <td>{{ row.orderNo || '-' }}</td>
            <td>{{ row.faultType }}</td>
            <td>{{ row.faultLevel || '一般' }}</td>
            <td>{{ row.faultPhenomenon || row.faultDesc || '-' }}</td>
            <td>{{ row.durationMinutes }}</td>
            <td>{{ row.handlerName }}</td>
            <td>{{ row.handleTime }}</td>
            <td class="ops">
              <el-button type="text" @click="openDetail(row)">详情</el-button>
              <el-button type="text" @click="openEdit(row)">编辑</el-button>
              <el-button type="text" @click="promote(row)">沉淀案例</el-button>
              <el-button v-if="isAdmin" type="text" style="color:#f56c6c" @click="remove(row)">删除</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无故障记录</div>
    </div>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
                   :page-size="query.size" :current-page.sync="query.page" @current-change="load" />

    <el-dialog :title="form.id ? '编辑故障记录' : '新增故障记录'" :visible.sync="visible" width="640px">
      <el-form label-width="100px">
        <el-form-item label="设备">
          <el-select v-model="form.deviceId" filterable style="width:100%">
            <el-option v-for="d in devices" :key="d.id" :label="d.deviceName + '（' + d.deviceNo + '）'" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障类型">
          <el-select v-model="form.faultType" style="width:100%">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障等级">
          <el-select v-model="form.faultLevel" style="width:100%">
            <el-option v-for="l in levels" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联工单ID"><el-input v-model="form.orderId" placeholder="可选" /></el-form-item>
        <el-form-item label="故障现象"><el-input v-model="form.faultPhenomenon" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="故障原因"><el-input v-model="form.faultReason" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="处理方案"><el-input v-model="form.solution" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="使用备件"><el-input v-model="form.partsUsed" placeholder="文字说明，库存扣减在备件模块" /></el-form-item>
        <el-form-item label="耗时(分钟)"><el-input-number v-model="form.durationMinutes" :min="0" /></el-form-item>
        <el-form-item label="故障图片">
          <el-upload action="#" :http-request="uploadImage" list-type="picture-card"
                     :file-list="imageList" :on-success="onUpload" :on-remove="onRemove"
                     :limit="6" accept="image/*">
            <i class="el-icon-plus" />
          </el-upload>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog title="故障记录详情" :visible.sync="detailVisible" width="640px">
      <div v-if="current">
        <p><b>编号：</b>{{ current.recordNo }}</p>
        <p><b>设备：</b>{{ current.deviceName }}（{{ current.deviceNo }}） / {{ current.deviceType || '-' }}</p>
        <p><b>冷藏间：</b>{{ current.roomName || '-' }}</p>
        <p><b>关联工单：</b>{{ current.orderNo || '-' }}</p>
        <p><b>类型/等级：</b>{{ current.faultType }} / {{ current.faultLevel || '一般' }}</p>
        <p><b>现象：</b>{{ current.faultPhenomenon || current.faultDesc || '-' }}</p>
        <p><b>原因：</b>{{ current.faultReason || '-' }}</p>
        <p><b>方案：</b>{{ current.solution || '-' }}</p>
        <p><b>备件：</b>{{ current.partsUsed || '-' }}</p>
        <p><b>耗时：</b>{{ current.durationMinutes != null ? current.durationMinutes + ' 分钟' : '-' }}</p>
        <p><b>处理人：</b>{{ current.handlerName || '-' }}</p>
        <p><b>处理时间：</b>{{ current.handleTime || '-' }}</p>
        <p><b>备注：</b>{{ current.remark || '-' }}</p>
        <div v-if="images(current.faultImages).length" class="img-row">
          <img v-for="(u,i) in images(current.faultImages)" :key="i" :src="u" @click="preview(u)" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import http from '../../utils/http'
import { getUser } from '../../utils/auth'

export default {
  data() {
    return {
      query: { page: 1, size: 10, faultType: '', faultLevel: '', deviceId: null },
      dateRange: [],
      list: [],
      total: 0,
      types: [],
      levels: [],
      devices: [],
      visible: false,
      form: {},
      imageList: [],
      detailVisible: false,
      current: null
    }
  },
  computed: {
    isAdmin() { return (getUser() || {}).role === 'ADMIN' }
  },
  created() { this.init() },
  methods: {
    images(str) {
      if (!str) return []
      return String(str).split(',').map(s => s.trim()).filter(Boolean)
    },
    preview(url) { window.open(url, '_blank') },
    async uploadImage(option) {
      const form = new FormData()
      form.append('file', option.file)
      try {
        const res = await http.post('/files/upload', form)
        option.onSuccess(res)
      } catch (e) {
        option.onError(e)
      }
    },
    syncImages(fileList) {
      return fileList.map(f => (f.response && f.response.data && f.response.data.url) || f.url).filter(Boolean).join(',')
    },
    onUpload(res, file, fileList) {
      if (res && res.data && res.data.url) file.url = res.data.url
      this.imageList = fileList
      this.form.faultImages = this.syncImages(fileList)
    },
    onRemove(file, fileList) {
      this.imageList = fileList
      this.form.faultImages = this.syncImages(fileList)
    },
    async init() {
      const [types, levels, devices] = await Promise.all([
        http.get('/faults/types'),
        http.get('/faults/levels'),
        http.get('/devices/page', { params: { page: 1, size: 200 } })
      ])
      this.types = types.data || []
      this.levels = levels.data || ['一般', '严重', '紧急']
      this.devices = (devices.data && devices.data.records) || []
      this.load()
    },
    async load() {
      const params = { ...this.query }
      if (this.dateRange && this.dateRange.length === 2) {
        params.startTime = this.dateRange[0]
        params.endTime = this.dateRange[1]
      }
      const res = await http.get('/faults/page', { params })
      this.list = (res.data && res.data.records) || []
      this.total = (res.data && res.data.total) || 0
    },
    openEdit(row) {
      this.form = row ? { ...row } : {
        deviceId: null, faultType: this.types[0], faultLevel: '一般',
        faultPhenomenon: '', faultReason: '', solution: '', partsUsed: '',
        durationMinutes: 30, faultImages: '', remark: '', orderId: null
      }
      this.imageList = this.images(this.form.faultImages).map((url, i) => ({ name: 'img' + i, url }))
      this.visible = true
    },
    async openDetail(row) {
      const res = await http.get('/faults/' + row.id)
      this.current = res.data
      this.detailVisible = true
    },
    async save() {
      const payload = { ...this.form }
      if (payload.orderId === '' || payload.orderId === undefined) payload.orderId = null
      else if (payload.orderId) payload.orderId = Number(payload.orderId)
      if (!payload.faultDesc) payload.faultDesc = payload.faultPhenomenon
      await http.post('/faults', payload)
      this.$message.success('保存成功')
      this.visible = false
      this.load()
    },
    async promote(row) {
      const { value } = await this.$prompt('案例标题（可改）', '沉淀到案例库', {
        inputValue: (row.faultType || '故障') + '案例-' + row.recordNo
      })
      await http.post('/fault-cases/promote/' + row.id, { title: value })
      this.$message.success('已沉淀到案例库')
    },
    remove(row) {
      this.$confirm('确认删除？', '提示').then(async () => {
        await http.delete('/faults/' + row.id)
        this.$message.success('已删除')
        this.load()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.img-row { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.img-row img { width: 72px; height: 72px; object-fit: cover; border-radius: 4px; cursor: pointer; border: 1px solid #ebeef5; }
</style>
