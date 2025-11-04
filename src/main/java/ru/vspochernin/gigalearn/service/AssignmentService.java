package ru.vspochernin.gigalearn.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vspochernin.gigalearn.entity.Assignment;
import ru.vspochernin.gigalearn.entity.Lesson;
import ru.vspochernin.gigalearn.repository.AssignmentRepository;
import ru.vspochernin.gigalearn.repository.LessonRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final LessonRepository lessonRepository;

    @Transactional
    public long createAssignment(long lessonId, String title, String description, Integer maxScore) {
        // Проверяем существование урока
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonId));

        // Создаем задание
        Assignment assignment = Assignment.builder()
                .title(title)
                .description(description)
                .maxScore(maxScore)
                .lesson(lesson)
                .build();

        Assignment saved = assignmentRepository.save(assignment);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public Assignment getById(long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Assignment> getByLesson(long lessonId) {
        // Проверяем существование урока
        if (!lessonRepository.existsById(lessonId)) {
            throw new IllegalArgumentException("Lesson not found: " + lessonId);
        }

        return assignmentRepository.findByLessonId(lessonId);
    }

    @Transactional
    public void delete(long id) {
        if (!assignmentRepository.existsById(id)) {
            throw new IllegalArgumentException("Assignment not found: " + id);
        }
        // Каскад и orphanRemoval должны автоматически удалить связанные Submission
        assignmentRepository.deleteById(id);
    }
}

