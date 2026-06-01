<template>
  <section class="admin-page wealth-overview-page">
    <div class="wealth-page-head">
      <h1>资产总览</h1>
      <div class="wealth-head-toolbar">
        <div class="wealth-summary-grid">
          <div class="wealth-total-panel">
            <span>总资产</span>
            <strong>{{ moneyText(totalAssets) }}</strong>
          </div>
          <div class="wealth-total-panel wealth-disposable-panel">
            <span>可支配资产</span>
            <strong>{{ moneyText(disposableAssets) }}</strong>
          </div>
        </div>
        <div class="wealth-head-actions">
          <button class="primary-btn wealth-add-btn" type="button" @click="openForm()">新增资产</button>
          <button class="ghost-btn refresh-btn" type="button" @click="bootstrap">刷新</button>
        </div>
      </div>
    </div>

    <ResizableTable
      :key="isMobile ? 'wealth-mobile' : 'wealth-desktop'"
      :class="{ 'wealth-mobile-table': isMobile }"
      :columns="tableColumns"
      :rows="sortedAssets"
      :row-clickable="isMobile"
      empty-text="暂无财富资产"
      @row-click="openForm"
    >
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
      isMobile: false,
      mobileBreakpoint: null,
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
      ],
      mobileColumns: [
        { key: 'asset_name', label: '资产名称', width: 120 },
        { key: 'asset_type', label: '资产类型', width: 96 },
        { key: 'amount', label: '金额', width: 108, slot: 'amount' }
      ]
    }
  },
  created() {
    this.bootstrap()
    this.setupMobileListener()
  },
  beforeDestroy() {
    if (this.mobileBreakpoint) {
      this.mobileBreakpoint.removeEventListener('change', this.syncMobileState)
    }
  },
  computed: {
    tableColumns() {
      return this.isMobile ? this.mobileColumns : this.columns
    },
    sortedAssets() {
      return sortAssetsByType(this.assets)
    },
    totalAssets() {
      return this.sortedAssets.reduce((total, asset) => total + Number(asset.amount || 0), 0)
    },
    disposableAssets() {
      return this.sortedAssets.reduce((total, asset) => {
        if (!isDisposableAsset(asset)) {
          return total
        }
        return total + Number(asset.amount || 0)
      }, 0)
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

const DISPOSABLE_ASSET_TYPES = new Set(['微信', '支付宝', '银行卡', '基金', '现金'])

function isDisposableAsset(asset) {
  return DISPOSABLE_ASSET_TYPES.has(String(asset.asset_type || '').trim())
}

function sortAssetsByType(assets) {
  return [...assets].sort((left, right) => {
    const typeCompare = String(left.asset_type || '').localeCompare(String(right.asset_type || ''), 'zh-CN')
    if (typeCompare !== 0) {
      return typeCompare
    }
    return Number(right.id || 0) - Number(left.id || 0)
  })
}
</script>
