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
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/c2/barrierSetC2.hpp"
#include "libadt/vectset.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "opto/ad.hpp"
#include "opto/callGenerator.hpp"
#include "opto/castnode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/connode.hpp"
#include "opto/loopnode.hpp"
#include "opto/machnode.hpp"
#include "opto/matcher.hpp"
#include "opto/node.hpp"
#include "opto/opcodes.hpp"
#include "opto/regmask.hpp"
#include "opto/rootnode.hpp"
#include "opto/type.hpp"
#include "utilities/copy.hpp"
#include "utilities/macros.hpp"
#include "utilities/powerOfTwo.hpp"

class RegMask;
// #include "phase.hpp"
class PhaseTransform;
class PhaseGVN;

// Arena we are currently building Nodes in
const uint Node::NotAMachineReg = 0xffff0000;

#ifndef PRODUCT
extern int nodes_created;
#endif
#ifdef __clang__
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wuninitialized"
#endif

#ifdef ASSERT

//-------------------------- construct_node------------------------------------
// Set a breakpoint here to identify where a particular node index is built.
void Node::verify_construction() {
  _debug_orig = NULL;
  int old_debug_idx = Compile::debug_idx();
  int new_debug_idx = old_debug_idx + 1;
  if (new_debug_idx > 0) {
    // Arrange that the lowest five decimal digits of _debug_idx
    // will repeat those of _idx. In case this is somehow pathological,
    // we continue to assign negative numbers (!) consecutively.
    const int mod = 100000;
    int bump = (int)(_idx - new_debug_idx) % mod;
    if (bump < 0) {
      bump += mod;
    }
    assert(bump >= 0 && bump < mod, "");
    new_debug_idx += bump;
  }
  Compile::set_debug_idx(new_debug_idx);
  set_debug_idx(new_debug_idx);
  Compile* C = Compile::current();
  assert(C->unique() < (INT_MAX - 1), "Node limit exceeded INT_MAX");
  if (!C->phase_optimize_finished()) {
    // Only check assert during parsing and optimization phase. Skip it while generating code.
    assert(C->live_nodes() <= C->max_node_limit(), "Live Node limit exceeded limit");
  }
  if (BreakAtNode != 0 && (_debug_idx == BreakAtNode || (int)_idx == BreakAtNode)) {
    tty->print_cr("BreakAtNode: _idx=%d _debug_idx=%d", _idx, _debug_idx);
    BREAKPOINT;
  }
#if OPTO_DU_ITERATOR_ASSERT
  _last_del = NULL;
  _del_tick = 0;
#endif
  _hash_lock = 0;
}


// #ifdef ASSERT ...

#if OPTO_DU_ITERATOR_ASSERT
void DUIterator_Common::sample(const Node* node) {
  _vdui     = VerifyDUIterators;
  _node     = node;
  _outcnt   = node->_outcnt;
  _del_tick = node->_del_tick;
  _last     = NULL;
}

void DUIterator_Common::verify(const Node* node, bool at_end_ok) {
  assert(_node     == node, "consistent iterator source");
  assert(_del_tick == node->_del_tick, "no unexpected deletions allowed");
}

void DUIterator_Common::verify_resync() {
  // Ensure that the loop body has just deleted the last guy produced.
  const Node* node = _node;
  // Ensure that at least one copy of the last-seen edge was deleted.
  // Note:  It is OK to delete multiple copies of the last-seen edge.
  // Unfortunately, we have no way to verify that all the deletions delete
  // that same edge.  On this point we must use the Honor System.
  assert(node->_del_tick >= _del_tick+1, "must have deleted an edge");
  assert(node->_last_del == _last, "must have deleted the edge just produced");
  // We liked this deletion, so accept the resulting outcnt and tick.
  _outcnt   = node->_outcnt;
  _del_tick = node->_del_tick;
}

void DUIterator_Common::reset(const DUIterator_Common& that) {
  if (this == &that)  return;  // ignore assignment to self
  if (!_vdui) {
    // We need to initialize everything, overwriting garbage values.
    _last = that._last;
    _vdui = that._vdui;
  }
  // Note:  It is legal (though odd) for an iterator over some node x
  // to be reassigned to iterate over another node y.  Some doubly-nested
  // progress loops depend on being able to do this.
  const Node* node = that._node;
  // Re-initialize everything, except _last.
  _node     = node;
  _outcnt   = node->_outcnt;
  _del_tick = node->_del_tick;
}

void DUIterator::sample(const Node* node) {
  DUIterator_Common::sample(node);      // Initialize the assertion data.
  _refresh_tick = 0;                    // No refreshes have happened, as yet.
}

void DUIterator::verify(const Node* node, bool at_end_ok) {
  DUIterator_Common::verify(node, at_end_ok);
  assert(_idx      <  node->_outcnt + (uint)at_end_ok, "idx in range");
}

void DUIterator::verify_increment() {
  if (_refresh_tick & 1) {
    // We have refreshed the index during this loop.
    // Fix up _idx to meet asserts.
    if (_idx > _outcnt)  _idx = _outcnt;
  }
  verify(_node, true);
}

void DUIterator::verify_resync() {
  // Note:  We do not assert on _outcnt, because insertions are OK here.
  DUIterator_Common::verify_resync();
  // Make sure we are still in sync, possibly with no more out-edges:
  verify(_node, true);
}

void DUIterator::reset(const DUIterator& that) {
  if (this == &that)  return;  // self assignment is always a no-op
  assert(that._refresh_tick == 0, "assign only the result of Node::outs()");
  assert(that._idx          == 0, "assign only the result of Node::outs()");
  assert(_idx               == that._idx, "already assigned _idx");
  if (!_vdui) {
    // We need to initialize everything, overwriting garbage values.
    sample(that._node);
  } else {
    DUIterator_Common::reset(that);
    if (_refresh_tick & 1) {
      _refresh_tick++;                  // Clear the "was refreshed" flag.
    }
    assert(_refresh_tick < 2*100000, "DU iteration must converge quickly");
  }
}

void DUIterator::refresh() {
  DUIterator_Common::sample(_node);     // Re-fetch assertion data.
  _refresh_tick |= 1;                   // Set the "was refreshed" flag.
}

void DUIterator::verify_finish() {
  // If the loop has killed the node, do not require it to re-run.
  if (_node->_outcnt == 0)  _refresh_tick &= ~1;
  // If this assert triggers, it means that a loop used refresh_out_pos
  // to re-synch an iteration index, but the loop did not correctly
  // re-run itself, using a "while (progress)" construct.
  // This iterator enforces the rule that you must keep trying the loop
  // until it "runs clean" without any need for refreshing.
  assert(!(_refresh_tick & 1), "the loop must run once with no refreshing");
}


void DUIterator_Fast::verify(const Node* node, bool at_end_ok) {
  DUIterator_Common::verify(node, at_end_ok);
  Node** out    = node->_out;
  uint   cnt    = node->_outcnt;
  assert(cnt == _outcnt, "no insertions allowed");
  assert(_outp >= out && _outp <= out + cnt - !at_end_ok, "outp in range");
  // This last check is carefully designed to work for NO_OUT_ARRAY.
}

void DUIterator_Fast::verify_limit() {
  const Node* node = _node;
  verify(node, true);
  assert(_outp == node->_out + node->_outcnt, "limit still correct");
}

void DUIterator_Fast::verify_resync() {
  const Node* node = _node;
  if (_outp == node->_out + _outcnt) {
    // Note that the limit imax, not the pointer i, gets updated with the
    // exact count of deletions.  (For the pointer it's always "--i".)
    assert(node->_outcnt+node->_del_tick == _outcnt+_del_tick, "no insertions allowed with deletion(s)");
    // This is a limit pointer, with a name like "imax".
    // Fudge the _last field so that the common assert will be happy.
    _last = (Node*) node->_last_del;
    DUIterator_Common::verify_resync();
  } else {
    assert(node->_outcnt < _outcnt, "no insertions allowed with deletion(s)");
    // A normal internal pointer.
    DUIterator_Common::verify_resync();
    // Make sure we are still in sync, possibly with no more out-edges:
    verify(node, true);
  }
}

void DUIterator_Fast::verify_relimit(uint n) {
  const Node* node = _node;
  assert((int)n > 0, "use imax -= n only with a positive count");
  // This must be a limit pointer, with a name like "imax".
  assert(_outp == node->_out + node->_outcnt, "apply -= only to a limit (imax)");
  // The reported number of deletions must match what the node saw.
  assert(node->_del_tick == _del_tick + n, "must have deleted n edges");
  // Fudge the _last field so that the common assert will be happy.
  _last = (Node*) node->_last_del;
  DUIterator_Common::verify_resync();
}

void DUIterator_Fast::reset(const DUIterator_Fast& that) {
  assert(_outp              == that._outp, "already assigned _outp");
  DUIterator_Common::reset(that);
}

void DUIterator_Last::verify(const Node* node, bool at_end_ok) {
  // at_end_ok means the _outp is allowed to underflow by 1
  _outp += at_end_ok;
  DUIterator_Fast::verify(node, at_end_ok);  // check _del_tick, etc.
  _outp -= at_end_ok;
  assert(_outp == (node->_out + node->_outcnt) - 1, "pointer must point to end of nodes");
}

void DUIterator_Last::verify_limit() {
  // Do not require the limit address to be resynched.
  //verify(node, true);
  assert(_outp == _node->_out, "limit still correct");
}

void DUIterator_Last::verify_step(uint num_edges) {
  assert((int)num_edges > 0, "need non-zero edge count for loop progress");
  _outcnt   -= num_edges;
  _del_tick += num_edges;
  // Make sure we are still in sync, possibly with no more out-edges:
  const Node* node = _node;
  verify(node, true);
  assert(node->_last_del == _last, "must have deleted the edge just produced");
}

#endif //OPTO_DU_ITERATOR_ASSERT


#endif //ASSERT


// This constant used to initialize _out may be any non-null value.
// The value NULL is reserved for the top node only.
#define NO_OUT_ARRAY ((Node**)-1)

// Out-of-line code from node constructors.
// Executed only when extra debug info. is being passed around.
static void init_node_notes(Compile* C, int idx, Node_Notes* nn) {
  C->set_node_notes_at(idx, nn);
}

// Shared initialization code.
inline int Node::Init(int req) {
  Compile* C = Compile::current();
  int idx = C->next_unique();
  NOT_PRODUCT(_igv_idx = C->next_igv_idx());

  // Allocate memory for the necessary number of edges.
  if (req > 0) {
    // Allocate space for _in array to have double alignment.
    _in = (Node **) ((char *) (C->node_arena()->AmallocWords(req * sizeof(void*))));
  }
  // If there are default notes floating around, capture them:
  Node_Notes* nn = C->default_node_notes();
  if (nn != NULL)  init_node_notes(C, idx, nn);

  // Note:  At this point, C is dead,
  // and we begin to initialize the new Node.

  _cnt = _max = req;
  _outcnt = _outmax = 0;
  _class_id = Class_Node;
  _flags = 0;
  _out = NO_OUT_ARRAY;
  return idx;
}

//------------------------------Node-------------------------------------------
// Create a Node, with a given number of required edges.
Node::Node(uint req)
  : _idx(Init(req))
#ifdef ASSERT
  , _parse_idx(_idx)
  , _indent(0)
#endif
{
  assert( req < Compile::current()->max_node_limit() - NodeLimitFudgeFactor, "Input limit exceeded" );
  debug_only( verify_construction() );
  NOT_PRODUCT(nodes_created++);
  if (req == 0) {
    _in = NULL;
  } else {
    Node** to = _in;
    for(uint i = 0; i < req; i++) {
      to[i] = NULL;
    }
  }
}

//------------------------------Node-------------------------------------------
Node::Node(Node *n0)
  : _idx(Init(1))
#ifdef ASSERT
  , _parse_idx(_idx)
  , _indent(0)
#endif
{
  debug_only( verify_construction() );
  NOT_PRODUCT(nodes_created++);
  assert( is_not_dead(n0), "can not use dead node");
  _in[0] = n0; if (n0 != NULL) n0->add_out((Node *)this);
}

//------------------------------Node-------------------------------------------
Node::Node(Node *n0, Node *n1)
  : _idx(Init(2))
#ifdef ASSERT
  , _parse_idx(_idx)
  , _indent(0)
#endif
{
  debug_only( verify_construction() );
  NOT_PRODUCT(nodes_created++);
  assert( is_not_dead(n0), "can not use dead node");
  assert( is_not_dead(n1), "can not use dead node");
  _in[0] = n0; if (n0 != NULL) n0->add_out((Node *)this);
  _in[1] = n1; if (n1 != NULL) n1->add_out((Node *)this);
}

//------------------------------Node-------------------------------------------
Node::Node(Node *n0, Node *n1, Node *n2)
  : _idx(Init(3))
#ifdef ASSERT
  , _parse_idx(_idx)
  , _indent(0)
#endif
{
  debug_only( verify_construction() );
  NOT_PRODUCT(nodes_created++);
  assert( is_not_dead(n0), "can not use dead node");
  assert( is_not_dead(n1), "can not use dead node");
  assert( is_not_dead(n2), "can not use dead node");
  _in[0] = n0; if (n0 != NULL) n0->add_out((Node *)this);
  _in[1] = n1; if (n1 != NULL) n1->add_out((Node *)this);
  _in[2] = n2; if (n2 != NULL) n2->add_out((Node *)this);
}

//------------------------------Node-------------------------------------------
Node::Node(Node *n0, Node *n1, Node *n2, Node *n3)
  : _idx(Init(4))
#ifdef ASSERT
  , _parse_idx(_idx)
  , _indent(0)
#endif
{
  debug_only( verify_construction() );
  NOT_PRODUCT(nodes_created++);
  assert( is_not_dead(n0), "can not use dead node");
  assert( is_not_dead(n1), "can not use dead node");
  assert( is_not_dead(n2), "can not use dead node");
  assert( is_not_dead(n3), "can not use dead node");
  _in[0] = n0; if (n0 != NULL) n0->add_out((Node *)this);
  _in[1] = n1; if (n1 != NULL) n1->add_out((Node *)this);
  _in[2] = n2; if (n2 != NULL) n2->add_out((Node *)this);
  _in[3] = n3; if (n3 != NULL) n3->add_out((Node *)this);
}

//------------------------------Node-------------------------------------------
Node::Node(Node *n0, Node *n1, Node *n2, Node *n3, Node *n4)
  : _idx(Init(5))
#ifdef ASSERT
  , _parse_idx(_idx)
  , _indent(0)
#endif
{
  debug_only( verify_construction() );
  NOT_PRODUCT(nodes_created++);
  assert( is_not_dead(n0), "can not use dead node");
  assert( is_not_dead(n1), "can not use dead node");
  assert( is_not_dead(n2), "can not use dead node");
  assert( is_not_dead(n3), "can not use dead node");
  assert( is_not_dead(n4), "can not use dead node");
  _in[0] = n0; if (n0 != NULL) n0->add_out((Node *)this);
  _in[1] = n1; if (n1 != NULL) n1->add_out((Node *)this);
  _in[2] = n2; if (n2 != NULL) n2->add_out((Node *)this);
  _in[3] = n3; if (n3 != NULL) n3->add_out((Node *)this);
  _in[4] = n4; if (n4 != NULL) n4->add_out((Node *)this);
}

//------------------------------Node-------------------------------------------
Node::Node(Node *n0, Node *n1, Node *n2, Node *n3,
                     Node *n4, Node *n5)
  : _idx(Init(6))
#ifdef ASSERT
  , _parse_idx(_idx)
  , _indent(0)
#endif
{
  debug_only( verify_construction() );
  NOT_PRODUCT(nodes_created++);
  assert( is_not_dead(n0), "can not use dead node");
  assert( is_not_dead(n1), "can not use dead node");
  assert( is_not_dead(n2), "can not use dead node");
  assert( is_not_dead(n3), "can not use dead node");
  assert( is_not_dead(n4), "can not use dead node");
  assert( is_not_dead(n5), "can not use dead node");
  _in[0] = n0; if (n0 != NULL) n0->add_out((Node *)this);
  _in[1] = n1; if (n1 != NULL) n1->add_out((Node *)this);
  _in[2] = n2; if (n2 != NULL) n2->add_out((Node *)this);
  _in[3] = n3; if (n3 != NULL) n3->add_out((Node *)this);
  _in[4] = n4; if (n4 != NULL) n4->add_out((Node *)this);
  _in[5] = n5; if (n5 != NULL) n5->add_out((Node *)this);
}

//------------------------------Node-------------------------------------------
Node::Node(Node *n0, Node *n1, Node *n2, Node *n3,
                     Node *n4, Node *n5, Node *n6)
  : _idx(Init(7))
#ifdef ASSERT
  , _parse_idx(_idx)
  , _indent(0)
#endif
{
  debug_only( verify_construction() );
  NOT_PRODUCT(nodes_created++);
  assert( is_not_dead(n0), "can not use dead node");
  assert( is_not_dead(n1), "can not use dead node");
  assert( is_not_dead(n2), "can not use dead node");
  assert( is_not_dead(n3), "can not use dead node");
  assert( is_not_dead(n4), "can not use dead node");
  assert( is_not_dead(n5), "can not use dead node");
  assert( is_not_dead(n6), "can not use dead node");
  _in[0] = n0; if (n0 != NULL) n0->add_out((Node *)this);
  _in[1] = n1; if (n1 != NULL) n1->add_out((Node *)this);
  _in[2] = n2; if (n2 != NULL) n2->add_out((Node *)this);
  _in[3] = n3; if (n3 != NULL) n3->add_out((Node *)this);
  _in[4] = n4; if (n4 != NULL) n4->add_out((Node *)this);
  _in[5] = n5; if (n5 != NULL) n5->add_out((Node *)this);
  _in[6] = n6; if (n6 != NULL) n6->add_out((Node *)this);
}

#ifdef __clang__
#pragma clang diagnostic pop
#endif


//------------------------------clone------------------------------------------
// Clone a Node.
Node *Node::clone() const {
  Compile* C = Compile::current();
  uint s = size_of();           // Size of inherited Node
  Node *n = (Node*)C->node_arena()->AmallocWords(size_of() + _max*sizeof(Node*));
  Copy::conjoint_words_to_lower((HeapWord*)this, (HeapWord*)n, s);
  // Set the new input pointer array
  n->_in = (Node**)(((char*)n)+s);
  // Cannot share the old output pointer array, so kill it
  n->_out = NO_OUT_ARRAY;
  // And reset the counters to 0
  n->_outcnt = 0;
  n->_outmax = 0;
  // Unlock this guy, since he is not in any hash table.
  debug_only(n->_hash_lock = 0);
  // Walk the old node's input list to duplicate its edges
  uint i;
  for( i = 0; i < len(); i++ ) {
    Node *x = in(i);
    n->_in[i] = x;
    if (x != NULL) x->add_out(n);
  }
  if (is_macro()) {
    C->add_macro_node(n);
  }
  if (is_expensive()) {
    C->add_expensive_node(n);
  }
  if (for_post_loop_opts_igvn()) {
    // Don't add cloned node to Compile::_for_post_loop_opts_igvn list automatically.
    // If it is applicable, it will happen anyway when the cloned node is registered with IGVN.
    n->remove_flag(Node::NodeFlags::Flag_for_post_loop_opts_igvn);
  }
  if (n->is_reduction()) {
    // Do not copy reduction information. This must be explicitly set by the calling code.
    n->remove_flag(Node::Flag_is_reduction);
  }
  BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
  bs->register_potential_barrier_node(n);

  n->set_idx(C->next_unique()); // Get new unique index as well
  NOT_PRODUCT(n->_igv_idx = C->next_igv_idx());
  debug_only( n->verify_construction() );
  NOT_PRODUCT(nodes_created++);
  // Do not patch over the debug_idx of a clone, because it makes it
  // impossible to break on the clone's moment of creation.
  //debug_only( n->set_debug_idx( debug_idx() ) );

  C->copy_node_notes_to(n, (Node*) this);

  // MachNode clone
  uint nopnds;
  if (this->is_Mach() && (nopnds = this->as_Mach()->num_opnds()) > 0) {
    MachNode *mach  = n->as_Mach();
    MachNode *mthis = this->as_Mach();
    // Get address of _opnd_array.
    // It should be the same offset since it is the clone of this node.
    MachOper **from = mthis->_opnds;
    MachOper **to = (MachOper **)((size_t)(&mach->_opnds) +
                    pointer_delta((const void*)from,
                                  (const void*)(&mthis->_opnds), 1));
    mach->_opnds = to;
    for ( uint i = 0; i < nopnds; ++i ) {
      to[i] = from[i]->clone();
    }
  }
  if (n->is_Call()) {
    // CallGenerator is linked to the original node.
    CallGenerator* cg = n->as_Call()->generator();
    if (cg != NULL) {
      CallGenerator* cloned_cg = cg->with_call_node(n->as_Call());
      n->as_Call()->set_generator(cloned_cg);

      C->print_inlining_assert_ready();
      C->print_inlining_move_to(cg);
      C->print_inlining_update(cloned_cg);
    }
  }
  if (n->is_SafePoint()) {
    // Scalar replacement and macro expansion might modify the JVMState.
    // Clone it to make sure it's not shared between SafePointNodes.
    n->as_SafePoint()->clone_jvms(C);
    n->as_SafePoint()->clone_replaced_nodes();
  }
  return n;                     // Return the clone
}

//---------------------------setup_is_top--------------------------------------
// Call this when changing the top node, to reassert the invariants
// required by Node::is_top.  See Compile::set_cached_top_node.
void Node::setup_is_top() {
  if (this == (Node*)Compile::current()->top()) {
    // This node has just become top.  Kill its out array.
    _outcnt = _outmax = 0;
    _out = NULL;                           // marker value for top
    assert(is_top(), "must be top");
  } else {
    if (_out == NULL)  _out = NO_OUT_ARRAY;
    assert(!is_top(), "must not be top");
  }
}

//------------------------------~Node------------------------------------------
// Fancy destructor; eagerly attempt to reclaim Node numberings and storage
void Node::destruct(PhaseValues* phase) {
  Compile* compile = (phase != NULL) ? phase->C : Compile::current();
  if (phase != NULL && phase->is_IterGVN()) {
    phase->is_IterGVN()->_worklist.remove(this);
  }
  // If this is the most recently created node, reclaim its index. Otherwise,
  // record the node as dead to keep liveness information accurate.
  if ((uint)_idx+1 == compile->unique()) {
    compile->set_unique(compile->unique()-1);
  } else {
    compile->record_dead_node(_idx);
  }
  // Clear debug info:
  Node_Notes* nn = compile->node_notes_at(_idx);
  if (nn != NULL)  nn->clear();
  // Walk the input array, freeing the corresponding output edges
  _cnt = _max;  // forget req/prec distinction
  uint i;
  for( i = 0; i < _max; i++ ) {
    set_req(i, NULL);
    //assert(def->out(def->outcnt()-1) == (Node *)this,"bad def-use hacking in reclaim");
  }
  assert(outcnt() == 0, "deleting a node must not leave a dangling use");
  // See if the input array was allocated just prior to the object
  int edge_size = _max*sizeof(void*);
  int out_edge_size = _outmax*sizeof(void*);
  char *edge_end = ((char*)_in) + edge_size;
  char *out_array = (char*)(_out == NO_OUT_ARRAY? NULL: _out);
  int node_size = size_of();

  // Free the output edge array
  if (out_edge_size > 0) {
    compile->node_arena()->Afree(out_array, out_edge_size);
  }

  // Free the input edge array and the node itself
  if( edge_end == (char*)this ) {
    // It was; free the input array and object all in one hit
#ifndef ASSERT
    compile->node_arena()->Afree(_in,edge_size+node_size);
#endif
  } else {
    // Free just the input array
    compile->node_arena()->Afree(_in,edge_size);

    // Free just the object
#ifndef ASSERT
    compile->node_arena()->Afree(this,node_size);
#endif
  }
  if (is_macro()) {
    compile->remove_macro_node(this);
  }
  if (is_expensive()) {
    compile->remove_expensive_node(this);
  }
  if (Opcode() == Op_Opaque4) {
    compile->remove_skeleton_predicate_opaq(this);
  }
  if (for_post_loop_opts_igvn()) {
    compile->remove_from_post_loop_opts_igvn(this);
  }

  if (is_SafePoint()) {
    as_SafePoint()->delete_replaced_nodes();
  }
  BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
  bs->unregister_potential_barrier_node(this);
#ifdef ASSERT
  // We will not actually delete the storage, but we'll make the node unusable.
  *(address*)this = badAddress;  // smash the C++ vtbl, probably
  _in = _out = (Node**) badAddress;
  _max = _cnt = _outmax = _outcnt = 0;
  compile->remove_modified_node(this);
#endif
}

//------------------------------grow-------------------------------------------
// Grow the input array, making space for more edges
void Node::grow(uint len) {
  Arena* arena = Compile::current()->node_arena();
  uint new_max = _max;
  if( new_max == 0 ) {
    _max = 4;
    _in = (Node**)arena->Amalloc(4*sizeof(Node*));
    Node** to = _in;
    to[0] = NULL;
    to[1] = NULL;
    to[2] = NULL;
    to[3] = NULL;
    return;
  }
  new_max = next_power_of_2(len);
  // Trimming to limit allows a uint8 to handle up to 255 edges.
  // Previously I was using only powers-of-2 which peaked at 128 edges.
  //if( new_max >= limit ) new_max = limit-1;
  _in = (Node**)arena->Arealloc(_in, _max*sizeof(Node*), new_max*sizeof(Node*));
  Copy::zero_to_bytes(&_in[_max], (new_max-_max)*sizeof(Node*)); // NULL all new space
  _max = new_max;               // Record new max length
  // This assertion makes sure that Node::_max is wide enough to
  // represent the numerical value of new_max.
  assert(_max == new_max && _max > len, "int width of _max is too small");
}

//-----------------------------out_grow----------------------------------------
// Grow the input array, making space for more edges
void Node::out_grow( uint len ) {
  assert(!is_top(), "cannot grow a top node's out array");
  Arena* arena = Compile::current()->node_arena();
  uint new_max = _outmax;
  if( new_max == 0 ) {
    _outmax = 4;
    _out = (Node **)arena->Amalloc(4*sizeof(Node*));
    return;
  }
  new_max = next_power_of_2(len);
  // Trimming to limit allows a uint8 to handle up to 255 edges.
  // Previously I was using only powers-of-2 which peaked at 128 edges.
  //if( new_max >= limit ) new_max = limit-1;
  assert(_out != NULL && _out != NO_OUT_ARRAY, "out must have sensible value");
  _out = (Node**)arena->Arealloc(_out,_outmax*sizeof(Node*),new_max*sizeof(Node*));
  //Copy::zero_to_bytes(&_out[_outmax], (new_max-_outmax)*sizeof(Node*)); // NULL all new space
  _outmax = new_max;               // Record new max length
  // This assertion makes sure that Node::_max is wide enough to
  // represent the numerical value of new_max.
  assert(_outmax == new_max && _outmax > len, "int width of _outmax is too small");
}

#ifdef ASSERT
//------------------------------is_dead----------------------------------------
bool Node::is_dead() const {
  // Mach and pinch point nodes may look like dead.
  if( is_top() || is_Mach() || (Opcode() == Op_Node && _outcnt > 0) )
    return false;
  for( uint i = 0; i < _max; i++ )
    if( _in[i] != NULL )
      return false;
  dump();
  return true;
}

bool Node::is_reachable_from_root() const {
  ResourceMark rm;
  Unique_Node_List wq;
  wq.push((Node*)this);
  RootNode* root = Compile::current()->root();
  for (uint i = 0; i < wq.size(); i++) {
    Node* m = wq.at(i);
    if (m == root) {
      return true;
    }
    for (DUIterator_Fast jmax, j = m->fast_outs(jmax); j < jmax; j++) {
      Node* u = m->fast_out(j);
      wq.push(u);
    }
  }
  return false;
}
#endif

//------------------------------is_unreachable---------------------------------
bool Node::is_unreachable(PhaseIterGVN &igvn) const {
  assert(!is_Mach(), "doesn't work with MachNodes");
  return outcnt() == 0 || igvn.type(this) == Type::TOP || (in(0) != NULL && in(0)->is_top());
}

//------------------------------add_req----------------------------------------
// Add a new required input at the end
void Node::add_req( Node *n ) {
  assert( is_not_dead(n), "can not use dead node");

  // Look to see if I can move precedence down one without reallocating
  if( (_cnt >= _max) || (in(_max-1) != NULL) )
    grow( _max+1 );

  // Find a precedence edge to move
  if( in(_cnt) != NULL ) {       // Next precedence edge is busy?
    uint i;
    for( i=_cnt; i<_max; i++ )
      if( in(i) == NULL )       // Find the NULL at end of prec edge list
        break;                  // There must be one, since we grew the array
    _in[i] = in(_cnt);          // Move prec over, making space for req edge
  }
  _in[_cnt++] = n;            // Stuff over old prec edge
  if (n != NULL) n->add_out((Node *)this);
}

//---------------------------add_req_batch-------------------------------------
// Add a new required input at the end
void Node::add_req_batch( Node *n, uint m ) {
  assert( is_not_dead(n), "can not use dead node");
  // check various edge cases
  if ((int)m <= 1) {
    assert((int)m >= 0, "oob");
    if (m != 0)  add_req(n);
    return;
  }

  // Look to see if I can move precedence down one without reallocating
  if( (_cnt+m) > _max || _in[_max-m] )
    grow( _max+m );

  // Find a precedence edge to move
  if( _in[_cnt] != NULL ) {     // Next precedence edge is busy?
    uint i;
    for( i=_cnt; i<_max; i++ )
      if( _in[i] == NULL )      // Find the NULL at end of prec edge list
        break;                  // There must be one, since we grew the array
    // Slide all the precs over by m positions (assume #prec << m).
    Copy::conjoint_words_to_higher((HeapWord*)&_in[_cnt], (HeapWord*)&_in[_cnt+m], ((i-_cnt)*sizeof(Node*)));
  }

  // Stuff over the old prec edges
  for(uint i=0; i<m; i++ ) {
    _in[_cnt++] = n;
  }

  // Insert multiple out edges on the node.
  if (n != NULL && !n->is_top()) {
    for(uint i=0; i<m; i++ ) {
      n->add_out((Node *)this);
    }
  }
}

//------------------------------del_req----------------------------------------
// Delete the required edge and compact the edge array
void Node::del_req( uint idx ) {
  assert( idx < _cnt, "oob");
  assert( !VerifyHashTableKeys || _hash_lock == 0,
          "remove node from hash table before modifying it");
  // First remove corresponding def-use edge
  Node *n = in(idx);
  if (n != NULL) n->del_out((Node *)this);
  _in[idx] = in(--_cnt); // Compact the array
  // Avoid spec violation: Gap in prec edges.
  close_prec_gap_at(_cnt);
  Compile::current()->record_modified_node(this);
}

//------------------------------del_req_ordered--------------------------------
// Delete the required edge and compact the edge array with preserved order
void Node::del_req_ordered( uint idx ) {
  assert( idx < _cnt, "oob");
  assert( !VerifyHashTableKeys || _hash_lock == 0,
          "remove node from hash table before modifying it");
  // First remove corresponding def-use edge
  Node *n = in(idx);
  if (n != NULL) n->del_out((Node *)this);
  if (idx < --_cnt) {    // Not last edge ?
    Copy::conjoint_words_to_lower((HeapWord*)&_in[idx+1], (HeapWord*)&_in[idx], ((_cnt-idx)*sizeof(Node*)));
  }
  // Avoid spec violation: Gap in prec edges.
  close_prec_gap_at(_cnt);
  Compile::current()->record_modified_node(this);
}

//------------------------------ins_req----------------------------------------
// Insert a new required input at the end
void Node::ins_req( uint idx, Node *n ) {
  assert( is_not_dead(n), "can not use dead node");
  add_req(NULL);                // Make space
  assert( idx < _max, "Must have allocated enough space");
  // Slide over
  if(_cnt-idx-1 > 0) {
    Copy::conjoint_words_to_higher((HeapWord*)&_in[idx], (HeapWord*)&_in[idx+1], ((_cnt-idx-1)*sizeof(Node*)));
  }
  _in[idx] = n;                            // Stuff over old required edge
  if (n != NULL) n->add_out((Node *)this); // Add reciprocal def-use edge
}

//-----------------------------find_edge---------------------------------------
int Node::find_edge(Node* n) {
  for (uint i = 0; i < len(); i++) {
    if (_in[i] == n)  return i;
  }
  return -1;
}

//----------------------------replace_edge-------------------------------------
int Node::replace_edge(Node* old, Node* neww, PhaseGVN* gvn) {
  if (old == neww)  return 0;  // nothing to do
  uint nrep = 0;
  for (uint i = 0; i < len(); i++) {
    if (in(i) == old) {
      if (i < req()) {
        if (gvn != NULL) {
          set_req_X(i, neww, gvn);
        } else {
          set_req(i, neww);
        }
      } else {
        assert(gvn == NULL || gvn->is_IterGVN() == NULL, "no support for igvn here");
        assert(find_prec_edge(neww) == -1, "spec violation: duplicated prec edge (node %d -> %d)", _idx, neww->_idx);
        set_prec(i, neww);
      }
      nrep++;
    }
  }
  return nrep;
}

/**
 * Replace input edges in the range pointing to 'old' node.
 */
int Node::replace_edges_in_range(Node* old, Node* neww, int start, int end, PhaseGVN* gvn) {
  if (old == neww)  return 0;  // nothing to do
  uint nrep = 0;
  for (int i = start; i < end; i++) {
    if (in(i) == old) {
      set_req_X(i, neww, gvn);
      nrep++;
    }
  }
  return nrep;
}

//-------------------------disconnect_inputs-----------------------------------
// NULL out all inputs to eliminate incoming Def-Use edges.
void Node::disconnect_inputs(Compile* C) {
  // the layout of Node::_in
  // r: a required input, null is allowed
  // p: a precedence, null values are all at the end
  // -----------------------------------
  // |r|...|r|p|...|p|null|...|null|
  //         |                     |
  //         req()                 len()
  // -----------------------------------
  for (uint i = 0; i < req(); ++i) {
    if (in(i) != nullptr) {
      set_req(i, nullptr);
    }
  }

  // Remove precedence edges if any exist
  // Note: Safepoints may have precedence edges, even during parsing
  for (uint i = len(); i > req(); ) {
    rm_prec(--i);  // no-op if _in[i] is nullptr
  }

#ifdef ASSERT
  // sanity check
  for (uint i = 0; i < len(); ++i) {
    assert(_in[i] == nullptr, "disconnect_inputs() failed!");
  }
#endif

  // Node::destruct requires all out edges be deleted first
  // debug_only(destruct();)   // no reuse benefit expected
  C->record_dead_node(_idx);
}

//-----------------------------uncast---------------------------------------
// %%% Temporary, until we sort out CheckCastPP vs. CastPP.
// Strip away casting.  (It is depth-limited.)
// Optionally, keep casts with dependencies.
Node* Node::uncast(bool keep_deps) const {
  // Should be inline:
  //return is_ConstraintCast() ? uncast_helper(this) : (Node*) this;
  if (is_ConstraintCast()) {
    return uncast_helper(this, keep_deps);
  } else {
    return (Node*) this;
  }
}

// Find out of current node that matches opcode.
Node* Node::find_out_with(int opcode) {
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    Node* use = fast_out(i);
    if (use->Opcode() == opcode) {
      return use;
    }
  }
  return NULL;
}

// Return true if the current node has an out that matches opcode.
bool Node::has_out_with(int opcode) {
  return (find_out_with(opcode) != NULL);
}

// Return true if the current node has an out that matches any of the opcodes.
bool Node::has_out_with(int opcode1, int opcode2, int opcode3, int opcode4) {
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
      int opcode = fast_out(i)->Opcode();
      if (opcode == opcode1 || opcode == opcode2 || opcode == opcode3 || opcode == opcode4) {
        return true;
      }
  }
  return false;
}


//---------------------------uncast_helper-------------------------------------
Node* Node::uncast_helper(const Node* p, bool keep_deps) {
#ifdef ASSERT
  uint depth_count = 0;
  const Node* orig_p = p;
#endif

  while (true) {
#ifdef ASSERT
    if (depth_count >= K) {
      orig_p->dump(4);
      if (p != orig_p)
        p->dump(1);
    }
    assert(depth_count++ < K, "infinite loop in Node::uncast_helper");
#endif
    if (p == NULL || p->req() != 2) {
      break;
    } else if (p->is_ConstraintCast()) {
      if (keep_deps && p->as_ConstraintCast()->carry_dependency()) {
        break; // stop at casts with dependencies
      }
      p = p->in(1);
    } else {
      break;
    }
  }
  return (Node*) p;
}

//------------------------------add_prec---------------------------------------
// Add a new precedence input.  Precedence inputs are unordered, with
// duplicates removed and NULLs packed down at the end.
void Node::add_prec( Node *n ) {
  assert( is_not_dead(n), "can not use dead node");

  // Check for NULL at end
  if( _cnt >= _max || in(_max-1) )
    grow( _max+1 );

  // Find a precedence edge to move
  uint i = _cnt;
  while( in(i) != NULL ) {
    if (in(i) == n) return; // Avoid spec violation: duplicated prec edge.
    i++;
  }
  _in[i] = n;                                // Stuff prec edge over NULL
  if ( n != NULL) n->add_out((Node *)this);  // Add mirror edge

#ifdef ASSERT
  while ((++i)<_max) { assert(_in[i] == NULL, "spec violation: Gap in prec edges (node %d)", _idx); }
#endif
}

//------------------------------rm_prec----------------------------------------
// Remove a precedence input.  Precedence inputs are unordered, with
// duplicates removed and NULLs packed down at the end.
void Node::rm_prec( uint j ) {
  assert(j < _max, "oob: i=%d, _max=%d", j, _max);
  assert(j >= _cnt, "not a precedence edge");
  if (_in[j] == NULL) return;   // Avoid spec violation: Gap in prec edges.
  _in[j]->del_out((Node *)this);
  close_prec_gap_at(j);
}

//------------------------------size_of----------------------------------------
uint Node::size_of() const { return sizeof(*this); }

//------------------------------ideal_reg--------------------------------------
uint Node::ideal_reg() const { return 0; }

//------------------------------jvms-------------------------------------------
JVMState* Node::jvms() const { return NULL; }

#ifdef ASSERT
//------------------------------jvms-------------------------------------------
bool Node::verify_jvms(const JVMState* using_jvms) const {
  for (JVMState* jvms = this->jvms(); jvms != NULL; jvms = jvms->caller()) {
    if (jvms == using_jvms)  return true;
  }
  return false;
}

//------------------------------init_NodeProperty------------------------------
void Node::init_NodeProperty() {
  assert(_max_classes <= max_juint, "too many NodeProperty classes");
  assert(max_flags() <= max_juint, "too many NodeProperty flags");
}

//-----------------------------max_flags---------------------------------------
juint Node::max_flags() {
  return (PD::_last_flag << 1) - 1; // allow flags combination
}
#endif

//------------------------------format-----------------------------------------
// Print as assembly
void Node::format( PhaseRegAlloc *, outputStream *st ) const {}
//------------------------------emit-------------------------------------------
// Emit bytes starting at parameter 'ptr'.
void Node::emit(CodeBuffer &cbuf, PhaseRegAlloc *ra_) const {}
//------------------------------size-------------------------------------------
// Size of instruction in bytes
uint Node::size(PhaseRegAlloc *ra_) const { return 0; }

//------------------------------CFG Construction-------------------------------
// Nodes that end basic blocks, e.g. IfTrue/IfFalse, JumpProjNode, Root,
// Goto and Return.
const Node *Node::is_block_proj() const { return 0; }

// Minimum guaranteed type
const Type *Node::bottom_type() const { return Type::BOTTOM; }


//------------------------------raise_bottom_type------------------------------
// Get the worst-case Type output for this Node.
void Node::raise_bottom_type(const Type* new_type) {
  if (is_Type()) {
    TypeNode *n = this->as_Type();
    if (VerifyAliases) {
      assert(new_type->higher_equal_speculative(n->type()), "new type must refine old type");
    }
    n->set_type(new_type);
  } else if (is_Load()) {
    LoadNode *n = this->as_Load();
    if (VerifyAliases) {
      assert(new_type->higher_equal_speculative(n->type()), "new type must refine old type");
    }
    n->set_type(new_type);
  }
}

//------------------------------Identity---------------------------------------
// Return a node that the given node is equivalent to.
Node* Node::Identity(PhaseGVN* phase) {
  return this;                  // Default to no identities
}

//------------------------------Value------------------------------------------
// Compute a new Type for a node using the Type of the inputs.
const Type* Node::Value(PhaseGVN* phase) const {
  return bottom_type();         // Default to worst-case Type
}

//------------------------------Ideal------------------------------------------
//
// 'Idealize' the graph rooted at this Node.
//
// In order to be efficient and flexible there are some subtle invariants
// these Ideal calls need to hold.  Running with '+VerifyIterativeGVN' checks
// these invariants, although its too slow to have on by default.  If you are
// hacking an Ideal call, be sure to test with +VerifyIterativeGVN!
//
// The Ideal call almost arbitrarily reshape the graph rooted at the 'this'
// pointer.  If ANY change is made, it must return the root of the reshaped
// graph - even if the root is the same Node.  Example: swapping the inputs
// to an AddINode gives the same answer and same root, but you still have to
// return the 'this' pointer instead of NULL.
//
// You cannot return an OLD Node, except for the 'this' pointer.  Use the
// Identity call to return an old Node; basically if Identity can find
// another Node have the Ideal call make no change and return NULL.
// Example: AddINode::Ideal must check for add of zero; in this case it
// returns NULL instead of doing any graph reshaping.
//
// You cannot modify any old Nodes except for the 'this' pointer.  Due to
// sharing there may be other users of the old Nodes relying on their current
// semantics.  Modifying them will break the other users.
// Example: when reshape "(X+3)+4" into "X+7" you must leave the Node for
// "X+3" unchanged in case it is shared.
//
// If you modify the 'this' pointer's inputs, you should use
// 'set_req'.  If you are making a new Node (either as the new root or
// some new internal piece) you may use 'init_req' to set the initial
// value.  You can make a new Node with either 'new' or 'clone'.  In
// either case, def-use info is correctly maintained.
//
// Example: reshape "(X+3)+4" into "X+7":
//    set_req(1, in(1)->in(1));
//    set_req(2, phase->intcon(7));
//    return this;
// Example: reshape "X*4" into "X<<2"
//    return new LShiftINode(in(1), phase->intcon(2));
//
// You must call 'phase->transform(X)' on any new Nodes X you make, except
// for the returned root node.  Example: reshape "X*31" with "(X<<5)-X".
//    Node *shift=phase->transform(new LShiftINode(in(1),phase->intcon(5)));
//    return new AddINode(shift, in(1));
//
// When making a Node for a constant use 'phase->makecon' or 'phase->intcon'.
// These forms are faster than 'phase->transform(new ConNode())' and Do
// The Right Thing with def-use info.
//
// You cannot bury the 'this' Node inside of a graph reshape.  If the reshaped
// graph uses the 'this' Node it must be the root.  If you want a Node with
// the same Opcode as the 'this' pointer use 'clone'.
//
Node *Node::Ideal(PhaseGVN *phase, bool can_reshape) {
  return NULL;                  // Default to being Ideal already
}

// Some nodes have specific Ideal subgraph transformations only if they are
// unique users of specific nodes. Such nodes should be put on IGVN worklist
// for the transformations to happen.
bool Node::has_special_unique_user() const {
  assert(outcnt() == 1, "match only for unique out");
  Node* n = unique_out();
  int op  = Opcode();
  if (this->is_Store()) {
    // Condition for back-to-back stores folding.
    return n->Opcode() == op && n->in(MemNode::Memory) == this;
  } else if (this->is_Load() || this->is_DecodeN() || this->is_Phi()) {
    // Condition for removing an unused LoadNode or DecodeNNode from the MemBarAcquire precedence input
    return n->Opcode() == Op_MemBarAcquire;
  } else if (op == Op_AddL) {
    // Condition for convL2I(addL(x,y)) ==> addI(convL2I(x),convL2I(y))
    return n->Opcode() == Op_ConvL2I && n->in(1) == this;
  } else if (op == Op_SubI || op == Op_SubL) {
    // Condition for subI(x,subI(y,z)) ==> subI(addI(x,z),y)
    return n->Opcode() == op && n->in(2) == this;
  } else if (is_If() && (n->is_IfFalse() || n->is_IfTrue())) {
    // See IfProjNode::Identity()
    return true;
  } else {
    return false;
  }
};

//--------------------------find_exact_control---------------------------------
// Skip Proj and CatchProj nodes chains. Check for Null and Top.
Node* Node::find_exact_control(Node* ctrl) {
  if (ctrl == NULL && this->is_Region())
    ctrl = this->as_Region()->is_copy();

  if (ctrl != NULL && ctrl->is_CatchProj()) {
    if (ctrl->as_CatchProj()->_con == CatchProjNode::fall_through_index)
      ctrl = ctrl->in(0);
    if (ctrl != NULL && !ctrl->is_top())
      ctrl = ctrl->in(0);
  }

  if (ctrl != NULL && ctrl->is_Proj())
    ctrl = ctrl->in(0);

  return ctrl;
}

//--------------------------dominates------------------------------------------
// Helper function for MemNode::all_controls_dominate().
// Check if 'this' control node dominates or equal to 'sub' control node.
// We already know that if any path back to Root or Start reaches 'this',
// then all paths so, so this is a simple search for one example,
// not an exhaustive search for a counterexample.
bool Node::dominates(Node* sub, Node_List &nlist) {
  assert(this->is_CFG(), "expecting control");
  assert(sub != NULL && sub->is_CFG(), "expecting control");

  // detect dead cycle without regions
  int iterations_without_region_limit = DominatorSearchLimit;

  Node* orig_sub = sub;
  Node* dom      = this;
  bool  met_dom  = false;
  nlist.clear();

  // Walk 'sub' backward up the chain to 'dom', watching for regions.
  // After seeing 'dom', continue up to Root or Start.
  // If we hit a region (backward split point), it may be a loop head.
  // Keep going through one of the region's inputs.  If we reach the
  // same region again, go through a different input.  Eventually we
  // will either exit through the loop head, or give up.
  // (If we get confused, break out and return a conservative 'false'.)
  while (sub != NULL) {
    if (sub->is_top())  break; // Conservative answer for dead code.
    if (sub == dom) {
      if (nlist.size() == 0) {
        // No Region nodes except loops were visited before and the EntryControl
        // path was taken for loops: it did not walk in a cycle.
        return true;
      } else if (met_dom) {
        break;          // already met before: walk in a cycle
      } else {
        // Region nodes were visited. Continue walk up to Start or Root
        // to make sure that it did not walk in a cycle.
        met_dom = true; // first time meet
        iterations_without_region_limit = DominatorSearchLimit; // Reset
     }
    }
    if (sub->is_Start() || sub->is_Root()) {
      // Success if we met 'dom' along a path to Start or Root.
      // We assume there are no alternative paths that avoid 'dom'.
      // (This assumption is up to the caller to ensure!)
      return met_dom;
    }
    Node* up = sub->in(0);
    // Normalize simple pass-through regions and projections:
    up = sub->find_exact_control(up);
    // If sub == up, we found a self-loop.  Try to push past it.
    if (sub == up && sub->is_Loop()) {
      // Take loop entry path on the way up to 'dom'.
      up = sub->in(1); // in(LoopNode::EntryControl);
    } else if (sub == up && sub->is_Region() && sub->req() == 2) {
      // Take in(1) path on the way up to 'dom' for regions with only one input
      up = sub->in(1);
    } else if (sub == up && sub->is_Region() && sub->req() == 3) {
      // Try both paths for Regions with 2 input paths (it may be a loop head).
      // It could give conservative 'false' answer without information
      // which region's input is the entry path.
      iterations_without_region_limit = DominatorSearchLimit; // Reset

      bool region_was_visited_before = false;
      // Was this Region node visited before?
      // If so, we have reached it because we accidentally took a
      // loop-back edge from 'sub' back into the body of the loop,
      // and worked our way up again to the loop header 'sub'.
      // So, take the first unexplored path on the way up to 'dom'.
      for (int j = nlist.size() - 1; j >= 0; j--) {
        intptr_t ni = (intptr_t)nlist.at(j);
        Node* visited = (Node*)(ni & ~1);
        bool  visited_twice_already = ((ni & 1) != 0);
        if (visited == sub) {
          if (visited_twice_already) {
            // Visited 2 paths, but still stuck in loop body.  Give up.
            return false;
          }
          // The Region node was visited before only once.
          // (We will repush with the low bit set, below.)
          nlist.remove(j);
          // We will find a new edge and re-insert.
          region_was_visited_before = true;
          break;
        }
      }

      // Find an incoming edge which has not been seen yet; walk through it.
      assert(up == sub, "");
      uint skip = region_was_visited_before ? 1 : 0;
      for (uint i = 1; i < sub->req(); i++) {
        Node* in = sub->in(i);
        if (in != NULL && !in->is_top() && in != sub) {
          if (skip == 0) {
            up = in;
            break;
          }
          --skip;               // skip this nontrivial input
        }
      }

      // Set 0 bit to indicate that both paths were taken.
      nlist.push((Node*)((intptr_t)sub + (region_was_visited_before ? 1 : 0)));
    }

    if (up == sub) {
      break;    // some kind of tight cycle
    }
    if (up == orig_sub && met_dom) {
      // returned back after visiting 'dom'
      break;    // some kind of cycle
    }
    if (--iterations_without_region_limit < 0) {
      break;    // dead cycle
    }
    sub = up;
  }

  // Did not meet Root or Start node in pred. chain.
  // Conservative answer for dead code.
  return false;
}

//------------------------------remove_dead_region-----------------------------
// This control node is dead.  Follow the subgraph below it making everything
// using it dead as well.  This will happen normally via the usual IterGVN
// worklist but this call is more efficient.  Do not update use-def info
// inside the dead region, just at the borders.
static void kill_dead_code( Node *dead, PhaseIterGVN *igvn ) {
  // Con's are a popular node to re-hit in the hash table again.
  if( dead->is_Con() ) return;

  ResourceMark rm;
  Node_List nstack;

  Node *top = igvn->C->top();
  nstack.push(dead);
  bool has_irreducible_loop = igvn->C->has_irreducible_loop();

  while (nstack.size() > 0) {
    dead = nstack.pop();
    if (dead->Opcode() == Op_SafePoint) {
      dead->as_SafePoint()->disconnect_from_root(igvn);
    }
    if (dead->outcnt() > 0) {
      // Keep dead node on stack until all uses are processed.
      nstack.push(dead);
      // For all Users of the Dead...    ;-)
      for (DUIterator_Last kmin, k = dead->last_outs(kmin); k >= kmin; ) {
        Node* use = dead->last_out(k);
        igvn->hash_delete(use);       // Yank from hash table prior to mod
        if (use->in(0) == dead) {     // Found another dead node
          assert (!use->is_Con(), "Control for Con node should be Root node.");
          use->set_req(0, top);       // Cut dead edge to prevent processing
          nstack.push(use);           // the dead node again.
        } else if (!has_irreducible_loop && // Backedge could be alive in irreducible loop
                   use->is_Loop() && !use->is_Root() &&       // Don't kill Root (RootNode extends LoopNode)
                   use->in(LoopNode::EntryControl) == dead) { // Dead loop if its entry is dead
          use->set_req(LoopNode::EntryControl, top);          // Cut dead edge to prevent processing
          use->set_req(0, top);       // Cut self edge
          nstack.push(use);
        } else {                      // Else found a not-dead user
          // Dead if all inputs are top or null
          bool dead_use = !use->is_Root(); // Keep empty graph alive
          for (uint j = 1; j < use->req(); j++) {
            Node* in = use->in(j);
            if (in == dead) {         // Turn all dead inputs into TOP
              use->set_req(j, top);
            } else if (in != NULL && !in->is_top()) {
              dead_use = false;
            }
          }
          if (dead_use) {
            if (use->is_Region()) {
              use->set_req(0, top);   // Cut self edge
            }
            nstack.push(use);
          } else {
            igvn->_worklist.push(use);
          }
        }
        // Refresh the iterator, since any number of kills might have happened.
        k = dead->last_outs(kmin);
      }
    } else { // (dead->outcnt() == 0)
      // Done with outputs.
      igvn->hash_delete(dead);
      igvn->_worklist.remove(dead);
      igvn->set_type(dead, Type::TOP);
      // Kill all inputs to the dead guy
      for (uint i=0; i < dead->req(); i++) {
        Node *n = dead->in(i);      // Get input to dead guy
        if (n != NULL && !n->is_top()) { // Input is valid?
          dead->set_req(i, top);    // Smash input away
          if (n->outcnt() == 0) {   // Input also goes dead?
            if (!n->is_Con())
              nstack.push(n);       // Clear it out as well
          } else if (n->outcnt() == 1 &&
                     n->has_special_unique_user()) {
            igvn->add_users_to_worklist( n );
          } else if (n->outcnt() <= 2 && n->is_Store()) {
            // Push store's uses on worklist to enable folding optimization for
            // store/store and store/load to the same address.
            // The restriction (outcnt() <= 2) is the same as in set_req_X()
            // and remove_globally_dead_node().
            igvn->add_users_to_worklist( n );
          } else {
            BarrierSet::barrier_set()->barrier_set_c2()->enqueue_useful_gc_barrier(igvn, n);
          }
        }
      }
      igvn->C->remove_useless_node(dead);
    } // (dead->outcnt() == 0)
  }   // while (nstack.size() > 0) for outputs
  return;
}

//------------------------------remove_dead_region-----------------------------
bool Node::remove_dead_region(PhaseGVN *phase, bool can_reshape) {
  Node *n = in(0);
  if( !n ) return false;
  // Lost control into this guy?  I.e., it became unreachable?
  // Aggressively kill all unreachable code.
  if (can_reshape && n->is_top()) {
    kill_dead_code(this, phase->is_IterGVN());
    return false; // Node is dead.
  }

  if( n->is_Region() && n->as_Region()->is_copy() ) {
    Node *m = n->nonnull_req();
    set_req(0, m);
    return true;
  }
  return false;
}

//------------------------------hash-------------------------------------------
// Hash function over Nodes.
uint Node::hash() const {
  uint sum = 0;
  for( uint i=0; i<_cnt; i++ )  // Add in all inputs
    sum = (sum<<1)-(uintptr_t)in(i);        // Ignore embedded NULLs
  return (sum>>2) + _cnt + Opcode();
}

//------------------------------cmp--------------------------------------------
// Compare special parts of simple Nodes
bool Node::cmp( const Node &n ) const {
  return true;                  // Must be same
}

//------------------------------rematerialize-----------------------------------
// Should we clone rather than spill this instruction?
bool Node::rematerialize() const {
  if ( is_Mach() )
    return this->as_Mach()->rematerialize();
  else
    return (_flags & Flag_rematerialize) != 0;
}

//------------------------------needs_anti_dependence_check---------------------
// Nodes which use memory without consuming it, hence need antidependences.
bool Node::needs_anti_dependence_check() const {
  if (req() < 2 || (_flags & Flag_needs_anti_dependence_check) == 0) {
    return false;
  }
  return in(1)->bottom_type()->has_memory();
}

// Get an integer constant from a ConNode (or CastIINode).
// Return a default value if there is no apparent constant here.
const TypeInt* Node::find_int_type() const {
  if (this->is_Type()) {
    return this->as_Type()->type()->isa_int();
  } else if (this->is_Con()) {
    assert(is_Mach(), "should be ConNode(TypeNode) or else a MachNode");
    return this->bottom_type()->isa_int();
  }
  return NULL;
}

const TypeInteger* Node::find_integer_type(BasicType bt) const {
  if (this->is_Type()) {
    return this->as_Type()->type()->isa_integer(bt);
  } else if (this->is_Con()) {
    assert(is_Mach(), "should be ConNode(TypeNode) or else a MachNode");
    return this->bottom_type()->isa_integer(bt);
  }
  return NULL;
}

// Get a pointer constant from a ConstNode.
// Returns the constant if it is a pointer ConstNode
intptr_t Node::get_ptr() const {
  assert( Opcode() == Op_ConP, "" );
  return ((ConPNode*)this)->type()->is_ptr()->get_con();
}

// Get a narrow oop constant from a ConNNode.
intptr_t Node::get_narrowcon() const {
  assert( Opcode() == Op_ConN, "" );
  return ((ConNNode*)this)->type()->is_narrowoop()->get_con();
}

// Get a long constant from a ConNode.
// Return a default value if there is no apparent constant here.
const TypeLong* Node::find_long_type() const {
  if (this->is_Type()) {
    return this->as_Type()->type()->isa_long();
  } else if (this->is_Con()) {
    assert(is_Mach(), "should be ConNode(TypeNode) or else a MachNode");
    return this->bottom_type()->isa_long();
  }
  return NULL;
}


/**
 * Return a ptr type for nodes which should have it.
 */
const TypePtr* Node::get_ptr_type() const {
  const TypePtr* tp = this->bottom_type()->make_ptr();
#ifdef ASSERT
  if (tp == NULL) {
    this->dump(1);
    assert((tp != NULL), "unexpected node type");
  }
#endif
  return tp;
}

// Get a double constant from a ConstNode.
// Returns the constant if it is a double ConstNode
jdouble Node::getd() const {
  assert( Opcode() == Op_ConD, "" );
  return ((ConDNode*)this)->type()->is_double_constant()->getd();
}

// Get a float constant from a ConstNode.
// Returns the constant if it is a float ConstNode
jfloat Node::getf() const {
  assert( Opcode() == Op_ConF, "" );
  return ((ConFNode*)this)->type()->is_float_constant()->getf();
}

#ifndef PRODUCT

// Call this from debugger:
Node* find_node(Node* n, const int idx) {
  return n->find(idx);
}

// Call this from debugger with root node as default:
Node* find_node(const int idx) {
  return Compile::current()->root()->find(idx);
}

// Call this from debugger:
Node* find_ctrl(Node* n, const int idx) {
  return n->find_ctrl(idx);
}

// Call this from debugger with root node as default:
Node* find_ctrl(const int idx) {
  return Compile::current()->root()->find_ctrl(idx);
}

//------------------------------find_ctrl--------------------------------------
// Find an ancestor to this node in the control history with given _idx
Node* Node::find_ctrl(int idx) {
  return find(idx, true);
}

//------------------------------find-------------------------------------------
// Tries to find the node with the index |idx| starting from this node. If idx is negative,
// the search also includes forward (out) edges. Returns NULL if not found.
// If only_ctrl is set, the search will only be done on control nodes. Returns NULL if
// not found or if the node to be found is not a control node (search will not find it).
Node* Node::find(const int idx, bool only_ctrl) {
  ResourceMark rm;
  VectorSet old_space;
  VectorSet new_space;
  Node_List worklist;
  Arena* old_arena = Compile::current()->old_arena();
  add_to_worklist(this, &worklist, old_arena, &old_space, &new_space);
  Node* result = NULL;
  int node_idx = (idx >= 0) ? idx : -idx;

  for (uint list_index = 0; list_index < worklist.size(); list_index++) {
    Node* n = worklist[list_index];

    if ((int)n->_idx == node_idx debug_only(|| n->debug_idx() == node_idx)) {
      if (result != NULL) {
        tty->print("find: " INTPTR_FORMAT " and " INTPTR_FORMAT " both have idx==%d\n",
                  (uintptr_t)result, (uintptr_t)n, node_idx);
      }
      result = n;
    }

    for (uint i = 0; i < n->len(); i++) {
      if (!only_ctrl || n->is_Region() || (n->Opcode() == Op_Root) || (i == TypeFunc::Control)) {
        // If only_ctrl is set: Add regions, the root node, or control inputs only
        add_to_worklist(n->in(i), &worklist, old_arena, &old_space, &new_space);
      }
    }

    // Also search along forward edges if idx is negative and the search is not done on control nodes only
    if (idx < 0 && !only_ctrl) {
      for (uint i = 0; i < n->outcnt(); i++) {
        add_to_worklist(n->raw_out(i), &worklist, old_arena, &old_space, &new_space);
      }
    }
#ifdef ASSERT
    // Search along debug_orig edges last
    Node* orig = n->debug_orig();
    while (orig != NULL && add_to_worklist(orig, &worklist, old_arena, &old_space, &new_space)) {
      orig = orig->debug_orig();
    }
#endif // ASSERT
  }
  return result;
}

bool Node::add_to_worklist(Node* n, Node_List* worklist, Arena* old_arena, VectorSet* old_space, VectorSet* new_space) {
  if (not_a_node(n)) {
    return false; // Gracefully handle NULL, -1, 0xabababab, etc.
  }

  // Contained in new_space or old_space? Check old_arena first since it's mostly empty.
  VectorSet* v = old_arena->contains(n) ? old_space : new_space;
  if (!v->test_set(n->_idx)) {
    worklist->push(n);
    return true;
  }
  return false;
}

// -----------------------------Name-------------------------------------------
extern const char *NodeClassNames[];
const char *Node::Name() const { return NodeClassNames[Opcode()]; }

static bool is_disconnected(const Node* n) {
  for (uint i = 0; i < n->req(); i++) {
    if (n->in(i) != NULL)  return false;
  }
  return true;
}

#ifdef ASSERT
void Node::dump_orig(outputStream *st, bool print_key) const {
  Compile* C = Compile::current();
  Node* orig = _debug_orig;
  if (not_a_node(orig)) orig = NULL;
  if (orig != NULL && !C->node_arena()->contains(orig)) orig = NULL;
  if (orig == NULL) return;
  if (print_key) {
    st->print(" !orig=");
  }
  Node* fast = orig->debug_orig(); // tortoise & hare algorithm to detect loops
  if (not_a_node(fast)) fast = NULL;
  while (orig != NULL) {
    bool discon = is_disconnected(orig);  // if discon, print [123] else 123
    if (discon) st->print("[");
    if (!Compile::current()->node_arena()->contains(orig))
      st->print("o");
    st->print("%d", orig->_idx);
    if (discon) st->print("]");
    orig = orig->debug_orig();
    if (not_a_node(orig)) orig = NULL;
    if (orig != NULL && !C->node_arena()->contains(orig)) orig = NULL;
    if (orig != NULL) st->print(",");
    if (fast != NULL) {
      // Step fast twice for each single step of orig:
      fast = fast->debug_orig();
      if (not_a_node(fast)) fast = NULL;
      if (fast != NULL && fast != orig) {
        fast = fast->debug_orig();
        if (not_a_node(fast)) fast = NULL;
      }
      if (fast == orig) {
        st->print("...");
        break;
      }
    }
  }
}

void Node::set_debug_orig(Node* orig) {
  _debug_orig = orig;
  if (BreakAtNode == 0)  return;
  if (not_a_node(orig))  orig = NULL;
  int trip = 10;
  while (orig != NULL) {
    if (orig->debug_idx() == BreakAtNode || (int)orig->_idx == BreakAtNode) {
      tty->print_cr("BreakAtNode: _idx=%d _debug_idx=%d orig._idx=%d orig._debug_idx=%d",
                    this->_idx, this->debug_idx(), orig->_idx, orig->debug_idx());
      BREAKPOINT;
    }
    orig = orig->debug_orig();
    if (not_a_node(orig))  orig = NULL;
    if (trip-- <= 0)  break;
  }
}
#endif //ASSERT

//------------------------------dump------------------------------------------
// Dump a Node
void Node::dump(const char* suffix, bool mark, outputStream *st) const {
  Compile* C = Compile::current();
  bool is_new = C->node_arena()->contains(this);
  C->_in_dump_cnt++;

  if (_indent > 0) {
    st->print("%*s", (_indent << 1), "  ");
  }

  st->print("%c%d%s%s  === ", is_new ? ' ' : 'o', _idx, mark ? " >" : "  ", Name());

  // Dump the required and precedence inputs
  dump_req(st);
  dump_prec(st);
  // Dump the outputs
  dump_out(st);

  if (is_disconnected(this)) {
#ifdef ASSERT
    st->print("  [%d]",debug_idx());
    dump_orig(st);
#endif
    st->cr();
    C->_in_dump_cnt--;
    return;                     // don't process dead nodes
  }

  if (C->clone_map().value(_idx) != 0) {
    C->clone_map().dump(_idx);
  }
  // Dump node-specific info
  dump_spec(st);
#ifdef ASSERT
  // Dump the non-reset _debug_idx
  if (Verbose && WizardMode) {
    st->print("  [%d]",debug_idx());
  }
#endif

  const Type *t = bottom_type();

  if (t != NULL && (t->isa_instptr() || t->isa_klassptr())) {
    const TypeInstPtr  *toop = t->isa_instptr();
    const TypeKlassPtr *tkls = t->isa_klassptr();
    ciKlass*           klass = toop ? toop->klass() : (tkls ? tkls->klass() : NULL );
    if (klass && klass->is_loaded() && klass->is_interface()) {
      st->print("  Interface:");
    } else if (toop) {
      st->print("  Oop:");
    } else if (tkls) {
      st->print("  Klass:");
    }
    t->dump_on(st);
  } else if (t == Type::MEMORY) {
    st->print("  Memory:");
    MemNode::dump_adr_type(this, adr_type(), st);
  } else if (Verbose || WizardMode) {
    st->print("  Type:");
    if (t) {
      t->dump_on(st);
    } else {
      st->print("no type");
    }
  } else if (t->isa_vect() && this->is_MachSpillCopy()) {
    // Dump MachSpillcopy vector type.
    t->dump_on(st);
  }
  if (is_new) {
    DEBUG_ONLY(dump_orig(st));
    Node_Notes* nn = C->node_notes_at(_idx);
    if (nn != NULL && !nn->is_clear()) {
      if (nn->jvms() != NULL) {
        st->print(" !jvms:");
        nn->jvms()->dump_spec(st);
      }
    }
  }
  if (suffix) st->print("%s", suffix);
  C->_in_dump_cnt--;
}

//------------------------------dump_req--------------------------------------
void Node::dump_req(outputStream *st) const {
  // Dump the required input edges
  for (uint i = 0; i < req(); i++) {    // For all required inputs
    Node* d = in(i);
    if (d == NULL) {
      st->print("_ ");
    } else if (not_a_node(d)) {
      st->print("not_a_node ");  // uninitialized, sentinel, garbage, etc.
    } else {
      st->print("%c%d ", Compile::current()->node_arena()->contains(d) ? ' ' : 'o', d->_idx);
    }
  }
}


//------------------------------dump_prec-------------------------------------
void Node::dump_prec(outputStream *st) const {
  // Dump the precedence edges
  int any_prec = 0;
  for (uint i = req(); i < len(); i++) {       // For all precedence inputs
    Node* p = in(i);
    if (p != NULL) {
      if (!any_prec++) st->print(" |");
      if (not_a_node(p)) { st->print("not_a_node "); continue; }
      st->print("%c%d ", Compile::current()->node_arena()->contains(in(i)) ? ' ' : 'o', in(i)->_idx);
    }
  }
}

//------------------------------dump_out--------------------------------------
void Node::dump_out(outputStream *st) const {
  // Delimit the output edges
  st->print(" [[");
  // Dump the output edges
  for (uint i = 0; i < _outcnt; i++) {    // For all outputs
    Node* u = _out[i];
    if (u == NULL) {
      st->print("_ ");
    } else if (not_a_node(u)) {
      st->print("not_a_node ");
    } else {
      st->print("%c%d ", Compile::current()->node_arena()->contains(u) ? ' ' : 'o', u->_idx);
    }
  }
  st->print("]] ");
}

//----------------------------collect_nodes_i----------------------------------
// Collects nodes from an Ideal graph, starting from a given start node and
// moving in a given direction until a certain depth (distance from the start
// node) is reached. Duplicates are ignored.
// Arguments:
//   queue:         the nodes are collected into this array.
//   start:         the node at which to start collecting.
//   direction:     if this is a positive number, collect input nodes; if it is
//                  a negative number, collect output nodes.
//   depth:         collect nodes up to this distance from the start node.
//   include_start: whether to include the start node in the result collection.
//   only_ctrl:     whether to regard control edges only during traversal.
//   only_data:     whether to regard data edges only during traversal.
static void collect_nodes_i(GrowableArray<Node*>* queue, const Node* start, int direction, uint depth, bool include_start, bool only_ctrl, bool only_data) {
  bool indent = depth <= PrintIdealIndentThreshold;
  Node* s = (Node*) start; // remove const
  queue->append(s);
  int begin = 0;
  int end = 0;

  s->set_indent(0);
  for(uint i = 0; i < depth; i++) {
    end = queue->length();
    for(int j = begin; j < end; j++) {
      Node* tp  = queue->at(j);
      uint limit = direction > 0 ? tp->len() : tp->outcnt();
      for(uint k = 0; k < limit; k++) {
        Node* n = direction > 0 ? tp->in(k) : tp->raw_out(k);

        if (not_a_node(n))  continue;
        // do not recurse through top or the root (would reach unrelated stuff)
        if (n->is_Root() || n->is_top()) continue;
        if (only_ctrl && !n->is_CFG()) continue;
        if (only_data && n->is_CFG()) continue;
        bool in_queue = queue->contains(n);
        if (!in_queue) {
          queue->append(n);
          n->set_indent(indent ? (i + 1) : 0);
        }
      }
    }
    begin = end;
  }
  if (!include_start) {
    queue->remove(s);
  }
}

//------------------------------dump_nodes-------------------------------------
static void dump_nodes(const Node* start, int d, bool only_ctrl) {
  if (not_a_node(start)) return;

  GrowableArray <Node *> queue(Compile::current()->live_nodes());
  collect_nodes_i(&queue, start, d, (uint) ABS(d), true, only_ctrl, false);

  int end = queue.length();
  if (d > 0) {
    for(int j = end-1; j >= 0; j--) {
      queue.at(j)->dump();
    }
  } else {
    for(int j = 0; j < end; j++) {
      queue.at(j)->dump();
    }
  }
}

//------------------------------dump-------------------------------------------
void Node::dump(int d) const {
  dump_nodes(this, d, false);
}

//------------------------------dump_ctrl--------------------------------------
// Dump a Node's control history to depth
void Node::dump_ctrl(int d) const {
  dump_nodes(this, d, true);
}

//-----------------------------dump_compact------------------------------------
void Node::dump_comp() const {
  this->dump_comp("\n");
}

//-----------------------------dump_compact------------------------------------
// Dump a Node in compact representation, i.e., just print its name and index.
// Nodes can specify additional specifics to print in compact representation by
// implementing dump_compact_spec.
void Node::dump_comp(const char* suffix, outputStream *st) const {
  Compile* C = Compile::current();
  C->_in_dump_cnt++;
  st->print("%s(%d)", Name(), _idx);
  this->dump_compact_spec(st);
  if (suffix) {
    st->print("%s", suffix);
  }
  C->_in_dump_cnt--;
}

//----------------------------dump_related-------------------------------------
// Dump a Node's related nodes - the notion of "related" depends on the Node at
// hand and is determined by the implementation of the virtual method rel.
void Node::dump_related() const {
  Compile* C = Compile::current();
  GrowableArray <Node *> in_rel(C->unique());
  GrowableArray <Node *> out_rel(C->unique());
  this->related(&in_rel, &out_rel, false);
  for (int i = in_rel.length() - 1; i >= 0; i--) {
    in_rel.at(i)->dump();
  }
  this->dump("\n", true);
  for (int i = 0; i < out_rel.length(); i++) {
    out_rel.at(i)->dump();
  }
}

//----------------------------dump_related-------------------------------------
// Dump a Node's related nodes up to a given depth (distance from the start
// node).
// Arguments:
//   d_in:  depth for input nodes.
//   d_out: depth for output nodes (note: this also is a positive number).
void Node::dump_related(uint d_in, uint d_out) const {
  Compile* C = Compile::current();
  GrowableArray <Node *> in_rel(C->unique());
  GrowableArray <Node *> out_rel(C->unique());

  // call collect_nodes_i directly
  collect_nodes_i(&in_rel, this, 1, d_in, false, false, false);
  collect_nodes_i(&out_rel, this, -1, d_out, false, false, false);

  for (int i = in_rel.length() - 1; i >= 0; i--) {
    in_rel.at(i)->dump();
  }
  this->dump("\n", true);
  for (int i = 0; i < out_rel.length(); i++) {
    out_rel.at(i)->dump();
  }
}

//------------------------dump_related_compact---------------------------------
// Dump a Node's related nodes in compact representation. The notion of
// "related" depends on the Node at hand and is determined by the implementation
// of the virtual method rel.
void Node::dump_related_compact() const {
  Compile* C = Compile::current();
  GrowableArray <Node *> in_rel(C->unique());
  GrowableArray <Node *> out_rel(C->unique());
  this->related(&in_rel, &out_rel, true);
  int n_in = in_rel.length();
  int n_out = out_rel.length();

  this->dump_comp(n_in == 0 ? "\n" : "  ");
  for (int i = 0; i < n_in; i++) {
    in_rel.at(i)->dump_comp(i == n_in - 1 ? "\n" : "  ");
  }
  for (int i = 0; i < n_out; i++) {
    out_rel.at(i)->dump_comp(i == n_out - 1 ? "\n" : "  ");
  }
}

//------------------------------related----------------------------------------
// Collect a Node's related nodes. The default behaviour just collects the
// inputs and outputs at depth 1, including both control and data flow edges,
// regardless of whether the presentation is compact or not. For data nodes,
// the default is to collect all data inputs (till level 1 if compact), and
// outputs till level 1.
void Node::related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const {
  if (this->is_CFG()) {
    collect_nodes_i(in_rel, this, 1, 1, false, false, false);
    collect_nodes_i(out_rel, this, -1, 1, false, false, false);
  } else {
    if (compact) {
      this->collect_nodes(in_rel, 1, false, true);
    } else {
      this->collect_nodes_in_all_data(in_rel, false);
    }
    this->collect_nodes(out_rel, -1, false, false);
  }
}

//---------------------------collect_nodes-------------------------------------
// An entry point to the low-level node collection facility, to start from a
// given node in the graph. The start node is by default not included in the
// result.
// Arguments:
//   ns:   collect the nodes into this data structure.
//   d:    the depth (distance from start node) to which nodes should be
//         collected. A value >0 indicates input nodes, a value <0, output
//         nodes.
//   ctrl: include only control nodes.
//   data: include only data nodes.
void Node::collect_nodes(GrowableArray<Node*> *ns, int d, bool ctrl, bool data) const {
  if (ctrl && data) {
    // ignore nonsensical combination
    return;
  }
  collect_nodes_i(ns, this, d, (uint) ABS(d), false, ctrl, data);
}

//--------------------------collect_nodes_in-----------------------------------
static void collect_nodes_in(Node* start, GrowableArray<Node*> *ns, bool primary_is_data, bool collect_secondary) {
  // The maximum depth is determined using a BFS that visits all primary (data
  // or control) inputs and increments the depth at each level.
  uint d_in = 0;
  GrowableArray<Node*> nodes(Compile::current()->unique());
  nodes.push(start);
  int nodes_at_current_level = 1;
  int n_idx = 0;
  while (nodes_at_current_level > 0) {
    // Add all primary inputs reachable from the current level to the list, and
    // increase the depth if there were any.
    int nodes_at_next_level = 0;
    bool nodes_added = false;
    while (nodes_at_current_level > 0) {
      nodes_at_current_level--;
      Node* current = nodes.at(n_idx++);
      for (uint i = 0; i < current->len(); i++) {
        Node* n = current->in(i);
        if (not_a_node(n)) {
          continue;
        }
        if ((primary_is_data && n->is_CFG()) || (!primary_is_data && !n->is_CFG())) {
          continue;
        }
        if (!nodes.contains(n)) {
          nodes.push(n);
          nodes_added = true;
          nodes_at_next_level++;
        }
      }
    }
    if (nodes_added) {
      d_in++;
    }
    nodes_at_current_level = nodes_at_next_level;
  }
  start->collect_nodes(ns, d_in, !primary_is_data, primary_is_data);
  if (collect_secondary) {
    // Now, iterate over the secondary nodes in ns and add the respective
    // boundary reachable from them.
    GrowableArray<Node*> sns(Compile::current()->unique());
    for (GrowableArrayIterator<Node*> it = ns->begin(); it != ns->end(); ++it) {
      Node* n = *it;
      n->collect_nodes(&sns, 1, primary_is_data, !primary_is_data);
      for (GrowableArrayIterator<Node*> d = sns.begin(); d != sns.end(); ++d) {
        ns->append_if_missing(*d);
      }
      sns.clear();
    }
  }
}

//---------------------collect_nodes_in_all_data-------------------------------
// Collect the entire data input graph. Include the control boundary if
// requested.
// Arguments:
//   ns:   collect the nodes into this data structure.
//   ctrl: if true, include the control boundary.
void Node::collect_nodes_in_all_data(GrowableArray<Node*> *ns, bool ctrl) const {
  collect_nodes_in((Node*) this, ns, true, ctrl);
}

//--------------------------collect_nodes_in_all_ctrl--------------------------
// Collect the entire control input graph. Include the data boundary if
// requested.
//   ns:   collect the nodes into this data structure.
//   data: if true, include the control boundary.
void Node::collect_nodes_in_all_ctrl(GrowableArray<Node*> *ns, bool data) const {
  collect_nodes_in((Node*) this, ns, false, data);
}

//------------------collect_nodes_out_all_ctrl_boundary------------------------
// Collect the entire output graph until hitting control node boundaries, and
// include those.
void Node::collect_nodes_out_all_ctrl_boundary(GrowableArray<Node*> *ns) const {
  // Perform a BFS and stop at control nodes.
  GrowableArray<Node*> nodes(Compile::current()->unique());
  nodes.push((Node*) this);
  while (nodes.length() > 0) {
    Node* current = nodes.pop();
    if (not_a_node(current)) {
      continue;
    }
    ns->append_if_missing(current);
    if (!current->is_CFG()) {
      for (DUIterator i = current->outs(); current->has_out(i); i++) {
        nodes.push(current->out(i));
      }
    }
  }
  ns->remove((Node*) this);
}

// VERIFICATION CODE
// For each input edge to a node (ie - for each Use-Def edge), verify that
// there is a corresponding Def-Use edge.
//------------------------------verify_edges-----------------------------------
void Node::verify_edges(Unique_Node_List &visited) {
  uint i, j, idx;
  int  cnt;
  Node *n;

  // Recursive termination test
  if (visited.member(this))  return;
  visited.push(this);

  // Walk over all input edges, checking for correspondence
  for( i = 0; i < len(); i++ ) {
    n = in(i);
    if (n != NULL && !n->is_top()) {
      // Count instances of (Node *)this
      cnt = 0;
      for (idx = 0; idx < n->_outcnt; idx++ ) {
        if (n->_out[idx] == (Node *)this)  cnt++;
      }
      assert( cnt > 0,"Failed to find Def-Use edge." );
      // Check for duplicate edges
      // walk the input array downcounting the input edges to n
      for( j = 0; j < len(); j++ ) {
        if( in(j) == n ) cnt--;
      }
      assert( cnt == 0,"Mismatched edge count.");
    } else if (n == NULL) {
      assert(i >= req() || i == 0 || is_Region() || is_Phi() || is_ArrayCopy() || (is_Unlock() && i == req()-1)
              || (is_MemBar() && i == 5), // the precedence edge to a membar can be removed during macro node expansion
              "only region, phi, arraycopy, unlock or membar nodes have null data edges");
    } else {
      assert(n->is_top(), "sanity");
      // Nothing to check.
    }
  }
  // Recursive walk over all input edges
  for( i = 0; i < len(); i++ ) {
    n = in(i);
    if( n != NULL )
      in(i)->verify_edges(visited);
  }
}

// Verify all nodes if verify_depth is negative
void Node::verify(int verify_depth, VectorSet& visited, Node_List& worklist) {
  assert(verify_depth != 0, "depth should not be 0");
  Compile* C = Compile::current();
  uint last_index_on_current_depth = worklist.size() - 1;
  verify_depth--; // Visiting the first node on depth 1
  // Only add nodes to worklist if verify_depth is negative (visit all nodes) or greater than 0
  bool add_to_worklist = verify_depth != 0;

  for (uint list_index = 0; list_index < worklist.size(); list_index++) {
    Node* n = worklist[list_index];

    if (n->is_Con() && n->bottom_type() == Type::TOP) {
      if (C->cached_top_node() == NULL) {
        C->set_cached_top_node((Node*)n);
      }
      assert(C->cached_top_node() == n, "TOP node must be unique");
    }

    uint in_len = n->len();
    for (uint i = 0; i < in_len; i++) {
      Node* x = n->_in[i];
      if (!x || x->is_top()) {
        continue;
      }

      // Verify my input has a def-use edge to me
      // Count use-def edges from n to x
      int cnt = 1;
      for (uint j = 0; j < i; j++) {
        if (n->_in[j] == x) {
          cnt++;
          break;
        }
      }
      if (cnt == 2) {
        // x is already checked as n's previous input, skip its duplicated def-use count checking
        continue;
      }
      for (uint j = i + 1; j < in_len; j++) {
        if (n->_in[j] == x) {
          cnt++;
        }
      }

      // Count def-use edges from x to n
      uint max = x->_outcnt;
      for (uint k = 0; k < max; k++) {
        if (x->_out[k] == n) {
          cnt--;
        }
      }
      assert(cnt == 0, "mismatched def-use edge counts");

      if (add_to_worklist && !visited.test_set(x->_idx)) {
        worklist.push(x);
      }
    }

    if (verify_depth > 0 && list_index == last_index_on_current_depth) {
      // All nodes on this depth were processed and its inputs are on the worklist. Decrement verify_depth and
      // store the current last list index which is the last node in the list with the new depth. All nodes
      // added afterwards will have a new depth again. Stop adding new nodes if depth limit is reached (=0).
      verify_depth--;
      if (verify_depth == 0) {
        add_to_worklist = false;
      }
      last_index_on_current_depth = worklist.size() - 1;
    }
  }
}
#endif // not PRODUCT

//------------------------------Registers--------------------------------------
// Do we Match on this edge index or not?  Generally false for Control
// and true for everything else.  Weird for calls & returns.
uint Node::match_edge(uint idx) const {
  return idx;                   // True for other than index 0 (control)
}

// Register classes are defined for specific machines
const RegMask &Node::out_RegMask() const {
  ShouldNotCallThis();
  return RegMask::Empty;
}

const RegMask &Node::in_RegMask(uint) const {
  ShouldNotCallThis();
  return RegMask::Empty;
}

void Node_Array::grow(uint i) {
  assert(_max > 0, "invariant");
  uint old = _max;
  _max = next_power_of_2(i);
  _nodes = (Node**)_a->Arealloc( _nodes, old*sizeof(Node*),_max*sizeof(Node*));
  Copy::zero_to_bytes( &_nodes[old], (_max-old)*sizeof(Node*) );
}

void Node_Array::insert(uint i, Node* n) {
  if (_nodes[_max - 1]) {
    grow(_max);
  }
  Copy::conjoint_words_to_higher((HeapWord*)&_nodes[i], (HeapWord*)&_nodes[i + 1], ((_max - i - 1) * sizeof(Node*)));
  _nodes[i] = n;
}

void Node_Array::remove(uint i) {
  Copy::conjoint_words_to_lower((HeapWord*)&_nodes[i + 1], (HeapWord*)&_nodes[i], ((_max - i - 1) * sizeof(Node*)));
  _nodes[_max - 1] = NULL;
}

void Node_Array::dump() const {
#ifndef PRODUCT
  for (uint i = 0; i < _max; i++) {
    Node* nn = _nodes[i];
    if (nn != NULL) {
      tty->print("%5d--> ",i); nn->dump();
    }
  }
#endif
}

//--------------------------is_iteratively_computed------------------------------
// Operation appears to be iteratively computed (such as an induction variable)
// It is possible for this operation to return false for a loop-varying
// value, if it appears (by local graph inspection) to be computed by a simple conditional.
bool Node::is_iteratively_computed() {
  if (ideal_reg()) { // does operation have a result register?
    for (uint i = 1; i < req(); i++) {
      Node* n = in(i);
      if (n != NULL && n->is_Phi()) {
        for (uint j = 1; j < n->req(); j++) {
          if (n->in(j) == this) {
            return true;
          }
        }
      }
    }
  }
  return false;
}

//--------------------------find_similar------------------------------
// Return a node with opcode "opc" and same inputs as "this" if one can
// be found; Otherwise return NULL;
Node* Node::find_similar(int opc) {
  if (req() >= 2) {
    Node* def = in(1);
    if (def && def->outcnt() >= 2) {
      for (DUIterator_Fast dmax, i = def->fast_outs(dmax); i < dmax; i++) {
        Node* use = def->fast_out(i);
        if (use != this &&
            use->Opcode() == opc &&
            use->req() == req()) {
          uint j;
          for (j = 0; j < use->req(); j++) {
            if (use->in(j) != in(j)) {
              break;
            }
          }
          if (j == use->req()) {
            return use;
          }
        }
      }
    }
  }
  return NULL;
}


//--------------------------unique_ctrl_out------------------------------
// Return the unique control out if only one. Null if none or more than one.
Node* Node::unique_ctrl_out() const {
  Node* found = NULL;
  for (uint i = 0; i < outcnt(); i++) {
    Node* use = raw_out(i);
    if (use->is_CFG() && use != this) {
      if (found != NULL) {
        return NULL;
      }
      found = use;
    }
  }
  return found;
}

void Node::ensure_control_or_add_prec(Node* c) {
  if (in(0) == NULL) {
    set_req(0, c);
  } else if (in(0) != c) {
    add_prec(c);
  }
}

bool Node::is_dead_loop_safe() const {
  if (is_Phi()) {
    return true;
  }
  if (is_Proj() && in(0) == NULL)  {
    return true;
  }
  if ((_flags & (Flag_is_dead_loop_safe | Flag_is_Con)) != 0) {
    if (!is_Proj()) {
      return true;
    }
    if (in(0)->is_Allocate()) {
      return false;
    }
    // MemNode::can_see_stored_value() peeks through the boxing call
    if (in(0)->is_CallStaticJava() && in(0)->as_CallStaticJava()->is_boxing_method()) {
      return false;
    }
    return true;
  }
  return false;
}

//=============================================================================
//------------------------------yank-------------------------------------------
// Find and remove
void Node_List::yank( Node *n ) {
  uint i;
  for (i = 0; i < _cnt; i++) {
    if (_nodes[i] == n) {
      break;
    }
  }

  if (i < _cnt) {
    _nodes[i] = _nodes[--_cnt];
  }
}

//------------------------------dump-------------------------------------------
void Node_List::dump() const {
#ifndef PRODUCT
  for (uint i = 0; i < _cnt; i++) {
    if (_nodes[i]) {
      tty->print("%5d--> ", i);
      _nodes[i]->dump();
    }
  }
#endif
}

void Node_List::dump_simple() const {
#ifndef PRODUCT
  for (uint i = 0; i < _cnt; i++) {
    if( _nodes[i] ) {
      tty->print(" %d", _nodes[i]->_idx);
    } else {
      tty->print(" NULL");
    }
  }
#endif
}

//=============================================================================
//------------------------------remove-----------------------------------------
void Unique_Node_List::remove(Node* n) {
  if (_in_worklist.test(n->_idx)) {
    for (uint i = 0; i < size(); i++) {
      if (_nodes[i] == n) {
        map(i, Node_List::pop());
        _in_worklist.remove(n->_idx);
        return;
      }
    }
    ShouldNotReachHere();
  }
}

//-----------------------remove_useless_nodes----------------------------------
// Remove useless nodes from worklist
void Unique_Node_List::remove_useless_nodes(VectorSet &useful) {
  for (uint i = 0; i < size(); ++i) {
    Node *n = at(i);
    assert( n != NULL, "Did not expect null entries in worklist");
    if (!useful.test(n->_idx)) {
      _in_worklist.remove(n->_idx);
      map(i, Node_List::pop());
      --i;  // Visit popped node
      // If it was last entry, loop terminates since size() was also reduced
    }
  }
}

//=============================================================================
void Node_Stack::grow() {
  size_t old_top = pointer_delta(_inode_top,_inodes,sizeof(INode)); // save _top
  size_t old_max = pointer_delta(_inode_max,_inodes,sizeof(INode));
  size_t max = old_max << 1;             // max * 2
  _inodes = REALLOC_ARENA_ARRAY(_a, INode, _inodes, old_max, max);
  _inode_max = _inodes + max;
  _inode_top = _inodes + old_top;        // restore _top
}

// Node_Stack is used to map nodes.
Node* Node_Stack::find(uint idx) const {
  uint sz = size();
  for (uint i = 0; i < sz; i++) {
    if (idx == index_at(i)) {
      return node_at(i);
    }
  }
  return NULL;
}

//=============================================================================
uint TypeNode::size_of() const { return sizeof(*this); }
#ifndef PRODUCT
void TypeNode::dump_spec(outputStream *st) const {
  if (!Verbose && !WizardMode) {
    // standard dump does this in Verbose and WizardMode
    st->print(" #"); _type->dump_on(st);
  }
}

void TypeNode::dump_compact_spec(outputStream *st) const {
  st->print("#");
  _type->dump_on(st);
}
#endif
uint TypeNode::hash() const {
  return Node::hash() + _type->hash();
}
bool TypeNode::cmp(const Node& n) const {
  return !Type::cmp(_type, ((TypeNode&)n)._type);
}
const Type* TypeNode::bottom_type() const { return _type; }
const Type* TypeNode::Value(PhaseGVN* phase) const { return _type; }

//------------------------------ideal_reg--------------------------------------
uint TypeNode::ideal_reg() const {
  return _type->ideal_reg();
}
