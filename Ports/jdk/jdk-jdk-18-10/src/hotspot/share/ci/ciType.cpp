/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciEnv.hpp"
#include "ci/ciType.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/oop.inline.hpp"

ciType* ciType::_basic_types[T_CONFLICT+1];

// ciType
//
// This class represents a Java reference or primitive type.

// ------------------------------------------------------------------
// ciType::ciType
//
ciType::ciType(BasicType basic_type) : ciMetadata() {
  assert(basic_type >= T_BOOLEAN && basic_type <= T_CONFLICT, "range check");
  _basic_type = basic_type;
}

ciType::ciType(Klass* k) : ciMetadata(k) {
  _basic_type = k->is_array_klass() ? T_ARRAY : T_OBJECT;
}


// ------------------------------------------------------------------
// ciType::is_subtype_of
//
bool ciType::is_subtype_of(ciType* type) {
  if (this == type)  return true;
  if (is_klass() && type->is_klass())
    return this->as_klass()->is_subtype_of(type->as_klass());
  return false;
}

// ------------------------------------------------------------------
// ciType::name
//
// Return the name of this type
const char* ciType::name() {
  if (is_primitive_type()) {
    return type2name(basic_type());
  } else {
    assert(is_klass(), "must be");
    return as_klass()->name()->as_utf8();
  }
}

// ------------------------------------------------------------------
// ciType::print_impl
//
// Implementation of the print method.
void ciType::print_impl(outputStream* st) {
  st->print(" type=");
  print_name_on(st);
}

// ------------------------------------------------------------------
// ciType::print_name
//
// Print the name of this type
void ciType::print_name_on(outputStream* st) {
  ResourceMark rm;
  st->print("%s", name());
}



// ------------------------------------------------------------------
// ciType::java_mirror
//
ciInstance* ciType::java_mirror() {
  VM_ENTRY_MARK;
  return CURRENT_THREAD_ENV->get_instance(Universe::java_mirror(basic_type()));
}

// ------------------------------------------------------------------
// ciType::make
//
// Produce the ciType for a given primitive BasicType.
// As a bonus, produce the right reference type for T_OBJECT.
// Does not work on T_ARRAY.
ciType* ciType::make(BasicType t) {
  // short, etc.
  // Note: Bare T_ADDRESS means a raw pointer type, not a return_address.
  assert((uint)t < T_CONFLICT+1, "range check");
  if (t == T_OBJECT)  return ciEnv::_Object_klass;  // java/lang/Object
  assert(_basic_types[t] != NULL, "domain check");
  return _basic_types[t];
}

// ciReturnAddress
//
// This class represents the type of a specific return address in the
// bytecodes.

// ------------------------------------------------------------------
// ciReturnAddress::ciReturnAddress
//
ciReturnAddress::ciReturnAddress(int bci) : ciType(T_ADDRESS) {
  assert(0 <= bci, "bci cannot be negative");
  _bci = bci;
}

// ------------------------------------------------------------------
// ciReturnAddress::print_impl
//
// Implementation of the print method.
void ciReturnAddress::print_impl(outputStream* st) {
  st->print(" bci=%d", _bci);
}

// ------------------------------------------------------------------
// ciReturnAddress::make
ciReturnAddress* ciReturnAddress::make(int bci) {
  GUARDED_VM_ENTRY(return CURRENT_ENV->get_return_address(bci);)
}
