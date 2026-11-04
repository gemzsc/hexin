package ui.console;

import entity.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import service.ReserveRecordService;
import entity.Result;
import entity.CampusResource;
import service.ResourceService;
import java.util.List;
import entity.User;
import exception.BusinessException;
import exception.DataAccessException;
import service.UserService;
import java.util.Scanner;

public class ConsoleUI {
    public static void displayWarning(String message) {
        System.out.println(message);
    }

    private final Scanner scanner;
    private final UserService userService;
    private final ResourceService resourceService;
    private final ReserveRecordService reserveService;

    public ConsoleUI(Scanner scanner, UserService userService, ResourceService resourceService, ReserveRecordService reserveService) {
        this.scanner = scanner;
        this.userService = userService;
        this.resourceService = resourceService;
        this.reserveService = reserveService;
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
        } catch (DataAccessException e) {
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

    public void showAllResources() {
        Result<List<CampusResource>> result = resourceService.queryAllResources();
        System.out.println(result.getMsg());
        if (result.isSuccess()) {
            if (result.getData().isEmpty()) {
                System.out.println("暂无资源");
            } else {
                result.getData().forEach(System.out::println);
            }
        }
    }



private void showAdminMenu(User user) throws IOException{
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
                        addResult = inputAddResource(user);
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
                    showAllResources();
                    break;
                }
                case "3": {
                    Result<Void> updateResult;
                    try {
                        updateResult = inputUpdateResource(user);
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
                        deleteResult = inputDeleteResource(user);
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
                            resourceService.searchResourceByName(keyword);

                    System.out.println(searchResult.getMsg());
                    if (searchResult.isSuccess()) {
                        printResources(searchResult.getData());
                    }
                    break;
                }
                case "6": {
                    System.out.print("请输入资源 ID：");
                    String id = scanner.nextLine().trim();

                    Result<CampusResource> idResult = resourceService.queryResourceById(id);
                    System.out.println(idResult.getMsg());

                    if (idResult.isSuccess()) {
                        System.out.println(idResult.getData());
                    }
                    break;
                }
                case "7": {
                    Result<List<ReserveRecord>> reserveResult =
                            reserveService.queryAllReserves(user);

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
                            reserveService.queryReservesByCondition(user, resourceId, applicant, statusFilter);

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
                                reserveService.approveReserve(user, recordId);
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
                        Result<Void> result = reserveService.rejectReserve(user, recordId);
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
private void showStudentMenu(User user) {
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
                    Result<List<CampusResource>> result = resourceService.queryAvailableResources();
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

                        Result<ReserveRecord> reserveResult = reserveService.submitReserve(user,
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
                    Result<List<ReserveRecord>> reserveResult = reserveService.queryMyReserve(user);
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
                                reserveService.cancelReserve(user, recordId);
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
private static void printReserveRecords(List<ReserveRecord> records) {
        for (ReserveRecord record : records) {
            System.out.println(record);
        }
    }
 private String input(String prompt) { System.out.print(prompt); return scanner.nextLine().trim(); }
 private Result<CampusResource> inputAddResource(User user) throws IOException {
  String name=input("请输入资源名称："); String type=input("请输入资源类型：");
  String location=input("请输入资源位置："); String description=input("请输入资源描述(可留空)：");
  return resourceService.addResource(user,name,type,location,description);
 }
 private Result<Void> inputUpdateResource(User user) throws IOException {
  String id=input("请输入要修改的资源 ID：");
  String name=input("新名称（回车保留原值）："); String type=input("新类型（回车保留原值）：");
  String location=input("新位置（回车保留原值）："); String status=input("新状态（回车保留原值）：");
  String description=input("新描述（回车保留原值）：");
  return resourceService.updateResource(user,id,name,type,location,status,description);
 }
 private Result<Void> inputDeleteResource(User user) throws IOException {
  return resourceService.deleteResource(user,input("请输入要删除的资源 ID："));
 }
 public void start() {
  while(true) {
   System.out.println("1.登录  2.注册  0.退出");
   String choice=scanner.nextLine();
   try {
    if("0".equals(choice)) { System.out.println("再见！"); return; }
    if("2".equals(choice)) { handleRegister(); continue; }
    if(!"1".equals(choice)) { System.out.println("无效选项，请重新输入"); continue; }
    Result<User> result=handleLogin();
    if(!result.isSuccess()) { System.out.println(result.getMsg()); continue; }
    User user=result.getData();
    if("admin".equals(user.getRole())) { System.out.println("管理员登录成功"); showAdminMenu(user); }
    else if("student".equals(user.getRole())) { System.out.println("学生登录成功"); showStudentMenu(user); }
    else System.out.println("未知角色，无法进入系统");
   } catch(BusinessException | DataAccessException e) { System.out.println(e.getMessage()); }
   catch(IOException e) { System.out.println("数据保存失败，请稍后重试"); }
  }
 }

}