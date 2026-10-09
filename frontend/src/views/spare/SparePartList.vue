<template>
  <div class="page-card">
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="8"><div class="mini">备件SKU<br><b>{{ summary.skuCount || 0 }}</b></div></el-col>
      <el-col :span="8"><div class="mini">库存总量<br><b>{{ summary.totalQty || 0 }}</b></div></el-col>
      <el-col :span="8"><div class="mini">低库存预警<br><b style="color:#f56c6c">{{ summary.lowStockCount || 0 }}</b></div></el-col>
    </el-row>

    <div class="filter-row">
      <el-input v-model="query.partNo" placeholder="编号" clearable style="width:120px;margin-right:8px" />
      <el-input v-model="query.partName" placeholder="名称" clearable style="width:140px;margin-right:8px" />
      <el-select v-model="query.supplierId" clearable placeholder="供应商" style="width:160px;margin-right:8px">
        <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-checkbox v-model="query.lowOnly" style="margin-right:8px">仅低库存</el-checkbox>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="isAdmin" type="success" @click="openEdit()">新增备件</el-button>
      <el-button @click="openRecords()">出入库流水</el-button>
    </div>

    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>编号</th>
            <th>名称</th>
            <th>类型</th>
            <th>规格/品牌</th>
            <th>单位</th>
            <th>库存</th>
            <th>安全库存</th>
            <th>供应商</th>
            <th>单价</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id" :class="{ low: row.lowStock }">
            <td>{{ row.partNo }}</td>
            <td>{{ row.partName }}</td>
            <td>{{ row.partType || '-' }}</td>
            <td>{{ (row.spec || '-') + ' / ' + (row.brand || '-') }}</td>
            <td>{{ row.unit }}</td>
            <td :style="{color: row.lowStock ? '#f56c6c' : '', fontWeight: row.lowStock ? 700 : 400}">{{ row.stockQty }}</td>
            <td>{{ row.safetyStock }}</td>
            <td>{{ row.supplierName || '-' }}</td>
            <td>{{ row.unitPrice != null ? row.unitPrice : '-' }}</td>
            <td class="ops">
              <el-button type="text" @click="openStock(row, 'IN')">入库</el-button>
              <el-button type="text" @click="openStock(row, 'OUT')">出库</el-button>
              <el-button type="text" @click="openStock(row, 'ADJUST')">调整</el-button>
              <el-button v-if="isAdmin" type="text" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="isAdmin" type="text" style="color:#f56c6c" @click="remove(row)">删除</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无备件</div>
    </div>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
                   :page-size="query.size" :current-page.sync="query.page" @current-change="load" />

    <el-dialog :title="form.id ? '编辑备件' : '新增备件'" :visible.sync="visible" width="560px">
      <el-form label-width="100px">
        <el-form-item label="编号"><el-input v-model="form.partNo" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.partName" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="form.partType" placeholder="如油品/传感器" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="form.spec" /></el-form-item>
        <el-form-item label="品牌"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item v-if="!form.id" label="初始库存"><el-input-number v-model="form.stockQty" :min="0" /></el-form-item>
        <el-form-item label="安全库存"><el-input-number v-model="form.safetyStock" :min="0" /></el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="form.supplierId" clearable style="width:100%">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价"><el-input-number v-model="form.unitPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="stockTitle" :visible.sync="stockVisible" width="420px">
      <el-form label-width="100px">
        <el-form-item :label="stockType==='ADJUST' ? '调整后数量' : '数量'">
          <el-input-number v-model="stockQty" :min="0" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="stockRemark" type="textarea" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="stockVisible=false">取消</el-button>
        <el-button type="primary" @click="doStock">确认</el-button>
      </div>
    </el-dialog>

    <el-dialog title="出入库流水" :visible.sync="recordVisible" width="780px">
      <div class="filter-row">
        <el-select v-model="recordQuery.changeType" clearable placeholder="类型" style="width:120px;margin-right:8px">
          <el-option label="入库" value="IN" />
          <el-option label="出库" value="OUT" />
          <el-option label="调整" value="ADJUST" />
          <el-option label="盘点" value="CHECK" />
        </el-select>
        <el-button type="primary" size="mini" @click="loadRecords">刷新</el-button>
      </div>
      <table class="data-table" v-if="records.length">
        <thead>
          <tr><th>流水号</th><th>备件</th><th>类型</th><th>变动</th><th>前→后</th><th>业务</th><th>操作人</th><th>时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="r in records" :key="r.id">
            <td>{{ r.recordNo }}</td>
            <td>{{ r.partName }}</td>
            <td>{{ typeText(r.changeType) }}</td>
            <td>{{ r.changeQty }}</td>
            <td>{{ r.beforeQty }} → {{ r.afterQty }}</td>
            <td>{{ r.bizType || '-' }}</td>
            <td>{{ r.operatorName || '-' }}</td>
            <td>{{ r.createTime }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无流水</div>
    </el-dialog>
  </div>
</template>

<script>
import http from '../../utils/http'
import { getUser } from '../../utils/auth'

export default {
  data() {
    return {
      query: { page: 1, size: 10, partNo: '', partName: '', supplierId: null, lowOnly: false },
      list: [],
      total: 0,
      summary: {},
      suppliers: [],
      visible: false,
      form: {},
      stockVisible: false,
      stockType: 'IN',
      stockQty: 1,
      stockRemark: '',
      stockPartId: null,
      recordVisible: false,
      recordQuery: { page: 1, size: 20, changeType: '' },
      records: []
    }
  },
  computed: {
    isAdmin() { return (getUser() || {}).role === 'ADMIN' },
    stockTitle() {
      return ({ IN: '入库', OUT: '出库', ADJUST: '库存调整', CHECK: '盘点' })[this.stockType] || '库存变动'
    }
  },
  created() { this.init() },
  methods: {
    typeText(t) {
      return ({ IN: '入库', OUT: '出库', ADJUST: '调整', CHECK: '盘点' })[t] || t
    },
    async init() {
      const res = await http.get('/suppliers/list')
      this.suppliers = res.data || []
      this.loadSummary()
      this.load()
    },
    async loadSummary() {
      const res = await http.get('/spare-parts/summary')
      this.summary = res.data || {}
    },
    async load() {
      const res = await http.get('/spare-parts/page', { params: this.query })
      this.list = (res.data && res.data.records) || []
      this.total = (res.data && res.data.total) || 0
    },
    openEdit(row) {
      this.form = row ? { ...row } : {
        partNo: '', partName: '', partType: '', spec: '', brand: '', unit: '个',
        stockQty: 0, safetyStock: 5, supplierId: null, unitPrice: 0, remark: ''
      }
      this.visible = true
    },
    async save() {
      await http.post('/spare-parts', this.form)
      this.$message.success('保存成功')
      this.visible = false
      this.loadSummary()
      this.load()
    },
    openStock(row, type) {
      this.stockPartId = row.id
      this.stockType = type
      this.stockQty = type === 'ADJUST' ? row.stockQty : 1
      this.stockRemark = ''
      this.stockVisible = true
    },
    async doStock() {
      await http.post('/spare-parts/' + this.stockPartId + '/stock', {
        changeType: this.stockType,
        qty: this.stockQty,
        remark: this.stockRemark
      })
      this.$message.success('库存已更新')
      this.stockVisible = false
      this.loadSummary()
      this.load()
    },
    async openRecords() {
      this.recordVisible = true
      this.loadRecords()
    },
    async loadRecords() {
      const res = await http.get('/spare-parts/records', { params: this.recordQuery })
      this.records = (res.data && res.data.records) || []
    },
    remove(row) {
      this.$confirm('确认删除？', '提示').then(async () => {
        await http.delete('/spare-parts/' + row.id)
        this.$message.success('已删除')
        this.loadSummary()
        this.load()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.mini { background:#f5f7fa; border-radius:6px; padding:10px; text-align:center; color:#606266; }
.mini b { font-size:18px; color:#0b3a4a; }
.low td { background:#fff1f0; }
</style>
