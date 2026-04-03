<template>
  <div class="marketing-insight-page">
    <el-card shadow="hover">
      <template #header>
        <span>营销洞察 - 活动适合区域筛选</span>
      </template>

      <el-row :gutter="20" style="height: 540px;">
        <!-- 左侧：筛选条件 -->
        <el-col :span="5">
          <div class="filter-panel">
            <h4>筛选条件</h4>
            <el-form :model="filters" label-position="top">
              <!-- 省、市、区：下拉多选 -->
              <el-form-item label="省份">
                <el-select v-model="filters.provinces" placeholder="选择省份" multiple collapse-tags collapse-tags-tooltip size="default" style="width: 100%;" @change="handleProvinceChange">
                  <el-option label="四川省" value="sichuan" />
                  <el-option label="重庆市" value="chongqing" />
                  <el-option label="湖北省" value="hubei" />
                  <el-option label="陕西省" value="shaanxi" />
                </el-select>
              </el-form-item>

              <el-form-item label="城市">
                <el-select v-model="filters.cities" placeholder="选择城市" multiple collapse-tags collapse-tags-tooltip size="default" style="width: 100%;" @change="handleFilterChange">
                  <el-option v-for="city in availableCities" :key="city.value" :label="city.label" :value="city.value" />
                </el-select>
              </el-form-item>

              <el-form-item label="区县">
                <el-select v-model="filters.districts" placeholder="选择区县" multiple collapse-tags collapse-tags-tooltip size="default" style="width: 100%;" @change="handleFilterChange">
                  <el-option v-for="d in availableDistricts" :key="d.value" :label="d.label" :value="d.value" />
                </el-select>
              </el-form-item>

              <el-form-item label="市场动销(万元/月)">
                <el-input-number
                  v-model="filters.marketActivity"
                  :min="0"
                  :max="100"
                  :step="5"
                  placeholder="0-100"
                  size="default"
                  style="width: 100%;"
                />
              </el-form-item>

              <el-form-item label="广告投放">
                <el-select v-model="filters.adInvest" placeholder="选择广告投放等级" size="default" style="width: 100%;">
                  <el-option label="全部" value="" />
                  <el-option label="高强度投放" value="high" />
                  <el-option label="中强度投放" value="mid" />
                  <el-option label="低强度投放" value="low" />
                  <el-option label="无投放" value="none" />
                </el-select>
              </el-form-item>

              <el-form-item label="历史活动效果">
                <el-select v-model="filters.historicalEffect" placeholder="选择历史效果" size="default" style="width: 100%;">
                  <el-option label="全部" value="" />
                  <el-option label="效果优秀" value="excellent" />
                  <el-option label="效果良好" value="good" />
                  <el-option label="效果一般" value="normal" />
                  <el-option label="效果较差" value="poor" />
                </el-select>
              </el-form-item>

              <el-form-item label="网点密度">
                <el-slider
                  v-model="filters.storeDensity"
                  range
                  :min="0"
                  :max="100"
                  :marks="densityMarks"
                />
              </el-form-item>

              <el-form-item label="品牌氛围浓度">
                <el-select v-model="filters.brandAmbition" placeholder="选择品牌氛围" size="default" style="width: 100%;">
                  <el-option label="全部" value="" />
                  <el-option label="氛围浓厚" value="strong" />
                  <el-option label="氛围一般" value="medium" />
                  <el-option label="氛围较弱" value="weak" />
                </el-select>
              </el-form-item>

              <el-button type="primary" style="width: 100%" @click="handleAnalyze">
                开始分析
              </el-button>
            </el-form>
          </div>
        </el-col>

        <!-- 中间：地图 -->
        <el-col :span="12">
          <div class="map-wrapper">
            <GdsMap
              ref="mapRef"
              :center="[104.094, 30.679]"
              :zoom="12"
              @load="handleMapLoad"
            />
            <div class="map-legend">
              <div class="legend-section">
                <span class="legend-title">POI：</span>
                <span class="legend-item"><span class="dot jinpai"></span>劲牌门店</span>
                <span class="legend-item"><span class="dot mall"></span>购物中心</span>
                <span class="legend-item"><span class="dot community"></span>社区</span>
              </div>
              <div class="legend-section" style="margin-top: 4px;">
                <span class="legend-item"><span class="dot highlighted"></span>推荐网格</span>
                <span class="legend-item"><span class="dot unhighlighted"></span>未入选</span>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 右侧：POI统计 + 网格 -->
        <el-col :span="7">
          <!-- POI统计卡 -->
          <el-row :gutter="8" style="margin-bottom: 12px;">
            <el-col :span="8">
              <div class="poi-card jinpai">
                <div class="poi-icon"><span class="dot jinpai"></span></div>
                <div class="poi-info">
                  <div class="poi-count">{{ poiStats.jinpai }}</div>
                  <div class="poi-label">劲牌门店</div>
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="poi-card mall">
                <div class="poi-icon"><span class="dot mall"></span></div>
                <div class="poi-info">
                  <div class="poi-count">{{ poiStats.mall }}</div>
                  <div class="poi-label">购物中心</div>
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="poi-card community">
                <div class="poi-icon"><span class="dot community"></span></div>
                <div class="poi-info">
                  <div class="poi-count">{{ poiStats.community }}</div>
                  <div class="poi-label">社区</div>
                </div>
              </div>
            </el-col>
          </el-row>

          <!-- 网格统计 -->
          <div class="stats-panel">
            <h4>网格统计</h4>
            <div class="stat-row">
              <span class="label">总网格数</span>
              <span class="value">{{ gridStats.total }}</span>
            </div>
            <div class="stat-row">
              <span class="label">推荐网格</span>
              <span class="value recommend">{{ gridStats.recommended }}</span>
            </div>
            <div class="stat-row">
              <span class="label">已选网格</span>
              <span class="value selected">{{ selectedGrids.length }}</span>
            </div>
          </div>

          <!-- 网格清单 -->
          <div class="grid-panel">
            <h4>网格清单 <span class="tip">（点击切换选中状态）</span></h4>
            <div class="grid-list">
              <div
                v-for="grid in gridList"
                :key="grid.code"
                class="grid-item"
                :class="{ selected: selectedGrids.includes(grid.code) }"
                @click="handleSelectGrid(grid)"
              >
                <div class="rank">{{ grid.rank }}</div>
                <div class="info">
                  <div class="name">{{ grid.code }}</div>
                  <div class="street">{{ grid.street }}</div>
                </div>
                <div class="score">{{ grid.score }}</div>
              </div>
            </div>
            <div class="panel-actions">
              <el-button type="success" style="width: 100%;" @click="handleExport" :disabled="selectedGrids.length === 0">
                导出推荐网格 ({{ selectedGrids.length }})
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 下方图表 -->
    <el-row :gutter="20" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>筛选条件分布</template>
          <div ref="distChartRef" style="height: 250px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>网格综合评分分布</template>
          <div ref="scoreChartRef" style="height: 250px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import GdsMap from '@/components/GdsMap.vue'

const mapRef = ref()
const distChartRef = ref()
const scoreChartRef = ref()
let distChart = null
let scoreChart = null
let mapInstance = null
const roadLayerId = 'marketing-grids'

// POI 统计
const poiStats = reactive({ jinpai: 0, mall: 0, community: 0 })

// 筛选条件
const filters = reactive({
  provinces: [],
  cities: [],
  districts: [],
  marketActivity: 50,
  adInvest: '',
  historicalEffect: '',
  storeDensity: [0, 100],
  brandAmbition: ''
})

// 省市联动数据
const cityOptions = {
  sichuan: [
    { label: '成都', value: 'chengdu' },
    { label: '绵阳', value: 'mianyang' },
    { label: '德阳', value: 'deyang' }
  ],
  chongqing: [
    { label: '重庆', value: 'chongqing' }
  ],
  hubei: [
    { label: '武汉', value: 'wuhan' },
    { label: '襄阳', value: 'xiangyang' }
  ],
  shaanxi: [
    { label: '西安', value: 'xian' }
  ]
}

const districtOptions = {
  chengdu: [
    { label: '成华区', value: 'chenghua' },
    { label: '锦江区', value: 'jinjiang' },
    { label: '武侯区', value: 'wuhou' },
    { label: '高新区', value: 'gaoxin' },
    { label: '青羊区', value: 'qingyang' }
  ],
  chongqing: [
    { label: '渝北区', value: 'yubei' },
    { label: '沙坪坝区', value: 'shapingba' }
  ],
  wuhan: [
    { label: '洪山区', value: 'hongshan' },
    { label: '武昌区', value: 'wuchang' }
  ],
  xian: [
    { label: '雁塔区', value: 'yanta' },
    { label: '碑林区', value: 'beilin' }
  ]
}

const availableCities = ref([])
const availableDistricts = ref([])

const densityMarks = { 0: '低', 50: '中', 100: '高' }

// 网格统计
const gridStats = reactive({ total: 0, recommended: 0 })

// 选中网格
const selectedGrids = ref([])

// 网格清单
const gridList = ref([])
const allGrids = ref([])
let gridGeoJSON = null
const gridPOIData = {}

const handleMapLoad = (map) => {
  mapInstance = map
  loadGridData()
}

// 省份变化时更新城市选项
const handleProvinceChange = () => {
  filters.cities = []
  filters.districts = []
  availableCities.value = []
  availableDistricts.value = []

  filters.provinces.forEach(p => {
    if (cityOptions[p]) {
      availableCities.value.push(...cityOptions[p])
    }
  })
  handleFilterChange()
}

// 生成每个网格的 POI 数据
const generateGridPOIData = (feature, index) => {
  const cpt = feature.properties['中心点[内置]'] || '104.06,30.65'
  const [cl, ct] = cpt.split(',').map(Number)
  const spread = 0.015

  const data = { jinpai: [], mall: [], community: [] }

  const jinpaiCount = Math.floor(Math.random() * 3)
  for (let i = 0; i < jinpaiCount; i++) {
    data.jinpai.push({
      coordinates: [cl + (Math.random() - 0.5) * spread, ct + (Math.random() - 0.5) * spread],
      properties: { name: `劲牌门店${index}-${i + 1}` }
    })
  }

  if (Math.random() > 0.5) {
    data.mall.push({
      coordinates: [cl + (Math.random() - 0.5) * spread * 0.5, ct + (Math.random() - 0.5) * spread * 0.5],
      properties: { name: `购物中心${index}` }
    })
  }

  const communityCount = Math.floor(Math.random() * 4) + 2
  for (let i = 0; i < communityCount; i++) {
    data.community.push({
      coordinates: [cl + (Math.random() - 0.5) * spread, ct + (Math.random() - 0.5) * spread],
      properties: { name: `社区${index}-${i + 1}` }
    })
  }

  return data
}

// 计算网格综合评分
const calculateScore = (index) => {
  const brandScore = Math.floor(Math.random() * 40 + 50)
  const storeScore = Math.floor(Math.random() * 50 + 40)
  const marketScore = filters.marketActivity
  const adScore = getAdScore(filters.adInvest)
  const effectScore = getEffectScore(filters.historicalEffect)
  return Math.floor((brandScore + storeScore + marketScore + adScore + effectScore) / 5)
}

const loadGridData = async () => {
  try {
    const response = await fetch('/chenghua-district.geojson')
    const geojson = await response.json()
    gridGeoJSON = geojson

    // 计算每个网格评分
    geojson.features.slice(0, 30).forEach((feature, index) => {
      const score = calculateScore(index)
      feature.properties.totalScore = score
      feature.properties.recommended = score >= 60

      const code = feature.properties['主键'] || `CH-${String(index + 1).padStart(3, '0')}`
      gridPOIData[code] = generateGridPOIData(feature, index)
    })

    // 构建网格列表
    const defPt = '104.06,30.65'
    const [defLng, defLat] = defPt.split(',').map(Number)

    allGrids.value = geojson.features.slice(0, 30).map((feature, index) => {
      const cpt = feature.properties['中心点[内置]'] || `${defLng + index * 0.002},${defLat + index * 0.001}`
      const [lng, lat] = cpt.split(',').map(Number)
      return {
        code: feature.properties['主键'] || `CH-${String(index + 1).padStart(3, '0')}`,
        street: feature.properties['街道[内置]'] || '未知街道',
        score: feature.properties.totalScore || 0,
        recommended: feature.properties.recommended || false,
        coord: [lng, lat]
      }
    })

    allGrids.value.sort((a, b) => b.score - a.score)

    // 省市区分过滤
    applyAreaFilter()
    applyOtherFilters()

    // 默认选中所有推荐网格
    selectedGrids.value = gridList.value.filter(g => g.recommended).map(g => g.code)

    renderGridLayer()
    renderPOILayers()
    updateStats()
    updateCharts()

  } catch (e) {
    console.error('加载网格数据失败:', e)
    ElMessage.error('地图数据加载失败')
  }
}

const applyAreaFilter = () => {
  if (filters.provinces.length === 0 && filters.cities.length === 0 && filters.districts.length === 0) {
    return
  }

  allGrids.value = allGrids.value.filter(grid => {
    if (filters.districts.length > 0) {
      return filters.districts.some(d => grid.street.includes(d))
    }
    if (filters.cities.length > 0) {
      return filters.cities.some(c => grid.code.toLowerCase().includes(c))
    }
    return true
  })
}

const applyOtherFilters = () => {
  gridList.value = allGrids.value.filter(grid => {
    if (!grid.recommended) return false

    if (filters.brandAmbition) {
      const brandMap = { strong: [75, 100], medium: [50, 75], weak: [0, 50] }
      const [min, max] = brandMap[filters.brandAmbition] || [0, 100]
      if (grid.score < min || grid.score > max) return false
    }
    return true
  }).map((grid, index) => {
    grid.rank = index + 1
    return { ...grid }
  })
}

const renderGridLayer = () => {
  if (!mapInstance || !gridGeoJSON) return

  const removeIds = [
    `${roadLayerId}-highlighted-src`, `${roadLayerId}-unhighlighted-src`,
    `${roadLayerId}-hl-fill`, `${roadLayerId}-hl-border`,
    `${roadLayerId}-ul-fill`, `${roadLayerId}-ul-border`
  ]
  removeIds.forEach(id => {
    if (mapInstance.getLayer(id)) mapInstance.removeLayer(id)
    if (mapInstance.getSource(id)) mapInstance.removeSource(id)
  })

  // 推荐网格
  const hlFeatures = gridGeoJSON.features.filter(f => {
    const code = f.properties['主键'] || ''
    return selectedGrids.value.includes(code)
  })

  // 未入选网格
  const ulFeatures = gridGeoJSON.features.filter(f => {
    const code = f.properties['主键'] || ''
    return !selectedGrids.value.includes(code) && allGrids.value.some(g => g.code === code && g.recommended)
  })

  if (hlFeatures.length > 0) {
    mapInstance.addSource(`${roadLayerId}-highlighted-src`, {
      type: 'geojson',
      data: { type: 'FeatureCollection', features: hlFeatures }
    })
    mapInstance.addLayer({
      id: `${roadLayerId}-hl-fill`,
      type: 'fill',
      source: `${roadLayerId}-highlighted-src`,
      paint: { 'fill-color': '#9933ff', 'fill-opacity': 0.35 }
    })
    mapInstance.addLayer({
      id: `${roadLayerId}-hl-border`,
      type: 'line',
      source: `${roadLayerId}-highlighted-src`,
      paint: { 'line-color': '#9933ff', 'line-width': 2, 'line-opacity': 1 }
    })
  }

  if (ulFeatures.length > 0) {
    mapInstance.addSource(`${roadLayerId}-unhighlighted-src`, {
      type: 'geojson',
      data: { type: 'FeatureCollection', features: ulFeatures }
    })
    mapInstance.addLayer({
      id: `${roadLayerId}-ul-fill`,
      type: 'fill',
      source: `${roadLayerId}-unhighlighted-src`,
      paint: { 'fill-color': '#909399', 'fill-opacity': 0.15 }
    })
    mapInstance.addLayer({
      id: `${roadLayerId}-ul-border`,
      type: 'line',
      source: `${roadLayerId}-unhighlighted-src`,
      paint: { 'line-color': '#c0c4cc', 'line-width': 1, 'line-opacity': 0.5 }
    })
  }
}

const renderPOILayers = () => {
  ;['poi-jinpai', 'poi-mall', 'poi-community'].forEach(id => {
    mapRef.value?.removeLayer(id)
  })

  let jinpaiCount = 0, mallCount = 0, communityCount = 0
  const jinpaiData = [], mallData = [], communityData = []

  // 显示所有推荐网格内的 POI
  allGrids.value.filter(g => g.recommended).forEach(grid => {
    const data = gridPOIData[grid.code]
    if (!data) return
    jinpaiCount += data.jinpai.length
    mallCount += data.mall.length
    communityCount += data.community.length
    jinpaiData.push(...data.jinpai)
    mallData.push(...data.mall)
    communityData.push(...data.community)
  })

  poiStats.jinpai = jinpaiCount
  poiStats.mall = mallCount
  poiStats.community = communityCount

  if (jinpaiData.length > 0) {
    mapRef.value?.addScatterLayer('poi-jinpai', jinpaiData, { radius: 8, color: '#f56c6c', opacity: 0.9 })
  }
  if (mallData.length > 0) {
    mapRef.value?.addScatterLayer('poi-mall', mallData, { radius: 10, color: '#e6a23c', opacity: 0.85 })
  }
  if (communityData.length > 0) {
    mapRef.value?.addScatterLayer('poi-community', communityData, { radius: 5, color: '#67c23a', opacity: 0.7 })
  }
}

const getAdScore = (level) => {
  const map = { high: 90, mid: 70, low: 50, none: 30, '': 60 }
  return map[level] || 60
}

const getEffectScore = (level) => {
  const map = { excellent: 95, good: 75, normal: 55, poor: 35, '': 65 }
  return map[level] || 65
}

const handleFilterChange = () => {
  applyOtherFilters()
  if (gridGeoJSON) renderGridLayer()
  renderPOILayers()
  updateStats()
}

const updateStats = () => {
  gridStats.total = allGrids.value.length
  gridStats.recommended = allGrids.value.filter(g => g.recommended).length
}

const handleAnalyze = () => {
  if (!mapInstance) return
  selectedGrids.value = []
  loadGridData()
  ElMessage.success('分析完成')
}

const handleSelectGrid = (grid) => {
  const idx = selectedGrids.value.indexOf(grid.code)
  if (idx > -1) {
    selectedGrids.value.splice(idx, 1)
  } else {
    selectedGrids.value.push(grid.code)
  }

  if (gridGeoJSON) renderGridLayer()
  updateStats()
}

const handleExport = () => {
  const selected = gridList.value.filter(g => selectedGrids.value.includes(g.code))
  ElMessage.success(`已导出 ${selected.length} 个推荐网格`)
}

// 图表
const updateCharts = () => {
  nextTick(() => {
    initDistChart()
    initScoreChart()
  })
}

const initDistChart = () => {
  if (!distChartRef.value) return
  if (!distChart) distChart = echarts.init(distChartRef.value)

  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: ['品牌氛围', '网点密度', '市场动销', '广告投放', '历史效果'],
      axisLabel: { fontSize: 11, rotate: 15 }
    },
    yAxis: { type: 'value', name: '平均得分', nameLocation: 'middle', nameGap: 35, axisLabel: { fontSize: 11 } },
    series: [{
      data: [
        filters.brandAmbition ? getEffectScore(filters.brandAmbition) * 0.8 : 72,
        (filters.storeDensity[0] + filters.storeDensity[1]) / 2,
        filters.marketActivity,
        getAdScore(filters.adInvest),
        getEffectScore(filters.historicalEffect)
      ],
      type: 'bar',
      itemStyle: {
        color: (params) => {
          const colors = ['#f56c6c', '#409eff', '#e6a23c', '#67c23a', '#9933ff']
          return colors[params.dataIndex]
        }
      },
      label: { show: true, position: 'top', formatter: '{c}', fontSize: 10 }
    }]
  }
  distChart.setOption(option)
}

const initScoreChart = () => {
  if (!scoreChartRef.value) return
  if (!scoreChart) scoreChart = echarts.init(scoreChartRef.value)

  const bins = [
    { range: '90-100', count: 0 },
    { range: '80-89', count: 0 },
    { range: '70-79', count: 0 },
    { range: '60-69', count: 0 },
    { range: '50-59', count: 0 },
    { range: '<50', count: 0 }
  ]

  gridList.value.forEach(grid => {
    const s = grid.score
    if (s >= 90) bins[0].count++
    else if (s >= 80) bins[1].count++
    else if (s >= 70) bins[2].count++
    else if (s >= 60) bins[3].count++
    else if (s >= 50) bins[4].count++
    else bins[5].count++
  })

  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: bins.map(b => b.range), name: '综合得分区间', nameLocation: 'middle', nameGap: 30, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', name: '网格数量', nameLocation: 'middle', nameGap: 35, axisLabel: { fontSize: 10 } },
    series: [{
      data: bins.map(b => b.count),
      type: 'bar',
      itemStyle: {
        color: (params) => {
          const colors = ['#67c23a', '#85ce61', '#409eff', '#e6a23c', '#f56c6c', '#909399']
          return colors[params.dataIndex]
        }
      },
      label: { show: true, position: 'top', fontSize: 10 }
    }]
  }
  scoreChart.setOption(option)
}

const handleResize = () => {
  distChart?.resize()
  scoreChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  distChart?.dispose()
  scoreChart?.dispose()
})
</script>

<style scoped>
.marketing-insight-page { padding: 20px; }

.filter-panel {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  height: 100%;
}

.filter-panel h4 {
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e6e6e6;
}

:deep(.el-form-item) { margin-bottom: 12px; }
:deep(.el-form-item__label) { font-size: 13px; font-weight: 500; }

.map-wrapper {
  position: relative;
  height: 100%;
  border-radius: 4px;
  overflow: hidden;
}

.map-legend {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.95);
  padding: 10px 14px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1;
  font-size: 12px;
}

.legend-section {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.legend-title { font-weight: bold; color: #303133; }
.legend-item { display: flex; align-items: center; gap: 4px; color: #606266; }

.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot.jinpai { background: #f56c6c; }
.dot.mall { background: #e6a23c; }
.dot.community { background: #67c23a; }
.dot.highlighted { background: #9933ff; }
.dot.unhighlighted { background: #909399; }

.poi-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e6e6e6;
  transition: all 0.2s;
}

.poi-icon .dot { width: 14px; height: 14px; }

.poi-info { flex: 1; }

.poi-count {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.poi-label { font-size: 11px; color: #909399; margin-top: 2px; }

.stats-panel {
  padding: 14px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 12px;
}

.stats-panel h4 { margin-bottom: 10px; }

.stat-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid #e6e6e6;
  font-size: 13px;
}

.stat-row:last-child { border-bottom: none; }
.stat-row .label { color: #909399; }
.stat-row .value { font-weight: bold; }
.stat-row .value.recommend { color: #9933ff; }
.stat-row .value.selected { color: #409eff; }

.grid-panel {
  padding: 14px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  height: calc(100% - 168px);
  display: flex;
  flex-direction: column;
}

.grid-panel h4 { margin-bottom: 4px; }
.grid-panel .tip { font-size: 11px; color: #909399; font-weight: normal; }

.grid-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.grid-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.grid-item:hover { background: #f0f0ff; }

.grid-item.selected {
  border-color: #9933ff;
  background: #f5f0ff;
}

.grid-item .rank {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #409eff;
  color: #fff;
  border-radius: 50%;
  font-size: 10px;
  font-weight: bold;
  flex-shrink: 0;
}

.grid-item .info { flex: 1; min-width: 0; }

.grid-item .name {
  font-weight: bold;
  font-size: 11px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.grid-item .street {
  font-size: 10px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.grid-item .score {
  font-size: 12px;
  font-weight: bold;
  color: #409eff;
  flex-shrink: 0;
}

.panel-actions { margin-top: 10px; }
</style>
