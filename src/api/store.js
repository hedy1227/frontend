import request from './axios'

// 获取前端网点列表（带筛选条件）
export function getStoreList(params) {
  return request({
    url: '/store/front/list',
    method: 'get',
    params
  })
}

// 获取前端网点散点 GeoJSON
export function getStoreGeoJSON(params) {
  return request({
    url: '/store/front/geojson',
    method: 'get',
    params
  })
}

// 获取四级路网网格数据（热力色阶用）
export function getRoadGridGeoJSON() {
  return request({
    url: '/store/front/road-grid',
    method: 'get'
  })
}

// 获取统计卡片数据
export function getStoreStats(params) {
  return request({
    url: '/store/front/stats',
    method: 'get',
    params
  })
}

// 获取区域密度排行
export function getDensityRanking(params) {
  return request({
    url: '/store/front/density-ranking',
    method: 'get',
    params
  })
}
