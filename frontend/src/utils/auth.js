const TOKEN_KEY = 'token'
const USER_KEY = 'user'

/** 使用 sessionStorage：每个浏览器标签页独立登录，互不覆盖 */
function store() {
  return window.sessionStorage
}

export function getToken() {
  return store().getItem(TOKEN_KEY)
}

export function getUser() {
  try {
    return JSON.parse(store().getItem(USER_KEY) || 'null')
  } catch (e) {
    return null
  }
}

export function setAuth(payload) {
  store().setItem(TOKEN_KEY, payload.token)
  store().setItem(USER_KEY, JSON.stringify({
    userId: payload.userId,
    username: payload.username,
    realName: payload.realName,
    role: payload.role,
    phone: payload.phone,
    roomIds: payload.roomIds || []
  }))
  // 清掉旧的 localStorage，避免和历史实现串号
  try {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  } catch (e) { /* ignore */ }
}

export function setUser(user) {
  store().setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuth() {
  store().removeItem(TOKEN_KEY)
  store().removeItem(USER_KEY)
}

export function roleLabel(role) {
  return ({ ADMIN: '管理员', OPS: '运维人员', CLIENT: '甲方用户' })[role] || role
}

export function statusLabel(status) {
  return ({
    PENDING: '待受理',
    ASSIGNED: '已派单',
    PROCESSING: '处理中',
    ACCEPTING: '待验收',
    DONE: '已完成',
    EVALUATED: '已评价',
    ARCHIVED: '已归档',
    CLOSED: '已关闭'
  })[status] || status
}
