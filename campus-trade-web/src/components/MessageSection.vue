<template>
  <div class="message-section">
    <h3 class="section-title">
      <el-icon><ChatDotRound /></el-icon>
      留言区 ({{ messages.length }})
    </h3>

    <!-- 发送留言 -->
    <div v-if="userStore.isLoggedIn" class="send-box">
      <el-input
        v-model="newMessage"
        type="textarea"
        :rows="3"
        placeholder="写下你的留言..."
        maxlength="500"
        show-word-limit
      />
      <div class="send-footer">
        <el-button type="primary" :loading="sending" @click="handleSend">
          发送留言
        </el-button>
      </div>
    </div>
    <div v-else class="login-hint">
      <el-alert
        title="登录后才能留言"
        type="info"
        :closable="false"
        show-icon
      >
        <template #default>
          <el-button type="primary" link @click="$router.push('/login')">去登录</el-button>
        </template>
      </el-alert>
    </div>

    <!-- 留言列表 -->
    <div v-if="loading" class="loading-area">
      <el-skeleton :rows="3" animated />
    </div>
    <div v-else-if="messages.length === 0" class="empty-area">
      <el-empty description="暂无留言，快来第一个留言吧" :image-size="80" />
    </div>
    <div v-else class="message-list">
      <div v-for="msg in messages" :key="msg.id" class="message-item">
        <el-avatar :size="36" :src="msg.user?.avatar" class="msg-avatar">
          {{ (msg.user?.username || 'U').charAt(0).toUpperCase() }}
        </el-avatar>
        <div class="msg-content">
          <div class="msg-header">
            <span class="msg-username">{{ msg.user?.username || '未知用户' }}</span>
            <span class="msg-time">{{ formatRelativeTime(msg.createdAt) }}</span>
          </div>
          <p class="msg-text">{{ msg.content }}</p>
        </div>
        <el-button
          v-if="canDelete(msg)"
          type="danger"
          link
          size="small"
          :loading="deletingId === msg.id"
          @click="handleDelete(msg.id)"
        >
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'
import { getList as getMessages, send, remove } from '../api/message'

const props = defineProps({
  productId: { type: [Number, String], required: true },
  productOwnerId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:count'])

const userStore = useUserStore()
const messages = ref([])
const newMessage = ref('')
const loading = ref(false)
const sending = ref(false)
const deletingId = ref(null)

function canDelete(msg) {
  if (!userStore.user) return false
  return msg.user?.id === userStore.user.id || userStore.user.id === props.productOwnerId
}

async function fetchMessages() {
  loading.value = true
  try {
    const res = await getMessages(props.productId)
    messages.value = res.data || []
    emit('update:count', messages.value.length)
  } catch {
    messages.value = []
  } finally {
    loading.value = false
  }
}

async function handleSend() {
  const content = newMessage.value.trim()
  if (!content) {
    ElMessage.warning('请输入留言内容')
    return
  }

  sending.value = true
  try {
    await send(props.productId, content)
    newMessage.value = ''
    ElMessage.success('留言成功')
    await fetchMessages()
  } catch {
    // 已在拦截器中处理
  } finally {
    sending.value = false
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定要删除这条留言吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  deletingId.value = id
  try {
    await remove(id)
    ElMessage.success('删除成功')
    await fetchMessages()
  } catch {
    // 已在拦截器中处理
  } finally {
    deletingId.value = null
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

// 暴露给父组件调用
defineExpose({ fetchMessages })

onMounted(() => fetchMessages())
</script>

<style scoped>
.message-section {
  margin-top: 32px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
}

.send-box {
  margin-bottom: 20px;
}

.send-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.login-hint {
  margin-bottom: 20px;
}

.loading-area,
.empty-area {
  padding: 20px 0;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
  padding: 14px;
  background: #f9fafb;
  border-radius: 8px;
  transition: background 0.2s;
}

.message-item:hover {
  background: #f0f2f5;
}

.msg-avatar {
  flex-shrink: 0;
}

.msg-content {
  flex: 1;
  min-width: 0;
}

.msg-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.msg-username {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.msg-time {
  font-size: 12px;
  color: #c0c4cc;
}

.msg-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  word-break: break-all;
}
</style>
