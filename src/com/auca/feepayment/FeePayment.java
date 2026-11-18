package com.auca.feepayment;

import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;
import com.auca.student.Student;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** A tuition/fee payment made by a student. Money is always BigDecimal. */
public class FeePayment implements Identifiable, Registrable {

    private static final List<String> METHODS = Collections.unmodifiableList(
            Arrays.asList("CASH", "MOBILE_MONEY", "BANK_TRANSFER", "CARD"));

    private String paymentId;
    private Student student;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String receiptNumber;

    public FeePayment(String paymentId, Student student, BigDecimal amount,
                      LocalDate paymentDate, String paymentMethod, String receiptNumber) {
        this.paymentId = paymentId;
        this.student = student;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.receiptNumber = receiptNumber;
    }

    public static List<String> allowedMethods() { return METHODS; }

    @Override
    public String getId() { return paymentId; }

    @Override
    public String getDisplayName() {
        return "Receipt " + receiptNumber + " - " + (amount == null ? "?" : amount.toPlainString());
    }

    @Override
    public boolean isActive() { return true; }

    @Override
    public void validate() throws ValidationException {
        Registrable.requireId("Payment ID", paymentId);
        Registrable.requireNotNull("Student", student);
        Registrable.requireMoney("Amount", amount);
        Registrable.requireNotFuture("Payment date", paymentDate);
        if (paymentMethod == null || !METHODS.contains(paymentMethod)) {
            throw new ValidationException("Payment method must be one of " + METHODS + ".");
        }
        Registrable.requireCode("Receipt number", receiptNumber, 3, 30);
    }

    @Override
    public void printDetails() {
        System.out.println("  Payment ID          : " + paymentId);
        System.out.println("  Student             : " + (student == null ? "(none)"
                : "[" + student.getId() + "] " + student.getDisplayName()));
        System.out.println("  Amount              : " + (amount == null ? "(none)" : amount.toPlainString()));
        System.out.println("  Payment date        : " + paymentDate);
        System.out.println("  Payment method      : " + paymentMethod);
        System.out.println("  Receipt number      : " + receiptNumber);
    }

    @Override
    public String getRegistrationSummary() {
        return "[" + paymentId + "] " + (student == null ? "?" : student.getDisplayName()) + " paid "
                + (amount == null ? "?" : amount.toPlainString()) + " on " + paymentDate + " by "
                + paymentMethod + " (receipt " + receiptNumber + ")";
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    @Override
    public String toString() {
        return "FeePayment{id='" + paymentId + "', amount=" + amount + ", date=" + paymentDate
                + ", method=" + paymentMethod + ", receipt='" + receiptNumber + "'}";
    }
}
