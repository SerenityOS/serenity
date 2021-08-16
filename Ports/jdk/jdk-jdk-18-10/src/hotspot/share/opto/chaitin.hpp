/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_CHAITIN_HPP
#define SHARE_OPTO_CHAITIN_HPP

#include "code/vmreg.hpp"
#include "memory/resourceArea.hpp"
#include "opto/connode.hpp"
#include "opto/live.hpp"
#include "opto/machnode.hpp"
#include "opto/matcher.hpp"
#include "opto/phase.hpp"
#include "opto/regalloc.hpp"
#include "opto/regmask.hpp"

class Matcher;
class PhaseCFG;
class PhaseLive;
class PhaseRegAlloc;
class PhaseChaitin;

#define OPTO_DEBUG_SPLIT_FREQ  BLOCK_FREQUENCY(0.001)
#define OPTO_LRG_HIGH_FREQ     BLOCK_FREQUENCY(0.25)

//------------------------------LRG--------------------------------------------
// Live-RanGe structure.
class LRG : public ResourceObj {
  friend class VMStructs;
public:
  static const uint AllStack_size = 0xFFFFF; // This mask size is used to tell that the mask of this LRG supports stack positions
  enum { SPILL_REG=29999 };     // Register number of a spilled LRG

  double _cost;                 // 2 for loads/1 for stores times block freq
  double _area;                 // Sum of all simultaneously live values
  double score() const;         // Compute score from cost and area
  double _maxfreq;              // Maximum frequency of any def or use

  Node *_def;                   // Check for multi-def live ranges
#ifndef PRODUCT
  GrowableArray<Node*>* _defs;
#endif

  uint _risk_bias;              // Index of LRG which we want to avoid color
  uint _copy_bias;              // Index of LRG which we want to share color

  uint _next;                   // Index of next LRG in linked list
  uint _prev;                   // Index of prev LRG in linked list
private:
  uint _reg;                    // Chosen register; undefined if mask is plural
public:
  // Return chosen register for this LRG.  Error if the LRG is not bound to
  // a single register.
  OptoReg::Name reg() const { return OptoReg::Name(_reg); }
  void set_reg( OptoReg::Name r ) { _reg = r; }

private:
  uint _eff_degree;             // Effective degree: Sum of neighbors _num_regs
public:
  int degree() const { assert( _degree_valid , "" ); return _eff_degree; }
  // Degree starts not valid and any change to the IFG neighbor
  // set makes it not valid.
  void set_degree( uint degree ) {
    _eff_degree = degree;
    debug_only(_degree_valid = 1;)
    assert(!_mask.is_AllStack() || (_mask.is_AllStack() && lo_degree()), "_eff_degree can't be bigger than AllStack_size - _num_regs if the mask supports stack registers");
  }
  // Made a change that hammered degree
  void invalid_degree() { debug_only(_degree_valid=0;) }
  // Incrementally modify degree.  If it was correct, it should remain correct
  void inc_degree( uint mod ) {
    _eff_degree += mod;
    assert(!_mask.is_AllStack() || (_mask.is_AllStack() && lo_degree()), "_eff_degree can't be bigger than AllStack_size - _num_regs if the mask supports stack registers");
  }
  // Compute the degree between 2 live ranges
  int compute_degree( LRG &l ) const;
  bool mask_is_nonempty_and_up() const {
    return mask().is_UP() && mask_size();
  }
  bool is_float_or_vector() const {
    return _is_float || _is_vector;
  }

private:
  RegMask _mask;                // Allowed registers for this LRG
  uint _mask_size;              // cache of _mask.Size();
public:
  int compute_mask_size() const { return _mask.is_AllStack() ? AllStack_size : _mask.Size(); }
  void set_mask_size( int size ) {
    assert((size == (int)AllStack_size) || (size == (int)_mask.Size()), "");
    _mask_size = size;
#ifdef ASSERT
    _msize_valid=1;
    if (_is_vector) {
      assert(!_fat_proj, "sanity");
      if (!(_is_scalable && OptoReg::is_stack(_reg))) {
        assert(_mask.is_aligned_sets(_num_regs), "mask is not aligned, adjacent sets");
      }
    } else if (_num_regs == 2 && !_fat_proj) {
      assert(_mask.is_aligned_pairs(), "mask is not aligned, adjacent pairs");
    }
#endif
  }
  void compute_set_mask_size() { set_mask_size(compute_mask_size()); }
  int mask_size() const { assert( _msize_valid, "mask size not valid" );
                          return _mask_size; }
  // Get the last mask size computed, even if it does not match the
  // count of bits in the current mask.
  int get_invalid_mask_size() const { return _mask_size; }
  const RegMask &mask() const { return _mask; }
  void set_mask( const RegMask &rm ) { _mask = rm; debug_only(_msize_valid=0;)}
  void AND( const RegMask &rm ) { _mask.AND(rm); debug_only(_msize_valid=0;)}
  void SUBTRACT( const RegMask &rm ) { _mask.SUBTRACT(rm); debug_only(_msize_valid=0;)}
  void Clear()   { _mask.Clear()  ; debug_only(_msize_valid=1); _mask_size = 0; }
  void Set_All() { _mask.Set_All(); debug_only(_msize_valid=1); _mask_size = RegMask::CHUNK_SIZE; }

  void Insert( OptoReg::Name reg ) { _mask.Insert(reg);  debug_only(_msize_valid=0;) }
  void Remove( OptoReg::Name reg ) { _mask.Remove(reg);  debug_only(_msize_valid=0;) }
  void clear_to_sets()  { _mask.clear_to_sets(_num_regs); debug_only(_msize_valid=0;) }

private:
  // Number of registers this live range uses when it colors
  uint16_t _num_regs;           // 2 for Longs and Doubles, 1 for all else
                                // except _num_regs is kill count for fat_proj

  // For scalable register, num_regs may not be the actual physical register size.
  // We need to get the actual physical length of scalable register when scalable
  // register is spilled. The size of one slot is 32-bit.
  uint _scalable_reg_slots;     // Actual scalable register length of slots.
                                // Meaningful only when _is_scalable is true.
public:
  int num_regs() const { return _num_regs; }
  void set_num_regs( int reg ) { assert( _num_regs == reg || !_num_regs, "" ); _num_regs = reg; }

  uint scalable_reg_slots() { return _scalable_reg_slots; }
  void set_scalable_reg_slots(uint slots) {
    assert(_is_scalable, "scalable register");
    assert(slots > 0, "slots of scalable register is not valid");
    _scalable_reg_slots = slots;
  }

  bool is_scalable() {
#ifdef ASSERT
    if (_is_scalable) {
      // Should only be a vector for now, but it could also be a RegVectMask in future.
      assert(_is_vector && (_num_regs == RegMask::SlotsPerVecA), "unexpected scalable reg");
    }
#endif
    return Matcher::implements_scalable_vector && _is_scalable;
  }

private:
  // Number of physical registers this live range uses when it colors
  // Architecture and register-set dependent
  uint16_t _reg_pressure;
public:
  void set_reg_pressure(int i)  { _reg_pressure = i; }
  int      reg_pressure() const { return _reg_pressure; }

  // How much 'wiggle room' does this live range have?
  // How many color choices can it make (scaled by _num_regs)?
  int degrees_of_freedom() const { return mask_size() - _num_regs; }
  // Bound LRGs have ZERO degrees of freedom.  We also count
  // must_spill as bound.
  bool is_bound  () const { return _is_bound; }
  // Negative degrees-of-freedom; even with no neighbors this
  // live range must spill.
  bool not_free() const { return degrees_of_freedom() <  0; }
  // Is this live range of "low-degree"?  Trivially colorable?
  bool lo_degree () const { return degree() <= degrees_of_freedom(); }
  // Is this live range just barely "low-degree"?  Trivially colorable?
  bool just_lo_degree () const { return degree() == degrees_of_freedom(); }

  uint   _is_oop:1,             // Live-range holds an oop
         _is_float:1,           // True if in float registers
         _is_vector:1,          // True if in vector registers
         _is_scalable:1,        // True if register size is scalable
                                //      e.g. Arm SVE vector/predicate registers.
         _was_spilled1:1,       // True if prior spilling on def
         _was_spilled2:1,       // True if twice prior spilling on def
         _is_bound:1,           // live range starts life with no
                                // degrees of freedom.
         _direct_conflict:1,    // True if def and use registers in conflict
         _must_spill:1,         // live range has lost all degrees of freedom
    // If _fat_proj is set, live range does NOT require aligned, adjacent
    // registers and has NO interferences.
    // If _fat_proj is clear, live range requires num_regs() to be a power of
    // 2, and it requires registers to form an aligned, adjacent set.
         _fat_proj:1,           //
         _was_lo:1,             // Was lo-degree prior to coalesce
         _msize_valid:1,        // _mask_size cache valid
         _degree_valid:1,       // _degree cache valid
         _has_copy:1,           // Adjacent to some copy instruction
         _at_risk:1;            // Simplify says this guy is at risk to spill


  // Alive if non-zero, dead if zero
  bool alive() const { return _def != NULL; }
  bool is_multidef() const { return _def == NodeSentinel; }
  bool is_singledef() const { return _def != NodeSentinel; }

#ifndef PRODUCT
  void dump( ) const;
#endif
};

//------------------------------IFG--------------------------------------------
//                         InterFerence Graph
// An undirected graph implementation.  Created with a fixed number of
// vertices.  Edges can be added & tested.  Vertices can be removed, then
// added back later with all edges intact.  Can add edges between one vertex
// and a list of other vertices.  Can union vertices (and their edges)
// together.  The IFG needs to be really really fast, and also fairly
// abstract!  It needs abstraction so I can fiddle with the implementation to
// get even more speed.
class PhaseIFG : public Phase {
  friend class VMStructs;
  // Current implementation: a triangular adjacency list.

  // Array of adjacency-lists, indexed by live-range number
  IndexSet *_adjs;

  // Assertion bit for proper use of Squaring
  bool _is_square;

  // Live range structure goes here
  LRG *_lrgs;                   // Array of LRG structures

public:
  // Largest live-range number
  uint _maxlrg;

  Arena *_arena;

  // Keep track of inserted and deleted Nodes
  VectorSet *_yanked;

  PhaseIFG( Arena *arena );
  void init( uint maxlrg );

  // Add edge between a and b.  Returns true if actually addded.
  int add_edge( uint a, uint b );

  // Test for edge existance
  int test_edge( uint a, uint b ) const;

  // Square-up matrix for faster Union
  void SquareUp();

  // Return number of LRG neighbors
  uint neighbor_cnt( uint a ) const { return _adjs[a].count(); }
  // Union edges of b into a on Squared-up matrix
  void Union( uint a, uint b );
  // Test for edge in Squared-up matrix
  int test_edge_sq( uint a, uint b ) const;
  // Yank a Node and all connected edges from the IFG.  Be prepared to
  // re-insert the yanked Node in reverse order of yanking.  Return a
  // list of neighbors (edges) yanked.
  IndexSet *remove_node( uint a );
  // Reinsert a yanked Node
  void re_insert( uint a );
  // Return set of neighbors
  IndexSet *neighbors( uint a ) const { return &_adjs[a]; }

#ifndef PRODUCT
  // Dump the IFG
  void dump() const;
  void stats() const;
  void verify( const PhaseChaitin * ) const;
#endif

  //--------------- Live Range Accessors
  LRG &lrgs(uint idx) const { assert(idx < _maxlrg, "oob"); return _lrgs[idx]; }

  // Compute and set effective degree.  Might be folded into SquareUp().
  void Compute_Effective_Degree();

  // Compute effective degree as the sum of neighbors' _sizes.
  int effective_degree( uint lidx ) const;
};

// The LiveRangeMap class is responsible for storing node to live range id mapping.
// Each node is mapped to a live range id (a virtual register). Nodes that are
// not considered for register allocation are given live range id 0.
class LiveRangeMap {

private:

  uint _max_lrg_id;

  // Union-find map.  Declared as a short for speed.
  // Indexed by live-range number, it returns the compacted live-range number
  LRG_List _uf_map;

  // Map from Nodes to live ranges
  LRG_List _names;

  // Straight out of Tarjan's union-find algorithm
  uint find_compress(const Node *node) {
    uint lrg_id = find_compress(_names.at(node->_idx));
    _names.at_put(node->_idx, lrg_id);
    return lrg_id;
  }

  uint find_compress(uint lrg);

public:

  const LRG_List& names() {
    return _names;
  }

  uint max_lrg_id() const {
    return _max_lrg_id;
  }

  void set_max_lrg_id(uint max_lrg_id) {
    _max_lrg_id = max_lrg_id;
  }

  uint size() const {
    return _names.length();
  }

  uint live_range_id(uint idx) const {
    return _names.at(idx);
  }

  uint live_range_id(const Node *node) const {
    return _names.at(node->_idx);
  }

  uint uf_live_range_id(uint lrg_id) const {
    return _uf_map.at(lrg_id);
  }

  void map(uint idx, uint lrg_id) {
    _names.at_put(idx, lrg_id);
  }

  void uf_map(uint dst_lrg_id, uint src_lrg_id) {
    _uf_map.at_put(dst_lrg_id, src_lrg_id);
  }

  void extend(uint idx, uint lrg_id) {
    _names.at_put_grow(idx, lrg_id);
  }

  void uf_extend(uint dst_lrg_id, uint src_lrg_id) {
    _uf_map.at_put_grow(dst_lrg_id, src_lrg_id);
  }

  LiveRangeMap(Arena* arena, uint unique)
  :  _max_lrg_id(0)
  , _uf_map(arena, unique, unique, 0)
  , _names(arena, unique, unique, 0) {}

  uint find_id( const Node *n ) {
    uint retval = live_range_id(n);
    assert(retval == find(n),"Invalid node to lidx mapping");
    return retval;
  }

  // Reset the Union-Find map to identity
  void reset_uf_map(uint max_lrg_id);

  // Make all Nodes map directly to their final live range; no need for
  // the Union-Find mapping after this call.
  void compress_uf_map_for_nodes();

  uint find(uint lidx) {
    uint uf_lidx = _uf_map.at(lidx);
    return (uf_lidx == lidx) ? uf_lidx : find_compress(lidx);
  }

  // Convert a Node into a Live Range Index - a lidx
  uint find(const Node *node) {
    uint lidx = live_range_id(node);
    uint uf_lidx = _uf_map.at(lidx);
    return (uf_lidx == lidx) ? uf_lidx : find_compress(node);
  }

  // Like Find above, but no path compress, so bad asymptotic behavior
  uint find_const(uint lrg) const;

  // Like Find above, but no path compress, so bad asymptotic behavior
  uint find_const(const Node *node) const {
    if(node->_idx >= (uint)_names.length()) {
      return 0; // not mapped, usual for debug dump
    }
    return find_const(_names.at(node->_idx));
  }
};

//------------------------------Chaitin----------------------------------------
// Briggs-Chaitin style allocation, mostly.
class PhaseChaitin : public PhaseRegAlloc {
  friend class VMStructs;

  int _trip_cnt;
  int _alternate;

  PhaseLive *_live;             // Liveness, used in the interference graph
  PhaseIFG *_ifg;               // Interference graph (for original chunk)
  VectorSet _spilled_once;      // Nodes that have been spilled
  VectorSet _spilled_twice;     // Nodes that have been spilled twice

  // Combine the Live Range Indices for these 2 Nodes into a single live
  // range.  Future requests for any Node in either live range will
  // return the live range index for the combined live range.
  void Union( const Node *src, const Node *dst );

  void new_lrg( const Node *x, uint lrg );

  // Compact live ranges, removing unused ones.  Return new maxlrg.
  void compact();

  uint _lo_degree;              // Head of lo-degree LRGs list
  uint _lo_stk_degree;          // Head of lo-stk-degree LRGs list
  uint _hi_degree;              // Head of hi-degree LRGs list
  uint _simplified;             // Linked list head of simplified LRGs

  // Helper functions for Split()
  uint split_DEF(Node *def, Block *b, int loc, uint max, Node **Reachblock, Node **debug_defs, GrowableArray<uint> splits, int slidx );
  int split_USE(MachSpillCopyNode::SpillType spill_type, Node *def, Block *b, Node *use, uint useidx, uint max, bool def_down, bool cisc_sp, GrowableArray<uint> splits, int slidx );

  //------------------------------clone_projs------------------------------------
  // After cloning some rematerialized instruction, clone any MachProj's that
  // follow it.  Example: Intel zero is XOR, kills flags.  Sparc FP constants
  // use G3 as an address temp.
  int clone_projs(Block* b, uint idx, Node* orig, Node* copy, uint& max_lrg_id);

  int clone_projs(Block* b, uint idx, Node* orig, Node* copy, LiveRangeMap& lrg_map) {
    uint max_lrg_id = lrg_map.max_lrg_id();
    int found_projs = clone_projs(b, idx, orig, copy, max_lrg_id);
    if (found_projs > 0) {
      // max_lrg_id is updated during call above
      lrg_map.set_max_lrg_id(max_lrg_id);
    }
    return found_projs;
  }

  Node *split_Rematerialize(Node *def, Block *b, uint insidx, uint &maxlrg, GrowableArray<uint> splits,
                            int slidx, uint *lrg2reach, Node **Reachblock, bool walkThru);
  // True if lidx is used before any real register is def'd in the block
  bool prompt_use( Block *b, uint lidx );
  Node *get_spillcopy_wide(MachSpillCopyNode::SpillType spill_type, Node *def, Node *use, uint uidx );
  // Insert the spill at chosen location.  Skip over any intervening Proj's or
  // Phis.  Skip over a CatchNode and projs, inserting in the fall-through block
  // instead.  Update high-pressure indices.  Create a new live range.
  void insert_proj( Block *b, uint i, Node *spill, uint maxlrg );

  bool is_high_pressure( Block *b, LRG *lrg, uint insidx );

  uint _oldphi;                 // Node index which separates pre-allocation nodes

  Block **_blks;                // Array of blocks sorted by frequency for coalescing

  float _high_frequency_lrg;    // Frequency at which LRG will be spilled for debug info

#ifndef PRODUCT
  bool _trace_spilling;
#endif

public:
  PhaseChaitin(uint unique, PhaseCFG &cfg, Matcher &matcher, bool track_liveout_pressure);
  ~PhaseChaitin() {}

  LiveRangeMap _lrg_map;

  LRG &lrgs(uint idx) const { return _ifg->lrgs(idx); }

  // Do all the real work of allocate
  void Register_Allocate();

  float high_frequency_lrg() const { return _high_frequency_lrg; }

  // Used when scheduling info generated, not in general register allocation
  bool _scheduling_info_generated;

  void set_ifg(PhaseIFG &ifg) { _ifg = &ifg;  }
  void set_live(PhaseLive &live) { _live = &live; }
  PhaseLive* get_live() { return _live; }

  // Populate the live range maps with ssa info for scheduling
  void mark_ssa();

#ifndef PRODUCT
  bool trace_spilling() const { return _trace_spilling; }
#endif

private:
  // De-SSA the world.  Assign registers to Nodes.  Use the same register for
  // all inputs to a PhiNode, effectively coalescing live ranges.  Insert
  // copies as needed.
  void de_ssa();

  // Add edge between reg and everything in the vector.
  // Use the RegMask information to trim the set of interferences.  Return the
  // count of edges added.
  void interfere_with_live(uint lid, IndexSet* liveout);
#ifdef ASSERT
  // Count register pressure for asserts
  uint count_int_pressure(IndexSet* liveout);
  uint count_float_pressure(IndexSet* liveout);
#endif

  // Build the interference graph using virtual registers only.
  // Used for aggressive coalescing.
  void build_ifg_virtual( );

  // used when computing the register pressure for each block in the CFG. This
  // is done during IFG creation.
  class Pressure {
      // keeps track of the register pressure at the current
      // instruction (used when stepping backwards in the block)
      uint _current_pressure;

      // keeps track of the instruction index of the first low to high register pressure
      // transition (starting from the top) in the block
      // if high_pressure_index == 0 then the whole block is high pressure
      // if high_pressure_index = b.end_idx() + 1 then the whole block is low pressure
      uint _high_pressure_index;

      // stores the highest pressure we find
      uint _final_pressure;

      // number of live ranges that constitute high register pressure
      uint _high_pressure_limit;

      // initial pressure observed
      uint _start_pressure;

    public:

      // lower the register pressure and look for a low to high pressure
      // transition
      void lower(LRG& lrg, uint& location) {
        _current_pressure -= lrg.reg_pressure();
        if (_current_pressure == _high_pressure_limit) {
          _high_pressure_index = location;
        }
      }

      // raise the pressure and store the pressure if it's the biggest
      // pressure so far
      void raise(LRG &lrg) {
        _current_pressure += lrg.reg_pressure();
        if (_current_pressure > _final_pressure) {
          _final_pressure = _current_pressure;
        }
      }

      void init(int limit) {
        _current_pressure = 0;
        _high_pressure_index = 0;
        _final_pressure = 0;
        _high_pressure_limit = limit;
        _start_pressure = 0;
      }

      uint high_pressure_index() const {
        return _high_pressure_index;
      }

      uint final_pressure() const {
        return _final_pressure;
      }

      uint start_pressure() const {
        return _start_pressure;
      }

      uint current_pressure() const {
        return _current_pressure;
      }

      uint high_pressure_limit() const {
        return _high_pressure_limit;
      }

      void lower_high_pressure_index() {
        _high_pressure_index--;
      }

      void set_high_pressure_index_to_block_start() {
        _high_pressure_index = 0;
      }

      void set_start_pressure(int value) {
        _start_pressure = value;
        _final_pressure = value;
      }

      void set_current_pressure(int value) {
        _current_pressure = value;
      }

      void check_pressure_at_fatproj(uint fatproj_location, RegMask& fatproj_mask) {
        // this pressure is only valid at this instruction, i.e. we don't need to lower
        // the register pressure since the fat proj was never live before (going backwards)
        uint new_pressure = current_pressure() + fatproj_mask.Size();
        if (new_pressure > final_pressure()) {
          _final_pressure = new_pressure;
        }

        // if we were at a low pressure and now and the fat proj is at high pressure, record the fat proj location
        // as coming from a low to high (to low again)
        if (current_pressure() <= high_pressure_limit() && new_pressure > high_pressure_limit()) {
          _high_pressure_index = fatproj_location;
        }
      }

      Pressure(uint high_pressure_index, uint high_pressure_limit)
        : _current_pressure(0)
        , _high_pressure_index(high_pressure_index)
        , _final_pressure(0)
        , _high_pressure_limit(high_pressure_limit)
        , _start_pressure(0) {}
  };

  void check_for_high_pressure_transition_at_fatproj(uint& block_reg_pressure, uint location, LRG& lrg, Pressure& pressure, const int op_regtype);
  void add_input_to_liveout(Block* b, Node* n, IndexSet* liveout, double cost, Pressure& int_pressure, Pressure& float_pressure);
  void compute_initial_block_pressure(Block* b, IndexSet* liveout, Pressure& int_pressure, Pressure& float_pressure, double cost);
  bool remove_node_if_not_used(Block* b, uint location, Node* n, uint lid, IndexSet* liveout);
  void assign_high_score_to_immediate_copies(Block* b, Node* n, LRG& lrg, uint next_inst, uint last_inst);
  void remove_interference_from_copy(Block* b, uint location, uint lid_copy, IndexSet* liveout, double cost, Pressure& int_pressure, Pressure& float_pressure);
  void remove_bound_register_from_interfering_live_ranges(LRG& lrg, IndexSet* liveout, uint& must_spill);
  void check_for_high_pressure_block(Pressure& pressure);
  void adjust_high_pressure_index(Block* b, uint& hrp_index, Pressure& pressure);

  // Build the interference graph using physical registers when available.
  // That is, if 2 live ranges are simultaneously alive but in their
  // acceptable register sets do not overlap, then they do not interfere.
  uint build_ifg_physical( ResourceArea *a );

public:
  // Gather LiveRanGe information, including register masks and base pointer/
  // derived pointer relationships.
  void gather_lrg_masks( bool mod_cisc_masks );

  // user visible pressure variables for scheduling
  Pressure _sched_int_pressure;
  Pressure _sched_float_pressure;
  Pressure _scratch_int_pressure;
  Pressure _scratch_float_pressure;

  // Pressure functions for user context
  void lower_pressure(Block* b, uint location, LRG& lrg, IndexSet* liveout, Pressure& int_pressure, Pressure& float_pressure);
  void raise_pressure(Block* b, LRG& lrg, Pressure& int_pressure, Pressure& float_pressure);
  void compute_entry_block_pressure(Block* b);
  void compute_exit_block_pressure(Block* b);
  void print_pressure_info(Pressure& pressure, const char *str);

private:
  // Force the bases of derived pointers to be alive at GC points.
  bool stretch_base_pointer_live_ranges( ResourceArea *a );
  // Helper to stretch above; recursively discover the base Node for
  // a given derived Node.  Easy for AddP-related machine nodes, but
  // needs to be recursive for derived Phis.
  Node *find_base_for_derived( Node **derived_base_map, Node *derived, uint &maxlrg );

  // Set the was-lo-degree bit.  Conservative coalescing should not change the
  // colorability of the graph.  If any live range was of low-degree before
  // coalescing, it should Simplify.  This call sets the was-lo-degree bit.
  void set_was_low();

  // Init LRG caching of degree, numregs.  Init lo_degree list.
  void cache_lrg_info( );

  // Simplify the IFG by removing LRGs of low degree
  void Simplify();

  // Select colors by re-inserting edges into the IFG.
  // Return TRUE if any spills occurred.
  uint Select( );
  // Helper function for select which allows biased coloring
  OptoReg::Name choose_color( LRG &lrg, int chunk );
  // Helper function which implements biasing heuristic
  OptoReg::Name bias_color( LRG &lrg, int chunk );

  // Split uncolorable live ranges
  // Return new number of live ranges
  uint Split(uint maxlrg, ResourceArea* split_arena);

  // Set the 'spilled_once' or 'spilled_twice' flag on a node.
  void set_was_spilled( Node *n );

  // Convert ideal spill-nodes into machine loads & stores
  // Set C->failing when fixup spills could not complete, node limit exceeded.
  void fixup_spills();

  // Post-Allocation peephole copy removal
  void post_allocate_copy_removal();
  Node *skip_copies( Node *c );
  // Replace the old node with the current live version of that value
  // and yank the old value if it's dead.
  int replace_and_yank_if_dead( Node *old, OptoReg::Name nreg,
      Block *current_block, Node_List& value, Node_List& regnd ) {
    Node* v = regnd[nreg];
    assert(v->outcnt() != 0, "no dead values");
    old->replace_by(v);
    return yank_if_dead(old, current_block, &value, &regnd);
  }

  int yank_if_dead( Node *old, Block *current_block, Node_List *value, Node_List *regnd ) {
    return yank_if_dead_recurse(old, old, current_block, value, regnd);
  }
  int yank_if_dead_recurse(Node *old, Node *orig_old, Block *current_block,
      Node_List *value, Node_List *regnd);
  int yank( Node *old, Block *current_block, Node_List *value, Node_List *regnd );
  int elide_copy( Node *n, int k, Block *current_block, Node_List &value, Node_List &regnd, bool can_change_regs );
  int use_prior_register( Node *copy, uint idx, Node *def, Block *current_block, Node_List &value, Node_List &regnd );
  bool may_be_copy_of_callee( Node *def ) const;

  // If nreg already contains the same constant as val then eliminate it
  bool eliminate_copy_of_constant(Node* val, Node* n,
      Block *current_block, Node_List& value, Node_List &regnd,
      OptoReg::Name nreg, OptoReg::Name nreg2);
  // Extend the node to LRG mapping
  void add_reference( const Node *node, const Node *old_node);

  // Record the first use of a def in the block for a register.
  class RegDefUse {
    Node* _def;
    Node* _first_use;
  public:
    RegDefUse() : _def(NULL), _first_use(NULL) { }
    Node* def() const       { return _def;       }
    Node* first_use() const { return _first_use; }

    void update(Node* def, Node* use) {
      if (_def != def) {
        _def = def;
        _first_use = use;
      }
    }
    void clear() {
      _def = NULL;
      _first_use = NULL;
    }
  };
  typedef GrowableArray<RegDefUse> RegToDefUseMap;
  int possibly_merge_multidef(Node *n, uint k, Block *block, RegToDefUseMap& reg2defuse);

  // Merge nodes that are a part of a multidef lrg and produce the same value within a block.
  void merge_multidefs();

private:

  static int _final_loads, _final_stores, _final_copies, _final_memoves;
  static double _final_load_cost, _final_store_cost, _final_copy_cost, _final_memove_cost;
  static int _conserv_coalesce, _conserv_coalesce_pair;
  static int _conserv_coalesce_trie, _conserv_coalesce_quad;
  static int _post_alloc;
  static int _lost_opp_pp_coalesce, _lost_opp_cflow_coalesce;
  static int _used_cisc_instructions, _unused_cisc_instructions;
  static int _allocator_attempts, _allocator_successes;

#ifdef ASSERT
  // Verify that base pointers and derived pointers are still sane
  void verify_base_ptrs(ResourceArea* a) const;
  void verify(ResourceArea* a, bool verify_ifg = false) const;
#endif // ASSERT

#ifndef PRODUCT
  static uint _high_pressure, _low_pressure;

  void dump() const;
  void dump(const Node* n) const;
  void dump(const Block* b) const;
  void dump_degree_lists() const;
  void dump_simplified() const;
  void dump_lrg(uint lidx, bool defs_only) const;
  void dump_lrg(uint lidx) const {
    // dump defs and uses by default
    dump_lrg(lidx, false);
  }
  void dump_bb(uint pre_order) const;
  void dump_for_spill_split_recycle() const;

public:
  void dump_frame() const;
  char *dump_register(const Node* n, char* buf) const;
private:
  static void print_chaitin_statistics();
#endif // not PRODUCT
  friend class PhaseCoalesce;
  friend class PhaseAggressiveCoalesce;
  friend class PhaseConservativeCoalesce;
};

#endif // SHARE_OPTO_CHAITIN_HPP
