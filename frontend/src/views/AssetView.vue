<template>
  <section>
    <section class="page-head">
      <div>
        <p class="eyebrow">Family Asset Console</p>
        <h1>家庭资产管理</h1>
        <p>管理家庭里的食品、日用品、家电和重要物品。</p>
      </div>
      <button class="primary-btn" type="button" @click="openCreate">
        <span>＋</span>
        <span>新增资产</span>
      </button>
    </section>

    <section class="stats-grid">
      <article class="stat-card">
        <strong>{{ summary.totalCategories || 0 }}</strong>
        <span>总分类</span>
      </article>
      <article class="stat-card danger">
        <strong>{{ summary.expiredAssets || 0 }}</strong>
        <span>已过期</span>
      </article>
      <article class="stat-card warning">
        <strong>{{ summary.lowStockAssets || 0 }}</strong>
        <span>库存不足</span>
      </article>
      <article class="stat-card calm">
        <strong>{{ summary.totalAssets || assets.length }}</strong>
        <span>登记资产</span>
      </article>
    </section>

    <section class="toolbar">
      <label class="search-box">
        <span>⌕</span>
        <input
          v-model.trim="query.keyword"
          type="search"
          placeholder="搜索物品名称、标签或位置..."
          @keyup.enter="loadAssets"
        />
      </label>

      <div class="filters">
        <select v-model="query.category" @change="loadAssets">
          <option value="">全部分类</option>
          <option v-for="category in categories" :key="category" :value="category">{{ category }}</option>
        </select>
        <select v-model="query.location" @change="loadAssets">
          <option value="">全部位置</option>
          <option v-for="location in locations" :key="location" :value="location">{{ location }}</option>
        </select>
        <button class="ghost-btn" type="button" @click="loadAssets">查询</button>
      </div>
    </section>

    <section class="asset-grid" :class="{ loading: loading }">
      <article v-for="asset in assets" :key="asset.id" class="asset-card">
        <div class="asset-image" :style="{ backgroundImage: `url(${assetImage(asset)})` }"></div>
        <div class="asset-body">
          <div class="asset-title-row">
            <h2>{{ asset.name }}</h2>
            <span class="pill">{{ asset.category || '未分类' }}</span>
          </div>
          <div class="meta-line">
            <span>⌖ {{ asset.location || '未设置位置' }}</span>
            <span :class="expireClass(asset)">{{ expireText(asset) }}</span>
          </div>

          <div class="qty-row">
            <button type="button" title="减少" @click="adjustStock(asset, -1)">−</button>
            <strong>{{ numberText(asset.quantity) }} {{ asset.unit || '件' }}</strong>
            <button type="button" title="增加" @click="adjustStock(asset, 1)">＋</button>
          </div>

          <div class="card-actions">
            <button class="restock-btn" type="button" @click="adjustStock(asset, Math.max(Number(asset.warningQuantity || 1), 1))">
              <span>⊕</span>
              <span>补货</span>
            </button>
            <button class="icon-btn" type="button" title="编辑" @click="openEdit(asset)">✎</button>
            <button class="icon-btn danger-icon" type="button" title="删除" @click="removeAsset(asset)">×</button>
          </div>
        </div>
      </article>

      <div v-if="!assets.length && !loading" class="empty-state">
        <strong>还没有资产记录</strong>
        <span>点击右上角新增资产，开始建立家庭资产台账。</span>
      </div>
    </section>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeDialog">
      <form class="asset-form" @submit.prevent="submitForm">
        <div class="form-head">
          <div>
            <p class="eyebrow">{{ editingId ? 'Edit Asset' : 'New Asset' }}</p>
            <h2>{{ editingId ? '编辑资产' : '新增资产' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeDialog">×</button>
        </div>

        <div class="form-grid">
          <label>
            <span>资产名称</span>
            <input v-model.trim="form.name" required placeholder="例如：全脂鲜牛奶" />
          </label>
          <label>
            <span>分类</span>
            <input v-model.trim="form.category" required list="category-list" placeholder="生鲜食品" />
          </label>
          <label>
            <span>存放位置</span>
            <input v-model.trim="form.location" list="location-list" placeholder="冰箱 / 储物间" />
          </label>
          <label>
            <span>单位</span>
            <input v-model.trim="form.unit" required placeholder="盒 / 瓶 / 件" />
          </label>
          <label>
            <span>数量</span>
            <input v-model.number="form.quantity" required min="0" step="0.01" type="number" />
          </label>
          <label>
            <span>低库存阈值</span>
            <input v-model.number="form.warningQuantity" required min="0" step="0.01" type="number" />
          </label>
          <label>
            <span>购入日期</span>
            <input v-model="form.purchaseDate" type="date" />
          </label>
          <label>
            <span>到期日期</span>
            <input v-model="form.expireDate" type="date" />
          </label>
          <label class="wide">
            <span>图片地址</span>
            <input v-model.trim="form.imageUrl" placeholder="https://..." />
          </label>
          <label class="wide">
            <span>标签</span>
            <input v-model.trim="form.tags" placeholder="早餐,冷藏,补货" />
          </label>
          <label class="wide">
            <span>备注</span>
            <textarea v-model.trim="form.remark" rows="3" placeholder="记录保养、采购或使用提醒"></textarea>
          </label>
        </div>

        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closeDialog">取消</button>
          <button class="primary-btn" type="submit">{{ editingId ? '保存修改' : '创建资产' }}</button>
        </div>
      </form>
    </div>

    <datalist id="category-list">
      <option v-for="category in categories" :key="category" :value="category"></option>
    </datalist>
    <datalist id="location-list">
      <option v-for="location in locations" :key="location" :value="location"></option>
    </datalist>
  </section>
</template>

<script>
import {
  changeStock,
  createAsset,
  deleteAsset,
  getOptions,
  getSummary,
  listAssets,
  updateAsset
} from '../api/assets'

const demoAssets = [
  {
    id: 'demo-1',
    name: '全脂鲜牛奶',
    category: '生鲜食品',
    location: '冰箱',
    quantity: 3,
    unit: '盒',
    warningQuantity: 2,
    expireDate: nextDate(7),
    imageUrl: 'https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=900&q=80'
  },
  {
    id: 'demo-2',
    name: '特级生抽',
    category: '调味品',
    location: '厨房橱柜',
    quantity: 1,
    unit: '瓶',
    warningQuantity: 1,
    expireDate: nextDate(550),
    imageUrl: 'https://images.unsplash.com/photo-1472476443507-c7a5948772fc?auto=format&fit=crop&w=900&q=80'
  },
  {
    id: 'demo-3',
    name: '卷纸',
    category: '个护厨卫',
    location: '储物间',
    quantity: 12,
    unit: '卷',
    warningQuantity: 6,
    expireDate: '',
    imageUrl: 'https://images.unsplash.com/photo-1583947581924-a31b1d6bb176?auto=format&fit=crop&w=900&q=80'
  },
  {
    id: 'demo-4',
    name: '意大利面',
    category: '生鲜食品',
    location: '储物间',
    quantity: 0,
    unit: '袋',
    warningQuantity: 2,
    expireDate: nextDate(232),
    imageUrl: 'https://images.unsplash.com/photo-1551462147-ff29053bfc14?auto=format&fit=crop&w=900&q=80'
  }
]

export default {
  name: 'AssetView',
  data() {
    return {
      assets: [],
      summary: {},
      categories: ['生鲜食品', '调味品', '个护厨卫', '家电设备', '证件票据'],
      locations: ['冰箱', '厨房橱柜', '储物间', '卧室', '客厅'],
      query: {
        keyword: '',
        category: '',
        location: '',
        page: 1,
        size: 50
      },
      dialogVisible: false,
      editingId: null,
      form: emptyForm(),
      loading: false,
      offlineMode: false
    }
  },
  created() {
    this.bootstrap()
  },
  methods: {
    async bootstrap() {
      await Promise.all([this.loadAssets(), this.loadSummary(), this.loadOptions()])
    },
    async loadAssets() {
      this.loading = true
      try {
        const page = await listAssets(this.query)
        this.assets = page.records || []
        this.offlineMode = false
      } catch (error) {
        this.offlineMode = true
        const keyword = this.query.keyword
        this.assets = demoAssets.filter(asset => {
          const text = `${asset.name}${asset.category}${asset.location}${asset.tags || ''}`
          return (!keyword || text.includes(keyword)) &&
            (!this.query.category || asset.category === this.query.category) &&
            (!this.query.location || asset.location === this.query.location)
        })
      } finally {
        this.loading = false
      }
    },
    async loadSummary() {
      try {
        this.summary = await getSummary()
      } catch (error) {
        const categories = new Set(demoAssets.map(item => item.category))
        this.summary = {
          totalCategories: categories.size,
          totalAssets: demoAssets.length,
          expiredAssets: 0,
          lowStockAssets: demoAssets.filter(item => Number(item.quantity) <= Number(item.warningQuantity)).length
        }
      }
      this.$emit('summary-change', this.summary)
    },
    async loadOptions() {
      try {
        const options = await getOptions()
        this.categories = mergeOptions(this.categories, options.categories)
        this.locations = mergeOptions(this.locations, options.locations)
      } catch (error) {
        this.categories = mergeOptions(this.categories, demoAssets.map(item => item.category))
        this.locations = mergeOptions(this.locations, demoAssets.map(item => item.location))
      }
    },
    openCreate() {
      this.editingId = null
      this.form = emptyForm()
      this.dialogVisible = true
    },
    openEdit(asset) {
      this.editingId = asset.id
      this.form = { ...emptyForm(), ...asset }
      this.dialogVisible = true
    },
    closeDialog() {
      this.dialogVisible = false
    },
    async submitForm() {
      const payload = { ...this.form }
      if (this.offlineMode) {
        this.applyLocalSave(payload)
        this.closeDialog()
        return
      }
      if (this.editingId) {
        await updateAsset(this.editingId, payload)
      } else {
        await createAsset(payload)
      }
      this.closeDialog()
      await this.bootstrap()
      this.$emit('refresh-summary')
    },
    async adjustStock(asset, delta) {
      if (this.offlineMode || String(asset.id).startsWith('demo-')) {
        asset.quantity = Math.max(Number(asset.quantity || 0) + delta, 0)
        this.loadSummary()
        return
      }
      await changeStock(asset.id, delta)
      await this.bootstrap()
      this.$emit('refresh-summary')
    },
    async removeAsset(asset) {
      if (!window.confirm(`确认删除「${asset.name}」吗？`)) {
        return
      }
      if (this.offlineMode || String(asset.id).startsWith('demo-')) {
        this.assets = this.assets.filter(item => item.id !== asset.id)
        await this.loadSummary()
        return
      }
      await deleteAsset(asset.id)
      await this.bootstrap()
      this.$emit('refresh-summary')
    },
    applyLocalSave(payload) {
      if (this.editingId) {
        this.assets = this.assets.map(item => item.id === this.editingId ? { ...item, ...payload } : item)
      } else {
        this.assets.unshift({ ...payload, id: `local-${Date.now()}` })
      }
    },
    assetImage(asset) {
      return asset.imageUrl || 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=900&q=80'
    },
    numberText(value) {
      const number = Number(value || 0)
      return Number.isInteger(number) ? String(number) : number.toFixed(2)
    },
    expireText(asset) {
      if (!asset.expireDate) {
        return '长期可用'
      }
      const today = new Date()
      const expire = new Date(asset.expireDate)
      const diff = Math.ceil((expire - today) / 86400000)
      if (diff < 0) {
        return `已过期 ${Math.abs(diff)} 天`
      }
      if (diff === 0) {
        return '今天到期'
      }
      return `${diff} 天后过期`
    },
    expireClass(asset) {
      if (!asset.expireDate) {
        return 'fresh'
      }
      const diff = Math.ceil((new Date(asset.expireDate) - new Date()) / 86400000)
      if (diff < 0) {
        return 'expired'
      }
      if (diff <= 30) {
        return 'soon'
      }
      return 'fresh'
    }
  }
}

function nextDate(days) {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}

function emptyForm() {
  return {
    name: '',
    category: '',
    location: '',
    quantity: 1,
    unit: '件',
    warningQuantity: 1,
    purchaseDate: new Date().toISOString().slice(0, 10),
    expireDate: '',
    imageUrl: '',
    tags: '',
    remark: ''
  }
}

function mergeOptions(defaults, values) {
  return Array.from(new Set([...(defaults || []), ...(values || [])].filter(Boolean)))
}
</script>
