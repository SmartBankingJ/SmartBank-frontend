package ma.bankdirect.session;

import java.time.LocalDateTime;
import ma.bankdirect.model.Customer;
import ma.bankdirect.model.User;

/**
 * Gestionnaire de session utilisateur (Singleton).
 * Mémorise l'utilisateur et le client actuellement connectés.
 */
public final class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();

    public static SessionManager get() {
        return INSTANCE;
    }

    private User user;
    private Customer customer;
    private LocalDateTime loginTime;

    private SessionManager() {}

    public void start(User u, Customer c) {
        this.user = u;
        this.customer = c;
        this.loginTime = LocalDateTime.now();
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public boolean isLoggedIn() {
        return user != null;
    }

    public User currentUser() {
        requireLogin();
        return user;
    }

    public Customer currentCustomer() {
        requireLogin();
        return customer;
    }

    public void updateCustomer(Customer c) {
        this.customer = c;
    }

    public void clear() {
        user = null;
        customer = null;
        loginTime = null;
    }

    private void requireLogin() {
        if (user == null) {
            throw new IllegalStateException("Aucune session active");
        }
    }
}
