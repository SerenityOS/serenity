/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2008, 2010 Red Hat, Inc.
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

#ifndef CPU_ZERO_ENTRYFRAME_ZERO_HPP
#define CPU_ZERO_ENTRYFRAME_ZERO_HPP

#include "runtime/javaCalls.hpp"
#include "stack_zero.hpp"

// |  ...               |
// +--------------------+  ------------------
// | parameter n-1      |       low addresses
// |  ...               |
// | parameter 0        |
// | call_wrapper       |
// | frame_type         |
// | next_frame         |      high addresses
// +--------------------+  ------------------
// |  ...               |

class EntryFrame : public ZeroFrame {
 private:
  EntryFrame() : ZeroFrame() {
    ShouldNotCallThis();
  }

 protected:
  enum Layout {
    call_wrapper_off = jf_header_words,
    header_words
  };

 public:
  static EntryFrame *build(const intptr_t*  parameters,
                           int              parameter_words,
                           JavaCallWrapper* call_wrapper,
                           TRAPS);
 public:
  JavaCallWrapper **call_wrapper() const {
    return (JavaCallWrapper **) addr_of_word(call_wrapper_off);
  }

 public:
  void identify_word(int   frame_index,
                     int   offset,
                     char* fieldbuf,
                     char* valuebuf,
                     int   buflen) const;
};

#endif // CPU_ZERO_ENTRYFRAME_ZERO_HPP
