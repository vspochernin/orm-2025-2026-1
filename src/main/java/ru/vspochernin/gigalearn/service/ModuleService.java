package ru.vspochernin.gigalearn.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vspochernin.gigalearn.entity.Lesson;
import ru.vspochernin.gigalearn.repository.LessonRepository;
import ru.vspochernin.gigalearn.repository.ModuleRepository;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;

    @Transactional
    public Long addLesson(Long moduleId, String title, String content, String videoUrl) {
        ru.vspochernin.gigalearn.entity.Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));

        Lesson lesson = Lesson.builder()
                .title(title)
                .content(content)
                .videoUrl(videoUrl)
                .module(module)
                .build();

        Lesson savedLesson = lessonRepository.save(lesson);
        return savedLesson.getId();
    }

    @Transactional
    public void deleteModule(Long id) {
        if (!moduleRepository.existsById(id)) {
            throw new IllegalArgumentException("Module not found: " + id);
        }
        // Каскад и orphanRemoval должны автоматически удалить связанные Lesson и Assignment
        moduleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ru.vspochernin.gigalearn.entity.Module getModuleById(Long id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + id));
    }
}

