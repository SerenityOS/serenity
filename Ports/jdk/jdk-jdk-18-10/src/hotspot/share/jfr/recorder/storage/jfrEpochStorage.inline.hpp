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

#ifndef SHARE_JFR_RECORDER_STORAGE_JFREPOCHSTORAGE_INLINE_HPP
#define SHARE_JFR_RECORDER_STORAGE_JFREPOCHSTORAGE_INLINE_HPP

#include "jfr/recorder/storage/jfrEpochStorage.hpp"

#include "jfr/recorder/storage/jfrMemorySpace.inline.hpp"
#include "jfr/recorder/storage/jfrStorageUtils.inline.hpp"
#include "jfr/utilities/jfrConcurrentQueue.inline.hpp"
#include "jfr/utilities/jfrLinkedList.inline.hpp"
#include "logging/log.hpp"

template <typename NodeType, template <typename> class RetrievalPolicy, bool EagerReclaim>
JfrEpochStorageHost<NodeType, RetrievalPolicy, EagerReclaim>::JfrEpochStorageHost() : _mspace(NULL) {}

template <typename NodeType, template <typename> class RetrievalPolicy, bool EagerReclaim>
JfrEpochStorageHost<NodeType, RetrievalPolicy, EagerReclaim>::~JfrEpochStorageHost() {
  delete _mspace;
}

template <typename NodeType, template <typename> class RetrievalPolicy, bool EagerReclaim>
bool JfrEpochStorageHost<NodeType, RetrievalPolicy, EagerReclaim>::initialize(size_t min_elem_size, size_t free_list_cache_count_limit, size_t cache_prealloc_count) {
  assert(_mspace == NULL, "invariant");
  _mspace = new EpochMspace(min_elem_size, free_list_cache_count_limit, this);
  return _mspace != NULL && _mspace->initialize(cache_prealloc_count);
}

template <typename NodeType, template <typename> class RetrievalPolicy, bool EagerReclaim>
inline NodeType* JfrEpochStorageHost<NodeType, RetrievalPolicy, EagerReclaim>::acquire(size_t size, Thread* thread) {
  BufferPtr buffer = mspace_acquire_to_live_list(size, _mspace, thread);
  if (buffer == NULL) {
    log_warning(jfr)("Unable to allocate " SIZE_FORMAT " bytes of %s.", _mspace->min_element_size(), "epoch storage");
    return NULL;
  }
  assert(buffer->acquired_by_self(), "invariant");
  return buffer;
}

template <typename NodeType, template <typename> class RetrievalPolicy, bool EagerReclaim>
void JfrEpochStorageHost<NodeType, RetrievalPolicy, EagerReclaim>::release(NodeType* buffer) {
  assert(buffer != NULL, "invariant");
  buffer->set_retired();
}

template <typename NodeType, template <typename> class RetrievalPolicy, bool EagerReclaim>
void JfrEpochStorageHost<NodeType, RetrievalPolicy, EagerReclaim>::register_full(NodeType* buffer, Thread* thread) {
  // nothing here at the moment
}

template <typename NodeType, template <typename> class RetrievalPolicy, bool EagerReclaim>
template <typename Functor>
void JfrEpochStorageHost<NodeType, RetrievalPolicy, EagerReclaim>::iterate(Functor& functor, bool previous_epoch) {
  typedef ReinitializeAllReleaseRetiredOp<EpochMspace, typename EpochMspace::LiveList> PreviousEpochReleaseOperation;
  typedef CompositeOperation<Functor, PreviousEpochReleaseOperation> PreviousEpochOperation;
  typedef ReleaseRetiredOp<EpochMspace, typename EpochMspace::LiveList> CurrentEpochReleaseOperation;
  typedef CompositeOperation<Functor, CurrentEpochReleaseOperation> CurrentEpochOperation;
  if (previous_epoch) {
    PreviousEpochReleaseOperation pero(_mspace, _mspace->live_list(true));
    PreviousEpochOperation peo(&functor, &pero);
    process_live_list(peo, _mspace, true); // previous epoch list
    return;
  }
  if (EagerReclaim) {
    CurrentEpochReleaseOperation cero(_mspace, _mspace->live_list());
    CurrentEpochOperation ceo(&functor, &cero);
    process_live_list(ceo, _mspace, false); // current epoch list
    return;
  }
  process_live_list(functor, _mspace, false); // current epoch list
}

#ifdef ASSERT

template <typename Mspace>
class EmptyVerifier {
 private:
  Mspace* _mspace;
 public:
  typedef typename Mspace::Node Node;
  typedef typename Mspace::NodePtr NodePtr;
  EmptyVerifier(Mspace* mspace) : _mspace(mspace) {}
  bool process(NodePtr node) {
    assert(node != NULL, "invariant");
    assert(node->empty(), "invariant");
    return true;
  }
};

template <typename NodeType, template <typename> class RetrievalPolicy, bool EagerReclaim>
void JfrEpochStorageHost<NodeType, RetrievalPolicy, EagerReclaim>::verify_previous_empty() const {
  typedef EmptyVerifier<JfrEpochStorage::Mspace> VerifyEmptyMspace;
  VerifyEmptyMspace vem(_mspace);
  process_live_list(vem, _mspace, true);
}

#endif // ASSERT

#endif // SHARE_JFR_RECORDER_STORAGE_JFREPOCHSTORAGE_INLINE_HPP
