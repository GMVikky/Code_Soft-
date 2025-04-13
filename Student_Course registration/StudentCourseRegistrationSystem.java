import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

class Course {
    private final String code;
    private String title;
    private final String description;
    private final int capacity;
    private final String schedule;
    private final List<String> enrolledStudents;

    public Course(String code, String title, String description, int capacity, String schedule) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.capacity = capacity;
        this.schedule = schedule;
        this.enrolledStudents = new ArrayList<>();
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSchedule() {
        return schedule;
    }

    public int getAvailableSlots() {
        return capacity - enrolledStudents.size();
    }

    public int getCapacity() {
        return capacity;
    }

    public List<String> getEnrolledStudents() {
        return enrolledStudents;
    }

    @Override
    public String toString() {
        return "Course Code: " + code + "\nTitle: " + title + "\nDescription: " + description + 
               "\nSchedule: " + schedule + "\nAvailable Slots: " + getAvailableSlots() + "/" + capacity + "\n";
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

class Student {
    private final String studentId;
    private final String name;
    private final List<Course> registeredCourses;

    public Student(String studentId, String name) {
        this.studentId = studentId;
        this.name = name;
        this.registeredCourses = new ArrayList<>();
    }

    public String getStudentId() {
        return studentId;
    }

    public String getName() {
        return name;
    }

    public List<Course> getRegisteredCourses() {
        return registeredCourses;
    }

    public boolean registerCourse(Course course) {
        if (course.getAvailableSlots() > 0 && !registeredCourses.contains(course)) {
            registeredCourses.add(course);
            course.getEnrolledStudents().add(this.studentId);
            return true;
        }
        return false;
    }

    public boolean dropCourse(Course course) {
        if (registeredCourses.contains(course)) {
            registeredCourses.remove(course);
            course.getEnrolledStudents().remove(this.studentId);
            return true;
        }
        return false;
    }

    public String viewRegisteredCourses() {
        if (registeredCourses.isEmpty()) {
            return "No courses registered.";
        }

        StringBuilder result = new StringBuilder("Registered courses for " + name + " (ID: " + studentId + "):\n");
        for (Course course : registeredCourses) {
            result.append("- ").append(course.getCode()).append(": ")
                  .append(course.getTitle()).append(" (").append(course.getSchedule()).append(")\n");
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return "Student ID: " + studentId + "\nName: " + name + "\n";
    }
}

class RegistrationSystem {
    private final Map<String, Course> courses;
    private final Map<String, Student> students;

    public RegistrationSystem() {
        this.courses = new HashMap<>();
        this.students = new HashMap<>();
    }

    public void addCourse(Course course) {
        courses.put(course.getCode(), course);
    }

    public void addStudent(Student student) {
        students.put(student.getStudentId(), student);
    }

    public Course getCourse(String courseCode) {
        return courses.get(courseCode);
    }

    public Student getStudent(String studentId) {
        return students.get(studentId);
    }

    public String listAllCourses() {
        if (courses.isEmpty()) {
            return "No courses available.";
        }

        StringBuilder result = new StringBuilder("Available Courses:\n" + "=".repeat(50) + "\n");
        for (Course course : courses.values()) {
            result.append(course.toString()).append("-".repeat(30)).append("\n");
        }
        return result.toString();
    }

    public String listAvailableCourses() {
        if (courses.isEmpty()) {
            return "No courses available.";
        }

        StringBuilder result = new StringBuilder("Available Courses (with open slots):\n" + "=".repeat(50) + "\n");
        for (Course course : courses.values()) {
            if (course.getAvailableSlots() > 0) {
                result.append(course.toString()).append("-".repeat(30)).append("\n");
            }
        }
        return result.toString();
    }
}

public class StudentCourseRegistrationSystem {
    public static void main(String[] args) {
        RegistrationSystem system = new RegistrationSystem();

        try (Scanner scanner = new Scanner(System.in)) {

            system.addCourse(new Course("CS101", "Introduction to Programming",
                    "Basic programming concepts using Java", 30, "Mon/Wed 10:00-11:30"));
            system.addCourse(new Course("CS201", "Data Structures",
                    "Advanced data structures and algorithms", 25, "Tue/Thu 13:00-14:30"));
            system.addCourse(new Course("MATH101", "Calculus I",
                    "Introduction to differential calculus", 35, "Mon/Wed 13:00-14:30"));
            system.addCourse(new Course("ENG101", "English Composition",
                    "Fundamentals of writing", 40, "Fri 09:00-12:00"));

            system.addStudent(new Student("S1001", "John Doe"));
            system.addStudent(new Student("S1002", "Jane Smith"));

            boolean running = true;
            while (running) {
                System.out.println("\n==== Student Course Registration System ====");
                System.out.println("1. List All Courses");
                System.out.println("2. List Available Courses (with open slots)");
                System.out.println("3. Register a Student for a Course");
                System.out.println("4. Drop a Course");
                System.out.println("5. View Student's Registered Courses");
                System.out.println("6. Add New Student");
                System.out.println("7. Add New Course");
                System.out.println("8. Exit");

                System.out.print("\nEnter your choice (1-8): ");
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1" -> System.out.println(system.listAllCourses());

                    case "2" -> System.out.println(system.listAvailableCourses());

                    case "3" -> {
                        System.out.print("Enter student ID: ");
                        String studentId = scanner.nextLine();
                        System.out.print("Enter course code: ");
                        String courseCode = scanner.nextLine();

                        Student student = system.getStudent(studentId);
                        Course course = system.getCourse(courseCode);

                        if (student != null && course != null) {
                            if (student.registerCourse(course)) {
                                System.out.println("Successfully registered " + student.getName() +
                                        " for " + course.getTitle());
                            } else {
                                System.out.println("Registration failed. Course may be full or student already registered.");
                            }
                        } else {
                            System.out.println("Invalid student ID or course code.");
                        }
                    }

                    case "4" -> {
                        System.out.print("Enter student ID: ");
                        String studentId = scanner.nextLine();
                        System.out.print("Enter course code: ");
                        String courseCode = scanner.nextLine();
                        Student student = system.getStudent(studentId);
                        Course course = system.getCourse(courseCode);
                        if (student != null && course != null) {
                            if (student.dropCourse(course)) {
                                System.out.println("Successfully dropped " + course.getTitle() +
                                        " for " + student.getName());
                            } else {
                                System.out.println("Drop failed. Student may not be registered for this course.");
                            }
                        } else {
                            System.out.println("Invalid student ID or course code.");
                        }
                    }

                    case "5" -> {
                        System.out.print("Enter student ID: ");
                        String studentId = scanner.nextLine();
                        Student student = system.getStudent(studentId);
                        if (student != null) {
                            System.out.println(student.viewRegisteredCourses());
                        } else {
                            System.out.println("Invalid student ID.");
                        }
                    }

                    case "6" -> {
                        System.out.print("Enter new student ID: ");
                        String studentId = scanner.nextLine();
                        System.out.print("Enter student name: ");
                        String name = scanner.nextLine();
                        if (system.getStudent(studentId) != null) {
                            System.out.println("A student with this ID already exists.");
                        } else {
                            system.addStudent(new Student(studentId, name));
                            System.out.println("Student " + name + " added successfully.");
                        }
                    }

                    case "7" -> {
                        System.out.print("Enter course code: ");
                        String code = scanner.nextLine();
                        System.out.print("Enter course title: ");
                        String title = scanner.nextLine();
                        System.out.print("Enter course description: ");
                        String description = scanner.nextLine();
                        System.out.print("Enter course capacity: ");
                        int capacity = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter course schedule: ");
                        String schedule = scanner.nextLine();

                        if (system.getCourse(code) != null) {
                            System.out.println("A course with this code already exists.");
                        } else {
                            system.addCourse(new Course(code, title, description, capacity, schedule));
                            System.out.println("Course " + title + " added successfully.");
                        }
                    }

                    case "8" -> {
                        System.out.println("Thank you for using the Student Course Registration System. Goodbye!");
                        running = false;
                    }

                    default -> System.out.println("Invalid choice. Please try again.");
                }
            }
        }
    }
}