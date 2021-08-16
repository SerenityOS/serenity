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

#ifndef SHARE_JFR_UTILITIES_JFRCONCURRENTQUEUE_HPP
#define SHARE_JFR_UTILITIES_JFRCONCURRENTQUEUE_HPP

#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrConcurrentLinkedListHost.hpp"
#include "jfr/utilities/jfrRelation.hpp"
#include "jfr/utilities/jfrVersionSystem.hpp"

 /*
  * This is a thread-safe FIFO structure.
  * Although not non-blocking, for certain scenarios,
  * it can act as a close approximation, "mostly" concurrent.
  * For a more detailed description of its properties,
  * please see JfrConcurrentLinkedListHost.hpp.
  */
template <typename NodeType, typename AllocPolicy = JfrCHeapObj>
class JfrConcurrentQueue : public AllocPolicy {
 public:
  typedef NodeType Node;
  typedef NodeType* NodePtr;
  typedef const NodeType* ConstNodePtr;
  typedef JfrVersionSystem VersionSystem;
  JfrConcurrentQueue();
  bool initialize();
  bool is_empty() const;
  bool is_nonempty() const;
  void add(NodePtr node);
  NodePtr remove();
  template <typename Callback>
  void iterate(Callback& cb);
  bool in_list(const Node* node) const;
 private:
  JfrConcurrentLinkedListHost<JfrConcurrentQueue<Node, AllocPolicy>, HeadNode>* _list;
  Node _head;
  Node _last;
  const Node _tail;
  JfrVersionSystem _version_system;
  // callback for JfrConcurrentLinkedListHost
  typename VersionSystem::Handle get_version_handle();
  template <typename, template <typename> class, typename>
  friend class JfrConcurrentLinkedListHost;
};

#endif // SHARE_JFR_UTILITIES_JFRCONCURRENTQUEUE_HPP

