package ru.vspochernin.gigalearn.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.vspochernin.gigalearn.dto.CourseCreateDto;
import ru.vspochernin.gigalearn.dto.ModuleCreateDto;
import ru.vspochernin.gigalearn.entity.Category;
import ru.vspochernin.gigalearn.entity.Role;
import ru.vspochernin.gigalearn.entity.Tag;
import ru.vspochernin.gigalearn.entity.User;
import ru.vspochernin.gigalearn.repository.*;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CourseControllerTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "5000");
        registry.add("spring.datasource.hikari.validation-timeout", () -> "3000");
        registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "60000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private Long teacherId;
    private Long categoryId;
    private Long tagId;

    @BeforeEach
    void setUp() {
        // Очищаем данные
        enrollmentRepository.deleteAll();
        moduleRepository.deleteAll();
        courseRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        // Создаем базовые данные
        Category category = categoryRepository.save(
                Category.builder().name("Test Category").build()
        );
        categoryId = category.getId();

        User teacher = userRepository.save(
                User.builder()
                        .name("Test Teacher")
                        .email("teacher@test.com")
                        .role(Role.TEACHER)
                        .build()
        );
        teacherId = teacher.getId();

        Tag tag = tagRepository.save(
                Tag.builder().name("Test Tag").build()
        );
        tagId = tag.getId();
    }

    @Test
    void testCreateCourse() throws Exception {
        // Given
        CourseCreateDto dto = CourseCreateDto.builder()
                .title("Test Course")
                .description("Test Description")
                .categoryId(categoryId)
                .teacherId(teacherId)
                .tagIds(Set.of(tagId))
                .build();

        // When/Then
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Test Course"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.categoryName").value("Test Category"))
                .andExpect(jsonPath("$.teacherName").value("Test Teacher"))
                .andExpect(jsonPath("$.tagNames").isArray())
                .andExpect(jsonPath("$.tagNames[0]").value("Test Tag"));
    }

    @Test
    void testCreateCourseValidationFails() throws Exception {
        // Given: DTO с пустым title
        CourseCreateDto dto = CourseCreateDto.builder()
                .title("")  // Invalid
                .categoryId(categoryId)
                .teacherId(teacherId)
                .build();

        // When/Then
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void testGetCourse() throws Exception {
        // Given: Создаем курс через репозиторий
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        User teacher = userRepository.findById(teacherId).orElseThrow();

        var course = courseRepository.save(
                ru.vspochernin.gigalearn.entity.Course.builder()
                        .title("Test Course")
                        .description("Test Description")
                        .category(category)
                        .teacher(teacher)
                        .build()
        );

        // When/Then
        mockMvc.perform(get("/api/courses/" + course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(course.getId()))
                .andExpect(jsonPath("$.title").value("Test Course"))
                .andExpect(jsonPath("$.categoryName").value("Test Category"))
                .andExpect(jsonPath("$.teacherName").value("Test Teacher"));
    }

    @Test
    void testGetCourseNotFound() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/courses/99999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Course not found")));
    }

    @Test
    void testAddModule() throws Exception {
        // Given: Создаем курс
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        User teacher = userRepository.findById(teacherId).orElseThrow();

        var course = courseRepository.save(
                ru.vspochernin.gigalearn.entity.Course.builder()
                        .title("Test Course")
                        .category(category)
                        .teacher(teacher)
                        .build()
        );

        ModuleCreateDto dto = ModuleCreateDto.builder()
                .title("Test Module")
                .description("Test Description")
                .orderIndex(1)
                .build();

        // When/Then
        mockMvc.perform(post("/api/courses/" + course.getId() + "/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Test Module"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.orderIndex").value(1))
                .andExpect(jsonPath("$.courseId").value(course.getId()))
                .andExpect(jsonPath("$.courseTitle").value("Test Course"));
    }

    @Test
    void testEnrollStudent() throws Exception {
        // Given: Создаем курс и студента
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        User teacher = userRepository.findById(teacherId).orElseThrow();

        var course = courseRepository.save(
                ru.vspochernin.gigalearn.entity.Course.builder()
                        .title("Test Course")
                        .category(category)
                        .teacher(teacher)
                        .build()
        );

        User student = userRepository.save(
                User.builder()
                        .name("Student")
                        .email("student@test.com")
                        .role(Role.STUDENT)
                        .build()
        );

        // When/Then
        mockMvc.perform(post("/api/courses/" + course.getId() + "/enroll")
                        .param("userId", student.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.studentName").value("Student"))
                .andExpect(jsonPath("$.courseId").value(course.getId()))
                .andExpect(jsonPath("$.courseTitle").value("Test Course"))
                .andExpect(jsonPath("$.enrollDate").isNotEmpty())
                .andExpect(jsonPath("$.status").value("Active"));
    }

    @Test
    void testEnrollStudentDuplicate() throws Exception {
        // Given: Создаем курс, студента и записываем его
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        User teacher = userRepository.findById(teacherId).orElseThrow();

        var course = courseRepository.save(
                ru.vspochernin.gigalearn.entity.Course.builder()
                        .title("Test Course")
                        .category(category)
                        .teacher(teacher)
                        .build()
        );

        User student = userRepository.save(
                User.builder()
                        .name("Student")
                        .email("student@test.com")
                        .role(Role.STUDENT)
                        .build()
        );

        // Первая запись
        mockMvc.perform(post("/api/courses/" + course.getId() + "/enroll")
                        .param("userId", student.getId().toString()))
                .andExpect(status().isCreated());

        // When/Then: Повторная запись должна вернуть 409
        mockMvc.perform(post("/api/courses/" + course.getId() + "/enroll")
                        .param("userId", student.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already enrolled")));
    }

    @Test
    void testUnenrollStudent() throws Exception {
        // Given: Создаем курс, студента и записываем его
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        User teacher = userRepository.findById(teacherId).orElseThrow();

        var course = courseRepository.save(
                ru.vspochernin.gigalearn.entity.Course.builder()
                        .title("Test Course")
                        .category(category)
                        .teacher(teacher)
                        .build()
        );

        User student = userRepository.save(
                User.builder()
                        .name("Student")
                        .email("student@test.com")
                        .role(Role.STUDENT)
                        .build()
        );

        mockMvc.perform(post("/api/courses/" + course.getId() + "/enroll")
                        .param("userId", student.getId().toString()))
                .andExpect(status().isCreated());

        // When/Then: Отписка должна пройти успешно
        mockMvc.perform(delete("/api/courses/" + course.getId() + "/enroll")
                        .param("userId", student.getId().toString()))
                .andExpect(status().isNoContent());
    }
}

