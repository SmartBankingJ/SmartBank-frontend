package ma.bankdirect.auth;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.concurrent.Task;

/** Network work is performed outside the JavaFX application thread. */
public final class Async {
    private Async() {}
    public static <T> void run(Callable<T> operation,Consumer<T> success,Consumer<Throwable> failure) {
        Task<T> task=new Task<>() {protected T call() throws Exception{return operation.call();}};
        task.setOnSucceeded(event->success.accept(task.getValue()));
        task.setOnFailed(event->failure.accept(task.getException()));
        Thread thread=new Thread(task,"smartbank-api");thread.setDaemon(true);thread.start();
    }
}
