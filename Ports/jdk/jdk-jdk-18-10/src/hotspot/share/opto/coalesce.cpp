/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.inline.hpp"
#include "opto/block.hpp"
#include "opto/c2compiler.hpp"
#include "opto/cfgnode.hpp"
#include "opto/chaitin.hpp"
#include "opto/coalesce.hpp"
#include "opto/connode.hpp"
#include "opto/indexSet.hpp"
#include "opto/machnode.hpp"
#include "opto/matcher.hpp"
#include "opto/regmask.hpp"

#ifndef PRODUCT
void PhaseCoalesce::dump(Node *n) const {
  // Being a const function means I cannot use 'Find'
  uint r = _phc._lrg_map.find(n);
  tty->print("L%d/N%d ",r,n->_idx);
}

void PhaseCoalesce::dump() const {
  // I know I have a block layout now, so I can print blocks in a loop
  for( uint i=0; i<_phc._cfg.number_of_blocks(); i++ ) {
    uint j;
    Block* b = _phc._cfg.get_block(i);
    // Print a nice block header
    tty->print("B%d: ",b->_pre_order);
    for( j=1; j<b->num_preds(); j++ )
      tty->print("B%d ", _phc._cfg.get_block_for_node(b->pred(j))->_pre_order);
    tty->print("-> ");
    for( j=0; j<b->_num_succs; j++ )
      tty->print("B%d ",b->_succs[j]->_pre_order);
    tty->print(" IDom: B%d/#%d\n", b->_idom ? b->_idom->_pre_order : 0, b->_dom_depth);
    uint cnt = b->number_of_nodes();
    for( j=0; j<cnt; j++ ) {
      Node *n = b->get_node(j);
      dump( n );
      tty->print("\t%s\t",n->Name());

      // Dump the inputs
      uint k;                   // Exit value of loop
      for( k=0; k<n->req(); k++ ) // For all required inputs
        if( n->in(k) ) dump( n->in(k) );
        else tty->print("_ ");
      int any_prec = 0;
      for( ; k<n->len(); k++ )          // For all precedence inputs
        if( n->in(k) ) {
          if( !any_prec++ ) tty->print(" |");
          dump( n->in(k) );
        }

      // Dump node-specific info
      n->dump_spec(tty);
      tty->print("\n");

    }
    tty->print("\n");
  }
}
#endif

// Combine the live ranges def'd by these 2 Nodes.  N2 is an input to N1.
void PhaseCoalesce::combine_these_two(Node *n1, Node *n2) {
  uint lr1 = _phc._lrg_map.find(n1);
  uint lr2 = _phc._lrg_map.find(n2);
  if( lr1 != lr2 &&             // Different live ranges already AND
      !_phc._ifg->test_edge_sq( lr1, lr2 ) ) {  // Do not interfere
    LRG *lrg1 = &_phc.lrgs(lr1);
    LRG *lrg2 = &_phc.lrgs(lr2);
    // Not an oop->int cast; oop->oop, int->int, AND int->oop are OK.

    // Now, why is int->oop OK?  We end up declaring a raw-pointer as an oop
    // and in general that's a bad thing.  However, int->oop conversions only
    // happen at GC points, so the lifetime of the misclassified raw-pointer
    // is from the CheckCastPP (that converts it to an oop) backwards up
    // through a merge point and into the slow-path call, and around the
    // diamond up to the heap-top check and back down into the slow-path call.
    // The misclassified raw pointer is NOT live across the slow-path call,
    // and so does not appear in any GC info, so the fact that it is
    // misclassified is OK.

    if( (lrg1->_is_oop || !lrg2->_is_oop) && // not an oop->int cast AND
        // Compatible final mask
        lrg1->mask().overlap( lrg2->mask() ) ) {
      // Merge larger into smaller.
      if( lr1 > lr2 ) {
        uint  tmp =  lr1;  lr1 =  lr2;  lr2 =  tmp;
        Node   *n =   n1;   n1 =   n2;   n2 =    n;
        LRG *ltmp = lrg1; lrg1 = lrg2; lrg2 = ltmp;
      }
      // Union lr2 into lr1
      _phc.Union( n1, n2 );
      if (lrg1->_maxfreq < lrg2->_maxfreq)
        lrg1->_maxfreq = lrg2->_maxfreq;
      // Merge in the IFG
      _phc._ifg->Union( lr1, lr2 );
      // Combine register restrictions
      lrg1->AND(lrg2->mask());
    }
  }
}

// Copy coalescing
void PhaseCoalesce::coalesce_driver() {
  verify();
  // Coalesce from high frequency to low
  for (uint i = 0; i < _phc._cfg.number_of_blocks(); i++) {
    coalesce(_phc._blks[i]);
  }
}

// I am inserting copies to come out of SSA form.  In the general case, I am
// doing a parallel renaming.  I'm in the Named world now, so I can't do a
// general parallel renaming.  All the copies now use  "names" (live-ranges)
// to carry values instead of the explicit use-def chains.  Suppose I need to
// insert 2 copies into the same block.  They copy L161->L128 and L128->L132.
// If I insert them in the wrong order then L128 will get clobbered before it
// can get used by the second copy.  This cannot happen in the SSA model;
// direct use-def chains get me the right value.  It DOES happen in the named
// model so I have to handle the reordering of copies.
//
// In general, I need to topo-sort the placed copies to avoid conflicts.
// Its possible to have a closed cycle of copies (e.g., recirculating the same
// values around a loop).  In this case I need a temp to break the cycle.
void PhaseAggressiveCoalesce::insert_copy_with_overlap( Block *b, Node *copy, uint dst_name, uint src_name ) {

  // Scan backwards for the locations of the last use of the dst_name.
  // I am about to clobber the dst_name, so the copy must be inserted
  // after the last use.  Last use is really first-use on a backwards scan.
  uint i = b->end_idx()-1;
  while(1) {
    Node *n = b->get_node(i);
    // Check for end of virtual copies; this is also the end of the
    // parallel renaming effort.
    if (n->_idx < _unique) {
      break;
    }
    uint idx = n->is_Copy();
    assert( idx || n->is_Con() || n->is_MachProj(), "Only copies during parallel renaming" );
    if (idx && _phc._lrg_map.find(n->in(idx)) == dst_name) {
      break;
    }
    i--;
  }
  uint last_use_idx = i;

  // Also search for any kill of src_name that exits the block.
  // Since the copy uses src_name, I have to come before any kill.
  uint kill_src_idx = b->end_idx();
  // There can be only 1 kill that exits any block and that is
  // the last kill.  Thus it is the first kill on a backwards scan.
  i = b->end_idx()-1;
  while (1) {
    Node *n = b->get_node(i);
    // Check for end of virtual copies; this is also the end of the
    // parallel renaming effort.
    if (n->_idx < _unique) {
      break;
    }
    assert( n->is_Copy() || n->is_Con() || n->is_MachProj(), "Only copies during parallel renaming" );
    if (_phc._lrg_map.find(n) == src_name) {
      kill_src_idx = i;
      break;
    }
    i--;
  }
  // Need a temp?  Last use of dst comes after the kill of src?
  if (last_use_idx >= kill_src_idx) {
    // Need to break a cycle with a temp
    uint idx = copy->is_Copy();
    Node *tmp = copy->clone();
    uint max_lrg_id = _phc._lrg_map.max_lrg_id();
    _phc.new_lrg(tmp, max_lrg_id);
    _phc._lrg_map.set_max_lrg_id(max_lrg_id + 1);

    // Insert new temp between copy and source
    tmp ->set_req(idx,copy->in(idx));
    copy->set_req(idx,tmp);
    // Save source in temp early, before source is killed
    b->insert_node(tmp, kill_src_idx);
    _phc._cfg.map_node_to_block(tmp, b);
    last_use_idx++;
  }

  // Insert just after last use
  b->insert_node(copy, last_use_idx + 1);
}

void PhaseAggressiveCoalesce::insert_copies( Matcher &matcher ) {
  // We do LRGs compressing and fix a liveout data only here since the other
  // place in Split() is guarded by the assert which we never hit.
  _phc._lrg_map.compress_uf_map_for_nodes();
  // Fix block's liveout data for compressed live ranges.
  for (uint lrg = 1; lrg < _phc._lrg_map.max_lrg_id(); lrg++) {
    uint compressed_lrg = _phc._lrg_map.find(lrg);
    if (lrg != compressed_lrg) {
      for (uint bidx = 0; bidx < _phc._cfg.number_of_blocks(); bidx++) {
        IndexSet *liveout = _phc._live->live(_phc._cfg.get_block(bidx));
        if (liveout->member(lrg)) {
          liveout->remove(lrg);
          liveout->insert(compressed_lrg);
        }
      }
    }
  }

  // All new nodes added are actual copies to replace virtual copies.
  // Nodes with index less than '_unique' are original, non-virtual Nodes.
  _unique = C->unique();

  for (uint i = 0; i < _phc._cfg.number_of_blocks(); i++) {
    C->check_node_count(NodeLimitFudgeFactor, "out of nodes in coalesce");
    if (C->failing()) return;
    Block *b = _phc._cfg.get_block(i);
    uint cnt = b->num_preds();  // Number of inputs to the Phi

    for( uint l = 1; l<b->number_of_nodes(); l++ ) {
      Node *n = b->get_node(l);

      // Do not use removed-copies, use copied value instead
      uint ncnt = n->req();
      for( uint k = 1; k<ncnt; k++ ) {
        Node *copy = n->in(k);
        uint cidx = copy->is_Copy();
        if( cidx ) {
          Node *def = copy->in(cidx);
          if (_phc._lrg_map.find(copy) == _phc._lrg_map.find(def)) {
            n->set_req(k, def);
          }
        }
      }

      // Remove any explicit copies that get coalesced.
      uint cidx = n->is_Copy();
      if( cidx ) {
        Node *def = n->in(cidx);
        if (_phc._lrg_map.find(n) == _phc._lrg_map.find(def)) {
          n->replace_by(def);
          n->set_req(cidx,NULL);
          b->remove_node(l);
          l--;
          continue;
        }
      }

      if (n->is_Phi()) {
        // Get the chosen name for the Phi
        uint phi_name = _phc._lrg_map.find(n);
        // Ignore the pre-allocated specials
        if (!phi_name) {
          continue;
        }
        // Check for mismatch inputs to Phi
        for (uint j = 1; j < cnt; j++) {
          Node *m = n->in(j);
          uint src_name = _phc._lrg_map.find(m);
          if (src_name != phi_name) {
            Block *pred = _phc._cfg.get_block_for_node(b->pred(j));
            Node *copy;
            assert(!m->is_Con() || m->is_Mach(), "all Con must be Mach");
            // Rematerialize constants instead of copying them.
            // We do this only for immediate constants, we avoid constant table loads
            // because that will unsafely extend the live range of the constant table base.
            if (m->is_Mach() && m->as_Mach()->is_Con() && !m->as_Mach()->is_MachConstant() &&
                m->as_Mach()->rematerialize()) {
              copy = m->clone();
              // Insert the copy in the predecessor basic block
              pred->add_inst(copy);
              // Copy any flags as well
              _phc.clone_projs(pred, pred->end_idx(), m, copy, _phc._lrg_map);
            } else {
              uint ireg = m->ideal_reg();
              if (ireg == 0 || ireg == Op_RegFlags) {
                if (C->subsume_loads()) {
                  C->record_failure(C2Compiler::retry_no_subsuming_loads());
                } else {
                  assert(false, "attempted to spill a non-spillable item: %d: %s, ireg = %u, spill_type: %s",
                         m->_idx, m->Name(), ireg, MachSpillCopyNode::spill_type(MachSpillCopyNode::PhiInput));
                  C->record_method_not_compilable("attempted to spill a non-spillable item");
                }
                return;
              }
              const RegMask *rm = C->matcher()->idealreg2spillmask[ireg];
              copy = new MachSpillCopyNode(MachSpillCopyNode::PhiInput, m, *rm, *rm);
              // Find a good place to insert.  Kinda tricky, use a subroutine
              insert_copy_with_overlap(pred,copy,phi_name,src_name);
            }
            // Insert the copy in the use-def chain
            n->set_req(j, copy);
            _phc._cfg.map_node_to_block(copy, pred);
            // Extend ("register allocate") the names array for the copy.
            _phc._lrg_map.extend(copy->_idx, phi_name);
          } // End of if Phi names do not match
        } // End of for all inputs to Phi
      } else { // End of if Phi

        // Now check for 2-address instructions
        uint idx;
        if( n->is_Mach() && (idx=n->as_Mach()->two_adr()) ) {
          // Get the chosen name for the Node
          uint name = _phc._lrg_map.find(n);
          assert (name, "no 2-address specials");
          // Check for name mis-match on the 2-address input
          Node *m = n->in(idx);
          if (_phc._lrg_map.find(m) != name) {
            Node *copy;
            assert(!m->is_Con() || m->is_Mach(), "all Con must be Mach");
            // At this point it is unsafe to extend live ranges (6550579).
            // Rematerialize only constants as we do for Phi above.
            if (m->is_Mach() && m->as_Mach()->is_Con() && !m->as_Mach()->is_MachConstant() &&
                m->as_Mach()->rematerialize()) {
              copy = m->clone();
              // Insert the copy in the basic block, just before us
              b->insert_node(copy, l++);
              l += _phc.clone_projs(b, l, m, copy, _phc._lrg_map);
            } else {
              uint ireg = m->ideal_reg();
              if (ireg == 0 || ireg == Op_RegFlags) {
                assert(false, "attempted to spill a non-spillable item: %d: %s, ireg = %u, spill_type: %s",
                       m->_idx, m->Name(), ireg, MachSpillCopyNode::spill_type(MachSpillCopyNode::TwoAddress));
                C->record_method_not_compilable("attempted to spill a non-spillable item");
                return;
              }
              const RegMask *rm = C->matcher()->idealreg2spillmask[ireg];
              copy = new MachSpillCopyNode(MachSpillCopyNode::TwoAddress, m, *rm, *rm);
              // Insert the copy in the basic block, just before us
              b->insert_node(copy, l++);
            }
            // Insert the copy in the use-def chain
            n->set_req(idx, copy);
            // Extend ("register allocate") the names array for the copy.
            _phc._lrg_map.extend(copy->_idx, name);
            _phc._cfg.map_node_to_block(copy, b);
          }

        } // End of is two-adr

        // Insert a copy at a debug use for a lrg which has high frequency
        if (b->_freq < OPTO_DEBUG_SPLIT_FREQ || _phc._cfg.is_uncommon(b)) {
          // Walk the debug inputs to the node and check for lrg freq
          JVMState* jvms = n->jvms();
          uint debug_start = jvms ? jvms->debug_start() : 999999;
          uint debug_end   = jvms ? jvms->debug_end()   : 999999;
          for(uint inpidx = debug_start; inpidx < debug_end; inpidx++) {
            // Do not split monitors; they are only needed for debug table
            // entries and need no code.
            if (jvms->is_monitor_use(inpidx)) {
              continue;
            }
            Node *inp = n->in(inpidx);
            uint nidx = _phc._lrg_map.live_range_id(inp);
            LRG &lrg = lrgs(nidx);

            // If this lrg has a high frequency use/def
            if( lrg._maxfreq >= _phc.high_frequency_lrg() ) {
              // If the live range is also live out of this block (like it
              // would be for a fast/slow idiom), the normal spill mechanism
              // does an excellent job.  If it is not live out of this block
              // (like it would be for debug info to uncommon trap) splitting
              // the live range now allows a better allocation in the high
              // frequency blocks.
              //   Build_IFG_virtual has converted the live sets to
              // live-IN info, not live-OUT info.
              uint k;
              for( k=0; k < b->_num_succs; k++ )
                if( _phc._live->live(b->_succs[k])->member( nidx ) )
                  break;      // Live in to some successor block?
              if( k < b->_num_succs )
                continue;     // Live out; do not pre-split
              // Split the lrg at this use
              uint ireg = inp->ideal_reg();
              if (ireg == 0 || ireg == Op_RegFlags) {
                assert(false, "attempted to spill a non-spillable item: %d: %s, ireg = %u, spill_type: %s",
                       inp->_idx, inp->Name(), ireg, MachSpillCopyNode::spill_type(MachSpillCopyNode::DebugUse));
                C->record_method_not_compilable("attempted to spill a non-spillable item");
                return;
              }
              const RegMask *rm = C->matcher()->idealreg2spillmask[ireg];
              Node* copy = new MachSpillCopyNode(MachSpillCopyNode::DebugUse, inp, *rm, *rm);
              // Insert the copy in the use-def chain
              n->set_req(inpidx, copy );
              // Insert the copy in the basic block, just before us
              b->insert_node(copy,  l++);
              // Extend ("register allocate") the names array for the copy.
              uint max_lrg_id = _phc._lrg_map.max_lrg_id();
              _phc.new_lrg(copy, max_lrg_id);
              _phc._lrg_map.set_max_lrg_id(max_lrg_id + 1);
              _phc._cfg.map_node_to_block(copy, b);
              //tty->print_cr("Split a debug use in Aggressive Coalesce");
            }  // End of if high frequency use/def
          }  // End of for all debug inputs
        }  // End of if low frequency safepoint

      } // End of if Phi

    } // End of for all instructions
  } // End of for all blocks
}


// Aggressive (but pessimistic) copy coalescing of a single block

// The following coalesce pass represents a single round of aggressive
// pessimistic coalesce.  "Aggressive" means no attempt to preserve
// colorability when coalescing.  This occasionally means more spills, but
// it also means fewer rounds of coalescing for better code - and that means
// faster compiles.

// "Pessimistic" means we do not hit the fixed point in one pass (and we are
// reaching for the least fixed point to boot).  This is typically solved
// with a few more rounds of coalescing, but the compiler must run fast.  We
// could optimistically coalescing everything touching PhiNodes together
// into one big live range, then check for self-interference.  Everywhere
// the live range interferes with self it would have to be split.  Finding
// the right split points can be done with some heuristics (based on
// expected frequency of edges in the live range).  In short, it's a real
// research problem and the timeline is too short to allow such research.
// Further thoughts: (1) build the LR in a pass, (2) find self-interference
// in another pass, (3) per each self-conflict, split, (4) split by finding
// the low-cost cut (min-cut) of the LR, (5) edges in the LR are weighted
// according to the GCM algorithm (or just exec freq on CFG edges).

void PhaseAggressiveCoalesce::coalesce( Block *b ) {
  // Copies are still "virtual" - meaning we have not made them explicitly
  // copies.  Instead, Phi functions of successor blocks have mis-matched
  // live-ranges.  If I fail to coalesce, I'll have to insert a copy to line
  // up the live-ranges.  Check for Phis in successor blocks.
  uint i;
  for( i=0; i<b->_num_succs; i++ ) {
    Block *bs = b->_succs[i];
    // Find index of 'b' in 'bs' predecessors
    uint j=1;
    while (_phc._cfg.get_block_for_node(bs->pred(j)) != b) {
      j++;
    }

    // Visit all the Phis in successor block
    for( uint k = 1; k<bs->number_of_nodes(); k++ ) {
      Node *n = bs->get_node(k);
      if( !n->is_Phi() ) break;
      combine_these_two( n, n->in(j) );
    }
  } // End of for all successor blocks


  // Check _this_ block for 2-address instructions and copies.
  uint cnt = b->end_idx();
  for( i = 1; i<cnt; i++ ) {
    Node *n = b->get_node(i);
    uint idx;
    // 2-address instructions have a virtual Copy matching their input
    // to their output
    if (n->is_Mach() && (idx = n->as_Mach()->two_adr())) {
      MachNode *mach = n->as_Mach();
      combine_these_two(mach, mach->in(idx));
    }
  } // End of for all instructions in block
}

PhaseConservativeCoalesce::PhaseConservativeCoalesce(PhaseChaitin &chaitin) : PhaseCoalesce(chaitin) {
  _ulr.initialize(_phc._lrg_map.max_lrg_id());
}

void PhaseConservativeCoalesce::verify() {
#ifdef ASSERT
  _phc.set_was_low();
#endif
}

void PhaseConservativeCoalesce::union_helper( Node *lr1_node, Node *lr2_node, uint lr1, uint lr2, Node *src_def, Node *dst_copy, Node *src_copy, Block *b, uint bindex ) {
  // Join live ranges.  Merge larger into smaller.  Union lr2 into lr1 in the
  // union-find tree
  _phc.Union( lr1_node, lr2_node );

  // Single-def live range ONLY if both live ranges are single-def.
  // If both are single def, then src_def powers one live range
  // and def_copy powers the other.  After merging, src_def powers
  // the combined live range.
  lrgs(lr1)._def = (lrgs(lr1).is_multidef() ||
                        lrgs(lr2).is_multidef() )
    ? NodeSentinel : src_def;
  lrgs(lr2)._def = NULL;    // No def for lrg 2
  lrgs(lr2).Clear();        // Force empty mask for LRG 2
  //lrgs(lr2)._size = 0;      // Live-range 2 goes dead
  lrgs(lr1)._is_oop |= lrgs(lr2)._is_oop;
  lrgs(lr2)._is_oop = 0;    // In particular, not an oop for GC info

  if (lrgs(lr1)._maxfreq < lrgs(lr2)._maxfreq)
    lrgs(lr1)._maxfreq = lrgs(lr2)._maxfreq;

  // Copy original value instead.  Intermediate copies go dead, and
  // the dst_copy becomes useless.
  int didx = dst_copy->is_Copy();
  dst_copy->set_req( didx, src_def );
  // Add copy to free list
  // _phc.free_spillcopy(b->_nodes[bindex]);
  assert( b->get_node(bindex) == dst_copy, "" );
  dst_copy->replace_by( dst_copy->in(didx) );
  dst_copy->set_req( didx, NULL);
  b->remove_node(bindex);
  if( bindex < b->_ihrp_index ) b->_ihrp_index--;
  if( bindex < b->_fhrp_index ) b->_fhrp_index--;

  // Stretched lr1; add it to liveness of intermediate blocks
  Block *b2 = _phc._cfg.get_block_for_node(src_copy);
  while( b != b2 ) {
    b = _phc._cfg.get_block_for_node(b->pred(1));
    _phc._live->live(b)->insert(lr1);
  }
}

// Factored code from copy_copy that computes extra interferences from
// lengthening a live range by double-coalescing.
uint PhaseConservativeCoalesce::compute_separating_interferences(Node *dst_copy, Node *src_copy, Block *b, uint bindex, RegMask &rm, uint rm_size, uint reg_degree, uint lr1, uint lr2 ) {

  assert(!lrgs(lr1)._fat_proj, "cannot coalesce fat_proj");
  assert(!lrgs(lr2)._fat_proj, "cannot coalesce fat_proj");
  Node *prev_copy = dst_copy->in(dst_copy->is_Copy());
  Block *b2 = b;
  uint bindex2 = bindex;
  while( 1 ) {
    // Find previous instruction
    bindex2--;                  // Chain backwards 1 instruction
    while( bindex2 == 0 ) {     // At block start, find prior block
      assert( b2->num_preds() == 2, "cannot double coalesce across c-flow" );
      b2 = _phc._cfg.get_block_for_node(b2->pred(1));
      bindex2 = b2->end_idx()-1;
    }
    // Get prior instruction
    assert(bindex2 < b2->number_of_nodes(), "index out of bounds");
    Node *x = b2->get_node(bindex2);
    if( x == prev_copy ) {      // Previous copy in copy chain?
      if( prev_copy == src_copy)// Found end of chain and all interferences
        break;                  // So break out of loop
      // Else work back one in copy chain
      prev_copy = prev_copy->in(prev_copy->is_Copy());
    } else {                    // Else collect interferences
      uint lidx = _phc._lrg_map.find(x);
      // Found another def of live-range being stretched?
      if(lidx == lr1) {
        return max_juint;
      }
      if(lidx == lr2) {
        return max_juint;
      }

      // If we attempt to coalesce across a bound def
      if( lrgs(lidx).is_bound() ) {
        // Do not let the coalesced LRG expect to get the bound color
        rm.SUBTRACT( lrgs(lidx).mask() );
        // Recompute rm_size
        rm_size = rm.Size();
        //if( rm._flags ) rm_size += 1000000;
        if( reg_degree >= rm_size ) return max_juint;
      }
      if( rm.overlap(lrgs(lidx).mask()) ) {
        // Insert lidx into union LRG; returns TRUE if actually inserted
        if( _ulr.insert(lidx) ) {
          // Infinite-stack neighbors do not alter colorability, as they
          // can always color to some other color.
          if( !lrgs(lidx).mask().is_AllStack() ) {
            // If this coalesce will make any new neighbor uncolorable,
            // do not coalesce.
            if( lrgs(lidx).just_lo_degree() )
              return max_juint;
            // Bump our degree
            if( ++reg_degree >= rm_size )
              return max_juint;
          } // End of if not infinite-stack neighbor
        } // End of if actually inserted
      } // End of if live range overlaps
    } // End of else collect interferences for 1 node
  } // End of while forever, scan back for interferences
  return reg_degree;
}

void PhaseConservativeCoalesce::update_ifg(uint lr1, uint lr2, IndexSet *n_lr1, IndexSet *n_lr2) {
  // Some original neighbors of lr1 might have gone away
  // because the constrained register mask prevented them.
  // Remove lr1 from such neighbors.
  uint neighbor = 0;
  LRG &lrg1 = lrgs(lr1);
  if (!n_lr1->is_empty()) {
    IndexSetIterator one(n_lr1);
    while ((neighbor = one.next()) != 0) {
      if (!_ulr.member(neighbor)) {
        if (_phc._ifg->neighbors(neighbor)->remove(lr1)) {
          lrgs(neighbor).inc_degree(-lrg1.compute_degree(lrgs(neighbor)));
        }
      }
    }
  }


  // lr2 is now called (coalesced into) lr1.
  // Remove lr2 from the IFG.
  LRG &lrg2 = lrgs(lr2);
  if (!n_lr2->is_empty()) {
    IndexSetIterator two(n_lr2);
    while ((neighbor = two.next()) != 0) {
      if (_phc._ifg->neighbors(neighbor)->remove(lr2)) {
        lrgs(neighbor).inc_degree(-lrg2.compute_degree(lrgs(neighbor)));
      }
    }
  }

  // Some neighbors of intermediate copies now interfere with the
  // combined live range.
  if (!_ulr.is_empty()) {
    IndexSetIterator three(&_ulr);
    while ((neighbor = three.next()) != 0) {
      if (_phc._ifg->neighbors(neighbor)->insert(lr1)) {
        lrgs(neighbor).inc_degree(lrg1.compute_degree(lrgs(neighbor)));
      }
    }
  }
}

static void record_bias( const PhaseIFG *ifg, int lr1, int lr2 ) {
  // Tag copy bias here
  if( !ifg->lrgs(lr1)._copy_bias )
    ifg->lrgs(lr1)._copy_bias = lr2;
  if( !ifg->lrgs(lr2)._copy_bias )
    ifg->lrgs(lr2)._copy_bias = lr1;
}

// See if I can coalesce a series of multiple copies together.  I need the
// final dest copy and the original src copy.  They can be the same Node.
// Compute the compatible register masks.
bool PhaseConservativeCoalesce::copy_copy(Node *dst_copy, Node *src_copy, Block *b, uint bindex) {

  if (!dst_copy->is_SpillCopy()) {
    return false;
  }
  if (!src_copy->is_SpillCopy()) {
    return false;
  }
  Node *src_def = src_copy->in(src_copy->is_Copy());
  uint lr1 = _phc._lrg_map.find(dst_copy);
  uint lr2 = _phc._lrg_map.find(src_def);

  // Same live ranges already?
  if (lr1 == lr2) {
    return false;
  }

  // Interfere?
  if (_phc._ifg->test_edge_sq(lr1, lr2)) {
    return false;
  }

  // Not an oop->int cast; oop->oop, int->int, AND int->oop are OK.
  if (!lrgs(lr1)._is_oop && lrgs(lr2)._is_oop) { // not an oop->int cast
    return false;
  }

  // Coalescing between an aligned live range and a mis-aligned live range?
  // No, no!  Alignment changes how we count degree.
  if (lrgs(lr1)._fat_proj != lrgs(lr2)._fat_proj) {
    return false;
  }

  // Sort; use smaller live-range number
  Node *lr1_node = dst_copy;
  Node *lr2_node = src_def;
  if (lr1 > lr2) {
    uint tmp = lr1; lr1 = lr2; lr2 = tmp;
    lr1_node = src_def;  lr2_node = dst_copy;
  }

  // Check for compatibility of the 2 live ranges by
  // intersecting their allowed register sets.
  RegMask rm = lrgs(lr1).mask();
  rm.AND(lrgs(lr2).mask());
  // Number of bits free
  uint rm_size = rm.Size();

  if (UseFPUForSpilling && rm.is_AllStack() ) {
    // Don't coalesce when frequency difference is large
    Block *dst_b = _phc._cfg.get_block_for_node(dst_copy);
    Block *src_def_b = _phc._cfg.get_block_for_node(src_def);
    if (src_def_b->_freq > 10*dst_b->_freq )
      return false;
  }

  // If we can use any stack slot, then effective size is infinite
  if( rm.is_AllStack() ) rm_size += 1000000;
  // Incompatible masks, no way to coalesce
  if( rm_size == 0 ) return false;

  // Another early bail-out test is when we are double-coalescing and the
  // 2 copies are separated by some control flow.
  if( dst_copy != src_copy ) {
    Block *src_b = _phc._cfg.get_block_for_node(src_copy);
    Block *b2 = b;
    while( b2 != src_b ) {
      if( b2->num_preds() > 2 ){// Found merge-point
        _phc._lost_opp_cflow_coalesce++;
        // extra record_bias commented out because Chris believes it is not
        // productive.  Since we can record only 1 bias, we want to choose one
        // that stands a chance of working and this one probably does not.
        //record_bias( _phc._lrgs, lr1, lr2 );
        return false;           // To hard to find all interferences
      }
      b2 = _phc._cfg.get_block_for_node(b2->pred(1));
    }
  }

  // Union the two interference sets together into '_ulr'
  uint reg_degree = _ulr.lrg_union( lr1, lr2, rm_size, _phc._ifg, rm );

  if( reg_degree >= rm_size ) {
    record_bias( _phc._ifg, lr1, lr2 );
    return false;
  }

  // Now I need to compute all the interferences between dst_copy and
  // src_copy.  I'm not willing visit the entire interference graph, so
  // I limit my search to things in dst_copy's block or in a straight
  // line of previous blocks.  I give up at merge points or when I get
  // more interferences than my degree.  I can stop when I find src_copy.
  if( dst_copy != src_copy ) {
    reg_degree = compute_separating_interferences(dst_copy, src_copy, b, bindex, rm, rm_size, reg_degree, lr1, lr2 );
    if( reg_degree == max_juint ) {
      record_bias( _phc._ifg, lr1, lr2 );
      return false;
    }
  } // End of if dst_copy & src_copy are different


  // ---- THE COMBINED LRG IS COLORABLE ----

  // YEAH - Now coalesce this copy away
  assert( lrgs(lr1).num_regs() == lrgs(lr2).num_regs(),   "" );

  IndexSet *n_lr1 = _phc._ifg->neighbors(lr1);
  IndexSet *n_lr2 = _phc._ifg->neighbors(lr2);

  // Update the interference graph
  update_ifg(lr1, lr2, n_lr1, n_lr2);

  _ulr.remove(lr1);

  // Uncomment the following code to trace Coalescing in great detail.
  //
  //if (false) {
  //  tty->cr();
  //  tty->print_cr("#######################################");
  //  tty->print_cr("union %d and %d", lr1, lr2);
  //  n_lr1->dump();
  //  n_lr2->dump();
  //  tty->print_cr("resulting set is");
  //  _ulr.dump();
  //}

  // Replace n_lr1 with the new combined live range.  _ulr will use
  // n_lr1's old memory on the next iteration.  n_lr2 is cleared to
  // send its internal memory to the free list.
  _ulr.swap(n_lr1);
  _ulr.clear();
  n_lr2->clear();

  lrgs(lr1).set_degree( _phc._ifg->effective_degree(lr1) );
  lrgs(lr2).set_degree( 0 );

  // Join live ranges.  Merge larger into smaller.  Union lr2 into lr1 in the
  // union-find tree
  union_helper( lr1_node, lr2_node, lr1, lr2, src_def, dst_copy, src_copy, b, bindex );
  // Combine register restrictions
  lrgs(lr1).set_mask(rm);
  lrgs(lr1).compute_set_mask_size();
  lrgs(lr1)._cost += lrgs(lr2)._cost;
  lrgs(lr1)._area += lrgs(lr2)._area;

  // While its uncommon to successfully coalesce live ranges that started out
  // being not-lo-degree, it can happen.  In any case the combined coalesced
  // live range better Simplify nicely.
  lrgs(lr1)._was_lo = 1;

  // kinda expensive to do all the time
  //tty->print_cr("warning: slow verify happening");
  //_phc._ifg->verify( &_phc );
  return true;
}

// Conservative (but pessimistic) copy coalescing of a single block
void PhaseConservativeCoalesce::coalesce( Block *b ) {
  // Bail out on infrequent blocks
  if (_phc._cfg.is_uncommon(b)) {
    return;
  }
  // Check this block for copies.
  for( uint i = 1; i<b->end_idx(); i++ ) {
    // Check for actual copies on inputs.  Coalesce a copy into its
    // input if use and copy's input are compatible.
    Node *copy1 = b->get_node(i);
    uint idx1 = copy1->is_Copy();
    if( !idx1 ) continue;       // Not a copy

    if( copy_copy(copy1,copy1,b,i) ) {
      i--;                      // Retry, same location in block
      PhaseChaitin::_conserv_coalesce++;  // Collect stats on success
      continue;
    }
  }
}
