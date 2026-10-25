import exception.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;

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

        User user = findUserByUsername(users,username);

        if(user == null){
            throw new UserNotFoundException("用户不存在，请先注册");
        }

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
        List<CampusResource> availableResources = new ArrayList<>();

        for(CampusResource resource : resources){
            if("AVAILABLE".equals(resource.getStatus())){
                availableResources.add(resource);
            }
        }

        return Result.success("查询成功", availableResources);
    }

    private static void showAdminMenu(Scanner scanner,List<CampusResource> resources,User user) throws IOException{
        while (true) {
            System.out.println("\n管理员资源菜单");
            System.out.println("1. 新增资源");
            System.out.println("2. 查看全部资源");
            System.out.println("3. 修改资源");
            System.out.println("4. 删除资源");
            System.out.println("0. 返回主菜单");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
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
                case "2":
                    Result<List<CampusResource>> result = queryAllResources(resources);
                    if (result.isSuccess()) {
                        System.out.println(result.getMsg());
                        printResources(result.getData());
                    } else {
                        System.out.println(result.getMsg());
                    }
                    break;
                case "3":
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
                case "4":
                    Result<Void> deleteResult;
                    try {
                        deleteResult = deleteResource(scanner, resources, user);
                    } catch (BusinessException e) {
                        deleteResult = Result.fail(e.getMessage());
                    } catch (IOException e) {
                        deleteResult = Result.fail("资源数据保存失败，请稍后重试");
                    }
                    System.out.println(deleteResult.getMsg());
                    break;
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

    private static void showStudentMenu(Scanner scanner,List<CampusResource> resources) {
        while (true) {
            System.out.println("\n学生资源菜单");
            System.out.println("1. 查看可预约资源");
            System.out.println("0. 返回主菜单");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    Result<List<CampusResource>> result = queryAvailableResources(resources);
                    if (result.isSuccess()) {
                        System.out.println(result.getMsg());
                        printResources(result.getData());
                    } else {
                        System.out.println(result.getMsg());
                    }
                    break;
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

    private static CampusResource findResourceByIdOrNull(
            List<CampusResource> resources, String id) {
        for(CampusResource resource : resources){
            if(resource != null
                    && resource.getId().equals(id)){
                return resource;
            }
        }
        return null;
    }

    public static Result<CampusResource> findResourceById(
            List<CampusResource> resources, String id) {
        CampusResource resource = findResourceByIdOrNull(resources, id);
        if (resource == null) {
            return Result.fail("未找到该 ID 对应的资源");
        }
        return Result.success("查询成功", resource);
    }

    private static int findResourceIndexById(
            List<CampusResource> resources, String id) {
        for (int i = 0; i < resources.size(); i++) {
            CampusResource resource = resources.get(i);
            if (resource != null && id.equals(resource.getId())) {
                return i;
            }
        }
        return -1;
    }

    private static CampusResource requireResourceById(
            List<CampusResource> resources,String id){
        CampusResource resource = findResourceByIdOrNull(resources, id);

        if(resource == null){
            throw new BusinessException("资源不存在");
        }
        return resource;
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

        int targetIndex = -1;
        for(int i=0;i < updated.size();i++){
            if(id.equals(updated.get(i).getId())){
                targetIndex = i;
                break;
            }
        }

        if(targetIndex == -1){
            return Result.fail("资源不存在");
        }

        updated.set(targetIndex,changed);

        FileUtil.saveResources(updated);
        resources.clear();
        resources.addAll(updated);
        return Result.success("资源修改成功");
    }

    private static  Result<Void> deleteResource(
            Scanner scanner,List<CampusResource> resources,User currentUser) throws IOException{
        requireAdmin(currentUser);

        System.out.print("请输入要删除的资源 ID：");
        String id = scanner.nextLine().trim();

        CampusResource target = requireResourceById(resources, id);
        int targetIndex = findResourceIndexById(resources, target.getId());

        List<CampusResource> updated = new ArrayList<>(resources);
        if (targetIndex == -1) {
            return Result.fail("资源不存在");
        }
        updated.remove(targetIndex);

        FileUtil.saveResources(updated);

        resources.clear();
        resources.addAll(updated);
        return Result.success("资源删除成功");
    }

    public static void main(String[] args) {
        //创建Scanner
        Scanner scanner = new Scanner(System.in);

        List<User> users;
        List<CampusResource> resources;

        try {
            users = FileUtil.loadUsers();
            ensureAdminExists(users);
            resources = FileUtil.loadResources();
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
                                showAdminMenu(scanner, resources,user);
                            } catch (IOException e) {
                                System.out.println("资源数据保存失败，请稍后重试");
                            }
                        } else if ("student".equals(user.getRole())) {
                            System.out.println("学生登录成功");
                            showStudentMenu(scanner,resources);
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