# 0001: Android 原生 (Kotlin + Compose) over Flutter

项目最初按"未来可能发布、iOS+安卓双端覆盖"选择 Flutter 跨平台方案。但事实在 grilling 中途变了——确认为自用、不发布、只发安卓一端。Flutter 跨两端的核心卖点归零，而它的代价（SDK 工具链、构建速度、额外学习曲线）对单人单端项目成了纯负担。Android 原生 + Jetpack Compose + CameraX/Room/Coil 等官方一等公民库，文档最全、路径最直。

考虑过的选项：Flutter（原选）、微信小程序（本地存储 10MB 与图鉴存图冲突）、PWA（后台回收 + 拍照体验略逊）。权衡后选择最贴合"单端自用"的路线。
