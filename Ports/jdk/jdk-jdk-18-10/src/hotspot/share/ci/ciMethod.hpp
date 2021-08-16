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

#ifndef SHARE_CI_CIMETHOD_HPP
#define SHARE_CI_CIMETHOD_HPP

#include "ci/ciFlags.hpp"
#include "ci/ciInstanceKlass.hpp"
#include "ci/ciObject.hpp"
#include "ci/ciSignature.hpp"
#include "classfile/vmIntrinsics.hpp"
#include "compiler/methodLiveness.hpp"
#include "compiler/compilerOracle.hpp"
#include "oops/method.hpp"
#include "runtime/handles.hpp"
#include "utilities/bitMap.hpp"

class ciMethodBlocks;
class MethodLiveness;
class Arena;
class BCEscapeAnalyzer;
class InlineTree;
class xmlStream;

// Whether profiling found an oop to be always, never or sometimes
// null
enum ProfilePtrKind {
  ProfileAlwaysNull,
  ProfileNeverNull,
  ProfileMaybeNull
};

// ciMethod
//
// This class represents a Method* in the HotSpot virtual
// machine.
class ciMethod : public ciMetadata {
  friend class CompileBroker;
  CI_PACKAGE_ACCESS
  friend class ciEnv;
  friend class ciExceptionHandlerStream;
  friend class ciBytecodeStream;
  friend class ciMethodHandle;
  friend class ciReplay;
  friend class InlineTree;

 private:
  // General method information.
  ciFlags          _flags;
  ciSymbol*        _name;
  ciInstanceKlass* _holder;
  ciSignature*     _signature;
  ciMethodData*    _method_data;
  ciMethodBlocks*   _method_blocks;

  // Code attributes.
  int _code_size;
  int _max_stack;
  int _max_locals;
  vmIntrinsicID _intrinsic_id;
  int _handler_count;
  int _nmethod_age;
  int _interpreter_invocation_count;
  int _interpreter_throwout_count;
  int _instructions_size;
  int _size_of_parameters;

  bool _uses_monitors;
  bool _balanced_monitors;
  bool _is_c1_compilable;
  bool _is_c2_compilable;
  bool _can_be_parsed;
  bool _can_be_statically_bound;
  bool _has_reserved_stack_access;
  bool _is_overpass;

  // Lazy fields, filled in on demand
  address              _code;
  ciExceptionHandler** _exception_handlers;

  // Optional liveness analyzer.
  MethodLiveness* _liveness;
#if defined(COMPILER2)
  ciTypeFlow*         _flow;
  BCEscapeAnalyzer*   _bcea;
#endif

  ciMethod(const methodHandle& h_m, ciInstanceKlass* holder);
  ciMethod(ciInstanceKlass* holder, ciSymbol* name, ciSymbol* signature, ciInstanceKlass* accessor);

  oop loader() const                             { return _holder->loader(); }

  const char* type_string()                      { return "ciMethod"; }

  void print_impl(outputStream* st);

  void load_code();

  bool ensure_method_data(const methodHandle& h_m);

  void code_at_put(int bci, Bytecodes::Code code) {
    Bytecodes::check(code);
    assert(0 <= bci && bci < code_size(), "valid bci");
    address bcp = _code + bci;
    *bcp = code;
  }

  // Check bytecode and profile data collected are compatible
  void assert_virtual_call_type_ok(int bci);
  void assert_call_type_ok(int bci);

  // Check and update the profile counter in case of overflow
  static int check_overflow(int c, Bytecodes::Code code);

 public:
  void check_is_loaded() const                   { assert(is_loaded(), "not loaded"); }

  // Basic method information.
  ciFlags flags() const                          { check_is_loaded(); return _flags; }
  ciSymbol* name() const                         { return _name; }
  ciInstanceKlass* holder() const                { return _holder; }
  ciMethodData* method_data();
  ciMethodData* method_data_or_null();

  // Signature information.
  ciSignature* signature() const                 { return _signature; }
  ciType*      return_type() const               { return _signature->return_type(); }
  int          arg_size_no_receiver() const      { return _signature->size(); }
  // Can only be used on loaded ciMethods
  int          arg_size() const                  {
    check_is_loaded();
    return _signature->size() + (_flags.is_static() ? 0 : 1);
  }
  // Report the number of elements on stack when invoking the current method.
  // If the method is loaded, arg_size() gives precise information about the
  // number of stack elements (using the method's signature and its flags).
  // However, if the method is not loaded, the number of stack elements must
  // be determined differently, as the method's flags are not yet available.
  // The invoke_arg_size() method assumes in that case that all bytecodes except
  // invokestatic and invokedynamic have a receiver that is also pushed onto the
  // stack by the caller of the current method.
  int invoke_arg_size(Bytecodes::Code code) const {
    if (is_loaded()) {
      return arg_size();
    } else {
      int arg_size = _signature->size();
      if (code != Bytecodes::_invokestatic &&
          code != Bytecodes::_invokedynamic) {
        arg_size++;
      }
      return arg_size;
    }
  }

  Method* get_Method() const {
    Method* m = (Method*)_metadata;
    assert(m != NULL, "illegal use of unloaded method");
    return m;
  }

  // Method code and related information.
  address code()                                 { if (_code == NULL) load_code(); return _code; }
  int code_size() const                          { check_is_loaded(); return _code_size; }
  int max_stack() const                          { check_is_loaded(); return _max_stack; }
  int max_locals() const                         { check_is_loaded(); return _max_locals; }
  vmIntrinsicID intrinsic_id() const             { check_is_loaded(); return _intrinsic_id; }
  bool has_exception_handlers() const            { check_is_loaded(); return _handler_count > 0; }
  int exception_table_length() const             { check_is_loaded(); return _handler_count; }
  int interpreter_invocation_count() const       { check_is_loaded(); return _interpreter_invocation_count; }
  int interpreter_throwout_count() const         { check_is_loaded(); return _interpreter_throwout_count; }
  int size_of_parameters() const                 { check_is_loaded(); return _size_of_parameters; }
  int nmethod_age() const                        { check_is_loaded(); return _nmethod_age; }

  // Should the method be compiled with an age counter?
  bool profile_aging() const;

  // Code size for inlining decisions.
  int code_size_for_inlining();

  bool caller_sensitive()      const { return get_Method()->caller_sensitive();      }
  bool force_inline()          const { return get_Method()->force_inline();          }
  bool dont_inline()           const { return get_Method()->dont_inline();           }
  bool intrinsic_candidate()   const { return get_Method()->intrinsic_candidate();   }
  bool is_static_initializer() const { return get_Method()->is_static_initializer(); }

  bool check_intrinsic_candidate() const {
    if (intrinsic_id() == vmIntrinsics::_blackhole) {
      // This is the intrinsic without an associated method, so no intrinsic_candidate
      // flag is set. The intrinsic is still correct.
      return true;
    }
    return (CheckIntrinsics ? intrinsic_candidate() : true);
  }

  int highest_osr_comp_level();

  Bytecodes::Code java_code_at_bci(int bci) {
    address bcp = code() + bci;
    return Bytecodes::java_code_at(NULL, bcp);
  }
  Bytecodes::Code raw_code_at_bci(int bci) {
    address bcp = code() + bci;
    return Bytecodes::code_at(NULL, bcp);
  }
  BCEscapeAnalyzer  *get_bcea();
  ciMethodBlocks    *get_method_blocks();

  bool    has_linenumber_table() const;          // length unknown until decompression

  int line_number_from_bci(int bci) const;

  // Runtime information.
  int           vtable_index();

  // Analysis and profiling.
  //
  // Usage note: liveness_at_bci and init_vars should be wrapped in ResourceMarks.
  bool          has_monitor_bytecodes() const    { return _uses_monitors; }
  bool          has_balanced_monitors();

  // Returns a bitmap indicating which locals are required to be
  // maintained as live for deopt.  raw_liveness_at_bci is always the
  // direct output of the liveness computation while liveness_at_bci
  // may mark all locals as live to improve support for debugging Java
  // code by maintaining the state of as many locals as possible.
  MethodLivenessResult raw_liveness_at_bci(int bci);
  MethodLivenessResult liveness_at_bci(int bci);

  // Get the interpreters viewpoint on oop liveness.  MethodLiveness is
  // conservative in the sense that it may consider locals to be live which
  // cannot be live, like in the case where a local could contain an oop or
  // a primitive along different paths.  In that case the local must be
  // dead when those paths merge. Since the interpreter's viewpoint is
  // used when gc'ing an interpreter frame we need to use its viewpoint
  // during OSR when loading the locals.

  ResourceBitMap live_local_oops_at_bci(int bci);

  bool needs_clinit_barrier() const;

#ifdef COMPILER1
  const BitMap& bci_block_start();
#endif

  ciTypeFlow*   get_flow_analysis();
  ciTypeFlow*   get_osr_flow_analysis(int osr_bci);  // alternate entry point
  ciCallProfile call_profile_at_bci(int bci);

  // Does type profiling provide any useful information at this point?
  bool          argument_profiled_type(int bci, int i, ciKlass*& type, ProfilePtrKind& ptr_kind);
  bool          parameter_profiled_type(int i, ciKlass*& type, ProfilePtrKind& ptr_kind);
  bool          return_profiled_type(int bci, ciKlass*& type, ProfilePtrKind& ptr_kind);

  ciField*      get_field_at_bci( int bci, bool &will_link);
  ciMethod*     get_method_at_bci(int bci, bool &will_link, ciSignature* *declared_signature);
  ciMethod*     get_method_at_bci(int bci) {
    bool ignored_will_link;
    ciSignature* ignored_declared_signature;
    return get_method_at_bci(bci, ignored_will_link, &ignored_declared_signature);
  }

  ciKlass*      get_declared_method_holder_at_bci(int bci);

  ciSignature*  get_declared_signature_at_bci(int bci) {
    bool ignored_will_link;
    ciSignature* declared_signature;
    get_method_at_bci(bci, ignored_will_link, &declared_signature);
    assert(declared_signature != NULL, "cannot be null");
    return declared_signature;
  }

  // Given a certain calling environment, find the monomorphic target
  // for the call.  Return NULL if the call is not monomorphic in
  // its calling environment.
  ciMethod* find_monomorphic_target(ciInstanceKlass* caller,
                                    ciInstanceKlass* callee_holder,
                                    ciInstanceKlass* actual_receiver,
                                    bool check_access = true);

  // Given a known receiver klass, find the target for the call.
  // Return NULL if the call has no target or is abstract.
  ciMethod* resolve_invoke(ciKlass* caller, ciKlass* exact_receiver, bool check_access = true, bool allow_abstract = false);

  // Find the proper vtable index to invoke this method.
  int resolve_vtable_index(ciKlass* caller, ciKlass* receiver);

  bool has_option(enum CompileCommand option);
  bool has_option_value(enum CompileCommand option, double& value);
  bool can_be_compiled();
  bool can_be_parsed() const { return _can_be_parsed; }
  bool has_compiled_code();
  void log_nmethod_identity(xmlStream* log);
  bool is_not_reached(int bci);
  bool was_executed_more_than(int times);
  bool has_unloaded_classes_in_signature();
  bool is_klass_loaded(int refinfo_index, bool must_be_resolved) const;
  bool check_call(int refinfo_index, bool is_static) const;
  bool ensure_method_data();  // make sure it exists in the VM also
  MethodCounters* ensure_method_counters();
  int instructions_size();
  int scale_count(int count, float prof_factor = 1.);  // make MDO count commensurate with IIC

  // Stack walking support
  bool is_ignored_by_security_stack_walk() const;

  // JSR 292 support
  bool is_method_handle_intrinsic()  const;
  bool is_compiled_lambda_form() const;
  bool has_member_arg() const;

  // What kind of ciObject is this?
  bool is_method() const                         { return true; }

  // Java access flags
  bool is_public      () const                   { return flags().is_public(); }
  bool is_private     () const                   { return flags().is_private(); }
  bool is_protected   () const                   { return flags().is_protected(); }
  bool is_static      () const                   { return flags().is_static(); }
  bool is_final       () const                   { return flags().is_final(); }
  bool is_synchronized() const                   { return flags().is_synchronized(); }
  bool is_native      () const                   { return flags().is_native(); }
  bool is_interface   () const                   { return flags().is_interface(); }
  bool is_abstract    () const                   { return flags().is_abstract(); }

  // Other flags
  bool is_final_method() const                   { return is_final() || holder()->is_final(); }
  bool is_default_method() const                 { return !is_abstract() && !is_private() &&
                                                          holder()->is_interface(); }
  bool is_overpass    () const                   { check_is_loaded(); return _is_overpass; }
  bool has_loops      () const;
  bool has_jsrs       () const;
  bool is_getter      () const;
  bool is_setter      () const;
  bool is_accessor    () const;
  bool is_initializer () const;
  bool is_empty       () const;
  bool can_be_statically_bound() const           { return _can_be_statically_bound; }
  bool has_reserved_stack_access() const         { return _has_reserved_stack_access; }
  bool is_boxing_method() const;
  bool is_unboxing_method() const;
  bool is_vector_method() const;
  bool is_object_initializer() const;

  bool can_be_statically_bound(ciInstanceKlass* context) const;

  // Replay data methods
  void dump_name_as_ascii(outputStream* st);
  void dump_replay_data(outputStream* st);

  // Print the bytecodes of this method.
  void print_codes_on(outputStream* st);
  void print_codes() {
    print_codes_on(tty);
  }
  void print_codes_on(int from, int to, outputStream* st);

  // Print the name of this method in various incarnations.
  void print_name(outputStream* st = tty);
  void print_short_name(outputStream* st = tty);

  static bool is_consistent_info(ciMethod* declared_method, ciMethod* resolved_method);
};

#endif // SHARE_CI_CIMETHOD_HPP
