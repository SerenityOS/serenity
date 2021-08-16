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

#ifndef SHARE_JFR_WRITERS_JFRSTORAGEADAPTER_HPP
#define SHARE_JFR_WRITERS_JFRSTORAGEADAPTER_HPP

#include "jfr/utilities/jfrAllocation.hpp"

class Thread;

//
// The adapters present writers with a uniform interface over storage.
//
// Adapter policy
//
// StorageType* storage();
// const u1* start() const;
// const u1* pos();
// const u1* end() const;
// void commit(u1* position);
// bool flush(size_t used, size_t requested);
// void release();
//

template <typename Flush>
class Adapter {
 public:
  typedef typename Flush::Type StorageType;
  Adapter(StorageType* storage, Thread* thread) : _storage(storage), _thread(thread) {}
  Adapter(Thread* thread) : _storage(NULL), _thread(thread) {}

  void set_storage(StorageType* storage) {
    _storage = storage;
  }

  StorageType* storage() {
    return _storage;
  }

  const u1* start() const {
    assert(_storage != NULL, "invariant");
    return _storage->start();
  }

  u1* pos() {
    assert(_storage != NULL, "invariant");
    return _storage->pos();
  }

  const u1* end() const {
    assert(_storage != NULL, "invariant");
    return _storage->end();
  }

  void commit(u1* position) {
    assert(_storage != NULL, "invariant");
    _storage->set_pos(position);
  }

  bool flush(size_t used, size_t requested) {
    assert(_thread != NULL, "invariant");
    Flush f(_storage, used, requested, _thread);
    _storage = f.result();
    return _storage != NULL && !_storage->excluded();
  }

  void release() {
    if (_storage != NULL && _storage->lease()) {
      // This flush call will return the lease
      // of a temporary storage area.
      // Since the requested size is 0,
      // the flush implementation will accomodate
      // that 'size' request in the
      // original thread local storage,
      // by implication restoring the original
      // in the process of returning a lease.
      flush(0, 0);
    }
  }

 private:
  StorageType* _storage;
  Thread* _thread;
};

template <size_t DEFAULT_SIZE = K>
class MallocAdapter {
 private:
  u1* _start;
  u1* _pos;
  u1* _end;
  size_t _initial_size;
  bool _has_ownership;

  bool allocate(size_t size);
  void deallocate();

 public:
  typedef u1 StorageType;
  MallocAdapter(u1* storage, Thread* thread);
  MallocAdapter(u1* storage, size_t size);
  MallocAdapter(Thread* thread);
  ~MallocAdapter();

  StorageType* storage() { return _start; }
  const u1* start() const { return _start; }
  u1* pos() { return _pos; }
  void commit(u1* position) { _pos = position; }
  const u1* end() const { return _end; }
  void release() {}
  bool flush(size_t used, size_t requested);
};

template <size_t DEFAULT_SIZE>
MallocAdapter<DEFAULT_SIZE>::MallocAdapter(u1* storage, size_t size) :
  _start(storage),
  _pos(storage),
  _end(storage + size),
  _initial_size(size),
  _has_ownership(false) {
}

template <size_t DEFAULT_SIZE>
MallocAdapter<DEFAULT_SIZE> ::MallocAdapter(u1* storage, Thread* thread) :
  _start(storage),
  _pos(storage),
  _end(storage),
  _initial_size(0),
  _has_ownership(false) {
}

template <size_t DEFAULT_SIZE>
MallocAdapter<DEFAULT_SIZE>::MallocAdapter(Thread* thread) :
  _start(NULL),
  _pos(NULL),
  _end(NULL),
  _initial_size(DEFAULT_SIZE),
  _has_ownership(true) {
  allocate(DEFAULT_SIZE);
}

template <size_t DEFAULT_SIZE>
MallocAdapter<DEFAULT_SIZE>::~MallocAdapter() {
  if (_has_ownership) {
    deallocate();
  }
}

template <size_t DEFAULT_SIZE>
bool MallocAdapter<DEFAULT_SIZE>::allocate(size_t size) {
  if (NULL == _start) {
    _start = JfrCHeapObj::new_array<u1>(size);
    if (_start) {
      _pos = _start;
      _end = _start + size;
      _initial_size = size;
    }
  }
  return _start != NULL;
}

template <size_t DEFAULT_SIZE>
void MallocAdapter<DEFAULT_SIZE>::deallocate() {
  if (_start != NULL) {
    JfrCHeapObj::free(_start, (size_t)(_end - _start));
  }
}

template <size_t DEFAULT_SIZE>
bool MallocAdapter<DEFAULT_SIZE>::flush(size_t used, size_t requested) {
  if (!_has_ownership) {
    // can't just realloc a storage that we don't own
    return false;
  }
  assert(_start != NULL, "invariant");
  assert(used <= (size_t)(_end - _pos), "invariant");
  assert(_pos + used <= _end, "invariant");
  const size_t previous_storage_size = _end - _start;
  const size_t new_storage_size = used + requested + (previous_storage_size * 2);
  u1* const new_storage = JfrCHeapObj::new_array<u1>(new_storage_size);
  if (!new_storage) {
    return false;
  }
  const size_t previous_pos_offset = _pos - _start;
  // migrate in-flight data
  memcpy(new_storage, _start, previous_pos_offset + used);
  JfrCHeapObj::free(_start, previous_storage_size);
  _start = new_storage;
  _pos = _start + previous_pos_offset;
  _end = _start + new_storage_size;
  return true;
}

class NoOwnershipAdapter {
 private:
  u1* _start;
  u1* _pos;
  u1* _end;
  size_t _size;

 public:
  typedef u1 StorageType;
  NoOwnershipAdapter(u1* storage, size_t size) : _start(storage), _pos(storage), _end(storage + size), _size(size) {}
  NoOwnershipAdapter(u1* storage, Thread* thread) : _start(storage), _pos(storage), _end(storage), _size(0) {
    ShouldNotCallThis();
  }
  NoOwnershipAdapter(Thread* thread) : _start(NULL), _pos(NULL), _end(NULL), _size(0) {
    ShouldNotCallThis();
  }
  StorageType* storage() { return _start; }
  const u1* start() const { return _start; }
  u1* pos() { return _pos; }
  void commit(u1* position) { _pos = position; }
  const u1* end() const { return _end; }
  void release() {}
  bool flush(size_t used, size_t requested) {
    // don't flush/expand a buffer that is not our own
    _pos = _start;
    return true;
  }
};

#endif // SHARE_JFR_WRITERS_JFRSTORAGEADAPTER_HPP
