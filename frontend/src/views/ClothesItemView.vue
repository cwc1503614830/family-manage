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
            <span>备注</span>
            <textarea v-model.trim="form.remark" rows="3" placeholder="记录换季、清洗或特殊说明"></textarea>
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
import {
  createClothesItem,
  deleteClothesItem,
  getClothesOptions,
  listClothesItems,
  updateClothesItem
} from '../api/clothes'

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
      form: emptyForm(),
      columns: [
        { key: 'clothes_name', label: '衣服名称', width: 150 },
        { key: 'type_code', label: '类型编码', width: 120 },
        { key: 'type_name', label: '类型名称', width: 120 },
        { key: 'area_code', label: '区域编码', width: 140 },
        { key: 'area_name', label: '区域名称', width: 160 },
        { key: 'purchase_time', label: '买入时间', width: 120, formatter: formatDate },
        { key: 'expire_time', label: '过期时间', width: 120, formatter: formatDate },
        { key: 'image_url', label: '图片地址', width: 160 },
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
    },
    async submitForm() {
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
  return {
    clothesName: '',
    typeCode: '',
    areaCode: '',
    purchaseTime: new Date().toISOString().slice(0, 10),
    expireTime: '',
    imageUrl: '',
    ownerUserId: null,
    remark: ''
  }
}

function formatDate(value) {
  return value ? String(value).slice(0, 10) : ''
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}
</script>
