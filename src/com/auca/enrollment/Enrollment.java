package com.auca.enrollment;

import com.auca.course.Course;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;
import com.auca.student.Student;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Link between a student and a course for one semester. */
public class Enrollment implements Identifiable, Registrable {

    private static final List<String> STATUSES =
            Collections.unmodifiableList(Arrays.asList(Identifiable.DEFAULT_STATUS, "COMPLETED", "DROPPED"));
    private static final List<String> SEASONS =
            Collections.unmodifiableList(Arrays.asList("FALL", "SPRING", "SUMMER"));

    private String enrollmentId;
    private Student student;
    private Course course;
    private LocalDate enrollmentDate;
    private String semester;
    private String enrollmentStatus;

    public Enrollment(String enrollmentId, Student student, Course course,
                      LocalDate enrollmentDate, String semester, String enrollmentStatus) {
        this.enrollmentId = enrollmentId;
        this.student = student;
        this.course = course;
        this.enrollmentDate = enrollmentDate;
        this.semester = semester;
        this.enrollmentStatus = enrollmentStatus;
    }

    public static List<String> allowedStatuses() { return STATUSES; }

    /** Accepts "FALL 2026", "spring 2027"... and returns the normalised upper-case form. */
    public static String normalizeSemester(String raw) throws ValidationException {
        String message = "Semester must be a season (" + SEASONS + ") followed by a year, e.g. FALL 2026.";
        if (raw == null || raw.trim().isEmpty()) {
            throw new ValidationException("Semester is required and cannot be blank.");
        }
        String[] parts = raw.trim().toUpperCase(Locale.ROOT).split("\\s+");
        if (parts.length != 2 || !SEASONS.contains(parts[0]) || !parts[1].matches("\\d{4}")) {
            throw new ValidationException(message);
        }
        int year = Integer.parseInt(parts[1]);
        if (year < 2000 || year > 2100) {
            throw new ValidationException("Semester year must be between 2000 and 2100.");
        }
        return parts[0] + " " + parts[1];
    }

    @Override
    public String getId() { return enrollmentId; }

    @Override
    public String getDisplayName() {
        return (student == null ? "?" : student.getDisplayName()) + " in "
                + (course == null ? "?" : course.getCourseCode()) + " (" + semester + ")";
    }

    @Override
    public boolean isActive() { return Identifiable.DEFAULT_STATUS.equals(enrollmentStatus); }

    @Override
    public void validate() throws ValidationException {
        Registrable.requireId("Enrollment ID", enrollmentId);
        Registrable.requireNotNull("Student", student);
        Registrable.requireNotNull("Course", course);
        Registrable.requireNotFuture("Enrollment date", enrollmentDate);
        if (semester == null || !semester.equals(normalizeSemester(semester))) {
            throw new ValidationException("Semester must look like FALL 2026 (season + year).");
        }
        if (enrollmentStatus == null || !STATUSES.contains(enrollmentStatus)) {
            throw new ValidationException("Enrollment status must be one of " + STATUSES + ".");
        }
    }

    @Override
    public void printDetails() {
        System.out.println("  Enrollment ID       : " + enrollmentId);
        System.out.println("  Student             : " + (student == null ? "(none)"
                : "[" + student.getId() + "] " + student.getDisplayName()));
        System.out.println("  Course              : " + (course == null ? "(none)"
                : "[" + course.getId() + "] " + course.getDisplayName()));
        System.out.println("  Enrollment date     : " + enrollmentDate);
        System.out.println("  Semester            : " + semester);
        System.out.println("  Enrollment status   : " + enrollmentStatus);
    }

    @Override
    public String getRegistrationSummary() {
        return "[" + enrollmentId + "] " + getDisplayName() + " - " + enrollmentStatus;
    }

    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String enrollmentId) { this.enrollmentId = enrollmentId; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getEnrollmentStatus() { return enrollmentStatus; }
    public void setEnrollmentStatus(String enrollmentStatus) { this.enrollmentStatus = enrollmentStatus; }

    @Override
    public String toString() {
        return "Enrollment{id='" + enrollmentId + "', " + getDisplayName() + ", status=" + enrollmentStatus + "}";
    }
}
