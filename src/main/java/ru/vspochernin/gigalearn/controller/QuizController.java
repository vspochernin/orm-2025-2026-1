package ru.vspochernin.gigalearn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.vspochernin.gigalearn.dto.*;
import ru.vspochernin.gigalearn.entity.QuizSubmission;
import ru.vspochernin.gigalearn.service.QuizService;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/{id}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponseDto addQuestion(@PathVariable Long id, @Valid @RequestBody QuestionCreateDto dto) {
        Long questionId = quizService.addQuestion(id, dto.getText());
        return IdResponseDto.builder().id(questionId).build();
    }

    @PostMapping("/{id}/take")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponseDto takeQuiz(@PathVariable Long id, @Valid @RequestBody TakeQuizDto dto) {
        QuizSubmission submission = quizService.takeQuiz(
                dto.getStudentId(),
                id,
                dto.getAnswersByQuestion()
        );
        return IdResponseDto.builder().id(submission.getId()).build();
    }
}

