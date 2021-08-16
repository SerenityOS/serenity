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
#include "memory/allocation.inline.hpp"
#include "opto/callnode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/phaseX.hpp"
#include "opto/regmask.hpp"
#include "opto/rootnode.hpp"
#include "opto/subnode.hpp"
#include "opto/type.hpp"

//------------------------------Ideal------------------------------------------
// Remove dead inputs
Node *RootNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  bool modified = false;
  for( uint i = 1; i < req(); i++ ) { // For all inputs
    // Check for and remove dead inputs
    if( phase->type(in(i)) == Type::TOP ) {
      del_req(i--);             // Delete TOP inputs
      modified = true;
    }
  }

  // I used to do tail-splitting in the Ideal graph here, but it does not
  // work.  The tail-splitting forces values live into the Return to be
  // ready at a point which dominates the split returns.  This forces Stores
  // to be hoisted high.  The "proper" fix would be to split Stores down
  // each path, but this makes the split unprofitable.  If we want to do this
  // optimization, it needs to be done after allocation so we can count all
  // the instructions needing to be cloned in the cost metric.

  // There used to be a spoof here for caffeine marks which completely
  // eliminated very simple self-recursion recursions, but it's not worth it.
  // Deep inlining of self-calls gets nearly all of the same benefits.
  // If we want to get the rest of the win later, we should pattern match
  // simple recursive call trees to closed-form solutions.

  return modified ? this : NULL;
}

//=============================================================================
HaltNode::HaltNode(Node* ctrl, Node* frameptr, const char* halt_reason, bool reachable)
                        : Node(TypeFunc::Parms), _halt_reason(halt_reason), _reachable(reachable) {
  init_class_id(Class_Halt);
  Node* top = Compile::current()->top();
  init_req(TypeFunc::Control,  ctrl        );
  init_req(TypeFunc::I_O,      top);
  init_req(TypeFunc::Memory,   top);
  init_req(TypeFunc::FramePtr, frameptr    );
  init_req(TypeFunc::ReturnAdr,top);
}

const Type *HaltNode::bottom_type() const { return Type::BOTTOM; }
uint HaltNode::size_of() const { return sizeof(*this); }

//------------------------------Ideal------------------------------------------
Node *HaltNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  return remove_dead_region(phase, can_reshape) ? this : NULL;
}

//------------------------------Value------------------------------------------
const Type* HaltNode::Value(PhaseGVN* phase) const {
  return ( phase->type(in(TypeFunc::Control)) == Type::TOP)
    ? Type::TOP
    : Type::BOTTOM;
}

const RegMask &HaltNode::out_RegMask() const {
  return RegMask::Empty;
}

#ifndef PRODUCT
//-----------------------------related-----------------------------------------
// Include all control inputs in the related set, and also the input data
// boundary. In compact mode, include all inputs till level 2. Also include
// all outputs at level 1.
void HaltNode::related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const {
  if (compact) {
    this->collect_nodes(in_rel, 2, false, false);
  } else {
    this->collect_nodes_in_all_ctrl(in_rel, true);
  }
  this->collect_nodes(out_rel, -1, false, false);
}
#endif
