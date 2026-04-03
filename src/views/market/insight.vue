<template>
  <div class="insight-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>空白网点挖掘 - 双色差值分析</span>
          <div class="header-actions">
            <el-select v-model="filters.city" placeholder="选择城市" size="default" @change="handleFilterChange">
              <el-option label="全部城市" value="" />
              <el-option label="成都" value="chengdu" />
              <el-option label="重庆" value="chongqing" />
            </el-select>
            <el-select v-model="filters.type" placeholder="选择类型" size="default" @change="handleFilterChange">
              <el-option label="全部类型" value="" />
              <el-option label="餐饮" value="catering" />
              <el-option label="商超" value="supermarket" />
              <el-option label="便利店" value="convenience" />
            </el-select>
            <el-button type="primary" @click="loadData">刷新数据</el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="16">
          <div class="map-wrapper">
            <GdsMap
              ref="mapRef"
              :center="[104.094, 30.679]"
              :zoom="12"
              @load="handleMapLoad"
            />
            <div class="layer-control">
              <el-checkbox v-model="showUserLayer" label="用户分布热力(红)" @change="handleLayerChange" />
              <el-checkbox v-model="showStoreLayer" label="前端网点热力(蓝)" @change="handleLayerChange" />
              <el-checkbox v-model="showGapLayer" label="空白机会区(紫)" @change="handleLayerChange" />
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="legend-panel">
            <h4>双色差值图例</h4>
            <div class="bivariate-legend">
              <div class="legend-grid">
                <div class="cell" style="background: #f56c6c;"></div>
                <div class="cell" style="background: #9933ff;"></div>
                <div class="cell" style="background: #409eff;"></div>
                <div class="cell" style="background: #f5f7fa;"></div>
              </div>
              <p>红:用户密度 / 蓝:网点密度</p>
              <div class="gap-explanation">
                <div class="gap-item">
                  <span class="dot red"></span>
                  <span>用户多，网点少 = 空白机会</span>
                </div>
                <div class="gap-item">
                  <span class="dot blue"></span>
                  <span>用户少，网点多 = 竞争激烈</span>
                </div>
                <div class="gap-item">
                  <span class="dot purple"></span>
                  <span>双重高密度 = 核心区域</span>
                </div>
              </div>
            </div>
          </div>

          <div class="stats-panel" style="margin-top: 16px;">
            <h4>分析统计</h4>
            <div class="stat-row">
              <span class="label">空白机会区</span>
              <span class="value highlight">23个</span>
            </div>
            <div class="stat-row">
              <span class="label">覆盖人口</span>
              <span class="value">128万</span>
            </div>
            <div class="stat-row">
              <span class="label">潜在增量</span>
              <span class="value positive">+3500万/月</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="hover" style="margin-top: 20px">
      <template #header>空白机会区列表</template>
      <el-table :data="opportunityList" height="280">
        <el-table-column prop="gridCode" label="网格编码" width="120" />
        <el-table-column prop="location" label="位置描述" />
        <el-table-column prop="userDensity" label="用户密度" width="100" sortable>
          <template #default="{ row }">
            <div class="density-bar">
              <div class="bar-fill red" :style="{ width: row.userDensity + '%' }"></div>
              <span>{{ row.userDensity }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="storeDensity" label="网点密度" width="100" sortable>
          <template #default="{ row }">
            <div class="density-bar">
              <div class="bar-fill blue" :style="{ width: row.storeDensity + '%' }"></div>
              <span>{{ row.storeDensity }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="gapScore" label="空白指数" width="100" sortable>
          <template #default="{ row }">
            <el-tag :type="row.gapScore === '高' ? 'danger' : row.gapScore === '中' ? 'warning' : 'info'">
              {{ row.gapScore }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="potential" label="潜力评估" width="120">
          <template #default="{ row }">
            <el-rate v-model="row.potential" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleNavigate(row)">
              规划路线
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import GdsMap from '@/components/GdsMap.vue'

const mapRef = ref()

const filters = reactive({
  city: '',
  type: ''
})

const showUserLayer = ref(true)
const showStoreLayer = ref(true)
const showGapLayer = ref(true)

const opportunityList = ref([
  { gridCode: 'H38A21', location: '成都市武侯区-玉林小区', userDensity: 89, storeDensity: 12, gapScore: '高', potential: 5 },
  { gridCode: 'H38A22', location: '成都市高新区-孵化园', userDensity: 85, storeDensity: 15, gapScore: '高', potential: 5 },
  { gridCode: 'H38B15', location: '重庆市渝北区-新南路', userDensity: 78, storeDensity: 18, gapScore: '高', potential: 4 },
  { gridCode: 'H39C08', location: '武汉市洪山区-光谷广场', userDensity: 92, storeDensity: 22, gapScore: '中', potential: 4 },
  { gridCode: 'H39C12', location: '武汉市武昌区-街道口', userDensity: 76, storeDensity: 25, gapScore: '中', potential: 3 },
  { gridCode: 'H37D05', location: '西安市雁塔区-小寨', userDensity: 82, storeDensity: 28, gapScore: '中', potential: 3 },
  { gridCode: 'H36E11', location: '长沙市岳麓区-大学城', userDensity: 88, storeDensity: 20, gapScore: '高', potential: 4 }
])

let mapInstance = null

const handleMapLoad = (map) => {
  mapInstance = map
  loadData()
}

const loadData = () => {
  if (!mapInstance) return

  // 清理旧图层
  mapRef.value?.removeLayer('userHeat')
  mapRef.value?.removeLayer('storeHeat')
  mapRef.value?.removeLayer('gapScatter')
  mapRef.value?.removeLayer('stores')
  mapRef.value?.removeLayer('gapPoints')

  if (showUserLayer.value) {
    // 用户热力数据 - 红色
    const userData = []
    for (let i = 0; i < 100; i++) {
      userData.push({
        coordinates: [
          104.0 + Math.random() * 0.2,
          30.55 + Math.random() * 0.15
        ],
        intensity: 0.3 + Math.random() * 0.7
      })
    }
    mapRef.value?.addHeatLayer('userHeat', userData, {
      radius: 30,
      intensity: 1,
      opacity: 0.7
    })
  }

  if (showStoreLayer.value) {
    // 网点热力数据 - 蓝色
    const storeData = []
    for (let i = 0; i < 80; i++) {
      storeData.push({
        coordinates: [
          104.0 + Math.random() * 0.2,
          30.55 + Math.random() * 0.15
        ],
        intensity: 0.3 + Math.random() * 0.7
      })
    }
    mapRef.value?.addHeatLayer('storeHeat', storeData, {
      radius: 25,
      intensity: 0.8,
      opacity: 0.6
    })

    // 网点散点
    const scatterData = [
      { coordinates: [104.063, 30.548], properties: { name: '红旗超市' } },
      { coordinates: [104.071, 30.552], properties: { name: '永辉超市' } },
      { coordinates: [104.086, 30.659], properties: { name: '盒马鲜生' } }
    ]
    mapRef.value?.addScatterLayer('stores', scatterData, {
      radius: 8,
      color: '#409eff'
    })
  }

  if (showGapLayer.value) {
    // 空白机会区散点 - 紫色
    const gapData = opportunityList.value
      .filter(item => item.gapScore === '高')
      .map(item => ({
        coordinates: [
          104.0 + Math.random() * 0.15,
          30.55 + Math.random() * 0.12
        ],
        properties: { name: item.location, score: item.gapScore }
      }))

    mapRef.value?.addScatterLayer('gapPoints', gapData, {
      radius: 12,
      color: '#9933ff'
    })
  }

  ElMessage.success('数据已刷新')
}

const handleFilterChange = () => {
  loadData()
}

const handleLayerChange = () => {
  loadData()
}

const handleNavigate = (row) => {
  ElMessage.info(`正在规划路线至: ${row.location}`)
}
</script>

<style scoped>
.insight-page {
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

.layer-control {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-panel {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  height: 100%;
}

.legend-panel h4 {
  margin-bottom: 12px;
}

.bivariate-legend {
  text-align: center;
}

.legend-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  width: 150px;
  margin: 0 auto 8px;
}

.cell {
  width: 100%;
  height: 30px;
}

.bivariate-legend p {
  font-size: 11px;
  color: #666;
  margin-bottom: 12px;
}

.gap-explanation {
  text-align: left;
}

.gap-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.dot.red {
  background: #f56c6c;
}

.dot.blue {
  background: #409eff;
}

.dot.purple {
  background: #9933ff;
}

.stats-panel {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.stats-panel h4 {
  margin-bottom: 12px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #e6e6e6;
}

.stat-row .label {
  color: #666;
}

.stat-row .value {
  font-weight: bold;
}

.stat-row .value.highlight {
  color: #9933ff;
}

.stat-row .value.positive {
  color: #67c23a;
}

.density-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bar-fill {
  height: 8px;
  border-radius: 4px;
  transition: width 0.3s;
}

.bar-fill.red {
  background: linear-gradient(90deg, #f56c6c, #f78909);
}

.bar-fill.blue {
  background: linear-gradient(90deg, #409eff, #1866d9);
}
</style>
