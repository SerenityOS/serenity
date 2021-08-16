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

#ifndef SHARE_JFR_UTILITIES_JFRBLOB_HPP
#define SHARE_JFR_UTILITIES_JFRBLOB_HPP

#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrRefCountPointer.hpp"

class JfrBlob;
typedef RefCountPointer<JfrBlob, MultiThreadedRefCounter> JfrBlobReference;
typedef RefCountHandle<JfrBlobReference> JfrBlobHandle;

class JfrBlob : public JfrCHeapObj {
  template <typename, typename>
  friend class RefCountPointer;
 private:
  const u1* const _data;
  JfrBlobHandle _next;
  const size_t _size;
  mutable bool _written;

  JfrBlob(const u1* data, size_t size);
  ~JfrBlob();

 public:
  void set_next(const JfrBlobHandle& ref);
  void reset_write_state() const;
  static JfrBlobHandle make(const u1* data, size_t size);
  template <typename Writer>
  void write(Writer& writer) const {
    writer.write_bytes(_data, _size);
    if (_next.valid()) {
      _next->write(writer);
    }
  }
  template <typename Writer>
  void exclusive_write(Writer& writer) const {
    if (_written) {
      return;
    }
    writer.write_bytes(_data, _size);
    _written = true;
    if (_next.valid()) {
      _next->exclusive_write(writer);
    }
  }
};

#endif // SHARE_JFR_UTILITIES_JFRBLOB_HPP
