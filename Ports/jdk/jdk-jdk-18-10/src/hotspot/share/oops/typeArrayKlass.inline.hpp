/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_TYPEARRAYKLASS_INLINE_HPP
#define SHARE_OOPS_TYPEARRAYKLASS_INLINE_HPP

#include "oops/typeArrayKlass.hpp"

#include "oops/arrayKlass.hpp"
#include "oops/klass.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayOop.hpp"

class OopIterateClosure;

inline void TypeArrayKlass::oop_oop_iterate_impl(oop obj, OopIterateClosure* closure) {
  assert(obj->is_typeArray(),"must be a type array");
  // Performance tweak: We skip processing the klass pointer since all
  // TypeArrayKlasses are guaranteed processed via the null class loader.
}

template <typename T, typename OopClosureType>
void TypeArrayKlass::oop_oop_iterate(oop obj, OopClosureType* closure) {
  oop_oop_iterate_impl(obj, closure);
}

template <typename T, typename OopClosureType>
void TypeArrayKlass::oop_oop_iterate_bounded(oop obj, OopClosureType* closure, MemRegion mr) {
  oop_oop_iterate_impl(obj, closure);
}

template <typename T, typename OopClosureType>
void TypeArrayKlass::oop_oop_iterate_reverse(oop obj, OopClosureType* closure) {
  oop_oop_iterate_impl(obj, closure);
}

#endif // SHARE_OOPS_TYPEARRAYKLASS_INLINE_HPP
