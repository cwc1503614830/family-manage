<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Wealth Management</p>
        <h1>资产类型</h1>
        <p>维护现金、微信、支付宝、黄金、基金等财富资产类型。</p>
      </div>
      <button class="primary-btn" type="button" @click="openForm()">新增类型</button>
    </div>

    <ResizableTable :columns="columns" :rows="types" empty-text="暂无资产类型">
      <template #actions="{ row }">
        <button class="ghost-btn mini" type="button" @click="openForm(row)">编辑</button>
        <button class="ghost-btn mini danger-text" type="button" @click="removeType(row)">删除</button>
      </template>
    </ResizableTable>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeForm">
      <form class="asset-form" @submit.prevent="submitForm">
        <div class="form-head">
          <div>
            <p class="eyebrow">Wealth Type</p>
            <h2>{{ editingId ? '编辑类型' : '新增类型' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>资产类型</span>
            <input v-model.trim="form.assetType" required placeholder="现金" />
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
import { createWealthType, deleteWealthType, listWealthTypes, updateWealthType } from '../api/wealth'

export default {
  name: 'WealthTypeView',
  components: { ResizableTable },
  data() {
    return {
      types: [],
      dialogVisible: false,
      editingId: null,
      form: emptyForm(),
      columns: [
        { key: 'id', label: 'ID', width: 90 },
        { key: 'asset_type', label: '资产类型', width: 180 },
        { key: 'remark', label: '备注', width: 280 },
        { key: 'actions', label: '操作', width: 150, slot: 'actions' }
      ]
    }
  },
  created() {
    this.loadTypes()
  },
  methods: {
    async loadTypes() {
      this.types = await listWealthTypes()
    },
    openForm(type) {
      this.editingId = type ? type.id : null
      this.form = type ? { assetType: type.asset_type, remark: type.remark || '' } : emptyForm()
      this.dialogVisible = true
    },
    closeForm() {
      this.dialogVisible = false
    },
    async submitForm() {
      if (this.editingId) {
        await updateWealthType(this.editingId, this.form)
      } else {
        await createWealthType(this.form)
      }
      this.closeForm()
      await this.loadTypes()
    },
    async removeType(type) {
      if (window.confirm(`确认删除资产类型「${type.asset_type}」吗？`)) {
        await deleteWealthType(type.id)
        await this.loadTypes()
      }
    }
  }
}

function emptyForm() {
  return {
    assetType: '',
    remark: ''
  }
}
</script>
