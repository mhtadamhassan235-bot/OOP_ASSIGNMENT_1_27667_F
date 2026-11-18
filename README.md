# AUCA_ID - School Management System (console, Java)

Practical assignment for OOP (AUCA) - complete runnable project.
Requires a JDK 8 or newer (tested with `--release 8` and JDK 21). No database, no GUI, no external library.

## Compile and run

| System | Compile | Run |
|---|---|---|
| Linux / macOS | `./compile.sh` | `./run.sh` |
| Windows | `compile.bat` | `run.bat` |
| By hand | `javac -d out $(find src -name "*.java")` | `java -cp out com.auca.main.Main` |

IDE (IntelliJ / Eclipse / NetBeans): open the folder, mark `src` as the source root, run `com.auca.main.Main`.

## Package tree (root package `com.auca`)

```
src/com/auca/
  interfaces/     Identifiable, Registrable                      (2 interfaces)
  abstracts/      Person, AcademicMember                         (2 abstract classes)
  student/        Student          teacher/      Teacher
  administrator/  Administrator    department/   Department
  course/         Course           classroom/    Classroom
  enrollment/     Enrollment       assessment/   Assessment
  feepayment/     FeePayment       librarybook/  LibraryBook
  bookloan/       BookLoan                                       (11 concrete classes, one package each)
  exceptions/     ValidationException, DuplicateRecordException, RecordNotFoundException,
                  PrerequisiteMissingException, BookUnavailableException
  main/           Main                                           (entry point, menu, forms)
```
Exactly 15 domain types + `Main` + the 5 exceptions. Nothing else was added (the helper types of the form engine are private nested types inside `Main`).

## Hierarchy

```
Person (abstract, implements Identifiable + Registrable)
 |-- AcademicMember (abstract)  -- Student      } multilevel
 |                              \- Teacher      } + hierarchical
 \-- Administrator                              } hierarchical
Department, Course, Classroom, Enrollment, Assessment, FeePayment, LibraryBook, BookLoan
   implement Identifiable and Registrable directly.
```

## Design decisions to explain at the physical demonstration

* **getRoleDescription() / getAcademicCategory()**: the "cleaner alternative" of section 4 was chosen - they are `abstract` methods of `Person` / `AcademicMember` (not in `Registrable`), so Department, Course, ... are not forced to implement person-only operations.
* **Static validation toolkit inside `Registrable`** (`requireText`, `requireEmail`, `requirePhone`, `parseDate`, `requireMoney`...): interface static methods are explicitly allowed. Each rule is written once and used both by the classes' `validate()` and by the console forms.
* **One generic form engine** (in `Main`): each entity is a `Spec` = numbered fields + builder + cross-record rules. The same Spec runs registration, review, edit-field and "Update Existing Record", so there is a single validation path. Saving always re-checks uniqueness, references, `validate()` and the cross-record rules, then commits exactly one object.
* **Abstract classes cannot be instantiated** because they declare abstract methods (and a generic "person" has no meaning). Their constructors are only reachable through `super(...)` from Student / Teacher / Administrator (constructor chaining). `Student` also has a second constructor using `this(...)`.
* **Fields are private** everywhere (no protected fields were needed); getters/setters, `toString()` and `validate()` exist in every class.
* **Stock**: `availableCopies` is decremented exactly once in the loan's `storer` (only after every check passed) and incremented exactly once in *Return Book*. A cancelled or failed workflow never touches stock.

## Console commands inside forms

| Type | Effect |
|---|---|
| `BACK` | go to the previous field (values already entered are kept; ENTER keeps the shown value) |
| `CANCEL` | discard the unsaved record (nested form: return to the interrupted form) |
| `0` | cancel in numbered lists and menus |
| `TODAY` | accepted in date fields (explicit shortcut for the current date) |

Review screen: `1` Save, `2` Edit field (asks the field number 01-NN, shows the old value), `0` Cancel.

## Validation rules (section 8) and documented choices

* Names 2-60 chars (letters, spaces, `.` `'` `-`); advisor/program/specialization 2-120; IDs 4-30 chars (letters, digits, `.`, `_`, `-`).
* Unique: person ID (across students, teachers, administrators), person e-mail, member number, institutional e-mail, student number, employee number, staff number, department ID/name/code, course ID/code, classroom ID and building+room, enrollment ID, assessment ID, payment ID, **receipt number**, book ID and ISBN, loan ID.
* E-mail syntax + max 100 chars; phone / guardian phone: 10-15 digits with optional leading `+`.
* Dates parsed strictly with `LocalDate` (2026-02-30 rejected): date of birth in the past; join / admission / hire / employment / enrollment / assessment / payment / borrowed dates **not in the future** (documented rule); join/admission/hire dates after the date of birth; due date after borrowed date; returned date empty until return and never before borrowed date.
* **Study year 1-6**: the question says 1-6 in section 8 and 1-8 in the sample form of section 17; section 8 ("at least enforce") was followed. To change it, edit the three `1, 6` values for "Study year" in `Student.validate()` and `Main.studentSpec()`.
* Credit hours 1-6, capacity > 0 (max 1000), floor >= 0, salary and payments > 0 with at most 2 decimals (`BigDecimal`), copies >= 0 with available <= total, maximum score > 0 and 0 <= obtained <= maximum.
* Allowed values: academic status ACTIVE/INACTIVE; employment type FULL_TIME/PART_TIME/CONTRACT; enrollment status ACTIVE/COMPLETED/DROPPED; semester = FALL/SPRING/SUMMER + year (e.g. `FALL 2026`); assessment type QUIZ/ASSIGNMENT/MIDTERM/FINAL_EXAM/PROJECT; payment method CASH/MOBILE_MONEY/BANK_TRANSFER/CARD.
* Cross-record rules: no duplicate ACTIVE enrollment (student + course + semester); assessments only on ACTIVE enrollments; loans need an available copy; stock edited by hand must stay consistent with the loans in progress.
* Exceptions: `ValidationException`, `DuplicateRecordException`, `RecordNotFoundException`, `PrerequisiteMissingException`, `BookUnavailableException` are all thrown in real scenarios and caught at the form / menu boundary. `NumberFormatException` and `DateTimeParseException` are caught where input is parsed; no empty `catch`, no stack trace shown, the program never exits because one operation failed.

## Demonstration checklist (section 12.6) - how to show each item

| Item | How |
|---|---|
| (a) register every type | menu 1 to 11 (a Book loan is created by *Borrow Library Book*) |
| (b) missing prerequisite + guided registration | start with an empty system: menu **2** (Student) -> field 08 shows the empty-state, choose `1`, the Department form opens, save it, you return to field 08 with all entered values intact. Also menu 5 (Course needs Department **and** Teacher, nested twice) and menu 7 (Student and Course) |
| (c) duplicate ID rejected | type an existing Person ID / student number / receipt number |
| (d) invalid date and amount | `2004-02-30`, `abc`, `-5`, `500.555` |
| (e) enrollment / assessment by selection | menu 7 and 8 use numbered lists only |
| (f) borrow and return | menu 11 then 18 (stock down), 12 then 18 (stock up). Try borrowing a book with 0 copies left |
| (g) view / search / edit | menu 13, 14 (also finds student number, course code, ISBN, receipt), 15 |
| (h) cancel without partial save | `CANCEL` in any form, or `0` on the review screen |
| (i) recover from an exception | enter letters in a number field, a menu choice `abc`, a missing prerequisite |
| inheritance + polymorphism | menu **19** (List&lt;Person&gt;, List&lt;Identifiable&gt;, reflection on the class chain, why abstract) |

Data is kept in memory only; it disappears when the program exits (as required: no database).
