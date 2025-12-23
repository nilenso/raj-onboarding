package com.projectnil.common.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FunctionTest {

    @Test
    void builderCreatesFullyPopulatedFunction() {
        UUID id = UUID.randomUUID();
        byte[] wasm = {1, 2, 3};

        Function function = Function.builder()
                .id(id)
                .name("sum")
                .description("adds numbers")
                .language("assemblyscript")
                .source("export function sum(a: i32, b: i32): i32 { return a + b; }")
                .wasmBinary(wasm)
                .compileError(null)
                .status(FunctionStatus.READY)
                .build();

        assertEquals(id, function.getId());
        assertEquals("sum", function.getName());
        assertEquals("adds numbers", function.getDescription());
        assertEquals("assemblyscript", function.getLanguage());
        assertEquals("export function sum(a: i32, b: i32): i32 { return a + b; }", function.getSource());
        assertArrayEquals(wasm, function.getWasmBinary());
        assertEquals(FunctionStatus.READY, function.getStatus());
    }

    @Test
    void builderAppliesDefaultStatus() {
        Function function = Function.builder()
                .name("sum")
                .language("assemblyscript")
                .source("code")
                .build();

        assertEquals(FunctionStatus.PENDING, function.getStatus());
        assertNull(function.getCompileError());
    }

    @Test
    void protectedNoArgsConstructorExistsForJpa() {
        Function function = new Function();
        assertNotNull(function);
    }
}
