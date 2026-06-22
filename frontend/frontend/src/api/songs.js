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

// AI推荐 - SSE流式接口（使用POST + fetch解决中文编码问题）
export const getAIRecommendation = async (userMessage, onMessage, onError, onComplete) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ message: userMessage })
    })

    if (!response.ok) {
      throw new Error('请求失败')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // 处理SSE格式的数据
      const lines = buffer.split('\n')
      buffer = lines.pop() || '' // 保留未完成的行

      for (const line of lines) {
        if (line.startsWith('event:')) {
          const eventType = line.substring(6).trim()
          continue
        }

        if (line.startsWith('data:')) {
          const data = line.substring(5).trim()

          if (data === '') continue

          // 尝试解析为recommendation事件
          try {
            const parsed = JSON.parse(data)
            if (parsed.text !== undefined && parsed.song !== undefined) {
              // 这是recommendation事件
              onComplete(parsed)
              return
            }
          } catch (e) {
            // 不是JSON，是普通的message事件
            onMessage(data)
          }
        }
      }
    }
  } catch (error) {
    console.error('AI recommendation error:', error)
    onError('连接失败，请稍后重试')
  }
}

// ==================== 会话管理接口 ====================

// 创建新会话
export const createSession = async (title = '新会话') => {
  const response = await fetch(`${API_BASE_URL}/api/sessions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ title })
  })
  if (!response.ok) {
    throw new Error('创建会话失败')
  }
  return response.json()
}

// 获取所有会话列表
export const getAllSessions = async () => {
  const response = await fetch(`${API_BASE_URL}/api/sessions`)
  if (!response.ok) {
    throw new Error('获取会话列表失败')
  }
  return response.json()
}

// 获取会话的所有消息
export const getSessionMessages = async (sessionId) => {
  const response = await fetch(`${API_BASE_URL}/api/sessions/${sessionId}/messages`)
  if (!response.ok) {
    throw new Error('获取消息失败')
  }
  return response.json()
}

// 删除会话
export const deleteSession = async (sessionId) => {
  const response = await fetch(`${API_BASE_URL}/api/sessions/${sessionId}`, {
    method: 'DELETE'
  })
  if (!response.ok) {
    throw new Error('删除会话失败')
  }
}

// 发送消息并获取AI回复（流式）
export const sendMessage = async (sessionId, userMessage, onMessage, onError, onComplete) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/sessions/${sessionId}/messages`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ message: userMessage })
    })

    if (!response.ok) {
      throw new Error('请求失败')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // 处理SSE格式的数据
      const lines = buffer.split('\n')
      buffer = lines.pop() || '' // 保留未完成的行

      for (const line of lines) {
        if (line.startsWith('event:')) {
          continue
        }

        if (line.startsWith('data:')) {
          const data = line.substring(5).trim()
          if (data === '') continue

          // 尝试解析为recommendation事件
          try {
            const parsed = JSON.parse(data)
            if (parsed.text !== undefined && parsed.song !== undefined) {
              // 这是recommendation事件
              onComplete(parsed)
              return
            }
          } catch (e) {
            // 不是JSON，是普通的message事件
            onMessage(data)
          }
        }
      }
    }
  } catch (error) {
    console.error('Send message error:', error)
    onError('连接失败，请稍后重试')
  }
}
