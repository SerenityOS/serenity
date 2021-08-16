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
#include "metaprogramming/primitiveConversions.hpp"
#include "unittest.hpp"
#include "utilities/debug.hpp"

struct PrimitiveConversionsTestSupport: AllStatic {

  template<size_t byte_size> struct SignedTypeOfSize;
  template<size_t byte_size> struct UnsignedTypeOfSize;

  template<typename T> struct Signed;
  template<typename T> struct Unsigned;
};

#define DEFINE_CANONICAL_SIGNED_TYPE(T)                                 \
  template<>                                                            \
  struct PrimitiveConversionsTestSupport::SignedTypeOfSize<sizeof(T)>   \
    : public AllStatic                                                  \
  {                                                                     \
    typedef T type;                                                     \
  };

#define DEFINE_CANONICAL_UNSIGNED_TYPE(T)                               \
  template<>                                                            \
  struct PrimitiveConversionsTestSupport::UnsignedTypeOfSize<sizeof(T)> \
    : public AllStatic                                                  \
  {                                                                     \
    typedef T type;                                                     \
  };

#define DEFINE_INTEGER_TYPES_OF_SIZE(NBITS)            \
  DEFINE_CANONICAL_SIGNED_TYPE(int ## NBITS ## _t)     \
  DEFINE_CANONICAL_UNSIGNED_TYPE(uint ## NBITS ## _t)

DEFINE_INTEGER_TYPES_OF_SIZE(8)
DEFINE_INTEGER_TYPES_OF_SIZE(16)
DEFINE_INTEGER_TYPES_OF_SIZE(32)
DEFINE_INTEGER_TYPES_OF_SIZE(64)

#undef DEFINE_INTEGER_TYPES_OF_SIZE
#undef DEFINE_CANONICAL_SIGNED_TYPE
#undef DEFINE_CANONICAL_UNSIGNED_TYPE

template<typename T>
struct PrimitiveConversionsTestSupport::Signed
  : public SignedTypeOfSize<sizeof(T)>
{};

template<typename T>
struct PrimitiveConversionsTestSupport::Unsigned
  : public UnsignedTypeOfSize<sizeof(T)>
{};

TEST(PrimitiveConversionsTest, round_trip_int) {
  int  sfive = 5;
  int  mfive = -5;
  uint ufive = 5u;

  typedef PrimitiveConversionsTestSupport::Signed<int>::type SI;
  typedef PrimitiveConversionsTestSupport::Unsigned<int>::type UI;

  EXPECT_EQ(sfive, PrimitiveConversions::cast<int>(PrimitiveConversions::cast<SI>(sfive)));
  EXPECT_EQ(sfive, PrimitiveConversions::cast<int>(PrimitiveConversions::cast<UI>(sfive)));

  EXPECT_EQ(mfive, PrimitiveConversions::cast<int>(PrimitiveConversions::cast<SI>(mfive)));
  EXPECT_EQ(mfive, PrimitiveConversions::cast<int>(PrimitiveConversions::cast<UI>(mfive)));

  EXPECT_EQ(ufive, PrimitiveConversions::cast<uint>(PrimitiveConversions::cast<SI>(ufive)));
  EXPECT_EQ(ufive, PrimitiveConversions::cast<uint>(PrimitiveConversions::cast<UI>(ufive)));
}

TEST(PrimitiveConversionsTest, round_trip_int_constexpr) {
  constexpr int  sfive = 5;
  constexpr int  mfive = -5;
  constexpr uint ufive = 5u;

  typedef PrimitiveConversionsTestSupport::Signed<int>::type SI;
  typedef PrimitiveConversionsTestSupport::Unsigned<int>::type UI;

  {
    constexpr SI i = PrimitiveConversions::cast<SI>(sfive);
    constexpr int r = PrimitiveConversions::cast<int>(i);
    EXPECT_EQ(sfive, r);
  }

  {
    constexpr UI i = PrimitiveConversions::cast<UI>(sfive);
    constexpr int r = PrimitiveConversions::cast<int>(i);
    EXPECT_EQ(sfive, r);
  }

  {
    constexpr SI i = PrimitiveConversions::cast<SI>(mfive);
    constexpr int r = PrimitiveConversions::cast<int>(i);
    EXPECT_EQ(mfive, r);
  }

  {
    constexpr UI i = PrimitiveConversions::cast<UI>(mfive);
    constexpr int r = PrimitiveConversions::cast<int>(i);
    EXPECT_EQ(mfive, r);
  }

  {
    constexpr SI i = PrimitiveConversions::cast<SI>(ufive);
    constexpr uint r = PrimitiveConversions::cast<uint>(i);
    EXPECT_EQ(ufive, r);
  }

  {
    constexpr UI i = PrimitiveConversions::cast<UI>(ufive);
    constexpr uint r = PrimitiveConversions::cast<uint>(i);
    EXPECT_EQ(ufive, r);
  }
}

TEST(PrimitiveConversionsTest, round_trip_float) {
  float  ffive = 5.0f;
  double dfive = 5.0;

  typedef PrimitiveConversionsTestSupport::Signed<float>::type SF;
  typedef PrimitiveConversionsTestSupport::Unsigned<float>::type UF;

  typedef PrimitiveConversionsTestSupport::Signed<double>::type SD;
  typedef PrimitiveConversionsTestSupport::Unsigned<double>::type UD;

  EXPECT_EQ(ffive, PrimitiveConversions::cast<float>(PrimitiveConversions::cast<SF>(ffive)));
  EXPECT_EQ(ffive, PrimitiveConversions::cast<float>(PrimitiveConversions::cast<UF>(ffive)));

  EXPECT_EQ(dfive, PrimitiveConversions::cast<double>(PrimitiveConversions::cast<SD>(dfive)));
  EXPECT_EQ(dfive, PrimitiveConversions::cast<double>(PrimitiveConversions::cast<UD>(dfive)));
}

TEST(PrimitiveConversionsTest, round_trip_ptr) {
  int five = 5;
  int* pfive = &five;
  const int* cpfive = &five;

  typedef PrimitiveConversionsTestSupport::Signed<int*>::type SIP;
  typedef PrimitiveConversionsTestSupport::Unsigned<int*>::type UIP;

  EXPECT_EQ(pfive, PrimitiveConversions::cast<int*>(PrimitiveConversions::cast<SIP>(pfive)));
  EXPECT_EQ(pfive, PrimitiveConversions::cast<int*>(PrimitiveConversions::cast<UIP>(pfive)));

  EXPECT_EQ(cpfive, PrimitiveConversions::cast<const int*>(PrimitiveConversions::cast<SIP>(cpfive)));
  EXPECT_EQ(cpfive, PrimitiveConversions::cast<const int*>(PrimitiveConversions::cast<UIP>(cpfive)));
}
