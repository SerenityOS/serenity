/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciTypeArrayKlass.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "memory/universe.hpp"

// ciTypeArrayKlass
//
// This class represents a Klass* in the HotSpot virtual machine
// whose Klass part in a TypeArrayKlass.

// ------------------------------------------------------------------
// ciTypeArrayKlass::ciTypeArrayKlass
ciTypeArrayKlass::ciTypeArrayKlass(Klass* k) : ciArrayKlass(k) {
  assert(get_Klass()->is_typeArray_klass(), "wrong type");
  assert(element_type() == get_TypeArrayKlass()->element_type(), "");
}

// ------------------------------------------------------------------
// ciTypeArrayKlass::make_impl
//
// Implementation of make.
ciTypeArrayKlass* ciTypeArrayKlass::make_impl(BasicType t) {
  Klass* k = Universe::typeArrayKlassObj(t);
  return CURRENT_ENV->get_type_array_klass(k);
}

// ------------------------------------------------------------------
// ciTypeArrayKlass::make
//
// Make an array klass corresponding to the specified primitive type.
ciTypeArrayKlass* ciTypeArrayKlass::make(BasicType t) {
  GUARDED_VM_ENTRY(return make_impl(t);)
}
