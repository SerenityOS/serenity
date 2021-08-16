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

#ifndef SHARE_JVMCI_JVMCIJAVACLASSES_HPP
#define SHARE_JVMCI_JVMCIJAVACLASSES_HPP

#include "classfile/vmSymbols.hpp"
#include "jvmci/jvmciExceptions.hpp"
#include "jvmci/jvmciObject.hpp"

/*
 * This macro defines the structure of the JVMCI classes accessed from VM code.  It is used to
 * generate accessors similar to javaClasses.hpp, but with specializations for HotSpot and JNI based
 * access.
 *
 * HotSpotJVMCI: This class contains accessors based on the VM internal
 * interface to Java. It is used for JVMCI Java code executing on the HotSpot heap.
 *
 * JNIJVMCI: This class contains JNI based accessors and is used for JVMCI
 * Java code executing in the JVMCI shared library.
 */

#define JVMCI_CLASSES_DO(start_class, \
                         end_class, \
                         char_field, \
                         int_field, \
                         boolean_field, \
                         long_field, \
                         float_field, \
                         object_field, \
                         primarray_field, \
                         objectarray_field, \
                         static_object_field, \
                         static_objectarray_field, \
                         static_int_field, \
                         static_boolean_field, \
                         jvmci_method, \
                         jvmci_constructor) \
  start_class(Services, jdk_vm_ci_services_Services)                                                          \
    jvmci_method(CallStaticVoidMethod, GetStaticMethodID, call_static, void, Services, initializeSavedProperties, byte_array_void_signature, (JVMCIObject serializedProperties)) \
  end_class                                                                                                   \
  start_class(Architecture, jdk_vm_ci_code_Architecture)                                                      \
    object_field(Architecture, wordKind, "Ljdk/vm/ci/meta/PlatformKind;")                                     \
  end_class                                                                                                   \
  start_class(TargetDescription, jdk_vm_ci_code_TargetDescription)                                            \
    object_field(TargetDescription, arch, "Ljdk/vm/ci/code/Architecture;")                                    \
  end_class                                                                                                   \
  start_class(HotSpotResolvedObjectTypeImpl, jdk_vm_ci_hotspot_HotSpotResolvedObjectTypeImpl)                 \
    long_field(HotSpotResolvedObjectTypeImpl, metadataPointer)                                                \
  end_class                                                                                                   \
  start_class(HotSpotResolvedPrimitiveType, jdk_vm_ci_hotspot_HotSpotResolvedPrimitiveType)                   \
    object_field(HotSpotResolvedPrimitiveType, mirror, "Ljdk/vm/ci/hotspot/HotSpotObjectConstantImpl;")       \
  object_field(HotSpotResolvedPrimitiveType, kind, "Ljdk/vm/ci/meta/JavaKind;")                               \
    static_objectarray_field(HotSpotResolvedPrimitiveType, primitives, "[Ljdk/vm/ci/hotspot/HotSpotResolvedPrimitiveType;") \
  end_class                                                                                                   \
  start_class(HotSpotResolvedJavaFieldImpl, jdk_vm_ci_hotspot_HotSpotResolvedJavaFieldImpl)                   \
    object_field(HotSpotResolvedJavaFieldImpl, type, "Ljdk/vm/ci/meta/JavaType;")                             \
    object_field(HotSpotResolvedJavaFieldImpl, holder, "Ljdk/vm/ci/hotspot/HotSpotResolvedObjectTypeImpl;")   \
    int_field(HotSpotResolvedJavaFieldImpl, offset)                                                           \
    int_field(HotSpotResolvedJavaFieldImpl, modifiers)                                                        \
  end_class                                                                                                   \
  start_class(HotSpotResolvedJavaMethodImpl, jdk_vm_ci_hotspot_HotSpotResolvedJavaMethodImpl)                 \
    long_field(HotSpotResolvedJavaMethodImpl, metadataHandle)                                                 \
  end_class                                                                                                   \
  start_class(InstalledCode, jdk_vm_ci_code_InstalledCode)                                                    \
    long_field(InstalledCode, address)                                                                        \
    long_field(InstalledCode, entryPoint)                                                                     \
    long_field(InstalledCode, version)                                                                        \
    object_field(InstalledCode, name, "Ljava/lang/String;")                                                   \
  end_class                                                                                                   \
  start_class(HotSpotInstalledCode, jdk_vm_ci_hotspot_HotSpotInstalledCode)                                   \
    int_field(HotSpotInstalledCode, size)                                                                     \
    long_field(HotSpotInstalledCode, codeStart)                                                               \
    int_field(HotSpotInstalledCode, codeSize)                                                                 \
  end_class                                                                                                   \
  start_class(HotSpotNmethod, jdk_vm_ci_hotspot_HotSpotNmethod)                                               \
    boolean_field(HotSpotNmethod, isDefault)                                                                  \
    long_field(HotSpotNmethod, compileIdSnapshot)                                                             \
    object_field(HotSpotNmethod, method, "Ljdk/vm/ci/hotspot/HotSpotResolvedJavaMethodImpl;")                 \
    jvmci_constructor(HotSpotNmethod, "(Ljdk/vm/ci/hotspot/HotSpotResolvedJavaMethodImpl;Ljava/lang/String;ZJ)V") \
  end_class                                                                                                   \
  start_class(HotSpotCompiledCode, jdk_vm_ci_hotspot_HotSpotCompiledCode)                                     \
    object_field(HotSpotCompiledCode, name, "Ljava/lang/String;")                                             \
    primarray_field(HotSpotCompiledCode, targetCode, "[B")                                                    \
    int_field(HotSpotCompiledCode, targetCodeSize)                                                            \
    objectarray_field(HotSpotCompiledCode, sites, "[Ljdk/vm/ci/code/site/Site;")                              \
    objectarray_field(HotSpotCompiledCode, assumptions, "[Ljdk/vm/ci/meta/Assumptions$Assumption;")           \
    objectarray_field(HotSpotCompiledCode, methods, "[Ljdk/vm/ci/meta/ResolvedJavaMethod;")                   \
    objectarray_field(HotSpotCompiledCode, comments, "[Ljdk/vm/ci/hotspot/HotSpotCompiledCode$Comment;")      \
    primarray_field(HotSpotCompiledCode, dataSection, "[B")                                                   \
    int_field(HotSpotCompiledCode, dataSectionAlignment)                                                      \
    objectarray_field(HotSpotCompiledCode, dataSectionPatches, "[Ljdk/vm/ci/code/site/DataPatch;")            \
    boolean_field(HotSpotCompiledCode, isImmutablePIC)                                                        \
    int_field(HotSpotCompiledCode, totalFrameSize)                                                            \
    object_field(HotSpotCompiledCode, deoptRescueSlot, "Ljdk/vm/ci/code/StackSlot;")                          \
  end_class                                                                                                   \
  start_class(HotSpotCompiledCode_Comment, jdk_vm_ci_hotspot_HotSpotCompiledCode_Comment)                     \
    object_field(HotSpotCompiledCode_Comment, text, "Ljava/lang/String;")                                     \
    int_field(HotSpotCompiledCode_Comment, pcOffset)                                                          \
  end_class                                                                                                   \
  start_class(HotSpotCompiledNmethod, jdk_vm_ci_hotspot_HotSpotCompiledNmethod)                               \
    object_field(HotSpotCompiledNmethod, method, "Ljdk/vm/ci/hotspot/HotSpotResolvedJavaMethod;")             \
    object_field(HotSpotCompiledNmethod, installationFailureMessage, "Ljava/lang/String;")                    \
    int_field(HotSpotCompiledNmethod, entryBCI)                                                               \
    int_field(HotSpotCompiledNmethod, id)                                                                     \
    long_field(HotSpotCompiledNmethod, compileState)                                                          \
    boolean_field(HotSpotCompiledNmethod, hasUnsafeAccess)                                                    \
  end_class                                                                                                   \
  start_class(HotSpotForeignCallTarget, jdk_vm_ci_hotspot_HotSpotForeignCallTarget)                           \
    long_field(HotSpotForeignCallTarget, address)                                                             \
  end_class                                                                                                   \
  start_class(VMField, jdk_vm_ci_hotspot_VMField)                                                             \
    object_field(VMField, name, "Ljava/lang/String;")                                                         \
    object_field(VMField, type, "Ljava/lang/String;")                                                         \
    long_field(VMField, offset)                                                                               \
    long_field(VMField, address)                                                                              \
    object_field(VMField, value, "Ljava/lang/Object;")                                                        \
    jvmci_constructor(VMField, "(Ljava/lang/String;Ljava/lang/String;JJLjava/lang/Object;)V")                 \
  end_class                                                                                                   \
  start_class(VMFlag, jdk_vm_ci_hotspot_VMFlag)                                                               \
    object_field(VMFlag, name, "Ljava/lang/String;")                                                          \
    object_field(VMFlag, type, "Ljava/lang/String;")                                                          \
    object_field(VMFlag, value, "Ljava/lang/Object;")                                                         \
    jvmci_constructor(VMFlag, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V")                    \
  end_class                                                                                                   \
  start_class(VMIntrinsicMethod, jdk_vm_ci_hotspot_VMIntrinsicMethod)                                         \
    object_field(VMIntrinsicMethod, declaringClass, "Ljava/lang/String;")                                     \
    object_field(VMIntrinsicMethod, name, "Ljava/lang/String;")                                               \
    object_field(VMIntrinsicMethod, descriptor, "Ljava/lang/String;")                                         \
    int_field(VMIntrinsicMethod, id)                                                                          \
    jvmci_constructor(VMIntrinsicMethod, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V")        \
  end_class                                                                                                   \
  start_class(Assumptions_NoFinalizableSubclass, jdk_vm_ci_meta_Assumptions_NoFinalizableSubclass)            \
    object_field(Assumptions_NoFinalizableSubclass, receiverType, "Ljdk/vm/ci/meta/ResolvedJavaType;")        \
  end_class                                                                                                   \
  start_class(Assumptions_ConcreteSubtype, jdk_vm_ci_meta_Assumptions_ConcreteSubtype)                        \
    object_field(Assumptions_ConcreteSubtype, context, "Ljdk/vm/ci/meta/ResolvedJavaType;")                   \
    object_field(Assumptions_ConcreteSubtype, subtype, "Ljdk/vm/ci/meta/ResolvedJavaType;")                   \
  end_class                                                                                                   \
  start_class(Assumptions_LeafType, jdk_vm_ci_meta_Assumptions_LeafType)                                      \
    object_field(Assumptions_LeafType, context, "Ljdk/vm/ci/meta/ResolvedJavaType;")                          \
  end_class                                                                                                   \
  start_class(Assumptions_ConcreteMethod, jdk_vm_ci_meta_Assumptions_ConcreteMethod)                          \
    object_field(Assumptions_ConcreteMethod, method, "Ljdk/vm/ci/meta/ResolvedJavaMethod;")                   \
    object_field(Assumptions_ConcreteMethod, context, "Ljdk/vm/ci/meta/ResolvedJavaType;")                    \
    object_field(Assumptions_ConcreteMethod, impl, "Ljdk/vm/ci/meta/ResolvedJavaMethod;")                     \
  end_class                                                                                                   \
  start_class(Assumptions_CallSiteTargetValue, jdk_vm_ci_meta_Assumptions_CallSiteTargetValue)                \
    object_field(Assumptions_CallSiteTargetValue, callSite, "Ljdk/vm/ci/meta/JavaConstant;")                  \
    object_field(Assumptions_CallSiteTargetValue, methodHandle, "Ljdk/vm/ci/meta/JavaConstant;")              \
  end_class                                                                                                   \
  start_class(site_Site, jdk_vm_ci_code_site_Site)                                                            \
    int_field(site_Site, pcOffset)                                                                            \
  end_class                                                                                                   \
  start_class(site_Call, jdk_vm_ci_code_site_Call)                                                            \
    object_field(site_Call, target, "Ljdk/vm/ci/meta/InvokeTarget;")                                          \
    boolean_field(site_Call, direct)                                                                          \
  end_class                                                                                                   \
  start_class(site_ImplicitExceptionDispatch, jdk_vm_ci_code_site_ImplicitExceptionDispatch)                  \
    int_field(site_ImplicitExceptionDispatch, dispatchOffset)                                                 \
  end_class                                                                                                   \
  start_class(site_DataPatch, jdk_vm_ci_code_site_DataPatch)                                                  \
    object_field(site_DataPatch, reference, "Ljdk/vm/ci/code/site/Reference;")                                \
  end_class                                                                                                   \
  start_class(site_ConstantReference, jdk_vm_ci_code_site_ConstantReference)                                  \
    object_field(site_ConstantReference, constant, "Ljdk/vm/ci/meta/VMConstant;")                             \
  end_class                                                                                                   \
  start_class(site_DataSectionReference, jdk_vm_ci_code_site_DataSectionReference)                            \
    int_field(site_DataSectionReference, offset)                                                              \
  end_class                                                                                                   \
  start_class(site_InfopointReason, jdk_vm_ci_code_site_InfopointReason)                                      \
    static_object_field(site_InfopointReason, SAFEPOINT, "Ljdk/vm/ci/code/site/InfopointReason;")             \
    static_object_field(site_InfopointReason, CALL, "Ljdk/vm/ci/code/site/InfopointReason;")                  \
    static_object_field(site_InfopointReason, IMPLICIT_EXCEPTION, "Ljdk/vm/ci/code/site/InfopointReason;")    \
  end_class                                                                                                   \
  start_class(site_Infopoint, jdk_vm_ci_code_site_Infopoint)                                                  \
    object_field(site_Infopoint, debugInfo, "Ljdk/vm/ci/code/DebugInfo;")                                     \
    object_field(site_Infopoint, reason, "Ljdk/vm/ci/code/site/InfopointReason;")                             \
  end_class                                                                                                   \
  start_class(site_ExceptionHandler, jdk_vm_ci_code_site_ExceptionHandler)                                    \
    int_field(site_ExceptionHandler, handlerPos)                                                              \
  end_class                                                                                                   \
  start_class(site_Mark, jdk_vm_ci_code_site_Mark)                                                            \
    object_field(site_Mark, id, "Ljava/lang/Object;")                                                         \
  end_class                                                                                                   \
  start_class(HotSpotCompilationRequestResult, jdk_vm_ci_hotspot_HotSpotCompilationRequestResult)             \
    object_field(HotSpotCompilationRequestResult, failureMessage, "Ljava/lang/String;")                       \
    boolean_field(HotSpotCompilationRequestResult, retry)                                                     \
    int_field(HotSpotCompilationRequestResult, inlinedBytecodes)                                              \
  end_class                                                                                                   \
  start_class(DebugInfo, jdk_vm_ci_code_DebugInfo)                                                            \
    object_field(DebugInfo, bytecodePosition, "Ljdk/vm/ci/code/BytecodePosition;")                            \
    object_field(DebugInfo, referenceMap, "Ljdk/vm/ci/code/ReferenceMap;")                                    \
    object_field(DebugInfo, calleeSaveInfo, "Ljdk/vm/ci/code/RegisterSaveLayout;")                            \
    objectarray_field(DebugInfo, virtualObjectMapping, "[Ljdk/vm/ci/code/VirtualObject;")                     \
  end_class                                                                                                   \
  start_class(HotSpotReferenceMap, jdk_vm_ci_hotspot_HotSpotReferenceMap)                                     \
    objectarray_field(HotSpotReferenceMap, objects, "[Ljdk/vm/ci/code/Location;")                             \
    objectarray_field(HotSpotReferenceMap, derivedBase, "[Ljdk/vm/ci/code/Location;")                         \
    primarray_field(HotSpotReferenceMap, sizeInBytes, "[I")                                                   \
    int_field(HotSpotReferenceMap, maxRegisterSize)                                                           \
  end_class                                                                                                   \
  start_class(RegisterSaveLayout, jdk_vm_ci_code_RegisterSaveLayout)                                          \
    objectarray_field(RegisterSaveLayout, registers, "[Ljdk/vm/ci/code/Register;")                            \
    primarray_field(RegisterSaveLayout, slots, "[I")                                                          \
  end_class                                                                                                   \
  start_class(BytecodeFrame, jdk_vm_ci_code_BytecodeFrame)                                                    \
    objectarray_field(BytecodeFrame, values, "[Ljdk/vm/ci/meta/JavaValue;")                                   \
    objectarray_field(BytecodeFrame, slotKinds, "[Ljdk/vm/ci/meta/JavaKind;")                                 \
    int_field(BytecodeFrame, numLocals)                                                                       \
    int_field(BytecodeFrame, numStack)                                                                        \
    int_field(BytecodeFrame, numLocks)                                                                        \
    boolean_field(BytecodeFrame, rethrowException)                                                            \
    boolean_field(BytecodeFrame, duringCall)                                                                  \
    static_int_field(BytecodeFrame, UNKNOWN_BCI)                                                              \
    static_int_field(BytecodeFrame, UNWIND_BCI)                                                               \
    static_int_field(BytecodeFrame, BEFORE_BCI)                                                               \
    static_int_field(BytecodeFrame, AFTER_BCI)                                                                \
    static_int_field(BytecodeFrame, AFTER_EXCEPTION_BCI)                                                      \
    static_int_field(BytecodeFrame, INVALID_FRAMESTATE_BCI)                                                   \
  end_class                                                                                                   \
  start_class(BytecodePosition, jdk_vm_ci_code_BytecodePosition)                                              \
    object_field(BytecodePosition, caller, "Ljdk/vm/ci/code/BytecodePosition;")                               \
    object_field(BytecodePosition, method, "Ljdk/vm/ci/meta/ResolvedJavaMethod;")                             \
    int_field(BytecodePosition, bci)                                                                          \
  end_class                                                                                                   \
  start_class(JavaConstant, jdk_vm_ci_meta_JavaConstant)                                                      \
    static_object_field(JavaConstant, ILLEGAL, "Ljdk/vm/ci/meta/PrimitiveConstant;")                          \
    static_object_field(JavaConstant, NULL_POINTER, "Ljdk/vm/ci/meta/JavaConstant;")                          \
    jvmci_method(CallStaticObjectMethod, GetStaticMethodID, call_static, JVMCIObject, JavaConstant, forPrimitive, forPrimitive_signature, (JVMCIObject kind, jlong value, JVMCI_TRAPS)) \
  end_class                                                                                                   \
  start_class(ResolvedJavaMethod, jdk_vm_ci_meta_ResolvedJavaMethod)                                          \
  end_class                                                                                                   \
  start_class(PrimitiveConstant, jdk_vm_ci_meta_PrimitiveConstant)                                            \
    object_field(PrimitiveConstant, kind, "Ljdk/vm/ci/meta/JavaKind;")                                        \
    long_field(PrimitiveConstant, primitive)                                                                  \
  end_class                                                                                                   \
  start_class(RawConstant, jdk_vm_ci_meta_RawConstant)                                                        \
  end_class                                                                                                   \
  start_class(NullConstant, jdk_vm_ci_meta_NullConstant)                                                      \
  end_class                                                                                                   \
  start_class(HotSpotCompressedNullConstant, jdk_vm_ci_hotspot_HotSpotCompressedNullConstant)                 \
  end_class                                                                                                   \
  start_class(HotSpotObjectConstantImpl, jdk_vm_ci_hotspot_HotSpotObjectConstantImpl)                         \
    boolean_field(HotSpotObjectConstantImpl, compressed)                                                      \
  end_class                                                                                                   \
  start_class(DirectHotSpotObjectConstantImpl, jdk_vm_ci_hotspot_DirectHotSpotObjectConstantImpl)             \
    object_field(DirectHotSpotObjectConstantImpl, object, "Ljava/lang/Object;")                               \
    jvmci_constructor(DirectHotSpotObjectConstantImpl, "(Ljava/lang/Object;Z)V")                              \
  end_class                                                                                                   \
  start_class(IndirectHotSpotObjectConstantImpl, jdk_vm_ci_hotspot_IndirectHotSpotObjectConstantImpl)         \
    long_field(IndirectHotSpotObjectConstantImpl, objectHandle)                                               \
    jvmci_constructor(IndirectHotSpotObjectConstantImpl, "(JZZ)V")                                            \
  end_class                                                                                                   \
  start_class(HotSpotMetaspaceConstantImpl, jdk_vm_ci_hotspot_HotSpotMetaspaceConstantImpl)                   \
    object_field(HotSpotMetaspaceConstantImpl, metaspaceObject, "Ljdk/vm/ci/hotspot/MetaspaceObject;")        \
    boolean_field(HotSpotMetaspaceConstantImpl, compressed)                                                   \
  end_class                                                                                                   \
  start_class(HotSpotSentinelConstant, jdk_vm_ci_hotspot_HotSpotSentinelConstant)                             \
  end_class                                                                                                   \
  start_class(JavaKind, jdk_vm_ci_meta_JavaKind)                                                              \
    char_field(JavaKind, typeChar)                                                                            \
    static_object_field(JavaKind, Boolean, "Ljdk/vm/ci/meta/JavaKind;")                                       \
    static_object_field(JavaKind, Byte, "Ljdk/vm/ci/meta/JavaKind;")                                          \
    static_object_field(JavaKind, Char, "Ljdk/vm/ci/meta/JavaKind;")                                          \
    static_object_field(JavaKind, Short, "Ljdk/vm/ci/meta/JavaKind;")                                         \
    static_object_field(JavaKind, Int, "Ljdk/vm/ci/meta/JavaKind;")                                           \
    static_object_field(JavaKind, Float, "Ljdk/vm/ci/meta/JavaKind;")                                         \
    static_object_field(JavaKind, Long, "Ljdk/vm/ci/meta/JavaKind;")                                          \
    static_object_field(JavaKind, Double, "Ljdk/vm/ci/meta/JavaKind;")                                        \
  end_class                                                                                                   \
  start_class(ValueKind, jdk_vm_ci_meta_ValueKind)                                                            \
    object_field(ValueKind, platformKind, "Ljdk/vm/ci/meta/PlatformKind;")                                    \
  end_class                                                                                                   \
  start_class(Value, jdk_vm_ci_meta_Value)                                                                    \
    object_field(Value, valueKind, "Ljdk/vm/ci/meta/ValueKind;")                                              \
    static_object_field(Value, ILLEGAL, "Ljdk/vm/ci/meta/AllocatableValue;")                                  \
  end_class                                                                                                   \
  start_class(RegisterValue, jdk_vm_ci_code_RegisterValue)                                                    \
    object_field(RegisterValue, reg, "Ljdk/vm/ci/code/Register;")                                             \
  end_class                                                                                                   \
  start_class(code_Location, jdk_vm_ci_code_Location)                                                         \
    object_field(code_Location, reg, "Ljdk/vm/ci/code/Register;")                                             \
    int_field(code_Location, offset)                                                                          \
  end_class                                                                                                   \
  start_class(code_Register, jdk_vm_ci_code_Register)                                                         \
    int_field(code_Register, number)                                                                          \
    int_field(code_Register, encoding)                                                                        \
  end_class                                                                                                   \
  start_class(StackSlot, jdk_vm_ci_code_StackSlot)                                                            \
    int_field(StackSlot, offset)                                                                              \
    boolean_field(StackSlot, addFrameSize)                                                                    \
  end_class                                                                                                   \
  start_class(VirtualObject, jdk_vm_ci_code_VirtualObject)                                                    \
    int_field(VirtualObject, id)                                                                              \
    boolean_field(VirtualObject, isAutoBox)                                                                   \
    object_field(VirtualObject, type, "Ljdk/vm/ci/meta/ResolvedJavaType;")                                    \
    objectarray_field(VirtualObject, values, "[Ljdk/vm/ci/meta/JavaValue;")                                   \
    objectarray_field(VirtualObject, slotKinds, "[Ljdk/vm/ci/meta/JavaKind;")                                 \
  end_class                                                                                                   \
  start_class(StackLockValue, jdk_vm_ci_code_StackLockValue)                                                  \
    object_field(StackLockValue, owner, "Ljdk/vm/ci/meta/JavaValue;")                                         \
    object_field(StackLockValue, slot, "Ljdk/vm/ci/meta/AllocatableValue;")                                   \
    boolean_field(StackLockValue, eliminated)                                                                 \
  end_class                                                                                                   \
  start_class(HotSpotStackFrameReference, jdk_vm_ci_hotspot_HotSpotStackFrameReference)                       \
    object_field(HotSpotStackFrameReference, compilerToVM, "Ljdk/vm/ci/hotspot/CompilerToVM;")                \
    boolean_field(HotSpotStackFrameReference, objectsMaterialized)                                            \
    long_field(HotSpotStackFrameReference, stackPointer)                                                      \
    int_field(HotSpotStackFrameReference, frameNumber)                                                        \
    int_field(HotSpotStackFrameReference, bci)                                                                \
    object_field(HotSpotStackFrameReference, method, "Ljdk/vm/ci/hotspot/HotSpotResolvedJavaMethod;")         \
    objectarray_field(HotSpotStackFrameReference, locals, "[Ljava/lang/Object;")                              \
    primarray_field(HotSpotStackFrameReference, localIsVirtual, "[Z")                                         \
  end_class                                                                                                   \
  start_class(HotSpotMetaData, jdk_vm_ci_hotspot_HotSpotMetaData)                                             \
    primarray_field(HotSpotMetaData, pcDescBytes, "[B")                                                       \
    primarray_field(HotSpotMetaData, scopesDescBytes, "[B")                                                   \
    primarray_field(HotSpotMetaData, relocBytes, "[B")                                                        \
    primarray_field(HotSpotMetaData, exceptionBytes, "[B")                                                    \
    primarray_field(HotSpotMetaData, implicitExceptionBytes, "[B")                                            \
    primarray_field(HotSpotMetaData, oopMaps, "[B")                                                           \
    object_field(HotSpotMetaData, metadata, "[Ljava/lang/Object;")                                            \
  end_class                                                                                                   \
  start_class(HotSpotConstantPool, jdk_vm_ci_hotspot_HotSpotConstantPool)                                     \
    long_field(HotSpotConstantPool, metadataHandle)                                                           \
  end_class                                                                                                   \
  start_class(HotSpotJVMCIRuntime, jdk_vm_ci_hotspot_HotSpotJVMCIRuntime)                                     \
    objectarray_field(HotSpotJVMCIRuntime, excludeFromJVMCICompilation, "[Ljava/lang/Module;")                \
    jvmci_method(CallNonvirtualObjectMethod, GetMethodID, call_special, JVMCIObject, HotSpotJVMCIRuntime, compileMethod, compileMethod_signature, (JVMCIObject runtime, JVMCIObject method, int entry_bci, jlong env, int id)) \
    jvmci_method(CallNonvirtualObjectMethod, GetMethodID, call_special, JVMCIObject, HotSpotJVMCIRuntime, isGCSupported, int_bool_signature, (JVMCIObject runtime, int gcIdentifier)) \
    jvmci_method(CallStaticObjectMethod, GetStaticMethodID, call_static, JVMCIObject, HotSpotJVMCIRuntime, encodeThrowable, encodeThrowable_signature, (JVMCIObject throwable)) \
    jvmci_method(CallStaticObjectMethod, GetStaticMethodID, call_static, JVMCIObject, HotSpotJVMCIRuntime, decodeThrowable, decodeThrowable_signature, (JVMCIObject encodedThrowable)) \
    jvmci_method(CallNonvirtualVoidMethod, GetMethodID, call_special, void, HotSpotJVMCIRuntime, bootstrapFinished, void_method_signature, (JVMCIObject runtime, JVMCI_TRAPS)) \
    jvmci_method(CallNonvirtualVoidMethod, GetMethodID, call_special, void, HotSpotJVMCIRuntime, shutdown, void_method_signature, (JVMCIObject runtime)) \
    jvmci_method(CallStaticObjectMethod, GetStaticMethodID, call_static, JVMCIObject, HotSpotJVMCIRuntime, runtime, runtime_signature, (JVMCI_TRAPS)) \
    jvmci_method(CallObjectMethod, GetMethodID, call_virtual, JVMCIObject, HotSpotJVMCIRuntime, getCompiler, getCompiler_signature, (JVMCIObject runtime, JVMCI_TRAPS)) \
    jvmci_method(CallStaticObjectMethod, GetStaticMethodID, call_static, JVMCIObject, HotSpotJVMCIRuntime, callToString, callToString_signature, (JVMCIObject object, JVMCI_TRAPS)) \
  end_class                                                                                                   \
  start_class(JVMCIError, jdk_vm_ci_common_JVMCIError)                                                        \
    jvmci_constructor(JVMCIError, "(Ljava/lang/String;)V")                                                    \
  end_class                                                                                                   \
  start_class(InspectedFrameVisitor, jdk_vm_ci_code_stack_InspectedFrameVisitor)                              \
  end_class                                                                                                   \
  start_class(JVMCI, jdk_vm_ci_runtime_JVMCI)                                                                 \
    jvmci_method(CallStaticObjectMethod, GetStaticMethodID, call_static, JVMCIObject, JVMCI, getRuntime, getRuntime_signature, (JVMCI_TRAPS)) \
    jvmci_method(CallStaticObjectMethod, GetStaticMethodID, call_static, JVMCIObject, JVMCI, initializeRuntime, initializeRuntime_signature, (JVMCI_TRAPS)) \
  end_class                                                                                                   \
  start_class(Object, java_lang_Object)                                                                       \
  end_class                                                                                                   \
  start_class(String, java_lang_String)                                                                       \
  end_class                                                                                                   \
  start_class(Class, java_lang_Class)                                                                         \
    jvmci_method(CallObjectMethod, GetMethodID, call_virtual, JVMCIObject, Class, getName, void_string_signature, (JVMCI_TRAPS)) \
  end_class                                                                                                   \
  start_class(ArrayIndexOutOfBoundsException, java_lang_ArrayIndexOutOfBoundsException)                       \
    jvmci_constructor(ArrayIndexOutOfBoundsException, "(Ljava/lang/String;)V")                                \
  end_class                                                                                                   \
  start_class(IllegalStateException, java_lang_IllegalStateException)                                         \
    jvmci_constructor(IllegalStateException, "(Ljava/lang/String;)V")                                         \
  end_class                                                                                                   \
  start_class(NullPointerException, java_lang_NullPointerException)                                           \
    jvmci_constructor(NullPointerException, "(Ljava/lang/String;)V")                                          \
  end_class                                                                                                   \
  start_class(IllegalArgumentException, java_lang_IllegalArgumentException)                                   \
    jvmci_constructor(IllegalArgumentException, "(Ljava/lang/String;)V")                                      \
  end_class                                                                                                   \
  start_class(InternalError, java_lang_InternalError)                                                         \
    jvmci_constructor(InternalError, "(Ljava/lang/String;)V")                                                 \
  end_class                                                                                                   \
  start_class(ClassNotFoundException, java_lang_ClassNotFoundException)                                       \
    jvmci_constructor(ClassNotFoundException, "(Ljava/lang/String;)V")                                        \
  end_class                                                                                                   \
  start_class(InvalidInstalledCodeException, jdk_vm_ci_code_InvalidInstalledCodeException)                    \
    jvmci_constructor(InvalidInstalledCodeException, "(Ljava/lang/String;)V")                                 \
  end_class                                                                                                   \
  start_class(UnsatisfiedLinkError, java_lang_UnsatisfiedLinkError)                                           \
    jvmci_constructor(UnsatisfiedLinkError, "(Ljava/lang/String;)V")                                          \
  end_class                                                                                                   \
  start_class(UnsupportedOperationException, java_lang_UnsupportedOperationException)                         \
    jvmci_constructor(UnsupportedOperationException, "(Ljava/lang/String;)V")                                 \
  end_class                                                                                                   \
  start_class(StackTraceElement, java_lang_StackTraceElement)                                                 \
    object_field(StackTraceElement, declaringClass, "Ljava/lang/String;")                                     \
    object_field(StackTraceElement, methodName, "Ljava/lang/String;")                                         \
    object_field(StackTraceElement, fileName, "Ljava/lang/String;")                                           \
    int_field(StackTraceElement, lineNumber)                                                                  \
    jvmci_constructor(StackTraceElement, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V")        \
  end_class                                                                                                   \
  start_class(Throwable, java_lang_Throwable)                                                                 \
    object_field(Throwable, detailMessage, "Ljava/lang/String;")                                              \
  end_class                                                                                                   \
  /* end*/

class JVMCICompiler;
class JVMCIEnv;

#define START_CLASS(simpleClassName, fullClassName)      \
  class simpleClassName { \
    friend class JVMCIEnv; \
    static void initialize(JVMCI_TRAPS); \
    static bool is_instance(JVMCIEnv* jvmciEnv, JVMCIObject object); \

#define END_CLASS };

#define EMPTY_CAST
#define CHAR_FIELD(simpleClassName,  name) FIELD(simpleClassName, name, jchar)
#define INT_FIELD(simpleClassName,  name) FIELD(simpleClassName, name, jint)
#define BOOLEAN_FIELD(simpleClassName,  name) FIELD(simpleClassName, name, jboolean)
#define LONG_FIELD(simpleClassName,  name) FIELD(simpleClassName, name, jlong)
#define FLOAT_FIELD(simpleClassName,  name) FIELD(simpleClassName, name, jfloat)

#define OBJECT_FIELD(simpleClassName,  name, signature) OOPISH_FIELD(simpleClassName, name, JVMCIObject, oop)
#define OBJECTARRAY_FIELD(simpleClassName,  name, signature) OOPISH_FIELD(simpleClassName, name, JVMCIObjectArray, objArrayOop)
#define PRIMARRAY_FIELD(simpleClassName,  name, signature) OOPISH_FIELD(simpleClassName, name, JVMCIPrimitiveArray, typeArrayOop)

#define STATIC_INT_FIELD(simpleClassName, name) STATIC_FIELD(simpleClassName, name, jint)
#define STATIC_BOOLEAN_FIELD(simpleClassName, name) STATIC_FIELD(simpleClassName, name, jboolean)
#define STATIC_OBJECT_FIELD(simpleClassName, name, signature) STATIC_OOPISH_FIELD(simpleClassName, name, JVMCIObject, oop)
#define STATIC_OBJECTARRAY_FIELD(simpleClassName, name, signature) STATIC_OOPISH_FIELD(simpleClassName, name, JVMCIObjectArray, objArrayOop)

#define HS_START_CLASS(simpleClassName, fullClassName)                               \
  START_CLASS(simpleClassName, fullClassName)                                        \
  friend class HotSpotJVMCI;                                                         \
 private:                                                                            \
  static void check(oop obj, const char* field_name, int offset);                    \
  static InstanceKlass* _klass;                                                      \
 public:                                                                             \
  static InstanceKlass* klass() { assert(_klass != NULL, "uninit"); return _klass; } \
  static Symbol* symbol() { return vmSymbols::fullClassName(); }

#define FIELD(simpleClassName, name, type)                                                                   \
  private:                                                                                                   \
    static int _##name##_offset;                                                                             \
  public:                                                                                                    \
    static type get_ ## name(JVMCIEnv* env, JVMCIObject obj) { return name(env, resolve(obj)); }             \
    static void set_ ## name(JVMCIEnv* env, JVMCIObject obj, type x) { set_ ## name(env, resolve(obj), x); } \
    static type name(JVMCIEnv* env, oop obj);                                                                \
    static void set_ ## name(JVMCIEnv* env, oop obj, type x);

#define OOPISH_FIELD(simpleClassName, name, type, hstype)                                                                \
  private:                                                                                                                       \
    static int _##name##_offset;                                                                                                 \
  public:                                                                                                                        \
    static type get_  ## name(JVMCIEnv* env, JVMCIObject obj) { return (type) wrap(name(env, resolve(obj))); }                   \
    static void set_  ## name(JVMCIEnv* env, JVMCIObject obj, type x) { set_  ## name(env, resolve(obj), (hstype) resolve(x)); } \
    static hstype name(JVMCIEnv* env, oop obj);                                                                                  \
    static void set_  ## name(JVMCIEnv* env, oop obj, hstype x);

#define STATIC_FIELD(simpleClassName, name, type)     \
  private:                                            \
    static int _##name##_offset;                      \
  public:                                             \
    static type get_  ## name(JVMCIEnv* env);         \
    static void set_  ## name(JVMCIEnv* env, type x);

#define STATIC_OOPISH_FIELD(simpleClassName, name, type, hstype)                                  \
  private:                                                                                        \
    static int _##name##_offset;                                                                  \
  public:                                                                                         \
    static type get_  ## name(JVMCIEnv* env) { return (type) wrap(name(env)); }                   \
    static void set_  ## name(JVMCIEnv* env, type x) { set_  ## name(env, (hstype) resolve(x)); } \
    static hstype name(JVMCIEnv* env);                                                            \
    static void set_  ## name(JVMCIEnv* env, hstype hstype);

#define METHOD(jniCallType, jniGetMethod, hsCallType, returnType, simpleClassName, methodName, signatureSymbolName, args)
#define CONSTRUCTOR(className, signature)

/**
 * VM internal interface to Java classes, methods and objects. For example:
 *
 * class HotSpotJVMCI {
 *   ...
 *   class Architecture {
 *     static void initialize(JVMCIEnv* env);
 *     static bool is_instance(JVMCIEnv* env, JVMCIObject object);
 *    private:
 *     static void check(oop obj, const char *field_name, int offset);
 *    public:
 *     static InstanceKlass *klass() { ... }
 *     static Symbol *symbol() { return vmSymbols::jdk_vm_ci_code_Architecture(); }
 *    private:
 *     static int _wordKind_offset;
 *    public:
 *     static JVMCIObject get_wordKind(JVMCIEnv *env, JVMCIObject obj) { ... }
 *     static void set_wordKind(JVMCIEnv *env, JVMCIObject obj, JVMCIObject x) { ... }
 *     static oop wordKind(JVMCIEnv *env, oop obj);
 *     static void set_wordKind(JVMCIEnv *env, oop obj, oop x);
 *   }
 *   ...
 * };
 */
class HotSpotJVMCI {
  friend class JVMCIEnv;

 public:

  static oop resolve(JVMCIObject obj);

  static arrayOop resolve(JVMCIArray obj);
  static objArrayOop resolve(JVMCIObjectArray obj);
  static typeArrayOop resolve(JVMCIPrimitiveArray obj);

  static JVMCIObject wrap(jobject obj) { return JVMCIObject(obj, true); }
  static JVMCIObject wrap(oop obj);

  static inline Method* asMethod(JVMCIEnv* env, oop jvmci_method) {
    return *(Method**) HotSpotResolvedJavaMethodImpl::metadataHandle(env, jvmci_method);
  }
  static inline ConstantPool* asConstantPool(JVMCIEnv* env, oop jvmci_constant_pool) {
    return *(ConstantPool**) HotSpotConstantPool::metadataHandle(env, jvmci_constant_pool);
  }
  static inline Klass* asKlass(JVMCIEnv* env, oop jvmci_type) {
    return (Klass*) HotSpotResolvedObjectTypeImpl::metadataPointer(env, jvmci_type);
  }

  static void compute_offsets(TRAPS);
  static void compute_offset(int &dest_offset, Klass* klass, const char* name, const char* signature, bool static_field, TRAPS);

  JVMCI_CLASSES_DO(HS_START_CLASS, END_CLASS, CHAR_FIELD, INT_FIELD, BOOLEAN_FIELD, LONG_FIELD, FLOAT_FIELD, OBJECT_FIELD, PRIMARRAY_FIELD, OBJECTARRAY_FIELD, STATIC_OBJECT_FIELD, STATIC_OBJECTARRAY_FIELD, STATIC_INT_FIELD, STATIC_BOOLEAN_FIELD, METHOD, CONSTRUCTOR)
};

#undef HS_START_CLASS

#define JNI_START_CLASS(simpleClassName, fullClassName)                                             \
  START_CLASS(simpleClassName, fullClassName)                                                       \
  friend class JNIJVMCI;                                                                            \
  private:                                                                                          \
  static void check(JVMCIEnv* jvmciEnv, JVMCIObject obj, const char* field_name, jfieldID offset);  \
  static jclass _class;                                                                             \
public:                                                                                             \
 static jclass clazz() { assert(_class != NULL, #fullClassName " uninitialized"); return _class; }  \
 static jclass fullClassName ##_class()  { assert(_class != NULL, "uninit"); return _class; }

#undef METHOD
#undef CONSTRUCTOR
#undef FIELD
#undef OOPISH_FIELD
#undef STATIC_FIELD
#undef STATIC_OOPISH_FIELD

#define FIELD(simpleClassName, name, type)                                \
  private:                                                                \
    static jfieldID _##name##_field_id;                                   \
  public:                                                                 \
    static type get_  ## name(JVMCIEnv* jvmciEnv, JVMCIObject obj);       \
    static void set_  ## name(JVMCIEnv* jvmciEnv, JVMCIObject obj, type x);

#define OOPISH_FIELD(simpleClassName, name, type, hstype) \
  FIELD(simpleClassName, name, type)

#define STATIC_FIELD(simpleClassName, name, type)   \
  private:                                          \
    static jfieldID _##name##_field_id;             \
  public:                                           \
    static type get_  ## name(JVMCIEnv* jvmciEnv);  \
    static void set_  ## name(JVMCIEnv* jvmciEnv, type x);

#define STATIC_OOPISH_FIELD(simpleClassName, name, type, hstype) \
  STATIC_FIELD(simpleClassName, name, type)

#define METHOD(jniCallType, jniGetMethod, hsCallType, returnType, className, methodName, signatureSymbolName, args) \
  public:                                                                                                           \
    static jmethodID methodName##_method() { return _##methodName##_method; }                                       \
  private:                                                                                                          \
    static jmethodID _##methodName##_method;

#define CONSTRUCTOR(className, signature)                                                                           \
  public:                                                                                                           \
    static jmethodID constructor() { return _constructor; }                                                         \
  private:                                                                                                          \
    static jmethodID _constructor;

/**
 * JNI based interface to Java classes, methods and objects. For example:
 *
 * class JNIJVMCI {
 *   ...
 *   class Architecture {
 *     static void initialize(JVMCIEnv* env);
 *     static bool is_instance(JVMCIEnv* env, JVMCIObject object);
 *    private:
 *     static void check(oop obj, const char *field_name, int offset);
 *     static jclass _class;
 *    public:
 *     static jclass clazz() { return _class; }
 *     static jclass jdk_vm_ci_code_Architecture_class() { return _class; }
 *    private:
 *     static jfieldID _wordKind_field_id;
 *    public:
 *     static JVMCIObject get_wordKind(JVMCIEnv *env, JVMCIObject obj) { ... }
 *     static void set_wordKind(JVMCIEnv *env, JVMCIObject obj, JVMCIObject x) { ... }
 *   }
 *   ...
 * };
 */
class JNIJVMCI {
  friend class JVMCIEnv;

  static jclass _byte_array;
  static jclass _box_classes[T_CONFLICT+1];
  static jfieldID _box_fields[T_CONFLICT+1];
  static jmethodID _box_constructors[T_CONFLICT+1];
  static jmethodID _Class_getName_method;

  static jmethodID _HotSpotResolvedJavaMethodImpl_fromMetaspace_method;
  static jmethodID _HotSpotConstantPool_fromMetaspace_method;
  static jmethodID _HotSpotResolvedObjectTypeImpl_fromMetaspace_method;
  static jmethodID _HotSpotResolvedPrimitiveType_fromMetaspace_method;

 public:
  static jmethodID Class_getName_method() { return _Class_getName_method; }

  static jclass    byte_array()           { assert(_byte_array != NULL, "uninit");      return _byte_array; }

  static jclass    box_class(BasicType type)       { assert(_box_classes[type]!= NULL, "uninit");      return _box_classes[type]; }
  static jfieldID  box_field(BasicType type)       { assert(_box_fields[type]!= NULL, "uninit");       return _box_fields[type]; }
  static jmethodID box_constructor(BasicType type) { assert(_box_constructors[type]!= NULL, "uninit"); return _box_constructors[type]; }

  static jmethodID HotSpotResolvedJavaMethodImpl_fromMetaspace_method()     { assert(_HotSpotResolvedJavaMethodImpl_fromMetaspace_method     != NULL, "uninit"); return _HotSpotResolvedJavaMethodImpl_fromMetaspace_method; }
  static jmethodID HotSpotConstantPool_fromMetaspace_method()           { assert(_HotSpotConstantPool_fromMetaspace_method           != NULL, "uninit"); return _HotSpotConstantPool_fromMetaspace_method; }
  static jmethodID HotSpotResolvedObjectTypeImpl_fromMetaspace_method() { assert(_HotSpotResolvedObjectTypeImpl_fromMetaspace_method != NULL, "uninit"); return _HotSpotResolvedObjectTypeImpl_fromMetaspace_method; }
  static jmethodID HotSpotResolvedPrimitiveType_fromMetaspace_method()  { assert(_HotSpotResolvedPrimitiveType_fromMetaspace_method  != NULL, "uninit"); return _HotSpotResolvedPrimitiveType_fromMetaspace_method; }

  static void initialize_ids(JNIEnv* env);
  static void initialize_field_id(JNIEnv* env, jfieldID &dest_offset, jclass klass, const char* klass_name, const char* name, const char* signature, bool static_field);
  static void register_natives(JNIEnv* env);

  static jobject resolve_handle(JVMCIObject obj) { return obj.as_jobject(); }
  static JVMCIObject wrap(jobject obj) { return JVMCIObject(obj, false); }

  JVMCI_CLASSES_DO(JNI_START_CLASS, END_CLASS, CHAR_FIELD, INT_FIELD, BOOLEAN_FIELD, LONG_FIELD, FLOAT_FIELD, OBJECT_FIELD, PRIMARRAY_FIELD, OBJECTARRAY_FIELD, STATIC_OBJECT_FIELD, STATIC_OBJECTARRAY_FIELD, STATIC_INT_FIELD, STATIC_BOOLEAN_FIELD, METHOD, CONSTRUCTOR)
};

#undef JNI_START_CLASS
#undef START_CLASS
#undef END_CLASS
#undef METHOD
#undef CONSTRUCTOR
#undef FIELD
#undef CHAR_FIELD
#undef INT_FIELD
#undef BOOLEAN_FIELD
#undef LONG_FIELD
#undef FLOAT_FIELD
#undef OBJECT_FIELD
#undef PRIMARRAY_FIELD
#undef OBJECTARRAY_FIELD
#undef FIELD
#undef OOPISH_FIELD
#undef STATIC_FIELD
#undef STATIC_OOPISH_FIELD
#undef STATIC_FIELD
#undef STATIC_OBJECT_FIELD
#undef STATIC_OBJECTARRAY_FIELD
#undef STATIC_INT_FIELD
#undef STATIC_BOOLEAN_FIELD
#undef STATIC_PRIMITIVE_FIELD
#undef EMPTY_CAST

#endif // SHARE_JVMCI_JVMCIJAVACLASSES_HPP
