<template>
  <div class="evaluate-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>营销人员效能评估 - DEA效能排行榜</span>
          <div class="header-actions">
            <el-select v-model="filters.region" placeholder="选择大区" size="default" @change="loadData">
              <el-option label="全部大区" value="" />
              <el-option label="西南大区" value="xn" />
              <el-option label="华中大区" value="hz" />
            </el-select>
            <el-select v-model="filters.period" placeholder="评估周期" size="default" @change="loadData">
              <el-option label="本月" value="month" />
              <el-option label="本季" value="quarter" />
              <el-option label="本年" value="year" />
            </el-select>
          </div>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="12">
          <div class="map-wrapper">
            <GdsMap
              ref="mapRef"
              :center="[104.094, 30.679]"
              :zoom="10"
              @load="handleMapLoad"
            />
            <div class="rank-legend">
              <span class="legend-item"><span class="dot excellent"></span>卓越(>=90)</span>
              <span class="legend-item"><span class="dot good"></span>优秀(80-90)</span>
              <span class="legend-item"><span class="dot warning"></span>待改善(60-80)</span>
              <span class="legend-item"><span class="dot danger"></span>危险(<60)</span>
            </div>
          </div>

          <div class="rank-panel">
            <h4>效能排行榜</h4>
            <div class="rank-tabs">
              <el-radio-group v-model="rankType" size="small" @change="handleRankTypeChange">
                <el-radio-button label="top">Top 5 功臣</el-radio-button>
                <el-radio-button label="bottom">Bottom 5 待改善</el-radio-button>
              </el-radio-group>
            </div>
            <div class="rank-list">
              <div
                v-for="(item, index) in displayRankList"
                :key="item.id"
                class="rank-item"
                :class="{ selected: selectedPerson?.id === item.id }"
                @click="handleSelectPerson(item)"
              >
                <div class="rank-num" :class="{ top: index < 3 }">{{ index + 1 }}</div>
                <div class="rank-info">
                  <div class="name">{{ item.name }}</div>
                  <div class="region">{{ item.region }}</div>
                </div>
                <div class="score">
                  <span class="score-value">{{ item.score }}</span>
                  <span class="score-label">效能分</span>
                </div>
              </div>
            </div>
          </div>
        </el-col>

        <el-col :span="12">
          <div class="profile-panel" v-if="selectedPerson">
            <div class="profile-header">
              <el-avatar :size="64" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <div class="profile-info">
                <h3>{{ selectedPerson.name }}</h3>
                <div class="meta">{{ selectedPerson.region }} | {{ selectedPerson.title }}</div>
                <div class="efficiency-badge" :class="selectedPerson.level">
                  {{ getEfficiencyLevel(selectedPerson.score) }}
                </div>
              </div>
            </div>

            <div class="profile-content">
              <div class="radar-section">
                <h4>五维能力雷达图</h4>
                <div ref="radarChartRef" class="chart-container"></div>
              </div>

              <div class="score-bars">
                <h4>分项得分</h4>
                <div class="bar-item">
                  <span class="bar-label">规模贡献</span>
                  <div class="bar-track">
                    <div class="bar-fill" :style="{ width: selectedPerson.dimensions[0] + '%', background: '#409eff' }"></div>
                  </div>
                  <span class="bar-value">{{ selectedPerson.dimensions[0] }}</span>
                </div>
                <div class="bar-item">
                  <span class="bar-label">增长潜力</span>
                  <div class="bar-track">
                    <div class="bar-fill" :style="{ width: selectedPerson.dimensions[1] + '%', background: '#67c23a' }"></div>
                  </div>
                  <span class="bar-value">{{ selectedPerson.dimensions[1] }}</span>
                </div>
                <div class="bar-item">
                  <span class="bar-label">执行力</span>
                  <div class="bar-track">
                    <div class="bar-fill" :style="{ width: selectedPerson.dimensions[2] + '%', background: '#e6a23c' }"></div>
                  </div>
                  <span class="bar-value">{{ selectedPerson.dimensions[2] }}</span>
                </div>
                <div class="bar-item">
                  <span class="bar-label">费销比效能</span>
                  <div class="bar-track">
                    <div class="bar-fill" :style="{ width: selectedPerson.dimensions[3] + '%', background: '#f56c6c' }"></div>
                  </div>
                  <span class="bar-value">{{ selectedPerson.dimensions[3] }}</span>
                </div>
                <div class="bar-item">
                  <span class="bar-label">市场健康度</span>
                  <div class="bar-track">
                    <div class="bar-fill" :style="{ width: selectedPerson.dimensions[4] + '%', background: '#9933ff' }"></div>
                  </div>
                  <span class="bar-value">{{ selectedPerson.dimensions[4] }}</span>
                </div>
              </div>
            </div>

            <div class="ai-suggestion">
              <h4>
                <el-icon><MagicStick /></el-icon>
                AI智能调优建议
              </h4>
              <div class="suggestion-content">
                <div class="strengths">
                  <span class="tag success">优势</span>
                  <span>{{ selectedPerson.strengths }}</span>
                </div>
                <div class="weaknesses">
                  <span class="tag warning">待改进</span>
                  <span>{{ selectedPerson.weaknesses }}</span>
                </div>
                <div class="suggestion">
                  <span class="tag primary">建议</span>
                  <span>{{ selectedPerson.suggestion }}</span>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="点击左侧人员查看详情" />
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>效能分布统计</template>
          <div ref="distributionChartRef" style="height: 250px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>区域效能对比</template>
          <div ref="regionCompareRef" style="height: 250px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import GdsMap from '@/components/GdsMap.vue'

const mapRef = ref()
const radarChartRef = ref()
const distributionChartRef = ref()
const regionCompareRef = ref()

let radarChart = null
let distChart = null
let regionChart = null

const filters = reactive({
  region: '',
  period: 'month'
})

const rankType = ref('top')
const selectedPerson = ref(null)

const allRankList = ref([
  { id: 1, name: '张伟', region: '成都市锦江区', score: 95, title: '区域经理', level: 'excellent', dimensions: [92, 88, 95, 78, 85], strengths: '执行力强，费销比控制优秀', weaknesses: '增长潜力挖掘不足', suggestion: '建议加强新品推广力度，拓展高潜力客户。', coord: [104.063, 30.658] },
  { id: 2, name: '李娜', region: '重庆市渝北区', score: 92, title: '区域经理', level: 'excellent', dimensions: [88, 95, 85, 82, 90], strengths: '市场开拓能力强，客户关系好', weaknesses: '执行力有待提升', suggestion: '建议优化拜访路线，提高执行效率。', coord: [104.063, 30.548] },
  { id: 3, name: '王强', region: '武汉市武昌区', score: 88, title: '区域经理', level: 'good', dimensions: [95, 82, 88, 85, 78], strengths: '规模贡献突出，历史业绩稳定', weaknesses: '市场健康度波动较大', suggestion: '建议加强终端维护，稳固基础盘面。', coord: [104.063, 30.552] },
  { id: 4, name: '刘洋', region: '西安市雁塔区', score: 85, title: '区域经理', level: 'good', dimensions: [80, 85, 82, 88, 92], strengths: '市场健康度良好，客情稳定', weaknesses: '规模贡献中等', suggestion: '建议深耕核心客户，提升单产。', coord: [104.071, 30.552] },
  { id: 5, name: '陈静', region: '长沙市岳麓区', score: 82, title: '区域经理', level: 'good', dimensions: [78, 88, 85, 75, 80], strengths: '增长潜力大，学习能力强', weaknesses: '费销比效能偏低', suggestion: '建议加强费用管控，提升投入产出比。', coord: [104.086, 30.659] },
  { id: 6, name: '周磊', region: '成都市武侯区', score: 65, title: '区域经理', level: 'warning', dimensions: [60, 70, 62, 58, 72], strengths: '工作态度认真', weaknesses: '整体效能偏低，需要帮扶', suggestion: '建议安排老员工帮带，加强业务培训。', coord: [104.035, 30.625] },
  { id: 7, name: '吴敏', region: '重庆市江北区', score: 58, title: '区域经理', level: 'danger', dimensions: [55, 62, 52, 50, 65], strengths: '-', weaknesses: '多项目标落后，需重点关注', suggestion: '建议调整区域或加强管理监督。', coord: [104.103, 30.665] }
])

const displayRankList = computed(() => {
  return rankType.value === 'top'
    ? allRankList.value.slice(0, 5)
    : allRankList.value.slice(-5).reverse()
})

const getEfficiencyLevel = (score) => {
  if (score >= 90) return '卓越'
  if (score >= 80) return '优秀'
  if (score >= 70) return '良好'
  if (score >= 60) return '待改善'
  return '危险'
}

const handleMapLoad = (map) => {
  loadData()
}

const loadData = () => {
  if (!mapRef.value) return

  // 按等级分组
  const excellentList = allRankList.value.filter(a => a.score >= 90)
  const goodList = allRankList.value.filter(a => a.score >= 70 && a.score < 90)
  const warningList = allRankList.value.filter(a => a.score >= 60 && a.score < 70)
  const dangerList = allRankList.value.filter(a => a.score < 60)

  mapRef.value?.removeLayer('excellentMarker')
  mapRef.value?.removeLayer('goodMarker')
  mapRef.value?.removeLayer('warningMarker')
  mapRef.value?.removeLayer('dangerMarker')

  if (excellentList.length > 0) {
    const data = excellentList.map(a => ({ coordinates: a.coord, properties: { name: a.name, score: a.score } }))
    mapRef.value?.addScatterLayer('excellentMarker', data, { radius: 12, color: '#9933ff' })
  }

  if (goodList.length > 0) {
    const data = goodList.map(a => ({ coordinates: a.coord, properties: { name: a.name, score: a.score } }))
    mapRef.value?.addScatterLayer('goodMarker', data, { radius: 10, color: '#409eff' })
  }

  if (warningList.length > 0) {
    const data = warningList.map(a => ({ coordinates: a.coord, properties: { name: a.name, score: a.score } }))
    mapRef.value?.addScatterLayer('warningMarker', data, { radius: 8, color: '#e6a23c' })
  }

  if (dangerList.length > 0) {
    const data = dangerList.map(a => ({ coordinates: a.coord, properties: { name: a.name, score: a.score } }))
    mapRef.value?.addScatterLayer('dangerMarker', data, { radius: 8, color: '#f56c6c' })
  }
}

const initRadarChart = () => {
  if (!radarChartRef.value || !selectedPerson.value) return

  radarChart = echarts.init(radarChartRef.value)

  const option = {
    radar: {
      indicator: [
        { name: '规模贡献', max: 100 },
        { name: '增长潜力', max: 100 },
        { name: '执行力', max: 100 },
        { name: '费销比效能', max: 100 },
        { name: '市场健康度', max: 100 }
      ],
      radius: '65%',
      axisName: {
        fontSize: 11
      }
    },
    series: [{
      type: 'radar',
      data: [
        {
          value: selectedPerson.value.dimensions,
          name: selectedPerson.value.name,
          areaStyle: { color: 'rgba(64, 158, 255, 0.3)' },
          lineStyle: { color: '#409eff', width: 2 },
          itemStyle: { color: '#409eff' }
        },
        {
          value: [75, 75, 75, 75, 75],
          name: '全国平均',
          lineStyle: { color: '#909399', type: 'dashed', width: 2 },
          areaStyle: { color: 'rgba(144, 147, 153, 0.1)' },
          itemStyle: { color: '#909399' }
        }
      ]
    }]
  }

  radarChart.setOption(option)
}

const initDistributionChart = () => {
  if (!distributionChartRef.value) return

  distChart = echarts.init(distributionChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: 50,
      right: 20,
      top: 20,
      bottom: 30
    },
    xAxis: {
      type: 'category',
      data: ['<60', '60-70', '70-80', '80-90', '>90'],
      name: '效能分段',
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '人数',
      axisLabel: { fontSize: 11 }
    },
    series: [{
      data: [2, 5, 12, 18, 8],
      type: 'bar',
      itemStyle: {
        color: (params) => {
          const colors = ['#f56c6c', '#e6a23c', '#67c23a', '#409eff', '#9933ff']
          return colors[params.dataIndex]
        },
        borderRadius: [4, 4, 0, 0]
      },
      label: {
        show: true,
        position: 'top',
        fontSize: 11
      }
    }]
  }

  distChart.setOption(option)
}

const initRegionCompareChart = () => {
  if (!regionCompareRef.value) return

  regionChart = echarts.init(regionCompareRef.value)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['本月', '上月'],
      top: 0,
      textStyle: { fontSize: 11 }
    },
    grid: {
      left: 80,
      right: 20,
      top: 40,
      bottom: 30
    },
    xAxis: {
      type: 'category',
      data: ['成都锦江', '重庆渝北', '武汉武昌', '西安雁塔', '长沙岳麓', '成都武侯'],
      axisLabel: { fontSize: 10, rotate: 15 }
    },
    yAxis: {
      type: 'value',
      name: '效能分',
      axisLabel: { fontSize: 10 }
    },
    series: [
      {
        name: '本月',
        data: [95, 92, 88, 85, 82, 65],
        type: 'bar',
        itemStyle: { color: '#409eff' },
        barWidth: '35%'
      },
      {
        name: '上月',
        data: [92, 90, 86, 84, 80, 68],
        type: 'bar',
        itemStyle: { color: '#909399' },
        barWidth: '35%'
      }
    ]
  }

  regionChart.setOption(option)
}

const handleSelectPerson = (person) => {
  selectedPerson.value = person
  nextTick(() => {
    initRadarChart()
  })

  // 飞转到该人员区域
  mapRef.value?.flyTo({
    center: person.coord,
    zoom: 13,
    duration: 1000
  })
}

const handleRankTypeChange = () => {
  selectedPerson.value = null
}

const handleResize = () => {
  radarChart?.resize()
  distChart?.resize()
  regionChart?.resize()
}

onMounted(() => {
  initDistributionChart()
  initRegionCompareChart()
  if (allRankList.value.length > 0) {
    handleSelectPerson(allRankList.value[0])
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  radarChart?.dispose()
  distChart?.dispose()
  regionChart?.dispose()
})
</script>

<style scoped>
.evaluate-page {
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

.map-wrapper {
  position: relative;
  height: 250px;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 16px;
}

.rank-legend {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.95);
  padding: 8px 12px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  display: flex;
  gap: 12px;
  z-index: 1;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot.excellent {
  background: #9933ff;
}

.dot.good {
  background: #409eff;
}

.dot.warning {
  background: #e6a23c;
}

.dot.danger {
  background: #f56c6c;
}

.rank-panel {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.rank-panel h4 {
  margin-bottom: 12px;
}

.rank-tabs {
  margin-bottom: 12px;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 280px;
  overflow-y: auto;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.rank-item:hover {
  background: #e6f0ff;
}

.rank-item.selected {
  border-color: #409eff;
  background: #e6f0ff;
}

.rank-num {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #909399;
  color: #fff;
  border-radius: 50%;
  font-weight: bold;
  font-size: 12px;
}

.rank-num.top {
  background: linear-gradient(135deg, #f56c6c, #e04040);
}

.rank-info {
  flex: 1;
}

.rank-info .name {
  font-weight: bold;
  font-size: 14px;
}

.rank-info .region {
  font-size: 12px;
  color: #909399;
}

.score {
  text-align: center;
}

.score-value {
  display: block;
  font-size: 22px;
  font-weight: bold;
  color: #409eff;
}

.score-label {
  font-size: 11px;
  color: #909399;
}

.profile-panel {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  height: 100%;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.profile-info h3 {
  margin: 0 0 4px 0;
}

.profile-info .meta {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

.efficiency-badge {
  display: inline-block;
  padding: 2px 12px;
  border-radius: 12px;
  font-size: 12px;
  color: #fff;
}

.efficiency-badge.excellent {
  background: linear-gradient(135deg, #9933ff, #762a83);
}

.efficiency-badge.good {
  background: linear-gradient(135deg, #409eff, #1866d9);
}

.efficiency-badge.warning {
  background: linear-gradient(135deg, #e6a23c, #d4920a);
}

.efficiency-badge.danger {
  background: linear-gradient(135deg, #f56c6c, #e04040);
}

.profile-content {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.radar-section {
  flex: 1;
}

.score-bars {
  flex: 1;
}

.radar-section h4,
.score-bars h4 {
  margin-bottom: 12px;
  font-size: 14px;
}

.chart-container {
  height: 200px;
}

.bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.bar-label {
  width: 80px;
  font-size: 12px;
  color: #666;
}

.bar-track {
  flex: 1;
  height: 10px;
  background: #e6e6e6;
  border-radius: 5px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.5s ease;
}

.bar-value {
  width: 30px;
  font-size: 12px;
  font-weight: bold;
  text-align: right;
}

.ai-suggestion {
  background: #fff;
  border-radius: 4px;
  padding: 16px;
}

.ai-suggestion h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
}

.suggestion-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
  line-height: 1.6;
}

.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  color: #fff;
  margin-right: 6px;
}

.tag.success {
  background: #67c23a;
}

.tag.warning {
  background: #e6a23c;
}

.tag.primary {
  background: #409eff;
}
</style>
