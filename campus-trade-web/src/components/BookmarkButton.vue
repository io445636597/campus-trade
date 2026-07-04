<template>
  <el-button
    :type="isBookmarked ? 'warning' : 'default'"
    :icon="isBookmarked ? StarFilled : Star"
    :loading="loading"
    @click.stop="handleToggle"
    size="default"
  >
    {{ isBookmarked ? '已收藏' : '收藏' }}
    <span v-if="count != null" style="margin-left:4px">({{ count }})</span>
  </el-button>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { toggle } from '../api/bookmark'

const props = defineProps({
  productId: { type: [Number, String], required: true },
  bookmarked: { type: Boolean, default: false },
  count: { type: Number, default: null }
})

const emit = defineEmits(['toggle'])

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const isBookmarked = ref(props.bookmarked)

watch(() => props.bookmarked, (val) => { isBookmarked.value = val })

async function handleToggle() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }

  loading.value = true
  try {
    const res = await toggle(props.productId)
    isBookmarked.value = !isBookmarked.value
    ElMessage.success(res.message || (isBookmarked.value ? '收藏成功' : '已取消收藏'))
    emit('toggle', isBookmarked.value)
  } catch {
    // 请求失败已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>
