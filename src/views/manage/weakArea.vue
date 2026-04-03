<template>
  <div class="weak-area-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>薄弱区域洞察 - 成华区四级路网预警分析</span>
          <div class="header-actions">
            <el-select v-model="filters.level" placeholder="预警级别" size="default" @change="handleFilterChange">
              <el-option label="全部" value="" />
              <el-option label="红色预警" value="red" />
              <el-option label="黄色预警" value="yellow" />
              <el-option label="绿色健康" value="green" />
            </el-select>
            <el-button type="primary" @click="loadData">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="20" style="height: 520px;">
        <el-col :span="8">
          <div class="alert-summary">
            <h4>预警统计（成华区）</h4>
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
            <h4>预警列表（成华区四级路网）</h4>
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
                  <span class="time">{{ alert.blockId }}</span>
                </div>
                <div class="alert-content">
                  <div class="location">{{ alert.blockName }}</div>
                  <div class="street">{{ alert.street }}</div>
                </div>
                <div class="alert-score">
                  <span class="score-label">短板指数</span>
                  <span class="score-value" :class="alert.level">{{ alert.weakScore }}</span>
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
              :zoom="13"
              @load="handleMapLoad"
              @click="handleMapClick"
            />
            <div class="traffic-light-legend">
              <span class="legend-item"><span class="light red"></span>红色预警（短板严重）</span>
              <span class="legend-item"><span class="light yellow"></span>黄色预警（需改善）</span>
              <span class="legend-item"><span class="light green"></span>绿色健康</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>预警趋势分析（成华区）</template>
          <div ref="trendChartRef" style="height: 280px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 四级路网详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="四级路网详情" size="450px" direction="rtl">
      <div class="block-detail" v-if="selectedBlock">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="片区编号">{{ selectedBlock.blockId }}</el-descriptions-item>
          <el-descriptions-item label="片区名称">{{ selectedBlock.blockName }}</el-descriptions-item>
          <el-descriptions-item label="所属街道">{{ selectedBlock.street }}</el-descriptions-item>
          <el-descriptions-item label="预警等级">
            <el-tag :type="getTagType(selectedBlock.level)">{{ getLevelText(selectedBlock.level) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>核心指标</el-divider>

        <div class="metrics-grid">
          <!-- 前端覆盖率 -->
          <div class="metric-card" :class="getMetricStatus(selectedBlock.coverage, 'coverage')">
            <div class="metric-header">
              <span class="metric-name">前端覆盖率</span>
              <span class="metric-status">{{ getMetricStatus(selectedBlock.coverage, 'coverage') }}</span>
            </div>
            <el-progress
              :percentage="selectedBlock.coverage"
              :color="getMetricColor(selectedBlock.coverage, 'coverage')"
              :stroke-width="12"
            />
            <div class="metric-value">{{ selectedBlock.coverage }}%</div>
            <div class="metric-compare">均值: {{ avgMetrics.coverage }}%</div>
          </div>

          <!-- 拜访频率 -->
          <div class="metric-card" :class="getMetricStatus(selectedBlock.visitFreq, 'visit')">
            <div class="metric-header">
              <span class="metric-name">拜访频率</span>
              <span class="metric-status">{{ getMetricStatus(selectedBlock.visitFreq, 'visit') }}</span>
            </div>
            <el-progress
              :percentage="selectedBlock.visitFreq"
              :color="getMetricColor(selectedBlock.visitFreq, 'visit')"
              :stroke-width="12"
            />
            <div class="metric-value">{{ selectedBlock.visitFreq }}次/周</div>
            <div class="metric-compare">均值: {{ avgMetrics.visitFreq }}次/周</div>
          </div>

          <!-- 氛围浓度 -->
          <div class="metric-card" :class="getMetricStatus(selectedBlock.atmosphere, 'atmosphere')">
            <div class="metric-header">
              <span class="metric-name">氛围浓度</span>
              <span class="metric-status">{{ getMetricStatus(selectedBlock.atmosphere, 'atmosphere') }}</span>
            </div>
            <el-progress
              :percentage="selectedBlock.atmosphere"
              :color="getMetricColor(selectedBlock.atmosphere, 'atmosphere')"
              :stroke-width="12"
            />
            <div class="metric-value">{{ selectedBlock.atmosphere }}</div>
            <div class="metric-compare">均值: {{ avgMetrics.atmosphere }}</div>
          </div>

          <!-- 动销金额 -->
          <div class="metric-card" :class="getMetricStatus(selectedBlock.sales, 'sales')">
            <div class="metric-header">
              <span class="metric-name">动销金额</span>
              <span class="metric-status">{{ getMetricStatus(selectedBlock.sales, 'sales') }}</span>
            </div>
            <el-progress
              :percentage="selectedBlock.sales"
              :color="getMetricColor(selectedBlock.sales, 'sales')"
              :stroke-width="12"
            />
            <div class="metric-value">{{ selectedBlock.sales }}万/月</div>
            <div class="metric-compare">均值: {{ avgMetrics.sales }}万/月</div>
          </div>
        </div>

        <el-divider>综合短板指数</el-divider>

        <div class="weak-score-summary">
          <div class="score-circle" :class="selectedBlock.level">
            <span class="score-number">{{ selectedBlock.weakScore }}</span>
            <span class="score-text">/100</span>
          </div>
          <div class="score-desc">
            <span class="level-text" :class="selectedBlock.level">{{ getLevelText(selectedBlock.level) }}</span>
            <span class="level-tip">{{ getLevelTip(selectedBlock.level) }}</span>
          </div>
        </div>

        <div class="action-buttons">
          <el-button type="primary" @click="handleDispatch">派发整改工单</el-button>
          <el-button @click="handleMarkProcessed">标记已处理</el-button>
        </div>
      </div>
    </el-drawer>
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
  level: ''
})

const alertStats = reactive({
  red: 0,
  yellow: 0,
  green: 0
})

const avgMetrics = reactive({
  coverage: 72,
  visitFreq: 68,
  atmosphere: 75,
  sales: 65
})

const selectedBlock = ref(null)
const drawerVisible = ref(false)

// 四级路网预警数据
const alerts = ref([])

// 筛选后的预警列表
const filteredAlerts = computed(() => {
  if (!filters.level) return alerts.value
  return alerts.value.filter(alert => alert.level === filters.level)
})

const getTagType = (level) => {
  const map = { red: 'danger', yellow: 'warning', green: 'success' }
  return map[level] || 'info'
}

const getLevelText = (level) => {
  const map = { red: '红色预警', yellow: '黄色预警', green: '绿色健康' }
  return map[level] || level
}

const getLevelTip = (level) => {
  const map = {
    red: '多项核心指标严重低于均值，需立即整改',
    yellow: '部分指标低于均值，需关注改善',
    green: '各项指标正常，继续保持'
  }
  return map[level] || ''
}

// 根据指标类型判断状态
const getMetricStatus = (value, type) => {
  const thresholds = {
    coverage: { red: 50, yellow: 70 },
    visit: { red: 40, yellow: 60 },
    atmosphere: { red: 40, yellow: 65 },
    sales: { red: 35, yellow: 55 }
  }
  const t = thresholds[type] || { red: 40, yellow: 60 }
  if (value < t.red) return 'danger'
  if (value < t.yellow) return 'warning'
  return 'success'
}

const getMetricColor = (value, type) => {
  const status = getMetricStatus(value, type)
  const colors = { danger: '#f56c6c', warning: '#e6a23c', success: '#67c23a' }
  return colors[status]
}

const handleMapLoad = async (map) => {
  mapInstance = map
  await loadDistrictData()
  loadData()
}

// 加载成华区四级路网
const loadDistrictData = async () => {
  try {
    const response = await fetch('/chenghua-district.geojson')
    const geojson = await response.json()

    // 为每个片区分配预警指标（模拟数据）
    geojson.features.forEach((feature, index) => {
      const coverage = Math.floor(Math.random() * 50 + 40)  // 40-90
      const visitFreq = Math.floor(Math.random() * 50 + 40)  // 40-90
      const atmosphere = Math.floor(Math.random() * 50 + 40)  // 40-90
      const sales = Math.floor(Math.random() * 50 + 35)       // 35-85

      // 计算短板指数（综合评估，低于阈值越多越严重）
      const weakScore = Math.floor((coverage * 0.25 + visitFreq * 0.25 + atmosphere * 0.25 + sales * 0.25))

      feature.properties.coverage = coverage
      feature.properties.visitFreq = visitFreq
      feature.properties.atmosphere = atmosphere
      feature.properties.sales = sales
      feature.properties.weakScore = weakScore
      feature.properties.blockId = `CH-4L-${String(index + 1).padStart(4, '0')}`
      feature.properties.blockName = `成华区核心街区${String(index + 1).padStart(2, '0')}`
      feature.properties.street = feature.properties['街道[内置]'] || '府青路街道'
      feature.properties.index = index
    })

    // 根据短板指数分配预警等级
    geojson.features.forEach(feature => {
      const score = feature.properties.weakScore
      if (score < 50) {
        feature.properties.alertLevel = 'red'
      } else if (score < 65) {
        feature.properties.alertLevel = 'yellow'
      } else {
        feature.properties.alertLevel = 'green'
      }
    })

    // 渲染路网
    mapRef.value?.addRoadLayer('chenghua-road-network', geojson, {
      alertLevelField: 'alertLevel',
      opacity: 0.7
    })

    // 生成预警列表数据
    generateAlertList(geojson.features)

    // 更新统计
    updateStats()

    ElMessage.success('成华区四级路网数据加载成功')
  } catch (e) {
    console.error('加载成华区数据失败:', e)
    ElMessage.error('加载路网数据失败')
  }
}

const generateAlertList = (features) => {
  alerts.value = features.map(feature => {
    const props = feature.properties
    return {
      id: props.index,
      blockId: props.blockId,
      blockName: props.blockName,
      street: props.street,
      level: props.alertLevel,
      weakScore: props.weakScore,
      coverage: props.coverage,
      visitFreq: props.visitFreq,
      atmosphere: props.atmosphere,
      sales: props.sales,
      center: props['中心点[内置]']?.split(',').map(Number) || [104.094, 30.679],
      geojson: feature
    }
  }).sort((a, b) => a.weakScore - b.weakScore)
}

const updateStats = () => {
  alertStats.red = alerts.value.filter(a => a.level === 'red').length
  alertStats.yellow = alerts.value.filter(a => a.level === 'yellow').length
  alertStats.green = alerts.value.filter(a => a.level === 'green').length
}

const loadData = () => {
  if (!mapInstance) return
  // 重新加载数据
  loadDistrictData()
  ElMessage.success('数据已刷新')
}

const handleFilterChange = () => {
  // 筛选由计算属性处理
}

const handleMapClick = (e) => {
  if (!mapInstance || !e.features || e.features.length === 0) return

  // 获取点击的路网要素
  const feature = e.features.find(f => f.layer?.id?.includes('zone-fill'))
  if (!feature) return

  const props = feature.properties
  const alert = alerts.value.find(a => a.blockId === props.blockId)
  if (alert) {
    selectedBlock.value = alert
    drawerVisible.value = true

    // 飞转到该区域
    const center = props['中心点[内置]']?.split(',').map(Number) || [104.094, 30.679]
    mapRef.value?.flyTo({
      center,
      zoom: 15,
      duration: 1000
    })
  }
}

const handleSelectAlert = (alert) => {
  selectedBlock.value = alert
  drawerVisible.value = true

  // 飞转到该位置
  mapRef.value?.flyTo({
    center: alert.center,
    zoom: 15,
    duration: 1000
  })
}

const handleDispatch = () => {
  ElMessage.success('整改工单已派发')
  drawerVisible.value = false
}

const handleMarkProcessed = () => {
  ElMessage.success('已标记为已处理')
  drawerVisible.value = false
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
      data: ['2025年10月', '11月', '12月', '2026年1月', '2月', '3月'],
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '路网数量',
      axisLabel: { fontSize: 10 }
    },
    series: [
      {
        name: '红色预警',
        data: [8, 7, 6, 5, 4, 5],
        type: 'line',
        itemStyle: { color: '#f56c6c' },
        areaStyle: { color: 'rgba(245, 108, 108, 0.2)' }
      },
      {
        name: '黄色预警',
        data: [15, 14, 13, 12, 11, 12],
        type: 'line',
        itemStyle: { color: '#e6a23c' },
        areaStyle: { color: 'rgba(230, 162, 60, 0.2)' }
      },
      {
        name: '绿色健康',
        data: [39, 41, 43, 45, 47, 45],
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
  font-size: 14px;
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
  font-size: 14px;
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
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
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

.alert-content .street {
  font-size: 11px;
  color: #666;
}

.alert-score {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e6e6e6;
}

.score-label {
  font-size: 11px;
  color: #999;
}

.score-value {
  font-weight: bold;
  font-size: 14px;
}

.score-value.red {
  color: #f56c6c;
}

.score-value.yellow {
  color: #e6a23c;
}

.score-value.green {
  color: #67c23a;
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

/* 详情抽屉样式 */
.block-detail {
  padding: 0 16px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.metric-card {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #e6e6e6;
}

.metric-card.danger {
  background: #fef0f0;
  border-color: #f56c6c;
}

.metric-card.warning {
  background: #fdf6ec;
  border-color: #e6a23c;
}

.metric-card.success {
  background: #f0f9eb;
  border-color: #67c23a;
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.metric-name {
  font-weight: bold;
  font-size: 13px;
}

.metric-status {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 2px;
}

.metric-card.danger .metric-status {
  background: #f56c6c;
  color: #fff;
}

.metric-card.warning .metric-status {
  background: #e6a23c;
  color: #fff;
}

.metric-card.success .metric-status {
  background: #67c23a;
  color: #fff;
}

.metric-value {
  font-size: 20px;
  font-weight: bold;
  margin-top: 8px;
  color: #303133;
}

.metric-compare {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

.weak-score-summary {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.score-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.score-circle.red {
  background: linear-gradient(135deg, #f56c6c, #e04040);
}

.score-circle.yellow {
  background: linear-gradient(135deg, #e6a23c, #d4920a);
}

.score-circle.green {
  background: linear-gradient(135deg, #67c23a, #5daf34);
}

.score-number {
  font-size: 28px;
  font-weight: bold;
  line-height: 1;
}

.score-text {
  font-size: 12px;
  opacity: 0.8;
}

.score-desc {
  flex: 1;
}

.level-text {
  display: block;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 4px;
}

.level-text.red {
  color: #f56c6c;
}

.level-text.yellow {
  color: #e6a23c;
}

.level-text.green {
  color: #67c23a;
}

.level-tip {
  font-size: 12px;
  color: #666;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
