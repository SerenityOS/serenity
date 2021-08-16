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

#ifndef SHARE_CI_CIINSTANCE_HPP
#define SHARE_CI_CIINSTANCE_HPP

#include "ci/ciObject.hpp"
#include "oops/instanceOop.hpp"
#include "oops/oop.hpp"

// ciInstance
//
// This class represents an instanceOop in the HotSpot virtual
// machine.  This is an oop which corresponds to a non-array
// instance of java.lang.Object.
class ciInstance : public ciObject {
  CI_PACKAGE_ACCESS
  friend class ciField;

protected:
  ciInstance(instanceHandle h_i) : ciObject(h_i) {
    assert(h_i()->is_instance_noinline(), "wrong type");
  }

  ciInstance(ciKlass* klass) : ciObject(klass) {}

  const char* type_string() { return "ciInstance"; }

  void print_impl(outputStream* st);

  ciConstant field_value_impl(BasicType field_btype, int offset);

public:
  // If this object is a java mirror, return the corresponding type.
  // Otherwise, return NULL.
  // (Remember that a java mirror is an instance of java.lang.Class.)
  ciType* java_mirror_type();

  // What kind of ciObject is this?
  bool is_instance()     { return true; }

  // Constant value of a field.
  ciConstant field_value(ciField* field);

  // Constant value of a field at the specified offset.
  ciConstant field_value_by_offset(int field_offset);

  ciKlass* java_lang_Class_klass();
};

#endif // SHARE_CI_CIINSTANCE_HPP
