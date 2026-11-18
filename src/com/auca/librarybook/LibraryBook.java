package com.auca.librarybook;

import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;


public class LibraryBook implements Identifiable, Registrable {

    private String bookId;
    private String isbn;
    private String title;
    private String author;
    private int totalCopies;
    private int availableCopies;

    public LibraryBook(String bookId, String isbn, String title, String author,
                       int totalCopies, int availableCopies) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    /** Removes hyphens/spaces and checks the ISBN-10 / ISBN-13 shape; returns the normalised ISBN. */
    public static String normalizeIsbn(String raw) throws ValidationException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ValidationException("ISBN is required and cannot be blank.");
        }
        String digits = raw.replace("-", "").replace(" ", "");
        if (!digits.matches("\\d{9}[\\dXx]") && !digits.matches("\\d{13}")) {
            throw new ValidationException("ISBN must have 10 or 13 digits (hyphens allowed), e.g. 978-0-13-468599-1.");
        }
        return digits.toUpperCase();
    }

    @Override
    public String getId() { return bookId; }

    @Override
    public String getDisplayName() { return title; }

    @Override
    public boolean isActive() { return totalCopies > 0; }

    @Override
    public void validate() throws ValidationException {
        Registrable.requireId("Book ID", bookId);
        normalizeIsbn(isbn);
        Registrable.requireText("Title", title, 2, 120);
        Registrable.requireText("Author", author, 2, 100);
        if (totalCopies < 0) {
            throw new ValidationException("Total copies cannot be negative.");
        }
        if (availableCopies < 0) {
            throw new ValidationException("Available copies cannot be negative.");
        }
        if (availableCopies > totalCopies) {
            throw new ValidationException("Available copies (" + availableCopies
                    + ") cannot exceed total copies (" + totalCopies + ").");
        }
    }

    @Override
    public void printDetails() {
        System.out.println("  Book ID             : " + bookId);
        System.out.println("  ISBN                : " + isbn);
        System.out.println("  Title               : " + title);
        System.out.println("  Author              : " + author);
        System.out.println("  Total copies        : " + totalCopies);
        System.out.println("  Available copies    : " + availableCopies);
    }

    @Override
    public String getRegistrationSummary() {
        return "[" + bookId + "] " + title + " by " + author + " (ISBN " + isbn + ") - "
                + availableCopies + "/" + totalCopies + " available";
    }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    @Override
    public String toString() {
        return "LibraryBook{id='" + bookId + "', isbn='" + isbn + "', title='" + title + "', copies="
                + availableCopies + "/" + totalCopies + "}";
    }
}
