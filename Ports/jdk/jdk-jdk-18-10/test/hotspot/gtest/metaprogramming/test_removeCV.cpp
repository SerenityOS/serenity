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
#include "metaprogramming/removeCV.hpp"
#include "metaprogramming/isSame.hpp"
#include "utilities/debug.hpp"

class RemoveCVTest {
  class A: AllStatic {};

  typedef const A cA;
  typedef volatile A vA;
  typedef const volatile A cvA;
  typedef A* Aptr;
  typedef const A* cAptr;
  typedef A* const Aptrc;
  typedef const A* const cAptrc;
  typedef A& Aref;
  typedef const A& cAref;

  typedef RemoveCV<A>::type rcv_A;
  static const bool rcv_A_is_A = IsSame<rcv_A, A>::value;
  STATIC_ASSERT(rcv_A_is_A);

  typedef RemoveCV<cA>::type rcv_cA;
  static const bool rcv_cA_is_A = IsSame<rcv_cA, A>::value;
  STATIC_ASSERT(rcv_cA_is_A);

  typedef RemoveCV<vA>::type rcv_vA;
  static const bool rcv_vA_is_A = IsSame<rcv_vA, A>::value;
  STATIC_ASSERT(rcv_vA_is_A);

  typedef RemoveCV<cvA>::type rcv_cvA;
  static const bool rcv_cvA_is_A = IsSame<rcv_cvA, A>::value;
  STATIC_ASSERT(rcv_cvA_is_A);

  typedef RemoveCV<cAptr>::type rcv_cAptr;
  static const bool rcv_cAptr_is_cAptr = IsSame<rcv_cAptr, cAptr>::value;
  STATIC_ASSERT(rcv_cAptr_is_cAptr);

  typedef RemoveCV<Aptrc>::type rcv_Aptrc;
  static const bool rcv_Aptrc_is_Aptr = IsSame<rcv_Aptrc, Aptr>::value;
  STATIC_ASSERT(rcv_Aptrc_is_Aptr);

  typedef RemoveCV<cAptrc>::type rcv_cAptrc;
  static const bool rcv_cAptrc_is_cAptr = IsSame<rcv_cAptrc, cAptr>::value;
  STATIC_ASSERT(rcv_cAptrc_is_cAptr);

  typedef RemoveCV<cAref>::type rcv_cAref;
  static const bool rcv_cAref_is_cAref = IsSame<rcv_cAref, cAref>::value;
  STATIC_ASSERT(rcv_cAref_is_cAref);
};
