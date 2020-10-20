package app;

public class NoSuchUserException extends RuntimeException {

    public NoSuchUserException(int id){
        super("There is no user with id " + id);
    }
}
