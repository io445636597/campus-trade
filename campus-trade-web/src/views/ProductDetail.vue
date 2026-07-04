<template>
  <div class="product-detail">
    <el-skeleton v-if="loading" :rows="8" animated class="detail-skeleton" />

    <el-empty v-else-if="error" description="商品不存在或已下架" :image-size="160">
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </el-empty>

    <template v-else-if="product">
      <div class="detail-main">
        <div class="detail-image">
          <el-image
            v-if="product.imageUrl"
            :src="product.imageUrl"
            fit="contain"
            class="main-image"
            :preview-src-list="[product.imageUrl]"
            :preview-teleported="true"
          >
            <template #error>
              <div class="image-placeholder">
                <el-icon :size="64"><PictureFilled /></el-icon>
                <span>图片加载失败</span>
              </div>
            </template>
          </el-image>
          <div v-else class="image-placeholder">
            <el-icon :size="64"><PictureFilled /></el-icon>
            <span>暂无图片</span>
          </div>
        </div>

        <div class="detail-info">
          <h1 class="product-title">{{ product.title }}</h1>

          <div class="price-row">
            <span class="product-price">￥{{ (product.price != null ? Number(product.price) : 0).toFixed(2) }}</span>
          </div>

          <div class="tags-row">
            <el-tag :type="conditionTagType" size="default">{{ conditionText }}</el-tag>
            <el-tag type="info" size="default">{{ product.category }}</el-tag>
          </div>

          <div class="meta-row">
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              {{ formatRelativeTime(product.createdAt) }}
            </span>
            <span class="meta-item">
              <el-icon><View /></el-icon>
              {{ product.viewCount || 0 }} 次浏览
            </span>
          </div>

          <div class="stats-row">
            <span class="stat-item">
              <el-icon><Star /></el-icon>
              {{ product.bookmarkCount || 0 }} 收藏
            </span>
            <span class="stat-item">
              <el-icon><ChatDotRound /></el-icon>
              {{ product.messageCount || 0 }} 留言
            </span>
          </div>

          <div class="action-row">
            <BookmarkButton
              :product-id="product.id"
              :bookmarked="product.bookmarked || false"
              :count="product.bookmarkCount || 0"
              @toggle="handleBookmarkToggle"
            />
            <template v-if="isOwner">
              <el-button type="primary" plain @click="$router.push(`/product/${product.id}/edit`)">
                <el-icon style="margin-right:4px"><Edit /></el-icon>
                编辑商品
              </el-button>
              <el-button
                v-if="product.status === 'AVAILABLE'"
                type="success"
                plain
                :loading="statusLoading"
                @click="handleMarkSold"
              >
                标记已售
              </el-button>
              <el-button
                v-if="product.status === 'AVAILABLE'"
                type="danger"
                plain
                :loading="statusLoading"
                @click="handleTakeDown"
              >
                下架商品
              </el-button>
              <el-button
                v-if="product.status === 'SOLD' || product.status === 'OFF_SHELF'"
                type="primary"
                plain
                :loading="statusLoading"
                @click="handleReList"
              >
                重新上架
              </el-button>
            </template>
          </div>

          <div v-if="product.author" class="author-card">
            <router-link :to="`/user/${product.author.id}/products`" class="author-link">
              <el-avatar :size="40" :src="product.author.avatar">
                {{ (product.author.username || 'U').charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="author-name">{{ product.author.username }}</span>
            </router-link>
          </div>
        </div>
      </div>

      <div class="detail-description">
        <h3 class="section-title">商品描述</h3>
        <p class="description-text">{{ product.description || '卖家很懒，没有留下任何描述~' }}</p>
      </div>

      <MessageSection
        ref="messageSectionRef"
        :product-id="product.id"
        :product-owner-id="product.userId"
        @update:count="product.messageCount = $event"
      />
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'
import { getDetail, updateStatus } from '../api/product'
import { CONDITION_MAP } from '../utils/constants'
import BookmarkButton from '../components/BookmarkButton.vue'
import MessageSection from '../components/MessageSection.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const product = ref(null)
const loading = ref(true)
const error = ref(false)
const statusLoading = ref(false)
const messageSectionRef = ref(null)

const conditionText = computed(() => {
  if (!product.value) return ''
  return CONDITION_MAP[product.value.condition] || product.value.condition
})

const conditionTagType = computed(() => {
  if (!product.value) return 'info'
  const map = { NEW: 'success', LIKE_NEW: '', GOOD: 'warning', FAIR: 'info' }
  return map[product.value.condition] || 'info'
})

const isOwner = computed(() => {
  if (!product.value || !userStore.user) return false
  return product.value.userId === userStore.user.id
})

async function fetchDetail() {
  const id = route.params.id
  if (!id) {
    error.value = true
    loading.value = false
    return
  }

  loading.value = true
  error.value = false
  try {
    const res = await getDetail(id)
    product.value = res.data
  } catch {
    error.value = true
    product.value = null
  } finally {
    loading.value = false
  }
}

function handleBookmarkToggle(bookmarked) {
  if (product.value) {
    product.value.bookmarked = bookmarked
    product.value.bookmarkCount = (product.value.bookmarkCount || 0) + (bookmarked ? 1 : -1)
  }
}

async function handleMarkSold() {
  try {
    await ElMessageBox.confirm('确定要将此商品标记为已售吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
  } catch {
    return
  }

  statusLoading.value = true
  try {
    await updateStatus(product.value.id, 'SOLD')
    ElMessage.success('已标记为已售')
    product.value.status = 'SOLD'
  } catch {
    // handled in interceptor
  } finally {
    statusLoading.value = false
  }
}

async function handleTakeDown() {
  try {
    await ElMessageBox.confirm('确定要下架此商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  statusLoading.value = true
  try {
    await updateStatus(product.value.id, 'OFF_SHELF')
    ElMessage.success('已下架')
    product.value.status = 'OFF_SHELF'
  } catch {
    // handled in interceptor
  } finally {
    statusLoading.value = false
  }
}

async function handleReList() {
  statusLoading.value = true
  try {
    await updateStatus(product.value.id, 'AVAILABLE')
    ElMessage.success('已重新上架')
    product.value.status = 'AVAILABLE'
  } catch {
    // handled in interceptor
  } finally {
    statusLoading.value = false
  }
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

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.product-detail {
  max-width: 1200px;
  margin: 0 auto;
}

.detail-skeleton {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
}

.detail-main {
  display: flex;
  gap: 32px;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.detail-image {
  flex-shrink: 0;
  width: 400px;
  height: 400px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
}

.main-image {
  width: 100%;
  height: 100%;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  background: #f5f7fa;
  gap: 12px;
  font-size: 14px;
}

.detail-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.product-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.4;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.product-price {
  font-size: 32px;
  font-weight: 700;
  color: #f56c6c;
}

.tags-row {
  display: flex;
  gap: 8px;
}

.meta-row {
  display: flex;
  gap: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.stats-row {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #606266;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 8px;
}

.author-card {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.author-link {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f5f7fa;
  transition: background 0.2s;
}

.author-link:hover {
  background: #e8eaed;
}

.author-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.detail-description {
  margin-top: 24px;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
}

.description-text {
  font-size: 15px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
}

@media (max-width: 768px) {
  .detail-main {
    flex-direction: column;
    gap: 20px;
    padding: 16px;
  }

  .detail-image {
    width: 100%;
    height: 280px;
  }

  .product-title {
    font-size: 18px;
  }

  .product-price {
    font-size: 26px;
  }
}
</style>
