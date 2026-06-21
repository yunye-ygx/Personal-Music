<template>
  <div class="my-music-view">
    <!-- 顶部标题和操作按钮 -->
    <div class="header">
      <h1 class="page-title">我的音乐</h1>
      <div class="actions">
        <button class="btn-action" @click="showImportDialog = true">
          <span class="icon">⬇️</span>
          导入
        </button>
        <button class="btn-action" @click="handleExport">
          <span class="icon">⬆️</span>
          导出
        </button>
        <button class="btn-action btn-select">
          <span class="icon">✔️</span>
          多选
        </button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">加载中...</div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error">
      <p class="error-text">{{ error }}</p>
      <button class="btn-retry" @click="fetchSongs">重试</button>
    </div>

    <!-- 歌曲列表 -->
    <SongList
      v-else
      :songs="songs"
      @play="handlePlay"
      @toggle-like="handleToggleLike"
    />

    <!-- 导入对话框 -->
    <div v-if="showImportDialog" class="dialog-overlay" @click.self="closeImportDialog">
      <div class="dialog-content">
        <div class="dialog-header">
          <h2>导入音乐</h2>
          <button class="btn-close" @click="closeImportDialog">✕</button>
        </div>

        <!-- 方式选择器 -->
        <div class="import-tabs">
          <button
            :class="['tab-btn', { active: activeTab === 'upload' }]"
            @click="activeTab = 'upload'"
          >
            上传本地文件
          </button>
          <button
            :class="['tab-btn', { active: activeTab === 'download' }]"
            @click="activeTab = 'download'"
          >
            从网易云下载
          </button>
        </div>

        <!-- 上传本地文件 -->
        <div v-if="activeTab === 'upload'" class="import-panel">
          <div class="form-group">
            <label>选择文件</label>
            <input
              type="file"
              accept="audio/*"
              @change="handleFileSelect"
              ref="fileInput"
            />
            <p v-if="selectedFile" class="file-info">
              已选择: {{ selectedFile.name }} ({{ formatFileSize(selectedFile.size) }})
            </p>
          </div>

          <div class="form-group">
            <label>歌曲名称 *</label>
            <input
              v-model="uploadForm.title"
              type="text"
              placeholder="例如：晴天"
            />
          </div>

          <div class="form-group">
            <label>艺术家 *</label>
            <input
              v-model="uploadForm.artist"
              type="text"
              placeholder="例如：周杰伦"
            />
          </div>

          <div class="form-group">
            <label>流派</label>
            <input
              v-model="uploadForm.genre"
              type="text"
              placeholder="例如：流行（可选）"
            />
          </div>

          <button
            class="btn-submit"
            @click="handleUpload"
            :disabled="uploading || !selectedFile"
          >
            {{ uploading ? '上传中...' : '开始上传' }}
          </button>
        </div>

        <!-- 从网易云下载 -->
        <div v-if="activeTab === 'download'" class="import-panel">
          <div class="form-group">
            <label>粘贴分享链接</label>
            <input
              v-model="shareUrl"
              type="text"
              placeholder="例如：https://music.163.com/#/song?id=123456"
              @keyup.enter="handleDownload"
            />
          </div>

          <button
            class="btn-submit"
            @click="handleDownload"
            :disabled="downloading || !shareUrl"
          >
            {{ downloading ? '下载中...' : '解析并下载' }}
          </button>

          <p class="tip">💡 提示：在网易云音乐中点击"分享"按钮，复制链接粘贴到这里</p>
        </div>

        <!-- 结果提示 -->
        <div v-if="resultMessage" :class="['result-message', resultType]">
          {{ resultMessage }}
        </div>

        <!-- 批量导入失败列表 -->
        <div v-if="batchResult && batchResult.failCount > 0" class="failed-list">
          <h4>以下 {{ batchResult.failCount }} 首歌曲导入失败：</h4>
          <div class="failed-items">
            <div v-for="failed in batchResult.failedSongs" :key="failed.songId" class="failed-item">
              <div class="failed-info">
                <span class="failed-title">{{ failed.title }}</span>
                <span v-if="failed.artist" class="failed-artist">- {{ failed.artist }}</span>
              </div>
              <span class="failed-reason">{{ failed.reason }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import SongList from '@/components/SongList.vue'
import { getAllSongs, toggleLike } from '@/api/songs'
import { usePlayerStore } from '@/stores/player'

const playerStore = usePlayerStore()

// 状态
const songs = ref([])
const loading = ref(false)
const error = ref(null)

// 导入对话框状态
const showImportDialog = ref(false)
const activeTab = ref('upload')

// 上传相关
const selectedFile = ref(null)
const fileInput = ref(null)
const uploadForm = ref({
  title: '',
  artist: '',
  genre: ''
})
const uploading = ref(false)

// 下载相关
const shareUrl = ref('')
const downloading = ref(false)

// 结果提示
const resultMessage = ref('')
const resultType = ref('success')
const batchResult = ref(null) // 批量导入结果

// 获取歌曲列表
const fetchSongs = async () => {
  loading.value = true
  error.value = null
  try {
    songs.value = await getAllSongs()
  } catch (err) {
    error.value = '获取歌曲列表失败，请检查后端服务是否启动'
    console.error(err)
  } finally {
    loading.value = false
  }
}

// 播放歌曲
const handlePlay = (song) => {
  // 使用 Pinia store 播放歌曲
  playerStore.playSong(song, songs.value)
}

// 切换喜欢
const handleToggleLike = async (id) => {
  try {
    const updatedSong = await toggleLike(id)
    // 更新本地数据
    const index = songs.value.findIndex(s => s.id === id)
    if (index !== -1) {
      songs.value[index] = updatedSong
    }
  } catch (err) {
    alert('操作失败')
    console.error(err)
  }
}

// 导出歌曲
const handleExport = () => {
  alert('导出功能开发中...')
  // TODO: 实现导出功能
}

// 关闭导入对话框
const closeImportDialog = () => {
  showImportDialog.value = false
  resetUploadForm()
  shareUrl.value = ''
  resultMessage.value = ''
  batchResult.value = null
}

// 选择文件
const handleFileSelect = (event) => {
  const file = event.target.files[0]
  if (file) {
    selectedFile.value = file

    // 自动从文件名提取歌名
    if (!uploadForm.value.title) {
      const nameWithoutExt = file.name.replace(/\.[^/.]+$/, '')
      uploadForm.value.title = nameWithoutExt
    }
  }
}

// 上传本地文件
const handleUpload = async () => {
  if (!selectedFile.value) {
    showResult('请选择文件', 'error')
    return
  }

  if (!uploadForm.value.title || !uploadForm.value.artist) {
    showResult('歌曲名称和艺术家不能为空', 'error')
    return
  }

  uploading.value = true
  resultMessage.value = ''

  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('title', uploadForm.value.title)
    formData.append('artist', uploadForm.value.artist)
    formData.append('genre', uploadForm.value.genre || '未知')

    const response = await axios.post('http://localhost:8080/api/import/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    showResult(`上传成功：${response.data.title} - ${response.data.artist}`, 'success')
    resetUploadForm()

    // 刷新歌曲列表
    setTimeout(() => {
      fetchSongs()
      closeImportDialog()
    }, 1500)

  } catch (error) {
    console.error('上传失败:', error)
    showResult('上传失败: ' + (error.response?.data?.message || error.message), 'error')
  } finally {
    uploading.value = false
  }
}

// 从网易云下载
const handleDownload = async () => {
  if (!shareUrl.value) {
    showResult('请输入分享链接', 'error')
    return
  }

  downloading.value = true
  resultMessage.value = ''
  batchResult.value = null

  try {
    const response = await axios.post('http://localhost:8080/api/import/netease/download', {
      url: shareUrl.value
    })

    const data = response.data

    // 判断是单曲还是批量导入
    if (data.total !== undefined) {
      // 批量导入结果
      batchResult.value = data

      if (data.failCount === 0) {
        showResult(`批量导入成功：共 ${data.successCount} 首歌曲`, 'success')
      } else {
        showResult(`导入完成：成功 ${data.successCount} 首，失败 ${data.failCount} 首`, 'warning')
      }
    } else {
      // 单曲导入结果
      showResult(`下载成功：${data.title} - ${data.artist}`, 'success')
      shareUrl.value = ''
    }

    // 刷新歌曲列表
    setTimeout(() => {
      fetchSongs()
      if (data.total === undefined || data.failCount === 0) {
        closeImportDialog()
      }
    }, 1500)

  } catch (error) {
    console.error('下载失败:', error)
    showResult('下载失败: ' + (error.response?.data?.message || error.message), 'error')
  } finally {
    downloading.value = false
  }
}

// 显示结果提示
const showResult = (message, type) => {
  resultMessage.value = message
  resultType.value = type

  // 3秒后自动消失
  setTimeout(() => {
    resultMessage.value = ''
  }, 3000)
}

// 重置上传表单
const resetUploadForm = () => {
  selectedFile.value = null
  uploadForm.value = {
    title: '',
    artist: '',
    genre: ''
  }
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 页面加载时获取数据
onMounted(() => {
  fetchSongs()
})
</script>

<style scoped>
.my-music-view {
  width: 100%;
}

/* 顶部 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
}

.actions {
  display: flex;
  gap: 15px;
}

.btn-action {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background-color: #282828;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.2s;
}

.btn-action:hover {
  background-color: #3e3e3e;
}

.btn-action .icon {
  font-size: 16px;
}

.btn-select {
  background-color: #1a1a1a;
  border: 1px solid #404040;
}

/* 加载状态 */
.loading {
  text-align: center;
  padding: 60px 20px;
  color: #b3b3b3;
  font-size: 18px;
}

/* 错误状态 */
.error {
  text-align: center;
  padding: 60px 20px;
}

.error-text {
  color: #ff6b6b;
  font-size: 16px;
  margin-bottom: 20px;
}

.btn-retry {
  padding: 10px 30px;
  background-color: #78ffd6;
  color: #121212;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  transition: transform 0.2s;
}

.btn-retry:hover {
  transform: translateY(-2px);
}

/* 对话框遮罩 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* 对话框内容 */
.dialog-content {
  background: #1a1a1a;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 30px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

/* 对话框头部 */
.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dialog-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #ffffff;
}

.btn-close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #282828;
  color: #b3b3b3;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #3e3e3e;
  color: #ffffff;
}

/* 标签页 */
.import-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  border-bottom: 2px solid #282828;
}

.tab-btn {
  padding: 12px 24px;
  border: none;
  background: transparent;
  font-size: 15px;
  font-weight: 500;
  color: #8a8a80;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.3s;
}

.tab-btn:hover {
  color: #ffffff;
}

.tab-btn.active {
  color: #78ffd6;
  border-bottom-color: #78ffd6;
}

/* 面板 */
.import-panel {
  padding: 20px 0;
}

/* 表单 */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #b3b3b3;
}

.form-group input[type="text"],
.form-group input[type="file"] {
  padding: 12px;
  border: 2px solid #282828;
  border-radius: 8px;
  font-size: 14px;
  background: #0a0a0a;
  color: #ffffff;
  transition: border-color 0.3s;
}

.form-group input[type="text"]:focus {
  outline: none;
  border-color: #78ffd6;
}

.file-info {
  font-size: 13px;
  color: #8a8a80;
  margin-top: 4px;
}

/* 按钮 */
.btn-submit {
  width: 100%;
  padding: 14px 28px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #121212;
  background: #78ffd6;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 10px;
}

.btn-submit:hover:not(:disabled) {
  background: #5de7be;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(120, 255, 214, 0.3);
}

.btn-submit:disabled {
  background: #404040;
  color: #666666;
  cursor: not-allowed;
  transform: none;
}

/* 提示 */
.tip {
  font-size: 13px;
  color: #8a8a80;
  margin-top: 10px;
  padding: 10px;
  background: #0a0a0a;
  border-radius: 6px;
  border-left: 3px solid #78ffd6;
}

/* 结果提示 */
.result-message {
  margin-top: 20px;
  padding: 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
}

.result-message.success {
  background: #1a4d2e;
  color: #78ffd6;
  border: 1px solid #2a7d4e;
}

.result-message.error {
  background: #4d1a1a;
  color: #ff6b6b;
  border: 1px solid #7d2a2a;
}

.result-message.warning {
  background: #4d3a1a;
  color: #ffc078;
  border: 1px solid #7d5a2a;
}

/* 失败列表 */
.failed-list {
  margin-top: 20px;
  padding: 15px;
  background: #1a1a1a;
  border-radius: 8px;
  border: 1px solid #3a3a3a;
}

.failed-list h4 {
  color: #ff6b6b;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.failed-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.failed-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: #0f0f0f;
  border-radius: 6px;
  border: 1px solid #2a2a2a;
}

.failed-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 5px;
}

.failed-title {
  color: #e0e0e0;
  font-size: 14px;
  font-weight: 500;
}

.failed-artist {
  color: #888888;
  font-size: 13px;
}

.failed-reason {
  color: #ff6b6b;
  font-size: 12px;
  padding: 4px 8px;
  background: #2a1a1a;
  border-radius: 4px;
  white-space: nowrap;
}
</style>
