package com.projectnil.api.web;

public record FunctionRequest(
    String name,
    String description,
    String language,
    String source
) {}
