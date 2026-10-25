# 校园资源预约系统 V2.1

本目录保存校园资源预约系统任务7的增量版本。V2.1 基于 V2.0 的用户管理、资源管理、角色权限和文件持久化功能，新增泛型 `Result<T>`，统一登录、注册、资源操作与查询的业务返回结果。控制台根据成功标记、提示消息和返回数据展示结果；本阶段仍使用控制台交互，尚未进行三层架构重构。

## 功能

- 保留登录、注册和退出菜单。启动时从 `user.txt`、`resource.txt` 加载数据；若用户文件中没有管理员账号，则自动创建 `admin / 123456`。
- 注册成功返回 `Result<Void>`；登录成功返回包含 `User` 的 `Result<User>`。重复用户名、用户不存在、密码错误、参数为空等业务错误显示中文提示。
- 管理员可新增、查看、修改和删除资源；学生只能查看状态为 `AVAILABLE` 的资源。原有角色权限、字段校验和文件持久化规则保持不变。
- 资源新增成功返回包含新资源的 `Result<CampusResource>`；修改、删除返回 `Result<Void>`；资源列表查询返回 `Result<List<CampusResource>>`。按 ID 查询资源返回 `Result<CampusResource>`，不存在时返回失败消息。
- `Result<T>` 包含 `success`、`msg`、`data` 三个字段，并提供携带数据或消息的 `success(...)` 及 `fail(String msg)` 静态工厂方法。失败结果的 `data` 为 `null`。
- 菜单按 `isSuccess()` 判断操作结果，显示 `msg`，成功且有数据时再展示数据。业务异常和文件保存异常在交互调用处转换为友好的失败结果，不打印异常堆栈。
- 资源新增、修改和删除仍先构造待保存集合，写入 `resource.txt` 成功后再更新内存集合；注册也先保存用户，再加入内存集合，避免保存失败后仍显示操作成功。

当前版本还没有预约实体和预约记录，无法按真实预约记录判断资源能否删除。预约模块加入后，需要补充关联校验。

## 项目结构

```text
task07-v2.1
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

`SystemMain` 继续负责控制台输入、角色菜单和业务流程；`Result<T>` 只封装操作结果、消息与数据，不处理业务校验或文件读写；`User`、`CampusResource` 承载数据；`FileUtil` 负责文件创建、解析与保存。本次只统一返回值，没有提前拆分 UI、Service 和 DAO。

## 用户文件格式

`user.txt` 使用 UTF-8 编码，每行保存一名用户，字段顺序如下：

```text
username|password|role|registerTime
```

例如：

```text
alice|abc123|student|2026-10-17T14:30:00
```

时间使用 `LocalDateTime` 的标准文本形式保存和解析。由于竖线 `|` 是字段分隔符，用户名、密码和角色不能包含该字符。当前版本以明文保存练习数据，不适用于实际账户安全场景。

## 资源文件格式

`resource.txt` 使用 UTF-8 编码，每行保存一个资源，字段顺序如下：

```text
id|name|type|status|location|description
```

例如：

```text
550e8400-e29b-41d4-a716-446655440000|一号会议室|教室|AVAILABLE|教学楼1层|可容纳20人
```

状态只能是 `AVAILABLE`（可预约）、`OCCUPIED`（占用）或 `DISABLED`（停用）。描述可以为空。由于竖线 `|` 是字段分隔符，资源字段不能包含该字符。

`user.txt` 和 `resource.txt` 均使用相对路径，文件会创建在程序的当前工作目录。使用 IDE 运行时，请确认工作目录设置在本任务目录，避免数据文件出现在其他位置。`Result<T>` 只用于运行时传递操作结果，不写入数据文件。

## 环境与运行

需要 JDK 8，无第三方依赖。在 `task07-v2.1` 目录打开终端执行：

```bash
javac -encoding UTF-8 -d out -sourcepath src src/SystemMain.java
java -cp out SystemMain
```

`javac` 会根据 `SystemMain.java` 中的引用，从 `src` 自动编译 `Result`、`CampusResource`、`User`、`FileUtil` 和 `exception` 包内需要的类。也可以在 IntelliJ IDEA 中打开本目录并运行 `SystemMain.main`。运行配置的工作目录应设为 `task07-v2.1`。

## 建议验收场景

| 场景 | 预期结果 |
| --- | --- |
| 管理员、学生使用正确账号密码登录 | 登录结果成功，携带对应 `User`，进入正确身份菜单 |
| 用户不存在、密码错误或输入为空 | 显示相应中文失败消息，不读取失败结果中的 `data`，程序可继续使用 |
| 注册成功后重启程序 | 用户仍能登录；注册成功结果提供提示消息 |
| 注册重复用户名或非法参数 | 显示失败消息，不新增用户或破坏原有数据 |
| 管理员新增资源 | 返回成功消息及新资源 ID，资源状态为 `AVAILABLE`，重启后仍存在 |
| 管理员查看全部资源 | 显示查询提示和资源列表；空列表显示“暂无资源” |
| 学生查看资源 | 只显示状态为 `AVAILABLE` 的资源，不显示占用或停用资源 |
| 按不存在的 ID 查询资源 | `findResourceById` 返回失败 `Result`，`data` 为 `null` |
| 管理员修改、删除资源 | 返回成功消息；重启后变更仍存在 |
| 资源 ID 不存在、状态非法或权限不足 | 显示友好失败消息，程序不崩溃 |
| 用户或资源文件保存失败 | 显示保存失败提示，内存集合不报告未完成的操作为成功 |
| 打印包含数据的 `Result` | `toString()` 显示成功标记、消息和数据 |

## 本次设计要点

V2.0 的方法有的返回用户或资源对象，有的没有返回值，内部查找还可能返回 `null`，控制台需要分别处理。V2.1 使用 `Result<T>` 统一业务操作的对外结果：`success` 表示是否成功，`msg` 给出中文提示，`data` 保存与操作对应的具体类型。泛型让登录、资源查询和列表查询共用同一个结果结构，而无需把数据强制转换为具体类型。

自定义业务异常仍用于中断非法操作；交互调用处将其转换为 `Result.fail(...)` 展示给用户。私有查找辅助方法仍可用 `null` 或 `-1` 表示未找到，再由业务查询入口转换为明确的失败结果。`FileUtil` 继续负责底层 IO，`Result` 不参与持久化。本次只处理返回值标准化，为后续分层重构保留清晰的业务结果约定。
