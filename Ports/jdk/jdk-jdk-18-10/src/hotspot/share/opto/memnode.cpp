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

#include "precompiled.hpp"
#include "classfile/javaClasses.hpp"
#include "compiler/compileLog.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/c2/barrierSetC2.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/objArrayKlass.hpp"
#include "opto/addnode.hpp"
#include "opto/arraycopynode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/regalloc.hpp"
#include "opto/compile.hpp"
#include "opto/connode.hpp"
#include "opto/convertnode.hpp"
#include "opto/loopnode.hpp"
#include "opto/machnode.hpp"
#include "opto/matcher.hpp"
#include "opto/memnode.hpp"
#include "opto/mulnode.hpp"
#include "opto/narrowptrnode.hpp"
#include "opto/phaseX.hpp"
#include "opto/regmask.hpp"
#include "opto/rootnode.hpp"
#include "opto/vectornode.hpp"
#include "utilities/align.hpp"
#include "utilities/copy.hpp"
#include "utilities/macros.hpp"
#include "utilities/powerOfTwo.hpp"
#include "utilities/vmError.hpp"

// Portions of code courtesy of Clifford Click

// Optimization - Graph Style

static Node *step_through_mergemem(PhaseGVN *phase, MergeMemNode *mmem,  const TypePtr *tp, const TypePtr *adr_check, outputStream *st);

//=============================================================================
uint MemNode::size_of() const { return sizeof(*this); }

const TypePtr *MemNode::adr_type() const {
  Node* adr = in(Address);
  if (adr == NULL)  return NULL; // node is dead
  const TypePtr* cross_check = NULL;
  DEBUG_ONLY(cross_check = _adr_type);
  return calculate_adr_type(adr->bottom_type(), cross_check);
}

bool MemNode::check_if_adr_maybe_raw(Node* adr) {
  if (adr != NULL) {
    if (adr->bottom_type()->base() == Type::RawPtr || adr->bottom_type()->base() == Type::AnyPtr) {
      return true;
    }
  }
  return false;
}

#ifndef PRODUCT
void MemNode::dump_spec(outputStream *st) const {
  if (in(Address) == NULL)  return; // node is dead
#ifndef ASSERT
  // fake the missing field
  const TypePtr* _adr_type = NULL;
  if (in(Address) != NULL)
    _adr_type = in(Address)->bottom_type()->isa_ptr();
#endif
  dump_adr_type(this, _adr_type, st);

  Compile* C = Compile::current();
  if (C->alias_type(_adr_type)->is_volatile()) {
    st->print(" Volatile!");
  }
  if (_unaligned_access) {
    st->print(" unaligned");
  }
  if (_mismatched_access) {
    st->print(" mismatched");
  }
  if (_unsafe_access) {
    st->print(" unsafe");
  }
}

void MemNode::dump_adr_type(const Node* mem, const TypePtr* adr_type, outputStream *st) {
  st->print(" @");
  if (adr_type == NULL) {
    st->print("NULL");
  } else {
    adr_type->dump_on(st);
    Compile* C = Compile::current();
    Compile::AliasType* atp = NULL;
    if (C->have_alias_type(adr_type))  atp = C->alias_type(adr_type);
    if (atp == NULL)
      st->print(", idx=?\?;");
    else if (atp->index() == Compile::AliasIdxBot)
      st->print(", idx=Bot;");
    else if (atp->index() == Compile::AliasIdxTop)
      st->print(", idx=Top;");
    else if (atp->index() == Compile::AliasIdxRaw)
      st->print(", idx=Raw;");
    else {
      ciField* field = atp->field();
      if (field) {
        st->print(", name=");
        field->print_name_on(st);
      }
      st->print(", idx=%d;", atp->index());
    }
  }
}

extern void print_alias_types();

#endif

Node *MemNode::optimize_simple_memory_chain(Node *mchain, const TypeOopPtr *t_oop, Node *load, PhaseGVN *phase) {
  assert((t_oop != NULL), "sanity");
  bool is_instance = t_oop->is_known_instance_field();
  bool is_boxed_value_load = t_oop->is_ptr_to_boxed_value() &&
                             (load != NULL) && load->is_Load() &&
                             (phase->is_IterGVN() != NULL);
  if (!(is_instance || is_boxed_value_load))
    return mchain;  // don't try to optimize non-instance types
  uint instance_id = t_oop->instance_id();
  Node *start_mem = phase->C->start()->proj_out_or_null(TypeFunc::Memory);
  Node *prev = NULL;
  Node *result = mchain;
  while (prev != result) {
    prev = result;
    if (result == start_mem)
      break;  // hit one of our sentinels
    // skip over a call which does not affect this memory slice
    if (result->is_Proj() && result->as_Proj()->_con == TypeFunc::Memory) {
      Node *proj_in = result->in(0);
      if (proj_in->is_Allocate() && proj_in->_idx == instance_id) {
        break;  // hit one of our sentinels
      } else if (proj_in->is_Call()) {
        // ArrayCopyNodes processed here as well
        CallNode *call = proj_in->as_Call();
        if (!call->may_modify(t_oop, phase)) { // returns false for instances
          result = call->in(TypeFunc::Memory);
        }
      } else if (proj_in->is_Initialize()) {
        AllocateNode* alloc = proj_in->as_Initialize()->allocation();
        // Stop if this is the initialization for the object instance which
        // contains this memory slice, otherwise skip over it.
        if ((alloc == NULL) || (alloc->_idx == instance_id)) {
          break;
        }
        if (is_instance) {
          result = proj_in->in(TypeFunc::Memory);
        } else if (is_boxed_value_load) {
          Node* klass = alloc->in(AllocateNode::KlassNode);
          const TypeKlassPtr* tklass = phase->type(klass)->is_klassptr();
          if (tklass->klass_is_exact() && !tklass->klass()->equals(t_oop->klass())) {
            result = proj_in->in(TypeFunc::Memory); // not related allocation
          }
        }
      } else if (proj_in->is_MemBar()) {
        ArrayCopyNode* ac = NULL;
        if (ArrayCopyNode::may_modify(t_oop, proj_in->as_MemBar(), phase, ac)) {
          break;
        }
        result = proj_in->in(TypeFunc::Memory);
      } else {
        assert(false, "unexpected projection");
      }
    } else if (result->is_ClearArray()) {
      if (!is_instance || !ClearArrayNode::step_through(&result, instance_id, phase)) {
        // Can not bypass initialization of the instance
        // we are looking for.
        break;
      }
      // Otherwise skip it (the call updated 'result' value).
    } else if (result->is_MergeMem()) {
      result = step_through_mergemem(phase, result->as_MergeMem(), t_oop, NULL, tty);
    }
  }
  return result;
}

Node *MemNode::optimize_memory_chain(Node *mchain, const TypePtr *t_adr, Node *load, PhaseGVN *phase) {
  const TypeOopPtr* t_oop = t_adr->isa_oopptr();
  if (t_oop == NULL)
    return mchain;  // don't try to optimize non-oop types
  Node* result = optimize_simple_memory_chain(mchain, t_oop, load, phase);
  bool is_instance = t_oop->is_known_instance_field();
  PhaseIterGVN *igvn = phase->is_IterGVN();
  if (is_instance && igvn != NULL && result->is_Phi()) {
    PhiNode *mphi = result->as_Phi();
    assert(mphi->bottom_type() == Type::MEMORY, "memory phi required");
    const TypePtr *t = mphi->adr_type();
    if (t == TypePtr::BOTTOM || t == TypeRawPtr::BOTTOM ||
        (t->isa_oopptr() && !t->is_oopptr()->is_known_instance() &&
         t->is_oopptr()->cast_to_exactness(true)
           ->is_oopptr()->cast_to_ptr_type(t_oop->ptr())
            ->is_oopptr()->cast_to_instance_id(t_oop->instance_id()) == t_oop)) {
      // clone the Phi with our address type
      result = mphi->split_out_instance(t_adr, igvn);
    } else {
      assert(phase->C->get_alias_index(t) == phase->C->get_alias_index(t_adr), "correct memory chain");
    }
  }
  return result;
}

static Node *step_through_mergemem(PhaseGVN *phase, MergeMemNode *mmem,  const TypePtr *tp, const TypePtr *adr_check, outputStream *st) {
  uint alias_idx = phase->C->get_alias_index(tp);
  Node *mem = mmem;
#ifdef ASSERT
  {
    // Check that current type is consistent with the alias index used during graph construction
    assert(alias_idx >= Compile::AliasIdxRaw, "must not be a bad alias_idx");
    bool consistent =  adr_check == NULL || adr_check->empty() ||
                       phase->C->must_alias(adr_check, alias_idx );
    // Sometimes dead array references collapse to a[-1], a[-2], or a[-3]
    if( !consistent && adr_check != NULL && !adr_check->empty() &&
               tp->isa_aryptr() &&        tp->offset() == Type::OffsetBot &&
        adr_check->isa_aryptr() && adr_check->offset() != Type::OffsetBot &&
        ( adr_check->offset() == arrayOopDesc::length_offset_in_bytes() ||
          adr_check->offset() == oopDesc::klass_offset_in_bytes() ||
          adr_check->offset() == oopDesc::mark_offset_in_bytes() ) ) {
      // don't assert if it is dead code.
      consistent = true;
    }
    if( !consistent ) {
      st->print("alias_idx==%d, adr_check==", alias_idx);
      if( adr_check == NULL ) {
        st->print("NULL");
      } else {
        adr_check->dump();
      }
      st->cr();
      print_alias_types();
      assert(consistent, "adr_check must match alias idx");
    }
  }
#endif
  // TypeOopPtr::NOTNULL+any is an OOP with unknown offset - generally
  // means an array I have not precisely typed yet.  Do not do any
  // alias stuff with it any time soon.
  const TypeOopPtr *toop = tp->isa_oopptr();
  if( tp->base() != Type::AnyPtr &&
      !(toop &&
        toop->klass() != NULL &&
        toop->klass()->is_java_lang_Object() &&
        toop->offset() == Type::OffsetBot) ) {
    // compress paths and change unreachable cycles to TOP
    // If not, we can update the input infinitely along a MergeMem cycle
    // Equivalent code in PhiNode::Ideal
    Node* m  = phase->transform(mmem);
    // If transformed to a MergeMem, get the desired slice
    // Otherwise the returned node represents memory for every slice
    mem = (m->is_MergeMem())? m->as_MergeMem()->memory_at(alias_idx) : m;
    // Update input if it is progress over what we have now
  }
  return mem;
}

//--------------------------Ideal_common---------------------------------------
// Look for degenerate control and memory inputs.  Bypass MergeMem inputs.
// Unhook non-raw memories from complete (macro-expanded) initializations.
Node *MemNode::Ideal_common(PhaseGVN *phase, bool can_reshape) {
  // If our control input is a dead region, kill all below the region
  Node *ctl = in(MemNode::Control);
  if (ctl && remove_dead_region(phase, can_reshape))
    return this;
  ctl = in(MemNode::Control);
  // Don't bother trying to transform a dead node
  if (ctl && ctl->is_top())  return NodeSentinel;

  PhaseIterGVN *igvn = phase->is_IterGVN();
  // Wait if control on the worklist.
  if (ctl && can_reshape && igvn != NULL) {
    Node* bol = NULL;
    Node* cmp = NULL;
    if (ctl->in(0)->is_If()) {
      assert(ctl->is_IfTrue() || ctl->is_IfFalse(), "sanity");
      bol = ctl->in(0)->in(1);
      if (bol->is_Bool())
        cmp = ctl->in(0)->in(1)->in(1);
    }
    if (igvn->_worklist.member(ctl) ||
        (bol != NULL && igvn->_worklist.member(bol)) ||
        (cmp != NULL && igvn->_worklist.member(cmp)) ) {
      // This control path may be dead.
      // Delay this memory node transformation until the control is processed.
      igvn->_worklist.push(this);
      return NodeSentinel; // caller will return NULL
    }
  }
  // Ignore if memory is dead, or self-loop
  Node *mem = in(MemNode::Memory);
  if (phase->type( mem ) == Type::TOP) return NodeSentinel; // caller will return NULL
  assert(mem != this, "dead loop in MemNode::Ideal");

  if (can_reshape && igvn != NULL && igvn->_worklist.member(mem)) {
    // This memory slice may be dead.
    // Delay this mem node transformation until the memory is processed.
    igvn->_worklist.push(this);
    return NodeSentinel; // caller will return NULL
  }

  Node *address = in(MemNode::Address);
  const Type *t_adr = phase->type(address);
  if (t_adr == Type::TOP)              return NodeSentinel; // caller will return NULL

  if (can_reshape && is_unsafe_access() && (t_adr == TypePtr::NULL_PTR)) {
    // Unsafe off-heap access with zero address. Remove access and other control users
    // to not confuse optimizations and add a HaltNode to fail if this is ever executed.
    assert(ctl != NULL, "unsafe accesses should be control dependent");
    for (DUIterator_Fast imax, i = ctl->fast_outs(imax); i < imax; i++) {
      Node* u = ctl->fast_out(i);
      if (u != ctl) {
        igvn->rehash_node_delayed(u);
        int nb = u->replace_edge(ctl, phase->C->top(), igvn);
        --i, imax -= nb;
      }
    }
    Node* frame = igvn->transform(new ParmNode(phase->C->start(), TypeFunc::FramePtr));
    Node* halt = igvn->transform(new HaltNode(ctl, frame, "unsafe off-heap access with zero address"));
    phase->C->root()->add_req(halt);
    return this;
  }

  if (can_reshape && igvn != NULL &&
      (igvn->_worklist.member(address) ||
       (igvn->_worklist.size() > 0 && t_adr != adr_type())) ) {
    // The address's base and type may change when the address is processed.
    // Delay this mem node transformation until the address is processed.
    igvn->_worklist.push(this);
    return NodeSentinel; // caller will return NULL
  }

  // Do NOT remove or optimize the next lines: ensure a new alias index
  // is allocated for an oop pointer type before Escape Analysis.
  // Note: C++ will not remove it since the call has side effect.
  if (t_adr->isa_oopptr()) {
    int alias_idx = phase->C->get_alias_index(t_adr->is_ptr());
  }

  Node* base = NULL;
  if (address->is_AddP()) {
    base = address->in(AddPNode::Base);
  }
  if (base != NULL && phase->type(base)->higher_equal(TypePtr::NULL_PTR) &&
      !t_adr->isa_rawptr()) {
    // Note: raw address has TOP base and top->higher_equal(TypePtr::NULL_PTR) is true.
    // Skip this node optimization if its address has TOP base.
    return NodeSentinel; // caller will return NULL
  }

  // Avoid independent memory operations
  Node* old_mem = mem;

  // The code which unhooks non-raw memories from complete (macro-expanded)
  // initializations was removed. After macro-expansion all stores catched
  // by Initialize node became raw stores and there is no information
  // which memory slices they modify. So it is unsafe to move any memory
  // operation above these stores. Also in most cases hooked non-raw memories
  // were already unhooked by using information from detect_ptr_independence()
  // and find_previous_store().

  if (mem->is_MergeMem()) {
    MergeMemNode* mmem = mem->as_MergeMem();
    const TypePtr *tp = t_adr->is_ptr();

    mem = step_through_mergemem(phase, mmem, tp, adr_type(), tty);
  }

  if (mem != old_mem) {
    set_req(MemNode::Memory, mem);
    if (can_reshape && old_mem->outcnt() == 0 && igvn != NULL) {
      igvn->_worklist.push(old_mem);
    }
    if (phase->type(mem) == Type::TOP) return NodeSentinel;
    return this;
  }

  // let the subclass continue analyzing...
  return NULL;
}

// Helper function for proving some simple control dominations.
// Attempt to prove that all control inputs of 'dom' dominate 'sub'.
// Already assumes that 'dom' is available at 'sub', and that 'sub'
// is not a constant (dominated by the method's StartNode).
// Used by MemNode::find_previous_store to prove that the
// control input of a memory operation predates (dominates)
// an allocation it wants to look past.
bool MemNode::all_controls_dominate(Node* dom, Node* sub) {
  if (dom == NULL || dom->is_top() || sub == NULL || sub->is_top())
    return false; // Conservative answer for dead code

  // Check 'dom'. Skip Proj and CatchProj nodes.
  dom = dom->find_exact_control(dom);
  if (dom == NULL || dom->is_top())
    return false; // Conservative answer for dead code

  if (dom == sub) {
    // For the case when, for example, 'sub' is Initialize and the original
    // 'dom' is Proj node of the 'sub'.
    return false;
  }

  if (dom->is_Con() || dom->is_Start() || dom->is_Root() || dom == sub)
    return true;

  // 'dom' dominates 'sub' if its control edge and control edges
  // of all its inputs dominate or equal to sub's control edge.

  // Currently 'sub' is either Allocate, Initialize or Start nodes.
  // Or Region for the check in LoadNode::Ideal();
  // 'sub' should have sub->in(0) != NULL.
  assert(sub->is_Allocate() || sub->is_Initialize() || sub->is_Start() ||
         sub->is_Region() || sub->is_Call(), "expecting only these nodes");

  // Get control edge of 'sub'.
  Node* orig_sub = sub;
  sub = sub->find_exact_control(sub->in(0));
  if (sub == NULL || sub->is_top())
    return false; // Conservative answer for dead code

  assert(sub->is_CFG(), "expecting control");

  if (sub == dom)
    return true;

  if (sub->is_Start() || sub->is_Root())
    return false;

  {
    // Check all control edges of 'dom'.

    ResourceMark rm;
    Node_List nlist;
    Unique_Node_List dom_list;

    dom_list.push(dom);
    bool only_dominating_controls = false;

    for (uint next = 0; next < dom_list.size(); next++) {
      Node* n = dom_list.at(next);
      if (n == orig_sub)
        return false; // One of dom's inputs dominated by sub.
      if (!n->is_CFG() && n->pinned()) {
        // Check only own control edge for pinned non-control nodes.
        n = n->find_exact_control(n->in(0));
        if (n == NULL || n->is_top())
          return false; // Conservative answer for dead code
        assert(n->is_CFG(), "expecting control");
        dom_list.push(n);
      } else if (n->is_Con() || n->is_Start() || n->is_Root()) {
        only_dominating_controls = true;
      } else if (n->is_CFG()) {
        if (n->dominates(sub, nlist))
          only_dominating_controls = true;
        else
          return false;
      } else {
        // First, own control edge.
        Node* m = n->find_exact_control(n->in(0));
        if (m != NULL) {
          if (m->is_top())
            return false; // Conservative answer for dead code
          dom_list.push(m);
        }
        // Now, the rest of edges.
        uint cnt = n->req();
        for (uint i = 1; i < cnt; i++) {
          m = n->find_exact_control(n->in(i));
          if (m == NULL || m->is_top())
            continue;
          dom_list.push(m);
        }
      }
    }
    return only_dominating_controls;
  }
}

//---------------------detect_ptr_independence---------------------------------
// Used by MemNode::find_previous_store to prove that two base
// pointers are never equal.
// The pointers are accompanied by their associated allocations,
// if any, which have been previously discovered by the caller.
bool MemNode::detect_ptr_independence(Node* p1, AllocateNode* a1,
                                      Node* p2, AllocateNode* a2,
                                      PhaseTransform* phase) {
  // Attempt to prove that these two pointers cannot be aliased.
  // They may both manifestly be allocations, and they should differ.
  // Or, if they are not both allocations, they can be distinct constants.
  // Otherwise, one is an allocation and the other a pre-existing value.
  if (a1 == NULL && a2 == NULL) {           // neither an allocation
    return (p1 != p2) && p1->is_Con() && p2->is_Con();
  } else if (a1 != NULL && a2 != NULL) {    // both allocations
    return (a1 != a2);
  } else if (a1 != NULL) {                  // one allocation a1
    // (Note:  p2->is_Con implies p2->in(0)->is_Root, which dominates.)
    return all_controls_dominate(p2, a1);
  } else { //(a2 != NULL)                   // one allocation a2
    return all_controls_dominate(p1, a2);
  }
  return false;
}


// Find an arraycopy ac that produces the memory state represented by parameter mem.
// Return ac if
// (a) can_see_stored_value=true  and ac must have set the value for this load or if
// (b) can_see_stored_value=false and ac could have set the value for this load or if
// (c) can_see_stored_value=false and ac cannot have set the value for this load.
// In case (c) change the parameter mem to the memory input of ac to skip it
// when searching stored value.
// Otherwise return NULL.
Node* LoadNode::find_previous_arraycopy(PhaseTransform* phase, Node* ld_alloc, Node*& mem, bool can_see_stored_value) const {
  ArrayCopyNode* ac = find_array_copy_clone(phase, ld_alloc, mem);
  if (ac != NULL) {
    Node* ld_addp = in(MemNode::Address);
    Node* src = ac->in(ArrayCopyNode::Src);
    const TypeAryPtr* ary_t = phase->type(src)->isa_aryptr();

    // This is a load from a cloned array. The corresponding arraycopy ac must
    // have set the value for the load and we can return ac but only if the load
    // is known to be within bounds. This is checked below.
    if (ary_t != NULL && ld_addp->is_AddP()) {
      Node* ld_offs = ld_addp->in(AddPNode::Offset);
      BasicType ary_elem = ary_t->klass()->as_array_klass()->element_type()->basic_type();
      jlong header = arrayOopDesc::base_offset_in_bytes(ary_elem);
      jlong elemsize = type2aelembytes(ary_elem);

      const TypeX*   ld_offs_t = phase->type(ld_offs)->isa_intptr_t();
      const TypeInt* sizetype  = ary_t->size();

      if (ld_offs_t->_lo >= header && ld_offs_t->_hi < (sizetype->_lo * elemsize + header)) {
        // The load is known to be within bounds. It receives its value from ac.
        return ac;
      }
      // The load is known to be out-of-bounds.
    }
    // The load could be out-of-bounds. It must not be hoisted but must remain
    // dependent on the runtime range check. This is achieved by returning NULL.
  } else if (mem->is_Proj() && mem->in(0) != NULL && mem->in(0)->is_ArrayCopy()) {
    ArrayCopyNode* ac = mem->in(0)->as_ArrayCopy();

    if (ac->is_arraycopy_validated() ||
        ac->is_copyof_validated() ||
        ac->is_copyofrange_validated()) {
      Node* ld_addp = in(MemNode::Address);
      if (ld_addp->is_AddP()) {
        Node* ld_base = ld_addp->in(AddPNode::Address);
        Node* ld_offs = ld_addp->in(AddPNode::Offset);

        Node* dest = ac->in(ArrayCopyNode::Dest);

        if (dest == ld_base) {
          const TypeX *ld_offs_t = phase->type(ld_offs)->isa_intptr_t();
          if (ac->modifies(ld_offs_t->_lo, ld_offs_t->_hi, phase, can_see_stored_value)) {
            return ac;
          }
          if (!can_see_stored_value) {
            mem = ac->in(TypeFunc::Memory);
            return ac;
          }
        }
      }
    }
  }
  return NULL;
}

ArrayCopyNode* MemNode::find_array_copy_clone(PhaseTransform* phase, Node* ld_alloc, Node* mem) const {
  if (mem->is_Proj() && mem->in(0) != NULL && (mem->in(0)->Opcode() == Op_MemBarStoreStore ||
                                                 mem->in(0)->Opcode() == Op_MemBarCPUOrder)) {
    if (ld_alloc != NULL) {
      // Check if there is an array copy for a clone
      Node* mb = mem->in(0);
      ArrayCopyNode* ac = NULL;
      if (mb->in(0) != NULL && mb->in(0)->is_Proj() &&
          mb->in(0)->in(0) != NULL && mb->in(0)->in(0)->is_ArrayCopy()) {
        ac = mb->in(0)->in(0)->as_ArrayCopy();
      } else {
        // Step over GC barrier when ReduceInitialCardMarks is disabled
        BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
        Node* control_proj_ac = bs->step_over_gc_barrier(mb->in(0));

        if (control_proj_ac->is_Proj() && control_proj_ac->in(0)->is_ArrayCopy()) {
          ac = control_proj_ac->in(0)->as_ArrayCopy();
        }
      }

      if (ac != NULL && ac->is_clonebasic()) {
        AllocateNode* alloc = AllocateNode::Ideal_allocation(ac->in(ArrayCopyNode::Dest), phase);
        if (alloc != NULL && alloc == ld_alloc) {
          return ac;
        }
      }
    }
  }
  return NULL;
}

// The logic for reordering loads and stores uses four steps:
// (a) Walk carefully past stores and initializations which we
//     can prove are independent of this load.
// (b) Observe that the next memory state makes an exact match
//     with self (load or store), and locate the relevant store.
// (c) Ensure that, if we were to wire self directly to the store,
//     the optimizer would fold it up somehow.
// (d) Do the rewiring, and return, depending on some other part of
//     the optimizer to fold up the load.
// This routine handles steps (a) and (b).  Steps (c) and (d) are
// specific to loads and stores, so they are handled by the callers.
// (Currently, only LoadNode::Ideal has steps (c), (d).  More later.)
//
Node* MemNode::find_previous_store(PhaseTransform* phase) {
  Node*         ctrl   = in(MemNode::Control);
  Node*         adr    = in(MemNode::Address);
  intptr_t      offset = 0;
  Node*         base   = AddPNode::Ideal_base_and_offset(adr, phase, offset);
  AllocateNode* alloc  = AllocateNode::Ideal_allocation(base, phase);

  if (offset == Type::OffsetBot)
    return NULL;            // cannot unalias unless there are precise offsets

  const bool adr_maybe_raw = check_if_adr_maybe_raw(adr);
  const TypeOopPtr *addr_t = adr->bottom_type()->isa_oopptr();

  intptr_t size_in_bytes = memory_size();

  Node* mem = in(MemNode::Memory);   // start searching here...

  int cnt = 50;             // Cycle limiter
  for (;;) {                // While we can dance past unrelated stores...
    if (--cnt < 0)  break;  // Caught in cycle or a complicated dance?

    Node* prev = mem;
    if (mem->is_Store()) {
      Node* st_adr = mem->in(MemNode::Address);
      intptr_t st_offset = 0;
      Node* st_base = AddPNode::Ideal_base_and_offset(st_adr, phase, st_offset);
      if (st_base == NULL)
        break;              // inscrutable pointer

      // For raw accesses it's not enough to prove that constant offsets don't intersect.
      // We need the bases to be the equal in order for the offset check to make sense.
      if ((adr_maybe_raw || check_if_adr_maybe_raw(st_adr)) && st_base != base) {
        break;
      }

      if (st_offset != offset && st_offset != Type::OffsetBot) {
        const int MAX_STORE = MAX2(BytesPerLong, (int)MaxVectorSize);
        assert(mem->as_Store()->memory_size() <= MAX_STORE, "");
        if (st_offset >= offset + size_in_bytes ||
            st_offset <= offset - MAX_STORE ||
            st_offset <= offset - mem->as_Store()->memory_size()) {
          // Success:  The offsets are provably independent.
          // (You may ask, why not just test st_offset != offset and be done?
          // The answer is that stores of different sizes can co-exist
          // in the same sequence of RawMem effects.  We sometimes initialize
          // a whole 'tile' of array elements with a single jint or jlong.)
          mem = mem->in(MemNode::Memory);
          continue;           // (a) advance through independent store memory
        }
      }
      if (st_base != base &&
          detect_ptr_independence(base, alloc,
                                  st_base,
                                  AllocateNode::Ideal_allocation(st_base, phase),
                                  phase)) {
        // Success:  The bases are provably independent.
        mem = mem->in(MemNode::Memory);
        continue;           // (a) advance through independent store memory
      }

      // (b) At this point, if the bases or offsets do not agree, we lose,
      // since we have not managed to prove 'this' and 'mem' independent.
      if (st_base == base && st_offset == offset) {
        return mem;         // let caller handle steps (c), (d)
      }

    } else if (mem->is_Proj() && mem->in(0)->is_Initialize()) {
      InitializeNode* st_init = mem->in(0)->as_Initialize();
      AllocateNode*  st_alloc = st_init->allocation();
      if (st_alloc == NULL)
        break;              // something degenerated
      bool known_identical = false;
      bool known_independent = false;
      if (alloc == st_alloc)
        known_identical = true;
      else if (alloc != NULL)
        known_independent = true;
      else if (all_controls_dominate(this, st_alloc))
        known_independent = true;

      if (known_independent) {
        // The bases are provably independent: Either they are
        // manifestly distinct allocations, or else the control
        // of this load dominates the store's allocation.
        int alias_idx = phase->C->get_alias_index(adr_type());
        if (alias_idx == Compile::AliasIdxRaw) {
          mem = st_alloc->in(TypeFunc::Memory);
        } else {
          mem = st_init->memory(alias_idx);
        }
        continue;           // (a) advance through independent store memory
      }

      // (b) at this point, if we are not looking at a store initializing
      // the same allocation we are loading from, we lose.
      if (known_identical) {
        // From caller, can_see_stored_value will consult find_captured_store.
        return mem;         // let caller handle steps (c), (d)
      }

    } else if (find_previous_arraycopy(phase, alloc, mem, false) != NULL) {
      if (prev != mem) {
        // Found an arraycopy but it doesn't affect that load
        continue;
      }
      // Found an arraycopy that may affect that load
      return mem;
    } else if (addr_t != NULL && addr_t->is_known_instance_field()) {
      // Can't use optimize_simple_memory_chain() since it needs PhaseGVN.
      if (mem->is_Proj() && mem->in(0)->is_Call()) {
        // ArrayCopyNodes processed here as well.
        CallNode *call = mem->in(0)->as_Call();
        if (!call->may_modify(addr_t, phase)) {
          mem = call->in(TypeFunc::Memory);
          continue;         // (a) advance through independent call memory
        }
      } else if (mem->is_Proj() && mem->in(0)->is_MemBar()) {
        ArrayCopyNode* ac = NULL;
        if (ArrayCopyNode::may_modify(addr_t, mem->in(0)->as_MemBar(), phase, ac)) {
          break;
        }
        mem = mem->in(0)->in(TypeFunc::Memory);
        continue;           // (a) advance through independent MemBar memory
      } else if (mem->is_ClearArray()) {
        if (ClearArrayNode::step_through(&mem, (uint)addr_t->instance_id(), phase)) {
          // (the call updated 'mem' value)
          continue;         // (a) advance through independent allocation memory
        } else {
          // Can not bypass initialization of the instance
          // we are looking for.
          return mem;
        }
      } else if (mem->is_MergeMem()) {
        int alias_idx = phase->C->get_alias_index(adr_type());
        mem = mem->as_MergeMem()->memory_at(alias_idx);
        continue;           // (a) advance through independent MergeMem memory
      }
    }

    // Unless there is an explicit 'continue', we must bail out here,
    // because 'mem' is an inscrutable memory state (e.g., a call).
    break;
  }

  return NULL;              // bail out
}

//----------------------calculate_adr_type-------------------------------------
// Helper function.  Notices when the given type of address hits top or bottom.
// Also, asserts a cross-check of the type against the expected address type.
const TypePtr* MemNode::calculate_adr_type(const Type* t, const TypePtr* cross_check) {
  if (t == Type::TOP)  return NULL; // does not touch memory any more?
  #ifdef ASSERT
  if (!VerifyAliases || VMError::is_error_reported() || Node::in_dump())  cross_check = NULL;
  #endif
  const TypePtr* tp = t->isa_ptr();
  if (tp == NULL) {
    assert(cross_check == NULL || cross_check == TypePtr::BOTTOM, "expected memory type must be wide");
    return TypePtr::BOTTOM;           // touches lots of memory
  } else {
    #ifdef ASSERT
    // %%%% [phh] We don't check the alias index if cross_check is
    //            TypeRawPtr::BOTTOM.  Needs to be investigated.
    if (cross_check != NULL &&
        cross_check != TypePtr::BOTTOM &&
        cross_check != TypeRawPtr::BOTTOM) {
      // Recheck the alias index, to see if it has changed (due to a bug).
      Compile* C = Compile::current();
      assert(C->get_alias_index(cross_check) == C->get_alias_index(tp),
             "must stay in the original alias category");
      // The type of the address must be contained in the adr_type,
      // disregarding "null"-ness.
      // (We make an exception for TypeRawPtr::BOTTOM, which is a bit bucket.)
      const TypePtr* tp_notnull = tp->join(TypePtr::NOTNULL)->is_ptr();
      assert(cross_check->meet(tp_notnull) == cross_check->remove_speculative(),
             "real address must not escape from expected memory type");
    }
    #endif
    return tp;
  }
}

//=============================================================================
// Should LoadNode::Ideal() attempt to remove control edges?
bool LoadNode::can_remove_control() const {
  return true;
}
uint LoadNode::size_of() const { return sizeof(*this); }
bool LoadNode::cmp( const Node &n ) const
{ return !Type::cmp( _type, ((LoadNode&)n)._type ); }
const Type *LoadNode::bottom_type() const { return _type; }
uint LoadNode::ideal_reg() const {
  return _type->ideal_reg();
}

#ifndef PRODUCT
void LoadNode::dump_spec(outputStream *st) const {
  MemNode::dump_spec(st);
  if( !Verbose && !WizardMode ) {
    // standard dump does this in Verbose and WizardMode
    st->print(" #"); _type->dump_on(st);
  }
  if (!depends_only_on_test()) {
    st->print(" (does not depend only on test)");
  }
}
#endif

#ifdef ASSERT
//----------------------------is_immutable_value-------------------------------
// Helper function to allow a raw load without control edge for some cases
bool LoadNode::is_immutable_value(Node* adr) {
  return (adr->is_AddP() && adr->in(AddPNode::Base)->is_top() &&
          adr->in(AddPNode::Address)->Opcode() == Op_ThreadLocal &&
          (adr->in(AddPNode::Offset)->find_intptr_t_con(-1) ==
           in_bytes(JavaThread::osthread_offset()) ||
           adr->in(AddPNode::Offset)->find_intptr_t_con(-1) ==
           in_bytes(JavaThread::threadObj_offset())));
}
#endif

//----------------------------LoadNode::make-----------------------------------
// Polymorphic factory method:
Node *LoadNode::make(PhaseGVN& gvn, Node *ctl, Node *mem, Node *adr, const TypePtr* adr_type, const Type *rt, BasicType bt, MemOrd mo,
                     ControlDependency control_dependency, bool unaligned, bool mismatched, bool unsafe, uint8_t barrier_data) {
  Compile* C = gvn.C;

  // sanity check the alias category against the created node type
  assert(!(adr_type->isa_oopptr() &&
           adr_type->offset() == oopDesc::klass_offset_in_bytes()),
         "use LoadKlassNode instead");
  assert(!(adr_type->isa_aryptr() &&
           adr_type->offset() == arrayOopDesc::length_offset_in_bytes()),
         "use LoadRangeNode instead");
  // Check control edge of raw loads
  assert( ctl != NULL || C->get_alias_index(adr_type) != Compile::AliasIdxRaw ||
          // oop will be recorded in oop map if load crosses safepoint
          rt->isa_oopptr() || is_immutable_value(adr),
          "raw memory operations should have control edge");
  LoadNode* load = NULL;
  switch (bt) {
  case T_BOOLEAN: load = new LoadUBNode(ctl, mem, adr, adr_type, rt->is_int(),  mo, control_dependency); break;
  case T_BYTE:    load = new LoadBNode (ctl, mem, adr, adr_type, rt->is_int(),  mo, control_dependency); break;
  case T_INT:     load = new LoadINode (ctl, mem, adr, adr_type, rt->is_int(),  mo, control_dependency); break;
  case T_CHAR:    load = new LoadUSNode(ctl, mem, adr, adr_type, rt->is_int(),  mo, control_dependency); break;
  case T_SHORT:   load = new LoadSNode (ctl, mem, adr, adr_type, rt->is_int(),  mo, control_dependency); break;
  case T_LONG:    load = new LoadLNode (ctl, mem, adr, adr_type, rt->is_long(), mo, control_dependency); break;
  case T_FLOAT:   load = new LoadFNode (ctl, mem, adr, adr_type, rt,            mo, control_dependency); break;
  case T_DOUBLE:  load = new LoadDNode (ctl, mem, adr, adr_type, rt,            mo, control_dependency); break;
  case T_ADDRESS: load = new LoadPNode (ctl, mem, adr, adr_type, rt->is_ptr(),  mo, control_dependency); break;
  case T_OBJECT:
#ifdef _LP64
    if (adr->bottom_type()->is_ptr_to_narrowoop()) {
      load = new LoadNNode(ctl, mem, adr, adr_type, rt->make_narrowoop(), mo, control_dependency);
    } else
#endif
    {
      assert(!adr->bottom_type()->is_ptr_to_narrowoop() && !adr->bottom_type()->is_ptr_to_narrowklass(), "should have got back a narrow oop");
      load = new LoadPNode(ctl, mem, adr, adr_type, rt->is_ptr(), mo, control_dependency);
    }
    break;
  default:
    ShouldNotReachHere();
    break;
  }
  assert(load != NULL, "LoadNode should have been created");
  if (unaligned) {
    load->set_unaligned_access();
  }
  if (mismatched) {
    load->set_mismatched_access();
  }
  if (unsafe) {
    load->set_unsafe_access();
  }
  load->set_barrier_data(barrier_data);
  if (load->Opcode() == Op_LoadN) {
    Node* ld = gvn.transform(load);
    return new DecodeNNode(ld, ld->bottom_type()->make_ptr());
  }

  return load;
}

LoadLNode* LoadLNode::make_atomic(Node* ctl, Node* mem, Node* adr, const TypePtr* adr_type, const Type* rt, MemOrd mo,
                                  ControlDependency control_dependency, bool unaligned, bool mismatched, bool unsafe, uint8_t barrier_data) {
  bool require_atomic = true;
  LoadLNode* load = new LoadLNode(ctl, mem, adr, adr_type, rt->is_long(), mo, control_dependency, require_atomic);
  if (unaligned) {
    load->set_unaligned_access();
  }
  if (mismatched) {
    load->set_mismatched_access();
  }
  if (unsafe) {
    load->set_unsafe_access();
  }
  load->set_barrier_data(barrier_data);
  return load;
}

LoadDNode* LoadDNode::make_atomic(Node* ctl, Node* mem, Node* adr, const TypePtr* adr_type, const Type* rt, MemOrd mo,
                                  ControlDependency control_dependency, bool unaligned, bool mismatched, bool unsafe, uint8_t barrier_data) {
  bool require_atomic = true;
  LoadDNode* load = new LoadDNode(ctl, mem, adr, adr_type, rt, mo, control_dependency, require_atomic);
  if (unaligned) {
    load->set_unaligned_access();
  }
  if (mismatched) {
    load->set_mismatched_access();
  }
  if (unsafe) {
    load->set_unsafe_access();
  }
  load->set_barrier_data(barrier_data);
  return load;
}



//------------------------------hash-------------------------------------------
uint LoadNode::hash() const {
  // unroll addition of interesting fields
  return (uintptr_t)in(Control) + (uintptr_t)in(Memory) + (uintptr_t)in(Address);
}

static bool skip_through_membars(Compile::AliasType* atp, const TypeInstPtr* tp, bool eliminate_boxing) {
  if ((atp != NULL) && (atp->index() >= Compile::AliasIdxRaw)) {
    bool non_volatile = (atp->field() != NULL) && !atp->field()->is_volatile();
    bool is_stable_ary = FoldStableValues &&
                         (tp != NULL) && (tp->isa_aryptr() != NULL) &&
                         tp->isa_aryptr()->is_stable();

    return (eliminate_boxing && non_volatile) || is_stable_ary;
  }

  return false;
}

// Is the value loaded previously stored by an arraycopy? If so return
// a load node that reads from the source array so we may be able to
// optimize out the ArrayCopy node later.
Node* LoadNode::can_see_arraycopy_value(Node* st, PhaseGVN* phase) const {
  Node* ld_adr = in(MemNode::Address);
  intptr_t ld_off = 0;
  AllocateNode* ld_alloc = AllocateNode::Ideal_allocation(ld_adr, phase, ld_off);
  Node* ac = find_previous_arraycopy(phase, ld_alloc, st, true);
  if (ac != NULL) {
    assert(ac->is_ArrayCopy(), "what kind of node can this be?");

    Node* mem = ac->in(TypeFunc::Memory);
    Node* ctl = ac->in(0);
    Node* src = ac->in(ArrayCopyNode::Src);

    if (!ac->as_ArrayCopy()->is_clonebasic() && !phase->type(src)->isa_aryptr()) {
      return NULL;
    }

    LoadNode* ld = clone()->as_Load();
    Node* addp = in(MemNode::Address)->clone();
    if (ac->as_ArrayCopy()->is_clonebasic()) {
      assert(ld_alloc != NULL, "need an alloc");
      assert(addp->is_AddP(), "address must be addp");
      BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
      assert(bs->step_over_gc_barrier(addp->in(AddPNode::Base)) == bs->step_over_gc_barrier(ac->in(ArrayCopyNode::Dest)), "strange pattern");
      assert(bs->step_over_gc_barrier(addp->in(AddPNode::Address)) == bs->step_over_gc_barrier(ac->in(ArrayCopyNode::Dest)), "strange pattern");
      addp->set_req(AddPNode::Base, src);
      addp->set_req(AddPNode::Address, src);
    } else {
      assert(ac->as_ArrayCopy()->is_arraycopy_validated() ||
             ac->as_ArrayCopy()->is_copyof_validated() ||
             ac->as_ArrayCopy()->is_copyofrange_validated(), "only supported cases");
      assert(addp->in(AddPNode::Base) == addp->in(AddPNode::Address), "should be");
      addp->set_req(AddPNode::Base, src);
      addp->set_req(AddPNode::Address, src);

      const TypeAryPtr* ary_t = phase->type(in(MemNode::Address))->isa_aryptr();
      BasicType ary_elem  = ary_t->klass()->as_array_klass()->element_type()->basic_type();
      uint header = arrayOopDesc::base_offset_in_bytes(ary_elem);
      uint shift  = exact_log2(type2aelembytes(ary_elem));

      Node* diff = phase->transform(new SubINode(ac->in(ArrayCopyNode::SrcPos), ac->in(ArrayCopyNode::DestPos)));
#ifdef _LP64
      diff = phase->transform(new ConvI2LNode(diff));
#endif
      diff = phase->transform(new LShiftXNode(diff, phase->intcon(shift)));

      Node* offset = phase->transform(new AddXNode(addp->in(AddPNode::Offset), diff));
      addp->set_req(AddPNode::Offset, offset);
    }
    addp = phase->transform(addp);
#ifdef ASSERT
    const TypePtr* adr_type = phase->type(addp)->is_ptr();
    ld->_adr_type = adr_type;
#endif
    ld->set_req(MemNode::Address, addp);
    ld->set_req(0, ctl);
    ld->set_req(MemNode::Memory, mem);
    // load depends on the tests that validate the arraycopy
    ld->_control_dependency = UnknownControl;
    return ld;
  }
  return NULL;
}


//---------------------------can_see_stored_value------------------------------
// This routine exists to make sure this set of tests is done the same
// everywhere.  We need to make a coordinated change: first LoadNode::Ideal
// will change the graph shape in a way which makes memory alive twice at the
// same time (uses the Oracle model of aliasing), then some
// LoadXNode::Identity will fold things back to the equivalence-class model
// of aliasing.
Node* MemNode::can_see_stored_value(Node* st, PhaseTransform* phase) const {
  Node* ld_adr = in(MemNode::Address);
  intptr_t ld_off = 0;
  Node* ld_base = AddPNode::Ideal_base_and_offset(ld_adr, phase, ld_off);
  Node* ld_alloc = AllocateNode::Ideal_allocation(ld_base, phase);
  const TypeInstPtr* tp = phase->type(ld_adr)->isa_instptr();
  Compile::AliasType* atp = (tp != NULL) ? phase->C->alias_type(tp) : NULL;
  // This is more general than load from boxing objects.
  if (skip_through_membars(atp, tp, phase->C->eliminate_boxing())) {
    uint alias_idx = atp->index();
    bool final = !atp->is_rewritable();
    Node* result = NULL;
    Node* current = st;
    // Skip through chains of MemBarNodes checking the MergeMems for
    // new states for the slice of this load.  Stop once any other
    // kind of node is encountered.  Loads from final memory can skip
    // through any kind of MemBar but normal loads shouldn't skip
    // through MemBarAcquire since the could allow them to move out of
    // a synchronized region.
    while (current->is_Proj()) {
      int opc = current->in(0)->Opcode();
      if ((final && (opc == Op_MemBarAcquire ||
                     opc == Op_MemBarAcquireLock ||
                     opc == Op_LoadFence)) ||
          opc == Op_MemBarRelease ||
          opc == Op_StoreFence ||
          opc == Op_MemBarReleaseLock ||
          opc == Op_MemBarStoreStore ||
          opc == Op_MemBarCPUOrder) {
        Node* mem = current->in(0)->in(TypeFunc::Memory);
        if (mem->is_MergeMem()) {
          MergeMemNode* merge = mem->as_MergeMem();
          Node* new_st = merge->memory_at(alias_idx);
          if (new_st == merge->base_memory()) {
            // Keep searching
            current = new_st;
            continue;
          }
          // Save the new memory state for the slice and fall through
          // to exit.
          result = new_st;
        }
      }
      break;
    }
    if (result != NULL) {
      st = result;
    }
  }

  // Loop around twice in the case Load -> Initialize -> Store.
  // (See PhaseIterGVN::add_users_to_worklist, which knows about this case.)
  for (int trip = 0; trip <= 1; trip++) {

    if (st->is_Store()) {
      Node* st_adr = st->in(MemNode::Address);
      if (st_adr != ld_adr) {
        // Try harder before giving up. Unify base pointers with casts (e.g., raw/non-raw pointers).
        intptr_t st_off = 0;
        Node* st_base = AddPNode::Ideal_base_and_offset(st_adr, phase, st_off);
        if (ld_base == NULL)                                   return NULL;
        if (st_base == NULL)                                   return NULL;
        if (!ld_base->eqv_uncast(st_base, /*keep_deps=*/true)) return NULL;
        if (ld_off != st_off)                                  return NULL;
        if (ld_off == Type::OffsetBot)                         return NULL;
        // Same base, same offset.
        // Possible improvement for arrays: check index value instead of absolute offset.

        // At this point we have proven something like this setup:
        //   B = << base >>
        //   L =  LoadQ(AddP(Check/CastPP(B), #Off))
        //   S = StoreQ(AddP(             B , #Off), V)
        // (Actually, we haven't yet proven the Q's are the same.)
        // In other words, we are loading from a casted version of
        // the same pointer-and-offset that we stored to.
        // Casted version may carry a dependency and it is respected.
        // Thus, we are able to replace L by V.
      }
      // Now prove that we have a LoadQ matched to a StoreQ, for some Q.
      if (store_Opcode() != st->Opcode()) {
        return NULL;
      }
      // LoadVector/StoreVector needs additional check to ensure the types match.
      if (store_Opcode() == Op_StoreVector) {
        const TypeVect*  in_vt = st->as_StoreVector()->vect_type();
        const TypeVect* out_vt = as_LoadVector()->vect_type();
        if (in_vt != out_vt) {
          return NULL;
        }
      }
      return st->in(MemNode::ValueIn);
    }

    // A load from a freshly-created object always returns zero.
    // (This can happen after LoadNode::Ideal resets the load's memory input
    // to find_captured_store, which returned InitializeNode::zero_memory.)
    if (st->is_Proj() && st->in(0)->is_Allocate() &&
        (st->in(0) == ld_alloc) &&
        (ld_off >= st->in(0)->as_Allocate()->minimum_header_size())) {
      // return a zero value for the load's basic type
      // (This is one of the few places where a generic PhaseTransform
      // can create new nodes.  Think of it as lazily manifesting
      // virtually pre-existing constants.)
      if (memory_type() != T_VOID) {
        if (ReduceBulkZeroing || find_array_copy_clone(phase, ld_alloc, in(MemNode::Memory)) == NULL) {
          // If ReduceBulkZeroing is disabled, we need to check if the allocation does not belong to an
          // ArrayCopyNode clone. If it does, then we cannot assume zero since the initialization is done
          // by the ArrayCopyNode.
          return phase->zerocon(memory_type());
        }
      } else {
        // TODO: materialize all-zero vector constant
        assert(!isa_Load() || as_Load()->type()->isa_vect(), "");
      }
    }

    // A load from an initialization barrier can match a captured store.
    if (st->is_Proj() && st->in(0)->is_Initialize()) {
      InitializeNode* init = st->in(0)->as_Initialize();
      AllocateNode* alloc = init->allocation();
      if ((alloc != NULL) && (alloc == ld_alloc)) {
        // examine a captured store value
        st = init->find_captured_store(ld_off, memory_size(), phase);
        if (st != NULL) {
          continue;             // take one more trip around
        }
      }
    }

    // Load boxed value from result of valueOf() call is input parameter.
    if (this->is_Load() && ld_adr->is_AddP() &&
        (tp != NULL) && tp->is_ptr_to_boxed_value()) {
      intptr_t ignore = 0;
      Node* base = AddPNode::Ideal_base_and_offset(ld_adr, phase, ignore);
      BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
      base = bs->step_over_gc_barrier(base);
      if (base != NULL && base->is_Proj() &&
          base->as_Proj()->_con == TypeFunc::Parms &&
          base->in(0)->is_CallStaticJava() &&
          base->in(0)->as_CallStaticJava()->is_boxing_method()) {
        return base->in(0)->in(TypeFunc::Parms);
      }
    }

    break;
  }

  return NULL;
}

//----------------------is_instance_field_load_with_local_phi------------------
bool LoadNode::is_instance_field_load_with_local_phi(Node* ctrl) {
  if( in(Memory)->is_Phi() && in(Memory)->in(0) == ctrl &&
      in(Address)->is_AddP() ) {
    const TypeOopPtr* t_oop = in(Address)->bottom_type()->isa_oopptr();
    // Only instances and boxed values.
    if( t_oop != NULL &&
        (t_oop->is_ptr_to_boxed_value() ||
         t_oop->is_known_instance_field()) &&
        t_oop->offset() != Type::OffsetBot &&
        t_oop->offset() != Type::OffsetTop) {
      return true;
    }
  }
  return false;
}

//------------------------------Identity---------------------------------------
// Loads are identity if previous store is to same address
Node* LoadNode::Identity(PhaseGVN* phase) {
  // If the previous store-maker is the right kind of Store, and the store is
  // to the same address, then we are equal to the value stored.
  Node* mem = in(Memory);
  Node* value = can_see_stored_value(mem, phase);
  if( value ) {
    // byte, short & char stores truncate naturally.
    // A load has to load the truncated value which requires
    // some sort of masking operation and that requires an
    // Ideal call instead of an Identity call.
    if (memory_size() < BytesPerInt) {
      // If the input to the store does not fit with the load's result type,
      // it must be truncated via an Ideal call.
      if (!phase->type(value)->higher_equal(phase->type(this)))
        return this;
    }
    // (This works even when value is a Con, but LoadNode::Value
    // usually runs first, producing the singleton type of the Con.)
    return value;
  }

  // Search for an existing data phi which was generated before for the same
  // instance's field to avoid infinite generation of phis in a loop.
  Node *region = mem->in(0);
  if (is_instance_field_load_with_local_phi(region)) {
    const TypeOopPtr *addr_t = in(Address)->bottom_type()->isa_oopptr();
    int this_index  = phase->C->get_alias_index(addr_t);
    int this_offset = addr_t->offset();
    int this_iid    = addr_t->instance_id();
    if (!addr_t->is_known_instance() &&
         addr_t->is_ptr_to_boxed_value()) {
      // Use _idx of address base (could be Phi node) for boxed values.
      intptr_t   ignore = 0;
      Node*      base = AddPNode::Ideal_base_and_offset(in(Address), phase, ignore);
      if (base == NULL) {
        return this;
      }
      this_iid = base->_idx;
    }
    const Type* this_type = bottom_type();
    for (DUIterator_Fast imax, i = region->fast_outs(imax); i < imax; i++) {
      Node* phi = region->fast_out(i);
      if (phi->is_Phi() && phi != mem &&
          phi->as_Phi()->is_same_inst_field(this_type, (int)mem->_idx, this_iid, this_index, this_offset)) {
        return phi;
      }
    }
  }

  return this;
}

// Construct an equivalent unsigned load.
Node* LoadNode::convert_to_unsigned_load(PhaseGVN& gvn) {
  BasicType bt = T_ILLEGAL;
  const Type* rt = NULL;
  switch (Opcode()) {
    case Op_LoadUB: return this;
    case Op_LoadUS: return this;
    case Op_LoadB: bt = T_BOOLEAN; rt = TypeInt::UBYTE; break;
    case Op_LoadS: bt = T_CHAR;    rt = TypeInt::CHAR;  break;
    default:
      assert(false, "no unsigned variant: %s", Name());
      return NULL;
  }
  return LoadNode::make(gvn, in(MemNode::Control), in(MemNode::Memory), in(MemNode::Address),
                        raw_adr_type(), rt, bt, _mo, _control_dependency,
                        is_unaligned_access(), is_mismatched_access());
}

// Construct an equivalent signed load.
Node* LoadNode::convert_to_signed_load(PhaseGVN& gvn) {
  BasicType bt = T_ILLEGAL;
  const Type* rt = NULL;
  switch (Opcode()) {
    case Op_LoadUB: bt = T_BYTE;  rt = TypeInt::BYTE;  break;
    case Op_LoadUS: bt = T_SHORT; rt = TypeInt::SHORT; break;
    case Op_LoadB: // fall through
    case Op_LoadS: // fall through
    case Op_LoadI: // fall through
    case Op_LoadL: return this;
    default:
      assert(false, "no signed variant: %s", Name());
      return NULL;
  }
  return LoadNode::make(gvn, in(MemNode::Control), in(MemNode::Memory), in(MemNode::Address),
                        raw_adr_type(), rt, bt, _mo, _control_dependency,
                        is_unaligned_access(), is_mismatched_access());
}

bool LoadNode::has_reinterpret_variant(const Type* rt) {
  BasicType bt = rt->basic_type();
  switch (Opcode()) {
    case Op_LoadI: return (bt == T_FLOAT);
    case Op_LoadL: return (bt == T_DOUBLE);
    case Op_LoadF: return (bt == T_INT);
    case Op_LoadD: return (bt == T_LONG);

    default: return false;
  }
}

Node* LoadNode::convert_to_reinterpret_load(PhaseGVN& gvn, const Type* rt) {
  BasicType bt = rt->basic_type();
  assert(has_reinterpret_variant(rt), "no reinterpret variant: %s %s", Name(), type2name(bt));
  bool is_mismatched = is_mismatched_access();
  const TypeRawPtr* raw_type = gvn.type(in(MemNode::Memory))->isa_rawptr();
  if (raw_type == NULL) {
    is_mismatched = true; // conservatively match all non-raw accesses as mismatched
  }
  return LoadNode::make(gvn, in(MemNode::Control), in(MemNode::Memory), in(MemNode::Address),
                        raw_adr_type(), rt, bt, _mo, _control_dependency,
                        is_unaligned_access(), is_mismatched);
}

bool StoreNode::has_reinterpret_variant(const Type* vt) {
  BasicType bt = vt->basic_type();
  switch (Opcode()) {
    case Op_StoreI: return (bt == T_FLOAT);
    case Op_StoreL: return (bt == T_DOUBLE);
    case Op_StoreF: return (bt == T_INT);
    case Op_StoreD: return (bt == T_LONG);

    default: return false;
  }
}

Node* StoreNode::convert_to_reinterpret_store(PhaseGVN& gvn, Node* val, const Type* vt) {
  BasicType bt = vt->basic_type();
  assert(has_reinterpret_variant(vt), "no reinterpret variant: %s %s", Name(), type2name(bt));
  StoreNode* st = StoreNode::make(gvn, in(MemNode::Control), in(MemNode::Memory), in(MemNode::Address), raw_adr_type(), val, bt, _mo);

  bool is_mismatched = is_mismatched_access();
  const TypeRawPtr* raw_type = gvn.type(in(MemNode::Memory))->isa_rawptr();
  if (raw_type == NULL) {
    is_mismatched = true; // conservatively match all non-raw accesses as mismatched
  }
  if (is_mismatched) {
    st->set_mismatched_access();
  }
  return st;
}

// We're loading from an object which has autobox behaviour.
// If this object is result of a valueOf call we'll have a phi
// merging a newly allocated object and a load from the cache.
// We want to replace this load with the original incoming
// argument to the valueOf call.
Node* LoadNode::eliminate_autobox(PhaseIterGVN* igvn) {
  assert(igvn->C->eliminate_boxing(), "sanity");
  intptr_t ignore = 0;
  Node* base = AddPNode::Ideal_base_and_offset(in(Address), igvn, ignore);
  if ((base == NULL) || base->is_Phi()) {
    // Push the loads from the phi that comes from valueOf up
    // through it to allow elimination of the loads and the recovery
    // of the original value. It is done in split_through_phi().
    return NULL;
  } else if (base->is_Load() ||
             (base->is_DecodeN() && base->in(1)->is_Load())) {
    // Eliminate the load of boxed value for integer types from the cache
    // array by deriving the value from the index into the array.
    // Capture the offset of the load and then reverse the computation.

    // Get LoadN node which loads a boxing object from 'cache' array.
    if (base->is_DecodeN()) {
      base = base->in(1);
    }
    if (!base->in(Address)->is_AddP()) {
      return NULL; // Complex address
    }
    AddPNode* address = base->in(Address)->as_AddP();
    Node* cache_base = address->in(AddPNode::Base);
    if ((cache_base != NULL) && cache_base->is_DecodeN()) {
      // Get ConP node which is static 'cache' field.
      cache_base = cache_base->in(1);
    }
    if ((cache_base != NULL) && cache_base->is_Con()) {
      const TypeAryPtr* base_type = cache_base->bottom_type()->isa_aryptr();
      if ((base_type != NULL) && base_type->is_autobox_cache()) {
        Node* elements[4];
        int shift = exact_log2(type2aelembytes(T_OBJECT));
        int count = address->unpack_offsets(elements, ARRAY_SIZE(elements));
        if (count > 0 && elements[0]->is_Con() &&
            (count == 1 ||
             (count == 2 && elements[1]->Opcode() == Op_LShiftX &&
                            elements[1]->in(2) == igvn->intcon(shift)))) {
          ciObjArray* array = base_type->const_oop()->as_obj_array();
          // Fetch the box object cache[0] at the base of the array and get its value
          ciInstance* box = array->obj_at(0)->as_instance();
          ciInstanceKlass* ik = box->klass()->as_instance_klass();
          assert(ik->is_box_klass(), "sanity");
          assert(ik->nof_nonstatic_fields() == 1, "change following code");
          if (ik->nof_nonstatic_fields() == 1) {
            // This should be true nonstatic_field_at requires calling
            // nof_nonstatic_fields so check it anyway
            ciConstant c = box->field_value(ik->nonstatic_field_at(0));
            BasicType bt = c.basic_type();
            // Only integer types have boxing cache.
            assert(bt == T_BOOLEAN || bt == T_CHAR  ||
                   bt == T_BYTE    || bt == T_SHORT ||
                   bt == T_INT     || bt == T_LONG, "wrong type = %s", type2name(bt));
            jlong cache_low = (bt == T_LONG) ? c.as_long() : c.as_int();
            if (cache_low != (int)cache_low) {
              return NULL; // should not happen since cache is array indexed by value
            }
            jlong offset = arrayOopDesc::base_offset_in_bytes(T_OBJECT) - (cache_low << shift);
            if (offset != (int)offset) {
              return NULL; // should not happen since cache is array indexed by value
            }
           // Add up all the offsets making of the address of the load
            Node* result = elements[0];
            for (int i = 1; i < count; i++) {
              result = igvn->transform(new AddXNode(result, elements[i]));
            }
            // Remove the constant offset from the address and then
            result = igvn->transform(new AddXNode(result, igvn->MakeConX(-(int)offset)));
            // remove the scaling of the offset to recover the original index.
            if (result->Opcode() == Op_LShiftX && result->in(2) == igvn->intcon(shift)) {
              // Peel the shift off directly but wrap it in a dummy node
              // since Ideal can't return existing nodes
              igvn->_worklist.push(result); // remove dead node later
              result = new RShiftXNode(result->in(1), igvn->intcon(0));
            } else if (result->is_Add() && result->in(2)->is_Con() &&
                       result->in(1)->Opcode() == Op_LShiftX &&
                       result->in(1)->in(2) == igvn->intcon(shift)) {
              // We can't do general optimization: ((X<<Z) + Y) >> Z ==> X + (Y>>Z)
              // but for boxing cache access we know that X<<Z will not overflow
              // (there is range check) so we do this optimizatrion by hand here.
              igvn->_worklist.push(result); // remove dead node later
              Node* add_con = new RShiftXNode(result->in(2), igvn->intcon(shift));
              result = new AddXNode(result->in(1)->in(1), igvn->transform(add_con));
            } else {
              result = new RShiftXNode(result, igvn->intcon(shift));
            }
#ifdef _LP64
            if (bt != T_LONG) {
              result = new ConvL2INode(igvn->transform(result));
            }
#else
            if (bt == T_LONG) {
              result = new ConvI2LNode(igvn->transform(result));
            }
#endif
            // Boxing/unboxing can be done from signed & unsigned loads (e.g. LoadUB -> ... -> LoadB pair).
            // Need to preserve unboxing load type if it is unsigned.
            switch(this->Opcode()) {
              case Op_LoadUB:
                result = new AndINode(igvn->transform(result), igvn->intcon(0xFF));
                break;
              case Op_LoadUS:
                result = new AndINode(igvn->transform(result), igvn->intcon(0xFFFF));
                break;
            }
            return result;
          }
        }
      }
    }
  }
  return NULL;
}

static bool stable_phi(PhiNode* phi, PhaseGVN *phase) {
  Node* region = phi->in(0);
  if (region == NULL) {
    return false; // Wait stable graph
  }
  uint cnt = phi->req();
  for (uint i = 1; i < cnt; i++) {
    Node* rc = region->in(i);
    if (rc == NULL || phase->type(rc) == Type::TOP)
      return false; // Wait stable graph
    Node* in = phi->in(i);
    if (in == NULL || phase->type(in) == Type::TOP)
      return false; // Wait stable graph
  }
  return true;
}
//------------------------------split_through_phi------------------------------
// Split instance or boxed field load through Phi.
Node *LoadNode::split_through_phi(PhaseGVN *phase) {
  Node* mem     = in(Memory);
  Node* address = in(Address);
  const TypeOopPtr *t_oop = phase->type(address)->isa_oopptr();

  assert((t_oop != NULL) &&
         (t_oop->is_known_instance_field() ||
          t_oop->is_ptr_to_boxed_value()), "invalide conditions");

  Compile* C = phase->C;
  intptr_t ignore = 0;
  Node*    base = AddPNode::Ideal_base_and_offset(address, phase, ignore);
  bool base_is_phi = (base != NULL) && base->is_Phi();
  bool load_boxed_values = t_oop->is_ptr_to_boxed_value() && C->aggressive_unboxing() &&
                           (base != NULL) && (base == address->in(AddPNode::Base)) &&
                           phase->type(base)->higher_equal(TypePtr::NOTNULL);

  if (!((mem->is_Phi() || base_is_phi) &&
        (load_boxed_values || t_oop->is_known_instance_field()))) {
    return NULL; // memory is not Phi
  }

  if (mem->is_Phi()) {
    if (!stable_phi(mem->as_Phi(), phase)) {
      return NULL; // Wait stable graph
    }
    uint cnt = mem->req();
    // Check for loop invariant memory.
    if (cnt == 3) {
      for (uint i = 1; i < cnt; i++) {
        Node* in = mem->in(i);
        Node*  m = optimize_memory_chain(in, t_oop, this, phase);
        if (m == mem) {
          if (i == 1) {
            // if the first edge was a loop, check second edge too.
            // If both are replaceable - we are in an infinite loop
            Node *n = optimize_memory_chain(mem->in(2), t_oop, this, phase);
            if (n == mem) {
              break;
            }
          }
          set_req(Memory, mem->in(cnt - i));
          return this; // made change
        }
      }
    }
  }
  if (base_is_phi) {
    if (!stable_phi(base->as_Phi(), phase)) {
      return NULL; // Wait stable graph
    }
    uint cnt = base->req();
    // Check for loop invariant memory.
    if (cnt == 3) {
      for (uint i = 1; i < cnt; i++) {
        if (base->in(i) == base) {
          return NULL; // Wait stable graph
        }
      }
    }
  }

  // Split through Phi (see original code in loopopts.cpp).
  assert(C->have_alias_type(t_oop), "instance should have alias type");

  // Do nothing here if Identity will find a value
  // (to avoid infinite chain of value phis generation).
  if (this != Identity(phase)) {
    return NULL;
  }

  // Select Region to split through.
  Node* region;
  if (!base_is_phi) {
    assert(mem->is_Phi(), "sanity");
    region = mem->in(0);
    // Skip if the region dominates some control edge of the address.
    if (!MemNode::all_controls_dominate(address, region))
      return NULL;
  } else if (!mem->is_Phi()) {
    assert(base_is_phi, "sanity");
    region = base->in(0);
    // Skip if the region dominates some control edge of the memory.
    if (!MemNode::all_controls_dominate(mem, region))
      return NULL;
  } else if (base->in(0) != mem->in(0)) {
    assert(base_is_phi && mem->is_Phi(), "sanity");
    if (MemNode::all_controls_dominate(mem, base->in(0))) {
      region = base->in(0);
    } else if (MemNode::all_controls_dominate(address, mem->in(0))) {
      region = mem->in(0);
    } else {
      return NULL; // complex graph
    }
  } else {
    assert(base->in(0) == mem->in(0), "sanity");
    region = mem->in(0);
  }

  const Type* this_type = this->bottom_type();
  int this_index  = C->get_alias_index(t_oop);
  int this_offset = t_oop->offset();
  int this_iid    = t_oop->instance_id();
  if (!t_oop->is_known_instance() && load_boxed_values) {
    // Use _idx of address base for boxed values.
    this_iid = base->_idx;
  }
  PhaseIterGVN* igvn = phase->is_IterGVN();
  Node* phi = new PhiNode(region, this_type, NULL, mem->_idx, this_iid, this_index, this_offset);
  for (uint i = 1; i < region->req(); i++) {
    Node* x;
    Node* the_clone = NULL;
    Node* in = region->in(i);
    if (region->is_CountedLoop() && region->as_Loop()->is_strip_mined() && i == LoopNode::EntryControl &&
        in != NULL && in->is_OuterStripMinedLoop()) {
      // No node should go in the outer strip mined loop
      in = in->in(LoopNode::EntryControl);
    }
    if (in == NULL || in == C->top()) {
      x = C->top();      // Dead path?  Use a dead data op
    } else {
      x = this->clone();        // Else clone up the data op
      the_clone = x;            // Remember for possible deletion.
      // Alter data node to use pre-phi inputs
      if (this->in(0) == region) {
        x->set_req(0, in);
      } else {
        x->set_req(0, NULL);
      }
      if (mem->is_Phi() && (mem->in(0) == region)) {
        x->set_req(Memory, mem->in(i)); // Use pre-Phi input for the clone.
      }
      if (address->is_Phi() && address->in(0) == region) {
        x->set_req(Address, address->in(i)); // Use pre-Phi input for the clone
      }
      if (base_is_phi && (base->in(0) == region)) {
        Node* base_x = base->in(i); // Clone address for loads from boxed objects.
        Node* adr_x = phase->transform(new AddPNode(base_x,base_x,address->in(AddPNode::Offset)));
        x->set_req(Address, adr_x);
      }
    }
    // Check for a 'win' on some paths
    const Type *t = x->Value(igvn);

    bool singleton = t->singleton();

    // See comments in PhaseIdealLoop::split_thru_phi().
    if (singleton && t == Type::TOP) {
      singleton &= region->is_Loop() && (i != LoopNode::EntryControl);
    }

    if (singleton) {
      x = igvn->makecon(t);
    } else {
      // We now call Identity to try to simplify the cloned node.
      // Note that some Identity methods call phase->type(this).
      // Make sure that the type array is big enough for
      // our new node, even though we may throw the node away.
      // (This tweaking with igvn only works because x is a new node.)
      igvn->set_type(x, t);
      // If x is a TypeNode, capture any more-precise type permanently into Node
      // otherwise it will be not updated during igvn->transform since
      // igvn->type(x) is set to x->Value() already.
      x->raise_bottom_type(t);
      Node* y = x->Identity(igvn);
      if (y != x) {
        x = y;
      } else {
        y = igvn->hash_find_insert(x);
        if (y) {
          x = y;
        } else {
          // Else x is a new node we are keeping
          // We do not need register_new_node_with_optimizer
          // because set_type has already been called.
          igvn->_worklist.push(x);
        }
      }
    }
    if (x != the_clone && the_clone != NULL) {
      igvn->remove_dead_node(the_clone);
    }
    phi->set_req(i, x);
  }
  // Record Phi
  igvn->register_new_node_with_optimizer(phi);
  return phi;
}

AllocateNode* LoadNode::is_new_object_mark_load(PhaseGVN *phase) const {
  if (Opcode() == Op_LoadX) {
    Node* address = in(MemNode::Address);
    AllocateNode* alloc = AllocateNode::Ideal_allocation(address, phase);
    Node* mem = in(MemNode::Memory);
    if (alloc != NULL && mem->is_Proj() &&
        mem->in(0) != NULL &&
        mem->in(0) == alloc->initialization() &&
        alloc->initialization()->proj_out_or_null(0) != NULL) {
      return alloc;
    }
  }
  return NULL;
}


//------------------------------Ideal------------------------------------------
// If the load is from Field memory and the pointer is non-null, it might be possible to
// zero out the control input.
// If the offset is constant and the base is an object allocation,
// try to hook me up to the exact initializing store.
Node *LoadNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  Node* p = MemNode::Ideal_common(phase, can_reshape);
  if (p)  return (p == NodeSentinel) ? NULL : p;

  Node* ctrl    = in(MemNode::Control);
  Node* address = in(MemNode::Address);
  bool progress = false;

  bool addr_mark = ((phase->type(address)->isa_oopptr() || phase->type(address)->isa_narrowoop()) &&
         phase->type(address)->is_ptr()->offset() == oopDesc::mark_offset_in_bytes());

  // Skip up past a SafePoint control.  Cannot do this for Stores because
  // pointer stores & cardmarks must stay on the same side of a SafePoint.
  if( ctrl != NULL && ctrl->Opcode() == Op_SafePoint &&
      phase->C->get_alias_index(phase->type(address)->is_ptr()) != Compile::AliasIdxRaw  &&
      !addr_mark &&
      (depends_only_on_test() || has_unknown_control_dependency())) {
    ctrl = ctrl->in(0);
    set_req(MemNode::Control,ctrl);
    progress = true;
  }

  intptr_t ignore = 0;
  Node*    base   = AddPNode::Ideal_base_and_offset(address, phase, ignore);
  if (base != NULL
      && phase->C->get_alias_index(phase->type(address)->is_ptr()) != Compile::AliasIdxRaw) {
    // Check for useless control edge in some common special cases
    if (in(MemNode::Control) != NULL
        && can_remove_control()
        && phase->type(base)->higher_equal(TypePtr::NOTNULL)
        && all_controls_dominate(base, phase->C->start())) {
      // A method-invariant, non-null address (constant or 'this' argument).
      set_req(MemNode::Control, NULL);
      progress = true;
    }
  }

  Node* mem = in(MemNode::Memory);
  const TypePtr *addr_t = phase->type(address)->isa_ptr();

  if (can_reshape && (addr_t != NULL)) {
    // try to optimize our memory input
    Node* opt_mem = MemNode::optimize_memory_chain(mem, addr_t, this, phase);
    if (opt_mem != mem) {
      set_req_X(MemNode::Memory, opt_mem, phase);
      if (phase->type( opt_mem ) == Type::TOP) return NULL;
      return this;
    }
    const TypeOopPtr *t_oop = addr_t->isa_oopptr();
    if ((t_oop != NULL) &&
        (t_oop->is_known_instance_field() ||
         t_oop->is_ptr_to_boxed_value())) {
      PhaseIterGVN *igvn = phase->is_IterGVN();
      assert(igvn != NULL, "must be PhaseIterGVN when can_reshape is true");
      if (igvn->_worklist.member(opt_mem)) {
        // Delay this transformation until memory Phi is processed.
        igvn->_worklist.push(this);
        return NULL;
      }
      // Split instance field load through Phi.
      Node* result = split_through_phi(phase);
      if (result != NULL) return result;

      if (t_oop->is_ptr_to_boxed_value()) {
        Node* result = eliminate_autobox(igvn);
        if (result != NULL) return result;
      }
    }
  }

  // Is there a dominating load that loads the same value?  Leave
  // anything that is not a load of a field/array element (like
  // barriers etc.) alone
  if (in(0) != NULL && !adr_type()->isa_rawptr() && can_reshape) {
    for (DUIterator_Fast imax, i = mem->fast_outs(imax); i < imax; i++) {
      Node *use = mem->fast_out(i);
      if (use != this &&
          use->Opcode() == Opcode() &&
          use->in(0) != NULL &&
          use->in(0) != in(0) &&
          use->in(Address) == in(Address)) {
        Node* ctl = in(0);
        for (int i = 0; i < 10 && ctl != NULL; i++) {
          ctl = IfNode::up_one_dom(ctl);
          if (ctl == use->in(0)) {
            set_req(0, use->in(0));
            return this;
          }
        }
      }
    }
  }

  // Check for prior store with a different base or offset; make Load
  // independent.  Skip through any number of them.  Bail out if the stores
  // are in an endless dead cycle and report no progress.  This is a key
  // transform for Reflection.  However, if after skipping through the Stores
  // we can't then fold up against a prior store do NOT do the transform as
  // this amounts to using the 'Oracle' model of aliasing.  It leaves the same
  // array memory alive twice: once for the hoisted Load and again after the
  // bypassed Store.  This situation only works if EVERYBODY who does
  // anti-dependence work knows how to bypass.  I.e. we need all
  // anti-dependence checks to ask the same Oracle.  Right now, that Oracle is
  // the alias index stuff.  So instead, peek through Stores and IFF we can
  // fold up, do so.
  Node* prev_mem = find_previous_store(phase);
  if (prev_mem != NULL) {
    Node* value = can_see_arraycopy_value(prev_mem, phase);
    if (value != NULL) {
      return value;
    }
  }
  // Steps (a), (b):  Walk past independent stores to find an exact match.
  if (prev_mem != NULL && prev_mem != in(MemNode::Memory)) {
    // (c) See if we can fold up on the spot, but don't fold up here.
    // Fold-up might require truncation (for LoadB/LoadS/LoadUS) or
    // just return a prior value, which is done by Identity calls.
    if (can_see_stored_value(prev_mem, phase)) {
      // Make ready for step (d):
      set_req_X(MemNode::Memory, prev_mem, phase);
      return this;
    }
  }

  return progress ? this : NULL;
}

// Helper to recognize certain Klass fields which are invariant across
// some group of array types (e.g., int[] or all T[] where T < Object).
const Type*
LoadNode::load_array_final_field(const TypeKlassPtr *tkls,
                                 ciKlass* klass) const {
  if (tkls->offset() == in_bytes(Klass::modifier_flags_offset())) {
    // The field is Klass::_modifier_flags.  Return its (constant) value.
    // (Folds up the 2nd indirection in aClassConstant.getModifiers().)
    assert(this->Opcode() == Op_LoadI, "must load an int from _modifier_flags");
    return TypeInt::make(klass->modifier_flags());
  }
  if (tkls->offset() == in_bytes(Klass::access_flags_offset())) {
    // The field is Klass::_access_flags.  Return its (constant) value.
    // (Folds up the 2nd indirection in Reflection.getClassAccessFlags(aClassConstant).)
    assert(this->Opcode() == Op_LoadI, "must load an int from _access_flags");
    return TypeInt::make(klass->access_flags());
  }
  if (tkls->offset() == in_bytes(Klass::layout_helper_offset())) {
    // The field is Klass::_layout_helper.  Return its constant value if known.
    assert(this->Opcode() == Op_LoadI, "must load an int from _layout_helper");
    return TypeInt::make(klass->layout_helper());
  }

  // No match.
  return NULL;
}

//------------------------------Value-----------------------------------------
const Type* LoadNode::Value(PhaseGVN* phase) const {
  // Either input is TOP ==> the result is TOP
  Node* mem = in(MemNode::Memory);
  const Type *t1 = phase->type(mem);
  if (t1 == Type::TOP)  return Type::TOP;
  Node* adr = in(MemNode::Address);
  const TypePtr* tp = phase->type(adr)->isa_ptr();
  if (tp == NULL || tp->empty())  return Type::TOP;
  int off = tp->offset();
  assert(off != Type::OffsetTop, "case covered by TypePtr::empty");
  Compile* C = phase->C;

  // Try to guess loaded type from pointer type
  if (tp->isa_aryptr()) {
    const TypeAryPtr* ary = tp->is_aryptr();
    const Type* t = ary->elem();

    // Determine whether the reference is beyond the header or not, by comparing
    // the offset against the offset of the start of the array's data.
    // Different array types begin at slightly different offsets (12 vs. 16).
    // We choose T_BYTE as an example base type that is least restrictive
    // as to alignment, which will therefore produce the smallest
    // possible base offset.
    const int min_base_off = arrayOopDesc::base_offset_in_bytes(T_BYTE);
    const bool off_beyond_header = (off >= min_base_off);

    // Try to constant-fold a stable array element.
    if (FoldStableValues && !is_mismatched_access() && ary->is_stable()) {
      // Make sure the reference is not into the header and the offset is constant
      ciObject* aobj = ary->const_oop();
      if (aobj != NULL && off_beyond_header && adr->is_AddP() && off != Type::OffsetBot) {
        int stable_dimension = (ary->stable_dimension() > 0 ? ary->stable_dimension() - 1 : 0);
        const Type* con_type = Type::make_constant_from_array_element(aobj->as_array(), off,
                                                                      stable_dimension,
                                                                      memory_type(), is_unsigned());
        if (con_type != NULL) {
          return con_type;
        }
      }
    }

    // Don't do this for integer types. There is only potential profit if
    // the element type t is lower than _type; that is, for int types, if _type is
    // more restrictive than t.  This only happens here if one is short and the other
    // char (both 16 bits), and in those cases we've made an intentional decision
    // to use one kind of load over the other. See AndINode::Ideal and 4965907.
    // Also, do not try to narrow the type for a LoadKlass, regardless of offset.
    //
    // Yes, it is possible to encounter an expression like (LoadKlass p1:(AddP x x 8))
    // where the _gvn.type of the AddP is wider than 8.  This occurs when an earlier
    // copy p0 of (AddP x x 8) has been proven equal to p1, and the p0 has been
    // subsumed by p1.  If p1 is on the worklist but has not yet been re-transformed,
    // it is possible that p1 will have a type like Foo*[int+]:NotNull*+any.
    // In fact, that could have been the original type of p1, and p1 could have
    // had an original form like p1:(AddP x x (LShiftL quux 3)), where the
    // expression (LShiftL quux 3) independently optimized to the constant 8.
    if ((t->isa_int() == NULL) && (t->isa_long() == NULL)
        && (_type->isa_vect() == NULL)
        && Opcode() != Op_LoadKlass && Opcode() != Op_LoadNKlass) {
      // t might actually be lower than _type, if _type is a unique
      // concrete subclass of abstract class t.
      if (off_beyond_header || off == Type::OffsetBot) {  // is the offset beyond the header?
        const Type* jt = t->join_speculative(_type);
        // In any case, do not allow the join, per se, to empty out the type.
        if (jt->empty() && !t->empty()) {
          // This can happen if a interface-typed array narrows to a class type.
          jt = _type;
        }
#ifdef ASSERT
        if (phase->C->eliminate_boxing() && adr->is_AddP()) {
          // The pointers in the autobox arrays are always non-null
          Node* base = adr->in(AddPNode::Base);
          if ((base != NULL) && base->is_DecodeN()) {
            // Get LoadN node which loads IntegerCache.cache field
            base = base->in(1);
          }
          if ((base != NULL) && base->is_Con()) {
            const TypeAryPtr* base_type = base->bottom_type()->isa_aryptr();
            if ((base_type != NULL) && base_type->is_autobox_cache()) {
              // It could be narrow oop
              assert(jt->make_ptr()->ptr() == TypePtr::NotNull,"sanity");
            }
          }
        }
#endif
        return jt;
      }
    }
  } else if (tp->base() == Type::InstPtr) {
    assert( off != Type::OffsetBot ||
            // arrays can be cast to Objects
            tp->is_oopptr()->klass()->is_java_lang_Object() ||
            // unsafe field access may not have a constant offset
            C->has_unsafe_access(),
            "Field accesses must be precise" );
    // For oop loads, we expect the _type to be precise.

    // Optimize loads from constant fields.
    const TypeInstPtr* tinst = tp->is_instptr();
    ciObject* const_oop = tinst->const_oop();
    if (!is_mismatched_access() && off != Type::OffsetBot && const_oop != NULL && const_oop->is_instance()) {
      const Type* con_type = Type::make_constant_from_field(const_oop->as_instance(), off, is_unsigned(), memory_type());
      if (con_type != NULL) {
        return con_type;
      }
    }
  } else if (tp->base() == Type::KlassPtr) {
    assert( off != Type::OffsetBot ||
            // arrays can be cast to Objects
            tp->is_klassptr()->klass()->is_java_lang_Object() ||
            // also allow array-loading from the primary supertype
            // array during subtype checks
            Opcode() == Op_LoadKlass,
            "Field accesses must be precise" );
    // For klass/static loads, we expect the _type to be precise
  } else if (tp->base() == Type::RawPtr && adr->is_Load() && off == 0) {
    /* With mirrors being an indirect in the Klass*
     * the VM is now using two loads. LoadKlass(LoadP(LoadP(Klass, mirror_offset), zero_offset))
     * The LoadP from the Klass has a RawPtr type (see LibraryCallKit::load_mirror_from_klass).
     *
     * So check the type and klass of the node before the LoadP.
     */
    Node* adr2 = adr->in(MemNode::Address);
    const TypeKlassPtr* tkls = phase->type(adr2)->isa_klassptr();
    if (tkls != NULL && !StressReflectiveCode) {
      ciKlass* klass = tkls->klass();
      if (klass->is_loaded() && tkls->klass_is_exact() && tkls->offset() == in_bytes(Klass::java_mirror_offset())) {
        assert(adr->Opcode() == Op_LoadP, "must load an oop from _java_mirror");
        assert(Opcode() == Op_LoadP, "must load an oop from _java_mirror");
        return TypeInstPtr::make(klass->java_mirror());
      }
    }
  }

  const TypeKlassPtr *tkls = tp->isa_klassptr();
  if (tkls != NULL && !StressReflectiveCode) {
    ciKlass* klass = tkls->klass();
    if (klass->is_loaded() && tkls->klass_is_exact()) {
      // We are loading a field from a Klass metaobject whose identity
      // is known at compile time (the type is "exact" or "precise").
      // Check for fields we know are maintained as constants by the VM.
      if (tkls->offset() == in_bytes(Klass::super_check_offset_offset())) {
        // The field is Klass::_super_check_offset.  Return its (constant) value.
        // (Folds up type checking code.)
        assert(Opcode() == Op_LoadI, "must load an int from _super_check_offset");
        return TypeInt::make(klass->super_check_offset());
      }
      // Compute index into primary_supers array
      juint depth = (tkls->offset() - in_bytes(Klass::primary_supers_offset())) / sizeof(Klass*);
      // Check for overflowing; use unsigned compare to handle the negative case.
      if( depth < ciKlass::primary_super_limit() ) {
        // The field is an element of Klass::_primary_supers.  Return its (constant) value.
        // (Folds up type checking code.)
        assert(Opcode() == Op_LoadKlass, "must load a klass from _primary_supers");
        ciKlass *ss = klass->super_of_depth(depth);
        return ss ? TypeKlassPtr::make(ss) : TypePtr::NULL_PTR;
      }
      const Type* aift = load_array_final_field(tkls, klass);
      if (aift != NULL)  return aift;
    }

    // We can still check if we are loading from the primary_supers array at a
    // shallow enough depth.  Even though the klass is not exact, entries less
    // than or equal to its super depth are correct.
    if (klass->is_loaded() ) {
      ciType *inner = klass;
      while( inner->is_obj_array_klass() )
        inner = inner->as_obj_array_klass()->base_element_type();
      if( inner->is_instance_klass() &&
          !inner->as_instance_klass()->flags().is_interface() ) {
        // Compute index into primary_supers array
        juint depth = (tkls->offset() - in_bytes(Klass::primary_supers_offset())) / sizeof(Klass*);
        // Check for overflowing; use unsigned compare to handle the negative case.
        if( depth < ciKlass::primary_super_limit() &&
            depth <= klass->super_depth() ) { // allow self-depth checks to handle self-check case
          // The field is an element of Klass::_primary_supers.  Return its (constant) value.
          // (Folds up type checking code.)
          assert(Opcode() == Op_LoadKlass, "must load a klass from _primary_supers");
          ciKlass *ss = klass->super_of_depth(depth);
          return ss ? TypeKlassPtr::make(ss) : TypePtr::NULL_PTR;
        }
      }
    }

    // If the type is enough to determine that the thing is not an array,
    // we can give the layout_helper a positive interval type.
    // This will help short-circuit some reflective code.
    if (tkls->offset() == in_bytes(Klass::layout_helper_offset())
        && !klass->is_array_klass() // not directly typed as an array
        && !klass->is_interface()  // specifically not Serializable & Cloneable
        && !klass->is_java_lang_Object()   // not the supertype of all T[]
        ) {
      // Note:  When interfaces are reliable, we can narrow the interface
      // test to (klass != Serializable && klass != Cloneable).
      assert(Opcode() == Op_LoadI, "must load an int from _layout_helper");
      jint min_size = Klass::instance_layout_helper(oopDesc::header_size(), false);
      // The key property of this type is that it folds up tests
      // for array-ness, since it proves that the layout_helper is positive.
      // Thus, a generic value like the basic object layout helper works fine.
      return TypeInt::make(min_size, max_jint, Type::WidenMin);
    }
  }

  // If we are loading from a freshly-allocated object, produce a zero,
  // if the load is provably beyond the header of the object.
  // (Also allow a variable load from a fresh array to produce zero.)
  const TypeOopPtr *tinst = tp->isa_oopptr();
  bool is_instance = (tinst != NULL) && tinst->is_known_instance_field();
  bool is_boxed_value = (tinst != NULL) && tinst->is_ptr_to_boxed_value();
  if (ReduceFieldZeroing || is_instance || is_boxed_value) {
    Node* value = can_see_stored_value(mem,phase);
    if (value != NULL && value->is_Con()) {
      assert(value->bottom_type()->higher_equal(_type),"sanity");
      return value->bottom_type();
    }
  }

  bool is_vect = (_type->isa_vect() != NULL);
  if (is_instance && !is_vect) {
    // If we have an instance type and our memory input is the
    // programs's initial memory state, there is no matching store,
    // so just return a zero of the appropriate type -
    // except if it is vectorized - then we have no zero constant.
    Node *mem = in(MemNode::Memory);
    if (mem->is_Parm() && mem->in(0)->is_Start()) {
      assert(mem->as_Parm()->_con == TypeFunc::Memory, "must be memory Parm");
      return Type::get_zero_type(_type->basic_type());
    }
  }

  Node* alloc = is_new_object_mark_load(phase);
  if (alloc != NULL) {
    return TypeX::make(markWord::prototype().value());
  }

  return _type;
}

//------------------------------match_edge-------------------------------------
// Do we Match on this edge index or not?  Match only the address.
uint LoadNode::match_edge(uint idx) const {
  return idx == MemNode::Address;
}

//--------------------------LoadBNode::Ideal--------------------------------------
//
//  If the previous store is to the same address as this load,
//  and the value stored was larger than a byte, replace this load
//  with the value stored truncated to a byte.  If no truncation is
//  needed, the replacement is done in LoadNode::Identity().
//
Node* LoadBNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  Node* mem = in(MemNode::Memory);
  Node* value = can_see_stored_value(mem,phase);
  if (value != NULL) {
    Node* narrow = Compile::narrow_value(T_BYTE, value, _type, phase, false);
    if (narrow != value) {
      return narrow;
    }
  }
  // Identity call will handle the case where truncation is not needed.
  return LoadNode::Ideal(phase, can_reshape);
}

const Type* LoadBNode::Value(PhaseGVN* phase) const {
  Node* mem = in(MemNode::Memory);
  Node* value = can_see_stored_value(mem,phase);
  if (value != NULL && value->is_Con() &&
      !value->bottom_type()->higher_equal(_type)) {
    // If the input to the store does not fit with the load's result type,
    // it must be truncated. We can't delay until Ideal call since
    // a singleton Value is needed for split_thru_phi optimization.
    int con = value->get_int();
    return TypeInt::make((con << 24) >> 24);
  }
  return LoadNode::Value(phase);
}

//--------------------------LoadUBNode::Ideal-------------------------------------
//
//  If the previous store is to the same address as this load,
//  and the value stored was larger than a byte, replace this load
//  with the value stored truncated to a byte.  If no truncation is
//  needed, the replacement is done in LoadNode::Identity().
//
Node* LoadUBNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  Node* mem = in(MemNode::Memory);
  Node* value = can_see_stored_value(mem, phase);
  if (value != NULL) {
    Node* narrow = Compile::narrow_value(T_BOOLEAN, value, _type, phase, false);
    if (narrow != value) {
      return narrow;
    }
  }
  // Identity call will handle the case where truncation is not needed.
  return LoadNode::Ideal(phase, can_reshape);
}

const Type* LoadUBNode::Value(PhaseGVN* phase) const {
  Node* mem = in(MemNode::Memory);
  Node* value = can_see_stored_value(mem,phase);
  if (value != NULL && value->is_Con() &&
      !value->bottom_type()->higher_equal(_type)) {
    // If the input to the store does not fit with the load's result type,
    // it must be truncated. We can't delay until Ideal call since
    // a singleton Value is needed for split_thru_phi optimization.
    int con = value->get_int();
    return TypeInt::make(con & 0xFF);
  }
  return LoadNode::Value(phase);
}

//--------------------------LoadUSNode::Ideal-------------------------------------
//
//  If the previous store is to the same address as this load,
//  and the value stored was larger than a char, replace this load
//  with the value stored truncated to a char.  If no truncation is
//  needed, the replacement is done in LoadNode::Identity().
//
Node* LoadUSNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  Node* mem = in(MemNode::Memory);
  Node* value = can_see_stored_value(mem,phase);
  if (value != NULL) {
    Node* narrow = Compile::narrow_value(T_CHAR, value, _type, phase, false);
    if (narrow != value) {
      return narrow;
    }
  }
  // Identity call will handle the case where truncation is not needed.
  return LoadNode::Ideal(phase, can_reshape);
}

const Type* LoadUSNode::Value(PhaseGVN* phase) const {
  Node* mem = in(MemNode::Memory);
  Node* value = can_see_stored_value(mem,phase);
  if (value != NULL && value->is_Con() &&
      !value->bottom_type()->higher_equal(_type)) {
    // If the input to the store does not fit with the load's result type,
    // it must be truncated. We can't delay until Ideal call since
    // a singleton Value is needed for split_thru_phi optimization.
    int con = value->get_int();
    return TypeInt::make(con & 0xFFFF);
  }
  return LoadNode::Value(phase);
}

//--------------------------LoadSNode::Ideal--------------------------------------
//
//  If the previous store is to the same address as this load,
//  and the value stored was larger than a short, replace this load
//  with the value stored truncated to a short.  If no truncation is
//  needed, the replacement is done in LoadNode::Identity().
//
Node* LoadSNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  Node* mem = in(MemNode::Memory);
  Node* value = can_see_stored_value(mem,phase);
  if (value != NULL) {
    Node* narrow = Compile::narrow_value(T_SHORT, value, _type, phase, false);
    if (narrow != value) {
      return narrow;
    }
  }
  // Identity call will handle the case where truncation is not needed.
  return LoadNode::Ideal(phase, can_reshape);
}

const Type* LoadSNode::Value(PhaseGVN* phase) const {
  Node* mem = in(MemNode::Memory);
  Node* value = can_see_stored_value(mem,phase);
  if (value != NULL && value->is_Con() &&
      !value->bottom_type()->higher_equal(_type)) {
    // If the input to the store does not fit with the load's result type,
    // it must be truncated. We can't delay until Ideal call since
    // a singleton Value is needed for split_thru_phi optimization.
    int con = value->get_int();
    return TypeInt::make((con << 16) >> 16);
  }
  return LoadNode::Value(phase);
}

//=============================================================================
//----------------------------LoadKlassNode::make------------------------------
// Polymorphic factory method:
Node* LoadKlassNode::make(PhaseGVN& gvn, Node* ctl, Node* mem, Node* adr, const TypePtr* at, const TypeKlassPtr* tk) {
  // sanity check the alias category against the created node type
  const TypePtr *adr_type = adr->bottom_type()->isa_ptr();
  assert(adr_type != NULL, "expecting TypeKlassPtr");
#ifdef _LP64
  if (adr_type->is_ptr_to_narrowklass()) {
    assert(UseCompressedClassPointers, "no compressed klasses");
    Node* load_klass = gvn.transform(new LoadNKlassNode(ctl, mem, adr, at, tk->make_narrowklass(), MemNode::unordered));
    return new DecodeNKlassNode(load_klass, load_klass->bottom_type()->make_ptr());
  }
#endif
  assert(!adr_type->is_ptr_to_narrowklass() && !adr_type->is_ptr_to_narrowoop(), "should have got back a narrow oop");
  return new LoadKlassNode(ctl, mem, adr, at, tk, MemNode::unordered);
}

//------------------------------Value------------------------------------------
const Type* LoadKlassNode::Value(PhaseGVN* phase) const {
  return klass_value_common(phase);
}

// In most cases, LoadKlassNode does not have the control input set. If the control
// input is set, it must not be removed (by LoadNode::Ideal()).
bool LoadKlassNode::can_remove_control() const {
  return false;
}

const Type* LoadNode::klass_value_common(PhaseGVN* phase) const {
  // Either input is TOP ==> the result is TOP
  const Type *t1 = phase->type( in(MemNode::Memory) );
  if (t1 == Type::TOP)  return Type::TOP;
  Node *adr = in(MemNode::Address);
  const Type *t2 = phase->type( adr );
  if (t2 == Type::TOP)  return Type::TOP;
  const TypePtr *tp = t2->is_ptr();
  if (TypePtr::above_centerline(tp->ptr()) ||
      tp->ptr() == TypePtr::Null)  return Type::TOP;

  // Return a more precise klass, if possible
  const TypeInstPtr *tinst = tp->isa_instptr();
  if (tinst != NULL) {
    ciInstanceKlass* ik = tinst->klass()->as_instance_klass();
    int offset = tinst->offset();
    if (ik == phase->C->env()->Class_klass()
        && (offset == java_lang_Class::klass_offset() ||
            offset == java_lang_Class::array_klass_offset())) {
      // We are loading a special hidden field from a Class mirror object,
      // the field which points to the VM's Klass metaobject.
      ciType* t = tinst->java_mirror_type();
      // java_mirror_type returns non-null for compile-time Class constants.
      if (t != NULL) {
        // constant oop => constant klass
        if (offset == java_lang_Class::array_klass_offset()) {
          if (t->is_void()) {
            // We cannot create a void array.  Since void is a primitive type return null
            // klass.  Users of this result need to do a null check on the returned klass.
            return TypePtr::NULL_PTR;
          }
          return TypeKlassPtr::make(ciArrayKlass::make(t));
        }
        if (!t->is_klass()) {
          // a primitive Class (e.g., int.class) has NULL for a klass field
          return TypePtr::NULL_PTR;
        }
        // (Folds up the 1st indirection in aClassConstant.getModifiers().)
        return TypeKlassPtr::make(t->as_klass());
      }
      // non-constant mirror, so we can't tell what's going on
    }
    if( !ik->is_loaded() )
      return _type;             // Bail out if not loaded
    if (offset == oopDesc::klass_offset_in_bytes()) {
      if (tinst->klass_is_exact()) {
        return TypeKlassPtr::make(ik);
      }
      // See if we can become precise: no subklasses and no interface
      // (Note:  We need to support verified interfaces.)
      if (!ik->is_interface() && !ik->has_subklass()) {
        // Add a dependence; if any subclass added we need to recompile
        if (!ik->is_final()) {
          // %%% should use stronger assert_unique_concrete_subtype instead
          phase->C->dependencies()->assert_leaf_type(ik);
        }
        // Return precise klass
        return TypeKlassPtr::make(ik);
      }

      // Return root of possible klass
      return TypeKlassPtr::make(TypePtr::NotNull, ik, 0/*offset*/);
    }
  }

  // Check for loading klass from an array
  const TypeAryPtr *tary = tp->isa_aryptr();
  if( tary != NULL ) {
    ciKlass *tary_klass = tary->klass();
    if (tary_klass != NULL   // can be NULL when at BOTTOM or TOP
        && tary->offset() == oopDesc::klass_offset_in_bytes()) {
      if (tary->klass_is_exact()) {
        return TypeKlassPtr::make(tary_klass);
      }
      ciArrayKlass *ak = tary->klass()->as_array_klass();
      // If the klass is an object array, we defer the question to the
      // array component klass.
      if( ak->is_obj_array_klass() ) {
        assert( ak->is_loaded(), "" );
        ciKlass *base_k = ak->as_obj_array_klass()->base_element_klass();
        if( base_k->is_loaded() && base_k->is_instance_klass() ) {
          ciInstanceKlass* ik = base_k->as_instance_klass();
          // See if we can become precise: no subklasses and no interface
          if (!ik->is_interface() && !ik->has_subklass()) {
            // Add a dependence; if any subclass added we need to recompile
            if (!ik->is_final()) {
              phase->C->dependencies()->assert_leaf_type(ik);
            }
            // Return precise array klass
            return TypeKlassPtr::make(ak);
          }
        }
        return TypeKlassPtr::make(TypePtr::NotNull, ak, 0/*offset*/);
      } else {                  // Found a type-array?
        assert( ak->is_type_array_klass(), "" );
        return TypeKlassPtr::make(ak); // These are always precise
      }
    }
  }

  // Check for loading klass from an array klass
  const TypeKlassPtr *tkls = tp->isa_klassptr();
  if (tkls != NULL && !StressReflectiveCode) {
    ciKlass* klass = tkls->klass();
    if( !klass->is_loaded() )
      return _type;             // Bail out if not loaded
    if( klass->is_obj_array_klass() &&
        tkls->offset() == in_bytes(ObjArrayKlass::element_klass_offset())) {
      ciKlass* elem = klass->as_obj_array_klass()->element_klass();
      // // Always returning precise element type is incorrect,
      // // e.g., element type could be object and array may contain strings
      // return TypeKlassPtr::make(TypePtr::Constant, elem, 0);

      // The array's TypeKlassPtr was declared 'precise' or 'not precise'
      // according to the element type's subclassing.
      return TypeKlassPtr::make(tkls->ptr(), elem, 0/*offset*/);
    }
    if( klass->is_instance_klass() && tkls->klass_is_exact() &&
        tkls->offset() == in_bytes(Klass::super_offset())) {
      ciKlass* sup = klass->as_instance_klass()->super();
      // The field is Klass::_super.  Return its (constant) value.
      // (Folds up the 2nd indirection in aClassConstant.getSuperClass().)
      return sup ? TypeKlassPtr::make(sup) : TypePtr::NULL_PTR;
    }
  }

  // Bailout case
  return LoadNode::Value(phase);
}

//------------------------------Identity---------------------------------------
// To clean up reflective code, simplify k.java_mirror.as_klass to plain k.
// Also feed through the klass in Allocate(...klass...)._klass.
Node* LoadKlassNode::Identity(PhaseGVN* phase) {
  return klass_identity_common(phase);
}

Node* LoadNode::klass_identity_common(PhaseGVN* phase) {
  Node* x = LoadNode::Identity(phase);
  if (x != this)  return x;

  // Take apart the address into an oop and and offset.
  // Return 'this' if we cannot.
  Node*    adr    = in(MemNode::Address);
  intptr_t offset = 0;
  Node*    base   = AddPNode::Ideal_base_and_offset(adr, phase, offset);
  if (base == NULL)     return this;
  const TypeOopPtr* toop = phase->type(adr)->isa_oopptr();
  if (toop == NULL)     return this;

  // Step over potential GC barrier for OopHandle resolve
  BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
  if (bs->is_gc_barrier_node(base)) {
    base = bs->step_over_gc_barrier(base);
  }

  // We can fetch the klass directly through an AllocateNode.
  // This works even if the klass is not constant (clone or newArray).
  if (offset == oopDesc::klass_offset_in_bytes()) {
    Node* allocated_klass = AllocateNode::Ideal_klass(base, phase);
    if (allocated_klass != NULL) {
      return allocated_klass;
    }
  }

  // Simplify k.java_mirror.as_klass to plain k, where k is a Klass*.
  // See inline_native_Class_query for occurrences of these patterns.
  // Java Example:  x.getClass().isAssignableFrom(y)
  //
  // This improves reflective code, often making the Class
  // mirror go completely dead.  (Current exception:  Class
  // mirrors may appear in debug info, but we could clean them out by
  // introducing a new debug info operator for Klass.java_mirror).

  if (toop->isa_instptr() && toop->klass() == phase->C->env()->Class_klass()
      && offset == java_lang_Class::klass_offset()) {
    if (base->is_Load()) {
      Node* base2 = base->in(MemNode::Address);
      if (base2->is_Load()) { /* direct load of a load which is the OopHandle */
        Node* adr2 = base2->in(MemNode::Address);
        const TypeKlassPtr* tkls = phase->type(adr2)->isa_klassptr();
        if (tkls != NULL && !tkls->empty()
            && (tkls->klass()->is_instance_klass() ||
              tkls->klass()->is_array_klass())
            && adr2->is_AddP()
           ) {
          int mirror_field = in_bytes(Klass::java_mirror_offset());
          if (tkls->offset() == mirror_field) {
            return adr2->in(AddPNode::Base);
          }
        }
      }
    }
  }

  return this;
}


//------------------------------Value------------------------------------------
const Type* LoadNKlassNode::Value(PhaseGVN* phase) const {
  const Type *t = klass_value_common(phase);
  if (t == Type::TOP)
    return t;

  return t->make_narrowklass();
}

//------------------------------Identity---------------------------------------
// To clean up reflective code, simplify k.java_mirror.as_klass to narrow k.
// Also feed through the klass in Allocate(...klass...)._klass.
Node* LoadNKlassNode::Identity(PhaseGVN* phase) {
  Node *x = klass_identity_common(phase);

  const Type *t = phase->type( x );
  if( t == Type::TOP ) return x;
  if( t->isa_narrowklass()) return x;
  assert (!t->isa_narrowoop(), "no narrow oop here");

  return phase->transform(new EncodePKlassNode(x, t->make_narrowklass()));
}

//------------------------------Value-----------------------------------------
const Type* LoadRangeNode::Value(PhaseGVN* phase) const {
  // Either input is TOP ==> the result is TOP
  const Type *t1 = phase->type( in(MemNode::Memory) );
  if( t1 == Type::TOP ) return Type::TOP;
  Node *adr = in(MemNode::Address);
  const Type *t2 = phase->type( adr );
  if( t2 == Type::TOP ) return Type::TOP;
  const TypePtr *tp = t2->is_ptr();
  if (TypePtr::above_centerline(tp->ptr()))  return Type::TOP;
  const TypeAryPtr *tap = tp->isa_aryptr();
  if( !tap ) return _type;
  return tap->size();
}

//-------------------------------Ideal---------------------------------------
// Feed through the length in AllocateArray(...length...)._length.
Node *LoadRangeNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  Node* p = MemNode::Ideal_common(phase, can_reshape);
  if (p)  return (p == NodeSentinel) ? NULL : p;

  // Take apart the address into an oop and and offset.
  // Return 'this' if we cannot.
  Node*    adr    = in(MemNode::Address);
  intptr_t offset = 0;
  Node*    base   = AddPNode::Ideal_base_and_offset(adr, phase,  offset);
  if (base == NULL)     return NULL;
  const TypeAryPtr* tary = phase->type(adr)->isa_aryptr();
  if (tary == NULL)     return NULL;

  // We can fetch the length directly through an AllocateArrayNode.
  // This works even if the length is not constant (clone or newArray).
  if (offset == arrayOopDesc::length_offset_in_bytes()) {
    AllocateArrayNode* alloc = AllocateArrayNode::Ideal_array_allocation(base, phase);
    if (alloc != NULL) {
      Node* allocated_length = alloc->Ideal_length();
      Node* len = alloc->make_ideal_length(tary, phase);
      if (allocated_length != len) {
        // New CastII improves on this.
        return len;
      }
    }
  }

  return NULL;
}

//------------------------------Identity---------------------------------------
// Feed through the length in AllocateArray(...length...)._length.
Node* LoadRangeNode::Identity(PhaseGVN* phase) {
  Node* x = LoadINode::Identity(phase);
  if (x != this)  return x;

  // Take apart the address into an oop and and offset.
  // Return 'this' if we cannot.
  Node*    adr    = in(MemNode::Address);
  intptr_t offset = 0;
  Node*    base   = AddPNode::Ideal_base_and_offset(adr, phase, offset);
  if (base == NULL)     return this;
  const TypeAryPtr* tary = phase->type(adr)->isa_aryptr();
  if (tary == NULL)     return this;

  // We can fetch the length directly through an AllocateArrayNode.
  // This works even if the length is not constant (clone or newArray).
  if (offset == arrayOopDesc::length_offset_in_bytes()) {
    AllocateArrayNode* alloc = AllocateArrayNode::Ideal_array_allocation(base, phase);
    if (alloc != NULL) {
      Node* allocated_length = alloc->Ideal_length();
      // Do not allow make_ideal_length to allocate a CastII node.
      Node* len = alloc->make_ideal_length(tary, phase, false);
      if (allocated_length == len) {
        // Return allocated_length only if it would not be improved by a CastII.
        return allocated_length;
      }
    }
  }

  return this;

}

//=============================================================================
//---------------------------StoreNode::make-----------------------------------
// Polymorphic factory method:
StoreNode* StoreNode::make(PhaseGVN& gvn, Node* ctl, Node* mem, Node* adr, const TypePtr* adr_type, Node* val, BasicType bt, MemOrd mo) {
  assert((mo == unordered || mo == release), "unexpected");
  Compile* C = gvn.C;
  assert(C->get_alias_index(adr_type) != Compile::AliasIdxRaw ||
         ctl != NULL, "raw memory operations should have control edge");

  switch (bt) {
  case T_BOOLEAN: val = gvn.transform(new AndINode(val, gvn.intcon(0x1))); // Fall through to T_BYTE case
  case T_BYTE:    return new StoreBNode(ctl, mem, adr, adr_type, val, mo);
  case T_INT:     return new StoreINode(ctl, mem, adr, adr_type, val, mo);
  case T_CHAR:
  case T_SHORT:   return new StoreCNode(ctl, mem, adr, adr_type, val, mo);
  case T_LONG:    return new StoreLNode(ctl, mem, adr, adr_type, val, mo);
  case T_FLOAT:   return new StoreFNode(ctl, mem, adr, adr_type, val, mo);
  case T_DOUBLE:  return new StoreDNode(ctl, mem, adr, adr_type, val, mo);
  case T_METADATA:
  case T_ADDRESS:
  case T_OBJECT:
#ifdef _LP64
    if (adr->bottom_type()->is_ptr_to_narrowoop()) {
      val = gvn.transform(new EncodePNode(val, val->bottom_type()->make_narrowoop()));
      return new StoreNNode(ctl, mem, adr, adr_type, val, mo);
    } else if (adr->bottom_type()->is_ptr_to_narrowklass() ||
               (UseCompressedClassPointers && val->bottom_type()->isa_klassptr() &&
                adr->bottom_type()->isa_rawptr())) {
      val = gvn.transform(new EncodePKlassNode(val, val->bottom_type()->make_narrowklass()));
      return new StoreNKlassNode(ctl, mem, adr, adr_type, val, mo);
    }
#endif
    {
      return new StorePNode(ctl, mem, adr, adr_type, val, mo);
    }
  default:
    ShouldNotReachHere();
    return (StoreNode*)NULL;
  }
}

StoreLNode* StoreLNode::make_atomic(Node* ctl, Node* mem, Node* adr, const TypePtr* adr_type, Node* val, MemOrd mo) {
  bool require_atomic = true;
  return new StoreLNode(ctl, mem, adr, adr_type, val, mo, require_atomic);
}

StoreDNode* StoreDNode::make_atomic(Node* ctl, Node* mem, Node* adr, const TypePtr* adr_type, Node* val, MemOrd mo) {
  bool require_atomic = true;
  return new StoreDNode(ctl, mem, adr, adr_type, val, mo, require_atomic);
}


//--------------------------bottom_type----------------------------------------
const Type *StoreNode::bottom_type() const {
  return Type::MEMORY;
}

//------------------------------hash-------------------------------------------
uint StoreNode::hash() const {
  // unroll addition of interesting fields
  //return (uintptr_t)in(Control) + (uintptr_t)in(Memory) + (uintptr_t)in(Address) + (uintptr_t)in(ValueIn);

  // Since they are not commoned, do not hash them:
  return NO_HASH;
}

//------------------------------Ideal------------------------------------------
// Change back-to-back Store(, p, x) -> Store(m, p, y) to Store(m, p, x).
// When a store immediately follows a relevant allocation/initialization,
// try to capture it into the initialization, or hoist it above.
Node *StoreNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  Node* p = MemNode::Ideal_common(phase, can_reshape);
  if (p)  return (p == NodeSentinel) ? NULL : p;

  Node* mem     = in(MemNode::Memory);
  Node* address = in(MemNode::Address);
  Node* value   = in(MemNode::ValueIn);
  // Back-to-back stores to same address?  Fold em up.  Generally
  // unsafe if I have intervening uses...  Also disallowed for StoreCM
  // since they must follow each StoreP operation.  Redundant StoreCMs
  // are eliminated just before matching in final_graph_reshape.
  {
    Node* st = mem;
    // If Store 'st' has more than one use, we cannot fold 'st' away.
    // For example, 'st' might be the final state at a conditional
    // return.  Or, 'st' might be used by some node which is live at
    // the same time 'st' is live, which might be unschedulable.  So,
    // require exactly ONE user until such time as we clone 'mem' for
    // each of 'mem's uses (thus making the exactly-1-user-rule hold
    // true).
    while (st->is_Store() && st->outcnt() == 1 && st->Opcode() != Op_StoreCM) {
      // Looking at a dead closed cycle of memory?
      assert(st != st->in(MemNode::Memory), "dead loop in StoreNode::Ideal");
      assert(Opcode() == st->Opcode() ||
             st->Opcode() == Op_StoreVector ||
             Opcode() == Op_StoreVector ||
             st->Opcode() == Op_StoreVectorScatter ||
             Opcode() == Op_StoreVectorScatter ||
             phase->C->get_alias_index(adr_type()) == Compile::AliasIdxRaw ||
             (Opcode() == Op_StoreL && st->Opcode() == Op_StoreI) || // expanded ClearArrayNode
             (Opcode() == Op_StoreI && st->Opcode() == Op_StoreL) || // initialization by arraycopy
             (is_mismatched_access() || st->as_Store()->is_mismatched_access()),
             "no mismatched stores, except on raw memory: %s %s", NodeClassNames[Opcode()], NodeClassNames[st->Opcode()]);

      if (st->in(MemNode::Address)->eqv_uncast(address) &&
          st->as_Store()->memory_size() <= this->memory_size()) {
        Node* use = st->raw_out(0);
        if (phase->is_IterGVN()) {
          phase->is_IterGVN()->rehash_node_delayed(use);
        }
        // It's OK to do this in the parser, since DU info is always accurate,
        // and the parser always refers to nodes via SafePointNode maps.
        use->set_req_X(MemNode::Memory, st->in(MemNode::Memory), phase);
        return this;
      }
      st = st->in(MemNode::Memory);
    }
  }


  // Capture an unaliased, unconditional, simple store into an initializer.
  // Or, if it is independent of the allocation, hoist it above the allocation.
  if (ReduceFieldZeroing && /*can_reshape &&*/
      mem->is_Proj() && mem->in(0)->is_Initialize()) {
    InitializeNode* init = mem->in(0)->as_Initialize();
    intptr_t offset = init->can_capture_store(this, phase, can_reshape);
    if (offset > 0) {
      Node* moved = init->capture_store(this, offset, phase, can_reshape);
      // If the InitializeNode captured me, it made a raw copy of me,
      // and I need to disappear.
      if (moved != NULL) {
        // %%% hack to ensure that Ideal returns a new node:
        mem = MergeMemNode::make(mem);
        return mem;             // fold me away
      }
    }
  }

  // Fold reinterpret cast into memory operation:
  //    StoreX mem (MoveY2X v) => StoreY mem v
  if (value->is_Move()) {
    const Type* vt = value->in(1)->bottom_type();
    if (has_reinterpret_variant(vt)) {
      if (phase->C->post_loop_opts_phase()) {
        return convert_to_reinterpret_store(*phase, value->in(1), vt);
      } else {
        phase->C->record_for_post_loop_opts_igvn(this); // attempt the transformation once loop opts are over
      }
    }
  }

  return NULL;                  // No further progress
}

//------------------------------Value-----------------------------------------
const Type* StoreNode::Value(PhaseGVN* phase) const {
  // Either input is TOP ==> the result is TOP
  const Type *t1 = phase->type( in(MemNode::Memory) );
  if( t1 == Type::TOP ) return Type::TOP;
  const Type *t2 = phase->type( in(MemNode::Address) );
  if( t2 == Type::TOP ) return Type::TOP;
  const Type *t3 = phase->type( in(MemNode::ValueIn) );
  if( t3 == Type::TOP ) return Type::TOP;
  return Type::MEMORY;
}

//------------------------------Identity---------------------------------------
// Remove redundant stores:
//   Store(m, p, Load(m, p)) changes to m.
//   Store(, p, x) -> Store(m, p, x) changes to Store(m, p, x).
Node* StoreNode::Identity(PhaseGVN* phase) {
  Node* mem = in(MemNode::Memory);
  Node* adr = in(MemNode::Address);
  Node* val = in(MemNode::ValueIn);

  Node* result = this;

  // Load then Store?  Then the Store is useless
  if (val->is_Load() &&
      val->in(MemNode::Address)->eqv_uncast(adr) &&
      val->in(MemNode::Memory )->eqv_uncast(mem) &&
      val->as_Load()->store_Opcode() == Opcode()) {
    result = mem;
  }

  // Two stores in a row of the same value?
  if (result == this &&
      mem->is_Store() &&
      mem->in(MemNode::Address)->eqv_uncast(adr) &&
      mem->in(MemNode::ValueIn)->eqv_uncast(val) &&
      mem->Opcode() == Opcode()) {
    result = mem;
  }

  // Store of zero anywhere into a freshly-allocated object?
  // Then the store is useless.
  // (It must already have been captured by the InitializeNode.)
  if (result == this &&
      ReduceFieldZeroing && phase->type(val)->is_zero_type()) {
    // a newly allocated object is already all-zeroes everywhere
    if (mem->is_Proj() && mem->in(0)->is_Allocate()) {
      result = mem;
    }

    if (result == this) {
      // the store may also apply to zero-bits in an earlier object
      Node* prev_mem = find_previous_store(phase);
      // Steps (a), (b):  Walk past independent stores to find an exact match.
      if (prev_mem != NULL) {
        Node* prev_val = can_see_stored_value(prev_mem, phase);
        if (prev_val != NULL && prev_val == val) {
          // prev_val and val might differ by a cast; it would be good
          // to keep the more informative of the two.
          result = mem;
        }
      }
    }
  }

  PhaseIterGVN* igvn = phase->is_IterGVN();
  if (result != this && igvn != NULL) {
    MemBarNode* trailing = trailing_membar();
    if (trailing != NULL) {
#ifdef ASSERT
      const TypeOopPtr* t_oop = phase->type(in(Address))->isa_oopptr();
      assert(t_oop == NULL || t_oop->is_known_instance_field(), "only for non escaping objects");
#endif
      trailing->remove(igvn);
    }
  }

  return result;
}

//------------------------------match_edge-------------------------------------
// Do we Match on this edge index or not?  Match only memory & value
uint StoreNode::match_edge(uint idx) const {
  return idx == MemNode::Address || idx == MemNode::ValueIn;
}

//------------------------------cmp--------------------------------------------
// Do not common stores up together.  They generally have to be split
// back up anyways, so do not bother.
bool StoreNode::cmp( const Node &n ) const {
  return (&n == this);          // Always fail except on self
}

//------------------------------Ideal_masked_input-----------------------------
// Check for a useless mask before a partial-word store
// (StoreB ... (AndI valIn conIa) )
// If (conIa & mask == mask) this simplifies to
// (StoreB ... (valIn) )
Node *StoreNode::Ideal_masked_input(PhaseGVN *phase, uint mask) {
  Node *val = in(MemNode::ValueIn);
  if( val->Opcode() == Op_AndI ) {
    const TypeInt *t = phase->type( val->in(2) )->isa_int();
    if( t && t->is_con() && (t->get_con() & mask) == mask ) {
      set_req_X(MemNode::ValueIn, val->in(1), phase);
      return this;
    }
  }
  return NULL;
}


//------------------------------Ideal_sign_extended_input----------------------
// Check for useless sign-extension before a partial-word store
// (StoreB ... (RShiftI _ (LShiftI _ valIn conIL ) conIR) )
// If (conIL == conIR && conIR <= num_bits)  this simplifies to
// (StoreB ... (valIn) )
Node *StoreNode::Ideal_sign_extended_input(PhaseGVN *phase, int num_bits) {
  Node *val = in(MemNode::ValueIn);
  if( val->Opcode() == Op_RShiftI ) {
    const TypeInt *t = phase->type( val->in(2) )->isa_int();
    if( t && t->is_con() && (t->get_con() <= num_bits) ) {
      Node *shl = val->in(1);
      if( shl->Opcode() == Op_LShiftI ) {
        const TypeInt *t2 = phase->type( shl->in(2) )->isa_int();
        if( t2 && t2->is_con() && (t2->get_con() == t->get_con()) ) {
          set_req_X(MemNode::ValueIn, shl->in(1), phase);
          return this;
        }
      }
    }
  }
  return NULL;
}

//------------------------------value_never_loaded-----------------------------------
// Determine whether there are any possible loads of the value stored.
// For simplicity, we actually check if there are any loads from the
// address stored to, not just for loads of the value stored by this node.
//
bool StoreNode::value_never_loaded( PhaseTransform *phase) const {
  Node *adr = in(Address);
  const TypeOopPtr *adr_oop = phase->type(adr)->isa_oopptr();
  if (adr_oop == NULL)
    return false;
  if (!adr_oop->is_known_instance_field())
    return false; // if not a distinct instance, there may be aliases of the address
  for (DUIterator_Fast imax, i = adr->fast_outs(imax); i < imax; i++) {
    Node *use = adr->fast_out(i);
    if (use->is_Load() || use->is_LoadStore()) {
      return false;
    }
  }
  return true;
}

MemBarNode* StoreNode::trailing_membar() const {
  if (is_release()) {
    MemBarNode* trailing_mb = NULL;
    for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
      Node* u = fast_out(i);
      if (u->is_MemBar()) {
        if (u->as_MemBar()->trailing_store()) {
          assert(u->Opcode() == Op_MemBarVolatile, "");
          assert(trailing_mb == NULL, "only one");
          trailing_mb = u->as_MemBar();
#ifdef ASSERT
          Node* leading = u->as_MemBar()->leading_membar();
          assert(leading->Opcode() == Op_MemBarRelease, "incorrect membar");
          assert(leading->as_MemBar()->leading_store(), "incorrect membar pair");
          assert(leading->as_MemBar()->trailing_membar() == u, "incorrect membar pair");
#endif
        } else {
          assert(u->as_MemBar()->standalone(), "");
        }
      }
    }
    return trailing_mb;
  }
  return NULL;
}


//=============================================================================
//------------------------------Ideal------------------------------------------
// If the store is from an AND mask that leaves the low bits untouched, then
// we can skip the AND operation.  If the store is from a sign-extension
// (a left shift, then right shift) we can skip both.
Node *StoreBNode::Ideal(PhaseGVN *phase, bool can_reshape){
  Node *progress = StoreNode::Ideal_masked_input(phase, 0xFF);
  if( progress != NULL ) return progress;

  progress = StoreNode::Ideal_sign_extended_input(phase, 24);
  if( progress != NULL ) return progress;

  // Finally check the default case
  return StoreNode::Ideal(phase, can_reshape);
}

//=============================================================================
//------------------------------Ideal------------------------------------------
// If the store is from an AND mask that leaves the low bits untouched, then
// we can skip the AND operation
Node *StoreCNode::Ideal(PhaseGVN *phase, bool can_reshape){
  Node *progress = StoreNode::Ideal_masked_input(phase, 0xFFFF);
  if( progress != NULL ) return progress;

  progress = StoreNode::Ideal_sign_extended_input(phase, 16);
  if( progress != NULL ) return progress;

  // Finally check the default case
  return StoreNode::Ideal(phase, can_reshape);
}

//=============================================================================
//------------------------------Identity---------------------------------------
Node* StoreCMNode::Identity(PhaseGVN* phase) {
  // No need to card mark when storing a null ptr
  Node* my_store = in(MemNode::OopStore);
  if (my_store->is_Store()) {
    const Type *t1 = phase->type( my_store->in(MemNode::ValueIn) );
    if( t1 == TypePtr::NULL_PTR ) {
      return in(MemNode::Memory);
    }
  }
  return this;
}

//=============================================================================
//------------------------------Ideal---------------------------------------
Node *StoreCMNode::Ideal(PhaseGVN *phase, bool can_reshape){
  Node* progress = StoreNode::Ideal(phase, can_reshape);
  if (progress != NULL) return progress;

  Node* my_store = in(MemNode::OopStore);
  if (my_store->is_MergeMem()) {
    Node* mem = my_store->as_MergeMem()->memory_at(oop_alias_idx());
    set_req_X(MemNode::OopStore, mem, phase);
    return this;
  }

  return NULL;
}

//------------------------------Value-----------------------------------------
const Type* StoreCMNode::Value(PhaseGVN* phase) const {
  // Either input is TOP ==> the result is TOP (checked in StoreNode::Value).
  // If extra input is TOP ==> the result is TOP
  const Type* t = phase->type(in(MemNode::OopStore));
  if (t == Type::TOP) {
    return Type::TOP;
  }
  return StoreNode::Value(phase);
}


//=============================================================================
//----------------------------------SCMemProjNode------------------------------
const Type* SCMemProjNode::Value(PhaseGVN* phase) const
{
  if (in(0) == NULL || phase->type(in(0)) == Type::TOP) {
    return Type::TOP;
  }
  return bottom_type();
}

//=============================================================================
//----------------------------------LoadStoreNode------------------------------
LoadStoreNode::LoadStoreNode( Node *c, Node *mem, Node *adr, Node *val, const TypePtr* at, const Type* rt, uint required )
  : Node(required),
    _type(rt),
    _adr_type(at),
    _barrier_data(0)
{
  init_req(MemNode::Control, c  );
  init_req(MemNode::Memory , mem);
  init_req(MemNode::Address, adr);
  init_req(MemNode::ValueIn, val);
  init_class_id(Class_LoadStore);
}

//------------------------------Value-----------------------------------------
const Type* LoadStoreNode::Value(PhaseGVN* phase) const {
  // Either input is TOP ==> the result is TOP
  if (!in(MemNode::Control) || phase->type(in(MemNode::Control)) == Type::TOP) {
    return Type::TOP;
  }
  const Type* t = phase->type(in(MemNode::Memory));
  if (t == Type::TOP) {
    return Type::TOP;
  }
  t = phase->type(in(MemNode::Address));
  if (t == Type::TOP) {
    return Type::TOP;
  }
  t = phase->type(in(MemNode::ValueIn));
  if (t == Type::TOP) {
    return Type::TOP;
  }
  return bottom_type();
}

uint LoadStoreNode::ideal_reg() const {
  return _type->ideal_reg();
}

bool LoadStoreNode::result_not_used() const {
  for( DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++ ) {
    Node *x = fast_out(i);
    if (x->Opcode() == Op_SCMemProj) continue;
    return false;
  }
  return true;
}

MemBarNode* LoadStoreNode::trailing_membar() const {
  MemBarNode* trailing = NULL;
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    Node* u = fast_out(i);
    if (u->is_MemBar()) {
      if (u->as_MemBar()->trailing_load_store()) {
        assert(u->Opcode() == Op_MemBarAcquire, "");
        assert(trailing == NULL, "only one");
        trailing = u->as_MemBar();
#ifdef ASSERT
        Node* leading = trailing->leading_membar();
        assert(support_IRIW_for_not_multiple_copy_atomic_cpu || leading->Opcode() == Op_MemBarRelease, "incorrect membar");
        assert(leading->as_MemBar()->leading_load_store(), "incorrect membar pair");
        assert(leading->as_MemBar()->trailing_membar() == trailing, "incorrect membar pair");
#endif
      } else {
        assert(u->as_MemBar()->standalone(), "wrong barrier kind");
      }
    }
  }

  return trailing;
}

uint LoadStoreNode::size_of() const { return sizeof(*this); }

//=============================================================================
//----------------------------------LoadStoreConditionalNode--------------------
LoadStoreConditionalNode::LoadStoreConditionalNode( Node *c, Node *mem, Node *adr, Node *val, Node *ex ) : LoadStoreNode(c, mem, adr, val, NULL, TypeInt::BOOL, 5) {
  init_req(ExpectedIn, ex );
}

const Type* LoadStoreConditionalNode::Value(PhaseGVN* phase) const {
  // Either input is TOP ==> the result is TOP
  const Type* t = phase->type(in(ExpectedIn));
  if (t == Type::TOP) {
    return Type::TOP;
  }
  return LoadStoreNode::Value(phase);
}

//=============================================================================
//-------------------------------adr_type--------------------------------------
const TypePtr* ClearArrayNode::adr_type() const {
  Node *adr = in(3);
  if (adr == NULL)  return NULL; // node is dead
  return MemNode::calculate_adr_type(adr->bottom_type());
}

//------------------------------match_edge-------------------------------------
// Do we Match on this edge index or not?  Do not match memory
uint ClearArrayNode::match_edge(uint idx) const {
  return idx > 1;
}

//------------------------------Identity---------------------------------------
// Clearing a zero length array does nothing
Node* ClearArrayNode::Identity(PhaseGVN* phase) {
  return phase->type(in(2))->higher_equal(TypeX::ZERO)  ? in(1) : this;
}

//------------------------------Idealize---------------------------------------
// Clearing a short array is faster with stores
Node *ClearArrayNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  // Already know this is a large node, do not try to ideal it
  if (!IdealizeClearArrayNode || _is_large) return NULL;

  const int unit = BytesPerLong;
  const TypeX* t = phase->type(in(2))->isa_intptr_t();
  if (!t)  return NULL;
  if (!t->is_con())  return NULL;
  intptr_t raw_count = t->get_con();
  intptr_t size = raw_count;
  if (!Matcher::init_array_count_is_in_bytes) size *= unit;
  // Clearing nothing uses the Identity call.
  // Negative clears are possible on dead ClearArrays
  // (see jck test stmt114.stmt11402.val).
  if (size <= 0 || size % unit != 0)  return NULL;
  intptr_t count = size / unit;
  // Length too long; communicate this to matchers and assemblers.
  // Assemblers are responsible to produce fast hardware clears for it.
  if (size > InitArrayShortSize) {
    return new ClearArrayNode(in(0), in(1), in(2), in(3), true);
  } else if (size > 2 && Matcher::match_rule_supported_vector(Op_ClearArray, 4, T_LONG)) {
    return NULL;
  }
  Node *mem = in(1);
  if( phase->type(mem)==Type::TOP ) return NULL;
  Node *adr = in(3);
  const Type* at = phase->type(adr);
  if( at==Type::TOP ) return NULL;
  const TypePtr* atp = at->isa_ptr();
  // adjust atp to be the correct array element address type
  if (atp == NULL)  atp = TypePtr::BOTTOM;
  else              atp = atp->add_offset(Type::OffsetBot);
  // Get base for derived pointer purposes
  if( adr->Opcode() != Op_AddP ) Unimplemented();
  Node *base = adr->in(1);

  Node *zero = phase->makecon(TypeLong::ZERO);
  Node *off  = phase->MakeConX(BytesPerLong);
  mem = new StoreLNode(in(0),mem,adr,atp,zero,MemNode::unordered,false);
  count--;
  while( count-- ) {
    mem = phase->transform(mem);
    adr = phase->transform(new AddPNode(base,adr,off));
    mem = new StoreLNode(in(0),mem,adr,atp,zero,MemNode::unordered,false);
  }
  return mem;
}

//----------------------------step_through----------------------------------
// Return allocation input memory edge if it is different instance
// or itself if it is the one we are looking for.
bool ClearArrayNode::step_through(Node** np, uint instance_id, PhaseTransform* phase) {
  Node* n = *np;
  assert(n->is_ClearArray(), "sanity");
  intptr_t offset;
  AllocateNode* alloc = AllocateNode::Ideal_allocation(n->in(3), phase, offset);
  // This method is called only before Allocate nodes are expanded
  // during macro nodes expansion. Before that ClearArray nodes are
  // only generated in PhaseMacroExpand::generate_arraycopy() (before
  // Allocate nodes are expanded) which follows allocations.
  assert(alloc != NULL, "should have allocation");
  if (alloc->_idx == instance_id) {
    // Can not bypass initialization of the instance we are looking for.
    return false;
  }
  // Otherwise skip it.
  InitializeNode* init = alloc->initialization();
  if (init != NULL)
    *np = init->in(TypeFunc::Memory);
  else
    *np = alloc->in(TypeFunc::Memory);
  return true;
}

//----------------------------clear_memory-------------------------------------
// Generate code to initialize object storage to zero.
Node* ClearArrayNode::clear_memory(Node* ctl, Node* mem, Node* dest,
                                   intptr_t start_offset,
                                   Node* end_offset,
                                   PhaseGVN* phase) {
  intptr_t offset = start_offset;

  int unit = BytesPerLong;
  if ((offset % unit) != 0) {
    Node* adr = new AddPNode(dest, dest, phase->MakeConX(offset));
    adr = phase->transform(adr);
    const TypePtr* atp = TypeRawPtr::BOTTOM;
    mem = StoreNode::make(*phase, ctl, mem, adr, atp, phase->zerocon(T_INT), T_INT, MemNode::unordered);
    mem = phase->transform(mem);
    offset += BytesPerInt;
  }
  assert((offset % unit) == 0, "");

  // Initialize the remaining stuff, if any, with a ClearArray.
  return clear_memory(ctl, mem, dest, phase->MakeConX(offset), end_offset, phase);
}

Node* ClearArrayNode::clear_memory(Node* ctl, Node* mem, Node* dest,
                                   Node* start_offset,
                                   Node* end_offset,
                                   PhaseGVN* phase) {
  if (start_offset == end_offset) {
    // nothing to do
    return mem;
  }

  int unit = BytesPerLong;
  Node* zbase = start_offset;
  Node* zend  = end_offset;

  // Scale to the unit required by the CPU:
  if (!Matcher::init_array_count_is_in_bytes) {
    Node* shift = phase->intcon(exact_log2(unit));
    zbase = phase->transform(new URShiftXNode(zbase, shift) );
    zend  = phase->transform(new URShiftXNode(zend,  shift) );
  }

  // Bulk clear double-words
  Node* zsize = phase->transform(new SubXNode(zend, zbase) );
  Node* adr = phase->transform(new AddPNode(dest, dest, start_offset) );
  mem = new ClearArrayNode(ctl, mem, zsize, adr, false);
  return phase->transform(mem);
}

Node* ClearArrayNode::clear_memory(Node* ctl, Node* mem, Node* dest,
                                   intptr_t start_offset,
                                   intptr_t end_offset,
                                   PhaseGVN* phase) {
  if (start_offset == end_offset) {
    // nothing to do
    return mem;
  }

  assert((end_offset % BytesPerInt) == 0, "odd end offset");
  intptr_t done_offset = end_offset;
  if ((done_offset % BytesPerLong) != 0) {
    done_offset -= BytesPerInt;
  }
  if (done_offset > start_offset) {
    mem = clear_memory(ctl, mem, dest,
                       start_offset, phase->MakeConX(done_offset), phase);
  }
  if (done_offset < end_offset) { // emit the final 32-bit store
    Node* adr = new AddPNode(dest, dest, phase->MakeConX(done_offset));
    adr = phase->transform(adr);
    const TypePtr* atp = TypeRawPtr::BOTTOM;
    mem = StoreNode::make(*phase, ctl, mem, adr, atp, phase->zerocon(T_INT), T_INT, MemNode::unordered);
    mem = phase->transform(mem);
    done_offset += BytesPerInt;
  }
  assert(done_offset == end_offset, "");
  return mem;
}

//=============================================================================
MemBarNode::MemBarNode(Compile* C, int alias_idx, Node* precedent)
  : MultiNode(TypeFunc::Parms + (precedent == NULL? 0: 1)),
    _adr_type(C->get_adr_type(alias_idx)), _kind(Standalone)
#ifdef ASSERT
  , _pair_idx(0)
#endif
{
  init_class_id(Class_MemBar);
  Node* top = C->top();
  init_req(TypeFunc::I_O,top);
  init_req(TypeFunc::FramePtr,top);
  init_req(TypeFunc::ReturnAdr,top);
  if (precedent != NULL)
    init_req(TypeFunc::Parms, precedent);
}

//------------------------------cmp--------------------------------------------
uint MemBarNode::hash() const { return NO_HASH; }
bool MemBarNode::cmp( const Node &n ) const {
  return (&n == this);          // Always fail except on self
}

//------------------------------make-------------------------------------------
MemBarNode* MemBarNode::make(Compile* C, int opcode, int atp, Node* pn) {
  switch (opcode) {
  case Op_MemBarAcquire:     return new MemBarAcquireNode(C, atp, pn);
  case Op_LoadFence:         return new LoadFenceNode(C, atp, pn);
  case Op_MemBarRelease:     return new MemBarReleaseNode(C, atp, pn);
  case Op_StoreFence:        return new StoreFenceNode(C, atp, pn);
  case Op_MemBarAcquireLock: return new MemBarAcquireLockNode(C, atp, pn);
  case Op_MemBarReleaseLock: return new MemBarReleaseLockNode(C, atp, pn);
  case Op_MemBarVolatile:    return new MemBarVolatileNode(C, atp, pn);
  case Op_MemBarCPUOrder:    return new MemBarCPUOrderNode(C, atp, pn);
  case Op_OnSpinWait:        return new OnSpinWaitNode(C, atp, pn);
  case Op_Initialize:        return new InitializeNode(C, atp, pn);
  case Op_MemBarStoreStore:  return new MemBarStoreStoreNode(C, atp, pn);
  case Op_Blackhole:         return new BlackholeNode(C, atp, pn);
  default: ShouldNotReachHere(); return NULL;
  }
}

void MemBarNode::remove(PhaseIterGVN *igvn) {
  if (outcnt() != 2) {
    assert(Opcode() == Op_Initialize, "Only seen when there are no use of init memory");
    assert(outcnt() == 1, "Only control then");
  }
  if (trailing_store() || trailing_load_store()) {
    MemBarNode* leading = leading_membar();
    if (leading != NULL) {
      assert(leading->trailing_membar() == this, "inconsistent leading/trailing membars");
      leading->remove(igvn);
    }
  }
  if (proj_out_or_null(TypeFunc::Memory) != NULL) {
    igvn->replace_node(proj_out(TypeFunc::Memory), in(TypeFunc::Memory));
  }
  if (proj_out_or_null(TypeFunc::Control) != NULL) {
    igvn->replace_node(proj_out(TypeFunc::Control), in(TypeFunc::Control));
  }
}

//------------------------------Ideal------------------------------------------
// Return a node which is more "ideal" than the current node.  Strip out
// control copies
Node *MemBarNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  if (remove_dead_region(phase, can_reshape)) return this;
  // Don't bother trying to transform a dead node
  if (in(0) && in(0)->is_top()) {
    return NULL;
  }

  bool progress = false;
  // Eliminate volatile MemBars for scalar replaced objects.
  if (can_reshape && req() == (Precedent+1)) {
    bool eliminate = false;
    int opc = Opcode();
    if ((opc == Op_MemBarAcquire || opc == Op_MemBarVolatile)) {
      // Volatile field loads and stores.
      Node* my_mem = in(MemBarNode::Precedent);
      // The MembarAquire may keep an unused LoadNode alive through the Precedent edge
      if ((my_mem != NULL) && (opc == Op_MemBarAcquire) && (my_mem->outcnt() == 1)) {
        // if the Precedent is a decodeN and its input (a Load) is used at more than one place,
        // replace this Precedent (decodeN) with the Load instead.
        if ((my_mem->Opcode() == Op_DecodeN) && (my_mem->in(1)->outcnt() > 1))  {
          Node* load_node = my_mem->in(1);
          set_req(MemBarNode::Precedent, load_node);
          phase->is_IterGVN()->_worklist.push(my_mem);
          my_mem = load_node;
        } else {
          assert(my_mem->unique_out() == this, "sanity");
          del_req(Precedent);
          phase->is_IterGVN()->_worklist.push(my_mem); // remove dead node later
          my_mem = NULL;
        }
        progress = true;
      }
      if (my_mem != NULL && my_mem->is_Mem()) {
        const TypeOopPtr* t_oop = my_mem->in(MemNode::Address)->bottom_type()->isa_oopptr();
        // Check for scalar replaced object reference.
        if( t_oop != NULL && t_oop->is_known_instance_field() &&
            t_oop->offset() != Type::OffsetBot &&
            t_oop->offset() != Type::OffsetTop) {
          eliminate = true;
        }
      }
    } else if (opc == Op_MemBarRelease) {
      // Final field stores.
      Node* alloc = AllocateNode::Ideal_allocation(in(MemBarNode::Precedent), phase);
      if ((alloc != NULL) && alloc->is_Allocate() &&
          alloc->as_Allocate()->does_not_escape_thread()) {
        // The allocated object does not escape.
        eliminate = true;
      }
    }
    if (eliminate) {
      // Replace MemBar projections by its inputs.
      PhaseIterGVN* igvn = phase->is_IterGVN();
      remove(igvn);
      // Must return either the original node (now dead) or a new node
      // (Do not return a top here, since that would break the uniqueness of top.)
      return new ConINode(TypeInt::ZERO);
    }
  }
  return progress ? this : NULL;
}

//------------------------------Value------------------------------------------
const Type* MemBarNode::Value(PhaseGVN* phase) const {
  if( !in(0) ) return Type::TOP;
  if( phase->type(in(0)) == Type::TOP )
    return Type::TOP;
  return TypeTuple::MEMBAR;
}

//------------------------------match------------------------------------------
// Construct projections for memory.
Node *MemBarNode::match( const ProjNode *proj, const Matcher *m ) {
  switch (proj->_con) {
  case TypeFunc::Control:
  case TypeFunc::Memory:
    return new MachProjNode(this,proj->_con,RegMask::Empty,MachProjNode::unmatched_proj);
  }
  ShouldNotReachHere();
  return NULL;
}

void MemBarNode::set_store_pair(MemBarNode* leading, MemBarNode* trailing) {
  trailing->_kind = TrailingStore;
  leading->_kind = LeadingStore;
#ifdef ASSERT
  trailing->_pair_idx = leading->_idx;
  leading->_pair_idx = leading->_idx;
#endif
}

void MemBarNode::set_load_store_pair(MemBarNode* leading, MemBarNode* trailing) {
  trailing->_kind = TrailingLoadStore;
  leading->_kind = LeadingLoadStore;
#ifdef ASSERT
  trailing->_pair_idx = leading->_idx;
  leading->_pair_idx = leading->_idx;
#endif
}

MemBarNode* MemBarNode::trailing_membar() const {
  ResourceMark rm;
  Node* trailing = (Node*)this;
  VectorSet seen;
  Node_Stack multis(0);
  do {
    Node* c = trailing;
    uint i = 0;
    do {
      trailing = NULL;
      for (; i < c->outcnt(); i++) {
        Node* next = c->raw_out(i);
        if (next != c && next->is_CFG()) {
          if (c->is_MultiBranch()) {
            if (multis.node() == c) {
              multis.set_index(i+1);
            } else {
              multis.push(c, i+1);
            }
          }
          trailing = next;
          break;
        }
      }
      if (trailing != NULL && !seen.test_set(trailing->_idx)) {
        break;
      }
      while (multis.size() > 0) {
        c = multis.node();
        i = multis.index();
        if (i < c->req()) {
          break;
        }
        multis.pop();
      }
    } while (multis.size() > 0);
  } while (!trailing->is_MemBar() || !trailing->as_MemBar()->trailing());

  MemBarNode* mb = trailing->as_MemBar();
  assert((mb->_kind == TrailingStore && _kind == LeadingStore) ||
         (mb->_kind == TrailingLoadStore && _kind == LeadingLoadStore), "bad trailing membar");
  assert(mb->_pair_idx == _pair_idx, "bad trailing membar");
  return mb;
}

MemBarNode* MemBarNode::leading_membar() const {
  ResourceMark rm;
  VectorSet seen;
  Node_Stack regions(0);
  Node* leading = in(0);
  while (leading != NULL && (!leading->is_MemBar() || !leading->as_MemBar()->leading())) {
    while (leading == NULL || leading->is_top() || seen.test_set(leading->_idx)) {
      leading = NULL;
      while (regions.size() > 0 && leading == NULL) {
        Node* r = regions.node();
        uint i = regions.index();
        if (i < r->req()) {
          leading = r->in(i);
          regions.set_index(i+1);
        } else {
          regions.pop();
        }
      }
      if (leading == NULL) {
        assert(regions.size() == 0, "all paths should have been tried");
        return NULL;
      }
    }
    if (leading->is_Region()) {
      regions.push(leading, 2);
      leading = leading->in(1);
    } else {
      leading = leading->in(0);
    }
  }
#ifdef ASSERT
  Unique_Node_List wq;
  wq.push((Node*)this);
  uint found = 0;
  for (uint i = 0; i < wq.size(); i++) {
    Node* n = wq.at(i);
    if (n->is_Region()) {
      for (uint j = 1; j < n->req(); j++) {
        Node* in = n->in(j);
        if (in != NULL && !in->is_top()) {
          wq.push(in);
        }
      }
    } else {
      if (n->is_MemBar() && n->as_MemBar()->leading()) {
        assert(n == leading, "consistency check failed");
        found++;
      } else {
        Node* in = n->in(0);
        if (in != NULL && !in->is_top()) {
          wq.push(in);
        }
      }
    }
  }
  assert(found == 1 || (found == 0 && leading == NULL), "consistency check failed");
#endif
  if (leading == NULL) {
    return NULL;
  }
  MemBarNode* mb = leading->as_MemBar();
  assert((mb->_kind == LeadingStore && _kind == TrailingStore) ||
         (mb->_kind == LeadingLoadStore && _kind == TrailingLoadStore), "bad leading membar");
  assert(mb->_pair_idx == _pair_idx, "bad leading membar");
  return mb;
}

#ifndef PRODUCT
void BlackholeNode::format(PhaseRegAlloc* ra, outputStream* st) const {
  st->print("blackhole ");
  bool first = true;
  for (uint i = 0; i < req(); i++) {
    Node* n = in(i);
    if (n != NULL && OptoReg::is_valid(ra->get_reg_first(n))) {
      if (first) {
        first = false;
      } else {
        st->print(", ");
      }
      char buf[128];
      ra->dump_register(n, buf);
      st->print("%s", buf);
    }
  }
  st->cr();
}
#endif

//===========================InitializeNode====================================
// SUMMARY:
// This node acts as a memory barrier on raw memory, after some raw stores.
// The 'cooked' oop value feeds from the Initialize, not the Allocation.
// The Initialize can 'capture' suitably constrained stores as raw inits.
// It can coalesce related raw stores into larger units (called 'tiles').
// It can avoid zeroing new storage for memory units which have raw inits.
// At macro-expansion, it is marked 'complete', and does not optimize further.
//
// EXAMPLE:
// The object 'new short[2]' occupies 16 bytes in a 32-bit machine.
//   ctl = incoming control; mem* = incoming memory
// (Note:  A star * on a memory edge denotes I/O and other standard edges.)
// First allocate uninitialized memory and fill in the header:
//   alloc = (Allocate ctl mem* 16 #short[].klass ...)
//   ctl := alloc.Control; mem* := alloc.Memory*
//   rawmem = alloc.Memory; rawoop = alloc.RawAddress
// Then initialize to zero the non-header parts of the raw memory block:
//   init = (Initialize alloc.Control alloc.Memory* alloc.RawAddress)
//   ctl := init.Control; mem.SLICE(#short[*]) := init.Memory
// After the initialize node executes, the object is ready for service:
//   oop := (CheckCastPP init.Control alloc.RawAddress #short[])
// Suppose its body is immediately initialized as {1,2}:
//   store1 = (StoreC init.Control init.Memory (+ oop 12) 1)
//   store2 = (StoreC init.Control store1      (+ oop 14) 2)
//   mem.SLICE(#short[*]) := store2
//
// DETAILS:
// An InitializeNode collects and isolates object initialization after
// an AllocateNode and before the next possible safepoint.  As a
// memory barrier (MemBarNode), it keeps critical stores from drifting
// down past any safepoint or any publication of the allocation.
// Before this barrier, a newly-allocated object may have uninitialized bits.
// After this barrier, it may be treated as a real oop, and GC is allowed.
//
// The semantics of the InitializeNode include an implicit zeroing of
// the new object from object header to the end of the object.
// (The object header and end are determined by the AllocateNode.)
//
// Certain stores may be added as direct inputs to the InitializeNode.
// These stores must update raw memory, and they must be to addresses
// derived from the raw address produced by AllocateNode, and with
// a constant offset.  They must be ordered by increasing offset.
// The first one is at in(RawStores), the last at in(req()-1).
// Unlike most memory operations, they are not linked in a chain,
// but are displayed in parallel as users of the rawmem output of
// the allocation.
//
// (See comments in InitializeNode::capture_store, which continue
// the example given above.)
//
// When the associated Allocate is macro-expanded, the InitializeNode
// may be rewritten to optimize collected stores.  A ClearArrayNode
// may also be created at that point to represent any required zeroing.
// The InitializeNode is then marked 'complete', prohibiting further
// capturing of nearby memory operations.
//
// During macro-expansion, all captured initializations which store
// constant values of 32 bits or smaller are coalesced (if advantageous)
// into larger 'tiles' 32 or 64 bits.  This allows an object to be
// initialized in fewer memory operations.  Memory words which are
// covered by neither tiles nor non-constant stores are pre-zeroed
// by explicit stores of zero.  (The code shape happens to do all
// zeroing first, then all other stores, with both sequences occurring
// in order of ascending offsets.)
//
// Alternatively, code may be inserted between an AllocateNode and its
// InitializeNode, to perform arbitrary initialization of the new object.
// E.g., the object copying intrinsics insert complex data transfers here.
// The initialization must then be marked as 'complete' disable the
// built-in zeroing semantics and the collection of initializing stores.
//
// While an InitializeNode is incomplete, reads from the memory state
// produced by it are optimizable if they match the control edge and
// new oop address associated with the allocation/initialization.
// They return a stored value (if the offset matches) or else zero.
// A write to the memory state, if it matches control and address,
// and if it is to a constant offset, may be 'captured' by the
// InitializeNode.  It is cloned as a raw memory operation and rewired
// inside the initialization, to the raw oop produced by the allocation.
// Operations on addresses which are provably distinct (e.g., to
// other AllocateNodes) are allowed to bypass the initialization.
//
// The effect of all this is to consolidate object initialization
// (both arrays and non-arrays, both piecewise and bulk) into a
// single location, where it can be optimized as a unit.
//
// Only stores with an offset less than TrackedInitializationLimit words
// will be considered for capture by an InitializeNode.  This puts a
// reasonable limit on the complexity of optimized initializations.

//---------------------------InitializeNode------------------------------------
InitializeNode::InitializeNode(Compile* C, int adr_type, Node* rawoop)
  : MemBarNode(C, adr_type, rawoop),
    _is_complete(Incomplete), _does_not_escape(false)
{
  init_class_id(Class_Initialize);

  assert(adr_type == Compile::AliasIdxRaw, "only valid atp");
  assert(in(RawAddress) == rawoop, "proper init");
  // Note:  allocation() can be NULL, for secondary initialization barriers
}

// Since this node is not matched, it will be processed by the
// register allocator.  Declare that there are no constraints
// on the allocation of the RawAddress edge.
const RegMask &InitializeNode::in_RegMask(uint idx) const {
  // This edge should be set to top, by the set_complete.  But be conservative.
  if (idx == InitializeNode::RawAddress)
    return *(Compile::current()->matcher()->idealreg2spillmask[in(idx)->ideal_reg()]);
  return RegMask::Empty;
}

Node* InitializeNode::memory(uint alias_idx) {
  Node* mem = in(Memory);
  if (mem->is_MergeMem()) {
    return mem->as_MergeMem()->memory_at(alias_idx);
  } else {
    // incoming raw memory is not split
    return mem;
  }
}

bool InitializeNode::is_non_zero() {
  if (is_complete())  return false;
  remove_extra_zeroes();
  return (req() > RawStores);
}

void InitializeNode::set_complete(PhaseGVN* phase) {
  assert(!is_complete(), "caller responsibility");
  _is_complete = Complete;

  // After this node is complete, it contains a bunch of
  // raw-memory initializations.  There is no need for
  // it to have anything to do with non-raw memory effects.
  // Therefore, tell all non-raw users to re-optimize themselves,
  // after skipping the memory effects of this initialization.
  PhaseIterGVN* igvn = phase->is_IterGVN();
  if (igvn)  igvn->add_users_to_worklist(this);
}

// convenience function
// return false if the init contains any stores already
bool AllocateNode::maybe_set_complete(PhaseGVN* phase) {
  InitializeNode* init = initialization();
  if (init == NULL || init->is_complete())  return false;
  init->remove_extra_zeroes();
  // for now, if this allocation has already collected any inits, bail:
  if (init->is_non_zero())  return false;
  init->set_complete(phase);
  return true;
}

void InitializeNode::remove_extra_zeroes() {
  if (req() == RawStores)  return;
  Node* zmem = zero_memory();
  uint fill = RawStores;
  for (uint i = fill; i < req(); i++) {
    Node* n = in(i);
    if (n->is_top() || n == zmem)  continue;  // skip
    if (fill < i)  set_req(fill, n);          // compact
    ++fill;
  }
  // delete any empty spaces created:
  while (fill < req()) {
    del_req(fill);
  }
}

// Helper for remembering which stores go with which offsets.
intptr_t InitializeNode::get_store_offset(Node* st, PhaseTransform* phase) {
  if (!st->is_Store())  return -1;  // can happen to dead code via subsume_node
  intptr_t offset = -1;
  Node* base = AddPNode::Ideal_base_and_offset(st->in(MemNode::Address),
                                               phase, offset);
  if (base == NULL)     return -1;  // something is dead,
  if (offset < 0)       return -1;  //        dead, dead
  return offset;
}

// Helper for proving that an initialization expression is
// "simple enough" to be folded into an object initialization.
// Attempts to prove that a store's initial value 'n' can be captured
// within the initialization without creating a vicious cycle, such as:
//     { Foo p = new Foo(); p.next = p; }
// True for constants and parameters and small combinations thereof.
bool InitializeNode::detect_init_independence(Node* value, PhaseGVN* phase) {
  ResourceMark rm;
  Unique_Node_List worklist;
  worklist.push(value);

  uint complexity_limit = 20;
  for (uint j = 0; j < worklist.size(); j++) {
    if (j >= complexity_limit) {
      return false;  // Bail out if processed too many nodes
    }

    Node* n = worklist.at(j);
    if (n == NULL)      continue;   // (can this really happen?)
    if (n->is_Proj())   n = n->in(0);
    if (n == this)      return false;  // found a cycle
    if (n->is_Con())    continue;
    if (n->is_Start())  continue;   // params, etc., are OK
    if (n->is_Root())   continue;   // even better

    // There cannot be any dependency if 'n' is a CFG node that dominates the current allocation
    if (n->is_CFG() && phase->is_dominator(n, allocation())) {
      continue;
    }

    Node* ctl = n->in(0);
    if (ctl != NULL && !ctl->is_top()) {
      if (ctl->is_Proj())  ctl = ctl->in(0);
      if (ctl == this)  return false;

      // If we already know that the enclosing memory op is pinned right after
      // the init, then any control flow that the store has picked up
      // must have preceded the init, or else be equal to the init.
      // Even after loop optimizations (which might change control edges)
      // a store is never pinned *before* the availability of its inputs.
      if (!MemNode::all_controls_dominate(n, this))
        return false;                  // failed to prove a good control
    }

    // Check data edges for possible dependencies on 'this'.
    for (uint i = 1; i < n->req(); i++) {
      Node* m = n->in(i);
      if (m == NULL || m == n || m->is_top())  continue;

      // Only process data inputs once
      worklist.push(m);
    }
  }

  return true;
}

// Here are all the checks a Store must pass before it can be moved into
// an initialization.  Returns zero if a check fails.
// On success, returns the (constant) offset to which the store applies,
// within the initialized memory.
intptr_t InitializeNode::can_capture_store(StoreNode* st, PhaseGVN* phase, bool can_reshape) {
  const int FAIL = 0;
  if (st->req() != MemNode::ValueIn + 1)
    return FAIL;                // an inscrutable StoreNode (card mark?)
  Node* ctl = st->in(MemNode::Control);
  if (!(ctl != NULL && ctl->is_Proj() && ctl->in(0) == this))
    return FAIL;                // must be unconditional after the initialization
  Node* mem = st->in(MemNode::Memory);
  if (!(mem->is_Proj() && mem->in(0) == this))
    return FAIL;                // must not be preceded by other stores
  Node* adr = st->in(MemNode::Address);
  intptr_t offset;
  AllocateNode* alloc = AllocateNode::Ideal_allocation(adr, phase, offset);
  if (alloc == NULL)
    return FAIL;                // inscrutable address
  if (alloc != allocation())
    return FAIL;                // wrong allocation!  (store needs to float up)
  int size_in_bytes = st->memory_size();
  if ((size_in_bytes != 0) && (offset % size_in_bytes) != 0) {
    return FAIL;                // mismatched access
  }
  Node* val = st->in(MemNode::ValueIn);

  if (!detect_init_independence(val, phase))
    return FAIL;                // stored value must be 'simple enough'

  // The Store can be captured only if nothing after the allocation
  // and before the Store is using the memory location that the store
  // overwrites.
  bool failed = false;
  // If is_complete_with_arraycopy() is true the shape of the graph is
  // well defined and is safe so no need for extra checks.
  if (!is_complete_with_arraycopy()) {
    // We are going to look at each use of the memory state following
    // the allocation to make sure nothing reads the memory that the
    // Store writes.
    const TypePtr* t_adr = phase->type(adr)->isa_ptr();
    int alias_idx = phase->C->get_alias_index(t_adr);
    ResourceMark rm;
    Unique_Node_List mems;
    mems.push(mem);
    Node* unique_merge = NULL;
    for (uint next = 0; next < mems.size(); ++next) {
      Node *m  = mems.at(next);
      for (DUIterator_Fast jmax, j = m->fast_outs(jmax); j < jmax; j++) {
        Node *n = m->fast_out(j);
        if (n->outcnt() == 0) {
          continue;
        }
        if (n == st) {
          continue;
        } else if (n->in(0) != NULL && n->in(0) != ctl) {
          // If the control of this use is different from the control
          // of the Store which is right after the InitializeNode then
          // this node cannot be between the InitializeNode and the
          // Store.
          continue;
        } else if (n->is_MergeMem()) {
          if (n->as_MergeMem()->memory_at(alias_idx) == m) {
            // We can hit a MergeMemNode (that will likely go away
            // later) that is a direct use of the memory state
            // following the InitializeNode on the same slice as the
            // store node that we'd like to capture. We need to check
            // the uses of the MergeMemNode.
            mems.push(n);
          }
        } else if (n->is_Mem()) {
          Node* other_adr = n->in(MemNode::Address);
          if (other_adr == adr) {
            failed = true;
            break;
          } else {
            const TypePtr* other_t_adr = phase->type(other_adr)->isa_ptr();
            if (other_t_adr != NULL) {
              int other_alias_idx = phase->C->get_alias_index(other_t_adr);
              if (other_alias_idx == alias_idx) {
                // A load from the same memory slice as the store right
                // after the InitializeNode. We check the control of the
                // object/array that is loaded from. If it's the same as
                // the store control then we cannot capture the store.
                assert(!n->is_Store(), "2 stores to same slice on same control?");
                Node* base = other_adr;
                assert(base->is_AddP(), "should be addp but is %s", base->Name());
                base = base->in(AddPNode::Base);
                if (base != NULL) {
                  base = base->uncast();
                  if (base->is_Proj() && base->in(0) == alloc) {
                    failed = true;
                    break;
                  }
                }
              }
            }
          }
        } else {
          failed = true;
          break;
        }
      }
    }
  }
  if (failed) {
    if (!can_reshape) {
      // We decided we couldn't capture the store during parsing. We
      // should try again during the next IGVN once the graph is
      // cleaner.
      phase->C->record_for_igvn(st);
    }
    return FAIL;
  }

  return offset;                // success
}

// Find the captured store in(i) which corresponds to the range
// [start..start+size) in the initialized object.
// If there is one, return its index i.  If there isn't, return the
// negative of the index where it should be inserted.
// Return 0 if the queried range overlaps an initialization boundary
// or if dead code is encountered.
// If size_in_bytes is zero, do not bother with overlap checks.
int InitializeNode::captured_store_insertion_point(intptr_t start,
                                                   int size_in_bytes,
                                                   PhaseTransform* phase) {
  const int FAIL = 0, MAX_STORE = MAX2(BytesPerLong, (int)MaxVectorSize);

  if (is_complete())
    return FAIL;                // arraycopy got here first; punt

  assert(allocation() != NULL, "must be present");

  // no negatives, no header fields:
  if (start < (intptr_t) allocation()->minimum_header_size())  return FAIL;

  // after a certain size, we bail out on tracking all the stores:
  intptr_t ti_limit = (TrackedInitializationLimit * HeapWordSize);
  if (start >= ti_limit)  return FAIL;

  for (uint i = InitializeNode::RawStores, limit = req(); ; ) {
    if (i >= limit)  return -(int)i; // not found; here is where to put it

    Node*    st     = in(i);
    intptr_t st_off = get_store_offset(st, phase);
    if (st_off < 0) {
      if (st != zero_memory()) {
        return FAIL;            // bail out if there is dead garbage
      }
    } else if (st_off > start) {
      // ...we are done, since stores are ordered
      if (st_off < start + size_in_bytes) {
        return FAIL;            // the next store overlaps
      }
      return -(int)i;           // not found; here is where to put it
    } else if (st_off < start) {
      assert(st->as_Store()->memory_size() <= MAX_STORE, "");
      if (size_in_bytes != 0 &&
          start < st_off + MAX_STORE &&
          start < st_off + st->as_Store()->memory_size()) {
        return FAIL;            // the previous store overlaps
      }
    } else {
      if (size_in_bytes != 0 &&
          st->as_Store()->memory_size() != size_in_bytes) {
        return FAIL;            // mismatched store size
      }
      return i;
    }

    ++i;
  }
}

// Look for a captured store which initializes at the offset 'start'
// with the given size.  If there is no such store, and no other
// initialization interferes, then return zero_memory (the memory
// projection of the AllocateNode).
Node* InitializeNode::find_captured_store(intptr_t start, int size_in_bytes,
                                          PhaseTransform* phase) {
  assert(stores_are_sane(phase), "");
  int i = captured_store_insertion_point(start, size_in_bytes, phase);
  if (i == 0) {
    return NULL;                // something is dead
  } else if (i < 0) {
    return zero_memory();       // just primordial zero bits here
  } else {
    Node* st = in(i);           // here is the store at this position
    assert(get_store_offset(st->as_Store(), phase) == start, "sanity");
    return st;
  }
}

// Create, as a raw pointer, an address within my new object at 'offset'.
Node* InitializeNode::make_raw_address(intptr_t offset,
                                       PhaseTransform* phase) {
  Node* addr = in(RawAddress);
  if (offset != 0) {
    Compile* C = phase->C;
    addr = phase->transform( new AddPNode(C->top(), addr,
                                                 phase->MakeConX(offset)) );
  }
  return addr;
}

// Clone the given store, converting it into a raw store
// initializing a field or element of my new object.
// Caller is responsible for retiring the original store,
// with subsume_node or the like.
//
// From the example above InitializeNode::InitializeNode,
// here are the old stores to be captured:
//   store1 = (StoreC init.Control init.Memory (+ oop 12) 1)
//   store2 = (StoreC init.Control store1      (+ oop 14) 2)
//
// Here is the changed code; note the extra edges on init:
//   alloc = (Allocate ...)
//   rawoop = alloc.RawAddress
//   rawstore1 = (StoreC alloc.Control alloc.Memory (+ rawoop 12) 1)
//   rawstore2 = (StoreC alloc.Control alloc.Memory (+ rawoop 14) 2)
//   init = (Initialize alloc.Control alloc.Memory rawoop
//                      rawstore1 rawstore2)
//
Node* InitializeNode::capture_store(StoreNode* st, intptr_t start,
                                    PhaseGVN* phase, bool can_reshape) {
  assert(stores_are_sane(phase), "");

  if (start < 0)  return NULL;
  assert(can_capture_store(st, phase, can_reshape) == start, "sanity");

  Compile* C = phase->C;
  int size_in_bytes = st->memory_size();
  int i = captured_store_insertion_point(start, size_in_bytes, phase);
  if (i == 0)  return NULL;     // bail out
  Node* prev_mem = NULL;        // raw memory for the captured store
  if (i > 0) {
    prev_mem = in(i);           // there is a pre-existing store under this one
    set_req(i, C->top());       // temporarily disconnect it
    // See StoreNode::Ideal 'st->outcnt() == 1' for the reason to disconnect.
  } else {
    i = -i;                     // no pre-existing store
    prev_mem = zero_memory();   // a slice of the newly allocated object
    if (i > InitializeNode::RawStores && in(i-1) == prev_mem)
      set_req(--i, C->top());   // reuse this edge; it has been folded away
    else
      ins_req(i, C->top());     // build a new edge
  }
  Node* new_st = st->clone();
  new_st->set_req(MemNode::Control, in(Control));
  new_st->set_req(MemNode::Memory,  prev_mem);
  new_st->set_req(MemNode::Address, make_raw_address(start, phase));
  new_st = phase->transform(new_st);

  // At this point, new_st might have swallowed a pre-existing store
  // at the same offset, or perhaps new_st might have disappeared,
  // if it redundantly stored the same value (or zero to fresh memory).

  // In any case, wire it in:
  PhaseIterGVN* igvn = phase->is_IterGVN();
  if (igvn) {
    igvn->rehash_node_delayed(this);
  }
  set_req(i, new_st);

  // The caller may now kill the old guy.
  DEBUG_ONLY(Node* check_st = find_captured_store(start, size_in_bytes, phase));
  assert(check_st == new_st || check_st == NULL, "must be findable");
  assert(!is_complete(), "");
  return new_st;
}

static bool store_constant(jlong* tiles, int num_tiles,
                           intptr_t st_off, int st_size,
                           jlong con) {
  if ((st_off & (st_size-1)) != 0)
    return false;               // strange store offset (assume size==2**N)
  address addr = (address)tiles + st_off;
  assert(st_off >= 0 && addr+st_size <= (address)&tiles[num_tiles], "oob");
  switch (st_size) {
  case sizeof(jbyte):  *(jbyte*) addr = (jbyte) con; break;
  case sizeof(jchar):  *(jchar*) addr = (jchar) con; break;
  case sizeof(jint):   *(jint*)  addr = (jint)  con; break;
  case sizeof(jlong):  *(jlong*) addr = (jlong) con; break;
  default: return false;        // strange store size (detect size!=2**N here)
  }
  return true;                  // return success to caller
}

// Coalesce subword constants into int constants and possibly
// into long constants.  The goal, if the CPU permits,
// is to initialize the object with a small number of 64-bit tiles.
// Also, convert floating-point constants to bit patterns.
// Non-constants are not relevant to this pass.
//
// In terms of the running example on InitializeNode::InitializeNode
// and InitializeNode::capture_store, here is the transformation
// of rawstore1 and rawstore2 into rawstore12:
//   alloc = (Allocate ...)
//   rawoop = alloc.RawAddress
//   tile12 = 0x00010002
//   rawstore12 = (StoreI alloc.Control alloc.Memory (+ rawoop 12) tile12)
//   init = (Initialize alloc.Control alloc.Memory rawoop rawstore12)
//
void
InitializeNode::coalesce_subword_stores(intptr_t header_size,
                                        Node* size_in_bytes,
                                        PhaseGVN* phase) {
  Compile* C = phase->C;

  assert(stores_are_sane(phase), "");
  // Note:  After this pass, they are not completely sane,
  // since there may be some overlaps.

  int old_subword = 0, old_long = 0, new_int = 0, new_long = 0;

  intptr_t ti_limit = (TrackedInitializationLimit * HeapWordSize);
  intptr_t size_limit = phase->find_intptr_t_con(size_in_bytes, ti_limit);
  size_limit = MIN2(size_limit, ti_limit);
  size_limit = align_up(size_limit, BytesPerLong);
  int num_tiles = size_limit / BytesPerLong;

  // allocate space for the tile map:
  const int small_len = DEBUG_ONLY(true ? 3 :) 30; // keep stack frames small
  jlong  tiles_buf[small_len];
  Node*  nodes_buf[small_len];
  jlong  inits_buf[small_len];
  jlong* tiles = ((num_tiles <= small_len) ? &tiles_buf[0]
                  : NEW_RESOURCE_ARRAY(jlong, num_tiles));
  Node** nodes = ((num_tiles <= small_len) ? &nodes_buf[0]
                  : NEW_RESOURCE_ARRAY(Node*, num_tiles));
  jlong* inits = ((num_tiles <= small_len) ? &inits_buf[0]
                  : NEW_RESOURCE_ARRAY(jlong, num_tiles));
  // tiles: exact bitwise model of all primitive constants
  // nodes: last constant-storing node subsumed into the tiles model
  // inits: which bytes (in each tile) are touched by any initializations

  //// Pass A: Fill in the tile model with any relevant stores.

  Copy::zero_to_bytes(tiles, sizeof(tiles[0]) * num_tiles);
  Copy::zero_to_bytes(nodes, sizeof(nodes[0]) * num_tiles);
  Copy::zero_to_bytes(inits, sizeof(inits[0]) * num_tiles);
  Node* zmem = zero_memory(); // initially zero memory state
  for (uint i = InitializeNode::RawStores, limit = req(); i < limit; i++) {
    Node* st = in(i);
    intptr_t st_off = get_store_offset(st, phase);

    // Figure out the store's offset and constant value:
    if (st_off < header_size)             continue; //skip (ignore header)
    if (st->in(MemNode::Memory) != zmem)  continue; //skip (odd store chain)
    int st_size = st->as_Store()->memory_size();
    if (st_off + st_size > size_limit)    break;

    // Record which bytes are touched, whether by constant or not.
    if (!store_constant(inits, num_tiles, st_off, st_size, (jlong) -1))
      continue;                 // skip (strange store size)

    const Type* val = phase->type(st->in(MemNode::ValueIn));
    if (!val->singleton())                continue; //skip (non-con store)
    BasicType type = val->basic_type();

    jlong con = 0;
    switch (type) {
    case T_INT:    con = val->is_int()->get_con();  break;
    case T_LONG:   con = val->is_long()->get_con(); break;
    case T_FLOAT:  con = jint_cast(val->getf());    break;
    case T_DOUBLE: con = jlong_cast(val->getd());   break;
    default:                              continue; //skip (odd store type)
    }

    if (type == T_LONG && Matcher::isSimpleConstant64(con) &&
        st->Opcode() == Op_StoreL) {
      continue;                 // This StoreL is already optimal.
    }

    // Store down the constant.
    store_constant(tiles, num_tiles, st_off, st_size, con);

    intptr_t j = st_off >> LogBytesPerLong;

    if (type == T_INT && st_size == BytesPerInt
        && (st_off & BytesPerInt) == BytesPerInt) {
      jlong lcon = tiles[j];
      if (!Matcher::isSimpleConstant64(lcon) &&
          st->Opcode() == Op_StoreI) {
        // This StoreI is already optimal by itself.
        jint* intcon = (jint*) &tiles[j];
        intcon[1] = 0;  // undo the store_constant()

        // If the previous store is also optimal by itself, back up and
        // undo the action of the previous loop iteration... if we can.
        // But if we can't, just let the previous half take care of itself.
        st = nodes[j];
        st_off -= BytesPerInt;
        con = intcon[0];
        if (con != 0 && st != NULL && st->Opcode() == Op_StoreI) {
          assert(st_off >= header_size, "still ignoring header");
          assert(get_store_offset(st, phase) == st_off, "must be");
          assert(in(i-1) == zmem, "must be");
          DEBUG_ONLY(const Type* tcon = phase->type(st->in(MemNode::ValueIn)));
          assert(con == tcon->is_int()->get_con(), "must be");
          // Undo the effects of the previous loop trip, which swallowed st:
          intcon[0] = 0;        // undo store_constant()
          set_req(i-1, st);     // undo set_req(i, zmem)
          nodes[j] = NULL;      // undo nodes[j] = st
          --old_subword;        // undo ++old_subword
        }
        continue;               // This StoreI is already optimal.
      }
    }

    // This store is not needed.
    set_req(i, zmem);
    nodes[j] = st;              // record for the moment
    if (st_size < BytesPerLong) // something has changed
          ++old_subword;        // includes int/float, but who's counting...
    else  ++old_long;
  }

  if ((old_subword + old_long) == 0)
    return;                     // nothing more to do

  //// Pass B: Convert any non-zero tiles into optimal constant stores.
  // Be sure to insert them before overlapping non-constant stores.
  // (E.g., byte[] x = { 1,2,y,4 }  =>  x[int 0] = 0x01020004, x[2]=y.)
  for (int j = 0; j < num_tiles; j++) {
    jlong con  = tiles[j];
    jlong init = inits[j];
    if (con == 0)  continue;
    jint con0,  con1;           // split the constant, address-wise
    jint init0, init1;          // split the init map, address-wise
    { union { jlong con; jint intcon[2]; } u;
      u.con = con;
      con0  = u.intcon[0];
      con1  = u.intcon[1];
      u.con = init;
      init0 = u.intcon[0];
      init1 = u.intcon[1];
    }

    Node* old = nodes[j];
    assert(old != NULL, "need the prior store");
    intptr_t offset = (j * BytesPerLong);

    bool split = !Matcher::isSimpleConstant64(con);

    if (offset < header_size) {
      assert(offset + BytesPerInt >= header_size, "second int counts");
      assert(*(jint*)&tiles[j] == 0, "junk in header");
      split = true;             // only the second word counts
      // Example:  int a[] = { 42 ... }
    } else if (con0 == 0 && init0 == -1) {
      split = true;             // first word is covered by full inits
      // Example:  int a[] = { ... foo(), 42 ... }
    } else if (con1 == 0 && init1 == -1) {
      split = true;             // second word is covered by full inits
      // Example:  int a[] = { ... 42, foo() ... }
    }

    // Here's a case where init0 is neither 0 nor -1:
    //   byte a[] = { ... 0,0,foo(),0,  0,0,0,42 ... }
    // Assuming big-endian memory, init0, init1 are 0x0000FF00, 0x000000FF.
    // In this case the tile is not split; it is (jlong)42.
    // The big tile is stored down, and then the foo() value is inserted.
    // (If there were foo(),foo() instead of foo(),0, init0 would be -1.)

    Node* ctl = old->in(MemNode::Control);
    Node* adr = make_raw_address(offset, phase);
    const TypePtr* atp = TypeRawPtr::BOTTOM;

    // One or two coalesced stores to plop down.
    Node*    st[2];
    intptr_t off[2];
    int  nst = 0;
    if (!split) {
      ++new_long;
      off[nst] = offset;
      st[nst++] = StoreNode::make(*phase, ctl, zmem, adr, atp,
                                  phase->longcon(con), T_LONG, MemNode::unordered);
    } else {
      // Omit either if it is a zero.
      if (con0 != 0) {
        ++new_int;
        off[nst]  = offset;
        st[nst++] = StoreNode::make(*phase, ctl, zmem, adr, atp,
                                    phase->intcon(con0), T_INT, MemNode::unordered);
      }
      if (con1 != 0) {
        ++new_int;
        offset += BytesPerInt;
        adr = make_raw_address(offset, phase);
        off[nst]  = offset;
        st[nst++] = StoreNode::make(*phase, ctl, zmem, adr, atp,
                                    phase->intcon(con1), T_INT, MemNode::unordered);
      }
    }

    // Insert second store first, then the first before the second.
    // Insert each one just before any overlapping non-constant stores.
    while (nst > 0) {
      Node* st1 = st[--nst];
      C->copy_node_notes_to(st1, old);
      st1 = phase->transform(st1);
      offset = off[nst];
      assert(offset >= header_size, "do not smash header");
      int ins_idx = captured_store_insertion_point(offset, /*size:*/0, phase);
      guarantee(ins_idx != 0, "must re-insert constant store");
      if (ins_idx < 0)  ins_idx = -ins_idx;  // never overlap
      if (ins_idx > InitializeNode::RawStores && in(ins_idx-1) == zmem)
        set_req(--ins_idx, st1);
      else
        ins_req(ins_idx, st1);
    }
  }

  if (PrintCompilation && WizardMode)
    tty->print_cr("Changed %d/%d subword/long constants into %d/%d int/long",
                  old_subword, old_long, new_int, new_long);
  if (C->log() != NULL)
    C->log()->elem("comment that='%d/%d subword/long to %d/%d int/long'",
                   old_subword, old_long, new_int, new_long);

  // Clean up any remaining occurrences of zmem:
  remove_extra_zeroes();
}

// Explore forward from in(start) to find the first fully initialized
// word, and return its offset.  Skip groups of subword stores which
// together initialize full words.  If in(start) is itself part of a
// fully initialized word, return the offset of in(start).  If there
// are no following full-word stores, or if something is fishy, return
// a negative value.
intptr_t InitializeNode::find_next_fullword_store(uint start, PhaseGVN* phase) {
  int       int_map = 0;
  intptr_t  int_map_off = 0;
  const int FULL_MAP = right_n_bits(BytesPerInt);  // the int_map we hope for

  for (uint i = start, limit = req(); i < limit; i++) {
    Node* st = in(i);

    intptr_t st_off = get_store_offset(st, phase);
    if (st_off < 0)  break;  // return conservative answer

    int st_size = st->as_Store()->memory_size();
    if (st_size >= BytesPerInt && (st_off % BytesPerInt) == 0) {
      return st_off;            // we found a complete word init
    }

    // update the map:

    intptr_t this_int_off = align_down(st_off, BytesPerInt);
    if (this_int_off != int_map_off) {
      // reset the map:
      int_map = 0;
      int_map_off = this_int_off;
    }

    int subword_off = st_off - this_int_off;
    int_map |= right_n_bits(st_size) << subword_off;
    if ((int_map & FULL_MAP) == FULL_MAP) {
      return this_int_off;      // we found a complete word init
    }

    // Did this store hit or cross the word boundary?
    intptr_t next_int_off = align_down(st_off + st_size, BytesPerInt);
    if (next_int_off == this_int_off + BytesPerInt) {
      // We passed the current int, without fully initializing it.
      int_map_off = next_int_off;
      int_map >>= BytesPerInt;
    } else if (next_int_off > this_int_off + BytesPerInt) {
      // We passed the current and next int.
      return this_int_off + BytesPerInt;
    }
  }

  return -1;
}


// Called when the associated AllocateNode is expanded into CFG.
// At this point, we may perform additional optimizations.
// Linearize the stores by ascending offset, to make memory
// activity as coherent as possible.
Node* InitializeNode::complete_stores(Node* rawctl, Node* rawmem, Node* rawptr,
                                      intptr_t header_size,
                                      Node* size_in_bytes,
                                      PhaseIterGVN* phase) {
  assert(!is_complete(), "not already complete");
  assert(stores_are_sane(phase), "");
  assert(allocation() != NULL, "must be present");

  remove_extra_zeroes();

  if (ReduceFieldZeroing || ReduceBulkZeroing)
    // reduce instruction count for common initialization patterns
    coalesce_subword_stores(header_size, size_in_bytes, phase);

  Node* zmem = zero_memory();   // initially zero memory state
  Node* inits = zmem;           // accumulating a linearized chain of inits
  #ifdef ASSERT
  intptr_t first_offset = allocation()->minimum_header_size();
  intptr_t last_init_off = first_offset;  // previous init offset
  intptr_t last_init_end = first_offset;  // previous init offset+size
  intptr_t last_tile_end = first_offset;  // previous tile offset+size
  #endif
  intptr_t zeroes_done = header_size;

  bool do_zeroing = true;       // we might give up if inits are very sparse
  int  big_init_gaps = 0;       // how many large gaps have we seen?

  if (UseTLAB && ZeroTLAB)  do_zeroing = false;
  if (!ReduceFieldZeroing && !ReduceBulkZeroing)  do_zeroing = false;

  for (uint i = InitializeNode::RawStores, limit = req(); i < limit; i++) {
    Node* st = in(i);
    intptr_t st_off = get_store_offset(st, phase);
    if (st_off < 0)
      break;                    // unknown junk in the inits
    if (st->in(MemNode::Memory) != zmem)
      break;                    // complicated store chains somehow in list

    int st_size = st->as_Store()->memory_size();
    intptr_t next_init_off = st_off + st_size;

    if (do_zeroing && zeroes_done < next_init_off) {
      // See if this store needs a zero before it or under it.
      intptr_t zeroes_needed = st_off;

      if (st_size < BytesPerInt) {
        // Look for subword stores which only partially initialize words.
        // If we find some, we must lay down some word-level zeroes first,
        // underneath the subword stores.
        //
        // Examples:
        //   byte[] a = { p,q,r,s }  =>  a[0]=p,a[1]=q,a[2]=r,a[3]=s
        //   byte[] a = { x,y,0,0 }  =>  a[0..3] = 0, a[0]=x,a[1]=y
        //   byte[] a = { 0,0,z,0 }  =>  a[0..3] = 0, a[2]=z
        //
        // Note:  coalesce_subword_stores may have already done this,
        // if it was prompted by constant non-zero subword initializers.
        // But this case can still arise with non-constant stores.

        intptr_t next_full_store = find_next_fullword_store(i, phase);

        // In the examples above:
        //   in(i)          p   q   r   s     x   y     z
        //   st_off        12  13  14  15    12  13    14
        //   st_size        1   1   1   1     1   1     1
        //   next_full_s.  12  16  16  16    16  16    16
        //   z's_done      12  16  16  16    12  16    12
        //   z's_needed    12  16  16  16    16  16    16
        //   zsize          0   0   0   0     4   0     4
        if (next_full_store < 0) {
          // Conservative tack:  Zero to end of current word.
          zeroes_needed = align_up(zeroes_needed, BytesPerInt);
        } else {
          // Zero to beginning of next fully initialized word.
          // Or, don't zero at all, if we are already in that word.
          assert(next_full_store >= zeroes_needed, "must go forward");
          assert((next_full_store & (BytesPerInt-1)) == 0, "even boundary");
          zeroes_needed = next_full_store;
        }
      }

      if (zeroes_needed > zeroes_done) {
        intptr_t zsize = zeroes_needed - zeroes_done;
        // Do some incremental zeroing on rawmem, in parallel with inits.
        zeroes_done = align_down(zeroes_done, BytesPerInt);
        rawmem = ClearArrayNode::clear_memory(rawctl, rawmem, rawptr,
                                              zeroes_done, zeroes_needed,
                                              phase);
        zeroes_done = zeroes_needed;
        if (zsize > InitArrayShortSize && ++big_init_gaps > 2)
          do_zeroing = false;   // leave the hole, next time
      }
    }

    // Collect the store and move on:
    phase->replace_input_of(st, MemNode::Memory, inits);
    inits = st;                 // put it on the linearized chain
    set_req(i, zmem);           // unhook from previous position

    if (zeroes_done == st_off)
      zeroes_done = next_init_off;

    assert(!do_zeroing || zeroes_done >= next_init_off, "don't miss any");

    #ifdef ASSERT
    // Various order invariants.  Weaker than stores_are_sane because
    // a large constant tile can be filled in by smaller non-constant stores.
    assert(st_off >= last_init_off, "inits do not reverse");
    last_init_off = st_off;
    const Type* val = NULL;
    if (st_size >= BytesPerInt &&
        (val = phase->type(st->in(MemNode::ValueIn)))->singleton() &&
        (int)val->basic_type() < (int)T_OBJECT) {
      assert(st_off >= last_tile_end, "tiles do not overlap");
      assert(st_off >= last_init_end, "tiles do not overwrite inits");
      last_tile_end = MAX2(last_tile_end, next_init_off);
    } else {
      intptr_t st_tile_end = align_up(next_init_off, BytesPerLong);
      assert(st_tile_end >= last_tile_end, "inits stay with tiles");
      assert(st_off      >= last_init_end, "inits do not overlap");
      last_init_end = next_init_off;  // it's a non-tile
    }
    #endif //ASSERT
  }

  remove_extra_zeroes();        // clear out all the zmems left over
  add_req(inits);

  if (!(UseTLAB && ZeroTLAB)) {
    // If anything remains to be zeroed, zero it all now.
    zeroes_done = align_down(zeroes_done, BytesPerInt);
    // if it is the last unused 4 bytes of an instance, forget about it
    intptr_t size_limit = phase->find_intptr_t_con(size_in_bytes, max_jint);
    if (zeroes_done + BytesPerLong >= size_limit) {
      AllocateNode* alloc = allocation();
      assert(alloc != NULL, "must be present");
      if (alloc != NULL && alloc->Opcode() == Op_Allocate) {
        Node* klass_node = alloc->in(AllocateNode::KlassNode);
        ciKlass* k = phase->type(klass_node)->is_klassptr()->klass();
        if (zeroes_done == k->layout_helper())
          zeroes_done = size_limit;
      }
    }
    if (zeroes_done < size_limit) {
      rawmem = ClearArrayNode::clear_memory(rawctl, rawmem, rawptr,
                                            zeroes_done, size_in_bytes, phase);
    }
  }

  set_complete(phase);
  return rawmem;
}


#ifdef ASSERT
bool InitializeNode::stores_are_sane(PhaseTransform* phase) {
  if (is_complete())
    return true;                // stores could be anything at this point
  assert(allocation() != NULL, "must be present");
  intptr_t last_off = allocation()->minimum_header_size();
  for (uint i = InitializeNode::RawStores; i < req(); i++) {
    Node* st = in(i);
    intptr_t st_off = get_store_offset(st, phase);
    if (st_off < 0)  continue;  // ignore dead garbage
    if (last_off > st_off) {
      tty->print_cr("*** bad store offset at %d: " INTX_FORMAT " > " INTX_FORMAT, i, last_off, st_off);
      this->dump(2);
      assert(false, "ascending store offsets");
      return false;
    }
    last_off = st_off + st->as_Store()->memory_size();
  }
  return true;
}
#endif //ASSERT




//============================MergeMemNode=====================================
//
// SEMANTICS OF MEMORY MERGES:  A MergeMem is a memory state assembled from several
// contributing store or call operations.  Each contributor provides the memory
// state for a particular "alias type" (see Compile::alias_type).  For example,
// if a MergeMem has an input X for alias category #6, then any memory reference
// to alias category #6 may use X as its memory state input, as an exact equivalent
// to using the MergeMem as a whole.
//   Load<6>( MergeMem(<6>: X, ...), p ) <==> Load<6>(X,p)
//
// (Here, the <N> notation gives the index of the relevant adr_type.)
//
// In one special case (and more cases in the future), alias categories overlap.
// The special alias category "Bot" (Compile::AliasIdxBot) includes all memory
// states.  Therefore, if a MergeMem has only one contributing input W for Bot,
// it is exactly equivalent to that state W:
//   MergeMem(<Bot>: W) <==> W
//
// Usually, the merge has more than one input.  In that case, where inputs
// overlap (i.e., one is Bot), the narrower alias type determines the memory
// state for that type, and the wider alias type (Bot) fills in everywhere else:
//   Load<5>( MergeMem(<Bot>: W, <6>: X), p ) <==> Load<5>(W,p)
//   Load<6>( MergeMem(<Bot>: W, <6>: X), p ) <==> Load<6>(X,p)
//
// A merge can take a "wide" memory state as one of its narrow inputs.
// This simply means that the merge observes out only the relevant parts of
// the wide input.  That is, wide memory states arriving at narrow merge inputs
// are implicitly "filtered" or "sliced" as necessary.  (This is rare.)
//
// These rules imply that MergeMem nodes may cascade (via their <Bot> links),
// and that memory slices "leak through":
//   MergeMem(<Bot>: MergeMem(<Bot>: W, <7>: Y)) <==> MergeMem(<Bot>: W, <7>: Y)
//
// But, in such a cascade, repeated memory slices can "block the leak":
//   MergeMem(<Bot>: MergeMem(<Bot>: W, <7>: Y), <7>: Y') <==> MergeMem(<Bot>: W, <7>: Y')
//
// In the last example, Y is not part of the combined memory state of the
// outermost MergeMem.  The system must, of course, prevent unschedulable
// memory states from arising, so you can be sure that the state Y is somehow
// a precursor to state Y'.
//
//
// REPRESENTATION OF MEMORY MERGES: The indexes used to address the Node::in array
// of each MergeMemNode array are exactly the numerical alias indexes, including
// but not limited to AliasIdxTop, AliasIdxBot, and AliasIdxRaw.  The functions
// Compile::alias_type (and kin) produce and manage these indexes.
//
// By convention, the value of in(AliasIdxTop) (i.e., in(1)) is always the top node.
// (Note that this provides quick access to the top node inside MergeMem methods,
// without the need to reach out via TLS to Compile::current.)
//
// As a consequence of what was just described, a MergeMem that represents a full
// memory state has an edge in(AliasIdxBot) which is a "wide" memory state,
// containing all alias categories.
//
// MergeMem nodes never (?) have control inputs, so in(0) is NULL.
//
// All other edges in(N) (including in(AliasIdxRaw), which is in(3)) are either
// a memory state for the alias type <N>, or else the top node, meaning that
// there is no particular input for that alias type.  Note that the length of
// a MergeMem is variable, and may be extended at any time to accommodate new
// memory states at larger alias indexes.  When merges grow, they are of course
// filled with "top" in the unused in() positions.
//
// This use of top is named "empty_memory()", or "empty_mem" (no-memory) as a variable.
// (Top was chosen because it works smoothly with passes like GCM.)
//
// For convenience, we hardwire the alias index for TypeRawPtr::BOTTOM.  (It is
// the type of random VM bits like TLS references.)  Since it is always the
// first non-Bot memory slice, some low-level loops use it to initialize an
// index variable:  for (i = AliasIdxRaw; i < req(); i++).
//
//
// ACCESSORS:  There is a special accessor MergeMemNode::base_memory which returns
// the distinguished "wide" state.  The accessor MergeMemNode::memory_at(N) returns
// the memory state for alias type <N>, or (if there is no particular slice at <N>,
// it returns the base memory.  To prevent bugs, memory_at does not accept <Top>
// or <Bot> indexes.  The iterator MergeMemStream provides robust iteration over
// MergeMem nodes or pairs of such nodes, ensuring that the non-top edges are visited.
//
// %%%% We may get rid of base_memory as a separate accessor at some point; it isn't
// really that different from the other memory inputs.  An abbreviation called
// "bot_memory()" for "memory_at(AliasIdxBot)" would keep code tidy.
//
//
// PARTIAL MEMORY STATES:  During optimization, MergeMem nodes may arise that represent
// partial memory states.  When a Phi splits through a MergeMem, the copy of the Phi
// that "emerges though" the base memory will be marked as excluding the alias types
// of the other (narrow-memory) copies which "emerged through" the narrow edges:
//
//   Phi<Bot>(U, MergeMem(<Bot>: W, <8>: Y))
//     ==Ideal=>  MergeMem(<Bot>: Phi<Bot-8>(U, W), Phi<8>(U, Y))
//
// This strange "subtraction" effect is necessary to ensure IGVN convergence.
// (It is currently unimplemented.)  As you can see, the resulting merge is
// actually a disjoint union of memory states, rather than an overlay.
//

//------------------------------MergeMemNode-----------------------------------
Node* MergeMemNode::make_empty_memory() {
  Node* empty_memory = (Node*) Compile::current()->top();
  assert(empty_memory->is_top(), "correct sentinel identity");
  return empty_memory;
}

MergeMemNode::MergeMemNode(Node *new_base) : Node(1+Compile::AliasIdxRaw) {
  init_class_id(Class_MergeMem);
  // all inputs are nullified in Node::Node(int)
  // set_input(0, NULL);  // no control input

  // Initialize the edges uniformly to top, for starters.
  Node* empty_mem = make_empty_memory();
  for (uint i = Compile::AliasIdxTop; i < req(); i++) {
    init_req(i,empty_mem);
  }
  assert(empty_memory() == empty_mem, "");

  if( new_base != NULL && new_base->is_MergeMem() ) {
    MergeMemNode* mdef = new_base->as_MergeMem();
    assert(mdef->empty_memory() == empty_mem, "consistent sentinels");
    for (MergeMemStream mms(this, mdef); mms.next_non_empty2(); ) {
      mms.set_memory(mms.memory2());
    }
    assert(base_memory() == mdef->base_memory(), "");
  } else {
    set_base_memory(new_base);
  }
}

// Make a new, untransformed MergeMem with the same base as 'mem'.
// If mem is itself a MergeMem, populate the result with the same edges.
MergeMemNode* MergeMemNode::make(Node* mem) {
  return new MergeMemNode(mem);
}

//------------------------------cmp--------------------------------------------
uint MergeMemNode::hash() const { return NO_HASH; }
bool MergeMemNode::cmp( const Node &n ) const {
  return (&n == this);          // Always fail except on self
}

//------------------------------Identity---------------------------------------
Node* MergeMemNode::Identity(PhaseGVN* phase) {
  // Identity if this merge point does not record any interesting memory
  // disambiguations.
  Node* base_mem = base_memory();
  Node* empty_mem = empty_memory();
  if (base_mem != empty_mem) {  // Memory path is not dead?
    for (uint i = Compile::AliasIdxRaw; i < req(); i++) {
      Node* mem = in(i);
      if (mem != empty_mem && mem != base_mem) {
        return this;            // Many memory splits; no change
      }
    }
  }
  return base_mem;              // No memory splits; ID on the one true input
}

//------------------------------Ideal------------------------------------------
// This method is invoked recursively on chains of MergeMem nodes
Node *MergeMemNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  // Remove chain'd MergeMems
  //
  // This is delicate, because the each "in(i)" (i >= Raw) is interpreted
  // relative to the "in(Bot)".  Since we are patching both at the same time,
  // we have to be careful to read each "in(i)" relative to the old "in(Bot)",
  // but rewrite each "in(i)" relative to the new "in(Bot)".
  Node *progress = NULL;


  Node* old_base = base_memory();
  Node* empty_mem = empty_memory();
  if (old_base == empty_mem)
    return NULL; // Dead memory path.

  MergeMemNode* old_mbase;
  if (old_base != NULL && old_base->is_MergeMem())
    old_mbase = old_base->as_MergeMem();
  else
    old_mbase = NULL;
  Node* new_base = old_base;

  // simplify stacked MergeMems in base memory
  if (old_mbase)  new_base = old_mbase->base_memory();

  // the base memory might contribute new slices beyond my req()
  if (old_mbase)  grow_to_match(old_mbase);

  // Look carefully at the base node if it is a phi.
  PhiNode* phi_base;
  if (new_base != NULL && new_base->is_Phi())
    phi_base = new_base->as_Phi();
  else
    phi_base = NULL;

  Node*    phi_reg = NULL;
  uint     phi_len = (uint)-1;
  if (phi_base != NULL) {
    phi_reg = phi_base->region();
    phi_len = phi_base->req();
    // see if the phi is unfinished
    for (uint i = 1; i < phi_len; i++) {
      if (phi_base->in(i) == NULL) {
        // incomplete phi; do not look at it yet!
        phi_reg = NULL;
        phi_len = (uint)-1;
        break;
      }
    }
  }

  // Note:  We do not call verify_sparse on entry, because inputs
  // can normalize to the base_memory via subsume_node or similar
  // mechanisms.  This method repairs that damage.

  assert(!old_mbase || old_mbase->is_empty_memory(empty_mem), "consistent sentinels");

  // Look at each slice.
  for (uint i = Compile::AliasIdxRaw; i < req(); i++) {
    Node* old_in = in(i);
    // calculate the old memory value
    Node* old_mem = old_in;
    if (old_mem == empty_mem)  old_mem = old_base;
    assert(old_mem == memory_at(i), "");

    // maybe update (reslice) the old memory value

    // simplify stacked MergeMems
    Node* new_mem = old_mem;
    MergeMemNode* old_mmem;
    if (old_mem != NULL && old_mem->is_MergeMem())
      old_mmem = old_mem->as_MergeMem();
    else
      old_mmem = NULL;
    if (old_mmem == this) {
      // This can happen if loops break up and safepoints disappear.
      // A merge of BotPtr (default) with a RawPtr memory derived from a
      // safepoint can be rewritten to a merge of the same BotPtr with
      // the BotPtr phi coming into the loop.  If that phi disappears
      // also, we can end up with a self-loop of the mergemem.
      // In general, if loops degenerate and memory effects disappear,
      // a mergemem can be left looking at itself.  This simply means
      // that the mergemem's default should be used, since there is
      // no longer any apparent effect on this slice.
      // Note: If a memory slice is a MergeMem cycle, it is unreachable
      //       from start.  Update the input to TOP.
      new_mem = (new_base == this || new_base == empty_mem)? empty_mem : new_base;
    }
    else if (old_mmem != NULL) {
      new_mem = old_mmem->memory_at(i);
    }
    // else preceding memory was not a MergeMem

    // maybe store down a new value
    Node* new_in = new_mem;
    if (new_in == new_base)  new_in = empty_mem;

    if (new_in != old_in) {
      // Warning:  Do not combine this "if" with the previous "if"
      // A memory slice might have be be rewritten even if it is semantically
      // unchanged, if the base_memory value has changed.
      set_req_X(i, new_in, phase);
      progress = this;          // Report progress
    }
  }

  if (new_base != old_base) {
    set_req_X(Compile::AliasIdxBot, new_base, phase);
    // Don't use set_base_memory(new_base), because we need to update du.
    assert(base_memory() == new_base, "");
    progress = this;
  }

  if( base_memory() == this ) {
    // a self cycle indicates this memory path is dead
    set_req(Compile::AliasIdxBot, empty_mem);
  }

  // Resolve external cycles by calling Ideal on a MergeMem base_memory
  // Recursion must occur after the self cycle check above
  if( base_memory()->is_MergeMem() ) {
    MergeMemNode *new_mbase = base_memory()->as_MergeMem();
    Node *m = phase->transform(new_mbase);  // Rollup any cycles
    if( m != NULL &&
        (m->is_top() ||
         (m->is_MergeMem() && m->as_MergeMem()->base_memory() == empty_mem)) ) {
      // propagate rollup of dead cycle to self
      set_req(Compile::AliasIdxBot, empty_mem);
    }
  }

  if( base_memory() == empty_mem ) {
    progress = this;
    // Cut inputs during Parse phase only.
    // During Optimize phase a dead MergeMem node will be subsumed by Top.
    if( !can_reshape ) {
      for (uint i = Compile::AliasIdxRaw; i < req(); i++) {
        if( in(i) != empty_mem ) { set_req(i, empty_mem); }
      }
    }
  }

  if( !progress && base_memory()->is_Phi() && can_reshape ) {
    // Check if PhiNode::Ideal's "Split phis through memory merges"
    // transform should be attempted. Look for this->phi->this cycle.
    uint merge_width = req();
    if (merge_width > Compile::AliasIdxRaw) {
      PhiNode* phi = base_memory()->as_Phi();
      for( uint i = 1; i < phi->req(); ++i ) {// For all paths in
        if (phi->in(i) == this) {
          phase->is_IterGVN()->_worklist.push(phi);
          break;
        }
      }
    }
  }

  assert(progress || verify_sparse(), "please, no dups of base");
  return progress;
}

//-------------------------set_base_memory-------------------------------------
void MergeMemNode::set_base_memory(Node *new_base) {
  Node* empty_mem = empty_memory();
  set_req(Compile::AliasIdxBot, new_base);
  assert(memory_at(req()) == new_base, "must set default memory");
  // Clear out other occurrences of new_base:
  if (new_base != empty_mem) {
    for (uint i = Compile::AliasIdxRaw; i < req(); i++) {
      if (in(i) == new_base)  set_req(i, empty_mem);
    }
  }
}

//------------------------------out_RegMask------------------------------------
const RegMask &MergeMemNode::out_RegMask() const {
  return RegMask::Empty;
}

//------------------------------dump_spec--------------------------------------
#ifndef PRODUCT
void MergeMemNode::dump_spec(outputStream *st) const {
  st->print(" {");
  Node* base_mem = base_memory();
  for( uint i = Compile::AliasIdxRaw; i < req(); i++ ) {
    Node* mem = (in(i) != NULL) ? memory_at(i) : base_mem;
    if (mem == base_mem) { st->print(" -"); continue; }
    st->print( " N%d:", mem->_idx );
    Compile::current()->get_adr_type(i)->dump_on(st);
  }
  st->print(" }");
}
#endif // !PRODUCT


#ifdef ASSERT
static bool might_be_same(Node* a, Node* b) {
  if (a == b)  return true;
  if (!(a->is_Phi() || b->is_Phi()))  return false;
  // phis shift around during optimization
  return true;  // pretty stupid...
}

// verify a narrow slice (either incoming or outgoing)
static void verify_memory_slice(const MergeMemNode* m, int alias_idx, Node* n) {
  if (!VerifyAliases)                return;  // don't bother to verify unless requested
  if (VMError::is_error_reported())  return;  // muzzle asserts when debugging an error
  if (Node::in_dump())               return;  // muzzle asserts when printing
  assert(alias_idx >= Compile::AliasIdxRaw, "must not disturb base_memory or sentinel");
  assert(n != NULL, "");
  // Elide intervening MergeMem's
  while (n->is_MergeMem()) {
    n = n->as_MergeMem()->memory_at(alias_idx);
  }
  Compile* C = Compile::current();
  const TypePtr* n_adr_type = n->adr_type();
  if (n == m->empty_memory()) {
    // Implicit copy of base_memory()
  } else if (n_adr_type != TypePtr::BOTTOM) {
    assert(n_adr_type != NULL, "new memory must have a well-defined adr_type");
    assert(C->must_alias(n_adr_type, alias_idx), "new memory must match selected slice");
  } else {
    // A few places like make_runtime_call "know" that VM calls are narrow,
    // and can be used to update only the VM bits stored as TypeRawPtr::BOTTOM.
    bool expected_wide_mem = false;
    if (n == m->base_memory()) {
      expected_wide_mem = true;
    } else if (alias_idx == Compile::AliasIdxRaw ||
               n == m->memory_at(Compile::AliasIdxRaw)) {
      expected_wide_mem = true;
    } else if (!C->alias_type(alias_idx)->is_rewritable()) {
      // memory can "leak through" calls on channels that
      // are write-once.  Allow this also.
      expected_wide_mem = true;
    }
    assert(expected_wide_mem, "expected narrow slice replacement");
  }
}
#else // !ASSERT
#define verify_memory_slice(m,i,n) (void)(0)  // PRODUCT version is no-op
#endif


//-----------------------------memory_at---------------------------------------
Node* MergeMemNode::memory_at(uint alias_idx) const {
  assert(alias_idx >= Compile::AliasIdxRaw ||
         alias_idx == Compile::AliasIdxBot && Compile::current()->AliasLevel() == 0,
         "must avoid base_memory and AliasIdxTop");

  // Otherwise, it is a narrow slice.
  Node* n = alias_idx < req() ? in(alias_idx) : empty_memory();
  Compile *C = Compile::current();
  if (is_empty_memory(n)) {
    // the array is sparse; empty slots are the "top" node
    n = base_memory();
    assert(Node::in_dump()
           || n == NULL || n->bottom_type() == Type::TOP
           || n->adr_type() == NULL // address is TOP
           || n->adr_type() == TypePtr::BOTTOM
           || n->adr_type() == TypeRawPtr::BOTTOM
           || Compile::current()->AliasLevel() == 0,
           "must be a wide memory");
    // AliasLevel == 0 if we are organizing the memory states manually.
    // See verify_memory_slice for comments on TypeRawPtr::BOTTOM.
  } else {
    // make sure the stored slice is sane
    #ifdef ASSERT
    if (VMError::is_error_reported() || Node::in_dump()) {
    } else if (might_be_same(n, base_memory())) {
      // Give it a pass:  It is a mostly harmless repetition of the base.
      // This can arise normally from node subsumption during optimization.
    } else {
      verify_memory_slice(this, alias_idx, n);
    }
    #endif
  }
  return n;
}

//---------------------------set_memory_at-------------------------------------
void MergeMemNode::set_memory_at(uint alias_idx, Node *n) {
  verify_memory_slice(this, alias_idx, n);
  Node* empty_mem = empty_memory();
  if (n == base_memory())  n = empty_mem;  // collapse default
  uint need_req = alias_idx+1;
  if (req() < need_req) {
    if (n == empty_mem)  return;  // already the default, so do not grow me
    // grow the sparse array
    do {
      add_req(empty_mem);
    } while (req() < need_req);
  }
  set_req( alias_idx, n );
}



//--------------------------iteration_setup------------------------------------
void MergeMemNode::iteration_setup(const MergeMemNode* other) {
  if (other != NULL) {
    grow_to_match(other);
    // invariant:  the finite support of mm2 is within mm->req()
    #ifdef ASSERT
    for (uint i = req(); i < other->req(); i++) {
      assert(other->is_empty_memory(other->in(i)), "slice left uncovered");
    }
    #endif
  }
  // Replace spurious copies of base_memory by top.
  Node* base_mem = base_memory();
  if (base_mem != NULL && !base_mem->is_top()) {
    for (uint i = Compile::AliasIdxBot+1, imax = req(); i < imax; i++) {
      if (in(i) == base_mem)
        set_req(i, empty_memory());
    }
  }
}

//---------------------------grow_to_match-------------------------------------
void MergeMemNode::grow_to_match(const MergeMemNode* other) {
  Node* empty_mem = empty_memory();
  assert(other->is_empty_memory(empty_mem), "consistent sentinels");
  // look for the finite support of the other memory
  for (uint i = other->req(); --i >= req(); ) {
    if (other->in(i) != empty_mem) {
      uint new_len = i+1;
      while (req() < new_len)  add_req(empty_mem);
      break;
    }
  }
}

//---------------------------verify_sparse-------------------------------------
#ifndef PRODUCT
bool MergeMemNode::verify_sparse() const {
  assert(is_empty_memory(make_empty_memory()), "sane sentinel");
  Node* base_mem = base_memory();
  // The following can happen in degenerate cases, since empty==top.
  if (is_empty_memory(base_mem))  return true;
  for (uint i = Compile::AliasIdxRaw; i < req(); i++) {
    assert(in(i) != NULL, "sane slice");
    if (in(i) == base_mem)  return false;  // should have been the sentinel value!
  }
  return true;
}

bool MergeMemStream::match_memory(Node* mem, const MergeMemNode* mm, int idx) {
  Node* n;
  n = mm->in(idx);
  if (mem == n)  return true;  // might be empty_memory()
  n = (idx == Compile::AliasIdxBot)? mm->base_memory(): mm->memory_at(idx);
  if (mem == n)  return true;
  return false;
}
#endif // !PRODUCT
