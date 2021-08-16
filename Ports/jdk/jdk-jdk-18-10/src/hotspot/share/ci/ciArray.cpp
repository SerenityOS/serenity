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

#include "precompiled.hpp"
#include "ci/ciArray.hpp"
#include "ci/ciArrayKlass.hpp"
#include "ci/ciConstant.hpp"
#include "ci/ciKlass.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "utilities/powerOfTwo.hpp"

// ciArray
//
// This class represents an arrayOop in the HotSpot virtual
// machine.
static BasicType fixup_element_type(BasicType bt) {
  if (is_reference_type(bt))  return T_OBJECT;
  if (bt == T_BOOLEAN)  return T_BYTE;
  return bt;
}

ciConstant ciArray::element_value_impl(BasicType elembt,
                                       arrayOop ary,
                                       int index) {
  if (ary == NULL)
    return ciConstant();
  assert(ary->is_array(), "");
  if (index < 0 || index >= ary->length())
    return ciConstant();
  ArrayKlass* ak = (ArrayKlass*) ary->klass();
  BasicType abt = ak->element_type();
  if (fixup_element_type(elembt) !=
      fixup_element_type(abt))
    return ciConstant();
  switch (elembt) {
  case T_ARRAY:
  case T_OBJECT:
    {
      assert(ary->is_objArray(), "");
      objArrayOop objary = (objArrayOop) ary;
      oop elem = objary->obj_at(index);
      ciEnv* env = CURRENT_ENV;
      ciObject* box = env->get_object(elem);
      return ciConstant(T_OBJECT, box);
    }
  default:
    break;
  }
  assert(ary->is_typeArray(), "");
  typeArrayOop tary = (typeArrayOop) ary;
  jint value = 0;
  switch (elembt) {
  case T_LONG:          return ciConstant(tary->long_at(index));
  case T_FLOAT:         return ciConstant(tary->float_at(index));
  case T_DOUBLE:        return ciConstant(tary->double_at(index));
  default:              return ciConstant();
  case T_BYTE:          value = tary->byte_at(index);           break;
  case T_BOOLEAN:       value = tary->byte_at(index) & 1;       break;
  case T_SHORT:         value = tary->short_at(index);          break;
  case T_CHAR:          value = tary->char_at(index);           break;
  case T_INT:           value = tary->int_at(index);            break;
  }
  return ciConstant(elembt, value);
}

// ------------------------------------------------------------------
// ciArray::element_value
//
// Current value of an element.
// Returns T_ILLEGAL if there is no element at the given index.
ciConstant ciArray::element_value(int index) {
  BasicType elembt = element_basic_type();
  GUARDED_VM_ENTRY(
    return element_value_impl(elembt, get_arrayOop(), index);
  )
}

// ------------------------------------------------------------------
// ciArray::element_value_by_offset
//
// Current value of an element at the specified offset.
// Returns T_ILLEGAL if there is no element at the given offset.
ciConstant ciArray::element_value_by_offset(intptr_t element_offset) {
  BasicType elembt = element_basic_type();
  intptr_t shift  = exact_log2(type2aelembytes(elembt));
  intptr_t header = arrayOopDesc::base_offset_in_bytes(elembt);
  intptr_t index = (element_offset - header) >> shift;
  intptr_t offset = header + ((intptr_t)index << shift);
  if (offset != element_offset || index != (jint)index || index < 0 || index >= length()) {
    return ciConstant();
  }
  return element_value((jint) index);
}

// ------------------------------------------------------------------
// ciArray::print_impl
//
// Implementation of the print method.
void ciArray::print_impl(outputStream* st) {
  st->print(" length=%d type=", length());
  klass()->print(st);
}
