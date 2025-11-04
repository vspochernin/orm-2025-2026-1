package ru.vspochernin.gigalearn.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vspochernin.gigalearn.entity.Course;
import ru.vspochernin.gigalearn.entity.Enrollment;
import ru.vspochernin.gigalearn.entity.User;
import ru.vspochernin.gigalearn.exception.DuplicateEnrollmentException;
import ru.vspochernin.gigalearn.repository.CourseRepository;
import ru.vspochernin.gigalearn.repository.EnrollmentRepository;
import ru.vspochernin.gigalearn.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public long enrollStudent(long courseId, long studentId) {
        // Проверяем существование курса
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        // Проверяем существование студента
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + studentId));

        // Проверяем, не записан ли студент уже на этот курс
        if (enrollmentRepository.findByUserIdAndCourseId(studentId, courseId).isPresent()) {
            throw new DuplicateEnrollmentException(
                    String.format("Student %d is already enrolled in course %d", studentId, courseId)
            );
        }

        // Создаем новую запись
        Enrollment enrollment = Enrollment.builder()
                .user(student)
                .course(course)
                .enrollDate(LocalDate.now())
                .status("Active")
                .build();

        try {
            Enrollment saved = enrollmentRepository.save(enrollment);
            return saved.getId();
        } catch (DataIntegrityViolationException e) {
            // На случай race condition - если между проверкой и сохранением успели записать
            throw new DuplicateEnrollmentException(
                    String.format("Student %d is already enrolled in course %d", studentId, courseId),
                    e
            );
        }
    }

    @Transactional
    public boolean unenrollStudent(long courseId, long studentId) {
        // Ищем запись о записи на курс
        return enrollmentRepository.findByUserIdAndCourseId(studentId, courseId)
                .map(enrollment -> {
                    enrollmentRepository.delete(enrollment);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesForStudent(long studentId) {
        // Проверяем существование студента
        if (!userRepository.existsById(studentId)) {
            throw new IllegalArgumentException("User not found: " + studentId);
        }

        // Получаем все записи студента и извлекаем курсы
        // Инициализируем базовые поля курса внутри транзакции
        return enrollmentRepository.findByUserId(studentId).stream()
                .map(enrollment -> {
                    Course course = enrollment.getCourse();
                    // Инициализируем основные поля для использования вне транзакции
                    course.getId();
                    course.getTitle();
                    course.getDescription();
                    return course;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<User> getStudentsForCourse(long courseId) {
        // Проверяем существование курса
        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("Course not found: " + courseId);
        }

        // Получаем все записи курса и извлекаем студентов
        // Инициализируем базовые поля пользователя внутри транзакции
        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(enrollment -> {
                    User user = enrollment.getUser();
                    // Инициализируем основные поля для использования вне транзакции
                    user.getId();
                    user.getName();
                    user.getEmail();
                    return user;
                })
                .toList();
    }
}

