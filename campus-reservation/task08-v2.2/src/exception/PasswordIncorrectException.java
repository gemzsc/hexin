package exception;

public class PasswordIncorrectException extends BusinessException{
    public PasswordIncorrectException (String message){
        super(message);
    }
}
