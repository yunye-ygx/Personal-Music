<template>
  <nav class="navbar">
    <!-- Logo -->
    <div class="logo">
      <span class="logo-icon">🎵</span>
      <span class="logo-text">MoodTune</span>
    </div>

    <!-- 导航链接 -->
    <div class="nav-links">
      <RouterLink to="/discover" class="nav-link">发现音乐</RouterLink>
      <RouterLink to="/charts" class="nav-link">排行榜</RouterLink>
      <RouterLink to="/ai-recommend" class="nav-link ai-highlight">
        <span class="ai-icon">✨</span>
        AI推荐
      </RouterLink>
      <RouterLink to="/radio" class="nav-link">电台</RouterLink>
      <RouterLink to="/my-music" class="nav-link">我的音乐</RouterLink>
    </div>

    <!-- 搜索框 -->
    <div class="search-box">
      <input
        type="text"
        placeholder="搜索歌曲、艺术家..."
        v-model="searchKeyword"
        @keyup.enter="handleSearch"
      />
      <span class="search-icon" @click="handleSearch">🔍</span>
    </div>
  </nav>
</template>

<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()
const searchKeyword = ref('')

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({
      name: 'search',
      query: { q: searchKeyword.value.trim() }
    })
  }
}
</script>

<style scoped>
.navbar {
  width: 100%;
  height: 70px;
  background-color: #1a1a1a;
  display: flex;
  align-items: center;
  padding: 0 40px;
  gap: 40px;
  border-bottom: 1px solid #282828;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 24px;
  font-weight: bold;
}

.logo-icon {
  font-size: 32px;
}

/* 导航链接 */
.nav-links {
  display: flex;
  gap: 30px;
  flex: 1;
}

.nav-link {
  color: #b3b3b3;
  font-size: 16px;
  font-weight: 500;
  transition: color 0.2s;
  padding: 8px 16px;
  border-radius: 20px;
}

.nav-link:hover {
  color: #ffffff;
}

.nav-link.router-link-active {
  color: #ffffff;
}

/* AI推荐特殊样式 */
.ai-highlight {
  background: linear-gradient(135deg, #a8ff78 0%, #78ffd6 100%);
  color: #121212 !important;
  font-weight: 600;
  box-shadow: 0 4px 15px rgba(168, 255, 120, 0.3);
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-highlight:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(168, 255, 120, 0.4);
}

.ai-icon {
  font-size: 18px;
}

/* 搜索框 */
.search-box {
  position: relative;
  width: 300px;
}

.search-box input {
  width: 100%;
  height: 40px;
  background-color: #282828;
  border: 1px solid transparent;
  border-radius: 20px;
  padding: 0 40px 0 20px;
  color: #ffffff;
  font-size: 14px;
  transition: border-color 0.2s, background-color 0.2s;
}

.search-box input:focus {
  outline: none;
  border-color: #78ffd6;
  background-color: #1a1a1a;
}

.search-box input::placeholder {
  color: #666666;
}

.search-icon {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  color: #666666;
  cursor: pointer;
  transition: color 0.2s;
}

.search-icon:hover {
  color: #78ffd6;
}
</style>
