package com.auca.main;

import com.auca.abstracts.AcademicMember;
import com.auca.abstracts.Person;
import com.auca.administrator.Administrator;
import com.auca.assessment.Assessment;
import com.auca.bookloan.BookLoan;
import com.auca.classroom.Classroom;
import com.auca.course.Course;
import com.auca.department.Department;
import com.auca.enrollment.Enrollment;
import com.auca.exceptions.BookUnavailableException;
import com.auca.exceptions.DuplicateRecordException;
import com.auca.exceptions.PrerequisiteMissingException;
import com.auca.exceptions.RecordNotFoundException;
import com.auca.exceptions.ValidationException;
import com.auca.feepayment.FeePayment;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;
import com.auca.librarybook.LibraryBook;
import com.auca.student.Student;
import com.auca.teacher.Teacher;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Main {

    private static final Scanner SCANNER = new Scanner(System.in);

    private static final List<Department> DEPARTMENTS = new ArrayList<>();
    private static final List<Student> STUDENTS = new ArrayList<>();
    private static final List<Teacher> TEACHERS = new ArrayList<>();
    private static final List<Administrator> ADMINISTRATORS = new ArrayList<>();
    private static final List<Course> COURSES = new ArrayList<>();
    private static final List<Classroom> CLASSROOMS = new ArrayList<>();
    private static final List<Enrollment> ENROLLMENTS = new ArrayList<>();
    private static final List<Assessment> ASSESSMENTS = new ArrayList<>();
    private static final List<FeePayment> PAYMENTS = new ArrayList<>();
    private static final List<LibraryBook> BOOKS = new ArrayList<>();
    private static final List<BookLoan> LOANS = new ArrayList<>();

    private static final int MAX_NESTING = 5;  
    private static int nesting = 0;

    private static final int W = 60;          

    private static final int ANY = 0;
    private static final int PAST = 1;
    private static final int NOT_FUTURE = 2;


    private static final class Signal extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final boolean cancel;

        Signal(boolean cancel) {
            super(cancel ? "CANCEL" : "BACK", null, false, false);
            this.cancel = cancel;
        }
    }

    private static final class InputClosedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        InputClosedException() {
            super("Input closed");
        }
    }


    @FunctionalInterface private interface Parser { Object parse(Form f, String raw) throws Exception; }
    @FunctionalInterface private interface Chooser { Object choose(Form f) throws Exception; }
    @FunctionalInterface private interface Rule { void check(Form f) throws Exception; }
    @FunctionalInterface private interface Taken { boolean test(Form f, String value); }
    @FunctionalInterface private interface Builder { Object build(Form f) throws Exception; }
    @FunctionalInterface private interface Storer { void store(Object created, Form f) throws Exception; }

 
    private static final class Field {
        final int no;
        final String label;
        final String hint;
        final int step;
        Parser parser;
        Chooser chooser;
        Taken taken;
        Function<Object, Object> getter;
        BiConsumer<Object, Object> setter;
        boolean isDerived;
        boolean isFixed;
        String nullText = "(none)";
        Object value;
        Object original;
        boolean filled;

        Field(int no, String label, String hint, int step) {
            this.no = no;
            this.label = label;
            this.hint = hint;
            this.step = step;
        }

        Field unique(Taken t) { this.taken = t; return this; }
        Field fixed() { this.isFixed = true; return this; }
        Field derived() { this.isDerived = true; return this; }
        Field none(String text) { this.nullText = text; return this; }

        <T> Field gs(Class<T> type, Function<T, Object> g, BiConsumer<T, Object> s) {
            this.getter = o -> g.apply(type.cast(o));
            this.setter = (o, v) -> s.accept(type.cast(o), v);
            return this;
        }

        <T> Field getOnly(Class<T> type, Function<T, Object> g) {
            this.getter = o -> g.apply(type.cast(o));
            return this;
        }
    }

    /** Full description of one entity form. */
    private static final class Spec {
        final String entity;
        final String title;
        final String[] steps;
        final List<Field> fields = new ArrayList<>();
        final List<Rule> rules = new ArrayList<>();
        Builder builder;
        Storer storer;

        Spec(String entity, String title, String... steps) {
            this.entity = entity;
            this.title = title;
            this.steps = steps;
        }

        Spec add(Field f) {
            if (f.no != fields.size() + 1) {
                throw new IllegalStateException("Field numbering error at " + f.label);
            }
            fields.add(f);
            return this;
        }
    }

   
    private static final class Form {
        final Spec spec;
        final Object editing;   

        Form(Spec spec, Object editing) {
            this.spec = spec;
            this.editing = editing;
        }

        Field field(int no) { return spec.fields.get(no - 1); }
        @SuppressWarnings("unchecked") <T> T val(int no) { return (T) field(no).value; }
        String str(int no) { return (String) field(no).value; }
        LocalDate date(int no) { return (LocalDate) field(no).value; }
        int integer(int no) { return (Integer) field(no).value; }
        double dbl(int no) { return (Double) field(no).value; }
        boolean bool(int no) { return (Boolean) field(no).value; }
        BigDecimal money(int no) { return (BigDecimal) field(no).value; }
    }

    private static void out(String s) { System.out.println(s); }

    private static String rep(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    private static String pad(String s, int n) { return s.length() >= n ? s : s + rep(' ', n - s.length()); }
    private static String two(int n) { return String.format("%02d", n); }
    private static void edge() { out("+" + rep('-', W) + "+"); }
    private static void boxLine(String t) { out("| " + pad(t, W - 2) + " |"); }

    private static String titleBar(String title) {
        String t = " " + title + " ";
        int left = Math.max(0, (W - t.length()) / 2);
        int right = Math.max(0, W - t.length() - left);
        return "+" + rep('-', left) + t + rep('-', right) + "+";
    }

  
    private static String readRaw(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        if (!SCANNER.hasNextLine()) {
            throw new InputClosedException();
        }
        return SCANNER.nextLine().trim();
    }

    private static String ask(String prompt) {
        String s = readRaw(prompt);
        if (s.equalsIgnoreCase("CANCEL")) throw new Signal(true);
        if (s.equalsIgnoreCase("BACK")) throw new Signal(false);
        return s;
    }

    private static String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private static boolean confirm(String question) {
        while (true) {
            String a = readRaw(question + " (Y/N): ");
            if (a.equalsIgnoreCase("Y") || a.equalsIgnoreCase("YES")) return true;
            if (a.equalsIgnoreCase("N") || a.equalsIgnoreCase("NO")) return false;
            out("! Please answer Y or N.");
        }
    }


    private static String label(Object o) {
        if (o instanceof Department) {
            Department d = (Department) o;
            return "[" + d.getDepartmentId() + "] " + d.getDepartmentName() + " (" + d.getDepartmentCode() + ")";
        }
        if (o instanceof Student) {
            Student s = (Student) o;
            return "[" + s.getPersonId() + "] " + s.getDisplayName() + " - student no. " + s.getStudentNumber();
        }
        if (o instanceof Teacher) {
            Teacher t = (Teacher) o;
            return "[" + t.getPersonId() + "] " + t.getDisplayName() + " - " + t.getSpecialization()
                    + " (" + t.getDepartment().getDepartmentCode() + ")";
        }
        if (o instanceof Administrator) {
            Administrator a = (Administrator) o;
            return "[" + a.getPersonId() + "] " + a.getDisplayName() + " - " + a.getJobTitle();
        }
        if (o instanceof Course) {
            Course c = (Course) o;
            return "[" + c.getCourseId() + "] " + c.getCourseCode() + " - " + c.getCourseTitle();
        }
        if (o instanceof Classroom) {
            Classroom c = (Classroom) o;
            return "[" + c.getClassroomId() + "] " + c.getDisplayName() + " (capacity " + c.getCapacity() + ")";
        }
        if (o instanceof Enrollment) {
            Enrollment e = (Enrollment) o;
            return "[" + e.getEnrollmentId() + "] " + e.getStudent().getDisplayName() + " / "
                    + e.getCourse().getCourseCode() + " / " + e.getSemester() + " / " + e.getEnrollmentStatus();
        }
        if (o instanceof Assessment) {
            Assessment a = (Assessment) o;
            return "[" + a.getAssessmentId() + "] " + a.getAssessmentType() + " " + a.getObtainedScore() + "/" + a.getMaximumScore();
        }
        if (o instanceof FeePayment) {
            FeePayment p = (FeePayment) o;
            return "[" + p.getPaymentId() + "] " + p.getAmount().toPlainString() + " on " + p.getPaymentDate();
        }
        if (o instanceof LibraryBook) {
            LibraryBook b = (LibraryBook) o;
            return "[" + b.getBookId() + "] " + b.getTitle() + " by " + b.getAuthor() + " ("
                    + b.getAvailableCopies() + "/" + b.getTotalCopies() + " available)";
        }
        if (o instanceof BookLoan) {
            BookLoan l = (BookLoan) o;
            return "[" + l.getLoanId() + "] " + l.getBook().getTitle() + " -> " + l.getStudent().getDisplayName()
                    + " (due " + l.getDueDate() + ")";
        }
        if (o instanceof Identifiable) {
            return "[" + ((Identifiable) o).getId() + "] " + ((Identifiable) o).getDisplayName();
        }
        return String.valueOf(o);
    }

    private static String show(Object v, Field fld) {
        if (v == null) return fld == null ? "(none)" : fld.nullText;
        if (v instanceof Boolean) return ((Boolean) v) ? "YES" : "NO";
        if (v instanceof BigDecimal) return ((BigDecimal) v).toPlainString();
        if (v instanceof Identifiable) return label(v);
        return String.valueOf(v);
    }

    private static List<Person> allPersons() {
        List<Person> l = new ArrayList<>();
        l.addAll(STUDENTS);
        l.addAll(TEACHERS);
        l.addAll(ADMINISTRATORS);
        return l;
    }

    private static List<AcademicMember> allMembers() {
        List<AcademicMember> l = new ArrayList<>();
        l.addAll(STUDENTS);
        l.addAll(TEACHERS);
        return l;
    }

    private static List<Identifiable> allEntities() {
        List<Identifiable> l = new ArrayList<>();
        l.addAll(DEPARTMENTS);
        l.addAll(STUDENTS);
        l.addAll(TEACHERS);
        l.addAll(ADMINISTRATORS);
        l.addAll(COURSES);
        l.addAll(CLASSROOMS);
        l.addAll(ENROLLMENTS);
        l.addAll(ASSESSMENTS);
        l.addAll(PAYMENTS);
        l.addAll(BOOKS);
        l.addAll(LOANS);
        return l;
    }

    private static boolean isRegistered(Object o) {
        for (Identifiable i : allEntities()) {
            if (i == o) return true;
        }
        return false;
    }

    private static <T> boolean taken(Form f, List<T> list, Function<T, String> key, String v) {
        for (T t : list) {
            if (t != f.editing && v.equalsIgnoreCase(key.apply(t))) return true;
        }
        return false;
    }

    private static List<Enrollment> activeEnrollments() {
        List<Enrollment> l = new ArrayList<>();
        for (Enrollment e : ENROLLMENTS) if (e.isActive()) l.add(e);
        return l;
    }

    private static List<BookLoan> activeLoanList() {
        List<BookLoan> l = new ArrayList<>();
        for (BookLoan b : LOANS) if (!b.isReturned()) l.add(b);
        return l;
    }

    private static int activeLoansOf(LibraryBook book) {
        int n = 0;
        for (BookLoan b : LOANS) if (b.getBook() == book && !b.isReturned()) n++;
        return n;
    }

    private static boolean activeEnrollmentExists(Student s, Course c, String semester, Object exclude) {
        for (Enrollment e : ENROLLMENTS) {
            if (e != exclude && e.isActive() && e.getStudent() == s && e.getCourse() == c
                    && e.getSemester().equalsIgnoreCase(semester)) {
                return true;
            }
        }
        return false;
    }


    private static Parser pText(String label, int min, int max) {
        return (f, raw) -> Registrable.requireText(label, raw, min, max);
    }

    private static Parser pName(String label) {
        return (f, raw) -> Registrable.requireName(label, raw);
    }

    private static Parser pId(String label) {
        return (f, raw) -> Registrable.requireId(label, raw);
    }

    private static Parser pCode(String label, int min, int max) {
        return (f, raw) -> Registrable.requireCode(label, raw, min, max);
    }

    private static Parser pEmail(String label) {
        return (f, raw) -> Registrable.requireEmail(label, raw);
    }

    private static Parser pPhone(String label) {
        return (f, raw) -> Registrable.requirePhone(label, raw);
    }

    private static int parseInt(String label, String raw) throws ValidationException {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(label + " must be a whole number (got '" + raw + "').", e);
        }
    }

    private static double parseDouble(String label, String raw) throws ValidationException {
        try {
            double d = Double.parseDouble(raw.trim());
            if (Double.isNaN(d) || Double.isInfinite(d)) throw new NumberFormatException("not finite");
            return d;
        } catch (NumberFormatException e) {
            throw new ValidationException(label + " must be a valid number, e.g. 17.5 (got '" + raw + "').", e);
        }
    }

    private static Parser pInt(String label, int min, int max) {
        return (f, raw) -> {
            int v = parseInt(label, raw);
            Registrable.requireRange(label, v, min, max);
            return v;
        };
    }

    private static String choiceHint(List<String> options) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append(i + 1).append(' ').append(options.get(i));
        }
        return sb.toString();
    }

    private static Parser pChoice(String label, List<String> options) {
        return (f, raw) -> {
            String r = raw.trim();
            try {
                int k = Integer.parseInt(r);
                if (k >= 1 && k <= options.size()) return options.get(k - 1);
            } catch (NumberFormatException ignoredNotANumber) {
                // not a number: fall through and try to match the text of an option
                r = r.toUpperCase().replace(' ', '_');
            }
            for (String o : options) {
                if (o.equalsIgnoreCase(r)) return o;
            }
            throw new ValidationException(label + " must be one of: " + choiceHint(options) + ".");
        };
    }

    private static final List<String> YES_NO = java.util.Arrays.asList("YES", "NO");

    private static Parser pYesNo(String label) {
        Parser p = pChoice(label, YES_NO);
        return (f, raw) -> {
            String r = raw.trim().toUpperCase();
            if (r.equals("Y") || r.equals("TRUE")) r = "YES";
            if (r.equals("N") || r.equals("FALSE")) r = "NO";
            return "YES".equals(p.parse(f, r));
        };
    }

    private static Parser pMoney(String label) {
        return (f, raw) -> {
            BigDecimal b;
            try {
                b = new BigDecimal(raw.trim().replace(",", ""));
            } catch (NumberFormatException e) {
                throw new ValidationException(label + " must be a valid amount, e.g. 150.75 (got '" + raw + "').", e);
            }
            Registrable.requireMoney(label, b);
            return b.setScale(2, RoundingMode.HALF_UP);
        };
    }


    private static Parser pDate(String label, int rule, int afterField) {
        return (f, raw) -> {
            LocalDate d = raw.trim().equalsIgnoreCase("TODAY") ? LocalDate.now() : Registrable.parseDate(raw);
            if (rule == PAST) Registrable.requirePastDate(label, d);
            if (rule == NOT_FUTURE) Registrable.requireNotFuture(label, d);
            if (afterField > 0) {
                LocalDate other = f.date(afterField);
                if (other != null && !d.isAfter(other)) {
                    throw new ValidationException(label + " must be after " + f.field(afterField).label.toLowerCase()
                            + " (" + other + ").");
                }
            }
            return d;
        };
    }

    private static Field fld(int no, String label, String hint, int step, Parser p) {
        Field f = new Field(no, label, hint, step);
        f.parser = p;
        return f;
    }

    private static Field sel(int no, String label, String hint, int step, Chooser c) {
        Field f = new Field(no, label, hint, step);
        f.chooser = c;
        return f;
    }


    private static String need(String noun, String purpose) {
        return "No " + noun + " exists. Register a " + noun.toLowerCase() + " before " + purpose + ".";
    }

    private static Chooser selector(String noun, Supplier<List<?>> items, Supplier<Spec> nested,
                                    String purpose, String cancelText) {
        return f -> pick(noun, items, nested, false, need(noun, purpose), cancelText, null);
    }

    
    private static Object pick(String noun, Supplier<List<?>> items, Supplier<Spec> nested, boolean optional,
                               String emptyMessage, String cancelText, String noneText)
            throws PrerequisiteMissingException {
        while (true) {
            List<?> list = items.get();
            if (list.isEmpty() && !optional) {
                if (nested == null) {
                    throw new PrerequisiteMissingException(emptyMessage);
                }
                out("");
                out("! " + emptyMessage);
                out("  1  Register prerequisite now (" + noun + ")");
                out("  2  Return to menu (cancel " + cancelText + ")");
                String c = ask("Select option: ");
                if (c.equals("1")) {
                    runNested(nested);
                    continue;
                }
                if (c.equals("2") || c.equals("0")) {
                    throw new PrerequisiteMissingException(emptyMessage);
                }
                out("! Invalid option. Enter 1 or 2.");
                continue;
            }
            out(titleBar("CHOOSE " + noun.toUpperCase()));
            if (list.isEmpty()) {
                out("| (No " + noun + " is registered yet.)");
            }
            int i = 1;
            for (Object o : list) {
                out("| " + i++ + " " + label(o));
            }
            int reg = 0;
            int none = 0;
            int next = list.size() + 1;
            if (nested != null) {
                reg = next++;
                out("| " + reg + " [+] Register a new " + noun);
            }
            if (optional) {
                none = next++;
                out("| " + none + " " + noneText);
            }
            out("| 0 Cancel " + cancelText);
            out("+" + rep('-', W) + "+");
            String c = ask("Select option: ");
            int k;
            try {
                k = Integer.parseInt(c);
            } catch (NumberFormatException e) {
                out("! Enter a number from the list (you typed '" + c + "').");
                continue;
            }
            if (k == 0) throw new Signal(true);
            if (k >= 1 && k <= list.size()) return list.get(k - 1);
            if (reg != 0 && k == reg) {
                runNested(nested);
                continue;
            }
            if (none != 0 && k == none) return null;
            out("! Invalid selection. Choose a number between 0 and " + (next - 1) + ".");
        }
    }

    private static void runNested(Supplier<Spec> nested) {
        out("");
        out(">> Nested registration started (type CANCEL to return to the interrupted form).");
        Object created = createRecord(nested.get());
        out(created != null
                ? ">> Returning to the interrupted form..."
                : ">> Nested registration cancelled. Returning to the interrupted form...");
    }

    
    private static Object pickOrNull(String noun, Supplier<List<?>> items, Supplier<Spec> nested,
                                     String emptyMessage, String cancelText) {
        try {
            return pick(noun, items, nested, false, emptyMessage, cancelText, null);
        } catch (PrerequisiteMissingException e) {
            out("! " + e.getMessage() + " Nothing was changed.");
        } catch (Signal s) {
            out("Cancelled. Nothing was changed.");
        }
        return null;
    }

    private static void banner(Spec spec, int step) {
        String[] st = {spec.steps[0], spec.steps[1], spec.steps[2], "Review & confirm"};
        edge();
        boxLine("AUCA_ID / SCHOOL MANAGEMENT");
        String right = "Step " + step + " of 4";
        boxLine(pad(spec.title, W - 2 - right.length()) + right);
        edge();
        boxLine(pad("[1] " + st[0], 28) + "[2] " + st[1]);
        boxLine(pad("[3] " + st[2], 28) + "[4] " + st[3]);
        edge();
        boxLine("Enter the requested value. Type BACK to return;");
        boxLine("CANCEL to discard the unsaved " + spec.entity.toLowerCase() + ".");
        edge();
        if (step <= 3) {
            out("[STEP " + step + "/4] " + st[step - 1].toUpperCase());
        }
    }

    private static void fillField(Form f, Field fld) throws Exception {
        if (fld.chooser != null) {
            out(two(fld.no) + " " + fld.label + " [" + fld.hint + "]");
            fld.value = fld.chooser.choose(f);
            fld.filled = true;
            out("   Selected: " + show(fld.value, fld));
            return;
        }
        String prompt = String.format("%02d %-19s [%s] : ", fld.no, fld.label, fld.hint);
        if (fld.filled) {
            out("   Current value: " + show(fld.value, fld) + "   (press ENTER to keep it)");
        }
        while (true) {
            String raw = ask(prompt); 
            if (raw.isEmpty()) {
                if (fld.filled) {
                    out("   Kept: " + show(fld.value, fld));
                    return;
                }
                out("! " + fld.label + " is required and cannot be blank.");
                prompt = "   Please re-enter field " + two(fld.no) + " [" + fld.hint + "]: ";
                continue;
            }
            try {
                fld.value = fld.parser.parse(f, raw);
                fld.filled = true;
                return;
            } catch (DuplicateRecordException e) {
                out("! " + e.getMessage());
                prompt = "   Please enter a different " + fld.label.toLowerCase() + ": ";
            } catch (Signal s) {
                throw s;
            } catch (InputClosedException e) {
                throw e;
            } catch (Exception e) {
                out("! " + describe(e));
                prompt = "   Please re-enter field " + two(fld.no) + " [" + fld.hint + "]: ";
            }
        }
    }

    /** Walks the form, reviews, saves. Returns the saved record or null when cancelled. */
    private static Object createRecord(Spec spec) {
        if (nesting >= MAX_NESTING) {
            out("! Too many nested registrations. Finish or cancel the current one first.");
            return null;
        }
        nesting++;
        try {
            Form f = new Form(spec, null);
            int n = spec.fields.size();
            int i = 0;
            int dir = 1;
            int shownStep = 0;
            while (i < n) {
                Field fld = spec.fields.get(i);
                if (fld.isDerived) {
                    fld.value = null;
                    fld.filled = true;
                    i += dir;
                    if (i < 0) { i = 0; dir = 1; }
                    continue;
                }
                if (fld.step != shownStep) {
                    banner(spec, fld.step);
                    out("[ENTER] keeps a shown value   [BACK] previous field   [CANCEL] exit");
                    shownStep = fld.step;
                }
                try {
                    fillField(f, fld);
                    i++;
                    dir = 1;
                } catch (Signal s) {
                    if (s.cancel) {
                        out("Registration cancelled. Nothing was saved.");
                        return null;
                    }
                    if (i == 0) {
                        out("   You are already at the first field.");
                    } else {
                        i--;
                        dir = -1;
                    }
                }
            }
            return reviewAndSave(f);
        } catch (PrerequisiteMissingException e) {
            out("Registration of " + spec.entity.toLowerCase() + " cancelled: prerequisite missing. Nothing was saved.");
            return null;
        } catch (InputClosedException e) {
            throw e;
        } catch (Exception e) {
            out("! Unexpected problem: " + describe(e) + ". Nothing was saved.");
            return null;
        } finally {
            nesting--;
        }
    }

    private static void printReview(Form f, String title, boolean update) {
        int lw = 0;
        for (Field x : f.spec.fields) lw = Math.max(lw, x.label.length());
        out(titleBar(title));
        int shown = 0;
        for (Field x : f.spec.fields) {
            if (x.step != shown) {
                shown = x.step;
                out("| " + f.spec.steps[shown - 1].toUpperCase());
            }
            String mark = "";
            if (update) {
                if (x.isFixed || x.isDerived) mark = "   (locked)";
                else if (!Objects.equals(x.value, x.original)) mark = "   *changed*";
            }
            out(String.format("|   %02d %-" + lw + "s : %s%s", x.no, x.label, show(x.value, x), mark));
        }
        out("+" + rep('-', W) + "+");
    }

    private static void editField(Form f, boolean update) throws Exception {
        int n = f.spec.fields.size();
        String raw = ask("Field number to edit (01-" + two(n) + "): ");
        int no;
        try {
            no = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            out("! Enter a field number between 01 and " + two(n) + ".");
            return;
        }
        if (no < 1 || no > n) {
            out("! Field number must be between 01 and " + two(n) + ".");
            return;
        }
        Field fld = f.field(no);
        if (fld.isDerived || (update && fld.isFixed)) {
            out("! Field " + two(no) + " (" + fld.label + ") cannot be edited.");
            return;
        }
        out("   Old value: " + show(fld.value, fld));
        fillField(f, fld);
    }

    private static Object reviewAndSave(Form f) throws Exception {
        banner(f.spec, 4);
        while (true) {
            printReview(f, "REVIEW " + f.spec.entity.toUpperCase(), false);
            out("| [1] Save " + f.spec.entity.toLowerCase() + "   [2] Edit field   [0] Cancel");
            String c;
            try {
                c = ask("Select option: ");
            } catch (Signal s) {
                if (s.cancel) {
                    out("Registration cancelled. Nothing was saved.");
                    return null;
                }
                continue;
            }
            if (c.equals("1")) {
                try {
                    Object saved = commitCreate(f);
                    out("+ " + f.spec.entity + " " + ((Identifiable) saved).getId() + " registered successfully.");
                    return saved;
                } catch (PrerequisiteMissingException e) {
                    throw e;
                } catch (InputClosedException e) {
                    throw e;
                } catch (Exception e) {
                    out("! Cannot save: " + describe(e));
                    out("  Nothing was saved. Use option 2 to correct the field concerned.");
                }
            } else if (c.equals("2")) {
                try {
                    editField(f, false);
                } catch (Signal s) {
                    out("   Edit abandoned; the previous value is kept.");
                }
            } else if (c.equals("0")) {
                out("Registration cancelled. Nothing was saved.");
                return null;
            } else {
                out("! Invalid option. Enter 1, 2 or 0.");
            }
        }
    }

    private static void checkFields(Form f) throws Exception {
        for (Field x : f.spec.fields) {
            if (x.taken != null && x.value instanceof String && x.taken.test(f, (String) x.value)) {
                throw new DuplicateRecordException(x.label + " " + x.value + " already exists.");
            }
            if (x.value instanceof Identifiable && !isRegistered(x.value)) {
                throw new RecordNotFoundException(x.label + " " + label(x.value) + " is no longer registered.");
            }
        }
    }


    private static Object commitCreate(Form f) throws Exception {
        checkFields(f);
        Object o = f.spec.builder.build(f);
        ((Registrable) o).validate();
        for (Rule r : f.spec.rules) r.check(f);
        f.spec.storer.store(o, f);
        return o;
    }


    private static void updateRecord(Spec spec, Object target) {
        Form f = new Form(spec, target);
        for (Field x : spec.fields) {
            x.value = x.getter != null ? x.getter.apply(target) : x.value;
            x.original = x.value;
            x.filled = true;
        }
        try {
            while (true) {
                printReview(f, "UPDATE " + spec.entity.toUpperCase(), true);
                out("| [1] Save changes   [2] Edit field   [0] Cancel (discard)");
                String c;
                try {
                    c = ask("Select option: ");
                } catch (Signal s) {
                    if (s.cancel) {
                        out("Update cancelled. The record is unchanged.");
                        return;
                    }
                    continue;
                }
                if (c.equals("1")) {
                    if (!confirm("Apply these changes to " + label(target) + "?")) {
                        out("Changes not applied.");
                        continue;
                    }
                    try {
                        commitUpdate(f);
                        out("+ " + spec.entity + " " + ((Identifiable) target).getId() + " updated successfully.");
                        return;
                    } catch (InputClosedException e) {
                        throw e;
                    } catch (Exception e) {
                        out("! Cannot save changes: " + describe(e));
                        out("  The record was left unchanged. Use option 2 to correct the field concerned.");
                    }
                } else if (c.equals("2")) {
                    try {
                        editField(f, true);
                    } catch (Signal s) {
                        out("   Edit abandoned; the previous value is kept.");
                    } catch (PrerequisiteMissingException e) {
                        out("! " + e.getMessage());
                    }
                } else if (c.equals("0")) {
                    out("Update cancelled. The record is unchanged.");
                    return;
                } else {
                    out("! Invalid option. Enter 1, 2 or 0.");
                }
            }
        } catch (Exception e) {
            if (e instanceof InputClosedException) throw (InputClosedException) e;
            out("! Unexpected problem: " + describe(e) + ". The record is unchanged.");
        }
    }

    private static void commitUpdate(Form f) throws Exception {
        checkFields(f);
        List<Field> settable = new ArrayList<>();
        List<Object> oldValues = new ArrayList<>();
        for (Field x : f.spec.fields) {
            if (x.setter != null && !x.isFixed && !x.isDerived) {
                settable.add(x);
                oldValues.add(x.getter.apply(f.editing));
            }
        }
        try {
            for (Field x : settable) x.setter.accept(f.editing, x.value);
            ((Registrable) f.editing).validate();
            for (Rule r : f.spec.rules) r.check(f);
        } catch (Exception e) {
            for (int i = 0; i < settable.size(); i++) settable.get(i).setter.accept(f.editing, oldValues.get(i));
            throw e;
        }
    }


    private static final String H_ID = "String | unique | 4-30 chars";
    private static final String H_DATE = "LocalDate | YYYY-MM-DD or TODAY";

    private static Chooser departmentSelector(String purpose, String cancelText) {
        return selector("Department", () -> DEPARTMENTS, Main::departmentSpec, purpose, cancelText);
    }

    private static void addPersonFields(Spec s) {
        s.add(fld(1, "Person ID", H_ID, 1, pId("Person ID"))
                .unique((f, v) -> taken(f, allPersons(), Person::getPersonId, v))
                .gs(Person.class, Person::getPersonId, (o, v) -> o.setPersonId((String) v)));
        s.add(fld(2, "First name", "String | 2-60 chars", 1, pName("First name"))
                .gs(Person.class, Person::getFirstName, (o, v) -> o.setFirstName((String) v)));
        s.add(fld(3, "Last name", "String | 2-60 chars", 1, pName("Last name"))
                .gs(Person.class, Person::getLastName, (o, v) -> o.setLastName((String) v)));
        s.add(fld(4, "Email", "String | unique | email", 1, pEmail("Email"))
                .unique((f, v) -> taken(f, allPersons(), Person::getEmail, v))
                .gs(Person.class, Person::getEmail, (o, v) -> o.setEmail((String) v)));
        s.add(fld(5, "Phone", "String | 10-15 digits, optional +", 1, pPhone("Phone"))
                .gs(Person.class, Person::getPhoneNumber, (o, v) -> o.setPhoneNumber((String) v)));
        s.add(fld(6, "Date of birth", "LocalDate | YYYY-MM-DD", 1, pDate("Date of birth", PAST, 0))
                .gs(Person.class, Person::getDateOfBirth, (o, v) -> o.setDateOfBirth((LocalDate) v)));
    }

    private static void addAcademicFields(Spec s, String purpose, String cancelText) {
        s.add(fld(7, "Member number", "String | unique | 4-30", 2, pId("Member number"))
                .unique((f, v) -> taken(f, allMembers(), AcademicMember::getMemberNumber, v))
                .gs(AcademicMember.class, AcademicMember::getMemberNumber, (o, v) -> o.setMemberNumber((String) v)));
        s.add(sel(8, "Department", "Department | select", 2, departmentSelector(purpose, cancelText))
                .gs(AcademicMember.class, AcademicMember::getDepartment, (o, v) -> o.setDepartment((Department) v)));
        s.add(fld(9, "Join date", H_DATE, 2, pDate("Join date", NOT_FUTURE, 6))
                .gs(AcademicMember.class, AcademicMember::getJoinDate, (o, v) -> o.setJoinDate((LocalDate) v)));
        s.add(fld(10, "Academic status", choiceHint(AcademicMember.allowedStatuses()), 2,
                pChoice("Academic status", AcademicMember.allowedStatuses()))
                .gs(AcademicMember.class, AcademicMember::getAcademicStatus, (o, v) -> o.setAcademicStatus((String) v)));
        s.add(fld(11, "Institutional email", "String | unique | email", 2, pEmail("Institutional email"))
                .unique((f, v) -> taken(f, allMembers(), AcademicMember::getInstitutionalEmail, v))
                .gs(AcademicMember.class, AcademicMember::getInstitutionalEmail, (o, v) -> o.setInstitutionalEmail((String) v)));
        s.add(fld(12, "Advisor name", "String | 2-120 chars", 2, pText("Advisor name", 2, 120))
                .gs(AcademicMember.class, AcademicMember::getAdvisorName, (o, v) -> o.setAdvisorName((String) v)));
    }

    private static Spec departmentSpec() {
        Spec s = new Spec("Department", "DEPARTMENT REGISTRATION", "Identification", "Department details", "Contact & leadership");
        s.add(fld(1, "Department ID", H_ID, 1, pId("Department ID"))
                .unique((f, v) -> taken(f, DEPARTMENTS, Department::getDepartmentId, v))
                .gs(Department.class, Department::getDepartmentId, (o, v) -> o.setDepartmentId((String) v)));
        s.add(fld(2, "Department name", "String | unique | 2-60 chars", 1, pText("Department name", 2, 60))
                .unique((f, v) -> taken(f, DEPARTMENTS, Department::getDepartmentName, v))
                .gs(Department.class, Department::getDepartmentName, (o, v) -> o.setDepartmentName((String) v)));
        s.add(fld(3, "Department code", "String | unique | 2-10 chars", 2, pCode("Department code", 2, 10))
                .unique((f, v) -> taken(f, DEPARTMENTS, Department::getDepartmentCode, v))
                .gs(Department.class, Department::getDepartmentCode, (o, v) -> o.setDepartmentCode((String) v)));
        s.add(fld(4, "Office location", "String | 2-60 chars", 2, pText("Office location", 2, 60))
                .gs(Department.class, Department::getOfficeLocation, (o, v) -> o.setOfficeLocation((String) v)));
        s.add(fld(5, "Contact email", "String | email", 3, pEmail("Contact email"))
                .gs(Department.class, Department::getContactEmail, (o, v) -> o.setContactEmail((String) v)));
        s.add(sel(6, "Head teacher", "Teacher | optional, can be assigned later", 3,
                f -> pick("Teacher", () -> TEACHERS, null, true, "", "department registration",
                        "No head teacher yet (assign later)"))
                .none("(none assigned)")
                .gs(Department.class, Department::getHeadTeacher, (o, v) -> o.setHeadTeacher((Teacher) v)));
        s.builder = f -> new Department(f.str(1), f.str(2), f.str(3), f.str(4), f.str(5), f.<Teacher>val(6));
        s.storer = (o, f) -> DEPARTMENTS.add((Department) o);
        return s;
    }

    private static Spec studentSpec() {
        Spec s = new Spec("Student", "STUDENT REGISTRATION", "Personal details", "Academic details", "Student details");
        addPersonFields(s);
        addAcademicFields(s, "registering a student", "student registration");
        s.add(fld(13, "Student number", "String | unique | 4-30", 3, pId("Student number"))
                .unique((f, v) -> taken(f, STUDENTS, Student::getStudentNumber, v))
                .gs(Student.class, Student::getStudentNumber, (o, v) -> o.setStudentNumber((String) v)));
        s.add(fld(14, "Program name", "String | 2-120 chars", 3, pText("Program name", 2, 120))
                .gs(Student.class, Student::getProgramName, (o, v) -> o.setProgramName((String) v)));
        s.add(fld(15, "Admission date", H_DATE, 3, pDate("Admission date", NOT_FUTURE, 6))
                .gs(Student.class, Student::getAdmissionDate, (o, v) -> o.setAdmissionDate((LocalDate) v)));
        s.add(fld(16, "Study year", "int | 1-6", 3, pInt("Study year", 1, 6))
                .gs(Student.class, Student::getStudyYear, (o, v) -> o.setStudyYear((Integer) v)));
        s.add(fld(17, "Guardian phone", "String | 10-15 digits, optional +", 3, pPhone("Guardian phone"))
                .gs(Student.class, Student::getGuardianPhone, (o, v) -> o.setGuardianPhone((String) v)));
        s.add(fld(18, "Scholarship", "1 YES / 2 NO", 3, pYesNo("Scholarship"))
                .gs(Student.class, Student::isScholarshipStatus, (o, v) -> o.setScholarshipStatus((Boolean) v)));
        s.builder = f -> new Student(f.str(1), f.str(2), f.str(3), f.str(4), f.str(5), f.date(6),
                f.str(7), f.<Department>val(8), f.date(9), f.str(10), f.str(11), f.str(12),
                f.str(13), f.str(14), f.date(15), f.integer(16), f.str(17), f.bool(18));
        s.storer = (o, f) -> STUDENTS.add((Student) o);
        return s;
    }

    private static Spec teacherSpec() {
        Spec s = new Spec("Teacher", "TEACHER REGISTRATION", "Personal details", "Academic details", "Teacher details");
        addPersonFields(s);
        addAcademicFields(s, "registering a teacher", "teacher registration");
        s.add(fld(13, "Employee number", "String | unique | 4-30", 3, pId("Employee number"))
                .unique((f, v) -> taken(f, TEACHERS, Teacher::getEmployeeNumber, v))
                .gs(Teacher.class, Teacher::getEmployeeNumber, (o, v) -> o.setEmployeeNumber((String) v)));
        s.add(fld(14, "Specialization", "String | 2-120 chars", 3, pText("Specialization", 2, 120))
                .gs(Teacher.class, Teacher::getSpecialization, (o, v) -> o.setSpecialization((String) v)));
        s.add(fld(15, "Hire date", H_DATE, 3, pDate("Hire date", NOT_FUTURE, 6))
                .gs(Teacher.class, Teacher::getHireDate, (o, v) -> o.setHireDate((LocalDate) v)));
        s.add(fld(16, "Salary", "BigDecimal | > 0, max 2 decimals", 3, pMoney("Salary"))
                .gs(Teacher.class, Teacher::getSalary, (o, v) -> o.setSalary((BigDecimal) v)));
        s.add(fld(17, "Office number", "String | 1-20 chars", 3, pText("Office number", 1, 20))
                .gs(Teacher.class, Teacher::getOfficeNumber, (o, v) -> o.setOfficeNumber((String) v)));
        s.add(fld(18, "Employment type", choiceHint(Teacher.allowedEmploymentTypes()), 3,
                pChoice("Employment type", Teacher.allowedEmploymentTypes()))
                .gs(Teacher.class, Teacher::getEmploymentType, (o, v) -> o.setEmploymentType((String) v)));
        s.builder = f -> new Teacher(f.str(1), f.str(2), f.str(3), f.str(4), f.str(5), f.date(6),
                f.str(7), f.<Department>val(8), f.date(9), f.str(10), f.str(11), f.str(12),
                f.str(13), f.str(14), f.date(15), f.money(16), f.str(17), f.str(18));
        s.storer = (o, f) -> TEACHERS.add((Teacher) o);
        return s;
    }

    private static Spec administratorSpec() {
        Spec s = new Spec("Administrator", "ADMINISTRATOR REGISTRATION", "Personal details", "Staff details", "Access & status");
        addPersonFields(s);
        s.add(fld(7, "Staff number", "String | unique | 4-30", 2, pId("Staff number"))
                .unique((f, v) -> taken(f, ADMINISTRATORS, Administrator::getStaffNumber, v))
                .gs(Administrator.class, Administrator::getStaffNumber, (o, v) -> o.setStaffNumber((String) v)));
        s.add(fld(8, "Job title", "String | 2-60 chars", 2, pText("Job title", 2, 60))
                .gs(Administrator.class, Administrator::getJobTitle, (o, v) -> o.setJobTitle((String) v)));
        s.add(fld(9, "Assigned office", "String | 1-30 chars", 2, pText("Assigned office", 1, 30))
                .gs(Administrator.class, Administrator::getAssignedOffice, (o, v) -> o.setAssignedOffice((String) v)));
        s.add(fld(10, "Employment date", H_DATE, 3, pDate("Employment date", NOT_FUTURE, 6))
                .gs(Administrator.class, Administrator::getEmploymentDate, (o, v) -> o.setEmploymentDate((LocalDate) v)));
        s.add(fld(11, "Access level", "int | 1-5", 3, pInt("Access level", 1, 5))
                .gs(Administrator.class, Administrator::getAccessLevel, (o, v) -> o.setAccessLevel((Integer) v)));
        s.add(fld(12, "Active", "1 YES / 2 NO", 3, pYesNo("Active"))
                .gs(Administrator.class, Administrator::isActive, (o, v) -> o.setActive((Boolean) v)));
        s.builder = f -> new Administrator(f.str(1), f.str(2), f.str(3), f.str(4), f.str(5), f.date(6),
                f.str(7), f.str(8), f.str(9), f.date(10), f.integer(11), f.bool(12));
        s.storer = (o, f) -> ADMINISTRATORS.add((Administrator) o);
        return s;
    }

    private static Spec courseSpec() {
        Spec s = new Spec("Course", "COURSE REGISTRATION", "Identification", "Course details", "Department & teacher");
        s.add(fld(1, "Course ID", H_ID, 1, pId("Course ID"))
                .unique((f, v) -> taken(f, COURSES, Course::getCourseId, v))
                .gs(Course.class, Course::getCourseId, (o, v) -> o.setCourseId((String) v)));
        s.add(fld(2, "Course code", "String | unique | 3-15 chars", 1, pCode("Course code", 3, 15))
                .unique((f, v) -> taken(f, COURSES, Course::getCourseCode, v))
                .gs(Course.class, Course::getCourseCode, (o, v) -> o.setCourseCode((String) v)));
        s.add(fld(3, "Course title", "String | 2-100 chars", 2, pText("Course title", 2, 100))
                .gs(Course.class, Course::getCourseTitle, (o, v) -> o.setCourseTitle((String) v)));
        s.add(fld(4, "Credit hours", "int | 1-6", 2, pInt("Credit hours", 1, 6))
                .gs(Course.class, Course::getCreditHours, (o, v) -> o.setCreditHours((Integer) v)));
        s.add(sel(5, "Department", "Department | select", 3, departmentSelector("registering a course", "course registration"))
                .gs(Course.class, Course::getDepartment, (o, v) -> o.setDepartment((Department) v)));
        s.add(sel(6, "Assigned teacher", "Teacher | select", 3,
                selector("Teacher", () -> TEACHERS, Main::teacherSpec, "registering a course", "course registration"))
                .gs(Course.class, Course::getAssignedTeacher, (o, v) -> o.setAssignedTeacher((Teacher) v)));
        s.builder = f -> new Course(f.str(1), f.str(2), f.str(3), f.integer(4), f.<Department>val(5), f.<Teacher>val(6));
        s.storer = (o, f) -> COURSES.add((Course) o);
        return s;
    }

    private static Spec classroomSpec() {
        Spec s = new Spec("Classroom", "CLASSROOM REGISTRATION", "Identification", "Location", "Capacity & availability");
        s.add(fld(1, "Classroom ID", H_ID, 1, pId("Classroom ID"))
                .unique((f, v) -> taken(f, CLASSROOMS, Classroom::getClassroomId, v))
                .gs(Classroom.class, Classroom::getClassroomId, (o, v) -> o.setClassroomId((String) v)));
        s.add(fld(2, "Building name", "String | 2-60 chars", 1, pText("Building name", 2, 60))
                .gs(Classroom.class, Classroom::getBuildingName, (o, v) -> o.setBuildingName((String) v)));
        s.add(fld(3, "Room number", "String | 1-10 chars", 2, pText("Room number", 1, 10))
                .gs(Classroom.class, Classroom::getRoomNumber, (o, v) -> o.setRoomNumber((String) v)));
        s.add(fld(4, "Capacity", "int | 1-1000", 2, pInt("Capacity", 1, 1000))
                .gs(Classroom.class, Classroom::getCapacity, (o, v) -> o.setCapacity((Integer) v)));
        s.add(fld(5, "Floor number", "int | 0-100", 3, pInt("Floor number", 0, 100))
                .gs(Classroom.class, Classroom::getFloorNumber, (o, v) -> o.setFloorNumber((Integer) v)));
        s.add(fld(6, "Available", "1 YES / 2 NO", 3, pYesNo("Available"))
                .gs(Classroom.class, Classroom::isAvailable, (o, v) -> o.setAvailable((Boolean) v)));
        s.rules.add(f -> {
            for (Classroom c : CLASSROOMS) {
                if (c != f.editing && c.getBuildingName().equalsIgnoreCase(f.str(2))
                        && c.getRoomNumber().equalsIgnoreCase(f.str(3))) {
                    throw new DuplicateRecordException("Room " + f.str(3) + " in " + f.str(2) + " is already registered.");
                }
            }
        });
        s.builder = f -> new Classroom(f.str(1), f.str(2), f.str(3), f.integer(4), f.integer(5), f.bool(6));
        s.storer = (o, f) -> CLASSROOMS.add((Classroom) o);
        return s;
    }

    private static Spec enrollmentSpec() {
        Spec s = new Spec("Enrollment", "STUDENT ENROLLMENT", "Identification", "Student & course", "Semester & status");
        s.add(fld(1, "Enrollment ID", H_ID, 1, pId("Enrollment ID"))
                .unique((f, v) -> taken(f, ENROLLMENTS, Enrollment::getEnrollmentId, v))
                .gs(Enrollment.class, Enrollment::getEnrollmentId, (o, v) -> o.setEnrollmentId((String) v)));
        s.add(sel(2, "Student", "Student | select", 2,
                selector("Student", () -> STUDENTS, Main::studentSpec, "enrolling a student", "enrollment"))
                .gs(Enrollment.class, Enrollment::getStudent, (o, v) -> o.setStudent((Student) v)));
        s.add(sel(3, "Course", "Course | select", 2,
                selector("Course", () -> COURSES, Main::courseSpec, "enrolling a student", "enrollment"))
                .gs(Enrollment.class, Enrollment::getCourse, (o, v) -> o.setCourse((Course) v)));
        s.add(fld(4, "Enrollment date", H_DATE, 3, pDate("Enrollment date", NOT_FUTURE, 0))
                .gs(Enrollment.class, Enrollment::getEnrollmentDate, (o, v) -> o.setEnrollmentDate((LocalDate) v)));
        s.add(fld(5, "Semester", "String | FALL/SPRING/SUMMER + year, e.g. FALL 2026", 3, (f, raw) -> {
            String sem = Enrollment.normalizeSemester(raw);
            Student st = f.val(2);
            Course c = f.val(3);
            if (activeEnrollmentExists(st, c, sem, f.editing)) {
                throw new DuplicateRecordException(st.getDisplayName() + " already has an ACTIVE enrollment in "
                        + c.getCourseCode() + " for " + sem + ". Enter a different semester.");
            }
            return sem;
        }).gs(Enrollment.class, Enrollment::getSemester, (o, v) -> o.setSemester((String) v)));
        s.add(fld(6, "Enrollment status", choiceHint(Enrollment.allowedStatuses()), 3,
                pChoice("Enrollment status", Enrollment.allowedStatuses()))
                .gs(Enrollment.class, Enrollment::getEnrollmentStatus, (o, v) -> o.setEnrollmentStatus((String) v)));
        s.rules.add(f -> {
            if ("ACTIVE".equals(f.str(6)) && activeEnrollmentExists(f.<Student>val(2), f.<Course>val(3), f.str(5), f.editing)) {
                throw new DuplicateRecordException("A duplicate ACTIVE enrollment exists for this student, course and semester.");
            }
        });
        s.builder = f -> new Enrollment(f.str(1), f.<Student>val(2), f.<Course>val(3), f.date(4), f.str(5), f.str(6));
        s.storer = (o, f) -> ENROLLMENTS.add((Enrollment) o);
        return s;
    }

    private static Spec assessmentSpec() {
        Spec s = new Spec("Assessment", "RECORD ASSESSMENT", "Identification", "Type & scores", "Date");
        s.add(fld(1, "Assessment ID", H_ID, 1, pId("Assessment ID"))
                .unique((f, v) -> taken(f, ASSESSMENTS, Assessment::getAssessmentId, v))
                .gs(Assessment.class, Assessment::getAssessmentId, (o, v) -> o.setAssessmentId((String) v)));
        s.add(sel(2, "Enrollment", "Enrollment | select an ACTIVE one", 1,
                f -> pick("Enrollment", Main::activeEnrollments, Main::enrollmentSpec, false,
                        "No active Enrollment exists. Enroll a student before recording an assessment.",
                        "assessment", null))
                .gs(Assessment.class, Assessment::getEnrollment, (o, v) -> o.setEnrollment((Enrollment) v)));
        s.add(fld(3, "Assessment type", choiceHint(Assessment.allowedTypes()), 2,
                pChoice("Assessment type", Assessment.allowedTypes()))
                .gs(Assessment.class, Assessment::getAssessmentType, (o, v) -> o.setAssessmentType((String) v)));
        s.add(fld(4, "Maximum score", "double | > 0", 2, (f, raw) -> {
            double d = parseDouble("Maximum score", raw);
            if (d <= 0) throw new ValidationException("Maximum score must be greater than zero.");
            if (d > 10000) throw new ValidationException("Maximum score cannot exceed 10000.");
            return d;
        }).gs(Assessment.class, Assessment::getMaximumScore, (o, v) -> o.setMaximumScore((Double) v)));
        s.add(fld(5, "Obtained score", "double | 0 to maximum score", 3, (f, raw) -> {
            double d = parseDouble("Obtained score", raw);
            double max = f.dbl(4);
            if (d < 0 || d > max) {
                throw new ValidationException("Obtained score must be between 0 and " + max + ".");
            }
            return d;
        }).gs(Assessment.class, Assessment::getObtainedScore, (o, v) -> o.setObtainedScore((Double) v)));
        s.add(fld(6, "Assessment date", H_DATE, 3, (f, raw) -> {
            LocalDate d = raw.trim().equalsIgnoreCase("TODAY") ? LocalDate.now() : Registrable.parseDate(raw);
            Registrable.requireNotFuture("Assessment date", d);
            Enrollment e = f.val(2);
            if (d.isBefore(e.getEnrollmentDate())) {
                throw new ValidationException("Assessment date cannot be before the enrollment date (" + e.getEnrollmentDate() + ").");
            }
            return d;
        }).gs(Assessment.class, Assessment::getAssessmentDate, (o, v) -> o.setAssessmentDate((LocalDate) v)));
        s.rules.add(f -> {
            Enrollment e = f.val(2);
            if (f.field(2).original != e && !e.isActive()) {
                throw new ValidationException("Assessments can only be recorded for an ACTIVE enrollment.");
            }
        });
        s.builder = f -> new Assessment(f.str(1), f.<Enrollment>val(2), f.str(3), f.dbl(4), f.dbl(5), f.date(6));
        s.storer = (o, f) -> ASSESSMENTS.add((Assessment) o);
        return s;
    }

    private static Spec feePaymentSpec() {
        Spec s = new Spec("Fee payment", "RECORD FEE PAYMENT", "Identification", "Amount & date", "Method & receipt");
        s.add(fld(1, "Payment ID", H_ID, 1, pId("Payment ID"))
                .unique((f, v) -> taken(f, PAYMENTS, FeePayment::getPaymentId, v))
                .gs(FeePayment.class, FeePayment::getPaymentId, (o, v) -> o.setPaymentId((String) v)));
        s.add(sel(2, "Student", "Student | select", 1,
                selector("Student", () -> STUDENTS, Main::studentSpec, "recording a payment", "fee payment"))
                .gs(FeePayment.class, FeePayment::getStudent, (o, v) -> o.setStudent((Student) v)));
        s.add(fld(3, "Amount", "BigDecimal | > 0, max 2 decimals", 2, pMoney("Amount"))
                .gs(FeePayment.class, FeePayment::getAmount, (o, v) -> o.setAmount((BigDecimal) v)));
        s.add(fld(4, "Payment date", H_DATE, 2, pDate("Payment date", NOT_FUTURE, 0))
                .gs(FeePayment.class, FeePayment::getPaymentDate, (o, v) -> o.setPaymentDate((LocalDate) v)));
        s.add(fld(5, "Payment method", choiceHint(FeePayment.allowedMethods()), 3,
                pChoice("Payment method", FeePayment.allowedMethods()))
                .gs(FeePayment.class, FeePayment::getPaymentMethod, (o, v) -> o.setPaymentMethod((String) v)));
        s.add(fld(6, "Receipt number", "String | unique | 3-30 chars", 3, pCode("Receipt number", 3, 30))
                .unique((f, v) -> taken(f, PAYMENTS, FeePayment::getReceiptNumber, v))
                .gs(FeePayment.class, FeePayment::getReceiptNumber, (o, v) -> o.setReceiptNumber((String) v)));
        s.builder = f -> new FeePayment(f.str(1), f.<Student>val(2), f.money(3), f.date(4), f.str(5), f.str(6));
        s.storer = (o, f) -> PAYMENTS.add((FeePayment) o);
        return s;
    }

    private static Spec libraryBookSpec() {
        Spec s = new Spec("Library book", "REGISTER LIBRARY BOOK", "Identification", "Title & author", "Stock");
        s.add(fld(1, "Book ID", H_ID, 1, pId("Book ID"))
                .unique((f, v) -> taken(f, BOOKS, LibraryBook::getBookId, v))
                .gs(LibraryBook.class, LibraryBook::getBookId, (o, v) -> o.setBookId((String) v)));
        s.add(fld(2, "ISBN", "String | unique | 10 or 13 digits", 1, (f, raw) -> LibraryBook.normalizeIsbn(raw))
                .unique((f, v) -> taken(f, BOOKS, LibraryBook::getIsbn, v))
                .gs(LibraryBook.class, LibraryBook::getIsbn, (o, v) -> o.setIsbn((String) v)));
        s.add(fld(3, "Title", "String | 2-120 chars", 2, pText("Title", 2, 120))
                .gs(LibraryBook.class, LibraryBook::getTitle, (o, v) -> o.setTitle((String) v)));
        s.add(fld(4, "Author", "String | 2-100 chars", 2, pText("Author", 2, 100))
                .gs(LibraryBook.class, LibraryBook::getAuthor, (o, v) -> o.setAuthor((String) v)));
        s.add(fld(5, "Total copies", "int | 0-10000", 3, pInt("Total copies", 0, 10000))
                .gs(LibraryBook.class, LibraryBook::getTotalCopies, (o, v) -> o.setTotalCopies((Integer) v)));
        s.add(fld(6, "Available copies", "int | 0 to total copies", 3, (f, raw) -> {
            int v = parseInt("Available copies", raw);
            if (v < 0) throw new ValidationException("Available copies cannot be negative.");
            if (v > f.integer(5)) {
                throw new ValidationException("Available copies (" + v + ") cannot exceed total copies (" + f.integer(5) + ").");
            }
            return v;
        }).gs(LibraryBook.class, LibraryBook::getAvailableCopies, (o, v) -> o.setAvailableCopies((Integer) v)));
        s.rules.add(f -> {
            if (f.editing != null) {
                int out = activeLoansOf((LibraryBook) f.editing);
                if (f.integer(5) - f.integer(6) < out) {
                    throw new ValidationException("Stock inconsistent: " + out + " copy(ies) are currently on loan, so at most "
                            + (f.integer(5) - out) + " can be available.");
                }
            }
        });
        s.builder = f -> new LibraryBook(f.str(1), f.str(2), f.str(3), f.str(4), f.integer(5), f.integer(6));
        s.storer = (o, f) -> BOOKS.add((LibraryBook) o);
        return s;
    }

    private static Spec bookLoanSpec() {
        Spec s = new Spec("Book loan", "BORROW LIBRARY BOOK", "Identification", "Student & book", "Dates");
        s.add(fld(1, "Loan ID", H_ID, 1, pId("Loan ID"))
                .unique((f, v) -> taken(f, LOANS, BookLoan::getLoanId, v))
                .gs(BookLoan.class, BookLoan::getLoanId, (o, v) -> o.setLoanId((String) v)));
        s.add(sel(2, "Student", "Student | select", 2,
                selector("Student", () -> STUDENTS, Main::studentSpec, "borrowing a book", "book loan"))
                .gs(BookLoan.class, BookLoan::getStudent, (o, v) -> o.setStudent((Student) v)));
        s.add(sel(3, "Library book", "LibraryBook | select (needs a free copy)", 2, f -> {
            while (true) {
                LibraryBook b = (LibraryBook) pick("Library book", () -> BOOKS, Main::libraryBookSpec, false,
                        need("Library book", "borrowing one"), "book loan", null);
                try {
                    if (b.getAvailableCopies() <= 0) {
                        throw new BookUnavailableException("No copy of '" + b.getTitle() + "' is available right now. Choose another book.");
                    }
                    return b;
                } catch (BookUnavailableException e) {
                    out("! " + e.getMessage());
                }
            }
        }).fixed().gs(BookLoan.class, BookLoan::getBook, (o, v) -> o.setBook((LibraryBook) v)));
        s.add(fld(4, "Borrowed date", H_DATE, 3, pDate("Borrowed date", NOT_FUTURE, 0))
                .gs(BookLoan.class, BookLoan::getBorrowedDate, (o, v) -> o.setBorrowedDate((LocalDate) v)));
        s.add(fld(5, "Due date", "LocalDate | YYYY-MM-DD, after borrowed date", 3, pDate("Due date", ANY, 4))
                .gs(BookLoan.class, BookLoan::getDueDate, (o, v) -> o.setDueDate((LocalDate) v)));
        s.add(sel(6, "Returned date", "LocalDate | derived: empty until the book is returned", 3, f -> null)
                .derived().none("(not returned yet)")
                .getOnly(BookLoan.class, BookLoan::getReturnedDate));
        s.rules.add(f -> {
            if (f.editing == null) {
                LibraryBook b = f.val(3);
                if (b.getAvailableCopies() <= 0) {
                    throw new BookUnavailableException("No copy of '" + b.getTitle() + "' is available.");
                }
            }
        });
        s.builder = f -> new BookLoan(f.str(1), f.<Student>val(2), f.<LibraryBook>val(3), f.date(4), f.date(5), null);
        // stock is decremented exactly once, only here, after every check succeeded
        s.storer = (o, f) -> {
            BookLoan loan = (BookLoan) o;
            LibraryBook b = loan.getBook();
            b.setAvailableCopies(b.getAvailableCopies() - 1);
            LOANS.add(loan);
        };
        return s;
    }

    private static void register(Supplier<Spec> factory) {
        while (true) {
            Object o = createRecord(factory.get());
            if (o == null) return;
            while (true) {
                out(" [1] Register another   [2] View " + factory.get().entity.toLowerCase() + "   [0] Main menu");
                String c = readRaw("Select option: ");
                if (c.equals("1")) break;
                if (c.equals("2")) {
                    out(titleBar("SAVED RECORD"));
                    ((Registrable) o).printDetails();
                } else if (c.equals("0")) {
                    return;
                } else {
                    out("! Invalid option. Enter 1, 2 or 0.");
                }
            }
        }
    }

    private static void returnBook() throws Exception {
        out(titleBar("RETURN LIBRARY BOOK"));
        BookLoan loan = (BookLoan) pickOrNull("Book loan", Main::activeLoanList, Main::bookLoanSpec,
                "No active loan exists. Borrow a book before returning one.", "return");
        if (loan == null) return;
        out("Loan to return:");
        loan.printDetails();
        LocalDate returned;
        while (true) {
            String raw = ask("Return date [LocalDate | YYYY-MM-DD or TODAY] : ");
            try {
                returned = raw.equalsIgnoreCase("TODAY") ? LocalDate.now() : Registrable.parseDate(raw);
                Registrable.requireNotFuture("Return date", returned);
                if (returned.isBefore(loan.getBorrowedDate())) {
                    throw new ValidationException("Return date cannot be before the borrowed date (" + loan.getBorrowedDate() + ").");
                }
                break;
            } catch (ValidationException e) {
                out("! " + e.getMessage());
            }
        }
        LibraryBook book = loan.getBook();
        if (book.getAvailableCopies() >= book.getTotalCopies()) {
            throw new ValidationException("Stock error: all " + book.getTotalCopies()
                    + " copies of '" + book.getTitle() + "' are already in the library. Nothing was changed.");
        }
        if (!confirm("Confirm return of '" + book.getTitle() + "' on " + returned + "?")) {
            out("Return cancelled. No changes were made.");
            return;
        }
        loan.setReturnedDate(returned);                                  // exactly one update of the loan
        book.setAvailableCopies(book.getAvailableCopies() + 1);          // exactly one stock increment
        out("+ Book returned. Available copies of '" + book.getTitle() + "': "
                + book.getAvailableCopies() + "/" + book.getTotalCopies() + ".");
        if (returned.isAfter(loan.getDueDate())) {
            out("  Note: returned " + ChronoUnit.DAYS.between(loan.getDueDate(), returned) + " day(s) after the due date.");
        }
    }

    private static void printList(String name, List<? extends Registrable> list) {
        out(titleBar(name.toUpperCase() + " (" + list.size() + ")"));
        if (list.isEmpty()) {
            out("  (no " + name.toLowerCase() + " registered yet)");
            return;
        }
        int i = 1;
        for (Registrable r : list) {
            out(String.format("%3d. %s", i++, r.getRegistrationSummary()));
        }
    }

    private static void viewAll() {
        while (true) {
            out(titleBar("VIEW ALL RECORDS"));
            out("  1 Departments   2 Students     3 Teachers      4 Administrators");
            out("  5 Courses       6 Classrooms   7 Enrollments   8 Assessments");
            out("  9 Fee payments 10 Library books 11 Book loans  12 Everything");
            out("  0 Back to main menu");
            String c = readRaw("Select option: ");
            int k;
            try {
                k = Integer.parseInt(c);
            } catch (NumberFormatException e) {
                out("! Enter a number between 0 and 12.");
                continue;
            }
            switch (k) {
                case 0: return;
                case 1: printList("Departments", DEPARTMENTS); break;
                case 2: printList("Students", STUDENTS); break;
                case 3: printList("Teachers", TEACHERS); break;
                case 4: printList("Administrators", ADMINISTRATORS); break;
                case 5: printList("Courses", COURSES); break;
                case 6: printList("Classrooms", CLASSROOMS); break;
                case 7: printList("Enrollments", ENROLLMENTS); break;
                case 8: printList("Assessments", ASSESSMENTS); break;
                case 9: printList("Fee payments", PAYMENTS); break;
                case 10: printList("Library books", BOOKS); break;
                case 11: printList("Book loans", LOANS); break;
                case 12:
                    printList("Departments", DEPARTMENTS);
                    printList("Students", STUDENTS);
                    printList("Teachers", TEACHERS);
                    printList("Administrators", ADMINISTRATORS);
                    printList("Courses", COURSES);
                    printList("Classrooms", CLASSROOMS);
                    printList("Enrollments", ENROLLMENTS);
                    printList("Assessments", ASSESSMENTS);
                    printList("Fee payments", PAYMENTS);
                    printList("Library books", BOOKS);
                    printList("Book loans", LOANS);
                    break;
                default: out("! Choose a number between 0 and 12.");
            }
        }
    }

    /** Alternative keys that also find a record (student number, course code, ISBN, receipt...). */
    private static List<String> keysOf(Object o) {
        List<String> k = new ArrayList<>();
        k.add(((Identifiable) o).getId());
        if (o instanceof AcademicMember) k.add(((AcademicMember) o).getMemberNumber());
        if (o instanceof Student) k.add(((Student) o).getStudentNumber());
        if (o instanceof Teacher) k.add(((Teacher) o).getEmployeeNumber());
        if (o instanceof Administrator) k.add(((Administrator) o).getStaffNumber());
        if (o instanceof Department) k.add(((Department) o).getDepartmentCode());
        if (o instanceof Course) k.add(((Course) o).getCourseCode());
        if (o instanceof LibraryBook) k.add(((LibraryBook) o).getIsbn());
        if (o instanceof FeePayment) k.add(((FeePayment) o).getReceiptNumber());
        return k;
    }

    private static void searchById() throws Exception {
        out(titleBar("SEARCH RECORD BY ID"));
        String id = ask("ID to search (record ID, student/employee/staff number, course or department code, ISBN, receipt) : ");
        if (id.isEmpty()) throw new ValidationException("Search text cannot be blank.");
        int found = 0;
        for (Identifiable i : allEntities()) {
            for (String key : keysOf(i)) {
                if (key != null && key.equalsIgnoreCase(id)) {
                    found++;
                    out("--- Match " + found + ": " + i.getClass().getSimpleName() + " ---");
                    ((Registrable) i).printDetails();
                    break;
                }
            }
        }
        if (found == 0) throw new RecordNotFoundException("No record found for '" + id + "'.");
    }

    private static void updateMenu() throws Exception {
        out(titleBar("UPDATE EXISTING RECORD"));
        out("  1 Department   2 Student     3 Teacher    4 Administrator  5 Course     6 Classroom");
        out("  7 Enrollment   8 Assessment  9 Fee payment 10 Library book 11 Book loan 0 Back");
        String c = readRaw("Which kind of record? ");
        int k;
        try {
            k = Integer.parseInt(c);
        } catch (NumberFormatException e) {
            throw new ValidationException("Enter a number between 0 and 11 (you typed '" + c + "').", e);
        }
        switch (k) {
            case 0: return;
            case 1: updateFrom(Main::departmentSpec, DEPARTMENTS, "Department"); break;
            case 2: updateFrom(Main::studentSpec, STUDENTS, "Student"); break;
            case 3: updateFrom(Main::teacherSpec, TEACHERS, "Teacher"); break;
            case 4: updateFrom(Main::administratorSpec, ADMINISTRATORS, "Administrator"); break;
            case 5: updateFrom(Main::courseSpec, COURSES, "Course"); break;
            case 6: updateFrom(Main::classroomSpec, CLASSROOMS, "Classroom"); break;
            case 7: updateFrom(Main::enrollmentSpec, ENROLLMENTS, "Enrollment"); break;
            case 8: updateFrom(Main::assessmentSpec, ASSESSMENTS, "Assessment"); break;
            case 9: updateFrom(Main::feePaymentSpec, PAYMENTS, "Fee payment"); break;
            case 10: updateFrom(Main::libraryBookSpec, BOOKS, "Library book"); break;
            case 11: updateFrom(Main::bookLoanSpec, LOANS, "Book loan"); break;
            default: throw new ValidationException("Choose a number between 0 and 11.");
        }
    }

    private static void updateFrom(Supplier<Spec> spec, List<?> list, String noun) {
        Object target = pickOrNull(noun, () -> list, null,
                "No " + noun + " exists, so there is nothing to update.", "update");
        if (target != null) updateRecord(spec.get(), target);
    }

    private static void academicSummary() {
        out(titleBar("STUDENT ACADEMIC SUMMARY"));
        Student st = (Student) pickOrNull("Student", () -> STUDENTS, Main::studentSpec,
                need("Student", "viewing an academic summary"), "summary");
        if (st == null) return;
        out("");
        out("Student     : " + st.getDisplayName() + " [" + st.getPersonId() + "], student no. " + st.getStudentNumber());
        out("Program     : " + st.getProgramName() + ", year " + st.getStudyYear()
                + ", scholarship: " + (st.isScholarshipStatus() ? "YES" : "NO"));
        out("Department  : " + label(st.getDepartment()) + "   Status: " + st.getAcademicStatus()
                + "   Advisor: " + st.getAdvisorName());
        out("");
        int credits = 0;
        int count = 0;
        double sumPct = 0;
        int scored = 0;
        for (Enrollment e : ENROLLMENTS) {
            if (e.getStudent() != st) continue;
            count++;
            Course c = e.getCourse();
            if (!"DROPPED".equals(e.getEnrollmentStatus())) credits += c.getCreditHours();
            out("- " + c.getCourseCode() + " " + c.getCourseTitle() + " (" + c.getCreditHours() + " cr) | "
                    + e.getSemester() + " | " + e.getEnrollmentStatus() + " | teacher: " + c.getAssignedTeacher().getDisplayName());
            double sum = 0;
            int n = 0;
            for (Assessment a : ASSESSMENTS) {
                if (a.getEnrollment() != e) continue;
                out(String.format("    * %-10s %6.2f / %-6.2f (%5.1f%%)  %s", a.getAssessmentType(),
                        a.getObtainedScore(), a.getMaximumScore(), a.getPercentage(), a.getAssessmentDate()));
                sum += a.getPercentage();
                n++;
            }
            if (n == 0) {
                out("    (no assessment recorded yet)");
            } else {
                out(String.format("    Course average: %.1f%%", sum / n));
                sumPct += sum;
                scored += n;
            }
        }
        if (count == 0) out("(no enrollment recorded for this student)");
        out("");
        out("Enrollments : " + count + "   Credit hours (not dropped): " + credits);
        out(scored == 0 ? "Overall average : n/a" : String.format("Overall average : %.1f%% over %d assessment(s)", sumPct / scored, scored));
        BigDecimal paid = BigDecimal.ZERO;
        for (FeePayment p : PAYMENTS) if (p.getStudent() == st) paid = paid.add(p.getAmount());
        int loans = 0;
        for (BookLoan l : LOANS) if (l.getStudent() == st && !l.isReturned()) loans++;
        out("Fees paid   : " + paid.toPlainString() + "   Books currently on loan: " + loans);
    }

    private static void paymentHistory() {
        out(titleBar("STUDENT PAYMENT HISTORY"));
        Student st = (Student) pickOrNull("Student", () -> STUDENTS, Main::studentSpec,
                need("Student", "viewing a payment history"), "payment history");
        if (st == null) return;
        List<FeePayment> list = new ArrayList<>();
        for (FeePayment p : PAYMENTS) if (p.getStudent() == st) list.add(p);
        list.sort(Comparator.comparing(FeePayment::getPaymentDate));
        out("Payments of " + st.getDisplayName() + " [" + st.getPersonId() + "]:");
        if (list.isEmpty()) {
            out("  (no payment recorded for this student)");
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (FeePayment p : list) {
            out(String.format("  %s | %-13s | %12s | receipt %s | id %s", p.getPaymentDate(), p.getPaymentMethod(),
                    p.getAmount().toPlainString(), p.getReceiptNumber(), p.getPaymentId()));
            total = total.add(p.getAmount());
        }
        out("  " + list.size() + " payment(s), total paid: " + total.toPlainString());
    }

    private static void availableBooks() {
        out(titleBar("AVAILABLE LIBRARY BOOKS"));
        int n = 0;
        for (LibraryBook b : BOOKS) {
            if (b.getAvailableCopies() > 0) {
                out("  " + b.getRegistrationSummary());
                n++;
            }
        }
        if (n == 0) {
            out(BOOKS.isEmpty() ? "  No library book is registered yet (use option 10)."
                    : "  Every copy is currently on loan.");
        }
    }

    private static void demonstratePolymorphism() {
        out(titleBar("RUNTIME POLYMORPHISM"));
        out("A) Superclass polymorphism - List<Person> holding Student, Teacher and Administrator");
        List<Person> people = allPersons();
        if (people.isEmpty()) {
            out("   (no person registered yet - register a student, a teacher and an administrator to see it live)");
        }
        for (Person p : people) {
            String extra = p instanceof AcademicMember ? " | category: " + ((AcademicMember) p).getAcademicCategory() : "";
            out("   " + pad(p.getClass().getSimpleName(), 14) + " getRoleDescription() -> " + p.getRoleDescription() + extra);
            out("   " + pad("", 14) + " getDisplayName()    -> " + p.getDisplayName() + " | isActive() -> " + p.isActive());
            out("   " + pad("", 14) + " getRegistrationSummary() -> " + p.getRegistrationSummary());
        }
        Set<Class<?>> shown = new HashSet<>();
        for (Person p : people) {
            if (shown.add(p.getClass())) {
                out("");
                out("   printDetails() through a Person reference for the first " + p.getClass().getSimpleName()
                        + " (overridden version runs, calling super.printDetails() up the chain):");
                p.printDetails();
            }
        }
        out("");
        out("B) Interface polymorphism - List<Identifiable> holding unrelated entities");
        List<Identifiable> everything = allEntities();
        if (everything.isEmpty()) out("   (no record registered yet)");
        for (Identifiable i : everything) {
            out("   " + pad(i.getClass().getSimpleName(), 14) + " id=" + i.getId() + " | " + i.getDisplayName()
                    + " | active=" + i.isActive());
        }
        out("   Interface constants: ID_PREFIX=" + Identifiable.ID_PREFIX + ", ID_SEPARATOR='" + Identifiable.ID_SEPARATOR
                + "', ID length " + Identifiable.MIN_ID_LENGTH + "-" + Identifiable.MAX_ID_LENGTH
                + ", DEFAULT_STATUS=" + Identifiable.DEFAULT_STATUS + ", ENTITY_VERSION=" + Identifiable.ENTITY_VERSION);
        out("");
        out("C) Inheritance chains (checked with reflection)");
        out("   " + chain(Student.class));
        out("   " + chain(Teacher.class));
        out("   " + chain(Administrator.class));
        out("   Person abstract? " + Modifier.isAbstract(Person.class.getModifiers())
                + " | AcademicMember abstract? " + Modifier.isAbstract(AcademicMember.class.getModifiers()));
        out("");
        out("D) Why can Person and AcademicMember not be instantiated?");
        out("   They are abstract: they declare abstract methods (getRoleDescription, getAcademicCategory) that have no");
        out("   meaning for a 'generic person'. 'new Person(...)' is a compile-time error; only concrete subclasses can");
        out("   be created, and their constructors reach the shared state with super(...) (constructor chaining).");
    }

    private static String chain(Class<?> c) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            if (sb.length() > 0) sb.append(" extends ");
            sb.append(k.getSimpleName());
        }
        return sb.toString();
    }

    private static void printMainMenu() {
        String[] left = {"Register Department", "Register Student", "Register Teacher", "Register Administrator",
                "Register Course", "Register Classroom", "Enroll Student", "Record Assessment",
                "Record Fee Payment", "Register Library Book"};
        String[] right = {"Borrow Library Book", "Return Library Book", "View All Records", "Search Record by ID",
                "Update Existing Record", "Student Academic Summary", "Student Payment History",
                "View Available Books", "Demonstrate Polymorphism", "Exit"};
        out("");
        out("============== AUCA_ID SCHOOL MANAGEMENT ==============");
        for (int i = 0; i < 10; i++) {
            int rightNo = i == 9 ? 0 : 11 + i;
            out(String.format(" %2d. %-26s %2d. %s", i + 1, left[i], rightNo, right[i]));
        }
    }

    private static void reportProblem(Exception e) {
        if (e instanceof Signal) {
            out("Operation cancelled. Nothing was changed.");
        } else if (e instanceof PrerequisiteMissingException) {
            out("! " + e.getMessage());
            out("  Operation cancelled. Nothing was saved.");
        } else if (e instanceof ValidationException || e instanceof DuplicateRecordException
                || e instanceof RecordNotFoundException || e instanceof BookUnavailableException) {
            out("! " + e.getMessage());
        } else if (e instanceof NullPointerException) {
            out("! A required value was missing; the operation was stopped safely. Please try again.");
        } else {
            out("! Unexpected problem: " + describe(e) + ". Returning to the main menu.");
        }
    }

    public static void main(String[] args) {
        out("Welcome to AUCA_ID - School Management System");
        try {
            boolean running = true;
            while (running) {
                printMainMenu();
                String input = readRaw("Select an option: ");
                int choice;
                try {
                    choice = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    out("! '" + input + "' is not a number. Enter a number between 0 and 19.");
                    continue;
                }
                try {
                    switch (choice) {
                        case 1: register(Main::departmentSpec); break;
                        case 2: register(Main::studentSpec); break;
                        case 3: register(Main::teacherSpec); break;
                        case 4: register(Main::administratorSpec); break;
                        case 5: register(Main::courseSpec); break;
                        case 6: register(Main::classroomSpec); break;
                        case 7: register(Main::enrollmentSpec); break;
                        case 8: register(Main::assessmentSpec); break;
                        case 9: register(Main::feePaymentSpec); break;
                        case 10: register(Main::libraryBookSpec); break;
                        case 11: register(Main::bookLoanSpec); break;
                        case 12: returnBook(); break;
                        case 13: viewAll(); break;
                        case 14: searchById(); break;
                        case 15: updateMenu(); break;
                        case 16: academicSummary(); break;
                        case 17: paymentHistory(); break;
                        case 18: availableBooks(); break;
                        case 19: demonstratePolymorphism(); break;
                        case 0:
                            running = false;
                            out("Goodbye! (All data is kept in memory only and is now released.)");
                            break;
                        default:
                            out("! Invalid option " + choice + ". Choose a number between 0 and 19.");
                    }
                } catch (InputClosedException e) {
                    throw e;
                } catch (Exception e) {
                    reportProblem(e);
                }
            }
        } catch (InputClosedException e) {
            out("");
            out("Input closed - exiting AUCA_ID.");
        }
    }
}
