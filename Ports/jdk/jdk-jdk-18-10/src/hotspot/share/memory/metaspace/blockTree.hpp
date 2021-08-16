/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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

#ifndef SHARE_MEMORY_METASPACE_BLOCKTREE_HPP
#define SHARE_MEMORY_METASPACE_BLOCKTREE_HPP

#include "memory/allocation.hpp"
#include "memory/metaspace/chunklevel.hpp"
#include "memory/metaspace/counters.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

namespace metaspace {

// BlockTree is a rather simple binary search tree. It is used to
//  manage small to medium free memory blocks (see class FreeBlocks).
//
// There is no separation between payload (managed blocks) and nodes: the
//  memory blocks themselves are the nodes, with the block size being the key.
//
// We store node pointer information in these blocks when storing them. That
//  imposes a minimum size to the managed memory blocks.
//  See get_raw_word_size_for_requested_word_size() (msCommon.hpp).
//
// We want to manage many memory blocks of the same size, but we want
//  to prevent the tree from blowing up and degenerating into a list. Therefore
//  there is only one node for each unique block size; subsequent blocks of the
//  same size are stacked below that first node:
//
//                   +-----+
//                   | 100 |
//                   +-----+
//                  /       \
//           +-----+
//           | 80  |
//           +-----+
//          /   |   \
//         / +-----+ \
//  +-----+  | 80  |  +-----+
//  | 70  |  +-----+  | 85  |
//  +-----+     |     +-----+
//           +-----+
//           | 80  |
//           +-----+
//
//
// Todo: This tree is unbalanced. It would be a good fit for a red-black tree.
//  In order to make this a red-black tree, we need an algorithm which can deal
//  with nodes which are their own payload (most red-black tree implementations
//  swap payloads of their nodes at some point, see e.g. j.u.TreeSet).
// A good example is the Linux kernel rbtree, which is a clean, easy-to-read
//  implementation.

class BlockTree: public CHeapObj<mtMetaspace> {

  struct Node {

    static const intptr_t _canary_value =
        NOT_LP64(0x4e4f4445) LP64_ONLY(0x4e4f44454e4f4445ULL); // "NODE" resp "NODENODE"

    // Note: we afford us the luxury of an always-there canary value.
    //  The space for that is there (these nodes are only used to manage larger blocks,
    //  see FreeBlocks::MaxSmallBlocksWordSize).
    //  It is initialized in debug and release, but only automatically tested
    //  in debug.
    const intptr_t _canary;

    // Normal tree node stuff...
    //  (Note: all null if this is a stacked node)
    Node* _parent;
    Node* _left;
    Node* _right;

    // Blocks with the same size are put in a list with this node as head.
    Node* _next;

    // Word size of node. Note that size cannot be larger than max metaspace size,
    // so this could be very well a 32bit value (in case we ever make this a balancing
    // tree and need additional space for weighting information).
    const size_t _word_size;

    Node(size_t word_size) :
      _canary(_canary_value),
      _parent(NULL),
      _left(NULL),
      _right(NULL),
      _next(NULL),
      _word_size(word_size)
    {}

#ifdef ASSERT
    bool valid() const {
      return _canary == _canary_value &&
        _word_size >= sizeof(Node) &&
        _word_size < chunklevel::MAX_CHUNK_WORD_SIZE;
    }
#endif
  };

  // Needed for verify() and print_tree()
  struct walkinfo;

#ifdef ASSERT
  // Run a quick check on a node; upon suspicion dive into a full tree check.
  void check_node(const Node* n) const { if (!n->valid()) verify(); }
#endif

public:

  // Minimum word size a block has to be to be added to this structure (note ceil division).
  const static size_t MinWordSize =
      (sizeof(Node) + sizeof(MetaWord) - 1) / sizeof(MetaWord);

private:

  Node* _root;

  MemRangeCounter _counter;

  // Given a node n, add it to the list starting at head
  static void add_to_list(Node* n, Node* head) {
    assert(head->_word_size == n->_word_size, "sanity");
    n->_next = head->_next;
    head->_next = n;
    DEBUG_ONLY(n->_left = n->_right = n->_parent = NULL;)
  }

  // Given a node list starting at head, remove one of the follow up nodes from
  //  that list and return it. The head node gets not modified and remains in the
  //  tree.
  // List must contain at least one other node.
  static Node* remove_from_list(Node* head) {
    assert(head->_next != NULL, "sanity");
    Node* n = head->_next;
    head->_next = n->_next;
    return n;
  }

  // Given a node c and a node p, wire up c as left child of p.
  static void set_left_child(Node* p, Node* c) {
    p->_left = c;
    if (c != NULL) {
      assert(c->_word_size < p->_word_size, "sanity");
      c->_parent = p;
    }
  }

  // Given a node c and a node p, wire up c as right child of p.
  static void set_right_child(Node* p, Node* c) {
    p->_right = c;
    if (c != NULL) {
      assert(c->_word_size > p->_word_size, "sanity");
      c->_parent = p;
    }
  }

  // Given a node n, return its successor in the tree
  // (node with the next-larger size).
  static Node* successor(Node* n) {
    Node* succ = NULL;
    if (n->_right != NULL) {
      // If there is a right child, search the left-most
      // child of that child.
      succ = n->_right;
      while (succ->_left != NULL) {
        succ = succ->_left;
      }
    } else {
      succ = n->_parent;
      Node* n2 = n;
      // As long as I am the right child of my parent, search upward
      while (succ != NULL && n2 == succ->_right) {
        n2 = succ;
        succ = succ->_parent;
      }
    }
    return succ;
  }

  // Given a node, replace it with a replacement node as a child for its parent.
  // If the node is root and has no parent, sets it as root.
  void replace_node_in_parent(Node* child, Node* replace) {
    Node* parent = child->_parent;
    if (parent != NULL) {
      if (parent->_left == child) { // Child is left child
        set_left_child(parent, replace);
      } else {
        set_right_child(parent, replace);
      }
    } else {
      assert(child == _root, "must be root");
      _root = replace;
      if (replace != NULL) {
        replace->_parent = NULL;
      }
    }
    return;
  }

  // Given a node n and an insertion point, insert n under insertion point.
  void insert(Node* insertion_point, Node* n) {
    assert(n->_parent == NULL, "Sanity");
    for (;;) {
      DEBUG_ONLY(check_node(insertion_point);)
      if (n->_word_size == insertion_point->_word_size) {
        add_to_list(n, insertion_point); // parent stays NULL in this case.
        break;
      } else if (n->_word_size > insertion_point->_word_size) {
        if (insertion_point->_right == NULL) {
          set_right_child(insertion_point, n);
          break;
        } else {
          insertion_point = insertion_point->_right;
        }
      } else {
        if (insertion_point->_left == NULL) {
          set_left_child(insertion_point, n);
          break;
        } else {
          insertion_point = insertion_point->_left;
        }
      }
    }
  }

  // Given a node and a wish size, search this node and all children for
  // the node closest (equal or larger sized) to the size s.
  Node* find_closest_fit(Node* n, size_t s) {
    Node* best_match = NULL;
    while (n != NULL) {
      DEBUG_ONLY(check_node(n);)
      if (n->_word_size >= s) {
        best_match = n;
        if (n->_word_size == s) {
          break; // perfect match or max depth reached
        }
        n = n->_left;
      } else {
        n = n->_right;
      }
    }
    return best_match;
  }

  // Given a wish size, search the whole tree for a
  // node closest (equal or larger sized) to the size s.
  Node* find_closest_fit(size_t s) {
    if (_root != NULL) {
      return find_closest_fit(_root, s);
    }
    return NULL;
  }

  // Given a node n, remove it from the tree and repair tree.
  void remove_node_from_tree(Node* n) {
    assert(n->_next == NULL, "do not delete a node which has a non-empty list");

    if (n->_left == NULL && n->_right == NULL) {
      replace_node_in_parent(n, NULL);

    } else if (n->_left == NULL && n->_right != NULL) {
      replace_node_in_parent(n, n->_right);

    } else if (n->_left != NULL && n->_right == NULL) {
      replace_node_in_parent(n, n->_left);

    } else {
      // Node has two children.

      // 1) Find direct successor (the next larger node).
      Node* succ = successor(n);

      // There has to be a successor since n->right was != NULL...
      assert(succ != NULL, "must be");

      // ... and it should not have a left child since successor
      //     is supposed to be the next larger node, so it must be the mostleft node
      //     in the sub tree rooted at n->right
      assert(succ->_left == NULL, "must be");
      assert(succ->_word_size > n->_word_size, "sanity");

      Node* successor_parent = succ->_parent;
      Node* successor_right_child = succ->_right;

      // Remove successor from its parent.
      if (successor_parent == n) {

        // special case: successor is a direct child of n. Has to be the right child then.
        assert(n->_right == succ, "sanity");

        // Just replace n with this successor.
        replace_node_in_parent(n, succ);

        // Take over n's old left child, too.
        // We keep the successor's right child.
        set_left_child(succ, n->_left);
      } else {
        // If the successors parent is not n, we are deeper in the tree,
        //  the successor has to be the left child of its parent.
        assert(successor_parent->_left == succ, "sanity");

        // The right child of the successor (if there was one) replaces
        //  the successor at its parent's left child.
        set_left_child(successor_parent, succ->_right);

        // and the successor replaces n at its parent
        replace_node_in_parent(n, succ);

        // and takes over n's old children
        set_left_child(succ, n->_left);
        set_right_child(succ, n->_right);
      }
    }
  }

#ifdef ASSERT
  void zap_range(MetaWord* p, size_t word_size);
  // Helper for verify()
  void verify_node_pointer(const Node* n) const;
#endif // ASSERT

public:

  BlockTree() : _root(NULL) {}

  // Add a memory block to the tree. Its content will be overwritten.
  void add_block(MetaWord* p, size_t word_size) {
    DEBUG_ONLY(zap_range(p, word_size));
    assert(word_size >= MinWordSize, "invalid block size " SIZE_FORMAT, word_size);
    Node* n = new(p) Node(word_size);
    if (_root == NULL) {
      _root = n;
    } else {
      insert(_root, n);
    }
    _counter.add(word_size);
  }

  // Given a word_size, search and return the smallest block that is equal or
  //  larger than that size. Upon return, *p_real_word_size contains the actual
  //  block size.
  MetaWord* remove_block(size_t word_size, size_t* p_real_word_size) {
    assert(word_size >= MinWordSize, "invalid block size " SIZE_FORMAT, word_size);

    Node* n = find_closest_fit(word_size);

    if (n != NULL) {
      DEBUG_ONLY(check_node(n);)
      assert(n->_word_size >= word_size, "sanity");

      if (n->_next != NULL) {
        // If the node is head of a chain of same sized nodes, we leave it alone
        //  and instead remove one of the follow up nodes (which is simpler than
        //  removing the chain head node and then having to graft the follow up
        //  node into its place in the tree).
        n = remove_from_list(n);
      } else {
        remove_node_from_tree(n);
      }

      MetaWord* p = (MetaWord*)n;
      *p_real_word_size = n->_word_size;

      _counter.sub(n->_word_size);

      DEBUG_ONLY(zap_range(p, n->_word_size));
      return p;
    }
    return NULL;
  }

  // Returns number of blocks in this structure
  unsigned count() const { return _counter.count(); }

  // Returns total size, in words, of all elements.
  size_t total_size() const { return _counter.total_size(); }

  bool is_empty() const { return _root == NULL; }

  DEBUG_ONLY(void print_tree(outputStream* st) const;)
  DEBUG_ONLY(void verify() const;)
};

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_BLOCKTREE_HPP
