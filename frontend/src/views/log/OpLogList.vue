<template>
  <div class="page-card">
    <div class="filter-row">
      <el-input v-model="query.username" placeholder="用户名" clearable style="width:140px;margin-right:8px" />
      <el-input v-model="query.module" placeholder="模块" clearable style="width:140px;margin-right:8px" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <div class="table-wrap">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>时间</th>
            <th>用户</th>
            <th>角色</th>
            <th>模块</th>
            <th>动作</th>
            <th>详情</th>
            <th>IP</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.createTime }}</td>
            <td>{{ row.username }}</td>
            <td>{{ row.role }}</td>
            <td>{{ row.module }}</td>
            <td>{{ row.action }}</td>
            <td>{{ row.detail }}</td>
            <td>{{ row.ip }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无操作日志</div>
    </div>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
                   :page-size="query.size" :current-page.sync="query.page" @current-change="load" />
  </div>
</template>

<script>
import http from '../../utils/http'

export default {
  data() {
    return {
      query: { page: 1, size: 10, username: '', module: '' },
      list: [],
      total: 0
    }
  },
  created() { this.load() },
  methods: {
    async load() {
      const res = await http.get('/logs/page', { params: this.query })
      this.list = (res.data && res.data.records) || []
      this.total = (res.data && res.data.total) || 0
    }
  }
}
</script>
