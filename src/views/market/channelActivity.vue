<template>
  <div class="channel-activity-page">
    <el-row :gutter="16" class="main-row">
      <!-- 中间地图 4/5 -->
      <el-col :span="16">
        <MapCanvas
          ref="mapRef"
          :filters="filters"
          @stats-update="handleStatsUpdate"
          @store-click="handleStoreClick"
        />
      </el-col>

      <!-- 右侧统计面板 1/5 -->
      <el-col :span="8">
        <div class="right-panel">
          <!-- 筛选条件 -->
          <FilterPanel
            v-model="filters"
            @change="handleFilterChange"
          />

          <!-- 统计卡片 -->
          <StatsPanel
            :stats="stats"
            :loading="statsLoading"
          />
        </div>
      </el-col>
    </el-row>

    <!-- 门店详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="门店详情"
      size="400px"
      direction="rtl"
    >
      <div class="store-detail" v-if="selectedStore">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="网点名称">
            {{ selectedStore.name }}
          </el-descriptions-item>
          <el-descriptions-item label="补货频次">
            <el-tag :type="getLevelType(selectedStore.replenishLevel)" size="small">
              {{ getLevelText(selectedStore.replenishLevel) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="月销量">
            {{ selectedStore.sales }}万
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import FilterPanel from './channelActivity/FilterPanel.vue'
import MapCanvas from './channelActivity/MapCanvas.vue'
import StatsPanel from './channelActivity/StatsPanel.vue'

const mapRef = ref()
const statsLoading = ref(false)
const drawerVisible = ref(false)
const selectedStore = ref(null)

const filters = reactive({
  province: '',
  city: '',
  district: '',
  category: '',
  brand: '',
  priceRange: '',
  visitFreq: ''
})

const stats = reactive({
  storeStats: { high: 0, medium: 0, low: 0 },
  districtStats: []
})

const handleFilterChange = (newFilters) => {
  console.log('筛选条件变化:', newFilters)
}

const handleStatsUpdate = (data) => {
  stats.storeStats = data.storeStats
  stats.districtStats = data.districtStats
}

const handleStoreClick = (store) => {
  selectedStore.value = store
  drawerVisible.value = true
}

const getLevelType = (level) => {
  const types = { high: 'success', medium: 'warning', low: 'info' }
  return types[level] || 'info'
}

const getLevelText = (level) => {
  const texts = { high: '高补货频次', medium: '中补货频次', low: '低补货频次' }
  return texts[level] || '未知'
}
</script>

<style scoped>
.channel-activity-page {
  padding: 16px;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.main-row {
  flex: 1;
  min-height: 0;
}

.main-row > .el-col {
  height: 100%;
}

.right-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.store-detail {
  padding: 0 16px;
}
</style>
