package ru.vspochernin.gigalearn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.vspochernin.gigalearn.dto.IdResponseDto;
import ru.vspochernin.gigalearn.dto.LessonCreateDto;
import ru.vspochernin.gigalearn.dto.QuizCreateDto;
import ru.vspochernin.gigalearn.service.ModuleService;
import ru.vspochernin.gigalearn.service.QuizService;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;
    private final QuizService quizService;

    @PostMapping("/{id}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponseDto addLesson(@PathVariable Long id, @Valid @RequestBody LessonCreateDto dto) {
        Long lessonId = moduleService.addLesson(
                id,
                dto.getTitle(),
                dto.getContent(),
                dto.getVideoUrl()
        );
        return IdResponseDto.builder().id(lessonId).build();
    }

    @PostMapping("/{id}/quiz")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponseDto createQuiz(@PathVariable Long id, @Valid @RequestBody QuizCreateDto dto) {
        Long quizId = quizService.createQuiz(
                id,
                dto.getTitle(),
                dto.getTimeLimitSeconds()
        );
        return IdResponseDto.builder().id(quizId).build();
    }
}

