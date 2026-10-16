<template>
  <section class="admin-page goods-item-page">
    <div class="goods-item-page-head">
      <h1>物品明细</h1>
      <div class="goods-item-toolbar">
        <button class="primary-btn" type="button" @click="openForm()">新增物品</button>
        <button class="ghost-btn refresh-btn" type="button" :disabled="loading" @click="loadItems">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
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

    <ResizableTable
      :key="isMobile ? 'goods-item-mobile' : 'goods-item-desktop'"
      class="goods-item-table"
      :class="{ loading: loading, 'goods-item-mobile-table': isMobile }"
      :columns="tableColumns"
      :rows="items"
      :row-clickable="isMobile"
      empty-text="暂无物品"
      @row-click="openForm"
    >
      <template #selectHeader>
        <label class="goods-item-select-cell">
          <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
        </label>
      </template>
      <template #select="{ row }">
        <label class="goods-item-select-cell">
          <input type="checkbox" :checked="isSelected(row.id)" @change="toggleSelect(row.id)" />
        </label>
      </template>
      <template #image="{ row }">
        <div class="clothes-image-cell">
          <img v-if="row.image_url" :src="row.image_url" alt="物品图片" class="clothes-thumb" @click.stop="previewImage(row.image_url)" />
          <span v-else class="clothes-no-image">无</span>
        </div>
      </template>
      <template #purchase_time="{ row }">
        {{ formatDate(row.purchase_time) }}
      </template>
      <template #expire_time="{ row }">
        <span :class="{ 'danger-text': isExpired(row.expire_time) }">{{ formatDate(row.expire_time) }}</span>
      </template>
      <template #created_at="{ row }">
        {{ formatDateTime(row.created_at) }}
      </template>
      <template #updated_at="{ row }">
        {{ formatDateTime(row.updated_at) }}
      </template>
      <template #actions="{ row }">
        <button class="ghost-btn mini" type="button" @click.stop="openForm(row)">编辑</button>
        <button class="ghost-btn mini danger-text" type="button" @click.stop="removeItem(row)">删除</button>
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

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeForm">
      <form class="asset-form" @submit.prevent="submitForm">
        <div class="form-head">
          <div>
            <p class="eyebrow">Goods Item</p>
            <h2>{{ editingId ? '编辑物品' : '新增物品' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>物品名称</span>
            <input
              v-model.trim="form.itemName"
              :disabled="Boolean(editingId)"
              required
              placeholder="例如：备用钥匙"
            />
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
            <span>买入时间</span>
            <input v-model="form.purchaseTime" :disabled="Boolean(editingId)" type="date" />
          </label>
          <label>
            <span>过期时间</span>
            <input v-model="form.expireTime" type="date" />
          </label>
          <label class="wide">
            <span>物品图片</span>
            <div class="goods-upload">
              <div v-if="form.imageUrl" class="goods-preview">
                <img :src="form.imageUrl" alt="预览图" @click.stop="previewImage(form.imageUrl)" />
                <button class="ghost-btn mini" type="button" @click="clearImage">移除</button>
              </div>
              <div class="goods-upload-actions">
                <input
                  ref="fileInput"
                  type="file"
                  accept="image/*"
                  class="goods-file-input"
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
            <textarea v-model.trim="form.remark" rows="3" placeholder="记录用途、规格或查找提示"></textarea>
          </label>
        </div>
        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closeForm">取消</button>
          <button class="primary-btn" type="submit" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </form>
    </div>

    <div v-if="previewUrl" class="modal-mask album-preview-mask" @click="previewUrl = ''">
      <button class="album-preview-close" @click.stop="previewUrl = ''">✕</button>
      <img :src="previewUrl" alt="预览" class="album-preview-image" @click.stop />
    </div>
  </section>
</template>

<script>
import ResizableTable from '../components/ResizableTable.vue'
import {
  createGoodsItem,
  deleteGoodsItem,
  getGoodsOptions,
  listGoodsItems,
  updateGoodsItem
} from '../api/goods'
import { uploadFile } from '../api/upload'

export default {
  name: 'GoodsItemView',
  components: { ResizableTable },
  data() {
    return {
      items: [],
      locations: [],
      loading: false,
      saving: false,
      total: 0,
      page: 1,
      pageSize: 10,
      dialogVisible: false,
      editingId: null,
      uploading: false,
      uploadError: '',
      selectedIds: [],
      previewUrl: '',
      form: emptyForm(),
      isMobile: false,
      mobileBreakpoint: null,
      columns: [
        { key: 'id', label: 'ID', width: 70 },
        { key: 'item_name', label: '物品名称', width: 140 },
        { key: 'area_name', label: '存放位置', width: 150 },
        { key: 'purchase_time', label: '买入时间', width: 120, slot: 'purchase_time' },
        { key: 'expire_time', label: '过期时间', width: 120, slot: 'expire_time' },
        { key: 'image', label: '图片', width: 80, slot: 'image' },
        { key: 'remark', label: '备注', width: 180 },
        { key: 'created_by', label: '创建人', width: 100 },
        { key: 'created_at', label: '创建时间', width: 160, slot: 'created_at' },
        { key: 'updated_by', label: '更新人', width: 100 },
        { key: 'updated_at', label: '更新时间', width: 160, slot: 'updated_at' },
        { key: 'actions', label: '操作', width: 150, slot: 'actions' }
      ],
      mobileColumns: [
        { key: 'image', label: '图片', width: 48, slot: 'image' },
        { key: 'item_name', label: '物品名称', width: 96 },
        { key: 'area_name', label: '存放位置', width: 108 }
      ]
    }
  },
  computed: {
    tableColumns() {
      const selectColumn = { key: '_select', label: '', width: 44, slot: 'select', headerSlot: 'selectHeader' }
      const dataColumns = this.isMobile ? this.mobileColumns : this.columns
      return [selectColumn, ...dataColumns]
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
  activated() {
    this.bootstrap()
  },
  beforeDestroy() {
    if (this.mobileBreakpoint) {
      this.mobileBreakpoint.removeEventListener('change', this.syncMobileState)
    }
  },
  mounted() {
    this._onKeydown = (e) => {
      if (e.key === 'Escape' && this.previewUrl) {
        this.previewUrl = ''
      }
    }
    window.addEventListener('keydown', this._onKeydown)
  },
  beforeUnmount() {
    if (this._onKeydown) {
      window.removeEventListener('keydown', this._onKeydown)
    }
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
    async loadOptions() {
      const options = await getGoodsOptions()
      this.locations = options.locations || []
    },
    async loadItems() {
      this.loading = true
      try {
        const result = await listGoodsItems({ page: this.page, size: this.pageSize })
        this.items = (result.records || []).map(normalizeItemRow)
        this.total = Number(result.total || 0)
        this.selectedIds = this.selectedIds.filter(id => this.items.some(item => item.id === id))
      } catch (error) {
        this.items = []
        this.total = 0
        this.selectedIds = []
        window.alert(error.message || '加载物品列表失败')
      } finally {
        this.loading = false
      }
    },
    goPage(next) {
      this.page = next
      this.loadItems()
    },
    changePageSize() {
      this.page = 1
      this.loadItems()
    },
    async openForm(row) {
      await this.loadOptions()
      this.editingId = row ? row.id : null
      this.saving = false
      this.uploading = false
      this.uploadError = ''
      this.form = row
        ? {
            itemName: row.item_name,
            areaCode: row.area_code,
            purchaseTime: formatDateInput(row.purchase_time),
            expireTime: formatDateInput(row.expire_time),
            imageUrl: row.image_url || '',
            remark: row.remark || ''
          }
        : emptyForm()
      this.dialogVisible = true
    },
    closeForm() {
      this.dialogVisible = false
      this.uploading = false
      this.uploadError = ''
    },
    buildCreatePayload() {
      return {
        itemName: (this.form.itemName || '').trim(),
        areaCode: this.form.areaCode,
        purchaseTime: this.form.purchaseTime || '',
        expireTime: this.form.expireTime || '',
        imageUrl: this.form.imageUrl || '',
        remark: this.form.remark || ''
      }
    },
    buildUpdatePayload() {
      return {
        areaCode: this.form.areaCode,
        expireTime: this.form.expireTime || '',
        imageUrl: this.form.imageUrl || '',
        remark: this.form.remark || ''
      }
    },
    async submitForm() {
      if (this.saving || this.uploading) {
        return
      }
      if (!this.form.areaCode) {
        window.alert('请选择存放位置')
        return
      }
      if (!this.editingId && !(this.form.itemName || '').trim()) {
        window.alert('请输入物品名称')
        return
      }
      if (!this.locations.length) {
        window.alert('请先在「存放位置」中维护区域')
        return
      }
      this.saving = true
      try {
        if (this.editingId) {
          await updateGoodsItem(this.editingId, this.buildUpdatePayload())
        } else {
          await createGoodsItem(this.buildCreatePayload())
          this.page = 1
        }
        this.closeForm()
        await this.loadItems()
      } catch (error) {
        window.alert(error.message || '保存失败，请稍后重试')
      } finally {
        this.saving = false
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
        const result = await uploadFile(file, 'goods')
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
        this.previewUrl = url
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
      const names = selectedItems.map(item => item.item_name).join('、')
      const message = selectedItems.length === 1
        ? `确认删除物品「${names}」吗？`
        : `确认删除选中的 ${selectedItems.length} 条物品吗？`
      if (!window.confirm(message)) {
        return
      }
      try {
        for (const id of [...this.selectedIds]) {
          await deleteGoodsItem(id)
        }
        if (this.selectedIds.length >= this.items.length && this.page > 1) {
          this.page -= 1
        }
        this.selectedIds = []
        await this.loadItems()
      } catch (error) {
        window.alert(error.message || '删除失败')
      }
    },
    async removeItem(row) {
      if (window.confirm(`确认删除物品「${row.item_name}」吗？`)) {
        try {
          await deleteGoodsItem(row.id)
          this.selectedIds = this.selectedIds.filter(id => id !== row.id)
          await this.loadItems()
        } catch (error) {
          window.alert(error.message || '删除失败')
        }
      }
    },
    isExpired(dateStr) {
      if (!dateStr) return false
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      return new Date(dateStr) < today
    },
    formatDate,
    formatDateTime
  }
}

function emptyForm() {
  return {
    itemName: '',
    areaCode: '',
    purchaseTime: '',
    expireTime: '',
    imageUrl: '',
    remark: ''
  }
}

function formatDate(value) {
  if (!value) return '-'
  const text = String(value)
  if (text.includes('T')) {
    return text.slice(0, 10)
  }
  return text.length >= 10 ? text.slice(0, 10) : text
}

function formatDateInput(value) {
  if (!value) return ''
  return formatDate(value) === '-' ? '' : formatDate(value)
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}

function normalizeItemRow(row) {
  return {
    ...row,
    item_name: row.item_name || row.itemName || '',
    area_code: row.area_code || row.areaCode || '',
    area_name: row.area_name || row.areaName || '',
    purchase_time: row.purchase_time || row.purchaseTime,
    expire_time: row.expire_time || row.expireTime,
    image_url: row.image_url || row.imageUrl || '',
    created_by: row.created_by || row.createdBy || '',
    updated_by: row.updated_by || row.updatedBy || ''
  }
}
</script>

<style scoped>
.goods-item-page-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.goods-item-page-head h1 {
  margin: 0;
}

.goods-item-toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.goods-item-table.loading {
  opacity: 0.6;
  pointer-events: none;
}

.goods-upload {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.goods-preview {
  display: flex;
  align-items: center;
  gap: 12px;
}

.goods-preview img {
  width: 88px;
  height: 88px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid var(--parchment);
}

.goods-upload-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.goods-file-input {
  display: none;
}

.clothes-image-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
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

.goods-item-page :deep(.admin-table td),
.goods-item-page :deep(.admin-table th) {
  white-space: nowrap;
}

.goods-item-page :deep(.admin-table td) {
  height: 64px;
  vertical-align: middle;
}
</style>
