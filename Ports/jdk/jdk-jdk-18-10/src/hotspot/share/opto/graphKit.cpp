/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciUtilities.hpp"
#include "classfile/javaClasses.hpp"
#include "ci/ciNativeEntryPoint.hpp"
#include "ci/ciObjArray.hpp"
#include "asm/register.hpp"
#include "compiler/compileLog.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/c2/barrierSetC2.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/resourceArea.hpp"
#include "opto/addnode.hpp"
#include "opto/castnode.hpp"
#include "opto/convertnode.hpp"
#include "opto/graphKit.hpp"
#include "opto/idealKit.hpp"
#include "opto/intrinsicnode.hpp"
#include "opto/locknode.hpp"
#include "opto/machnode.hpp"
#include "opto/opaquenode.hpp"
#include "opto/parse.hpp"
#include "opto/rootnode.hpp"
#include "opto/runtime.hpp"
#include "opto/subtypenode.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/sharedRuntime.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/powerOfTwo.hpp"
#include "utilities/growableArray.hpp"

//----------------------------GraphKit-----------------------------------------
// Main utility constructor.
GraphKit::GraphKit(JVMState* jvms)
  : Phase(Phase::Parser),
    _env(C->env()),
    _gvn(*C->initial_gvn()),
    _barrier_set(BarrierSet::barrier_set()->barrier_set_c2())
{
  _exceptions = jvms->map()->next_exception();
  if (_exceptions != NULL)  jvms->map()->set_next_exception(NULL);
  set_jvms(jvms);
}

// Private constructor for parser.
GraphKit::GraphKit()
  : Phase(Phase::Parser),
    _env(C->env()),
    _gvn(*C->initial_gvn()),
    _barrier_set(BarrierSet::barrier_set()->barrier_set_c2())
{
  _exceptions = NULL;
  set_map(NULL);
  debug_only(_sp = -99);
  debug_only(set_bci(-99));
}



//---------------------------clean_stack---------------------------------------
// Clear away rubbish from the stack area of the JVM state.
// This destroys any arguments that may be waiting on the stack.
void GraphKit::clean_stack(int from_sp) {
  SafePointNode* map      = this->map();
  JVMState*      jvms     = this->jvms();
  int            stk_size = jvms->stk_size();
  int            stkoff   = jvms->stkoff();
  Node*          top      = this->top();
  for (int i = from_sp; i < stk_size; i++) {
    if (map->in(stkoff + i) != top) {
      map->set_req(stkoff + i, top);
    }
  }
}


//--------------------------------sync_jvms-----------------------------------
// Make sure our current jvms agrees with our parse state.
JVMState* GraphKit::sync_jvms() const {
  JVMState* jvms = this->jvms();
  jvms->set_bci(bci());       // Record the new bci in the JVMState
  jvms->set_sp(sp());         // Record the new sp in the JVMState
  assert(jvms_in_sync(), "jvms is now in sync");
  return jvms;
}

//--------------------------------sync_jvms_for_reexecute---------------------
// Make sure our current jvms agrees with our parse state.  This version
// uses the reexecute_sp for reexecuting bytecodes.
JVMState* GraphKit::sync_jvms_for_reexecute() {
  JVMState* jvms = this->jvms();
  jvms->set_bci(bci());          // Record the new bci in the JVMState
  jvms->set_sp(reexecute_sp());  // Record the new sp in the JVMState
  return jvms;
}

#ifdef ASSERT
bool GraphKit::jvms_in_sync() const {
  Parse* parse = is_Parse();
  if (parse == NULL) {
    if (bci() !=      jvms()->bci())          return false;
    if (sp()  != (int)jvms()->sp())           return false;
    return true;
  }
  if (jvms()->method() != parse->method())    return false;
  if (jvms()->bci()    != parse->bci())       return false;
  int jvms_sp = jvms()->sp();
  if (jvms_sp          != parse->sp())        return false;
  int jvms_depth = jvms()->depth();
  if (jvms_depth       != parse->depth())     return false;
  return true;
}

// Local helper checks for special internal merge points
// used to accumulate and merge exception states.
// They are marked by the region's in(0) edge being the map itself.
// Such merge points must never "escape" into the parser at large,
// until they have been handed to gvn.transform.
static bool is_hidden_merge(Node* reg) {
  if (reg == NULL)  return false;
  if (reg->is_Phi()) {
    reg = reg->in(0);
    if (reg == NULL)  return false;
  }
  return reg->is_Region() && reg->in(0) != NULL && reg->in(0)->is_Root();
}

void GraphKit::verify_map() const {
  if (map() == NULL)  return;  // null map is OK
  assert(map()->req() <= jvms()->endoff(), "no extra garbage on map");
  assert(!map()->has_exceptions(),    "call add_exception_states_from 1st");
  assert(!is_hidden_merge(control()), "call use_exception_state, not set_map");
}

void GraphKit::verify_exception_state(SafePointNode* ex_map) {
  assert(ex_map->next_exception() == NULL, "not already part of a chain");
  assert(has_saved_ex_oop(ex_map), "every exception state has an ex_oop");
}
#endif

//---------------------------stop_and_kill_map---------------------------------
// Set _map to NULL, signalling a stop to further bytecode execution.
// First smash the current map's control to a constant, to mark it dead.
void GraphKit::stop_and_kill_map() {
  SafePointNode* dead_map = stop();
  if (dead_map != NULL) {
    dead_map->disconnect_inputs(C); // Mark the map as killed.
    assert(dead_map->is_killed(), "must be so marked");
  }
}


//--------------------------------stopped--------------------------------------
// Tell if _map is NULL, or control is top.
bool GraphKit::stopped() {
  if (map() == NULL)           return true;
  else if (control() == top()) return true;
  else                         return false;
}


//-----------------------------has_ex_handler----------------------------------
// Tell if this method or any caller method has exception handlers.
bool GraphKit::has_ex_handler() {
  for (JVMState* jvmsp = jvms(); jvmsp != NULL; jvmsp = jvmsp->caller()) {
    if (jvmsp->has_method() && jvmsp->method()->has_exception_handlers()) {
      return true;
    }
  }
  return false;
}

//------------------------------save_ex_oop------------------------------------
// Save an exception without blowing stack contents or other JVM state.
void GraphKit::set_saved_ex_oop(SafePointNode* ex_map, Node* ex_oop) {
  assert(!has_saved_ex_oop(ex_map), "clear ex-oop before setting again");
  ex_map->add_req(ex_oop);
  debug_only(verify_exception_state(ex_map));
}

inline static Node* common_saved_ex_oop(SafePointNode* ex_map, bool clear_it) {
  assert(GraphKit::has_saved_ex_oop(ex_map), "ex_oop must be there");
  Node* ex_oop = ex_map->in(ex_map->req()-1);
  if (clear_it)  ex_map->del_req(ex_map->req()-1);
  return ex_oop;
}

//-----------------------------saved_ex_oop------------------------------------
// Recover a saved exception from its map.
Node* GraphKit::saved_ex_oop(SafePointNode* ex_map) {
  return common_saved_ex_oop(ex_map, false);
}

//--------------------------clear_saved_ex_oop---------------------------------
// Erase a previously saved exception from its map.
Node* GraphKit::clear_saved_ex_oop(SafePointNode* ex_map) {
  return common_saved_ex_oop(ex_map, true);
}

#ifdef ASSERT
//---------------------------has_saved_ex_oop----------------------------------
// Erase a previously saved exception from its map.
bool GraphKit::has_saved_ex_oop(SafePointNode* ex_map) {
  return ex_map->req() == ex_map->jvms()->endoff()+1;
}
#endif

//-------------------------make_exception_state--------------------------------
// Turn the current JVM state into an exception state, appending the ex_oop.
SafePointNode* GraphKit::make_exception_state(Node* ex_oop) {
  sync_jvms();
  SafePointNode* ex_map = stop();  // do not manipulate this map any more
  set_saved_ex_oop(ex_map, ex_oop);
  return ex_map;
}


//--------------------------add_exception_state--------------------------------
// Add an exception to my list of exceptions.
void GraphKit::add_exception_state(SafePointNode* ex_map) {
  if (ex_map == NULL || ex_map->control() == top()) {
    return;
  }
#ifdef ASSERT
  verify_exception_state(ex_map);
  if (has_exceptions()) {
    assert(ex_map->jvms()->same_calls_as(_exceptions->jvms()), "all collected exceptions must come from the same place");
  }
#endif

  // If there is already an exception of exactly this type, merge with it.
  // In particular, null-checks and other low-level exceptions common up here.
  Node*       ex_oop  = saved_ex_oop(ex_map);
  const Type* ex_type = _gvn.type(ex_oop);
  if (ex_oop == top()) {
    // No action needed.
    return;
  }
  assert(ex_type->isa_instptr(), "exception must be an instance");
  for (SafePointNode* e2 = _exceptions; e2 != NULL; e2 = e2->next_exception()) {
    const Type* ex_type2 = _gvn.type(saved_ex_oop(e2));
    // We check sp also because call bytecodes can generate exceptions
    // both before and after arguments are popped!
    if (ex_type2 == ex_type
        && e2->_jvms->sp() == ex_map->_jvms->sp()) {
      combine_exception_states(ex_map, e2);
      return;
    }
  }

  // No pre-existing exception of the same type.  Chain it on the list.
  push_exception_state(ex_map);
}

//-----------------------add_exception_states_from-----------------------------
void GraphKit::add_exception_states_from(JVMState* jvms) {
  SafePointNode* ex_map = jvms->map()->next_exception();
  if (ex_map != NULL) {
    jvms->map()->set_next_exception(NULL);
    for (SafePointNode* next_map; ex_map != NULL; ex_map = next_map) {
      next_map = ex_map->next_exception();
      ex_map->set_next_exception(NULL);
      add_exception_state(ex_map);
    }
  }
}

//-----------------------transfer_exceptions_into_jvms-------------------------
JVMState* GraphKit::transfer_exceptions_into_jvms() {
  if (map() == NULL) {
    // We need a JVMS to carry the exceptions, but the map has gone away.
    // Create a scratch JVMS, cloned from any of the exception states...
    if (has_exceptions()) {
      _map = _exceptions;
      _map = clone_map();
      _map->set_next_exception(NULL);
      clear_saved_ex_oop(_map);
      debug_only(verify_map());
    } else {
      // ...or created from scratch
      JVMState* jvms = new (C) JVMState(_method, NULL);
      jvms->set_bci(_bci);
      jvms->set_sp(_sp);
      jvms->set_map(new SafePointNode(TypeFunc::Parms, jvms));
      set_jvms(jvms);
      for (uint i = 0; i < map()->req(); i++)  map()->init_req(i, top());
      set_all_memory(top());
      while (map()->req() < jvms->endoff())  map()->add_req(top());
    }
    // (This is a kludge, in case you didn't notice.)
    set_control(top());
  }
  JVMState* jvms = sync_jvms();
  assert(!jvms->map()->has_exceptions(), "no exceptions on this map yet");
  jvms->map()->set_next_exception(_exceptions);
  _exceptions = NULL;   // done with this set of exceptions
  return jvms;
}

static inline void add_n_reqs(Node* dstphi, Node* srcphi) {
  assert(is_hidden_merge(dstphi), "must be a special merge node");
  assert(is_hidden_merge(srcphi), "must be a special merge node");
  uint limit = srcphi->req();
  for (uint i = PhiNode::Input; i < limit; i++) {
    dstphi->add_req(srcphi->in(i));
  }
}
static inline void add_one_req(Node* dstphi, Node* src) {
  assert(is_hidden_merge(dstphi), "must be a special merge node");
  assert(!is_hidden_merge(src), "must not be a special merge node");
  dstphi->add_req(src);
}

//-----------------------combine_exception_states------------------------------
// This helper function combines exception states by building phis on a
// specially marked state-merging region.  These regions and phis are
// untransformed, and can build up gradually.  The region is marked by
// having a control input of its exception map, rather than NULL.  Such
// regions do not appear except in this function, and in use_exception_state.
void GraphKit::combine_exception_states(SafePointNode* ex_map, SafePointNode* phi_map) {
  if (failing())  return;  // dying anyway...
  JVMState* ex_jvms = ex_map->_jvms;
  assert(ex_jvms->same_calls_as(phi_map->_jvms), "consistent call chains");
  assert(ex_jvms->stkoff() == phi_map->_jvms->stkoff(), "matching locals");
  assert(ex_jvms->sp() == phi_map->_jvms->sp(), "matching stack sizes");
  assert(ex_jvms->monoff() == phi_map->_jvms->monoff(), "matching JVMS");
  assert(ex_jvms->scloff() == phi_map->_jvms->scloff(), "matching scalar replaced objects");
  assert(ex_map->req() == phi_map->req(), "matching maps");
  uint tos = ex_jvms->stkoff() + ex_jvms->sp();
  Node*         hidden_merge_mark = root();
  Node*         region  = phi_map->control();
  MergeMemNode* phi_mem = phi_map->merged_memory();
  MergeMemNode* ex_mem  = ex_map->merged_memory();
  if (region->in(0) != hidden_merge_mark) {
    // The control input is not (yet) a specially-marked region in phi_map.
    // Make it so, and build some phis.
    region = new RegionNode(2);
    _gvn.set_type(region, Type::CONTROL);
    region->set_req(0, hidden_merge_mark);  // marks an internal ex-state
    region->init_req(1, phi_map->control());
    phi_map->set_control(region);
    Node* io_phi = PhiNode::make(region, phi_map->i_o(), Type::ABIO);
    record_for_igvn(io_phi);
    _gvn.set_type(io_phi, Type::ABIO);
    phi_map->set_i_o(io_phi);
    for (MergeMemStream mms(phi_mem); mms.next_non_empty(); ) {
      Node* m = mms.memory();
      Node* m_phi = PhiNode::make(region, m, Type::MEMORY, mms.adr_type(C));
      record_for_igvn(m_phi);
      _gvn.set_type(m_phi, Type::MEMORY);
      mms.set_memory(m_phi);
    }
  }

  // Either or both of phi_map and ex_map might already be converted into phis.
  Node* ex_control = ex_map->control();
  // if there is special marking on ex_map also, we add multiple edges from src
  bool add_multiple = (ex_control->in(0) == hidden_merge_mark);
  // how wide was the destination phi_map, originally?
  uint orig_width = region->req();

  if (add_multiple) {
    add_n_reqs(region, ex_control);
    add_n_reqs(phi_map->i_o(), ex_map->i_o());
  } else {
    // ex_map has no merges, so we just add single edges everywhere
    add_one_req(region, ex_control);
    add_one_req(phi_map->i_o(), ex_map->i_o());
  }
  for (MergeMemStream mms(phi_mem, ex_mem); mms.next_non_empty2(); ) {
    if (mms.is_empty()) {
      // get a copy of the base memory, and patch some inputs into it
      const TypePtr* adr_type = mms.adr_type(C);
      Node* phi = mms.force_memory()->as_Phi()->slice_memory(adr_type);
      assert(phi->as_Phi()->region() == mms.base_memory()->in(0), "");
      mms.set_memory(phi);
      // Prepare to append interesting stuff onto the newly sliced phi:
      while (phi->req() > orig_width)  phi->del_req(phi->req()-1);
    }
    // Append stuff from ex_map:
    if (add_multiple) {
      add_n_reqs(mms.memory(), mms.memory2());
    } else {
      add_one_req(mms.memory(), mms.memory2());
    }
  }
  uint limit = ex_map->req();
  for (uint i = TypeFunc::Parms; i < limit; i++) {
    // Skip everything in the JVMS after tos.  (The ex_oop follows.)
    if (i == tos)  i = ex_jvms->monoff();
    Node* src = ex_map->in(i);
    Node* dst = phi_map->in(i);
    if (src != dst) {
      PhiNode* phi;
      if (dst->in(0) != region) {
        dst = phi = PhiNode::make(region, dst, _gvn.type(dst));
        record_for_igvn(phi);
        _gvn.set_type(phi, phi->type());
        phi_map->set_req(i, dst);
        // Prepare to append interesting stuff onto the new phi:
        while (dst->req() > orig_width)  dst->del_req(dst->req()-1);
      } else {
        assert(dst->is_Phi(), "nobody else uses a hidden region");
        phi = dst->as_Phi();
      }
      if (add_multiple && src->in(0) == ex_control) {
        // Both are phis.
        add_n_reqs(dst, src);
      } else {
        while (dst->req() < region->req())  add_one_req(dst, src);
      }
      const Type* srctype = _gvn.type(src);
      if (phi->type() != srctype) {
        const Type* dsttype = phi->type()->meet_speculative(srctype);
        if (phi->type() != dsttype) {
          phi->set_type(dsttype);
          _gvn.set_type(phi, dsttype);
        }
      }
    }
  }
  phi_map->merge_replaced_nodes_with(ex_map);
}

//--------------------------use_exception_state--------------------------------
Node* GraphKit::use_exception_state(SafePointNode* phi_map) {
  if (failing()) { stop(); return top(); }
  Node* region = phi_map->control();
  Node* hidden_merge_mark = root();
  assert(phi_map->jvms()->map() == phi_map, "sanity: 1-1 relation");
  Node* ex_oop = clear_saved_ex_oop(phi_map);
  if (region->in(0) == hidden_merge_mark) {
    // Special marking for internal ex-states.  Process the phis now.
    region->set_req(0, region);  // now it's an ordinary region
    set_jvms(phi_map->jvms());   // ...so now we can use it as a map
    // Note: Setting the jvms also sets the bci and sp.
    set_control(_gvn.transform(region));
    uint tos = jvms()->stkoff() + sp();
    for (uint i = 1; i < tos; i++) {
      Node* x = phi_map->in(i);
      if (x->in(0) == region) {
        assert(x->is_Phi(), "expected a special phi");
        phi_map->set_req(i, _gvn.transform(x));
      }
    }
    for (MergeMemStream mms(merged_memory()); mms.next_non_empty(); ) {
      Node* x = mms.memory();
      if (x->in(0) == region) {
        assert(x->is_Phi(), "nobody else uses a hidden region");
        mms.set_memory(_gvn.transform(x));
      }
    }
    if (ex_oop->in(0) == region) {
      assert(ex_oop->is_Phi(), "expected a special phi");
      ex_oop = _gvn.transform(ex_oop);
    }
  } else {
    set_jvms(phi_map->jvms());
  }

  assert(!is_hidden_merge(phi_map->control()), "hidden ex. states cleared");
  assert(!is_hidden_merge(phi_map->i_o()), "hidden ex. states cleared");
  return ex_oop;
}

//---------------------------------java_bc-------------------------------------
Bytecodes::Code GraphKit::java_bc() const {
  ciMethod* method = this->method();
  int       bci    = this->bci();
  if (method != NULL && bci != InvocationEntryBci)
    return method->java_code_at_bci(bci);
  else
    return Bytecodes::_illegal;
}

void GraphKit::uncommon_trap_if_should_post_on_exceptions(Deoptimization::DeoptReason reason,
                                                          bool must_throw) {
    // if the exception capability is set, then we will generate code
    // to check the JavaThread.should_post_on_exceptions flag to see
    // if we actually need to report exception events (for this
    // thread).  If we don't need to report exception events, we will
    // take the normal fast path provided by add_exception_events.  If
    // exception event reporting is enabled for this thread, we will
    // take the uncommon_trap in the BuildCutout below.

    // first must access the should_post_on_exceptions_flag in this thread's JavaThread
    Node* jthread = _gvn.transform(new ThreadLocalNode());
    Node* adr = basic_plus_adr(top(), jthread, in_bytes(JavaThread::should_post_on_exceptions_flag_offset()));
    Node* should_post_flag = make_load(control(), adr, TypeInt::INT, T_INT, Compile::AliasIdxRaw, MemNode::unordered);

    // Test the should_post_on_exceptions_flag vs. 0
    Node* chk = _gvn.transform( new CmpINode(should_post_flag, intcon(0)) );
    Node* tst = _gvn.transform( new BoolNode(chk, BoolTest::eq) );

    // Branch to slow_path if should_post_on_exceptions_flag was true
    { BuildCutout unless(this, tst, PROB_MAX);
      // Do not try anything fancy if we're notifying the VM on every throw.
      // Cf. case Bytecodes::_athrow in parse2.cpp.
      uncommon_trap(reason, Deoptimization::Action_none,
                    (ciKlass*)NULL, (char*)NULL, must_throw);
    }

}

//------------------------------builtin_throw----------------------------------
void GraphKit::builtin_throw(Deoptimization::DeoptReason reason, Node* arg) {
  bool must_throw = true;

  // If this particular condition has not yet happened at this
  // bytecode, then use the uncommon trap mechanism, and allow for
  // a future recompilation if several traps occur here.
  // If the throw is hot, try to use a more complicated inline mechanism
  // which keeps execution inside the compiled code.
  bool treat_throw_as_hot = false;
  ciMethodData* md = method()->method_data();

  if (ProfileTraps) {
    if (too_many_traps(reason)) {
      treat_throw_as_hot = true;
    }
    // (If there is no MDO at all, assume it is early in
    // execution, and that any deopts are part of the
    // startup transient, and don't need to be remembered.)

    // Also, if there is a local exception handler, treat all throws
    // as hot if there has been at least one in this method.
    if (C->trap_count(reason) != 0
        && method()->method_data()->trap_count(reason) != 0
        && has_ex_handler()) {
        treat_throw_as_hot = true;
    }
  }

  // If this throw happens frequently, an uncommon trap might cause
  // a performance pothole.  If there is a local exception handler,
  // and if this particular bytecode appears to be deoptimizing often,
  // let us handle the throw inline, with a preconstructed instance.
  // Note:   If the deopt count has blown up, the uncommon trap
  // runtime is going to flush this nmethod, not matter what.
  if (treat_throw_as_hot
      && (!StackTraceInThrowable || OmitStackTraceInFastThrow)) {
    // If the throw is local, we use a pre-existing instance and
    // punt on the backtrace.  This would lead to a missing backtrace
    // (a repeat of 4292742) if the backtrace object is ever asked
    // for its backtrace.
    // Fixing this remaining case of 4292742 requires some flavor of
    // escape analysis.  Leave that for the future.
    ciInstance* ex_obj = NULL;
    switch (reason) {
    case Deoptimization::Reason_null_check:
      ex_obj = env()->NullPointerException_instance();
      break;
    case Deoptimization::Reason_div0_check:
      ex_obj = env()->ArithmeticException_instance();
      break;
    case Deoptimization::Reason_range_check:
      ex_obj = env()->ArrayIndexOutOfBoundsException_instance();
      break;
    case Deoptimization::Reason_class_check:
      if (java_bc() == Bytecodes::_aastore) {
        ex_obj = env()->ArrayStoreException_instance();
      } else {
        ex_obj = env()->ClassCastException_instance();
      }
      break;
    default:
      break;
    }
    if (failing()) { stop(); return; }  // exception allocation might fail
    if (ex_obj != NULL) {
      if (env()->jvmti_can_post_on_exceptions()) {
        // check if we must post exception events, take uncommon trap if so
        uncommon_trap_if_should_post_on_exceptions(reason, must_throw);
        // here if should_post_on_exceptions is false
        // continue on with the normal codegen
      }

      // Cheat with a preallocated exception object.
      if (C->log() != NULL)
        C->log()->elem("hot_throw preallocated='1' reason='%s'",
                       Deoptimization::trap_reason_name(reason));
      const TypeInstPtr* ex_con  = TypeInstPtr::make(ex_obj);
      Node*              ex_node = _gvn.transform(ConNode::make(ex_con));

      // Clear the detail message of the preallocated exception object.
      // Weblogic sometimes mutates the detail message of exceptions
      // using reflection.
      int offset = java_lang_Throwable::get_detailMessage_offset();
      const TypePtr* adr_typ = ex_con->add_offset(offset);

      Node *adr = basic_plus_adr(ex_node, ex_node, offset);
      const TypeOopPtr* val_type = TypeOopPtr::make_from_klass(env()->String_klass());
      Node *store = access_store_at(ex_node, adr, adr_typ, null(), val_type, T_OBJECT, IN_HEAP);

      add_exception_state(make_exception_state(ex_node));
      return;
    }
  }

  // %%% Maybe add entry to OptoRuntime which directly throws the exc.?
  // It won't be much cheaper than bailing to the interp., since we'll
  // have to pass up all the debug-info, and the runtime will have to
  // create the stack trace.

  // Usual case:  Bail to interpreter.
  // Reserve the right to recompile if we haven't seen anything yet.

  ciMethod* m = Deoptimization::reason_is_speculate(reason) ? C->method() : NULL;
  Deoptimization::DeoptAction action = Deoptimization::Action_maybe_recompile;
  if (treat_throw_as_hot
      && (method()->method_data()->trap_recompiled_at(bci(), m)
          || C->too_many_traps(reason))) {
    // We cannot afford to take more traps here.  Suffer in the interpreter.
    if (C->log() != NULL)
      C->log()->elem("hot_throw preallocated='0' reason='%s' mcount='%d'",
                     Deoptimization::trap_reason_name(reason),
                     C->trap_count(reason));
    action = Deoptimization::Action_none;
  }

  // "must_throw" prunes the JVM state to include only the stack, if there
  // are no local exception handlers.  This should cut down on register
  // allocation time and code size, by drastically reducing the number
  // of in-edges on the call to the uncommon trap.

  uncommon_trap(reason, action, (ciKlass*)NULL, (char*)NULL, must_throw);
}


//----------------------------PreserveJVMState---------------------------------
PreserveJVMState::PreserveJVMState(GraphKit* kit, bool clone_map) {
  debug_only(kit->verify_map());
  _kit    = kit;
  _map    = kit->map();   // preserve the map
  _sp     = kit->sp();
  kit->set_map(clone_map ? kit->clone_map() : NULL);
#ifdef ASSERT
  _bci    = kit->bci();
  Parse* parser = kit->is_Parse();
  int block = (parser == NULL || parser->block() == NULL) ? -1 : parser->block()->rpo();
  _block  = block;
#endif
}
PreserveJVMState::~PreserveJVMState() {
  GraphKit* kit = _kit;
#ifdef ASSERT
  assert(kit->bci() == _bci, "bci must not shift");
  Parse* parser = kit->is_Parse();
  int block = (parser == NULL || parser->block() == NULL) ? -1 : parser->block()->rpo();
  assert(block == _block,    "block must not shift");
#endif
  kit->set_map(_map);
  kit->set_sp(_sp);
}


//-----------------------------BuildCutout-------------------------------------
BuildCutout::BuildCutout(GraphKit* kit, Node* p, float prob, float cnt)
  : PreserveJVMState(kit)
{
  assert(p->is_Con() || p->is_Bool(), "test must be a bool");
  SafePointNode* outer_map = _map;   // preserved map is caller's
  SafePointNode* inner_map = kit->map();
  IfNode* iff = kit->create_and_map_if(outer_map->control(), p, prob, cnt);
  outer_map->set_control(kit->gvn().transform( new IfTrueNode(iff) ));
  inner_map->set_control(kit->gvn().transform( new IfFalseNode(iff) ));
}
BuildCutout::~BuildCutout() {
  GraphKit* kit = _kit;
  assert(kit->stopped(), "cutout code must stop, throw, return, etc.");
}

//---------------------------PreserveReexecuteState----------------------------
PreserveReexecuteState::PreserveReexecuteState(GraphKit* kit) {
  assert(!kit->stopped(), "must call stopped() before");
  _kit    =    kit;
  _sp     =    kit->sp();
  _reexecute = kit->jvms()->_reexecute;
}
PreserveReexecuteState::~PreserveReexecuteState() {
  if (_kit->stopped()) return;
  _kit->jvms()->_reexecute = _reexecute;
  _kit->set_sp(_sp);
}

//------------------------------clone_map--------------------------------------
// Implementation of PreserveJVMState
//
// Only clone_map(...) here. If this function is only used in the
// PreserveJVMState class we may want to get rid of this extra
// function eventually and do it all there.

SafePointNode* GraphKit::clone_map() {
  if (map() == NULL)  return NULL;

  // Clone the memory edge first
  Node* mem = MergeMemNode::make(map()->memory());
  gvn().set_type_bottom(mem);

  SafePointNode *clonemap = (SafePointNode*)map()->clone();
  JVMState* jvms = this->jvms();
  JVMState* clonejvms = jvms->clone_shallow(C);
  clonemap->set_memory(mem);
  clonemap->set_jvms(clonejvms);
  clonejvms->set_map(clonemap);
  record_for_igvn(clonemap);
  gvn().set_type_bottom(clonemap);
  return clonemap;
}


//-----------------------------set_map_clone-----------------------------------
void GraphKit::set_map_clone(SafePointNode* m) {
  _map = m;
  _map = clone_map();
  _map->set_next_exception(NULL);
  debug_only(verify_map());
}


//----------------------------kill_dead_locals---------------------------------
// Detect any locals which are known to be dead, and force them to top.
void GraphKit::kill_dead_locals() {
  // Consult the liveness information for the locals.  If any
  // of them are unused, then they can be replaced by top().  This
  // should help register allocation time and cut down on the size
  // of the deoptimization information.

  // This call is made from many of the bytecode handling
  // subroutines called from the Big Switch in do_one_bytecode.
  // Every bytecode which might include a slow path is responsible
  // for killing its dead locals.  The more consistent we
  // are about killing deads, the fewer useless phis will be
  // constructed for them at various merge points.

  // bci can be -1 (InvocationEntryBci).  We return the entry
  // liveness for the method.

  if (method() == NULL || method()->code_size() == 0) {
    // We are building a graph for a call to a native method.
    // All locals are live.
    return;
  }

  ResourceMark rm;

  // Consult the liveness information for the locals.  If any
  // of them are unused, then they can be replaced by top().  This
  // should help register allocation time and cut down on the size
  // of the deoptimization information.
  MethodLivenessResult live_locals = method()->liveness_at_bci(bci());

  int len = (int)live_locals.size();
  assert(len <= jvms()->loc_size(), "too many live locals");
  for (int local = 0; local < len; local++) {
    if (!live_locals.at(local)) {
      set_local(local, top());
    }
  }
}

#ifdef ASSERT
//-------------------------dead_locals_are_killed------------------------------
// Return true if all dead locals are set to top in the map.
// Used to assert "clean" debug info at various points.
bool GraphKit::dead_locals_are_killed() {
  if (method() == NULL || method()->code_size() == 0) {
    // No locals need to be dead, so all is as it should be.
    return true;
  }

  // Make sure somebody called kill_dead_locals upstream.
  ResourceMark rm;
  for (JVMState* jvms = this->jvms(); jvms != NULL; jvms = jvms->caller()) {
    if (jvms->loc_size() == 0)  continue;  // no locals to consult
    SafePointNode* map = jvms->map();
    ciMethod* method = jvms->method();
    int       bci    = jvms->bci();
    if (jvms == this->jvms()) {
      bci = this->bci();  // it might not yet be synched
    }
    MethodLivenessResult live_locals = method->liveness_at_bci(bci);
    int len = (int)live_locals.size();
    if (!live_locals.is_valid() || len == 0)
      // This method is trivial, or is poisoned by a breakpoint.
      return true;
    assert(len == jvms->loc_size(), "live map consistent with locals map");
    for (int local = 0; local < len; local++) {
      if (!live_locals.at(local) && map->local(jvms, local) != top()) {
        if (PrintMiscellaneous && (Verbose || WizardMode)) {
          tty->print_cr("Zombie local %d: ", local);
          jvms->dump();
        }
        return false;
      }
    }
  }
  return true;
}

#endif //ASSERT

// Helper function for enforcing certain bytecodes to reexecute if deoptimization happens.
static bool should_reexecute_implied_by_bytecode(JVMState *jvms, bool is_anewarray) {
  ciMethod* cur_method = jvms->method();
  int       cur_bci   = jvms->bci();
  if (cur_method != NULL && cur_bci != InvocationEntryBci) {
    Bytecodes::Code code = cur_method->java_code_at_bci(cur_bci);
    return Interpreter::bytecode_should_reexecute(code) ||
           (is_anewarray && code == Bytecodes::_multianewarray);
    // Reexecute _multianewarray bytecode which was replaced with
    // sequence of [a]newarray. See Parse::do_multianewarray().
    //
    // Note: interpreter should not have it set since this optimization
    // is limited by dimensions and guarded by flag so in some cases
    // multianewarray() runtime calls will be generated and
    // the bytecode should not be reexecutes (stack will not be reset).
  } else {
    return false;
  }
}

// Helper function for adding JVMState and debug information to node
void GraphKit::add_safepoint_edges(SafePointNode* call, bool must_throw) {
  // Add the safepoint edges to the call (or other safepoint).

  // Make sure dead locals are set to top.  This
  // should help register allocation time and cut down on the size
  // of the deoptimization information.
  assert(dead_locals_are_killed(), "garbage in debug info before safepoint");

  // Walk the inline list to fill in the correct set of JVMState's
  // Also fill in the associated edges for each JVMState.

  // If the bytecode needs to be reexecuted we need to put
  // the arguments back on the stack.
  const bool should_reexecute = jvms()->should_reexecute();
  JVMState* youngest_jvms = should_reexecute ? sync_jvms_for_reexecute() : sync_jvms();

  // NOTE: set_bci (called from sync_jvms) might reset the reexecute bit to
  // undefined if the bci is different.  This is normal for Parse but it
  // should not happen for LibraryCallKit because only one bci is processed.
  assert(!is_LibraryCallKit() || (jvms()->should_reexecute() == should_reexecute),
         "in LibraryCallKit the reexecute bit should not change");

  // If we are guaranteed to throw, we can prune everything but the
  // input to the current bytecode.
  bool can_prune_locals = false;
  uint stack_slots_not_pruned = 0;
  int inputs = 0, depth = 0;
  if (must_throw) {
    assert(method() == youngest_jvms->method(), "sanity");
    if (compute_stack_effects(inputs, depth)) {
      can_prune_locals = true;
      stack_slots_not_pruned = inputs;
    }
  }

  if (env()->should_retain_local_variables()) {
    // At any safepoint, this method can get breakpointed, which would
    // then require an immediate deoptimization.
    can_prune_locals = false;  // do not prune locals
    stack_slots_not_pruned = 0;
  }

  // do not scribble on the input jvms
  JVMState* out_jvms = youngest_jvms->clone_deep(C);
  call->set_jvms(out_jvms); // Start jvms list for call node

  // For a known set of bytecodes, the interpreter should reexecute them if
  // deoptimization happens. We set the reexecute state for them here
  if (out_jvms->is_reexecute_undefined() && //don't change if already specified
      should_reexecute_implied_by_bytecode(out_jvms, call->is_AllocateArray())) {
#ifdef ASSERT
    int inputs = 0, not_used; // initialized by GraphKit::compute_stack_effects()
    assert(method() == youngest_jvms->method(), "sanity");
    assert(compute_stack_effects(inputs, not_used), "unknown bytecode: %s", Bytecodes::name(java_bc()));
    assert(out_jvms->sp() >= (uint)inputs, "not enough operands for reexecution");
#endif // ASSERT
    out_jvms->set_should_reexecute(true); //NOTE: youngest_jvms not changed
  }

  // Presize the call:
  DEBUG_ONLY(uint non_debug_edges = call->req());
  call->add_req_batch(top(), youngest_jvms->debug_depth());
  assert(call->req() == non_debug_edges + youngest_jvms->debug_depth(), "");

  // Set up edges so that the call looks like this:
  //  Call [state:] ctl io mem fptr retadr
  //       [parms:] parm0 ... parmN
  //       [root:]  loc0 ... locN stk0 ... stkSP mon0 obj0 ... monN objN
  //    [...mid:]   loc0 ... locN stk0 ... stkSP mon0 obj0 ... monN objN [...]
  //       [young:] loc0 ... locN stk0 ... stkSP mon0 obj0 ... monN objN
  // Note that caller debug info precedes callee debug info.

  // Fill pointer walks backwards from "young:" to "root:" in the diagram above:
  uint debug_ptr = call->req();

  // Loop over the map input edges associated with jvms, add them
  // to the call node, & reset all offsets to match call node array.
  for (JVMState* in_jvms = youngest_jvms; in_jvms != NULL; ) {
    uint debug_end   = debug_ptr;
    uint debug_start = debug_ptr - in_jvms->debug_size();
    debug_ptr = debug_start;  // back up the ptr

    uint p = debug_start;  // walks forward in [debug_start, debug_end)
    uint j, k, l;
    SafePointNode* in_map = in_jvms->map();
    out_jvms->set_map(call);

    if (can_prune_locals) {
      assert(in_jvms->method() == out_jvms->method(), "sanity");
      // If the current throw can reach an exception handler in this JVMS,
      // then we must keep everything live that can reach that handler.
      // As a quick and dirty approximation, we look for any handlers at all.
      if (in_jvms->method()->has_exception_handlers()) {
        can_prune_locals = false;
      }
    }

    // Add the Locals
    k = in_jvms->locoff();
    l = in_jvms->loc_size();
    out_jvms->set_locoff(p);
    if (!can_prune_locals) {
      for (j = 0; j < l; j++)
        call->set_req(p++, in_map->in(k+j));
    } else {
      p += l;  // already set to top above by add_req_batch
    }

    // Add the Expression Stack
    k = in_jvms->stkoff();
    l = in_jvms->sp();
    out_jvms->set_stkoff(p);
    if (!can_prune_locals) {
      for (j = 0; j < l; j++)
        call->set_req(p++, in_map->in(k+j));
    } else if (can_prune_locals && stack_slots_not_pruned != 0) {
      // Divide stack into {S0,...,S1}, where S0 is set to top.
      uint s1 = stack_slots_not_pruned;
      stack_slots_not_pruned = 0;  // for next iteration
      if (s1 > l)  s1 = l;
      uint s0 = l - s1;
      p += s0;  // skip the tops preinstalled by add_req_batch
      for (j = s0; j < l; j++)
        call->set_req(p++, in_map->in(k+j));
    } else {
      p += l;  // already set to top above by add_req_batch
    }

    // Add the Monitors
    k = in_jvms->monoff();
    l = in_jvms->mon_size();
    out_jvms->set_monoff(p);
    for (j = 0; j < l; j++)
      call->set_req(p++, in_map->in(k+j));

    // Copy any scalar object fields.
    k = in_jvms->scloff();
    l = in_jvms->scl_size();
    out_jvms->set_scloff(p);
    for (j = 0; j < l; j++)
      call->set_req(p++, in_map->in(k+j));

    // Finish the new jvms.
    out_jvms->set_endoff(p);

    assert(out_jvms->endoff()     == debug_end,             "fill ptr must match");
    assert(out_jvms->depth()      == in_jvms->depth(),      "depth must match");
    assert(out_jvms->loc_size()   == in_jvms->loc_size(),   "size must match");
    assert(out_jvms->mon_size()   == in_jvms->mon_size(),   "size must match");
    assert(out_jvms->scl_size()   == in_jvms->scl_size(),   "size must match");
    assert(out_jvms->debug_size() == in_jvms->debug_size(), "size must match");

    // Update the two tail pointers in parallel.
    out_jvms = out_jvms->caller();
    in_jvms  = in_jvms->caller();
  }

  assert(debug_ptr == non_debug_edges, "debug info must fit exactly");

  // Test the correctness of JVMState::debug_xxx accessors:
  assert(call->jvms()->debug_start() == non_debug_edges, "");
  assert(call->jvms()->debug_end()   == call->req(), "");
  assert(call->jvms()->debug_depth() == call->req() - non_debug_edges, "");
}

bool GraphKit::compute_stack_effects(int& inputs, int& depth) {
  Bytecodes::Code code = java_bc();
  if (code == Bytecodes::_wide) {
    code = method()->java_code_at_bci(bci() + 1);
  }

  BasicType rtype = T_ILLEGAL;
  int       rsize = 0;

  if (code != Bytecodes::_illegal) {
    depth = Bytecodes::depth(code); // checkcast=0, athrow=-1
    rtype = Bytecodes::result_type(code); // checkcast=P, athrow=V
    if (rtype < T_CONFLICT)
      rsize = type2size[rtype];
  }

  switch (code) {
  case Bytecodes::_illegal:
    return false;

  case Bytecodes::_ldc:
  case Bytecodes::_ldc_w:
  case Bytecodes::_ldc2_w:
    inputs = 0;
    break;

  case Bytecodes::_dup:         inputs = 1;  break;
  case Bytecodes::_dup_x1:      inputs = 2;  break;
  case Bytecodes::_dup_x2:      inputs = 3;  break;
  case Bytecodes::_dup2:        inputs = 2;  break;
  case Bytecodes::_dup2_x1:     inputs = 3;  break;
  case Bytecodes::_dup2_x2:     inputs = 4;  break;
  case Bytecodes::_swap:        inputs = 2;  break;
  case Bytecodes::_arraylength: inputs = 1;  break;

  case Bytecodes::_getstatic:
  case Bytecodes::_putstatic:
  case Bytecodes::_getfield:
  case Bytecodes::_putfield:
    {
      bool ignored_will_link;
      ciField* field = method()->get_field_at_bci(bci(), ignored_will_link);
      int      size  = field->type()->size();
      bool is_get = (depth >= 0), is_static = (depth & 1);
      inputs = (is_static ? 0 : 1);
      if (is_get) {
        depth = size - inputs;
      } else {
        inputs += size;        // putxxx pops the value from the stack
        depth = - inputs;
      }
    }
    break;

  case Bytecodes::_invokevirtual:
  case Bytecodes::_invokespecial:
  case Bytecodes::_invokestatic:
  case Bytecodes::_invokedynamic:
  case Bytecodes::_invokeinterface:
    {
      bool ignored_will_link;
      ciSignature* declared_signature = NULL;
      ciMethod* ignored_callee = method()->get_method_at_bci(bci(), ignored_will_link, &declared_signature);
      assert(declared_signature != NULL, "cannot be null");
      inputs   = declared_signature->arg_size_for_bc(code);
      int size = declared_signature->return_type()->size();
      depth = size - inputs;
    }
    break;

  case Bytecodes::_multianewarray:
    {
      ciBytecodeStream iter(method());
      iter.reset_to_bci(bci());
      iter.next();
      inputs = iter.get_dimensions();
      assert(rsize == 1, "");
      depth = rsize - inputs;
    }
    break;

  case Bytecodes::_ireturn:
  case Bytecodes::_lreturn:
  case Bytecodes::_freturn:
  case Bytecodes::_dreturn:
  case Bytecodes::_areturn:
    assert(rsize == -depth, "");
    inputs = rsize;
    break;

  case Bytecodes::_jsr:
  case Bytecodes::_jsr_w:
    inputs = 0;
    depth  = 1;                  // S.B. depth=1, not zero
    break;

  default:
    // bytecode produces a typed result
    inputs = rsize - depth;
    assert(inputs >= 0, "");
    break;
  }

#ifdef ASSERT
  // spot check
  int outputs = depth + inputs;
  assert(outputs >= 0, "sanity");
  switch (code) {
  case Bytecodes::_checkcast: assert(inputs == 1 && outputs == 1, ""); break;
  case Bytecodes::_athrow:    assert(inputs == 1 && outputs == 0, ""); break;
  case Bytecodes::_aload_0:   assert(inputs == 0 && outputs == 1, ""); break;
  case Bytecodes::_return:    assert(inputs == 0 && outputs == 0, ""); break;
  case Bytecodes::_drem:      assert(inputs == 4 && outputs == 2, ""); break;
  default:                    break;
  }
#endif //ASSERT

  return true;
}



//------------------------------basic_plus_adr---------------------------------
Node* GraphKit::basic_plus_adr(Node* base, Node* ptr, Node* offset) {
  // short-circuit a common case
  if (offset == intcon(0))  return ptr;
  return _gvn.transform( new AddPNode(base, ptr, offset) );
}

Node* GraphKit::ConvI2L(Node* offset) {
  // short-circuit a common case
  jint offset_con = find_int_con(offset, Type::OffsetBot);
  if (offset_con != Type::OffsetBot) {
    return longcon((jlong) offset_con);
  }
  return _gvn.transform( new ConvI2LNode(offset));
}

Node* GraphKit::ConvI2UL(Node* offset) {
  juint offset_con = (juint) find_int_con(offset, Type::OffsetBot);
  if (offset_con != (juint) Type::OffsetBot) {
    return longcon((julong) offset_con);
  }
  Node* conv = _gvn.transform( new ConvI2LNode(offset));
  Node* mask = _gvn.transform(ConLNode::make((julong) max_juint));
  return _gvn.transform( new AndLNode(conv, mask) );
}

Node* GraphKit::ConvL2I(Node* offset) {
  // short-circuit a common case
  jlong offset_con = find_long_con(offset, (jlong)Type::OffsetBot);
  if (offset_con != (jlong)Type::OffsetBot) {
    return intcon((int) offset_con);
  }
  return _gvn.transform( new ConvL2INode(offset));
}

//-------------------------load_object_klass-----------------------------------
Node* GraphKit::load_object_klass(Node* obj) {
  // Special-case a fresh allocation to avoid building nodes:
  Node* akls = AllocateNode::Ideal_klass(obj, &_gvn);
  if (akls != NULL)  return akls;
  Node* k_adr = basic_plus_adr(obj, oopDesc::klass_offset_in_bytes());
  return _gvn.transform(LoadKlassNode::make(_gvn, NULL, immutable_memory(), k_adr, TypeInstPtr::KLASS));
}

//-------------------------load_array_length-----------------------------------
Node* GraphKit::load_array_length(Node* array) {
  // Special-case a fresh allocation to avoid building nodes:
  AllocateArrayNode* alloc = AllocateArrayNode::Ideal_array_allocation(array, &_gvn);
  Node *alen;
  if (alloc == NULL) {
    Node *r_adr = basic_plus_adr(array, arrayOopDesc::length_offset_in_bytes());
    alen = _gvn.transform( new LoadRangeNode(0, immutable_memory(), r_adr, TypeInt::POS));
  } else {
    alen = array_ideal_length(alloc, _gvn.type(array)->is_oopptr(), false);
  }
  return alen;
}

Node* GraphKit::array_ideal_length(AllocateArrayNode* alloc,
                                   const TypeOopPtr* oop_type,
                                   bool replace_length_in_map) {
  Node* length = alloc->Ideal_length();
  if (replace_length_in_map == false || map()->find_edge(length) >= 0) {
    Node* ccast = alloc->make_ideal_length(oop_type, &_gvn);
    if (ccast != length) {
      // do not transfrom ccast here, it might convert to top node for
      // negative array length and break assumptions in parsing stage.
      _gvn.set_type_bottom(ccast);
      record_for_igvn(ccast);
      if (replace_length_in_map) {
        replace_in_map(length, ccast);
      }
      return ccast;
    }
  }
  return length;
}

//------------------------------do_null_check----------------------------------
// Helper function to do a NULL pointer check.  Returned value is
// the incoming address with NULL casted away.  You are allowed to use the
// not-null value only if you are control dependent on the test.
#ifndef PRODUCT
extern int explicit_null_checks_inserted,
           explicit_null_checks_elided;
#endif
Node* GraphKit::null_check_common(Node* value, BasicType type,
                                  // optional arguments for variations:
                                  bool assert_null,
                                  Node* *null_control,
                                  bool speculative) {
  assert(!assert_null || null_control == NULL, "not both at once");
  if (stopped())  return top();
  NOT_PRODUCT(explicit_null_checks_inserted++);

  // Construct NULL check
  Node *chk = NULL;
  switch(type) {
    case T_LONG   : chk = new CmpLNode(value, _gvn.zerocon(T_LONG)); break;
    case T_INT    : chk = new CmpINode(value, _gvn.intcon(0)); break;
    case T_ARRAY  : // fall through
      type = T_OBJECT;  // simplify further tests
    case T_OBJECT : {
      const Type *t = _gvn.type( value );

      const TypeOopPtr* tp = t->isa_oopptr();
      if (tp != NULL && tp->klass() != NULL && !tp->klass()->is_loaded()
          // Only for do_null_check, not any of its siblings:
          && !assert_null && null_control == NULL) {
        // Usually, any field access or invocation on an unloaded oop type
        // will simply fail to link, since the statically linked class is
        // likely also to be unloaded.  However, in -Xcomp mode, sometimes
        // the static class is loaded but the sharper oop type is not.
        // Rather than checking for this obscure case in lots of places,
        // we simply observe that a null check on an unloaded class
        // will always be followed by a nonsense operation, so we
        // can just issue the uncommon trap here.
        // Our access to the unloaded class will only be correct
        // after it has been loaded and initialized, which requires
        // a trip through the interpreter.
#ifndef PRODUCT
        if (WizardMode) { tty->print("Null check of unloaded "); tp->klass()->print(); tty->cr(); }
#endif
        uncommon_trap(Deoptimization::Reason_unloaded,
                      Deoptimization::Action_reinterpret,
                      tp->klass(), "!loaded");
        return top();
      }

      if (assert_null) {
        // See if the type is contained in NULL_PTR.
        // If so, then the value is already null.
        if (t->higher_equal(TypePtr::NULL_PTR)) {
          NOT_PRODUCT(explicit_null_checks_elided++);
          return value;           // Elided null assert quickly!
        }
      } else {
        // See if mixing in the NULL pointer changes type.
        // If so, then the NULL pointer was not allowed in the original
        // type.  In other words, "value" was not-null.
        if (t->meet(TypePtr::NULL_PTR) != t->remove_speculative()) {
          // same as: if (!TypePtr::NULL_PTR->higher_equal(t)) ...
          NOT_PRODUCT(explicit_null_checks_elided++);
          return value;           // Elided null check quickly!
        }
      }
      chk = new CmpPNode( value, null() );
      break;
    }

    default:
      fatal("unexpected type: %s", type2name(type));
  }
  assert(chk != NULL, "sanity check");
  chk = _gvn.transform(chk);

  BoolTest::mask btest = assert_null ? BoolTest::eq : BoolTest::ne;
  BoolNode *btst = new BoolNode( chk, btest);
  Node   *tst = _gvn.transform( btst );

  //-----------
  // if peephole optimizations occurred, a prior test existed.
  // If a prior test existed, maybe it dominates as we can avoid this test.
  if (tst != btst && type == T_OBJECT) {
    // At this point we want to scan up the CFG to see if we can
    // find an identical test (and so avoid this test altogether).
    Node *cfg = control();
    int depth = 0;
    while( depth < 16 ) {       // Limit search depth for speed
      if( cfg->Opcode() == Op_IfTrue &&
          cfg->in(0)->in(1) == tst ) {
        // Found prior test.  Use "cast_not_null" to construct an identical
        // CastPP (and hence hash to) as already exists for the prior test.
        // Return that casted value.
        if (assert_null) {
          replace_in_map(value, null());
          return null();  // do not issue the redundant test
        }
        Node *oldcontrol = control();
        set_control(cfg);
        Node *res = cast_not_null(value);
        set_control(oldcontrol);
        NOT_PRODUCT(explicit_null_checks_elided++);
        return res;
      }
      cfg = IfNode::up_one_dom(cfg, /*linear_only=*/ true);
      if (cfg == NULL)  break;  // Quit at region nodes
      depth++;
    }
  }

  //-----------
  // Branch to failure if null
  float ok_prob = PROB_MAX;  // a priori estimate:  nulls never happen
  Deoptimization::DeoptReason reason;
  if (assert_null) {
    reason = Deoptimization::reason_null_assert(speculative);
  } else if (type == T_OBJECT) {
    reason = Deoptimization::reason_null_check(speculative);
  } else {
    reason = Deoptimization::Reason_div0_check;
  }
  // %%% Since Reason_unhandled is not recorded on a per-bytecode basis,
  // ciMethodData::has_trap_at will return a conservative -1 if any
  // must-be-null assertion has failed.  This could cause performance
  // problems for a method after its first do_null_assert failure.
  // Consider using 'Reason_class_check' instead?

  // To cause an implicit null check, we set the not-null probability
  // to the maximum (PROB_MAX).  For an explicit check the probability
  // is set to a smaller value.
  if (null_control != NULL || too_many_traps(reason)) {
    // probability is less likely
    ok_prob =  PROB_LIKELY_MAG(3);
  } else if (!assert_null &&
             (ImplicitNullCheckThreshold > 0) &&
             method() != NULL &&
             (method()->method_data()->trap_count(reason)
              >= (uint)ImplicitNullCheckThreshold)) {
    ok_prob =  PROB_LIKELY_MAG(3);
  }

  if (null_control != NULL) {
    IfNode* iff = create_and_map_if(control(), tst, ok_prob, COUNT_UNKNOWN);
    Node* null_true = _gvn.transform( new IfFalseNode(iff));
    set_control(      _gvn.transform( new IfTrueNode(iff)));
#ifndef PRODUCT
    if (null_true == top()) {
      explicit_null_checks_elided++;
    }
#endif
    (*null_control) = null_true;
  } else {
    BuildCutout unless(this, tst, ok_prob);
    // Check for optimizer eliding test at parse time
    if (stopped()) {
      // Failure not possible; do not bother making uncommon trap.
      NOT_PRODUCT(explicit_null_checks_elided++);
    } else if (assert_null) {
      uncommon_trap(reason,
                    Deoptimization::Action_make_not_entrant,
                    NULL, "assert_null");
    } else {
      replace_in_map(value, zerocon(type));
      builtin_throw(reason);
    }
  }

  // Must throw exception, fall-thru not possible?
  if (stopped()) {
    return top();               // No result
  }

  if (assert_null) {
    // Cast obj to null on this path.
    replace_in_map(value, zerocon(type));
    return zerocon(type);
  }

  // Cast obj to not-null on this path, if there is no null_control.
  // (If there is a null_control, a non-null value may come back to haunt us.)
  if (type == T_OBJECT) {
    Node* cast = cast_not_null(value, false);
    if (null_control == NULL || (*null_control) == top())
      replace_in_map(value, cast);
    value = cast;
  }

  return value;
}


//------------------------------cast_not_null----------------------------------
// Cast obj to not-null on this path
Node* GraphKit::cast_not_null(Node* obj, bool do_replace_in_map) {
  const Type *t = _gvn.type(obj);
  const Type *t_not_null = t->join_speculative(TypePtr::NOTNULL);
  // Object is already not-null?
  if( t == t_not_null ) return obj;

  Node *cast = new CastPPNode(obj,t_not_null);
  cast->init_req(0, control());
  cast = _gvn.transform( cast );

  // Scan for instances of 'obj' in the current JVM mapping.
  // These instances are known to be not-null after the test.
  if (do_replace_in_map)
    replace_in_map(obj, cast);

  return cast;                  // Return casted value
}

// Sometimes in intrinsics, we implicitly know an object is not null
// (there's no actual null check) so we can cast it to not null. In
// the course of optimizations, the input to the cast can become null.
// In that case that data path will die and we need the control path
// to become dead as well to keep the graph consistent. So we have to
// add a check for null for which one branch can't be taken. It uses
// an Opaque4 node that will cause the check to be removed after loop
// opts so the test goes away and the compiled code doesn't execute a
// useless check.
Node* GraphKit::must_be_not_null(Node* value, bool do_replace_in_map) {
  if (!TypePtr::NULL_PTR->higher_equal(_gvn.type(value))) {
    return value;
  }
  Node* chk = _gvn.transform(new CmpPNode(value, null()));
  Node *tst = _gvn.transform(new BoolNode(chk, BoolTest::ne));
  Node* opaq = _gvn.transform(new Opaque4Node(C, tst, intcon(1)));
  IfNode *iff = new IfNode(control(), opaq, PROB_MAX, COUNT_UNKNOWN);
  _gvn.set_type(iff, iff->Value(&_gvn));
  Node *if_f = _gvn.transform(new IfFalseNode(iff));
  Node *frame = _gvn.transform(new ParmNode(C->start(), TypeFunc::FramePtr));
  Node* halt = _gvn.transform(new HaltNode(if_f, frame, "unexpected null in intrinsic"));
  C->root()->add_req(halt);
  Node *if_t = _gvn.transform(new IfTrueNode(iff));
  set_control(if_t);
  return cast_not_null(value, do_replace_in_map);
}


//--------------------------replace_in_map-------------------------------------
void GraphKit::replace_in_map(Node* old, Node* neww) {
  if (old == neww) {
    return;
  }

  map()->replace_edge(old, neww);

  // Note: This operation potentially replaces any edge
  // on the map.  This includes locals, stack, and monitors
  // of the current (innermost) JVM state.

  // don't let inconsistent types from profiling escape this
  // method

  const Type* told = _gvn.type(old);
  const Type* tnew = _gvn.type(neww);

  if (!tnew->higher_equal(told)) {
    return;
  }

  map()->record_replaced_node(old, neww);
}


//=============================================================================
//--------------------------------memory---------------------------------------
Node* GraphKit::memory(uint alias_idx) {
  MergeMemNode* mem = merged_memory();
  Node* p = mem->memory_at(alias_idx);
  assert(p != mem->empty_memory(), "empty");
  _gvn.set_type(p, Type::MEMORY);  // must be mapped
  return p;
}

//-----------------------------reset_memory------------------------------------
Node* GraphKit::reset_memory() {
  Node* mem = map()->memory();
  // do not use this node for any more parsing!
  debug_only( map()->set_memory((Node*)NULL) );
  return _gvn.transform( mem );
}

//------------------------------set_all_memory---------------------------------
void GraphKit::set_all_memory(Node* newmem) {
  Node* mergemem = MergeMemNode::make(newmem);
  gvn().set_type_bottom(mergemem);
  map()->set_memory(mergemem);
}

//------------------------------set_all_memory_call----------------------------
void GraphKit::set_all_memory_call(Node* call, bool separate_io_proj) {
  Node* newmem = _gvn.transform( new ProjNode(call, TypeFunc::Memory, separate_io_proj) );
  set_all_memory(newmem);
}

//=============================================================================
//
// parser factory methods for MemNodes
//
// These are layered on top of the factory methods in LoadNode and StoreNode,
// and integrate with the parser's memory state and _gvn engine.
//

// factory methods in "int adr_idx"
Node* GraphKit::make_load(Node* ctl, Node* adr, const Type* t, BasicType bt,
                          int adr_idx,
                          MemNode::MemOrd mo,
                          LoadNode::ControlDependency control_dependency,
                          bool require_atomic_access,
                          bool unaligned,
                          bool mismatched,
                          bool unsafe,
                          uint8_t barrier_data) {
  assert(adr_idx != Compile::AliasIdxTop, "use other make_load factory" );
  const TypePtr* adr_type = NULL; // debug-mode-only argument
  debug_only(adr_type = C->get_adr_type(adr_idx));
  Node* mem = memory(adr_idx);
  Node* ld;
  if (require_atomic_access && bt == T_LONG) {
    ld = LoadLNode::make_atomic(ctl, mem, adr, adr_type, t, mo, control_dependency, unaligned, mismatched, unsafe, barrier_data);
  } else if (require_atomic_access && bt == T_DOUBLE) {
    ld = LoadDNode::make_atomic(ctl, mem, adr, adr_type, t, mo, control_dependency, unaligned, mismatched, unsafe, barrier_data);
  } else {
    ld = LoadNode::make(_gvn, ctl, mem, adr, adr_type, t, bt, mo, control_dependency, unaligned, mismatched, unsafe, barrier_data);
  }
  ld = _gvn.transform(ld);
  if (((bt == T_OBJECT) && C->do_escape_analysis()) || C->eliminate_boxing()) {
    // Improve graph before escape analysis and boxing elimination.
    record_for_igvn(ld);
  }
  return ld;
}

Node* GraphKit::store_to_memory(Node* ctl, Node* adr, Node *val, BasicType bt,
                                int adr_idx,
                                MemNode::MemOrd mo,
                                bool require_atomic_access,
                                bool unaligned,
                                bool mismatched,
                                bool unsafe) {
  assert(adr_idx != Compile::AliasIdxTop, "use other store_to_memory factory" );
  const TypePtr* adr_type = NULL;
  debug_only(adr_type = C->get_adr_type(adr_idx));
  Node *mem = memory(adr_idx);
  Node* st;
  if (require_atomic_access && bt == T_LONG) {
    st = StoreLNode::make_atomic(ctl, mem, adr, adr_type, val, mo);
  } else if (require_atomic_access && bt == T_DOUBLE) {
    st = StoreDNode::make_atomic(ctl, mem, adr, adr_type, val, mo);
  } else {
    st = StoreNode::make(_gvn, ctl, mem, adr, adr_type, val, bt, mo);
  }
  if (unaligned) {
    st->as_Store()->set_unaligned_access();
  }
  if (mismatched) {
    st->as_Store()->set_mismatched_access();
  }
  if (unsafe) {
    st->as_Store()->set_unsafe_access();
  }
  st = _gvn.transform(st);
  set_memory(st, adr_idx);
  // Back-to-back stores can only remove intermediate store with DU info
  // so push on worklist for optimizer.
  if (mem->req() > MemNode::Address && adr == mem->in(MemNode::Address))
    record_for_igvn(st);

  return st;
}

Node* GraphKit::access_store_at(Node* obj,
                                Node* adr,
                                const TypePtr* adr_type,
                                Node* val,
                                const Type* val_type,
                                BasicType bt,
                                DecoratorSet decorators) {
  // Transformation of a value which could be NULL pointer (CastPP #NULL)
  // could be delayed during Parse (for example, in adjust_map_after_if()).
  // Execute transformation here to avoid barrier generation in such case.
  if (_gvn.type(val) == TypePtr::NULL_PTR) {
    val = _gvn.makecon(TypePtr::NULL_PTR);
  }

  if (stopped()) {
    return top(); // Dead path ?
  }

  assert(val != NULL, "not dead path");

  C2AccessValuePtr addr(adr, adr_type);
  C2AccessValue value(val, val_type);
  C2ParseAccess access(this, decorators | C2_WRITE_ACCESS, bt, obj, addr);
  if (access.is_raw()) {
    return _barrier_set->BarrierSetC2::store_at(access, value);
  } else {
    return _barrier_set->store_at(access, value);
  }
}

Node* GraphKit::access_load_at(Node* obj,   // containing obj
                               Node* adr,   // actual adress to store val at
                               const TypePtr* adr_type,
                               const Type* val_type,
                               BasicType bt,
                               DecoratorSet decorators) {
  if (stopped()) {
    return top(); // Dead path ?
  }

  C2AccessValuePtr addr(adr, adr_type);
  C2ParseAccess access(this, decorators | C2_READ_ACCESS, bt, obj, addr);
  if (access.is_raw()) {
    return _barrier_set->BarrierSetC2::load_at(access, val_type);
  } else {
    return _barrier_set->load_at(access, val_type);
  }
}

Node* GraphKit::access_load(Node* adr,   // actual adress to load val at
                            const Type* val_type,
                            BasicType bt,
                            DecoratorSet decorators) {
  if (stopped()) {
    return top(); // Dead path ?
  }

  C2AccessValuePtr addr(adr, adr->bottom_type()->is_ptr());
  C2ParseAccess access(this, decorators | C2_READ_ACCESS, bt, NULL, addr);
  if (access.is_raw()) {
    return _barrier_set->BarrierSetC2::load_at(access, val_type);
  } else {
    return _barrier_set->load_at(access, val_type);
  }
}

Node* GraphKit::access_atomic_cmpxchg_val_at(Node* obj,
                                             Node* adr,
                                             const TypePtr* adr_type,
                                             int alias_idx,
                                             Node* expected_val,
                                             Node* new_val,
                                             const Type* value_type,
                                             BasicType bt,
                                             DecoratorSet decorators) {
  C2AccessValuePtr addr(adr, adr_type);
  C2AtomicParseAccess access(this, decorators | C2_READ_ACCESS | C2_WRITE_ACCESS,
                        bt, obj, addr, alias_idx);
  if (access.is_raw()) {
    return _barrier_set->BarrierSetC2::atomic_cmpxchg_val_at(access, expected_val, new_val, value_type);
  } else {
    return _barrier_set->atomic_cmpxchg_val_at(access, expected_val, new_val, value_type);
  }
}

Node* GraphKit::access_atomic_cmpxchg_bool_at(Node* obj,
                                              Node* adr,
                                              const TypePtr* adr_type,
                                              int alias_idx,
                                              Node* expected_val,
                                              Node* new_val,
                                              const Type* value_type,
                                              BasicType bt,
                                              DecoratorSet decorators) {
  C2AccessValuePtr addr(adr, adr_type);
  C2AtomicParseAccess access(this, decorators | C2_READ_ACCESS | C2_WRITE_ACCESS,
                        bt, obj, addr, alias_idx);
  if (access.is_raw()) {
    return _barrier_set->BarrierSetC2::atomic_cmpxchg_bool_at(access, expected_val, new_val, value_type);
  } else {
    return _barrier_set->atomic_cmpxchg_bool_at(access, expected_val, new_val, value_type);
  }
}

Node* GraphKit::access_atomic_xchg_at(Node* obj,
                                      Node* adr,
                                      const TypePtr* adr_type,
                                      int alias_idx,
                                      Node* new_val,
                                      const Type* value_type,
                                      BasicType bt,
                                      DecoratorSet decorators) {
  C2AccessValuePtr addr(adr, adr_type);
  C2AtomicParseAccess access(this, decorators | C2_READ_ACCESS | C2_WRITE_ACCESS,
                        bt, obj, addr, alias_idx);
  if (access.is_raw()) {
    return _barrier_set->BarrierSetC2::atomic_xchg_at(access, new_val, value_type);
  } else {
    return _barrier_set->atomic_xchg_at(access, new_val, value_type);
  }
}

Node* GraphKit::access_atomic_add_at(Node* obj,
                                     Node* adr,
                                     const TypePtr* adr_type,
                                     int alias_idx,
                                     Node* new_val,
                                     const Type* value_type,
                                     BasicType bt,
                                     DecoratorSet decorators) {
  C2AccessValuePtr addr(adr, adr_type);
  C2AtomicParseAccess access(this, decorators | C2_READ_ACCESS | C2_WRITE_ACCESS, bt, obj, addr, alias_idx);
  if (access.is_raw()) {
    return _barrier_set->BarrierSetC2::atomic_add_at(access, new_val, value_type);
  } else {
    return _barrier_set->atomic_add_at(access, new_val, value_type);
  }
}

void GraphKit::access_clone(Node* src, Node* dst, Node* size, bool is_array) {
  return _barrier_set->clone(this, src, dst, size, is_array);
}

//-------------------------array_element_address-------------------------
Node* GraphKit::array_element_address(Node* ary, Node* idx, BasicType elembt,
                                      const TypeInt* sizetype, Node* ctrl) {
  uint shift  = exact_log2(type2aelembytes(elembt));
  uint header = arrayOopDesc::base_offset_in_bytes(elembt);

  // short-circuit a common case (saves lots of confusing waste motion)
  jint idx_con = find_int_con(idx, -1);
  if (idx_con >= 0) {
    intptr_t offset = header + ((intptr_t)idx_con << shift);
    return basic_plus_adr(ary, offset);
  }

  // must be correct type for alignment purposes
  Node* base  = basic_plus_adr(ary, header);
  idx = Compile::conv_I2X_index(&_gvn, idx, sizetype, ctrl);
  Node* scale = _gvn.transform( new LShiftXNode(idx, intcon(shift)) );
  return basic_plus_adr(ary, base, scale);
}

//-------------------------load_array_element-------------------------
Node* GraphKit::load_array_element(Node* ctl, Node* ary, Node* idx, const TypeAryPtr* arytype) {
  const Type* elemtype = arytype->elem();
  BasicType elembt = elemtype->array_element_basic_type();
  Node* adr = array_element_address(ary, idx, elembt, arytype->size());
  if (elembt == T_NARROWOOP) {
    elembt = T_OBJECT; // To satisfy switch in LoadNode::make()
  }
  Node* ld = make_load(ctl, adr, elemtype, elembt, arytype, MemNode::unordered);
  return ld;
}

//-------------------------set_arguments_for_java_call-------------------------
// Arguments (pre-popped from the stack) are taken from the JVMS.
void GraphKit::set_arguments_for_java_call(CallJavaNode* call) {
  // Add the call arguments:
  uint nargs = call->method()->arg_size();
  for (uint i = 0; i < nargs; i++) {
    Node* arg = argument(i);
    call->init_req(i + TypeFunc::Parms, arg);
  }
}

//---------------------------set_edges_for_java_call---------------------------
// Connect a newly created call into the current JVMS.
// A return value node (if any) is returned from set_edges_for_java_call.
void GraphKit::set_edges_for_java_call(CallJavaNode* call, bool must_throw, bool separate_io_proj) {

  // Add the predefined inputs:
  call->init_req( TypeFunc::Control, control() );
  call->init_req( TypeFunc::I_O    , i_o() );
  call->init_req( TypeFunc::Memory , reset_memory() );
  call->init_req( TypeFunc::FramePtr, frameptr() );
  call->init_req( TypeFunc::ReturnAdr, top() );

  add_safepoint_edges(call, must_throw);

  Node* xcall = _gvn.transform(call);

  if (xcall == top()) {
    set_control(top());
    return;
  }
  assert(xcall == call, "call identity is stable");

  // Re-use the current map to produce the result.

  set_control(_gvn.transform(new ProjNode(call, TypeFunc::Control)));
  set_i_o(    _gvn.transform(new ProjNode(call, TypeFunc::I_O    , separate_io_proj)));
  set_all_memory_call(xcall, separate_io_proj);

  //return xcall;   // no need, caller already has it
}

Node* GraphKit::set_results_for_java_call(CallJavaNode* call, bool separate_io_proj, bool deoptimize) {
  if (stopped())  return top();  // maybe the call folded up?

  // Capture the return value, if any.
  Node* ret;
  if (call->method() == NULL ||
      call->method()->return_type()->basic_type() == T_VOID)
        ret = top();
  else  ret = _gvn.transform(new ProjNode(call, TypeFunc::Parms));

  // Note:  Since any out-of-line call can produce an exception,
  // we always insert an I_O projection from the call into the result.

  make_slow_call_ex(call, env()->Throwable_klass(), separate_io_proj, deoptimize);

  if (separate_io_proj) {
    // The caller requested separate projections be used by the fall
    // through and exceptional paths, so replace the projections for
    // the fall through path.
    set_i_o(_gvn.transform( new ProjNode(call, TypeFunc::I_O) ));
    set_all_memory(_gvn.transform( new ProjNode(call, TypeFunc::Memory) ));
  }
  return ret;
}

//--------------------set_predefined_input_for_runtime_call--------------------
// Reading and setting the memory state is way conservative here.
// The real problem is that I am not doing real Type analysis on memory,
// so I cannot distinguish card mark stores from other stores.  Across a GC
// point the Store Barrier and the card mark memory has to agree.  I cannot
// have a card mark store and its barrier split across the GC point from
// either above or below.  Here I get that to happen by reading ALL of memory.
// A better answer would be to separate out card marks from other memory.
// For now, return the input memory state, so that it can be reused
// after the call, if this call has restricted memory effects.
Node* GraphKit::set_predefined_input_for_runtime_call(SafePointNode* call, Node* narrow_mem) {
  // Set fixed predefined input arguments
  Node* memory = reset_memory();
  Node* m = narrow_mem == NULL ? memory : narrow_mem;
  call->init_req( TypeFunc::Control,   control()  );
  call->init_req( TypeFunc::I_O,       top()      ); // does no i/o
  call->init_req( TypeFunc::Memory,    m          ); // may gc ptrs
  call->init_req( TypeFunc::FramePtr,  frameptr() );
  call->init_req( TypeFunc::ReturnAdr, top()      );
  return memory;
}

//-------------------set_predefined_output_for_runtime_call--------------------
// Set control and memory (not i_o) from the call.
// If keep_mem is not NULL, use it for the output state,
// except for the RawPtr output of the call, if hook_mem is TypeRawPtr::BOTTOM.
// If hook_mem is NULL, this call produces no memory effects at all.
// If hook_mem is a Java-visible memory slice (such as arraycopy operands),
// then only that memory slice is taken from the call.
// In the last case, we must put an appropriate memory barrier before
// the call, so as to create the correct anti-dependencies on loads
// preceding the call.
void GraphKit::set_predefined_output_for_runtime_call(Node* call,
                                                      Node* keep_mem,
                                                      const TypePtr* hook_mem) {
  // no i/o
  set_control(_gvn.transform( new ProjNode(call,TypeFunc::Control) ));
  if (keep_mem) {
    // First clone the existing memory state
    set_all_memory(keep_mem);
    if (hook_mem != NULL) {
      // Make memory for the call
      Node* mem = _gvn.transform( new ProjNode(call, TypeFunc::Memory) );
      // Set the RawPtr memory state only.  This covers all the heap top/GC stuff
      // We also use hook_mem to extract specific effects from arraycopy stubs.
      set_memory(mem, hook_mem);
    }
    // ...else the call has NO memory effects.

    // Make sure the call advertises its memory effects precisely.
    // This lets us build accurate anti-dependences in gcm.cpp.
    assert(C->alias_type(call->adr_type()) == C->alias_type(hook_mem),
           "call node must be constructed correctly");
  } else {
    assert(hook_mem == NULL, "");
    // This is not a "slow path" call; all memory comes from the call.
    set_all_memory_call(call);
  }
}

// Keep track of MergeMems feeding into other MergeMems
static void add_mergemem_users_to_worklist(Unique_Node_List& wl, Node* mem) {
  if (!mem->is_MergeMem()) {
    return;
  }
  for (SimpleDUIterator i(mem); i.has_next(); i.next()) {
    Node* use = i.get();
    if (use->is_MergeMem()) {
      wl.push(use);
    }
  }
}

// Replace the call with the current state of the kit.
void GraphKit::replace_call(CallNode* call, Node* result, bool do_replaced_nodes) {
  JVMState* ejvms = NULL;
  if (has_exceptions()) {
    ejvms = transfer_exceptions_into_jvms();
  }

  ReplacedNodes replaced_nodes = map()->replaced_nodes();
  ReplacedNodes replaced_nodes_exception;
  Node* ex_ctl = top();

  SafePointNode* final_state = stop();

  // Find all the needed outputs of this call
  CallProjections callprojs;
  call->extract_projections(&callprojs, true);

  Unique_Node_List wl;
  Node* init_mem = call->in(TypeFunc::Memory);
  Node* final_mem = final_state->in(TypeFunc::Memory);
  Node* final_ctl = final_state->in(TypeFunc::Control);
  Node* final_io = final_state->in(TypeFunc::I_O);

  // Replace all the old call edges with the edges from the inlining result
  if (callprojs.fallthrough_catchproj != NULL) {
    C->gvn_replace_by(callprojs.fallthrough_catchproj, final_ctl);
  }
  if (callprojs.fallthrough_memproj != NULL) {
    if (final_mem->is_MergeMem()) {
      // Parser's exits MergeMem was not transformed but may be optimized
      final_mem = _gvn.transform(final_mem);
    }
    C->gvn_replace_by(callprojs.fallthrough_memproj,   final_mem);
    add_mergemem_users_to_worklist(wl, final_mem);
  }
  if (callprojs.fallthrough_ioproj != NULL) {
    C->gvn_replace_by(callprojs.fallthrough_ioproj,    final_io);
  }

  // Replace the result with the new result if it exists and is used
  if (callprojs.resproj != NULL && result != NULL) {
    C->gvn_replace_by(callprojs.resproj, result);
  }

  if (ejvms == NULL) {
    // No exception edges to simply kill off those paths
    if (callprojs.catchall_catchproj != NULL) {
      C->gvn_replace_by(callprojs.catchall_catchproj, C->top());
    }
    if (callprojs.catchall_memproj != NULL) {
      C->gvn_replace_by(callprojs.catchall_memproj,   C->top());
    }
    if (callprojs.catchall_ioproj != NULL) {
      C->gvn_replace_by(callprojs.catchall_ioproj,    C->top());
    }
    // Replace the old exception object with top
    if (callprojs.exobj != NULL) {
      C->gvn_replace_by(callprojs.exobj, C->top());
    }
  } else {
    GraphKit ekit(ejvms);

    // Load my combined exception state into the kit, with all phis transformed:
    SafePointNode* ex_map = ekit.combine_and_pop_all_exception_states();
    replaced_nodes_exception = ex_map->replaced_nodes();

    Node* ex_oop = ekit.use_exception_state(ex_map);

    if (callprojs.catchall_catchproj != NULL) {
      C->gvn_replace_by(callprojs.catchall_catchproj, ekit.control());
      ex_ctl = ekit.control();
    }
    if (callprojs.catchall_memproj != NULL) {
      Node* ex_mem = ekit.reset_memory();
      C->gvn_replace_by(callprojs.catchall_memproj,   ex_mem);
      add_mergemem_users_to_worklist(wl, ex_mem);
    }
    if (callprojs.catchall_ioproj != NULL) {
      C->gvn_replace_by(callprojs.catchall_ioproj,    ekit.i_o());
    }

    // Replace the old exception object with the newly created one
    if (callprojs.exobj != NULL) {
      C->gvn_replace_by(callprojs.exobj, ex_oop);
    }
  }

  // Disconnect the call from the graph
  call->disconnect_inputs(C);
  C->gvn_replace_by(call, C->top());

  // Clean up any MergeMems that feed other MergeMems since the
  // optimizer doesn't like that.
  while (wl.size() > 0) {
    _gvn.transform(wl.pop());
  }

  if (callprojs.fallthrough_catchproj != NULL && !final_ctl->is_top() && do_replaced_nodes) {
    replaced_nodes.apply(C, final_ctl);
  }
  if (!ex_ctl->is_top() && do_replaced_nodes) {
    replaced_nodes_exception.apply(C, ex_ctl);
  }
}


//------------------------------increment_counter------------------------------
// for statistics: increment a VM counter by 1

void GraphKit::increment_counter(address counter_addr) {
  Node* adr1 = makecon(TypeRawPtr::make(counter_addr));
  increment_counter(adr1);
}

void GraphKit::increment_counter(Node* counter_addr) {
  int adr_type = Compile::AliasIdxRaw;
  Node* ctrl = control();
  Node* cnt  = make_load(ctrl, counter_addr, TypeLong::LONG, T_LONG, adr_type, MemNode::unordered);
  Node* incr = _gvn.transform(new AddLNode(cnt, _gvn.longcon(1)));
  store_to_memory(ctrl, counter_addr, incr, T_LONG, adr_type, MemNode::unordered);
}


//------------------------------uncommon_trap----------------------------------
// Bail out to the interpreter in mid-method.  Implemented by calling the
// uncommon_trap blob.  This helper function inserts a runtime call with the
// right debug info.
void GraphKit::uncommon_trap(int trap_request,
                             ciKlass* klass, const char* comment,
                             bool must_throw,
                             bool keep_exact_action) {
  if (failing())  stop();
  if (stopped())  return; // trap reachable?

  // Note:  If ProfileTraps is true, and if a deopt. actually
  // occurs here, the runtime will make sure an MDO exists.  There is
  // no need to call method()->ensure_method_data() at this point.

  // Set the stack pointer to the right value for reexecution:
  set_sp(reexecute_sp());

#ifdef ASSERT
  if (!must_throw) {
    // Make sure the stack has at least enough depth to execute
    // the current bytecode.
    int inputs, ignored_depth;
    if (compute_stack_effects(inputs, ignored_depth)) {
      assert(sp() >= inputs, "must have enough JVMS stack to execute %s: sp=%d, inputs=%d",
             Bytecodes::name(java_bc()), sp(), inputs);
    }
  }
#endif

  Deoptimization::DeoptReason reason = Deoptimization::trap_request_reason(trap_request);
  Deoptimization::DeoptAction action = Deoptimization::trap_request_action(trap_request);

  switch (action) {
  case Deoptimization::Action_maybe_recompile:
  case Deoptimization::Action_reinterpret:
    // Temporary fix for 6529811 to allow virtual calls to be sure they
    // get the chance to go from mono->bi->mega
    if (!keep_exact_action &&
        Deoptimization::trap_request_index(trap_request) < 0 &&
        too_many_recompiles(reason)) {
      // This BCI is causing too many recompilations.
      if (C->log() != NULL) {
        C->log()->elem("observe that='trap_action_change' reason='%s' from='%s' to='none'",
                Deoptimization::trap_reason_name(reason),
                Deoptimization::trap_action_name(action));
      }
      action = Deoptimization::Action_none;
      trap_request = Deoptimization::make_trap_request(reason, action);
    } else {
      C->set_trap_can_recompile(true);
    }
    break;
  case Deoptimization::Action_make_not_entrant:
    C->set_trap_can_recompile(true);
    break;
  case Deoptimization::Action_none:
  case Deoptimization::Action_make_not_compilable:
    break;
  default:
#ifdef ASSERT
    fatal("unknown action %d: %s", action, Deoptimization::trap_action_name(action));
#endif
    break;
  }

  if (TraceOptoParse) {
    char buf[100];
    tty->print_cr("Uncommon trap %s at bci:%d",
                  Deoptimization::format_trap_request(buf, sizeof(buf),
                                                      trap_request), bci());
  }

  CompileLog* log = C->log();
  if (log != NULL) {
    int kid = (klass == NULL)? -1: log->identify(klass);
    log->begin_elem("uncommon_trap bci='%d'", bci());
    char buf[100];
    log->print(" %s", Deoptimization::format_trap_request(buf, sizeof(buf),
                                                          trap_request));
    if (kid >= 0)         log->print(" klass='%d'", kid);
    if (comment != NULL)  log->print(" comment='%s'", comment);
    log->end_elem();
  }

  // Make sure any guarding test views this path as very unlikely
  Node *i0 = control()->in(0);
  if (i0 != NULL && i0->is_If()) {        // Found a guarding if test?
    IfNode *iff = i0->as_If();
    float f = iff->_prob;   // Get prob
    if (control()->Opcode() == Op_IfTrue) {
      if (f > PROB_UNLIKELY_MAG(4))
        iff->_prob = PROB_MIN;
    } else {
      if (f < PROB_LIKELY_MAG(4))
        iff->_prob = PROB_MAX;
    }
  }

  // Clear out dead values from the debug info.
  kill_dead_locals();

  // Now insert the uncommon trap subroutine call
  address call_addr = SharedRuntime::uncommon_trap_blob()->entry_point();
  const TypePtr* no_memory_effects = NULL;
  // Pass the index of the class to be loaded
  Node* call = make_runtime_call(RC_NO_LEAF | RC_UNCOMMON |
                                 (must_throw ? RC_MUST_THROW : 0),
                                 OptoRuntime::uncommon_trap_Type(),
                                 call_addr, "uncommon_trap", no_memory_effects,
                                 intcon(trap_request));
  assert(call->as_CallStaticJava()->uncommon_trap_request() == trap_request,
         "must extract request correctly from the graph");
  assert(trap_request != 0, "zero value reserved by uncommon_trap_request");

  call->set_req(TypeFunc::ReturnAdr, returnadr());
  // The debug info is the only real input to this call.

  // Halt-and-catch fire here.  The above call should never return!
  HaltNode* halt = new HaltNode(control(), frameptr(), "uncommon trap returned which should never happen"
                                                       PRODUCT_ONLY(COMMA /*reachable*/false));
  _gvn.set_type_bottom(halt);
  root()->add_req(halt);

  stop_and_kill_map();
}


//--------------------------just_allocated_object------------------------------
// Report the object that was just allocated.
// It must be the case that there are no intervening safepoints.
// We use this to determine if an object is so "fresh" that
// it does not require card marks.
Node* GraphKit::just_allocated_object(Node* current_control) {
  Node* ctrl = current_control;
  // Object::<init> is invoked after allocation, most of invoke nodes
  // will be reduced, but a region node is kept in parse time, we check
  // the pattern and skip the region node if it degraded to a copy.
  if (ctrl != NULL && ctrl->is_Region() && ctrl->req() == 2 &&
      ctrl->as_Region()->is_copy()) {
    ctrl = ctrl->as_Region()->is_copy();
  }
  if (C->recent_alloc_ctl() == ctrl) {
   return C->recent_alloc_obj();
  }
  return NULL;
}


/**
 * Record profiling data exact_kls for Node n with the type system so
 * that it can propagate it (speculation)
 *
 * @param n          node that the type applies to
 * @param exact_kls  type from profiling
 * @param maybe_null did profiling see null?
 *
 * @return           node with improved type
 */
Node* GraphKit::record_profile_for_speculation(Node* n, ciKlass* exact_kls, ProfilePtrKind ptr_kind) {
  const Type* current_type = _gvn.type(n);
  assert(UseTypeSpeculation, "type speculation must be on");

  const TypePtr* speculative = current_type->speculative();

  // Should the klass from the profile be recorded in the speculative type?
  if (current_type->would_improve_type(exact_kls, jvms()->depth())) {
    const TypeKlassPtr* tklass = TypeKlassPtr::make(exact_kls);
    const TypeOopPtr* xtype = tklass->as_instance_type();
    assert(xtype->klass_is_exact(), "Should be exact");
    // Any reason to believe n is not null (from this profiling or a previous one)?
    assert(ptr_kind != ProfileAlwaysNull, "impossible here");
    const TypePtr* ptr = (ptr_kind == ProfileMaybeNull && current_type->speculative_maybe_null()) ? TypePtr::BOTTOM : TypePtr::NOTNULL;
    // record the new speculative type's depth
    speculative = xtype->cast_to_ptr_type(ptr->ptr())->is_ptr();
    speculative = speculative->with_inline_depth(jvms()->depth());
  } else if (current_type->would_improve_ptr(ptr_kind)) {
    // Profiling report that null was never seen so we can change the
    // speculative type to non null ptr.
    if (ptr_kind == ProfileAlwaysNull) {
      speculative = TypePtr::NULL_PTR;
    } else {
      assert(ptr_kind == ProfileNeverNull, "nothing else is an improvement");
      const TypePtr* ptr = TypePtr::NOTNULL;
      if (speculative != NULL) {
        speculative = speculative->cast_to_ptr_type(ptr->ptr())->is_ptr();
      } else {
        speculative = ptr;
      }
    }
  }

  if (speculative != current_type->speculative()) {
    // Build a type with a speculative type (what we think we know
    // about the type but will need a guard when we use it)
    const TypeOopPtr* spec_type = TypeOopPtr::make(TypePtr::BotPTR, Type::OffsetBot, TypeOopPtr::InstanceBot, speculative);
    // We're changing the type, we need a new CheckCast node to carry
    // the new type. The new type depends on the control: what
    // profiling tells us is only valid from here as far as we can
    // tell.
    Node* cast = new CheckCastPPNode(control(), n, current_type->remove_speculative()->join_speculative(spec_type));
    cast = _gvn.transform(cast);
    replace_in_map(n, cast);
    n = cast;
  }

  return n;
}

/**
 * Record profiling data from receiver profiling at an invoke with the
 * type system so that it can propagate it (speculation)
 *
 * @param n  receiver node
 *
 * @return   node with improved type
 */
Node* GraphKit::record_profiled_receiver_for_speculation(Node* n) {
  if (!UseTypeSpeculation) {
    return n;
  }
  ciKlass* exact_kls = profile_has_unique_klass();
  ProfilePtrKind ptr_kind = ProfileMaybeNull;
  if ((java_bc() == Bytecodes::_checkcast ||
       java_bc() == Bytecodes::_instanceof ||
       java_bc() == Bytecodes::_aastore) &&
      method()->method_data()->is_mature()) {
    ciProfileData* data = method()->method_data()->bci_to_data(bci());
    if (data != NULL) {
      if (!data->as_BitData()->null_seen()) {
        ptr_kind = ProfileNeverNull;
      } else {
        assert(data->is_ReceiverTypeData(), "bad profile data type");
        ciReceiverTypeData* call = (ciReceiverTypeData*)data->as_ReceiverTypeData();
        uint i = 0;
        for (; i < call->row_limit(); i++) {
          ciKlass* receiver = call->receiver(i);
          if (receiver != NULL) {
            break;
          }
        }
        ptr_kind = (i == call->row_limit()) ? ProfileAlwaysNull : ProfileMaybeNull;
      }
    }
  }
  return record_profile_for_speculation(n, exact_kls, ptr_kind);
}

/**
 * Record profiling data from argument profiling at an invoke with the
 * type system so that it can propagate it (speculation)
 *
 * @param dest_method  target method for the call
 * @param bc           what invoke bytecode is this?
 */
void GraphKit::record_profiled_arguments_for_speculation(ciMethod* dest_method, Bytecodes::Code bc) {
  if (!UseTypeSpeculation) {
    return;
  }
  const TypeFunc* tf    = TypeFunc::make(dest_method);
  int             nargs = tf->domain()->cnt() - TypeFunc::Parms;
  int skip = Bytecodes::has_receiver(bc) ? 1 : 0;
  for (int j = skip, i = 0; j < nargs && i < TypeProfileArgsLimit; j++) {
    const Type *targ = tf->domain()->field_at(j + TypeFunc::Parms);
    if (is_reference_type(targ->basic_type())) {
      ProfilePtrKind ptr_kind = ProfileMaybeNull;
      ciKlass* better_type = NULL;
      if (method()->argument_profiled_type(bci(), i, better_type, ptr_kind)) {
        record_profile_for_speculation(argument(j), better_type, ptr_kind);
      }
      i++;
    }
  }
}

/**
 * Record profiling data from parameter profiling at an invoke with
 * the type system so that it can propagate it (speculation)
 */
void GraphKit::record_profiled_parameters_for_speculation() {
  if (!UseTypeSpeculation) {
    return;
  }
  for (int i = 0, j = 0; i < method()->arg_size() ; i++) {
    if (_gvn.type(local(i))->isa_oopptr()) {
      ProfilePtrKind ptr_kind = ProfileMaybeNull;
      ciKlass* better_type = NULL;
      if (method()->parameter_profiled_type(j, better_type, ptr_kind)) {
        record_profile_for_speculation(local(i), better_type, ptr_kind);
      }
      j++;
    }
  }
}

/**
 * Record profiling data from return value profiling at an invoke with
 * the type system so that it can propagate it (speculation)
 */
void GraphKit::record_profiled_return_for_speculation() {
  if (!UseTypeSpeculation) {
    return;
  }
  ProfilePtrKind ptr_kind = ProfileMaybeNull;
  ciKlass* better_type = NULL;
  if (method()->return_profiled_type(bci(), better_type, ptr_kind)) {
    // If profiling reports a single type for the return value,
    // feed it to the type system so it can propagate it as a
    // speculative type
    record_profile_for_speculation(stack(sp()-1), better_type, ptr_kind);
  }
}

void GraphKit::round_double_arguments(ciMethod* dest_method) {
  if (Matcher::strict_fp_requires_explicit_rounding) {
    // (Note:  TypeFunc::make has a cache that makes this fast.)
    const TypeFunc* tf    = TypeFunc::make(dest_method);
    int             nargs = tf->domain()->cnt() - TypeFunc::Parms;
    for (int j = 0; j < nargs; j++) {
      const Type *targ = tf->domain()->field_at(j + TypeFunc::Parms);
      if (targ->basic_type() == T_DOUBLE) {
        // If any parameters are doubles, they must be rounded before
        // the call, dstore_rounding does gvn.transform
        Node *arg = argument(j);
        arg = dstore_rounding(arg);
        set_argument(j, arg);
      }
    }
  }
}

// rounding for strict float precision conformance
Node* GraphKit::precision_rounding(Node* n) {
  if (Matcher::strict_fp_requires_explicit_rounding) {
#ifdef IA32
    if (UseSSE == 0) {
      return _gvn.transform(new RoundFloatNode(0, n));
    }
#else
    Unimplemented();
#endif // IA32
  }
  return n;
}

// rounding for strict double precision conformance
Node* GraphKit::dprecision_rounding(Node *n) {
  if (Matcher::strict_fp_requires_explicit_rounding) {
#ifdef IA32
    if (UseSSE < 2) {
      return _gvn.transform(new RoundDoubleNode(0, n));
    }
#else
    Unimplemented();
#endif // IA32
  }
  return n;
}

// rounding for non-strict double stores
Node* GraphKit::dstore_rounding(Node* n) {
  if (Matcher::strict_fp_requires_explicit_rounding) {
#ifdef IA32
    if (UseSSE < 2) {
      return _gvn.transform(new RoundDoubleNode(0, n));
    }
#else
    Unimplemented();
#endif // IA32
  }
  return n;
}

//=============================================================================
// Generate a fast path/slow path idiom.  Graph looks like:
// [foo] indicates that 'foo' is a parameter
//
//              [in]     NULL
//                 \    /
//                  CmpP
//                  Bool ne
//                   If
//                  /  \
//              True    False-<2>
//              / |
//             /  cast_not_null
//           Load  |    |   ^
//        [fast_test]   |   |
// gvn to   opt_test    |   |
//          /    \      |  <1>
//      True     False  |
//        |         \\  |
//   [slow_call]     \[fast_result]
//    Ctl   Val       \      \
//     |               \      \
//    Catch       <1>   \      \
//   /    \        ^     \      \
//  Ex    No_Ex    |      \      \
//  |       \   \  |       \ <2>  \
//  ...      \  [slow_res] |  |    \   [null_result]
//            \         \--+--+---  |  |
//             \           | /    \ | /
//              --------Region     Phi
//
//=============================================================================
// Code is structured as a series of driver functions all called 'do_XXX' that
// call a set of helper functions.  Helper functions first, then drivers.

//------------------------------null_check_oop---------------------------------
// Null check oop.  Set null-path control into Region in slot 3.
// Make a cast-not-nullness use the other not-null control.  Return cast.
Node* GraphKit::null_check_oop(Node* value, Node* *null_control,
                               bool never_see_null,
                               bool safe_for_replace,
                               bool speculative) {
  // Initial NULL check taken path
  (*null_control) = top();
  Node* cast = null_check_common(value, T_OBJECT, false, null_control, speculative);

  // Generate uncommon_trap:
  if (never_see_null && (*null_control) != top()) {
    // If we see an unexpected null at a check-cast we record it and force a
    // recompile; the offending check-cast will be compiled to handle NULLs.
    // If we see more than one offending BCI, then all checkcasts in the
    // method will be compiled to handle NULLs.
    PreserveJVMState pjvms(this);
    set_control(*null_control);
    replace_in_map(value, null());
    Deoptimization::DeoptReason reason = Deoptimization::reason_null_check(speculative);
    uncommon_trap(reason,
                  Deoptimization::Action_make_not_entrant);
    (*null_control) = top();    // NULL path is dead
  }
  if ((*null_control) == top() && safe_for_replace) {
    replace_in_map(value, cast);
  }

  // Cast away null-ness on the result
  return cast;
}

//------------------------------opt_iff----------------------------------------
// Optimize the fast-check IfNode.  Set the fast-path region slot 2.
// Return slow-path control.
Node* GraphKit::opt_iff(Node* region, Node* iff) {
  IfNode *opt_iff = _gvn.transform(iff)->as_If();

  // Fast path taken; set region slot 2
  Node *fast_taken = _gvn.transform( new IfFalseNode(opt_iff) );
  region->init_req(2,fast_taken); // Capture fast-control

  // Fast path not-taken, i.e. slow path
  Node *slow_taken = _gvn.transform( new IfTrueNode(opt_iff) );
  return slow_taken;
}

//-----------------------------make_runtime_call-------------------------------
Node* GraphKit::make_runtime_call(int flags,
                                  const TypeFunc* call_type, address call_addr,
                                  const char* call_name,
                                  const TypePtr* adr_type,
                                  // The following parms are all optional.
                                  // The first NULL ends the list.
                                  Node* parm0, Node* parm1,
                                  Node* parm2, Node* parm3,
                                  Node* parm4, Node* parm5,
                                  Node* parm6, Node* parm7) {
  assert(call_addr != NULL, "must not call NULL targets");

  // Slow-path call
  bool is_leaf = !(flags & RC_NO_LEAF);
  bool has_io  = (!is_leaf && !(flags & RC_NO_IO));
  if (call_name == NULL) {
    assert(!is_leaf, "must supply name for leaf");
    call_name = OptoRuntime::stub_name(call_addr);
  }
  CallNode* call;
  if (!is_leaf) {
    call = new CallStaticJavaNode(call_type, call_addr, call_name, adr_type);
  } else if (flags & RC_NO_FP) {
    call = new CallLeafNoFPNode(call_type, call_addr, call_name, adr_type);
  } else  if (flags & RC_VECTOR){
    uint num_bits = call_type->range()->field_at(TypeFunc::Parms)->is_vect()->length_in_bytes() * BitsPerByte;
    call = new CallLeafVectorNode(call_type, call_addr, call_name, adr_type, num_bits);
  } else {
    call = new CallLeafNode(call_type, call_addr, call_name, adr_type);
  }

  // The following is similar to set_edges_for_java_call,
  // except that the memory effects of the call are restricted to AliasIdxRaw.

  // Slow path call has no side-effects, uses few values
  bool wide_in  = !(flags & RC_NARROW_MEM);
  bool wide_out = (C->get_alias_index(adr_type) == Compile::AliasIdxBot);

  Node* prev_mem = NULL;
  if (wide_in) {
    prev_mem = set_predefined_input_for_runtime_call(call);
  } else {
    assert(!wide_out, "narrow in => narrow out");
    Node* narrow_mem = memory(adr_type);
    prev_mem = set_predefined_input_for_runtime_call(call, narrow_mem);
  }

  // Hook each parm in order.  Stop looking at the first NULL.
  if (parm0 != NULL) { call->init_req(TypeFunc::Parms+0, parm0);
  if (parm1 != NULL) { call->init_req(TypeFunc::Parms+1, parm1);
  if (parm2 != NULL) { call->init_req(TypeFunc::Parms+2, parm2);
  if (parm3 != NULL) { call->init_req(TypeFunc::Parms+3, parm3);
  if (parm4 != NULL) { call->init_req(TypeFunc::Parms+4, parm4);
  if (parm5 != NULL) { call->init_req(TypeFunc::Parms+5, parm5);
  if (parm6 != NULL) { call->init_req(TypeFunc::Parms+6, parm6);
  if (parm7 != NULL) { call->init_req(TypeFunc::Parms+7, parm7);
    /* close each nested if ===> */  } } } } } } } }
  assert(call->in(call->req()-1) != NULL, "must initialize all parms");

  if (!is_leaf) {
    // Non-leaves can block and take safepoints:
    add_safepoint_edges(call, ((flags & RC_MUST_THROW) != 0));
  }
  // Non-leaves can throw exceptions:
  if (has_io) {
    call->set_req(TypeFunc::I_O, i_o());
  }

  if (flags & RC_UNCOMMON) {
    // Set the count to a tiny probability.  Cf. Estimate_Block_Frequency.
    // (An "if" probability corresponds roughly to an unconditional count.
    // Sort of.)
    call->set_cnt(PROB_UNLIKELY_MAG(4));
  }

  Node* c = _gvn.transform(call);
  assert(c == call, "cannot disappear");

  if (wide_out) {
    // Slow path call has full side-effects.
    set_predefined_output_for_runtime_call(call);
  } else {
    // Slow path call has few side-effects, and/or sets few values.
    set_predefined_output_for_runtime_call(call, prev_mem, adr_type);
  }

  if (has_io) {
    set_i_o(_gvn.transform(new ProjNode(call, TypeFunc::I_O)));
  }
  return call;

}

// i2b
Node* GraphKit::sign_extend_byte(Node* in) {
  Node* tmp = _gvn.transform(new LShiftINode(in, _gvn.intcon(24)));
  return _gvn.transform(new RShiftINode(tmp, _gvn.intcon(24)));
}

// i2s
Node* GraphKit::sign_extend_short(Node* in) {
  Node* tmp = _gvn.transform(new LShiftINode(in, _gvn.intcon(16)));
  return _gvn.transform(new RShiftINode(tmp, _gvn.intcon(16)));
}

//-----------------------------make_native_call-------------------------------
Node* GraphKit::make_native_call(address call_addr, const TypeFunc* call_type, uint nargs, ciNativeEntryPoint* nep) {
  // Select just the actual call args to pass on
  // [MethodHandle fallback, long addr, HALF addr, ... args , NativeEntryPoint nep]
  //                                             |          |
  //                                             V          V
  //                                             [ ... args ]
  uint n_filtered_args = nargs - 4; // -fallback, -addr (2), -nep;
  ResourceMark rm;
  Node** argument_nodes = NEW_RESOURCE_ARRAY(Node*, n_filtered_args);
  const Type** arg_types = TypeTuple::fields(n_filtered_args);
  GrowableArray<VMReg> arg_regs(C->comp_arena(), n_filtered_args, n_filtered_args, VMRegImpl::Bad());

  VMReg* argRegs = nep->argMoves();
  {
    for (uint vm_arg_pos = 0, java_arg_read_pos = 0;
        vm_arg_pos < n_filtered_args; vm_arg_pos++) {
      uint vm_unfiltered_arg_pos = vm_arg_pos + 3; // +3 to skip fallback handle argument and addr (2 since long)
      Node* node = argument(vm_unfiltered_arg_pos);
      const Type* type = call_type->domain()->field_at(TypeFunc::Parms + vm_unfiltered_arg_pos);
      VMReg reg = type == Type::HALF
        ? VMRegImpl::Bad()
        : argRegs[java_arg_read_pos++];

      argument_nodes[vm_arg_pos] = node;
      arg_types[TypeFunc::Parms + vm_arg_pos] = type;
      arg_regs.at_put(vm_arg_pos, reg);
    }
  }

  uint n_returns = call_type->range()->cnt() - TypeFunc::Parms;
  GrowableArray<VMReg> ret_regs(C->comp_arena(), n_returns, n_returns, VMRegImpl::Bad());
  const Type** ret_types = TypeTuple::fields(n_returns);

  VMReg* retRegs = nep->returnMoves();
  {
    for (uint vm_ret_pos = 0, java_ret_read_pos = 0;
        vm_ret_pos < n_returns; vm_ret_pos++) { // 0 or 1
      const Type* type = call_type->range()->field_at(TypeFunc::Parms + vm_ret_pos);
      VMReg reg = type == Type::HALF
        ? VMRegImpl::Bad()
        : retRegs[java_ret_read_pos++];

      ret_regs.at_put(vm_ret_pos, reg);
      ret_types[TypeFunc::Parms + vm_ret_pos] = type;
    }
  }

  const TypeFunc* new_call_type = TypeFunc::make(
    TypeTuple::make(TypeFunc::Parms + n_filtered_args, arg_types),
    TypeTuple::make(TypeFunc::Parms + n_returns, ret_types)
  );

  if (nep->need_transition()) {
    RuntimeStub* invoker = SharedRuntime::make_native_invoker(call_addr,
                                                              nep->shadow_space(),
                                                              arg_regs, ret_regs);
    if (invoker == NULL) {
      C->record_failure("native invoker not implemented on this platform");
      return NULL;
    }
    C->add_native_invoker(invoker);
    call_addr = invoker->code_begin();
  }
  assert(call_addr != NULL, "sanity");

  CallNativeNode* call = new CallNativeNode(new_call_type, call_addr, nep->name(), TypePtr::BOTTOM,
                                            arg_regs,
                                            ret_regs,
                                            nep->shadow_space(),
                                            nep->need_transition());

  if (call->_need_transition) {
    add_safepoint_edges(call);
  }

  set_predefined_input_for_runtime_call(call);

  for (uint i = 0; i < n_filtered_args; i++) {
    call->init_req(i + TypeFunc::Parms, argument_nodes[i]);
  }

  Node* c = gvn().transform(call);
  assert(c == call, "cannot disappear");

  set_predefined_output_for_runtime_call(call);

  Node* ret;
  if (method() == NULL || method()->return_type()->basic_type() == T_VOID) {
    ret = top();
  } else {
    ret =  gvn().transform(new ProjNode(call, TypeFunc::Parms));
    // Unpack native results if needed
    // Need this method type since it's unerased
    switch (nep->method_type()->rtype()->basic_type()) {
      case T_CHAR:
        ret = _gvn.transform(new AndINode(ret, _gvn.intcon(0xFFFF)));
        break;
      case T_BYTE:
        ret = sign_extend_byte(ret);
        break;
      case T_SHORT:
        ret = sign_extend_short(ret);
        break;
      default: // do nothing
        break;
    }
  }

  push_node(method()->return_type()->basic_type(), ret);

  return call;
}

//------------------------------merge_memory-----------------------------------
// Merge memory from one path into the current memory state.
void GraphKit::merge_memory(Node* new_mem, Node* region, int new_path) {
  for (MergeMemStream mms(merged_memory(), new_mem->as_MergeMem()); mms.next_non_empty2(); ) {
    Node* old_slice = mms.force_memory();
    Node* new_slice = mms.memory2();
    if (old_slice != new_slice) {
      PhiNode* phi;
      if (old_slice->is_Phi() && old_slice->as_Phi()->region() == region) {
        if (mms.is_empty()) {
          // clone base memory Phi's inputs for this memory slice
          assert(old_slice == mms.base_memory(), "sanity");
          phi = PhiNode::make(region, NULL, Type::MEMORY, mms.adr_type(C));
          _gvn.set_type(phi, Type::MEMORY);
          for (uint i = 1; i < phi->req(); i++) {
            phi->init_req(i, old_slice->in(i));
          }
        } else {
          phi = old_slice->as_Phi(); // Phi was generated already
        }
      } else {
        phi = PhiNode::make(region, old_slice, Type::MEMORY, mms.adr_type(C));
        _gvn.set_type(phi, Type::MEMORY);
      }
      phi->set_req(new_path, new_slice);
      mms.set_memory(phi);
    }
  }
}

//------------------------------make_slow_call_ex------------------------------
// Make the exception handler hookups for the slow call
void GraphKit::make_slow_call_ex(Node* call, ciInstanceKlass* ex_klass, bool separate_io_proj, bool deoptimize) {
  if (stopped())  return;

  // Make a catch node with just two handlers:  fall-through and catch-all
  Node* i_o  = _gvn.transform( new ProjNode(call, TypeFunc::I_O, separate_io_proj) );
  Node* catc = _gvn.transform( new CatchNode(control(), i_o, 2) );
  Node* norm = _gvn.transform( new CatchProjNode(catc, CatchProjNode::fall_through_index, CatchProjNode::no_handler_bci) );
  Node* excp = _gvn.transform( new CatchProjNode(catc, CatchProjNode::catch_all_index,    CatchProjNode::no_handler_bci) );

  { PreserveJVMState pjvms(this);
    set_control(excp);
    set_i_o(i_o);

    if (excp != top()) {
      if (deoptimize) {
        // Deoptimize if an exception is caught. Don't construct exception state in this case.
        uncommon_trap(Deoptimization::Reason_unhandled,
                      Deoptimization::Action_none);
      } else {
        // Create an exception state also.
        // Use an exact type if the caller has a specific exception.
        const Type* ex_type = TypeOopPtr::make_from_klass_unique(ex_klass)->cast_to_ptr_type(TypePtr::NotNull);
        Node*       ex_oop  = new CreateExNode(ex_type, control(), i_o);
        add_exception_state(make_exception_state(_gvn.transform(ex_oop)));
      }
    }
  }

  // Get the no-exception control from the CatchNode.
  set_control(norm);
}

static IfNode* gen_subtype_check_compare(Node* ctrl, Node* in1, Node* in2, BoolTest::mask test, float p, PhaseGVN& gvn, BasicType bt) {
  Node* cmp = NULL;
  switch(bt) {
  case T_INT: cmp = new CmpINode(in1, in2); break;
  case T_ADDRESS: cmp = new CmpPNode(in1, in2); break;
  default: fatal("unexpected comparison type %s", type2name(bt));
  }
  gvn.transform(cmp);
  Node* bol = gvn.transform(new BoolNode(cmp, test));
  IfNode* iff = new IfNode(ctrl, bol, p, COUNT_UNKNOWN);
  gvn.transform(iff);
  if (!bol->is_Con()) gvn.record_for_igvn(iff);
  return iff;
}

//-------------------------------gen_subtype_check-----------------------------
// Generate a subtyping check.  Takes as input the subtype and supertype.
// Returns 2 values: sets the default control() to the true path and returns
// the false path.  Only reads invariant memory; sets no (visible) memory.
// The PartialSubtypeCheckNode sets the hidden 1-word cache in the encoding
// but that's not exposed to the optimizer.  This call also doesn't take in an
// Object; if you wish to check an Object you need to load the Object's class
// prior to coming here.
Node* Phase::gen_subtype_check(Node* subklass, Node* superklass, Node** ctrl, Node* mem, PhaseGVN& gvn) {
  Compile* C = gvn.C;
  if ((*ctrl)->is_top()) {
    return C->top();
  }

  // Fast check for identical types, perhaps identical constants.
  // The types can even be identical non-constants, in cases
  // involving Array.newInstance, Object.clone, etc.
  if (subklass == superklass)
    return C->top();             // false path is dead; no test needed.

  if (gvn.type(superklass)->singleton()) {
    ciKlass* superk = gvn.type(superklass)->is_klassptr()->klass();
    ciKlass* subk   = gvn.type(subklass)->is_klassptr()->klass();

    // In the common case of an exact superklass, try to fold up the
    // test before generating code.  You may ask, why not just generate
    // the code and then let it fold up?  The answer is that the generated
    // code will necessarily include null checks, which do not always
    // completely fold away.  If they are also needless, then they turn
    // into a performance loss.  Example:
    //    Foo[] fa = blah(); Foo x = fa[0]; fa[1] = x;
    // Here, the type of 'fa' is often exact, so the store check
    // of fa[1]=x will fold up, without testing the nullness of x.
    switch (C->static_subtype_check(superk, subk)) {
    case Compile::SSC_always_false:
      {
        Node* always_fail = *ctrl;
        *ctrl = gvn.C->top();
        return always_fail;
      }
    case Compile::SSC_always_true:
      return C->top();
    case Compile::SSC_easy_test:
      {
        // Just do a direct pointer compare and be done.
        IfNode* iff = gen_subtype_check_compare(*ctrl, subklass, superklass, BoolTest::eq, PROB_STATIC_FREQUENT, gvn, T_ADDRESS);
        *ctrl = gvn.transform(new IfTrueNode(iff));
        return gvn.transform(new IfFalseNode(iff));
      }
    case Compile::SSC_full_test:
      break;
    default:
      ShouldNotReachHere();
    }
  }

  // %%% Possible further optimization:  Even if the superklass is not exact,
  // if the subklass is the unique subtype of the superklass, the check
  // will always succeed.  We could leave a dependency behind to ensure this.

  // First load the super-klass's check-offset
  Node *p1 = gvn.transform(new AddPNode(superklass, superklass, gvn.MakeConX(in_bytes(Klass::super_check_offset_offset()))));
  Node* m = C->immutable_memory();
  Node *chk_off = gvn.transform(new LoadINode(NULL, m, p1, gvn.type(p1)->is_ptr(), TypeInt::INT, MemNode::unordered));
  int cacheoff_con = in_bytes(Klass::secondary_super_cache_offset());
  bool might_be_cache = (gvn.find_int_con(chk_off, cacheoff_con) == cacheoff_con);

  // Load from the sub-klass's super-class display list, or a 1-word cache of
  // the secondary superclass list, or a failing value with a sentinel offset
  // if the super-klass is an interface or exceptionally deep in the Java
  // hierarchy and we have to scan the secondary superclass list the hard way.
  // Worst-case type is a little odd: NULL is allowed as a result (usually
  // klass loads can never produce a NULL).
  Node *chk_off_X = chk_off;
#ifdef _LP64
  chk_off_X = gvn.transform(new ConvI2LNode(chk_off_X));
#endif
  Node *p2 = gvn.transform(new AddPNode(subklass,subklass,chk_off_X));
  // For some types like interfaces the following loadKlass is from a 1-word
  // cache which is mutable so can't use immutable memory.  Other
  // types load from the super-class display table which is immutable.
  Node *kmem = C->immutable_memory();
  // secondary_super_cache is not immutable but can be treated as such because:
  // - no ideal node writes to it in a way that could cause an
  //   incorrect/missed optimization of the following Load.
  // - it's a cache so, worse case, not reading the latest value
  //   wouldn't cause incorrect execution
  if (might_be_cache && mem != NULL) {
    kmem = mem->is_MergeMem() ? mem->as_MergeMem()->memory_at(C->get_alias_index(gvn.type(p2)->is_ptr())) : mem;
  }
  Node *nkls = gvn.transform(LoadKlassNode::make(gvn, NULL, kmem, p2, gvn.type(p2)->is_ptr(), TypeKlassPtr::OBJECT_OR_NULL));

  // Compile speed common case: ARE a subtype and we canNOT fail
  if( superklass == nkls )
    return C->top();             // false path is dead; no test needed.

  // See if we get an immediate positive hit.  Happens roughly 83% of the
  // time.  Test to see if the value loaded just previously from the subklass
  // is exactly the superklass.
  IfNode *iff1 = gen_subtype_check_compare(*ctrl, superklass, nkls, BoolTest::eq, PROB_LIKELY(0.83f), gvn, T_ADDRESS);
  Node *iftrue1 = gvn.transform( new IfTrueNode (iff1));
  *ctrl = gvn.transform(new IfFalseNode(iff1));

  // Compile speed common case: Check for being deterministic right now.  If
  // chk_off is a constant and not equal to cacheoff then we are NOT a
  // subklass.  In this case we need exactly the 1 test above and we can
  // return those results immediately.
  if (!might_be_cache) {
    Node* not_subtype_ctrl = *ctrl;
    *ctrl = iftrue1; // We need exactly the 1 test above
    return not_subtype_ctrl;
  }

  // Gather the various success & failures here
  RegionNode *r_ok_subtype = new RegionNode(4);
  gvn.record_for_igvn(r_ok_subtype);
  RegionNode *r_not_subtype = new RegionNode(3);
  gvn.record_for_igvn(r_not_subtype);

  r_ok_subtype->init_req(1, iftrue1);

  // Check for immediate negative hit.  Happens roughly 11% of the time (which
  // is roughly 63% of the remaining cases).  Test to see if the loaded
  // check-offset points into the subklass display list or the 1-element
  // cache.  If it points to the display (and NOT the cache) and the display
  // missed then it's not a subtype.
  Node *cacheoff = gvn.intcon(cacheoff_con);
  IfNode *iff2 = gen_subtype_check_compare(*ctrl, chk_off, cacheoff, BoolTest::ne, PROB_LIKELY(0.63f), gvn, T_INT);
  r_not_subtype->init_req(1, gvn.transform(new IfTrueNode (iff2)));
  *ctrl = gvn.transform(new IfFalseNode(iff2));

  // Check for self.  Very rare to get here, but it is taken 1/3 the time.
  // No performance impact (too rare) but allows sharing of secondary arrays
  // which has some footprint reduction.
  IfNode *iff3 = gen_subtype_check_compare(*ctrl, subklass, superklass, BoolTest::eq, PROB_LIKELY(0.36f), gvn, T_ADDRESS);
  r_ok_subtype->init_req(2, gvn.transform(new IfTrueNode(iff3)));
  *ctrl = gvn.transform(new IfFalseNode(iff3));

  // -- Roads not taken here: --
  // We could also have chosen to perform the self-check at the beginning
  // of this code sequence, as the assembler does.  This would not pay off
  // the same way, since the optimizer, unlike the assembler, can perform
  // static type analysis to fold away many successful self-checks.
  // Non-foldable self checks work better here in second position, because
  // the initial primary superclass check subsumes a self-check for most
  // types.  An exception would be a secondary type like array-of-interface,
  // which does not appear in its own primary supertype display.
  // Finally, we could have chosen to move the self-check into the
  // PartialSubtypeCheckNode, and from there out-of-line in a platform
  // dependent manner.  But it is worthwhile to have the check here,
  // where it can be perhaps be optimized.  The cost in code space is
  // small (register compare, branch).

  // Now do a linear scan of the secondary super-klass array.  Again, no real
  // performance impact (too rare) but it's gotta be done.
  // Since the code is rarely used, there is no penalty for moving it
  // out of line, and it can only improve I-cache density.
  // The decision to inline or out-of-line this final check is platform
  // dependent, and is found in the AD file definition of PartialSubtypeCheck.
  Node* psc = gvn.transform(
    new PartialSubtypeCheckNode(*ctrl, subklass, superklass));

  IfNode *iff4 = gen_subtype_check_compare(*ctrl, psc, gvn.zerocon(T_OBJECT), BoolTest::ne, PROB_FAIR, gvn, T_ADDRESS);
  r_not_subtype->init_req(2, gvn.transform(new IfTrueNode (iff4)));
  r_ok_subtype ->init_req(3, gvn.transform(new IfFalseNode(iff4)));

  // Return false path; set default control to true path.
  *ctrl = gvn.transform(r_ok_subtype);
  return gvn.transform(r_not_subtype);
}

Node* GraphKit::gen_subtype_check(Node* obj_or_subklass, Node* superklass) {
  bool expand_subtype_check = C->post_loop_opts_phase() ||   // macro node expansion is over
                              ExpandSubTypeCheckAtParseTime; // forced expansion
  if (expand_subtype_check) {
    MergeMemNode* mem = merged_memory();
    Node* ctrl = control();
    Node* subklass = obj_or_subklass;
    if (!_gvn.type(obj_or_subklass)->isa_klassptr()) {
      subklass = load_object_klass(obj_or_subklass);
    }

    Node* n = Phase::gen_subtype_check(subklass, superklass, &ctrl, mem, _gvn);
    set_control(ctrl);
    return n;
  }

  Node* check = _gvn.transform(new SubTypeCheckNode(C, obj_or_subklass, superklass));
  Node* bol = _gvn.transform(new BoolNode(check, BoolTest::eq));
  IfNode* iff = create_and_xform_if(control(), bol, PROB_STATIC_FREQUENT, COUNT_UNKNOWN);
  set_control(_gvn.transform(new IfTrueNode(iff)));
  return _gvn.transform(new IfFalseNode(iff));
}

// Profile-driven exact type check:
Node* GraphKit::type_check_receiver(Node* receiver, ciKlass* klass,
                                    float prob,
                                    Node* *casted_receiver) {
  assert(!klass->is_interface(), "no exact type check on interfaces");

  const TypeKlassPtr* tklass = TypeKlassPtr::make(klass);
  Node* recv_klass = load_object_klass(receiver);
  Node* want_klass = makecon(tklass);
  Node* cmp = _gvn.transform(new CmpPNode(recv_klass, want_klass));
  Node* bol = _gvn.transform(new BoolNode(cmp, BoolTest::eq));
  IfNode* iff = create_and_xform_if(control(), bol, prob, COUNT_UNKNOWN);
  set_control( _gvn.transform(new IfTrueNode (iff)));
  Node* fail = _gvn.transform(new IfFalseNode(iff));

  if (!stopped()) {
    const TypeOopPtr* receiver_type = _gvn.type(receiver)->isa_oopptr();
    const TypeOopPtr* recvx_type = tklass->as_instance_type();
    assert(recvx_type->klass_is_exact(), "");

    if (!receiver_type->higher_equal(recvx_type)) { // ignore redundant casts
      // Subsume downstream occurrences of receiver with a cast to
      // recv_xtype, since now we know what the type will be.
      Node* cast = new CheckCastPPNode(control(), receiver, recvx_type);
      (*casted_receiver) = _gvn.transform(cast);
      // (User must make the replace_in_map call.)
    }
  }

  return fail;
}

//------------------------------subtype_check_receiver-------------------------
Node* GraphKit::subtype_check_receiver(Node* receiver, ciKlass* klass,
                                       Node** casted_receiver) {
  const TypeKlassPtr* tklass = TypeKlassPtr::make(klass);
  Node* want_klass = makecon(tklass);

  Node* slow_ctl = gen_subtype_check(receiver, want_klass);

  // Ignore interface type information until interface types are properly tracked.
  if (!stopped() && !klass->is_interface()) {
    const TypeOopPtr* receiver_type = _gvn.type(receiver)->isa_oopptr();
    const TypeOopPtr* recv_type = tklass->cast_to_exactness(false)->is_klassptr()->as_instance_type();
    if (!receiver_type->higher_equal(recv_type)) { // ignore redundant casts
      Node* cast = new CheckCastPPNode(control(), receiver, recv_type);
      (*casted_receiver) = _gvn.transform(cast);
    }
  }

  return slow_ctl;
}

//------------------------------seems_never_null-------------------------------
// Use null_seen information if it is available from the profile.
// If we see an unexpected null at a type check we record it and force a
// recompile; the offending check will be recompiled to handle NULLs.
// If we see several offending BCIs, then all checks in the
// method will be recompiled.
bool GraphKit::seems_never_null(Node* obj, ciProfileData* data, bool& speculating) {
  speculating = !_gvn.type(obj)->speculative_maybe_null();
  Deoptimization::DeoptReason reason = Deoptimization::reason_null_check(speculating);
  if (UncommonNullCast               // Cutout for this technique
      && obj != null()               // And not the -Xcomp stupid case?
      && !too_many_traps(reason)
      ) {
    if (speculating) {
      return true;
    }
    if (data == NULL)
      // Edge case:  no mature data.  Be optimistic here.
      return true;
    // If the profile has not seen a null, assume it won't happen.
    assert(java_bc() == Bytecodes::_checkcast ||
           java_bc() == Bytecodes::_instanceof ||
           java_bc() == Bytecodes::_aastore, "MDO must collect null_seen bit here");
    return !data->as_BitData()->null_seen();
  }
  speculating = false;
  return false;
}

void GraphKit::guard_klass_being_initialized(Node* klass) {
  int init_state_off = in_bytes(InstanceKlass::init_state_offset());
  Node* adr = basic_plus_adr(top(), klass, init_state_off);
  Node* init_state = LoadNode::make(_gvn, NULL, immutable_memory(), adr,
                                    adr->bottom_type()->is_ptr(), TypeInt::BYTE,
                                    T_BYTE, MemNode::unordered);
  init_state = _gvn.transform(init_state);

  Node* being_initialized_state = makecon(TypeInt::make(InstanceKlass::being_initialized));

  Node* chk = _gvn.transform(new CmpINode(being_initialized_state, init_state));
  Node* tst = _gvn.transform(new BoolNode(chk, BoolTest::eq));

  { BuildCutout unless(this, tst, PROB_MAX);
    uncommon_trap(Deoptimization::Reason_initialized, Deoptimization::Action_reinterpret);
  }
}

void GraphKit::guard_init_thread(Node* klass) {
  int init_thread_off = in_bytes(InstanceKlass::init_thread_offset());
  Node* adr = basic_plus_adr(top(), klass, init_thread_off);

  Node* init_thread = LoadNode::make(_gvn, NULL, immutable_memory(), adr,
                                     adr->bottom_type()->is_ptr(), TypePtr::NOTNULL,
                                     T_ADDRESS, MemNode::unordered);
  init_thread = _gvn.transform(init_thread);

  Node* cur_thread = _gvn.transform(new ThreadLocalNode());

  Node* chk = _gvn.transform(new CmpPNode(cur_thread, init_thread));
  Node* tst = _gvn.transform(new BoolNode(chk, BoolTest::eq));

  { BuildCutout unless(this, tst, PROB_MAX);
    uncommon_trap(Deoptimization::Reason_uninitialized, Deoptimization::Action_none);
  }
}

void GraphKit::clinit_barrier(ciInstanceKlass* ik, ciMethod* context) {
  if (ik->is_being_initialized()) {
    if (C->needs_clinit_barrier(ik, context)) {
      Node* klass = makecon(TypeKlassPtr::make(ik));
      guard_klass_being_initialized(klass);
      guard_init_thread(klass);
      insert_mem_bar(Op_MemBarCPUOrder);
    }
  } else if (ik->is_initialized()) {
    return; // no barrier needed
  } else {
    uncommon_trap(Deoptimization::Reason_uninitialized,
                  Deoptimization::Action_reinterpret,
                  NULL);
  }
}

//------------------------maybe_cast_profiled_receiver-------------------------
// If the profile has seen exactly one type, narrow to exactly that type.
// Subsequent type checks will always fold up.
Node* GraphKit::maybe_cast_profiled_receiver(Node* not_null_obj,
                                             ciKlass* require_klass,
                                             ciKlass* spec_klass,
                                             bool safe_for_replace) {
  if (!UseTypeProfile || !TypeProfileCasts) return NULL;

  Deoptimization::DeoptReason reason = Deoptimization::reason_class_check(spec_klass != NULL);

  // Make sure we haven't already deoptimized from this tactic.
  if (too_many_traps_or_recompiles(reason))
    return NULL;

  // (No, this isn't a call, but it's enough like a virtual call
  // to use the same ciMethod accessor to get the profile info...)
  // If we have a speculative type use it instead of profiling (which
  // may not help us)
  ciKlass* exact_kls = spec_klass == NULL ? profile_has_unique_klass() : spec_klass;
  if (exact_kls != NULL) {// no cast failures here
    if (require_klass == NULL ||
        C->static_subtype_check(require_klass, exact_kls) == Compile::SSC_always_true) {
      // If we narrow the type to match what the type profile sees or
      // the speculative type, we can then remove the rest of the
      // cast.
      // This is a win, even if the exact_kls is very specific,
      // because downstream operations, such as method calls,
      // will often benefit from the sharper type.
      Node* exact_obj = not_null_obj; // will get updated in place...
      Node* slow_ctl  = type_check_receiver(exact_obj, exact_kls, 1.0,
                                            &exact_obj);
      { PreserveJVMState pjvms(this);
        set_control(slow_ctl);
        uncommon_trap_exact(reason, Deoptimization::Action_maybe_recompile);
      }
      if (safe_for_replace) {
        replace_in_map(not_null_obj, exact_obj);
      }
      return exact_obj;
    }
    // assert(ssc == Compile::SSC_always_true)... except maybe the profile lied to us.
  }

  return NULL;
}

/**
 * Cast obj to type and emit guard unless we had too many traps here
 * already
 *
 * @param obj       node being casted
 * @param type      type to cast the node to
 * @param not_null  true if we know node cannot be null
 */
Node* GraphKit::maybe_cast_profiled_obj(Node* obj,
                                        ciKlass* type,
                                        bool not_null) {
  if (stopped()) {
    return obj;
  }

  // type == NULL if profiling tells us this object is always null
  if (type != NULL) {
    Deoptimization::DeoptReason class_reason = Deoptimization::Reason_speculate_class_check;
    Deoptimization::DeoptReason null_reason = Deoptimization::Reason_speculate_null_check;

    if (!too_many_traps_or_recompiles(null_reason) &&
        !too_many_traps_or_recompiles(class_reason)) {
      Node* not_null_obj = NULL;
      // not_null is true if we know the object is not null and
      // there's no need for a null check
      if (!not_null) {
        Node* null_ctl = top();
        not_null_obj = null_check_oop(obj, &null_ctl, true, true, true);
        assert(null_ctl->is_top(), "no null control here");
      } else {
        not_null_obj = obj;
      }

      Node* exact_obj = not_null_obj;
      ciKlass* exact_kls = type;
      Node* slow_ctl  = type_check_receiver(exact_obj, exact_kls, 1.0,
                                            &exact_obj);
      {
        PreserveJVMState pjvms(this);
        set_control(slow_ctl);
        uncommon_trap_exact(class_reason, Deoptimization::Action_maybe_recompile);
      }
      replace_in_map(not_null_obj, exact_obj);
      obj = exact_obj;
    }
  } else {
    if (!too_many_traps_or_recompiles(Deoptimization::Reason_null_assert)) {
      Node* exact_obj = null_assert(obj);
      replace_in_map(obj, exact_obj);
      obj = exact_obj;
    }
  }
  return obj;
}

//-------------------------------gen_instanceof--------------------------------
// Generate an instance-of idiom.  Used by both the instance-of bytecode
// and the reflective instance-of call.
Node* GraphKit::gen_instanceof(Node* obj, Node* superklass, bool safe_for_replace) {
  kill_dead_locals();           // Benefit all the uncommon traps
  assert( !stopped(), "dead parse path should be checked in callers" );
  assert(!TypePtr::NULL_PTR->higher_equal(_gvn.type(superklass)->is_klassptr()),
         "must check for not-null not-dead klass in callers");

  // Make the merge point
  enum { _obj_path = 1, _fail_path, _null_path, PATH_LIMIT };
  RegionNode* region = new RegionNode(PATH_LIMIT);
  Node*       phi    = new PhiNode(region, TypeInt::BOOL);
  C->set_has_split_ifs(true); // Has chance for split-if optimization

  ciProfileData* data = NULL;
  if (java_bc() == Bytecodes::_instanceof) {  // Only for the bytecode
    data = method()->method_data()->bci_to_data(bci());
  }
  bool speculative_not_null = false;
  bool never_see_null = (ProfileDynamicTypes  // aggressive use of profile
                         && seems_never_null(obj, data, speculative_not_null));

  // Null check; get casted pointer; set region slot 3
  Node* null_ctl = top();
  Node* not_null_obj = null_check_oop(obj, &null_ctl, never_see_null, safe_for_replace, speculative_not_null);

  // If not_null_obj is dead, only null-path is taken
  if (stopped()) {              // Doing instance-of on a NULL?
    set_control(null_ctl);
    return intcon(0);
  }
  region->init_req(_null_path, null_ctl);
  phi   ->init_req(_null_path, intcon(0)); // Set null path value
  if (null_ctl == top()) {
    // Do this eagerly, so that pattern matches like is_diamond_phi
    // will work even during parsing.
    assert(_null_path == PATH_LIMIT-1, "delete last");
    region->del_req(_null_path);
    phi   ->del_req(_null_path);
  }

  // Do we know the type check always succeed?
  bool known_statically = false;
  if (_gvn.type(superklass)->singleton()) {
    ciKlass* superk = _gvn.type(superklass)->is_klassptr()->klass();
    ciKlass* subk = _gvn.type(obj)->is_oopptr()->klass();
    if (subk != NULL && subk->is_loaded()) {
      int static_res = C->static_subtype_check(superk, subk);
      known_statically = (static_res == Compile::SSC_always_true || static_res == Compile::SSC_always_false);
    }
  }

  if (!known_statically) {
    const TypeOopPtr* obj_type = _gvn.type(obj)->is_oopptr();
    // We may not have profiling here or it may not help us. If we
    // have a speculative type use it to perform an exact cast.
    ciKlass* spec_obj_type = obj_type->speculative_type();
    if (spec_obj_type != NULL || (ProfileDynamicTypes && data != NULL)) {
      Node* cast_obj = maybe_cast_profiled_receiver(not_null_obj, NULL, spec_obj_type, safe_for_replace);
      if (stopped()) {            // Profile disagrees with this path.
        set_control(null_ctl);    // Null is the only remaining possibility.
        return intcon(0);
      }
      if (cast_obj != NULL) {
        not_null_obj = cast_obj;
      }
    }
  }

  // Generate the subtype check
  Node* not_subtype_ctrl = gen_subtype_check(not_null_obj, superklass);

  // Plug in the success path to the general merge in slot 1.
  region->init_req(_obj_path, control());
  phi   ->init_req(_obj_path, intcon(1));

  // Plug in the failing path to the general merge in slot 2.
  region->init_req(_fail_path, not_subtype_ctrl);
  phi   ->init_req(_fail_path, intcon(0));

  // Return final merged results
  set_control( _gvn.transform(region) );
  record_for_igvn(region);

  // If we know the type check always succeeds then we don't use the
  // profiling data at this bytecode. Don't lose it, feed it to the
  // type system as a speculative type.
  if (safe_for_replace) {
    Node* casted_obj = record_profiled_receiver_for_speculation(obj);
    replace_in_map(obj, casted_obj);
  }

  return _gvn.transform(phi);
}

//-------------------------------gen_checkcast---------------------------------
// Generate a checkcast idiom.  Used by both the checkcast bytecode and the
// array store bytecode.  Stack must be as-if BEFORE doing the bytecode so the
// uncommon-trap paths work.  Adjust stack after this call.
// If failure_control is supplied and not null, it is filled in with
// the control edge for the cast failure.  Otherwise, an appropriate
// uncommon trap or exception is thrown.
Node* GraphKit::gen_checkcast(Node *obj, Node* superklass,
                              Node* *failure_control) {
  kill_dead_locals();           // Benefit all the uncommon traps
  const TypeKlassPtr *tk = _gvn.type(superklass)->is_klassptr();
  const Type *toop = TypeOopPtr::make_from_klass(tk->klass());

  // Fast cutout:  Check the case that the cast is vacuously true.
  // This detects the common cases where the test will short-circuit
  // away completely.  We do this before we perform the null check,
  // because if the test is going to turn into zero code, we don't
  // want a residual null check left around.  (Causes a slowdown,
  // for example, in some objArray manipulations, such as a[i]=a[j].)
  if (tk->singleton()) {
    const TypeOopPtr* objtp = _gvn.type(obj)->isa_oopptr();
    if (objtp != NULL && objtp->klass() != NULL) {
      switch (C->static_subtype_check(tk->klass(), objtp->klass())) {
      case Compile::SSC_always_true:
        // If we know the type check always succeed then we don't use
        // the profiling data at this bytecode. Don't lose it, feed it
        // to the type system as a speculative type.
        return record_profiled_receiver_for_speculation(obj);
      case Compile::SSC_always_false:
        // It needs a null check because a null will *pass* the cast check.
        // A non-null value will always produce an exception.
        if (!objtp->maybe_null()) {
          builtin_throw(Deoptimization::Reason_class_check, makecon(TypeKlassPtr::make(objtp->klass())));
          return top();
        } else if (!too_many_traps_or_recompiles(Deoptimization::Reason_null_assert)) {
          return null_assert(obj);
        }
        break; // Fall through to full check
      }
    }
  }

  ciProfileData* data = NULL;
  bool safe_for_replace = false;
  if (failure_control == NULL) {        // use MDO in regular case only
    assert(java_bc() == Bytecodes::_aastore ||
           java_bc() == Bytecodes::_checkcast,
           "interpreter profiles type checks only for these BCs");
    data = method()->method_data()->bci_to_data(bci());
    safe_for_replace = true;
  }

  // Make the merge point
  enum { _obj_path = 1, _null_path, PATH_LIMIT };
  RegionNode* region = new RegionNode(PATH_LIMIT);
  Node*       phi    = new PhiNode(region, toop);
  C->set_has_split_ifs(true); // Has chance for split-if optimization

  // Use null-cast information if it is available
  bool speculative_not_null = false;
  bool never_see_null = ((failure_control == NULL)  // regular case only
                         && seems_never_null(obj, data, speculative_not_null));

  // Null check; get casted pointer; set region slot 3
  Node* null_ctl = top();
  Node* not_null_obj = null_check_oop(obj, &null_ctl, never_see_null, safe_for_replace, speculative_not_null);

  // If not_null_obj is dead, only null-path is taken
  if (stopped()) {              // Doing instance-of on a NULL?
    set_control(null_ctl);
    return null();
  }
  region->init_req(_null_path, null_ctl);
  phi   ->init_req(_null_path, null());  // Set null path value
  if (null_ctl == top()) {
    // Do this eagerly, so that pattern matches like is_diamond_phi
    // will work even during parsing.
    assert(_null_path == PATH_LIMIT-1, "delete last");
    region->del_req(_null_path);
    phi   ->del_req(_null_path);
  }

  Node* cast_obj = NULL;
  if (tk->klass_is_exact()) {
    // The following optimization tries to statically cast the speculative type of the object
    // (for example obtained during profiling) to the type of the superklass and then do a
    // dynamic check that the type of the object is what we expect. To work correctly
    // for checkcast and aastore the type of superklass should be exact.
    const TypeOopPtr* obj_type = _gvn.type(obj)->is_oopptr();
    // We may not have profiling here or it may not help us. If we have
    // a speculative type use it to perform an exact cast.
    ciKlass* spec_obj_type = obj_type->speculative_type();
    if (spec_obj_type != NULL || data != NULL) {
      cast_obj = maybe_cast_profiled_receiver(not_null_obj, tk->klass(), spec_obj_type, safe_for_replace);
      if (cast_obj != NULL) {
        if (failure_control != NULL) // failure is now impossible
          (*failure_control) = top();
        // adjust the type of the phi to the exact klass:
        phi->raise_bottom_type(_gvn.type(cast_obj)->meet_speculative(TypePtr::NULL_PTR));
      }
    }
  }

  if (cast_obj == NULL) {
    // Generate the subtype check
    Node* not_subtype_ctrl = gen_subtype_check(not_null_obj, superklass );

    // Plug in success path into the merge
    cast_obj = _gvn.transform(new CheckCastPPNode(control(), not_null_obj, toop));
    // Failure path ends in uncommon trap (or may be dead - failure impossible)
    if (failure_control == NULL) {
      if (not_subtype_ctrl != top()) { // If failure is possible
        PreserveJVMState pjvms(this);
        set_control(not_subtype_ctrl);
        builtin_throw(Deoptimization::Reason_class_check, load_object_klass(not_null_obj));
      }
    } else {
      (*failure_control) = not_subtype_ctrl;
    }
  }

  region->init_req(_obj_path, control());
  phi   ->init_req(_obj_path, cast_obj);

  // A merge of NULL or Casted-NotNull obj
  Node* res = _gvn.transform(phi);

  // Note I do NOT always 'replace_in_map(obj,result)' here.
  //  if( tk->klass()->can_be_primary_super()  )
    // This means that if I successfully store an Object into an array-of-String
    // I 'forget' that the Object is really now known to be a String.  I have to
    // do this because we don't have true union types for interfaces - if I store
    // a Baz into an array-of-Interface and then tell the optimizer it's an
    // Interface, I forget that it's also a Baz and cannot do Baz-like field
    // references to it.  FIX THIS WHEN UNION TYPES APPEAR!
  //  replace_in_map( obj, res );

  // Return final merged results
  set_control( _gvn.transform(region) );
  record_for_igvn(region);

  return record_profiled_receiver_for_speculation(res);
}

//------------------------------next_monitor-----------------------------------
// What number should be given to the next monitor?
int GraphKit::next_monitor() {
  int current = jvms()->monitor_depth()* C->sync_stack_slots();
  int next = current + C->sync_stack_slots();
  // Keep the toplevel high water mark current:
  if (C->fixed_slots() < next)  C->set_fixed_slots(next);
  return current;
}

//------------------------------insert_mem_bar---------------------------------
// Memory barrier to avoid floating things around
// The membar serves as a pinch point between both control and all memory slices.
Node* GraphKit::insert_mem_bar(int opcode, Node* precedent) {
  MemBarNode* mb = MemBarNode::make(C, opcode, Compile::AliasIdxBot, precedent);
  mb->init_req(TypeFunc::Control, control());
  mb->init_req(TypeFunc::Memory,  reset_memory());
  Node* membar = _gvn.transform(mb);
  set_control(_gvn.transform(new ProjNode(membar, TypeFunc::Control)));
  set_all_memory_call(membar);
  return membar;
}

//-------------------------insert_mem_bar_volatile----------------------------
// Memory barrier to avoid floating things around
// The membar serves as a pinch point between both control and memory(alias_idx).
// If you want to make a pinch point on all memory slices, do not use this
// function (even with AliasIdxBot); use insert_mem_bar() instead.
Node* GraphKit::insert_mem_bar_volatile(int opcode, int alias_idx, Node* precedent) {
  // When Parse::do_put_xxx updates a volatile field, it appends a series
  // of MemBarVolatile nodes, one for *each* volatile field alias category.
  // The first membar is on the same memory slice as the field store opcode.
  // This forces the membar to follow the store.  (Bug 6500685 broke this.)
  // All the other membars (for other volatile slices, including AliasIdxBot,
  // which stands for all unknown volatile slices) are control-dependent
  // on the first membar.  This prevents later volatile loads or stores
  // from sliding up past the just-emitted store.

  MemBarNode* mb = MemBarNode::make(C, opcode, alias_idx, precedent);
  mb->set_req(TypeFunc::Control,control());
  if (alias_idx == Compile::AliasIdxBot) {
    mb->set_req(TypeFunc::Memory, merged_memory()->base_memory());
  } else {
    assert(!(opcode == Op_Initialize && alias_idx != Compile::AliasIdxRaw), "fix caller");
    mb->set_req(TypeFunc::Memory, memory(alias_idx));
  }
  Node* membar = _gvn.transform(mb);
  set_control(_gvn.transform(new ProjNode(membar, TypeFunc::Control)));
  if (alias_idx == Compile::AliasIdxBot) {
    merged_memory()->set_base_memory(_gvn.transform(new ProjNode(membar, TypeFunc::Memory)));
  } else {
    set_memory(_gvn.transform(new ProjNode(membar, TypeFunc::Memory)),alias_idx);
  }
  return membar;
}

//------------------------------shared_lock------------------------------------
// Emit locking code.
FastLockNode* GraphKit::shared_lock(Node* obj) {
  // bci is either a monitorenter bc or InvocationEntryBci
  // %%% SynchronizationEntryBCI is redundant; use InvocationEntryBci in interfaces
  assert(SynchronizationEntryBCI == InvocationEntryBci, "");

  if( !GenerateSynchronizationCode )
    return NULL;                // Not locking things?
  if (stopped())                // Dead monitor?
    return NULL;

  assert(dead_locals_are_killed(), "should kill locals before sync. point");

  // Box the stack location
  Node* box = _gvn.transform(new BoxLockNode(next_monitor()));
  Node* mem = reset_memory();

  FastLockNode * flock = _gvn.transform(new FastLockNode(0, obj, box) )->as_FastLock();

  // Create the rtm counters for this fast lock if needed.
  flock->create_rtm_lock_counter(sync_jvms()); // sync_jvms used to get current bci

  // Add monitor to debug info for the slow path.  If we block inside the
  // slow path and de-opt, we need the monitor hanging around
  map()->push_monitor( flock );

  const TypeFunc *tf = LockNode::lock_type();
  LockNode *lock = new LockNode(C, tf);

  lock->init_req( TypeFunc::Control, control() );
  lock->init_req( TypeFunc::Memory , mem );
  lock->init_req( TypeFunc::I_O    , top() )     ;   // does no i/o
  lock->init_req( TypeFunc::FramePtr, frameptr() );
  lock->init_req( TypeFunc::ReturnAdr, top() );

  lock->init_req(TypeFunc::Parms + 0, obj);
  lock->init_req(TypeFunc::Parms + 1, box);
  lock->init_req(TypeFunc::Parms + 2, flock);
  add_safepoint_edges(lock);

  lock = _gvn.transform( lock )->as_Lock();

  // lock has no side-effects, sets few values
  set_predefined_output_for_runtime_call(lock, mem, TypeRawPtr::BOTTOM);

  insert_mem_bar(Op_MemBarAcquireLock);

  // Add this to the worklist so that the lock can be eliminated
  record_for_igvn(lock);

#ifndef PRODUCT
  if (PrintLockStatistics) {
    // Update the counter for this lock.  Don't bother using an atomic
    // operation since we don't require absolute accuracy.
    lock->create_lock_counter(map()->jvms());
    increment_counter(lock->counter()->addr());
  }
#endif

  return flock;
}


//------------------------------shared_unlock----------------------------------
// Emit unlocking code.
void GraphKit::shared_unlock(Node* box, Node* obj) {
  // bci is either a monitorenter bc or InvocationEntryBci
  // %%% SynchronizationEntryBCI is redundant; use InvocationEntryBci in interfaces
  assert(SynchronizationEntryBCI == InvocationEntryBci, "");

  if( !GenerateSynchronizationCode )
    return;
  if (stopped()) {               // Dead monitor?
    map()->pop_monitor();        // Kill monitor from debug info
    return;
  }

  // Memory barrier to avoid floating things down past the locked region
  insert_mem_bar(Op_MemBarReleaseLock);

  const TypeFunc *tf = OptoRuntime::complete_monitor_exit_Type();
  UnlockNode *unlock = new UnlockNode(C, tf);
#ifdef ASSERT
  unlock->set_dbg_jvms(sync_jvms());
#endif
  uint raw_idx = Compile::AliasIdxRaw;
  unlock->init_req( TypeFunc::Control, control() );
  unlock->init_req( TypeFunc::Memory , memory(raw_idx) );
  unlock->init_req( TypeFunc::I_O    , top() )     ;   // does no i/o
  unlock->init_req( TypeFunc::FramePtr, frameptr() );
  unlock->init_req( TypeFunc::ReturnAdr, top() );

  unlock->init_req(TypeFunc::Parms + 0, obj);
  unlock->init_req(TypeFunc::Parms + 1, box);
  unlock = _gvn.transform(unlock)->as_Unlock();

  Node* mem = reset_memory();

  // unlock has no side-effects, sets few values
  set_predefined_output_for_runtime_call(unlock, mem, TypeRawPtr::BOTTOM);

  // Kill monitor from debug info
  map()->pop_monitor( );
}

//-------------------------------get_layout_helper-----------------------------
// If the given klass is a constant or known to be an array,
// fetch the constant layout helper value into constant_value
// and return (Node*)NULL.  Otherwise, load the non-constant
// layout helper value, and return the node which represents it.
// This two-faced routine is useful because allocation sites
// almost always feature constant types.
Node* GraphKit::get_layout_helper(Node* klass_node, jint& constant_value) {
  const TypeKlassPtr* inst_klass = _gvn.type(klass_node)->isa_klassptr();
  if (!StressReflectiveCode && inst_klass != NULL) {
    ciKlass* klass = inst_klass->klass();
    bool    xklass = inst_klass->klass_is_exact();
    if (xklass || klass->is_array_klass()) {
      jint lhelper = klass->layout_helper();
      if (lhelper != Klass::_lh_neutral_value) {
        constant_value = lhelper;
        return (Node*) NULL;
      }
    }
  }
  constant_value = Klass::_lh_neutral_value;  // put in a known value
  Node* lhp = basic_plus_adr(klass_node, klass_node, in_bytes(Klass::layout_helper_offset()));
  return make_load(NULL, lhp, TypeInt::INT, T_INT, MemNode::unordered);
}

// We just put in an allocate/initialize with a big raw-memory effect.
// Hook selected additional alias categories on the initialization.
static void hook_memory_on_init(GraphKit& kit, int alias_idx,
                                MergeMemNode* init_in_merge,
                                Node* init_out_raw) {
  DEBUG_ONLY(Node* init_in_raw = init_in_merge->base_memory());
  assert(init_in_merge->memory_at(alias_idx) == init_in_raw, "");

  Node* prevmem = kit.memory(alias_idx);
  init_in_merge->set_memory_at(alias_idx, prevmem);
  kit.set_memory(init_out_raw, alias_idx);
}

//---------------------------set_output_for_allocation-------------------------
Node* GraphKit::set_output_for_allocation(AllocateNode* alloc,
                                          const TypeOopPtr* oop_type,
                                          bool deoptimize_on_exception) {
  int rawidx = Compile::AliasIdxRaw;
  alloc->set_req( TypeFunc::FramePtr, frameptr() );
  add_safepoint_edges(alloc);
  Node* allocx = _gvn.transform(alloc);
  set_control( _gvn.transform(new ProjNode(allocx, TypeFunc::Control) ) );
  // create memory projection for i_o
  set_memory ( _gvn.transform( new ProjNode(allocx, TypeFunc::Memory, true) ), rawidx );
  make_slow_call_ex(allocx, env()->Throwable_klass(), true, deoptimize_on_exception);

  // create a memory projection as for the normal control path
  Node* malloc = _gvn.transform(new ProjNode(allocx, TypeFunc::Memory));
  set_memory(malloc, rawidx);

  // a normal slow-call doesn't change i_o, but an allocation does
  // we create a separate i_o projection for the normal control path
  set_i_o(_gvn.transform( new ProjNode(allocx, TypeFunc::I_O, false) ) );
  Node* rawoop = _gvn.transform( new ProjNode(allocx, TypeFunc::Parms) );

  // put in an initialization barrier
  InitializeNode* init = insert_mem_bar_volatile(Op_Initialize, rawidx,
                                                 rawoop)->as_Initialize();
  assert(alloc->initialization() == init,  "2-way macro link must work");
  assert(init ->allocation()     == alloc, "2-way macro link must work");
  {
    // Extract memory strands which may participate in the new object's
    // initialization, and source them from the new InitializeNode.
    // This will allow us to observe initializations when they occur,
    // and link them properly (as a group) to the InitializeNode.
    assert(init->in(InitializeNode::Memory) == malloc, "");
    MergeMemNode* minit_in = MergeMemNode::make(malloc);
    init->set_req(InitializeNode::Memory, minit_in);
    record_for_igvn(minit_in); // fold it up later, if possible
    Node* minit_out = memory(rawidx);
    assert(minit_out->is_Proj() && minit_out->in(0) == init, "");
    // Add an edge in the MergeMem for the header fields so an access
    // to one of those has correct memory state
    set_memory(minit_out, C->get_alias_index(oop_type->add_offset(oopDesc::mark_offset_in_bytes())));
    set_memory(minit_out, C->get_alias_index(oop_type->add_offset(oopDesc::klass_offset_in_bytes())));
    if (oop_type->isa_aryptr()) {
      const TypePtr* telemref = oop_type->add_offset(Type::OffsetBot);
      int            elemidx  = C->get_alias_index(telemref);
      hook_memory_on_init(*this, elemidx, minit_in, minit_out);
    } else if (oop_type->isa_instptr()) {
      ciInstanceKlass* ik = oop_type->klass()->as_instance_klass();
      for (int i = 0, len = ik->nof_nonstatic_fields(); i < len; i++) {
        ciField* field = ik->nonstatic_field_at(i);
        if (field->offset() >= TrackedInitializationLimit * HeapWordSize)
          continue;  // do not bother to track really large numbers of fields
        // Find (or create) the alias category for this field:
        int fieldidx = C->alias_type(field)->index();
        hook_memory_on_init(*this, fieldidx, minit_in, minit_out);
      }
    }
  }

  // Cast raw oop to the real thing...
  Node* javaoop = new CheckCastPPNode(control(), rawoop, oop_type);
  javaoop = _gvn.transform(javaoop);
  C->set_recent_alloc(control(), javaoop);
  assert(just_allocated_object(control()) == javaoop, "just allocated");

#ifdef ASSERT
  { // Verify that the AllocateNode::Ideal_allocation recognizers work:
    assert(AllocateNode::Ideal_allocation(rawoop, &_gvn) == alloc,
           "Ideal_allocation works");
    assert(AllocateNode::Ideal_allocation(javaoop, &_gvn) == alloc,
           "Ideal_allocation works");
    if (alloc->is_AllocateArray()) {
      assert(AllocateArrayNode::Ideal_array_allocation(rawoop, &_gvn) == alloc->as_AllocateArray(),
             "Ideal_allocation works");
      assert(AllocateArrayNode::Ideal_array_allocation(javaoop, &_gvn) == alloc->as_AllocateArray(),
             "Ideal_allocation works");
    } else {
      assert(alloc->in(AllocateNode::ALength)->is_top(), "no length, please");
    }
  }
#endif //ASSERT

  return javaoop;
}

//---------------------------new_instance--------------------------------------
// This routine takes a klass_node which may be constant (for a static type)
// or may be non-constant (for reflective code).  It will work equally well
// for either, and the graph will fold nicely if the optimizer later reduces
// the type to a constant.
// The optional arguments are for specialized use by intrinsics:
//  - If 'extra_slow_test' if not null is an extra condition for the slow-path.
//  - If 'return_size_val', report the the total object size to the caller.
//  - deoptimize_on_exception controls how Java exceptions are handled (rethrow vs deoptimize)
Node* GraphKit::new_instance(Node* klass_node,
                             Node* extra_slow_test,
                             Node* *return_size_val,
                             bool deoptimize_on_exception) {
  // Compute size in doublewords
  // The size is always an integral number of doublewords, represented
  // as a positive bytewise size stored in the klass's layout_helper.
  // The layout_helper also encodes (in a low bit) the need for a slow path.
  jint  layout_con = Klass::_lh_neutral_value;
  Node* layout_val = get_layout_helper(klass_node, layout_con);
  int   layout_is_con = (layout_val == NULL);

  if (extra_slow_test == NULL)  extra_slow_test = intcon(0);
  // Generate the initial go-slow test.  It's either ALWAYS (return a
  // Node for 1) or NEVER (return a NULL) or perhaps (in the reflective
  // case) a computed value derived from the layout_helper.
  Node* initial_slow_test = NULL;
  if (layout_is_con) {
    assert(!StressReflectiveCode, "stress mode does not use these paths");
    bool must_go_slow = Klass::layout_helper_needs_slow_path(layout_con);
    initial_slow_test = must_go_slow ? intcon(1) : extra_slow_test;
  } else {   // reflective case
    // This reflective path is used by Unsafe.allocateInstance.
    // (It may be stress-tested by specifying StressReflectiveCode.)
    // Basically, we want to get into the VM is there's an illegal argument.
    Node* bit = intcon(Klass::_lh_instance_slow_path_bit);
    initial_slow_test = _gvn.transform( new AndINode(layout_val, bit) );
    if (extra_slow_test != intcon(0)) {
      initial_slow_test = _gvn.transform( new OrINode(initial_slow_test, extra_slow_test) );
    }
    // (Macro-expander will further convert this to a Bool, if necessary.)
  }

  // Find the size in bytes.  This is easy; it's the layout_helper.
  // The size value must be valid even if the slow path is taken.
  Node* size = NULL;
  if (layout_is_con) {
    size = MakeConX(Klass::layout_helper_size_in_bytes(layout_con));
  } else {   // reflective case
    // This reflective path is used by clone and Unsafe.allocateInstance.
    size = ConvI2X(layout_val);

    // Clear the low bits to extract layout_helper_size_in_bytes:
    assert((int)Klass::_lh_instance_slow_path_bit < BytesPerLong, "clear bit");
    Node* mask = MakeConX(~ (intptr_t)right_n_bits(LogBytesPerLong));
    size = _gvn.transform( new AndXNode(size, mask) );
  }
  if (return_size_val != NULL) {
    (*return_size_val) = size;
  }

  // This is a precise notnull oop of the klass.
  // (Actually, it need not be precise if this is a reflective allocation.)
  // It's what we cast the result to.
  const TypeKlassPtr* tklass = _gvn.type(klass_node)->isa_klassptr();
  if (!tklass)  tklass = TypeKlassPtr::OBJECT;
  const TypeOopPtr* oop_type = tklass->as_instance_type();

  // Now generate allocation code

  // The entire memory state is needed for slow path of the allocation
  // since GC and deoptimization can happened.
  Node *mem = reset_memory();
  set_all_memory(mem); // Create new memory state

  AllocateNode* alloc = new AllocateNode(C, AllocateNode::alloc_type(Type::TOP),
                                         control(), mem, i_o(),
                                         size, klass_node,
                                         initial_slow_test);

  return set_output_for_allocation(alloc, oop_type, deoptimize_on_exception);
}

//-------------------------------new_array-------------------------------------
// helper for both newarray and anewarray
// The 'length' parameter is (obviously) the length of the array.
// See comments on new_instance for the meaning of the other arguments.
Node* GraphKit::new_array(Node* klass_node,     // array klass (maybe variable)
                          Node* length,         // number of array elements
                          int   nargs,          // number of arguments to push back for uncommon trap
                          Node* *return_size_val,
                          bool deoptimize_on_exception) {
  jint  layout_con = Klass::_lh_neutral_value;
  Node* layout_val = get_layout_helper(klass_node, layout_con);
  int   layout_is_con = (layout_val == NULL);

  if (!layout_is_con && !StressReflectiveCode &&
      !too_many_traps(Deoptimization::Reason_class_check)) {
    // This is a reflective array creation site.
    // Optimistically assume that it is a subtype of Object[],
    // so that we can fold up all the address arithmetic.
    layout_con = Klass::array_layout_helper(T_OBJECT);
    Node* cmp_lh = _gvn.transform( new CmpINode(layout_val, intcon(layout_con)) );
    Node* bol_lh = _gvn.transform( new BoolNode(cmp_lh, BoolTest::eq) );
    { BuildCutout unless(this, bol_lh, PROB_MAX);
      inc_sp(nargs);
      uncommon_trap(Deoptimization::Reason_class_check,
                    Deoptimization::Action_maybe_recompile);
    }
    layout_val = NULL;
    layout_is_con = true;
  }

  // Generate the initial go-slow test.  Make sure we do not overflow
  // if length is huge (near 2Gig) or negative!  We do not need
  // exact double-words here, just a close approximation of needed
  // double-words.  We can't add any offset or rounding bits, lest we
  // take a size -1 of bytes and make it positive.  Use an unsigned
  // compare, so negative sizes look hugely positive.
  int fast_size_limit = FastAllocateSizeLimit;
  if (layout_is_con) {
    assert(!StressReflectiveCode, "stress mode does not use these paths");
    // Increase the size limit if we have exact knowledge of array type.
    int log2_esize = Klass::layout_helper_log2_element_size(layout_con);
    fast_size_limit <<= (LogBytesPerLong - log2_esize);
  }

  Node* initial_slow_cmp  = _gvn.transform( new CmpUNode( length, intcon( fast_size_limit ) ) );
  Node* initial_slow_test = _gvn.transform( new BoolNode( initial_slow_cmp, BoolTest::gt ) );

  // --- Size Computation ---
  // array_size = round_to_heap(array_header + (length << elem_shift));
  // where round_to_heap(x) == align_to(x, MinObjAlignmentInBytes)
  // and align_to(x, y) == ((x + y-1) & ~(y-1))
  // The rounding mask is strength-reduced, if possible.
  int round_mask = MinObjAlignmentInBytes - 1;
  Node* header_size = NULL;
  int   header_size_min  = arrayOopDesc::base_offset_in_bytes(T_BYTE);
  // (T_BYTE has the weakest alignment and size restrictions...)
  if (layout_is_con) {
    int       hsize  = Klass::layout_helper_header_size(layout_con);
    int       eshift = Klass::layout_helper_log2_element_size(layout_con);
    BasicType etype  = Klass::layout_helper_element_type(layout_con);
    if ((round_mask & ~right_n_bits(eshift)) == 0)
      round_mask = 0;  // strength-reduce it if it goes away completely
    assert((hsize & right_n_bits(eshift)) == 0, "hsize is pre-rounded");
    assert(header_size_min <= hsize, "generic minimum is smallest");
    header_size_min = hsize;
    header_size = intcon(hsize + round_mask);
  } else {
    Node* hss   = intcon(Klass::_lh_header_size_shift);
    Node* hsm   = intcon(Klass::_lh_header_size_mask);
    Node* hsize = _gvn.transform( new URShiftINode(layout_val, hss) );
    hsize       = _gvn.transform( new AndINode(hsize, hsm) );
    Node* mask  = intcon(round_mask);
    header_size = _gvn.transform( new AddINode(hsize, mask) );
  }

  Node* elem_shift = NULL;
  if (layout_is_con) {
    int eshift = Klass::layout_helper_log2_element_size(layout_con);
    if (eshift != 0)
      elem_shift = intcon(eshift);
  } else {
    // There is no need to mask or shift this value.
    // The semantics of LShiftINode include an implicit mask to 0x1F.
    assert(Klass::_lh_log2_element_size_shift == 0, "use shift in place");
    elem_shift = layout_val;
  }

  // Transition to native address size for all offset calculations:
  Node* lengthx = ConvI2X(length);
  Node* headerx = ConvI2X(header_size);
#ifdef _LP64
  { const TypeInt* tilen = _gvn.find_int_type(length);
    if (tilen != NULL && tilen->_lo < 0) {
      // Add a manual constraint to a positive range.  Cf. array_element_address.
      jint size_max = fast_size_limit;
      if (size_max > tilen->_hi)  size_max = tilen->_hi;
      const TypeInt* tlcon = TypeInt::make(0, size_max, Type::WidenMin);

      // Only do a narrow I2L conversion if the range check passed.
      IfNode* iff = new IfNode(control(), initial_slow_test, PROB_MIN, COUNT_UNKNOWN);
      _gvn.transform(iff);
      RegionNode* region = new RegionNode(3);
      _gvn.set_type(region, Type::CONTROL);
      lengthx = new PhiNode(region, TypeLong::LONG);
      _gvn.set_type(lengthx, TypeLong::LONG);

      // Range check passed. Use ConvI2L node with narrow type.
      Node* passed = IfFalse(iff);
      region->init_req(1, passed);
      // Make I2L conversion control dependent to prevent it from
      // floating above the range check during loop optimizations.
      lengthx->init_req(1, C->constrained_convI2L(&_gvn, length, tlcon, passed));

      // Range check failed. Use ConvI2L with wide type because length may be invalid.
      region->init_req(2, IfTrue(iff));
      lengthx->init_req(2, ConvI2X(length));

      set_control(region);
      record_for_igvn(region);
      record_for_igvn(lengthx);
    }
  }
#endif

  // Combine header size (plus rounding) and body size.  Then round down.
  // This computation cannot overflow, because it is used only in two
  // places, one where the length is sharply limited, and the other
  // after a successful allocation.
  Node* abody = lengthx;
  if (elem_shift != NULL)
    abody     = _gvn.transform( new LShiftXNode(lengthx, elem_shift) );
  Node* size  = _gvn.transform( new AddXNode(headerx, abody) );
  if (round_mask != 0) {
    Node* mask = MakeConX(~round_mask);
    size       = _gvn.transform( new AndXNode(size, mask) );
  }
  // else if round_mask == 0, the size computation is self-rounding

  if (return_size_val != NULL) {
    // This is the size
    (*return_size_val) = size;
  }

  // Now generate allocation code

  // The entire memory state is needed for slow path of the allocation
  // since GC and deoptimization can happened.
  Node *mem = reset_memory();
  set_all_memory(mem); // Create new memory state

  if (initial_slow_test->is_Bool()) {
    // Hide it behind a CMoveI, or else PhaseIdealLoop::split_up will get sick.
    initial_slow_test = initial_slow_test->as_Bool()->as_int_value(&_gvn);
  }

  // Create the AllocateArrayNode and its result projections
  AllocateArrayNode* alloc
    = new AllocateArrayNode(C, AllocateArrayNode::alloc_type(TypeInt::INT),
                            control(), mem, i_o(),
                            size, klass_node,
                            initial_slow_test,
                            length);

  // Cast to correct type.  Note that the klass_node may be constant or not,
  // and in the latter case the actual array type will be inexact also.
  // (This happens via a non-constant argument to inline_native_newArray.)
  // In any case, the value of klass_node provides the desired array type.
  const TypeInt* length_type = _gvn.find_int_type(length);
  const TypeOopPtr* ary_type = _gvn.type(klass_node)->is_klassptr()->as_instance_type();
  if (ary_type->isa_aryptr() && length_type != NULL) {
    // Try to get a better type than POS for the size
    ary_type = ary_type->is_aryptr()->cast_to_size(length_type);
  }

  Node* javaoop = set_output_for_allocation(alloc, ary_type, deoptimize_on_exception);

  array_ideal_length(alloc, ary_type, true);
  return javaoop;
}

// The following "Ideal_foo" functions are placed here because they recognize
// the graph shapes created by the functions immediately above.

//---------------------------Ideal_allocation----------------------------------
// Given an oop pointer or raw pointer, see if it feeds from an AllocateNode.
AllocateNode* AllocateNode::Ideal_allocation(Node* ptr, PhaseTransform* phase) {
  if (ptr == NULL) {     // reduce dumb test in callers
    return NULL;
  }

  BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
  ptr = bs->step_over_gc_barrier(ptr);

  if (ptr->is_CheckCastPP()) { // strip only one raw-to-oop cast
    ptr = ptr->in(1);
    if (ptr == NULL) return NULL;
  }
  // Return NULL for allocations with several casts:
  //   j.l.reflect.Array.newInstance(jobject, jint)
  //   Object.clone()
  // to keep more precise type from last cast.
  if (ptr->is_Proj()) {
    Node* allo = ptr->in(0);
    if (allo != NULL && allo->is_Allocate()) {
      return allo->as_Allocate();
    }
  }
  // Report failure to match.
  return NULL;
}

// Fancy version which also strips off an offset (and reports it to caller).
AllocateNode* AllocateNode::Ideal_allocation(Node* ptr, PhaseTransform* phase,
                                             intptr_t& offset) {
  Node* base = AddPNode::Ideal_base_and_offset(ptr, phase, offset);
  if (base == NULL)  return NULL;
  return Ideal_allocation(base, phase);
}

// Trace Initialize <- Proj[Parm] <- Allocate
AllocateNode* InitializeNode::allocation() {
  Node* rawoop = in(InitializeNode::RawAddress);
  if (rawoop->is_Proj()) {
    Node* alloc = rawoop->in(0);
    if (alloc->is_Allocate()) {
      return alloc->as_Allocate();
    }
  }
  return NULL;
}

// Trace Allocate -> Proj[Parm] -> Initialize
InitializeNode* AllocateNode::initialization() {
  ProjNode* rawoop = proj_out_or_null(AllocateNode::RawAddress);
  if (rawoop == NULL)  return NULL;
  for (DUIterator_Fast imax, i = rawoop->fast_outs(imax); i < imax; i++) {
    Node* init = rawoop->fast_out(i);
    if (init->is_Initialize()) {
      assert(init->as_Initialize()->allocation() == this, "2-way link");
      return init->as_Initialize();
    }
  }
  return NULL;
}

//----------------------------- loop predicates ---------------------------

//------------------------------add_predicate_impl----------------------------
void GraphKit::add_empty_predicate_impl(Deoptimization::DeoptReason reason, int nargs) {
  // Too many traps seen?
  if (too_many_traps(reason)) {
#ifdef ASSERT
    if (TraceLoopPredicate) {
      int tc = C->trap_count(reason);
      tty->print("too many traps=%s tcount=%d in ",
                    Deoptimization::trap_reason_name(reason), tc);
      method()->print(); // which method has too many predicate traps
      tty->cr();
    }
#endif
    // We cannot afford to take more traps here,
    // do not generate predicate.
    return;
  }

  Node *cont    = _gvn.intcon(1);
  Node* opq     = _gvn.transform(new Opaque1Node(C, cont));
  Node *bol     = _gvn.transform(new Conv2BNode(opq));
  IfNode* iff   = create_and_map_if(control(), bol, PROB_MAX, COUNT_UNKNOWN);
  Node* iffalse = _gvn.transform(new IfFalseNode(iff));
  C->add_predicate_opaq(opq);
  {
    PreserveJVMState pjvms(this);
    set_control(iffalse);
    inc_sp(nargs);
    uncommon_trap(reason, Deoptimization::Action_maybe_recompile);
  }
  Node* iftrue = _gvn.transform(new IfTrueNode(iff));
  set_control(iftrue);
}

//------------------------------add_predicate---------------------------------
void GraphKit::add_empty_predicates(int nargs) {
  // These loop predicates remain empty. All concrete loop predicates are inserted above the corresponding
  // empty loop predicate later by 'PhaseIdealLoop::create_new_if_for_predicate'. All concrete loop predicates of
  // a specific kind (normal, profile or limit check) share the same uncommon trap as the empty loop predicate.
  if (UseLoopPredicate) {
    add_empty_predicate_impl(Deoptimization::Reason_predicate, nargs);
  }
  if (UseProfiledLoopPredicate) {
    add_empty_predicate_impl(Deoptimization::Reason_profile_predicate, nargs);
  }
  // loop's limit check predicate should be near the loop.
  add_empty_predicate_impl(Deoptimization::Reason_loop_limit_check, nargs);
}

void GraphKit::sync_kit(IdealKit& ideal) {
  set_all_memory(ideal.merged_memory());
  set_i_o(ideal.i_o());
  set_control(ideal.ctrl());
}

void GraphKit::final_sync(IdealKit& ideal) {
  // Final sync IdealKit and graphKit.
  sync_kit(ideal);
}

Node* GraphKit::load_String_length(Node* str, bool set_ctrl) {
  Node* len = load_array_length(load_String_value(str, set_ctrl));
  Node* coder = load_String_coder(str, set_ctrl);
  // Divide length by 2 if coder is UTF16
  return _gvn.transform(new RShiftINode(len, coder));
}

Node* GraphKit::load_String_value(Node* str, bool set_ctrl) {
  int value_offset = java_lang_String::value_offset();
  const TypeInstPtr* string_type = TypeInstPtr::make(TypePtr::NotNull, C->env()->String_klass(),
                                                     false, NULL, 0);
  const TypePtr* value_field_type = string_type->add_offset(value_offset);
  const TypeAryPtr* value_type = TypeAryPtr::make(TypePtr::NotNull,
                                                  TypeAry::make(TypeInt::BYTE, TypeInt::POS),
                                                  ciTypeArrayKlass::make(T_BYTE), true, 0);
  Node* p = basic_plus_adr(str, str, value_offset);
  Node* load = access_load_at(str, p, value_field_type, value_type, T_OBJECT,
                              IN_HEAP | (set_ctrl ? C2_CONTROL_DEPENDENT_LOAD : 0) | MO_UNORDERED);
  return load;
}

Node* GraphKit::load_String_coder(Node* str, bool set_ctrl) {
  if (!CompactStrings) {
    return intcon(java_lang_String::CODER_UTF16);
  }
  int coder_offset = java_lang_String::coder_offset();
  const TypeInstPtr* string_type = TypeInstPtr::make(TypePtr::NotNull, C->env()->String_klass(),
                                                     false, NULL, 0);
  const TypePtr* coder_field_type = string_type->add_offset(coder_offset);

  Node* p = basic_plus_adr(str, str, coder_offset);
  Node* load = access_load_at(str, p, coder_field_type, TypeInt::BYTE, T_BYTE,
                              IN_HEAP | (set_ctrl ? C2_CONTROL_DEPENDENT_LOAD : 0) | MO_UNORDERED);
  return load;
}

void GraphKit::store_String_value(Node* str, Node* value) {
  int value_offset = java_lang_String::value_offset();
  const TypeInstPtr* string_type = TypeInstPtr::make(TypePtr::NotNull, C->env()->String_klass(),
                                                     false, NULL, 0);
  const TypePtr* value_field_type = string_type->add_offset(value_offset);

  access_store_at(str,  basic_plus_adr(str, value_offset), value_field_type,
                  value, TypeAryPtr::BYTES, T_OBJECT, IN_HEAP | MO_UNORDERED);
}

void GraphKit::store_String_coder(Node* str, Node* value) {
  int coder_offset = java_lang_String::coder_offset();
  const TypeInstPtr* string_type = TypeInstPtr::make(TypePtr::NotNull, C->env()->String_klass(),
                                                     false, NULL, 0);
  const TypePtr* coder_field_type = string_type->add_offset(coder_offset);

  access_store_at(str, basic_plus_adr(str, coder_offset), coder_field_type,
                  value, TypeInt::BYTE, T_BYTE, IN_HEAP | MO_UNORDERED);
}

// Capture src and dst memory state with a MergeMemNode
Node* GraphKit::capture_memory(const TypePtr* src_type, const TypePtr* dst_type) {
  if (src_type == dst_type) {
    // Types are equal, we don't need a MergeMemNode
    return memory(src_type);
  }
  MergeMemNode* merge = MergeMemNode::make(map()->memory());
  record_for_igvn(merge); // fold it up later, if possible
  int src_idx = C->get_alias_index(src_type);
  int dst_idx = C->get_alias_index(dst_type);
  merge->set_memory_at(src_idx, memory(src_idx));
  merge->set_memory_at(dst_idx, memory(dst_idx));
  return merge;
}

Node* GraphKit::compress_string(Node* src, const TypeAryPtr* src_type, Node* dst, Node* count) {
  assert(Matcher::match_rule_supported(Op_StrCompressedCopy), "Intrinsic not supported");
  assert(src_type == TypeAryPtr::BYTES || src_type == TypeAryPtr::CHARS, "invalid source type");
  // If input and output memory types differ, capture both states to preserve
  // the dependency between preceding and subsequent loads/stores.
  // For example, the following program:
  //  StoreB
  //  compress_string
  //  LoadB
  // has this memory graph (use->def):
  //  LoadB -> compress_string -> CharMem
  //             ... -> StoreB -> ByteMem
  // The intrinsic hides the dependency between LoadB and StoreB, causing
  // the load to read from memory not containing the result of the StoreB.
  // The correct memory graph should look like this:
  //  LoadB -> compress_string -> MergeMem(CharMem, StoreB(ByteMem))
  Node* mem = capture_memory(src_type, TypeAryPtr::BYTES);
  StrCompressedCopyNode* str = new StrCompressedCopyNode(control(), mem, src, dst, count);
  Node* res_mem = _gvn.transform(new SCMemProjNode(_gvn.transform(str)));
  set_memory(res_mem, TypeAryPtr::BYTES);
  return str;
}

void GraphKit::inflate_string(Node* src, Node* dst, const TypeAryPtr* dst_type, Node* count) {
  assert(Matcher::match_rule_supported(Op_StrInflatedCopy), "Intrinsic not supported");
  assert(dst_type == TypeAryPtr::BYTES || dst_type == TypeAryPtr::CHARS, "invalid dest type");
  // Capture src and dst memory (see comment in 'compress_string').
  Node* mem = capture_memory(TypeAryPtr::BYTES, dst_type);
  StrInflatedCopyNode* str = new StrInflatedCopyNode(control(), mem, src, dst, count);
  set_memory(_gvn.transform(str), dst_type);
}

void GraphKit::inflate_string_slow(Node* src, Node* dst, Node* start, Node* count) {
  /**
   * int i_char = start;
   * for (int i_byte = 0; i_byte < count; i_byte++) {
   *   dst[i_char++] = (char)(src[i_byte] & 0xff);
   * }
   */
  add_empty_predicates();
  C->set_has_loops(true);

  RegionNode* head = new RegionNode(3);
  head->init_req(1, control());
  gvn().set_type(head, Type::CONTROL);
  record_for_igvn(head);

  Node* i_byte = new PhiNode(head, TypeInt::INT);
  i_byte->init_req(1, intcon(0));
  gvn().set_type(i_byte, TypeInt::INT);
  record_for_igvn(i_byte);

  Node* i_char = new PhiNode(head, TypeInt::INT);
  i_char->init_req(1, start);
  gvn().set_type(i_char, TypeInt::INT);
  record_for_igvn(i_char);

  Node* mem = PhiNode::make(head, memory(TypeAryPtr::BYTES), Type::MEMORY, TypeAryPtr::BYTES);
  gvn().set_type(mem, Type::MEMORY);
  record_for_igvn(mem);
  set_control(head);
  set_memory(mem, TypeAryPtr::BYTES);
  Node* ch = load_array_element(control(), src, i_byte, TypeAryPtr::BYTES);
  Node* st = store_to_memory(control(), array_element_address(dst, i_char, T_BYTE),
                             AndI(ch, intcon(0xff)), T_CHAR, TypeAryPtr::BYTES, MemNode::unordered,
                             false, false, true /* mismatched */);

  IfNode* iff = create_and_map_if(head, Bool(CmpI(i_byte, count), BoolTest::lt), PROB_FAIR, COUNT_UNKNOWN);
  head->init_req(2, IfTrue(iff));
  mem->init_req(2, st);
  i_byte->init_req(2, AddI(i_byte, intcon(1)));
  i_char->init_req(2, AddI(i_char, intcon(2)));

  set_control(IfFalse(iff));
  set_memory(st, TypeAryPtr::BYTES);
}

Node* GraphKit::make_constant_from_field(ciField* field, Node* obj) {
  if (!field->is_constant()) {
    return NULL; // Field not marked as constant.
  }
  ciInstance* holder = NULL;
  if (!field->is_static()) {
    ciObject* const_oop = obj->bottom_type()->is_oopptr()->const_oop();
    if (const_oop != NULL && const_oop->is_instance()) {
      holder = const_oop->as_instance();
    }
  }
  const Type* con_type = Type::make_constant_from_field(field, holder, field->layout_type(),
                                                        /*is_unsigned_load=*/false);
  if (con_type != NULL) {
    return makecon(con_type);
  }
  return NULL;
}
