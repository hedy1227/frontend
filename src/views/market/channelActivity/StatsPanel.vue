<template>
  <div class="stats-panel">
    <!-- 三类门店数量统计 -->
    <div class="stats-cards">
      <el-card shadow="hover" class="stat-card high">
        <div class="stat-content">
          <div class="stat-icon">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">高补货频次</div>
            <div class="stat-value">{{ stats.storeStats.high }}</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card medium">
        <div class="stat-content">
          <div class="stat-icon">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">中补货频次</div>
            <div class="stat-value">{{ stats.storeStats.medium }}</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card low">
        <div class="stat-content">
          <div class="stat-icon">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">低补货频次</div>
            <div class="stat-value">{{ stats.storeStats.low }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 区域分布统计 -->
    <el-card shadow="hover" class="district-card">
      <template #header>
        <div class="card-header">
          <span>区域分布统计</span>
        </div>
      </template>

      <div class="district-table" v-loading="loading">
        <el-table :data="stats.districtStats" stripe size="small" max-height="320">
          <el-table-column prop="district" label="区域" width="80" />
          <el-table-column label="高补货" width="70" align="center">
            <template #default="{ row }">
              <span class="count high">{{ row.high }}</span>
            </template>
          </el-table-column>
          <el-table-column label="中补货" width="70" align="center">
            <template #default="{ row }">
              <span class="count medium">{{ row.medium }}</span>
            </template>
          </el-table-column>
          <el-table-column label="低补货" width="70" align="center">
            <template #default="{ row }">
              <span class="count low">{{ row.low }}</span>
            </template>
          </el-table-column>
          <el-table-column label="总计" align="center">
            <template #default="{ row }">
              <span class="count total">{{ row.high + row.medium + row.low }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 分布占比图 -->
    <el-card shadow="hover" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>门店分布占比</span>
        </div>
      </template>
      <div class="pie-chart" v-if="total > 0">
        <div class="pie-container">
          <div
            class="pie-slice high"
            :style="{ '--pct': highPercent + '%' }"
          ></div>
          <div
            class="pie-slice medium"
            :style="{ '--pct': mediumPercent + '%' }"
          ></div>
          <div
            class="pie-slice low"
            :style="{ '--pct': lowPercent + '%' }"
          ></div>
        </div>
        <div class="pie-legend">
          <div class="legend-item">
            <span class="dot high"></span>
            <span class="label">高补货</span>
            <span class="value">{{ highPercent }}%</span>
          </div>
          <div class="legend-item">
            <span class="dot medium"></span>
            <span class="label">中补货</span>
            <span class="value">{{ mediumPercent }}%</span>
          </div>
          <div class="legend-item">
            <span class="dot low"></span>
            <span class="label">低补货</span>
            <span class="value">{{ lowPercent }}%</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无数据" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { TrendCharts, DataLine, Warning } from '@element-plus/icons-vue'

const props = defineProps({
  stats: {
    type: Object,
    default: () => ({
      storeStats: { high: 0, medium: 0, low: 0 },
      districtStats: []
    })
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const total = computed(() => {
  return props.stats.storeStats.high + props.stats.storeStats.medium + props.stats.storeStats.low
})

const highPercent = computed(() => {
  if (total.value === 0) return 0
  return Math.round((props.stats.storeStats.high / total.value) * 100)
})

const mediumPercent = computed(() => {
  if (total.value === 0) return 0
  return Math.round((props.stats.storeStats.medium / total.value) * 100)
})

const lowPercent = computed(() => {
  if (total.value === 0) return 0
  return 100 - highPercent.value - mediumPercent.value
})
</script>

<style scoped>
.stats-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stats-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stat-card {
  flex-shrink: 0;
}

.stat-card.high {
  border-left: 3px solid #67c23a;
}

.stat-card.medium {
  border-left: 3px solid #e6a23c;
}

.stat-card.low {
  border-left: 3px solid #909399;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.stat-card.high .stat-icon {
  background: rgba(103, 194, 58, 0.15);
  color: #67c23a;
}

.stat-card.medium .stat-icon {
  background: rgba(230, 162, 60, 0.15);
  color: #e6a23c;
}

.stat-card.low .stat-icon {
  background: rgba(144, 147, 153, 0.15);
  color: #909399;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.district-card,
.chart-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.district-card :deep(.el-card__body),
.chart-card :deep(.el-card__body) {
  flex: 1;
  overflow-y: auto;
}

.card-header {
  font-weight: 600;
}

.count {
  font-weight: 600;
}

.count.high { color: #67c23a; }
.count.medium { color: #e6a23c; }
.count.low { color: #909399; }
.count.total { color: #409eff; }

.pie-chart {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 10px 0;
}

.pie-container {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: conic-gradient(
    #67c23a var(--pct),
    #e6a23c var(--pct),
    #909399 100%
  );
  position: relative;
}

.pie-legend {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot.high { background: #67c23a; }
.dot.medium { background: #e6a23c; }
.dot.low { background: #909399; }

.legend-item .label {
  flex: 1;
  color: #606266;
}

.legend-item .value {
  font-weight: 600;
  color: #303133;
}
</style>
