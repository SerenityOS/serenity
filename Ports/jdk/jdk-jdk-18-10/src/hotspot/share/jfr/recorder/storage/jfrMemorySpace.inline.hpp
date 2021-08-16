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

#ifndef SHARE_JFR_RECORDER_STORAGE_JFRMEMORYSPACE_INLINE_HPP
#define SHARE_JFR_RECORDER_STORAGE_JFRMEMORYSPACE_INLINE_HPP

#include "jfr/recorder/storage/jfrMemorySpace.hpp"

#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdEpoch.hpp"
#include "runtime/atomic.hpp"
#include "runtime/os.hpp"

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::
JfrMemorySpace(size_t min_element_size, size_t free_list_cache_count_limit, Client* client) :
  _free_list(),
  _live_list_epoch_0(),
  _live_list_epoch_1(),
  _client(client),
  _min_element_size(min_element_size),
  _free_list_cache_count_limit(free_list_cache_count_limit),
  _free_list_cache_count(0) {}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::~JfrMemorySpace() {
  while (_live_list_epoch_0.is_nonempty()) {
    deallocate(_live_list_epoch_0.remove());
  }
  while (_live_list_epoch_1.is_nonempty()) {
    deallocate(_live_list_epoch_1.remove());
  }
  while (_free_list.is_nonempty()) {
    deallocate(_free_list.remove());
  }
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
bool JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::initialize(size_t cache_prealloc_count, bool prealloc_to_free_list) {
  if (!(_free_list.initialize() && _live_list_epoch_0.initialize() && _live_list_epoch_1.initialize())) {
    return false;
  }
  // pre-allocate elements to be cached in the requested list
  for (size_t i = 0; i < cache_prealloc_count; ++i) {
    NodePtr const node = allocate(_min_element_size);
    if (node == NULL) {
      return false;
    }
    if (prealloc_to_free_list) {
      add_to_free_list(node);
    } else {
      add_to_live_list(node);
    }
  }
  return true;
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline bool JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::should_populate_free_list_cache() const {
  return !is_free_list_cache_limited() || _free_list_cache_count < _free_list_cache_count_limit;
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline bool JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::is_free_list_cache_limited() const {
  return _free_list_cache_count_limit != JFR_MSPACE_UNLIMITED_CACHE_SIZE;
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline size_t JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::min_element_size() const {
  return _min_element_size;
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline FreeListType& JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::free_list() {
  return _free_list;
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline const FreeListType& JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::free_list() const {
  return _free_list;
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline FullListType& JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::live_list(bool previous_epoch) {
  if (epoch_aware) {
    return previous_epoch ? previous_epoch_list() : current_epoch_list();
  }
  return _live_list_epoch_0;
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline const FullListType& JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::live_list(bool previous_epoch) const {
  if (epoch_aware) {
    return previous_epoch ? previous_epoch_list() : current_epoch_list();
  }
  return _live_list_epoch_0;
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline bool JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::free_list_is_empty() const {
  return _free_list.is_empty();
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline bool JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::free_list_is_nonempty() const {
  return !free_list_is_empty();
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline bool JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::live_list_is_empty(bool previous_epoch) const {
  return live_list().is_empty();
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline bool JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::live_list_is_nonempty(bool previous_epoch) const {
  return live_list().is_nonempty();
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
bool JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::in_free_list(const typename FreeListType::Node* node) const {
  return _free_list.in_list(node);
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline const typename JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::LiveList&
JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::epoch_list_selector(u1 epoch) const {
  assert(epoch_aware, "invariant");
  return epoch == 0 ? _live_list_epoch_0 : _live_list_epoch_1;
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline typename JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::LiveList&
JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::epoch_list_selector(u1 epoch) {
  return const_cast<typename JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::LiveList&>(
    const_cast<const JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>*>(this)->epoch_list_selector(epoch));
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline const typename JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::LiveList&
JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::current_epoch_list() const {
  assert(epoch_aware, "invariant");
  return epoch_list_selector(JfrTraceIdEpoch::current());
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline typename JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::LiveList&
JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::current_epoch_list() {
  return const_cast<typename JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::LiveList&>(
    const_cast<const JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>*>(this)->current_epoch_list());
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline const typename JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::LiveList&
JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::previous_epoch_list() const {
  assert(epoch_aware, "invariant");
  return epoch_list_selector(JfrTraceIdEpoch::previous());
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline typename JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::LiveList&
JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::previous_epoch_list() {
  return const_cast<typename JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::LiveList&>(
    const_cast<const JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>*>(this)->previous_epoch_list());
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
bool JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::in_live_list(const typename FreeListType::Node* node, bool previous_epoch) const {
  return live_list(previous_epoch).in_list(node);
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline bool JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::in_current_epoch_list(const typename FreeListType::Node* node) const {
  assert(epoch_aware, "invariant");
  return current_epoch_list().in_list(node);
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline bool JfrMemorySpace< Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::in_previous_epoch_list(const typename FreeListType::Node* node) const {
  assert(epoch_aware, "invariant");
  return previous_epoch_list().in_list(node);
}

// allocations are even multiples of the mspace min size
static inline size_t align_allocation_size(size_t requested_size, size_t min_element_size) {
  u8 alloc_size_bytes = min_element_size;
  while (requested_size > alloc_size_bytes) {
    alloc_size_bytes <<= 1;
  }
  return (size_t)alloc_size_bytes;
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline typename FreeListType::NodePtr JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::allocate(size_t size) {
  const size_t aligned_size_bytes = align_allocation_size(size, _min_element_size);
  void* const allocation = JfrCHeapObj::new_array<u1>(aligned_size_bytes + sizeof(Node));
  if (allocation == NULL) {
    return NULL;
  }
  NodePtr node = new (allocation) Node();
  assert(node != NULL, "invariant");
  if (!node->initialize(sizeof(Node), aligned_size_bytes)) {
    JfrCHeapObj::free(node, aligned_size_bytes + sizeof(Node));
    return NULL;
  }
  return node;
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline void JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::deallocate(typename FreeListType::NodePtr node) {
  assert(node != NULL, "invariant");
  assert(!in_free_list(node), "invariant");
  assert(!_live_list_epoch_0.in_list(node), "invariant");
  assert(!_live_list_epoch_1.in_list(node), "invariant");
  assert(node != NULL, "invariant");
  JfrCHeapObj::free(node, node->total_size());
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline typename FreeListType::NodePtr JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::acquire(size_t size, bool free_list, Thread* thread, bool previous_epoch) {
  return RetrievalPolicy<JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware> >::acquire(this, free_list, thread, size, previous_epoch);
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline void JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::release(typename FreeListType::NodePtr node) {
  assert(node != NULL, "invariant");
  if (node->transient()) {
    deallocate(node);
    return;
  }
  assert(node->empty(), "invariant");
  assert(!node->retired(), "invariant");
  assert(node->identity() == NULL, "invariant");
  if (should_populate_free_list_cache()) {
    add_to_free_list(node);
  } else {
    deallocate(node);
  }
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline void JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::add_to_free_list(typename FreeListType::NodePtr node) {
  assert(node != NULL, "invariant");
  _free_list.add(node);
  if (is_free_list_cache_limited()) {
    Atomic::inc(&_free_list_cache_count);
  }
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline void JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::add_to_live_list(typename FreeListType::NodePtr node, bool previous_epoch) {
  assert(node != NULL, "invariant");
  live_list(previous_epoch).add(node);
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline void JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::decrement_free_list_count() {
  if (is_free_list_cache_limited()) {
    Atomic::dec(&_free_list_cache_count);
  }
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
template <typename Callback>
inline void JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::iterate_free_list(Callback& callback) {
  return _free_list.iterate(callback);
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
template <typename Callback>
inline void JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::iterate_live_list(Callback& callback, bool previous_epoch) {
  if (epoch_aware) {
    live_list(previous_epoch).iterate(callback);
    return;
  }
  _live_list_epoch_0.iterate(callback);
}

template <typename Client, template <typename> class RetrievalPolicy, typename FreeListType, typename FullListType, bool epoch_aware>
inline void JfrMemorySpace<Client, RetrievalPolicy, FreeListType, FullListType, epoch_aware>::register_full(typename FreeListType::NodePtr node, Thread* thread) {
  _client->register_full(node, thread);
}

template <typename Mspace, typename Client>
static inline Mspace* create_mspace(size_t min_element_size, size_t free_list_cache_count_limit, size_t cache_prealloc_count, bool prealloc_to_free_list, Client* cb) {
  Mspace* const mspace = new Mspace(min_element_size, free_list_cache_count_limit, cb);
  if (mspace != NULL) {
    mspace->initialize(cache_prealloc_count, prealloc_to_free_list);
  }
  return mspace;
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_allocate(size_t size, Mspace* mspace) {
  return mspace->allocate(size);
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_allocate_acquired(size_t size, Mspace* mspace, Thread* thread) {
  typename Mspace::NodePtr node = mspace_allocate(size, mspace);
  if (node == NULL) return NULL;
  node->set_identity(thread);
  return node;
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_allocate_transient(size_t size, Mspace* mspace, Thread* thread) {
  typename Mspace::NodePtr node = mspace_allocate_acquired(size, mspace, thread);
  if (node == NULL) return NULL;
  assert(node->acquired_by_self(), "invariant");
  node->set_transient();
  return node;
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_allocate_transient_lease(size_t size, Mspace* mspace, Thread* thread) {
  typename Mspace::NodePtr node = mspace_allocate_transient(size, mspace, thread);
  if (node == NULL) return NULL;
  assert(node->transient(), "invariant");
  node->set_lease();
  return node;
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_allocate_transient_lease_to_free(size_t size, Mspace* mspace, Thread* thread) {
  typename Mspace::NodePtr node = mspace_allocate_transient_lease(size, mspace, thread);
  if (node == NULL) return NULL;
  assert(node->lease(), "invariant");
  mspace->add_to_free_list(node);
  return node;
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_acquire_free(size_t size, Mspace* mspace, Thread* thread) {
  return mspace->acquire(size, true, thread);
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_acquire_free_with_retry(size_t size, Mspace* mspace, size_t retry_count, Thread* thread) {
  assert(size <= mspace->min_element_size(), "invariant");
  for (size_t i = 0; i < retry_count; ++i) {
    typename Mspace::NodePtr node = mspace_acquire_free(size, mspace, thread);
    if (node != NULL) {
      return node;
    }
  }
  return NULL;
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_allocate_to_live_list(size_t size, Mspace* mspace, Thread* thread) {
  typename Mspace::NodePtr node = mspace_allocate_acquired(size, mspace, thread);
  if (node == NULL) return NULL;
  assert(node->acquired_by_self(), "invariant");
  mspace->add_to_live_list(node);
  return node;
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_allocate_transient_to_live_list(size_t size, Mspace* mspace, Thread* thread, bool previous_epoch = false) {
  typename Mspace::NodePtr node = mspace_allocate_transient(size, mspace, thread);
  if (node == NULL) return NULL;
  assert(node->transient(), "invariant");
  mspace->add_to_live_list(node, previous_epoch);
  return node;
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_allocate_transient_lease_to_live_list(size_t size, Mspace* mspace, Thread* thread, bool previous_epoch = false) {
  typename Mspace::NodePtr node = mspace_allocate_transient_lease(size, mspace, thread);
  if (node == NULL) return NULL;
  assert(node->lease(), "invariant");
  mspace->add_to_live_list(node, previous_epoch);
  return node;
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_acquire_free_to_live_list(size_t size, Mspace* mspace, Thread* thread, bool previous_epoch = false) {
  assert(size <= mspace->min_element_size(), "invariant");
  typename Mspace::NodePtr node = mspace_acquire_free(size, mspace, thread);
  if (node == NULL) {
    return NULL;
  }
  assert(node->acquired_by_self(), "invariant");
  mspace->add_to_live_list(node, previous_epoch);
  return node;
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_acquire_to_live_list(size_t size, Mspace* mspace, Thread* thread, bool previous_epoch = false) {
  if (size <= mspace->min_element_size()) {
    typename Mspace::NodePtr node = mspace_acquire_free_to_live_list(size, mspace, thread, previous_epoch);
    if (node != NULL) {
      return node;
    }
  }
  return mspace_allocate_to_live_list(size, mspace, thread);
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_acquire_live(size_t size, Mspace* mspace, Thread* thread, bool previous_epoch = false) {
  return mspace->acquire(size, false, thread, previous_epoch);
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_acquire_live_with_retry(size_t size, Mspace* mspace, size_t retry_count, Thread* thread, bool previous_epoch = false) {
  assert(size <= mspace->min_element_size(), "invariant");
  for (size_t i = 0; i < retry_count; ++i) {
    typename Mspace::NodePtr const node = mspace_acquire_live(size, mspace, thread, previous_epoch);
    if (node != NULL) {
      return node;
    }
  }
  return NULL;
}

template <typename Mspace>
inline typename Mspace::NodePtr mspace_acquire_lease_with_retry(size_t size, Mspace* mspace, size_t retry_count, Thread* thread, bool previous_epoch = false) {
  typename Mspace::NodePtr node = mspace_acquire_live_with_retry(size, mspace, retry_count, thread, previous_epoch);
  if (node != NULL) {
    node->set_lease();
  }
  return node;
}

template <typename Mspace>
inline void mspace_release(typename Mspace::NodePtr node, Mspace* mspace) {
  assert(node != NULL, "invariant");
  assert(node->unflushed_size() == 0, "invariant");
  assert(mspace != NULL, "invariant");
  mspace->release(node);
}

template <typename Callback, typename Mspace>
inline void process_live_list(Callback& callback, Mspace* mspace, bool previous_epoch = false) {
  assert(mspace != NULL, "invariant");
  mspace->iterate_live_list(callback, previous_epoch);
}

template <typename Callback, typename Mspace>
inline void process_free_list(Callback& callback, Mspace* mspace) {
  assert(mspace != NULL, "invariant");
  assert(mspace->free_list_is_nonempty(), "invariant");
  mspace->iterate_free_list(callback);
}

template <typename Mspace>
class ReleaseOp : public StackObj {
 private:
  Mspace* _mspace;
  bool _previous_epoch;
 public:
  typedef typename Mspace::Node Node;
  ReleaseOp(Mspace* mspace) : _mspace(mspace) {}
  bool process(typename Mspace::NodePtr node);
  size_t processed() const { return 0; }
};

template <typename Mspace>
inline bool ReleaseOp<Mspace>::process(typename Mspace::NodePtr node) {
  assert(node != NULL, "invariant");
  // assumes some means of exclusive access to the node
  if (node->transient()) {
    // make sure the transient node is already detached
    _mspace->release(node);
    return true;
  }
  node->reinitialize();
  if (node->identity() != NULL) {
    assert(node->empty(), "invariant");
    assert(!node->retired(), "invariant");
    node->release(); // publish
  }
  return true;
}

template <typename Mspace, typename List>
class ReleaseWithExcisionOp : public ReleaseOp<Mspace> {
 private:
  List& _list;
  typename List::NodePtr _prev;
  size_t _count;
  size_t _amount;
 public:
  ReleaseWithExcisionOp(Mspace* mspace, List& list) :
    ReleaseOp<Mspace>(mspace), _list(list), _prev(NULL), _count(0), _amount(0) {}
  bool process(typename List::NodePtr node);
  size_t processed() const { return _count; }
  size_t amount() const { return _amount; }
};

template <typename Mspace, typename List>
inline bool ReleaseWithExcisionOp<Mspace, List>::process(typename List::NodePtr node) {
  assert(node != NULL, "invariant");
  if (node->transient()) {
    _prev = _list.excise(_prev, node);
  } else {
    _prev = node;
  }
  return ReleaseOp<Mspace>::process(node);
}

template <typename Mspace, typename List>
class ScavengingReleaseOp : public StackObj {
 protected:
  Mspace* _mspace;
  List& _list;
  typename List::NodePtr _prev;
  size_t _count;
  size_t _amount;
    bool excise_with_release(typename List::NodePtr node);
 public:
  typedef typename List::Node Node;
  ScavengingReleaseOp(Mspace* mspace, List& list) :
    _mspace(mspace), _list(list), _prev(NULL), _count(0), _amount(0) {}
  bool process(typename List::NodePtr node);
  size_t processed() const { return _count; }
  size_t amount() const { return _amount; }
};

template <typename Mspace, typename List>
inline bool ScavengingReleaseOp<Mspace, List>::process(typename List::NodePtr node) {
  assert(node != NULL, "invariant");
  assert(!node->transient(), "invariant");
  if (node->retired()) {
    return excise_with_release(node);
  }
  _prev = node;
  return true;
}

template <typename Mspace, typename List>
inline bool ScavengingReleaseOp<Mspace, List>::excise_with_release(typename List::NodePtr node) {
  assert(node != NULL, "invariant");
  assert(node->retired(), "invariant");
  _prev = _list.excise(_prev, node);
  if (node->transient()) {
    _mspace->deallocate(node);
    return true;
  }
  assert(node->identity() != NULL, "invariant");
  assert(node->empty(), "invariant");
  assert(!node->lease(), "invariant");
  assert(!node->excluded(), "invariant");
  ++_count;
  _amount += node->total_size();
  node->clear_retired();
  node->release();
  mspace_release(node, _mspace);
  return true;
}

template <typename Mspace, typename FromList>
class ReleaseRetiredOp : public StackObj {
private:
  Mspace* _mspace;
  FromList& _list;
  typename Mspace::NodePtr _prev;
public:
  typedef typename Mspace::Node Node;
  ReleaseRetiredOp(Mspace* mspace, FromList& list) :
    _mspace(mspace), _list(list), _prev(NULL) {}
  bool process(Node* node);
};

template <typename Mspace, typename FromList>
inline bool ReleaseRetiredOp<Mspace, FromList>::process(typename Mspace::Node* node) {
  assert(node != NULL, "invariant");
  if (node->retired()) {
    _prev = _list.excise(_prev, node);
    node->reinitialize();
    assert(node->empty(), "invariant");
    assert(!node->retired(), "invariant");
    node->release();
    mspace_release(node, _mspace);
  } else {
    _prev = node;
  }
  return true;
}

template <typename Mspace, typename FromList>
class ReinitializeAllReleaseRetiredOp : public StackObj {
private:
  Mspace* _mspace;
  FromList& _list;
  typename Mspace::NodePtr _prev;
 public:
  typedef typename Mspace::Node Node;
  ReinitializeAllReleaseRetiredOp(Mspace* mspace, FromList& list) :
    _mspace(mspace), _list(list), _prev(NULL) {}
  bool process(Node* node);
};

template <typename Mspace, typename FromList>
inline bool ReinitializeAllReleaseRetiredOp<Mspace, FromList>::process(typename Mspace::Node* node) {
  assert(node != NULL, "invariant");
  // assumes some means of exclusive access to node
  const bool retired = node->retired();
  node->reinitialize();
  assert(node->empty(), "invariant");
  assert(!node->retired(), "invariant");
  if (retired) {
    _prev = _list.excise(_prev, node);
    node->release();
    mspace_release(node, _mspace);
  } else {
    _prev = node;
  }
  return true;
}

#ifdef ASSERT
template <typename Node>
inline void assert_migration_state(const Node* old, const Node* new_node, size_t used, size_t requested) {
  assert(old != NULL, "invariant");
  assert(new_node != NULL, "invariant");
  assert(old->pos() >= old->start(), "invariant");
  assert(old->pos() + used <= old->end(), "invariant");
  assert(new_node->free_size() >= (used + requested), "invariant");
}
#endif // ASSERT

template <typename Node>
inline void migrate_outstanding_writes(const Node* old, Node* new_node, size_t used, size_t requested) {
  DEBUG_ONLY(assert_migration_state(old, new_node, used, requested);)
  if (used > 0) {
    memcpy(new_node->pos(), old->pos(), used);
  }
}

#endif // SHARE_JFR_RECORDER_STORAGE_JFRMEMORYSPACE_INLINE_HPP
