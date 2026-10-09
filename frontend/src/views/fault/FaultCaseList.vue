<template>
  <div class="page-card">
    <div class="filter-row">
      <el-select v-model="query.deviceType" clearable placeholder="适用设备类型" style="width:180px;margin-right:8px">
        <el-option v-for="t in deviceTypes" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="query.faultType" clearable placeholder="故障类型" style="width:160px;margin-right:8px">
        <el-option v-for="t in faultTypes" :key="t" :label="t" :value="t" />
      </el-select>
      <el-input v-model="query.keyword" clearable placeholder="关键词（现象/原因/方案）" style="width:220px;margin-right:8px" />
      <el-button type="primary" @click="load">检索</el-button>
      <el-button type="success" @click="openEdit()">新增案例</el-button>
    </div>
    <p class="hint">非 AI 经验库：按设备类型 + 故障类型 + 关键词检索历史处理经验。</p>
    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>标题</th>
            <th>故障类型</th>
            <th>适用设备类型</th>
            <th>故障现象</th>
            <th>处理方案</th>
            <th>创建人</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.title }}</td>
            <td>{{ row.faultType }}</td>
            <td>{{ row.deviceType || '-' }}</td>
            <td>{{ row.faultPhenomenon || '-' }}</td>
            <td>{{ row.solution || '-' }}</td>
            <td>{{ row.creatorName || '-' }}</td>
            <td>{{ row.createTime }}</td>
            <td class="ops">
              <el-button type="text" @click="openDetail(row)">详情</el-button>
              <el-button v-if="canEdit(row)" type="text" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="canEdit(row)" type="text" style="color:#f56c6c" @click="remove(row)">删除</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无案例，可从故障记录「沉淀案例」或在此新增</div>
    </div>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
                   :page-size="query.size" :current-page.sync="query.page" @current-change="load" />

    <el-dialog :title="form.id ? '编辑案例' : '新增案例'" :visible.sync="visible" width="680px">
      <el-form label-width="110px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="故障类型">
          <el-select v-model="form.faultType" style="width:100%">
            <el-option v-for="t in faultTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用设备类型">
          <el-select v-model="form.deviceType" clearable style="width:100%">
            <el-option v-for="t in deviceTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障现象"><el-input v-model="form.faultPhenomenon" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="故障原因"><el-input v-model="form.faultReason" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="排查方法"><el-input v-model="form.checkMethod" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="处理方案"><el-input v-model="form.solution" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="注意事项"><el-input v-model="form.notice" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="附件图片">
          <el-upload action="#" :http-request="uploadImage" list-type="picture-card"
                     :file-list="fileList" :on-success="onUpload" :on-remove="onRemove"
                     :limit="6" accept="image/*">
            <i class="el-icon-plus" />
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog title="案例详情" :visible.sync="detailVisible" width="680px">
      <div v-if="current">
        <p><b>标题：</b>{{ current.title }}</p>
        <p><b>故障类型：</b>{{ current.faultType }}</p>
        <p><b>适用设备：</b>{{ current.deviceType || '-' }}</p>
        <p><b>故障现象：</b>{{ current.faultPhenomenon || '-' }}</p>
        <p><b>故障原因：</b>{{ current.faultReason || '-' }}</p>
        <p><b>排查方法：</b>{{ current.checkMethod || '-' }}</p>
        <p><b>处理方案：</b>{{ current.solution || '-' }}</p>
        <p><b>注意事项：</b>{{ current.notice || '-' }}</p>
        <p><b>创建人：</b>{{ current.creatorName || '-' }} · {{ current.createTime }}</p>
        <div v-if="images(current.attachment).length" class="img-row">
          <img v-for="(u,i) in images(current.attachment)" :key="i" :src="u" @click="preview(u)" />
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
      query: { page: 1, size: 10, deviceType: '', faultType: '', keyword: '' },
      list: [],
      total: 0,
      deviceTypes: [],
      faultTypes: [],
      visible: false,
      form: {},
      fileList: [],
      detailVisible: false,
      current: null
    }
  },
  computed: {
    user() { return getUser() || {} },
    isAdmin() { return this.user.role === 'ADMIN' }
  },
  created() { this.init() },
  methods: {
    canEdit(row) {
      return this.isAdmin || String(row.creatorId) === String(this.user.userId)
    },
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
      this.fileList = fileList
      this.form.attachment = this.syncImages(fileList)
    },
    onRemove(file, fileList) {
      this.fileList = fileList
      this.form.attachment = this.syncImages(fileList)
    },
    async init() {
      const [dt, ft] = await Promise.all([
        http.get('/devices/types'),
        http.get('/faults/types')
      ])
      this.deviceTypes = dt.data || []
      this.faultTypes = ft.data || []
      this.load()
    },
    async load() {
      const res = await http.get('/fault-cases/page', { params: this.query })
      this.list = (res.data && res.data.records) || []
      this.total = (res.data && res.data.total) || 0
    },
    openEdit(row) {
      this.form = row ? { ...row } : {
        title: '', faultType: this.faultTypes[0], deviceType: '',
        faultPhenomenon: '', faultReason: '', checkMethod: '', solution: '', notice: '', attachment: ''
      }
      this.fileList = this.images(this.form.attachment).map((url, i) => ({ name: 'img' + i, url }))
      this.visible = true
    },
    async openDetail(row) {
      const res = await http.get('/fault-cases/' + row.id)
      this.current = res.data
      this.detailVisible = true
    },
    async save() {
      await http.post('/fault-cases', this.form)
      this.$message.success('保存成功')
      this.visible = false
      this.load()
    },
    remove(row) {
      this.$confirm('确认删除该案例？', '提示').then(async () => {
        await http.delete('/fault-cases/' + row.id)
        this.$message.success('已删除')
        this.load()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.hint { color: #909399; font-size: 12px; margin: 0 0 10px; }
.img-row { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.img-row img { width: 72px; height: 72px; object-fit: cover; border-radius: 4px; cursor: pointer; border: 1px solid #ebeef5; }
</style>
