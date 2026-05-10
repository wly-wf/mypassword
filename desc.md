# MyPassword - 产品设计文档

## 产品定位

一款极简的本地密码本 —— 只做两件事：**存密码、查密码**。不联网、不自动填充、不云同步。换机时通过加密文件导出导入。

---

## 核心功能

### 1. 数据模型

统一条目结构，通过「类型」字段区分：

```
Entry
├── type:     URL ｜ APP        （类型）
├── target:   string            （网址URL 或 App名称）
├── username: string            （账号）
├── password: string            （密码）
├── note:     string?           （备注，可选）
├── createdAt: datetime         （创建时间）
└── updatedAt: datetime         （修改时间）
```

展示上按两个 Tab 分类：**网址** / **App**，共用同一套增删改查逻辑。

### 2. 功能清单

| 功能 | 描述 | 优先级 |
|------|------|:---:|
| 密码解锁 | 进入App需输入主密码，主密码通过 Argon2 派生加密密钥 | P0 |
| 指纹解锁 | 通过 BiometricPrompt 调用系统指纹/人脸识别快速解锁 | P0 |
| 列表浏览 | 首页两个Tab（网址/App），支持搜索过滤 | P0 |
| 添加条目 | 表单页，根据类型字段不同（URL输入 vs App名输入） | P0 |
| 查看/复制 | 点击条目查看详情，长按复制账号/密码到剪贴板 | P0 |
| 编辑条目 | 修改已有条目的任意字段 | P1 |
| 删除条目 | 滑动删除或详情页删除，二次确认 | P1 |
| 密码生成器 | 可调长度(8-64)、字符类型(大小写/数字/符号)的随机密码生成 | P1 |
| 加密导出 | 将全部数据 AES-256 加密后导出为 `.mpbak` 文件存到本地 | P1 |
| 加密导入 | 选择 `.mpbak` 文件，输入主密码解密后合并/覆盖数据 | P1 |

### 3. 明确不做

- ❌ Android 自动填充服务
- ❌ 云同步 / 多设备同步
- ❌ 浏览器扩展
- ❌ TOTP / Passkey
- ❌ 密码泄露检测
- ❌ 文件附件
- ❌ 多人共享

---

## 加密方案

### 存储加密

```
主密码
  │
  ▼ Argon2id (salt, iterations, memory)
256位密钥 ──► AES-256-GCM ──► 加密 SQLite (SQLCipher)
```

- 主密码本身不存设备，只存 Argon2 派生的哈希用于验证
- 所有条目数据通过 SQLCipher 以 AES-256 加密落盘
- App 退出或切后台后立即锁定

### 解锁流程

```
┌──────────────────────────────┐
│         启动 App              │
└──────────┬───────────────────┘
           ▼
┌──────────────────────┐
│   是否已录入指纹？     │
└──────┬───────┬───────┘
      是      否
       │       │
       ▼       ▼
┌──────────┐ ┌──────────────┐
│ 指纹识别  │ │  输入主密码   │
│ (优先)   │ │              │
└────┬─────┘ └──────┬───────┘
     │              │
     ▼              ▼
┌──────────────────────────┐
│  密钥解密数据库，进入 App  │
└──────────────────────────┘
```

- **指纹优先**：设备支持且已录入指纹时，优先展示指纹识别，用户也可切换到密码输入
- **密码兜底**：指纹识别失败3次后自动切回密码输入；重启手机后首次必须输入密码（Android 安全要求）
- **密钥缓存**：指纹验证通过后，从 Android Keystore 中取出加密的数据库密钥，解密数据库

### 导出加密

```
导出密码 (用户单独设定或使用主密码)
  │
  ▼ PBKDF2 / Argon2id
AES-256-GCM 密钥
  │
  ▼
JSON 明文 ──► 加密二进制 ──► .mpbak 文件
```

导出文件 `.mpbak` 是加密二进制文件，即使文件被他人获取也无法读取内容。

### 导入流程

```
选择 .mpbak 文件 ──► 输入导出密码 ──► 解密验证 ──► 合并/覆盖到当前数据库
```

---

## UI/UX 设计方向

- **设计语言**：Material You (Material Design 3)，跟随系统壁纸动态取色
- **布局**：单 Activity 架构，Navigation Compose 管理页面栈
- **首页**：两个 Tab（网址 | App），每项显示 target + username，密码默认遮罩
- **详情**：点击卡片展开，密码默认 `••••••`，点击眼睛图标切换明文，一键复制按钮
- **搜索**：顶部搜索栏，实时过滤列表
- **空状态**：无数据时展示简洁插画 + 引导文字
- **动效**：列表项进出动画、复制成功 snackbar

---

## 技术选型

| 层面 | 选择 | 理由 |
|------|------|------|
| 语言 | Kotlin | Android 原生首选 |
| UI | Jetpack Compose + Material 3 | 声明式UI，Material You 支持最好 |
| 架构 | MVVM + Repository | Google 官方推荐架构 |
| 本地数据库 | Room + SQLCipher | Room 标准 ORM + AES-256 透明加密 |
| 密钥派生 | Argon2 (通过 BouncyCastle) | 抗 GPU 暴力破解 |
| 加密 | AES-256-GCM (javax.crypto) | 标准对称加密 |
| 生物识别 | BiometricPrompt (AndroidX) | 系统级指纹/人脸 |
| 最低 SDK | Android 8.0 (API 26) | 覆盖 95%+ 设备 |
| 目标 SDK | Android 15 (API 35) | 最新稳定版 |

---

## 项目结构（草案）

```
app/
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt          # Room + SQLCipher
│   │   ├── EntryDao.kt
│   │   └── entity/Entry.kt
│   ├── crypto/
│   │   ├── KeyDerivation.kt        # Argon2
│   │   ├── DatabaseEncryption.kt   # SQLCipher helper
│   │   └── BackupEncryption.kt     # 导出导入加解密
│   └── repository/
│       └── EntryRepository.kt
├── ui/
│   ├── unlock/UnlockScreen.kt
│   ├── list/ListScreen.kt
│   ├── detail/DetailScreen.kt
│   ├── edit/EditScreen.kt
│   ├── generator/PasswordGenerator.kt
│   ├── backup/BackupScreen.kt
│   └── theme/Theme.kt + Color.kt
├── viewmodel/
│   ├── UnlockViewModel.kt
│   ├── ListViewModel.kt
│   └── EditViewModel.kt
└── MainActivity.kt
```
