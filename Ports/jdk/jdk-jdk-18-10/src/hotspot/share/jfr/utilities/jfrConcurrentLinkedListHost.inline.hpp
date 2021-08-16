/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRCONCURRENTLINKEDLISTHOST_INLINE_HPP
#define SHARE_JFR_UTILITIES_JFRCONCURRENTLINKEDLISTHOST_INLINE_HPP

#include "jfr/utilities/jfrConcurrentLinkedListHost.hpp"

#include "jfr/utilities/jfrRelation.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "runtime/atomic.hpp"
#include "utilities/globalDefinitions.hpp"

/*
 * The removal marker (i.e. the excision bit) is represented by '( )' as part of state description comments:
 * "node --> next" becomes "(node) --> next", when node is logically deleted.
 */
template <typename Node>
inline Node* mark_for_removal(Node* node) {
  assert(node != NULL, "invariant");
  const Node* next = node->_next;
  assert(next != NULL, "invariant");
  Node* const unmasked_next = unmask(next);
  return next == unmasked_next && cas(&node->_next, unmasked_next, set_excision_bit(unmasked_next)) ? unmasked_next : NULL;
}

/*
 * The insertion marker (i.e. the insertion bit) is represented by '[ ]' as part of state description comments:
 * "node --> next" becomes "[node] --> next", in an attempt to convey the node as exlusively reserved.
 */
template <typename Node>
inline bool mark_for_insertion(Node* node, const Node* tail) {
  assert(node != NULL, "invariant");
  return node->_next == tail && cas(&node->_next, const_cast<Node*>(tail), set_insertion_bit(tail));
}

/*
 * Find a predecessor and successor node pair where successor covers predecessor (adjacency).
 */
template <typename Node, typename VersionHandle, template <typename> class SearchPolicy>
Node* find_adjacent(Node* head, const Node* tail, Node** predecessor, VersionHandle& version_handle, SearchPolicy<Node>& predicate) {
  assert(head != NULL, "invariant");
  assert(tail != NULL, "invariant");
  assert(head != tail, "invariant");
  Node* predecessor_next = NULL;
  while (true) {
    Node* current = head;
    version_handle->checkout();
    Node* next = Atomic::load_acquire(&current->_next);
    do {
      assert(next != NULL, "invariant");
      Node* const unmasked_next = unmask(next);
      // 1A: Locate the first node to keep as predecessor.
      if (!is_marked_for_removal(next)) {
        *predecessor = current;
        predecessor_next = unmasked_next;
      }
      // 1B: Locate the next node to keep as successor.
      current = unmasked_next;
      if (current == tail) break;
      next = current->_next;
    } while (predicate(current, next));
    // current represents the successor node from here on out.
    // 2: Check predecessor and successor node pair for adjacency.
    if (predecessor_next == current) {
      // Invariant: predecessor --> successor
      return current;
    }
    // 3: Successor does not (yet) cover predecessor.
    // Invariant: predecessor --> (logically excised nodes) --> successor
    // Physically excise one or more logically excised nodes in-between.
    if (cas(&(*predecessor)->_next, predecessor_next, current)) {
      // Invariant: predecessor --> successor
      return current;
    }
  }
}

template <typename Client, template <typename> class SearchPolicy, typename AllocPolicy>
JfrConcurrentLinkedListHost<Client, SearchPolicy, AllocPolicy>::JfrConcurrentLinkedListHost(Client* client) : _client(client) {}

template <typename Client, template <typename> class SearchPolicy, typename AllocPolicy>
bool JfrConcurrentLinkedListHost<Client, SearchPolicy, AllocPolicy>::initialize() {
  return true;
}

template <typename Client, template <typename> class SearchPolicy, typename AllocPolicy>
void JfrConcurrentLinkedListHost<Client, SearchPolicy, AllocPolicy>::insert_head(typename Client::Node* node,
                                                                                 typename Client::Node* head,
                                                                                 const typename Client::Node* tail) const {
  Node* predecessor;
  Node* successor;
  HeadNode<Node> predicate(node);
  VersionHandle version_handle = _client->get_version_handle();
  while (true) {
    // Find an adjacent predecessor and successor node pair.
    successor = find_adjacent<Node, VersionHandle, HeadNode>(head, tail, &predecessor, version_handle, predicate);
    // Invariant (adjacency): predecessor --> successor
    // Invariant (optional: key-based total order): predecessor->key() < key  && key <= successor->key().
    // We can now attempt to insert the new node in-between.
    node->_next = successor;
    if (cas(&predecessor->_next, successor, node)) {
      // Invariant: predecessor --> node --> successor
      // An insert to head is a benign modification and will not need to be committed to the version control system.
      return;
    }
  }
}

template <typename Client, template <typename> class SearchPolicy, typename AllocPolicy>
void JfrConcurrentLinkedListHost<Client, SearchPolicy, AllocPolicy>::insert_tail(typename Client::Node* node,
                                                                                 typename Client::Node* head,
                                                                                 typename Client::Node* last,
                                                                                 const typename Client::Node* tail) const {
  assert(node != NULL, "invariant");
  assert(head != NULL, "invariant");
  assert(last != NULL, "invarinat");
  assert(tail != NULL, "invariant");
  // Mark the new node to be inserted with the insertion marker already.
  node->_next = set_insertion_bit(const_cast<NodePtr>(tail));
  // Invariant: [node]--> tail
  assert(is_marked_for_insertion(node->_next), "invariant");
  NodePtr predecessor;
  LastNode<Node> predicate;
  VersionHandle version_handle = _client->get_version_handle();
  while (true) {
    // Find an adjacent predecessor and successor node pair, where the successor == tail
    const NodePtr successor = find_adjacent<Node, VersionHandle, LastNode>(last, tail, &predecessor, version_handle, predicate);
    assert(successor == tail, "invariant");
    // Invariant: predecessor --> successor
    // We first attempt to mark the predecessor node to signal our intent of performing an insertion.
    if (mark_for_insertion(predecessor, tail)) {
      break;
    }
  }
  // Predecessor node is claimed for insertion.
  // Invariant: [predecessor] --> tail
  assert(is_marked_for_insertion(predecessor->_next), "invariant");
  assert(predecessor != head, "invariant");
  if (Atomic::load_acquire(&last->_next) == predecessor) {
    /* Even after we store the new node into the last->_next field, there is no race
       because it is also marked with the insertion bit. */
    last->_next = node;
    // Invariant: last --> [node] --> tail
    OrderAccess::storestore();
    // Perform the link with the predecessor node, which by this store becomes visible for removal.
    predecessor->_next = node;
    // Invariant: predecessor --> [node] --> tail
  } else {
    assert(last == predecessor, "invariant");
    last->_next = node;
    // Invariant: last --> [node] --> tail
    OrderAccess::storestore();
    /* This implies the list is logically empty from the removal perspective.
       cas is not needed here because inserts must not come in from the head side
       concurrently with inserts from tail which are currently blocked by us.
       Invariant (logical): head --> tail. */
    head->_next = node;
    // Invariant: head --> [node] --> tail
  }
  OrderAccess::storestore();
  // Publish the inserted node by removing the insertion marker.
  node->_next = const_cast<NodePtr>(tail);
  // Invariant: last --> node --> tail (possibly also head --> node --> tail)
}

template <typename Client, template <typename> class SearchPolicy, typename AllocPolicy>
typename Client::Node* JfrConcurrentLinkedListHost<Client, SearchPolicy, AllocPolicy>::remove(typename Client::Node* head,
                                                                                              const typename Client::Node* tail,
                                                                                              typename Client::Node* last /* NULL */,
                                                                                              bool insert_is_head /* true */) {
  assert(head != NULL, "invariant");
  assert(tail != NULL, "invariant");
  assert(head != tail, "invariant");
  NodePtr predecessor;
  NodePtr successor;
  NodePtr successor_next;
  SearchPolicy<Node> predicate;
  VersionHandle version_handle = _client->get_version_handle();
  while (true) {
    // Find an adjacent predecessor and successor node pair.
    successor = find_adjacent<Node, VersionHandle, SearchPolicy>(head, tail, &predecessor, version_handle, predicate);
    if (successor == tail) {
      return NULL;
    }
    // Invariant: predecessor --> successor
    // Invariant (optional: key-based total order): predecessor->key() < key  && key <= successor->key()
    // It is the successor node that is to be removed.
    // We first attempt to reserve (logically excise) the successor node.
    successor_next = mark_for_removal(successor);
    if (successor_next != NULL) {
      break;
    }
  }
  // Invariant: predecessor --> (successor) --> successor_next
  // Successor node now logically excised.
  assert(is_marked_for_removal(successor->_next), "invariant");
  // Now attempt to physically excise the successor node.
  // If the cas fails, we can optimize for the slow path if we know we are not performing
  // insertions from the head. Then a failed cas results not from a new node being inserted,
  // but only because another thread excised us already.
  if (!cas(&predecessor->_next, successor, successor_next) && insert_is_head) {
    // Physically excise using slow path, can be completed asynchronously by other threads.
    Identity<Node> excise(successor);
    find_adjacent<Node, VersionHandle, Identity>(head, tail, &predecessor, version_handle, excise);
  }
  if (last != NULL && Atomic::load_acquire(&last->_next) == successor) {
    guarantee(!insert_is_head, "invariant");
    guarantee(successor_next == tail, "invariant");
    LastNode<Node> excise;
    find_adjacent<Node, VersionHandle, LastNode>(last, tail, &predecessor, version_handle, excise);
    // Invariant: successor excised from last list
  }
  // Commit the modification back to the version control system.
  // It blocks until all checkouts for versions earlier than the commit have been released.
  version_handle->commit();
  // At this point we know there can be no references onto the excised node. It is safe, enjoy it.
  return successor;
}

template <typename Client, template <typename> class SearchPolicy, typename AllocPolicy>
bool JfrConcurrentLinkedListHost<Client, SearchPolicy, AllocPolicy>::in_list(const typename Client::Node* node,
                                                                             typename Client::Node* head,
                                                                             const typename Client::Node* tail) const {
  assert(head != NULL, "invariant");
  assert(tail != NULL, "invariant");
  assert(head != tail, "invariant");
  VersionHandle version_handle = _client->get_version_handle();
  const Node* current = head;
  version_handle->checkout();
  const Node* next = Atomic::load_acquire(&current->_next);
  while (true) {
    if (!is_marked_for_removal(next)) {
      if (current == node) {
        return true;
      }
    }
    current = unmask(next);
    if (current == tail) break;
    next = current->_next;
  }
  return false;
}

template <typename Client, template <typename> class SearchPolicy, typename AllocPolicy>
template <typename Callback>
inline void JfrConcurrentLinkedListHost<Client, SearchPolicy, AllocPolicy>::iterate(typename Client::Node* head,
                                                                                    const typename Client::Node* tail,
                                                                                    Callback& cb) {
  assert(head != NULL, "invariant");
  assert(tail != NULL, "invariant");
  assert(head != tail, "invariant");
  VersionHandle version_handle = _client->get_version_handle();
  NodePtr current = head;
  version_handle->checkout();
  NodePtr next = Atomic::load_acquire(&current->_next);
  while (true) {
    if (!is_marked_for_removal(next)) {
      if (!cb.process(current)) {
        return;
      }
    }
    current = unmask(next);
    if (current == tail) break;
    next = current->_next;
  }
}

#endif // SHARE_JFR_UTILITIES_JFRCONCURRENTLINKEDLISTHOST_INLINE_HPP
