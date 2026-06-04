<template>
  <div class="album-page" :class="{ 'album-mobile-mode': isMobile }">
    <!-- 主题详情子页面 -->
    <section v-if="activeTheme" class="album-detail-page" :class="{ 'album-mobile-detail': isMobile }">
      <header class="album-detail-head" :class="{ 'album-mobile-detail-head': isMobile }">
        <button class="ghost-btn album-back-btn" type="button" @click="closeThemeDetail">←</button>
        <div class="album-detail-title">
          <h1>{{ activeTheme.theme_name }}</h1>
          <p>{{ activeTheme.photo_count || 0 }} 张照片</p>
        </div>
        <button
          v-if="!isMobile"
          class="primary-btn"
          type="button"
          @click="openUploadForm"
        >上传照片</button>
      </header>

      <section class="album-photo-grid" :class="{ loading: detailLoading, 'album-mobile-photo-grid': isMobile }">
        <article
          v-for="photo in photos"
          :key="photo.id"
          class="album-photo-card"
          :class="{ 'album-mobile-photo-item': isMobile }"
        >
          <div class="album-photo-card-image" @click="previewPhoto(photo)">
            <img :src="photo.image_url" :alt="photo.image_name" />
            <span v-if="!isMobile" class="album-photo-count-badge">{{ photo.image_name }}</span>
          </div>
          <div v-if="!isMobile" class="album-photo-card-actions">
            <button class="ghost-btn mini" type="button" @click="openPhotoForm(photo)">编辑</button>
            <button class="ghost-btn mini danger-text" type="button" @click="removePhoto(photo)">删除</button>
          </div>
        </article>
      </section>
      <p v-if="!detailLoading && !photos.length" class="album-empty">
        {{ isMobile ? '暂无照片，点右下角 + 上传' : '该主题暂无照片，点击上方上传' }}
      </p>
      <div v-if="photoTotal > pageSize" class="table-pagination">
        <span class="pagination-summary">共 {{ photoTotal }} 张</span>
        <button class="ghost-btn mini" type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
        <span class="pagination-current">第 {{ page }} / {{ totalPages }} 页</span>
        <button class="ghost-btn mini" type="button" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
      </div>

      <button
        v-if="isMobile"
        class="album-fab"
        type="button"
        aria-label="上传照片"
        @click="openUploadForm"
      >+</button>
    </section>

    <!-- 相册主页面 -->
    <section v-else class="admin-page album-list-page" :class="{ 'album-mobile-list-page': isMobile }">
      <!-- 移动端 QQ 空间风格顶栏 -->
      <header v-if="isMobile" class="album-mobile-header">
        <h1>家庭相册</h1>
        <p>{{ albumCount }} 相册 · {{ photoCount }} 照片</p>
      </header>

      <!-- 桌面端顶栏 -->
      <div v-else class="album-page-head">
        <div>
          <p class="eyebrow">Album Management</p>
          <h1>家庭相册</h1>
          <p class="album-summary">{{ albumCount }} 相册 · {{ photoCount }} 照片</p>
        </div>
        <button class="ghost-btn refresh-btn" type="button" @click="loadCards">刷新</button>
      </div>

      <!-- 移动端分段：相册 / 照片 -->
      <nav v-if="isMobile" class="album-mobile-segment">
        <button
          type="button"
          class="album-segment-btn"
          :class="{ active: mobileTab === 'albums' }"
          @click="mobileTab = 'albums'"
        >相册</button>
        <button
          type="button"
          class="album-segment-btn"
          :class="{ active: mobileTab === 'photos' }"
          @click="switchToPhotosTab"
        >照片</button>
      </nav>

      <!-- 相册卡片 -->
      <section
        v-if="!isMobile || mobileTab === 'albums'"
        class="album-card-grid"
        :class="{ loading: loading, 'album-qq-grid': isMobile }"
      >
        <article class="album-theme-card album-create-card" @click="openCreateTheme">
          <div class="album-theme-card-cover create">
            <div class="album-create-inner">
              <span class="album-create-icon" aria-hidden="true">
                <i class="album-create-square back"></i>
                <i class="album-create-square front"></i>
              </span>
              <span class="album-create-label">新建相册</span>
            </div>
          </div>
        </article>

        <article
          v-for="card in cards"
          :key="card.id"
          class="album-theme-card album-qq-card"
          @click="openThemeDetail(card)"
        >
          <div class="album-theme-card-cover">
            <img v-if="card.cover_url" :src="card.cover_url" :alt="card.theme_name" />
            <span v-else class="album-no-cover">暂无封面</span>
            <span v-if="isMobile && card.remark" class="album-qq-tag">{{ card.remark }}</span>
            <span v-if="card.photo_count" class="album-count-badge">{{ card.photo_count }}张</span>
          </div>
          <div class="album-theme-card-body">
            <h3>{{ card.theme_name }}</h3>
            <p v-if="!isMobile && card.remark" class="album-theme-remark">{{ card.remark }}</p>
          </div>
        </article>
      </section>

      <!-- 移动端：全部照片流 -->
      <section
        v-if="isMobile && mobileTab === 'photos'"
        class="album-all-photo-grid"
        :class="{ loading: allPhotosLoading }"
      >
        <article
          v-for="photo in allPhotos"
          :key="photo.id"
          class="album-all-photo-item"
          @click="previewPhoto(photo)"
        >
          <img :src="photo.image_url" :alt="photo.image_name" />
          <span v-if="photo.theme_name" class="album-all-photo-tag">{{ photo.theme_name }}</span>
        </article>
      </section>
      <p v-if="isMobile && mobileTab === 'photos' && !allPhotosLoading && !allPhotos.length" class="album-empty">
        暂无照片
      </p>

      <p v-if="!isMobile && !loading && !cards.length" class="album-empty">暂无相册主题，点击「新建相册」开始创建</p>

      <button
        v-if="isMobile"
        class="album-fab"
        type="button"
        aria-label="新建相册"
        @click="handleFabClick"
      >+</button>
    </section>

    <!-- 新建主题弹窗 -->
    <div v-if="themeDialogVisible" class="modal-mask" @click.self="closeCreateTheme">
      <form class="asset-form" @submit.prevent="submitTheme">
        <div class="form-head">
          <div>
            <p class="eyebrow">Album Theme</p>
            <h2>新建相册</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeCreateTheme">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>主题名称</span>
            <input v-model.trim="themeForm.themeName" required placeholder="例如：武汉、旅行" />
          </label>
          <label class="wide">
            <span>备注</span>
            <textarea v-model.trim="themeForm.remark" rows="2"></textarea>
          </label>
        </div>
        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closeCreateTheme">取消</button>
          <button class="primary-btn" type="submit">创建</button>
        </div>
      </form>
    </div>

    <!-- 上传/编辑照片弹窗 -->
    <div v-if="photoDialogVisible" class="modal-mask" @click.self="closePhotoForm">
      <form class="asset-form" @submit.prevent="submitPhotoForm">
        <div class="form-head">
          <div>
            <p class="eyebrow">Album Photo</p>
            <h2>{{ editingPhotoId ? '编辑照片' : '上传照片' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closePhotoForm">×</button>
        </div>
        <div class="form-grid">
          <label v-if="!activeTheme && !editingPhotoId">
            <span>所属主题</span>
            <select v-model.number="photoForm.themeId" required>
              <option :value="null" disabled>请选择主题</option>
              <option v-for="theme in allThemes" :key="theme.id" :value="theme.id">{{ theme.theme_name }}</option>
            </select>
          </label>
          <label class="wide">
            <span>照片</span>
            <div class="clothes-upload">
              <div v-if="pendingUploads.length" class="album-upload-preview-grid">
                <div v-for="(item, index) in pendingUploads" :key="index" class="album-upload-preview-item">
                  <img :src="item.previewUrl" alt="预览" />
                  <button class="ghost-btn mini" type="button" @click="removePendingUpload(index)">移除</button>
                </div>
              </div>
              <div v-else-if="photoForm.imageUrl" class="clothes-preview">
                <img :src="photoForm.imageUrl" alt="预览图" />
                <button class="ghost-btn mini" type="button" @click="clearImage">移除</button>
              </div>
              <div class="clothes-upload-actions">
                <input
                  ref="fileInput"
                  type="file"
                  accept="image/*"
                  :multiple="!editingPhotoId"
                  class="clothes-file-input"
                  @change="handleImageSelect"
                />
                <button class="ghost-btn" type="button" :disabled="uploading" @click="triggerFileSelect">
                  {{ uploading ? '上传中...' : editingPhotoId ? '更换图片' : '选择图片（可多选）' }}
                </button>
                <span v-if="uploadError" class="form-error">{{ uploadError }}</span>
              </div>
            </div>
          </label>
        </div>
        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closePhotoForm">取消</button>
          <button class="primary-btn" type="submit" :disabled="uploading">
            {{ uploading ? '上传中...' : '保存' }}
          </button>
        </div>
      </form>
    </div>

    <!-- 图片预览 -->
    <div v-if="previewUrl" class="modal-mask album-preview-mask" @click="previewUrl = ''">
      <img :src="previewUrl" alt="预览" class="album-preview-image" @click.stop />
    </div>
  </div>
</template>

<script>
import {
  createAlbumPhoto,
  createAlbumPhotosBatch,
  createAlbumTheme,
  deleteAlbumPhoto,
  listAlbumCards,
  listAlbumPhotos,
  listAlbumThemes,
  updateAlbumPhoto
} from '../api/album'
import { uploadFile } from '../api/upload'

const MOBILE_PHOTO_PAGE_SIZE = 60

export default {
  name: 'AlbumPhotoView',
  data() {
    return {
      isMobile: false,
      mobileBreakpoint: null,
      mobileTab: 'albums',
      loading: false,
      detailLoading: false,
      allPhotosLoading: false,
      cards: [],
      albumCount: 0,
      photoCount: 0,
      allPhotos: [],
      activeTheme: null,
      photos: [],
      photoTotal: 0,
      page: 1,
      pageSize: 20,
      allThemes: [],
      themeDialogVisible: false,
      themeForm: emptyThemeForm(),
      photoDialogVisible: false,
      editingPhotoId: null,
      photoForm: emptyPhotoForm(),
      pendingUploads: [],
      uploading: false,
      uploadError: '',
      previewUrl: ''
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.photoTotal / this.pageSize))
    }
  },
  created() {
    this.setupMobileListener()
    this.loadCards()
  },
  beforeDestroy() {
    if (this.mobileBreakpoint) {
      this.mobileBreakpoint.removeEventListener('change', this.syncMobileState)
    }
  },
  methods: {
    setupMobileListener() {
      this.mobileBreakpoint = window.matchMedia('(max-width: 640px)')
      this.syncMobileState()
      this.mobileBreakpoint.addEventListener('change', this.syncMobileState)
    },
    syncMobileState() {
      this.isMobile = Boolean(this.mobileBreakpoint && this.mobileBreakpoint.matches)
      if (this.isMobile && this.mobileTab === 'photos') {
        this.loadAllPhotos()
      }
    },
    async loadCards() {
      this.loading = true
      try {
        const data = await listAlbumCards()
        this.cards = data.cards || []
        this.albumCount = data.albumCount || 0
        this.photoCount = data.photoCount || 0
      } finally {
        this.loading = false
      }
    },
    async loadAllPhotos() {
      this.allPhotosLoading = true
      try {
        const data = await listAlbumPhotos({ page: 1, size: MOBILE_PHOTO_PAGE_SIZE })
        this.allPhotos = data.records || []
      } finally {
        this.allPhotosLoading = false
      }
    },
    switchToPhotosTab() {
      this.mobileTab = 'photos'
      this.loadAllPhotos()
    },
    handleFabClick() {
      if (this.mobileTab === 'photos') {
        this.openUploadForm()
        return
      }
      this.openCreateTheme()
    },
    async loadThemes() {
      this.allThemes = await listAlbumThemes()
    },
    openCreateTheme() {
      this.themeForm = emptyThemeForm()
      this.themeDialogVisible = true
    },
    closeCreateTheme() {
      this.themeDialogVisible = false
    },
    async submitTheme() {
      const theme = await createAlbumTheme(this.themeForm)
      this.closeCreateTheme()
      await this.loadCards()
      this.openThemeDetail({
        id: theme.id,
        theme_name: theme.theme_name,
        photo_count: 0,
        remark: theme.remark
      })
    },
    openThemeDetail(card) {
      this.activeTheme = { ...card }
      this.page = 1
      this.loadThemePhotos()
    },
    closeThemeDetail() {
      this.activeTheme = null
      this.photos = []
      this.loadCards()
      if (this.isMobile && this.mobileTab === 'photos') {
        this.loadAllPhotos()
      }
    },
    async loadThemePhotos() {
      if (!this.activeTheme) {
        return
      }
      this.detailLoading = true
      try {
        const data = await listAlbumPhotos({
          themeId: this.activeTheme.id,
          page: this.page,
          size: this.pageSize
        })
        this.photos = data.records || []
        this.photoTotal = data.total || 0
        this.activeTheme.photo_count = this.photoTotal
      } finally {
        this.detailLoading = false
      }
    },
    goPage(nextPage) {
      this.page = nextPage
      this.loadThemePhotos()
    },
    async openUploadForm() {
      this.editingPhotoId = null
      this.photoForm = {
        ...emptyPhotoForm(),
        themeId: this.activeTheme ? this.activeTheme.id : null
      }
      this.pendingUploads = []
      this.uploadError = ''
      await this.loadThemes()
      this.photoDialogVisible = true
    },
    openPhotoForm(photo) {
      this.editingPhotoId = photo.id
      this.photoForm = {
        imageName: photo.image_name,
        imageUrl: photo.image_url,
        themeId: photo.theme_id,
        remark: photo.remark || ''
      }
      this.pendingUploads = []
      this.uploadError = ''
      this.photoDialogVisible = true
    },
    closePhotoForm() {
      this.revokePendingPreviews()
      this.photoDialogVisible = false
    },
    triggerFileSelect() {
      this.$refs.fileInput && this.$refs.fileInput.click()
    },
    async handleImageSelect(event) {
      const files = Array.from(event.target.files || [])
      event.target.value = ''
      if (!files.length) {
        return
      }
      this.uploadError = ''
      if (this.editingPhotoId) {
        await this.uploadSingleFile(files[0], true)
        return
      }
      for (const file of files) {
        await this.uploadSingleFile(file, false)
      }
    },
    async uploadSingleFile(file, replace) {
      this.uploading = true
      try {
        const result = await uploadFile(file, 'album')
        const previewUrl = result.url || result.publicUrl || result.path || ''
        if (replace) {
          this.photoForm.imageUrl = previewUrl
          this.pendingUploads = []
        } else {
          const baseName = file.name.replace(/\.[^.]+$/, '')
          this.pendingUploads.push({
            previewUrl,
            imageUrl: previewUrl,
            imageName: baseName || `照片${this.pendingUploads.length + 1}`
          })
        }
      } catch (error) {
        this.uploadError = error.message || '上传失败'
      } finally {
        this.uploading = false
      }
    },
    removePendingUpload(index) {
      const item = this.pendingUploads[index]
      if (item && item.previewUrl && item.previewUrl.startsWith('blob:')) {
        URL.revokeObjectURL(item.previewUrl)
      }
      this.pendingUploads.splice(index, 1)
    },
    revokePendingPreviews() {
      this.pendingUploads.forEach(item => {
        if (item.previewUrl && item.previewUrl.startsWith('blob:')) {
          URL.revokeObjectURL(item.previewUrl)
        }
      })
      this.pendingUploads = []
    },
    clearImage() {
      this.photoForm.imageUrl = ''
    },
    async submitPhotoForm() {
      const themeId = this.activeTheme ? this.activeTheme.id : this.photoForm.themeId
      if (!themeId) {
        this.uploadError = '请选择相册主题'
        return
      }
      if (this.editingPhotoId) {
        await updateAlbumPhoto(this.editingPhotoId, {
          imageName: this.photoForm.imageName || resolveAutoImageName(this.photoForm.imageUrl, 0),
          imageUrl: this.photoForm.imageUrl,
          themeId,
          remark: this.photoForm.remark || ''
        })
      } else if (this.pendingUploads.length) {
        const batch = this.pendingUploads.map((item, index) => ({
          imageName: resolveAutoImageName(item.imageName || item.imageUrl, index),
          imageUrl: item.imageUrl,
          themeId,
          remark: ''
        }))
        await createAlbumPhotosBatch(batch)
      } else if (this.photoForm.imageUrl) {
        await createAlbumPhoto({
          imageName: resolveAutoImageName(this.photoForm.imageUrl, 0),
          imageUrl: this.photoForm.imageUrl,
          themeId,
          remark: ''
        })
      } else {
        this.uploadError = '请上传至少一张照片'
        return
      }
      this.closePhotoForm()
      if (this.activeTheme) {
        await this.loadThemePhotos()
      } else {
        await this.loadCards()
        if (this.isMobile && this.mobileTab === 'photos') {
          await this.loadAllPhotos()
        }
      }
    },
    async removePhoto(photo) {
      if (window.confirm(`确认删除照片「${photo.image_name}」吗？`)) {
        await deleteAlbumPhoto(photo.id)
        await this.loadThemePhotos()
      }
    },
    previewPhoto(photo) {
      this.previewUrl = photo.image_url
    }
  }
}

function emptyThemeForm() {
  return { themeName: '', remark: '' }
}

function emptyPhotoForm() {
  return { imageName: '', imageUrl: '', themeId: null, remark: '' }
}

function resolveAutoImageName(source, index) {
  const text = String(source || '').trim()
  if (text && !text.startsWith('http') && !text.includes('/')) {
    return text
  }
  const fileName = text.split('/').pop() || ''
  const baseName = fileName.replace(/\.[^.]+$/, '').replace(/\?.*$/, '')
  if (baseName) {
    return baseName
  }
  return `照片${index + 1}`
}
</script>
