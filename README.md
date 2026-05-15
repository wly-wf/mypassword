# MyPassword

极简离线 Android 密码管理器。纯本地存储，永不联网，Material You 设计，指纹+密码双解锁。

## 功能

- **密码管理**：网站/App 账号密码存储，支持标题、账号、密码、网址、备注
- **地址管理**：外卖/快递/家/学校/公司/其他地址存储，类型标签分类
- **离线安全**：所有数据仅存设备本地，无任何网络权限，杜绝云端泄露
- **双重加密**：AES-256-GCM 加密导出 + SQLCipher 加密数据库 + Argon2id 密钥派生
- **指纹解锁**：支持系统 BiometricPrompt 指纹/面部识别快速解锁
- **密码自动锁**：熄屏即锁定，亮屏后要求重新验证身份
- **忘记密码**：指纹验证后可重置主密码，数据不丢失
- **密码生成器**：内置随机强密码生成，支持自定义长度和字符集
- **加密备份**：支持密码和地址数据一键导出 `.mpbak` 加密备份，去重导入
- **底部导航**：密码、地址、设置三 Tab 切换，中文拼音排序
- **Material You**：适配 Android 12+ 动态取色，暗色模式自动跟随系统

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository |
| 数据库 | Room + SQLCipher (AES-256) |
| 密钥派生 | Argon2id (BouncyCastle) |
| 生物识别 | BiometricPrompt + EncryptedSharedPreferences |
| 导航 | Navigation Compose (单 Activity) |
| 最低版本 | Android 8.0 (API 26) |

## 构建

1. 用 Android Studio 打开项目根目录
2. 等待 Gradle 同步完成
3. `Build` → `Build APK(s)` 生成 Debug APK
4. 或 `Build` → `Generate Signed Bundle / APK` 生成 Release APK

## 项目结构

```
app/src/main/java/com/mypassword/app/
├── MainActivity.kt              # 单 Activity 入口
├── MyPasswordApplication.kt     # Application，生命周期管理
├── AppNavGraph.kt               # 导航图
├── ui/
│   ├── theme/                   # Material You 主题
│   ├── navigation/              # 路由定义
│   ├── home/                    # 首页（底部导航栏）
│   ├── unlock/                  # 解锁/设置主密码
│   ├── list/                    # 密码列表
│   ├── detail/                  # 密码详情
│   ├── edit/                    # 添加/编辑密码
│   ├── address/                 # 地址列表、详情、编辑
│   ├── settings/                # 设置（指纹开关、改密码、备份）
│   ├── backup/                  # 加密备份与恢复
│   └── generator/               # 密码生成器
├── viewmodel/                   # ViewModel 层
└── data/
    ├── db/                      # Room 数据库 + DAO
    ├── crypto/                  # 加密（Argon2、AES、密钥管理）
    └── repository/              # 数据仓库
```

## 安全设计

- 主密码通过 Argon2id 派生出 256 位密钥，盐值随机生成
- 数据库使用 SQLCipher 以 AES-256 加密，密钥从主密码派生
- 指纹解锁使用 Android Keystore 硬件安全模块存储密钥
- 导出备份为 `.mpbak` 格式：AES-256-GCM 加密的 JSON 数据
- 切后台或熄屏立即清除内存中的密钥缓存
- 主密码永不明文落盘

## License

MIT
