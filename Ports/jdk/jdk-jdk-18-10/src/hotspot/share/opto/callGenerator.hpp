/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_CALLGENERATOR_HPP
#define SHARE_OPTO_CALLGENERATOR_HPP

#include "compiler/compileBroker.hpp"
#include "opto/callnode.hpp"
#include "opto/compile.hpp"
#include "opto/type.hpp"
#include "runtime/deoptimization.hpp"

//---------------------------CallGenerator-------------------------------------
// The subclasses of this class handle generation of ideal nodes for
// call sites and method entry points.

class CallGenerator : public ResourceObj {
 private:
  ciMethod*             _method;                // The method being called.

 protected:
  CallGenerator(ciMethod* method) : _method(method) {}

  void do_late_inline_helper();

  virtual bool           do_late_inline_check(Compile* C, JVMState* jvms) { ShouldNotReachHere(); return false; }
  virtual CallGenerator* inline_cg()    const                             { ShouldNotReachHere(); return NULL;  }
  virtual bool           is_pure_call() const                             { ShouldNotReachHere(); return false; }

 public:
  // Accessors
  ciMethod*          method() const             { return _method; }

  // is_inline: At least some code implementing the method is copied here.
  virtual bool      is_inline() const           { return false; }
  // is_intrinsic: There's a method-specific way of generating the inline code.
  virtual bool      is_intrinsic() const        { return false; }
  // is_parse: Bytecodes implementing the specific method are copied here.
  virtual bool      is_parse() const            { return false; }
  // is_virtual: The call uses the receiver type to select or check the method.
  virtual bool      is_virtual() const          { return false; }
  // is_deferred: The decision whether to inline or not is deferred.
  virtual bool      is_deferred() const         { return false; }
  // is_predicated: Uses an explicit check (predicate).
  virtual bool      is_predicated() const       { return false; }
  virtual int       predicates_count() const    { return 0; }
  // is_trap: Does not return to the caller.  (E.g., uncommon trap.)
  virtual bool      is_trap() const             { return false; }
  // does_virtual_dispatch: Should try inlining as normal method first.
  virtual bool      does_virtual_dispatch() const     { return false; }

  // is_late_inline: supports conversion of call into an inline
  virtual bool      is_late_inline() const         { return false; }
  // same but for method handle calls
  virtual bool      is_mh_late_inline() const      { return false; }
  virtual bool      is_boxing_late_inline() const  { return false; }
  virtual bool      is_string_late_inline() const  { return false; }
  virtual bool      is_virtual_late_inline() const { return false; }

  // Replace the call with an inline version of the code
  virtual void do_late_inline() { ShouldNotReachHere(); }

  virtual CallNode* call_node() const { return NULL; }
  virtual CallGenerator* with_call_node(CallNode* call)  { return this; }

  virtual void set_unique_id(jlong id)          { fatal("unique id only for late inlines"); };
  virtual jlong unique_id() const               { fatal("unique id only for late inlines"); return 0; };

  virtual void set_callee_method(ciMethod* callee) { ShouldNotReachHere(); }

  // Note:  It is possible for a CG to be both inline and virtual.
  // (The hashCode intrinsic does a vtable check and an inlined fast path.)

  // Allocate CallGenerators only in Compile arena since some of them are referenced from CallNodes.
  void* operator new(size_t size) throw() {
    Compile* C = Compile::current();
    return ResourceObj::operator new(size, C->comp_arena());
  }

  // Utilities:
  const TypeFunc*   tf() const;

  // The given jvms has state and arguments for a call to my method.
  // Edges after jvms->argoff() carry all (pre-popped) argument values.
  //
  // Update the map with state and return values (if any) and return it.
  // The return values (0, 1, or 2) must be pushed on the map's stack,
  // and the sp of the jvms incremented accordingly.
  //
  // The jvms is returned on success.  Alternatively, a copy of the
  // given jvms, suitably updated, may be returned, in which case the
  // caller should discard the original jvms.
  //
  // The non-Parm edges of the returned map will contain updated global state,
  // and one or two edges before jvms->sp() will carry any return values.
  // Other map edges may contain locals or monitors, and should not
  // be changed in meaning.
  //
  // If the call traps, the returned map must have a control edge of top.
  // If the call can throw, the returned map must report has_exceptions().
  //
  // If the result is NULL, it means that this CallGenerator was unable
  // to handle the given call, and another CallGenerator should be consulted.
  virtual JVMState* generate(JVMState* jvms) = 0;

  // How to generate a call site that is inlined:
  static CallGenerator* for_inline(ciMethod* m, float expected_uses = -1);
  // How to generate code for an on-stack replacement handler.
  static CallGenerator* for_osr(ciMethod* m, int osr_bci);

  // How to generate vanilla out-of-line call sites:
  static CallGenerator* for_direct_call(ciMethod* m, bool separate_io_projs = false);   // static, special
  static CallGenerator* for_virtual_call(ciMethod* m, int vtable_index);  // virtual, interface

  static CallGenerator* for_method_handle_call(  JVMState* jvms, ciMethod* caller, ciMethod* callee, bool allow_inline);
  static CallGenerator* for_method_handle_inline(JVMState* jvms, ciMethod* caller, ciMethod* callee, bool allow_inline, bool& input_not_const);

  // How to generate a replace a direct call with an inline version
  static CallGenerator* for_late_inline(ciMethod* m, CallGenerator* inline_cg);
  static CallGenerator* for_mh_late_inline(ciMethod* caller, ciMethod* callee, bool input_not_const);
  static CallGenerator* for_string_late_inline(ciMethod* m, CallGenerator* inline_cg);
  static CallGenerator* for_boxing_late_inline(ciMethod* m, CallGenerator* inline_cg);
  static CallGenerator* for_vector_reboxing_late_inline(ciMethod* m, CallGenerator* inline_cg);
  static CallGenerator* for_late_inline_virtual(ciMethod* m, int vtable_index, float expected_uses);

  // How to make a call that optimistically assumes a receiver type:
  static CallGenerator* for_predicted_call(ciKlass* predicted_receiver,
                                           CallGenerator* if_missed,
                                           CallGenerator* if_hit,
                                           float hit_prob);

  static CallGenerator* for_guarded_call(ciKlass* predicted_receiver,
                                         CallGenerator* if_missed,
                                         CallGenerator* if_hit);

  // How to make a call that optimistically assumes a MethodHandle target:
  static CallGenerator* for_predicted_dynamic_call(ciMethodHandle* predicted_method_handle,
                                                   CallGenerator* if_missed,
                                                   CallGenerator* if_hit,
                                                   float hit_prob);

  // How to make a call that gives up and goes back to the interpreter:
  static CallGenerator* for_uncommon_trap(ciMethod* m,
                                          Deoptimization::DeoptReason reason,
                                          Deoptimization::DeoptAction action);

  // Registry for intrinsics:
  static CallGenerator* for_intrinsic(ciMethod* m);
  static void register_intrinsic(ciMethod* m, CallGenerator* cg);
  static CallGenerator* for_predicated_intrinsic(CallGenerator* intrinsic,
                                                 CallGenerator* cg);
  virtual Node* generate_predicate(JVMState* jvms, int predicate) { return NULL; };

  virtual void print_inlining_late(const char* msg) { ShouldNotReachHere(); }

  static void print_inlining(Compile* C, ciMethod* callee, int inline_level, int bci, const char* msg) {
    if (C->print_inlining()) {
      C->print_inlining(callee, inline_level, bci, msg);
    }
  }

  static void print_inlining_failure(Compile* C, ciMethod* callee, int inline_level, int bci, const char* msg) {
    print_inlining(C, callee, inline_level, bci, msg);
    C->log_inline_failure(msg);
  }

  static bool is_inlined_method_handle_intrinsic(JVMState* jvms, ciMethod* m);
  static bool is_inlined_method_handle_intrinsic(ciMethod* caller, int bci, ciMethod* m);
  static bool is_inlined_method_handle_intrinsic(ciMethod* symbolic_info, ciMethod* m);
};


//------------------------InlineCallGenerator----------------------------------
class InlineCallGenerator : public CallGenerator {
 protected:
  InlineCallGenerator(ciMethod* method) : CallGenerator(method) {}

 public:
  virtual bool      is_inline() const           { return true; }
};

#endif // SHARE_OPTO_CALLGENERATOR_HPP
