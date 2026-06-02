<template>
  <div class="admin-table resizable-table">
    <table>
      <colgroup>
        <col
          v-for="column in columns"
          :key="column.key"
          :style="{ width: `${widths[column.key] || column.width || 140}px` }"
        />
      </colgroup>
      <thead>
        <tr>
          <th v-for="column in columns" :key="column.key">
            <slot v-if="column.headerSlot" :name="column.headerSlot"></slot>
            <span v-else class="th-label">{{ column.label }}</span>
            <span
              class="column-resizer"
              title="拖拽调整列宽"
              @mousedown.prevent="startResize(column, $event)"
            ></span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="row in rows"
          :key="row[rowKey]"
          :class="{ 'row-clickable': rowClickable }"
          @click="handleRowClick(row, $event)"
        >
          <td v-for="column in columns" :key="column.key">
            <slot v-if="column.slot" :name="column.slot" :row="row"></slot>
            <span v-else class="truncate">{{ formatCell(row, column) }}</span>
          </td>
        </tr>
        <tr v-if="!rows.length">
          <td class="table-empty" :colspan="columns.length">{{ emptyText }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: 'ResizableTable',
  props: {
    columns: {
      type: Array,
      required: true
    },
    rows: {
      type: Array,
      default: () => []
    },
    rowKey: {
      type: String,
      default: 'id'
    },
    emptyText: {
      type: String,
      default: '暂无数据'
    },
    rowClickable: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      widths: {}
    }
  },
  created() {
    this.columns.forEach(column => {
      this.$set(this.widths, column.key, column.width || 140)
    })
  },
  beforeDestroy() {
    this.stopResize()
  },
  methods: {
    handleRowClick(row, event) {
      if (!this.rowClickable) {
        return
      }
      if (event.target.closest('button, a, input, select, textarea, label')) {
        return
      }
      this.$emit('row-click', row)
    },
    formatCell(row, column) {
      if (typeof column.formatter === 'function') {
        return column.formatter(row[column.key], row)
      }
      const value = row[column.key]
      return value === null || value === undefined || value === '' ? '-' : value
    },
    startResize(column, event) {
      const startX = event.clientX
      const startWidth = this.widths[column.key] || column.width || 140
      this.resizeState = { key: column.key, startX, startWidth }
      document.addEventListener('mousemove', this.handleResize)
      document.addEventListener('mouseup', this.stopResize)
    },
    handleResize(event) {
      if (!this.resizeState) {
        return
      }
      const nextWidth = Math.max(72, this.resizeState.startWidth + event.clientX - this.resizeState.startX)
      this.$set(this.widths, this.resizeState.key, nextWidth)
    },
    stopResize() {
      document.removeEventListener('mousemove', this.handleResize)
      document.removeEventListener('mouseup', this.stopResize)
      this.resizeState = null
    }
  }
}
</script>
