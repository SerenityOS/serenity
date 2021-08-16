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

#ifndef SHARE_JFR_WRITERS_JFRSTORAGEHOST_INLINE_HPP
#define SHARE_JFR_WRITERS_JFRSTORAGEHOST_INLINE_HPP

#include "jfr/writers/jfrStorageHost.hpp"

template <typename Adapter, typename AP>
inline void StorageHost<Adapter, AP>::bind() {
  if (is_backed()) {
    this->hard_reset();
    assert(is_valid(), "invariant");
    return;
  }
  this->set_start_pos(NULL);
  this->set_current_pos((const u1*)NULL);
  this->set_end_pos(NULL);
}

template <typename Adapter, typename AP>
inline void StorageHost<Adapter, AP>::soft_reset() {
  this->set_start_pos(this->current_pos());
}

template <typename Adapter, typename AP>
inline void StorageHost<Adapter, AP>::hard_reset() {
  this->set_start_pos(_adapter.pos());
  this->set_current_pos(_adapter.pos());
  this->set_end_pos(_adapter.end());
}

template <typename Adapter, typename AP>
inline void StorageHost<Adapter, AP>::cancel() {
  this->set_end_pos(NULL);
}

template <typename Adapter, typename AP>
inline bool StorageHost<Adapter, AP>::is_backed() {
  return _adapter.storage() != NULL;
}

template <typename Adapter, typename AP>
inline bool StorageHost<Adapter, AP>::accommodate(size_t used, size_t requested) {
  if (!_adapter.flush(used, requested)) {
    this->cancel();
    return false;
  }
  assert(is_backed(), "invariant");
  this->hard_reset();
  this->set_current_pos(used);
  return true;
}

template <typename Adapter, typename AP>
inline void StorageHost<Adapter, AP>::commit() {
  if (this->is_valid()) {
    assert(_adapter.pos() == this->start_pos(), "invariant");
    assert(_adapter.end() == this->end_pos(), "invariant");
    u1* new_position = this->current_pos();
    _adapter.commit(new_position);
    this->set_start_pos(new_position);
  }
}

template <typename Adapter, typename AP>
inline void StorageHost<Adapter, AP>::release() {
  _adapter.release();
}

template <typename Adapter, typename AP>
inline StorageHost<Adapter, AP>::StorageHost(typename Adapter::StorageType* storage, Thread* thread) : Position<AP>(), _adapter(storage, thread) {
  bind();
}

template <typename Adapter, typename AP>
inline StorageHost<Adapter, AP>::StorageHost(typename Adapter::StorageType* storage, size_t size) : Position<AP>(), _adapter(storage, size) {
  bind();
}

template <typename Adapter, typename AP>
inline StorageHost<Adapter, AP>::StorageHost(Thread* thread) : Position<AP>(), _adapter(thread) {
  bind();
}

template <typename Adapter, typename AP>
inline bool StorageHost<Adapter, AP>::is_valid() const {
  return this->end_pos() != NULL;
}

template <typename Adapter, typename AP>
inline typename Adapter::StorageType* StorageHost<Adapter, AP>::storage() {
  return _adapter.storage();
}

template <typename Adapter, typename AP>
inline void StorageHost<Adapter, AP>::set_storage(typename Adapter::StorageType* storage) {
  _adapter.set_storage(storage);
  bind();
}

template <typename Adapter, typename AP>
inline void StorageHost<Adapter, AP>::flush() {
  this->accommodate(this->is_valid() ? this->used_size() : 0, 0);
}

template <typename Adapter, typename AP>
inline void StorageHost<Adapter, AP>::seek(intptr_t offset) {
  if (this->is_valid()) {
    assert(offset >= 0, "negative offsets not supported");
    assert(this->start_pos() + offset <= this->end_pos(), "invariant");
    assert(this->start_pos() + offset >= this->start_pos(), "invariant");
    this->set_current_pos(this->start_pos() + offset);
  }
}

#endif // SHARE_JFR_WRITERS_JFRSTORAGEHOST_INLINE_HPP
