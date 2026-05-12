# MyPassword 开发任务清单

> 总计 8 个阶段，约 30 个小任务

---

## 阶段一：项目脚手架

- [x] **1.1** 创建 Android 项目（Kotlin + Jetpack Compose，包名 `com.mypassword.app`，minSdk 26，targetSdk 35）
- [x] **1.2** 配置 `build.gradle`（添加 Room、SQLCipher、BouncyCastle、Biometric、Navigation Compose、Material 3 依赖）
- [x] **1.3** 配置混淆规则（SQLCipher 和 BouncyCastle 的 proguard 规则）
- [x] **1.4** 搭建 Material You 主题（跟随系统壁纸动态取色，亮/暗色模式支持）
- [x] **1.5** 搭建单 Activity 导航骨架（MainActivity + NavHost，定义各页面路由）

---

## 阶段二：数据层

- [x] **2.1** 定义 `Entry` 实体（Room Entity，字段：id, type, target, username, password, note, createdAt, updatedAt）
- [x] **2.2** 编写 `EntryDao`（增删改查 + 按类型筛选 + 关键词搜索）
- [x] **2.3** 实现 `AppDatabase`（Room + SQLCipher 加密支持，通过 SupportFactory 传入加密密钥）
- [x] **2.4** 实现 `KeyDerivation` 工具类（Argon2id 主密码 → 派生 256 位密钥 + 验证哈希）
- [x] **2.5** 实现 `SessionManager`（管理数据库密钥缓存：内存缓存 + Android Keystore 持久化，供指纹解锁使用）
- [x] **2.6** 实现 `EntryRepository`（封装 DAO 操作，提供 Flow<List<Entry>> 响应式数据流）

---

## 阶段三：解锁模块

- [x] **3.1** 实现 `UnlockViewModel`（密码输入验证、错误计数、锁定倒计时）
- [x] **3.2** 编写 `UnlockScreen` UI（主密码输入框 + 数字键盘 / 全键盘切换 + 错误提示动画）
- [x] **3.3** 集成 `BiometricPrompt`（指纹/人脸识别，成功 → 从 Keystore 取密钥解密数据库）
- [x] **3.4** 处理边界情况（重启后首次必须输密码、指纹失败3次切回密码、切后台立即锁定）

---

## 阶段四：列表模块（首页）

- [x] **4.1** 实现 `ListViewModel`（管理两个 Tab 的条目列表 + 搜索过滤状态）
- [x] **4.2** 编写 `ListScreen` UI（顶部搜索栏 + Tab 切换「网址」「App」+ LazyColumn 条目卡片）
- [x] **4.3** 实现条目卡片组件（显示 target + username，密码默认••••遮罩，右侧复制按钮）
- [x] **4.4** 实现滑动删除（SwipeToDismiss + 删除确认对话框）
- [x] **4.5** 实现空状态页面（无条目时的插画 + 引导添加文字）

---

## 阶段五：添加/编辑模块

- [x] **5.1** 编写 `EditScreen` UI（表单：类型选择器、target 输入框、username、password、note + 保存按钮）
- [x] **5.2** URL 类型时 target 字段增加 URL 格式校验，App 类型时不做校验
- [x] **5.3** 实现密码可见/隐藏切换（眼睛图标，点击切换 inputType）
- [x] **5.4** 实现 `EditViewModel`（表单状态管理、校验、保存新增/更新到 Repository）
- [x] **5.5** 编辑模式复用 EditScreen（传入 entryId → 加载已有数据 → 修改后保存）

---

## 阶段六：密码生成器

- [x] **6.1** 实现密码生成逻辑（可配置：长度 8-64、包含大写、包含小写、包含数字、包含符号）
- [x] **6.2** 编写密码生成器弹窗 UI（Slider 调长度 + Checkbox 选字符类型 + 实时预览 + 一键填充到密码字段）
- [x] **6.3** 添加密码强度指示条（弱/中/强，基于熵值计算）

---

## 阶段七：导出/导入模块

- [x] **7.1** 实现 `BackupEncryption` 工具类（AES-256-GCM 加密/解密数据流）
- [x] **7.2** 实现导出功能（读取全部 Entry → 序列化 JSON → AES-256-GCM 加密 → 写入 `.mpbak` 文件到用户指定目录）
- [x] **7.3** 实现导入功能（用户选择 `.mpbak` 文件 → 输入导出密码 → 解密 → JSON 反序列化 → 合并到数据库）
- [x] **7.4** 编写 `BackupScreen` UI（导出按钮 + 导入按钮 + 操作结果提示）
- [x] **7.5** 导入覆盖策略选择（追加合并 / 清空后导入，提示用户确认）

---

## 阶段八：收尾与打磨

- [x] **8.1** 全局复制成功提示（Snackbar/Toast，复制后 15 秒自动清除剪贴板）
- [x] **8.2** 列表项进出动画（AnimatedVisibility + 添加删除过渡动效）
- [x] **8.3** 应用图标（自适应图标 adaptive-icon，前景矢量图 + 背景 Material You 取色层）
- [x] **8.4** 整体回归测试（安装 → 设主密码 → 添加网址/App条目 → 搜索 → 编辑 → 删除 → 导出 → 卸载重装 → 导入 → 指纹解锁）
