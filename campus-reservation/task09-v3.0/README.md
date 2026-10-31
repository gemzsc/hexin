# 校园资源预约系统 V3.0

本目录对应任务9“资源预约核心业务开发”。本版本基于任务8 V2.2 增量开发，保留登录注册、资源管理、Stream 查询、Result 统一返回和文件持久化，新增学生申请预约、管理员审核、学生取消及资源状态联动。本阶段业务仍集中在 SystemMain，三层架构重构安排在任务10。

## 功能与权限

- 用户：登录、注册、退出。注册账号默认为学生；启动时若没有管理员角色账号，创建测试管理员 admin / 123456。
- 管理员：资源增删改查、名称搜索、按 ID 查询；查看全部预约，按资源 ID、申请人、预约状态组合筛选；通过或驳回待审核预约。
- 学生：查看可预约资源，提交申请，查询自己的预约，取消自己的待审核或已通过预约。
- 权限校验在预约业务方法中执行。学生不能审核或查看全部预约；管理员不能代替学生提交、取消预约。
- 申请人从当前登录用户取得，预约编号使用 UUID 生成，申请时间自动记录。

## 预约规则

提交申请时，资源必须存在且状态为 AVAILABLE；开始时间必须早于结束时间，结束时间不能早于当前时间。时间输入使用 LocalDateTime 标准格式，例如 2026-11-02T09:00；实际测试应选择尚未过期的时间。

同一资源的已有 PASS 预约参与时间冲突检查。采用左闭右开时间段：新开始早于旧结束，且新结束晚于旧开始时视为重叠；首尾相接不算重叠。备注可以为空，但不能含竖线或换行符。

| 操作 | 预约状态变化 | 资源状态变化 |
| --- | --- | --- |
| 提交申请 | 新建 PENDING | 保持 AVAILABLE |
| 审核通过 | PENDING → PASS | AVAILABLE → OCCUPIED |
| 审核驳回 | PENDING → REJECT | 不变 |
| 取消待审核申请 | PENDING → CANCEL | 不变 |
| 取消已通过预约 | PASS → CANCEL | 恢复 AVAILABLE |

审核只允许处理 PENDING 记录；通过前再次确认关联资源存在且为 AVAILABLE。取消只允许申请人本人操作，REJECT 和 CANCEL 不可再次取消。

存在 PASS 预约的资源不能删除，也不能手动改为 AVAILABLE 或 DISABLED。只有待审核、驳回、取消记录的资源可以删除，历史预约保留；审核已删除资源的申请时提示资源不存在。

## 项目结构

```text
task09-v3.0
├─ README.md
└─ src
   ├─ SystemMain.java
   ├─ User.java
   ├─ CampusResource.java
   ├─ ReserveRecord.java
   ├─ ReserveStatus.java
   ├─ Result.java
   ├─ FileUtil.java
   └─ exception
      ├─ BusinessException.java
      ├─ EmptyParameterException.java
      ├─ PasswordIncorrectException.java
      ├─ UserAlreadyExistsException.java
      └─ UserNotFoundException.java
```

- SystemMain：程序入口、菜单、输入输出及当前版本业务流程。
- User、CampusResource、ReserveRecord：承载用户、资源和预约数据。
- ReserveStatus：PENDING、PASS、REJECT、CANCEL 四种预约状态。
- Result<T>：封装业务结果、提示和返回数据。
- FileUtil：用户、资源和预约文件读写。
- exception：业务异常；交互处捕获并展示友好提示。

## 文件持久化

三个文件均使用 UTF-8 编码，每行一条记录。启动时加载到内存，文件不存在时创建。所有路径均相对于程序工作目录，运行时应将工作目录设置为 task09-v3.0，避免读写其他版本的数据。

用户文件 user.txt：

```text
username|password|role|registerTime
```

资源文件 resource.txt：

```text
id|name|type|status|location|description
```

预约文件 reserve.txt：

```text
recordId|resourceId|applicantUsername|applyTime|reserveStartTime|reserveEndTime|status|remark
```

预约文件示例（资源和用户需在对应文件中存在）：

```text
r001|room001|alice|2026-11-01T08:00|2026-11-02T09:00|2026-11-02T10:00|PENDING|小组讨论
```

时间使用 LocalDateTime 的标准文本形式；枚举保存为 name()，读取时使用 valueOf()。split("\\|", -1) 保留末尾空备注。加载预约时检查字段数、必要字段、重复编号及时间和枚举格式，无法解析的记录提示后跳过。

新增用户采用追加写入；资源和预约更新采用全量覆盖。准备变更时创建候选集合，修改记录使用新对象替换，保存成功后更新内存，避免浅拷贝提前改变原对象。IO 使用 try-with-resources 关闭流。Result 不写入文件。

## 环境与运行

课程目标环境为 JDK 8，使用原生 Java，无第三方框架。进入 task09-v3.0 目录，确保 out 目录存在后执行：

```bash
javac -encoding UTF-8 -d out -sourcepath src src/SystemMain.java
java -cp out SystemMain
```

也可在 IntelliJ IDEA 中将项目 JDK 和语言级别设为 Java 8，运行 SystemMain.main，工作目录设为本版本目录。用户密码以明文保存在练习文件中，使用测试账号和测试密码。

## 验收清单

以下为待执行或记录结果的验收项，不代表全部运行通过。当前已完成源码检查和编译检查，完整控制台流程及重启验证仍需运行确认。

| 场景 | 预期结果 |
| --- | --- |
| 登录注册、资源增删改查、名称与 ID 查询 | 原版本功能正常 |
| 学生查看资源 | 只展示 AVAILABLE 资源 |
| 提交合法申请，备注为空 | 生成独立编号和 PENDING 记录，资源不占用 |
| 时间格式错误、起止相等或颠倒、结束时间已过 | 友好提示，不创建预约 |
| 备注含竖线 | 友好拒绝，程序继续运行 |
| 资源不存在、停用或占用 | 拒绝提交 |
| 时间段部分重叠、包含、首尾相接 | 冲突判定分别为冲突、冲突、不冲突；业务仍受资源状态限制 |
| 两名学生查看个人预约 | 各自只能看到自己的记录 |
| 管理员按资源、申请人、状态筛选 | 返回符合全部已填条件的记录 |
| 管理员通过待审核申请 | 记录 PASS，资源 OCCUPIED |
| 同一资源两条待审核申请，先通过一条再通过另一条 | 第二条因资源不可用被拒绝 |
| 管理员驳回待审核申请 | 记录 REJECT，资源状态不变 |
| 取消自己的待审核或已通过预约 | 记录 CANCEL；只有原 PASS 需要释放资源 |
| 取消他人预约、重复取消、取消已驳回预约 | 拒绝操作 |
| 学生审核、管理员提交或取消预约 | 业务方法拒绝越权调用 |
| 删除或修改存在 PASS 预约的资源状态 | 拒绝删除及改为可预约或停用 |
| 删除只有待审核记录的资源，再审核该记录 | 提示资源不存在 |
| 退出重启后查看预约与资源 | 已保存数据和状态保留 |

建议完整演示顺序：学生提交 → 退出重启 → 管理员查询并通过 → 学生取消 → 确认资源恢复可预约。另用独立申请演示驳回与非法操作。

## 设计要点与当前限制

本次通过独立预约实体表达用户与资源的关联，以用户名、资源 ID 连接数据；使用枚举限定预约状态，通过业务条件控制状态流转。Stream、Lambda 用于筛选和冲突检查，Optional 表达按编号查找可能没有结果，业务入口沿用 Result 和自定义异常。

预约和资源是可变对象。new ArrayList<>(原集合) 只复制容器，因此修改时构造新对象再替换候选集合元素，避免保存失败前原内存数据已被改变。

当前资源采用整体占用模型：通过预约后即设为 OCCUPIED，不论其他时间段是否空闲，都不能继续提交申请；没有按时间自动释放资源的机制。这是本版本的课程业务范围。

审核通过和取消已通过预约需要先后写入 reserve.txt 与 resource.txt，两次写入不具备跨文件事务保证。第二次保存失败可能造成文件状态不一致；普通覆盖写入也不能保证中途故障时文件完整。当前实现不能视为已具备事务或崩溃恢复能力。

本版本不提前引入 Controller、Service、DAO。任务10将在现有业务基础上进行三层架构重构并绘制 UML；驳回原因、审核时间等完善内容留到任务11。
