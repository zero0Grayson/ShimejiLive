# 开发者指南

## 项目结构

本项目采用标准的Maven项目结构：

```
dc-ShimejiLive/
├── src/
│   ├── main/
│   │   ├── java/                # 源代码
│   │   │   ├── com/
│   │   │   ├── hqx/
│   │   │   └── module-info.java # Java模块定义
│   │   └── resources/           # 内嵌资源文件
│   └── test/
│       └── java/                # 测试代码
├── conf/                        # 外部配置文件
├── img/                         # 图像资源
├── docs/                        # 文档
│   ├── api/                     # API文档
│   ├── development/             # 开发文档
│   └── user/                    # 用户文档
├── lib/                         # 第三方库（历史遗留）
└── target/                      # Maven构建输出

```

## 构建系统

### 开发环境要求
- JDK 21+
- Maven 3.8+

### 常用命令

```bash
# 编译项目
mvn compile

# 运行项目
mvn -P run

# 打包项目
mvn package

# 创建Windows安装包
mvn -P jpackage

# 只创建便携版
mvn -P jpackage-portable

# 只创建MSI安装包
mvn -P jpackage-msi
```

## 代码组织

### 主要包结构
- `com.group_finity.mascot` - 核心功能
- `com.group_finity.mascot.win` - Windows平台特定实现
- `hqx` - 图像缩放算法

### 模块依赖
项目使用Java模块系统（JPMS），主要依赖：
- `java.desktop` - GUI功能
- `com.sun.jna` - 原生代码调用
- `org.mozilla.rhino` - JavaScript脚本引擎
- `com.formdev.flatlaf` - 现代化UI主题

## 开发指南

### 代码风格
- 使用4空格缩进
- 遵循标准Java命名约定
- 添加适当的注释和JavaDoc

### 测试
测试代码位于`src/test/java`目录下。

### 文档
- API文档：自动生成的JavaDoc
- 开发文档：位于`docs/development/`
- 用户文档：位于`docs/user/`
