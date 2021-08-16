/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_INSTANCEREFKLASS_HPP
#define SHARE_OOPS_INSTANCEREFKLASS_HPP

#include "oops/instanceKlass.hpp"
#include "utilities/macros.hpp"

class ClassFileParser;

// An InstanceRefKlass is a specialized InstanceKlass for Java
// classes that are subclasses of java/lang/ref/Reference.
//
// These classes are used to implement soft/weak/final/phantom
// references and finalization, and need special treatment by the
// garbage collector.
//
// During GC discovered reference objects are added (chained) to one
// of the four lists below, depending on the type of reference.
// The linked occurs through the next field in class java/lang/ref/Reference.
//
// Afterwards, the discovered references are processed in decreasing
// order of reachability. Reference objects eligible for notification
// are linked to the static pending_list in class java/lang/ref/Reference,
// and the pending list lock object in the same class is notified.


class InstanceRefKlass: public InstanceKlass {
  friend class InstanceKlass;
 public:
  static const KlassID ID = InstanceRefKlassID;

 private:
  InstanceRefKlass(const ClassFileParser& parser) : InstanceKlass(parser, InstanceKlass::_kind_reference, ID) {}

 public:
  InstanceRefKlass() { assert(DumpSharedSpaces || UseSharedSpaces, "only for CDS"); }

  // Oop fields (and metadata) iterators
  //
  // The InstanceRefKlass iterators also support reference processing.


  // Forward iteration
  // Iterate over all oop fields and metadata.
  template <typename T, class OopClosureType>
  inline void oop_oop_iterate(oop obj, OopClosureType* closure);

  // Reverse iteration
  // Iterate over all oop fields and metadata.
  template <typename T, class OopClosureType>
  inline void oop_oop_iterate_reverse(oop obj, OopClosureType* closure);

  // Bounded range iteration
  // Iterate over all oop fields and metadata.
  template <typename T, class OopClosureType>
  inline void oop_oop_iterate_bounded(oop obj, OopClosureType* closure, MemRegion mr);

  private:

  // Reference processing part of the iterators.

  template <typename T, class OopClosureType, class Contains>
  inline void oop_oop_iterate_ref_processing(oop obj, OopClosureType* closure, Contains& contains);

  // Only perform reference processing if the referent object is within mr.
  template <typename T, class OopClosureType>
  inline void oop_oop_iterate_ref_processing_bounded(oop obj, OopClosureType* closure, MemRegion mr);

  // Reference processing
  template <typename T, class OopClosureType>
  inline void oop_oop_iterate_ref_processing(oop obj, OopClosureType* closure);

  // Building blocks for specialized handling.
  template <typename T, class OopClosureType, class Contains>
  static void do_referent(oop obj, OopClosureType* closure, Contains& contains);

  template <typename T, class OopClosureType, class Contains>
  static void do_next(oop obj, OopClosureType* closure, Contains& contains);

  template <typename T, class OopClosureType, class Contains>
  static void do_discovered(oop obj, OopClosureType* closure, Contains& contains);

  template <typename T, class OopClosureType>
  static bool try_discover(oop obj, ReferenceType type, OopClosureType* closure);

  // Do discovery while handling InstanceRefKlasses. Reference discovery
  // is only done if the closure provides a ReferenceProcessor.
  template <typename T, class OopClosureType, class Contains>
  static void oop_oop_iterate_discovery(oop obj, ReferenceType type, OopClosureType* closure, Contains& contains);

  // Used for a special case in G1 where the closure needs to be applied
  // to the discovered field. Reference discovery is also done if the
  // closure provides a ReferenceProcessor.
  template <typename T, class OopClosureType, class Contains>
  static void oop_oop_iterate_discovered_and_discovery(oop obj, ReferenceType type, OopClosureType* closure, Contains& contains);

  // Apply the closure to all fields. No reference discovery is done.
  template <typename T, class OopClosureType, class Contains>
  static void oop_oop_iterate_fields(oop obj, OopClosureType* closure, Contains& contains);

  // Apply the closure to all fields, except the referent field. No reference discovery is done.
  template <typename T, class OopClosureType, class Contains>
  static void oop_oop_iterate_fields_except_referent(oop obj, OopClosureType* closure, Contains& contains);

  template <typename T>
  static void trace_reference_gc(const char *s, oop obj) NOT_DEBUG_RETURN;

 public:
  // Update non-static oop maps so 'referent', 'nextPending' and
  // 'discovered' will look like non-oops
  static void update_nonstatic_oop_maps(Klass* k);

 public:
  // Verification
  void oop_verify_on(oop obj, outputStream* st);
};

#endif // SHARE_OOPS_INSTANCEREFKLASS_HPP
