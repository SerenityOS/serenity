/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "opto/locknode.hpp"
#include "opto/parse.hpp"
#include "opto/rootnode.hpp"
#include "opto/runtime.hpp"

//=============================================================================
const RegMask &BoxLockNode::in_RegMask(uint i) const {
  return _inmask;
}

const RegMask &BoxLockNode::out_RegMask() const {
  return *Matcher::idealreg2regmask[Op_RegP];
}

uint BoxLockNode::size_of() const { return sizeof(*this); }

BoxLockNode::BoxLockNode( int slot ) : Node( Compile::current()->root() ),
                                       _slot(slot), _is_eliminated(false) {
  init_class_id(Class_BoxLock);
  init_flags(Flag_rematerialize);
  OptoReg::Name reg = OptoReg::stack2reg(_slot);
  _inmask.Insert(reg);
}

//-----------------------------hash--------------------------------------------
uint BoxLockNode::hash() const {
  if (EliminateNestedLocks)
    return NO_HASH; // Each locked region has own BoxLock node
  return Node::hash() + _slot + (_is_eliminated ? Compile::current()->fixed_slots() : 0);
}

//------------------------------cmp--------------------------------------------
bool BoxLockNode::cmp( const Node &n ) const {
  if (EliminateNestedLocks)
    return (&n == this); // Always fail except on self
  const BoxLockNode &bn = (const BoxLockNode &)n;
  return bn._slot == _slot && bn._is_eliminated == _is_eliminated;
}

BoxLockNode* BoxLockNode::box_node(Node* box) {
  // Chase down the BoxNode after RA which may spill box nodes.
  while (!box->is_BoxLock()) {
    //    if (box_node->is_SpillCopy()) {
    //      Node *m = box_node->in(1);
    //      if (m->is_Mach() && m->as_Mach()->ideal_Opcode() == Op_StoreP) {
    //        box_node = m->in(m->as_Mach()->operand_index(2));
    //        continue;
    //      }
    //    }
    assert(box->is_SpillCopy() || box->is_Phi(), "Bad spill of Lock.");
    // Only BoxLock nodes with the same stack slot are merged.
    // So it is enough to trace one path to find the slot value.
    box = box->in(1);
  }
  return box->as_BoxLock();
}

OptoReg::Name BoxLockNode::reg(Node* box) {
  return box_node(box)->in_RegMask(0).find_first_elem();
}

// Is BoxLock node used for one simple lock region (same box and obj)?
bool BoxLockNode::is_simple_lock_region(LockNode** unique_lock, Node* obj, Node** bad_lock) {
  LockNode* lock = NULL;
  bool has_one_lock = false;
  for (uint i = 0; i < this->outcnt(); i++) {
    Node* n = this->raw_out(i);
    assert(!n->is_Phi(), "should not merge BoxLock nodes");
    if (n->is_AbstractLock()) {
      AbstractLockNode* alock = n->as_AbstractLock();
      // Check lock's box since box could be referenced by Lock's debug info.
      if (alock->box_node() == this) {
        if (alock->obj_node()->eqv_uncast(obj)) {
          if ((unique_lock != NULL) && alock->is_Lock()) {
            if (lock == NULL) {
              lock = alock->as_Lock();
              has_one_lock = true;
            } else if (lock != alock->as_Lock()) {
              has_one_lock = false;
              if (bad_lock != NULL) {
                *bad_lock = alock;
              }
            }
          }
        } else {
          if (bad_lock != NULL) {
            *bad_lock = alock;
          }
          return false; // Different objects
        }
      }
    }
  }
#ifdef ASSERT
  // Verify that FastLock and Safepoint reference only this lock region.
  for (uint i = 0; i < this->outcnt(); i++) {
    Node* n = this->raw_out(i);
    if (n->is_FastLock()) {
      FastLockNode* flock = n->as_FastLock();
      assert((flock->box_node() == this) && flock->obj_node()->eqv_uncast(obj),"");
    }
    // Don't check monitor info in safepoints since the referenced object could
    // be different from the locked object. It could be Phi node of different
    // cast nodes which point to this locked object.
    // We assume that no other objects could be referenced in monitor info
    // associated with this BoxLock node because all associated locks and
    // unlocks are reference only this one object.
  }
#endif
  if (unique_lock != NULL && has_one_lock) {
    *unique_lock = lock;
  }
  return true;
}

//=============================================================================
//-----------------------------hash--------------------------------------------
uint FastLockNode::hash() const { return NO_HASH; }

uint FastLockNode::size_of() const { return sizeof(*this); }

//------------------------------cmp--------------------------------------------
bool FastLockNode::cmp( const Node &n ) const {
  return (&n == this);                // Always fail except on self
}

//=============================================================================
//-----------------------------hash--------------------------------------------
uint FastUnlockNode::hash() const { return NO_HASH; }

//------------------------------cmp--------------------------------------------
bool FastUnlockNode::cmp( const Node &n ) const {
  return (&n == this);                // Always fail except on self
}

void FastLockNode::create_rtm_lock_counter(JVMState* state) {
#if INCLUDE_RTM_OPT
  Compile* C = Compile::current();
  if (C->profile_rtm() || (PrintPreciseRTMLockingStatistics && C->use_rtm())) {
    RTMLockingNamedCounter* rlnc = (RTMLockingNamedCounter*)
           OptoRuntime::new_named_counter(state, NamedCounter::RTMLockingCounter);
    _rtm_counters = rlnc->counters();
    if (UseRTMForStackLocks) {
      rlnc = (RTMLockingNamedCounter*)
           OptoRuntime::new_named_counter(state, NamedCounter::RTMLockingCounter);
      _stack_rtm_counters = rlnc->counters();
    }
  }
#endif
}

//=============================================================================
//------------------------------do_monitor_enter-------------------------------
void Parse::do_monitor_enter() {
  kill_dead_locals();

  // Null check; get casted pointer.
  Node* obj = null_check(peek());
  // Check for locking null object
  if (stopped()) return;

  // the monitor object is not part of debug info expression stack
  pop();

  // Insert a FastLockNode which takes as arguments the current thread pointer,
  // the obj pointer & the address of the stack slot pair used for the lock.
  shared_lock(obj);
}

//------------------------------do_monitor_exit--------------------------------
void Parse::do_monitor_exit() {
  kill_dead_locals();

  pop();                        // Pop oop to unlock
  // Because monitors are guaranteed paired (else we bail out), we know
  // the matching Lock for this Unlock.  Hence we know there is no need
  // for a null check on Unlock.
  shared_unlock(map()->peek_monitor_box(), map()->peek_monitor_obj());
}
