<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Clothes Management</p>
        <h1>衣服明细</h1>
        <p>登记家庭成员衣服，并维护类型、位置、拥有者和过期时间。</p>
      </div>
      <button class="primary-btn" type="button" @click="openForm()">新增衣服</button>
    </div>

    <div class="menu-toolbar menu-filter">
      <button class="ghost-btn refresh-btn" type="button" @click="bootstrap">刷新</button>
    </div>

    <ResizableTable :columns="columns" :rows="items" empty-text="暂无衣服明细">
      <template #image="{ row }">
        <div class="clothes-image-cell">
          <img
            v-if="row.image_url"
            :src="row.image_url"
            alt="衣服图片"
            class="clothes-thumb"
            @click="previewImage(row.image_url)"
          />
          <span v-else class="clothes-no-image">无</span>
        </div>
      </template>
      <template #actions="{ row }">
        <button class="ghost-btn mini" type="button" @click="openForm(row)">编辑</button>
        <button class="ghost-btn mini danger-text" type="button" @click="removeItem(row)">删除</button>
      </template>
    </ResizableTable>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeForm">
      <form class="asset-form" @submit.prevent="submitForm">
        <div class="form-head">
          <div>
            <p class="eyebrow">Clothes Item</p>
            <h2>{{ editingId ? '编辑衣服' : '新增衣服' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
        </div>

        <div class="form-grid">
          <label>
            <span>衣服名称</span>
            <input v-model.trim="form.clothesName" :disabled="Boolean(editingId)" required placeholder="例如：黑色羽绒服" />
          </label>
          <label>
            <span>衣服类型</span>
            <select v-model="form.typeCode" required>
              <option value="">请选择</option>
              <option v-for="type in types" :key="type.type_code" :value="type.type_code">
                {{ type.type_code }} - {{ type.type_name }}
              </option>
            </select>
          </label>
          <label>
            <span>存放位置</span>
            <select v-model="form.areaCode" required>
              <option value="">请选择</option>
              <option v-for="location in locations" :key="location.area_code" :value="location.area_code">
                {{ location.area_code }} - {{ location.area_name }}
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
          <label>
            <span>买入时间</span>
            <input v-model="form.purchaseTime" :disabled="Boolean(editingId)" type="date" />
          </label>
          <label>
            <span>过期时间</span>
            <input v-model="form.expireTime" type="date" />
          </label>
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
            <textarea v-model.trim="form.remark" rows="3" placeholder="记录换季、清洗或特殊说明"></textarea>
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
      types: [],
      locations: [],
      users: [],
      dialogVisible: false,
      editingId: null,
      uploading: false,
      uploadError: '',
      form: emptyForm(),
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
        { key: 'updated_at', label: '更新时间', width: 160, formatter: formatDateTime },
        { key: 'actions', label: '操作', width: 150, slot: 'actions' }
      ]
    }
  },
  created() {
    this.bootstrap()
  },
  methods: {
    async bootstrap() {
      await Promise.all([this.loadItems(), this.loadOptions()])
    },
    async loadItems() {
      this.items = await listClothesItems()
    },
    async loadOptions() {
      const options = await getClothesOptions()
      this.types = options.types || []
      this.locations = options.locations || []
      this.users = options.users || []
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
    },
    closeForm() {
      this.dialogVisible = false
      this.uploadError = ''
      this.uploading = false
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
      if (file.size > 10 * 1024 * 1024) {
        this.uploadError = '图片大小不能超过 10MB'
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
    },
    async removeItem(item) {
      if (window.confirm(`确认删除衣服「${item.clothes_name}」吗？`)) {
        await deleteClothesItem(item.id)
        await this.loadItems()
      }
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
}

.clothes-no-image {
  color: var(--ink-ghost);
  font-size: 12px;
}

.clothes-upload {
  display: grid;
  gap: 12px;
}

.clothes-preview {
  display: flex;
  align-items: center;
  gap: 12px;
}

.clothes-preview img {
  width: 96px;
  height: 96px;
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
</style>
