# Foundry4j FFM

<img src="docs/logo.svg" alt="Foundry4j FFM logo" align="right" width="128" height="128" />

A zero-dependency, Java-native, high-performance external memory access library
for Java 27, built on top of `java.lang.foreign`.

![Java 27+](https://img.shields.io/badge/Java-27%2B-ED8B00?logo=openjdk&logoColor=white)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.foundry4j/foundry4j-ffm?logo=apachemaven&color=blue)](https://central.sonatype.com/artifact/io.github.foundry4j/foundry4j-ffm)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=foundry4j_foundry4j-ffm&metric=coverage)](https://sonarcloud.io/summary/new_code?id=foundry4j_foundry4j-ffm)
[![CI](https://github.com/foundry4j/foundry4j-ffm/actions/workflows/ci.yml/badge.svg)](https://github.com/foundry4j/foundry4j-ffm/actions/workflows/ci.yml)

## Features

- **Zero runtime dependencies** — built exclusively on `java.lang.foreign`,
  `java.lang.invoke`, `java.nio.charset` and `java.util`, with a JPMS module
  descriptor (`io.github.foundry4j.ffm`).
- **Thread-local arena pool** — `MemoryArenaPool.acquire()` reuses confined
  arenas across calls on the same thread, so hot-path code never pays the
  JVM-level arena allocation cost.
- **Struct / union builders with typed records** — `StructLayouts.struct()`
  and `union()` produce layouts plus `MappedRecord` wrappers that use
  direct `MemorySegment.get/set` typed accessors (no VarHandle dispatch).
- **Aligned & unaligned primitives** — `MemorySlices` exposes every
  primitive carrier as both an aligned and an unaligned accessor, including
  bulk array copies.
- **Native C string helpers** — `NativeStrings` allocates, encodes and
  decodes null-terminated C strings on an Arena, with pool-backed and
  global shortcuts.
- **ByteBuffer bridge** — `ByteBufferBridge` allocates direct buffers backed
  by an Arena (no `Unsafe`), and copies between segments and heap / direct
  `ByteBuffer`s with zero extra copies where possible.

## Installation

Gradle (Kotlin DSL):

```kotlin
dependencies {
    implementation("io.github.foundry4j:foundry4j-ffm:1.0.0")
}
```

## Quick start

```java
import io.github.foundry4j.ffm.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

void main() {
    try (Arena arena = Arena.ofConfined()) {

        // Allocate a 1 KB segment and zero it.
        MemorySegment buffer = FFM.allocate(arena, 1024);
        FFM.clear(buffer);

        // Write a native C string and read it back.
        MemorySegment nativeStr = NativeStrings.writeUtf8CString(arena, "hello");
        String decoded = NativeStrings.toUtf8String(nativeStr); // "hello"

        // Build a struct and access fields by name.
        StructLayouts.MappedRecordFactory point = StructLayouts.struct()
                .field("x", ValueLayout.JAVA_INT)
                .field("y", ValueLayout.JAVA_INT)
                .buildRecord();

        StructLayouts.MappedRecord pt = point.map(arena.allocate(8, 4));
        pt.set("x", 3);
        pt.set("y", 4);
        int distance = (int) Math.hypot((int) pt.get("x"), (int) pt.get("y")); // 5.0

        // Typed access helpers: same as segment.get/set with sugar.
        MemorySlices.putInt(buffer, 0, 42);
        int answer = MemorySlices.getInt(buffer, 0); // 42
    }
}
```

## API overview

| Class | Purpose |
|-------|---------|
| `FFM` | Static entry point: allocate, fill/clear, copy, `toByteArray`, `toHex`, `slice`, `tail`, `mismatch`, `equal`, `from` zero-copy wrappers for every primitive array. |
| `MemoryArenaPool` | Thread-local confined-arena pool. `acquire()` returns a pooled arena whose `close()` returns it to the pool. `closeCurrentThread()` drains the current thread's pool. |
| `NativeStrings` | Allocate and encode C strings (`writeUtf8CString`, `writeCStringTo`), decode them (`toUtf8String`, `toString`), and inspect them (`nullByteIndex`, `cStringLength`). `pooledUtf8CString` and `globalUtf8CString` shortcuts. |
| `MemorySlices` | Typed primitive getters/setters (`getInt`, `putLongUnaligned`, `getAddressRaw`, ...) plus bulk `putBytes`, `putShorts`, `putInts`, `putLongs`, `getBytes`. |
| `StructLayouts` | Fluent `struct()` / `union()` builder, optional `alignedTo()`, and a `MappedRecordFactory` that emits `MappedRecord` wrappers. `MappedRecord.get/set` dispatch to precomputed-offset typed segment accessors. |
| `ByteBufferBridge` | `allocateDirectByteBuffer(Arena, long)` for Arena-backed direct buffers; `copyFromByteBuffer` / `copyToByteBuffer` handle heap and direct ByteBuffer copies; `asMemorySegment` wraps any ByteBuffer. |

## `FFM` entry point

The entry point is intentionally stateless and delegates everything to FFM:

```java
MemorySegment a = FFM.allocate(arena, ValueLayout.JAVA_INT, 100); // 100 × int
MemorySegment b = FFM.from(new byte[]{1, 2, 3});                 // zero-copy view

byte[] copy = FFM.toByteArray(someSegment);
String hex  = FFM.toHex(someSegment);       // full segment
String hex2 = FFM.toHex(someSegment, 16);   // first 16 bytes

MemorySegment sliced = FFM.slice(seg, ValueLayout.JAVA_LONG, 8);
```

`EMPTY` is a zero-size heap-backed segment constant useful for "no data" defaults.

## `MemoryArenaPool`

Arena creation is cheap but not free. On hot paths where an arena is needed
per operation, the thread-local pool keeps one confined arena per thread ready
to go, up to `MAX_PER_THREAD` (currently 8).

```java
try (Arena arena = MemoryArenaPool.acquire()) {
    MemorySegment tmp = arena.allocate(256, 1);
    // ... reuse tmp as a scratch buffer inside this block ...
}
// arena returned to pool — no OS-level deallocation happened
```

Pooled arenas are **confined to the acquiring thread**, mirroring FFM's arena
model. The pool is per-thread only — no cross-thread sharing and no
synchronization inside the pool's hot path.

## `NativeStrings`

```java
// Allocate + encode in one call.
MemorySegment nativeStr = NativeStrings.writeUtf8CString(arena, "hello");

// Overwrite an existing segment (e.g. a struct field pre-allocated for strings).
NativeStrings.writeUtf8CStringTo(structFieldSegment, "world");

// Decode, stopping at the first null byte.
String back = NativeStrings.toUtf8String(nativeStr);

// Low-level inspection.
long len   = NativeStrings.cStringLength(nativeStr);
long nullAt = NativeStrings.nullByteIndex(nativeStr);
```

Pooled and global shortcuts:

```java
MemorySegment pooled = NativeStrings.pooledUtf8CString("hot");   // pooled arena
MemorySegment global = NativeStrings.globalUtf8CString("static"); // Arena.global()
```

## `MemorySlices`

Typed accessors on `MemorySegment` that avoid manual `ValueLayout` lookup:

```java
MemorySlices.putInt(seg, 0, 42);
int v = MemorySlices.getInt(seg, 0);

// When alignment is not guaranteed — e.g. a pointer into a packed structure.
MemorySlices.putLongUnaligned(seg, 7, 0xDEADBEEFL);
long unaligned = MemorySlices.getLongUnaligned(seg, 7);

// Address handling.
MemorySlices.putAddress(seg, 0, pointerSegment);
MemorySegment ptr = MemorySlices.getAddress(seg, 0);

// Bulk copy from/to Java arrays.
MemorySlices.putLongs(seg, 0, new long[]{1, 2, 3, 4});
MemorySlices.getBytes(seg, 0, new byte[1024]);
```

Every aligned variant has an `Unaligned` counterpart that accepts any offset.
Bulk array copy methods use `MemorySegment.copy` internally — the same path
the JDK uses for heap↔segment bulk moves.

## `StructLayouts`

Fluent builder → layout factory → typed record wrappers:

```java
StructLayouts.MappedRecordFactory factory = StructLayouts.struct()
        .field("id",   ValueLayout.JAVA_LONG)
        .field("flag", ValueLayout.JAVA_BOOLEAN)
        .cStringField("name", 64)
        .alignedTo(8)
        .buildRecord();

try (Arena arena = Arena.ofConfined()) {
    StructLayouts.MappedRecord rec = factory.map(arena.allocate(factory.layout()));
    rec.set("id", 9001L);
    rec.set("flag", true);

    long id = (long) rec.get("id");  // typed, with precomputed offset
}
```

`MappedRecord` uses direct `MemorySegment.get/set` accessors — no
`VarHandle` dispatch, no reflection, no per-accessor field resolution. Each
`TypedField` stores a `(offset, ValueLayout)` pair and switches on the
carrier class once, at creation time.

## `ByteBufferBridge`

```java
// Allocate a direct ByteBuffer owned by an Arena (no Unsafe).
ByteBuffer bb = ByteBufferBridge.allocateDirectByteBuffer(arena, 64);

// Copy the remaining bytes of a ByteBuffer into a segment.
ByteBufferBridge.copyFromByteBuffer(srcBuffer, dstSegment, 0);

// Copy from a segment into a ByteBuffer, advancing position on both sides.
ByteBufferBridge.copyToByteBuffer(srcSegment, 0, dstBuffer);

// Wrap any ByteBuffer as a MemorySegment (segment lifecycle = buffer lifecycle).
MemorySegment seg = ByteBufferBridge.asMemorySegment(existingBuffer);
```

`copyFromByteBuffer` and `copyToByteBuffer` use the heap-array fast path
(`src.array()` / `dst.array()`) when available, and fall back to a
`MemorySegment.ofBuffer` view for direct / mapped buffers — so there is
never an extra intermediate copy.

## Performance notes

- **Arena pool** — acquires return a pre-warmed confined arena; cold path is
  one `Arena.ofConfined()` call. Pool exhaustion (`MAX_PER_THREAD`) falls
  back to eager close.
- **Struct field access** — `MappedRecord.get/set` resolve the carrier once
  (at factory creation) and then dispatch directly to `segment.get/set`;
  there is no VarHandle and no per-call field lookup.
- **Zero-copy array views** — `FFM.from(byte[])` / `from(int[])` ... are
  zero-copy `MemorySegment.ofArray` wrappers — the array sits in the heap;
  FFM reads and writes through it directly.
- **ByteBuffer bridge** — `asMemorySegment` is a zero-cost wrap.
  `copyFromByteBuffer` / `copyToByteBuffer` pick the heap-array fast path
  when the buffer has one, and otherwise do a single segment-to-segment copy.

## Semantics and constraints

- **Thread confinement** — confined arenas (the default and the pool's
  arena type) throw `WrongThreadException` if accessed from a different
  thread. `Arena.global()` and shared arenas are thread-safe but carry no
  lifecycle semantics.
- **Alignment** — aligned accessors throw if the offset violates the
  layout's natural alignment. Unaligned variants never do.
- **Lifecycle** — segments allocated from an Arena become invalid when the
  Arena is closed. Pooled arenas returned to the pool stay valid until the
  thread's pool is drained or the JVM exits.
- **Null safety** — every public method validates its arguments with
  `Objects.requireNonNull` and throws `NullPointerException` on null input.
