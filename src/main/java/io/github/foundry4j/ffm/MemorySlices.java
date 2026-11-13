package io.github.foundry4j.ffm;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/// Typed primitive read/write helpers on [MemorySegment] that do **not** require
/// the caller to build a [java.lang.foreign.VarHandle] manually.
///
/// Each method delegates directly to [MemorySegment#get] and [MemorySegment#set]
/// with the correct [ValueLayout], so performance is identical to writing the
/// `VarHandle` call yourself. The helpers are just sugar.
///
/// ```java
/// segment.set(ValueLayout.JAVA_INT, 0L, 42);
/// //    becomes
/// MemorySlices.putInt(segment, 0L, 42);
/// ```
public final class MemorySlices {

    private MemorySlices() {
    }

    // ---- Byte -----------------------------------------------------------------

    /// Reads a `byte` at the given offset.
    public static byte getByte(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_BYTE, offset);
    }

    /// Writes a `byte` at the given offset.
    public static void putByte(MemorySegment segment, long offset, byte value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_BYTE, offset, value);
    }

    // ---- Short ----------------------------------------------------------------

    /// Reads a `short` at the given offset.
    public static short getShort(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_SHORT, offset);
    }

    /// Reads a `short` at the given offset without alignment enforcement.
    public static short getShortUnaligned(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_SHORT_UNALIGNED, offset);
    }

    /// Writes a `short` at the given offset.
    public static void putShort(MemorySegment segment, long offset, short value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_SHORT, offset, value);
    }

    /// Writes a `short` at the given offset without alignment enforcement.
    public static void putShortUnaligned(MemorySegment segment, long offset, short value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_SHORT_UNALIGNED, offset, value);
    }

    // ---- Int ------------------------------------------------------------------

    /// Reads an `int` at the given offset.
    public static int getInt(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_INT, offset);
    }

    /// Reads an `int` at the given offset without alignment enforcement.
    public static int getIntUnaligned(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_INT_UNALIGNED, offset);
    }

    /// Writes an `int` at the given offset.
    public static void putInt(MemorySegment segment, long offset, int value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_INT, offset, value);
    }

    /// Writes an `int` at the given offset without alignment enforcement.
    public static void putIntUnaligned(MemorySegment segment, long offset, int value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, value);
    }

    // ---- Long -----------------------------------------------------------------

    /// Reads a `long` at the given offset.
    public static long getLong(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_LONG, offset);
    }

    /// Reads a `long` at the given offset without alignment enforcement.
    public static long getLongUnaligned(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, offset);
    }

    /// Writes a `long` at the given offset.
    public static void putLong(MemorySegment segment, long offset, long value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_LONG, offset, value);
    }

    /// Writes a `long` at the given offset without alignment enforcement.
    public static void putLongUnaligned(MemorySegment segment, long offset, long value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_LONG_UNALIGNED, offset, value);
    }

    // ---- Float ----------------------------------------------------------------

    /// Reads a `float` at the given offset.
    public static float getFloat(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_FLOAT, offset);
    }

    /// Reads a `float` at the given offset without alignment enforcement.
    public static float getFloatUnaligned(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_FLOAT_UNALIGNED, offset);
    }

    /// Writes a `float` at the given offset.
    public static void putFloat(MemorySegment segment, long offset, float value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_FLOAT, offset, value);
    }

    /// Writes a `float` at the given offset without alignment enforcement.
    public static void putFloatUnaligned(MemorySegment segment, long offset, float value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, value);
    }

    // ---- Double ---------------------------------------------------------------

    /// Reads a `double` at the given offset.
    public static double getDouble(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_DOUBLE, offset);
    }

    /// Reads a `double` at the given offset without alignment enforcement.
    public static double getDoubleUnaligned(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset);
    }

    /// Writes a `double` at the given offset.
    public static void putDouble(MemorySegment segment, long offset, double value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_DOUBLE, offset, value);
    }

    /// Writes a `double` at the given offset without alignment enforcement.
    public static void putDoubleUnaligned(MemorySegment segment, long offset, double value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset, value);
    }

    // ---- Char -----------------------------------------------------------------

    /// Reads a `char` at the given offset.
    public static char getChar(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_CHAR, offset);
    }

    /// Reads a `char` at the given offset without alignment enforcement.
    public static char getCharUnaligned(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_CHAR_UNALIGNED, offset);
    }

    /// Writes a `char` at the given offset.
    public static void putChar(MemorySegment segment, long offset, char value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_CHAR, offset, value);
    }

    /// Writes a `char` at the given offset without alignment enforcement.
    public static void putCharUnaligned(MemorySegment segment, long offset, char value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_CHAR_UNALIGNED, offset, value);
    }

    // ---- Boolean --------------------------------------------------------------

    /// Reads a `boolean` at the given offset. In native memory, a boolean is
    /// represented as a single byte with any non-zero value meaning `true`.
    public static boolean getBoolean(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_BOOLEAN, offset);
    }

    /// Writes a `boolean` at the given offset as a single byte (`1` for `true`,
    /// `0` for `false`).
    public static void putBoolean(MemorySegment segment, long offset, boolean value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_BOOLEAN, offset, value);
    }

    // ---- Address --------------------------------------------------------------

    /// Reads a native address at the given offset as a [MemorySegment] wrapping
    /// that pointer. This matches the semantics of
    /// [MemorySegment#get(ValueLayout.AddressLayout, long)].
    public static MemorySegment getAddress(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.ADDRESS, offset);
    }

    /// Writes a native address at the given offset from a [MemorySegment].
    public static void putAddress(MemorySegment segment, long offset, MemorySegment value) {
        checkSegment(segment);
        segment.set(ValueLayout.ADDRESS, offset, value);
    }

    /// Reads a native address as a raw `long` value at the given offset.
    ///
    /// This method is best-effort: it reads the pointer as a `JAVA_LONG`, which
    /// works correctly on any JVM where native pointers are 8 bytes wide
    /// (essentially every modern JVM). On a 32-bit JVM, only the low 4 bytes of
    /// the address are meaningful.
    ///
    /// When the caller needs the typed address segment that FFM normally returns
    /// for pointer fields (with proper scope and alignment), prefer
    /// [getAddress] to this method.
    public static long getAddressRaw(MemorySegment segment, long offset) {
        checkSegment(segment);
        return segment.get(ValueLayout.JAVA_LONG, offset);
    }

    /// Writes a raw pointer as a 64-bit `long` at the given offset. Prefer
    /// [putAddress] when the caller has a proper [MemorySegment] to persist.
    public static void putAddressRaw(MemorySegment segment, long offset, long value) {
        checkSegment(segment);
        segment.set(ValueLayout.JAVA_LONG, offset, value);
    }

    // ---- Bulk put from Java arrays -------------------------------------------

    /// Copies the contents of `src` into `segment` starting at `offset`.
    public static void putBytes(MemorySegment segment, long offset, byte[] src) {
        Objects.requireNonNull(segment, "segment must not be null");
        Objects.requireNonNull(src, "src must not be null");
        MemorySegment.copy(src, 0, segment, ValueLayout.JAVA_BYTE, offset, src.length);
    }

    /// Copies the contents of `src` into `segment` starting at `offset`.
    public static void putShorts(MemorySegment segment, long offset, short[] src) {
        Objects.requireNonNull(segment, "segment must not be null");
        Objects.requireNonNull(src, "src must not be null");
        MemorySegment.copy(src, 0, segment, ValueLayout.JAVA_SHORT, offset, src.length);
    }

    /// Copies the contents of `src` into `segment` starting at `offset`.
    public static void putInts(MemorySegment segment, long offset, int[] src) {
        Objects.requireNonNull(segment, "segment must not be null");
        Objects.requireNonNull(src, "src must not be null");
        MemorySegment.copy(src, 0, segment, ValueLayout.JAVA_INT, offset, src.length);
    }

    /// Copies the contents of `src` into `segment` starting at `offset`.
    public static void putLongs(MemorySegment segment, long offset, long[] src) {
        Objects.requireNonNull(segment, "segment must not be null");
        Objects.requireNonNull(src, "src must not be null");
        MemorySegment.copy(src, 0, segment, ValueLayout.JAVA_LONG, offset, src.length);
    }

    // ---- Bulk get into Java arrays --------------------------------------------

    /// Copies bytes from `segment` starting at `offset` into `dst`.
    public static void getBytes(MemorySegment segment, long offset, byte[] dst) {
        Objects.requireNonNull(segment, "segment must not be null");
        Objects.requireNonNull(dst, "dst must not be null");
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, offset, dst, 0, dst.length);
    }

    // ---- Segment validation ---------------------------------------------------

    private static void checkSegment(MemorySegment segment) {
        Objects.requireNonNull(segment, "segment must not be null");
    }
}
