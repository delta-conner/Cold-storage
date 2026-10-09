<template>
  <div class="page-card">
    <div class="filter-row">
      <el-input v-model="query.name" placeholder="冷藏间名称" clearable style="width:180px;margin-right:8px" />
      <el-select v-model="query.storageId" clearable placeholder="所属冷库" style="width:180px;margin-right:8px">
        <el-option v-for="s in storages" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="isAdmin" type="success" @click="openEdit()">新增冷藏间</el-button>
    </div>
    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>ID</th>
            <th>所属冷库</th>
            <th>冷藏间名称</th>
            <th>编码</th>
            <th>状态</th>
            <th>温度范围</th>
            <th>容积</th>
            <th>备注</th>
            <th v-if="isAdmin">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.id }}</td>
            <td>{{ row.storageName }}</td>
            <td>{{ row.name }}</td>
            <td>{{ row.code }}</td>
            <td>{{ row.status === 'DISABLED' ? '停用' : '启用' }}</td>
            <td>{{ row.tempMin != null ? (row.tempMin + ' ~ ' + row.tempMax + '℃') : '-' }}</td>
            <td>{{ row.volume != null ? row.volume : '-' }}</td>
            <td>{{ row.remark }}</td>
            <td v-if="isAdmin" class="ops">
              <el-button type="text" @click="openEdit(row)">编辑</el-button>
              <el-button type="text" style="color:#f56c6c" @click="remove(row)">删除</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无冷藏间数据</div>
    </div>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total" :page-size="query.size" :current-page.sync="query.page" @current-change="load" />

    <el-dialog :title="form.id ? '编辑冷藏间' : '新增冷藏间'" :visible.sync="visible" width="480px">
      <el-form label-width="90px">
        <el-form-item label="所属冷库">
          <el-select v-model="form.storageId" style="width:100%">
            <el-option v-for="s in storages" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="温度下限"><el-input-number v-model="form.tempMin" :step="0.5" /></el-form-item>
        <el-form-item label="温度上限"><el-input-number v-model="form.tempMax" :step="0.5" /></el-form-item>
        <el-form-item label="容积"><el-input-number v-model="form.volume" :min="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
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
      query: { page: 1, size: 10, name: '', storageId: null },
      list: [],
      total: 0,
      storages: [],
      visible: false,
      form: {}
    }
  },
  computed: {
    isAdmin() {
      return (getUser() || {}).role === 'ADMIN'
    }
  },
  created() {
    if (this.$route.query.storageId) {
      this.query.storageId = Number(this.$route.query.storageId)
    }
    this.loadStorages()
    this.load()
  },
  methods: {
    async loadStorages() {
      const res = await http.get('/storages/list')
      this.storages = res.data || []
    },
    async load() {
      const res = await http.get('/rooms/page', { params: this.query })
      this.list = res.data.records || []
      this.total = res.data.total || 0
    },
    openEdit(row) {
      this.form = row ? { ...row } : { storageId: this.storages[0] && this.storages[0].id, name: '', code: '', status: 'ENABLED', remark: '' }
      this.visible = true
    },
    async save() {
      await http.post('/rooms', this.form)
      this.$message.success('保存成功')
      this.visible = false
      this.load()
    },
    remove(row) {
      this.$confirm('确认删除该冷藏间？', '提示').then(async () => {
        await http.delete('/rooms/' + row.id)
        this.$message.success('已删除')
        this.load()
      }).catch(() => {})
    }
  }
}
</script>
