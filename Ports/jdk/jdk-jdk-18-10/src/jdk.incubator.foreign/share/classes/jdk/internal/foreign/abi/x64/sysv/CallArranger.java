/*
 *  Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.internal.foreign.abi.CallingSequenceBuilder;
import jdk.internal.foreign.abi.UpcallHandler;
import jdk.internal.foreign.abi.ABIDescriptor;
import jdk.internal.foreign.abi.Binding;
import jdk.internal.foreign.abi.CallingSequence;
import jdk.internal.foreign.abi.ProgrammableInvoker;
import jdk.internal.foreign.abi.ProgrammableUpcallHandler;
import jdk.internal.foreign.abi.VMStorage;
import jdk.internal.foreign.abi.x64.X86_64Architecture;
import jdk.internal.foreign.abi.SharedUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;

import static jdk.internal.foreign.PlatformLayouts.*;
import static jdk.internal.foreign.abi.Binding.*;
import static jdk.internal.foreign.abi.x64.X86_64Architecture.*;
import static jdk.internal.foreign.abi.x64.sysv.SysVx64Linker.MAX_INTEGER_ARGUMENT_REGISTERS;
import static jdk.internal.foreign.abi.x64.sysv.SysVx64Linker.MAX_VECTOR_ARGUMENT_REGISTERS;

/**
 * For the SysV x64 C ABI specifically, this class uses the ProgrammableInvoker API, namely CallingSequenceBuilder2
 * to translate a C FunctionDescriptor into a CallingSequence, which can then be turned into a MethodHandle.
 *
 * This includes taking care of synthetic arguments like pointers to return buffers for 'in-memory' returns.
 */
public class CallArranger {
    private static final ABIDescriptor CSysV = X86_64Architecture.abiFor(
        new VMStorage[] { rdi, rsi, rdx, rcx, r8, r9, rax },
        new VMStorage[] { xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7 },
        new VMStorage[] { rax, rdx },
        new VMStorage[] { xmm0, xmm1 },
        2,
        new VMStorage[] { r10, r11 },
        new VMStorage[] { xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15 },
        16,
        0 //no shadow space
    );

    // record
    public static class Bindings {
        public final CallingSequence callingSequence;
        public final boolean isInMemoryReturn;
        public final int nVectorArgs;

        Bindings(CallingSequence callingSequence, boolean isInMemoryReturn, int nVectorArgs) {
            this.callingSequence = callingSequence;
            this.isInMemoryReturn = isInMemoryReturn;
            this.nVectorArgs = nVectorArgs;
        }
    }

    public static Bindings getBindings(MethodType mt, FunctionDescriptor cDesc, boolean forUpcall) {
        SharedUtils.checkFunctionTypes(mt, cDesc, SysVx64Linker.ADDRESS_SIZE);

        CallingSequenceBuilder csb = new CallingSequenceBuilder(forUpcall);

        BindingCalculator argCalc = forUpcall ? new BoxBindingCalculator(true) : new UnboxBindingCalculator(true);
        BindingCalculator retCalc = forUpcall ? new UnboxBindingCalculator(false) : new BoxBindingCalculator(false);

        boolean returnInMemory = isInMemoryReturn(cDesc.returnLayout());
        if (returnInMemory) {
            Class<?> carrier = MemoryAddress.class;
            MemoryLayout layout = SysV.C_POINTER;
            csb.addArgumentBindings(carrier, layout, argCalc.getBindings(carrier, layout));
        } else if (cDesc.returnLayout().isPresent()) {
            Class<?> carrier = mt.returnType();
            MemoryLayout layout = cDesc.returnLayout().get();
            csb.setReturnBindings(carrier, layout, retCalc.getBindings(carrier, layout));
        }

        for (int i = 0; i < mt.parameterCount(); i++) {
            Class<?> carrier = mt.parameterType(i);
            MemoryLayout layout = cDesc.argumentLayouts().get(i);
            csb.addArgumentBindings(carrier, layout, argCalc.getBindings(carrier, layout));
        }

        if (!forUpcall) {
            //add extra binding for number of used vector registers (used for variadic calls)
            csb.addArgumentBindings(long.class, SysV.C_LONG,
                    List.of(vmStore(rax, long.class)));
        }

        csb.setTrivial(SharedUtils.isTrivial(cDesc));

        return new Bindings(csb.build(), returnInMemory, argCalc.storageCalculator.nVectorReg);
    }

    public static MethodHandle arrangeDowncall(MethodType mt, FunctionDescriptor cDesc) {
        Bindings bindings = getBindings(mt, cDesc, false);

        MethodHandle handle = new ProgrammableInvoker(CSysV, bindings.callingSequence).getBoundMethodHandle();
        handle = MethodHandles.insertArguments(handle, handle.type().parameterCount() - 1, bindings.nVectorArgs);

        if (bindings.isInMemoryReturn) {
            handle = SharedUtils.adaptDowncallForIMR(handle, cDesc);
        }

        return handle;
    }

    public static UpcallHandler arrangeUpcall(MethodHandle target, MethodType mt, FunctionDescriptor cDesc) {
        Bindings bindings = getBindings(mt, cDesc, true);

        if (bindings.isInMemoryReturn) {
            target = SharedUtils.adaptUpcallForIMR(target, true /* drop return, since we don't have bindings for it */);
        }

        return ProgrammableUpcallHandler.make(CSysV, target, bindings.callingSequence);
    }

    private static boolean isInMemoryReturn(Optional<MemoryLayout> returnLayout) {
        return returnLayout
                .filter(GroupLayout.class::isInstance)
                .filter(g -> TypeClass.classifyLayout(g).inMemory())
                .isPresent();
    }

    static class StorageCalculator {
        private final boolean forArguments;

        private int nVectorReg = 0;
        private int nIntegerReg = 0;
        private long stackOffset = 0;

        public StorageCalculator(boolean forArguments) {
            this.forArguments = forArguments;
        }

        private int maxRegisterArguments(int type) {
            return type == StorageClasses.INTEGER ?
                    MAX_INTEGER_ARGUMENT_REGISTERS :
                    SysVx64Linker.MAX_VECTOR_ARGUMENT_REGISTERS;
        }

        VMStorage stackAlloc() {
            assert forArguments : "no stack returns";
            VMStorage storage = X86_64Architecture.stackStorage((int)stackOffset);
            stackOffset++;
            return storage;
        }

        VMStorage nextStorage(int type) {
            int registerCount = registerCount(type);
            if (registerCount < maxRegisterArguments(type)) {
                VMStorage[] source =
                    (forArguments ? CSysV.inputStorage : CSysV.outputStorage)[type];
                incrementRegisterCount(type);
                return source[registerCount];
            } else {
                return stackAlloc();
            }
        }

        VMStorage[] structStorages(TypeClass typeClass) {
            if (typeClass.inMemory()) {
                return typeClass.classes.stream().map(c -> stackAlloc()).toArray(VMStorage[]::new);
            }
            long nIntegerReg = typeClass.nIntegerRegs();

            if (this.nIntegerReg + nIntegerReg > MAX_INTEGER_ARGUMENT_REGISTERS) {
                //not enough registers - pass on stack
                return typeClass.classes.stream().map(c -> stackAlloc()).toArray(VMStorage[]::new);
            }

            long nVectorReg = typeClass.nVectorRegs();

            if (this.nVectorReg + nVectorReg > MAX_VECTOR_ARGUMENT_REGISTERS) {
                //not enough registers - pass on stack
                return typeClass.classes.stream().map(c -> stackAlloc()).toArray(VMStorage[]::new);
            }

            //ok, let's pass pass on registers
            VMStorage[] storage = new VMStorage[(int)(nIntegerReg + nVectorReg)];
            for (int i = 0 ; i < typeClass.classes.size() ; i++) {
                boolean sse = typeClass.classes.get(i) == ArgumentClassImpl.SSE;
                storage[i] = nextStorage(sse ? StorageClasses.VECTOR : StorageClasses.INTEGER);
            }
            return storage;
        }

        int registerCount(int type) {
            switch (type) {
                case StorageClasses.INTEGER:
                    return nIntegerReg;
                case StorageClasses.VECTOR:
                    return nVectorReg;
                default:
                    throw new IllegalStateException();
            }
        }

        void incrementRegisterCount(int type) {
            switch (type) {
                case StorageClasses.INTEGER:
                    nIntegerReg++;
                    break;
                case StorageClasses.VECTOR:
                    nVectorReg++;
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    static abstract class BindingCalculator {
        protected final StorageCalculator storageCalculator;

        protected BindingCalculator(boolean forArguments) {
            this.storageCalculator = new StorageCalculator(forArguments);
        }

        abstract List<Binding> getBindings(Class<?> carrier, MemoryLayout layout);
    }

    static class UnboxBindingCalculator extends BindingCalculator {

        UnboxBindingCalculator(boolean forArguments) {
            super(forArguments);
        }

        @Override
        List<Binding> getBindings(Class<?> carrier, MemoryLayout layout) {
            TypeClass argumentClass = TypeClass.classifyLayout(layout);
            Binding.Builder bindings = Binding.builder();
            switch (argumentClass.kind()) {
                case STRUCT: {
                    assert carrier == MemorySegment.class;
                    VMStorage[] regs = storageCalculator.structStorages(argumentClass);
                    int regIndex = 0;
                    long offset = 0;
                    while (offset < layout.byteSize()) {
                        final long copy = Math.min(layout.byteSize() - offset, 8);
                        VMStorage storage = regs[regIndex++];
                        if (offset + copy < layout.byteSize()) {
                            bindings.dup();
                        }
                        boolean useFloat = storage.type() == StorageClasses.VECTOR;
                        Class<?> type = SharedUtils.primitiveCarrierForSize(copy, useFloat);
                        bindings.bufferLoad(offset, type)
                                .vmStore(storage, type);
                        offset += copy;
                    }
                    break;
                }
                case POINTER: {
                    bindings.unboxAddress();
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.INTEGER);
                    bindings.vmStore(storage, long.class);
                    break;
                }
                case INTEGER: {
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.INTEGER);
                    bindings.vmStore(storage, carrier);
                    break;
                }
                case FLOAT: {
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.VECTOR);
                    bindings.vmStore(storage, carrier);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unhandled class " + argumentClass);
            }
            return bindings.build();
        }
    }

    static class BoxBindingCalculator extends BindingCalculator {

        BoxBindingCalculator(boolean forArguments) {
            super(forArguments);
        }

        @Override
        List<Binding> getBindings(Class<?> carrier, MemoryLayout layout) {
            TypeClass argumentClass = TypeClass.classifyLayout(layout);
            Binding.Builder bindings = Binding.builder();
            switch (argumentClass.kind()) {
                case STRUCT: {
                    assert carrier == MemorySegment.class;
                    bindings.allocate(layout);
                    VMStorage[] regs = storageCalculator.structStorages(argumentClass);
                    int regIndex = 0;
                    long offset = 0;
                    while (offset < layout.byteSize()) {
                        final long copy = Math.min(layout.byteSize() - offset, 8);
                        VMStorage storage = regs[regIndex++];
                        bindings.dup();
                        boolean useFloat = storage.type() == StorageClasses.VECTOR;
                        Class<?> type = SharedUtils.primitiveCarrierForSize(copy, useFloat);
                        bindings.vmLoad(storage, type)
                                .bufferStore(offset, type);
                        offset += copy;
                    }
                    break;
                }
                case POINTER: {
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.INTEGER);
                    bindings.vmLoad(storage, long.class)
                            .boxAddress();
                    break;
                }
                case INTEGER: {
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.INTEGER);
                    bindings.vmLoad(storage, carrier);
                    break;
                }
                case FLOAT: {
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.VECTOR);
                    bindings.vmLoad(storage, carrier);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unhandled class " + argumentClass);
            }
            return bindings.build();
        }
    }

}
