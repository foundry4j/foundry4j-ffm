package io.github.foundry4j.ffm;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests for [MemorySlices].
class MemorySlicesTest {

    @Test
    void intPutGet() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(8, 4);
            MemorySlices.putInt(seg, 0, 42);
            MemorySlices.putInt(seg, 4, -7);
            assertEquals(42, MemorySlices.getInt(seg, 0));
            assertEquals(-7, MemorySlices.getInt(seg, 4));
        }
    }

    @Test
    void longPutGet() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(8, 8);
            MemorySlices.putLong(seg, 0, 0xDEADBEEFL);
            assertEquals(0xDEADBEEFL, MemorySlices.getLong(seg, 0));
        }
    }

    @Test
    void booleanPutGet() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(1, 1);
            MemorySlices.putBoolean(seg, 0, true);
            assertTrue(MemorySlices.getBoolean(seg, 0));
            MemorySlices.putBoolean(seg, 0, false);
            assertFalse(MemorySlices.getBoolean(seg, 0));
        }
    }

    @Test
    void floatDoubleUnaligned() {
        try (Arena arena = Arena.ofConfined()) {
            // Place a float at offset 3 (not 4-aligned).
            MemorySegment seg = arena.allocate(8, 1);
            MemorySlices.putFloatUnaligned(seg, 3, 3.14f);
            assertEquals(3.14f, MemorySlices.getFloatUnaligned(seg, 3), 0.0001f);
        }
    }

    @Test
    void bulkArrayRoundTrip() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(16, 8);
            MemorySlices.putInts(seg, 0, new int[]{1, 2, 3, 4});
            assertEquals(1, MemorySlices.getInt(seg, 0));
            assertEquals(2, MemorySlices.getInt(seg, 4));
            assertEquals(3, MemorySlices.getInt(seg, 8));
            assertEquals(4, MemorySlices.getInt(seg, 12));

            byte[] bytes = new byte[16];
            MemorySlices.getBytes(seg, 0, bytes);
            assertEquals(1, bytes[0]); // little-endian on x86: low byte first
        }
    }

    @Test
    void nullSegmentThrows() {
        assertThrows(NullPointerException.class,
                () -> MemorySlices.getInt(null, 0));
    }
}
