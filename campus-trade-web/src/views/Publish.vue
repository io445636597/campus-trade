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

        <el-form-item label="图片" prop="imageUrl">
          <el-upload
            class="image-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            accept="image/*"
            drag
          >
            <img v-if="form.imageUrl" :src="form.imageUrl" class="uploaded-image" />
            <div v-else>
              <el-icon :size="48"><UploadFilled /></el-icon>
              <div class="upload-text">点击或拖拽上传图片</div>
              <div class="upload-tip">支持 JPG/PNG，单张不超过 5MB</div>
            </div>
          </el-upload>
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
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { create } from '../api/product'
import { useUserStore } from '../store/user'
import { CATEGORIES, CONDITIONS } from '../utils/constants'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const submitting = ref(false)

const uploadUrl = computed(() => (import.meta.env.PROD
  ? 'https://campus-trade-89gs.onrender.com'
  : '') + '/api/product/upload/image')

const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + (userStore.token || localStorage.getItem('token') || '')
}))

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

function handleUploadSuccess(response) {
  if (response.code === 200 && response.data?.url) {
    form.imageUrl = response.data.url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function handleUploadError() {
  ElMessage.error('上传失败，请重试')
}

function beforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
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

.image-uploader {
  width: 100%;
}

.image-uploader :deep(.el-upload-dragger) {
  width: 100%;
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

.uploaded-image {
  max-width: 100%;
  max-height: 240px;
  border-radius: 8px;
  object-fit: contain;
}

.upload-text {
  font-size: 14px;
  color: #606266;
  margin-top: 8px;
}

.upload-tip {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
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
