/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "compiler/compileLog.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "libadt/vectset.hpp"
#include "memory/universe.hpp"
#include "opto/addnode.hpp"
#include "opto/arraycopynode.hpp"
#include "opto/callnode.hpp"
#include "opto/castnode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/compile.hpp"
#include "opto/convertnode.hpp"
#include "opto/graphKit.hpp"
#include "opto/intrinsicnode.hpp"
#include "opto/locknode.hpp"
#include "opto/loopnode.hpp"
#include "opto/macro.hpp"
#include "opto/memnode.hpp"
#include "opto/narrowptrnode.hpp"
#include "opto/node.hpp"
#include "opto/opaquenode.hpp"
#include "opto/phaseX.hpp"
#include "opto/rootnode.hpp"
#include "opto/runtime.hpp"
#include "opto/subnode.hpp"
#include "opto/subtypenode.hpp"
#include "opto/type.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/sharedRuntime.hpp"
#include "utilities/macros.hpp"
#include "utilities/powerOfTwo.hpp"
#if INCLUDE_G1GC
#include "gc/g1/g1ThreadLocalData.hpp"
#endif // INCLUDE_G1GC
#if INCLUDE_SHENANDOAHGC
#include "gc/shenandoah/c2/shenandoahBarrierSetC2.hpp"
#endif


//
// Replace any references to "oldref" in inputs to "use" with "newref".
// Returns the number of replacements made.
//
int PhaseMacroExpand::replace_input(Node *use, Node *oldref, Node *newref) {
  int nreplacements = 0;
  uint req = use->req();
  for (uint j = 0; j < use->len(); j++) {
    Node *uin = use->in(j);
    if (uin == oldref) {
      if (j < req)
        use->set_req(j, newref);
      else
        use->set_prec(j, newref);
      nreplacements++;
    } else if (j >= req && uin == NULL) {
      break;
    }
  }
  return nreplacements;
}

void PhaseMacroExpand::migrate_outs(Node *old, Node *target) {
  assert(old != NULL, "sanity");
  for (DUIterator_Fast imax, i = old->fast_outs(imax); i < imax; i++) {
    Node* use = old->fast_out(i);
    _igvn.rehash_node_delayed(use);
    imax -= replace_input(use, old, target);
    // back up iterator
    --i;
  }
  assert(old->outcnt() == 0, "all uses must be deleted");
}

Node* PhaseMacroExpand::opt_bits_test(Node* ctrl, Node* region, int edge, Node* word, int mask, int bits, bool return_fast_path) {
  Node* cmp;
  if (mask != 0) {
    Node* and_node = transform_later(new AndXNode(word, MakeConX(mask)));
    cmp = transform_later(new CmpXNode(and_node, MakeConX(bits)));
  } else {
    cmp = word;
  }
  Node* bol = transform_later(new BoolNode(cmp, BoolTest::ne));
  IfNode* iff = new IfNode( ctrl, bol, PROB_MIN, COUNT_UNKNOWN );
  transform_later(iff);

  // Fast path taken.
  Node *fast_taken = transform_later(new IfFalseNode(iff));

  // Fast path not-taken, i.e. slow path
  Node *slow_taken = transform_later(new IfTrueNode(iff));

  if (return_fast_path) {
    region->init_req(edge, slow_taken); // Capture slow-control
    return fast_taken;
  } else {
    region->init_req(edge, fast_taken); // Capture fast-control
    return slow_taken;
  }
}

//--------------------copy_predefined_input_for_runtime_call--------------------
void PhaseMacroExpand::copy_predefined_input_for_runtime_call(Node * ctrl, CallNode* oldcall, CallNode* call) {
  // Set fixed predefined input arguments
  call->init_req( TypeFunc::Control, ctrl );
  call->init_req( TypeFunc::I_O    , oldcall->in( TypeFunc::I_O) );
  call->init_req( TypeFunc::Memory , oldcall->in( TypeFunc::Memory ) ); // ?????
  call->init_req( TypeFunc::ReturnAdr, oldcall->in( TypeFunc::ReturnAdr ) );
  call->init_req( TypeFunc::FramePtr, oldcall->in( TypeFunc::FramePtr ) );
}

//------------------------------make_slow_call---------------------------------
CallNode* PhaseMacroExpand::make_slow_call(CallNode *oldcall, const TypeFunc* slow_call_type,
                                           address slow_call, const char* leaf_name, Node* slow_path,
                                           Node* parm0, Node* parm1, Node* parm2) {

  // Slow-path call
 CallNode *call = leaf_name
   ? (CallNode*)new CallLeafNode      ( slow_call_type, slow_call, leaf_name, TypeRawPtr::BOTTOM )
   : (CallNode*)new CallStaticJavaNode( slow_call_type, slow_call, OptoRuntime::stub_name(slow_call), TypeRawPtr::BOTTOM );

  // Slow path call has no side-effects, uses few values
  copy_predefined_input_for_runtime_call(slow_path, oldcall, call );
  if (parm0 != NULL)  call->init_req(TypeFunc::Parms+0, parm0);
  if (parm1 != NULL)  call->init_req(TypeFunc::Parms+1, parm1);
  if (parm2 != NULL)  call->init_req(TypeFunc::Parms+2, parm2);
  call->copy_call_debug_info(&_igvn, oldcall);
  call->set_cnt(PROB_UNLIKELY_MAG(4));  // Same effect as RC_UNCOMMON.
  _igvn.replace_node(oldcall, call);
  transform_later(call);

  return call;
}

void PhaseMacroExpand::eliminate_gc_barrier(Node* p2x) {
  BarrierSetC2 *bs = BarrierSet::barrier_set()->barrier_set_c2();
  bs->eliminate_gc_barrier(this, p2x);
}

// Search for a memory operation for the specified memory slice.
static Node *scan_mem_chain(Node *mem, int alias_idx, int offset, Node *start_mem, Node *alloc, PhaseGVN *phase) {
  Node *orig_mem = mem;
  Node *alloc_mem = alloc->in(TypeFunc::Memory);
  const TypeOopPtr *tinst = phase->C->get_adr_type(alias_idx)->isa_oopptr();
  while (true) {
    if (mem == alloc_mem || mem == start_mem ) {
      return mem;  // hit one of our sentinels
    } else if (mem->is_MergeMem()) {
      mem = mem->as_MergeMem()->memory_at(alias_idx);
    } else if (mem->is_Proj() && mem->as_Proj()->_con == TypeFunc::Memory) {
      Node *in = mem->in(0);
      // we can safely skip over safepoints, calls, locks and membars because we
      // already know that the object is safe to eliminate.
      if (in->is_Initialize() && in->as_Initialize()->allocation() == alloc) {
        return in;
      } else if (in->is_Call()) {
        CallNode *call = in->as_Call();
        if (call->may_modify(tinst, phase)) {
          assert(call->is_ArrayCopy(), "ArrayCopy is the only call node that doesn't make allocation escape");
          if (call->as_ArrayCopy()->modifies(offset, offset, phase, false)) {
            return in;
          }
        }
        mem = in->in(TypeFunc::Memory);
      } else if (in->is_MemBar()) {
        ArrayCopyNode* ac = NULL;
        if (ArrayCopyNode::may_modify(tinst, in->as_MemBar(), phase, ac)) {
          if (ac != NULL) {
            assert(ac->is_clonebasic(), "Only basic clone is a non escaping clone");
            return ac;
          }
        }
        mem = in->in(TypeFunc::Memory);
      } else {
#ifdef ASSERT
        in->dump();
        mem->dump();
        assert(false, "unexpected projection");
#endif
      }
    } else if (mem->is_Store()) {
      const TypePtr* atype = mem->as_Store()->adr_type();
      int adr_idx = phase->C->get_alias_index(atype);
      if (adr_idx == alias_idx) {
        assert(atype->isa_oopptr(), "address type must be oopptr");
        int adr_offset = atype->offset();
        uint adr_iid = atype->is_oopptr()->instance_id();
        // Array elements references have the same alias_idx
        // but different offset and different instance_id.
        if (adr_offset == offset && adr_iid == alloc->_idx) {
          return mem;
        }
      } else {
        assert(adr_idx == Compile::AliasIdxRaw, "address must match or be raw");
      }
      mem = mem->in(MemNode::Memory);
    } else if (mem->is_ClearArray()) {
      if (!ClearArrayNode::step_through(&mem, alloc->_idx, phase)) {
        // Can not bypass initialization of the instance
        // we are looking.
        debug_only(intptr_t offset;)
        assert(alloc == AllocateNode::Ideal_allocation(mem->in(3), phase, offset), "sanity");
        InitializeNode* init = alloc->as_Allocate()->initialization();
        // We are looking for stored value, return Initialize node
        // or memory edge from Allocate node.
        if (init != NULL) {
          return init;
        } else {
          return alloc->in(TypeFunc::Memory); // It will produce zero value (see callers).
        }
      }
      // Otherwise skip it (the call updated 'mem' value).
    } else if (mem->Opcode() == Op_SCMemProj) {
      mem = mem->in(0);
      Node* adr = NULL;
      if (mem->is_LoadStore()) {
        adr = mem->in(MemNode::Address);
      } else {
        assert(mem->Opcode() == Op_EncodeISOArray ||
               mem->Opcode() == Op_StrCompressedCopy, "sanity");
        adr = mem->in(3); // Destination array
      }
      const TypePtr* atype = adr->bottom_type()->is_ptr();
      int adr_idx = phase->C->get_alias_index(atype);
      if (adr_idx == alias_idx) {
        DEBUG_ONLY(mem->dump();)
        assert(false, "Object is not scalar replaceable if a LoadStore node accesses its field");
        return NULL;
      }
      mem = mem->in(MemNode::Memory);
   } else if (mem->Opcode() == Op_StrInflatedCopy) {
      Node* adr = mem->in(3); // Destination array
      const TypePtr* atype = adr->bottom_type()->is_ptr();
      int adr_idx = phase->C->get_alias_index(atype);
      if (adr_idx == alias_idx) {
        DEBUG_ONLY(mem->dump();)
        assert(false, "Object is not scalar replaceable if a StrInflatedCopy node accesses its field");
        return NULL;
      }
      mem = mem->in(MemNode::Memory);
    } else {
      return mem;
    }
    assert(mem != orig_mem, "dead memory loop");
  }
}

// Generate loads from source of the arraycopy for fields of
// destination needed at a deoptimization point
Node* PhaseMacroExpand::make_arraycopy_load(ArrayCopyNode* ac, intptr_t offset, Node* ctl, Node* mem, BasicType ft, const Type *ftype, AllocateNode *alloc) {
  BasicType bt = ft;
  const Type *type = ftype;
  if (ft == T_NARROWOOP) {
    bt = T_OBJECT;
    type = ftype->make_oopptr();
  }
  Node* res = NULL;
  if (ac->is_clonebasic()) {
    assert(ac->in(ArrayCopyNode::Src) != ac->in(ArrayCopyNode::Dest), "clone source equals destination");
    Node* base = ac->in(ArrayCopyNode::Src);
    Node* adr = _igvn.transform(new AddPNode(base, base, MakeConX(offset)));
    const TypePtr* adr_type = _igvn.type(base)->is_ptr()->add_offset(offset);
    MergeMemNode* mergemen = _igvn.transform(MergeMemNode::make(mem))->as_MergeMem();
    BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
    res = ArrayCopyNode::load(bs, &_igvn, ctl, mergemen, adr, adr_type, type, bt);
  } else {
    if (ac->modifies(offset, offset, &_igvn, true)) {
      assert(ac->in(ArrayCopyNode::Dest) == alloc->result_cast(), "arraycopy destination should be allocation's result");
      uint shift = exact_log2(type2aelembytes(bt));
      Node* src_pos = ac->in(ArrayCopyNode::SrcPos);
      Node* dest_pos = ac->in(ArrayCopyNode::DestPos);
      const TypeInt* src_pos_t = _igvn.type(src_pos)->is_int();
      const TypeInt* dest_pos_t = _igvn.type(dest_pos)->is_int();

      Node* adr = NULL;
      const TypePtr* adr_type = NULL;
      if (src_pos_t->is_con() && dest_pos_t->is_con()) {
        intptr_t off = ((src_pos_t->get_con() - dest_pos_t->get_con()) << shift) + offset;
        Node* base = ac->in(ArrayCopyNode::Src);
        adr = _igvn.transform(new AddPNode(base, base, MakeConX(off)));
        adr_type = _igvn.type(base)->is_ptr()->add_offset(off);
        if (ac->in(ArrayCopyNode::Src) == ac->in(ArrayCopyNode::Dest)) {
          // Don't emit a new load from src if src == dst but try to get the value from memory instead
          return value_from_mem(ac->in(TypeFunc::Memory), ctl, ft, ftype, adr_type->isa_oopptr(), alloc);
        }
      } else {
        Node* diff = _igvn.transform(new SubINode(ac->in(ArrayCopyNode::SrcPos), ac->in(ArrayCopyNode::DestPos)));
#ifdef _LP64
        diff = _igvn.transform(new ConvI2LNode(diff));
#endif
        diff = _igvn.transform(new LShiftXNode(diff, intcon(shift)));

        Node* off = _igvn.transform(new AddXNode(MakeConX(offset), diff));
        Node* base = ac->in(ArrayCopyNode::Src);
        adr = _igvn.transform(new AddPNode(base, base, off));
        adr_type = _igvn.type(base)->is_ptr()->add_offset(Type::OffsetBot);
        if (ac->in(ArrayCopyNode::Src) == ac->in(ArrayCopyNode::Dest)) {
          // Non constant offset in the array: we can't statically
          // determine the value
          return NULL;
        }
      }
      MergeMemNode* mergemen = _igvn.transform(MergeMemNode::make(mem))->as_MergeMem();
      BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
      res = ArrayCopyNode::load(bs, &_igvn, ctl, mergemen, adr, adr_type, type, bt);
    }
  }
  if (res != NULL) {
    if (ftype->isa_narrowoop()) {
      // PhaseMacroExpand::scalar_replacement adds DecodeN nodes
      res = _igvn.transform(new EncodePNode(res, ftype));
    }
    return res;
  }
  return NULL;
}

//
// Given a Memory Phi, compute a value Phi containing the values from stores
// on the input paths.
// Note: this function is recursive, its depth is limited by the "level" argument
// Returns the computed Phi, or NULL if it cannot compute it.
Node *PhaseMacroExpand::value_from_mem_phi(Node *mem, BasicType ft, const Type *phi_type, const TypeOopPtr *adr_t, AllocateNode *alloc, Node_Stack *value_phis, int level) {
  assert(mem->is_Phi(), "sanity");
  int alias_idx = C->get_alias_index(adr_t);
  int offset = adr_t->offset();
  int instance_id = adr_t->instance_id();

  // Check if an appropriate value phi already exists.
  Node* region = mem->in(0);
  for (DUIterator_Fast kmax, k = region->fast_outs(kmax); k < kmax; k++) {
    Node* phi = region->fast_out(k);
    if (phi->is_Phi() && phi != mem &&
        phi->as_Phi()->is_same_inst_field(phi_type, (int)mem->_idx, instance_id, alias_idx, offset)) {
      return phi;
    }
  }
  // Check if an appropriate new value phi already exists.
  Node* new_phi = value_phis->find(mem->_idx);
  if (new_phi != NULL)
    return new_phi;

  if (level <= 0) {
    return NULL; // Give up: phi tree too deep
  }
  Node *start_mem = C->start()->proj_out_or_null(TypeFunc::Memory);
  Node *alloc_mem = alloc->in(TypeFunc::Memory);

  uint length = mem->req();
  GrowableArray <Node *> values(length, length, NULL);

  // create a new Phi for the value
  PhiNode *phi = new PhiNode(mem->in(0), phi_type, NULL, mem->_idx, instance_id, alias_idx, offset);
  transform_later(phi);
  value_phis->push(phi, mem->_idx);

  for (uint j = 1; j < length; j++) {
    Node *in = mem->in(j);
    if (in == NULL || in->is_top()) {
      values.at_put(j, in);
    } else  {
      Node *val = scan_mem_chain(in, alias_idx, offset, start_mem, alloc, &_igvn);
      if (val == start_mem || val == alloc_mem) {
        // hit a sentinel, return appropriate 0 value
        values.at_put(j, _igvn.zerocon(ft));
        continue;
      }
      if (val->is_Initialize()) {
        val = val->as_Initialize()->find_captured_store(offset, type2aelembytes(ft), &_igvn);
      }
      if (val == NULL) {
        return NULL;  // can't find a value on this path
      }
      if (val == mem) {
        values.at_put(j, mem);
      } else if (val->is_Store()) {
        Node* n = val->in(MemNode::ValueIn);
        BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
        n = bs->step_over_gc_barrier(n);
        if (is_subword_type(ft)) {
          n = Compile::narrow_value(ft, n, phi_type, &_igvn, true);
        }
        values.at_put(j, n);
      } else if(val->is_Proj() && val->in(0) == alloc) {
        values.at_put(j, _igvn.zerocon(ft));
      } else if (val->is_Phi()) {
        val = value_from_mem_phi(val, ft, phi_type, adr_t, alloc, value_phis, level-1);
        if (val == NULL) {
          return NULL;
        }
        values.at_put(j, val);
      } else if (val->Opcode() == Op_SCMemProj) {
        assert(val->in(0)->is_LoadStore() ||
               val->in(0)->Opcode() == Op_EncodeISOArray ||
               val->in(0)->Opcode() == Op_StrCompressedCopy, "sanity");
        assert(false, "Object is not scalar replaceable if a LoadStore node accesses its field");
        return NULL;
      } else if (val->is_ArrayCopy()) {
        Node* res = make_arraycopy_load(val->as_ArrayCopy(), offset, val->in(0), val->in(TypeFunc::Memory), ft, phi_type, alloc);
        if (res == NULL) {
          return NULL;
        }
        values.at_put(j, res);
      } else {
        DEBUG_ONLY( val->dump(); )
        assert(false, "unknown node on this path");
        return NULL;  // unknown node on this path
      }
    }
  }
  // Set Phi's inputs
  for (uint j = 1; j < length; j++) {
    if (values.at(j) == mem) {
      phi->init_req(j, phi);
    } else {
      phi->init_req(j, values.at(j));
    }
  }
  return phi;
}

// Search the last value stored into the object's field.
Node *PhaseMacroExpand::value_from_mem(Node *sfpt_mem, Node *sfpt_ctl, BasicType ft, const Type *ftype, const TypeOopPtr *adr_t, AllocateNode *alloc) {
  assert(adr_t->is_known_instance_field(), "instance required");
  int instance_id = adr_t->instance_id();
  assert((uint)instance_id == alloc->_idx, "wrong allocation");

  int alias_idx = C->get_alias_index(adr_t);
  int offset = adr_t->offset();
  Node *start_mem = C->start()->proj_out_or_null(TypeFunc::Memory);
  Node *alloc_ctrl = alloc->in(TypeFunc::Control);
  Node *alloc_mem = alloc->in(TypeFunc::Memory);
  VectorSet visited;

  bool done = sfpt_mem == alloc_mem;
  Node *mem = sfpt_mem;
  while (!done) {
    if (visited.test_set(mem->_idx)) {
      return NULL;  // found a loop, give up
    }
    mem = scan_mem_chain(mem, alias_idx, offset, start_mem, alloc, &_igvn);
    if (mem == start_mem || mem == alloc_mem) {
      done = true;  // hit a sentinel, return appropriate 0 value
    } else if (mem->is_Initialize()) {
      mem = mem->as_Initialize()->find_captured_store(offset, type2aelembytes(ft), &_igvn);
      if (mem == NULL) {
        done = true; // Something go wrong.
      } else if (mem->is_Store()) {
        const TypePtr* atype = mem->as_Store()->adr_type();
        assert(C->get_alias_index(atype) == Compile::AliasIdxRaw, "store is correct memory slice");
        done = true;
      }
    } else if (mem->is_Store()) {
      const TypeOopPtr* atype = mem->as_Store()->adr_type()->isa_oopptr();
      assert(atype != NULL, "address type must be oopptr");
      assert(C->get_alias_index(atype) == alias_idx &&
             atype->is_known_instance_field() && atype->offset() == offset &&
             atype->instance_id() == instance_id, "store is correct memory slice");
      done = true;
    } else if (mem->is_Phi()) {
      // try to find a phi's unique input
      Node *unique_input = NULL;
      Node *top = C->top();
      for (uint i = 1; i < mem->req(); i++) {
        Node *n = scan_mem_chain(mem->in(i), alias_idx, offset, start_mem, alloc, &_igvn);
        if (n == NULL || n == top || n == mem) {
          continue;
        } else if (unique_input == NULL) {
          unique_input = n;
        } else if (unique_input != n) {
          unique_input = top;
          break;
        }
      }
      if (unique_input != NULL && unique_input != top) {
        mem = unique_input;
      } else {
        done = true;
      }
    } else if (mem->is_ArrayCopy()) {
      done = true;
    } else {
      DEBUG_ONLY( mem->dump(); )
      assert(false, "unexpected node");
    }
  }
  if (mem != NULL) {
    if (mem == start_mem || mem == alloc_mem) {
      // hit a sentinel, return appropriate 0 value
      return _igvn.zerocon(ft);
    } else if (mem->is_Store()) {
      Node* n = mem->in(MemNode::ValueIn);
      BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
      n = bs->step_over_gc_barrier(n);
      return n;
    } else if (mem->is_Phi()) {
      // attempt to produce a Phi reflecting the values on the input paths of the Phi
      Node_Stack value_phis(8);
      Node* phi = value_from_mem_phi(mem, ft, ftype, adr_t, alloc, &value_phis, ValueSearchLimit);
      if (phi != NULL) {
        return phi;
      } else {
        // Kill all new Phis
        while(value_phis.is_nonempty()) {
          Node* n = value_phis.node();
          _igvn.replace_node(n, C->top());
          value_phis.pop();
        }
      }
    } else if (mem->is_ArrayCopy()) {
      Node* ctl = mem->in(0);
      Node* m = mem->in(TypeFunc::Memory);
      if (sfpt_ctl->is_Proj() && sfpt_ctl->as_Proj()->is_uncommon_trap_proj(Deoptimization::Reason_none)) {
        // pin the loads in the uncommon trap path
        ctl = sfpt_ctl;
        m = sfpt_mem;
      }
      return make_arraycopy_load(mem->as_ArrayCopy(), offset, ctl, m, ft, ftype, alloc);
    }
  }
  // Something go wrong.
  return NULL;
}

// Check the possibility of scalar replacement.
bool PhaseMacroExpand::can_eliminate_allocation(AllocateNode *alloc, GrowableArray <SafePointNode *>& safepoints) {
  //  Scan the uses of the allocation to check for anything that would
  //  prevent us from eliminating it.
  NOT_PRODUCT( const char* fail_eliminate = NULL; )
  DEBUG_ONLY( Node* disq_node = NULL; )
  bool  can_eliminate = true;

  Node* res = alloc->result_cast();
  const TypeOopPtr* res_type = NULL;
  if (res == NULL) {
    // All users were eliminated.
  } else if (!res->is_CheckCastPP()) {
    NOT_PRODUCT(fail_eliminate = "Allocation does not have unique CheckCastPP";)
    can_eliminate = false;
  } else {
    res_type = _igvn.type(res)->isa_oopptr();
    if (res_type == NULL) {
      NOT_PRODUCT(fail_eliminate = "Neither instance or array allocation";)
      can_eliminate = false;
    } else if (res_type->isa_aryptr()) {
      int length = alloc->in(AllocateNode::ALength)->find_int_con(-1);
      if (length < 0) {
        NOT_PRODUCT(fail_eliminate = "Array's size is not constant";)
        can_eliminate = false;
      }
    }
  }

  if (can_eliminate && res != NULL) {
    for (DUIterator_Fast jmax, j = res->fast_outs(jmax);
                               j < jmax && can_eliminate; j++) {
      Node* use = res->fast_out(j);

      if (use->is_AddP()) {
        const TypePtr* addp_type = _igvn.type(use)->is_ptr();
        int offset = addp_type->offset();

        if (offset == Type::OffsetTop || offset == Type::OffsetBot) {
          NOT_PRODUCT(fail_eliminate = "Undefined field referrence";)
          can_eliminate = false;
          break;
        }
        for (DUIterator_Fast kmax, k = use->fast_outs(kmax);
                                   k < kmax && can_eliminate; k++) {
          Node* n = use->fast_out(k);
          if (!n->is_Store() && n->Opcode() != Op_CastP2X
              SHENANDOAHGC_ONLY(&& (!UseShenandoahGC || !ShenandoahBarrierSetC2::is_shenandoah_wb_pre_call(n))) ) {
            DEBUG_ONLY(disq_node = n;)
            if (n->is_Load() || n->is_LoadStore()) {
              NOT_PRODUCT(fail_eliminate = "Field load";)
            } else {
              NOT_PRODUCT(fail_eliminate = "Not store field referrence";)
            }
            can_eliminate = false;
          }
        }
      } else if (use->is_ArrayCopy() &&
                 (use->as_ArrayCopy()->is_clonebasic() ||
                  use->as_ArrayCopy()->is_arraycopy_validated() ||
                  use->as_ArrayCopy()->is_copyof_validated() ||
                  use->as_ArrayCopy()->is_copyofrange_validated()) &&
                 use->in(ArrayCopyNode::Dest) == res) {
        // ok to eliminate
      } else if (use->is_SafePoint()) {
        SafePointNode* sfpt = use->as_SafePoint();
        if (sfpt->is_Call() && sfpt->as_Call()->has_non_debug_use(res)) {
          // Object is passed as argument.
          DEBUG_ONLY(disq_node = use;)
          NOT_PRODUCT(fail_eliminate = "Object is passed as argument";)
          can_eliminate = false;
        }
        Node* sfptMem = sfpt->memory();
        if (sfptMem == NULL || sfptMem->is_top()) {
          DEBUG_ONLY(disq_node = use;)
          NOT_PRODUCT(fail_eliminate = "NULL or TOP memory";)
          can_eliminate = false;
        } else {
          safepoints.append_if_missing(sfpt);
        }
      } else if (use->Opcode() != Op_CastP2X) { // CastP2X is used by card mark
        if (use->is_Phi()) {
          if (use->outcnt() == 1 && use->unique_out()->Opcode() == Op_Return) {
            NOT_PRODUCT(fail_eliminate = "Object is return value";)
          } else {
            NOT_PRODUCT(fail_eliminate = "Object is referenced by Phi";)
          }
          DEBUG_ONLY(disq_node = use;)
        } else {
          if (use->Opcode() == Op_Return) {
            NOT_PRODUCT(fail_eliminate = "Object is return value";)
          }else {
            NOT_PRODUCT(fail_eliminate = "Object is referenced by node";)
          }
          DEBUG_ONLY(disq_node = use;)
        }
        can_eliminate = false;
      }
    }
  }

#ifndef PRODUCT
  if (PrintEliminateAllocations) {
    if (can_eliminate) {
      tty->print("Scalar ");
      if (res == NULL)
        alloc->dump();
      else
        res->dump();
    } else if (alloc->_is_scalar_replaceable) {
      tty->print("NotScalar (%s)", fail_eliminate);
      if (res == NULL)
        alloc->dump();
      else
        res->dump();
#ifdef ASSERT
      if (disq_node != NULL) {
          tty->print("  >>>> ");
          disq_node->dump();
      }
#endif /*ASSERT*/
    }
  }
#endif
  return can_eliminate;
}

// Do scalar replacement.
bool PhaseMacroExpand::scalar_replacement(AllocateNode *alloc, GrowableArray <SafePointNode *>& safepoints) {
  GrowableArray <SafePointNode *> safepoints_done;

  ciKlass* klass = NULL;
  ciInstanceKlass* iklass = NULL;
  int nfields = 0;
  int array_base = 0;
  int element_size = 0;
  BasicType basic_elem_type = T_ILLEGAL;
  ciType* elem_type = NULL;

  Node* res = alloc->result_cast();
  assert(res == NULL || res->is_CheckCastPP(), "unexpected AllocateNode result");
  const TypeOopPtr* res_type = NULL;
  if (res != NULL) { // Could be NULL when there are no users
    res_type = _igvn.type(res)->isa_oopptr();
  }

  if (res != NULL) {
    klass = res_type->klass();
    if (res_type->isa_instptr()) {
      // find the fields of the class which will be needed for safepoint debug information
      assert(klass->is_instance_klass(), "must be an instance klass.");
      iklass = klass->as_instance_klass();
      nfields = iklass->nof_nonstatic_fields();
    } else {
      // find the array's elements which will be needed for safepoint debug information
      nfields = alloc->in(AllocateNode::ALength)->find_int_con(-1);
      assert(klass->is_array_klass() && nfields >= 0, "must be an array klass.");
      elem_type = klass->as_array_klass()->element_type();
      basic_elem_type = elem_type->basic_type();
      array_base = arrayOopDesc::base_offset_in_bytes(basic_elem_type);
      element_size = type2aelembytes(basic_elem_type);
    }
  }
  //
  // Process the safepoint uses
  //
  while (safepoints.length() > 0) {
    SafePointNode* sfpt = safepoints.pop();
    Node* mem = sfpt->memory();
    Node* ctl = sfpt->control();
    assert(sfpt->jvms() != NULL, "missed JVMS");
    // Fields of scalar objs are referenced only at the end
    // of regular debuginfo at the last (youngest) JVMS.
    // Record relative start index.
    uint first_ind = (sfpt->req() - sfpt->jvms()->scloff());
    SafePointScalarObjectNode* sobj = new SafePointScalarObjectNode(res_type,
#ifdef ASSERT
                                                 alloc,
#endif
                                                 first_ind, nfields);
    sobj->init_req(0, C->root());
    transform_later(sobj);

    // Scan object's fields adding an input to the safepoint for each field.
    for (int j = 0; j < nfields; j++) {
      intptr_t offset;
      ciField* field = NULL;
      if (iklass != NULL) {
        field = iklass->nonstatic_field_at(j);
        offset = field->offset();
        elem_type = field->type();
        basic_elem_type = field->layout_type();
      } else {
        offset = array_base + j * (intptr_t)element_size;
      }

      const Type *field_type;
      // The next code is taken from Parse::do_get_xxx().
      if (is_reference_type(basic_elem_type)) {
        if (!elem_type->is_loaded()) {
          field_type = TypeInstPtr::BOTTOM;
        } else if (field != NULL && field->is_static_constant()) {
          // This can happen if the constant oop is non-perm.
          ciObject* con = field->constant_value().as_object();
          // Do not "join" in the previous type; it doesn't add value,
          // and may yield a vacuous result if the field is of interface type.
          field_type = TypeOopPtr::make_from_constant(con)->isa_oopptr();
          assert(field_type != NULL, "field singleton type must be consistent");
        } else {
          field_type = TypeOopPtr::make_from_klass(elem_type->as_klass());
        }
        if (UseCompressedOops) {
          field_type = field_type->make_narrowoop();
          basic_elem_type = T_NARROWOOP;
        }
      } else {
        field_type = Type::get_const_basic_type(basic_elem_type);
      }

      const TypeOopPtr *field_addr_type = res_type->add_offset(offset)->isa_oopptr();

      Node *field_val = value_from_mem(mem, ctl, basic_elem_type, field_type, field_addr_type, alloc);
      if (field_val == NULL) {
        // We weren't able to find a value for this field,
        // give up on eliminating this allocation.

        // Remove any extra entries we added to the safepoint.
        uint last = sfpt->req() - 1;
        for (int k = 0;  k < j; k++) {
          sfpt->del_req(last--);
        }
        _igvn._worklist.push(sfpt);
        // rollback processed safepoints
        while (safepoints_done.length() > 0) {
          SafePointNode* sfpt_done = safepoints_done.pop();
          // remove any extra entries we added to the safepoint
          last = sfpt_done->req() - 1;
          for (int k = 0;  k < nfields; k++) {
            sfpt_done->del_req(last--);
          }
          JVMState *jvms = sfpt_done->jvms();
          jvms->set_endoff(sfpt_done->req());
          // Now make a pass over the debug information replacing any references
          // to SafePointScalarObjectNode with the allocated object.
          int start = jvms->debug_start();
          int end   = jvms->debug_end();
          for (int i = start; i < end; i++) {
            if (sfpt_done->in(i)->is_SafePointScalarObject()) {
              SafePointScalarObjectNode* scobj = sfpt_done->in(i)->as_SafePointScalarObject();
              if (scobj->first_index(jvms) == sfpt_done->req() &&
                  scobj->n_fields() == (uint)nfields) {
                assert(scobj->alloc() == alloc, "sanity");
                sfpt_done->set_req(i, res);
              }
            }
          }
          _igvn._worklist.push(sfpt_done);
        }
#ifndef PRODUCT
        if (PrintEliminateAllocations) {
          if (field != NULL) {
            tty->print("=== At SafePoint node %d can't find value of Field: ",
                       sfpt->_idx);
            field->print();
            int field_idx = C->get_alias_index(field_addr_type);
            tty->print(" (alias_idx=%d)", field_idx);
          } else { // Array's element
            tty->print("=== At SafePoint node %d can't find value of array element [%d]",
                       sfpt->_idx, j);
          }
          tty->print(", which prevents elimination of: ");
          if (res == NULL)
            alloc->dump();
          else
            res->dump();
        }
#endif
        return false;
      }
      if (UseCompressedOops && field_type->isa_narrowoop()) {
        // Enable "DecodeN(EncodeP(Allocate)) --> Allocate" transformation
        // to be able scalar replace the allocation.
        if (field_val->is_EncodeP()) {
          field_val = field_val->in(1);
        } else {
          field_val = transform_later(new DecodeNNode(field_val, field_val->get_ptr_type()));
        }
      }
      sfpt->add_req(field_val);
    }
    JVMState *jvms = sfpt->jvms();
    jvms->set_endoff(sfpt->req());
    // Now make a pass over the debug information replacing any references
    // to the allocated object with "sobj"
    int start = jvms->debug_start();
    int end   = jvms->debug_end();
    sfpt->replace_edges_in_range(res, sobj, start, end, &_igvn);
    _igvn._worklist.push(sfpt);
    safepoints_done.append_if_missing(sfpt); // keep it for rollback
  }
  return true;
}

static void disconnect_projections(MultiNode* n, PhaseIterGVN& igvn) {
  Node* ctl_proj = n->proj_out_or_null(TypeFunc::Control);
  Node* mem_proj = n->proj_out_or_null(TypeFunc::Memory);
  if (ctl_proj != NULL) {
    igvn.replace_node(ctl_proj, n->in(0));
  }
  if (mem_proj != NULL) {
    igvn.replace_node(mem_proj, n->in(TypeFunc::Memory));
  }
}

// Process users of eliminated allocation.
void PhaseMacroExpand::process_users_of_allocation(CallNode *alloc) {
  Node* res = alloc->result_cast();
  if (res != NULL) {
    for (DUIterator_Last jmin, j = res->last_outs(jmin); j >= jmin; ) {
      Node *use = res->last_out(j);
      uint oc1 = res->outcnt();

      if (use->is_AddP()) {
        for (DUIterator_Last kmin, k = use->last_outs(kmin); k >= kmin; ) {
          Node *n = use->last_out(k);
          uint oc2 = use->outcnt();
          if (n->is_Store()) {
#ifdef ASSERT
            // Verify that there is no dependent MemBarVolatile nodes,
            // they should be removed during IGVN, see MemBarNode::Ideal().
            for (DUIterator_Fast pmax, p = n->fast_outs(pmax);
                                       p < pmax; p++) {
              Node* mb = n->fast_out(p);
              assert(mb->is_Initialize() || !mb->is_MemBar() ||
                     mb->req() <= MemBarNode::Precedent ||
                     mb->in(MemBarNode::Precedent) != n,
                     "MemBarVolatile should be eliminated for non-escaping object");
            }
#endif
            _igvn.replace_node(n, n->in(MemNode::Memory));
          } else {
            eliminate_gc_barrier(n);
          }
          k -= (oc2 - use->outcnt());
        }
        _igvn.remove_dead_node(use);
      } else if (use->is_ArrayCopy()) {
        // Disconnect ArrayCopy node
        ArrayCopyNode* ac = use->as_ArrayCopy();
        if (ac->is_clonebasic()) {
          Node* membar_after = ac->proj_out(TypeFunc::Control)->unique_ctrl_out();
          disconnect_projections(ac, _igvn);
          assert(alloc->in(TypeFunc::Memory)->is_Proj() && alloc->in(TypeFunc::Memory)->in(0)->Opcode() == Op_MemBarCPUOrder, "mem barrier expected before allocation");
          Node* membar_before = alloc->in(TypeFunc::Memory)->in(0);
          disconnect_projections(membar_before->as_MemBar(), _igvn);
          if (membar_after->is_MemBar()) {
            disconnect_projections(membar_after->as_MemBar(), _igvn);
          }
        } else {
          assert(ac->is_arraycopy_validated() ||
                 ac->is_copyof_validated() ||
                 ac->is_copyofrange_validated(), "unsupported");
          CallProjections callprojs;
          ac->extract_projections(&callprojs, true);

          _igvn.replace_node(callprojs.fallthrough_ioproj, ac->in(TypeFunc::I_O));
          _igvn.replace_node(callprojs.fallthrough_memproj, ac->in(TypeFunc::Memory));
          _igvn.replace_node(callprojs.fallthrough_catchproj, ac->in(TypeFunc::Control));

          // Set control to top. IGVN will remove the remaining projections
          ac->set_req(0, top());
          ac->replace_edge(res, top(), &_igvn);

          // Disconnect src right away: it can help find new
          // opportunities for allocation elimination
          Node* src = ac->in(ArrayCopyNode::Src);
          ac->replace_edge(src, top(), &_igvn);
          // src can be top at this point if src and dest of the
          // arraycopy were the same
          if (src->outcnt() == 0 && !src->is_top()) {
            _igvn.remove_dead_node(src);
          }
        }
        _igvn._worklist.push(ac);
      } else {
        eliminate_gc_barrier(use);
      }
      j -= (oc1 - res->outcnt());
    }
    assert(res->outcnt() == 0, "all uses of allocated objects must be deleted");
    _igvn.remove_dead_node(res);
  }

  //
  // Process other users of allocation's projections
  //
  if (_callprojs.resproj != NULL && _callprojs.resproj->outcnt() != 0) {
    // First disconnect stores captured by Initialize node.
    // If Initialize node is eliminated first in the following code,
    // it will kill such stores and DUIterator_Last will assert.
    for (DUIterator_Fast jmax, j = _callprojs.resproj->fast_outs(jmax);  j < jmax; j++) {
      Node* use = _callprojs.resproj->fast_out(j);
      if (use->is_AddP()) {
        // raw memory addresses used only by the initialization
        _igvn.replace_node(use, C->top());
        --j; --jmax;
      }
    }
    for (DUIterator_Last jmin, j = _callprojs.resproj->last_outs(jmin); j >= jmin; ) {
      Node* use = _callprojs.resproj->last_out(j);
      uint oc1 = _callprojs.resproj->outcnt();
      if (use->is_Initialize()) {
        // Eliminate Initialize node.
        InitializeNode *init = use->as_Initialize();
        assert(init->outcnt() <= 2, "only a control and memory projection expected");
        Node *ctrl_proj = init->proj_out_or_null(TypeFunc::Control);
        if (ctrl_proj != NULL) {
          _igvn.replace_node(ctrl_proj, init->in(TypeFunc::Control));
#ifdef ASSERT
          // If the InitializeNode has no memory out, it will die, and tmp will become NULL
          Node* tmp = init->in(TypeFunc::Control);
          assert(tmp == NULL || tmp == _callprojs.fallthrough_catchproj, "allocation control projection");
#endif
        }
        Node *mem_proj = init->proj_out_or_null(TypeFunc::Memory);
        if (mem_proj != NULL) {
          Node *mem = init->in(TypeFunc::Memory);
#ifdef ASSERT
          if (mem->is_MergeMem()) {
            assert(mem->in(TypeFunc::Memory) == _callprojs.fallthrough_memproj, "allocation memory projection");
          } else {
            assert(mem == _callprojs.fallthrough_memproj, "allocation memory projection");
          }
#endif
          _igvn.replace_node(mem_proj, mem);
        }
      } else  {
        assert(false, "only Initialize or AddP expected");
      }
      j -= (oc1 - _callprojs.resproj->outcnt());
    }
  }
  if (_callprojs.fallthrough_catchproj != NULL) {
    _igvn.replace_node(_callprojs.fallthrough_catchproj, alloc->in(TypeFunc::Control));
  }
  if (_callprojs.fallthrough_memproj != NULL) {
    _igvn.replace_node(_callprojs.fallthrough_memproj, alloc->in(TypeFunc::Memory));
  }
  if (_callprojs.catchall_memproj != NULL) {
    _igvn.replace_node(_callprojs.catchall_memproj, C->top());
  }
  if (_callprojs.fallthrough_ioproj != NULL) {
    _igvn.replace_node(_callprojs.fallthrough_ioproj, alloc->in(TypeFunc::I_O));
  }
  if (_callprojs.catchall_ioproj != NULL) {
    _igvn.replace_node(_callprojs.catchall_ioproj, C->top());
  }
  if (_callprojs.catchall_catchproj != NULL) {
    _igvn.replace_node(_callprojs.catchall_catchproj, C->top());
  }
}

bool PhaseMacroExpand::eliminate_allocate_node(AllocateNode *alloc) {
  // If reallocation fails during deoptimization we'll pop all
  // interpreter frames for this compiled frame and that won't play
  // nice with JVMTI popframe.
  // We avoid this issue by eager reallocation when the popframe request
  // is received.
  if (!EliminateAllocations || !alloc->_is_non_escaping) {
    return false;
  }
  Node* klass = alloc->in(AllocateNode::KlassNode);
  const TypeKlassPtr* tklass = _igvn.type(klass)->is_klassptr();
  Node* res = alloc->result_cast();
  // Eliminate boxing allocations which are not used
  // regardless scalar replacable status.
  bool boxing_alloc = C->eliminate_boxing() &&
                      tklass->klass()->is_instance_klass()  &&
                      tklass->klass()->as_instance_klass()->is_box_klass();
  if (!alloc->_is_scalar_replaceable && (!boxing_alloc || (res != NULL))) {
    return false;
  }

  alloc->extract_projections(&_callprojs, false /*separate_io_proj*/, false /*do_asserts*/);

  GrowableArray <SafePointNode *> safepoints;
  if (!can_eliminate_allocation(alloc, safepoints)) {
    return false;
  }

  if (!alloc->_is_scalar_replaceable) {
    assert(res == NULL, "sanity");
    // We can only eliminate allocation if all debug info references
    // are already replaced with SafePointScalarObject because
    // we can't search for a fields value without instance_id.
    if (safepoints.length() > 0) {
      return false;
    }
  }

  if (!scalar_replacement(alloc, safepoints)) {
    return false;
  }

  CompileLog* log = C->log();
  if (log != NULL) {
    log->head("eliminate_allocation type='%d'",
              log->identify(tklass->klass()));
    JVMState* p = alloc->jvms();
    while (p != NULL) {
      log->elem("jvms bci='%d' method='%d'", p->bci(), log->identify(p->method()));
      p = p->caller();
    }
    log->tail("eliminate_allocation");
  }

  process_users_of_allocation(alloc);

#ifndef PRODUCT
  if (PrintEliminateAllocations) {
    if (alloc->is_AllocateArray())
      tty->print_cr("++++ Eliminated: %d AllocateArray", alloc->_idx);
    else
      tty->print_cr("++++ Eliminated: %d Allocate", alloc->_idx);
  }
#endif

  return true;
}

bool PhaseMacroExpand::eliminate_boxing_node(CallStaticJavaNode *boxing) {
  // EA should remove all uses of non-escaping boxing node.
  if (!C->eliminate_boxing() || boxing->proj_out_or_null(TypeFunc::Parms) != NULL) {
    return false;
  }

  assert(boxing->result_cast() == NULL, "unexpected boxing node result");

  boxing->extract_projections(&_callprojs, false /*separate_io_proj*/, false /*do_asserts*/);

  const TypeTuple* r = boxing->tf()->range();
  assert(r->cnt() > TypeFunc::Parms, "sanity");
  const TypeInstPtr* t = r->field_at(TypeFunc::Parms)->isa_instptr();
  assert(t != NULL, "sanity");

  CompileLog* log = C->log();
  if (log != NULL) {
    log->head("eliminate_boxing type='%d'",
              log->identify(t->klass()));
    JVMState* p = boxing->jvms();
    while (p != NULL) {
      log->elem("jvms bci='%d' method='%d'", p->bci(), log->identify(p->method()));
      p = p->caller();
    }
    log->tail("eliminate_boxing");
  }

  process_users_of_allocation(boxing);

#ifndef PRODUCT
  if (PrintEliminateAllocations) {
    tty->print("++++ Eliminated: %d ", boxing->_idx);
    boxing->method()->print_short_name(tty);
    tty->cr();
  }
#endif

  return true;
}

//---------------------------set_eden_pointers-------------------------
void PhaseMacroExpand::set_eden_pointers(Node* &eden_top_adr, Node* &eden_end_adr) {
  if (UseTLAB) {                // Private allocation: load from TLS
    Node* thread = transform_later(new ThreadLocalNode());
    int tlab_top_offset = in_bytes(JavaThread::tlab_top_offset());
    int tlab_end_offset = in_bytes(JavaThread::tlab_end_offset());
    eden_top_adr = basic_plus_adr(top()/*not oop*/, thread, tlab_top_offset);
    eden_end_adr = basic_plus_adr(top()/*not oop*/, thread, tlab_end_offset);
  } else {                      // Shared allocation: load from globals
    CollectedHeap* ch = Universe::heap();
    address top_adr = (address)ch->top_addr();
    address end_adr = (address)ch->end_addr();
    eden_top_adr = makecon(TypeRawPtr::make(top_adr));
    eden_end_adr = basic_plus_adr(eden_top_adr, end_adr - top_adr);
  }
}


Node* PhaseMacroExpand::make_load(Node* ctl, Node* mem, Node* base, int offset, const Type* value_type, BasicType bt) {
  Node* adr = basic_plus_adr(base, offset);
  const TypePtr* adr_type = adr->bottom_type()->is_ptr();
  Node* value = LoadNode::make(_igvn, ctl, mem, adr, adr_type, value_type, bt, MemNode::unordered);
  transform_later(value);
  return value;
}


Node* PhaseMacroExpand::make_store(Node* ctl, Node* mem, Node* base, int offset, Node* value, BasicType bt) {
  Node* adr = basic_plus_adr(base, offset);
  mem = StoreNode::make(_igvn, ctl, mem, adr, NULL, value, bt, MemNode::unordered);
  transform_later(mem);
  return mem;
}

//=============================================================================
//
//                              A L L O C A T I O N
//
// Allocation attempts to be fast in the case of frequent small objects.
// It breaks down like this:
//
// 1) Size in doublewords is computed.  This is a constant for objects and
// variable for most arrays.  Doubleword units are used to avoid size
// overflow of huge doubleword arrays.  We need doublewords in the end for
// rounding.
//
// 2) Size is checked for being 'too large'.  Too-large allocations will go
// the slow path into the VM.  The slow path can throw any required
// exceptions, and does all the special checks for very large arrays.  The
// size test can constant-fold away for objects.  For objects with
// finalizers it constant-folds the otherway: you always go slow with
// finalizers.
//
// 3) If NOT using TLABs, this is the contended loop-back point.
// Load-Locked the heap top.  If using TLABs normal-load the heap top.
//
// 4) Check that heap top + size*8 < max.  If we fail go the slow ` route.
// NOTE: "top+size*8" cannot wrap the 4Gig line!  Here's why: for largish
// "size*8" we always enter the VM, where "largish" is a constant picked small
// enough that there's always space between the eden max and 4Gig (old space is
// there so it's quite large) and large enough that the cost of entering the VM
// is dwarfed by the cost to initialize the space.
//
// 5) If NOT using TLABs, Store-Conditional the adjusted heap top back
// down.  If contended, repeat at step 3.  If using TLABs normal-store
// adjusted heap top back down; there is no contention.
//
// 6) If !ZeroTLAB then Bulk-clear the object/array.  Fill in klass & mark
// fields.
//
// 7) Merge with the slow-path; cast the raw memory pointer to the correct
// oop flavor.
//
//=============================================================================
// FastAllocateSizeLimit value is in DOUBLEWORDS.
// Allocations bigger than this always go the slow route.
// This value must be small enough that allocation attempts that need to
// trigger exceptions go the slow route.  Also, it must be small enough so
// that heap_top + size_in_bytes does not wrap around the 4Gig limit.
//=============================================================================j//
// %%% Here is an old comment from parseHelper.cpp; is it outdated?
// The allocator will coalesce int->oop copies away.  See comment in
// coalesce.cpp about how this works.  It depends critically on the exact
// code shape produced here, so if you are changing this code shape
// make sure the GC info for the heap-top is correct in and around the
// slow-path call.
//

void PhaseMacroExpand::expand_allocate_common(
            AllocateNode* alloc, // allocation node to be expanded
            Node* length,  // array length for an array allocation
            const TypeFunc* slow_call_type, // Type of slow call
            address slow_call_address  // Address of slow call
    )
{
  Node* ctrl = alloc->in(TypeFunc::Control);
  Node* mem  = alloc->in(TypeFunc::Memory);
  Node* i_o  = alloc->in(TypeFunc::I_O);
  Node* size_in_bytes     = alloc->in(AllocateNode::AllocSize);
  Node* klass_node        = alloc->in(AllocateNode::KlassNode);
  Node* initial_slow_test = alloc->in(AllocateNode::InitialTest);
  assert(ctrl != NULL, "must have control");

  // We need a Region and corresponding Phi's to merge the slow-path and fast-path results.
  // they will not be used if "always_slow" is set
  enum { slow_result_path = 1, fast_result_path = 2 };
  Node *result_region = NULL;
  Node *result_phi_rawmem = NULL;
  Node *result_phi_rawoop = NULL;
  Node *result_phi_i_o = NULL;

  // The initial slow comparison is a size check, the comparison
  // we want to do is a BoolTest::gt
  bool expand_fast_path = true;
  int tv = _igvn.find_int_con(initial_slow_test, -1);
  if (tv >= 0) {
    // InitialTest has constant result
    //   0 - can fit in TLAB
    //   1 - always too big or negative
    assert(tv <= 1, "0 or 1 if a constant");
    expand_fast_path = (tv == 0);
    initial_slow_test = NULL;
  } else {
    initial_slow_test = BoolNode::make_predicate(initial_slow_test, &_igvn);
  }

  if (C->env()->dtrace_alloc_probes() ||
      (!UseTLAB && !Universe::heap()->supports_inline_contig_alloc())) {
    // Force slow-path allocation
    expand_fast_path = false;
    initial_slow_test = NULL;
  }

  bool allocation_has_use = (alloc->result_cast() != NULL);
  if (!allocation_has_use) {
    InitializeNode* init = alloc->initialization();
    if (init != NULL) {
      init->remove(&_igvn);
    }
    if (expand_fast_path && (initial_slow_test == NULL)) {
      // Remove allocation node and return.
      // Size is a non-negative constant -> no initial check needed -> directly to fast path.
      // Also, no usages -> empty fast path -> no fall out to slow path -> nothing left.
#ifndef PRODUCT
      if (PrintEliminateAllocations) {
        tty->print("NotUsed ");
        Node* res = alloc->proj_out_or_null(TypeFunc::Parms);
        if (res != NULL) {
          res->dump();
        } else {
          alloc->dump();
        }
      }
#endif
      yank_alloc_node(alloc);
      return;
    }
  }

  enum { too_big_or_final_path = 1, need_gc_path = 2 };
  Node *slow_region = NULL;
  Node *toobig_false = ctrl;

  // generate the initial test if necessary
  if (initial_slow_test != NULL ) {
    assert (expand_fast_path, "Only need test if there is a fast path");
    slow_region = new RegionNode(3);

    // Now make the initial failure test.  Usually a too-big test but
    // might be a TRUE for finalizers or a fancy class check for
    // newInstance0.
    IfNode *toobig_iff = new IfNode(ctrl, initial_slow_test, PROB_MIN, COUNT_UNKNOWN);
    transform_later(toobig_iff);
    // Plug the failing-too-big test into the slow-path region
    Node *toobig_true = new IfTrueNode( toobig_iff );
    transform_later(toobig_true);
    slow_region    ->init_req( too_big_or_final_path, toobig_true );
    toobig_false = new IfFalseNode( toobig_iff );
    transform_later(toobig_false);
  } else {
    // No initial test, just fall into next case
    assert(allocation_has_use || !expand_fast_path, "Should already have been handled");
    toobig_false = ctrl;
    debug_only(slow_region = NodeSentinel);
  }

  // If we are here there are several possibilities
  // - expand_fast_path is false - then only a slow path is expanded. That's it.
  // no_initial_check means a constant allocation.
  // - If check always evaluates to false -> expand_fast_path is false (see above)
  // - If check always evaluates to true -> directly into fast path (but may bailout to slowpath)
  // if !allocation_has_use the fast path is empty
  // if !allocation_has_use && no_initial_check
  // - Then there are no fastpath that can fall out to slowpath -> no allocation code at all.
  //   removed by yank_alloc_node above.

  Node *slow_mem = mem;  // save the current memory state for slow path
  // generate the fast allocation code unless we know that the initial test will always go slow
  if (expand_fast_path) {
    // Fast path modifies only raw memory.
    if (mem->is_MergeMem()) {
      mem = mem->as_MergeMem()->memory_at(Compile::AliasIdxRaw);
    }

    // allocate the Region and Phi nodes for the result
    result_region = new RegionNode(3);
    result_phi_rawmem = new PhiNode(result_region, Type::MEMORY, TypeRawPtr::BOTTOM);
    result_phi_i_o    = new PhiNode(result_region, Type::ABIO); // I/O is used for Prefetch

    // Grab regular I/O before optional prefetch may change it.
    // Slow-path does no I/O so just set it to the original I/O.
    result_phi_i_o->init_req(slow_result_path, i_o);

    // Name successful fast-path variables
    Node* fast_oop_ctrl;
    Node* fast_oop_rawmem;
    if (allocation_has_use) {
      Node* needgc_ctrl = NULL;
      result_phi_rawoop = new PhiNode(result_region, TypeRawPtr::BOTTOM);

      intx prefetch_lines = length != NULL ? AllocatePrefetchLines : AllocateInstancePrefetchLines;
      BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
      Node* fast_oop = bs->obj_allocate(this, mem, toobig_false, size_in_bytes, i_o, needgc_ctrl,
                                        fast_oop_ctrl, fast_oop_rawmem,
                                        prefetch_lines);

      if (initial_slow_test != NULL) {
        // This completes all paths into the slow merge point
        slow_region->init_req(need_gc_path, needgc_ctrl);
        transform_later(slow_region);
      } else {
        // No initial slow path needed!
        // Just fall from the need-GC path straight into the VM call.
        slow_region = needgc_ctrl;
      }

      InitializeNode* init = alloc->initialization();
      fast_oop_rawmem = initialize_object(alloc,
                                          fast_oop_ctrl, fast_oop_rawmem, fast_oop,
                                          klass_node, length, size_in_bytes);
      expand_initialize_membar(alloc, init, fast_oop_ctrl, fast_oop_rawmem);
      expand_dtrace_alloc_probe(alloc, fast_oop, fast_oop_ctrl, fast_oop_rawmem);

      result_phi_rawoop->init_req(fast_result_path, fast_oop);
    } else {
      assert (initial_slow_test != NULL, "sanity");
      fast_oop_ctrl   = toobig_false;
      fast_oop_rawmem = mem;
      transform_later(slow_region);
    }

    // Plug in the successful fast-path into the result merge point
    result_region    ->init_req(fast_result_path, fast_oop_ctrl);
    result_phi_i_o   ->init_req(fast_result_path, i_o);
    result_phi_rawmem->init_req(fast_result_path, fast_oop_rawmem);
  } else {
    slow_region = ctrl;
    result_phi_i_o = i_o; // Rename it to use in the following code.
  }

  // Generate slow-path call
  CallNode *call = new CallStaticJavaNode(slow_call_type, slow_call_address,
                               OptoRuntime::stub_name(slow_call_address),
                               TypePtr::BOTTOM);
  call->init_req(TypeFunc::Control,   slow_region);
  call->init_req(TypeFunc::I_O,       top());    // does no i/o
  call->init_req(TypeFunc::Memory,    slow_mem); // may gc ptrs
  call->init_req(TypeFunc::ReturnAdr, alloc->in(TypeFunc::ReturnAdr));
  call->init_req(TypeFunc::FramePtr,  alloc->in(TypeFunc::FramePtr));

  call->init_req(TypeFunc::Parms+0, klass_node);
  if (length != NULL) {
    call->init_req(TypeFunc::Parms+1, length);
  }

  // Copy debug information and adjust JVMState information, then replace
  // allocate node with the call
  call->copy_call_debug_info(&_igvn, alloc);
  if (expand_fast_path) {
    call->set_cnt(PROB_UNLIKELY_MAG(4));  // Same effect as RC_UNCOMMON.
  } else {
    // Hook i_o projection to avoid its elimination during allocation
    // replacement (when only a slow call is generated).
    call->set_req(TypeFunc::I_O, result_phi_i_o);
  }
  _igvn.replace_node(alloc, call);
  transform_later(call);

  // Identify the output projections from the allocate node and
  // adjust any references to them.
  // The control and io projections look like:
  //
  //        v---Proj(ctrl) <-----+   v---CatchProj(ctrl)
  //  Allocate                   Catch
  //        ^---Proj(io) <-------+   ^---CatchProj(io)
  //
  //  We are interested in the CatchProj nodes.
  //
  call->extract_projections(&_callprojs, false /*separate_io_proj*/, false /*do_asserts*/);

  // An allocate node has separate memory projections for the uses on
  // the control and i_o paths. Replace the control memory projection with
  // result_phi_rawmem (unless we are only generating a slow call when
  // both memory projections are combined)
  if (expand_fast_path && _callprojs.fallthrough_memproj != NULL) {
    migrate_outs(_callprojs.fallthrough_memproj, result_phi_rawmem);
  }
  // Now change uses of catchall_memproj to use fallthrough_memproj and delete
  // catchall_memproj so we end up with a call that has only 1 memory projection.
  if (_callprojs.catchall_memproj != NULL ) {
    if (_callprojs.fallthrough_memproj == NULL) {
      _callprojs.fallthrough_memproj = new ProjNode(call, TypeFunc::Memory);
      transform_later(_callprojs.fallthrough_memproj);
    }
    migrate_outs(_callprojs.catchall_memproj, _callprojs.fallthrough_memproj);
    _igvn.remove_dead_node(_callprojs.catchall_memproj);
  }

  // An allocate node has separate i_o projections for the uses on the control
  // and i_o paths. Always replace the control i_o projection with result i_o
  // otherwise incoming i_o become dead when only a slow call is generated
  // (it is different from memory projections where both projections are
  // combined in such case).
  if (_callprojs.fallthrough_ioproj != NULL) {
    migrate_outs(_callprojs.fallthrough_ioproj, result_phi_i_o);
  }
  // Now change uses of catchall_ioproj to use fallthrough_ioproj and delete
  // catchall_ioproj so we end up with a call that has only 1 i_o projection.
  if (_callprojs.catchall_ioproj != NULL ) {
    if (_callprojs.fallthrough_ioproj == NULL) {
      _callprojs.fallthrough_ioproj = new ProjNode(call, TypeFunc::I_O);
      transform_later(_callprojs.fallthrough_ioproj);
    }
    migrate_outs(_callprojs.catchall_ioproj, _callprojs.fallthrough_ioproj);
    _igvn.remove_dead_node(_callprojs.catchall_ioproj);
  }

  // if we generated only a slow call, we are done
  if (!expand_fast_path) {
    // Now we can unhook i_o.
    if (result_phi_i_o->outcnt() > 1) {
      call->set_req(TypeFunc::I_O, top());
    } else {
      assert(result_phi_i_o->unique_ctrl_out() == call, "sanity");
      // Case of new array with negative size known during compilation.
      // AllocateArrayNode::Ideal() optimization disconnect unreachable
      // following code since call to runtime will throw exception.
      // As result there will be no users of i_o after the call.
      // Leave i_o attached to this call to avoid problems in preceding graph.
    }
    return;
  }

  if (_callprojs.fallthrough_catchproj != NULL) {
    ctrl = _callprojs.fallthrough_catchproj->clone();
    transform_later(ctrl);
    _igvn.replace_node(_callprojs.fallthrough_catchproj, result_region);
  } else {
    ctrl = top();
  }
  Node *slow_result;
  if (_callprojs.resproj == NULL) {
    // no uses of the allocation result
    slow_result = top();
  } else {
    slow_result = _callprojs.resproj->clone();
    transform_later(slow_result);
    _igvn.replace_node(_callprojs.resproj, result_phi_rawoop);
  }

  // Plug slow-path into result merge point
  result_region->init_req( slow_result_path, ctrl);
  transform_later(result_region);
  if (allocation_has_use) {
    result_phi_rawoop->init_req(slow_result_path, slow_result);
    transform_later(result_phi_rawoop);
  }
  result_phi_rawmem->init_req(slow_result_path, _callprojs.fallthrough_memproj);
  transform_later(result_phi_rawmem);
  transform_later(result_phi_i_o);
  // This completes all paths into the result merge point
}

// Remove alloc node that has no uses.
void PhaseMacroExpand::yank_alloc_node(AllocateNode* alloc) {
  Node* ctrl = alloc->in(TypeFunc::Control);
  Node* mem  = alloc->in(TypeFunc::Memory);
  Node* i_o  = alloc->in(TypeFunc::I_O);

  alloc->extract_projections(&_callprojs, false /*separate_io_proj*/, false /*do_asserts*/);
  if (_callprojs.resproj != NULL) {
    for (DUIterator_Fast imax, i = _callprojs.resproj->fast_outs(imax); i < imax; i++) {
      Node* use = _callprojs.resproj->fast_out(i);
      use->isa_MemBar()->remove(&_igvn);
      --imax;
      --i; // back up iterator
    }
    assert(_callprojs.resproj->outcnt() == 0, "all uses must be deleted");
    _igvn.remove_dead_node(_callprojs.resproj);
  }
  if (_callprojs.fallthrough_catchproj != NULL) {
    migrate_outs(_callprojs.fallthrough_catchproj, ctrl);
    _igvn.remove_dead_node(_callprojs.fallthrough_catchproj);
  }
  if (_callprojs.catchall_catchproj != NULL) {
    _igvn.rehash_node_delayed(_callprojs.catchall_catchproj);
    _callprojs.catchall_catchproj->set_req(0, top());
  }
  if (_callprojs.fallthrough_proj != NULL) {
    Node* catchnode = _callprojs.fallthrough_proj->unique_ctrl_out();
    _igvn.remove_dead_node(catchnode);
    _igvn.remove_dead_node(_callprojs.fallthrough_proj);
  }
  if (_callprojs.fallthrough_memproj != NULL) {
    migrate_outs(_callprojs.fallthrough_memproj, mem);
    _igvn.remove_dead_node(_callprojs.fallthrough_memproj);
  }
  if (_callprojs.fallthrough_ioproj != NULL) {
    migrate_outs(_callprojs.fallthrough_ioproj, i_o);
    _igvn.remove_dead_node(_callprojs.fallthrough_ioproj);
  }
  if (_callprojs.catchall_memproj != NULL) {
    _igvn.rehash_node_delayed(_callprojs.catchall_memproj);
    _callprojs.catchall_memproj->set_req(0, top());
  }
  if (_callprojs.catchall_ioproj != NULL) {
    _igvn.rehash_node_delayed(_callprojs.catchall_ioproj);
    _callprojs.catchall_ioproj->set_req(0, top());
  }
#ifndef PRODUCT
  if (PrintEliminateAllocations) {
    if (alloc->is_AllocateArray()) {
      tty->print_cr("++++ Eliminated: %d AllocateArray", alloc->_idx);
    } else {
      tty->print_cr("++++ Eliminated: %d Allocate", alloc->_idx);
    }
  }
#endif
  _igvn.remove_dead_node(alloc);
}

void PhaseMacroExpand::expand_initialize_membar(AllocateNode* alloc, InitializeNode* init,
                                                Node*& fast_oop_ctrl, Node*& fast_oop_rawmem) {
  // If initialization is performed by an array copy, any required
  // MemBarStoreStore was already added. If the object does not
  // escape no need for a MemBarStoreStore. If the object does not
  // escape in its initializer and memory barrier (MemBarStoreStore or
  // stronger) is already added at exit of initializer, also no need
  // for a MemBarStoreStore. Otherwise we need a MemBarStoreStore
  // so that stores that initialize this object can't be reordered
  // with a subsequent store that makes this object accessible by
  // other threads.
  // Other threads include java threads and JVM internal threads
  // (for example concurrent GC threads). Current concurrent GC
  // implementation: G1 will not scan newly created object,
  // so it's safe to skip storestore barrier when allocation does
  // not escape.
  if (!alloc->does_not_escape_thread() &&
    !alloc->is_allocation_MemBar_redundant() &&
    (init == NULL || !init->is_complete_with_arraycopy())) {
    if (init == NULL || init->req() < InitializeNode::RawStores) {
      // No InitializeNode or no stores captured by zeroing
      // elimination. Simply add the MemBarStoreStore after object
      // initialization.
      MemBarNode* mb = MemBarNode::make(C, Op_MemBarStoreStore, Compile::AliasIdxBot);
      transform_later(mb);

      mb->init_req(TypeFunc::Memory, fast_oop_rawmem);
      mb->init_req(TypeFunc::Control, fast_oop_ctrl);
      fast_oop_ctrl = new ProjNode(mb, TypeFunc::Control);
      transform_later(fast_oop_ctrl);
      fast_oop_rawmem = new ProjNode(mb, TypeFunc::Memory);
      transform_later(fast_oop_rawmem);
    } else {
      // Add the MemBarStoreStore after the InitializeNode so that
      // all stores performing the initialization that were moved
      // before the InitializeNode happen before the storestore
      // barrier.

      Node* init_ctrl = init->proj_out_or_null(TypeFunc::Control);
      Node* init_mem = init->proj_out_or_null(TypeFunc::Memory);

      MemBarNode* mb = MemBarNode::make(C, Op_MemBarStoreStore, Compile::AliasIdxBot);
      transform_later(mb);

      Node* ctrl = new ProjNode(init, TypeFunc::Control);
      transform_later(ctrl);
      Node* mem = new ProjNode(init, TypeFunc::Memory);
      transform_later(mem);

      // The MemBarStoreStore depends on control and memory coming
      // from the InitializeNode
      mb->init_req(TypeFunc::Memory, mem);
      mb->init_req(TypeFunc::Control, ctrl);

      ctrl = new ProjNode(mb, TypeFunc::Control);
      transform_later(ctrl);
      mem = new ProjNode(mb, TypeFunc::Memory);
      transform_later(mem);

      // All nodes that depended on the InitializeNode for control
      // and memory must now depend on the MemBarNode that itself
      // depends on the InitializeNode
      if (init_ctrl != NULL) {
        _igvn.replace_node(init_ctrl, ctrl);
      }
      if (init_mem != NULL) {
        _igvn.replace_node(init_mem, mem);
      }
    }
  }
}

void PhaseMacroExpand::expand_dtrace_alloc_probe(AllocateNode* alloc, Node* oop,
                                                Node*& ctrl, Node*& rawmem) {
  if (C->env()->dtrace_extended_probes()) {
    // Slow-path call
    int size = TypeFunc::Parms + 2;
    CallLeafNode *call = new CallLeafNode(OptoRuntime::dtrace_object_alloc_Type(),
                                          CAST_FROM_FN_PTR(address, SharedRuntime::dtrace_object_alloc_base),
                                          "dtrace_object_alloc",
                                          TypeRawPtr::BOTTOM);

    // Get base of thread-local storage area
    Node* thread = new ThreadLocalNode();
    transform_later(thread);

    call->init_req(TypeFunc::Parms + 0, thread);
    call->init_req(TypeFunc::Parms + 1, oop);
    call->init_req(TypeFunc::Control, ctrl);
    call->init_req(TypeFunc::I_O    , top()); // does no i/o
    call->init_req(TypeFunc::Memory , ctrl);
    call->init_req(TypeFunc::ReturnAdr, alloc->in(TypeFunc::ReturnAdr));
    call->init_req(TypeFunc::FramePtr, alloc->in(TypeFunc::FramePtr));
    transform_later(call);
    ctrl = new ProjNode(call, TypeFunc::Control);
    transform_later(ctrl);
    rawmem = new ProjNode(call, TypeFunc::Memory);
    transform_later(rawmem);
  }
}

// Helper for PhaseMacroExpand::expand_allocate_common.
// Initializes the newly-allocated storage.
Node*
PhaseMacroExpand::initialize_object(AllocateNode* alloc,
                                    Node* control, Node* rawmem, Node* object,
                                    Node* klass_node, Node* length,
                                    Node* size_in_bytes) {
  InitializeNode* init = alloc->initialization();
  // Store the klass & mark bits
  Node* mark_node = alloc->make_ideal_mark(&_igvn, object, control, rawmem);
  if (!mark_node->is_Con()) {
    transform_later(mark_node);
  }
  rawmem = make_store(control, rawmem, object, oopDesc::mark_offset_in_bytes(), mark_node, TypeX_X->basic_type());

  rawmem = make_store(control, rawmem, object, oopDesc::klass_offset_in_bytes(), klass_node, T_METADATA);
  int header_size = alloc->minimum_header_size();  // conservatively small

  // Array length
  if (length != NULL) {         // Arrays need length field
    rawmem = make_store(control, rawmem, object, arrayOopDesc::length_offset_in_bytes(), length, T_INT);
    // conservatively small header size:
    header_size = arrayOopDesc::base_offset_in_bytes(T_BYTE);
    ciKlass* k = _igvn.type(klass_node)->is_klassptr()->klass();
    if (k->is_array_klass())    // we know the exact header size in most cases:
      header_size = Klass::layout_helper_header_size(k->layout_helper());
  }

  // Clear the object body, if necessary.
  if (init == NULL) {
    // The init has somehow disappeared; be cautious and clear everything.
    //
    // This can happen if a node is allocated but an uncommon trap occurs
    // immediately.  In this case, the Initialize gets associated with the
    // trap, and may be placed in a different (outer) loop, if the Allocate
    // is in a loop.  If (this is rare) the inner loop gets unrolled, then
    // there can be two Allocates to one Initialize.  The answer in all these
    // edge cases is safety first.  It is always safe to clear immediately
    // within an Allocate, and then (maybe or maybe not) clear some more later.
    if (!(UseTLAB && ZeroTLAB)) {
      rawmem = ClearArrayNode::clear_memory(control, rawmem, object,
                                            header_size, size_in_bytes,
                                            &_igvn);
    }
  } else {
    if (!init->is_complete()) {
      // Try to win by zeroing only what the init does not store.
      // We can also try to do some peephole optimizations,
      // such as combining some adjacent subword stores.
      rawmem = init->complete_stores(control, rawmem, object,
                                     header_size, size_in_bytes, &_igvn);
    }
    // We have no more use for this link, since the AllocateNode goes away:
    init->set_req(InitializeNode::RawAddress, top());
    // (If we keep the link, it just confuses the register allocator,
    // who thinks he sees a real use of the address by the membar.)
  }

  return rawmem;
}

// Generate prefetch instructions for next allocations.
Node* PhaseMacroExpand::prefetch_allocation(Node* i_o, Node*& needgc_false,
                                        Node*& contended_phi_rawmem,
                                        Node* old_eden_top, Node* new_eden_top,
                                        intx lines) {
   enum { fall_in_path = 1, pf_path = 2 };
   if( UseTLAB && AllocatePrefetchStyle == 2 ) {
      // Generate prefetch allocation with watermark check.
      // As an allocation hits the watermark, we will prefetch starting
      // at a "distance" away from watermark.

      Node *pf_region = new RegionNode(3);
      Node *pf_phi_rawmem = new PhiNode( pf_region, Type::MEMORY,
                                                TypeRawPtr::BOTTOM );
      // I/O is used for Prefetch
      Node *pf_phi_abio = new PhiNode( pf_region, Type::ABIO );

      Node *thread = new ThreadLocalNode();
      transform_later(thread);

      Node *eden_pf_adr = new AddPNode( top()/*not oop*/, thread,
                   _igvn.MakeConX(in_bytes(JavaThread::tlab_pf_top_offset())) );
      transform_later(eden_pf_adr);

      Node *old_pf_wm = new LoadPNode(needgc_false,
                                   contended_phi_rawmem, eden_pf_adr,
                                   TypeRawPtr::BOTTOM, TypeRawPtr::BOTTOM,
                                   MemNode::unordered);
      transform_later(old_pf_wm);

      // check against new_eden_top
      Node *need_pf_cmp = new CmpPNode( new_eden_top, old_pf_wm );
      transform_later(need_pf_cmp);
      Node *need_pf_bol = new BoolNode( need_pf_cmp, BoolTest::ge );
      transform_later(need_pf_bol);
      IfNode *need_pf_iff = new IfNode( needgc_false, need_pf_bol,
                                       PROB_UNLIKELY_MAG(4), COUNT_UNKNOWN );
      transform_later(need_pf_iff);

      // true node, add prefetchdistance
      Node *need_pf_true = new IfTrueNode( need_pf_iff );
      transform_later(need_pf_true);

      Node *need_pf_false = new IfFalseNode( need_pf_iff );
      transform_later(need_pf_false);

      Node *new_pf_wmt = new AddPNode( top(), old_pf_wm,
                                    _igvn.MakeConX(AllocatePrefetchDistance) );
      transform_later(new_pf_wmt );
      new_pf_wmt->set_req(0, need_pf_true);

      Node *store_new_wmt = new StorePNode(need_pf_true,
                                       contended_phi_rawmem, eden_pf_adr,
                                       TypeRawPtr::BOTTOM, new_pf_wmt,
                                       MemNode::unordered);
      transform_later(store_new_wmt);

      // adding prefetches
      pf_phi_abio->init_req( fall_in_path, i_o );

      Node *prefetch_adr;
      Node *prefetch;
      uint step_size = AllocatePrefetchStepSize;
      uint distance = 0;

      for ( intx i = 0; i < lines; i++ ) {
        prefetch_adr = new AddPNode( old_pf_wm, new_pf_wmt,
                                            _igvn.MakeConX(distance) );
        transform_later(prefetch_adr);
        prefetch = new PrefetchAllocationNode( i_o, prefetch_adr );
        transform_later(prefetch);
        distance += step_size;
        i_o = prefetch;
      }
      pf_phi_abio->set_req( pf_path, i_o );

      pf_region->init_req( fall_in_path, need_pf_false );
      pf_region->init_req( pf_path, need_pf_true );

      pf_phi_rawmem->init_req( fall_in_path, contended_phi_rawmem );
      pf_phi_rawmem->init_req( pf_path, store_new_wmt );

      transform_later(pf_region);
      transform_later(pf_phi_rawmem);
      transform_later(pf_phi_abio);

      needgc_false = pf_region;
      contended_phi_rawmem = pf_phi_rawmem;
      i_o = pf_phi_abio;
   } else if( UseTLAB && AllocatePrefetchStyle == 3 ) {
      // Insert a prefetch instruction for each allocation.
      // This code is used to generate 1 prefetch instruction per cache line.

      // Generate several prefetch instructions.
      uint step_size = AllocatePrefetchStepSize;
      uint distance = AllocatePrefetchDistance;

      // Next cache address.
      Node *cache_adr = new AddPNode(old_eden_top, old_eden_top,
                                     _igvn.MakeConX(step_size + distance));
      transform_later(cache_adr);
      cache_adr = new CastP2XNode(needgc_false, cache_adr);
      transform_later(cache_adr);
      // Address is aligned to execute prefetch to the beginning of cache line size
      // (it is important when BIS instruction is used on SPARC as prefetch).
      Node* mask = _igvn.MakeConX(~(intptr_t)(step_size-1));
      cache_adr = new AndXNode(cache_adr, mask);
      transform_later(cache_adr);
      cache_adr = new CastX2PNode(cache_adr);
      transform_later(cache_adr);

      // Prefetch
      Node *prefetch = new PrefetchAllocationNode( contended_phi_rawmem, cache_adr );
      prefetch->set_req(0, needgc_false);
      transform_later(prefetch);
      contended_phi_rawmem = prefetch;
      Node *prefetch_adr;
      distance = step_size;
      for ( intx i = 1; i < lines; i++ ) {
        prefetch_adr = new AddPNode( cache_adr, cache_adr,
                                            _igvn.MakeConX(distance) );
        transform_later(prefetch_adr);
        prefetch = new PrefetchAllocationNode( contended_phi_rawmem, prefetch_adr );
        transform_later(prefetch);
        distance += step_size;
        contended_phi_rawmem = prefetch;
      }
   } else if( AllocatePrefetchStyle > 0 ) {
      // Insert a prefetch for each allocation only on the fast-path
      Node *prefetch_adr;
      Node *prefetch;
      // Generate several prefetch instructions.
      uint step_size = AllocatePrefetchStepSize;
      uint distance = AllocatePrefetchDistance;
      for ( intx i = 0; i < lines; i++ ) {
        prefetch_adr = new AddPNode( old_eden_top, new_eden_top,
                                            _igvn.MakeConX(distance) );
        transform_later(prefetch_adr);
        prefetch = new PrefetchAllocationNode( i_o, prefetch_adr );
        // Do not let it float too high, since if eden_top == eden_end,
        // both might be null.
        if( i == 0 ) { // Set control for first prefetch, next follows it
          prefetch->init_req(0, needgc_false);
        }
        transform_later(prefetch);
        distance += step_size;
        i_o = prefetch;
      }
   }
   return i_o;
}


void PhaseMacroExpand::expand_allocate(AllocateNode *alloc) {
  expand_allocate_common(alloc, NULL,
                         OptoRuntime::new_instance_Type(),
                         OptoRuntime::new_instance_Java());
}

void PhaseMacroExpand::expand_allocate_array(AllocateArrayNode *alloc) {
  Node* length = alloc->in(AllocateNode::ALength);
  InitializeNode* init = alloc->initialization();
  Node* klass_node = alloc->in(AllocateNode::KlassNode);
  ciKlass* k = _igvn.type(klass_node)->is_klassptr()->klass();
  address slow_call_address;  // Address of slow call
  if (init != NULL && init->is_complete_with_arraycopy() &&
      k->is_type_array_klass()) {
    // Don't zero type array during slow allocation in VM since
    // it will be initialized later by arraycopy in compiled code.
    slow_call_address = OptoRuntime::new_array_nozero_Java();
  } else {
    slow_call_address = OptoRuntime::new_array_Java();
  }
  expand_allocate_common(alloc, length,
                         OptoRuntime::new_array_Type(),
                         slow_call_address);
}

//-------------------mark_eliminated_box----------------------------------
//
// During EA obj may point to several objects but after few ideal graph
// transformations (CCP) it may point to only one non escaping object
// (but still using phi), corresponding locks and unlocks will be marked
// for elimination. Later obj could be replaced with a new node (new phi)
// and which does not have escape information. And later after some graph
// reshape other locks and unlocks (which were not marked for elimination
// before) are connected to this new obj (phi) but they still will not be
// marked for elimination since new obj has no escape information.
// Mark all associated (same box and obj) lock and unlock nodes for
// elimination if some of them marked already.
void PhaseMacroExpand::mark_eliminated_box(Node* oldbox, Node* obj) {
  if (oldbox->as_BoxLock()->is_eliminated()) {
    return; // This BoxLock node was processed already.
  }
  // New implementation (EliminateNestedLocks) has separate BoxLock
  // node for each locked region so mark all associated locks/unlocks as
  // eliminated even if different objects are referenced in one locked region
  // (for example, OSR compilation of nested loop inside locked scope).
  if (EliminateNestedLocks ||
      oldbox->as_BoxLock()->is_simple_lock_region(NULL, obj, NULL)) {
    // Box is used only in one lock region. Mark this box as eliminated.
    _igvn.hash_delete(oldbox);
    oldbox->as_BoxLock()->set_eliminated(); // This changes box's hash value
     _igvn.hash_insert(oldbox);

    for (uint i = 0; i < oldbox->outcnt(); i++) {
      Node* u = oldbox->raw_out(i);
      if (u->is_AbstractLock() && !u->as_AbstractLock()->is_non_esc_obj()) {
        AbstractLockNode* alock = u->as_AbstractLock();
        // Check lock's box since box could be referenced by Lock's debug info.
        if (alock->box_node() == oldbox) {
          // Mark eliminated all related locks and unlocks.
#ifdef ASSERT
          alock->log_lock_optimization(C, "eliminate_lock_set_non_esc4");
#endif
          alock->set_non_esc_obj();
        }
      }
    }
    return;
  }

  // Create new "eliminated" BoxLock node and use it in monitor debug info
  // instead of oldbox for the same object.
  BoxLockNode* newbox = oldbox->clone()->as_BoxLock();

  // Note: BoxLock node is marked eliminated only here and it is used
  // to indicate that all associated lock and unlock nodes are marked
  // for elimination.
  newbox->set_eliminated();
  transform_later(newbox);

  // Replace old box node with new box for all users of the same object.
  for (uint i = 0; i < oldbox->outcnt();) {
    bool next_edge = true;

    Node* u = oldbox->raw_out(i);
    if (u->is_AbstractLock()) {
      AbstractLockNode* alock = u->as_AbstractLock();
      if (alock->box_node() == oldbox && alock->obj_node()->eqv_uncast(obj)) {
        // Replace Box and mark eliminated all related locks and unlocks.
#ifdef ASSERT
        alock->log_lock_optimization(C, "eliminate_lock_set_non_esc5");
#endif
        alock->set_non_esc_obj();
        _igvn.rehash_node_delayed(alock);
        alock->set_box_node(newbox);
        next_edge = false;
      }
    }
    if (u->is_FastLock() && u->as_FastLock()->obj_node()->eqv_uncast(obj)) {
      FastLockNode* flock = u->as_FastLock();
      assert(flock->box_node() == oldbox, "sanity");
      _igvn.rehash_node_delayed(flock);
      flock->set_box_node(newbox);
      next_edge = false;
    }

    // Replace old box in monitor debug info.
    if (u->is_SafePoint() && u->as_SafePoint()->jvms()) {
      SafePointNode* sfn = u->as_SafePoint();
      JVMState* youngest_jvms = sfn->jvms();
      int max_depth = youngest_jvms->depth();
      for (int depth = 1; depth <= max_depth; depth++) {
        JVMState* jvms = youngest_jvms->of_depth(depth);
        int num_mon  = jvms->nof_monitors();
        // Loop over monitors
        for (int idx = 0; idx < num_mon; idx++) {
          Node* obj_node = sfn->monitor_obj(jvms, idx);
          Node* box_node = sfn->monitor_box(jvms, idx);
          if (box_node == oldbox && obj_node->eqv_uncast(obj)) {
            int j = jvms->monitor_box_offset(idx);
            _igvn.replace_input_of(u, j, newbox);
            next_edge = false;
          }
        }
      }
    }
    if (next_edge) i++;
  }
}

//-----------------------mark_eliminated_locking_nodes-----------------------
void PhaseMacroExpand::mark_eliminated_locking_nodes(AbstractLockNode *alock) {
  if (EliminateNestedLocks) {
    if (alock->is_nested()) {
       assert(alock->box_node()->as_BoxLock()->is_eliminated(), "sanity");
       return;
    } else if (!alock->is_non_esc_obj()) { // Not eliminated or coarsened
      // Only Lock node has JVMState needed here.
      // Not that preceding claim is documented anywhere else.
      if (alock->jvms() != NULL) {
        if (alock->as_Lock()->is_nested_lock_region()) {
          // Mark eliminated related nested locks and unlocks.
          Node* obj = alock->obj_node();
          BoxLockNode* box_node = alock->box_node()->as_BoxLock();
          assert(!box_node->is_eliminated(), "should not be marked yet");
          // Note: BoxLock node is marked eliminated only here
          // and it is used to indicate that all associated lock
          // and unlock nodes are marked for elimination.
          box_node->set_eliminated(); // Box's hash is always NO_HASH here
          for (uint i = 0; i < box_node->outcnt(); i++) {
            Node* u = box_node->raw_out(i);
            if (u->is_AbstractLock()) {
              alock = u->as_AbstractLock();
              if (alock->box_node() == box_node) {
                // Verify that this Box is referenced only by related locks.
                assert(alock->obj_node()->eqv_uncast(obj), "");
                // Mark all related locks and unlocks.
#ifdef ASSERT
                alock->log_lock_optimization(C, "eliminate_lock_set_nested");
#endif
                alock->set_nested();
              }
            }
          }
        } else {
#ifdef ASSERT
          alock->log_lock_optimization(C, "eliminate_lock_NOT_nested_lock_region");
          if (C->log() != NULL)
            alock->as_Lock()->is_nested_lock_region(C); // rerun for debugging output
#endif
        }
      }
      return;
    }
    // Process locks for non escaping object
    assert(alock->is_non_esc_obj(), "");
  } // EliminateNestedLocks

  if (alock->is_non_esc_obj()) { // Lock is used for non escaping object
    // Look for all locks of this object and mark them and
    // corresponding BoxLock nodes as eliminated.
    Node* obj = alock->obj_node();
    for (uint j = 0; j < obj->outcnt(); j++) {
      Node* o = obj->raw_out(j);
      if (o->is_AbstractLock() &&
          o->as_AbstractLock()->obj_node()->eqv_uncast(obj)) {
        alock = o->as_AbstractLock();
        Node* box = alock->box_node();
        // Replace old box node with new eliminated box for all users
        // of the same object and mark related locks as eliminated.
        mark_eliminated_box(box, obj);
      }
    }
  }
}

// we have determined that this lock/unlock can be eliminated, we simply
// eliminate the node without expanding it.
//
// Note:  The membar's associated with the lock/unlock are currently not
//        eliminated.  This should be investigated as a future enhancement.
//
bool PhaseMacroExpand::eliminate_locking_node(AbstractLockNode *alock) {

  if (!alock->is_eliminated()) {
    return false;
  }
#ifdef ASSERT
  if (!alock->is_coarsened()) {
    // Check that new "eliminated" BoxLock node is created.
    BoxLockNode* oldbox = alock->box_node()->as_BoxLock();
    assert(oldbox->is_eliminated(), "should be done already");
  }
#endif

  alock->log_lock_optimization(C, "eliminate_lock");

#ifndef PRODUCT
  if (PrintEliminateLocks) {
    tty->print_cr("++++ Eliminated: %d %s '%s'", alock->_idx, (alock->is_Lock() ? "Lock" : "Unlock"), alock->kind_as_string());
  }
#endif

  Node* mem  = alock->in(TypeFunc::Memory);
  Node* ctrl = alock->in(TypeFunc::Control);
  guarantee(ctrl != NULL, "missing control projection, cannot replace_node() with NULL");

  alock->extract_projections(&_callprojs, false /*separate_io_proj*/, false /*do_asserts*/);
  // There are 2 projections from the lock.  The lock node will
  // be deleted when its last use is subsumed below.
  assert(alock->outcnt() == 2 &&
         _callprojs.fallthrough_proj != NULL &&
         _callprojs.fallthrough_memproj != NULL,
         "Unexpected projections from Lock/Unlock");

  Node* fallthroughproj = _callprojs.fallthrough_proj;
  Node* memproj_fallthrough = _callprojs.fallthrough_memproj;

  // The memory projection from a lock/unlock is RawMem
  // The input to a Lock is merged memory, so extract its RawMem input
  // (unless the MergeMem has been optimized away.)
  if (alock->is_Lock()) {
    // Seach for MemBarAcquireLock node and delete it also.
    MemBarNode* membar = fallthroughproj->unique_ctrl_out()->as_MemBar();
    assert(membar != NULL && membar->Opcode() == Op_MemBarAcquireLock, "");
    Node* ctrlproj = membar->proj_out(TypeFunc::Control);
    Node* memproj = membar->proj_out(TypeFunc::Memory);
    _igvn.replace_node(ctrlproj, fallthroughproj);
    _igvn.replace_node(memproj, memproj_fallthrough);

    // Delete FastLock node also if this Lock node is unique user
    // (a loop peeling may clone a Lock node).
    Node* flock = alock->as_Lock()->fastlock_node();
    if (flock->outcnt() == 1) {
      assert(flock->unique_out() == alock, "sanity");
      _igvn.replace_node(flock, top());
    }
  }

  // Seach for MemBarReleaseLock node and delete it also.
  if (alock->is_Unlock() && ctrl->is_Proj() && ctrl->in(0)->is_MemBar()) {
    MemBarNode* membar = ctrl->in(0)->as_MemBar();
    assert(membar->Opcode() == Op_MemBarReleaseLock &&
           mem->is_Proj() && membar == mem->in(0), "");
    _igvn.replace_node(fallthroughproj, ctrl);
    _igvn.replace_node(memproj_fallthrough, mem);
    fallthroughproj = ctrl;
    memproj_fallthrough = mem;
    ctrl = membar->in(TypeFunc::Control);
    mem  = membar->in(TypeFunc::Memory);
  }

  _igvn.replace_node(fallthroughproj, ctrl);
  _igvn.replace_node(memproj_fallthrough, mem);
  return true;
}


//------------------------------expand_lock_node----------------------
void PhaseMacroExpand::expand_lock_node(LockNode *lock) {

  Node* ctrl = lock->in(TypeFunc::Control);
  Node* mem = lock->in(TypeFunc::Memory);
  Node* obj = lock->obj_node();
  Node* box = lock->box_node();
  Node* flock = lock->fastlock_node();

  assert(!box->as_BoxLock()->is_eliminated(), "sanity");

  // Make the merge point
  Node *region;
  Node *mem_phi;
  Node *slow_path;

  region  = new RegionNode(3);
  // create a Phi for the memory state
  mem_phi = new PhiNode( region, Type::MEMORY, TypeRawPtr::BOTTOM);

  // Optimize test; set region slot 2
  slow_path = opt_bits_test(ctrl, region, 2, flock, 0, 0);
  mem_phi->init_req(2, mem);

  // Make slow path call
  CallNode *call = make_slow_call((CallNode *) lock, OptoRuntime::complete_monitor_enter_Type(),
                                  OptoRuntime::complete_monitor_locking_Java(), NULL, slow_path,
                                  obj, box, NULL);

  call->extract_projections(&_callprojs, false /*separate_io_proj*/, false /*do_asserts*/);

  // Slow path can only throw asynchronous exceptions, which are always
  // de-opted.  So the compiler thinks the slow-call can never throw an
  // exception.  If it DOES throw an exception we would need the debug
  // info removed first (since if it throws there is no monitor).
  assert(_callprojs.fallthrough_ioproj == NULL && _callprojs.catchall_ioproj == NULL &&
         _callprojs.catchall_memproj == NULL && _callprojs.catchall_catchproj == NULL, "Unexpected projection from Lock");

  // Capture slow path
  // disconnect fall-through projection from call and create a new one
  // hook up users of fall-through projection to region
  Node *slow_ctrl = _callprojs.fallthrough_proj->clone();
  transform_later(slow_ctrl);
  _igvn.hash_delete(_callprojs.fallthrough_proj);
  _callprojs.fallthrough_proj->disconnect_inputs(C);
  region->init_req(1, slow_ctrl);
  // region inputs are now complete
  transform_later(region);
  _igvn.replace_node(_callprojs.fallthrough_proj, region);

  Node *memproj = transform_later(new ProjNode(call, TypeFunc::Memory));
  mem_phi->init_req(1, memproj );
  transform_later(mem_phi);
  _igvn.replace_node(_callprojs.fallthrough_memproj, mem_phi);
}

//------------------------------expand_unlock_node----------------------
void PhaseMacroExpand::expand_unlock_node(UnlockNode *unlock) {

  Node* ctrl = unlock->in(TypeFunc::Control);
  Node* mem = unlock->in(TypeFunc::Memory);
  Node* obj = unlock->obj_node();
  Node* box = unlock->box_node();

  assert(!box->as_BoxLock()->is_eliminated(), "sanity");

  // No need for a null check on unlock

  // Make the merge point
  Node *region;
  Node *mem_phi;

  region  = new RegionNode(3);
  // create a Phi for the memory state
  mem_phi = new PhiNode( region, Type::MEMORY, TypeRawPtr::BOTTOM);

  FastUnlockNode *funlock = new FastUnlockNode( ctrl, obj, box );
  funlock = transform_later( funlock )->as_FastUnlock();
  // Optimize test; set region slot 2
  Node *slow_path = opt_bits_test(ctrl, region, 2, funlock, 0, 0);
  Node *thread = transform_later(new ThreadLocalNode());

  CallNode *call = make_slow_call((CallNode *) unlock, OptoRuntime::complete_monitor_exit_Type(),
                                  CAST_FROM_FN_PTR(address, SharedRuntime::complete_monitor_unlocking_C),
                                  "complete_monitor_unlocking_C", slow_path, obj, box, thread);

  call->extract_projections(&_callprojs, false /*separate_io_proj*/, false /*do_asserts*/);
  assert(_callprojs.fallthrough_ioproj == NULL && _callprojs.catchall_ioproj == NULL &&
         _callprojs.catchall_memproj == NULL && _callprojs.catchall_catchproj == NULL, "Unexpected projection from Lock");

  // No exceptions for unlocking
  // Capture slow path
  // disconnect fall-through projection from call and create a new one
  // hook up users of fall-through projection to region
  Node *slow_ctrl = _callprojs.fallthrough_proj->clone();
  transform_later(slow_ctrl);
  _igvn.hash_delete(_callprojs.fallthrough_proj);
  _callprojs.fallthrough_proj->disconnect_inputs(C);
  region->init_req(1, slow_ctrl);
  // region inputs are now complete
  transform_later(region);
  _igvn.replace_node(_callprojs.fallthrough_proj, region);

  Node *memproj = transform_later(new ProjNode(call, TypeFunc::Memory) );
  mem_phi->init_req(1, memproj );
  mem_phi->init_req(2, mem);
  transform_later(mem_phi);
  _igvn.replace_node(_callprojs.fallthrough_memproj, mem_phi);
}

void PhaseMacroExpand::expand_subtypecheck_node(SubTypeCheckNode *check) {
  assert(check->in(SubTypeCheckNode::Control) == NULL, "should be pinned");
  Node* bol = check->unique_out();
  Node* obj_or_subklass = check->in(SubTypeCheckNode::ObjOrSubKlass);
  Node* superklass = check->in(SubTypeCheckNode::SuperKlass);
  assert(bol->is_Bool() && bol->as_Bool()->_test._test == BoolTest::ne, "unexpected bool node");

  for (DUIterator_Last imin, i = bol->last_outs(imin); i >= imin; --i) {
    Node* iff = bol->last_out(i);
    assert(iff->is_If(), "where's the if?");

    if (iff->in(0)->is_top()) {
      _igvn.replace_input_of(iff, 1, C->top());
      continue;
    }

    Node* iftrue = iff->as_If()->proj_out(1);
    Node* iffalse = iff->as_If()->proj_out(0);
    Node* ctrl = iff->in(0);

    Node* subklass = NULL;
    if (_igvn.type(obj_or_subklass)->isa_klassptr()) {
      subklass = obj_or_subklass;
    } else {
      Node* k_adr = basic_plus_adr(obj_or_subklass, oopDesc::klass_offset_in_bytes());
      subklass = _igvn.transform(LoadKlassNode::make(_igvn, NULL, C->immutable_memory(), k_adr, TypeInstPtr::KLASS));
    }

    Node* not_subtype_ctrl = Phase::gen_subtype_check(subklass, superklass, &ctrl, NULL, _igvn);

    _igvn.replace_input_of(iff, 0, C->top());
    _igvn.replace_node(iftrue, not_subtype_ctrl);
    _igvn.replace_node(iffalse, ctrl);
  }
  _igvn.replace_node(check, C->top());
}

//---------------------------eliminate_macro_nodes----------------------
// Eliminate scalar replaced allocations and associated locks.
void PhaseMacroExpand::eliminate_macro_nodes() {
  if (C->macro_count() == 0)
    return;

  // Before elimination may re-mark (change to Nested or NonEscObj)
  // all associated (same box and obj) lock and unlock nodes.
  int cnt = C->macro_count();
  for (int i=0; i < cnt; i++) {
    Node *n = C->macro_node(i);
    if (n->is_AbstractLock()) { // Lock and Unlock nodes
      mark_eliminated_locking_nodes(n->as_AbstractLock());
    }
  }
  // Re-marking may break consistency of Coarsened locks.
  if (!C->coarsened_locks_consistent()) {
    return; // recompile without Coarsened locks if broken
  }

  // First, attempt to eliminate locks
  bool progress = true;
  while (progress) {
    progress = false;
    for (int i = C->macro_count(); i > 0; i = MIN2(i - 1, C->macro_count())) { // more than 1 element can be eliminated at once
      Node* n = C->macro_node(i - 1);
      bool success = false;
      DEBUG_ONLY(int old_macro_count = C->macro_count();)
      if (n->is_AbstractLock()) {
        success = eliminate_locking_node(n->as_AbstractLock());
      }
      assert(success == (C->macro_count() < old_macro_count), "elimination reduces macro count");
      progress = progress || success;
    }
  }
  // Next, attempt to eliminate allocations
  _has_locks = false;
  progress = true;
  while (progress) {
    progress = false;
    for (int i = C->macro_count(); i > 0; i = MIN2(i - 1, C->macro_count())) { // more than 1 element can be eliminated at once
      Node* n = C->macro_node(i - 1);
      bool success = false;
      DEBUG_ONLY(int old_macro_count = C->macro_count();)
      switch (n->class_id()) {
      case Node::Class_Allocate:
      case Node::Class_AllocateArray:
        success = eliminate_allocate_node(n->as_Allocate());
        break;
      case Node::Class_CallStaticJava:
        success = eliminate_boxing_node(n->as_CallStaticJava());
        break;
      case Node::Class_Lock:
      case Node::Class_Unlock:
        assert(!n->as_AbstractLock()->is_eliminated(), "sanity");
        _has_locks = true;
        break;
      case Node::Class_ArrayCopy:
        break;
      case Node::Class_OuterStripMinedLoop:
        break;
      case Node::Class_SubTypeCheck:
        break;
      case Node::Class_Opaque1:
        break;
      default:
        assert(n->Opcode() == Op_LoopLimit ||
               n->Opcode() == Op_Opaque2   ||
               n->Opcode() == Op_Opaque3   ||
               BarrierSet::barrier_set()->barrier_set_c2()->is_gc_barrier_node(n),
               "unknown node type in macro list");
      }
      assert(success == (C->macro_count() < old_macro_count), "elimination reduces macro count");
      progress = progress || success;
    }
  }
}

//------------------------------expand_macro_nodes----------------------
//  Returns true if a failure occurred.
bool PhaseMacroExpand::expand_macro_nodes() {
  // Last attempt to eliminate macro nodes.
  eliminate_macro_nodes();
  if (C->failing())  return true;

  // Eliminate Opaque and LoopLimit nodes. Do it after all loop optimizations.
  bool progress = true;
  while (progress) {
    progress = false;
    for (int i = C->macro_count(); i > 0; i--) {
      Node* n = C->macro_node(i-1);
      bool success = false;
      DEBUG_ONLY(int old_macro_count = C->macro_count();)
      if (n->Opcode() == Op_LoopLimit) {
        // Remove it from macro list and put on IGVN worklist to optimize.
        C->remove_macro_node(n);
        _igvn._worklist.push(n);
        success = true;
      } else if (n->Opcode() == Op_CallStaticJava) {
        // Remove it from macro list and put on IGVN worklist to optimize.
        C->remove_macro_node(n);
        _igvn._worklist.push(n);
        success = true;
      } else if (n->is_Opaque1() || n->Opcode() == Op_Opaque2) {
        _igvn.replace_node(n, n->in(1));
        success = true;
#if INCLUDE_RTM_OPT
      } else if ((n->Opcode() == Op_Opaque3) && ((Opaque3Node*)n)->rtm_opt()) {
        assert(C->profile_rtm(), "should be used only in rtm deoptimization code");
        assert((n->outcnt() == 1) && n->unique_out()->is_Cmp(), "");
        Node* cmp = n->unique_out();
#ifdef ASSERT
        // Validate graph.
        assert((cmp->outcnt() == 1) && cmp->unique_out()->is_Bool(), "");
        BoolNode* bol = cmp->unique_out()->as_Bool();
        assert((bol->outcnt() == 1) && bol->unique_out()->is_If() &&
               (bol->_test._test == BoolTest::ne), "");
        IfNode* ifn = bol->unique_out()->as_If();
        assert((ifn->outcnt() == 2) &&
               ifn->proj_out(1)->is_uncommon_trap_proj(Deoptimization::Reason_rtm_state_change) != NULL, "");
#endif
        Node* repl = n->in(1);
        if (!_has_locks) {
          // Remove RTM state check if there are no locks in the code.
          // Replace input to compare the same value.
          repl = (cmp->in(1) == n) ? cmp->in(2) : cmp->in(1);
        }
        _igvn.replace_node(n, repl);
        success = true;
#endif
      } else if (n->Opcode() == Op_OuterStripMinedLoop) {
        n->as_OuterStripMinedLoop()->adjust_strip_mined_loop(&_igvn);
        C->remove_macro_node(n);
        success = true;
      }
      assert(!success || (C->macro_count() == (old_macro_count - 1)), "elimination must have deleted one node from macro list");
      progress = progress || success;
    }
  }

  // Clean up the graph so we're less likely to hit the maximum node
  // limit
  _igvn.set_delay_transform(false);
  _igvn.optimize();
  if (C->failing())  return true;
  _igvn.set_delay_transform(true);


  // Because we run IGVN after each expansion, some macro nodes may go
  // dead and be removed from the list as we iterate over it. Move
  // Allocate nodes (processed in a second pass) at the beginning of
  // the list and then iterate from the last element of the list until
  // an Allocate node is seen. This is robust to random deletion in
  // the list due to nodes going dead.
  C->sort_macro_nodes();

  // expand arraycopy "macro" nodes first
  // For ReduceBulkZeroing, we must first process all arraycopy nodes
  // before the allocate nodes are expanded.
  while (C->macro_count() > 0) {
    int macro_count = C->macro_count();
    Node * n = C->macro_node(macro_count-1);
    assert(n->is_macro(), "only macro nodes expected here");
    if (_igvn.type(n) == Type::TOP || (n->in(0) != NULL && n->in(0)->is_top())) {
      // node is unreachable, so don't try to expand it
      C->remove_macro_node(n);
      continue;
    }
    if (n->is_Allocate()) {
      break;
    }
    // Make sure expansion will not cause node limit to be exceeded.
    // Worst case is a macro node gets expanded into about 200 nodes.
    // Allow 50% more for optimization.
    if (C->check_node_count(300, "out of nodes before macro expansion")) {
      return true;
    }

    DEBUG_ONLY(int old_macro_count = C->macro_count();)
    switch (n->class_id()) {
    case Node::Class_Lock:
      expand_lock_node(n->as_Lock());
      break;
    case Node::Class_Unlock:
      expand_unlock_node(n->as_Unlock());
      break;
    case Node::Class_ArrayCopy:
      expand_arraycopy_node(n->as_ArrayCopy());
      break;
    case Node::Class_SubTypeCheck:
      expand_subtypecheck_node(n->as_SubTypeCheck());
      break;
    default:
      assert(false, "unknown node type in macro list");
    }
    assert(C->macro_count() == (old_macro_count - 1), "expansion must have deleted one node from macro list");
    if (C->failing())  return true;

    // Clean up the graph so we're less likely to hit the maximum node
    // limit
    _igvn.set_delay_transform(false);
    _igvn.optimize();
    if (C->failing())  return true;
    _igvn.set_delay_transform(true);
  }

  // All nodes except Allocate nodes are expanded now. There could be
  // new optimization opportunities (such as folding newly created
  // load from a just allocated object). Run IGVN.

  // expand "macro" nodes
  // nodes are removed from the macro list as they are processed
  while (C->macro_count() > 0) {
    int macro_count = C->macro_count();
    Node * n = C->macro_node(macro_count-1);
    assert(n->is_macro(), "only macro nodes expected here");
    if (_igvn.type(n) == Type::TOP || (n->in(0) != NULL && n->in(0)->is_top())) {
      // node is unreachable, so don't try to expand it
      C->remove_macro_node(n);
      continue;
    }
    // Make sure expansion will not cause node limit to be exceeded.
    // Worst case is a macro node gets expanded into about 200 nodes.
    // Allow 50% more for optimization.
    if (C->check_node_count(300, "out of nodes before macro expansion")) {
      return true;
    }
    switch (n->class_id()) {
    case Node::Class_Allocate:
      expand_allocate(n->as_Allocate());
      break;
    case Node::Class_AllocateArray:
      expand_allocate_array(n->as_AllocateArray());
      break;
    default:
      assert(false, "unknown node type in macro list");
    }
    assert(C->macro_count() < macro_count, "must have deleted a node from macro list");
    if (C->failing())  return true;

    // Clean up the graph so we're less likely to hit the maximum node
    // limit
    _igvn.set_delay_transform(false);
    _igvn.optimize();
    if (C->failing())  return true;
    _igvn.set_delay_transform(true);
  }

  _igvn.set_delay_transform(false);
  return false;
}
