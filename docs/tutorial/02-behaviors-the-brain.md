# 第二章：行为大脑 - behaviors.xml 深度剖析

欢迎来到第二章。如果说 [`actions.xml`](01-actions-the-foundation.md) 是桌宠的“四肢和肌肉”，定义了它能做什么动作，那么 [`conf/behaviors.xml`](conf/behaviors.xml) 就是它的“大脑”，决定了它在何时、何地、以何种倾向去做什么。

## 2.1 从 XML 到 Java 对象：行为的“蓝图”

与 `ActionBuilder` 类似，程序在启动时会为 `behaviors.xml` 中的每一个 `<Behavior>` 标签创建一个 `com.group_finity.mascot.config.BehaviorBuilder` 对象。

`BehaviorBuilder.java` 的构造函数（简化版）揭示了它如何收集决策所需的所有信息：
```java
// src/com/group_finity/mascot/config/BehaviorBuilder.java
public BehaviorBuilder(final Configuration configuration, final Entry behaviorNode, final List<String> conditions) {
    // 读取基本属性
    this.name = behaviorNode.getAttribute("Name"); // "SitDown"
    this.frequency = Integer.parseInt(behaviorNode.getAttribute("Frequency")); // 100

    // 收集所有嵌套的 <Condition>
    this.conditions = new ArrayList<String>(conditions);
    this.getConditions().add(behaviorNode.getAttribute("Condition"));

    // 读取 <NextBehavior> 列表
    for (final Entry nextList : behaviorNode.selectChildren("NextBehaviorList")) {
        // ... 在这里递归地为 <BehaviorReference> 创建新的 BehaviorBuilder
    }
}
```
这个 `BehaviorBuilder` “蓝图”存储了行为名称、频率、所有生效条件以及后续行为列表。

## 2.2 决策核心：`buildNextBehavior()` 方法

桌宠的每一次行为决策，都源于 `Configuration.java` 中一个名为 `buildNextBehavior()` 的核心方法。这个方法的逻辑，完美地诠释了 `<Condition>` 和 `Frequency` 是如何协同工作的。

以下是 `buildNextBehavior()` 的简化版伪代码，展示了其工作流程：
```java
// src/com/group_finity/mascot/config/Configuration.java 伪代码
public Behavior buildNextBehavior(Mascot mascot) {

    // 1. 筛选候选列表
    List<BehaviorBuilder> candidates = new ArrayList<>();
    long totalFrequency = 0;

    // 遍历所有已知的 BehaviorBuilder
    for (BehaviorBuilder builder : this.getAllBehaviorBuilders()) {
        // 检查所有 <Condition> 是否为真
        if (builder.isEffective(mascot)) {
            candidates.add(builder);
            totalFrequency += builder.getFrequency();
        }
    }

    // (此处省略了处理 <NextBehavior> 的逻辑，我们稍后讨论)

    // 2. 加权随机选择
    if (totalFrequency == 0) {
        // 如果没有可用的行为，就默认执行 Fall (下落)
        return buildBehavior("Fall");
    }

    double random = Math.random() * totalFrequency;

    for (BehaviorBuilder candidate : candidates) {
        random -= candidate.getFrequency();
        if (random < 0) {
            // "命中"了！创建并返回这个行为的实例
            return candidate.buildBehavior();
        }
    }

    return null;
}
```

**代码解读**:
1.  **筛选**: 程序首先会遍历 **所有** 在 `behaviors.xml` 中定义的 `BehaviorBuilder`。对于每一个 `Builder`，它会调用 `isEffective()` 方法来检查其所有的 `<Condition>` 条件。只有 **所有条件都满足** 的 `Builder` 才会被加入到 `candidates` (候选人) 列表中。
2.  **加权随机**: 在收集了所有符合条件的候选行为后，程序会计算出它们的 `Frequency` 总和 (`totalFrequency`)。然后，它生成一个 `0` 到 `totalFrequency` 之间的随机数，并通过逐个减去候选行为的 `Frequency` 来确定最终应该选择哪一个。这是一种非常经典的 **加权随机算法**。

## 2.3 实例剖析：一个完整的决策流程

让我们通过一个完整的例子，来模拟桌宠的决策过程。

**场景**: 桌宠当前正趴在屏幕底部（`Sprawl` 动作），刚刚完成了这个动作。现在，它需要决定下一步做什么。

1.  **筛选候选行为**:
    程序开始遍历 [`behaviors.xml`](conf/behaviors.xml)。
    -   `<Behavior Name="ClimbWall" ...>`: 不满足条件，因为桌宠不在墙上。**排除**。
    -   `<Behavior Name="ClimbCeiling" ...>`: 不满足条件，因为桌宠不在天花板上。**排除**。
    -   `<Condition Condition="#{mascot.environment.floor.isOn(mascot.anchor)}">`: **条件满足！** 程序进入这个区块，开始评估里面的行为：
        -   `<Behavior Name="StandUp" Frequency="200" ...>`: **入选**。
        -   `<Behavior Name="SitDown" Frequency="200" ...>`: **入选**。
        -   `<Behavior Name="WalkAlongWorkAreaFloor" Frequency="100" ...>`: **入选**。
        -   ... 其他所有在地面条件下的行为都入选。

2.  **处理 `<NextBehavior>`**:
    程序会检查上一个完成的行为，也就是 `Sprawl` (趴着)。在 `behaviors.xml` 中，我们并没有为 `Sprawl` 定义 `<NextBehavior>`。但是，让我们看看 `LieDown` (躺下) 的例子：
    ```xml
    <Behavior Name="LieDown" Frequency="0">
        <NextBehavior Add="false">
            <BehaviorReference Name="SitDown" Frequency="100" />
            <BehaviorReference Name="CrawlAlongWorkAreaFloor" Frequency="100" />
        </NextBehavior>
    </Behavior>
    ```
    -   `isNextAdditive()`: `Configuration.java` 会检查 `LieDown` 对应的 `BehaviorBuilder` 的 `isNextAdditive()` 方法的返回值（它读取的是 `Add` 属性）。
    -   `Add="false"`: 如果返回 `false`，程序会 **清空** 之前通过全局筛选得到的所有候选行为，只把 `<NextBehavior>` 中定义的 `SitDown` 和 `CrawlAlongWorkAreaFloor` 加入候选列表。
    -   `Add="true"`: 如果返回 `true`，程序则会 **保留** 全局候选列表，并将 `<NextBehavior>` 中的行为 **添加** 进去。

3.  **最终决策**:
    回到我们的 `Sprawl` 场景，由于没有 `<NextBehavior>` 的干预，候选列表就是所有在地面上可执行的行为。假设总 `Frequency` 是 1000。程序生成一个 0-1000 的随机数，比如 `450`。
    -   `450 -= 200` (`StandUp`) => `250`
    -   `250 -= 200` (`SitDown`) => `50`
    -   `50 -= 100` (`WalkAlong...`) => `-50`
    -   **命中！** `random` 变成了负数，所以程序最终决定，桌宠的下一个行为是 `WalkAlongWorkAreaFloor`。`Configuration` 会调用 `WalkAlongWorkAreaFloor` 对应的 `BehaviorBuilder` 的 `buildBehavior()` 方法，创建一个新的 `Behavior` 实例并返回给 `Mascot` 对象。

---

**章节总结**

在本章中，我们深入到了 Shimeji-ee 的“大脑”中枢：
-   `<Behavior>` 标签在启动时被解析为 `BehaviorBuilder`“蓝图”，存储了决策所需的所有信息。
-   我们剖析了 `Configuration.java` 中的 `buildNextBehavior()` 方法，理解了 **筛选** 和 **加权随机** 的决策过程。
-   我们学习了 `<NextBehavior>` 是如何通过 `Add` 属性来干预和引导决策流，从而创造出更具逻辑性的行为链。

现在，你已经不仅知道如何修改行为，更知道了这些修改在代码层面是如何生效的。在下一章，我们将解锁一个极为强大的功能：**脚本引擎**。