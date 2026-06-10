<template>
  <section class="admin-page todo-item-page">
    <div class="todo-item-page-head">
      <div>
        <p v-if="!isMobile" class="eyebrow">Todo Management</p>
        <h1>代办清单</h1>
        <p v-if="!isMobile">记录家庭待办事项，按截止日期跟进完成情况。</p>
      </div>
      <div class="todo-item-toolbar">
        <button class="primary-btn" type="button" @click="openForm()">新增事项</button>
        <button class="ghost-btn refresh-btn" type="button" :disabled="loading" @click="loadItems">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </div>

    <ResizableTable
      :key="isMobile ? 'todo-mobile' : 'todo-desktop'"
      class="todo-item-table"
      :class="{ loading: loading, 'todo-item-mobile-table': isMobile }"
      :columns="tableColumns"
      :rows="items"
      :row-clickable="isMobile"
      empty-text="暂无代办事项"
      @row-click="openForm"
    >
      <template #due_date="{ row }">
        <span :class="{ 'danger-text': isOverdue(row) }">{{ formatDate(row.due_date) }}</span>
      </template>
      <template #is_done="{ row }">
        <span :class="row.is_done === 1 ? 'travel-check done' : 'travel-check'">
          {{ row.is_done === 1 ? '已完成' : '未完成' }}
        </span>
      </template>
      <template #actions="{ row }">
        <button
          v-if="row.is_done !== 1"
          class="ghost-btn mini"
          type="button"
          @click.stop="completeItem(row)"
        >完成</button>
        <button class="ghost-btn mini" type="button" @click.stop="openForm(row)">编辑</button>
        <button class="ghost-btn mini danger-text" type="button" @click.stop="removeItem(row)">删除</button>
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
            <p class="eyebrow">Todo Item</p>
            <h2>{{ editingId ? '编辑事项' : '新增事项' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
        </div>
        <div class="form-grid">
          <label class="wide">
            <span>事项</span>
            <input v-model.trim="form.itemTitle" required placeholder="例如：更换净水器滤芯" />
          </label>
          <label>
            <span>截止日期</span>
            <input v-model="form.dueDate" type="date" />
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
            <textarea v-model.trim="form.remark" rows="3" placeholder="补充说明或提醒"></textarea>
          </label>
        </div>
        <div class="form-actions">
          <button class="ghost-btn" type="button" @click="closeForm">取消</button>
          <button class="primary-btn" type="submit" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </form>
    </div>
  </section>
</template>

<script>
import ResizableTable from '../components/ResizableTable.vue'
import {
  createTodoItem,
  deleteTodoItem,
  listTodoItems,
  updateTodoItem
} from '../api/todo'

export default {
  name: 'TodoItemView',
  components: { ResizableTable },
  data() {
    return {
      items: [],
      loading: false,
      saving: false,
      total: 0,
      page: 1,
      pageSize: 10,
      dialogVisible: false,
      editingId: null,
      form: emptyForm(),
      isMobile: false,
      mobileBreakpoint: null,
      columns: [
        { key: 'id', label: 'ID', width: 70 },
        { key: 'item_title', label: '事项', width: 220 },
        { key: 'due_date', label: '截止日期', width: 120, slot: 'due_date' },
        { key: 'is_done', label: '是否完成', width: 100, slot: 'is_done' },
        { key: 'remark', label: '备注', width: 240 },
        { key: 'actions', label: '操作', width: 180, slot: 'actions' }
      ],
      mobileColumns: [
        { key: 'item_title', label: '事项', width: 140 },
        { key: 'due_date', label: '截止日期', width: 110, slot: 'due_date' },
        { key: 'is_done', label: '是否完成', width: 100, slot: 'is_done' }
      ]
    }
  },
  computed: {
    tableColumns() {
      if (this.isMobile) {
        return this.mobileColumns
      }
      return this.columns
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    }
  },
  created() {
    this.setupMobileListener()
    this.loadItems()
  },
  activated() {
    this.loadItems()
  },
  beforeDestroy() {
    if (this.mobileBreakpoint) {
      this.mobileBreakpoint.removeEventListener('change', this.syncMobileState)
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
    async loadItems() {
      this.loading = true
      try {
        const result = await listTodoItems({ page: this.page, size: this.pageSize })
        this.items = sortTodoItems((result.records || []).map(normalizeRow))
        this.total = Number(result.total || 0)
      } catch (error) {
        this.items = []
        this.total = 0
        window.alert(error.message || '加载代办清单失败')
      } finally {
        this.loading = false
      }
    },
    goPage(next) {
      this.page = next
      this.loadItems()
    },
    changePageSize() {
      this.page = 1
      this.loadItems()
    },
    openForm(row) {
      this.editingId = row ? row.id : null
      this.saving = false
      this.form = row
        ? {
            itemTitle: row.item_title,
            dueDate: formatDateInput(row.due_date),
            isDone: row.is_done === 1 ? 1 : 0,
            remark: row.remark || ''
          }
        : emptyForm()
      this.dialogVisible = true
    },
    closeForm() {
      this.dialogVisible = false
    },
    buildPayload() {
      return {
        itemTitle: (this.form.itemTitle || '').trim(),
        dueDate: this.form.dueDate || '',
        isDone: this.form.isDone,
        remark: this.form.remark || ''
      }
    },
    async submitForm() {
      if (this.saving) {
        return
      }
      if (!(this.form.itemTitle || '').trim()) {
        window.alert('请输入事项')
        return
      }
      this.saving = true
      try {
        const payload = this.buildPayload()
        if (this.editingId) {
          await updateTodoItem(this.editingId, payload)
        } else {
          await createTodoItem(payload)
          this.page = 1
        }
        this.closeForm()
        await this.loadItems()
      } catch (error) {
        window.alert(error.message || '保存失败，请稍后重试')
      } finally {
        this.saving = false
      }
    },
    async completeItem(row) {
      try {
        await updateTodoItem(row.id, {
          itemTitle: row.item_title,
          dueDate: formatDateInput(row.due_date),
          isDone: 1,
          remark: row.remark || ''
        })
        await this.loadItems()
      } catch (error) {
        window.alert(error.message || '操作失败')
      }
    },
    async removeItem(row) {
      if (window.confirm(`确认删除事项「${row.item_title}」吗？`)) {
        try {
          await deleteTodoItem(row.id)
          if (this.items.length <= 1 && this.page > 1) {
            this.page -= 1
          }
          await this.loadItems()
        } catch (error) {
          window.alert(error.message || '删除失败')
        }
      }
    },
    isOverdue(row) {
      if (row.is_done === 1 || !row.due_date) {
        return false
      }
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      return new Date(row.due_date) < today
    },
    formatDate
  }
}

function emptyForm() {
  return {
    itemTitle: '',
    dueDate: '',
    isDone: 0,
    remark: ''
  }
}

function formatDate(value) {
  if (!value) {
    return '-'
  }
  const text = String(value)
  return text.includes('T') ? text.slice(0, 10) : text.slice(0, 10)
}

function formatDateInput(value) {
  if (!value) {
    return ''
  }
  return formatDate(value) === '-' ? '' : formatDate(value)
}

function normalizeRow(row) {
  return {
    ...row,
    item_title: row.item_title || row.itemTitle || '',
    due_date: row.due_date || row.dueDate,
    is_done: Number(row.is_done != null ? row.is_done : row.isDone) === 1 ? 1 : 0,
    remark: row.remark || ''
  }
}

function sortTodoItems(items) {
  return [...items].sort((a, b) => {
    if (a.is_done !== b.is_done) {
      return a.is_done - b.is_done
    }
    const dateA = a.due_date ? String(a.due_date).slice(0, 10) : '9999-12-31'
    const dateB = b.due_date ? String(b.due_date).slice(0, 10) : '9999-12-31'
    if (dateA !== dateB) {
      return dateA.localeCompare(dateB)
    }
    return Number(b.id || 0) - Number(a.id || 0)
  })
}
</script>

<style scoped>
.todo-item-page-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.todo-item-page-head h1 {
  margin: 0;
}

.todo-item-toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.todo-item-table.loading {
  opacity: 0.6;
  pointer-events: none;
}
</style>
