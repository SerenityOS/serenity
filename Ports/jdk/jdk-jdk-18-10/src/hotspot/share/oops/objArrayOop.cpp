/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "oops/access.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"

oop objArrayOopDesc::atomic_compare_exchange_oop(int index, oop exchange_value,
                                                 oop compare_value) {
  ptrdiff_t offs;
  if (UseCompressedOops) {
    offs = objArrayOopDesc::obj_at_offset<narrowOop>(index);
  } else {
    offs = objArrayOopDesc::obj_at_offset<oop>(index);
  }
  return HeapAccess<IS_ARRAY>::oop_atomic_cmpxchg_at(as_oop(), offs, compare_value, exchange_value);
}

Klass* objArrayOopDesc::element_klass() {
  return ObjArrayKlass::cast(klass())->element_klass();
}
