import java.util.Scanner;



public class SystemMain {
    public static void main(String[] args) {
        //创建Scanner
        Scanner scanner = new Scanner(System.in);
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