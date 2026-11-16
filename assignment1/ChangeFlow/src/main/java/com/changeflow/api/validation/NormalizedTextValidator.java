package com.changeflow.api.validation;
import com.changeflow.domain.ContentRules;
import jakarta.validation.*;
public class NormalizedTextValidator implements ConstraintValidator<NormalizedText, String> {
    private int maximum;
    @Override public void initialize(NormalizedText constraint) { maximum = constraint.max(); }
    @Override public boolean isValid(String value, ConstraintValidatorContext context) {
        try { ContentRules.normalize(value, maximum); return true; }
        catch (IllegalArgumentException error) { return false; }
    }
}
