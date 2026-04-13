<template>
  <div class="heatmap-page">
    <el-row :gutter="16" class="main-row">
      <!-- 左侧 1/5 筛选面板 -->
      <el-col :span="5">
        <FilterPanel
          v-model="filters"
          @change="handleFilterChange"
        />
      </el-col>

      <!-- 中间 3/5 地图大屏 -->
      <el-col :span="14">
        <MapCanvas
          ref="mapRef"
          :filters="filters"
          :heat-visible="heatVisible"
          @update:heat-visible="heatVisible = $event"
          @store-click="handleStoreClick"
        />
      </el-col>

      <!-- 右侧 1/5 统计卡片 -->
      <el-col :span="5">
        <StatsPanel
          :filters="filters"
          :stats="stats"
          :loading="statsLoading"
          @rank-click="handleRankClick"
        />
      </el-col>
    </el-row>

    <!-- 门店详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="网点详情"
      size="400px"
      direction="rtl"
    >
      <div class="store-detail" v-if="selectedStore">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="网点名称">
            {{ selectedStore.name }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedStore.status === '活跃' ? 'success' : 'info'" size="small">
              {{ selectedStore.status }}
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
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import FilterPanel from './heatMap/FilterPanel.vue'
import MapCanvas from './heatMap/MapCanvas.vue'
import StatsPanel from './heatMap/StatsPanel.vue'

const mapRef = ref()
const heatVisible = ref(false)
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
  replenishFreq: '',
  visitFreq: ''
})

// 模拟统计数据
const stats = reactive({
  total: 12856,
  target: 10000,
  densityRanking: [
    { rank: 1, districtName: '成华区', storeCount: 3420, targetCount: 3800, density: 0.90 },
    { rank: 2, districtName: '锦江区', storeCount: 2890, targetCount: 3200, density: 0.85 },
    { rank: 3, districtName: '武侯区', storeCount: 2650, targetCount: 3100, density: 0.78 },
    { rank: 4, districtName: '青羊区', storeCount: 2100, targetCount: 2800, density: 0.65 },
    { rank: 5, districtName: '金牛区', storeCount: 1796, targetCount: 2600, density: 0.52 }
  ]
})

const handleFilterChange = (newFilters) => {
  // 筛选条件变化时重新加载数据
  // 目前使用模拟数据，如果需要调用 API 可以在这里实现
  console.log('筛选条件变化:', newFilters)
}

const handleStoreClick = (store) => {
  selectedStore.value = store
  drawerVisible.value = true

  // 飞转到该网点
  const storeData = {
    chengdu: { lng: 104.063, lat: 30.548, name: '红旗超市(天府店)' },
    gaoxin: { lng: 104.071, lat: 30.552, name: '永辉超市(高新店)' },
    jinjiang: { lng: 104.086, lat: 30.659, name: '盒马鲜生(锦江店)' }
  }

  // 根据门店名称匹配坐标
  const coords = {
    '红旗超市(天府店)': [104.063, 30.548],
    '永辉超市(高新店)': [104.071, 30.552],
    '盒马鲜生(锦江店)': [104.086, 30.659],
    '沃尔玛(武侯店)': [104.035, 30.625],
    '家乐福(青羊店)': [104.053, 30.671],
    '舞东风(玉林店)': [104.052, 30.641],
    '红旗超市(建设路)': [104.103, 30.665],
    '步步高(春熙路)': [104.091, 30.658],
    '大润发(双楠店)': [104.075, 30.572],
    '欧尚(龙潭寺)': [104.118, 30.634]
  }

  if (store.name && coords[store.name]) {
    mapRef.value?.flyTo(coords[store.name], 15)
  }
}

const handleRankClick = (item) => {
  ElMessage.info(`查看 ${item.districtName} 区域详情`)
}
</script>

<style scoped>
.heatmap-page {
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

.main-row > .el-col:nth-child(2) {
  height: 100%;
}

.store-detail {
  padding: 0 16px;
}
</style>
