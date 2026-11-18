package com.auca.abstracts;

import com.auca.department.Department;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public abstract class AcademicMember extends Person {

    private static final List<String> STATUSES =
            Collections.unmodifiableList(Arrays.asList(Identifiable.DEFAULT_STATUS, "INACTIVE"));

    private String memberNumber;
    private Department department;
    private LocalDate joinDate;
    private String academicStatus;
    private String institutionalEmail;
    private String advisorName;

    protected AcademicMember(String personId, String firstName, String lastName, String email,
                             String phoneNumber, LocalDate dateOfBirth,
                             String memberNumber, Department department, LocalDate joinDate,
                             String academicStatus, String institutionalEmail, String advisorName) {
        super(personId, firstName, lastName, email, phoneNumber, dateOfBirth); // constructor chaining
        this.memberNumber = memberNumber;
        this.department = department;
        this.joinDate = joinDate;
        this.academicStatus = academicStatus;
        this.institutionalEmail = institutionalEmail;
        this.advisorName = advisorName;
    }

   
    public abstract String getAcademicCategory();

    public static List<String> allowedStatuses() {
        return STATUSES;
    }

    @Override
    public boolean isActive() {
        return Identifiable.DEFAULT_STATUS.equals(academicStatus);
    }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        Registrable.requireId("Member number", memberNumber);
        Registrable.requireNotNull("Department", department);
        Registrable.requireNotFuture("Join date", joinDate);
        if (!joinDate.isAfter(getDateOfBirth())) {
            throw new ValidationException("Join date must be after the date of birth.");
        }
        if (academicStatus == null || !STATUSES.contains(academicStatus)) {
            throw new ValidationException("Academic status must be one of " + STATUSES + ".");
        }
        Registrable.requireEmail("Institutional email", institutionalEmail);
        Registrable.requireText("Advisor name", advisorName, 2, 120);
    }

    @Override
    public void printDetails() {
        super.printDetails();
        System.out.println("  Academic category   : " + getAcademicCategory());
        System.out.println("  Member number       : " + memberNumber);
        System.out.println("  Department          : " + (department == null ? "(none)"
                : "[" + department.getId() + "] " + department.getDepartmentName()));
        System.out.println("  Join date           : " + joinDate);
        System.out.println("  Academic status     : " + academicStatus);
        System.out.println("  Institutional email : " + institutionalEmail);
        System.out.println("  Advisor name        : " + advisorName);
    }

    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }
    public String getAcademicStatus() { return academicStatus; }
    public void setAcademicStatus(String academicStatus) { this.academicStatus = academicStatus; }
    public String getInstitutionalEmail() { return institutionalEmail; }
    public void setInstitutionalEmail(String institutionalEmail) { this.institutionalEmail = institutionalEmail; }
    public String getAdvisorName() { return advisorName; }
    public void setAdvisorName(String advisorName) { this.advisorName = advisorName; }

    @Override
    public String toString() {
        return super.toString() + "+{memberNumber='" + memberNumber + "', department="
                + (department == null ? "null" : department.getDepartmentCode())
                + ", joinDate=" + joinDate + ", status=" + academicStatus + "}";
    }
}
