# BlogBackend
个人博客 SpringBoot 后端项目源码
技术栈：JDK 17 + SpringBoot 3.x + Redis + MybatisPlus + Sentinel

## 一、DockerHub
https://hub.docker.com/r/maiyihe/blog_backend

## 二、项目说明
- 文章详情读取：返回前端文件树中包含了文章详情 id，前端根据 id 访问后端拿到文件路径，后端再根据文件路径本地 IO 查询到 md 笔记返回前端
    - Redis 对文件树与文章详情进行缓存
    - 访客只有 **读取文章的权限** ，没有写的权限，不会有 ACID 事务问题；因此程序没有选择 Mysql 管理文章详情，而是采用：Mysql 建立文章元数据索引 + 文件系统存储正文。对博客内容修改 **直接进入服务器后台** ，同步博客仓库
        - 将文件置于博客仓库后，需要进行 contentScan 来创建 Mysql 中存储的文章元数据
        - `.git` 这类 dotfiles 默认不会被读取。也可以自定义配置过滤读取的文件
- 对于图片，采用阿里云 oss 预签名，设置了签发时间
    - 默认每篇笔记存储在对应 Topic 的 `-1_figures` 目录下。figuresScan 会扫描该目录并在后台上传至阿里云 OSS（需要预先设置阿里云 OSS 的访问账户和密码）
- Sentinel 实现限流降级、熔断，防止恶意流量
- DB 与 Redis 缓存一致性问题：解决方式较为暴力，即在 contentScan 更新数据库后，直接清空缓存
### 2.1 主体部分运行原理，与使用本项目记录博客时的日常工作流
<img width="1424" height="1117" alt="image" src="https://github.com/user-attachments/assets/8c701605-afb5-4bbb-b6d7-b8a6c00c3924" />

- '-1_figures' 是 Obsidian 里定义的，图片存储的路径，在 'contentScan.yml' 中过滤了 `api/admin/content/scan` 的扫描，不会被记录到数据表当中

### 2.2 contentScan 的原理
<img width="1371" height="845" alt="image" src="https://github.com/user-attachments/assets/670a8ddd-58b5-4b1d-8964-7913f02657a1" />

- 比较本地目录下的文件和数据库当中的文件，然后执行增量更新

### 2.3 关键数据表Topic和Note
<img width="1688" height="1087" alt="image" src="https://github.com/user-attachments/assets/f36110d2-6433-43ae-a915-907d1ff94277" />

## 三、docker-compose.yml 与默认配置文件
https://github.com/maiyihe/BlogConfig
