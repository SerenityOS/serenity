/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "metaprogramming/removeExtent.hpp"
#include "metaprogramming/isSame.hpp"
#include "utilities/debug.hpp"

class RemoveExtentTest {
  class A: AllStatic {};

  typedef A* Aptr;
  typedef A Aarr[];
  typedef A Aarr10[10];
  typedef const A cA;
  typedef const A* cAptr;
  typedef const A cAarr[];
  typedef const A cAarr10[10];

  typedef RemoveExtent<Aptr>::type ra_Aptr;
  static const bool ra_Aptr_is_Aptr = IsSame<ra_Aptr, Aptr>::value;
  STATIC_ASSERT(ra_Aptr_is_Aptr);

  typedef RemoveExtent<Aarr>::type ra_Aarr;
  static const bool ra_Aarr_is_A = IsSame<ra_Aarr, A>::value;
  STATIC_ASSERT(ra_Aarr_is_A);

  typedef RemoveExtent<Aarr10>::type ra_Aarr10;
  static const bool ra_Aarr10_is_A = IsSame<ra_Aarr10, A>::value;
  STATIC_ASSERT(ra_Aarr10_is_A);

  typedef RemoveExtent<cAptr>::type ra_cAptr;
  static const bool ra_cAptr_is_cAptr = IsSame<ra_cAptr, cAptr>::value;
  STATIC_ASSERT(ra_cAptr_is_cAptr);

  typedef RemoveExtent<cAarr>::type ra_cAarr;
  static const bool ra_cAarr_is_cA = IsSame<ra_cAarr, cA>::value;
  STATIC_ASSERT(ra_cAarr_is_cA);

  typedef RemoveExtent<cAarr10>::type ra_cAarr10;
  static const bool ra_cAarr10_is_cA = IsSame<ra_cAarr10, cA>::value;
  STATIC_ASSERT(ra_cAarr10_is_cA);
};
