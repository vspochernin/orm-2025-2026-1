package ru.vspochernin.gigalearn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.vspochernin.gigalearn.dto.*;
import ru.vspochernin.gigalearn.entity.Question;
import ru.vspochernin.gigalearn.entity.Quiz;
import ru.vspochernin.gigalearn.entity.QuizSubmission;
import ru.vspochernin.gigalearn.repository.QuestionRepository;
import ru.vspochernin.gigalearn.service.QuizService;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final QuestionRepository questionRepository;

    @PostMapping("/{id}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public QuestionResponseDto addQuestion(@PathVariable Long id, @Valid @RequestBody QuestionCreateDto dto) {
        Long questionId = quizService.addQuestion(id, dto.getText());

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        return QuestionResponseDto.builder()
                .id(question.getId())
                .text(question.getText())
                .quizId(question.getQuiz().getId())
                .quizTitle(question.getQuiz().getTitle())
                .build();
    }

    @PostMapping("/{id}/take")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public QuizSubmissionResponseDto takeQuiz(@PathVariable Long id, @Valid @RequestBody TakeQuizDto dto) {
        QuizSubmission submission = quizService.takeQuiz(
                dto.getStudentId(),
                id,
                dto.getAnswersByQuestion()
        );

        Quiz quiz = submission.getQuiz();
        int totalQuestions = quiz.getQuestions().size();

        return QuizSubmissionResponseDto.builder()
                .id(submission.getId())
                .studentId(submission.getStudent().getId())
                .studentName(submission.getStudent().getName())
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .score(submission.getScore())
                .totalQuestions(totalQuestions)
                .takenAt(submission.getTakenAt())
                .build();
    }
}

