/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_CLASSFILESTREAM_HPP
#define SHARE_CLASSFILE_CLASSFILESTREAM_HPP

#include "memory/allocation.hpp"
#include "utilities/bytes.hpp"
#include "utilities/exceptions.hpp"

// Input stream for reading .class file
//
// The entire input stream is present in a buffer allocated by the caller.
// The caller is responsible for deallocating the buffer and for using
// ResourceMarks appropriately when constructing streams.

class ClassPathEntry;

class ClassFileStream: public ResourceObj {
 private:
  const u1* const _buffer_start; // Buffer bottom
  const u1* const _buffer_end;   // Buffer top (one past last element)
  mutable const u1* _current;    // Current buffer position
  const char* const _source;     // Source of stream (directory name, ZIP/JAR archive name)
  bool _need_verify;             // True if verification is on for the class file
  bool _from_boot_loader_modules_image;  // True if this was created by ClassPathImageEntry.
  void truncated_file_error(TRAPS) const ;

 protected:
  const u1* clone_buffer() const;
  const char* const clone_source() const;

 public:
  static const bool verify;

  ClassFileStream(const u1* buffer,
                  int length,
                  const char* source,
                  bool verify_stream = verify,  // to be verified by default
                  bool from_boot_loader_modules_image = false);

  virtual const ClassFileStream* clone() const;

  // Buffer access
  const u1* buffer() const { return _buffer_start; }
  int length() const { return _buffer_end - _buffer_start; }
  const u1* current() const { return _current; }
  void set_current(const u1* pos) const {
    assert(pos >= _buffer_start && pos <= _buffer_end, "invariant");
    _current = pos;
  }

  // for relative positioning
  juint current_offset() const {
    return (juint)(_current - _buffer_start);
  }
  const char* source() const { return _source; }
  bool need_verify() const { return _need_verify; }
  void set_verify(bool flag) { _need_verify = flag; }
  bool from_boot_loader_modules_image() const { return _from_boot_loader_modules_image; }

  void check_truncated_file(bool b, TRAPS) const {
    if (b) {
      truncated_file_error(THREAD);
    }
  }

  void guarantee_more(int size, TRAPS) const {
    size_t remaining = (size_t)(_buffer_end - _current);
    unsigned int usize = (unsigned int)size;
    check_truncated_file(usize > remaining, CHECK);
  }

  // Read u1 from stream
  u1 get_u1_fast() const {
    return *_current++;
  }
  u1 get_u1(TRAPS) const {
    if (_need_verify) {
      guarantee_more(1, CHECK_0);
    } else {
      assert(1 <= _buffer_end - _current, "buffer overflow");
    }
    return get_u1_fast();
  }

  // Read u2 from stream
  u2 get_u2_fast() const {
    u2 res = Bytes::get_Java_u2((address)_current);
    _current += 2;
    return res;
  }
  u2 get_u2(TRAPS) const {
    if (_need_verify) {
      guarantee_more(2, CHECK_0);
    } else {
      assert(2 <= _buffer_end - _current, "buffer overflow");
    }
    return get_u2_fast();
  }

  // Read u4 from stream
  u4 get_u4_fast() const {
    u4 res = Bytes::get_Java_u4((address)_current);
    _current += 4;
    return res;
  }

  // Read u8 from stream
  u8 get_u8_fast() const {
    u8 res = Bytes::get_Java_u8((address)_current);
    _current += 8;
    return res;
  }

  // Skip length elements from stream
  void skip_u1(int length, TRAPS) const {
    if (_need_verify) {
      guarantee_more(length, CHECK);
    }
    skip_u1_fast(length);
  }
  void skip_u1_fast(int length) const {
    _current += length;
  }

  void skip_u2_fast(int length) const {
    _current += 2 * length;
  }

  void skip_u4_fast(int length) const {
    _current += 4 * length;
  }

  // Tells whether eos is reached
  bool at_eos() const { return _current == _buffer_end; }

  uint64_t compute_fingerprint() const;
};

#endif // SHARE_CLASSFILE_CLASSFILESTREAM_HPP
