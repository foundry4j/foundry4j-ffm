package io.github.foundry4j.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/// Bridges between [MemorySegment] and [ByteBuffer].
///
/// `java.lang.foreign.MemorySegment` already provides `ofBuffer` to wrap a
/// `ByteBuffer` and `asByteBuffer` to get a view back. This class adds two
/// commonly-needed helpers that are not one-liners:
///
/// - [allocateDirectByteBuffer] returns a direct `ByteBuffer` backed by a
///   caller-provided arena, so callers get an `Arena` lifecycle instead of
///   relying on `sun.misc.Unsafe`.
/// - [copyFromByteBuffer] and [copyToByteBuffer] move data between a segment and
///   a `ByteBuffer` while handling heap-backed `ByteBuffer`s without double-copying
///   when possible.
public final class ByteBufferBridge {

    private ByteBufferBridge() {
    }

    // ---- Allocation -----------------------------------------------------------

    /// Allocates a direct `ByteBuffer` of `byteSize` bytes backed by the given
    /// arena. The returned buffer's lifecycle (and deallocation) is tied to
    /// `arena.close()`.
    ///
    /// Unlike `ByteBuffer.allocateDirect`, which internally calls
    /// `sun.misc.Unsafe`, this method uses the FFM `Arena.allocate` path and is
    /// therefore portable and future-proof.
    ///
    /// @param arena    the arena that owns the backing memory
    /// @param byteSize the requested size in bytes, must be non-negative
    /// @return a big-endian direct `ByteBuffer` backed by `arena`
    public static ByteBuffer allocateDirectByteBuffer(Arena arena, long byteSize) {
        Objects.requireNonNull(arena, "arena must not be null");
        if (byteSize < 0) {
            throw new IllegalArgumentException("byteSize must be non-negative, was " + byteSize);
        }
        MemorySegment segment = arena.allocate(byteSize, 1);
        ByteBuffer buffer = segment.asByteBuffer();
        buffer.order(ByteOrder.nativeOrder());
        return buffer;
    }

    /// Allocates a direct `ByteBuffer` backed by the global (non-owning) arena.
    /// Use this only for buffers that are intentionally outlive all other
    /// resources; otherwise prefer a confined or shared arena.
    ///
    /// @param byteSize the requested size in bytes
    /// @return a big-endian direct `ByteBuffer` backed by [Arena#global()]
    public static ByteBuffer globalDirectByteBuffer(long byteSize) {
        return allocateDirectByteBuffer(Arena.global(), byteSize);
    }

    // ---- Copying --------------------------------------------------------------

    /// Copies the **remaining** bytes of `src` into `segment` starting at
    /// `dstOffset`.
    ///
    /// @param src       the source `ByteBuffer`; its position is advanced by
    ///                  `min(src.remaining(), segment.byteSize() - dstOffset)`
    /// @param segment   the destination segment
    /// @param dstOffset the starting offset in bytes inside `segment`
    public static void copyFromByteBuffer(ByteBuffer src, MemorySegment segment, long dstOffset) {
        Objects.requireNonNull(src, "src must not be null");
        Objects.requireNonNull(segment, "segment must not be null");
        if (dstOffset < 0 || dstOffset > segment.byteSize()) {
            throw new IllegalArgumentException("dstOffset out of range: " + dstOffset);
        }
        long capacity = segment.byteSize() - dstOffset;
        int bytes = (int) Math.min(src.remaining(), capacity);
        if (bytes <= 0) {
            return;
        }

        if (src.hasArray()) {
            MemorySegment.copy(src.array(), src.arrayOffset() + src.position(),
                    segment, ValueLayout.JAVA_BYTE, dstOffset, bytes);
            src.position(src.position() + bytes);
            return;
        }

        // Direct or Mapped ByteBuffer — copy via segment view.
        MemorySegment srcSeg = MemorySegment.ofBuffer(src).asSlice(
                src.position(), bytes);
        MemorySegment.copy(srcSeg, 0L, segment, dstOffset, bytes);
        src.position(src.position() + bytes);
    }

    /// Copies bytes from `segment` starting at `srcOffset` into the remaining
    /// space of `dst`.
    ///
    /// @param segment   the source segment
    /// @param srcOffset the starting offset in bytes inside `segment`
    /// @param dst       the destination `ByteBuffer`; its position is advanced
    ///                  by the number of bytes copied
    public static void copyToByteBuffer(MemorySegment segment, long srcOffset, ByteBuffer dst) {
        Objects.requireNonNull(segment, "segment must not be null");
        Objects.requireNonNull(dst, "dst must not be null");
        if (srcOffset < 0 || srcOffset > segment.byteSize()) {
            throw new IllegalArgumentException("srcOffset out of range: " + srcOffset);
        }
        long available = segment.byteSize() - srcOffset;
        int bytes = (int) Math.min(dst.remaining(), available);
        if (bytes <= 0) {
            return;
        }

        if (dst.hasArray()) {
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, srcOffset,
                    dst.array(), dst.arrayOffset() + dst.position(), bytes);
            dst.position(dst.position() + bytes);
            return;
        }

        MemorySegment dstSeg = MemorySegment.ofBuffer(dst).asSlice(
                dst.position(), bytes);
        MemorySegment.copy(segment, srcOffset, dstSeg, 0L, bytes);
        dst.position(dst.position() + bytes);
    }

    // ---- Conversions ----------------------------------------------------------

    /// Returns a zero-copy `MemorySegment` view of `buffer`'s whole capacity.
    /// Equivalent to [MemorySegment#ofBuffer(ByteBuffer)] exposed here for
    /// discoverability.
    ///
    /// @param buffer the source `ByteBuffer`
    /// @return a segment whose size matches `buffer.capacity()`
    public static MemorySegment asMemorySegment(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");
        return MemorySegment.ofBuffer(buffer);
    }
}
