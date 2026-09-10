<template>
  <div class="clothes-item-page">
    <section class="admin-page clothes-item-list-page">
      <div class="clothes-item-page-head">
        <h1>衣服明细</h1>
        <div class="clothes-item-toolbar">
          <div class="clothes-item-actions">
            <button class="primary-btn" type="button" @click="openForm()">新增衣服</button>
            <button class="ghost-btn refresh-btn" type="button" @click="bootstrap">刷新</button>
            <button
              class="ghost-btn danger-text"
              type="button"
              :disabled="!selectedIds.length"
              @click="removeSelected"
            >
              删除
            </button>
          </div>
        </div>
      </div>

      <ResizableTable
        :key="isMobile ? 'clothes-mobile' : 'clothes-desktop'"
        :class="{ 'clothes-item-mobile-table': isMobile, 'clothes-item-desktop-table': !isMobile }"
        :columns="tableColumns"
        :rows="items"
        row-clickable
        empty-text="暂无衣服明细"
        @row-click="openForm"
      >
        <template #selectHeader>
          <label class="clothes-select-cell">
            <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
          </label>
        </template>
        <template #select="{ row }">
          <label class="clothes-select-cell">
            <input type="checkbox" :checked="isSelected(row.id)" @change="toggleSelect(row.id)" />
          </label>
        </template>
        <template #image="{ row }">
          <div class="clothes-image-cell">
            <img
              v-if="row.image_url"
              :src="row.image_url"
              alt="衣服图片"
              class="clothes-thumb"
              @click.stop="previewImage(row.image_url)"
            />
            <span v-else class="clothes-no-image">无</span>
          </div>
        </template>
      </ResizableTable>

      <div class="table-pagination">
        <span class="pagination-summary">共 {{ total }} 条</span>
        <label class="pagination-size">
          <span>每页</span>
          <select v-model.number="pageSize" @change="changePageSize">
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </label>
        <button class="ghost-btn mini" type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
        <span class="pagination-current">第 {{ page }} / {{ totalPages }} 页</span>
        <button class="ghost-btn mini" type="button" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
      </div>
    </section>

    <div v-if="dialogVisible" ref="clothesItemModal" class="clothes-modal-mask clothes-item-modal" @click.self="closeForm">
      <form class="clothes-modal-form clothes-modal-sheet" @submit.prevent="submitForm">
        <div class="form-head">
          <div>
            <p class="eyebrow">Clothes Item</p>
            <h2>{{ editingId ? '编辑衣服' : '新增衣服' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
        </div>

        <div class="clothes-modal-body">
          <div class="clothes-form-grid">
            <label>
              <span>衣服名称</span>
              <input v-model.trim="form.clothesName" required placeholder="例如：黑色羽绒服" />
            </label>
            <label>
              <span>衣服类型</span>
              <select v-model="form.typeCode" required>
                <option value="">请选择</option>
                <option v-for="type in types" :key="type.type_code" :value="type.type_code">
                  {{ type.type_name }}
                </option>
              </select>
            </label>
            <label>
              <span>存放位置</span>
              <select v-model="form.areaCode" required>
                <option value="">请选择</option>
                <option v-for="location in locations" :key="location.area_code" :value="location.area_code">
                  {{ location.area_name }}
                </option>
              </select>
            </label>
            <label>
              <span>拥有者</span>
              <select v-model.number="form.ownerUserId" :disabled="Boolean(editingId)" required>
                <option :value="null">请选择</option>
                <option v-for="user in users" :key="user.id" :value="user.id">
                  {{ user.display_name || user.username }}
                </option>
              </select>
            </label>
            <div class="clothes-date-row">
              <label>
                <span>买入时间</span>
                <input v-model="form.purchaseTime" :disabled="Boolean(editingId)" type="date" />
              </label>
              <label>
                <span>过期时间</span>
                <input v-model="form.expireTime" type="date" />
              </label>
            </div>
            <label class="wide">
              <span>衣服图片</span>
              <div class="clothes-upload">
                <div v-if="form.imageUrl" class="clothes-preview">
                  <img :src="form.imageUrl" alt="预览图" />
                  <button class="ghost-btn mini" type="button" @click="clearImage">移除</button>
                </div>
                <div class="clothes-upload-actions">
                  <input
                    ref="fileInput"
                    type="file"
                    accept="image/*"
                    class="clothes-file-input"
                    @change="handleImageSelect"
                  />
                  <button class="ghost-btn" type="button" :disabled="uploading" @click="triggerFileSelect">
                    {{ uploading ? '上传中...' : form.imageUrl ? '更换图片' : '选择图片' }}
                  </button>
                  <span v-if="uploadError" class="form-error">{{ uploadError }}</span>
                </div>
              </div>
            </label>
            <label class="wide">
              <span>备注</span>
              <textarea v-model.trim="form.remark" rows="2" placeholder="记录换季、清洗或特殊说明"></textarea>
            </label>
          </div>
        </div>

        <div class="form-actions clothes-form-actions">
          <button class="ghost-btn" type="button" @click="closeForm">取消</button>
          <button class="primary-btn" type="submit" :disabled="uploading">保存</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import ResizableTable from '../components/ResizableTable.vue'
import {
  createClothesItem,
  deleteClothesItem,
  getClothesOptions,
  listClothesItems,
  updateClothesItem
} from '../api/clothes'
import { uploadFile } from '../api/upload'

export default {
  name: 'ClothesItemView',
  components: { ResizableTable },
  data() {
    return {
      items: [],
      total: 0,
      page: 1,
      pageSize: 10,
      types: [],
      locations: [],
      users: [],
      dialogVisible: false,
      editingId: null,
      uploading: false,
      uploadError: '',
      form: emptyForm(),
      selectedIds: [],
      isMobile: false,
      mobileBreakpoint: null,
      columns: [
        { key: 'clothes_name', label: '衣服名称', width: 150 },
        { key: 'type_code', label: '类型编码', width: 120 },
        { key: 'type_name', label: '类型名称', width: 120 },
        { key: 'area_code', label: '区域编码', width: 140 },
        { key: 'area_name', label: '区域名称', width: 160 },
        { key: 'purchase_time', label: '买入时间', width: 120, formatter: formatDate },
        { key: 'expire_time', label: '过期时间', width: 120, formatter: formatDate },
        { key: 'image_url', label: '图片', width: 90, slot: 'image' },
        { key: 'owner_name', label: '拥有者', width: 120 },
        { key: 'remark', label: '备注', width: 180 },
        { key: 'created_by', label: '创建人', width: 100 },
        { key: 'created_at', label: '创建时间', width: 160, formatter: formatDateTime },
        { key: 'updated_by', label: '更新人', width: 100 },
        { key: 'updated_at', label: '更新时间', width: 160, formatter: formatDateTime }
      ],
      mobileColumns: [
        { key: 'clothes_name', label: '衣服名称', width: 78 },
        { key: 'area_name', label: '区域名称', width: 96 },
        { key: 'image_url', label: '图片', width: 42, slot: 'image' },
        { key: 'owner_name', label: '拥有者', width: 60 }
      ]
    }
  },
  computed: {
    tableColumns() {
      const selectColumn = { key: '_select', label: '', width: 44, slot: 'select', headerSlot: 'selectHeader' }
      if (this.isMobile) {
        return [selectColumn, ...this.mobileColumns]
      }
      return [selectColumn, ...this.columns]
    },
    isAllSelected() {
      return this.items.length > 0 && this.items.every(item => this.selectedIds.includes(item.id))
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    }
  },
  created() {
    this.setupMobileListener()
    this.bootstrap()
  },
  beforeDestroy() {
    if (this.mobileBreakpoint) {
      this.mobileBreakpoint.removeEventListener('change', this.syncMobileState)
    }
    this.teardownMobileFormViewport()
    document.body.classList.remove('modal-open', 'clothes-modal-open')
  },
  methods: {
    setupMobileListener() {
      if (typeof window === 'undefined' || !window.matchMedia) {
        return
      }
      this.mobileBreakpoint = window.matchMedia('(max-width: 640px)')
      this.syncMobileState()
      this.mobileBreakpoint.addEventListener('change', this.syncMobileState)
    },
    syncMobileState() {
      this.isMobile = Boolean(this.mobileBreakpoint && this.mobileBreakpoint.matches)
    },
    async bootstrap() {
      await Promise.all([this.loadItems(), this.loadOptions()])
    },
    async loadItems() {
      const result = await listClothesItems({ page: this.page, size: this.pageSize })
      this.items = result.records || []
      this.total = Number(result.total || 0)
      this.page = Number(result.page || this.page)
      this.pageSize = Number(result.size || this.pageSize)
      this.selectedIds = []
      if (this.page > this.totalPages) {
        this.page = this.totalPages
        if (this.total > 0) {
          await this.loadItems()
        }
      }
    },
    async loadOptions() {
      const options = await getClothesOptions()
      this.types = options.types || []
      this.locations = options.locations || []
      this.users = options.users || []
    },
    goPage(nextPage) {
      if (nextPage < 1 || nextPage > this.totalPages || nextPage === this.page) {
        return
      }
      this.page = nextPage
      this.loadItems()
    },
    changePageSize() {
      this.page = 1
      this.loadItems()
    },
    async openForm(item) {
      await this.loadOptions()
      this.editingId = item ? item.id : null
      this.uploadError = ''
      this.uploading = false
      this.form = item
        ? {
            clothesName: item.clothes_name,
            typeCode: item.type_code,
            areaCode: item.area_code,
            purchaseTime: formatDate(item.purchase_time),
            expireTime: formatDate(item.expire_time),
            imageUrl: item.image_url || '',
            ownerUserId: item.owner_user_id,
            remark: item.remark || ''
          }
        : emptyForm()
      this.dialogVisible = true
      document.body.classList.add('modal-open')
      this.$nextTick(() => {
        this.mountClothesModalToBody()
        this.setupMobileFormViewport()
      })
    },
    closeForm() {
      this.teardownMobileFormViewport()
      this.dialogVisible = false
      this.uploadError = ''
      this.uploading = false
      document.body.classList.remove('modal-open')
    },
    mountClothesModalToBody() {
      const mask = this.$refs.clothesItemModal
      if (!mask || mask.parentNode === document.body) {
        return
      }
      document.body.appendChild(mask)
    },
    setupMobileFormViewport() {
      if (!this.isMobile || typeof window === 'undefined') {
        return
      }
      document.body.classList.add('clothes-modal-open')
      this.updateMobileFormViewport()
      this._onMobileFormViewportChange = () => this.updateMobileFormViewport()
      window.addEventListener('resize', this._onMobileFormViewportChange)
      if (window.visualViewport) {
        window.visualViewport.addEventListener('resize', this._onMobileFormViewportChange)
        window.visualViewport.addEventListener('scroll', this._onMobileFormViewportChange)
      }
      this._onMobileFormFocusIn = event => {
        const target = event.target
        if (!target || !target.closest || !target.closest('.clothes-item-modal')) {
          return
        }
        if (!['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)) {
          return
        }
        window.setTimeout(() => {
          this.updateMobileFormViewport()
          this.scrollFormFieldIntoView(target)
        }, 360)
      }
      document.addEventListener('focusin', this._onMobileFormFocusIn)
    },
    teardownMobileFormViewport() {
      if (typeof window === 'undefined') {
        return
      }
      document.body.classList.remove('clothes-modal-open')
      if (this._onMobileFormViewportChange) {
        window.removeEventListener('resize', this._onMobileFormViewportChange)
        if (window.visualViewport) {
          window.visualViewport.removeEventListener('resize', this._onMobileFormViewportChange)
          window.visualViewport.removeEventListener('scroll', this._onMobileFormViewportChange)
        }
      }
      if (this._onMobileFormFocusIn) {
        document.removeEventListener('focusin', this._onMobileFormFocusIn)
      }
      this._onMobileFormViewportChange = null
      this._onMobileFormFocusIn = null
      this.resetMobileFormViewportStyles()
    },
    resetMobileFormViewportStyles() {
      const mask = this.$refs.clothesItemModal
      if (!mask) {
        return
      }
      ;['top', 'left', 'width', 'height', 'right', 'bottom', 'padding', 'boxSizing'].forEach(prop => {
        mask.style[prop] = ''
      })
      mask.style.removeProperty('--clothes-modal-height')
    },
    updateMobileFormViewport() {
      const mask = this.$refs.clothesItemModal
      if (!mask || !this.isMobile) {
        return
      }
      const viewport = window.visualViewport
      const layoutTop = viewport ? viewport.offsetTop : 0
      const layoutLeft = viewport ? viewport.offsetLeft : 0
      const layoutWidth = viewport ? viewport.width : window.innerWidth
      const layoutHeight = viewport ? viewport.height : window.innerHeight
      const edgeGap = 8

      mask.style.position = 'fixed'
      mask.style.top = `${layoutTop}px`
      mask.style.left = `${layoutLeft}px`
      mask.style.width = `${layoutWidth}px`
      mask.style.height = `${layoutHeight}px`
      mask.style.right = 'auto'
      mask.style.bottom = 'auto'
      mask.style.padding = `${edgeGap}px`
      mask.style.boxSizing = 'border-box'

      const formHeight = Math.max(240, layoutHeight - edgeGap * 2)
      mask.style.setProperty('--clothes-modal-height', `${formHeight}px`)
    },
    scrollFormFieldIntoView(field) {
      const form = field.closest('.clothes-modal-sheet')
      const body = form && form.querySelector('.clothes-modal-body')
      if (!body) {
        field.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
        return
      }
      const bodyRect = body.getBoundingClientRect()
      const fieldRect = field.getBoundingClientRect()
      const actions = form.querySelector('.clothes-form-actions')
      const reservedBottom = actions ? actions.offsetHeight + 12 : 12
      const visibleBottom = bodyRect.bottom - reservedBottom
      if (fieldRect.bottom > visibleBottom) {
        body.scrollTop += fieldRect.bottom - visibleBottom
      }
      if (fieldRect.top < bodyRect.top + 12) {
        body.scrollTop -= bodyRect.top + 12 - fieldRect.top
      }
    },
    triggerFileSelect() {
      this.$refs.fileInput.click()
    },
    async handleImageSelect(event) {
      const file = event.target.files && event.target.files[0]
      event.target.value = ''
      if (!file) {
        return
      }
      if (!file.type.startsWith('image/')) {
        this.uploadError = '请选择图片文件'
        return
      }
      this.uploadError = ''
      this.uploading = true
      try {
        const result = await uploadFile(file)
        this.form.imageUrl = result.url || ''
      } catch (error) {
        this.uploadError = error.message || '图片上传失败'
      } finally {
        this.uploading = false
      }
    },
    clearImage() {
      this.form.imageUrl = ''
      this.uploadError = ''
    },
    previewImage(url) {
      if (url) {
        window.open(url, '_blank')
      }
    },
    isSelected(id) {
      return this.selectedIds.includes(id)
    },
    toggleSelect(id) {
      const index = this.selectedIds.indexOf(id)
      if (index >= 0) {
        this.selectedIds.splice(index, 1)
      } else {
        this.selectedIds.push(id)
      }
    },
    toggleSelectAll(event) {
      if (event.target.checked) {
        this.selectedIds = this.items.map(item => item.id)
        return
      }
      this.selectedIds = []
    },
    async removeSelected() {
      if (!this.selectedIds.length) {
        return
      }
      const selectedItems = this.items.filter(item => this.selectedIds.includes(item.id))
      const names = selectedItems.map(item => item.clothes_name).join('、')
      const message = selectedItems.length === 1
        ? `确认删除衣服「${names}」吗？`
        : `确认删除选中的 ${selectedItems.length} 条衣服明细吗？`
      if (!window.confirm(message)) {
        return
      }
      for (const id of [...this.selectedIds]) {
        await deleteClothesItem(id)
      }
      if (this.selectedIds.length >= this.items.length && this.page > 1) {
        this.page -= 1
      }
      this.selectedIds = []
      await this.loadItems()
    },
    async submitForm() {
      if (this.uploading) {
        return
      }
      const payload = { ...this.form, imageUrl: this.form.imageUrl || '' }
      if (this.editingId) {
        await updateClothesItem(this.editingId, payload)
      } else {
        await createClothesItem(payload)
      }
      this.closeForm()
      await this.loadItems()
    }
  }
}

function emptyForm() {
  const today = new Date()
  const expireDate = new Date(today)
  expireDate.setFullYear(expireDate.getFullYear() + 3)

  return {
    clothesName: '',
    typeCode: '',
    areaCode: '',
    purchaseTime: formatDateInput(today),
    expireTime: formatDateInput(expireDate),
    imageUrl: '',
    ownerUserId: null,
    remark: ''
  }
}

function formatDateInput(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDate(value) {
  return value ? String(value).slice(0, 10) : ''
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}
</script>

<style scoped>
.clothes-item-page {
  width: 100%;
}

.clothes-image-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.clothes-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid var(--parchment);
  box-sizing: border-box;
}

.clothes-no-image {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-ghost);
  font-size: 12px;
  border-radius: 8px;
  border: 1px dashed var(--linen);
  box-sizing: border-box;
}

.clothes-item-page :deep(.admin-table td),
.clothes-item-page :deep(.admin-table th) {
  white-space: nowrap;
}

.clothes-item-page :deep(.admin-table td) {
  height: 64px;
  vertical-align: middle;
}

.clothes-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  overflow: hidden;
  background: rgba(28, 22, 16, 0.45);
  backdrop-filter: blur(6px);
}

.clothes-modal-form {
  width: min(1180px, calc(100vw - 24px));
  height: calc(100vh - 24px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 20px;
  background: var(--card-bg);
  padding: 24px 28px;
  box-shadow: 0 30px 80px rgba(28, 22, 16, 0.2), 0 0 0 1px rgba(0, 0, 0, 0.04);
}

.clothes-modal-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
  margin-top: 16px;
}

.clothes-form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  align-content: start;
}

.clothes-form-grid label {
  display: grid;
  gap: 8px;
  align-self: start;
  color: var(--ink-muted);
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.clothes-date-row {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.clothes-date-row label {
  min-width: 0;
}

.clothes-form-grid input,
.clothes-form-grid textarea,
.clothes-form-grid select {
  min-height: 42px;
  padding: 10px 14px;
  color: var(--ink);
  font-weight: 500;
  font-size: 14.5px;
  border: 1px solid var(--parchment);
  border-radius: 12px;
  background: var(--cream);
}

.clothes-form-grid textarea {
  resize: none;
  min-height: 72px;
}

.clothes-form-grid .wide {
  grid-column: 1 / -1;
}

.clothes-form-actions {
  margin-top: 0;
  padding-top: 16px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom, 0px));
  flex-shrink: 0;
  background: var(--card-bg);
  box-shadow: 0 -10px 28px rgba(28, 22, 16, 0.08);
}

.clothes-upload {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.clothes-preview {
  display: flex;
  align-items: center;
  gap: 12px;
}

.clothes-preview img {
  width: 88px;
  height: 88px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid var(--parchment);
}

.clothes-upload-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.clothes-file-input {
  display: none;
}

@media (max-width: 960px) {
  .clothes-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .clothes-modal-form {
    width: 100%;
    height: var(--clothes-modal-height, calc(100dvh - 16px));
    max-height: var(--clothes-modal-height, calc(100dvh - 16px));
    padding: 18px 16px;
  }

  .clothes-form-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .clothes-date-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .clothes-date-row input[type='date'] {
    min-width: 0;
    padding-left: 10px;
    padding-right: 10px;
    font-size: 13px;
  }

  .clothes-form-actions .ghost-btn,
  .clothes-form-actions .primary-btn {
    min-height: 44px;
  }
}
</style>
