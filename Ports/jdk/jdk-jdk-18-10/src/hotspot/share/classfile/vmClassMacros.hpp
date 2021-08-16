/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_VMCLASSMACROS_HPP
#define SHARE_CLASSFILE_VMCLASSMACROS_HPP

// _VM_CLASS_ENUM - internal: should be used only by vmClass*.{hpp,cpp}
#define _VM_CLASS_ENUM(kname)    kname##_knum

#define VM_CLASS_ID(kname)      vmClassID::_VM_CLASS_ENUM(kname)

// VM_CLASSES_DO iterates the classes that are directly referenced
// by the VM, suhch as java.lang.Object and java.lang.String. These
// classes are resolved at VM bootstrap, before any Java code is executed,
// so no class loader is able to provide a different definition.
//
// Each VM class has a short klass name (like Object_klass),
// and a vmSymbol name (like java_lang_Object). Both of these can
// be used to find the vmClassID for this class. The following two
// macros will evaluate to the same value:
//
//    VM_CLASS_ID(Object_klass)
//    VM_CLASS_ID(java_lang_Object)
//
// The order of these definitions is significant: the classes are
// resolved by vmClasses::resolve_all() in this order. Changing the
// order may require careful restructuring of the VM start-up sequence.
//
#define VM_CLASSES_DO(do_klass)                                                                                 \
  /* well-known classes */                                                                                      \
  do_klass(Object_klass,                                java_lang_Object                                      ) \
  do_klass(String_klass,                                java_lang_String                                      ) \
  do_klass(Class_klass,                                 java_lang_Class                                       ) \
  do_klass(Cloneable_klass,                             java_lang_Cloneable                                   ) \
  do_klass(ClassLoader_klass,                           java_lang_ClassLoader                                 ) \
  do_klass(Serializable_klass,                          java_io_Serializable                                  ) \
  do_klass(System_klass,                                java_lang_System                                      ) \
  do_klass(Throwable_klass,                             java_lang_Throwable                                   ) \
  do_klass(Error_klass,                                 java_lang_Error                                       ) \
  do_klass(ThreadDeath_klass,                           java_lang_ThreadDeath                                 ) \
  do_klass(Exception_klass,                             java_lang_Exception                                   ) \
  do_klass(RuntimeException_klass,                      java_lang_RuntimeException                            ) \
  do_klass(SecurityManager_klass,                       java_lang_SecurityManager                             ) \
  do_klass(ProtectionDomain_klass,                      java_security_ProtectionDomain                        ) \
  do_klass(AccessControlContext_klass,                  java_security_AccessControlContext                    ) \
  do_klass(AccessController_klass,                      java_security_AccessController                        ) \
  do_klass(SecureClassLoader_klass,                     java_security_SecureClassLoader                       ) \
  do_klass(ClassNotFoundException_klass,                java_lang_ClassNotFoundException                      ) \
  do_klass(Record_klass,                                java_lang_Record                                      ) \
  do_klass(NoClassDefFoundError_klass,                  java_lang_NoClassDefFoundError                        ) \
  do_klass(LinkageError_klass,                          java_lang_LinkageError                                ) \
  do_klass(ClassCastException_klass,                    java_lang_ClassCastException                          ) \
  do_klass(ArrayStoreException_klass,                   java_lang_ArrayStoreException                         ) \
  do_klass(VirtualMachineError_klass,                   java_lang_VirtualMachineError                         ) \
  do_klass(InternalError_klass,                         java_lang_InternalError                               ) \
  do_klass(OutOfMemoryError_klass,                      java_lang_OutOfMemoryError                            ) \
  do_klass(StackOverflowError_klass,                    java_lang_StackOverflowError                          ) \
  do_klass(IllegalMonitorStateException_klass,          java_lang_IllegalMonitorStateException                ) \
  do_klass(Reference_klass,                             java_lang_ref_Reference                               ) \
                                                                                                                \
  /* ref klasses and set reference types */                                                                     \
  do_klass(SoftReference_klass,                         java_lang_ref_SoftReference                           ) \
  do_klass(WeakReference_klass,                         java_lang_ref_WeakReference                           ) \
  do_klass(FinalReference_klass,                        java_lang_ref_FinalReference                          ) \
  do_klass(PhantomReference_klass,                      java_lang_ref_PhantomReference                        ) \
  do_klass(Finalizer_klass,                             java_lang_ref_Finalizer                               ) \
                                                                                                                \
  do_klass(Thread_klass,                                java_lang_Thread                                      ) \
  do_klass(ThreadGroup_klass,                           java_lang_ThreadGroup                                 ) \
  do_klass(Properties_klass,                            java_util_Properties                                  ) \
  do_klass(Module_klass,                                java_lang_Module                                      ) \
  do_klass(reflect_AccessibleObject_klass,              java_lang_reflect_AccessibleObject                    ) \
  do_klass(reflect_Field_klass,                         java_lang_reflect_Field                               ) \
  do_klass(reflect_Parameter_klass,                     java_lang_reflect_Parameter                           ) \
  do_klass(reflect_Method_klass,                        java_lang_reflect_Method                              ) \
  do_klass(reflect_Constructor_klass,                   java_lang_reflect_Constructor                         ) \
                                                                                                                \
  /* NOTE: needed too early in bootstrapping process to have checks based on JDK version */                     \
  /* It's okay if this turns out to be NULL in non-1.4 JDKs. */                                                 \
  do_klass(reflect_MagicAccessorImpl_klass,             reflect_MagicAccessorImpl                             ) \
  do_klass(reflect_MethodAccessorImpl_klass,            reflect_MethodAccessorImpl                            ) \
  do_klass(reflect_ConstructorAccessorImpl_klass,       reflect_ConstructorAccessorImpl                       ) \
  do_klass(reflect_DelegatingClassLoader_klass,         reflect_DelegatingClassLoader                         ) \
  do_klass(reflect_ConstantPool_klass,                  reflect_ConstantPool                                  ) \
  do_klass(reflect_UnsafeStaticFieldAccessorImpl_klass, reflect_UnsafeStaticFieldAccessorImpl                 ) \
  do_klass(reflect_CallerSensitive_klass,               reflect_CallerSensitive                               ) \
  do_klass(reflect_NativeConstructorAccessorImpl_klass, reflect_NativeConstructorAccessorImpl                 ) \
                                                                                                                \
  /* support for dynamic typing; it's OK if these are NULL in earlier JDKs */                                   \
  do_klass(DirectMethodHandle_klass,                    java_lang_invoke_DirectMethodHandle                   ) \
  do_klass(MethodHandle_klass,                          java_lang_invoke_MethodHandle                         ) \
  do_klass(VarHandle_klass,                             java_lang_invoke_VarHandle                            ) \
  do_klass(MemberName_klass,                            java_lang_invoke_MemberName                           ) \
  do_klass(ResolvedMethodName_klass,                    java_lang_invoke_ResolvedMethodName                   ) \
  do_klass(MethodHandleNatives_klass,                   java_lang_invoke_MethodHandleNatives                  ) \
  do_klass(LambdaForm_klass,                            java_lang_invoke_LambdaForm                           ) \
  do_klass(MethodType_klass,                            java_lang_invoke_MethodType                           ) \
  do_klass(BootstrapMethodError_klass,                  java_lang_BootstrapMethodError                        ) \
  do_klass(CallSite_klass,                              java_lang_invoke_CallSite                             ) \
  do_klass(NativeEntryPoint_klass,                      jdk_internal_invoke_NativeEntryPoint                  ) \
  do_klass(Context_klass,                               java_lang_invoke_MethodHandleNatives_CallSiteContext  ) \
  do_klass(ConstantCallSite_klass,                      java_lang_invoke_ConstantCallSite                     ) \
  do_klass(MutableCallSite_klass,                       java_lang_invoke_MutableCallSite                      ) \
  do_klass(VolatileCallSite_klass,                      java_lang_invoke_VolatileCallSite                     ) \
  /* Note: MethodHandle must be first, and VolatileCallSite last in group */                                    \
                                                                                                                \
  do_klass(AssertionStatusDirectives_klass,             java_lang_AssertionStatusDirectives                   ) \
  do_klass(StringBuffer_klass,                          java_lang_StringBuffer                                ) \
  do_klass(StringBuilder_klass,                         java_lang_StringBuilder                               ) \
  do_klass(UnsafeConstants_klass,                       jdk_internal_misc_UnsafeConstants                     ) \
  do_klass(internal_Unsafe_klass,                       jdk_internal_misc_Unsafe                              ) \
  do_klass(module_Modules_klass,                        jdk_internal_module_Modules                           ) \
                                                                                                                \
  /* support for CDS */                                                                                         \
  do_klass(ByteArrayInputStream_klass,                  java_io_ByteArrayInputStream                          ) \
  do_klass(URL_klass,                                   java_net_URL                                          ) \
  do_klass(Jar_Manifest_klass,                          java_util_jar_Manifest                                ) \
  do_klass(jdk_internal_loader_BuiltinClassLoader_klass,jdk_internal_loader_BuiltinClassLoader                ) \
  do_klass(jdk_internal_loader_ClassLoaders_klass,      jdk_internal_loader_ClassLoaders                      ) \
  do_klass(jdk_internal_loader_ClassLoaders_AppClassLoader_klass,      jdk_internal_loader_ClassLoaders_AppClassLoader) \
  do_klass(jdk_internal_loader_ClassLoaders_PlatformClassLoader_klass, jdk_internal_loader_ClassLoaders_PlatformClassLoader) \
  do_klass(CodeSource_klass,                            java_security_CodeSource                              ) \
  do_klass(ConcurrentHashMap_klass,                     java_util_concurrent_ConcurrentHashMap                ) \
  do_klass(ArrayList_klass,                             java_util_ArrayList                                   ) \
                                                                                                                \
  do_klass(StackTraceElement_klass,                     java_lang_StackTraceElement                           ) \
                                                                                                                \
  /* It's okay if this turns out to be NULL in non-1.4 JDKs. */                                                 \
  do_klass(nio_Buffer_klass,                            java_nio_Buffer                                       ) \
                                                                                                                \
  /* Stack Walking */                                                                                           \
  do_klass(StackWalker_klass,                           java_lang_StackWalker                                 ) \
  do_klass(AbstractStackWalker_klass,                   java_lang_StackStreamFactory_AbstractStackWalker      ) \
  do_klass(StackFrameInfo_klass,                        java_lang_StackFrameInfo                              ) \
  do_klass(LiveStackFrameInfo_klass,                    java_lang_LiveStackFrameInfo                          ) \
                                                                                                                \
  /* support for stack dump lock analysis */                                                                    \
  do_klass(java_util_concurrent_locks_AbstractOwnableSynchronizer_klass, java_util_concurrent_locks_AbstractOwnableSynchronizer) \
                                                                                                                \
  /* boxing klasses */                                                                                          \
  do_klass(Boolean_klass,                               java_lang_Boolean                                     ) \
  do_klass(Character_klass,                             java_lang_Character                                   ) \
  do_klass(Float_klass,                                 java_lang_Float                                       ) \
  do_klass(Double_klass,                                java_lang_Double                                      ) \
  do_klass(Byte_klass,                                  java_lang_Byte                                        ) \
  do_klass(Short_klass,                                 java_lang_Short                                       ) \
  do_klass(Integer_klass,                               java_lang_Integer                                     ) \
  do_klass(Long_klass,                                  java_lang_Long                                        ) \
                                                                                                                \
  /* force inline of iterators */                                                                               \
  do_klass(Iterator_klass,                              java_util_Iterator                                    ) \
                                                                                                                \
  /* support for records */                                                                                     \
  do_klass(RecordComponent_klass,                       java_lang_reflect_RecordComponent                     ) \
                                                                                                                \
  /* support for vectors*/                                                                                      \
  do_klass(vector_VectorSupport_klass,                  jdk_internal_vm_vector_VectorSupport                  ) \
  do_klass(vector_VectorPayload_klass,                  jdk_internal_vm_vector_VectorPayload                  ) \
  do_klass(vector_Vector_klass,                         jdk_internal_vm_vector_Vector                         ) \
  do_klass(vector_VectorMask_klass,                     jdk_internal_vm_vector_VectorMask                     ) \
  do_klass(vector_VectorShuffle_klass,                  jdk_internal_vm_vector_VectorShuffle                  ) \
                                                                                                                \
  /*end*/

#endif // SHARE_CLASSFILE_VMCLASSMACROS_HPP

