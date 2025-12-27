package com.projectnil.api.service;

import com.projectnil.api.messaging.PgmqClient;
import com.projectnil.api.repository.FunctionRepository;
import com.projectnil.api.web.FunctionRequest;
import com.projectnil.api.web.FunctionResponse;
import com.projectnil.common.domain.Function;
import com.projectnil.common.domain.FunctionStatus;
import com.projectnil.common.domain.queue.CompilationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing functions.
 */
@Service
@Transactional(readOnly = true)
public class FunctionService {

    private static final Logger LOG = LoggerFactory.getLogger(FunctionService.class);

    /**
     * Supported languages for Phase 0.
     */
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("assemblyscript");

    private final FunctionRepository functionRepository;
    private final PgmqClient pgmqClient;

    public FunctionService(FunctionRepository functionRepository, PgmqClient pgmqClient) {
        this.functionRepository = functionRepository;
        this.pgmqClient = pgmqClient;
    }

    /**
     * Create a new function and enqueue it for compilation.
     *
     * <p>Per scope/flows.md Flow 1:
     * <ol>
     *   <li>Validate request (language must be supported)</li>
     *   <li>Save function with status PENDING</li>
     *   <li>Publish CompilationJob to queue</li>
     *   <li>Return FunctionResponse</li>
     * </ol>
     *
     * @param request the function creation request
     * @return the created function response
     * @throws UnsupportedLanguageException if language is not supported
     */
    @Transactional
    public FunctionResponse create(FunctionRequest request) {
        validateLanguage(request.language());

        Function function = Function.builder()
                .name(request.name())
                .description(request.description())
                .language(request.language())
                .source(request.source())
                .status(FunctionStatus.PENDING)
                .build();

        function = functionRepository.save(function);

        CompilationJob job = new CompilationJob(
                function.getId(),
                function.getLanguage(),
                function.getSource()
        );
        pgmqClient.publishJob(job);

        LOG.info("function.created id={} name={} language={}",
                function.getId(), function.getName(), function.getLanguage());

        return toResponse(function);
    }

    /**
     * Find all functions.
     *
     * @return list of all functions
     */
    public List<FunctionResponse> findAll() {
        return functionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
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

    /**
     * Delete a function by ID.
     *
     * @param id the function ID
     * @throws FunctionNotFoundException if the function is not found
     */
    @Transactional
    public void delete(UUID id) {
        if (!functionRepository.existsById(id)) {
            throw new FunctionNotFoundException(id);
        }
        functionRepository.deleteById(id);
        LOG.info("function.deleted id={}", id);
    }

    private void validateLanguage(String language) {
        if (language == null || !SUPPORTED_LANGUAGES.contains(language.toLowerCase())) {
            throw new UnsupportedLanguageException(language, SUPPORTED_LANGUAGES);
        }
    }

    private FunctionResponse toResponse(Function function) {
        return new FunctionResponse(
                function.getId(),
                function.getName(),
                function.getStatus(),
                function.getCreatedAt()
        );
    }
}
