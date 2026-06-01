<template>
  <LoginView
    v-if="!authReady || !currentUser"
    :auth-ready="authReady"
    :loading="loginLoading"
    :error="loginError"
    @login="submitLogin"
  />

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
        <div v-for="menu in visibleMenus" :key="menu.code" class="nav-group">
          <button
            class="nav-item"
            :class="{ active: activeView === menu.code, open: menu.children && menu.children.length && menuGroups[menu.code] }"
            type="button"
            @click="handleMenuClick(menu)"
          >
            <span class="nav-icon">{{ menu.icon || '▦' }}</span>
            <span>{{ menu.name }}</span>
            <span v-if="menu.children && menu.children.length" class="nav-caret">⌄</span>
          </button>
          <div v-if="menu.children && menu.children.length && menuGroups[menu.code]" class="nav-children">
            <button
              v-for="child in menu.children"
              :key="child.code"
              class="nav-item child"
              :class="{ active: activeView === child.code }"
              type="button"
              @click="switchView(child.code)"
            >
              <span class="nav-icon">{{ child.icon || '•' }}</span>
              <span>{{ child.name }}</span>
            </button>
          </div>
        </div>
      </nav>

      <div class="side-note">
        <b>{{ summary.lowStockAssets || 0 }}</b>
        <span>{{ currentUser.displayName || currentUser.username }}，欢迎回来</span>
        <button class="side-logout" type="button" @click="logoutUser">退出登录</button>
      </div>
    </aside>

    <main class="workspace">
      <component
        :is="currentViewComponent"
        @summary-change="summary = $event"
        @refresh-summary="loadSummary"
      />
    </main>

    <nav class="mobile-bottom-tabs">
      <div ref="mobileTabsScroll" class="mobile-bottom-tabs-scroll">
        <button
          v-for="tab in mobileTabItems"
          :key="tab.code"
          class="tab-btn"
          :class="{ active: activeView === tab.code }"
          type="button"
          @click="switchView(tab.code)"
        >
          <span class="tab-icon">{{ tab.icon }}</span>
          <span class="tab-label">{{ tab.name }}</span>
        </button>
      </div>
    </nav>
  </div>
</template>

<script>
import { getSummary } from './api/assets'
import { currentUser as fetchCurrentUser, login, logout, setAuthToken } from './api/auth'
import AssetView from './views/AssetView.vue'
import ClothesItemView from './views/ClothesItemView.vue'
import ClothesLocationView from './views/ClothesLocationView.vue'
import ClothesTypeView from './views/ClothesTypeView.vue'
import LoginView from './views/LoginView.vue'
import MenuManageView from './views/MenuManageView.vue'
import RoleManageView from './views/RoleManageView.vue'
import UserManageView from './views/UserManageView.vue'
import WealthOverviewView from './views/WealthOverviewView.vue'
import WealthTypeView from './views/WealthTypeView.vue'
import TravelWuhanView from './views/TravelWuhanView.vue'
import TravelDomesticView from './views/TravelDomesticView.vue'

const viewMap = {
  'asset:view': AssetView,
  'clothes:item': ClothesItemView,
  'clothes:type': ClothesTypeView,
  'clothes:location': ClothesLocationView,
  'wealth:overview': WealthOverviewView,
  'wealth:type': WealthTypeView,
  'travel:wuhan': TravelWuhanView,
  'travel:domestic': TravelDomesticView,
  'system:user': UserManageView,
  'system:role': RoleManageView,
  'system:menu': MenuManageView
}

export default {
  name: 'App',
  components: {
    LoginView,
    AssetView,
    ClothesItemView,
    ClothesTypeView,
    ClothesLocationView,
    WealthOverviewView,
    WealthTypeView,
    TravelWuhanView,
    TravelDomesticView,
    UserManageView,
    RoleManageView,
    MenuManageView
  },
  data() {
    return {
      authReady: false,
      currentUser: null,
      permissions: [],
      visibleMenus: [],
      menuGroups: {},
      activeView: 'asset:view',
      loginError: '',
      loginLoading: false,
      summary: {}
    }
  },
  computed: {
    currentViewComponent() {
      return viewMap[this.activeView] || AssetView
    },
    mobileTabItems() {
      if (!this.currentUser) {
        return []
      }
      return buildMobileTabItems(this.visibleMenus)
    }
  },
  watch: {
    activeView() {
      this.$nextTick(() => this.scrollActiveMobileTabIntoView())
    }
  },
  created() {
    this.restoreSession()
  },
  updated() {
    this.scrollActiveMobileTabIntoView()
  },
  methods: {
    scrollActiveMobileTabIntoView() {
      const scroll = this.$refs.mobileTabsScroll
      const active = scroll && scroll.querySelector('.tab-btn.active')
      if (scroll && active) {
        active.scrollIntoView({ inline: 'center', block: 'nearest', behavior: 'smooth' })
      }
    },
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
    async submitLogin(loginForm) {
      this.loginLoading = true
      this.loginError = ''
      try {
        const session = await login(loginForm)
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
      this.visibleMenus = normalizeSystemMenus(session.menus || [])
      this.menuGroups = this.visibleMenus.reduce((groups, menu) => {
        if (menu.children && menu.children.length) {
          groups[menu.code] = true
        }
        return groups
      }, {})
      const firstMenu = firstLeafMenu(this.visibleMenus)
      this.activeView = this.permissions.includes('asset:view') ? 'asset:view' : (firstMenu ? firstMenu.code : '')
      this.loadSummary()
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
      this.menuGroups = {}
      this.summary = {}
    },
    handleMenuClick(menu) {
      if (menu.children && menu.children.length) {
        this.$set(this.menuGroups, menu.code, !this.menuGroups[menu.code])
        if (this.menuGroups[menu.code]) {
          this.switchView(menu.children[0].code)
        }
        return
      }
      this.switchView(menu.code)
    },
    switchView(code) {
      if (code !== 'system:manage') {
        this.activeView = code
      }
    },
    async loadSummary() {
      try {
        this.summary = await getSummary()
      } catch (error) {
        this.summary = {}
      }
    }
  }
}

function normalizeSystemMenus(items) {
  const clones = (items || []).map(item => ({
    ...item,
    children: [...(item.children || [])]
  }))
  const systemMenu = clones.find(item => item.code === 'system:manage')
  if (!systemMenu) {
    return clones
  }
  const systemCodes = ['system:user', 'system:role', 'system:menu']
  const normalized = []
  clones.forEach(item => {
    if (item.code === 'system:manage') {
      normalized.push(item)
      return
    }
    if (systemCodes.includes(item.code)) {
      const alreadyChild = systemMenu.children.find(child => child.code === item.code)
      if (!alreadyChild) {
        systemMenu.children.push({
          ...item,
          parent_id: systemMenu.id,
          parentId: systemMenu.id,
          children: []
        })
      }
      return
    }
    normalized.push(item)
  })
  systemMenu.children.sort((a, b) => Number(a.sort_no || a.sortNo || 0) - Number(b.sort_no || b.sortNo || 0))
  return normalized
}

const MOBILE_TAB_WHITELIST = [
  { code: 'asset:view', name: '全部资产', icon: '▦' },
  { code: 'clothes:item', name: '衣服明细', icon: '⌥' },
  { code: 'wealth:overview', name: '资产总览', icon: '⌂' },
  { code: 'travel:wuhan', name: '武汉游', icon: '⚑' }
]

function buildMobileTabItems(menus) {
  const menuMap = flattenMenuByCode(menus)
  return MOBILE_TAB_WHITELIST.map(tab => ({
    code: tab.code,
    name: menuMap[tab.code]?.name || tab.name,
    icon: menuMap[tab.code]?.icon || tab.icon
  }))
}

function flattenMenuByCode(menus) {
  const map = {}
  const walk = items => {
    ;(items || []).forEach(item => {
      if (item.children && item.children.length) {
        walk(item.children)
      } else if (item.code) {
        map[item.code] = item
      }
    })
  }
  walk(menus)
  return map
}

function firstLeafMenu(menus) {
  for (const menu of menus || []) {
    if (menu.children && menu.children.length) {
      const child = firstLeafMenu(menu.children)
      if (child) {
        return child
      }
    } else {
      return menu
    }
  }
  return null
}
</script>
