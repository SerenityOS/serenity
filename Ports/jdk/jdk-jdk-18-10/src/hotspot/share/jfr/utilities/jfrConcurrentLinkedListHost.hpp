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

#ifndef SHARE_JFR_UTILITIES_JFRCONCURRENTLINKEDLISTHOST_HPP
#define SHARE_JFR_UTILITIES_JFRCONCURRENTLINKEDLISTHOST_HPP

#include "jfr/utilities/jfrAllocation.hpp"

/*
* This implementation is a derivation from Harris
* https://www.cl.cam.ac.uk/research/srg/netos/papers/2001-caslists.pdf
*
* A concurrent LIFO structure can be built using the pair:
*
*   insert_head() and remove()
*
* The LIFO algorithm is non-blocking, more specifically wait-free.
* When combined with a system for safe memory reclamation, where a thread will require
* to know if other threads are possibly reading the memory that is to be reclaimed (more below),
* a potential wait point is introduced, so technically, we are no longer wait-free.
* The combination is still lock-free, but since it is no longer pure non-blocking,
* we instead say the solution is concurrent.
*
* It is also possible to build a FIFO structure using the pair:
*
*   insert_tail() and remove()
*
* To allow FIFO, the solution extends support to mark, or reserve a node, not only as part of deletions
* as with the LIFO case, but also, to enable tail insertions.
*
* Compared to the LIFO algorithm, the FIFO algorithm is not non-blocking, because inserts to the tail block,
* making it not lock-free. remove() is lock-free up until the last node in the list. In practice, the FIFO
* solution can be used in certain ways that very closely approximate non-blocking, for example, situations
* involving a single producer and multiple consumers.
*
* Although the FIFO algorithm is not non-blocking, it includes an optimization for remove() that is attractive:
* In the LIFO case, a slow path taken as the result of a failed excision would have to re-traverse the list
* to find the updated adjacent node pair for the already marked node. However, that node might already have
* been excised by some other thread, letting the thread potentially traverse the entire list just to discover
* it is no longer present (not an issue if the list is ordered by a key, then traversal is only to node >= key).
* In the FIFO case, premised on the invariant that inserts only come in from the tail, it is possible to prove
* a failed cas not to be the result of a new node inserted as with the LIFO case. With FIFO, there is only a single
* failure mode, i.e. some other thread excised the node already. Therefore, in the FIFO case, we skip the slow-path search pass.
*
* We say that the FIFO solution is "mostly" concurrent, in certain situations.
*
* Safe memory reclamation is based on a reference tracking scheme based on versioning, implemented using JfrVersionSystem.
* An access to the list is "versioned", with clients checking out the latest version describing the list.
* Destructive modifications made by clients, i.e. deletions, are signalled by incrementing the version.
* Before reclamation, a client inspects JfrVersionSystem to ensure checkouts with versions strictly
* less than the version of the modification have been relinquished. See utilities/JfrVersionSystem.hpp.
*
* Insertions can only take place from one end of the list, head or tail, exclusively.
* Specializations, a.k.a clients, must ensure this requirement.
*/

template <typename Client, template <typename> class SearchPolicy, typename AllocPolicy = JfrCHeapObj>
class JfrConcurrentLinkedListHost : public AllocPolicy {
 private:
  Client* _client;
  typedef typename Client::Node Node;
  typedef Node* NodePtr;
  typedef const Node* ConstNodePtr;
  typedef typename Client::VersionSystem::Type VersionType;
  typedef typename Client::VersionSystem::Handle VersionHandle;
 public:
  JfrConcurrentLinkedListHost(Client* client);
  bool initialize();
  void insert_head(NodePtr node, NodePtr head, ConstNodePtr tail) const;
  void insert_tail(NodePtr node, NodePtr head, NodePtr last, ConstNodePtr tail) const;
  NodePtr remove(NodePtr head, ConstNodePtr tail, NodePtr last = NULL, bool insert_is_head = true);
  template <typename Callback>
  void iterate(NodePtr head, ConstNodePtr tail, Callback& cb);
  bool in_list(ConstNodePtr node, NodePtr head, ConstNodePtr tail) const;
};

#endif // SHARE_JFR_UTILITIES_JFRCONCURRENTLINKEDLISTHOST_HPP
