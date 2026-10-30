# macOS 原生窗口标题栏

Mac 启动时不再使用自绘 Windows 标题栏。`OceanWindowChrome` 按操作系统选择窗口装饰：macOS 使用支持时的 `StageStyle.UNIFIED`，否则使用 `DECORATED`；由系统提供左上角原生红绿灯、窗口缩放、全屏和边框操作。应用 Logo 和名称移到工具栏最右侧。Windows 继续使用原有左侧品牌及右侧三个窗口按钮。

当前 JavaFX 21 的系统标题栏独立于应用工具栏：红绿灯在最顶端的系统标题栏左上，搜索、工作区入口和右侧品牌在其下方工具栏。系统装饰的颜色和按钮状态由 macOS 与运行时管理。没有使用内部 Cocoa 接口或升级 JavaFX 依赖。

Mac 弹窗同样保留原生系统标题栏，保留原有标题、正文、表单、暗色内容和模态行为。Mac 分支不安装 Windows 标题按钮或自定义边缘缩放过滤器。切换中英文不会把品牌移回左侧。

验证：完整 Maven 回归共 153 项，152 项通过、1 项 Mac 视频原生测试在 Windows 上按平台跳过。新增 JavaFX 测试在 Windows 上验证两个平台策略的窗口类型、品牌顺序、Windows 按钮保留、语言切换及 Mac 弹窗原内容保留；这不等于在 Mac 上验证了系统绘制的红绿灯外观。

Mac 实机检查：同步代码后完全退出旧进程，执行 `mvn clean javafx:run`。确认主窗口左上原生红绿灯、Logo 位于工具栏右上、窗口拖动/缩放/绿灯操作可用，打开“管理文件夹”和删除确认弹窗，检查系统标题栏与原内容均正常。

[JavaFX 21 StageStyle 文档](https://openjfx.io/javadoc/21/javafx.graphics/javafx/stage/StageStyle.html)
