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
import ru.vspochernin.gigalearn.entity.*;
import ru.vspochernin.gigalearn.exception.DuplicateSubmissionException;
import ru.vspochernin.gigalearn.repository.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SubmissionServiceTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Настройки Hikari для тестов
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "5000");
        registry.add("spring.datasource.hikari.validation-timeout", () -> "3000");
        registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "60000");
    }

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        // Очищаем данные перед каждым тестом
        submissionRepository.deleteAll();
        assignmentRepository.deleteAll();
        lessonRepository.deleteAll();
        moduleRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testSubmitAssignmentSuccessfully() {
        // Given: Создаем структуру курс -> модуль -> урок -> задание + студента
        Long assignmentId = createTestAssignment();
        User student = createTestStudent("Student", "student@test.com");

        // When: Студент сдает задание
        long submissionId = submissionService.submit(student.getId(), assignmentId, "My solution");

        // Then: Проверяем, что сдача создана
        assertThat(submissionId).isPositive();

        Submission submission = submissionRepository.findById(submissionId).orElseThrow();
        assertThat(submission.getContent()).isEqualTo("My solution");
        assertThat(submission.getSubmittedAt()).isNotNull();
        assertThat(submission.getScore()).isNull(); // Еще не оценено
    }

    @Test
    void testSubmitDuplicateThrowsException() {
        // Given: Создаем задание, студента и сдачу
        Long assignmentId = createTestAssignment();
        User student = createTestStudent("Student", "student@test.com");

        submissionService.submit(student.getId(), assignmentId, "First submission");

        // When/Then: Повторная сдача должна выбросить исключение
        assertThatThrownBy(() -> submissionService.submit(student.getId(), assignmentId, "Second submission"))
                .isInstanceOf(DuplicateSubmissionException.class)
                .hasMessageContaining("already submitted");
    }

    @Test
    void testGradeSubmissionSuccessfully() {
        // Given: Создаем сдачу
        Long assignmentId = createTestAssignment();
        User student = createTestStudent("Student", "student@test.com");
        long submissionId = submissionService.submit(student.getId(), assignmentId, "My solution");

        // When: Оцениваем сдачу
        submissionService.grade(submissionId, 85, "Good work!");

        // Then: Проверяем, что оценка и отзыв сохранились
        Submission graded = submissionRepository.findById(submissionId).orElseThrow();
        assertThat(graded.getScore()).isEqualTo(85);
        assertThat(graded.getFeedback()).isEqualTo("Good work!");
    }

    @Test
    void testGradeNonExistentSubmissionThrowsException() {
        // When/Then: Попытка оценить несуществующую сдачу
        assertThatThrownBy(() -> submissionService.grade(99999L, 50, "Feedback"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Submission not found");
    }

    @Test
    void testListQueriesWithMultipleSubmissions() {
        // Given: Создаем 2 задания, 2 студентов и разные комбинации сдач
        Long assignment1Id = createTestAssignment("Assignment 1");
        Long assignment2Id = createTestAssignment("Assignment 2");

        User student1 = createTestStudent("Student 1", "student1@test.com");
        User student2 = createTestStudent("Student 2", "student2@test.com");

        // Student1 сдает оба задания
        long sub1 = submissionService.submit(student1.getId(), assignment1Id, "Solution 1-1");
        long sub2 = submissionService.submit(student1.getId(), assignment2Id, "Solution 1-2");

        // Student2 сдает только первое задание
        long sub3 = submissionService.submit(student2.getId(), assignment1Id, "Solution 2-1");

        // When: Получаем списки
        List<Submission> assignment1Submissions = submissionService.getByAssignment(assignment1Id);
        List<Submission> assignment2Submissions = submissionService.getByAssignment(assignment2Id);

        List<Submission> student1Submissions = submissionService.getByStudent(student1.getId());
        List<Submission> student2Submissions = submissionService.getByStudent(student2.getId());

        // Then: Проверяем корректность списков
        assertThat(assignment1Submissions).hasSize(2);
        assertThat(assignment1Submissions).extracting(Submission::getId)
                .containsExactlyInAnyOrder(sub1, sub3);

        assertThat(assignment2Submissions).hasSize(1);
        assertThat(assignment2Submissions.get(0).getId()).isEqualTo(sub2);

        assertThat(student1Submissions).hasSize(2);
        assertThat(student1Submissions).extracting(Submission::getId)
                .containsExactlyInAnyOrder(sub1, sub2);

        assertThat(student2Submissions).hasSize(1);
        assertThat(student2Submissions.get(0).getId()).isEqualTo(sub3);
    }

    @Test
    void testCascadeDeleteOnAssignment() {
        // Given: Создаем задание с несколькими сдачами
        Long assignmentId = createTestAssignment();
        User student1 = createTestStudent("Student 1", "student1@test.com");
        User student2 = createTestStudent("Student 2", "student2@test.com");

        long sub1 = submissionService.submit(student1.getId(), assignmentId, "Solution 1");
        long sub2 = submissionService.submit(student2.getId(), assignmentId, "Solution 2");

        // When: Удаляем задание
        assignmentService.delete(assignmentId);

        // Then: Проверяем каскадное удаление сдач
        assertThat(assignmentRepository.existsById(assignmentId)).isFalse();
        assertThat(submissionRepository.existsById(sub1)).isFalse();
        assertThat(submissionRepository.existsById(sub2)).isFalse();
    }

    @Test
    void testValidateScoreAgainstMaxScore() {
        // Given: Создаем задание с maxScore = 100
        Long assignmentId = createTestAssignmentWithMaxScore(100);
        User student = createTestStudent("Student", "student@test.com");
        long submissionId = submissionService.submit(student.getId(), assignmentId, "Solution");

        // When/Then: Попытка выставить оценку выше maxScore должна быть отклонена
        assertThatThrownBy(() -> submissionService.grade(submissionId, 150, "Too high!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum score");

        // When/Then: Оценка в пределах maxScore должна работать
        submissionService.grade(submissionId, 95, "Excellent!");

        Submission graded = submissionRepository.findById(submissionId).orElseThrow();
        assertThat(graded.getScore()).isEqualTo(95);
    }

    // === Helper methods ===

    private Long createTestAssignment() {
        return createTestAssignment("Test Assignment");
    }

    private Long createTestAssignment(String title) {
        return createTestAssignmentWithMaxScore(title, 100);
    }

    private Long createTestAssignmentWithMaxScore(Integer maxScore) {
        return createTestAssignmentWithMaxScore("Test Assignment", maxScore);
    }

    private Long createTestAssignmentWithMaxScore(String title, Integer maxScore) {
        // Создаем минимальную структуру для задания
        Category category = categoryRepository.save(
                Category.builder().name("Test Category " + System.currentTimeMillis()).build()
        );

        User teacher = userRepository.save(
                User.builder()
                        .name("Teacher " + System.currentTimeMillis())
                        .email("teacher" + System.currentTimeMillis() + "@test.com")
                        .role(Role.TEACHER)
                        .build()
        );

        Course course = courseRepository.save(
                Course.builder()
                        .title("Test Course")
                        .description("Description")
                        .category(category)
                        .teacher(teacher)
                        .build()
        );

        ru.vspochernin.gigalearn.entity.Module module = moduleRepository.save(
                ru.vspochernin.gigalearn.entity.Module.builder()
                        .title("Test Module")
                        .course(course)
                        .orderIndex(1)
                        .build()
        );

        Lesson lesson = lessonRepository.save(
                Lesson.builder()
                        .title("Test Lesson")
                        .content("Content")
                        .module(module)
                        .build()
        );

        return assignmentService.createAssignment(lesson.getId(), title, "Description", maxScore);
    }

    private User createTestStudent(String name, String email) {
        return userRepository.save(
                User.builder()
                        .name(name)
                        .email(email)
                        .role(Role.STUDENT)
                        .build()
        );
    }
}

