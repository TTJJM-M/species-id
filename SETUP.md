# 物种识别 (Species ID) — 项目指南

项目根目录：`/Users/ttjjm/Documents/app`

## 项目简介

个人移动端万物识别工具。拍照后自动识别图片中的主体内容，给出名称、类别、简介和置信度。识别成功的记录自动收入图鉴，支持按类别筛选、按名称搜索、查看原图大图和删除。

| 组件 | 技术栈 | 路径 |
|------|--------|------|
| Android App | Kotlin + Jetpack Compose | `/Users/ttjjm/Documents/app/app/` |
| 后端 API | Python FastAPI + 百炼大模型 | `/Users/ttjjm/Documents/app/backend/` |
| 模型 | qwen3-vl-235b (阿里百炼) | — |

---

## 从零搭建

### 前置条件

- macOS（后端在本机跑）
- Android 手机（minSdk 33，Android 13+）
- Java 17+、Android SDK 35
- Python 3.10+

### 1. 克隆项目

```bash
git clone <repo-url> /Users/ttjjm/Documents/app
cd /Users/ttjjm/Documents/app
```

### 2. 搭建后端

```bash
cd /Users/ttjjm/Documents/app/backend

# 创建虚拟环境
python3 -m venv venv
source venv/bin/activate

# 安装依赖
pip install fastapi uvicorn httpx python-dotenv

# 配置百炼 API Key
echo 'BAILIAN_API_KEY=你的百炼key' > .env
```

验证后端：

```bash
cd /Users/ttjjm/Documents/app/backend && source venv/bin/activate && pytest
```

### 3. 搭建 Android App

```bash
cd /Users/ttjjm/Documents/app/app

# 确认 local.properties 里的 Android SDK 路径正确
# sdk.dir=/Users/你的用户名/Android/Sdk

# 跑测试验证环境
./gradlew :app:testDebugUnitTest   # 18 个测试应该全绿
```

### 4. 连接手机

- 手机开启「开发者选项」→「USB 调试」
- USB 连接 Mac，手机上点「允许」
- 验证：`adb devices` 能看到你的设备

---

## 日常使用

### 启动

**后端**（先起）：

```bash
cd /Users/ttjjm/Documents/app/backend && source venv/bin/activate && uvicorn main:app --host 0.0.0.0 --port 8000
```

看到 `Uvicorn running on http://0.0.0.0:8000` 就绪。

**App**（装到手机）：

```bash
cd /Users/ttjjm/Documents/app/app && ./gradlew :app:installDebug
```

手机桌面点击「物种识别」图标打开。

> 首次启动 App 会自动使用默认地址 `http://192.168.1.3:8000/`。如果你的 Mac IP 不同，点拍照页右上角齿轮修改。

### 关闭

```bash
bash /Users/ttjjm/Documents/app/stop.sh
```

或者手动：后端终端 `Ctrl+C`，App 手机划掉。

---

## 不在同一网络时使用（出门 / 蜂窝网络）

上面启动的是局域网模式，手机和 Mac 必须在同一个 Wi-Fi 下。如果出门在外想用，需要把本地后端暴露到公网。

### 方案：cloudflared 隧道（已安装，免费）

在新终端窗口运行：

```bash
cloudflared tunnel --url http://localhost:8000 --protocol http2
```

启动后会输出一个公网地址：

```
https://xxx-xxx-trycloudflare.com
```

**把这个地址填进 App 设置**（拍照页右上角齿轮 → 粘贴 → 保存），之后手机不管在哪都能连。

> 注意：每次重启 cloudflared 地址会变，需要重新复制粘贴一次。用完 `Ctrl+C` 关掉。

### 一键启动脚本

把下面内容保存为 `/Users/ttjjm/Documents/app/start.sh`：

```bash
#!/usr/bin/env bash
cd /Users/ttjjm/Documents/app/backend && source venv/bin/activate
uvicorn main:app --host 127.0.0.1 --port 8000 &
sleep 1
echo "公网地址（填入 App 设置）："
cloudflared tunnel --url http://localhost:8000 --protocol http2 2>&1 | grep -oE 'https://[a-zA-Z0-9.-]+\.trycloudflare\.com'
```

以后一条命令搞定：

```bash
bash /Users/ttjjm/Documents/app/start.sh
```

---

## 运行测试

```bash
# 后端
cd /Users/ttjjm/Documents/app/backend && source venv/bin/activate && pytest

# Android App
cd /Users/ttjjm/Documents/app/app && ./gradlew :app:testDebugUnitTest
```

---

## 项目结构

```
/Users/ttjjm/Documents/app/
├── CONTEXT.md              # 领域术语表
├── docs/adr/               # 架构决策记录
├── backend/
│   ├── main.py             # FastAPI 服务（POST /recognize）
│   ├── test_main.py        # 后端测试
│   ├── .env                # 百炼 API Key（不入 git）
│   └── venv/               # Python 虚拟环境
└── app/
    ├── build.gradle.kts
    ├── gradlew
    └── app/
        ├── build.gradle.kts
        └── src/
            ├── main/java/com/ttjjm/speciesid/
            │   ├── SpeciesIdApp.kt         # Application
            │   ├── MainActivity.kt
            │   ├── net/
            │   │   ├── ApiService.kt       # Retrofit 接口
            │   │   └── RetrofitClient.kt   # 网络客户端（默认地址）
            │   ├── data/
            │   │   ├── RecognitionResponse.kt
            │   │   ├── AppDatabase.kt
            │   │   └── guide/              # 图鉴：Room DAO + Repository
            │   └── ui/
            │       ├── camera/             # 拍照识别页
            │       ├── guide/              # 图鉴列表 + 详情
            │       ├── settings/           # 后端地址设置
            │       └── theme/              # 杂志风设计语言
            └── test/                       # 单元测试
```
