/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_VECTOR_HPP
#define SHARE_OPTO_VECTOR_HPP

#include "opto/node.hpp"
#include "opto/phaseX.hpp"
#include "opto/type.hpp"
#include "opto/vectornode.hpp"

class PhaseVector : public Phase {
 private:
  PhaseIterGVN& _igvn;

  void expand_vbox_nodes();
  void expand_vbox_node(VectorBoxNode* vec_box);
  Node* expand_vbox_node_helper(Node* vbox,
                                Node* vect,
                                const TypeInstPtr* box_type,
                                const TypeVect* vect_type);
  Node* expand_vbox_alloc_node(VectorBoxAllocateNode* vbox_alloc,
                               Node* value,
                               const TypeInstPtr* box_type,
                               const TypeVect* vect_type);
  void scalarize_vbox_nodes();
  void scalarize_vbox_node(VectorBoxNode* vec_box);
  void expand_vunbox_nodes();
  void expand_vunbox_node(VectorUnboxNode* vec_box);
  void eliminate_vbox_alloc_nodes();
  void eliminate_vbox_alloc_node(VectorBoxAllocateNode* vbox_alloc);
  void do_cleanup();
  void scalarize_vector_boxes();
  void expand_vector_boxes();

 public:
  PhaseVector(PhaseIterGVN& igvn) : Phase(Vector), _igvn(igvn) {}
  void optimize_vector_boxes();
};

#endif // SHARE_OPTO_VECTOR_HPP
