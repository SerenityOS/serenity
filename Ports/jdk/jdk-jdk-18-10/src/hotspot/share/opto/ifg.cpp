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
#include "compiler/oopMap.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "opto/addnode.hpp"
#include "opto/block.hpp"
#include "opto/callnode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/chaitin.hpp"
#include "opto/coalesce.hpp"
#include "opto/indexSet.hpp"
#include "opto/machnode.hpp"
#include "opto/memnode.hpp"
#include "opto/opcodes.hpp"

PhaseIFG::PhaseIFG( Arena *arena ) : Phase(Interference_Graph), _arena(arena) {
}

void PhaseIFG::init( uint maxlrg ) {
  _maxlrg = maxlrg;
  _yanked = new (_arena) VectorSet(_arena);
  _is_square = false;
  // Make uninitialized adjacency lists
  _adjs = (IndexSet*)_arena->Amalloc(sizeof(IndexSet)*maxlrg);
  // Also make empty live range structures
  _lrgs = (LRG *)_arena->Amalloc( maxlrg * sizeof(LRG) );
  memset((void*)_lrgs,0,sizeof(LRG)*maxlrg);
  // Init all to empty
  for( uint i = 0; i < maxlrg; i++ ) {
    _adjs[i].initialize(maxlrg);
    _lrgs[i].Set_All();
  }
}

// Add edge between vertices a & b.  These are sorted (triangular matrix),
// then the smaller number is inserted in the larger numbered array.
int PhaseIFG::add_edge( uint a, uint b ) {
  lrgs(a).invalid_degree();
  lrgs(b).invalid_degree();
  // Sort a and b, so that a is bigger
  assert( !_is_square, "only on triangular" );
  if( a < b ) { uint tmp = a; a = b; b = tmp; }
  return _adjs[a].insert( b );
}

// Is there an edge between a and b?
int PhaseIFG::test_edge( uint a, uint b ) const {
  // Sort a and b, so that a is larger
  assert( !_is_square, "only on triangular" );
  if( a < b ) { uint tmp = a; a = b; b = tmp; }
  return _adjs[a].member(b);
}

// Convert triangular matrix to square matrix
void PhaseIFG::SquareUp() {
  assert( !_is_square, "only on triangular" );

  // Simple transpose
  for(uint i = 0; i < _maxlrg; i++ ) {
    if (!_adjs[i].is_empty()) {
      IndexSetIterator elements(&_adjs[i]);
      uint datum;
      while ((datum = elements.next()) != 0) {
        _adjs[datum].insert(i);
      }
    }
  }
  _is_square = true;
}

// Compute effective degree in bulk
void PhaseIFG::Compute_Effective_Degree() {
  assert( _is_square, "only on square" );

  for( uint i = 0; i < _maxlrg; i++ )
    lrgs(i).set_degree(effective_degree(i));
}

int PhaseIFG::test_edge_sq( uint a, uint b ) const {
  assert( _is_square, "only on square" );
  // Swap, so that 'a' has the lesser count.  Then binary search is on
  // the smaller of a's list and b's list.
  if( neighbor_cnt(a) > neighbor_cnt(b) ) { uint tmp = a; a = b; b = tmp; }
  //return _adjs[a].unordered_member(b);
  return _adjs[a].member(b);
}

// Union edges of B into A
void PhaseIFG::Union(uint a, uint b) {
  assert( _is_square, "only on square" );
  IndexSet *A = &_adjs[a];
  if (!_adjs[b].is_empty()) {
    IndexSetIterator b_elements(&_adjs[b]);
    uint datum;
    while ((datum = b_elements.next()) != 0) {
      if (A->insert(datum)) {
        _adjs[datum].insert(a);
        lrgs(a).invalid_degree();
        lrgs(datum).invalid_degree();
      }
    }
  }
}

// Yank a Node and all connected edges from the IFG.  Return a
// list of neighbors (edges) yanked.
IndexSet *PhaseIFG::remove_node( uint a ) {
  assert( _is_square, "only on square" );
  assert( !_yanked->test(a), "" );
  _yanked->set(a);

  // I remove the LRG from all neighbors.
  LRG &lrg_a = lrgs(a);

  if (!_adjs[a].is_empty()) {
    IndexSetIterator elements(&_adjs[a]);
    uint datum;
    while ((datum = elements.next()) != 0) {
      _adjs[datum].remove(a);
      lrgs(datum).inc_degree(-lrg_a.compute_degree(lrgs(datum)));
    }
  }
  return neighbors(a);
}

// Re-insert a yanked Node.
void PhaseIFG::re_insert(uint a) {
  assert( _is_square, "only on square" );
  assert( _yanked->test(a), "" );
  _yanked->remove(a);

  if (_adjs[a].is_empty()) return;

  IndexSetIterator elements(&_adjs[a]);
  uint datum;
  while ((datum = elements.next()) != 0) {
    _adjs[datum].insert(a);
    lrgs(datum).invalid_degree();
  }
}

// Compute the degree between 2 live ranges.  If both live ranges are
// aligned-adjacent powers-of-2 then we use the MAX size.  If either is
// mis-aligned (or for Fat-Projections, not-adjacent) then we have to
// MULTIPLY the sizes.  Inspect Brigg's thesis on register pairs to see why
// this is so.
int LRG::compute_degree(LRG &l) const {
  int tmp;
  int num_regs = _num_regs;
  int nregs = l.num_regs();
  tmp =  (_fat_proj || l._fat_proj)     // either is a fat-proj?
    ? (num_regs * nregs)                // then use product
    : MAX2(num_regs,nregs);             // else use max
  return tmp;
}

// Compute effective degree for this live range.  If both live ranges are
// aligned-adjacent powers-of-2 then we use the MAX size.  If either is
// mis-aligned (or for Fat-Projections, not-adjacent) then we have to
// MULTIPLY the sizes.  Inspect Brigg's thesis on register pairs to see why
// this is so.
int PhaseIFG::effective_degree(uint lidx) const {
  IndexSet *s = neighbors(lidx);
  if (s->is_empty()) return 0;
  int eff = 0;
  int num_regs = lrgs(lidx).num_regs();
  int fat_proj = lrgs(lidx)._fat_proj;
  IndexSetIterator elements(s);
  uint nidx;
  while ((nidx = elements.next()) != 0) {
    LRG &lrgn = lrgs(nidx);
    int nregs = lrgn.num_regs();
    eff += (fat_proj || lrgn._fat_proj) // either is a fat-proj?
      ? (num_regs * nregs)              // then use product
      : MAX2(num_regs,nregs);           // else use max
  }
  return eff;
}


#ifndef PRODUCT
void PhaseIFG::dump() const {
  tty->print_cr("-- Interference Graph --%s--",
                _is_square ? "square" : "triangular" );
  if (_is_square) {
    for (uint i = 0; i < _maxlrg; i++) {
      tty->print(_yanked->test(i) ? "XX " : "  ");
      tty->print("L%d: { ",i);
      if (!_adjs[i].is_empty()) {
        IndexSetIterator elements(&_adjs[i]);
        uint datum;
        while ((datum = elements.next()) != 0) {
          tty->print("L%d ", datum);
        }
      }
      tty->print_cr("}");

    }
    return;
  }

  // Triangular
  for( uint i = 0; i < _maxlrg; i++ ) {
    uint j;
    tty->print(_yanked->test(i) ? "XX " : "  ");
    tty->print("L%d: { ",i);
    for( j = _maxlrg; j > i; j-- )
      if( test_edge(j - 1,i) ) {
        tty->print("L%d ",j - 1);
      }
    tty->print("| ");
    if (!_adjs[i].is_empty()) {
      IndexSetIterator elements(&_adjs[i]);
      uint datum;
      while ((datum = elements.next()) != 0) {
        tty->print("L%d ", datum);
      }
    }
    tty->print("}\n");
  }
  tty->print("\n");
}

void PhaseIFG::stats() const {
  ResourceMark rm;
  int *h_cnt = NEW_RESOURCE_ARRAY(int,_maxlrg*2);
  memset( h_cnt, 0, sizeof(int)*_maxlrg*2 );
  uint i;
  for( i = 0; i < _maxlrg; i++ ) {
    h_cnt[neighbor_cnt(i)]++;
  }
  tty->print_cr("--Histogram of counts--");
  for( i = 0; i < _maxlrg*2; i++ )
    if( h_cnt[i] )
      tty->print("%d/%d ",i,h_cnt[i]);
  tty->cr();
}

void PhaseIFG::verify( const PhaseChaitin *pc ) const {
  // IFG is square, sorted and no need for Find
  for( uint i = 0; i < _maxlrg; i++ ) {
    assert(!_yanked->test(i) || !neighbor_cnt(i), "Is removed completely" );
    IndexSet *set = &_adjs[i];
    if (!set->is_empty()) {
      IndexSetIterator elements(set);
      uint idx;
      uint last = 0;
      while ((idx = elements.next()) != 0) {
        assert(idx != i, "Must have empty diagonal");
        assert(pc->_lrg_map.find_const(idx) == idx, "Must not need Find");
        assert(_adjs[idx].member(i), "IFG not square");
        assert(!_yanked->test(idx), "No yanked neighbors");
        assert(last < idx, "not sorted increasing");
        last = idx;
      }
    }
    assert(!lrgs(i)._degree_valid || effective_degree(i) == lrgs(i).degree(), "degree is valid but wrong");
  }
}
#endif

/*
 * Interfere this register with everything currently live.
 * Check for interference by checking overlap of regmasks.
 * Only interfere if acceptable register masks overlap.
 */
void PhaseChaitin::interfere_with_live(uint lid, IndexSet* liveout) {
  if (!liveout->is_empty()) {
    LRG& lrg = lrgs(lid);
    const RegMask &rm = lrg.mask();
    IndexSetIterator elements(liveout);
    uint interfering_lid = elements.next();
    while (interfering_lid != 0) {
      LRG& interfering_lrg = lrgs(interfering_lid);
      if (rm.overlap(interfering_lrg.mask())) {
        _ifg->add_edge(lid, interfering_lid);
      }
      interfering_lid = elements.next();
    }
  }
}

// Actually build the interference graph.  Uses virtual registers only, no
// physical register masks.  This allows me to be very aggressive when
// coalescing copies.  Some of this aggressiveness will have to be undone
// later, but I'd rather get all the copies I can now (since unremoved copies
// at this point can end up in bad places).  Copies I re-insert later I have
// more opportunity to insert them in low-frequency locations.
void PhaseChaitin::build_ifg_virtual( ) {
  Compile::TracePhase tp("buildIFG_virt", &timers[_t_buildIFGvirtual]);

  // For all blocks (in any order) do...
  for (uint i = 0; i < _cfg.number_of_blocks(); i++) {
    Block* block = _cfg.get_block(i);
    IndexSet* liveout = _live->live(block);

    // The IFG is built by a single reverse pass over each basic block.
    // Starting with the known live-out set, we remove things that get
    // defined and add things that become live (essentially executing one
    // pass of a standard LIVE analysis). Just before a Node defines a value
    // (and removes it from the live-ness set) that value is certainly live.
    // The defined value interferes with everything currently live.  The
    // value is then removed from the live-ness set and it's inputs are
    // added to the live-ness set.
    for (uint j = block->end_idx() + 1; j > 1; j--) {
      Node* n = block->get_node(j - 1);

      // Get value being defined
      uint r = _lrg_map.live_range_id(n);

      // Some special values do not allocate
      if (r) {

        // Remove from live-out set
        liveout->remove(r);

        // Copies do not define a new value and so do not interfere.
        // Remove the copies source from the liveout set before interfering.
        uint idx = n->is_Copy();
        if (idx != 0) {
          liveout->remove(_lrg_map.live_range_id(n->in(idx)));
        }

        // Interfere with everything live
        interfere_with_live(r, liveout);
      }

      // Make all inputs live
      if (!n->is_Phi()) {      // Phi function uses come from prior block
        for(uint k = 1; k < n->req(); k++) {
          liveout->insert(_lrg_map.live_range_id(n->in(k)));
        }
      }

      // 2-address instructions always have the defined value live
      // on entry to the instruction, even though it is being defined
      // by the instruction.  We pretend a virtual copy sits just prior
      // to the instruction and kills the src-def'd register.
      // In other words, for 2-address instructions the defined value
      // interferes with all inputs.
      uint idx;
      if( n->is_Mach() && (idx = n->as_Mach()->two_adr()) ) {
        const MachNode *mach = n->as_Mach();
        // Sometimes my 2-address ADDs are commuted in a bad way.
        // We generally want the USE-DEF register to refer to the
        // loop-varying quantity, to avoid a copy.
        uint op = mach->ideal_Opcode();
        // Check that mach->num_opnds() == 3 to ensure instruction is
        // not subsuming constants, effectively excludes addI_cin_imm
        // Can NOT swap for instructions like addI_cin_imm since it
        // is adding zero to yhi + carry and the second ideal-input
        // points to the result of adding low-halves.
        // Checking req() and num_opnds() does NOT distinguish addI_cout from addI_cout_imm
        if( (op == Op_AddI && mach->req() == 3 && mach->num_opnds() == 3) &&
            n->in(1)->bottom_type()->base() == Type::Int &&
            // See if the ADD is involved in a tight data loop the wrong way
            n->in(2)->is_Phi() &&
            n->in(2)->in(2) == n ) {
          Node *tmp = n->in(1);
          n->set_req( 1, n->in(2) );
          n->set_req( 2, tmp );
        }
        // Defined value interferes with all inputs
        uint lidx = _lrg_map.live_range_id(n->in(idx));
        for (uint k = 1; k < n->req(); k++) {
          uint kidx = _lrg_map.live_range_id(n->in(k));
          if (kidx != lidx) {
            _ifg->add_edge(r, kidx);
          }
        }
      }
    } // End of forall instructions in block
  } // End of forall blocks
}

#ifdef ASSERT
uint PhaseChaitin::count_int_pressure(IndexSet* liveout) {
  if (liveout->is_empty()) {
    return 0;
  }
  IndexSetIterator elements(liveout);
  uint lidx = elements.next();
  uint cnt = 0;
  while (lidx != 0) {
    LRG& lrg = lrgs(lidx);
    if (lrg.mask_is_nonempty_and_up() &&
        !lrg.is_float_or_vector() &&
        (lrg.mask().overlap(*Matcher::idealreg2regmask[Op_RegI]) ||
         (Matcher::has_predicated_vectors() &&
          lrg.mask().overlap(*Matcher::idealreg2regmask[Op_RegVectMask])))) {
      cnt += lrg.reg_pressure();
    }
    lidx = elements.next();
  }
  return cnt;
}

uint PhaseChaitin::count_float_pressure(IndexSet* liveout) {
  if (liveout->is_empty()) {
    return 0;
  }
  IndexSetIterator elements(liveout);
  uint lidx = elements.next();
  uint cnt = 0;
  while (lidx != 0) {
    LRG& lrg = lrgs(lidx);
    if (lrg.mask_is_nonempty_and_up() && lrg.is_float_or_vector()) {
      cnt += lrg.reg_pressure();
    }
    lidx = elements.next();
  }
  return cnt;
}
#endif

/*
 * Adjust register pressure down by 1.  Capture last hi-to-low transition,
 */
void PhaseChaitin::lower_pressure(Block* b, uint location, LRG& lrg, IndexSet* liveout, Pressure& int_pressure, Pressure& float_pressure) {
  if (lrg.mask_is_nonempty_and_up()) {
    if (lrg.is_float_or_vector()) {
      float_pressure.lower(lrg, location);
    } else {
      // Do not count the SP and flag registers
      const RegMask& r = lrg.mask();
      if (r.overlap(*Matcher::idealreg2regmask[Op_RegI]) ||
           (Matcher::has_predicated_vectors() &&
            r.overlap(*Matcher::idealreg2regmask[Op_RegVectMask]))) {
        int_pressure.lower(lrg, location);
      }
    }
  }
  if (_scheduling_info_generated == false) {
    assert(int_pressure.current_pressure() == count_int_pressure(liveout), "the int pressure is incorrect");
    assert(float_pressure.current_pressure() == count_float_pressure(liveout), "the float pressure is incorrect");
  }
}

/* Go to the first non-phi index in a block */
static uint first_nonphi_index(Block* b) {
  uint i;
  uint end_idx = b->end_idx();
  for (i = 1; i < end_idx; i++) {
    Node* n = b->get_node(i);
    if (!n->is_Phi()) {
      break;
    }
  }
  return i;
}

/*
 * Spills could be inserted before a CreateEx node which should be the first
 * instruction in a block after Phi nodes. If so, move the CreateEx node up.
 */
static void move_exception_node_up(Block* b, uint first_inst, uint last_inst) {
  for (uint i = first_inst; i < last_inst; i++) {
    Node* ex = b->get_node(i);
    if (ex->is_SpillCopy()) {
      continue;
    }

    if (i > first_inst &&
        ex->is_Mach() && ex->as_Mach()->ideal_Opcode() == Op_CreateEx) {
      b->remove_node(i);
      b->insert_node(ex, first_inst);
    }
    // Stop once a CreateEx or any other node is found
    break;
  }
}

/*
 * When new live ranges are live, we raise the register pressure
 */
void PhaseChaitin::raise_pressure(Block* b, LRG& lrg, Pressure& int_pressure, Pressure& float_pressure) {
  if (lrg.mask_is_nonempty_and_up()) {
    if (lrg.is_float_or_vector()) {
      float_pressure.raise(lrg);
    } else {
      // Do not count the SP and flag registers
      const RegMask& rm = lrg.mask();
      if (rm.overlap(*Matcher::idealreg2regmask[Op_RegI]) ||
           (Matcher::has_predicated_vectors() &&
            rm.overlap(*Matcher::idealreg2regmask[Op_RegVectMask]))) {
        int_pressure.raise(lrg);
      }
    }
  }
}


/*
 * Computes the initial register pressure of a block, looking at all live
 * ranges in the liveout. The register pressure is computed for both float
 * and int/pointer registers.
 * Live ranges in the liveout are presumed live for the whole block.
 * We add the cost for the whole block to the area of the live ranges initially.
 * If a live range gets killed in the block, we'll subtract the unused part of
 * the block from the area.
 */
void PhaseChaitin::compute_initial_block_pressure(Block* b, IndexSet* liveout, Pressure& int_pressure, Pressure& float_pressure, double cost) {
  if (!liveout->is_empty()) {
    IndexSetIterator elements(liveout);
    uint lid = elements.next();
    while (lid != 0) {
      LRG &lrg = lrgs(lid);
      lrg._area += cost;
      raise_pressure(b, lrg, int_pressure, float_pressure);
      lid = elements.next();
    }
  }
  assert(int_pressure.current_pressure() == count_int_pressure(liveout), "the int pressure is incorrect");
  assert(float_pressure.current_pressure() == count_float_pressure(liveout), "the float pressure is incorrect");
}

/*
* Computes the entry register pressure of a block, looking at all live
* ranges in the livein. The register pressure is computed for both float
* and int/pointer registers.
*/
void PhaseChaitin::compute_entry_block_pressure(Block* b) {
  IndexSet *livein = _live->livein(b);
  if (!livein->is_empty()) {
    IndexSetIterator elements(livein);
    uint lid = elements.next();
    while (lid != 0) {
      LRG &lrg = lrgs(lid);
      raise_pressure(b, lrg, _sched_int_pressure, _sched_float_pressure);
      lid = elements.next();
    }
  }
  // Now check phis for locally defined inputs
  for (uint j = 0; j < b->number_of_nodes(); j++) {
    Node* n = b->get_node(j);
    if (n->is_Phi()) {
      for (uint k = 1; k < n->req(); k++) {
        Node* phi_in = n->in(k);
        // Because we are talking about phis, raise register pressure once for each
        // instance of a phi to account for a single value
        if (_cfg.get_block_for_node(phi_in) == b) {
          LRG& lrg = lrgs(phi_in->_idx);
          raise_pressure(b, lrg, _sched_int_pressure, _sched_float_pressure);
          break;
        }
      }
    }
  }
  _sched_int_pressure.set_start_pressure(_sched_int_pressure.current_pressure());
  _sched_float_pressure.set_start_pressure(_sched_float_pressure.current_pressure());
}

/*
* Computes the exit register pressure of a block, looking at all live
* ranges in the liveout. The register pressure is computed for both float
* and int/pointer registers.
*/
void PhaseChaitin::compute_exit_block_pressure(Block* b) {

  IndexSet* livein = _live->live(b);
  _sched_int_pressure.set_current_pressure(0);
  _sched_float_pressure.set_current_pressure(0);
  if (!livein->is_empty()) {
    IndexSetIterator elements(livein);
    uint lid = elements.next();
    while (lid != 0) {
      LRG &lrg = lrgs(lid);
      raise_pressure(b, lrg, _sched_int_pressure, _sched_float_pressure);
      lid = elements.next();
    }
  }
}

/*
 * Remove dead node if it's not used.
 * We only remove projection nodes if the node "defining" the projection is
 * dead, for example on x86, if we have a dead Add node we remove its
 * RFLAGS node.
 */
bool PhaseChaitin::remove_node_if_not_used(Block* b, uint location, Node* n, uint lid, IndexSet* liveout) {
  Node* def = n->in(0);
  if (!n->is_Proj() ||
      (_lrg_map.live_range_id(def) && !liveout->member(_lrg_map.live_range_id(def)))) {
    if (n->is_MachProj()) {
      // Don't remove KILL projections if their "defining" nodes have
      // memory effects (have SCMemProj projection node) -
      // they are not dead even when their result is not used.
      // For example, compareAndSwapL (and other CAS) and EncodeISOArray nodes.
      // The method add_input_to_liveout() keeps such nodes alive (put them on liveout list)
      // when it sees SCMemProj node in a block. Unfortunately SCMemProj node could be placed
      // in block in such order that KILL MachProj nodes are processed first.
      if (def->has_out_with(Op_SCMemProj)) {
        return false;
      }
    }
    b->remove_node(location);
    LRG& lrg = lrgs(lid);
    if (lrg._def == n) {
      lrg._def = 0;
    }
    n->disconnect_inputs(C);
    _cfg.unmap_node_from_block(n);
    n->replace_by(C->top());
    return true;
  }
  return false;
}

/*
 * When encountering a fat projection, we might go from a low to high to low
 * (since the fat proj only lives at this instruction) going backwards in the
 * block. If we find a low to high transition, we record it.
 */
void PhaseChaitin::check_for_high_pressure_transition_at_fatproj(uint& block_reg_pressure, uint location, LRG& lrg, Pressure& pressure, const int op_regtype) {
  RegMask mask_tmp = lrg.mask();
  mask_tmp.AND(*Matcher::idealreg2regmask[op_regtype]);
  pressure.check_pressure_at_fatproj(location, mask_tmp);
}

/*
 * Insure high score for immediate-use spill copies so they get a color.
 * All single-use MachSpillCopy(s) that immediately precede their
 * use must color early.  If a longer live range steals their
 * color, the spill copy will split and may push another spill copy
 * further away resulting in an infinite spill-split-retry cycle.
 * Assigning a zero area results in a high score() and a good
 * location in the simplify list.
 */
void PhaseChaitin::assign_high_score_to_immediate_copies(Block* b, Node* n, LRG& lrg, uint next_inst, uint last_inst) {
  if (n->is_SpillCopy() &&
      lrg.is_singledef() && // A multi defined live range can still split
      n->outcnt() == 1 &&   // and use must be in this block
      _cfg.get_block_for_node(n->unique_out()) == b) {

    Node* single_use = n->unique_out();
    assert(b->find_node(single_use) >= next_inst, "Use must be later in block");
    // Use can be earlier in block if it is a Phi, but then I should be a MultiDef

    // Find first non SpillCopy 'm' that follows the current instruction
    // (current_inst - 1) is index for current instruction 'n'
    Node* m = n;
    for (uint i = next_inst; i <= last_inst && m->is_SpillCopy(); ++i) {
      m = b->get_node(i);
    }
    if (m == single_use) {
      lrg._area = 0.0;
    }
  }
}

/*
 * Copies do not define a new value and so do not interfere.
 * Remove the copies source from the liveout set before interfering.
 */
void PhaseChaitin::remove_interference_from_copy(Block* b, uint location, uint lid_copy, IndexSet* liveout, double cost, Pressure& int_pressure, Pressure& float_pressure) {
  if (liveout->remove(lid_copy)) {
    LRG& lrg_copy = lrgs(lid_copy);
    lrg_copy._area -= cost;

    // Lower register pressure since copy and definition can share the same register
    lower_pressure(b, location, lrg_copy, liveout, int_pressure, float_pressure);
  }
}

/*
 * The defined value must go in a particular register. Remove that register from
 * all conflicting parties and avoid the interference.
 */
void PhaseChaitin::remove_bound_register_from_interfering_live_ranges(LRG& lrg, IndexSet* liveout, uint& must_spill) {
  if (liveout->is_empty()) return;
  // Check for common case
  const RegMask& rm = lrg.mask();
  int r_size = lrg.num_regs();
  // Smear odd bits
  IndexSetIterator elements(liveout);
  uint l = elements.next();
  while (l != 0) {
    LRG& interfering_lrg = lrgs(l);
    // If 'l' must spill already, do not further hack his bits.
    // He'll get some interferences and be forced to spill later.
    if (interfering_lrg._must_spill) {
      l = elements.next();
      continue;
    }

    // Remove bound register(s) from 'l's choices
    RegMask old = interfering_lrg.mask();
    uint old_size = interfering_lrg.mask_size();

    // Remove the bits from LRG 'rm' from LRG 'l' so 'l' no
    // longer interferes with 'rm'.  If 'l' requires aligned
    // adjacent pairs, subtract out bit pairs.
    assert(!interfering_lrg._is_vector || !interfering_lrg._fat_proj, "sanity");

    if (interfering_lrg.num_regs() > 1 && !interfering_lrg._fat_proj) {
      RegMask r2mask = rm;
      // Leave only aligned set of bits.
      r2mask.smear_to_sets(interfering_lrg.num_regs());
      // It includes vector case.
      interfering_lrg.SUBTRACT(r2mask);
      interfering_lrg.compute_set_mask_size();
    } else if (r_size != 1) {
      // fat proj
      interfering_lrg.SUBTRACT(rm);
      interfering_lrg.compute_set_mask_size();
    } else {
      // Common case: size 1 bound removal
      OptoReg::Name r_reg = rm.find_first_elem();
      if (interfering_lrg.mask().Member(r_reg)) {
        interfering_lrg.Remove(r_reg);
        interfering_lrg.set_mask_size(interfering_lrg.mask().is_AllStack() ? LRG::AllStack_size : old_size - 1);
      }
    }

    // If 'l' goes completely dry, it must spill.
    if (interfering_lrg.not_free()) {
      // Give 'l' some kind of reasonable mask, so it picks up
      // interferences (and will spill later).
      interfering_lrg.set_mask(old);
      interfering_lrg.set_mask_size(old_size);
      must_spill++;
      interfering_lrg._must_spill = 1;
      interfering_lrg.set_reg(OptoReg::Name(LRG::SPILL_REG));
    }
    l = elements.next();
  }
}

/*
 * Start loop at 1 (skip control edge) for most Nodes. SCMemProj's might be the
 * sole use of a StoreLConditional. While StoreLConditionals set memory (the
 * SCMemProj use) they also def flags; if that flag def is unused the allocator
 * sees a flag-setting instruction with no use of the flags and assumes it's
 * dead.  This keeps the (useless) flag-setting behavior alive while also
 * keeping the (useful) memory update effect.
 */
void PhaseChaitin::add_input_to_liveout(Block* b, Node* n, IndexSet* liveout, double cost, Pressure& int_pressure, Pressure& float_pressure) {
  JVMState* jvms = n->jvms();
  uint debug_start = jvms ? jvms->debug_start() : 999999;

  for (uint k = ((n->Opcode() == Op_SCMemProj) ? 0:1); k < n->req(); k++) {
    Node* def = n->in(k);
    uint lid = _lrg_map.live_range_id(def);
    if (!lid) {
      continue;
    }
    LRG& lrg = lrgs(lid);

    // No use-side cost for spilling debug info
    if (k < debug_start) {
      // A USE costs twice block frequency (once for the Load, once
      // for a Load-delay).  Rematerialized uses only cost once.
      lrg._cost += (def->rematerialize() ? b->_freq : (b->_freq * 2));
    }

    if (liveout->insert(lid)) {
      // Newly live things assumed live from here to top of block
      lrg._area += cost;
      raise_pressure(b, lrg, int_pressure, float_pressure);
      assert(int_pressure.current_pressure() == count_int_pressure(liveout), "the int pressure is incorrect");
      assert(float_pressure.current_pressure() == count_float_pressure(liveout), "the float pressure is incorrect");
    }
    assert(lrg._area >= 0.0, "negative spill area" );
  }
}

/*
 * If we run off the top of the block with high pressure just record that the
 * whole block is high pressure. (Even though we might have a transition
 * later down in the block)
 */
void PhaseChaitin::check_for_high_pressure_block(Pressure& pressure) {
  // current pressure now means the pressure before the first instruction in the block
  // (since we have stepped through all instructions backwards)
  if (pressure.current_pressure() > pressure.high_pressure_limit()) {
    pressure.set_high_pressure_index_to_block_start();
  }
}

/*
 * Compute high pressure indice; avoid landing in the middle of projnodes
 * and set the high pressure index for the block
 */
void PhaseChaitin::adjust_high_pressure_index(Block* b, uint& block_hrp_index, Pressure& pressure) {
  uint i = pressure.high_pressure_index();
  if (i < b->number_of_nodes() && i < b->end_idx() + 1) {
    Node* cur = b->get_node(i);
    while (cur->is_Proj() || (cur->is_MachNullCheck()) || cur->is_Catch()) {
      cur = b->get_node(--i);
    }
  }
  block_hrp_index = i;
}

void PhaseChaitin::print_pressure_info(Pressure& pressure, const char *str) {
  if (str != NULL) {
    tty->print_cr("#  *** %s ***", str);
  }
  tty->print_cr("#     start pressure is = %d", pressure.start_pressure());
  tty->print_cr("#     max pressure is = %d", pressure.final_pressure());
  tty->print_cr("#     end pressure is = %d", pressure.current_pressure());
  tty->print_cr("#");
}

/* Build an interference graph:
 *   That is, if 2 live ranges are simultaneously alive but in their acceptable
 *   register sets do not overlap, then they do not interfere. The IFG is built
 *   by a single reverse pass over each basic block. Starting with the known
 *   live-out set, we remove things that get defined and add things that become
 *   live (essentially executing one pass of a standard LIVE analysis). Just
 *   before a Node defines a value (and removes it from the live-ness set) that
 *   value is certainly live. The defined value interferes with everything
 *   currently live. The value is then removed from the live-ness set and it's
 *   inputs are added to the live-ness set.
 * Compute register pressure for each block:
 *   We store the biggest register pressure for each block and also the first
 *   low to high register pressure transition within the block (if any).
 */
uint PhaseChaitin::build_ifg_physical( ResourceArea *a ) {
  Compile::TracePhase tp("buildIFG", &timers[_t_buildIFGphysical]);

  uint must_spill = 0;
  for (uint i = 0; i < _cfg.number_of_blocks(); i++) {
    Block* block = _cfg.get_block(i);

    // Clone (rather than smash in place) the liveout info, so it is alive
    // for the "collect_gc_info" phase later.
    IndexSet liveout(_live->live(block));

    uint first_inst = first_nonphi_index(block);
    uint last_inst = block->end_idx();

    move_exception_node_up(block, first_inst, last_inst);

    Pressure int_pressure(last_inst + 1, Matcher::int_pressure_limit());
    Pressure float_pressure(last_inst + 1, Matcher::float_pressure_limit());
    block->_reg_pressure = 0;
    block->_freg_pressure = 0;

    int inst_count = last_inst - first_inst;
    double cost = (inst_count <= 0) ? 0.0 : block->_freq * double(inst_count);
    assert(cost >= 0.0, "negative spill cost" );

    compute_initial_block_pressure(block, &liveout, int_pressure, float_pressure, cost);

    for (uint location = last_inst; location > 0; location--) {
      Node* n = block->get_node(location);
      uint lid = _lrg_map.live_range_id(n);

      if (lid) {
        LRG& lrg = lrgs(lid);

        // A DEF normally costs block frequency; rematerialized values are
        // removed from the DEF sight, so LOWER costs here.
        lrg._cost += n->rematerialize() ? 0 : block->_freq;

        if (!liveout.member(lid) && n->Opcode() != Op_SafePoint) {
          if (remove_node_if_not_used(block, location, n, lid, &liveout)) {
            float_pressure.lower_high_pressure_index();
            int_pressure.lower_high_pressure_index();
            continue;
          }
          if (lrg._fat_proj) {
            check_for_high_pressure_transition_at_fatproj(block->_reg_pressure, location, lrg, int_pressure, Op_RegI);
            check_for_high_pressure_transition_at_fatproj(block->_freg_pressure, location, lrg, float_pressure, Op_RegD);
          }
        } else {
          // A live range ends at its definition, remove the remaining area.
          // If the cost is +Inf (which might happen in extreme cases), the lrg area will also be +Inf,
          // and +Inf - +Inf = NaN. So let's not do that subtraction.
          if (g_isfinite(cost)) {
            lrg._area -= cost;
          }
          assert(lrg._area >= 0.0, "negative spill area" );

          assign_high_score_to_immediate_copies(block, n, lrg, location + 1, last_inst);

          if (liveout.remove(lid)) {
            lower_pressure(block, location, lrg, &liveout, int_pressure, float_pressure);
          }
          uint copy_idx = n->is_Copy();
          if (copy_idx) {
            uint lid_copy = _lrg_map.live_range_id(n->in(copy_idx));
            remove_interference_from_copy(block, location, lid_copy, &liveout, cost, int_pressure, float_pressure);
          }
        }

        // Since rematerializable DEFs are not bound but the live range is,
        // some uses must be bound. If we spill live range 'r', it can
        // rematerialize at each use site according to its bindings.
        if (lrg.is_bound() && !n->rematerialize() && lrg.mask().is_NotEmpty()) {
          remove_bound_register_from_interfering_live_ranges(lrg, &liveout, must_spill);
        }
        interfere_with_live(lid, &liveout);
      }

      // Area remaining in the block
      inst_count--;
      cost = (inst_count <= 0) ? 0.0 : block->_freq * double(inst_count);

      if (!n->is_Phi()) {
        add_input_to_liveout(block, n, &liveout, cost, int_pressure, float_pressure);
      }
    }

    check_for_high_pressure_block(int_pressure);
    check_for_high_pressure_block(float_pressure);
    adjust_high_pressure_index(block, block->_ihrp_index, int_pressure);
    adjust_high_pressure_index(block, block->_fhrp_index, float_pressure);
    // set the final_pressure as the register pressure for the block
    block->_reg_pressure = int_pressure.final_pressure();
    block->_freg_pressure = float_pressure.final_pressure();

#ifndef PRODUCT
    // Gather Register Pressure Statistics
    if (PrintOptoStatistics) {
      if (block->_reg_pressure > int_pressure.high_pressure_limit() || block->_freg_pressure > float_pressure.high_pressure_limit()) {
        _high_pressure++;
      } else {
        _low_pressure++;
      }
    }
#endif
  }

  return must_spill;
}
