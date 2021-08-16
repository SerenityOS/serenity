/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.hpp"
#include "metaprogramming/isSame.hpp"
#include "utilities/debug.hpp"

class IsSameTest: AllStatic {
  class A: AllStatic {};
  class B: AllStatic {};

  static const bool const_A_is_A = IsSame<const A, A>::value;
  STATIC_ASSERT(!const_A_is_A);

  static const bool volatile_A_is_A = IsSame<volatile A, A>::value;
  STATIC_ASSERT(!volatile_A_is_A);

  static const bool Aref_is_A = IsSame<A&, A>::value;
  STATIC_ASSERT(!Aref_is_A);

  static const bool Aptr_is_A = IsSame<A*, A>::value;
  STATIC_ASSERT(!Aptr_is_A);

  static const bool A_is_B = IsSame<A, B>::value;
  STATIC_ASSERT(!A_is_B);

  static const bool A_is_A = IsSame<A, A>::value;
  STATIC_ASSERT(A_is_A);
};
