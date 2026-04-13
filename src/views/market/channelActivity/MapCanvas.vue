<template>
  <div class="map-canvas">
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
        <div class="legend-title">渠道活跃度</div>
        <div class="legend-items">
          <div class="legend-item">
            <span class="dot high"></span>
            <span>高补货频次</span>
          </div>
          <div class="legend-item">
            <span class="dot medium"></span>
            <span>中补货频次</span>
          </div>
          <div class="legend-item">
            <span class="dot low"></span>
            <span>低补货频次</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, shallowRef } from 'vue'
import GdsMap from '@/components/GdsMap.vue'
import * as turf from '@turf/turf'

const props = defineProps({
  filters: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['store-click', 'stats-update'])

const mapRef = ref()
const mapInstance = shallowRef(null)

const hexGridLayerId = 'channel-hex-grid'
const hexBorderLayerId = 'channel-hex-border'
const storeLayerId = 'channel-stores'

// 模拟门店数据，包含补货频次等级
const mockStoreData = [
  { lng: 104.063, lat: 30.548, name: '红旗超市(天府店)', status: '活跃', category: '白酒', brand: '劲牌', priceRange: '20-50', visitFreq: 'high', replenishLevel: 'high', sales: 128, district: '高新区' },
  { lng: 104.071, lat: 30.552, name: '永辉超市(高新店)', status: '活跃', category: '啤酒', brand: '其它', priceRange: '50-100', visitFreq: 'medium', replenishLevel: 'high', sales: 96, district: '高新区' },
  { lng: 104.086, lat: 30.659, name: '盒马鲜生(锦江店)', status: '活跃', category: '红酒', brand: '其它', priceRange: '100+', visitFreq: 'high', replenishLevel: 'high', sales: 75, district: '锦江区' },
  { lng: 104.035, lat: 30.625, name: '沃尔玛(武侯店)', status: '活跃', category: '白酒', brand: '劲牌', priceRange: '50-100', visitFreq: 'medium', replenishLevel: 'medium', sales: 58, district: '武侯区' },
  { lng: 104.053, lat: 30.671, name: '家乐福(青羊店)', status: '一般', category: '啤酒', brand: '其它', priceRange: '0-20', visitFreq: 'low', replenishLevel: 'low', sales: 32, district: '青羊区' },
  { lng: 104.052, lat: 30.641, name: '舞东风(玉林店)', status: '活跃', category: '白酒', brand: '劲牌', priceRange: '20-50', visitFreq: 'high', replenishLevel: 'high', sales: 89, district: '武侯区' },
  { lng: 104.103, lat: 30.665, name: '红旗超市(建设路)', status: '活跃', category: '啤酒', brand: '劲牌', priceRange: '20-50', visitFreq: 'medium', replenishLevel: 'medium', sales: 67, district: '成华区' },
  { lng: 104.091, lat: 30.658, name: '步步高(春熙路)', status: '活跃', category: '红酒', brand: '其它', priceRange: '50-100', visitFreq: 'high', replenishLevel: 'high', sales: 45, district: '锦江区' },
  { lng: 104.075, lat: 30.572, name: '大润发(双楠店)', status: '活跃', category: '白酒', brand: '劲牌', priceRange: '20-50', visitFreq: 'medium', replenishLevel: 'medium', sales: 112, district: '武侯区' },
  { lng: 104.118, lat: 30.634, name: '欧尚(龙潭寺)', status: '一般', category: '啤酒', brand: '其它', priceRange: '0-20', visitFreq: 'low', replenishLevel: 'low', sales: 38, district: '成华区' },
  { lng: 104.055, lat: 30.685, name: '红旗超市(动物园)', status: '活跃', category: '白酒', brand: '劲牌', priceRange: '20-50', visitFreq: 'high', replenishLevel: 'high', sales: 78, district: '成华区' },
  { lng: 104.112, lat: 30.595, name: '永辉超市(龙泉驿)', status: '一般', category: '啤酒', brand: '其它', priceRange: '50-100', visitFreq: 'medium', replenishLevel: 'medium', sales: 52, district: '龙泉驿' },
  { lng: 104.028, lat: 30.658, name: '家乐福(双流店)', status: '一般', category: '红酒', brand: '其它', priceRange: '100+', visitFreq: 'low', replenishLevel: 'low', sales: 28, district: '双流区' },
  { lng: 104.135, lat: 30.662, name: '舞东风(十陵店)', status: '活跃', category: '白酒', brand: '劲牌', priceRange: '20-50', visitFreq: 'high', replenishLevel: 'high', sales: 85, district: '成华区' },
  { lng: 104.065, lat: 30.615, name: '步步高(武侯祠)', status: '一般', category: '啤酒', brand: '其它', priceRange: '0-20', visitFreq: 'low', replenishLevel: 'low', sales: 35, district: '武侯区' },
  { lng: 104.078, lat: 30.698, name: '永辉超市(昭觉寺)', status: '活跃', category: '白酒', brand: '劲牌', priceRange: '50-100', visitFreq: 'high', replenishLevel: 'high', sales: 92, district: '成华区' },
  { lng: 104.045, lat: 30.652, name: '红旗超市(金沙店)', status: '一般', category: '啤酒', brand: '其它', priceRange: '20-50', visitFreq: 'medium', replenishLevel: 'medium', sales: 48, district: '青羊区' },
  { lng: 104.098, lat: 30.612, name: '家乐福(中和店)', status: '一般', category: '红酒', brand: '其它', priceRange: '0-20', visitFreq: 'low', replenishLevel: 'low', sales: 25, district: '高新区' },
  { lng: 104.122, lat: 30.568, name: '舞东风(东客站)', status: '活跃', category: '白酒', brand: '劲牌', priceRange: '20-50', visitFreq: 'high', replenishLevel: 'high', sales: 82, district: '成华区' },
  { lng: 104.038, lat: 30.628, name: '步步高(黄田坝)', status: '一般', category: '啤酒', brand: '其它', priceRange: '50-100', visitFreq: 'medium', replenishLevel: 'medium', sales: 42, district: '青羊区' }
]

// 蜂窝网格数据
let hexGridData = null

// 生成蜂窝网格
const generateHexGrid = () => {
  // 成都市中心坐标
  const centerLng = 104.094
  const centerLat = 30.679

  // 覆盖范围约 30KM * 30KM 的矩形区域
  const bbox = [centerLng - 0.18, centerLat - 0.15, centerLng + 0.18, centerLat + 0.15]

  // 使用 turf 生成蜂窝网格，radius 单位为公里
  const hexGrid = turf.hexGrid(bbox, 1, { units: 'kilometers' })

  return turf.featureCollection(hexGrid.features)
}

const handleMapLoad = async (map) => {
  mapInstance.value = map
  loadHexGrid()
  loadStoreData()
}

const loadHexGrid = () => {
  if (!mapInstance.value) return

  // 生成蜂窝网格
  hexGridData = generateHexGrid()

  // 清理旧图层
  if (mapRef.value) {
    mapRef.value.removeLayer(hexGridLayerId)
    mapRef.value.removeLayer(hexBorderLayerId)
  }

  // 添加蜂窝网格 Source
  mapInstance.value?.addSource(hexGridLayerId, {
    type: 'geojson',
    data: hexGridData
  })

  // 添加蜂窝边框图层（只显示边框，不填充）
  mapInstance.value?.addLayer({
    id: hexBorderLayerId,
    type: 'line',
    source: hexGridLayerId,
    paint: {
      'line-color': '#666666',
      'line-width': 1,
      'line-opacity': 0.5
    }
  })
}

const loadStoreData = () => {
  if (!mapInstance.value) return

  // 根据筛选条件过滤数据
  let filteredData = [...mockStoreData]

  if (props.filters.category) {
    filteredData = filteredData.filter(d => d.category === props.filters.category)
  }
  if (props.filters.brand) {
    filteredData = filteredData.filter(d => d.brand === props.filters.brand)
  }
  if (props.filters.priceRange) {
    filteredData = filteredData.filter(d => d.priceRange === props.filters.priceRange)
  }
  if (props.filters.visitFreq) {
    filteredData = filteredData.filter(d => d.visitFreq === props.filters.visitFreq)
  }

  // 清理旧散点图层
  if (mapRef.value) {
    mapRef.value.removeLayer(storeLayerId)
    if (mapInstance.value?.getSource(storeLayerId)) {
      mapInstance.value.removeSource(storeLayerId)
    }
  }

  // 添加散点 Source
  mapInstance.value?.addSource(storeLayerId, {
    type: 'geojson',
    data: {
      type: 'FeatureCollection',
      features: filteredData.map(item => ({
        type: 'Feature',
        properties: {
          name: item.name,
          status: item.status,
          replenishLevel: item.replenishLevel,
          sales: item.sales
        },
        geometry: {
          type: 'Point',
          coordinates: [item.lng, item.lat]
        }
      }))
    }
  })

  // 添加散点图层，使用分类颜色
  mapInstance.value?.addLayer({
    id: storeLayerId,
    type: 'circle',
    source: storeLayerId,
    paint: {
      'circle-radius': 10,
      'circle-color': [
        'match',
        ['get', 'replenishLevel'],
        'high', '#67c23a',
        'medium', '#e6a23c',
        'low', '#909399',
        '#409eff'
      ],
      'circle-opacity': 1,
      'circle-stroke-width': 2,
      'circle-stroke-color': '#ffffff'
    }
  })

  // 更新统计
  const stats = {
    high: filteredData.filter(d => d.replenishLevel === 'high').length,
    medium: filteredData.filter(d => d.replenishLevel === 'medium').length,
    low: filteredData.filter(d => d.replenishLevel === 'low').length
  }

  // 按区域统计
  const districts = ['成华区', '锦江区', '武侯区', '青羊区', '金牛区', '高新区']
  const districtStats = districts.map(district => {
    const districtStores = filteredData.filter(d => d.district === district)
    return {
      district,
      high: districtStores.filter(s => s.replenishLevel === 'high').length,
      medium: districtStores.filter(s => s.replenishLevel === 'medium').length,
      low: districtStores.filter(s => s.replenishLevel === 'low').length
    }
  })

  emit('stats-update', { storeStats: stats, districtStats })
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

.map-wrapper {
  flex: 1;
  position: relative;
  min-height: 0;
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
  width: 14px;
  height: 14px;
  border-radius: 50%;
}

.dot.high {
  background: #67c23a;
  box-shadow: 0 0 4px #67c23a;
}

.dot.medium {
  background: #e6a23c;
  box-shadow: 0 0 4px #e6a23c;
}

.dot.low {
  background: #909399;
}
</style>
