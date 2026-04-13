<template>
  <div class="select-area-page">
    <el-card shadow="hover">
      <template #header>
        <span>产品推广选区</span>
      </template>

      <el-row :gutter="20" style="height: 520px;">
        <el-col :span="5">
          <div class="config-panel">
            <h4>推广配置</h4>

            <el-form :model="config" label-position="top">
              <!-- 主题方案选择 -->
              <el-form-item label="主题方案">
                <el-select v-model="config.theme" placeholder="选择主题方案" size="default" clearable @change="handleThemeChange">
                  <el-option label="毛铺草本酒（绿荞）" value="lüqiao" />
                  <el-option label="金标劲酒" value="jinbiao" />
                </el-select>
              </el-form-item>

              <!-- 自定义主题配置 -->
              <div class="custom-config" v-if="config.theme">
                <h5>自定义主题配置</h5>
                <el-form-item label="用户消费价格带">
                  <el-select v-model="config.priceRange" placeholder="选择价格带" size="default">
                    <el-option label="0-20元" value="0-20" />
                    <el-option label="20-50元" value="20-50" />
                    <el-option label="50-100元" value="50-100" />
                    <el-option label="100元以上" value="100+" />
                  </el-select>
                </el-form-item>

                <el-form-item label="目标人群密度">
                  <el-select v-model="config.density" placeholder="选择密度" size="default">
                    <el-option label="高密度" value="high" />
                    <el-option label="中密度" value="medium" />
                    <el-option label="低密度" value="low" />
                  </el-select>
                </el-form-item>

                <el-form-item label="产品偏好">
                  <el-select v-model="config.preference" placeholder="选择偏好" size="default">
                    <el-option label="劲酒" value="jinjiu" />
                    <el-option label="毛铺" value="maopu" />
                    <el-option label="椰岛" value="yedao" />
                    <el-option label="无明显偏好" value="none" />
                  </el-select>
                </el-form-item>
              </div>

              <!-- 不选择主题时显示完整配置 -->
              <div class="full-config" v-else>
                <el-form-item label="用户消费价格带">
                  <el-select v-model="config.priceRange" placeholder="选择价格带" size="default">
                    <el-option label="0-20元" value="0-20" />
                    <el-option label="20-50元" value="20-50" />
                    <el-option label="50-100元" value="50-100" />
                    <el-option label="100元以上" value="100+" />
                  </el-select>
                </el-form-item>

                <el-form-item label="目标人群密度">
                  <el-select v-model="config.density" placeholder="选择密度" size="default">
                    <el-option label="高密度" value="high" />
                    <el-option label="中密度" value="medium" />
                    <el-option label="低密度" value="low" />
                  </el-select>
                </el-form-item>

                <el-form-item label="产品偏好">
                  <el-select v-model="config.preference" placeholder="选择偏好" size="default">
                    <el-option label="劲酒" value="jinjiu" />
                    <el-option label="毛铺" value="maopu" />
                    <el-option label="椰岛" value="yedao" />
                    <el-option label="无明显偏好" value="none" />
                  </el-select>
                </el-form-item>
              </div>

              <div class="config-actions">
                <el-button type="primary" size="default" @click="handleCalculate">
                  计算推荐区域
                </el-button>
                <el-button size="default" @click="handleSaveConfig">
                  保存配置
                </el-button>
              </div>
            </el-form>

            <!-- 已保存配置列表 -->
            <div class="saved-configs" v-if="savedConfigs.length > 0">
              <h5>已保存配置</h5>
              <div class="config-list">
                <div
                  v-for="(item, index) in savedConfigs"
                  :key="index"
                  class="config-item"
                  @click="loadConfig(item)"
                >
                  <span class="config-name">{{ item.name }}</span>
                  <el-tag v-if="item.theme" size="small" type="warning">{{ getThemeLabel(item.theme) }}</el-tag>
                  <el-tag v-else size="small" type="info">{{ item.priceRange }}</el-tag>
                  <el-icon class="delete-icon" @click.stop="deleteConfig(index)"><Delete /></el-icon>
                </div>
              </div>
            </div>
          </div>
        </el-col>

        <el-col :span="12">
          <div class="map-wrapper">
            <GdsMap
              ref="mapRef"
              :center="[104.10, 30.66]"
              :zoom="10"
              @load="handleMapLoad"
              @click="handleMapClick"
            />
            <div class="map-legend">
              <span class="legend-title">区县图例</span>
              <span class="legend-item"><span class="dot recommended"></span>推荐区县</span>
              <span class="legend-item"><span class="dot normal"></span>可选区县</span>
            </div>
          </div>
        </el-col>

        <el-col :span="7">
          <div class="right-panels">
            <!-- 推荐区县列表 -->
            <div class="recommend-panel">
              <div class="panel-header">
                <h4>推荐区县列表</h4>
                <el-button type="success" size="small" @click="handleExportGrid">
                  <el-icon><Download /></el-icon>
                  导出
                </el-button>
              </div>
              <div class="recommend-list">
                <el-table :data="recommendedGrids" stripe size="small" max-height="180" @row-click="handleSelectGrid">
                  <el-table-column prop="rank" label="排名" width="55" />
                  <el-table-column prop="code" label="区县编码" width="90" />
                  <el-table-column prop="street" label="区县名称" />
                  <el-table-column prop="userCount" label="用户数" width="60" />
                  <el-table-column label="推荐" width="55">
                    <template #default="{ row }">
                      <el-tag v-if="row.recommended" type="danger" size="small">推荐</el-tag>
                      <span v-else style="color: #909399; font-size: 12px;">-</span>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>

            <!-- 区县内前端门店清单 -->
            <div class="store-panel">
              <div class="panel-header">
                <h4>区县内前端门店清单</h4>
                <el-button type="success" size="small" @click="handleExportStore">
                  <el-icon><Download /></el-icon>
                  导出
                </el-button>
              </div>
              <div class="store-list">
                <el-table :data="gridStores" stripe size="small" max-height="200" empty-text="请选择一个区县查看门店">
                  <el-table-column prop="name" label="门店名称" min-width="140" show-overflow-tooltip />
                  <el-table-column prop="category" label="品类" width="70" />
                  <el-table-column prop="address" label="地址" min-width="120" show-overflow-tooltip />
                </el-table>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 保存配置弹窗 -->
    <el-dialog v-model="saveDialogVisible" title="保存推广配置" width="400px">
      <el-form :model="saveForm" label-position="top">
        <el-form-item label="配置名称">
          <el-input v-model="saveForm.name" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="配置说明">
          <el-input v-model="saveForm.description" type="textarea" placeholder="请输入配置说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSaveConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Delete } from '@element-plus/icons-vue'
import GdsMap from '@/components/GdsMap.vue'

const mapRef = ref()
const mapInstance = shallowRef(null)
const gridGeoJSON = ref(null)

const config = reactive({
  theme: '',
  priceRange: '',
  density: '',
  preference: ''
})

const savedConfigs = ref([])
const recommendedGrids = ref([])
const selectedGridCode = ref(null)
const gridStores = ref([])

const saveDialogVisible = ref(false)
const saveForm = reactive({
  name: '',
  description: ''
})

// 加载保存的配置
const loadSavedConfigs = () => {
  const saved = localStorage.getItem('promotionConfigs')
  if (saved) {
    savedConfigs.value = JSON.parse(saved)
  }
}

// 获取主题方案标签
const getThemeLabel = (theme) => {
  const themeMap = {
    'lüqiao': '绿荞',
    'jinbiao': '金标'
  }
  return themeMap[theme] || theme
}

// 主题方案切换
const handleThemeChange = (theme) => {
  if (theme) {
    // 根据主题预设配置
    if (theme === 'lüqiao') {
      // 毛铺草本酒（绿荞）- 预设配置
      config.priceRange = '20-50'
      config.density = 'high'
      config.preference = 'maopu'
      ElMessage.success('已加载：毛铺草本酒（绿荞）推广配置')
    } else if (theme === 'jinbiao') {
      // 金标劲酒 - 预设配置
      config.priceRange = '50-100'
      config.density = 'medium'
      config.preference = 'jinjiu'
      ElMessage.success('已加载：金标劲酒推广配置')
    }
  }
}

const handleSaveConfig = () => {
  if (!config.priceRange || !config.density || !config.preference) {
    ElMessage.warning('请先填写完整的配置条件')
    return
  }
  saveDialogVisible.value = true
}

const confirmSaveConfig = () => {
  if (!saveForm.name) {
    ElMessage.warning('请输入配置名称')
    return
  }

  const newConfig = {
    name: saveForm.name,
    description: saveForm.description,
    theme: config.theme,
    priceRange: config.priceRange,
    density: config.density,
    preference: config.preference,
    createTime: new Date().toLocaleString()
  }

  savedConfigs.value.push(newConfig)
  localStorage.setItem('promotionConfigs', JSON.stringify(savedConfigs.value))

  saveDialogVisible.value = false
  saveForm.name = ''
  saveForm.description = ''
  ElMessage.success('配置已保存')
}

const loadConfig = (item) => {
  config.theme = item.theme || ''
  config.priceRange = item.priceRange
  config.density = item.density
  config.preference = item.preference
  ElMessage.success(`已加载配置: ${item.name}`)
}

const deleteConfig = (index) => {
  savedConfigs.value.splice(index, 1)
  localStorage.setItem('promotionConfigs', JSON.stringify(savedConfigs.value))
  ElMessage.success('配置已删除')
}

const handleMapLoad = async (map) => {
  mapInstance.value = map
  await loadGridData()
}

// 成都市区县边界模拟数据（区县级别的片区）
const districtGeoJSON = {
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": { "name": "锦江区", "code": "510104", "center": "104.086,30.658" },
      "geometry": { "type": "Polygon", "coordinates": [[[104.06, 30.62], [104.11, 30.62], [104.11, 30.70], [104.06, 30.70], [104.06, 30.62]]] }
    },
    {
      "type": "Feature",
      "properties": { "name": "青羊区", "code": "510105", "center": "104.043,30.680" },
      "geometry": { "type": "Polygon", "coordinates": [[[104.02, 30.65], [104.06, 30.65], [104.06, 30.71], [104.02, 30.71], [104.02, 30.65]]] }
    },
    {
      "type": "Feature",
      "properties": { "name": "金牛区", "code": "510106", "center": "104.028,30.706" },
      "geometry": { "type": "Polygon", "coordinates": [[[104.00, 30.68], [104.05, 30.68], [104.05, 30.74], [104.00, 30.74], [104.00, 30.68]]] }
    },
    {
      "type": "Feature",
      "properties": { "name": "武侯区", "code": "510107", "center": "104.046,30.628" },
      "geometry": { "type": "Polygon", "coordinates": [[[104.02, 30.60], [104.07, 30.60], [104.07, 30.65], [104.02, 30.65], [104.02, 30.60]]] }
    },
    {
      "type": "Feature",
      "properties": { "name": "成华区", "code": "510108", "center": "104.095,30.684" },
      "geometry": { "type": "Polygon", "coordinates": [[[104.07, 30.66], [104.12, 30.66], [104.12, 30.72], [104.07, 30.72], [104.07, 30.66]]] }
    },
    {
      "type": "Feature",
      "properties": { "name": "龙泉驿区", "code": "510112", "center": "104.248,30.560" },
      "geometry": { "type": "Polygon", "coordinates": [[[104.18, 30.50], [104.32, 30.50], [104.32, 30.62], [104.18, 30.62], [104.18, 30.50]]] }
    },
    {
      "type": "Feature",
      "properties": { "name": "青白江区", "code": "510113", "center": "104.232,30.882" },
      "geometry": { "type": "Polygon", "coordinates": [[[104.18, 30.84], [104.28, 30.84], [104.28, 30.92], [104.18, 30.92], [104.18, 30.84]]] }
    },
    {
      "type": "Feature",
      "properties": { "name": "新都区", "code": "510114", "center": "104.158,30.818" },
      "geometry": { "type": "Polygon", "coordinates": [[[104.10, 30.78], [104.22, 30.78], [104.22, 30.86], [104.10, 30.86], [104.10, 30.78]]] }
    },
    {
      "type": "Feature",
      "properties": { "name": "温江区", "code": "510115", "center": "103.848,30.700" },
      "geometry": { "type": "Polygon", "coordinates": [[[103.78, 30.66], [103.92, 30.66], [103.92, 30.74], [103.78, 30.74], [103.78, 30.66]]] }
    },
    {
      "type": "Feature",
      "properties": { "name": "双流区", "code": "510116", "center": "103.922,30.548" },
      "geometry": { "type": "Polygon", "coordinates": [[[103.84, 30.48], [104.00, 30.48], [104.00, 30.60], [103.84, 30.60], [103.84, 30.48]]] }
    },
    {
      "type": "Feature",
      "properties": { "name": "郫都区", "code": "510117", "center": "103.898,30.806" },
      "geometry": { "type": "Polygon", "coordinates": [[[103.82, 30.76], [103.98, 30.76], [103.98, 30.86], [103.82, 30.86], [103.82, 30.76]]] }
    },
    {
      "type": "Feature",
      "properties": { "name": "天府新区", "code": "510186", "center": "104.158,30.458" },
      "geometry": { "type": "Polygon", "coordinates": [[[104.08, 30.38], [104.24, 30.38], [104.24, 30.54], [104.08, 30.54], [104.08, 30.38]]] }
    }
  ]
}

const loadGridData = async () => {
  try {
    // 使用区县级别数据
    gridGeoJSON.value = districtGeoJSON

    // 生成推荐区县数据
    recommendedGrids.value = districtGeoJSON.features.map((feature, index) => {
      const [lng, lat] = feature.properties.center.split(',').map(Number)
      return {
        rank: index + 1,
        code: feature.properties.code,
        street: feature.properties.name,
        userCount: Math.floor(Math.random() * 2000 + 500),
        density: ['high', 'medium', 'low'][Math.floor(Math.random() * 3)],
        coord: [lng, lat]
      }
    })

    // 按用户数排序
    recommendedGrids.value.sort((a, b) => b.userCount - a.userCount)
    recommendedGrids.value.forEach((item, index) => {
      item.rank = index + 1
      // 前8名为推荐区县
      item.recommended = index < 8
    })

    // 渲染网格到地图，推荐区县红色，其他浅灰色
    renderGridToMap()
  } catch (e) {
    console.error('加载网格数据失败:', e)
  }
}

const renderGridToMap = () => {
  if (!mapInstance.value || !gridGeoJSON.value) return

  // 清理旧图层和源
  mapRef.value?.removeLayer('grid-fill')
  mapRef.value?.removeLayer('grid-border')
  if (mapInstance.value?.getSource('grid-source')) {
    mapInstance.value.removeSource('grid-source')
  }

  // 获取推荐网格编码
  const recommendedCodes = recommendedGrids.value.filter(g => g.recommended).map(g => g.code)

  // 为网格设置推荐属性
  const styledGeoJSON = JSON.parse(JSON.stringify(gridGeoJSON.value))
  styledGeoJSON.features.forEach(feature => {
    const code = feature.properties.code || feature.properties.name
    feature.properties.isRecommended = recommendedCodes.includes(code)
  })

  // 添加 GeoJSON Source
  mapInstance.value?.addSource('grid-source', {
    type: 'geojson',
    data: styledGeoJSON
  })

  // 添加填充图层 - 推荐红色，其他浅灰色
  mapInstance.value?.addLayer({
    id: 'grid-fill',
    type: 'fill',
    source: 'grid-source',
    paint: {
      'fill-color': [
        'case',
        ['==', ['get', 'isRecommended'], true],
        '#f56c6c',  // 推荐网格 - 红色
        '#d3d3d3'   // 其他网格 - 浅灰色
      ],
      'fill-opacity': 0.5
    }
  })

  // 添加边框图层
  mapInstance.value?.addLayer({
    id: 'grid-border',
    type: 'line',
    source: 'grid-source',
    paint: {
      'line-color': [
        'case',
        ['==', ['get', 'isRecommended'], true],
        '#c0392b',  // 推荐网格边框 - 深红色
        '#b0b0b0'    // 其他网格边框 - 灰色
      ],
      'line-width': [
        'case',
        ['==', ['get', 'isRecommended'], true],
        2,  // 推荐网格边框较粗
        1   // 其他网格边框较细
      ]
    }
  })
}

const handleCalculate = () => {
  if (!config.priceRange || !config.density || !config.preference) {
    ElMessage.warning('请先填写完整的配置条件')
    return
  }

  // 根据配置条件重新排序推荐网格
  // 模拟：根据不同条件重新计算推荐顺序
  recommendedGrids.value.forEach((grid, index) => {
    // 保持10个推荐网格
    grid.recommended = index < 10
  })

  // 重新渲染地图
  renderGridToMap()

  ElMessage.success('推荐区域计算完成')
}

const handleMapClick = (e) => {
  if (!e.features?.length) return
  const feature = e.features[0]
  const props = feature.properties
  const code = props.code || props.name

  // 查找对应的网格
  const grid = recommendedGrids.value.find(g => g.code === code)
  if (grid) {
    handleSelectGrid(grid)
  }
}

const handleSelectGrid = (grid) => {
  selectedGridCode.value = grid.code

  // 生成该网格内的前端门店模拟数据
  gridStores.value = generateGridStores(grid)

  // 飞转到该区县
  mapRef.value?.flyTo({
    center: grid.coord,
    zoom: 12,
    duration: 1000
  })

  ElMessage.info(`已选中区县: ${grid.code} - ${grid.street}，共 ${gridStores.value.length} 家门店`)
}

// 生成网格内前端门店模拟数据
const generateGridStores = (grid) => {
  const storeNames = [
    '红旗超市', '永辉超市', '盒马鲜生', '沃尔玛', '家乐福',
    '舞东风', '步步高', '大润发', '欧尚', '华润万家',
    '人人乐', '世纪联华', '乐购', '物美', '京客隆'
  ]
  const categories = ['保健酒', '草本酒']
  const streets = ['天府街', '建设路', '春熙路', '玉林路', '双楠路', '龙潭寺', '武侯祠']

  // 根据区县编码生成不同的门店数量
  const storeCount = Math.floor(Math.random() * 15) + 5
  const stores = []

  for (let i = 0; i < storeCount; i++) {
    stores.push({
      name: `${storeNames[Math.floor(Math.random() * storeNames.length)]}${grid.street || '店'}${i + 1}号`,
      category: categories[Math.floor(Math.random() * categories.length)],
      address: `${grid.street || '未知区县'}${streets[Math.floor(Math.random() * streets.length)]}${Math.floor(Math.random() * 100) + 1}号`
    })
  }

  return stores
}

const handleExportGrid = () => {
  if (recommendedGrids.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }

  // 生成 Excel 数据
  const themeName = config.theme === 'lüqiao' ? '毛铺草本酒（绿荞）' : config.theme === 'jinbiao' ? '金标劲酒' : '自定义'
  const exportData = recommendedGrids.value.map(grid => ({
    '排名': grid.rank,
    '区县编码': grid.code,
    '区县名称': grid.street,
    '用户数': grid.userCount,
    '人群密度': grid.density === 'high' ? '高' : grid.density === 'medium' ? '中' : '低',
    '是否推荐': grid.recommended ? '是' : '否',
    '主题方案': themeName,
    '配置条件': `价格带:${config.priceRange || '-'} | 密度:${config.density || '-'} | 偏好:${config.preference || '-'}`
  }))

  // 转换为 CSV 格式
  const headers = Object.keys(exportData[0])
  const csvContent = [
    headers.join(','),
    ...exportData.map(row => headers.map(h => row[h]).join(','))
  ].join('\n')

  // 下载文件
  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', `推广选区区县_${new Date().toLocaleDateString()}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  ElMessage.success('网格列表已导出')
}

const handleExportStore = () => {
  if (gridStores.value.length === 0) {
    ElMessage.warning('暂无门店数据可导出')
    return
  }

  // 生成 Excel 数据
  const exportData = gridStores.value.map((store, index) => ({
    '序号': index + 1,
    '区县编码': selectedGridCode.value,
    '门店名称': store.name,
    '品类': store.category,
    '地址': store.address
  }))

  // 转换为 CSV 格式
  const headers = Object.keys(exportData[0])
  const csvContent = [
    headers.join(','),
    ...exportData.map(row => headers.map(h => row[h]).join(','))
  ].join('\n')

  // 下载文件
  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', `区县门店清单_${selectedGridCode.value}_${new Date().toLocaleDateString()}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  ElMessage.success('门店清单已导出')
}

onMounted(() => {
  loadSavedConfigs()
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
  height: 100%;
}

.config-panel h4 {
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e6e6e6;
}

.config-panel h5 {
  margin: 16px 0 8px;
  font-size: 13px;
  color: #606266;
}

.custom-config,
.full-config {
  padding: 12px;
  background: #fff;
  border-radius: 4px;
  border: 1px dashed #dcdfe6;
}

.custom-config {
  border-color: #409eff;
  background: #f0f7ff;
}

.custom-config h5 {
  margin: 0 0 12px 0;
  color: #409eff;
  font-size: 13px;
}

:deep(.el-form-item) {
  margin-bottom: 12px;
}

:deep(.el-select) {
  width: 100%;
}

.config-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

.config-actions .el-button {
  flex: 1;
}

.saved-configs {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e6e6e6;
}

.config-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.config-item:hover {
  background: #ecf5ff;
}

.config-name {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-icon {
  color: #f56c6c;
  cursor: pointer;
}

.delete-icon:hover {
  color: #f56c6c;
}

.map-wrapper {
  position: relative;
  height: 100%;
  border-radius: 4px;
  overflow: hidden;
}

.map-legend {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.95);
  padding: 10px 14px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  gap: 6px;
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

.dot.recommended {
  background: #f56c6c;
  box-shadow: 0 0 4px #f56c6c;
}

.dot.normal {
  background: #909399;
}

.right-panels {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommend-panel,
.store-panel {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 12px;
  flex-shrink: 0;
}

.recommend-panel {
  flex: 0 0 auto;
}

.store-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.panel-header h4 {
  margin: 0;
  font-size: 14px;
}

.recommend-list {
  flex-shrink: 0;
}

.store-list {
  flex: 1;
  min-height: 0;
}

:deep(.el-table__row) {
  cursor: pointer;
}

:deep(.el-table__row:hover) {
  background-color: #ecf5ff;
}
</style>
