import dao.*;
import dao.file.*;
import service.*;
import exception.*;
import ui.console.ConsoleUI;
import utils.FileUtil;
import java.io.IOException;
import java.util.Scanner;

public class SystemMain {
 public static void main(String[] args) {
  try(Scanner scanner=new Scanner(System.in)) {
   FileUtil.setWarningSink(ConsoleUI::displayWarning);
   try {
    UserDao userDao=new FileUserDao();
    ResourceDao resourceDao=new FileResourceDao();
    ReserveRecordDao recordDao=new FileReserveRecordDao();
    UserService userService=new UserService(userDao);
    userService.ensureAdminExists();
    ResourceService resourceService=new ResourceService(resourceDao,recordDao);
    ReserveRecordService reserveService=new ReserveRecordService(resourceDao,recordDao);
    new ConsoleUI(scanner,userService,resourceService,reserveService).start();
   } catch(IOException | DataAccessException | BusinessException e) {
    ConsoleUI.displayWarning("读取数据失败，程序无法启动："+e.getMessage());
   }
  }
 }
}
