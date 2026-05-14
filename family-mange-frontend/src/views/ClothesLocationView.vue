<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Clothes Management</p>
        <h1>存放位置</h1>
        <p>维护衣柜、箱子等衣服所在区域编码与名称。</p>
      </div>
      <button class="primary-btn" type="button" @click="openForm()">新增位置</button>
    </div>

    <ResizableTable :columns="columns" :rows="locations" empty-text="暂无存放位置">
      <template #actions="{ row }">
        <button class="ghost-btn mini" type="button" @click="openForm(row)">编辑</button>
        <button class="ghost-btn mini danger-text" type="button" @click="removeLocation(row)">删除</button>
      </template>
    </ResizableTable>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeForm">
      <form class="asset-form" @submit.prevent="submitForm">
        <div class="form-head">
          <div>
            <p class="eyebrow">Storage Location</p>
            <h2>{{ editingId ? '编辑位置' : '新增位置' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>所在区域编码</span>
            <input v-model.trim="form.areaCode" required placeholder="BOX1-ZWYG" />
          </label>
          <label>
            <span>所在区域名称</span>
            <input v-model.trim="form.areaName" required placeholder="1号箱子-主卧衣柜" />
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
import {
  createClothesLocation,
  deleteClothesLocation,
  listClothesLocations,
  updateClothesLocation
} from '../api/clothes'

export default {
  name: 'ClothesLocationView',
  components: { ResizableTable },
  data() {
    return {
      locations: [],
      dialogVisible: false,
      editingId: null,
      form: emptyForm(),
      columns: [
        { key: 'id', label: 'ID', width: 90 },
        { key: 'area_code', label: '所在区域编码', width: 180 },
        { key: 'area_name', label: '所在区域名称', width: 220 },
        { key: 'remark', label: '备注', width: 260 },
        { key: 'actions', label: '操作', width: 150, slot: 'actions' }
      ]
    }
  },
  created() {
    this.loadLocations()
  },
  methods: {
    async loadLocations() {
      this.locations = await listClothesLocations()
    },
    openForm(location) {
      this.editingId = location ? location.id : null
      this.form = location
        ? { areaCode: location.area_code, areaName: location.area_name, remark: location.remark || '' }
        : emptyForm()
      this.dialogVisible = true
    },
    closeForm() {
      this.dialogVisible = false
    },
    async submitForm() {
      if (this.editingId) {
        await updateClothesLocation(this.editingId, this.form)
      } else {
        await createClothesLocation(this.form)
      }
      this.closeForm()
      await this.loadLocations()
    },
    async removeLocation(location) {
      if (window.confirm(`确认删除存放位置「${location.area_name}」吗？`)) {
        await deleteClothesLocation(location.id)
        await this.loadLocations()
      }
    }
  }
}

function emptyForm() {
  return {
    areaCode: '',
    areaName: '',
    remark: ''
  }
}
</script>
