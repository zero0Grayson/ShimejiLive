# 第三章：脚本引擎 - 赋予桌宠动态能力

欢迎来到第三章。在前面的章节中，我们学习了如何定义桌宠的动作和决策逻辑。现在，我们将解锁一个让桌宠变得真正“鲜活”起来的强大工具：**脚本引擎**。

在 Shimeji-ee 的 XML 文件中，你会看到大量形如 `#{...}` 和 `${...}` 的表达式。这些不仅仅是占位符，它们是可执行的 **脚本代码**，允许你在运行时动态地计算数值、设置条件和传递参数。

## 3.1 脚本引擎的核心：EL (Expression Language)

Shimeji-ee 使用的是 Java 的 **Expression Language (EL)** 引擎（具体来说，是一个名为 JUEL 的实现）。这是一种轻量级的脚本语言，语法与 JavaScript 非常相似，主要用于访问 Java 对象的属性和方法。

### 源码视角：`Variable.java`

脚本的解析和执行，其核心位于 `com.group_finity.mascot.script.Variable.java`。

```java
// src/com/group_finity/mascot/script/Variable.java (简化版)
public class Variable {
    // ...
    private final ValueExpression valueExpression;

    public Variable(String expression) {
        // 创建一个 EL 上下文，用于解析表达式
        SimpleContext context = new SimpleContext(...);
        // 将表达式字符串（如 "#{mascot.anchor.x > 100}"）编译成一个可执行的 ValueExpression 对象
        this.valueExpression = factory.createValueExpression(context, expression, Object.class);
    }

    public Object get(VariableMap variables) {
        // 创建一个新的 EL 上下文，并将当前的变量（如 mascot 对象）放入其中
        SimpleContext context = new SimpleContext(new VariableMapperImpl(variables));
        // 执行编译好的表达式，并返回结果
        return this.valueExpression.getValue(context);
    }
}
```
**工作流程**:
1.  **编译**: 程序启动时，所有 XML 中的表达式都会被 `Variable` 类 **预编译** 成一个 `ValueExpression` 对象。这大大提高了运行时的效率。
2.  **执行**: 当程序需要评估一个条件或计算一个参数时，它会调用 `get()` 方法。这个方法会把当前的 **上下文变量**（最重要的就是 `mascot` 对象本身）提供给 `ValueExpression`，然后执行并返回结果（比如 `true` 或 `false`，或者一个计算出的数值）。

## 3.2 两种表达式，两种用途

在 XML 中，你会看到 `#{...}` 和 `${...}` 两种格式的表达式。它们虽然都使用 EL，但 Shimeji-ee 赋予了它们不同的解析时机和用途。

-   **`#{...}` (Deferred Evaluation - 延迟求值)**: 这种表达式的值 **不会** 在程序加载时计算，而是在 **每次需要时** 都重新计算。它主要用于 `<Condition>` 标签，因为条件判断需要基于桌宠的 **实时状态**。
-   **`${...}` (Immediate Evaluation - 立即求值)**: 这种表达式的值只在 **动作或行为被创建的瞬间** 计算一次，然后这个值就会被固定下来，在整个动作执行期间保持不变。它主要用于为动作传递 **初始参数**。

### 实例对比

**场景**: 让桌宠走到屏幕上的一个随机位置。

**正确的做法 (`${...}`)**:
```xml
<!-- 位于 conf/actions.xml -->
<ActionReference Name="Walk" TargetX="${mascot.environment.workArea.left+64+Math.random()*(mascot.environment.workArea.width-128)}" />
```
**工作原理**: 当 `Walk` 动作 **开始** 的那一刻，`${...}` 表达式会被 **立即计算一次**。`Math.random()` 会生成一个随机数，例如 `0.5`，然后计算出一个具体的目标 X 坐标，比如 `800`。在接下来的整个走路过程中，`TargetX` 的值将 **始终是 800**，桌宠会坚定地走向这个目标。

**错误的做法 (`#{...}`)**:
如果错误地写成 `TargetX="#{...}"`，那么在走路的 **每一帧**，这个表达式都会被 **重新计算**。`Math.random()` 每帧都会生成一个新的随机数，导致桌宠的目标点每时每刻都在疯狂变化，结果就是它会在原地迷惑地来回踱步。

## 3.3 你的武器库：可用的内置对象和变量

在编写 EL 脚本时，你可以访问一系列强大的内置对象，获取关于桌宠自身和外部环境的一切信息。

### 核心对象: `mascot`

`mascot` 对象代表了当前正在执行脚本的桌宠实例 (`com.group_finity.mascot.Mascot.java`)。

**常用属性**:
-   `mascot.anchor.x`: 桌宠锚点的 X 坐标。
-   `mascot.anchor.y`: 桌宠锚点的 Y 坐标。
-   `mascot.lookRight`: 一个布尔值，`true` 表示朝向右边。
-   `mascot.totalCount`: 屏幕上所有桌宠的总数。

### 环境对象: `mascot.environment`

`mascot.environment` 对象 (`com.group_finity.mascot.environment.MascotEnvironment.java`) 提供了关于屏幕、窗口等外部环境的信息。

**常用属性和方法**:
-   `mascot.environment.workArea`: 当前工作区（通常是除任务栏外的桌面）的矩形区域。
    -   `.left`, `.right`, `.top`, `.bottom`, `.width`, `.height`
-   `mascot.environment.screen`: 整个屏幕的矩形区域。
-   `mascot.environment.cursor`: 鼠标指针的坐标。
    -   `.x`, `.y`
-   `mascot.environment.activeIE`: 当前活动窗口的信息。
    -   `.left`, `.right`, `.top`, `.bottom`
    -   `.visible`: 是否可见 (布尔值)
-   `mascot.environment.floor.isOn(mascot.anchor)`: 判断桌宠是否在地面上 (返回布尔值)。
-   `mascot.environment.wall.isOn(mascot.anchor)`: 判断桌宠是否在墙上。
-   `mascot.environment.ceiling.isOn(mascot.anchor)`: 判断桌宠是否在天花板上。

### 其他可用功能

-   **数学函数**: 你可以使用 `Math` 对象，例如 `Math.random()` (生成 0-1 的随机数), `Math.min()`, `Math.max()`。
-   **逻辑运算**: `&&` (与), `||` (或), `!` (非)。
-   **比较运算**: `==`, `!=`, `>`, `<`, `>=`, `<=`。

---

**章节总结**

在本章中，我们揭开了 Shimeji-ee 动态能力的面纱：
-   核心是 **EL 脚本引擎**，它在运行时动态地执行 XML 中的表达式。
-   **`#{...}`** 用于需要 **实时** 判断的 **条件**。
-   **`${...}`** 用于在动作开始时 **一次性** 初始化的 **参数**。
-   我们掌握了 `mascot` 和 `mascot.environment` 这两个强大的内置对象，它们是编写复杂逻辑的钥匙。

