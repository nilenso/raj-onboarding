package com.projectnil.api.repository;

import com.projectnil.common.domain.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Execution entities.
 */
@Repository
public interface ExecutionRepository extends JpaRepository<Execution, UUID> {

    /**
     * Find all executions for a given function, ordered by creation time descending.
     *
     * @param functionId the function ID
     * @return list of executions
     */
    List<Execution> findByFunctionIdOrderByCreatedAtDesc(UUID functionId);
}
