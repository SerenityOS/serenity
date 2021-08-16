/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.vm.ci.code.test;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.DebugInfo;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueKindFactory;
import jdk.vm.ci.code.site.Call;
import jdk.vm.ci.code.site.ConstantReference;
import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.DataSectionReference;
import jdk.vm.ci.code.site.Infopoint;
import jdk.vm.ci.code.site.InfopointReason;
import jdk.vm.ci.code.site.Mark;
import jdk.vm.ci.code.site.Reference;
import jdk.vm.ci.code.site.Site;
import jdk.vm.ci.hotspot.HotSpotCompiledCode;
import jdk.vm.ci.hotspot.HotSpotCompiledCode.Comment;
import jdk.vm.ci.hotspot.HotSpotCompiledNmethod;
import jdk.vm.ci.hotspot.HotSpotConstant;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Assumptions.Assumption;
import jdk.vm.ci.meta.InvokeTarget;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.VMConstant;
import jdk.vm.ci.meta.ValueKind;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Simple assembler used by the code installation tests.
 */
public abstract class TestAssembler {

    /**
     * Emit the method prologue code (e.g. building the new stack frame).
     */
    public abstract void emitPrologue();

    /**
     * Emit the method epilogue code (e.g. the deopt handler).
     */
    public abstract void emitEpilogue();

    /**
     * Emit code to grow the stack frame.
     *
     * @param size the size in bytes that the stack should grow
     */
    public abstract void emitGrowStack(int size);

    /**
     * Get the register containing the first 32-bit integer argument.
     */
    public abstract Register emitIntArg0();

    /**
     * Get the register containing the second 32-bit integer argument.
     */
    public abstract Register emitIntArg1();

    /**
     * Emit code to add two 32-bit integer registers. May reuse one of the argument registers.
     */
    public abstract Register emitIntAdd(Register a, Register b);

    /**
     * Emit code to load a constant 32-bit integer to a register.
     */
    public abstract Register emitLoadInt(int value);

    /**
     * Emit code to load a constant 64-bit integer to a register.
     */
    public abstract Register emitLoadLong(long value);

    /**
     * Emit code to load a constant single-precision float to a register.
     */
    public abstract Register emitLoadFloat(float value);

    /**
     * Emit code to load a constant oop or metaspace pointer to a register. The pointer may be wide
     * or narrow, depending on {@link HotSpotConstant#isCompressed() c.isCompressed()}.
     */
    public abstract Register emitLoadPointer(HotSpotConstant c);

    /**
     * Emit code to load a wide pointer from the {@link HotSpotCompiledCode#dataSection} to a
     * register.
     */
    public abstract Register emitLoadPointer(DataSectionReference ref);

    /**
     * Emit code to load a narrow pointer from the {@link HotSpotCompiledCode#dataSection} to a
     * register.
     */
    public abstract Register emitLoadNarrowPointer(DataSectionReference ref);

    /**
     * Emit code to load a (wide) pointer from a memory location to a register.
     */
    public abstract Register emitLoadPointer(Register base, int offset);

    /**
     * Emit code to store a 32-bit integer from a register to a new stack slot.
     */
    public abstract StackSlot emitIntToStack(Register a);

    /**
     * Emit code to store a 64-bit integer from a register to a new stack slot.
     */
    public abstract StackSlot emitLongToStack(Register a);

    /**
     * Emit code to store a single-precision float from a register to a new stack slot.
     */
    public abstract StackSlot emitFloatToStack(Register a);

    /**
     * Emit code to store a double-precision float from a register to a new stack slot.
     */
    public abstract StackSlot emitDoubleToStack(Register a);

    /**
     * Emit code to store a wide pointer from a register to a new stack slot.
     */
    public abstract StackSlot emitPointerToStack(Register a);

    /**
     * Emit code to store a narrow pointer from a register to a new stack slot.
     */
    public abstract StackSlot emitNarrowPointerToStack(Register a);

    /**
     * Emit code to uncompress a narrow pointer. The input pointer is guaranteed to be non-null.
     */
    public abstract Register emitUncompressPointer(Register compressed, long base, int shift);

    /**
     * Emit code to return from a function, returning a 32-bit integer.
     */
    public abstract void emitIntRet(Register a);

    /**
     * Emit code to return from a function, returning a single precision float.
     */
    public abstract void emitFloatRet(Register a);

    /**
     * Emit code to return from a function, returning a wide oop pointer.
     */
    public abstract void emitPointerRet(Register a);

    /**
     * Emit code that traps, forcing a deoptimization.
     */
    public abstract void emitTrap(DebugInfo info);

    public final ValueKind<?> narrowOopKind;

    protected final Buffer code;
    protected final Buffer data;
    private final ArrayList<Site> sites;
    private final ArrayList<DataPatch> dataPatches;

    protected final CodeCacheProvider codeCache;
    protected final TestHotSpotVMConfig config;

    private final Register[] registers;
    private int nextRegister;

    protected int frameSize;
    private int stackAlignment;
    private int curStackSlot;

    private StackSlot deoptRescue;

    public ValueKindFactory<TestValueKind> valueKindFactory = new ValueKindFactory<>() {
        public TestValueKind getValueKind(JavaKind javaKind) {
            return (TestValueKind) TestAssembler.this.getValueKind(javaKind);
        }
    };

    static class TestValueKind extends ValueKind<TestValueKind> {

        TestValueKind(PlatformKind kind) {
            super(kind);
        }

        @Override
        public TestValueKind changeType(PlatformKind kind) {
            return new TestValueKind(kind);
        }
    }

    protected TestAssembler(CodeCacheProvider codeCache, TestHotSpotVMConfig config, int initialFrameSize, int stackAlignment, PlatformKind narrowOopKind, Register... registers) {
        this.narrowOopKind = new TestValueKind(narrowOopKind);

        this.code = new Buffer();
        this.data = new Buffer();
        this.sites = new ArrayList<>();
        this.dataPatches = new ArrayList<>();

        this.codeCache = codeCache;
        this.config = config;

        this.registers = registers;
        this.nextRegister = 0;

        this.frameSize = initialFrameSize;
        this.stackAlignment = stackAlignment;
        this.curStackSlot = initialFrameSize;
    }

    public ValueKind<?> getValueKind(JavaKind kind) {
        return new TestValueKind(codeCache.getTarget().arch.getPlatformKind(kind));
    }

    protected Register newRegister() {
        return registers[nextRegister++];
    }

    protected StackSlot newStackSlot(PlatformKind kind) {
        growFrame(kind.getSizeInBytes());
        return StackSlot.get(new TestValueKind(kind), -curStackSlot, true);
    }

    public int getOffset(StackSlot slot) {
        return slot.getOffset(frameSize);
    }

    protected void growFrame(int sizeInBytes) {
        curStackSlot += sizeInBytes;
        if (curStackSlot > frameSize) {
            int newFrameSize = curStackSlot;
            if (newFrameSize % stackAlignment != 0) {
                newFrameSize += stackAlignment - (newFrameSize % stackAlignment);
            }
            emitGrowStack(newFrameSize - frameSize);
            frameSize = newFrameSize;
        }
    }

    protected void setDeoptRescueSlot(StackSlot deoptRescue) {
        this.deoptRescue = deoptRescue;
    }

    protected void recordCall(InvokeTarget target, int size, boolean direct, DebugInfo debugInfo) {
        sites.add(new Call(target, code.position(), size, direct, debugInfo));
    }

    protected void recordMark(Object id) {
        sites.add(new Mark(code.position(), id));
    }

    protected void recordImplicitException(DebugInfo info) {
        sites.add(new Infopoint(code.position(), info, InfopointReason.IMPLICIT_EXCEPTION));
    }

    protected void recordDataPatchInCode(Reference ref) {
        sites.add(new DataPatch(code.position(), ref));
    }

    protected void recordDataPatchInData(Reference ref) {
        dataPatches.add(new DataPatch(data.position(), ref));
    }

    public DataSectionReference emitDataItem(HotSpotConstant c) {
        DataSectionReference ref = new DataSectionReference();
        ref.setOffset(data.position());

        recordDataPatchInData(new ConstantReference((VMConstant) c));
        if (c.isCompressed()) {
            data.emitInt(0xDEADDEAD);
        } else {
            data.emitLong(0xDEADDEADDEADDEADL);
        }

        return ref;
    }

    public HotSpotCompiledCode finish(HotSpotResolvedJavaMethod method) {
        int id = method.allocateCompileId(0);
        byte[] finishedCode = code.finish();
        Site[] finishedSites = sites.toArray(new Site[0]);
        byte[] finishedData = data.finish();
        DataPatch[] finishedDataPatches = dataPatches.toArray(new DataPatch[0]);
        return new HotSpotCompiledNmethod(method.getName(), finishedCode, finishedCode.length, finishedSites, new Assumption[0], new ResolvedJavaMethod[]{method}, new Comment[0], finishedData, 16,
                        finishedDataPatches, false, frameSize, deoptRescue, method, 0, id, 0L, false);
    }

    protected static class Buffer {

        private ByteBuffer data = ByteBuffer.allocate(32).order(ByteOrder.nativeOrder());

        private void ensureSize(int length) {
            if (length >= data.limit()) {
                byte[] newBuf = Arrays.copyOf(data.array(), length * 4);
                ByteBuffer newData = ByteBuffer.wrap(newBuf);
                newData.order(data.order());
                newData.position(data.position());
                data = newData;
            }
        }

        public int position() {
            return data.position();
        }

        public void emitByte(int b) {
            ensureSize(data.position() + 1);
            data.put((byte) (b & 0xFF));
        }

        public void emitShort(int b) {
            ensureSize(data.position() + 2);
            data.putShort((short) b);
        }

        public void emitInt(int b) {
            ensureSize(data.position() + 4);
            data.putInt(b);
        }

        public void emitLong(long b) {
            ensureSize(data.position() + 8);
            data.putLong(b);
        }

        public void emitFloat(float f) {
            ensureSize(data.position() + 4);
            data.putFloat(f);
        }

        public void emitDouble(double f) {
            ensureSize(data.position() + 8);
            data.putDouble(f);
        }

        public void align(int alignment) {
            int pos = data.position();
            int misaligned = pos % alignment;
            if (misaligned != 0) {
                pos += alignment - misaligned;
                data.position(pos);
            }
        }

        private byte[] finish() {
            return Arrays.copyOf(data.array(), data.position());
        }
    }

    /**
     * Loads a primitive into the Allocatable <code>av</code>. Implementors may only implement
     * primitive types.
     */
    public abstract void emitLoad(AllocatableValue av, Object prim);

    /**
     * Emit a call to a fixed address <code>addr</code>.
     */
    public abstract void emitCall(long addr);

    /**
     * Emit code which is necessary to call a method with {@link CallingConvention} <code>cc</code>
     * and arguments <coe>prim</code>.
     */
    public abstract void emitCallPrologue(CallingConvention cc, Object... prim);

    /**
     * Emit code which is necessary after calling a method with CallingConvention <code>cc</code>.
     */
    public abstract void emitCallEpilogue(CallingConvention cc);

}
