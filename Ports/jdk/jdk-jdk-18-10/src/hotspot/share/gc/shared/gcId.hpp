/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_GCID_HPP
#define SHARE_GC_SHARED_GCID_HPP

#include "memory/allocation.hpp"

class GCId : public AllStatic {
private:
  friend class GCIdMark;

  static uint _next_id;
  static const uint UNDEFINED = (uint)-1;
  static uint create();

public:
  // Returns the currently active GC id. Asserts that there is an active GC id.
  static uint current();
  // Same as current() but can return undefined() if no GC id is currently active
  static uint current_or_undefined();
  // Returns the next expected GCId.
  static uint peek();
  static uint undefined() { return UNDEFINED; }
  static size_t print_prefix(char* buf, size_t len);
};

class GCIdMark : public StackObj {
private:
  const uint _previous_gc_id;

public:
  GCIdMark();
  GCIdMark(uint gc_id);
  ~GCIdMark();
};

#endif // SHARE_GC_SHARED_GCID_HPP
