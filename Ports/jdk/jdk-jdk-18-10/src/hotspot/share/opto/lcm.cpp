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
#include "asm/macroAssembler.inline.hpp"
#include "gc/shared/gc_globals.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/compressedOops.hpp"
#include "opto/ad.hpp"
#include "opto/block.hpp"
#include "opto/c2compiler.hpp"
#include "opto/callnode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/machnode.hpp"
#include "opto/runtime.hpp"
#include "opto/chaitin.hpp"
#include "runtime/sharedRuntime.hpp"

// Optimization - Graph Style

// Check whether val is not-null-decoded compressed oop,
// i.e. will grab into the base of the heap if it represents NULL.
static bool accesses_heap_base_zone(Node *val) {
  if (CompressedOops::base() != NULL) { // Implies UseCompressedOops.
    if (val && val->is_Mach()) {
      if (val->as_Mach()->ideal_Opcode() == Op_DecodeN) {
        // This assumes all Decodes with TypePtr::NotNull are matched to nodes that
        // decode NULL to point to the heap base (Decode_NN).
        if (val->bottom_type()->is_oopptr()->ptr() == TypePtr::NotNull) {
          return true;
        }
      }
      // Must recognize load operation with Decode matched in memory operand.
      // We should not reach here exept for PPC/AIX, as os::zero_page_read_protected()
      // returns true everywhere else. On PPC, no such memory operands
      // exist, therefore we did not yet implement a check for such operands.
      NOT_AIX(Unimplemented());
    }
  }
  return false;
}

static bool needs_explicit_null_check_for_read(Node *val) {
  // On some OSes (AIX) the page at address 0 is only write protected.
  // If so, only Store operations will trap.
  if (os::zero_page_read_protected()) {
    return false;  // Implicit null check will work.
  }
  // Also a read accessing the base of a heap-based compressed heap will trap.
  if (accesses_heap_base_zone(val) &&         // Hits the base zone page.
      CompressedOops::use_implicit_null_checks()) { // Base zone page is protected.
    return false;
  }

  return true;
}

//------------------------------implicit_null_check----------------------------
// Detect implicit-null-check opportunities.  Basically, find NULL checks
// with suitable memory ops nearby.  Use the memory op to do the NULL check.
// I can generate a memory op if there is not one nearby.
// The proj is the control projection for the not-null case.
// The val is the pointer being checked for nullness or
// decodeHeapOop_not_null node if it did not fold into address.
void PhaseCFG::implicit_null_check(Block* block, Node *proj, Node *val, int allowed_reasons) {
  // Assume if null check need for 0 offset then always needed
  // Intel solaris doesn't support any null checks yet and no
  // mechanism exists (yet) to set the switches at an os_cpu level
  if( !ImplicitNullChecks || MacroAssembler::needs_explicit_null_check(0)) return;

  // Make sure the ptr-is-null path appears to be uncommon!
  float f = block->end()->as_MachIf()->_prob;
  if( proj->Opcode() == Op_IfTrue ) f = 1.0f - f;
  if( f > PROB_UNLIKELY_MAG(4) ) return;

  uint bidx = 0;                // Capture index of value into memop
  bool was_store;               // Memory op is a store op

  // Get the successor block for if the test ptr is non-null
  Block* not_null_block;  // this one goes with the proj
  Block* null_block;
  if (block->get_node(block->number_of_nodes()-1) == proj) {
    null_block     = block->_succs[0];
    not_null_block = block->_succs[1];
  } else {
    assert(block->get_node(block->number_of_nodes()-2) == proj, "proj is one or the other");
    not_null_block = block->_succs[0];
    null_block     = block->_succs[1];
  }
  while (null_block->is_Empty() == Block::empty_with_goto) {
    null_block     = null_block->_succs[0];
  }

  // Search the exception block for an uncommon trap.
  // (See Parse::do_if and Parse::do_ifnull for the reason
  // we need an uncommon trap.  Briefly, we need a way to
  // detect failure of this optimization, as in 6366351.)
  {
    bool found_trap = false;
    for (uint i1 = 0; i1 < null_block->number_of_nodes(); i1++) {
      Node* nn = null_block->get_node(i1);
      if (nn->is_MachCall() &&
          nn->as_MachCall()->entry_point() == SharedRuntime::uncommon_trap_blob()->entry_point()) {
        const Type* trtype = nn->in(TypeFunc::Parms)->bottom_type();
        if (trtype->isa_int() && trtype->is_int()->is_con()) {
          jint tr_con = trtype->is_int()->get_con();
          Deoptimization::DeoptReason reason = Deoptimization::trap_request_reason(tr_con);
          Deoptimization::DeoptAction action = Deoptimization::trap_request_action(tr_con);
          assert((int)reason < (int)BitsPerInt, "recode bit map");
          if (is_set_nth_bit(allowed_reasons, (int) reason)
              && action != Deoptimization::Action_none) {
            // This uncommon trap is sure to recompile, eventually.
            // When that happens, C->too_many_traps will prevent
            // this transformation from happening again.
            found_trap = true;
          }
        }
        break;
      }
    }
    if (!found_trap) {
      // We did not find an uncommon trap.
      return;
    }
  }

  // Check for decodeHeapOop_not_null node which did not fold into address
  bool is_decoden = ((intptr_t)val) & 1;
  val = (Node*)(((intptr_t)val) & ~1);

  assert(!is_decoden || (val->in(0) == NULL) && val->is_Mach() &&
         (val->as_Mach()->ideal_Opcode() == Op_DecodeN), "sanity");

  // Search the successor block for a load or store who's base value is also
  // the tested value.  There may be several.
  MachNode *best = NULL;        // Best found so far
  for (DUIterator i = val->outs(); val->has_out(i); i++) {
    Node *m = val->out(i);
    if( !m->is_Mach() ) continue;
    MachNode *mach = m->as_Mach();
    was_store = false;
    int iop = mach->ideal_Opcode();
    switch( iop ) {
    case Op_LoadB:
    case Op_LoadUB:
    case Op_LoadUS:
    case Op_LoadD:
    case Op_LoadF:
    case Op_LoadI:
    case Op_LoadL:
    case Op_LoadP:
    case Op_LoadN:
    case Op_LoadS:
    case Op_LoadKlass:
    case Op_LoadNKlass:
    case Op_LoadRange:
    case Op_LoadD_unaligned:
    case Op_LoadL_unaligned:
      assert(mach->in(2) == val, "should be address");
      break;
    case Op_StoreB:
    case Op_StoreC:
    case Op_StoreCM:
    case Op_StoreD:
    case Op_StoreF:
    case Op_StoreI:
    case Op_StoreL:
    case Op_StoreP:
    case Op_StoreN:
    case Op_StoreNKlass:
      was_store = true;         // Memory op is a store op
      // Stores will have their address in slot 2 (memory in slot 1).
      // If the value being nul-checked is in another slot, it means we
      // are storing the checked value, which does NOT check the value!
      if( mach->in(2) != val ) continue;
      break;                    // Found a memory op?
    case Op_StrComp:
    case Op_StrEquals:
    case Op_StrIndexOf:
    case Op_StrIndexOfChar:
    case Op_AryEq:
    case Op_StrInflatedCopy:
    case Op_StrCompressedCopy:
    case Op_EncodeISOArray:
    case Op_HasNegatives:
      // Not a legit memory op for implicit null check regardless of
      // embedded loads
      continue;
    default:                    // Also check for embedded loads
      if( !mach->needs_anti_dependence_check() )
        continue;               // Not an memory op; skip it
      if( must_clone[iop] ) {
        // Do not move nodes which produce flags because
        // RA will try to clone it to place near branch and
        // it will cause recompilation, see clone_node().
        continue;
      }
      {
        // Check that value is used in memory address in
        // instructions with embedded load (CmpP val1,(val2+off)).
        Node* base;
        Node* index;
        const MachOper* oper = mach->memory_inputs(base, index);
        if (oper == NULL || oper == (MachOper*)-1) {
          continue;             // Not an memory op; skip it
        }
        if (val == base ||
            (val == index && val->bottom_type()->isa_narrowoop())) {
          break;                // Found it
        } else {
          continue;             // Skip it
        }
      }
      break;
    }

    // On some OSes (AIX) the page at address 0 is only write protected.
    // If so, only Store operations will trap.
    // But a read accessing the base of a heap-based compressed heap will trap.
    if (!was_store && needs_explicit_null_check_for_read(val)) {
      continue;
    }

    // Check that node's control edge is not-null block's head or dominates it,
    // otherwise we can't hoist it because there are other control dependencies.
    Node* ctrl = mach->in(0);
    if (ctrl != NULL && !(ctrl == not_null_block->head() ||
        get_block_for_node(ctrl)->dominates(not_null_block))) {
      continue;
    }

    // check if the offset is not too high for implicit exception
    {
      intptr_t offset = 0;
      const TypePtr *adr_type = NULL;  // Do not need this return value here
      const Node* base = mach->get_base_and_disp(offset, adr_type);
      if (base == NULL || base == NodeSentinel) {
        // Narrow oop address doesn't have base, only index.
        // Give up if offset is beyond page size or if heap base is not protected.
        if (val->bottom_type()->isa_narrowoop() &&
            (MacroAssembler::needs_explicit_null_check(offset) ||
             !CompressedOops::use_implicit_null_checks()))
          continue;
        // cannot reason about it; is probably not implicit null exception
      } else {
        const TypePtr* tptr;
        if ((UseCompressedOops || UseCompressedClassPointers) &&
            (CompressedOops::shift() == 0 || CompressedKlassPointers::shift() == 0)) {
          // 32-bits narrow oop can be the base of address expressions
          tptr = base->get_ptr_type();
        } else {
          // only regular oops are expected here
          tptr = base->bottom_type()->is_ptr();
        }
        // Give up if offset is not a compile-time constant.
        if (offset == Type::OffsetBot || tptr->_offset == Type::OffsetBot)
          continue;
        offset += tptr->_offset; // correct if base is offseted
        // Give up if reference is beyond page size.
        if (MacroAssembler::needs_explicit_null_check(offset))
          continue;
        // Give up if base is a decode node and the heap base is not protected.
        if (base->is_Mach() && base->as_Mach()->ideal_Opcode() == Op_DecodeN &&
            !CompressedOops::use_implicit_null_checks())
          continue;
      }
    }

    // Check ctrl input to see if the null-check dominates the memory op
    Block *cb = get_block_for_node(mach);
    cb = cb->_idom;             // Always hoist at least 1 block
    if( !was_store ) {          // Stores can be hoisted only one block
      while( cb->_dom_depth > (block->_dom_depth + 1))
        cb = cb->_idom;         // Hoist loads as far as we want
      // The non-null-block should dominate the memory op, too. Live
      // range spilling will insert a spill in the non-null-block if it is
      // needs to spill the memory op for an implicit null check.
      if (cb->_dom_depth == (block->_dom_depth + 1)) {
        if (cb != not_null_block) continue;
        cb = cb->_idom;
      }
    }
    if( cb != block ) continue;

    // Found a memory user; see if it can be hoisted to check-block
    uint vidx = 0;              // Capture index of value into memop
    uint j;
    for( j = mach->req()-1; j > 0; j-- ) {
      if( mach->in(j) == val ) {
        vidx = j;
        // Ignore DecodeN val which could be hoisted to where needed.
        if( is_decoden ) continue;
      }
      // Block of memory-op input
      Block *inb = get_block_for_node(mach->in(j));
      Block *b = block;          // Start from nul check
      while( b != inb && b->_dom_depth > inb->_dom_depth )
        b = b->_idom;           // search upwards for input
      // See if input dominates null check
      if( b != inb )
        break;
    }
    if( j > 0 )
      continue;
    Block *mb = get_block_for_node(mach);
    // Hoisting stores requires more checks for the anti-dependence case.
    // Give up hoisting if we have to move the store past any load.
    if( was_store ) {
      Block *b = mb;            // Start searching here for a local load
      // mach use (faulting) trying to hoist
      // n might be blocker to hoisting
      while( b != block ) {
        uint k;
        for( k = 1; k < b->number_of_nodes(); k++ ) {
          Node *n = b->get_node(k);
          if( n->needs_anti_dependence_check() &&
              n->in(LoadNode::Memory) == mach->in(StoreNode::Memory) )
            break;              // Found anti-dependent load
        }
        if( k < b->number_of_nodes() )
          break;                // Found anti-dependent load
        // Make sure control does not do a merge (would have to check allpaths)
        if( b->num_preds() != 2 ) break;
        b = get_block_for_node(b->pred(1)); // Move up to predecessor block
      }
      if( b != block ) continue;
    }

    // Make sure this memory op is not already being used for a NullCheck
    Node *e = mb->end();
    if( e->is_MachNullCheck() && e->in(1) == mach )
      continue;                 // Already being used as a NULL check

    // Found a candidate!  Pick one with least dom depth - the highest
    // in the dom tree should be closest to the null check.
    if (best == NULL || get_block_for_node(mach)->_dom_depth < get_block_for_node(best)->_dom_depth) {
      best = mach;
      bidx = vidx;
    }
  }
  // No candidate!
  if (best == NULL) {
    return;
  }

  // ---- Found an implicit null check
#ifndef PRODUCT
  extern int implicit_null_checks;
  implicit_null_checks++;
#endif

  if( is_decoden ) {
    // Check if we need to hoist decodeHeapOop_not_null first.
    Block *valb = get_block_for_node(val);
    if( block != valb && block->_dom_depth < valb->_dom_depth ) {
      // Hoist it up to the end of the test block together with its inputs if they exist.
      for (uint i = 2; i < val->req(); i++) {
        // DecodeN has 2 regular inputs + optional MachTemp or load Base inputs.
        Node *temp = val->in(i);
        Block *tempb = get_block_for_node(temp);
        if (!tempb->dominates(block)) {
          assert(block->dominates(tempb), "sanity check: temp node placement");
          // We only expect nodes without further inputs, like MachTemp or load Base.
          assert(temp->req() == 0 || (temp->req() == 1 && temp->in(0) == (Node*)C->root()),
                 "need for recursive hoisting not expected");
          tempb->find_remove(temp);
          block->add_inst(temp);
          map_node_to_block(temp, block);
        }
      }
      valb->find_remove(val);
      block->add_inst(val);
      map_node_to_block(val, block);
      // DecodeN on x86 may kill flags. Check for flag-killing projections
      // that also need to be hoisted.
      for (DUIterator_Fast jmax, j = val->fast_outs(jmax); j < jmax; j++) {
        Node* n = val->fast_out(j);
        if( n->is_MachProj() ) {
          get_block_for_node(n)->find_remove(n);
          block->add_inst(n);
          map_node_to_block(n, block);
        }
      }
    }
  }
  // Hoist the memory candidate up to the end of the test block.
  Block *old_block = get_block_for_node(best);
  old_block->find_remove(best);
  block->add_inst(best);
  map_node_to_block(best, block);

  // Move the control dependence if it is pinned to not-null block.
  // Don't change it in other cases: NULL or dominating control.
  Node* ctrl = best->in(0);
  if (ctrl != NULL && get_block_for_node(ctrl) == not_null_block) {
    // Set it to control edge of null check.
    best->set_req(0, proj->in(0)->in(0));
  }

  // Check for flag-killing projections that also need to be hoisted
  // Should be DU safe because no edge updates.
  for (DUIterator_Fast jmax, j = best->fast_outs(jmax); j < jmax; j++) {
    Node* n = best->fast_out(j);
    if( n->is_MachProj() ) {
      get_block_for_node(n)->find_remove(n);
      block->add_inst(n);
      map_node_to_block(n, block);
    }
  }

  // proj==Op_True --> ne test; proj==Op_False --> eq test.
  // One of two graph shapes got matched:
  //   (IfTrue  (If (Bool NE (CmpP ptr NULL))))
  //   (IfFalse (If (Bool EQ (CmpP ptr NULL))))
  // NULL checks are always branch-if-eq.  If we see a IfTrue projection
  // then we are replacing a 'ne' test with a 'eq' NULL check test.
  // We need to flip the projections to keep the same semantics.
  if( proj->Opcode() == Op_IfTrue ) {
    // Swap order of projections in basic block to swap branch targets
    Node *tmp1 = block->get_node(block->end_idx()+1);
    Node *tmp2 = block->get_node(block->end_idx()+2);
    block->map_node(tmp2, block->end_idx()+1);
    block->map_node(tmp1, block->end_idx()+2);
    Node *tmp = new Node(C->top()); // Use not NULL input
    tmp1->replace_by(tmp);
    tmp2->replace_by(tmp1);
    tmp->replace_by(tmp2);
    tmp->destruct(NULL);
  }

  // Remove the existing null check; use a new implicit null check instead.
  // Since schedule-local needs precise def-use info, we need to correct
  // it as well.
  Node *old_tst = proj->in(0);
  MachNode *nul_chk = new MachNullCheckNode(old_tst->in(0),best,bidx);
  block->map_node(nul_chk, block->end_idx());
  map_node_to_block(nul_chk, block);
  // Redirect users of old_test to nul_chk
  for (DUIterator_Last i2min, i2 = old_tst->last_outs(i2min); i2 >= i2min; --i2)
    old_tst->last_out(i2)->set_req(0, nul_chk);
  // Clean-up any dead code
  for (uint i3 = 0; i3 < old_tst->req(); i3++) {
    Node* in = old_tst->in(i3);
    old_tst->set_req(i3, NULL);
    if (in->outcnt() == 0) {
      // Remove dead input node
      in->disconnect_inputs(C);
      block->find_remove(in);
    }
  }

  latency_from_uses(nul_chk);
  latency_from_uses(best);

  // insert anti-dependences to defs in this block
  if (! best->needs_anti_dependence_check()) {
    for (uint k = 1; k < block->number_of_nodes(); k++) {
      Node *n = block->get_node(k);
      if (n->needs_anti_dependence_check() &&
          n->in(LoadNode::Memory) == best->in(StoreNode::Memory)) {
        // Found anti-dependent load
        insert_anti_dependences(block, n);
      }
    }
  }
}


//------------------------------select-----------------------------------------
// Select a nice fellow from the worklist to schedule next. If there is only
// one choice, then use it. Projections take top priority for correctness
// reasons - if I see a projection, then it is next.  There are a number of
// other special cases, for instructions that consume condition codes, et al.
// These are chosen immediately. Some instructions are required to immediately
// precede the last instruction in the block, and these are taken last. Of the
// remaining cases (most), choose the instruction with the greatest latency
// (that is, the most number of pseudo-cycles required to the end of the
// routine). If there is a tie, choose the instruction with the most inputs.
Node* PhaseCFG::select(
  Block* block,
  Node_List &worklist,
  GrowableArray<int> &ready_cnt,
  VectorSet &next_call,
  uint sched_slot,
  intptr_t* recalc_pressure_nodes) {

  // If only a single entry on the stack, use it
  uint cnt = worklist.size();
  if (cnt == 1) {
    Node *n = worklist[0];
    worklist.map(0,worklist.pop());
    return n;
  }

  uint choice  = 0; // Bigger is most important
  uint latency = 0; // Bigger is scheduled first
  uint score   = 0; // Bigger is better
  int idx = -1;     // Index in worklist
  int cand_cnt = 0; // Candidate count
  bool block_size_threshold_ok = (block->number_of_nodes() > 10) ? true : false;

  for( uint i=0; i<cnt; i++ ) { // Inspect entire worklist
    // Order in worklist is used to break ties.
    // See caller for how this is used to delay scheduling
    // of induction variable increments to after the other
    // uses of the phi are scheduled.
    Node *n = worklist[i];      // Get Node on worklist

    int iop = n->is_Mach() ? n->as_Mach()->ideal_Opcode() : 0;
    if( n->is_Proj() ||         // Projections always win
        n->Opcode()== Op_Con || // So does constant 'Top'
        iop == Op_CreateEx ||   // Create-exception must start block
        iop == Op_CheckCastPP
        ) {
      worklist.map(i,worklist.pop());
      return n;
    }

    // Final call in a block must be adjacent to 'catch'
    Node *e = block->end();
    if( e->is_Catch() && e->in(0)->in(0) == n )
      continue;

    // Memory op for an implicit null check has to be at the end of the block
    if( e->is_MachNullCheck() && e->in(1) == n )
      continue;

    // Schedule IV increment last.
    if (e->is_Mach() && e->as_Mach()->ideal_Opcode() == Op_CountedLoopEnd) {
      // Cmp might be matched into CountedLoopEnd node.
      Node *cmp = (e->in(1)->ideal_reg() == Op_RegFlags) ? e->in(1) : e;
      if (cmp->req() > 1 && cmp->in(1) == n && n->is_iteratively_computed()) {
        continue;
      }
    }

    uint n_choice  = 2;

    // See if this instruction is consumed by a branch. If so, then (as the
    // branch is the last instruction in the basic block) force it to the
    // end of the basic block
    if ( must_clone[iop] ) {
      // See if any use is a branch
      bool found_machif = false;

      for (DUIterator_Fast jmax, j = n->fast_outs(jmax); j < jmax; j++) {
        Node* use = n->fast_out(j);

        // The use is a conditional branch, make them adjacent
        if (use->is_MachIf() && get_block_for_node(use) == block) {
          found_machif = true;
          break;
        }

        // More than this instruction pending for successor to be ready,
        // don't choose this if other opportunities are ready
        if (ready_cnt.at(use->_idx) > 1)
          n_choice = 1;
      }

      // loop terminated, prefer not to use this instruction
      if (found_machif)
        continue;
    }

    // See if this has a predecessor that is "must_clone", i.e. sets the
    // condition code. If so, choose this first
    for (uint j = 0; j < n->req() ; j++) {
      Node *inn = n->in(j);
      if (inn) {
        if (inn->is_Mach() && must_clone[inn->as_Mach()->ideal_Opcode()] ) {
          n_choice = 3;
          break;
        }
      }
    }

    // MachTemps should be scheduled last so they are near their uses
    if (n->is_MachTemp()) {
      n_choice = 1;
    }

    uint n_latency = get_latency_for_node(n);
    uint n_score = n->req();   // Many inputs get high score to break ties

    if (OptoRegScheduling && block_size_threshold_ok) {
      if (recalc_pressure_nodes[n->_idx] == 0x7fff7fff) {
        _regalloc->_scratch_int_pressure.init(_regalloc->_sched_int_pressure.high_pressure_limit());
        _regalloc->_scratch_float_pressure.init(_regalloc->_sched_float_pressure.high_pressure_limit());
        // simulate the notion that we just picked this node to schedule
        n->add_flag(Node::Flag_is_scheduled);
        // now caculate its effect upon the graph if we did
        adjust_register_pressure(n, block, recalc_pressure_nodes, false);
        // return its state for finalize in case somebody else wins
        n->remove_flag(Node::Flag_is_scheduled);
        // now save the two final pressure components of register pressure, limiting pressure calcs to short size
        short int_pressure = (short)_regalloc->_scratch_int_pressure.current_pressure();
        short float_pressure = (short)_regalloc->_scratch_float_pressure.current_pressure();
        recalc_pressure_nodes[n->_idx] = int_pressure;
        recalc_pressure_nodes[n->_idx] |= (float_pressure << 16);
      }

      if (_scheduling_for_pressure) {
        latency = n_latency;
        if (n_choice != 3) {
          // Now evaluate each register pressure component based on threshold in the score.
          // In general the defining register type will dominate the score, ergo we will not see register pressure grow on both banks
          // on a single instruction, but we might see it shrink on both banks.
          // For each use of register that has a register class that is over the high pressure limit, we build n_score up for
          // live ranges that terminate on this instruction.
          if (_regalloc->_sched_int_pressure.current_pressure() > _regalloc->_sched_int_pressure.high_pressure_limit()) {
            short int_pressure = (short)recalc_pressure_nodes[n->_idx];
            n_score = (int_pressure < 0) ? ((score + n_score) - int_pressure) : (int_pressure > 0) ? 1 : n_score;
          }
          if (_regalloc->_sched_float_pressure.current_pressure() > _regalloc->_sched_float_pressure.high_pressure_limit()) {
            short float_pressure = (short)(recalc_pressure_nodes[n->_idx] >> 16);
            n_score = (float_pressure < 0) ? ((score + n_score) - float_pressure) : (float_pressure > 0) ? 1 : n_score;
          }
        } else {
          // make sure we choose these candidates
          score = 0;
        }
      }
    }

    // Keep best latency found
    cand_cnt++;
    if (choice < n_choice ||
        (choice == n_choice &&
         ((StressLCM && C->randomized_select(cand_cnt)) ||
          (!StressLCM &&
           (latency < n_latency ||
            (latency == n_latency &&
             (score < n_score))))))) {
      choice  = n_choice;
      latency = n_latency;
      score   = n_score;
      idx     = i;               // Also keep index in worklist
    }
  } // End of for all ready nodes in worklist

  guarantee(idx >= 0, "index should be set");
  Node *n = worklist[(uint)idx];      // Get the winner

  worklist.map((uint)idx, worklist.pop());     // Compress worklist
  return n;
}

//-------------------------adjust_register_pressure----------------------------
void PhaseCFG::adjust_register_pressure(Node* n, Block* block, intptr_t* recalc_pressure_nodes, bool finalize_mode) {
  PhaseLive* liveinfo = _regalloc->get_live();
  IndexSet* liveout = liveinfo->live(block);
  // first adjust the register pressure for the sources
  for (uint i = 1; i < n->req(); i++) {
    bool lrg_ends = false;
    Node *src_n = n->in(i);
    if (src_n == NULL) continue;
    if (!src_n->is_Mach()) continue;
    uint src = _regalloc->_lrg_map.find(src_n);
    if (src == 0) continue;
    LRG& lrg_src = _regalloc->lrgs(src);
    // detect if the live range ends or not
    if (liveout->member(src) == false) {
      lrg_ends = true;
      for (DUIterator_Fast jmax, j = src_n->fast_outs(jmax); j < jmax; j++) {
        Node* m = src_n->fast_out(j); // Get user
        if (m == n) continue;
        if (!m->is_Mach()) continue;
        MachNode *mach = m->as_Mach();
        bool src_matches = false;
        int iop = mach->ideal_Opcode();

        switch (iop) {
        case Op_StoreB:
        case Op_StoreC:
        case Op_StoreCM:
        case Op_StoreD:
        case Op_StoreF:
        case Op_StoreI:
        case Op_StoreL:
        case Op_StoreP:
        case Op_StoreN:
        case Op_StoreVector:
        case Op_StoreVectorScatter:
        case Op_StoreVectorMasked:
        case Op_StoreNKlass:
          for (uint k = 1; k < m->req(); k++) {
            Node *in = m->in(k);
            if (in == src_n) {
              src_matches = true;
              break;
            }
          }
          break;

        default:
          src_matches = true;
          break;
        }

        // If we have a store as our use, ignore the non source operands
        if (src_matches == false) continue;

        // Mark every unscheduled use which is not n with a recalculation
        if ((get_block_for_node(m) == block) && (!m->is_scheduled())) {
          if (finalize_mode && !m->is_Phi()) {
            recalc_pressure_nodes[m->_idx] = 0x7fff7fff;
          }
          lrg_ends = false;
        }
      }
    }
    // if none, this live range ends and we can adjust register pressure
    if (lrg_ends) {
      if (finalize_mode) {
        _regalloc->lower_pressure(block, 0, lrg_src, NULL, _regalloc->_sched_int_pressure, _regalloc->_sched_float_pressure);
      } else {
        _regalloc->lower_pressure(block, 0, lrg_src, NULL, _regalloc->_scratch_int_pressure, _regalloc->_scratch_float_pressure);
      }
    }
  }

  // now add the register pressure from the dest and evaluate which heuristic we should use:
  // 1.) The default, latency scheduling
  // 2.) Register pressure scheduling based on the high pressure limit threshold for int or float register stacks
  uint dst = _regalloc->_lrg_map.find(n);
  if (dst != 0) {
    LRG& lrg_dst = _regalloc->lrgs(dst);
    if (finalize_mode) {
      _regalloc->raise_pressure(block, lrg_dst, _regalloc->_sched_int_pressure, _regalloc->_sched_float_pressure);
      // check to see if we fall over the register pressure cliff here
      if (_regalloc->_sched_int_pressure.current_pressure() > _regalloc->_sched_int_pressure.high_pressure_limit()) {
        _scheduling_for_pressure = true;
      } else if (_regalloc->_sched_float_pressure.current_pressure() > _regalloc->_sched_float_pressure.high_pressure_limit()) {
        _scheduling_for_pressure = true;
      } else {
        // restore latency scheduling mode
        _scheduling_for_pressure = false;
      }
    } else {
      _regalloc->raise_pressure(block, lrg_dst, _regalloc->_scratch_int_pressure, _regalloc->_scratch_float_pressure);
    }
  }
}

//------------------------------set_next_call----------------------------------
void PhaseCFG::set_next_call(Block* block, Node* n, VectorSet& next_call) {
  if( next_call.test_set(n->_idx) ) return;
  for( uint i=0; i<n->len(); i++ ) {
    Node *m = n->in(i);
    if( !m ) continue;  // must see all nodes in block that precede call
    if (get_block_for_node(m) == block) {
      set_next_call(block, m, next_call);
    }
  }
}

//------------------------------needed_for_next_call---------------------------
// Set the flag 'next_call' for each Node that is needed for the next call to
// be scheduled.  This flag lets me bias scheduling so Nodes needed for the
// next subroutine call get priority - basically it moves things NOT needed
// for the next call till after the call.  This prevents me from trying to
// carry lots of stuff live across a call.
void PhaseCFG::needed_for_next_call(Block* block, Node* this_call, VectorSet& next_call) {
  // Find the next control-defining Node in this block
  Node* call = NULL;
  for (DUIterator_Fast imax, i = this_call->fast_outs(imax); i < imax; i++) {
    Node* m = this_call->fast_out(i);
    if (get_block_for_node(m) == block && // Local-block user
        m != this_call &&       // Not self-start node
        m->is_MachCall()) {
      call = m;
      break;
    }
  }
  if (call == NULL)  return;    // No next call (e.g., block end is near)
  // Set next-call for all inputs to this call
  set_next_call(block, call, next_call);
}

//------------------------------add_call_kills-------------------------------------
// helper function that adds caller save registers to MachProjNode
static void add_call_kills(MachProjNode *proj, RegMask& regs, const char* save_policy, bool exclude_soe) {
  // Fill in the kill mask for the call
  for( OptoReg::Name r = OptoReg::Name(0); r < _last_Mach_Reg; r=OptoReg::add(r,1) ) {
    if( !regs.Member(r) ) {     // Not already defined by the call
      // Save-on-call register?
      if ((save_policy[r] == 'C') ||
          (save_policy[r] == 'A') ||
          ((save_policy[r] == 'E') && exclude_soe)) {
        proj->_rout.Insert(r);
      }
    }
  }
}


//------------------------------sched_call-------------------------------------
uint PhaseCFG::sched_call(Block* block, uint node_cnt, Node_List& worklist, GrowableArray<int>& ready_cnt, MachCallNode* mcall, VectorSet& next_call) {
  RegMask regs;

  // Schedule all the users of the call right now.  All the users are
  // projection Nodes, so they must be scheduled next to the call.
  // Collect all the defined registers.
  for (DUIterator_Fast imax, i = mcall->fast_outs(imax); i < imax; i++) {
    Node* n = mcall->fast_out(i);
    assert( n->is_MachProj(), "" );
    int n_cnt = ready_cnt.at(n->_idx)-1;
    ready_cnt.at_put(n->_idx, n_cnt);
    assert( n_cnt == 0, "" );
    // Schedule next to call
    block->map_node(n, node_cnt++);
    // Collect defined registers
    regs.OR(n->out_RegMask());
    // Check for scheduling the next control-definer
    if( n->bottom_type() == Type::CONTROL )
      // Warm up next pile of heuristic bits
      needed_for_next_call(block, n, next_call);

    // Children of projections are now all ready
    for (DUIterator_Fast jmax, j = n->fast_outs(jmax); j < jmax; j++) {
      Node* m = n->fast_out(j); // Get user
      if(get_block_for_node(m) != block) {
        continue;
      }
      if( m->is_Phi() ) continue;
      int m_cnt = ready_cnt.at(m->_idx) - 1;
      ready_cnt.at_put(m->_idx, m_cnt);
      if( m_cnt == 0 )
        worklist.push(m);
    }

  }

  // Act as if the call defines the Frame Pointer.
  // Certainly the FP is alive and well after the call.
  regs.Insert(_matcher.c_frame_pointer());

  // Set all registers killed and not already defined by the call.
  uint r_cnt = mcall->tf()->range()->cnt();
  int op = mcall->ideal_Opcode();
  MachProjNode *proj = new MachProjNode( mcall, r_cnt+1, RegMask::Empty, MachProjNode::fat_proj );
  map_node_to_block(proj, block);
  block->insert_node(proj, node_cnt++);

  // Select the right register save policy.
  const char *save_policy = NULL;
  switch (op) {
    case Op_CallRuntime:
    case Op_CallLeaf:
    case Op_CallLeafNoFP:
    case Op_CallLeafVector:
      // Calling C code so use C calling convention
      save_policy = _matcher._c_reg_save_policy;
      break;

    case Op_CallStaticJava:
    case Op_CallDynamicJava:
      // Calling Java code so use Java calling convention
      save_policy = _matcher._register_save_policy;
      break;
    case Op_CallNative:
      // We use the c reg save policy here since Foreign Linker
      // only supports the C ABI currently.
      // TODO compute actual save policy based on nep->abi
      save_policy = _matcher._c_reg_save_policy;
      break;

    default:
      ShouldNotReachHere();
  }

  // When using CallRuntime mark SOE registers as killed by the call
  // so values that could show up in the RegisterMap aren't live in a
  // callee saved register since the register wouldn't know where to
  // find them.  CallLeaf and CallLeafNoFP are ok because they can't
  // have debug info on them.  Strictly speaking this only needs to be
  // done for oops since idealreg2debugmask takes care of debug info
  // references but there no way to handle oops differently than other
  // pointers as far as the kill mask goes.
  //
  // Also, native callees can not save oops, so we kill the SOE registers
  // here in case a native call has a safepoint. This doesn't work for
  // RBP though, which seems to be special-cased elsewhere to always be
  // treated as alive, so we instead manually save the location of RBP
  // before doing the native call (see NativeInvokerGenerator::generate).
  bool exclude_soe = op == Op_CallRuntime
    || (op == Op_CallNative && mcall->guaranteed_safepoint());

  // If the call is a MethodHandle invoke, we need to exclude the
  // register which is used to save the SP value over MH invokes from
  // the mask.  Otherwise this register could be used for
  // deoptimization information.
  if (op == Op_CallStaticJava) {
    MachCallStaticJavaNode* mcallstaticjava = (MachCallStaticJavaNode*) mcall;
    if (mcallstaticjava->_method_handle_invoke)
      proj->_rout.OR(Matcher::method_handle_invoke_SP_save_mask());
  }

  add_call_kills(proj, regs, save_policy, exclude_soe);

  return node_cnt;
}


//------------------------------schedule_local---------------------------------
// Topological sort within a block.  Someday become a real scheduler.
bool PhaseCFG::schedule_local(Block* block, GrowableArray<int>& ready_cnt, VectorSet& next_call, intptr_t *recalc_pressure_nodes) {
  // Already "sorted" are the block start Node (as the first entry), and
  // the block-ending Node and any trailing control projections.  We leave
  // these alone.  PhiNodes and ParmNodes are made to follow the block start
  // Node.  Everything else gets topo-sorted.

#ifndef PRODUCT
    if (trace_opto_pipelining()) {
      tty->print_cr("# --- schedule_local B%d, before: ---", block->_pre_order);
      for (uint i = 0;i < block->number_of_nodes(); i++) {
        tty->print("# ");
        block->get_node(i)->fast_dump();
      }
      tty->print_cr("#");
    }
#endif

  // RootNode is already sorted
  if (block->number_of_nodes() == 1) {
    return true;
  }

  bool block_size_threshold_ok = (block->number_of_nodes() > 10) ? true : false;

  // We track the uses of local definitions as input dependences so that
  // we know when a given instruction is avialable to be scheduled.
  uint i;
  if (OptoRegScheduling && block_size_threshold_ok) {
    for (i = 1; i < block->number_of_nodes(); i++) { // setup nodes for pressure calc
      Node *n = block->get_node(i);
      n->remove_flag(Node::Flag_is_scheduled);
      if (!n->is_Phi()) {
        recalc_pressure_nodes[n->_idx] = 0x7fff7fff;
      }
    }
  }

  // Move PhiNodes and ParmNodes from 1 to cnt up to the start
  uint node_cnt = block->end_idx();
  uint phi_cnt = 1;
  for( i = 1; i<node_cnt; i++ ) { // Scan for Phi
    Node *n = block->get_node(i);
    if( n->is_Phi() ||          // Found a PhiNode or ParmNode
        (n->is_Proj()  && n->in(0) == block->head()) ) {
      // Move guy at 'phi_cnt' to the end; makes a hole at phi_cnt
      block->map_node(block->get_node(phi_cnt), i);
      block->map_node(n, phi_cnt++);  // swap Phi/Parm up front
      if (OptoRegScheduling && block_size_threshold_ok) {
        // mark n as scheduled
        n->add_flag(Node::Flag_is_scheduled);
      }
    } else {                    // All others
      // Count block-local inputs to 'n'
      uint cnt = n->len();      // Input count
      uint local = 0;
      for( uint j=0; j<cnt; j++ ) {
        Node *m = n->in(j);
        if( m && get_block_for_node(m) == block && !m->is_top() )
          local++;              // One more block-local input
      }
      ready_cnt.at_put(n->_idx, local); // Count em up

#ifdef ASSERT
      if (UseG1GC) {
        if( n->is_Mach() && n->as_Mach()->ideal_Opcode() == Op_StoreCM ) {
          // Check the precedence edges
          for (uint prec = n->req(); prec < n->len(); prec++) {
            Node* oop_store = n->in(prec);
            if (oop_store != NULL) {
              assert(get_block_for_node(oop_store)->_dom_depth <= block->_dom_depth, "oop_store must dominate card-mark");
            }
          }
        }
      }
#endif

      // A few node types require changing a required edge to a precedence edge
      // before allocation.
      if( n->is_Mach() && n->req() > TypeFunc::Parms &&
          (n->as_Mach()->ideal_Opcode() == Op_MemBarAcquire ||
           n->as_Mach()->ideal_Opcode() == Op_MemBarVolatile) ) {
        // MemBarAcquire could be created without Precedent edge.
        // del_req() replaces the specified edge with the last input edge
        // and then removes the last edge. If the specified edge > number of
        // edges the last edge will be moved outside of the input edges array
        // and the edge will be lost. This is why this code should be
        // executed only when Precedent (== TypeFunc::Parms) edge is present.
        Node *x = n->in(TypeFunc::Parms);
        if (x != NULL && get_block_for_node(x) == block && n->find_prec_edge(x) != -1) {
          // Old edge to node within same block will get removed, but no precedence
          // edge will get added because it already exists. Update ready count.
          int cnt = ready_cnt.at(n->_idx);
          assert(cnt > 1, "MemBar node %d must not get ready here", n->_idx);
          ready_cnt.at_put(n->_idx, cnt-1);
        }
        n->del_req(TypeFunc::Parms);
        n->add_prec(x);
      }
    }
  }
  for(uint i2=i; i2< block->number_of_nodes(); i2++ ) // Trailing guys get zapped count
    ready_cnt.at_put(block->get_node(i2)->_idx, 0);

  // All the prescheduled guys do not hold back internal nodes
  uint i3;
  for (i3 = 0; i3 < phi_cnt; i3++) {  // For all pre-scheduled
    Node *n = block->get_node(i3);       // Get pre-scheduled
    for (DUIterator_Fast jmax, j = n->fast_outs(jmax); j < jmax; j++) {
      Node* m = n->fast_out(j);
      if (get_block_for_node(m) == block) { // Local-block user
        int m_cnt = ready_cnt.at(m->_idx)-1;
        if (OptoRegScheduling && block_size_threshold_ok) {
          // mark m as scheduled
          if (m_cnt < 0) {
            m->add_flag(Node::Flag_is_scheduled);
          }
        }
        ready_cnt.at_put(m->_idx, m_cnt);   // Fix ready count
      }
    }
  }

  Node_List delay;
  // Make a worklist
  Node_List worklist;
  for(uint i4=i3; i4<node_cnt; i4++ ) {    // Put ready guys on worklist
    Node *m = block->get_node(i4);
    if( !ready_cnt.at(m->_idx) ) {   // Zero ready count?
      if (m->is_iteratively_computed()) {
        // Push induction variable increments last to allow other uses
        // of the phi to be scheduled first. The select() method breaks
        // ties in scheduling by worklist order.
        delay.push(m);
      } else if (m->is_Mach() && m->as_Mach()->ideal_Opcode() == Op_CreateEx) {
        // Force the CreateEx to the top of the list so it's processed
        // first and ends up at the start of the block.
        worklist.insert(0, m);
      } else {
        worklist.push(m);         // Then on to worklist!
      }
    }
  }
  while (delay.size()) {
    Node* d = delay.pop();
    worklist.push(d);
  }

  if (OptoRegScheduling && block_size_threshold_ok) {
    // To stage register pressure calculations we need to examine the live set variables
    // breaking them up by register class to compartmentalize the calculations.
    _regalloc->_sched_int_pressure.init(Matcher::int_pressure_limit());
    _regalloc->_sched_float_pressure.init(Matcher::float_pressure_limit());
    _regalloc->_scratch_int_pressure.init(Matcher::int_pressure_limit());
    _regalloc->_scratch_float_pressure.init(Matcher::float_pressure_limit());

    _regalloc->compute_entry_block_pressure(block);
  }

  // Warm up the 'next_call' heuristic bits
  needed_for_next_call(block, block->head(), next_call);

#ifndef PRODUCT
    if (trace_opto_pipelining()) {
      for (uint j=0; j< block->number_of_nodes(); j++) {
        Node     *n = block->get_node(j);
        int     idx = n->_idx;
        tty->print("#   ready cnt:%3d  ", ready_cnt.at(idx));
        tty->print("latency:%3d  ", get_latency_for_node(n));
        tty->print("%4d: %s\n", idx, n->Name());
      }
    }
#endif

  uint max_idx = (uint)ready_cnt.length();
  // Pull from worklist and schedule
  while( worklist.size() ) {    // Worklist is not ready

#ifndef PRODUCT
    if (trace_opto_pipelining()) {
      tty->print("#   ready list:");
      for( uint i=0; i<worklist.size(); i++ ) { // Inspect entire worklist
        Node *n = worklist[i];      // Get Node on worklist
        tty->print(" %d", n->_idx);
      }
      tty->cr();
    }
#endif

    // Select and pop a ready guy from worklist
    Node* n = select(block, worklist, ready_cnt, next_call, phi_cnt, recalc_pressure_nodes);
    block->map_node(n, phi_cnt++);    // Schedule him next

    if (OptoRegScheduling && block_size_threshold_ok) {
      n->add_flag(Node::Flag_is_scheduled);

      // Now adjust the resister pressure with the node we selected
      if (!n->is_Phi()) {
        adjust_register_pressure(n, block, recalc_pressure_nodes, true);
      }
    }

#ifndef PRODUCT
    if (trace_opto_pipelining()) {
      tty->print("#    select %d: %s", n->_idx, n->Name());
      tty->print(", latency:%d", get_latency_for_node(n));
      n->dump();
      if (Verbose) {
        tty->print("#   ready list:");
        for( uint i=0; i<worklist.size(); i++ ) { // Inspect entire worklist
          Node *n = worklist[i];      // Get Node on worklist
          tty->print(" %d", n->_idx);
        }
        tty->cr();
      }
    }

#endif
    if( n->is_MachCall() ) {
      MachCallNode *mcall = n->as_MachCall();
      phi_cnt = sched_call(block, phi_cnt, worklist, ready_cnt, mcall, next_call);
      continue;
    }

    if (n->is_Mach() && n->as_Mach()->has_call()) {
      RegMask regs;
      regs.Insert(_matcher.c_frame_pointer());
      regs.OR(n->out_RegMask());

      MachProjNode *proj = new MachProjNode( n, 1, RegMask::Empty, MachProjNode::fat_proj );
      map_node_to_block(proj, block);
      block->insert_node(proj, phi_cnt++);

      add_call_kills(proj, regs, _matcher._c_reg_save_policy, false);
    }

    // Children are now all ready
    for (DUIterator_Fast i5max, i5 = n->fast_outs(i5max); i5 < i5max; i5++) {
      Node* m = n->fast_out(i5); // Get user
      if (get_block_for_node(m) != block) {
        continue;
      }
      if( m->is_Phi() ) continue;
      if (m->_idx >= max_idx) { // new node, skip it
        assert(m->is_MachProj() && n->is_Mach() && n->as_Mach()->has_call(), "unexpected node types");
        continue;
      }
      int m_cnt = ready_cnt.at(m->_idx) - 1;
      ready_cnt.at_put(m->_idx, m_cnt);
      if( m_cnt == 0 )
        worklist.push(m);
    }
  }

  if( phi_cnt != block->end_idx() ) {
    // did not schedule all.  Retry, Bailout, or Die
    if (C->subsume_loads() == true && !C->failing()) {
      // Retry with subsume_loads == false
      // If this is the first failure, the sentinel string will "stick"
      // to the Compile object, and the C2Compiler will see it and retry.
      C->record_failure(C2Compiler::retry_no_subsuming_loads());
    } else {
      assert(false, "graph should be schedulable");
    }
    // assert( phi_cnt == end_idx(), "did not schedule all" );
    return false;
  }

  if (OptoRegScheduling && block_size_threshold_ok) {
    _regalloc->compute_exit_block_pressure(block);
    block->_reg_pressure = _regalloc->_sched_int_pressure.final_pressure();
    block->_freg_pressure = _regalloc->_sched_float_pressure.final_pressure();
  }

#ifndef PRODUCT
  if (trace_opto_pipelining()) {
    tty->print_cr("#");
    tty->print_cr("# after schedule_local");
    for (uint i = 0;i < block->number_of_nodes();i++) {
      tty->print("# ");
      block->get_node(i)->fast_dump();
    }
    tty->print_cr("# ");

    if (OptoRegScheduling && block_size_threshold_ok) {
      tty->print_cr("# pressure info : %d", block->_pre_order);
      _regalloc->print_pressure_info(_regalloc->_sched_int_pressure, "int register info");
      _regalloc->print_pressure_info(_regalloc->_sched_float_pressure, "float register info");
    }
    tty->cr();
  }
#endif

  return true;
}

//--------------------------catch_cleanup_fix_all_inputs-----------------------
static void catch_cleanup_fix_all_inputs(Node *use, Node *old_def, Node *new_def) {
  for (uint l = 0; l < use->len(); l++) {
    if (use->in(l) == old_def) {
      if (l < use->req()) {
        use->set_req(l, new_def);
      } else {
        use->rm_prec(l);
        use->add_prec(new_def);
        l--;
      }
    }
  }
}

//------------------------------catch_cleanup_find_cloned_def------------------
Node* PhaseCFG::catch_cleanup_find_cloned_def(Block *use_blk, Node *def, Block *def_blk, int n_clone_idx) {
  assert( use_blk != def_blk, "Inter-block cleanup only");

  // The use is some block below the Catch.  Find and return the clone of the def
  // that dominates the use. If there is no clone in a dominating block, then
  // create a phi for the def in a dominating block.

  // Find which successor block dominates this use.  The successor
  // blocks must all be single-entry (from the Catch only; I will have
  // split blocks to make this so), hence they all dominate.
  while( use_blk->_dom_depth > def_blk->_dom_depth+1 )
    use_blk = use_blk->_idom;

  // Find the successor
  Node *fixup = NULL;

  uint j;
  for( j = 0; j < def_blk->_num_succs; j++ )
    if( use_blk == def_blk->_succs[j] )
      break;

  if( j == def_blk->_num_succs ) {
    // Block at same level in dom-tree is not a successor.  It needs a
    // PhiNode, the PhiNode uses from the def and IT's uses need fixup.
    Node_Array inputs = new Node_List();
    for(uint k = 1; k < use_blk->num_preds(); k++) {
      Block* block = get_block_for_node(use_blk->pred(k));
      inputs.map(k, catch_cleanup_find_cloned_def(block, def, def_blk, n_clone_idx));
    }

    // Check to see if the use_blk already has an identical phi inserted.
    // If it exists, it will be at the first position since all uses of a
    // def are processed together.
    Node *phi = use_blk->get_node(1);
    if( phi->is_Phi() ) {
      fixup = phi;
      for (uint k = 1; k < use_blk->num_preds(); k++) {
        if (phi->in(k) != inputs[k]) {
          // Not a match
          fixup = NULL;
          break;
        }
      }
    }

    // If an existing PhiNode was not found, make a new one.
    if (fixup == NULL) {
      Node *new_phi = PhiNode::make(use_blk->head(), def);
      use_blk->insert_node(new_phi, 1);
      map_node_to_block(new_phi, use_blk);
      for (uint k = 1; k < use_blk->num_preds(); k++) {
        new_phi->set_req(k, inputs[k]);
      }
      fixup = new_phi;
    }

  } else {
    // Found the use just below the Catch.  Make it use the clone.
    fixup = use_blk->get_node(n_clone_idx);
  }

  return fixup;
}

//--------------------------catch_cleanup_intra_block--------------------------
// Fix all input edges in use that reference "def".  The use is in the same
// block as the def and both have been cloned in each successor block.
static void catch_cleanup_intra_block(Node *use, Node *def, Block *blk, int beg, int n_clone_idx) {

  // Both the use and def have been cloned. For each successor block,
  // get the clone of the use, and make its input the clone of the def
  // found in that block.

  uint use_idx = blk->find_node(use);
  uint offset_idx = use_idx - beg;
  for( uint k = 0; k < blk->_num_succs; k++ ) {
    // Get clone in each successor block
    Block *sb = blk->_succs[k];
    Node *clone = sb->get_node(offset_idx+1);
    assert( clone->Opcode() == use->Opcode(), "" );

    // Make use-clone reference the def-clone
    catch_cleanup_fix_all_inputs(clone, def, sb->get_node(n_clone_idx));
  }
}

//------------------------------catch_cleanup_inter_block---------------------
// Fix all input edges in use that reference "def".  The use is in a different
// block than the def.
void PhaseCFG::catch_cleanup_inter_block(Node *use, Block *use_blk, Node *def, Block *def_blk, int n_clone_idx) {
  if( !use_blk ) return;        // Can happen if the use is a precedence edge

  Node *new_def = catch_cleanup_find_cloned_def(use_blk, def, def_blk, n_clone_idx);
  catch_cleanup_fix_all_inputs(use, def, new_def);
}

//------------------------------call_catch_cleanup-----------------------------
// If we inserted any instructions between a Call and his CatchNode,
// clone the instructions on all paths below the Catch.
void PhaseCFG::call_catch_cleanup(Block* block) {

  // End of region to clone
  uint end = block->end_idx();
  if( !block->get_node(end)->is_Catch() ) return;
  // Start of region to clone
  uint beg = end;
  while(!block->get_node(beg-1)->is_MachProj() ||
        !block->get_node(beg-1)->in(0)->is_MachCall() ) {
    beg--;
    assert(beg > 0,"Catch cleanup walking beyond block boundary");
  }
  // Range of inserted instructions is [beg, end)
  if( beg == end ) return;

  // Clone along all Catch output paths.  Clone area between the 'beg' and
  // 'end' indices.
  for( uint i = 0; i < block->_num_succs; i++ ) {
    Block *sb = block->_succs[i];
    // Clone the entire area; ignoring the edge fixup for now.
    for( uint j = end; j > beg; j-- ) {
      Node *clone = block->get_node(j-1)->clone();
      sb->insert_node(clone, 1);
      map_node_to_block(clone, sb);
      if (clone->needs_anti_dependence_check()) {
        insert_anti_dependences(sb, clone);
      }
    }
  }


  // Fixup edges.  Check the def-use info per cloned Node
  for(uint i2 = beg; i2 < end; i2++ ) {
    uint n_clone_idx = i2-beg+1; // Index of clone of n in each successor block
    Node *n = block->get_node(i2);        // Node that got cloned
    // Need DU safe iterator because of edge manipulation in calls.
    Unique_Node_List* out = new Unique_Node_List();
    for (DUIterator_Fast j1max, j1 = n->fast_outs(j1max); j1 < j1max; j1++) {
      out->push(n->fast_out(j1));
    }
    uint max = out->size();
    for (uint j = 0; j < max; j++) {// For all users
      Node *use = out->pop();
      Block *buse = get_block_for_node(use);
      if( use->is_Phi() ) {
        for( uint k = 1; k < use->req(); k++ )
          if( use->in(k) == n ) {
            Block* b = get_block_for_node(buse->pred(k));
            Node *fixup = catch_cleanup_find_cloned_def(b, n, block, n_clone_idx);
            use->set_req(k, fixup);
          }
      } else {
        if (block == buse) {
          catch_cleanup_intra_block(use, n, block, beg, n_clone_idx);
        } else {
          catch_cleanup_inter_block(use, buse, n, block, n_clone_idx);
        }
      }
    } // End for all users

  } // End of for all Nodes in cloned area

  // Remove the now-dead cloned ops
  for(uint i3 = beg; i3 < end; i3++ ) {
    block->get_node(beg)->disconnect_inputs(C);
    block->remove_node(beg);
  }

  // If the successor blocks have a CreateEx node, move it back to the top
  for (uint i4 = 0; i4 < block->_num_succs; i4++) {
    Block *sb = block->_succs[i4];
    uint new_cnt = end - beg;
    // Remove any newly created, but dead, nodes by traversing their schedule
    // backwards. Here, a dead node is a node whose only outputs (if any) are
    // unused projections.
    for (uint j = new_cnt; j > 0; j--) {
      Node *n = sb->get_node(j);
      // Individual projections are examined together with all siblings when
      // their parent is visited.
      if (n->is_Proj()) {
        continue;
      }
      bool dead = true;
      for (DUIterator_Fast imax, i = n->fast_outs(imax); i < imax; i++) {
        Node* out = n->fast_out(i);
        // n is live if it has a non-projection output or a used projection.
        if (!out->is_Proj() || out->outcnt() > 0) {
          dead = false;
          break;
        }
      }
      if (dead) {
        // n's only outputs (if any) are unused projections scheduled next to n
        // (see PhaseCFG::select()). Remove these projections backwards.
        for (uint k = j + n->outcnt(); k > j; k--) {
          Node* proj = sb->get_node(k);
          assert(proj->is_Proj() && proj->in(0) == n,
                 "projection should correspond to dead node");
          proj->disconnect_inputs(C);
          sb->remove_node(k);
          new_cnt--;
        }
        // Now remove the node itself.
        n->disconnect_inputs(C);
        sb->remove_node(j);
        new_cnt--;
      }
    }
    // If any newly created nodes remain, move the CreateEx node to the top
    if (new_cnt > 0) {
      Node *cex = sb->get_node(1+new_cnt);
      if( cex->is_Mach() && cex->as_Mach()->ideal_Opcode() == Op_CreateEx ) {
        sb->remove_node(1+new_cnt);
        sb->insert_node(cex, 1);
      }
    }
  }
}
