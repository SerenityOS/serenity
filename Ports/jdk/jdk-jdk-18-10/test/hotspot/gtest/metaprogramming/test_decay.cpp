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
#include "metaprogramming/decay.hpp"
#include "metaprogramming/isSame.hpp"
#include "utilities/debug.hpp"

class TestDecay: AllStatic {
  class A: AllStatic {};

  typedef const volatile A cvA;
  typedef const volatile A& cvAref;
  typedef const volatile A* cvAptr;
  typedef const volatile A* const volatile cvAptrcv;
  typedef A& Aref;

  typedef Decay<cvAref>::type rr_cvAref;
  static const bool decay_cvAref_is_A = IsSame<rr_cvAref, A>::value;
  STATIC_ASSERT(decay_cvAref_is_A);

  typedef Decay<cvAptrcv>::type rr_cvAptrcv;
  static const bool decay_cvAptrcv_is_cvAptr = IsSame<rr_cvAptrcv, cvAptr>::value;
  STATIC_ASSERT(decay_cvAptrcv_is_cvAptr);
};
