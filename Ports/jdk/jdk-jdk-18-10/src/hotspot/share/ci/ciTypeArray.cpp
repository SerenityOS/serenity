/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciTypeArray.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"

// ciTypeArray
//
// This class represents an typeArrayOop in the HotSpot virtual
// machine.


// ------------------------------------------------------------------
// ciTypeArray::char_at
//
// Implementation of the char_at method.
jchar ciTypeArray::char_at(int index) {
  VM_ENTRY_MARK;
  assert(index >= 0 && index < length(), "out of range");
  jchar c = get_typeArrayOop()->char_at(index);
#ifdef ASSERT
  jchar d = element_value(index).as_char();
  assert(c == d, "");
#endif //ASSERT
  return c;
}

// ------------------------------------------------------------------
// ciTypeArray::byte_at
//
// Implementation of the byte_at method.
jbyte ciTypeArray::byte_at(int index) {
  VM_ENTRY_MARK;
  assert(index >= 0 && index < length(), "out of range");
  return get_typeArrayOop()->byte_at(index);
}
