<template>
  <div class="user-heatmap-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>用户热力图</span>
          <div class="header-actions">
            <el-select v-model="filters.gender" placeholder="性别" size="default" clearable style="width: 100px" @change="handleFilterChange">
              <el-option label="全部性别" value="" />
              <el-option label="男" value="male" />
              <el-option label="女" value="female" />
            </el-select>
            <el-select v-model="filters.age" placeholder="年龄段" size="default" clearable style="width: 110px" @change="handleFilterChange">
              <el-option label="全部年龄段" value="" />
              <el-option label="18-25岁" value="18-25" />
              <el-option label="26-35岁" value="26-35" />
              <el-option label="36-45岁" value="36-45" />
              <el-option label="46-55岁" value="46-55" />
              <el-option label="55岁以上" value="55+" />
            </el-select>
            <el-select v-model="filters.activity" placeholder="活跃度" size="default" clearable style="width: 100px" @change="handleFilterChange">
              <el-option label="全部活跃度" value="" />
              <el-option label="高活跃" value="high" />
              <el-option label="中活跃" value="medium" />
              <el-option label="低活跃" value="low" />
              <el-option label="沉默" value="silent" />
            </el-select>
            <el-select v-model="filters.consumption" placeholder="消费层级" size="default" clearable style="width: 100px" @change="handleFilterChange">
              <el-option label="全部消费层级" value="" />
              <el-option label="5A" value="5A" />
              <el-option label="4R" value="4R" />
              <el-option label="3R" value="3R" />
              <el-option label="2R" value="2R" />
              <el-option label="1R" value="1R" />
            </el-select>
            <el-select v-model="filters.loyalty" placeholder="忠诚度状态" size="default" clearable style="width: 110px" @change="handleFilterChange">
              <el-option label="全部忠诚度" value="" />
              <el-option label="忠实用户" value="loyal" />
              <el-option label="活跃会员" value="active" />
              <el-option label="普通用户" value="normal" />
              <el-option label="流失风险" value="risk" />
            </el-select>
            <el-select v-model="filters.preference" placeholder="饮酒偏好" size="default" clearable style="width: 110px" @change="handleFilterChange">
              <el-option label="全部饮酒偏好" value="" />
              <el-option label="白酒偏好" value="baijiu" />
              <el-option label="啤酒偏好" value="beer" />
              <el-option label="混饮偏好" value="mix" />
              <el-option label="无明显偏好" value="none" />
            </el-select>
            <el-button type="primary" @click="loadUserHeatData">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="20" style="display: flex">
        <el-col :span="16">
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
                <span class="label">用户规模:</span>
                <span class="value highlight">{{ stats.totalUsers.toLocaleString() }}</span>
              </div>
              <div class="info-item">
                <span class="label">高密度区域:</span>
                <span class="value">{{ stats.highDensity }}</span>
              </div>
              <div class="info-item">
                <span class="label">总覆盖率:</span>
                <span class="value">{{ stats.coverage }}%</span>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="8" class="right-panel">
          <el-card shadow="hover" class="stats-card">
            <template #header>用户规模统计</template>
            <div class="stats-overview">
              <div class="stat-item">
                <span class="stat-label">总用户数</span>
                <span class="stat-value">{{ stats.totalUsers.toLocaleString() }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">活跃用户</span>
                <span class="stat-value success">{{ stats.activeUsers.toLocaleString() }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">高密度路网</span>
                <span class="stat-value warning">{{ stats.highDensity }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">平均密度</span>
                <span class="stat-value">{{ stats.avgDensity }}</span>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="detail-card">
            <template #header>选中路网详情</template>
            <div v-if="selectedRoad" class="road-detail">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="路网名称">{{ selectedRoad.blockName }}</el-descriptions-item>
                <el-descriptions-item label="所属街道">{{ selectedRoad.street }}</el-descriptions-item>
                <el-descriptions-item label="用户数">
                  <span class="highlight">{{ selectedRoad.userCount.toLocaleString() }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="密度等级">
                  <el-tag :type="getDensityType(selectedRoad.density)" size="small">
                    {{ getDensityLabel(selectedRoad.density) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="多样性指数">{{ (selectedRoad.diversityIndex * 100).toFixed(1) }}%</el-descriptions-item>
              </el-descriptions>

              <el-divider>标签分布</el-divider>

              <div class="tag-section">
                <div class="tag-title">性别分布</div>
                <div class="icon-dist-row">
                  <div class="icon-dist-item">
                    <span class="icon-badge male">♂</span>
                    <span class="icon-label">男</span>
                    <div class="icon-bar"><div class="icon-bar-fill male" :style="{width: selectedRoad.tagDistribution.gender.male + '%'}"></div></div>
                    <span class="icon-pct">{{ selectedRoad.tagDistribution.gender.male }}%</span>
                  </div>
                  <div class="icon-dist-item">
                    <span class="icon-badge female">♀</span>
                    <span class="icon-label">女</span>
                    <div class="icon-bar"><div class="icon-bar-fill female" :style="{width: selectedRoad.tagDistribution.gender.female + '%'}"></div></div>
                    <span class="icon-pct">{{ selectedRoad.tagDistribution.gender.female }}%</span>
                  </div>
                </div>
              </div>

              <div class="tag-section">
                <div class="tag-title">年龄分布</div>
                <div class="icon-tags">
                  <div v-for="(val, key) in selectedRoad.tagDistribution.age" :key="key" class="icon-tag-chip">
                    <span class="chip-icon">👤</span>
                    <span class="chip-label">{{ key }}</span>
                    <span class="chip-pct">{{ val }}%</span>
                  </div>
                </div>
              </div>

              <div class="tag-section">
                <div class="tag-title">活跃度分布</div>
                <div class="icon-tags">
                  <div v-for="(val, key) in selectedRoad.tagDistribution.activity" :key="key" class="icon-tag-chip">
                    <span class="chip-icon">{{ getActivityIcon(key) }}</span>
                    <span class="chip-label">{{ getActivityLabel(key) }}</span>
                    <span class="chip-pct">{{ val }}%</span>
                  </div>
                </div>
              </div>

              <div class="tag-section">
                <div class="tag-title">消费层级分布</div>
                <div class="icon-tags">
                  <div v-for="(val, key) in selectedRoad.tagDistribution.consumption" :key="key" class="icon-tag-chip">
                    <span class="chip-icon">💎</span>
                    <span class="chip-label">{{ key }}</span>
                    <span class="chip-pct">{{ val }}%</span>
                  </div>
                </div>
              </div>

              <div class="tag-section">
                <div class="tag-title">忠诚度分布</div>
                <div class="icon-tags">
                  <div v-for="(val, key) in selectedRoad.tagDistribution.loyalty" :key="key" class="icon-tag-chip">
                    <span class="chip-icon">{{ getLoyaltyIcon(key) }}</span>
                    <span class="chip-label">{{ getLoyaltyLabel(key) }}</span>
                    <span class="chip-pct">{{ val }}%</span>
                  </div>
                </div>
              </div>

              <div class="tag-section">
                <div class="tag-title">饮酒偏好分布</div>
                <div class="icon-tags">
                  <div v-for="(val, key) in selectedRoad.tagDistribution.preference" :key="key" class="icon-tag-chip">
                    <span class="chip-icon">{{ getPreferenceIcon(key) }}</span>
                    <span class="chip-label">{{ getPreferenceLabel(key) }}</span>
                    <span class="chip-pct">{{ val }}%</span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="no-selection">
              <el-empty description="点击地图上的路网查看详情" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="hover" style="margin-top: 20px">
      <template #header>
        <span>用户群体分布排名</span>
        <el-button type="primary" size="small" style="float: right;" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出Excel
        </el-button>
      </template>
      <el-table :data="tableData" height="250" stripe @row-click="handleRowClick">
        <el-table-column prop="rank" label="排名" width="70" sortable>
          <template #default="{ row }">
            <span :class="['rank-badge', row.rank <= 3 ? 'top' : '']">{{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="blockName" label="路网名称" />
        <el-table-column prop="street" label="所属街道" width="120" />
        <el-table-column prop="userCount" label="用户数" width="100" sortable>
          <template #default="{ row }">
            {{ row.userCount.toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column prop="density" label="密度等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getDensityType(row.density)" size="small">
              {{ getDensityLabel(row.density) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="diversityIndex" label="标签多样性指数" width="140">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round(row.diversityIndex * 100)"
              :color="getDiversityColor(row.diversityIndex)"
              :stroke-width="10"
            />
          </template>
        </el-table-column>
        <el-table-column prop="coreTags" label="核心标签" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="tag in row.coreTags" :key="tag" size="small" style="margin-right: 4px;">
              {{ tag }}
            </el-tag>
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
const mapCenter = ref([104.094, 30.679])
const mapStyle = ref('streets')
const currentArea = ref('成都市成华区')
const roadLayerId = 'chenghua-road-network'

const filters = reactive({
  gender: '',
  age: '',
  activity: '',
  consumption: '',
  loyalty: '',
  preference: ''
})

const stats = reactive({
  totalUsers: 0,
  activeUsers: 0,
  highDensity: 0,
  coverage: 0,
  avgDensity: 0
})

const selectedRoad = ref(null)
const tableData = ref([])
let mapInstance = null
let originalGeoJSON = null

const handleMapLoad = (map) => {
  mapInstance = map
  loadRoadNetwork()
}

const loadRoadNetwork = async () => {
  try {
    const response = await fetch('/chenghua-district.geojson')
    const geojson = await response.json()
    originalGeoJSON = JSON.parse(JSON.stringify(geojson))

    geojson.features.forEach((feature, index) => {
      feature.properties.userCount = 0
      feature.properties.density = 'none'
      feature.properties.id = index
      feature.properties.colorValue = Math.random()
    })

    mapRef.value?.addRoadLayer(roadLayerId, geojson, {
      colorField: 'userCount',
      opacity: 0.7
    })

    loadUserHeatData()
    ElMessage.success('路网数据加载成功')
  } catch (e) {
    console.error('加载路网数据失败:', e)
    ElMessage.error('加载路网数据失败')
  }
}

const loadUserHeatData = () => {
  if (!mapInstance || !originalGeoJSON) return

  // 生成模拟用户数据
  const streets = ['府青路街道', '二仙桥街道', '跳蹬河街道', '双桥子街道', '万年场街道', '保和街道', '青龙街道', '龙潭街道']

  const mockData = []
  originalGeoJSON.features.forEach((feature, index) => {
    const baseCount = Math.floor(Math.random() * 1500 + 100)
    let userCount = baseCount

    // 根据筛选条件筛选（OR逻辑）
    if (filters.gender || filters.age || filters.activity || filters.consumption || filters.loyalty || filters.preference) {
      const matchConditions = []

      if (filters.gender) matchConditions.push(Math.random() > 0.4)
      if (filters.age) matchConditions.push(Math.random() > 0.5)
      if (filters.activity) {
        const activityProb = { high: 0.25, medium: 0.35, low: 0.25, silent: 0.15 }
        matchConditions.push(Math.random() < (activityProb[filters.activity] || 0.25))
      }
      if (filters.consumption) {
        const consumptionProb = { '5A': 0.08, '4R': 0.22, '3R': 0.35, '2R': 0.25, '1R': 0.10 }
        matchConditions.push(Math.random() < (consumptionProb[filters.consumption] || 0.25))
      }
      if (filters.loyalty) {
        const loyaltyProb = { loyal: 0.20, active: 0.30, normal: 0.35, risk: 0.15 }
        matchConditions.push(Math.random() < (loyaltyProb[filters.loyalty] || 0.25))
      }
      if (filters.preference) {
        const prefProb = { baijiu: 0.40, beer: 0.25, mix: 0.15, none: 0.20 }
        matchConditions.push(Math.random() < (prefProb[filters.preference] || 0.25))
      }

      // OR逻辑：满足任一条件即可
      if (!matchConditions.some(Boolean)) {
        userCount = Math.floor(userCount * 0.1)
      } else {
        userCount = Math.floor(userCount * (1 + Math.random() * 0.5))
      }
    }

    const density = userCount > 1000 ? 'high' : userCount > 500 ? 'medium' : userCount > 0 ? 'low' : 'none'

    feature.properties.userCount = userCount
    feature.properties.density = density
    feature.properties.street = streets[index % streets.length]
    // 随机色阶值，用于生成随机颜色
    feature.properties.colorValue = Math.random()

    const genderDistribution = { male: Math.floor(Math.random() * 30 + 45), female: 0 }
    genderDistribution.female = 100 - genderDistribution.male

    const ageDistribution = {
      '18-25': Math.floor(Math.random() * 20 + 5),
      '26-35': Math.floor(Math.random() * 25 + 20),
      '36-45': Math.floor(Math.random() * 20 + 20),
      '46-55': Math.floor(Math.random() * 15 + 10),
      '55+': Math.floor(Math.random() * 10 + 3)
    }
    const ageTotal = Object.values(ageDistribution).reduce((a, b) => a + b, 0)
    Object.keys(ageDistribution).forEach(k => {
      ageDistribution[k] = Math.round(ageDistribution[k] / ageTotal * 100)
    })

    const activityDistribution = {
      high: Math.floor(Math.random() * 20 + 15),
      medium: Math.floor(Math.random() * 20 + 25),
      low: Math.floor(Math.random() * 15 + 20),
      silent: 0
    }
    activityDistribution.silent = 100 - Object.values(activityDistribution).reduce((a, b) => a + b, 0)

    const consumptionDistribution = {
      '5A': Math.floor(Math.random() * 8 + 3),
      '4R': Math.floor(Math.random() * 15 + 15),
      '3R': Math.floor(Math.random() * 15 + 20),
      '2R': Math.floor(Math.random() * 15 + 15),
      '1R': 0
    }
    consumptionDistribution['1R'] = 100 - Object.values(consumptionDistribution).reduce((a, b) => a + b, 0)

    const loyaltyDistribution = {
      loyal: Math.floor(Math.random() * 15 + 10),
      active: Math.floor(Math.random() * 20 + 20),
      normal: Math.floor(Math.random() * 20 + 25),
      risk: 0
    }
    loyaltyDistribution.risk = 100 - Object.values(loyaltyDistribution).reduce((a, b) => a + b, 0)

    const preferenceDistribution = {
      baijiu: Math.floor(Math.random() * 25 + 30),
      beer: Math.floor(Math.random() * 15 + 15),
      mix: Math.floor(Math.random() * 10 + 10),
      none: 0
    }
    preferenceDistribution.none = 100 - Object.values(preferenceDistribution).reduce((a, b) => a + b, 0)

    mockData.push({
      id: index,
      blockName: feature.properties.名称 || `成华区四级路网${String(index + 1).padStart(3, '0')}`,
      street: feature.properties.street,
      userCount,
      density,
      diversityIndex: Math.round((Math.random() * 0.4 + 0.5) * 100) / 100,
      tagDistribution: {
        gender: genderDistribution,
        age: ageDistribution,
        activity: activityDistribution,
        consumption: consumptionDistribution,
        loyalty: loyaltyDistribution,
        preference: preferenceDistribution
      }
    })
  })

  // 更新地图 - 使用随机色阶
  mapRef.value?.removeLayer(roadLayerId)
  mapRef.value?.addRoadLayer(roadLayerId, originalGeoJSON, {
    colorField: 'colorValue',
    opacity: 0.7
  })

  // 计算统计
  const totalUsers = mockData.reduce((sum, item) => sum + item.userCount, 0)
  const activeUsers = mockData.filter(item => item.density !== 'low' && item.density !== 'none').length
  const highDensity = mockData.filter(item => item.density === 'high').length
  const avgDensity = mockData.length > 0 ? Math.round(totalUsers / mockData.filter(item => item.userCount > 0).length) : 0
  const coverage = Math.round((mockData.filter(item => item.userCount > 0).length / mockData.length) * 100)

  stats.totalUsers = totalUsers
  stats.activeUsers = activeUsers
  stats.highDensity = highDensity
  stats.avgDensity = avgDensity.toLocaleString()
  stats.coverage = coverage

  // 排序生成表格数据
  mockData.sort((a, b) => b.userCount - a.userCount)
  const rankedData = mockData.map((item, index) => ({
    ...item,
    rank: index + 1,
    coreTags: generateCoreTags(item)
  }))

  tableData.value = rankedData

  ElMessage.success('用户热力数据已刷新')
}

const generateCoreTags = (item) => {
  const tags = []

  const topAge = Object.entries(item.tagDistribution.age).sort((a, b) => b[1] - a[1])[0]
  if (topAge) tags.push(`${topAge[0]}为主`)

  if (item.tagDistribution.activity.high > 30) tags.push('高活跃')
  if (item.tagDistribution.loyalty.loyal > 25) tags.push('忠实用户')
  if (item.tagDistribution.consumption['5A'] > 10 || item.tagDistribution.consumption['4R'] > 30) tags.push('高消费')

  const topPref = Object.entries(item.tagDistribution.preference).sort((a, b) => b[1] - a[1])[0]
  if (topPref && topPref[1] > 40) {
    const prefLabels = { baijiu: '白酒偏好', beer: '啤酒偏好', mix: '混饮偏好', none: '无偏好' }
    tags.push(prefLabels[topPref[0]])
  }

  return tags.slice(0, 3)
}

const handleFilterChange = () => {
  loadUserHeatData()
}

const handleMapClick = (e) => {
  if (!e || !e.features || e.features.length === 0) {
    selectedRoad.value = null
    return
  }

  const feature = e.features[0]
  const props = feature.properties

  const index = props.id !== undefined ? props.id : tableData.value.findIndex(item => item.blockName === (props.名称 || props.name))

  if (index >= 0 && tableData.value[index]) {
    selectedRoad.value = tableData.value[index]
  }
}

const handleRowClick = (row) => {
  selectedRoad.value = row

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

const getDensityType = (density) => {
  const types = { high: 'danger', medium: 'warning', low: 'success', none: 'info' }
  return types[density] || 'info'
}

const getDensityLabel = (density) => {
  const labels = { high: '高密度', medium: '中密度', low: '低密度', none: '无数据' }
  return labels[density] || '未知'
}

const getActivityLabel = (key) => {
  const labels = { high: '高活跃', medium: '中活跃', low: '低活跃', silent: '沉默' }
  return labels[key] || key
}

const getActivityIcon = (key) => {
  const icons = { high: '⚡', medium: '📊', low: '🐢', silent: '💤' }
  return icons[key] || '📌'
}

const getLoyaltyIcon = (key) => {
  const icons = { loyal: '💛', active: '💙', normal: '🩶', risk: '🔴' }
  return icons[key] || '📌'
}

const getPreferenceIcon = (key) => {
  const icons = { baijiu: '🍶', beer: '🍺', mix: '🥂', none: '🚫' }
  return icons[key] || '📌'
}

const getActivityColor = (key) => {
  const colors = { high: '#67c23a', medium: '#409EFF', low: '#e6a23c', silent: '#909399' }
  return colors[key] || '#909399'
}

const getLoyaltyLabel = (key) => {
  const labels = { loyal: '忠实用户', active: '活跃会员', normal: '普通用户', risk: '流失风险' }
  return labels[key] || key
}

const getLoyaltyColor = (key) => {
  const colors = { loyal: '#67c23a', active: '#409EFF', normal: '#e6a23c', risk: '#f56c6c' }
  return colors[key] || '#909399'
}

const getPreferenceLabel = (key) => {
  const labels = { baijiu: '白酒偏好', beer: '啤酒偏好', mix: '混饮偏好', none: '无偏好' }
  return labels[key] || key
}

const getPreferenceColor = (key) => {
  const colors = { baijiu: '#f56c6c', beer: '#409EFF', mix: '#9c27b0', none: '#909399' }
  return colors[key] || '#909399'
}

const getDiversityColor = (index) => {
  if (index >= 0.7) return '#67c23a'
  if (index >= 0.5) return '#e6a23c'
  return '#f56c6c'
}
</script>

<style scoped>
.user-heatmap-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.map-wrapper {
  position: relative;
  height: 650px;
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
  color: #409EFF;
  font-size: 16px;
}

.stats-overview {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #666;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.warning {
  color: #e6a23c;
}

.road-detail {
  padding: 0 8px;
}

.highlight {
  color: #409EFF;
  font-weight: bold;
}

.el-divider {
  margin: 16px 0;
}

.tag-section {
  margin-bottom: 16px;
}

.tag-section:last-child {
  margin-bottom: 0;
}

.tag-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

/* 性别分布横向条 */
.icon-dist-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.icon-dist-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-badge {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  flex-shrink: 0;
}

.icon-badge.male {
  background: #e6f0ff;
  color: #409EFF;
}

.icon-badge.female {
  background: #fce6e6;
  color: #f56c6c;
}

.icon-label {
  font-size: 12px;
  color: #606266;
  width: 24px;
  flex-shrink: 0;
}

.icon-bar {
  flex: 1;
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.icon-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s;
}

.icon-bar-fill.male {
  background: linear-gradient(90deg, #74add1, #409EFF);
}

.icon-bar-fill.female {
  background: linear-gradient(90deg, #f46d43, #f56c6c);
}

.icon-pct {
  font-size: 12px;
  font-weight: bold;
  color: #303133;
  width: 36px;
  text-align: right;
  flex-shrink: 0;
}

/* 图标标签芯片 */
.icon-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.icon-tag-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #f5f7fa;
  border: 1px solid #e6e8eb;
  border-radius: 16px;
  padding: 4px 10px 4px 6px;
  transition: all 0.2s;
}

.icon-tag-chip:hover {
  border-color: #409EFF;
  background: #ecf5ff;
}

.chip-icon {
  font-size: 14px;
}

.chip-label {
  font-size: 11px;
  color: #606266;
}

.chip-pct {
  font-size: 12px;
  font-weight: bold;
  color: #303133;
}

.no-selection {
  padding: 40px 0;
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

.right-panel {
  display: flex;
  flex-direction: column;
  height: 650px;
  gap: 16px;
}

.stats-card {
  flex-shrink: 0;
}

.detail-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.detail-card :deep(.el-card__body) {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.road-detail {
  padding: 0;
}
</style>
