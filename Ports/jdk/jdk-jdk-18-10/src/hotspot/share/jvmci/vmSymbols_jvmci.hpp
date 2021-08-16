/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JVMCI_VMSYMBOLS_JVMCI_HPP
#define SHARE_JVMCI_VMSYMBOLS_JVMCI_HPP


#if !INCLUDE_JVMCI
#define JVMCI_VM_SYMBOLS_DO(template, do_alias)
#else
#define JVMCI_VM_SYMBOLS_DO(template, do_alias)                                                                                           \
  template(jdk_vm_ci_services_Services,                           "jdk/vm/ci/services/Services")                                          \
  template(jdk_vm_ci_runtime_JVMCI,                               "jdk/vm/ci/runtime/JVMCI")                                              \
  template(jdk_vm_ci_hotspot_HotSpotCompiledCode,                 "jdk/vm/ci/hotspot/HotSpotCompiledCode")                                \
  template(jdk_vm_ci_hotspot_HotSpotCompiledCode_Comment,         "jdk/vm/ci/hotspot/HotSpotCompiledCode$Comment")                        \
  template(jdk_vm_ci_hotspot_HotSpotCompiledNmethod,              "jdk/vm/ci/hotspot/HotSpotCompiledNmethod")                             \
  template(jdk_vm_ci_hotspot_HotSpotForeignCallTarget,            "jdk/vm/ci/hotspot/HotSpotForeignCallTarget")                           \
  template(jdk_vm_ci_hotspot_HotSpotReferenceMap,                 "jdk/vm/ci/hotspot/HotSpotReferenceMap")                                \
  template(jdk_vm_ci_hotspot_CompilerToVM,                        "jdk/vm/ci/hotspot/CompilerToVM")                                       \
  template(jdk_vm_ci_hotspot_HotSpotInstalledCode,                "jdk/vm/ci/hotspot/HotSpotInstalledCode")                               \
  template(jdk_vm_ci_hotspot_HotSpotNmethod,                      "jdk/vm/ci/hotspot/HotSpotNmethod")                                     \
  template(jdk_vm_ci_hotspot_HotSpotResolvedJavaMethodImpl,       "jdk/vm/ci/hotspot/HotSpotResolvedJavaMethodImpl")                      \
  template(jdk_vm_ci_hotspot_HotSpotResolvedObjectTypeImpl,       "jdk/vm/ci/hotspot/HotSpotResolvedObjectTypeImpl")                      \
  template(jdk_vm_ci_hotspot_HotSpotResolvedPrimitiveType,        "jdk/vm/ci/hotspot/HotSpotResolvedPrimitiveType")                       \
  template(jdk_vm_ci_hotspot_HotSpotResolvedJavaFieldImpl,        "jdk/vm/ci/hotspot/HotSpotResolvedJavaFieldImpl")                       \
  template(jdk_vm_ci_hotspot_HotSpotCompressedNullConstant,       "jdk/vm/ci/hotspot/HotSpotCompressedNullConstant")                      \
  template(jdk_vm_ci_hotspot_HotSpotObjectConstantImpl,           "jdk/vm/ci/hotspot/HotSpotObjectConstantImpl")                          \
  template(jdk_vm_ci_hotspot_DirectHotSpotObjectConstantImpl,     "jdk/vm/ci/hotspot/DirectHotSpotObjectConstantImpl")                    \
  template(jdk_vm_ci_hotspot_IndirectHotSpotObjectConstantImpl,   "jdk/vm/ci/hotspot/IndirectHotSpotObjectConstantImpl")                  \
  template(jdk_vm_ci_hotspot_HotSpotMetaspaceConstantImpl,        "jdk/vm/ci/hotspot/HotSpotMetaspaceConstantImpl")                       \
  template(jdk_vm_ci_hotspot_HotSpotSentinelConstant,             "jdk/vm/ci/hotspot/HotSpotSentinelConstant")                            \
  template(jdk_vm_ci_hotspot_HotSpotStackFrameReference,          "jdk/vm/ci/hotspot/HotSpotStackFrameReference")                         \
  template(jdk_vm_ci_hotspot_HotSpotMetaData,                     "jdk/vm/ci/hotspot/HotSpotMetaData")                                    \
  template(jdk_vm_ci_hotspot_HotSpotConstantPool,                 "jdk/vm/ci/hotspot/HotSpotConstantPool")                                \
  template(jdk_vm_ci_hotspot_HotSpotJVMCIRuntime,                 "jdk/vm/ci/hotspot/HotSpotJVMCIRuntime")                                \
  template(jdk_vm_ci_hotspot_HotSpotSpeculationLog,               "jdk/vm/ci/hotspot/HotSpotSpeculationLog")                              \
  template(jdk_vm_ci_hotspot_HotSpotCompilationRequestResult,     "jdk/vm/ci/hotspot/HotSpotCompilationRequestResult")                    \
  template(jdk_vm_ci_hotspot_VMField,                             "jdk/vm/ci/hotspot/VMField")                                            \
  template(jdk_vm_ci_hotspot_VMFlag,                              "jdk/vm/ci/hotspot/VMFlag")                                             \
  template(jdk_vm_ci_hotspot_VMIntrinsicMethod,                   "jdk/vm/ci/hotspot/VMIntrinsicMethod")                                  \
  template(jdk_vm_ci_meta_ResolvedJavaMethod,                     "jdk/vm/ci/meta/ResolvedJavaMethod")                                    \
  template(jdk_vm_ci_meta_JavaConstant,                           "jdk/vm/ci/meta/JavaConstant")                                          \
  template(jdk_vm_ci_meta_PrimitiveConstant,                      "jdk/vm/ci/meta/PrimitiveConstant")                                     \
  template(jdk_vm_ci_meta_RawConstant,                            "jdk/vm/ci/meta/RawConstant")                                           \
  template(jdk_vm_ci_meta_NullConstant,                           "jdk/vm/ci/meta/NullConstant")                                          \
  template(jdk_vm_ci_meta_ExceptionHandler,                       "jdk/vm/ci/meta/ExceptionHandler")                                      \
  template(jdk_vm_ci_meta_JavaKind,                               "jdk/vm/ci/meta/JavaKind")                                              \
  template(jdk_vm_ci_meta_ValueKind,                              "jdk/vm/ci/meta/ValueKind")                                             \
  template(jdk_vm_ci_meta_Value,                                  "jdk/vm/ci/meta/Value")                                                 \
  template(jdk_vm_ci_meta_Assumptions_ConcreteSubtype,            "jdk/vm/ci/meta/Assumptions$ConcreteSubtype")                           \
  template(jdk_vm_ci_meta_Assumptions_LeafType,                   "jdk/vm/ci/meta/Assumptions$LeafType")                                  \
  template(jdk_vm_ci_meta_Assumptions_NoFinalizableSubclass,      "jdk/vm/ci/meta/Assumptions$NoFinalizableSubclass")                     \
  template(jdk_vm_ci_meta_Assumptions_ConcreteMethod,             "jdk/vm/ci/meta/Assumptions$ConcreteMethod")                            \
  template(jdk_vm_ci_meta_Assumptions_CallSiteTargetValue,        "jdk/vm/ci/meta/Assumptions$CallSiteTargetValue")                       \
  template(jdk_vm_ci_code_Architecture,                           "jdk/vm/ci/code/Architecture")                                          \
  template(jdk_vm_ci_code_BytecodeFrame,                          "jdk/vm/ci/code/BytecodeFrame")                                         \
  template(jdk_vm_ci_code_BytecodePosition,                       "jdk/vm/ci/code/BytecodePosition")                                      \
  template(jdk_vm_ci_code_DebugInfo,                              "jdk/vm/ci/code/DebugInfo")                                             \
  template(jdk_vm_ci_code_InstalledCode,                          "jdk/vm/ci/code/InstalledCode")                                         \
  template(jdk_vm_ci_code_Location,                               "jdk/vm/ci/code/Location")                                              \
  template(jdk_vm_ci_code_Register,                               "jdk/vm/ci/code/Register")                                              \
  template(jdk_vm_ci_code_RegisterValue,                          "jdk/vm/ci/code/RegisterValue")                                         \
  template(jdk_vm_ci_code_StackSlot,                              "jdk/vm/ci/code/StackSlot")                                             \
  template(jdk_vm_ci_code_StackLockValue,                         "jdk/vm/ci/code/StackLockValue")                                        \
  template(jdk_vm_ci_code_TargetDescription,                      "jdk/vm/ci/code/TargetDescription")                                     \
  template(jdk_vm_ci_code_VirtualObject,                          "jdk/vm/ci/code/VirtualObject")                                         \
  template(jdk_vm_ci_code_RegisterSaveLayout,                     "jdk/vm/ci/code/RegisterSaveLayout")                                    \
  template(jdk_vm_ci_code_InvalidInstalledCodeException,          "jdk/vm/ci/code/InvalidInstalledCodeException")                         \
  template(jdk_vm_ci_code_site_Call,                              "jdk/vm/ci/code/site/Call")                                             \
  template(jdk_vm_ci_code_site_ConstantReference,                 "jdk/vm/ci/code/site/ConstantReference")                                \
  template(jdk_vm_ci_code_site_DataPatch,                         "jdk/vm/ci/code/site/DataPatch")                                        \
  template(jdk_vm_ci_code_site_DataSectionReference,              "jdk/vm/ci/code/site/DataSectionReference")                             \
  template(jdk_vm_ci_code_site_ExceptionHandler,                  "jdk/vm/ci/code/site/ExceptionHandler")                                 \
  template(jdk_vm_ci_code_site_Mark,                              "jdk/vm/ci/code/site/Mark")                                             \
  template(jdk_vm_ci_code_site_ImplicitExceptionDispatch,         "jdk/vm/ci/code/site/ImplicitExceptionDispatch")                        \
  template(jdk_vm_ci_code_site_Infopoint,                         "jdk/vm/ci/code/site/Infopoint")                                        \
  template(jdk_vm_ci_code_stack_InspectedFrameVisitor,            "jdk/vm/ci/code/stack/InspectedFrameVisitor")                           \
  template(jdk_vm_ci_code_site_Site,                              "jdk/vm/ci/code/site/Site")                                             \
  template(jdk_vm_ci_code_site_InfopointReason,                   "jdk/vm/ci/code/site/InfopointReason")                                  \
  template(jdk_vm_ci_common_JVMCIError,                           "jdk/vm/ci/common/JVMCIError")                                          \
                                                                                                                                          \
  template(visitFrame_name,                                       "visitFrame")                                                           \
  template(visitFrame_signature,                                  "(Ljdk/vm/ci/code/stack/InspectedFrame;)Ljava/lang/Object;")            \
  template(compileMethod_name,                                    "compileMethod")                                                        \
  template(compileMethod_signature,                               "(Ljdk/vm/ci/hotspot/HotSpotResolvedJavaMethod;IJI)Ljdk/vm/ci/hotspot/HotSpotCompilationRequestResult;") \
  template(isGCSupported_name,                                    "isGCSupported")                                                        \
  template(encodeThrowable_name,                                  "encodeThrowable")                                                      \
  template(encodeThrowable_signature,                             "(Ljava/lang/Throwable;)Ljava/lang/String;")                            \
  template(decodeThrowable_name,                                  "decodeThrowable")                                                      \
  template(decodeThrowable_signature,                             "(Ljava/lang/String;)Ljava/lang/Throwable;")                            \
  template(fromMetaspace_name,                                    "fromMetaspace")                                                        \
  template(method_fromMetaspace_signature,                        "(J)Ljdk/vm/ci/hotspot/HotSpotResolvedJavaMethod;")                     \
  template(constantPool_fromMetaspace_signature,                  "(J)Ljdk/vm/ci/hotspot/HotSpotConstantPool;")                           \
  template(klass_fromMetaspace_signature,                         "(JLjava/lang/String;)Ljdk/vm/ci/hotspot/HotSpotResolvedObjectTypeImpl;") \
  template(primitive_fromMetaspace_signature,                     "(Ljdk/vm/ci/hotspot/HotSpotObjectConstantImpl;C)Ljdk/vm/ci/hotspot/HotSpotResolvedPrimitiveType;") \
  template(getRuntime_name,                                       "getRuntime")                                                           \
  template(getRuntime_signature,                                  "()Ljdk/vm/ci/runtime/JVMCIRuntime;")                                   \
  template(initializeRuntime_name,                                "initializeRuntime")                                                    \
  do_alias(initializeRuntime_signature, getRuntime_signature)                                                                             \
  template(runtime_name,                                          "runtime")                                                              \
  template(runtime_signature,                                     "()Ljdk/vm/ci/hotspot/HotSpotJVMCIRuntime;")                            \
  template(getCompiler_name,                                      "getCompiler")                                                          \
  template(getCompiler_signature,                                 "()Ljdk/vm/ci/runtime/JVMCICompiler;")                                  \
  template(callToString_name,                                     "callToString")                                                         \
  template(callToString_signature,                                "(Ljava/lang/Object;)Ljava/lang/String;")                               \
  template(getName_name,                                          "getName")                                                              \
  template(bootstrapFinished_name,                                "bootstrapFinished")                                                    \
  template(forPrimitive_name,                                     "forPrimitive")                                                         \
  template(forPrimitive_signature,                                "(Ljdk/vm/ci/meta/JavaKind;J)Ljdk/vm/ci/meta/PrimitiveConstant;")       \
  template(method_string_bool_long_signature,                     "(Ljdk/vm/ci/hotspot/HotSpotResolvedJavaMethodImpl;Ljava/lang/String;ZJ)V") \
  template(initializeSavedProperties_name,                        "initializeSavedProperties")                                            \

#endif

#endif // SHARE_JVMCI_VMSYMBOLS_JVMCI_HPP
