package ru.vspochernin.gigalearn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vspochernin.gigalearn.entity.Quiz;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Optional<Quiz> findByModuleId(Long moduleId);
}

