package ru.vspochernin.gigalearn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.vspochernin.gigalearn.dto.AssignmentCreateDto;
import ru.vspochernin.gigalearn.dto.IdResponseDto;
import ru.vspochernin.gigalearn.service.AssignmentService;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final AssignmentService assignmentService;

    @PostMapping("/{id}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponseDto createAssignment(@PathVariable Long id, @Valid @RequestBody AssignmentCreateDto dto) {
        Long assignmentId = assignmentService.createAssignment(
                id,
                dto.getTitle(),
                dto.getDescription(),
                dto.getMaxScore()
        );
        return IdResponseDto.builder().id(assignmentId).build();
    }
}

