<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Access Control</p>
        <h1>菜单权限</h1>
        <p>配置父子菜单、页面路径、组件地址和菜单权限。</p>
      </div>
      <div class="head-actions">
        <button class="primary-btn" type="button" @click="openMenuForm()">＋ 新增</button>
        <button class="ghost-btn danger-text" type="button" @click="removeSelectedMenus">批量删除</button>
      </div>
    </div>

    <div class="menu-toolbar">
      <button class="ghost-btn" type="button" @click="filterVisible = !filterVisible">⌕ 筛选</button>
      <button class="ghost-btn refresh-btn" type="button" @click="loadMenus">↻ 刷新</button>
    </div>
    <section v-if="filterVisible" class="toolbar menu-filter">
      <label class="search-box">
        <span>⌕</span>
        <input v-model.trim="keyword" type="search" placeholder="搜索菜单名称、路径、组件或权限..." />
      </label>
    </section>

    <div class="admin-table">
      <table class="menu-table">
        <thead>
          <tr>
            <th class="check-col"><input type="checkbox" :checked="allRowsSelected" @change="toggleAllRows($event)" /></th>
            <th>菜单名称</th>
            <th>菜单图标</th>
            <th>菜单路径</th>
            <th>菜单组件</th>
            <th>网页地址</th>
            <th>菜单按钮</th>
            <th>排序</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="menu in menuTreeRows" :key="menu.id">
            <td class="check-col"><input v-model="selectedIds" type="checkbox" :value="menu.id" /></td>
            <td>
              <div class="tree-name" :style="{ paddingLeft: `${menu.level * 24}px` }">
                <span class="tree-toggle">{{ menu.hasChildren ? '⌄' : '·' }}</span>
                <span class="tree-icon">{{ menu.icon || '▦' }}</span>
                <strong>{{ menu.name }}</strong>
              </div>
            </td>
            <td>{{ menu.icon || '-' }}</td>
            <td class="truncate">{{ menu.path || '-' }}</td>
            <td class="truncate">{{ menu.component || '-' }}</td>
            <td class="truncate">{{ menu.web_url || '-' }}</td>
            <td class="truncate">{{ menu.code || '-' }}</td>
            <td>{{ menu.sort_no }}</td>
            <td><span class="pill">{{ Number(menu.visible) === 1 ? '显示' : '隐藏' }}</span></td>
            <td>
              <button class="ghost-btn mini" type="button" @click="openMenuForm(menu)">编辑</button>
              <button class="ghost-btn mini danger-text" type="button" @click="removeMenu(menu)">删除</button>
              <button class="ghost-btn mini" type="button" @click="openMenuForm(null, menu)">授权</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeMenuForm">
      <form class="asset-form" @submit.prevent="submitMenu">
        <div class="form-head">
          <div>
            <p class="eyebrow">Permission</p>
            <h2>{{ editingId ? '编辑权限' : '新增权限' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeMenuForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>上级菜单</span>
            <select v-model.number="form.parentId">
              <option :value="0">顶级菜单</option>
              <option v-for="item in parentMenuOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </option>
            </select>
          </label>
          <label>
            <span>菜单名称</span>
            <input v-model.trim="form.name" required />
          </label>
          <label>
            <span>菜单图标</span>
            <input v-model.trim="form.icon" placeholder="▦" />
          </label>
          <label>
            <span>菜单路径</span>
            <input v-model.trim="form.path" placeholder="/system/menus" />
          </label>
          <label>
            <span>菜单组件</span>
            <input v-model.trim="form.component" placeholder="SystemMenu" />
          </label>
          <label>
            <span>网页地址</span>
            <input v-model.trim="form.webUrl" placeholder="https://..." />
          </label>
          <label>
            <span>菜单按钮</span>
            <input v-model.trim="form.code" required placeholder="system:menu" />
          </label>
          <label>
            <span>排序</span>
            <input v-model.number="form.sortNo" min="0" type="number" />
          </label>
          <label class="wide checkbox-line">
            <input v-model="form.visible" type="checkbox" />
            <span>作为侧边栏菜单显示</span>
          </label>
        </div>
        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closeMenuForm">取消</button>
          <button class="primary-btn" type="submit">保存</button>
        </div>
      </form>
    </div>
  </section>
</template>

<script>
import { createMenu, deleteMenu, listMenus, updateMenu } from '../api/system'

export default {
  name: 'MenuManageView',
  data() {
    return {
      menus: [],
      selectedIds: [],
      keyword: '',
      filterVisible: false,
      dialogVisible: false,
      editingId: null,
      form: emptyMenuForm()
    }
  },
  computed: {
    menuTreeRows() {
      const keyword = this.keyword.trim()
      return this.flattenMenus(buildTree(this.menus), 0)
        .filter(menu => {
          if (!keyword) {
            return true
          }
          const text = `${menu.name || ''}${menu.code || ''}${menu.path || ''}${menu.component || ''}${menu.web_url || ''}`
          return text.includes(keyword)
        })
    },
    parentMenuOptions() {
      return this.menuTreeRows
        .filter(menu => menu.id !== this.editingId)
        .map(menu => ({
          id: menu.id,
          name: `${'　'.repeat(menu.level)}${menu.name}`
        }))
    },
    allRowsSelected() {
      return this.menuTreeRows.length > 0 && this.menuTreeRows.every(menu => this.selectedIds.includes(menu.id))
    }
  },
  created() {
    this.loadMenus()
  },
  methods: {
    async loadMenus() {
      this.menus = await listMenus()
      this.selectedIds = this.selectedIds.filter(id => this.menus.find(menu => menu.id === id))
    },
    openMenuForm(menu, parent) {
      this.editingId = menu ? menu.id : null
      this.form = menu
        ? {
            parentId: Number(menu.parent_id || 0),
            name: menu.name,
            code: menu.code,
            icon: menu.icon || '',
            path: menu.path || '',
            component: menu.component || '',
            webUrl: menu.web_url || '',
            sortNo: Number(menu.sort_no || 100),
            visible: Number(menu.visible) === 1
          }
        : emptyMenuForm()
      if (!menu && parent) {
        this.form.parentId = parent.id
        this.form.visible = false
        this.form.sortNo = Number(parent.sort_no || 100) + 1
      }
      this.dialogVisible = true
    },
    closeMenuForm() {
      this.dialogVisible = false
    },
    async submitMenu() {
      if (this.editingId) {
        await updateMenu(this.editingId, this.form)
      } else {
        await createMenu(this.form)
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
    async removeSelectedMenus() {
      if (!this.selectedIds.length) {
        window.alert('请先选择要删除的菜单')
        return
      }
      if (!window.confirm(`确认删除选中的 ${this.selectedIds.length} 个菜单吗？`)) {
        return
      }
      for (const id of [...this.selectedIds]) {
        await deleteMenu(id)
      }
      this.selectedIds = []
      await this.loadMenus()
    },
    toggleAllRows(event) {
      this.selectedIds = event.target.checked ? this.menuTreeRows.map(menu => menu.id) : []
    },
    flattenMenus(menus, level) {
      return menus.reduce((rows, menu) => {
        rows.push({
          ...menu,
          level,
          hasChildren: Boolean(menu.children && menu.children.length)
        })
        if (menu.children && menu.children.length) {
          rows.push(...this.flattenMenus(menu.children, level + 1))
        }
        return rows
      }, [])
    }
  }
}

function emptyMenuForm() {
  return {
    parentId: 0,
    name: '',
    code: '',
    icon: '',
    path: '',
    component: '',
    webUrl: '',
    sortNo: 100,
    visible: true
  }
}

function buildTree(items) {
  const map = new Map()
  const roots = []
  ;(normalizeSystemMenus(items || [])).forEach(item => {
    map.set(item.id, { ...item, children: [] })
  })
  map.forEach(item => {
    const parentId = Number(item.parent_id || item.parentId || 0)
    if (parentId && map.has(parentId)) {
      map.get(parentId).children.push(item)
    } else {
      roots.push(item)
    }
  })
  return roots
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
</script>
