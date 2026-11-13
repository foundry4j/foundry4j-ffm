package io.github.foundry4j.ffm;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Additional pool tests.
class MemoryArenaPoolExtraTest {

    @Test
    void drainReleasesArenas() {
        for (int i = 0; i < 3; i++) {
            try (Arena arena = MemoryArenaPool.acquire()) {
                arena.allocate(8, 8);
            }
        }
        MemoryArenaPool.closeCurrentThread();

        try (Arena arena = MemoryArenaPool.acquire()) {
            arena.allocate(8, 8);
        }
    }

    @Test
    void arenaLifecycle() {
        Arena arena = MemoryArenaPool.acquire();
        MemorySegment seg = arena.allocate(4, 4);
        seg.set(ValueLayout.JAVA_INT, 0, 1);
        assertEquals(1, seg.get(ValueLayout.JAVA_INT, 0));
        arena.close();

        assertThrows(IllegalStateException.class,
                () -> arena.allocate(4, 4));
    }
}
