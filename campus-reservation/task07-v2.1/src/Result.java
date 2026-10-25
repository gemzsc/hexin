public class Result<T> {
    private boolean success;
    private String msg;
    private T data;

    private Result(boolean success,String msg,T data){
        this.success = success;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> success(String msg,T data){
        return new Result<T>(true,msg,data);
    }

    public static <T> Result<T> fail(String msg){
        return new Result<T>(false,msg,null);
    }

    public static <T> Result<T> success(T data){
        return new Result<T>(true,null,data);
    }

    public static <T> Result<T> success(String msg){
        return new Result<T>(true,msg,null);
    }

    public boolean isSuccess(){
        return success;
    }

    public void setSuccess(boolean success ){
        this.success = success;
    }

    public String getMsg(){
        return msg;
    }

    public void setMsg( String msg){
        this.msg = msg;
    }

    public T getData(){
        return data;
    }

    public void setData(T data){
        this.data = data;
    }

    @Override
    public String toString(){
        return "Result{success='" + success
                +"',msg='" + msg
                +"',data="+"}";
    }
}
