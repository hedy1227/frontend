<template>
  <div class="map-canvas">
    <!-- 地图工具栏 -->
    <div class="map-toolbar">
      <div class="toolbar-left">
        <el-switch
          v-model="localHeatVisible"
          active-text="用户热力图"
          inactive-text=""
          @change="handleHeatToggle"
        />
        <span class="toolbar-hint" v-if="localHeatVisible">1KM蜂窝网格用户密度热力图</span>
      </div>
    </div>

    <!-- 地图容器 -->
    <div class="map-wrapper">
      <GdsMap
        ref="mapRef"
        :center="[104.094, 30.679]"
        :zoom="12"
        @load="handleMapLoad"
        @click="handleMapClick"
      />

      <!-- 图例 -->
      <div class="map-legend">
        <div class="legend-title">图例</div>
        <div class="legend-items">
          <div class="legend-item">
            <span class="dot active"></span>
            <span>活跃网点</span>
          </div>
          <div class="legend-item">
            <span class="dot normal"></span>
            <span>一般网点</span>
          </div>
          <div class="legend-item" v-if="localHeatVisible">
            <div class="density-legend">
              <span class="legend-label">用户密度:</span>
              <span class="legend-colors">
                <span class="color-item">
                  <span class="color-box low"></span>
                  <span>低</span>
                </span>
                <span class="color-item">
                  <span class="color-box mid"></span>
                  <span>中</span>
                </span>
                <span class="color-item">
                  <span class="color-box high"></span>
                  <span>高</span>
                </span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, shallowRef, onMounted } from 'vue'
import GdsMap from '@/components/GdsMap.vue'
import * as turf from '@turf/turf'

const props = defineProps({
  filters: {
    type: Object,
    default: () => ({})
  },
  heatVisible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:heatVisible', 'store-click'])

const mapRef = ref()
const mapInstance = shallowRef(null)
const localHeatVisible = ref(false)
const storeCount = ref(0)

const hexGridLayerId = 'user-hex-grid'
const storeLayerId = 'front-stores'

// 模拟数据 - 门店散点
const mockStoreData = [
  { lng: 104.063, lat: 30.548, name: '红旗超市(天府店)', status: '活跃', category: '白酒', sales: 128 },
  { lng: 104.071, lat: 30.552, name: '永辉超市(高新店)', status: '活跃', category: '啤酒', sales: 96 },
  { lng: 104.086, lat: 30.659, name: '盒马鲜生(锦江店)', status: '活跃', category: '红酒', sales: 75 },
  { lng: 104.035, lat: 30.625, name: '沃尔玛(武侯店)', status: '一般', category: '白酒', sales: 58 },
  { lng: 104.053, lat: 30.671, name: '家乐福(青羊店)', status: '一般', category: '啤酒', sales: 32 },
  { lng: 104.052, lat: 30.641, name: '舞东风(玉林店)', status: '活跃', category: '白酒', sales: 89 },
  { lng: 104.103, lat: 30.665, name: '红旗超市(建设路)', status: '活跃', category: '啤酒', sales: 67 },
  { lng: 104.091, lat: 30.658, name: '步步高(春熙路)', status: '活跃', category: '红酒', sales: 45 },
  { lng: 104.075, lat: 30.572, name: '大润发(双楠店)', status: '活跃', category: '白酒', sales: 112 },
  { lng: 104.118, lat: 30.634, name: '欧尚(龙潭寺)', status: '一般', category: '啤酒', sales: 38 }
]

// 蜂窝网格数据
let hexGridData = null

// 生成 1KM 蜂窝网格覆盖成都市区域
const generateHexGrid = () => {
  // 成都市中心坐标
  const centerLng = 104.094
  const centerLat = 30.679

  // 覆盖范围约 30KM * 30KM 的矩形区域
  const bbox = [centerLng - 0.18, centerLat - 0.15, centerLng + 0.18, centerLat + 0.15]

  // 使用 turf 生成蜂窝网格，radius 单位为公里
  const hexGrid = turf.hexGrid(bbox, 1, { units: 'kilometers' })

  // 为每个蜂窝生成模拟用户密度值
  hexGrid.features.forEach((feature, index) => {
    // 以市中心为基准，生成符合现实规律的热力分布
    // 越靠近市中心密度越高，边缘逐渐降低
    const hexCenter = turf.centroid(feature)
    const distToCenter = turf.distance(
      turf.point([centerLng, centerLat]),
      hexCenter,
      { units: 'kilometers' }
    )

    // 根据距离计算基础密度，加上随机波动
    const baseDensity = Math.max(0, 1 - distToCenter / 15)
    const randomFactor = 0.3 + Math.random() * 0.7
    feature.properties = {
      gridId: `hex_${index}`,
      density: Math.min(1, baseDensity * randomFactor),
      userCount: Math.floor(baseDensity * randomFactor * 500)
    }
  })

  hexGridData = hexGrid
  return hexGrid
}

const handleMapLoad = async (map) => {
  mapInstance.value = map
  generateHexGrid()
  // 先加载网格，再加载散点，保证散点在网格上方
  loadHexGrid()
  loadStoreData()
}

const loadStoreData = () => {
  if (!mapInstance.value) return

  // 模拟根据筛选条件过滤数据
  let filteredData = [...mockStoreData]
  if (props.filters.category) {
    filteredData = filteredData.filter(d => d.category === props.filters.category)
  }

  storeCount.value = filteredData.length

  const scatterData = filteredData.map(item => ({
    coordinates: [item.lng, item.lat],
    properties: {
      name: item.name,
      status: item.status,
      sales: item.sales
    }
  }))

  // 移除旧图层
  if (mapRef.value) {
    mapRef.value.removeLayer(storeLayerId)
  }

  mapRef.value?.addScatterLayer(storeLayerId, scatterData, {
    radius: 10,
    color: '#409eff'
  })
}

const loadHexGrid = () => {
  if (!mapInstance.value || !hexGridData) return

  // 清理旧图层
  if (mapRef.value) {
    mapRef.value.removeLayer(hexGridLayerId)
  }

  // 添加蜂窝网格图层，使用 5 档色阶
  mapRef.value?.addRoadLayer(hexGridLayerId, hexGridData, {
    colorField: 'density',
    opacity: 0.7
  })

  // 默认隐藏，等用户开启热力图
  mapRef.value?.toggleLayerVisibility(hexGridLayerId, false)
}

const handleHeatToggle = (visible) => {
  if (!mapInstance.value) return

  mapRef.value?.toggleLayerVisibility(hexGridLayerId, visible)

  // 散点已在网格上方，无需调整透明度
  emit('update:heatVisible', visible)
}

const handleMapClick = (e) => {
  if (!e.features?.length) return
  const feature = e.features[0]
  if (feature.properties?.name) {
    emit('store-click', feature.properties)
  }
}

// 监听筛选条件变化
watch(() => props.filters, () => {
  loadStoreData()
}, { deep: true })

// 监听外部 heatVisible 变化
watch(() => props.heatVisible, (v) => {
  localHeatVisible.value = v
})

defineExpose({
  flyTo: (coords, zoom = 15) => {
    mapRef.value?.flyTo({ center: coords, zoom, duration: 1000 })
  }
})
</script>

<style scoped>
.map-canvas {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.map-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-radius: 4px 4px 0 0;
  border-bottom: 1px solid #ebeef5;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-hint {
  font-size: 12px;
  color: #909399;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.store-count {
  font-size: 14px;
  color: #606266;
}

.store-count strong {
  color: #409eff;
  font-size: 16px;
}

.map-wrapper {
  flex: 1;
  position: relative;
  min-height: 0;
}

.map-wrapper .gds-map-container {
  border-radius: 0 0 4px 4px;
}

.map-legend {
  position: absolute;
  bottom: 40px;
  left: 16px;
  background: rgba(255, 255, 255, 0.95);
  padding: 12px 16px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1;
}

.legend-title {
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 13px;
}

.legend-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #606266;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.dot.active {
  background: #67c23a;
  box-shadow: 0 0 4px #67c23a;
}

.dot.normal {
  background: #909399;
}

.density-legend {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.legend-label {
  font-size: 12px;
  color: #606266;
}

.legend-colors {
  display: flex;
  gap: 12px;
}

.color-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
}

.color-box {
  width: 16px;
  height: 16px;
  border-radius: 2px;
}

.color-box.low { background: #67c23a; }
.color-box.mid { background: #e6a23c; }
.color-box.high { background: #f56c6c; }
</style>
