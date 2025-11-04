package ru.vspochernin.gigalearn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vspochernin.gigalearn.entity.AnswerOption;

import java.util.List;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {

    List<AnswerOption> findByQuestionId(Long questionId);
}

