package org.alexstpm.fms.category.repository;

import org.alexstpm.fms.category.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByParent_Id(Long parentId);
}
