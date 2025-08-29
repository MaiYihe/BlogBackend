# BlogBackend
个人博客 SpringBoot 后端项目源码
- 建议结合 Obsidian 进行使用

## 一、DockerHub
https://hub.docker.com/r/maiyihe/blog_backend

## 二、项目说明
### 2.1 主体部分运行原理，与使用本项目记录博客时的日常工作流
<img width="1424" height="1119" alt="image" src="https://github.com/user-attachments/assets/092d0d48-967c-484a-ba83-58602f45aa9c" />

- '-1_figures' 是 Obsidian 里定义的，图片存储的路径，不参与 `api/admin/content/scan` 的扫描，不会被记录到数据表当中

### 2.2 contentScan 的原理
<img width="1371" height="845" alt="image" src="https://github.com/user-attachments/assets/670a8ddd-58b5-4b1d-8964-7913f02657a1" />

- 比较本地目录下的文件和数据库当中的文件，然后执行增量更新

### 2.3 关键数据表Topic和Note
<img width="1688" height="1087" alt="image" src="https://github.com/user-attachments/assets/f36110d2-6433-43ae-a915-907d1ff94277" />

## 三、docker-compose.yml 与默认配置文件
https://github.com/maiyihe/BlogConfig
