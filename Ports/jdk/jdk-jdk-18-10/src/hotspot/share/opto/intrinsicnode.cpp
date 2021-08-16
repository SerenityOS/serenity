/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "opto/intrinsicnode.hpp"
#include "opto/memnode.hpp"
#include "opto/phaseX.hpp"

//=============================================================================
// Do not match memory edge.
uint StrIntrinsicNode::match_edge(uint idx) const {
  return idx == 2 || idx == 3;
}

//------------------------------Ideal------------------------------------------
// Return a node which is more "ideal" than the current node.  Strip out
// control copies
Node* StrIntrinsicNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  if (remove_dead_region(phase, can_reshape)) return this;
  // Don't bother trying to transform a dead node
  if (in(0) && in(0)->is_top())  return NULL;

  if (can_reshape) {
    Node* mem = phase->transform(in(MemNode::Memory));
    // If transformed to a MergeMem, get the desired slice
    uint alias_idx = phase->C->get_alias_index(adr_type());
    mem = mem->is_MergeMem() ? mem->as_MergeMem()->memory_at(alias_idx) : mem;
    if (mem != in(MemNode::Memory)) {
      set_req_X(MemNode::Memory, mem, phase);
      return this;
    }
  }
  return NULL;
}

//------------------------------Value------------------------------------------
const Type* StrIntrinsicNode::Value(PhaseGVN* phase) const {
  if (in(0) && phase->type(in(0)) == Type::TOP) return Type::TOP;
  return bottom_type();
}

uint StrIntrinsicNode::size_of() const { return sizeof(*this); }

//=============================================================================
//------------------------------Ideal------------------------------------------
// Return a node which is more "ideal" than the current node.  Strip out
// control copies
Node* StrCompressedCopyNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  return remove_dead_region(phase, can_reshape) ? this : NULL;
}

//=============================================================================
//------------------------------Ideal------------------------------------------
// Return a node which is more "ideal" than the current node.  Strip out
// control copies
Node* StrInflatedCopyNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  return remove_dead_region(phase, can_reshape) ? this : NULL;
}

//=============================================================================
//------------------------------match_edge-------------------------------------
// Do not match memory edge
uint EncodeISOArrayNode::match_edge(uint idx) const {
  return idx == 2 || idx == 3; // EncodeISOArray src (Binary dst len)
}

//------------------------------Ideal------------------------------------------
// Return a node which is more "ideal" than the current node.  Strip out
// control copies
Node* EncodeISOArrayNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  return remove_dead_region(phase, can_reshape) ? this : NULL;
}

//------------------------------Value------------------------------------------
const Type* EncodeISOArrayNode::Value(PhaseGVN* phase) const {
  if (in(0) && phase->type(in(0)) == Type::TOP) return Type::TOP;
  return bottom_type();
}

//------------------------------CopySign-----------------------------------------
CopySignDNode* CopySignDNode::make(PhaseGVN& gvn, Node* in1, Node* in2) {
  return new CopySignDNode(in1, in2, gvn.makecon(TypeD::ZERO));
}

//------------------------------Signum-------------------------------------------
SignumDNode* SignumDNode::make(PhaseGVN& gvn, Node* in) {
  return new SignumDNode(in, gvn.makecon(TypeD::ZERO), gvn.makecon(TypeD::ONE));
}

SignumFNode* SignumFNode::make(PhaseGVN& gvn, Node* in) {
  return new SignumFNode(in, gvn.makecon(TypeF::ZERO), gvn.makecon(TypeF::ONE));
}

