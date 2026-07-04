<template>
  <div class="user-products-page">
    <el-skeleton v-if="userLoading" :rows="3" animated class="loading-skeleton" />

    <el-result
      v-else-if="userError"
      icon="error"
      title="加载失败"
      :sub-title="userError"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </template>
    </el-result>

    <template v-else-if="userInfo">
      <div class="user-header">
        <el-avatar :size="72" :src="userInfo.avatar" class="user-avatar">
          {{ (userInfo.username || 'U').charAt(0).toUpperCase() }}
        </el-avatar>
        <div class="user-meta">
          <h2 class="user-name">{{ userInfo.username }}</h2>
          <p v-if="userInfo.school" class="user-school">
            <el-icon><School /></el-icon>
            {{ userInfo.school }}
          </p>
          <p v-if="userInfo.bio" class="user-bio">{{ userInfo.bio }}</p>
        </div>
      </div>

      <div class="products-section">
        <h2 class="section-title">
          <span v-if="isSelf">我发布的商品</span>
          <span v-else>{{ userInfo.username }} 发布的商品</span>
          <span class="product-count">({{ products.length }})</span>
        </h2>

        <div v-if="productsLoading" class="loading-area">
          <el-skeleton :rows="3" animated />
          <el-row :gutter="16" style="margin-top:16px">
            <el-col v-for="i in 8" :key="i" :xs="12" :sm="8" :md="6" :lg="6" style="margin-bottom:16px">
              <el-skeleton animated>
                <template #template>
                  <el-skeleton-item variant="image" style="width:100%;height:140px" />
                  <el-skeleton-item variant="text" style="width:80%;margin-top:8px" />
                  <el-skeleton-item variant="text" style="width:50%" />
                </template>
              </el-skeleton>
            </el-col>
          </el-row>
        </div>

        <el-empty v-else-if="availableProducts.length === 0" description="暂无在售商品" :image-size="120" />

        <el-row v-else :gutter="16">
          <el-col
            v-for="product in availableProducts"
            :key="product.id"
            :xs="12"
            :sm="8"
            :md="6"
            :lg="6"
            style="margin-bottom: 16px"
          >
            <ProductCard :product="product" />
          </el-col>
        </el-row>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { getUserById, getUserProducts } from '../api/user'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const userStore = useUserStore()

const userId = computed(() => Number(route.params.id))

const userInfo = ref(null)
const userLoading = ref(true)
const userError = ref('')

const products = ref([])
const productsLoading = ref(false)

const isSelf = computed(() => {
  return userStore.user?.id === userId.value
})

const availableProducts = computed(() => {
  return products.value.filter(p => p.status === 'AVAILABLE')
})

async function fetchUserInfo() {
  userLoading.value = true
  userError.value = ''
  try {
    const res = await getUserById(userId.value)
    userInfo.value = res.data
  } catch {
    userError.value = '用户不存在或加载失败'
    userInfo.value = null
  } finally {
    userLoading.value = false
  }
}

async function fetchProducts() {
  productsLoading.value = true
  try {
    const res = await getUserProducts(userId.value)
    products.value = res.data?.records || res.data || []
  } catch {
    products.value = []
  } finally {
    productsLoading.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
  fetchProducts()
})
</script>

<style scoped>
.user-products-page {
  max-width: 1200px;
  margin: 0 auto;
}

.loading-skeleton {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  max-width: 600px;
  margin: 0 auto;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 28px 32px;
  background: #fff;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.user-avatar {
  flex-shrink: 0;
  border: 3px solid #e8eaed;
}

.user-meta {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 6px;
}

.user-school {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}

.user-bio {
  font-size: 14px;
  color: #909399;
  line-height: 1.5;
}

.products-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
}

.product-count {
  font-size: 14px;
  color: #909399;
  font-weight: 400;
  margin-left: 4px;
}

.loading-area {
  padding: 8px 0;
}

@media (max-width: 768px) {
  .user-header {
    flex-direction: column;
    text-align: center;
    padding: 20px 16px;
  }

  .products-section {
    padding: 16px;
  }
}
</style>
