<template>
  <div class="category-filter">
    <div class="filter-row">
      <span class="filter-label">分类：</span>
      <div class="category-buttons">
        <el-button
          :type="currentCategory === '' ? 'primary' : 'default'"
          size="small"
          @click="selectCategory('')"
        >
          全部
        </el-button>
        <el-button
          v-for="cat in CATEGORIES"
          :key="cat.value"
          :type="currentCategory === cat.value ? 'primary' : 'default'"
          size="small"
          @click="selectCategory(cat.value)"
        >
          {{ cat.label }}
        </el-button>
      </div>
    </div>
    <div class="filter-row filter-options">
      <div class="filter-item">
        <span class="filter-label">成色：</span>
        <el-select
          v-model="currentCondition"
          placeholder="全部成色"
          size="small"
          clearable
          style="width: 140px"
          @change="emitChange"
        >
          <el-option
            v-for="cond in CONDITIONS"
            :key="cond.value"
            :label="cond.label"
            :value="cond.value"
          />
        </el-select>
      </div>
      <div class="filter-item">
        <span class="filter-label">排序：</span>
        <el-select
          v-model="currentSort"
          size="small"
          style="width: 140px"
          @change="emitChange"
        >
          <el-option
            v-for="sort in SORT_OPTIONS"
            :key="sort.value"
            :label="sort.label"
            :value="sort.value"
          />
        </el-select>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { CATEGORIES, CONDITIONS, SORT_OPTIONS } from '../utils/constants'

const props = defineProps({
  category: { type: String, default: '' },
  condition: { type: String, default: '' },
  sort: { type: String, default: 'newest' }
})

const emit = defineEmits(['update:category', 'update:condition', 'update:sort', 'change'])

const currentCategory = ref(props.category || '')
const currentCondition = ref(props.condition || '')
const currentSort = ref(props.sort || 'newest')

watch(() => props.category, (val) => { currentCategory.value = val || '' })
watch(() => props.condition, (val) => { currentCondition.value = val || '' })
watch(() => props.sort, (val) => { currentSort.value = val || 'newest' })

function selectCategory(cat) {
  currentCategory.value = cat
  emit('update:category', cat)
  emitChange()
}

function emitChange() {
  emit('update:condition', currentCondition.value)
  emit('update:sort', currentSort.value)
  emit('change', {
    category: currentCategory.value,
    condition: currentCondition.value,
    sort: currentSort.value
  })
}
</script>

<style scoped>
.category-filter {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
  border: 1px solid #ebeef5;
}

.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.filter-row + .filter-row {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  margin-right: 8px;
  white-space: nowrap;
}

.category-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.filter-options {
  display: flex;
  gap: 24px;
}

.filter-item {
  display: flex;
  align-items: center;
}
</style>
