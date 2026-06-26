<template>
  <div class="search-view">
    <div class="search-header">
      <h1 class="page-title">搜索结果</h1>
      <p class="search-keyword" v-if="keyword">关键词：{{ keyword }}</p>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading">
      <p>搜索中...</p>
    </div>

    <!-- 搜索结果 -->
    <div v-else-if="songs.length > 0" class="search-results">
      <p class="result-count">找到 {{ songs.length }} 首歌曲</p>
      <SongList :songs="songs" @play="handlePlay" @toggle-like="handleToggleLike" />
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <p class="empty-icon">🔍</p>
      <p class="empty-text">没有找到相关歌曲</p>
      <p class="empty-hint">试试其他关键词吧</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import SongList from '@/components/SongList.vue'
import { searchSongs, toggleLike } from '@/api/songs.js'

const route = useRoute()
const keyword = ref('')
const songs = ref([])
const loading = ref(false)

// 执行搜索
const performSearch = async () => {
  if (!keyword.value) return

  loading.value = true
  try {
    songs.value = await searchSongs(keyword.value)
  } catch (error) {
    console.error('搜索失败:', error)
    songs.value = []
  } finally {
    loading.value = false
  }
}

// 播放歌曲
const handlePlay = (song) => {
  console.log('播放歌曲:', song)
  // TODO: 实现播放逻辑
}

// 切换喜欢状态
const handleToggleLike = async (id) => {
  try {
    await toggleLike(id)
    const song = songs.value.find(s => s.id === id)
    if (song) {
      song.liked = !song.liked
    }
  } catch (error) {
    console.error('操作失败:', error)
  }
}

// 监听路由变化
watch(() => route.query.q, (newKeyword) => {
  if (newKeyword) {
    keyword.value = newKeyword
    performSearch()
  }
})

// 初始化
onMounted(() => {
  keyword.value = route.query.q || ''
  if (keyword.value) {
    performSearch()
  }
})
</script>

<style scoped>
.search-view {
  padding: 40px;
}

/* 搜索头部 */
.search-header {
  margin-bottom: 30px;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 10px;
}

.search-keyword {
  color: #b3b3b3;
  font-size: 16px;
}

/* 结果数量 */
.result-count {
  color: #b3b3b3;
  font-size: 14px;
  margin-bottom: 20px;
}

/* 加载中 */
.loading {
  text-align: center;
  padding: 60px 20px;
  color: #b3b3b3;
  font-size: 16px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.empty-text {
  color: #b3b3b3;
  font-size: 18px;
  margin-bottom: 10px;
}

.empty-hint {
  color: #666666;
  font-size: 14px;
}
</style>
