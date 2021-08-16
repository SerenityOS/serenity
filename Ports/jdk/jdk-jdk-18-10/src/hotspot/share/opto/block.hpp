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

#ifndef SHARE_OPTO_BLOCK_HPP
#define SHARE_OPTO_BLOCK_HPP

#include "opto/multnode.hpp"
#include "opto/node.hpp"
#include "opto/phase.hpp"
#include "utilities/powerOfTwo.hpp"

// Optimization - Graph Style

class Block;
class CFGLoop;
class MachCallNode;
class Matcher;
class RootNode;
class VectorSet;
class PhaseChaitin;
struct Tarjan;

//------------------------------Block_Array------------------------------------
// Map dense integer indices to Blocks.  Uses classic doubling-array trick.
// Abstractly provides an infinite array of Block*'s, initialized to NULL.
// Note that the constructor just zeros things, and since I use Arena
// allocation I do not need a destructor to reclaim storage.
class Block_Array : public ResourceObj {
  friend class VMStructs;
  uint _size;                   // allocated size, as opposed to formal limit
  debug_only(uint _limit;)      // limit to formal domain
  Arena *_arena;                // Arena to allocate in
protected:
  Block **_blocks;
  void grow( uint i );          // Grow array node to fit

public:
  Block_Array(Arena *a) : _size(OptoBlockListSize), _arena(a) {
    debug_only(_limit=0);
    _blocks = NEW_ARENA_ARRAY( a, Block *, OptoBlockListSize );
    for( int i = 0; i < OptoBlockListSize; i++ ) {
      _blocks[i] = NULL;
    }
  }
  Block *lookup( uint i ) const // Lookup, or NULL for not mapped
  { return (i<Max()) ? _blocks[i] : (Block*)NULL; }
  Block *operator[] ( uint i ) const // Lookup, or assert for not mapped
  { assert( i < Max(), "oob" ); return _blocks[i]; }
  // Extend the mapping: index i maps to Block *n.
  void map( uint i, Block *n ) { if( i>=Max() ) grow(i); _blocks[i] = n; }
  uint Max() const { debug_only(return _limit); return _size; }
};


class Block_List : public Block_Array {
  friend class VMStructs;
public:
  uint _cnt;
  Block_List() : Block_Array(Thread::current()->resource_area()), _cnt(0) {}
  void push( Block *b ) {  map(_cnt++,b); }
  Block *pop() { return _blocks[--_cnt]; }
  Block *rpop() { Block *b = _blocks[0]; _blocks[0]=_blocks[--_cnt]; return b;}
  void remove( uint i );
  void insert( uint i, Block *n );
  uint size() const { return _cnt; }
  void reset() { _cnt = 0; }
  void print();
};


class CFGElement : public ResourceObj {
  friend class VMStructs;
 public:
  double _freq; // Execution frequency (estimate)

  CFGElement() : _freq(0.0) {}
  virtual bool is_block() { return false; }
  virtual bool is_loop()  { return false; }
  Block*   as_Block() { assert(is_block(), "must be block"); return (Block*)this; }
  CFGLoop* as_CFGLoop()  { assert(is_loop(),  "must be loop");  return (CFGLoop*)this;  }
};

//------------------------------Block------------------------------------------
// This class defines a Basic Block.
// Basic blocks are used during the output routines, and are not used during
// any optimization pass.  They are created late in the game.
class Block : public CFGElement {
  friend class VMStructs;

private:
  // Nodes in this block, in order
  Node_List _nodes;

public:

  // Get the node at index 'at_index', if 'at_index' is out of bounds return NULL
  Node* get_node(uint at_index) const {
    return _nodes[at_index];
  }

  // Get the number of nodes in this block
  uint number_of_nodes() const {
    return _nodes.size();
  }

  // Map a node 'node' to index 'to_index' in the block, if the index is out of bounds the size of the node list is increased
  void map_node(Node* node, uint to_index) {
    _nodes.map(to_index, node);
  }

  // Insert a node 'node' at index 'at_index', moving all nodes that are on a higher index one step, if 'at_index' is out of bounds we crash
  void insert_node(Node* node, uint at_index) {
    _nodes.insert(at_index, node);
  }

  // Remove a node at index 'at_index'
  void remove_node(uint at_index) {
    _nodes.remove(at_index);
  }

  // Push a node 'node' onto the node list
  void push_node(Node* node) {
    _nodes.push(node);
  }

  // Pop the last node off the node list
  Node* pop_node() {
    return _nodes.pop();
  }

  // Basic blocks have a Node which defines Control for all Nodes pinned in
  // this block.  This Node is a RegionNode.  Exception-causing Nodes
  // (division, subroutines) and Phi functions are always pinned.  Later,
  // every Node will get pinned to some block.
  Node *head() const { return get_node(0); }

  // CAUTION: num_preds() is ONE based, so that predecessor numbers match
  // input edges to Regions and Phis.
  uint num_preds() const { return head()->req(); }
  Node *pred(uint i) const { return head()->in(i); }

  // Array of successor blocks, same size as projs array
  Block_Array _succs;

  // Basic blocks have some number of Nodes which split control to all
  // following blocks.  These Nodes are always Projections.  The field in
  // the Projection and the block-ending Node determine which Block follows.
  uint _num_succs;

  // Basic blocks also carry all sorts of good old fashioned DFS information
  // used to find loops, loop nesting depth, dominators, etc.
  uint _pre_order;              // Pre-order DFS number

  // Dominator tree
  uint _dom_depth;              // Depth in dominator tree for fast LCA
  Block* _idom;                 // Immediate dominator block

  CFGLoop *_loop;               // Loop to which this block belongs
  uint _rpo;                    // Number in reverse post order walk

  virtual bool is_block() { return true; }
  float succ_prob(uint i);      // return probability of i'th successor
  int num_fall_throughs();      // How many fall-through candidate this block has
  void update_uncommon_branch(Block* un); // Lower branch prob to uncommon code
  bool succ_fall_through(uint i); // Is successor "i" is a fall-through candidate
  Block* lone_fall_through();   // Return lone fall-through Block or null

  Block* dom_lca(Block* that);  // Compute LCA in dominator tree.

  bool dominates(Block* that) {
    int dom_diff = this->_dom_depth - that->_dom_depth;
    if (dom_diff > 0)  return false;
    for (; dom_diff < 0; dom_diff++)  that = that->_idom;
    return this == that;
  }

  // Report the alignment required by this block.  Must be a power of 2.
  // The previous block will insert nops to get this alignment.
  uint code_alignment() const;
  uint compute_loop_alignment();

  // BLOCK_FREQUENCY is a sentinel to mark uses of constant block frequencies.
  // It is currently also used to scale such frequencies relative to
  // FreqCountInvocations relative to the old value of 1500.
#define BLOCK_FREQUENCY(f) ((f * (double) 1500) / FreqCountInvocations)

  // Register Pressure (estimate) for Splitting heuristic
  uint _reg_pressure;
  uint _ihrp_index;
  uint _freg_pressure;
  uint _fhrp_index;

  // Mark and visited bits for an LCA calculation in insert_anti_dependences.
  // Since they hold unique node indexes, they do not need reinitialization.
  node_idx_t _raise_LCA_mark;
  void    set_raise_LCA_mark(node_idx_t x)    { _raise_LCA_mark = x; }
  node_idx_t  raise_LCA_mark() const          { return _raise_LCA_mark; }
  node_idx_t _raise_LCA_visited;
  void    set_raise_LCA_visited(node_idx_t x) { _raise_LCA_visited = x; }
  node_idx_t  raise_LCA_visited() const       { return _raise_LCA_visited; }

  // Estimated size in bytes of first instructions in a loop.
  uint _first_inst_size;
  uint first_inst_size() const     { return _first_inst_size; }
  void set_first_inst_size(uint s) { _first_inst_size = s; }

  // Compute the size of first instructions in this block.
  uint compute_first_inst_size(uint& sum_size, uint inst_cnt, PhaseRegAlloc* ra);

  // Compute alignment padding if the block needs it.
  // Align a loop if loop's padding is less or equal to padding limit
  // or the size of first instructions in the loop > padding.
  uint alignment_padding(int current_offset) {
    int block_alignment = code_alignment();
    int max_pad = block_alignment-relocInfo::addr_unit();
    if( max_pad > 0 ) {
      assert(is_power_of_2(max_pad+relocInfo::addr_unit()), "");
      int current_alignment = current_offset & max_pad;
      if( current_alignment != 0 ) {
        uint padding = (block_alignment-current_alignment) & max_pad;
        if( has_loop_alignment() &&
            padding > (uint)MaxLoopPad &&
            first_inst_size() <= padding ) {
          return 0;
        }
        return padding;
      }
    }
    return 0;
  }

  // Connector blocks. Connector blocks are basic blocks devoid of
  // instructions, but may have relevant non-instruction Nodes, such as
  // Phis or MergeMems. Such blocks are discovered and marked during the
  // RemoveEmpty phase, and elided during Output.
  bool _connector;
  void set_connector() { _connector = true; }
  bool is_connector() const { return _connector; };

  // Loop_alignment will be set for blocks which are at the top of loops.
  // The block layout pass may rotate loops such that the loop head may not
  // be the sequentially first block of the loop encountered in the linear
  // list of blocks.  If the layout pass is not run, loop alignment is set
  // for each block which is the head of a loop.
  uint _loop_alignment;
  void set_loop_alignment(Block *loop_top) {
    uint new_alignment = loop_top->compute_loop_alignment();
    if (new_alignment > _loop_alignment) {
      _loop_alignment = new_alignment;
    }
  }
  uint loop_alignment() const { return _loop_alignment; }
  bool has_loop_alignment() const { return loop_alignment() > 0; }

  // Create a new Block with given head Node.
  // Creates the (empty) predecessor arrays.
  Block( Arena *a, Node *headnode )
    : CFGElement(),
      _nodes(a),
      _succs(a),
      _num_succs(0),
      _pre_order(0),
      _idom(0),
      _loop(NULL),
      _reg_pressure(0),
      _ihrp_index(1),
      _freg_pressure(0),
      _fhrp_index(1),
      _raise_LCA_mark(0),
      _raise_LCA_visited(0),
      _first_inst_size(999999),
      _connector(false),
      _loop_alignment(0) {
    _nodes.push(headnode);
  }

  // Index of 'end' Node
  uint end_idx() const {
    // %%%%% add a proj after every goto
    // so (last->is_block_proj() != last) always, then simplify this code
    // This will not give correct end_idx for block 0 when it only contains root.
    int last_idx = _nodes.size() - 1;
    Node *last  = _nodes[last_idx];
    assert(last->is_block_proj() == last || last->is_block_proj() == _nodes[last_idx - _num_succs], "");
    return (last->is_block_proj() == last) ? last_idx : (last_idx - _num_succs);
  }

  // Basic blocks have a Node which ends them.  This Node determines which
  // basic block follows this one in the program flow.  This Node is either an
  // IfNode, a GotoNode, a JmpNode, or a ReturnNode.
  Node *end() const { return _nodes[end_idx()]; }

  // Add an instruction to an existing block.  It must go after the head
  // instruction and before the end instruction.
  void add_inst( Node *n ) { insert_node(n, end_idx()); }
  // Find node in block. Fails if node not in block.
  uint find_node( const Node *n ) const;
  // Find and remove n from block list
  void find_remove( const Node *n );
  // Check whether the node is in the block.
  bool contains (const Node *n) const;

  // Return the empty status of a block
  enum { not_empty, empty_with_goto, completely_empty };
  int is_Empty() const;

  // Forward through connectors
  Block* non_connector() {
    Block* s = this;
    while (s->is_connector()) {
      s = s->_succs[0];
    }
    return s;
  }

  // Return true if b is a successor of this block
  bool has_successor(Block* b) const {
    for (uint i = 0; i < _num_succs; i++ ) {
      if (non_connector_successor(i) == b) {
        return true;
      }
    }
    return false;
  }

  // Successor block, after forwarding through connectors
  Block* non_connector_successor(int i) const {
    return _succs[i]->non_connector();
  }

  // Examine block's code shape to predict if it is not commonly executed.
  bool has_uncommon_code() const;

#ifndef PRODUCT
  // Debugging print of basic block
  void dump_bidx(const Block* orig, outputStream* st = tty) const;
  void dump_pred(const PhaseCFG* cfg, Block* orig, outputStream* st = tty) const;
  void dump_head(const PhaseCFG* cfg, outputStream* st = tty) const;
  void dump() const;
  void dump(const PhaseCFG* cfg) const;
#endif
};


//------------------------------PhaseCFG---------------------------------------
// Build an array of Basic Block pointers, one per Node.
class PhaseCFG : public Phase {
  friend class VMStructs;
 private:
  // Root of whole program
  RootNode* _root;

  // The block containing the root node
  Block* _root_block;

  // List of basic blocks that are created during CFG creation
  Block_List _blocks;

  // Count of basic blocks
  uint _number_of_blocks;

  // Arena for the blocks to be stored in
  Arena* _block_arena;

  // Info used for scheduling
  PhaseChaitin* _regalloc;

  // Register pressure heuristic used?
  bool _scheduling_for_pressure;

  // The matcher for this compilation
  Matcher& _matcher;

  // Map nodes to owning basic block
  Block_Array _node_to_block_mapping;

  // Loop from the root
  CFGLoop* _root_loop;

  // Outmost loop frequency
  double _outer_loop_frequency;

  // Per node latency estimation, valid only during GCM
  GrowableArray<uint>* _node_latency;

  // Build a proper looking cfg.  Return count of basic blocks
  uint build_cfg();

  // Build the dominator tree so that we know where we can move instructions
  void build_dominator_tree();

  // Estimate block frequencies based on IfNode probabilities, so that we know where we want to move instructions
  void estimate_block_frequency();

  // Global Code Motion.  See Click's PLDI95 paper.  Place Nodes in specific
  // basic blocks; i.e. _node_to_block_mapping now maps _idx for all Nodes to some Block.
  // Move nodes to ensure correctness from GVN and also try to move nodes out of loops.
  void global_code_motion();

  // Schedule Nodes early in their basic blocks.
  bool schedule_early(VectorSet &visited, Node_Stack &roots);

  // For each node, find the latest block it can be scheduled into
  // and then select the cheapest block between the latest and earliest
  // block to place the node.
  void schedule_late(VectorSet &visited, Node_Stack &stack);

  // Compute the (backwards) latency of a node from a single use
  int latency_from_use(Node *n, const Node *def, Node *use);

  // Compute the (backwards) latency of a node from the uses of this instruction
  void partial_latency_of_defs(Node *n);

  // Compute the instruction global latency with a backwards walk
  void compute_latencies_backwards(VectorSet &visited, Node_Stack &stack);

  // Check if a block between early and LCA block of uses is cheaper by
  // frequency-based policy, latency-based policy and random-based policy
  bool is_cheaper_block(Block* LCA, Node* self, uint target_latency,
                        uint end_latency, double least_freq,
                        int cand_cnt, bool in_latency);

  // Pick a block between early and late that is a cheaper alternative
  // to late. Helper for schedule_late.
  Block* hoist_to_cheaper_block(Block* LCA, Block* early, Node* self);

  bool schedule_local(Block* block, GrowableArray<int>& ready_cnt, VectorSet& next_call, intptr_t* recacl_pressure_nodes);
  void set_next_call(Block* block, Node* n, VectorSet& next_call);
  void needed_for_next_call(Block* block, Node* this_call, VectorSet& next_call);

  // Perform basic-block local scheduling
  Node* select(Block* block, Node_List& worklist, GrowableArray<int>& ready_cnt, VectorSet& next_call, uint sched_slot,
               intptr_t* recacl_pressure_nodes);
  void adjust_register_pressure(Node* n, Block* block, intptr_t *recalc_pressure_nodes, bool finalize_mode);

  // Schedule a call next in the block
  uint sched_call(Block* block, uint node_cnt, Node_List& worklist, GrowableArray<int>& ready_cnt, MachCallNode* mcall, VectorSet& next_call);

  // Cleanup if any code lands between a Call and his Catch
  void call_catch_cleanup(Block* block);

  Node* catch_cleanup_find_cloned_def(Block* use_blk, Node* def, Block* def_blk, int n_clone_idx);
  void  catch_cleanup_inter_block(Node *use, Block *use_blk, Node *def, Block *def_blk, int n_clone_idx);

  // Detect implicit-null-check opportunities.  Basically, find NULL checks
  // with suitable memory ops nearby.  Use the memory op to do the NULL check.
  // I can generate a memory op if there is not one nearby.
  void implicit_null_check(Block* block, Node *proj, Node *val, int allowed_reasons);

  // Perform a Depth First Search (DFS).
  // Setup 'vertex' as DFS to vertex mapping.
  // Setup 'semi' as vertex to DFS mapping.
  // Set 'parent' to DFS parent.
  uint do_DFS(Tarjan* tarjan, uint rpo_counter);

  // Helper function to insert a node into a block
  void schedule_node_into_block( Node *n, Block *b );

  void replace_block_proj_ctrl( Node *n );

  // Set the basic block for pinned Nodes
  void schedule_pinned_nodes( VectorSet &visited );

  // I'll need a few machine-specific GotoNodes.  Clone from this one.
  // Used when building the CFG and creating end nodes for blocks.
  MachNode* _goto;

  Block* insert_anti_dependences(Block* LCA, Node* load, bool verify = false);
  void verify_anti_dependences(Block* LCA, Node* load) const {
    assert(LCA == get_block_for_node(load), "should already be scheduled");
    const_cast<PhaseCFG*>(this)->insert_anti_dependences(LCA, load, true);
  }

  bool move_to_next(Block* bx, uint b_index);
  void move_to_end(Block* bx, uint b_index);

  void insert_goto_at(uint block_no, uint succ_no);

  // Check for NeverBranch at block end.  This needs to become a GOTO to the
  // true target.  NeverBranch are treated as a conditional branch that always
  // goes the same direction for most of the optimizer and are used to give a
  // fake exit path to infinite loops.  At this late stage they need to turn
  // into Goto's so that when you enter the infinite loop you indeed hang.
  void convert_NeverBranch_to_Goto(Block *b);

  CFGLoop* create_loop_tree();
  bool is_dominator(Node* dom_node, Node* node);
  bool is_CFG(Node* n);
  bool is_control_proj_or_safepoint(Node* n) const;
  Block* find_block_for_node(Node* n) const;
  bool is_dominating_control(Node* dom_ctrl, Node* n);
  #ifndef PRODUCT
  bool _trace_opto_pipelining;  // tracing flag
  #endif

 public:
  PhaseCFG(Arena* arena, RootNode* root, Matcher& matcher);

  void set_latency_for_node(Node* node, int latency) {
    _node_latency->at_put_grow(node->_idx, latency);
  }

  uint get_latency_for_node(Node* node) {
    return _node_latency->at_grow(node->_idx);
  }

  // Get the outer most frequency
  double get_outer_loop_frequency() const {
    return _outer_loop_frequency;
  }

  // Get the root node of the CFG
  RootNode* get_root_node() const {
    return _root;
  }

  // Get the block of the root node
  Block* get_root_block() const {
    return _root_block;
  }

  // Add a block at a position and moves the later ones one step
  void add_block_at(uint pos, Block* block) {
    _blocks.insert(pos, block);
    _number_of_blocks++;
  }

  // Adds a block to the top of the block list
  void add_block(Block* block) {
    _blocks.push(block);
    _number_of_blocks++;
  }

  // Clear the list of blocks
  void clear_blocks() {
    _blocks.reset();
    _number_of_blocks = 0;
  }

  // Get the block at position pos in _blocks
  Block* get_block(uint pos) const {
    return _blocks[pos];
  }

  // Number of blocks
  uint number_of_blocks() const {
    return _number_of_blocks;
  }

  // set which block this node should reside in
  void map_node_to_block(const Node* node, Block* block) {
    _node_to_block_mapping.map(node->_idx, block);
  }

  // removes the mapping from a node to a block
  void unmap_node_from_block(const Node* node) {
    _node_to_block_mapping.map(node->_idx, NULL);
  }

  // get the block in which this node resides
  Block* get_block_for_node(const Node* node) const {
    return _node_to_block_mapping[node->_idx];
  }

  // does this node reside in a block; return true
  bool has_block(const Node* node) const {
    return (_node_to_block_mapping.lookup(node->_idx) != NULL);
  }

  // Use frequency calculations and code shape to predict if the block
  // is uncommon.
  bool is_uncommon(const Block* block);

#ifdef ASSERT
  Unique_Node_List _raw_oops;
#endif

  // Do global code motion by first building dominator tree and estimate block frequency
  // Returns true on success
  bool do_global_code_motion();

  // Compute the (backwards) latency of a node from the uses
  void latency_from_uses(Node *n);

  // Set loop alignment
  void set_loop_alignment();

  // Remove empty basic blocks
  void remove_empty_blocks();
  Block *fixup_trap_based_check(Node *branch, Block *block, int block_pos, Block *bnext);
  void fixup_flow();

  // Insert a node into a block at index and map the node to the block
  void insert(Block *b, uint idx, Node *n) {
    b->insert_node(n , idx);
    map_node_to_block(n, b);
  }

  // Check all nodes and postalloc_expand them if necessary.
  void postalloc_expand(PhaseRegAlloc* _ra);

#ifndef PRODUCT
  bool trace_opto_pipelining() const { return _trace_opto_pipelining; }

  // Debugging print of CFG
  void dump( ) const;           // CFG only
  void _dump_cfg( const Node *end, VectorSet &visited  ) const;
  void dump_headers();
#else
  bool trace_opto_pipelining() const { return false; }
#endif

  bool unrelated_load_in_store_null_block(Node* store, Node* load);

  // Check that block b is in the home loop (or an ancestor) of n, if n is a
  // memory writer.
  void verify_memory_writer_placement(const Block* b, const Node* n) const NOT_DEBUG_RETURN;
  void verify() const NOT_DEBUG_RETURN;
};


//------------------------------UnionFind--------------------------------------
// Map Block indices to a block-index for a cfg-cover.
// Array lookup in the optimized case.
class UnionFind : public ResourceObj {
  uint _cnt, _max;
  uint* _indices;
  ReallocMark _nesting;  // assertion check for reallocations
public:
  UnionFind( uint max );
  void reset( uint max );  // Reset to identity map for [0..max]

  uint lookup( uint nidx ) const {
    return _indices[nidx];
  }
  uint operator[] (uint nidx) const { return lookup(nidx); }

  void map( uint from_idx, uint to_idx ) {
    assert( from_idx < _cnt, "oob" );
    _indices[from_idx] = to_idx;
  }
  void extend( uint from_idx, uint to_idx );

  uint Size() const { return _cnt; }

  uint Find( uint idx ) {
    assert( idx < 65536, "Must fit into uint");
    uint uf_idx = lookup(idx);
    return (uf_idx == idx) ? uf_idx : Find_compress(idx);
  }
  uint Find_compress( uint idx );
  uint Find_const( uint idx ) const;
  void Union( uint idx1, uint idx2 );

};

//----------------------------BlockProbPair---------------------------
// Ordered pair of Node*.
class BlockProbPair {
protected:
  Block* _target;      // block target
  double  _prob;        // probability of edge to block
public:
  BlockProbPair() : _target(NULL), _prob(0.0) {}
  BlockProbPair(Block* b, double p) : _target(b), _prob(p) {}

  Block* get_target() const { return _target; }
  double get_prob() const { return _prob; }
};

//------------------------------CFGLoop-------------------------------------------
class CFGLoop : public CFGElement {
  friend class VMStructs;
  int _id;
  int _depth;
  CFGLoop *_parent;      // root of loop tree is the method level "pseudo" loop, it's parent is null
  CFGLoop *_sibling;     // null terminated list
  CFGLoop *_child;       // first child, use child's sibling to visit all immediately nested loops
  GrowableArray<CFGElement*> _members; // list of members of loop
  GrowableArray<BlockProbPair> _exits; // list of successor blocks and their probabilities
  double _exit_prob;       // probability any loop exit is taken on a single loop iteration
  void update_succ_freq(Block* b, double freq);

 public:
  CFGLoop(int id) :
    CFGElement(),
    _id(id),
    _depth(0),
    _parent(NULL),
    _sibling(NULL),
    _child(NULL),
    _exit_prob(1.0f) {}
  CFGLoop* parent() { return _parent; }
  void push_pred(Block* blk, int i, Block_List& worklist, PhaseCFG* cfg);
  void add_member(CFGElement *s) { _members.push(s); }
  void add_nested_loop(CFGLoop* cl);
  Block* head() {
    assert(_members.at(0)->is_block(), "head must be a block");
    Block* hd = _members.at(0)->as_Block();
    assert(hd->_loop == this, "just checking");
    assert(hd->head()->is_Loop(), "must begin with loop head node");
    return hd;
  }
  Block* backedge_block(); // Return the block on the backedge of the loop (else NULL)
  void compute_loop_depth(int depth);
  void compute_freq(); // compute frequency with loop assuming head freq 1.0f
  void scale_freq();   // scale frequency by loop trip count (including outer loops)
  double outer_loop_freq() const; // frequency of outer loop
  bool in_loop_nest(Block* b);
  double trip_count() const { return 1.0 / _exit_prob; }
  virtual bool is_loop()  { return true; }
  int id() { return _id; }
  int depth() { return _depth; }

#ifndef PRODUCT
  void dump( ) const;
  void dump_tree() const;
#endif
};


//----------------------------------CFGEdge------------------------------------
// A edge between two basic blocks that will be embodied by a branch or a
// fall-through.
class CFGEdge : public ResourceObj {
  friend class VMStructs;
 private:
  Block * _from;        // Source basic block
  Block * _to;          // Destination basic block
  double _freq;          // Execution frequency (estimate)
  int   _state;
  bool  _infrequent;
  int   _from_pct;
  int   _to_pct;

  // Private accessors
  int  from_pct() const { return _from_pct; }
  int  to_pct()   const { return _to_pct;   }
  int  from_infrequent() const { return from_pct() < BlockLayoutMinDiamondPercentage; }
  int  to_infrequent()   const { return to_pct()   < BlockLayoutMinDiamondPercentage; }

 public:
  enum {
    open,               // initial edge state; unprocessed
    connected,          // edge used to connect two traces together
    interior            // edge is interior to trace (could be backedge)
  };

  CFGEdge(Block *from, Block *to, double freq, int from_pct, int to_pct) :
    _from(from), _to(to), _freq(freq),
    _state(open), _from_pct(from_pct), _to_pct(to_pct) {
    _infrequent = from_infrequent() || to_infrequent();
  }

  double  freq() const { return _freq; }
  Block* from() const { return _from; }
  Block* to  () const { return _to;   }
  int  infrequent() const { return _infrequent; }
  int state() const { return _state; }

  void set_state(int state) { _state = state; }

#ifndef PRODUCT
  void dump( ) const;
#endif
};


//-----------------------------------Trace-------------------------------------
// An ordered list of basic blocks.
class Trace : public ResourceObj {
 private:
  uint _id;             // Unique Trace id (derived from initial block)
  Block ** _next_list;  // Array mapping index to next block
  Block ** _prev_list;  // Array mapping index to previous block
  Block * _first;       // First block in the trace
  Block * _last;        // Last block in the trace

  // Return the block that follows "b" in the trace.
  Block * next(Block *b) const { return _next_list[b->_pre_order]; }
  void set_next(Block *b, Block *n) const { _next_list[b->_pre_order] = n; }

  // Return the block that precedes "b" in the trace.
  Block * prev(Block *b) const { return _prev_list[b->_pre_order]; }
  void set_prev(Block *b, Block *p) const { _prev_list[b->_pre_order] = p; }

  // We've discovered a loop in this trace. Reset last to be "b", and first as
  // the block following "b
  void break_loop_after(Block *b) {
    _last = b;
    _first = next(b);
    set_prev(_first, NULL);
    set_next(_last, NULL);
  }

 public:

  Trace(Block *b, Block **next_list, Block **prev_list) :
    _id(b->_pre_order),
    _next_list(next_list),
    _prev_list(prev_list),
    _first(b),
    _last(b) {
    set_next(b, NULL);
    set_prev(b, NULL);
  };

  // Return the id number
  uint id() const { return _id; }
  void set_id(uint id) { _id = id; }

  // Return the first block in the trace
  Block * first_block() const { return _first; }

  // Return the last block in the trace
  Block * last_block() const { return _last; }

  // Insert a trace in the middle of this one after b
  void insert_after(Block *b, Trace *tr) {
    set_next(tr->last_block(), next(b));
    if (next(b) != NULL) {
      set_prev(next(b), tr->last_block());
    }

    set_next(b, tr->first_block());
    set_prev(tr->first_block(), b);

    if (b == _last) {
      _last = tr->last_block();
    }
  }

  void insert_before(Block *b, Trace *tr) {
    Block *p = prev(b);
    assert(p != NULL, "use append instead");
    insert_after(p, tr);
  }

  // Append another trace to this one.
  void append(Trace *tr) {
    insert_after(_last, tr);
  }

  // Append a block at the end of this trace
  void append(Block *b) {
    set_next(_last, b);
    set_prev(b, _last);
    _last = b;
  }

  // Adjust the the blocks in this trace
  void fixup_blocks(PhaseCFG &cfg);
  bool backedge(CFGEdge *e);

#ifndef PRODUCT
  void dump( ) const;
#endif
};

//------------------------------PhaseBlockLayout-------------------------------
// Rearrange blocks into some canonical order, based on edges and their frequencies
class PhaseBlockLayout : public Phase {
  friend class VMStructs;
  PhaseCFG &_cfg;               // Control flow graph

  GrowableArray<CFGEdge *> *edges;
  Trace **traces;
  Block **next;
  Block **prev;
  UnionFind *uf;

  // Given a block, find its encompassing Trace
  Trace * trace(Block *b) {
    return traces[uf->Find_compress(b->_pre_order)];
  }
 public:
  PhaseBlockLayout(PhaseCFG &cfg);

  void find_edges();
  void grow_traces();
  void merge_traces(bool loose_connections);
  void reorder_traces(int count);
  void union_traces(Trace* from, Trace* to);
};

#endif // SHARE_OPTO_BLOCK_HPP
