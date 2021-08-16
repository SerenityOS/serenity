/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/log.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/metaspaceClosure.hpp"
#include "oops/annotations.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/recordComponent.hpp"
#include "utilities/globalDefinitions.hpp"

RecordComponent* RecordComponent::allocate(ClassLoaderData* loader_data,
                                           u2 name_index, u2 descriptor_index,
                                           u2 attributes_count,
                                           u2 generic_signature_index,
                                           AnnotationArray* annotations,
                                           AnnotationArray* type_annotations, TRAPS) {
  return new (loader_data, size(), MetaspaceObj::RecordComponentType, THREAD)
         RecordComponent(name_index, descriptor_index, attributes_count,
                         generic_signature_index, annotations, type_annotations);
}

void RecordComponent::deallocate_contents(ClassLoaderData* loader_data) {
  if (annotations() != NULL) {
    MetadataFactory::free_array<u1>(loader_data, annotations());
  }
  if (type_annotations() != NULL) {
    MetadataFactory::free_array<u1>(loader_data, type_annotations());
  }
}

void RecordComponent::metaspace_pointers_do(MetaspaceClosure* it) {
  log_trace(cds)("Iter(RecordComponent): %p", this);
  it->push(&_annotations);
  it->push(&_type_annotations);
}

void RecordComponent::print_value_on(outputStream* st) const {
  st->print("RecordComponent(" INTPTR_FORMAT ")", p2i(this));
}

#ifndef PRODUCT
void RecordComponent::print_on(outputStream* st) const {
  st->print("name_index: %d", _name_index);
  st->print(" - descriptor_index: %d", _descriptor_index);
  st->print(" - attributes_count: %d", _attributes_count);
  if (_generic_signature_index != 0) {
    st->print(" - generic_signature_index: %d", _generic_signature_index);
  }
  st->cr();
  if (_annotations != NULL) {
    st->print_cr("record component annotations");
    _annotations->print_value_on(st);
  }
  if (_type_annotations != NULL) {
    st->print_cr("record component type annotations");
    _type_annotations->print_value_on(st);
  }
}
#endif // PRODUCT
