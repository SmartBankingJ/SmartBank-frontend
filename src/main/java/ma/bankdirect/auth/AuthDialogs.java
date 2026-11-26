package ma.bankdirect.auth;

import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ma.bankdirect.App;
import ma.bankdirect.api.AuthApi;

/** Password lifecycle forms, matching the existing desktop palette. */
public final class AuthDialogs {
    private AuthDialogs() {}
    private static final AuthApi API=AuthApi.INSTANCE;
    private static Dialog<Void> dialog(Window owner,String title) {
        Dialog<Void> dialog=new Dialog<>();dialog.initOwner(owner);dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        ((Button)dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Fermer");
        dialog.getDialogPane().getStyleClass().add("auth-dialog");
        dialog.getDialogPane().getStylesheets().add(App.class.getResource("/com/bankapp/auth.css").toExternalForm());
        dialog.getDialogPane().setPrefWidth(480);return dialog;
    }
    private static VBox content(){VBox box=new VBox(12);box.setPadding(new Insets(16));return box;}
    private static Label feedback(){Label label=new Label();label.setWrapText(true);label.setMaxWidth(400);label.setStyle("-fx-text-fill:#A52D37;");return label;}
    private static PasswordField password(String hint){PasswordField field=new PasswordField();field.setPromptText(hint);field.setMaxWidth(Double.MAX_VALUE);return field;}
    private static Button primary(String text){Button button=new Button(text);button.getStyleClass().add("login-button");button.setMaxWidth(Double.MAX_VALUE);button.setDefaultButton(true);return button;}
    private static void message(Label target,Throwable error){target.setText(error instanceof AuthApi.Failure?error.getMessage():"Une erreur est survenue. Réessayez.");}

    public static void change(Window owner,String token,boolean firstLogin,Consumer<AuthApi.Result> onSuccess) {
        Dialog<Void> dialog=dialog(owner,"Sécurité du mot de passe");
        VBox box=content();
        Label title=new Label(firstLogin?"Sécurisez votre première connexion":"Changer le mot de passe");title.getStyleClass().add("card-title");title.setWrapText(true);
        Label instruction=new Label("Choisissez un mot de passe unique de 12 à 128 caractères.");instruction.setWrapText(true);
        PasswordField current=password("Mot de passe actuel"),replacement=password("Nouveau mot de passe"),confirm=password("Confirmer le nouveau mot de passe");
        Label feedback=feedback();Button submit=primary("Enregistrer le mot de passe");
        box.getChildren().addAll(title,instruction,new Label("Mot de passe actuel"),current,new Label("Nouveau mot de passe"),replacement,new Label("Confirmation"),confirm,feedback,submit);
        dialog.getDialogPane().setContent(box);
        boolean[] completed={false};
        dialog.setOnHidden(event->{if(firstLogin && !completed[0])Async.run(()->{API.logout(token);return null;},unused->{},error->{});});
        submit.setOnAction(event->{
            if(!replacement.getText().equals(confirm.getText())){feedback.setText("Les mots de passe ne correspondent pas.");return;}
            if(replacement.getLength()<12 || replacement.getLength()>128){feedback.setText("Utilisez 12 à 128 caractères.");return;}
            submit.setDisable(true);feedback.setText("Vérification en cours…");
            String old=current.getText(),next=replacement.getText();
            Async.run(()->API.changePassword(token,old,next),result->{
                if(!dialog.isShowing()) {
                    Async.run(()->{API.logout(result.token());return null;},unused->{},error->{});
                    return;
                }
                completed[0]=true;current.clear();replacement.clear();confirm.clear();dialog.close();onSuccess.accept(result);
            },error->{
                if(error instanceof AuthApi.Failure failure && failure.status()==401) {
                    dialog.close();App.logout(error.getMessage());return;
                }
                submit.setDisable(false);message(feedback,error);
            });
        });
        dialog.show();
    }

    public static void recover(Window owner,String initialUsername) {
        Dialog<Void> dialog=dialog(owner,"Récupérer mes accès");
        VBox box=content();Label title=new Label("Retrouver mon accès");title.getStyleClass().add("card-title");
        TextField username=new TextField(initialUsername);username.setPromptText("Votre identifiant");
        Label feedback=feedback();Button send=primary("Recevoir les instructions");
        Label instruction=new Label("Si un compte correspond, des instructions de récupération seront transmises.");instruction.setWrapText(true);
        box.getChildren().addAll(title,instruction,new Label("Identifiant"),username,feedback,send);
        dialog.getDialogPane().setContent(box);
        send.setOnAction(event->{
            if(username.getText().isBlank()){feedback.setText("Renseignez votre identifiant.");return;}
            String name=username.getText().trim();send.setDisable(true);feedback.setText("Demande en cours…");
            Async.run(()->{API.forgot(name);return null;},unused->resetStep(dialog,name),error->{send.setDisable(false);message(feedback,error);});
        });dialog.show();
    }
    private static void resetStep(Dialog<Void> dialog,String username) {
        if(!dialog.isShowing())return;
        VBox box=content();Label title=new Label("Nouveau mot de passe");title.getStyleClass().add("card-title");
        Label instruction=new Label("Si un compte correspond, utilisez le code reçu pour définir un nouveau mot de passe. Le code expire après 15 minutes.");instruction.setWrapText(true);
        Label simulation=new Label("Développement : le code est remis dans la boîte locale de l’administrateur ; aucun e-mail n’est envoyé.");simulation.setWrapText(true);simulation.setStyle("-fx-text-fill:#007F8D;");
        TextField code=new TextField();code.setPromptText("Collez votre code de récupération");
        PasswordField replacement=password("12 à 128 caractères"),confirm=password("Confirmer le mot de passe");
        Label feedback=feedback();Button submit=primary("Réinitialiser mon mot de passe");
        Hyperlink resend=new Hyperlink("Renvoyer les instructions (une fois par minute)");
        box.getChildren().addAll(title,instruction,simulation,new Label("Code de récupération"),code,new Label("Nouveau mot de passe"),replacement,new Label("Confirmation"),confirm,feedback,submit,resend);
        dialog.getDialogPane().setContent(box);dialog.getDialogPane().getScene().getWindow().sizeToScene();
        submit.setOnAction(event->{
            if(code.getText().isBlank()){feedback.setText("Renseignez le code de récupération.");return;}
            if(!replacement.getText().equals(confirm.getText())){feedback.setText("Les mots de passe ne correspondent pas.");return;}
            if(replacement.getLength()<12 || replacement.getLength()>128){feedback.setText("Utilisez 12 à 128 caractères.");return;}
            String resetCode=code.getText().trim(),next=replacement.getText();submit.setDisable(true);resend.setDisable(true);
            Async.run(()->{API.reset(resetCode,next);return null;},unused->{
                code.clear();replacement.clear();confirm.clear();dialog.close();
                Alert done=new Alert(Alert.AlertType.INFORMATION,"Votre mot de passe a été réinitialisé. Connectez-vous avec votre nouveau mot de passe.");
                done.initOwner(dialog.getOwner());done.setHeaderText("Accès rétabli");done.show();
            },error->{submit.setDisable(false);resend.setDisable(false);message(feedback,error);});
        });
        resend.setOnAction(event->{
            resend.setDisable(true);
            Async.run(()->{API.forgot(username);return null;},unused->{feedback.setText("Si un compte correspond, les instructions seront transmises. Attendez une minute entre deux demandes.");resend.setDisable(false);},error->{message(feedback,error);resend.setDisable(false);});
        });
    }
}
