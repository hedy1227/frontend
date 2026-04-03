<template>
  <div class="core-atmosphere-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>核心氛围街区</span>
          <div class="header-actions">
            <el-select v-model="filters.atmosphereTag" placeholder="选择氛围标签" size="default" @change="handleFilterChange">
              <el-option label="全部氛围标签" value="" />
              <el-option label="货架贴" value="货架贴" />
              <el-option label="瓶身贴" value="瓶身贴" />
              <el-option label="瓶颈卡" value="瓶颈卡" />
              <el-option label="堆头装饰" value="堆头装饰" />
              <el-option label="组合陈列" value="组合陈列" />
            </el-select>
            <el-select v-model="filters.concentrationLevel" placeholder="浓度等级" size="default" @change="handleFilterChange">
              <el-option label="全部等级" value="" />
              <el-option label="高浓度" value="high" />
              <el-option label="中浓度" value="medium" />
              <el-option label="低浓度" value="low" />
            </el-select>
            <el-button type="primary" @click="loadAtmosphereData">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </div>
        </div>
      </template>

      <div class="map-wrapper">
        <GdsMap
          ref="mapRef"
          :center="mapCenter"
          :zoom="13"
          :style="mapStyle"
          @load="handleMapLoad"
          @click="handleMapClick"
        />
        <div class="map-overlay-info">
          <div class="info-item">
            <span class="label">当前区域:</span>
            <span class="value">{{ currentArea }}</span>
          </div>
          <div class="info-item">
            <span class="label">高浓度街区:</span>
            <span class="value highlight">{{ stats.highConcentration }}</span>
          </div>
          <div class="info-item">
            <span class="label">中浓度街区:</span>
            <span class="value">{{ stats.mediumConcentration }}</span>
          </div>
          <div class="info-item">
            <span class="label">低浓度街区:</span>
            <span class="value">{{ stats.lowConcentration }}</span>
          </div>
        </div>
      </div>

      <div class="legend">
        <div class="legend-title">氛围浓度色阶</div>
        <div class="legend-items">
          <div class="legend-item">
            <span class="color-gradient"></span>
            <span>浓度: 低 → 高</span>
          </div>
          <div class="legend-item">
            <span class="dot high"></span>
            <span>高浓度街区</span>
          </div>
          <div class="legend-item">
            <span class="dot medium"></span>
            <span>中浓度街区</span>
          </div>
          <div class="legend-item">
            <span class="dot low"></span>
            <span>低浓度街区</span>
          </div>
        </div>
        <div class="legend-tags">
          <span class="tag-item"><span class="tag-icon">📦</span>货架贴</span>
          <span class="tag-item"><span class="tag-icon">🍶</span>瓶身贴</span>
          <span class="tag-item"><span class="tag-icon">🏷️</span>瓶颈卡</span>
          <span class="tag-item"><span class="tag-icon">🗂️</span>堆头装饰</span>
          <span class="tag-item"><span class="tag-icon">📚</span>组合陈列</span>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <span>高浓度品牌氛围街区排名</span>
            <el-button type="primary" size="small" style="float: right;" @click="handleExport">
              <el-icon><Download /></el-icon>
              导出Excel
            </el-button>
          </template>
          <el-table :data="tableData" height="350" stripe @row-click="handleRowClick">
            <el-table-column prop="rank" label="排名" width="70" sortable>
              <template #default="{ row }">
                <span :class="['rank-badge', row.rank <= 3 ? 'top' : '']">{{ row.rank }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="blockName" label="街区名称" />
            <el-table-column prop="street" label="所属街道" width="120" />
            <el-table-column prop="atmosphereScore" label="氛围浓度指数" width="120" sortable>
              <template #default="{ row }">
                <el-progress
                  :percentage="row.atmosphereScore"
                  :color="getScoreColor(row.atmosphereScore)"
                  :stroke-width="10"
                />
              </template>
            </el-table-column>
            <el-table-column prop="mainTag" label="主氛围标签" width="100">
              <template #default="{ row }">
                <el-tag size="small" type="success">{{ row.mainTag }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="storeCount" label="氛围网点数" width="100" sortable />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>街区详情</template>
          <div v-if="selectedBlock" class="block-detail">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="街区名称">{{ selectedBlock.blockName }}</el-descriptions-item>
              <el-descriptions-item label="所属街道">{{ selectedBlock.street }}</el-descriptions-item>
              <el-descriptions-item label="氛围浓度指数">
                <el-progress
                  :percentage="selectedBlock.atmosphereScore"
                  :color="getScoreColor(selectedBlock.atmosphereScore)"
                  :stroke-width="8"
                />
              </el-descriptions-item>
              <el-descriptions-item label="主氛围标签">
                <el-tag size="small" type="success">{{ selectedBlock.mainTag }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="氛围网点数">{{ selectedBlock.storeCount }}</el-descriptions-item>
            </el-descriptions>

            <el-divider>氛围标签分布</el-divider>
            <div class="tag-distribution">
              <div v-for="item in selectedBlock.tagDistribution" :key="item.tag" class="tag-item">
                <span class="tag-name">{{ item.tag }}</span>
                <el-progress :percentage="item.percentage" :color="getScoreColor(item.percentage)" :stroke-width="6" />
              </div>
            </div>
          </div>
          <div v-else class="no-selection">
            <el-empty description="点击左侧列表查看街区详情" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import GdsMap from '@/components/GdsMap.vue'

const mapRef = ref()
const mapCenter = ref([104.094, 30.679])
const mapStyle = ref('streets')
const currentArea = ref('成都市成华区')
const roadLayerId = 'chenghua-road-network'

const filters = reactive({
  atmosphereTag: '',
  concentrationLevel: ''
})

const stats = reactive({
  highConcentration: 0,
  mediumConcentration: 0,
  lowConcentration: 0
})

const selectedBlock = ref(null)
const tableData = ref([])
let mapInstance = null

const handleMapLoad = (map) => {
  mapInstance = map
  loadRoadNetwork()
  loadAtmosphereData()
}

const loadRoadNetwork = async () => {
  try {
    const response = await fetch('/chenghua-district.geojson')
    const geojson = await response.json()

    // 为每个片区初始化氛围浓度（后续根据标签数据更新）
    geojson.features.forEach((feature, index) => {
      feature.properties.atmosphereScore = Math.random()
      feature.properties.id = index
    })

    // 使用热力色阶渲染
    mapRef.value?.addRoadLayer(roadLayerId, geojson, {
      colorField: 'atmosphereScore',
      opacity: 0.7
    })

    ElMessage.success('路网数据加载成功')
  } catch (e) {
    console.error('加载路网数据失败:', e)
    ElMessage.error('加载路网数据失败')
  }
}

const loadAtmosphereData = () => {
  if (!mapInstance) return

  // 模拟氛围数据生成
  const tags = ['货架贴', '瓶身贴', '瓶颈卡', '堆头装饰', '组合陈列']
  const streets = ['府青路街道', '二仙桥街道', '跳蹬河街道', '双桥子街道', '万年场街道', '保和街道', '青龙街道', '龙潭街道']

  // 生成模拟数据
  const mockData = []
  for (let i = 0; i < 20; i++) {
    const score = Math.floor(Math.random() * 40 + 60) // 60-100
    const mainTag = tags[Math.floor(Math.random() * tags.length)]
    const street = streets[Math.floor(Math.random() * streets.length)]

    mockData.push({
      rank: i + 1,
      blockName: `成华区核心街区${String(i + 1).padStart(2, '0')}`,
      street,
      atmosphereScore: score,
      mainTag,
      storeCount: Math.floor(Math.random() * 50 + 10),
      tagDistribution: tags.map(tag => ({
        tag,
        percentage: tag === mainTag ? Math.floor(Math.random() * 30 + 40) : Math.floor(Math.random() * 30)
      }))
    })
  }

  // 按氛围浓度排序
  mockData.sort((a, b) => b.atmosphereScore - a.atmosphereScore)
  mockData.forEach((item, index) => {
    item.rank = index + 1
  })

  tableData.value = mockData

  // 统计
  stats.highConcentration = mockData.filter(item => item.atmosphereScore >= 80).length
  stats.mediumConcentration = mockData.filter(item => item.atmosphereScore >= 60 && item.atmosphereScore < 80).length
  stats.lowConcentration = mockData.filter(item => item.atmosphereScore < 60).length

  // 更新地图上路网的氛围浓度
  updateRoadNetworkColors(mockData)

  ElMessage.success('氛围数据已刷新')
}

const updateRoadNetworkColors = (data) => {
  if (!mapInstance) return

  // 重新加载路网数据并根据新数据设置颜色
  const geojson = mapInstance.getSource(roadLayerId)?._data
  if (geojson) {
    data.forEach((item, index) => {
      if (geojson.features[index]) {
        geojson.features[index].properties.atmosphereScore = item.atmosphereScore / 100
      }
    })
    mapRef.value?.removeLayer(roadLayerId)
    mapRef.value?.addRoadLayer(roadLayerId, geojson, {
      colorField: 'atmosphereScore',
      opacity: 0.7
    })
  }
}

const handleFilterChange = () => {
  loadAtmosphereData()
}

const handleMapClick = (e) => {
  console.log('Map clicked:', e)
}

const handleRowClick = (row) => {
  selectedBlock.value = row

  // 飞转到该街区（基于街道中心点估算）
  const streetCenters = {
    '府青路街道': [104.093, 30.678],
    '二仙桥街道': [104.101, 30.685],
    '跳蹬河街道': [104.105, 30.695],
    '双桥子街道': [104.095, 30.700],
    '万年场街道': [104.108, 30.688],
    '保和街道': [104.115, 30.682],
    '青龙街道': [104.085, 30.690],
    '龙潭街道': [104.120, 30.675]
  }

  const center = streetCenters[row.street] || [104.094, 30.679]
  mapRef.value?.flyTo({
    center,
    zoom: 15,
    duration: 1000
  })
}

const handleExport = () => {
  ElMessage.info('导出功能开发中...')
}

const getScoreColor = (percentage) => {
  if (percentage >= 80) return '#f56c6c'
  if (percentage >= 60) return '#e6a23c'
  return '#67c23a'
}
</script>

<style scoped>
.core-atmosphere-page {
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
  height: 500px;
  border-radius: 4px;
  overflow: hidden;
}

.map-overlay-info {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.95);
  padding: 12px 16px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-item .label {
  color: #666;
  font-size: 12px;
}

.info-item .value {
  font-weight: bold;
}

.info-item .value.highlight {
  color: #f56c6c;
  font-size: 16px;
}

.legend {
  margin-top: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.legend-title {
  font-weight: bold;
  margin-bottom: 8px;
}

.legend-items {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.color-gradient {
  width: 80px;
  height: 12px;
  background: linear-gradient(90deg, #313695, #74add1, #fdae61, #f46d43, #d73027);
  border-radius: 2px;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.dot.high {
  background: #f56c6c;
}

.dot.medium {
  background: #e6a23c;
}

.dot.low {
  background: #67c23a;
}

.legend-tags {
  display: flex;
  gap: 16px;
  padding-top: 8px;
  border-top: 1px solid #e6e6e6;
  flex-wrap: wrap;
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
}

.tag-icon {
  font-size: 14px;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background: #e6e6e6;
  color: #666;
  font-weight: bold;
  font-size: 12px;
}

.rank-badge.top {
  background: #f56c6c;
  color: #fff;
}

.block-detail {
  padding: 0 8px;
}

.tag-distribution {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tag-distribution .tag-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tag-distribution .tag-name {
  font-size: 12px;
  color: #666;
}

.no-selection {
  padding: 40px 0;
}
</style>
