package com.changeflow.api.dto;

import com.changeflow.domain.ContentRules;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.*;
import java.lang.annotation.*;

/** Omission preserves a value; an explicitly supplied null fails validation. */
@EditRequest.ValidFields
public final class EditRequest {
    private boolean titleSupplied, descriptionSupplied, reasonSupplied;
    private String title, description, reason;

    @JsonSetter public void setTitle(String value) { titleSupplied = true; title = normalize(value); }
    @JsonSetter public void setDescription(String value) { descriptionSupplied = true; description = normalize(value); }
    @JsonSetter public void setReason(String value) { reasonSupplied = true; reason = normalize(value); }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getReason() { return reason; }
    public boolean titleSupplied() { return titleSupplied; }
    public boolean descriptionSupplied() { return descriptionSupplied; }
    public boolean reasonSupplied() { return reasonSupplied; }
    private static String normalize(String value) { return value == null ? null : value.strip(); }

    @Documented @Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = FieldsValidator.class)
    public @interface ValidFields {
        String message() default "Must contain valid supplied content.";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }
    public static final class FieldsValidator implements ConstraintValidator<ValidFields, EditRequest> {
        @Override public boolean isValid(EditRequest value, ConstraintValidatorContext context) {
            if (value == null) return false;
            context.disableDefaultConstraintViolation();
            boolean title = check(value.titleSupplied, value.title, 200, "title", context);
            boolean description = check(value.descriptionSupplied, value.description, 5000, "description", context);
            boolean reason = check(value.reasonSupplied, value.reason, 2000, "reason", context);
            return title && description && reason;
        }
        private boolean check(boolean supplied, String text, int maximum, String field, ConstraintValidatorContext context) {
            if (!supplied) return true;
            try { ContentRules.normalize(text, maximum); return true; }
            catch (IllegalArgumentException invalid) {
                context.buildConstraintViolationWithTemplate("Must contain valid content.").addPropertyNode(field).addConstraintViolation();
                return false;
            }
        }
    }
}
