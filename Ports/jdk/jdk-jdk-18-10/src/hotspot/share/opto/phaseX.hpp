/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_PHASEX_HPP
#define SHARE_OPTO_PHASEX_HPP

#include "libadt/dict.hpp"
#include "libadt/vectset.hpp"
#include "memory/resourceArea.hpp"
#include "opto/memnode.hpp"
#include "opto/node.hpp"
#include "opto/phase.hpp"
#include "opto/type.hpp"

class Compile;
class ConINode;
class ConLNode;
class Node;
class Type;
class PhaseTransform;
class   PhaseGVN;
class     PhaseIterGVN;
class       PhaseCCP;
class   PhasePeephole;
class   PhaseRegAlloc;


//-----------------------------------------------------------------------------
// Expandable closed hash-table of nodes, initialized to NULL.
// Note that the constructor just zeros things
// Storage is reclaimed when the Arena's lifetime is over.
class NodeHash : public StackObj {
protected:
  Arena *_a;                    // Arena to allocate in
  uint   _max;                  // Size of table (power of 2)
  uint   _inserts;              // For grow and debug, count of hash_inserts
  uint   _insert_limit;         // 'grow' when _inserts reaches _insert_limit
  Node **_table;                // Hash table of Node pointers
  Node  *_sentinel;             // Replaces deleted entries in hash table

public:
  NodeHash(uint est_max_size);
  NodeHash(Arena *arena, uint est_max_size);
  NodeHash(NodeHash *use_this_state);
#ifdef ASSERT
  ~NodeHash();                  // Unlock all nodes upon destruction of table.
  void operator=(const NodeHash&); // Unlock all nodes upon replacement of table.
#endif
  Node  *hash_find(const Node*);// Find an equivalent version in hash table
  Node  *hash_find_insert(Node*);// If not in table insert else return found node
  void   hash_insert(Node*);    // Insert into hash table
  bool   hash_delete(const Node*);// Replace with _sentinel in hash table
  void   check_grow() {
    _inserts++;
    if( _inserts == _insert_limit ) { grow(); }
    assert( _inserts <= _insert_limit, "hash table overflow");
    assert( _inserts < _max, "hash table overflow" );
  }
  static uint round_up(uint);   // Round up to nearest power of 2
  void   grow();                // Grow _table to next power of 2 and rehash
  // Return 75% of _max, rounded up.
  uint   insert_limit() const { return _max - (_max>>2); }

  void   clear();               // Set all entries to NULL, keep storage.
  // Size of hash table
  uint   size()         const { return _max; }
  // Return Node* at index in table
  Node  *at(uint table_index) {
    assert(table_index < _max, "Must be within table");
    return _table[table_index];
  }

  void   remove_useless_nodes(VectorSet& useful); // replace with sentinel
  void   replace_with(NodeHash* nh);
  void   check_no_speculative_types(); // Check no speculative part for type nodes in table

  Node  *sentinel() { return _sentinel; }

#ifndef PRODUCT
  Node  *find_index(uint idx);  // For debugging
  void   dump();                // For debugging, dump statistics
  uint   _grows;                // For debugging, count of table grow()s
  uint   _look_probes;          // For debugging, count of hash probes
  uint   _lookup_hits;          // For debugging, count of hash_finds
  uint   _lookup_misses;        // For debugging, count of hash_finds
  uint   _insert_probes;        // For debugging, count of hash probes
  uint   _delete_probes;        // For debugging, count of hash probes for deletes
  uint   _delete_hits;          // For debugging, count of hash probes for deletes
  uint   _delete_misses;        // For debugging, count of hash probes for deletes
  uint   _total_inserts;        // For debugging, total inserts into hash table
  uint   _total_insert_probes;  // For debugging, total probes while inserting
#endif
};


//-----------------------------------------------------------------------------
// Map dense integer indices to Types.  Uses classic doubling-array trick.
// Abstractly provides an infinite array of Type*'s, initialized to NULL.
// Note that the constructor just zeros things, and since I use Arena
// allocation I do not need a destructor to reclaim storage.
// Despite the general name, this class is customized for use by PhaseTransform.
class Type_Array : public StackObj {
  Arena *_a;                    // Arena to allocate in
  uint   _max;
  const Type **_types;
  void grow( uint i );          // Grow array node to fit
  const Type *operator[] ( uint i ) const // Lookup, or NULL for not mapped
  { return (i<_max) ? _types[i] : (Type*)NULL; }
  friend class PhaseTransform;
public:
  Type_Array(Arena *a) : _a(a), _max(0), _types(0) {}
  Type_Array(Type_Array *ta) : _a(ta->_a), _max(ta->_max), _types(ta->_types) { }
  const Type *fast_lookup(uint i) const{assert(i<_max,"oob");return _types[i];}
  // Extend the mapping: index i maps to Type *n.
  void map( uint i, const Type *n ) { if( i>=_max ) grow(i); _types[i] = n; }
  uint Size() const { return _max; }
#ifndef PRODUCT
  void dump() const;
#endif
};


//------------------------------PhaseRemoveUseless-----------------------------
// Remove useless nodes from GVN hash-table, worklist, and graph
class PhaseRemoveUseless : public Phase {
protected:
  Unique_Node_List _useful;   // Nodes reachable from root
                              // list is allocated from current resource area
public:
  PhaseRemoveUseless(PhaseGVN *gvn, Unique_Node_List *worklist, PhaseNumber phase_num = Remove_Useless);

  Unique_Node_List *get_useful() { return &_useful; }
};

//------------------------------PhaseRenumber----------------------------------
// Phase that first performs a PhaseRemoveUseless, then it renumbers compiler
// structures accordingly.
class PhaseRenumberLive : public PhaseRemoveUseless {
protected:
  Type_Array _new_type_array; // Storage for the updated type information.
  GrowableArray<int> _old2new_map;
  Node_List _delayed;
  bool _is_pass_finished;
  uint _live_node_count;

  int update_embedded_ids(Node* n);
  int new_index(int old_idx);

public:
  PhaseRenumberLive(PhaseGVN* gvn,
                    Unique_Node_List* worklist, Unique_Node_List* new_worklist,
                    PhaseNumber phase_num = Remove_Useless_And_Renumber_Live);
};


//------------------------------PhaseTransform---------------------------------
// Phases that analyze, then transform.  Constructing the Phase object does any
// global or slow analysis.  The results are cached later for a fast
// transformation pass.  When the Phase object is deleted the cached analysis
// results are deleted.
class PhaseTransform : public Phase {
protected:
  Arena*     _arena;
  Node_List  _nodes;           // Map old node indices to new nodes.
  Type_Array _types;           // Map old node indices to Types.

  // ConNode caches:
  enum { _icon_min = -1 * HeapWordSize,
         _icon_max = 16 * HeapWordSize,
         _lcon_min = _icon_min,
         _lcon_max = _icon_max,
         _zcon_max = (uint)T_CONFLICT
  };
  ConINode* _icons[_icon_max - _icon_min + 1];   // cached jint constant nodes
  ConLNode* _lcons[_lcon_max - _lcon_min + 1];   // cached jlong constant nodes
  ConNode*  _zcons[_zcon_max + 1];               // cached is_zero_type nodes
  void init_con_caches();

  // Support both int and long caches because either might be an intptr_t,
  // so they show up frequently in address computations.

public:
  PhaseTransform( PhaseNumber pnum );
  PhaseTransform( Arena *arena, PhaseNumber pnum );
  PhaseTransform( PhaseTransform *phase, PhaseNumber pnum );

  Arena*      arena()   { return _arena; }
  Type_Array& types()   { return _types; }
  void replace_types(Type_Array new_types) {
    _types = new_types;
  }
  // _nodes is used in varying ways by subclasses, which define local accessors
  uint nodes_size() {
    return _nodes.size();
  }

public:
  // Get a previously recorded type for the node n.
  // This type must already have been recorded.
  // If you want the type of a very new (untransformed) node,
  // you must use type_or_null, and test the result for NULL.
  const Type* type(const Node* n) const {
    assert(_pnum != Ideal_Loop, "should not be used from PhaseIdealLoop");
    assert(n != NULL, "must not be null");
    const Type* t = _types.fast_lookup(n->_idx);
    assert(t != NULL, "must set before get");
    return t;
  }
  // Get a previously recorded type for the node n,
  // or else return NULL if there is none.
  const Type* type_or_null(const Node* n) const {
    assert(_pnum != Ideal_Loop, "should not be used from PhaseIdealLoop");
    return _types.fast_lookup(n->_idx);
  }
  // Record a type for a node.
  void    set_type(const Node* n, const Type *t) {
    assert(t != NULL, "type must not be null");
    _types.map(n->_idx, t);
  }
  // Record an initial type for a node, the node's bottom type.
  void    set_type_bottom(const Node* n) {
    // Use this for initialization when bottom_type() (or better) is not handy.
    // Usually the initialization shoudl be to n->Value(this) instead,
    // or a hand-optimized value like Type::MEMORY or Type::CONTROL.
    assert(_types[n->_idx] == NULL, "must set the initial type just once");
    _types.map(n->_idx, n->bottom_type());
  }
  // Make sure the types array is big enough to record a size for the node n.
  // (In product builds, we never want to do range checks on the types array!)
  void ensure_type_or_null(const Node* n) {
    if (n->_idx >= _types.Size())
      _types.map(n->_idx, NULL);   // Grow the types array as needed.
  }

  // Utility functions:
  const TypeInt*  find_int_type( Node* n);
  const TypeLong* find_long_type(Node* n);
  jint  find_int_con( Node* n, jint  value_if_unknown) {
    const TypeInt* t = find_int_type(n);
    return (t != NULL && t->is_con()) ? t->get_con() : value_if_unknown;
  }
  jlong find_long_con(Node* n, jlong value_if_unknown) {
    const TypeLong* t = find_long_type(n);
    return (t != NULL && t->is_con()) ? t->get_con() : value_if_unknown;
  }

  // Make an idealized constant, i.e., one of ConINode, ConPNode, ConFNode, etc.
  // Same as transform(ConNode::make(t)).
  ConNode* makecon(const Type* t);
  virtual ConNode* uncached_makecon(const Type* t)  // override in PhaseValues
  { ShouldNotCallThis(); return NULL; }

  // Fast int or long constant.  Same as TypeInt::make(i) or TypeLong::make(l).
  ConINode* intcon(jint i);
  ConLNode* longcon(jlong l);
  ConNode* integercon(jlong l, BasicType bt);

  // Fast zero or null constant.  Same as makecon(Type::get_zero_type(bt)).
  ConNode* zerocon(BasicType bt);

  // Return a node which computes the same function as this node, but
  // in a faster or cheaper fashion.
  virtual Node *transform( Node *n ) = 0;

  // For pessimistic passes, the return type must monotonically narrow.
  // For optimistic  passes, the return type must monotonically widen.
  // It is possible to get into a "death march" in either type of pass,
  // where the types are continually moving but it will take 2**31 or
  // more steps to converge.  This doesn't happen on most normal loops.
  //
  // Here is an example of a deadly loop for an optimistic pass, along
  // with a partial trace of inferred types:
  //    x = phi(0,x'); L: x' = x+1; if (x' >= 0) goto L;
  //    0                 1                join([0..max], 1)
  //    [0..1]            [1..2]           join([0..max], [1..2])
  //    [0..2]            [1..3]           join([0..max], [1..3])
  //      ... ... ...
  //    [0..max]          [min]u[1..max]   join([0..max], [min..max])
  //    [0..max] ==> fixpoint
  // We would have proven, the hard way, that the iteration space is all
  // non-negative ints, with the loop terminating due to 32-bit overflow.
  //
  // Here is the corresponding example for a pessimistic pass:
  //    x = phi(0,x'); L: x' = x-1; if (x' >= 0) goto L;
  //    int               int              join([0..max], int)
  //    [0..max]          [-1..max-1]      join([0..max], [-1..max-1])
  //    [0..max-1]        [-1..max-2]      join([0..max], [-1..max-2])
  //      ... ... ...
  //    [0..1]            [-1..0]          join([0..max], [-1..0])
  //    0                 -1               join([0..max], -1)
  //    0 == fixpoint
  // We would have proven, the hard way, that the iteration space is {0}.
  // (Usually, other optimizations will make the "if (x >= 0)" fold up
  // before we get into trouble.  But not always.)
  //
  // It's a pleasant thing to observe that the pessimistic pass
  // will make short work of the optimistic pass's deadly loop,
  // and vice versa.  That is a good example of the complementary
  // purposes of the CCP (optimistic) vs. GVN (pessimistic) phases.
  //
  // In any case, only widen or narrow a few times before going to the
  // correct flavor of top or bottom.
  //
  // This call only needs to be made once as the data flows around any
  // given cycle.  We do it at Phis, and nowhere else.
  // The types presented are the new type of a phi (computed by PhiNode::Value)
  // and the previously computed type, last time the phi was visited.
  //
  // The third argument is upper limit for the saturated value,
  // if the phase wishes to widen the new_type.
  // If the phase is narrowing, the old type provides a lower limit.
  // Caller guarantees that old_type and new_type are no higher than limit_type.
  virtual const Type* saturate(const Type* new_type, const Type* old_type,
                               const Type* limit_type) const
  { ShouldNotCallThis(); return NULL; }

  // true if CFG node d dominates CFG node n
  virtual bool is_dominator(Node *d, Node *n) { fatal("unimplemented for this pass"); return false; };

#ifndef PRODUCT
  void dump_old2new_map() const;
  void dump_new( uint new_lidx ) const;
  void dump_types() const;
  void dump_nodes_and_types(const Node *root, uint depth, bool only_ctrl = true);
  void dump_nodes_and_types_recur( const Node *n, uint depth, bool only_ctrl, VectorSet &visited);

  uint   _count_progress;       // For profiling, count transforms that make progress
  void   set_progress()        { ++_count_progress; assert( allow_progress(),"No progress allowed during verification"); }
  void   clear_progress()      { _count_progress = 0; }
  uint   made_progress() const { return _count_progress; }

  uint   _count_transforms;     // For profiling, count transforms performed
  void   set_transforms()      { ++_count_transforms; }
  void   clear_transforms()    { _count_transforms = 0; }
  uint   made_transforms() const{ return _count_transforms; }

  bool   _allow_progress;      // progress not allowed during verification pass
  void   set_allow_progress(bool allow) { _allow_progress = allow; }
  bool   allow_progress()               { return _allow_progress; }
#endif
};

//------------------------------PhaseValues------------------------------------
// Phase infrastructure to support values
class PhaseValues : public PhaseTransform {
protected:
  NodeHash  _table;             // Hash table for value-numbering
  bool      _iterGVN;
public:
  PhaseValues(Arena* arena, uint est_max_size);
  PhaseValues(PhaseValues* pt);
  NOT_PRODUCT(~PhaseValues();)
  PhaseIterGVN* is_IterGVN() { return (_iterGVN) ? (PhaseIterGVN*)this : NULL; }

  // Some Ideal and other transforms delete --> modify --> insert values
  bool   hash_delete(Node* n)     { return _table.hash_delete(n); }
  void   hash_insert(Node* n)     { _table.hash_insert(n); }
  Node*  hash_find_insert(Node* n){ return _table.hash_find_insert(n); }
  Node*  hash_find(const Node* n) { return _table.hash_find(n); }

  // Used after parsing to eliminate values that are no longer in program
  void   remove_useless_nodes(VectorSet &useful) {
    _table.remove_useless_nodes(useful);
    // this may invalidate cached cons so reset the cache
    init_con_caches();
  }

  virtual ConNode* uncached_makecon(const Type* t);  // override from PhaseTransform

  const Type* saturate(const Type* new_type, const Type* old_type,
                       const Type* limit_type) const
  { return new_type; }

#ifndef PRODUCT
  uint   _count_new_values;     // For profiling, count new values produced
  void    inc_new_values()        { ++_count_new_values; }
  void    clear_new_values()      { _count_new_values = 0; }
  uint    made_new_values() const { return _count_new_values; }
#endif
};


//------------------------------PhaseGVN---------------------------------------
// Phase for performing local, pessimistic GVN-style optimizations.
class PhaseGVN : public PhaseValues {
protected:
  bool is_dominator_helper(Node *d, Node *n, bool linear_only);

public:
  PhaseGVN( Arena *arena, uint est_max_size ) : PhaseValues( arena, est_max_size ) {}
  PhaseGVN( PhaseGVN *gvn ) : PhaseValues( gvn ) {}

  // Return a node which computes the same function as this node, but
  // in a faster or cheaper fashion.
  Node  *transform( Node *n );
  Node  *transform_no_reclaim( Node *n );
  virtual void record_for_igvn(Node *n) {
    C->record_for_igvn(n);
  }

  void replace_with(PhaseGVN* gvn) {
    _table.replace_with(&gvn->_table);
    _types = gvn->_types;
  }

  bool is_dominator(Node *d, Node *n) { return is_dominator_helper(d, n, true); }

  // Helper to call Node::Ideal() and BarrierSetC2::ideal_node().
  Node* apply_ideal(Node* i, bool can_reshape);

#ifdef ASSERT
  void dump_infinite_loop_info(Node* n, const char* where);
  // Check for a simple dead loop when a data node references itself.
  void dead_loop_check(Node *n);
#endif
};

//------------------------------PhaseIterGVN-----------------------------------
// Phase for iteratively performing local, pessimistic GVN-style optimizations.
// and ideal transformations on the graph.
class PhaseIterGVN : public PhaseGVN {
private:
  bool _delay_transform;  // When true simply register the node when calling transform
                          // instead of actually optimizing it

  // Idealize old Node 'n' with respect to its inputs and its value
  virtual Node *transform_old( Node *a_node );

  // Subsume users of node 'old' into node 'nn'
  void subsume_node( Node *old, Node *nn );

  Node_Stack _stack;      // Stack used to avoid recursion
protected:

  // Shuffle worklist, for stress testing
  void shuffle_worklist();

  virtual const Type* saturate(const Type* new_type, const Type* old_type,
                               const Type* limit_type) const;
  // Usually returns new_type.  Returns old_type if new_type is only a slight
  // improvement, such that it would take many (>>10) steps to reach 2**32.

public:
  PhaseIterGVN(PhaseIterGVN* igvn); // Used by CCP constructor
  PhaseIterGVN(PhaseGVN* gvn); // Used after Parser

  // Idealize new Node 'n' with respect to its inputs and its value
  virtual Node *transform( Node *a_node );
  virtual void record_for_igvn(Node *n) { }

  Unique_Node_List _worklist;       // Iterative worklist

  // Given def-use info and an initial worklist, apply Node::Ideal,
  // Node::Value, Node::Identity, hash-based value numbering, Node::Ideal_DU
  // and dominator info to a fixed point.
  void optimize();

#ifndef PRODUCT
  void trace_PhaseIterGVN(Node* n, Node* nn, const Type* old_type);
  void init_verifyPhaseIterGVN();
  void verify_PhaseIterGVN();
#endif

#ifdef ASSERT
  void dump_infinite_loop_info(Node* n, const char* where);
  void trace_PhaseIterGVN_verbose(Node* n, int num_processed);
#endif

  // Register a new node with the iter GVN pass without transforming it.
  // Used when we need to restructure a Region/Phi area and all the Regions
  // and Phis need to complete this one big transform before any other
  // transforms can be triggered on the region.
  // Optional 'orig' is an earlier version of this node.
  // It is significant only for debugging and profiling.
  Node* register_new_node_with_optimizer(Node* n, Node* orig = NULL);

  // Kill a globally dead Node.  All uses are also globally dead and are
  // aggressively trimmed.
  void remove_globally_dead_node( Node *dead );

  // Kill all inputs to a dead node, recursively making more dead nodes.
  // The Node must be dead locally, i.e., have no uses.
  void remove_dead_node( Node *dead ) {
    assert(dead->outcnt() == 0 && !dead->is_top(), "node must be dead");
    remove_globally_dead_node(dead);
  }

  // Add users of 'n' to worklist
  void add_users_to_worklist0( Node *n );
  void add_users_to_worklist ( Node *n );

  // Replace old node with new one.
  void replace_node( Node *old, Node *nn ) {
    add_users_to_worklist(old);
    hash_delete(old); // Yank from hash before hacking edges
    subsume_node(old, nn);
  }

  // Delayed node rehash: remove a node from the hash table and rehash it during
  // next optimizing pass
  void rehash_node_delayed(Node* n) {
    hash_delete(n);
    _worklist.push(n);
  }

  // Replace ith edge of "n" with "in"
  void replace_input_of(Node* n, int i, Node* in) {
    rehash_node_delayed(n);
    n->set_req_X(i, in, this);
  }

  // Delete ith edge of "n"
  void delete_input_of(Node* n, int i) {
    rehash_node_delayed(n);
    n->del_req(i);
  }

  bool delay_transform() const { return _delay_transform; }

  void set_delay_transform(bool delay) {
    _delay_transform = delay;
  }

  void remove_speculative_types();
  void check_no_speculative_types() {
    _table.check_no_speculative_types();
  }

  bool is_dominator(Node *d, Node *n) { return is_dominator_helper(d, n, false); }
  bool no_dependent_zero_check(Node* n) const;

#ifndef PRODUCT
protected:
  // Sub-quadratic implementation of VerifyIterativeGVN.
  julong _verify_counter;
  julong _verify_full_passes;
  enum { _verify_window_size = 30 };
  Node* _verify_window[_verify_window_size];
  void verify_step(Node* n);
#endif
};

//------------------------------PhaseCCP---------------------------------------
// Phase for performing global Conditional Constant Propagation.
// Should be replaced with combined CCP & GVN someday.
class PhaseCCP : public PhaseIterGVN {
  // Non-recursive.  Use analysis to transform single Node.
  virtual Node *transform_once( Node *n );

public:
  PhaseCCP( PhaseIterGVN *igvn ); // Compute conditional constants
  NOT_PRODUCT( ~PhaseCCP(); )

  // Worklist algorithm identifies constants
  void analyze();
  // Recursive traversal of program.  Used analysis to modify program.
  virtual Node *transform( Node *n );
  // Do any transformation after analysis
  void          do_transform();

  virtual const Type* saturate(const Type* new_type, const Type* old_type,
                               const Type* limit_type) const;
  // Returns new_type->widen(old_type), which increments the widen bits until
  // giving up with TypeInt::INT or TypeLong::LONG.
  // Result is clipped to limit_type if necessary.

#ifndef PRODUCT
  static uint _total_invokes;    // For profiling, count invocations
  void    inc_invokes()          { ++PhaseCCP::_total_invokes; }

  static uint _total_constants;  // For profiling, count constants found
  uint   _count_constants;
  void    clear_constants()      { _count_constants = 0; }
  void    inc_constants()        { ++_count_constants; }
  uint    count_constants() const { return _count_constants; }

  static void print_statistics();
#endif
};


//------------------------------PhasePeephole----------------------------------
// Phase for performing peephole optimizations on register allocated basic blocks.
class PhasePeephole : public PhaseTransform {
  PhaseRegAlloc *_regalloc;
  PhaseCFG     &_cfg;
  // Recursive traversal of program.  Pure function is unused in this phase
  virtual Node *transform( Node *n );

public:
  PhasePeephole( PhaseRegAlloc *regalloc, PhaseCFG &cfg );
  NOT_PRODUCT( ~PhasePeephole(); )

  // Do any transformation after analysis
  void          do_transform();

#ifndef PRODUCT
  static uint _total_peepholes;  // For profiling, count peephole rules applied
  uint   _count_peepholes;
  void    clear_peepholes()      { _count_peepholes = 0; }
  void    inc_peepholes()        { ++_count_peepholes; }
  uint    count_peepholes() const { return _count_peepholes; }

  static void print_statistics();
#endif
};

#endif // SHARE_OPTO_PHASEX_HPP
