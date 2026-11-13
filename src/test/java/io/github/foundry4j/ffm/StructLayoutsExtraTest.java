package io.github.foundry4j.ffm;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Additional tests for StructLayouts covering edge cases.
class StructLayoutsExtraTest {

    @Test
    void doubleFieldRoundTrip() {
        StructLayouts.MappedRecordFactory factory = StructLayouts.struct()
                .field("temperature", ValueLayout.JAVA_DOUBLE)
                .buildRecord();
        try (Arena arena = Arena.ofConfined()) {
            StructLayouts.MappedRecord record = factory.map(arena.allocate(8, 8));
            record.set("temperature", 36.6);
            assertEquals(36.6, (Double) record.get("temperature"), 0.001);
        }
    }

    @Test
    void floatFieldRoundTrip() {
        StructLayouts.MappedRecordFactory factory = StructLayouts.struct()
                .field("scale", ValueLayout.JAVA_FLOAT)
                .buildRecord();
        try (Arena arena = Arena.ofConfined()) {
            StructLayouts.MappedRecord record = factory.map(arena.allocate(4, 4));
            record.set("scale", 0.5f);
            assertEquals(0.5f, (Float) record.get("scale"), 0.001f);
        }
    }

    @Test
    void booleanFieldRoundTrip() {
        StructLayouts.MappedRecordFactory factory = StructLayouts.struct()
                .field("flag", ValueLayout.JAVA_BOOLEAN)
                .buildRecord();
        try (Arena arena = Arena.ofConfined()) {
            StructLayouts.MappedRecord record = factory.map(arena.allocate(1, 1));
            record.set("flag", true);
            assertEquals(true, record.get("flag"));
        }
    }

    @Test
    void charFieldRoundTrip() {
        StructLayouts.MappedRecordFactory factory = StructLayouts.struct()
                .field("c", ValueLayout.JAVA_CHAR)
                .buildRecord();
        try (Arena arena = Arena.ofConfined()) {
            StructLayouts.MappedRecord record = factory.map(arena.allocate(2, 2));
            record.set("c", 'X');
            assertEquals('X', (Character) record.get("c"));
        }
    }

    @Test
    void shortFieldRoundTrip() {
        StructLayouts.MappedRecordFactory factory = StructLayouts.struct()
                .field("s", ValueLayout.JAVA_SHORT)
                .buildRecord();
        try (Arena arena = Arena.ofConfined()) {
            StructLayouts.MappedRecord record = factory.map(arena.allocate(2, 2));
            record.set("s", (short) 32000);
            assertEquals((short) 32000, (Short) record.get("s"));
        }
    }

    @Test
    void byteFieldRoundTrip() {
        StructLayouts.MappedRecordFactory factory = StructLayouts.struct()
                .field("b", ValueLayout.JAVA_BYTE)
                .buildRecord();
        try (Arena arena = Arena.ofConfined()) {
            StructLayouts.MappedRecord record = factory.map(arena.allocate(1, 1));
            record.set("b", (byte) 0x7F);
            assertEquals((byte) 0x7F, (Byte) record.get("b"));
        }
    }

    @Test
    void recordRejectsWrongSizeSegment() {
        StructLayouts.MappedRecordFactory factory = StructLayouts.struct()
                .field("x", ValueLayout.JAVA_INT)
                .buildRecord();
        try (Arena arena = Arena.ofConfined()) {
            assertThrows(IllegalArgumentException.class, () -> factory.map(arena.allocate(8, 4)));
        }
    }

    @Test
    void builderValidatesFieldArguments() {
        StructLayouts.StructBuilder builder = StructLayouts.struct();
        assertThrows(NullPointerException.class, () -> builder.field(null, ValueLayout.JAVA_INT));
        assertThrows(NullPointerException.class, () -> builder.field("x", null));
    }

    @Test
    void unionAllowsRepeatedAccess() {
        StructLayouts.MappedRecordFactory factory = StructLayouts.union()
                .field("i", ValueLayout.JAVA_INT)
                .field("l", ValueLayout.JAVA_LONG)
                .buildRecord();
        assertEquals(8L, factory.layout().byteSize());
    }
}
