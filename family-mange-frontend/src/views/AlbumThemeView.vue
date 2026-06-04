<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Album Management</p>
        <h1>相册主题</h1>
        <p>维护家庭相册主题分类，每个主题可上传多张照片。</p>
      </div>
      <button class="primary-btn" type="button" @click="openForm()">新增主题</button>
    </div>

    <ResizableTable :columns="columns" :rows="themes" empty-text="暂无相册主题">
      <template #photo_count="{ row }">
        <span>{{ row.photo_count || 0 }} 张</span>
      </template>
      <template #actions="{ row }">
        <button class="ghost-btn mini" type="button" @click="openForm(row)">编辑</button>
        <button class="ghost-btn mini danger-text" type="button" @click="removeTheme(row)">删除</button>
      </template>
    </ResizableTable>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeForm">
      <form class="asset-form" @submit.prevent="submitForm">
        <div class="form-head">
          <div>
            <p class="eyebrow">Album Theme</p>
            <h2>{{ editingId ? '编辑主题' : '新增主题' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>主题</span>
            <input v-model.trim="form.themeName" required placeholder="例如：旅行、美食" />
          </label>
          <label class="wide">
            <span>备注</span>
            <textarea v-model.trim="form.remark" rows="3"></textarea>
          </label>
        </div>
        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closeForm">取消</button>
          <button class="primary-btn" type="submit">保存</button>
        </div>
      </form>
    </div>
  </section>
</template>

<script>
import ResizableTable from '../components/ResizableTable.vue'
import { createAlbumTheme, deleteAlbumTheme, listAlbumThemes, updateAlbumTheme } from '../api/album'

export default {
  name: 'AlbumThemeView',
  components: { ResizableTable },
  data() {
    return {
      themes: [],
      dialogVisible: false,
      editingId: null,
      form: emptyForm(),
      columns: [
        { key: 'id', label: 'ID', width: 90 },
        { key: 'theme_name', label: '主题', width: 180 },
        { key: 'photo_count', label: '照片数', width: 120, slot: 'photo_count' },
        { key: 'remark', label: '备注', width: 280 },
        { key: 'actions', label: '操作', width: 150, slot: 'actions' }
      ]
    }
  },
  created() {
    this.loadThemes()
  },
  methods: {
    async loadThemes() {
      this.themes = await listAlbumThemes()
    },
    openForm(theme) {
      this.editingId = theme ? theme.id : null
      this.form = theme
        ? { themeName: theme.theme_name, remark: theme.remark || '' }
        : emptyForm()
      this.dialogVisible = true
    },
    closeForm() {
      this.dialogVisible = false
    },
    async submitForm() {
      if (this.editingId) {
        await updateAlbumTheme(this.editingId, this.form)
      } else {
        await createAlbumTheme(this.form)
      }
      this.closeForm()
      await this.loadThemes()
    },
    async removeTheme(theme) {
      const count = theme.photo_count || 0
      const hint = count > 0 ? `该主题下有 ${count} 张照片，删除后照片也会一并删除。` : ''
      if (window.confirm(`确认删除主题「${theme.theme_name}」吗？${hint}`)) {
        await deleteAlbumTheme(theme.id)
        await this.loadThemes()
      }
    }
  }
}

function emptyForm() {
  return {
    themeName: '',
    remark: ''
  }
}
</script>
