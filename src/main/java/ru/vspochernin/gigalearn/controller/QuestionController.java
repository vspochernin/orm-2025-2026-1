package ru.vspochernin.gigalearn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.vspochernin.gigalearn.dto.AnswerOptionCreateDto;
import ru.vspochernin.gigalearn.dto.IdResponseDto;
import ru.vspochernin.gigalearn.service.QuizService;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuizService quizService;

    @PostMapping("/{id}/options")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponseDto addAnswerOption(@PathVariable Long id, @Valid @RequestBody AnswerOptionCreateDto dto) {
        Long optionId = quizService.addAnswerOption(id, dto.getText(), dto.getIsCorrect());
        return IdResponseDto.builder().id(optionId).build();
    }
}

