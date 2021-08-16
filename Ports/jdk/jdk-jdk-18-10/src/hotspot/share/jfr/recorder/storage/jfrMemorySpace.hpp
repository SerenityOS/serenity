/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
#ifndef SHARE_JFR_RECORDER_STORAGE_JFRMEMORYSPACE_HPP
#define SHARE_JFR_RECORDER_STORAGE_JFRMEMORYSPACE_HPP

#include "jfr/utilities/jfrAllocation.hpp"

const size_t JFR_MSPACE_UNLIMITED_CACHE_SIZE = max_uintx;

/*
 * A JfrMemorySpace abstracts a memory area by exposing configurable relations and functions.
 *
 * A memory space, or mspace for short, manages committed memory as multiples of a basic unit, min_element_size.
 * At the lowest level, and for higher levels of control, memory units can be directly managed using the allocate() and deallocate() functions.
 * More convenience is achieved by instead using one of the many higher level functions, which use allocate() and deallocate() underneath.
 * For storage, there exist two lists, a free list and a live list, each of a type that is configurable using policies.
 * To get memory from the mspace, use the acquire() function. To release the memory back, use release().
 * The exact means for how memory is provisioned and delivered through acquire() is configurable using a RetreivalPolicy.
 * A JfrMemorySpace can be specialized to be 'epoch aware', meaning it will perform list management as a function of
 * epoch state. This provides a convenient, relatively low-level mechanism, to process epoch relative data.
 *
 * A client of a JfrMemorySpace will specialize it according to the dimensions exposed by the following policies:
 *
 * Client            the type of the client, an instance is to be passed into the constructor.
 *                   a client must provide a single callback function:
 *                   register_full(FreeListType::Node*, Thread*);
 *
 * RetrievalPolicy   a template template class detailing how to retrieve memory for acquire.
 *                   the type parameter for the RetrivalPolicy template class is JfrMemorySpace and the policy class must provide:
 *                   FreeListType::Node* acquire(JfrMemorySpace* mspace, FreeListType* free_list, Thread*, size_t size, bool previous_epoch);
 *
 * FreeListType      the type of the free list. The syntactic interface to be fullfilled is most conveniently read from an example,
 *                   please see utilities/jfrConcurrentQueue.hpp.
 *
 * FreeListType::Node gives the basic node type for each individual unit to be managed by the memory space.
 *
 * LiveListType      the type of the live list. The syntactic interface is equivalent to the FreeListType.
 *                   LiveListType::Node must be compatible with FreeListType::Node.
 *
 * epoch_aware       boolean, default value is false.
 *
 */

template <typename Client,
          template <typename> class RetrievalPolicy,
          typename FreeListType,
          typename LiveListType = FreeListType,
          bool epoch_aware = false>
class JfrMemorySpace : public JfrCHeapObj {
 public:
  typedef FreeListType FreeList;
  typedef LiveListType LiveList;
  typedef typename FreeListType::Node Node;
  typedef typename FreeListType::NodePtr NodePtr;
 public:
  JfrMemorySpace(size_t min_elem_size, size_t free_list_cache_count_limit, Client* client);
  ~JfrMemorySpace();
  bool initialize(size_t cache_prealloc_count, bool prealloc_to_free_list = true);

  size_t min_element_size() const;

  NodePtr allocate(size_t size);
  void deallocate(NodePtr node);

  NodePtr acquire(size_t size, bool free_list, Thread* thread, bool previous_epoch = false);
  void release(NodePtr node);
  void release_live(NodePtr t, bool previous_epoch = false);
  void release_free(NodePtr t);

  FreeList& free_list();
  const FreeList& free_list() const;

  LiveList& live_list(bool previous_epoch = false);
  const LiveList& live_list(bool previous_epoch = false) const;

  bool free_list_is_empty() const;
  bool free_list_is_nonempty() const;
  bool live_list_is_empty(bool previous_epoch = false) const;
  bool live_list_is_nonempty(bool previous_epoch = false) const;

  void add_to_free_list(NodePtr node);
  void add_to_live_list(NodePtr node, bool previous_epoch = false);

  template <typename Callback>
  void iterate_free_list(Callback& callback);

  template <typename Callback>
  void iterate_live_list(Callback& callback, bool previous_epoch = false);

  bool in_free_list(const Node* node) const;
  bool in_live_list(const Node* node, bool previous_epoch = false) const;
  bool in_current_epoch_list(const Node* node) const;
  bool in_previous_epoch_list(const Node* node) const;

  void decrement_free_list_count();
  void register_full(NodePtr node, Thread* thread);

 private:
  FreeList _free_list;
  LiveList _live_list_epoch_0;
  LiveList _live_list_epoch_1;
  Client*  _client;
  const size_t _min_element_size;
  const size_t _free_list_cache_count_limit;
  size_t _free_list_cache_count;

  bool should_populate_free_list_cache() const;
  bool is_free_list_cache_limited() const;
  const LiveList& epoch_list_selector(u1 epoch) const;
  LiveList& epoch_list_selector(u1 epoch);
  const LiveList& current_epoch_list() const;
  LiveList& current_epoch_list();
  const LiveList& previous_epoch_list() const;
  LiveList& previous_epoch_list();
};

#endif // SHARE_JFR_RECORDER_STORAGE_JFRMEMORYSPACE_HPP
