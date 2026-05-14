<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Clothes Management</p>
        <h1>衣服类型</h1>
        <p>维护短袖、夏裤等衣服类型编码与名称。</p>
      </div>
      <button class="primary-btn" type="button" @click="openForm()">新增类型</button>
    </div>

    <ResizableTable :columns="columns" :rows="types" empty-text="暂无衣服类型">
      <template #actions="{ row }">
        <button class="ghost-btn mini" type="button" @click="openForm(row)">编辑</button>
        <button class="ghost-btn mini danger-text" type="button" @click="removeType(row)">删除</button>
      </template>
    </ResizableTable>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeForm">
      <form class="asset-form" @submit.prevent="submitForm">
        <div class="form-head">
          <div>
            <p class="eyebrow">Clothes Type</p>
            <h2>{{ editingId ? '编辑类型' : '新增类型' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>衣服类型编码</span>
            <input v-model.trim="form.typeCode" required placeholder="DuanXiu" />
          </label>
          <label>
            <span>衣服类型名称</span>
            <input v-model.trim="form.typeName" required placeholder="短袖" />
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
import { createClothesType, deleteClothesType, listClothesTypes, updateClothesType } from '../api/clothes'

export default {
  name: 'ClothesTypeView',
  components: { ResizableTable },
  data() {
    return {
      types: [],
      dialogVisible: false,
      editingId: null,
      form: emptyForm(),
      columns: [
        { key: 'id', label: 'ID', width: 90 },
        { key: 'type_code', label: '衣服类型编码', width: 180 },
        { key: 'type_name', label: '衣服类型名称', width: 180 },
        { key: 'remark', label: '备注', width: 260 },
        { key: 'actions', label: '操作', width: 150, slot: 'actions' }
      ]
    }
  },
  created() {
    this.loadTypes()
  },
  methods: {
    async loadTypes() {
      this.types = await listClothesTypes()
    },
    openForm(type) {
      this.editingId = type ? type.id : null
      this.form = type
        ? { typeCode: type.type_code, typeName: type.type_name, remark: type.remark || '' }
        : emptyForm()
      this.dialogVisible = true
    },
    closeForm() {
      this.dialogVisible = false
    },
    async submitForm() {
      if (this.editingId) {
        await updateClothesType(this.editingId, this.form)
      } else {
        await createClothesType(this.form)
      }
      this.closeForm()
      await this.loadTypes()
    },
    async removeType(type) {
      if (window.confirm(`确认删除衣服类型「${type.type_name}」吗？`)) {
        await deleteClothesType(type.id)
        await this.loadTypes()
      }
    }
  }
}

function emptyForm() {
  return {
    typeCode: '',
    typeName: '',
    remark: ''
  }
}
</script>
