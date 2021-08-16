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
#include "classfile/javaClasses.hpp"
#include "classfile/vmClasses.hpp"
#include "memory/universe.hpp"
#include "runtime/reflectionUtils.hpp"

KlassStream::KlassStream(InstanceKlass* klass, bool local_only,
                         bool classes_only, bool walk_defaults) {
  _klass = _base_klass = klass;
  _base_class_search_defaults = false;
  _defaults_checked = false;
  if (classes_only) {
    _interfaces = Universe::the_empty_instance_klass_array();
  } else {
    _interfaces = klass->transitive_interfaces();
  }
  _interface_index = _interfaces->length();
  _local_only = local_only;
  _classes_only = classes_only;
  _walk_defaults = walk_defaults;
}

bool KlassStream::eos() {
  if (index() >= 0) return false;
  if (_local_only) return true;
  if (!_klass->is_interface() && _klass->super() != NULL) {
    // go up superclass chain (not for interfaces)
    _klass = _klass->java_super();
  // Next for method walks, walk default methods
  } else if (_walk_defaults && (_defaults_checked == false)  && (_base_klass->default_methods() != NULL)) {
      _base_class_search_defaults = true;
      _klass = _base_klass;
      _defaults_checked = true;
  } else {
    // Next walk transitive interfaces
    if (_interface_index > 0) {
      _klass = _interfaces->at(--_interface_index);
    } else {
      return true;
    }
  }
  _index = length();
  next();
  return eos();
}


GrowableArray<FilteredField*> *FilteredFieldsMap::_filtered_fields =
  new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<FilteredField*>(3, mtServiceability);


void FilteredFieldsMap::initialize() {
  int offset = reflect_ConstantPool::oop_offset();
  _filtered_fields->append(new FilteredField(vmClasses::reflect_ConstantPool_klass(), offset));
  offset = reflect_UnsafeStaticFieldAccessorImpl::base_offset();
  _filtered_fields->append(new FilteredField(vmClasses::reflect_UnsafeStaticFieldAccessorImpl_klass(), offset));
}

int FilteredFieldStream::field_count() {
  int numflds = 0;
  for (;!eos(); next()) {
    numflds++;
  }
  return numflds;
}
