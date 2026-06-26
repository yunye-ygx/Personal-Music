# 项目开发规范

## 重要行为规则

### ⛔ 禁止行为

1. **永远不要自动启动后端服务**
   - 不要运行 `mvn spring-boot:run`
   - 不要运行 `java -jar` 启动后端
   - 用户会自己手动启动后端
   - 如果需要验证后端，只能使用 `curl` 测试接口

2. **不要自动重启服务**
   - 修改代码后不要主动重启
   - 等待用户明确指示

## 代码风格规范

### 包结构规范

**严格按照职责分类，保持包的纯净性：**

```
src/main/java/com/moodtune/
├── config/          # 配置类（Spring 配置、Bean 定义）
├── controller/      # 控制器（REST API）
├── service/         # 业务逻辑（纯业务代码）
├── mapper/          # 数据访问层（MyBatis）
├── entity/          # 实体类（对应数据库表）
├── dto/             # 数据传输对象（API 请求/响应）
├── tool/            # AI 工具类（Function Calling 工具）
└── util/            # 工具类
```

**禁止行为：**
- ❌ 不要把工具类放在 `service/` 包
- ❌ 不要把参数类嵌套在工具类内部
- ❌ 不要污染 `dto/` 和 `vo/` 包

**正确示例：**
```java
// ✅ 正确：工具类在专门的 tool 包
package com.moodtune.tool;

@Component
public class SongSearchTool implements Function<SongSearchRequest, List<Song>> {
    // ...
}

// ✅ 正确：参数类独立文件
package com.moodtune.tool;

@Data
@NoArgsConstructor
public class SongSearchRequest {
    private String title;
    private String artist;
}
```

### Java 编码规范

1. **使用 Lombok 注解**
   ```java
   @Data                    // getter/setter/toString/equals/hashCode
   @NoArgsConstructor       // 无参构造器
   @AllArgsConstructor      // 全参构造器
   @Builder                 // Builder 模式
   @RequiredArgsConstructor // final 字段构造器
   ```

2. **字段私有化，使用 getter/setter**
   ```java
   // ✅ 正确
   @Data
   public class Request {
       private String name;
   }
   
   // ❌ 错误
   public class Request {
       public String name;  // 不要用 public 字段
   }
   ```

3. **Lambda 中的变量必须是 final**
   ```java
   // ✅ 正确
   final String keyword = request.getKeyword();
   wrapper.and(w -> w.like("title", keyword));
   
   // ❌ 错误
   String keyword = request.getKeyword();
   keyword = keyword.trim();  // 重新赋值
   wrapper.and(w -> w.like("title", keyword));  // 编译错误
   ```

4. **避免嵌套类**
   - 参数类、配置类等应独立为文件
   - 保持类的单一职责

## Spring AI 工具调用规范

### Function Calling 实现方式

使用 Spring AI 1.0.0-M5+ 的新 API：

```java
@Configuration
public class AiConfig {
    
    @Bean
    @Primary
    public ChatClient.Builder customChatClientBuilder(
            ChatModel chatModel, 
            SongSearchTool songSearchTool) {
        
        return ChatClient.builder(chatModel)
            .defaultFunction("searchLikedSongs",
                           "工具描述",
                           songSearchTool);
    }
}
```

**工具类实现：**
```java
@Component
public class SongSearchTool implements Function<SongSearchRequest, List<Song>> {
    
    @Override
    public List<Song> apply(SongSearchRequest request) {
        // 实现逻辑
        return results;
    }
}
```

## 前端规范

### 接口调用
- 前端统一通过 `src/api/` 下的模块调用后端接口
- 不在组件中直接写 `fetch` 或 `axios`

### 状态管理
- 使用后端接口存储数据，不使用 `localStorage` 作为主存储
- `localStorage` 只用于临时缓存或用户偏好设置

## Git 提交规范

遵循 Conventional Commits：

```
feat: 新功能
fix: 修复 bug
refactor: 重构代码
docs: 文档更新
style: 代码格式调整
test: 测试相关
chore: 构建/工具配置
```

## 技术栈版本

- Java 17
- Spring Boot 3.3.0
- Spring AI 1.0.0-M5
- MyBatis Plus 3.5.x
- Vue 3 + Vite
- PostgreSQL 14+
