/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CIENV_HPP
#define SHARE_CI_CIENV_HPP

#include "ci/ciClassList.hpp"
#include "ci/ciObjectFactory.hpp"
#include "classfile/vmClassMacros.hpp"
#include "code/debugInfoRec.hpp"
#include "code/dependencies.hpp"
#include "code/exceptionHandlerTable.hpp"
#include "compiler/compilerThread.hpp"
#include "oops/methodData.hpp"
#include "runtime/thread.hpp"

class CompileTask;
class OopMapSet;

// ciEnv
//
// This class is the top level broker for requests from the compiler
// to the VM.
class ciEnv : StackObj {
  CI_PACKAGE_ACCESS_TO

  friend class CompileBroker;
  friend class Dependencies;  // for get_object, during logging
  friend class PrepareExtraDataClosure;

private:
  Arena*           _arena;       // Alias for _ciEnv_arena except in init_shared_objects()
  Arena            _ciEnv_arena;
  ciObjectFactory* _factory;
  OopRecorder*     _oop_recorder;
  DebugInformationRecorder* _debug_info;
  Dependencies*    _dependencies;
  const char*      _failure_reason;
  bool             _inc_decompile_count_on_failure;
  int              _compilable;
  bool             _break_at_compile;
  int              _num_inlined_bytecodes;
  CompileTask*     _task;           // faster access to CompilerThread::task
  CompileLog*      _log;            // faster access to CompilerThread::log
  void*            _compiler_data;  // compiler-specific stuff, if any

  char* _name_buffer;
  int   _name_buffer_len;

  // Cache Jvmti state
  uint64_t _jvmti_redefinition_count;
  bool  _jvmti_can_hotswap_or_post_breakpoint;
  bool  _jvmti_can_access_local_variables;
  bool  _jvmti_can_post_on_exceptions;
  bool  _jvmti_can_pop_frame;
  bool  _jvmti_can_get_owned_monitor_info; // includes can_get_owned_monitor_stack_depth_info
  bool  _jvmti_can_walk_any_space;

  // Cache DTrace flags
  bool  _dtrace_extended_probes;
  bool  _dtrace_method_probes;
  bool  _dtrace_alloc_probes;

  // Distinguished instances of certain ciObjects..
  static ciObject*              _null_object_instance;

#define VM_CLASS_DECL(name, ignore_s) static ciInstanceKlass* _##name;
  VM_CLASSES_DO(VM_CLASS_DECL)
#undef VM_CLASS_DECL

  static ciSymbol*        _unloaded_cisymbol;
  static ciInstanceKlass* _unloaded_ciinstance_klass;
  static ciObjArrayKlass* _unloaded_ciobjarrayklass;

  static jobject _ArrayIndexOutOfBoundsException_handle;
  static jobject _ArrayStoreException_handle;
  static jobject _ClassCastException_handle;

  ciInstance* _NullPointerException_instance;
  ciInstance* _ArithmeticException_instance;
  ciInstance* _ArrayIndexOutOfBoundsException_instance;
  ciInstance* _ArrayStoreException_instance;
  ciInstance* _ClassCastException_instance;

  ciInstance* _the_null_string;      // The Java string "null"
  ciInstance* _the_min_jint_string; // The Java string "-2147483648"

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
  ciKlass* get_klass_by_name(ciKlass* accessing_klass,
                             ciSymbol* klass_name,
                             bool require_local);

  // Constant pool access.
  ciKlass*   get_klass_by_index(const constantPoolHandle& cpool,
                                int klass_index,
                                bool& is_accessible,
                                ciInstanceKlass* loading_klass);
  ciConstant get_constant_by_index(const constantPoolHandle& cpool,
                                   int pool_index, int cache_index,
                                   ciInstanceKlass* accessor);
  ciField*   get_field_by_index(ciInstanceKlass* loading_klass,
                                int field_index);
  ciMethod*  get_method_by_index(const constantPoolHandle& cpool,
                                 int method_index, Bytecodes::Code bc,
                                 ciInstanceKlass* loading_klass);

  // Implementation methods for loading and constant pool access.
  ciKlass* get_klass_by_name_impl(ciKlass* accessing_klass,
                                  const constantPoolHandle& cpool,
                                  ciSymbol* klass_name,
                                  bool require_local);
  ciKlass*   get_klass_by_index_impl(const constantPoolHandle& cpool,
                                     int klass_index,
                                     bool& is_accessible,
                                     ciInstanceKlass* loading_klass);
  ciConstant get_constant_by_index_impl(const constantPoolHandle& cpool,
                                        int pool_index, int cache_index,
                                        ciInstanceKlass* loading_klass);
  ciField*   get_field_by_index_impl(ciInstanceKlass* loading_klass,
                                     int field_index);
  ciMethod*  get_method_by_index_impl(const constantPoolHandle& cpool,
                                      int method_index, Bytecodes::Code bc,
                                      ciInstanceKlass* loading_klass);

  // Helper methods
  bool       check_klass_accessibility(ciKlass* accessing_klass,
                                      Klass* resolved_klass);
  Method*    lookup_method(ciInstanceKlass* accessor,
                           ciKlass*         holder,
                           Symbol*          name,
                           Symbol*          sig,
                           Bytecodes::Code  bc,
                           constantTag      tag);

  // Get a ciObject from the object factory.  Ensures uniqueness
  // of ciObjects.
  ciObject* get_object(oop o) {
    if (o == NULL) {
      return _null_object_instance;
    } else {
      return _factory->get(o);
    }
  }

  ciSymbol* get_symbol(Symbol* o) {
    if (o == NULL) {
      ShouldNotReachHere();
      return NULL;
    } else {
      return _factory->get_symbol(o);
    }
  }

  ciMetadata* get_metadata(Metadata* o) {
    if (o == NULL) {
      return NULL;
    } else {
      return _factory->get_metadata(o);
    }
  }

  ciMetadata* cached_metadata(Metadata* o) {
    return _factory->cached_metadata(o);
  }

  ciInstance* get_instance(oop o) {
    if (o == NULL) return NULL;
    return get_object(o)->as_instance();
  }
  ciObjArrayKlass* get_obj_array_klass(Klass* o) {
    if (o == NULL) return NULL;
    return get_metadata(o)->as_obj_array_klass();
  }
  ciTypeArrayKlass* get_type_array_klass(Klass* o) {
    if (o == NULL) return NULL;
    return get_metadata(o)->as_type_array_klass();
  }
  ciKlass* get_klass(Klass* o) {
    if (o == NULL) return NULL;
    return get_metadata(o)->as_klass();
  }
  ciInstanceKlass* get_instance_klass(Klass* o) {
    if (o == NULL) return NULL;
    return get_metadata(o)->as_instance_klass();
  }
  ciMethod* get_method(Method* o) {
    if (o == NULL) return NULL;
    return get_metadata(o)->as_method();
  }
  ciMethodData* get_method_data(MethodData* o) {
    if (o == NULL) return NULL;
    return get_metadata(o)->as_method_data();
  }

  ciMethod* get_method_from_handle(Method* method);

  ciInstance* get_or_create_exception(jobject& handle, Symbol* name);

  // Get a ciMethod representing either an unfound method or
  // a method with an unloaded holder.  Ensures uniqueness of
  // the result.
  ciMethod* get_unloaded_method(ciKlass*         holder,
                                ciSymbol*        name,
                                ciSymbol*        signature,
                                ciInstanceKlass* accessor) {
    ciInstanceKlass* declared_holder = get_instance_klass_for_declared_method_holder(holder);
    return _factory->get_unloaded_method(declared_holder, name, signature, accessor);
  }

  // Get a ciKlass representing an unloaded klass.
  // Ensures uniqueness of the result.
  ciKlass* get_unloaded_klass(ciKlass*  accessing_klass,
                              ciSymbol* name) {
    return _factory->get_unloaded_klass(accessing_klass, name, true);
  }

  // Get a ciKlass representing an unloaded klass mirror.
  // Result is not necessarily unique, but will be unloaded.
  ciInstance* get_unloaded_klass_mirror(ciKlass* type) {
    return _factory->get_unloaded_klass_mirror(type);
  }

  // Get a ciInstance representing an unresolved method handle constant.
  ciInstance* get_unloaded_method_handle_constant(ciKlass*  holder,
                                                  ciSymbol* name,
                                                  ciSymbol* signature,
                                                  int       ref_kind) {
    return _factory->get_unloaded_method_handle_constant(holder, name, signature, ref_kind);
  }

  // Get a ciInstance representing an unresolved method type constant.
  ciInstance* get_unloaded_method_type_constant(ciSymbol* signature) {
    return _factory->get_unloaded_method_type_constant(signature);
  }

  // See if we already have an unloaded klass for the given name
  // or return NULL if not.
  ciKlass *check_get_unloaded_klass(ciKlass*  accessing_klass, ciSymbol* name) {
    return _factory->get_unloaded_klass(accessing_klass, name, false);
  }

  // Get a ciReturnAddress corresponding to the given bci.
  // Ensures uniqueness of the result.
  ciReturnAddress* get_return_address(int bci) {
    return _factory->get_return_address(bci);
  }

  // Get a ciMethodData representing the methodData for a method
  // with none.
  ciMethodData* get_empty_methodData() {
    return _factory->get_empty_methodData();
  }

  // General utility : get a buffer of some required length.
  // Used in symbol creation.
  char* name_buffer(int req_len);

  // Is this thread currently in the VM state?
  static bool is_in_vm();

  // Helper routine for determining the validity of a compilation with
  // respect to method dependencies (e.g. concurrent class loading).
  void validate_compile_task_dependencies(ciMethod* target);
public:
  enum {
    MethodCompilable,
    MethodCompilable_not_at_tier,
    MethodCompilable_never
  };

  ciEnv(CompileTask* task);
  // Used only during initialization of the ci
  ciEnv(Arena* arena);
  ~ciEnv();

  OopRecorder* oop_recorder() { return _oop_recorder; }
  void set_oop_recorder(OopRecorder* r) { _oop_recorder = r; }

  DebugInformationRecorder* debug_info() { return _debug_info; }
  void set_debug_info(DebugInformationRecorder* i) { _debug_info = i; }

  Dependencies* dependencies() { return _dependencies; }
  void set_dependencies(Dependencies* d) { _dependencies = d; }

  // This is true if the compilation is not going to produce code.
  // (It is reasonable to retry failed compilations.)
  bool failing() { return _failure_reason != NULL; }

  // Reason this compilation is failing, such as "too many basic blocks".
  const char* failure_reason() { return _failure_reason; }

  // Return state of appropriate compilability
  int compilable() { return _compilable; }

  const char* retry_message() const {
    switch (_compilable) {
      case ciEnv::MethodCompilable_not_at_tier:
        return "retry at different tier";
      case ciEnv::MethodCompilable_never:
        return "not retryable";
      case ciEnv::MethodCompilable:
        return NULL;
      default:
        ShouldNotReachHere();
        return NULL;
    }
  }

  bool break_at_compile() { return _break_at_compile; }
  void set_break_at_compile(bool z) { _break_at_compile = z; }

  // Cache Jvmti state
  bool  cache_jvmti_state();
  bool  jvmti_state_changed() const;
  bool  should_retain_local_variables() const {
    return _jvmti_can_access_local_variables || _jvmti_can_pop_frame;
  }
  bool  jvmti_can_hotswap_or_post_breakpoint() const { return _jvmti_can_hotswap_or_post_breakpoint; }
  bool  jvmti_can_post_on_exceptions()         const { return _jvmti_can_post_on_exceptions; }
  bool  jvmti_can_get_owned_monitor_info()     const { return _jvmti_can_get_owned_monitor_info; }
  bool  jvmti_can_walk_any_space()             const { return _jvmti_can_walk_any_space; }

  // Cache DTrace flags
  void  cache_dtrace_flags();
  bool  dtrace_extended_probes() const { return _dtrace_extended_probes; }
  bool  dtrace_method_probes()   const { return _dtrace_method_probes; }
  bool  dtrace_alloc_probes()    const { return _dtrace_alloc_probes; }

  // The compiler task which has created this env.
  // May be useful to find out compile_id, comp_level, etc.
  CompileTask* task() { return _task; }

  // Handy forwards to the task:
  int comp_level();   // task()->comp_level()
  uint compile_id();  // task()->compile_id()

  // Register the result of a compilation.
  void register_method(ciMethod*                 target,
                       int                       entry_bci,
                       CodeOffsets*              offsets,
                       int                       orig_pc_offset,
                       CodeBuffer*               code_buffer,
                       int                       frame_words,
                       OopMapSet*                oop_map_set,
                       ExceptionHandlerTable*    handler_table,
                       ImplicitExceptionTable*   inc_table,
                       AbstractCompiler*         compiler,
                       bool                      has_unsafe_access,
                       bool                      has_wide_vectors,
                       RTMState                  rtm_state = NoRTM,
                       const GrowableArrayView<RuntimeStub*>& native_invokers = GrowableArrayView<RuntimeStub*>::EMPTY);


  // Access to certain well known ciObjects.
#define VM_CLASS_FUNC(name, ignore_s) \
  ciInstanceKlass* name() { \
    return _##name;\
  }
  VM_CLASSES_DO(VM_CLASS_FUNC)
#undef VM_CLASS_FUNC

  ciInstance* NullPointerException_instance() {
    assert(_NullPointerException_instance != NULL, "initialization problem");
    return _NullPointerException_instance;
  }
  ciInstance* ArithmeticException_instance() {
    assert(_ArithmeticException_instance != NULL, "initialization problem");
    return _ArithmeticException_instance;
  }

  // Lazy constructors:
  ciInstance* ArrayIndexOutOfBoundsException_instance();
  ciInstance* ArrayStoreException_instance();
  ciInstance* ClassCastException_instance();

  ciInstance* the_null_string();
  ciInstance* the_min_jint_string();

  static ciSymbol* unloaded_cisymbol() {
    return _unloaded_cisymbol;
  }
  static ciObjArrayKlass* unloaded_ciobjarrayklass() {
    return _unloaded_ciobjarrayklass;
  }
  static ciInstanceKlass* unloaded_ciinstance_klass() {
    return _unloaded_ciinstance_klass;
  }
  ciInstance* unloaded_ciinstance();

  // Note:  To find a class from its name string, use ciSymbol::make,
  // but consider adding to vmSymbols.hpp instead.

  // converts the ciKlass* representing the holder of a method into a
  // ciInstanceKlass*.  This is needed since the holder of a method in
  // the bytecodes could be an array type.  Basically this converts
  // array types into java/lang/Object and other types stay as they are.
  static ciInstanceKlass* get_instance_klass_for_declared_method_holder(ciKlass* klass);

  // Access to the compile-lifetime allocation arena.
  Arena*    arena() { return _arena; }

  // What is the current compilation environment?
  static ciEnv* current() { return CompilerThread::current()->env(); }

  // Overload with current thread argument
  static ciEnv* current(CompilerThread *thread) { return thread->env(); }

  // Per-compiler data.  (Used by C2 to publish the Compile* pointer.)
  void* compiler_data() { return _compiler_data; }
  void set_compiler_data(void* x) { _compiler_data = x; }

  // Notice that a method has been inlined in the current compile;
  // used only for statistics.
  void notice_inlined_method(ciMethod* method);

  // Total number of bytecodes in inlined methods in this compile
  int num_inlined_bytecodes() const;

  // Output stream for logging compilation info.
  CompileLog* log() { return _log; }
  void set_log(CompileLog* log) { _log = log; }

  void record_failure(const char* reason);      // Record failure and report later
  void report_failure(const char* reason);      // Report failure immediately
  void record_method_not_compilable(const char* reason, bool all_tiers = false);
  void record_out_of_memory_failure();

  // RedefineClasses support
  void metadata_do(MetadataClosure* f) { _factory->metadata_do(f); }

  // Dump the compilation replay data for the ciEnv to the stream.
  void dump_replay_data(int compile_id);
  void dump_inline_data(int compile_id);
  void dump_replay_data(outputStream* out);
  void dump_replay_data_unsafe(outputStream* out);
  void dump_compile_data(outputStream* out);
};

#endif // SHARE_CI_CIENV_HPP
