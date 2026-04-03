<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>前端网点总数</span>
            </div>
          </template>
          <div class="stat-value">12,856</div>
          <div class="stat-trend up">较上月 +328</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>本月扫码量</span>
            </div>
          </template>
          <div class="stat-value">2.85亿</div>
          <div class="stat-trend up">较上月 +12.5%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>活跃渠道数</span>
            </div>
          </template>
          <div class="stat-value">8,642</div>
          <div class="stat-trend up">较上月 +156</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>异常预警</span>
            </div>
          </template>
          <div class="stat-value warning">23</div>
          <div class="stat-trend down">较上月 -8</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>前端分布热力图</span>
              <el-button type="primary" size="small" @click="$router.push('/market/heatmap')">
                查看详情
              </el-button>
            </div>
          </template>
          <div class="map-wrapper">
            <GdsMap
              ref="dashboardMapRef"
              :center="[104.094, 30.679]"
              :zoom="11"
              @load="handleDashboardMapLoad"
            />
            <div class="map-legend-overlay">
              <span class="legend-item"><span class="dot heat"></span>热力密度</span>
              <span class="legend-item"><span class="dot store"></span>网点位置</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>快捷入口</span>
            </div>
          </template>
          <div class="quick-entry">
            <div class="entry-item" @click="$router.push('/market/insight')">
              <el-icon :size="32"><Location /></el-icon>
              <span>空白网点挖掘</span>
            </div>
            <div class="entry-item" @click="$router.push('/market/flow')">
              <el-icon :size="32"><Guide /></el-icon>
              <span>订单流向</span>
            </div>
            <div class="entry-item" @click="$router.push('/user/select')">
              <el-icon :size="32"><Aim /></el-icon>
              <span>产品推广选区</span>
            </div>
            <div class="entry-item" @click="$router.push('/manage/weak')">
              <el-icon :size="32"><Warning /></el-icon>
              <span>薄弱区域洞察</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>区域效能排行</span>
            </div>
          </template>
          <div ref="rankChartRef" style="height: 250px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>最近预警</span>
              <el-button type="primary" size="small" @click="$router.push('/manage/weak')">
                查看全部
              </el-button>
            </div>
          </template>
          <el-table :data="warningList" style="width: 100%">
            <el-table-column prop="time" label="时间" width="100" />
            <el-table-column prop="location" label="位置" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === '待处理' ? 'danger' : 'info'">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>月度销量趋势</span>
            </div>
          </template>
          <div ref="salesTrendRef" style="height: 250px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>品类占比分布</span>
            </div>
          </template>
          <div ref="categoryPieRef" style="height: 250px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import GdsMap from '@/components/GdsMap.vue'

const dashboardMapRef = ref()
const rankChartRef = ref()
const salesTrendRef = ref()
const categoryPieRef = ref()

let rankChart = null
let salesChart = null
let categoryChart = null

const warningList = [
  { time: '10:32', location: '成都市高新区-天府大道', type: '动销断崖', status: '待处理' },
  { time: '09:45', location: '重庆市江北区-观音桥', type: '拜访频次下降', status: '待处理' },
  { time: '09:12', location: '武汉市洪山区-光谷', type: '陈列异常', status: '已处理' }
]

// GIS地图数据
const storeLocations = [
  { name: '红旗超市(天府店)', lng: 104.063, lat: 30.548 },
  { name: '永辉超市(高新店)', lng: 104.071, lat: 30.552 },
  { name: '盒马鲜生(锦江店)', lng: 104.086, lat: 30.659 },
  { name: '沃尔玛(武侯店)', lng: 104.035, lat: 30.625 },
  { name: '舞东风(玉林店)', lng: 104.052, lat: 30.641 },
  { name: '红旗超市(建设路)', lng: 104.103, lat: 30.665 },
  { name: '步步高(春熙路)', lng: 104.091, lat: 30.658 },
  { name: '家乐福(青羊店)', lng: 104.053, lat: 30.671 }
]

const handleDashboardMapLoad = (map) => {
  // 生成热力数据
  const heatData = []
  for (let i = 0; i < 150; i++) {
    heatData.push({
      coordinates: [
        104.0 + Math.random() * 0.2,
        30.55 + Math.random() * 0.15
      ],
      intensity: Math.random()
    })
  }

  // 添加热力图层
  dashboardMapRef.value?.addHeatLayer('dashboardHeat', heatData, {
    radius: 35,
    intensity: 0.8,
    opacity: 0.6
  })

  // 添加网点散点
  const scatterData = storeLocations.map(store => ({
    coordinates: [store.lng, store.lat],
    properties: { name: store.name }
  }))

  dashboardMapRef.value?.addScatterLayer('dashboardStores', scatterData, {
    radius: 8,
    color: '#409eff'
  })
}

const initRankChart = () => {
  if (!rankChartRef.value) return

  rankChart = echarts.init(rankChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: 100,
      right: 40,
      top: 20,
      bottom: 30
    },
    xAxis: {
      type: 'value'
    },
    yAxis: {
      type: 'category',
      data: ['长沙市岳麓区', '西安市雁塔区', '武汉市武昌区', '重庆市渝北区', '成都市锦江区'],
      axisLabel: {
        fontSize: 11
      }
    },
    series: [{
      data: [82, 85, 88, 92, 95],
      type: 'bar',
      itemStyle: {
        color: (params) => {
          const colors = ['#909399', '#e6a23c', '#67c23a', '#409eff', '#f56c6c']
          return colors[params.dataIndex]
        },
        borderRadius: [0, 4, 4, 0]
      },
      label: {
        show: true,
        position: 'right',
        fontSize: 12,
        formatter: '{c}分'
      }
    }]
  }

  rankChart.setOption(option)
}

const initSalesChart = () => {
  if (!salesTrendRef.value) return

  salesChart = echarts.init(salesTrendRef.value)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['白酒', '啤酒', '红酒'],
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
      axisLabel: {
        fontSize: 11,
        formatter: (value) => value >= 10000 ? (value / 10000) + '亿' : value + '万'
      }
    },
    series: [
      {
        name: '白酒',
        data: [8500, 9200, 10800, 11500, 12800, 14200],
        type: 'line',
        smooth: true,
        itemStyle: { color: '#f56c6c' }
      },
      {
        name: '啤酒',
        data: [5200, 5800, 6500, 7200, 8100, 8900],
        type: 'line',
        smooth: true,
        itemStyle: { color: '#409eff' }
      },
      {
        name: '红酒',
        data: [3100, 3500, 3800, 4200, 4600, 5100],
        type: 'line',
        smooth: true,
        itemStyle: { color: '#67c23a' }
      }
    ]
  }

  salesChart.setOption(option)
}

const initCategoryChart = () => {
  if (!categoryPieRef.value) return

  categoryChart = echarts.init(categoryPieRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}万 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 20,
      top: 'center',
      textStyle: { fontSize: 11 }
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 6,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
        }
      },
      data: [
        { value: 12800, name: '白酒', itemStyle: { color: '#f56c6c' } },
        { value: 8100, name: '啤酒', itemStyle: { color: '#409eff' } },
        { value: 4600, name: '红酒', itemStyle: { color: '#67c23a' } },
        { value: 3200, name: '其他', itemStyle: { color: '#909399' } }
      ]
    }]
  }

  categoryChart.setOption(option)
}

const handleResize = () => {
  rankChart?.resize()
  salesChart?.resize()
  categoryChart?.resize()
}

onMounted(() => {
  initRankChart()
  initSalesChart()
  initCategoryChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  rankChart?.dispose()
  salesChart?.dispose()
  categoryChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

.stat-value.warning {
  color: #f56c6c;
}

.stat-trend {
  font-size: 14px;
  margin-top: 8px;
}

.stat-trend.up {
  color: #67c23a;
}

.stat-trend.down {
  color: #f56c6c;
}

.map-wrapper {
  position: relative;
  height: 300px;
  border-radius: 4px;
  overflow: hidden;
}

.map-legend-overlay {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.95);
  padding: 8px 12px;
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

.legend-item .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-item .dot.heat {
  background: linear-gradient(135deg, #313695, #d73027);
}

.legend-item .dot.store {
  background: #409eff;
}

.quick-entry {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.entry-item:hover {
  background: #e6f0ff;
  color: #409eff;
}

.entry-item span {
  font-size: 14px;
}

.trend-up {
  color: #67c23a;
}

.trend-down {
  color: #f56c6c;
}
</style>
