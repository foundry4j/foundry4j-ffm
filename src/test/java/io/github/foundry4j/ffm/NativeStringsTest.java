package io.github.foundry4j.ffm;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Tests for [NativeStrings].
class NativeStringsTest {

    @Test
    void roundTripUtf8CString() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nativeStr = NativeStrings.writeUtf8CString(arena, "hello");
            // 5 content bytes + 1 null terminator
            assertEquals(6L, nativeStr.byteSize());
            assertEquals("hello", NativeStrings.toUtf8String(nativeStr));
        }
    }

    @Test
    void nullByteIndexAndLength() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nativeStr = NativeStrings.writeUtf8CString(arena, "hi");
            assertEquals(2L, NativeStrings.nullByteIndex(nativeStr));
            assertEquals(2L, NativeStrings.cStringLength(nativeStr));
        }
    }

    @Test
    void toUtf8StringHandlesNoTerminator() {
        // A segment with no null byte should decode the whole segment.
        MemorySegment seg = FFM.from("abc".getBytes(StandardCharsets.UTF_8));
        assertEquals("abc", NativeStrings.toUtf8String(seg));
    }

    @Test
    void toUtf8StringHandlesEmptyAndNullStart() {
        assertEquals("", NativeStrings.toUtf8String(FFM.EMPTY));
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(1, 1);
            seg.set(ValueLayout.JAVA_BYTE, 0, (byte) 0);
            assertEquals("", NativeStrings.toUtf8String(seg));
        }
    }

    @Test
    void writeToExistingSegmentOverwritesContent() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(8, 1);
            seg.fill((byte) 0xFF);
            NativeStrings.writeUtf8CStringTo(seg, "hi");
            assertEquals("hi", NativeStrings.toUtf8String(seg));
        }
    }

    @Test
    void writeToSegmentTooSmallFails() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(2, 1);
            assertThrows(IndexOutOfBoundsException.class,
                    () -> NativeStrings.writeUtf8CStringTo(seg, "toolong"));
        }
    }
}
