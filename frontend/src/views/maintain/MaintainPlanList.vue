<template>
  <div class="page-card">
    <div class="filter-row">
      <el-select v-model="query.cycleType" clearable placeholder="周期" style="width:140px;margin-right:8px">
        <el-option v-for="c in cycles" :key="c.value" :label="c.label" :value="c.value" />
      </el-select>
      <el-select v-model="query.status" clearable placeholder="状态" style="width:120px;margin-right:8px">
        <el-option label="进行中" value="ACTIVE" />
        <el-option label="已关闭" value="CLOSED" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="isAdmin" type="success" @click="openEdit()">新建计划</el-button>
    </div>
    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>计划编号</th>
            <th>标题</th>
            <th>周期</th>
            <th>设备类型</th>
            <th>到期日</th>
            <th>任务完成</th>
            <th>执行状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.planNo }}</td>
            <td>{{ row.title }}</td>
            <td>{{ cycleLabel(row.cycleType) }}</td>
            <td>{{ row.deviceType || '全部类型' }}</td>
            <td>{{ row.dueDate }}</td>
            <td>{{ row.taskDone || 0 }}/{{ row.taskTotal || 0 }}</td>
            <td>{{ row.displayStatus }}</td>
            <td class="ops">
              <el-button type="text" @click="openDetail(row)">详情</el-button>
              <el-button type="text" @click="$router.push({ path: '/maintain-tasks', query: { planId: row.id } })">任务</el-button>
              <el-button v-if="isAdmin && row.status!=='CLOSED'" type="text" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="isAdmin && row.status!=='CLOSED'" type="text" @click="generate(row)">生成任务</el-button>
              <el-button v-if="isAdmin && row.status!=='CLOSED'" type="text" @click="closePlan(row)">关闭</el-button>
              <el-button v-if="isAdmin" type="text" style="color:#f56c6c" @click="remove(row)">删除</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无维保计划</div>
    </div>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
                   :page-size="query.size" :current-page.sync="query.page" @current-change="load" />

    <el-dialog :title="form.id ? '编辑维保计划' : '新建维保计划'" :visible.sync="visible" width="640px">
      <el-form label-width="110px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="周期类型">
          <el-select v-model="form.cycleType" style="width:100%">
            <el-option v-for="c in cycles" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用设备类型">
          <el-select v-model="form.deviceType" clearable filterable style="width:100%" @change="loadDefaultItems">
            <el-option v-for="t in deviceTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划到期日">
          <el-date-picker v-model="form.dueDate" type="date" value-format="yyyy-MM-dd" style="width:100%" />
        </el-form-item>
        <el-form-item label="检查项目">
          <div v-for="(it, idx) in form.items" :key="idx" style="display:flex;margin-bottom:6px">
            <el-input v-model="form.items[idx]" />
            <el-button type="text" style="color:#f56c6c;margin-left:6px" @click="form.items.splice(idx,1)">删</el-button>
          </div>
          <el-button size="mini" @click="form.items.push('')">添加项目</el-button>
          <el-button size="mini" type="text" @click="loadDefaultItems">载入推荐项</el-button>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog title="计划详情" :visible.sync="detailVisible" width="560px">
      <div v-if="current">
        <p><b>编号：</b>{{ current.planNo }}</p>
        <p><b>标题：</b>{{ current.title }}</p>
        <p><b>周期：</b>{{ cycleLabel(current.cycleType) }}</p>
        <p><b>设备类型：</b>{{ current.deviceType || '全部' }}</p>
        <p><b>到期日：</b>{{ current.dueDate }}</p>
        <p><b>执行状态：</b>{{ current.displayStatus }}</p>
        <p><b>任务：</b>{{ current.taskDone || 0 }}/{{ current.taskTotal || 0 }}</p>
        <p><b>检查项：</b></p>
        <ul>
          <li v-for="(it,i) in (current.items || [])" :key="i">{{ it }}</li>
        </ul>
        <p><b>备注：</b>{{ current.remark || '-' }}</p>
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
      query: { page: 1, size: 10, status: '', cycleType: '' },
      list: [],
      total: 0,
      cycles: [
        { label: '月度', value: 'MONTHLY' },
        { label: '季度', value: 'QUARTERLY' },
        { label: '半年度', value: 'HALF_YEAR' },
        { label: '年度', value: 'YEARLY' }
      ],
      deviceTypes: [],
      visible: false,
      form: {},
      detailVisible: false,
      current: null
    }
  },
  computed: {
    isAdmin() { return (getUser() || {}).role === 'ADMIN' }
  },
  created() { this.init() },
  methods: {
    cycleLabel(v) {
      const hit = this.cycles.find(c => c.value === v)
      return hit ? hit.label : v
    },
    async init() {
      const res = await http.get('/devices/types')
      this.deviceTypes = res.data || []
      this.load()
    },
    async load() {
      const res = await http.get('/maintain-plans/page', { params: this.query })
      this.list = (res.data && res.data.records) || []
      this.total = (res.data && res.data.total) || 0
    },
    async loadDefaultItems() {
      const res = await http.get('/maintain-plans/default-items', { params: { deviceType: this.form.deviceType } })
      this.$set(this.form, 'items', res.data || [])
    },
    async openEdit(row) {
      if (row) {
        const res = await http.get('/maintain-plans/' + row.id)
        this.form = { ...res.data, items: [...(res.data.items || [])] }
      } else {
        this.form = {
          title: '', cycleType: 'MONTHLY', deviceType: '', dueDate: '',
          status: 'ACTIVE', remark: '', items: []
        }
        await this.loadDefaultItems()
      }
      this.visible = true
    },
    async openDetail(row) {
      const res = await http.get('/maintain-plans/' + row.id)
      this.current = res.data
      this.detailVisible = true
    },
    async save() {
      await http.post('/maintain-plans', this.form)
      this.$message.success('保存成功')
      this.visible = false
      this.load()
    },
    async generate(row) {
      await this.$confirm('按计划设备类型生成维保任务？', '提示')
      const res = await http.post('/maintain-plans/' + row.id + '/generate-tasks')
      this.$message.success('已生成 ' + ((res.data && res.data.created) || 0) + ' 条任务')
      this.load()
    },
    async closePlan(row) {
      await this.$confirm('确认关闭该计划？', '提示')
      await http.post('/maintain-plans/' + row.id + '/close')
      this.$message.success('已关闭')
      this.load()
    },
    remove(row) {
      this.$confirm('确认删除？无任务的计划才可删', '提示').then(async () => {
        await http.delete('/maintain-plans/' + row.id)
        this.$message.success('已删除')
        this.load()
      }).catch(() => {})
    }
  }
}
</script>
