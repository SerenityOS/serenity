/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
#ifndef SHARE_JFR_RECORDER_STORAGE_JFREPOCHSTORAGE_HPP
#define SHARE_JFR_RECORDER_STORAGE_JFREPOCHSTORAGE_HPP

#include "jfr/recorder/storage/jfrBuffer.hpp"
#include "jfr/recorder/storage/jfrMemorySpace.hpp"
#include "jfr/recorder/storage/jfrMemorySpaceRetrieval.hpp"
#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrConcurrentQueue.hpp"
#include "jfr/utilities/jfrLinkedList.hpp"

/*
 * Provides storage as a function of an epoch, with iteration capabilities for the current and previous epoch.
 *
 * When iterating the previous epoch, where exclusive access to buffers is assumed,
 * all buffers will be reinitialized post-callback, with retired buffers reclaimed
 * and moved onto the free list and non-retired buffers left in-place.
 *
 * When iterating the current epoch, where concurrent access to buffers is assumed,
 * there exist two modes, controlled by the EagerReclaim parameter.
 * By default, EagerReclaim is false, meaning no retired buffers are reclaimed during the current epoch.
 * Setting EagerReclaim to true, retired buffers will be reclaimed post-callback, by reinitialization
 * and by moving them onto the free list, just like is done when iterating the previous epoch.
 *
 * The design caters to use cases having multiple incremental iterations over the current epoch,
 * and a single iteration over the previous epoch.
 *
 * The JfrEpochStorage can be specialized by the following policies:
 *
 * NodeType          the type of the Node to be managed by the JfrMemorySpace.
 *
 * RetrievalPolicy   see jfrMemorySpace.hpp for a description.
 *
 * EagerReclaim      should retired buffers be reclaimed also during the current epoch (i.e. eagerly)
 *
 */
template <typename NodeType, template <typename> class RetrievalPolicy, bool EagerReclaim = false>
class JfrEpochStorageHost : public JfrCHeapObj {
  typedef JfrMemorySpace<JfrEpochStorageHost<NodeType, RetrievalPolicy, EagerReclaim>,
                         RetrievalPolicy,
                         JfrConcurrentQueue<NodeType>,
                         JfrLinkedList<NodeType>,
                         true> EpochMspace;
 public:
  typedef NodeType Buffer;
  typedef NodeType* BufferPtr;
  typedef EpochMspace Mspace;

  JfrEpochStorageHost();
  ~JfrEpochStorageHost();
  bool initialize(size_t min_elem_size, size_t free_list_cache_count_limit, size_t cache_prealloc_count);

  BufferPtr acquire(size_t size, Thread* thread);
  void release(BufferPtr buffer);

  template <typename Functor>
  void iterate(Functor& functor, bool previous_epoch = false);

  DEBUG_ONLY(void verify_previous_empty() const;)

 private:
  EpochMspace* _mspace;

  // mspace callback
  void register_full(BufferPtr buffer, Thread* thread);

  template <typename, template <typename> class, typename, typename, bool>
  friend class JfrMemorySpace;
};

typedef JfrEpochStorageHost<JfrBuffer, JfrMspaceRemoveRetrieval> JfrEpochStorage;

#endif // SHARE_JFR_RECORDER_STORAGE_JFREPOCHSTORAGE_HPP
