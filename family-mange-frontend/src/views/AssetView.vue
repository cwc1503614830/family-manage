<template>
  <section class="dashboard-page">
    <section class="page-head dashboard-page-head">
      <div>
        <p class="eyebrow">Family Dashboard</p>
        <h1>家庭数据大屏</h1>
        <p>汇总财富、食材与代办等关键家庭指标。</p>
      </div>
      <button class="ghost-btn refresh-btn" type="button" :disabled="loading" @click="loadDashboard">
        {{ loading ? '刷新中...' : '刷新' }}
      </button>
    </section>

    <section class="stats-grid dashboard-stats-grid" :class="{ loading: loading }">
      <article class="stat-card dashboard-stat-card calm">
        <strong>{{ moneyText(dashboard.totalWealthAssets) }}</strong>
        <span>总资产</span>
      </article>
      <article class="stat-card dashboard-stat-card">
        <strong>{{ moneyText(dashboard.disposableWealthAssets) }}</strong>
        <span>可支配资产</span>
      </article>
      <article class="stat-card dashboard-stat-card warning">
        <strong>{{ dashboard.expiringIngredientCount || 0 }}</strong>
        <span>快过期食材</span>
        <small>三个月内到期</small>
      </article>
      <article class="stat-card dashboard-stat-card danger">
        <strong>{{ dashboard.pendingTodoCount || 0 }}</strong>
        <span>未完成代办</span>
      </article>
    </section>
  </section>
</template>

<script>
import { getSummary } from '../api/assets'

export default {
  name: 'AssetView',
  data() {
    return {
      dashboard: emptyDashboard(),
      loading: false
    }
  },
  created() {
    this.loadDashboard()
  },
  activated() {
    this.loadDashboard()
  },
  methods: {
    async loadDashboard() {
      this.loading = true
      try {
        const data = await getSummary()
        this.dashboard = normalizeDashboard(data)
      } catch (error) {
        this.dashboard = emptyDashboard()
      } finally {
        this.loading = false
        this.$emit('summary-change', this.dashboard)
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

function emptyDashboard() {
  return {
    totalWealthAssets: 0,
    disposableWealthAssets: 0,
    expiringIngredientCount: 0,
    pendingTodoCount: 0
  }
}

function normalizeDashboard(data) {
  return {
    totalWealthAssets: pickNumber(data, 'totalWealthAssets', 'total_wealth_assets'),
    disposableWealthAssets: pickNumber(data, 'disposableWealthAssets', 'disposable_wealth_assets'),
    expiringIngredientCount: pickNumber(data, 'expiringIngredientCount', 'expiring_ingredient_count'),
    pendingTodoCount: pickNumber(data, 'pendingTodoCount', 'pending_todo_count')
  }
}

function pickNumber(data, camelKey, snakeKey) {
  if (!data) {
    return 0
  }
  const value = data[camelKey] != null ? data[camelKey] : data[snakeKey]
  return Number(value || 0)
}
</script>

<style scoped>
.dashboard-page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.dashboard-stats-grid.loading {
  opacity: 0.6;
  pointer-events: none;
}

.dashboard-stat-card small {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: var(--ink-muted);
}
</style>
