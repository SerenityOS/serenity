/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_RECORDCOMPONENT_HPP
#define SHARE_OOPS_RECORDCOMPONENT_HPP

#include "oops/annotations.hpp"
#include "oops/metadata.hpp"
#include "utilities/globalDefinitions.hpp"

// This class stores information extracted from the Record class attribute.
class RecordComponent: public MetaspaceObj {
  private:
    AnnotationArray* _annotations;
    AnnotationArray* _type_annotations;
    u2 _name_index;
    u2 _descriptor_index;
    u2 _attributes_count;

    // generic_signature_index gets set if the Record component has a Signature
    // attribute.  A zero value indicates that there was no Signature attribute.
    u2 _generic_signature_index;

  public:
    RecordComponent(u2 name_index, u2 descriptor_index, u2 attributes_count,
                    u2 generic_signature_index, AnnotationArray* annotations,
                    AnnotationArray* type_annotations):
                    _annotations(annotations), _type_annotations(type_annotations),
                    _name_index(name_index), _descriptor_index(descriptor_index),
                    _attributes_count(attributes_count),
                    _generic_signature_index(generic_signature_index) { }

    // Allocate instance of this class
    static RecordComponent* allocate(ClassLoaderData* loader_data,
                                     u2 name_index, u2 descriptor_index,
                                     u2 attributes_count,
                                     u2 generic_signature_index,
                                     AnnotationArray* annotations,
                                     AnnotationArray* type_annotations, TRAPS);

    void deallocate_contents(ClassLoaderData* loader_data);

    u2 name_index() const { return _name_index; }
    void set_name_index(u2 name_index) { _name_index = name_index; }

    u2 descriptor_index() const { return _descriptor_index; }
    void set_descriptor_index(u2 descriptor_index) {
      _descriptor_index = descriptor_index;
    }

    u2 attributes_count() const { return _attributes_count; }

    u2 generic_signature_index() const { return _generic_signature_index; }
    void set_generic_signature_index(u2 generic_signature_index) {
      _generic_signature_index = generic_signature_index;
    }

    AnnotationArray* annotations() const { return _annotations; }
    AnnotationArray* type_annotations() const { return _type_annotations; }

    // Size of RecordComponent, not including size of any annotations.
    static int size() { return sizeof(RecordComponent) / wordSize; }

    void metaspace_pointers_do(MetaspaceClosure* it);
    MetaspaceObj::Type type() const { return RecordComponentType; }

    // Record_components should be stored in the read-only region of CDS archive.
    static bool is_read_only_by_default() { return true; }
    DEBUG_ONLY(bool on_stack() { return false; })  // for template

    bool is_klass() const { return false; }

#ifndef PRODUCT
    void print_on(outputStream* st) const;
#endif
    void print_value_on(outputStream* st) const;

};

#endif // SHARE_OOPS_RECORDCOMPONENT_HPP
