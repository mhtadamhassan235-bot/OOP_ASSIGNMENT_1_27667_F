package com.auca.assessment;

import com.auca.enrollment.Enrollment;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class Assessment implements Identifiable, Registrable {

    private static final List<String> TYPES = Collections.unmodifiableList(
            Arrays.asList("QUIZ", "ASSIGNMENT", "MIDTERM", "FINAL_EXAM", "PROJECT"));

    private String assessmentId;
    private Enrollment enrollment;
    private String assessmentType;
    private double maximumScore;
    private double obtainedScore;
    private LocalDate assessmentDate;

    public Assessment(String assessmentId, Enrollment enrollment, String assessmentType,
                      double maximumScore, double obtainedScore, LocalDate assessmentDate) {
        this.assessmentId = assessmentId;
        this.enrollment = enrollment;
        this.assessmentType = assessmentType;
        this.maximumScore = maximumScore;
        this.obtainedScore = obtainedScore;
        this.assessmentDate = assessmentDate;
    }

    public static List<String> allowedTypes() { return TYPES; }

   
    public double getPercentage() {
        return maximumScore > 0 ? obtainedScore * 100.0 / maximumScore : 0.0;
    }

    @Override
    public String getId() { return assessmentId; }

    @Override
    public String getDisplayName() {
        return assessmentType + " " + obtainedScore + "/" + maximumScore;
    }

    @Override
    public boolean isActive() { return enrollment != null && enrollment.isActive(); }

    @Override
    public void validate() throws ValidationException {
        Registrable.requireId("Assessment ID", assessmentId);
        Registrable.requireNotNull("Enrollment", enrollment);
        if (assessmentType == null || !TYPES.contains(assessmentType)) {
            throw new ValidationException("Assessment type must be one of " + TYPES + ".");
        }
        if (Double.isNaN(maximumScore) || Double.isInfinite(maximumScore) || maximumScore <= 0) {
            throw new ValidationException("Maximum score must be greater than zero.");
        }
        if (Double.isNaN(obtainedScore) || obtainedScore < 0 || obtainedScore > maximumScore) {
            throw new ValidationException("Obtained score must be between 0 and the maximum score (" + maximumScore + ").");
        }
        Registrable.requireNotFuture("Assessment date", assessmentDate);
    }

    @Override
    public void printDetails() {
        System.out.println("  Assessment ID       : " + assessmentId);
        System.out.println("  Enrollment          : " + (enrollment == null ? "(none)"
                : "[" + enrollment.getId() + "] " + enrollment.getDisplayName()));
        System.out.println("  Assessment type     : " + assessmentType);
        System.out.println("  Maximum score       : " + maximumScore);
        System.out.println("  Obtained score      : " + obtainedScore + String.format(" (%.1f%%)", getPercentage()));
        System.out.println("  Assessment date     : " + assessmentDate);
    }

    @Override
    public String getRegistrationSummary() {
        return "[" + assessmentId + "] " + assessmentType + " - " + obtainedScore + "/" + maximumScore
                + String.format(" (%.1f%%)", getPercentage()) + " - "
                + (enrollment == null ? "?" : enrollment.getDisplayName());
    }

    public String getAssessmentId() { return assessmentId; }
    public void setAssessmentId(String assessmentId) { this.assessmentId = assessmentId; }
    public Enrollment getEnrollment() { return enrollment; }
    public void setEnrollment(Enrollment enrollment) { this.enrollment = enrollment; }
    public String getAssessmentType() { return assessmentType; }
    public void setAssessmentType(String assessmentType) { this.assessmentType = assessmentType; }
    public double getMaximumScore() { return maximumScore; }
    public void setMaximumScore(double maximumScore) { this.maximumScore = maximumScore; }
    public double getObtainedScore() { return obtainedScore; }
    public void setObtainedScore(double obtainedScore) { this.obtainedScore = obtainedScore; }
    public LocalDate getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(LocalDate assessmentDate) { this.assessmentDate = assessmentDate; }

    @Override
    public String toString() {
        return "Assessment{id='" + assessmentId + "', type=" + assessmentType + ", score=" + obtainedScore
                + "/" + maximumScore + ", date=" + assessmentDate + "}";
    }
}
