/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_METAPROGRAMMING_INTEGRALCONSTANT_HPP
#define SHARE_METAPROGRAMMING_INTEGRALCONSTANT_HPP


// An Integral Constant is a class providing a compile-time value of an
// integral type.  An Integral Constant is also a nullary metafunction,
// returning itself.  An integral constant object is implicitly
// convertible to the associated value.
//
// A type n is a model of Integral Constant if it meets the following
// requirements:
//
// n::ValueType                : The integral type of n::value
// n::value                    : An integral constant expression
// n::type                     : IsSame<n::type, n>::value is true
// n::value_type const c = n() : c == n::value

// A model of the Integer Constant concept.
// T is an integral type, and is the value_type.
// v is an integral constant, and is the value.
template<typename T, T v>
struct IntegralConstant {
  typedef T value_type;
  static const value_type value = v;
  typedef IntegralConstant<T, v> type;
  operator value_type() { return value; }
};

// A bool valued IntegralConstant whose value is true.
typedef IntegralConstant<bool, true> TrueType;

// A bool valued IntegralConstant whose value is false.
typedef IntegralConstant<bool, false> FalseType;

#endif // SHARE_METAPROGRAMMING_INTEGRALCONSTANT_HPP
