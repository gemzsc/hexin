import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;


public class SystemMain {

    private static boolean isUsernameValid(String username){
        return username != null && !username.trim().isEmpty();
    }

    private static boolean isPasswordValid(String password){
        return password !=null
                && !password.trim().isEmpty()
                && password.length() >= 6;
    }

    private static void register(Scanner scanner , Map<String,String> users){
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

        if(users.containsKey(username)){
            System.out.println("用户名已存在");
            return;
        }

        users.put(username,password);
        System.out.println("注册成功");
    }

    private static void login(Scanner scanner, Map<String,String> users){
        System.out.print("请输入用户名：");
        String username = scanner.nextLine().trim();

        System.out.print("请输入密码：");
        String password = scanner.nextLine();

        if(!users.containsKey(username)){
            System.out.println("用户不存在，请重新输入或注册");
        }else if(!users.get(username).equals(password)){
            System.out.println("密码错误");
        }else{
            System.out.println("学生登录成功");
        }
    }

    public static void main(String[] args) {
        //创建Scanner
        Scanner scanner = new Scanner(System.in);
        //创建键值表保存账号(键)、密码
        Map<String,String> users = new HashMap<>();

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