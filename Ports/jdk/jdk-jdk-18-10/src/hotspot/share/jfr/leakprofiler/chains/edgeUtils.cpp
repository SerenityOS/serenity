/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/leakprofiler/chains/edge.hpp"
#include "jfr/leakprofiler/chains/edgeStore.hpp"
#include "jfr/leakprofiler/chains/edgeUtils.hpp"
#include "jfr/leakprofiler/utilities/unifiedOopRef.inline.hpp"
#include "oops/fieldStreams.inline.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/instanceMirrorKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/handles.inline.hpp"

bool EdgeUtils::is_leak_edge(const Edge& edge) {
  return (const Edge*)edge.pointee()->mark().to_pointer() == &edge;
}

static bool is_static_field(const oop ref_owner, const InstanceKlass* ik, int offset) {
  assert(ref_owner != NULL, "invariant");
  assert(ik != NULL, "invariant");
  assert(ref_owner->klass() == ik, "invariant");
  return ik->is_mirror_instance_klass() && offset >= InstanceMirrorKlass::cast(ik)->offset_of_static_fields();
}

static int field_offset(const Edge& edge, const oop ref_owner) {
  assert(ref_owner != NULL, "invariant");
  assert(!ref_owner->is_array(), "invariant");
  assert(ref_owner->is_instance(), "invariant");
  UnifiedOopRef reference = edge.reference();
  assert(!reference.is_null(), "invariant");
  const int offset = (int)(reference.addr<uintptr_t>() - cast_from_oop<uintptr_t>(ref_owner));
  assert(offset < ref_owner->size() * HeapWordSize, "invariant");
  return offset;
}

const Symbol* EdgeUtils::field_name(const Edge& edge, jshort* modifiers) {
  assert(!edge.is_root(), "invariant");
  assert(!EdgeUtils::is_array_element(edge), "invariant");
  assert(modifiers != NULL, "invariant");
  const oop ref_owner = edge.reference_owner();
  assert(ref_owner != NULL, "invariant");
  assert(ref_owner->klass()->is_instance_klass(), "invariant");
  const InstanceKlass* ik = InstanceKlass::cast(ref_owner->klass());
  const int offset = field_offset(edge, ref_owner);
  if (is_static_field(ref_owner, ik, offset)) {
    assert(ik->is_mirror_instance_klass(), "invariant");
    assert(java_lang_Class::as_Klass(ref_owner)->is_instance_klass(), "invariant");
    ik = InstanceKlass::cast(java_lang_Class::as_Klass(ref_owner));
  }
  while (ik != NULL) {
    JavaFieldStream jfs(ik);
    while (!jfs.done()) {
      if (offset == jfs.offset()) {
        *modifiers = jfs.access_flags().as_short();
        return jfs.name();
      }
      jfs.next();
    }
    ik = (const InstanceKlass*)ik->super();
  }
  *modifiers = 0;
  return NULL;
}

bool EdgeUtils::is_array_element(const Edge& edge) {
  assert(!edge.is_root(), "invariant");
  const oop ref_owner = edge.reference_owner();
  assert(ref_owner != NULL, "invariant");
  return ref_owner->is_objArray();
}

static int array_offset(const Edge& edge) {
  assert(EdgeUtils::is_array_element(edge), "invariant");
  const oop ref_owner = edge.reference_owner();
  assert(ref_owner != NULL, "invariant");
  UnifiedOopRef reference = edge.reference();
  assert(!reference.is_null(), "invariant");
  assert(ref_owner->is_array(), "invariant");
  const objArrayOop ref_owner_array = static_cast<const objArrayOop>(ref_owner);
  const int offset = (int)pointer_delta(reference.addr<HeapWord*>(), ref_owner_array->base(), heapOopSize);
  assert(offset >= 0 && offset < ref_owner_array->length(), "invariant");
  return offset;
}

int EdgeUtils::array_index(const Edge& edge) {
  return array_offset(edge);
}

int EdgeUtils::array_size(const Edge& edge) {
  assert(is_array_element(edge), "invariant");
  const oop ref_owner = edge.reference_owner();
  assert(ref_owner != NULL, "invariant");
  assert(ref_owner->is_objArray(), "invariant");
  return ((objArrayOop)ref_owner)->length();
}

const Edge* EdgeUtils::root(const Edge& edge) {
  const Edge* current = &edge;
  const Edge* parent = current->parent();
  while (parent != NULL) {
    current = parent;
    parent = current->parent();
  }
  assert(current != NULL, "invariant");
  return current;
}

const Edge* EdgeUtils::ancestor(const Edge& edge, size_t distance) {
  const Edge* current = &edge;
  const Edge* parent = current->parent();
  size_t seek = 0;
  while (parent != NULL && seek != distance) {
    seek++;
    current = parent;
    parent = parent->parent();
  }
  return current;
}
