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
    private String token;
    private long lastActivity;
    private long lastVerified;

    private SessionManager() {}

    public void start(String token, User u, Customer c) {
        if (token == null || u == null) throw new IllegalArgumentException("An authenticated server session is required");
        this.token = token;
        this.user = u;
        this.customer = c;
        this.loginTime = LocalDateTime.now();
        this.lastActivity = this.lastVerified = System.nanoTime();
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public boolean isLoggedIn() {
        return user != null && token != null;
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
        token = null;
        user = null;
        customer = null;
        loginTime = null;
    }

    private void requireLogin() {
        if (!isLoggedIn()) {
            throw new IllegalStateException("Aucune session active");
        }
    }

    public String token() { requireLogin(); return token; }
    public void activity() { if (isLoggedIn()) lastActivity = System.nanoTime(); }
    public boolean idleExpired() { return isLoggedIn() && System.nanoTime() - lastActivity >= java.time.Duration.ofMinutes(15).toNanos(); }
    public boolean needsVerification() {
        return isLoggedIn() && lastActivity > lastVerified && System.nanoTime()-lastVerified >= java.time.Duration.ofSeconds(60).toNanos();
    }
    public void verified() { lastVerified = System.nanoTime(); }
}
