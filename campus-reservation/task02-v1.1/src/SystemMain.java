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

    }

    public static void main(String[] args) {
        //创建Scanner
        Scanner scanner = new Scanner(System.in);
        //创建键值表保存账号(键)、密码
        Map<String,String> users = new HashMap<>()

        while(true){
            System.out.print("  菜单  ");
            System.out.print(" 1.登录 ");
            System.out.print(" 2.注册 ");
            System.out.print(" 0.退出 ");

            String choice =scanner.nextLine();

            switch(choice){
                case "1":

                    break;
                case "2":


                    break;
                case "0":
                    System.out.print("再见！")
                    scanner.close();
                    return;
                default:
                    System.out.print("无效选项,请重新输入！");
            }
        }
        //读取账号密码
        System.out.print("请输入账号: ");
        String username = scanner.nextLine();
        System.out.print("请输入密码: ");
        String password = scanner.nextLine();
        //判断固定账号并输出登录结果
        if("admin".equals(username) && "123456".equals(password)){
            System.out.print("登录成功，欢迎管理员！");
        } else if ("student".equals(username) && "123456".equals(password)) {
            System.out.print("登录成功，欢迎同学！");
        }
        else {
            System.out.print("登录失败，账号或密码错误。");
        }
        
        scanner.close();
    }
}