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
 *
 */

#include "precompiled.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "opto/arraycopynode.hpp"
#include "oops/objArrayKlass.hpp"
#include "opto/convertnode.hpp"
#include "opto/vectornode.hpp"
#include "opto/graphKit.hpp"
#include "opto/macro.hpp"
#include "opto/runtime.hpp"
#include "opto/castnode.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/align.hpp"
#include "utilities/powerOfTwo.hpp"

void PhaseMacroExpand::insert_mem_bar(Node** ctrl, Node** mem, int opcode, Node* precedent) {
  MemBarNode* mb = MemBarNode::make(C, opcode, Compile::AliasIdxBot, precedent);
  mb->init_req(TypeFunc::Control, *ctrl);
  mb->init_req(TypeFunc::Memory, *mem);
  transform_later(mb);
  *ctrl = new ProjNode(mb,TypeFunc::Control);
  transform_later(*ctrl);
  Node* mem_proj = new ProjNode(mb,TypeFunc::Memory);
  transform_later(mem_proj);
  *mem = mem_proj;
}

Node* PhaseMacroExpand::array_element_address(Node* ary, Node* idx, BasicType elembt) {
  uint shift  = exact_log2(type2aelembytes(elembt));
  uint header = arrayOopDesc::base_offset_in_bytes(elembt);
  Node* base =  basic_plus_adr(ary, header);
#ifdef _LP64
  // see comment in GraphKit::array_element_address
  int index_max = max_jint - 1;  // array size is max_jint, index is one less
  const TypeLong* lidxtype = TypeLong::make(CONST64(0), index_max, Type::WidenMax);
  idx = transform_later( new ConvI2LNode(idx, lidxtype) );
#endif
  Node* scale = new LShiftXNode(idx, intcon(shift));
  transform_later(scale);
  return basic_plus_adr(ary, base, scale);
}

Node* PhaseMacroExpand::ConvI2L(Node* offset) {
  return transform_later(new ConvI2LNode(offset));
}

Node* PhaseMacroExpand::make_leaf_call(Node* ctrl, Node* mem,
                                       const TypeFunc* call_type, address call_addr,
                                       const char* call_name,
                                       const TypePtr* adr_type,
                                       Node* parm0, Node* parm1,
                                       Node* parm2, Node* parm3,
                                       Node* parm4, Node* parm5,
                                       Node* parm6, Node* parm7) {
  Node* call = new CallLeafNoFPNode(call_type, call_addr, call_name, adr_type);
  call->init_req(TypeFunc::Control, ctrl);
  call->init_req(TypeFunc::I_O    , top());
  call->init_req(TypeFunc::Memory , mem);
  call->init_req(TypeFunc::ReturnAdr, top());
  call->init_req(TypeFunc::FramePtr, top());

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

  return call;
}


//------------------------------generate_guard---------------------------
// Helper function for generating guarded fast-slow graph structures.
// The given 'test', if true, guards a slow path.  If the test fails
// then a fast path can be taken.  (We generally hope it fails.)
// In all cases, GraphKit::control() is updated to the fast path.
// The returned value represents the control for the slow path.
// The return value is never 'top'; it is either a valid control
// or NULL if it is obvious that the slow path can never be taken.
// Also, if region and the slow control are not NULL, the slow edge
// is appended to the region.
Node* PhaseMacroExpand::generate_guard(Node** ctrl, Node* test, RegionNode* region, float true_prob) {
  if ((*ctrl)->is_top()) {
    // Already short circuited.
    return NULL;
  }
  // Build an if node and its projections.
  // If test is true we take the slow path, which we assume is uncommon.
  if (_igvn.type(test) == TypeInt::ZERO) {
    // The slow branch is never taken.  No need to build this guard.
    return NULL;
  }

  IfNode* iff = new IfNode(*ctrl, test, true_prob, COUNT_UNKNOWN);
  transform_later(iff);

  Node* if_slow = new IfTrueNode(iff);
  transform_later(if_slow);

  if (region != NULL) {
    region->add_req(if_slow);
  }

  Node* if_fast = new IfFalseNode(iff);
  transform_later(if_fast);

  *ctrl = if_fast;

  return if_slow;
}

inline Node* PhaseMacroExpand::generate_slow_guard(Node** ctrl, Node* test, RegionNode* region) {
  return generate_guard(ctrl, test, region, PROB_UNLIKELY_MAG(3));
}

void PhaseMacroExpand::generate_negative_guard(Node** ctrl, Node* index, RegionNode* region) {
  if ((*ctrl)->is_top())
    return;                // already stopped
  if (_igvn.type(index)->higher_equal(TypeInt::POS)) // [0,maxint]
    return;                // index is already adequately typed
  Node* cmp_lt = new CmpINode(index, intcon(0));
  transform_later(cmp_lt);
  Node* bol_lt = new BoolNode(cmp_lt, BoolTest::lt);
  transform_later(bol_lt);
  generate_guard(ctrl, bol_lt, region, PROB_MIN);
}

void PhaseMacroExpand::generate_limit_guard(Node** ctrl, Node* offset, Node* subseq_length, Node* array_length, RegionNode* region) {
  if ((*ctrl)->is_top())
    return;                // already stopped
  bool zero_offset = _igvn.type(offset) == TypeInt::ZERO;
  if (zero_offset && subseq_length->eqv_uncast(array_length))
    return;                // common case of whole-array copy
  Node* last = subseq_length;
  if (!zero_offset) {            // last += offset
    last = new AddINode(last, offset);
    transform_later(last);
  }
  Node* cmp_lt = new CmpUNode(array_length, last);
  transform_later(cmp_lt);
  Node* bol_lt = new BoolNode(cmp_lt, BoolTest::lt);
  transform_later(bol_lt);
  generate_guard(ctrl, bol_lt, region, PROB_MIN);
}

//
// Partial in-lining handling for smaller conjoint/disjoint array copies having
// length(in bytes) less than ArrayOperationPartialInlineSize.
//  if (length <= ArrayOperationPartialInlineSize) {
//    partial_inlining_block:
//      mask = Mask_Gen
//      vload = LoadVectorMasked src , mask
//      StoreVectorMasked dst, mask, vload
//  } else {
//    stub_block:
//      callstub array_copy
//  }
//  exit_block:
//    Phi = label partial_inlining_block:mem , label stub_block:mem (filled by caller)
//    mem = MergeMem (Phi)
//    control = stub_block
//
//  Exit_block and associated phi(memory) are partially initialized for partial_in-lining_block
//  edges. Remaining edges for exit_block coming from stub_block are connected by the caller
//  post stub nodes creation.
//

void PhaseMacroExpand::generate_partial_inlining_block(Node** ctrl, MergeMemNode** mem, const TypePtr* adr_type,
                                                       RegionNode** exit_block, Node** result_memory, Node* length,
                                                       Node* src_start, Node* dst_start, BasicType type) {
  const TypePtr *src_adr_type = _igvn.type(src_start)->isa_ptr();
  Node* inline_block = NULL;
  Node* stub_block = NULL;

  int const_len = -1;
  const TypeInt* lty = NULL;
  uint shift  = exact_log2(type2aelembytes(type));
  if (length->Opcode() == Op_ConvI2L) {
    lty = _igvn.type(length->in(1))->isa_int();
  } else  {
    lty = _igvn.type(length)->isa_int();
  }
  if (lty && lty->is_con()) {
    const_len = lty->get_con() << shift;
  }

  // Return if copy length is greater than partial inline size limit or
  // target does not supports masked load/stores.
  int lane_count = ArrayCopyNode::get_partial_inline_vector_lane_count(type, const_len);
  if ( const_len > ArrayOperationPartialInlineSize ||
      !Matcher::match_rule_supported_vector(Op_LoadVectorMasked, lane_count, type)  ||
      !Matcher::match_rule_supported_vector(Op_StoreVectorMasked, lane_count, type) ||
      !Matcher::match_rule_supported_vector(Op_VectorMaskGen, lane_count, type)) {
    return;
  }

  int inline_limit = ArrayOperationPartialInlineSize / type2aelembytes(type);
  Node* casted_length = new CastLLNode(*ctrl, length, TypeLong::make(0, inline_limit, Type::WidenMin));
  transform_later(casted_length);
  Node* copy_bytes = new LShiftXNode(length, intcon(shift));
  transform_later(copy_bytes);

  Node* cmp_le = new CmpULNode(copy_bytes, longcon(ArrayOperationPartialInlineSize));
  transform_later(cmp_le);
  Node* bol_le = new BoolNode(cmp_le, BoolTest::le);
  transform_later(bol_le);
  inline_block  = generate_guard(ctrl, bol_le, NULL, PROB_FAIR);
  stub_block = *ctrl;

  Node* mask_gen =  new VectorMaskGenNode(casted_length, TypeVect::VECTMASK, type);
  transform_later(mask_gen);

  unsigned vec_size = lane_count *  type2aelembytes(type);
  if (C->max_vector_size() < vec_size) {
    C->set_max_vector_size(vec_size);
  }

  const TypeVect * vt = TypeVect::make(type, lane_count);
  Node* mm = (*mem)->memory_at(C->get_alias_index(src_adr_type));
  Node* masked_load = new LoadVectorMaskedNode(inline_block, mm, src_start,
                                               src_adr_type, vt, mask_gen);
  transform_later(masked_load);

  mm = (*mem)->memory_at(C->get_alias_index(adr_type));
  Node* masked_store = new StoreVectorMaskedNode(inline_block, mm, dst_start,
                                                 masked_load, adr_type, mask_gen);
  transform_later(masked_store);

  // Convergence region for inline_block and stub_block.
  *exit_block = new RegionNode(3);
  transform_later(*exit_block);
  (*exit_block)->init_req(1, inline_block);
  *result_memory = new PhiNode(*exit_block, Type::MEMORY, adr_type);
  transform_later(*result_memory);
  (*result_memory)->init_req(1, masked_store);

  *ctrl = stub_block;
}


Node* PhaseMacroExpand::generate_nonpositive_guard(Node** ctrl, Node* index, bool never_negative) {
  if ((*ctrl)->is_top())  return NULL;

  if (_igvn.type(index)->higher_equal(TypeInt::POS1)) // [1,maxint]
    return NULL;                // index is already adequately typed
  Node* cmp_le = new CmpINode(index, intcon(0));
  transform_later(cmp_le);
  BoolTest::mask le_or_eq = (never_negative ? BoolTest::eq : BoolTest::le);
  Node* bol_le = new BoolNode(cmp_le, le_or_eq);
  transform_later(bol_le);
  Node* is_notp = generate_guard(ctrl, bol_le, NULL, PROB_MIN);

  return is_notp;
}

void PhaseMacroExpand::finish_arraycopy_call(Node* call, Node** ctrl, MergeMemNode** mem, const TypePtr* adr_type) {
  transform_later(call);

  *ctrl = new ProjNode(call,TypeFunc::Control);
  transform_later(*ctrl);
  Node* newmem = new ProjNode(call, TypeFunc::Memory);
  transform_later(newmem);

  uint alias_idx = C->get_alias_index(adr_type);
  if (alias_idx != Compile::AliasIdxBot) {
    *mem = MergeMemNode::make(*mem);
    (*mem)->set_memory_at(alias_idx, newmem);
  } else {
    *mem = MergeMemNode::make(newmem);
  }
  transform_later(*mem);
}

address PhaseMacroExpand::basictype2arraycopy(BasicType t,
                                              Node* src_offset,
                                              Node* dest_offset,
                                              bool disjoint_bases,
                                              const char* &name,
                                              bool dest_uninitialized) {
  const TypeInt* src_offset_inttype  = _igvn.find_int_type(src_offset);
  const TypeInt* dest_offset_inttype = _igvn.find_int_type(dest_offset);

  bool aligned = false;
  bool disjoint = disjoint_bases;

  // if the offsets are the same, we can treat the memory regions as
  // disjoint, because either the memory regions are in different arrays,
  // or they are identical (which we can treat as disjoint.)  We can also
  // treat a copy with a destination index  less that the source index
  // as disjoint since a low->high copy will work correctly in this case.
  if (src_offset_inttype != NULL && src_offset_inttype->is_con() &&
      dest_offset_inttype != NULL && dest_offset_inttype->is_con()) {
    // both indices are constants
    int s_offs = src_offset_inttype->get_con();
    int d_offs = dest_offset_inttype->get_con();
    int element_size = type2aelembytes(t);
    aligned = ((arrayOopDesc::base_offset_in_bytes(t) + s_offs * element_size) % HeapWordSize == 0) &&
              ((arrayOopDesc::base_offset_in_bytes(t) + d_offs * element_size) % HeapWordSize == 0);
    if (s_offs >= d_offs)  disjoint = true;
  } else if (src_offset == dest_offset && src_offset != NULL) {
    // This can occur if the offsets are identical non-constants.
    disjoint = true;
  }

  return StubRoutines::select_arraycopy_function(t, aligned, disjoint, name, dest_uninitialized);
}

#define XTOP LP64_ONLY(COMMA top())

// Generate an optimized call to arraycopy.
// Caller must guard against non-arrays.
// Caller must determine a common array basic-type for both arrays.
// Caller must validate offsets against array bounds.
// The slow_region has already collected guard failure paths
// (such as out of bounds length or non-conformable array types).
// The generated code has this shape, in general:
//
//     if (length == 0)  return   // via zero_path
//     slowval = -1
//     if (types unknown) {
//       slowval = call generic copy loop
//       if (slowval == 0)  return  // via checked_path
//     } else if (indexes in bounds) {
//       if ((is object array) && !(array type check)) {
//         slowval = call checked copy loop
//         if (slowval == 0)  return  // via checked_path
//       } else {
//         call bulk copy loop
//         return  // via fast_path
//       }
//     }
//     // adjust params for remaining work:
//     if (slowval != -1) {
//       n = -1^slowval; src_offset += n; dest_offset += n; length -= n
//     }
//   slow_region:
//     call slow arraycopy(src, src_offset, dest, dest_offset, length)
//     return  // via slow_call_path
//
// This routine is used from several intrinsics:  System.arraycopy,
// Object.clone (the array subcase), and Arrays.copyOf[Range].
//
Node* PhaseMacroExpand::generate_arraycopy(ArrayCopyNode *ac, AllocateArrayNode* alloc,
                                           Node** ctrl, MergeMemNode* mem, Node** io,
                                           const TypePtr* adr_type,
                                           BasicType basic_elem_type,
                                           Node* src,  Node* src_offset,
                                           Node* dest, Node* dest_offset,
                                           Node* copy_length,
                                           bool disjoint_bases,
                                           bool length_never_negative,
                                           RegionNode* slow_region) {
  if (slow_region == NULL) {
    slow_region = new RegionNode(1);
    transform_later(slow_region);
  }

  Node* original_dest = dest;
  bool  dest_needs_zeroing   = false;
  bool  acopy_to_uninitialized = false;

  // See if this is the initialization of a newly-allocated array.
  // If so, we will take responsibility here for initializing it to zero.
  // (Note:  Because tightly_coupled_allocation performs checks on the
  // out-edges of the dest, we need to avoid making derived pointers
  // from it until we have checked its uses.)
  if (ReduceBulkZeroing
      && !(UseTLAB && ZeroTLAB) // pointless if already zeroed
      && basic_elem_type != T_CONFLICT // avoid corner case
      && !src->eqv_uncast(dest)
      && alloc != NULL
      && _igvn.find_int_con(alloc->in(AllocateNode::ALength), 1) > 0) {
    assert(ac->is_alloc_tightly_coupled(), "sanity");
    // acopy to uninitialized tightly coupled allocations
    // needs zeroing outside the copy range
    // and the acopy itself will be to uninitialized memory
    acopy_to_uninitialized = true;
    if (alloc->maybe_set_complete(&_igvn)) {
      // "You break it, you buy it."
      InitializeNode* init = alloc->initialization();
      assert(init->is_complete(), "we just did this");
      init->set_complete_with_arraycopy();
      assert(dest->is_CheckCastPP(), "sanity");
      assert(dest->in(0)->in(0) == init, "dest pinned");
      adr_type = TypeRawPtr::BOTTOM;  // all initializations are into raw memory
      // From this point on, every exit path is responsible for
      // initializing any non-copied parts of the object to zero.
      // Also, if this flag is set we make sure that arraycopy interacts properly
      // with G1, eliding pre-barriers. See CR 6627983.
      dest_needs_zeroing = true;
    } else {
      // dest_need_zeroing = false;
    }
  } else {
    // No zeroing elimination needed here.
    alloc                  = NULL;
    acopy_to_uninitialized = false;
    //original_dest        = dest;
    //dest_needs_zeroing   = false;
  }

  uint alias_idx = C->get_alias_index(adr_type);

  // Results are placed here:
  enum { fast_path        = 1,  // normal void-returning assembly stub
         checked_path     = 2,  // special assembly stub with cleanup
         slow_call_path   = 3,  // something went wrong; call the VM
         zero_path        = 4,  // bypass when length of copy is zero
         bcopy_path       = 5,  // copy primitive array by 64-bit blocks
         PATH_LIMIT       = 6
  };
  RegionNode* result_region = new RegionNode(PATH_LIMIT);
  PhiNode*    result_i_o    = new PhiNode(result_region, Type::ABIO);
  PhiNode*    result_memory = new PhiNode(result_region, Type::MEMORY, adr_type);
  assert(adr_type != TypePtr::BOTTOM, "must be RawMem or a T[] slice");
  transform_later(result_region);
  transform_later(result_i_o);
  transform_later(result_memory);

  // The slow_control path:
  Node* slow_control;
  Node* slow_i_o = *io;
  Node* slow_mem = mem->memory_at(alias_idx);
  DEBUG_ONLY(slow_control = (Node*) badAddress);

  // Checked control path:
  Node* checked_control = top();
  Node* checked_mem     = NULL;
  Node* checked_i_o     = NULL;
  Node* checked_value   = NULL;

  if (basic_elem_type == T_CONFLICT) {
    assert(!dest_needs_zeroing, "");
    Node* cv = generate_generic_arraycopy(ctrl, &mem,
                                          adr_type,
                                          src, src_offset, dest, dest_offset,
                                          copy_length, acopy_to_uninitialized);
    if (cv == NULL)  cv = intcon(-1);  // failure (no stub available)
    checked_control = *ctrl;
    checked_i_o     = *io;
    checked_mem     = mem->memory_at(alias_idx);
    checked_value   = cv;
    *ctrl = top();
  }

  Node* not_pos = generate_nonpositive_guard(ctrl, copy_length, length_never_negative);
  if (not_pos != NULL) {
    Node* local_ctrl = not_pos, *local_io = *io;
    MergeMemNode* local_mem = MergeMemNode::make(mem);
    transform_later(local_mem);

    // (6) length must not be negative.
    if (!length_never_negative) {
      generate_negative_guard(&local_ctrl, copy_length, slow_region);
    }

    // copy_length is 0.
    if (dest_needs_zeroing) {
      assert(!local_ctrl->is_top(), "no ctrl?");
      Node* dest_length = alloc->in(AllocateNode::ALength);
      if (copy_length->eqv_uncast(dest_length)
          || _igvn.find_int_con(dest_length, 1) <= 0) {
        // There is no zeroing to do. No need for a secondary raw memory barrier.
      } else {
        // Clear the whole thing since there are no source elements to copy.
        generate_clear_array(local_ctrl, local_mem,
                             adr_type, dest, basic_elem_type,
                             intcon(0), NULL,
                             alloc->in(AllocateNode::AllocSize));
        // Use a secondary InitializeNode as raw memory barrier.
        // Currently it is needed only on this path since other
        // paths have stub or runtime calls as raw memory barriers.
        MemBarNode* mb = MemBarNode::make(C, Op_Initialize,
                                          Compile::AliasIdxRaw,
                                          top());
        transform_later(mb);
        mb->set_req(TypeFunc::Control,local_ctrl);
        mb->set_req(TypeFunc::Memory, local_mem->memory_at(Compile::AliasIdxRaw));
        local_ctrl = transform_later(new ProjNode(mb, TypeFunc::Control));
        local_mem->set_memory_at(Compile::AliasIdxRaw, transform_later(new ProjNode(mb, TypeFunc::Memory)));

        InitializeNode* init = mb->as_Initialize();
        init->set_complete(&_igvn);  // (there is no corresponding AllocateNode)
      }
    }

    // Present the results of the fast call.
    result_region->init_req(zero_path, local_ctrl);
    result_i_o   ->init_req(zero_path, local_io);
    result_memory->init_req(zero_path, local_mem->memory_at(alias_idx));
  }

  if (!(*ctrl)->is_top() && dest_needs_zeroing) {
    // We have to initialize the *uncopied* part of the array to zero.
    // The copy destination is the slice dest[off..off+len].  The other slices
    // are dest_head = dest[0..off] and dest_tail = dest[off+len..dest.length].
    Node* dest_size   = alloc->in(AllocateNode::AllocSize);
    Node* dest_length = alloc->in(AllocateNode::ALength);
    Node* dest_tail   = transform_later( new AddINode(dest_offset, copy_length));

    // If there is a head section that needs zeroing, do it now.
    if (_igvn.find_int_con(dest_offset, -1) != 0) {
      generate_clear_array(*ctrl, mem,
                           adr_type, dest, basic_elem_type,
                           intcon(0), dest_offset,
                           NULL);
    }

    // Next, perform a dynamic check on the tail length.
    // It is often zero, and we can win big if we prove this.
    // There are two wins:  Avoid generating the ClearArray
    // with its attendant messy index arithmetic, and upgrade
    // the copy to a more hardware-friendly word size of 64 bits.
    Node* tail_ctl = NULL;
    if (!(*ctrl)->is_top() && !dest_tail->eqv_uncast(dest_length)) {
      Node* cmp_lt   = transform_later( new CmpINode(dest_tail, dest_length) );
      Node* bol_lt   = transform_later( new BoolNode(cmp_lt, BoolTest::lt) );
      tail_ctl = generate_slow_guard(ctrl, bol_lt, NULL);
      assert(tail_ctl != NULL || !(*ctrl)->is_top(), "must be an outcome");
    }

    // At this point, let's assume there is no tail.
    if (!(*ctrl)->is_top() && alloc != NULL && basic_elem_type != T_OBJECT) {
      // There is no tail.  Try an upgrade to a 64-bit copy.
      bool didit = false;
      {
        Node* local_ctrl = *ctrl, *local_io = *io;
        MergeMemNode* local_mem = MergeMemNode::make(mem);
        transform_later(local_mem);

        didit = generate_block_arraycopy(&local_ctrl, &local_mem, local_io,
                                         adr_type, basic_elem_type, alloc,
                                         src, src_offset, dest, dest_offset,
                                         dest_size, acopy_to_uninitialized);
        if (didit) {
          // Present the results of the block-copying fast call.
          result_region->init_req(bcopy_path, local_ctrl);
          result_i_o   ->init_req(bcopy_path, local_io);
          result_memory->init_req(bcopy_path, local_mem->memory_at(alias_idx));
        }
      }
      if (didit) {
        *ctrl = top();     // no regular fast path
      }
    }

    // Clear the tail, if any.
    if (tail_ctl != NULL) {
      Node* notail_ctl = (*ctrl)->is_top() ? NULL : *ctrl;
      *ctrl = tail_ctl;
      if (notail_ctl == NULL) {
        generate_clear_array(*ctrl, mem,
                             adr_type, dest, basic_elem_type,
                             dest_tail, NULL,
                             dest_size);
      } else {
        // Make a local merge.
        Node* done_ctl = transform_later(new RegionNode(3));
        Node* done_mem = transform_later(new PhiNode(done_ctl, Type::MEMORY, adr_type));
        done_ctl->init_req(1, notail_ctl);
        done_mem->init_req(1, mem->memory_at(alias_idx));
        generate_clear_array(*ctrl, mem,
                             adr_type, dest, basic_elem_type,
                             dest_tail, NULL,
                             dest_size);
        done_ctl->init_req(2, *ctrl);
        done_mem->init_req(2, mem->memory_at(alias_idx));
        *ctrl = done_ctl;
        mem->set_memory_at(alias_idx, done_mem);
      }
    }
  }

  BasicType copy_type = basic_elem_type;
  assert(basic_elem_type != T_ARRAY, "caller must fix this");
  if (!(*ctrl)->is_top() && copy_type == T_OBJECT) {
    // If src and dest have compatible element types, we can copy bits.
    // Types S[] and D[] are compatible if D is a supertype of S.
    //
    // If they are not, we will use checked_oop_disjoint_arraycopy,
    // which performs a fast optimistic per-oop check, and backs off
    // further to JVM_ArrayCopy on the first per-oop check that fails.
    // (Actually, we don't move raw bits only; the GC requires card marks.)

    // We don't need a subtype check for validated copies and Object[].clone()
    bool skip_subtype_check = ac->is_arraycopy_validated() || ac->is_copyof_validated() ||
                              ac->is_copyofrange_validated() || ac->is_clone_oop_array();
    if (!skip_subtype_check) {
      // Get the klass* for both src and dest
      Node* src_klass  = ac->in(ArrayCopyNode::SrcKlass);
      Node* dest_klass = ac->in(ArrayCopyNode::DestKlass);

      assert(src_klass != NULL && dest_klass != NULL, "should have klasses");

      // Generate the subtype check.
      // This might fold up statically, or then again it might not.
      //
      // Non-static example:  Copying List<String>.elements to a new String[].
      // The backing store for a List<String> is always an Object[],
      // but its elements are always type String, if the generic types
      // are correct at the source level.
      //
      // Test S[] against D[], not S against D, because (probably)
      // the secondary supertype cache is less busy for S[] than S.
      // This usually only matters when D is an interface.
      Node* not_subtype_ctrl = Phase::gen_subtype_check(src_klass, dest_klass, ctrl, mem, _igvn);
      // Plug failing path into checked_oop_disjoint_arraycopy
      if (not_subtype_ctrl != top()) {
        Node* local_ctrl = not_subtype_ctrl;
        MergeMemNode* local_mem = MergeMemNode::make(mem);
        transform_later(local_mem);

        // (At this point we can assume disjoint_bases, since types differ.)
        int ek_offset = in_bytes(ObjArrayKlass::element_klass_offset());
        Node* p1 = basic_plus_adr(dest_klass, ek_offset);
        Node* n1 = LoadKlassNode::make(_igvn, NULL, C->immutable_memory(), p1, TypeRawPtr::BOTTOM);
        Node* dest_elem_klass = transform_later(n1);
        Node* cv = generate_checkcast_arraycopy(&local_ctrl, &local_mem,
                                                adr_type,
                                                dest_elem_klass,
                                                src, src_offset, dest, dest_offset,
                                                ConvI2X(copy_length), acopy_to_uninitialized);
        if (cv == NULL)  cv = intcon(-1);  // failure (no stub available)
        checked_control = local_ctrl;
        checked_i_o     = *io;
        checked_mem     = local_mem->memory_at(alias_idx);
        checked_value   = cv;
      }
    }
    // At this point we know we do not need type checks on oop stores.

    BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
    if (!bs->array_copy_requires_gc_barriers(alloc != NULL, copy_type, false, false, BarrierSetC2::Expansion)) {
      // If we do not need gc barriers, copy using the jint or jlong stub.
      copy_type = LP64_ONLY(UseCompressedOops ? T_INT : T_LONG) NOT_LP64(T_INT);
      assert(type2aelembytes(basic_elem_type) == type2aelembytes(copy_type),
             "sizes agree");
    }
  }

  bool is_partial_array_copy = false;
  if (!(*ctrl)->is_top()) {
    // Generate the fast path, if possible.
    Node* local_ctrl = *ctrl;
    MergeMemNode* local_mem = MergeMemNode::make(mem);
    transform_later(local_mem);
    is_partial_array_copy = generate_unchecked_arraycopy(&local_ctrl, &local_mem,
                                                         adr_type, copy_type, disjoint_bases,
                                                         src, src_offset, dest, dest_offset,
                                                         ConvI2X(copy_length), acopy_to_uninitialized);

    // Present the results of the fast call.
    result_region->init_req(fast_path, local_ctrl);
    result_i_o   ->init_req(fast_path, *io);
    result_memory->init_req(fast_path, local_mem->memory_at(alias_idx));
  }

  // Here are all the slow paths up to this point, in one bundle:
  assert(slow_region != NULL, "allocated on entry");
  slow_control = slow_region;
  DEBUG_ONLY(slow_region = (RegionNode*)badAddress);

  *ctrl = checked_control;
  if (!(*ctrl)->is_top()) {
    // Clean up after the checked call.
    // The returned value is either 0 or -1^K,
    // where K = number of partially transferred array elements.
    Node* cmp = new CmpINode(checked_value, intcon(0));
    transform_later(cmp);
    Node* bol = new BoolNode(cmp, BoolTest::eq);
    transform_later(bol);
    IfNode* iff = new IfNode(*ctrl, bol, PROB_MAX, COUNT_UNKNOWN);
    transform_later(iff);

    // If it is 0, we are done, so transfer to the end.
    Node* checks_done = new IfTrueNode(iff);
    transform_later(checks_done);
    result_region->init_req(checked_path, checks_done);
    result_i_o   ->init_req(checked_path, checked_i_o);
    result_memory->init_req(checked_path, checked_mem);

    // If it is not zero, merge into the slow call.
    *ctrl = new IfFalseNode(iff);
    transform_later(*ctrl);
    RegionNode* slow_reg2 = new RegionNode(3);
    PhiNode*    slow_i_o2 = new PhiNode(slow_reg2, Type::ABIO);
    PhiNode*    slow_mem2 = new PhiNode(slow_reg2, Type::MEMORY, adr_type);
    transform_later(slow_reg2);
    transform_later(slow_i_o2);
    transform_later(slow_mem2);
    slow_reg2  ->init_req(1, slow_control);
    slow_i_o2  ->init_req(1, slow_i_o);
    slow_mem2  ->init_req(1, slow_mem);
    slow_reg2  ->init_req(2, *ctrl);
    slow_i_o2  ->init_req(2, checked_i_o);
    slow_mem2  ->init_req(2, checked_mem);

    slow_control = slow_reg2;
    slow_i_o     = slow_i_o2;
    slow_mem     = slow_mem2;

    if (alloc != NULL) {
      // We'll restart from the very beginning, after zeroing the whole thing.
      // This can cause double writes, but that's OK since dest is brand new.
      // So we ignore the low 31 bits of the value returned from the stub.
    } else {
      // We must continue the copy exactly where it failed, or else
      // another thread might see the wrong number of writes to dest.
      Node* checked_offset = new XorINode(checked_value, intcon(-1));
      Node* slow_offset    = new PhiNode(slow_reg2, TypeInt::INT);
      transform_later(checked_offset);
      transform_later(slow_offset);
      slow_offset->init_req(1, intcon(0));
      slow_offset->init_req(2, checked_offset);

      // Adjust the arguments by the conditionally incoming offset.
      Node* src_off_plus  = new AddINode(src_offset,  slow_offset);
      transform_later(src_off_plus);
      Node* dest_off_plus = new AddINode(dest_offset, slow_offset);
      transform_later(dest_off_plus);
      Node* length_minus  = new SubINode(copy_length, slow_offset);
      transform_later(length_minus);

      // Tweak the node variables to adjust the code produced below:
      src_offset  = src_off_plus;
      dest_offset = dest_off_plus;
      copy_length = length_minus;
    }
  }
  *ctrl = slow_control;
  if (!(*ctrl)->is_top()) {
    Node* local_ctrl = *ctrl, *local_io = slow_i_o;
    MergeMemNode* local_mem = MergeMemNode::make(mem);
    transform_later(local_mem);

    // Generate the slow path, if needed.
    local_mem->set_memory_at(alias_idx, slow_mem);

    if (dest_needs_zeroing) {
      generate_clear_array(local_ctrl, local_mem,
                           adr_type, dest, basic_elem_type,
                           intcon(0), NULL,
                           alloc->in(AllocateNode::AllocSize));
    }

    local_mem = generate_slow_arraycopy(ac,
                                        &local_ctrl, local_mem, &local_io,
                                        adr_type,
                                        src, src_offset, dest, dest_offset,
                                        copy_length, /*dest_uninitialized*/false);

    result_region->init_req(slow_call_path, local_ctrl);
    result_i_o   ->init_req(slow_call_path, local_io);
    result_memory->init_req(slow_call_path, local_mem->memory_at(alias_idx));
  } else {
    ShouldNotReachHere(); // no call to generate_slow_arraycopy:
                          // projections were not extracted
  }

  // Remove unused edges.
  for (uint i = 1; i < result_region->req(); i++) {
    if (result_region->in(i) == NULL) {
      result_region->init_req(i, top());
    }
  }

  // Finished; return the combined state.
  *ctrl = result_region;
  *io = result_i_o;
  mem->set_memory_at(alias_idx, result_memory);

  // mem no longer guaranteed to stay a MergeMemNode
  Node* out_mem = mem;
  DEBUG_ONLY(mem = NULL);

  // The memory edges above are precise in order to model effects around
  // array copies accurately to allow value numbering of field loads around
  // arraycopy.  Such field loads, both before and after, are common in Java
  // collections and similar classes involving header/array data structures.
  //
  // But with low number of register or when some registers are used or killed
  // by arraycopy calls it causes registers spilling on stack. See 6544710.
  // The next memory barrier is added to avoid it. If the arraycopy can be
  // optimized away (which it can, sometimes) then we can manually remove
  // the membar also.
  //
  // Do not let reads from the cloned object float above the arraycopy.
  if (alloc != NULL && !alloc->initialization()->does_not_escape()) {
    // Do not let stores that initialize this object be reordered with
    // a subsequent store that would make this object accessible by
    // other threads.
    insert_mem_bar(ctrl, &out_mem, Op_MemBarStoreStore);
  } else {
    insert_mem_bar(ctrl, &out_mem, Op_MemBarCPUOrder);
  }

  if (is_partial_array_copy) {
    assert((*ctrl)->is_Proj(), "MemBar control projection");
    assert((*ctrl)->in(0)->isa_MemBar(), "MemBar node");
    (*ctrl)->in(0)->isa_MemBar()->set_trailing_partial_array_copy();
  }

  _igvn.replace_node(_callprojs.fallthrough_memproj, out_mem);
  _igvn.replace_node(_callprojs.fallthrough_ioproj, *io);
  _igvn.replace_node(_callprojs.fallthrough_catchproj, *ctrl);

#ifdef ASSERT
  const TypeOopPtr* dest_t = _igvn.type(dest)->is_oopptr();
  if (dest_t->is_known_instance() && !is_partial_array_copy) {
    ArrayCopyNode* ac = NULL;
    assert(ArrayCopyNode::may_modify(dest_t, (*ctrl)->in(0)->as_MemBar(), &_igvn, ac), "dependency on arraycopy lost");
    assert(ac == NULL, "no arraycopy anymore");
  }
#endif

  return out_mem;
}

// Helper for initialization of arrays, creating a ClearArray.
// It writes zero bits in [start..end), within the body of an array object.
// The memory effects are all chained onto the 'adr_type' alias category.
//
// Since the object is otherwise uninitialized, we are free
// to put a little "slop" around the edges of the cleared area,
// as long as it does not go back into the array's header,
// or beyond the array end within the heap.
//
// The lower edge can be rounded down to the nearest jint and the
// upper edge can be rounded up to the nearest MinObjAlignmentInBytes.
//
// Arguments:
//   adr_type           memory slice where writes are generated
//   dest               oop of the destination array
//   basic_elem_type    element type of the destination
//   slice_idx          array index of first element to store
//   slice_len          number of elements to store (or NULL)
//   dest_size          total size in bytes of the array object
//
// Exactly one of slice_len or dest_size must be non-NULL.
// If dest_size is non-NULL, zeroing extends to the end of the object.
// If slice_len is non-NULL, the slice_idx value must be a constant.
void PhaseMacroExpand::generate_clear_array(Node* ctrl, MergeMemNode* merge_mem,
                                            const TypePtr* adr_type,
                                            Node* dest,
                                            BasicType basic_elem_type,
                                            Node* slice_idx,
                                            Node* slice_len,
                                            Node* dest_size) {
  // one or the other but not both of slice_len and dest_size:
  assert((slice_len != NULL? 1: 0) + (dest_size != NULL? 1: 0) == 1, "");
  if (slice_len == NULL)  slice_len = top();
  if (dest_size == NULL)  dest_size = top();

  uint alias_idx = C->get_alias_index(adr_type);

  // operate on this memory slice:
  Node* mem = merge_mem->memory_at(alias_idx); // memory slice to operate on

  // scaling and rounding of indexes:
  int scale = exact_log2(type2aelembytes(basic_elem_type));
  int abase = arrayOopDesc::base_offset_in_bytes(basic_elem_type);
  int clear_low = (-1 << scale) & (BytesPerInt  - 1);
  int bump_bit  = (-1 << scale) & BytesPerInt;

  // determine constant starts and ends
  const intptr_t BIG_NEG = -128;
  assert(BIG_NEG + 2*abase < 0, "neg enough");
  intptr_t slice_idx_con = (intptr_t) _igvn.find_int_con(slice_idx, BIG_NEG);
  intptr_t slice_len_con = (intptr_t) _igvn.find_int_con(slice_len, BIG_NEG);
  if (slice_len_con == 0) {
    return;                     // nothing to do here
  }
  intptr_t start_con = (abase + (slice_idx_con << scale)) & ~clear_low;
  intptr_t end_con   = _igvn.find_intptr_t_con(dest_size, -1);
  if (slice_idx_con >= 0 && slice_len_con >= 0) {
    assert(end_con < 0, "not two cons");
    end_con = align_up(abase + ((slice_idx_con + slice_len_con) << scale),
                       BytesPerLong);
  }

  if (start_con >= 0 && end_con >= 0) {
    // Constant start and end.  Simple.
    mem = ClearArrayNode::clear_memory(ctrl, mem, dest,
                                       start_con, end_con, &_igvn);
  } else if (start_con >= 0 && dest_size != top()) {
    // Constant start, pre-rounded end after the tail of the array.
    Node* end = dest_size;
    mem = ClearArrayNode::clear_memory(ctrl, mem, dest,
                                       start_con, end, &_igvn);
  } else if (start_con >= 0 && slice_len != top()) {
    // Constant start, non-constant end.  End needs rounding up.
    // End offset = round_up(abase + ((slice_idx_con + slice_len) << scale), 8)
    intptr_t end_base  = abase + (slice_idx_con << scale);
    int      end_round = (-1 << scale) & (BytesPerLong  - 1);
    Node*    end       = ConvI2X(slice_len);
    if (scale != 0)
      end = transform_later(new LShiftXNode(end, intcon(scale) ));
    end_base += end_round;
    end = transform_later(new AddXNode(end, MakeConX(end_base)) );
    end = transform_later(new AndXNode(end, MakeConX(~end_round)) );
    mem = ClearArrayNode::clear_memory(ctrl, mem, dest,
                                       start_con, end, &_igvn);
  } else if (start_con < 0 && dest_size != top()) {
    // Non-constant start, pre-rounded end after the tail of the array.
    // This is almost certainly a "round-to-end" operation.
    Node* start = slice_idx;
    start = ConvI2X(start);
    if (scale != 0)
      start = transform_later(new LShiftXNode( start, intcon(scale) ));
    start = transform_later(new AddXNode(start, MakeConX(abase)) );
    if ((bump_bit | clear_low) != 0) {
      int to_clear = (bump_bit | clear_low);
      // Align up mod 8, then store a jint zero unconditionally
      // just before the mod-8 boundary.
      if (((abase + bump_bit) & ~to_clear) - bump_bit
          < arrayOopDesc::length_offset_in_bytes() + BytesPerInt) {
        bump_bit = 0;
        assert((abase & to_clear) == 0, "array base must be long-aligned");
      } else {
        // Bump 'start' up to (or past) the next jint boundary:
        start = transform_later( new AddXNode(start, MakeConX(bump_bit)) );
        assert((abase & clear_low) == 0, "array base must be int-aligned");
      }
      // Round bumped 'start' down to jlong boundary in body of array.
      start = transform_later(new AndXNode(start, MakeConX(~to_clear)) );
      if (bump_bit != 0) {
        // Store a zero to the immediately preceding jint:
        Node* x1 = transform_later(new AddXNode(start, MakeConX(-bump_bit)) );
        Node* p1 = basic_plus_adr(dest, x1);
        mem = StoreNode::make(_igvn, ctrl, mem, p1, adr_type, intcon(0), T_INT, MemNode::unordered);
        mem = transform_later(mem);
      }
    }
    Node* end = dest_size; // pre-rounded
    mem = ClearArrayNode::clear_memory(ctrl, mem, dest,
                                       start, end, &_igvn);
  } else {
    // Non-constant start, unrounded non-constant end.
    // (Nobody zeroes a random midsection of an array using this routine.)
    ShouldNotReachHere();       // fix caller
  }

  // Done.
  merge_mem->set_memory_at(alias_idx, mem);
}

bool PhaseMacroExpand::generate_block_arraycopy(Node** ctrl, MergeMemNode** mem, Node* io,
                                                const TypePtr* adr_type,
                                                BasicType basic_elem_type,
                                                AllocateNode* alloc,
                                                Node* src,  Node* src_offset,
                                                Node* dest, Node* dest_offset,
                                                Node* dest_size, bool dest_uninitialized) {
  // See if there is an advantage from block transfer.
  int scale = exact_log2(type2aelembytes(basic_elem_type));
  if (scale >= LogBytesPerLong)
    return false;               // it is already a block transfer

  // Look at the alignment of the starting offsets.
  int abase = arrayOopDesc::base_offset_in_bytes(basic_elem_type);

  intptr_t src_off_con  = (intptr_t) _igvn.find_int_con(src_offset, -1);
  intptr_t dest_off_con = (intptr_t) _igvn.find_int_con(dest_offset, -1);
  if (src_off_con < 0 || dest_off_con < 0) {
    // At present, we can only understand constants.
    return false;
  }

  intptr_t src_off  = abase + (src_off_con  << scale);
  intptr_t dest_off = abase + (dest_off_con << scale);

  if (((src_off | dest_off) & (BytesPerLong-1)) != 0) {
    // Non-aligned; too bad.
    // One more chance:  Pick off an initial 32-bit word.
    // This is a common case, since abase can be odd mod 8.
    if (((src_off | dest_off) & (BytesPerLong-1)) == BytesPerInt &&
        ((src_off ^ dest_off) & (BytesPerLong-1)) == 0) {
      Node* sptr = basic_plus_adr(src,  src_off);
      Node* dptr = basic_plus_adr(dest, dest_off);
      const TypePtr* s_adr_type = _igvn.type(sptr)->is_ptr();
      assert(s_adr_type->isa_aryptr(), "impossible slice");
      uint s_alias_idx = C->get_alias_index(s_adr_type);
      uint d_alias_idx = C->get_alias_index(adr_type);
      bool is_mismatched = (basic_elem_type != T_INT);
      Node* sval = transform_later(
          LoadNode::make(_igvn, *ctrl, (*mem)->memory_at(s_alias_idx), sptr, s_adr_type,
                         TypeInt::INT, T_INT, MemNode::unordered, LoadNode::DependsOnlyOnTest,
                         false /*unaligned*/, is_mismatched));
      Node* st = transform_later(
          StoreNode::make(_igvn, *ctrl, (*mem)->memory_at(d_alias_idx), dptr, adr_type,
                          sval, T_INT, MemNode::unordered));
      if (is_mismatched) {
        st->as_Store()->set_mismatched_access();
      }
      (*mem)->set_memory_at(d_alias_idx, st);
      src_off += BytesPerInt;
      dest_off += BytesPerInt;
    } else {
      return false;
    }
  }
  assert(src_off % BytesPerLong == 0, "");
  assert(dest_off % BytesPerLong == 0, "");

  // Do this copy by giant steps.
  Node* sptr  = basic_plus_adr(src,  src_off);
  Node* dptr  = basic_plus_adr(dest, dest_off);
  Node* countx = dest_size;
  countx = transform_later(new SubXNode(countx, MakeConX(dest_off)));
  countx = transform_later(new URShiftXNode(countx, intcon(LogBytesPerLong)));

  bool disjoint_bases = true;   // since alloc != NULL
  generate_unchecked_arraycopy(ctrl, mem,
                               adr_type, T_LONG, disjoint_bases,
                               sptr, NULL, dptr, NULL, countx, dest_uninitialized);

  return true;
}

// Helper function; generates code for the slow case.
// We make a call to a runtime method which emulates the native method,
// but without the native wrapper overhead.
MergeMemNode* PhaseMacroExpand::generate_slow_arraycopy(ArrayCopyNode *ac,
                                                        Node** ctrl, Node* mem, Node** io,
                                                        const TypePtr* adr_type,
                                                        Node* src,  Node* src_offset,
                                                        Node* dest, Node* dest_offset,
                                                        Node* copy_length, bool dest_uninitialized) {
  assert(!dest_uninitialized, "Invariant");

  const TypeFunc* call_type = OptoRuntime::slow_arraycopy_Type();
  CallNode* call = new CallStaticJavaNode(call_type, OptoRuntime::slow_arraycopy_Java(),
                                          "slow_arraycopy", TypePtr::BOTTOM);

  call->init_req(TypeFunc::Control, *ctrl);
  call->init_req(TypeFunc::I_O    , *io);
  call->init_req(TypeFunc::Memory , mem);
  call->init_req(TypeFunc::ReturnAdr, top());
  call->init_req(TypeFunc::FramePtr, top());
  call->init_req(TypeFunc::Parms+0, src);
  call->init_req(TypeFunc::Parms+1, src_offset);
  call->init_req(TypeFunc::Parms+2, dest);
  call->init_req(TypeFunc::Parms+3, dest_offset);
  call->init_req(TypeFunc::Parms+4, copy_length);
  call->copy_call_debug_info(&_igvn, ac);

  call->set_cnt(PROB_UNLIKELY_MAG(4));  // Same effect as RC_UNCOMMON.
  _igvn.replace_node(ac, call);
  transform_later(call);

  call->extract_projections(&_callprojs, false /*separate_io_proj*/, false /*do_asserts*/);
  *ctrl = _callprojs.fallthrough_catchproj->clone();
  transform_later(*ctrl);

  Node* m = _callprojs.fallthrough_memproj->clone();
  transform_later(m);

  uint alias_idx = C->get_alias_index(adr_type);
  MergeMemNode* out_mem;
  if (alias_idx != Compile::AliasIdxBot) {
    out_mem = MergeMemNode::make(mem);
    out_mem->set_memory_at(alias_idx, m);
  } else {
    out_mem = MergeMemNode::make(m);
  }
  transform_later(out_mem);

  *io = _callprojs.fallthrough_ioproj->clone();
  transform_later(*io);

  return out_mem;
}

// Helper function; generates code for cases requiring runtime checks.
Node* PhaseMacroExpand::generate_checkcast_arraycopy(Node** ctrl, MergeMemNode** mem,
                                                     const TypePtr* adr_type,
                                                     Node* dest_elem_klass,
                                                     Node* src,  Node* src_offset,
                                                     Node* dest, Node* dest_offset,
                                                     Node* copy_length, bool dest_uninitialized) {
  if ((*ctrl)->is_top())  return NULL;

  address copyfunc_addr = StubRoutines::checkcast_arraycopy(dest_uninitialized);
  if (copyfunc_addr == NULL) { // Stub was not generated, go slow path.
    return NULL;
  }

  // Pick out the parameters required to perform a store-check
  // for the target array.  This is an optimistic check.  It will
  // look in each non-null element's class, at the desired klass's
  // super_check_offset, for the desired klass.
  int sco_offset = in_bytes(Klass::super_check_offset_offset());
  Node* p3 = basic_plus_adr(dest_elem_klass, sco_offset);
  Node* n3 = new LoadINode(NULL, *mem /*memory(p3)*/, p3, _igvn.type(p3)->is_ptr(), TypeInt::INT, MemNode::unordered);
  Node* check_offset = ConvI2X(transform_later(n3));
  Node* check_value  = dest_elem_klass;

  Node* src_start  = array_element_address(src,  src_offset,  T_OBJECT);
  Node* dest_start = array_element_address(dest, dest_offset, T_OBJECT);

  const TypeFunc* call_type = OptoRuntime::checkcast_arraycopy_Type();
  Node* call = make_leaf_call(*ctrl, *mem, call_type, copyfunc_addr, "checkcast_arraycopy", adr_type,
                              src_start, dest_start, copy_length XTOP, check_offset XTOP, check_value);

  finish_arraycopy_call(call, ctrl, mem, adr_type);

  Node* proj =  new ProjNode(call, TypeFunc::Parms);
  transform_later(proj);

  return proj;
}

// Helper function; generates code for cases requiring runtime checks.
Node* PhaseMacroExpand::generate_generic_arraycopy(Node** ctrl, MergeMemNode** mem,
                                                   const TypePtr* adr_type,
                                                   Node* src,  Node* src_offset,
                                                   Node* dest, Node* dest_offset,
                                                   Node* copy_length, bool dest_uninitialized) {
  if ((*ctrl)->is_top()) return NULL;
  assert(!dest_uninitialized, "Invariant");

  address copyfunc_addr = StubRoutines::generic_arraycopy();
  if (copyfunc_addr == NULL) { // Stub was not generated, go slow path.
    return NULL;
  }

  const TypeFunc* call_type = OptoRuntime::generic_arraycopy_Type();
  Node* call = make_leaf_call(*ctrl, *mem, call_type, copyfunc_addr, "generic_arraycopy", adr_type,
                              src, src_offset, dest, dest_offset, copy_length);

  finish_arraycopy_call(call, ctrl, mem, adr_type);

  Node* proj =  new ProjNode(call, TypeFunc::Parms);
  transform_later(proj);

  return proj;
}

// Helper function; generates the fast out-of-line call to an arraycopy stub.
bool PhaseMacroExpand::generate_unchecked_arraycopy(Node** ctrl, MergeMemNode** mem,
                                                    const TypePtr* adr_type,
                                                    BasicType basic_elem_type,
                                                    bool disjoint_bases,
                                                    Node* src,  Node* src_offset,
                                                    Node* dest, Node* dest_offset,
                                                    Node* copy_length, bool dest_uninitialized) {
  if ((*ctrl)->is_top()) return false;

  Node* src_start  = src;
  Node* dest_start = dest;
  if (src_offset != NULL || dest_offset != NULL) {
    src_start =  array_element_address(src, src_offset, basic_elem_type);
    dest_start = array_element_address(dest, dest_offset, basic_elem_type);
  }

  // Figure out which arraycopy runtime method to call.
  const char* copyfunc_name = "arraycopy";
  address     copyfunc_addr =
      basictype2arraycopy(basic_elem_type, src_offset, dest_offset,
                          disjoint_bases, copyfunc_name, dest_uninitialized);

  Node* result_memory = NULL;
  RegionNode* exit_block = NULL;
  if (ArrayOperationPartialInlineSize > 0 && is_subword_type(basic_elem_type) &&
    Matcher::vector_width_in_bytes(basic_elem_type) >= 16) {
    generate_partial_inlining_block(ctrl, mem, adr_type, &exit_block, &result_memory,
                                    copy_length, src_start, dest_start, basic_elem_type);
  }

  const TypeFunc* call_type = OptoRuntime::fast_arraycopy_Type();
  Node* call = make_leaf_call(*ctrl, *mem, call_type, copyfunc_addr, copyfunc_name, adr_type,
                              src_start, dest_start, copy_length XTOP);

  finish_arraycopy_call(call, ctrl, mem, adr_type);

  // Connecting remaining edges for exit_block coming from stub_block.
  if (exit_block) {
    exit_block->init_req(2, *ctrl);

    // Memory edge corresponding to stub_region.
    result_memory->init_req(2, *mem);

    uint alias_idx = C->get_alias_index(adr_type);
    if (alias_idx != Compile::AliasIdxBot) {
      *mem = MergeMemNode::make(*mem);
      (*mem)->set_memory_at(alias_idx, result_memory);
    } else {
      *mem = MergeMemNode::make(result_memory);
    }
    transform_later(*mem);
    *ctrl = exit_block;
    return true;
  }
  return false;
}

#undef XTOP

void PhaseMacroExpand::expand_arraycopy_node(ArrayCopyNode *ac) {
  Node* ctrl = ac->in(TypeFunc::Control);
  Node* io = ac->in(TypeFunc::I_O);
  Node* src = ac->in(ArrayCopyNode::Src);
  Node* src_offset = ac->in(ArrayCopyNode::SrcPos);
  Node* dest = ac->in(ArrayCopyNode::Dest);
  Node* dest_offset = ac->in(ArrayCopyNode::DestPos);
  Node* length = ac->in(ArrayCopyNode::Length);
  MergeMemNode* merge_mem = NULL;

  if (ac->is_clonebasic()) {
    BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
    bs->clone_at_expansion(this, ac);
    return;
  } else if (ac->is_copyof() || ac->is_copyofrange() || ac->is_clone_oop_array()) {
    Node* mem = ac->in(TypeFunc::Memory);
    merge_mem = MergeMemNode::make(mem);
    transform_later(merge_mem);

    AllocateArrayNode* alloc = NULL;
    if (ac->is_alloc_tightly_coupled()) {
      alloc = AllocateArrayNode::Ideal_array_allocation(dest, &_igvn);
      assert(alloc != NULL, "expect alloc");
    }

    const TypePtr* adr_type = _igvn.type(dest)->is_oopptr()->add_offset(Type::OffsetBot);
    if (ac->_dest_type != TypeOopPtr::BOTTOM) {
      adr_type = ac->_dest_type->add_offset(Type::OffsetBot)->is_ptr();
    }
    generate_arraycopy(ac, alloc, &ctrl, merge_mem, &io,
                       adr_type, T_OBJECT,
                       src, src_offset, dest, dest_offset, length,
                       true, !ac->is_copyofrange());

    return;
  }

  AllocateArrayNode* alloc = NULL;
  if (ac->is_alloc_tightly_coupled()) {
    alloc = AllocateArrayNode::Ideal_array_allocation(dest, &_igvn);
    assert(alloc != NULL, "expect alloc");
  }

  assert(ac->is_arraycopy() || ac->is_arraycopy_validated(), "should be an arraycopy");

  // Compile time checks.  If any of these checks cannot be verified at compile time,
  // we do not make a fast path for this call.  Instead, we let the call remain as it
  // is.  The checks we choose to mandate at compile time are:
  //
  // (1) src and dest are arrays.
  const Type* src_type = src->Value(&_igvn);
  const Type* dest_type = dest->Value(&_igvn);
  const TypeAryPtr* top_src = src_type->isa_aryptr();
  const TypeAryPtr* top_dest = dest_type->isa_aryptr();

  BasicType src_elem = T_CONFLICT;
  BasicType dest_elem = T_CONFLICT;

  if (top_dest != NULL && top_dest->klass() != NULL) {
    dest_elem = top_dest->klass()->as_array_klass()->element_type()->basic_type();
  }
  if (top_src != NULL && top_src->klass() != NULL) {
    src_elem = top_src->klass()->as_array_klass()->element_type()->basic_type();
  }
  if (is_reference_type(src_elem))  src_elem  = T_OBJECT;
  if (is_reference_type(dest_elem)) dest_elem = T_OBJECT;

  if (ac->is_arraycopy_validated() &&
      dest_elem != T_CONFLICT &&
      src_elem == T_CONFLICT) {
    src_elem = dest_elem;
  }

  if (src_elem == T_CONFLICT || dest_elem == T_CONFLICT) {
    // Conservatively insert a memory barrier on all memory slices.
    // Do not let writes into the source float below the arraycopy.
    {
      Node* mem = ac->in(TypeFunc::Memory);
      insert_mem_bar(&ctrl, &mem, Op_MemBarCPUOrder);

      merge_mem = MergeMemNode::make(mem);
      transform_later(merge_mem);
    }

    // Call StubRoutines::generic_arraycopy stub.
    Node* mem = generate_arraycopy(ac, NULL, &ctrl, merge_mem, &io,
                                   TypeRawPtr::BOTTOM, T_CONFLICT,
                                   src, src_offset, dest, dest_offset, length,
                                   // If a  negative length guard was generated for the ArrayCopyNode,
                                   // the length of the array can never be negative.
                                   false, ac->has_negative_length_guard());
    return;
  }

  assert(!ac->is_arraycopy_validated() || (src_elem == dest_elem && dest_elem != T_VOID), "validated but different basic types");

  // (2) src and dest arrays must have elements of the same BasicType
  // Figure out the size and type of the elements we will be copying.
  if (src_elem != dest_elem || dest_elem == T_VOID) {
    // The component types are not the same or are not recognized.  Punt.
    // (But, avoid the native method wrapper to JVM_ArrayCopy.)
    {
      Node* mem = ac->in(TypeFunc::Memory);
      merge_mem = generate_slow_arraycopy(ac, &ctrl, mem, &io, TypePtr::BOTTOM, src, src_offset, dest, dest_offset, length, false);
    }

    _igvn.replace_node(_callprojs.fallthrough_memproj, merge_mem);
    _igvn.replace_node(_callprojs.fallthrough_ioproj, io);
    _igvn.replace_node(_callprojs.fallthrough_catchproj, ctrl);
    return;
  }

  //---------------------------------------------------------------------------
  // We will make a fast path for this call to arraycopy.

  // We have the following tests left to perform:
  //
  // (3) src and dest must not be null.
  // (4) src_offset must not be negative.
  // (5) dest_offset must not be negative.
  // (6) length must not be negative.
  // (7) src_offset + length must not exceed length of src.
  // (8) dest_offset + length must not exceed length of dest.
  // (9) each element of an oop array must be assignable

  {
    Node* mem = ac->in(TypeFunc::Memory);
    merge_mem = MergeMemNode::make(mem);
    transform_later(merge_mem);
  }

  RegionNode* slow_region = new RegionNode(1);
  transform_later(slow_region);

  if (!ac->is_arraycopy_validated()) {
    // (3) operands must not be null
    // We currently perform our null checks with the null_check routine.
    // This means that the null exceptions will be reported in the caller
    // rather than (correctly) reported inside of the native arraycopy call.
    // This should be corrected, given time.  We do our null check with the
    // stack pointer restored.
    // null checks done library_call.cpp

    // (4) src_offset must not be negative.
    generate_negative_guard(&ctrl, src_offset, slow_region);

    // (5) dest_offset must not be negative.
    generate_negative_guard(&ctrl, dest_offset, slow_region);

    // (6) length must not be negative (moved to generate_arraycopy()).
    // generate_negative_guard(length, slow_region);

    // (7) src_offset + length must not exceed length of src.
    Node* alen = ac->in(ArrayCopyNode::SrcLen);
    assert(alen != NULL, "need src len");
    generate_limit_guard(&ctrl,
                         src_offset, length,
                         alen,
                         slow_region);

    // (8) dest_offset + length must not exceed length of dest.
    alen = ac->in(ArrayCopyNode::DestLen);
    assert(alen != NULL, "need dest len");
    generate_limit_guard(&ctrl,
                         dest_offset, length,
                         alen,
                         slow_region);

    // (9) each element of an oop array must be assignable
    // The generate_arraycopy subroutine checks this.
  }
  // This is where the memory effects are placed:
  const TypePtr* adr_type = NULL;
  if (ac->_dest_type != TypeOopPtr::BOTTOM) {
    adr_type = ac->_dest_type->add_offset(Type::OffsetBot)->is_ptr();
  } else {
    adr_type = TypeAryPtr::get_array_body_type(dest_elem);
  }

  generate_arraycopy(ac, alloc, &ctrl, merge_mem, &io,
                     adr_type, dest_elem,
                     src, src_offset, dest, dest_offset, length,
                     // If a  negative length guard was generated for the ArrayCopyNode,
                     // the length of the array can never be negative.
                     false, ac->has_negative_length_guard(), slow_region);
}
