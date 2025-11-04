package ru.vspochernin.gigalearn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vspochernin.gigalearn.entity.Module;

import java.util.List;

public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findByCourseId(Long courseId);
}

