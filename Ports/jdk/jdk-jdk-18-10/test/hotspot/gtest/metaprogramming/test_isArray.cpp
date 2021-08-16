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
#include "metaprogramming/isArray.hpp"
#include "utilities/debug.hpp"

class IsArrayTest: AllStatic {
  class A: AllStatic {};

  static const bool ia_A = IsArray<A>::value;
  STATIC_ASSERT(!ia_A);

  static const bool ia_Aptr = IsArray<A*>::value;
  STATIC_ASSERT(!ia_Aptr);

  static const bool ia_Aarr = IsArray<A[]>::value;
  STATIC_ASSERT(ia_Aarr);

  static const bool ia_Aarr10 = IsArray<A[10]>::value;
  STATIC_ASSERT(ia_Aarr10);

  static const bool ia_Aptrarr10 = IsArray<A*[10]>::value;
  STATIC_ASSERT(ia_Aptrarr10);

  static const bool ia_Aarr10arr10 = IsArray<A[10][10]>::value;
  STATIC_ASSERT(ia_Aarr10arr10);

  static const bool ia_cAarr = IsArray<const A[]>::value;
  STATIC_ASSERT(ia_cAarr);

  static const bool ia_vAarr = IsArray<volatile A[]>::value;
  STATIC_ASSERT(ia_vAarr);

  static const bool ia_cAarr10 = IsArray<const A[10]>::value;
  STATIC_ASSERT(ia_cAarr10);

  static const bool ia_vAarr10 = IsArray<volatile A[10]>::value;
  STATIC_ASSERT(ia_vAarr10);

  static const bool ia_voidptr = IsArray<void*>::value;
  STATIC_ASSERT(!ia_voidptr);

  static const bool ia_intptrt = IsArray<intptr_t>::value;
  STATIC_ASSERT(!ia_intptrt);

  static const bool ia_char = IsArray<char>::value;
  STATIC_ASSERT(!ia_char);
};
