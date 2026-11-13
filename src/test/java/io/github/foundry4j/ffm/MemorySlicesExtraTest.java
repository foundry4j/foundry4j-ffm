package io.github.foundry4j.ffm;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// Tests for MemorySlices address accessors and remaining carriers.
class MemorySlicesExtraTest {

    @Test
    void addressRawRoundTrip() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(8, 8);
            MemorySegment pointer = arena.allocate(16, 8);
            MemorySlices.putAddress(seg, 0, pointer);
            MemorySegment back = MemorySlices.getAddress(seg, 0);
            assertNotNull(back);
            // MemorySegment ofAddress returns a segment with address set
            assertEquals(pointer.address(), back.address());
        }
    }

    @Test
    void addressRawLong() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(8, 8);
            long ptr = 0x12345678L;
            MemorySlices.putAddressRaw(seg, 0, ptr);
            assertEquals(ptr, MemorySlices.getAddressRaw(seg, 0));
        }
    }

    @Test
    void bulkShorts() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(8, 2);
            MemorySlices.putShorts(seg, 0, new short[]{1, 2, 3, 4});
            assertEquals(3, MemorySlices.getShort(seg, 4));
        }
    }

    @Test
    void bulkBytes() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(4, 1);
            byte[] src = {9, 8, 7, 6};
            MemorySlices.putBytes(seg, 0, src);
            byte[] dst = new byte[4];
            MemorySlices.getBytes(seg, 0, dst);
            for (int i = 0; i < 4; i++) assertEquals(src[i], dst[i]);
        }
    }
}
