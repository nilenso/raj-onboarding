package com.projectnil.api.service;

import java.util.Set;

/**
 * Exception thrown when an unsupported language is requested.
 */
public class UnsupportedLanguageException extends RuntimeException {

    private final String requestedLanguage;
    private final Set<String> supportedLanguages;

    public UnsupportedLanguageException(String requestedLanguage, Set<String> supportedLanguages) {
        super("Unsupported language: " + requestedLanguage + ". Supported: " + supportedLanguages);
        this.requestedLanguage = requestedLanguage;
        this.supportedLanguages = supportedLanguages;
    }

    public String getRequestedLanguage() {
        return requestedLanguage;
    }

    public Set<String> getSupportedLanguages() {
        return supportedLanguages;
    }
}
