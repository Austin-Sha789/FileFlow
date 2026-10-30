# 软件更新与发布

Settings → Software updates（设置 → 软件更新）支持手动检查正式版、下载对应系统与架构的安装包，以及用户确认后启动安装。网络请求、下载和校验均在后台执行；离开设置页不会中断下载。下载完成不会自动运行安装包，点击安装后再次校验，打开成功才退出 FileFlow。macOS 的 DMG 会打开磁盘映像，后续按系统流程安装。

## 更新来源与版本

- 来源固定为本仓库 GitHub Releases 的 latest API，仅接受正式发布，跳过草稿和预发布。
- `pom.xml` 的 `project.version` 是版本来源；构建时仅过滤 `app-version.properties`，程序读取该资源进行语义版本比较。首个安装包版本为 `1.0.0`。
- 推送代码不会自动公开更新。通过 GitHub Actions 的 `Release installers` 手动运行发布，三个目标平台全部构建、自检并校验成功后才公开 Release。
- 原生安装包流水线位于 `.github/workflows/release.yml`，构建脚本为 `scripts/package-release.py`。当前输出 Windows x64 MSI、Mac Apple Silicon DMG、Mac Intel DMG，均内置 Java 运行环境和高清图标。

## 安装包名称

Release 标签使用 `v1.1.0` 这样的版本号，安装包文件名中的版本必须匹配标签：

| 平台 | 示例 |
| --- | --- |
| Windows x64 | `FileFlow-1.1.0-windows-x64.msi` |
| Windows ARM64 | `FileFlow-1.1.0-windows-arm64.msi` |
| Windows EXE 安装器 | `FileFlow-1.1.0-windows-x64-setup.exe` |
| Mac Apple Silicon | `FileFlow-1.1.0-macos-arm64.dmg` |
| Mac Intel | `FileFlow-1.1.0-macos-x64.dmg` |
| Mac 通用 | `FileFlow-1.1.0-macos-universal.dmg` |

Mac 也支持 `.pkg`。优先匹配当前架构，再考虑 Mac 通用包；Windows 优先 MSI。普通便携 EXE、ZIP 和源码包不会作为安装包运行。更新器识别 Mac 包不代表应用的 macOS 移植已完成。

## 校验与存储

安装包必须来自本仓库的 Release 下载地址，HTTPS 跳转限制在 GitHub 及其下载域名。使用 GitHub API 返回的 `digest` 字段（`sha256:...`），同时检查文件大小；下载后及安装前各校验一次。没有 SHA-256 的附件不会启用应用内安装，可重新上传附件以生成校验值。

下载使用临时 `.part` 文件，成功后才转为安装包；取消或失败清理临时文件。Windows 缓存位于 `%LOCALAPPDATA%\FileFlow\Updates`；Mac 位于 `~/Library/Caches/FileFlow/Updates`。缓存中完整且校验通过的包可复用。最大包大小 2 GiB，网络请求超时 15 秒，下载总时长限制 30 分钟。

## 以后发布新版本

1. 修改 `pom.xml` 为正式版本，例如 `1.1.0`，添加 `docs/releases/1.1.0.md` 发布说明，运行回归测试并推送。
2. 在 GitHub Actions → `Release installers` → Run workflow 中填写版本号（不带 v）。
3. 流水线在 Windows、Mac ARM、Mac Intel 上运行测试、生成自带 Java 的应用并启动自检，随后生成安装包。
4. 三个任务全部成功后，发布任务核对附件与 SHA-256，创建目标提交的标签和草稿 Release，附件上传成功才公开。失败时查看 Actions 日志，修复后重试；不要用相同版本覆盖已经公开的包。
5. 老版本用户点击“检查更新”即可下载；下载完成后点击“安装更新”并确认。正式分发前仍应人工验证安装、卸载和跨版本升级。

数据库已移至稳定用户目录：Windows `%LOCALAPPDATA%/FileFlow`，Mac `~/Library/Application Support/FileFlow`。从旧源码运行目录启动时会用 SQLite 一致性复制迁移旧库并保留原文件，已有目标库不会被覆盖。Windows 使用固定升级 UUID，Mac 应用标识为 `com.austin.fileflow`。

首版未进行开发者身份签名，Mac 只有运行所需的 ad-hoc 签名、未经 Apple 公证，首次启动可能受到系统安全检查限制。签名与公证需要后续配置证书。MOV 缩略图的已知问题记录在 Issue #1。原生包在各自目标系统构建，不能在 Windows 上直接生成 Mac 原生安装包。

## 验证

新增版本、网络和 JavaFX 流程测试，覆盖架构匹配、无 Release、限流、超时、取消、校验失败、损坏包拒绝、安装前篡改、切换设置页保留任务，以及明确点击安装才调用安装器。测试使用本地 HTTP 附件和注入的安装回调，不执行真实安装器。
