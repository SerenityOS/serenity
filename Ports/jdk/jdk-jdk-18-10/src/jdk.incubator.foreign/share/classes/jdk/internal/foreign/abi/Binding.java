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

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.internal.foreign.MemoryAddressImpl;
import jdk.internal.foreign.ResourceScopeImpl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

import static java.lang.invoke.MethodHandles.collectArguments;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodType.methodType;

/**
 * The binding operators defined in the Binding class can be combined into argument and return value processing 'recipes'.
 *
 * The binding operators are interpreted using a stack-base interpreter. Operators can either consume operands from the
 * stack, or push them onto the stack.
 *
 * In the description of each binding we talk about 'boxing' and 'unboxing'.
 *  - Unboxing is the process of taking a Java value and decomposing it, and storing components into machine
 *    storage locations. As such, the binding interpreter stack starts with the Java value on it, and should end empty.
 *  - Boxing is the process of re-composing a Java value by pulling components from machine storage locations.
 *    If a MemorySegment is needed to store the result, one should be allocated using the ALLOCATE_BUFFER operator.
 *    The binding interpreter stack starts off empty, and ends with the value to be returned as the only value on it.
 * A binding operator can be interpreted differently based on whether we are boxing or unboxing a value. For example,
 * the CONVERT_ADDRESS operator 'unboxes' a MemoryAddress to a long, but 'boxes' a long to a MemoryAddress.
 *
 * Here are some examples of binding recipes derived from C declarations, and according to the Windows ABI (recipes are
 * ABI-specific). Note that each argument has it's own recipe, which is indicated by '[number]:' (though, the only
 * example that has multiple arguments is the one using varargs).
 *
 * --------------------
 *
 * void f(int i);
 *
 * Argument bindings:
 * 0: VM_STORE(rcx, int.class) // move an 'int' into the RCX register
 *
 * Return bindings:
 * none
 *
 * --------------------
 *
 * void f(int* i);
 *
 * Argument bindings:
 * 0: UNBOX_ADDRESS // the 'MemoryAddress' is converted into a 'long'
 *    VM_STORE(rcx, long.class) // the 'long' is moved into the RCX register
 *
 * Return bindings:
 * none
 *
 * --------------------
 *
 * int* f();
 *
 * Argument bindings:
 * none
 *
 * Return bindings:
 * 0: VM_LOAD(rax, long) // load a 'long' from the RAX register
 *    BOX_ADDRESS // convert the 'long' into a 'MemoryAddress'
 *
 * --------------------
 *
 * typedef struct { // fits into single register
 *   int x;
 *   int y;
 * } MyStruct;
 *
 * void f(MyStruct ms);
 *
 * Argument bindings:
 * 0: BUFFER_LOAD(0, long.class) // From the struct's memory region, load a 'long' from offset '0'
 *    VM_STORE(rcx, long.class) // and copy that into the RCX register
 *
 * Return bindings:
 * none
 *
 * --------------------
 *
 * typedef struct { // does not fit into single register
 *   long long x;
 *   long long y;
 * } MyStruct;
 *
 * void f(MyStruct ms);
 *
 * For the Windows ABI:
 *
 * Argument bindings:
 * 0: COPY(16, 8) // copy the memory region containing the struct
 *    BASE_ADDRESS // take the base address of the copy
 *    UNBOX_ADDRESS // converts the base address to a 'long'
 *    VM_STORE(rcx, long.class) // moves the 'long' into the RCX register
 *
 * Return bindings:
 * none
 *
 * For the SysV ABI:
 *
 * Argument bindings:
 * 0: DUP // duplicates the MemoryRegion operand
 *    BUFFER_LOAD(0, long.class) // loads a 'long' from offset '0'
 *    VM_STORE(rdx, long.class) // moves the long into the RDX register
 *    BUFFER_LOAD(8, long.class) // loads a 'long' from offset '8'
 *    VM_STORE(rcx, long.class) // moves the long into the RCX register
 *
 * Return bindings:
 * none
 *
 * --------------------
 *
 * typedef struct { // fits into single register
 *   int x;
 *   int y;
 * } MyStruct;
 *
 * MyStruct f();
 *
 * Argument bindings:
 * none
 *
 * Return bindings:
 * 0: ALLOCATE(GroupLayout(C_INT, C_INT)) // allocate a buffer with the memory layout of the struct
 *    DUP // duplicate the allocated buffer
 *    VM_LOAD(rax, long.class) // loads a 'long' from rax
 *    BUFFER_STORE(0, long.class) // stores a 'long' at offset 0
 *
 * --------------------
 *
 * typedef struct { // does not fit into single register
 *   long long x;
 *   long long y;
 * } MyStruct;
 *
 * MyStruct f();
 *
 * !! uses synthetic argument, which is a pointer to a pre-allocated buffer
 *
 * Argument bindings:
 * 0: UNBOX_ADDRESS // unbox the MemoryAddress synthetic argument
 *    VM_STORE(rcx, long.class) // moves the 'long' into the RCX register
 *
 * Return bindings:
 * none
 *
 * --------------------
 *
 * void f(int dummy, ...); // varargs
 *
 * f(0, 10f); // passing a float
 *
 * Argument bindings:
 * 0: VM_STORE(rcx, int.class) // moves the 'int dummy' into the RCX register
 *
 * 1: DUP // duplicates the '10f' argument
 *    VM_STORE(rdx, float.class) // move one copy into the RDX register
 *    VM_STORE(xmm1, float.class) // moves the other copy into the xmm2 register
 *
 * Return bindings:
 * none
 *
 * --------------------
 */
public abstract class Binding {
    private static final MethodHandle MH_UNBOX_ADDRESS;
    private static final MethodHandle MH_BOX_ADDRESS;
    private static final MethodHandle MH_BASE_ADDRESS;
    private static final MethodHandle MH_COPY_BUFFER;
    private static final MethodHandle MH_ALLOCATE_BUFFER;
    private static final MethodHandle MH_TO_SEGMENT;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MH_UNBOX_ADDRESS = lookup.findVirtual(MemoryAddress.class, "toRawLongValue",
                    methodType(long.class));
            MH_BOX_ADDRESS = lookup.findStatic(MemoryAddress.class, "ofLong",
                    methodType(MemoryAddress.class, long.class));
            MH_BASE_ADDRESS = lookup.findVirtual(MemorySegment.class, "address",
                    methodType(MemoryAddress.class));
            MH_COPY_BUFFER = lookup.findStatic(Binding.Copy.class, "copyBuffer",
                    methodType(MemorySegment.class, MemorySegment.class, long.class, long.class, Context.class));
            MH_ALLOCATE_BUFFER = lookup.findStatic(Binding.Allocate.class, "allocateBuffer",
                    methodType(MemorySegment.class, long.class, long.class, Context.class));
            MH_TO_SEGMENT = lookup.findStatic(Binding.ToSegment.class, "toSegment",
                    methodType(MemorySegment.class, MemoryAddress.class, long.class, Context.class));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A binding context is used as an helper to carry out evaluation of certain bindings; for instance,
     * it helps {@link Allocate} bindings, by providing the {@link SegmentAllocator} that should be used for
     * the allocation operation, or {@link ToSegment} bindings, by providing the {@link ResourceScope} that
     * should be used to create an unsafe struct from a memory address.
     */
    public static class Context implements AutoCloseable {
        private final SegmentAllocator allocator;
        private final ResourceScope scope;

        private Context(SegmentAllocator allocator, ResourceScope scope) {
            this.allocator = allocator;
            this.scope = scope;
        }

        public SegmentAllocator allocator() {
            return allocator;
        }

        public ResourceScope scope() {
            return scope;
        }

        @Override
        public void close() {
            scope().close();
        }

        /**
         * Create a binding context from given native scope.
         */
        public static Context ofBoundedAllocator(long size) {
            ResourceScope scope = ResourceScope.newConfinedScope();
            return new Context(SegmentAllocator.arenaAllocator(size, scope), scope);
        }

        /**
         * Create a binding context from given segment allocator. The resulting context will throw when
         * the context's scope is accessed.
         */
        public static Context ofAllocator(SegmentAllocator allocator) {
            return new Context(allocator, null) {
                @Override
                public ResourceScope scope() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /**
         * Create a binding context from given scope. The resulting context will throw when
         * the context's allocator is accessed.
         */
        public static Context ofScope() {
            ResourceScope scope = ResourceScope.newConfinedScope();
            return new Context(null, scope) {
                @Override
                public SegmentAllocator allocator() { throw new UnsupportedOperationException(); }
            };
        }

        /**
         * Dummy binding context. Throws exceptions when attempting to access scope, return a throwing allocator, and has
         * an idempotent {@link #close()}.
         */
        public static final Context DUMMY = new Context(null, null) {
            @Override
            public SegmentAllocator allocator() {
                return SharedUtils.THROWING_ALLOCATOR;
            }

            @Override
            public ResourceScope scope() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void close() {
                // do nothing
            }
        };
    }

    enum Tag {
        VM_STORE,
        VM_LOAD,
        BUFFER_STORE,
        BUFFER_LOAD,
        COPY_BUFFER,
        ALLOC_BUFFER,
        BOX_ADDRESS,
        UNBOX_ADDRESS,
        BASE_ADDRESS,
        TO_SEGMENT,
        DUP
    }

    private final Tag tag;

    private Binding(Tag tag) {
        this.tag = tag;
    }

    public Tag tag() {
        return tag;
    }

    public abstract void verify(Deque<Class<?>> stack);

    public abstract void interpret(Deque<Object> stack, BindingInterpreter.StoreFunc storeFunc,
                                   BindingInterpreter.LoadFunc loadFunc, Context context);

    public abstract MethodHandle specialize(MethodHandle specializedHandle, int insertPos, int allocatorPos);

    private static void checkType(Class<?> type) {
        if (!type.isPrimitive() || type == void.class || type == boolean.class)
            throw new IllegalArgumentException("Illegal type: " + type);
    }

    private static void checkOffset(long offset) {
        if (offset < 0)
            throw new IllegalArgumentException("Negative offset: " + offset);
    }

    public static VMStore vmStore(VMStorage storage, Class<?> type) {
        checkType(type);
        return new VMStore(storage, type);
    }

    public static VMLoad vmLoad(VMStorage storage, Class<?> type) {
        checkType(type);
        return new VMLoad(storage, type);
    }

    public static BufferStore bufferStore(long offset, Class<?> type) {
        checkType(type);
        checkOffset(offset);
        return new BufferStore(offset, type);
    }

    public static BufferLoad bufferLoad(long offset, Class<?> type) {
        checkType(type);
        checkOffset(offset);
        return new BufferLoad(offset, type);
    }

    public static Copy copy(MemoryLayout layout) {
        return new Copy(layout.byteSize(), layout.byteAlignment());
    }

    public static Allocate allocate(MemoryLayout layout) {
        return new Allocate(layout.byteSize(), layout.byteAlignment());
    }

    public static BoxAddress boxAddress() {
        return BoxAddress.INSTANCE;
    }

    public static UnboxAddress unboxAddress() {
        return UnboxAddress.INSTANCE;
    }

    public static BaseAddress baseAddress() {
        return BaseAddress.INSTANCE;
    }

    public static ToSegment toSegment(MemoryLayout layout) {
        return new ToSegment(layout.byteSize());
    }

    public static Dup dup() {
        return Dup.INSTANCE;
    }


    public static Binding.Builder builder() {
        return new Binding.Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Binding binding = (Binding) o;
        return tag == binding.tag;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag);
    }

    /**
     * A builder helper class for generating lists of Bindings
     */
    public static class Builder {
        private final List<Binding> bindings = new ArrayList<>();

        public Binding.Builder vmStore(VMStorage storage, Class<?> type) {
            bindings.add(Binding.vmStore(storage, type));
            return this;
        }

        public Binding.Builder vmLoad(VMStorage storage, Class<?> type) {
            bindings.add(Binding.vmLoad(storage, type));
            return this;
        }

        public Binding.Builder bufferStore(long offset, Class<?> type) {
            bindings.add(Binding.bufferStore(offset, type));
            return this;
        }

        public Binding.Builder bufferLoad(long offset, Class<?> type) {
            bindings.add(Binding.bufferLoad(offset, type));
            return this;
        }

        public Binding.Builder copy(MemoryLayout layout) {
            bindings.add(Binding.copy(layout));
            return this;
        }

        public Binding.Builder allocate(MemoryLayout layout) {
            bindings.add(Binding.allocate(layout));
            return this;
        }

        public Binding.Builder boxAddress() {
            bindings.add(Binding.boxAddress());
            return this;
        }

        public Binding.Builder unboxAddress() {
            bindings.add(Binding.unboxAddress());
            return this;
        }

        public Binding.Builder baseAddress() {
            bindings.add(Binding.baseAddress());
            return this;
        }

        public Binding.Builder toSegment(MemoryLayout layout) {
            bindings.add(Binding.toSegment(layout));
            return this;
        }

        public Binding.Builder dup() {
            bindings.add(Binding.dup());
            return this;
        }

        public List<Binding> build() {
            return new ArrayList<>(bindings);
        }
    }

    static abstract class Move extends Binding {
        private final VMStorage storage;
        private final Class<?> type;

        private Move(Tag tag, VMStorage storage, Class<?> type) {
            super(tag);
            this.storage = storage;
            this.type = type;
        }

        public VMStorage storage() {
            return storage;
        }

        public Class<?> type() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Move move = (Move) o;
            return Objects.equals(storage, move.storage) &&
                    Objects.equals(type, move.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), storage, type);
        }
    }

    /**
     * VM_STORE([storage location], [type])
     * Pops a [type] from the operand stack, and moves it to [storage location]
     * The [type] must be one of byte, short, char, int, long, float, or double
     */
    public static class VMStore extends Move {
        private VMStore(VMStorage storage, Class<?> type) {
            super(Tag.VM_STORE, storage, type);
        }

        @Override
        public void verify(Deque<Class<?>> stack) {
            Class<?> actualType = stack.pop();
            Class<?> expectedType = type();
            SharedUtils.checkType(actualType, expectedType);
        }

        @Override
        public void interpret(Deque<Object> stack, BindingInterpreter.StoreFunc storeFunc,
                              BindingInterpreter.LoadFunc loadFunc, Context context) {
            storeFunc.store(storage(), type(), stack.pop());
        }

        @Override
        public MethodHandle specialize(MethodHandle specializedHandle, int insertPos, int allocatorPos) {
            return specializedHandle; // no-op
        }

        @Override
        public String toString() {
            return "VMStore{" +
                    "storage=" + storage() +
                    ", type=" + type() +
                    '}';
        }
    }

    /**
     * VM_LOAD([storage location], [type])
     * Loads a [type] from [storage location], and pushes it onto the operand stack.
     * The [type] must be one of byte, short, char, int, long, float, or double
     */
    public static class VMLoad extends Move {
        private VMLoad(VMStorage storage, Class<?> type) {
            super(Tag.VM_LOAD, storage, type);
        }

        @Override
        public void verify(Deque<Class<?>> stack) {
            stack.push(type());
        }

        @Override
        public void interpret(Deque<Object> stack, BindingInterpreter.StoreFunc storeFunc,
                              BindingInterpreter.LoadFunc loadFunc, Context context) {
            stack.push(loadFunc.load(storage(), type()));
        }

        @Override
        public MethodHandle specialize(MethodHandle specializedHandle, int insertPos, int allocatorPos) {
            return specializedHandle; // no-op
        }

        @Override
        public String toString() {
            return "VMLoad{" +
                    "storage=" + storage() +
                    ", type=" + type() +
                    '}';
        }
    }

    private static abstract class Dereference extends Binding {
        private final long offset;
        private final Class<?> type;

        private Dereference(Tag tag, long offset, Class<?> type) {
            super(tag);
            this.offset = offset;
            this.type = type;
        }

        public long offset() {
            return offset;
        }

        public Class<?> type() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Dereference that = (Dereference) o;
            return offset == that.offset &&
                    Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), offset, type);
        }

        public VarHandle varHandle() {
            // alignment is set to 1 byte here to avoid exceptions for cases where we do super word
            // copies of e.g. 2 int fields of a struct as a single long, while the struct is only
            // 4-byte-aligned (since it only contains ints)
            return MemoryHandles.insertCoordinates(MemoryHandles.varHandle(type, 1, ByteOrder.nativeOrder()), 1, offset);
        }
    }

    /**
     * BUFFER_STORE([offset into memory region], [type])
     * Pops a MemorySegment from the operand stack, loads a [type] from
     * [offset into memory region] from it, and pushes it onto the operand stack.
     * The [type] must be one of byte, short, char, int, long, float, or double
     */
    public static class BufferStore extends Dereference {
        private BufferStore(long offset, Class<?> type) {
            super(Tag.BUFFER_STORE, offset, type);
        }

        @Override
        public void verify(Deque<Class<?>> stack) {
            Class<?> storeType = stack.pop();
            SharedUtils.checkType(storeType, type());
            Class<?> segmentType = stack.pop();
            SharedUtils.checkType(segmentType, MemorySegment.class);
        }

        @Override
        public void interpret(Deque<Object> stack, BindingInterpreter.StoreFunc storeFunc,
                              BindingInterpreter.LoadFunc loadFunc, Context context) {
            Object value = stack.pop();
            MemorySegment operand = (MemorySegment) stack.pop();
            MemorySegment writeAddress = operand.asSlice(offset());
            SharedUtils.write(writeAddress, type(), value);
        }

        @Override
        public MethodHandle specialize(MethodHandle specializedHandle, int insertPos, int allocatorPos) {
            MethodHandle setter = varHandle().toMethodHandle(VarHandle.AccessMode.SET);
            setter = setter.asType(methodType(void.class, MemorySegment.class, type()));
            return collectArguments(specializedHandle, insertPos + 1, setter);
        }

        @Override
        public String toString() {
            return "BufferStore{" +
                    "offset=" + offset() +
                    ", type=" + type() +
                    '}';
        }
    }

    /**
     * BUFFER_LOAD([offset into memory region], [type])
     * Pops a [type], and then a MemorySegment from the operand stack,
     * and then stores [type] to [offset into memory region] of the MemorySegment.
     * The [type] must be one of byte, short, char, int, long, float, or double
     */
    public static class BufferLoad extends Dereference {
        private BufferLoad(long offset, Class<?> type) {
            super(Tag.BUFFER_LOAD, offset, type);
        }

        @Override
        public void verify(Deque<Class<?>> stack) {
            Class<?> actualType = stack.pop();
            SharedUtils.checkType(actualType, MemorySegment.class);
            Class<?> newType = type();
            stack.push(newType);
        }

        @Override
        public void interpret(Deque<Object> stack, BindingInterpreter.StoreFunc storeFunc,
                              BindingInterpreter.LoadFunc loadFunc, Context context) {
            MemorySegment operand = (MemorySegment) stack.pop();
            MemorySegment readAddress = operand.asSlice(offset());
            stack.push(SharedUtils.read(readAddress, type()));
        }

        @Override
        public MethodHandle specialize(MethodHandle specializedHandle, int insertPos, int allocatorPos) {
            MethodHandle filter = varHandle()
                    .toMethodHandle(VarHandle.AccessMode.GET)
                    .asType(methodType(type(), MemorySegment.class));
            return filterArguments(specializedHandle, insertPos, filter);
        }

        @Override
        public String toString() {
            return "BufferLoad{" +
                    "offset=" + offset() +
                    ", type=" + type() +
                    '}';
        }
    }

    /**
     * COPY([size], [alignment])
     *   Creates a new MemorySegment with the given [size] and [alignment],
     *     and copies contents from a MemorySegment popped from the top of the operand stack into this new buffer,
     *     and pushes the new buffer onto the operand stack
     */
    public static class Copy extends Binding {
        private final long size;
        private final long alignment;

        private Copy(long size, long alignment) {
            super(Tag.COPY_BUFFER);
            this.size = size;
            this.alignment = alignment;
        }

        private static MemorySegment copyBuffer(MemorySegment operand, long size, long alignment,
                                                    Context context) {
            MemorySegment copy = context.allocator().allocate(size, alignment);
            copy.copyFrom(operand.asSlice(0, size));
            return copy;
        }

        public long size() {
            return size;
        }

        public long alignment() {
            return alignment;
        }

        @Override
        public String toString() {
            return "Copy{" +
                    "tag=" + tag() +
                    ", size=" + size +
                    ", alignment=" + alignment +
                    '}';
        }

        @Override
        public void verify(Deque<Class<?>> stack) {
            Class<?> actualType = stack.pop();
            SharedUtils.checkType(actualType, MemorySegment.class);
            stack.push(MemorySegment.class);
        }

        @Override
        public void interpret(Deque<Object> stack, BindingInterpreter.StoreFunc storeFunc,
                              BindingInterpreter.LoadFunc loadFunc, Context context) {
            MemorySegment operand = (MemorySegment) stack.pop();
            MemorySegment copy = copyBuffer(operand, size, alignment, context);
            stack.push(copy);
        }

        @Override
        public MethodHandle specialize(MethodHandle specializedHandle, int insertPos, int allocatorPos) {
            MethodHandle filter = insertArguments(MH_COPY_BUFFER, 1, size, alignment);
            specializedHandle = collectArguments(specializedHandle, insertPos, filter);
            return SharedUtils.mergeArguments(specializedHandle, allocatorPos, insertPos + 1);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Copy copy = (Copy) o;
            return size == copy.size &&
                    alignment == copy.alignment;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), size, alignment);
        }
    }

    /**
     * ALLOCATE([size], [alignment])
     *   Creates a new MemorySegment with the give [size] and [alignment], and pushes it onto the operand stack.
     */
    public static class Allocate extends Binding {
        private final long size;
        private final long alignment;

        private Allocate(long size, long alignment) {
            super(Tag.ALLOC_BUFFER);
            this.size = size;
            this.alignment = alignment;
        }

        private static MemorySegment allocateBuffer(long size, long allignment, Context context) {
            return context.allocator().allocate(size, allignment);
        }

        public long size() {
            return size;
        }

        public long alignment() {
            return alignment;
        }

        @Override
        public String toString() {
            return "AllocateBuffer{" +
                    "tag=" + tag() +
                    "size=" + size +
                    ", alignment=" + alignment +
                    '}';
        }

        @Override
        public void verify(Deque<Class<?>> stack) {
            stack.push(MemorySegment.class);
        }

        @Override
        public void interpret(Deque<Object> stack, BindingInterpreter.StoreFunc storeFunc,
                              BindingInterpreter.LoadFunc loadFunc, Context context) {
            stack.push(allocateBuffer(size, alignment, context));
        }

        @Override
        public MethodHandle specialize(MethodHandle specializedHandle, int insertPos, int allocatorPos) {
            MethodHandle allocateBuffer = insertArguments(MH_ALLOCATE_BUFFER, 0, size, alignment);
            specializedHandle = collectArguments(specializedHandle, insertPos, allocateBuffer);
            return SharedUtils.mergeArguments(specializedHandle, allocatorPos, insertPos);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Allocate allocate = (Allocate) o;
            return size == allocate.size &&
                    alignment == allocate.alignment;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), size, alignment);
        }
    }

    /**
     * UNBOX_ADDRESS()
     * Pops a 'MemoryAddress' from the operand stack, converts it to a 'long',
     *     and pushes that onto the operand stack.
     */
    public static class UnboxAddress extends Binding {
        private static final UnboxAddress INSTANCE = new UnboxAddress();
        private UnboxAddress() {
            super(Tag.UNBOX_ADDRESS);
        }

        @Override
        public void verify(Deque<Class<?>> stack) {
            Class<?> actualType = stack.pop();
            SharedUtils.checkType(actualType, MemoryAddress.class);
            stack.push(long.class);
        }

        @Override
        public void interpret(Deque<Object> stack, BindingInterpreter.StoreFunc storeFunc,
                              BindingInterpreter.LoadFunc loadFunc, Context context) {
            stack.push(((MemoryAddress)stack.pop()).toRawLongValue());
        }

        @Override
        public MethodHandle specialize(MethodHandle specializedHandle, int insertPos, int allocatorPos) {
            return filterArguments(specializedHandle, insertPos, MH_UNBOX_ADDRESS);
        }

        @Override
        public String toString() {
            return "UnboxAddress{}";
        }
    }

    /**
     * BOX_ADDRESS()
     * Pops a 'long' from the operand stack, converts it to a 'MemoryAddress',
     *     and pushes that onto the operand stack.
     */
    public static class BoxAddress extends Binding {
        private static final BoxAddress INSTANCE = new BoxAddress();
        private BoxAddress() {
            super(Tag.BOX_ADDRESS);
        }

        @Override
        public void verify(Deque<Class<?>> stack) {
            Class<?> actualType = stack.pop();
            SharedUtils.checkType(actualType, long.class);
            stack.push(MemoryAddress.class);
        }

        @Override
        public void interpret(Deque<Object> stack, BindingInterpreter.StoreFunc storeFunc,
                              BindingInterpreter.LoadFunc loadFunc, Context context) {
            stack.push(MemoryAddress.ofLong((long) stack.pop()));
        }

        @Override
        public MethodHandle specialize(MethodHandle specializedHandle, int insertPos, int allocatorPos) {
            return filterArguments(specializedHandle, insertPos, MH_BOX_ADDRESS);
        }

        @Override
        public String toString() {
            return "BoxAddress{}";
        }
    }

    /**
     * BASE_ADDRESS()
     *   Pops a MemorySegment from the operand stack, and takes the base address of the segment
     *   (the MemoryAddress that points to the start), and pushes that onto the operand stack
     */
    public static class BaseAddress extends Binding {
        private static final BaseAddress INSTANCE = new BaseAddress();
        private BaseAddress() {
            super(Tag.BASE_ADDRESS);
        }

        @Override
        public void verify(Deque<Class<?>> stack) {
            Class<?> actualType = stack.pop();
            SharedUtils.checkType(actualType, MemorySegment.class);
            stack.push(MemoryAddress.class);
        }

        @Override
        public void interpret(Deque<Object> stack, BindingInterpreter.StoreFunc storeFunc,
                              BindingInterpreter.LoadFunc loadFunc, Context context) {
            stack.push(((MemorySegment) stack.pop()).address());
        }

        @Override
        public MethodHandle specialize(MethodHandle specializedHandle, int insertPos, int allocatorPos) {
            return filterArguments(specializedHandle, insertPos, MH_BASE_ADDRESS);
        }

        @Override
        public String toString() {
            return "BaseAddress{}";
        }
    }

    /**
     * TO_SEGMENT([size])
     *   Pops a MemoryAddress from the operand stack, and converts it to a MemorySegment
     *   with the given size, and pushes that onto the operand stack
     */
    public static class ToSegment extends Binding {
        private final long size;
        // FIXME alignment?

        public ToSegment(long size) {
            super(Tag.TO_SEGMENT);
            this.size = size;
        }

        private static MemorySegment toSegment(MemoryAddress operand, long size, Context context) {
            return MemoryAddressImpl.ofLongUnchecked(operand.toRawLongValue(), size, (ResourceScopeImpl) context.scope);
        }

        @Override
        public void verify(Deque<Class<?>> stack) {
            Class<?> actualType = stack.pop();
            SharedUtils.checkType(actualType, MemoryAddress.class);
            stack.push(MemorySegment.class);
        }

        @Override
        public void interpret(Deque<Object> stack, BindingInterpreter.StoreFunc storeFunc,
                              BindingInterpreter.LoadFunc loadFunc, Context context) {
            MemoryAddress operand = (MemoryAddress) stack.pop();
            MemorySegment segment = toSegment(operand, size, context);
            stack.push(segment);
        }

        @Override
        public MethodHandle specialize(MethodHandle specializedHandle, int insertPos, int allocatorPos) {
            MethodHandle toSegmentHandle = insertArguments(MH_TO_SEGMENT, 1, size);
            specializedHandle = collectArguments(specializedHandle, insertPos, toSegmentHandle);
            return SharedUtils.mergeArguments(specializedHandle, allocatorPos, insertPos + 1);
        }

        @Override
        public String toString() {
            return "ToSegemnt{" +
                    "size=" + size +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            ToSegment toSegemnt = (ToSegment) o;
            return size == toSegemnt.size;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), size);
        }
    }

    /**
     * DUP()
     *   Duplicates the value on the top of the operand stack (without popping it!),
     *   and pushes the duplicate onto the operand stack
     */
    public static class Dup extends Binding {
        private static final Dup INSTANCE = new Dup();
        private Dup() {
            super(Tag.DUP);
        }

        @Override
        public void verify(Deque<Class<?>> stack) {
            stack.push(stack.peekLast());
        }

        @Override
        public void interpret(Deque<Object> stack, BindingInterpreter.StoreFunc storeFunc,
                              BindingInterpreter.LoadFunc loadFunc, Context context) {
            stack.push(stack.peekLast());
        }

        /*
         * Fixes up Y-shaped data graphs (produced by DEREFERENCE):
         *
         * 1. DUP()
         * 2. BUFFER_LOAD(0, int.class)
         * 3. VM_STORE  (ignored)
         * 4. BUFFER_LOAD(4, int.class)
         * 5. VM_STORE  (ignored)
         *
         * (specialized in reverse!)
         *
         * 5. (int, int) -> void                       insertPos = 1
         * 4. (MemorySegment, int) -> void             insertPos = 1
         * 3. (MemorySegment, int) -> void             insertPos = 0
         * 2. (MemorySegment, MemorySegment) -> void   insertPos = 0
         * 1. (MemorySegment) -> void                  insertPos = 0
         *
         */
        @Override
        public MethodHandle specialize(MethodHandle specializedHandle, int insertPos, int allocatorPos) {
            return SharedUtils.mergeArguments(specializedHandle, insertPos, insertPos + 1);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }

        @Override
        public String toString() {
            return "Dup{}";
        }
    }
}
