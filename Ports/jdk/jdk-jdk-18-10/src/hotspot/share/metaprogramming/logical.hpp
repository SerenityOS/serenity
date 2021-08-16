/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_METAPROGRAMMING_LOGICAL_HPP
#define SHARE_METAPROGRAMMING_LOGICAL_HPP

// Stand-ins for C++17 logical operations on types.

#include <type_traits>

// Stand-in for C++17 std::bool_constant<value>.
template<bool Value>
using BoolConstant = std::integral_constant<bool, Value>;

// Stand-in for C++17 std::conjunction<T...>
template<typename... T>
struct Conjunction : public std::true_type {};

template<typename T1>
struct Conjunction<T1> : public T1 {};

template<typename T1, typename... T>
struct Conjunction<T1, T...> :
  public std::conditional_t<bool(T1::value), Conjunction<T...>, T1>
{};

// Stand-in for C++17 std::disjunction<T...>.
template<typename... T>
struct Disjunction : public std::false_type {};

template<typename T1>
struct Disjunction<T1> : public T1 {};

template<typename T1, typename... T>
struct Disjunction<T1, T...> :
  public std::conditional_t<bool(T1::value), T1, Disjunction<T...>>
{};

// Stand-in for C++17 std::negation<T>.
template<typename T>
using Negation = BoolConstant<!bool(T::value)>;

#endif // SHARE_METAPROGRAMMING_LOGICAL_HPP
