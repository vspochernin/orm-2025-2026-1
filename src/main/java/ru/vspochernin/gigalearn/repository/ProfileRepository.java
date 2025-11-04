package ru.vspochernin.gigalearn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vspochernin.gigalearn.entity.Profile;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
}

