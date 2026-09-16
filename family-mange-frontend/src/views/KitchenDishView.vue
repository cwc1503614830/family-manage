<template>
  <div class="kitchen-dish-page">
    <!-- 移动端全屏子页面 -->
    <section v-if="isMobile && mobileScreen" class="kitchen-mobile-page">
      <header class="kitchen-mobile-page-head">
        <button class="ghost-btn kitchen-back-btn" type="button" @click="closeMobileScreen">← 返回</button>
        <h1>{{ mobileScreenTitle }}</h1>
      </header>

      <!-- 购物车：待下单 + 下单记录 -->
      <div v-if="mobileScreen === 'cart'" class="kitchen-mobile-page-body">
        <div class="kitchen-mobile-block">
          <h3>待下单（{{ localCart.length }}）</h3>
          <p v-if="localCart.length" class="kitchen-mobile-hint">请确认菜品后，点击下方按钮一起提交给厨房</p>
          <ul v-if="localCart.length" class="kitchen-mobile-list">
            <li v-for="(item, index) in localCart" :key="'cart-' + index" class="kitchen-mobile-list-item">
              <span class="kitchen-mobile-list-name">{{ item.dish_name }}</span>
              <button class="ghost-btn mini danger-text" type="button" @click="removeFromLocalCart(index)">移除</button>
            </li>
          </ul>
          <p v-else class="kitchen-empty">暂无待下单菜品，返回列表点菜</p>
        </div>

        <div v-if="myPendingRecords.length" class="kitchen-mobile-block">
          <h3>制作中（{{ myPendingRecords.length }}）</h3>
          <ul class="kitchen-mobile-list">
            <li v-for="record in myPendingRecords" :key="'pending-' + record.id" class="kitchen-mobile-list-item">
              <div>
                <strong>{{ record.dish_name }}</strong>
                <span class="kitchen-cart-date">{{ formatDate(record.cook_date) }} · 等待厨房</span>
              </div>
              <span class="kitchen-status-tag pending">制作中</span>
            </li>
          </ul>
        </div>
        <div v-if="myDoneRecords.length" class="kitchen-mobile-block">
          <h3>已完成（{{ myDoneRecords.length }}）</h3>
          <ul class="kitchen-mobile-list">
            <li v-for="record in myDoneRecords" :key="'done-' + record.id" class="kitchen-mobile-list-item">
              <div>
                <strong>{{ record.dish_name }}</strong>
                <span class="kitchen-cart-date">{{ formatDate(record.cook_date) }}</span>
              </div>
              <span class="kitchen-status-tag done">已完成</span>
            </li>
          </ul>
        </div>
      </div>
      <div v-if="mobileScreen === 'cart' && localCart.length" class="kitchen-mobile-page-footer">
        <button class="primary-btn kitchen-submit-btn" type="button" :disabled="ordering" @click="submitOrder">
          {{ ordering ? '提交中...' : `确认下单（${localCart.length} 道菜）` }}
        </button>
      </div>

      <!-- 管理员待制作 -->
      <div v-else-if="mobileScreen === 'adminPending'" class="kitchen-mobile-page-body">
        <p v-if="adminPendingRecords.length" class="kitchen-mobile-hint">以下订单待厨房制作，完成后用户购物车将清空对应菜品</p>
        <ul v-if="adminPendingRecords.length" class="kitchen-mobile-list">
          <li v-for="record in adminPendingRecords" :key="record.id" class="kitchen-mobile-list-item stack">
            <div>
              <strong>{{ record.dish_name }}</strong>
              <span class="kitchen-cart-date">
                {{ record.ordered_by_name || '未知用户' }} · {{ formatDate(record.cook_date) }}
              </span>
            </div>
            <button class="primary-btn mini" type="button" @click="completeCook(record)">完成制作</button>
          </li>
        </ul>
        <p v-else class="kitchen-empty">暂无待制作订单</p>
      </div>
    </section>

    <!-- 菜品列表主页面 -->
    <section v-show="!isMobile || !mobileScreen" class="admin-page kitchen-dish-list-page">
      <div class="kitchen-dish-page-head">
        <h1>菜品清单</h1>
        <div class="kitchen-dish-toolbar">
          <template v-if="isMobile">
            <button class="kitchen-head-action cart" type="button" @click="openCart">
              <span>🛒</span>
              <span>购物车</span>
              <em v-if="localCart.length">{{ localCart.length }}</em>
            </button>
            <button
              v-if="isKitchenAdmin"
              class="kitchen-head-action pending"
              type="button"
              @click="openMobileScreen('adminPending')"
            >
              <span>👨‍🍳</span>
              <span>待制作</span>
              <em v-if="adminPendingCount">{{ adminPendingCount }}</em>
            </button>
          </template>
          <button v-if="!isMobile" class="primary-btn" type="button" @click="openForm()">新增菜品</button>
          <button class="ghost-btn refresh-btn" type="button" @click="refreshPage">刷新</button>
        </div>
      </div>

      <template v-if="!isMobile">
        <ResizableTable
          class="kitchen-dish-desktop-table"
          :columns="columns"
          :rows="dishes"
          row-clickable
          empty-text="暂无菜品"
          @row-click="openForm"
        >
          <template #image="{ row }">
            <div class="clothes-image-cell">
              <img v-if="row.image_url" :src="row.image_url" alt="菜品图片" class="clothes-thumb" @click.stop="previewImage(row.image_url)" />
              <span v-else class="clothes-no-image">无</span>
            </div>
          </template>
          <template #actions="{ row }">
            <button class="ghost-btn mini" type="button" @click.stop="openForm(row)">编辑</button>
            <button class="ghost-btn mini danger-text" type="button" @click.stop="removeDish(row)">删除</button>
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
      </template>

      <template v-else>
        <section class="kitchen-dish-grid" :class="{ loading: loading }">
          <article v-for="dish in dishes" :key="dish.id" class="kitchen-dish-card">
            <div class="kitchen-dish-card-image">
              <img v-if="dish.image_url" :src="dish.image_url" :alt="dish.dish_name" />
              <span v-else class="kitchen-dish-no-image">无图</span>
            </div>
            <div class="kitchen-dish-card-body">
              <h3>{{ dish.dish_name }}</h3>
              <p v-if="dish.ingredients" class="kitchen-dish-ingredients">{{ dish.ingredients }}</p>
              <button class="primary-btn mini" type="button" @click="addToCart(dish)">点菜</button>
            </div>
          </article>
        </section>
        <p v-if="!loading && !dishes.length" class="kitchen-empty">暂无菜品</p>
        <div class="table-pagination">
          <span class="pagination-summary">共 {{ total }} 条</span>
          <button class="ghost-btn mini" type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
          <span class="pagination-current">第 {{ page }} / {{ totalPages }} 页</span>
          <button class="ghost-btn mini" type="button" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
        </div>
      </template>
    </section>

    <p v-if="isMobile && !mobileScreen && cartTip" class="kitchen-cart-tip">{{ cartTip }}</p>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeForm">
      <form class="asset-form" @submit.prevent="submitForm">
        <div class="form-head">
          <div>
            <p class="eyebrow">Kitchen Dish</p>
            <h2>{{ editingId ? '编辑菜品' : '新增菜品' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>菜名</span>
            <input v-model.trim="form.dishName" required placeholder="例如：红烧肉" />
          </label>
          <label class="wide">
            <span>配料</span>
            <textarea v-model.trim="form.ingredients" rows="2" placeholder="猪肉、冰糖、生抽等"></textarea>
          </label>
          <label class="wide">
            <span>菜品图片</span>
            <div class="clothes-upload">
              <div v-if="form.imageUrl" class="clothes-preview">
                <img :src="form.imageUrl" alt="预览图" @click.stop="previewImage(form.imageUrl)" />
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
            <textarea v-model.trim="form.remark" rows="2"></textarea>
          </label>
        </div>
        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closeForm">取消</button>
          <button class="primary-btn" type="submit" :disabled="uploading">保存</button>
        </div>
      </form>
    </div>

    <div v-if="previewUrl" class="modal-mask album-preview-mask" @click="previewUrl = ''">
      <button class="album-preview-close" @click.stop="previewUrl = ''">✕</button>
      <img :src="previewUrl" alt="预览" class="album-preview-image" @click.stop />
    </div>
  </div>
</template>

<script>
import ResizableTable from '../components/ResizableTable.vue'
import {
  completeKitchenCookRecord,
  createKitchenDish,
  deleteKitchenDish,
  listKitchenCookRecords,
  listKitchenDishes,
  orderKitchenFromCart,
  updateKitchenDish
} from '../api/kitchen'
import { uploadFile } from '../api/upload'
import { currentUser } from '../api/auth'

const MOBILE_PAGE_SIZE = 6
const POLL_INTERVAL_MS = 20000

export default {
  name: 'KitchenDishView',
  components: { ResizableTable },
  data() {
    return {
      dishes: [],
      total: 0,
      page: 1,
      pageSize: 10,
      loading: false,
      dialogVisible: false,
      editingId: null,
      uploading: false,
      uploadError: '',
      previewUrl: '',
      form: emptyForm(),
      isMobile: false,
      mobileBreakpoint: null,
      mobileScreen: null,
      localCart: [],
      myPendingRecords: [],
      myDoneRecords: [],
      adminPendingRecords: [],
      ordering: false,
      isKitchenAdmin: false,
      cartTip: '',
      pollTimer: null,
      columns: [
        { key: 'id', label: 'ID', width: 70 },
        { key: 'dish_name', label: '菜名', width: 140 },
        { key: 'ingredients', label: '配料', width: 220 },
        { key: 'image', label: '图片', width: 72, slot: 'image' },
        { key: 'remark', label: '备注', width: 200 },
        { key: 'actions', label: '操作', width: 150, slot: 'actions' }
      ]
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.total / (this.isMobile ? MOBILE_PAGE_SIZE : this.pageSize)))
    },
    adminPendingCount() {
      return this.adminPendingRecords.length
    },
    mobileScreenTitle() {
      if (this.mobileScreen === 'cart') return '购物车'
      if (this.mobileScreen === 'adminPending') return '待制作菜单'
      return ''
    }
  },
  async created() {
    await this.loadSession()
    this.setupMobileListener()
    this.loadDishes()
    this.refreshOrderState()
    this.startPolling()
  },
  beforeDestroy() {
    if (this.mobileBreakpoint) {
      this.mobileBreakpoint.removeEventListener('change', this.syncMobileState)
    }
    this.stopPolling()
    document.body.classList.remove('kitchen-modal-open')
  },
  watch: {
    mobileScreen(screen) {
      if (screen === 'cart') {
        this.loadMyOrderRecords()
      } else if (screen === 'adminPending') {
        this.loadAdminPendingRecords()
      }
      this.syncKitchenModalClass()
    },
    isMobile(mobile) {
      if (mobile) {
        this.startPolling()
      } else {
        this.stopPolling()
        this.mobileScreen = null
        this.syncKitchenModalClass()
      }
    }
  },
  mounted() {
    this._onKeydown = (e) => {
      if (e.key === 'Escape' && this.previewUrl) {
        this.previewUrl = ''
      }
    }
    window.addEventListener('keydown', this._onKeydown)
  },
  beforeUnmount() {
    if (this._onKeydown) {
      window.removeEventListener('keydown', this._onKeydown)
    }
  },
  methods: {
    syncKitchenModalClass() {
      if (this.isMobile && this.mobileScreen) {
        document.body.classList.add('kitchen-modal-open')
      } else {
        document.body.classList.remove('kitchen-modal-open')
      }
    },
    openCart() {
      this.openMobileScreen('cart')
    },
    async loadSession() {
      try {
        const session = await currentUser()
        this.isKitchenAdmin = (session.permissions || []).includes('system:manage')
      } catch (error) {
        this.isKitchenAdmin = false
      }
    },
    openMobileScreen(screen) {
      this.mobileScreen = screen
      if (screen === 'cart') {
        this.loadMyOrderRecords()
      } else if (screen === 'adminPending') {
        this.loadAdminPendingRecords()
      }
      this.syncKitchenModalClass()
    },
    closeMobileScreen() {
      this.mobileScreen = null
      this.syncKitchenModalClass()
    },
    startPolling() {
      this.stopPolling()
      if (!this.isMobile) return
      this.pollTimer = window.setInterval(() => {
        this.refreshOrderState()
        if (this.mobileScreen === 'cart') {
          this.loadMyOrderRecords()
        } else if (this.mobileScreen === 'adminPending') {
          this.loadAdminPendingRecords()
        }
      }, POLL_INTERVAL_MS)
    },
    stopPolling() {
      if (this.pollTimer) {
        window.clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    async refreshPage() {
      await Promise.all([this.loadDishes(), this.refreshOrderState()])
      if (this.mobileScreen === 'cart') {
        await this.loadMyOrderRecords()
      }
    },
    async refreshOrderState() {
      await this.loadMyPendingRecords()
      if (this.isKitchenAdmin) {
        await this.loadAdminPendingRecords()
      }
    },
    showCartTip(message) {
      this.cartTip = message
      window.clearTimeout(this._cartTipTimer)
      this._cartTipTimer = window.setTimeout(() => {
        this.cartTip = ''
      }, 2000)
    },
    setupMobileListener() {
      this.mobileBreakpoint = window.matchMedia('(max-width: 640px)')
      this.syncMobileState()
      this.mobileBreakpoint.addEventListener('change', this.syncMobileState)
    },
    syncMobileState() {
      const wasMobile = this.isMobile
      this.isMobile = Boolean(this.mobileBreakpoint && this.mobileBreakpoint.matches)
      if (wasMobile !== this.isMobile) {
        this.page = 1
        this.pageSize = this.isMobile ? MOBILE_PAGE_SIZE : 10
        this.loadDishes()
        if (this.isMobile) {
          this.startPolling()
          this.refreshOrderState()
        } else {
          this.stopPolling()
          this.mobileScreen = null
        }
      }
    },
    async loadDishes() {
      this.loading = true
      try {
        const size = this.isMobile ? MOBILE_PAGE_SIZE : this.pageSize
        const data = await listKitchenDishes({ page: this.page, size })
        this.dishes = data.records || []
        this.total = data.total || 0
      } finally {
        this.loading = false
      }
    },
    async loadMyPendingRecords() {
      const data = await listKitchenCookRecords({ page: 1, size: 100, pendingOnly: true, mine: true })
      this.myPendingRecords = data.records || []
    },
    async loadMyOrderRecords() {
      const data = await listKitchenCookRecords({ page: 1, size: 100, mine: true })
      const records = data.records || []
      this.myPendingRecords = records.filter(item => item.is_done !== 1)
      this.myDoneRecords = records.filter(item => item.is_done === 1)
    },
    async loadAdminPendingRecords() {
      const data = await listKitchenCookRecords({ page: 1, size: 100, pendingOnly: true })
      this.adminPendingRecords = data.records || []
    },
    goPage(next) {
      this.page = next
      this.loadDishes()
    },
    changePageSize() {
      this.page = 1
      this.loadDishes()
    },
    addToCart(dish) {
      const exists = this.localCart.some(item => item.dish_name === dish.dish_name)
      const inPending = this.myPendingRecords.some(item => item.dish_name === dish.dish_name)
      if (exists || inPending) {
        this.showCartTip('该菜品已在购物车或制作中')
        return
      }
      this.localCart.push({ dish_name: dish.dish_name })
      this.showCartTip(`已加入购物车：${dish.dish_name}`)
    },
    removeFromLocalCart(index) {
      this.localCart.splice(index, 1)
    },
    async submitOrder() {
      if (!this.localCart.length) {
        window.alert('请先添加菜品')
        return
      }
      this.ordering = true
      try {
        const dishNames = this.localCart.map(item => item.dish_name)
        await orderKitchenFromCart({ dishNames })
        this.localCart = []
        await this.refreshOrderState()
        await this.loadMyOrderRecords()
        this.showCartTip('下单成功，厨房制作中')
      } catch (error) {
        window.alert(error.message || '下单失败，请稍后重试')
      } finally {
        this.ordering = false
      }
    },
    async completeCook(record) {
      if (!window.confirm(`确认「${record.dish_name}」已完成制作吗？`)) {
        return
      }
      await completeKitchenCookRecord(record.id)
      await this.refreshOrderState()
      if (this.mobileScreen === 'adminPending') {
        await this.loadAdminPendingRecords()
      }
      this.showCartTip(`「${record.dish_name}」已完成`)
    },
    openForm(row) {
      this.editingId = row ? row.id : null
      this.form = row
        ? {
            dishName: row.dish_name,
            ingredients: row.ingredients || '',
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
    previewImage(url) {
      if (url) {
        this.previewUrl = url
      }
    },
    async submitForm() {
      if (this.editingId) {
        await updateKitchenDish(this.editingId, this.form)
      } else {
        await createKitchenDish(this.form)
      }
      this.closeForm()
      await this.loadDishes()
    },
    async removeDish(row) {
      if (window.confirm(`确认删除菜品「${row.dish_name}」吗？`)) {
        await deleteKitchenDish(row.id)
        await this.loadDishes()
      }
    },
    formatDate(value) {
      if (!value) return ''
      const text = String(value)
      return text.length >= 10 ? text.slice(0, 10) : text
    }
  }
}

function emptyForm() {
  return {
    dishName: '',
    ingredients: '',
    imageUrl: '',
    remark: ''
  }
}
</script>

<style scoped>
.kitchen-dish-page-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.kitchen-dish-page-head h1 {
  margin: 0;
}

.kitchen-dish-toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.kitchen-dish-list-page :deep(.kitchen-dish-desktop-table) img {
  width: 48px;
  height: 48px;
  max-width: 48px;
  max-height: 48px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--parchment);
  display: block;
}

.clothes-image-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  overflow: hidden;
}

.clothes-thumb {
  width: 48px;
  height: 48px;
  max-width: 48px;
  max-height: 48px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid var(--parchment);
  box-sizing: border-box;
}

.clothes-no-image {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-ghost);
  font-size: 12px;
  border-radius: 8px;
  border: 1px dashed var(--linen);
  box-sizing: border-box;
}

.kitchen-dish-page :deep(.admin-table td),
.kitchen-dish-page :deep(.admin-table th) {
  white-space: nowrap;
}

.kitchen-dish-page :deep(.admin-table td) {
  height: 64px;
  vertical-align: middle;
}

.kitchen-mobile-page {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: var(--bg);
  display: flex;
  flex-direction: column;
}

.kitchen-mobile-page-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
  background: var(--panel);
  flex-shrink: 0;
}

.kitchen-mobile-page-head h1 {
  margin: 0;
  font-size: 18px;
}

.kitchen-back-btn {
  flex-shrink: 0;
  padding: 6px 10px;
}

.kitchen-mobile-page-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
  padding-bottom: 100px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.kitchen-mobile-hint {
  margin: 0;
  font-size: 14px;
  color: var(--muted);
}

.kitchen-mobile-block h3 {
  margin: 0 0 10px;
  font-size: 15px;
}

.kitchen-mobile-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kitchen-mobile-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 12px;
}

.kitchen-mobile-list-item.stack {
  flex-direction: column;
  align-items: stretch;
}

.kitchen-mobile-list-name {
  font-size: 16px;
  font-weight: 600;
}

.kitchen-dish-toolbar {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.kitchen-head-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--panel);
  font-size: 12px;
  cursor: pointer;
  position: relative;
}

.kitchen-head-action.cart {
  border-color: rgba(196, 106, 58, 0.35);
  color: var(--accent);
}

.kitchen-head-action.pending {
  border-color: rgba(45, 106, 79, 0.35);
  color: #2d6a4f;
}

.kitchen-head-action em {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #e74c3c;
  color: #fff;
  font-size: 11px;
  font-style: normal;
  line-height: 18px;
  text-align: center;
}

.kitchen-mobile-page-footer {
  flex-shrink: 0;
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1201;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom, 0px));
  background: var(--panel);
  border-top: 1px solid var(--line);
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.08);
}

.kitchen-submit-btn {
  width: 100%;
}

.kitchen-status-tag {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  flex-shrink: 0;
}

.kitchen-status-tag.pending {
  background: #fff3e0;
  color: #e65100;
}

.kitchen-status-tag.done {
  background: #e8f5e9;
  color: #2e7d32;
}
</style>
