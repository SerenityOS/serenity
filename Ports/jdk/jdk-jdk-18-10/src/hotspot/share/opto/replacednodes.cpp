/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/resourceArea.hpp"
#include "opto/cfgnode.hpp"
#include "opto/phaseX.hpp"
#include "opto/replacednodes.hpp"

void ReplacedNodes::allocate_if_necessary() {
  if (_replaced_nodes == NULL) {
    _replaced_nodes = new GrowableArray<ReplacedNode>();
  }
}

bool ReplacedNodes::is_empty() const {
  return _replaced_nodes == NULL || _replaced_nodes->length() == 0;
}

bool ReplacedNodes::has_node(const ReplacedNode& r) const {
  return _replaced_nodes->find(r) != -1;
}

bool ReplacedNodes::has_target_node(Node* n) const {
  for (int i = 0; i < _replaced_nodes->length(); i++) {
    if (_replaced_nodes->at(i).improved() == n) {
      return true;
    }
  }
  return false;
}

// Record replaced node if not seen before
void ReplacedNodes::record(Node* initial, Node* improved) {
  allocate_if_necessary();
  ReplacedNode r(initial, improved);
  if (!has_node(r)) {
    _replaced_nodes->push(r);
  }
}

// Copy replaced nodes from one map to another. idx is used to
// identify nodes that are too new to be of interest in the target
// node list.
void ReplacedNodes::transfer_from(const ReplacedNodes& other, uint idx) {
  if (other.is_empty()) {
    return;
  }
  allocate_if_necessary();
  for (int i = 0; i < other._replaced_nodes->length(); i++) {
    ReplacedNode replaced = other._replaced_nodes->at(i);
    // Only transfer the nodes that can actually be useful
    if (!has_node(replaced) && (replaced.initial()->_idx < idx || has_target_node(replaced.initial()))) {
      _replaced_nodes->push(replaced);
    }
  }
}

void ReplacedNodes::clone() {
  if (_replaced_nodes != NULL) {
    GrowableArray<ReplacedNode>* replaced_nodes_clone = new GrowableArray<ReplacedNode>();
    replaced_nodes_clone->appendAll(_replaced_nodes);
    _replaced_nodes = replaced_nodes_clone;
  }
}

void ReplacedNodes::reset() {
  if (_replaced_nodes != NULL) {
    _replaced_nodes->clear();
  }
}

// Perfom node replacement (used when returning to caller)
void ReplacedNodes::apply(Node* n, uint idx) {
  if (is_empty()) {
    return;
  }
  for (int i = 0; i < _replaced_nodes->length(); i++) {
    ReplacedNode replaced = _replaced_nodes->at(i);
    // Only apply if improved node was created in a callee to avoid
    // issues with irreducible loops in the caller
    if (replaced.improved()->_idx >= idx) {
      n->replace_edge(replaced.initial(), replaced.improved());
    }
  }
}

static void enqueue_use(Node* n, Node* use, Unique_Node_List& work) {
  if (use->is_Phi()) {
    Node* r = use->in(0);
    assert(r->is_Region(), "Phi should have Region");
    for (uint i = 1; i < use->req(); i++) {
      if (use->in(i) == n) {
        work.push(r->in(i));
      }
    }
  } else {
    work.push(use);
  }
}

// Perform node replacement following late inlining.
void ReplacedNodes::apply(Compile* C, Node* ctl) {
  // ctl is the control on exit of the method that was late inlined
  if (is_empty()) {
    return;
  }
  for (int i = 0; i < _replaced_nodes->length(); i++) {
    ReplacedNode replaced = _replaced_nodes->at(i);
    Node* initial = replaced.initial();
    Node* improved = replaced.improved();
    assert (ctl != NULL && !ctl->is_top(), "replaced node should have actual control");

    ResourceMark rm;
    Unique_Node_List work;
    // Go over all the uses of the node that is considered for replacement...
    for (DUIterator j = initial->outs(); initial->has_out(j); j++) {
      Node* use = initial->out(j);

      if (use == improved || use->outcnt() == 0) {
        continue;
      }
      work.clear();
      enqueue_use(initial, use, work);
      bool replace = true;
      // Check that this use is dominated by ctl. Go ahead with the replacement if it is.
      while (work.size() != 0 && replace) {
        Node* n = work.pop();
        if (use->outcnt() == 0) {
          continue;
        }
        if (n->is_CFG() || (n->in(0) != NULL && !n->in(0)->is_top())) {
          // Skip projections, since some of the multi nodes aren't CFG (e.g., LoadStore and SCMemProj).
          if (n->is_Proj()) {
            n = n->in(0);
          }
          if (!n->is_CFG()) {
            n = n->in(0);
          }
          assert(n->is_CFG(), "should be CFG now");
          int depth = 0;
          while(n != ctl) {
            n = IfNode::up_one_dom(n);
            depth++;
            // limit search depth
            if (depth >= 100 || n == NULL) {
              replace = false;
              break;
            }
          }
        } else {
          for (DUIterator k = n->outs(); n->has_out(k); k++) {
            enqueue_use(n, n->out(k), work);
          }
        }
      }
      if (replace) {
        bool is_in_table = C->initial_gvn()->hash_delete(use);
        int replaced = use->replace_edge(initial, improved);
        if (is_in_table) {
          C->initial_gvn()->hash_find_insert(use);
        }
        C->record_for_igvn(use);

        assert(replaced > 0, "inconsistent");
        --j;
      }
    }
  }
}

void ReplacedNodes::dump(outputStream *st) const {
  if (!is_empty()) {
    st->print("replaced nodes: ");
    for (int i = 0; i < _replaced_nodes->length(); i++) {
      st->print("%d->%d", _replaced_nodes->at(i).initial()->_idx, _replaced_nodes->at(i).improved()->_idx);
      if (i < _replaced_nodes->length()-1) {
        st->print(",");
      }
    }
  }
}

// Merge 2 list of replaced node at a point where control flow paths merge
void ReplacedNodes::merge_with(const ReplacedNodes& other) {
  if (is_empty()) {
    return;
  }
  if (other.is_empty()) {
    reset();
    return;
  }
  int shift = 0;
  int len = _replaced_nodes->length();
  for (int i = 0; i < len; i++) {
    if (!other.has_node(_replaced_nodes->at(i))) {
      shift++;
    } else if (shift > 0) {
      _replaced_nodes->at_put(i-shift, _replaced_nodes->at(i));
    }
  }
  if (shift > 0) {
    _replaced_nodes->trunc_to(len - shift);
  }
}
