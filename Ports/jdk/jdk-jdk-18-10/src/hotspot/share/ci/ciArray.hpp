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

#ifndef SHARE_CI_CIARRAY_HPP
#define SHARE_CI_CIARRAY_HPP

#include "ci/ciArrayKlass.hpp"
#include "ci/ciConstant.hpp"
#include "ci/ciObject.hpp"
#include "oops/arrayOop.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/typeArrayOop.hpp"

// ciArray
//
// This class represents an arrayOop in the HotSpot virtual
// machine.
class ciArray : public ciObject {
private:
  int _length;

protected:
  ciArray( objArrayHandle h_a) : ciObject(h_a), _length(h_a()->length()) {}
  ciArray(typeArrayHandle h_a) : ciObject(h_a), _length(h_a()->length()) {}

  arrayOop get_arrayOop() const { return (arrayOop)get_oop(); }

  const char* type_string() { return "ciArray"; }

  void print_impl(outputStream* st);

  ciConstant element_value_impl(BasicType elembt, arrayOop ary, int index);

public:
  int length() { return _length; }

  // Convenience routines.
  ciArrayKlass* array_type()         { return klass()->as_array_klass(); }
  ciType*       element_type()       { return array_type()->element_type(); }
  BasicType     element_basic_type() { return element_type()->basic_type(); }

  // Current value of an element.
  // Returns T_ILLEGAL if there is no element at the given index.
  ciConstant element_value(int index);

  // Current value of an element at the specified offset.
  // Returns T_ILLEGAL if there is no element at the given offset.
  ciConstant element_value_by_offset(intptr_t element_offset);

  // What kind of ciObject is this?
  bool is_array()        { return true; }
};

#endif // SHARE_CI_CIARRAY_HPP
