package ru.vspochernin.gigalearn.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.vspochernin.gigalearn.entity.*;
import ru.vspochernin.gigalearn.repository.*;

import java.time.LocalDate;
import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== Начало загрузки демо-данных для dev-профиля ===");

        // Проверяем, нужна ли загрузка данных
        if (courseRepository.count() > 0) {
            log.info("База данных уже содержит данные (курсов: {}), пропускаем загрузку демо-данных",
                    courseRepository.count());
            return;
        }

        // 1. Создаем пользователей
        User teacher = User.builder()
                .name("Иван Петров")
                .email("teacher@gigalearn.ru")
                .role(Role.TEACHER)
                .build();
        teacher = userRepository.save(teacher);
        log.info("Создан преподаватель: {}", teacher.getName());

        User student1 = User.builder()
                .name("Анна Смирнова")
                .email("anna@student.ru")
                .role(Role.STUDENT)
                .build();
        student1 = userRepository.save(student1);

        User student2 = User.builder()
                .name("Петр Иванов")
                .email("petr@student.ru")
                .role(Role.STUDENT)
                .build();
        student2 = userRepository.save(student2);
        log.info("Созданы студенты: {} и {}", student1.getName(), student2.getName());

        // 2. Создаем категорию
        Category category = Category.builder()
                .name("Программирование")
                .build();
        category = categoryRepository.save(category);
        log.info("Создана категория: {}", category.getName());

        // 3. Создаем теги
        Tag tag1 = Tag.builder()
                .name("Java")
                .build();
        tag1 = tagRepository.save(tag1);

        Tag tag2 = Tag.builder()
                .name("ORM")
                .build();
        tag2 = tagRepository.save(tag2);
        log.info("Созданы теги: {}, {}", tag1.getName(), tag2.getName());

        // 4. Создаем курс с тегами
        Course course = Course.builder()
                .title("Основы Hibernate и JPA")
                .description("Изучение объектно-реляционного отображения в Java")
                .duration("8 недель")
                .startDate(LocalDate.now().plusDays(7))
                .category(category)
                .teacher(teacher)
                .tags(Set.of(tag1, tag2))
                .build();
        course = courseRepository.save(course);
        log.info("Создан курс: {}", course.getTitle());

        // 5. Создаем модули
        ru.vspochernin.gigalearn.entity.Module module1 = ru.vspochernin.gigalearn.entity.Module.builder()
                .title("Введение в ORM")
                .description("Основные концепции объектно-реляционного отображения")
                .orderIndex(1)
                .course(course)
                .build();
        module1 = moduleRepository.save(module1);

        ru.vspochernin.gigalearn.entity.Module module2 = ru.vspochernin.gigalearn.entity.Module.builder()
                .title("Продвинутые возможности Hibernate")
                .description("Кэширование, ленивая загрузка, оптимизация запросов")
                .orderIndex(2)
                .course(course)
                .build();
        module2 = moduleRepository.save(module2);
        log.info("Созданы модули: {} и {}", module1.getTitle(), module2.getTitle());

        // 6. Создаем уроки для модуля 1
        Lesson lesson1_1 = Lesson.builder()
                .title("Что такое ORM?")
                .content("Объектно-реляционное отображение (ORM) позволяет работать с базой данных как с объектами.")
                .videoUrl("https://example.com/video1")
                .module(module1)
                .build();
        lesson1_1 = lessonRepository.save(lesson1_1);

        Lesson lesson1_2 = Lesson.builder()
                .title("Настройка Hibernate")
                .content("Конфигурация и первый проект с Hibernate")
                .videoUrl("https://example.com/video2")
                .module(module1)
                .build();
        lesson1_2 = lessonRepository.save(lesson1_2);

        // 7. Создаем уроки для модуля 2
        Lesson lesson2_1 = Lesson.builder()
                .title("Ленивая загрузка")
                .content("Разбираем стратегии загрузки данных: LAZY vs EAGER")
                .videoUrl("https://example.com/video3")
                .module(module2)
                .build();
        lesson2_1 = lessonRepository.save(lesson2_1);

        Lesson lesson2_2 = Lesson.builder()
                .title("Кэширование в Hibernate")
                .content("Первый и второй уровень кэша")
                .module(module2)
                .build();
        lesson2_2 = lessonRepository.save(lesson2_2);
        log.info("Созданы уроки: 4 урока в двух модулях");

        // 8. Создаем задание к одному из уроков
        Assignment assignment = Assignment.builder()
                .title("Практическое задание: настройка проекта")
                .description("Создайте простой проект с Hibernate и выполните базовые CRUD-операции")
                .dueDate(LocalDate.now().plusDays(14))
                .maxScore(100)
                .lesson(lesson1_2)
                .build();
        assignment = assignmentRepository.save(assignment);
        log.info("Создано задание: {}", assignment.getTitle());

        // 9. Создаем квиз для модуля 1
        Quiz quiz = Quiz.builder()
                .title("Тест по введению в ORM")
                .timeLimit(30)
                .module(module1)
                .build();
        quiz = quizRepository.save(quiz);
        log.info("Создан квиз: {}", quiz.getTitle());

        // 10. Создаем вопросы для квиза
        Question question1 = Question.builder()
                .text("Что такое ORM?")
                .type("SINGLE_CHOICE")
                .quiz(quiz)
                .build();
        question1 = questionRepository.save(question1);

        Question question2 = Question.builder()
                .text("Какая стратегия загрузки является ленивой?")
                .type("SINGLE_CHOICE")
                .quiz(quiz)
                .build();
        question2 = questionRepository.save(question2);
        log.info("Созданы вопросы: 2 вопроса");

        // 11. Создаем варианты ответов для вопроса 1
        AnswerOption option1_1 = AnswerOption.builder()
                .text("Object-Relational Mapping")
                .isCorrect(true)
                .question(question1)
                .build();
        answerOptionRepository.save(option1_1);

        AnswerOption option1_2 = AnswerOption.builder()
                .text("Object-Remote Method")
                .isCorrect(false)
                .question(question1)
                .build();
        answerOptionRepository.save(option1_2);

        AnswerOption option1_3 = AnswerOption.builder()
                .text("Operational Resource Manager")
                .isCorrect(false)
                .question(question1)
                .build();
        answerOptionRepository.save(option1_3);

        // 12. Создаем варианты ответов для вопроса 2
        AnswerOption option2_1 = AnswerOption.builder()
                .text("EAGER")
                .isCorrect(false)
                .question(question2)
                .build();
        answerOptionRepository.save(option2_1);

        AnswerOption option2_2 = AnswerOption.builder()
                .text("LAZY")
                .isCorrect(true)
                .question(question2)
                .build();
        answerOptionRepository.save(option2_2);

        AnswerOption option2_3 = AnswerOption.builder()
                .text("IMMEDIATE")
                .isCorrect(false)
                .question(question2)
                .build();
        answerOptionRepository.save(option2_3);

        log.info("Созданы варианты ответов: 6 вариантов");

        // 13. Выводим статистику
        log.info("=== Статистика загруженных данных ===");
        log.info("Курсов: {}", courseRepository.count());
        log.info("Модулей: {}", moduleRepository.count());
        log.info("Уроков: {}", lessonRepository.count());
        log.info("Заданий: {}", assignmentRepository.count());
        log.info("Квизов: {}", quizRepository.count());
        log.info("Вопросов: {}", questionRepository.count());
        log.info("Вариантов ответов: {}", answerOptionRepository.count());
        log.info("=== Загрузка демо-данных завершена ===");
    }
}

