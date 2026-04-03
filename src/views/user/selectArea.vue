<template>
  <div class="select-area-page">
    <el-card shadow="hover">
      <template #header>
        <span>产品推广选区模型 - ROI预测沙盘</span>
      </template>

      <el-row :gutter="20" style="height: 520px;">
        <el-col :span="5">
          <div class="config-panel">
            <h4>推广配置</h4>

            <el-form :model="config" label-position="top">
              <el-form-item label="产品名称">
                <el-select v-model="config.product" placeholder="选择产品" size="default">
                  <el-option label="绿荞啤酒" value="lvjiao" />
                  <el-option label="金荞白酒" value="jinqiao" />
                  <el-option label="新品A" value="newA" />
                </el-select>
              </el-form-item>

              <el-form-item label="目标价格带">
                <el-radio-group v-model="config.priceRange">
                  <el-radio label="high">高档</el-radio>
                  <el-radio label="mid">中档</el-radio>
                  <el-radio label="low">大众</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="目标人群">
                <el-checkbox-group v-model="config.targetCrowd">
                  <el-checkbox label="young">年轻人(18-30)</el-checkbox>
                  <el-checkbox label="middle">中年人(30-45)</el-checkbox>
                  <el-checkbox label="business">商务人士</el-checkbox>
                </el-checkbox-group>
              </el-form-item>

              <el-form-item label="预算金额(万元)">
                <el-input-number v-model="config.budget" :min="1" :max="1000" size="default" />
              </el-form-item>

              <el-button type="primary" style="width: 100%" @click="handleCalculate">
                计算推荐区域
              </el-button>
            </el-form>

            <div class="roi-preview" v-if="roiResult">
              <h4>ROI预测结果</h4>
              <div class="roi-item">
                <span>覆盖目标人群</span>
                <span class="value">{{ roiResult.targetPopulation }}万人</span>
              </div>
              <div class="roi-item">
                <span>预计铺市成功率</span>
                <span class="value">{{ roiResult.successRate }}%</span>
              </div>
              <div class="roi-item">
                <span>预计转化产出</span>
                <span class="value highlight">{{ roiResult.expectedOutput }}万元</span>
              </div>
              <div class="roi-item">
                <span>预期ROI</span>
                <span class="value positive">{{ roiResult.roi }}x</span>
              </div>
            </div>
          </div>
        </el-col>

        <el-col :span="12">
          <div class="map-wrapper">
            <GdsMap
              ref="mapRef"
              :center="[104.094, 30.679]"
              :zoom="12"
              @load="handleMapLoad"
            />
            <div class="bivariate-legend">
              <span>推广适合度: </span>
              <span class="legend-item"><span class="dot best"></span>最佳</span>
              <span class="legend-item"><span class="dot good"></span>良好</span>
              <span class="legend-item"><span class="dot normal"></span>一般</span>
            </div>
          </div>
        </el-col>

        <el-col :span="7">
          <div class="recommend-panel">
            <h4>推荐网格列表</h4>
            <div class="recommend-list">
              <div
                v-for="(grid, index) in recommendedGrids"
                :key="grid.code"
                class="recommend-item"
                :class="{ selected: selectedGrid === grid.code }"
                @click="handleSelectGrid(grid)"
              >
                <div class="rank">{{ index + 1 }}</div>
                <div class="info">
                  <div class="name">{{ grid.code }}</div>
                  <div class="street">{{ grid.street }}</div>
                </div>
                <div class="score">
                  <el-tag :type="grid.recommend ? 'success' : 'info'" size="small">
                    {{ grid.recommend ? '推荐' : '可选' }}
                  </el-tag>
                </div>
              </div>
            </div>
            <el-button type="primary" style="width: 100%; margin-top: 16px" @click="handleExport">
              导出作战清单
            </el-button>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>ROI预测趋势</template>
          <div ref="roiChartRef" style="height: 250px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>网格推荐分布</template>
          <div ref="gridDistRef" style="height: 250px;"></div>
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
const roiChartRef = ref()
const gridDistRef = ref()
let roiChart = null
let gridChart = null

const config = reactive({
  product: 'lvjiao',
  priceRange: 'mid',
  targetCrowd: ['young'],
  budget: 50
})

const roiResult = ref(null)
const selectedGrid = ref(null)

const recommendedGrids = ref([
  { code: 'G001', name: '天府大道南段', userDensity: 92, penetration: 15, recommend: true, coord: [104.08, 30.55] },
  { code: 'G002', name: '高新区科技园', userDensity: 88, penetration: 18, recommend: true, coord: [104.07, 30.56] },
  { code: 'G003', name: '武侯区玉林路', userDensity: 85, penetration: 20, recommend: true, coord: [104.05, 30.64] },
  { code: 'G004', name: '锦江区春熙路', userDensity: 95, penetration: 25, recommend: true, coord: [104.09, 30.66] },
  { code: 'G005', name: '成华区建设路', userDensity: 78, penetration: 22, recommend: false, coord: [104.10, 30.67] },
  { code: 'G006', name: '青羊区宽窄巷', userDensity: 72, penetration: 28, recommend: false, coord: [104.06, 30.68] }
])

const handleMapLoad = (map) => {
  loadChenghuaDistricts()
  loadRecommendationData()
}

// 加载成华区四级路网网格（按推广适合度着色）
const loadChenghuaDistricts = async () => {
  try {
    const response = await fetch('/chenghua-district.geojson')
    const geojson = await response.json()

    // 为每个片区随机分配推广适合度：最佳(0-33%)、良好(33-66%)、一般(66-100%)
    geojson.features.forEach(feature => {
      const rand = Math.random()
      if (rand < 0.33) {
        feature.properties.suitability = 'best'      // 最佳 - 绿色
      } else if (rand < 0.66) {
        feature.properties.suitability = 'good'      // 良好 - 蓝色
      } else {
        feature.properties.suitability = 'normal'   // 一般 - 灰色
      }
    })

    // 使用推广适合度着色
    mapRef.value?.addRoadLayer('chenghua-grids', geojson, {
      suitabilityField: 'suitability'
    })

    // 更新推荐网格列表，使用实际的片区编码和街道
    // 前3个为推荐，其余为可选
    recommendedGrids.value = geojson.features.slice(0, 20).map((feature, index) => {
      const centerPoint = feature.properties['中心点[内置]'] || '104.06,30.65'
      const [lng, lat] = centerPoint.split(',').map(Number)
      return {
        code: feature.properties['主键'] || `CH-${String(index + 1).padStart(3, '0')}`,
        street: feature.properties['街道[内置]'] || '未知街道',
        recommend: index < 3,  // 前3个为推荐
        coord: [lng, lat]
      }
    })
  } catch (e) {
    console.error('加载片区数据失败:', e)
  }
}

const loadRecommendationData = () => {
  if (!mapRef.value) return

  // 清理之前的散点图层
  mapRef.value?.removeLayer('recommendAreas')
  mapRef.value?.removeLayer('optionalAreas')
}

const handleCalculate = () => {
  roiResult.value = {
    targetPopulation: 128.5,
    successRate: 72.3,
    expectedOutput: 856,
    roi: 3.8
  }
  loadRecommendationData()
  ElMessage.success('ROI预测计算完成')
}

const handleSelectGrid = (grid) => {
  selectedGrid.value = grid.code
  mapRef.value?.flyTo({
    center: grid.coord,
    zoom: 14,
    duration: 1000
  })
  ElMessage.info(`已选中: ${grid.name}`)
}

const handleExport = () => {
  ElMessage.success('作战清单已导出')
}

const initRoiChart = () => {
  if (!roiChartRef.value) return

  roiChart = echarts.init(roiChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const budget = params[0].axisValue
        const roi = params[0].data
        const output = (budget * roi).toFixed(0)
        return `<strong>预算: ${budget}万</strong><br/>
                ROI: ${roi}x<br/>
                预期产出: ${output}万`
      }
    },
    grid: {
      left: 50,
      right: 20,
      top: 20,
      bottom: 30
    },
    xAxis: {
      type: 'category',
      data: ['10', '20', '30', '50', '80', '100'],
      name: '预算(万元)',
      nameLocation: 'middle',
      nameGap: 25,
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: 'ROI倍数',
      nameLocation: 'middle',
      nameGap: 35,
      axisLabel: { fontSize: 11 }
    },
    series: [{
      data: [2.8, 3.2, 3.5, 3.8, 3.5, 3.2],
      type: 'line',
      smooth: true,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(103, 194, 58, 0.4)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0.1)' }
          ]
        }
      },
      lineStyle: { color: '#67c23a', width: 3 },
      itemStyle: { color: '#67c23a' }
    }],
    markLine: {
      data: [{ xAxis: '50', name: '最优预算点' }],
      lineStyle: { color: '#f56c6c', type: 'dashed' }
    }
  }

  roiChart.setOption(option)
}

const initGridDistChart = () => {
  if (!gridDistRef.value) return

  gridChart = echarts.init(gridDistRef.value)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['推荐网格', '可选网格'],
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
      type: 'value',
      name: '用户密度',
      nameLocation: 'middle',
      nameGap: 25,
      axisLabel: { fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      name: '渗透率',
      nameLocation: 'middle',
      nameGap: 35,
      axisLabel: { fontSize: 10 }
    },
    series: [
      {
        name: '推荐网格',
        data: recommendedGrids.value.filter(g => g.recommend).map(g => [g.userDensity, g.penetration]),
        type: 'scatter',
        symbolSize: 15,
        itemStyle: { color: '#67c23a' },
        label: {
          show: true,
          position: 'right',
          formatter: (params) => params.dataIndex + 1,
          fontSize: 10,
          color: '#333'
        }
      },
      {
        name: '可选网格',
        data: recommendedGrids.value.filter(g => !g.recommend).map(g => [g.userDensity, g.penetration]),
        type: 'scatter',
        symbolSize: 12,
        itemStyle: { color: '#909399' }
      },
      {
        name: '推荐边界',
        type: 'line',
        data: [[60, 0], [60, 30], [100, 30], [100, 0]],
        lineStyle: { color: '#67c23a', type: 'dashed' },
        areaStyle: {
          color: 'rgba(103, 194, 58, 0.1)'
        }
      }
    ]
  }

  gridChart.setOption(option)
}

const handleResize = () => {
  roiChart?.resize()
  gridChart?.resize()
}

onMounted(() => {
  initRoiChart()
  initGridDistChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  roiChart?.dispose()
  gridChart?.dispose()
})
</script>

<style scoped>
.select-area-page {
  padding: 20px;
}

.config-panel {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.config-panel h4 {
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e6e6e6;
}

:deep(.el-form-item) {
  margin-bottom: 12px;
}

:deep(.el-radio-group),
:deep(.el-checkbox-group) {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.roi-preview {
  margin-top: 24px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
}

.roi-preview h4 {
  margin-bottom: 12px;
  color: #409eff;
}

.roi-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f5f7fa;
  font-size: 13px;
}

.roi-item .value {
  font-weight: bold;
}

.roi-item .value.highlight {
  color: #409eff;
  font-size: 16px;
}

.roi-item .value.positive {
  color: #67c23a;
  font-size: 18px;
}

.map-wrapper {
  position: relative;
  height: 100%;
  border-radius: 4px;
  overflow: hidden;
}

.bivariate-legend {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.95);
  padding: 8px 12px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  display: flex;
  gap: 12px;
  align-items: center;
  z-index: 1;
  font-size: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot.best {
  background: #67c23a;
}

.dot.good {
  background: #409eff;
}

.dot.normal {
  background: #cccccc;
}

.recommend-panel {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  height: 100%;
}

.recommend-panel h4 {
  margin-bottom: 16px;
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 360px;
  overflow-y: auto;
}

.recommend-item {
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

.recommend-item:hover {
  background: #e6f0ff;
}

.recommend-item.selected {
  border-color: #409eff;
  background: #e6f0ff;
}

.recommend-item .rank {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #67c23a;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: bold;
}

.recommend-item .info {
  flex: 1;
}

.recommend-item .name {
  font-weight: bold;
  font-size: 13px;
  margin-bottom: 2px;
}

.recommend-item .street {
  font-size: 11px;
  color: #909399;
}
</style>
