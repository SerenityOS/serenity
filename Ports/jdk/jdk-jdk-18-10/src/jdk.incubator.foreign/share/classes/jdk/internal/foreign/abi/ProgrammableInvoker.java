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
package jdk.internal.foreign.abi;

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.internal.access.JavaLangInvokeAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.invoke.NativeEntryPoint;
import jdk.internal.invoke.VMStorageProxy;
import sun.security.action.GetPropertyAction;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.lang.invoke.MethodHandles.collectArguments;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.identity;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodType.methodType;
import static sun.security.action.GetBooleanAction.privilegedGetProperty;

/**
 * This class implements native call invocation through a so called 'universal adapter'. A universal adapter takes
 * an array of longs together with a call 'recipe', which is used to move the arguments in the right places as
 * expected by the system ABI.
 */
public class ProgrammableInvoker {
    private static final boolean DEBUG =
        privilegedGetProperty("jdk.internal.foreign.ProgrammableInvoker.DEBUG");
    private static final boolean USE_SPEC = Boolean.parseBoolean(
        GetPropertyAction.privilegedGetProperty("jdk.internal.foreign.ProgrammableInvoker.USE_SPEC", "true"));
    private static final boolean USE_INTRINSICS = Boolean.parseBoolean(
        GetPropertyAction.privilegedGetProperty("jdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS", "true"));

    private static final JavaLangInvokeAccess JLIA = SharedSecrets.getJavaLangInvokeAccess();

    private static final VarHandle VH_LONG = MemoryLayouts.JAVA_LONG.varHandle(long.class);

    private static final MethodHandle MH_INVOKE_MOVES;
    private static final MethodHandle MH_INVOKE_INTERP_BINDINGS;
    private static final MethodHandle MH_ADDR_TO_LONG;
    private static final MethodHandle MH_WRAP_ALLOCATOR;

    private static final Map<ABIDescriptor, Long> adapterStubs = new ConcurrentHashMap<>();

    private static final MethodHandle EMPTY_OBJECT_ARRAY_HANDLE = MethodHandles.constant(Object[].class, new Object[0]);

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MH_INVOKE_MOVES = lookup.findVirtual(ProgrammableInvoker.class, "invokeMoves",
                    methodType(Object.class, long.class, Object[].class, Binding.VMStore[].class, Binding.VMLoad[].class));
            MH_INVOKE_INTERP_BINDINGS = lookup.findVirtual(ProgrammableInvoker.class, "invokeInterpBindings",
                    methodType(Object.class, Addressable.class, SegmentAllocator.class, Object[].class, MethodHandle.class, Map.class, Map.class));
            MH_WRAP_ALLOCATOR = lookup.findStatic(Binding.Context.class, "ofAllocator",
                    methodType(Binding.Context.class, SegmentAllocator.class));
            MH_ADDR_TO_LONG = lookup.findStatic(ProgrammableInvoker.class, "unboxTargetAddress", methodType(long.class, Addressable.class));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private final ABIDescriptor abi;
    private final BufferLayout layout;
    private final long stackArgsBytes;

    private final CallingSequence callingSequence;

    private final long stubAddress;

    private final long bufferCopySize;

    public ProgrammableInvoker(ABIDescriptor abi, CallingSequence callingSequence) {
        this.abi = abi;
        this.layout = BufferLayout.of(abi);
        this.stubAddress = adapterStubs.computeIfAbsent(abi, key -> generateAdapter(key, layout));

        this.callingSequence = callingSequence;

        this.stackArgsBytes = argMoveBindingsStream(callingSequence)
                .map(Binding.VMStore::storage)
                .filter(s -> abi.arch.isStackType(s.type()))
                .count()
                * abi.arch.typeSize(abi.arch.stackType());

        this.bufferCopySize = SharedUtils.bufferCopySize(callingSequence);
    }

    public MethodHandle getBoundMethodHandle() {
        Binding.VMStore[] argMoves = argMoveBindingsStream(callingSequence).toArray(Binding.VMStore[]::new);
        Class<?>[] argMoveTypes = Arrays.stream(argMoves).map(Binding.VMStore::type).toArray(Class<?>[]::new);

        Binding.VMLoad[] retMoves = retMoveBindings(callingSequence);
        Class<?> returnType = retMoves.length == 0
                ? void.class
                : retMoves.length == 1
                    ? retMoves[0].type()
                    : Object[].class;

        MethodType leafType = methodType(returnType, argMoveTypes);
        MethodType leafTypeWithAddress = leafType.insertParameterTypes(0, long.class);

        MethodHandle handle = insertArguments(MH_INVOKE_MOVES.bindTo(this), 2, argMoves, retMoves);
        MethodHandle collector = makeCollectorHandle(leafType);
        handle = collectArguments(handle, 1, collector);
        handle = handle.asType(leafTypeWithAddress);

        boolean isSimple = !(retMoves.length > 1);
        boolean usesStackArgs = stackArgsBytes != 0;
        if (USE_INTRINSICS && isSimple && !usesStackArgs) {
            NativeEntryPoint nep = NativeEntryPoint.make(
                "native_call",
                abi,
                toStorageArray(argMoves),
                toStorageArray(retMoves),
                !callingSequence.isTrivial(),
                leafTypeWithAddress
            );

            handle = JLIA.nativeMethodHandle(nep, handle);
        }
        handle = filterArguments(handle, 0, MH_ADDR_TO_LONG);

        if (USE_SPEC && isSimple) {
            handle = specialize(handle);
         } else {
            Map<VMStorage, Integer> argIndexMap = SharedUtils.indexMap(argMoves);
            Map<VMStorage, Integer> retIndexMap = SharedUtils.indexMap(retMoves);

            handle = insertArguments(MH_INVOKE_INTERP_BINDINGS.bindTo(this), 3, handle, argIndexMap, retIndexMap);
            MethodHandle collectorInterp = makeCollectorHandle(callingSequence.methodType());
            handle = collectArguments(handle, 2, collectorInterp);
            handle = handle.asType(handle.type().changeReturnType(callingSequence.methodType().returnType()));
         }

        return handle;
    }

    private static long unboxTargetAddress(Addressable addr) {
        MemoryAddress ma = SharedUtils.checkSymbol(addr);
        return ma.toRawLongValue();
    }

    // Funnel from type to Object[]
    private static MethodHandle makeCollectorHandle(MethodType type) {
        return type.parameterCount() == 0
            ? EMPTY_OBJECT_ARRAY_HANDLE
            : identity(Object[].class)
                .asCollector(Object[].class, type.parameterCount())
                .asType(type.changeReturnType(Object[].class));
    }

    private Stream<Binding.VMStore> argMoveBindingsStream(CallingSequence callingSequence) {
        return callingSequence.argumentBindings()
                .filter(Binding.VMStore.class::isInstance)
                .map(Binding.VMStore.class::cast);
    }

    private Binding.VMLoad[] retMoveBindings(CallingSequence callingSequence) {
        return callingSequence.returnBindings().stream()
                .filter(Binding.VMLoad.class::isInstance)
                .map(Binding.VMLoad.class::cast)
                .toArray(Binding.VMLoad[]::new);
    }


    private VMStorageProxy[] toStorageArray(Binding.Move[] moves) {
        return Arrays.stream(moves).map(Binding.Move::storage).toArray(VMStorage[]::new);
    }

    private MethodHandle specialize(MethodHandle leafHandle) {
        MethodType highLevelType = callingSequence.methodType();

        int argInsertPos = 1;
        int argContextPos = 1;

        MethodHandle specializedHandle = dropArguments(leafHandle, argContextPos, Binding.Context.class);

        for (int i = 0; i < highLevelType.parameterCount(); i++) {
            List<Binding> bindings = callingSequence.argumentBindings(i);
            argInsertPos += bindings.stream().filter(Binding.VMStore.class::isInstance).count() + 1;
            // We interpret the bindings in reverse since we have to construct a MethodHandle from the bottom up
            for (int j = bindings.size() - 1; j >= 0; j--) {
                Binding binding = bindings.get(j);
                if (binding.tag() == Binding.Tag.VM_STORE) {
                    argInsertPos--;
                } else {
                    specializedHandle = binding.specialize(specializedHandle, argInsertPos, argContextPos);
                }
            }
        }

        if (highLevelType.returnType() != void.class) {
            MethodHandle returnFilter = identity(highLevelType.returnType());
            int retContextPos = 0;
            int retInsertPos = 1;
            returnFilter = dropArguments(returnFilter, retContextPos, Binding.Context.class);
            List<Binding> bindings = callingSequence.returnBindings();
            for (int j = bindings.size() - 1; j >= 0; j--) {
                Binding binding = bindings.get(j);
                returnFilter = binding.specialize(returnFilter, retInsertPos, retContextPos);
            }
            returnFilter = MethodHandles.filterArguments(returnFilter, retContextPos, MH_WRAP_ALLOCATOR);
            // (SegmentAllocator, Addressable, Context, ...) -> ...
            specializedHandle = MethodHandles.collectArguments(returnFilter, retInsertPos, specializedHandle);
            // (Addressable, SegmentAllocator, Context, ...) -> ...
            specializedHandle = SharedUtils.swapArguments(specializedHandle, 0, 1); // normalize parameter order
        } else {
            specializedHandle = MethodHandles.dropArguments(specializedHandle, 1, SegmentAllocator.class);
        }

        // now bind the internal context parameter

        argContextPos++; // skip over the return SegmentAllocator (inserted by the above code)
        specializedHandle = SharedUtils.wrapWithAllocator(specializedHandle, argContextPos, bufferCopySize, false);
        return specializedHandle;
    }

    /**
     * Does a native invocation by moving primitive values from the arg array into an intermediate buffer
     * and calling the assembly stub that forwards arguments from the buffer to the target function
     *
     * @param args an array of primitive values to be copied in to the buffer
     * @param argBindings Binding.Move values describing how arguments should be copied
     * @param returnBindings Binding.Move values describing how return values should be copied
     * @return null, a single primitive value, or an Object[] of primitive values
     */
    Object invokeMoves(long addr, Object[] args, Binding.VMStore[] argBindings, Binding.VMLoad[] returnBindings) {
        MemorySegment stackArgsSeg = null;
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment argBuffer = MemorySegment.allocateNative(layout.size, 64, scope);
            if (stackArgsBytes > 0) {
                stackArgsSeg = MemorySegment.allocateNative(stackArgsBytes, 8, scope);
            }

            VH_LONG.set(argBuffer.asSlice(layout.arguments_next_pc), addr);
            VH_LONG.set(argBuffer.asSlice(layout.stack_args_bytes), stackArgsBytes);
            VH_LONG.set(argBuffer.asSlice(layout.stack_args), stackArgsSeg == null ? 0L : stackArgsSeg.address().toRawLongValue());

            for (int i = 0; i < argBindings.length; i++) {
                Binding.VMStore binding = argBindings[i];
                VMStorage storage = binding.storage();
                MemorySegment ptr = abi.arch.isStackType(storage.type())
                    ? stackArgsSeg.asSlice(storage.index() * abi.arch.typeSize(abi.arch.stackType()))
                    : argBuffer.asSlice(layout.argOffset(storage));
                SharedUtils.writeOverSized(ptr, binding.type(), args[i]);
            }

            if (DEBUG) {
                System.err.println("Buffer state before:");
                layout.dump(abi.arch, argBuffer, System.err);
            }

            invokeNative(stubAddress, argBuffer.address().toRawLongValue());

            if (DEBUG) {
                System.err.println("Buffer state after:");
                layout.dump(abi.arch, argBuffer, System.err);
            }

            if (returnBindings.length == 0) {
                return null;
            } else if (returnBindings.length == 1) {
                Binding.VMLoad move = returnBindings[0];
                VMStorage storage = move.storage();
                return SharedUtils.read(argBuffer.asSlice(layout.retOffset(storage)), move.type());
            } else { // length > 1
                Object[] returns = new Object[returnBindings.length];
                for (int i = 0; i < returnBindings.length; i++) {
                    Binding.VMLoad move = returnBindings[i];
                    VMStorage storage = move.storage();
                    returns[i] = SharedUtils.read(argBuffer.asSlice(layout.retOffset(storage)), move.type());
                }
                return returns;
            }
        }
    }

    Object invokeInterpBindings(Addressable address, SegmentAllocator allocator, Object[] args, MethodHandle leaf,
                                Map<VMStorage, Integer> argIndexMap,
                                Map<VMStorage, Integer> retIndexMap) throws Throwable {
        Binding.Context unboxContext = bufferCopySize != 0
                ? Binding.Context.ofBoundedAllocator(bufferCopySize)
                : Binding.Context.DUMMY;
        try (unboxContext) {
            // do argument processing, get Object[] as result
            Object[] leafArgs = new Object[leaf.type().parameterCount()];
            leafArgs[0] = address; // addr
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                BindingInterpreter.unbox(arg, callingSequence.argumentBindings(i),
                        (storage, type, value) -> {
                            leafArgs[argIndexMap.get(storage) + 1] = value; // +1 to skip addr
                        }, unboxContext);
            }

            // call leaf
            Object o = leaf.invokeWithArguments(leafArgs);
            // make sure arguments are reachable during the call
            // technically we only need to do all Addressable parameters here
            Reference.reachabilityFence(address);
            Reference.reachabilityFence(args);

            // return value processing
            if (o == null) {
                return null;
            } else if (o instanceof Object[]) {
                Object[] oArr = (Object[]) o;
                return BindingInterpreter.box(callingSequence.returnBindings(),
                        (storage, type) -> oArr[retIndexMap.get(storage)], Binding.Context.ofAllocator(allocator));
            } else {
                return BindingInterpreter.box(callingSequence.returnBindings(), (storage, type) -> o,
                        Binding.Context.ofAllocator(allocator));
            }
        }
    }

    //natives

    static native void invokeNative(long adapterStub, long buff);
    static native long generateAdapter(ABIDescriptor abi, BufferLayout layout);

    private static native void registerNatives();
    static {
        registerNatives();
    }
}

