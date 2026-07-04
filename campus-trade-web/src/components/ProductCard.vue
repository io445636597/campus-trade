<template>
  <div class="product-card" @click="goDetail">
    <div class="product-image">
      <el-image
        :src="product.imageUrl"
        fit="cover"
        class="image-el"
        :preview-src-list="product.imageUrl ? [product.imageUrl] : []"
      >
        <template #error>
          <div class="image-placeholder">
            <el-icon :size="48"><PictureFilled /></el-icon>
            <span>暂无图片</span>
          </div>
        </template>
      </el-image>
      <span class="condition-tag" :class="'condition-' + product.condition">
        {{ conditionText }}
      </span>
    </div>
    <div class="product-info">
      <h3 class="product-title" :title="product.title">{{ product.title }}</h3>
      <div class="product-meta">
        <span class="product-price">￥{{ product.price }}</span>
        <span class="product-category">{{ product.category }}</span>
      </div>
      <div class="product-footer">
        <span class="product-time">{{ relativeTime }}</span>
        <div class="product-stats">
          <span v-if="product.bookmarkCount != null" class="stat-item">
            <el-icon :size="14"><Star /></el-icon>
            {{ product.bookmarkCount }}
          </span>
          <span v-if="product.messageCount != null" class="stat-item">
            <el-icon :size="14"><ChatDotRound /></el-icon>
            {{ product.messageCount }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { CONDITION_MAP } from '../utils/constants'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})

const router = useRouter()

const conditionText = computed(() => {
  return CONDITION_MAP[props.product.condition] || props.product.condition
})

const relativeTime = computed(() => {
  return formatRelativeTime(props.product.createdAt)
})

function goDetail() {
  router.push(`/product/${props.product.id}`)
}

function formatRelativeTime(dateStr) {
  if (!dateStr) return ''
  const now = new Date()
  const date = new Date(dateStr)
  const diff = now - date
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
</script>

<style scoped>
.product-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #ebeef5;
  transition: all 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border-color: #c6e2ff;
}

.product-image {
  position: relative;
  width: 100%;
  padding-top: 75%;
  overflow: hidden;
  background: #f5f7fa;
}

.image-el {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  background: #f5f7fa;
  gap: 8px;
  font-size: 13px;
}

.condition-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
  font-weight: 500;
}

.condition-NEW {
  background: #67c23a;
}

.condition-LIKE_NEW {
  background: #409eff;
}

.condition-GOOD {
  background: #e6a23c;
}

.condition-FAIR {
  background: #909399;
}

.product-info {
  padding: 12px 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.product-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.product-price {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}

.product-category {
  font-size: 12px;
  color: #909399;
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
}

.product-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}

.product-time {
  font-size: 12px;
  color: #c0c4cc;
}

.product-stats {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: #909399;
}
</style>
