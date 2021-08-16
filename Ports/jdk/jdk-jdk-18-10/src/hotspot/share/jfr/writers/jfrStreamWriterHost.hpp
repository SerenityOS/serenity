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

#ifndef SHARE_JFR_WRITERS_JFRSTREAMWRITERHOST_HPP
#define SHARE_JFR_WRITERS_JFRSTREAMWRITERHOST_HPP

#include "jfr/utilities/jfrTypes.hpp"
#include "jfr/writers/jfrMemoryWriterHost.inline.hpp"

template <typename Adapter, typename AP> // Adapter and AllocationPolicy
class StreamWriterHost : public MemoryWriterHost<Adapter, AP> {
 public:
  typedef typename Adapter::StorageType StorageType;
 private:
  int64_t _stream_pos;
  fio_fd _fd;
  int64_t current_stream_position() const;

  void write_bytes(const u1* buf, intptr_t len);

 protected:
  StreamWriterHost(StorageType* storage, Thread* thread);
  StreamWriterHost(StorageType* storage, size_t size);
  StreamWriterHost(Thread* thread);
  bool accommodate(size_t used, size_t requested);
  void write_bytes(void* dest, const void* src, intptr_t len);
  void flush(size_t size);
  bool has_valid_fd() const;

 public:
  int64_t current_offset() const;
  void seek(int64_t offset);
  void flush();
  void write_unbuffered(const void* src, intptr_t len);
  bool is_valid() const;
  void close_fd();
  void reset(fio_fd fd);
};

#endif // SHARE_JFR_WRITERS_JFRSTREAMWRITERHOST_HPP
