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
#include "metaprogramming/isIntegral.hpp"
#include "utilities/debug.hpp"

class IsIntegralTest: AllStatic {
  class A: AllStatic {};

  static const bool ii_voidptr = IsIntegral<void*>::value;
  STATIC_ASSERT(!ii_voidptr);

  static const bool ii_Aptr = IsIntegral<A*>::value;
  STATIC_ASSERT(!ii_Aptr);

  static const bool ii_cAptr = IsIntegral<const A*>::value;
  STATIC_ASSERT(!ii_cAptr);

  static const bool ii_vAptr = IsIntegral<volatile A*>::value;
  STATIC_ASSERT(!ii_vAptr);

  static const bool ii_Avptr = IsIntegral<A* volatile>::value;
  STATIC_ASSERT(!ii_Avptr);

  static const bool ii_intptrt = IsIntegral<intptr_t>::value;
  STATIC_ASSERT(ii_intptrt);

  static const bool ii_char = IsIntegral<char>::value;
  STATIC_ASSERT(ii_char);

  static const bool ii_cintptrt = IsIntegral<const intptr_t>::value;
  STATIC_ASSERT(ii_cintptrt);

  static const bool ii_vintptrt = IsIntegral<volatile intptr_t>::value;
  STATIC_ASSERT(ii_vintptrt);

  static const bool ii_cvintptrt = IsIntegral<const volatile intptr_t>::value;
  STATIC_ASSERT(ii_cvintptrt);
};
