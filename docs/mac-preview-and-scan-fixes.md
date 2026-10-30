# Mac 图标、视频与临时扫描

## 行为

- `AppIcon.installDockIcon()` 在主窗口显示后，使用进程级 `java.awt.Taskbar` 设置 Dock 图标。它与窗口图标共用原始高清 PNG；仅在 macOS、支持 ICON_IMAGE 时执行，在 AWT 事件线程运行，不阻塞 JavaFX 界面。
- 视频缩略图优先使用 FFmpeg。Mac 的查找顺序为显式配置、PATH、Homebrew Apple Silicon 路径 `/opt/homebrew/bin`、Intel 路径 `/usr/local/bin`、MacPorts 路径 `/opt/local/bin`。GUI 启动时无需依赖 shell 的 PATH 配置。
- 引擎缺失或解码失败时，Mac 尝试 `/usr/bin/qlmanage -t` 的系统 Quick Look 缩略图。使用独立临时目录、8 秒超时，保留现有后台队列、取消和海报缓存机制。原生输出经过图片格式、大小和尺寸检查，再缩放到最多 512×384。系统不支持的编码仍可能没有预览，不会因此打开外部视频窗口。
- 单独扫描文件夹不再添加或保存工作区快捷方式，工作区为空时也不再临时插入条目。只有“管理文件夹”明确保存的目录会成为工作区条目。扫描结果与扫描记录仍可正常使用，已保存目录可切换到当前会话中的缓存结果。
- 旧版本已保存的条目保留，避免误删用户主动添加的工作区。可以在侧栏右键移除不需要的条目。

## 验证与 Mac 复查

Windows 上完整回归：150 项中 149 项通过，1 项 Mac 原生 Quick Look 测试按平台跳过。覆盖真实 H.264/HEVC MOV 解码、引擎路径查找、原生回退及取消、缩略图尺寸与临时文件清理，以及保存工作区和临时扫描的 JavaFX 集成行为。Dock 和实际 Quick Look 系统行为尚待 Mac 实机确认。

同步本次代码到 Mac 后，先完整退出旧进程，再在仓库目录执行 `mvn clean javafx:run`。检查运行中的 Dock 图标、MOV 在文件列表和 Quick Info 中的预览，以及扫描新目录后侧栏没有新增条目。当前代码尚未提交或推送，先完成同步才能看到变化。

如果 Mac 上已经安装 FFmpeg，可以执行 `mvn -Dfileflow.videoTests=true test`，其中 Mac 专用测试直接用 Quick Look 提取 MOV（FFmpeg 只用于生成测试样本）。无需通过修改操作系统属性在 Windows 上模拟实际 Mac 系统调用。

## 依据

- [Java Taskbar API](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/Taskbar.html)
- [Apple Quick Look 调试工具](https://developer.apple.com/library/archive/documentation/UserExperience/Conceptual/Quicklook_Programming_Guide/Articles/QLDebugTest.html)
