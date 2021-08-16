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
#include "metaprogramming/isSigned.hpp"
#include "utilities/debug.hpp"

class IsSignedTest: AllStatic {
  template <typename SignedType, typename UnsignedType>
  class TestIntegers: AllStatic {
    static const bool _signed_type_is_signed = IsSigned<SignedType>::value;
    STATIC_ASSERT(_signed_type_is_signed);
    static const bool _unsigned_type_is_unsigned = !IsSigned<UnsignedType>::value;
    STATIC_ASSERT(_unsigned_type_is_unsigned);

    static const bool _cvsigned_type_is_signed = IsSigned<const volatile SignedType>::value;
    STATIC_ASSERT(_signed_type_is_signed);
    static const bool _cvunsigned_type_is_unsigned = !IsSigned<const volatile UnsignedType>::value;
    STATIC_ASSERT(_unsigned_type_is_unsigned);
  };

  const TestIntegers<int8_t,  uint8_t>  TestByte;
  const TestIntegers<int16_t, uint16_t> TestShort;
  const TestIntegers<int32_t, uint32_t> TestInt;
  const TestIntegers<int64_t, uint64_t> TestLong;
};
