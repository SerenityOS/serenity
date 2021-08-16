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

#ifndef SHARE_JFR_RECORDER_STORAGE_JFRFULLSTORAGE_HPP
#define SHARE_JFR_RECORDER_STORAGE_JFRFULLSTORAGE_HPP

#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrConcurrentQueue.hpp"

class JfrStorageControl;

/*
 * For full storage management.
 *
 * In essence, full storage is added to a FIFO queue, where the insertion order
 * is used to represent the "is older" relation. Removes oldest data first.
 *
 * FullType     the type of the data value to be stored in the list.
 *
 * NodeType     template class for the node to store a value of FullType.
 *
 * AllocPolicy  memory alloction.
 */
template <typename FullType, template <typename> class NodeType, typename AllocPolicy = JfrCHeapObj>
class JfrFullStorage : public AllocPolicy {
 public:
  typedef FullType Value;
  typedef NodeType<Value>* NodePtr;
  typedef NodeType<Value> Node;
  JfrFullStorage(JfrStorageControl& control);
  ~JfrFullStorage();
  bool initialize(size_t free_list_prealloc_count);
  bool is_empty() const;
  bool is_nonempty() const;
  bool add(Value value);
  Value remove();
  template <typename Callback>
  void iterate(Callback& cb);
 private:
  JfrStorageControl& _control;
  JfrConcurrentQueue<Node, AllocPolicy>* _free_node_list;
  JfrConcurrentQueue<Node, AllocPolicy>* _queue;
  NodePtr acquire();
  void release(NodePtr node);
};

#endif // SHARE_JFR_RECORDER_STORAGE_JFRFULLSTORAGE_HPP
