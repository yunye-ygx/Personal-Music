<template>
  <div class="song-list">
    <!-- 空状态 -->
    <div v-if="songs.length === 0" class="empty-state">
      <p class="empty-text">暂无歌曲</p>
      <p class="empty-hint">点击"导入"按钮添加你的第一首歌曲</p>
    </div>

    <!-- 歌曲列表 -->
    <div v-else class="song-items">
      <div
        v-for="(song, index) in songs"
        :key="song.id"
        class="song-item"
        @click="$emit('play', song)"
      >
        <!-- 序号 -->
        <div class="song-index">{{ index + 1 }}</div>

        <!-- 歌曲信息 -->
        <div class="song-info">
          <div class="song-title">{{ song.title }}</div>
          <div class="song-artist">{{ song.artist }}</div>
        </div>

        <!-- 曲风 -->
        <div class="song-genre">{{ song.genre || '-' }}</div>

        <!-- 操作按钮 -->
        <div class="song-actions">
          <button
            class="btn-like"
            :class="{ liked: song.liked }"
            @click.stop="$emit('toggle-like', song.id)"
          >
            {{ song.liked ? '❤️' : '🤍' }}
          </button>
          <button class="btn-more" @click.stop>⋯</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  songs: {
    type: Array,
    required: true
  }
})

defineEmits(['play', 'toggle-like'])
</script>

<style scoped>
.song-list {
  width: 100%;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
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

/* 歌曲列表 */
.song-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.song-item {
  display: grid;
  grid-template-columns: 50px 1fr 120px 100px;
  align-items: center;
  gap: 20px;
  padding: 12px 20px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.song-item:hover {
  background-color: #282828;
}

/* 序号 */
.song-index {
  color: #b3b3b3;
  font-size: 16px;
  text-align: center;
}

/* 歌曲信息 */
.song-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.song-title {
  font-size: 16px;
  font-weight: 500;
  color: #ffffff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.song-artist {
  font-size: 14px;
  color: #b3b3b3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 曲风 */
.song-genre {
  font-size: 14px;
  color: #b3b3b3;
}

/* 操作按钮 */
.song-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
}

.btn-like {
  font-size: 20px;
  padding: 8px;
  opacity: 0.6;
  transition: opacity 0.2s, transform 0.2s;
}

.btn-like:hover {
  opacity: 1;
  transform: scale(1.1);
}

.btn-like.liked {
  opacity: 1;
}

.btn-more {
  font-size: 24px;
  padding: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.song-item:hover .btn-more {
  opacity: 0.6;
}

.btn-more:hover {
  opacity: 1;
}
</style>
