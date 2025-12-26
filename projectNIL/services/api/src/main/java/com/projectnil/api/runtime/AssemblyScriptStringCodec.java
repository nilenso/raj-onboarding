package com.projectnil.api.runtime;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * String codec for AssemblyScript-compiled WASM modules.
 * 
 * <p>AssemblyScript uses:
 * <ul>
 *   <li>UTF-16LE encoding for strings</li>
 *   <li>Garbage-collected memory with a 20-byte object header</li>
 *   <li>Runtime exports: __new, __pin, __unpin for memory management</li>
 * </ul>
 * 
 * <p>String layout in memory:
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │  20-byte header (GC metadata)  │  UTF-16LE payload          │
 * ├────────────────────────────────┼─────────────────────────────┤
 * │  ... | rtId (4B) | rtSize (4B) │  string data (rtSize bytes) │
 * └────────────────────────────────┴─────────────────────────────┘
 *          offset -8    offset -4    offset 0 (pointer location)
 * </pre>
 * 
 * @see <a href="https://www.assemblyscript.org/runtime.html">AssemblyScript Runtime</a>
 */
public class AssemblyScriptStringCodec implements WasmStringCodec {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssemblyScriptStringCodec.class);

    /**
     * AssemblyScript class ID for String objects.
     */
    private static final int STRING_CLASS_ID = 2;

    /**
     * Offset from pointer to rtSize field (stores byte length of payload).
     */
    private static final int RT_SIZE_OFFSET = -4;

    @Override
    public void validateExports(Instance instance) throws WasmAbiException {
        validateExport(instance, "__new", "memory allocation");
        validateExport(instance, "__pin", "memory pinning");
        validateExport(instance, "__unpin", "memory unpinning");
    }

    private void validateExport(Instance instance, String name, String purpose) 
            throws WasmAbiException {
        try {
            instance.export(name);
        } catch (Exception e) {
            throw new WasmAbiException(
                "Module missing required export '" + name + "' for " + purpose 
                + ". Ensure module is compiled with --exportRuntime flag.", e);
        }
    }

    @Override
    public int writeString(Instance instance, String value) {
        ExportFunction newFn = instance.export("__new");
        ExportFunction pinFn = instance.export("__pin");
        Memory memory = instance.memory();

        // Convert Java String to UTF-16LE bytes
        byte[] utf16Bytes = value.getBytes(StandardCharsets.UTF_16LE);
        int byteLength = utf16Bytes.length;

        // Allocate memory: __new(size, classId) -> pointer
        long[] allocResult = newFn.apply(byteLength, STRING_CLASS_ID);
        int ptr = (int) allocResult[0];

        // Pin the object to prevent GC during execution
        pinFn.apply(ptr);

        // Write UTF-16LE bytes to memory
        memory.write(ptr, utf16Bytes);

        LOGGER.debug("Wrote string of {} bytes to WASM memory at pointer {}", byteLength, ptr);
        return ptr;
    }

    @Override
    public String readString(Instance instance, int pointer) {
        if (pointer == 0) {
            LOGGER.warn("Received null pointer (0) when reading string");
            return null;
        }

        Memory memory = instance.memory();

        // Read rtSize from 4 bytes before the pointer
        int rtSize = memory.readInt(pointer + RT_SIZE_OFFSET);

        if (rtSize < 0 || rtSize > 10_000_000) {
            // Sanity check: reject obviously invalid sizes (> 10MB)
            LOGGER.warn("Invalid rtSize {} at pointer {}", rtSize, pointer);
            throw new WasmExecutionException(
                "Invalid string size in WASM memory: " + rtSize);
        }

        // Read UTF-16LE bytes using readBytes for efficiency
        byte[] utf16Bytes = memory.readBytes(pointer, rtSize);

        // Decode to Java String
        String result = new String(utf16Bytes, StandardCharsets.UTF_16LE);
        LOGGER.debug("Read string of {} bytes from WASM memory at pointer {}", rtSize, pointer);
        return result;
    }

    @Override
    public void cleanup(Instance instance, int pointer) {
        if (pointer == 0) {
            return;
        }

        try {
            ExportFunction unpinFn = instance.export("__unpin");
            unpinFn.apply(pointer);
            LOGGER.debug("Unpinned memory at pointer {}", pointer);
        } catch (Exception e) {
            // Log but don't fail - cleanup is best-effort
            LOGGER.warn("Failed to unpin memory at pointer {}: {}", pointer, e.getMessage());
        }
    }
}
