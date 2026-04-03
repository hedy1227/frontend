import request from './axios'

/**
 * 上传路网GeoJSON文件
 * @param {File} file
 * @returns {Promise<{id, fileName, geoJsonUrl, featureCount, roadClass, message}>}
 */
export function uploadRoadNetwork(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/map/road/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function listRoadNetworks() {
  return request({
    url: '/map/road/list',
    method: 'get'
  })
}

export function deleteRoadNetwork(id) {
  return request({
    url: `/map/road/${id}`,
    method: 'delete'
  })
}
