import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const usePlayerStore = defineStore('player', () => {
  // 状态
  const currentSong = ref(null)        // 当前播放的歌曲
  const isPlaying = ref(false)         // 是否正在播放
  const playlist = ref([])             // 播放列表
  const currentIndex = ref(0)          // 当前播放索引
  const playMode = ref('sequence')     // 播放模式: sequence(顺序), loop(单曲循环), random(随机)
  const volume = ref(0.7)              // 音量 0-1
  const currentTime = ref(0)           // 当前播放时间（秒）
  const duration = ref(0)              // 歌曲总时长（秒）

  // 计算属性
  const hasNext = computed(() => {
    if (playMode.value === 'loop') return true
    return currentIndex.value < playlist.value.length - 1
  })

  const hasPrevious = computed(() => {
    if (playMode.value === 'loop') return true
    return currentIndex.value > 0
  })

  // 播放进度百分比
  const progress = computed(() => {
    if (duration.value === 0) return 0
    return (currentTime.value / duration.value) * 100
  })

  // Actions

  /**
   * 播放指定歌曲
   * @param {Object} song - 歌曲对象
   * @param {Array} newPlaylist - 可选，新的播放列表
   */
  const playSong = (song, newPlaylist = null) => {
    if (newPlaylist) {
      playlist.value = newPlaylist
      currentIndex.value = newPlaylist.findIndex(s => s.id === song.id)
    } else {
      // 如果没有传入新播放列表，查找当前列表中的索引
      const index = playlist.value.findIndex(s => s.id === song.id)
      if (index !== -1) {
        currentIndex.value = index
      } else {
        // 如果不在列表中，添加到末尾
        playlist.value.push(song)
        currentIndex.value = playlist.value.length - 1
      }
    }

    currentSong.value = song
    isPlaying.value = true
    currentTime.value = 0
  }

  /**
   * 播放/暂停切换
   */
  const togglePlay = () => {
    isPlaying.value = !isPlaying.value
  }

  /**
   * 下一曲
   */
  const playNext = () => {
    if (playlist.value.length === 0) return

    if (playMode.value === 'random') {
      // 随机模式
      const randomIndex = Math.floor(Math.random() * playlist.value.length)
      currentIndex.value = randomIndex
    } else if (playMode.value === 'loop') {
      // 单曲循环，不切歌
      currentTime.value = 0
    } else {
      // 顺序播放
      if (currentIndex.value < playlist.value.length - 1) {
        currentIndex.value++
      } else {
        currentIndex.value = 0  // 列表循环
      }
    }

    currentSong.value = playlist.value[currentIndex.value]
    isPlaying.value = true
  }

  /**
   * 上一曲
   */
  const playPrevious = () => {
    if (playlist.value.length === 0) return

    if (playMode.value === 'random') {
      // 随机模式
      const randomIndex = Math.floor(Math.random() * playlist.value.length)
      currentIndex.value = randomIndex
    } else {
      // 顺序播放
      if (currentIndex.value > 0) {
        currentIndex.value--
      } else {
        currentIndex.value = playlist.value.length - 1  // 回到最后一首
      }
    }

    currentSong.value = playlist.value[currentIndex.value]
    isPlaying.value = true
  }

  /**
   * 切换播放模式
   */
  const togglePlayMode = () => {
    const modes = ['sequence', 'loop', 'random']
    const currentModeIndex = modes.indexOf(playMode.value)
    playMode.value = modes[(currentModeIndex + 1) % modes.length]
  }

  /**
   * 设置音量
   * @param {number} val - 0-1
   */
  const setVolume = (val) => {
    volume.value = Math.max(0, Math.min(1, val))
  }

  /**
   * 更新播放时间
   * @param {number} time - 当前时间（秒）
   */
  const updateCurrentTime = (time) => {
    currentTime.value = time
  }

  /**
   * 更新总时长
   * @param {number} time - 总时长（秒）
   */
  const updateDuration = (time) => {
    duration.value = time
  }

  /**
   * 跳转到指定时间
   * @param {number} time - 目标时间（秒）
   */
  const seek = (time) => {
    currentTime.value = time
  }

  /**
   * 清空播放列表
   */
  const clearPlaylist = () => {
    playlist.value = []
    currentSong.value = null
    isPlaying.value = false
    currentIndex.value = 0
  }

  return {
    // 状态
    currentSong,
    isPlaying,
    playlist,
    currentIndex,
    playMode,
    volume,
    currentTime,
    duration,

    // 计算属性
    hasNext,
    hasPrevious,
    progress,

    // Actions
    playSong,
    togglePlay,
    playNext,
    playPrevious,
    togglePlayMode,
    setVolume,
    updateCurrentTime,
    updateDuration,
    seek,
    clearPlaylist
  }
})
