package ru.vspochernin.gigalearn.service;

import org.hibernate.LazyInitializationException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CourseContentServiceTest {

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
    private CourseService courseService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private LessonService lessonService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Long teacherId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        // Очищаем данные перед каждым тестом
        assignmentRepository.deleteAll();
        lessonRepository.deleteAll();
        moduleRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        // Создаем базовые данные для тестов
        User teacher = User.builder()
                .name("Test Teacher")
                .email("teacher@test.com")
                .role(Role.TEACHER)
                .build();
        teacher = userRepository.save(teacher);
        teacherId = teacher.getId();

        Category category = Category.builder()
                .name("Test Category")
                .build();
        category = categoryRepository.save(category);
        categoryId = category.getId();
    }

    @Test
    void testCreateAndReadCourseWithContent() {
        // Given: Создаем курс с модулями и уроками
        Course createdCourse = courseService.createCourse(
                "Test Course",
                "Test Description",
                categoryId,
                teacherId,
                "4 weeks",
                null
        );
        Long courseId = createdCourse.getId();

        // Добавляем 2 модуля
        Long module1Id = courseService.addModule(courseId, "Module 1", "Description 1", 1);
        Long module2Id = courseService.addModule(courseId, "Module 2", "Description 2", 2);

        // Добавляем уроки к модулям
        moduleService.addLesson(module1Id, "Lesson 1.1", "Content 1.1", null);
        moduleService.addLesson(module1Id, "Lesson 1.2", "Content 1.2", null);
        moduleService.addLesson(module2Id, "Lesson 2.1", "Content 2.1", null);

        // When: Загружаем курс с контентом
        Course course = courseService.getCourseWithContent(courseId);

        // Then: Проверяем, что все данные загружены корректно
        assertThat(course).isNotNull();
        assertThat(course.getTitle()).isEqualTo("Test Course");
        assertThat(course.getModules()).hasSize(2);

        // Проверяем первый модуль
        ru.vspochernin.gigalearn.entity.Module firstModule = course.getModules().stream()
                .filter(m -> m.getTitle().equals("Module 1"))
                .findFirst()
                .orElseThrow();
        assertThat(firstModule.getLessons()).hasSize(2);

        // Проверяем второй модуль
        ru.vspochernin.gigalearn.entity.Module secondModule = course.getModules().stream()
                .filter(m -> m.getTitle().equals("Module 2"))
                .findFirst()
                .orElseThrow();
        assertThat(secondModule.getLessons()).hasSize(1);
    }

    @Test
    void testOrphanRemovalAndCascadeDelete() {
        // Given: Создаем курс с модулями, уроками и заданием
        Course createdCourse = courseService.createCourse(
                "Test Course",
                "Test Description",
                categoryId,
                teacherId,
                "4 weeks",
                null
        );
        Long courseId = createdCourse.getId();

        Long module1Id = courseService.addModule(courseId, "Module 1", "Description 1", 1);
        Long module2Id = courseService.addModule(courseId, "Module 2", "Description 2", 2);

        Long lesson1Id = moduleService.addLesson(module1Id, "Lesson 1.1", "Content 1.1", null);
        Long lesson2Id = moduleService.addLesson(module1Id, "Lesson 1.2", "Content 1.2", null);
        Long lesson3Id = moduleService.addLesson(module2Id, "Lesson 2.1", "Content 2.1", null);

        // Создаем задание для урока
        Assignment assignment = Assignment.builder()
                .title("Assignment 1")
                .description("Test assignment")
                .lesson(lessonRepository.findById(lesson1Id).orElseThrow())
                .maxScore(100)
                .build();
        Assignment savedAssignment = assignmentRepository.save(assignment);
        Long assignmentId = savedAssignment.getId();

        // When: Удаляем модуль 1
        moduleService.deleteModule(module1Id);

        // Then: Проверяем, что связанные уроки и задания удалены (orphanRemoval + cascade)
        assertThat(moduleRepository.existsById(module1Id)).isFalse();
        assertThat(lessonRepository.existsById(lesson1Id)).isFalse();
        assertThat(lessonRepository.existsById(lesson2Id)).isFalse();
        assertThat(assignmentRepository.existsById(assignmentId)).isFalse();

        // Модуль 2 и его урок должны остаться
        assertThat(moduleRepository.existsById(module2Id)).isTrue();
        assertThat(lessonRepository.existsById(lesson3Id)).isTrue();

        // When: Удаляем весь курс
        courseService.deleteCourse(courseId);

        // Then: Проверяем каскадное удаление всех связанных данных
        assertThat(courseRepository.existsById(courseId)).isFalse();
        assertThat(moduleRepository.existsById(module2Id)).isFalse();
        assertThat(lessonRepository.existsById(lesson3Id)).isFalse();
    }

    @Test
    void testLazyInitializationException() {
        // Given: Создаем курс с модулями
        Course createdCourse = courseService.createCourse(
                "Test Course",
                "Test Description",
                categoryId,
                teacherId,
                "4 weeks",
                null
        );
        Long courseId = createdCourse.getId();

        courseService.addModule(courseId, "Module 1", "Description 1", 1);
        courseService.addModule(courseId, "Module 2", "Description 2", 2);

        // When: Получаем курс без загрузки графа (вне транзакции)
        Course course = courseService.getCourseById(courseId);

        // Then: При попытке обратиться к ленивой коллекции вне транзакции должна возникнуть LazyInitializationException
        assertThatThrownBy(() -> course.getModules().size())
                .isInstanceOf(LazyInitializationException.class)
                .hasMessageContaining("could not initialize proxy");
    }

    @Test
    void testUpdateCourse() {
        // Given: Создаем курс
        Course course = courseService.createCourse(
                "Original Title",
                "Original Description",
                categoryId,
                teacherId,
                "4 weeks",
                null
        );
        Long courseId = course.getId();

        // When: Обновляем курс
        Course updatedCourse = courseService.updateCourse(
                courseId,
                "Updated Title",
                "Updated Description",
                "8 weeks",
                null
        );

        // Then: Проверяем обновленные данные
        assertThat(updatedCourse.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedCourse.getDescription()).isEqualTo("Updated Description");
        assertThat(updatedCourse.getDuration()).isEqualTo("8 weeks");

        // Проверяем, что изменения сохранились в БД
        Course fetchedCourse = courseService.getCourseById(courseId);
        assertThat(fetchedCourse.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    void testLessonCRUD() {
        // Given: Создаем курс и модуль
        Course course = courseService.createCourse(
                "Test Course",
                "Test Description",
                categoryId,
                teacherId,
                "4 weeks",
                null
        );
        Long moduleId = courseService.addModule(course.getId(), "Module 1", "Description", 1);

        // When: Создаем урок через ModuleService
        Long lessonId = moduleService.addLesson(moduleId, "Test Lesson", "Test Content", "http://video.url");

        // Then: Проверяем, что урок создан и доступен через LessonService
        Lesson lesson = lessonService.getLessonById(lessonId);
        assertThat(lesson).isNotNull();
        assertThat(lesson.getTitle()).isEqualTo("Test Lesson");
        assertThat(lesson.getContent()).isEqualTo("Test Content");
        assertThat(lesson.getVideoUrl()).isEqualTo("http://video.url");

        // When: Обновляем урок
        Lesson updatedLesson = lessonService.updateLesson(
                lessonId,
                "Updated Lesson",
                "Updated Content",
                "http://new-video.url"
        );

        // Then: Проверяем обновление
        assertThat(updatedLesson.getTitle()).isEqualTo("Updated Lesson");
        assertThat(updatedLesson.getContent()).isEqualTo("Updated Content");
        assertThat(updatedLesson.getVideoUrl()).isEqualTo("http://new-video.url");

        // When: Получаем уроки по модулю
        var lessons = lessonService.getLessonsByModuleId(moduleId);

        // Then: Проверяем, что урок в списке
        assertThat(lessons).hasSize(1);
        assertThat(lessons.get(0).getTitle()).isEqualTo("Updated Lesson");

        // When: Удаляем урок
        lessonService.deleteLesson(lessonId);

        // Then: Проверяем, что урок удален
        assertThat(lessonRepository.existsById(lessonId)).isFalse();
    }

    @Test
    void testModuleOrdering() {
        // Given: Создаем курс
        Course course = courseService.createCourse(
                "Test Course",
                "Test Description",
                categoryId,
                teacherId,
                "4 weeks",
                null
        );
        Long courseId = course.getId();

        // When: Добавляем модули с разным orderIndex
        Long module1Id = courseService.addModule(courseId, "Module 1", "First", 1);
        Long module2Id = courseService.addModule(courseId, "Module 2", "Second", 2);
        Long module3Id = courseService.addModule(courseId, "Module 3", "Third", 3);

        // Then: Проверяем orderIndex через прямое чтение из репозитория
        ru.vspochernin.gigalearn.entity.Module m1 = moduleService.getModuleById(module1Id);
        ru.vspochernin.gigalearn.entity.Module m2 = moduleService.getModuleById(module2Id);
        ru.vspochernin.gigalearn.entity.Module m3 = moduleService.getModuleById(module3Id);

        assertThat(m1.getOrderIndex()).isEqualTo(1);
        assertThat(m2.getOrderIndex()).isEqualTo(2);
        assertThat(m3.getOrderIndex()).isEqualTo(3);
    }

    @Test
    void testCascadeOnPartialDelete() {
        // Given: Создаем структуру с несколькими уровнями
        Course course = courseService.createCourse(
                "Test Course",
                "Test Description",
                categoryId,
                teacherId,
                "4 weeks",
                null
        );
        Long courseId = course.getId();

        Long module1Id = courseService.addModule(courseId, "Module 1", "First", 1);
        Long lesson1Id = moduleService.addLesson(module1Id, "Lesson 1", "Content 1", null);
        Long lesson2Id = moduleService.addLesson(module1Id, "Lesson 2", "Content 2", null);

        // Создаем задания
        Assignment assignment1 = Assignment.builder()
                .title("Assignment 1")
                .description("Test")
                .lesson(lessonRepository.findById(lesson1Id).orElseThrow())
                .maxScore(100)
                .build();
        assignmentRepository.save(assignment1);

        Assignment assignment2 = Assignment.builder()
                .title("Assignment 2")
                .description("Test")
                .lesson(lessonRepository.findById(lesson2Id).orElseThrow())
                .maxScore(100)
                .build();
        assignmentRepository.save(assignment2);

        // When: Удаляем один урок (не модуль целиком)
        lessonService.deleteLesson(lesson1Id);

        // Then: Проверяем, что только этот урок и его задания удалены
        assertThat(lessonRepository.existsById(lesson1Id)).isFalse();
        assertThat(lessonRepository.existsById(lesson2Id)).isTrue(); // Второй урок остался
        assertThat(moduleRepository.existsById(module1Id)).isTrue(); // Модуль остался
        assertThat(courseRepository.existsById(courseId)).isTrue(); // Курс остался

        // Проверяем, что задание первого урока удалено каскадно
        long assignmentCount = assignmentRepository.count();
        assertThat(assignmentCount).isEqualTo(1); // Осталось только одно задание (от lesson2)
    }
}

