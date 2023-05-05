package hu.tomlincoln.icfhomework.auth;

public class AuthResponse {

    private final String email;
    private final String accessToken;

    public AuthResponse(String email, String accessToken) {
        this.email = email;
        this.accessToken = accessToken;
    }

    public String getEmail() {
        return email;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
