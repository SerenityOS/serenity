/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_JVMTIEXPORT_HPP
#define SHARE_PRIMS_JVMTIEXPORT_HPP

#include "jvmtifiles/jvmti.h"
#include "memory/allocation.hpp"
#include "memory/iterator.hpp"
#include "oops/oop.hpp"
#include "oops/oopHandle.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/frame.hpp"
#include "runtime/handles.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/macros.hpp"

// Must be included after jvmti.h.
#include "jvmticmlr.h"

// Forward declarations

struct JvmtiCachedClassFileData;
class JvmtiEventControllerPrivate;
class JvmtiManageCapabilities;
class JvmtiEnv;
class JvmtiThreadState;

class OopStorage;

#define JVMTI_SUPPORT_FLAG(key)                                           \
  private:                                                                \
  static bool  _##key;                                                    \
  public:                                                                 \
  inline static void set_##key(bool on) {                                 \
    JVMTI_ONLY(_##key = (on != 0));                                       \
    NOT_JVMTI(report_unsupported(on));                                    \
  }                                                                       \
  inline static bool key() {                                              \
    JVMTI_ONLY(return _##key);                                            \
    NOT_JVMTI(return false);                                              \
  }


// This class contains the JVMTI interface for the rest of hotspot.
//
class JvmtiExport : public AllStatic {
  friend class VMStructs;
  friend class CompileReplay;

 private:

#if INCLUDE_JVMTI
  static int         _field_access_count;
  static int         _field_modification_count;

  static bool        _can_access_local_variables;
  static bool        _can_hotswap_or_post_breakpoint;
  static bool        _can_modify_any_class;
  static bool        _can_walk_any_space;
#endif // INCLUDE_JVMTI

  JVMTI_SUPPORT_FLAG(can_get_source_debug_extension)
  JVMTI_SUPPORT_FLAG(can_maintain_original_method_order)
  JVMTI_SUPPORT_FLAG(can_post_interpreter_events)
  JVMTI_SUPPORT_FLAG(can_post_on_exceptions)
  JVMTI_SUPPORT_FLAG(can_post_breakpoint)
  JVMTI_SUPPORT_FLAG(can_post_field_access)
  JVMTI_SUPPORT_FLAG(can_post_field_modification)
  JVMTI_SUPPORT_FLAG(can_post_method_entry)
  JVMTI_SUPPORT_FLAG(can_post_method_exit)
  JVMTI_SUPPORT_FLAG(can_pop_frame)
  JVMTI_SUPPORT_FLAG(can_force_early_return)

  JVMTI_SUPPORT_FLAG(early_vmstart_recorded)
  JVMTI_SUPPORT_FLAG(can_get_owned_monitor_info) // includes can_get_owned_monitor_stack_depth_info

  friend class JvmtiEventControllerPrivate;  // should only modify these flags
  JVMTI_SUPPORT_FLAG(should_post_single_step)
  JVMTI_SUPPORT_FLAG(should_post_field_access)
  JVMTI_SUPPORT_FLAG(should_post_field_modification)
  JVMTI_SUPPORT_FLAG(should_post_class_load)
  JVMTI_SUPPORT_FLAG(should_post_class_prepare)
  JVMTI_SUPPORT_FLAG(should_post_class_unload)
  JVMTI_SUPPORT_FLAG(should_post_native_method_bind)
  JVMTI_SUPPORT_FLAG(should_post_compiled_method_load)
  JVMTI_SUPPORT_FLAG(should_post_compiled_method_unload)
  JVMTI_SUPPORT_FLAG(should_post_dynamic_code_generated)
  JVMTI_SUPPORT_FLAG(should_post_monitor_contended_enter)
  JVMTI_SUPPORT_FLAG(should_post_monitor_contended_entered)
  JVMTI_SUPPORT_FLAG(should_post_monitor_wait)
  JVMTI_SUPPORT_FLAG(should_post_monitor_waited)
  JVMTI_SUPPORT_FLAG(should_post_data_dump)
  JVMTI_SUPPORT_FLAG(should_post_garbage_collection_start)
  JVMTI_SUPPORT_FLAG(should_post_garbage_collection_finish)
  JVMTI_SUPPORT_FLAG(should_post_on_exceptions)

  // ------ the below maybe don't have to be (but are for now)
  // fixed conditions here ------------
  // any events can be enabled
  JVMTI_SUPPORT_FLAG(should_post_thread_life)
  JVMTI_SUPPORT_FLAG(should_post_object_free)
  JVMTI_SUPPORT_FLAG(should_post_resource_exhausted)

  // we are holding objects on the heap - need to talk to GC - e.g.
  // breakpoint info
  JVMTI_SUPPORT_FLAG(should_clean_up_heap_objects)
  JVMTI_SUPPORT_FLAG(should_post_vm_object_alloc)
  JVMTI_SUPPORT_FLAG(should_post_sampled_object_alloc)

  // If flag cannot be implemented, give an error if on=true
  static void report_unsupported(bool on);

  // these should only be called by the friend class
  friend class JvmtiManageCapabilities;
  inline static void set_can_modify_any_class(bool on) {
    JVMTI_ONLY(_can_modify_any_class = (on != 0);)
  }
  inline static void set_can_access_local_variables(bool on) {
    JVMTI_ONLY(_can_access_local_variables = (on != 0);)
  }
  inline static void set_can_hotswap_or_post_breakpoint(bool on) {
    JVMTI_ONLY(_can_hotswap_or_post_breakpoint = (on != 0);)
  }
  inline static void set_can_walk_any_space(bool on) {
    JVMTI_ONLY(_can_walk_any_space = (on != 0);)
  }

  enum {
    JVMTI_VERSION_MASK   = 0x70000000,
    JVMTI_VERSION_VALUE  = 0x30000000,
    JVMDI_VERSION_VALUE  = 0x20000000
  };

  static void post_field_modification(JavaThread *thread, Method* method, address location,
                                      Klass* field_klass, Handle object, jfieldID field,
                                      char sig_type, jvalue *value);


  // posts a DynamicCodeGenerated event (internal/private implementation).
  // The public post_dynamic_code_generated* functions make use of the
  // internal implementation.  Also called from JvmtiDeferredEvent::post()
  static void post_dynamic_code_generated_internal(const char *name, const void *code_begin, const void *code_end) NOT_JVMTI_RETURN;

  static void post_class_unload_internal(const char *name) NOT_JVMTI_RETURN;

  static void initialize_oop_storage() NOT_JVMTI_RETURN;
  static OopStorage* jvmti_oop_storage();
  static OopStorage* weak_tag_storage();
 private:

  // GenerateEvents support to allow posting of CompiledMethodLoad and
  // DynamicCodeGenerated events for a given environment.
  friend class JvmtiCodeBlobEvents;

  static void post_dynamic_code_generated(JvmtiEnv* env, const char *name, const void *code_begin,
                                          const void *code_end) NOT_JVMTI_RETURN;

  // This flag indicates whether RedefineClasses() has ever redefined
  // one or more classes during the lifetime of the VM. The flag should
  // only be set by the friend class and can be queried by other sub
  // systems as needed to relax invariant checks.
  static uint64_t _redefinition_count;
  friend class VM_RedefineClasses;
  inline static void increment_redefinition_count() {
    JVMTI_ONLY(_redefinition_count++;)
  }
  // Flag to indicate if the compiler has recorded all dependencies. When the
  // can_redefine_classes capability is enabled in the OnLoad phase then the compiler
  // records all dependencies from startup. However if the capability is first
  // enabled some time later then the dependencies recorded by the compiler
  // are incomplete. This flag is used by RedefineClasses to know if the
  // dependency information is complete or not.
  static bool _all_dependencies_are_recorded;

  static void post_method_exit_inner(JavaThread* thread,
                                     methodHandle& mh,
                                     JvmtiThreadState *state,
                                     bool exception_exit,
                                     frame current_frame,
                                     jvalue& value);

 public:
  inline static bool has_redefined_a_class() {
    JVMTI_ONLY(return _redefinition_count != 0);
    NOT_JVMTI(return false);
  }

  // Only set in safepoint, so no memory ordering needed.
  inline static uint64_t redefinition_count() {
    JVMTI_ONLY(return _redefinition_count);
    NOT_JVMTI(return 0);
  }

  inline static bool all_dependencies_are_recorded() {
    return _all_dependencies_are_recorded;
  }

  inline static void set_all_dependencies_are_recorded(bool on) {
    _all_dependencies_are_recorded = (on != 0);
  }

  // Add read edges to the unnamed modules of the bootstrap and app class loaders
  static void add_default_read_edges(Handle h_module, TRAPS) NOT_JVMTI_RETURN;

  // Add a read edge to the module
  static jvmtiError add_module_reads(Handle module, Handle to_module, TRAPS);

  // Updates a module to export a package
  static jvmtiError add_module_exports(Handle module, Handle pkg_name, Handle to_module, TRAPS);

  // Updates a module to open a package
  static jvmtiError add_module_opens(Handle module, Handle pkg_name, Handle to_module, TRAPS);

  // Add a used service to the module
  static jvmtiError add_module_uses(Handle module, Handle service, TRAPS);

  // Add a service provider to the module
  static jvmtiError add_module_provides(Handle module, Handle service, Handle impl_class, TRAPS);

  // let JVMTI know that the JVM_OnLoad code is running
  static void enter_onload_phase() NOT_JVMTI_RETURN;

  // let JVMTI know that the VM isn't up yet (and JVM_OnLoad code isn't running)
  static void enter_primordial_phase() NOT_JVMTI_RETURN;

  // let JVMTI know that the VM isn't up yet but JNI is live
  static void enter_early_start_phase() NOT_JVMTI_RETURN;
  static void enter_start_phase() NOT_JVMTI_RETURN;

  // let JVMTI know that the VM is fully up and running now
  static void enter_live_phase() NOT_JVMTI_RETURN;

  // ------ can_* conditions (below) are set at OnLoad and never changed ------------
  inline static bool can_modify_any_class()                       {
    JVMTI_ONLY(return _can_modify_any_class);
    NOT_JVMTI(return false);
  }
  inline static bool can_access_local_variables()                 {
    JVMTI_ONLY(return _can_access_local_variables);
    NOT_JVMTI(return false);
  }
  inline static bool can_hotswap_or_post_breakpoint()             {
    JVMTI_ONLY(return _can_hotswap_or_post_breakpoint);
    NOT_JVMTI(return false);
  }
  inline static bool can_walk_any_space()                         {
    JVMTI_ONLY(return _can_walk_any_space);
    NOT_JVMTI(return false);
  }

  // field access management
  static address  get_field_access_count_addr() NOT_JVMTI_RETURN_(0);

  // field modification management
  static address  get_field_modification_count_addr() NOT_JVMTI_RETURN_(0);

  // -----------------

  static bool is_jvmti_version(jint version)                      {
    JVMTI_ONLY(return (version & JVMTI_VERSION_MASK) == JVMTI_VERSION_VALUE);
    NOT_JVMTI(return false);
  }
  static bool is_jvmdi_version(jint version)                      {
    JVMTI_ONLY(return (version & JVMTI_VERSION_MASK) == JVMDI_VERSION_VALUE);
    NOT_JVMTI(return false);
  }
  static jint get_jvmti_interface(JavaVM *jvm, void **penv, jint version) NOT_JVMTI_RETURN_(0);
  static void decode_version_values(jint version, int * major, int * minor,
                                    int * micro) NOT_JVMTI_RETURN;

  // single stepping management methods
  static void at_single_stepping_point(JavaThread *thread, Method* method, address location) NOT_JVMTI_RETURN;
  static void expose_single_stepping(JavaThread *thread) NOT_JVMTI_RETURN;
  static bool hide_single_stepping(JavaThread *thread) NOT_JVMTI_RETURN_(false);

  // Methods that notify the debugger that something interesting has happened in the VM.
  static void post_early_vm_start        () NOT_JVMTI_RETURN;
  static void post_vm_start              () NOT_JVMTI_RETURN;
  static void post_vm_initialized        () NOT_JVMTI_RETURN;
  static void post_vm_death              () NOT_JVMTI_RETURN;

  static void post_single_step           (JavaThread *thread, Method* method, address location) NOT_JVMTI_RETURN;
  static void post_raw_breakpoint        (JavaThread *thread, Method* method, address location) NOT_JVMTI_RETURN;

  static void post_exception_throw       (JavaThread *thread, Method* method, address location, oop exception) NOT_JVMTI_RETURN;
  static void notice_unwind_due_to_exception (JavaThread *thread, Method* method, address location, oop exception, bool in_handler_frame) NOT_JVMTI_RETURN;

  static oop jni_GetField_probe          (JavaThread *thread, jobject jobj,
    oop obj, Klass* klass, jfieldID fieldID, bool is_static)
    NOT_JVMTI_RETURN_(NULL);
  static void post_field_access_by_jni   (JavaThread *thread, oop obj,
    Klass* klass, jfieldID fieldID, bool is_static) NOT_JVMTI_RETURN;
  static void post_field_access          (JavaThread *thread, Method* method,
    address location, Klass* field_klass, Handle object, jfieldID field) NOT_JVMTI_RETURN;
  static oop jni_SetField_probe          (JavaThread *thread, jobject jobj,
    oop obj, Klass* klass, jfieldID fieldID, bool is_static, char sig_type,
    jvalue *value) NOT_JVMTI_RETURN_(NULL);
  static void post_field_modification_by_jni(JavaThread *thread, oop obj,
    Klass* klass, jfieldID fieldID, bool is_static, char sig_type,
    jvalue *value);
  static void post_raw_field_modification(JavaThread *thread, Method* method,
    address location, Klass* field_klass, Handle object, jfieldID field,
    char sig_type, jvalue *value) NOT_JVMTI_RETURN;

  static void post_method_entry          (JavaThread *thread, Method* method, frame current_frame) NOT_JVMTI_RETURN;
  static void post_method_exit           (JavaThread *thread, Method* method, frame current_frame) NOT_JVMTI_RETURN;

  static void post_class_load            (JavaThread *thread, Klass* klass) NOT_JVMTI_RETURN;
  static void post_class_unload          (Klass* klass) NOT_JVMTI_RETURN;
  static void post_class_prepare         (JavaThread *thread, Klass* klass) NOT_JVMTI_RETURN;

  static void post_thread_start          (JavaThread *thread) NOT_JVMTI_RETURN;
  static void post_thread_end            (JavaThread *thread) NOT_JVMTI_RETURN;

  // Support for java.lang.instrument agent loading.
  static bool _should_post_class_file_load_hook;
  inline static void set_should_post_class_file_load_hook(bool on)     { _should_post_class_file_load_hook = on;  }
  inline static bool should_post_class_file_load_hook()           {
    JVMTI_ONLY(return _should_post_class_file_load_hook);
    NOT_JVMTI(return false;)
  }
  static bool is_early_phase() NOT_JVMTI_RETURN_(false);
  static bool has_early_class_hook_env() NOT_JVMTI_RETURN_(false);
  // Return true if the class was modified by the hook.
  static bool post_class_file_load_hook(Symbol* h_name, Handle class_loader,
                                        Handle h_protection_domain,
                                        unsigned char **data_ptr, unsigned char **end_ptr,
                                        JvmtiCachedClassFileData **cache_ptr) NOT_JVMTI_RETURN_(false);
  static void post_native_method_bind(Method* method, address* function_ptr) NOT_JVMTI_RETURN;
  static void post_compiled_method_load(JvmtiEnv* env, nmethod *nm) NOT_JVMTI_RETURN;
  static void post_compiled_method_load(nmethod *nm) NOT_JVMTI_RETURN;
  static void post_dynamic_code_generated(const char *name, const void *code_begin, const void *code_end) NOT_JVMTI_RETURN;

  // used to post a CompiledMethodUnload event
  static void post_compiled_method_unload(jmethodID mid, const void *code_begin) NOT_JVMTI_RETURN;

  // similiar to post_dynamic_code_generated except that it can be used to
  // post a DynamicCodeGenerated event while holding locks in the VM. Any event
  // posted using this function is recorded by the enclosing event collector
  // -- JvmtiDynamicCodeEventCollector.
  static void post_dynamic_code_generated_while_holding_locks(const char* name, address code_begin, address code_end) NOT_JVMTI_RETURN;

  static void post_garbage_collection_finish() NOT_JVMTI_RETURN;
  static void post_garbage_collection_start() NOT_JVMTI_RETURN;
  static void post_data_dump() NOT_JVMTI_RETURN;
  static void post_monitor_contended_enter(JavaThread *thread, ObjectMonitor *obj_mntr) NOT_JVMTI_RETURN;
  static void post_monitor_contended_entered(JavaThread *thread, ObjectMonitor *obj_mntr) NOT_JVMTI_RETURN;
  static void post_monitor_wait(JavaThread *thread, oop obj, jlong timeout) NOT_JVMTI_RETURN;
  static void post_monitor_waited(JavaThread *thread, ObjectMonitor *obj_mntr, jboolean timed_out) NOT_JVMTI_RETURN;
  static void post_object_free(JvmtiEnv* env, jlong tag) NOT_JVMTI_RETURN;
  static void post_resource_exhausted(jint resource_exhausted_flags, const char* detail) NOT_JVMTI_RETURN;
  static void record_vm_internal_object_allocation(oop object) NOT_JVMTI_RETURN;
  // Post objects collected by vm_object_alloc_event_collector.
  static void post_vm_object_alloc(JavaThread *thread, oop object) NOT_JVMTI_RETURN;
  // Collects vm internal objects for later event posting.
  inline static void vm_object_alloc_event_collector(oop object) {
    if (should_post_vm_object_alloc()) {
      record_vm_internal_object_allocation(object);
    }
  }

  static void record_sampled_internal_object_allocation(oop object) NOT_JVMTI_RETURN;
  // Post objects collected by sampled_object_alloc_event_collector.
  static void post_sampled_object_alloc(JavaThread *thread, oop object) NOT_JVMTI_RETURN;

  // Collects vm internal objects for later event posting.
  inline static void sampled_object_alloc_event_collector(oop object) {
    if (should_post_sampled_object_alloc()) {
      record_sampled_internal_object_allocation(object);
    }
  }

  inline static void post_array_size_exhausted() {
    if (should_post_resource_exhausted()) {
      post_resource_exhausted(JVMTI_RESOURCE_EXHAUSTED_OOM_ERROR,
                              "Requested array size exceeds VM limit");
    }
  }

  static void cleanup_thread             (JavaThread* thread) NOT_JVMTI_RETURN;
  static void clear_detected_exception   (JavaThread* thread) NOT_JVMTI_RETURN;

  static void transition_pending_onload_raw_monitors() NOT_JVMTI_RETURN;

#if INCLUDE_SERVICES
  // attach support
  static jint load_agent_library(const char *agent, const char *absParam, const char *options, outputStream* out) NOT_JVMTI_RETURN_(JNI_ERR);
#endif

  // SetNativeMethodPrefix support
  static char** get_all_native_method_prefixes(int* count_ptr) NOT_JVMTI_RETURN_(NULL);

  // JavaThread lifecycle support:
  static jvmtiError cv_external_thread_to_JavaThread(ThreadsList * t_list,
                                                     jthread thread,
                                                     JavaThread ** jt_pp,
                                                     oop * thread_oop_p);
  static jvmtiError cv_oop_to_JavaThread(ThreadsList * t_list, oop thread_oop,
                                         JavaThread ** jt_pp);
};

// Support class used by JvmtiDynamicCodeEventCollector and others. It
// describes a single code blob by name and address range.
class JvmtiCodeBlobDesc : public CHeapObj<mtInternal> {
 private:
  char _name[64];
  address _code_begin;
  address _code_end;

 public:
  JvmtiCodeBlobDesc(const char *name, address code_begin, address code_end) {
    assert(name != NULL, "all code blobs must be named");
    strncpy(_name, name, sizeof(_name) - 1);
    _name[sizeof(_name)-1] = '\0';
    _code_begin = code_begin;
    _code_end = code_end;
  }
  char* name()                  { return _name; }
  address code_begin()          { return _code_begin; }
  address code_end()            { return _code_end; }
};

// JvmtiEventCollector is a helper class to setup thread for
// event collection.
class JvmtiEventCollector : public StackObj {
 private:
  JvmtiEventCollector* _prev;  // Save previous one to support nested event collector.
  bool _unset_jvmti_thread_state;

 public:
  JvmtiEventCollector() : _prev(NULL), _unset_jvmti_thread_state(false) {}

  void setup_jvmti_thread_state(); // Set this collector in current thread, returns if success.
  void unset_jvmti_thread_state(); // Reset previous collector in current thread.
  virtual bool is_dynamic_code_event()   { return false; }
  virtual bool is_vm_object_alloc_event(){ return false; }
  virtual bool is_sampled_object_alloc_event(){ return false; }
  JvmtiEventCollector *get_prev()        { return _prev; }
};

// A JvmtiDynamicCodeEventCollector is a helper class for the JvmtiExport
// interface. It collects "dynamic code generated" events that are posted
// while holding locks. When the event collector goes out of scope the
// events will be posted.
//
// Usage :-
//
// {
//   JvmtiDynamicCodeEventCollector event_collector;
//   :
//   { MutexLocker ml(...)
//     :
//     JvmtiExport::post_dynamic_code_generated_while_holding_locks(...)
//   }
//   // event collector goes out of scope => post events to profiler.
// }

class JvmtiDynamicCodeEventCollector : public JvmtiEventCollector {
 private:
  GrowableArray<JvmtiCodeBlobDesc*>* _code_blobs;           // collected code blob events

  friend class JvmtiExport;
  void register_stub(const char* name, address start, address end);

 public:
  JvmtiDynamicCodeEventCollector()  NOT_JVMTI_RETURN;
  ~JvmtiDynamicCodeEventCollector() NOT_JVMTI_RETURN;
  bool is_dynamic_code_event()   { return true; }

};

// Used as a base class for object allocation collection and then posting
// the allocations to any event notification callbacks.
//
class JvmtiObjectAllocEventCollector : public JvmtiEventCollector {
 protected:
  GrowableArray<OopHandle>* _allocated;      // field to record collected allocated object oop.
  bool _enable;                   // This flag is enabled in constructor if set up in the thread state
                                  // and disabled in destructor before posting event. To avoid
                                  // collection of objects allocated while running java code inside
                                  // agent post_X_object_alloc() event handler.
  void (*_post_callback)(JavaThread*, oop); // what callback to use when destroying the collector.

  friend class JvmtiExport;

  // Record allocated object oop.
  inline void record_allocation(oop obj);

 public:
  JvmtiObjectAllocEventCollector()  NOT_JVMTI_RETURN;

  void generate_call_for_allocated();

  bool is_enabled()                 { return _enable; }
  void set_enabled(bool on)         { _enable = on; }
};

// Used to record vm internally allocated object oops and post
// vm object alloc event for objects visible to java world.
// Constructor enables JvmtiThreadState flag and all vm allocated
// objects are recorded in a growable array. When destructor is
// called the vm object alloc event is posted for each object
// visible to java world.
// See jvm.cpp file for its usage.
//
class JvmtiVMObjectAllocEventCollector : public JvmtiObjectAllocEventCollector {
 public:
  JvmtiVMObjectAllocEventCollector()  NOT_JVMTI_RETURN;
  ~JvmtiVMObjectAllocEventCollector()  NOT_JVMTI_RETURN;
  virtual bool is_vm_object_alloc_event()   { return true; }
};

// Used to record sampled allocated object oops and post
// sampled object alloc event.
// Constructor enables JvmtiThreadState flag and all sampled allocated
// objects are recorded in a growable array. When destructor is
// called the sampled object alloc event is posted for each sampled object.
// See jvm.cpp file for its usage.
//
class JvmtiSampledObjectAllocEventCollector : public JvmtiObjectAllocEventCollector {
 public:
  JvmtiSampledObjectAllocEventCollector()  NOT_JVMTI_RETURN;
  ~JvmtiSampledObjectAllocEventCollector()  NOT_JVMTI_RETURN;
  bool is_sampled_object_alloc_event()    { return true; }
  static bool object_alloc_is_safe_to_sample() NOT_JVMTI_RETURN_(false);
};

// Marker class to disable the posting of VMObjectAlloc events
// within its scope.
//
// Usage :-
//
// {
//   NoJvmtiVMObjectAllocMark njm;
//   :
//   // VMObjAlloc event will not be posted
//   JvmtiExport::vm_object_alloc_event_collector(obj);
//   :
// }

class NoJvmtiVMObjectAllocMark : public StackObj {
 private:
  // enclosing collector if enabled, NULL otherwise
  JvmtiVMObjectAllocEventCollector *_collector;

  bool was_enabled()    { return _collector != NULL; }

 public:
  NoJvmtiVMObjectAllocMark() NOT_JVMTI_RETURN;
  ~NoJvmtiVMObjectAllocMark() NOT_JVMTI_RETURN;
};


// Base class for reporting GC events to JVMTI.
class JvmtiGCMarker : public StackObj {
 public:
  JvmtiGCMarker() NOT_JVMTI_RETURN;
  ~JvmtiGCMarker() NOT_JVMTI_RETURN;
};

// JvmtiHideSingleStepping is a helper class for hiding
// internal single step events.
class JvmtiHideSingleStepping : public StackObj {
 private:
  bool         _single_step_hidden;
  JavaThread * _thread;

 public:
  JvmtiHideSingleStepping(JavaThread * thread) {
    assert(thread != NULL, "sanity check");

    _single_step_hidden = false;
    _thread = thread;
    if (JvmtiExport::should_post_single_step()) {
      _single_step_hidden = JvmtiExport::hide_single_stepping(_thread);
    }
  }

  ~JvmtiHideSingleStepping() {
    if (_single_step_hidden) {
      JvmtiExport::expose_single_stepping(_thread);
    }
  }
};

#endif // SHARE_PRIMS_JVMTIEXPORT_HPP
