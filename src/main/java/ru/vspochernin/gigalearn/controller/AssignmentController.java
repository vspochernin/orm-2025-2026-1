package ru.vspochernin.gigalearn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.vspochernin.gigalearn.dto.IdResponseDto;
import ru.vspochernin.gigalearn.dto.SubmissionCreateDto;
import ru.vspochernin.gigalearn.service.SubmissionService;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final SubmissionService submissionService;

    @PostMapping("/{id}/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponseDto submitAssignment(@PathVariable Long id, @Valid @RequestBody SubmissionCreateDto dto) {
        Long submissionId = submissionService.submit(
                dto.getStudentId(),
                id,
                dto.getContent()
        );
        return IdResponseDto.builder().id(submissionId).build();
    }
}

