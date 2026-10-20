package com.studyhive.desktop.config;

import com.studyhive.desktop.api.ApiClient;
import com.studyhive.desktop.api.AuthApi;
import com.studyhive.desktop.api.CommunitiesApi;
import com.studyhive.desktop.api.MaterialsApi;
import com.studyhive.desktop.api.QuestionPapersApi;
import com.studyhive.desktop.api.ReportsApi;
import com.studyhive.desktop.api.SubjectsApi;
import com.studyhive.desktop.model.AuthResponse;
import com.studyhive.desktop.model.LoginRequest;
import com.studyhive.desktop.model.RegisterRequest;
import com.studyhive.desktop.model.User;

/**
 * Process-wide singleton wiring the API client/session together. FXML controllers are
 * instantiated by FXMLLoader without constructor injection, so this is the simplest
 * practical way to share one HTTP client and auth state across every screen.
 */
public final class AppContext {

    private static final AppContext INSTANCE = new AppContext();

    private final ApiClient apiClient = new ApiClient();
    private final SessionStore sessionStore = new SessionStore();

    public final AuthApi auth = new AuthApi(apiClient);
    public final CommunitiesApi communities = new CommunitiesApi(apiClient);
    public final SubjectsApi subjects = new SubjectsApi(apiClient);
    public final MaterialsApi materials = new MaterialsApi(apiClient);
    public final QuestionPapersApi questionPapers = new QuestionPapersApi(apiClient);
    public final ReportsApi reports = new ReportsApi(apiClient);

    private User currentUser;

    private AppContext() {
    }

    public static AppContext get() {
        return INSTANCE;
    }

    public User currentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Tries to restore a saved session on startup. Returns true if a valid session was restored. */
    public boolean restoreSession() {
        var savedToken = sessionStore.loadToken();
        if (savedToken.isEmpty()) {
            return false;
        }
        apiClient.setAuthToken(savedToken.get());
        try {
            currentUser = auth.me();
            return true;
        } catch (RuntimeException e) {
            // Token expired/invalid -- clear it and fall back to the login screen.
            sessionStore.clear();
            apiClient.clearAuthToken();
            return false;
        }
    }

    public User login(String usernameOrEmail, String password) {
        AuthResponse response = auth.login(new LoginRequest(usernameOrEmail, password));
        applySession(response);
        return response.user();
    }

    public User register(String username, String email, String password, String displayName) {
        AuthResponse response = auth.register(new RegisterRequest(username, email, password, displayName));
        applySession(response);
        return response.user();
    }

    public void logout() {
        currentUser = null;
        apiClient.clearAuthToken();
        sessionStore.clear();
    }

    private void applySession(AuthResponse response) {
        apiClient.setAuthToken(response.token());
        sessionStore.saveToken(response.token());
        currentUser = response.user();
    }
}
