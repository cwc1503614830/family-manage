<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Wealth Management</p>
        <h1>资产总览</h1>
        <p>维护家庭财富资产、类型、金额和持有人。</p>
      </div>
      <div class="wealth-total-panel">
        <span>总资产</span>
        <strong>{{ moneyText(totalAssets) }}</strong>
      </div>
      <button class="primary-btn" type="button" @click="openForm()">新增资产</button>
    </div>

    <div class="menu-toolbar menu-filter">
      <button class="ghost-btn refresh-btn" type="button" @click="bootstrap">刷新</button>
    </div>

    <ResizableTable :columns="columns" :rows="assets" empty-text="暂无财富资产">
      <template #amount="{ row }">
        <strong>{{ moneyText(row.amount) }}</strong>
      </template>
      <template #actions="{ row }">
        <button class="ghost-btn mini" type="button" @click="openForm(row)">编辑</button>
        <button class="ghost-btn mini danger-text" type="button" @click="removeAsset(row)">删除</button>
      </template>
    </ResizableTable>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeForm">
      <form class="asset-form" @submit.prevent="submitForm">
        <div class="form-head">
          <div>
            <p class="eyebrow">Wealth Asset</p>
            <h2>{{ editingId ? '编辑资产' : '新增资产' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
        </div>

        <div class="form-grid">
          <label>
            <span>资产名称</span>
            <input v-model.trim="form.assetName" :disabled="Boolean(editingId)" required placeholder="例如：微信零钱" />
          </label>
          <label>
            <span>资产类型</span>
            <select v-model.number="form.typeId" required>
              <option :value="null">请选择</option>
              <option v-for="type in types" :key="type.id" :value="type.id">
                {{ type.asset_type }}
              </option>
            </select>
          </label>
          <label>
            <span>金额</span>
            <input v-model.number="form.amount" required min="0" step="0.01" type="number" />
          </label>
          <label>
            <span>入账日期</span>
            <input v-model="form.recordDate" type="date" />
          </label>
          <label>
            <span>持有人</span>
            <select v-model.number="form.ownerUserId" required>
              <option :value="null">请选择</option>
              <option v-for="user in users" :key="user.id" :value="user.id">
                {{ user.display_name || user.username }}
              </option>
            </select>
          </label>
          <label class="wide">
            <span>备注</span>
            <textarea v-model.trim="form.remark" rows="3" placeholder="记录账户、归属或资产说明"></textarea>
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
  createWealthAsset,
  deleteWealthAsset,
  getWealthOptions,
  listWealthAssets,
  updateWealthAsset
} from '../api/wealth'

export default {
  name: 'WealthOverviewView',
  components: { ResizableTable },
  data() {
    return {
      assets: [],
      types: [],
      users: [],
      dialogVisible: false,
      editingId: null,
      form: emptyForm(),
      columns: [
        { key: 'id', label: 'ID', width: 90 },
        { key: 'asset_name', label: '资产名称', width: 180 },
        { key: 'asset_type', label: '资产类型', width: 140 },
        { key: 'amount', label: '金额', width: 140, slot: 'amount' },
        { key: 'record_date', label: '入账日期', width: 120, formatter: formatDate },
        { key: 'owner_name', label: '持有人', width: 120 },
        { key: 'remark', label: '备注', width: 220 },
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
  computed: {
    totalAssets() {
      return this.assets.reduce((total, asset) => total + Number(asset.amount || 0), 0)
    }
  },
  methods: {
    async bootstrap() {
      await Promise.all([this.loadAssets(), this.loadOptions()])
    },
    async loadAssets() {
      this.assets = await listWealthAssets()
    },
    async loadOptions() {
      const options = await getWealthOptions()
      this.types = options.types || []
      this.users = options.users || []
    },
    async openForm(asset) {
      await this.loadOptions()
      this.editingId = asset ? asset.id : null
      this.form = asset
        ? {
            assetName: asset.asset_name,
            typeId: asset.type_id,
            amount: Number(asset.amount || 0),
            recordDate: formatDate(asset.record_date),
            ownerUserId: asset.owner_user_id,
            remark: asset.remark || ''
          }
        : emptyForm()
      this.dialogVisible = true
    },
    closeForm() {
      this.dialogVisible = false
    },
    async submitForm() {
      const payload = { ...this.form }
      if (this.editingId) {
        await updateWealthAsset(this.editingId, payload)
      } else {
        await createWealthAsset(payload)
      }
      this.closeForm()
      await this.loadAssets()
    },
    async removeAsset(asset) {
      if (window.confirm(`确认删除资产「${asset.asset_name}」吗？`)) {
        await deleteWealthAsset(asset.id)
        await this.loadAssets()
      }
    },
    moneyText(value) {
      return Number(value || 0).toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })
    }
  }
}

function emptyForm() {
  return {
    assetName: '',
    typeId: null,
    amount: 0,
    recordDate: new Date().toISOString().slice(0, 10),
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
