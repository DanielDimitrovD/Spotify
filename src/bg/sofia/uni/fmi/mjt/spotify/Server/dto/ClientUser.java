package bg.sofia.uni.fmi.mjt.spotify.Server.dto;

public class ClientUser {
    private final String email;
    private final String password;

    public ClientUser(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
