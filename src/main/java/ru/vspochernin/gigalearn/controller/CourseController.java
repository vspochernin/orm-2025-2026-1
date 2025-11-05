package ru.vspochernin.gigalearn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.vspochernin.gigalearn.dto.*;
import ru.vspochernin.gigalearn.entity.Course;
import ru.vspochernin.gigalearn.entity.Enrollment;
import ru.vspochernin.gigalearn.entity.Tag;
import ru.vspochernin.gigalearn.repository.EnrollmentRepository;
import ru.vspochernin.gigalearn.repository.TagRepository;
import ru.vspochernin.gigalearn.repository.UserRepository;
import ru.vspochernin.gigalearn.service.CourseService;
import ru.vspochernin.gigalearn.service.EnrollmentService;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final TagRepository tagRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CourseResponseDto createCourse(@Valid @RequestBody CourseCreateDto dto) {
        Course course = courseService.createCourse(
                dto.getTitle(),
                dto.getDescription(),
                dto.getCategoryId(),
                dto.getTeacherId(),
                null,
                null
        );

        // Добавляем теги, если указаны
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            Set<Tag> tags = new HashSet<>();
            for (Long tagId : dto.getTagIds()) {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new IllegalArgumentException("Tag not found: " + tagId));
                tags.add(tag);
            }
            course.getTags().addAll(tags);
        }

        // Строим полный DTO для ответа
        return CourseResponseDto.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .duration(course.getDuration())
                .startDate(course.getStartDate())
                .categoryName(course.getCategory().getName())
                .teacherName(course.getTeacher().getName())
                .tagNames(course.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toList()))
                .build();
    }

    @GetMapping("/{id}")
    public CourseResponseDto getCourse(@PathVariable Long id) {
        Course course = courseService.getCourseWithContent(id);

        return CourseResponseDto.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .duration(course.getDuration())
                .startDate(course.getStartDate())
                .categoryName(course.getCategory().getName())
                .teacherName(course.getTeacher().getName())
                .tagNames(course.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toList()))
                .build();
    }

    @PostMapping("/{id}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ModuleResponseDto addModule(@PathVariable Long id, @Valid @RequestBody ModuleCreateDto dto) {
        Long moduleId = courseService.addModule(
                id,
                dto.getTitle(),
                dto.getDescription(),
                dto.getOrderIndex()
        );

        ru.vspochernin.gigalearn.entity.Module module = courseService.getCourseById(id).getModules().stream()
                .filter(m -> m.getId().equals(moduleId))
                .findFirst()
                .orElseThrow();

        return ModuleResponseDto.builder()
                .id(module.getId())
                .title(module.getTitle())
                .description(module.getDescription())
                .orderIndex(module.getOrderIndex())
                .courseId(module.getCourse().getId())
                .courseTitle(module.getCourse().getTitle())
                .build();
    }

    @PostMapping("/{id}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public EnrollmentResponseDto enrollStudent(@PathVariable Long id, @RequestParam Long userId) {
        long enrollmentId = enrollmentService.enrollStudent(id, userId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));

        return EnrollmentResponseDto.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getUser().getId())
                .studentName(enrollment.getUser().getName())
                .courseId(enrollment.getCourse().getId())
                .courseTitle(enrollment.getCourse().getTitle())
                .enrollDate(enrollment.getEnrollDate())
                .status(enrollment.getStatus())
                .build();
    }

    @DeleteMapping("/{id}/enroll")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenrollStudent(@PathVariable Long id, @RequestParam Long userId) {
        boolean removed = enrollmentService.unenrollStudent(id, userId);
        if (!removed) {
            throw new IllegalArgumentException(
                    String.format("Student %d is not enrolled in course %d", userId, id)
            );
        }
    }
}

