/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "compiler/oopMap.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "opto/addnode.hpp"
#include "opto/block.hpp"
#include "opto/callnode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/chaitin.hpp"
#include "opto/coalesce.hpp"
#include "opto/connode.hpp"
#include "opto/idealGraphPrinter.hpp"
#include "opto/indexSet.hpp"
#include "opto/machnode.hpp"
#include "opto/memnode.hpp"
#include "opto/movenode.hpp"
#include "opto/opcodes.hpp"
#include "opto/rootnode.hpp"
#include "utilities/align.hpp"

#ifndef PRODUCT
void LRG::dump() const {
  ttyLocker ttyl;
  tty->print("%d ",num_regs());
  _mask.dump();
  if( _msize_valid ) {
    if( mask_size() == compute_mask_size() ) tty->print(", #%d ",_mask_size);
    else tty->print(", #!!!_%d_vs_%d ",_mask_size,_mask.Size());
  } else {
    tty->print(", #?(%d) ",_mask.Size());
  }

  tty->print("EffDeg: ");
  if( _degree_valid ) tty->print( "%d ", _eff_degree );
  else tty->print("? ");

  if( is_multidef() ) {
    tty->print("MultiDef ");
    if (_defs != NULL) {
      tty->print("(");
      for (int i = 0; i < _defs->length(); i++) {
        tty->print("N%d ", _defs->at(i)->_idx);
      }
      tty->print(") ");
    }
  }
  else if( _def == 0 ) tty->print("Dead ");
  else tty->print("Def: N%d ",_def->_idx);

  tty->print("Cost:%4.2g Area:%4.2g Score:%4.2g ",_cost,_area, score());
  // Flags
  if( _is_oop ) tty->print("Oop ");
  if( _is_float ) tty->print("Float ");
  if( _is_vector ) tty->print("Vector ");
  if( _is_scalable ) tty->print("Scalable ");
  if( _was_spilled1 ) tty->print("Spilled ");
  if( _was_spilled2 ) tty->print("Spilled2 ");
  if( _direct_conflict ) tty->print("Direct_conflict ");
  if( _fat_proj ) tty->print("Fat ");
  if( _was_lo ) tty->print("Lo ");
  if( _has_copy ) tty->print("Copy ");
  if( _at_risk ) tty->print("Risk ");

  if( _must_spill ) tty->print("Must_spill ");
  if( _is_bound ) tty->print("Bound ");
  if( _msize_valid ) {
    if( _degree_valid && lo_degree() ) tty->print("Trivial ");
  }

  tty->cr();
}
#endif

// Compute score from cost and area.  Low score is best to spill.
static double raw_score( double cost, double area ) {
  return cost - (area*RegisterCostAreaRatio) * 1.52588e-5;
}

double LRG::score() const {
  // Scale _area by RegisterCostAreaRatio/64K then subtract from cost.
  // Bigger area lowers score, encourages spilling this live range.
  // Bigger cost raise score, prevents spilling this live range.
  // (Note: 1/65536 is the magic constant below; I dont trust the C optimizer
  // to turn a divide by a constant into a multiply by the reciprical).
  double score = raw_score( _cost, _area);

  // Account for area.  Basically, LRGs covering large areas are better
  // to spill because more other LRGs get freed up.
  if( _area == 0.0 )            // No area?  Then no progress to spill
    return 1e35;

  if( _was_spilled2 )           // If spilled once before, we are unlikely
    return score + 1e30;        // to make progress again.

  if( _cost >= _area*3.0 )      // Tiny area relative to cost
    return score + 1e17;        // Probably no progress to spill

  if( (_cost+_cost) >= _area*3.0 ) // Small area relative to cost
    return score + 1e10;        // Likely no progress to spill

  return score;
}

#define NUMBUCKS 3

// Straight out of Tarjan's union-find algorithm
uint LiveRangeMap::find_compress(uint lrg) {
  uint cur = lrg;
  uint next = _uf_map.at(cur);
  while (next != cur) { // Scan chain of equivalences
    assert( next < cur, "always union smaller");
    cur = next; // until find a fixed-point
    next = _uf_map.at(cur);
  }

  // Core of union-find algorithm: update chain of
  // equivalences to be equal to the root.
  while (lrg != next) {
    uint tmp = _uf_map.at(lrg);
    _uf_map.at_put(lrg, next);
    lrg = tmp;
  }
  return lrg;
}

// Reset the Union-Find map to identity
void LiveRangeMap::reset_uf_map(uint max_lrg_id) {
  _max_lrg_id= max_lrg_id;
  // Force the Union-Find mapping to be at least this large
  _uf_map.at_put_grow(_max_lrg_id, 0);
  // Initialize it to be the ID mapping.
  for (uint i = 0; i < _max_lrg_id; ++i) {
    _uf_map.at_put(i, i);
  }
}

// Make all Nodes map directly to their final live range; no need for
// the Union-Find mapping after this call.
void LiveRangeMap::compress_uf_map_for_nodes() {
  // For all Nodes, compress mapping
  uint unique = _names.length();
  for (uint i = 0; i < unique; ++i) {
    uint lrg = _names.at(i);
    uint compressed_lrg = find(lrg);
    if (lrg != compressed_lrg) {
      _names.at_put(i, compressed_lrg);
    }
  }
}

// Like Find above, but no path compress, so bad asymptotic behavior
uint LiveRangeMap::find_const(uint lrg) const {
  if (!lrg) {
    return lrg; // Ignore the zero LRG
  }

  // Off the end?  This happens during debugging dumps when you got
  // brand new live ranges but have not told the allocator yet.
  if (lrg >= _max_lrg_id) {
    return lrg;
  }

  uint next = _uf_map.at(lrg);
  while (next != lrg) { // Scan chain of equivalences
    assert(next < lrg, "always union smaller");
    lrg = next; // until find a fixed-point
    next = _uf_map.at(lrg);
  }
  return next;
}

PhaseChaitin::PhaseChaitin(uint unique, PhaseCFG &cfg, Matcher &matcher, bool scheduling_info_generated)
  : PhaseRegAlloc(unique, cfg, matcher,
#ifndef PRODUCT
       print_chaitin_statistics
#else
       NULL
#endif
       )
  , _live(0)
  , _lo_degree(0), _lo_stk_degree(0), _hi_degree(0), _simplified(0)
  , _oldphi(unique)
#ifndef PRODUCT
  , _trace_spilling(C->directive()->TraceSpillingOption)
#endif
  , _lrg_map(Thread::current()->resource_area(), unique)
  , _scheduling_info_generated(scheduling_info_generated)
  , _sched_int_pressure(0, Matcher::int_pressure_limit())
  , _sched_float_pressure(0, Matcher::float_pressure_limit())
  , _scratch_int_pressure(0, Matcher::int_pressure_limit())
  , _scratch_float_pressure(0, Matcher::float_pressure_limit())
{
  Compile::TracePhase tp("ctorChaitin", &timers[_t_ctorChaitin]);

  _high_frequency_lrg = MIN2(double(OPTO_LRG_HIGH_FREQ), _cfg.get_outer_loop_frequency());

  // Build a list of basic blocks, sorted by frequency
  _blks = NEW_RESOURCE_ARRAY(Block *, _cfg.number_of_blocks());
  // Experiment with sorting strategies to speed compilation
  double  cutoff = BLOCK_FREQUENCY(1.0); // Cutoff for high frequency bucket
  Block **buckets[NUMBUCKS];             // Array of buckets
  uint    buckcnt[NUMBUCKS];             // Array of bucket counters
  double  buckval[NUMBUCKS];             // Array of bucket value cutoffs
  for (uint i = 0; i < NUMBUCKS; i++) {
    buckets[i] = NEW_RESOURCE_ARRAY(Block *, _cfg.number_of_blocks());
    buckcnt[i] = 0;
    // Bump by three orders of magnitude each time
    cutoff *= 0.001;
    buckval[i] = cutoff;
    for (uint j = 0; j < _cfg.number_of_blocks(); j++) {
      buckets[i][j] = NULL;
    }
  }
  // Sort blocks into buckets
  for (uint i = 0; i < _cfg.number_of_blocks(); i++) {
    for (uint j = 0; j < NUMBUCKS; j++) {
      if ((j == NUMBUCKS - 1) || (_cfg.get_block(i)->_freq > buckval[j])) {
        // Assign block to end of list for appropriate bucket
        buckets[j][buckcnt[j]++] = _cfg.get_block(i);
        break; // kick out of inner loop
      }
    }
  }
  // Dump buckets into final block array
  uint blkcnt = 0;
  for (uint i = 0; i < NUMBUCKS; i++) {
    for (uint j = 0; j < buckcnt[i]; j++) {
      _blks[blkcnt++] = buckets[i][j];
    }
  }

  assert(blkcnt == _cfg.number_of_blocks(), "Block array not totally filled");
}

// union 2 sets together.
void PhaseChaitin::Union( const Node *src_n, const Node *dst_n ) {
  uint src = _lrg_map.find(src_n);
  uint dst = _lrg_map.find(dst_n);
  assert(src, "");
  assert(dst, "");
  assert(src < _lrg_map.max_lrg_id(), "oob");
  assert(dst < _lrg_map.max_lrg_id(), "oob");
  assert(src < dst, "always union smaller");
  _lrg_map.uf_map(dst, src);
}

void PhaseChaitin::new_lrg(const Node *x, uint lrg) {
  // Make the Node->LRG mapping
  _lrg_map.extend(x->_idx,lrg);
  // Make the Union-Find mapping an identity function
  _lrg_map.uf_extend(lrg, lrg);
}


int PhaseChaitin::clone_projs(Block* b, uint idx, Node* orig, Node* copy, uint& max_lrg_id) {
  assert(b->find_node(copy) == (idx - 1), "incorrect insert index for copy kill projections");
  DEBUG_ONLY( Block* borig = _cfg.get_block_for_node(orig); )
  int found_projs = 0;
  uint cnt = orig->outcnt();
  for (uint i = 0; i < cnt; i++) {
    Node* proj = orig->raw_out(i);
    if (proj->is_MachProj()) {
      assert(proj->outcnt() == 0, "only kill projections are expected here");
      assert(_cfg.get_block_for_node(proj) == borig, "incorrect block for kill projections");
      found_projs++;
      // Copy kill projections after the cloned node
      Node* kills = proj->clone();
      kills->set_req(0, copy);
      b->insert_node(kills, idx++);
      _cfg.map_node_to_block(kills, b);
      new_lrg(kills, max_lrg_id++);
    }
  }
  return found_projs;
}

// Renumber the live ranges to compact them.  Makes the IFG smaller.
void PhaseChaitin::compact() {
  Compile::TracePhase tp("chaitinCompact", &timers[_t_chaitinCompact]);

  // Current the _uf_map contains a series of short chains which are headed
  // by a self-cycle.  All the chains run from big numbers to little numbers.
  // The Find() call chases the chains & shortens them for the next Find call.
  // We are going to change this structure slightly.  Numbers above a moving
  // wave 'i' are unchanged.  Numbers below 'j' point directly to their
  // compacted live range with no further chaining.  There are no chains or
  // cycles below 'i', so the Find call no longer works.
  uint j=1;
  uint i;
  for (i = 1; i < _lrg_map.max_lrg_id(); i++) {
    uint lr = _lrg_map.uf_live_range_id(i);
    // Ignore unallocated live ranges
    if (!lr) {
      continue;
    }
    assert(lr <= i, "");
    _lrg_map.uf_map(i, ( lr == i ) ? j++ : _lrg_map.uf_live_range_id(lr));
  }
  // Now change the Node->LR mapping to reflect the compacted names
  uint unique = _lrg_map.size();
  for (i = 0; i < unique; i++) {
    uint lrg_id = _lrg_map.live_range_id(i);
    _lrg_map.map(i, _lrg_map.uf_live_range_id(lrg_id));
  }

  // Reset the Union-Find mapping
  _lrg_map.reset_uf_map(j);
}

void PhaseChaitin::Register_Allocate() {

  // Above the OLD FP (and in registers) are the incoming arguments.  Stack
  // slots in this area are called "arg_slots".  Above the NEW FP (and in
  // registers) is the outgoing argument area; above that is the spill/temp
  // area.  These are all "frame_slots".  Arg_slots start at the zero
  // stack_slots and count up to the known arg_size.  Frame_slots start at
  // the stack_slot #arg_size and go up.  After allocation I map stack
  // slots to actual offsets.  Stack-slots in the arg_slot area are biased
  // by the frame_size; stack-slots in the frame_slot area are biased by 0.

  _trip_cnt = 0;
  _alternate = 0;
  _matcher._allocation_started = true;

  ResourceArea split_arena(mtCompiler);     // Arena for Split local resources
  ResourceArea live_arena(mtCompiler);      // Arena for liveness & IFG info
  ResourceMark rm(&live_arena);

  // Need live-ness for the IFG; need the IFG for coalescing.  If the
  // liveness is JUST for coalescing, then I can get some mileage by renaming
  // all copy-related live ranges low and then using the max copy-related
  // live range as a cut-off for LIVE and the IFG.  In other words, I can
  // build a subset of LIVE and IFG just for copies.
  PhaseLive live(_cfg, _lrg_map.names(), &live_arena, false);

  // Need IFG for coalescing and coloring
  PhaseIFG ifg(&live_arena);
  _ifg = &ifg;

  // Come out of SSA world to the Named world.  Assign (virtual) registers to
  // Nodes.  Use the same register for all inputs and the output of PhiNodes
  // - effectively ending SSA form.  This requires either coalescing live
  // ranges or inserting copies.  For the moment, we insert "virtual copies"
  // - we pretend there is a copy prior to each Phi in predecessor blocks.
  // We will attempt to coalesce such "virtual copies" before we manifest
  // them for real.
  de_ssa();

#ifdef ASSERT
  // Veify the graph before RA.
  verify(&live_arena);
#endif

  {
    Compile::TracePhase tp("computeLive", &timers[_t_computeLive]);
    _live = NULL;                 // Mark live as being not available
    rm.reset_to_mark();           // Reclaim working storage
    IndexSet::reset_memory(C, &live_arena);
    ifg.init(_lrg_map.max_lrg_id()); // Empty IFG
    gather_lrg_masks( false );    // Collect LRG masks
    live.compute(_lrg_map.max_lrg_id()); // Compute liveness
    _live = &live;                // Mark LIVE as being available
  }

  // Base pointers are currently "used" by instructions which define new
  // derived pointers.  This makes base pointers live up to the where the
  // derived pointer is made, but not beyond.  Really, they need to be live
  // across any GC point where the derived value is live.  So this code looks
  // at all the GC points, and "stretches" the live range of any base pointer
  // to the GC point.
  if (stretch_base_pointer_live_ranges(&live_arena)) {
    Compile::TracePhase tp("computeLive (sbplr)", &timers[_t_computeLive]);
    // Since some live range stretched, I need to recompute live
    _live = NULL;
    rm.reset_to_mark();         // Reclaim working storage
    IndexSet::reset_memory(C, &live_arena);
    ifg.init(_lrg_map.max_lrg_id());
    gather_lrg_masks(false);
    live.compute(_lrg_map.max_lrg_id());
    _live = &live;
  }
  // Create the interference graph using virtual copies
  build_ifg_virtual();  // Include stack slots this time

  // The IFG is/was triangular.  I am 'squaring it up' so Union can run
  // faster.  Union requires a 'for all' operation which is slow on the
  // triangular adjacency matrix (quick reminder: the IFG is 'sparse' -
  // meaning I can visit all the Nodes neighbors less than a Node in time
  // O(# of neighbors), but I have to visit all the Nodes greater than a
  // given Node and search them for an instance, i.e., time O(#MaxLRG)).
  _ifg->SquareUp();

  // Aggressive (but pessimistic) copy coalescing.
  // This pass works on virtual copies.  Any virtual copies which are not
  // coalesced get manifested as actual copies
  {
    Compile::TracePhase tp("chaitinCoalesce1", &timers[_t_chaitinCoalesce1]);

    PhaseAggressiveCoalesce coalesce(*this);
    coalesce.coalesce_driver();
    // Insert un-coalesced copies.  Visit all Phis.  Where inputs to a Phi do
    // not match the Phi itself, insert a copy.
    coalesce.insert_copies(_matcher);
    if (C->failing()) {
      return;
    }
  }

  // After aggressive coalesce, attempt a first cut at coloring.
  // To color, we need the IFG and for that we need LIVE.
  {
    Compile::TracePhase tp("computeLive", &timers[_t_computeLive]);
    _live = NULL;
    rm.reset_to_mark();           // Reclaim working storage
    IndexSet::reset_memory(C, &live_arena);
    ifg.init(_lrg_map.max_lrg_id());
    gather_lrg_masks( true );
    live.compute(_lrg_map.max_lrg_id());
    _live = &live;
  }

  // Build physical interference graph
  uint must_spill = 0;
  must_spill = build_ifg_physical(&live_arena);
  // If we have a guaranteed spill, might as well spill now
  if (must_spill) {
    if(!_lrg_map.max_lrg_id()) {
      return;
    }
    // Bail out if unique gets too large (ie - unique > MaxNodeLimit)
    C->check_node_count(10*must_spill, "out of nodes before split");
    if (C->failing()) {
      return;
    }

    uint new_max_lrg_id = Split(_lrg_map.max_lrg_id(), &split_arena);  // Split spilling LRG everywhere
    _lrg_map.set_max_lrg_id(new_max_lrg_id);
    // Bail out if unique gets too large (ie - unique > MaxNodeLimit - 2*NodeLimitFudgeFactor)
    // or we failed to split
    C->check_node_count(2*NodeLimitFudgeFactor, "out of nodes after physical split");
    if (C->failing()) {
      return;
    }

    NOT_PRODUCT(C->verify_graph_edges();)

    compact();                  // Compact LRGs; return new lower max lrg

    {
      Compile::TracePhase tp("computeLive", &timers[_t_computeLive]);
      _live = NULL;
      rm.reset_to_mark();         // Reclaim working storage
      IndexSet::reset_memory(C, &live_arena);
      ifg.init(_lrg_map.max_lrg_id()); // Build a new interference graph
      gather_lrg_masks( true );   // Collect intersect mask
      live.compute(_lrg_map.max_lrg_id()); // Compute LIVE
      _live = &live;
    }
    build_ifg_physical(&live_arena);
    _ifg->SquareUp();
    _ifg->Compute_Effective_Degree();
    // Only do conservative coalescing if requested
    if (OptoCoalesce) {
      Compile::TracePhase tp("chaitinCoalesce2", &timers[_t_chaitinCoalesce2]);
      // Conservative (and pessimistic) copy coalescing of those spills
      PhaseConservativeCoalesce coalesce(*this);
      // If max live ranges greater than cutoff, don't color the stack.
      // This cutoff can be larger than below since it is only done once.
      coalesce.coalesce_driver();
    }
    _lrg_map.compress_uf_map_for_nodes();

#ifdef ASSERT
    verify(&live_arena, true);
#endif
  } else {
    ifg.SquareUp();
    ifg.Compute_Effective_Degree();
#ifdef ASSERT
    set_was_low();
#endif
  }

  // Prepare for Simplify & Select
  cache_lrg_info();           // Count degree of LRGs

  // Simplify the InterFerence Graph by removing LRGs of low degree.
  // LRGs of low degree are trivially colorable.
  Simplify();

  // Select colors by re-inserting LRGs back into the IFG in reverse order.
  // Return whether or not something spills.
  uint spills = Select( );

  // If we spill, split and recycle the entire thing
  while( spills ) {
    if( _trip_cnt++ > 24 ) {
      DEBUG_ONLY( dump_for_spill_split_recycle(); )
      if( _trip_cnt > 27 ) {
        C->record_method_not_compilable("failed spill-split-recycle sanity check");
        return;
      }
    }

    if (!_lrg_map.max_lrg_id()) {
      return;
    }
    uint new_max_lrg_id = Split(_lrg_map.max_lrg_id(), &split_arena);  // Split spilling LRG everywhere
    _lrg_map.set_max_lrg_id(new_max_lrg_id);
    // Bail out if unique gets too large (ie - unique > MaxNodeLimit - 2*NodeLimitFudgeFactor)
    C->check_node_count(2 * NodeLimitFudgeFactor, "out of nodes after split");
    if (C->failing()) {
      return;
    }

    compact(); // Compact LRGs; return new lower max lrg

    // Nuke the live-ness and interference graph and LiveRanGe info
    {
      Compile::TracePhase tp("computeLive", &timers[_t_computeLive]);
      _live = NULL;
      rm.reset_to_mark();         // Reclaim working storage
      IndexSet::reset_memory(C, &live_arena);
      ifg.init(_lrg_map.max_lrg_id());

      // Create LiveRanGe array.
      // Intersect register masks for all USEs and DEFs
      gather_lrg_masks(true);
      live.compute(_lrg_map.max_lrg_id());
      _live = &live;
    }
    must_spill = build_ifg_physical(&live_arena);
    _ifg->SquareUp();
    _ifg->Compute_Effective_Degree();

    // Only do conservative coalescing if requested
    if (OptoCoalesce) {
      Compile::TracePhase tp("chaitinCoalesce3", &timers[_t_chaitinCoalesce3]);
      // Conservative (and pessimistic) copy coalescing
      PhaseConservativeCoalesce coalesce(*this);
      // Check for few live ranges determines how aggressive coalesce is.
      coalesce.coalesce_driver();
    }
    _lrg_map.compress_uf_map_for_nodes();
#ifdef ASSERT
    verify(&live_arena, true);
#endif
    cache_lrg_info();           // Count degree of LRGs

    // Simplify the InterFerence Graph by removing LRGs of low degree.
    // LRGs of low degree are trivially colorable.
    Simplify();

    // Select colors by re-inserting LRGs back into the IFG in reverse order.
    // Return whether or not something spills.
    spills = Select();
  }

  // Count number of Simplify-Select trips per coloring success.
  _allocator_attempts += _trip_cnt + 1;
  _allocator_successes += 1;

  // Peephole remove copies
  post_allocate_copy_removal();

  // Merge multidefs if multiple defs representing the same value are used in a single block.
  merge_multidefs();

#ifdef ASSERT
  // Veify the graph after RA.
  verify(&live_arena);
#endif

  // max_reg is past the largest *register* used.
  // Convert that to a frame_slot number.
  if (_max_reg <= _matcher._new_SP) {
    _framesize = C->out_preserve_stack_slots();
  }
  else {
    _framesize = _max_reg -_matcher._new_SP;
  }
  assert((int)(_matcher._new_SP+_framesize) >= (int)_matcher._out_arg_limit, "framesize must be large enough");

  // This frame must preserve the required fp alignment
  _framesize = align_up(_framesize, Matcher::stack_alignment_in_slots());
  assert(_framesize <= 1000000, "sanity check");
#ifndef PRODUCT
  _total_framesize += _framesize;
  if ((int)_framesize > _max_framesize) {
    _max_framesize = _framesize;
  }
#endif

  // Convert CISC spills
  fixup_spills();

  // Log regalloc results
  CompileLog* log = Compile::current()->log();
  if (log != NULL) {
    log->elem("regalloc attempts='%d' success='%d'", _trip_cnt, !C->failing());
  }

  if (C->failing()) {
    return;
  }

  NOT_PRODUCT(C->verify_graph_edges();)

  // Move important info out of the live_arena to longer lasting storage.
  alloc_node_regs(_lrg_map.size());
  for (uint i=0; i < _lrg_map.size(); i++) {
    if (_lrg_map.live_range_id(i)) { // Live range associated with Node?
      LRG &lrg = lrgs(_lrg_map.live_range_id(i));
      if (!lrg.alive()) {
        set_bad(i);
      } else if (lrg.num_regs() == 1) {
        set1(i, lrg.reg());
      } else {                  // Must be a register-set
        if (!lrg._fat_proj) {   // Must be aligned adjacent register set
          // Live ranges record the highest register in their mask.
          // We want the low register for the AD file writer's convenience.
          OptoReg::Name hi = lrg.reg(); // Get hi register
          int num_regs = lrg.num_regs();
          if (lrg.is_scalable() && OptoReg::is_stack(hi)) {
            // For scalable vector registers, when they are allocated in physical
            // registers, num_regs is RegMask::SlotsPerVecA for reg mask of scalable
            // vector. If they are allocated on stack, we need to get the actual
            // num_regs, which reflects the physical length of scalable registers.
            num_regs = lrg.scalable_reg_slots();
          }
          OptoReg::Name lo = OptoReg::add(hi, (1-num_regs)); // Find lo
          // We have to use pair [lo,lo+1] even for wide vectors because
          // the rest of code generation works only with pairs. It is safe
          // since for registers encoding only 'lo' is used.
          // Second reg from pair is used in ScheduleAndBundle on SPARC where
          // vector max size is 8 which corresponds to registers pair.
          // It is also used in BuildOopMaps but oop operations are not
          // vectorized.
          set2(i, lo);
        } else {                // Misaligned; extract 2 bits
          OptoReg::Name hi = lrg.reg(); // Get hi register
          lrg.Remove(hi);       // Yank from mask
          int lo = lrg.mask().find_first_elem(); // Find lo
          set_pair(i, hi, lo);
        }
      }
      if( lrg._is_oop ) _node_oops.set(i);
    } else {
      set_bad(i);
    }
  }

  // Done!
  _live = NULL;
  _ifg = NULL;
  C->set_indexSet_arena(NULL);  // ResourceArea is at end of scope
}

void PhaseChaitin::de_ssa() {
  // Set initial Names for all Nodes.  Most Nodes get the virtual register
  // number.  A few get the ZERO live range number.  These do not
  // get allocated, but instead rely on correct scheduling to ensure that
  // only one instance is simultaneously live at a time.
  uint lr_counter = 1;
  for( uint i = 0; i < _cfg.number_of_blocks(); i++ ) {
    Block* block = _cfg.get_block(i);
    uint cnt = block->number_of_nodes();

    // Handle all the normal Nodes in the block
    for( uint j = 0; j < cnt; j++ ) {
      Node *n = block->get_node(j);
      // Pre-color to the zero live range, or pick virtual register
      const RegMask &rm = n->out_RegMask();
      _lrg_map.map(n->_idx, rm.is_NotEmpty() ? lr_counter++ : 0);
    }
  }

  // Reset the Union-Find mapping to be identity
  _lrg_map.reset_uf_map(lr_counter);
}

void PhaseChaitin::mark_ssa() {
  // Use ssa names to populate the live range maps or if no mask
  // is available, use the 0 entry.
  uint max_idx = 0;
  for ( uint i = 0; i < _cfg.number_of_blocks(); i++ ) {
    Block* block = _cfg.get_block(i);
    uint cnt = block->number_of_nodes();

    // Handle all the normal Nodes in the block
    for ( uint j = 0; j < cnt; j++ ) {
      Node *n = block->get_node(j);
      // Pre-color to the zero live range, or pick virtual register
      const RegMask &rm = n->out_RegMask();
      _lrg_map.map(n->_idx, rm.is_NotEmpty() ? n->_idx : 0);
      max_idx = (n->_idx > max_idx) ? n->_idx : max_idx;
    }
  }
  _lrg_map.set_max_lrg_id(max_idx+1);

  // Reset the Union-Find mapping to be identity
  _lrg_map.reset_uf_map(max_idx+1);
}


// Gather LiveRanGe information, including register masks.  Modification of
// cisc spillable in_RegMasks should not be done before AggressiveCoalesce.
void PhaseChaitin::gather_lrg_masks( bool after_aggressive ) {

  // Nail down the frame pointer live range
  uint fp_lrg = _lrg_map.live_range_id(_cfg.get_root_node()->in(1)->in(TypeFunc::FramePtr));
  lrgs(fp_lrg)._cost += 1e12;   // Cost is infinite

  // For all blocks
  for (uint i = 0; i < _cfg.number_of_blocks(); i++) {
    Block* block = _cfg.get_block(i);

    // For all instructions
    for (uint j = 1; j < block->number_of_nodes(); j++) {
      Node* n = block->get_node(j);
      uint input_edge_start =1; // Skip control most nodes
      bool is_machine_node = false;
      if (n->is_Mach()) {
        is_machine_node = true;
        input_edge_start = n->as_Mach()->oper_input_base();
      }
      uint idx = n->is_Copy();

      // Get virtual register number, same as LiveRanGe index
      uint vreg = _lrg_map.live_range_id(n);
      LRG& lrg = lrgs(vreg);
      if (vreg) {              // No vreg means un-allocable (e.g. memory)

        // Check for float-vs-int live range (used in register-pressure
        // calculations)
        const Type *n_type = n->bottom_type();
        if (n_type->is_floatingpoint()) {
          lrg._is_float = 1;
        }

        // Check for twice prior spilling.  Once prior spilling might have
        // spilled 'soft', 2nd prior spill should have spilled 'hard' and
        // further spilling is unlikely to make progress.
        if (_spilled_once.test(n->_idx)) {
          lrg._was_spilled1 = 1;
          if (_spilled_twice.test(n->_idx)) {
            lrg._was_spilled2 = 1;
          }
        }

#ifndef PRODUCT
        // Collect bits not used by product code, but which may be useful for
        // debugging.

        // Collect has-copy bit
        if (idx) {
          lrg._has_copy = 1;
          uint clidx = _lrg_map.live_range_id(n->in(idx));
          LRG& copy_src = lrgs(clidx);
          copy_src._has_copy = 1;
        }

        if (trace_spilling() && lrg._def != NULL) {
          // collect defs for MultiDef printing
          if (lrg._defs == NULL) {
            lrg._defs = new (_ifg->_arena) GrowableArray<Node*>(_ifg->_arena, 2, 0, NULL);
            lrg._defs->append(lrg._def);
          }
          lrg._defs->append(n);
        }
#endif

        // Check for a single def LRG; these can spill nicely
        // via rematerialization.  Flag as NULL for no def found
        // yet, or 'n' for single def or -1 for many defs.
        lrg._def = lrg._def ? NodeSentinel : n;

        // Limit result register mask to acceptable registers
        const RegMask &rm = n->out_RegMask();
        lrg.AND( rm );

        uint ireg = n->ideal_reg();
        assert( !n->bottom_type()->isa_oop_ptr() || ireg == Op_RegP,
                "oops must be in Op_RegP's" );

        // Check for vector live range (only if vector register is used).
        // On SPARC vector uses RegD which could be misaligned so it is not
        // processes as vector in RA.
        if (RegMask::is_vector(ireg)) {
          lrg._is_vector = 1;
          if (Matcher::implements_scalable_vector && ireg == Op_VecA) {
            assert(Matcher::supports_scalable_vector(), "scalable vector should be supported");
            lrg._is_scalable = 1;
            // For scalable vector, when it is allocated in physical register,
            // num_regs is RegMask::SlotsPerVecA for reg mask,
            // which may not be the actual physical register size.
            // If it is allocated in stack, we need to get the actual
            // physical length of scalable vector register.
            lrg.set_scalable_reg_slots(Matcher::scalable_vector_reg_size(T_FLOAT));
          }
        }
        assert(n_type->isa_vect() == NULL || lrg._is_vector ||
               ireg == Op_RegD || ireg == Op_RegL  || ireg == Op_RegVectMask,
               "vector must be in vector registers");

        // Check for bound register masks
        const RegMask &lrgmask = lrg.mask();
        if (lrgmask.is_bound(ireg)) {
          lrg._is_bound = 1;
        }

        // Check for maximum frequency value
        if (lrg._maxfreq < block->_freq) {
          lrg._maxfreq = block->_freq;
        }

        // Check for oop-iness, or long/double
        // Check for multi-kill projection
        switch (ireg) {
        case MachProjNode::fat_proj:
          // Fat projections have size equal to number of registers killed
          lrg.set_num_regs(rm.Size());
          lrg.set_reg_pressure(lrg.num_regs());
          lrg._fat_proj = 1;
          lrg._is_bound = 1;
          break;
        case Op_RegP:
#ifdef _LP64
          lrg.set_num_regs(2);  // Size is 2 stack words
#else
          lrg.set_num_regs(1);  // Size is 1 stack word
#endif
          // Register pressure is tracked relative to the maximum values
          // suggested for that platform, INTPRESSURE and FLOATPRESSURE,
          // and relative to other types which compete for the same regs.
          //
          // The following table contains suggested values based on the
          // architectures as defined in each .ad file.
          // INTPRESSURE and FLOATPRESSURE may be tuned differently for
          // compile-speed or performance.
          // Note1:
          // SPARC and SPARCV9 reg_pressures are at 2 instead of 1
          // since .ad registers are defined as high and low halves.
          // These reg_pressure values remain compatible with the code
          // in is_high_pressure() which relates get_invalid_mask_size(),
          // Block::_reg_pressure and INTPRESSURE, FLOATPRESSURE.
          // Note2:
          // SPARC -d32 has 24 registers available for integral values,
          // but only 10 of these are safe for 64-bit longs.
          // Using set_reg_pressure(2) for both int and long means
          // the allocator will believe it can fit 26 longs into
          // registers.  Using 2 for longs and 1 for ints means the
          // allocator will attempt to put 52 integers into registers.
          // The settings below limit this problem to methods with
          // many long values which are being run on 32-bit SPARC.
          //
          // ------------------- reg_pressure --------------------
          // Each entry is reg_pressure_per_value,number_of_regs
          //         RegL  RegI  RegFlags   RegF RegD    INTPRESSURE  FLOATPRESSURE
          // IA32     2     1     1          1    1          6           6
          // IA64     1     1     1          1    1         50          41
          // SPARC    2     2     2          2    2         48 (24)     52 (26)
          // SPARCV9  2     2     2          2    2         48 (24)     52 (26)
          // AMD64    1     1     1          1    1         14          15
          // -----------------------------------------------------
          lrg.set_reg_pressure(1);  // normally one value per register
          if( n_type->isa_oop_ptr() ) {
            lrg._is_oop = 1;
          }
          break;
        case Op_RegL:           // Check for long or double
        case Op_RegD:
          lrg.set_num_regs(2);
          // Define platform specific register pressure
#if defined(ARM32)
          lrg.set_reg_pressure(2);
#elif defined(IA32)
          if( ireg == Op_RegL ) {
            lrg.set_reg_pressure(2);
          } else {
            lrg.set_reg_pressure(1);
          }
#else
          lrg.set_reg_pressure(1);  // normally one value per register
#endif
          // If this def of a double forces a mis-aligned double,
          // flag as '_fat_proj' - really flag as allowing misalignment
          // AND changes how we count interferences.  A mis-aligned
          // double can interfere with TWO aligned pairs, or effectively
          // FOUR registers!
          if (rm.is_misaligned_pair()) {
            lrg._fat_proj = 1;
            lrg._is_bound = 1;
          }
          break;
        case Op_RegVectMask:
          lrg.set_num_regs(RegMask::SlotsPerRegVectMask);
          lrg.set_reg_pressure(1);
          break;
        case Op_RegF:
        case Op_RegI:
        case Op_RegN:
        case Op_RegFlags:
        case 0:                 // not an ideal register
          lrg.set_num_regs(1);
          lrg.set_reg_pressure(1);
          break;
        case Op_VecA:
          assert(Matcher::supports_scalable_vector(), "does not support scalable vector");
          assert(RegMask::num_registers(Op_VecA) == RegMask::SlotsPerVecA, "sanity");
          assert(lrgmask.is_aligned_sets(RegMask::SlotsPerVecA), "vector should be aligned");
          lrg.set_num_regs(RegMask::SlotsPerVecA);
          lrg.set_reg_pressure(1);
          break;
        case Op_VecS:
          assert(Matcher::vector_size_supported(T_BYTE,4), "sanity");
          assert(RegMask::num_registers(Op_VecS) == RegMask::SlotsPerVecS, "sanity");
          lrg.set_num_regs(RegMask::SlotsPerVecS);
          lrg.set_reg_pressure(1);
          break;
        case Op_VecD:
          assert(Matcher::vector_size_supported(T_FLOAT,RegMask::SlotsPerVecD), "sanity");
          assert(RegMask::num_registers(Op_VecD) == RegMask::SlotsPerVecD, "sanity");
          assert(lrgmask.is_aligned_sets(RegMask::SlotsPerVecD), "vector should be aligned");
          lrg.set_num_regs(RegMask::SlotsPerVecD);
          lrg.set_reg_pressure(1);
          break;
        case Op_VecX:
          assert(Matcher::vector_size_supported(T_FLOAT,RegMask::SlotsPerVecX), "sanity");
          assert(RegMask::num_registers(Op_VecX) == RegMask::SlotsPerVecX, "sanity");
          assert(lrgmask.is_aligned_sets(RegMask::SlotsPerVecX), "vector should be aligned");
          lrg.set_num_regs(RegMask::SlotsPerVecX);
          lrg.set_reg_pressure(1);
          break;
        case Op_VecY:
          assert(Matcher::vector_size_supported(T_FLOAT,RegMask::SlotsPerVecY), "sanity");
          assert(RegMask::num_registers(Op_VecY) == RegMask::SlotsPerVecY, "sanity");
          assert(lrgmask.is_aligned_sets(RegMask::SlotsPerVecY), "vector should be aligned");
          lrg.set_num_regs(RegMask::SlotsPerVecY);
          lrg.set_reg_pressure(1);
          break;
        case Op_VecZ:
          assert(Matcher::vector_size_supported(T_FLOAT,RegMask::SlotsPerVecZ), "sanity");
          assert(RegMask::num_registers(Op_VecZ) == RegMask::SlotsPerVecZ, "sanity");
          assert(lrgmask.is_aligned_sets(RegMask::SlotsPerVecZ), "vector should be aligned");
          lrg.set_num_regs(RegMask::SlotsPerVecZ);
          lrg.set_reg_pressure(1);
          break;
        default:
          ShouldNotReachHere();
        }
      }

      // Now do the same for inputs
      uint cnt = n->req();
      // Setup for CISC SPILLING
      uint inp = (uint)AdlcVMDeps::Not_cisc_spillable;
      if( UseCISCSpill && after_aggressive ) {
        inp = n->cisc_operand();
        if( inp != (uint)AdlcVMDeps::Not_cisc_spillable )
          // Convert operand number to edge index number
          inp = n->as_Mach()->operand_index(inp);
      }

      // Prepare register mask for each input
      for( uint k = input_edge_start; k < cnt; k++ ) {
        uint vreg = _lrg_map.live_range_id(n->in(k));
        if (!vreg) {
          continue;
        }

        // If this instruction is CISC Spillable, add the flags
        // bit to its appropriate input
        if( UseCISCSpill && after_aggressive && inp == k ) {
#ifndef PRODUCT
          if( TraceCISCSpill ) {
            tty->print("  use_cisc_RegMask: ");
            n->dump();
          }
#endif
          n->as_Mach()->use_cisc_RegMask();
        }

        if (is_machine_node && _scheduling_info_generated) {
          MachNode* cur_node = n->as_Mach();
          // this is cleaned up by register allocation
          if (k >= cur_node->num_opnds()) continue;
        }

        LRG &lrg = lrgs(vreg);
        // // Testing for floating point code shape
        // Node *test = n->in(k);
        // if( test->is_Mach() ) {
        //   MachNode *m = test->as_Mach();
        //   int  op = m->ideal_Opcode();
        //   if (n->is_Call() && (op == Op_AddF || op == Op_MulF) ) {
        //     int zzz = 1;
        //   }
        // }

        // Limit result register mask to acceptable registers.
        // Do not limit registers from uncommon uses before
        // AggressiveCoalesce.  This effectively pre-virtual-splits
        // around uncommon uses of common defs.
        const RegMask &rm = n->in_RegMask(k);
        if (!after_aggressive && _cfg.get_block_for_node(n->in(k))->_freq > 1000 * block->_freq) {
          // Since we are BEFORE aggressive coalesce, leave the register
          // mask untrimmed by the call.  This encourages more coalescing.
          // Later, AFTER aggressive, this live range will have to spill
          // but the spiller handles slow-path calls very nicely.
        } else {
          lrg.AND( rm );
        }

        // Check for bound register masks
        const RegMask &lrgmask = lrg.mask();
        uint kreg = n->in(k)->ideal_reg();
        bool is_vect = RegMask::is_vector(kreg);
        assert(n->in(k)->bottom_type()->isa_vect() == NULL || is_vect ||
               kreg == Op_RegD || kreg == Op_RegL || kreg == Op_RegVectMask,
               "vector must be in vector registers");
        if (lrgmask.is_bound(kreg))
          lrg._is_bound = 1;

        // If this use of a double forces a mis-aligned double,
        // flag as '_fat_proj' - really flag as allowing misalignment
        // AND changes how we count interferences.  A mis-aligned
        // double can interfere with TWO aligned pairs, or effectively
        // FOUR registers!
#ifdef ASSERT
        if (is_vect && !_scheduling_info_generated) {
          if (lrg.num_regs() != 0) {
            assert(lrgmask.is_aligned_sets(lrg.num_regs()), "vector should be aligned");
            assert(!lrg._fat_proj, "sanity");
            assert(RegMask::num_registers(kreg) == lrg.num_regs(), "sanity");
          } else {
            assert(n->is_Phi(), "not all inputs processed only if Phi");
          }
        }
#endif
        if (!is_vect && lrg.num_regs() == 2 && !lrg._fat_proj && rm.is_misaligned_pair()) {
          lrg._fat_proj = 1;
          lrg._is_bound = 1;
        }
        // if the LRG is an unaligned pair, we will have to spill
        // so clear the LRG's register mask if it is not already spilled
        if (!is_vect && !n->is_SpillCopy() &&
            (lrg._def == NULL || lrg.is_multidef() || !lrg._def->is_SpillCopy()) &&
            lrgmask.is_misaligned_pair()) {
          lrg.Clear();
        }

        // Check for maximum frequency value
        if (lrg._maxfreq < block->_freq) {
          lrg._maxfreq = block->_freq;
        }

      } // End for all allocated inputs
    } // end for all instructions
  } // end for all blocks

  // Final per-liverange setup
  for (uint i2 = 0; i2 < _lrg_map.max_lrg_id(); i2++) {
    LRG &lrg = lrgs(i2);
    assert(!lrg._is_vector || !lrg._fat_proj, "sanity");
    if (lrg.num_regs() > 1 && !lrg._fat_proj) {
      lrg.clear_to_sets();
    }
    lrg.compute_set_mask_size();
    if (lrg.not_free()) {      // Handle case where we lose from the start
      lrg.set_reg(OptoReg::Name(LRG::SPILL_REG));
      lrg._direct_conflict = 1;
    }
    lrg.set_degree(0);          // no neighbors in IFG yet
  }
}

// Set the was-lo-degree bit.  Conservative coalescing should not change the
// colorability of the graph.  If any live range was of low-degree before
// coalescing, it should Simplify.  This call sets the was-lo-degree bit.
// The bit is checked in Simplify.
void PhaseChaitin::set_was_low() {
#ifdef ASSERT
  for (uint i = 1; i < _lrg_map.max_lrg_id(); i++) {
    int size = lrgs(i).num_regs();
    uint old_was_lo = lrgs(i)._was_lo;
    lrgs(i)._was_lo = 0;
    if( lrgs(i).lo_degree() ) {
      lrgs(i)._was_lo = 1;      // Trivially of low degree
    } else {                    // Else check the Brigg's assertion
      // Brigg's observation is that the lo-degree neighbors of a
      // hi-degree live range will not interfere with the color choices
      // of said hi-degree live range.  The Simplify reverse-stack-coloring
      // order takes care of the details.  Hence you do not have to count
      // low-degree neighbors when determining if this guy colors.
      int briggs_degree = 0;
      IndexSet *s = _ifg->neighbors(i);
      IndexSetIterator elements(s);
      uint lidx;
      while((lidx = elements.next()) != 0) {
        if( !lrgs(lidx).lo_degree() )
          briggs_degree += MAX2(size,lrgs(lidx).num_regs());
      }
      if( briggs_degree < lrgs(i).degrees_of_freedom() )
        lrgs(i)._was_lo = 1;    // Low degree via the briggs assertion
    }
    assert(old_was_lo <= lrgs(i)._was_lo, "_was_lo may not decrease");
  }
#endif
}

// Compute cost/area ratio, in case we spill.  Build the lo-degree list.
void PhaseChaitin::cache_lrg_info( ) {
  Compile::TracePhase tp("chaitinCacheLRG", &timers[_t_chaitinCacheLRG]);

  for (uint i = 1; i < _lrg_map.max_lrg_id(); i++) {
    LRG &lrg = lrgs(i);

    // Check for being of low degree: means we can be trivially colored.
    // Low degree, dead or must-spill guys just get to simplify right away
    if( lrg.lo_degree() ||
       !lrg.alive() ||
        lrg._must_spill ) {
      // Split low degree list into those guys that must get a
      // register and those that can go to register or stack.
      // The idea is LRGs that can go register or stack color first when
      // they have a good chance of getting a register.  The register-only
      // lo-degree live ranges always get a register.
      OptoReg::Name hi_reg = lrg.mask().find_last_elem();
      if( OptoReg::is_stack(hi_reg)) { // Can go to stack?
        lrg._next = _lo_stk_degree;
        _lo_stk_degree = i;
      } else {
        lrg._next = _lo_degree;
        _lo_degree = i;
      }
    } else {                    // Else high degree
      lrgs(_hi_degree)._prev = i;
      lrg._next = _hi_degree;
      lrg._prev = 0;
      _hi_degree = i;
    }
  }
}

// Simplify the IFG by removing LRGs of low degree.
void PhaseChaitin::Simplify( ) {
  Compile::TracePhase tp("chaitinSimplify", &timers[_t_chaitinSimplify]);

  while( 1 ) {                  // Repeat till simplified it all
    // May want to explore simplifying lo_degree before _lo_stk_degree.
    // This might result in more spills coloring into registers during
    // Select().
    while( _lo_degree || _lo_stk_degree ) {
      // If possible, pull from lo_stk first
      uint lo;
      if( _lo_degree ) {
        lo = _lo_degree;
        _lo_degree = lrgs(lo)._next;
      } else {
        lo = _lo_stk_degree;
        _lo_stk_degree = lrgs(lo)._next;
      }

      // Put the simplified guy on the simplified list.
      lrgs(lo)._next = _simplified;
      _simplified = lo;
      // If this guy is "at risk" then mark his current neighbors
      if (lrgs(lo)._at_risk && !_ifg->neighbors(lo)->is_empty()) {
        IndexSetIterator elements(_ifg->neighbors(lo));
        uint datum;
        while ((datum = elements.next()) != 0) {
          lrgs(datum)._risk_bias = lo;
        }
      }

      // Yank this guy from the IFG.
      IndexSet *adj = _ifg->remove_node(lo);
      if (adj->is_empty()) {
        continue;
      }

      // If any neighbors' degrees fall below their number of
      // allowed registers, then put that neighbor on the low degree
      // list.  Note that 'degree' can only fall and 'numregs' is
      // unchanged by this action.  Thus the two are equal at most once,
      // so LRGs hit the lo-degree worklist at most once.
      IndexSetIterator elements(adj);
      uint neighbor;
      while ((neighbor = elements.next()) != 0) {
        LRG *n = &lrgs(neighbor);
#ifdef ASSERT
        if (VerifyRegisterAllocator) {
          assert( _ifg->effective_degree(neighbor) == n->degree(), "" );
        }
#endif

        // Check for just becoming of-low-degree just counting registers.
        // _must_spill live ranges are already on the low degree list.
        if (n->just_lo_degree() && !n->_must_spill) {
          assert(!_ifg->_yanked->test(neighbor), "Cannot move to lo degree twice");
          // Pull from hi-degree list
          uint prev = n->_prev;
          uint next = n->_next;
          if (prev) {
            lrgs(prev)._next = next;
          } else {
            _hi_degree = next;
          }
          lrgs(next)._prev = prev;
          n->_next = _lo_degree;
          _lo_degree = neighbor;
        }
      }
    } // End of while lo-degree/lo_stk_degree worklist not empty

    // Check for got everything: is hi-degree list empty?
    if (!_hi_degree) break;

    // Time to pick a potential spill guy
    uint lo_score = _hi_degree;
    double score = lrgs(lo_score).score();
    double area = lrgs(lo_score)._area;
    double cost = lrgs(lo_score)._cost;
    bool bound = lrgs(lo_score)._is_bound;

    // Find cheapest guy
    debug_only( int lo_no_simplify=0; );
    for (uint i = _hi_degree; i; i = lrgs(i)._next) {
      assert(!_ifg->_yanked->test(i), "");
      // It's just vaguely possible to move hi-degree to lo-degree without
      // going through a just-lo-degree stage: If you remove a double from
      // a float live range it's degree will drop by 2 and you can skip the
      // just-lo-degree stage.  It's very rare (shows up after 5000+ methods
      // in -Xcomp of Java2Demo).  So just choose this guy to simplify next.
      if( lrgs(i).lo_degree() ) {
        lo_score = i;
        break;
      }
      debug_only( if( lrgs(i)._was_lo ) lo_no_simplify=i; );
      double iscore = lrgs(i).score();
      double iarea = lrgs(i)._area;
      double icost = lrgs(i)._cost;
      bool ibound = lrgs(i)._is_bound;

      // Compare cost/area of i vs cost/area of lo_score.  Smaller cost/area
      // wins.  Ties happen because all live ranges in question have spilled
      // a few times before and the spill-score adds a huge number which
      // washes out the low order bits.  We are choosing the lesser of 2
      // evils; in this case pick largest area to spill.
      // Ties also happen when live ranges are defined and used only inside
      // one block. In which case their area is 0 and score set to max.
      // In such case choose bound live range over unbound to free registers
      // or with smaller cost to spill.
      if ( iscore < score ||
          (iscore == score && iarea > area && lrgs(lo_score)._was_spilled2) ||
          (iscore == score && iarea == area &&
           ( (ibound && !bound) || (ibound == bound && (icost < cost)) )) ) {
        lo_score = i;
        score = iscore;
        area = iarea;
        cost = icost;
        bound = ibound;
      }
    }
    LRG *lo_lrg = &lrgs(lo_score);
    // The live range we choose for spilling is either hi-degree, or very
    // rarely it can be low-degree.  If we choose a hi-degree live range
    // there better not be any lo-degree choices.
    assert( lo_lrg->lo_degree() || !lo_no_simplify, "Live range was lo-degree before coalesce; should simplify" );

    // Pull from hi-degree list
    uint prev = lo_lrg->_prev;
    uint next = lo_lrg->_next;
    if( prev ) lrgs(prev)._next = next;
    else _hi_degree = next;
    lrgs(next)._prev = prev;
    // Jam him on the lo-degree list, despite his high degree.
    // Maybe he'll get a color, and maybe he'll spill.
    // Only Select() will know.
    lrgs(lo_score)._at_risk = true;
    _lo_degree = lo_score;
    lo_lrg->_next = 0;

  } // End of while not simplified everything

}

// Is 'reg' register legal for 'lrg'?
static bool is_legal_reg(LRG &lrg, OptoReg::Name reg, int chunk) {
  if (reg >= chunk && reg < (chunk + RegMask::CHUNK_SIZE) &&
      lrg.mask().Member(OptoReg::add(reg,-chunk))) {
    // RA uses OptoReg which represent the highest element of a registers set.
    // For example, vectorX (128bit) on x86 uses [XMM,XMMb,XMMc,XMMd] set
    // in which XMMd is used by RA to represent such vectors. A double value
    // uses [XMM,XMMb] pairs and XMMb is used by RA for it.
    // The register mask uses largest bits set of overlapping register sets.
    // On x86 with AVX it uses 8 bits for each XMM registers set.
    //
    // The 'lrg' already has cleared-to-set register mask (done in Select()
    // before calling choose_color()). Passing mask.Member(reg) check above
    // indicates that the size (num_regs) of 'reg' set is less or equal to
    // 'lrg' set size.
    // For set size 1 any register which is member of 'lrg' mask is legal.
    if (lrg.num_regs()==1)
      return true;
    // For larger sets only an aligned register with the same set size is legal.
    int mask = lrg.num_regs()-1;
    if ((reg&mask) == mask)
      return true;
  }
  return false;
}

static OptoReg::Name find_first_set(LRG &lrg, RegMask mask, int chunk) {
  int num_regs = lrg.num_regs();
  OptoReg::Name assigned = mask.find_first_set(lrg, num_regs);

  if (lrg.is_scalable()) {
    // a physical register is found
    if (chunk == 0 && OptoReg::is_reg(assigned)) {
      return assigned;
    }

    // find available stack slots for scalable register
    if (lrg._is_vector) {
      num_regs = lrg.scalable_reg_slots();
      // if actual scalable vector register is exactly SlotsPerVecA * 32 bits
      if (num_regs == RegMask::SlotsPerVecA) {
        return assigned;
      }

      // mask has been cleared out by clear_to_sets(SlotsPerVecA) before choose_color, but it
      // does not work for scalable size. We have to find adjacent scalable_reg_slots() bits
      // instead of SlotsPerVecA bits.
      assigned = mask.find_first_set(lrg, num_regs); // find highest valid reg
      while (OptoReg::is_valid(assigned) && RegMask::can_represent(assigned)) {
        // Verify the found reg has scalable_reg_slots() bits set.
        if (mask.is_valid_reg(assigned, num_regs)) {
          return assigned;
        } else {
          // Remove more for each iteration
          mask.Remove(assigned - num_regs + 1); // Unmask the lowest reg
          mask.clear_to_sets(RegMask::SlotsPerVecA); // Align by SlotsPerVecA bits
          assigned = mask.find_first_set(lrg, num_regs);
        }
      }
      return OptoReg::Bad; // will cause chunk change, and retry next chunk
    }
  }

  return assigned;
}

// Choose a color using the biasing heuristic
OptoReg::Name PhaseChaitin::bias_color( LRG &lrg, int chunk ) {

  // Check for "at_risk" LRG's
  uint risk_lrg = _lrg_map.find(lrg._risk_bias);
  if (risk_lrg != 0 && !_ifg->neighbors(risk_lrg)->is_empty()) {
    // Walk the colored neighbors of the "at_risk" candidate
    // Choose a color which is both legal and already taken by a neighbor
    // of the "at_risk" candidate in order to improve the chances of the
    // "at_risk" candidate of coloring
    IndexSetIterator elements(_ifg->neighbors(risk_lrg));
    uint datum;
    while ((datum = elements.next()) != 0) {
      OptoReg::Name reg = lrgs(datum).reg();
      // If this LRG's register is legal for us, choose it
      if (is_legal_reg(lrg, reg, chunk))
        return reg;
    }
  }

  uint copy_lrg = _lrg_map.find(lrg._copy_bias);
  if (copy_lrg != 0) {
    // If he has a color,
    if(!_ifg->_yanked->test(copy_lrg)) {
      OptoReg::Name reg = lrgs(copy_lrg).reg();
      //  And it is legal for you,
      if (is_legal_reg(lrg, reg, chunk))
        return reg;
    } else if( chunk == 0 ) {
      // Choose a color which is legal for him
      RegMask tempmask = lrg.mask();
      tempmask.AND(lrgs(copy_lrg).mask());
      tempmask.clear_to_sets(lrg.num_regs());
      OptoReg::Name reg = find_first_set(lrg, tempmask, chunk);
      if (OptoReg::is_valid(reg))
        return reg;
    }
  }

  // If no bias info exists, just go with the register selection ordering
  if (lrg._is_vector || lrg.num_regs() == 2) {
    // Find an aligned set
    return OptoReg::add(find_first_set(lrg, lrg.mask(), chunk), chunk);
  }

  // CNC - Fun hack.  Alternate 1st and 2nd selection.  Enables post-allocate
  // copy removal to remove many more copies, by preventing a just-assigned
  // register from being repeatedly assigned.
  OptoReg::Name reg = lrg.mask().find_first_elem();
  if( (++_alternate & 1) && OptoReg::is_valid(reg) ) {
    // This 'Remove; find; Insert' idiom is an expensive way to find the
    // SECOND element in the mask.
    lrg.Remove(reg);
    OptoReg::Name reg2 = lrg.mask().find_first_elem();
    lrg.Insert(reg);
    if( OptoReg::is_reg(reg2))
      reg = reg2;
  }
  return OptoReg::add( reg, chunk );
}

// Choose a color in the current chunk
OptoReg::Name PhaseChaitin::choose_color( LRG &lrg, int chunk ) {
  assert( C->in_preserve_stack_slots() == 0 || chunk != 0 || lrg._is_bound || lrg.mask().is_bound1() || !lrg.mask().Member(OptoReg::Name(_matcher._old_SP-1)), "must not allocate stack0 (inside preserve area)");
  assert(C->out_preserve_stack_slots() == 0 || chunk != 0 || lrg._is_bound || lrg.mask().is_bound1() || !lrg.mask().Member(OptoReg::Name(_matcher._old_SP+0)), "must not allocate stack0 (inside preserve area)");

  if( lrg.num_regs() == 1 ||    // Common Case
      !lrg._fat_proj )          // Aligned+adjacent pairs ok
    // Use a heuristic to "bias" the color choice
    return bias_color(lrg, chunk);

  assert(!lrg._is_vector, "should be not vector here" );
  assert( lrg.num_regs() >= 2, "dead live ranges do not color" );

  // Fat-proj case or misaligned double argument.
  assert(lrg.compute_mask_size() == lrg.num_regs() ||
         lrg.num_regs() == 2,"fat projs exactly color" );
  assert( !chunk, "always color in 1st chunk" );
  // Return the highest element in the set.
  return lrg.mask().find_last_elem();
}

// Select colors by re-inserting LRGs back into the IFG.  LRGs are re-inserted
// in reverse order of removal.  As long as nothing of hi-degree was yanked,
// everything going back is guaranteed a color.  Select that color.  If some
// hi-degree LRG cannot get a color then we record that we must spill.
uint PhaseChaitin::Select( ) {
  Compile::TracePhase tp("chaitinSelect", &timers[_t_chaitinSelect]);

  uint spill_reg = LRG::SPILL_REG;
  _max_reg = OptoReg::Name(0);  // Past max register used
  while( _simplified ) {
    // Pull next LRG from the simplified list - in reverse order of removal
    uint lidx = _simplified;
    LRG *lrg = &lrgs(lidx);
    _simplified = lrg->_next;

#ifndef PRODUCT
    if (trace_spilling()) {
      ttyLocker ttyl;
      tty->print_cr("L%d selecting degree %d degrees_of_freedom %d", lidx, lrg->degree(),
                    lrg->degrees_of_freedom());
      lrg->dump();
    }
#endif

    // Re-insert into the IFG
    _ifg->re_insert(lidx);
    if( !lrg->alive() ) continue;
    // capture allstackedness flag before mask is hacked
    const int is_allstack = lrg->mask().is_AllStack();

    // Yeah, yeah, yeah, I know, I know.  I can refactor this
    // to avoid the GOTO, although the refactored code will not
    // be much clearer.  We arrive here IFF we have a stack-based
    // live range that cannot color in the current chunk, and it
    // has to move into the next free stack chunk.
    int chunk = 0;              // Current chunk is first chunk
    retry_next_chunk:

    // Remove neighbor colors
    IndexSet *s = _ifg->neighbors(lidx);
    debug_only(RegMask orig_mask = lrg->mask();)

    if (!s->is_empty()) {
      IndexSetIterator elements(s);
      uint neighbor;
      while ((neighbor = elements.next()) != 0) {
        // Note that neighbor might be a spill_reg.  In this case, exclusion
        // of its color will be a no-op, since the spill_reg chunk is in outer
        // space.  Also, if neighbor is in a different chunk, this exclusion
        // will be a no-op.  (Later on, if lrg runs out of possible colors in
        // its chunk, a new chunk of color may be tried, in which case
        // examination of neighbors is started again, at retry_next_chunk.)
        LRG &nlrg = lrgs(neighbor);
        OptoReg::Name nreg = nlrg.reg();
        // Only subtract masks in the same chunk
        if (nreg >= chunk && nreg < chunk + RegMask::CHUNK_SIZE) {
#ifndef PRODUCT
          uint size = lrg->mask().Size();
          RegMask rm = lrg->mask();
#endif
          lrg->SUBTRACT(nlrg.mask());
#ifndef PRODUCT
          if (trace_spilling() && lrg->mask().Size() != size) {
            ttyLocker ttyl;
            tty->print("L%d ", lidx);
            rm.dump();
            tty->print(" intersected L%d ", neighbor);
            nlrg.mask().dump();
            tty->print(" removed ");
            rm.SUBTRACT(lrg->mask());
            rm.dump();
            tty->print(" leaving ");
            lrg->mask().dump();
            tty->cr();
          }
#endif
        }
      }
    }
    //assert(is_allstack == lrg->mask().is_AllStack(), "nbrs must not change AllStackedness");
    // Aligned pairs need aligned masks
    assert(!lrg->_is_vector || !lrg->_fat_proj, "sanity");
    if (lrg->num_regs() > 1 && !lrg->_fat_proj) {
      lrg->clear_to_sets();
    }

    // Check if a color is available and if so pick the color
    OptoReg::Name reg = choose_color( *lrg, chunk );

    //---------------
    // If we fail to color and the AllStack flag is set, trigger
    // a chunk-rollover event
    if(!OptoReg::is_valid(OptoReg::add(reg,-chunk)) && is_allstack) {
      // Bump register mask up to next stack chunk
      chunk += RegMask::CHUNK_SIZE;
      lrg->Set_All();
      goto retry_next_chunk;
    }

    //---------------
    // Did we get a color?
    else if( OptoReg::is_valid(reg)) {
#ifndef PRODUCT
      RegMask avail_rm = lrg->mask();
#endif

      // Record selected register
      lrg->set_reg(reg);

      if( reg >= _max_reg )     // Compute max register limit
        _max_reg = OptoReg::add(reg,1);
      // Fold reg back into normal space
      reg = OptoReg::add(reg,-chunk);

      // If the live range is not bound, then we actually had some choices
      // to make.  In this case, the mask has more bits in it than the colors
      // chosen.  Restrict the mask to just what was picked.
      int n_regs = lrg->num_regs();
      assert(!lrg->_is_vector || !lrg->_fat_proj, "sanity");
      if (n_regs == 1 || !lrg->_fat_proj) {
        if (Matcher::supports_scalable_vector()) {
          assert(!lrg->_is_vector || n_regs <= RegMask::SlotsPerVecA, "sanity");
        } else {
          assert(!lrg->_is_vector || n_regs <= RegMask::SlotsPerVecZ, "sanity");
        }
        lrg->Clear();           // Clear the mask
        lrg->Insert(reg);       // Set regmask to match selected reg
        // For vectors and pairs, also insert the low bit of the pair
        // We always choose the high bit, then mask the low bits by register size
        if (lrg->is_scalable() && OptoReg::is_stack(lrg->reg())) { // stack
          n_regs = lrg->scalable_reg_slots();
        }
        for (int i = 1; i < n_regs; i++) {
          lrg->Insert(OptoReg::add(reg,-i));
        }
        lrg->set_mask_size(n_regs);
      } else {                  // Else fatproj
        // mask must be equal to fatproj bits, by definition
      }
#ifndef PRODUCT
      if (trace_spilling()) {
        ttyLocker ttyl;
        tty->print("L%d selected ", lidx);
        lrg->mask().dump();
        tty->print(" from ");
        avail_rm.dump();
        tty->cr();
      }
#endif
      // Note that reg is the highest-numbered register in the newly-bound mask.
    } // end color available case

    //---------------
    // Live range is live and no colors available
    else {
      assert( lrg->alive(), "" );
      assert( !lrg->_fat_proj || lrg->is_multidef() ||
              lrg->_def->outcnt() > 0, "fat_proj cannot spill");
      assert( !orig_mask.is_AllStack(), "All Stack does not spill" );

      // Assign the special spillreg register
      lrg->set_reg(OptoReg::Name(spill_reg++));
      // Do not empty the regmask; leave mask_size lying around
      // for use during Spilling
#ifndef PRODUCT
      if( trace_spilling() ) {
        ttyLocker ttyl;
        tty->print("L%d spilling with neighbors: ", lidx);
        s->dump();
        debug_only(tty->print(" original mask: "));
        debug_only(orig_mask.dump());
        dump_lrg(lidx);
      }
#endif
    } // end spill case

  }

  return spill_reg-LRG::SPILL_REG;      // Return number of spills
}

// Set the 'spilled_once' or 'spilled_twice' flag on a node.
void PhaseChaitin::set_was_spilled( Node *n ) {
  if( _spilled_once.test_set(n->_idx) )
    _spilled_twice.set(n->_idx);
}

// Convert Ideal spill instructions into proper FramePtr + offset Loads and
// Stores.  Use-def chains are NOT preserved, but Node->LRG->reg maps are.
void PhaseChaitin::fixup_spills() {
  // This function does only cisc spill work.
  if( !UseCISCSpill ) return;

  Compile::TracePhase tp("fixupSpills", &timers[_t_fixupSpills]);

  // Grab the Frame Pointer
  Node *fp = _cfg.get_root_block()->head()->in(1)->in(TypeFunc::FramePtr);

  // For all blocks
  for (uint i = 0; i < _cfg.number_of_blocks(); i++) {
    Block* block = _cfg.get_block(i);

    // For all instructions in block
    uint last_inst = block->end_idx();
    for (uint j = 1; j <= last_inst; j++) {
      Node* n = block->get_node(j);

      // Dead instruction???
      assert( n->outcnt() != 0 ||// Nothing dead after post alloc
              C->top() == n ||  // Or the random TOP node
              n->is_Proj(),     // Or a fat-proj kill node
              "No dead instructions after post-alloc" );

      int inp = n->cisc_operand();
      if( inp != AdlcVMDeps::Not_cisc_spillable ) {
        // Convert operand number to edge index number
        MachNode *mach = n->as_Mach();
        inp = mach->operand_index(inp);
        Node *src = n->in(inp);   // Value to load or store
        LRG &lrg_cisc = lrgs(_lrg_map.find_const(src));
        OptoReg::Name src_reg = lrg_cisc.reg();
        // Doubles record the HIGH register of an adjacent pair.
        src_reg = OptoReg::add(src_reg,1-lrg_cisc.num_regs());
        if( OptoReg::is_stack(src_reg) ) { // If input is on stack
          // This is a CISC Spill, get stack offset and construct new node
#ifndef PRODUCT
          if( TraceCISCSpill ) {
            tty->print("    reg-instr:  ");
            n->dump();
          }
#endif
          int stk_offset = reg2offset(src_reg);
          // Bailout if we might exceed node limit when spilling this instruction
          C->check_node_count(0, "out of nodes fixing spills");
          if (C->failing())  return;
          // Transform node
          MachNode *cisc = mach->cisc_version(stk_offset)->as_Mach();
          cisc->set_req(inp,fp);          // Base register is frame pointer
          if( cisc->oper_input_base() > 1 && mach->oper_input_base() <= 1 ) {
            assert( cisc->oper_input_base() == 2, "Only adding one edge");
            cisc->ins_req(1,src);         // Requires a memory edge
          }
          block->map_node(cisc, j);          // Insert into basic block
          n->subsume_by(cisc, C); // Correct graph
          //
          ++_used_cisc_instructions;
#ifndef PRODUCT
          if( TraceCISCSpill ) {
            tty->print("    cisc-instr: ");
            cisc->dump();
          }
#endif
        } else {
#ifndef PRODUCT
          if( TraceCISCSpill ) {
            tty->print("    using reg-instr: ");
            n->dump();
          }
#endif
          ++_unused_cisc_instructions;    // input can be on stack
        }
      }

    } // End of for all instructions

  } // End of for all blocks
}

// Helper to stretch above; recursively discover the base Node for a
// given derived Node.  Easy for AddP-related machine nodes, but needs
// to be recursive for derived Phis.
Node *PhaseChaitin::find_base_for_derived( Node **derived_base_map, Node *derived, uint &maxlrg ) {
  // See if already computed; if so return it
  if( derived_base_map[derived->_idx] )
    return derived_base_map[derived->_idx];

  // See if this happens to be a base.
  // NOTE: we use TypePtr instead of TypeOopPtr because we can have
  // pointers derived from NULL!  These are always along paths that
  // can't happen at run-time but the optimizer cannot deduce it so
  // we have to handle it gracefully.
  assert(!derived->bottom_type()->isa_narrowoop() ||
          derived->bottom_type()->make_ptr()->is_ptr()->_offset == 0, "sanity");
  const TypePtr *tj = derived->bottom_type()->isa_ptr();
  // If its an OOP with a non-zero offset, then it is derived.
  if( tj == NULL || tj->_offset == 0 ) {
    derived_base_map[derived->_idx] = derived;
    return derived;
  }
  // Derived is NULL+offset?  Base is NULL!
  if( derived->is_Con() ) {
    Node *base = _matcher.mach_null();
    assert(base != NULL, "sanity");
    if (base->in(0) == NULL) {
      // Initialize it once and make it shared:
      // set control to _root and place it into Start block
      // (where top() node is placed).
      base->init_req(0, _cfg.get_root_node());
      Block *startb = _cfg.get_block_for_node(C->top());
      uint node_pos = startb->find_node(C->top());
      startb->insert_node(base, node_pos);
      _cfg.map_node_to_block(base, startb);
      assert(_lrg_map.live_range_id(base) == 0, "should not have LRG yet");

      // The loadConP0 might have projection nodes depending on architecture
      // Add the projection nodes to the CFG
      for (DUIterator_Fast imax, i = base->fast_outs(imax); i < imax; i++) {
        Node* use = base->fast_out(i);
        if (use->is_MachProj()) {
          startb->insert_node(use, ++node_pos);
          _cfg.map_node_to_block(use, startb);
          new_lrg(use, maxlrg++);
        }
      }
    }
    if (_lrg_map.live_range_id(base) == 0) {
      new_lrg(base, maxlrg++);
    }
    assert(base->in(0) == _cfg.get_root_node() && _cfg.get_block_for_node(base) == _cfg.get_block_for_node(C->top()), "base NULL should be shared");
    derived_base_map[derived->_idx] = base;
    return base;
  }

  // Check for AddP-related opcodes
  if (!derived->is_Phi()) {
    assert(derived->as_Mach()->ideal_Opcode() == Op_AddP, "but is: %s", derived->Name());
    Node *base = derived->in(AddPNode::Base);
    derived_base_map[derived->_idx] = base;
    return base;
  }

  // Recursively find bases for Phis.
  // First check to see if we can avoid a base Phi here.
  Node *base = find_base_for_derived( derived_base_map, derived->in(1),maxlrg);
  uint i;
  for( i = 2; i < derived->req(); i++ )
    if( base != find_base_for_derived( derived_base_map,derived->in(i),maxlrg))
      break;
  // Went to the end without finding any different bases?
  if( i == derived->req() ) {   // No need for a base Phi here
    derived_base_map[derived->_idx] = base;
    return base;
  }

  // Now we see we need a base-Phi here to merge the bases
  const Type *t = base->bottom_type();
  base = new PhiNode( derived->in(0), t );
  for( i = 1; i < derived->req(); i++ ) {
    base->init_req(i, find_base_for_derived(derived_base_map, derived->in(i), maxlrg));
    t = t->meet(base->in(i)->bottom_type());
  }
  base->as_Phi()->set_type(t);

  // Search the current block for an existing base-Phi
  Block *b = _cfg.get_block_for_node(derived);
  for( i = 1; i <= b->end_idx(); i++ ) {// Search for matching Phi
    Node *phi = b->get_node(i);
    if( !phi->is_Phi() ) {      // Found end of Phis with no match?
      b->insert_node(base,  i); // Must insert created Phi here as base
      _cfg.map_node_to_block(base, b);
      new_lrg(base,maxlrg++);
      break;
    }
    // See if Phi matches.
    uint j;
    for( j = 1; j < base->req(); j++ )
      if( phi->in(j) != base->in(j) &&
          !(phi->in(j)->is_Con() && base->in(j)->is_Con()) ) // allow different NULLs
        break;
    if( j == base->req() ) {    // All inputs match?
      base = phi;               // Then use existing 'phi' and drop 'base'
      break;
    }
  }


  // Cache info for later passes
  derived_base_map[derived->_idx] = base;
  return base;
}

// At each Safepoint, insert extra debug edges for each pair of derived value/
// base pointer that is live across the Safepoint for oopmap building.  The
// edge pairs get added in after sfpt->jvmtail()->oopoff(), but are in the
// required edge set.
bool PhaseChaitin::stretch_base_pointer_live_ranges(ResourceArea *a) {
  int must_recompute_live = false;
  uint maxlrg = _lrg_map.max_lrg_id();
  Node **derived_base_map = (Node**)a->Amalloc(sizeof(Node*)*C->unique());
  memset( derived_base_map, 0, sizeof(Node*)*C->unique() );

  // For all blocks in RPO do...
  for (uint i = 0; i < _cfg.number_of_blocks(); i++) {
    Block* block = _cfg.get_block(i);
    // Note use of deep-copy constructor.  I cannot hammer the original
    // liveout bits, because they are needed by the following coalesce pass.
    IndexSet liveout(_live->live(block));

    for (uint j = block->end_idx() + 1; j > 1; j--) {
      Node* n = block->get_node(j - 1);

      // Pre-split compares of loop-phis.  Loop-phis form a cycle we would
      // like to see in the same register.  Compare uses the loop-phi and so
      // extends its live range BUT cannot be part of the cycle.  If this
      // extended live range overlaps with the update of the loop-phi value
      // we need both alive at the same time -- which requires at least 1
      // copy.  But because Intel has only 2-address registers we end up with
      // at least 2 copies, one before the loop-phi update instruction and
      // one after.  Instead we split the input to the compare just after the
      // phi.
      if( n->is_Mach() && n->as_Mach()->ideal_Opcode() == Op_CmpI ) {
        Node *phi = n->in(1);
        if( phi->is_Phi() && phi->as_Phi()->region()->is_Loop() ) {
          Block *phi_block = _cfg.get_block_for_node(phi);
          if (_cfg.get_block_for_node(phi_block->pred(2)) == block) {
            const RegMask *mask = C->matcher()->idealreg2spillmask[Op_RegI];
            Node *spill = new MachSpillCopyNode(MachSpillCopyNode::LoopPhiInput, phi, *mask, *mask);
            insert_proj( phi_block, 1, spill, maxlrg++ );
            n->set_req(1,spill);
            must_recompute_live = true;
          }
        }
      }

      // Get value being defined
      uint lidx = _lrg_map.live_range_id(n);
      // Ignore the occasional brand-new live range
      if (lidx && lidx < _lrg_map.max_lrg_id()) {
        // Remove from live-out set
        liveout.remove(lidx);

        // Copies do not define a new value and so do not interfere.
        // Remove the copies source from the liveout set before interfering.
        uint idx = n->is_Copy();
        if (idx) {
          liveout.remove(_lrg_map.live_range_id(n->in(idx)));
        }
      }

      // Found a safepoint?
      JVMState *jvms = n->jvms();
      if (jvms && !liveout.is_empty()) {
        // Now scan for a live derived pointer
        IndexSetIterator elements(&liveout);
        uint neighbor;
        while ((neighbor = elements.next()) != 0) {
          // Find reaching DEF for base and derived values
          // This works because we are still in SSA during this call.
          Node *derived = lrgs(neighbor)._def;
          const TypePtr *tj = derived->bottom_type()->isa_ptr();
          assert(!derived->bottom_type()->isa_narrowoop() ||
                  derived->bottom_type()->make_ptr()->is_ptr()->_offset == 0, "sanity");
          // If its an OOP with a non-zero offset, then it is derived.
          if( tj && tj->_offset != 0 && tj->isa_oop_ptr() ) {
            Node *base = find_base_for_derived(derived_base_map, derived, maxlrg);
            assert(base->_idx < _lrg_map.size(), "");
            // Add reaching DEFs of derived pointer and base pointer as a
            // pair of inputs
            n->add_req(derived);
            n->add_req(base);

            // See if the base pointer is already live to this point.
            // Since I'm working on the SSA form, live-ness amounts to
            // reaching def's.  So if I find the base's live range then
            // I know the base's def reaches here.
            if ((_lrg_map.live_range_id(base) >= _lrg_map.max_lrg_id() || // (Brand new base (hence not live) or
                 !liveout.member(_lrg_map.live_range_id(base))) && // not live) AND
                 (_lrg_map.live_range_id(base) > 0) && // not a constant
                 _cfg.get_block_for_node(base) != block) { // base not def'd in blk)
              // Base pointer is not currently live.  Since I stretched
              // the base pointer to here and it crosses basic-block
              // boundaries, the global live info is now incorrect.
              // Recompute live.
              must_recompute_live = true;
            } // End of if base pointer is not live to debug info
          }
        } // End of scan all live data for derived ptrs crossing GC point
      } // End of if found a GC point

      // Make all inputs live
      if (!n->is_Phi()) {      // Phi function uses come from prior block
        for (uint k = 1; k < n->req(); k++) {
          uint lidx = _lrg_map.live_range_id(n->in(k));
          if (lidx < _lrg_map.max_lrg_id()) {
            liveout.insert(lidx);
          }
        }
      }

    } // End of forall instructions in block
    liveout.clear();  // Free the memory used by liveout.

  } // End of forall blocks
  _lrg_map.set_max_lrg_id(maxlrg);

  // If I created a new live range I need to recompute live
  if (maxlrg != _ifg->_maxlrg) {
    must_recompute_live = true;
  }

  return must_recompute_live != 0;
}

// Extend the node to LRG mapping

void PhaseChaitin::add_reference(const Node *node, const Node *old_node) {
  _lrg_map.extend(node->_idx, _lrg_map.live_range_id(old_node));
}

#ifndef PRODUCT
void PhaseChaitin::dump(const Node* n) const {
  uint r = (n->_idx < _lrg_map.size()) ? _lrg_map.find_const(n) : 0;
  tty->print("L%d",r);
  if (r && n->Opcode() != Op_Phi) {
    if( _node_regs ) {          // Got a post-allocation copy of allocation?
      tty->print("[");
      OptoReg::Name second = get_reg_second(n);
      if( OptoReg::is_valid(second) ) {
        if( OptoReg::is_reg(second) )
          tty->print("%s:",Matcher::regName[second]);
        else
          tty->print("%s+%d:",OptoReg::regname(OptoReg::c_frame_pointer), reg2offset_unchecked(second));
      }
      OptoReg::Name first = get_reg_first(n);
      if( OptoReg::is_reg(first) )
        tty->print("%s]",Matcher::regName[first]);
      else
         tty->print("%s+%d]",OptoReg::regname(OptoReg::c_frame_pointer), reg2offset_unchecked(first));
    } else
    n->out_RegMask().dump();
  }
  tty->print("/N%d\t",n->_idx);
  tty->print("%s === ", n->Name());
  uint k;
  for (k = 0; k < n->req(); k++) {
    Node *m = n->in(k);
    if (!m) {
      tty->print("_ ");
    }
    else {
      uint r = (m->_idx < _lrg_map.size()) ? _lrg_map.find_const(m) : 0;
      tty->print("L%d",r);
      // Data MultiNode's can have projections with no real registers.
      // Don't die while dumping them.
      int op = n->Opcode();
      if( r && op != Op_Phi && op != Op_Proj && op != Op_SCMemProj) {
        if( _node_regs ) {
          tty->print("[");
          OptoReg::Name second = get_reg_second(n->in(k));
          if( OptoReg::is_valid(second) ) {
            if( OptoReg::is_reg(second) )
              tty->print("%s:",Matcher::regName[second]);
            else
              tty->print("%s+%d:",OptoReg::regname(OptoReg::c_frame_pointer),
                         reg2offset_unchecked(second));
          }
          OptoReg::Name first = get_reg_first(n->in(k));
          if( OptoReg::is_reg(first) )
            tty->print("%s]",Matcher::regName[first]);
          else
            tty->print("%s+%d]",OptoReg::regname(OptoReg::c_frame_pointer),
                       reg2offset_unchecked(first));
        } else
          n->in_RegMask(k).dump();
      }
      tty->print("/N%d ",m->_idx);
    }
  }
  if( k < n->len() && n->in(k) ) tty->print("| ");
  for( ; k < n->len(); k++ ) {
    Node *m = n->in(k);
    if(!m) {
      break;
    }
    uint r = (m->_idx < _lrg_map.size()) ? _lrg_map.find_const(m) : 0;
    tty->print("L%d",r);
    tty->print("/N%d ",m->_idx);
  }
  if( n->is_Mach() ) n->as_Mach()->dump_spec(tty);
  else n->dump_spec(tty);
  if( _spilled_once.test(n->_idx ) ) {
    tty->print(" Spill_1");
    if( _spilled_twice.test(n->_idx ) )
      tty->print(" Spill_2");
  }
  tty->print("\n");
}

void PhaseChaitin::dump(const Block* b) const {
  b->dump_head(&_cfg);

  // For all instructions
  for( uint j = 0; j < b->number_of_nodes(); j++ )
    dump(b->get_node(j));
  // Print live-out info at end of block
  if( _live ) {
    tty->print("Liveout: ");
    IndexSet *live = _live->live(b);
    IndexSetIterator elements(live);
    tty->print("{");
    uint i;
    while ((i = elements.next()) != 0) {
      tty->print("L%d ", _lrg_map.find_const(i));
    }
    tty->print_cr("}");
  }
  tty->print("\n");
}

void PhaseChaitin::dump() const {
  tty->print( "--- Chaitin -- argsize: %d  framesize: %d ---\n",
              _matcher._new_SP, _framesize );

  // For all blocks
  for (uint i = 0; i < _cfg.number_of_blocks(); i++) {
    dump(_cfg.get_block(i));
  }
  // End of per-block dump
  tty->print("\n");

  if (!_ifg) {
    tty->print("(No IFG.)\n");
    return;
  }

  // Dump LRG array
  tty->print("--- Live RanGe Array ---\n");
  for (uint i2 = 1; i2 < _lrg_map.max_lrg_id(); i2++) {
    tty->print("L%d: ",i2);
    if (i2 < _ifg->_maxlrg) {
      lrgs(i2).dump();
    }
    else {
      tty->print_cr("new LRG");
    }
  }
  tty->cr();

  // Dump lo-degree list
  tty->print("Lo degree: ");
  for(uint i3 = _lo_degree; i3; i3 = lrgs(i3)._next )
    tty->print("L%d ",i3);
  tty->cr();

  // Dump lo-stk-degree list
  tty->print("Lo stk degree: ");
  for(uint i4 = _lo_stk_degree; i4; i4 = lrgs(i4)._next )
    tty->print("L%d ",i4);
  tty->cr();

  // Dump lo-degree list
  tty->print("Hi degree: ");
  for(uint i5 = _hi_degree; i5; i5 = lrgs(i5)._next )
    tty->print("L%d ",i5);
  tty->cr();
}

void PhaseChaitin::dump_degree_lists() const {
  // Dump lo-degree list
  tty->print("Lo degree: ");
  for( uint i = _lo_degree; i; i = lrgs(i)._next )
    tty->print("L%d ",i);
  tty->cr();

  // Dump lo-stk-degree list
  tty->print("Lo stk degree: ");
  for(uint i2 = _lo_stk_degree; i2; i2 = lrgs(i2)._next )
    tty->print("L%d ",i2);
  tty->cr();

  // Dump lo-degree list
  tty->print("Hi degree: ");
  for(uint i3 = _hi_degree; i3; i3 = lrgs(i3)._next )
    tty->print("L%d ",i3);
  tty->cr();
}

void PhaseChaitin::dump_simplified() const {
  tty->print("Simplified: ");
  for( uint i = _simplified; i; i = lrgs(i)._next )
    tty->print("L%d ",i);
  tty->cr();
}

static char *print_reg(OptoReg::Name reg, const PhaseChaitin* pc, char* buf) {
  if ((int)reg < 0)
    sprintf(buf, "<OptoReg::%d>", (int)reg);
  else if (OptoReg::is_reg(reg))
    strcpy(buf, Matcher::regName[reg]);
  else
    sprintf(buf,"%s + #%d",OptoReg::regname(OptoReg::c_frame_pointer),
            pc->reg2offset(reg));
  return buf+strlen(buf);
}

// Dump a register name into a buffer.  Be intelligent if we get called
// before allocation is complete.
char *PhaseChaitin::dump_register(const Node* n, char* buf) const {
  if( _node_regs ) {
    // Post allocation, use direct mappings, no LRG info available
    print_reg( get_reg_first(n), this, buf );
  } else {
    uint lidx = _lrg_map.find_const(n); // Grab LRG number
    if( !_ifg ) {
      sprintf(buf,"L%d",lidx);  // No register binding yet
    } else if( !lidx ) {        // Special, not allocated value
      strcpy(buf,"Special");
    } else {
      if (lrgs(lidx)._is_vector) {
        if (lrgs(lidx).mask().is_bound_set(lrgs(lidx).num_regs()))
          print_reg( lrgs(lidx).reg(), this, buf ); // a bound machine register
        else
          sprintf(buf,"L%d",lidx); // No register binding yet
      } else if( (lrgs(lidx).num_regs() == 1)
                 ? lrgs(lidx).mask().is_bound1()
                 : lrgs(lidx).mask().is_bound_pair() ) {
        // Hah!  We have a bound machine register
        print_reg( lrgs(lidx).reg(), this, buf );
      } else {
        sprintf(buf,"L%d",lidx); // No register binding yet
      }
    }
  }
  return buf+strlen(buf);
}

void PhaseChaitin::dump_for_spill_split_recycle() const {
  if( WizardMode && (PrintCompilation || PrintOpto) ) {
    // Display which live ranges need to be split and the allocator's state
    tty->print_cr("Graph-Coloring Iteration %d will split the following live ranges", _trip_cnt);
    for (uint bidx = 1; bidx < _lrg_map.max_lrg_id(); bidx++) {
      if( lrgs(bidx).alive() && lrgs(bidx).reg() >= LRG::SPILL_REG ) {
        tty->print("L%d: ", bidx);
        lrgs(bidx).dump();
      }
    }
    tty->cr();
    dump();
  }
}

void PhaseChaitin::dump_frame() const {
  const char *fp = OptoReg::regname(OptoReg::c_frame_pointer);
  const TypeTuple *domain = C->tf()->domain();
  const int        argcnt = domain->cnt() - TypeFunc::Parms;

  // Incoming arguments in registers dump
  for( int k = 0; k < argcnt; k++ ) {
    OptoReg::Name parmreg = _matcher._parm_regs[k].first();
    if( OptoReg::is_reg(parmreg))  {
      const char *reg_name = OptoReg::regname(parmreg);
      tty->print("#r%3.3d %s", parmreg, reg_name);
      parmreg = _matcher._parm_regs[k].second();
      if( OptoReg::is_reg(parmreg))  {
        tty->print(":%s", OptoReg::regname(parmreg));
      }
      tty->print("   : parm %d: ", k);
      domain->field_at(k + TypeFunc::Parms)->dump();
      tty->cr();
    }
  }

  // Check for un-owned padding above incoming args
  OptoReg::Name reg = _matcher._new_SP;
  if( reg > _matcher._in_arg_limit ) {
    reg = OptoReg::add(reg, -1);
    tty->print_cr("#r%3.3d %s+%2d: pad0, owned by CALLER", reg, fp, reg2offset_unchecked(reg));
  }

  // Incoming argument area dump
  OptoReg::Name begin_in_arg = OptoReg::add(_matcher._old_SP,C->out_preserve_stack_slots());
  while( reg > begin_in_arg ) {
    reg = OptoReg::add(reg, -1);
    tty->print("#r%3.3d %s+%2d: ",reg,fp,reg2offset_unchecked(reg));
    int j;
    for( j = 0; j < argcnt; j++) {
      if( _matcher._parm_regs[j].first() == reg ||
          _matcher._parm_regs[j].second() == reg ) {
        tty->print("parm %d: ",j);
        domain->field_at(j + TypeFunc::Parms)->dump();
        tty->cr();
        break;
      }
    }
    if( j >= argcnt )
      tty->print_cr("HOLE, owned by SELF");
  }

  // Old outgoing preserve area
  while( reg > _matcher._old_SP ) {
    reg = OptoReg::add(reg, -1);
    tty->print_cr("#r%3.3d %s+%2d: old out preserve",reg,fp,reg2offset_unchecked(reg));
  }

  // Old SP
  tty->print_cr("# -- Old %s -- Framesize: %d --",fp,
    reg2offset_unchecked(OptoReg::add(_matcher._old_SP,-1)) - reg2offset_unchecked(_matcher._new_SP)+jintSize);

  // Preserve area dump
  int fixed_slots = C->fixed_slots();
  OptoReg::Name begin_in_preserve = OptoReg::add(_matcher._old_SP, -(int)C->in_preserve_stack_slots());
  OptoReg::Name return_addr = _matcher.return_addr();

  reg = OptoReg::add(reg, -1);
  while (OptoReg::is_stack(reg)) {
    tty->print("#r%3.3d %s+%2d: ",reg,fp,reg2offset_unchecked(reg));
    if (return_addr == reg) {
      tty->print_cr("return address");
    } else if (reg >= begin_in_preserve) {
      // Preserved slots are present on x86
      if (return_addr == OptoReg::add(reg, VMRegImpl::slots_per_word))
        tty->print_cr("saved fp register");
      else if (return_addr == OptoReg::add(reg, 2*VMRegImpl::slots_per_word) &&
               VerifyStackAtCalls)
        tty->print_cr("0xBADB100D   +VerifyStackAtCalls");
      else
        tty->print_cr("in_preserve");
    } else if ((int)OptoReg::reg2stack(reg) < fixed_slots) {
      tty->print_cr("Fixed slot %d", OptoReg::reg2stack(reg));
    } else {
      tty->print_cr("pad2, stack alignment");
    }
    reg = OptoReg::add(reg, -1);
  }

  // Spill area dump
  reg = OptoReg::add(_matcher._new_SP, _framesize );
  while( reg > _matcher._out_arg_limit ) {
    reg = OptoReg::add(reg, -1);
    tty->print_cr("#r%3.3d %s+%2d: spill",reg,fp,reg2offset_unchecked(reg));
  }

  // Outgoing argument area dump
  while( reg > OptoReg::add(_matcher._new_SP, C->out_preserve_stack_slots()) ) {
    reg = OptoReg::add(reg, -1);
    tty->print_cr("#r%3.3d %s+%2d: outgoing argument",reg,fp,reg2offset_unchecked(reg));
  }

  // Outgoing new preserve area
  while( reg > _matcher._new_SP ) {
    reg = OptoReg::add(reg, -1);
    tty->print_cr("#r%3.3d %s+%2d: new out preserve",reg,fp,reg2offset_unchecked(reg));
  }
  tty->print_cr("#");
}

void PhaseChaitin::dump_bb(uint pre_order) const {
  tty->print_cr("---dump of B%d---",pre_order);
  for (uint i = 0; i < _cfg.number_of_blocks(); i++) {
    Block* block = _cfg.get_block(i);
    if (block->_pre_order == pre_order) {
      dump(block);
    }
  }
}

void PhaseChaitin::dump_lrg(uint lidx, bool defs_only) const {
  tty->print_cr("---dump of L%d---",lidx);

  if (_ifg) {
    if (lidx >= _lrg_map.max_lrg_id()) {
      tty->print("Attempt to print live range index beyond max live range.\n");
      return;
    }
    tty->print("L%d: ",lidx);
    if (lidx < _ifg->_maxlrg) {
      lrgs(lidx).dump();
    } else {
      tty->print_cr("new LRG");
    }
  }
  if( _ifg && lidx < _ifg->_maxlrg) {
    tty->print("Neighbors: %d - ", _ifg->neighbor_cnt(lidx));
    _ifg->neighbors(lidx)->dump();
    tty->cr();
  }
  // For all blocks
  for (uint i = 0; i < _cfg.number_of_blocks(); i++) {
    Block* block = _cfg.get_block(i);
    int dump_once = 0;

    // For all instructions
    for( uint j = 0; j < block->number_of_nodes(); j++ ) {
      Node *n = block->get_node(j);
      if (_lrg_map.find_const(n) == lidx) {
        if (!dump_once++) {
          tty->cr();
          block->dump_head(&_cfg);
        }
        dump(n);
        continue;
      }
      if (!defs_only) {
        uint cnt = n->req();
        for( uint k = 1; k < cnt; k++ ) {
          Node *m = n->in(k);
          if (!m)  {
            continue;  // be robust in the dumper
          }
          if (_lrg_map.find_const(m) == lidx) {
            if (!dump_once++) {
              tty->cr();
              block->dump_head(&_cfg);
            }
            dump(n);
          }
        }
      }
    }
  } // End of per-block dump
  tty->cr();
}
#endif // not PRODUCT

#ifdef ASSERT
// Verify that base pointers and derived pointers are still sane.
void PhaseChaitin::verify_base_ptrs(ResourceArea* a) const {
  Unique_Node_List worklist(a);
  for (uint i = 0; i < _cfg.number_of_blocks(); i++) {
    Block* block = _cfg.get_block(i);
    for (uint j = block->end_idx() + 1; j > 1; j--) {
      Node* n = block->get_node(j-1);
      if (n->is_Phi()) {
        break;
      }
      // Found a safepoint?
      if (n->is_MachSafePoint()) {
        MachSafePointNode* sfpt = n->as_MachSafePoint();
        JVMState* jvms = sfpt->jvms();
        if (jvms != NULL) {
          // Now scan for a live derived pointer
          if (jvms->oopoff() < sfpt->req()) {
            // Check each derived/base pair
            for (uint idx = jvms->oopoff(); idx < sfpt->req(); idx++) {
              Node* check = sfpt->in(idx);
              bool is_derived = ((idx - jvms->oopoff()) & 1) == 0;
              // search upwards through spills and spill phis for AddP
              worklist.clear();
              worklist.push(check);
              uint k = 0;
              while (k < worklist.size()) {
                check = worklist.at(k);
                assert(check, "Bad base or derived pointer");
                // See PhaseChaitin::find_base_for_derived() for all cases.
                int isc = check->is_Copy();
                if (isc) {
                  worklist.push(check->in(isc));
                } else if (check->is_Phi()) {
                  for (uint m = 1; m < check->req(); m++) {
                    worklist.push(check->in(m));
                  }
                } else if (check->is_Con()) {
                  if (is_derived && check->bottom_type()->is_ptr()->_offset != 0) {
                    // Derived is NULL+non-zero offset, base must be NULL.
                    assert(check->bottom_type()->is_ptr()->ptr() == TypePtr::Null, "Bad derived pointer");
                  } else {
                    assert(check->bottom_type()->is_ptr()->_offset == 0, "Bad base pointer");
                    // Base either ConP(NULL) or loadConP
                    if (check->is_Mach()) {
                      assert(check->as_Mach()->ideal_Opcode() == Op_ConP, "Bad base pointer");
                    } else {
                      assert(check->Opcode() == Op_ConP &&
                             check->bottom_type()->is_ptr()->ptr() == TypePtr::Null, "Bad base pointer");
                    }
                  }
                } else if (check->bottom_type()->is_ptr()->_offset == 0) {
                  if (check->is_Proj() || (check->is_Mach() &&
                     (check->as_Mach()->ideal_Opcode() == Op_CreateEx ||
                      check->as_Mach()->ideal_Opcode() == Op_ThreadLocal ||
                      check->as_Mach()->ideal_Opcode() == Op_CMoveP ||
                      check->as_Mach()->ideal_Opcode() == Op_CheckCastPP ||
#ifdef _LP64
                      (UseCompressedOops && check->as_Mach()->ideal_Opcode() == Op_CastPP) ||
                      (UseCompressedOops && check->as_Mach()->ideal_Opcode() == Op_DecodeN) ||
                      (UseCompressedClassPointers && check->as_Mach()->ideal_Opcode() == Op_DecodeNKlass) ||
#endif // _LP64
                      check->as_Mach()->ideal_Opcode() == Op_LoadP ||
                      check->as_Mach()->ideal_Opcode() == Op_LoadKlass))) {
                    // Valid nodes
                  } else {
                    check->dump();
                    assert(false, "Bad base or derived pointer");
                  }
                } else {
                  assert(is_derived, "Bad base pointer");
                  assert(check->is_Mach() && check->as_Mach()->ideal_Opcode() == Op_AddP, "Bad derived pointer");
                }
                k++;
                assert(k < 100000, "Derived pointer checking in infinite loop");
              } // End while
            }
          } // End of check for derived pointers
        } // End of Kcheck for debug info
      } // End of if found a safepoint
    } // End of forall instructions in block
  } // End of forall blocks
}

// Verify that graphs and base pointers are still sane.
void PhaseChaitin::verify(ResourceArea* a, bool verify_ifg) const {
  if (VerifyRegisterAllocator) {
    _cfg.verify();
    verify_base_ptrs(a);
    if (verify_ifg) {
      _ifg->verify(this);
    }
  }
}
#endif // ASSERT

int PhaseChaitin::_final_loads  = 0;
int PhaseChaitin::_final_stores = 0;
int PhaseChaitin::_final_memoves= 0;
int PhaseChaitin::_final_copies = 0;
double PhaseChaitin::_final_load_cost  = 0;
double PhaseChaitin::_final_store_cost = 0;
double PhaseChaitin::_final_memove_cost= 0;
double PhaseChaitin::_final_copy_cost  = 0;
int PhaseChaitin::_conserv_coalesce = 0;
int PhaseChaitin::_conserv_coalesce_pair = 0;
int PhaseChaitin::_conserv_coalesce_trie = 0;
int PhaseChaitin::_conserv_coalesce_quad = 0;
int PhaseChaitin::_post_alloc = 0;
int PhaseChaitin::_lost_opp_pp_coalesce = 0;
int PhaseChaitin::_lost_opp_cflow_coalesce = 0;
int PhaseChaitin::_used_cisc_instructions   = 0;
int PhaseChaitin::_unused_cisc_instructions = 0;
int PhaseChaitin::_allocator_attempts       = 0;
int PhaseChaitin::_allocator_successes      = 0;

#ifndef PRODUCT
uint PhaseChaitin::_high_pressure           = 0;
uint PhaseChaitin::_low_pressure            = 0;

void PhaseChaitin::print_chaitin_statistics() {
  tty->print_cr("Inserted %d spill loads, %d spill stores, %d mem-mem moves and %d copies.", _final_loads, _final_stores, _final_memoves, _final_copies);
  tty->print_cr("Total load cost= %6.0f, store cost = %6.0f, mem-mem cost = %5.2f, copy cost = %5.0f.", _final_load_cost, _final_store_cost, _final_memove_cost, _final_copy_cost);
  tty->print_cr("Adjusted spill cost = %7.0f.",
                _final_load_cost*4.0 + _final_store_cost  * 2.0 +
                _final_copy_cost*1.0 + _final_memove_cost*12.0);
  tty->print("Conservatively coalesced %d copies, %d pairs",
                _conserv_coalesce, _conserv_coalesce_pair);
  if( _conserv_coalesce_trie || _conserv_coalesce_quad )
    tty->print(", %d tries, %d quads", _conserv_coalesce_trie, _conserv_coalesce_quad);
  tty->print_cr(", %d post alloc.", _post_alloc);
  if( _lost_opp_pp_coalesce || _lost_opp_cflow_coalesce )
    tty->print_cr("Lost coalesce opportunity, %d private-private, and %d cflow interfered.",
                  _lost_opp_pp_coalesce, _lost_opp_cflow_coalesce );
  if( _used_cisc_instructions || _unused_cisc_instructions )
    tty->print_cr("Used cisc instruction  %d,  remained in register %d",
                   _used_cisc_instructions, _unused_cisc_instructions);
  if( _allocator_successes != 0 )
    tty->print_cr("Average allocation trips %f", (float)_allocator_attempts/(float)_allocator_successes);
  tty->print_cr("High Pressure Blocks = %d, Low Pressure Blocks = %d", _high_pressure, _low_pressure);
}
#endif // not PRODUCT
