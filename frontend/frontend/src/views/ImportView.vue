<template>
  <div class="import-view">
    <h1>导入音乐</h1>

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
      <h2>上传本地音乐文件</h2>

      <div class="upload-form">
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
          class="upload-btn"
          @click="handleUpload"
          :disabled="uploading || !selectedFile"
        >
          {{ uploading ? '上传中...' : '开始上传' }}
        </button>
      </div>
    </div>

    <!-- 从网易云下载 -->
    <div v-if="activeTab === 'download'" class="import-panel">
      <h2>从网易云音乐下载</h2>

      <div class="download-form">
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
          class="download-btn"
          @click="handleDownload"
          :disabled="downloading || !shareUrl"
        >
          {{ downloading ? '下载中...' : '解析并下载' }}
        </button>

        <p class="tip">💡 提示：在网易云音乐中点击"分享"按钮，复制链接粘贴到这里</p>
      </div>
    </div>

    <!-- 结果提示 -->
    <div v-if="resultMessage" :class="['result-message', resultType]">
      {{ resultMessage }}
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'ImportView',
  data() {
    return {
      activeTab: 'upload',  // 'upload' 或 'download'

      // 上传相关
      selectedFile: null,
      uploadForm: {
        title: '',
        artist: '',
        genre: ''
      },
      uploading: false,

      // 下载相关
      shareUrl: '',
      downloading: false,

      // 结果提示
      resultMessage: '',
      resultType: 'success'  // 'success' 或 'error'
    };
  },
  methods: {
    // 选择文件
    handleFileSelect(event) {
      const file = event.target.files[0];
      if (file) {
        this.selectedFile = file;

        // 自动从文件名提取歌名（去掉扩展名）
        if (!this.uploadForm.title) {
          const nameWithoutExt = file.name.replace(/\.[^/.]+$/, '');
          this.uploadForm.title = nameWithoutExt;
        }
      }
    },

    // 上传本地文件
    async handleUpload() {
      if (!this.selectedFile) {
        this.showResult('请选择文件', 'error');
        return;
      }

      if (!this.uploadForm.title || !this.uploadForm.artist) {
        this.showResult('歌曲名称和艺术家不能为空', 'error');
        return;
      }

      this.uploading = true;
      this.resultMessage = '';

      try {
        const formData = new FormData();
        formData.append('file', this.selectedFile);
        formData.append('title', this.uploadForm.title);
        formData.append('artist', this.uploadForm.artist);
        formData.append('genre', this.uploadForm.genre || '未知');

        const response = await axios.post('http://localhost:8080/api/import/upload', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        });

        this.showResult(`上传成功：${response.data.title} - ${response.data.artist}`, 'success');

        // 清空表单
        this.resetUploadForm();

      } catch (error) {
        console.error('上传失败:', error);
        this.showResult('上传失败: ' + (error.response?.data?.message || error.message), 'error');
      } finally {
        this.uploading = false;
      }
    },

    // 从网易云下载
    async handleDownload() {
      if (!this.shareUrl) {
        this.showResult('请输入分享链接', 'error');
        return;
      }

      this.downloading = true;
      this.resultMessage = '';

      try {
        const response = await axios.post('http://localhost:8080/api/import/netease/download', {
          url: this.shareUrl
        });

        this.showResult(`下载成功：${response.data.title} - ${response.data.artist}`, 'success');

        // 清空输入框
        this.shareUrl = '';

      } catch (error) {
        console.error('下载失败:', error);
        this.showResult('下载失败: ' + (error.response?.data?.message || error.message), 'error');
      } finally {
        this.downloading = false;
      }
    },

    // 显示结果提示
    showResult(message, type) {
      this.resultMessage = message;
      this.resultType = type;

      // 3秒后自动消失
      setTimeout(() => {
        this.resultMessage = '';
      }, 3000);
    },

    // 重置上传表单
    resetUploadForm() {
      this.selectedFile = null;
      this.uploadForm = {
        title: '',
        artist: '',
        genre: ''
      };
      if (this.$refs.fileInput) {
        this.$refs.fileInput.value = '';
      }
    },

    // 格式化文件大小
    formatFileSize(bytes) {
      if (bytes < 1024) return bytes + ' B';
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
      return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
    }
  }
};
</script>

<style scoped>
.import-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
}

h1 {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 30px;
  color: #1f2421;
}

/* 标签页 */
.import-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 30px;
  border-bottom: 2px solid #e2d9c8;
}

.tab-btn {
  padding: 12px 24px;
  border: none;
  background: transparent;
  font-size: 16px;
  font-weight: 500;
  color: #8a8a80;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.3s;
}

.tab-btn:hover {
  color: #c8853f;
}

.tab-btn.active {
  color: #c8853f;
  border-bottom-color: #c8853f;
}

/* 面板 */
.import-panel {
  background: #fbf7ef;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

h2 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #2a2723;
}

/* 表单 */
.upload-form,
.download-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #1f2421;
}

.form-group input[type="text"],
.form-group input[type="file"] {
  padding: 12px;
  border: 2px solid #e2d9c8;
  border-radius: 8px;
  font-size: 14px;
  background: #ffffff;
  transition: border-color 0.3s;
}

.form-group input[type="text"]:focus {
  outline: none;
  border-color: #c8853f;
}

.file-info {
  font-size: 13px;
  color: #8a8a80;
  margin-top: 4px;
}

/* 按钮 */
.upload-btn,
.download-btn {
  padding: 14px 28px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  background: #c8853f;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 10px;
}

.upload-btn:hover:not(:disabled),
.download-btn:hover:not(:disabled) {
  background: #a86b2c;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(200, 133, 63, 0.3);
}

.upload-btn:disabled,
.download-btn:disabled {
  background: #8a8a80;
  cursor: not-allowed;
  transform: none;
}

/* 提示 */
.tip {
  font-size: 13px;
  color: #8a8a80;
  margin-top: 10px;
  padding: 10px;
  background: #f0e3d0;
  border-radius: 6px;
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
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.result-message.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}
</style>
