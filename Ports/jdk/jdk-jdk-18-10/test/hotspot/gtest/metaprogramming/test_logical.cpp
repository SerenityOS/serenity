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

#include "precompiled.hpp"
#include "metaprogramming/logical.hpp"
#include <type_traits>

class TestBoolConstant {
  static_assert(BoolConstant<true>::value, "true");
  static_assert(!BoolConstant<false>::value, "false");
};

class TestConjunction {
  class A : public std::true_type {};
  class B : public std::true_type {};
  class C : public std::false_type {};
  class D : public std::false_type {};

  static_assert(Conjunction<>::value, "nullary value");

  static_assert(Conjunction<A>::value, "true value");
  static_assert(std::is_base_of<A, Conjunction<A>>::value, "true type");

  static_assert(!Conjunction<C>::value, "false value");
  static_assert(std::is_base_of<C, Conjunction<C>>::value, "false type");

  static_assert(Conjunction<A, B>::value, "true/true value");
  static_assert(std::is_base_of<B, Conjunction<A, B>>::value, "true/true type");

  static_assert(!Conjunction<A, C>::value, "true/false value");
  static_assert(std::is_base_of<C, Conjunction<A, C>>::value, "true/false type");

  static_assert(!Conjunction<C, A>::value, "false/true value");
  static_assert(std::is_base_of<C, Conjunction<C, A>>::value, "false/true type");

  static_assert(!Conjunction<C, D>::value, "false/false value");
  static_assert(std::is_base_of<C, Conjunction<C, D>>::value, "false/false type");
};

class TestDisjunction {
  class A : public std::true_type {};
  class B : public std::true_type {};
  class C : public std::false_type {};
  class D : public std::false_type {};

  static_assert(!Disjunction<>::value, "nullary value");

  static_assert(Disjunction<A>::value, "true value");
  static_assert(std::is_base_of<A, Disjunction<A>>::value, "true type");

  static_assert(!Disjunction<C>::value, "false value");
  static_assert(std::is_base_of<C, Disjunction<C>>::value, "false type");

  static_assert(Disjunction<A, B>::value, "true/true value");
  static_assert(std::is_base_of<A, Disjunction<A, B>>::value, "true/true type");

  static_assert(Disjunction<A, C>::value, "true/false value");
  static_assert(std::is_base_of<A, Disjunction<A, C>>::value, "true/false type");

  static_assert(Disjunction<C, A>::value, "false/true value");
  static_assert(std::is_base_of<A, Disjunction<C, A>>::value, "false/true type");

  static_assert(!Disjunction<C, D>::value, "false/false value");
  static_assert(std::is_base_of<D, Disjunction<C, D>>::value, "false/false type");
};

class TestNegation {
  static_assert(Negation<std::false_type>::value, "false -> true");
  static_assert(!Negation<std::true_type>::value, "true -> false");
};
