# 第一章：动作词典 - actions.xml 深度剖析

欢迎来到教程的第一章。在本章中，我们将深入探索 [`conf/actions.xml`](conf/actions.xml) 文件。你可以把这个文件想象成一本详尽的 **动作词典**，它定义了桌宠身体能做的每一个 **独立、具体** 的动作。掌握它，是创造生动桌宠的第一步。

## 1.1 从 XML 到 Java 对象：动作的“蓝图”

当我们谈论一个 `<Action>` 标签时，它在程序启动时并不会立刻变成一个可执行的动作。相反，程序会为每一个 `<Action>` 标签创建一个“蓝图”对象，这个对象的 Java 类是 `com.group_finity.mascot.config.ActionBuilder`。

让我们看 `ActionBuilder.java` 的构造函数（简化版）：
```java
// src/com/group_finity/mascot/config/ActionBuilder.java
public ActionBuilder( final Configuration configuration, final Entry actionNode, ... )
{
    // 从 XML 节点中读取属性值
    name = actionNode.getAttribute( "Name" ); // 例如 "Sit"
    type = actionNode.getAttribute( "Type" ); // 例如 "Stay"
    
    // 读取所有的 <Animation> 节点
    for( final Entry node : actionNode.selectChildren( "Animation" ) )
    {
        getAnimationBuilders( ).add( new AnimationBuilder( schema, node, imageSet ) );
    }

    // 读取所有的 <ActionReference> 或内嵌的 <Action> 节点
    for( final Entry node : actionNode.getChildren( ) )
    {
        if( node.getName( ).equals( "ActionReference" ) ) { ... }
        else if( node.getName( ).equals( "Action" ) ) { ... }
    }
}
```
如代码所示，`ActionBuilder` 在被创建时，会 **解析** `<Action>` 标签的所有属性和子节点，并将这些信息储存在自己内部。它就像一个模具，随时准备根据这些信息“铸造”出一个真正的 `Action` 实例。

## 1.2 原子动作：桌宠的一举一动

原子动作是构成一切复杂行为的基础，主要有 `Stay`, `Move` 和 `Animate` 三种类型。

### 1.2.1 `Type="Stay"`: 静止的艺术

`Stay` 类型的动作意味着桌宠会保持在原地不动，只播放动画。

**XML 实例 (`Sit`)**:
```xml
<!-- 位于 conf/actions.xml -->
<Action Name="Sit" Type="Stay" BorderType="Floor">
    <Animation>
        <Pose Image="/shime11.png" ImageAnchor="64,128" Velocity="0,0" Duration="250" />
    </Animation>
</Action>
```
**工作原理**:
当这个动作被执行时，`ActionBuilder` 会创建一个 `com.group_finity.mascot.action.Stay` 类的实例。`Stay.java` 类的 `next()` 方法（每个 tick 调用一次）非常简单：它只是检查当前姿势的持续时间是否结束。在 `Sit` 这个例子中，因为只有一个 `<Pose>` 且 `Duration` 长达 250 tick，所以桌宠会保持这个姿势长达约10秒。

### 1.2.2 `Type="Move"`: 移动的逻辑

`Move` 类型的动作会在播放动画的同时，改变桌宠的坐标。

**XML 实例 (`Walk`)**:
```xml
<!-- 位于 conf/actions.xml -->
<Action Name="Walk" Type="Move" BorderType="Floor">
    <Animation>
        <Pose Image="/shime1.png" ImageAnchor="64,128" Velocity="-2,0" Duration="6" />
        <Pose Image="/shime2.png" ImageAnchor="64,128" Velocity="-2,0" Duration="6" />
        <Pose Image="/shime1.png" ImageAnchor="64,128" Velocity="-2,0" Duration="6" />
        <Pose Image="/shime3.png" ImageAnchor="64,128" Velocity="-2,0" Duration="6" />
    </Animation>
</Action>
```
**工作原理**:
这次，`ActionBuilder` 会创建一个 `com.group_finity.mascot.action.Move` 类的实例。`Move.java` 类的 `next()` 方法除了处理动画帧的切换外，还会执行类似以下伪代码的逻辑：
```java
// Move.java 伪代码
void next() {
    super.next(); // 处理动画计时和帧切换
    
    // 获取当前 Pose 的速度
    int velX = this.getCurrentAnimation().getCurrentPose().getVelocityX();
    int velY = this.getCurrentAnimation().getCurrentPose().getVelocityY();

    // 更新桌宠的锚点坐标
    mascot.getAnchor().translate(velX, velY);
}
```
这就是为什么 `<Pose>` 中的 `Velocity` 如此重要：它直接决定了 `Move` 类动作在每一帧如何改变桌宠的位置。

## 1.3 复合动作：行为的编排

复合动作是将原子动作组合成更复杂序列的粘合剂。

### 1.3.1 `Type="Sequence"`: 剧本

`Sequence` 动作像一个剧本，规定了子动作的 **执行顺序**。

**XML 实例 (`WalkRightAlongFloorAndSit`)**:
```xml
<!-- 位于 conf/actions.xml -->
<Action Name="WalkRightAlongFloorAndSit" Type="Sequence" Loop="false">
    <ActionReference Name="Walk" TargetX="${...}" />
    <ActionReference Name="Stand" Duration="${...}" />
    <ActionReference Name="Look" LookRight="false" />
    <ActionReference Name="Stand" Duration="${...}" />
    <ActionReference Name="Sit" Duration="${...}" />
</Action>
```
**工作原理**:
当 `ActionBuilder` 构建一个 `Sequence` 动作时，它会创建一个 `com.group_finity.mascot.action.Sequence` 类的实例，并将所有引用的 `Action` 作为列表传递进去。`Sequence.java` 的 `next()` 方法的逻辑如下（伪代码）：
```java
// Sequence.java 伪代码
void next() {
    // 获取当前正在执行的子动作
    Action currentAction = this.actions.get(this.currentIndex);
    
    // 如果当前子动作还有下一步
    if (currentAction.hasNext()) {
        currentAction.next(); // 执行子动作的下一步
    } else {
        // 否则，切换到下一个子动作
        this.currentIndex++;
    }
}
```
它就像一个指挥家，一个接一个地执行它的子动作，直到整个序列完成。

### 1.3.2 `Type="Select"`: 分岔路口

`Select` 动作为桌宠的行为提供了 **决策能力**。它会按顺序评估所有子动作的 `Condition`，并执行 **第一个** 满足条件的。

**XML 实例 (`Fall` 动作的结尾部分)**:
```xml
<!-- 位于 conf/actions.xml -->
<Action Type="Select">
    <Action Type="Sequence" Condition="${mascot.environment.floor.isOn(mascot.anchor) || ...}">
        <ActionReference Name="Bouncing" />
        <ActionReference Name="Stand" />
    </Action>
    <ActionReference Name="GrabWall" Duration="100" />
</Action>
```
**工作原理**:
`Select.java` 的逻辑在 **初始化时** (`init()` 方法) 就已经确定了。它的伪代码如下：
```java
// Select.java 伪代码
void init(Mascot mascot) {
    // 遍历所有子动作
    for (Action childAction : this.actions) {
        // 检查该子动作是否有 Condition，并评估它
        if (childAction.hasCondition() && childAction.getCondition().isTrue(mascot)) {
            // 如果条件为真，就选择这个子动作作为后续要执行的动作
            this.selectedAction = childAction;
            return; // 停止遍历
        }
    }
    // 如果所有带条件的动作都不满足，则选择最后一个没有条件的动作（如果有）
    this.selectedAction = this.defaultAction;
}
```
在这个例子中，程序会先检查 `Condition="${mascot.environment.floor.isOn(mascot.anchor)}"`。如果桌宠确实掉在了地上，这个条件就为真，`Select` 动作就会选择执行包含 `Bouncing` 的那个 `Sequence`。如果条件为假（比如桌宠撞在了墙上），它就会跳过这个 `Sequence`，选择执行下一个没有条件的 `ActionReference`——`GrabWall`。

---

**章节总结**

在本章中，我们不仅学习了 `actions.xml` 的语法，还深入到了其背后的 Java 实现：
-   `<Action>` 标签在启动时被解析为 `ActionBuilder`“蓝图”。
-   不同的 `Type` 对应不同的 Java `Action` 子类 (`Stay`, `Move` 等)，它们各自拥有不同的执行逻辑。
-   复合动作 (`Sequence`, `Select`) 通过在内部管理一个 `Action` 列表，实现了对原子动作的编排和决策。

理解了 XML 和 Java 之间的这层关系，你就真正掌握了动作系统的核心。在下一章，我们将用同样的视角，去剖析更为复杂的行为系统。