/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.foreign.abi;

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.internal.access.JavaLangInvokeAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.foreign.MemoryAddressImpl;
import sun.security.action.GetPropertyAction;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.identity;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static jdk.internal.foreign.abi.SharedUtils.mergeArguments;
import static sun.security.action.GetBooleanAction.privilegedGetProperty;

/**
 * This class implements upcall invocation from native code through a so called 'universal adapter'. A universal upcall adapter
 * takes an array of storage pointers, which describes the state of the CPU at the time of the upcall. This can be used
 * by the Java code to fetch the upcall arguments and to store the results to the desired location, as per system ABI.
 */
public class ProgrammableUpcallHandler {
    private static final boolean DEBUG =
        privilegedGetProperty("jdk.internal.foreign.ProgrammableUpcallHandler.DEBUG");
    private static final boolean USE_SPEC = Boolean.parseBoolean(
        GetPropertyAction.privilegedGetProperty("jdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC", "true"));
    private static final boolean USE_INTRINSICS = Boolean.parseBoolean(
        GetPropertyAction.privilegedGetProperty("jdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS", "true"));

    private static final JavaLangInvokeAccess JLI = SharedSecrets.getJavaLangInvokeAccess();

    private static final VarHandle VH_LONG = MemoryLayouts.JAVA_LONG.varHandle(long.class);

    private static final MethodHandle MH_invokeMoves;
    private static final MethodHandle MH_invokeInterpBindings;

    static {
        try {
            MethodHandles.Lookup lookup = lookup();
            MH_invokeMoves = lookup.findStatic(ProgrammableUpcallHandler.class, "invokeMoves",
                    methodType(void.class, MemoryAddress.class, MethodHandle.class,
                               Binding.VMLoad[].class, Binding.VMStore[].class, ABIDescriptor.class, BufferLayout.class));
            MH_invokeInterpBindings = lookup.findStatic(ProgrammableUpcallHandler.class, "invokeInterpBindings",
                    methodType(Object.class, Object[].class, MethodHandle.class, Map.class, Map.class,
                            CallingSequence.class, long.class));
        } catch (ReflectiveOperationException e) {
            throw new InternalError(e);
        }
    }

    public static UpcallHandler make(ABIDescriptor abi, MethodHandle target, CallingSequence callingSequence) {
        Binding.VMLoad[] argMoves = argMoveBindings(callingSequence);
        Binding.VMStore[] retMoves = retMoveBindings(callingSequence);

        boolean isSimple = !(retMoves.length > 1);

        Class<?> llReturn = !isSimple
            ? Object[].class
            : retMoves.length == 1
                ? retMoves[0].type()
                : void.class;
        Class<?>[] llParams = Arrays.stream(argMoves).map(Binding.Move::type).toArray(Class<?>[]::new);
        MethodType llType = MethodType.methodType(llReturn, llParams);

        MethodHandle doBindings;
        long bufferCopySize = SharedUtils.bufferCopySize(callingSequence);
        if (USE_SPEC && isSimple) {
            doBindings = specializedBindingHandle(target, callingSequence, llReturn, bufferCopySize);
            assert doBindings.type() == llType;
        } else {
            Map<VMStorage, Integer> argIndices = SharedUtils.indexMap(argMoves);
            Map<VMStorage, Integer> retIndices = SharedUtils.indexMap(retMoves);
            target = target.asSpreader(Object[].class, callingSequence.methodType().parameterCount());
            doBindings = insertArguments(MH_invokeInterpBindings, 1, target, argIndices, retIndices, callingSequence,
                    bufferCopySize);
            doBindings = doBindings.asCollector(Object[].class, llType.parameterCount());
            doBindings = doBindings.asType(llType);
        }

        long entryPoint;
        boolean usesStackArgs = argMoveBindingsStream(callingSequence)
                .map(Binding.VMLoad::storage)
                .anyMatch(s -> abi.arch.isStackType(s.type()));
        if (USE_INTRINSICS && isSimple && !usesStackArgs && supportsOptimizedUpcalls()) {
            checkPrimitive(doBindings.type());
            JLI.ensureCustomized(doBindings);
            VMStorage[] args = Arrays.stream(argMoves).map(Binding.Move::storage).toArray(VMStorage[]::new);
            VMStorage[] rets = Arrays.stream(retMoves).map(Binding.Move::storage).toArray(VMStorage[]::new);
            CallRegs conv = new CallRegs(args, rets);
            entryPoint = allocateOptimizedUpcallStub(doBindings, abi, conv);
        } else {
            BufferLayout layout = BufferLayout.of(abi);
            MethodHandle doBindingsErased = doBindings.asSpreader(Object[].class, doBindings.type().parameterCount());
            MethodHandle invokeMoves = insertArguments(MH_invokeMoves, 1, doBindingsErased, argMoves, retMoves, abi, layout);
            entryPoint = allocateUpcallStub(invokeMoves, abi, layout);
        }
        return () -> entryPoint;
    }

    private static void checkPrimitive(MethodType type) {
        if (!type.returnType().isPrimitive()
                || type.parameterList().stream().anyMatch(p -> !p.isPrimitive()))
            throw new IllegalArgumentException("MethodHandle type must be primitive: " + type);
    }

    private static Stream<Binding.VMLoad> argMoveBindingsStream(CallingSequence callingSequence) {
        return callingSequence.argumentBindings()
                .filter(Binding.VMLoad.class::isInstance)
                .map(Binding.VMLoad.class::cast);
    }

    private static Binding.VMLoad[] argMoveBindings(CallingSequence callingSequence) {
        return argMoveBindingsStream(callingSequence)
                .toArray(Binding.VMLoad[]::new);
    }

    private static Binding.VMStore[] retMoveBindings(CallingSequence callingSequence) {
        return callingSequence.returnBindings().stream()
                .filter(Binding.VMStore.class::isInstance)
                .map(Binding.VMStore.class::cast)
                .toArray(Binding.VMStore[]::new);
    }

    private static MethodHandle specializedBindingHandle(MethodHandle target, CallingSequence callingSequence,
                                                         Class<?> llReturn, long bufferCopySize) {
        MethodType highLevelType = callingSequence.methodType();

        MethodHandle specializedHandle = target; // initial

        int argAllocatorPos = 0;
        int argInsertPos = 1;
        specializedHandle = dropArguments(specializedHandle, argAllocatorPos, Binding.Context.class);
        for (int i = 0; i < highLevelType.parameterCount(); i++) {
            MethodHandle filter = identity(highLevelType.parameterType(i));
            int filterAllocatorPos = 0;
            int filterInsertPos = 1; // +1 for allocator
            filter = dropArguments(filter, filterAllocatorPos, Binding.Context.class);

            List<Binding> bindings = callingSequence.argumentBindings(i);
            for (int j = bindings.size() - 1; j >= 0; j--) {
                Binding binding = bindings.get(j);
                filter = binding.specialize(filter, filterInsertPos, filterAllocatorPos);
            }
            specializedHandle = MethodHandles.collectArguments(specializedHandle, argInsertPos, filter);
            specializedHandle = mergeArguments(specializedHandle, argAllocatorPos, argInsertPos + filterAllocatorPos);
            argInsertPos += filter.type().parameterCount() - 1; // -1 for allocator
        }

        if (llReturn != void.class) {
            int retAllocatorPos = -1; // assumed not needed
            int retInsertPos = 0;
            MethodHandle filter = identity(llReturn);
            List<Binding> bindings = callingSequence.returnBindings();
            for (int j = bindings.size() - 1; j >= 0; j--) {
                Binding binding = bindings.get(j);
                filter = binding.specialize(filter, retInsertPos, retAllocatorPos);
            }
            specializedHandle = filterReturnValue(specializedHandle, filter);
        }

        specializedHandle = SharedUtils.wrapWithAllocator(specializedHandle, argAllocatorPos, bufferCopySize, true);

        return specializedHandle;
    }

    public static void invoke(MethodHandle mh, long address) throws Throwable {
        mh.invokeExact(MemoryAddress.ofLong(address));
    }

    private static void invokeMoves(MemoryAddress buffer, MethodHandle leaf,
                                    Binding.VMLoad[] argBindings, Binding.VMStore[] returnBindings,
                                    ABIDescriptor abi, BufferLayout layout) throws Throwable {
        MemorySegment bufferBase = MemoryAddressImpl.ofLongUnchecked(buffer.toRawLongValue(), layout.size);

        if (DEBUG) {
            System.err.println("Buffer state before:");
            layout.dump(abi.arch, bufferBase, System.err);
        }

        MemorySegment stackArgsBase = MemoryAddressImpl.ofLongUnchecked((long)VH_LONG.get(bufferBase.asSlice(layout.stack_args)));
        Object[] moves = new Object[argBindings.length];
        for (int i = 0; i < moves.length; i++) {
            Binding.VMLoad binding = argBindings[i];
            VMStorage storage = binding.storage();
            MemorySegment ptr = abi.arch.isStackType(storage.type())
                ? stackArgsBase.asSlice(storage.index() * abi.arch.typeSize(abi.arch.stackType()))
                : bufferBase.asSlice(layout.argOffset(storage));
            moves[i] = SharedUtils.read(ptr, binding.type());
        }

        // invokeInterpBindings, and then actual target
        Object o = leaf.invoke(moves);

        if (o == null) {
            // nop
        } else if (o instanceof Object[] returns) {
            for (int i = 0; i < returnBindings.length; i++) {
                Binding.VMStore binding = returnBindings[i];
                VMStorage storage = binding.storage();
                MemorySegment ptr = bufferBase.asSlice(layout.retOffset(storage));
                SharedUtils.writeOverSized(ptr, binding.type(), returns[i]);
            }
        } else { // single Object
            Binding.VMStore binding = returnBindings[0];
            VMStorage storage = binding.storage();
            MemorySegment ptr = bufferBase.asSlice(layout.retOffset(storage));
            SharedUtils.writeOverSized(ptr, binding.type(), o);
        }

        if (DEBUG) {
            System.err.println("Buffer state after:");
            layout.dump(abi.arch, bufferBase, System.err);
        }
    }

    private static Object invokeInterpBindings(Object[] moves, MethodHandle leaf,
                                               Map<VMStorage, Integer> argIndexMap,
                                               Map<VMStorage, Integer> retIndexMap,
                                               CallingSequence callingSequence,
                                               long bufferCopySize) throws Throwable {
        Binding.Context allocator = bufferCopySize != 0
                ? Binding.Context.ofBoundedAllocator(bufferCopySize)
                : Binding.Context.ofScope();
        try (allocator) {
            /// Invoke interpreter, got array of high-level arguments back
            Object[] args = new Object[callingSequence.methodType().parameterCount()];
            for (int i = 0; i < args.length; i++) {
                args[i] = BindingInterpreter.box(callingSequence.argumentBindings(i),
                        (storage, type) -> moves[argIndexMap.get(storage)], allocator);
            }

            if (DEBUG) {
                System.err.println("Java arguments:");
                System.err.println(Arrays.toString(args).indent(2));
            }

            // invoke our target
            Object o = leaf.invoke(args);

            if (DEBUG) {
                System.err.println("Java return:");
                System.err.println(Objects.toString(o).indent(2));
            }

            Object[] returnMoves = new Object[retIndexMap.size()];
            if (leaf.type().returnType() != void.class) {
                BindingInterpreter.unbox(o, callingSequence.returnBindings(),
                        (storage, type, value) -> returnMoves[retIndexMap.get(storage)] = value, null);
            }

            if (returnMoves.length == 0) {
                return null;
            } else if (returnMoves.length == 1) {
                return returnMoves[0];
            } else {
                return returnMoves;
            }
        } catch(Throwable t) {
            SharedUtils.handleUncaughtException(t);
            return null;
        }
    }

    // used for transporting data into native code
    private static record CallRegs(VMStorage[] argRegs, VMStorage[] retRegs) {}

    static native long allocateOptimizedUpcallStub(MethodHandle mh, ABIDescriptor abi, CallRegs conv);
    static native long allocateUpcallStub(MethodHandle mh, ABIDescriptor abi, BufferLayout layout);
    static native boolean supportsOptimizedUpcalls();

    private static native void registerNatives();
    static {
        registerNatives();
    }
}
