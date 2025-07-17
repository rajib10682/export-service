package com.metrics.exportservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.Month;

public class QuarterEndDateValidator implements ConstraintValidator<QuarterEndDate, LocalDate> {
    
    @Override
    public void initialize(QuarterEndDate constraintAnnotation) {}
    
    @Override
    public boolean isValid(LocalDate date, ConstraintValidatorContext context) {
        if (date == null) {
            return true;
        }
        
        Month month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();
        
        switch (month) {
            case MARCH:
                return dayOfMonth == 31;
            case JUNE:
                return dayOfMonth == 30;
            case SEPTEMBER:
                return dayOfMonth == 30;
            case DECEMBER:
                return dayOfMonth == 31;
            default:
                return false;
        }
    }
}
