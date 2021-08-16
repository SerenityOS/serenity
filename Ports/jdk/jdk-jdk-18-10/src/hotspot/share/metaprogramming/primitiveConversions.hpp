/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_METAPROGRAMMING_PRIMITIVECONVERSIONS_HPP
#define SHARE_METAPROGRAMMING_PRIMITIVECONVERSIONS_HPP

#include "memory/allStatic.hpp"
#include "metaprogramming/enableIf.hpp"
#include "utilities/globalDefinitions.hpp"
#include <type_traits>

class PrimitiveConversions : public AllStatic {

  // True if types are the same size and either is integral.
  template<typename To, typename From>
  static constexpr bool check_cast() {
    return (sizeof(To) == sizeof(From)) &&
           (std::is_integral<To>::value || std::is_integral<From>::value);
  }

public:
  // template<typename To, typename From> To cast(From x)
  //
  // Return a value of type To with the same value representation as x.
  //
  // To and From must be of the same size.
  //
  // At least one of To or From must be an integral type.  The other must
  // be an integral, enum, floating point, or pointer type.

  // integer -> integer
  // Use static_cast for conversion.  See C++14 4.7 Integral
  // conversions. If To is signed and From unsigned, the result is
  // implementation-defined.  All supported platforms provide two's
  // complement behavior, and that behavior is required by C++20.
  // Using an lvalue to reference cast (see C++03 3.10/15) involves a
  // reinterpret_cast, which prevents constexpr support.
  template<typename To, typename From,
           ENABLE_IF(sizeof(To) == sizeof(From)),
           ENABLE_IF(std::is_integral<To>::value),
           ENABLE_IF(std::is_integral<From>::value)>
  static constexpr To cast(From x) {
    return static_cast<To>(x);
  }

  // integer -> enum, enum -> integer
  // Use the enum's underlying type for integer -> integer cast.
  template<typename To, typename From,
           ENABLE_IF(check_cast<To, From>()),
           ENABLE_IF(std::is_enum<To>::value)>
  static constexpr To cast(From x) {
    return static_cast<To>(cast<std::underlying_type_t<To>>(x));
  }

  template<typename To, typename From,
           ENABLE_IF(check_cast<To, From>()),
           ENABLE_IF(std::is_enum<From>::value)>
  static constexpr To cast(From x) {
    return cast<To>(static_cast<std::underlying_type_t<From>>(x));
  }

  // integer -> pointer, pointer -> integer
  // Use reinterpret_cast, so no constexpr support.
  template<typename To, typename From,
           ENABLE_IF(check_cast<To, From>()),
           ENABLE_IF(std::is_pointer<To>::value || std::is_pointer<From>::value)>
  static To cast(From x) {
    return reinterpret_cast<To>(x);
  }

  // integer -> floating point, floating point -> integer
  // Use the union trick.  The union trick is technically UB, but is
  // widely and well supported, producing good code.  In some cases,
  // such as gcc, that support is explicitly documented.  Using memcpy
  // is the correct method, but some compilers produce wretched code
  // for that method, even at maximal optimization levels.  Neither
  // the union trick nor memcpy provides constexpr support.
  template<typename To, typename From,
           ENABLE_IF(check_cast<To, From>()),
           ENABLE_IF(std::is_floating_point<To>::value ||
                     std::is_floating_point<From>::value)>
  static To cast(From x) {
    union { From from; To to; } converter = { x };
    return converter.to;
  }

  // Support thin wrappers over primitive types.
  // If derived from std::true_type, provides representational conversion
  // from T to some other type.  When true, must provide
  // - Value: typedef for T.
  // - Decayed: typedef for decayed type.
  // - static Decayed decay(T x): return value of type Decayed with
  //   the same value representation as x.
  // - static T recover(Decayed x): return a value of type T with the
  //   same value representation as x.
  template<typename T> struct Translate : public std::false_type {};
};

// jfloat and jdouble translation to integral types

template<>
struct PrimitiveConversions::Translate<jdouble> : public std::true_type {
  typedef double Value;
  typedef int64_t Decayed;

  static Decayed decay(Value x) { return PrimitiveConversions::cast<Decayed>(x); }
  static Value recover(Decayed x) { return PrimitiveConversions::cast<Value>(x); }
};

template<>
struct PrimitiveConversions::Translate<jfloat> : public std::true_type {
  typedef float Value;
  typedef int32_t Decayed;

  static Decayed decay(Value x) { return PrimitiveConversions::cast<Decayed>(x); }
  static Value recover(Decayed x) { return PrimitiveConversions::cast<Value>(x); }
};

#endif // SHARE_METAPROGRAMMING_PRIMITIVECONVERSIONS_HPP
