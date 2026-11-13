package io.github.foundry4j.ffm;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests for [FFM].
class FFMTest {

    @Test
    void allocateRespectsSizeAndAlignment() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = FFM.allocate(arena, 32, 4);
            assertEquals(32L, seg.byteSize());
            // Note: maxByteAlignment may be higher than the requested alignment
            // because the OS allocator is free to use a larger alignment for the
            // underlying page. We only verify that the segment is at least as
            // well aligned as requested.
            assertTrue(seg.maxByteAlignment() >= 4L);
        }
    }

    @Test
    void allocateWithLayout() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = FFM.allocate(arena, ValueLayout.JAVA_INT, 4);
            assertEquals(16L, seg.byteSize());
        }
    }

    @Test
    void allocateRejectsNegativeSize() {
        try (Arena arena = Arena.ofConfined()) {
            assertThrows(IllegalArgumentException.class, () -> FFM.allocate(arena, -1));
        }
    }

    @Test
    void allocateRejectsBadAlignment() {
        try (Arena arena = Arena.ofConfined()) {
            assertThrows(IllegalArgumentException.class, () -> FFM.allocate(arena, 8, 3));
            assertThrows(IllegalArgumentException.class, () -> FFM.allocate(arena, 8, 0));
        }
    }

    @Test
    void fillAndClear() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = FFM.allocate(arena, 4);
            FFM.fill(seg, (byte) 0xAB);
            assertEquals((byte) 0xAB, seg.get(ValueLayout.JAVA_BYTE, 0));
            assertEquals((byte) 0xAB, seg.get(ValueLayout.JAVA_BYTE, 3));
            FFM.clear(seg);
            assertEquals(0, seg.get(ValueLayout.JAVA_INT, 0));
        }
    }

    @Test
    void copyArrayToSegmentRoundTrips() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = FFM.allocate(arena, 4);
            byte[] src = {1, 2, 3, 4};
            FFM.copy(src, seg, 0);
            byte[] dst = FFM.toByteArray(seg);
            assertArrayEquals(src, dst);
        }
    }

    @Test
    void copySegmentToArrayRoundTrips() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = FFM.allocate(arena, 4);
            seg.set(ValueLayout.JAVA_BYTE, 0, (byte) 1);
            seg.set(ValueLayout.JAVA_BYTE, 1, (byte) 2);
            seg.set(ValueLayout.JAVA_BYTE, 2, (byte) 3);
            seg.set(ValueLayout.JAVA_BYTE, 3, (byte) 4);
            byte[] dst = new byte[4];
            FFM.copy(seg, 0, dst);
            assertArrayEquals(new byte[]{1, 2, 3, 4}, dst);
        }
    }

    @Test
    void mismatchReturnsFirstDifference() {
        try (Arena _ = Arena.ofConfined()) {
            MemorySegment a = FFM.from(new byte[]{1, 2, 3, 4});
            MemorySegment b = FFM.from(new byte[]{1, 2, 9, 4});
            assertEquals(2L, FFM.mismatch(a, b));
            assertEquals(-1L, FFM.mismatch(a, FFM.from(new byte[]{1, 2, 3, 4})));
        }
    }

    @Test
    void equalAndToString() {
        try (Arena _ = Arena.ofConfined()) {
            MemorySegment a = FFM.from(new byte[]{1, 2, 3});
            MemorySegment b = FFM.from(new byte[]{1, 2, 3});
            MemorySegment c = FFM.from(new byte[]{1, 2, 4});
            assertTrue(FFM.equal(a, b));
            assertFalse(FFM.equal(a, c));
            assertFalse(FFM.equal(a, FFM.from(new byte[]{1, 2, 3, 0})));
        }
    }

    @Test
    void toHex() {
        assertEquals("", FFM.toHex(FFM.EMPTY));
        assertEquals("48656c6c6f", FFM.toHex(FFM.from("Hello".getBytes())));
    }

    @Test
    void tailReturnsSlice() {
        MemorySegment seg = FFM.from(new byte[]{1, 2, 3, 4, 5});
        MemorySegment tail = FFM.tail(seg, 2);
        assertEquals(3L, tail.byteSize());
        assertEquals((byte) 3, tail.get(ValueLayout.JAVA_BYTE, 0));
    }

    @Test
    void fromReturnsZeroCopySegment() {
        int[] src = {10, 20, 30};
        MemorySegment seg = FFM.from(src);
        assertEquals(12L, seg.byteSize());
        assertEquals(20, seg.get(ValueLayout.JAVA_INT, 4));
    }
}
