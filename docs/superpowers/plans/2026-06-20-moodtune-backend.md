# MoodTune Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the MoodTune backend — a Spring Boot service that manages songs, chat sessions, and provides AI-powered music recommendations via SSE streaming.

**Architecture:** Standard Spring Boot layered architecture (Controller → Service → Repository → Entity). LLM integration via HTTP client to domestic AI APIs. SSE for streaming chat responses. MinIO for song file storage.

**Tech Stack:** Java 17+, Spring Boot 3.x, Spring Data JPA, PostgreSQL, Redis, MinIO SDK, SSE (SseEmitter)

**Spec:** `docs/superpowers/specs/2026-06-20-moodtune-design.md`

---

## Phase 1: Project Scaffolding

### Task 1: Create Spring Boot Project Structure

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/moodtune/MoodTuneApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/moodtune/MoodTuneApplicationTest.java`
- Create: `backend/.gitignore`

- [ ] **Step 1: Create backend directory and pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <groupId>com.moodtune</groupId>
    <artifactId>moodtune-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>moodtune-backend</name>
    <description>MoodTune - Private Music Assistant Backend</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Data JPA + PostgreSQL -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- MinIO -->
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>8.5.10</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create main application class**

```java
package com.moodtune;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MoodTuneApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoodTuneApplication.class, args);
    }
}
```

- [ ] **Step 3: Create application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/moodtune
    username: postgres
    password: your_password_here
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  data:
    redis:
      host: localhost
      port: 6379
      password:

# MinIO Configuration
minio:
  endpoint: http://localhost:9000
  access-key: your_minio_access_key
  secret-key: your_minio_secret_key
  bucket-name: moodtune-songs

# LLM Configuration
llm:
  api-url: https://your-llm-api-endpoint.com
  api-key: your_api_key_here
  model: your_model_name
```

- [ ] **Step 4: Create test application class**

```java
package com.moodtune;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MoodTuneApplicationTest {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 5: Create .gitignore**

```
target/
*.class
*.jar
*.log
.idea/
*.iml
.DS_Store
node_modules/
dist/
.env
```

- [ ] **Step 6: Verify project compiles**

Run: `cd backend && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git init
git add backend/
git commit -m "feat: scaffold Spring Boot project with dependencies"
```

---

## Phase 2: Entity & Repository Layer

### Task 2: Song Entity and Repository

**Files:**
- Create: `backend/src/main/java/com/moodtune/entity/Song.java`
- Create: `backend/src/main/java/com/moodtune/repository/SongRepository.java`
- Create: `backend/src/test/java/com/moodtune/repository/SongRepositoryTest.java`

- [ ] **Step 1: Create Song entity**

```java
package com.moodtune.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "songs")
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    private String genre;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(nullable = false)
    private Boolean liked = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: Create SongRepository**

```java
package com.moodtune.repository;

import com.moodtune.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SongRepository extends JpaRepository<Song, Long> {
    List<Song> findByLikedTrue();
    List<Song> findByGenre(String genre);
    List<Song> findByTitleContainingOrArtistContaining(String title, String artist);
}
```

- [ ] **Step 3: Write repository test**

```java
package com.moodtune.repository;

import com.moodtune.entity.Song;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SongRepositoryTest {

    @Autowired
    private SongRepository songRepository;

    @Test
    void saveAndFindSong() {
        Song song = new Song();
        song.setTitle("爱错");
        song.setArtist("王力宏");
        song.setGenre("流行");
        song.setFileUrl("http://minio/test.mp3");
        song.setLiked(true);

        Song saved = songRepository.save(song);
        assertNotNull(saved.getId());
        assertEquals("爱错", saved.getTitle());
    }

    @Test
    void findLikedSongs() {
        Song s1 = new Song();
        s1.setTitle("Song A");
        s1.setArtist("Artist A");
        s1.setLiked(true);
        s1.setFileUrl("http://minio/a.mp3");
        songRepository.save(s1);

        Song s2 = new Song();
        s2.setTitle("Song B");
        s2.setArtist("Artist B");
        s2.setLiked(false);
        s2.setFileUrl("http://minio/b.mp3");
        songRepository.save(s2);

        List<Song> liked = songRepository.findByLikedTrue();
        assertEquals(1, liked.size());
        assertEquals("Song A", liked.get(0).getTitle());
    }
}
```

- [ ] **Step 4: Run tests**

Run: `cd backend && mvn test -pl . -Dtest=SongRepositoryTest`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/moodtune/entity/Song.java backend/src/main/java/com/moodtune/repository/SongRepository.java backend/src/test/java/com/moodtune/repository/SongRepositoryTest.java
git commit -m "feat: add Song entity and repository with tests"
```

### Task 3: SongGroup and SongGroupRelation Entity

**Files:**
- Create: `backend/src/main/java/com/moodtune/entity/SongGroup.java`
- Create: `backend/src/main/java/com/moodtune/entity/SongGroupRelation.java`
- Create: `backend/src/main/java/com/moodtune/repository/SongGroupRepository.java`
- Create: `backend/src/main/java/com/moodtune/repository/SongGroupRelationRepository.java`
- Create: `backend/src/test/java/com/moodtune/repository/SongGroupRepositoryTest.java`

- [ ] **Step 1: Create SongGroup entity**

```java
package com.moodtune.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "song_groups")
public class SongGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String source; // "imported" or "manual"

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: Create SongGroupRelation entity**

```java
package com.moodtune.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "song_group_relations")
public class SongGroupRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;
}
```

- [ ] **Step 3: Create repositories**

```java
package com.moodtune.repository;

import com.moodtune.entity.SongGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SongGroupRepository extends JpaRepository<SongGroup, Long> {
    List<SongGroup> findBySource(String source);
}
```

```java
package com.moodtune.repository;

import com.moodtune.entity.SongGroupRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SongGroupRelationRepository extends JpaRepository<SongGroupRelation, Long> {
    List<SongGroupRelation> findByGroupId(Long groupId);
    List<SongGroupRelation> findBySongId(Long songId);
    void deleteBySongIdAndGroupId(Long songId, Long groupId);
}
```

- [ ] **Step 4: Write tests**

```java
package com.moodtune.repository;

import com.moodtune.entity.SongGroup;
import com.moodtune.entity.SongGroupRelation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SongGroupRepositoryTest {

    @Autowired
    private SongGroupRepository groupRepository;

    @Autowired
    private SongGroupRelationRepository relationRepository;

    @Test
    void createGroupAndRelations() {
        SongGroup group = new SongGroup();
        group.setName("睡前听的歌");
        group.setSource("manual");
        SongGroup savedGroup = groupRepository.save(group);

        SongGroupRelation rel = new SongGroupRelation();
        rel.setSongId(1L);
        rel.setGroupId(savedGroup.getId());
        relationRepository.save(rel);

        List<SongGroupRelation> relations = relationRepository.findByGroupId(savedGroup.getId());
        assertEquals(1, relations.size());
    }

    @Test
    void findGroupsBySource() {
        SongGroup g1 = new SongGroup();
        g1.setName("导入歌单");
        g1.setSource("imported");
        groupRepository.save(g1);

        SongGroup g2 = new SongGroup();
        g2.setName("手动分组");
        g2.setSource("manual");
        groupRepository.save(g2);

        List<SongGroup> imported = groupRepository.findBySource("imported");
        assertEquals(1, imported.size());
        assertEquals("导入歌单", imported.get(0).getName());
    }
}
```

- [ ] **Step 5: Run tests**

Run: `cd backend && mvn test -Dtest=SongGroupRepositoryTest`
Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/
git commit -m "feat: add SongGroup and SongGroupRelation entities and repositories"
```

### Task 4: SongImport Entity

**Files:**
- Create: `backend/src/main/java/com/moodtune/entity/SongImport.java`
- Create: `backend/src/main/java/com/moodtune/repository/SongImportRepository.java`

- [ ] **Step 1: Create SongImport entity**

```java
package com.moodtune.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "song_imports")
public class SongImport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_link", nullable = false)
    private String sourceLink;

    @Column(nullable = false)
    private String platform; // "qq_music", "netease", etc.

    @Column(nullable = false)
    private String status; // "pending", "success", "failed"

    @Column(name = "imported_count")
    private Integer importedCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: Create repository**

```java
package com.moodtune.repository;

import com.moodtune.entity.SongImport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SongImportRepository extends JpaRepository<SongImport, Long> {
    List<SongImport> findByStatus(String status);
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/
git commit -m "feat: add SongImport entity and repository"
```

### Task 5: ChatSession and ChatMessage Entity

**Files:**
- Create: `backend/src/main/java/com/moodtune/entity/ChatSession.java`
- Create: `backend/src/main/java/com/moodtune/entity/ChatMessage.java`
- Create: `backend/src/main/java/com/moodtune/repository/ChatSessionRepository.java`
- Create: `backend/src/main/java/com/moodtune/repository/ChatMessageRepository.java`
- Create: `backend/src/test/java/com/moodtune/repository/ChatRepositoryTest.java`

- [ ] **Step 1: Create ChatSession entity**

```java
package com.moodtune.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_sessions")
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: Create ChatMessage entity**

```java
package com.moodtune.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "sender_type", nullable = false)
    private String senderType; // "user" or "ai"

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "song_id")
    private Long songId; // nullable, only for AI messages with recommendation

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: Create repositories**

```java
package com.moodtune.repository;

import com.moodtune.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findAllByOrderByUpdatedAtDesc();
}
```

```java
package com.moodtune.repository;

import com.moodtune.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
```

- [ ] **Step 4: Write tests**

```java
package com.moodtune.repository;

import com.moodtune.entity.ChatSession;
import com.moodtune.entity.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ChatRepositoryTest {

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Test
    void createSessionAndMessages() {
        ChatSession session = new ChatSession();
        session.setTitle("今天好累");
        ChatSession savedSession = sessionRepository.save(session);
        assertNotNull(savedSession.getId());

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(savedSession.getId());
        userMsg.setSenderType("user");
        userMsg.setTextContent("今天好累，想听点歌");
        messageRepository.save(userMsg);

        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(savedSession.getId());
        aiMsg.setSenderType("ai");
        aiMsg.setTextContent("给你推荐一首安静的歌");
        aiMsg.setSongId(1L);
        messageRepository.save(aiMsg);

        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(savedSession.getId());
        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).getSenderType());
        assertEquals("ai", messages.get(1).getSenderType());
        assertNotNull(messages.get(1).getSongId());
    }
}
```

- [ ] **Step 5: Run tests**

Run: `cd backend && mvn test -Dtest=ChatRepositoryTest`
Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/
git commit -m "feat: add ChatSession and ChatMessage entities and repositories"
```

---

## Phase 3: Service Layer (Song Module)

### Task 6: SongService

**Files:**
- Create: `backend/src/main/java/com/moodtune/service/SongService.java`
- Create: `backend/src/main/java/com/moodtune/dto/SongDTO.java`
- Create: `backend/src/test/java/com/moodtune/service/SongServiceTest.java`

- [ ] **Step 1: Create SongDTO**

```java
package com.moodtune.dto;

import lombok.Data;

@Data
public class SongDTO {
    private Long id;
    private String title;
    private String artist;
    private String genre;
    private String fileUrl;
    private Boolean liked;
}
```

- [ ] **Step 2: Create SongService**

```java
package com.moodtune.service;

import com.moodtune.dto.SongDTO;
import com.moodtune.entity.Song;
import com.moodtune.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SongService {

    private final SongRepository songRepository;

    public List<SongDTO> getAllSongs() {
        return songRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<SongDTO> getLikedSongs() {
        return songRepository.findByLikedTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public SongDTO getSongById(Long id) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found: " + id));
        return toDTO(song);
    }

    @Transactional
    public SongDTO addSong(SongDTO dto) {
        Song song = new Song();
        song.setTitle(dto.getTitle());
        song.setArtist(dto.getArtist());
        song.setGenre(dto.getGenre());
        song.setFileUrl(dto.getFileUrl());
        song.setLiked(dto.getLiked() != null ? dto.getLiked() : false);
        return toDTO(songRepository.save(song));
    }

    @Transactional
    public SongDTO toggleLiked(Long id) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found: " + id));
        song.setLiked(!song.getLiked());
        return toDTO(songRepository.save(song));
    }

    private SongDTO toDTO(Song song) {
        SongDTO dto = new SongDTO();
        dto.setId(song.getId());
        dto.setTitle(song.getTitle());
        dto.setArtist(song.getArtist());
        dto.setGenre(song.getGenre());
        dto.setFileUrl(song.getFileUrl());
        dto.setLiked(song.getLiked());
        return dto;
    }
}
```

- [ ] **Step 3: Write service test (using H2 in-memory DB)**

```java
package com.moodtune.service;

import com.moodtune.dto.SongDTO;
import com.moodtune.entity.Song;
import com.moodtune.repository.SongRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SongServiceTest {

    @Autowired
    private SongService songService;

    @Autowired
    private SongRepository songRepository;

    @Test
    void addAndGetSong() {
        SongDTO dto = new SongDTO();
        dto.setTitle("测试歌曲");
        dto.setArtist("测试歌手");
        dto.setGenre("流行");
        dto.setFileUrl("http://minio/test.mp3");

        SongDTO saved = songService.addSong(dto);
        assertNotNull(saved.getId());
        assertEquals("测试歌曲", saved.getTitle());
        assertFalse(saved.getLiked());
    }

    @Test
    void toggleLiked() {
        Song song = new Song();
        song.setTitle("Toggle Test");
        song.setArtist("Artist");
        song.setFileUrl("http://minio/toggle.mp3");
        song.setLiked(false);
        Song saved = songRepository.save(song);

        SongDTO toggled = songService.toggleLiked(saved.getId());
        assertTrue(toggled.getLiked());

        SongDTO toggledAgain = songService.toggleLiked(saved.getId());
        assertFalse(toggledAgain.getLiked());
    }
}
```

- [ ] **Step 4: Run tests**

Run: `cd backend && mvn test -Dtest=SongServiceTest`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/
git commit -m "feat: add SongService with CRUD and toggle liked"
```

### Task 7: SongGroupService

**Files:**
- Create: `backend/src/main/java/com/moodtune/service/SongGroupService.java`
- Create: `backend/src/main/java/com/moodtune/dto/SongGroupDTO.java`

- [ ] **Step 1: Create SongGroupDTO**

```java
package com.moodtune.dto;

import lombok.Data;
import java.util.List;

@Data
public class SongGroupDTO {
    private Long id;
    private String name;
    private String source;
    private List<SongDTO> songs;
}
```

- [ ] **Step 2: Create SongGroupService**

```java
package com.moodtune.service;

import com.moodtune.dto.SongDTO;
import com.moodtune.dto.SongGroupDTO;
import com.moodtune.entity.SongGroup;
import com.moodtune.entity.SongGroupRelation;
import com.moodtune.repository.SongGroupRelationRepository;
import com.moodtune.repository.SongGroupRepository;
import com.moodtune.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SongGroupService {

    private final SongGroupRepository groupRepository;
    private final SongGroupRelationRepository relationRepository;
    private final SongRepository songRepository;
    private final SongService songService;

    public List<SongGroupDTO> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SongGroupDTO createGroup(String name, String source) {
        SongGroup group = new SongGroup();
        group.setName(name);
        group.setSource(source);
        return toDTO(groupRepository.save(group));
    }

    @Transactional
    public void addSongToGroup(Long songId, Long groupId) {
        SongGroupRelation rel = new SongGroupRelation();
        rel.setSongId(songId);
        rel.setGroupId(groupId);
        relationRepository.save(rel);
    }

    @Transactional
    public void removeSongFromGroup(Long songId, Long groupId) {
        relationRepository.deleteBySongIdAndGroupId(songId, groupId);
    }

    private SongGroupDTO toDTO(SongGroup group) {
        SongGroupDTO dto = new SongGroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setSource(group.getSource());

        List<SongGroupRelation> relations = relationRepository.findByGroupId(group.getId());
        List<SongDTO> songs = relations.stream()
                .map(rel -> songService.getSongById(rel.getSongId()))
                .collect(Collectors.toList());
        dto.setSongs(songs);

        return dto;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/
git commit -m "feat: add SongGroupService with group management"
```

---

## Phase 4: LLM Integration & Recommendation Engine

### Task 8: LLM Client

**Files:**
- Create: `backend/src/main/java/com/moodtune/config/LlmConfig.java`
- Create: `backend/src/main/java/com/moodtune/service/LlmClient.java`
- Create: `backend/src/main/java/com/moodtune/dto/LlmMessage.java`
- Create: `backend/src/main/java/com/moodtune/dto/LlmResponse.java`

- [ ] **Step 1: Create LLM config**

```java
package com.moodtune.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmConfig {
    private String apiUrl;
    private String apiKey;
    private String model;
}
```

- [ ] **Step 2: Create LLM DTOs**

```java
package com.moodtune.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LlmMessage {
    private String role; // "system", "user", "assistant"
    private String content;
}
```

```java
package com.moodtune.dto;

import lombok.Data;
import java.util.List;

@Data
public class LlmResponse {
    private List<Choice> choices;

    @Data
    public static class Choice {
        private LlmMessage message;
        private Delta delta;
    }

    @Data
    public static class Delta {
        private String content;
    }
}
```

- [ ] **Step 3: Create LlmClient with streaming support**

```java
package com.moodtune.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodtune.config.LlmConfig;
import com.moodtune.dto.LlmMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClient {

    private final LlmConfig llmConfig;
    private final ObjectMapper objectMapper;

    /**
     * Call LLM with streaming, sending chunks to SseEmitter.
     * Returns the full accumulated response text.
     */
    public String chatStream(List<LlmMessage> messages, SseEmitter emitter) throws IOException {
        WebClient client = WebClient.builder()
                .baseUrl(llmConfig.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + llmConfig.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = Map.of(
                "model", llmConfig.getModel(),
                "messages", messages,
                "stream", true
        );

        StringBuilder fullResponse = new StringBuilder();

        String responseBody = client.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Parse SSE response lines
        if (responseBody != null) {
            for (String line : responseBody.split("\n")) {
                if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                    try {
                        String json = line.substring(6);
                        JsonNode node = objectMapper.readTree(json);
                        String delta = node.path("choices").path(0).path("delta").path("content").asText("");
                        if (!delta.isEmpty()) {
                            fullResponse.append(delta);
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(delta));
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse SSE chunk: {}", line, e);
                    }
                }
            }
        }

        return fullResponse.toString();
    }

    /**
     * Non-streaming call for simpler use cases.
     */
    public String chat(List<LlmMessage> messages) {
        WebClient client = WebClient.builder()
                .baseUrl(llmConfig.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + llmConfig.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = Map.of(
                "model", llmConfig.getModel(),
                "messages", messages,
                "stream", false
        );

        String responseBody = client.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode node = objectMapper.readTree(responseBody);
            return node.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.error("Failed to parse LLM response", e);
            return "";
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/
git commit -m "feat: add LLM client with streaming support"
```

### Task 9: Recommendation Engine

**Files:**
- Create: `backend/src/main/java/com/moodtune/service/RecommendationService.java`
- Create: `backend/src/main/java/com/moodtune/dto/RecommendationResult.java`

- [ ] **Step 1: Create RecommendationResult DTO**

```java
package com.moodtune.dto;

import lombok.Data;

@Data
public class RecommendationResult {
    private String text;
    private Long songId;
}
```

- [ ] **Step 2: Create RecommendationService**

```java
package com.moodtune.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodtune.dto.*;
import com.moodtune.entity.ChatMessage;
import com.moodtune.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final LlmClient llmClient;
    private final SongService songService;
    private final ChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            你是一个私人音乐顾问，是用户的知心朋友。
            你的任务是根据用户当下的状态和心情，从他们的歌单中推荐一首最合适的歌。

            你的歌单如下：
            %s

            当前时间：%s

            规则：
            1. 根据对话内容理解用户的心情和需求
            2. 从歌单中选择最匹配的歌曲
            3. 推荐理由要自然，像朋友推荐一样
            4. 如果用户说"换一首"，推荐不同的歌曲
            5. 你的回复必须是JSON格式：{"text": "你的回复文字", "song_id": 歌曲ID}
            6. song_id 必须是歌单中某首歌的ID，如果是纯对话不需要推荐歌曲则为 null
            7. text 中包含推荐理由，自然地融入心情和场景描述
            """;

    /**
     * Build recommendation and stream response.
     */
    public RecommendationResult recommend(Long sessionId, String userMessage, SseEmitter emitter) throws IOException {
        // 1. Save user message
        ChatMessage userChatMsg = new ChatMessage();
        userChatMsg.setSessionId(sessionId);
        userChatMsg.setSenderType("user");
        userChatMsg.setTextContent(userMessage);
        messageRepository.save(userChatMsg);

        // 2. Build song list for system prompt
        String songList = buildSongList();
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm EEEE"));
        String systemPrompt = String.format(SYSTEM_PROMPT, songList, currentTime);

        // 3. Build message history
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", systemPrompt));

        List<ChatMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        for (ChatMessage msg : history) {
            String role = "user".equals(msg.getSenderType()) ? "user" : "assistant";
            messages.add(new LlmMessage(role, msg.getTextContent()));
        }

        // 4. Call LLM with streaming
        String fullResponse = llmClient.chatStream(messages, emitter);

        // 5. Parse structured response
        RecommendationResult result = parseResponse(fullResponse);

        // 6. Save AI message
        ChatMessage aiChatMsg = new ChatMessage();
        aiChatMsg.setSessionId(sessionId);
        aiChatMsg.setSenderType("ai");
        aiChatMsg.setTextContent(result.getText());
        aiChatMsg.setSongId(result.getSongId());
        messageRepository.save(aiChatMsg);

        return result;
    }

    private String buildSongList() {
        return songService.getAllSongs().stream()
                .map(s -> String.format("ID:%d | %s - %s | 风格:%s | 喜欢:%s",
                        s.getId(), s.getTitle(), s.getArtist(),
                        s.getGenre() != null ? s.getGenre() : "未知",
                        s.getLiked() ? "是" : "否"))
                .collect(Collectors.joining("\n"));
    }

    private RecommendationResult parseResponse(String response) {
        RecommendationResult result = new RecommendationResult();
        try {
            // Try to extract JSON from response
            String jsonStr = response.trim();
            // Handle case where LLM wraps JSON in markdown code block
            if (jsonStr.contains("```json")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
                jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("```"));
            } else if (jsonStr.contains("```")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
                jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("```"));
            }
            jsonStr = jsonStr.trim();

            JsonNode node = objectMapper.readTree(jsonStr);
            result.setText(node.path("text").asText(response));
            result.setSongId(node.has("song_id") && !node.get("song_id").isNull()
                    ? node.get("song_id").asLong() : null);
        } catch (Exception e) {
            log.warn("Failed to parse LLM response as JSON, using raw text: {}", e.getMessage());
            result.setText(response);
            result.setSongId(null);
        }
        return result;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/
git commit -m "feat: add RecommendationService with LLM prompt and response parsing"
```

---

## Phase 5: Controller Layer

### Task 10: SongController

**Files:**
- Create: `backend/src/main/java/com/moodtune/controller/SongController.java`
- Create: `backend/src/main/java/com/moodtune/controller/SongGroupController.java`

- [ ] **Step 1: Create SongController**

```java
package com.moodtune.controller;

import com.moodtune.dto.SongDTO;
import com.moodtune.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    @GetMapping
    public ResponseEntity<List<SongDTO>> getAllSongs() {
        return ResponseEntity.ok(songService.getAllSongs());
    }

    @GetMapping("/liked")
    public ResponseEntity<List<SongDTO>> getLikedSongs() {
        return ResponseEntity.ok(songService.getLikedSongs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SongDTO> getSong(@PathVariable Long id) {
        return ResponseEntity.ok(songService.getSongById(id));
    }

    @PostMapping
    public ResponseEntity<SongDTO> addSong(@RequestBody SongDTO dto) {
        return ResponseEntity.ok(songService.addSong(dto));
    }

    @PatchMapping("/{id}/like")
    public ResponseEntity<SongDTO> toggleLike(@PathVariable Long id) {
        return ResponseEntity.ok(songService.toggleLiked(id));
    }
}
```

- [ ] **Step 2: Create SongGroupController**

```java
package com.moodtune.controller;

import com.moodtune.dto.SongGroupDTO;
import com.moodtune.service.SongGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/song-groups")
@RequiredArgsConstructor
public class SongGroupController {

    private final SongGroupService groupService;

    @GetMapping
    public ResponseEntity<List<SongGroupDTO>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @PostMapping
    public ResponseEntity<SongGroupDTO> createGroup(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String source = body.getOrDefault("source", "manual");
        return ResponseEntity.ok(groupService.createGroup(name, source));
    }

    @PostMapping("/{groupId}/songs/{songId}")
    public ResponseEntity<Void> addSongToGroup(@PathVariable Long groupId, @PathVariable Long songId) {
        groupService.addSongToGroup(songId, groupId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/songs/{songId}")
    public ResponseEntity<Void> removeSongFromGroup(@PathVariable Long groupId, @PathVariable Long songId) {
        groupService.removeSongFromGroup(songId, groupId);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/
git commit -m "feat: add SongController and SongGroupController"
```

### Task 11: ChatController with SSE

**Files:**
- Create: `backend/src/main/java/com/moodtune/controller/ChatController.java`
- Create: `backend/src/main/java/com/moodtune/dto/ChatSessionDTO.java`
- Create: `backend/src/main/java/com/moodtune/dto/ChatMessageDTO.java`
- Create: `backend/src/main/java/com/moodtune/service/ChatService.java`

- [ ] **Step 1: Create DTOs**

```java
package com.moodtune.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatSessionDTO {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
package com.moodtune.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {
    private Long id;
    private Long sessionId;
    private String senderType;
    private String textContent;
    private SongDTO song; // populated when songId is not null
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: Create ChatService**

```java
package com.moodtune.service;

import com.moodtune.dto.*;
import com.moodtune.entity.ChatSession;
import com.moodtune.entity.ChatMessage;
import com.moodtune.repository.ChatSessionRepository;
import com.moodtune.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final SongService songService;

    public ChatSessionDTO createSession(String title) {
        ChatSession session = new ChatSession();
        session.setTitle(title);
        ChatSession saved = sessionRepository.save(session);
        return toSessionDTO(saved);
    }

    public List<ChatSessionDTO> getAllSessions() {
        return sessionRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toSessionDTO)
                .collect(Collectors.toList());
    }

    public List<ChatMessageDTO> getSessionMessages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    private ChatSessionDTO toSessionDTO(ChatSession session) {
        ChatSessionDTO dto = new ChatSessionDTO();
        dto.setId(session.getId());
        dto.setTitle(session.getTitle());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        return dto;
    }

    private ChatMessageDTO toMessageDTO(ChatMessage msg) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(msg.getId());
        dto.setSessionId(msg.getSessionId());
        dto.setSenderType(msg.getSenderType());
        dto.setTextContent(msg.getTextContent());
        dto.setCreatedAt(msg.getCreatedAt());
        if (msg.getSongId() != null) {
            try {
                dto.setSong(songService.getSongById(msg.getSongId()));
            } catch (Exception e) {
                // Song may have been deleted
            }
        }
        return dto;
    }
}
```

- [ ] **Step 3: Create ChatController**

```java
package com.moodtune.controller;

import com.moodtune.dto.*;
import com.moodtune.service.ChatService;
import com.moodtune.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final RecommendationService recommendationService;

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionDTO> createSession(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "新会话");
        return ResponseEntity.ok(chatService.createSession(title));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionDTO>> getSessions() {
        return ResponseEntity.ok(chatService.getAllSessions());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(@PathVariable Long sessionId) {
        return ResponseEntity.ok(chatService.getSessionMessages(sessionId));
    }

    @PostMapping(value = "/sessions/{sessionId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@PathVariable Long sessionId, @RequestBody Map<String, String> body) {
        String message = body.get("message");
        SseEmitter emitter = new SseEmitter(60000L); // 60s timeout

        // Run in async thread to not block
        new Thread(() -> {
            try {
                RecommendationResult result = recommendationService.recommend(sessionId, message, emitter);

                // Send final event with structured data
                emitter.send(SseEmitter.event()
                        .name("recommendation")
                        .data(result));

                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/
git commit -m "feat: add ChatController with SSE streaming and ChatService"
```

### Task 12: CORS Configuration

**Files:**
- Create: `backend/src/main/java/com/moodtune/config/CorsConfig.java`

- [ ] **Step 1: Create CORS config**

```java
package com.moodtune.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/
git commit -m "feat: add CORS configuration for frontend access"
```

---

## Phase 6: MinIO Integration

### Task 13: MinIO Service

**Files:**
- Create: `backend/src/main/java/com/moodtune/config/MinioConfig.java`
- Create: `backend/src/main/java/com/moodtune/service/MinioService.java`

- [ ] **Step 1: Create MinIO config**

```java
package com.moodtune.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

- [ ] **Step 2: Create MinIO service**

```java
package com.moodtune.service;

import com.moodtune.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * Ensure bucket exists.
     */
    public void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioConfig.getBucketName()).build());
                log.info("Created bucket: {}", minioConfig.getBucketName());
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket exists", e);
        }
    }

    /**
     * Upload file and return presigned URL.
     */
    public String uploadFile(MultipartFile file, String originalFilename) {
        try {
            ensureBucket();
            String objectName = UUID.randomUUID() + "_" + originalFilename;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            return getPresignedUrl(objectName);
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    /**
     * Get presigned URL for an object (valid for 7 days).
     */
    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to get presigned URL", e);
            throw new RuntimeException("Failed to get presigned URL", e);
        }
    }
}
```

- [ ] **Step 3: Add file upload endpoint to SongController**

Add to `SongController.java`:

```java
@PostMapping("/upload")
public ResponseEntity<SongDTO> uploadSong(
        @RequestParam("file") MultipartFile file,
        @RequestParam("title") String title,
        @RequestParam("artist") String artist,
        @RequestParam(value = "genre", required = false) String genre) {
    String fileUrl = minioService.uploadFile(file, file.getOriginalFilename());
    SongDTO dto = new SongDTO();
    dto.setTitle(title);
    dto.setArtist(artist);
    dto.setGenre(genre);
    dto.setFileUrl(fileUrl);
    return ResponseEntity.ok(songService.addSong(dto));
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/
git commit -m "feat: add MinIO integration for song file storage"
```

---

## Phase 7: Project Root Setup

### Task 14: Git Init and Root README

**Files:**
- Create: `.gitignore` (root)
- Create: `README.md` (root)

- [ ] **Step 1: Create root .gitignore**

```
# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store
Thumbs.db

# Backend
backend/target/
backend/*.class
backend/*.jar

# Frontend (will be added later)
frontend/node_modules/
frontend/dist/

# Env
.env
*.log
```

- [ ] **Step 2: Initialize git**

```bash
cd C:/javapractice/Sass
git init
git add .
git commit -m "init: MoodTune project with Spring Boot backend"
```

---

## Summary

| Phase | Tasks | Description |
|-------|-------|-------------|
| 1 | Task 1 | Project scaffolding, pom.xml, application.yml |
| 2 | Tasks 2-5 | All entities and repositories |
| 3 | Tasks 6-7 | Song and SongGroup services |
| 4 | Tasks 8-9 | LLM client and Recommendation engine |
| 5 | Tasks 10-12 | Controllers and CORS |
| 6 | Task 13 | MinIO integration |
| 7 | Task 14 | Git init and project root |

After completing this plan, the backend will have:
- Full CRUD for songs and groups
- Chat sessions with message history
- AI-powered recommendation via SSE streaming
- File upload and storage via MinIO
- Ready for Vue frontend integration
