<template>
  <div class="pagination-wrapper" v-if="total > 0">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="currentSize"
      :page-sizes="pageSizes"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 12 },
  pageSizes: {
    type: Array,
    default: () => [8, 12, 16, 20]
  }
})

const emit = defineEmits(['page-change', 'size-change'])

const currentPage = ref(props.page)
const currentSize = ref(props.size)

watch(() => props.page, (val) => { currentPage.value = val })
watch(() => props.size, (val) => { currentSize.value = val })

function handlePageChange(page) {
  emit('page-change', page)
}

function handleSizeChange(size) {
  emit('size-change', size)
}
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding: 16px 0;
}
</style>
