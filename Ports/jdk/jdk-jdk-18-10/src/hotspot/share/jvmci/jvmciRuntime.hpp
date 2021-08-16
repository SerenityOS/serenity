/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JVMCI_JVMCIRUNTIME_HPP
#define SHARE_JVMCI_JVMCIRUNTIME_HPP

#include "jvm_io.h"
#include "code/nmethod.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "jvmci/jvmci.hpp"
#include "jvmci/jvmciExceptions.hpp"
#include "jvmci/jvmciObject.hpp"
#include "utilities/linkedlist.hpp"
#if INCLUDE_G1GC
#include "gc/g1/g1CardTable.hpp"
#endif // INCLUDE_G1GC

class JVMCIEnv;
class JVMCICompiler;
class JVMCICompileState;
class MetadataHandles;

// Encapsulates the JVMCI metadata for an nmethod.
// JVMCINMethodData objects are inlined into nmethods
// at nmethod::_jvmci_data_offset.
class JVMCINMethodData {
  friend class JVMCIVMStructs;
  // Index for the HotSpotNmethod mirror in the nmethod's oops table.
  // This is -1 if there is no mirror in the oops table.
  int _nmethod_mirror_index;

  // Is HotSpotNmethod.name non-null? If so, the value is
  // embedded in the end of this object.
  bool _has_name;

  // Address of the failed speculations list to which a speculation
  // is appended when it causes a deoptimization.
  FailedSpeculation** _failed_speculations;

  // A speculation id is a length (low 5 bits) and an index into
  // a jbyte array (i.e. 31 bits for a positive Java int).
  enum {
    // Keep in sync with HotSpotSpeculationEncoding.
    SPECULATION_LENGTH_BITS = 5,
    SPECULATION_LENGTH_MASK = (1 << SPECULATION_LENGTH_BITS) - 1
  };

public:
  // Computes the size of a JVMCINMethodData object
  static int compute_size(const char* nmethod_mirror_name) {
    int size = sizeof(JVMCINMethodData);
    if (nmethod_mirror_name != NULL) {
      size += (int) strlen(nmethod_mirror_name) + 1;
    }
    return size;
  }

  void initialize(int nmethod_mirror_index,
             const char* name,
             FailedSpeculation** failed_speculations);

  // Adds `speculation` to the failed speculations list.
  void add_failed_speculation(nmethod* nm, jlong speculation);

  // Gets the JVMCI name of the nmethod (which may be NULL).
  const char* name() { return _has_name ? (char*)(((address) this) + sizeof(JVMCINMethodData)) : NULL; }

  // Clears the HotSpotNmethod.address field in the  mirror. If nm
  // is dead, the HotSpotNmethod.entryPoint field is also cleared.
  void invalidate_nmethod_mirror(nmethod* nm);

  // Gets the mirror from nm's oops table.
  oop get_nmethod_mirror(nmethod* nm, bool phantom_ref);

  // Sets the mirror in nm's oops table.
  void set_nmethod_mirror(nmethod* nm, oop mirror);

  // Clears the mirror in nm's oops table.
  void clear_nmethod_mirror(nmethod* nm);
};

// A top level class that represents an initialized JVMCI runtime.
// There is one instance of this class per HotSpotJVMCIRuntime object.
class JVMCIRuntime: public CHeapObj<mtJVMCI> {
  friend class JVMCI;
 public:
  // Constants describing whether JVMCI wants to be able to adjust the compilation
  // level selected for a method by the VM compilation policy and if so, based on
  // what information about the method being schedule for compilation.
  enum CompLevelAdjustment {
     none = 0,             // no adjustment
     by_holder = 1,        // adjust based on declaring class of method
     by_full_signature = 2 // adjust based on declaring class, name and signature of method
  };

 private:

  enum InitState {
    uninitialized,
    being_initialized,
    fully_initialized
  };

  // Initialization state of this JVMCIRuntime.
  InitState _init_state;

  // A wrapper for a VM scoped JNI global handle (i.e. JVMCIEnv::make_global)
  // to a HotSpotJVMCIRuntime instance. This JNI global handle must never
  // be explicitly destroyed as it can be accessed in a racy way during
  // JVMCI shutdown. Furthermore, it will be reclaimed when
  // the VM or shared library JavaVM managing the handle dies.
  JVMCIObject _HotSpotJVMCIRuntime_instance;

  // Result of calling JNI_CreateJavaVM in the JVMCI shared library.
  // Must only be modified under JVMCI_lock.
  volatile JavaVM* _shared_library_javavm;

  // The HotSpot heap based runtime will have an id of -1 and the
  // JVMCI shared library runtime will have an id of 0.
  int _id;

  // Handles to Metadata objects.
  MetadataHandles* _metadata_handles;

  JVMCIObject create_jvmci_primitive_type(BasicType type, JVMCI_TRAPS);

  // Implementation methods for loading and constant pool access.
  static Klass* get_klass_by_name_impl(Klass*& accessing_klass,
                                       const constantPoolHandle& cpool,
                                       Symbol* klass_name,
                                       bool require_local);
  static Klass*   get_klass_by_index_impl(const constantPoolHandle& cpool,
                                          int klass_index,
                                          bool& is_accessible,
                                          Klass* loading_klass);
  static void   get_field_by_index_impl(InstanceKlass* loading_klass, fieldDescriptor& fd,
                                        int field_index);
  static Method*  get_method_by_index_impl(const constantPoolHandle& cpool,
                                           int method_index, Bytecodes::Code bc,
                                           InstanceKlass* loading_klass);

  // Helper methods
  static bool       check_klass_accessibility(Klass* accessing_klass, Klass* resolved_klass);
  static Method*    lookup_method(InstanceKlass*  accessor,
                                  Klass*  holder,
                                  Symbol*         name,
                                  Symbol*         sig,
                                  Bytecodes::Code bc,
                                  constantTag     tag);

 public:
  JVMCIRuntime(int id);

  int id() const        { return _id;   }

  // Ensures that a JVMCI shared library JavaVM exists for this runtime.
  // If the JavaVM was created by this call, then the thread-local JNI
  // interface pointer for the JavaVM is returned otherwise NULL is returned.
  JNIEnv* init_shared_library_javavm();

  // Determines if the JVMCI shared library JavaVM exists for this runtime.
  bool has_shared_library_javavm() { return _shared_library_javavm != NULL; }

  // Copies info about the JVMCI shared library JavaVM associated with this
  // runtime into `info` as follows:
  // {
  //     javaVM, // the {@code JavaVM*} value
  //     javaVM->functions->reserved0,
  //     javaVM->functions->reserved1,
  //     javaVM->functions->reserved2
  // }
  void init_JavaVM_info(jlongArray info, JVMCI_TRAPS);

  // Wrappers for calling Invocation Interface functions on the
  // JVMCI shared library JavaVM associated with this runtime.
  // These wrappers ensure all required thread state transitions are performed.
  jint AttachCurrentThread(JavaThread* thread, void **penv, void *args);
  jint AttachCurrentThreadAsDaemon(JavaThread* thread, void **penv, void *args);
  jint DetachCurrentThread(JavaThread* thread);
  jint GetEnv(JavaThread* thread, void **penv, jint version);

  // Compute offsets and construct any state required before executing JVMCI code.
  void initialize(JVMCIEnv* jvmciEnv);

  // Allocation and management of JNI global object handles
  // whose lifetime is scoped by this JVMCIRuntime. The lifetime
  // of these handles is the same as the JVMCI shared library JavaVM
  // associated with this JVMCIRuntime. These JNI handles are
  // used when creating a IndirectHotSpotObjectConstantImpl in the
  // shared library JavaVM.
  jobject make_global(const Handle& obj);
  void destroy_global(jobject handle);
  bool is_global_handle(jobject handle);

  // Allocation and management of metadata handles.
  jmetadata allocate_handle(const methodHandle& handle);
  jmetadata allocate_handle(const constantPoolHandle& handle);
  void release_handle(jmetadata handle);

  // Gets the HotSpotJVMCIRuntime instance for this runtime,
  // initializing it first if necessary.
  JVMCIObject get_HotSpotJVMCIRuntime(JVMCI_TRAPS);

  bool is_HotSpotJVMCIRuntime_initialized() {
    return _HotSpotJVMCIRuntime_instance.is_non_null();
  }

  // Gets the current HotSpotJVMCIRuntime instance for this runtime which
  // may be a "null" JVMCIObject value.
  JVMCIObject probe_HotSpotJVMCIRuntime() {
    return _HotSpotJVMCIRuntime_instance;
  }

  // Trigger initialization of HotSpotJVMCIRuntime through JVMCI.getRuntime()
  void initialize_JVMCI(JVMCI_TRAPS);

  // Explicitly initialize HotSpotJVMCIRuntime itself
  void initialize_HotSpotJVMCIRuntime(JVMCI_TRAPS);

  void call_getCompiler(TRAPS);

  // Shuts down this runtime by calling HotSpotJVMCIRuntime.shutdown().
  void shutdown();

  void bootstrap_finished(TRAPS);

  // Look up a klass by name from a particular class loader (the accessor's).
  // If require_local, result must be defined in that class loader, or NULL.
  // If !require_local, a result from remote class loader may be reported,
  // if sufficient class loader constraints exist such that initiating
  // a class loading request from the given loader is bound to return
  // the class defined in the remote loader (or throw an error).
  //
  // Return an unloaded klass if !require_local and no class at all is found.
  //
  // The CI treats a klass as loaded if it is consistently defined in
  // another loader, even if it hasn't yet been loaded in all loaders
  // that could potentially see it via delegation.
  static Klass* get_klass_by_name(Klass* accessing_klass,
                                  Symbol* klass_name,
                                  bool require_local);

  // Constant pool access.
  static Klass*   get_klass_by_index(const constantPoolHandle& cpool,
                                     int klass_index,
                                     bool& is_accessible,
                                     Klass* loading_klass);
  static void   get_field_by_index(InstanceKlass* loading_klass, fieldDescriptor& fd,
                                   int field_index);
  static Method*  get_method_by_index(const constantPoolHandle& cpool,
                                      int method_index, Bytecodes::Code bc,
                                      InstanceKlass* loading_klass);

  // converts the Klass* representing the holder of a method into a
  // InstanceKlass*.  This is needed since the holder of a method in
  // the bytecodes could be an array type.  Basically this converts
  // array types into java/lang/Object and other types stay as they are.
  static InstanceKlass* get_instance_klass_for_declared_method_holder(Klass* klass);

  // Helper routine for determining the validity of a compilation
  // with respect to concurrent class loading.
  static JVMCI::CodeInstallResult validate_compile_task_dependencies(Dependencies* target, JVMCICompileState* task, char** failure_detail);

  // Compiles `target` with the JVMCI compiler.
  void compile_method(JVMCIEnv* JVMCIENV, JVMCICompiler* compiler, const methodHandle& target, int entry_bci);

  // Determines if the GC identified by `name` is supported by the JVMCI compiler.
  bool is_gc_supported(JVMCIEnv* JVMCIENV, CollectedHeap::Name name);

  // Register the result of a compilation.
  JVMCI::CodeInstallResult register_method(JVMCIEnv* JVMCIENV,
                       const methodHandle&       target,
                       nmethodLocker&            code_handle,
                       int                       entry_bci,
                       CodeOffsets*              offsets,
                       int                       orig_pc_offset,
                       CodeBuffer*               code_buffer,
                       int                       frame_words,
                       OopMapSet*                oop_map_set,
                       ExceptionHandlerTable*    handler_table,
                       ImplicitExceptionTable*   implicit_exception_table,
                       AbstractCompiler*         compiler,
                       DebugInformationRecorder* debug_info,
                       Dependencies*             dependencies,
                       int                       compile_id,
                       bool                      has_unsafe_access,
                       bool                      has_wide_vector,
                       JVMCIObject               compiled_code,
                       JVMCIObject               nmethod_mirror,
                       FailedSpeculation**       failed_speculations,
                       char*                     speculations,
                       int                       speculations_len);

  // Reports an unexpected exception and exits the VM with a fatal error.
  static void fatal_exception(JVMCIEnv* JVMCIENV, const char* message);

  static void describe_pending_hotspot_exception(JavaThread* THREAD, bool clear);

#define CHECK_EXIT THREAD); \
  if (HAS_PENDING_EXCEPTION) { \
    char buf[256]; \
    jio_snprintf(buf, 256, "Uncaught exception at %s:%d", __FILE__, __LINE__); \
    JVMCIRuntime::fatal_exception(NULL, buf); \
    return; \
  } \
  (void)(0

#define CHECK_EXIT_(v) THREAD);                 \
  if (HAS_PENDING_EXCEPTION) { \
    char buf[256]; \
    jio_snprintf(buf, 256, "Uncaught exception at %s:%d", __FILE__, __LINE__); \
    JVMCIRuntime::fatal_exception(NULL, buf); \
    return v; \
  } \
  (void)(0

#define JVMCI_CHECK_EXIT JVMCIENV); \
  if (JVMCIENV->has_pending_exception()) {      \
    char buf[256]; \
    jio_snprintf(buf, 256, "Uncaught exception at %s:%d", __FILE__, __LINE__); \
    JVMCIRuntime::fatal_exception(JVMCIENV, buf); \
    return; \
  } \
  (void)(0

#define JVMCI_CHECK_EXIT_(result) JVMCIENV); \
  if (JVMCIENV->has_pending_exception()) {      \
    char buf[256]; \
    jio_snprintf(buf, 256, "Uncaught exception at %s:%d", __FILE__, __LINE__); \
    JVMCIRuntime::fatal_exception(JVMCIENV, buf); \
    return result; \
  } \
  (void)(0

  static BasicType kindToBasicType(const Handle& kind, TRAPS);

  static void new_instance_common(JavaThread* current, Klass* klass, bool null_on_fail);
  static void new_array_common(JavaThread* current, Klass* klass, jint length, bool null_on_fail);
  static void new_multi_array_common(JavaThread* current, Klass* klass, int rank, jint* dims, bool null_on_fail);
  static void dynamic_new_array_common(JavaThread* current, oopDesc* element_mirror, jint length, bool null_on_fail);
  static void dynamic_new_instance_common(JavaThread* current, oopDesc* type_mirror, bool null_on_fail);

  // The following routines are called from compiled JVMCI code

  // When allocation fails, these stubs:
  // 1. Exercise -XX:+HeapDumpOnOutOfMemoryError and -XX:OnOutOfMemoryError handling and also
  //    post a JVMTI_EVENT_RESOURCE_EXHAUSTED event if the failure is an OutOfMemroyError
  // 2. Return NULL with a pending exception.
  // Compiled code must ensure these stubs are not called twice for the same allocation
  // site due to the non-repeatable side effects in the case of OOME.
  static void new_instance(JavaThread* current, Klass* klass) { new_instance_common(current, klass, false); }
  static void new_array(JavaThread* current, Klass* klass, jint length) { new_array_common(current, klass, length, false); }
  static void new_multi_array(JavaThread* current, Klass* klass, int rank, jint* dims) { new_multi_array_common(current, klass, rank, dims, false); }
  static void dynamic_new_array(JavaThread* current, oopDesc* element_mirror, jint length) { dynamic_new_array_common(current, element_mirror, length, false); }
  static void dynamic_new_instance(JavaThread* current, oopDesc* type_mirror) { dynamic_new_instance_common(current, type_mirror, false); }

  // When allocation fails, these stubs return NULL and have no pending exception. Compiled code
  // can use these stubs if a failed allocation will be retried (e.g., by deoptimizing and
  // re-executing in the interpreter).
  static void new_instance_or_null(JavaThread* thread, Klass* klass) { new_instance_common(thread, klass, true); }
  static void new_array_or_null(JavaThread* thread, Klass* klass, jint length) { new_array_common(thread, klass, length, true); }
  static void new_multi_array_or_null(JavaThread* thread, Klass* klass, int rank, jint* dims) { new_multi_array_common(thread, klass, rank, dims, true); }
  static void dynamic_new_array_or_null(JavaThread* thread, oopDesc* element_mirror, jint length) { dynamic_new_array_common(thread, element_mirror, length, true); }
  static void dynamic_new_instance_or_null(JavaThread* thread, oopDesc* type_mirror) { dynamic_new_instance_common(thread, type_mirror, true); }

  static void vm_message(jboolean vmError, jlong format, jlong v1, jlong v2, jlong v3);
  static jint identity_hash_code(JavaThread* current, oopDesc* obj);
  static address exception_handler_for_pc(JavaThread* current);
  static void monitorenter(JavaThread* current, oopDesc* obj, BasicLock* lock);
  static void monitorexit (JavaThread* current, oopDesc* obj, BasicLock* lock);
  static jboolean object_notify(JavaThread* current, oopDesc* obj);
  static jboolean object_notifyAll(JavaThread* current, oopDesc* obj);
  static void vm_error(JavaThread* current, jlong where, jlong format, jlong value);
  static oopDesc* load_and_clear_exception(JavaThread* thread);
  static void log_printf(JavaThread* thread, const char* format, jlong v1, jlong v2, jlong v3);
  static void log_primitive(JavaThread* thread, jchar typeChar, jlong value, jboolean newline);
  // Print the passed in object, optionally followed by a newline.  If
  // as_string is true and the object is a java.lang.String then it
  // printed as a string, otherwise the type of the object is printed
  // followed by its address.
  static void log_object(JavaThread* thread, oopDesc* object, bool as_string, bool newline);
#if INCLUDE_G1GC
  using CardValue = G1CardTable::CardValue;
  static void write_barrier_pre(JavaThread* thread, oopDesc* obj);
  static void write_barrier_post(JavaThread* thread, volatile CardValue* card);
#endif
  static jboolean validate_object(JavaThread* thread, oopDesc* parent, oopDesc* child);

  // used to throw exceptions from compiled JVMCI code
  static int throw_and_post_jvmti_exception(JavaThread* current, const char* exception, const char* message);
  // helper methods to throw exception with complex messages
  static int throw_klass_external_name_exception(JavaThread* current, const char* exception, Klass* klass);
  static int throw_class_cast_exception(JavaThread* current, const char* exception, Klass* caster_klass, Klass* target_klass);

  // A helper to allow invocation of an arbitrary Java method.  For simplicity the method is
  // restricted to a static method that takes at most one argument.  For calling convention
  // simplicty all types are passed by being converted into a jlong
  static jlong invoke_static_method_one_arg(JavaThread* current, Method* method, jlong argument);

  // Test only function
  static jint test_deoptimize_call_int(JavaThread* current, int value);
};
#endif // SHARE_JVMCI_JVMCIRUNTIME_HPP
