<template>
  <div class="search-bar">
    <el-input
      v-model="keyword"
      size="large"
      placeholder="搜索商品..."
      clearable
      @keyup.enter="handleSearch"
      @clear="handleSearch"
      class="search-input"
    >
      <template #append>
        <el-button :icon="Search" @click="handleSearch" :loading="loading">
          搜索
        </el-button>
      </template>
    </el-input>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'search'])

const keyword = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  keyword.value = val
})

function handleSearch() {
  emit('update:modelValue', keyword.value)
  emit('search', keyword.value)
}
</script>

<style scoped>
.search-bar {
  margin-bottom: 20px;
}

.search-input :deep(.el-input-group__append) {
  background-color: #409eff;
  border-color: #409eff;
  color: #fff;
}

.search-input :deep(.el-input-group__append .el-button) {
  color: #fff;
  border: none;
  background: transparent;
  font-size: 15px;
}
</style>
