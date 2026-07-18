# Species ID · 物种识别

万物识别——拍照或选图，自动识别主体内容。基于阿里百炼大模型（qwen3-vl），后端 Python FastAPI，前端 Android Kotlin/Compose。

**关键词：** FastAPI · Jetpack Compose · 百炼 · 万物识别 · Room

## 截图

| 识别结果 | 图鉴列表 | 图鉴详情 |
|----------|----------|----------|
| 拍照识别后自动展示 | 杂志风档案集布局 | 原图缩放 + 完整信息 |

## 项目结构

```
species-id/
├── backend/               # Python FastAPI 后端
│   ├── main.py            # POST /recognize — 收图片返回识别结果
│   └── test_main.py       # 后端测试
├── app/                   # Android 原生 App
│   ├── app/src/main/java/com/ttjjm/speciesid/
│   │   ├── net/           # Retrofit 网络层
│   │   ├── data/          # Room 数据库 + Repository
│   │   └── ui/            # 拍照、图鉴、设置、主题
│   └── app/src/test/      # 单元测试（Robolectric）
├── SETUP.md               # 从零搭建指南
└── CONTEXT.md             # 领域术语表
```

## 主要功能

- **拍照 / 选图识别** — 拍下或从相册选择图片，自动识别并展示结果
- **万物识别** — 不限类别：植物、动物、菜品、建筑、车辆、日用品……任何图片主体
- **杂志风 UI** — 白底大黑标题、渐变色快门、置信度圆环、色点筛选卡
- **图鉴** — 识别成功自动入册，支持按类别筛选、按名称搜索、原图缩放、删除
- **数据本地化** — 所有记录存在手机 Room 数据库，后端无状态

## 启动

```bash
# 后端
cd backend && source venv/bin/activate && uvicorn main:app --host 0.0.0.0 --port 8000

# App 装到手机
cd app && ./gradlew :app:installDebug
```

详细步骤见 [SETUP.md](SETUP.md)。

## 技术栈

| 层 | 技术 |
|----|------|
| 模型 | 阿里百炼 qwen3-vl-235b |
| 后端 | FastAPI + httpx (Python) |
| Android | Jetpack Compose + Retrofit + Room + Moshi |
| 测试 | JUnit + Turbine + Robolectric + pytest |

## 许可证

MIT
