package io.github.foundry4j.ffm;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.UnionLayout;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Fluent builder for [StructLayout] and [UnionLayout] instances with named
/// children and a [MappedRecord] helper for runtime field access.
///
/// ```java
/// StructLayout point = StructLayouts.struct()
///         .field("x", ValueLayout.JAVA_INT)
///         .field("y", ValueLayout.JAVA_INT)
///         .build();
/// ```
///
/// [MappedRecord] uses FFM's typed [MemorySegment] accessors directly — each
/// record call expands to a single `segment.get/set` with the carrier-specific
/// layout and a precomputed offset, so there is no VarHandle dispatch at
/// runtime beyond what FFM itself uses for the typed accessor.
public final class StructLayouts {

    private StructLayouts() {
    }

    /// Returns a new struct builder.
    public static StructBuilder struct() {
        return new StructBuilder(false);
    }

    /// Returns a new union builder.
    public static StructBuilder union() {
        return new StructBuilder(true);
    }

    // -----------------------------------------------------------------------
    // Fluent builder
    // -----------------------------------------------------------------------

    /// A fluent builder for a [StructLayout] or [UnionLayout].
    public static final class StructBuilder {

        private final boolean union;
        private final List<Field> fields = new ArrayList<>();
        private long customAlignment = -1;

        private StructBuilder(boolean union) {
            this.union = union;
        }

        /// Adds a field with the given name and layout.
        public StructBuilder field(String name, MemoryLayout layout) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(layout, "layout must not be null");
            fields.add(new Field(name, layout));
            return this;
        }

        /// Adds a fixed-size sequence of elements (a C-style array field).
        public StructBuilder field(String name, MemoryLayout element, long count) {
            return field(name, MemoryLayout.sequenceLayout(count, element));
        }

        /// Adds a C-style null-terminated byte array of `capacity` bytes.
        public StructBuilder cStringField(String name, long capacity) {
            return field(name, MemoryLayout.sequenceLayout(capacity, ValueLayout.JAVA_BYTE));
        }

        /// Overrides the natural struct/union alignment.
        public StructBuilder alignedTo(long alignment) {
            if (alignment <= 0 || (alignment & (alignment - 1)) != 0) {
                throw new IllegalArgumentException(
                        "alignment must be a positive power of two, was " + alignment);
            }
            this.customAlignment = alignment;
            return this;
        }

        /// Builds the [StructLayout] or [UnionLayout] with named fields.
        public MemoryLayout build() {
            MemoryLayout[] childLayouts = new MemoryLayout[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                childLayouts[i] = fields.get(i).layout().withName(fields.get(i).name());
            }
            MemoryLayout built = union
                    ? MemoryLayout.unionLayout(childLayouts)
                    : MemoryLayout.structLayout(childLayouts);

            if (customAlignment >= 0) {
                built = built.withByteAlignment(customAlignment);
            }
            return built;
        }

        /// Builds the layout and returns a [MappedRecordFactory] that produces
        /// [MappedRecord] wrappers with typed field getters/setters.
        ///
        /// Field offsets are resolved eagerly from the built layout, so each
        /// [MappedRecord] call is a single typed segment accessor with no field
        /// resolution overhead.
        public MappedRecordFactory buildRecord() {
            MemoryLayout layout = build();
            Map<String, TypedField> fieldsMap = new LinkedHashMap<>();
            for (Field f : fields) {
                long offset;
                try {
                    offset = layout.byteOffset(MemoryLayout.PathElement.groupElement(f.name()));
                } catch (IllegalArgumentException ignored) {
                    offset = -1;
                }
                fieldsMap.put(f.name(), new TypedField(offset, f.layout()));
            }
            return new MappedRecordFactory(layout, fieldsMap);
        }

        private record Field(String name, MemoryLayout layout) {
        }
    }

    // -----------------------------------------------------------------------
    // Typed field — uses MemorySegment typed accessors directly
    // -----------------------------------------------------------------------

    /// A struct field pre-bound to its offset and carrier type.
    ///
    /// Getters and setters dispatch to the correct `MemorySegment.get/set`
    /// overload based on the field's [ValueLayout].
    record TypedField(long offset, MemoryLayout layout) {

        Object get(MemorySegment segment) {
            Class<?> carrier = carrier(layout);
            if (carrier == null) {
                throw new UnsupportedOperationException(
                        "Cannot determine carrier for layout " + layout.getClass().getName());
            }
            return switch (carrier) {
                case Class<?> c when c == byte.class -> segment.get(ValueLayout.JAVA_BYTE, offset);
                case Class<?> c when c == boolean.class -> segment.get(ValueLayout.JAVA_BOOLEAN, offset);
                case Class<?> c when c == char.class -> segment.get(ValueLayout.JAVA_CHAR, offset);
                case Class<?> c when c == short.class -> segment.get(ValueLayout.JAVA_SHORT, offset);
                case Class<?> c when c == int.class -> segment.get(ValueLayout.JAVA_INT, offset);
                case Class<?> c when c == long.class -> segment.get(ValueLayout.JAVA_LONG, offset);
                case Class<?> c when c == float.class -> segment.get(ValueLayout.JAVA_FLOAT, offset);
                case Class<?> c when c == double.class -> segment.get(ValueLayout.JAVA_DOUBLE, offset);
                case Class<?> c when c == MemorySegment.class -> segment.get(ValueLayout.ADDRESS, offset);
                default -> throw new UnsupportedOperationException(
                        "Unsupported carrier " + carrier.getName());
            };
        }

        void set(MemorySegment segment, Object value) {
            Class<?> carrier = carrier(layout);
            if (carrier == null) {
                throw new UnsupportedOperationException(
                        "Cannot determine carrier for layout " + layout.getClass().getName());
            }
            try {
                switch (carrier) {
                    case Class<?> c when c == byte.class ->
                            segment.set(ValueLayout.JAVA_BYTE, offset, (Byte) value);
                    case Class<?> c when c == boolean.class ->
                            segment.set(ValueLayout.JAVA_BOOLEAN, offset, (Boolean) value);
                    case Class<?> c when c == char.class ->
                            segment.set(ValueLayout.JAVA_CHAR, offset, (Character) value);
                    case Class<?> c when c == short.class ->
                            segment.set(ValueLayout.JAVA_SHORT, offset, (Short) value);
                    case Class<?> c when c == int.class ->
                            segment.set(ValueLayout.JAVA_INT, offset, (Integer) value);
                    case Class<?> c when c == long.class ->
                            segment.set(ValueLayout.JAVA_LONG, offset, (Long) value);
                    case Class<?> c when c == float.class ->
                            segment.set(ValueLayout.JAVA_FLOAT, offset, (Float) value);
                    case Class<?> c when c == double.class ->
                            segment.set(ValueLayout.JAVA_DOUBLE, offset, (Double) value);
                    case Class<?> c when c == MemorySegment.class ->
                            segment.set(ValueLayout.ADDRESS, offset, (MemorySegment) value);
                    default -> throw new UnsupportedOperationException(
                            "Unsupported carrier " + carrier.getName());
                }
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(
                        "Cannot assign value of type "
                                + (value == null ? "null" : value.getClass().getName())
                                + " to field with layout " + layout, e);
            }
        }

        private static Class<?> carrier(MemoryLayout layout) {
            if (layout instanceof ValueLayout vl) {
                return vl.carrier();
            }
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // MappedRecord helpers
    // -----------------------------------------------------------------------

    /// Factory that produces [MappedRecord] wrappers from segments that match
    /// a known [StructLayout].
    public static final class MappedRecordFactory {

        private final MemoryLayout layout;
        private final Map<String, TypedField> fields;

        MappedRecordFactory(MemoryLayout layout, Map<String, TypedField> fields) {
            this.layout = layout;
            this.fields = Map.copyOf(fields);
        }

        /// The layout this factory was built for.
        public MemoryLayout layout() {
            return layout;
        }

        /// Wraps a segment as a [MappedRecord].
        ///
        /// @param segment the segment to wrap, must have size `layout.byteSize()`
        public MappedRecord map(MemorySegment segment) {
            Objects.requireNonNull(segment, "segment must not be null");
            long expected = layout.byteSize();
            if (segment.byteSize() != expected) {
                throw new IllegalArgumentException(
                        "segment size " + segment.byteSize()
                                + " does not match layout size " + expected);
            }
            return new MappedRecord(segment, fields);
        }
    }

    /// A typed wrapper around a `MemorySegment` that exposes named field
    /// getters/setters backed by precomputed offsets.
    ///
    /// Getters return `Object`; callers cast for convenience: `(int) record.get("x")`.
    /// Setters accept boxed primitives (e.g. `record.set("x", 42)`) and the
    /// wrapper dispatches to the correct [MemorySegment] typed accessor.
    public static final class MappedRecord {

        private final MemorySegment segment;
        private final Map<String, TypedField> fields;

        MappedRecord(MemorySegment segment, Map<String, TypedField> fields) {
            this.segment = segment;
            this.fields = fields;
        }

        /// The wrapped segment.
        public MemorySegment segment() {
            return segment;
        }

        /// Reads a field by name.
        public Object get(String name) {
            return field(name).get(segment);
        }

        /// Writes a field by name.
        public void set(String name, Object value) {
            field(name).set(segment, value);
        }

        private TypedField field(String name) {
            TypedField field = fields.get(name);
            if (field == null) {
                throw new IllegalArgumentException("unknown field name: " + name);
            }
            return field;
        }
    }
}
