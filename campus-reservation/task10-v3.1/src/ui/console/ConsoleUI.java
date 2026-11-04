package ui.console;

import entity.Result;
import entity.User;
import exception.BusinessException;
import service.UserService;
import java.io.IOException;
import java.util.Scanner;

public class ConsoleUI {
    private final Scanner scanner;
    private final UserService userService;

    public ConsoleUI(Scanner scanner, UserService userService) {
        this.scanner = scanner;
        this.userService = userService;
    }

    public void handleRegister() {
        System.out.print("请输入用户名：");
        String username = scanner.nextLine().trim();
        System.out.print("请输入密码：");
        String password = scanner.nextLine();
        Result<Void> result;
        try {
            result = userService.register(username, password);
        } catch (BusinessException e) {
            result = Result.fail(e.getMessage());
        } catch (IOException e) {
            result = Result.fail("用户数据保存失败，请稍后重试");
        }
        System.out.println(result.getMsg());
    }

    public Result<User> handleLogin() {
        System.out.print("请输入用户名：");
        String username = scanner.nextLine().trim();

        System.out.print("请输入密码：");
        String password = scanner.nextLine();

        try {
            return userService.login(username, password);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    public void start() {
        while (true) {
            System.out.println("1. 登录");
            System.out.println("2. 注册");
            System.out.println("0. 退出");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    handleLogin();      // 登录交互迁入 ConsoleUI 后再接入
                    break;
                case "2":
                    handleRegister();   // 你现在已有的方法
                    break;
                case "0":
                    System.out.println("再见！");
                    return;             // 结束 start()，回到 main()
                default:
                    System.out.println("无效选项，请重新输入");
            }
        }
    }
}
