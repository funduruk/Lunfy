package ru.funduruk.manager;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class FieldsManager {

    public static boolean checkPassword(String pass) {

        if (pass.length() < 8) {
            return false;
        }

        boolean hasUppercase = pass.chars().anyMatch(Character::isUpperCase);
        boolean hasSymbol    = pass.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        boolean hasDigit = pass.chars().anyMatch(Character::isDigit);

        return hasUppercase && hasSymbol && hasDigit;
    }

    public static boolean checkEmail(String email){

        return email.contains("@");
    }


}
