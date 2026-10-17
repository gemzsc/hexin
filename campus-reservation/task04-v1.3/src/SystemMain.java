import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

public class SystemMain {

    private static boolean isUsernameValid(String username) {
        return username != null
                && !username.trim().isEmpty()
                && !username.contains("|");
    }

    private static boolean isPasswordValid(String password) {
        return password != null
                && !password.trim().isEmpty()
                && password.length() >= 6
                && !password.contains("|");
    }

    private static void register(Scanner scanner , List<User> users){
        System.out.print("请输入用户名：");
        String username = scanner.nextLine().trim();

        System.out.print("请输入密码：");
        String password = scanner.nextLine();

        if(!isUsernameValid(username)){
            System.out.println("用户名不能为空");
            return;
        }

        if (!isPasswordValid(password)){
            System.out.println("密码不能为空,且长度至少为6位");
            return;
        }

        if(findUserByUsername(users,username) != null){
            System.out.println("用户名已存在");
            return;
        }

        User newUser = new User(
                username, password, "student", LocalDateTime.now()
        );

        try {
            FileUtil.saveUser(newUser);
            users.add(newUser);
            System.out.println("注册成功");
        } catch (IOException e) {
            System.out.println("用户保存失败，请稍后重试");
        }
    }

    private static void login(Scanner scanner, List<User> users){
        System.out.print("请输入用户名：");
        String username = scanner.nextLine().trim();

        System.out.print("请输入密码：");
        String password = scanner.nextLine();

        User user = findUserByUsername(users,username);

        if(user == null){
            System.out.println("用户不存在，请重新输入或注册");
        }else if(!user.getPassword().equals(password)){
            System.out.println("密码错误");
        }else{
            System.out.println("学生登录成功");
            System.out.println(user);
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

    public static void main(String[] args) {
        //创建Scanner
        Scanner scanner = new Scanner(System.in);

        List<User> users;

        try {
            users = FileUtil.loadUsers();
        } catch (IOException e) {
            System.out.println("读取用户数据失败，程序无法启动");
            scanner.close();
            return;
        }

        while(true){
            System.out.println("  菜单  ");
            System.out.println(" 1.登录 ");
            System.out.println(" 2.注册 ");
            System.out.println(" 0.退出 ");

            String choice =scanner.nextLine();

            switch(choice){
                case "1":
                    login(scanner,users);
                    break;
                case "2":
                    register(scanner,users);
                    break;
                case "0":
                    System.out.println("再见！");
                    scanner.close();
                    return;
                default:
                    System.out.println("无效选项,请重新输入！");
            }
        }

    }
}