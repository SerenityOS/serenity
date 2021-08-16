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

#ifndef SHARE_OPTO_GRAPHKIT_HPP
#define SHARE_OPTO_GRAPHKIT_HPP

#include "ci/ciEnv.hpp"
#include "ci/ciMethodData.hpp"
#include "gc/shared/c2/barrierSetC2.hpp"
#include "opto/addnode.hpp"
#include "opto/callnode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/compile.hpp"
#include "opto/divnode.hpp"
#include "opto/mulnode.hpp"
#include "opto/phaseX.hpp"
#include "opto/subnode.hpp"
#include "opto/type.hpp"
#include "runtime/deoptimization.hpp"

class BarrierSetC2;
class FastLockNode;
class FastUnlockNode;
class IdealKit;
class LibraryCallKit;
class Parse;
class RootNode;

//-----------------------------------------------------------------------------
//----------------------------GraphKit-----------------------------------------
// Toolkit for building the common sorts of subgraphs.
// Does not know about bytecode parsing or type-flow results.
// It is able to create graphs implementing the semantics of most
// or all bytecodes, so that it can expand intrinsics and calls.
// It may depend on JVMState structure, but it must not depend
// on specific bytecode streams.
class GraphKit : public Phase {
  friend class PreserveJVMState;

 protected:
  ciEnv*            _env;       // Compilation environment
  PhaseGVN         &_gvn;       // Some optimizations while parsing
  SafePointNode*    _map;       // Parser map from JVM to Nodes
  SafePointNode*    _exceptions;// Parser map(s) for exception state(s)
  int               _bci;       // JVM Bytecode Pointer
  ciMethod*         _method;    // JVM Current Method
  BarrierSetC2*     _barrier_set;

 private:
  int               _sp;        // JVM Expression Stack Pointer; don't modify directly!

 private:
  SafePointNode*     map_not_null() const {
    assert(_map != NULL, "must call stopped() to test for reset compiler map");
    return _map;
  }

 public:
  GraphKit();                   // empty constructor
  GraphKit(JVMState* jvms);     // the JVM state on which to operate

#ifdef ASSERT
  ~GraphKit() {
    assert(!has_exceptions(), "user must call transfer_exceptions_into_jvms");
  }
#endif

  virtual Parse*          is_Parse()          const { return NULL; }
  virtual LibraryCallKit* is_LibraryCallKit() const { return NULL; }

  ciEnv*        env()               const { return _env; }
  PhaseGVN&     gvn()               const { return _gvn; }
  void*         barrier_set_state() const { return C->barrier_set_state(); }

  void record_for_igvn(Node* n) const { C->record_for_igvn(n); }  // delegate to Compile

  // Handy well-known nodes:
  Node*         null()          const { return zerocon(T_OBJECT); }
  Node*         top()           const { return C->top(); }
  RootNode*     root()          const { return C->root(); }

  // Create or find a constant node
  Node* intcon(jint con)        const { return _gvn.intcon(con); }
  Node* longcon(jlong con)      const { return _gvn.longcon(con); }
  Node* integercon(jlong con, BasicType bt)   const {
    if (bt == T_INT) {
      return intcon(checked_cast<jint>(con));
    }
    assert(bt == T_LONG, "basic type not an int or long");
    return longcon(con);
  }
  Node* makecon(const Type *t)  const { return _gvn.makecon(t); }
  Node* zerocon(BasicType bt)   const { return _gvn.zerocon(bt); }
  // (See also macro MakeConX in type.hpp, which uses intcon or longcon.)

  jint  find_int_con(Node* n, jint value_if_unknown) {
    return _gvn.find_int_con(n, value_if_unknown);
  }
  jlong find_long_con(Node* n, jlong value_if_unknown) {
    return _gvn.find_long_con(n, value_if_unknown);
  }
  // (See also macro find_intptr_t_con in type.hpp, which uses one of these.)

  // JVM State accessors:
  // Parser mapping from JVM indices into Nodes.
  // Low slots are accessed by the StartNode::enum.
  // Then come the locals at StartNode::Parms to StartNode::Parms+max_locals();
  // Then come JVM stack slots.
  // Finally come the monitors, if any.
  // See layout accessors in class JVMState.

  SafePointNode*     map()      const { return _map; }
  bool               has_exceptions() const { return _exceptions != NULL; }
  JVMState*          jvms()     const { return map_not_null()->_jvms; }
  int                sp()       const { return _sp; }
  int                bci()      const { return _bci; }
  Bytecodes::Code    java_bc()  const;
  ciMethod*          method()   const { return _method; }

  void set_jvms(JVMState* jvms)       { set_map(jvms->map());
                                        assert(jvms == this->jvms(), "sanity");
                                        _sp = jvms->sp();
                                        _bci = jvms->bci();
                                        _method = jvms->has_method() ? jvms->method() : NULL; }
  void set_map(SafePointNode* m)      { _map = m; debug_only(verify_map()); }
  void set_sp(int sp)                 { assert(sp >= 0, "sp must be non-negative: %d", sp); _sp = sp; }
  void clean_stack(int from_sp); // clear garbage beyond from_sp to top

  void inc_sp(int i)                  { set_sp(sp() + i); }
  void dec_sp(int i)                  { set_sp(sp() - i); }
  void set_bci(int bci)               { _bci = bci; }

  // Make sure jvms has current bci & sp.
  JVMState* sync_jvms() const;
  JVMState* sync_jvms_for_reexecute();

#ifdef ASSERT
  // Make sure JVMS has an updated copy of bci and sp.
  // Also sanity-check method, depth, and monitor depth.
  bool jvms_in_sync() const;

  // Make sure the map looks OK.
  void verify_map() const;

  // Make sure a proposed exception state looks OK.
  static void verify_exception_state(SafePointNode* ex_map);
#endif

  // Clone the existing map state.  (Implements PreserveJVMState.)
  SafePointNode* clone_map();

  // Set the map to a clone of the given one.
  void set_map_clone(SafePointNode* m);

  // Tell if the compilation is failing.
  bool failing() const { return C->failing(); }

  // Set _map to NULL, signalling a stop to further bytecode execution.
  // Preserve the map intact for future use, and return it back to the caller.
  SafePointNode* stop() { SafePointNode* m = map(); set_map(NULL); return m; }

  // Stop, but first smash the map's inputs to NULL, to mark it dead.
  void stop_and_kill_map();

  // Tell if _map is NULL, or control is top.
  bool stopped();

  // Tell if this method or any caller method has exception handlers.
  bool has_ex_handler();

  // Save an exception without blowing stack contents or other JVM state.
  // (The extra pointer is stuck with add_req on the map, beyond the JVMS.)
  static void set_saved_ex_oop(SafePointNode* ex_map, Node* ex_oop);

  // Recover a saved exception from its map.
  static Node* saved_ex_oop(SafePointNode* ex_map);

  // Recover a saved exception from its map, and remove it from the map.
  static Node* clear_saved_ex_oop(SafePointNode* ex_map);

#ifdef ASSERT
  // Recover a saved exception from its map, and remove it from the map.
  static bool has_saved_ex_oop(SafePointNode* ex_map);
#endif

  // Push an exception in the canonical position for handlers (stack(0)).
  void push_ex_oop(Node* ex_oop) {
    ensure_stack(1);  // ensure room to push the exception
    set_stack(0, ex_oop);
    set_sp(1);
    clean_stack(1);
  }

  // Detach and return an exception state.
  SafePointNode* pop_exception_state() {
    SafePointNode* ex_map = _exceptions;
    if (ex_map != NULL) {
      _exceptions = ex_map->next_exception();
      ex_map->set_next_exception(NULL);
      debug_only(verify_exception_state(ex_map));
    }
    return ex_map;
  }

  // Add an exception, using the given JVM state, without commoning.
  void push_exception_state(SafePointNode* ex_map) {
    debug_only(verify_exception_state(ex_map));
    ex_map->set_next_exception(_exceptions);
    _exceptions = ex_map;
  }

  // Turn the current JVM state into an exception state, appending the ex_oop.
  SafePointNode* make_exception_state(Node* ex_oop);

  // Add an exception, using the given JVM state.
  // Combine all exceptions with a common exception type into a single state.
  // (This is done via combine_exception_states.)
  void add_exception_state(SafePointNode* ex_map);

  // Combine all exceptions of any sort whatever into a single master state.
  SafePointNode* combine_and_pop_all_exception_states() {
    if (_exceptions == NULL)  return NULL;
    SafePointNode* phi_map = pop_exception_state();
    SafePointNode* ex_map;
    while ((ex_map = pop_exception_state()) != NULL) {
      combine_exception_states(ex_map, phi_map);
    }
    return phi_map;
  }

  // Combine the two exception states, building phis as necessary.
  // The second argument is updated to include contributions from the first.
  void combine_exception_states(SafePointNode* ex_map, SafePointNode* phi_map);

  // Reset the map to the given state.  If there are any half-finished phis
  // in it (created by combine_exception_states), transform them now.
  // Returns the exception oop.  (Caller must call push_ex_oop if required.)
  Node* use_exception_state(SafePointNode* ex_map);

  // Collect exceptions from a given JVM state into my exception list.
  void add_exception_states_from(JVMState* jvms);

  // Collect all raised exceptions into the current JVM state.
  // Clear the current exception list and map, returns the combined states.
  JVMState* transfer_exceptions_into_jvms();

  // Helper to throw a built-in exception.
  // Range checks take the offending index.
  // Cast and array store checks take the offending class.
  // Others do not take the optional argument.
  // The JVMS must allow the bytecode to be re-executed
  // via an uncommon trap.
  void builtin_throw(Deoptimization::DeoptReason reason, Node* arg = NULL);

  // Helper to check the JavaThread::_should_post_on_exceptions flag
  // and branch to an uncommon_trap if it is true (with the specified reason and must_throw)
  void uncommon_trap_if_should_post_on_exceptions(Deoptimization::DeoptReason reason,
                                                  bool must_throw) ;

  // Helper Functions for adding debug information
  void kill_dead_locals();
#ifdef ASSERT
  bool dead_locals_are_killed();
#endif
  // The call may deoptimize.  Supply required JVM state as debug info.
  // If must_throw is true, the call is guaranteed not to return normally.
  void add_safepoint_edges(SafePointNode* call,
                           bool must_throw = false);

  // How many stack inputs does the current BC consume?
  // And, how does the stack change after the bytecode?
  // Returns false if unknown.
  bool compute_stack_effects(int& inputs, int& depth);

  // Add a fixed offset to a pointer
  Node* basic_plus_adr(Node* base, Node* ptr, intptr_t offset) {
    return basic_plus_adr(base, ptr, MakeConX(offset));
  }
  Node* basic_plus_adr(Node* base, intptr_t offset) {
    return basic_plus_adr(base, base, MakeConX(offset));
  }
  // Add a variable offset to a pointer
  Node* basic_plus_adr(Node* base, Node* offset) {
    return basic_plus_adr(base, base, offset);
  }
  Node* basic_plus_adr(Node* base, Node* ptr, Node* offset);


  // Some convenient shortcuts for common nodes
  Node* IfTrue(IfNode* iff)                   { return _gvn.transform(new IfTrueNode(iff));      }
  Node* IfFalse(IfNode* iff)                  { return _gvn.transform(new IfFalseNode(iff));     }

  Node* AddI(Node* l, Node* r)                { return _gvn.transform(new AddINode(l, r));       }
  Node* SubI(Node* l, Node* r)                { return _gvn.transform(new SubINode(l, r));       }
  Node* MulI(Node* l, Node* r)                { return _gvn.transform(new MulINode(l, r));       }
  Node* DivI(Node* ctl, Node* l, Node* r)     { return _gvn.transform(new DivINode(ctl, l, r));  }

  Node* AndI(Node* l, Node* r)                { return _gvn.transform(new AndINode(l, r));       }
  Node* OrI(Node* l, Node* r)                 { return _gvn.transform(new OrINode(l, r));        }
  Node* XorI(Node* l, Node* r)                { return _gvn.transform(new XorINode(l, r));       }

  Node* MaxI(Node* l, Node* r)                { return _gvn.transform(new MaxINode(l, r));       }
  Node* MinI(Node* l, Node* r)                { return _gvn.transform(new MinINode(l, r));       }

  Node* LShiftI(Node* l, Node* r)             { return _gvn.transform(new LShiftINode(l, r));    }
  Node* RShiftI(Node* l, Node* r)             { return _gvn.transform(new RShiftINode(l, r));    }
  Node* URShiftI(Node* l, Node* r)            { return _gvn.transform(new URShiftINode(l, r));   }

  Node* CmpI(Node* l, Node* r)                { return _gvn.transform(new CmpINode(l, r));       }
  Node* CmpL(Node* l, Node* r)                { return _gvn.transform(new CmpLNode(l, r));       }
  Node* CmpP(Node* l, Node* r)                { return _gvn.transform(new CmpPNode(l, r));       }
  Node* Bool(Node* cmp, BoolTest::mask relop) { return _gvn.transform(new BoolNode(cmp, relop)); }

  Node* AddP(Node* b, Node* a, Node* o)       { return _gvn.transform(new AddPNode(b, a, o));    }

  // Convert between int and long, and size_t.
  // (See macros ConvI2X, etc., in type.hpp for ConvI2X, etc.)
  Node* ConvI2L(Node* offset);
  Node* ConvI2UL(Node* offset);
  Node* ConvL2I(Node* offset);
  // Find out the klass of an object.
  Node* load_object_klass(Node* object);
  // Find out the length of an array.
  Node* load_array_length(Node* array);
  // Cast array allocation's length as narrow as possible.
  // If replace_length_in_map is true, replace length with CastIINode in map.
  // This method is invoked after creating/moving ArrayAllocationNode or in load_array_length
  Node* array_ideal_length(AllocateArrayNode* alloc,
                           const TypeOopPtr* oop_type,
                           bool replace_length_in_map);


  // Helper function to do a NULL pointer check or ZERO check based on type.
  // Throw an exception if a given value is null.
  // Return the value cast to not-null.
  // Be clever about equivalent dominating null checks.
  Node* null_check_common(Node* value, BasicType type,
                          bool assert_null = false,
                          Node* *null_control = NULL,
                          bool speculative = false);
  Node* null_check(Node* value, BasicType type = T_OBJECT) {
    return null_check_common(value, type, false, NULL, !_gvn.type(value)->speculative_maybe_null());
  }
  Node* null_check_receiver() {
    assert(argument(0)->bottom_type()->isa_ptr(), "must be");
    return null_check(argument(0));
  }
  Node* zero_check_int(Node* value) {
    assert(value->bottom_type()->basic_type() == T_INT,
           "wrong type: %s", type2name(value->bottom_type()->basic_type()));
    return null_check_common(value, T_INT);
  }
  Node* zero_check_long(Node* value) {
    assert(value->bottom_type()->basic_type() == T_LONG,
           "wrong type: %s", type2name(value->bottom_type()->basic_type()));
    return null_check_common(value, T_LONG);
  }
  // Throw an uncommon trap if a given value is __not__ null.
  // Return the value cast to null, and be clever about dominating checks.
  Node* null_assert(Node* value, BasicType type = T_OBJECT) {
    return null_check_common(value, type, true, NULL, _gvn.type(value)->speculative_always_null());
  }

  // Check if value is null and abort if it is
  Node* must_be_not_null(Node* value, bool do_replace_in_map);

  // Null check oop.  Return null-path control into (*null_control).
  // Return a cast-not-null node which depends on the not-null control.
  // If never_see_null, use an uncommon trap (*null_control sees a top).
  // The cast is not valid along the null path; keep a copy of the original.
  // If safe_for_replace, then we can replace the value with the cast
  // in the parsing map (the cast is guaranteed to dominate the map)
  Node* null_check_oop(Node* value, Node* *null_control,
                       bool never_see_null = false,
                       bool safe_for_replace = false,
                       bool speculative = false);

  // Check the null_seen bit.
  bool seems_never_null(Node* obj, ciProfileData* data, bool& speculating);

  void guard_klass_being_initialized(Node* klass);
  void guard_init_thread(Node* klass);

  void clinit_barrier(ciInstanceKlass* ik, ciMethod* context);

  // Check for unique class for receiver at call
  ciKlass* profile_has_unique_klass() {
    ciCallProfile profile = method()->call_profile_at_bci(bci());
    if (profile.count() >= 0 &&         // no cast failures here
        profile.has_receiver(0) &&
        profile.morphism() == 1) {
      return profile.receiver(0);
    }
    return NULL;
  }

  // record type from profiling with the type system
  Node* record_profile_for_speculation(Node* n, ciKlass* exact_kls, ProfilePtrKind ptr_kind);
  void record_profiled_arguments_for_speculation(ciMethod* dest_method, Bytecodes::Code bc);
  void record_profiled_parameters_for_speculation();
  void record_profiled_return_for_speculation();
  Node* record_profiled_receiver_for_speculation(Node* n);

  // Use the type profile to narrow an object type.
  Node* maybe_cast_profiled_receiver(Node* not_null_obj,
                                     ciKlass* require_klass,
                                     ciKlass* spec,
                                     bool safe_for_replace);

  // Cast obj to type and emit guard unless we had too many traps here already
  Node* maybe_cast_profiled_obj(Node* obj,
                                ciKlass* type,
                                bool not_null = false);

  // Cast obj to not-null on this path
  Node* cast_not_null(Node* obj, bool do_replace_in_map = true);
  // Replace all occurrences of one node by another.
  void replace_in_map(Node* old, Node* neww);

  void  push(Node* n)     { map_not_null();        _map->set_stack(_map->_jvms,   _sp++        , n); }
  Node* pop()             { map_not_null(); return _map->stack(    _map->_jvms, --_sp             ); }
  Node* peek(int off = 0) { map_not_null(); return _map->stack(    _map->_jvms,   _sp - off - 1   ); }

  void push_pair(Node* ldval) {
    push(ldval);
    push(top());  // the halfword is merely a placeholder
  }
  void push_pair_local(int i) {
    // longs are stored in locals in "push" order
    push(  local(i+0) );  // the real value
    assert(local(i+1) == top(), "");
    push(top());  // halfword placeholder
  }
  Node* pop_pair() {
    // the second half is pushed last & popped first; it contains exactly nothing
    Node* halfword = pop();
    assert(halfword == top(), "");
    // the long bits are pushed first & popped last:
    return pop();
  }
  void set_pair_local(int i, Node* lval) {
    // longs are stored in locals as a value/half pair (like doubles)
    set_local(i+0, lval);
    set_local(i+1, top());
  }

  // Push the node, which may be zero, one, or two words.
  void push_node(BasicType n_type, Node* n) {
    int n_size = type2size[n_type];
    if      (n_size == 1)  push(      n );  // T_INT, ...
    else if (n_size == 2)  push_pair( n );  // T_DOUBLE, T_LONG
    else                   { assert(n_size == 0, "must be T_VOID"); }
  }

  Node* pop_node(BasicType n_type) {
    int n_size = type2size[n_type];
    if      (n_size == 1)  return pop();
    else if (n_size == 2)  return pop_pair();
    else                   return NULL;
  }

  Node* control()               const { return map_not_null()->control(); }
  Node* i_o()                   const { return map_not_null()->i_o(); }
  Node* returnadr()             const { return map_not_null()->returnadr(); }
  Node* frameptr()              const { return map_not_null()->frameptr(); }
  Node* local(uint idx)         const { map_not_null(); return _map->local(      _map->_jvms, idx); }
  Node* stack(uint idx)         const { map_not_null(); return _map->stack(      _map->_jvms, idx); }
  Node* argument(uint idx)      const { map_not_null(); return _map->argument(   _map->_jvms, idx); }
  Node* monitor_box(uint idx)   const { map_not_null(); return _map->monitor_box(_map->_jvms, idx); }
  Node* monitor_obj(uint idx)   const { map_not_null(); return _map->monitor_obj(_map->_jvms, idx); }

  void set_control  (Node* c)         { map_not_null()->set_control(c); }
  void set_i_o      (Node* c)         { map_not_null()->set_i_o(c); }
  void set_local(uint idx, Node* c)   { map_not_null(); _map->set_local(   _map->_jvms, idx, c); }
  void set_stack(uint idx, Node* c)   { map_not_null(); _map->set_stack(   _map->_jvms, idx, c); }
  void set_argument(uint idx, Node* c){ map_not_null(); _map->set_argument(_map->_jvms, idx, c); }
  void ensure_stack(uint stk_size)    { map_not_null(); _map->ensure_stack(_map->_jvms, stk_size); }

  // Access unaliased memory
  Node* memory(uint alias_idx);
  Node* memory(const TypePtr *tp) { return memory(C->get_alias_index(tp)); }
  Node* memory(Node* adr) { return memory(_gvn.type(adr)->is_ptr()); }

  // Access immutable memory
  Node* immutable_memory() { return C->immutable_memory(); }

  // Set unaliased memory
  void set_memory(Node* c, uint alias_idx) { merged_memory()->set_memory_at(alias_idx, c); }
  void set_memory(Node* c, const TypePtr *tp) { set_memory(c,C->get_alias_index(tp)); }
  void set_memory(Node* c, Node* adr) { set_memory(c,_gvn.type(adr)->is_ptr()); }

  // Get the entire memory state (probably a MergeMemNode), and reset it
  // (The resetting prevents somebody from using the dangling Node pointer.)
  Node* reset_memory();

  // Get the entire memory state, asserted to be a MergeMemNode.
  MergeMemNode* merged_memory() {
    Node* mem = map_not_null()->memory();
    assert(mem->is_MergeMem(), "parse memory is always pre-split");
    return mem->as_MergeMem();
  }

  // Set the entire memory state; produce a new MergeMemNode.
  void set_all_memory(Node* newmem);

  // Create a memory projection from the call, then set_all_memory.
  void set_all_memory_call(Node* call, bool separate_io_proj = false);

  // Create a LoadNode, reading from the parser's memory state.
  // (Note:  require_atomic_access is useful only with T_LONG.)
  //
  // We choose the unordered semantics by default because we have
  // adapted the `do_put_xxx' and `do_get_xxx' procedures for the case
  // of volatile fields.
  Node* make_load(Node* ctl, Node* adr, const Type* t, BasicType bt,
                  MemNode::MemOrd mo, LoadNode::ControlDependency control_dependency = LoadNode::DependsOnlyOnTest,
                  bool require_atomic_access = false, bool unaligned = false,
                  bool mismatched = false, bool unsafe = false, uint8_t barrier_data = 0) {
    // This version computes alias_index from bottom_type
    return make_load(ctl, adr, t, bt, adr->bottom_type()->is_ptr(),
                     mo, control_dependency, require_atomic_access,
                     unaligned, mismatched, unsafe, barrier_data);
  }
  Node* make_load(Node* ctl, Node* adr, const Type* t, BasicType bt, const TypePtr* adr_type,
                  MemNode::MemOrd mo, LoadNode::ControlDependency control_dependency = LoadNode::DependsOnlyOnTest,
                  bool require_atomic_access = false, bool unaligned = false,
                  bool mismatched = false, bool unsafe = false, uint8_t barrier_data = 0) {
    // This version computes alias_index from an address type
    assert(adr_type != NULL, "use other make_load factory");
    return make_load(ctl, adr, t, bt, C->get_alias_index(adr_type),
                     mo, control_dependency, require_atomic_access,
                     unaligned, mismatched, unsafe, barrier_data);
  }
  // This is the base version which is given an alias index.
  Node* make_load(Node* ctl, Node* adr, const Type* t, BasicType bt, int adr_idx,
                  MemNode::MemOrd mo, LoadNode::ControlDependency control_dependency = LoadNode::DependsOnlyOnTest,
                  bool require_atomic_access = false, bool unaligned = false,
                  bool mismatched = false, bool unsafe = false, uint8_t barrier_data = 0);

  // Create & transform a StoreNode and store the effect into the
  // parser's memory state.
  //
  // We must ensure that stores of object references will be visible
  // only after the object's initialization. So the clients of this
  // procedure must indicate that the store requires `release'
  // semantics, if the stored value is an object reference that might
  // point to a new object and may become externally visible.
  Node* store_to_memory(Node* ctl, Node* adr, Node* val, BasicType bt,
                        const TypePtr* adr_type,
                        MemNode::MemOrd mo,
                        bool require_atomic_access = false,
                        bool unaligned = false,
                        bool mismatched = false,
                        bool unsafe = false) {
    // This version computes alias_index from an address type
    assert(adr_type != NULL, "use other store_to_memory factory");
    return store_to_memory(ctl, adr, val, bt,
                           C->get_alias_index(adr_type),
                           mo, require_atomic_access,
                           unaligned, mismatched, unsafe);
  }
  // This is the base version which is given alias index
  // Return the new StoreXNode
  Node* store_to_memory(Node* ctl, Node* adr, Node* val, BasicType bt,
                        int adr_idx,
                        MemNode::MemOrd,
                        bool require_atomic_access = false,
                        bool unaligned = false,
                        bool mismatched = false,
                        bool unsafe = false);

  // Perform decorated accesses

  Node* access_store_at(Node* obj,   // containing obj
                        Node* adr,   // actual adress to store val at
                        const TypePtr* adr_type,
                        Node* val,
                        const Type* val_type,
                        BasicType bt,
                        DecoratorSet decorators);

  Node* access_load_at(Node* obj,   // containing obj
                       Node* adr,   // actual adress to load val at
                       const TypePtr* adr_type,
                       const Type* val_type,
                       BasicType bt,
                       DecoratorSet decorators);

  Node* access_load(Node* adr,   // actual adress to load val at
                    const Type* val_type,
                    BasicType bt,
                    DecoratorSet decorators);

  Node* access_atomic_cmpxchg_val_at(Node* obj,
                                     Node* adr,
                                     const TypePtr* adr_type,
                                     int alias_idx,
                                     Node* expected_val,
                                     Node* new_val,
                                     const Type* value_type,
                                     BasicType bt,
                                     DecoratorSet decorators);

  Node* access_atomic_cmpxchg_bool_at(Node* obj,
                                      Node* adr,
                                      const TypePtr* adr_type,
                                      int alias_idx,
                                      Node* expected_val,
                                      Node* new_val,
                                      const Type* value_type,
                                      BasicType bt,
                                      DecoratorSet decorators);

  Node* access_atomic_xchg_at(Node* obj,
                              Node* adr,
                              const TypePtr* adr_type,
                              int alias_idx,
                              Node* new_val,
                              const Type* value_type,
                              BasicType bt,
                              DecoratorSet decorators);

  Node* access_atomic_add_at(Node* obj,
                             Node* adr,
                             const TypePtr* adr_type,
                             int alias_idx,
                             Node* new_val,
                             const Type* value_type,
                             BasicType bt,
                             DecoratorSet decorators);

  void access_clone(Node* src, Node* dst, Node* size, bool is_array);

  // Return addressing for an array element.
  Node* array_element_address(Node* ary, Node* idx, BasicType elembt,
                              // Optional constraint on the array size:
                              const TypeInt* sizetype = NULL,
                              // Optional control dependency (for example, on range check)
                              Node* ctrl = NULL);

  // Return a load of array element at idx.
  Node* load_array_element(Node* ctl, Node* ary, Node* idx, const TypeAryPtr* arytype);

  //---------------- Dtrace support --------------------
  void make_dtrace_method_entry_exit(ciMethod* method, bool is_entry);
  void make_dtrace_method_entry(ciMethod* method) {
    make_dtrace_method_entry_exit(method, true);
  }
  void make_dtrace_method_exit(ciMethod* method) {
    make_dtrace_method_entry_exit(method, false);
  }

  //--------------- stub generation -------------------
 public:
  void gen_stub(address C_function,
                const char *name,
                int is_fancy_jump,
                bool pass_tls,
                bool return_pc);

  //---------- help for generating calls --------------

  // Do a null check on the receiver as it would happen before the call to
  // callee (with all arguments still on the stack).
  Node* null_check_receiver_before_call(ciMethod* callee) {
    assert(!callee->is_static(), "must be a virtual method");
    // Callsite signature can be different from actual method being called (i.e _linkTo* sites).
    // Use callsite signature always.
    ciMethod* declared_method = method()->get_method_at_bci(bci());
    const int nargs = declared_method->arg_size();
    inc_sp(nargs);
    Node* n = null_check_receiver();
    dec_sp(nargs);
    return n;
  }

  // Fill in argument edges for the call from argument(0), argument(1), ...
  // (The next step is to call set_edges_for_java_call.)
  void  set_arguments_for_java_call(CallJavaNode* call);

  // Fill in non-argument edges for the call.
  // Transform the call, and update the basics: control, i_o, memory.
  // (The next step is usually to call set_results_for_java_call.)
  void set_edges_for_java_call(CallJavaNode* call,
                               bool must_throw = false, bool separate_io_proj = false);

  // Finish up a java call that was started by set_edges_for_java_call.
  // Call add_exception on any throw arising from the call.
  // Return the call result (transformed).
  Node* set_results_for_java_call(CallJavaNode* call, bool separate_io_proj = false, bool deoptimize = false);

  // Similar to set_edges_for_java_call, but simplified for runtime calls.
  void  set_predefined_output_for_runtime_call(Node* call) {
    set_predefined_output_for_runtime_call(call, NULL, NULL);
  }
  void  set_predefined_output_for_runtime_call(Node* call,
                                               Node* keep_mem,
                                               const TypePtr* hook_mem);
  Node* set_predefined_input_for_runtime_call(SafePointNode* call, Node* narrow_mem = NULL);

  // Replace the call with the current state of the kit.  Requires
  // that the call was generated with separate io_projs so that
  // exceptional control flow can be handled properly.
  void replace_call(CallNode* call, Node* result, bool do_replaced_nodes = false);

  // helper functions for statistics
  void increment_counter(address counter_addr);   // increment a debug counter
  void increment_counter(Node*   counter_addr);   // increment a debug counter

  // Bail out to the interpreter right now
  // The optional klass is the one causing the trap.
  // The optional reason is debug information written to the compile log.
  // Optional must_throw is the same as with add_safepoint_edges.
  void uncommon_trap(int trap_request,
                     ciKlass* klass = NULL, const char* reason_string = NULL,
                     bool must_throw = false, bool keep_exact_action = false);

  // Shorthand, to avoid saying "Deoptimization::" so many times.
  void uncommon_trap(Deoptimization::DeoptReason reason,
                     Deoptimization::DeoptAction action,
                     ciKlass* klass = NULL, const char* reason_string = NULL,
                     bool must_throw = false, bool keep_exact_action = false) {
    uncommon_trap(Deoptimization::make_trap_request(reason, action),
                  klass, reason_string, must_throw, keep_exact_action);
  }

  // Bail out to the interpreter and keep exact action (avoid switching to Action_none).
  void uncommon_trap_exact(Deoptimization::DeoptReason reason,
                           Deoptimization::DeoptAction action,
                           ciKlass* klass = NULL, const char* reason_string = NULL,
                           bool must_throw = false) {
    uncommon_trap(Deoptimization::make_trap_request(reason, action),
                  klass, reason_string, must_throw, /*keep_exact_action=*/true);
  }

  // SP when bytecode needs to be reexecuted.
  virtual int reexecute_sp() { return sp(); }

  // Report if there were too many traps at the current method and bci.
  // Report if a trap was recorded, and/or PerMethodTrapLimit was exceeded.
  // If there is no MDO at all, report no trap unless told to assume it.
  bool too_many_traps(Deoptimization::DeoptReason reason) {
    return C->too_many_traps(method(), bci(), reason);
  }

  // Report if there were too many recompiles at the current method and bci.
  bool too_many_recompiles(Deoptimization::DeoptReason reason) {
    return C->too_many_recompiles(method(), bci(), reason);
  }

  bool too_many_traps_or_recompiles(Deoptimization::DeoptReason reason) {
      return C->too_many_traps_or_recompiles(method(), bci(), reason);
  }

  // Returns the object (if any) which was created the moment before.
  Node* just_allocated_object(Node* current_control);

  // Sync Ideal and Graph kits.
  void sync_kit(IdealKit& ideal);
  void final_sync(IdealKit& ideal);

  public:
  // Helper function to round double arguments before a call
  void round_double_arguments(ciMethod* dest_method);

  // rounding for strict float precision conformance
  Node* precision_rounding(Node* n);

  // rounding for strict double precision conformance
  Node* dprecision_rounding(Node* n);

  // rounding for non-strict double stores
  Node* dstore_rounding(Node* n);

  // Helper functions for fast/slow path codes
  Node* opt_iff(Node* region, Node* iff);
  Node* make_runtime_call(int flags,
                          const TypeFunc* call_type, address call_addr,
                          const char* call_name,
                          const TypePtr* adr_type, // NULL if no memory effects
                          Node* parm0 = NULL, Node* parm1 = NULL,
                          Node* parm2 = NULL, Node* parm3 = NULL,
                          Node* parm4 = NULL, Node* parm5 = NULL,
                          Node* parm6 = NULL, Node* parm7 = NULL);

  Node* sign_extend_byte(Node* in);
  Node* sign_extend_short(Node* in);

  Node* make_native_call(address call_addr, const TypeFunc* call_type, uint nargs, ciNativeEntryPoint* nep);

  enum {  // flag values for make_runtime_call
    RC_NO_FP = 1,               // CallLeafNoFPNode
    RC_NO_IO = 2,               // do not hook IO edges
    RC_NO_LEAF = 4,             // CallStaticJavaNode
    RC_MUST_THROW = 8,          // flag passed to add_safepoint_edges
    RC_NARROW_MEM = 16,         // input memory is same as output
    RC_UNCOMMON = 32,           // freq. expected to be like uncommon trap
    RC_VECTOR = 64,             // CallLeafVectorNode
    RC_LEAF = 0                 // null value:  no flags set
  };

  // merge in all memory slices from new_mem, along the given path
  void merge_memory(Node* new_mem, Node* region, int new_path);
  void make_slow_call_ex(Node* call, ciInstanceKlass* ex_klass, bool separate_io_proj, bool deoptimize = false);

  // Helper functions to build synchronizations
  int next_monitor();
  Node* insert_mem_bar(int opcode, Node* precedent = NULL);
  Node* insert_mem_bar_volatile(int opcode, int alias_idx, Node* precedent = NULL);
  // Optional 'precedent' is appended as an extra edge, to force ordering.
  FastLockNode* shared_lock(Node* obj);
  void shared_unlock(Node* box, Node* obj);

  // helper functions for the fast path/slow path idioms
  Node* fast_and_slow(Node* in, const Type *result_type, Node* null_result, IfNode* fast_test, Node* fast_result, address slow_call, const TypeFunc *slow_call_type, Node* slow_arg, Klass* ex_klass, Node* slow_result);

  // Generate an instance-of idiom.  Used by both the instance-of bytecode
  // and the reflective instance-of call.
  Node* gen_instanceof(Node *subobj, Node* superkls, bool safe_for_replace = false);

  // Generate a check-cast idiom.  Used by both the check-cast bytecode
  // and the array-store bytecode
  Node* gen_checkcast( Node *subobj, Node* superkls,
                       Node* *failure_control = NULL );

  Node* gen_subtype_check(Node* obj, Node* superklass);

  // Exact type check used for predicted calls and casts.
  // Rewrites (*casted_receiver) to be casted to the stronger type.
  // (Caller is responsible for doing replace_in_map.)
  Node* type_check_receiver(Node* receiver, ciKlass* klass, float prob,
                            Node* *casted_receiver);

  // Inexact type check used for predicted calls.
  Node* subtype_check_receiver(Node* receiver, ciKlass* klass,
                               Node** casted_receiver);

  // implementation of object creation
  Node* set_output_for_allocation(AllocateNode* alloc,
                                  const TypeOopPtr* oop_type,
                                  bool deoptimize_on_exception=false);
  Node* get_layout_helper(Node* klass_node, jint& constant_value);
  Node* new_instance(Node* klass_node,
                     Node* slow_test = NULL,
                     Node* *return_size_val = NULL,
                     bool deoptimize_on_exception = false);
  Node* new_array(Node* klass_node, Node* count_val, int nargs,
                  Node* *return_size_val = NULL,
                  bool deoptimize_on_exception = false);

  // java.lang.String helpers
  Node* load_String_length(Node* str, bool set_ctrl);
  Node* load_String_value(Node* str, bool set_ctrl);
  Node* load_String_coder(Node* str, bool set_ctrl);
  void store_String_value(Node* str, Node* value);
  void store_String_coder(Node* str, Node* value);
  Node* capture_memory(const TypePtr* src_type, const TypePtr* dst_type);
  Node* compress_string(Node* src, const TypeAryPtr* src_type, Node* dst, Node* count);
  void inflate_string(Node* src, Node* dst, const TypeAryPtr* dst_type, Node* count);
  void inflate_string_slow(Node* src, Node* dst, Node* start, Node* count);

  // Handy for making control flow
  IfNode* create_and_map_if(Node* ctrl, Node* tst, float prob, float cnt) {
    IfNode* iff = new IfNode(ctrl, tst, prob, cnt);// New IfNode's
    _gvn.set_type(iff, iff->Value(&_gvn)); // Value may be known at parse-time
    // Place 'if' on worklist if it will be in graph
    if (!tst->is_Con())  record_for_igvn(iff);     // Range-check and Null-check removal is later
    return iff;
  }

  IfNode* create_and_xform_if(Node* ctrl, Node* tst, float prob, float cnt) {
    IfNode* iff = new IfNode(ctrl, tst, prob, cnt);// New IfNode's
    _gvn.transform(iff);                           // Value may be known at parse-time
    // Place 'if' on worklist if it will be in graph
    if (!tst->is_Con())  record_for_igvn(iff);     // Range-check and Null-check removal is later
    return iff;
  }

  void add_empty_predicates(int nargs = 0);
  void add_empty_predicate_impl(Deoptimization::DeoptReason reason, int nargs);

  Node* make_constant_from_field(ciField* field, Node* obj);

  // Vector API support (implemented in vectorIntrinsics.cpp)
  Node* box_vector(Node* in, const TypeInstPtr* vbox_type, BasicType elem_bt, int num_elem, bool deoptimize_on_exception = false);
  Node* unbox_vector(Node* in, const TypeInstPtr* vbox_type, BasicType elem_bt, int num_elem, bool shuffle_to_vector = false);
  Node* vector_shift_count(Node* cnt, int shift_op, BasicType bt, int num_elem);
};

// Helper class to support building of control flow branches. Upon
// creation the map and sp at bci are cloned and restored upon de-
// struction. Typical use:
//
// { PreserveJVMState pjvms(this);
//   // code of new branch
// }
// // here the JVM state at bci is established

class PreserveJVMState: public StackObj {
 protected:
  GraphKit*      _kit;
#ifdef ASSERT
  int            _block;  // PO of current block, if a Parse
  int            _bci;
#endif
  SafePointNode* _map;
  uint           _sp;

 public:
  PreserveJVMState(GraphKit* kit, bool clone_map = true);
  ~PreserveJVMState();
};

// Helper class to build cutouts of the form if (p) ; else {x...}.
// The code {x...} must not fall through.
// The kit's main flow of control is set to the "then" continuation of if(p).
class BuildCutout: public PreserveJVMState {
 public:
  BuildCutout(GraphKit* kit, Node* p, float prob, float cnt = COUNT_UNKNOWN);
  ~BuildCutout();
};

// Helper class to preserve the original _reexecute bit and _sp and restore
// them back
class PreserveReexecuteState: public StackObj {
 protected:
  GraphKit*                 _kit;
  uint                      _sp;
  JVMState::ReexecuteState  _reexecute;

 public:
  PreserveReexecuteState(GraphKit* kit);
  ~PreserveReexecuteState();
};

#endif // SHARE_OPTO_GRAPHKIT_HPP
