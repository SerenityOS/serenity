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

#ifndef SHARE_JFR_WRITERS_JFRSTREAMWRITERHOST_INLINE_HPP
#define SHARE_JFR_WRITERS_JFRSTREAMWRITERHOST_INLINE_HPP

#include "jfr/writers/jfrStreamWriterHost.hpp"

#include "runtime/os.hpp"

template <typename Adapter, typename AP>
StreamWriterHost<Adapter, AP>::StreamWriterHost(typename Adapter::StorageType* storage, Thread* thread) :
  MemoryWriterHost<Adapter, AP>(storage, thread), _stream_pos(0), _fd(invalid_fd) {
}

template <typename Adapter, typename AP>
StreamWriterHost<Adapter, AP>::StreamWriterHost(typename Adapter::StorageType* storage, size_t size) :
  MemoryWriterHost<Adapter, AP>(storage, size), _stream_pos(0), _fd(invalid_fd) {
}

template <typename Adapter, typename AP>
StreamWriterHost<Adapter, AP>::StreamWriterHost(Thread* thread) :
  MemoryWriterHost<Adapter, AP>(thread), _stream_pos(0), _fd(invalid_fd) {
}

template <typename Adapter, typename AP>
inline int64_t StreamWriterHost<Adapter, AP>::current_stream_position() const {
  return this->used_offset() + _stream_pos;
}

template <typename Adapter, typename AP>
inline bool StreamWriterHost<Adapter, AP>::accommodate(size_t used, size_t requested) {
  if (used > 0) {
    this->flush(used);
  }
  assert(this->used_size() == 0, "invariant");
  if (this->available_size() >= requested) {
    return true;
  }
  return StorageHost<Adapter, AP>::accommodate(0, requested);
}

template <typename Adapter, typename AP>
inline void StreamWriterHost<Adapter, AP>::write_bytes(void* dest, const void* buf, intptr_t len) {
  assert(len >= 0, "invariant");
  if (len > (intptr_t)this->available_size()) {
    this->write_unbuffered(buf, len);
    return;
  }
  MemoryWriterHost<Adapter, AP>::write_bytes(dest, buf, len);
}

template <typename Adapter, typename AP>
inline void StreamWriterHost<Adapter, AP>::write_bytes(const u1* buf, intptr_t len) {
  assert(len >= 0, "invariant");
  while (len > 0) {
    const unsigned int nBytes = len > INT_MAX ? INT_MAX : (unsigned int)len;
    const ssize_t num_written = (ssize_t)os::write(_fd, buf, nBytes);
    guarantee(num_written > 0, "Nothing got written, or os::write() failed");
    _stream_pos += num_written;
    len -= num_written;
    buf += num_written;
  }
}

template <typename Adapter, typename AP>
inline void StreamWriterHost<Adapter, AP>::flush(size_t size) {
  assert(size > 0, "invariant");
  assert(this->is_valid(), "invariant");
  this->write_bytes(this->start_pos(), (intptr_t)size);
  StorageHost<Adapter, AP>::reset();
  assert(0 == this->used_offset(), "invariant");
}

template <typename Adapter, typename AP>
inline bool StreamWriterHost<Adapter, AP>::has_valid_fd() const {
  return invalid_fd != _fd;
}

template <typename Adapter, typename AP>
inline int64_t StreamWriterHost<Adapter, AP>::current_offset() const {
  return current_stream_position();
}

template <typename Adapter, typename AP>
void StreamWriterHost<Adapter, AP>::seek(int64_t offset) {
  this->flush();
  assert(0 == this->used_offset(), "can only seek from beginning");
  _stream_pos = os::seek_to_file_offset(_fd, offset);
}

template <typename Adapter, typename AP>
void StreamWriterHost<Adapter, AP>::flush() {
  if (this->is_valid()) {
    const size_t used = this->used_size();
    if (used > 0) {
      this->flush(used);
    }
  }
}

template <typename Adapter, typename AP>
void StreamWriterHost<Adapter, AP>::write_unbuffered(const void* buf, intptr_t len) {
  this->flush();
  assert(0 == this->used_offset(), "can only seek from beginning");
  this->write_bytes((const u1*)buf, len);
}

template <typename Adapter, typename AP>
inline bool StreamWriterHost<Adapter, AP>::is_valid() const {
  return has_valid_fd();
}

template <typename Adapter, typename AP>
inline void StreamWriterHost<Adapter, AP>::close_fd() {
  assert(this->has_valid_fd(), "closing invalid fd!");
  os::close(_fd);
  _fd = invalid_fd;
}

template <typename Adapter, typename AP>
inline void StreamWriterHost<Adapter, AP>::reset(fio_fd fd) {
  assert(!this->has_valid_fd(), "invariant");
  _fd = fd;
  _stream_pos = 0;
  this->hard_reset();
}

#endif // SHARE_JFR_WRITERS_JFRSTREAMWRITERHOST_INLINE_HPP
