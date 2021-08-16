/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoaderData.hpp"
#include "logging/log.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/metaspaceClosure.hpp"
#include "memory/oopFactory.hpp"
#include "oops/annotations.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "utilities/ostream.hpp"

// Allocate annotations in metadata area
Annotations* Annotations::allocate(ClassLoaderData* loader_data, TRAPS) {
  return new (loader_data, size(), MetaspaceObj::AnnotationsType, THREAD) Annotations();
}

// helper
void Annotations::free_contents(ClassLoaderData* loader_data, Array<AnnotationArray*>* p) {
  if (p != NULL) {
    for (int i = 0; i < p->length(); i++) {
      MetadataFactory::free_array<u1>(loader_data, p->at(i));
    }
    MetadataFactory::free_array<AnnotationArray*>(loader_data, p);
  }
}

void Annotations::deallocate_contents(ClassLoaderData* loader_data) {
  if (class_annotations() != NULL) {
    MetadataFactory::free_array<u1>(loader_data, class_annotations());
  }
  free_contents(loader_data, fields_annotations());

  if (class_type_annotations() != NULL) {
    MetadataFactory::free_array<u1>(loader_data, class_type_annotations());
  }
  free_contents(loader_data, fields_type_annotations());
}

// Copy annotations to JVM call or reflection to the java heap.
// The alternative to creating this array and adding to Java heap pressure
// is to have a hashtable of the already created typeArrayOops
typeArrayOop Annotations::make_java_array(AnnotationArray* annotations, TRAPS) {
  if (annotations != NULL) {
    int length = annotations->length();
    typeArrayOop copy = oopFactory::new_byteArray(length, CHECK_NULL);
    for (int i = 0; i< length; i++) {
      copy->byte_at_put(i, annotations->at(i));
    }
    return copy;
  } else {
    return NULL;
  }
}

void Annotations::metaspace_pointers_do(MetaspaceClosure* it) {
  log_trace(cds)("Iter(Annotations): %p", this);
  it->push(&_class_annotations);
  it->push(&_fields_annotations);
  it->push(&_class_type_annotations);
  it->push(&_fields_type_annotations); // FIXME: need a test case where _fields_type_annotations != NULL
}

void Annotations::print_value_on(outputStream* st) const {
  st->print("Annotations(" INTPTR_FORMAT ")", p2i(this));
}

#define BULLET  " - "

#ifndef PRODUCT
void Annotations::print_on(outputStream* st) const {
  st->print(BULLET"class_annotations            "); class_annotations()->print_value_on(st);
  st->print(BULLET"fields_annotations           "); fields_annotations()->print_value_on(st);
  st->print(BULLET"class_type_annotations       "); class_type_annotations()->print_value_on(st);
  st->print(BULLET"fields_type_annotations      "); fields_type_annotations()->print_value_on(st);
}
#endif // PRODUCT
