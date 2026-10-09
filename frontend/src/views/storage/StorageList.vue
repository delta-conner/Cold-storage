<template>
  <div class="page-card">
    <div class="filter-row">
      <el-input v-model="query.name" placeholder="冷库名称" clearable style="width:180px;margin-right:8px" />
      <el-select v-model="query.status" clearable placeholder="状态" style="width:120px;margin-right:8px">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="isAdmin" type="success" @click="openEdit()">新增冷库</el-button>
    </div>
    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>名称</th>
            <th>地址</th>
            <th>联系人</th>
            <th>电话</th>
            <th>投用时间</th>
            <th>状态</th>
            <th>冷藏间数</th>
            <th>设备数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.name }}</td>
            <td>{{ row.address }}</td>
            <td>{{ row.contactName }}</td>
            <td>{{ row.contactPhone }}</td>
            <td>{{ row.commissionDate }}</td>
            <td>{{ row.status === 'ENABLED' ? '启用' : '停用' }}</td>
            <td>{{ row.roomCount }}</td>
            <td>{{ row.deviceCount }}</td>
            <td class="ops">
              <el-button type="text" @click="goDetail(row)">进入办事台</el-button>
              <el-button v-if="isAdmin" type="text" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="isAdmin" type="text" style="color:#f56c6c" @click="remove(row)">删除</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无冷库数据</div>
    </div>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
                   :page-size="query.size" :current-page.sync="query.page" @current-change="load" />

    <el-dialog :title="form.id ? '编辑冷库' : '新增冷库'" :visible.sync="visible" width="520px">
      <el-form label-width="90px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.contactPhone" /></el-form-item>
        <el-form-item label="投用时间">
          <el-date-picker v-model="form.commissionDate" type="date" value-format="yyyy-MM-dd" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
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
      const res = await http.get('/storages/page', { params: this.query })
      this.list = (res.data && res.data.records) || []
      this.total = (res.data && res.data.total) || 0
    },
    goDetail(row) {
      this.$router.push('/storages/' + row.id)
    },
    openEdit(row) {
      this.form = row ? { ...row } : { name: '', address: '', contactName: '', contactPhone: '', status: 'ENABLED', remark: '' }
      this.visible = true
    },
    async save() {
      await http.post('/storages', this.form)
      this.$message.success('保存成功')
      this.visible = false
      this.load()
    },
    remove(row) {
      this.$confirm('确认删除该冷库？', '提示').then(async () => {
        await http.delete('/storages/' + row.id)
        this.$message.success('已删除')
        this.load()
      }).catch(() => {})
    }
  }
}
</script>
