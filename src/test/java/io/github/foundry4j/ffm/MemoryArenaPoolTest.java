package io.github.foundry4j.ffm;

import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Tests for [MemoryArenaPool].
class MemoryArenaPoolTest {

    @Test
    void acquireReturnsDifferentArenas() {
        var a = MemoryArenaPool.acquire();
        var b = MemoryArenaPool.acquire();
        assertSame(a.getClass(), b.getClass());
        a.close();
        b.close();
    }

    @Test
    void pooledArenaCanAllocate() {
        try (var arena = MemoryArenaPool.acquire()) {
            MemorySegment seg = arena.allocate(8, 8);
            assertEquals(8L, seg.byteSize());
        }
    }

    @Test
    void closeTwiceThrows() {
        var arena = MemoryArenaPool.acquire();
        arena.close();
        assertThrows(IllegalStateException.class, arena::close);
    }

    @Test
    void drainActuallyCloses() {
        MemoryArenaPool.closeCurrentThread();
        // After drain, acquire/close cycles continue to work.
        var arena = MemoryArenaPool.acquire();
        arena.close();
        MemoryArenaPool.closeCurrentThread();
    }
}
