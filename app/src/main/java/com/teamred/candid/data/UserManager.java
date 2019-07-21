package com.teamred.candid.data;

import java.io.File;

public class UserManager {

    private String userHash;
    private final File root;

    static UserManager instance;

    public static UserManager getInstance(File root) {
        return instance == null ? instance = new UserManager(root) : instance;
    }

    private UserManager(File root) {
        this.root = root;
    }

    void setCurrentUser(String email) {
        userHash = String.valueOf(Math.abs(email.hashCode()));
    }

    File getCurrentUserDirectory() {
        File dir = new File(root, userHash);
        if (!dir.exists()) dir.mkdir();
        return dir;
    }
}
