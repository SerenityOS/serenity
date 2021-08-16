/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "opto/callnode.hpp"
#include "opto/loopnode.hpp"
#include "opto/movenode.hpp"


//------------------------------split_thru_region------------------------------
// Split Node 'n' through merge point.
Node *PhaseIdealLoop::split_thru_region( Node *n, Node *region ) {
  uint wins = 0;
  assert( n->is_CFG(), "" );
  assert( region->is_Region(), "" );
  Node *r = new RegionNode( region->req() );
  IdealLoopTree *loop = get_loop( n );
  for( uint i = 1; i < region->req(); i++ ) {
    Node *x = n->clone();
    Node *in0 = n->in(0);
    if( in0->in(0) == region ) x->set_req( 0, in0->in(i) );
    for( uint j = 1; j < n->req(); j++ ) {
      Node *in = n->in(j);
      if( get_ctrl(in) == region )
        x->set_req( j, in->in(i) );
    }
    _igvn.register_new_node_with_optimizer(x);
    set_loop(x, loop);
    set_idom(x, x->in(0), dom_depth(x->in(0))+1);
    r->init_req(i, x);
  }

  // Record region
  r->set_req(0,region);         // Not a TRUE RegionNode
  _igvn.register_new_node_with_optimizer(r);
  set_loop(r, loop);
  if( !loop->_child )
    loop->_body.push(r);
  return r;
}

//------------------------------split_up---------------------------------------
// Split block-local op up through the phis to empty the current block
bool PhaseIdealLoop::split_up( Node *n, Node *blk1, Node *blk2 ) {
  if( n->is_CFG() ) {
    assert( n->in(0) != blk1, "Lousy candidate for split-if" );
    return false;
  }
  if( get_ctrl(n) != blk1 && get_ctrl(n) != blk2 )
    return false;               // Not block local
  if( n->is_Phi() ) return false; // Local PHIs are expected

  // Recursively split-up inputs
  for (uint i = 1; i < n->req(); i++) {
    if( split_up( n->in(i), blk1, blk2 ) ) {
      // Got split recursively and self went dead?
      if (n->outcnt() == 0)
        _igvn.remove_dead_node(n);
      return true;
    }
  }

  // Check for needing to clone-up a compare.  Can't do that, it forces
  // another (nested) split-if transform.  Instead, clone it "down".
  if( n->is_Cmp() ) {
    assert(get_ctrl(n) == blk2 || get_ctrl(n) == blk1, "must be in block with IF");
    // Check for simple Cmp/Bool/CMove which we can clone-up.  Cmp/Bool/CMove
    // sequence can have no other users and it must all reside in the split-if
    // block.  Non-simple Cmp/Bool/CMove sequences are 'cloned-down' below -
    // private, per-use versions of the Cmp and Bool are made.  These sink to
    // the CMove block.  If the CMove is in the split-if block, then in the
    // next iteration this will become a simple Cmp/Bool/CMove set to clone-up.
    Node *bol, *cmov;
    if( !(n->outcnt() == 1 && n->unique_out()->is_Bool() &&
          (bol = n->unique_out()->as_Bool()) &&
          (get_ctrl(bol) == blk1 ||
           get_ctrl(bol) == blk2) &&
          bol->outcnt() == 1 &&
          bol->unique_out()->is_CMove() &&
          (cmov = bol->unique_out()->as_CMove()) &&
          (get_ctrl(cmov) == blk1 ||
           get_ctrl(cmov) == blk2) ) ) {

      // Must clone down
#ifndef PRODUCT
      if( PrintOpto && VerifyLoopOptimizations ) {
        tty->print("Cloning down: ");
        n->dump();
      }
#endif
      if (!n->is_FastLock()) {
        // Clone down any block-local BoolNode uses of this CmpNode
        for (DUIterator i = n->outs(); n->has_out(i); i++) {
          Node* bol = n->out(i);
          assert( bol->is_Bool(), "" );
          if (bol->outcnt() == 1) {
            Node* use = bol->unique_out();
            if (use->Opcode() == Op_Opaque4) {
              if (use->outcnt() == 1) {
                Node* iff = use->unique_out();
                assert(iff->is_If(), "unexpected node type");
                Node *use_c = iff->in(0);
                if (use_c == blk1 || use_c == blk2) {
                  continue;
                }
              }
            } else {
              // We might see an Opaque1 from a loop limit check here
              assert(use->is_If() || use->is_CMove() || use->Opcode() == Op_Opaque1, "unexpected node type");
              Node *use_c = use->is_If() ? use->in(0) : get_ctrl(use);
              if (use_c == blk1 || use_c == blk2) {
                assert(use->is_CMove(), "unexpected node type");
                continue;
              }
            }
          }
          if (get_ctrl(bol) == blk1 || get_ctrl(bol) == blk2) {
            // Recursively sink any BoolNode
#ifndef PRODUCT
            if( PrintOpto && VerifyLoopOptimizations ) {
              tty->print("Cloning down: ");
              bol->dump();
            }
#endif
            for (DUIterator j = bol->outs(); bol->has_out(j); j++) {
              Node* u = bol->out(j);
              // Uses are either IfNodes, CMoves or Opaque4
              if (u->Opcode() == Op_Opaque4) {
                assert(u->in(1) == bol, "bad input");
                for (DUIterator_Last kmin, k = u->last_outs(kmin); k >= kmin; --k) {
                  Node* iff = u->last_out(k);
                  assert(iff->is_If() || iff->is_CMove(), "unexpected node type");
                  assert( iff->in(1) == u, "" );
                  // Get control block of either the CMove or the If input
                  Node *iff_ctrl = iff->is_If() ? iff->in(0) : get_ctrl(iff);
                  Node *x1 = bol->clone();
                  Node *x2 = u->clone();
                  register_new_node(x1, iff_ctrl);
                  register_new_node(x2, iff_ctrl);
                  _igvn.replace_input_of(x2, 1, x1);
                  _igvn.replace_input_of(iff, 1, x2);
                }
                _igvn.remove_dead_node(u);
                --j;
              } else {
                // We might see an Opaque1 from a loop limit check here
                assert(u->is_If() || u->is_CMove() || u->Opcode() == Op_Opaque1, "unexpected node type");
                assert(u->in(1) == bol, "");
                // Get control block of either the CMove or the If input
                Node *u_ctrl = u->is_If() ? u->in(0) : get_ctrl(u);
                assert((u_ctrl != blk1 && u_ctrl != blk2) || u->is_CMove(), "won't converge");
                Node *x = bol->clone();
                register_new_node(x, u_ctrl);
                _igvn.replace_input_of(u, 1, x);
                --j;
              }
            }
            _igvn.remove_dead_node(bol);
            --i;
          }
        }
      }
      // Clone down this CmpNode
      for (DUIterator_Last jmin, j = n->last_outs(jmin); j >= jmin; --j) {
        Node* use = n->last_out(j);
        uint pos = 1;
        if (n->is_FastLock()) {
          pos = TypeFunc::Parms + 2;
          assert(use->is_Lock(), "FastLock only used by LockNode");
        }
        assert(use->in(pos) == n, "" );
        Node *x = n->clone();
        register_new_node(x, ctrl_or_self(use));
        _igvn.replace_input_of(use, pos, x);
      }
      _igvn.remove_dead_node( n );

      return true;
    }
  }

  // See if splitting-up a Store.  Any anti-dep loads must go up as
  // well.  An anti-dep load might be in the wrong block, because in
  // this particular layout/schedule we ignored anti-deps and allow
  // memory to be alive twice.  This only works if we do the same
  // operations on anti-dep loads as we do their killing stores.
  if( n->is_Store() && n->in(MemNode::Memory)->in(0) == n->in(0) ) {
    // Get store's memory slice
    int alias_idx = C->get_alias_index(_igvn.type(n->in(MemNode::Address))->is_ptr());

    // Get memory-phi anti-dep loads will be using
    Node *memphi = n->in(MemNode::Memory);
    assert( memphi->is_Phi(), "" );
    // Hoist any anti-dep load to the splitting block;
    // it will then "split-up".
    for (DUIterator_Fast imax,i = memphi->fast_outs(imax); i < imax; i++) {
      Node *load = memphi->fast_out(i);
      if( load->is_Load() && alias_idx == C->get_alias_index(_igvn.type(load->in(MemNode::Address))->is_ptr()) )
        set_ctrl(load,blk1);
    }
  }

  // Found some other Node; must clone it up
#ifndef PRODUCT
  if( PrintOpto && VerifyLoopOptimizations ) {
    tty->print("Cloning up: ");
    n->dump();
  }
#endif

  // ConvI2L may have type information on it which becomes invalid if
  // it moves up in the graph so change any clones so widen the type
  // to TypeLong::INT when pushing it up.
  const Type* rtype = NULL;
  if (n->Opcode() == Op_ConvI2L && n->bottom_type() != TypeLong::INT) {
    rtype = TypeLong::INT;
  }

  // Now actually split-up this guy.  One copy per control path merging.
  Node *phi = PhiNode::make_blank(blk1, n);
  for( uint j = 1; j < blk1->req(); j++ ) {
    Node *x = n->clone();
    // Widen the type of the ConvI2L when pushing up.
    if (rtype != NULL) x->as_Type()->set_type(rtype);
    if( n->in(0) && n->in(0) == blk1 )
      x->set_req( 0, blk1->in(j) );
    for( uint i = 1; i < n->req(); i++ ) {
      Node *m = n->in(i);
      if( get_ctrl(m) == blk1 ) {
        assert( m->in(0) == blk1, "" );
        x->set_req( i, m->in(j) );
      }
    }
    register_new_node( x, blk1->in(j) );
    phi->init_req( j, x );
  }
  // Announce phi to optimizer
  register_new_node(phi, blk1);

  // Remove cloned-up value from optimizer; use phi instead
  _igvn.replace_node( n, phi );

  // (There used to be a self-recursive call to split_up() here,
  // but it is not needed.  All necessary forward walking is done
  // by do_split_if() below.)

  return true;
}

//------------------------------register_new_node------------------------------
void PhaseIdealLoop::register_new_node( Node *n, Node *blk ) {
  assert(!n->is_CFG(), "must be data node");
  _igvn.register_new_node_with_optimizer(n);
  set_ctrl(n, blk);
  IdealLoopTree *loop = get_loop(blk);
  if( !loop->_child )
    loop->_body.push(n);
}

//------------------------------small_cache------------------------------------
struct small_cache : public Dict {

  small_cache() : Dict( cmpkey, hashptr ) {}
  Node *probe( Node *use_blk ) { return (Node*)((*this)[use_blk]); }
  void lru_insert( Node *use_blk, Node *new_def ) { Insert(use_blk,new_def); }
};

//------------------------------spinup-----------------------------------------
// "Spin up" the dominator tree, starting at the use site and stopping when we
// find the post-dominating point.

// We must be at the merge point which post-dominates 'new_false' and
// 'new_true'.  Figure out which edges into the RegionNode eventually lead up
// to false and which to true.  Put in a PhiNode to merge values; plug in
// the appropriate false-arm or true-arm values.  If some path leads to the
// original IF, then insert a Phi recursively.
Node *PhaseIdealLoop::spinup( Node *iff_dom, Node *new_false, Node *new_true, Node *use_blk, Node *def, small_cache *cache ) {
  if (use_blk->is_top())        // Handle dead uses
    return use_blk;
  Node *prior_n = (Node*)((intptr_t)0xdeadbeef);
  Node *n = use_blk;            // Get path input
  assert( use_blk != iff_dom, "" );
  // Here's the "spinup" the dominator tree loop.  Do a cache-check
  // along the way, in case we've come this way before.
  while( n != iff_dom ) {       // Found post-dominating point?
    prior_n = n;
    n = idom(n);                // Search higher
    Node *s = cache->probe( prior_n ); // Check cache
    if( s ) return s;           // Cache hit!
  }

  Node *phi_post;
  if( prior_n == new_false || prior_n == new_true ) {
    phi_post = def->clone();
    phi_post->set_req(0, prior_n );
    register_new_node(phi_post, prior_n);
  } else {
    // This method handles both control uses (looking for Regions) or data
    // uses (looking for Phis).  If looking for a control use, then we need
    // to insert a Region instead of a Phi; however Regions always exist
    // previously (the hash_find_insert below would always hit) so we can
    // return the existing Region.
    if( def->is_CFG() ) {
      phi_post = prior_n;       // If looking for CFG, return prior
    } else {
      assert( def->is_Phi(), "" );
      assert( prior_n->is_Region(), "must be a post-dominating merge point" );

      // Need a Phi here
      phi_post = PhiNode::make_blank(prior_n, def);
      // Search for both true and false on all paths till find one.
      for( uint i = 1; i < phi_post->req(); i++ ) // For all paths
        phi_post->init_req( i, spinup( iff_dom, new_false, new_true, prior_n->in(i), def, cache ) );
      Node *t = _igvn.hash_find_insert(phi_post);
      if( t ) {                 // See if we already have this one
        // phi_post will not be used, so kill it
        _igvn.remove_dead_node(phi_post);
        phi_post->destruct(&_igvn);
        phi_post = t;
      } else {
        register_new_node( phi_post, prior_n );
      }
    }
  }

  // Update cache everywhere
  prior_n = (Node*)((intptr_t)0xdeadbeef);  // Reset IDOM walk
  n = use_blk;                  // Get path input
  // Spin-up the idom tree again, basically doing path-compression.
  // Insert cache entries along the way, so that if we ever hit this
  // point in the IDOM tree again we'll stop immediately on a cache hit.
  while( n != iff_dom ) {       // Found post-dominating point?
    prior_n = n;
    n = idom(n);                // Search higher
    cache->lru_insert( prior_n, phi_post ); // Fill cache
  } // End of while not gone high enough

  return phi_post;
}

//------------------------------find_use_block---------------------------------
// Find the block a USE is in.  Normally USE's are in the same block as the
// using instruction.  For Phi-USE's, the USE is in the predecessor block
// along the corresponding path.
Node *PhaseIdealLoop::find_use_block( Node *use, Node *def, Node *old_false, Node *new_false, Node *old_true, Node *new_true ) {
  // CFG uses are their own block
  if( use->is_CFG() )
    return use;

  if( use->is_Phi() ) {         // Phi uses in prior block
    // Grab the first Phi use; there may be many.
    // Each will be handled as a separate iteration of
    // the "while( phi->outcnt() )" loop.
    uint j;
    for( j = 1; j < use->req(); j++ )
      if( use->in(j) == def )
        break;
    assert( j < use->req(), "def should be among use's inputs" );
    return use->in(0)->in(j);
  }
  // Normal (non-phi) use
  Node *use_blk = get_ctrl(use);
  // Some uses are directly attached to the old (and going away)
  // false and true branches.
  if( use_blk == old_false ) {
    use_blk = new_false;
    set_ctrl(use, new_false);
  }
  if( use_blk == old_true ) {
    use_blk = new_true;
    set_ctrl(use, new_true);
  }

  if (use_blk == NULL) {        // He's dead, Jim
    _igvn.replace_node(use, C->top());
  }

  return use_blk;
}

//------------------------------handle_use-------------------------------------
// Handle uses of the merge point.  Basically, split-if makes the merge point
// go away so all uses of the merge point must go away as well.  Most block
// local uses have already been split-up, through the merge point.  Uses from
// far below the merge point can't always be split up (e.g., phi-uses are
// pinned) and it makes too much stuff live.  Instead we use a path-based
// solution to move uses down.
//
// If the use is along the pre-split-CFG true branch, then the new use will
// be from the post-split-CFG true merge point.  Vice-versa for the false
// path.  Some uses will be along both paths; then we sink the use to the
// post-dominating location; we may need to insert a Phi there.
void PhaseIdealLoop::handle_use( Node *use, Node *def, small_cache *cache, Node *region_dom, Node *new_false, Node *new_true, Node *old_false, Node *old_true ) {

  Node *use_blk = find_use_block(use,def,old_false,new_false,old_true,new_true);
  if( !use_blk ) return;        // He's dead, Jim

  // Walk up the dominator tree until I hit either the old IfFalse, the old
  // IfTrue or the old If.  Insert Phis where needed.
  Node *new_def = spinup( region_dom, new_false, new_true, use_blk, def, cache );

  // Found where this USE goes.  Re-point him.
  uint i;
  for( i = 0; i < use->req(); i++ )
    if( use->in(i) == def )
      break;
  assert( i < use->req(), "def should be among use's inputs" );
  _igvn.replace_input_of(use, i, new_def);
}

//------------------------------do_split_if------------------------------------
// Found an If getting its condition-code input from a Phi in the same block.
// Split thru the Region.
void PhaseIdealLoop::do_split_if( Node *iff ) {
  if (PrintOpto && VerifyLoopOptimizations) {
    tty->print_cr("Split-if");
  }
  if (TraceLoopOpts) {
    tty->print_cr("SplitIf");
  }

  C->set_major_progress();
  Node *region = iff->in(0);
  Node *region_dom = idom(region);

  // We are going to clone this test (and the control flow with it) up through
  // the incoming merge point.  We need to empty the current basic block.
  // Clone any instructions which must be in this block up through the merge
  // point.
  DUIterator i, j;
  bool progress = true;
  while (progress) {
    progress = false;
    for (i = region->outs(); region->has_out(i); i++) {
      Node* n = region->out(i);
      if( n == region ) continue;
      // The IF to be split is OK.
      if( n == iff ) continue;
      if( !n->is_Phi() ) {      // Found pinned memory op or such
        if (split_up(n, region, iff)) {
          i = region->refresh_out_pos(i);
          progress = true;
        }
        continue;
      }
      assert( n->in(0) == region, "" );

      // Recursively split up all users of a Phi
      for (j = n->outs(); n->has_out(j); j++) {
        Node* m = n->out(j);
        // If m is dead, throw it away, and declare progress
        if (_nodes[m->_idx] == NULL) {
          _igvn.remove_dead_node(m);
          // fall through
        }
        else if (m != iff && split_up(m, region, iff)) {
          // fall through
        } else {
          continue;
        }
        // Something unpredictable changed.
        // Tell the iterators to refresh themselves, and rerun the loop.
        i = region->refresh_out_pos(i);
        j = region->refresh_out_pos(j);
        progress = true;
      }
    }
  }

  // Now we have no instructions in the block containing the IF.
  // Split the IF.
  Node *new_iff = split_thru_region( iff, region );

  // Replace both uses of 'new_iff' with Regions merging True/False
  // paths.  This makes 'new_iff' go dead.
  Node *old_false = NULL, *old_true = NULL;
  Node *new_false = NULL, *new_true = NULL;
  for (DUIterator_Last j2min, j2 = iff->last_outs(j2min); j2 >= j2min; --j2) {
    Node *ifp = iff->last_out(j2);
    assert( ifp->Opcode() == Op_IfFalse || ifp->Opcode() == Op_IfTrue, "" );
    ifp->set_req(0, new_iff);
    Node *ifpx = split_thru_region( ifp, region );

    // Replace 'If' projection of a Region with a Region of
    // 'If' projections.
    ifpx->set_req(0, ifpx);       // A TRUE RegionNode

    // Setup dominator info
    set_idom(ifpx, region_dom, dom_depth(region_dom) + 1);

    // Check for splitting loop tails
    if( get_loop(iff)->tail() == ifp )
      get_loop(iff)->_tail = ifpx;

    // Replace in the graph with lazy-update mechanism
    new_iff->set_req(0, new_iff); // hook self so it does not go dead
    lazy_replace(ifp, ifpx);
    new_iff->set_req(0, region);

    // Record bits for later xforms
    if( ifp->Opcode() == Op_IfFalse ) {
      old_false = ifp;
      new_false = ifpx;
    } else {
      old_true = ifp;
      new_true = ifpx;
    }
  }
  _igvn.remove_dead_node(new_iff);
  // Lazy replace IDOM info with the region's dominator
  lazy_replace(iff, region_dom);
  lazy_update(region, region_dom); // idom must be update before handle_uses
  region->set_req(0, NULL);        // Break the self-cycle. Required for lazy_update to work on region

  // Now make the original merge point go dead, by handling all its uses.
  small_cache region_cache;
  // Preload some control flow in region-cache
  region_cache.lru_insert( new_false, new_false );
  region_cache.lru_insert( new_true , new_true  );
  // Now handle all uses of the splitting block
  for (DUIterator k = region->outs(); region->has_out(k); k++) {
    Node* phi = region->out(k);
    if (!phi->in(0)) {         // Dead phi?  Remove it
      _igvn.remove_dead_node(phi);
    } else if (phi == region) { // Found the self-reference
      continue;                 // No roll-back of DUIterator
    } else if (phi->is_Phi()) { // Expected common case: Phi hanging off of Region
      assert(phi->in(0) == region, "Inconsistent graph");
      // Need a per-def cache.  Phi represents a def, so make a cache
      small_cache phi_cache;

      // Inspect all Phi uses to make the Phi go dead
      for (DUIterator_Last lmin, l = phi->last_outs(lmin); l >= lmin; --l) {
        Node* use = phi->last_out(l);
        // Compute the new DEF for this USE.  New DEF depends on the path
        // taken from the original DEF to the USE.  The new DEF may be some
        // collection of PHI's merging values from different paths.  The Phis
        // inserted depend only on the location of the USE.  We use a
        // 2-element cache to handle multiple uses from the same block.
        handle_use(use, phi, &phi_cache, region_dom, new_false, new_true, old_false, old_true);
      } // End of while phi has uses
      // Remove the dead Phi
      _igvn.remove_dead_node( phi );
    } else {
      assert(phi->in(0) == region, "Inconsistent graph");
      // Random memory op guarded by Region.  Compute new DEF for USE.
      handle_use(phi, region, &region_cache, region_dom, new_false, new_true, old_false, old_true);
    }
    // Every path above deletes a use of the region, except for the region
    // self-cycle (which is needed by handle_use calling find_use_block
    // calling get_ctrl calling get_ctrl_no_update looking for dead
    // regions).  So roll back the DUIterator innards.
    --k;
  } // End of while merge point has phis

  _igvn.remove_dead_node(region);

#ifndef PRODUCT
  if( VerifyLoopOptimizations ) verify();
#endif
}
