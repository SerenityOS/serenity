/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016 SAP SE. All rights reserved.
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

#ifndef CPU_S390_JAVAFRAMEANCHOR_S390_HPP
#define CPU_S390_JAVAFRAMEANCHOR_S390_HPP

 public:

  // Each arch must define reset, save, restore.
  // These are used by objects that only care about:
  //  1 - initializing a new state (thread creation, javaCalls)
  //  2 - saving a current state (javaCalls)
  //  3 - restoring an old state (javaCalls).

  inline void clear(void) {
    // Clearing _last_Java_sp must be first.
    OrderAccess::release();
    _last_Java_sp = NULL;
    // Fence?
    OrderAccess::fence();

    _last_Java_pc = NULL;
  }

  inline void set(intptr_t* sp, address pc) {
    _last_Java_pc = pc;

    OrderAccess::release();
    _last_Java_sp = sp;
  }

  void copy(JavaFrameAnchor* src) {
    // In order to make sure the transition state is valid for "this"
    // we must clear _last_Java_sp before copying the rest of the new data.
    // Hack Alert: Temporary bugfix for 4717480/4721647
    // To act like previous version (pd_cache_state) don't NULL _last_Java_sp
    // unless the value is changing.
    //
    if (_last_Java_sp != src->_last_Java_sp) {
      OrderAccess::release();
      _last_Java_sp = NULL;
      OrderAccess::fence();
    }
    _last_Java_pc = src->_last_Java_pc;
    // Must be last so profiler will always see valid frame if has_last_frame() is true.

    OrderAccess::release();
    _last_Java_sp = src->_last_Java_sp;
  }

  // We don't have to flush registers, so the stack is always walkable.
  inline bool walkable(void) { return true; }
  inline void make_walkable(JavaThread* thread) { }

 public:

  // We don't have a frame pointer.
  intptr_t* last_Java_fp(void)        { return NULL; }

  intptr_t* last_Java_sp() const      { return _last_Java_sp; }
  void set_last_Java_sp(intptr_t* sp) { OrderAccess::release(); _last_Java_sp = sp; }

  address last_Java_pc(void)          { return _last_Java_pc; }

#endif // CPU_S390_JAVAFRAMEANCHOR_S390_HPP
