# MinerU PDF Parser

Android 端 MinerU 文档解析工具，支持将 PDF 上传到 MinerU API 进行智能解析，输出结构化 Markdown 文档。

## 功能

- 选择本地 PDF 文件
- 自动检查文件大小/页数（超过 180MB 或 180 页自动分割）
- 通过 MinerU API 进行文档解析
- 下载解析结果（Markdown + 图片）
- 图片路径自动修正为相对路径
- 输出到 Download/PDFParser/ 目录

## 技术栈

- Kotlin
- MinerU API（预签名上传模式）
- PdfiumAndroid（PDF 处理）
- OkHttp（网络请求）
- WorkManager（后台任务）
- EncryptedSharedPreferences（Token 加密存储）
- zip4j（ZIP 解压）

## 构建

### Android Studio

1. 克隆仓库
2. 用 Android Studio 打开项目
3. 等待 Gradle 同步
4. 连接设备或启动模拟器，点击 Run

### 命令行

```bash
chmod +x gradlew
./gradlew assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## CI/CD

项目配置了 GitHub Actions，推送到 main 分支会自动构建：

- Debug APK
- Release APK（未签名）

构建产物在 Actions → 对应 workflow → Artifacts 中下载。

## 使用流程

1. 安装并打开应用
2. 点击右下角 ⚙ 设置，输入 MinerU Token
3. 点击"测试连接"验证 Token
4. 返回主页，点击"选择 PDF 文件"
5. 选择 PDF 后自动检查文件信息
6. 点击"开始解析"
7. 等待处理完成，结果输出到 Download/PDFParser/{文件名}/

## 输出结构

```
/Download/PDFParser/example/
├── full.md              ← Markdown 文档
└── images/              ← 图片目录
    ├── image1.png
    └── image2.jpg
```

## 项目结构

```
app/src/main/java/com/example/pdfparser/
├── PdfParserApp.kt
├── data/
│   ├── model/MinerUModels.kt
│   ├── local/TokenStorage.kt
│   └── repository/MineruRepository.kt
├── pdf/PdfRepository.kt
├── processor/ResultMerger.kt
├── ui/
│   ├── MainActivity.kt
│   ├── SettingsActivity.kt
│   ├── ProcessingActivity.kt
│   └── ProgressViewModel.kt
├── util/
│   ├── FileUtils.kt
│   └── MarkdownUtils.kt
└── worker/ParseWorker.kt
```

## License

MIT
