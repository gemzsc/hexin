# 校园资源预约系统 V3.1

本目录对应任务10：三层架构重构。在任务9业务基础上迁移职责，调用顺序为 ConsoleUI → Service → DAO。SystemMain 仅装配依赖和启动。未提前引入 Controller、单例、工厂或数据库。

## 代码职责

- ui.console.ConsoleUI：登录注册、主菜单、管理员与学生菜单、输入解析、列表展示和友好错误提示。
- service.UserService：注册登录规则与管理员初始化。
- service.ResourceService：资源查询、增删改、权限检查及关联预约校验。
- service.ReserveRecordService：预约申请、查询、审核、取消、时间检查及资源状态联动。
- dao：UserDao、ResourceDao、ReserveRecordDao 三个接口。
- dao.file：三个文件实现，加载集合并委托 FileUtil 持久化。
- entity：User、CampusResource、ReserveRecord、ReserveStatus、Result。
- exception：业务异常和 DataAccessException。
- utils.FileUtil：UTF-8 文本解析与保存。提示通过 Consumer 回调交给 ConsoleUI 展示。

Service 通过构造方法接收 DAO 接口。资源和预约服务共享同一组 DAO 加载的数据集合，避免重复加载和不同实例读到不同内存状态。UI 不持有 DAO，也不操作文件。

## 保留的业务规则

学生注册后可登录，只能查看可预约资源及自己的预约。管理员管理资源并审核预约。提交创建 PENDING；通过改为 PASS 并占用资源；驳回为 REJECT；取消 PENDING 或 PASS 改为 CANCEL，只有取消 PASS 才释放资源。存在 PASS 预约时禁止删除资源。查询继续使用 Stream，业务结果使用 Result。

资源采用整体占用模型，通过后立即 OCCUPIED，不支持按不同时段继续预约或自动到期释放。审核和取消涉及两个文件的顺序保存，不具备跨文件事务和崩溃恢复保证。文件 DAO 提供共享集合，Service 构造候选数据、保存成功后更新内存；以后引入并发时还需进一步封装与同步。

## 数据与运行

原文件格式保持不变：user.txt、resource.txt、reserve.txt，每行用竖线分隔，UTF-8 编码。时间用 LocalDateTime 文本，预约状态用枚举名。启动时文件不存在则创建；坏记录提示并跳过。无管理员时初始化 admin / 123456，仅用于课程测试。

在本目录运行，避免相对路径读取其他版本的数据：

```text
javac -encoding UTF-8 -d out -sourcepath src src/SystemMain.java
java -cp out SystemMain
```

使用 Java 8 语法及 API。若使用较新 JDK 验证 Java 8 API 兼容性，可加 --release 8。

## UML 与架构解释

[类图源码](docs/task10-class-diagram.puml) 按实际类、字段和方法整理，包含包边界、DAO 接口实现、三层依赖与异常继承。可用 PlantUML 渲染；类图源码本身可编辑。实体中的用户名和资源 ID 表示标识关联，不是组合关系。

本次体现单一职责、依赖倒置和按业务拆分的小接口。三层分包本身不等于依赖倒置，关键是 Service 依赖 DAO 接口，由启动代码传入文件实现。

## 回归验证

[Task10Regression.java](tests/Task10Regression.java) 是无第三方依赖的检查程序，覆盖注册、登录、重复注册、资源 CRUD、预约合法/非法流转、权限与持久化重新加载。必须在独立空目录运行，避免污染真实数据。

```text
javac -encoding UTF-8 -d out -sourcepath src tests/Task10Regression.java
```

随后进入新建的空测试目录，使用 out 的绝对路径执行 java -cp <out绝对路径> Task10Regression。成功时输出 PASS。完整菜单另需演示学生申请、管理员审核、学生取消和重启查询。
