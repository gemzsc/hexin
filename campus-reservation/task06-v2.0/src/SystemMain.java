import exception.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;
import java.util.concurrent.Callable;

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

    private static void register(Scanner scanner , List<User> users) throws IOException{

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
        System.out.println("注册成功");

    }

    private static User login(Scanner scanner, List<User> users){
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

        return user;

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

    private static void showAdminMenu(Scanner scanner,List<CampusResource> resources) throws IOException{
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
                    addResource(scanner, resources);
                    break;
                case "2":
                    printResources(resources);
                    break;
                case "3":
                    updateResource(scanner,resources);
                    break;
                case "4":
                    System.out.println("删除资源功能待实现");
                    break;
                case "0":
                    return;
                default:
                    System.out.println("无效选项，请重新输入");
            }
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
                    List<CampusResource> avaliableResources = new ArrayList<>();

                    for(CampusResource resource : resources){
                        if("AVAILABLE".equals(resource.getStatus())){
                            avaliableResources.add(resource);
                        }
                    }

                    printResources(avaliableResources);
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

    public static void addResource(
            Scanner scanner,List<CampusResource> resources) throws IOException{
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

                System.out.println("资源新增成功，资源 ID：" + id);
    }

    public static CampusResource findResourceById(
            List<CampusResource> resources,String id){
        for(CampusResource resource : resources){
            if(resource != null
            && resource.getId().equals(id)){
                return resource;
            }
        }
        return null;
    }

    private static void updateResource(
            Scanner scanner,List<CampusResource> resources) throws IOException{
        System.out.print("请输入要修改的资源 ID：");
        String id = scanner.nextLine().trim();

        CampusResource oldResource = findResourceById(resources, id);
        if (oldResource == null) {
            System.out.println("资源不存在");
            return;
        }

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

        if(isValidResourceStatus(newStatus)){
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
            System.out.println("资源不存在");
            return;
        }

        updated.set(targetIndex,changed);

        FileUtil.saveResources(updated);
        resources.clear();
        resources.addAll(updated);
        System.out.println("资源修改成功");
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
                        User user = login(scanner, users);

                        if ("admin".equals(user.getRole())) {
                            System.out.println("管理员登录成功");
                            showAdminMenu(scanner,resources);
                        } else if ("student".equals(user.getRole())) {
                            System.out.println("学生登录成功");
                            showStudentMenu(scanner,resources);
                        } else {
                            System.out.println("未知角色，无法进入系统");
                        }
                        break;
                    case "2":
                        register(scanner , users);
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
            }catch(IOException e){
                System.out.println("用户数据保存失败，请稍后重试");
            }
        }
    }
}