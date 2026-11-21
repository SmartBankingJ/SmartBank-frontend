package ma.bankdirect;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;


/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 1180, 780);
        scene.getStylesheets().add(App.class.getResource("/com/bankapp/auth.css").toExternalForm());
        stage.setTitle("SmartBanking — Espace client");
        stage.setMinWidth(1000);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
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
