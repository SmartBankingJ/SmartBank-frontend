package ma.bankdirect;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import ma.bankdirect.api.AuthApi;
import ma.bankdirect.auth.Async;
import ma.bankdirect.session.SessionManager;


/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private Timeline sessionMonitor;
    private boolean verifying;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 1180, 780);
        scene.getStylesheets().add(App.class.getResource("/com/bankapp/auth.css").toExternalForm());
        stage.setTitle("SmartBanking — Espace client");
        stage.setMinWidth(1000);
        stage.setMinHeight(720);
        stage.setScene(scene);
        scene.addEventFilter(KeyEvent.ANY, event -> SessionManager.get().activity());
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> SessionManager.get().activity());
        scene.addEventFilter(ScrollEvent.SCROLL, event -> SessionManager.get().activity());
        sessionMonitor = new Timeline(new KeyFrame(Duration.seconds(15), event -> verifySession()));
        sessionMonitor.setCycleCount(Timeline.INDEFINITE); sessionMonitor.play();
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        if (fxml.contains("dashboard") && !SessionManager.get().isLoggedIn())
            throw new IOException("Aucune session authentifiée");
        scene.setRoot(loadFXML(fxml));
    }

    private void verifySession() {
        SessionManager session = SessionManager.get();
        if (session.idleExpired()) { logout("Votre session a expiré après 15 minutes d’inactivité."); return; }
        if (!session.needsVerification() || verifying) return;
        String token = session.token(); verifying = true;
        Async.run(() -> { AuthApi.INSTANCE.me(token); return null; }, unused -> {
            verifying = false;
            if (session.isLoggedIn() && token.equals(session.token())) session.verified();
        }, error -> {
            verifying = false;
            if (session.isLoggedIn() && token.equals(session.token()))
                logout(error instanceof AuthApi.Failure ? error.getMessage() : "Votre accès doit être vérifié. Reconnectez-vous.");
        });
    }

    public static void logout(String message) {
        SessionManager session = SessionManager.get();
        String token = session.isLoggedIn() ? session.token() : null;
        session.clear();
        Async.run(() -> { AuthApi.INSTANCE.logout(token); return null; }, unused -> {}, error -> {});
        try {
            setRoot("primary");
            if (message != null && !message.isBlank()) {
                javafx.scene.control.Label feedback = (javafx.scene.control.Label) scene.getRoot().lookup("#feedbackLabel");
                feedback.setText(message); feedback.setVisible(true); feedback.setManaged(true);
            }
        } catch (IOException e) { throw new IllegalStateException("Impossible d’afficher la connexion", e); }
    }

    @Override public void stop() {
        if (sessionMonitor != null) sessionMonitor.stop();
        SessionManager.get().clear();
    }

    public static Parent loadFXML(String fxmlPath) throws IOException {
        String path = fxmlPath.endsWith(".fxml") ? fxmlPath : fxmlPath + ".fxml";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        var resource = App.class.getResource(path);
        if (resource == null) {
            resource = App.class.getResource("/com/bankapp" + path);
        }
        if (resource == null) {
            resource = App.class.getResource("/fxml" + path);
        }
        if (resource == null) {
            throw new IOException("FXML introuvable : " + path);
        }
        FXMLLoader fxmlLoader = new FXMLLoader(resource);
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}
