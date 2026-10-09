import Vue from 'vue'
import Router from 'vue-router'
import { getUser, getToken } from '../utils/auth'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import DeviceList from '../views/device/DeviceList.vue'
import DeviceHealth from '../views/device/DeviceHealth.vue'
import ColdRoomList from '../views/room/ColdRoomList.vue'
import OrderList from '../views/order/OrderList.vue'
import Profile from '../views/Profile.vue'
import UserList from '../views/user/UserList.vue'
import FaultList from '../views/fault/FaultList.vue'
import FaultCaseList from '../views/fault/FaultCaseList.vue'
import MaintainList from '../views/maintain/MaintainList.vue'
import MaintainPlanList from '../views/maintain/MaintainPlanList.vue'
import MaintainTaskList from '../views/maintain/MaintainTaskList.vue'
import SupplierList from '../views/spare/SupplierList.vue'
import SparePartList from '../views/spare/SparePartList.vue'
import StatsView from '../views/stats/StatsView.vue'
import OpsDashboard from '../views/dashboard/OpsDashboard.vue'
import StorageList from '../views/storage/StorageList.vue'
import StorageDetail from '../views/storage/StorageDetail.vue'
import OpLogList from '../views/log/OpLogList.vue'
import MessageList from '../views/message/MessageList.vue'

Vue.use(Router)

const router = new Router({
  mode: 'hash',
  routes: [
    { path: '/login', component: Login },
    {
      path: '/',
      component: Layout,
      redirect: '/orders',
      children: [
        { path: 'dashboard', component: OpsDashboard, meta: { roles: ['OPS', 'ADMIN'], title: '运维工作台' } },
        { path: 'storages', component: StorageList, meta: { roles: ['ADMIN', 'OPS', 'CLIENT'], title: '冷库管理' } },
        { path: 'storages/:id', component: StorageDetail, meta: { roles: ['ADMIN', 'OPS', 'CLIENT'], title: '库区详情' } },
        { path: 'rooms', component: ColdRoomList, meta: { roles: ['ADMIN', 'OPS', 'CLIENT'], title: '冷藏间' } },
        { path: 'devices', component: DeviceList, meta: { roles: ['ADMIN', 'OPS', 'CLIENT'], title: '设备台账' } },
        { path: 'device-health', component: DeviceHealth, meta: { roles: ['ADMIN', 'OPS'], title: '设备健康分析' } },
        { path: 'orders', component: OrderList, meta: { roles: ['ADMIN', 'OPS', 'CLIENT'], title: '运维工单' } },
        { path: 'users', component: UserList, meta: { roles: ['ADMIN'], title: '用户管理' } },
        { path: 'logs', component: OpLogList, meta: { roles: ['ADMIN'], title: '操作日志' } },
        { path: 'profile', component: Profile, meta: { roles: ['ADMIN', 'OPS', 'CLIENT'], title: '个人中心' } },
        { path: 'faults', component: FaultList, meta: { roles: ['ADMIN', 'OPS'], title: '故障记录' } },
        { path: 'fault-cases', component: FaultCaseList, meta: { roles: ['ADMIN', 'OPS'], title: '故障案例库' } },
        { path: 'maintain-plans', component: MaintainPlanList, meta: { roles: ['ADMIN', 'OPS'], title: '维保计划' } },
        { path: 'maintain-tasks', component: MaintainTaskList, meta: { roles: ['ADMIN', 'OPS'], title: '维保任务' } },
        { path: 'maintain', component: MaintainList, meta: { roles: ['ADMIN', 'OPS'], title: '维保周期提醒' } },
        { path: 'suppliers', component: SupplierList, meta: { roles: ['ADMIN', 'OPS'], title: '供应商' } },
        { path: 'spare-parts', component: SparePartList, meta: { roles: ['ADMIN', 'OPS'], title: '备件库存' } },
        { path: 'stats', component: StatsView, meta: { roles: ['ADMIN'], title: '数据统计' } },
        { path: 'messages', component: MessageList, meta: { roles: ['ADMIN', 'OPS', 'CLIENT'], title: '消息中心' } }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  if (to.path === '/login') {
    next()
    return
  }
  const user = getUser()
  if (!user || !getToken()) {
    next('/login')
    return
  }
  const roles = (to.meta && to.meta.roles) || []
  if (roles.length && !roles.includes(user.role)) {
    next('/orders')
    return
  }
  next()
})

export default router
