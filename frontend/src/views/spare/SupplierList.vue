<template>
  <div class="page-card">
    <div class="filter-row">
      <el-input v-model="query.name" placeholder="供应商名称" clearable style="width:180px;margin-right:8px" />
      <el-select v-model="query.status" clearable placeholder="合作状态" style="width:140px;margin-right:8px">
        <el-option label="合作中" value="COOPERATING" />
        <el-option label="已停用" value="STOPPED" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="isAdmin" type="success" @click="openEdit()">新增供应商</el-button>
    </div>
    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>名称</th>
            <th>联系人</th>
            <th>电话</th>
            <th>供应范围</th>
            <th>状态</th>
            <th>关联备件数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.name }}</td>
            <td>{{ row.contactName || '-' }}</td>
            <td>{{ row.phone || '-' }}</td>
            <td>{{ row.supplyScope || '-' }}</td>
            <td>{{ row.status === 'STOPPED' ? '已停用' : '合作中' }}</td>
            <td>{{ row.partCount || 0 }}</td>
            <td class="ops">
              <el-button v-if="isAdmin" type="text" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="isAdmin" type="text" style="color:#f56c6c" @click="remove(row)">删除</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无供应商</div>
    </div>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
                   :page-size="query.size" :current-page.sync="query.page" @current-change="load" />

    <el-dialog :title="form.id ? '编辑供应商' : '新增供应商'" :visible.sync="visible" width="560px">
      <el-form label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="供应范围"><el-input v-model="form.supplyScope" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="合作中" value="COOPERATING" />
            <el-option label="已停用" value="STOPPED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
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
      query: { page: 1, size: 10, name: '', status: '' },
      list: [],
      total: 0,
      visible: false,
      form: {}
    }
  },
  computed: {
    isAdmin() { return (getUser() || {}).role === 'ADMIN' }
  },
  created() { this.load() },
  methods: {
    async load() {
      const res = await http.get('/suppliers/page', { params: this.query })
      this.list = (res.data && res.data.records) || []
      this.total = (res.data && res.data.total) || 0
    },
    openEdit(row) {
      this.form = row ? { ...row } : { name: '', contactName: '', phone: '', address: '', supplyScope: '', status: 'COOPERATING', remark: '' }
      this.visible = true
    },
    async save() {
      await http.post('/suppliers', this.form)
      this.$message.success('保存成功')
      this.visible = false
      this.load()
    },
    remove(row) {
      this.$confirm('确认删除？', '提示').then(async () => {
        await http.delete('/suppliers/' + row.id)
        this.$message.success('已删除')
        this.load()
      }).catch(() => {})
    }
  }
}
</script>
