<template>
  <div class="gds-map-container" ref="mapContainer">
    <div v-if="loading" class="map-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>地图加载中...</span>
    </div>
    <div v-if="error" class="map-error">
      <el-icon><Warning /></el-icon>
      <span>{{ error }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, shallowRef } from 'vue'
import maplibregl from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'

const props = defineProps({
  // 中心点 [lng, lat]
  center: {
    type: Array,
    default: () => [104.06, 30.65] // 成都
  },
  // 缩放级别
  zoom: {
    type: Number,
    default: 11
  },
  // 样式风格: 'streets' | 'dark'
  style: {
    type: String,
    default: 'streets'
  },
  // 是否显示导航控件
  navigation: {
    type: Boolean,
    default: true
  },
  // 是否可旋转
  rotate: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['load', 'click', 'move'])

// 使用 shallowRef 避免深度响应式
const mapContainer = ref()
const map = shallowRef(null)
const loading = ref(true)
const error = ref(null)

// 免费瓦片样式 - 使用CartoDB
const styleUrls = {
  streets: 'https://basemaps.cartocdn.com/gl/positron-gl-style/style.json',
  dark: 'https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json'
}

const initMap = () => {
  if (!mapContainer.value) return

  try {
    // 高德地图瓦片源
    const gaodeTiles = {
      type: 'raster',
      tiles: [
        'https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
        'https://webrd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
        'https://webrd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}'
      ],
      tileSize: 256,
      subdomains: [1, 2, 3]
    }

    const mapInstance = new maplibregl.Map({
      container: mapContainer.value,
      style: {
        version: 8,
        sources: {
          'gaode': gaodeTiles
        },
        layers: [
          {
            id: 'gaode-raster',
            type: 'raster',
            source: 'gaode',
            minzoom: 3,
            maxzoom: 18
          }
        ]
      },
      center: props.center,
      zoom: props.zoom,
      pitch: 0,
      bearing: 0,
      attributionControl: false,
      preserveDrawingBuffer: true
    })

    // 添加导航控件
    if (props.navigation) {
      mapInstance.addControl(new maplibregl.NavigationControl({
        showCompass: props.rotate,
        showZoom: true
      }), 'top-right')
    }

    // 添加比例尺
    mapInstance.addControl(new maplibregl.ScaleControl({
      maxWidth: 100,
      unit: 'metric'
    }), 'bottom-right')

    // 地图加载事件
    mapInstance.on('load', () => {
      loading.value = false
      emit('load', mapInstance)

      // 添加四级路网图层
      addRoadLayers(mapInstance)
    })

    // 点击事件
    mapInstance.on('click', (e) => {
      emit('click', {
        lng: e.lngLat.lng,
        lat: e.lngLat.lat,
        features: mapInstance.queryRenderedFeatures(e.point)
      })
    })

    // 移动事件
    mapInstance.on('move', () => {
      emit('move', {
        center: mapInstance.getCenter(),
        zoom: mapInstance.getZoom()
      })
    })

    // 错误处理 - 只记录日志，防止单张瓦片失败导致整个地图显示错误
    mapInstance.on('error', (e) => {
      console.warn('Map warning:', e.error?.message || e.error)
    })

    map.value = mapInstance

  } catch (e) {
    console.error('Map init error:', e)
    error.value = '地图初始化失败'
    loading.value = false
  }
}

// 添加四级路网图层
const addRoadLayers = (mapInstance) => {
  // 高速公路
  if (mapInstance.getLayer('road-highway')) {
    mapInstance.removeLayer('road-highway')
  }
  if (!mapInstance.getSource('roads')) {
    // OSM矢量瓦片自带道路图层，不需要额外添加
  }
}

// 添加路网图层（支持GeoJSON URL或对象，自动识别LineString道路或Polygon片区）
// options: { opacity, lineWidth, color, colorField, suitabilityField, alertLevelField }
const addRoadLayer = (id, geojson, options = {}) => {
  if (!map.value) return

  const {
    opacity = 0.6,
    lineWidth = 1,
    color = '#409eff',
    colorField = null,        // 热力值字段名，归一化0-1
    suitabilityField = null,  // 推广适合度字段：best/good/normal
    alertLevelField = null    // 预警等级字段：red/yellow/green
  } = options

  // 清理旧图层和源
  if (map.value.getSource(id)) {
    ;['zone-fill', 'zone-border', 'zone-line'].forEach(l => {
      if (map.value.getLayer(`${id}-${l}`)) {
        map.value.removeLayer(`${id}-${l}`)
      }
    })
    map.value.removeSource(id)
  }

  // 添加GeoJSON Source
  map.value.addSource(id, {
    type: 'geojson',
    data: geojson
  })

  // 构建 fill 颜色表达式
  let fillColorExpr
  if (alertLevelField) {
    // 预警等级：红色(0.4)/黄色(0.6)/绿色(1.0) 透明度
    fillColorExpr = [
      'match',
      ['get', alertLevelField],
      'red', '#f56c6c',    // 红色预警
      'yellow', '#e6a23c',  // 黄色预警
      'green', '#67c23a',   // 绿色健康
      color
    ]
  } else if (suitabilityField) {
    // 推广适合度：最佳(绿)、良好(蓝)、一般(灰)
    fillColorExpr = [
      'match',
      ['get', suitabilityField],
      'best', '#67c23a',    // 绿色 - 最佳
      'good', '#409eff',    // 蓝色 - 良好
      'normal', '#909399',  // 灰色 - 一般
      color
    ]
  } else if (colorField) {
    // 热力值色阶：蓝-青-黄-橙-红
    fillColorExpr = [
      'interpolate',
      ['linear'],
      ['get', colorField],
      0, '#313695',
      0.25, '#74add1',
      0.5, '#fee090',
      0.75, '#f46d43',
      1, '#a50026'
    ]
  } else {
    fillColorExpr = color
  }

  // 渲染 Polygon 片区（电子围栏）
  map.value.addLayer({
    id: `${id}-zone-fill`,
    type: 'fill',
    source: id,
    paint: {
      'fill-color': fillColorExpr,
      'fill-opacity': opacity
    },
    filter: ['==', ['geometry-type'], 'Polygon']
  })

  map.value.addLayer({
    id: `${id}-zone-border`,
    type: 'line',
    source: id,
    paint: {
      'line-color': alertLevelField ? '#333' : color,
      'line-width': lineWidth,
      'line-opacity': 0.9
    },
    layout: {
      'line-join': 'round'
    },
    filter: ['==', ['geometry-type'], 'Polygon']
  })

  // 渲染 LineString 道路
  map.value.addLayer({
    id: `${id}-zone-line`,
    type: 'line',
    source: id,
    paint: {
      'line-color': color,
      'line-width': 2,
      'line-opacity': 0.8
    },
    layout: {
      'line-join': 'round',
      'line-cap': 'round'
    },
    filter: ['==', ['geometry-type'], 'LineString']
  })
}

// 添加热力图层
const addHeatLayer = (id, data, options = {}) => {
  if (!map.value) return

  const { radius = 30, intensity = 1, opacity = 0.6 } = options

  if (map.value.getSource(id)) {
    map.value.getSource(id).setData({
      type: 'FeatureCollection',
      features: data.map(item => ({
        type: 'Feature',
        properties: { intensity: item.intensity || 1 },
        geometry: {
          type: 'Point',
          coordinates: item.coordinates || item
        }
      }))
    })
  } else {
    map.value.addSource(id, {
      type: 'geojson',
      data: {
        type: 'FeatureCollection',
        features: data.map(item => ({
          type: 'Feature',
          properties: { intensity: item.intensity || 1 },
          geometry: {
            type: 'Point',
            coordinates: item.coordinates || item
          }
        }))
      }
    })

    map.value.addLayer({
      id: id,
      type: 'heatmap',
      source: id,
      paint: {
        'heatmap-radius': radius,
        'heatmap-intensity': intensity,
        'heatmap-opacity': opacity,
        'heatmap-color': [
          'interpolate',
          ['linear'],
          ['heatmap-density'],
          0, 'rgba(0,0,0,0)',
          0.1, 'rgba(49,54,149,0.5)',
          0.3, 'rgba(116,173,209,0.7)',
          0.5, 'rgba(254,224,144,0.8)',
          0.7, 'rgba(253,174,97,0.9)',
          0.9, 'rgba(215,48,39,0.95)',
          1, 'rgba(165,0,38,1)'
        ]
      }
    })
  }
}

// 添加散点图层
const addScatterLayer = (id, data, options = {}) => {
  if (!map.value) return

  const { radius = 8, color = '#409eff', opacity = 1 } = options

  if (map.value.getSource(id)) {
    map.value.getSource(id).setData({
      type: 'FeatureCollection',
      features: data.map(item => ({
        type: 'Feature',
        properties: item.properties || {},
        geometry: {
          type: 'Point',
          coordinates: item.coordinates || item
        }
      }))
    })
  } else {
    map.value.addSource(id, {
      type: 'geojson',
      data: {
        type: 'FeatureCollection',
        features: data.map(item => ({
          type: 'Feature',
          properties: item.properties || {},
          geometry: {
            type: 'Point',
            coordinates: item.coordinates || item
          }
        }))
      }
    })

    map.value.addLayer({
      id: id,
      type: 'circle',
      source: id,
      paint: {
        'circle-radius': radius,
        'circle-color': color,
        'circle-opacity': opacity,
        'circle-stroke-width': 2,
        'circle-stroke-color': '#ffffff'
      }
    })
  }
}

// 添加弧线流向图层
const addArcLayer = (id, data, options = {}) => {
  if (!map.value) return

  const { color = '#409eff', width = 2 } = options

  if (map.value.getSource(id)) {
    map.value.getSource(id).setData({
      type: 'FeatureCollection',
      features: data.map(item => ({
        type: 'Feature',
        properties: {
          color: item.color || color,
          width: item.width || width
        },
        geometry: {
          type: 'LineString',
          coordinates: item.coordinates
        }
      }))
    })
  } else {
    map.value.addSource(id, {
      type: 'geojson',
      data: {
        type: 'FeatureCollection',
        features: data.map(item => ({
          type: 'Feature',
          properties: {
            color: item.color || color,
            width: item.width || width
          },
          geometry: {
            type: 'LineString',
            coordinates: item.coordinates
          }
        }))
      }
    })

    map.value.addLayer({
      id: id,
      type: 'line',
      source: id,
      paint: {
        'line-color': ['get', 'color'],
        'line-width': ['get', 'width'],
        'line-opacity': 0.8,
        'line-dasharray': [2, 1]
      }
    })
  }
}

// 添加多边形区域图层
const addPolygonLayer = (id, data, options = {}) => {
  if (!map.value) return

  const { color = '#409eff', opacity = 0.3, borderColor = '#ffffff' } = options

  if (map.value.getSource(id)) {
    map.value.getSource(id).setData({
      type: 'FeatureCollection',
      features: data.map(item => ({
        type: 'Feature',
        properties: { color: item.color || color },
        geometry: {
          type: 'Polygon',
          coordinates: [item.coordinates]
        }
      }))
    })
  } else {
    map.value.addSource(id, {
      type: 'geojson',
      data: {
        type: 'FeatureCollection',
        features: data.map(item => ({
          type: 'Feature',
          properties: { color: item.color || color },
          geometry: {
            type: 'Polygon',
            coordinates: [item.coordinates]
          }
        }))
      }
    })

    map.value.addLayer({
      id: id,
      type: 'fill',
      source: id,
      paint: {
        'fill-color': ['get', 'color'],
        'fill-opacity': opacity
      }
    })

    map.value.addLayer({
      id: id + '-border',
      type: 'line',
      source: id,
      paint: {
        'line-color': borderColor,
        'line-width': 2
      }
    })
  }
}

// 移除图层
const removeLayer = (id) => {
  if (!map.value) return
  if (map.value.getLayer(id)) map.value.removeLayer(id)
  if (map.value.getSource(id)) map.value.removeSource(id)
}

// 飞行到指定位置
const flyTo = (options) => {
  if (!map.value) return
  map.value.flyTo(options)
}

// 获取地图实例
const getMap = () => map.value

// 暴露方法给父组件
defineExpose({
  addHeatLayer,
  addScatterLayer,
  addArcLayer,
  addPolygonLayer,
  addRoadLayer,
  removeLayer,
  flyTo,
  getMap
})

onMounted(() => {
  initMap()
})

onUnmounted(() => {
  if (map.value) {
    map.value.remove()
    map.value = null
  }
})
</script>

<style scoped>
.gds-map-container {
  width: 100%;
  height: 100%;
  position: relative;
  border-radius: 4px;
  overflow: hidden;
}

.map-loading,
.map-error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #666;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  padding: 20px 40px;
  border-radius: 8px;
}

.map-error {
  color: #f56c6c;
}

/* MapLibre 控件样式覆盖 */
:deep(.maplibregl-ctrl-group) {
  border-radius: 4px;
  overflow: hidden;
}

:deep(.maplibregl-ctrl-group button) {
  width: 32px;
  height: 32px;
}

:deep(.maplibregl-ctrl-scale) {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 2px;
  font-size: 11px;
}
</style>
