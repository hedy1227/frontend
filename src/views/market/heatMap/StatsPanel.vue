<template>
  <div class="stats-panel">
    <!-- 前端网点总数卡片 -->
    <el-card shadow="hover" class="stat-card highlight">
      <div class="stat-content">
        <div class="stat-icon">
          <el-icon><OfficeBuilding /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">前端网点总数</div>
          <div class="stat-value">{{ stats.total?.toLocaleString() ?? '--' }}</div>
        </div>
      </div>
    </el-card>

    <!-- 目标门店数量卡片 -->
    <el-card shadow="hover" class="stat-card target">
      <div class="stat-content">
        <div class="stat-icon target">
          <el-icon><Aim /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">目标门店数量</div>
          <div class="stat-value">{{ stats.target?.toLocaleString() ?? '--' }}</div>
        </div>
      </div>
    </el-card>

    <!-- 区域门店密度排行卡片 -->
    <el-card shadow="hover" class="rank-card">
      <template #header>
        <div class="card-header">
          <span>区域门店密度排行</span>
          <el-tag size="small" type="info">TOP 5</el-tag>
        </div>
      </template>

      <div class="rank-list" v-loading="loading">
        <div
          v-for="item in stats.densityRanking"
          :key="item.rank"
          class="rank-item"
          @click="handleRankClick(item)"
        >
          <span class="rank-num" :class="{ top: item.rank <= 3 }">
            {{ item.rank }}
          </span>
          <div class="rank-info">
            <span class="rank-name">{{ item.districtName }}</span>
            <span class="rank-count">{{ item.storeCount }} / {{ item.targetCount }}</span>
          </div>
          <div class="rank-bar-wrap">
            <div
              class="rank-bar"
              :style="{ width: (item.density * 100) + '%', background: getDensityColor(item.density) }"
            ></div>
          </div>
          <span class="rank-pct">{{ (item.density * 100).toFixed(0) }}%</span>
        </div>

        <el-empty
          v-if="!loading && (!stats.densityRanking || stats.densityRanking.length === 0)"
          description="暂无数据"
          :image-size="60"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { OfficeBuilding, Aim } from '@element-plus/icons-vue'

const props = defineProps({
  filters: {
    type: Object,
    default: () => ({})
  },
  stats: {
    type: Object,
    default: () => ({
      total: 0,
      target: 0,
      densityRanking: []
    })
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['rank-click'])

const getDensityColor = (density) => {
  if (density >= 0.8) return '#67c23a'
  if (density >= 0.6) return '#409eff'
  if (density >= 0.4) return '#e6a23c'
  return '#f56c6c'
}

const handleRankClick = (item) => {
  emit('rank-click', item)
}
</script>

<style scoped>
.stats-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-card {
  flex-shrink: 0;
}

.stat-card.highlight {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #fff;
}

.stat-card.highlight :deep(.el-card__body) {
  padding: 16px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: rgba(64, 158, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #409eff;
}

.stat-card.highlight .stat-icon {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.stat-icon.target {
  background: rgba(103, 194, 58, 0.15);
  color: #67c23a;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-card.highlight .stat-label {
  color: rgba(255, 255, 255, 0.85);
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stat-card.highlight .stat-value {
  color: #fff;
}

.stat-card.target {
  border-left: 3px solid #67c23a;
}

.rank-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.rank-card :deep(.el-card__body) {
  flex: 1;
  overflow-y: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.rank-item:hover {
  background: #ecf5ff;
  transform: translateX(4px);
}

.rank-num {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: #c0c4cc;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-num.top {
  background: linear-gradient(135deg, #ffd700 0%, #ffb347 100%);
  color: #664400;
}

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-name {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-count {
  display: block;
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.rank-bar-wrap {
  width: 50px;
  height: 6px;
  background: #e6e8eb;
  border-radius: 3px;
  overflow: hidden;
}

.rank-bar {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.rank-pct {
  width: 36px;
  text-align: right;
  font-size: 12px;
  font-weight: 600;
  color: #606266;
}
</style>
