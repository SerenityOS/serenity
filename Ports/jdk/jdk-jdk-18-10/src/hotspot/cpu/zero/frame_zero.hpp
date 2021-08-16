/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ZERO_FRAME_ZERO_HPP
#define CPU_ZERO_FRAME_ZERO_HPP

// A frame represents a physical stack frame on the Zero stack.

 public:
  enum {
    pc_return_offset = 0
  };

  // Constructor
 public:
  frame(ZeroFrame* zeroframe, intptr_t* sp);

 private:
  ZeroFrame* _zeroframe;

 public:
  const ZeroFrame *zeroframe() const {
    return _zeroframe;
  }

  intptr_t* fp() const {
    return (intptr_t *) zeroframe();
  }

  inline interpreterState get_interpreterState() const;

 public:
  const EntryFrame *zero_entryframe() const {
    return zeroframe()->as_entry_frame();
  }
  const InterpreterFrame *zero_interpreterframe() const {
    return zeroframe()->as_interpreter_frame();
  }

 public:
  bool is_fake_stub_frame() const;

 public:
  frame sender_for_nonentry_frame(RegisterMap* map) const;

 public:
  void zero_print_on_error(int           index,
                           outputStream* st,
                           char*         buf,
                           int           buflen) const;

  static jint interpreter_frame_expression_stack_direction() { return -1; }

#endif // CPU_ZERO_FRAME_ZERO_HPP
