/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.foreign.abi.x64.windows;

import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.internal.foreign.Utils;
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
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;

import static jdk.internal.foreign.PlatformLayouts.*;
import static jdk.internal.foreign.abi.x64.X86_64Architecture.*;

/**
 * For the Windowx x64 C ABI specifically, this class uses the ProgrammableInvoker API, namely CallingSequenceBuilder2
 * to translate a C FunctionDescriptor into a CallingSequence2, which can then be turned into a MethodHandle.
 *
 * This includes taking care of synthetic arguments like pointers to return buffers for 'in-memory' returns.
 */
public class CallArranger {
    private static final int STACK_SLOT_SIZE = 8;

    private static final ABIDescriptor CWindows = X86_64Architecture.abiFor(
        new VMStorage[] { rcx, rdx, r8, r9 },
        new VMStorage[] { xmm0, xmm1, xmm2, xmm3 },
        new VMStorage[] { rax },
        new VMStorage[] { xmm0 },
        0,
        new VMStorage[] { rax, r10, r11 },
        new VMStorage[] { xmm4, xmm5 },
        16,
        32
    );

    // record
    public static class Bindings {
        public final CallingSequence callingSequence;
        public final boolean isInMemoryReturn;

        Bindings(CallingSequence callingSequence, boolean isInMemoryReturn) {
            this.callingSequence = callingSequence;
            this.isInMemoryReturn = isInMemoryReturn;
        }
    }

    public static Bindings getBindings(MethodType mt, FunctionDescriptor cDesc, boolean forUpcall) {
        SharedUtils.checkFunctionTypes(mt, cDesc, Windowsx64Linker.ADDRESS_SIZE);

        class CallingSequenceBuilderHelper {
            final CallingSequenceBuilder csb = new CallingSequenceBuilder(forUpcall);
            final BindingCalculator argCalc =
                forUpcall ? new BoxBindingCalculator(true) : new UnboxBindingCalculator(true);
            final BindingCalculator retCalc =
                forUpcall ? new UnboxBindingCalculator(false) : new BoxBindingCalculator(false);

            void addArgumentBindings(Class<?> carrier, MemoryLayout layout) {
                csb.addArgumentBindings(carrier, layout, argCalc.getBindings(carrier, layout));
            }

            void setReturnBindings(Class<?> carrier, MemoryLayout layout) {
                csb.setReturnBindings(carrier, layout, retCalc.getBindings(carrier, layout));
            }
        }
        var csb = new CallingSequenceBuilderHelper();

        boolean returnInMemory = isInMemoryReturn(cDesc.returnLayout());
        if (returnInMemory) {
            Class<?> carrier = MemoryAddress.class;
            MemoryLayout layout = Win64.C_POINTER;
            csb.addArgumentBindings(carrier, layout);
            if (forUpcall) {
                csb.setReturnBindings(carrier, layout);
            }
        } else if (cDesc.returnLayout().isPresent()) {
            csb.setReturnBindings(mt.returnType(), cDesc.returnLayout().get());
        }

        for (int i = 0; i < mt.parameterCount(); i++) {
            csb.addArgumentBindings(mt.parameterType(i), cDesc.argumentLayouts().get(i));
        }

        csb.csb.setTrivial(SharedUtils.isTrivial(cDesc));

        return new Bindings(csb.csb.build(), returnInMemory);
    }

    public static MethodHandle arrangeDowncall(MethodType mt, FunctionDescriptor cDesc) {
        Bindings bindings = getBindings(mt, cDesc, false);

        MethodHandle handle = new ProgrammableInvoker(CWindows, bindings.callingSequence).getBoundMethodHandle();

        if (bindings.isInMemoryReturn) {
            handle = SharedUtils.adaptDowncallForIMR(handle, cDesc);
        }

        return handle;
    }

    public static UpcallHandler arrangeUpcall(MethodHandle target, MethodType mt, FunctionDescriptor cDesc) {
        Bindings bindings = getBindings(mt, cDesc, true);

        if (bindings.isInMemoryReturn) {
            target = SharedUtils.adaptUpcallForIMR(target, false /* need the return value as well */);
        }

        return ProgrammableUpcallHandler.make(CWindows, target, bindings.callingSequence);
    }

    private static boolean isInMemoryReturn(Optional<MemoryLayout> returnLayout) {
        return returnLayout
                .filter(GroupLayout.class::isInstance)
                .filter(g -> !TypeClass.isRegisterAggregate(g))
                .isPresent();
    }

    static class StorageCalculator {
        private final boolean forArguments;

        private int nRegs = 0;
        private long stackOffset = 0;

        public StorageCalculator(boolean forArguments) {
            this.forArguments = forArguments;
        }

        VMStorage nextStorage(int type, MemoryLayout layout) {
            if (nRegs >= Windowsx64Linker.MAX_REGISTER_ARGUMENTS) {
                assert forArguments : "no stack returns";
                // stack
                long alignment = Math.max(SharedUtils.alignment(layout, true), STACK_SLOT_SIZE);
                stackOffset = Utils.alignUp(stackOffset, alignment);

                VMStorage storage = X86_64Architecture.stackStorage((int) (stackOffset / STACK_SLOT_SIZE));
                stackOffset += STACK_SLOT_SIZE;
                return storage;
            }
            return (forArguments
                    ? CWindows.inputStorage
                    : CWindows.outputStorage)
                 [type][nRegs++];
        }

        public VMStorage extraVarargsStorage() {
            assert forArguments;
            return CWindows.inputStorage[StorageClasses.INTEGER][nRegs - 1];
        }
    }

    private interface BindingCalculator {
        List<Binding> getBindings(Class<?> carrier, MemoryLayout layout);
    }

    static class UnboxBindingCalculator implements BindingCalculator {
        private final StorageCalculator storageCalculator;

        UnboxBindingCalculator(boolean forArguments) {
            this.storageCalculator = new StorageCalculator(forArguments);
        }

        @Override
        public List<Binding> getBindings(Class<?> carrier, MemoryLayout layout) {
            TypeClass argumentClass = TypeClass.typeClassFor(layout);
            Binding.Builder bindings = Binding.builder();
            switch (argumentClass) {
                case STRUCT_REGISTER: {
                    assert carrier == MemorySegment.class;
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.INTEGER, layout);
                    Class<?> type = SharedUtils.primitiveCarrierForSize(layout.byteSize(), false);
                    bindings.bufferLoad(0, type)
                            .vmStore(storage, type);
                    break;
                }
                case STRUCT_REFERENCE: {
                    assert carrier == MemorySegment.class;
                    bindings.copy(layout)
                            .baseAddress()
                            .unboxAddress();
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.INTEGER, layout);
                    bindings.vmStore(storage, long.class);
                    break;
                }
                case POINTER: {
                    bindings.unboxAddress();
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.INTEGER, layout);
                    bindings.vmStore(storage, long.class);
                    break;
                }
                case INTEGER: {
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.INTEGER, layout);
                    bindings.vmStore(storage, carrier);
                    break;
                }
                case FLOAT: {
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.VECTOR, layout);
                    bindings.vmStore(storage, carrier);
                    break;
                }
                case VARARG_FLOAT: {
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.VECTOR, layout);
                    if (!INSTANCE.isStackType(storage.type())) { // need extra for register arg
                        VMStorage extraStorage = storageCalculator.extraVarargsStorage();
                        bindings.dup()
                                .vmStore(extraStorage, carrier);
                    }

                    bindings.vmStore(storage, carrier);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unhandled class " + argumentClass);
            }
            return bindings.build();
        }
    }

    static class BoxBindingCalculator implements BindingCalculator {
        private final StorageCalculator storageCalculator;

        BoxBindingCalculator(boolean forArguments) {
            this.storageCalculator = new StorageCalculator(forArguments);
        }

        @Override
        public List<Binding> getBindings(Class<?> carrier, MemoryLayout layout) {
            TypeClass argumentClass = TypeClass.typeClassFor(layout);
            Binding.Builder bindings = Binding.builder();
            switch (argumentClass) {
                case STRUCT_REGISTER: {
                    assert carrier == MemorySegment.class;
                    bindings.allocate(layout)
                            .dup();
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.INTEGER, layout);
                    Class<?> type = SharedUtils.primitiveCarrierForSize(layout.byteSize(), false);
                    bindings.vmLoad(storage, type)
                            .bufferStore(0, type);
                    break;
                }
                case STRUCT_REFERENCE: {
                    assert carrier == MemorySegment.class;
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.INTEGER, layout);
                    bindings.vmLoad(storage, long.class)
                            .boxAddress()
                            .toSegment(layout);
                    break;
                }
                case POINTER: {
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.INTEGER, layout);
                    bindings.vmLoad(storage, long.class)
                            .boxAddress();
                    break;
                }
                case INTEGER: {
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.INTEGER, layout);
                    bindings.vmLoad(storage, carrier);
                    break;
                }
                case FLOAT: {
                    VMStorage storage = storageCalculator.nextStorage(StorageClasses.VECTOR, layout);
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
