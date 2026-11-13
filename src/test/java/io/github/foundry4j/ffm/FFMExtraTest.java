package io.github.foundry4j.ffm;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Additional tests for FFM covering the builder's `from` overloads and
/// remaining copy paths.
class FFMExtraTest {

    @Test
    void fromIntArray() {
        MemorySegment seg = FFM.from(new int[]{1, 2, 3});
        assertEquals(12L, seg.byteSize());
        assertEquals(2, seg.get(ValueLayout.JAVA_INT, 4));
    }

    @Test
    void fromLongArray() {
        MemorySegment seg = FFM.from(new long[]{0xABCDL});
        assertEquals(0xABCDL, seg.get(ValueLayout.JAVA_LONG, 0));
    }

    @Test
    void fromFloatArray() {
        MemorySegment seg = FFM.from(new float[]{1.5f, 2.5f});
        assertEquals(1.5f, seg.get(ValueLayout.JAVA_FLOAT, 0), 0.001f);
    }

    @Test
    void fromDoubleArray() {
        MemorySegment seg = FFM.from(new double[]{3.14});
        assertEquals(3.14, seg.get(ValueLayout.JAVA_DOUBLE, 0), 0.001);
    }

    @Test
    void fromCharArray() {
        MemorySegment seg = FFM.from(new char[]{'a', 'b', 'c'});
        assertEquals('b', seg.get(ValueLayout.JAVA_CHAR, 2));
    }

    @Test
    void fromShortArray() {
        MemorySegment seg = FFM.from(new short[]{100, 200});
        assertEquals(200, seg.get(ValueLayout.JAVA_SHORT, 2));
    }

    @Test
    void tailRejectsInvalidOffset() {
        MemorySegment seg = FFM.EMPTY;
        assertThrows(IllegalArgumentException.class, () -> FFM.tail(seg, 1));
        assertThrows(IllegalArgumentException.class, () -> FFM.tail(seg, -1));
    }

    @Test
    void copyBetweenSegments() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment src = FFM.from(new byte[]{10, 20, 30});
            MemorySegment dst = arena.allocate(5, 1);
            FFM.copy(src, 0, dst, 1, 3);
            assertEquals(10, dst.get(ValueLayout.JAVA_BYTE, 1));
            assertEquals(30, dst.get(ValueLayout.JAVA_BYTE, 3));
        }
    }

    @Test
    void allocateWithDefaultAllocator() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(8);
            FFM.clear(seg);
            assertEquals(0, seg.get(ValueLayout.JAVA_INT, 0));
        }
    }

    @Test
    void copyEmptyArrayDoesNothing() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(4, 1);
            seg.set(ValueLayout.JAVA_BYTE, 0, (byte) 9);
            FFM.copy(new byte[0], seg, 0);
            assertEquals(9, seg.get(ValueLayout.JAVA_BYTE, 0));
        }
    }

    @Test
    void mismatchWorks() {
        MemorySegment a = FFM.from(new byte[]{1, 2, 3});
        MemorySegment b = FFM.from(new byte[]{1, 2, 3, 4});
        // FFM's mismatch returns the length of the shorter segment when the
        // longer one starts with the shorter's content — 3 in this case.
        assertEquals(3L, FFM.mismatch(a, b));
        assertFalse(FFM.equal(a, b));
    }
}
