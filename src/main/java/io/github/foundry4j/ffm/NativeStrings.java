package io.github.foundry4j.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Utilities for working with native (zero-terminated) strings backed by an
/// external memory arena.
///
/// Unlike Java `String`, a native C string has a fixed, null-terminated layout.
/// This class uses `Arena.allocate` to back the memory, so the returned
/// segment is valid as long as the owning arena is open.
///
/// ```java
/// try (Arena arena = Arena.ofConfined()) {
///     MemorySegment native = NativeStrings.writeUtf8CString(arena, "hello");
///     String back = NativeStrings.toUtf8String(native); // "hello"
/// }
/// ```
public final class NativeStrings {

    /// Default charset used when callers do not specify one.
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private NativeStrings() {
    }

    // ---- Allocation -----------------------------------------------------------

    /// Allocates a new native C string segment backed by the `allocator` arena
    /// and writes the UTF-8 encoded bytes of `value`, followed by a null
    /// terminator.
    ///
    /// The returned segment has exactly `encoded.length + 1` bytes of content,
    /// with the last byte set to `0`.
    ///
    /// @param allocator the arena that owns the returned segment
    /// @param value     the Java string to encode, must not be `null`
    /// @return a new native segment holding the encoded C string
    public static MemorySegment writeUtf8CString(SegmentAllocator allocator, CharSequence value) {
        return writeCString(allocator, value, DEFAULT_CHARSET);
    }

    /// Allocates a new native C string segment backed by `allocator` and writes
    /// the encoded bytes of `value`, followed by a null terminator.
    ///
    /// @param allocator the arena that owns the returned segment
    /// @param value     the Java string to encode
    /// @param charset   the charset used to encode `value`, must not be `null`
    /// @return a new native segment holding the encoded C string
    public static MemorySegment writeCString(SegmentAllocator allocator, CharSequence value,
                                             Charset charset) {
        Objects.requireNonNull(allocator, "allocator must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(charset, "charset must not be null");

        byte[] encoded = value.toString().getBytes(charset);
        MemorySegment segment = allocator.allocate(encoded.length + 1, 1);
        MemorySegment.copy(encoded, 0, segment, ValueLayout.JAVA_BYTE, 0, encoded.length);
        // Null terminator is already zero in a freshly allocated segment, but
        // we write it explicitly so this method is safe to call on a reused
        // segment whose contents have not been cleared.
        segment.set(ValueLayout.JAVA_BYTE, encoded.length, (byte) 0);
        return segment;
    }

    /// Overwrites `segment` with the UTF-8 encoded bytes of `value` followed
    /// by a null terminator. The caller is responsible for ensuring that
    /// `segment.byteSize() >= value.length() + 1`.
    ///
    /// @param segment the destination segment, must be writable
    /// @param value   the Java string to encode
    /// @throws IndexOutOfBoundsException if `segment` is too small
    public static void writeUtf8CStringTo(MemorySegment segment, CharSequence value) {
        writeCStringTo(segment, value, DEFAULT_CHARSET);
    }

    /// Overwrites `segment` with the encoded bytes of `value` followed by a
    /// null terminator.
    ///
    /// @param segment the destination segment, must be writable
    /// @param value   the Java string to encode
    /// @param charset the charset used to encode `value`
    public static void writeCStringTo(MemorySegment segment, CharSequence value, Charset charset) {
        Objects.requireNonNull(segment, "segment must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(charset, "charset must not be null");

        byte[] encoded = value.toString().getBytes(charset);
        if (encoded.length + 1 > segment.byteSize()) {
            throw new IndexOutOfBoundsException(
                    "segment too small: need " + (encoded.length + 1)
                            + " bytes but segment is only " + segment.byteSize());
        }
        MemorySegment.copy(encoded, 0, segment, ValueLayout.JAVA_BYTE, 0, encoded.length);
        segment.set(ValueLayout.JAVA_BYTE, encoded.length, (byte) 0);
    }

    // ---- Decoding -------------------------------------------------------------

    /// Reads the bytes of a C string until the first null byte, then decodes
    /// them as UTF-8.
    ///
    /// @param segment the source segment, may be longer than the C string
    /// @return a new Java string, empty if the segment starts with a null byte
    public static String toUtf8String(MemorySegment segment) {
        return toString(segment, DEFAULT_CHARSET);
    }

    /// Reads the bytes of a C string until the first null byte, then decodes
    /// them using `charset`.
    ///
    /// When the segment has no null byte, the entire segment is treated as the
    /// C string content (useful for strings that have already been null-terminated
    /// and stored with their full length).
    ///
    /// @param segment the source segment
    /// @param charset the charset used to decode the bytes
    /// @return a new Java string
    public static String toString(MemorySegment segment, Charset charset) {
        Objects.requireNonNull(segment, "segment must not be null");
        Objects.requireNonNull(charset, "charset must not be null");

        long size = segment.byteSize();
        if (size == 0) {
            return "";
        }

        long length = 0;
        while (length < size && segment.get(ValueLayout.JAVA_BYTE, length) != 0) {
            length++;
        }

        if (length == 0) {
            return "";
        }
        if (length == size) {
            // No null terminator present; treat the whole segment as content.
            return FFM.toString(segment, charset);
        }

        byte[] bytes = new byte[(int) length];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, 0L, bytes, 0, (int) length);
        return new String(bytes, charset);
    }

    /// Finds the first null byte inside `segment`, returning its index in bytes,
    /// or `-1` when no null byte is present.
    ///
    /// @param segment the segment to scan
    /// @return the byte offset of the first `0` byte, or `-1`
    public static long nullByteIndex(MemorySegment segment) {
        Objects.requireNonNull(segment, "segment must not be null");
        long size = segment.byteSize();
        for (long i = 0; i < size; i++) {
            if (segment.get(ValueLayout.JAVA_BYTE, i) == 0) {
                return i;
            }
        }
        return -1;
    }

    /// Returns the length of the null-terminated C string stored at the start of
    /// `segment`, excluding the null terminator.
    ///
    /// @param segment the segment holding the C string at offset 0
    /// @return the number of content bytes
    public static long cStringLength(MemorySegment segment) {
        Objects.requireNonNull(segment, "segment must not be null");
        long size = segment.byteSize();
        for (long i = 0; i < size; i++) {
            if (segment.get(ValueLayout.JAVA_BYTE, i) == 0) {
                return i;
            }
        }
        return size;
    }

    // ---- Arena-backed pool usage ---------------------------------------------

    /// Returns a UTF-8 C string segment allocated from the shared arena pool.
    /// This is equivalent to `writeUtf8CString(MemoryArenaPool.acquire(), value)`;
    /// callers should use the pool when strings are allocated on a hot path and
    /// can be released once done.
    ///
    /// @param value the Java string to encode
    /// @return a segment owned by a pooled arena
    public static MemorySegment pooledUtf8CString(CharSequence value) {
        return writeUtf8CString(MemoryArenaPool.acquire(), value);
    }

    /// Returns a UTF-8 C string segment allocated from a global (non-owning)
    /// arena. The caller does **not** need to close the arena, but the returned
    /// segment lives until the global arena is shut down or leaks — use this
    /// only for static, one-time strings.
    ///
    /// @param value the Java string to encode
    /// @return a segment owned by [Arena#global()]
    public static MemorySegment globalUtf8CString(CharSequence value) {
        return writeUtf8CString(Arena.global(), value);
    }
}
