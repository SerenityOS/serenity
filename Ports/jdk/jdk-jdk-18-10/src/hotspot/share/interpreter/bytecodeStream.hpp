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

#ifndef SHARE_INTERPRETER_BYTECODESTREAM_HPP
#define SHARE_INTERPRETER_BYTECODESTREAM_HPP

#include "interpreter/bytecode.hpp"
#include "memory/allocation.hpp"
#include "oops/method.hpp"
#include "utilities/bytes.hpp"

// A BytecodeStream is used for fast iteration over the bytecodes
// of a Method*.
//
// Usage:
//
// BytecodeStream s(method);
// Bytecodes::Code c;
// while ((c = s.next()) >= 0) {
//   ...
// }

// A RawBytecodeStream is a simple version of BytecodeStream.
// It is used ONLY when we know the bytecodes haven't been rewritten
// yet, such as in the rewriter or the verifier.

// Here is the common base class for both RawBytecodeStream and BytecodeStream:
class BaseBytecodeStream: StackObj {
 protected:
  // stream buffer
  methodHandle    _method;                       // read from method directly

  // reading position
  int             _bci;                          // bci if current bytecode
  int             _next_bci;                     // bci of next bytecode
  int             _end_bci;                      // bci after the current iteration interval

  // last bytecode read
  Bytecodes::Code _raw_code;
  bool            _is_wide;
  bool            _is_raw;                       // false in 'cooked' BytecodeStream

  // Construction
  BaseBytecodeStream(const methodHandle& method);

 public:
  // Iteration control
  void set_interval(int beg_bci, int end_bci) {
    // iterate over the interval [beg_bci, end_bci)
    assert(0 <= beg_bci && beg_bci <= method()->code_size(), "illegal beg_bci");
    assert(0 <= end_bci && end_bci <= method()->code_size(), "illegal end_bci");
    // setup of iteration pointers
    _bci      = beg_bci;
    _next_bci = beg_bci;
    _end_bci  = end_bci;
  }
  void set_start   (int beg_bci) {
    set_interval(beg_bci, _method->code_size());
  }

  bool is_raw() const { return _is_raw; }

  // Stream attributes
  const methodHandle& method() const             { return _method; }

  int             bci() const                    { return _bci; }
  int             next_bci() const               { return _next_bci; }
  int             end_bci() const                { return _end_bci; }

  Bytecodes::Code raw_code() const               { return _raw_code; }
  bool            is_wide() const                { return _is_wide; }
  int             instruction_size() const       { return (_next_bci - _bci); }
  bool            is_last_bytecode() const       { return _next_bci >= _end_bci; }

  address         bcp() const                    { return method()->code_base() + _bci; }
  Bytecode        bytecode() const               { return Bytecode(_method(), bcp()); }

  // State changes
  void            set_next_bci(int bci)          { assert(0 <= bci && bci <= method()->code_size(), "illegal bci"); _next_bci = bci; }

  // Bytecode-specific attributes
  int             dest() const                   { return bci() + bytecode().get_offset_s2(raw_code()); }
  int             dest_w() const                 { return bci() + bytecode().get_offset_s4(raw_code()); }

  // One-byte indices.
  int             get_index_u1() const           { assert_raw_index_size(1); return *(jubyte*)(bcp()+1); }

 protected:
  void assert_raw_index_size(int size) const NOT_DEBUG_RETURN;
  void assert_raw_stream(bool want_raw) const NOT_DEBUG_RETURN;
};

class RawBytecodeStream: public BaseBytecodeStream {
 public:
  // Construction
  RawBytecodeStream(const methodHandle& method) : BaseBytecodeStream(method) {
    _is_raw = true;
  }

 public:
  // Iteration
  // Use raw_next() rather than next() for faster method reference
  Bytecodes::Code raw_next() {
    Bytecodes::Code code;
    // set reading position
    _bci = _next_bci;
    assert(!is_last_bytecode(), "caller should check is_last_bytecode()");

    address bcp = this->bcp();
    code        = Bytecodes::code_or_bp_at(bcp);

    // set next bytecode position
    int len = Bytecodes::length_for(code);
    if (len > 0 && (_bci <= _end_bci - len)) {
      assert(code != Bytecodes::_wide && code != Bytecodes::_tableswitch
             && code != Bytecodes::_lookupswitch, "can't be special bytecode");
      _is_wide = false;
      _next_bci += len;
      if (_next_bci <= _bci) { // Check for integer overflow
        code = Bytecodes::_illegal;
      }
      _raw_code = code;
      return code;
    } else {
      return raw_next_special(code);
    }
  }
  Bytecodes::Code raw_next_special(Bytecodes::Code code);

  // Unsigned indices, widening, with no swapping of bytes
  int             get_index() const          { return (is_wide()) ? get_index_u2_raw(bcp() + 2) : get_index_u1(); }
  // Get an unsigned 2-byte index, with no swapping of bytes.
  int             get_index_u2() const       { assert(!is_wide(), ""); return get_index_u2_raw(bcp() + 1);  }

 private:
  int get_index_u2_raw(address p) const {
    assert_raw_index_size(2); assert_raw_stream(true);
    return Bytes::get_Java_u2(p);
  }
};

// In BytecodeStream, non-java bytecodes will be translated into the
// corresponding java bytecodes.

class BytecodeStream: public BaseBytecodeStream {
  Bytecodes::Code _code;

 public:
  // Construction
  BytecodeStream(const methodHandle& method) : BaseBytecodeStream(method) { }

  BytecodeStream(const methodHandle& method, int bci) : BaseBytecodeStream(method) {
    set_start(bci);
  }

  // Iteration
  Bytecodes::Code next() {
    Bytecodes::Code raw_code, code;
    // set reading position
    _bci = _next_bci;
    if (is_last_bytecode()) {
      // indicate end of bytecode stream
      raw_code = code = Bytecodes::_illegal;
    } else {
      // get bytecode
      address bcp = this->bcp();
      raw_code = Bytecodes::code_at(_method(), bcp);
      code = Bytecodes::java_code(raw_code);
      // set next bytecode position
      //
      // note that we cannot advance before having the
      // tty bytecode otherwise the stepping is wrong!
      // (carefull: length_for(...) must be used first!)
      int len = Bytecodes::length_for(code);
      if (len == 0) len = Bytecodes::length_at(_method(), bcp);
      if (len <= 0 || (_bci > _end_bci - len) || (_bci - len >= _next_bci)) {
        raw_code = code = Bytecodes::_illegal;
      } else {
        _next_bci  += len;
        assert(_bci < _next_bci, "length must be > 0");
        // set attributes
        _is_wide      = false;
        // check for special (uncommon) cases
        if (code == Bytecodes::_wide) {
          raw_code = (Bytecodes::Code)bcp[1];
          code = raw_code;  // wide BCs are always Java-normal
          _is_wide = true;
        }
        assert(Bytecodes::is_java_code(code), "sanity check");
      }
    }
    _raw_code = raw_code;
    _code = code;
    return _code;
  }

  Bytecodes::Code code() const                   { return _code; }

  // Unsigned indices, widening
  int             get_index() const              { return is_wide() ? bytecode().get_index_u2(raw_code(), true) : get_index_u1(); }
  // Get an unsigned 2-byte index, swapping the bytes if necessary.
  int             get_index_u2() const           { assert_raw_stream(false);
                                                   return bytecode().get_index_u2(raw_code(), false); }
  // Get an unsigned 2-byte index in native order.
  int             get_index_u2_cpcache() const   { assert_raw_stream(false);
                                                   return bytecode().get_index_u2_cpcache(raw_code()); }
  int             get_index_u4() const           { assert_raw_stream(false);
                                                   return bytecode().get_index_u4(raw_code()); }
  bool            has_index_u4() const           { return bytecode().has_index_u4(raw_code()); }
};

#endif // SHARE_INTERPRETER_BYTECODESTREAM_HPP
