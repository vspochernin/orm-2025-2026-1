package ru.vspochernin.gigalearn.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vspochernin.gigalearn.entity.Assignment;
import ru.vspochernin.gigalearn.entity.Submission;
import ru.vspochernin.gigalearn.entity.User;
import ru.vspochernin.gigalearn.exception.DuplicateSubmissionException;
import ru.vspochernin.gigalearn.repository.AssignmentRepository;
import ru.vspochernin.gigalearn.repository.SubmissionRepository;
import ru.vspochernin.gigalearn.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public long submit(long studentId, long assignmentId, String content) {
        // Проверяем существование студента
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + studentId));

        // Проверяем существование задания
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        // Проверяем, не сдавал ли студент уже это задание
        if (submissionRepository.findByStudentIdAndAssignmentId(studentId, assignmentId).isPresent()) {
            throw new DuplicateSubmissionException(
                    String.format("Student %d has already submitted assignment %d", studentId, assignmentId)
            );
        }

        // Создаем сдачу
        Submission submission = Submission.builder()
                .student(student)
                .assignment(assignment)
                .content(content)
                .submittedAt(OffsetDateTime.now())
                .build();

        try {
            Submission saved = submissionRepository.save(submission);
            return saved.getId();
        } catch (DataIntegrityViolationException e) {
            // На случай race condition - если между проверкой и сохранением успели сдать
            throw new DuplicateSubmissionException(
                    String.format("Student %d has already submitted assignment %d", studentId, assignmentId),
                    e
            );
        }
    }

    @Transactional
    public void grade(long submissionId, int score, String feedback) {
        // Получаем сдачу
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + submissionId));

        // Валидируем score относительно maxScore
        Integer maxScore = submission.getAssignment().getMaxScore();
        if (maxScore != null && score > maxScore) {
            throw new IllegalArgumentException(
                    String.format("Score %d exceeds maximum score %d for this assignment", score, maxScore)
            );
        }

        if (score < 0) {
            throw new IllegalArgumentException("Score cannot be negative");
        }

        // Устанавливаем оценку и отзыв
        submission.setScore(score);
        submission.setFeedback(feedback);

        submissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public List<Submission> getByAssignment(long assignmentId) {
        // Проверяем существование задания
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new IllegalArgumentException("Assignment not found: " + assignmentId);
        }

        // Получаем все сдачи для задания
        // Инициализируем базовые поля внутри транзакции
        return submissionRepository.findByAssignmentId(assignmentId).stream()
                .map(submission -> {
                    // Инициализируем поля для использования вне транзакции
                    submission.getId();
                    submission.getContent();
                    submission.getScore();
                    submission.getSubmittedAt();
                    // Инициализируем поля студента
                    User student = submission.getStudent();
                    student.getId();
                    student.getName();
                    student.getEmail();
                    return submission;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Submission> getByStudent(long studentId) {
        // Проверяем существование студента
        if (!userRepository.existsById(studentId)) {
            throw new IllegalArgumentException("User not found: " + studentId);
        }

        // Получаем все сдачи студента
        // Инициализируем базовые поля внутри транзакции
        return submissionRepository.findByStudentId(studentId).stream()
                .map(submission -> {
                    // Инициализируем поля для использования вне транзакции
                    submission.getId();
                    submission.getContent();
                    submission.getScore();
                    submission.getSubmittedAt();
                    // Инициализируем поля задания
                    Assignment assignment = submission.getAssignment();
                    assignment.getId();
                    assignment.getTitle();
                    assignment.getMaxScore();
                    return submission;
                })
                .toList();
    }
}

