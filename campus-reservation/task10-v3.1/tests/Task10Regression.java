import dao.*;
import dao.file.*;
import service.*;
import entity.*;
import exception.*;
import java.time.LocalDateTime;

/** Run in an empty temporary working directory; writes course data files. */
public class Task10Regression {
 interface Action { void run() throws Exception; }
 static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
 static void denied(Action action) throws Exception {
  try { action.run(); } catch(BusinessException e) { return; }
  throw new AssertionError("Expected business rejection");
 }
 public static void main(String[] args) throws Exception {
  UserService users=new UserService(new FileUserDao()); users.ensureAdminExists();
  User admin=users.login("admin","123456").getData();
  users.register("alice","123456"); users.register("bob","123456");
  User alice=users.login("alice","123456").getData();
  User bob=users.login("bob","123456").getData();
  denied(() -> users.register("alice","123456"));
  denied(() -> users.login("alice","wrong"));
  ResourceDao rd=new FileResourceDao(); ReserveRecordDao bd=new FileReserveRecordDao();
  ResourceService resources=new ResourceService(rd,bd);
  ReserveRecordService bookings=new ReserveRecordService(rd,bd);
  CampusResource room=resources.addResource(admin,"Room","Classroom","Building A","").getData();
  String id=room.getId();
  denied(() -> resources.addResource(alice,"Bad","Classroom","B",""));
  resources.updateResource(admin,id,"Room changed","","","","");
  check(resources.searchResourceByName("changed").getData().size()==1,"search/update");
  LocalDateTime begin=LocalDateTime.now().plusDays(2),end=begin.plusHours(1);
  denied(() -> bookings.submitReserve(alice,id,end,begin,""));
  ReserveRecord rec=bookings.submitReserve(alice,id,begin,end,"").getData();
  check(bookings.queryMyReserve(bob).getData().isEmpty(),"privacy");
  denied(() -> bookings.approveReserve(alice,rec.getRecordId()));
  denied(() -> bookings.cancelReserve(bob,rec.getRecordId()));
  bookings.approveReserve(admin,rec.getRecordId());
  check("OCCUPIED".equals(resources.queryResourceById(id).getData().getStatus()),"approve link");
  denied(() -> resources.deleteResource(admin,id));
  denied(() -> bookings.submitReserve(alice,id,begin,end,""));
  bookings.cancelReserve(alice,rec.getRecordId());
  check("AVAILABLE".equals(resources.queryResourceById(id).getData().getStatus()),"cancel link");
  denied(() -> bookings.cancelReserve(alice,rec.getRecordId()));
  ReserveRecord rejected=bookings.submitReserve(alice,id,begin,end,"").getData();
  bookings.rejectReserve(admin,rejected.getRecordId());
  denied(() -> bookings.approveReserve(admin,rejected.getRecordId()));
  check(new FileReserveRecordDao().findAll().size()==2,"persist bookings");
  check(new UserService(new FileUserDao()).login("alice","123456").isSuccess(),"restart login");
  check(new FileResourceDao().findAll().size()==1,"persist resource");
  resources.deleteResource(admin,id);
  check(resources.queryAllResources().getData().isEmpty(),"delete");
  System.out.println("PASS: user, resource CRUD, booking lifecycle, permissions, persistence");
 }
}
