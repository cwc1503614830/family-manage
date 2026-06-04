<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Kitchen Management</p>
        <h1>制作记录</h1>
        <p>由购物车下单生成，可查看与维护制作进度。</p>
      </div>
      <button class="primary-btn" type="button" @click="openForm()">新增记录</button>
    </div>

    <ResizableTable :columns="columns" :rows="records" empty-text="暂无制作记录">
      <template #is_done="{ row }">
        <span :class="row.is_done === 1 ? 'travel-check done' : 'travel-check'">
          {{ row.is_done === 1 ? '已完成' : '未完成' }}
        </span>
      </template>
      <template #actions="{ row }">
        <button v-if="isKitchenAdmin && row.is_done !== 1" class="ghost-btn mini" type="button" @click="completeRecord(row)">完成</button>
        <button class="ghost-btn mini" type="button" @click="openForm(row)">编辑</button>
        <button class="ghost-btn mini danger-text" type="button" @click="removeRecord(row)">删除</button>
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
            <p class="eyebrow">Cook Record</p>
            <h2>{{ editingId ? '编辑记录' : '新增记录' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>菜名</span>
            <input v-model.trim="form.dishName" required placeholder="例如：番茄炒蛋" />
          </label>
          <label>
            <span>制作日期</span>
            <input v-model="form.cookDate" type="date" required />
          </label>
          <label>
            <span>是否完成</span>
            <select v-model.number="form.isDone">
              <option :value="0">未完成</option>
              <option :value="1">已完成</option>
            </select>
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
  completeKitchenCookRecord,
  createKitchenCookRecord,
  deleteKitchenCookRecord,
  listKitchenCookRecords,
  updateKitchenCookRecord
} from '../api/kitchen'
import { currentUser } from '../api/auth'

export default {
  name: 'KitchenCookRecordView',
  components: { ResizableTable },
  data() {
    return {
      records: [],
      isKitchenAdmin: false,
      total: 0,
      page: 1,
      pageSize: 10,
      dialogVisible: false,
      editingId: null,
      form: emptyForm(),
      columns: [
        { key: 'id', label: 'ID', width: 70 },
        { key: 'dish_name', label: '菜名', width: 160 },
        { key: 'cook_date', label: '制作日期', width: 120, formatter: formatDate },
        { key: 'is_done', label: '是否完成', width: 100, slot: 'is_done' },
        { key: 'remark', label: '备注', width: 240 },
        { key: 'actions', label: '操作', width: 180, slot: 'actions' }
      ]
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    }
  },
  async created() {
    try {
      const session = await currentUser()
      this.isKitchenAdmin = (session.permissions || []).includes('system:manage')
    } catch (error) {
      this.isKitchenAdmin = false
    }
    this.loadRecords()
  },
  methods: {
    async loadRecords() {
      const data = await listKitchenCookRecords({ page: this.page, size: this.pageSize })
      this.records = data.records || []
      this.total = data.total || 0
    },
    goPage(next) {
      this.page = next
      this.loadRecords()
    },
    changePageSize() {
      this.page = 1
      this.loadRecords()
    },
    openForm(row) {
      this.editingId = row ? row.id : null
      this.form = row
        ? {
            dishName: row.dish_name,
            cookDate: formatDate(row.cook_date),
            isDone: row.is_done === 1 ? 1 : 0,
            remark: row.remark || ''
          }
        : emptyForm()
      this.dialogVisible = true
    },
    closeForm() {
      this.dialogVisible = false
    },
    async submitForm() {
      if (this.editingId) {
        await updateKitchenCookRecord(this.editingId, this.form)
      } else {
        await createKitchenCookRecord(this.form)
      }
      this.closeForm()
      await this.loadRecords()
    },
    async completeRecord(row) {
      await completeKitchenCookRecord(row.id)
      await this.loadRecords()
    },
    async removeRecord(row) {
      if (window.confirm(`确认删除制作记录「${row.dish_name}」吗？`)) {
        await deleteKitchenCookRecord(row.id)
        await this.loadRecords()
      }
    }
  }
}

function emptyForm() {
  const today = new Date()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return {
    dishName: '',
    cookDate: `${today.getFullYear()}-${month}-${day}`,
    isDone: 0,
    remark: ''
  }
}

function formatDate(value) {
  if (!value) return ''
  const text = String(value)
  return text.length >= 10 ? text.slice(0, 10) : text
}
</script>
