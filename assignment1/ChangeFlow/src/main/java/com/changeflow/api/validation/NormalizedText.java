package com.changeflow.api.validation;
import jakarta.validation.*;
import java.lang.annotation.*;
@Documented @Constraint(validatedBy = NormalizedTextValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NormalizedText {
    String message() default "Must be nonblank and within the normalized code-point limit.";
    int max() default 2000;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
