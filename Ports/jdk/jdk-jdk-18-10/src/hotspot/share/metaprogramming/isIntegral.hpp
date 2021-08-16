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


#ifndef SHARE_METAPROGRAMMING_ISINTEGRAL_HPP
#define SHARE_METAPROGRAMMING_ISINTEGRAL_HPP

#include "metaprogramming/integralConstant.hpp"
#include "metaprogramming/isSigned.hpp"
#include "metaprogramming/removeCV.hpp"
#include <limits>

// This metafunction returns true iff the type T (irrespective of CV qualifiers)
// is an integral type. Note that this is false for enums.

template<typename T>
struct IsIntegral
  : public IntegralConstant<bool, std::numeric_limits<typename RemoveCV<T>::type>::is_integer>
{};

// This metafunction returns true iff the type T (irrespective of CV qualifiers)
// is a signed integral type. Note that this is false for enums.

template<typename T>
struct IsSignedIntegral
  : public IntegralConstant<bool, IsIntegral<T>::value && IsSigned<T>::value>
{};

// This metafunction returns true iff the type T (irrespective of CV qualifiers)
// is an unsigned integral type. Note that this is false for enums.

template<typename T>
struct IsUnsignedIntegral
  : public IntegralConstant<bool, IsIntegral<T>::value && !IsSigned<T>::value>
{};

#endif // SHARE_METAPROGRAMMING_ISINTEGRAL_HPP
