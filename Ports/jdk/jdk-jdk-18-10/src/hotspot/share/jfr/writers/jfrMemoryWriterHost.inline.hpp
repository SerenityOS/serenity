/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_WRITERS_JFRMEMORYWRITERHOST_INLINE_HPP
#define SHARE_JFR_WRITERS_JFRMEMORYWRITERHOST_INLINE_HPP

#include "jfr/writers/jfrMemoryWriterHost.hpp"

template <typename Adapter, typename AP, typename AccessAssert>
inline void MemoryWriterHost<Adapter, AP, AccessAssert>::write_bytes(void* dest, const void* buf, intptr_t len) {
  assert(dest != NULL, "invariant");
  assert(len >= 0, "invariant");
  memcpy(dest, buf, (size_t)len); // no encoding
  this->set_current_pos(len);
}

template <typename Adapter, typename AP, typename AccessAssert>
inline MemoryWriterHost<Adapter, AP, AccessAssert>::MemoryWriterHost(typename Adapter::StorageType* storage, Thread* thread) :
  StorageHost<Adapter, AP>(storage, thread) {
}

template <typename Adapter, typename AP, typename AccessAssert>
inline MemoryWriterHost<Adapter, AP, AccessAssert>::MemoryWriterHost(typename Adapter::StorageType* storage, size_t size) :
  StorageHost<Adapter, AP>(storage, size) {
}

template <typename Adapter, typename AP, typename AccessAssert>
inline MemoryWriterHost<Adapter, AP, AccessAssert>::MemoryWriterHost(Thread* thread) :
  StorageHost<Adapter, AP>(thread) {
}

template <typename Adapter, typename AP, typename AccessAssert>
inline void MemoryWriterHost<Adapter, AP, AccessAssert>::acquire() {
  debug_only(_access.acquire();)
  if (!this->is_valid()) {
    this->flush();
  }
  debug_only(is_acquired();)
}

template <typename Adapter, typename AP, typename AccessAssert>
inline void MemoryWriterHost<Adapter, AP, AccessAssert>::release() {
  debug_only(is_acquired();)
  StorageHost<Adapter, AP>::release();
  debug_only(_access.release();)
}

#ifdef ASSERT
template <typename Adapter, typename AP, typename AccessAssert>
inline bool MemoryWriterHost<Adapter, AP, AccessAssert>::is_acquired() const {
  return _access.is_acquired();
}
#endif

template <typename Adapter, typename AP>
inline AcquireReleaseMemoryWriterHost<Adapter, AP>::AcquireReleaseMemoryWriterHost(typename Adapter::StorageType* storage, Thread* thread) :
  MemoryWriterHost<Adapter, AP>(storage, thread) {
  this->acquire();
}

template <typename Adapter, typename AP>
inline AcquireReleaseMemoryWriterHost<Adapter, AP>::AcquireReleaseMemoryWriterHost(typename Adapter::StorageType* storage, size_t size) :
  MemoryWriterHost<Adapter, AP>(storage, size) {
  this->acquire();
}

template <typename Adapter, typename AP>
inline AcquireReleaseMemoryWriterHost<Adapter, AP>::AcquireReleaseMemoryWriterHost(Thread* thread) :
  MemoryWriterHost<Adapter, AP>(thread) {
  this->acquire();
}

template <typename Adapter, typename AP>
inline AcquireReleaseMemoryWriterHost<Adapter, AP>::~AcquireReleaseMemoryWriterHost() {
  assert(this->is_acquired(), "invariant");
  this->release();
}

#endif // SHARE_JFR_WRITERS_JFRMEMORYWRITERHOST_INLINE_HPP
