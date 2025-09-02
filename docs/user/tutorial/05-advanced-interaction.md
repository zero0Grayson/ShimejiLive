# 第五章：高级课题 - 桌宠的社交

欢迎来到本系列教程的最终章。在这里，我们将深入探讨 Shimeji-ee 最吸引人的特性之一：**桌宠之间的互动**。我们将通过剖析 XML 和 Java 源码，揭示其背后的工作原理，并告诉你如何创造属于你自己的、独特的互动动画。

## 5.1 互动的核心：`Embedded` (嵌入式) 动作

回顾第一章，我们知道 `<Action>` 有 `Stay`, `Move` 等类型。但实现互动的关键，是一种特殊的类型：`Embedded`。

```xml
<!-- 位于 conf/actions.xml -->
<Action Name="PullUpShimeji1" Type="Embedded" Class="com.group_finity.mascot.action.Breed"
        BornX="-32" BornY="96" BornBehavior="PullUp">
    ...
</Action>
```

-   **`Type="Embedded"`**: 这个类型告诉 Shimeji-ee，该动作的逻辑 **不由 XML 定义**，而是由一个 Java 类来处理。
-   **`Class="com.group_finity.mascot.action.Breed"`**: 这就是实现了该动作逻辑的 Java 类。这意味着，当 `PullUpShimeji1` 动作被调用时，程序会去执行 `Breed.java` 文件中的代码。

这种机制非常强大，因为它允许开发者实现 XML 标签无法描述的复杂逻辑，比如物理模拟、文件操作，以及我们这里要讲的——**与其他桌宠的互动**。

## 5.2 实例剖析：`PullUpShimeji` (拔萝卜)

让我们一步步分解 `PullUpShimeji` 这个最经典的互动动作，看看它在 XML 和 Java 层面是如何协同工作的。

### 源码视角：`Breed.java`

互动的核心逻辑位于 `com.group_finity.mascot.action.Breed.java`。以下是其 `init()` 方法的简化版伪代码：
```java
// src/com/group_finity/mascot/action/Breed.java (伪代码)
@Override
public void init(Mascot mascot) {
    super.init(mascot);

    // 从 XML 中读取 BornX, BornY, BornBehavior 等参数
    int bornX = Integer.parseInt(this.getParams().get("BornX"));
    int bornY = Integer.parseInt(this.getParams().get("BornY"));
    String bornBehavior = this.getParams().get("BornBehavior");

    // 计算新桌宠的出生坐标（相对于发起者的锚点）
    Point newAnchor = new Point(mascot.getAnchor().x + bornX, mascot.getAnchor().y + bornY);

    // 获取 Manager 对象，它是所有桌宠的管理器
    Manager manager = mascot.getManager();

    // 命令 Manager 在指定位置创建一个新的桌宠
    Mascot newMascot = manager.createMascot(mascot.getImageSet());
    newMascot.setAnchor(newAnchor);

    // !!! 互动的关键 !!!
    // 强制设置新桌宠的第一个行为
    newMascot.setBehavior(configuration.buildBehavior(bornBehavior));
}
```
**代码解读**:
这段 Java 代码清晰地展示了互动的全部流程：
1.  **读取参数**: 从 XML 的 `<Action>` 标签中获取自定义参数。
2.  **创建实例**: 通过 `Manager` 对象，在指定位置创建一个全新的 `Mascot` 实例。
3.  **行为注入**: 这是最关键的一步。它直接调用新桌宠的 `setBehavior()` 方法，将 `BornBehavior` 属性中指定的行为（例如 `PullUp`）**强制** 设为它的第一个行为。

### 完整的互动链

现在，我们可以将 XML 和 Java 的逻辑串联起来，形成一个完整的互动故事：

1.  **发起者 (XML)**:
    -   某个行为（比如 `PullUpShimeji`）被触发。
    -   它是一个 `Sequence`，第一步执行了 `PullUpShimeji1` 这个 `Embedded` 动作。

2.  **执行互动 (Java)**:
    -   `Breed.java` 被执行。
    -   它在发起者旁边创建了一个新的桌宠（我们称之为“响应者”）。
    -   它对响应者说：“你的第一个任务是执行 `PullUp` 行为！”

3.  **响应者 (XML)**:
    -   新生的响应者被创建后，它的“大脑” (`behaviors.xml`) 被暂时忽略了。
    -   它的第一个行为被强制设定为 `PullUp`。
    -   `PullUp` 行为调用了 [`actions.xml`](conf/actions.xml) 中同名的 `PullUp` 动作：
        ```xml
        <Action Name="PullUp" Type="Sequence">
            <ActionReference Name="Falling" InitialVX="${...}" InitialVY="-40" />
            <ActionReference Name="Bouncing" />
        </Action>
        ```
    -   响应者完美地执行了一个被向上抛飞、然后落地的动画。

至此，一次由“主导者”发起，由“响应者”完成的、看似复杂的互动，就通过 `Embedded` 动作和行为注入机制，被完美地实现了。

## 5.3 教程总结与展望

恭喜你，你已经完成了 Shimeji-ee 的全部核心教程！

让我们回顾一下我们走过的路：
-   在 **第一章**，我们学会了如何使用 `actions.xml` 来定义桌宠的 **一举一动**。
-   在 **第二章**，我们通过 `behaviors.xml` 探究了桌宠的 **所思所想**。
-   在 **第三章**，我们解锁了 **脚本引擎**，学会了赋予桌宠动态能力。
-   在 **第四章**，我们通过实例，掌握了如何塑造桌宠的 **独特个性**。
-   在 **本章**，我们深入源码，揭示了桌宠之间 **社交互动** 的底层奥秘。

你现在所掌握的知识，已经足够让你：
-   **修改和优化** 任何现有的 Shimeji-ee 角色包。
-   为你自己的原创角色 **从零开始编写** 全套的动作和行为。
-   利用 `Breed` 机制，创造出 **新颖有趣的互动** 动画。
-   (高级) 尝试编写自己的 `Embedded Action` Java 类，来实现 XML 无法做到的、更高级的互动逻辑。

前方的道路已经为你敞开。去发挥你的想象力，创造出那个只属于你的、独一无二的桌面伙伴吧！