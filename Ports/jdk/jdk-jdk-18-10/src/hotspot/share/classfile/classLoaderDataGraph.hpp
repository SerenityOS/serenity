/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_CLASSLOADERDATAGRAPH_HPP
#define SHARE_CLASSFILE_CLASSLOADERDATAGRAPH_HPP

#include "classfile/classLoaderData.hpp"
#include "memory/allocation.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/macros.hpp"

// GC root for walking class loader data created

class ClassLoaderDataGraph : public AllStatic {
  friend class ClassLoaderData;
  friend class ClassLoaderDataGraphMetaspaceIterator;
  friend class ClassLoaderDataGraphKlassIteratorAtomic;
  friend class ClassLoaderDataGraphKlassIteratorStatic;
  friend class ClassLoaderDataGraphIterator;
  friend class VMStructs;
 private:
  // All CLDs (except the null CLD) can be reached by walking _head->_next->...
  static ClassLoaderData* volatile _head;
  static ClassLoaderData* _unloading;

  // Set if there's anything to purge in the deallocate lists or previous versions
  // during a safepoint after class unloading in a full GC.
  static bool _should_clean_deallocate_lists;
  static bool _safepoint_cleanup_needed;

  // OOM has been seen in metaspace allocation. Used to prevent some
  // allocations until class unloading
  static bool _metaspace_oom;

  static volatile size_t  _num_instance_classes;
  static volatile size_t  _num_array_classes;

  static ClassLoaderData* add_to_graph(Handle class_loader, bool has_class_mirror_holder);

 public:
  static ClassLoaderData* find_or_create(Handle class_loader);
  static ClassLoaderData* add(Handle class_loader, bool has_class_mirror_holder);
  static void clean_module_and_package_info();
  static void purge(bool at_safepoint);
  static void clear_claimed_marks();
  static void clear_claimed_marks(int claim);
  // Iteration through CLDG inside a safepoint; GC support
  static void cld_do(CLDClosure* cl);
  static void cld_unloading_do(CLDClosure* cl);
  static void roots_cld_do(CLDClosure* strong, CLDClosure* weak);
  static void always_strong_cld_do(CLDClosure* cl);
  // Iteration through CLDG not by GC.
  static void loaded_cld_do(CLDClosure* cl);
  // klass do
  // Walking classes through the ClassLoaderDataGraph include array classes.  It also includes
  // classes that are allocated but not loaded, classes that have errors, and scratch classes
  // for redefinition.  These classes are removed during the next class unloading.
  // Walking the ClassLoaderDataGraph also includes hidden classes.
  static void classes_do(KlassClosure* klass_closure);
  static void classes_do(void f(Klass* const));
  static void methods_do(void f(Method*));
  static void modules_do(void f(ModuleEntry*));
  static void modules_unloading_do(void f(ModuleEntry*));
  static void packages_do(void f(PackageEntry*));
  static void packages_unloading_do(void f(PackageEntry*));
  static void loaded_classes_do(KlassClosure* klass_closure);
  static void classes_unloading_do(void f(Klass* const));
  static bool do_unloading();

  static inline bool should_clean_metaspaces_and_reset();
  static void set_should_clean_deallocate_lists() { _should_clean_deallocate_lists = true; }
  static void clean_deallocate_lists(bool purge_previous_versions);
  // Called from ServiceThread
  static void safepoint_and_clean_metaspaces();
  // Called from VMOperation
  static void walk_metadata_and_clean_metaspaces();

  // dictionary do
  // Iterate over all klasses in dictionary, but
  // just the classes from defining class loaders.
  static void dictionary_classes_do(void f(InstanceKlass*));
  // Added for initialize_itable_for_klass to handle exceptions.
  static void dictionary_classes_do(void f(InstanceKlass*, TRAPS), TRAPS);

  // VM_CounterDecay iteration support
  static InstanceKlass* try_get_next_class();
  static void adjust_saved_class(ClassLoaderData* cld);
  static void adjust_saved_class(Klass* klass);

  static void verify_dictionary();
  static void print_dictionary(outputStream* st);
  static void print_table_statistics(outputStream* st);

  static int resize_dictionaries();

  static bool has_metaspace_oom()           { return _metaspace_oom; }
  static void set_metaspace_oom(bool value) { _metaspace_oom = value; }

  static void print_on(outputStream * const out) PRODUCT_RETURN;
  static void print();
  static void verify();

  // instance and array class counters
  static inline size_t num_instance_classes();
  static inline size_t num_array_classes();
  static inline void inc_instance_classes(size_t count);
  static inline void dec_instance_classes(size_t count);
  static inline void inc_array_classes(size_t count);
  static inline void dec_array_classes(size_t count);

#ifndef PRODUCT
  static bool contains_loader_data(ClassLoaderData* loader_data);
#endif

  // Check if ClassLoaderData is part of the ClassLoaderDataGraph (not unloaded)
  // Usage without lock only allowed during error reporting.
  static bool is_valid(ClassLoaderData* loader_data);
};

class LockedClassesDo : public KlassClosure {
  typedef void (*classes_do_func_t)(Klass*);
  classes_do_func_t _function;
  bool _do_lock;
public:
  LockedClassesDo();  // For callers who provide their own do_klass
  LockedClassesDo(classes_do_func_t function);
  ~LockedClassesDo();

  void do_klass(Klass* k) {
    (*_function)(k);
  }
};

// An iterator that distributes Klasses to parallel worker threads.
class ClassLoaderDataGraphKlassIteratorAtomic : public StackObj {
 Klass* volatile _next_klass;
 public:
  ClassLoaderDataGraphKlassIteratorAtomic();
  Klass* next_klass();
 private:
  static Klass* next_klass_in_cldg(Klass* klass);
};

#endif // SHARE_CLASSFILE_CLASSLOADERDATAGRAPH_HPP
