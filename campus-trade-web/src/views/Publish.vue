<template>
  <div class="publish-page">
    <el-card class="publish-card" shadow="never">
      <template #header>
        <h2 class="card-title">发布商品</h2>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        class="publish-form"
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
            {{ submitting ? '发布中...' : '立即发布' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { create } from '../api/product'
import { CATEGORIES, CONDITIONS } from '../utils/constants'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

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
    const res = await create(data)
    ElMessage.success('发布成功')
    router.push(`/product/${res.data?.id || res.id}`)
  } catch {
    // handled in interceptor
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.publish-page {
  max-width: 680px;
  margin: 0 auto;
}

.publish-card {
  border-radius: 12px;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.publish-card :deep(.el-card__header) {
  padding: 20px 24px 0;
  border-bottom: none;
}

.publish-card :deep(.el-card__body) {
  padding: 24px;
}

.card-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.publish-form {
  margin-top: 8px;
}

.publish-form :deep(.el-form-item__label) {
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
  .publish-card :deep(.el-card__body) {
    padding: 16px;
  }

  .publish-form :deep(.el-form-item__label) {
    width: 64px !important;
  }
}
</style>
