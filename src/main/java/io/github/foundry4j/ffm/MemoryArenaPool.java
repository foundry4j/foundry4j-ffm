package io.github.foundry4j.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;

/// A simple, thread-local pool of [Arena] instances, useful for hot-path
/// allocations where arena creation and per-call cleanup would otherwise be a
/// noticeable overhead.
///
/// Each thread has its own stack of confined arenas. Arenas are **not** pooled
/// across threads — Java FFM's confined arena is intentionally single-threaded.
/// Shared arenas would require additional synchronization that outweighs their
/// benefits on typical hot paths.
///
/// ```java
/// try (Arena arena = MemoryArenaPool.acquire()) {
///     MemorySegment buffer = arena.allocate(1024, 1);
///     // ...
/// }
/// ```
///
/// The pool grows on demand and is pruned lazily. It never retains more than
/// [MAX_PER_THREAD] arenas per thread; arenas released beyond the cap are
/// closed immediately.
public final class MemoryArenaPool {

    /// Maximum number of confined arenas retained per thread before extra
    /// releases are closed eagerly. The current value was chosen as a small
    /// constant rather than a tunable flag because hot-path code usually only
    /// needs one arena at a time per thread.
    public static final int MAX_PER_THREAD = 8;

    private static final ThreadLocal<ThreadState> STATE = ThreadLocal.withInitial(ThreadState::new);

    private MemoryArenaPool() {
    }

    /// Acquires a confined arena for the current thread.
    ///
    /// When an arena is available in the thread-local pool, it is returned
    /// directly (and its [Arena#close] method will return it to the pool instead
    /// of actually closing it). Otherwise a new confined arena is created.
    ///
    /// Callers must `close()` the arena once they are done.
    ///
    /// @return an arena that is only safe for use by the current thread
    public static Arena acquire() {
        return STATE.get().acquire();
    }

    /// Forces all arenas in the current thread's pool to close immediately.
    /// Useful in tests and at application shutdown.
    ///
    /// Note: this only affects the calling thread's pool; other threads keep
    /// their own arenas.
    public static void closeCurrentThread() {
        STATE.get().drain();
    }

    // -----------------------------------------------------------------------
    // Thread-local state
    // -----------------------------------------------------------------------

    private static final class ThreadState {

        private final Deque<Arena> pool = new ArrayDeque<>();

        private Arena acquire() {
            Arena reused = pool.poll();
            if (reused != null) {
                return new PooledArena(reused, pool);
            }
            return new PooledArena(Arena.ofConfined(), pool);
        }

        private void drain() {
            Arena arena;
            while ((arena = pool.poll()) != null) {
                arena.close();
            }
        }
    }

    // -----------------------------------------------------------------------
    // Arena wrapper that returns to the pool instead of closing
    // -----------------------------------------------------------------------

    /// An [Arena] wrapper that returns to the pool on close.
    ///
    /// The wrapper delegates every method call to the underlying arena; only
    /// [close] is overridden to push the wrapped arena back into the pool instead
    /// of actually closing it.
    private static final class PooledArena implements Arena {

        private final Arena delegate;
        private final Deque<Arena> pool;
        private final AtomicBoolean closed = new AtomicBoolean();

        PooledArena(Arena delegate, Deque<Arena> pool) {
            this.delegate = delegate;
            this.pool = pool;
        }

        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            if (closed.get()) {
                throw new IllegalStateException("pooled arena is closed");
            }
            return delegate.allocate(byteSize, byteAlignment);
        }

        @Override
        public MemorySegment.Scope scope() {
            return delegate.scope();
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                throw new IllegalStateException("pooled arena is already closed");
            }
            synchronized (pool) {
                if (pool.size() < MAX_PER_THREAD) {
                    pool.push(delegate);
                    return;
                }
            }
            delegate.close();
        }
    }
}
