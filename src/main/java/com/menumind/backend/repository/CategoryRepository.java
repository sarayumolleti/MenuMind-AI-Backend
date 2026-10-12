package com.menumind.backend.repository;

import com.menumind.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CategoryRepository
 * 
 * Provides database operations for Categories.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByIsActiveTrue();

    Optional<Category> findByNameIgnoreCase(String name);

    Boolean existsByNameIgnoreCase(String name);
}
