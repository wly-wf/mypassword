# CLAUDE.md

## 项目概述

MyPassword — 极简 Android 离线密码管理器。纯本地存储、Material You UI、指纹+密码双解锁。

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- MVVM + Repository 架构
- Room + SQLCipher（AES-256 加密数据库）
- Argon2 密钥派生（BouncyCastle）
- AES-256-GCM 加密导出
- BiometricPrompt 生物识别
- Navigation Compose 单 Activity 架构
- minSdk 26 / targetSdk 35

## 关键实现约束

- **永不联网**：不添加 INTERNET 权限，不做任何网络请求
- **纯本地**：所有数据只存设备，导出的 `.mpbak` 文件也是加密二进制
- **密码永不明文落盘**：数据库通过 SQLCipher 加密，密钥由主密码经 Argon2 派生
- **重启后首次必须输密码**：Android Keystore 在重启后指纹不可用，这是系统安全约束
- **切后台即锁定**：onStop 中清除内存密钥，回前台必须重新验证

## 项目文件

- `desc.md` — 产品设计文档（数据模型、功能清单、加密方案、UI方向、技术选型）
- `todo.md` — 开发任务清单（8阶段30任务）

## 开发约定

- 每个 todo 阶段的一个子任务对应一个 git commit
- 包名：`com.mypassword.app`
- UI 文本使用中文字符串资源（`strings.xml`）
- 最低 Android 8.0 (API 26)
