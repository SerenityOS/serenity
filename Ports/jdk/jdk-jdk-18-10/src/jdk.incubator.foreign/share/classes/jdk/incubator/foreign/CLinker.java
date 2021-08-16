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
package jdk.incubator.foreign;

import jdk.internal.foreign.AbstractCLinker;
import jdk.internal.foreign.NativeMemorySegmentImpl;
import jdk.internal.foreign.PlatformLayouts;
import jdk.internal.foreign.SystemLookup;
import jdk.internal.foreign.abi.SharedUtils;
import jdk.internal.foreign.abi.aarch64.linux.LinuxAArch64VaList;
import jdk.internal.foreign.abi.aarch64.macos.MacOsAArch64VaList;
import jdk.internal.foreign.abi.x64.sysv.SysVVaList;
import jdk.internal.foreign.abi.x64.windows.WinVaList;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;

import java.lang.constant.Constable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

import static jdk.internal.foreign.PlatformLayouts.*;

/**
 * A C linker implements the C Application Binary Interface (ABI) calling conventions.
 * Instances of this interface can be used to link foreign functions in native libraries that
 * follow the JVM's target platform C ABI.
 * <p>
 * Linking a foreign function is a process which requires two components: a method type, and
 * a function descriptor. The method type, consists of a set of <em>carrier</em> types, which, together,
 * specify the Java signature which clients must adhere to when calling the underlying foreign function.
 * The function descriptor contains a set of memory layouts which, together, specify the foreign function
 * signature and classification information (via a custom layout attributes, see {@link TypeKind}), so that linking can take place.
 * <p>
 * Clients of this API can build function descriptors using the predefined memory layout constants
 * (based on a subset of the built-in types provided by the C language), found in this interface; alternatively,
 * they can also decorate existing value layouts using the required {@link TypeKind} classification attribute
 * (this can be done using the {@link MemoryLayout#withAttribute(String, Constable)} method). A failure to do so might
 * result in linkage errors, given that linking requires additional classification information to determine, for instance,
 * how arguments should be loaded into registers during a foreign function call.
 * <p>
 * Implementations of this interface support the following primitive carrier types:
 * {@code byte}, {@code short}, {@code char}, {@code int}, {@code long}, {@code float},
 * and {@code double}, as well as {@link MemoryAddress} for passing pointers, and
 * {@link MemorySegment} for passing structs and unions. Finally, the {@link VaList}
 * carrier type can be used to match the native {@code va_list} type.
 * <p>
 * For the linking process to be successful, some requirements must be satisfied; if {@code M} and {@code F} are
 * the method type (obtained after dropping any prefix arguments) and the function descriptor, respectively,
 * used during the linking process, then it must be that:
 * <ul>
 *     <li>The arity of {@code M} is the same as that of {@code F};</li>
 *     <li>If the return type of {@code M} is {@code void}, then {@code F} should have no return layout
 *     (see {@link FunctionDescriptor#ofVoid(MemoryLayout...)});</li>
 *     <li>for each pair of carrier type {@code C} and layout {@code L} in {@code M} and {@code F}, respectively,
 *     where {@code C} and {@code L} refer to the same argument, or to the return value, the following conditions must hold:
 *     <ul>
 *       <li>If {@code C} is a primitve type, then {@code L} must be a {@code ValueLayout}, and the size of the layout must match
 *       that of the carrier type (see {@link Integer#SIZE} and similar fields in other primitive wrapper classes);</li>
 *       <li>If {@code C} is {@code MemoryAddress.class}, then {@code L} must be a {@code ValueLayout}, and its size must match
 *       the platform's address size (see {@link MemoryLayouts#ADDRESS}). For this purpose, the {@link CLinker#C_POINTER} layout
 *       constant can  be used;</li>
 *       <li>If {@code C} is {@code MemorySegment.class}, then {@code L} must be a {@code GroupLayout}</li>
 *       <li>If {@code C} is {@code VaList.class}, then {@code L} must be {@link CLinker#C_VA_LIST}</li>
 *     </ul>
 *     </li>
 * </ul>
 *
 * <p>Variadic functions, declared in C either with a trailing ellipses ({@code ...}) at the end of the formal parameter
 * list or with an empty formal parameter list, are not supported directly. It is not possible to create a method handle
 * that takes a variable number of arguments, and neither is it possible to create an upcall stub wrapping a method
 * handle that accepts a variable number of arguments. However, for downcalls only, it is possible to link a native
 * variadic function by using a <em>specialized</em> method type and function descriptor: for each argument that is to be
 * passed as a variadic argument, an explicit, additional, carrier type and memory layout must be present in the method type and
 * function descriptor objects passed to the linker. Furthermore, as memory layouts corresponding to variadic arguments in
 * a function descriptor must contain additional classification information, it is required that
 * {@link #asVarArg(MemoryLayout)} is used to create the memory layouts for each parameter corresponding to a variadic
 * argument in a specialized function descriptor.
 *
 * <p>On unsupported platforms this class will fail to initialize with an {@link ExceptionInInitializerError}.
 *
 * <p> Unless otherwise specified, passing a {@code null} argument, or an array argument containing one or more {@code null}
 * elements to a method in this class causes a {@link NullPointerException NullPointerException} to be thrown. </p>
 *
 * @implSpec
 * Implementations of this interface are immutable, thread-safe and <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>.
 */
public sealed interface CLinker permits AbstractCLinker {

    /**
     * Returns the C linker for the current platform.
     * <p>
     * This method is <a href="package-summary.html#restricted"><em>restricted</em></a>.
     * Restricted methods are unsafe, and, if used incorrectly, their use might crash
     * the JVM or, worse, silently result in memory corruption. Thus, clients should refrain from depending on
     * restricted methods, and use safe and supported functionalities, where possible.
     *
     * @return a linker for this system.
     * @throws IllegalCallerException if access to this method occurs from a module {@code M} and the command line option
     * {@code --enable-native-access} is either absent, or does not mention the module name {@code M}, or
     * {@code ALL-UNNAMED} in case {@code M} is an unnamed module.
     */
    @CallerSensitive
    static CLinker getInstance() {
        Reflection.ensureNativeAccess(Reflection.getCallerClass());
        return SharedUtils.getSystemLinker();
    }

    /**
     * Obtains a system lookup which is suitable to find symbols in the standard C libraries. The set of symbols
     * available for lookup is unspecified, as it depends on the platform and on the operating system.
     * <p>
     * This method is <a href="package-summary.html#restricted"><em>restricted</em></a>.
     * Restricted methods are unsafe, and, if used incorrectly, their use might crash
     * the JVM or, worse, silently result in memory corruption. Thus, clients should refrain from depending on
     * restricted methods, and use safe and supported functionalities, where possible.
     * @return a system-specific library lookup which is suitable to find symbols in the standard C libraries.
     * @throws IllegalCallerException if access to this method occurs from a module {@code M} and the command line option
     * {@code --enable-native-access} is either absent, or does not mention the module name {@code M}, or
     * {@code ALL-UNNAMED} in case {@code M} is an unnamed module.
     */
    @CallerSensitive
    static SymbolLookup systemLookup() {
        Reflection.ensureNativeAccess(Reflection.getCallerClass());
        return SystemLookup.getInstance();
    }

    /**
     * Obtains a foreign method handle, with the given type and featuring the given function descriptor,
     * which can be used to call a target foreign function at the given address.
     * <p>
     * If the provided method type's return type is {@code MemorySegment}, then the resulting method handle features
     * an additional prefix parameter, of type {@link SegmentAllocator}, which will be used by the linker runtime
     * to allocate structs returned by-value.
     *
     * @param symbol   downcall symbol.
     * @param type     the method type.
     * @param function the function descriptor.
     * @return the downcall method handle.
     * @throws IllegalArgumentException in the case of a method type and function descriptor mismatch, or if the symbol
     *                                  is {@link MemoryAddress#NULL}
     *
     * @see SymbolLookup
     */
    MethodHandle downcallHandle(Addressable symbol, MethodType type, FunctionDescriptor function);

    /**
     * Obtain a foreign method handle, with the given type and featuring the given function descriptor,
     * which can be used to call a target foreign function at the given address.
     * <p>
     * If the provided method type's return type is {@code MemorySegment}, then the provided allocator will be used by
     * the linker runtime to allocate structs returned by-value.
     *
     * @param symbol    downcall symbol.
     * @param allocator the segment allocator.
     * @param type      the method type.
     * @param function  the function descriptor.
     * @return the downcall method handle.
     * @throws IllegalArgumentException in the case of a method type and function descriptor mismatch, or if the symbol
     *                                  is {@link MemoryAddress#NULL}
     *
     * @see SymbolLookup
     */
    MethodHandle downcallHandle(Addressable symbol, SegmentAllocator allocator, MethodType type, FunctionDescriptor function);

    /**
     * Obtains a foreign method handle, with the given type and featuring the given function descriptor, which can be
     * used to call a target foreign function at an address.
     * The resulting method handle features a prefix parameter (as the first parameter) corresponding to the address, of
     * type {@link Addressable}.
     * <p>
     * If the provided method type's return type is {@code MemorySegment}, then the resulting method handle features an
     * additional prefix parameter (inserted immediately after the address parameter), of type {@link SegmentAllocator}),
     * which will be used by the linker runtime to allocate structs returned by-value.
     * <p>
     * The returned method handle will throw an {@link IllegalArgumentException} if the target address passed to it is
     * {@link MemoryAddress#NULL}, or a {@link NullPointerException} if the target address is {@code null}.
     *
     * @param type     the method type.
     * @param function the function descriptor.
     * @return the downcall method handle.
     * @throws IllegalArgumentException in the case of a method type and function descriptor mismatch.
     *
     * @see SymbolLookup
     */
    MethodHandle downcallHandle(MethodType type, FunctionDescriptor function);

    /**
     * Allocates a native stub with given scope which can be passed to other foreign functions (as a function pointer);
     * calling such a function pointer from native code will result in the execution of the provided method handle.
     *
     * <p>
     * The returned memory address is associated with the provided scope. When such scope is closed,
     * the corresponding native stub will be deallocated.
     * <p>
     * The target method handle should not throw any exceptions. If the target method handle does throw an exception,
     * the VM will exit with a non-zero exit code. To avoid the VM aborting due to an uncaught exception, clients
     * could wrap all code in the target method handle in a try/catch block that catches any {@link Throwable}, for
     * instance by using the {@link java.lang.invoke.MethodHandles#catchException(MethodHandle, Class, MethodHandle)}
     * method handle combinator, and handle exceptions as desired in the corresponding catch block.
     *
     * @param target   the target method handle.
     * @param function the function descriptor.
     * @param scope the upcall stub scope.
     * @return the native stub segment.
     * @throws IllegalArgumentException if the target's method type and the function descriptor mismatch.
     * @throws IllegalStateException if {@code scope} has been already closed, or if access occurs from a thread other
     * than the thread owning {@code scope}.
     */
    MemoryAddress upcallStub(MethodHandle target, FunctionDescriptor function, ResourceScope scope);

    /**
     * The layout for the {@code char} C type
     */
    ValueLayout C_CHAR = pick(SysV.C_CHAR, Win64.C_CHAR, AArch64.C_CHAR);
    /**
     * The layout for the {@code short} C type
     */
    ValueLayout C_SHORT = pick(SysV.C_SHORT, Win64.C_SHORT, AArch64.C_SHORT);
    /**
     * The layout for the {@code int} C type
     */
    ValueLayout C_INT = pick(SysV.C_INT, Win64.C_INT, AArch64.C_INT);
    /**
     * The layout for the {@code long} C type
     */
    ValueLayout C_LONG = pick(SysV.C_LONG, Win64.C_LONG, AArch64.C_LONG);
    /**
     * The layout for the {@code long long} C type.
     */
    ValueLayout C_LONG_LONG = pick(SysV.C_LONG_LONG, Win64.C_LONG_LONG, AArch64.C_LONG_LONG);
    /**
     * The layout for the {@code float} C type
     */
    ValueLayout C_FLOAT = pick(SysV.C_FLOAT, Win64.C_FLOAT, AArch64.C_FLOAT);
    /**
     * The layout for the {@code double} C type
     */
    ValueLayout C_DOUBLE = pick(SysV.C_DOUBLE, Win64.C_DOUBLE, AArch64.C_DOUBLE);
    /**
     * The {@code T*} native type.
     */
    ValueLayout C_POINTER = pick(SysV.C_POINTER, Win64.C_POINTER, AArch64.C_POINTER);
    /**
     * The layout for the {@code va_list} C type
     */
    MemoryLayout C_VA_LIST = pick(SysV.C_VA_LIST, Win64.C_VA_LIST, AArch64.C_VA_LIST);

    /**
     * Returns a memory layout that is suitable to use as the layout for variadic arguments in a specialized
     * function descriptor.
     * @param <T> the memory layout type
     * @param layout the layout the adapt
     * @return a potentially newly created layout with the right attributes
     */
    @SuppressWarnings("unchecked")
    static <T extends MemoryLayout> T asVarArg(T layout) {
        Objects.requireNonNull(layout);
        return (T) PlatformLayouts.asVarArg(layout);
    }

    /**
     * Converts a Java string into a UTF-8 encoded, null-terminated C string,
     * storing the result into a native memory segment allocated using the provided allocator.
     * <p>
     * This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement byte array.  The
     * {@link java.nio.charset.CharsetEncoder} class should be used when more
     * control over the encoding process is required.
     *
     * @param str the Java string to be converted into a C string.
     * @param allocator the allocator to be used for the native segment allocation.
     * @return a new native memory segment containing the converted C string.
     */
    static MemorySegment toCString(String str, SegmentAllocator allocator) {
        Objects.requireNonNull(str);
        Objects.requireNonNull(allocator);
        return toCString(str.getBytes(StandardCharsets.UTF_8), allocator);
    }

    /**
     * Converts a Java string into a UTF-8 encoded, null-terminated C string,
     * storing the result into a native memory segment associated with the provided resource scope.
     * <p>
     * This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement byte array.  The
     * {@link java.nio.charset.CharsetEncoder} class should be used when more
     * control over the encoding process is required.
     *
     * @param str the Java string to be converted into a C string.
     * @param scope the resource scope to be associated with the returned segment.
     * @return a new native memory segment containing the converted C string.
     * @throws IllegalStateException if {@code scope} has been already closed, or if access occurs from a thread other
     * than the thread owning {@code scope}.
     */
    static MemorySegment toCString(String str, ResourceScope scope) {
        return toCString(str, SegmentAllocator.ofScope(scope));
    }

    /**
     * Converts a UTF-8 encoded, null-terminated C string stored at given address into a Java string.
     * <p>
     * This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     * <p>
     * This method is <a href="package-summary.html#restricted"><em>restricted</em></a>.
     * Restricted methods are unsafe, and, if used incorrectly, their use might crash
     * the JVM or, worse, silently result in memory corruption. Thus, clients should refrain from depending on
     * restricted methods, and use safe and supported functionalities, where possible.
     *
     * @param addr the address at which the string is stored.
     * @return a Java string with the contents of the null-terminated C string at given address.
     * @throws IllegalArgumentException if the size of the native string is greater than the largest string supported by the platform,
     * or if {@code addr == MemoryAddress.NULL}.
     * @throws IllegalCallerException if access to this method occurs from a module {@code M} and the command line option
     * {@code --enable-native-access} is either absent, or does not mention the module name {@code M}, or
     * {@code ALL-UNNAMED} in case {@code M} is an unnamed module.
     */
    @CallerSensitive
    static String toJavaString(MemoryAddress addr) {
        Reflection.ensureNativeAccess(Reflection.getCallerClass());
        SharedUtils.checkAddress(addr);
        return SharedUtils.toJavaStringInternal(NativeMemorySegmentImpl.EVERYTHING, addr.toRawLongValue());
    }

    /**
     * Converts a UTF-8 encoded, null-terminated C string stored at given address into a Java string.
     * <p>
     * This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     * @param addr the address at which the string is stored.
     * @return a Java string with the contents of the null-terminated C string at given address.
     * @throws IllegalArgumentException if the size of the native string is greater than the largest string supported by the platform.
     * @throws IllegalStateException if the size of the native string is greater than the size of the segment
     * associated with {@code addr}, or if {@code addr} is associated with a segment that is <em>not alive</em>.
     */
    static String toJavaString(MemorySegment addr) {
        Objects.requireNonNull(addr);
        return SharedUtils.toJavaStringInternal(addr, 0L);
    }

    private static void copy(MemorySegment addr, byte[] bytes) {
        var heapSegment = MemorySegment.ofArray(bytes);
        addr.copyFrom(heapSegment);
        MemoryAccess.setByteAtOffset(addr, bytes.length, (byte)0);
    }

    private static MemorySegment toCString(byte[] bytes, SegmentAllocator allocator) {
        MemorySegment addr = allocator.allocate(bytes.length + 1, 1L);
        copy(addr, bytes);
        return addr;
    }

    /**
     * Allocates memory of given size using malloc.
     * <p>
     * This method is <a href="package-summary.html#restricted"><em>restricted</em></a>.
     * Restricted methods are unsafe, and, if used incorrectly, their use might crash
     * the JVM or, worse, silently result in memory corruption. Thus, clients should refrain from depending on
     * restricted methods, and use safe and supported functionalities, where possible.
     *
     * @param size memory size to be allocated
     * @return addr memory address of the allocated memory
     * @throws OutOfMemoryError if malloc could not allocate the required amount of native memory.
     * @throws IllegalCallerException if access to this method occurs from a module {@code M} and the command line option
     * {@code --enable-native-access} is either absent, or does not mention the module name {@code M}, or
     * {@code ALL-UNNAMED} in case {@code M} is an unnamed module.
     */
    @CallerSensitive
    static MemoryAddress allocateMemory(long size) {
        Reflection.ensureNativeAccess(Reflection.getCallerClass());
        MemoryAddress addr = SharedUtils.allocateMemoryInternal(size);
        if (addr.equals(MemoryAddress.NULL)) {
            throw new OutOfMemoryError();
        } else {
            return addr;
        }
    }

    /**
     * Frees the memory pointed by the given memory address.
     * <p>
     * This method is <a href="package-summary.html#restricted"><em>restricted</em></a>.
     * Restricted methods are unsafe, and, if used incorrectly, their use might crash
     * the JVM or, worse, silently result in memory corruption. Thus, clients should refrain from depending on
     * restricted methods, and use safe and supported functionalities, where possible.
     *
     * @param addr memory address of the native memory to be freed
     * @throws IllegalCallerException if access to this method occurs from a module {@code M} and the command line option
     * @throws IllegalArgumentException if {@code addr == MemoryAddress.NULL}.
     * {@code --enable-native-access} is either absent, or does not mention the module name {@code M}, or
     * {@code ALL-UNNAMED} in case {@code M} is an unnamed module.
     */
    @CallerSensitive
    static void freeMemory(MemoryAddress addr) {
        Reflection.ensureNativeAccess(Reflection.getCallerClass());
        SharedUtils.checkAddress(addr);
        SharedUtils.freeMemoryInternal(addr);
    }

    /**
     * An interface that models a C {@code va_list}.
     * <p>
     * A va list is a stateful cursor used to iterate over a set of variadic arguments.
     * <p>
     * Per the C specification (see C standard 6.5.2.2 Function calls - item 6),
     * arguments to variadic calls are erased by way of 'default argument promotions',
     * which erases integral types by way of integer promotion (see C standard 6.3.1.1 - item 2),
     * and which erases all {@code float} arguments to {@code double}.
     * <p>
     * As such, this interface only supports reading {@code int}, {@code double},
     * and any other type that fits into a {@code long}.
     *
     * <p> Unless otherwise specified, passing a {@code null} argument, or an array argument containing one or more {@code null}
     * elements to a method in this class causes a {@link NullPointerException NullPointerException} to be thrown. </p>
     */
    sealed interface VaList extends Addressable permits WinVaList, SysVVaList, LinuxAArch64VaList, MacOsAArch64VaList, SharedUtils.EmptyVaList {

        /**
         * Reads the next value as an {@code int} and advances this va list's position.
         *
         * @param layout the layout of the value
         * @return the value read as an {@code int}
         * @throws IllegalStateException if the resource scope associated with this instance has been closed
         * (see {@link #scope()}).
         * @throws IllegalArgumentException if the given memory layout is not compatible with {@code int}
         */
        int vargAsInt(MemoryLayout layout);

        /**
         * Reads the next value as a {@code long} and advances this va list's position.
         *
         * @param layout the layout of the value
         * @return the value read as an {@code long}
         * @throws IllegalStateException if the resource scope associated with this instance has been closed
         * (see {@link #scope()}).
         * @throws IllegalArgumentException if the given memory layout is not compatible with {@code long}
         */
        long vargAsLong(MemoryLayout layout);

        /**
         * Reads the next value as a {@code double} and advances this va list's position.
         *
         * @param layout the layout of the value
         * @return the value read as an {@code double}
         * @throws IllegalStateException if the resource scope associated with this instance has been closed
         * (see {@link #scope()}).
         * @throws IllegalArgumentException if the given memory layout is not compatible with {@code double}
         */
        double vargAsDouble(MemoryLayout layout);

        /**
         * Reads the next value as a {@code MemoryAddress} and advances this va list's position.
         *
         * @param layout the layout of the value
         * @return the value read as an {@code MemoryAddress}
         * @throws IllegalStateException if the resource scope associated with this instance has been closed
         * (see {@link #scope()}).
         * @throws IllegalArgumentException if the given memory layout is not compatible with {@code MemoryAddress}
         */
        MemoryAddress vargAsAddress(MemoryLayout layout);

        /**
         * Reads the next value as a {@code MemorySegment}, and advances this va list's position.
         * <p>
         * The memory segment returned by this method will be allocated using the given {@link SegmentAllocator}.
         *
         * @param layout the layout of the value
         * @param allocator the allocator to be used for the native segment allocation
         * @return the value read as an {@code MemorySegment}
         * @throws IllegalStateException if the resource scope associated with this instance has been closed
         * (see {@link #scope()}).
         * @throws IllegalArgumentException if the given memory layout is not compatible with {@code MemorySegment}
         */
        MemorySegment vargAsSegment(MemoryLayout layout, SegmentAllocator allocator);

        /**
         * Reads the next value as a {@code MemorySegment}, and advances this va list's position.
         * <p>
         * The memory segment returned by this method will be associated with the given {@link ResourceScope}.
         *
         * @param layout the layout of the value
         * @param scope the resource scope to be associated with the returned segment
         * @return the value read as an {@code MemorySegment}
         * @throws IllegalStateException if the resource scope associated with this instance has been closed
         * (see {@link #scope()}).
         * @throws IllegalArgumentException if the given memory layout is not compatible with {@code MemorySegment}
         * @throws IllegalStateException if {@code scope} has been already closed, or if access occurs from a thread other
         * than the thread owning {@code scope}.
         */
        MemorySegment vargAsSegment(MemoryLayout layout, ResourceScope scope);

        /**
         * Skips a number of elements with the given memory layouts, and advances this va list's position.
         *
         * @param layouts the layout of the value
         * @throws IllegalStateException if the resource scope associated with this instance has been closed
         * (see {@link #scope()}).
         */
        void skip(MemoryLayout... layouts);

        /**
         * Returns the resource scope associated with this instance.
         * @return the resource scope associated with this instance.
         */
        ResourceScope scope();

        /**
         * Copies this C {@code va_list} at its current position. Copying is useful to traverse the va list's elements
         * starting from the current position, without affecting the state of the original va list, essentially
         * allowing the elements to be traversed multiple times.
         * <p>
         * Any native resource required by the execution of this method will be allocated in the resource scope
         * associated with this instance (see {@link #scope()}).
         * <p>
         * This method only copies the va list cursor itself and not the memory that may be attached to the
         * va list which holds its elements. That means that if this va list was created with the
         * {@link #make(Consumer, ResourceScope)} method, closing this va list will also release the native memory that holds its
         * elements, making the copy unusable.
         *
         * @return a copy of this C {@code va_list}.
         * @throws IllegalStateException if the resource scope associated with this instance has been closed
         * (see {@link #scope()}).
         */
        VaList copy();

        /**
         * Returns the memory address of the C {@code va_list} associated with this instance.
         * The returned memory address is associated with same resource scope as that associated with this instance.
         *
         * @return the memory address of the C {@code va_list} associated with this instance.
         */
        @Override
        MemoryAddress address();

        /**
         * Constructs a new {@code VaList} instance out of a memory address pointing to an existing C {@code va_list},
         * backed by the {@linkplain ResourceScope#globalScope() global} resource scope.
         * <p>
         * This method is <a href="package-summary.html#restricted"><em>restricted</em></a>.
         * Restricted methods are unsafe, and, if used incorrectly, their use might crash
         * the JVM or, worse, silently result in memory corruption. Thus, clients should refrain from depending on
         * restricted methods, and use safe and supported functionalities, where possible.
         *
         * @param address a memory address pointing to an existing C {@code va_list}.
         * @return a new {@code VaList} instance backed by the C {@code va_list} at {@code address}.
         * @throws IllegalCallerException if access to this method occurs from a module {@code M} and the command line option
         * {@code --enable-native-access} is either absent, or does not mention the module name {@code M}, or
         * {@code ALL-UNNAMED} in case {@code M} is an unnamed module.
         */
        @CallerSensitive
        static VaList ofAddress(MemoryAddress address) {
            Reflection.ensureNativeAccess(Reflection.getCallerClass());
            return SharedUtils.newVaListOfAddress(address, ResourceScope.globalScope());
        }

        /**
         * Constructs a new {@code VaList} instance out of a memory address pointing to an existing C {@code va_list},
         * with given resource scope.
         * <p>
         * This method is <a href="package-summary.html#restricted"><em>restricted</em></a>.
         * Restricted methods are unsafe, and, if used incorrectly, their use might crash
         * the JVM or, worse, silently result in memory corruption. Thus, clients should refrain from depending on
         * restricted methods, and use safe and supported functionalities, where possible.
         *
         * @param address a memory address pointing to an existing C {@code va_list}.
         * @param scope the resource scope to be associated with the returned {@code VaList} instance.
         * @return a new {@code VaList} instance backed by the C {@code va_list} at {@code address}.
         * @throws IllegalStateException if {@code scope} has been already closed, or if access occurs from a thread other
         * than the thread owning {@code scope}.
         * @throws IllegalCallerException if access to this method occurs from a module {@code M} and the command line option
         * {@code --enable-native-access} is either absent, or does not mention the module name {@code M}, or
         * {@code ALL-UNNAMED} in case {@code M} is an unnamed module.
         */
        @CallerSensitive
        static VaList ofAddress(MemoryAddress address, ResourceScope scope) {
            Reflection.ensureNativeAccess(Reflection.getCallerClass());
            Objects.requireNonNull(address);
            Objects.requireNonNull(scope);
            return SharedUtils.newVaListOfAddress(address, scope);
        }

        /**
         * Constructs a new {@code VaList} using a builder (see {@link Builder}), associated with a given
         * {@linkplain ResourceScope resource scope}.
         * <p>
         * If this method needs to allocate native memory, such memory will be managed by the given
         * {@linkplain ResourceScope resource scope}, and will be released when the resource scope is {@linkplain ResourceScope#close closed}.
         * <p>
         * Note that when there are no elements added to the created va list,
         * this method will return the same as {@link #empty()}.
         *
         * @param actions a consumer for a builder (see {@link Builder}) which can be used to specify the elements
         *                of the underlying C {@code va_list}.
         * @param scope the scope to be used for the valist allocation.
         * @return a new {@code VaList} instance backed by a fresh C {@code va_list}.
         * @throws IllegalStateException if {@code scope} has been already closed, or if access occurs from a thread other
         * than the thread owning {@code scope}.
         */
        static VaList make(Consumer<Builder> actions, ResourceScope scope) {
            Objects.requireNonNull(actions);
            Objects.requireNonNull(scope);
            return SharedUtils.newVaList(actions, scope);
        }

        /**
         * Returns an empty C {@code va_list} constant.
         * <p>
         * The returned {@code VaList} can not be closed.
         *
         * @return a {@code VaList} modelling an empty C {@code va_list}.
         */
        static VaList empty() {
            return SharedUtils.emptyVaList();
        }

        /**
         * A builder interface used to construct a C {@code va_list}.
         *
         * <p> Unless otherwise specified, passing a {@code null} argument, or an array argument containing one or more {@code null}
         * elements to a method in this class causes a {@link NullPointerException NullPointerException} to be thrown. </p>
         */
        sealed interface Builder permits WinVaList.Builder, SysVVaList.Builder, LinuxAArch64VaList.Builder, MacOsAArch64VaList.Builder {

            /**
             * Adds a native value represented as an {@code int} to the C {@code va_list} being constructed.
             *
             * @param layout the native layout of the value.
             * @param value the value, represented as an {@code int}.
             * @return this builder.
             * @throws IllegalArgumentException if the given memory layout is not compatible with {@code int}
             */
            Builder vargFromInt(ValueLayout layout, int value);

            /**
             * Adds a native value represented as a {@code long} to the C {@code va_list} being constructed.
             *
             * @param layout the native layout of the value.
             * @param value the value, represented as a {@code long}.
             * @return this builder.
             * @throws IllegalArgumentException if the given memory layout is not compatible with {@code long}
             */
            Builder vargFromLong(ValueLayout layout, long value);

            /**
             * Adds a native value represented as a {@code double} to the C {@code va_list} being constructed.
             *
             * @param layout the native layout of the value.
             * @param value the value, represented as a {@code double}.
             * @return this builder.
             * @throws IllegalArgumentException if the given memory layout is not compatible with {@code double}
             */
            Builder vargFromDouble(ValueLayout layout, double value);

            /**
             * Adds a native value represented as a {@code MemoryAddress} to the C {@code va_list} being constructed.
             *
             * @param layout the native layout of the value.
             * @param value the value, represented as a {@code Addressable}.
             * @return this builder.
             * @throws IllegalArgumentException if the given memory layout is not compatible with {@code MemoryAddress}
             */
            Builder vargFromAddress(ValueLayout layout, Addressable value);

            /**
             * Adds a native value represented as a {@code MemorySegment} to the C {@code va_list} being constructed.
             *
             * @param layout the native layout of the value.
             * @param value the value, represented as a {@code MemorySegment}.
             * @return this builder.
             * @throws IllegalArgumentException if the given memory layout is not compatible with {@code MemorySegment}
             */
            Builder vargFromSegment(GroupLayout layout, MemorySegment value);
        }
    }

    /**
     * A C type kind. Each kind corresponds to a particular C language builtin type, and can be attached to
     * {@link ValueLayout} instances using the {@link MemoryLayout#withAttribute(String, Constable)} in order
     * to obtain a layout which can be classified accordingly by {@link CLinker#downcallHandle(Addressable, MethodType, FunctionDescriptor)}
     * and {@link CLinker#upcallStub(MethodHandle, FunctionDescriptor, ResourceScope)}.
     */
    enum TypeKind {
        /**
         * A kind corresponding to the <em>integral</em> C {@code char} type
         */
        CHAR(true),
        /**
         * A kind corresponding to the <em>integral</em> C {@code short} type
         */
        SHORT(true),
        /**
         * A kind corresponding to the <em>integral</em> C {@code int} type
         */
        INT(true),
        /**
         * A kind corresponding to the <em>integral</em> C {@code long} type
         */
        LONG(true),
        /**
         * A kind corresponding to the <em>integral</em> C {@code long long} type
         */
        LONG_LONG(true),
        /**
         * A kind corresponding to the <em>floating-point</em> C {@code float} type
         */
        FLOAT(false),
        /**
         * A kind corresponding to the <em>floating-point</em> C {@code double} type
         */
        DOUBLE(false),
        /**
         * A kind corresponding to the an <em>integral</em> C pointer type
         */
        POINTER(false);

        private final boolean isIntegral;

        TypeKind(boolean isIntegral) {
            this.isIntegral = isIntegral;
        }

        /**
         * Is this kind integral?
         *
         * @return true if this kind is integral
         */
        public boolean isIntegral() {
            return isIntegral;
        }

        /**
         * Is this kind a floating point type?
         *
         * @return true if this kind is a floating point type
         */
        public boolean isFloat() {
            return !isIntegral() && !isPointer();
        }

        /**
         * Is this kind a pointer kind?
         *
         * @return true if this kind is a pointer kind
         */
        public boolean isPointer() {
            return this == POINTER;
        }

        /**
         * The layout attribute name associated with this classification kind. Clients can retrieve the type kind
         * of a layout using the following code:
         * <blockquote><pre>{@code
        ValueLayout layout = ...
        TypeKind = layout.attribute(TypeKind.ATTR_NAME).orElse(null);
         * }</pre></blockquote>
         */
        public static final String ATTR_NAME = "abi/kind";
    }
}
