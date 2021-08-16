/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_STACKVALUECOLLECTION_HPP
#define SHARE_RUNTIME_STACKVALUECOLLECTION_HPP

#include "memory/allocation.hpp"
#include "runtime/stackValue.hpp"
#include "utilities/growableArray.hpp"

class StackValueCollection : public ResourceObj {
 private:
  GrowableArray<StackValue*>* _values;

 public:
  StackValueCollection()            { _values = new GrowableArray<StackValue*>(); }
  StackValueCollection(int length)  { _values = new GrowableArray<StackValue*>(length); }

  void add(StackValue *val) const   { _values->push(val); }
  int  size() const                 { return _values->length(); }
  bool is_empty() const             { return (size() == 0); }
  StackValue* at(int i) const       { return _values->at(i); }

  // Get typed locals/expressions
  jint  int_at(int slot) const;
  jlong long_at(int slot) const;
  Handle obj_at(int slot) const;
  jfloat  float_at(int slot) const;
  jdouble double_at(int slot) const;

  // Set typed locals/expressions
  void set_int_at(int slot, jint value);
  void set_long_at(int slot, jlong value);
  void set_obj_at(int slot, Handle value);
  void set_float_at(int slot, jfloat value);
  void set_double_at(int slot, jdouble value);

  void print();
};

#endif // SHARE_RUNTIME_STACKVALUECOLLECTION_HPP
