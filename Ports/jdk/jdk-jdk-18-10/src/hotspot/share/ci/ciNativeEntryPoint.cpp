/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciClassList.hpp"
#include "ci/ciNativeEntryPoint.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "ci/ciArray.hpp"
#include "classfile/javaClasses.hpp"
#include "oops/oop.inline.hpp"
#include "memory/allocation.hpp"

VMReg* getVMRegArray(ciArray* array) {
  assert(array->element_basic_type() == T_LONG, "Unexpected type");

  VMReg* out = NEW_ARENA_ARRAY(CURRENT_ENV->arena(), VMReg, array->length());

  for (int i = 0; i < array->length(); i++) {
    ciConstant con = array->element_value(i);
    VMReg reg = VMRegImpl::as_VMReg(con.as_long());
    out[i] = reg;
  }

  return out;
}

ciNativeEntryPoint::ciNativeEntryPoint(instanceHandle h_i) : ciInstance(h_i), _name(NULL) {
  // Copy name
  oop name_str = jdk_internal_invoke_NativeEntryPoint::name(get_oop());
  if (name_str != NULL) {
    char* temp_name = java_lang_String::as_quoted_ascii(name_str);
    size_t len = strlen(temp_name) + 1;
    char* name = (char*)CURRENT_ENV->arena()->Amalloc(len);
    strncpy(name, temp_name, len);
    _name = name;
  }

  _arg_moves = getVMRegArray(CURRENT_ENV->get_object(jdk_internal_invoke_NativeEntryPoint::argMoves(get_oop()))->as_array());
  _ret_moves = getVMRegArray(CURRENT_ENV->get_object(jdk_internal_invoke_NativeEntryPoint::returnMoves(get_oop()))->as_array());
}

jint ciNativeEntryPoint::shadow_space() const {
  VM_ENTRY_MARK;
  return jdk_internal_invoke_NativeEntryPoint::shadow_space(get_oop());
}

VMReg* ciNativeEntryPoint::argMoves() const {
  return _arg_moves;
}

VMReg* ciNativeEntryPoint::returnMoves() const {
  return _ret_moves;
}

jboolean ciNativeEntryPoint::need_transition() const {
  VM_ENTRY_MARK;
  return jdk_internal_invoke_NativeEntryPoint::need_transition(get_oop());
}

ciMethodType* ciNativeEntryPoint::method_type() const {
  VM_ENTRY_MARK;
  return CURRENT_ENV->get_object(jdk_internal_invoke_NativeEntryPoint::method_type(get_oop()))->as_method_type();
}

const char* ciNativeEntryPoint::name() {
  return _name;
}
