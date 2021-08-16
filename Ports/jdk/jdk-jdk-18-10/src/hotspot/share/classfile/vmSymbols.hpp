/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#ifndef SHARE_CLASSFILE_VMSYMBOLS_HPP
#define SHARE_CLASSFILE_VMSYMBOLS_HPP

#include "classfile/vmIntrinsics.hpp"
#include "jvmci/vmSymbols_jvmci.hpp"
#include "memory/iterator.hpp"
#include "oops/symbol.hpp"
#include "utilities/macros.hpp"
#include "utilities/enumIterator.hpp"

// The class vmSymbols is a name space for fast lookup of
// symbols commonly used in the VM.
//
// Sample usage:
//
//   Symbol* obj       = vmSymbols::java_lang_Object();


// Useful sub-macros exported by this header file:

#define VM_SYMBOL_ENUM_NAME_(name)    name##_enum
#define VM_SYMBOL_ENUM_NAME(name)    vmSymbolID::VM_SYMBOL_ENUM_NAME_(name)
#define VM_INTRINSIC_IGNORE(id, class, name, sig, flags) /*ignored*/
#define VM_SYMBOL_IGNORE(id, name)                       /*ignored*/
#define VM_ALIAS_IGNORE(id, id2)                         /*ignored*/

// Mapping function names to values. New entries should be added below.

#define VM_SYMBOLS_DO(template, do_alias)                                                         \
  /* commonly used class, package, module names */                                                \
  template(java_base,                                 "java.base")                                \
  template(java_lang_System,                          "java/lang/System")                         \
  template(java_lang_Object,                          "java/lang/Object")                         \
  template(java_lang_Class,                           "java/lang/Class")                          \
  template(java_lang_Package,                         "java/lang/Package")                        \
  template(java_lang_Module,                          "java/lang/Module")                         \
  template(java_lang_String,                          "java/lang/String")                         \
  template(java_lang_StringLatin1,                    "java/lang/StringLatin1")                   \
  template(java_lang_StringUTF16,                     "java/lang/StringUTF16")                    \
  template(java_lang_Thread,                          "java/lang/Thread")                         \
  template(java_lang_ThreadGroup,                     "java/lang/ThreadGroup")                    \
  template(java_lang_Cloneable,                       "java/lang/Cloneable")                      \
  template(java_lang_Throwable,                       "java/lang/Throwable")                      \
  template(java_lang_ClassLoader,                     "java/lang/ClassLoader")                    \
  template(java_lang_ThreadDeath,                     "java/lang/ThreadDeath")                    \
  template(java_lang_Boolean,                         "java/lang/Boolean")                        \
  template(java_lang_Character,                       "java/lang/Character")                      \
  template(java_lang_Character_CharacterCache,        "java/lang/Character$CharacterCache")       \
  template(java_lang_CharacterDataLatin1,             "java/lang/CharacterDataLatin1")            \
  template(java_lang_Float,                           "java/lang/Float")                          \
  template(java_lang_Double,                          "java/lang/Double")                         \
  template(java_lang_Byte,                            "java/lang/Byte")                           \
  template(java_lang_Byte_ByteCache,                  "java/lang/Byte$ByteCache")                 \
  template(java_lang_Short,                           "java/lang/Short")                          \
  template(java_lang_Short_ShortCache,                "java/lang/Short$ShortCache")               \
  template(java_lang_Integer,                         "java/lang/Integer")                        \
  template(java_lang_Integer_IntegerCache,            "java/lang/Integer$IntegerCache")           \
  template(java_lang_Long,                            "java/lang/Long")                           \
  template(java_lang_Long_LongCache,                  "java/lang/Long$LongCache")                 \
                                                                                                  \
  template(jdk_internal_vm_vector_VectorSupport,      "jdk/internal/vm/vector/VectorSupport")               \
  template(jdk_internal_vm_vector_VectorPayload,      "jdk/internal/vm/vector/VectorSupport$VectorPayload") \
  template(jdk_internal_vm_vector_Vector,             "jdk/internal/vm/vector/VectorSupport$Vector")        \
  template(jdk_internal_vm_vector_VectorMask,         "jdk/internal/vm/vector/VectorSupport$VectorMask")    \
  template(jdk_internal_vm_vector_VectorShuffle,      "jdk/internal/vm/vector/VectorSupport$VectorShuffle") \
  template(payload_name,                              "payload")                                            \
  template(ETYPE_name,                                "ETYPE")                                              \
  template(VLENGTH_name,                              "VLENGTH")                                            \
                                                                                                  \
  template(java_lang_Shutdown,                        "java/lang/Shutdown")                       \
  template(java_lang_ref_Reference,                   "java/lang/ref/Reference")                  \
  template(java_lang_ref_SoftReference,               "java/lang/ref/SoftReference")              \
  template(java_lang_ref_WeakReference,               "java/lang/ref/WeakReference")              \
  template(java_lang_ref_FinalReference,              "java/lang/ref/FinalReference")             \
  template(java_lang_ref_PhantomReference,            "java/lang/ref/PhantomReference")           \
  template(java_lang_ref_Finalizer,                   "java/lang/ref/Finalizer")                  \
  template(java_lang_reflect_AccessibleObject,        "java/lang/reflect/AccessibleObject")       \
  template(java_lang_reflect_Method,                  "java/lang/reflect/Method")                 \
  template(java_lang_reflect_Constructor,             "java/lang/reflect/Constructor")            \
  template(java_lang_reflect_Field,                   "java/lang/reflect/Field")                  \
  template(java_lang_reflect_Parameter,               "java/lang/reflect/Parameter")              \
  template(java_lang_reflect_Array,                   "java/lang/reflect/Array")                  \
  template(java_lang_reflect_RecordComponent,         "java/lang/reflect/RecordComponent")        \
  template(java_lang_StringBuffer,                    "java/lang/StringBuffer")                   \
  template(java_lang_StringBuilder,                   "java/lang/StringBuilder")                  \
  template(java_lang_CharSequence,                    "java/lang/CharSequence")                   \
  template(java_lang_SecurityManager,                 "java/lang/SecurityManager")                \
  template(java_security_AccessControlContext,        "java/security/AccessControlContext")       \
  template(java_security_AccessController,            "java/security/AccessController")           \
  template(executePrivileged_name,                    "executePrivileged")                        \
  template(java_security_CodeSource,                  "java/security/CodeSource")                 \
  template(java_security_ProtectionDomain,            "java/security/ProtectionDomain")           \
  template(java_security_SecureClassLoader,           "java/security/SecureClassLoader")          \
  template(java_net_URL,                              "java/net/URL")                             \
  template(java_util_jar_Manifest,                    "java/util/jar/Manifest")                   \
  template(java_io_OutputStream,                      "java/io/OutputStream")                     \
  template(java_io_Reader,                            "java/io/Reader")                           \
  template(java_io_BufferedReader,                    "java/io/BufferedReader")                   \
  template(java_io_File,                              "java/io/File")                             \
  template(java_io_FileInputStream,                   "java/io/FileInputStream")                  \
  template(java_io_ByteArrayInputStream,              "java/io/ByteArrayInputStream")             \
  template(java_io_Serializable,                      "java/io/Serializable")                     \
  template(java_nio_Buffer,                           "java/nio/Buffer")                          \
  template(java_util_Arrays,                          "java/util/Arrays")                         \
  template(java_util_Objects,                         "java/util/Objects")                        \
  template(java_util_Properties,                      "java/util/Properties")                     \
  template(java_util_Vector,                          "java/util/Vector")                         \
  template(java_util_AbstractList,                    "java/util/AbstractList")                   \
  template(java_util_Hashtable,                       "java/util/Hashtable")                      \
  template(java_lang_Compiler,                        "java/lang/Compiler")                       \
  template(jdk_internal_misc_Signal,                  "jdk/internal/misc/Signal")                 \
  template(jdk_internal_util_Preconditions,           "jdk/internal/util/Preconditions")          \
  template(java_lang_AssertionStatusDirectives,       "java/lang/AssertionStatusDirectives")      \
  template(getBootClassPathEntryForClass_name,        "getBootClassPathEntryForClass")            \
  template(jdk_internal_vm_PostVMInitHook,            "jdk/internal/vm/PostVMInitHook")           \
  template(sun_net_www_ParseUtil,                     "sun/net/www/ParseUtil")                    \
  template(java_util_Iterator,                        "java/util/Iterator")                       \
  template(java_lang_Record,                          "java/lang/Record")                         \
  template(sun_instrument_InstrumentationImpl,        "sun/instrument/InstrumentationImpl")       \
                                                                                                  \
  template(jdk_internal_loader_NativeLibraries,       "jdk/internal/loader/NativeLibraries")      \
  template(jdk_internal_loader_BuiltinClassLoader,    "jdk/internal/loader/BuiltinClassLoader")   \
  template(jdk_internal_loader_ClassLoaders_AppClassLoader,      "jdk/internal/loader/ClassLoaders$AppClassLoader")      \
  template(jdk_internal_loader_ClassLoaders_PlatformClassLoader, "jdk/internal/loader/ClassLoaders$PlatformClassLoader") \
                                                                                                  \
  /* Java runtime version access */                                                               \
  template(java_lang_VersionProps,                    "java/lang/VersionProps")                   \
  template(java_version_name,                         "java_version")                             \
  template(java_runtime_name_name,                    "java_runtime_name")                        \
  template(java_runtime_version_name,                 "java_runtime_version")                     \
  template(java_runtime_vendor_version_name,          "VENDOR_VERSION")                           \
  template(java_runtime_vendor_vm_bug_url_name,       "VENDOR_URL_VM_BUG")                        \
                                                                                                  \
  /* system initialization */                                                                     \
  template(initPhase1_name,                           "initPhase1")                               \
  template(initPhase2_name,                           "initPhase2")                               \
  template(initPhase3_name,                           "initPhase3")                               \
  template(java_lang_module_init_signature,           "(Ljava/lang/ClassLoader;Ljava/lang/String;)V") \
                                                                                                  \
  /* class file format tags */                                                                    \
  template(tag_source_file,                           "SourceFile")                               \
  template(tag_inner_classes,                         "InnerClasses")                             \
  template(tag_nest_members,                          "NestMembers")                              \
  template(tag_nest_host,                             "NestHost")                                 \
  template(tag_constant_value,                        "ConstantValue")                            \
  template(tag_code,                                  "Code")                                     \
  template(tag_exceptions,                            "Exceptions")                               \
  template(tag_line_number_table,                     "LineNumberTable")                          \
  template(tag_local_variable_table,                  "LocalVariableTable")                       \
  template(tag_local_variable_type_table,             "LocalVariableTypeTable")                   \
  template(tag_method_parameters,                     "MethodParameters")                         \
  template(tag_stack_map_table,                       "StackMapTable")                            \
  template(tag_synthetic,                             "Synthetic")                                \
  template(tag_deprecated,                            "Deprecated")                               \
  template(tag_source_debug_extension,                "SourceDebugExtension")                     \
  template(tag_signature,                             "Signature")                                \
  template(tag_record,                                "Record")                                   \
  template(tag_runtime_visible_annotations,           "RuntimeVisibleAnnotations")                \
  template(tag_runtime_invisible_annotations,         "RuntimeInvisibleAnnotations")              \
  template(tag_runtime_visible_parameter_annotations, "RuntimeVisibleParameterAnnotations")       \
  template(tag_runtime_invisible_parameter_annotations,"RuntimeInvisibleParameterAnnotations")    \
  template(tag_annotation_default,                    "AnnotationDefault")                        \
  template(tag_runtime_visible_type_annotations,      "RuntimeVisibleTypeAnnotations")            \
  template(tag_runtime_invisible_type_annotations,    "RuntimeInvisibleTypeAnnotations")          \
  template(tag_enclosing_method,                      "EnclosingMethod")                          \
  template(tag_bootstrap_methods,                     "BootstrapMethods")                         \
  template(tag_permitted_subclasses,                  "PermittedSubclasses")                      \
                                                                                                  \
  /* exception klasses: at least all exceptions thrown by the VM have entries here */             \
  template(java_lang_ArithmeticException,             "java/lang/ArithmeticException")            \
  template(java_lang_ArrayIndexOutOfBoundsException,  "java/lang/ArrayIndexOutOfBoundsException") \
  template(java_lang_ArrayStoreException,             "java/lang/ArrayStoreException")            \
  template(java_lang_ClassCastException,              "java/lang/ClassCastException")             \
  template(java_lang_ClassNotFoundException,          "java/lang/ClassNotFoundException")         \
  template(java_lang_CloneNotSupportedException,      "java/lang/CloneNotSupportedException")     \
  template(java_lang_IllegalAccessException,          "java/lang/IllegalAccessException")         \
  template(java_lang_IllegalArgumentException,        "java/lang/IllegalArgumentException")       \
  template(java_lang_IllegalStateException,           "java/lang/IllegalStateException")          \
  template(java_lang_IllegalMonitorStateException,    "java/lang/IllegalMonitorStateException")   \
  template(java_lang_IllegalThreadStateException,     "java/lang/IllegalThreadStateException")    \
  template(java_lang_IndexOutOfBoundsException,       "java/lang/IndexOutOfBoundsException")      \
  template(java_lang_InstantiationException,          "java/lang/InstantiationException")         \
  template(java_lang_InstantiationError,              "java/lang/InstantiationError")             \
  template(java_lang_InterruptedException,            "java/lang/InterruptedException")           \
  template(java_lang_BootstrapMethodError,            "java/lang/BootstrapMethodError")           \
  template(java_lang_LinkageError,                    "java/lang/LinkageError")                   \
  template(java_lang_NegativeArraySizeException,      "java/lang/NegativeArraySizeException")     \
  template(java_lang_NoSuchFieldException,            "java/lang/NoSuchFieldException")           \
  template(java_lang_NoSuchMethodException,           "java/lang/NoSuchMethodException")          \
  template(java_lang_NullPointerException,            "java/lang/NullPointerException")           \
  template(java_lang_StringIndexOutOfBoundsException, "java/lang/StringIndexOutOfBoundsException")\
  template(java_lang_UnsupportedOperationException,   "java/lang/UnsupportedOperationException")  \
  template(java_lang_InvalidClassException,           "java/lang/InvalidClassException")          \
  template(java_lang_reflect_InvocationTargetException, "java/lang/reflect/InvocationTargetException") \
  template(java_lang_Exception,                       "java/lang/Exception")                      \
  template(java_lang_RuntimeException,                "java/lang/RuntimeException")               \
  template(java_io_IOException,                       "java/io/IOException")                      \
  template(java_security_PrivilegedActionException,   "java/security/PrivilegedActionException")  \
                                                                                                  \
  /* error klasses: at least all errors thrown by the VM have entries here */                     \
  template(java_lang_AbstractMethodError,             "java/lang/AbstractMethodError")            \
  template(java_lang_ClassCircularityError,           "java/lang/ClassCircularityError")          \
  template(java_lang_ClassFormatError,                "java/lang/ClassFormatError")               \
  template(java_lang_UnsupportedClassVersionError,    "java/lang/UnsupportedClassVersionError")   \
  template(java_lang_Error,                           "java/lang/Error")                          \
  template(java_lang_ExceptionInInitializerError,     "java/lang/ExceptionInInitializerError")    \
  template(java_lang_IllegalAccessError,              "java/lang/IllegalAccessError")             \
  template(java_lang_IncompatibleClassChangeError,    "java/lang/IncompatibleClassChangeError")   \
  template(java_lang_InternalError,                   "java/lang/InternalError")                  \
  template(java_lang_NoClassDefFoundError,            "java/lang/NoClassDefFoundError")           \
  template(java_lang_NoSuchFieldError,                "java/lang/NoSuchFieldError")               \
  template(java_lang_NoSuchMethodError,               "java/lang/NoSuchMethodError")              \
  template(java_lang_OutOfMemoryError,                "java/lang/OutOfMemoryError")               \
  template(java_lang_UnsatisfiedLinkError,            "java/lang/UnsatisfiedLinkError")           \
  template(java_lang_VerifyError,                     "java/lang/VerifyError")                    \
  template(java_lang_SecurityException,               "java/lang/SecurityException")              \
  template(java_lang_VirtualMachineError,             "java/lang/VirtualMachineError")            \
  template(java_lang_StackOverflowError,              "java/lang/StackOverflowError")             \
  template(java_lang_StackTraceElement,               "java/lang/StackTraceElement")              \
                                                                                                  \
  /* Concurrency support */                                                                       \
  template(java_util_concurrent_locks_AbstractOwnableSynchronizer,           "java/util/concurrent/locks/AbstractOwnableSynchronizer") \
  template(java_util_concurrent_atomic_AtomicIntegerFieldUpdater_Impl,       "java/util/concurrent/atomic/AtomicIntegerFieldUpdater$AtomicIntegerFieldUpdaterImpl") \
  template(java_util_concurrent_atomic_AtomicLongFieldUpdater_CASUpdater,    "java/util/concurrent/atomic/AtomicLongFieldUpdater$CASUpdater") \
  template(java_util_concurrent_atomic_AtomicLongFieldUpdater_LockedUpdater, "java/util/concurrent/atomic/AtomicLongFieldUpdater$LockedUpdater") \
  template(java_util_concurrent_atomic_AtomicReferenceFieldUpdater_Impl,     "java/util/concurrent/atomic/AtomicReferenceFieldUpdater$AtomicReferenceFieldUpdaterImpl") \
  template(jdk_internal_vm_annotation_Contended_signature,                   "Ljdk/internal/vm/annotation/Contended;")    \
  template(jdk_internal_vm_annotation_ReservedStackAccess_signature,         "Ljdk/internal/vm/annotation/ReservedStackAccess;") \
  template(jdk_internal_ValueBased_signature,                                "Ljdk/internal/ValueBased;") \
                                                                                                  \
  /* class symbols needed by intrinsics */                                                        \
  VM_INTRINSICS_DO(VM_INTRINSIC_IGNORE, template, VM_SYMBOL_IGNORE, VM_SYMBOL_IGNORE, VM_ALIAS_IGNORE) \
                                                                                                  \
  /* Support for reflection based on dynamic bytecode generation (JDK 1.4 and above) */           \
                                                                                                  \
  template(jdk_internal_reflect,                      "jdk/internal/reflect")                     \
  template(reflect_MagicAccessorImpl,                 "jdk/internal/reflect/MagicAccessorImpl")       \
  template(reflect_MethodAccessorImpl,                "jdk/internal/reflect/MethodAccessorImpl")      \
  template(reflect_ConstructorAccessorImpl,           "jdk/internal/reflect/ConstructorAccessorImpl") \
  template(reflect_DelegatingClassLoader,             "jdk/internal/reflect/DelegatingClassLoader")   \
  template(reflect_Reflection,                        "jdk/internal/reflect/Reflection")              \
  template(reflect_CallerSensitive,                   "jdk/internal/reflect/CallerSensitive")         \
  template(reflect_CallerSensitive_signature,         "Ljdk/internal/reflect/CallerSensitive;")       \
  template(reflect_NativeConstructorAccessorImpl,     "jdk/internal/reflect/NativeConstructorAccessorImpl")\
  template(checkedExceptions_name,                    "checkedExceptions")                        \
  template(clazz_name,                                "clazz")                                    \
  template(exceptionTypes_name,                       "exceptionTypes")                           \
  template(modifiers_name,                            "modifiers")                                \
  template(invokeBasic_name,                          "invokeBasic")                              \
  template(linkToVirtual_name,                        "linkToVirtual")                            \
  template(linkToStatic_name,                         "linkToStatic")                             \
  template(linkToSpecial_name,                        "linkToSpecial")                            \
  template(linkToInterface_name,                      "linkToInterface")                          \
  template(linkToNative_name,                         "linkToNative")                             \
  template(compiledLambdaForm_name,                   "<compiledLambdaForm>")  /*fake name*/      \
  template(star_name,                                 "*") /*not really a name*/                  \
  template(invoke_name,                               "invoke")                                   \
  template(parameterTypes_name,                       "parameterTypes")                           \
  template(returnType_name,                           "returnType")                               \
  template(signature_name,                            "signature")                                \
  template(slot_name,                                 "slot")                                     \
  template(trusted_final_name,                        "trustedFinal")                             \
  template(blackhole_name,                            "<blackhole>")  /*fake name*/               \
                                                                                                  \
  /* Support for annotations (JDK 1.5 and above) */                                               \
                                                                                                  \
  template(annotations_name,                          "annotations")                              \
  template(index_name,                                "index")                                    \
  template(executable_name,                           "executable")                               \
  template(parameter_annotations_name,                "parameterAnnotations")                     \
  template(annotation_default_name,                   "annotationDefault")                        \
  template(reflect_ConstantPool,                      "jdk/internal/reflect/ConstantPool")        \
  template(reflect_UnsafeStaticFieldAccessorImpl,     "jdk/internal/reflect/UnsafeStaticFieldAccessorImpl")\
  template(base_name,                                 "base")                                     \
  /* Type Annotations (JDK 8 and above) */                                                        \
  template(type_annotations_name,                     "typeAnnotations")                          \
                                                                                                  \
  /* Intrinsic Annotation (JDK 9 and above) */                                                    \
  template(jdk_internal_vm_annotation_DontInline_signature,  "Ljdk/internal/vm/annotation/DontInline;")  \
  template(jdk_internal_vm_annotation_ForceInline_signature, "Ljdk/internal/vm/annotation/ForceInline;") \
  template(jdk_internal_vm_annotation_Hidden_signature,      "Ljdk/internal/vm/annotation/Hidden;") \
  template(jdk_internal_misc_Scoped_signature,               "Ljdk/internal/misc/ScopedMemoryAccess$Scoped;") \
  template(jdk_internal_vm_annotation_IntrinsicCandidate_signature, "Ljdk/internal/vm/annotation/IntrinsicCandidate;") \
  template(jdk_internal_vm_annotation_Stable_signature,      "Ljdk/internal/vm/annotation/Stable;") \
  /* Support for JSR 292 & invokedynamic (JDK 1.7 and above) */                                   \
  template(java_lang_invoke_CallSite,                 "java/lang/invoke/CallSite")                \
  template(java_lang_invoke_ConstantCallSite,         "java/lang/invoke/ConstantCallSite")        \
  template(java_lang_invoke_DirectMethodHandle,       "java/lang/invoke/DirectMethodHandle")      \
  template(java_lang_invoke_MutableCallSite,          "java/lang/invoke/MutableCallSite")         \
  template(java_lang_invoke_VolatileCallSite,         "java/lang/invoke/VolatileCallSite")        \
  template(java_lang_invoke_MethodHandle,             "java/lang/invoke/MethodHandle")            \
  template(java_lang_invoke_VarHandle,                "java/lang/invoke/VarHandle")               \
  template(java_lang_invoke_MethodType,               "java/lang/invoke/MethodType")              \
  template(java_lang_invoke_MethodType_signature,     "Ljava/lang/invoke/MethodType;")            \
  template(java_lang_invoke_ResolvedMethodName_signature, "Ljava/lang/invoke/ResolvedMethodName;")\
  template(java_lang_invoke_MemberName_signature,     "Ljava/lang/invoke/MemberName;")            \
  template(java_lang_invoke_LambdaForm_signature,     "Ljava/lang/invoke/LambdaForm;")            \
  template(java_lang_invoke_MethodHandle_signature,   "Ljava/lang/invoke/MethodHandle;")          \
  /* internal classes known only to the JVM: */                                                   \
  template(java_lang_invoke_MemberName,               "java/lang/invoke/MemberName")              \
  template(java_lang_invoke_ResolvedMethodName,       "java/lang/invoke/ResolvedMethodName")      \
  template(java_lang_invoke_MethodHandleNatives,      "java/lang/invoke/MethodHandleNatives")     \
  template(java_lang_invoke_MethodHandleNatives_CallSiteContext, "java/lang/invoke/MethodHandleNatives$CallSiteContext") \
  template(java_lang_invoke_LambdaForm,               "java/lang/invoke/LambdaForm")              \
  template(java_lang_invoke_InjectedProfile_signature, "Ljava/lang/invoke/InjectedProfile;")      \
  template(java_lang_invoke_LambdaForm_Compiled_signature, "Ljava/lang/invoke/LambdaForm$Compiled;") \
  template(java_lang_invoke_MethodHandleNatives_CallSiteContext_signature, "Ljava/lang/invoke/MethodHandleNatives$CallSiteContext;") \
  /* internal up-calls made only by the JVM, via class sun.invoke.MethodHandleNatives: */         \
  template(findMethodHandleType_name,                 "findMethodHandleType")                     \
  template(findMethodHandleType_signature,       "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;") \
  template(invokeExact_name,                          "invokeExact")                              \
  template(linkMethodHandleConstant_name,             "linkMethodHandleConstant")                 \
  template(linkMethodHandleConstant_signature, "(Ljava/lang/Class;ILjava/lang/Class;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;") \
  template(linkMethod_name,                           "linkMethod")                               \
  template(linkMethod_signature, "(Ljava/lang/Class;ILjava/lang/Class;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/invoke/MemberName;") \
  template(linkDynamicConstant_name,                  "linkDynamicConstant")                      \
  template(linkDynamicConstant_signature, "(Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;") \
  template(linkCallSite_name,                         "linkCallSite")                             \
  template(linkCallSite_signature, "(Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/invoke/MemberName;") \
  template(setTargetNormal_name,                      "setTargetNormal")                          \
  template(setTargetVolatile_name,                    "setTargetVolatile")                        \
  template(setTarget_signature,                       "(Ljava/lang/invoke/MethodHandle;)V")       \
  template(DEFAULT_CONTEXT_name,                      "DEFAULT_CONTEXT")                          \
  NOT_LP64(  do_alias(intptr_signature,               int_signature)  )                           \
  LP64_ONLY( do_alias(intptr_signature,               long_signature) )                           \
  /* Foreign API Support */                                                                                          \
  template(jdk_internal_invoke_NativeEntryPoint,                 "jdk/internal/invoke/NativeEntryPoint")           \
  template(jdk_internal_invoke_NativeEntryPoint_signature,       "Ljdk/internal/invoke/NativeEntryPoint;")         \
  template(jdk_incubator_foreign_MemoryAccess,       "jdk/incubator/foreign/MemoryAccess")        \
                                                                                                  \
  /* Support for JVMCI */                                                                         \
  JVMCI_VM_SYMBOLS_DO(template, do_alias)                                                         \
                                                                                                  \
  template(java_lang_StackWalker,                     "java/lang/StackWalker")                    \
  template(java_lang_StackFrameInfo,                  "java/lang/StackFrameInfo")                 \
  template(java_lang_LiveStackFrameInfo,              "java/lang/LiveStackFrameInfo")             \
  template(java_lang_StackStreamFactory_AbstractStackWalker, "java/lang/StackStreamFactory$AbstractStackWalker") \
  template(doStackWalk_signature,                     "(JIIII)Ljava/lang/Object;")                \
  template(asPrimitive_name,                          "asPrimitive")                              \
  template(asPrimitive_int_signature,                 "(I)Ljava/lang/LiveStackFrame$PrimitiveSlot;") \
  template(asPrimitive_long_signature,                "(J)Ljava/lang/LiveStackFrame$PrimitiveSlot;") \
                                                                                                  \
  /* common method and field names */                                                             \
  template(object_initializer_name,                   "<init>")                                   \
  template(class_initializer_name,                    "<clinit>")                                 \
  template(println_name,                              "println")                                  \
  template(printStackTrace_name,                      "printStackTrace")                          \
  template(main_name,                                 "main")                                     \
  template(name_name,                                 "name")                                     \
  template(priority_name,                             "priority")                                 \
  template(stillborn_name,                            "stillborn")                                \
  template(group_name,                                "group")                                    \
  template(daemon_name,                               "daemon")                                   \
  template(run_method_name,                           "run")                                      \
  template(exit_method_name,                          "exit")                                     \
  template(add_method_name,                           "add")                                      \
  template(remove_method_name,                        "remove")                                   \
  template(parent_name,                               "parent")                                   \
  template(threads_name,                              "threads")                                  \
  template(groups_name,                               "groups")                                   \
  template(maxPriority_name,                          "maxPriority")                              \
  template(destroyed_name,                            "destroyed")                                \
  template(nthreads_name,                             "nthreads")                                 \
  template(ngroups_name,                              "ngroups")                                  \
  template(shutdown_name,                             "shutdown")                                 \
  template(finalize_method_name,                      "finalize")                                 \
  template(reference_lock_name,                       "lock")                                     \
  template(reference_discovered_name,                 "discovered")                               \
  template(run_finalization_name,                     "runFinalization")                          \
  template(dispatchUncaughtException_name,            "dispatchUncaughtException")                \
  template(loadClass_name,                            "loadClass")                                \
  template(get_name,                                  "get")                                      \
  template(refersTo0_name,                            "refersTo0")                                \
  template(put_name,                                  "put")                                      \
  template(type_name,                                 "type")                                     \
  template(findNative_name,                           "findNative")                               \
  template(deadChild_name,                            "deadChild")                                \
  template(getFromClass_name,                         "getFromClass")                             \
  template(dispatch_name,                             "dispatch")                                 \
  template(getPlatformClassLoader_name,               "getPlatformClassLoader")                   \
  template(getSystemClassLoader_name,                 "getSystemClassLoader")                     \
  template(fillInStackTrace_name,                     "fillInStackTrace")                         \
  template(getCause_name,                             "getCause")                                 \
  template(initCause_name,                            "initCause")                                \
  template(getProperty_name,                          "getProperty")                              \
  template(context_name,                              "context")                                  \
  template(contextClassLoader_name,                   "contextClassLoader")                       \
  template(inheritedAccessControlContext_name,        "inheritedAccessControlContext")            \
  template(getClassContext_name,                      "getClassContext")                          \
  template(wait_name,                                 "wait")                                     \
  template(checkPackageAccess_name,                   "checkPackageAccess")                       \
  template(newInstance0_name,                         "newInstance0")                             \
  template(forName_name,                              "forName")                                  \
  template(forName0_name,                             "forName0")                                 \
  template(isJavaIdentifierStart_name,                "isJavaIdentifierStart")                    \
  template(isJavaIdentifierPart_name,                 "isJavaIdentifierPart")                     \
  template(cache_field_name,                          "cache")                                    \
  template(value_name,                                "value")                                    \
  template(compact_strings_name,                      "COMPACT_STRINGS")                          \
  template(numberOfLeadingZeros_name,                 "numberOfLeadingZeros")                     \
  template(numberOfTrailingZeros_name,                "numberOfTrailingZeros")                    \
  template(bitCount_name,                             "bitCount")                                 \
  template(profile_name,                              "profile")                                  \
  template(equals_name,                               "equals")                                   \
  template(length_name,                               "length")                                   \
  template(target_name,                               "target")                                   \
  template(toString_name,                             "toString")                                 \
  template(values_name,                               "values")                                   \
  template(receiver_name,                             "receiver")                                 \
  template(vmtarget_name,                             "vmtarget")                                 \
  template(vmholder_name,                             "vmholder")                                 \
  template(method_name,                               "method")                                   \
  template(vmindex_name,                              "vmindex")                                  \
  template(vmcount_name,                              "vmcount")                                  \
  template(flags_name,                                "flags")                                    \
  template(basicType_name,                            "basicType")                                \
  template(append_name,                               "append")                                   \
  template(klass_name,                                "klass")                                    \
  template(array_klass_name,                          "array_klass")                              \
  template(mid_name,                                  "mid")                                      \
  template(cpref_name,                                "cpref")                                    \
  template(version_name,                              "version")                                  \
  template(methodName_name,                           "methodName")                               \
  template(fileName_name,                             "fileName")                                 \
  template(lineNumber_name,                           "lineNumber")                               \
  template(oop_size_name,                             "oop_size")                                 \
  template(static_oop_field_count_name,               "static_oop_field_count")                   \
  template(protection_domain_name,                    "protection_domain")                        \
  template(signers_name,                              "signers_name")                             \
  template(source_file_name,                          "source_file")                              \
  template(loader_data_name,                          "loader_data")                              \
  template(vmdependencies_name,                       "vmdependencies")                           \
  template(last_cleanup_name,                         "last_cleanup")                             \
  template(loader_name,                               "loader")                                   \
  template(getModule_name,                            "getModule")                                \
  template(input_stream_void_signature,               "(Ljava/io/InputStream;)V")                 \
  template(input_stream_signature,                    "Ljava/io/InputStream;")                    \
  template(print_stream_signature,                    "Ljava/io/PrintStream;")                    \
  template(security_manager_signature,                "Ljava/lang/SecurityManager;")              \
  template(defineOrCheckPackage_name,                 "defineOrCheckPackage")                     \
  template(defineOrCheckPackage_signature,            "(Ljava/lang/String;Ljava/util/jar/Manifest;Ljava/net/URL;)Ljava/lang/Package;") \
  template(fileToEncodedURL_name,                     "fileToEncodedURL")                         \
  template(fileToEncodedURL_signature,                "(Ljava/io/File;)Ljava/net/URL;")           \
  template(getProtectionDomain_name,                  "getProtectionDomain")                      \
  template(getProtectionDomain_signature,             "(Ljava/security/CodeSource;)Ljava/security/ProtectionDomain;") \
  template(java_lang_Integer_array_signature,         "[Ljava/lang/Integer;")                     \
  template(java_lang_Long_array_signature,            "[Ljava/lang/Long;")                        \
  template(java_lang_Character_array_signature,       "[Ljava/lang/Character;")                   \
  template(java_lang_Short_array_signature,           "[Ljava/lang/Short;")                       \
  template(java_lang_Byte_array_signature,            "[Ljava/lang/Byte;")                        \
  template(java_lang_Boolean_signature,               "Ljava/lang/Boolean;")                      \
  template(url_code_signer_array_void_signature,      "(Ljava/net/URL;[Ljava/security/CodeSigner;)V") \
  template(module_entry_name,                         "module_entry")                             \
  template(resolved_references_name,                  "<resolved_references>")                    \
  template(init_lock_name,                            "<init_lock>")                              \
  template(address_size_name,                         "ADDRESS_SIZE0")                            \
  template(page_size_name,                            "PAGE_SIZE")                                \
  template(big_endian_name,                           "BIG_ENDIAN")                               \
  template(use_unaligned_access_name,                 "UNALIGNED_ACCESS")                         \
  template(data_cache_line_flush_size_name,           "DATA_CACHE_LINE_FLUSH_SIZE")               \
  template(during_unsafe_access_name,                 "during_unsafe_access")                     \
  template(checkIndex_name,                           "checkIndex")                               \
                                                                                                  \
  /* name symbols needed by intrinsics */                                                         \
  VM_INTRINSICS_DO(VM_INTRINSIC_IGNORE, VM_SYMBOL_IGNORE, template, VM_SYMBOL_IGNORE, VM_ALIAS_IGNORE) \
                                                                                                  \
  /* common signatures names */                                                                   \
  template(void_method_signature,                     "()V")                                      \
  template(void_boolean_signature,                    "()Z")                                      \
  template(void_byte_signature,                       "()B")                                      \
  template(void_char_signature,                       "()C")                                      \
  template(void_short_signature,                      "()S")                                      \
  template(void_int_signature,                        "()I")                                      \
  template(void_long_signature,                       "()J")                                      \
  template(void_float_signature,                      "()F")                                      \
  template(void_double_signature,                     "()D")                                      \
  template(bool_void_signature,                       "(Z)V")                                     \
  template(int_void_signature,                        "(I)V")                                     \
  template(int_int_signature,                         "(I)I")                                     \
  template(char_char_signature,                       "(C)C")                                     \
  template(short_short_signature,                     "(S)S")                                     \
  template(int_bool_signature,                        "(I)Z")                                     \
  template(float_int_signature,                       "(F)I")                                     \
  template(double_long_signature,                     "(D)J")                                     \
  template(double_double_signature,                   "(D)D")                                     \
  template(float_float_signature,                     "(F)F")                                     \
  template(int_float_signature,                       "(I)F")                                     \
  template(long_int_signature,                        "(J)I")                                     \
  template(long_long_signature,                       "(J)J")                                     \
  template(long_double_signature,                     "(J)D")                                     \
  template(long_void_signature,                       "(J)V")                                     \
  template(byte_signature,                            "B")                                        \
  template(char_signature,                            "C")                                        \
  template(double_signature,                          "D")                                        \
  template(float_signature,                           "F")                                        \
  template(int_signature,                             "I")                                        \
  template(long_signature,                            "J")                                        \
  template(short_signature,                           "S")                                        \
  template(bool_signature,                            "Z")                                        \
  template(void_signature,                            "V")                                        \
  template(bool_array_signature,                      "[Z")                                       \
  template(byte_array_signature,                      "[B")                                       \
  template(char_array_signature,                      "[C")                                       \
  template(int_array_signature,                       "[I")                                       \
  template(long_array_signature,                      "[J")                                       \
  template(object_void_signature,                     "(Ljava/lang/Object;)V")                    \
  template(object_int_signature,                      "(Ljava/lang/Object;)I")                    \
  template(long_object_long_signature,                "(JLjava/lang/Object;)J")                   \
  template(object_boolean_signature,                  "(Ljava/lang/Object;)Z")                    \
  template(object_object_signature,                   "(Ljava/lang/Object;)Ljava/lang/Object;")   \
  template(string_void_signature,                     "(Ljava/lang/String;)V")                    \
  template(string_int_signature,                      "(Ljava/lang/String;)I")                    \
  template(throwable_signature,                       "Ljava/lang/Throwable;")                    \
  template(throwable_void_signature,                  "(Ljava/lang/Throwable;)V")                 \
  template(void_throwable_signature,                  "()Ljava/lang/Throwable;")                  \
  template(class_void_signature,                      "(Ljava/lang/Class;)V")                     \
  template(class_int_signature,                       "(Ljava/lang/Class;)I")                     \
  template(class_long_signature,                      "(Ljava/lang/Class;)J")                     \
  template(class_boolean_signature,                   "(Ljava/lang/Class;)Z")                     \
  template(throwable_throwable_signature,             "(Ljava/lang/Throwable;)Ljava/lang/Throwable;")             \
  template(throwable_string_void_signature,           "(Ljava/lang/Throwable;Ljava/lang/String;)V")               \
  template(string_array_void_signature,               "([Ljava/lang/String;)V")                                   \
  template(string_array_string_array_void_signature,  "([Ljava/lang/String;[Ljava/lang/String;)V")                \
  template(thread_throwable_void_signature,           "(Ljava/lang/Thread;Ljava/lang/Throwable;)V")               \
  template(thread_void_signature,                     "(Ljava/lang/Thread;)V")                                    \
  template(threadgroup_runnable_void_signature,       "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;)V")           \
  template(threadgroup_string_void_signature,         "(Ljava/lang/ThreadGroup;Ljava/lang/String;)V")             \
  template(string_class_signature,                    "(Ljava/lang/String;)Ljava/lang/Class;")                    \
  template(object_object_object_signature,            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;") \
  template(string_string_string_signature,            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;") \
  template(string_string_signature,                   "(Ljava/lang/String;)Ljava/lang/String;")                   \
  template(classloader_string_long_signature,         "(Ljava/lang/ClassLoader;Ljava/lang/String;)J")             \
  template(byte_array_void_signature,                 "([B)V")                                                    \
  template(char_array_void_signature,                 "([C)V")                                                    \
  template(int_int_void_signature,                    "(II)V")                                                    \
  template(long_long_void_signature,                  "(JJ)V")                                                    \
  template(void_classloader_signature,                "()Ljava/lang/ClassLoader;")                                \
  template(void_object_signature,                     "()Ljava/lang/Object;")                                     \
  template(void_class_signature,                      "()Ljava/lang/Class;")                                      \
  template(void_class_array_signature,                "()[Ljava/lang/Class;")                                     \
  template(void_string_signature,                     "()Ljava/lang/String;")                                     \
  template(void_module_signature,                     "()Ljava/lang/Module;")                                     \
  template(object_array_object_signature,             "([Ljava/lang/Object;)Ljava/lang/Object;")                  \
  template(object_object_array_object_signature,      "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")\
  template(exception_void_signature,                  "(Ljava/lang/Exception;)V")                                 \
  template(protectiondomain_signature,                "[Ljava/security/ProtectionDomain;")                        \
  template(accesscontrolcontext_signature,            "Ljava/security/AccessControlContext;")                     \
  template(class_protectiondomain_signature,          "(Ljava/lang/Class;Ljava/security/ProtectionDomain;)V")     \
  template(thread_signature,                          "Ljava/lang/Thread;")                                       \
  template(thread_array_signature,                    "[Ljava/lang/Thread;")                                      \
  template(threadgroup_signature,                     "Ljava/lang/ThreadGroup;")                                  \
  template(threadgroup_array_signature,               "[Ljava/lang/ThreadGroup;")                                 \
  template(class_array_signature,                     "[Ljava/lang/Class;")                                       \
  template(classloader_signature,                     "Ljava/lang/ClassLoader;")                                  \
  template(object_signature,                          "Ljava/lang/Object;")                                       \
  template(object_array_signature,                    "[Ljava/lang/Object;")                                      \
  template(class_signature,                           "Ljava/lang/Class;")                                        \
  template(string_signature,                          "Ljava/lang/String;")                                       \
  template(string_array_signature,                    "[Ljava/lang/String;")                                      \
  template(reference_signature,                       "Ljava/lang/ref/Reference;")                                \
  template(referencequeue_signature,                  "Ljava/lang/ref/ReferenceQueue;")                           \
  template(executable_signature,                      "Ljava/lang/reflect/Executable;")                           \
  template(module_signature,                          "Ljava/lang/Module;")                                       \
  template(concurrenthashmap_signature,               "Ljava/util/concurrent/ConcurrentHashMap;")                 \
  template(String_StringBuilder_signature,            "(Ljava/lang/String;)Ljava/lang/StringBuilder;")            \
  template(int_StringBuilder_signature,               "(I)Ljava/lang/StringBuilder;")                             \
  template(char_StringBuilder_signature,              "(C)Ljava/lang/StringBuilder;")                             \
  template(String_StringBuffer_signature,             "(Ljava/lang/String;)Ljava/lang/StringBuffer;")             \
  template(int_StringBuffer_signature,                "(I)Ljava/lang/StringBuffer;")                              \
  template(char_StringBuffer_signature,               "(C)Ljava/lang/StringBuffer;")                              \
  template(int_String_signature,                      "(I)Ljava/lang/String;")                                    \
  template(boolean_boolean_int_signature,             "(ZZ)I")                                                    \
  template(big_integer_shift_worker_signature,        "([I[IIII)V")                                               \
  template(reflect_method_signature,                  "Ljava/lang/reflect/Method;")                                                    \
  /* signature symbols needed by intrinsics */                                                                    \
  VM_INTRINSICS_DO(VM_INTRINSIC_IGNORE, VM_SYMBOL_IGNORE, VM_SYMBOL_IGNORE, template, VM_ALIAS_IGNORE)            \
                                                                                                                  \
  /* symbol aliases needed by intrinsics */                                                                       \
  VM_INTRINSICS_DO(VM_INTRINSIC_IGNORE, VM_SYMBOL_IGNORE, VM_SYMBOL_IGNORE, VM_SYMBOL_IGNORE, do_alias)           \
                                                                                                                  \
  /* returned by the C1 compiler in case there's not enough memory to allocate a new symbol*/                     \
  template(dummy_symbol,                              "illegal symbol")                                           \
                                                                                                                  \
  /* used by ClassFormatError when class name is not known yet */                                                 \
  template(unknown_class_name,                        "<Unknown>")                                                \
                                                                                                                  \
  /* JVM monitoring and management support */                                                                     \
  template(java_lang_StackTraceElement_array,          "[Ljava/lang/StackTraceElement;")                          \
  template(java_lang_management_ThreadState,           "java/lang/management/ThreadState")                        \
  template(java_lang_management_MemoryUsage,           "java/lang/management/MemoryUsage")                        \
  template(java_lang_management_ThreadInfo,            "java/lang/management/ThreadInfo")                         \
  template(jdk_internal_agent_Agent,                   "jdk/internal/agent/Agent")                                \
  template(sun_management_Sensor,                      "sun/management/Sensor")                                   \
  template(sun_management_ManagementFactoryHelper,     "sun/management/ManagementFactoryHelper")                  \
  template(com_sun_management_internal_DiagnosticCommandImpl,  "com/sun/management/internal/DiagnosticCommandImpl")     \
  template(com_sun_management_internal_GarbageCollectorExtImpl,"com/sun/management/internal/GarbageCollectorExtImpl")   \
  template(getDiagnosticCommandMBean_name,             "getDiagnosticCommandMBean")                               \
  template(getDiagnosticCommandMBean_signature,        "()Lcom/sun/management/DiagnosticCommandMBean;")           \
  template(getGcInfoBuilder_name,                      "getGcInfoBuilder")                                        \
  template(getGcInfoBuilder_signature,                 "()Lcom/sun/management/internal/GcInfoBuilder;")           \
  template(com_sun_management_GcInfo,                  "com/sun/management/GcInfo")                               \
  template(com_sun_management_GcInfo_constructor_signature, "(Lcom/sun/management/internal/GcInfoBuilder;JJJ[Ljava/lang/management/MemoryUsage;[Ljava/lang/management/MemoryUsage;[Ljava/lang/Object;)V") \
  template(createGCNotification_name,                  "createGCNotification")                                    \
  template(createGCNotification_signature,             "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/sun/management/GcInfo;)V") \
  template(createDiagnosticFrameworkNotification_name, "createDiagnosticFrameworkNotification")                   \
  template(trigger_name,                               "trigger")                                                 \
  template(clear_name,                                 "clear")                                                   \
  template(trigger_method_signature,                   "(ILjava/lang/management/MemoryUsage;)V")                  \
  template(startAgent_name,                            "startAgent")                                              \
  template(startRemoteAgent_name,                      "startRemoteManagementAgent")                              \
  template(startLocalAgent_name,                       "startLocalManagementAgent")                               \
  template(stopRemoteAgent_name,                       "stopRemoteManagementAgent")                               \
  template(getAgentStatus_name,                        "getManagementAgentStatus")                                \
  template(java_lang_management_ThreadInfo_constructor_signature, "(Ljava/lang/Thread;ILjava/lang/Object;Ljava/lang/Thread;JJJJ[Ljava/lang/StackTraceElement;)V") \
  template(java_lang_management_ThreadInfo_with_locks_constructor_signature, "(Ljava/lang/Thread;ILjava/lang/Object;Ljava/lang/Thread;JJJJ[Ljava/lang/StackTraceElement;[Ljava/lang/Object;[I[Ljava/lang/Object;)V") \
  template(long_long_long_long_void_signature,         "(JJJJ)V")                                                 \
  template(finalizer_histogram_klass,                  "java/lang/ref/FinalizerHistogram")                        \
  template(void_finalizer_histogram_entry_array_signature,  "()[Ljava/lang/ref/FinalizerHistogram$Entry;")                        \
  template(get_finalizer_histogram_name,               "getFinalizerHistogram")                                   \
  template(finalizer_histogram_entry_name_field,       "className")                                               \
  template(finalizer_histogram_entry_count_field,      "instanceCount")                                           \
                                                                                                                  \
  template(java_lang_management_MemoryPoolMXBean,      "java/lang/management/MemoryPoolMXBean")                   \
  template(java_lang_management_MemoryManagerMXBean,   "java/lang/management/MemoryManagerMXBean")                \
  template(java_lang_management_GarbageCollectorMXBean,"java/lang/management/GarbageCollectorMXBean")             \
  template(gcInfoBuilder_name,                         "gcInfoBuilder")                                           \
  template(createMemoryPool_name,                      "createMemoryPool")                                        \
  template(createMemoryManager_name,                   "createMemoryManager")                                     \
  template(createGarbageCollector_name,                "createGarbageCollector")                                  \
  template(createMemoryPool_signature,                 "(Ljava/lang/String;ZJJ)Ljava/lang/management/MemoryPoolMXBean;") \
  template(createMemoryManager_signature,              "(Ljava/lang/String;)Ljava/lang/management/MemoryManagerMXBean;") \
  template(createGarbageCollector_signature,           "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/management/GarbageCollectorMXBean;") \
  template(addThreadDumpForMonitors_name,              "addThreadDumpForMonitors")                                \
  template(addThreadDumpForSynchronizers_name,         "addThreadDumpForSynchronizers")                           \
  template(addThreadDumpForMonitors_signature,         "(Ljava/lang/management/ThreadInfo;[Ljava/lang/Object;[I)V") \
  template(addThreadDumpForSynchronizers_signature,    "(Ljava/lang/management/ThreadInfo;[Ljava/lang/Object;)V")   \
                                                                                                                  \
  /* JVMTI/java.lang.instrument support and VM Attach mechanism */                                                \
  template(jdk_internal_module_Modules,                "jdk/internal/module/Modules")                             \
  template(jdk_internal_vm_VMSupport,                  "jdk/internal/vm/VMSupport")                               \
  template(addReads_name,                              "addReads")                                                \
  template(addReads_signature,                         "(Ljava/lang/Module;Ljava/lang/Module;)V")                 \
  template(addExports_name,                            "addExports")                                              \
  template(addOpens_name,                              "addOpens")                                                \
  template(addExports_signature,                       "(Ljava/lang/Module;Ljava/lang/String;Ljava/lang/Module;)V") \
  template(addUses_name,                               "addUses")                                                 \
  template(addUses_signature,                          "(Ljava/lang/Module;Ljava/lang/Class;)V")                  \
  template(addProvides_name,                           "addProvides")                                             \
  template(addProvides_signature,                      "(Ljava/lang/Module;Ljava/lang/Class;Ljava/lang/Class;)V") \
  template(loadModule_name,                            "loadModule")                                              \
  template(loadModule_signature,                       "(Ljava/lang/String;)Ljava/lang/Module;")                  \
  template(transformedByAgent_name,                    "transformedByAgent")                                      \
  template(transformedByAgent_signature,               "(Ljava/lang/Module;)V")                                   \
  template(appendToClassPathForInstrumentation_name,   "appendToClassPathForInstrumentation")                     \
  do_alias(appendToClassPathForInstrumentation_signature, string_void_signature)                                  \
  template(serializePropertiesToByteArray_name,        "serializePropertiesToByteArray")                          \
  template(serializePropertiesToByteArray_signature,   "()[B")                                                    \
  template(serializeAgentPropertiesToByteArray_name,   "serializeAgentPropertiesToByteArray")                     \
  template(classRedefinedCount_name,                   "classRedefinedCount")                                     \
  template(classLoader_name,                           "classLoader")                                             \
  template(componentType_name,                         "componentType")                                           \
                                                                                                                  \
  /* forEachRemaining support */                                                                                  \
  template(java_util_stream_StreamsRangeIntSpliterator,          "java/util/stream/Streams$RangeIntSpliterator")  \
                                                                                                                  \
  /* jfr signatures */                                                                                            \
  JFR_TEMPLATES(template)                                                                                         \
                                                                                                                  \
  /* CDS */                                                                                                       \
  template(dumpSharedArchive,                               "dumpSharedArchive")                                  \
  template(dumpSharedArchive_signature,                     "(ZLjava/lang/String;)V")                             \
  template(generateLambdaFormHolderClasses,                 "generateLambdaFormHolderClasses")                    \
  template(generateLambdaFormHolderClasses_signature,       "([Ljava/lang/String;)[Ljava/lang/Object;")           \
  template(java_lang_invoke_Invokers_Holder,                "java/lang/invoke/Invokers$Holder")                   \
  template(java_lang_invoke_DirectMethodHandle_Holder,      "java/lang/invoke/DirectMethodHandle$Holder")         \
  template(java_lang_invoke_LambdaForm_Holder,              "java/lang/invoke/LambdaForm$Holder")                 \
  template(java_lang_invoke_DelegatingMethodHandle_Holder,  "java/lang/invoke/DelegatingMethodHandle$Holder")     \
  template(jdk_internal_loader_ClassLoaders,                "jdk/internal/loader/ClassLoaders")                   \
  template(jdk_internal_misc_CDS,                           "jdk/internal/misc/CDS")                              \
  template(java_util_concurrent_ConcurrentHashMap,          "java/util/concurrent/ConcurrentHashMap")             \
  template(java_util_ArrayList,                             "java/util/ArrayList")                                \
  template(toFileURL_name,                                  "toFileURL")                                          \
  template(toFileURL_signature,                             "(Ljava/lang/String;)Ljava/net/URL;")                 \
  template(url_void_signature,                              "(Ljava/net/URL;)V")                                  \
                                                                                                                  \
  /*end*/

// enum for figuring positions and size of Symbol::_vm_symbols[]
enum class vmSymbolID : int {
  // [FIRST_SID ... LAST_SID] is the iteration range for the *valid* symbols.
  // NO_SID is used to indicate an invalid symbol. Some implementation code
  // *may* read _vm_symbols[NO_SID], so it must be a valid array index.
  NO_SID = 0,                // exclusive lower limit

  #define VM_SYMBOL_ENUM(name, string) VM_SYMBOL_ENUM_NAME_(name),
  VM_SYMBOLS_DO(VM_SYMBOL_ENUM, VM_ALIAS_IGNORE)
  #undef VM_SYMBOL_ENUM

  SID_LIMIT,                 // exclusive upper limit

  #define VM_ALIAS_ENUM(name, def) VM_SYMBOL_ENUM_NAME_(name) = VM_SYMBOL_ENUM_NAME_(def),
  VM_SYMBOLS_DO(VM_SYMBOL_IGNORE, VM_ALIAS_ENUM)
  #undef VM_ALIAS_ENUM

  FIRST_SID = NO_SID + 1,    // inclusive lower limit
  LAST_SID = SID_LIMIT - 1,  // inclusive upper limit
};

ENUMERATOR_RANGE(vmSymbolID, vmSymbolID::FIRST_SID, vmSymbolID::LAST_SID)

class vmSymbols: AllStatic {
  friend class vmIntrinsics;
  friend class VMStructs;
  friend class JVMCIVMStructs;

  static const int NO_SID    = static_cast<int>(vmSymbolID::NO_SID);    // exclusive lower limit
  static const int FIRST_SID = static_cast<int>(vmSymbolID::FIRST_SID); // inclusive lower limit
  static const int LAST_SID  = static_cast<int>(vmSymbolID::FIRST_SID); // inclusive upper limit
  static const int SID_LIMIT = static_cast<int>(vmSymbolID::SID_LIMIT); // exclusive upper limit

 public:
  static constexpr bool is_valid_id(int id) {
    return (id >= FIRST_SID && id < SID_LIMIT);
  }
  static constexpr bool is_valid_id(vmSymbolID sid) {
    return is_valid_id(static_cast<int>(sid));
  }

  static constexpr vmSymbolID as_SID(int id) {
    assert(is_valid_id(id), "must be");
    return static_cast<vmSymbolID>(id);
  }

  static constexpr int as_int(vmSymbolID sid) {
    assert(is_valid_id(sid), "must be");
    return static_cast<int>(sid);
  }

  static constexpr int number_of_symbols() {
    static_assert(NO_SID == 0, "must be a valid array index");
    static_assert(FIRST_SID == 1, "must not be the same as NO_SID");
    return SID_LIMIT;
  }

  enum {
    log2_SID_LIMIT = 11         // checked by an assert at start-up
  };

 private:

  // Field signatures indexed by BasicType.
  static Symbol* _type_signatures[T_VOID+1];

 public:
  // Initialization
  static void initialize();
  // Accessing
  #define VM_SYMBOL_DECLARE(name, ignore)                 \
    static Symbol* name() {                               \
      return Symbol::_vm_symbols[static_cast<int>(VM_SYMBOL_ENUM_NAME(name))]; \
    }
  VM_SYMBOLS_DO(VM_SYMBOL_DECLARE, VM_SYMBOL_DECLARE)
  #undef VM_SYMBOL_DECLARE

  // Sharing support
  static void symbols_do(SymbolClosure* f);
  static void metaspace_pointers_do(MetaspaceClosure *it);
  static void serialize(SerializeClosure* soc);

  static Symbol* type_signature(BasicType t) {
    assert((uint)t < T_VOID+1, "range check");
    assert(_type_signatures[t] != NULL, "domain check");
    return _type_signatures[t];
  }

  static Symbol* symbol_at(vmSymbolID id) {
    return Symbol::vm_symbol_at(id);
  }

  // Returns symbol's vmSymbolID if one is assigned, else vmSymbolID::NO_SID.
  static vmSymbolID find_sid(const Symbol* symbol);
  static vmSymbolID find_sid(const char* symbol_name);

#ifndef PRODUCT
  // No need for this in the product:
  static const char* name_for(vmSymbolID sid);
#endif //PRODUCT
};

#endif // SHARE_CLASSFILE_VMSYMBOLS_HPP
