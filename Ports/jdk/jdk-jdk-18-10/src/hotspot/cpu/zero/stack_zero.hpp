/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2008, 2009, 2010 Red Hat, Inc.
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

#ifndef CPU_ZERO_STACK_ZERO_HPP
#define CPU_ZERO_STACK_ZERO_HPP

#include "utilities/align.hpp"
#include "utilities/sizes.hpp"

class ZeroStack {
 private:
  intptr_t *_base; // the last available word
  intptr_t *_top;  // the word past the end of the stack
  intptr_t *_sp;   // the top word on the stack

 private:
  int _shadow_pages_size; // how much ABI stack must we keep free?

 public:
  ZeroStack();

  bool needs_setup() const {
    return _base == NULL;
  }

  int suggest_size(Thread *thread) const;

  void setup(void *mem, size_t size) {
    assert(needs_setup(), "already set up");
    assert(!(size & WordAlignmentMask), "unaligned");

    _base = (intptr_t *) mem;
    _top  = _base + (size >> LogBytesPerWord);
    _sp   = _top;
  }
  void teardown() {
    assert(!needs_setup(), "not set up");
    assert(_sp == _top, "stuff on stack at teardown");

    _base = NULL;
    _top  = NULL;
    _sp   = NULL;
  }

  intptr_t *sp() const {
    return _sp;
  }
  void set_sp(intptr_t *new_sp) {
    assert(_top >= new_sp && new_sp >= _base, "bad stack pointer");
    _sp = new_sp;
  }

  int total_words() const {
    return _top - _base;
  }
  int available_words() const {
    return _sp - _base;
  }

  void push(intptr_t value) {
    assert(_sp > _base, "stack overflow");
    *(--_sp) = value;
  }
  intptr_t pop() {
    assert(_sp < _top, "stack underflow");
    return *(_sp++);
  }

  void *alloc(size_t size) {
    int count = align_up(size, wordSize) >> LogBytesPerWord;
    assert(count <= available_words(), "stack overflow");
    return _sp -= count;
  }

  int shadow_pages_size() const {
    return _shadow_pages_size;
  }
  int abi_stack_available(Thread *thread) const;

 public:
  void overflow_check(int required_words, TRAPS);
  static void handle_overflow(TRAPS);

 public:
  void zap(int c) PRODUCT_RETURN;

 public:
  static ByteSize base_offset() {
    return byte_offset_of(ZeroStack, _base);
  }
  static ByteSize top_offset() {
    return byte_offset_of(ZeroStack, _top);
  }
  static ByteSize sp_offset() {
    return byte_offset_of(ZeroStack, _sp);
  }
};


class EntryFrame;
class InterpreterFrame;
class FakeStubFrame;

//
// |  ...               |
// +--------------------+  ------------------
// |  ...               |       low addresses
// | frame_type         |
// | next_frame         |      high addresses
// +--------------------+  ------------------
// |  ...               |

class ZeroFrame {
  friend class frame;
  friend class ZeroStackPrinter;

 protected:
  ZeroFrame() {
    ShouldNotCallThis();
  }

  enum Layout {
    next_frame_off,
    frame_type_off,
    jf_header_words
  };

  enum FrameType {
    ENTRY_FRAME = 1,
    INTERPRETER_FRAME,
    FAKE_STUB_FRAME
  };

 protected:
  intptr_t *addr_of_word(int offset) const {
    return (intptr_t *) this - offset;
  }
  intptr_t value_of_word(int offset) const {
    return *addr_of_word(offset);
  }

 public:
  ZeroFrame *next() const {
    return (ZeroFrame *) value_of_word(next_frame_off);
  }

 protected:
  FrameType type() const {
    return (FrameType) value_of_word(frame_type_off);
  }

 public:
  bool is_entry_frame() const {
    return type() == ENTRY_FRAME;
  }
  bool is_interpreter_frame() const {
    return type() == INTERPRETER_FRAME;
  }
  bool is_fake_stub_frame() const {
    return type() == FAKE_STUB_FRAME;
  }

 public:
  EntryFrame *as_entry_frame() const {
    assert(is_entry_frame(), "should be");
    return (EntryFrame *) this;
  }
  InterpreterFrame *as_interpreter_frame() const {
    assert(is_interpreter_frame(), "should be");
    return (InterpreterFrame *) this;
  }
  FakeStubFrame *as_fake_stub_frame() const {
    assert(is_fake_stub_frame(), "should be");
    return (FakeStubFrame *) this;
  }

 public:
  void identify_word(int   frame_index,
                     int   offset,
                     char* fieldbuf,
                     char* valuebuf,
                     int   buflen) const;

 protected:
  void identify_vp_word(int       frame_index,
                        intptr_t* addr,
                        intptr_t* monitor_base,
                        intptr_t* stack_base,
                        char*     fieldbuf,
                        int       buflen) const;
};

#endif // CPU_ZERO_STACK_ZERO_HPP
