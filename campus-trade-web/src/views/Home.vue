<template>
  <div class="home-page">
    <div class="hero-section">
      <h1 class="hero-title">校园二手交易平台</h1>
      <p class="hero-subtitle">在校园内自由买卖闲置物品，让你的旧物找到新主人</p>
      <SearchBar v-model="keyword" @search="handleSearch" :loading="loading" />
    </div>

    <!-- 热门商品 -->
    <div v-if="hotProducts.length > 0 && !keyword" class="hot-section">
      <div class="section-title">
        <span class="title-text">热门排行</span>
        <span class="title-badge">TOP {{ hotProducts.length }}</span>
      </div>
      <el-row :gutter="16">
        <el-col
          v-for="(product, index) in hotProducts"
          :key="'hot-' + product.id"
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

    <CategoryFilter
      v-model:category="category"
      v-model:condition="condition"
      v-model:sort="sort"
      @change="handleFilterChange"
    />

    <div v-if="loading" class="loading-area">
      <el-skeleton :rows="4" animated />
      <el-row :gutter="16" style="margin-top:16px">
        <el-col v-for="i in 8" :key="i" :xs="12" :sm="6" :md="6" :lg="6">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="width:100%;height:160px" />
              <el-skeleton-item variant="text" style="width:80%;margin-top:8px" />
              <el-skeleton-item variant="text" style="width:50%" />
            </template>
          </el-skeleton>
        </el-col>
      </el-row>
    </div>

    <div v-else-if="products.length === 0" class="empty-area">
      <el-empty description="暂无商品，快去发布第一个吧" :image-size="120" />
    </div>

    <div v-else>
      <div class="result-info">
        共找到 <strong>{{ total }}</strong> 件商品
      </div>
      <el-row :gutter="16">
        <el-col
          v-for="product in products"
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

      <Pagination
        :total="total"
        :page="page"
        :size="size"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SearchBar from '../components/SearchBar.vue'
import CategoryFilter from '../components/CategoryFilter.vue'
import ProductCard from '../components/ProductCard.vue'
import Pagination from '../components/Pagination.vue'
import { getList, getHot } from '../api/product'

const route = useRoute()
const router = useRouter()

const hotProducts = ref([])
const products = ref([])
const total = ref(0)
const loading = ref(false)
const keyword = ref(route.query.keyword || '')
const category = ref(route.query.category || '')
const condition = ref(route.query.condition || '')
const sort = ref(route.query.sort || 'newest')
const page = ref(1)
const size = ref(12)

async function fetchProducts() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
      sort: sort.value
    }
    if (category.value) params.category = category.value
    if (condition.value) params.condition = condition.value
    if (keyword.value) params.keyword = keyword.value

    const res = await getList(params)
    products.value = res.data?.records || res.data?.content || res.data || []
    total.value = res.data?.total || res.data?.totalElements || 0
  } catch {
    products.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch(val) {
  keyword.value = val
  page.value = 1
  updateQuery()
  fetchProducts()
}

function handleFilterChange() {
  page.value = 1
  updateQuery()
  fetchProducts()
}

function handlePageChange(p) {
  page.value = p
  updateQuery()
  fetchProducts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleSizeChange(s) {
  size.value = s
  page.value = 1
  updateQuery()
  fetchProducts()
}

function updateQuery() {
  const query = {}
  if (keyword.value) query.keyword = keyword.value
  if (category.value) query.category = category.value
  if (condition.value) query.condition = condition.value
  if (sort.value && sort.value !== 'newest') query.sort = sort.value
  router.replace({ query })
}

async function fetchHot() {
  try {
    const res = await getHot()
    hotProducts.value = res.data || []
  } catch {
    hotProducts.value = []
  }
}

onMounted(() => {
  fetchHot()
  fetchProducts()
})
</script>

<style scoped>
.home-page {
  max-width: 1200px;
  margin: 0 auto;
}

.hero-section {
  text-align: center;
  padding: 32px 20px 24px;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  border-radius: 12px;
  margin-bottom: 24px;
  color: #fff;
}

.hero-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.hero-subtitle {
  font-size: 15px;
  margin-bottom: 20px;
  opacity: 0.9;
}

.hero-section :deep(.search-bar) {
  max-width: 560px;
  margin: 0 auto;
}

.hot-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px 20px 4px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f56c6c;
}

.title-text {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.title-badge {
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #f56c6c, #e6a23c);
  padding: 2px 10px;
  border-radius: 10px;
}

.hero-section :deep(.search-input .el-input-group__append) {
  background-color: #fff;
  border-color: #fff;
  color: #409eff;
}

.loading-area {
  padding: 20px 0;
}

.empty-area {
  padding: 60px 0;
}

.result-info {
  font-size: 14px;
  color: #909399;
  margin-bottom: 16px;
}

.result-info strong {
  color: #409eff;
}
</style>
