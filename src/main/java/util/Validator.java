package util;

import java.util.regex.Pattern;

public class Validator {
    private static final Pattern Email = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{4,20}$");
    private static final Pattern PASSWORD = Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$");

    public static boolean isValidEmail(String email) {
        return Email.matcher(email).matches();
    }

    public static boolean isValidUsername(String username) {
        return USERNAME.matcher(username).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD.matcher(password).matches();
    }
}
