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

#include "precompiled.hpp"
#include "ci/ciCallSite.hpp"
#include "ci/ciMethodHandle.hpp"
#include "ci/ciSymbols.hpp"
#include "classfile/vmSymbols.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/compileLog.hpp"
#include "interpreter/linkResolver.hpp"
#include "opto/addnode.hpp"
#include "opto/callGenerator.hpp"
#include "opto/castnode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/mulnode.hpp"
#include "opto/parse.hpp"
#include "opto/rootnode.hpp"
#include "opto/runtime.hpp"
#include "opto/subnode.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/sharedRuntime.hpp"

void trace_type_profile(Compile* C, ciMethod *method, int depth, int bci, ciMethod *prof_method, ciKlass *prof_klass, int site_count, int receiver_count) {
  if (TraceTypeProfile || C->print_inlining()) {
    outputStream* out = tty;
    if (!C->print_inlining()) {
      if (!PrintOpto && !PrintCompilation) {
        method->print_short_name();
        tty->cr();
      }
      CompileTask::print_inlining_tty(prof_method, depth, bci);
    } else {
      out = C->print_inlining_stream();
    }
    CompileTask::print_inline_indent(depth, out);
    out->print(" \\-> TypeProfile (%d/%d counts) = ", receiver_count, site_count);
    stringStream ss;
    prof_klass->name()->print_symbol_on(&ss);
    out->print("%s", ss.as_string());
    out->cr();
  }
}

CallGenerator* Compile::call_generator(ciMethod* callee, int vtable_index, bool call_does_dispatch,
                                       JVMState* jvms, bool allow_inline,
                                       float prof_factor, ciKlass* speculative_receiver_type,
                                       bool allow_intrinsics) {
  ciMethod*       caller   = jvms->method();
  int             bci      = jvms->bci();
  Bytecodes::Code bytecode = caller->java_code_at_bci(bci);
  guarantee(callee != NULL, "failed method resolution");

  const bool is_virtual_or_interface = (bytecode == Bytecodes::_invokevirtual) ||
                                       (bytecode == Bytecodes::_invokeinterface);

  // Dtrace currently doesn't work unless all calls are vanilla
  if (env()->dtrace_method_probes()) {
    allow_inline = false;
  }

  // Note: When we get profiling during stage-1 compiles, we want to pull
  // from more specific profile data which pertains to this inlining.
  // Right now, ignore the information in jvms->caller(), and do method[bci].
  ciCallProfile profile = caller->call_profile_at_bci(bci);

  // See how many times this site has been invoked.
  int site_count = profile.count();
  int receiver_count = -1;
  if (call_does_dispatch && UseTypeProfile && profile.has_receiver(0)) {
    // Receivers in the profile structure are ordered by call counts
    // so that the most called (major) receiver is profile.receiver(0).
    receiver_count = profile.receiver_count(0);
  }

  CompileLog* log = this->log();
  if (log != NULL) {
    int rid = (receiver_count >= 0)? log->identify(profile.receiver(0)): -1;
    int r2id = (rid != -1 && profile.has_receiver(1))? log->identify(profile.receiver(1)):-1;
    log->begin_elem("call method='%d' count='%d' prof_factor='%f'",
                    log->identify(callee), site_count, prof_factor);
    if (call_does_dispatch)  log->print(" virtual='1'");
    if (allow_inline)     log->print(" inline='1'");
    if (receiver_count >= 0) {
      log->print(" receiver='%d' receiver_count='%d'", rid, receiver_count);
      if (profile.has_receiver(1)) {
        log->print(" receiver2='%d' receiver2_count='%d'", r2id, profile.receiver_count(1));
      }
    }
    if (callee->is_method_handle_intrinsic()) {
      log->print(" method_handle_intrinsic='1'");
    }
    log->end_elem();
  }

  // Special case the handling of certain common, profitable library
  // methods.  If these methods are replaced with specialized code,
  // then we return it as the inlined version of the call.
  CallGenerator* cg_intrinsic = NULL;
  if (allow_inline && allow_intrinsics) {
    CallGenerator* cg = find_intrinsic(callee, call_does_dispatch);
    if (cg != NULL) {
      if (cg->is_predicated()) {
        // Code without intrinsic but, hopefully, inlined.
        CallGenerator* inline_cg = this->call_generator(callee,
              vtable_index, call_does_dispatch, jvms, allow_inline, prof_factor, speculative_receiver_type, false);
        if (inline_cg != NULL) {
          cg = CallGenerator::for_predicated_intrinsic(cg, inline_cg);
        }
      }

      // If intrinsic does the virtual dispatch, we try to use the type profile
      // first, and hopefully inline it as the regular virtual call below.
      // We will retry the intrinsic if nothing had claimed it afterwards.
      if (cg->does_virtual_dispatch()) {
        cg_intrinsic = cg;
        cg = NULL;
      } else if (should_delay_vector_inlining(callee, jvms)) {
        return CallGenerator::for_late_inline(callee, cg);
      } else {
        return cg;
      }
    }
  }

  // Do method handle calls.
  // NOTE: This must happen before normal inlining logic below since
  // MethodHandle.invoke* are native methods which obviously don't
  // have bytecodes and so normal inlining fails.
  if (callee->is_method_handle_intrinsic()) {
    CallGenerator* cg = CallGenerator::for_method_handle_call(jvms, caller, callee, allow_inline);
    return cg;
  }

  // Attempt to inline...
  if (allow_inline) {
    // The profile data is only partly attributable to this caller,
    // scale back the call site information.
    float past_uses = jvms->method()->scale_count(site_count, prof_factor);
    // This is the number of times we expect the call code to be used.
    float expected_uses = past_uses;

    // Try inlining a bytecoded method:
    if (!call_does_dispatch) {
      InlineTree* ilt = InlineTree::find_subtree_from_root(this->ilt(), jvms->caller(), jvms->method());
      bool should_delay = false;
      if (ilt->ok_to_inline(callee, jvms, profile, should_delay)) {
        CallGenerator* cg = CallGenerator::for_inline(callee, expected_uses);
        // For optimized virtual calls assert at runtime that receiver object
        // is a subtype of the inlined method holder. CHA can report a method
        // as a unique target under an abstract method, but receiver type
        // sometimes has a broader type. Similar scenario is possible with
        // default methods when type system loses information about implemented
        // interfaces.
        if (cg != NULL && is_virtual_or_interface && !callee->is_static()) {
          CallGenerator* trap_cg = CallGenerator::for_uncommon_trap(callee,
              Deoptimization::Reason_receiver_constraint, Deoptimization::Action_none);

          cg = CallGenerator::for_guarded_call(callee->holder(), trap_cg, cg);
        }
        if (cg != NULL) {
          // Delay the inlining of this method to give us the
          // opportunity to perform some high level optimizations
          // first.
          if (should_delay_string_inlining(callee, jvms)) {
            return CallGenerator::for_string_late_inline(callee, cg);
          } else if (should_delay_boxing_inlining(callee, jvms)) {
            return CallGenerator::for_boxing_late_inline(callee, cg);
          } else if (should_delay_vector_reboxing_inlining(callee, jvms)) {
            return CallGenerator::for_vector_reboxing_late_inline(callee, cg);
          } else if ((should_delay || AlwaysIncrementalInline)) {
            return CallGenerator::for_late_inline(callee, cg);
          } else {
            return cg;
          }
        }
      }
    }

    // Try using the type profile.
    if (call_does_dispatch && site_count > 0 && UseTypeProfile) {
      // The major receiver's count >= TypeProfileMajorReceiverPercent of site_count.
      bool have_major_receiver = profile.has_receiver(0) && (100.*profile.receiver_prob(0) >= (float)TypeProfileMajorReceiverPercent);
      ciMethod* receiver_method = NULL;

      int morphism = profile.morphism();
      if (speculative_receiver_type != NULL) {
        if (!too_many_traps_or_recompiles(caller, bci, Deoptimization::Reason_speculate_class_check)) {
          // We have a speculative type, we should be able to resolve
          // the call. We do that before looking at the profiling at
          // this invoke because it may lead to bimorphic inlining which
          // a speculative type should help us avoid.
          receiver_method = callee->resolve_invoke(jvms->method()->holder(),
                                                   speculative_receiver_type);
          if (receiver_method == NULL) {
            speculative_receiver_type = NULL;
          } else {
            morphism = 1;
          }
        } else {
          // speculation failed before. Use profiling at the call
          // (could allow bimorphic inlining for instance).
          speculative_receiver_type = NULL;
        }
      }
      if (receiver_method == NULL &&
          (have_major_receiver || morphism == 1 ||
           (morphism == 2 && UseBimorphicInlining))) {
        // receiver_method = profile.method();
        // Profiles do not suggest methods now.  Look it up in the major receiver.
        receiver_method = callee->resolve_invoke(jvms->method()->holder(),
                                                      profile.receiver(0));
      }
      if (receiver_method != NULL) {
        // The single majority receiver sufficiently outweighs the minority.
        CallGenerator* hit_cg = this->call_generator(receiver_method,
              vtable_index, !call_does_dispatch, jvms, allow_inline, prof_factor);
        if (hit_cg != NULL) {
          // Look up second receiver.
          CallGenerator* next_hit_cg = NULL;
          ciMethod* next_receiver_method = NULL;
          if (morphism == 2 && UseBimorphicInlining) {
            next_receiver_method = callee->resolve_invoke(jvms->method()->holder(),
                                                               profile.receiver(1));
            if (next_receiver_method != NULL) {
              next_hit_cg = this->call_generator(next_receiver_method,
                                  vtable_index, !call_does_dispatch, jvms,
                                  allow_inline, prof_factor);
              if (next_hit_cg != NULL && !next_hit_cg->is_inline() &&
                  have_major_receiver && UseOnlyInlinedBimorphic) {
                  // Skip if we can't inline second receiver's method
                  next_hit_cg = NULL;
              }
            }
          }
          CallGenerator* miss_cg;
          Deoptimization::DeoptReason reason = (morphism == 2
                                               ? Deoptimization::Reason_bimorphic
                                               : Deoptimization::reason_class_check(speculative_receiver_type != NULL));
          if ((morphism == 1 || (morphism == 2 && next_hit_cg != NULL)) &&
              !too_many_traps_or_recompiles(caller, bci, reason)
             ) {
            // Generate uncommon trap for class check failure path
            // in case of monomorphic or bimorphic virtual call site.
            miss_cg = CallGenerator::for_uncommon_trap(callee, reason,
                        Deoptimization::Action_maybe_recompile);
          } else {
            // Generate virtual call for class check failure path
            // in case of polymorphic virtual call site.
            miss_cg = (IncrementalInlineVirtual ? CallGenerator::for_late_inline_virtual(callee, vtable_index, prof_factor)
                                                : CallGenerator::for_virtual_call(callee, vtable_index));
          }
          if (miss_cg != NULL) {
            if (next_hit_cg != NULL) {
              assert(speculative_receiver_type == NULL, "shouldn't end up here if we used speculation");
              trace_type_profile(C, jvms->method(), jvms->depth() - 1, jvms->bci(), next_receiver_method, profile.receiver(1), site_count, profile.receiver_count(1));
              // We don't need to record dependency on a receiver here and below.
              // Whenever we inline, the dependency is added by Parse::Parse().
              miss_cg = CallGenerator::for_predicted_call(profile.receiver(1), miss_cg, next_hit_cg, PROB_MAX);
            }
            if (miss_cg != NULL) {
              ciKlass* k = speculative_receiver_type != NULL ? speculative_receiver_type : profile.receiver(0);
              trace_type_profile(C, jvms->method(), jvms->depth() - 1, jvms->bci(), receiver_method, k, site_count, receiver_count);
              float hit_prob = speculative_receiver_type != NULL ? 1.0 : profile.receiver_prob(0);
              CallGenerator* cg = CallGenerator::for_predicted_call(k, miss_cg, hit_cg, hit_prob);
              if (cg != NULL)  return cg;
            }
          }
        }
      }
    }

    // If there is only one implementor of this interface then we
    // may be able to bind this invoke directly to the implementing
    // klass but we need both a dependence on the single interface
    // and on the method we bind to. Additionally since all we know
    // about the receiver type is that it's supposed to implement the
    // interface we have to insert a check that it's the class we
    // expect.  Interface types are not checked by the verifier so
    // they are roughly equivalent to Object.
    // The number of implementors for declared_interface is less or
    // equal to the number of implementors for target->holder() so
    // if number of implementors of target->holder() == 1 then
    // number of implementors for decl_interface is 0 or 1. If
    // it's 0 then no class implements decl_interface and there's
    // no point in inlining.
    if (call_does_dispatch && bytecode == Bytecodes::_invokeinterface) {
      ciInstanceKlass* declared_interface =
          caller->get_declared_method_holder_at_bci(bci)->as_instance_klass();
      ciInstanceKlass* singleton = declared_interface->unique_implementor();

      if (singleton != NULL) {
        assert(singleton != declared_interface, "not a unique implementor");

        ciMethod* cha_monomorphic_target =
            callee->find_monomorphic_target(caller->holder(), declared_interface, singleton);

        if (cha_monomorphic_target != NULL &&
            cha_monomorphic_target->holder() != env()->Object_klass()) { // subtype check against Object is useless
          ciKlass* holder = cha_monomorphic_target->holder();

          // Try to inline the method found by CHA. Inlined method is guarded by the type check.
          CallGenerator* hit_cg = call_generator(cha_monomorphic_target,
              vtable_index, !call_does_dispatch, jvms, allow_inline, prof_factor);

          // Deoptimize on type check fail. The interpreter will throw ICCE for us.
          CallGenerator* miss_cg = CallGenerator::for_uncommon_trap(callee,
              Deoptimization::Reason_class_check, Deoptimization::Action_none);

          CallGenerator* cg = CallGenerator::for_guarded_call(holder, miss_cg, hit_cg);
          if (hit_cg != NULL && cg != NULL) {
            dependencies()->assert_unique_concrete_method(declared_interface, cha_monomorphic_target, declared_interface, callee);
            return cg;
          }
        }
      }
    } // call_does_dispatch && bytecode == Bytecodes::_invokeinterface

    // Nothing claimed the intrinsic, we go with straight-forward inlining
    // for already discovered intrinsic.
    if (allow_intrinsics && cg_intrinsic != NULL) {
      assert(cg_intrinsic->does_virtual_dispatch(), "sanity");
      return cg_intrinsic;
    }
  } // allow_inline

  // There was no special inlining tactic, or it bailed out.
  // Use a more generic tactic, like a simple call.
  if (call_does_dispatch) {
    const char* msg = "virtual call";
    if (C->print_inlining()) {
      print_inlining(callee, jvms->depth() - 1, jvms->bci(), msg);
    }
    C->log_inline_failure(msg);
    if (IncrementalInlineVirtual && allow_inline) {
      return CallGenerator::for_late_inline_virtual(callee, vtable_index, prof_factor); // attempt to inline through virtual call later
    } else {
      return CallGenerator::for_virtual_call(callee, vtable_index);
    }
  } else {
    // Class Hierarchy Analysis or Type Profile reveals a unique target, or it is a static or special call.
    CallGenerator* cg = CallGenerator::for_direct_call(callee, should_delay_inlining(callee, jvms));
    // For optimized virtual calls assert at runtime that receiver object
    // is a subtype of the method holder.
    if (cg != NULL && is_virtual_or_interface && !callee->is_static()) {
      CallGenerator* trap_cg = CallGenerator::for_uncommon_trap(callee,
          Deoptimization::Reason_receiver_constraint, Deoptimization::Action_none);
      cg = CallGenerator::for_guarded_call(callee->holder(), trap_cg, cg);
    }
    return cg;
  }
}

// Return true for methods that shouldn't be inlined early so that
// they are easier to analyze and optimize as intrinsics.
bool Compile::should_delay_string_inlining(ciMethod* call_method, JVMState* jvms) {
  if (has_stringbuilder()) {

    if ((call_method->holder() == C->env()->StringBuilder_klass() ||
         call_method->holder() == C->env()->StringBuffer_klass()) &&
        (jvms->method()->holder() == C->env()->StringBuilder_klass() ||
         jvms->method()->holder() == C->env()->StringBuffer_klass())) {
      // Delay SB calls only when called from non-SB code
      return false;
    }

    switch (call_method->intrinsic_id()) {
      case vmIntrinsics::_StringBuilder_void:
      case vmIntrinsics::_StringBuilder_int:
      case vmIntrinsics::_StringBuilder_String:
      case vmIntrinsics::_StringBuilder_append_char:
      case vmIntrinsics::_StringBuilder_append_int:
      case vmIntrinsics::_StringBuilder_append_String:
      case vmIntrinsics::_StringBuilder_toString:
      case vmIntrinsics::_StringBuffer_void:
      case vmIntrinsics::_StringBuffer_int:
      case vmIntrinsics::_StringBuffer_String:
      case vmIntrinsics::_StringBuffer_append_char:
      case vmIntrinsics::_StringBuffer_append_int:
      case vmIntrinsics::_StringBuffer_append_String:
      case vmIntrinsics::_StringBuffer_toString:
      case vmIntrinsics::_Integer_toString:
        return true;

      case vmIntrinsics::_String_String:
        {
          Node* receiver = jvms->map()->in(jvms->argoff() + 1);
          if (receiver->is_Proj() && receiver->in(0)->is_CallStaticJava()) {
            CallStaticJavaNode* csj = receiver->in(0)->as_CallStaticJava();
            ciMethod* m = csj->method();
            if (m != NULL &&
                (m->intrinsic_id() == vmIntrinsics::_StringBuffer_toString ||
                 m->intrinsic_id() == vmIntrinsics::_StringBuilder_toString))
              // Delay String.<init>(new SB())
              return true;
          }
          return false;
        }

      default:
        return false;
    }
  }
  return false;
}

bool Compile::should_delay_boxing_inlining(ciMethod* call_method, JVMState* jvms) {
  if (eliminate_boxing() && call_method->is_boxing_method()) {
    set_has_boxed_value(true);
    return aggressive_unboxing();
  }
  return false;
}

bool Compile::should_delay_vector_inlining(ciMethod* call_method, JVMState* jvms) {
  return EnableVectorSupport && call_method->is_vector_method();
}

bool Compile::should_delay_vector_reboxing_inlining(ciMethod* call_method, JVMState* jvms) {
  return EnableVectorSupport && (call_method->intrinsic_id() == vmIntrinsics::_VectorRebox);
}

// uncommon-trap call-sites where callee is unloaded, uninitialized or will not link
bool Parse::can_not_compile_call_site(ciMethod *dest_method, ciInstanceKlass* klass) {
  // Additional inputs to consider...
  // bc      = bc()
  // caller  = method()
  // iter().get_method_holder_index()
  assert( dest_method->is_loaded(), "ciTypeFlow should not let us get here" );
  // Interface classes can be loaded & linked and never get around to
  // being initialized.  Uncommon-trap for not-initialized static or
  // v-calls.  Let interface calls happen.
  ciInstanceKlass* holder_klass = dest_method->holder();
  if (!holder_klass->is_being_initialized() &&
      !holder_klass->is_initialized() &&
      !holder_klass->is_interface()) {
    uncommon_trap(Deoptimization::Reason_uninitialized,
                  Deoptimization::Action_reinterpret,
                  holder_klass);
    return true;
  }

  assert(dest_method->is_loaded(), "dest_method: typeflow responsibility");
  return false;
}

#ifdef ASSERT
static bool check_call_consistency(JVMState* jvms, CallGenerator* cg) {
  ciMethod* symbolic_info = jvms->method()->get_method_at_bci(jvms->bci());
  ciMethod* resolved_method = cg->method();
  if (!ciMethod::is_consistent_info(symbolic_info, resolved_method)) {
    tty->print_cr("JVMS:");
    jvms->dump();
    tty->print_cr("Bytecode info:");
    jvms->method()->get_method_at_bci(jvms->bci())->print(); tty->cr();
    tty->print_cr("Resolved method:");
    cg->method()->print(); tty->cr();
    return false;
  }
  return true;
}
#endif // ASSERT

//------------------------------do_call----------------------------------------
// Handle your basic call.  Inline if we can & want to, else just setup call.
void Parse::do_call() {
  // It's likely we are going to add debug info soon.
  // Also, if we inline a guy who eventually needs debug info for this JVMS,
  // our contribution to it is cleaned up right here.
  kill_dead_locals();

  C->print_inlining_assert_ready();

  // Set frequently used booleans
  const bool is_virtual = bc() == Bytecodes::_invokevirtual;
  const bool is_virtual_or_interface = is_virtual || bc() == Bytecodes::_invokeinterface;
  const bool has_receiver = Bytecodes::has_receiver(bc());

  // Find target being called
  bool             will_link;
  ciSignature*     declared_signature = NULL;
  ciMethod*        orig_callee  = iter().get_method(will_link, &declared_signature);  // callee in the bytecode
  ciInstanceKlass* holder_klass = orig_callee->holder();
  ciKlass*         holder       = iter().get_declared_method_holder();
  ciInstanceKlass* klass = ciEnv::get_instance_klass_for_declared_method_holder(holder);
  assert(declared_signature != NULL, "cannot be null");

  // Bump max node limit for JSR292 users
  if (bc() == Bytecodes::_invokedynamic || orig_callee->is_method_handle_intrinsic()) {
    C->set_max_node_limit(3*MaxNodeLimit);
  }

  // uncommon-trap when callee is unloaded, uninitialized or will not link
  // bailout when too many arguments for register representation
  if (!will_link || can_not_compile_call_site(orig_callee, klass)) {
    if (PrintOpto && (Verbose || WizardMode)) {
      method()->print_name(); tty->print_cr(" can not compile call at bci %d to:", bci());
      orig_callee->print_name(); tty->cr();
    }
    return;
  }
  assert(holder_klass->is_loaded(), "");
  //assert((bc_callee->is_static() || is_invokedynamic) == !has_receiver , "must match bc");  // XXX invokehandle (cur_bc_raw)
  // Note: this takes into account invokeinterface of methods declared in java/lang/Object,
  // which should be invokevirtuals but according to the VM spec may be invokeinterfaces
  assert(holder_klass->is_interface() || holder_klass->super() == NULL || (bc() != Bytecodes::_invokeinterface), "must match bc");
  // Note:  In the absence of miranda methods, an abstract class K can perform
  // an invokevirtual directly on an interface method I.m if K implements I.

  // orig_callee is the resolved callee which's signature includes the
  // appendix argument.
  const int nargs = orig_callee->arg_size();
  const bool is_signature_polymorphic = MethodHandles::is_signature_polymorphic(orig_callee->intrinsic_id());

  // Push appendix argument (MethodType, CallSite, etc.), if one.
  if (iter().has_appendix()) {
    ciObject* appendix_arg = iter().get_appendix();
    const TypeOopPtr* appendix_arg_type = TypeOopPtr::make_from_constant(appendix_arg, /* require_const= */ true);
    Node* appendix_arg_node = _gvn.makecon(appendix_arg_type);
    push(appendix_arg_node);
  }

  // ---------------------
  // Does Class Hierarchy Analysis reveal only a single target of a v-call?
  // Then we may inline or make a static call, but become dependent on there being only 1 target.
  // Does the call-site type profile reveal only one receiver?
  // Then we may introduce a run-time check and inline on the path where it succeeds.
  // The other path may uncommon_trap, check for another receiver, or do a v-call.

  // Try to get the most accurate receiver type
  ciMethod* callee             = orig_callee;
  int       vtable_index       = Method::invalid_vtable_index;
  bool      call_does_dispatch = false;

  // Speculative type of the receiver if any
  ciKlass* speculative_receiver_type = NULL;
  if (is_virtual_or_interface) {
    Node* receiver_node             = stack(sp() - nargs);
    const TypeOopPtr* receiver_type = _gvn.type(receiver_node)->isa_oopptr();
    // call_does_dispatch and vtable_index are out-parameters.  They might be changed.
    // For arrays, klass below is Object. When vtable calls are used,
    // resolving the call with Object would allow an illegal call to
    // finalize() on an array. We use holder instead: illegal calls to
    // finalize() won't be compiled as vtable calls (IC call
    // resolution will catch the illegal call) and the few legal calls
    // on array types won't be either.
    callee = C->optimize_virtual_call(method(), klass, holder, orig_callee,
                                      receiver_type, is_virtual,
                                      call_does_dispatch, vtable_index);  // out-parameters
    speculative_receiver_type = receiver_type != NULL ? receiver_type->speculative_type() : NULL;
  }

  // Additional receiver subtype checks for interface calls via invokespecial or invokeinterface.
  ciKlass* receiver_constraint = NULL;
  if (iter().cur_bc_raw() == Bytecodes::_invokespecial && !orig_callee->is_object_initializer()) {
    ciInstanceKlass* calling_klass = method()->holder();
    ciInstanceKlass* sender_klass = calling_klass;
    if (sender_klass->is_interface()) {
      receiver_constraint = sender_klass;
    }
  } else if (iter().cur_bc_raw() == Bytecodes::_invokeinterface && orig_callee->is_private()) {
    assert(holder->is_interface(), "How did we get a non-interface method here!");
    receiver_constraint = holder;
  }

  if (receiver_constraint != NULL) {
    Node* receiver_node = stack(sp() - nargs);
    Node* cls_node = makecon(TypeKlassPtr::make(receiver_constraint));
    Node* bad_type_ctrl = NULL;
    Node* casted_receiver = gen_checkcast(receiver_node, cls_node, &bad_type_ctrl);
    if (bad_type_ctrl != NULL) {
      PreserveJVMState pjvms(this);
      set_control(bad_type_ctrl);
      uncommon_trap(Deoptimization::Reason_class_check,
                    Deoptimization::Action_none);
    }
    if (stopped()) {
      return; // MUST uncommon-trap?
    }
    set_stack(sp() - nargs, casted_receiver);
  }

  // Note:  It's OK to try to inline a virtual call.
  // The call generator will not attempt to inline a polymorphic call
  // unless it knows how to optimize the receiver dispatch.
  bool try_inline = (C->do_inlining() || InlineAccessors);

  // ---------------------
  dec_sp(nargs);              // Temporarily pop args for JVM state of call
  JVMState* jvms = sync_jvms();

  // ---------------------
  // Decide call tactic.
  // This call checks with CHA, the interpreter profile, intrinsics table, etc.
  // It decides whether inlining is desirable or not.
  CallGenerator* cg = C->call_generator(callee, vtable_index, call_does_dispatch, jvms, try_inline, prof_factor(), speculative_receiver_type);

  // NOTE:  Don't use orig_callee and callee after this point!  Use cg->method() instead.
  orig_callee = callee = NULL;

  // ---------------------
  // Round double arguments before call
  round_double_arguments(cg->method());

  // Feed profiling data for arguments to the type system so it can
  // propagate it as speculative types
  record_profiled_arguments_for_speculation(cg->method(), bc());

#ifndef PRODUCT
  // bump global counters for calls
  count_compiled_calls(/*at_method_entry*/ false, cg->is_inline());

  // Record first part of parsing work for this call
  parse_histogram()->record_change();
#endif // not PRODUCT

  assert(jvms == this->jvms(), "still operating on the right JVMS");
  assert(jvms_in_sync(),       "jvms must carry full info into CG");

  // save across call, for a subsequent cast_not_null.
  Node* receiver = has_receiver ? argument(0) : NULL;

  // The extra CheckCastPPs for speculative types mess with PhaseStringOpts
  if (receiver != NULL && !call_does_dispatch && !cg->is_string_late_inline()) {
    // Feed profiling data for a single receiver to the type system so
    // it can propagate it as a speculative type
    receiver = record_profiled_receiver_for_speculation(receiver);
  }

  JVMState* new_jvms = cg->generate(jvms);
  if (new_jvms == NULL) {
    // When inlining attempt fails (e.g., too many arguments),
    // it may contaminate the current compile state, making it
    // impossible to pull back and try again.  Once we call
    // cg->generate(), we are committed.  If it fails, the whole
    // compilation task is compromised.
    if (failing())  return;

    // This can happen if a library intrinsic is available, but refuses
    // the call site, perhaps because it did not match a pattern the
    // intrinsic was expecting to optimize. Should always be possible to
    // get a normal java call that may inline in that case
    cg = C->call_generator(cg->method(), vtable_index, call_does_dispatch, jvms, try_inline, prof_factor(), speculative_receiver_type, /* allow_intrinsics= */ false);
    new_jvms = cg->generate(jvms);
    if (new_jvms == NULL) {
      guarantee(failing(), "call failed to generate:  calls should work");
      return;
    }
  }

  if (cg->is_inline()) {
    // Accumulate has_loops estimate
    C->env()->notice_inlined_method(cg->method());
  }

  // Reset parser state from [new_]jvms, which now carries results of the call.
  // Return value (if any) is already pushed on the stack by the cg.
  add_exception_states_from(new_jvms);
  if (new_jvms->map()->control() == top()) {
    stop_and_kill_map();
  } else {
    assert(new_jvms->same_calls_as(jvms), "method/bci left unchanged");
    set_jvms(new_jvms);
  }

  assert(check_call_consistency(jvms, cg), "inconsistent info");

  if (!stopped()) {
    // This was some sort of virtual call, which did a null check for us.
    // Now we can assert receiver-not-null, on the normal return path.
    if (receiver != NULL && cg->is_virtual()) {
      Node* cast = cast_not_null(receiver);
      // %%% assert(receiver == cast, "should already have cast the receiver");
    }

    ciType* rtype = cg->method()->return_type();
    ciType* ctype = declared_signature->return_type();

    if (Bytecodes::has_optional_appendix(iter().cur_bc_raw()) || is_signature_polymorphic) {
      // Be careful here with return types.
      if (ctype != rtype) {
        BasicType rt = rtype->basic_type();
        BasicType ct = ctype->basic_type();
        if (ct == T_VOID) {
          // It's OK for a method  to return a value that is discarded.
          // The discarding does not require any special action from the caller.
          // The Java code knows this, at VerifyType.isNullConversion.
          pop_node(rt);  // whatever it was, pop it
        } else if (rt == T_INT || is_subword_type(rt)) {
          // Nothing.  These cases are handled in lambda form bytecode.
          assert(ct == T_INT || is_subword_type(ct), "must match: rt=%s, ct=%s", type2name(rt), type2name(ct));
        } else if (is_reference_type(rt)) {
          assert(is_reference_type(ct), "rt=%s, ct=%s", type2name(rt), type2name(ct));
          if (ctype->is_loaded()) {
            const TypeOopPtr* arg_type = TypeOopPtr::make_from_klass(rtype->as_klass());
            const Type*       sig_type = TypeOopPtr::make_from_klass(ctype->as_klass());
            if (arg_type != NULL && !arg_type->higher_equal(sig_type)) {
              Node* retnode = pop();
              Node* cast_obj = _gvn.transform(new CheckCastPPNode(control(), retnode, sig_type));
              push(cast_obj);
            }
          }
        } else {
          assert(rt == ct, "unexpected mismatch: rt=%s, ct=%s", type2name(rt), type2name(ct));
          // push a zero; it's better than getting an oop/int mismatch
          pop_node(rt);
          Node* retnode = zerocon(ct);
          push_node(ct, retnode);
        }
        // Now that the value is well-behaved, continue with the call-site type.
        rtype = ctype;
      }
    } else {
      // Symbolic resolution enforces the types to be the same.
      // NOTE: We must relax the assert for unloaded types because two
      // different ciType instances of the same unloaded class type
      // can appear to be "loaded" by different loaders (depending on
      // the accessing class).
      assert(!rtype->is_loaded() || !ctype->is_loaded() || rtype == ctype,
             "mismatched return types: rtype=%s, ctype=%s", rtype->name(), ctype->name());
    }

    // If the return type of the method is not loaded, assert that the
    // value we got is a null.  Otherwise, we need to recompile.
    if (!rtype->is_loaded()) {
      if (PrintOpto && (Verbose || WizardMode)) {
        method()->print_name(); tty->print_cr(" asserting nullness of result at bci: %d", bci());
        cg->method()->print_name(); tty->cr();
      }
      if (C->log() != NULL) {
        C->log()->elem("assert_null reason='return' klass='%d'",
                       C->log()->identify(rtype));
      }
      // If there is going to be a trap, put it at the next bytecode:
      set_bci(iter().next_bci());
      null_assert(peek());
      set_bci(iter().cur_bci()); // put it back
    }
    BasicType ct = ctype->basic_type();
    if (is_reference_type(ct)) {
      record_profiled_return_for_speculation();
    }
  }

  // Restart record of parsing work after possible inlining of call
#ifndef PRODUCT
  parse_histogram()->set_initial_state(bc());
#endif
}

//---------------------------catch_call_exceptions-----------------------------
// Put a Catch and CatchProj nodes behind a just-created call.
// Send their caught exceptions to the proper handler.
// This may be used after a call to the rethrow VM stub,
// when it is needed to process unloaded exception classes.
void Parse::catch_call_exceptions(ciExceptionHandlerStream& handlers) {
  // Exceptions are delivered through this channel:
  Node* i_o = this->i_o();

  // Add a CatchNode.
  GrowableArray<int>* bcis = new (C->node_arena()) GrowableArray<int>(C->node_arena(), 8, 0, -1);
  GrowableArray<const Type*>* extypes = new (C->node_arena()) GrowableArray<const Type*>(C->node_arena(), 8, 0, NULL);
  GrowableArray<int>* saw_unloaded = new (C->node_arena()) GrowableArray<int>(C->node_arena(), 8, 0, 0);

  bool default_handler = false;
  for (; !handlers.is_done(); handlers.next()) {
    ciExceptionHandler* h        = handlers.handler();
    int                 h_bci    = h->handler_bci();
    ciInstanceKlass*    h_klass  = h->is_catch_all() ? env()->Throwable_klass() : h->catch_klass();
    // Do not introduce unloaded exception types into the graph:
    if (!h_klass->is_loaded()) {
      if (saw_unloaded->contains(h_bci)) {
        /* We've already seen an unloaded exception with h_bci,
           so don't duplicate. Duplication will cause the CatchNode to be
           unnecessarily large. See 4713716. */
        continue;
      } else {
        saw_unloaded->append(h_bci);
      }
    }
    const Type*         h_extype = TypeOopPtr::make_from_klass(h_klass);
    // (We use make_from_klass because it respects UseUniqueSubclasses.)
    h_extype = h_extype->join(TypeInstPtr::NOTNULL);
    assert(!h_extype->empty(), "sanity");
    // Note:  It's OK if the BCIs repeat themselves.
    bcis->append(h_bci);
    extypes->append(h_extype);
    if (h_bci == -1) {
      default_handler = true;
    }
  }

  if (!default_handler) {
    bcis->append(-1);
    extypes->append(TypeOopPtr::make_from_klass(env()->Throwable_klass())->is_instptr());
  }

  int len = bcis->length();
  CatchNode *cn = new CatchNode(control(), i_o, len+1);
  Node *catch_ = _gvn.transform(cn);

  // now branch with the exception state to each of the (potential)
  // handlers
  for(int i=0; i < len; i++) {
    // Setup JVM state to enter the handler.
    PreserveJVMState pjvms(this);
    // Locals are just copied from before the call.
    // Get control from the CatchNode.
    int handler_bci = bcis->at(i);
    Node* ctrl = _gvn.transform( new CatchProjNode(catch_, i+1,handler_bci));
    // This handler cannot happen?
    if (ctrl == top())  continue;
    set_control(ctrl);

    // Create exception oop
    const TypeInstPtr* extype = extypes->at(i)->is_instptr();
    Node *ex_oop = _gvn.transform(new CreateExNode(extypes->at(i), ctrl, i_o));

    // Handle unloaded exception classes.
    if (saw_unloaded->contains(handler_bci)) {
      // An unloaded exception type is coming here.  Do an uncommon trap.
#ifndef PRODUCT
      // We do not expect the same handler bci to take both cold unloaded
      // and hot loaded exceptions.  But, watch for it.
      if ((Verbose || WizardMode) && extype->is_loaded()) {
        tty->print("Warning: Handler @%d takes mixed loaded/unloaded exceptions in ", bci());
        method()->print_name(); tty->cr();
      } else if (PrintOpto && (Verbose || WizardMode)) {
        tty->print("Bailing out on unloaded exception type ");
        extype->klass()->print_name();
        tty->print(" at bci:%d in ", bci());
        method()->print_name(); tty->cr();
      }
#endif
      // Emit an uncommon trap instead of processing the block.
      set_bci(handler_bci);
      push_ex_oop(ex_oop);
      uncommon_trap(Deoptimization::Reason_unloaded,
                    Deoptimization::Action_reinterpret,
                    extype->klass(), "!loaded exception");
      set_bci(iter().cur_bci()); // put it back
      continue;
    }

    // go to the exception handler
    if (handler_bci < 0) {     // merge with corresponding rethrow node
      throw_to_exit(make_exception_state(ex_oop));
    } else {                      // Else jump to corresponding handle
      push_ex_oop(ex_oop);        // Clear stack and push just the oop.
      merge_exception(handler_bci);
    }
  }

  // The first CatchProj is for the normal return.
  // (Note:  If this is a call to rethrow_Java, this node goes dead.)
  set_control(_gvn.transform( new CatchProjNode(catch_, CatchProjNode::fall_through_index, CatchProjNode::no_handler_bci)));
}


//----------------------------catch_inline_exceptions--------------------------
// Handle all exceptions thrown by an inlined method or individual bytecode.
// Common case 1: we have no handler, so all exceptions merge right into
// the rethrow case.
// Case 2: we have some handlers, with loaded exception klasses that have
// no subklasses.  We do a Deutsch-Shiffman style type-check on the incoming
// exception oop and branch to the handler directly.
// Case 3: We have some handlers with subklasses or are not loaded at
// compile-time.  We have to call the runtime to resolve the exception.
// So we insert a RethrowCall and all the logic that goes with it.
void Parse::catch_inline_exceptions(SafePointNode* ex_map) {
  // Caller is responsible for saving away the map for normal control flow!
  assert(stopped(), "call set_map(NULL) first");
  assert(method()->has_exception_handlers(), "don't come here w/o work to do");

  Node* ex_node = saved_ex_oop(ex_map);
  if (ex_node == top()) {
    // No action needed.
    return;
  }
  const TypeInstPtr* ex_type = _gvn.type(ex_node)->isa_instptr();
  NOT_PRODUCT(if (ex_type==NULL) tty->print_cr("*** Exception not InstPtr"));
  if (ex_type == NULL)
    ex_type = TypeOopPtr::make_from_klass(env()->Throwable_klass())->is_instptr();

  // determine potential exception handlers
  ciExceptionHandlerStream handlers(method(), bci(),
                                    ex_type->klass()->as_instance_klass(),
                                    ex_type->klass_is_exact());

  // Start executing from the given throw state.  (Keep its stack, for now.)
  // Get the exception oop as known at compile time.
  ex_node = use_exception_state(ex_map);

  // Get the exception oop klass from its header
  Node* ex_klass_node = NULL;
  if (has_ex_handler() && !ex_type->klass_is_exact()) {
    Node* p = basic_plus_adr( ex_node, ex_node, oopDesc::klass_offset_in_bytes());
    ex_klass_node = _gvn.transform(LoadKlassNode::make(_gvn, NULL, immutable_memory(), p, TypeInstPtr::KLASS, TypeKlassPtr::OBJECT));

    // Compute the exception klass a little more cleverly.
    // Obvious solution is to simple do a LoadKlass from the 'ex_node'.
    // However, if the ex_node is a PhiNode, I'm going to do a LoadKlass for
    // each arm of the Phi.  If I know something clever about the exceptions
    // I'm loading the class from, I can replace the LoadKlass with the
    // klass constant for the exception oop.
    if (ex_node->is_Phi()) {
      ex_klass_node = new PhiNode(ex_node->in(0), TypeKlassPtr::OBJECT);
      for (uint i = 1; i < ex_node->req(); i++) {
        Node* ex_in = ex_node->in(i);
        if (ex_in == top() || ex_in == NULL) {
          // This path was not taken.
          ex_klass_node->init_req(i, top());
          continue;
        }
        Node* p = basic_plus_adr(ex_in, ex_in, oopDesc::klass_offset_in_bytes());
        Node* k = _gvn.transform( LoadKlassNode::make(_gvn, NULL, immutable_memory(), p, TypeInstPtr::KLASS, TypeKlassPtr::OBJECT));
        ex_klass_node->init_req( i, k );
      }
      _gvn.set_type(ex_klass_node, TypeKlassPtr::OBJECT);

    }
  }

  // Scan the exception table for applicable handlers.
  // If none, we can call rethrow() and be done!
  // If precise (loaded with no subklasses), insert a D.S. style
  // pointer compare to the correct handler and loop back.
  // If imprecise, switch to the Rethrow VM-call style handling.

  int remaining = handlers.count_remaining();

  // iterate through all entries sequentially
  for (;!handlers.is_done(); handlers.next()) {
    ciExceptionHandler* handler = handlers.handler();

    if (handler->is_rethrow()) {
      // If we fell off the end of the table without finding an imprecise
      // exception klass (and without finding a generic handler) then we
      // know this exception is not handled in this method.  We just rethrow
      // the exception into the caller.
      throw_to_exit(make_exception_state(ex_node));
      return;
    }

    // exception handler bci range covers throw_bci => investigate further
    int handler_bci = handler->handler_bci();

    if (remaining == 1) {
      push_ex_oop(ex_node);        // Push exception oop for handler
      if (PrintOpto && WizardMode) {
        tty->print_cr("  Catching every inline exception bci:%d -> handler_bci:%d", bci(), handler_bci);
      }
      merge_exception(handler_bci); // jump to handler
      return;                   // No more handling to be done here!
    }

    // Get the handler's klass
    ciInstanceKlass* klass = handler->catch_klass();

    if (!klass->is_loaded()) {  // klass is not loaded?
      // fall through into catch_call_exceptions which will emit a
      // handler with an uncommon trap.
      break;
    }

    if (klass->is_interface())  // should not happen, but...
      break;                    // bail out

    // Check the type of the exception against the catch type
    const TypeKlassPtr *tk = TypeKlassPtr::make(klass);
    Node* con = _gvn.makecon(tk);
    Node* not_subtype_ctrl = gen_subtype_check(ex_klass_node, con);
    if (!stopped()) {
      PreserveJVMState pjvms(this);
      const TypeInstPtr* tinst = TypeOopPtr::make_from_klass_unique(klass)->cast_to_ptr_type(TypePtr::NotNull)->is_instptr();
      assert(klass->has_subklass() || tinst->klass_is_exact(), "lost exactness");
      Node* ex_oop = _gvn.transform(new CheckCastPPNode(control(), ex_node, tinst));
      push_ex_oop(ex_oop);      // Push exception oop for handler
      if (PrintOpto && WizardMode) {
        tty->print("  Catching inline exception bci:%d -> handler_bci:%d -- ", bci(), handler_bci);
        klass->print_name();
        tty->cr();
      }
      merge_exception(handler_bci);
    }
    set_control(not_subtype_ctrl);

    // Come here if exception does not match handler.
    // Carry on with more handler checks.
    --remaining;
  }

  assert(!stopped(), "you should return if you finish the chain");

  // Oops, need to call into the VM to resolve the klasses at runtime.
  // Note:  This call must not deoptimize, since it is not a real at this bci!
  kill_dead_locals();

  make_runtime_call(RC_NO_LEAF | RC_MUST_THROW,
                    OptoRuntime::rethrow_Type(),
                    OptoRuntime::rethrow_stub(),
                    NULL, NULL,
                    ex_node);

  // Rethrow is a pure call, no side effects, only a result.
  // The result cannot be allocated, so we use I_O

  // Catch exceptions from the rethrow
  catch_call_exceptions(handlers);
}


// (Note:  Moved add_debug_info into GraphKit::add_safepoint_edges.)


#ifndef PRODUCT
void Parse::count_compiled_calls(bool at_method_entry, bool is_inline) {
  if( CountCompiledCalls ) {
    if( at_method_entry ) {
      // bump invocation counter if top method (for statistics)
      if (CountCompiledCalls && depth() == 1) {
        const TypePtr* addr_type = TypeMetadataPtr::make(method());
        Node* adr1 = makecon(addr_type);
        Node* adr2 = basic_plus_adr(adr1, adr1, in_bytes(Method::compiled_invocation_counter_offset()));
        increment_counter(adr2);
      }
    } else if (is_inline) {
      switch (bc()) {
      case Bytecodes::_invokevirtual:   increment_counter(SharedRuntime::nof_inlined_calls_addr()); break;
      case Bytecodes::_invokeinterface: increment_counter(SharedRuntime::nof_inlined_interface_calls_addr()); break;
      case Bytecodes::_invokestatic:
      case Bytecodes::_invokedynamic:
      case Bytecodes::_invokespecial:   increment_counter(SharedRuntime::nof_inlined_static_calls_addr()); break;
      default: fatal("unexpected call bytecode");
      }
    } else {
      switch (bc()) {
      case Bytecodes::_invokevirtual:   increment_counter(SharedRuntime::nof_normal_calls_addr()); break;
      case Bytecodes::_invokeinterface: increment_counter(SharedRuntime::nof_interface_calls_addr()); break;
      case Bytecodes::_invokestatic:
      case Bytecodes::_invokedynamic:
      case Bytecodes::_invokespecial:   increment_counter(SharedRuntime::nof_static_calls_addr()); break;
      default: fatal("unexpected call bytecode");
      }
    }
  }
}
#endif //PRODUCT


ciMethod* Compile::optimize_virtual_call(ciMethod* caller, ciInstanceKlass* klass,
                                         ciKlass* holder, ciMethod* callee,
                                         const TypeOopPtr* receiver_type, bool is_virtual,
                                         bool& call_does_dispatch, int& vtable_index,
                                         bool check_access) {
  // Set default values for out-parameters.
  call_does_dispatch = true;
  vtable_index       = Method::invalid_vtable_index;

  // Choose call strategy.
  ciMethod* optimized_virtual_method = optimize_inlining(caller, klass, holder, callee,
                                                         receiver_type, check_access);

  // Have the call been sufficiently improved such that it is no longer a virtual?
  if (optimized_virtual_method != NULL) {
    callee             = optimized_virtual_method;
    call_does_dispatch = false;
  } else if (!UseInlineCaches && is_virtual && callee->is_loaded()) {
    // We can make a vtable call at this site
    vtable_index = callee->resolve_vtable_index(caller->holder(), holder);
  }
  return callee;
}

// Identify possible target method and inlining style
ciMethod* Compile::optimize_inlining(ciMethod* caller, ciInstanceKlass* klass, ciKlass* holder,
                                     ciMethod* callee, const TypeOopPtr* receiver_type,
                                     bool check_access) {
  // only use for virtual or interface calls

  // If it is obviously final, do not bother to call find_monomorphic_target,
  // because the class hierarchy checks are not needed, and may fail due to
  // incompletely loaded classes.  Since we do our own class loading checks
  // in this module, we may confidently bind to any method.
  if (callee->can_be_statically_bound()) {
    return callee;
  }

  if (receiver_type == NULL) {
    return NULL; // no receiver type info
  }

  // Attempt to improve the receiver
  bool actual_receiver_is_exact = false;
  ciInstanceKlass* actual_receiver = klass;
  // Array methods are all inherited from Object, and are monomorphic.
  // finalize() call on array is not allowed.
  if (receiver_type->isa_aryptr() &&
      callee->holder() == env()->Object_klass() &&
      callee->name() != ciSymbols::finalize_method_name()) {
    return callee;
  }

  // All other interesting cases are instance klasses.
  if (!receiver_type->isa_instptr()) {
    return NULL;
  }

  ciInstanceKlass* receiver_klass = receiver_type->klass()->as_instance_klass();
  if (receiver_klass->is_loaded() && receiver_klass->is_initialized() && !receiver_klass->is_interface() &&
      (receiver_klass == actual_receiver || receiver_klass->is_subtype_of(actual_receiver))) {
    // ikl is a same or better type than the original actual_receiver,
    // e.g. static receiver from bytecodes.
    actual_receiver = receiver_klass;
    // Is the actual_receiver exact?
    actual_receiver_is_exact = receiver_type->klass_is_exact();
  }

  ciInstanceKlass*   calling_klass = caller->holder();
  ciMethod* cha_monomorphic_target = callee->find_monomorphic_target(calling_klass, klass, actual_receiver, check_access);

  if (cha_monomorphic_target != NULL) {
    // Hardwiring a virtual.
    assert(!callee->can_be_statically_bound(), "should have been handled earlier");
    assert(!cha_monomorphic_target->is_abstract(), "");
    if (!cha_monomorphic_target->can_be_statically_bound(actual_receiver)) {
      // If we inlined because CHA revealed only a single target method,
      // then we are dependent on that target method not getting overridden
      // by dynamic class loading.  Be sure to test the "static" receiver
      // dest_method here, as opposed to the actual receiver, which may
      // falsely lead us to believe that the receiver is final or private.
      dependencies()->assert_unique_concrete_method(actual_receiver, cha_monomorphic_target, holder, callee);
    }
    return cha_monomorphic_target;
  }

  // If the type is exact, we can still bind the method w/o a vcall.
  // (This case comes after CHA so we can see how much extra work it does.)
  if (actual_receiver_is_exact) {
    // In case of evolution, there is a dependence on every inlined method, since each
    // such method can be changed when its class is redefined.
    ciMethod* exact_method = callee->resolve_invoke(calling_klass, actual_receiver);
    if (exact_method != NULL) {
      return exact_method;
    }
  }

  return NULL;
}
