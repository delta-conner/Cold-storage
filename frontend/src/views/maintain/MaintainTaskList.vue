<template>
  <div class="page-card">
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="4"><div class="mini">任务总数<br><b>{{ summary.total || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="mini">待执行<br><b>{{ summary.pending || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="mini">即将到期<br><b>{{ summary.nearDue || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="mini">已逾期<br><b style="color:#f56c6c">{{ summary.overdue || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="mini">已完成<br><b>{{ summary.done || 0 }}</b></div></el-col>
      <el-col :span="4"><div class="mini">完成率<br><b>{{ summary.doneRate || 0 }}%</b></div></el-col>
    </el-row>

    <div class="filter-row">
      <el-select v-model="query.status" clearable placeholder="完成状态" style="width:130px;margin-right:8px">
        <el-option label="未完成" value="PENDING" />
        <el-option label="已完成" value="DONE" />
      </el-select>
      <el-select v-model="query.displayFilter" clearable placeholder="到期状态" style="width:130px;margin-right:8px">
        <el-option label="待执行" value="待执行" />
        <el-option label="即将到期" value="即将到期" />
        <el-option label="已逾期" value="已逾期" />
        <el-option label="已完成" value="已完成" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="$router.push('/maintain-plans')">维保计划</el-button>
      <el-button @click="$router.push('/maintain')">周期提醒(旧)</el-button>
    </div>

    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>任务编号</th>
            <th>计划</th>
            <th>设备</th>
            <th>冷藏间</th>
            <th>到期日</th>
            <th>剩余</th>
            <th>状态</th>
            <th>指派运维</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id" :class="{ urgent: row.displayStatus==='已逾期' }">
            <td>{{ row.taskNo }}</td>
            <td>{{ row.planTitle }}</td>
            <td>{{ row.deviceName }}</td>
            <td>{{ row.roomName || '-' }}</td>
            <td>{{ row.dueDate }}</td>
            <td>{{ row.status==='DONE' ? '-' : ((row.daysLeft != null ? row.daysLeft : '-') + '天') }}</td>
            <td>{{ row.displayStatus }}</td>
            <td>{{ row.assigneeName || '-' }}</td>
            <td class="ops">
              <el-button type="text" @click="openDetail(row)">详情</el-button>
              <el-button v-if="isAdmin && row.status!=='DONE'" type="text" @click="openAssign(row)">指派</el-button>
              <el-button v-if="canComplete(row)" type="text" @click="openComplete(row)">完成维保</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无维保任务</div>
    </div>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
                   :page-size="query.size" :current-page.sync="query.page" @current-change="load" />

    <el-dialog title="指派运维" :visible.sync="assignVisible" width="420px">
      <el-select v-model="assigneeId" style="width:100%">
        <el-option v-for="u in opsList" :key="u.id" :label="u.realName || u.username" :value="u.id" />
      </el-select>
      <div slot="footer">
        <el-button @click="assignVisible=false">取消</el-button>
        <el-button type="primary" @click="doAssign">确认</el-button>
      </div>
    </el-dialog>

    <el-dialog title="完成维保" :visible.sync="completeVisible" width="720px">
      <el-form label-width="100px" v-if="completeForm">
        <el-form-item label="检查项目">
          <table class="data-table">
            <thead><tr><th>项目</th><th>结果</th><th>备注</th></tr></thead>
            <tbody>
              <tr v-for="it in completeForm.items" :key="it.id">
                <td>{{ it.itemName }}</td>
                <td>
                  <el-select v-model="it.result" size="mini" style="width:110px">
                    <el-option label="正常" value="OK" />
                    <el-option label="异常" value="ABNORMAL" />
                    <el-option label="不适用" value="NA" />
                  </el-select>
                </td>
                <td><el-input v-model="it.remark" size="mini" /></td>
              </tr>
            </tbody>
          </table>
        </el-form-item>
        <el-form-item label="结果摘要"><el-input v-model="completeForm.resultSummary" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="处理措施"><el-input v-model="completeForm.measures" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="使用备件"><el-input v-model="completeForm.partsUsed" placeholder="文字说明" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="completeVisible=false">取消</el-button>
        <el-button type="primary" @click="doComplete">提交完成</el-button>
      </div>
    </el-dialog>

    <el-dialog title="任务详情" :visible.sync="detailVisible" width="640px">
      <div v-if="current">
        <p><b>任务号：</b>{{ current.taskNo }}</p>
        <p><b>计划：</b>{{ current.planTitle }}</p>
        <p><b>设备：</b>{{ current.deviceName }}（{{ current.deviceNo }}）</p>
        <p><b>到期日：</b>{{ current.dueDate }} · {{ current.displayStatus }}</p>
        <p><b>指派：</b>{{ current.assigneeName || '-' }}</p>
        <p><b>处理人：</b>{{ current.handlerName || '-' }}</p>
        <p><b>结果摘要：</b>{{ current.resultSummary || '-' }}</p>
        <p><b>处理措施：</b>{{ current.measures || '-' }}</p>
        <p><b>备件：</b>{{ current.partsUsed || '-' }}</p>
        <p><b>是否异常：</b>{{ current.hasAbnormal === 1 ? '是' : '否' }}</p>
        <table class="data-table" v-if="(current.items || []).length" style="margin-top:8px">
          <thead><tr><th>检查项</th><th>结果</th><th>备注</th></tr></thead>
          <tbody>
            <tr v-for="it in current.items" :key="it.id">
              <td>{{ it.itemName }}</td>
              <td>{{ resultText(it.result) }}</td>
              <td>{{ it.remark || '-' }}</td>
            </tr>
          </tbody>
        </table>
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
      query: { page: 1, size: 10, status: '', displayFilter: '', planId: null },
      list: [],
      total: 0,
      summary: {},
      assignVisible: false,
      assigneeId: null,
      opsList: [],
      currentId: null,
      completeVisible: false,
      completeForm: null,
      detailVisible: false,
      current: null
    }
  },
  computed: {
    user() { return getUser() || {} },
    isAdmin() { return this.user.role === 'ADMIN' },
    isOps() { return this.user.role === 'OPS' }
  },
  created() {
    if (this.$route.query.planId) {
      this.query.planId = Number(this.$route.query.planId)
    }
    this.loadSummary()
    this.load()
  },
  methods: {
    resultText(r) {
      return ({ OK: '正常', ABNORMAL: '异常', NA: '不适用' })[r] || r || '-'
    },
    canComplete(row) {
      if (row.status === 'DONE') return false
      if (this.isAdmin) return true
      if (!this.isOps) return false
      if (!row.assigneeId) return true
      return String(row.assigneeId) === String(this.user.userId)
    },
    async loadSummary() {
      const res = await http.get('/maintain-tasks/summary')
      this.summary = res.data || {}
    },
    async load() {
      const res = await http.get('/maintain-tasks/page', { params: this.query })
      this.list = (res.data && res.data.records) || []
      this.total = (res.data && res.data.total) || 0
    },
    async openAssign(row) {
      this.currentId = row.id
      const res = await http.get('/users/ops')
      this.opsList = res.data || []
      this.assigneeId = this.opsList[0] && this.opsList[0].id
      this.assignVisible = true
    },
    async doAssign() {
      await http.post('/maintain-tasks/' + this.currentId + '/assign', { assigneeId: this.assigneeId })
      this.$message.success('已指派')
      this.assignVisible = false
      this.load()
    },
    async openComplete(row) {
      const res = await http.get('/maintain-tasks/' + row.id)
      const data = res.data || {}
      this.completeForm = {
        id: data.id,
        resultSummary: data.resultSummary || '',
        measures: data.measures || '',
        partsUsed: data.partsUsed || '',
        items: (data.items || []).map(it => ({ ...it, result: it.result || 'OK' }))
      }
      this.completeVisible = true
    },
    async doComplete() {
      await http.post('/maintain-tasks/' + this.completeForm.id + '/complete', this.completeForm)
      this.$message.success('维保已完成')
      this.completeVisible = false
      this.loadSummary()
      this.load()
    },
    async openDetail(row) {
      const res = await http.get('/maintain-tasks/' + row.id)
      this.current = res.data
      this.detailVisible = true
    }
  }
}
</script>

<style scoped>
.mini { background:#f5f7fa; border-radius:6px; padding:10px 8px; text-align:center; color:#606266; }
.mini b { font-size:18px; color:#0b3a4a; }
.urgent td { background:#fff1f0; }
</style>
