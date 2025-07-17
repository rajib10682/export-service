package com.metrics.exportservice.validation;

import java.util.regex.Pattern;

public class SpecialCharacterValidator {
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[!@#$%^&*()]");
    
    public static boolean containsSpecialCharacters(String value) {
        return value != null && SPECIAL_CHARS.matcher(value).find();
    }
    
    public static String validateCellValue(String value) {
        if (containsSpecialCharacters(value)) {
            return "Contains invalid special characters: !@#$%^&*()";
        }
        return null;
    }
}
