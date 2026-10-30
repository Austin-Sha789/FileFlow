# 软件更新与发布

Settings → Software updates（设置 → 软件更新）支持手动检查正式版、下载对应系统与架构的安装包，以及用户确认后启动安装。网络请求、下载和校验均在后台执行；离开设置页不会中断下载。下载完成不会自动运行安装包，点击安装后再次校验，打开成功才退出 FileFlow。macOS 的 DMG 会打开磁盘映像，后续按系统流程安装。

## 更新来源与版本

- 来源固定为本仓库 GitHub Releases 的 latest API，仅接受正式发布，跳过草稿和预发布。
- `pom.xml` 的 `project.version` 是版本来源；构建时仅过滤 `app-version.properties`，程序读取该资源进行语义版本比较。开发版本目前为 `1.0.0-SNAPSHOT`。
- 推送代码不会生成更新。目前仓库没有正式 Release，检查时会显示“目前尚未发布正式版本”。
- 当前仓库尚无原生安装包发布流水线。此功能完成了检查、下载、校验和调用安装包的流程；首次发布仍需准备实际可安装、可升级的包。

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

1. 修改 `pom.xml` 为正式版本，例如 `1.1.0`，运行测试并构建。
2. 在各目标系统构建原生安装包，并验证旧版本升级、卸载和数据保留。
3. 创建对应的 Git 标签与正式 GitHub Release，上传按上述规则命名的安装包；发布说明会在设置页显示。
4. 老版本用户点击“检查更新”即可下载；下载完成后点击“安装更新”并确认。

首次分发安装包前，还需把当前相对路径 `fileflow.db` 迁移到稳定的用户数据目录，并确定 Windows 安装器的升级标识、Mac 应用身份及签名方式。这些属于安装包发布工作，当前检查更新功能不会改动用户数据库。原生包应在对应操作系统构建；不能仅在 Windows 上生成并验证 Mac 安装包。

## 验证

新增版本、网络和 JavaFX 流程测试，覆盖架构匹配、无 Release、限流、超时、取消、校验失败、损坏包拒绝、安装前篡改、切换设置页保留任务，以及明确点击安装才调用安装器。测试使用本地 HTTP 附件和注入的安装回调，不执行真实安装器。
