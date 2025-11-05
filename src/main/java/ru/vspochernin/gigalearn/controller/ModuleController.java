package ru.vspochernin.gigalearn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.vspochernin.gigalearn.dto.LessonCreateDto;
import ru.vspochernin.gigalearn.dto.LessonResponseDto;
import ru.vspochernin.gigalearn.dto.QuizCreateDto;
import ru.vspochernin.gigalearn.dto.QuizResponseDto;
import ru.vspochernin.gigalearn.entity.Lesson;
import ru.vspochernin.gigalearn.entity.Quiz;
import ru.vspochernin.gigalearn.repository.LessonRepository;
import ru.vspochernin.gigalearn.repository.QuizRepository;
import ru.vspochernin.gigalearn.service.ModuleService;
import ru.vspochernin.gigalearn.service.QuizService;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;
    private final QuizService quizService;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;

    @PostMapping("/{id}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public LessonResponseDto addLesson(@PathVariable Long id, @Valid @RequestBody LessonCreateDto dto) {
        Long lessonId = moduleService.addLesson(
                id,
                dto.getTitle(),
                dto.getContent(),
                dto.getVideoUrl()
        );

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        return LessonResponseDto.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .content(lesson.getContent())
                .videoUrl(lesson.getVideoUrl())
                .moduleId(lesson.getModule().getId())
                .moduleTitle(lesson.getModule().getTitle())
                .build();
    }

    @PostMapping("/{id}/quiz")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public QuizResponseDto createQuiz(@PathVariable Long id, @Valid @RequestBody QuizCreateDto dto) {
        Long quizId = quizService.createQuiz(
                id,
                dto.getTitle(),
                dto.getTimeLimitSeconds()
        );

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        return QuizResponseDto.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .timeLimit(quiz.getTimeLimit())
                .moduleId(quiz.getModule().getId())
                .moduleTitle(quiz.getModule().getTitle())
                .build();
    }
}

