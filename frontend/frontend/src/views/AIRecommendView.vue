<template>
  <div class="ai-recommend-view">
    <!-- 左侧会话列表 -->
    <aside class="conversations-sidebar">
      <div class="sidebar-header">
        <h2 class="sidebar-title">对话列表</h2>
        <button @click="createNewConversation" class="new-chat-btn" title="新建对话">
          <span class="plus-icon">+</span>
        </button>
      </div>

      <div class="conversations-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          @click="selectConversation(conv.id)"
          :class="['conversation-item', { active: currentConversationId === conv.id }]"
        >
          <div class="conversation-preview">
            <h3 class="conversation-title">{{ conv.title || '新对话' }}</h3>
            <p class="conversation-last-message">{{ conv.lastMessage || '还没有消息' }}</p>
          </div>
          <div class="conversation-time">{{ formatTime(conv.updatedAt) }}</div>
        </div>

        <div v-if="conversations.length === 0" class="empty-state">
          <p>还没有对话</p>
          <p class="empty-hint">点击右上角 + 创建新对话</p>
        </div>
      </div>
    </aside>

    <!-- 右侧聊天区域 -->
    <main class="chat-main">
      <div v-if="!currentConversation" class="welcome-screen">
        <div class="welcome-content">
          <h1 class="welcome-title">✨ AI 音乐推荐</h1>
          <p class="welcome-subtitle">告诉我你的心情，让我为你推荐最合适的音乐</p>

          <div class="quick-actions">
            <h3>💡 试试这些问法：</h3>
            <button @click="startWithExample('我现在心情有点低落，想听点治愈的歌')" class="example-btn">
              "我现在心情有点低落，想听点治愈的歌"
            </button>
            <button @click="startWithExample('来点轻快欢乐的音乐')" class="example-btn">
              "来点轻快欢乐的音乐"
            </button>
            <button @click="startWithExample('想听伤感一点的歌')" class="example-btn">
              "想听伤感一点的歌"
            </button>
          </div>
        </div>
      </div>

      <div v-else class="chat-area">
        <!-- 对话标题栏 -->
        <div class="chat-header">
          <h2 class="chat-title">{{ currentConversation.title || '新对话' }}</h2>
          <button @click="deleteConversation" class="delete-btn" title="删除对话">
            🗑️
          </button>
        </div>

        <!-- 消息列表 -->
        <div class="messages-container" ref="messagesContainer">
          <div
            v-for="(message, index) in currentMessages"
            :key="index"
            :class="['message', message.role]"
          >
            <!-- 用户消息 -->
            <div v-if="message.role === 'user'" class="message-bubble user-bubble">
              <div class="message-text">{{ message.content }}</div>
              <div class="user-avatar">👤</div>
            </div>

            <!-- AI 消息 -->
            <div v-else class="message-bubble ai-bubble">
              <div class="ai-avatar">🤖</div>
              <div class="ai-message-content">
                <div class="message-text">{{ message.content }}</div>

                <!-- 推荐的歌曲卡片 -->
                <div v-if="message.song" class="song-card">
                  <div class="song-cover">
                    <div class="cover-placeholder">🎵</div>
                  </div>
                  <div class="song-info">
                    <h4 class="song-title">{{ message.song.title }}</h4>
                    <p class="song-artist">{{ message.song.artist }}</p>
                    <div class="song-meta">
                      <span class="genre-tag">{{ message.song.genre || '未分类' }}</span>
                      <span v-for="tag in message.song.moodTags" :key="tag" class="mood-tag">
                        {{ tag }}
                      </span>
                    </div>
                  </div>
                  <button @click="playSong(message.song)" class="play-btn">
                    <span class="play-icon">▶</span>
                    播放
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 加载指示器 -->
          <div v-if="loading" class="message assistant">
            <div class="message-bubble ai-bubble">
              <div class="ai-avatar">🤖</div>
              <div class="ai-message-content">
                <div class="typing-indicator">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <div v-if="error" class="error-message">
            ⚠️ {{ error }}
          </div>

          <div class="input-wrapper">
            <textarea
              v-model="userInput"
              placeholder="告诉我你现在的心情或想听什么类型的音乐..."
              class="message-input"
              rows="1"
              @keydown.enter.exact.prevent="handleSubmit"
              @input="autoResize"
              ref="inputArea"
            ></textarea>
            <button
              @click="handleSubmit"
              :disabled="loading || !userInput.trim()"
              class="send-btn"
              title="发送 (Enter)"
            >
              <span class="send-icon">➤</span>
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { sendMessage, createSession, getAllSessions, getSessionMessages, deleteSession as apiDeleteSession } from '@/api/songs'
import { usePlayerStore } from '@/stores/player'

const playerStore = usePlayerStore()

// 会话数据
const conversations = ref([])
const currentConversationId = ref(null)
const conversationMessages = ref({}) // 缓存会话消息 { sessionId: [...messages] }
const userInput = ref('')
const loading = ref(false)
const error = ref('')
const messagesContainer = ref(null)
const inputArea = ref(null)

// 当前会话
const currentConversation = computed(() => {
  return conversations.value.find(c => c.id === currentConversationId.value)
})

// 当前会话的消息
const currentMessages = computed(() => {
  if (!currentConversationId.value) return []
  return conversationMessages.value[currentConversationId.value] || []
})

// 初始化
onMounted(async () => {
  await loadConversations()
  if (conversations.value.length > 0) {
    await selectConversation(conversations.value[0].id)
  }
})

// 监听当前会话变化，滚动到底部
watch(currentConversationId, () => {
  nextTick(() => {
    scrollToBottom()
  })
})

// 从后端加载会话列表
const loadConversations = async () => {
  try {
    const sessions = await getAllSessions()
    conversations.value = sessions

    // 如果没有会话，创建第一个
    if (conversations.value.length === 0) {
      await createNewConversation()
    }
  } catch (e) {
    console.error('加载会话列表失败:', e)
    error.value = '加载会话列表失败'
  }
}

// 创建新会话
const createNewConversation = async () => {
  try {
    const newSession = await createSession('新会话')
    conversations.value.unshift(newSession)
    currentConversationId.value = newSession.id
    conversationMessages.value[newSession.id] = []
  } catch (e) {
    console.error('创建会话失败:', e)
    error.value = '创建会话失败'
  }
}

// 选择会话
const selectConversation = async (id) => {
  currentConversationId.value = id
  error.value = ''

  // 如果还未加载该会话的消息，从后端加载
  if (!conversationMessages.value[id]) {
    try {
      const messages = await getSessionMessages(id)
      // 转换后端消息格式为前端格式
      conversationMessages.value[id] = messages.map(msg => ({
        role: msg.senderType === 'USER' ? 'user' : 'assistant',
        content: msg.textContent,
        song: msg.song || null
      }))
    } catch (e) {
      console.error('加载消息失败:', e)
      error.value = '加载消息失败'
      conversationMessages.value[id] = []
    }
  }
}

// 删除会话
const deleteConversation = async () => {
  if (!confirm('确定要删除这个对话吗？')) return

  try {
    await apiDeleteSession(currentConversationId.value)

    const index = conversations.value.findIndex(c => c.id === currentConversationId.value)
    conversations.value.splice(index, 1)
    delete conversationMessages.value[currentConversationId.value]

    if (conversations.value.length > 0) {
      await selectConversation(conversations.value[0].id)
    } else {
      currentConversationId.value = null
      await createNewConversation()
    }
  } catch (e) {
    console.error('删除会话失败:', e)
    error.value = '删除会话失败'
  }
}

// 用示例开始新对话
const startWithExample = async (example) => {
  if (!currentConversation.value || currentMessages.value.length > 0) {
    await createNewConversation()
  }
  userInput.value = example
  nextTick(() => {
    handleSubmit()
  })
}

// 提交消息
const handleSubmit = async () => {
  if (!userInput.value.trim() || loading.value || !currentConversation.value) return

  const messageText = userInput.value.trim()
  userInput.value = ''
  error.value = ''

  // 添加用户消息到本地缓存
  const messages = conversationMessages.value[currentConversationId.value]
  messages.push({
    role: 'user',
    content: messageText
  })

  scrollToBottom()
  loading.value = true

  // 准备AI消息
  const aiMessage = {
    role: 'assistant',
    content: '',
    song: null
  }

  messages.push(aiMessage)

  try {
    await sendMessage(
      currentConversationId.value,
      messageText,
      (chunk) => {
        // 接收流式文本
        aiMessage.content += chunk
        scrollToBottom()
      },
      (errorMsg) => {
        // 错误处理
        error.value = errorMsg
        loading.value = false
        // 移除未完成的AI消息
        messages.pop()
      },
      (data) => {
        // 接收完整推荐结果（替换流式累加的内容）
        aiMessage.content = data.text || aiMessage.content
        if (data.song) {
          aiMessage.song = data.song
        }
        loading.value = false
        scrollToBottom()

        // 刷新会话列表以更新lastMessage和updatedAt
        loadConversations()
      }
    )
  } catch (e) {
    error.value = '推荐失败，请稍后重试'
    loading.value = false
    messages.pop()
  }
}

// 播放歌曲
const playSong = (song) => {
  if (song) {
    playerStore.playSong(song, [song])
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 自动调整输入框高度
const autoResize = () => {
  if (inputArea.value) {
    inputArea.value.style.height = 'auto'
    inputArea.value.style.height = Math.min(inputArea.value.scrollHeight, 120) + 'px'
  }
}

// 格式化时间
const formatTime = (isoString) => {
  if (!isoString) return ''
  const date = new Date(isoString)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  return date.toLocaleDateString()
}
</script>

<style scoped>
.ai-recommend-view {
  display: flex;
  height: 100vh;
  background: #121212;
}

/* 左侧会话列表 */
.conversations-sidebar {
  width: 280px;
  background: #1a1a1a;
  border-right: 1px solid #2a2a2a;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 20px 16px;
  border-bottom: 1px solid #2a2a2a;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar-title {
  font-size: 18px;
  font-weight: 600;
  color: #e0e0e0;
  margin: 0;
}

.new-chat-btn {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #a8ff78 0%, #78ffd6 100%);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.new-chat-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(168, 255, 120, 0.3);
}

.plus-icon {
  font-size: 24px;
  color: #000;
  font-weight: 300;
}

.conversations-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conversation-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.conversation-item:hover {
  background: #2a2a2a;
}

.conversation-item.active {
  background: #2a2a2a;
  border-color: #a8ff78;
}

.conversation-preview {
  margin-bottom: 6px;
}

.conversation-title {
  font-size: 14px;
  font-weight: 500;
  color: #e0e0e0;
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-last-message {
  font-size: 12px;
  color: #888;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-time {
  font-size: 11px;
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #666;
}

.empty-hint {
  font-size: 12px;
  margin-top: 8px;
}

/* 右侧聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.welcome-screen {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.welcome-content {
  max-width: 600px;
  text-align: center;
}

.welcome-title {
  font-size: 48px;
  font-weight: bold;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #a8ff78 0%, #78ffd6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-subtitle {
  font-size: 18px;
  color: #b3b3b3;
  margin-bottom: 48px;
}

.quick-actions {
  text-align: left;
}

.quick-actions h3 {
  font-size: 16px;
  color: #e0e0e0;
  margin-bottom: 16px;
}

.example-btn {
  display: block;
  width: 100%;
  background: #1a1a1a;
  border: 1px solid #3a3a3a;
  border-radius: 12px;
  padding: 14px 18px;
  color: #b3b3b3;
  font-size: 14px;
  cursor: pointer;
  text-align: left;
  margin-bottom: 12px;
  transition: all 0.3s;
}

.example-btn:hover {
  background: #2a2a2a;
  border-color: #a8ff78;
  color: #a8ff78;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  padding: 20px 24px;
  border-bottom: 1px solid #2a2a2a;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-title {
  font-size: 18px;
  font-weight: 600;
  color: #e0e0e0;
  margin: 0;
}

.delete-btn {
  background: transparent;
  border: none;
  font-size: 18px;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.2s;
  padding: 6px;
}

.delete-btn:hover {
  opacity: 1;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.message {
  margin-bottom: 20px;
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 70%;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

/* 用户消息 — 右对齐，头像在右侧 */
.user-bubble {
  flex-direction: row-reverse;
  background: linear-gradient(135deg, #a8ff78 0%, #78ffd6 100%);
  border-radius: 18px 18px 4px 18px;
  padding: 12px 16px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.user-bubble .message-text {
  color: #000;
  font-size: 15px;
  line-height: 1.5;
}

/* AI 消息 — 左对齐，头像在左侧 */
.ai-bubble {
  background: transparent;
  padding: 0;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.ai-message-content {
  background: #1a1a1a;
  border-radius: 4px 18px 18px 18px;
  padding: 12px 16px;
}

.ai-message-content .message-text {
  color: #e0e0e0;
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.song-card {
  margin-top: 16px;
  background: #2a2a2a;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #3a3a3a;
}

.song-cover {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-placeholder {
  font-size: 32px;
}

.song-info {
  flex: 1;
  min-width: 0;
}

.song-title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-artist {
  font-size: 13px;
  color: #b3b3b3;
  margin: 0 0 10px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.genre-tag {
  background: #3a3a3a;
  color: #a8ff78;
  padding: 3px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.mood-tag {
  background: rgba(168, 255, 120, 0.15);
  color: #a8ff78;
  padding: 3px 10px;
  border-radius: 10px;
  font-size: 11px;
}

.play-btn {
  background: linear-gradient(135deg, #a8ff78 0%, #78ffd6 100%);
  border: none;
  border-radius: 20px;
  padding: 10px 20px;
  color: #000;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s;
  flex-shrink: 0;
}

.play-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(168, 255, 120, 0.4);
}

.play-icon {
  font-size: 12px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #a8ff78;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-6px);
  }
}

/* 输入区域 */
.input-area {
  padding: 16px 24px;
  border-top: 1px solid #2a2a2a;
  background: #121212;
}

.error-message {
  background: rgba(255, 100, 100, 0.1);
  border: 1px solid rgba(255, 100, 100, 0.3);
  border-radius: 8px;
  padding: 12px;
  color: #ff6464;
  margin-bottom: 12px;
  font-size: 14px;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
  background: #1a1a1a;
  border: 2px solid #2a2a2a;
  border-radius: 12px;
  padding: 12px 16px;
  color: #fff;
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  font-family: inherit;
  max-height: 120px;
  overflow-y: auto;
  transition: border-color 0.3s;
}

.message-input:focus {
  outline: none;
  border-color: #a8ff78;
}

.message-input::placeholder {
  color: #666;
}

.send-btn {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #a8ff78 0%, #78ffd6 100%);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(168, 255, 120, 0.4);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-icon {
  font-size: 18px;
  color: #000;
}

/* 滚动条样式 */
.conversations-list::-webkit-scrollbar,
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.conversations-list::-webkit-scrollbar-track,
.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.conversations-list::-webkit-scrollbar-thumb,
.messages-container::-webkit-scrollbar-thumb {
  background: #3a3a3a;
  border-radius: 3px;
}

.conversations-list::-webkit-scrollbar-thumb:hover,
.messages-container::-webkit-scrollbar-thumb:hover {
  background: #4a4a4a;
}
</style>
