import exception.*;

import java.io.File;
import java.util.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.time.format.DateTimeParseException;

public class SystemMain {

    private static void validateUsername(String username){
        if(username ==null || username.trim().isEmpty()){
            throw new EmptyParameterException("用户名不能为空");
        }

        if(username.contains("|")){
            throw new BusinessException("用户名不能包含分隔符 |");
        }
    }

    private static void validatePassword(String password){
        if(password == null || password.trim().isEmpty()){
            throw new EmptyParameterException("密码不能为空");
        }

        if(password.length() < 6){
            throw new BusinessException("密码长度至少为6位");
        }

        if(password.contains("|")){
            throw new BusinessException("密码不能包含分隔符 |");
        }
    }

    private static boolean isValidResourceStatus(String status){
        return "AVAILABLE".equals(status)
                || "OCCUPIED".equals(status)
                || "DISABLED".equals(status);
    }

    private static Result<Void> register(Scanner scanner , List<User> users) throws IOException{

        System.out.print("请输入用户名：");
        String username = scanner.nextLine().trim();

        System.out.print("请输入密码：");
        String password = scanner.nextLine();

        validateUsername(username);

        validatePassword(password);

        if(findUserByUsername(users,username) != null){
            throw  new UserAlreadyExistsException("用户名已存在");
        }

        User newUser = new User(
                username, password, "student", LocalDateTime.now()
        );

        FileUtil.saveUser(newUser);
        users.add(newUser);
        return Result.success("注册成功");

    }

    private static Result<User> login(Scanner scanner, List<User> users){
        System.out.print("请输入用户名：");
        String username = scanner.nextLine().trim();

        System.out.print("请输入密码：");
        String password = scanner.nextLine();

        if (username == null || username.trim().isEmpty()) {
            throw new EmptyParameterException("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new EmptyParameterException("密码不能为空");
        }

        Result<User> userResult = queryUserByUsername(users, username);
        if (!userResult.isSuccess()) {
            throw new UserNotFoundException("用户不存在，请先注册");
        }
        User user = userResult.getData();

        if(!user.getPassword().equals(password)){
            throw new PasswordIncorrectException("密码错误");
        }

        return Result.success("登录成功",user);

    }

    private static void ensureAdminExists(List<User> users) throws IOException{
        boolean adminExists = users.stream()
                .anyMatch(user -> "admin".equals(user.getRole()));

        if(!adminExists){
            User admin = new User(
                    "admin","123456","admin",LocalDateTime.now()
            );
            FileUtil.saveUser(admin);
            users.add(admin);
        }
    }

    private static User findUserByUsername(List<User> users,String username){
        for(User user : users){
            if(user.getUsername().equals(username)){
                return user;
            }
        }
        return null;


    }

    private static Result<User> queryUserByUsername(
            List<User> users, String username) {
        User user = findUserByUsername(users, username);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.success("查询成功", user);
    }

    private static Result<List<CampusResource>> queryAllResources(
            List<CampusResource> resources) {
        return Result.success("查询成功", resources);
    }

    private static Result<List<CampusResource>> queryAvailableResources(
            List<CampusResource> resources) {
        List<CampusResource> availableResources = resources.stream().filter(resource ->"AVAILABLE".
                equals(resource.getStatus())).collect(Collectors.toList());

        return Result.success("查询成功", availableResources);
    }

    private  static Result<List<CampusResource>> searchResourceByName(
            List<CampusResource> resources,String keyword){
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.fail("搜索关键词不能为空");
        }

        String normalizeKeyword = keyword.trim().toLowerCase();

        List<CampusResource> matched = resources.stream()
                .filter(resource -> resource.getName() != null
                        && resource.getName().toLowerCase()
                        .contains(normalizeKeyword))
                .sorted(Comparator.comparing(CampusResource::getId))
                .collect(Collectors.toList());

        return Result.success("查询成功",matched);
    }

    private static void showAdminMenu(Scanner scanner,List<CampusResource> resources,List<ReserveRecord> records,User user) throws IOException{
        while (true) {
            System.out.println("\n管理员资源菜单");
            System.out.println("1. 新增资源");
            System.out.println("2. 查看全部资源");
            System.out.println("3. 修改资源");
            System.out.println("4. 删除资源");
            System.out.println("5. 按名称搜索资源");
            System.out.println("6. 按 ID 查询资源");
            System.out.println("7. 查询全部预约");
            System.out.println("8. 按条件查询预约");
            System.out.println("9. 审核通过预约");
            System.out.println("10. 驳回预约");
            System.out.println("0. 返回主菜单");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1": {
                    Result<CampusResource> addResult;
                    try {
                        addResult = addResource(scanner, resources, user);
                    } catch (BusinessException e) {
                        addResult = Result.fail(e.getMessage());
                    } catch (IOException e) {
                        addResult = Result.fail("资源数据保存失败，请稍后重试");
                    }

                    System.out.println(addResult.getMsg());
                    if (addResult.isSuccess()) {
                        System.out.println("资源 ID：" + addResult.getData().getId());
                    }
                    break;
                }
                case "2": {
                    Result<List<CampusResource>> result = queryAllResources(resources);
                    if (result.isSuccess()) {
                        System.out.println(result.getMsg());
                        printResources(result.getData());
                    } else {
                        System.out.println(result.getMsg());
                    }
                    break;
                }
                case "3": {
                    Result<Void> updateResult;
                    try {
                        updateResult = updateResource(scanner, resources, user);
                    } catch (BusinessException e) {
                        updateResult = Result.fail(e.getMessage());
                    } catch (IOException e) {
                        updateResult = Result.fail("资源数据保存失败，请稍后重试");
                    }
                    System.out.println(updateResult.getMsg());
                    break;
                }
                case "4": {
                    Result<Void> deleteResult;
                    try {
                        deleteResult = deleteResource(scanner, resources, records, user);
                    } catch (BusinessException e) {
                        deleteResult = Result.fail(e.getMessage());
                    } catch (IOException e) {
                        deleteResult = Result.fail("资源数据保存失败，请稍后重试");
                    }
                    System.out.println(deleteResult.getMsg());
                    break;
                }
                case "5": {
                    System.out.print("请输入资源名称关键词：");
                    String keyword = scanner.nextLine();

                    Result<List<CampusResource>> searchResult =
                            searchResourceByName(resources, keyword);

                    System.out.println(searchResult.getMsg());
                    if (searchResult.isSuccess()) {
                        printResources(searchResult.getData());
                    }
                    break;
                }
                case "6": {
                    System.out.print("请输入资源 ID：");
                    String id = scanner.nextLine().trim();

                    Result<CampusResource> idResult = queryResourceById(resources, id);
                    System.out.println(idResult.getMsg());

                    if (idResult.isSuccess()) {
                        System.out.println(idResult.getData());
                    }
                    break;
                }
                case "7": {
                    Result<List<ReserveRecord>> reserveResult =
                            queryAllReserves(records, user);

                    if (reserveResult.isSuccess()) {
                        if (reserveResult.getData().isEmpty()) {
                            System.out.println("暂无预约记录");
                        } else {
                            System.out.println(reserveResult.getMsg());
                            printReserveRecords(reserveResult.getData());
                        }
                    } else {
                        System.out.println(reserveResult.getMsg());
                    }
                    break;
                }
                case "8":{
                    System.out.print("资源 ID（回车表示不限）：");
                    String resourceId = scanner.nextLine().trim();

                    System.out.print("申请人账号（回车表示不限）：");
                    String applicant = scanner.nextLine().trim();

                    System.out.print("预约状态 PENDING/PASS/REJECT/CANCEL（回车表示不限）：");
                    String statusInput = scanner.nextLine().trim();

                    ReserveStatus parsedStatus = null;
                    if (!statusInput.isEmpty()) {
                        try {
                            parsedStatus = ReserveStatus.valueOf(statusInput.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            System.out.println("状态只能是 PENDING、PASS、REJECT 或 CANCEL");
                            break;
                        }
                    }

                    final ReserveStatus statusFilter = parsedStatus;

                    Result<List<ReserveRecord>> reserveResult =
                            queryReservesByCondition(
                                    records, user, resourceId, applicant, statusFilter);

                    if (!reserveResult.isSuccess()) {
                        System.out.println(reserveResult.getMsg());
                    } else if (reserveResult.getData().isEmpty()) {
                        System.out.println("没有符合条件的预约");
                    } else {
                        System.out.println(reserveResult.getMsg());
                        printReserveRecords(reserveResult.getData());
                    }
                    break;
                }
                case "9": {
                    System.out.print("请输入要通过的预约 ID：");
                    String recordId = scanner.nextLine().trim();

                    try {
                        Result<Void> result =
                                approveReserve(records, resources, user, recordId);
                        System.out.println(result.getMsg());
                    } catch (BusinessException e) {
                        System.out.println(e.getMessage());
                    } catch (IOException e) {
                        System.out.println("预约或资源数据保存失败");
                    }
                    break;
                }
                case "10": {
                    System.out.print("请输入要驳回的预约 ID：");
                    String recordId = scanner.nextLine().trim();

                    try {
                        Result<Void> result = rejectReserve(records, user, recordId);
                        System.out.println(result.getMsg());
                    } catch (BusinessException e) {
                        System.out.println(e.getMessage());
                    } catch (IOException e) {
                        System.out.println("预约数据保存失败");
                    }
                    break;
                }
                case "0":
                    return;
                default:
                    System.out.println("无效选项，请重新输入");
            }
        }
    }

    private static void requireAdmin(User currentUser){
        if(currentUser == null
                ||!"admin".equals(currentUser.getRole())){
            throw new BusinessException("无权限执行资源管理操作");
        }
    }

    private static void showStudentMenu(Scanner scanner,List<CampusResource> resources,List<ReserveRecord> records,User user) {
        while (true) {
            System.out.println("\n学生资源菜单");
            System.out.println("1. 查看可预约资源");
            System.out.println("2. 提交预约申请");
            System.out.println("3. 查询个人预约");
            System.out.println("4. 取消预约");
            System.out.println("0. 返回主菜单");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1": {
                    Result<List<CampusResource>> result = queryAvailableResources(resources);
                    if (result.isSuccess()) {
                        System.out.println(result.getMsg());
                        printResources(result.getData());
                    } else {
                        System.out.println(result.getMsg());
                    }
                    break;
                }
                case "2": {
                    System.out.print("请输入资源 ID：");
                    String resourceId = scanner.nextLine().trim();

                    System.out.print("请输入预约开始时间（yyyy-MM-ddTHH:mm）：");
                    String startInput = scanner.nextLine().trim();

                    System.out.print("请输入预约结束时间（yyyy-MM-ddTHH:mm）：");
                    String endInput = scanner.nextLine().trim();

                    System.out.print("请输入预约备注（可留空）：");
                    String remark = scanner.nextLine().trim();

                    try {
                        LocalDateTime start = LocalDateTime.parse(startInput);
                        LocalDateTime end = LocalDateTime.parse(endInput);

                        Result<ReserveRecord> reserveResult = submitReserve(
                                records,
                                resources,
                                user,
                                resourceId,
                                start,
                                end,
                                remark
                        );

                        System.out.println(reserveResult.getMsg());

                        if (reserveResult.isSuccess()) {
                            System.out.println("预约记录：" + reserveResult.getData());
                        }
                    } catch (DateTimeParseException e) {
                        System.out.println("时间格式错误，请按 yyyy-MM-ddTHH:mm 输入，例如 2026-11-02T09:00");
                    } catch (BusinessException e) {
                        System.out.println(e.getMessage());
                    } catch (IOException e) {
                        System.out.println("预约数据保存失败，请稍后重试");
                    }

                    break;
                }
                case "3": {
                    Result<List<ReserveRecord>> reserveResult = queryMyReserve(records, user);
                    if(reserveResult.isSuccess()){
                        if(reserveResult.getData().isEmpty()){
                            System.out.println("暂无预约");
                        }else {
                            System.out.println(reserveResult.getMsg());
                            printReserveRecords(reserveResult.getData());
                        }
                    }else {
                        System.out.println(reserveResult.getMsg());
                    }
                    break;
                }
                case "4": {
                    System.out.print("请输入要取消的预约 ID：");
                    String recordId = scanner.nextLine().trim();

                    try {
                        Result<Void> result =
                                cancelReserve(records, resources, user, recordId);
                        System.out.println(result.getMsg());
                    } catch (BusinessException e) {
                        System.out.println(e.getMessage());
                    } catch (IOException e) {
                        System.out.println("预约或资源数据保存失败");
                    }
                    break;
                }
                case "0":
                    return;
                default:
                    System.out.println("无效选项，请重新输入");
            }
        }
    }

    private static void printResources(List<CampusResource> resources){
        if(resources.isEmpty()){
            System.out.println("暂无资源");
            return;
        }

        for(CampusResource resource : resources){
            System.out.println(resource);
        }
    }

    private static Result<CampusResource> addResource(
            Scanner scanner,List<CampusResource> resources,User currentUser) throws IOException{
                requireAdmin(currentUser);

                System.out.print("请输入资源名称：");
                String name = scanner.nextLine().trim();

                System.out.print("请输入资源类型：");
                String type = scanner.nextLine().trim();

                System.out.print("请输入资源位置：");
                String location = scanner.nextLine().trim();

                System.out.print("请输入资源描述(可留空)：");
                String description = scanner.nextLine().trim();

                if(name.isEmpty()){
                    throw new BusinessException("资源名称不能为空");
                }
                if(name.contains("|")){
                    throw new BusinessException("资源名称不能包含分隔符 |");
                }

                if(type.isEmpty()){
                    throw new BusinessException("资源类型不能为空");
                }
                if(type.contains("|")){
                    throw new BusinessException("资源类型不能包含分隔符 |");
                }

                if(location.isEmpty()){
                    throw new BusinessException("资源位置不能为空");
                }
                if(location.contains("|")){
                    throw new BusinessException("资源位置不能包含分隔符 |");
                }

                if(description.contains("|")){
                    throw new BusinessException("资源描述不能包含分隔符 |");
                }

                String id = java.util.UUID.randomUUID().toString();
                String status = "AVAILABLE";

                CampusResource resource = new CampusResource(
                        id,name,type,status,location,description);

                List<CampusResource> updated = new ArrayList<>(resources);
                updated.add(resource);

                FileUtil.saveResources(updated);
                resources.clear();
                resources.addAll(updated);

                return Result.success("资源新增成功",resource);
    }


    private static Optional<CampusResource> findResourceOptional(
            List<CampusResource> resources, String id) {
        if (id == null) {
            return Optional.empty();
        }

        return resources.stream()
                .filter(resource -> resource != null
                        && id.equals(resource.getId()))
                .findFirst();
    }

    public static Result<CampusResource> queryResourceById(
            List<CampusResource> resources, String id) {
        Optional<CampusResource> found = findResourceOptional(resources, id);

        if (found.isPresent()) {
            return Result.success("查询成功", found.get());
        }
        return Result.fail("未找到该 ID 对应的资源");
    }

    private static int findResourceIndexById(
            List<CampusResource> resources, String id) {

        if (resources == null || id == null) {
            return -1;
        }

        return IntStream.range(0, resources.size())
                .filter(index -> resources.get(index) != null
                        && id.equals(resources.get(index).getId()))
                .findFirst()
                .orElse(-1);
    }

    private static CampusResource requireResourceById(
            List<CampusResource> resources,String id){
        return findResourceOptional(resources,id)
                .orElseThrow(()-> new BusinessException("资源不存在"));
    }

    private static Result<Void> updateResource(
            Scanner scanner,List<CampusResource> resources,User currentUser) throws IOException{
        requireAdmin(currentUser);

        System.out.print("请输入要修改的资源 ID：");
        String id = scanner.nextLine().trim();

        CampusResource oldResource = requireResourceById(resources, id);

        System.out.print("新名称（回车保留原值）：");
        String input = scanner.nextLine().trim();
        String newName = input.isEmpty() ? oldResource.getName() : input;

        System.out.print("新类型（回车保留原值）：");
        input = scanner.nextLine().trim();
        String newType = input.isEmpty() ? oldResource.getType() : input;

        System.out.print("新位置（回车保留原值）：");
        input = scanner.nextLine().trim();
        String newLocation = input.isEmpty()
                ? oldResource.getLocation() : input;

        System.out.print("新状态（回车保留原值）：");
        input = scanner.nextLine().trim();
        String newStatus = input.isEmpty()
                ? oldResource.getStatus() : input;

        System.out.print("新描述（回车保留原值）：");
        input = scanner.nextLine().trim();
        String newDescription = input.isEmpty()
                ? oldResource.getDescription() : input;

        if(newName.trim().isEmpty()
                || newType.trim().isEmpty()
                || newLocation.trim().isEmpty()){
            throw new BusinessException("名称、类型和位置不能为空");
        }

        if(!isValidResourceStatus(newStatus)){
            throw new BusinessException(
                    "资源状态只能是 AVAILABLE、OCCUPIED 或 DISABLED");
        }

        String[] fields = {
                newName, newType, newStatus,
                newLocation, newDescription
        };

        for (String field : fields) {
            if (field.contains("|")) {
                throw new BusinessException("资源字段不能包含分隔符 |");
            }
        }

        CampusResource changed = new CampusResource(
                oldResource.getId(),newName,newType,newStatus,
                newLocation,newDescription
        );

        List<CampusResource> updated = new ArrayList<>(resources);
        int targetIndex = findResourceIndexById(updated, id);

        if (targetIndex == -1) {
            return Result.fail("资源不存在");
        }

        updated.set(targetIndex, changed);

        FileUtil.saveResources(updated);
        resources.clear();
        resources.addAll(updated);
        return Result.success("资源修改成功");
    }

    private static  Result<Void> deleteResource(
            Scanner scanner,List<CampusResource> resources,List<ReserveRecord> records,User currentUser) throws IOException{
        requireAdmin(currentUser);

        System.out.print("请输入要删除的资源 ID：");
        String id = scanner.nextLine().trim();

        CampusResource target = requireResourceById(resources, id);

        boolean hasPassedReserve = records.stream()
                .filter(r -> r != null)
                .anyMatch(r -> id.equals(r.getResourceId())
                        && r.getStatus() == ReserveStatus.PASS);

        if (hasPassedReserve) {
            throw new BusinessException("该资源存在已通过预约，不能删除");
        }

        int targetIndex = findResourceIndexById(resources, target.getId());

        if (targetIndex == -1) {
            return Result.fail("资源不存在");
        }

        List<CampusResource> updated = new ArrayList<>(resources);
        updated.remove(targetIndex);

        FileUtil.saveResources(updated);

        resources.clear();
        resources.addAll(updated);
        return Result.success("资源删除成功");
    }

    private static Result<List<ReserveRecord>> queryMyReserve(List<ReserveRecord> records,User currentUser){
        requireStudent(currentUser);

        List<ReserveRecord> mine = records.stream()
                .filter(r -> currentUser.getUsername()
                        .equals(r.getApplicantUsername()))
                .collect(Collectors.toList());

        return Result.success("查询成功",mine);

    }

    private static Result<List<ReserveRecord>> queryAllReserves(
            List<ReserveRecord> records, User currentUser) {
        requireAdmin(currentUser);
        return Result.success("查询成功", records);
    }

    private static Result<List<ReserveRecord>> queryReservesByCondition(
            List<ReserveRecord> records,
            User currentUser,
            String resourceId,
            String applicantUsername,
            ReserveStatus statusFilter) {

        requireAdmin(currentUser);

        List<ReserveRecord> matched = records.stream()
                .filter(r -> r != null)
                .filter(r -> resourceId == null || resourceId.trim().isEmpty()
                        || resourceId.trim().equals(r.getResourceId()))
                .filter(r -> applicantUsername == null
                        || applicantUsername.trim().isEmpty()
                        || applicantUsername.trim()
                        .equals(r.getApplicantUsername()))
                .filter(r -> statusFilter == null
                        || statusFilter == r.getStatus())
                .collect(Collectors.toList());

        return Result.success("查询成功", matched);
    }

    private static Result<ReserveRecord> submitReserve(
            List<ReserveRecord> records,
            List<CampusResource> resources,
            User currentUser,
            String resourceId,
            LocalDateTime start,
            LocalDateTime end,
            String remark) throws IOException {

        requireStudent(currentUser);

        if (records == null || resourceId == null
                || resourceId.trim().isEmpty()
                || start == null || end == null) {
            throw new EmptyParameterException("资源 ID 和预约时间不能为空");
        }

        if (!start.isBefore(end)) {
            throw new BusinessException("预约开始时间必须早于结束时间");
        }

        if (end.isBefore(LocalDateTime.now())) {
            throw new BusinessException("预约结束时间不能早于当前时间");
        }

        // 复用已有的资源查找方法；资源不存在时由它抛出业务异常
        CampusResource resource = requireResourceById(resources, resourceId);

        if (!"AVAILABLE".equals(resource.getStatus())) {
            throw new BusinessException("该资源当前不可预约");
        }

        boolean conflict = records.stream()
                .filter(record -> record != null)
                .filter(record -> resourceId.equals(record.getResourceId()))
                .filter(record -> record.getStatus() == ReserveStatus.PASS)
                .anyMatch(record ->
                        start.isBefore(record.getReserveEndTime())
                                && end.isAfter(record.getReserveStartTime()));

        if (conflict) {
            throw new BusinessException("预约时间与已有预约冲突");
        }

        String recordId = UUID.randomUUID().toString();

        ReserveRecord newRecord = new ReserveRecord(
                recordId,
                resourceId,
                currentUser.getUsername(),
                LocalDateTime.now(),
                start,
                end,
                ReserveStatus.PENDING,
                remark == null ? "" : remark
        );

        // 先保存候选集合；保存成功后才更新内存集合
        List<ReserveRecord> updated = new ArrayList<>(records);
        updated.add(newRecord);
        FileUtil.saveReserveRecords(updated);

        records.clear();
        records.addAll(updated);

        return Result.success("预约申请已提交，等待管理员审核", newRecord);
    }

    private static Result<Void> approveReserve(
            List<ReserveRecord> records,
            List<CampusResource> resources,
            User currentUser,
            String recordId) throws IOException {

        requireAdmin(currentUser);

        ReserveRecord oldRecord = findReserveOptional(records, recordId)
                .orElseThrow(() -> new BusinessException("预约记录不存在"));

        if (oldRecord.getStatus() != ReserveStatus.PENDING) {
            throw new BusinessException("只有待审核预约才能通过");
        }

        CampusResource oldResource =
                requireResourceById(resources, oldRecord.getResourceId());

        if (!"AVAILABLE".equals(oldResource.getStatus())) {
            throw new BusinessException("关联资源当前不可用");
        }

        // 复制集合后，用新对象替换预约记录，避免浅拷贝提前改动原记录
        List<ReserveRecord> updatedRecords = new ArrayList<>(records);
        int recordIndex = updatedRecords.indexOf(oldRecord);

        ReserveRecord passedRecord = new ReserveRecord(
                oldRecord.getRecordId(),
                oldRecord.getResourceId(),
                oldRecord.getApplicantUsername(),
                oldRecord.getApplyTime(),
                oldRecord.getReserveStartTime(),
                oldRecord.getReserveEndTime(),
                ReserveStatus.PASS,
                oldRecord.getRemark()
        );
        updatedRecords.set(recordIndex, passedRecord);

        List<CampusResource> updatedResources = new ArrayList<>(resources);
        int resourceIndex = updatedResources.indexOf(oldResource);

        CampusResource occupiedResource = new CampusResource(
                oldResource.getId(),
                oldResource.getName(),
                oldResource.getType(),
                "OCCUPIED",
                oldResource.getLocation(),
                oldResource.getDescription()
        );
        updatedResources.set(resourceIndex, occupiedResource);

        FileUtil.saveReserveRecords(updatedRecords);
        FileUtil.saveResources(updatedResources);

        // 两次保存都成功后，才替换内存列表
        records.clear();
        records.addAll(updatedRecords);
        resources.clear();
        resources.addAll(updatedResources);

        return Result.success("预约审核通过，资源已设为占用");
    }

    private static Result<Void> rejectReserve(
            List<ReserveRecord> records,
            User currentUser,
            String recordId) throws IOException {

        requireAdmin(currentUser);

        ReserveRecord oldRecord = findReserveOptional(records, recordId)
                .orElseThrow(() -> new BusinessException("预约记录不存在"));

        if (oldRecord.getStatus() != ReserveStatus.PENDING) {
            throw new BusinessException("只有待审核预约才能驳回");
        }

        List<ReserveRecord> updatedRecords = new ArrayList<>(records);
        int index = updatedRecords.indexOf(oldRecord);

        ReserveRecord rejectedRecord = new ReserveRecord(
                oldRecord.getRecordId(),
                oldRecord.getResourceId(),
                oldRecord.getApplicantUsername(),
                oldRecord.getApplyTime(),
                oldRecord.getReserveStartTime(),
                oldRecord.getReserveEndTime(),
                ReserveStatus.REJECT,
                oldRecord.getRemark()
        );
        updatedRecords.set(index, rejectedRecord);

        FileUtil.saveReserveRecords(updatedRecords);

        records.clear();
        records.addAll(updatedRecords);

        return Result.success("预约已驳回");
    }

    private static Result<Void> cancelReserve(
            List<ReserveRecord> records,
            List<CampusResource> resources,
            User currentUser,
            String recordId) throws IOException {

        requireStudent(currentUser);

        ReserveRecord oldRecord = findReserveOptional(records, recordId)
                .orElseThrow(() -> new BusinessException("预约记录不存在"));

        if (!currentUser.getUsername().equals(oldRecord.getApplicantUsername())) {
            throw new BusinessException("只能取消自己的预约");
        }

        ReserveStatus oldStatus = oldRecord.getStatus();
        if (oldStatus != ReserveStatus.PENDING
                && oldStatus != ReserveStatus.PASS) {
            throw new BusinessException("当前状态不允许取消预约");
        }

        ReserveRecord cancelledRecord = new ReserveRecord(
                oldRecord.getRecordId(),
                oldRecord.getResourceId(),
                oldRecord.getApplicantUsername(),
                oldRecord.getApplyTime(),
                oldRecord.getReserveStartTime(),
                oldRecord.getReserveEndTime(),
                ReserveStatus.CANCEL,
                oldRecord.getRemark()
        );

        List<ReserveRecord> updatedRecords = new ArrayList<>(records);
        updatedRecords.set(updatedRecords.indexOf(oldRecord), cancelledRecord);

        // PENDING 取消只改预约记录；PASS 取消还要释放资源
        CampusResource releasedResource = null;
        List<CampusResource> updatedResources = null;

        if (oldStatus == ReserveStatus.PASS) {
            CampusResource oldResource =
                    requireResourceById(resources, oldRecord.getResourceId());

            releasedResource = new CampusResource(
                    oldResource.getId(),
                    oldResource.getName(),
                    oldResource.getType(),
                    "AVAILABLE",
                    oldResource.getLocation(),
                    oldResource.getDescription()
            );

            updatedResources = new ArrayList<>(resources);
            updatedResources.set(updatedResources.indexOf(oldResource), releasedResource);
        }

        FileUtil.saveReserveRecords(updatedRecords);
        if (oldStatus == ReserveStatus.PASS) {
            FileUtil.saveResources(updatedResources);
        }

        records.clear();
        records.addAll(updatedRecords);

        if (oldStatus == ReserveStatus.PASS) {
            resources.clear();
            resources.addAll(updatedResources);
        }

        return Result.success("预约已取消");
    }

    private static Optional<ReserveRecord> findReserveOptional(
            List<ReserveRecord> records, String id){
        if (id == null) {
            return Optional.empty();
        }

        return records.stream()
                .filter(r ->r!= null && id.equals(r.getRecordId()))
                .findFirst();
    }

    private static void requireStudent(User currentUser){
        if(currentUser == null
                ||!"student".equals(currentUser.getRole())){
            throw new BusinessException("无权限执行学生预约操作");
        }
    }

    private static void printReserveRecords(List<ReserveRecord> records) {
        for (ReserveRecord record : records) {
            System.out.println(record);
        }
    }

    public static void main(String[] args) {
        //创建Scanner
        Scanner scanner = new Scanner(System.in);

        List<User> users;
        List<CampusResource> resources;
        List<ReserveRecord> records;

        try {
            users = FileUtil.loadUsers();
            ensureAdminExists(users);
            resources = FileUtil.loadResources();
            records = FileUtil.loadReserveRecords();
        } catch (IOException e) {
            System.out.println("读取用户或资源数据失败，程序无法启动");
            scanner.close();
            return;
        }

        while(true){
            System.out.println("  菜单  ");
            System.out.println(" 1.登录 ");
            System.out.println(" 2.注册 ");
            System.out.println(" 0.退出 ");

            String choice =scanner.nextLine();

            try{
                switch(choice){
                    case "1":
                        Result<User> loginResult;
                        try {
                            loginResult = login(scanner, users);
                        } catch (BusinessException e) {
                            loginResult = Result.fail(e.getMessage());
                        }
                        if(!loginResult.isSuccess()){
                            System.out.println(loginResult.getMsg());
                            break;
                        }

                        User user = loginResult.getData();
                        if ("admin".equals(user.getRole())) {
                            System.out.println("管理员登录成功");
                            try {
                                showAdminMenu(scanner, resources,records,user);
                            } catch (IOException e) {
                                System.out.println("资源数据保存失败，请稍后重试");
                            }
                        } else if ("student".equals(user.getRole())) {
                            System.out.println("学生登录成功");
                            showStudentMenu(scanner,resources,records,user);
                        } else {
                            System.out.println("未知角色，无法进入系统");
                        }
                        break;
                    case "2":
                        Result<Void> registerResult;
                        try{
                            registerResult = register(scanner,users);
                        }catch(BusinessException e){
                            registerResult = Result.fail(e.getMessage());
                        }catch (IOException e){
                            registerResult = Result.fail("用户数据保存失败，请稍后重试");
                        }
                        System.out.println(registerResult.getMsg());
                        break;
                    case "0":
                        System.out.println("再见！");
                        scanner.close();
                        return;
                    default:
                        System.out.println("无效选项,请重新输入！");
                }
            }catch(BusinessException e){
                System.out.println(e.getMessage());
            }
        }
    }
}