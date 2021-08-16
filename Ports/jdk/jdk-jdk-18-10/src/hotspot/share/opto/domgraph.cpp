/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "libadt/vectset.hpp"
#include "memory/allocation.hpp"
#include "memory/resourceArea.hpp"
#include "opto/block.hpp"
#include "opto/machnode.hpp"
#include "opto/phaseX.hpp"
#include "opto/rootnode.hpp"

// Portions of code courtesy of Clifford Click

// A data structure that holds all the information needed to find dominators.
struct Tarjan {
  Block *_block;                // Basic block for this info

  uint _semi;                   // Semi-dominators
  uint _size;                   // Used for faster LINK and EVAL
  Tarjan *_parent;              // Parent in DFS
  Tarjan *_label;               // Used for LINK and EVAL
  Tarjan *_ancestor;            // Used for LINK and EVAL
  Tarjan *_child;               // Used for faster LINK and EVAL
  Tarjan *_dom;                 // Parent in dominator tree (immediate dom)
  Tarjan *_bucket;              // Set of vertices with given semidominator

  Tarjan *_dom_child;           // Child in dominator tree
  Tarjan *_dom_next;            // Next in dominator tree

  // Fast union-find work
  void COMPRESS();
  Tarjan *EVAL(void);
  void LINK( Tarjan *w, Tarjan *tarjan0 );

  void setdepth( uint size );

};

// Compute the dominator tree of the CFG.  The CFG must already have been
// constructed.  This is the Lengauer & Tarjan O(E-alpha(E,V)) algorithm.
void PhaseCFG::build_dominator_tree() {
  // Pre-grow the blocks array, prior to the ResourceMark kicking in
  _blocks.map(number_of_blocks(), 0);

  ResourceMark rm;
  // Setup mappings from my Graph to Tarjan's stuff and back
  // Note: Tarjan uses 1-based arrays
  Tarjan* tarjan = NEW_RESOURCE_ARRAY(Tarjan, number_of_blocks() + 1);

  // Tarjan's algorithm, almost verbatim:
  // Step 1:
  uint dfsnum = do_DFS(tarjan, number_of_blocks());
  if (dfsnum - 1 != number_of_blocks()) { // Check for unreachable loops!
    // If the returned dfsnum does not match the number of blocks, then we
    // must have some unreachable loops.  These can be made at any time by
    // IterGVN.  They are cleaned up by CCP or the loop opts, but the last
    // IterGVN can always make more that are not cleaned up.  Highly unlikely
    // except in ZKM.jar, where endless irreducible loops cause the loop opts
    // to not get run.
    //
    // Having found unreachable loops, we have made a bad RPO _block layout.
    // We can re-run the above DFS pass with the correct number of blocks,
    // and hack the Tarjan algorithm below to be robust in the presence of
    // such dead loops (as was done for the NTarjan code farther below).
    // Since this situation is so unlikely, instead I've decided to bail out.
    // CNC 7/24/2001
    C->record_method_not_compilable("unreachable loop");
    return;
  }
  _blocks._cnt = number_of_blocks();

  // Tarjan is using 1-based arrays, so these are some initialize flags
  tarjan[0]._size = tarjan[0]._semi = 0;
  tarjan[0]._label = &tarjan[0];

  for (uint i = number_of_blocks(); i >= 2; i--) { // For all vertices in DFS order
    Tarjan *w = &tarjan[i];     // Get vertex from DFS

    // Step 2:
    Node *whead = w->_block->head();
    for (uint j = 1; j < whead->req(); j++) {
      Block* b = get_block_for_node(whead->in(j));
      Tarjan *vx = &tarjan[b->_pre_order];
      Tarjan *u = vx->EVAL();
      if( u->_semi < w->_semi )
        w->_semi = u->_semi;
    }

    // w is added to a bucket here, and only here.
    // Thus w is in at most one bucket and the sum of all bucket sizes is O(n).
    // Thus bucket can be a linked list.
    // Thus we do not need a small integer name for each Block.
    w->_bucket = tarjan[w->_semi]._bucket;
    tarjan[w->_semi]._bucket = w;

    w->_parent->LINK( w, &tarjan[0] );

    // Step 3:
    for( Tarjan *vx = w->_parent->_bucket; vx; vx = vx->_bucket ) {
      Tarjan *u = vx->EVAL();
      vx->_dom = (u->_semi < vx->_semi) ? u : w->_parent;
    }
  }

  // Step 4:
  for (uint i = 2; i <= number_of_blocks(); i++) {
    Tarjan *w = &tarjan[i];
    if( w->_dom != &tarjan[w->_semi] )
      w->_dom = w->_dom->_dom;
    w->_dom_next = w->_dom_child = NULL;  // Initialize for building tree later
  }
  // No immediate dominator for the root
  Tarjan *w = &tarjan[get_root_block()->_pre_order];
  w->_dom = NULL;
  w->_dom_next = w->_dom_child = NULL;  // Initialize for building tree later

  // Convert the dominator tree array into my kind of graph
  for(uint i = 1; i <= number_of_blocks(); i++){ // For all Tarjan vertices
    Tarjan *t = &tarjan[i];     // Handy access
    Tarjan *tdom = t->_dom;     // Handy access to immediate dominator
    if( tdom )  {               // Root has no immediate dominator
      t->_block->_idom = tdom->_block; // Set immediate dominator
      t->_dom_next = tdom->_dom_child; // Make me a sibling of parent's child
      tdom->_dom_child = t;     // Make me a child of my parent
    } else
      t->_block->_idom = NULL;  // Root
  }
  w->setdepth(number_of_blocks() + 1); // Set depth in dominator tree

}

class Block_Stack {
  private:
    struct Block_Descr {
      Block  *block;     // Block
      int    index;      // Index of block's successor pushed on stack
      int    freq_idx;   // Index of block's most frequent successor
    };
    Block_Descr *_stack_top;
    Block_Descr *_stack_max;
    Block_Descr *_stack;
    Tarjan *_tarjan;
    uint most_frequent_successor( Block *b );
  public:
    Block_Stack(Tarjan *tarjan, int size) : _tarjan(tarjan) {
      _stack = NEW_RESOURCE_ARRAY(Block_Descr, size);
      _stack_max = _stack + size;
      _stack_top = _stack - 1; // stack is empty
    }
    void push(uint pre_order, Block *b) {
      Tarjan *t = &_tarjan[pre_order]; // Fast local access
      b->_pre_order = pre_order;    // Flag as visited
      t->_block = b;                // Save actual block
      t->_semi = pre_order;         // Block to DFS map
      t->_label = t;                // DFS to vertex map
      t->_ancestor = NULL;          // Fast LINK & EVAL setup
      t->_child = &_tarjan[0];      // Sentenial
      t->_size = 1;
      t->_bucket = NULL;
      if (pre_order == 1)
        t->_parent = NULL;          // first block doesn't have parent
      else {
        // Save parent (current top block on stack) in DFS
        t->_parent = &_tarjan[_stack_top->block->_pre_order];
      }
      // Now put this block on stack
      ++_stack_top;
      assert(_stack_top < _stack_max, ""); // assert if stack have to grow
      _stack_top->block  = b;
      _stack_top->index  = -1;
      // Find the index into b->succs[] array of the most frequent successor.
      _stack_top->freq_idx = most_frequent_successor(b); // freq_idx >= 0
    }
    Block* pop() { Block* b = _stack_top->block; _stack_top--; return b; }
    bool is_nonempty() { return (_stack_top >= _stack); }
    bool last_successor() { return (_stack_top->index == _stack_top->freq_idx); }
    Block* next_successor()  {
      int i = _stack_top->index;
      i++;
      if (i == _stack_top->freq_idx) i++;
      if (i >= (int)(_stack_top->block->_num_succs)) {
        i = _stack_top->freq_idx;   // process most frequent successor last
      }
      _stack_top->index = i;
      return _stack_top->block->_succs[ i ];
    }
};

// Find the index into the b->succs[] array of the most frequent successor.
uint Block_Stack::most_frequent_successor( Block *b ) {
  uint freq_idx = 0;
  int eidx = b->end_idx();
  Node *n = b->get_node(eidx);
  int op = n->is_Mach() ? n->as_Mach()->ideal_Opcode() : n->Opcode();
  switch( op ) {
  case Op_CountedLoopEnd:
  case Op_If: {               // Split frequency amongst children
    float prob = n->as_MachIf()->_prob;
    // Is succ[0] the TRUE branch or the FALSE branch?
    if( b->get_node(eidx+1)->Opcode() == Op_IfFalse )
      prob = 1.0f - prob;
    freq_idx = prob < PROB_FAIR;      // freq=1 for succ[0] < 0.5 prob
    break;
  }
  case Op_Catch:                // Split frequency amongst children
    for( freq_idx = 0; freq_idx < b->_num_succs; freq_idx++ )
      if( b->get_node(eidx+1+freq_idx)->as_CatchProj()->_con == CatchProjNode::fall_through_index )
        break;
    // Handle case of no fall-thru (e.g., check-cast MUST throw an exception)
    if( freq_idx == b->_num_succs ) freq_idx = 0;
    break;
    // Currently there is no support for finding out the most
    // frequent successor for jumps, so lets just make it the first one
  case Op_Jump:
  case Op_Root:
  case Op_Goto:
  case Op_NeverBranch:
    freq_idx = 0;               // fall thru
    break;
  case Op_TailCall:
  case Op_TailJump:
  case Op_Return:
  case Op_Halt:
  case Op_Rethrow:
    break;
  default:
    ShouldNotReachHere();
  }
  return freq_idx;
}

// Perform DFS search.  Setup 'vertex' as DFS to vertex mapping.  Setup
// 'semi' as vertex to DFS mapping.  Set 'parent' to DFS parent.
uint PhaseCFG::do_DFS(Tarjan *tarjan, uint rpo_counter) {
  Block* root_block = get_root_block();
  uint pre_order = 1;
  // Allocate stack of size number_of_blocks() + 1 to avoid frequent realloc
  Block_Stack bstack(tarjan, number_of_blocks() + 1);

  // Push on stack the state for the first block
  bstack.push(pre_order, root_block);
  ++pre_order;

  while (bstack.is_nonempty()) {
    if (!bstack.last_successor()) {
      // Walk over all successors in pre-order (DFS).
      Block* next_block = bstack.next_successor();
      if (next_block->_pre_order == 0) { // Check for no-pre-order, not-visited
        // Push on stack the state of successor
        bstack.push(pre_order, next_block);
        ++pre_order;
      }
    }
    else {
      // Build a reverse post-order in the CFG _blocks array
      Block *stack_top = bstack.pop();
      stack_top->_rpo = --rpo_counter;
      _blocks.map(stack_top->_rpo, stack_top);
    }
  }
  return pre_order;
}

void Tarjan::COMPRESS()
{
  assert( _ancestor != 0, "" );
  if( _ancestor->_ancestor != 0 ) {
    _ancestor->COMPRESS( );
    if( _ancestor->_label->_semi < _label->_semi )
      _label = _ancestor->_label;
    _ancestor = _ancestor->_ancestor;
  }
}

Tarjan *Tarjan::EVAL() {
  if( !_ancestor ) return _label;
  COMPRESS();
  return (_ancestor->_label->_semi >= _label->_semi) ? _label : _ancestor->_label;
}

void Tarjan::LINK( Tarjan *w, Tarjan *tarjan0 ) {
  Tarjan *s = w;
  while( w->_label->_semi < s->_child->_label->_semi ) {
    if( s->_size + s->_child->_child->_size >= (s->_child->_size << 1) ) {
      s->_child->_ancestor = s;
      s->_child = s->_child->_child;
    } else {
      s->_child->_size = s->_size;
      s = s->_ancestor = s->_child;
    }
  }
  s->_label = w->_label;
  _size += w->_size;
  if( _size < (w->_size << 1) ) {
    Tarjan *tmp = s; s = _child; _child = tmp;
  }
  while( s != tarjan0 ) {
    s->_ancestor = this;
    s = s->_child;
  }
}

void Tarjan::setdepth( uint stack_size ) {
  Tarjan **top  = NEW_RESOURCE_ARRAY(Tarjan*, stack_size);
  Tarjan **next = top;
  Tarjan **last;
  uint depth = 0;
  *top = this;
  ++top;
  do {
    // next level
    ++depth;
    last = top;
    do {
      // Set current depth for all tarjans on this level
      Tarjan *t = *next;     // next tarjan from stack
      ++next;
      do {
        t->_block->_dom_depth = depth; // Set depth in dominator tree
        Tarjan *dom_child = t->_dom_child;
        t = t->_dom_next;    // next tarjan
        if (dom_child != NULL) {
          *top = dom_child;  // save child on stack
          ++top;
        }
      } while (t != NULL);
    } while (next < last);
  } while (last < top);
}

// Compute dominators on the Sea of Nodes form
// A data structure that holds all the information needed to find dominators.
struct NTarjan {
  Node *_control;               // Control node associated with this info

  uint _semi;                   // Semi-dominators
  uint _size;                   // Used for faster LINK and EVAL
  NTarjan *_parent;             // Parent in DFS
  NTarjan *_label;              // Used for LINK and EVAL
  NTarjan *_ancestor;           // Used for LINK and EVAL
  NTarjan *_child;              // Used for faster LINK and EVAL
  NTarjan *_dom;                // Parent in dominator tree (immediate dom)
  NTarjan *_bucket;             // Set of vertices with given semidominator

  NTarjan *_dom_child;          // Child in dominator tree
  NTarjan *_dom_next;           // Next in dominator tree

  // Perform DFS search.
  // Setup 'vertex' as DFS to vertex mapping.
  // Setup 'semi' as vertex to DFS mapping.
  // Set 'parent' to DFS parent.
  static int DFS( NTarjan *ntarjan, VectorSet &visited, PhaseIdealLoop *pil, uint *dfsorder );
  void setdepth( uint size, uint *dom_depth );

  // Fast union-find work
  void COMPRESS();
  NTarjan *EVAL(void);
  void LINK( NTarjan *w, NTarjan *ntarjan0 );
#ifndef PRODUCT
  void dump(int offset) const;
#endif
};

// Compute the dominator tree of the sea of nodes.  This version walks all CFG
// nodes (using the is_CFG() call) and places them in a dominator tree.  Thus,
// it needs a count of the CFG nodes for the mapping table. This is the
// Lengauer & Tarjan O(E-alpha(E,V)) algorithm.
void PhaseIdealLoop::Dominators() {
  ResourceMark rm;
  // Setup mappings from my Graph to Tarjan's stuff and back
  // Note: Tarjan uses 1-based arrays
  NTarjan *ntarjan = NEW_RESOURCE_ARRAY(NTarjan,C->unique()+1);
  // Initialize _control field for fast reference
  int i;
  for( i= C->unique()-1; i>=0; i-- )
    ntarjan[i]._control = NULL;

  // Store the DFS order for the main loop
  const uint fill_value = max_juint;
  uint *dfsorder = NEW_RESOURCE_ARRAY(uint,C->unique()+1);
  memset(dfsorder, fill_value, (C->unique()+1) * sizeof(uint));

  // Tarjan's algorithm, almost verbatim:
  // Step 1:
  VectorSet visited;
  int dfsnum = NTarjan::DFS( ntarjan, visited, this, dfsorder);

  // Tarjan is using 1-based arrays, so these are some initialize flags
  ntarjan[0]._size = ntarjan[0]._semi = 0;
  ntarjan[0]._label = &ntarjan[0];

  for( i = dfsnum-1; i>1; i-- ) {        // For all nodes in reverse DFS order
    NTarjan *w = &ntarjan[i];            // Get Node from DFS
    assert(w->_control != NULL,"bad DFS walk");

    // Step 2:
    Node *whead = w->_control;
    for( uint j=0; j < whead->req(); j++ ) { // For each predecessor
      if( whead->in(j) == NULL || !whead->in(j)->is_CFG() )
        continue;                            // Only process control nodes
      uint b = dfsorder[whead->in(j)->_idx];
      if(b == fill_value) continue;
      NTarjan *vx = &ntarjan[b];
      NTarjan *u = vx->EVAL();
      if( u->_semi < w->_semi )
        w->_semi = u->_semi;
    }

    // w is added to a bucket here, and only here.
    // Thus w is in at most one bucket and the sum of all bucket sizes is O(n).
    // Thus bucket can be a linked list.
    w->_bucket = ntarjan[w->_semi]._bucket;
    ntarjan[w->_semi]._bucket = w;

    w->_parent->LINK( w, &ntarjan[0] );

    // Step 3:
    for( NTarjan *vx = w->_parent->_bucket; vx; vx = vx->_bucket ) {
      NTarjan *u = vx->EVAL();
      vx->_dom = (u->_semi < vx->_semi) ? u : w->_parent;
    }

    // Cleanup any unreachable loops now.  Unreachable loops are loops that
    // flow into the main graph (and hence into ROOT) but are not reachable
    // from above.  Such code is dead, but requires a global pass to detect
    // it; this global pass was the 'build_loop_tree' pass run just prior.
    if( !_verify_only && whead->is_Region() ) {
      for( uint i = 1; i < whead->req(); i++ ) {
        if (!has_node(whead->in(i))) {
          // Kill dead input path
          assert( !visited.test(whead->in(i)->_idx),
                  "input with no loop must be dead" );
          _igvn.delete_input_of(whead, i);
          for (DUIterator_Fast jmax, j = whead->fast_outs(jmax); j < jmax; j++) {
            Node* p = whead->fast_out(j);
            if( p->is_Phi() ) {
              _igvn.delete_input_of(p, i);
            }
          }
          i--;                  // Rerun same iteration
        } // End of if dead input path
      } // End of for all input paths
    } // End if if whead is a Region
  } // End of for all Nodes in reverse DFS order

  // Step 4:
  for( i=2; i < dfsnum; i++ ) { // DFS order
    NTarjan *w = &ntarjan[i];
    assert(w->_control != NULL,"Bad DFS walk");
    if( w->_dom != &ntarjan[w->_semi] )
      w->_dom = w->_dom->_dom;
    w->_dom_next = w->_dom_child = NULL;  // Initialize for building tree later
  }
  // No immediate dominator for the root
  NTarjan *w = &ntarjan[dfsorder[C->root()->_idx]];
  w->_dom = NULL;
  w->_parent = NULL;
  w->_dom_next = w->_dom_child = NULL;  // Initialize for building tree later

  // Convert the dominator tree array into my kind of graph
  for( i=1; i<dfsnum; i++ ) {          // For all Tarjan vertices
    NTarjan *t = &ntarjan[i];          // Handy access
    assert(t->_control != NULL,"Bad DFS walk");
    NTarjan *tdom = t->_dom;           // Handy access to immediate dominator
    if( tdom )  {                      // Root has no immediate dominator
      _idom[t->_control->_idx] = tdom->_control; // Set immediate dominator
      t->_dom_next = tdom->_dom_child; // Make me a sibling of parent's child
      tdom->_dom_child = t;            // Make me a child of my parent
    } else
      _idom[C->root()->_idx] = NULL; // Root
  }
  w->setdepth( C->unique()+1, _dom_depth ); // Set depth in dominator tree
  // Pick up the 'top' node as well
  _idom     [C->top()->_idx] = C->root();
  _dom_depth[C->top()->_idx] = 1;

  // Debug Print of Dominator tree
  if( PrintDominators ) {
#ifndef PRODUCT
    w->dump(0);
#endif
  }
}

// Perform DFS search.  Setup 'vertex' as DFS to vertex mapping.  Setup
// 'semi' as vertex to DFS mapping.  Set 'parent' to DFS parent.
int NTarjan::DFS( NTarjan *ntarjan, VectorSet &visited, PhaseIdealLoop *pil, uint *dfsorder) {
  // Allocate stack of size C->live_nodes()/8 to avoid frequent realloc
  GrowableArray <Node *> dfstack(pil->C->live_nodes() >> 3);
  Node *b = pil->C->root();
  int dfsnum = 1;
  dfsorder[b->_idx] = dfsnum; // Cache parent's dfsnum for a later use
  dfstack.push(b);

  while (dfstack.is_nonempty()) {
    b = dfstack.pop();
    if( !visited.test_set(b->_idx) ) { // Test node and flag it as visited
      NTarjan *w = &ntarjan[dfsnum];
      // Only fully process control nodes
      w->_control = b;                 // Save actual node
      // Use parent's cached dfsnum to identify "Parent in DFS"
      w->_parent = &ntarjan[dfsorder[b->_idx]];
      dfsorder[b->_idx] = dfsnum;      // Save DFS order info
      w->_semi = dfsnum;               // Node to DFS map
      w->_label = w;                   // DFS to vertex map
      w->_ancestor = NULL;             // Fast LINK & EVAL setup
      w->_child = &ntarjan[0];         // Sentinal
      w->_size = 1;
      w->_bucket = NULL;

      // Need DEF-USE info for this pass
      for ( int i = b->outcnt(); i-- > 0; ) { // Put on stack backwards
        Node* s = b->raw_out(i);       // Get a use
        // CFG nodes only and not dead stuff
        if( s->is_CFG() && pil->has_node(s) && !visited.test(s->_idx) ) {
          dfsorder[s->_idx] = dfsnum;  // Cache parent's dfsnum for a later use
          dfstack.push(s);
        }
      }
      dfsnum++;  // update after parent's dfsnum has been cached.
    }
  }

  return dfsnum;
}

void NTarjan::COMPRESS()
{
  assert( _ancestor != 0, "" );
  if( _ancestor->_ancestor != 0 ) {
    _ancestor->COMPRESS( );
    if( _ancestor->_label->_semi < _label->_semi )
      _label = _ancestor->_label;
    _ancestor = _ancestor->_ancestor;
  }
}

NTarjan *NTarjan::EVAL() {
  if( !_ancestor ) return _label;
  COMPRESS();
  return (_ancestor->_label->_semi >= _label->_semi) ? _label : _ancestor->_label;
}

void NTarjan::LINK( NTarjan *w, NTarjan *ntarjan0 ) {
  NTarjan *s = w;
  while( w->_label->_semi < s->_child->_label->_semi ) {
    if( s->_size + s->_child->_child->_size >= (s->_child->_size << 1) ) {
      s->_child->_ancestor = s;
      s->_child = s->_child->_child;
    } else {
      s->_child->_size = s->_size;
      s = s->_ancestor = s->_child;
    }
  }
  s->_label = w->_label;
  _size += w->_size;
  if( _size < (w->_size << 1) ) {
    NTarjan *tmp = s; s = _child; _child = tmp;
  }
  while( s != ntarjan0 ) {
    s->_ancestor = this;
    s = s->_child;
  }
}

void NTarjan::setdepth( uint stack_size, uint *dom_depth ) {
  NTarjan **top  = NEW_RESOURCE_ARRAY(NTarjan*, stack_size);
  NTarjan **next = top;
  NTarjan **last;
  uint depth = 0;
  *top = this;
  ++top;
  do {
    // next level
    ++depth;
    last = top;
    do {
      // Set current depth for all tarjans on this level
      NTarjan *t = *next;    // next tarjan from stack
      ++next;
      do {
        dom_depth[t->_control->_idx] = depth; // Set depth in dominator tree
        NTarjan *dom_child = t->_dom_child;
        t = t->_dom_next;    // next tarjan
        if (dom_child != NULL) {
          *top = dom_child;  // save child on stack
          ++top;
        }
      } while (t != NULL);
    } while (next < last);
  } while (last < top);
}

#ifndef PRODUCT
void NTarjan::dump(int offset) const {
  // Dump the data from this node
  int i;
  for(i = offset; i >0; i--)  // Use indenting for tree structure
    tty->print("  ");
  tty->print("Dominator Node: ");
  _control->dump();               // Control node for this dom node
  tty->print("\n");
  for(i = offset; i >0; i--)      // Use indenting for tree structure
    tty->print("  ");
  tty->print("semi:%d, size:%d\n",_semi, _size);
  for(i = offset; i >0; i--)      // Use indenting for tree structure
    tty->print("  ");
  tty->print("DFS Parent: ");
  if(_parent != NULL)
    _parent->_control->dump();    // Parent in DFS
  tty->print("\n");
  for(i = offset; i >0; i--)      // Use indenting for tree structure
    tty->print("  ");
  tty->print("Dom Parent: ");
  if(_dom != NULL)
    _dom->_control->dump();       // Parent in Dominator Tree
  tty->print("\n");

  // Recurse over remaining tree
  if( _dom_child ) _dom_child->dump(offset+2);   // Children in dominator tree
  if( _dom_next  ) _dom_next ->dump(offset  );   // Siblings in dominator tree

}
#endif
