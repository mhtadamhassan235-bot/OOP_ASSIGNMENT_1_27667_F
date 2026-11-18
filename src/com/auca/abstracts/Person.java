package com.auca.abstracts;

import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;

import java.time.LocalDate;

/**
 * Root of the person hierarchy. Abstract: a "generic person" has no meaning in the school,
 * so only Student, Teacher and Administrator can be instantiated.
 * All attributes are private; subclasses reach them through getters/setters.
 */
public abstract class Person implements Identifiable, Registrable {

    private String personId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;

    protected Person(String personId, String firstName, String lastName, String email,
                     String phoneNumber, LocalDate dateOfBirth) {
        this.personId = personId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
    }

    /** Role-specific description, implemented by every concrete person type. */
    public abstract String getRoleDescription();

    // ----- Identifiable -----
    @Override
    public String getId() {
        return personId;
    }

    @Override
    public String getDisplayName() {
        return firstName + " " + lastName;
    }

    /** Default behaviour; AcademicMember and Administrator override it. */
    @Override
    public boolean isActive() {
        return true;
    }

    // ----- Registrable -----
    @Override
    public void validate() throws ValidationException {
        Registrable.requireId("Person ID", personId);
        Registrable.requireName("First name", firstName);
        Registrable.requireName("Last name", lastName);
        Registrable.requireEmail("Email", email);
        Registrable.requirePhone("Phone", phoneNumber);
        Registrable.requirePastDate("Date of birth", dateOfBirth);
    }

    @Override
    public void printDetails() {
        System.out.println("  Role                : " + getRoleDescription());
        System.out.println("  Person ID           : " + personId);
        System.out.println("  First name          : " + firstName);
        System.out.println("  Last name           : " + lastName);
        System.out.println("  Email               : " + email);
        System.out.println("  Phone               : " + phoneNumber);
        System.out.println("  Date of birth       : " + dateOfBirth);
    }

    @Override
    public String getRegistrationSummary() {
        return "[" + personId + "] " + getDisplayName() + " - " + getRoleDescription();
    }

    // ----- getters / setters -----
    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{personId='" + personId + "', name='" + getDisplayName()
                + "', email='" + email + "', phone='" + phoneNumber + "', dateOfBirth=" + dateOfBirth + "}";
    }
}
