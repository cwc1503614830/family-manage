<template>
  <div class="travel-page-root">
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Travel Management</p>
        <h1>国内游</h1>
        <p>记录国内城市旅行花费，并编写 Markdown 旅游笔记。</p>
      </div>
      <div class="wealth-total-panel">
        <span>总花费</span>
        <strong>{{ moneyText(totalCost) }}</strong>
      </div>
      <button class="primary-btn" type="button" @click="openForm()">新增记录</button>
    </div>

    <div class="menu-toolbar menu-filter">
      <button class="ghost-btn refresh-btn" type="button" @click="bootstrap">刷新</button>
    </div>

    <ResizableTable :columns="columns" :rows="trips" empty-text="暂无国内游记录">
      <template #cost="{ row }">
        <strong>{{ moneyText(row.cost) }}</strong>
      </template>
      <template #note="{ row }">
        <span class="travel-note-flag" :class="{ has: row.note_id }">{{ row.note_id ? '已有笔记' : '无笔记' }}</span>
      </template>
      <template #actions="{ row }">
        <button class="ghost-btn mini" type="button" @click="openNote(row)">详情</button>
        <button class="ghost-btn mini" type="button" @click="openForm(row)">编辑</button>
        <button class="ghost-btn mini danger-text" type="button" @click="removeTrip(row)">删除</button>
      </template>
    </ResizableTable>
  </section>

  <div v-if="dialogVisible" class="modal-mask" @click.self="closeForm">
    <form class="asset-form" @submit.prevent="submitForm">
      <div class="form-head">
        <div>
          <p class="eyebrow">Domestic Trip</p>
          <h2>{{ editingId ? '编辑记录' : '新增记录' }}</h2>
        </div>
        <button class="icon-btn" type="button" title="关闭" @click="closeForm">×</button>
      </div>
      <div class="form-grid">
        <label>
          <span>省份</span>
          <select v-model="selectedProvince" required @change="onProvinceChange">
            <option value="">请选择省份</option>
            <option v-for="item in provinces" :key="item.province" :value="item.province">
              {{ item.province }}
            </option>
          </select>
        </label>
        <label>
          <span>城市</span>
          <select v-model.number="form.cityId" required>
            <option :value="null">请选择城市</option>
            <option v-for="city in filteredCities" :key="city.id" :value="city.id">
              {{ city.city }}
            </option>
          </select>
        </label>
        <label>
          <span>花费（元）</span>
          <input v-model.number="form.cost" required min="0" step="0.01" type="number" />
        </label>
      </div>
      <div class="form-actions">
        <button class="ghost-btn" type="button" @click="closeForm">取消</button>
        <button class="primary-btn" type="submit">保存</button>
      </div>
    </form>
  </div>

  <div v-if="noteVisible" class="modal-mask travel-note-mask" @click.self="closeNote">
    <form class="asset-form travel-note-form" @submit.prevent="submitNote">
      <div class="form-head">
        <div>
          <p class="eyebrow">Travel Note</p>
          <h2>旅游笔记 · {{ noteTripLabel }}</h2>
        </div>
        <button class="icon-btn" type="button" title="关闭" @click="closeNote">×</button>
      </div>
      <label class="travel-note-title">
        <span>标题</span>
        <input v-model.trim="noteForm.title" placeholder="例如：成都三日游" />
      </label>
      <div class="travel-note-editor">
        <div class="travel-note-pane">
          <div class="travel-note-toolbar">
            <button class="ghost-btn mini" type="button" @click="insertMarkdown('## ', '')">标题</button>
            <button class="ghost-btn mini" type="button" @click="insertMarkdown('**', '**')">加粗</button>
            <button class="ghost-btn mini" type="button" :disabled="uploading" @click="triggerImageUpload">
              {{ uploading ? '上传中...' : '插入图片' }}
            </button>
            <input ref="noteFileInput" type="file" accept="image/*" class="clothes-file-input" @change="handleImageUpload" />
          </div>
          <textarea
            ref="noteTextarea"
            v-model="noteForm.content"
            class="travel-note-textarea"
            placeholder="支持 Markdown，可插入图片 ![](url)"
          ></textarea>
        </div>
        <div class="travel-note-pane preview">
          <p class="travel-note-preview-title">预览</p>
          <div class="travel-note-preview" v-html="notePreviewHtml"></div>
        </div>
      </div>
      <div v-if="uploadError" class="form-error">{{ uploadError }}</div>
      <div class="form-actions">
        <button class="ghost-btn" type="button" @click="closeNote">取消</button>
        <button class="primary-btn" type="submit" :disabled="uploading">保存笔记</button>
      </div>
    </form>
  </div>
  </div>
</template>

<script>
import ResizableTable from '../components/ResizableTable.vue'
import { renderSimpleMarkdown } from '../utils/simpleMarkdown'
import { uploadFile } from '../api/upload'
import {
  createDomesticTrip,
  deleteDomesticTrip,
  getDomesticNote,
  getTravelOptions,
  listDomesticTrips,
  saveDomesticNote,
  updateDomesticTrip
} from '../api/travel'

export default {
  name: 'TravelDomesticView',
  components: { ResizableTable },
  data() {
    return {
      trips: [],
      provinces: [],
      cities: [],
      selectedProvince: '',
      dialogVisible: false,
      noteVisible: false,
      editingId: null,
      noteTripId: null,
      noteTripLabel: '',
      uploading: false,
      uploadError: '',
      form: emptyTripForm(),
      noteForm: emptyNoteForm(),
      columns: [
        { key: 'id', label: 'ID', width: 80 },
        { key: 'province', label: '省份', width: 140 },
        { key: 'city', label: '城市', width: 140 },
        { key: 'cost', label: '花费', width: 120, slot: 'cost' },
        { key: 'note', label: '笔记', width: 110, slot: 'note' },
        { key: 'updated_at', label: '更新时间', width: 160, formatter: formatDateTime },
        { key: 'actions', label: '操作', width: 200, slot: 'actions' }
      ]
    }
  },
  computed: {
    totalCost() {
      return this.trips.reduce((sum, item) => sum + Number(item.cost || 0), 0)
    },
    filteredCities() {
      if (!this.selectedProvince) {
        return this.cities
      }
      return this.cities.filter(item => item.province === this.selectedProvince)
    },
    notePreviewHtml() {
      return renderSimpleMarkdown(this.noteForm.content)
    }
  },
  created() {
    this.bootstrap()
  },
  beforeDestroy() {
    document.body.classList.remove('modal-open')
  },
  methods: {
    async bootstrap() {
      await Promise.all([this.loadTrips(), this.loadOptions()])
    },
    async loadTrips() {
      this.trips = await listDomesticTrips()
    },
    async loadOptions(province) {
      const options = await getTravelOptions(province)
      this.provinces = options.provinces || []
      this.cities = options.cities || []
    },
    async onProvinceChange() {
      await this.loadOptions(this.selectedProvince)
      const exists = this.filteredCities.some(item => item.id === this.form.cityId)
      if (!exists) {
        this.form.cityId = null
      }
    },
    async openForm(trip) {
      await this.loadOptions()
      this.editingId = trip ? trip.id : null
      if (trip) {
        this.selectedProvince = trip.province
        await this.loadOptions(trip.province)
        this.form = { cityId: trip.city_id, cost: Number(trip.cost || 0) }
      } else {
        this.selectedProvince = ''
        this.form = emptyTripForm()
      }
      this.dialogVisible = true
    },
    closeForm() {
      this.dialogVisible = false
    },
    async submitForm() {
      const payload = { cityId: this.form.cityId, cost: this.form.cost }
      if (this.editingId) {
        await updateDomesticTrip(this.editingId, payload)
      } else {
        await createDomesticTrip(payload)
      }
      this.closeForm()
      await this.loadTrips()
    },
    async removeTrip(trip) {
      if (window.confirm(`确认删除「${trip.province} ${trip.city}」的旅行记录吗？`)) {
        await deleteDomesticTrip(trip.id)
        await this.loadTrips()
      }
    },
    async openNote(trip) {
      this.noteTripId = trip.id
      this.noteTripLabel = `${trip.province} ${trip.city}`
      const note = await getDomesticNote(trip.id)
      this.noteForm = {
        title: note.title || '',
        content: note.content || ''
      }
      this.uploadError = ''
      this.noteVisible = true
      document.body.classList.add('modal-open')
    },
    closeNote() {
      this.noteVisible = false
      document.body.classList.remove('modal-open')
    },
    async submitNote() {
      await saveDomesticNote(this.noteTripId, this.noteForm)
      this.closeNote()
      await this.loadTrips()
    },
    triggerImageUpload() {
      this.$refs.noteFileInput.click()
    },
    async handleImageUpload(event) {
      const file = event.target.files && event.target.files[0]
      event.target.value = ''
      if (!file) {
        return
      }
      this.uploading = true
      this.uploadError = ''
      try {
        const result = await uploadFile(file, 'travel')
        this.insertMarkdown(`![${file.name}](${result.url})`, '')
      } catch (error) {
        this.uploadError = error.message || '图片上传失败'
      } finally {
        this.uploading = false
      }
    },
    insertMarkdown(prefix, suffix) {
      const textarea = this.$refs.noteTextarea
      if (!textarea) {
        this.noteForm.content += prefix + suffix
        return
      }
      const start = textarea.selectionStart
      const end = textarea.selectionEnd
      const selected = this.noteForm.content.slice(start, end)
      const next = prefix + selected + suffix
      this.noteForm.content = this.noteForm.content.slice(0, start) + next + this.noteForm.content.slice(end)
      this.$nextTick(() => {
        textarea.focus()
        const cursor = start + prefix.length + selected.length
        textarea.setSelectionRange(cursor, cursor)
      })
    },
    moneyText(value) {
      return Number(value || 0).toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })
    }
  }
}

function emptyTripForm() {
  return { cityId: null, cost: 0 }
}

function emptyNoteForm() {
  return { title: '', content: '' }
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}
</script>
