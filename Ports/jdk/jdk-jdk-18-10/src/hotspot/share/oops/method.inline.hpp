/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_METHOD_INLINE_HPP
#define SHARE_OOPS_METHOD_INLINE_HPP

#include "oops/method.hpp"

#include "runtime/atomic.hpp"

inline address Method::from_compiled_entry() const {
  return Atomic::load_acquire(&_from_compiled_entry);
}

inline address Method::from_interpreted_entry() const {
  return Atomic::load_acquire(&_from_interpreted_entry);
}

inline void Method::set_method_data(MethodData* data) {
  // The store into method must be released. On platforms without
  // total store order (TSO) the reference may become visible before
  // the initialization of data otherwise.
  Atomic::release_store(&_method_data, data);
}

inline CompiledMethod* volatile Method::code() const {
  assert( check_code(), "" );
  return Atomic::load_acquire(&_code);
}

// Write (bci, line number) pair to stream
inline void CompressedLineNumberWriteStream::write_pair_regular(int bci_delta, int line_delta) {
  // bci and line number does not compress into single byte.
  // Write out escape character and use regular compression for bci and line number.
  write_byte((jubyte)0xFF);
  write_signed_int(bci_delta);
  write_signed_int(line_delta);
}

inline void CompressedLineNumberWriteStream::write_pair_inline(int bci, int line) {
  int bci_delta = bci - _bci;
  int line_delta = line - _line;
  _bci = bci;
  _line = line;
  // Skip (0,0) deltas - they do not add information and conflict with terminator.
  if (bci_delta == 0 && line_delta == 0) return;
  // Check if bci is 5-bit and line number 3-bit unsigned.
  if (((bci_delta & ~0x1F) == 0) && ((line_delta & ~0x7) == 0)) {
    // Compress into single byte.
    jubyte value = ((jubyte) bci_delta << 3) | (jubyte) line_delta;
    // Check that value doesn't match escape character.
    if (value != 0xFF) {
      write_byte(value);
      return;
    }
  }
  write_pair_regular(bci_delta, line_delta);
}

inline void CompressedLineNumberWriteStream::write_pair(int bci, int line) {
  write_pair_inline(bci, line);
}

inline bool Method::has_compiled_code() const { return code() != NULL; }

inline bool Method::is_empty_method() const {
  return  code_size() == 1
      && *code_base() == Bytecodes::_return;
}

#endif // SHARE_OOPS_METHOD_INLINE_HPP
