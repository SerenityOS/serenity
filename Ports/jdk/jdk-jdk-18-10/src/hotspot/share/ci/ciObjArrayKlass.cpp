/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciInstanceKlass.hpp"
#include "ci/ciObjArrayKlass.hpp"
#include "ci/ciSymbol.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "runtime/signature.hpp"

// ciObjArrayKlass
//
// This class represents a Klass* in the HotSpot virtual machine
// whose Klass part is an ObjArrayKlass.

// ------------------------------------------------------------------
// ciObjArrayKlass::ciObjArrayKlass
//
// Constructor for loaded object array klasses.
ciObjArrayKlass::ciObjArrayKlass(Klass* k) : ciArrayKlass(k) {
  assert(get_Klass()->is_objArray_klass(), "wrong type");
  Klass* element_Klass = get_ObjArrayKlass()->bottom_klass();
  _base_element_klass = CURRENT_ENV->get_klass(element_Klass);
  assert(_base_element_klass->is_instance_klass() ||
         _base_element_klass->is_type_array_klass(), "bad base klass");
  if (dimension() == 1) {
    _element_klass = _base_element_klass;
  } else {
    _element_klass = NULL;
  }
  if (!ciObjectFactory::is_initialized()) {
    assert(_element_klass->is_java_lang_Object(), "only arrays of object are shared");
  }
}

// ------------------------------------------------------------------
// ciObjArrayKlass::ciObjArrayKlass
//
// Constructor for unloaded object array klasses.
ciObjArrayKlass::ciObjArrayKlass(ciSymbol* array_name,
                                 ciKlass* base_element_klass,
                                 int dimension)
  : ciArrayKlass(array_name,
                 dimension, T_OBJECT) {
    _base_element_klass = base_element_klass;
    assert(_base_element_klass->is_instance_klass() ||
           _base_element_klass->is_type_array_klass(), "bad base klass");
    if (dimension == 1) {
      _element_klass = base_element_klass;
    } else {
      _element_klass = NULL;
    }
}

// ------------------------------------------------------------------
// ciObjArrayKlass::element_klass
//
// What is the one-level element type of this array?
ciKlass* ciObjArrayKlass::element_klass() {
  if (_element_klass == NULL) {
    assert(dimension() > 1, "_element_klass should not be NULL");
    // Produce the element klass.
    if (is_loaded()) {
      VM_ENTRY_MARK;
      Klass* element_Klass = get_ObjArrayKlass()->element_klass();
      _element_klass = CURRENT_THREAD_ENV->get_klass(element_Klass);
    } else {
      VM_ENTRY_MARK;
      // We are an unloaded array klass.  Attempt to fetch our
      // element klass by name.
      _element_klass = CURRENT_THREAD_ENV->get_klass_by_name_impl(
                          this,
                          constantPoolHandle(),
                          construct_array_name(base_element_klass()->name(),
                                               dimension() - 1),
                          false);
    }
  }
  return _element_klass;
}

// ------------------------------------------------------------------
// ciObjArrayKlass::construct_array_name
//
// Build an array name from an element name and a dimension.
ciSymbol* ciObjArrayKlass::construct_array_name(ciSymbol* element_name,
                                                int dimension) {
  EXCEPTION_CONTEXT;
  int element_len = element_name->utf8_length();
  int buflen = dimension + element_len + 3;  // '['+ + 'L'? + (element) + ';'? + '\0'
  char* name = CURRENT_THREAD_ENV->name_buffer(buflen);
  int pos = 0;
  for ( ; pos < dimension; pos++) {
    name[pos] = JVM_SIGNATURE_ARRAY;
  }
  Symbol* base_name_sym = element_name->get_symbol();

  if (Signature::is_array(base_name_sym) ||
      Signature::has_envelope(base_name_sym)) {
    strncpy(&name[pos], (char*)element_name->base(), element_len);
    name[pos + element_len] = '\0';
  } else {
    name[pos++] = JVM_SIGNATURE_CLASS;
    strncpy(&name[pos], (char*)element_name->base(), element_len);
    name[pos + element_len] = JVM_SIGNATURE_ENDCLASS;
    name[pos + element_len + 1] = '\0';
  }
  return ciSymbol::make(name);
}

// ------------------------------------------------------------------
// ciObjArrayKlass::make_impl
//
// Implementation of make.
ciObjArrayKlass* ciObjArrayKlass::make_impl(ciKlass* element_klass) {

  if (element_klass->is_loaded()) {
    EXCEPTION_CONTEXT;
    // The element klass is loaded
    Klass* array = element_klass->get_Klass()->array_klass(THREAD);
    if (HAS_PENDING_EXCEPTION) {
      CLEAR_PENDING_EXCEPTION;
      CURRENT_THREAD_ENV->record_out_of_memory_failure();
      return ciEnv::unloaded_ciobjarrayklass();
    }
    return CURRENT_THREAD_ENV->get_obj_array_klass(array);
  }

  // The array klass was unable to be made or the element klass was
  // not loaded.
  ciSymbol* array_name = construct_array_name(element_klass->name(), 1);
  if (array_name == ciEnv::unloaded_cisymbol()) {
    return ciEnv::unloaded_ciobjarrayklass();
  }
  return
    CURRENT_ENV->get_unloaded_klass(element_klass, array_name)
                        ->as_obj_array_klass();
}

// ------------------------------------------------------------------
// ciObjArrayKlass::make
//
// Make an array klass corresponding to the specified primitive type.
ciObjArrayKlass* ciObjArrayKlass::make(ciKlass* element_klass) {
  GUARDED_VM_ENTRY(return make_impl(element_klass);)
}

ciKlass* ciObjArrayKlass::exact_klass() {
  ciType* base = base_element_type();
  if (base->is_instance_klass()) {
    ciInstanceKlass* ik = base->as_instance_klass();
    if (ik->exact_klass() != NULL) {
      return this;
    }
  } else if (base->is_primitive_type()) {
    return this;
  }
  return NULL;
}
