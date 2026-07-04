<template>
  <div class="profile-page">
    <el-tabs v-model="activeTab" class="profile-tabs">
      <el-tab-pane label="个人信息" name="info">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="section-header">
              <span class="section-title">基本信息</span>
              <el-button type="primary" size="small" @click="openEditDialog">
                <el-icon style="margin-right:4px"><Edit /></el-icon>
                编辑资料
              </el-button>
            </div>
          </template>

          <el-skeleton v-if="infoLoading" :rows="5" animated />

          <el-empty v-else-if="!userStore.user" description="请先登录" :image-size="100">
            <el-button type="primary" @click="$router.push('/login')">去登录</el-button>
          </el-empty>

          <el-descriptions v-else :column="1" border>
            <el-descriptions-item label="用户名">{{ userStore.user.username }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ userStore.user.email || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ userStore.user.phone || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="学校">{{ userStore.user.school || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="个人简介">{{ userStore.user.bio || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ formatDate(userStore.user.createdAt) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="我的发布" name="my-products">
        <el-card shadow="never" class="section-card">
          <template #header>
            <span class="section-title">我发布的商品</span>
          </template>

          <div v-if="myProductsLoading" class="loading-area">
            <el-skeleton :rows="4" animated />
          </div>

          <el-empty v-else-if="myProducts.length === 0" description="还没有发布过商品">
            <el-button type="primary" @click="$router.push('/publish')">发布商品</el-button>
          </el-empty>

          <template v-else>
            <div class="product-list">
              <div v-for="item in myProducts" :key="item.id" class="product-item">
                <div class="product-image-box">
                  <el-image
                    v-if="item.imageUrl"
                    :src="item.imageUrl"
                    fit="cover"
                    class="thumb-image"
                  >
                    <template #error>
                      <div class="thumb-placeholder">
                        <el-icon :size="28"><PictureFilled /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <div v-else class="thumb-placeholder">
                    <el-icon :size="28"><PictureFilled /></el-icon>
                  </div>
                </div>
                <div class="product-info">
                  <router-link :to="`/product/${item.id}`" class="product-title-link">
                    {{ item.title }}
                  </router-link>
                  <div class="product-meta">
                    <span class="product-price">￥{{ (item.price != null ? Number(item.price) : 0).toFixed(2) }}</span>
                    <el-tag :type="statusTagType(item.status)" size="small">
                      {{ statusLabel(item.status) }}
                    </el-tag>
                  </div>
                  <div class="product-time">{{ formatRelativeTime(item.createdAt) }}</div>
                </div>
                <div class="product-actions">
                  <el-button size="small" plain @click="$router.push(`/product/${item.id}/edit`)">
                    编辑
                  </el-button>
                  <el-button
                    v-if="item.status === 'AVAILABLE'"
                    size="small"
                    type="success"
                    plain
                    :loading="statusChangingId === item.id"
                    @click="handleMarkSold(item)"
                  >
                    标记已售
                  </el-button>
                  <el-button
                    v-if="item.status === 'AVAILABLE'"
                    size="small"
                    type="danger"
                    plain
                    :loading="statusChangingId === item.id"
                    @click="handleTakeDown(item)"
                  >
                    下架
                  </el-button>
                  <el-button
                    v-if="item.status === 'SOLD' || item.status === 'OFF_SHELF'"
                    size="small"
                    type="primary"
                    plain
                    :loading="statusChangingId === item.id"
                    @click="handleReList(item)"
                  >
                    重新上架
                  </el-button>
                </div>
              </div>
            </div>
          </template>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="我的收藏" name="bookmarks">
        <el-card shadow="never" class="section-card">
          <template #header>
            <span class="section-title">我收藏的商品</span>
          </template>

          <div v-if="bookmarksLoading" class="loading-area">
            <el-row :gutter="16">
              <el-col v-for="i in 4" :key="i" :xs="12" :sm="8" :md="6" :lg="6" style="margin-bottom:16px">
                <el-skeleton animated>
                  <template #template>
                    <el-skeleton-item variant="image" style="width:100%;height:140px" />
                    <el-skeleton-item variant="text" style="width:80%;margin-top:8px" />
                  </template>
                </el-skeleton>
              </el-col>
            </el-row>
          </div>

          <el-empty v-else-if="bookmarks.length === 0" description="还没有收藏商品" />

          <el-row v-else :gutter="16">
            <el-col
              v-for="bm in bookmarks"
              :key="bm.id"
              :xs="12"
              :sm="8"
              :md="6"
              :lg="6"
              style="margin-bottom: 16px"
            >
              <ProductCard :product="bm" />
            </el-col>
          </el-row>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 编辑资料对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑个人资料"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        label-width="80px"
      >
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="学校" prop="school">
          <el-input v-model="editForm.school" placeholder="请输入学校" />
        </el-form-item>
        <el-form-item label="个人简介" prop="bio">
          <el-input
            v-model="editForm.bio"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="介绍一下自己吧"
          />
        </el-form-item>
        <el-form-item label="头像地址" prop="avatar">
          <el-input v-model="editForm.avatar" placeholder="输入头像图片链接" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="handleSaveProfile">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { updateMe, getUserProducts } from '../api/user'
import { updateStatus } from '../api/product'
import { getMyBookmarks } from '../api/bookmark'
import ProductCard from '../components/ProductCard.vue'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('info')

// 个人信息
const infoLoading = ref(false)
const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref(null)

const editForm = reactive({
  phone: '',
  school: '',
  bio: '',
  avatar: ''
})

function openEditDialog() {
  editForm.phone = userStore.user?.phone || ''
  editForm.school = userStore.user?.school || ''
  editForm.bio = userStore.user?.bio || ''
  editForm.avatar = userStore.user?.avatar || ''
  editDialogVisible.value = true
}

async function handleSaveProfile() {
  editSubmitting.value = true
  try {
    const res = await updateMe({
      phone: editForm.phone.trim(),
      school: editForm.school.trim(),
      bio: editForm.bio.trim(),
      avatar: editForm.avatar.trim()
    })
    userStore.updateUser(res.data)
    ElMessage.success('资料更新成功')
    editDialogVisible.value = false
  } catch {
    // handled in interceptor
  } finally {
    editSubmitting.value = false
  }
}

// 我的发布
const myProducts = ref([])
const myProductsLoading = ref(false)
const statusChangingId = ref(null)

async function fetchMyProducts() {
  if (!userStore.user?.id) return
  myProductsLoading.value = true
  try {
    const res = await getUserProducts(userStore.user.id)
    myProducts.value = res.data?.records || res.data || []
  } catch {
    myProducts.value = []
  } finally {
    myProductsLoading.value = false
  }
}

function statusTagType(status) {
  const map = { AVAILABLE: 'success', SOLD: 'danger', OFF_SHELF: 'info' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { AVAILABLE: '在售', SOLD: '已售', OFF_SHELF: '已下架' }
  return map[status] || status
}

async function handleMarkSold(item) {
  try {
    await ElMessageBox.confirm('确定将此商品标记为已售吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
  } catch {
    return
  }

  statusChangingId.value = item.id
  try {
    await updateStatus(item.id, 'SOLD')
    ElMessage.success('已标记为已售')
    item.status = 'SOLD'
  } catch {
    // handled in interceptor
  } finally {
    statusChangingId.value = null
  }
}

async function handleTakeDown(item) {
  try {
    await ElMessageBox.confirm('确定要下架此商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  statusChangingId.value = item.id
  try {
    await updateStatus(item.id, 'OFF_SHELF')
    ElMessage.success('已下架')
    item.status = 'OFF_SHELF'
  } catch {
    // handled in interceptor
  } finally {
    statusChangingId.value = null
  }
}

async function handleReList(item) {
  statusChangingId.value = item.id
  try {
    await updateStatus(item.id, 'AVAILABLE')
    ElMessage.success('已重新上架')
    item.status = 'AVAILABLE'
  } catch {
    // handled in interceptor
  } finally {
    statusChangingId.value = null
  }
}

// 我的收藏
const bookmarks = ref([])
const bookmarksLoading = ref(false)

async function fetchBookmarks() {
  bookmarksLoading.value = true
  try {
    const res = await getMyBookmarks()
    bookmarks.value = res.data?.records || res.data || []
  } catch {
    bookmarks.value = []
  } finally {
    bookmarksLoading.value = false
  }
}

// 工具函数
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

function formatDate(dateStr) {
  if (!dateStr) return '未知'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

// 标签切换时加载对应数据
watch(activeTab, (tab) => {
  if (tab === 'my-products' && myProducts.value.length === 0) {
    fetchMyProducts()
  }
  if (tab === 'bookmarks' && bookmarks.value.length === 0) {
    fetchBookmarks()
  }
})

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
  }
})
</script>

<style scoped>
.profile-page {
  max-width: 960px;
  margin: 0 auto;
}

.profile-tabs {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  padding: 4px 20px 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.section-card {
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.section-card :deep(.el-card__header) {
  padding: 16px 20px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.loading-area {
  padding: 16px 0;
}

/* 我的发布列表 */
.product-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: border-color 0.2s;
}

.product-item:hover {
  border-color: #c6e2ff;
}

.product-image-box {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
  background: #f5f7fa;
}

.thumb-image {
  width: 100%;
  height: 100%;
}

.thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.product-info {
  flex: 1;
  min-width: 0;
}

.product-title-link {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  text-decoration: none;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
}

.product-title-link:hover {
  color: #409eff;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.product-price {
  font-size: 16px;
  font-weight: 700;
  color: #f56c6c;
}

.product-time {
  font-size: 12px;
  color: #c0c4cc;
}

.product-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .profile-tabs {
    padding: 4px 12px 16px;
  }

  .product-item {
    flex-wrap: wrap;
  }

  .product-actions {
    width: 100%;
    padding-left: 96px;
  }
}
</style>
