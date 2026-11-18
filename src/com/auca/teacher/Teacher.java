package com.auca.teacher;

import com.auca.abstracts.AcademicMember;
import com.auca.department.Department;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Registrable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class Teacher extends AcademicMember {

    private static final List<String> EMPLOYMENT_TYPES =
            Collections.unmodifiableList(Arrays.asList("FULL_TIME", "PART_TIME", "CONTRACT"));

    private String employeeNumber;
    private String specialization;
    private LocalDate hireDate;
    private BigDecimal salary;
    private String officeNumber;
    private String employmentType;

    public Teacher(String personId, String firstName, String lastName, String email,
                   String phoneNumber, LocalDate dateOfBirth,
                   String memberNumber, Department department, LocalDate joinDate,
                   String academicStatus, String institutionalEmail, String advisorName,
                   String employeeNumber, String specialization, LocalDate hireDate,
                   BigDecimal salary, String officeNumber, String employmentType) {
        super(personId, firstName, lastName, email, phoneNumber, dateOfBirth,
                memberNumber, department, joinDate, academicStatus, institutionalEmail, advisorName);
        this.employeeNumber = employeeNumber;
        this.specialization = specialization;
        this.hireDate = hireDate;
        this.salary = salary;
        this.officeNumber = officeNumber;
        this.employmentType = employmentType;
    }

    public static List<String> allowedEmploymentTypes() {
        return EMPLOYMENT_TYPES;
    }

    @Override
    public String getAcademicCategory() {
        return "Faculty (teaching staff)";
    }

    @Override
    public String getRoleDescription() {
        return "Teacher specialised in " + specialization + " (" + employmentType + ")";
    }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        Registrable.requireId("Employee number", employeeNumber);
        Registrable.requireText("Specialization", specialization, 2, 120);
        Registrable.requireNotFuture("Hire date", hireDate);
        if (!hireDate.isAfter(getDateOfBirth())) {
            throw new ValidationException("Hire date must be after the date of birth.");
        }
        Registrable.requireMoney("Salary", salary);
        Registrable.requireText("Office number", officeNumber, 1, 20);
        if (employmentType == null || !EMPLOYMENT_TYPES.contains(employmentType)) {
            throw new ValidationException("Employment type must be one of " + EMPLOYMENT_TYPES + ".");
        }
    }

    @Override
    public void printDetails() {
        super.printDetails();
        System.out.println("  Employee number     : " + employeeNumber);
        System.out.println("  Specialization      : " + specialization);
        System.out.println("  Hire date           : " + hireDate);
        System.out.println("  Salary              : " + (salary == null ? "(none)" : salary.toPlainString()));
        System.out.println("  Office number       : " + officeNumber);
        System.out.println("  Employment type     : " + employmentType);
    }

    @Override
    public String getRegistrationSummary() {
        return super.getRegistrationSummary() + " | Employee no. " + employeeNumber;
    }

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    public String getOfficeNumber() { return officeNumber; }
    public void setOfficeNumber(String officeNumber) { this.officeNumber = officeNumber; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    @Override
    public String toString() {
        return "Teacher{employeeNumber='" + employeeNumber + "', name='" + getDisplayName()
                + "', specialization='" + specialization + "', type=" + employmentType + "}";
    }
}
