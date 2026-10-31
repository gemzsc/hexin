import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;


public class FileUtil {
    private FileUtil(){
    }
    //用户文件格式： username|password|role|registerTime
    //资源文件格式: id|name|type|status|location|description
    private static final Path USER_FILE = Paths.get("user.txt");
    private static final Path RESOURCE_FILE = Paths.get("resource.txt");

    public static void ensureUserFileExists() throws IOException{
        if (!Files.exists(USER_FILE)){
            Files.createFile(USER_FILE);
            System.out.println("已创建用户数据文件");
        }
    }

    public static void ensureResourceFileExists() throws IOException{
        if (!Files.exists(RESOURCE_FILE)){
            Files.createFile(RESOURCE_FILE);
            System.out.println("已创建资源数据文件");
        }
    }

    private static void validateResources(List<CampusResource> resources) {
        for (CampusResource resource : resources) {
            if (resource == null
                    || resource.getId() == null
                    || resource.getName() == null
                    || resource.getType() == null
                    || resource.getStatus() == null
                    || resource.getLocation() == null
                    || resource.getDescription() == null) {
                throw new IllegalArgumentException("资源信息不完整");
            }

            if (!isValidResourceStatus(resource.getStatus())) {
                throw new IllegalArgumentException("资源状态不合法");
            }

            String[] fields = {
                    resource.getId(),
                    resource.getName(),
                    resource.getType(),
                    resource.getStatus(),
                    resource.getLocation(),
                    resource.getDescription()
            };

            for (String field : fields) {
                if (field.contains("|")) {
                    throw new IllegalArgumentException("资源字段不能包含分隔符 |");
                }
            }
        }
    }

    public static List<User> loadUsers() throws IOException{
        List<User> users =new ArrayList<>();

        ensureUserFileExists();

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

    private static boolean isValidResourceStatus(String status) {
        return "AVAILABLE".equals(status)
                || "OCCUPIED".equals(status)
                || "DISABLED".equals(status);
    }

    public static List<CampusResource> loadResources() throws IOException {
        List<CampusResource> resources = new ArrayList<>();
        Set<String> resourceIds = new HashSet<>();
        ensureResourceFileExists();

        try (BufferedReader reader =
                     Files.newBufferedReader(RESOURCE_FILE, StandardCharsets.UTF_8)) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|", -1);

                if (fields.length != 6) {
                    System.out.println("跳过格式错误的资源数据：" + line);
                    continue;
                }

                if (fields[0].trim().isEmpty()
                        || fields[1].trim().isEmpty()
                        || fields[2].trim().isEmpty()
                        || fields[3].trim().isEmpty()
                        || fields[4].trim().isEmpty()) {
                    System.out.println("跳过必要字段为空的资源数据：" + line);
                    continue;
                }
                String status = fields[3];
                boolean validStatus =
                        "AVAILABLE".equals(status)
                                || "OCCUPIED".equals(status)
                                || "DISABLED".equals(status);

                if (!validStatus) {
                    System.out.println("跳过状态不合法的资源数据：" + line);
                    continue;
                }

                String id = fields[0];
                if (resourceIds.contains(id)) {
                    System.out.println("跳过资源 ID 重复的数据：" + line);
                    continue;
                }
                resourceIds.add(id);

                CampusResource resource = new CampusResource(
                        fields[0], fields[1], fields[2],
                        fields[3], fields[4], fields[5]
                );
                resources.add(resource);
            }
        }

        return resources;
    }

    public static void saveResources(List<CampusResource> resources) throws IOException {
        if (resources == null) {
            throw new IllegalArgumentException("资源集合不能为空");
        }

        validateResources(resources);

        try (BufferedWriter writer = Files.newBufferedWriter(
                RESOURCE_FILE,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {

            for (CampusResource resource : resources) {

                String line = resource.getId() + "|"
                        + resource.getName() + "|"
                        + resource.getType() + "|"
                        + resource.getStatus() + "|"
                        + resource.getLocation() + "|"
                        + resource.getDescription();

                writer.write(line);
                writer.newLine();
            }
        }
    }

}
