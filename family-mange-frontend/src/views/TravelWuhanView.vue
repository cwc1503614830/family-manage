<template>
  <div class="travel-page-root">
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Travel Management</p>
        <h1>武汉游</h1>
        <p>记录武汉景点打卡情况，支持室内、室外类型。</p>
      </div>
      <button class="primary-btn" type="button" @click="openForm()">新增景点</button>
    </div>

    <div class="menu-toolbar menu-filter">
      <button class="ghost-btn refresh-btn" type="button" @click="loadSpots">刷新</button>
      <span class="travel-stat">已打卡 {{ checkedCount }} / {{ spots.length }}</span>
    </div>

    <ResizableTable :columns="columns" :rows="spots" empty-text="暂无武汉景点">
      <template #spot_type="{ row }">
        <span class="travel-tag" :class="row.spot_type === '室外' ? 'outdoor' : 'indoor'">{{ row.spot_type }}</span>
      </template>
      <template #checked_in="{ row }">
        <span class="travel-check" :class="{ done: row.checked_in === 1 }">
          {{ row.checked_in === 1 ? '已打卡' : '未打卡' }}
        </span>
      </template>
      <template #actions="{ row }">
        <button class="ghost-btn mini" type="button" @click="openForm(row)">编辑</button>
        <button class="ghost-btn mini danger-text" type="button" @click="removeSpot(row)">删除</button>
      </template>
    </ResizableTable>
  </section>

  <div v-if="dialogVisible" class="modal-mask" @click.self="closeForm">
    <form class="asset-form" @submit.prevent="submitForm">
      <div class="form-head">
        <div>
          <p class="eyebrow">Wuhan Spot</p>
          <h2>{{ editingId ? '编辑景点' : '新增景点' }}</h2>
        </div>
        <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
      </div>
      <div class="form-grid">
        <label>
          <span>景点名称</span>
          <input v-model.trim="form.spotName" required placeholder="例如：黄鹤楼" />
        </label>
        <label>
          <span>景点类型</span>
          <select v-model="form.spotType" required>
            <option value="室内">室内</option>
            <option value="室外">室外</option>
          </select>
        </label>
        <label>
          <span>是否打卡</span>
          <select v-model.number="form.checkedIn">
            <option :value="0">未打卡</option>
            <option :value="1">已打卡</option>
          </select>
        </label>
        <label class="wide">
          <span>备注</span>
          <textarea v-model.trim="form.remark" rows="3" placeholder="记录游玩时间、感受等"></textarea>
        </label>
      </div>
      <div class="form-actions">
        <button class="ghost-btn" type="button" @click="closeForm">取消</button>
        <button class="primary-btn" type="submit">保存</button>
      </div>
    </form>
  </div>
  </div>
</template>

<script>
import ResizableTable from '../components/ResizableTable.vue'
import { createWuhanSpot, deleteWuhanSpot, listWuhanSpots, updateWuhanSpot } from '../api/travel'

export default {
  name: 'TravelWuhanView',
  components: { ResizableTable },
  data() {
    return {
      spots: [],
      dialogVisible: false,
      editingId: null,
      form: emptyForm(),
      columns: [
        { key: 'id', label: 'ID', width: 80 },
        { key: 'spot_name', label: '景点名称', width: 180 },
        { key: 'spot_type', label: '景点类型', width: 110, slot: 'spot_type' },
        { key: 'checked_in', label: '是否打卡', width: 110, slot: 'checked_in' },
        { key: 'remark', label: '备注', width: 260 },
        { key: 'updated_at', label: '更新时间', width: 160, formatter: formatDateTime },
        { key: 'actions', label: '操作', width: 150, slot: 'actions' }
      ]
    }
  },
  computed: {
    checkedCount() {
      return this.spots.filter(item => item.checked_in === 1).length
    }
  },
  created() {
    this.loadSpots()
  },
  methods: {
    async loadSpots() {
      this.spots = await listWuhanSpots()
    },
    openForm(spot) {
      this.editingId = spot ? spot.id : null
      this.form = spot
        ? {
            spotName: spot.spot_name,
            spotType: spot.spot_type || '室内',
            checkedIn: spot.checked_in === 1 ? 1 : 0,
            remark: spot.remark || ''
          }
        : emptyForm()
      this.dialogVisible = true
    },
    closeForm() {
      this.dialogVisible = false
    },
    async submitForm() {
      if (this.editingId) {
        await updateWuhanSpot(this.editingId, this.form)
      } else {
        await createWuhanSpot(this.form)
      }
      this.closeForm()
      await this.loadSpots()
    },
    async removeSpot(spot) {
      if (window.confirm(`确认删除景点「${spot.spot_name}」吗？`)) {
        await deleteWuhanSpot(spot.id)
        await this.loadSpots()
      }
    }
  }
}

function emptyForm() {
  return {
    spotName: '',
    spotType: '室内',
    checkedIn: 0,
    remark: ''
  }
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}
</script>
