package ma.bankdirect.controller.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import ma.bankdirect.model.Account;
import ma.bankdirect.model.Customer;
import ma.bankdirect.model.Transaction;
import ma.bankdirect.model.enums.AccountStatus;
import ma.bankdirect.model.enums.AccountType;
import ma.bankdirect.model.enums.Direction;
import ma.bankdirect.model.enums.TransactionStatus;
import ma.bankdirect.model.enums.TransactionType;
import ma.bankdirect.session.SessionManager;
import ma.bankdirect.util.Money;

/**
 * Contrôleur de l'écran d'accueil - Reproduction fidèle de l'application BMCE Direct.
 */
public class DashboardController {

    // 1. Éléments de la carte compte
    @FXML private Label currentRibLabel;
    @FXML private Label currentBalanceLabel;

    // 2. Éléments des 3 opérations récentes dans la carte
    @FXML private Label opDate1;
    @FXML private Label opLabel1;
    @FXML private Label opAmount1;

    @FXML private Label opDate2;
    @FXML private Label opLabel2;
    @FXML private Label opAmount2;

    @FXML private Label opDate3;
    @FXML private Label opLabel3;
    @FXML private Label opAmount3;

    // 3. Conseiller
    @FXML private Label advisorNameLabel;

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM");

    @FXML
    public void initialize() {
        // Chargement des données à l'ouverture de l'écran
        loadAccountData();
        loadRecentTransactions();
    }

    /**
     * Charge le compte et le solde du client connecté.
     */
    private void loadAccountData() {
        Customer customer = SessionManager.get().currentCustomer();
        if (customer == null) throw new IllegalStateException("Le tableau de bord client nécessite un profil client");

        // Compte Chèque Particulier
        Account account = new Account(1L, customer.getId(), "011780000012345678901234", AccountType.CURRENT, "MAD",
                new BigDecimal("24500.00"), AccountStatus.ACTIVE, LocalDateTime.now().minusYears(1));

        currentRibLabel.setText(account.getMaskedRib());
        currentBalanceLabel.setText(Money.format(account.getBalance()));

        advisorNameLabel.setText("Mme Malika Ezzakri");
    }

    /**
     * Charge les 3 dernières transactions visibles directement dans la carte du compte.
     */
    private void loadRecentTransactions() {
        LocalDateTime now = LocalDateTime.now();

        // Transaction 1
        opDate1.setText(now.format(SHORT_DATE));
        opLabel1.setText("Virement Vers Bénéficiaire...");
        opAmount1.setText("- 500,00 MAD");

        // Transaction 2
        opDate2.setText(now.format(SHORT_DATE));
        opLabel2.setText("Virement Vers Bénéficiaire...");
        opAmount2.setText("- 1 200,00 MAD");

        // Transaction 3
        opDate3.setText(now.minusDays(1).format(SHORT_DATE));
        opLabel3.setText("Paiement Par Carte 47791...");
        opAmount3.setText("- 350,00 MAD");
    }

    // ==========================================
    // Événements et Actions utilisateurs
    // ==========================================

    @FXML
    private void onLogout() {
        ma.bankdirect.App.logout(null);
    }

    @FXML
    private void onAdvisorClicked() {
        showMessage("Mon Conseiller", "Contact agence : Mme Malika Ezzakri\nTél : +212 522-001122\nEmail : m.ezzakri@bmcedirect.ma");
    }

    @FXML
    private void onLiveChatClicked() {
        showMessage("Live Chat", "Démarrage de la session d'assistance en ligne avec le support bancaire...");
    }

    @FXML
    private void onSituationClicked() {
        showMessage("Navigation", "Ouverture de l'onglet : Ma situation patrimoniale.");
    }

    @FXML
    private void onTransactionsClicked() {
        showMessage("Navigation", "Ouverture de l'onglet : Toutes les transactions et virements.");
    }

    @FXML
    private void onProductsClicked() {
        showMessage("Navigation", "Ouverture de l'onglet : Mes produits (Cartes, Assurances, Crédits).");
    }

    @FXML
    private void onMenuClicked() {
        String token = SessionManager.get().token();
        ma.bankdirect.auth.AuthDialogs.change(advisorNameLabel.getScene().getWindow(), token, false, result ->
                SessionManager.get().start(result.token(), result.user(), result.customer()));
    }

    private void showMessage(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
