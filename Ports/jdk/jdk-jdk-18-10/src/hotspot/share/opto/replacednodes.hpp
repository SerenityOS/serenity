/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_REPLACEDNODES_HPP
#define SHARE_OPTO_REPLACEDNODES_HPP

#include "opto/connode.hpp"

// During parsing, when a node is "improved",
// GraphKit::replace_in_map() is called to update the current map so
// that the improved node is used from that point
// on. GraphKit::replace_in_map() doesn't operate on the callers maps
// and so some optimization opportunities may be lost. The
// ReplacedNodes class addresses that problem.
//
// A ReplacedNodes object is a list of pair of nodes. Every
// SafePointNode carries a ReplacedNodes object. Every time
// GraphKit::replace_in_map() is called, a new pair of nodes is pushed
// on the list of replaced nodes. When control flow paths merge, their
// replaced nodes are also merged. When parsing exits a method to
// return to a caller, the replaced nodes on the exit path are used to
// update the caller's map.
class ReplacedNodes {
 private:
  class ReplacedNode {
  private:
    Node* _initial;
    Node* _improved;
  public:
    ReplacedNode() : _initial(NULL), _improved(NULL) {}
    ReplacedNode(Node* initial, Node* improved) : _initial(initial), _improved(improved) {}
    Node* initial() const  { return _initial; }
    Node* improved() const { return _improved; }

    bool operator==(const ReplacedNode& other) {
      return _initial == other._initial && _improved == other._improved;
    }
  };
  GrowableArray<ReplacedNode>* _replaced_nodes;

  void allocate_if_necessary();
  bool has_node(const ReplacedNode& r) const;
  bool has_target_node(Node* n) const;

 public:
  ReplacedNodes()
    : _replaced_nodes(NULL) {}

  void clone();
  void record(Node* initial, Node* improved);
  void transfer_from(const ReplacedNodes& other, uint idx);
  void reset();
  void apply(Node* n, uint idx);
  void merge_with(const ReplacedNodes& other);
  bool is_empty() const;
  void dump(outputStream *st) const;
  void apply(Compile* C, Node* ctl);
};

#endif // SHARE_OPTO_REPLACEDNODES_HPP
