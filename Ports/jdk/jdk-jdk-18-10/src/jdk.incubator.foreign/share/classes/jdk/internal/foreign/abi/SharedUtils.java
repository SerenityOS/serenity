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
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.incubator.foreign.SequenceLayout;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.ValueLayout;
import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.foreign.CABI;
import jdk.internal.foreign.MemoryAddressImpl;
import jdk.internal.foreign.Utils;
import jdk.internal.foreign.abi.aarch64.linux.LinuxAArch64Linker;
import jdk.internal.foreign.abi.aarch64.macos.MacOsAArch64Linker;
import jdk.internal.foreign.abi.x64.sysv.SysVx64Linker;
import jdk.internal.foreign.abi.x64.windows.Windowsx64Linker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.ref.Reference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.invoke.MethodHandles.collectArguments;
import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.dropReturn;
import static java.lang.invoke.MethodHandles.empty;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.identity;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodHandles.permuteArguments;
import static java.lang.invoke.MethodHandles.tryFinally;
import static java.lang.invoke.MethodType.methodType;
import static jdk.incubator.foreign.CLinker.*;

public class SharedUtils {

    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

    private static final MethodHandle MH_ALLOC_BUFFER;
    private static final MethodHandle MH_BASEADDRESS;
    private static final MethodHandle MH_BUFFER_COPY;
    private static final MethodHandle MH_MAKE_CONTEXT_NO_ALLOCATOR;
    private static final MethodHandle MH_MAKE_CONTEXT_BOUNDED_ALLOCATOR;
    private static final MethodHandle MH_CLOSE_CONTEXT;
    private static final MethodHandle MH_REACHBILITY_FENCE;
    private static final MethodHandle MH_HANDLE_UNCAUGHT_EXCEPTION;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MH_ALLOC_BUFFER = lookup.findVirtual(SegmentAllocator.class, "allocate",
                    methodType(MemorySegment.class, MemoryLayout.class));
            MH_BASEADDRESS = lookup.findVirtual(MemorySegment.class, "address",
                    methodType(MemoryAddress.class));
            MH_BUFFER_COPY = lookup.findStatic(SharedUtils.class, "bufferCopy",
                    methodType(MemoryAddress.class, MemoryAddress.class, MemorySegment.class));
            MH_MAKE_CONTEXT_NO_ALLOCATOR = lookup.findStatic(Binding.Context.class, "ofScope",
                    methodType(Binding.Context.class));
            MH_MAKE_CONTEXT_BOUNDED_ALLOCATOR = lookup.findStatic(Binding.Context.class, "ofBoundedAllocator",
                    methodType(Binding.Context.class, long.class));
            MH_CLOSE_CONTEXT = lookup.findVirtual(Binding.Context.class, "close",
                    methodType(void.class));
            MH_REACHBILITY_FENCE = lookup.findStatic(Reference.class, "reachabilityFence",
                    methodType(void.class, Object.class));
            MH_HANDLE_UNCAUGHT_EXCEPTION = lookup.findStatic(SharedUtils.class, "handleUncaughtException",
                    methodType(void.class, Throwable.class));
        } catch (ReflectiveOperationException e) {
            throw new BootstrapMethodError(e);
        }
    }

    // this allocator should be used when no allocation is expected
    public static final SegmentAllocator THROWING_ALLOCATOR = (size, align) -> { throw new IllegalStateException("Cannot get here"); };

    /**
     * Align the specified type from a given address
     * @return The address the data should be at based on alignment requirement
     */
    public static long align(MemoryLayout t, boolean isVar, long addr) {
        return alignUp(addr, alignment(t, isVar));
    }

    public static long alignUp(long addr, long alignment) {
        return ((addr - 1) | (alignment - 1)) + 1;
    }

    /**
     * The alignment requirement for a given type
     * @param isVar indicate if the type is a standalone variable. This change how
     * array is aligned. for example.
     */
    public static long alignment(MemoryLayout t, boolean isVar) {
        if (t instanceof ValueLayout) {
            return alignmentOfScalar((ValueLayout) t);
        } else if (t instanceof SequenceLayout) {
            // when array is used alone
            return alignmentOfArray((SequenceLayout) t, isVar);
        } else if (t instanceof GroupLayout) {
            return alignmentOfContainer((GroupLayout) t);
        } else if (t.isPadding()) {
            return 1;
        } else {
            throw new IllegalArgumentException("Invalid type: " + t);
        }
    }

    private static long alignmentOfScalar(ValueLayout st) {
        return st.byteSize();
    }

    private static long alignmentOfArray(SequenceLayout ar, boolean isVar) {
        if (ar.elementCount().orElseThrow() == 0) {
            // VLA or incomplete
            return 16;
        } else if ((ar.byteSize()) >= 16 && isVar) {
            return 16;
        } else {
            // align as element type
            MemoryLayout elementType = ar.elementLayout();
            return alignment(elementType, false);
        }
    }

    private static long alignmentOfContainer(GroupLayout ct) {
        // Most strict member
        return ct.memberLayouts().stream().mapToLong(t -> alignment(t, false)).max().orElse(1);
    }

    /**
     * Takes a MethodHandle that takes an input buffer as a first argument (a MemoryAddress), and returns nothing,
     * and adapts it to return a MemorySegment, by allocating a MemorySegment for the input
     * buffer, calling the target MethodHandle, and then returning the allocated MemorySegment.
     *
     * This allows viewing a MethodHandle that makes use of in memory return (IMR) as a MethodHandle that just returns
     * a MemorySegment without requiring a pre-allocated buffer as an explicit input.
     *
     * @param handle the target handle to adapt
     * @param cDesc the function descriptor of the native function (with actual return layout)
     * @return the adapted handle
     */
    public static MethodHandle adaptDowncallForIMR(MethodHandle handle, FunctionDescriptor cDesc) {
        if (handle.type().returnType() != void.class)
            throw new IllegalArgumentException("return expected to be void for in memory returns: " + handle.type());
        if (handle.type().parameterType(2) != MemoryAddress.class)
            throw new IllegalArgumentException("MemoryAddress expected as third param: " + handle.type());
        if (cDesc.returnLayout().isEmpty())
            throw new IllegalArgumentException("Return layout needed: " + cDesc);

        MethodHandle ret = identity(MemorySegment.class); // (MemorySegment) MemorySegment
        handle = collectArguments(ret, 1, handle); // (MemorySegment, Addressable, SegmentAllocator, MemoryAddress, ...) MemorySegment
        handle = collectArguments(handle, 3, MH_BASEADDRESS); // (MemorySegment, Addressable, SegmentAllocator, MemorySegment, ...) MemorySegment
        handle = mergeArguments(handle, 0, 3);  // (MemorySegment, Addressable, SegmentAllocator, ...) MemorySegment
        handle = collectArguments(handle, 0, insertArguments(MH_ALLOC_BUFFER, 1, cDesc.returnLayout().get())); // (SegmentAllocator, Addressable, SegmentAllocator, ...) MemoryAddress
        handle = mergeArguments(handle, 0, 2);  // (SegmentAllocator, Addressable, ...) MemoryAddress
        handle = swapArguments(handle, 0, 1); // (Addressable, SegmentAllocator, ...) MemoryAddress
        return handle;
    }

    /**
     * Takes a MethodHandle that returns a MemorySegment, and adapts it to take an input buffer as a first argument
     * (a MemoryAddress), and upon invocation, copies the contents of the returned MemorySegment into the input buffer
     * passed as the first argument.
     *
     * @param target the target handle to adapt
     * @return the adapted handle
     */
    public static MethodHandle adaptUpcallForIMR(MethodHandle target, boolean dropReturn) {
        if (target.type().returnType() != MemorySegment.class)
            throw new IllegalArgumentException("Must return MemorySegment for IMR");

        target = collectArguments(MH_BUFFER_COPY, 1, target); // (MemoryAddress, ...) MemoryAddress

        if (dropReturn) { // no handling for return value, need to drop it
            target = dropReturn(target);
        }

        return target;
    }

    private static MemoryAddress bufferCopy(MemoryAddress dest, MemorySegment buffer) {
        MemoryAddressImpl.ofLongUnchecked(dest.toRawLongValue(), buffer.byteSize()).copyFrom(buffer);
        return dest;
    }

    public static void checkCompatibleType(Class<?> carrier, MemoryLayout layout, long addressSize) {
        if (carrier.isPrimitive()) {
            Utils.checkPrimitiveCarrierCompat(carrier, layout);
        } else if (carrier == MemoryAddress.class) {
            Utils.checkLayoutType(layout, ValueLayout.class);
            if (layout.bitSize() != addressSize)
                throw new IllegalArgumentException("Address size mismatch: " + addressSize + " != " + layout.bitSize());
        } else if (carrier == MemorySegment.class) {
            Utils.checkLayoutType(layout, GroupLayout.class);
        } else {
            throw new IllegalArgumentException("Unsupported carrier: " + carrier);
        }
    }

    public static void checkFunctionTypes(MethodType mt, FunctionDescriptor cDesc, long addressSize) {
        if (mt.returnType() == void.class != cDesc.returnLayout().isEmpty())
            throw new IllegalArgumentException("Return type mismatch: " + mt + " != " + cDesc);
        List<MemoryLayout> argLayouts = cDesc.argumentLayouts();
        if (mt.parameterCount() != argLayouts.size())
            throw new IllegalArgumentException("Arity mismatch: " + mt + " != " + cDesc);

        int paramCount = mt.parameterCount();
        for (int i = 0; i < paramCount; i++) {
            checkCompatibleType(mt.parameterType(i), argLayouts.get(i), addressSize);
        }
        cDesc.returnLayout().ifPresent(rl -> checkCompatibleType(mt.returnType(), rl, addressSize));
    }

    public static Class<?> primitiveCarrierForSize(long size, boolean useFloat) {
        if (useFloat) {
            if (size == 4) {
                return float.class;
            } else if (size == 8) {
                return double.class;
            }
        } else {
            if (size == 1) {
                return byte.class;
            } else if (size == 2) {
                return short.class;
            } else if (size <= 4) {
                return int.class;
            } else if (size <= 8) {
                return long.class;
            }
        }

        throw new IllegalArgumentException("No type for size: " + size + " isFloat=" + useFloat);
    }

    public static CLinker getSystemLinker() {
        return switch (CABI.current()) {
            case Win64 -> Windowsx64Linker.getInstance();
            case SysV -> SysVx64Linker.getInstance();
            case LinuxAArch64 -> LinuxAArch64Linker.getInstance();
            case MacOsAArch64 -> MacOsAArch64Linker.getInstance();
        };
    }

    public static String toJavaStringInternal(MemorySegment segment, long start) {
        int len = strlen(segment, start);
        byte[] bytes = new byte[len];
        MemorySegment.ofArray(bytes)
                .copyFrom(segment.asSlice(start, len));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int strlen(MemorySegment segment, long start) {
        // iterate until overflow (String can only hold a byte[], whose length can be expressed as an int)
        for (int offset = 0; offset >= 0; offset++) {
            byte curr = MemoryAccess.getByteAtOffset(segment, start + offset);
            if (curr == 0) {
                return offset;
            }
        }
        throw new IllegalArgumentException("String too large");
    }

    static long bufferCopySize(CallingSequence callingSequence) {
        // FIXME: > 16 bytes alignment might need extra space since the
        // starting address of the allocator might be un-aligned.
        long size = 0;
        for (int i = 0; i < callingSequence.argumentCount(); i++) {
            List<Binding> bindings = callingSequence.argumentBindings(i);
            for (Binding b : bindings) {
                if (b instanceof Binding.Copy) {
                    Binding.Copy c = (Binding.Copy) b;
                    size = Utils.alignUp(size, c.alignment());
                    size += c.size();
                } else if (b instanceof Binding.Allocate) {
                    Binding.Allocate c = (Binding.Allocate) b;
                    size = Utils.alignUp(size, c.alignment());
                    size += c.size();
                }
            }
        }
        return size;
    }

    static Map<VMStorage, Integer> indexMap(Binding.Move[] moves) {
        return IntStream.range(0, moves.length)
                        .boxed()
                        .collect(Collectors.toMap(i -> moves[i].storage(), i -> i));
    }

    static MethodHandle mergeArguments(MethodHandle mh, int sourceIndex, int destIndex) {
        MethodType oldType = mh.type();
        Class<?> sourceType = oldType.parameterType(sourceIndex);
        Class<?> destType = oldType.parameterType(destIndex);
        if (sourceType != destType) {
            // TODO meet?
            throw new IllegalArgumentException("Parameter types differ: " + sourceType + " != " + destType);
        }
        MethodType newType = oldType.dropParameterTypes(destIndex, destIndex + 1);
        int[] reorder = new int[oldType.parameterCount()];
        assert destIndex > sourceIndex;
        for (int i = 0, index = 0; i < reorder.length; i++) {
            if (i != destIndex) {
                reorder[i] = index++;
            } else {
                reorder[i] = sourceIndex;
            }
        }
        return permuteArguments(mh, newType, reorder);
    }


    static MethodHandle swapArguments(MethodHandle mh, int firstArg, int secondArg) {
        MethodType mtype = mh.type();
        int[] perms = new int[mtype.parameterCount()];
        MethodType swappedType = MethodType.methodType(mtype.returnType());
        for (int i = 0 ; i < perms.length ; i++) {
            int dst = i;
            if (i == firstArg) dst = secondArg;
            if (i == secondArg) dst = firstArg;
            perms[i] = dst;
            swappedType = swappedType.appendParameterTypes(mtype.parameterType(dst));
        }
        return permuteArguments(mh, swappedType, perms);
    }

    private static MethodHandle reachabilityFenceHandle(Class<?> type) {
        return MH_REACHBILITY_FENCE.asType(MethodType.methodType(void.class, type));
    }

    static void handleUncaughtException(Throwable t) {
        if (t != null) {
            t.printStackTrace();
            JLA.exit(1);
        }
    }

    static MethodHandle wrapWithAllocator(MethodHandle specializedHandle,
                                          int allocatorPos, long bufferCopySize,
                                          boolean upcall) {
        // insert try-finally to close the NativeScope used for Binding.Copy
        MethodHandle closer;
        int insertPos;
        if (specializedHandle.type().returnType() == void.class) {
            if (!upcall) {
                closer = empty(methodType(void.class, Throwable.class)); // (Throwable) -> void
            } else {
                closer = MH_HANDLE_UNCAUGHT_EXCEPTION;
            }
            insertPos = 1;
        } else {
            closer = identity(specializedHandle.type().returnType()); // (V) -> V
            closer = dropArguments(closer, 0, Throwable.class); // (Throwable, V) -> V
            insertPos = 2;
        }

        // downcalls get the leading Addressable/SegmentAllocator param as well
        if (!upcall) {
            closer = collectArguments(closer, insertPos++, reachabilityFenceHandle(Addressable.class));
            closer = dropArguments(closer, insertPos++, SegmentAllocator.class); // (Throwable, V?, Addressable, SegmentAllocator) -> V/void
        }

        closer = collectArguments(closer, insertPos++, MH_CLOSE_CONTEXT); // (Throwable, V?, Addressable?, BindingContext) -> V/void

        if (!upcall) {
            // now for each Addressable parameter, add a reachability fence
            MethodType specType = specializedHandle.type();
            // skip 3 for address, segment allocator, and binding context
            for (int i = 3; i < specType.parameterCount(); i++) {
                Class<?> param = specType.parameterType(i);
                if (Addressable.class.isAssignableFrom(param)) {
                    closer = collectArguments(closer, insertPos++, reachabilityFenceHandle(param));
                } else {
                    closer = dropArguments(closer, insertPos++, param);
                }
            }
        }

        MethodHandle contextFactory;

        if (bufferCopySize > 0) {
            contextFactory = MethodHandles.insertArguments(MH_MAKE_CONTEXT_BOUNDED_ALLOCATOR, 0, bufferCopySize);
        } else if (upcall) {
            contextFactory = MH_MAKE_CONTEXT_NO_ALLOCATOR;
        } else {
            // this path is probably never used now, since ProgrammableInvoker never calls this routine with bufferCopySize == 0
            contextFactory = constant(Binding.Context.class, Binding.Context.DUMMY);
        }

        specializedHandle = tryFinally(specializedHandle, closer);
        specializedHandle = collectArguments(specializedHandle, allocatorPos, contextFactory);
        return specializedHandle;
    }

    // lazy init MH_ALLOC and MH_FREE handles
    private static class AllocHolder {

        private static final CLinker SYS_LINKER = getSystemLinker();

        static final MethodHandle MH_MALLOC = SYS_LINKER.downcallHandle(CLinker.systemLookup().lookup("malloc").get(),
                        MethodType.methodType(MemoryAddress.class, long.class),
                FunctionDescriptor.of(C_POINTER, C_LONG_LONG));

        static final MethodHandle MH_FREE = SYS_LINKER.downcallHandle(CLinker.systemLookup().lookup("free").get(),
                        MethodType.methodType(void.class, MemoryAddress.class),
                FunctionDescriptor.ofVoid(C_POINTER));
    }

    public static MemoryAddress checkSymbol(Addressable symbol) {
        return checkAddressable(symbol, "Symbol is NULL");
    }

    public static MemoryAddress checkAddress(MemoryAddress address) {
        return checkAddressable(address, "Address is NULL");
    }

    private static MemoryAddress checkAddressable(Addressable symbol, String msg) {
        Objects.requireNonNull(symbol);
        MemoryAddress symbolAddr = symbol.address();
        if (symbolAddr.equals(MemoryAddress.NULL))
            throw new IllegalArgumentException("Symbol is NULL: " + symbolAddr);
        return symbolAddr;
    }

    public static MemoryAddress allocateMemoryInternal(long size) {
        try {
            return (MemoryAddress) AllocHolder.MH_MALLOC.invokeExact(size);
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }

    public static void freeMemoryInternal(MemoryAddress addr) {
        try {
            AllocHolder.MH_FREE.invokeExact(addr);
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }

    public static VaList newVaList(Consumer<VaList.Builder> actions, ResourceScope scope) {
        return switch (CABI.current()) {
            case Win64 -> Windowsx64Linker.newVaList(actions, scope);
            case SysV -> SysVx64Linker.newVaList(actions, scope);
            case LinuxAArch64 -> LinuxAArch64Linker.newVaList(actions, scope);
            case MacOsAArch64 -> MacOsAArch64Linker.newVaList(actions, scope);
        };
    }

    public static VarHandle vhPrimitiveOrAddress(Class<?> carrier, MemoryLayout layout) {
        return carrier == MemoryAddress.class
            ? MemoryHandles.asAddressVarHandle(layout.varHandle(primitiveCarrierForSize(layout.byteSize(), false)))
            : layout.varHandle(carrier);
    }

    public static VaList newVaListOfAddress(MemoryAddress ma, ResourceScope scope) {
        return switch (CABI.current()) {
            case Win64 -> Windowsx64Linker.newVaListOfAddress(ma, scope);
            case SysV -> SysVx64Linker.newVaListOfAddress(ma, scope);
            case LinuxAArch64 -> LinuxAArch64Linker.newVaListOfAddress(ma, scope);
            case MacOsAArch64 -> MacOsAArch64Linker.newVaListOfAddress(ma, scope);
        };
    }

    public static VaList emptyVaList() {
        return switch (CABI.current()) {
            case Win64 -> Windowsx64Linker.emptyVaList();
            case SysV -> SysVx64Linker.emptyVaList();
            case LinuxAArch64 -> LinuxAArch64Linker.emptyVaList();
            case MacOsAArch64 -> MacOsAArch64Linker.emptyVaList();
        };
    }

    public static MethodType convertVaListCarriers(MethodType mt, Class<?> carrier) {
        Class<?>[] params = new Class<?>[mt.parameterCount()];
        for (int i = 0; i < params.length; i++) {
            Class<?> pType = mt.parameterType(i);
            params[i] = ((pType == VaList.class) ? carrier : pType);
        }
        return methodType(mt.returnType(), params);
    }

    public static MethodHandle unboxVaLists(MethodType type, MethodHandle handle, MethodHandle unboxer) {
        for (int i = 0; i < type.parameterCount(); i++) {
            if (type.parameterType(i) == VaList.class) {
               handle = filterArguments(handle, i + 1, unboxer); // +1 for leading address
            }
        }
        return handle;
    }

    public static MethodHandle boxVaLists(MethodHandle handle, MethodHandle boxer) {
        MethodType type = handle.type();
        for (int i = 0; i < type.parameterCount(); i++) {
            if (type.parameterType(i) == VaList.class) {
               handle = filterArguments(handle, i, boxer);
            }
        }
        return handle;
    }

    static void checkType(Class<?> actualType, Class<?> expectedType) {
        if (expectedType != actualType) {
            throw new IllegalArgumentException(
                    String.format("Invalid operand type: %s. %s expected", actualType, expectedType));
        }
    }

    public static boolean isTrivial(FunctionDescriptor cDesc) {
        return cDesc.attribute(FunctionDescriptor.TRIVIAL_ATTRIBUTE_NAME)
                .map(Boolean.class::cast)
                .orElse(false);
    }

    public static class SimpleVaArg {
        public final Class<?> carrier;
        public final MemoryLayout layout;
        public final Object value;

        public SimpleVaArg(Class<?> carrier, MemoryLayout layout, Object value) {
            this.carrier = carrier;
            this.layout = layout;
            this.value = value;
        }

        public VarHandle varHandle() {
            return carrier == MemoryAddress.class
                ? MemoryHandles.asAddressVarHandle(layout.varHandle(primitiveCarrierForSize(layout.byteSize(), false)))
                : layout.varHandle(carrier);
        }
    }

    public static non-sealed class EmptyVaList implements VaList {

        private final MemoryAddress address;

        public EmptyVaList(MemoryAddress address) {
            this.address = address;
        }

        private static UnsupportedOperationException uoe() {
            return new UnsupportedOperationException("Empty VaList");
        }

        @Override
        public int vargAsInt(MemoryLayout layout) {
            throw uoe();
        }

        @Override
        public long vargAsLong(MemoryLayout layout) {
            throw uoe();
        }

        @Override
        public double vargAsDouble(MemoryLayout layout) {
            throw uoe();
        }

        @Override
        public MemoryAddress vargAsAddress(MemoryLayout layout) {
            throw uoe();
        }

        @Override
        public MemorySegment vargAsSegment(MemoryLayout layout, SegmentAllocator allocator) {
            throw uoe();
        }

        @Override
        public MemorySegment vargAsSegment(MemoryLayout layout, ResourceScope scope) {
            throw uoe();
        }

        @Override
        public void skip(MemoryLayout... layouts) {
            throw uoe();
        }

        @Override
        public ResourceScope scope() {
            return ResourceScope.globalScope();
        }

        @Override
        public VaList copy() {
            return this;
        }

        @Override
        public MemoryAddress address() {
            return address;
        }
    }

    static void writeOverSized(MemorySegment ptr, Class<?> type, Object o) {
        // use VH_LONG for integers to zero out the whole register in the process
        if (type == long.class) {
            MemoryAccess.setLong(ptr, (long) o);
        } else if (type == int.class) {
            MemoryAccess.setLong(ptr, (int) o);
        } else if (type == short.class) {
            MemoryAccess.setLong(ptr, (short) o);
        } else if (type == char.class) {
            MemoryAccess.setLong(ptr, (char) o);
        } else if (type == byte.class) {
            MemoryAccess.setLong(ptr, (byte) o);
        } else if (type == float.class) {
            MemoryAccess.setFloat(ptr, (float) o);
        } else if (type == double.class) {
            MemoryAccess.setDouble(ptr, (double) o);
        } else {
            throw new IllegalArgumentException("Unsupported carrier: " + type);
        }
    }

    static void write(MemorySegment ptr, Class<?> type, Object o) {
        if (type == long.class) {
            MemoryAccess.setLong(ptr, (long) o);
        } else if (type == int.class) {
            MemoryAccess.setInt(ptr, (int) o);
        } else if (type == short.class) {
            MemoryAccess.setShort(ptr, (short) o);
        } else if (type == char.class) {
            MemoryAccess.setChar(ptr, (char) o);
        } else if (type == byte.class) {
            MemoryAccess.setByte(ptr, (byte) o);
        } else if (type == float.class) {
            MemoryAccess.setFloat(ptr, (float) o);
        } else if (type == double.class) {
            MemoryAccess.setDouble(ptr, (double) o);
        } else {
            throw new IllegalArgumentException("Unsupported carrier: " + type);
        }
    }

    static Object read(MemorySegment ptr, Class<?> type) {
        if (type == long.class) {
            return MemoryAccess.getLong(ptr);
        } else if (type == int.class) {
            return MemoryAccess.getInt(ptr);
        } else if (type == short.class) {
            return MemoryAccess.getShort(ptr);
        } else if (type == char.class) {
            return MemoryAccess.getChar(ptr);
        } else if (type == byte.class) {
            return MemoryAccess.getByte(ptr);
        } else if (type == float.class) {
            return MemoryAccess.getFloat(ptr);
        } else if (type == double.class) {
            return MemoryAccess.getDouble(ptr);
        } else {
            throw new IllegalArgumentException("Unsupported carrier: " + type);
        }
    }
}
