/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.vm.ci.hotspot;

import static jdk.vm.ci.common.InitTimer.timer;
import static jdk.vm.ci.hotspot.HotSpotJVMCIRuntime.runtime;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.InvalidInstalledCodeException;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.stack.InspectedFrameVisitor;
import jdk.vm.ci.common.InitTimer;
import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * Calls from Java into HotSpot. The behavior of all the methods in this class that take a native
 * pointer as an argument (e.g., {@link #getSymbol(long)}) is undefined if the argument does not
 * denote a valid native object.
 */
final class CompilerToVM {
    /**
     * Initializes the native part of the JVMCI runtime.
     */
    private static native void registerNatives();

    /**
     * These values mirror the equivalent values from {@code Unsafe} but are appropriate for the JVM
     * being compiled against.
     */
    // Checkstyle: stop
    final int ARRAY_BOOLEAN_BASE_OFFSET;
    final int ARRAY_BYTE_BASE_OFFSET;
    final int ARRAY_SHORT_BASE_OFFSET;
    final int ARRAY_CHAR_BASE_OFFSET;
    final int ARRAY_INT_BASE_OFFSET;
    final int ARRAY_LONG_BASE_OFFSET;
    final int ARRAY_FLOAT_BASE_OFFSET;
    final int ARRAY_DOUBLE_BASE_OFFSET;
    final int ARRAY_OBJECT_BASE_OFFSET;
    final int ARRAY_BOOLEAN_INDEX_SCALE;
    final int ARRAY_BYTE_INDEX_SCALE;
    final int ARRAY_SHORT_INDEX_SCALE;
    final int ARRAY_CHAR_INDEX_SCALE;
    final int ARRAY_INT_INDEX_SCALE;
    final int ARRAY_LONG_INDEX_SCALE;
    final int ARRAY_FLOAT_INDEX_SCALE;
    final int ARRAY_DOUBLE_INDEX_SCALE;
    final int ARRAY_OBJECT_INDEX_SCALE;
    // Checkstyle: resume

    @SuppressWarnings("try")
    CompilerToVM() {
        try (InitTimer t = timer("CompilerToVM.registerNatives")) {
            registerNatives();
            ARRAY_BOOLEAN_BASE_OFFSET = arrayBaseOffset(JavaKind.Boolean);
            ARRAY_BYTE_BASE_OFFSET = arrayBaseOffset(JavaKind.Byte);
            ARRAY_SHORT_BASE_OFFSET = arrayBaseOffset(JavaKind.Short);
            ARRAY_CHAR_BASE_OFFSET = arrayBaseOffset(JavaKind.Char);
            ARRAY_INT_BASE_OFFSET = arrayBaseOffset(JavaKind.Int);
            ARRAY_LONG_BASE_OFFSET = arrayBaseOffset(JavaKind.Long);
            ARRAY_FLOAT_BASE_OFFSET = arrayBaseOffset(JavaKind.Float);
            ARRAY_DOUBLE_BASE_OFFSET = arrayBaseOffset(JavaKind.Double);
            ARRAY_OBJECT_BASE_OFFSET = arrayBaseOffset(JavaKind.Object);
            ARRAY_BOOLEAN_INDEX_SCALE = arrayIndexScale(JavaKind.Boolean);
            ARRAY_BYTE_INDEX_SCALE = arrayIndexScale(JavaKind.Byte);
            ARRAY_SHORT_INDEX_SCALE = arrayIndexScale(JavaKind.Short);
            ARRAY_CHAR_INDEX_SCALE = arrayIndexScale(JavaKind.Char);
            ARRAY_INT_INDEX_SCALE = arrayIndexScale(JavaKind.Int);
            ARRAY_LONG_INDEX_SCALE = arrayIndexScale(JavaKind.Long);
            ARRAY_FLOAT_INDEX_SCALE = arrayIndexScale(JavaKind.Float);
            ARRAY_DOUBLE_INDEX_SCALE = arrayIndexScale(JavaKind.Double);
            ARRAY_OBJECT_INDEX_SCALE = arrayIndexScale(JavaKind.Object);
        }
    }

    native int arrayBaseOffset(JavaKind kind);

    native int arrayIndexScale(JavaKind kind);

    /**
     * Gets the {@link CompilerToVM} instance associated with the singleton
     * {@link HotSpotJVMCIRuntime} instance.
     */
    public static CompilerToVM compilerToVM() {
        return runtime().getCompilerToVM();
    }

    /**
     * Copies the original bytecode of {@code method} into a new byte array and returns it.
     *
     * @return a new byte array containing the original bytecode of {@code method}
     */
    native byte[] getBytecode(HotSpotResolvedJavaMethodImpl method);

    /**
     * Gets the number of entries in {@code method}'s exception handler table or 0 if it has no
     * exception handler table.
     */
    native int getExceptionTableLength(HotSpotResolvedJavaMethodImpl method);

    /**
     * Gets the address of the first entry in {@code method}'s exception handler table.
     *
     * Each entry is a native object described by these fields:
     *
     * <ul>
     * <li>{@link HotSpotVMConfig#exceptionTableElementSize}</li>
     * <li>{@link HotSpotVMConfig#exceptionTableElementStartPcOffset}</li>
     * <li>{@link HotSpotVMConfig#exceptionTableElementEndPcOffset}</li>
     * <li>{@link HotSpotVMConfig#exceptionTableElementHandlerPcOffset}</li>
     * <li>{@link HotSpotVMConfig#exceptionTableElementCatchTypeIndexOffset}
     * </ul>
     *
     * @return 0 if {@code method} has no exception handlers (i.e.
     *         {@code getExceptionTableLength(method) == 0})
     */
    native long getExceptionTableStart(HotSpotResolvedJavaMethodImpl method);

    /**
     * Determines whether {@code method} is currently compilable by the JVMCI compiler being used by
     * the VM. This can return false if JVMCI compilation failed earlier for {@code method}, a
     * breakpoint is currently set in {@code method} or {@code method} contains other bytecode
     * features that require special handling by the VM.
     */
    native boolean isCompilable(HotSpotResolvedJavaMethodImpl method);

    /**
     * Determines if {@code method} is targeted by a VM directive (e.g.,
     * {@code -XX:CompileCommand=dontinline,<pattern>}) or annotation (e.g.,
     * {@code jdk.internal.vm.annotation.DontInline}) that specifies it should not be inlined.
     */
    native boolean hasNeverInlineDirective(HotSpotResolvedJavaMethodImpl method);

    /**
     * Determines if {@code method} should be inlined at any cost. This could be because:
     * <ul>
     * <li>a CompileOracle directive may forces inlining of this methods</li>
     * <li>an annotation forces inlining of this method</li>
     * </ul>
     */
    native boolean shouldInlineMethod(HotSpotResolvedJavaMethodImpl method);

    /**
     * Used to implement {@link ResolvedJavaType#findUniqueConcreteMethod(ResolvedJavaMethod)}.
     *
     * @param method the method on which to base the search
     * @param actualHolderType the best known type of receiver
     * @return the method result or 0 is there is no unique concrete method for {@code method}
     */
    native HotSpotResolvedJavaMethodImpl findUniqueConcreteMethod(HotSpotResolvedObjectTypeImpl actualHolderType, HotSpotResolvedJavaMethodImpl method);

    /**
     * Gets the implementor for the interface class {@code type}.
     *
     * @return the implementor if there is a single implementor, {@code null} if there is no
     *         implementor, or {@code type} itself if there is more than one implementor
     * @throws IllegalArgumentException if type is not an interface type
     */
    native HotSpotResolvedObjectTypeImpl getImplementor(HotSpotResolvedObjectTypeImpl type);

    /**
     * Determines if {@code method} is ignored by security stack walks.
     */
    native boolean methodIsIgnoredBySecurityStackWalk(HotSpotResolvedJavaMethodImpl method);

    /**
     * Converts a name to a type.
     *
     * @param name a well formed Java type in {@linkplain JavaType#getName() internal} format
     * @param accessingClass the context of resolution. A value of {@code null} implies that the
     *            class should be resolved with the class loader.
     * @param resolve force resolution to a {@link ResolvedJavaType}. If true, this method will
     *            either return a {@link ResolvedJavaType} or throw an exception
     * @return the type for {@code name} or 0 if resolution failed and {@code resolve == false}
     * @throws ClassNotFoundException if {@code resolve == true} and the resolution failed
     */
    native HotSpotResolvedJavaType lookupType(String name, HotSpotResolvedObjectTypeImpl accessingClass, boolean resolve) throws ClassNotFoundException;

    native HotSpotResolvedJavaType lookupClass(Class<?> javaClass);

    /**
     * Resolves the entry at index {@code cpi} in {@code constantPool} to an object, looking in the
     * constant pool cache first.
     *
     * The behavior of this method is undefined if {@code cpi} does not denote one of the following
     * entry types: {@code JVM_CONSTANT_Dynamic}, {@code JVM_CONSTANT_String},
     * {@code JVM_CONSTANT_MethodHandle}, {@code JVM_CONSTANT_MethodHandleInError},
     * {@code JVM_CONSTANT_MethodType} and {@code JVM_CONSTANT_MethodTypeInError}.
     */
    native JavaConstant resolvePossiblyCachedConstantInPool(HotSpotConstantPool constantPool, int cpi);

    /**
     * Gets the {@code JVM_CONSTANT_NameAndType} index from the entry at index {@code cpi} in
     * {@code constantPool}.
     *
     * The behavior of this method is undefined if {@code cpi} does not denote an entry containing a
     * {@code JVM_CONSTANT_NameAndType} index.
     */
    native int lookupNameAndTypeRefIndexInPool(HotSpotConstantPool constantPool, int cpi);

    /**
     * Gets the name of the {@code JVM_CONSTANT_NameAndType} entry referenced by another entry
     * denoted by {@code which} in {@code constantPool}.
     *
     * The behavior of this method is undefined if {@code which} does not denote a entry that
     * references a {@code JVM_CONSTANT_NameAndType} entry.
     */
    native String lookupNameInPool(HotSpotConstantPool constantPool, int which);

    /**
     * Gets the signature of the {@code JVM_CONSTANT_NameAndType} entry referenced by another entry
     * denoted by {@code which} in {@code constantPool}.
     *
     * The behavior of this method is undefined if {@code which} does not denote a entry that
     * references a {@code JVM_CONSTANT_NameAndType} entry.
     */
    native String lookupSignatureInPool(HotSpotConstantPool constantPool, int which);

    /**
     * Gets the {@code JVM_CONSTANT_Class} index from the entry at index {@code cpi} in
     * {@code constantPool}.
     *
     * The behavior of this method is undefined if {@code cpi} does not denote an entry containing a
     * {@code JVM_CONSTANT_Class} index.
     */
    native int lookupKlassRefIndexInPool(HotSpotConstantPool constantPool, int cpi);

    /**
     * Looks up a class denoted by the {@code JVM_CONSTANT_Class} entry at index {@code cpi} in
     * {@code constantPool}. This method does not perform any resolution.
     *
     * The behavior of this method is undefined if {@code cpi} does not denote a
     * {@code JVM_CONSTANT_Class} entry.
     *
     * @return the resolved class entry or a String otherwise
     */
    native Object lookupKlassInPool(HotSpotConstantPool constantPool, int cpi);

    /**
     * Looks up a method denoted by the entry at index {@code cpi} in {@code constantPool}. This
     * method does not perform any resolution.
     *
     * The behavior of this method is undefined if {@code cpi} does not denote an entry representing
     * a method.
     *
     * @param opcode the opcode of the instruction for which the lookup is being performed or
     *            {@code -1}. If non-negative, then resolution checks specific to the bytecode it
     *            denotes are performed if the method is already resolved. Should any of these
     *            checks fail, 0 is returned.
     * @return the resolved method entry, 0 otherwise
     */
    native HotSpotResolvedJavaMethodImpl lookupMethodInPool(HotSpotConstantPool constantPool, int cpi, byte opcode);

    // TODO resolving JVM_CONSTANT_Dynamic

    /**
     * Ensures that the type referenced by the specified {@code JVM_CONSTANT_InvokeDynamic} entry at
     * index {@code cpi} in {@code constantPool} is loaded and initialized.
     *
     * The behavior of this method is undefined if {@code cpi} does not denote a
     * {@code JVM_CONSTANT_InvokeDynamic} entry.
     */
    native void resolveInvokeDynamicInPool(HotSpotConstantPool constantPool, int cpi);

    /**
     * If {@code cpi} denotes an entry representing a
     * <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-2.html#jvms-2.9">signature
     * polymorphic</a> method, this method ensures that the type referenced by the entry is loaded
     * and initialized. It {@code cpi} does not denote a signature polymorphic method, this method
     * does nothing.
     */
    native void resolveInvokeHandleInPool(HotSpotConstantPool constantPool, int cpi);

    /**
     * If {@code cpi} denotes an entry representing a resolved dynamic adapter (see
     * {@link #resolveInvokeDynamicInPool} and {@link #resolveInvokeHandleInPool}), return the
     * opcode of the instruction for which the resolution was performed ({@code invokedynamic} or
     * {@code invokevirtual}), or {@code -1} otherwise.
     */
    native int isResolvedInvokeHandleInPool(HotSpotConstantPool constantPool, int cpi);

    /**
     * Gets the list of type names (in the format of {@link JavaType#getName()}) denoting the
     * classes that define signature polymorphic methods.
     */
    native String[] getSignaturePolymorphicHolders();

    /**
     * Gets the resolved type denoted by the entry at index {@code cpi} in {@code constantPool}.
     *
     * The behavior of this method is undefined if {@code cpi} does not denote an entry representing
     * a class.
     *
     * @throws LinkageError if resolution failed
     */
    native HotSpotResolvedObjectTypeImpl resolveTypeInPool(HotSpotConstantPool constantPool, int cpi) throws LinkageError;

    /**
     * Looks up and attempts to resolve the {@code JVM_CONSTANT_Field} entry for at index
     * {@code cpi} in {@code constantPool}. For some opcodes, checks are performed that require the
     * {@code method} that contains {@code opcode} to be specified. The values returned in
     * {@code info} are:
     *
     * <pre>
     *     [ flags,  // fieldDescriptor::access_flags()
     *       offset, // fieldDescriptor::offset()
     *       index   // fieldDescriptor::index()
     *     ]
     * </pre>
     *
     * The behavior of this method is undefined if {@code cpi} does not denote a
     * {@code JVM_CONSTANT_Field} entry.
     *
     * @param info an array in which the details of the field are returned
     * @return the type defining the field if resolution is successful, 0 otherwise
     */
    native HotSpotResolvedObjectTypeImpl resolveFieldInPool(HotSpotConstantPool constantPool, int cpi, HotSpotResolvedJavaMethodImpl method, byte opcode, int[] info);

    /**
     * Converts {@code cpci} from an index into the cache for {@code constantPool} to an index
     * directly into {@code constantPool}.
     *
     * The behavior of this method is undefined if {@code ccpi} is an invalid constant pool cache
     * index.
     */
    native int constantPoolRemapInstructionOperandFromCache(HotSpotConstantPool constantPool, int cpci);

    /**
     * Gets the appendix object (if any) associated with the entry at index {@code cpi} in
     * {@code constantPool}.
     */
    native HotSpotObjectConstantImpl lookupAppendixInPool(HotSpotConstantPool constantPool, int cpi);

    /**
     * Installs the result of a compilation into the code cache.
     *
     * @param target the target where this code should be installed
     * @param compiledCode the result of a compilation
     * @param code the details of the installed CodeBlob are written to this object
     * @return the outcome of the installation which will be one of
     *         {@link HotSpotVMConfig#codeInstallResultOk},
     *         {@link HotSpotVMConfig#codeInstallResultCacheFull},
     *         {@link HotSpotVMConfig#codeInstallResultCodeTooLarge} or
     *         {@link HotSpotVMConfig#codeInstallResultDependenciesFailed}.
     * @throws JVMCIError if there is something wrong with the compiled code or the associated
     *             metadata.
     */
    native int installCode(TargetDescription target, HotSpotCompiledCode compiledCode, InstalledCode code, long failedSpeculationsAddress, byte[] speculations);

    /**
     * Generates the VM metadata for some compiled code and copies them into {@code metaData}. This
     * method does not install anything into the code cache.
     *
     * @param target the target where this code would be installed
     * @param compiledCode the result of a compilation
     * @param metaData the metadata is written to this object
     * @return the outcome of the installation which will be one of
     *         {@link HotSpotVMConfig#codeInstallResultOk},
     *         {@link HotSpotVMConfig#codeInstallResultCacheFull},
     *         {@link HotSpotVMConfig#codeInstallResultCodeTooLarge} or
     *         {@link HotSpotVMConfig#codeInstallResultDependenciesFailed}.
     * @throws JVMCIError if there is something wrong with the compiled code or the metadata
     */
    native int getMetadata(TargetDescription target, HotSpotCompiledCode compiledCode, HotSpotMetaData metaData);

    /**
     * Resets all compilation statistics.
     */
    native void resetCompilationStatistics();

    /**
     * Reads the database of VM info. The return value encodes the info in a nested object array
     * that is described by the pseudo Java object {@code info} below:
     *
     * <pre>
     *     info = [
     *         VMField[] vmFields,
     *         [String name, Long size, ...] vmTypeSizes,
     *         [String name, Long value, ...] vmConstants,
     *         [String name, Long value, ...] vmAddresses,
     *         VMFlag[] vmFlags
     *         VMIntrinsicMethod[] vmIntrinsics
     *     ]
     * </pre>
     *
     * @return VM info as encoded above
     */
    native Object[] readConfiguration();

    /**
     * Resolves the implementation of {@code method} for virtual dispatches on objects of dynamic
     * type {@code exactReceiver}. This resolution process only searches "up" the class hierarchy of
     * {@code exactReceiver}.
     *
     * @param caller the caller or context type used to perform access checks
     * @return the link-time resolved method (might be abstract) or {@code null} if it is either a
     *         signature polymorphic method or can not be linked.
     */
    native HotSpotResolvedJavaMethodImpl resolveMethod(HotSpotResolvedObjectTypeImpl exactReceiver, HotSpotResolvedJavaMethodImpl method, HotSpotResolvedObjectTypeImpl caller);

    /**
     * Gets the static initializer of {@code type}.
     *
     * @return {@code null} if {@code type} has no static initializer
     */
    native HotSpotResolvedJavaMethodImpl getClassInitializer(HotSpotResolvedObjectTypeImpl type);

    /**
     * Determines if {@code type} or any of its currently loaded subclasses overrides
     * {@code Object.finalize()}.
     */
    native boolean hasFinalizableSubclass(HotSpotResolvedObjectTypeImpl type);

    /**
     * Gets the method corresponding to {@code executable}.
     */
    native HotSpotResolvedJavaMethodImpl asResolvedJavaMethod(Executable executable);

    /**
     * Gets the maximum absolute offset of a PC relative call to {@code address} from any position
     * in the code cache.
     *
     * @param address an address that may be called from any code in the code cache
     * @return -1 if {@code address == 0}
     */
    native long getMaxCallTargetOffset(long address);

    /**
     * Gets a textual disassembly of {@code codeBlob}.
     *
     * @return a non-zero length string containing a disassembly of {@code codeBlob} or null if
     *         {@code codeBlob} could not be disassembled for some reason
     */
    // The HotSpot disassembler seems not to be thread safe so it's better to synchronize its usage
    synchronized native String disassembleCodeBlob(InstalledCode installedCode);

    /**
     * Gets a stack trace element for {@code method} at bytecode index {@code bci}.
     */
    native StackTraceElement getStackTraceElement(HotSpotResolvedJavaMethodImpl method, int bci);

    /**
     * Executes some {@code installedCode} with arguments {@code args}.
     *
     * @return the result of executing {@code nmethodMirror}
     * @throws InvalidInstalledCodeException if {@code nmethodMirror} has been invalidated
     */
    native Object executeHotSpotNmethod(Object[] args, HotSpotNmethod nmethodMirror) throws InvalidInstalledCodeException;

    /**
     * Gets the line number table for {@code method}. The line number table is encoded as (bci,
     * source line number) pairs.
     *
     * @return the line number table for {@code method} or null if it doesn't have one
     */
    native long[] getLineNumberTable(HotSpotResolvedJavaMethodImpl method);

    /**
     * Gets the number of entries in the local variable table for {@code method}.
     *
     * @return the number of entries in the local variable table for {@code method}
     */
    native int getLocalVariableTableLength(HotSpotResolvedJavaMethodImpl method);

    /**
     * Gets the address of the first entry in the local variable table for {@code method}.
     *
     * Each entry is a native object described by these fields:
     *
     * <ul>
     * <li>{@link HotSpotVMConfig#localVariableTableElementSize}</li>
     * <li>{@link HotSpotVMConfig#localVariableTableElementLengthOffset}</li>
     * <li>{@link HotSpotVMConfig#localVariableTableElementNameCpIndexOffset}</li>
     * <li>{@link HotSpotVMConfig#localVariableTableElementDescriptorCpIndexOffset}</li>
     * <li>{@link HotSpotVMConfig#localVariableTableElementSlotOffset}
     * <li>{@link HotSpotVMConfig#localVariableTableElementStartBciOffset}
     * </ul>
     *
     * @return 0 if {@code method} does not have a local variable table
     */
    native long getLocalVariableTableStart(HotSpotResolvedJavaMethodImpl method);

    /**
     * Sets flags on {@code method} indicating that it should never be inlined or compiled by the
     * VM.
     */
    native void setNotInlinableOrCompilable(HotSpotResolvedJavaMethodImpl method);

    /**
     * Invalidates the profiling information for {@code method} and (re)initializes it such that
     * profiling restarts upon its next invocation.
     */
    native void reprofile(HotSpotResolvedJavaMethodImpl method);

    /**
     * Invalidates {@code nmethodMirror} such that {@link InvalidInstalledCodeException} will be
     * raised the next time {@code nmethodMirror} is {@linkplain #executeHotSpotNmethod executed}.
     * The {@code nmethod} associated with {@code nmethodMirror} is also made non-entrant and any
     * current activations of the {@code nmethod} are deoptimized.
     */
    native void invalidateHotSpotNmethod(HotSpotNmethod nmethodMirror);

    /**
     * Collects the current values of all JVMCI benchmark counters, summed up over all threads.
     */
    native long[] collectCounters();

    /**
     * Get the current number of counters allocated for use by JVMCI. Should be the same value as
     * the flag {@code JVMCICounterSize}.
     */
    native int getCountersSize();

    /**
     * Attempt to change the size of the counters allocated for JVMCI. This requires a safepoint to
     * safely reallocate the storage but it's advisable to increase the size in reasonable chunks.
     */
    native boolean setCountersSize(int newSize);

    /**
     * Determines if {@code metaspaceMethodData} is mature.
     */
    native boolean isMature(long metaspaceMethodData);

    /**
     * Generate a unique id to identify the result of the compile.
     */
    native int allocateCompileId(HotSpotResolvedJavaMethodImpl method, int entryBCI);

    /**
     * Determines if {@code method} has OSR compiled code identified by {@code entryBCI} for
     * compilation level {@code level}.
     */
    native boolean hasCompiledCodeForOSR(HotSpotResolvedJavaMethodImpl method, int entryBCI, int level);

    /**
     * Gets the value of {@code metaspaceSymbol} as a String.
     */
    native String getSymbol(long metaspaceSymbol);

    /**
     * @see jdk.vm.ci.code.stack.StackIntrospection#iterateFrames
     */
    native <T> T iterateFrames(ResolvedJavaMethod[] initialMethods, ResolvedJavaMethod[] matchingMethods, int initialSkip, InspectedFrameVisitor<T> visitor);

    /**
     * Materializes all virtual objects within {@code stackFrame} and updates its locals.
     *
     * @param invalidate if {@code true}, the compiled method for the stack frame will be
     *            invalidated
     */
    native void materializeVirtualObjects(HotSpotStackFrameReference stackFrame, boolean invalidate);

    /**
     * Gets the v-table index for interface method {@code method} in the receiver {@code type} or
     * {@link HotSpotVMConfig#invalidVtableIndex} if {@code method} is not in {@code type}'s
     * v-table.
     *
     * @throws InternalError if {@code type} is an interface, {@code method} is not defined by an
     *             interface, {@code type} does not implement the interface defining {@code method}
     *             or class represented by {@code type} is not initialized
     */
    native int getVtableIndexForInterfaceMethod(HotSpotResolvedObjectTypeImpl type, HotSpotResolvedJavaMethodImpl method);

    /**
     * Determines if debug info should also be emitted at non-safepoint locations.
     */
    native boolean shouldDebugNonSafepoints();

    /**
     * Writes {@code length} bytes from {@code buffer} to HotSpot's log stream.
     *
     * @param buffer if {@code length <= 8}, then the bytes are encoded in this value in native
     *            endianness order otherwise this is the address of a native memory buffer holding
     *            the bytes
     * @param flush specifies if the log stream should be flushed after writing
     */
    native void writeDebugOutput(long buffer, int length, boolean flush);

    /**
     * Flush HotSpot's log stream.
     */
    native void flushDebugOutput();

    /**
     * Read a HotSpot Method* value from the memory location described by {@code base} plus
     * {@code displacement} and return the {@link HotSpotResolvedJavaMethodImpl} wrapping it. This
     * method does no checking that the memory location actually contains a valid pointer and may
     * crash the VM if an invalid location is provided. If the {@code base} is null then
     * {@code displacement} is used by itself. If {@code base} is a
     * {@link HotSpotResolvedJavaMethodImpl}, {@link HotSpotConstantPool} or
     * {@link HotSpotResolvedObjectTypeImpl} then the metaspace pointer is fetched from that object
     * and added to {@code displacement}. Any other non-null object type causes an
     * {@link IllegalArgumentException} to be thrown.
     *
     * @param base an object to read from or null
     * @param displacement
     * @return null or the resolved method for this location
     */
    native HotSpotResolvedJavaMethodImpl getResolvedJavaMethod(HotSpotObjectConstantImpl base, long displacement);

    /**
     * Gets the {@code ConstantPool*} associated with {@code object} and returns a
     * {@link HotSpotConstantPool} wrapping it.
     *
     * @param object a {@link HotSpotResolvedJavaMethodImpl} or
     *            {@link HotSpotResolvedObjectTypeImpl} object
     * @return a {@link HotSpotConstantPool} wrapping the {@code ConstantPool*} associated with
     *         {@code object}
     * @throws NullPointerException if {@code object == null}
     * @throws IllegalArgumentException if {@code object} is neither a
     *             {@link HotSpotResolvedJavaMethodImpl} nor a {@link HotSpotResolvedObjectTypeImpl}
     */
    native HotSpotConstantPool getConstantPool(MetaspaceObject object);

    /**
     * Read a HotSpot Klass* value from the memory location described by {@code base} plus
     * {@code displacement} and return the {@link HotSpotResolvedObjectTypeImpl} wrapping it. This
     * method does no checking that the memory location actually contains a valid pointer and may
     * crash the VM if an invalid location is provided. If the {@code base} is null then
     * {@code displacement} is used by itself. If {@code base} is a
     * {@link HotSpotResolvedJavaMethodImpl}, {@link HotSpotConstantPool} or
     * {@link HotSpotResolvedObjectTypeImpl} then the metaspace pointer is fetched from that object
     * and added to {@code displacement}. Any other non-null object type causes an
     * {@link IllegalArgumentException} to be thrown.
     *
     * @param base an object to read from or null
     * @param displacement
     * @param compressed true if the location contains a compressed Klass*
     * @return null or the resolved method for this location
     */
    private native HotSpotResolvedObjectTypeImpl getResolvedJavaType0(Object base, long displacement, boolean compressed);

    HotSpotResolvedObjectTypeImpl getResolvedJavaType(MetaspaceObject base, long displacement, boolean compressed) {
        return getResolvedJavaType0(base, displacement, compressed);
    }

    HotSpotResolvedObjectTypeImpl getResolvedJavaType(HotSpotObjectConstantImpl base, long displacement, boolean compressed) {
        return getResolvedJavaType0(base, displacement, compressed);
    }

    HotSpotResolvedObjectTypeImpl getResolvedJavaType(long displacement, boolean compressed) {
        return getResolvedJavaType0(null, displacement, compressed);
    }

    /**
     * Return the size of the HotSpot ProfileData* pointed at by {@code position}. If
     * {@code position} is outside the space of the MethodData then an
     * {@link IllegalArgumentException} is thrown. A {@code position} inside the MethodData but that
     * isn't pointing at a valid ProfileData will crash the VM.
     *
     * @param metaspaceMethodData
     * @param position
     * @return the size of the ProfileData item pointed at by {@code position}
     * @throws IllegalArgumentException if an out of range position is given
     */
    native int methodDataProfileDataSize(long metaspaceMethodData, int position);

    /**
     * Gets the fingerprint for a given Klass*.
     *
     * @param metaspaceKlass
     * @return the value of the fingerprint (zero for arrays and synthetic classes).
     */
    native long getFingerprint(long metaspaceKlass);

    /**
     * Return the amount of native stack required for the interpreter frames represented by
     * {@code frame}. This is used when emitting the stack banging code to ensure that there is
     * enough space for the frames during deoptimization.
     *
     * @param frame
     * @return the number of bytes required for deoptimization of this frame state
     */
    native int interpreterFrameSize(BytecodeFrame frame);

    /**
     * Invokes non-public method {@code java.lang.invoke.LambdaForm.compileToBytecode()} on
     * {@code lambdaForm} (which must be a {@code java.lang.invoke.LambdaForm} instance).
     */
    native void compileToBytecode(HotSpotObjectConstantImpl lambdaForm);

    /**
     * Gets the value of the VM flag named {@code name}.
     *
     * @param name name of a VM option
     * @return {@code this} if the named VM option doesn't exist, a {@link String} or {@code null}
     *         if its type is {@code ccstr} or {@code ccstrlist}, a {@link Double} if its type is
     *         {@code double}, a {@link Boolean} if its type is {@code bool} otherwise a
     *         {@link Long}
     */
    native Object getFlagValue(String name);

    /**
     * @see ResolvedJavaType#getInterfaces()
     */
    native HotSpotResolvedObjectTypeImpl[] getInterfaces(HotSpotResolvedObjectTypeImpl type);

    /**
     * @see ResolvedJavaType#getComponentType()
     */
    native HotSpotResolvedJavaType getComponentType(HotSpotResolvedObjectTypeImpl type);

    /**
     * Get the array class for {@code type}. This can't be done symbolically since hidden classes
     * can't be looked up by name.
     */
    native HotSpotResolvedObjectTypeImpl getArrayType(HotSpotResolvedJavaType type);

    /**
     * Forces initialization of {@code type}.
     */
    native void ensureInitialized(HotSpotResolvedObjectTypeImpl type);

    /**
     * Forces linking of {@code type}.
     */
    native void ensureLinked(HotSpotResolvedObjectTypeImpl type);

    /**
     * Checks if {@code object} is a String and is an interned string value.
     */
    native boolean isInternedString(HotSpotObjectConstantImpl object);

    /**
     * Gets the {@linkplain System#identityHashCode(Object) identity} has code for the object
     * represented by this constant.
     */
    native int getIdentityHashCode(HotSpotObjectConstantImpl object);

    /**
     * Converts a constant object representing a boxed primitive into a boxed primitive.
     */
    native Object unboxPrimitive(HotSpotObjectConstantImpl object);

    /**
     * Converts a boxed primitive into a JavaConstant representing the same value.
     */
    native HotSpotObjectConstantImpl boxPrimitive(Object source);

    /**
     * Gets the {@link ResolvedJavaMethod}s for all the constructors of the type {@code holder}.
     */
    native ResolvedJavaMethod[] getDeclaredConstructors(HotSpotResolvedObjectTypeImpl holder);

    /**
     * Gets the {@link ResolvedJavaMethod}s for all the non-constructor methods of the type
     * {@code holder}.
     */
    native ResolvedJavaMethod[] getDeclaredMethods(HotSpotResolvedObjectTypeImpl holder);

    /**
     * Reads the current value of a static field. If {@code expectedType} is non-null, then the
     * object is exptected to be a subtype of {@code expectedType} and extra sanity checking is
     * performed on the offset and kind of the read being performed.
     */
    native JavaConstant readFieldValue(HotSpotResolvedObjectTypeImpl object, HotSpotResolvedObjectTypeImpl expectedType, long offset, boolean isVolatile, JavaKind kind);

    /**
     * Reads the current value of an instance field. If {@code expectedType} is non-null, then the
     * object is exptected to be a subtype of {@code expectedType} and extra sanity checking is
     * performed on the offset and kind of the read being performed.
     */
    native JavaConstant readFieldValue(HotSpotObjectConstantImpl object, HotSpotResolvedObjectTypeImpl expectedType, long offset, boolean isVolatile, JavaKind kind);

    /**
     * @see ResolvedJavaType#isInstance(JavaConstant)
     */
    native boolean isInstance(HotSpotResolvedObjectTypeImpl holder, HotSpotObjectConstantImpl object);

    /**
     * @see ResolvedJavaType#isAssignableFrom(ResolvedJavaType)
     */
    native boolean isAssignableFrom(HotSpotResolvedObjectTypeImpl holder, HotSpotResolvedObjectTypeImpl otherType);

    /**
     * @see ConstantReflectionProvider#asJavaType(Constant)
     */
    native HotSpotResolvedJavaType asJavaType(HotSpotObjectConstantImpl object);

    /**
     * Converts a String constant into a String.
     */
    native String asString(HotSpotObjectConstantImpl object);

    /**
     * Compares the contents of {@code xHandle} and {@code yHandle} for pointer equality.
     */
    native boolean equals(HotSpotObjectConstantImpl x, long xHandle, HotSpotObjectConstantImpl y, long yHandle);

    /**
     * Gets a {@link JavaConstant} wrapping the {@link java.lang.Class} mirror for {@code type}.
     */
    native HotSpotObjectConstantImpl getJavaMirror(HotSpotResolvedJavaType type);

    /**
     * Returns the length of the array if {@code object} represents an array or -1 otherwise.
     */
    native int getArrayLength(HotSpotObjectConstantImpl object);

    /**
     * Reads the element at {@code index} if {@code object} is an array. Elements of an object array
     * are returned as {@link JavaConstant}s and primitives are returned as boxed values. The value
     * {@code null} is returned if the {@code index} is out of range or object is not an array.
     */
    native Object readArrayElement(HotSpotObjectConstantImpl object, int index);

    /**
     * @see HotSpotJVMCIRuntime#registerNativeMethods
     */
    native long[] registerNativeMethods(Class<?> clazz);

    /**
     * @see HotSpotJVMCIRuntime#translate(Object)
     */
    native long translate(Object obj);

    /**
     * @see HotSpotJVMCIRuntime#unhand(Class, long)
     */
    native Object unhand(long handle);

    /**
     * Updates {@code address} and {@code entryPoint} fields of {@code nmethodMirror} based on the
     * current state of the {@code nmethod} identified by {@code address} and
     * {@code nmethodMirror.compileId} in the code cache.
     */
    native void updateHotSpotNmethod(HotSpotNmethod nmethodMirror);

    /**
     * @see InstalledCode#getCode()
     */
    native byte[] getCode(HotSpotInstalledCode code);

    /**
     * Gets a {@link Executable} corresponding to {@code method}.
     */
    native Executable asReflectionExecutable(HotSpotResolvedJavaMethodImpl method);

    /**
     * Gets a {@link Field} denoted by {@code holder} and {@code index}.
     *
     * @param holder the class in which the requested field is declared
     * @param fieldIndex the {@code fieldDescriptor::index()} denoting the field
     */
    native Field asReflectionField(HotSpotResolvedObjectTypeImpl holder, int fieldIndex);

    /**
     * @see HotSpotJVMCIRuntime#getIntrinsificationTrustPredicate(Class...)
     */
    native boolean isTrustedForIntrinsics(HotSpotResolvedObjectTypeImpl type);

    /**
     * Releases the resources backing the global JNI {@code handle}. This is equivalent to the
     * {@code DeleteGlobalRef} JNI function.
     */
    native void deleteGlobalHandle(long handle);

    /**
     * Gets the failed speculations pointed to by {@code *failedSpeculationsAddress}.
     *
     * @param currentFailures the known failures at {@code failedSpeculationsAddress}
     * @return the list of failed speculations with each entry being a single speculation in the
     *         format emitted by {@link HotSpotSpeculationEncoding#toByteArray()}
     */
    native byte[][] getFailedSpeculations(long failedSpeculationsAddress, byte[][] currentFailures);

    /**
     * Gets the address of the {@code MethodData::_failed_speculations} field in the
     * {@code MethodData} associated with {@code method}. This will create and install the
     * {@code MethodData} if it didn't already exist.
     */
    native long getFailedSpeculationsAddress(HotSpotResolvedJavaMethodImpl method);

    /**
     * Frees the failed speculations pointed to by {@code *failedSpeculationsAddress}.
     */
    native void releaseFailedSpeculations(long failedSpeculationsAddress);

    /**
     * Adds a speculation to the failed speculations pointed to by
     * {@code *failedSpeculationsAddress}.
     *
     * @return {@code false} if the speculation could not be appended to the list
     */
    native boolean addFailedSpeculation(long failedSpeculationsAddress, byte[] speculation);

    /**
     * @see HotSpotJVMCIRuntime#isCurrentThreadAttached()
     */
    native boolean isCurrentThreadAttached();

    /**
     * @see HotSpotJVMCIRuntime#getCurrentJavaThread()
     */
    native long getCurrentJavaThread();

    /**
     * @param name name of current thread if in a native image otherwise {@code null}
     * @see HotSpotJVMCIRuntime#attachCurrentThread
     */
    native boolean attachCurrentThread(byte[] name, boolean asDaemon);

    /**
     * @see HotSpotJVMCIRuntime#detachCurrentThread()
     */
    native void detachCurrentThread();

    /**
     * @see HotSpotJVMCIRuntime#exitHotSpot(int)
     */
    native void callSystemExit(int status);

    /**
     * @see JFR.Ticks#now
     */
    native long ticksNow();

    /**
     * Adds phases in HotSpot JFR.
     *
     * @see JFR.CompilerPhaseEvent#write
     */
    native int registerCompilerPhase(String phaseName);

    /**
     * @see JFR.CompilerPhaseEvent#write
     */
    native void notifyCompilerPhaseEvent(long startTime, int phase, int compileId, int level);

    /**
     * @see JFR.CompilerInliningEvent#write
     */
    native void notifyCompilerInliningEvent(int compileId, HotSpotResolvedJavaMethodImpl caller, HotSpotResolvedJavaMethodImpl callee, boolean succeeded, String message, int bci);

}
