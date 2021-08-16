/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef SHARE_GC_Z_ZADDRESS_HPP
#define SHARE_GC_Z_ZADDRESS_HPP

#include "memory/allocation.hpp"

class ZAddress : public AllStatic {
  friend class ZAddressTest;

private:
  static void set_good_mask(uintptr_t mask);

public:
  static void initialize();

  static void flip_to_marked();
  static void flip_to_remapped();

  static bool is_null(uintptr_t value);
  static bool is_bad(uintptr_t value);
  static bool is_good(uintptr_t value);
  static bool is_good_or_null(uintptr_t value);
  static bool is_weak_bad(uintptr_t value);
  static bool is_weak_good(uintptr_t value);
  static bool is_weak_good_or_null(uintptr_t value);
  static bool is_marked(uintptr_t value);
  static bool is_marked_or_null(uintptr_t value);
  static bool is_finalizable(uintptr_t value);
  static bool is_finalizable_good(uintptr_t value);
  static bool is_remapped(uintptr_t value);
  static bool is_in(uintptr_t value);

  static uintptr_t offset(uintptr_t value);
  static uintptr_t good(uintptr_t value);
  static uintptr_t good_or_null(uintptr_t value);
  static uintptr_t finalizable_good(uintptr_t value);
  static uintptr_t marked(uintptr_t value);
  static uintptr_t marked0(uintptr_t value);
  static uintptr_t marked1(uintptr_t value);
  static uintptr_t remapped(uintptr_t value);
  static uintptr_t remapped_or_null(uintptr_t value);
};

#endif // SHARE_GC_Z_ZADDRESS_HPP
