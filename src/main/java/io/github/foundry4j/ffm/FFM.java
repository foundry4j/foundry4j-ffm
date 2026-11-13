package io.github.foundry4j.ffm;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.nio.charset.Charset;
import java.util.Objects;

/// Static entry point for [foundry4j-ffm](https://github.com/foundry4j/foundry4j-ffm).
///
/// ```java
/// try (Arena arena = Arena.ofConfined()) {
///     MemorySegment buffer = FFM.allocate(arena, 1024);
///     FFM.fill(buffer, (byte) 0);
/// }
/// ```
///
/// All methods are stateless. Arena management is the caller's responsibility,
/// and callers are encouraged to use [MemoryArenaPool] on hot paths so that
/// no arena allocation happens per operation.
public final class FFM {

    private FFM() {
    }

    // ---- Allocation -----------------------------------------------------------

    /// Allocates an external memory segment of `byteSize` bytes using the given
    /// allocator with default (1-byte) alignment.
    public static MemorySegment allocate(SegmentAllocator allocator, long byteSize) {
        Objects.requireNonNull(allocator, "allocator must not be null");
        if (byteSize < 0) {
            throw new IllegalArgumentException("byteSize must be non-negative, was " + byteSize);
        }
        return allocator.allocate(byteSize, 1);
    }

    /// Allocates an external memory segment of `byteSize` bytes using the given
    /// allocator and alignment.
    public static MemorySegment allocate(SegmentAllocator allocator, long byteSize, long alignment) {
        Objects.requireNonNull(allocator, "allocator must not be null");
        if (byteSize < 0) {
            throw new IllegalArgumentException("byteSize must be non-negative, was " + byteSize);
        }
        if (alignment <= 0 || (alignment & (alignment - 1)) != 0) {
            throw new IllegalArgumentException(
                    "alignment must be a positive power of two, was " + alignment);
        }
        return allocator.allocate(byteSize, alignment);
    }

    /// Allocates a segment sized to hold `count` elements of the given layout.
    public static MemorySegment allocate(SegmentAllocator allocator, MemoryLayout layout, long count) {
        Objects.requireNonNull(allocator, "allocator must not be null");
        Objects.requireNonNull(layout, "layout must not be null");
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative, was " + count);
        }
        return allocator.allocate(layout.byteSize() * count, layout.byteAlignment());
    }

    // ---- Filling and clearing -------------------------------------------------

    /// Fills `segment` with `value`.
    public static MemorySegment fill(MemorySegment segment, byte value) {
        Objects.requireNonNull(segment, "segment must not be null");
        return segment.fill(value);
    }

    /// Clears `segment` by filling it with zero bytes.
    public static MemorySegment clear(MemorySegment segment) {
        return fill(segment, (byte) 0);
    }

    // ---- Copy helpers ---------------------------------------------------------

    /// Copies `length` bytes from `src[srcOffset..]` into `dst[dstOffset..]`.
    public static void copy(MemorySegment src, long srcOffset,
                            MemorySegment dst, long dstOffset,
                            long length) {
        Objects.requireNonNull(src, "src must not be null");
        Objects.requireNonNull(dst, "dst must not be null");
        MemorySegment.copy(src, srcOffset, dst, dstOffset, length);
    }

    /// Copies a Java `byte[]` into a destination segment.
    public static void copy(byte[] src, MemorySegment dst, long dstOffset) {
        Objects.requireNonNull(src, "src must not be null");
        Objects.requireNonNull(dst, "dst must not be null");
        MemorySegment.copy(src, 0, dst, ValueLayout.JAVA_BYTE, dstOffset, src.length);
    }

    /// Copies bytes from a segment into a Java `byte[]`.
    public static void copy(MemorySegment src, long srcOffset, byte[] dst) {
        Objects.requireNonNull(src, "src must not be null");
        Objects.requireNonNull(dst, "dst must not be null");
        MemorySegment.copy(src, ValueLayout.JAVA_BYTE, srcOffset, dst, 0, dst.length);
    }

    // ---- Array / segment conversions ------------------------------------------

    /// Returns a new `byte[]` copy of the segment's contents.
    public static byte[] toByteArray(MemorySegment segment) {
        Objects.requireNonNull(segment, "segment must not be null");
        long size = segment.byteSize();
        if (size > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("segment too large for byte[]: " + size + " bytes");
        }
        byte[] copy = new byte[(int) size];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, 0L, copy, 0, copy.length);
        return copy;
    }

    /// Returns a new `String` by decoding the whole segment with the given
    /// charset.
    ///
    /// When the caller is certain that the segment holds a C string (a
    /// null-terminated sequence), use [NativeStrings#toUtf8String(MemorySegment)]
    /// instead, which stops at the first null byte.
    public static String toString(MemorySegment segment, Charset charset) {
        Objects.requireNonNull(segment, "segment must not be null");
        Objects.requireNonNull(charset, "charset must not be null");
        return new String(toByteArray(segment), charset);
    }

    /// Returns a slice of `segment` from `offset` (inclusive) to the end of the
    /// segment.
    public static MemorySegment tail(MemorySegment segment, long offset) {
        Objects.requireNonNull(segment, "segment must not be null");
        long size = segment.byteSize();
        if (offset < 0 || offset > size) {
            throw new IllegalArgumentException(
                    "offset out of range: " + offset + " for segment of size " + size);
        }
        return segment.asSlice(offset, size - offset);
    }

    /// Returns a slice of `segment` sized exactly for `layout` starting at
    /// `offset`.
    public static MemorySegment slice(MemorySegment segment, MemoryLayout layout, long offset) {
        Objects.requireNonNull(segment, "segment must not be null");
        Objects.requireNonNull(layout, "layout must not be null");
        return segment.asSlice(offset, layout);
    }

    // ---- Misc helpers ---------------------------------------------------------

    /// Returns the first index where two segments differ, or `-1` when they
    /// are byte-identical.
    public static long mismatch(MemorySegment a, MemorySegment b) {
        Objects.requireNonNull(a, "a must not be null");
        Objects.requireNonNull(b, "b must not be null");
        return a.mismatch(b);
    }

    /// Returns `true` when `a` and `b` are byte-identical.
    public static boolean equal(MemorySegment a, MemorySegment b) {
        Objects.requireNonNull(a, "a must not be null");
        Objects.requireNonNull(b, "b must not be null");
        return a.byteSize() == b.byteSize() && a.mismatch(b) == -1;
    }

    /// Returns a lowercase hex string for up to `maxBytes` bytes of a segment.
    public static String toHex(MemorySegment segment, int maxBytes) {
        Objects.requireNonNull(segment, "segment must not be null");
        if (maxBytes < 0) {
            throw new IllegalArgumentException("maxBytes must be non-negative, was " + maxBytes);
        }
        int bytes = (int) Math.min(segment.byteSize(), maxBytes);
        if (bytes == 0) {
            return "";
        }
        byte[] copy = new byte[bytes];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, 0L, copy, 0, bytes);
        StringBuilder sb = new StringBuilder(bytes * 2);
        for (byte b : copy) {
            sb.append(Character.forDigit((b >>> 4) & 0xf, 16));
            sb.append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }

    /// Returns a lowercase hex string for the whole segment.
    public static String toHex(MemorySegment segment) {
        return toHex(segment, Integer.MAX_VALUE);
    }

    // ---- Factory helpers -----------------------------------------------------

    /// An empty heap-backed segment.
    public static final MemorySegment EMPTY = MemorySegment.ofArray(new byte[0]);

    /// Returns a zero-copy segment view over a Java `byte[]`.
    public static MemorySegment from(byte[] array) {
        Objects.requireNonNull(array, "array must not be null");
        return MemorySegment.ofArray(array);
    }

    /// Returns a zero-copy segment view over a Java `char[]`.
    public static MemorySegment from(char[] array) {
        Objects.requireNonNull(array, "array must not be null");
        return MemorySegment.ofArray(array);
    }

    /// Returns a zero-copy segment view over a Java `short[]`.
    public static MemorySegment from(short[] array) {
        Objects.requireNonNull(array, "array must not be null");
        return MemorySegment.ofArray(array);
    }

    /// Returns a zero-copy segment view over a Java `int[]`.
    public static MemorySegment from(int[] array) {
        Objects.requireNonNull(array, "array must not be null");
        return MemorySegment.ofArray(array);
    }

    /// Returns a zero-copy segment view over a Java `long[]`.
    public static MemorySegment from(long[] array) {
        Objects.requireNonNull(array, "array must not be null");
        return MemorySegment.ofArray(array);
    }

    /// Returns a zero-copy segment view over a Java `float[]`.
    public static MemorySegment from(float[] array) {
        Objects.requireNonNull(array, "array must not be null");
        return MemorySegment.ofArray(array);
    }

    /// Returns a zero-copy segment view over a Java `double[]`.
    public static MemorySegment from(double[] array) {
        Objects.requireNonNull(array, "array must not be null");
        return MemorySegment.ofArray(array);
    }
}
