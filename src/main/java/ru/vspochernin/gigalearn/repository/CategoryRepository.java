package ru.vspochernin.gigalearn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vspochernin.gigalearn.entity.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);
}

