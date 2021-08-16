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

#ifndef SHARE_OOPS_INSTANCEMIRRORKLASS_HPP
#define SHARE_OOPS_INSTANCEMIRRORKLASS_HPP

#include "classfile/vmClasses.hpp"
#include "oops/instanceKlass.hpp"
#include "runtime/handles.hpp"
#include "utilities/macros.hpp"

class ClassFileParser;

// An InstanceMirrorKlass is a specialized InstanceKlass for
// java.lang.Class instances.  These instances are special because
// they contain the static fields of the class in addition to the
// normal fields of Class.  This means they are variable sized
// instances and need special logic for computing their size and for
// iteration of their oops.


class InstanceMirrorKlass: public InstanceKlass {
  friend class VMStructs;
  friend class InstanceKlass;

 public:
  static const KlassID ID = InstanceMirrorKlassID;

 private:
  static int _offset_of_static_fields;

  InstanceMirrorKlass(const ClassFileParser& parser) : InstanceKlass(parser, InstanceKlass::_kind_mirror, ID) {}

 public:
  InstanceMirrorKlass() { assert(DumpSharedSpaces || UseSharedSpaces, "only for CDS"); }

  static InstanceMirrorKlass* cast(Klass* k) {
    return const_cast<InstanceMirrorKlass*>(cast(const_cast<const Klass*>(k)));
  }

  static const InstanceMirrorKlass* cast(const Klass* k) {
    assert(InstanceKlass::cast(k)->is_mirror_instance_klass(), "cast to InstanceMirrorKlass");
    return static_cast<const InstanceMirrorKlass*>(k);
  }

  // Returns the size of the instance including the extra static fields.
  virtual int oop_size(oop obj) const;

  // Static field offset is an offset into the Heap, should be converted by
  // based on UseCompressedOop for traversal
  static HeapWord* start_of_static_fields(oop obj) {
    return (HeapWord*)(cast_from_oop<intptr_t>(obj) + offset_of_static_fields());
  }

  static void init_offset_of_static_fields() {
    // Cache the offset of the static fields in the Class instance
    assert(_offset_of_static_fields == 0, "once");
    _offset_of_static_fields = InstanceMirrorKlass::cast(vmClasses::Class_klass())->size_helper() << LogHeapWordSize;
  }

  static int offset_of_static_fields() {
    return _offset_of_static_fields;
  }

  int compute_static_oop_field_count(oop obj);

  // Given a Klass return the size of the instance
  int instance_size(Klass* k);

  // allocation
  instanceOop allocate_instance(Klass* k, TRAPS);

  static void serialize_offsets(class SerializeClosure* f) NOT_CDS_RETURN;

  // Oop fields (and metadata) iterators
  //
  // The InstanceMirrorKlass iterators also visit the hidden Klass pointer.

  // Iterate over the static fields.
  template <typename T, class OopClosureType>
  inline void oop_oop_iterate_statics(oop obj, OopClosureType* closure);

  // Forward iteration
  // Iterate over the oop fields and metadata.
  template <typename T, class OopClosureType>
  inline void oop_oop_iterate(oop obj, OopClosureType* closure);

  // Reverse iteration
  // Iterate over the oop fields and metadata.
  template <typename T, class OopClosureType>
  inline void oop_oop_iterate_reverse(oop obj, OopClosureType* closure);

  // Bounded range iteration
  // Iterate over the oop fields and metadata.
  template <typename T, class OopClosureType>
  inline void oop_oop_iterate_bounded(oop obj, OopClosureType* closure, MemRegion mr);

 private:

  // Iterate over the static fields.
  template <typename T, class OopClosureType>
  inline void oop_oop_iterate_statics_bounded(oop obj, OopClosureType* closure, MemRegion mr);
};

#endif // SHARE_OOPS_INSTANCEMIRRORKLASS_HPP
