package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}