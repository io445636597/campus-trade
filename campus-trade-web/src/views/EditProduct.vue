<template>
  <div class="edit-page">
    <el-skeleton v-if="pageLoading" :rows="6" animated class="edit-skeleton" />

    <el-result
      v-else-if="accessError"
      icon="warning"
      title="无权访问"
      :sub-title="accessError"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </template>
    </el-result>

    <el-card v-else class="edit-card" shadow="never">
      <template #header>
        <h2 class="card-title">编辑商品</h2>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        class="edit-form"
      >
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入商品标题，最多100字"
            :maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="cat in CATEGORIES"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="成色" prop="condition">
          <el-select v-model="form.condition" placeholder="请选择成色" style="width: 100%">
            <el-option
              v-for="cond in CONDITIONS"
              :key="cond.value"
              :label="cond.label"
              :value="cond.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="价格" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            :precision="2"
            :step="1"
            placeholder="请输入价格"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            placeholder="请详细描述商品信息"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="图片地址" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="可选，输入图片链接" />
          <div class="form-tip">支持任意可访问的图片URL，留空则不展示图片</div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            @click="handleSubmit"
            size="large"
            style="width: 100%"
          >
            {{ submitting ? '保存中...' : '保存修改' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import { getDetail, update } from '../api/product'
import { CATEGORIES, CONDITIONS } from '../utils/constants'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const pageLoading = ref(true)
const submitting = ref(false)
const accessError = ref('')

const productId = Number(route.params.id)

const form = reactive({
  title: '',
  category: '',
  condition: '',
  price: null,
  description: '',
  imageUrl: ''
})

const rules = {
  title: [
    { required: true, message: '请输入商品标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过100个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  condition: [
    { required: true, message: '请选择成色', trigger: 'change' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '价格必须大于0', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入商品描述', trigger: 'blur' }
  ]
}

async function fetchProduct() {
  pageLoading.value = true
  try {
    const res = await getDetail(productId)
    const product = res.data

    // 验证是否是发布者
    if (!userStore.user || product.userId !== userStore.user.id) {
      accessError.value = '您不是该商品的发布者，无权编辑'
      return
    }

    form.title = product.title || ''
    form.category = product.category || ''
    form.condition = product.condition || ''
    form.price = product.price || null
    form.description = product.description || ''
    form.imageUrl = product.imageUrl || ''
  } catch {
    accessError.value = '商品不存在或已删除'
  } finally {
    pageLoading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请完善表单信息')
    return
  }

  submitting.value = true
  try {
    const data = {
      title: form.title.trim(),
      category: form.category,
      condition: form.condition,
      price: form.price,
      description: form.description.trim(),
      imageUrl: form.imageUrl.trim()
    }
    await update(productId, data)
    ElMessage.success('修改成功')
    router.push(`/product/${productId}`)
  } catch {
    // handled in interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchProduct()
})
</script>

<style scoped>
.edit-page {
  max-width: 680px;
  margin: 0 auto;
}

.edit-skeleton {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.edit-card {
  border-radius: 12px;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.edit-card :deep(.el-card__header) {
  padding: 20px 24px 0;
  border-bottom: none;
}

.edit-card :deep(.el-card__body) {
  padding: 24px;
}

.card-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.edit-form {
  margin-top: 8px;
}

.edit-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}

.form-tip {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .edit-card :deep(.el-card__body) {
    padding: 16px;
  }

  .edit-form :deep(.el-form-item__label) {
    width: 64px !important;
  }
}
</style>
