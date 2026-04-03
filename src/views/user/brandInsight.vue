<template>
  <div class="brand-insight-page">
    <el-card shadow="hover">
      <template #header>
        <span>品牌洞察 - 品牌影响力热力图</span>
        <div class="header-actions">
          <el-select v-model="filters.province" placeholder="选择省份" size="default" clearable @change="handleAnalyze">
            <el-option label="四川省" value="sichuan" />
            <el-option label="重庆市" value="chongqing" />
            <el-option label="湖北省" value="hubei" />
          </el-select>
          <el-select v-model="filters.city" placeholder="选择城市" size="default" clearable @change="handleAnalyze">
            <el-option label="成都" value="chengdu" />
            <el-option label="重庆" value="chongqing" />
            <el-option label="武汉" value="wuhan" />
          </el-select>
          <el-button type="primary" @click="handleAnalyze">刷新数据</el-button>
        </div>
      </template>

      <el-row :gutter="20" style="height: 500px;">
        <!-- 左侧：筛选 + 品牌列表 -->
        <el-col :span="5">
          <div class="filter-panel">
            <h4>品牌选择</h4>
            <div class="brand-list">
              <div
                v-for="brand in brandList"
                :key="brand.id"
                class="brand-item"
                :class="{ active: selectedBrand === brand.id }"
                @click="handleSelectBrand(brand)"
              >
                <div class="brand-color" :style="{ background: brand.color }"></div>
                <div class="brand-info">
                  <div class="brand-name">{{ brand.name }}</div>
                  <div class="brand-score">热度 {{ brand.heatScore }}</div>
                </div>
                <el-tag :type="brand.trend > 0 ? 'success' : 'danger'" size="small">
                  {{ brand.trend > 0 ? '↑' : '↓' }}{{ Math.abs(brand.trend) }}%
                </el-tag>
              </div>
            </div>
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
            <div class="layer-control">
              <el-checkbox v-model="layerOptions.activity" label="品牌活跃度" @change="handleLayerToggle" />
              <el-checkbox v-model="layerOptions.social" label="社媒搜索指数" @change="handleLayerToggle" />
              <el-checkbox v-model="layerOptions.crowd" label="A1-A3人群占比" @change="handleLayerToggle" />
              <el-checkbox v-model="layerOptions.preference" label="好感度与偏好度" @change="handleLayerToggle" />
              <el-divider style="margin: 6px 0;" />
              <el-checkbox v-model="layerOptions.poi" label="品牌网点" @change="handleLayerToggle" />
            </div>
            <div class="map-legend">
              <span>品牌热度：</span>
              <span class="legend-item"><span class="heat-bar high"></span>高</span>
              <span class="legend-item"><span class="heat-bar mid"></span>中</span>
              <span class="legend-item"><span class="heat-bar low"></span>低</span>
            </div>
          </div>
        </el-col>

        <!-- 右侧：品牌指标详情 -->
        <el-col :span="7">
          <!-- 品牌综合指数 -->
          <div class="brand-score-card" v-if="currentBrand">
            <div class="brand-header">
              <div class="brand-color-dot" :style="{ background: currentBrand.color }"></div>
              <span class="brand-title">{{ currentBrand.name }}</span>
              <el-tag :type="currentBrand.trend > 0 ? 'success' : 'danger'" size="small">
                {{ currentBrand.trend > 0 ? '↑' : '↓' }}{{ Math.abs(currentBrand.trend) }}%
              </el-tag>
            </div>
            <div class="overall-score">
              <span class="score-label">综合热度指数</span>
              <span class="score-value">{{ currentBrand.heatScore }}</span>
            </div>
          </div>

          <!-- 四维指标卡片 -->
          <div class="metrics-grid">
            <div class="metric-card">
              <div class="metric-header">
                <span class="metric-icon">🔥</span>
                <span class="metric-name">品牌活跃度</span>
              </div>
              <div class="metric-value">{{ currentBrand?.activity || 0 }}</div>
              <div class="metric-bar">
                <div class="bar-fill" :style="{ width: (currentBrand?.activity || 0) + '%', background: '#f56c6c' }"></div>
              </div>
            </div>

            <div class="metric-card">
              <div class="metric-header">
                <span class="metric-icon">🔍</span>
                <span class="metric-name">社媒搜索指数</span>
              </div>
              <div class="metric-value">{{ currentBrand?.socialIndex || 0 }}</div>
              <div class="metric-bar">
                <div class="bar-fill" :style="{ width: (currentBrand?.socialIndex || 0) + '%', background: '#409eff' }"></div>
              </div>
            </div>

            <div class="metric-card">
              <div class="metric-header">
                <span class="metric-icon">👥</span>
                <span class="metric-name">A1-A3人群占比</span>
              </div>
              <div class="metric-value">{{ currentBrand?.crowdRatio || 0 }}%</div>
              <div class="metric-bar">
                <div class="bar-fill" :style="{ width: (currentBrand?.crowdRatio || 0) + '%', background: '#67c23a' }"></div>
              </div>
            </div>

            <div class="metric-card">
              <div class="metric-header">
                <span class="metric-icon">❤️</span>
                <span class="metric-name">好感度与偏好度</span>
              </div>
              <div class="metric-value">{{ currentBrand?.preference || 0 }}</div>
              <div class="metric-bar">
                <div class="bar-fill" :style="{ width: (currentBrand?.preference || 0) + '%', background: '#e6a23c' }"></div>
              </div>
            </div>
          </div>

          <!-- A1-A3人群分布 -->
          <div class="crowd-dist-card">
            <h4>A1-A3人群流转</h4>
            <div class="crowd-flow">
              <div class="crowd-item">
                <div class="crowd-circle a1">
                  <span class="crowd-label">A1</span>
                  <span class="crowd-value">{{ crowdData.a1 }}%</span>
                </div>
                <div class="crowd-arrow">→</div>
                <div class="crowd-circle a2">
                  <span class="crowd-label">A2</span>
                  <span class="crowd-value">{{ crowdData.a2 }}%</span>
                </div>
                <div class="crowd-arrow">→</div>
                <div class="crowd-circle a3">
                  <span class="crowd-label">A3</span>
                  <span class="crowd-value">{{ crowdData.a3 }}%</span>
                </div>
              </div>
            </div>
            <div class="crowd-desc">
              <p><span class="dot a1-dot"></span><strong>A1(认知)</strong>：知道该品牌的用户</p>
              <p><span class="dot a2-dot"></span><strong>A2(兴趣)</strong>：对品牌产生兴趣的用户</p>
              <p><span class="dot a3-dot"></span><strong>A3(询问)</strong>：主动搜索/询问品牌的用户</p>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 下方图表 -->
    <el-row :gutter="20" style="margin-top: 16px;">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>品牌热度对比</template>
          <div ref="compareChartRef" style="height: 250px;"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>各区品牌热度分布</template>
          <div ref="districtChartRef" style="height: 250px;"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>品牌人群转化漏斗</template>
          <div ref="funnelChartRef" style="height: 250px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import GdsMap from '@/components/GdsMap.vue'

const mapRef = ref()
const compareChartRef = ref()
const districtChartRef = ref()
const funnelChartRef = ref()
let compareChart = null
let districtChart = null
let funnelChart = null
let mapInstance = null

// 筛选条件
const filters = reactive({
  province: '',
  city: ''
})

// 图层开关
const layerOptions = reactive({
  activity: true,
  social: true,
  crowd: true,
  preference: true,
  poi: true
})

// 品牌列表
const brandList = ref([
  { id: 'jinpai', name: '劲牌', color: '#f56c6c', heatScore: 92, trend: 5, activity: 88, socialIndex: 85, crowdRatio: 42, preference: 78 },
  { id: 'maotai', name: '茅台', color: '#e6a23c', heatScore: 95, trend: 3, activity: 92, socialIndex: 96, crowdRatio: 55, preference: 88 },
  { id: 'wuliangye', name: '五粮液', color: '#67c23a', heatScore: 88, trend: -2, activity: 85, socialIndex: 82, crowdRatio: 48, preference: 75 },
  { id: 'yanghe', name: '洋河', color: '#409eff', heatScore: 76, trend: 8, activity: 72, socialIndex: 78, crowdRatio: 35, preference: 65 },
  { id: 'luzhoulaojiao', name: '泸州老窖', color: '#9933ff', heatScore: 80, trend: 1, activity: 78, socialIndex: 75, crowdRatio: 40, preference: 70 },
  { id: 'tuopai', name: '沱牌', color: '#00b894', heatScore: 62, trend: -5, activity: 58, socialIndex: 60, crowdRatio: 28, preference: 55 }
])

// 当前选中品牌
const selectedBrand = ref('jinpai')

const currentBrand = computed(() => {
  return brandList.value.find(b => b.id === selectedBrand.value) || brandList.value[0]
})

// A1-A3人群数据
const crowdData = reactive({ a1: 42, a2: 28, a3: 15 })

// 区域热度数据
const districtData = ref([
  { name: '成华区', value: 85 },
  { name: '锦江区', value: 92 },
  { name: '武侯区', value: 88 },
  { name: '高新区', value: 78 },
  { name: '青羊区', value: 72 },
  { name: '金牛区', value: 65 },
  { name: '双流区', value: 58 }
])

const handleMapLoad = (map) => {
  mapInstance = map
  loadBrandHeatmap()
}

const handleSelectBrand = (brand) => {
  selectedBrand.value = brand.id
  loadBrandHeatmap()
  updateMetrics()
  updateCharts()
  ElMessage.success(`已切换至 ${brand.name} 品牌分析`)
}

const handleLayerToggle = () => {
  loadBrandHeatmap()
}

const handleAnalyze = () => {
  // 重新生成数据
  brandList.value.forEach(brand => {
    brand.heatScore = Math.floor(Math.random() * 20 + 70)
    brand.activity = Math.floor(Math.random() * 20 + 70)
    brand.socialIndex = Math.floor(Math.random() * 20 + 70)
    brand.crowdRatio = Math.floor(Math.random() * 20 + 30)
    brand.preference = Math.floor(Math.random() * 20 + 60)
    brand.trend = Math.floor(Math.random() * 20) - 10
  })
  loadBrandHeatmap()
  updateMetrics()
  updateCharts()
  ElMessage.success('数据已刷新')
}

// 生成品牌热力图数据
const generateHeatData = (type, intensity) => {
  const data = []
  const baseLng = 104.06
  const baseLat = 30.65
  const count = type === 'poi' ? 15 : 80

  for (let i = 0; i < count; i++) {
    data.push({
      coordinates: [
        baseLng + (Math.random() - 0.5) * 0.18,
        baseLat + (Math.random() - 0.5) * 0.14
      ],
      intensity: Math.random() * intensity
    })
  }
  return data
}

const loadBrandHeatmap = () => {
  if (!mapInstance) return

  const layers = [
    { id: 'brand-activity', show: layerOptions.activity, color: '#f56c6c' },
    { id: 'brand-social', show: layerOptions.social, color: '#409eff' },
    { id: 'brand-crowd', show: layerOptions.crowd, color: '#67c23a' },
    { id: 'brand-preference', show: layerOptions.preference, color: '#e6a23c' }
  ]

  layers.forEach(({ id, show, color }) => {
    mapRef.value?.removeLayer(id)
    if (show) {
      const data = generateHeatData(id, 0.9)
      mapRef.value?.addHeatLayer(id, data, {
        radius: 35,
        intensity: 1.2,
        opacity: 0.65
      })
    }
  })

  // 品牌网点
  mapRef.value?.removeLayer('brand-poi')
  if (layerOptions.poi) {
    const poiData = []
    const baseLng = 104.06
    const baseLat = 30.65
    for (let i = 0; i < 20; i++) {
      poiData.push({
        coordinates: [
          baseLng + (Math.random() - 0.5) * 0.15,
          baseLat + (Math.random() - 0.5) * 0.12
        ],
        properties: { name: `劲牌网点${i + 1}` }
      })
    }
    mapRef.value?.addScatterLayer('brand-poi', poiData, {
      radius: 8,
      color: currentBrand.value.color || '#f56c6c',
      opacity: 0.9
    })
  }
}

const updateMetrics = () => {
  const brand = currentBrand.value
  crowdData.a1 = Math.floor(brand.crowdRatio * 0.6)
  crowdData.a2 = Math.floor(brand.crowdRatio * 0.3)
  crowdData.a3 = Math.floor(brand.crowdRatio * 0.1)
}

// 图表
const updateCharts = () => {
  nextTick(() => {
    initCompareChart()
    initDistrictChart()
    initFunnelChart()
  })
}

const initCompareChart = () => {
  if (!compareChartRef.value) return
  if (!compareChart) compareChart = echarts.init(compareChartRef.value)

  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    legend: { data: ['品牌活跃度', '社媒指数', 'A1-A3占比', '偏好度'], top: 0, textStyle: { fontSize: 10 } },
    xAxis: {
      type: 'category',
      data: brandList.value.map(b => b.name),
      axisLabel: { fontSize: 10, rotate: 15 }
    },
    yAxis: { type: 'value', max: 100, nameLocation: 'middle', nameGap: 35, axisLabel: { fontSize: 10 } },
    series: [
      { name: '品牌活跃度', data: brandList.value.map(b => b.activity), type: 'bar', itemStyle: { color: '#f56c6c' }, label: { show: true, position: 'top', fontSize: 9 } },
      { name: '社媒指数', data: brandList.value.map(b => b.socialIndex), type: 'bar', itemStyle: { color: '#409eff' }, label: { show: true, position: 'top', fontSize: 9 } },
      { name: 'A1-A3占比', data: brandList.value.map(b => b.crowdRatio), type: 'bar', itemStyle: { color: '#67c23a' }, label: { show: true, position: 'top', fontSize: 9 } },
      { name: '偏好度', data: brandList.value.map(b => b.preference), type: 'bar', itemStyle: { color: '#e6a23c' }, label: { show: true, position: 'top', fontSize: 9 } }
    ]
  }
  compareChart.setOption(option)
}

const initDistrictChart = () => {
  if (!districtChartRef.value) return
  if (!districtChart) districtChart = echarts.init(districtChartRef.value)

  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'value', max: 100, name: '热度指数', nameLocation: 'middle', nameGap: 30, axisLabel: { fontSize: 10 } },
    yAxis: {
      type: 'category',
      data: districtData.value.map(d => d.name).reverse(),
      nameLocation: 'middle', nameGap: 45, axisLabel: { fontSize: 10 }
    },
    series: [{
      data: districtData.value.map(d => d.value).reverse(),
      type: 'bar',
      itemStyle: {
        color: (params) => {
          const val = params.value
          if (val >= 85) return '#f56c6c'
          if (val >= 70) return '#e6a23c'
          return '#67c23a'
        }
      },
      label: { show: true, position: 'right', formatter: '{c}', fontSize: 10 }
    }]
  }
  districtChart.setOption(option)
}

const initFunnelChart = () => {
  if (!funnelChartRef.value) return
  if (!funnelChart) funnelChart = echarts.init(funnelChartRef.value)

  const brand = currentBrand.value
  const total = 100
  const a1 = brand.crowdRatio * 0.6
  const a2 = brand.crowdRatio * 0.3
  const a3 = brand.crowdRatio * 0.1

  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    grid: { left: 40, right: 40, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: ['总曝光人群(A1)', '兴趣人群(A2)', '主动询问(A3)', '购买转化'],
      axisLabel: { fontSize: 10, rotate: 10 }
    },
    yAxis: { type: 'value', max: 100, name: '占比(%)', nameLocation: 'middle', nameGap: 35, axisLabel: { fontSize: 10 } },
    series: [{
      data: [total, Math.round(a1 / total * 100), Math.round(a2 / total * 100), Math.round(a3 / total * 100)],
      type: 'bar',
      itemStyle: {
        color: (params) => {
          const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c']
          return colors[params.dataIndex]
        }
      },
      label: { show: true, position: 'top', formatter: '{c}%', fontSize: 10 }
    }]
  }
  funnelChart.setOption(option)
}

const handleResize = () => {
  compareChart?.resize()
  districtChart?.resize()
  funnelChart?.resize()
}

onMounted(() => {
  updateMetrics()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  compareChart?.dispose()
  districtChart?.dispose()
  funnelChart?.dispose()
})
</script>

<style scoped>
.brand-insight-page { padding: 20px; }

.el-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.filter-panel {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  height: 100%;
}

.filter-panel h4 {
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e6e6e6;
}

.brand-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 420px;
  overflow-y: auto;
}

.brand-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.brand-item:hover {
  background: #f5f5f5;
}

.brand-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.brand-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.brand-info {
  flex: 1;
  min-width: 0;
}

.brand-name {
  font-weight: bold;
  font-size: 13px;
  color: #303133;
}

.brand-score {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.map-wrapper {
  position: relative;
  height: 100%;
  border-radius: 4px;
  overflow: hidden;
}

.layer-control {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.map-legend {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.95);
  padding: 8px 14px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.heat-bar {
  display: inline-block;
  width: 20px;
  height: 10px;
  border-radius: 2px;
}

.heat-bar.high { background: #f56c6c; }
.heat-bar.mid { background: #e6a23c; }
.heat-bar.low { background: #67c23a; }

/* 品牌综合指数卡片 */
.brand-score-card {
  padding: 14px 16px;
  background: linear-gradient(135deg, #f56c6c, #9933ff);
  border-radius: 8px;
  color: #fff;
  margin-bottom: 12px;
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.brand-color-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #fff;
}

.brand-title {
  font-weight: bold;
  font-size: 16px;
  flex: 1;
}

.overall-score {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.score-label {
  font-size: 12px;
  opacity: 0.9;
}

.score-value {
  font-size: 28px;
  font-weight: bold;
}

/* 四维指标卡片 */
.metrics-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}

.metric-card {
  padding: 12px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e6e6e6;
}

.metric-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.metric-icon {
  font-size: 14px;
}

.metric-name {
  font-size: 11px;
  color: #606266;
  font-weight: 500;
}

.metric-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 6px;
}

.metric-bar {
  height: 4px;
  background: #f5f7fa;
  border-radius: 2px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.5s;
}

/* A1-A3人群分布 */
.crowd-dist-card {
  padding: 14px 16px;
  background: #f5f7fa;
  border-radius: 6px;
}

.crowd-dist-card h4 {
  margin-bottom: 12px;
  font-size: 13px;
}

.crowd-flow {
  margin-bottom: 12px;
}

.crowd-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.crowd-circle {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: 2px solid;
}

.crowd-circle.a1 { background: rgba(64, 158, 255, 0.15); border-color: #409eff; }
.crowd-circle.a2 { background: rgba(103, 194, 58, 0.15); border-color: #67c23a; }
.crowd-circle.a3 { background: rgba(230, 162, 60, 0.15); border-color: #e6a23c; }

.crowd-label {
  font-size: 10px;
  font-weight: bold;
  color: #606266;
}

.crowd-value {
  font-size: 12px;
  font-weight: bold;
  color: #303133;
}

.crowd-arrow {
  font-size: 16px;
  color: #909399;
}

.crowd-desc p {
  margin: 4px 0;
  font-size: 11px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.a1-dot { background: #409eff; }
.a2-dot { background: #67c23a; }
.a3-dot { background: #e6a23c; }
</style>
