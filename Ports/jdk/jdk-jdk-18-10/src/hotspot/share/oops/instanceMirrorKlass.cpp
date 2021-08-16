/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.inline.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "memory/iterator.inline.hpp"
#include "memory/oopFactory.hpp"
#include "memory/universe.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/instanceMirrorKlass.hpp"
#include "oops/instanceOop.hpp"
#include "oops/oop.inline.hpp"
#include "oops/symbol.hpp"
#include "runtime/handles.inline.hpp"
#include "utilities/macros.hpp"

int InstanceMirrorKlass::_offset_of_static_fields = 0;

int InstanceMirrorKlass::instance_size(Klass* k) {
  if (k != NULL && k->is_instance_klass()) {
    return align_object_size(size_helper() + InstanceKlass::cast(k)->static_field_size());
  }
  return size_helper();
}

instanceOop InstanceMirrorKlass::allocate_instance(Klass* k, TRAPS) {
  // Query before forming handle.
  int size = instance_size(k);
  assert(size > 0, "total object size must be positive: %d", size);

  // Since mirrors can be variable sized because of the static fields, store
  // the size in the mirror itself.
  return (instanceOop)Universe::heap()->class_allocate(this, size, THREAD);
}

int InstanceMirrorKlass::oop_size(oop obj) const {
  return java_lang_Class::oop_size(obj);
}

int InstanceMirrorKlass::compute_static_oop_field_count(oop obj) {
  Klass* k = java_lang_Class::as_Klass(obj);
  if (k != NULL && k->is_instance_klass()) {
    return InstanceKlass::cast(k)->static_oop_field_count();
  }
  return 0;
}

#if INCLUDE_CDS
void InstanceMirrorKlass::serialize_offsets(SerializeClosure* f) {
  f->do_u4((u4*)&_offset_of_static_fields);
}
#endif
