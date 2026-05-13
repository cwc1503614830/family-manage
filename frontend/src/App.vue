<template>
  <div v-if="!authReady" class="login-shell">
    <div class="login-card">
      <div class="brand login-brand">
        <div class="brand-mark">H</div>
        <div>
          <strong>HomeGuard</strong>
          <span>家庭资产</span>
        </div>
      </div>
      <p class="eyebrow">Family Asset Console</p>
      <h1>家庭管理登录</h1>
    </div>
  </div>

  <div v-else-if="!currentUser" class="login-shell">
    <form class="login-card" @submit.prevent="submitLogin">
      <div class="brand login-brand">
        <div class="brand-mark">H</div>
        <div>
          <strong>HomeGuard</strong>
          <span>家庭资产</span>
        </div>
      </div>
      <p class="eyebrow">Family Asset Console</p>
      <h1>家庭管理登录</h1>
      <label>
        <span>账号</span>
        <input v-model.trim="loginForm.username" autocomplete="username" required />
      </label>
      <label>
        <span>密码</span>
        <input v-model="loginForm.password" autocomplete="current-password" required type="password" />
      </label>
      <p v-if="loginError" class="form-error">{{ loginError }}</p>
      <button class="primary-btn" type="submit" :disabled="loginLoading">
        {{ loginLoading ? '登录中...' : '登录' }}
      </button>
      <span class="login-hint">默认账号：admin / admin123</span>
    </form>
  </div>

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">H</div>
        <div>
          <strong>HomeGuard</strong>
          <span>家庭资产</span>
        </div>
      </div>

      <nav class="nav-list">
        <button
          v-for="menu in visibleMenus"
          :key="menu.code"
          class="nav-item"
          :class="{ active: activeView === menu.code }"
          type="button"
          @click="switchView(menu.code)"
        >
          <span class="nav-icon">{{ menu.icon || '▦' }}</span>
          <span>{{ menu.name }}</span>
        </button>
      </nav>

      <div class="side-note">
        <b>{{ summary.lowStockAssets || 0 }}</b>
        <span>{{ currentUser.displayName || currentUser.username }}，欢迎回来</span>
        <button class="side-logout" type="button" @click="logoutUser">退出登录</button>
      </div>
    </aside>

    <main class="workspace">
      <section v-if="activeView === 'asset:view'">
        <section class="page-head">
          <div>
            <p class="eyebrow">Family Asset Console</p>
            <h1>家庭资产管理</h1>
            <p>管理家庭里的食品、日用品、家电和重要物品。</p>
          </div>
          <button v-if="can('asset:create')" class="primary-btn" type="button" @click="openCreate">
            <span>＋</span>
            <span>新增资产</span>
          </button>
        </section>

        <section class="stats-grid">
          <article class="stat-card">
            <strong>{{ summary.totalCategories || 0 }}</strong>
            <span>总分类</span>
          </article>
          <article class="stat-card danger">
            <strong>{{ summary.expiredAssets || 0 }}</strong>
            <span>已过期</span>
          </article>
          <article class="stat-card warning">
            <strong>{{ summary.lowStockAssets || 0 }}</strong>
            <span>库存不足</span>
          </article>
          <article class="stat-card calm">
            <strong>{{ summary.totalAssets || assets.length }}</strong>
            <span>登记资产</span>
          </article>
        </section>

        <section class="toolbar">
          <label class="search-box">
            <span>⌕</span>
            <input
              v-model.trim="query.keyword"
              type="search"
              placeholder="搜索物品名称、标签或位置..."
              @keyup.enter="loadAssets"
            />
          </label>

          <div class="filters">
            <select v-model="query.category" @change="loadAssets">
              <option value="">全部分类</option>
              <option v-for="category in categories" :key="category" :value="category">{{ category }}</option>
            </select>
            <select v-model="query.location" @change="loadAssets">
              <option value="">全部位置</option>
              <option v-for="location in locations" :key="location" :value="location">{{ location }}</option>
            </select>
            <button class="ghost-btn" type="button" @click="loadAssets">查询</button>
          </div>
        </section>

        <section class="asset-grid" :class="{ loading: loading }">
          <article v-for="asset in assets" :key="asset.id" class="asset-card">
            <div class="asset-image" :style="{ backgroundImage: `url(${assetImage(asset)})` }"></div>
            <div class="asset-body">
              <div class="asset-title-row">
                <h2>{{ asset.name }}</h2>
                <span class="pill">{{ asset.category || '未分类' }}</span>
              </div>
              <div class="meta-line">
                <span>⌖ {{ asset.location || '未设置位置' }}</span>
                <span :class="expireClass(asset)">{{ expireText(asset) }}</span>
              </div>

              <div class="qty-row">
                <button type="button" title="减少" :disabled="!can('asset:update')" @click="adjustStock(asset, -1)">−</button>
                <strong>{{ numberText(asset.quantity) }} {{ asset.unit || '件' }}</strong>
                <button type="button" title="增加" :disabled="!can('asset:update')" @click="adjustStock(asset, 1)">＋</button>
              </div>

              <div class="card-actions">
                <button class="restock-btn" type="button" :disabled="!can('asset:update')" @click="adjustStock(asset, Math.max(Number(asset.warningQuantity || 1), 1))">
                  <span>⊕</span>
                  <span>补货</span>
                </button>
                <button v-if="can('asset:update')" class="icon-btn" type="button" title="编辑" @click="openEdit(asset)">✎</button>
                <button v-if="can('asset:delete')" class="icon-btn danger-icon" type="button" title="删除" @click="removeAsset(asset)">×</button>
              </div>
            </div>
          </article>

          <div v-if="!assets.length && !loading" class="empty-state">
            <strong>还没有资产记录</strong>
            <span>点击右上角新增资产，开始建立家庭资产台账。</span>
          </div>
        </section>
      </section>

      <section v-if="activeView === 'system:user'" class="admin-page">
        <div class="page-head">
          <div>
            <p class="eyebrow">Access Control</p>
            <h1>用户管理</h1>
            <p>维护家庭成员账号，并分配可用角色。</p>
          </div>
          <button class="primary-btn" type="button" @click="openUserForm()">新增用户</button>
        </div>
        <div class="admin-table">
          <table>
            <thead>
              <tr>
                <th>账号</th>
                <th>姓名</th>
                <th>角色</th>
                <th>状态</th>
                <th>最后登录</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td>{{ user.username }}</td>
                <td>{{ user.display_name }}</td>
                <td>{{ user.roleNames || '-' }}</td>
                <td><span class="pill">{{ Number(user.status) === 1 ? '启用' : '停用' }}</span></td>
                <td>{{ formatDate(user.last_login_at) }}</td>
                <td>
                  <button class="ghost-btn mini" type="button" @click="openUserForm(user)">编辑</button>
                  <button class="ghost-btn mini danger-text" type="button" @click="removeUser(user)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-if="activeView === 'system:role'" class="admin-page">
        <div class="page-head">
          <div>
            <p class="eyebrow">Access Control</p>
            <h1>角色管理</h1>
            <p>通过角色统一分配菜单和操作权限。</p>
          </div>
          <button class="primary-btn" type="button" @click="openRoleForm()">新增角色</button>
        </div>
        <div class="admin-table">
          <table>
            <thead>
              <tr>
                <th>角色编码</th>
                <th>角色名称</th>
                <th>备注</th>
                <th>权限数</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="role in roles" :key="role.id">
                <td>{{ role.code }}</td>
                <td>{{ role.name }}</td>
                <td>{{ role.remark || '-' }}</td>
                <td>{{ (role.menuIds || []).length }}</td>
                <td>
                  <button class="ghost-btn mini" type="button" @click="openRoleForm(role)">编辑</button>
                  <button class="ghost-btn mini danger-text" type="button" @click="removeRole(role)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-if="activeView === 'system:menu'" class="admin-page">
        <div class="page-head">
          <div>
            <p class="eyebrow">Access Control</p>
            <h1>菜单权限</h1>
            <p>配置侧边栏菜单和按钮级权限编码。</p>
          </div>
          <button class="primary-btn" type="button" @click="openMenuForm()">新增权限</button>
        </div>
        <div class="admin-table">
          <table>
            <thead>
              <tr>
                <th>名称</th>
                <th>编码</th>
                <th>图标</th>
                <th>排序</th>
                <th>菜单可见</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="menu in allMenus" :key="menu.id">
                <td>{{ menu.name }}</td>
                <td>{{ menu.code }}</td>
                <td>{{ menu.icon || '-' }}</td>
                <td>{{ menu.sort_no }}</td>
                <td><span class="pill">{{ Number(menu.visible) === 1 ? '显示' : '隐藏' }}</span></td>
                <td>
                  <button class="ghost-btn mini" type="button" @click="openMenuForm(menu)">编辑</button>
                  <button class="ghost-btn mini danger-text" type="button" @click="removeMenu(menu)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeDialog">
      <form class="asset-form" @submit.prevent="submitForm">
        <div class="form-head">
          <div>
            <p class="eyebrow">{{ editingId ? 'Edit Asset' : 'New Asset' }}</p>
            <h2>{{ editingId ? '编辑资产' : '新增资产' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeDialog">×</button>
        </div>

        <div class="form-grid">
          <label>
            <span>资产名称</span>
            <input v-model.trim="form.name" required placeholder="例如：全脂鲜牛奶" />
          </label>
          <label>
            <span>分类</span>
            <input v-model.trim="form.category" required list="category-list" placeholder="生鲜食品" />
          </label>
          <label>
            <span>存放位置</span>
            <input v-model.trim="form.location" list="location-list" placeholder="冰箱 / 储物间" />
          </label>
          <label>
            <span>单位</span>
            <input v-model.trim="form.unit" required placeholder="盒 / 瓶 / 件" />
          </label>
          <label>
            <span>数量</span>
            <input v-model.number="form.quantity" required min="0" step="0.01" type="number" />
          </label>
          <label>
            <span>低库存阈值</span>
            <input v-model.number="form.warningQuantity" required min="0" step="0.01" type="number" />
          </label>
          <label>
            <span>购入日期</span>
            <input v-model="form.purchaseDate" type="date" />
          </label>
          <label>
            <span>到期日期</span>
            <input v-model="form.expireDate" type="date" />
          </label>
          <label class="wide">
            <span>图片地址</span>
            <input v-model.trim="form.imageUrl" placeholder="https://..." />
          </label>
          <label class="wide">
            <span>标签</span>
            <input v-model.trim="form.tags" placeholder="早餐,冷藏,补货" />
          </label>
          <label class="wide">
            <span>备注</span>
            <textarea v-model.trim="form.remark" rows="3" placeholder="记录保养、采购或使用提醒"></textarea>
          </label>
        </div>

        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closeDialog">取消</button>
          <button class="primary-btn" type="submit">{{ editingId ? '保存修改' : '创建资产' }}</button>
        </div>
      </form>
    </div>

    <div v-if="userDialogVisible" class="modal-mask" @click.self="closeUserForm">
      <form class="asset-form" @submit.prevent="submitUser">
        <div class="form-head">
          <div>
            <p class="eyebrow">User</p>
            <h2>{{ editingUserId ? '编辑用户' : '新增用户' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeUserForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>账号</span>
            <input v-model.trim="userForm.username" required />
          </label>
          <label>
            <span>{{ editingUserId ? '新密码（可空）' : '初始密码' }}</span>
            <input v-model="userForm.password" :required="!editingUserId" type="password" />
          </label>
          <label>
            <span>姓名</span>
            <input v-model.trim="userForm.displayName" required />
          </label>
          <label>
            <span>状态</span>
            <select v-model.number="userForm.status">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </label>
          <label class="wide">
            <span>角色</span>
            <div class="check-grid">
              <label v-for="role in roles" :key="role.id">
                <input v-model="userForm.roleIds" type="checkbox" :value="role.id" />
                <span>{{ role.name }}</span>
              </label>
            </div>
          </label>
        </div>
        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closeUserForm">取消</button>
          <button class="primary-btn" type="submit">保存</button>
        </div>
      </form>
    </div>

    <div v-if="roleDialogVisible" class="modal-mask" @click.self="closeRoleForm">
      <form class="asset-form" @submit.prevent="submitRole">
        <div class="form-head">
          <div>
            <p class="eyebrow">Role</p>
            <h2>{{ editingRoleId ? '编辑角色' : '新增角色' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeRoleForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>角色编码</span>
            <input v-model.trim="roleForm.code" required placeholder="admin" />
          </label>
          <label>
            <span>角色名称</span>
            <input v-model.trim="roleForm.name" required placeholder="家庭管理员" />
          </label>
          <label class="wide">
            <span>备注</span>
            <textarea v-model.trim="roleForm.remark" rows="2"></textarea>
          </label>
          <label class="wide">
            <span>权限</span>
            <div class="check-grid permission-grid">
              <label v-for="menu in allMenus" :key="menu.id">
                <input v-model="roleForm.menuIds" type="checkbox" :value="menu.id" />
                <span>{{ menu.name }} · {{ menu.code }}</span>
              </label>
            </div>
          </label>
        </div>
        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closeRoleForm">取消</button>
          <button class="primary-btn" type="submit">保存</button>
        </div>
      </form>
    </div>

    <div v-if="menuDialogVisible" class="modal-mask" @click.self="closeMenuForm">
      <form class="asset-form" @submit.prevent="submitMenu">
        <div class="form-head">
          <div>
            <p class="eyebrow">Permission</p>
            <h2>{{ editingMenuId ? '编辑权限' : '新增权限' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeMenuForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>名称</span>
            <input v-model.trim="menuForm.name" required />
          </label>
          <label>
            <span>编码</span>
            <input v-model.trim="menuForm.code" required placeholder="asset:view" />
          </label>
          <label>
            <span>图标</span>
            <input v-model.trim="menuForm.icon" placeholder="▦" />
          </label>
          <label>
            <span>排序</span>
            <input v-model.number="menuForm.sortNo" min="0" type="number" />
          </label>
          <label class="wide checkbox-line">
            <input v-model="menuForm.visible" type="checkbox" />
            <span>作为侧边栏菜单显示</span>
          </label>
        </div>
        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closeMenuForm">取消</button>
          <button class="primary-btn" type="submit">保存</button>
        </div>
      </form>
    </div>

    <datalist id="category-list">
      <option v-for="category in categories" :key="category" :value="category"></option>
    </datalist>
    <datalist id="location-list">
      <option v-for="location in locations" :key="location" :value="location"></option>
    </datalist>
  </div>
</template>

<script>
import {
  changeStock,
  createAsset,
  deleteAsset,
  getOptions,
  getSummary,
  listAssets,
  updateAsset
} from './api/assets'
import { currentUser as fetchCurrentUser, login, logout, setAuthToken } from './api/auth'
import {
  createMenu,
  createRole,
  createUser,
  deleteMenu,
  deleteRole,
  deleteUser,
  listMenus,
  listRoles,
  listUsers,
  updateMenu,
  updateRole,
  updateUser
} from './api/system'

const demoAssets = [
  {
    id: 'demo-1',
    name: '全脂鲜牛奶',
    category: '生鲜食品',
    location: '冰箱',
    quantity: 3,
    unit: '盒',
    warningQuantity: 2,
    expireDate: nextDate(7),
    imageUrl: 'https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=900&q=80'
  },
  {
    id: 'demo-2',
    name: '特级生抽',
    category: '调味品',
    location: '厨房橱柜',
    quantity: 1,
    unit: '瓶',
    warningQuantity: 1,
    expireDate: nextDate(550),
    imageUrl: 'https://images.unsplash.com/photo-1472476443507-c7a5948772fc?auto=format&fit=crop&w=900&q=80'
  },
  {
    id: 'demo-3',
    name: '卷纸',
    category: '个护厨卫',
    location: '储物间',
    quantity: 12,
    unit: '卷',
    warningQuantity: 6,
    expireDate: '',
    imageUrl: 'https://images.unsplash.com/photo-1583947581924-a31b1d6bb176?auto=format&fit=crop&w=900&q=80'
  },
  {
    id: 'demo-4',
    name: '意大利面',
    category: '生鲜食品',
    location: '储物间',
    quantity: 0,
    unit: '袋',
    warningQuantity: 2,
    expireDate: nextDate(232),
    imageUrl: 'https://images.unsplash.com/photo-1551462147-ff29053bfc14?auto=format&fit=crop&w=900&q=80'
  }
]

function nextDate(days) {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}

function emptyForm() {
  return {
    name: '',
    category: '',
    location: '',
    quantity: 1,
    unit: '件',
    warningQuantity: 1,
    purchaseDate: new Date().toISOString().slice(0, 10),
    expireDate: '',
    imageUrl: '',
    tags: '',
    remark: ''
  }
}

function emptyUserForm() {
  return {
    username: '',
    password: '',
    displayName: '',
    status: 1,
    roleIds: []
  }
}

function emptyRoleForm() {
  return {
    code: '',
    name: '',
    remark: '',
    menuIds: []
  }
}

function emptyMenuForm() {
  return {
    name: '',
    code: '',
    icon: '',
    sortNo: 100,
    visible: true
  }
}

export default {
  name: 'App',
  data() {
    return {
      authReady: false,
      currentUser: null,
      permissions: [],
      visibleMenus: [],
      activeView: 'asset:view',
      loginForm: {
        username: 'admin',
        password: 'admin123'
      },
      loginError: '',
      loginLoading: false,
      assets: [],
      summary: {},
      categories: ['生鲜食品', '调味品', '个护厨卫', '家电设备', '证件票据'],
      locations: ['冰箱', '厨房橱柜', '储物间', '卧室', '客厅'],
      query: {
        keyword: '',
        category: '',
        location: '',
        page: 1,
        size: 50
      },
      dialogVisible: false,
      editingId: null,
      form: emptyForm(),
      loading: false,
      offlineMode: false,
      users: [],
      roles: [],
      allMenus: [],
      userDialogVisible: false,
      roleDialogVisible: false,
      menuDialogVisible: false,
      editingUserId: null,
      editingRoleId: null,
      editingMenuId: null,
      userForm: emptyUserForm(),
      roleForm: emptyRoleForm(),
      menuForm: emptyMenuForm()
    }
  },
  created() {
    this.restoreSession()
  },
  methods: {
    async restoreSession() {
      const token = window.localStorage.getItem('family-manage-token')
      if (!token) {
        this.authReady = true
        return
      }
      setAuthToken(token)
      try {
        const session = await fetchCurrentUser()
        this.applySession(session)
      } catch (error) {
        window.localStorage.removeItem('family-manage-token')
        setAuthToken('')
      } finally {
        this.authReady = true
      }
    },
    async submitLogin() {
      this.loginLoading = true
      this.loginError = ''
      try {
        const session = await login(this.loginForm)
        window.localStorage.setItem('family-manage-token', session.token)
        setAuthToken(session.token)
        this.applySession(session)
      } catch (error) {
        this.loginError = error.message || '登录失败'
      } finally {
        this.loginLoading = false
      }
    },
    applySession(session) {
      this.currentUser = session.user
      this.permissions = session.permissions || []
      this.visibleMenus = session.menus || []
      this.activeView = this.visibleMenus.length ? this.visibleMenus[0].code : ''
      if (this.can('asset:view')) {
        this.activeView = 'asset:view'
        this.bootstrap()
      }
    },
    async logoutUser() {
      try {
        await logout()
      } catch (error) {
        // 本地退出不依赖服务端结果。
      }
      window.localStorage.removeItem('family-manage-token')
      setAuthToken('')
      this.currentUser = null
      this.permissions = []
      this.visibleMenus = []
    },
    can(code) {
      return this.permissions.includes(code)
    },
    async switchView(code) {
      this.activeView = code
      if (code === 'asset:view') {
        await this.bootstrap()
      }
      if (code === 'system:user') {
        await Promise.all([this.loadUsers(), this.loadRoles()])
      }
      if (code === 'system:role') {
        await Promise.all([this.loadRoles(), this.loadMenus()])
      }
      if (code === 'system:menu') {
        await this.loadMenus()
      }
    },
    async bootstrap() {
      await Promise.all([this.loadAssets(), this.loadSummary(), this.loadOptions()])
    },
    async loadAssets() {
      this.loading = true
      try {
        const page = await listAssets(this.query)
        this.assets = page.records || []
        this.offlineMode = false
      } catch (error) {
        this.offlineMode = true
        const keyword = this.query.keyword
        this.assets = demoAssets.filter(asset => {
          const text = `${asset.name}${asset.category}${asset.location}${asset.tags || ''}`
          return (!keyword || text.includes(keyword)) &&
            (!this.query.category || asset.category === this.query.category) &&
            (!this.query.location || asset.location === this.query.location)
        })
      } finally {
        this.loading = false
      }
    },
    async loadSummary() {
      try {
        this.summary = await getSummary()
      } catch (error) {
        const categories = new Set(demoAssets.map(item => item.category))
        this.summary = {
          totalCategories: categories.size,
          totalAssets: demoAssets.length,
          expiredAssets: 0,
          lowStockAssets: demoAssets.filter(item => Number(item.quantity) <= Number(item.warningQuantity)).length
        }
      }
    },
    async loadOptions() {
      try {
        const options = await getOptions()
        this.categories = mergeOptions(this.categories, options.categories)
        this.locations = mergeOptions(this.locations, options.locations)
      } catch (error) {
        this.categories = mergeOptions(this.categories, demoAssets.map(item => item.category))
        this.locations = mergeOptions(this.locations, demoAssets.map(item => item.location))
      }
    },
    openCreate() {
      this.editingId = null
      this.form = emptyForm()
      this.dialogVisible = true
    },
    openEdit(asset) {
      this.editingId = asset.id
      this.form = { ...emptyForm(), ...asset }
      this.dialogVisible = true
    },
    closeDialog() {
      this.dialogVisible = false
    },
    async submitForm() {
      const payload = { ...this.form }
      if (this.offlineMode) {
        this.applyLocalSave(payload)
        this.closeDialog()
        return
      }
      if (this.editingId) {
        await updateAsset(this.editingId, payload)
      } else {
        await createAsset(payload)
      }
      this.closeDialog()
      await this.bootstrap()
    },
    async adjustStock(asset, delta) {
      if (!this.can('asset:update')) {
        return
      }
      if (this.offlineMode || String(asset.id).startsWith('demo-')) {
        asset.quantity = Math.max(Number(asset.quantity || 0) + delta, 0)
        this.loadSummary()
        return
      }
      await changeStock(asset.id, delta)
      await this.bootstrap()
    },
    async removeAsset(asset) {
      if (!window.confirm(`确认删除「${asset.name}」吗？`)) {
        return
      }
      if (this.offlineMode || String(asset.id).startsWith('demo-')) {
        this.assets = this.assets.filter(item => item.id !== asset.id)
        return
      }
      await deleteAsset(asset.id)
      await this.bootstrap()
    },
    applyLocalSave(payload) {
      if (this.editingId) {
        this.assets = this.assets.map(item => item.id === this.editingId ? { ...item, ...payload } : item)
      } else {
        this.assets.unshift({ ...payload, id: `local-${Date.now()}` })
      }
    },
    async loadUsers() {
      this.users = await listUsers()
    },
    async loadRoles() {
      this.roles = await listRoles()
    },
    async loadMenus() {
      this.allMenus = await listMenus()
    },
    async openUserForm(user) {
      await this.loadRoles()
      this.editingUserId = user ? user.id : null
      this.userForm = user
        ? {
            username: user.username,
            password: '',
            displayName: user.display_name,
            status: Number(user.status),
            roleIds: [...(user.roleIds || [])]
          }
        : emptyUserForm()
      this.userDialogVisible = true
    },
    closeUserForm() {
      this.userDialogVisible = false
    },
    async submitUser() {
      if (this.editingUserId) {
        await updateUser(this.editingUserId, this.userForm)
      } else {
        await createUser(this.userForm)
      }
      this.closeUserForm()
      await this.loadUsers()
    },
    async removeUser(user) {
      if (window.confirm(`确认删除用户「${user.username}」吗？`)) {
        await deleteUser(user.id)
        await this.loadUsers()
      }
    },
    async openRoleForm(role) {
      await this.loadMenus()
      this.editingRoleId = role ? role.id : null
      this.roleForm = role
        ? {
            code: role.code,
            name: role.name,
            remark: role.remark || '',
            menuIds: [...(role.menuIds || [])]
          }
        : emptyRoleForm()
      this.roleDialogVisible = true
    },
    closeRoleForm() {
      this.roleDialogVisible = false
    },
    async submitRole() {
      if (this.editingRoleId) {
        await updateRole(this.editingRoleId, this.roleForm)
      } else {
        await createRole(this.roleForm)
      }
      this.closeRoleForm()
      await this.loadRoles()
    },
    async removeRole(role) {
      if (window.confirm(`确认删除角色「${role.name}」吗？`)) {
        await deleteRole(role.id)
        await this.loadRoles()
      }
    },
    openMenuForm(menu) {
      this.editingMenuId = menu ? menu.id : null
      this.menuForm = menu
        ? {
            name: menu.name,
            code: menu.code,
            icon: menu.icon || '',
            sortNo: Number(menu.sort_no || 100),
            visible: Number(menu.visible) === 1
          }
        : emptyMenuForm()
      this.menuDialogVisible = true
    },
    closeMenuForm() {
      this.menuDialogVisible = false
    },
    async submitMenu() {
      if (this.editingMenuId) {
        await updateMenu(this.editingMenuId, this.menuForm)
      } else {
        await createMenu(this.menuForm)
      }
      this.closeMenuForm()
      await this.loadMenus()
    },
    async removeMenu(menu) {
      if (window.confirm(`确认删除权限「${menu.name}」吗？`)) {
        await deleteMenu(menu.id)
        await this.loadMenus()
      }
    },
    formatDate(value) {
      return value ? String(value).replace('T', ' ') : '-'
    },
    assetImage(asset) {
      return asset.imageUrl || 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=900&q=80'
    },
    numberText(value) {
      const number = Number(value || 0)
      return Number.isInteger(number) ? String(number) : number.toFixed(2)
    },
    expireText(asset) {
      if (!asset.expireDate) {
        return '长期可用'
      }
      const today = new Date()
      const expire = new Date(asset.expireDate)
      const diff = Math.ceil((expire - today) / 86400000)
      if (diff < 0) {
        return `已过期 ${Math.abs(diff)} 天`
      }
      if (diff === 0) {
        return '今天到期'
      }
      return `${diff} 天后过期`
    },
    expireClass(asset) {
      if (!asset.expireDate) {
        return 'fresh'
      }
      const diff = Math.ceil((new Date(asset.expireDate) - new Date()) / 86400000)
      if (diff < 0) {
        return 'expired'
      }
      if (diff <= 30) {
        return 'soon'
      }
      return 'fresh'
    }
  }
}

function mergeOptions(defaults, values) {
  return Array.from(new Set([...(defaults || []), ...(values || [])].filter(Boolean)))
}
</script>
