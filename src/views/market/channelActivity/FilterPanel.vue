<template>
  <div class="filter-panel">
    <el-card shadow="hover" class="panel-card">
      <template #header>
        <div class="panel-header">
          <el-icon><Filter /></el-icon>
          <span>筛选条件</span>
        </div>
      </template>

      <el-form label-position="top" size="default">
        <el-form-item label="省份">
          <el-select
            v-model="localFilters.province"
            placeholder="全部省份"
            clearable
            @change="handleProvinceChange"
          >
            <el-option label="四川省" value="sichuan" />
            <el-option label="重庆市" value="chongqing" />
            <el-option label="湖北省" value="hubei" />
          </el-select>
        </el-form-item>

        <el-form-item label="城市">
          <el-select
            v-model="localFilters.city"
            placeholder="全部城市"
            clearable
            :disabled="!localFilters.province"
            @change="handleCityChange"
          >
            <el-option label="成都市" value="chengdu" />
            <el-option label="绵阳市" value="mianyang" />
            <el-option label="德阳市" value="deyang" />
          </el-select>
        </el-form-item>

        <el-form-item label="区县">
          <el-select
            v-model="localFilters.district"
            placeholder="全部区县"
            clearable
            :disabled="!localFilters.city"
            @change="handleChange"
          >
            <el-option label="成华区" value="chenghua" />
            <el-option label="锦江区" value="jinjiang" />
            <el-option label="武侯区" value="wuhou" />
            <el-option label="青羊区" value="qingyang" />
            <el-option label="金牛区" value="jinniu" />
            <el-option label="高新区" value="gaoxin" />
          </el-select>
        </el-form-item>

        <el-divider />

        <el-form-item label="品类">
          <el-select
            v-model="localFilters.category"
            placeholder="全部品类"
            clearable
            @change="handleChange"
          >
            <el-option label="劲酒" value="jinjiu" />
            <el-option label="保健酒" value="health" />
            <el-option label="果酒" value="fruit" />
          </el-select>
        </el-form-item>

        <el-form-item label="品牌">
          <el-select
            v-model="localFilters.brand"
            placeholder="全部品牌"
            clearable
            @change="handleChange"
          >
            <el-option label="劲牌" value="jinpai" />
            <el-option label="其它" value="other" />
          </el-select>
        </el-form-item>

        <el-form-item label="价格带">
          <el-select
            v-model="localFilters.priceRange"
            placeholder="全部价格"
            clearable
            @change="handleChange"
          >
            <el-option label="0-20元" value="0-20" />
            <el-option label="20-50元" value="20-50" />
            <el-option label="50-100元" value="50-100" />
            <el-option label="100元以上" value="100+" />
          </el-select>
        </el-form-item>

        <el-form-item label="拜访频次">
          <el-select
            v-model="localFilters.visitFreq"
            placeholder="全部"
            clearable
            @change="handleChange"
          >
            <el-option label="高频(>=3次/月)" value="high" />
            <el-option label="中频(1-3次/月)" value="medium" />
            <el-option label="低频(<1次/月)" value="low" />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="panel-footer">
        <el-button type="primary" plain @click="handleReset">重置筛选</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'
import { Filter } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const localFilters = reactive({
  province: '',
  city: '',
  district: '',
  category: '',
  brand: '',
  priceRange: '',
  visitFreq: '',
  ...props.modelValue
})

watch(localFilters, () => {
  emit('update:modelValue', { ...localFilters })
  emit('change', { ...localFilters })
}, { deep: true })

const handleChange = () => {
  emit('change', { ...localFilters })
}

const handleReset = () => {
  Object.assign(localFilters, {
    province: '',
    city: '',
    district: '',
    category: '',
    brand: '',
    priceRange: '',
    visitFreq: ''
  })
}

const handleProvinceChange = () => {
  localFilters.city = ''
  localFilters.district = ''
  handleChange()
}

const handleCityChange = () => {
  localFilters.district = ''
  handleChange()
}
</script>

<style scoped>
.filter-panel {
  height: 100%;
}

.panel-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-card :deep(.el-card__body) {
  flex: 1;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.panel-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  text-align: center;
}

.panel-footer .el-button {
  width: 100%;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}

:deep(.el-select) {
  width: 100%;
}
</style>
