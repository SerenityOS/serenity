/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.foreign.abi.x64.sysv;

import jdk.incubator.foreign.*;
import jdk.internal.foreign.Utils;
import jdk.internal.foreign.abi.SharedUtils;
import jdk.internal.misc.Unsafe;

import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static jdk.internal.foreign.PlatformLayouts.SysV;
import static jdk.incubator.foreign.CLinker.VaList;
import static jdk.incubator.foreign.MemoryLayout.PathElement.groupElement;
import static jdk.internal.foreign.abi.SharedUtils.SimpleVaArg;
import static jdk.internal.foreign.abi.SharedUtils.THROWING_ALLOCATOR;
import static jdk.internal.foreign.abi.SharedUtils.checkCompatibleType;
import static jdk.internal.foreign.abi.SharedUtils.vhPrimitiveOrAddress;

// See https://software.intel.com/sites/default/files/article/402129/mpx-linux64-abi.pdf "3.5.7 Variable Argument Lists"
public non-sealed class SysVVaList implements VaList {
    private static final Unsafe U = Unsafe.getUnsafe();

    static final Class<?> CARRIER = MemoryAddress.class;

//    struct typedef __va_list_tag __va_list_tag {
//        unsigned int               gp_offset;            /*     0     4 */
//        unsigned int               fp_offset;            /*     4     4 */
//        void *                     overflow_arg_area;    /*     8     8 */
//        void *                     reg_save_area;        /*    16     8 */
//
//        /* size: 24, cachelines: 1, members: 4 */
//        /* last cacheline: 24 bytes */
//    };
    static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        SysV.C_INT.withName("gp_offset"),
        SysV.C_INT.withName("fp_offset"),
        SysV.C_POINTER.withName("overflow_arg_area"),
        SysV.C_POINTER.withName("reg_save_area")
    ).withName("__va_list_tag");

    private static final MemoryLayout GP_REG = MemoryLayout.valueLayout(64, ByteOrder.nativeOrder());
    private static final MemoryLayout FP_REG = MemoryLayout.valueLayout(128, ByteOrder.nativeOrder());

    private static final GroupLayout LAYOUT_REG_SAVE_AREA = MemoryLayout.structLayout(
        GP_REG.withName("%rdi"),
        GP_REG.withName("%rsi"),
        GP_REG.withName("%rdx"),
        GP_REG.withName("%rcx"),
        GP_REG.withName("%r8"),
        GP_REG.withName("%r9"),
        FP_REG.withName("%xmm0"),
        FP_REG.withName("%xmm1"),
        FP_REG.withName("%xmm2"),
        FP_REG.withName("%xmm3"),
        FP_REG.withName("%xmm4"),
        FP_REG.withName("%xmm5"),
        FP_REG.withName("%xmm6"),
        FP_REG.withName("%xmm7")
// specification and implementation differ as to whether the following are part of a reg save area
// Let's go with the implementation, since then it actually works :)
//        FP_REG.withName("%xmm8"),
//        FP_REG.withName("%xmm9"),
//        FP_REG.withName("%xmm10"),
//        FP_REG.withName("%xmm11"),
//        FP_REG.withName("%xmm12"),
//        FP_REG.withName("%xmm13"),
//        FP_REG.withName("%xmm14"),
//        FP_REG.withName("%xmm15")
    );

    private static final long FP_OFFSET = LAYOUT_REG_SAVE_AREA.byteOffset(groupElement("%xmm0"));

    private static final int GP_SLOT_SIZE = (int) GP_REG.byteSize();
    private static final int FP_SLOT_SIZE = (int) FP_REG.byteSize();

    private static final int MAX_GP_OFFSET = (int) FP_OFFSET; // 6 regs used
    private static final int MAX_FP_OFFSET = (int) LAYOUT_REG_SAVE_AREA.byteSize(); // 8 16 byte regs

    private static final VarHandle VH_fp_offset = LAYOUT.varHandle(int.class, groupElement("fp_offset"));
    private static final VarHandle VH_gp_offset = LAYOUT.varHandle(int.class, groupElement("gp_offset"));
    private static final VarHandle VH_overflow_arg_area
        = MemoryHandles.asAddressVarHandle(LAYOUT.varHandle(long.class, groupElement("overflow_arg_area")));
    private static final VarHandle VH_reg_save_area
        = MemoryHandles.asAddressVarHandle(LAYOUT.varHandle(long.class, groupElement("reg_save_area")));

    private static final Cleaner cleaner = Cleaner.create();
    private static final VaList EMPTY = new SharedUtils.EmptyVaList(emptyListAddress());

    private final MemorySegment segment;
    private final MemorySegment regSaveArea;

    private SysVVaList(MemorySegment segment, MemorySegment regSaveArea) {
        this.segment = segment;
        this.regSaveArea = regSaveArea;
    }

    private static SysVVaList readFromSegment(MemorySegment segment) {
        MemorySegment regSaveArea = getRegSaveArea(segment);
        return new SysVVaList(segment, regSaveArea);
    }

    private static MemoryAddress emptyListAddress() {
        long ptr = U.allocateMemory(LAYOUT.byteSize());
        MemorySegment base = MemoryAddress.ofLong(ptr).asSegment(
                LAYOUT.byteSize(), () -> U.freeMemory(ptr), ResourceScope.newSharedScope());
        cleaner.register(SysVVaList.class, () -> base.scope().close());
        VH_gp_offset.set(base, MAX_GP_OFFSET);
        VH_fp_offset.set(base, MAX_FP_OFFSET);
        VH_overflow_arg_area.set(base, MemoryAddress.NULL);
        VH_reg_save_area.set(base, MemoryAddress.NULL);
        return base.address();
    }

    public static VaList empty() {
        return EMPTY;
    }

    private int currentGPOffset() {
        return (int) VH_gp_offset.get(segment);
    }

    private void currentGPOffset(int i) {
        VH_gp_offset.set(segment, i);
    }

    private int currentFPOffset() {
        return (int) VH_fp_offset.get(segment);
    }

    private void currentFPOffset(int i) {
        VH_fp_offset.set(segment, i);
    }

    private MemoryAddress stackPtr() {
        return (MemoryAddress) VH_overflow_arg_area.get(segment);
    }

    private void stackPtr(MemoryAddress ptr) {
        VH_overflow_arg_area.set(segment, ptr);
    }

    private MemorySegment regSaveArea() {
        return getRegSaveArea(segment);
    }

    private static MemorySegment getRegSaveArea(MemorySegment segment) {
        return ((MemoryAddress)VH_reg_save_area.get(segment)).asSegment(
                LAYOUT_REG_SAVE_AREA.byteSize(), segment.scope());
    }

    private void preAlignStack(MemoryLayout layout) {
        if (layout.byteAlignment() > 8) {
            stackPtr(Utils.alignUp(stackPtr(), 16));
        }
    }

    private void postAlignStack(MemoryLayout layout) {
        stackPtr(Utils.alignUp(stackPtr().addOffset(layout.byteSize()), 8));
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
        return read(carrier, layout, THROWING_ALLOCATOR);
    }

    private Object read(Class<?> carrier, MemoryLayout layout, SegmentAllocator allocator) {
        Objects.requireNonNull(layout);
        checkCompatibleType(carrier, layout, SysVx64Linker.ADDRESS_SIZE);
        TypeClass typeClass = TypeClass.classifyLayout(layout);
        if (isRegOverflow(currentGPOffset(), currentFPOffset(), typeClass)
                || typeClass.inMemory()) {
            preAlignStack(layout);
            return switch (typeClass.kind()) {
                case STRUCT -> {
                    MemorySegment slice = stackPtr().asSegment(layout.byteSize(), scope());
                    MemorySegment seg = allocator.allocate(layout);
                    seg.copyFrom(slice);
                    postAlignStack(layout);
                    yield seg;
                }
                case POINTER, INTEGER, FLOAT -> {
                    VarHandle reader = vhPrimitiveOrAddress(carrier, layout);
                    try (ResourceScope localScope = ResourceScope.newConfinedScope()) {
                        MemorySegment slice = stackPtr().asSegment(layout.byteSize(), localScope);
                        Object res = reader.get(slice);
                        postAlignStack(layout);
                        yield res;
                    }
                }
            };
        } else {
            return switch (typeClass.kind()) {
                case STRUCT -> {
                    MemorySegment value = allocator.allocate(layout);
                    int classIdx = 0;
                    long offset = 0;
                    while (offset < layout.byteSize()) {
                        final long copy = Math.min(layout.byteSize() - offset, 8);
                        boolean isSSE = typeClass.classes.get(classIdx++) == ArgumentClassImpl.SSE;
                        MemorySegment slice = value.asSlice(offset, copy);
                        if (isSSE) {
                            slice.copyFrom(regSaveArea.asSlice(currentFPOffset(), copy));
                            currentFPOffset(currentFPOffset() + FP_SLOT_SIZE);
                        } else {
                            slice.copyFrom(regSaveArea.asSlice(currentGPOffset(), copy));
                            currentGPOffset(currentGPOffset() + GP_SLOT_SIZE);
                        }
                        offset += copy;
                    }
                    yield value;
                }
                case POINTER, INTEGER -> {
                    VarHandle reader = SharedUtils.vhPrimitiveOrAddress(carrier, layout);
                    Object res = reader.get(regSaveArea.asSlice(currentGPOffset()));
                    currentGPOffset(currentGPOffset() + GP_SLOT_SIZE);
                    yield res;
                }
                case FLOAT -> {
                    VarHandle reader = layout.varHandle(carrier);
                    Object res = reader.get(regSaveArea.asSlice(currentFPOffset()));
                    currentFPOffset(currentFPOffset() + FP_SLOT_SIZE);
                    yield res;
                }
            };
        }
    }

    @Override
    public void skip(MemoryLayout... layouts) {
        Objects.requireNonNull(layouts);
        for (MemoryLayout layout : layouts) {
            Objects.requireNonNull(layout);
            TypeClass typeClass = TypeClass.classifyLayout(layout);
            if (isRegOverflow(currentGPOffset(), currentFPOffset(), typeClass)) {
                preAlignStack(layout);
                postAlignStack(layout);
            } else {
                currentGPOffset(currentGPOffset() + (((int) typeClass.nIntegerRegs()) * GP_SLOT_SIZE));
                currentFPOffset(currentFPOffset() + (((int) typeClass.nVectorRegs()) * FP_SLOT_SIZE));
            }
        }
    }

    static SysVVaList.Builder builder(ResourceScope scope) {
        return new SysVVaList.Builder(scope);
    }

    public static VaList ofAddress(MemoryAddress ma, ResourceScope scope) {
        return readFromSegment(ma.asSegment(LAYOUT.byteSize(), scope));
    }

    @Override
    public ResourceScope scope() {
        return segment.scope();
    }

    @Override
    public VaList copy() {
        MemorySegment copy = MemorySegment.allocateNative(LAYOUT, segment.scope());
        copy.copyFrom(segment);
        return new SysVVaList(copy, regSaveArea);
    }

    @Override
    public MemoryAddress address() {
        return segment.address();
    }

    private static boolean isRegOverflow(long currentGPOffset, long currentFPOffset, TypeClass typeClass) {
        return currentGPOffset > MAX_GP_OFFSET - typeClass.nIntegerRegs() * GP_SLOT_SIZE
                || currentFPOffset > MAX_FP_OFFSET - typeClass.nVectorRegs() * FP_SLOT_SIZE;
    }

    @Override
    public String toString() {
        return "SysVVaList{"
               + "gp_offset=" + currentGPOffset()
               + ", fp_offset=" + currentFPOffset()
               + ", overflow_arg_area=" + stackPtr()
               + ", reg_save_area=" + regSaveArea()
               + '}';
    }

    public static non-sealed class Builder implements VaList.Builder {
        private final ResourceScope scope;
        private final MemorySegment reg_save_area;
        private long currentGPOffset = 0;
        private long currentFPOffset = FP_OFFSET;
        private final List<SimpleVaArg> stackArgs = new ArrayList<>();

        public Builder(ResourceScope scope) {
            this.scope = scope;
            this.reg_save_area = MemorySegment.allocateNative(LAYOUT_REG_SAVE_AREA, scope);
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

        private Builder arg(Class<?> carrier, MemoryLayout layout, Object value) {
            Objects.requireNonNull(layout);
            Objects.requireNonNull(value);
            checkCompatibleType(carrier, layout, SysVx64Linker.ADDRESS_SIZE);
            TypeClass typeClass = TypeClass.classifyLayout(layout);
            if (isRegOverflow(currentGPOffset, currentFPOffset, typeClass)
                    || typeClass.inMemory()) {
                // stack it!
                stackArgs.add(new SimpleVaArg(carrier, layout, value));
            } else {
                switch (typeClass.kind()) {
                    case STRUCT -> {
                        MemorySegment valueSegment = (MemorySegment) value;
                        int classIdx = 0;
                        long offset = 0;
                        while (offset < layout.byteSize()) {
                            final long copy = Math.min(layout.byteSize() - offset, 8);
                            boolean isSSE = typeClass.classes.get(classIdx++) == ArgumentClassImpl.SSE;
                            MemorySegment slice = valueSegment.asSlice(offset, copy);
                            if (isSSE) {
                                reg_save_area.asSlice(currentFPOffset, copy).copyFrom(slice);
                                currentFPOffset += FP_SLOT_SIZE;
                            } else {
                                reg_save_area.asSlice(currentGPOffset, copy).copyFrom(slice);
                                currentGPOffset += GP_SLOT_SIZE;
                            }
                            offset += copy;
                        }
                    }
                    case POINTER, INTEGER -> {
                        VarHandle writer = SharedUtils.vhPrimitiveOrAddress(carrier, layout);
                        writer.set(reg_save_area.asSlice(currentGPOffset), value);
                        currentGPOffset += GP_SLOT_SIZE;
                    }
                    case FLOAT -> {
                        VarHandle writer = layout.varHandle(carrier);
                        writer.set(reg_save_area.asSlice(currentFPOffset), value);
                        currentFPOffset += FP_SLOT_SIZE;
                    }
                }
            }
            return this;
        }

        private boolean isEmpty() {
            return currentGPOffset == 0 && currentFPOffset == FP_OFFSET && stackArgs.isEmpty();
        }

        public VaList build() {
            if (isEmpty()) {
                return EMPTY;
            }

            SegmentAllocator allocator = SegmentAllocator.arenaAllocator(scope);
            MemorySegment vaListSegment = allocator.allocate(LAYOUT);
            MemoryAddress stackArgsPtr = MemoryAddress.NULL;
            if (!stackArgs.isEmpty()) {
                long stackArgsSize = stackArgs.stream().reduce(0L, (acc, e) -> acc + e.layout.byteSize(), Long::sum);
                MemorySegment stackArgsSegment = allocator.allocate(stackArgsSize, 16);
                MemorySegment maOverflowArgArea = stackArgsSegment;
                for (SimpleVaArg arg : stackArgs) {
                    if (arg.layout.byteSize() > 8) {
                        maOverflowArgArea = Utils.alignUp(maOverflowArgArea, Math.min(16, arg.layout.byteSize()));
                    }
                    if (arg.value instanceof MemorySegment) {
                        maOverflowArgArea.copyFrom((MemorySegment) arg.value);
                    } else {
                        VarHandle writer = arg.varHandle();
                        writer.set(maOverflowArgArea, arg.value);
                    }
                    maOverflowArgArea = maOverflowArgArea.asSlice(arg.layout.byteSize());
                }
                stackArgsPtr = stackArgsSegment.address();
            }

            VH_fp_offset.set(vaListSegment, (int) FP_OFFSET);
            VH_overflow_arg_area.set(vaListSegment, stackArgsPtr);
            VH_reg_save_area.set(vaListSegment, reg_save_area.address());
            assert reg_save_area.scope().ownerThread() == vaListSegment.scope().ownerThread();
            return new SysVVaList(vaListSegment, reg_save_area);
        }
    }
}
