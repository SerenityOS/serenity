/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_LOADERCONSTRAINTS_HPP
#define SHARE_CLASSFILE_LOADERCONSTRAINTS_HPP

#include "classfile/placeholders.hpp"
#include "utilities/hashtable.hpp"

class ClassLoaderData;
class LoaderConstraintEntry;
class Symbol;

class LoaderConstraintTable : public Hashtable<InstanceKlass*, mtClass> {

private:
  LoaderConstraintEntry** find_loader_constraint(Symbol* name,
                                                 Handle loader);

public:

  LoaderConstraintTable(int table_size);

  LoaderConstraintEntry* new_entry(unsigned int hash, Symbol* name,
                                   InstanceKlass* klass, int num_loaders,
                                   int max_loaders);
  void free_entry(LoaderConstraintEntry *entry);

  LoaderConstraintEntry* bucket(int i) const {
    return (LoaderConstraintEntry*)Hashtable<InstanceKlass*, mtClass>::bucket(i);
  }

  LoaderConstraintEntry** bucket_addr(int i) {
    return (LoaderConstraintEntry**)Hashtable<InstanceKlass*, mtClass>::bucket_addr(i);
  }

  // Check class loader constraints
  bool add_entry(Symbol* name, InstanceKlass* klass1, Handle loader1,
                                    InstanceKlass* klass2, Handle loader2);

  // Note:  The main entry point for this module is via SystemDictionary.
  // SystemDictionary::check_signature_loaders(Symbol* signature,
  //                                           Klass* klass_being_linked,
  //                                           Handle loader1, Handle loader2,
  //                                           bool is_method)

  InstanceKlass* find_constrained_klass(Symbol* name, Handle loader);

  // Class loader constraints

  void ensure_loader_constraint_capacity(LoaderConstraintEntry *p, int nfree);
  void extend_loader_constraint(LoaderConstraintEntry* p, Handle loader,
                                InstanceKlass* klass);
  void merge_loader_constraints(LoaderConstraintEntry** pp1,
                                LoaderConstraintEntry** pp2, InstanceKlass* klass);

  bool check_or_update(InstanceKlass* k, Handle loader, Symbol* name);

  void purge_loader_constraints();

  void verify(PlaceholderTable* placeholders);
  void print() const;
  void print_on(outputStream* st) const;
};

class LoaderConstraintEntry : public HashtableEntry<InstanceKlass*, mtClass> {
private:
  Symbol*                _name;                   // class name
  int                    _num_loaders;
  int                    _max_loaders;
  // Loader constraints enforce correct linking behavior.
  // Thus, it really operates on ClassLoaderData which represents linking domain,
  // not class loaders.
  ClassLoaderData**              _loaders;                // initiating loaders

public:

  InstanceKlass* klass() { return literal(); }
  InstanceKlass** klass_addr() { return literal_addr(); }
  void set_klass(InstanceKlass* k) { set_literal(k); }

  LoaderConstraintEntry* next() {
    return (LoaderConstraintEntry*)HashtableEntry<InstanceKlass*, mtClass>::next();
  }

  LoaderConstraintEntry** next_addr() {
    return (LoaderConstraintEntry**)HashtableEntry<InstanceKlass*, mtClass>::next_addr();
  }
  void set_next(LoaderConstraintEntry* next) {
    HashtableEntry<InstanceKlass*, mtClass>::set_next(next);
  }

  Symbol* name() { return _name; }
  void set_name(Symbol* name) {
    _name = name;
    if (name != NULL) name->increment_refcount();
  }

  int num_loaders() { return _num_loaders; }
  void set_num_loaders(int i) { _num_loaders = i; }

  int max_loaders() { return _max_loaders; }
  void set_max_loaders(int i) { _max_loaders = i; }

  ClassLoaderData** loaders() { return _loaders; }
  void set_loaders(ClassLoaderData** loaders) { _loaders = loaders; }

  ClassLoaderData* loader_data(int i) { return _loaders[i]; }
  void set_loader_data(int i, ClassLoaderData* p) { _loaders[i] = p; }
  // convenience
  void set_loader(int i, oop p);
};

#endif // SHARE_CLASSFILE_LOADERCONSTRAINTS_HPP
