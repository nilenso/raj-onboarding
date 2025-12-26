package com.projectnil.api.repository;

import com.projectnil.common.domain.Function;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for Function entities.
 */
@Repository
public interface FunctionRepository extends JpaRepository<Function, UUID> {
}
