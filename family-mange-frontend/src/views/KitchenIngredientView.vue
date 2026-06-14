<template>
  <section class="admin-page kitchen-ingredient-page">
    <div class="kitchen-ingredient-page-head">
      <h1>食材清单</h1>
      <div class="kitchen-ingredient-toolbar">
        <button class="primary-btn" type="button" @click="openForm()">新增食材</button>
        <button class="ghost-btn refresh-btn" type="button" :disabled="loading" @click="loadIngredients">
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
      :key="isMobile ? 'kitchen-ingredient-mobile' : 'kitchen-ingredient-desktop'"
      class="kitchen-ingredient-table"
      :class="{ loading: loading, 'kitchen-ingredient-mobile-table': isMobile }"
      :columns="tableColumns"
      :rows="ingredients"
      :row-clickable="isMobile"
      empty-text="暂无食材"
      @row-click="openForm"
    >
      <template #selectHeader>
        <label class="kitchen-ingredient-select-cell">
          <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
        </label>
      </template>
      <template #select="{ row }">
        <label class="kitchen-ingredient-select-cell">
          <input type="checkbox" :checked="isSelected(row.id)" @change="toggleSelect(row.id)" />
        </label>
      </template>
      <template #image="{ row }">
        <div class="clothes-image-cell">
          <img v-if="row.image_url" :src="row.image_url" alt="食材图片" class="clothes-thumb" />
          <span v-else class="clothes-no-image">无</span>
        </div>
      </template>
      <template #expire_date="{ row }">
        <span :class="{ 'danger-text': isExpired(row.expire_date) }">{{ formatDate(row.expire_date) }}</span>
      </template>
      <template #production_date="{ row }">
        {{ formatDate(row.production_date) }}
      </template>
      <template #actions="{ row }">
        <button class="ghost-btn mini" type="button" @click.stop="openForm(row)">编辑</button>
        <button class="ghost-btn mini danger-text" type="button" @click.stop="removeIngredient(row)">删除</button>
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

    <div
      v-if="dialogVisible"
      ref="ingredientModal"
      class="modal-mask kitchen-ingredient-modal"
      @click.self="closeForm"
    >
      <form class="asset-form asset-form-sheet kitchen-ingredient-form" @click.stop @submit.prevent="saveIngredient">
        <div class="form-head">
          <div>
            <p class="eyebrow">Kitchen Ingredient</p>
            <h2>{{ editingId ? '编辑食材' : '新增食材' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
        </div>

        <div class="asset-form-body">
          <div class="form-grid">
            <label>
              <span>食材名称</span>
              <input v-model.trim="form.ingredientName" placeholder="例如：鸡蛋" />
            </label>
            <label>
              <span>生产日期</span>
              <input v-model="form.productionDate" type="date" />
            </label>
            <label>
              <span>保质期（月）</span>
              <input v-model.number="form.shelfLifeMonths" type="number" step="1" placeholder="选填，如 12" />
            </label>
            <label class="wide">
              <span>食材图片</span>
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
                    class="kitchen-ingredient-file-input"
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
              <textarea v-model.trim="form.remark" rows="3"></textarea>
            </label>
          </div>
        </div>

        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closeForm">取消</button>
          <button class="primary-btn" type="button" :disabled="uploading || saving" @click="saveIngredient">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </form>
    </div>
  </section>
</template>

<script>
import ResizableTable from '../components/ResizableTable.vue'
import {
  createKitchenIngredient,
  deleteKitchenIngredient,
  listKitchenIngredients,
  updateKitchenIngredient
} from '../api/kitchen'
import { uploadFile } from '../api/upload'

export default {
  name: 'KitchenIngredientView',
  components: { ResizableTable },
  data() {
    return {
      ingredients: [],
      loading: false,
      total: 0,
      page: 1,
      pageSize: 10,
      dialogVisible: false,
      editingId: null,
      uploading: false,
      saving: false,
      uploadError: '',
      selectedIds: [],
      form: emptyForm(),
      isMobile: false,
      mobileBreakpoint: null,
      columns: [
        { key: 'id', label: 'ID', width: 70 },
        { key: 'ingredient_name', label: '食材名称', width: 140 },
        { key: 'production_date', label: '生产日期', width: 120, slot: 'production_date' },
        { key: 'shelf_life_months', label: '保质期(月)', width: 100 },
        { key: 'expire_date', label: '过期日期', width: 120, slot: 'expire_date' },
        { key: 'image', label: '图片', width: 80, slot: 'image' },
        { key: 'remark', label: '备注', width: 200 },
        { key: 'actions', label: '操作', width: 150, slot: 'actions' }
      ],
      mobileColumns: [
        { key: 'ingredient_name', label: '食材名称', width: 96 },
        { key: 'production_date', label: '生产日期', width: 108, slot: 'production_date' },
        { key: 'shelf_life_months', label: '保质期(月)', width: 88 },
        { key: 'expire_date', label: '过期日期', width: 108, slot: 'expire_date' }
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
      return this.ingredients.length > 0 && this.ingredients.every(item => this.selectedIds.includes(item.id))
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    }
  },
  created() {
    this.setupMobileListener()
    this.loadIngredients()
  },
  activated() {
    this.loadIngredients()
  },
  beforeDestroy() {
    if (this.mobileBreakpoint) {
      this.mobileBreakpoint.removeEventListener('change', this.syncMobileState)
    }
    document.body.classList.remove('kitchen-ingredient-modal-open')
    const mask = this.$refs.ingredientModal
    if (mask && mask.parentNode === document.body) {
      document.body.removeChild(mask)
    }
  },
  methods: {
    formatDate,
    setupMobileListener() {
      this.mobileBreakpoint = window.matchMedia('(max-width: 640px)')
      this.syncMobileState()
      this.mobileBreakpoint.addEventListener('change', this.syncMobileState)
    },
    syncMobileState() {
      this.isMobile = Boolean(this.mobileBreakpoint && this.mobileBreakpoint.matches)
    },
    async loadIngredients() {
      this.loading = true
      try {
        const data = await listKitchenIngredients({ page: this.page, size: this.pageSize })
        const records = Array.isArray(data) ? data : (data && data.records) || []
        this.ingredients = records.map(normalizeIngredientRow)
        this.total = Array.isArray(data) ? records.length : Number((data && data.total) || records.length)
        this.selectedIds = this.selectedIds.filter(id => this.ingredients.some(item => item.id === id))
      } catch (error) {
        this.ingredients = []
        this.total = 0
        this.selectedIds = []
        window.alert(error.message || '加载食材列表失败')
      } finally {
        this.loading = false
      }
    },
    goPage(next) {
      this.page = next
      this.loadIngredients()
    },
    changePageSize() {
      this.page = 1
      this.loadIngredients()
    },
    openForm(row) {
      this.editingId = row ? row.id : null
      this.uploading = false
      this.saving = false
      this.uploadError = ''
      this.form = row
        ? {
            ingredientName: row.ingredient_name,
            productionDate: formatDate(row.production_date),
            shelfLifeMonths: row.shelf_life_months,
            imageUrl: row.image_url || '',
            remark: row.remark || ''
          }
        : emptyForm()
      this.dialogVisible = true
      this.$nextTick(() => {
        this.mountModalToBody()
      })
    },
    closeForm() {
      this.dialogVisible = false
      document.body.classList.remove('kitchen-ingredient-modal-open')
    },
    mountModalToBody() {
      const mask = this.$refs.ingredientModal
      if (!mask) {
        return
      }
      document.body.classList.add('kitchen-ingredient-modal-open')
      if (mask.parentNode !== document.body) {
        document.body.appendChild(mask)
      }
    },
    triggerFileSelect() {
      const input = this.$refs.fileInput
      if (input) {
        input.click()
      }
    },
    async handleImageSelect(event) {
      const file = event.target.files && event.target.files[0]
      event.target.value = ''
      if (!file) return
      if (!file.type.startsWith('image/')) {
        this.uploadError = '请选择图片文件'
        return
      }
      this.uploadError = ''
      this.uploading = true
      try {
        const result = await uploadFile(file, 'kitchen')
        this.form.imageUrl = (result && (result.url || result.publicUrl)) || ''
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
    buildPayload() {
      const name = (this.form.ingredientName || '').trim()
      const months = this.form.shelfLifeMonths
      let shelfLifeMonths = null
      if (months !== '' && months !== null && months !== undefined && !Number.isNaN(Number(months))) {
        shelfLifeMonths = Number(months)
      }
      return {
        ingredientName: name,
        productionDate: this.form.productionDate || '',
        shelfLifeMonths,
        imageUrl: this.form.imageUrl || '',
        remark: this.form.remark || ''
      }
    },
    async saveIngredient() {
      if (this.uploading) {
        window.alert('图片正在上传，请稍候再保存')
        return
      }
      if (this.saving) {
        return
      }
      const payload = this.buildPayload()
      if (!payload.ingredientName) {
        window.alert('请输入食材名称')
        return
      }
      this.saving = true
      try {
        if (this.editingId) {
          await updateKitchenIngredient(this.editingId, payload)
        } else {
          await createKitchenIngredient(payload)
          this.page = 1
        }
        this.closeForm()
        await this.loadIngredients()
      } catch (error) {
        window.alert(error.message || '保存失败，请稍后重试')
      } finally {
        this.saving = false
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
        this.selectedIds = this.ingredients.map(item => item.id)
        return
      }
      this.selectedIds = []
    },
    async removeSelected() {
      if (!this.selectedIds.length) {
        return
      }
      const selectedItems = this.ingredients.filter(item => this.selectedIds.includes(item.id))
      const names = selectedItems.map(item => item.ingredient_name).join('、')
      const message = selectedItems.length === 1
        ? `确认删除食材「${names}」吗？`
        : `确认删除选中的 ${selectedItems.length} 条食材吗？`
      if (!window.confirm(message)) {
        return
      }
      try {
        for (const id of [...this.selectedIds]) {
          await deleteKitchenIngredient(id)
        }
        if (this.selectedIds.length >= this.ingredients.length && this.page > 1) {
          this.page -= 1
        }
        this.selectedIds = []
        await this.loadIngredients()
      } catch (error) {
        window.alert(error.message || '删除失败')
      }
    },
    async removeIngredient(row) {
      if (window.confirm(`确认删除食材「${row.ingredient_name}」吗？`)) {
        try {
          await deleteKitchenIngredient(row.id)
          this.selectedIds = this.selectedIds.filter(id => id !== row.id)
          await this.loadIngredients()
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
    }
  }
}

function emptyForm() {
  return {
    ingredientName: '',
    productionDate: '',
    shelfLifeMonths: null,
    imageUrl: '',
    remark: ''
  }
}

function formatDate(value) {
  if (!value) return '-'
  if (Array.isArray(value) && value.length >= 3) {
    const year = value[0]
    const month = String(value[1]).padStart(2, '0')
    const day = String(value[2]).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  const text = String(value)
  if (text.includes('T')) {
    return text.slice(0, 10)
  }
  return text.length >= 10 ? text.slice(0, 10) : text
}

function normalizeIngredientRow(row) {
  return {
    ...row,
    id: row.id,
    ingredient_name: row.ingredient_name || row.ingredientName || '',
    production_date: row.production_date || row.productionDate,
    shelf_life_months: row.shelf_life_months != null ? row.shelf_life_months : row.shelfLifeMonths,
    expire_date: row.expire_date || row.expireDate,
    image_url: row.image_url || row.imageUrl || '',
    remark: row.remark || ''
  }
}
</script>

<style scoped>
.kitchen-ingredient-page-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.kitchen-ingredient-page-head h1 {
  margin: 0;
}

.kitchen-ingredient-toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.kitchen-ingredient-table.loading {
  opacity: 0.6;
  pointer-events: none;
}
.kitchen-ingredient-file-input {
  display: none;
}

.kitchen-ingredient-form .clothes-upload-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.kitchen-ingredient-page :deep(.clothes-image-cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.kitchen-ingredient-page :deep(.clothes-thumb) {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--parchment);
}
</style>
