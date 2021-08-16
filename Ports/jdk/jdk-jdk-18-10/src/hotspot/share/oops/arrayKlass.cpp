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

#include "precompiled.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/moduleEntry.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "jvmtifiles/jvmti.h"
#include "memory/metaspaceClosure.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/arrayKlass.hpp"
#include "oops/arrayOop.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"

int ArrayKlass::static_size(int header_size) {
  // size of an array klass object
  assert(header_size <= InstanceKlass::header_size(), "bad header size");
  // If this assert fails, see comments in base_create_array_klass.
  header_size = InstanceKlass::header_size();
  int vtable_len = Universe::base_vtable_size();
  int size = header_size + vtable_len;
  return align_metadata_size(size);
}


InstanceKlass* ArrayKlass::java_super() const {
  if (super() == NULL)  return NULL;  // bootstrap case
  // Array klasses have primary supertypes which are not reported to Java.
  // Example super chain:  String[][] -> Object[][] -> Object[] -> Object
  return vmClasses::Object_klass();
}


oop ArrayKlass::multi_allocate(int rank, jint* sizes, TRAPS) {
  ShouldNotReachHere();
  return NULL;
}

// find field according to JVM spec 5.4.3.2, returns the klass in which the field is defined
Klass* ArrayKlass::find_field(Symbol* name, Symbol* sig, fieldDescriptor* fd) const {
  // There are no fields in an array klass but look to the super class (Object)
  assert(super(), "super klass must be present");
  return super()->find_field(name, sig, fd);
}

Method* ArrayKlass::uncached_lookup_method(const Symbol* name,
                                           const Symbol* signature,
                                           OverpassLookupMode overpass_mode,
                                           PrivateLookupMode private_mode) const {
  // There are no methods in an array klass but the super class (Object) has some
  assert(super(), "super klass must be present");
  // Always ignore overpass methods in superclasses, although technically the
  // super klass of an array, (j.l.Object) should not have
  // any overpass methods present.
  return super()->uncached_lookup_method(name, signature, OverpassLookupMode::skip, private_mode);
}

ArrayKlass::ArrayKlass(Symbol* name, KlassID id) :
  Klass(id),
  _dimension(1),
  _higher_dimension(NULL),
  _lower_dimension(NULL) {
    // Arrays don't add any new methods, so their vtable is the same size as
    // the vtable of klass Object.
    set_vtable_length(Universe::base_vtable_size());
    set_name(name);
    set_super(Universe::is_bootstrapping() ? NULL : vmClasses::Object_klass());
    set_layout_helper(Klass::_lh_neutral_value);
    set_is_cloneable(); // All arrays are considered to be cloneable (See JLS 20.1.5)
    JFR_ONLY(INIT_ID(this);)
}


// Initialization of vtables and mirror object is done separatly from base_create_array_klass,
// since a GC can happen. At this point all instance variables of the ArrayKlass must be setup.
void ArrayKlass::complete_create_array_klass(ArrayKlass* k, Klass* super_klass, ModuleEntry* module_entry, TRAPS) {
  k->initialize_supers(super_klass, NULL, CHECK);
  k->vtable().initialize_vtable();

  // During bootstrapping, before java.base is defined, the module_entry may not be present yet.
  // These classes will be put on a fixup list and their module fields will be patched once
  // java.base is defined.
  assert((module_entry != NULL) || ((module_entry == NULL) && !ModuleEntryTable::javabase_defined()),
         "module entry not available post " JAVA_BASE_NAME " definition");
  oop module = (module_entry != NULL) ? module_entry->module() : (oop)NULL;
  java_lang_Class::create_mirror(k, Handle(THREAD, k->class_loader()), Handle(THREAD, module), Handle(), Handle(), CHECK);
}

GrowableArray<Klass*>* ArrayKlass::compute_secondary_supers(int num_extra_slots,
                                                            Array<InstanceKlass*>* transitive_interfaces) {
  // interfaces = { cloneable_klass, serializable_klass };
  assert(num_extra_slots == 0, "sanity of primitive array type");
  assert(transitive_interfaces == NULL, "sanity");
  // Must share this for correct bootstrapping!
  set_secondary_supers(Universe::the_array_interfaces_array());
  return NULL;
}

objArrayOop ArrayKlass::allocate_arrayArray(int n, int length, TRAPS) {
  check_array_allocation_length(length, arrayOopDesc::max_array_length(T_ARRAY), CHECK_NULL);
  int size = objArrayOopDesc::object_size(length);
  Klass* k = array_klass(n+dimension(), CHECK_NULL);
  ArrayKlass* ak = ArrayKlass::cast(k);
  objArrayOop o = (objArrayOop)Universe::heap()->array_allocate(ak, size, length,
                                                                /* do_zero */ true, CHECK_NULL);
  // initialization to NULL not necessary, area already cleared
  return o;
}

void ArrayKlass::array_klasses_do(void f(Klass* k, TRAPS), TRAPS) {
  Klass* k = this;
  // Iterate over this array klass and all higher dimensions
  while (k != NULL) {
    f(k, CHECK);
    k = ArrayKlass::cast(k)->higher_dimension();
  }
}

void ArrayKlass::array_klasses_do(void f(Klass* k)) {
  Klass* k = this;
  // Iterate over this array klass and all higher dimensions
  while (k != NULL) {
    f(k);
    k = ArrayKlass::cast(k)->higher_dimension();
  }
}

jint ArrayKlass::compute_modifier_flags() const {
  return JVM_ACC_ABSTRACT | JVM_ACC_FINAL | JVM_ACC_PUBLIC;
}

// JVMTI support

jint ArrayKlass::jvmti_class_status() const {
  return JVMTI_CLASS_STATUS_ARRAY;
}

void ArrayKlass::metaspace_pointers_do(MetaspaceClosure* it) {
  Klass::metaspace_pointers_do(it);

  ResourceMark rm;
  log_trace(cds)("Iter(ArrayKlass): %p (%s)", this, external_name());

  // need to cast away volatile
  it->push((Klass**)&_higher_dimension);
  it->push((Klass**)&_lower_dimension);
}

void ArrayKlass::remove_unshareable_info() {
  Klass::remove_unshareable_info();
  if (_higher_dimension != NULL) {
    ArrayKlass *ak = ArrayKlass::cast(higher_dimension());
    ak->remove_unshareable_info();
  }
}

void ArrayKlass::remove_java_mirror() {
  Klass::remove_java_mirror();
  if (_higher_dimension != NULL) {
    ArrayKlass *ak = ArrayKlass::cast(higher_dimension());
    ak->remove_java_mirror();
  }
}

void ArrayKlass::restore_unshareable_info(ClassLoaderData* loader_data, Handle protection_domain, TRAPS) {
  assert(loader_data == ClassLoaderData::the_null_class_loader_data(), "array classes belong to null loader");
  Klass::restore_unshareable_info(loader_data, protection_domain, CHECK);
  // Klass recreates the component mirror also

  if (_higher_dimension != NULL) {
    ArrayKlass *ak = ArrayKlass::cast(higher_dimension());
    ak->restore_unshareable_info(loader_data, protection_domain, CHECK);
  }
}

// Printing

void ArrayKlass::print_on(outputStream* st) const {
  assert(is_klass(), "must be klass");
  Klass::print_on(st);
}

void ArrayKlass::print_value_on(outputStream* st) const {
  assert(is_klass(), "must be klass");
  for(int index = 0; index < dimension(); index++) {
    st->print("[]");
  }
}

void ArrayKlass::oop_print_on(oop obj, outputStream* st) {
  assert(obj->is_array(), "must be array");
  Klass::oop_print_on(obj, st);
  st->print_cr(" - length: %d", arrayOop(obj)->length());
}


// Verification

void ArrayKlass::verify_on(outputStream* st) {
  Klass::verify_on(st);
}

void ArrayKlass::oop_verify_on(oop obj, outputStream* st) {
  guarantee(obj->is_array(), "must be array");
  arrayOop a = arrayOop(obj);
  guarantee(a->length() >= 0, "array with negative length?");
}
