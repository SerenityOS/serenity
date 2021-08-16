/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Arm Limited. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.internal.foreign.abi.aarch64.macos;

import jdk.incubator.foreign.*;
import jdk.incubator.foreign.CLinker.VaList;
import jdk.internal.foreign.ResourceScopeImpl;
import jdk.internal.foreign.abi.SharedUtils;
import jdk.internal.foreign.abi.SharedUtils.SimpleVaArg;
import jdk.internal.foreign.abi.aarch64.*;

import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static jdk.internal.foreign.PlatformLayouts.AArch64.C_POINTER;
import static jdk.internal.foreign.abi.SharedUtils.alignUp;

/**
 * Simplified va_list implementation used on macOS where all variadic
 * parameters are passed on the stack and the type of va_list decays to
 * char* instead of the structure defined in the AAPCS.
 */
public non-sealed class MacOsAArch64VaList implements VaList {
    public static final Class<?> CARRIER = MemoryAddress.class;
    private static final long VA_SLOT_SIZE_BYTES = 8;
    private static final VarHandle VH_address = MemoryHandles.asAddressVarHandle(C_POINTER.varHandle(long.class));

    private static final VaList EMPTY = new SharedUtils.EmptyVaList(MemoryAddress.NULL);

    private MemorySegment segment;
    private final ResourceScope scope;

    private MacOsAArch64VaList(MemorySegment segment, ResourceScope scope) {
        this.segment = segment;
        this.scope = scope;
    }

    public static final VaList empty() {
        return EMPTY;
    }

    @Override
    public int vargAsInt(MemoryLayout layout) {
        return (int) read(int.class, layout);
    }

    @Override
    public long vargAsLong(MemoryLayout layout) {
        return (long) read(long.class, layout);
    }

    @Override
    public double vargAsDouble(MemoryLayout layout) {
        return (double) read(double.class, layout);
    }

    @Override
    public MemoryAddress vargAsAddress(MemoryLayout layout) {
        return (MemoryAddress) read(MemoryAddress.class, layout);
    }

    @Override
    public MemorySegment vargAsSegment(MemoryLayout layout, SegmentAllocator allocator) {
        Objects.requireNonNull(allocator);
        return (MemorySegment) read(MemorySegment.class, layout, allocator);
    }

    @Override
    public MemorySegment vargAsSegment(MemoryLayout layout, ResourceScope scope) {
        return vargAsSegment(layout, SegmentAllocator.ofScope(scope));
    }

    private Object read(Class<?> carrier, MemoryLayout layout) {
        return read(carrier, layout, SharedUtils.THROWING_ALLOCATOR);
    }

    private Object read(Class<?> carrier, MemoryLayout layout, SegmentAllocator allocator) {
        Objects.requireNonNull(layout);
        SharedUtils.checkCompatibleType(carrier, layout, MacOsAArch64Linker.ADDRESS_SIZE);
        Object res;
        if (carrier == MemorySegment.class) {
            TypeClass typeClass = TypeClass.classifyLayout(layout);
            res = switch (typeClass) {
                case STRUCT_REFERENCE -> {
                    MemoryAddress structAddr = (MemoryAddress) VH_address.get(segment);
                    MemorySegment struct = structAddr.asSegment(layout.byteSize(), scope());
                    MemorySegment seg = allocator.allocate(layout);
                    seg.copyFrom(struct);
                    segment = segment.asSlice(VA_SLOT_SIZE_BYTES);
                    yield seg;
                }
                case STRUCT_REGISTER, STRUCT_HFA -> {
                    MemorySegment struct = allocator.allocate(layout);
                    struct.copyFrom(segment.asSlice(0L, layout.byteSize()));
                    segment = segment.asSlice(alignUp(layout.byteSize(), VA_SLOT_SIZE_BYTES));
                    yield struct;
                }
                default -> throw new IllegalStateException("Unexpected TypeClass: " + typeClass);
            };
        } else {
            VarHandle reader = SharedUtils.vhPrimitiveOrAddress(carrier, layout);
            res = reader.get(segment);
            segment = segment.asSlice(VA_SLOT_SIZE_BYTES);
        }
        return res;
    }

    @Override
    public void skip(MemoryLayout... layouts) {
        Objects.requireNonNull(layouts);

        for (MemoryLayout layout : layouts) {
            Objects.requireNonNull(layout);
            segment = segment.asSlice(switch (TypeClass.classifyLayout(layout)) {
                case STRUCT_REGISTER, STRUCT_HFA -> alignUp(layout.byteSize(), VA_SLOT_SIZE_BYTES);
                default -> VA_SLOT_SIZE_BYTES;
            });
        }
    }

    static MacOsAArch64VaList ofAddress(MemoryAddress addr, ResourceScope scope) {
        MemorySegment segment = addr.asSegment(Long.MAX_VALUE, scope);
        return new MacOsAArch64VaList(segment, scope);
    }

    static Builder builder(ResourceScope scope) {
        return new Builder(scope);
    }

    @Override
    public ResourceScope scope() {
        return scope;
    }

    @Override
    public VaList copy() {
        ((ResourceScopeImpl)scope).checkValidStateSlow();
        return new MacOsAArch64VaList(segment, scope);
    }

    @Override
    public MemoryAddress address() {
        return segment.address();
    }

    public static non-sealed class Builder implements VaList.Builder {

        private final ResourceScope scope;
        private final List<SimpleVaArg> args = new ArrayList<>();

        public Builder(ResourceScope scope) {
            ((ResourceScopeImpl)scope).checkValidStateSlow();
            this.scope = scope;
        }

        private Builder arg(Class<?> carrier, MemoryLayout layout, Object value) {
            Objects.requireNonNull(layout);
            Objects.requireNonNull(value);
            SharedUtils.checkCompatibleType(carrier, layout, MacOsAArch64Linker.ADDRESS_SIZE);
            args.add(new SimpleVaArg(carrier, layout, value));
            return this;
        }

        @Override
        public Builder vargFromInt(ValueLayout layout, int value) {
            return arg(int.class, layout, value);
        }

        @Override
        public Builder vargFromLong(ValueLayout layout, long value) {
            return arg(long.class, layout, value);
        }

        @Override
        public Builder vargFromDouble(ValueLayout layout, double value) {
            return arg(double.class, layout, value);
        }

        @Override
        public Builder vargFromAddress(ValueLayout layout, Addressable value) {
            return arg(MemoryAddress.class, layout, value.address());
        }

        @Override
        public Builder vargFromSegment(GroupLayout layout, MemorySegment value) {
            return arg(MemorySegment.class, layout, value);
        }

        public VaList build() {
            if (args.isEmpty()) {
                return EMPTY;
            }

            SegmentAllocator allocator = SegmentAllocator.arenaAllocator(scope);

            // Each argument may occupy up to four slots
            MemorySegment segment = allocator.allocate(VA_SLOT_SIZE_BYTES * args.size() * 4);

            List<MemorySegment> attachedSegments = new ArrayList<>();
            attachedSegments.add(segment);
            MemorySegment cursor = segment;

            for (SimpleVaArg arg : args) {
                if (arg.carrier == MemorySegment.class) {
                    MemorySegment msArg = ((MemorySegment) arg.value);
                    TypeClass typeClass = TypeClass.classifyLayout(arg.layout);
                    switch (typeClass) {
                        case STRUCT_REFERENCE -> {
                            MemorySegment copy = allocator.allocate(arg.layout);
                            copy.copyFrom(msArg); // by-value
                            attachedSegments.add(copy);
                            VH_address.set(cursor, copy.address());
                            cursor = cursor.asSlice(VA_SLOT_SIZE_BYTES);
                        }
                        case STRUCT_REGISTER, STRUCT_HFA -> {
                            cursor.copyFrom(msArg.asSlice(0, arg.layout.byteSize()));
                            cursor = cursor.asSlice(alignUp(arg.layout.byteSize(), VA_SLOT_SIZE_BYTES));
                        }
                        default -> throw new IllegalStateException("Unexpected TypeClass: " + typeClass);
                    }
                } else {
                    VarHandle writer = arg.varHandle();
                    writer.set(cursor, arg.value);
                    cursor = cursor.asSlice(VA_SLOT_SIZE_BYTES);
                }
            }

            return new MacOsAArch64VaList(segment, scope);
        }
    }
}
