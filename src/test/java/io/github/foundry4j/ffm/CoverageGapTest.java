package io.github.foundry4j.ffm;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Targeted tests that cover the remaining JaCoCo instruction gaps —
/// unaligned primitives, bulk putLongs, FFM.slice(MemoryLayout), the
/// StructBuilder.alignedTo path, PooledArena.scope, and the ByteBufferBridge
/// direct-buffer copy branches.
class CoverageGapTest {

    // -- MemorySlices: unaligned primitives (all had 0 coverage) -----------

    @Test
    void unalignedGetAndPut() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(128, 1);

            // ---- unaligned variants: offsets intentionally NOT aligned ----
            MemorySlices.putShortUnaligned(seg, 1, (short) 0xABCD);
            assertEquals((short) 0xABCD, MemorySlices.getShortUnaligned(seg, 1));

            MemorySlices.putIntUnaligned(seg, 3, 0x11223344);
            assertEquals(0x11223344, MemorySlices.getIntUnaligned(seg, 3));

            MemorySlices.putLongUnaligned(seg, 7, 0xFEEDFACEDEADBEEFL);
            assertEquals(0xFEEDFACEDEADBEEFL, MemorySlices.getLongUnaligned(seg, 7));

            MemorySlices.putFloatUnaligned(seg, 15, 123.25f);
            assertEquals(123.25f, MemorySlices.getFloatUnaligned(seg, 15), 0.001f);

            MemorySlices.putDoubleUnaligned(seg, 19, 3.1415926);
            assertEquals(3.1415926, MemorySlices.getDoubleUnaligned(seg, 19), 1e-6);

            MemorySlices.putCharUnaligned(seg, 27, 'Z');
            assertEquals('Z', MemorySlices.getCharUnaligned(seg, 27));

            // ---- aligned variants: offsets are powers-of-two aligned ----
            MemorySlices.putByte(seg, 0, (byte) 7);
            assertEquals((byte) 7, MemorySlices.getByte(seg, 0));

            MemorySlices.putShort(seg, 32, (short) 999);
            assertEquals((short) 999, MemorySlices.getShort(seg, 32));

            MemorySlices.putInt(seg, 40, 0xCAFEBABE);
            assertEquals(0xCAFEBABE, MemorySlices.getInt(seg, 40));

            MemorySlices.putLong(seg, 48, 42L);
            assertEquals(42L, MemorySlices.getLong(seg, 48));

            MemorySlices.putFloat(seg, 56, 1.5f);
            assertEquals(1.5f, MemorySlices.getFloat(seg, 56), 0.001f);

            MemorySlices.putDouble(seg, 64, 2.5);
            assertEquals(2.5, MemorySlices.getDouble(seg, 64), 0.001);

            MemorySlices.putChar(seg, 72, 'Q');
            assertEquals('Q', MemorySlices.getChar(seg, 72));

            // boolean uses 1-byte aligned
            MemorySlices.putBoolean(seg, 80, true);
            assertTrue(MemorySlices.getBoolean(seg, 80));
        }
    }

    @Test
    void bulkPutLongs() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(32, 8);
            long[] values = {1, 2, 3, 4};
            MemorySlices.putLongs(seg, 0, values);
            assertEquals(3L, MemorySlices.getLong(seg, 16));
            assertEquals(4L, MemorySlices.getLong(seg, 24));
        }
    }

    // -- FFM.slice(MemorySegment, MemoryLayout, long) ---------------------

    @Test
    void sliceWithLayout() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(32, 1);
            seg.set(ValueLayout.JAVA_INT, 0, 777);
            seg.set(ValueLayout.JAVA_INT, 8, 999);
            MemorySegment s = FFM.slice(seg, ValueLayout.JAVA_INT, 8);
            assertEquals(4L, s.byteSize());
            assertEquals(999, s.get(ValueLayout.JAVA_INT, 0));
        }
    }

    // -- StructBuilder.alignedTo -------------------------------------------

    @Test
    void alignedToBuild() {
        MemoryLayout layout = StructLayouts.struct()
                .field("a", ValueLayout.JAVA_INT)
                .alignedTo(16)
                .build();
        assertEquals(16L, layout.byteAlignment());
    }

    @Test
    void alignedToRejectsBadValues() {
        assertThrows(IllegalArgumentException.class,
                () -> StructLayouts.struct().alignedTo(0));
        assertThrows(IllegalArgumentException.class,
                () -> StructLayouts.struct().alignedTo(-1));
        assertThrows(IllegalArgumentException.class,
                () -> StructLayouts.struct().alignedTo(3)); // not power of two
    }

    // -- MemoryArenaPool.PooledArena.scope ---------------------------------

    @Test
    void pooledArenaScopeAndClose() {
        Arena arena = MemoryArenaPool.acquire();
        assertNotNull(arena.scope());
        MemorySegment seg = arena.allocate(16, 4);
        assertNotNull(seg);
        arena.close(); // returns to pool

        Arena sameThreadAgain = MemoryArenaPool.acquire();
        assertNotNull(sameThreadAgain);
        sameThreadAgain.close();
    }

    @Test
    void pooledArenaRejectsDoubleClose() {
        Arena arena = MemoryArenaPool.acquire();
        arena.close();
        assertThrows(IllegalStateException.class, arena::close);
    }

    @Test
    void pooledArenaRejectsAllocationAfterClose() {
        Arena arena = MemoryArenaPool.acquire();
        arena.close();
        assertThrows(IllegalStateException.class, () -> arena.allocate(1, 1));
    }

    // -- MemoryArenaPool MAX_PER_THREAD exhaustion path --------------------

    @Test
    void exceedingMaxPerThreadClosesImmediately() {
        MemoryArenaPool.closeCurrentThread();
        for (int i = 0; i < MemoryArenaPool.MAX_PER_THREAD + 2; i++) {
            Arena arena = MemoryArenaPool.acquire();
            arena.close();
        }
        MemoryArenaPool.closeCurrentThread();
    }

    // -- ByteBufferBridge direct (non-heap) copy paths --------------------

    @Test
    void copyFromDirectBufferIntoSegment() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(8, 1);
            ByteBuffer direct = ByteBufferBridge.allocateDirectByteBuffer(arena, 4);
            direct.put(new byte[]{10, 20, 30, 40});
            direct.flip();
            ByteBufferBridge.copyFromByteBuffer(direct, seg, 0);
            assertEquals(10, seg.get(ValueLayout.JAVA_BYTE, 0));
            assertEquals(40, seg.get(ValueLayout.JAVA_BYTE, 3));
        }
    }

    @Test
    void copyFromSegmentIntoDirectBuffer() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(4, 1);
            seg.set(ValueLayout.JAVA_BYTE, 0, (byte) 1);
            seg.set(ValueLayout.JAVA_BYTE, 1, (byte) 2);
            seg.set(ValueLayout.JAVA_BYTE, 2, (byte) 3);
            seg.set(ValueLayout.JAVA_BYTE, 3, (byte) 4);
            ByteBuffer direct = ByteBufferBridge.allocateDirectByteBuffer(arena, 16);
            ByteBufferBridge.copyToByteBuffer(seg, 0, direct);
            direct.flip();
            assertTrue(direct.remaining() >= 4);
            assertEquals((byte) 1, direct.get());
            assertEquals((byte) 2, direct.get());
            assertEquals((byte) 3, direct.get());
            assertEquals((byte) 4, direct.get());
        }
    }

    @Test
    void globalDirectByteBuffer() {
        ByteBuffer buf = ByteBufferBridge.globalDirectByteBuffer(8);
        assertNotNull(buf);
        assertEquals(8, buf.capacity());
    }

    // -- NativeStrings pooled path -----------------------------------------

    @Test
    void pooledUtf8CStringRoundTrip() {
        MemorySegment seg = NativeStrings.pooledUtf8CString("pooled");
        assertEquals("pooled", NativeStrings.toUtf8String(seg));
    }

    // -- StructLayouts.TypedField: ADDRESS carrier ------------------------

    @Test
    void addressFieldRoundTrip() {
        StructLayouts.MappedRecordFactory factory = StructLayouts.struct()
                .field("ptr", ValueLayout.ADDRESS)
                .buildRecord();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment target = arena.allocate(16, 8);
            StructLayouts.MappedRecord record = factory.map(arena.allocate(8, 8));
            record.set("ptr", target);
            Object back = record.get("ptr");
            assertNotNull(back);
            assertEquals(target.address(), ((MemorySegment) back).address());
        }
    }

    @Test
    void setRejectsWrongType() {
        StructLayouts.MappedRecordFactory factory = StructLayouts.struct()
                .field("x", ValueLayout.JAVA_INT)
                .buildRecord();
        try (Arena arena = Arena.ofConfined()) {
            StructLayouts.MappedRecord record = factory.map(arena.allocate(4, 4));
            assertThrows(IllegalArgumentException.class, () -> record.set("x", "nope"));
        }
    }

    // -- StructLayouts MappedRecord.segment() accessor ---------------------

    @Test
    void mappedRecordSegmentAccessor() {
        StructLayouts.MappedRecordFactory factory = StructLayouts.struct()
                .field("x", ValueLayout.JAVA_INT)
                .buildRecord();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment raw = arena.allocate(4, 4);
            StructLayouts.MappedRecord record = factory.map(raw);
            assertEquals(raw, record.segment());
        }
    }

    // -- StructLayouts field(String, MemoryLayout, long) sequence path -----

    @Test
    void sequenceFieldBuild() {
        MemoryLayout layout = StructLayouts.struct()
                .field("items", ValueLayout.JAVA_INT, 5)
                .build();
        assertEquals(20L, layout.byteSize());
    }

    // -- FFM.toHex and toByteArray edge paths ------------------------------

    @Test
    void toHexZeroBytesAndEmpty() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(4, 1);
            seg.set(ValueLayout.JAVA_BYTE, 0, (byte) 0xAB);
            seg.set(ValueLayout.JAVA_BYTE, 1, (byte) 0xCD);
            assertEquals("", FFM.toHex(seg, 0));
            assertEquals("abcd0000", FFM.toHex(seg, Integer.MAX_VALUE));
            assertEquals("abcd", FFM.toHex(seg, 2));
        }
    }

    @Test
    void toByteArrayRoundTrip() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(3, 1);
            seg.set(ValueLayout.JAVA_BYTE, 0, (byte) 1);
            seg.set(ValueLayout.JAVA_BYTE, 1, (byte) 2);
            seg.set(ValueLayout.JAVA_BYTE, 2, (byte) 3);
            byte[] copy = FFM.toByteArray(seg);
            assertEquals(3, copy.length);
            assertEquals(1, copy[0]);
            assertEquals(3, copy[2]);
        }
    }

    // -- FFM.allocate(SegmentAllocator, MemoryLayout, long) -----------------

    @Test
    void allocateByLayout() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = FFM.allocate(arena, ValueLayout.JAVA_INT, 10);
            assertEquals(40L, seg.byteSize());
            assertTrue(seg.maxByteAlignment() >= 4);
        }
    }

    @Test
    void allocateByLayoutRejectsNegativeCount() {
        try (Arena arena = Arena.ofConfined()) {
            assertThrows(IllegalArgumentException.class,
                    () -> FFM.allocate(arena, ValueLayout.JAVA_INT, -1));
        }
    }
}
