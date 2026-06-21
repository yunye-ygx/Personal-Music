// API 基础配置
const API_BASE_URL = 'http://localhost:8080'

// 获取所有歌曲
export const getAllSongs = async () => {
  const response = await fetch(`${API_BASE_URL}/api/songs`)
  if (!response.ok) {
    throw new Error('获取歌曲列表失败')
  }
  return response.json()
}

// 获取喜欢的歌曲
export const getLikedSongs = async () => {
  const response = await fetch(`${API_BASE_URL}/api/songs/liked`)
  if (!response.ok) {
    throw new Error('获取喜欢的歌曲失败')
  }
  return response.json()
}

// 切换喜欢状态
export const toggleLike = async (id) => {
  const response = await fetch(`${API_BASE_URL}/api/songs/${id}/like`, {
    method: 'PATCH'
  })
  if (!response.ok) {
    throw new Error('操作失败')
  }
  return response.json()
}

// 上传歌曲
export const uploadSong = async (file, title, artist, genre) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('title', title)
  formData.append('artist', artist)
  if (genre) {
    formData.append('genre', genre)
  }

  const response = await fetch(`${API_BASE_URL}/api/songs/upload`, {
    method: 'POST',
    body: formData
  })

  if (!response.ok) {
    throw new Error('上传失败')
  }
  return response.json()
}
