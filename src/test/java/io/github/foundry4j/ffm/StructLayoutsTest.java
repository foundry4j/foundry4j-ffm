package io.github.foundry4j.ffm;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Tests for [StructLayouts].
class StructLayoutsTest {

    @Test
    void buildsStructLayout() {
        MemoryLayout point = StructLayouts.struct()
                .field("x", ValueLayout.JAVA_INT)
                .field("y", ValueLayout.JAVA_INT)
                .build();

        assertEquals(8L, point.byteSize());
    }

    @Test
    void buildsMappedRecord() {
        StructLayouts.MappedRecordFactory factory = StructLayouts.struct()
                .field("x", ValueLayout.JAVA_INT)
                .field("y", ValueLayout.JAVA_INT)
                .buildRecord();

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(8, 4);
            StructLayouts.MappedRecord record = factory.map(seg);

            record.set("x", 10);
            record.set("y", 20);
            assertEquals(10, record.get("x"));
            assertEquals(20, record.get("y"));
        }
    }

    @Test
    void mappedRecordRejectsUnknownField() {
        StructLayouts.MappedRecordFactory factory = StructLayouts.struct()
                .field("x", ValueLayout.JAVA_INT)
                .buildRecord();
        try (Arena arena = Arena.ofConfined()) {
            StructLayouts.MappedRecord record = factory.map(arena.allocate(4, 4));
            assertThrows(IllegalArgumentException.class, () -> record.get("nope"));
        }
    }

    @Test
    void unionLaysOutOverlapping() {
        MemoryLayout union = StructLayouts.union()
                .field("i", ValueLayout.JAVA_INT)
                .field("d", ValueLayout.JAVA_DOUBLE)
                .build();

        assertEquals(8L, union.byteSize());
    }

    @Test
    void cStringFieldUsesSequence() {
        MemoryLayout layout = StructLayouts.struct()
                .cStringField("name", 16)
                .build();

        assertEquals(16L, layout.byteSize());
    }

    @Test
    void structBuilderValidatesAlignment() {
        StructLayouts.StructBuilder builder = StructLayouts.struct();
        assertThrows(IllegalArgumentException.class, () -> builder.alignedTo(3));
    }
}
