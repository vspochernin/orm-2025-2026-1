package ru.vspochernin.gigalearn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.vspochernin.gigalearn.dto.CourseCreateDto;
import ru.vspochernin.gigalearn.dto.CourseResponseDto;
import ru.vspochernin.gigalearn.dto.IdResponseDto;
import ru.vspochernin.gigalearn.dto.ModuleCreateDto;
import ru.vspochernin.gigalearn.entity.Course;
import ru.vspochernin.gigalearn.entity.Tag;
import ru.vspochernin.gigalearn.repository.TagRepository;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponseDto createCourse(@Valid @RequestBody CourseCreateDto dto) {
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

        return IdResponseDto.builder().id(course.getId()).build();
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
    public IdResponseDto addModule(@PathVariable Long id, @Valid @RequestBody ModuleCreateDto dto) {
        Long moduleId = courseService.addModule(
                id,
                dto.getTitle(),
                dto.getDescription(),
                dto.getOrderIndex()
        );
        return IdResponseDto.builder().id(moduleId).build();
    }

    @PostMapping("/{id}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponseDto enrollStudent(@PathVariable Long id, @RequestParam Long userId) {
        long enrollmentId = enrollmentService.enrollStudent(id, userId);
        return IdResponseDto.builder().id(enrollmentId).build();
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

