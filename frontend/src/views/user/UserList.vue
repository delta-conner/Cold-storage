<template>
  <div class="page-card">
    <div class="filter-row">
      <el-input v-model="query.username" placeholder="用户名" clearable style="width:160px;margin-right:8px" />
      <el-select v-model="query.role" clearable placeholder="角色" style="width:140px;margin-right:8px">
        <el-option label="管理员" value="ADMIN" />
        <el-option label="运维" value="OPS" />
        <el-option label="甲方" value="CLIENT" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openEdit()">新增用户</el-button>
    </div>
    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>姓名</th>
            <th>电话</th>
            <th>角色</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.id }}</td>
            <td>{{ row.username }}</td>
            <td>{{ row.realName }}</td>
            <td>{{ row.phone }}</td>
            <td>{{ roleLabel(row.role) }}</td>
            <td>{{ row.status === 0 ? '禁用' : '启用' }}</td>
            <td class="ops">
              <el-button type="text" @click="openEdit(row)">编辑</el-button>
              <el-button type="text" @click="toggleStatus(row)">{{ row.status === 0 ? '启用' : '禁用' }}</el-button>
              <el-button type="text" style="color:#f56c6c" @click="remove(row)">删除</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无用户数据</div>
    </div>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total" :page-size="query.size" :current-page.sync="query.page" @current-change="load" />

    <el-dialog :title="form.id ? '编辑用户' : '新增用户'" :visible.sync="visible" width="520px">
      <el-form label-width="100px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" show-password :placeholder="form.id ? '不修改请留空' : '默认123456'" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width:100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="运维" value="OPS" />
            <el-option label="甲方" value="CLIENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.role==='CLIENT'" label="绑定冷藏间">
          <el-select v-model="form.roomIds" multiple style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
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
import { roleLabel } from '../../utils/auth'

export default {
  data() {
    return {
      query: { page: 1, size: 10, username: '', role: '' },
      list: [],
      total: 0,
      rooms: [],
      visible: false,
      form: { roomIds: [] }
    }
  },
  created() {
    this.loadRooms()
    this.load()
  },
  methods: {
    roleLabel,
    async loadRooms() {
      const res = await http.get('/rooms/list')
      this.rooms = (res.data || []).filter(r => r.code !== 'PUB')
    },
    async load() {
      const res = await http.get('/users/page', { params: this.query })
      this.list = res.data.records || []
      this.total = res.data.total || 0
    },
    async openEdit(row) {
      if (row) {
        const res = await http.get('/users/' + row.id)
        this.form = {
          id: res.data.user.id,
          username: res.data.user.username,
          realName: res.data.user.realName,
          phone: res.data.user.phone,
          role: res.data.user.role,
          status: res.data.user.status == null ? 1 : res.data.user.status,
          password: '',
          roomIds: res.data.roomIds || []
        }
      } else {
        this.form = { username: '', password: '', realName: '', phone: '', role: 'CLIENT', status: 1, roomIds: [] }
      }
      this.visible = true
    },
    async save() {
      await http.post('/users', this.form)
      this.$message.success('保存成功')
      this.visible = false
      this.load()
    },
    async toggleStatus(row) {
      const status = row.status === 0 ? 1 : 0
      await http.post('/users/' + row.id + '/status', { status })
      this.$message.success('已更新状态')
      this.load()
    },
    remove(row) {
      this.$confirm('确认删除该用户？', '提示').then(async () => {
        await http.delete('/users/' + row.id)
        this.$message.success('已删除')
        this.load()
      }).catch(() => {})
    }
  }
}
</script>
