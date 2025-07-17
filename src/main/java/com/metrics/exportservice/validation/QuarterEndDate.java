package com.metrics.exportservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = QuarterEndDateValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface QuarterEndDate {
    String message() default "Date must be the last date of a quarter (Mar 31, Jun 30, Sep 30, or Dec 31)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
