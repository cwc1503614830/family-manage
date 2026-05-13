<template>
  <section class="admin-page">
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

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeUserForm">
      <form class="asset-form" @submit.prevent="submitUser">
        <div class="form-head">
          <div>
            <p class="eyebrow">User</p>
            <h2>{{ editingId ? '编辑用户' : '新增用户' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeUserForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>账号</span>
            <input v-model.trim="form.username" required />
          </label>
          <label>
            <span>{{ editingId ? '新密码（可空）' : '初始密码' }}</span>
            <input v-model="form.password" :required="!editingId" type="password" />
          </label>
          <label>
            <span>姓名</span>
            <input v-model.trim="form.displayName" required />
          </label>
          <label>
            <span>状态</span>
            <select v-model.number="form.status">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </label>
          <label class="wide">
            <span>角色</span>
            <div class="check-grid">
              <label v-for="role in roles" :key="role.id">
                <input v-model="form.roleIds" type="checkbox" :value="role.id" />
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
  </section>
</template>

<script>
import { createUser, deleteUser, listRoles, listUsers, updateUser } from '../api/system'

export default {
  name: 'UserManageView',
  data() {
    return {
      users: [],
      roles: [],
      dialogVisible: false,
      editingId: null,
      form: emptyUserForm()
    }
  },
  created() {
    this.bootstrap()
  },
  methods: {
    async bootstrap() {
      await Promise.all([this.loadUsers(), this.loadRoles()])
    },
    async loadUsers() {
      this.users = await listUsers()
    },
    async loadRoles() {
      this.roles = await listRoles()
    },
    async openUserForm(user) {
      await this.loadRoles()
      this.editingId = user ? user.id : null
      this.form = user
        ? {
            username: user.username,
            password: '',
            displayName: user.display_name,
            status: Number(user.status),
            roleIds: [...(user.roleIds || [])]
          }
        : emptyUserForm()
      this.dialogVisible = true
    },
    closeUserForm() {
      this.dialogVisible = false
    },
    async submitUser() {
      if (this.editingId) {
        await updateUser(this.editingId, this.form)
      } else {
        await createUser(this.form)
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
    formatDate(value) {
      return value ? String(value).replace('T', ' ') : '-'
    }
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
</script>
