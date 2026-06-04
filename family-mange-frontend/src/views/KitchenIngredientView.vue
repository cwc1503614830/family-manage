<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Kitchen Management</p>
        <h1>食材清单</h1>
        <p>维护食材信息，过期日期根据生产日期与保质期（月）自动计算。</p>
      </div>
      <button class="primary-btn" type="button" @click="openForm()">新增食材</button>
    </div>

    <ResizableTable :columns="columns" :rows="ingredients" empty-text="暂无食材">
      <template #image="{ row }">
        <div class="clothes-image-cell">
          <img v-if="row.image_url" :src="row.image_url" alt="食材图片" class="clothes-thumb" />
          <span v-else class="clothes-no-image">无</span>
        </div>
      </template>
      <template #expire_date="{ row }">
        <span :class="{ 'danger-text': isExpired(row.expire_date) }">{{ formatDate(row.expire_date) }}</span>
      </template>
      <template #actions="{ row }">
        <button class="ghost-btn mini" type="button" @click="openForm(row)">编辑</button>
        <button class="ghost-btn mini danger-text" type="button" @click="removeIngredient(row)">删除</button>
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
            <p class="eyebrow">Kitchen Ingredient</p>
            <h2>{{ editingId ? '编辑食材' : '新增食材' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>食材名称</span>
            <input v-model.trim="form.ingredientName" required placeholder="例如：鸡蛋" />
          </label>
          <label>
            <span>生产日期</span>
            <input v-model="form.productionDate" type="date" />
          </label>
          <label>
            <span>保质期（月）</span>
            <input v-model.number="form.shelfLifeMonths" type="number" min="1" placeholder="12" />
          </label>
          <label class="wide">
            <span>食材图片</span>
            <div class="clothes-upload">
              <div v-if="form.imageUrl" class="clothes-preview">
                <img :src="form.imageUrl" alt="预览图" />
                <button class="ghost-btn mini" type="button" @click="clearImage">移除</button>
              </div>
              <div class="clothes-upload-actions">
                <input ref="fileInput" type="file" accept="image/*" class="clothes-file-input" @change="handleImageSelect" />
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
        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closeForm">取消</button>
          <button class="primary-btn" type="submit" :disabled="uploading">保存</button>
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
      total: 0,
      page: 1,
      pageSize: 10,
      dialogVisible: false,
      editingId: null,
      uploading: false,
      uploadError: '',
      form: emptyForm(),
      columns: [
        { key: 'id', label: 'ID', width: 70 },
        { key: 'ingredient_name', label: '食材名称', width: 140 },
        { key: 'production_date', label: '生产日期', width: 120, formatter: formatDate },
        { key: 'shelf_life_months', label: '保质期(月)', width: 100 },
        { key: 'expire_date', label: '过期日期', width: 120, slot: 'expire_date' },
        { key: 'image', label: '图片', width: 80, slot: 'image' },
        { key: 'remark', label: '备注', width: 200 },
        { key: 'actions', label: '操作', width: 150, slot: 'actions' }
      ]
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    }
  },
  created() {
    this.loadIngredients()
  },
  methods: {
    async loadIngredients() {
      const data = await listKitchenIngredients({ page: this.page, size: this.pageSize })
      this.ingredients = data.records || []
      this.total = data.total || 0
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
    },
    closeForm() {
      this.dialogVisible = false
    },
    triggerFileSelect() {
      this.$refs.fileInput.click()
    },
    async handleImageSelect(event) {
      const file = event.target.files && event.target.files[0]
      event.target.value = ''
      if (!file) return
      if (!file.type.startsWith('image/')) {
        this.uploadError = '请选择图片文件'
        return
      }
      if (file.size > 10 * 1024 * 1024) {
        this.uploadError = '图片大小不能超过 10MB'
        return
      }
      this.uploadError = ''
      this.uploading = true
      try {
        const result = await uploadFile(file, 'kitchen')
        this.form.imageUrl = result.url || result.publicUrl || ''
      } catch (error) {
        this.uploadError = error.message || '图片上传失败'
      } finally {
        this.uploading = false
      }
    },
    clearImage() {
      this.form.imageUrl = ''
    },
    async submitForm() {
      if (this.editingId) {
        await updateKitchenIngredient(this.editingId, this.form)
      } else {
        await createKitchenIngredient(this.form)
      }
      this.closeForm()
      await this.loadIngredients()
    },
    async removeIngredient(row) {
      if (window.confirm(`确认删除食材「${row.ingredient_name}」吗？`)) {
        await deleteKitchenIngredient(row.id)
        await this.loadIngredients()
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
  if (!value) return ''
  const text = String(value)
  return text.length >= 10 ? text.slice(0, 10) : text
}
</script>
