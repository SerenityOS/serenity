/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "interpreter/bytecodeStream.hpp"
#include "interpreter/bytecodes.hpp"
#include "runtime/handles.inline.hpp"

Bytecodes::Code RawBytecodeStream::raw_next_special(Bytecodes::Code code) {
  assert(!is_last_bytecode(), "should have been checked");
  // set next bytecode position
  address bcp = RawBytecodeStream::bcp();
  address end = method()->code_base() + end_bci();
  int len = Bytecodes::raw_special_length_at(bcp, end);
  // Very large tableswitch or lookupswitch size can cause _next_bci to overflow.
  if (len <= 0 || (_bci > _end_bci - len) || (_bci - len >= _next_bci)) {
    code = Bytecodes::_illegal;
  } else {
    _next_bci += len;
    // set attributes
    _is_wide = false;
    // check for special (uncommon) cases
    if (code == Bytecodes::_wide) {
      if (bcp + 1 >= end) {
        code = Bytecodes::_illegal;
      } else {
        code = (Bytecodes::Code)bcp[1];
        _is_wide = true;
      }
    }
  }
  _raw_code = code;
  return code;
}

BaseBytecodeStream::BaseBytecodeStream(const methodHandle& method) : _method(method) {
  set_interval(0, _method->code_size());
  _is_raw = false;
}

#ifdef ASSERT
void BaseBytecodeStream::assert_raw_index_size(int size) const {
  if (raw_code() == Bytecodes::_invokedynamic && is_raw()) {
    // in raw mode, pretend indy is "bJJ__"
    assert(size == 2, "raw invokedynamic instruction has 2-byte index only");
  } else {
    bytecode().assert_index_size(size, raw_code(), is_wide());
  }
}

void BaseBytecodeStream::assert_raw_stream(bool want_raw) const {
  if (want_raw) {
    assert( is_raw(), "this function only works on raw streams");
  } else {
    assert(!is_raw(), "this function only works on non-raw streams");
  }
}
#endif //ASSERT
