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
import ru.vspochernin.gigalearn.repository.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class QuizServiceTest {

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
    private QuizService quizService;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerOptionRepository answerOptionRepository;

    @Autowired
    private QuizSubmissionRepository quizSubmissionRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        // Очищаем данные перед каждым тестом
        quizSubmissionRepository.deleteAll();
        answerOptionRepository.deleteAll();
        questionRepository.deleteAll();
        quizRepository.deleteAll();
        moduleRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testCreateQuizAndStructure() {
        // Given: Создаем модуль
        Long moduleId = createTestModule();

        // When: Создаем квиз
        long quizId = quizService.createQuiz(moduleId, "Test Quiz", 1800);

        // Then: Проверяем, что квиз создан
        Quiz quiz = quizService.getQuizById(quizId);
        assertThat(quiz).isNotNull();
        assertThat(quiz.getTitle()).isEqualTo("Test Quiz");
        assertThat(quiz.getTimeLimit()).isEqualTo(1800);

        // When: Добавляем 2 вопроса
        long question1Id = quizService.addQuestion(quizId, "What is ORM?");
        long question2Id = quizService.addQuestion(quizId, "What is LAZY loading?");

        // Then: Проверяем вопросы
        Question q1 = questionRepository.findById(question1Id).orElseThrow();
        Question q2 = questionRepository.findById(question2Id).orElseThrow();
        assertThat(q1.getText()).isEqualTo("What is ORM?");
        assertThat(q2.getText()).isEqualTo("What is LAZY loading?");

        // When: Добавляем варианты ответов для вопроса 1 (1 правильный)
        long option1_1 = quizService.addAnswerOption(question1Id, "Object-Relational Mapping", true);
        long option1_2 = quizService.addAnswerOption(question1Id, "Object-Remote Method", false);
        long option1_3 = quizService.addAnswerOption(question1Id, "Operational Resource", false);

        // When: Добавляем варианты ответов для вопроса 2 (2 правильных - multiple choice)
        long option2_1 = quizService.addAnswerOption(question2Id, "Loads data on demand", true);
        long option2_2 = quizService.addAnswerOption(question2Id, "Loads data immediately", false);
        long option2_3 = quizService.addAnswerOption(question2Id, "Reduces memory usage", true);

        // Then: Проверяем варианты
        List<AnswerOption> q1Options = answerOptionRepository.findByQuestionId(question1Id);
        List<AnswerOption> q2Options = answerOptionRepository.findByQuestionId(question2Id);

        assertThat(q1Options).hasSize(3);
        assertThat(q1Options.stream().filter(AnswerOption::getIsCorrect).count()).isEqualTo(1);

        assertThat(q2Options).hasSize(3);
        assertThat(q2Options.stream().filter(AnswerOption::getIsCorrect).count()).isEqualTo(2);
    }

    @Test
    void testTakeQuizWithPartiallyCorrectAnswers() {
        // Given: Создаем квиз со структурой
        QuizStructure structure = createQuizWithTwoQuestions();
        User student = createTestStudent("Student", "student@test.com");

        // When: Студент проходит тест с частично правильными ответами
        // Вопрос 1: правильный ответ (только option1_1)
        // Вопрос 2: неправильный ответ (выбрали только option2_1, а правильные оба: option2_1 и option2_3)
        Map<Long, List<Long>> answers = Map.of(
                structure.question1Id, List.of(structure.option1_1),  // Верно
                structure.question2Id, List.of(structure.option2_1)   // Неверно (не хватает option2_3)
        );

        QuizSubmission result = quizService.takeQuiz(student.getId(), structure.quizId, answers);

        // Then: Проверяем результат
        assertThat(result).isNotNull();
        assertThat(result.getScore()).isEqualTo(1); // Только 1 вопрос из 2 верно
        assertThat(result.getTakenAt()).isNotNull();

        // Проверяем, что результат сохранился
        QuizSubmission saved = quizSubmissionRepository.findById(result.getId()).orElseThrow();
        assertThat(saved.getScore()).isEqualTo(1);
    }

    @Test
    void testTakeQuizWithInvalidQuestionId() {
        // Given: Создаем квиз
        QuizStructure structure = createQuizWithTwoQuestions();
        User student = createTestStudent("Student", "student@test.com");

        // When/Then: Попытка передать questionId, не принадлежащий этому квизу
        Map<Long, List<Long>> answers = Map.of(
                99999L, List.of(structure.option1_1)  // Несуществующий questionId
        );

        assertThatThrownBy(() -> quizService.takeQuiz(student.getId(), structure.quizId, answers))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to quiz");
    }

    @Test
    void testTakeQuizWithInvalidOptionId() {
        // Given: Создаем квиз
        QuizStructure structure = createQuizWithTwoQuestions();
        User student = createTestStudent("Student", "student@test.com");

        // When/Then: Попытка передать optionId, не принадлежащий questionId
        Map<Long, List<Long>> answers = Map.of(
                structure.question1Id, List.of(99999L)  // Несуществующий optionId
        );

        assertThatThrownBy(() -> quizService.takeQuiz(student.getId(), structure.quizId, answers))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to question");
    }

    @Test
    void testMultipleAttemptsAllowed() {
        // Given: Создаем квиз и студента
        QuizStructure structure = createQuizWithTwoQuestions();
        User student = createTestStudent("Student", "student@test.com");

        // When: Первая попытка
        Map<Long, List<Long>> answers1 = Map.of(
                structure.question1Id, List.of(structure.option1_1)  // 1 балл
        );
        QuizSubmission attempt1 = quizService.takeQuiz(student.getId(), structure.quizId, answers1);

        // When: Вторая попытка (улучшенный результат)
        Map<Long, List<Long>> answers2 = Map.of(
                structure.question1Id, List.of(structure.option1_1),  // 1 балл
                structure.question2Id, List.of(structure.option2_1, structure.option2_3)  // 1 балл (оба правильных)
        );
        QuizSubmission attempt2 = quizService.takeQuiz(student.getId(), structure.quizId, answers2);

        // Then: Обе попытки должны быть сохранены
        assertThat(attempt1.getId()).isNotEqualTo(attempt2.getId());
        assertThat(attempt1.getScore()).isEqualTo(1);
        assertThat(attempt2.getScore()).isEqualTo(2);

        // Проверяем, что обе записи в БД
        List<QuizSubmission> studentSubmissions = quizService.getSubmissionsByStudent(student.getId());
        assertThat(studentSubmissions).hasSize(2);
    }

    @Test
    void testTakeQuizWithEmptyAnswers() {
        // Given: Создаем квиз и студента
        QuizStructure structure = createQuizWithTwoQuestions();
        User student = createTestStudent("Student", "student@test.com");

        // When: Студент не отвечает ни на один вопрос (пустая Map)
        Map<Long, List<Long>> emptyAnswers = Map.of();
        QuizSubmission result = quizService.takeQuiz(student.getId(), structure.quizId, emptyAnswers);

        // Then: Score должен быть 0
        assertThat(result.getScore()).isEqualTo(0);
        assertThat(result.getTakenAt()).isNotNull();
    }

    // === Helper methods ===

    private Long createTestModule() {
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

        return module.getId();
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

    private QuizStructure createQuizWithTwoQuestions() {
        Long moduleId = createTestModule();

        // Создаем квиз
        long quizId = quizService.createQuiz(moduleId, "Test Quiz", 1800);

        // Вопрос 1: один правильный вариант (SINGLE_CHOICE)
        long question1Id = quizService.addQuestion(quizId, "What is ORM?");
        long option1_1 = quizService.addAnswerOption(question1Id, "Object-Relational Mapping", true);
        long option1_2 = quizService.addAnswerOption(question1Id, "Object-Remote Method", false);
        long option1_3 = quizService.addAnswerOption(question1Id, "Operational Resource", false);

        // Вопрос 2: два правильных варианта (MULTIPLE_CHOICE)
        long question2Id = quizService.addQuestion(quizId, "What is LAZY loading?");
        long option2_1 = quizService.addAnswerOption(question2Id, "Loads data on demand", true);
        long option2_2 = quizService.addAnswerOption(question2Id, "Loads data immediately", false);
        long option2_3 = quizService.addAnswerOption(question2Id, "Reduces memory usage", true);

        return new QuizStructure(
                quizId,
                question1Id, option1_1, option1_2, option1_3,
                question2Id, option2_1, option2_2, option2_3
        );
    }

    // Helper record для хранения ID созданной структуры
    private record QuizStructure(
            long quizId,
            long question1Id, long option1_1, long option1_2, long option1_3,
            long question2Id, long option2_1, long option2_2, long option2_3
    ) {}
}

