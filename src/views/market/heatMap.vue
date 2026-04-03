<template>
  <div class="heat-map-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>前端分布热力图</span>
          <div class="header-actions">
            <el-select v-model="filters.city" placeholder="选择城市" size="default" @change="handleFilterChange">
              <el-option label="全部城市" value="" />
              <el-option label="成都" value="chengdu" />
              <el-option label="重庆" value="chongqing" />
              <el-option label="武汉" value="wuhan" />
            </el-select>
            <el-select v-model="filters.category" placeholder="选择品类" size="default" @change="handleFilterChange">
              <el-option label="全部品类" value="" />
              <el-option label="白酒" value="baijiu" />
              <el-option label="啤酒" value="pijiu" />
              <el-option label="红酒" value="hongjiu" />
            </el-select>
            <el-select v-model="mapStyle" placeholder="地图风格" size="default" style="width: 120px;">
              <el-option label="街道图" value="streets" />
              <el-option label="卫星图" value="satellite" />
              <el-option label="深色图" value="dark" />
            </el-select>
            <el-button type="primary" @click="loadHeatMapData">刷新数据</el-button>
            <el-upload
              ref="roadUploadRef"
              :action="'/api/map/road/upload'"
              :headers="{ Authorization: 'Bearer ' + token }"
              :show-file-list="false"
              :before-upload="beforeRoadUpload"
              :on-success="handleRoadUploadSuccess"
              :on-error="handleRoadUploadError"
              accept=".json,.geojson"
            >
              <el-button type="success">上传路网数据</el-button>
            </el-upload>
          </div>
        </div>
      </template>

      <div class="map-wrapper">
        <GdsMap
          ref="mapRef"
          :center="mapCenter"
          :zoom="12"
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
            <span class="label">网点数量:</span>
            <span class="value highlight">{{ tableData.length }}</span>
          </div>
        </div>
      </div>

      <div class="legend">
        <div class="legend-title">热力图例</div>
        <div class="legend-items">
          <div class="legend-item">
            <span class="color-gradient"></span>
            <span>密度: 低 → 高</span>
          </div>
          <div class="legend-item">
            <span class="dot active"></span>
            <span>活跃网点</span>
          </div>
          <div class="legend-item">
            <span class="dot normal"></span>
            <span>一般网点</span>
          </div>
          <div class="legend-item">
            <span class="radius-icon"></span>
            <span>圆圈大小 = 销量规模</span>
          </div>
        </div>
        <div class="legend-road">
          <span class="road-item"><span class="road highway"></span>高速公路</span>
          <span class="road-item"><span class="road primary"></span>主干道</span>
          <span class="road-item"><span class="road secondary"></span>次干道</span>
          <span class="road-item"><span class="road tertiary"></span>支路</span>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <span>网点明细列表</span>
            <el-button type="primary" size="small" style="float: right;" @click="handleExport">导出Excel</el-button>
          </template>
          <el-table :data="tableData" height="300" stripe @row-click="handleRowClick">
            <el-table-column prop="name" label="网点名称" />
            <el-table-column prop="address" label="地址" />
            <el-table-column prop="category" label="品类" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.category }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="replenishRate" label="补货频次" width="100" sortable />
            <el-table-column prop="salesAmount" label="月销量" width="100" sortable>
              <template #default="{ row }">
                <span class="sales-value">{{ row.salesAmount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === '活跃' ? 'success' : 'info'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>区域统计</template>
          <div class="stats-panel">
            <div class="stat-item">
              <span class="label">网点总数</span>
              <span class="value">{{ stats.total }}</span>
            </div>
            <div class="stat-item">
              <span class="label">本月新增</span>
              <span class="value positive">+{{ stats.newThisMonth }}</span>
            </div>
            <div class="stat-item">
              <span class="label">活跃网点</span>
              <span class="value">{{ stats.active }}</span>
            </div>
            <div class="stat-item">
              <span class="label">覆盖率</span>
              <span class="value">{{ stats.coverage }}%</span>
            </div>
            <div class="stat-item">
              <span class="label">本月总销量</span>
              <span class="value highlight">{{ stats.totalSales }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 网点详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="网点详情" size="400px" direction="rtl">
      <div class="store-detail" v-if="selectedStore">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="网点名称">{{ selectedStore.name }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ selectedStore.address }}</el-descriptions-item>
          <el-descriptions-item label="品类">{{ selectedStore.category }}</el-descriptions-item>
          <el-descriptions-item label="补货频次">{{ selectedStore.replenishRate }}</el-descriptions-item>
          <el-descriptions-item label="月销量">{{ selectedStore.salesAmount }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedStore.status === '活跃' ? 'success' : 'info'">
              {{ selectedStore.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="坐标">
            {{ selectedStore.lng?.toFixed(4) }}, {{ selectedStore.lat?.toFixed(4) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import GdsMap from '@/components/GdsMap.vue'
import { uploadRoadNetwork } from '@/api/roadNetwork'

const mapRef = ref()
const roadUploadRef = ref()
const token = localStorage.getItem('token')
const roadLayerId = 'uploaded-road-network'
const mapCenter = ref([104.094, 30.679])
const mapStyle = ref('streets')
const currentArea = ref('成都市')

const filters = reactive({
  city: '',
  category: ''
})

const stats = reactive({
  total: '12,856',
  newThisMonth: '328',
  active: '8,642',
  coverage: '67.2',
  totalSales: '2.85亿'
})

const drawerVisible = ref(false)
const selectedStore = ref(null)

const tableData = ref([
  { name: '红旗超市(天府店)', address: '成都市天府大道中段88号', category: '白酒', replenishRate: '4.2次/月', salesAmount: '128万', status: '活跃', lng: 104.063, lat: 30.548, sales: 128 },
  { name: '永辉超市(高新店)', address: '成都市高新区天府二街', category: '啤酒', replenishRate: '3.8次/月', salesAmount: '96万', status: '活跃', lng: 104.071, lat: 30.552, sales: 96 },
  { name: '盒马鲜生(锦江店)', address: '成都市锦江区红星路', category: '红酒', replenishRate: '2.1次/月', salesAmount: '75万', status: '活跃', lng: 104.086, lat: 30.659, sales: 75 },
  { name: '沃尔玛(武侯店)', address: '成都市武侯区科华北路', category: '白酒', replenishRate: '1.5次/月', salesAmount: '58万', status: '一般', lng: 104.035, lat: 30.625, sales: 58 },
  { name: '家乐福(青羊店)', address: '成都市青羊区太升南路', category: '啤酒', replenishRate: '0.8次/月', salesAmount: '32万', status: '一般', lng: 104.053, lat: 30.671, sales: 32 },
  { name: '舞东风(玉林店)', address: '成都市武侯区玉林西路', category: '白酒', replenishRate: '3.5次/月', salesAmount: '89万', status: '活跃', lng: 104.052, lat: 30.641, sales: 89 },
  { name: '红旗超市(建设路)', address: '成都市成华区建设路', category: '啤酒', replenishRate: '2.8次/月', salesAmount: '67万', status: '活跃', lng: 104.103, lat: 30.665, sales: 67 },
  { name: '步步高(春熙路)', address: '成都市锦江区春熙路', category: '红酒', replenishRate: '1.9次/月', salesAmount: '45万', status: '活跃', lng: 104.091, lat: 30.658, sales: 45 }
])

let mapInstance = null

const handleMapLoad = (map) => {
  mapInstance = map
  loadHeatMapData()
  loadChenghuaDistricts()
}

const loadHeatMapData = () => {
  if (!mapInstance) return

  // 生成热力数据
  const heatData = []
  for (let i = 0; i < 200; i++) {
    const lng = 104.0 + Math.random() * 0.2
    const lat = 30.55 + Math.random() * 0.15
    const intensity = Math.random()
    heatData.push({ coordinates: [lng, lat], intensity })
  }

  // 添加热力图层
  mapRef.value?.addHeatLayer('storeHeat', heatData, {
    radius: 40,
    intensity: 1,
    opacity: 0.6
  })

  // 添加网点散点
  const scatterData = tableData.value.map(item => ({
    coordinates: [item.lng, item.lat],
    properties: {
      name: item.name,
      status: item.status,
      sales: item.sales
    }
  }))

  mapRef.value?.addScatterLayer('stores', scatterData, {
    radius: 10,
    color: (item) => item.properties?.status === '活跃' ? '#67c23a' : '#909399'
  })

  ElMessage.success('数据已刷新')
}

const handleFilterChange = () => {
  loadHeatMapData()
}

const handleMapClick = (e) => {
  console.log('Map clicked:', e)
}

const handleRowClick = (row) => {
  selectedStore.value = row
  drawerVisible.value = true

  // 飞转到该网点
  if (mapInstance) {
    mapRef.value?.flyTo({
      center: [row.lng, row.lat],
      zoom: 15,
      duration: 1000
    })
  }
}

const handleExport = () => {
  ElMessage.info('导出功能开发中...')
}

watch(mapStyle, () => {
  // 重新初始化地图
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
})

// 路网上传相关
const beforeRoadUpload = (file) => {
  const isJson = file.name.endsWith('.json') || file.name.endsWith('.geojson')
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isJson) ElMessage.error('只支持.json或.geojson文件')
  if (!isLt10M) ElMessage.error('文件大小不能超过10MB')
  return isJson && isLt10M
}

const handleRoadUploadSuccess = (response) => {
  // axios interceptor 已剥掉外层Result，直接返回data
  const rn = response
  mapRef.value?.addRoadLayer(roadLayerId, rn.geoJsonUrl)
  ElMessage.success(rn.message || '上传成功')
}

const handleRoadUploadError = () => {
  ElMessage.error('上传失败，请重试')
}

// 自动加载成华区片区数据作为示例（每个片区随机热力值）
const loadChenghuaDistricts = async () => {
  try {
    const response = await fetch('/chenghua-district.geojson')
    const geojson = await response.json()

    // 为每个片区随机生成热力值
    geojson.features.forEach(feature => {
      feature.properties.heatValue = Math.random()
    })

    // 使用热力色阶渲染（蓝-青-黄-橙-红）
    mapRef.value?.addRoadLayer(roadLayerId, geojson, {
      colorField: 'heatValue'
    })
  } catch (e) {
    console.error('加载片区数据失败:', e)
  }
}
</script>

<style scoped>
.heat-map-page {
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
  color: #409eff;
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
  background: linear-gradient(90deg, #313695, #74add1, #fdae61, #d73027);
  border-radius: 2px;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.dot.active {
  background: #67c23a;
  box-shadow: 0 0 4px #67c23a;
}

.dot.normal {
  background: #909399;
}

.radius-icon {
  width: 16px;
  height: 16px;
  border: 2px solid #409eff;
  border-radius: 50%;
  background: rgba(64, 158, 255, 0.2);
}

.legend-road {
  display: flex;
  gap: 20px;
  padding-top: 8px;
  border-top: 1px solid #e6e6e6;
}

.road-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #666;
}

.road {
  width: 24px;
  height: 3px;
  border-radius: 2px;
}

.road.highway {
  background: #ffcc00;
}

.road.primary {
  background: #ff9900;
}

.road.secondary {
  background: #cccccc;
}

.road.tertiary {
  background: #dddddd;
}

.stats-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.stat-item .label {
  color: #909399;
}

.stat-item .value {
  font-weight: bold;
  font-size: 18px;
}

.stat-item .value.positive {
  color: #67c23a;
}

.stat-item .value.highlight {
  color: #409eff;
}

.sales-value {
  color: #67c23a;
  font-weight: 500;
}

.store-detail {
  padding: 0 16px;
}
</style>
