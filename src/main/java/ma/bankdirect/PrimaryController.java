package ma.bankdirect;

import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.*;
import ma.bankdirect.api.AuthApi;
import ma.bankdirect.auth.Async;
import ma.bankdirect.auth.AuthDialogs;
import ma.bankdirect.session.SessionManager;

/** Desktop sign-in presentation. No access is granted without an authentication service. */
public class PrimaryController {
    private final javafx.beans.property.BooleanProperty busy = new javafx.beans.property.SimpleBooleanProperty(false);
    @FXML private StackPane root;
    @FXML private VBox loginCard;
    @FXML private Circle outerRing;
    @FXML private Circle innerRing;
    @FXML private Polygon facet;
    @FXML private Ellipse blueWave;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button visibilityButton;
    @FXML private Button loginButton;
    @FXML private SVGPath eyeIcon;
    @FXML private Label feedbackLabel;

    @FXML
    private void initialize() {
        Rectangle viewport = new Rectangle();
        viewport.widthProperty().bind(root.widthProperty());
        viewport.heightProperty().bind(root.heightProperty());
        root.setClip(viewport);
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        loginButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> busy.get() || usernameField.getText().isBlank() || passwordField.getText().isEmpty(),
                usernameField.textProperty(), passwordField.textProperty(), busy));
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> clearFeedback());
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> clearFeedback());
        for (TextField field : new TextField[] { usernameField, passwordField, visiblePasswordField }) {
            javafx.scene.Parent row = field == usernameField ? field.getParent() : field.getParent().getParent();
            field.focusedProperty().addListener((observable, oldValue, focused) ->
                    row.pseudoClassStateChanged(PseudoClass.getPseudoClass("active-field"), focused));
        }
        loginCard.setMaxHeight(VBox.USE_PREF_SIZE);
        VBox.setMargin(loginButton, new Insets(20, 0, 0, 0));
        VBox.setMargin(feedbackLabel, new Insets(14, 0, 0, 0));
        root.widthProperty().addListener(observable -> resizeBackground());
        root.heightProperty().addListener(observable -> resizeBackground());
        resizeBackground();
    }

    private void resizeBackground() {
        double width = root.getWidth();
        double height = root.getHeight();
        root.pseudoClassStateChanged(PseudoClass.getPseudoClass("compact"), height < 730);
        outerRing.setCenterX(width * 0.32);
        outerRing.setCenterY(height * 0.1);
        outerRing.setRadius(width * 0.52);
        innerRing.setCenterX(width * 0.32);
        innerRing.setCenterY(height * 0.1);
        innerRing.setRadius(width * 0.31);
        facet.getPoints().setAll(width * 0.15, 0.0, width * 0.49, 0.0,
                width * 0.67, height * 0.37, width * 0.50, height * 0.70,
                width * 0.08, height * 0.36);
        blueWave.setCenterX(width * 0.52);
        blueWave.setCenterY(height + 140);
        blueWave.setRadiusX(width * 0.9);
        blueWave.setRadiusY(255);
    }

    @FXML
    private void focusPassword() {
        (visiblePasswordField.isVisible() ? visiblePasswordField : passwordField).requestFocus();
    }

    @FXML
    private void togglePasswordVisibility() {
        boolean show = !visiblePasswordField.isVisible();
        visiblePasswordField.setVisible(show);
        visiblePasswordField.setManaged(show);
        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
        visibilityButton.setAccessibleText(show ? "Masquer le mot de passe" : "Afficher le mot de passe");
        eyeIcon.setContent("M 1 12 C 6 2 18 2 23 12 C 18 22 6 22 1 12 Z "
                + "M 12 8 A 4 4 0 1 0 12 16 A 4 4 0 1 0 12 8"
                + (show ? "" : " M 2 2 L 22 22"));
        TextField activeField = show ? visiblePasswordField : passwordField;
        activeField.requestFocus();
        activeField.positionCaret(activeField.getLength());
    }

    @FXML
    private void submitLogin() {
        if (busy.get()) return;
        if (usernameField.getText().isBlank()) {
            usernameField.requestFocus();
            showFeedback("Veuillez renseigner votre identifiant.");
        } else if (passwordField.getText().isEmpty()) {
            focusPassword();
            showFeedback("Veuillez renseigner votre mot de passe.");
        } else {
            String username = usernameField.getText().trim(), password = passwordField.getText();
            busy.set(true); loginButton.setText("Connexion en cours…");
            usernameField.setDisable(true); passwordField.setDisable(true); visiblePasswordField.setDisable(true);
            Async.run(() -> AuthApi.INSTANCE.login(username, password), result -> {
                finishRequest(); passwordField.clear();
                if (root.getScene().getRoot() != root) {
                    Async.run(() -> { AuthApi.INSTANCE.logout(result.token()); return null; }, unused -> {}, error -> {});
                    return;
                }
                if (result.mustChangePassword()) {
                    showFeedback("Définissez un nouveau mot de passe pour terminer votre première connexion.");
                    AuthDialogs.change(root.getScene().getWindow(), result.token(), true, this::openAuthenticatedDashboard);
                } else openAuthenticatedDashboard(result);
            }, error -> { finishRequest(); passwordField.clear(); showFeedback(error instanceof AuthApi.Failure ? error.getMessage() : "Impossible de terminer la connexion."); });
        }
    }

    private void finishRequest() {
        busy.set(false); loginButton.setText("Se connecter");
        usernameField.setDisable(false); passwordField.setDisable(false); visiblePasswordField.setDisable(false);
    }

    private void openAuthenticatedDashboard(AuthApi.Result result) {
        if (!"CUSTOMER".equals(result.user().getRole()) || result.customer() == null) {
            Async.run(() -> { AuthApi.INSTANCE.logout(result.token()); return null; }, unused -> {}, error -> {});
            showFeedback("Votre rôle ne dispose pas encore d’un espace desktop. Contactez l’administrateur.");
            return;
        }
        SessionManager.get().start(result.token(), result.user(), result.customer());
        try { App.setRoot("/fxml/dashboard/dashboard.fxml"); }
        catch (Exception e) { App.logout("Impossible d’ouvrir le tableau de bord. Reconnectez-vous."); }
    }

    private void showFeedback(String message) {
        feedbackLabel.setText(message);
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
        feedbackLabel.setAccessibleText(message);
    }

    private void clearFeedback() {
        feedbackLabel.setText("");
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
    }

    @FXML
    private void showAccessHelp() {
        if (!busy.get()) AuthDialogs.recover(root.getScene().getWindow(), usernameField.getText());
    }

    @FXML
    private void showSecurityAdvice() {
        showInformation("Conseils de sécurité", "Protégez votre espace personnel",
                "Utilisez un mot de passe unique.\n\nNe partagez ni votre mot de passe ni vos codes de confirmation."
                + "\n\nDéconnectez-vous après utilisation d’un ordinateur partagé.");
    }

    @FXML
    private void showAbout() {
        showInformation("À propos", "SmartBanking — Application desktop",
                "Projet pédagogique de banque numérique développé en JavaFX.\n\n"
                + "Les opérations et données de démonstration ne correspondent pas à des services bancaires réels.");
    }

    @FXML
    private void showCustomerInformation() {
        showInformation("Devenir client", "Ouverture d’un espace client",
                "Les espaces clients sont créés par l’administrateur dans le cadre du projet. "
                + "L’ouverture autonome d’un compte n’est pas disponible.");
    }

    private void showInformation(String title, String heading, String message) {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.initOwner(root.getScene().getWindow());
        dialog.setTitle(title);
        dialog.setHeaderText(heading);
        dialog.setContentText(message);
        dialog.getDialogPane().getStyleClass().add("auth-dialog");
        dialog.getDialogPane().getStylesheets().add(
                App.class.getResource("/com/bankapp/auth.css").toExternalForm());
        dialog.getDialogPane().setPrefWidth(470);
        dialog.showAndWait();
    }
}
