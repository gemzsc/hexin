import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;


public class FileUtil {
    private FileUtil(){
    }
    //文件格式： username|password|role|registerTime
    private static final Path USER_FILE = Paths.get("user.txt");

    public static void ensureFileExists() throws IOException{
        if (!Files.exists(USER_FILE)){
            Files.createFile(USER_FILE);
            System.out.println("已创建用户数据文件");
        }
    }

    public static List<User> loadUsers() throws IOException{
        List<User> users =new ArrayList<>();

        ensureFileExists();

        try(BufferedReader reader =
                    Files.newBufferedReader(USER_FILE,StandardCharsets.UTF_8)){
            String line;

            while((line=reader.readLine()) != null){
                String[] fields = line.split("\\|",-1);

                if(fields.length != 4){
                    System.out.println("跳过格式错误的用户数据：" + line);
                    continue;           //单条记录格式错误时跳过，继续加载其余记录
                }

                if (fields[0].trim().isEmpty()
                        || fields[1].trim().isEmpty()
                        || fields[2].trim().isEmpty()) {
                    System.out.println("跳过字段为空的用户数据：" + line);
                    continue;
                }

                try{

                    User user = new User(
                            fields[0],
                            fields[1],
                            fields[2],
                            LocalDateTime.parse(fields[3])
                    );
                    users.add(user);
                }catch (java.time.format.DateTimeParseException e){
                    System.out.println("跳过注册时间格式错误的数据：" + line);
                }
            }
        }

        return users;
    }

    public static void saveUser(User user) throws IOException{
        if (user == null
                || user.getUsername() == null
                || user.getPassword() == null
                || user.getRole() == null
                || user.getRegisterTime() == null) {
            throw new IllegalArgumentException("用户信息不完整");
        }

        if (user.getUsername().contains("|")
                || user.getPassword().contains("|")
                || user.getRole().contains("|")) {
            throw new IllegalArgumentException("用户信息包含非法分隔符");
        }

        String line = user.getUsername() + "|"
                + user.getPassword() + "|"
                + user.getRole() + "|"
                + user.getRegisterTime();

        try (BufferedWriter writer = Files.newBufferedWriter(
                USER_FILE,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)){
            writer.write(line);
            writer.newLine();
        }
    }

}
