<template>
  <div class="flow-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>订单流向及时效分析</span>
          <div class="header-actions">
            <el-select v-model="filters.warehouse" placeholder="选择仓库" size="default" @change="loadData">
              <el-option label="全部仓库" value="" />
              <el-option label="成都中心仓" value="cd" />
              <el-option label="重庆中心仓" value="cq" />
            </el-select>
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              size="default"
              @change="loadData"
            />
            <el-button type="primary" @click="loadData">刷新</el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="16">
          <div class="map-wrapper">
            <GdsMap
              ref="mapRef"
              :center="[104.094, 30.679]"
              :zoom="11"
              @load="handleMapLoad"
            />
            <div class="flow-legend-overlay">
              <span class="legend-item"><span class="line green"></span>T+1 (1天内)</span>
              <span class="legend-item"><span class="line yellow"></span>T+2 (2天内)</span>
              <span class="legend-item"><span class="line red"></span>T+3+ (3天以上)</span>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="flow-stats">
            <h4>时效统计</h4>
            <div class="efficiency-item">
              <div class="efficiency-label">T+1时效达成率</div>
              <div class="efficiency-value positive">78.5%</div>
              <el-progress :percentage="78.5" :stroke-width="10" :color="'#67c23a'" />
            </div>
            <div class="efficiency-item">
              <div class="efficiency-label">T+2时效达成率</div>
              <div class="efficiency-value">94.2%</div>
              <el-progress :percentage="94.2" :stroke-width="10" :color="'#e6a23c'" />
            </div>
            <div class="efficiency-item">
              <div class="efficiency-label">T+3+时效占比</div>
              <div class="efficiency-value negative">5.8%</div>
              <el-progress :percentage="5.8" :stroke-width="10" :color="'#f56c6c'" />
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>时效统计趋势</template>
          <div ref="efficiencyTrendRef" style="height: 250px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>门店扫码闭环追踪</template>
          <el-table :data="scanData" height="220">
            <el-table-column prop="storeName" label="门店名称" />
            <el-table-column prop="orderNo" label="订单号" width="130" />
            <el-table-column prop="deliverTime" label="发货时间" width="100" />
            <el-table-column prop="scanTime" label="扫码时间" width="100" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === '已扫码' ? 'success' : 'info'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import GdsMap from '@/components/GdsMap.vue'

const mapRef = ref()
const efficiencyTrendRef = ref()
let mapInstance = null
let trendChart = null

const filters = reactive({
  warehouse: '',
  dateRange: []
})

const scanData = ref([
  { storeName: '红旗超市(天府店)', orderNo: 'ORD2026040201', deliverTime: '04-02 08:30', scanTime: '04-03 09:15', status: '已扫码' },
  { storeName: '永辉超市(高新店)', orderNo: 'ORD2026040202', deliverTime: '04-02 09:00', scanTime: '04-03 08:45', status: '已扫码' },
  { storeName: '盒马鲜生(锦江店)', orderNo: 'ORD2026040203', deliverTime: '04-02 10:00', scanTime: '-', status: '未扫码' },
  { storeName: '沃尔玛(武侯店)', orderNo: 'ORD2026040204', deliverTime: '04-02 14:00', scanTime: '04-03 10:20', status: '已扫码' },
  { storeName: '家乐福(青羊店)', orderNo: 'ORD2026040205', deliverTime: '04-02 15:30', scanTime: '-', status: '未扫码' }
])

const handleMapLoad = (map) => {
  mapInstance = map
  loadData()
}

const loadData = () => {
  if (!mapInstance) return

  // 仓库位置 (成都)
  const warehouse = [104.06, 30.65]

  // 门店位置和订单数据
  const stores = [
    { name: '红旗超市(天府店)', coord: [104.063, 30.548], amount: 128, days: 1 },
    { name: '永辉超市(高新店)', coord: [104.071, 30.552], amount: 96, days: 1 },
    { name: '盒马鲜生(锦江店)', coord: [104.086, 30.659], amount: 75, days: 2 },
    { name: '沃尔玛(武侯店)', coord: [104.035, 30.625], amount: 58, days: 1 },
    { name: '家乐福(青羊店)', coord: [104.053, 30.671], amount: 45, days: 3 },
    { name: '舞东风(玉林店)', coord: [104.052, 30.641], amount: 38, days: 1 },
    { name: '红旗超市(建设路)', coord: [104.103, 30.665], amount: 52, days: 2 },
    { name: '步步高(春熙路)', coord: [104.091, 30.658], amount: 42, days: 2 }
  ]

  // 生成弧线数据
  const arcData = stores.map(store => ({
    coordinates: [warehouse, store.coord],
    color: store.days === 1 ? '#67c23a' : store.days === 2 ? '#e6a23c' : '#f56c6c',
    width: 1 + store.amount / 50
  }))

  mapRef.value?.addArcLayer('flowArcs', arcData, {
    color: '#409eff',
    width: 2
  })

  // 添加仓库散点
  const warehouseData = [{ coordinates: warehouse, properties: { name: '成都中心仓' } }]
  mapRef.value?.addScatterLayer('warehouse', warehouseData, {
    radius: 15,
    color: '#f56c6c'
  })

  // 添加门店散点
  const storeData = stores.map(store => ({
    coordinates: store.coord,
    properties: { name: store.name, days: store.days }
  }))
  mapRef.value?.addScatterLayer('stores', storeData, {
    radius: 8,
    color: '#409eff'
  })

  ElMessage.success('数据已刷新')
}

const initTrendChart = () => {
  if (!efficiencyTrendRef.value) return

  trendChart = echarts.init(efficiencyTrendRef.value)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['T+1达成率', 'T+2达成率', '平均时效'],
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
    yAxis: [
      {
        type: 'value',
        min: 60,
        max: 100,
        axisLabel: { fontSize: 11, formatter: '{value}%' }
      },
      {
        type: 'value',
        min: 0.5,
        max: 2.5,
        axisLabel: { fontSize: 11, formatter: '{value}天' }
      }
    ],
    series: [
      {
        name: 'T+1达成率',
        data: [72.5, 74.2, 76.8, 75.5, 77.3, 78.5],
        type: 'line',
        smooth: true,
        itemStyle: { color: '#67c23a' }
      },
      {
        name: 'T+2达成率',
        data: [91.2, 92.5, 93.1, 93.8, 94.5, 94.2],
        type: 'line',
        smooth: true,
        itemStyle: { color: '#e6a23c' }
      },
      {
        name: '平均时效',
        data: [1.6, 1.5, 1.4, 1.45, 1.35, 1.3],
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        itemStyle: { color: '#409eff' },
        lineStyle: { type: 'dashed' }
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
.flow-page {
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
  height: 450px;
  border-radius: 4px;
  overflow: hidden;
}

.flow-legend-overlay {
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

.line {
  width: 30px;
  height: 3px;
  border-radius: 2px;
}

.line.green {
  background: #67c23a;
}

.line.yellow {
  background: #e6a23c;
}

.line.red {
  background: #f56c6c;
}

.flow-stats {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  height: 100%;
}

.flow-stats h4 {
  margin-bottom: 16px;
}

.efficiency-item {
  margin-bottom: 16px;
}

.efficiency-label {
  color: #666;
  font-size: 13px;
  margin-bottom: 4px;
}

.efficiency-value {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 6px;
}

.efficiency-value.positive {
  color: #67c23a;
}

.efficiency-value.negative {
  color: #f56c6c;
}
</style>
