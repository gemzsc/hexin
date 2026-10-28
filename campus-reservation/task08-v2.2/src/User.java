import java.time.LocalDateTime;

public class User {
    private String username;
    private String password;
    private String role;
    private LocalDateTime registerTime;

    public User(){}         //无参构造

    public User(String username,String password,        // 有参构造
                String role,LocalDateTime registerTime){
        this.username = username;
        this.password = password;
        this.role = role;
        this.registerTime = registerTime;
    }

    public String getUsername(){
        return username;
    }

    public void  setUsername(String username){
        this.username = username;
    }

    public String getPassword(){
        return password;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public String getRole(){
        return role;
    }

    public void setRole(String role){
        this.role = role;
    }

    public LocalDateTime getRegisterTime(){
        return registerTime;
    }

    public void setRegisterTime(LocalDateTime registerTime){
        this.registerTime = registerTime;
    }

    @Override
    public String toString(){
        return "User{username='"+ username
                +"',role='"+role
                +"',registerTime="+registerTime + "}";
    }
}
