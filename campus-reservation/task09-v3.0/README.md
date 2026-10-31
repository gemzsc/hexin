# 校园资源预约系统 V2.2

本目录对应任务8“函数式编程”迭代。V2.2 基于 V2.1 增量开发，在保留用户管理、资源管理、角色权限、`Result<T>` 返回结构和文件持久化行为的基础上，使用 Stream、Lambda 和 Optional 改写资源集合查询，并为管理员增加资源名称搜索和按 ID 查询入口。本阶段仍使用控制台交互，尚未进行三层架构重构。

## 功能

- 保留登录、注册和退出菜单。程序启动时从 `user.txt`、`resource.txt` 加载数据；用户文件中没有管理员账号时，自动创建 `admin / 123456`。
- 管理员可新增、查看、修改和删除资源，并可按名称关键词搜索资源、按 ID 查询资源。
- 学生只能查看状态为 `AVAILABLE` 的资源，不能使用管理员资源管理功能。
- 按名称搜索使用 Stream 过滤名称包含关键词的资源，忽略英文大小写，并按资源 ID 的字符串自然顺序升序排列。关键词为空时返回失败 `Result`。
- 按 ID 查询使用 Stream 和 `Optional<CampusResource>` 表达“可能找不到”的结果，再转换为 `Result<CampusResource>`；资源不存在时返回友好失败消息。
- 可预约资源筛选使用 Stream 过滤 `AVAILABLE` 状态，返回 `Result<List<CampusResource>>`。
- 用户和资源仍通过文件持久化。资源新增、修改和删除先保存文件，成功后再更新内存集合。
- 业务操作继续使用 `Result<T>` 传递成功状态、消息和数据；业务校验错误继续使用自定义异常，并在控制台交互处转换为友好提示。

## 项目结构

```text
task08-v2.2
├─ README.md
└─ src
   ├─ CampusResource.java
   ├─ FileUtil.java
   ├─ Result.java
   ├─ SystemMain.java
   ├─ User.java
   └─ exception
      ├─ BusinessException.java
      ├─ EmptyParameterException.java
      ├─ PasswordIncorrectException.java
      ├─ UserAlreadyExistsException.java
      └─ UserNotFoundException.java
```

`SystemMain` 继续负责控制台输入、菜单和当前版本的流程；`CampusResource`、`User` 负责承载数据；`Result<T>` 封装操作结果；`FileUtil` 负责文件读写；`exception` 包定义业务异常。本次没有提前创建 Controller、Service 或 DAO，也没有改变任务10才进行的分层重构安排。

## 用户文件格式

`user.txt` 使用 UTF-8 编码，每行保存一名用户，字段顺序如下：

```text
username|password|role|registerTime
```

例如：

```text
alice|abc123|student|2026-10-17T14:30:00
```

时间使用 `LocalDateTime` 的标准文本形式保存和解析。竖线 `|` 是字段分隔符，用户名、密码和角色不能包含该字符。当前版本以明文保存练习数据。

## 资源文件格式

`resource.txt` 使用 UTF-8 编码，每行保存一个资源，字段顺序如下：

```text
id|name|type|status|location|description
```

例如：

```text
550e8400-e29b-41d4-a716-446655440000|一号会议室|教室|AVAILABLE|教学楼1层|可容纳20人
```

状态为 `AVAILABLE`（可预约）、`OCCUPIED`（占用）或 `DISABLED`（停用）。描述可以为空。竖线 `|` 是字段分隔符，资源字段不能包含该字符。

`user.txt` 和 `resource.txt` 均使用相对路径，文件会创建在程序的当前工作目录。使用 IDE 运行时，请确认工作目录设置为 `task08-v2.2`，避免数据文件出现在其他位置。`Result<T>` 只用于运行时传递操作结果，不写入数据文件。

## 环境与运行

需要 JDK 8，无第三方依赖。在 `task08-v2.2` 目录打开终端执行：

```bash
javac -encoding UTF-8 -d out -sourcepath src src/SystemMain.java
java -cp out SystemMain
```

也可以在 IntelliJ IDEA 中打开本目录并运行 `SystemMain.main`。运行配置的工作目录应设为 `task08-v2.2`。

## 建议验收场景

| 场景 | 预期结果 |
| --- | --- |
| 学生查看可预约资源 | 只显示状态为 `AVAILABLE` 的资源；占用和停用资源不显示 |
| 管理员按名称关键词搜索 | 显示名称包含关键词的资源 |
| 搜索英文资源名称时改变关键词大小写 | 匹配结果不受英文大小写影响 |
| 搜索关键词为空或全为空格 | 返回失败 `Result` 并提示关键词不能为空，程序继续运行 |
| 搜索结果含有多个不同 ID 的资源 | 按 ID 字符串自然顺序升序展示 |
| 管理员按存在的 ID 查询 | 显示查询成功消息和对应资源 |
| 管理员按不存在的 ID 查询 | 显示友好失败消息，不读取失败结果中的数据 |
| 管理员新增、修改、删除资源后重启 | 文件中的资源状态与操作结果一致 |
| 学生进入菜单 | 不显示管理员搜索、按 ID 查询或资源管理入口 |

## 本次设计要点

V2.1 的资源筛选和查找主要通过显式循环完成。V2.2 使用 Stream 表达筛选、排序和结果收集，让查询规则直接体现在处理步骤中。`Optional` 用于表达按 ID 查找可能没有结果的情况，查询入口再把它转换为统一的 `Result`。

Stream 查询不会直接修改原始资源列表；排序针对查询结果。当前资源 ID 是字符串，因此排序采用字符串自然顺序。Optional 的查找结果需要在转换为 `Result` 前处理“存在”和“不存在”两种情况。

本阶段没有要求引入新的设计模式或绘制 UML 类图。资源查询之外的业务规则、文件持久化方式及整体单类结构继续沿用上一版本，为后续迭代保留连续的演进基础。