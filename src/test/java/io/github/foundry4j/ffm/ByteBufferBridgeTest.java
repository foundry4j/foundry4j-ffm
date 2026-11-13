package io.github.foundry4j.ffm;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests for [ByteBufferBridge].
class ByteBufferBridgeTest {

    @Test
    void directByteBufferIsBackedByArena() {
        try (Arena arena = Arena.ofConfined()) {
            ByteBuffer bb = ByteBufferBridge.allocateDirectByteBuffer(arena, 8);
            assertEquals(8, bb.capacity());
            bb.putInt(0, 42);
            MemorySegment seg = MemorySegment.ofBuffer(bb);
            assertEquals(42, seg.get(ValueLayout.JAVA_INT, 0));
        }
    }

    @Test
    void copyFromHeapByteBuffer() {
        try (Arena arena = Arena.ofConfined()) {
            ByteBuffer src = ByteBuffer.allocate(4);
            src.put(new byte[]{1, 2, 3, 4});
            src.flip();

            MemorySegment dst = arena.allocate(4, 1);
            ByteBufferBridge.copyFromByteBuffer(src, dst, 0);
            byte[] copy = new byte[4];
            MemorySegment.copy(dst, ValueLayout.JAVA_BYTE, 0L, copy, 0, 4);
            assertArrayEquals(new byte[]{1, 2, 3, 4}, copy);
        }
    }

    @Test
    void copyToHeapByteBuffer() {
        try (Arena _ = Arena.ofConfined()) {
            MemorySegment src = FFM.from(new byte[]{1, 2, 3, 4});
            ByteBuffer dst = ByteBuffer.allocate(6);
            dst.position(2);
            ByteBufferBridge.copyToByteBuffer(src, 0, dst);
            assertEquals(6, dst.position());
            dst.flip();
            byte[] out = new byte[4];
            dst.position(2);
            dst.get(out);
            assertArrayEquals(new byte[]{1, 2, 3, 4}, out);
        }
    }

    @Test
    void copyRespectsRemaining() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment src = arena.allocate(16, 1);
            src.set(ValueLayout.JAVA_BYTE, 0, (byte) 'a');
            src.set(ValueLayout.JAVA_BYTE, 1, (byte) 'b');

            ByteBuffer dst = ByteBuffer.allocate(2);
            ByteBufferBridge.copyToByteBuffer(src, 0, dst);
            assertEquals(2, dst.position());
        }
    }
}
