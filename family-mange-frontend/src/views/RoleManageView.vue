<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Access Control</p>
        <h1>角色管理</h1>
        <p>通过角色统一分配菜单权限。</p>
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

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeRoleForm">
      <form class="asset-form" @submit.prevent="submitRole">
        <div class="form-head">
          <div>
            <p class="eyebrow">Role</p>
            <h2>{{ editingId ? '编辑角色' : '新增角色' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeRoleForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>角色编码</span>
            <input v-model.trim="form.code" required placeholder="admin" />
          </label>
          <label>
            <span>角色名称</span>
            <input v-model.trim="form.name" required placeholder="家庭管理员" />
          </label>
          <label class="wide">
            <span>备注</span>
            <textarea v-model.trim="form.remark" rows="2"></textarea>
          </label>
          <label class="wide">
            <span>权限</span>
            <div class="check-grid permission-grid">
              <label v-for="menu in menus" :key="menu.id">
                <input v-model="form.menuIds" type="checkbox" :value="menu.id" />
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
  </section>
</template>

<script>
import { createRole, deleteRole, listMenus, listRoles, updateRole } from '../api/system'

export default {
  name: 'RoleManageView',
  data() {
    return {
      roles: [],
      menus: [],
      dialogVisible: false,
      editingId: null,
      form: emptyRoleForm()
    }
  },
  created() {
    this.bootstrap()
  },
  methods: {
    async bootstrap() {
      await Promise.all([this.loadRoles(), this.loadMenus()])
    },
    async loadRoles() {
      this.roles = await listRoles()
    },
    async loadMenus() {
      this.menus = await listMenus()
    },
    async openRoleForm(role) {
      await this.loadMenus()
      this.editingId = role ? role.id : null
      this.form = role
        ? {
            code: role.code,
            name: role.name,
            remark: role.remark || '',
            menuIds: [...(role.menuIds || [])]
          }
        : emptyRoleForm()
      this.dialogVisible = true
    },
    closeRoleForm() {
      this.dialogVisible = false
    },
    async submitRole() {
      if (this.editingId) {
        await updateRole(this.editingId, this.form)
      } else {
        await createRole(this.form)
      }
      this.closeRoleForm()
      await this.loadRoles()
    },
    async removeRole(role) {
      if (window.confirm(`确认删除角色「${role.name}」吗？`)) {
        await deleteRole(role.id)
        await this.loadRoles()
      }
    }
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
</script>
