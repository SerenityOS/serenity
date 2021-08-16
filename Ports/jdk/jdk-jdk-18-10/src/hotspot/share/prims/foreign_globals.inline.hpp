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
 */

#ifndef SHARE_PRIMS_FOREIGN_GLOBALS_INLINE_HPP
#define SHARE_PRIMS_FOREIGN_GLOBALS_INLINE_HPP

#include "prims/foreign_globals.hpp"

#include "oops/oopsHierarchy.hpp"
#include "oops/objArrayOop.hpp"

template<typename T>
static bool check_type(oop theOop) {
  static_assert(sizeof(T) == 0, "No check_type specialization found for this type");
  return false;
}
template<>
inline bool check_type<objArrayOop>(oop theOop) { return theOop->is_objArray(); }
template<>
inline bool check_type<typeArrayOop>(oop theOop) { return theOop->is_typeArray(); }

template<typename R>
R ForeignGlobals::cast(oop theOop) {
  assert(check_type<R>(theOop), "Invalid cast");
  return (R) theOop;
}

template<typename T, typename Func>
void ForeignGlobals::loadArray(objArrayOop jarray, int type_index, GrowableArray<T>& array, Func converter) const {
  objArrayOop subarray = cast<objArrayOop>(jarray->obj_at(type_index));
  int subarray_length = subarray->length();
  for (int i = 0; i < subarray_length; i++) {
    oop storage = subarray->obj_at(i);
    jint index = storage->int_field(VMS.index_offset);
    array.push(converter(index));
  }
}

#endif // SHARE_PRIMS_FOREIGN_GLOBALS_INLINE_HPP
