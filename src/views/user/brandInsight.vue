<template>
  <div class="brand-insight-page">
    <el-card shadow="hover">
      <template #header>
        <span>品牌洞察</span>
      </template>

      <el-row :gutter="20" style="height: 580px;">
        <!-- 左侧城市列表 -->
        <el-col :span="5">
          <div class="city-panel">
            <h4>河北省城市列表</h4>
            <el-input
              v-model="searchKeyword"
              placeholder="搜索城市"
              size="default"
              clearable
              style="margin-bottom: 12px;">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <div class="city-list">
              <div
                v-for="city in filteredCities"
                :key="city.code"
                class="city-item"
                :class="{ active: selectedCity === city.code, [city.heatLevel]: true }"
                @click="handleSelectCity(city)"
              >
                <div class="city-info">
                  <span class="city-name">{{ city.name }}</span>
                  <span class="city-rank">TOP {{ city.rank }}</span>
                </div>
                <div class="city-meta">
                  <span class="heat-tag" :class="city.heatLevel">{{ getHeatLabel(city.heatLevel) }}</span>
                  <span class="sales">{{ city.sales }}万</span>
                </div>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 中间地图 -->
        <el-col :span="12">
          <div class="map-wrapper">
            <div ref="mapContainer" class="map-container"></div>
            <div v-if="loading" class="map-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>地图加载中...</span>
            </div>
            <div class="map-legend">
              <div class="legend-title">品牌热度</div>
              <div class="legend-items">
                <div class="legend-item">
                  <span class="color-box high"></span>
                  <span>高热度</span>
                </div>
                <div class="legend-item">
                  <span class="color-box medium"></span>
                  <span>中热度</span>
                </div>
                <div class="legend-item">
                  <span class="color-box low"></span>
                  <span>低热度</span>
                </div>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 右侧统计面板 -->
        <el-col :span="7">
          <div class="stats-panel">
            <!-- 品牌热度统计卡片 -->
            <el-card shadow="hover" class="heat-stats-card">
              <template #header>
                <div class="card-header">
                  <span>品牌热度分布</span>
                </div>
              </template>
              <div class="heat-stats">
                <div class="heat-stat-item high">
                  <div class="stat-num">{{ heatStats.high }}</div>
                  <div class="stat-label">高热度城市</div>
                </div>
                <div class="heat-stat-item medium">
                  <div class="stat-num">{{ heatStats.medium }}</div>
                  <div class="stat-label">中热度城市</div>
                </div>
                <div class="heat-stat-item low">
                  <div class="stat-num">{{ heatStats.low }}</div>
                  <div class="stat-label">低热度城市</div>
                </div>
              </div>
            </el-card>

            <!-- 选中城市详情 -->
            <el-card shadow="hover" class="city-detail-card" v-if="selectedCityData">
              <template #header>
                <div class="card-header">
                  <span>{{ selectedCityData.name }}</span>
                  <el-tag size="small" :type="getHeatTagType(selectedCityData.heatLevel)">
                    {{ getHeatLabel(selectedCityData.heatLevel) }}
                  </el-tag>
                </div>
              </template>
              <div class="city-detail">
                <div class="detail-row">
                  <span class="label">品牌销量</span>
                  <span class="value">{{ selectedCityData.sales }} 万元</span>
                </div>
                <div class="detail-row">
                  <span class="label">市场占有率</span>
                  <span class="value">{{ selectedCityData.marketShare }}%</span>
                </div>
                <div class="detail-row">
                  <span class="label">网点数量</span>
                  <span class="value">{{ selectedCityData.storeCount }} 家</span>
                </div>
                <div class="detail-row">
                  <span class="label">同比增长率</span>
                  <span class="value" :style="{ color: selectedCityData.growth >= 0 ? '#67c23a' : '#f56c6c' }">
                    {{ selectedCityData.growth >= 0 ? '+' : '' }}{{ selectedCityData.growth }}%
                  </span>
                </div>
              </div>
            </el-card>

            <!-- 热度排行榜 -->
            <el-card shadow="hover" class="rank-card">
              <template #header>
                <div class="card-header">
                  <span>品牌热度排行</span>
                  <el-button type="success" size="small" @click="handleExport">
                    <el-icon><Download /></el-icon>
                    导出
                  </el-button>
                </div>
              </template>
              <div class="rank-list">
                <el-table :data="cityRankList" stripe size="small" max-height="200">
                  <el-table-column prop="rank" label="排名" width="50" />
                  <el-table-column prop="name" label="城市" />
                  <el-table-column prop="sales" label="销量(万)" width="80" />
                  <el-table-column label="热度" width="60">
                    <template #default="{ row }">
                      <span class="heat-dot" :class="row.heatLevel"></span>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-card>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, shallowRef, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Search, Loading } from '@element-plus/icons-vue'
import maplibregl from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'

const mapContainer = ref()
const mapInstance = shallowRef(null)
const loading = ref(true)
const cityGeoJSON = ref(null)

const searchKeyword = ref('')
const selectedCity = ref(null)
const cityData = ref([])

// 城市标注层实例，用于管理标注
const cityMarkers = []

// 初始化纯净地图（无行政区划边界）
const initMap = () => {
  if (!mapContainer.value) return

  // 使用 OSM 纯净底图（无标注）
  const map = new maplibregl.Map({
    container: mapContainer.value,
    style: {
      version: 8,
      sources: {
        'osm': {
          type: 'raster',
          tiles: [
            'https://a.basemaps.toolserver.org/basemaps-neutral/{z}/{x}/{y}.png',
            'https://b.basemaps.toolserver.org/basemaps-neutral/{z}/{x}/{y}.png',
            'https://c.basemaps.toolserver.org/basemaps-neutral/{z}/{x}/{y}.png'
          ],
          tileSize: 256,
          attribution: ''
        }
      },
      layers: [
        {
          id: 'osm-layer',
          type: 'raster',
          source: 'osm',
          minzoom: 0,
          maxzoom: 18
        }
      ]
    },
    center: [114.52, 38.05],
    zoom: 7,
    attributionControl: false
  })

  map.on('load', () => {
    loading.value = false
    mapInstance.value = map
    initCityData()
    cityGeoJSON.value = generateHebeiGeoJSON()
    renderCityLayer()
  })

  map.on('click', (e) => {
    const features = map.queryRenderedFeatures(e.point)
    if (!features?.length) return
    const feature = features[0]
    const code = feature.properties.code
    const city = cityData.value.find(c => c.code === code)
    if (city) {
      handleSelectCity(city)
    }
  })
}

// 清理地图资源
const cleanupMap = () => {
  if (mapInstance.value) {
    mapInstance.value.remove()
    mapInstance.value = null
  }
}

// 河北省城市数据
const hebeiCities = [
  { code: '130100', name: '石家庄', center: [114.52, 38.05], sales: 2850, marketShare: 18.5, storeCount: 1256, growth: 12.3 },
  { code: '130200', name: '唐山', center: [118.18, 39.63], sales: 1980, marketShare: 12.8, storeCount: 892, growth: 8.5 },
  { code: '130300', name: '秦皇岛', center: [119.59, 39.94], sales: 520, marketShare: 3.4, storeCount: 245, growth: -2.1 },
  { code: '130400', name: '邯郸', center: [114.54, 36.62], sales: 1560, marketShare: 10.1, storeCount: 678, growth: 5.2 },
  { code: '130500', name: '邢台', center: [114.50, 37.07], sales: 890, marketShare: 5.8, storeCount: 412, growth: 3.8 },
  { code: '130600', name: '保定', center: [115.47, 38.87], sales: 1720, marketShare: 11.2, storeCount: 756, growth: 9.1 },
  { code: '130700', name: '张家口', center: [114.88, 40.77], sales: 680, marketShare: 4.4, storeCount: 312, growth: 1.5 },
  { code: '130800', name: '承德', center: [117.94, 40.97], sales: 450, marketShare: 2.9, storeCount: 198, growth: -0.8 },
  { code: '130900', name: '沧州', center: [116.83, 38.30], sales: 1350, marketShare: 8.8, storeCount: 589, growth: 6.7 },
  { code: '131000', name: '廊坊', center: [116.68, 39.52], sales: 980, marketShare: 6.4, storeCount: 445, growth: 11.2 },
  { code: '131100', name: '衡水', center: [115.67, 37.74], sales: 720, marketShare: 4.7, storeCount: 334, growth: 4.3 },
  { code: '131200', name: '雄安新区', center: [115.82, 38.97], sales: 280, marketShare: 1.8, storeCount: 125, growth: 25.6 }
]

// 初始化城市数据（添加热度等级和排名）
const initCityData = () => {
  // 按销量排序
  const sorted = [...hebeiCities].sort((a, b) => b.sales - a.sales)

  // 根据排名分配热度等级（前4名高，中4名中，后4名低）
  cityData.value = sorted.map((city, index) => ({
    ...city,
    rank: index + 1,
    heatLevel: index < 4 ? 'high' : index < 8 ? 'medium' : 'low'
  }))
}

// 热度统计
const heatStats = computed(() => {
  return {
    high: cityData.value.filter(c => c.heatLevel === 'high').length,
    medium: cityData.value.filter(c => c.heatLevel === 'medium').length,
    low: cityData.value.filter(c => c.heatLevel === 'low').length
  }
})

// 过滤后的城市列表
const filteredCities = computed(() => {
  if (!searchKeyword.value) return cityData.value
  return cityData.value.filter(c => c.name.includes(searchKeyword.value))
})

// 城市排行榜
const cityRankList = computed(() => {
  return [...cityData.value].sort((a, b) => a.rank - b.rank)
})

// 选中城市详情
const selectedCityData = computed(() => {
  if (!selectedCity.value) return null
  return cityData.value.find(c => c.code === selectedCity.value)
})

// 获取热度标签
const getHeatLabel = (level) => {
  return { high: '高', medium: '中', low: '低' }[level] || '-'
}

// 获取热度标签类型
const getHeatTagType = (level) => {
  return { high: 'danger', medium: 'warning', low: 'info' }[level] || 'info'
}

// 生成河北省城市GeoJSON
const generateHebeiGeoJSON = () => {
  const cities = cityData.value
  return {
    "type": "FeatureCollection",
    "features": [
      {
        "type": "Feature",
        "properties": { code: '130100', name: '石家庄', heatLevel: cities.find(c => c.code === '130100')?.heatLevel },
        "geometry": { "type": "Polygon", "coordinates": [[[114.1, 37.7], [114.9, 37.7], [114.9, 38.4], [114.1, 38.4], [114.1, 37.7]]] }
      },
      {
        "type": "Feature",
        "properties": { code: '130200', name: '唐山', heatLevel: cities.find(c => c.code === '130200')?.heatLevel },
        "geometry": { "type": "Polygon", "coordinates": [[[117.5, 39.2], [119.0, 39.2], [119.0, 40.0], [117.5, 40.0], [117.5, 39.2]]] }
      },
      {
        "type": "Feature",
        "properties": { code: '130300', name: '秦皇岛', heatLevel: cities.find(c => c.code === '130300')?.heatLevel },
        "geometry": { "type": "Polygon", "coordinates": [[[119.0, 39.7], [119.9, 39.7], [119.9, 40.2], [119.0, 40.2], [119.0, 39.7]]] }
      },
      {
        "type": "Feature",
        "properties": { code: '130400', name: '邯郸', heatLevel: cities.find(c => c.code === '130400')?.heatLevel },
        "geometry": { "type": "Polygon", "coordinates": [[[114.0, 36.2], [115.1, 36.2], [115.1, 37.0], [114.0, 37.0], [114.0, 36.2]]] }
      },
      {
        "type": "Feature",
        "properties": { code: '130500', name: '邢台', heatLevel: cities.find(c => c.code === '130500')?.heatLevel },
        "geometry": { "type": "Polygon", "coordinates": [[[114.2, 36.8], [115.2, 36.8], [115.2, 37.4], [114.2, 37.4], [114.2, 36.8]]] }
      },
      {
        "type": "Feature",
        "properties": { code: '130600', name: '保定', heatLevel: cities.find(c => c.code === '130600')?.heatLevel },
        "geometry": { "type": "Polygon", "coordinates": [[[114.1, 38.4], [115.9, 38.4], [115.9, 39.2], [114.1, 39.2], [114.1, 38.4]]] }
      },
      {
        "type": "Feature",
        "properties": { code: '130700', name: '张家口', heatLevel: cities.find(c => c.code === '130700')?.heatLevel },
        "geometry": { "type": "Polygon", "coordinates": [[[114.2, 40.3], [115.6, 40.3], [115.6, 41.2], [114.2, 41.2], [114.2, 40.3]]] }
      },
      {
        "type": "Feature",
        "properties": { code: '130800', name: '承德', heatLevel: cities.find(c => c.code === '130800')?.heatLevel },
        "geometry": { "type": "Polygon", "coordinates": [[[117.0, 40.5], [118.5, 40.5], [118.5, 41.5], [117.0, 41.5], [117.0, 40.5]]] }
      },
      {
        "type": "Feature",
        "properties": { code: '130900', name: '沧州', heatLevel: cities.find(c => c.code === '130900')?.heatLevel },
        "geometry": { "type": "Polygon", "coordinates": [[[115.8, 38.0], [117.3, 38.0], [117.3, 38.6], [115.8, 38.6], [115.8, 38.0]]] }
      },
      {
        "type": "Feature",
        "properties": { code: '131000', name: '廊坊', heatLevel: cities.find(c => c.code === '131000')?.heatLevel },
        "geometry": { "type": "Polygon", "coordinates": [[[116.0, 39.2], [117.0, 39.2], [117.0, 39.8], [116.0, 39.8], [116.0, 39.2]]] }
      },
      {
        "type": "Feature",
        "properties": { code: '131100', name: '衡水', heatLevel: cities.find(c => c.code === '131100')?.heatLevel },
        "geometry": { "type": "Polygon", "coordinates": [[[115.2, 37.4], [116.2, 37.4], [116.2, 38.2], [115.2, 38.2], [115.2, 37.4]]] }
      },
      {
        "type": "Feature",
        "properties": { code: '131200', name: '雄安新区', heatLevel: cities.find(c => c.code === '131200')?.heatLevel },
        "geometry": { "type": "Polygon", "coordinates": [[[115.5, 38.7], [116.2, 38.7], [116.2, 39.2], [115.5, 39.2], [115.5, 38.7]]] }
      }
    ]
  }
}

const renderCityLayer = () => {
  if (!mapInstance.value || !cityGeoJSON.value) return

  // 清理旧图层和标注
  if (mapInstance.value.getLayer('city-fill')) {
    mapInstance.value.removeLayer('city-fill')
  }
  if (mapInstance.value.getLayer('city-border')) {
    mapInstance.value.removeLayer('city-border')
  }
  if (mapInstance.value.getSource('city-source')) {
    mapInstance.value.removeSource('city-source')
  }
  // 清理旧标注
  cityMarkers.forEach(marker => marker.remove())
  cityMarkers.length = 0

  // 添加 GeoJSON Source
  mapInstance.value?.addSource('city-source', {
    type: 'geojson',
    data: cityGeoJSON.value
  })

  // 添加填充图层 - 3档色阶
  mapInstance.value?.addLayer({
    id: 'city-fill',
    type: 'fill',
    source: 'city-source',
    paint: {
      'fill-color': [
        'match',
        ['get', 'heatLevel'],
        'high', '#f56c6c',    // 高热度 - 红色
        'medium', '#e6a23c', // 中热度 - 黄色
        'low', '#67c23a',    // 低热度 - 绿色
        '#d3d3d3'            // 默认灰色
      ],
      'fill-opacity': 0.6
    }
  })

  // 添加边框图层
  mapInstance.value?.addLayer({
    id: 'city-border',
    type: 'line',
    source: 'city-source',
    paint: {
      'line-color': [
        'match',
        ['get', 'heatLevel'],
        'high', '#c0392b',    // 高热度边框 - 深红
        'medium', '#c8860a',   // 中热度边框 - 深黄
        'low', '#4a9e2f',     // 低热度边框 - 深绿
        '#999999'             // 默认灰色
      ],
      'line-width': 2
    }
  })

  // 添加城市名称标注
  cityData.value.forEach(city => {
    const marker = new maplibregl.Marker({ color: '#303133' })
      .setLngLat(city.center)
      .setPopup(new maplibregl.Popup({ offset: 15 }).setHTML(`
        <div style="padding: 8px;">
          <strong>${city.name}</strong><br/>
          销量: ${city.sales}万<br/>
          热度: ${getHeatLabel(city.heatLevel)}
        </div>
      `))
      .addTo(mapInstance.value)
    cityMarkers.push(marker)
  })
}

const handleSelectCity = (city) => {
  selectedCity.value = city.code

  // 飞转到该城市
  mapInstance.value?.flyTo({
    center: city.center,
    zoom: 9,
    duration: 1000
  })
}

const handleExport = () => {
  if (cityData.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }

  // 生成 Excel 数据
  const exportData = cityData.value.map(city => ({
    '排名': city.rank,
    '城市编码': city.code,
    '城市名称': city.name,
    '品牌热度': getHeatLabel(city.heatLevel),
    '品牌销量(万元)': city.sales,
    '市场占有率(%)': city.marketShare,
    '网点数量': city.storeCount,
    '同比增长率(%)': city.growth
  }))

  // 转换为 CSV 格式
  const headers = Object.keys(exportData[0])
  const csvContent = [
    headers.join(','),
    ...exportData.map(row => headers.map(h => row[h]).join(','))
  ].join('\n')

  // 下载文件
  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', `河北省品牌热度_${new Date().toLocaleDateString()}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  ElMessage.success('数据已导出')
}

onMounted(() => {
  initMap()
})

onUnmounted(() => {
  cleanupMap()
})
</script>

<style scoped>
.brand-insight-page {
  padding: 20px;
}

.city-panel {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.city-panel h4 {
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e6e6e6;
}

.city-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.city-item {
  padding: 12px;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.city-item:hover {
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.city-item.active {
  border-left-color: #409eff;
  background: #ecf5ff;
}

.city-item.high {
  border-left-color: #f56c6c;
}

.city-item.medium {
  border-left-color: #e6a23c;
}

.city-item.low {
  border-left-color: #67c23a;
}

.city-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.city-name {
  font-weight: 500;
  color: #303133;
}

.city-rank {
  font-size: 12px;
  color: #909399;
}

.city-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.heat-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 3px;
  color: #fff;
}

.heat-tag.high {
  background: #f56c6c;
}

.heat-tag.medium {
  background: #e6a23c;
}

.heat-tag.low {
  background: #67c23a;
}

.sales {
  font-size: 12px;
  color: #606266;
}

.map-wrapper {
  position: relative;
  height: 100%;
  border-radius: 4px;
  overflow: hidden;
}

.map-container {
  width: 100%;
  height: 100%;
}

.map-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #666;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  padding: 20px 40px;
  border-radius: 8px;
}

.map-legend {
  position: absolute;
  bottom: 10px;
  left: 10px;
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
  gap: 6px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #606266;
}

.color-box {
  width: 16px;
  height: 16px;
  border-radius: 3px;
}

.color-box.high {
  background: #f56c6c;
}

.color-box.medium {
  background: #e6a23c;
}

.color-box.low {
  background: #67c23a;
}

.stats-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.heat-stats-card {
  flex-shrink: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.card-header span {
  font-size: 14px;
}

.heat-stats {
  display: flex;
  justify-content: space-around;
  padding: 12px 0;
}

.heat-stat-item {
  text-align: center;
  padding: 12px 20px;
  border-radius: 6px;
}

.heat-stat-item.high {
  background: rgba(245, 108, 108, 0.1);
}

.heat-stat-item.medium {
  background: rgba(230, 162, 60, 0.1);
}

.heat-stat-item.low {
  background: rgba(103, 194, 58, 0.1);
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.heat-stat-item.high .stat-num {
  color: #f56c6c;
}

.heat-stat-item.medium .stat-num {
  color: #e6a23c;
}

.heat-stat-item.low .stat-num {
  color: #67c23a;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.city-detail-card {
  flex-shrink: 0;
}

.city-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-row .label {
  font-size: 13px;
  color: #909399;
}

.detail-row .value {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
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

.rank-list {
  height: 100%;
}

.heat-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.heat-dot.high {
  background: #f56c6c;
}

.heat-dot.medium {
  background: #e6a23c;
}

.heat-dot.low {
  background: #67c23a;
}
</style>
