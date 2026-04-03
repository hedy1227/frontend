<template>
  <div class="weak-area-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>薄弱区域洞察 - 红绿灯预警地图</span>
          <div class="header-actions">
            <el-select v-model="filters.level" placeholder="预警级别" size="default" @change="loadData">
              <el-option label="全部" value="" />
              <el-option label="红色预警" value="red" />
              <el-option label="黄色预警" value="yellow" />
              <el-option label="绿色健康" value="green" />
            </el-select>
            <el-select v-model="filters.type" placeholder="异常类型" size="default" @change="loadData">
              <el-option label="全部类型" value="" />
              <el-option label="动销断崖" value="sales" />
              <el-option label="拜访频次下降" value="visit" />
              <el-option label="陈列异常" value="display" />
            </el-select>
            <el-button type="primary" @click="loadData">刷新</el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="20" style="height: 520px;">
        <el-col :span="8">
          <div class="alert-summary">
            <h4>预警统计</h4>
            <div class="summary-cards">
              <div class="summary-item red">
                <div class="count">{{ alertStats.red }}</div>
                <div class="label">红色预警</div>
              </div>
              <div class="summary-item yellow">
                <div class="count">{{ alertStats.yellow }}</div>
                <div class="label">黄色预警</div>
              </div>
              <div class="summary-item green">
                <div class="count">{{ alertStats.green }}</div>
                <div class="label">绿色健康</div>
              </div>
            </div>
          </div>

          <div class="alert-list">
            <h4>预警列表</h4>
            <div class="alert-scroll">
              <div
                v-for="alert in filteredAlerts"
                :key="alert.id"
                class="alert-item"
                :class="alert.level"
                @click="handleSelectAlert(alert)"
              >
                <div class="alert-header">
                  <el-tag :type="getTagType(alert.level)" size="small">
                    {{ getLevelText(alert.level) }}
                  </el-tag>
                  <span class="time">{{ alert.time }}</span>
                </div>
                <div class="alert-content">
                  <div class="location">{{ alert.location }}</div>
                  <div class="desc">{{ alert.description }}</div>
                </div>
              </div>
            </div>
          </div>
        </el-col>

        <el-col :span="16">
          <div class="map-wrapper">
            <GdsMap
              ref="mapRef"
              :center="[104.094, 30.679]"
              :zoom="11"
              @load="handleMapLoad"
              @click="handleMapClick"
            />
            <div class="traffic-light-legend">
              <span class="legend-item"><span class="light red"></span>红色预警</span>
              <span class="legend-item"><span class="light yellow"></span>黄色预警</span>
              <span class="legend-item"><span class="light green"></span>绿色健康</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>预警趋势分析</template>
          <div ref="trendChartRef" style="height: 250px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>归因分析 - {{ selectedAlert?.location || '请选择预警点' }}</template>
          <div class="attribution-panel" v-if="selectedAlert">
            <div class="tree-container">
              <div class="tree-node root negative">
                <div class="node-icon">!</div>
                <div class="node-content">
                  <div class="node-title">{{ selectedAlert.description }}</div>
                  <div class="node-value">{{ selectedAlert.metrics?.[0]?.value }}</div>
                </div>
              </div>
              <div class="tree-arrow">↓</div>
              <div class="tree-node level1">
                <div class="node-icon">→</div>
                <div class="node-content">
                  <div class="node-title">{{ selectedAlert.causeLevel1 }}</div>
                </div>
              </div>
              <div class="tree-arrow">↓</div>
              <div class="tree-node level2">
                <div class="node-icon">→</div>
                <div class="node-content">
                  <div class="node-title">{{ selectedAlert.causeLevel2 }}</div>
                </div>
              </div>
            </div>
            <div class="action-buttons">
              <el-button type="primary" @click="handleDispatch">派发整改工单</el-button>
              <el-button @click="handleMarkProcessed">标记已处理</el-button>
            </div>
          </div>
          <el-empty v-else description="点击左侧预警列表查看归因分析" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import GdsMap from '@/components/GdsMap.vue'

const mapRef = ref()
const trendChartRef = ref()
let mapInstance = null
let trendChart = null

const filters = reactive({
  level: '',
  type: ''
})

const alertStats = reactive({
  red: 12,
  yellow: 28,
  green: 156
})

const selectedAlert = ref(null)

const alerts = ref([
  { id: 1, level: 'red', time: '10:32', location: '成都市高新区-天府大道', type: 'sales', description: '动销断崖下降', metrics: [{ label: '动销', value: '-30%', type: 'negative' }], causeLevel1: '核心门店缺货率上升15%', causeLevel2: '业务员上周拜访频次下降40%', coord: [104.063, 30.548] },
  { id: 2, level: 'red', time: '09:45', location: '重庆市渝北区-观音桥', type: 'display', description: '陈列不合格率上升', metrics: [{ label: '陈列合格率', value: '-25%', type: 'negative' }], causeLevel1: '货架贴完好率下降', causeLevel2: '巡检频次减少', coord: [106.532, 29.617] },
  { id: 3, level: 'yellow', time: '09:12', location: '武汉市洪山区-光谷', type: 'visit', description: '补货频次波动', metrics: [{ label: '补货频次', value: '-12%', type: 'negative' }], causeLevel1: '新品动销放缓', causeLevel2: '待进一步观察', coord: [114.406, 30.514] },
  { id: 4, level: 'yellow', time: '08:55', location: '西安市雁塔区-小寨', type: 'sales', description: '销量环比下降', metrics: [{ label: '环比', value: '-8%', type: 'negative' }], causeLevel1: '竞品促销活动影响', causeLevel2: '待观察一周', coord: [108.948, 34.218] },
  { id: 5, level: 'green', time: '08:30', location: '长沙市岳麓区-大学城', type: 'sales', description: '市场健康', metrics: [], causeLevel1: '各项指标正常', causeLevel2: '-', coord: [112.944, 28.189] }
])

const filteredAlerts = computed(() => {
  return alerts.value.filter(alert => {
    if (filters.level && alert.level !== filters.level) return false
    if (filters.type && alert.type !== filters.type) return false
    return true
  })
})

const getTagType = (level) => {
  const map = { red: 'danger', yellow: 'warning', green: 'success' }
  return map[level] || 'info'
}

const getLevelText = (level) => {
  const map = { red: '红色预警', yellow: '黄色预警', green: '绿色健康' }
  return map[level] || level
}

const handleMapLoad = async (map) => {
  mapInstance = map
  await loadDistrictData()
  loadData()
}

// 加载成华区四级路网网格（按预警等级着色）
const loadDistrictData = async () => {
  try {
    const response = await fetch('/chenghua-district.geojson')
    const geojson = await response.json()

    // 为每个片区随机分配预警等级（模拟）
    const levels = ['red', 'yellow', 'green']
    const weights = [0.15, 0.25, 0.6] // 红色15%、黄色25%、绿色60%

    geojson.features.forEach(feature => {
      const rand = Math.random()
      if (rand < weights[0]) {
        feature.properties.alertLevel = 'red'
      } else if (rand < weights[0] + weights[1]) {
        feature.properties.alertLevel = 'yellow'
      } else {
        feature.properties.alertLevel = 'green'
      }
    })

    // 使用预警等级色阶渲染
    mapRef.value?.addRoadLayer('district-alert', geojson, {
      alertLevelField: 'alertLevel'
    })
  } catch (e) {
    console.error('加载片区数据失败:', e)
  }
}

const loadData = () => {
  if (!mapInstance) return

  // 按等级分组添加散点
  const redAlerts = alerts.value.filter(a => a.level === 'red')
  const yellowAlerts = alerts.value.filter(a => a.level === 'yellow')
  const greenAlerts = alerts.value.filter(a => a.level === 'green')

  // 清理旧图层
  mapRef.value?.removeLayer('redAlert')
  mapRef.value?.removeLayer('yellowAlert')
  mapRef.value?.removeLayer('greenAlert')

  // 红色预警点
  if (redAlerts.length > 0) {
    const redData = redAlerts.map(a => ({
      coordinates: a.coord,
      properties: { name: a.location, level: a.level }
    }))
    mapRef.value?.addScatterLayer('redAlert', redData, {
      radius: 15,
      color: '#f56c6c'
    })
  }

  // 黄色预警点
  if (yellowAlerts.length > 0) {
    const yellowData = yellowAlerts.map(a => ({
      coordinates: a.coord,
      properties: { name: a.location, level: a.level }
    }))
    mapRef.value?.addScatterLayer('yellowAlert', yellowData, {
      radius: 12,
      color: '#e6a23c'
    })
  }

  // 绿色健康点
  if (greenAlerts.length > 0) {
    const greenData = greenAlerts.map(a => ({
      coordinates: a.coord,
      properties: { name: a.location, level: a.level }
    }))
    mapRef.value?.addScatterLayer('greenAlert', greenData, {
      radius: 8,
      color: '#67c23a'
    })
  }

  ElMessage.success('数据已刷新')
}

const handleMapClick = (e) => {
  console.log('Map clicked:', e)
}

const handleSelectAlert = (alert) => {
  selectedAlert.value = alert
  // 飞转到该位置
  mapRef.value?.flyTo({
    center: alert.coord,
    zoom: 14,
    duration: 1000
  })
}

const handleDispatch = () => {
  ElMessage.success('整改工单已派发')
}

const handleMarkProcessed = () => {
  ElMessage.success('已标记为已处理')
}

const initTrendChart = () => {
  if (!trendChartRef.value) return

  trendChart = echarts.init(trendChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['红色预警', '黄色预警', '绿色健康'],
      top: 0,
      textStyle: { fontSize: 11 }
    },
    grid: {
      left: 50,
      right: 20,
      top: 40,
      bottom: 30
    },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月'],
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '区域数量',
      axisLabel: { fontSize: 10 }
    },
    series: [
      {
        name: '红色预警',
        data: [18, 15, 14, 12, 11, 12],
        type: 'line',
        itemStyle: { color: '#f56c6c' },
        areaStyle: { color: 'rgba(245, 108, 108, 0.2)' }
      },
      {
        name: '黄色预警',
        data: [35, 32, 30, 28, 26, 28],
        type: 'line',
        itemStyle: { color: '#e6a23c' },
        areaStyle: { color: 'rgba(230, 162, 60, 0.2)' }
      },
      {
        name: '绿色健康',
        data: [145, 150, 155, 160, 165, 156],
        type: 'line',
        itemStyle: { color: '#67c23a' },
        areaStyle: { color: 'rgba(103, 194, 58, 0.2)' }
      }
    ]
  }

  trendChart.setOption(option)
}

const handleResize = () => {
  trendChart?.resize()
}

onMounted(() => {
  initTrendChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style scoped>
.weak-area-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.alert-summary {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 16px;
}

.alert-summary h4 {
  margin-bottom: 12px;
}

.summary-cards {
  display: flex;
  gap: 12px;
}

.summary-item {
  flex: 1;
  text-align: center;
  padding: 12px;
  border-radius: 4px;
  color: #fff;
}

.summary-item.red {
  background: linear-gradient(135deg, #f56c6c, #e04040);
}

.summary-item.yellow {
  background: linear-gradient(135deg, #e6a23c, #d4920a);
}

.summary-item.green {
  background: linear-gradient(135deg, #67c23a, #5daf34);
}

.summary-item .count {
  font-size: 24px;
  font-weight: bold;
}

.summary-item .label {
  font-size: 11px;
  opacity: 0.9;
}

.alert-list {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.alert-list h4 {
  margin-bottom: 12px;
}

.alert-scroll {
  max-height: 350px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alert-item {
  padding: 12px;
  background: #fff;
  border-radius: 4px;
  border-left: 4px solid #67c23a;
  cursor: pointer;
  transition: all 0.3s;
}

.alert-item:hover {
  transform: translateX(4px);
}

.alert-item.red {
  border-left-color: #f56c6c;
  background: #fef0f0;
}

.alert-item.yellow {
  border-left-color: #e6a23c;
  background: #fdf6ec;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.alert-header .time {
  font-size: 11px;
  color: #999;
}

.alert-content .location {
  font-weight: bold;
  font-size: 13px;
  margin-bottom: 2px;
}

.alert-content .desc {
  font-size: 12px;
  color: #666;
}

.map-wrapper {
  position: relative;
  height: 100%;
  border-radius: 4px;
  overflow: hidden;
}

.traffic-light-legend {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.95);
  padding: 10px 14px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  display: flex;
  gap: 16px;
  z-index: 1;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.light {
  width: 14px;
  height: 14px;
  border-radius: 50%;
}

.light.red {
  background: #f56c6c;
  box-shadow: 0 0 6px #f56c6c;
}

.light.yellow {
  background: #e6a23c;
}

.light.green {
  background: #67c23a;
}

.attribution-panel {
  padding: 16px;
}

.tree-container {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 4px;
  margin-bottom: 16px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e6e6e6;
}

.tree-node.root {
  background: #fef0f0;
  border-color: #f56c6c;
}

.tree-node.level1 {
  margin-left: 30px;
  background: #fdf6ec;
  border-color: #e6a23c;
}

.tree-node.level2 {
  margin-left: 60px;
  background: #f5f7fa;
}

.node-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f56c6c;
  color: #fff;
  border-radius: 50%;
  font-weight: bold;
  font-size: 12px;
}

.tree-node.level1 .node-icon {
  background: #e6a23c;
}

.tree-node.level2 .node-icon {
  background: #909399;
}

.node-content {
  flex: 1;
}

.node-title {
  font-weight: 500;
}

.node-value {
  font-size: 12px;
  color: #f56c6c;
  font-weight: bold;
}

.tree-arrow {
  text-align: center;
  font-size: 18px;
  color: #999;
  padding: 8px 0;
  margin-left: 30px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
