/*
 *  Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */
package jdk.internal.foreign;

import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.internal.access.JavaLangInvokeAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.access.foreign.MemorySegmentProxy;

import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.SequenceLayout;
import jdk.incubator.foreign.ValueLayout;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;

/**
 * This class provide support for constructing layout paths; that is, starting from a root path (see {@link #rootPath(MemoryLayout, ToLongFunction)},
 * a path can be constructed by selecting layout elements using the selector methods provided by this class
 * (see {@link #sequenceElement()}, {@link #sequenceElement(long)}, {@link #sequenceElement(long, long)}, {@link #groupElement(String)}).
 * Once a path has been fully constructed, clients can ask for the offset associated with the layout element selected
 * by the path (see {@link #offset}), or obtain a memory access var handle to access the selected layout element
 * given an address pointing to a segment associated with the root layout (see {@link #dereferenceHandle(Class)}).
 */
public class LayoutPath {

    private static final JavaLangInvokeAccess JLI = SharedSecrets.getJavaLangInvokeAccess();

    private static final MethodHandle ADD_STRIDE;
    private static final MethodHandle MH_ADD_SCALED_OFFSET;
    private static final MethodHandle MH_SLICE;

    private static final int UNSPECIFIED_ELEM_INDEX = -1;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            ADD_STRIDE = lookup.findStatic(LayoutPath.class, "addStride",
                    MethodType.methodType(long.class, MemorySegment.class, long.class, long.class, long.class));
            MH_ADD_SCALED_OFFSET = lookup.findStatic(LayoutPath.class, "addScaledOffset",
                    MethodType.methodType(long.class, long.class, long.class, long.class));
            MH_SLICE = lookup.findVirtual(MemorySegment.class, "asSlice",
                    MethodType.methodType(MemorySegment.class, long.class, long.class));
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private final MemoryLayout layout;
    private final long offset;
    private final LayoutPath enclosing;
    private final long[] strides;
    private final long elementIndex;
    private final ToLongFunction<MemoryLayout> sizeFunc;

    private LayoutPath(MemoryLayout layout, long offset, long[] strides, long elementIndex, LayoutPath enclosing, ToLongFunction<MemoryLayout> sizeFunc) {
        this.layout = layout;
        this.offset = offset;
        this.strides = strides;
        this.enclosing = enclosing;
        this.elementIndex = elementIndex;
        this.sizeFunc = sizeFunc;
    }

    // Layout path selector methods

    public LayoutPath sequenceElement() {
        check(SequenceLayout.class, "attempting to select a sequence element from a non-sequence layout");
        SequenceLayout seq = (SequenceLayout)layout;
        MemoryLayout elem = seq.elementLayout();
        return LayoutPath.nestedPath(elem, offset, addStride(sizeFunc.applyAsLong(elem)), UNSPECIFIED_ELEM_INDEX, this);
    }

    public LayoutPath sequenceElement(long start, long step) {
        check(SequenceLayout.class, "attempting to select a sequence element from a non-sequence layout");
        SequenceLayout seq = (SequenceLayout)layout;
        checkSequenceBounds(seq, start);
        MemoryLayout elem = seq.elementLayout();
        long elemSize = sizeFunc.applyAsLong(elem);
        return LayoutPath.nestedPath(elem, offset + (start * elemSize), addStride(elemSize * step),
                UNSPECIFIED_ELEM_INDEX, this);
    }

    public LayoutPath sequenceElement(long index) {
        check(SequenceLayout.class, "attempting to select a sequence element from a non-sequence layout");
        SequenceLayout seq = (SequenceLayout)layout;
        checkSequenceBounds(seq, index);
        long elemOffset = 0;
        if (index > 0) {
            //if index == 0, we do not depend on sequence element size, so skip
            long elemSize = sizeFunc.applyAsLong(seq.elementLayout());
            elemOffset = elemSize * index;
        }
        return LayoutPath.nestedPath(seq.elementLayout(), offset + elemOffset, strides, index, this);
    }

    public LayoutPath groupElement(String name) {
        check(GroupLayout.class, "attempting to select a group element from a non-group layout");
        GroupLayout g = (GroupLayout)layout;
        long offset = 0;
        MemoryLayout elem = null;
        int index = -1;
        for (int i = 0; i < g.memberLayouts().size(); i++) {
            MemoryLayout l = g.memberLayouts().get(i);
            if (l.name().isPresent() &&
                l.name().get().equals(name)) {
                elem = l;
                index = i;
                break;
            } else if (g.isStruct()) {
                offset += sizeFunc.applyAsLong(l);
            }
        }
        if (elem == null) {
            throw badLayoutPath("cannot resolve '" + name + "' in layout " + layout);
        }
        return LayoutPath.nestedPath(elem, this.offset + offset, strides, index, this);
    }

    // Layout path projections

    public long offset() {
        return offset;
    }

    public VarHandle dereferenceHandle(Class<?> carrier) {
        Utils.checkPrimitiveCarrierCompat(carrier, layout);
        checkAlignment(this);

        List<Class<?>> expectedCoordinates = new ArrayList<>();
        Deque<Integer> perms = new ArrayDeque<>();
        perms.addFirst(0);
        expectedCoordinates.add(MemorySegment.class);

        VarHandle handle = Utils.fixUpVarHandle(JLI.memoryAccessVarHandle(carrier, true, layout.byteAlignment() - 1,
                ((ValueLayout)layout).order()));

        for (int i = 0 ; i < strides.length ; i++) {
            expectedCoordinates.add(long.class);
            perms.addFirst(0);
            perms.addLast(i + 1);
            //add stride
            handle = MemoryHandles.collectCoordinates(handle, 1 + i,
                    MethodHandles.insertArguments(ADD_STRIDE, 1, Utils.bitsToBytesOrThrow(strides[strides.length - 1 - i], IllegalStateException::new))); // MS, long, MS_n, long_n, long
        }
        //add offset
        handle = MemoryHandles.insertCoordinates(handle, 1 + strides.length, Utils.bitsToBytesOrThrow(offset, IllegalStateException::new));

        if (strides.length > 0) {
            // remove duplicate MS args
            handle = MemoryHandles.permuteCoordinates(handle, expectedCoordinates, perms.stream().mapToInt(i -> i).toArray());
        }
        return handle;
    }

    private static long addScaledOffset(long base, long index, long stride) {
        return base + (stride * index);
    }

    public MethodHandle offsetHandle() {
        MethodHandle mh = MethodHandles.identity(long.class);
        for (int i = strides.length - 1; i >=0; i--) {
            MethodHandle collector = MethodHandles.insertArguments(MH_ADD_SCALED_OFFSET, 2, strides[i]);
            // (J, ...) -> J to (J, J, ...) -> J
            // i.e. new coord is prefixed. Last coord will correspond to innermost layout
            mh = MethodHandles.collectArguments(mh, 0, collector);
        }
        mh = MethodHandles.insertArguments(mh, 0, offset);
        return mh;
    }

    public MethodHandle sliceHandle() {
        if (strides.length == 0) {
            // trigger checks eagerly
            Utils.bitsToBytesOrThrow(offset, Utils.bitsToBytesThrowOffset);
        }

        MethodHandle offsetHandle = offsetHandle(); // bit offset
        offsetHandle = MethodHandles.filterReturnValue(offsetHandle, Utils.MH_bitsToBytesOrThrowForOffset); // byte offset

        MethodHandle sliceHandle = MH_SLICE; // (MS, long, long) -> MS
        sliceHandle = MethodHandles.insertArguments(sliceHandle, 2, layout.byteSize()); // (MS, long) -> MS
        sliceHandle = MethodHandles.collectArguments(sliceHandle, 1, offsetHandle); // (MS, ...) -> MS

        return sliceHandle;
    }

    public MemoryLayout layout() {
        return layout;
    }

    public MemoryLayout map(UnaryOperator<MemoryLayout> op) {
        MemoryLayout newLayout = op.apply(layout);
        if (enclosing == null) {
            return newLayout;
        } else if (enclosing.layout instanceof SequenceLayout) {
            SequenceLayout seq = (SequenceLayout)enclosing.layout;
            if (seq.elementCount().isPresent()) {
                return enclosing.map(l -> dup(l, MemoryLayout.sequenceLayout(seq.elementCount().getAsLong(), newLayout)));
            } else {
                return enclosing.map(l -> dup(l, MemoryLayout.sequenceLayout(newLayout)));
            }
        } else if (enclosing.layout instanceof GroupLayout) {
            GroupLayout g = (GroupLayout)enclosing.layout;
            List<MemoryLayout> newElements = new ArrayList<>(g.memberLayouts());
            //if we selected a layout in a group we must have a valid index
            newElements.set((int)elementIndex, newLayout);
            if (g.isUnion()) {
                return enclosing.map(l -> dup(l, MemoryLayout.unionLayout(newElements.toArray(new MemoryLayout[0]))));
            } else {
                return enclosing.map(l -> dup(l, MemoryLayout.structLayout(newElements.toArray(new MemoryLayout[0]))));
            }
        } else {
            return newLayout;
        }
    }

    private MemoryLayout dup(MemoryLayout oldLayout, MemoryLayout newLayout) {
        newLayout = newLayout.withBitAlignment(oldLayout.bitAlignment());
        if (oldLayout.name().isPresent()) {
            newLayout.withName(oldLayout.name().get());
        }
        return newLayout;
    }

    // Layout path construction

    public static LayoutPath rootPath(MemoryLayout layout, ToLongFunction<MemoryLayout> sizeFunc) {
        return new LayoutPath(layout, 0L, EMPTY_STRIDES, -1, null, sizeFunc);
    }

    private static LayoutPath nestedPath(MemoryLayout layout, long offset, long[] strides, long elementIndex, LayoutPath encl) {
        return new LayoutPath(layout, offset, strides, elementIndex, encl, encl.sizeFunc);
    }

    // Helper methods

    private void check(Class<?> layoutClass, String msg) {
        if (!layoutClass.isAssignableFrom(layout.getClass())) {
            throw badLayoutPath(msg);
        }
    }

    private void checkSequenceBounds(SequenceLayout seq, long index) {
        if (seq.elementCount().isPresent() && index >= seq.elementCount().getAsLong()) {
            throw badLayoutPath(String.format("Sequence index out of bound; found: %d, size: %d", index, seq.elementCount().getAsLong()));
        }
    }

    private static IllegalArgumentException badLayoutPath(String cause) {
        return new IllegalArgumentException("Bad layout path: " + cause);
    }

    private static void checkAlignment(LayoutPath path) {
        MemoryLayout layout = path.layout;
        long alignment = layout.bitAlignment();
        if (path.offset % alignment != 0) {
            throw new UnsupportedOperationException("Invalid alignment requirements for layout " + layout);
        }
        for (long stride : path.strides) {
            if (stride % alignment != 0) {
                throw new UnsupportedOperationException("Alignment requirements for layout " + layout + " do not match stride " + stride);
            }
        }
        LayoutPath encl = path.enclosing;
        if (encl != null) {
            if (encl.layout.bitAlignment() < alignment) {
                throw new UnsupportedOperationException("Alignment requirements for layout " + layout + " do not match those for enclosing layout " + encl.layout);
            }
            checkAlignment(encl);
        }
    }

    private long[] addStride(long stride) {
        long[] newStrides = new long[strides.length + 1];
        System.arraycopy(strides, 0, newStrides, 0, strides.length);
        newStrides[strides.length] = stride;
        return newStrides;
    }

    private static final long[] EMPTY_STRIDES = new long[0];

    /**
     * This class provides an immutable implementation for the {@code PathElement} interface. A path element implementation
     * is simply a pointer to one of the selector methods provided by the {@code LayoutPath} class.
     */
    public static final class PathElementImpl implements MemoryLayout.PathElement, UnaryOperator<LayoutPath> {

        public enum PathKind {
            SEQUENCE_ELEMENT("unbound sequence element"),
            SEQUENCE_ELEMENT_INDEX("bound sequence element"),
            SEQUENCE_RANGE("sequence range"),
            GROUP_ELEMENT("group element");

            final String description;

            PathKind(String description) {
                this.description = description;
            }

            public String description() {
                return description;
            }
        }

        final PathKind kind;
        final UnaryOperator<LayoutPath> pathOp;

        public PathElementImpl(PathKind kind, UnaryOperator<LayoutPath> pathOp) {
            this.kind = kind;
            this.pathOp = pathOp;
        }

        @Override
        public LayoutPath apply(LayoutPath layoutPath) {
            return pathOp.apply(layoutPath);
        }

        public PathKind kind() {
            return kind;
        }
    }

    private static long addStride(MemorySegment segment, long stride, long base, long index) {
        return MemorySegmentProxy.addOffsets(base,
                    MemorySegmentProxy.multiplyOffsets(stride, index, ((MemorySegmentProxy)segment)), (MemorySegmentProxy)segment);
    }
}
