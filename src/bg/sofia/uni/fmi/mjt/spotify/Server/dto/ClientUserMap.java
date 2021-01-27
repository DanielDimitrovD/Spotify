package bg.sofia.uni.fmi.mjt.spotify.Server.dto;

import java.util.Map;

public class ClientUserMap {

    private Map<String, String> users;

    public ClientUserMap(Map<String, String> users) {
        this.users = users;
    }

    public Map<String, String> getUsers() {
        return users;
    }
}
