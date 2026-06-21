<template>
  <div class="player-bar">
    <!-- 左侧：当前播放歌曲信息 -->
    <div class="song-info">
      <img
        :src="currentSong.cover || '/placeholder-cover.png'"
        alt="封面"
        class="song-cover"
      />
      <div class="song-details">
        <div class="song-title">{{ currentSong.title || '未播放' }}</div>
        <div class="song-artist">{{ currentSong.artist || '-' }}</div>
      </div>
      <button class="btn-like" :class="{ liked: isLiked }">
        {{ isLiked ? '❤️' : '🤍' }}
      </button>
    </div>

    <!-- 中间：播放控制 -->
    <div class="player-controls">
      <div class="control-buttons">
        <button class="btn-control">⏮️</button>
        <button class="btn-play" @click="togglePlay">
          {{ isPlaying ? '⏸️' : '▶️' }}
        </button>
        <button class="btn-control">⏭️</button>
      </div>

      <div class="progress-bar">
        <span class="time-current">{{ formatTime(currentTime) }}</span>
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
          <div class="progress-thumb" :style="{ left: progressPercent + '%' }"></div>
        </div>
        <span class="time-total">{{ formatTime(duration) }}</span>
      </div>
    </div>

    <!-- 右侧：音量控制 -->
    <div class="volume-control">
      <button class="btn-volume">🔊</button>
      <div class="volume-slider">
        <div class="volume-track">
          <div class="volume-fill" :style="{ width: volume + '%' }"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

// 播放状态
const isPlaying = ref(false)
const isLiked = ref(false)
const currentTime = ref(0)
const duration = ref(180)
const volume = ref(70)

// 当前歌曲
const currentSong = ref({
  title: '',
  artist: '',
  cover: ''
})

// 进度百分比
const progressPercent = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

// 切换播放状态
const togglePlay = () => {
  isPlaying.value = !isPlaying.value
}

// 格式化时间
const formatTime = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}
</script>

<style scoped>
.player-bar {
  width: 100%;
  height: 90px;
  background-color: #181818;
  border-top: 1px solid #282828;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 20px;
}

/* 左侧：歌曲信息 */
.song-info {
  display: flex;
  align-items: center;
  gap: 15px;
  width: 300px;
}

.song-cover {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background-color: #282828;
  object-fit: cover;
}

.song-details {
  flex: 1;
}

.song-title {
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
  margin-bottom: 4px;
}

.song-artist {
  font-size: 12px;
  color: #b3b3b3;
}

.btn-like {
  font-size: 20px;
  padding: 8px;
  transition: transform 0.2s;
}

.btn-like:hover {
  transform: scale(1.1);
}

/* 中间：播放控制 */
.player-controls {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 600px;
}

.control-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
}

.btn-control {
  font-size: 24px;
  padding: 8px;
  transition: opacity 0.2s;
}

.btn-control:hover {
  opacity: 0.7;
}

.btn-play {
  width: 40px;
  height: 40px;
  background-color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  transition: transform 0.2s;
}

.btn-play:hover {
  transform: scale(1.05);
}

.progress-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.time-current,
.time-total {
  font-size: 12px;
  color: #b3b3b3;
  min-width: 40px;
}

.progress-track {
  flex: 1;
  height: 4px;
  background-color: #4d4d4d;
  border-radius: 2px;
  position: relative;
  cursor: pointer;
}

.progress-fill {
  height: 100%;
  background-color: #78ffd6;
  border-radius: 2px;
  transition: width 0.1s;
}

.progress-thumb {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 12px;
  height: 12px;
  background-color: #ffffff;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s;
}

.progress-track:hover .progress-thumb {
  opacity: 1;
}

/* 右侧：音量控制 */
.volume-control {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 150px;
}

.btn-volume {
  font-size: 20px;
  padding: 8px;
}

.volume-slider {
  flex: 1;
}

.volume-track {
  width: 100%;
  height: 4px;
  background-color: #4d4d4d;
  border-radius: 2px;
  position: relative;
  cursor: pointer;
}

.volume-fill {
  height: 100%;
  background-color: #ffffff;
  border-radius: 2px;
}
</style>
