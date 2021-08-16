/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_ARRAYKLASS_HPP
#define SHARE_OOPS_ARRAYKLASS_HPP

#include "oops/klass.hpp"

class fieldDescriptor;
class klassVtable;

// ArrayKlass is the abstract baseclass for all array classes

class ArrayKlass: public Klass {
  friend class VMStructs;
 private:
  // If you add a new field that points to any metaspace object, you
  // must add this field to ArrayKlass::metaspace_pointers_do().
  int      _dimension;         // This is n'th-dimensional array.
  Klass* volatile _higher_dimension;  // Refers the (n+1)'th-dimensional array (if present).
  Klass* volatile _lower_dimension;   // Refers the (n-1)'th-dimensional array (if present).

 protected:
  // Constructors
  // The constructor with the Symbol argument does the real array
  // initialization, the other is a dummy
  ArrayKlass(Symbol* name, KlassID id);
  ArrayKlass() { assert(DumpSharedSpaces || UseSharedSpaces, "only for cds"); }

 public:
  // Testing operation
  DEBUG_ONLY(bool is_array_klass_slow() const { return true; })

  // Instance variables
  int dimension() const                 { return _dimension;      }
  void set_dimension(int dimension)     { _dimension = dimension; }

  Klass* higher_dimension() const     { return _higher_dimension; }
  inline Klass* higher_dimension_acquire() const; // load with acquire semantics
  void set_higher_dimension(Klass* k) { _higher_dimension = k; }
  inline void release_set_higher_dimension(Klass* k); // store with release semantics

  Klass* lower_dimension() const      { return _lower_dimension; }
  void set_lower_dimension(Klass* k)  { _lower_dimension = k; }

  // offset of first element, including any padding for the sake of alignment
  int  array_header_in_bytes() const    { return layout_helper_header_size(layout_helper()); }
  int  log2_element_size() const        { return layout_helper_log2_element_size(layout_helper()); }
  // type of elements (T_OBJECT for both oop arrays and array-arrays)
  BasicType element_type() const        { return layout_helper_element_type(layout_helper()); }

  virtual InstanceKlass* java_super() const;

  // Allocation
  // Sizes points to the first dimension of the array, subsequent dimensions
  // are always in higher memory.  The callers of these set that up.
  virtual oop multi_allocate(int rank, jint* sizes, TRAPS);
  objArrayOop allocate_arrayArray(int n, int length, TRAPS);

  // find field according to JVM spec 5.4.3.2, returns the klass in which the field is defined
  Klass* find_field(Symbol* name, Symbol* sig, fieldDescriptor* fd) const;

  // Lookup operations
  Method* uncached_lookup_method(const Symbol* name,
                                 const Symbol* signature,
                                 OverpassLookupMode overpass_mode,
                                 PrivateLookupMode private_mode = PrivateLookupMode::find) const;

  static ArrayKlass* cast(Klass* k) {
    return const_cast<ArrayKlass*>(cast(const_cast<const Klass*>(k)));
  }

  static const ArrayKlass* cast(const Klass* k) {
    assert(k->is_array_klass(), "cast to ArrayKlass");
    return static_cast<const ArrayKlass*>(k);
  }

  GrowableArray<Klass*>* compute_secondary_supers(int num_extra_slots,
                                                  Array<InstanceKlass*>* transitive_interfaces);

  // Sizing
  static int static_size(int header_size);

  virtual void metaspace_pointers_do(MetaspaceClosure* iter);

  // Iterators
  void array_klasses_do(void f(Klass* k));
  void array_klasses_do(void f(Klass* k, TRAPS), TRAPS);

  // Return a handle.
  static void     complete_create_array_klass(ArrayKlass* k, Klass* super_klass, ModuleEntry* module, TRAPS);


  // jvm support
  jint compute_modifier_flags() const;

  // JVMTI support
  jint jvmti_class_status() const;

  // CDS support - remove and restore oops from metadata. Oops are not shared.
  virtual void remove_unshareable_info();
  virtual void remove_java_mirror();
  void restore_unshareable_info(ClassLoaderData* loader_data, Handle protection_domain, TRAPS);

  // Printing
  void print_on(outputStream* st) const;
  void print_value_on(outputStream* st) const;

  void oop_print_on(oop obj, outputStream* st);

  // Verification
  void verify_on(outputStream* st);

  void oop_verify_on(oop obj, outputStream* st);
};

#endif // SHARE_OOPS_ARRAYKLASS_HPP
