<template>
  <div class="page-card">
    <div class="filter-row">
      <el-input v-model="query.orderNo" placeholder="工单编号" clearable style="width:180px;margin-right:8px" />
      <el-select v-model="query.status" clearable placeholder="状态" style="width:140px;margin-right:8px">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="isClient || isAdmin" type="success" @click="openSubmit">提交报修</el-button>
    </div>
    <p class="flow-hint">流转：待受理 → 已派单 → 处理中 → 待验收 → 已完成 → 已评价 → 已归档</p>
    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>工单编号</th>
            <th>设备</th>
            <th>冷藏间</th>
            <th>故障描述</th>
            <th>状态</th>
            <th>提交人</th>
            <th>运维</th>
            <th>提交时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.orderNo }}</td>
            <td>{{ row.deviceName }}</td>
            <td>{{ row.roomName }}</td>
            <td>{{ row.faultDesc }}</td>
            <td>{{ statusText(row.status) }}</td>
            <td>{{ row.submitUserName }}</td>
            <td>{{ row.assigneeName || '-' }}</td>
            <td>{{ row.createTime }}</td>
            <td class="ops">
              <el-button type="text" @click="openDetail(row)">详情</el-button>
              <el-button v-if="isAdmin && row.status==='PENDING'" type="text" @click="openAssign(row, 'assign')">派单</el-button>
              <el-button v-if="isAdmin && (row.status==='ASSIGNED' || row.status==='PROCESSING')" type="text" @click="openAssign(row, 'transfer')">转派</el-button>
              <el-button v-if="isAdmin && row.status==='ASSIGNED'" type="text" @click="doWithdraw(row)">撤回</el-button>
              <el-button v-if="canStart(row)" type="text" @click="doStart(row)">接单</el-button>
              <el-button v-if="canProcess(row)" type="text" @click="openProcess(row)">处理</el-button>
              <el-button v-if="canAccept(row)" type="text" @click="openAccept(row)">确认验收</el-button>
              <el-button v-if="canEval(row)" type="text" @click="openEval(row)">评价</el-button>
              <el-button v-if="isAdmin && (row.status==='DONE' || row.status==='EVALUATED')" type="text" @click="doArchive(row)">归档</el-button>
              <el-button v-if="isAdmin && isOpen(row.status)" type="text" @click="doClose(row)">关闭</el-button>
              <el-button v-if="isAdmin && row.status==='ARCHIVED'" type="text" @click="doReopen(row)">重开</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无工单数据</div>
    </div>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total" :page-size="query.size" :current-page.sync="query.page" @current-change="load" />

    <el-dialog title="提交报修" :visible.sync="submitVisible" width="560px">
      <el-form label-width="90px">
        <el-form-item label="故障设备">
          <el-select v-model="submitForm.deviceId" filterable style="width:100%">
            <el-option v-for="d in repairDevices" :key="d.id" :label="d.deviceName + '（' + d.deviceNo + '）'" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障描述">
          <el-input v-model="submitForm.faultDesc" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="现场图片">
          <el-upload
            action="#"
            :http-request="uploadImage"
            list-type="picture-card"
            :file-list="faultFileList"
            :on-success="onFaultUpload"
            :on-remove="onFaultRemove"
            :before-upload="beforeUpload"
            accept="image/*"
            :limit="6"
          >
            <i class="el-icon-plus" />
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="submitVisible=false">取消</el-button>
        <el-button type="primary" @click="doSubmit">提交</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="assignMode==='transfer' ? '转派运维' : '派单'" :visible.sync="assignVisible" width="420px">
      <el-select v-model="assigneeId" placeholder="选择运维人员" style="width:100%">
        <el-option v-for="u in opsList" :key="u.id" :label="u.realName || u.username" :value="u.id" />
      </el-select>
      <div slot="footer">
        <el-button @click="assignVisible=false">取消</el-button>
        <el-button type="primary" @click="doAssign">确认</el-button>
      </div>
    </el-dialog>

    <el-dialog title="处理工单" :visible.sync="processVisible" width="580px">
      <el-form label-width="100px">
        <el-form-item label="处理记录">
          <el-input v-model="processForm.processRecord" type="textarea" :rows="3" placeholder="填写处理过程" />
        </el-form-item>
        <el-form-item label="故障类型">
          <el-select v-model="processForm.faultType" style="width:100%">
            <el-option v-for="t in faultTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障原因">
          <el-input v-model="processForm.faultReason" />
        </el-form-item>
        <el-form-item label="处理方案">
          <el-input v-model="processForm.solution" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="耗时(分钟)">
          <el-input-number v-model="processForm.durationMinutes" :min="0" />
        </el-form-item>
        <el-form-item label="维修图片">
          <el-upload
            action="#"
            :http-request="uploadImage"
            list-type="picture-card"
            :file-list="repairFileList"
            :on-success="onRepairUpload"
            :on-remove="onRepairRemove"
            :before-upload="beforeUpload"
            accept="image/*"
            :limit="6"
          >
            <i class="el-icon-plus" />
          </el-upload>
        </el-form-item>
        <el-form-item label="领用备件">
          <div v-for="(p, idx) in processParts" :key="idx" style="display:flex;gap:8px;margin-bottom:6px">
            <el-select v-model="p.partId" filterable placeholder="备件" style="flex:1">
              <el-option
                v-for="sp in spareOptions"
                :key="sp.id"
                :label="sp.partName + '（库存' + sp.stockQty + sp.unit + '）'"
                :value="sp.id"
                :disabled="sp.stockQty <= 0"
              />
            </el-select>
            <el-input-number v-model="p.quantity" :min="1" :max="999" />
            <el-button type="text" style="color:#f56c6c" @click="processParts.splice(idx,1)">删</el-button>
          </div>
          <el-button size="mini" @click="processParts.push({ partId: null, quantity: 1 })">添加备件</el-button>
          <div class="form-tip">提交完工时扣减库存；仅保存不扣库存。</div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="doProcess(false)">仅保存</el-button>
        <el-button type="primary" @click="doProcess(true)">提交完工（待验收）</el-button>
      </div>
    </el-dialog>

    <el-dialog title="确认验收" :visible.sync="acceptVisible" width="420px">
      <el-input v-model="acceptRemark" type="textarea" :rows="3" placeholder="验收备注（可选）" />
      <div slot="footer">
        <el-button @click="acceptVisible=false">取消</el-button>
        <el-button type="primary" @click="doAccept">确认完工</el-button>
      </div>
    </el-dialog>

    <el-dialog title="满意度评价" :visible.sync="evalVisible" width="420px">
      <el-rate v-model="satisfaction" :max="5" show-text />
      <el-input v-model="evaluateContent" type="textarea" :rows="3" placeholder="评价内容" style="margin-top:12px" />
      <div slot="footer">
        <el-button type="primary" @click="doEval">提交评价</el-button>
      </div>
    </el-dialog>

    <el-dialog title="工单详情" :visible.sync="detailVisible" width="640px">
      <div v-if="current">
        <p><b>工单编号：</b>{{ current.orderNo }}</p>
        <p><b>冷库：</b>{{ current.storageName || '-' }} / {{ current.roomName || '-' }}</p>
        <p><b>设备：</b>{{ current.deviceName }}（{{ current.deviceNo }}）</p>
        <p><b>故障描述：</b>{{ current.faultDesc }}</p>
        <p><b>状态：</b>{{ statusText(current.status) }}</p>
        <p><b>提交人：</b>{{ current.submitUserName }}</p>
        <p><b>指派运维：</b>{{ current.assigneeName || '-' }}</p>
        <p><b>处理记录：</b>{{ current.processRecord || '-' }}</p>
        <p><b>故障类型：</b>{{ current.faultType || '-' }}</p>
        <p><b>故障原因：</b>{{ current.faultReason || '-' }}</p>
        <p><b>处理方案：</b>{{ current.solution || '-' }}</p>
        <p><b>耗时：</b>{{ current.durationMinutes != null ? current.durationMinutes + ' 分钟' : '-' }}</p>
        <p v-if="imageList(current.faultImages).length"><b>报修图片：</b></p>
        <div class="img-row" v-if="imageList(current.faultImages).length">
          <img v-for="(u,i) in imageList(current.faultImages)" :key="'f'+i" :src="u" @click="preview(u)" />
        </div>
        <p v-if="imageList(current.repairImages).length"><b>维修图片：</b></p>
        <div class="img-row" v-if="imageList(current.repairImages).length">
          <img v-for="(u,i) in imageList(current.repairImages)" :key="'r'+i" :src="u" @click="preview(u)" />
        </div>
        <p><b>验收备注：</b>{{ current.acceptRemark || '-' }}</p>
        <p><b>满意度：</b>{{ current.satisfaction || '-' }}</p>
        <p><b>评价：</b>{{ current.evaluateContent || '-' }}</p>
        <p v-if="current.closeReason"><b>关闭原因：</b>{{ current.closeReason }}</p>
        <div v-if="(current.parts || []).length">
          <p><b>领用备件：</b></p>
          <ul>
            <li v-for="p in current.parts" :key="p.id">{{ p.partName }}（{{ p.partNo }}）× {{ p.quantity }} {{ p.unit || '' }}</li>
          </ul>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import http from '../../utils/http'
import { getUser, statusLabel } from '../../utils/auth'

export default {
  data() {
    return {
      query: { page: 1, size: 10, orderNo: '', status: '' },
      list: [],
      total: 0,
      statusOptions: [
        { label: '待受理', value: 'PENDING' },
        { label: '已派单', value: 'ASSIGNED' },
        { label: '处理中', value: 'PROCESSING' },
        { label: '待验收', value: 'ACCEPTING' },
        { label: '已完成', value: 'DONE' },
        { label: '已评价', value: 'EVALUATED' },
        { label: '已归档', value: 'ARCHIVED' }
      ],
      submitVisible: false,
      submitForm: { deviceId: null, faultDesc: '', faultImages: '' },
      faultFileList: [],
      repairDevices: [],
      assignVisible: false,
      assignMode: 'assign',
      assigneeId: null,
      opsList: [],
      currentId: null,
      processVisible: false,
      processForm: {
        processRecord: '', faultType: '其他', faultReason: '', solution: '',
        durationMinutes: 60, repairImages: ''
      },
      repairFileList: [],
      spareOptions: [],
      processParts: [],
      faultTypes: [],
      acceptVisible: false,
      acceptRemark: '',
      evalVisible: false,
      satisfaction: 5,
      evaluateContent: '',
      detailVisible: false,
      current: null
    }
  },
  computed: {
    user() { return getUser() || {} },
    isAdmin() { return this.user.role === 'ADMIN' },
    isClient() { return this.user.role === 'CLIENT' },
    isOps() { return this.user.role === 'OPS' },
  },
  created() {
    this.load()
    this.loadFaultTypes()
  },
  methods: {
    statusText(status) { return statusLabel(status) },
    isOpen(s) {
      return ['PENDING', 'ASSIGNED', 'PROCESSING', 'ACCEPTING'].indexOf(s) >= 0
    },
    imageList(str) {
      if (!str) return []
      return String(str).split(',').map(s => s.trim()).filter(Boolean)
    },
    preview(url) { window.open(url, '_blank') },
    beforeUpload(file) {
      const ok = file.size / 1024 / 1024 < 5
      if (!ok) this.$message.error('图片不能超过 5MB')
      return ok
    },
    async uploadImage(option) {
      const form = new FormData()
      form.append('file', option.file)
      try {
        const res = await http.post('/files/upload', form)
        const url = res && res.data && res.data.url
        if (!url) {
          throw new Error((res && res.message) || '上传失败')
        }
        option.onSuccess(res)
      } catch (e) {
        option.onError(e)
      }
    },
    syncImageUrls(fileList) {
      return fileList.map(f => {
        if (f.response && f.response.data && f.response.data.url) return f.response.data.url
        return f.url
      }).filter(Boolean).join(',')
    },
    onFaultUpload(res, file, fileList) {
      if (res && res.data && res.data.url) file.url = res.data.url
      this.faultFileList = fileList
      this.submitForm.faultImages = this.syncImageUrls(fileList)
    },
    onFaultRemove(file, fileList) {
      this.faultFileList = fileList
      this.submitForm.faultImages = this.syncImageUrls(fileList)
    },
    onRepairUpload(res, file, fileList) {
      if (res && res.data && res.data.url) file.url = res.data.url
      this.repairFileList = fileList
      this.processForm.repairImages = this.syncImageUrls(fileList)
    },
    onRepairRemove(file, fileList) {
      this.repairFileList = fileList
      this.processForm.repairImages = this.syncImageUrls(fileList)
    },
    async loadFaultTypes() {
      if (this.isClient) return
      try {
        const res = await http.get('/faults/types')
        this.faultTypes = res.data || []
        if (this.faultTypes.length) this.processForm.faultType = this.faultTypes[0]
      } catch (e) { /* ignore */ }
    },
    canStart(row) {
      if (row.status !== 'ASSIGNED') return false
      if (this.isAdmin) return true
      return this.isOps && String(row.assigneeId) === String(this.user.userId)
    },
    canProcess(row) {
      if (row.status !== 'PROCESSING') return false
      if (this.isAdmin) return true
      return this.isOps && String(row.assigneeId) === String(this.user.userId)
    },
    canAccept(row) {
      if (row.status !== 'ACCEPTING') return false
      if (this.isAdmin) return true
      return this.isClient && String(row.submitUserId) === String(this.user.userId)
    },
    canEval(row) {
      return this.isClient && row.status === 'DONE' && String(row.submitUserId) === String(this.user.userId)
    },
    async load() {
      const res = await http.get('/orders/page', { params: this.query })
      const page = res.data || {}
      this.list = Array.isArray(page.records) ? page.records : []
      this.total = page.total || 0
    },
    async openSubmit() {
      const res = await http.get('/devices/repair-options')
      this.repairDevices = res.data || []
      this.submitForm = { deviceId: null, faultDesc: '', faultImages: '' }
      this.faultFileList = []
      this.submitVisible = true
    },
    async doSubmit() {
      await http.post('/orders', this.submitForm)
      this.$message.success('报修已提交（待受理）')
      this.submitVisible = false
      this.load()
    },
    async openAssign(row, mode) {
      this.currentId = row.id
      this.assignMode = mode
      const res = await http.get('/users/ops')
      this.opsList = res.data || []
      this.assigneeId = this.opsList[0] && this.opsList[0].id
      this.assignVisible = true
    },
    async doAssign() {
      const url = this.assignMode === 'transfer'
        ? '/orders/' + this.currentId + '/transfer'
        : '/orders/' + this.currentId + '/assign'
      await http.post(url, { assigneeId: this.assigneeId })
      this.$message.success(this.assignMode === 'transfer' ? '已转派' : '已派单')
      this.assignVisible = false
      this.load()
    },
    async doWithdraw(row) {
      await this.$confirm('确认撤回该派单？', '提示')
      await http.post('/orders/' + row.id + '/withdraw')
      this.$message.success('已撤回')
      this.load()
    },
    async doStart(row) {
      await http.post('/orders/' + row.id + '/start')
      this.$message.success('已接单，进入处理中')
      this.load()
    },
    async openProcess(row) {
      this.currentId = row.id
      this.processForm = {
        processRecord: row.processRecord || '',
        faultType: row.faultType || (this.faultTypes[0] || '其他'),
        faultReason: row.faultReason || '',
        solution: row.solution || '',
        durationMinutes: row.durationMinutes != null ? row.durationMinutes : 60,
        repairImages: row.repairImages || ''
      }
      this.repairFileList = this.imageList(row.repairImages).map((url, i) => ({ name: 'img' + i, url }))
      this.processParts = []
      try {
        const res = await http.get('/spare-parts/options')
        this.spareOptions = res.data || []
      } catch (e) { this.spareOptions = [] }
      this.processVisible = true
    },
    async doProcess(finish) {
      const parts = this.processParts.filter(p => p.partId && p.quantity > 0)
      await http.post('/orders/' + this.currentId + '/process', {
        ...this.processForm,
        finish,
        parts: finish ? parts : []
      })
      this.$message.success(finish ? '已提交验收，备件已扣库存' : '已保存')
      this.processVisible = false
      this.load()
    },
    openAccept(row) {
      this.currentId = row.id
      this.acceptRemark = ''
      this.acceptVisible = true
    },
    async doAccept() {
      await http.post('/orders/' + this.currentId + '/accept', { acceptRemark: this.acceptRemark })
      this.$message.success('验收完成')
      this.acceptVisible = false
      this.load()
    },
    openEval(row) {
      this.currentId = row.id
      this.satisfaction = 5
      this.evaluateContent = ''
      this.evalVisible = true
    },
    async doEval() {
      await http.post('/orders/' + this.currentId + '/evaluate', {
        satisfaction: this.satisfaction,
        evaluateContent: this.evaluateContent
      })
      this.$message.success('评价成功')
      this.evalVisible = false
      this.load()
    },
    async doArchive(row) {
      await this.$confirm('确认归档该工单？', '提示')
      await http.post('/orders/' + row.id + '/archive')
      this.$message.success('已归档')
      this.load()
    },
    async doClose(row) {
      const { value } = await this.$prompt('请输入关闭原因', '关闭工单', {
        inputPlaceholder: '关闭原因'
      })
      await http.post('/orders/' + row.id + '/close', { closeReason: value })
      this.$message.success('已关闭')
      this.load()
    },
    async doReopen(row) {
      await this.$confirm('确认重开为待受理？', '提示')
      await http.post('/orders/' + row.id + '/reopen')
      this.$message.success('已重开')
      this.load()
    },
    async openDetail(row) {
      const res = await http.get('/orders/' + row.id)
      this.current = res.data
      this.detailVisible = true
    }
  }
}
</script>

<style scoped>
.flow-hint { color: #909399; font-size: 12px; margin: 0 0 10px; }
.form-tip { color: #909399; font-size: 12px; margin-top: 4px; }
.img-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.img-row img { width: 72px; height: 72px; object-fit: cover; border-radius: 4px; cursor: pointer; border: 1px solid #ebeef5; }
</style>
