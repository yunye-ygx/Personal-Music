<template>
  <div v-if="currentSong" class="audio-player">
    <!-- 隐藏的 audio 标签 -->
    <audio
      ref="audioElement"
      :src="currentSong.fileUrl"
      @play="handlePlay"
      @pause="handlePause"
      @timeupdate="handleTimeUpdate"
      @loadedmetadata="handleLoadedMetadata"
      @ended="handleEnded"
    ></audio>

    <!-- 播放器 UI -->
    <div class="player-content">
      <!-- 左侧：歌曲信息 -->
      <div class="song-info">
        <div class="cover">
          <img src="https://via.placeholder.com/56" alt="封面" />
        </div>
        <div class="meta">
          <div class="title">{{ currentSong.title }}</div>
          <div class="artist">{{ currentSong.artist }}</div>
        </div>
      </div>

      <!-- 中间：播放控制 -->
      <div class="player-controls">
        <!-- 控制按钮 -->
        <div class="control-buttons">
          <button class="btn-control" @click="playPrevious" :disabled="!hasPrevious">
            ⏮
          </button>
          <button class="btn-play" @click="togglePlay">
            {{ isPlaying ? '⏸' : '▶' }}
          </button>
          <button class="btn-control" @click="playNext" :disabled="!hasNext">
            ⏭
          </button>
          <button class="btn-mode" @click="togglePlayMode" :title="playModeText">
            {{ playModeIcon }}
          </button>
        </div>

        <!-- 进度条 -->
        <div class="progress-bar">
          <span class="time">{{ formatTime(currentTime) }}</span>
          <div class="slider-container" @click="handleProgressClick">
            <div class="slider-track">
              <div class="slider-fill" :style="{ width: progress + '%' }"></div>
              <div class="slider-thumb" :style="{ left: progress + '%' }"></div>
            </div>
          </div>
          <span class="time">{{ formatTime(duration) }}</span>
        </div>
      </div>

      <!-- 右侧：音量控制 -->
      <div class="volume-control">
        <button class="btn-volume" @click="toggleMute">
          {{ volume === 0 ? '🔇' : '🔊' }}
        </button>
        <div class="volume-slider">
          <input
            type="range"
            min="0"
            max="100"
            :value="volume * 100"
            @input="handleVolumeChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { usePlayerStore } from '@/stores/player'
import { storeToRefs } from 'pinia'

const playerStore = usePlayerStore()

// 从 store 获取状态（使用 storeToRefs 保持响应性）
const {
  currentSong,
  isPlaying,
  currentTime,
  duration,
  volume,
  progress,
  hasNext,
  hasPrevious,
  playMode
} = storeToRefs(playerStore)

// Audio 元素引用
const audioElement = ref(null)

// 播放模式文本和图标
const playModeText = computed(() => {
  const modeMap = {
    sequence: '顺序播放',
    loop: '单曲循环',
    random: '随机播放'
  }
  return modeMap[playMode.value]
})

const playModeIcon = computed(() => {
  const iconMap = {
    sequence: '🔁',
    loop: '🔂',
    random: '🔀'
  }
  return iconMap[playMode.value]
})

// 监听歌曲切换，自动播放
watch(currentSong, async (newSong) => {
  if (newSong && audioElement.value) {
    await nextTick()
    audioElement.value.load()
    if (isPlaying.value) {
      audioElement.value.play()
    }
  }
})

// 监听播放状态变化
watch(isPlaying, (playing) => {
  if (!audioElement.value) return

  if (playing) {
    audioElement.value.play()
  } else {
    audioElement.value.pause()
  }
})

// 监听音量变化
watch(volume, (newVolume) => {
  if (audioElement.value) {
    audioElement.value.volume = newVolume
  }
})

// 事件处理

const handlePlay = () => {
  playerStore.isPlaying = true
}

const handlePause = () => {
  playerStore.isPlaying = false
}

const handleTimeUpdate = () => {
  if (audioElement.value) {
    playerStore.updateCurrentTime(audioElement.value.currentTime)
  }
}

const handleLoadedMetadata = () => {
  if (audioElement.value) {
    playerStore.updateDuration(audioElement.value.duration)
  }
}

const handleEnded = () => {
  // 歌曲播放结束，自动下一曲
  playerStore.playNext()
}

const togglePlay = () => {
  playerStore.togglePlay()
}

const playNext = () => {
  playerStore.playNext()
}

const playPrevious = () => {
  playerStore.playPrevious()
}

const togglePlayMode = () => {
  playerStore.togglePlayMode()
}

const handleProgressClick = (event) => {
  const rect = event.currentTarget.getBoundingClientRect()
  const offsetX = event.clientX - rect.left
  const percentage = offsetX / rect.width
  const newTime = percentage * duration.value

  playerStore.seek(newTime)
  if (audioElement.value) {
    audioElement.value.currentTime = newTime
  }
}

const handleVolumeChange = (event) => {
  const newVolume = event.target.value / 100
  playerStore.setVolume(newVolume)
}

const toggleMute = () => {
  if (volume.value === 0) {
    playerStore.setVolume(0.7)
  } else {
    playerStore.setVolume(0)
  }
}

// 工具函数：格式化时间
const formatTime = (seconds) => {
  if (isNaN(seconds)) return '0:00'

  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}
</script>

<style scoped>
.audio-player {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 90px;
  background: #181818;
  border-top: 1px solid #282828;
  z-index: 100;
}

.player-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 20px;
  gap: 20px;
}

/* 左侧：歌曲信息 */
.song-info {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 0 0 300px;
}

.cover img {
  width: 56px;
  height: 56px;
  border-radius: 4px;
}

.meta {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.artist {
  font-size: 12px;
  color: #b3b3b3;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 中间：播放控制 */
.player-controls {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  max-width: 600px;
}

.control-buttons {
  display: flex;
  align-items: center;
  gap: 15px;
}

.btn-control,
.btn-play,
.btn-mode {
  background: transparent;
  border: none;
  color: #b3b3b3;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.2s;
  padding: 5px;
}

.btn-control:hover:not(:disabled),
.btn-mode:hover {
  color: #ffffff;
  transform: scale(1.1);
}

.btn-control:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.btn-play {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #ffffff;
  color: #000000;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-play:hover {
  transform: scale(1.1);
}

.btn-mode {
  font-size: 16px;
}

/* 进度条 */
.progress-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.time {
  font-size: 12px;
  color: #b3b3b3;
  min-width: 40px;
  text-align: center;
}

.slider-container {
  flex: 1;
  padding: 10px 0;
  cursor: pointer;
}

.slider-track {
  position: relative;
  height: 4px;
  background: #404040;
  border-radius: 2px;
}

.slider-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: #78ffd6;
  border-radius: 2px;
  transition: width 0.1s;
}

.slider-thumb {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 12px;
  height: 12px;
  background: #ffffff;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s;
}

.slider-container:hover .slider-thumb {
  opacity: 1;
}

/* 右侧：音量控制 */
.volume-control {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 150px;
}

.btn-volume {
  background: transparent;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #b3b3b3;
  transition: color 0.2s;
}

.btn-volume:hover {
  color: #ffffff;
}

.volume-slider {
  flex: 1;
}

.volume-slider input[type="range"] {
  width: 100%;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: #404040;
  border-radius: 2px;
  outline: none;
}

.volume-slider input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 12px;
  height: 12px;
  background: #ffffff;
  border-radius: 50%;
  cursor: pointer;
}

.volume-slider input[type="range"]::-moz-range-thumb {
  width: 12px;
  height: 12px;
  background: #ffffff;
  border-radius: 50%;
  cursor: pointer;
  border: none;
}
</style>
