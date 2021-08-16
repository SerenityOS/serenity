/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CITYPEARRAY_HPP
#define SHARE_CI_CITYPEARRAY_HPP

#include "ci/ciArray.hpp"
#include "ci/ciClassList.hpp"
#include "oops/typeArrayOop.hpp"

// ciTypeArray
//
// This class represents a typeArrayOop in the HotSpot virtual
// machine.
class ciTypeArray : public ciArray {
  CI_PACKAGE_ACCESS

protected:
  ciTypeArray(typeArrayHandle h_t) : ciArray(h_t) {}

  typeArrayOop get_typeArrayOop() {
    return (typeArrayOop)get_oop();
  }

  const char* type_string() { return "ciTypeArray"; }

public:
  // What kind of ciObject is this?
  bool is_type_array() { return true; }

  // Return character at index. This is only useful if the
  // compiler has already proved that the contents of the
  // array will never change.
  jchar char_at(int index);

  // Return byte at index.
  jbyte byte_at(int index);

};

#endif // SHARE_CI_CITYPEARRAY_HPP
