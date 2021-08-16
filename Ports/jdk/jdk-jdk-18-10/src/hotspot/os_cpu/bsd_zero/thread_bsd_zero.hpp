/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008, 2009, 2010 Red Hat, Inc.
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

#ifndef OS_CPU_BSD_ZERO_THREAD_BSD_ZERO_HPP
#define OS_CPU_BSD_ZERO_THREAD_BSD_ZERO_HPP

 private:
  ZeroStack  _zero_stack;
  ZeroFrame* _top_zero_frame;

  void pd_initialize() {
    _top_zero_frame = NULL;
  }

 public:
  ZeroStack *zero_stack() {
    return &_zero_stack;
  }

 public:
  ZeroFrame *top_zero_frame() {
    return _top_zero_frame;
  }
  void push_zero_frame(ZeroFrame *frame) {
    *(ZeroFrame **) frame = _top_zero_frame;
    _top_zero_frame = frame;
  }
  void pop_zero_frame() {
    zero_stack()->set_sp((intptr_t *) _top_zero_frame + 1);
    _top_zero_frame = *(ZeroFrame **) _top_zero_frame;
  }

 public:
  static ByteSize zero_stack_offset() {
    return byte_offset_of(JavaThread, _zero_stack);
  }
  static ByteSize top_zero_frame_offset() {
    return byte_offset_of(JavaThread, _top_zero_frame);
  }

 public:
  void set_last_Java_frame() {
    set_last_Java_frame(top_zero_frame(), zero_stack()->sp());
  }
  void reset_last_Java_frame() {
    frame_anchor()->zap();
  }
  void set_last_Java_frame(ZeroFrame* fp, intptr_t* sp) {
    frame_anchor()->set(sp, NULL, fp);
  }

 public:
  ZeroFrame* last_Java_fp() {
    return frame_anchor()->last_Java_fp();
  }

 private:
  frame pd_last_frame();

 public:
  static ByteSize last_Java_fp_offset() {
    return byte_offset_of(JavaThread, _anchor) +
      JavaFrameAnchor::last_Java_fp_offset();
  }

 public:
  // Check for pending suspend requests and pending asynchronous
  // exceptions.  There are separate accessors for these, but
  // _suspend_flags is volatile so using them would be unsafe.
  bool has_special_condition_for_native_trans() {
    return _suspend_flags != 0;
  }

 public:
  bool pd_get_top_frame_for_signal_handler(frame* fr_addr,
                                           void* ucontext,
                                           bool isInJava) {
    ShouldNotCallThis();
    return false;
  }

#endif // OS_CPU_BSD_ZERO_THREAD_BSD_ZERO_HPP
