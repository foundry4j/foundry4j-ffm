package io.github.foundry4j.ffm;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Edge-case tests for native string handling.
class NativeStringsExtraTest {

    @Test
    void cStringLengthHandlesNoNull() {
        try (Arena _ = Arena.ofConfined()) {
            MemorySegment seg = FFM.from("hello".getBytes());
            assertEquals(5L, NativeStrings.cStringLength(seg));
        }
    }

    @Test
    void nullByteIndexHandlesNoNull() {
        try (Arena _ = Arena.ofConfined()) {
            MemorySegment seg = FFM.from("hello".getBytes());
            assertEquals(-1L, NativeStrings.nullByteIndex(seg));
        }
    }

    @Test
    void customCharset() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = NativeStrings.writeCString(arena, "héllo", StandardCharsets.ISO_8859_1);
            assertEquals("héllo", NativeStrings.toString(seg, StandardCharsets.ISO_8859_1));
        }
    }

    @Test
    void emptyString() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = NativeStrings.writeUtf8CString(arena, "");
            assertEquals(1L, seg.byteSize()); // just null terminator
            assertEquals("", NativeStrings.toUtf8String(seg));
        }
    }

    @Test
    void pooledArenaRoundTrip() {
        try (Arena arena = MemoryArenaPool.acquire()) {
            MemorySegment seg = NativeStrings.writeUtf8CString(arena, "pool");
            assertEquals("pool", NativeStrings.toUtf8String(seg));
        }
    }

    @Test
    void globalArenaAllowsStaticStrings() {
        MemorySegment seg = NativeStrings.globalUtf8CString("global");
        assertEquals("global", NativeStrings.toUtf8String(seg));
    }
}
