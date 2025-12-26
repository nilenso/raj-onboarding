package com.projectnil.api.service;

import com.projectnil.api.repository.FunctionRepository;
import com.projectnil.common.domain.Function;
import com.projectnil.common.domain.FunctionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing functions.
 */
@Service
@Transactional(readOnly = true)
public class FunctionService {

    private static final Logger LOG = LoggerFactory.getLogger(FunctionService.class);

    private final FunctionRepository functionRepository;

    public FunctionService(FunctionRepository functionRepository) {
        this.functionRepository = functionRepository;
    }

    /**
     * Find a function by ID.
     *
     * @param id the function ID
     * @return the function
     * @throws FunctionNotFoundException if the function is not found
     */
    public Function findById(UUID id) {
        return functionRepository.findById(id)
                .orElseThrow(() -> new FunctionNotFoundException(id));
    }

    /**
     * Find a function by ID and validate it is ready for execution.
     *
     * @param id the function ID
     * @return the function (guaranteed to be in READY status)
     * @throws FunctionNotFoundException if the function is not found
     * @throws FunctionNotReadyException if the function is not in READY status
     */
    public Function findReadyById(UUID id) {
        Function function = findById(id);

        if (function.getStatus() != FunctionStatus.READY) {
            LOG.warn("Attempted to execute function {} with status {}",
                    id, function.getStatus());
            throw new FunctionNotReadyException(id, function.getStatus());
        }

        return function;
    }
}
