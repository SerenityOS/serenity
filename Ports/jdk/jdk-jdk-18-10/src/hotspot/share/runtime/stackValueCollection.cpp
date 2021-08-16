/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/stackValueCollection.hpp"

jint StackValueCollection::int_at(int slot) const {
  intptr_t val =  at(slot)->get_int();
  jint ival = *((jint*) (&val));
  return ival;
}

jlong StackValueCollection::long_at(int slot) const {
#ifdef _LP64
  return at(slot+1)->get_int();
#else
  union {
    jlong jl;
    jint  array[2];
  } value;
  // Interpreter stack is reversed in memory:
  // low memory location is in higher java local slot.
  value.array[0] = at(slot+1)->get_int();
  value.array[1] = at(slot  )->get_int();
  return value.jl;
#endif
}

Handle StackValueCollection::obj_at(int slot) const {
  return at(slot)->get_obj();
}

jfloat StackValueCollection::float_at(int slot) const {
  intptr_t res = at(slot)->get_int();
  return *((jfloat*) (&res));
}

jdouble StackValueCollection::double_at(int slot) const {
#ifdef _LP64
  intptr_t res = at(slot+1)->get_int();
  return *((jdouble*) (&res));
#else
  union {
    jdouble jd;
    jint    array[2];
  } value;
  // Interpreter stack is reversed in memory:
  // low memory location is in higher java local slot.
  value.array[0] = at(slot+1)->get_int();
  value.array[1] = at(slot  )->get_int();
  return value.jd;
#endif
}

void StackValueCollection::set_int_at(int slot, jint value) {
  intptr_t val;
  *((jint*) (&val)) = value;
  at(slot)->set_int(val);
}

void StackValueCollection::set_long_at(int slot, jlong value) {
#ifdef _LP64
  at(slot+1)->set_int(value);
#else
  union {
    jlong jl;
    jint  array[2];
  } x;
  // Interpreter stack is reversed in memory:
  // low memory location is in higher java local slot.
  x.jl = value;
  at(slot+1)->set_int(x.array[0]);
  at(slot+0)->set_int(x.array[1]);
#endif
}

void StackValueCollection::set_obj_at(int slot, Handle value) {
  at(slot)->set_obj(value);
}

void StackValueCollection::set_float_at(int slot, jfloat value) {
#ifdef _LP64
  union {
    intptr_t jd;
    jint    array[2];
  } val;
  // Interpreter stores 32 bit floats in first half of 64 bit word.
  val.array[0] = *(jint*)(&value);
  val.array[1] = 0;
  at(slot)->set_int(val.jd);
#else
  at(slot)->set_int(*(jint*)(&value));
#endif
}

void StackValueCollection::set_double_at(int slot, jdouble value) {
#ifdef _LP64
  at(slot+1)->set_int(*(intptr_t*)(&value));
#else
  union {
    jdouble jd;
    jint    array[2];
  } x;
  // Interpreter stack is reversed in memory:
  // low memory location is in higher java local slot.
  x.jd = value;
  at(slot+1)->set_int(x.array[0]);
  at(slot+0)->set_int(x.array[1]);
#endif
}

#ifndef PRODUCT
void StackValueCollection::print() {
  for(int index = 0; index < size(); index++) {
    tty->print("\t  %2d ", index);
    at(index)->print_on(tty);
    if( at(index  )->type() == T_INT &&
        index+1 < size() &&
        at(index+1)->type() == T_INT ) {
      tty->print("  " INT64_FORMAT " (long)", (int64_t)long_at(index));
      tty->cr();
      tty->print("\t     %.15e (double)", double_at(index));
      tty->print("  " PTR64_FORMAT " (longhex)", (int64_t)long_at(index));
    }
    tty->cr();
  }
}
#endif
