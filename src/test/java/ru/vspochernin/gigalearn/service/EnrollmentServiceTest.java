package ru.vspochernin.gigalearn.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.vspochernin.gigalearn.entity.Category;
import ru.vspochernin.gigalearn.entity.Course;
import ru.vspochernin.gigalearn.entity.Role;
import ru.vspochernin.gigalearn.entity.User;
import ru.vspochernin.gigalearn.exception.DuplicateEnrollmentException;
import ru.vspochernin.gigalearn.repository.CategoryRepository;
import ru.vspochernin.gigalearn.repository.CourseRepository;
import ru.vspochernin.gigalearn.repository.EnrollmentRepository;
import ru.vspochernin.gigalearn.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class EnrollmentServiceTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Настройки Hikari для тестов - предотвращаем зависание при завершении
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "5000");
        registry.add("spring.datasource.hikari.validation-timeout", () -> "3000");
        registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "60000");
    }

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @BeforeEach
    void setUp() {
        // Очищаем данные перед каждым тестом
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testEnrollStudentSuccessfully() {
        // Given: Создаем курс и студента
        Category category = categoryRepository.save(
                Category.builder().name("Test Category").build()
        );

        User teacher = userRepository.save(
                User.builder().name("Teacher").email("teacher@test.com").role(Role.TEACHER).build()
        );

        Course course = courseRepository.save(
                Course.builder()
                        .title("Test Course")
                        .description("Description")
                        .category(category)
                        .teacher(teacher)
                        .build()
        );

        User student = userRepository.save(
                User.builder().name("Student").email("student@test.com").role(Role.STUDENT).build()
        );

        // When: Записываем студента на курс
        long enrollmentId = enrollmentService.enrollStudent(course.getId(), student.getId());

        // Then: Проверяем, что запись создана
        assertThat(enrollmentId).isPositive();

        // Проверяем списки
        List<Course> studentCourses = enrollmentService.getCoursesForStudent(student.getId());
        assertThat(studentCourses).hasSize(1);
        assertThat(studentCourses.get(0).getId()).isEqualTo(course.getId());

        List<User> courseStudents = enrollmentService.getStudentsForCourse(course.getId());
        assertThat(courseStudents).hasSize(1);
        assertThat(courseStudents.get(0).getId()).isEqualTo(student.getId());
    }

    @Test
    void testEnrollDuplicateThrowsException() {
        // Given: Создаем курс, студента и записываем студента
        Category category = categoryRepository.save(
                Category.builder().name("Test Category").build()
        );
        
        User teacher = userRepository.save(
                User.builder().name("Teacher").email("teacher@test.com").role(Role.TEACHER).build()
        );
        
        Course course = courseRepository.save(
                Course.builder()
                        .title("Test Course")
                        .description("Description")
                        .category(category)
                        .teacher(teacher)
                        .build()
        );

        User student = userRepository.save(
                User.builder().name("Student").email("student@test.com").role(Role.STUDENT).build()
        );

        enrollmentService.enrollStudent(course.getId(), student.getId());

        // When/Then: Повторная запись должна выбросить исключение
        assertThatThrownBy(() -> enrollmentService.enrollStudent(course.getId(), student.getId()))
                .isInstanceOf(DuplicateEnrollmentException.class)
                .hasMessageContaining("already enrolled");
    }

    @Test
    void testUnenrollSuccessfully() {
        // Given: Создаем курс, студента и записываем
        Category category = categoryRepository.save(
                Category.builder().name("Test Category").build()
        );
        
        User teacher = userRepository.save(
                User.builder().name("Teacher").email("teacher@test.com").role(Role.TEACHER).build()
        );
        
        Course course = courseRepository.save(
                Course.builder()
                        .title("Test Course")
                        .description("Description")
                        .category(category)
                        .teacher(teacher)
                        .build()
        );

        User student = userRepository.save(
                User.builder().name("Student").email("student@test.com").role(Role.STUDENT).build()
        );

        enrollmentService.enrollStudent(course.getId(), student.getId());

        // When: Отписываем студента
        boolean result = enrollmentService.unenrollStudent(course.getId(), student.getId());

        // Then: Проверяем успешную отписку
        assertThat(result).isTrue();

        // Проверяем, что запись удалена
        List<Course> studentCourses = enrollmentService.getCoursesForStudent(student.getId());
        assertThat(studentCourses).isEmpty();

        // When: Повторная отписка
        boolean secondResult = enrollmentService.unenrollStudent(course.getId(), student.getId());

        // Then: Должна вернуть false
        assertThat(secondResult).isFalse();
    }

    @Test
    void testReenrollAfterUnenroll() {
        // Given: Создаем курс, студента, записываем и отписываем
        Category category = categoryRepository.save(
                Category.builder().name("Test Category").build()
        );
        
        User teacher = userRepository.save(
                User.builder().name("Teacher").email("teacher@test.com").role(Role.TEACHER).build()
        );
        
        Course course = courseRepository.save(
                Course.builder()
                        .title("Test Course")
                        .description("Description")
                        .category(category)
                        .teacher(teacher)
                        .build()
        );

        User student = userRepository.save(
                User.builder().name("Student").email("student@test.com").role(Role.STUDENT).build()
        );

        enrollmentService.enrollStudent(course.getId(), student.getId());
        enrollmentService.unenrollStudent(course.getId(), student.getId());

        // When: Повторная запись после отписки
        long newEnrollmentId = enrollmentService.enrollStudent(course.getId(), student.getId());

        // Then: Запись должна быть успешной
        assertThat(newEnrollmentId).isPositive();

        List<Course> studentCourses = enrollmentService.getCoursesForStudent(student.getId());
        assertThat(studentCourses).hasSize(1);
    }

    @Test
    void testEnrollNonExistentUserThrowsException() {
        // Given: Создаем только курс
        Category category = categoryRepository.save(
                Category.builder().name("Test Category").build()
        );

        User teacher = userRepository.save(
                User.builder().name("Teacher").email("teacher@test.com").role(Role.TEACHER).build()
        );

        Course course = courseRepository.save(
                Course.builder()
                        .title("Test Course")
                        .description("Description")
                        .category(category)
                        .teacher(teacher)
                        .build()
        );

        // When/Then: Попытка записать несуществующего студента
        assertThatThrownBy(() -> enrollmentService.enrollStudent(course.getId(), 99999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void testEnrollNonExistentCourseThrowsException() {
        // Given: Создаем только студента
        User student = userRepository.save(
                User.builder().name("Student").email("student@test.com").role(Role.STUDENT).build()
        );

        // When/Then: Попытка записать на несуществующий курс
        assertThatThrownBy(() -> enrollmentService.enrollStudent(99999L, student.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    void testListQueriesWithMultipleEnrollments() {
        // Given: Создаем 2 курса и 3 студентов
        Category category = categoryRepository.save(
                Category.builder().name("Test Category").build()
        );

        User teacher = userRepository.save(
                User.builder().name("Teacher").email("teacher@test.com").role(Role.TEACHER).build()
        );

        Course course1 = courseRepository.save(
                Course.builder()
                        .title("Course 1")
                        .description("Description 1")
                        .category(category)
                        .teacher(teacher)
                        .build()
        );

        Course course2 = courseRepository.save(
                Course.builder()
                        .title("Course 2")
                        .description("Description 2")
                        .category(category)
                        .teacher(teacher)
                        .build()
        );

        User student1 = userRepository.save(
                User.builder().name("Student 1").email("student1@test.com").role(Role.STUDENT).build()
        );

        User student2 = userRepository.save(
                User.builder().name("Student 2").email("student2@test.com").role(Role.STUDENT).build()
        );

        User student3 = userRepository.save(
                User.builder().name("Student 3").email("student3@test.com").role(Role.STUDENT).build()
        );

        // When: Создаем различные комбинации записей
        // Student1 -> Course1, Course2
        enrollmentService.enrollStudent(course1.getId(), student1.getId());
        enrollmentService.enrollStudent(course2.getId(), student1.getId());

        // Student2 -> Course1
        enrollmentService.enrollStudent(course1.getId(), student2.getId());

        // Student3 -> Course2
        enrollmentService.enrollStudent(course2.getId(), student3.getId());

        // Then: Проверяем списки курсов для студентов
        List<Course> student1Courses = enrollmentService.getCoursesForStudent(student1.getId());
        assertThat(student1Courses).hasSize(2);
        assertThat(student1Courses).extracting(Course::getTitle)
                .containsExactlyInAnyOrder("Course 1", "Course 2");

        List<Course> student2Courses = enrollmentService.getCoursesForStudent(student2.getId());
        assertThat(student2Courses).hasSize(1);
        assertThat(student2Courses.get(0).getTitle()).isEqualTo("Course 1");

        List<Course> student3Courses = enrollmentService.getCoursesForStudent(student3.getId());
        assertThat(student3Courses).hasSize(1);
        assertThat(student3Courses.get(0).getTitle()).isEqualTo("Course 2");

        // Then: Проверяем списки студентов для курсов
        List<User> course1Students = enrollmentService.getStudentsForCourse(course1.getId());
        assertThat(course1Students).hasSize(2);
        assertThat(course1Students).extracting(User::getName)
                .containsExactlyInAnyOrder("Student 1", "Student 2");

        List<User> course2Students = enrollmentService.getStudentsForCourse(course2.getId());
        assertThat(course2Students).hasSize(2);
        assertThat(course2Students).extracting(User::getName)
                .containsExactlyInAnyOrder("Student 1", "Student 3");
    }
}

