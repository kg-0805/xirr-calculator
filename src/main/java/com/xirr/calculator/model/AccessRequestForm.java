package com.xirr.calculator.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AccessRequestForm {

    @NotBlank(message = "Full name is required.")
    @Size(max = 120, message = "Full name must be 120 characters or fewer.")
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 160, message = "Email must be 160 characters or fewer.")
    private String email;

    @NotBlank(message = "Desired username is required.")
    @Size(min = 4, max = 32, message = "Username must be between 4 and 32 characters.")
    @Pattern(
            regexp = "^[A-Za-z0-9._-]+$",
            message = "Username can use letters, numbers, dots, underscores, and hyphens only."
    )
    private String desiredUsername;

    @NotBlank(message = "Please explain why you need access.")
    @Size(min = 20, max = 500, message = "Access reason must be between 20 and 500 characters.")
    private String purpose;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDesiredUsername() {
        return desiredUsername;
    }

    public void setDesiredUsername(String desiredUsername) {
        this.desiredUsername = desiredUsername;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
