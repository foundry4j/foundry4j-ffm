package io.github.foundry4j.ffm;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Additional ByteBufferBridge tests.
class ByteBufferBridgeExtraTest {

    @Test
    void copyRespectsOrderAndPosition() {
        try (Arena arena = Arena.ofConfined()) {
            ByteBuffer bb = ByteBufferBridge.allocateDirectByteBuffer(arena, 4);
            // Write a little-endian int (0x04030201 in memory when LE)
            bb.putInt(0, 0x01020304);
            MemorySegment seg = ByteBufferBridge.asMemorySegment(bb);
            // In native memory we read as per platform; direct segment is native-aligned
            assertEquals(0x01020304, seg.get(ValueLayout.JAVA_INT, 0));
        }
    }

    @Test
    void nullArgumentsThrow() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(4, 1);
            assertThrows(NullPointerException.class, () -> ByteBufferBridge.copyFromByteBuffer(null, seg, 0));
            assertThrows(NullPointerException.class, () -> ByteBufferBridge.copyFromByteBuffer(ByteBuffer.allocate(4), null, 0));
            assertThrows(NullPointerException.class, () -> ByteBufferBridge.copyToByteBuffer(seg, 0, null));
        }
    }
}
