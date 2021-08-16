/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_PACKAGEENTRY_HPP
#define SHARE_CLASSFILE_PACKAGEENTRY_HPP

#include "classfile/moduleEntry.hpp"
#include "oops/symbol.hpp"
#include "runtime/atomic.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/hashtable.hpp"
#include "utilities/macros.hpp"
#include "utilities/ostream.hpp"
#if INCLUDE_JFR
#include "jfr/support/jfrTraceIdExtension.hpp"
#endif

template <class T> class Array;
class MetaspaceClosure;

// A PackageEntry basically represents a Java package.  It contains:
//   - Symbol* containing the package's name.
//   - ModuleEntry* for this package's containing module.
//   - a field indicating if the package is exported unqualifiedly or to all
//     unnamed modules.
//   - a growable array containing other module entries that this
//     package is exported to.
//
// Packages can be exported in the following 3 ways:
//   - not exported:        the package does not have qualified or unqualified exports.
//   - qualified exports:   the package has been explicitly qualified to at least
//                            one particular module or has been qualifiedly exported
//                            to all unnamed modules.
//                            Note: being exported to all unnamed is a form of a qualified
//                            export. It is equivalent to the package being explicitly
//                            exported to all current and future unnamed modules.
//   - unqualified exports: the package is exported to all modules.
//
// A package can transition from:
//   - being not exported, to being exported either in a qualified or unqualified manner
//   - being qualifiedly exported, to unqualifiedly exported. Its exported scope is widened.
//
// A package cannot transition from:
//   - being unqualifiedly exported, to exported qualifiedly to a specific module.
//       This transition attempt is silently ignored in set_exported.
//   - being qualifiedly exported to not exported.
//       Because transitions are only allowed from less exposure to greater exposure,
//       the transition from qualifiedly exported to not exported would be considered
//       a backward direction.  Therefore the implementation considers a package as
//       qualifiedly exported even if its export-list exists but is empty.
//
// The Mutex Module_lock is shared between ModuleEntry and PackageEntry, to lock either
// data structure.

// PKG_EXP_UNQUALIFIED and PKG_EXP_ALLUNNAMED indicate whether the package is
// exported unqualifiedly or exported to all unnamed modules.  They are used to
// set the value of _export_flags.  Field _export_flags and the _qualified_exports
// list are used to determine a package's export state.
// Valid states are:
//
//   1. Package is not exported
//      _export_flags is zero and _qualified_exports is null
//   2. Package is unqualifiedly exported
//      _export_flags is set to PKG_EXP_UNQUALIFIED
//      _qualified_exports may or may not be null depending on whether the package
//        transitioned from qualifiedly exported to unqualifiedly exported.
//   3. Package is qualifiedly exported
//      _export_flags may be set to PKG_EXP_ALLUNNAMED if the package is also
//        exported to all unnamed modules
//      _qualified_exports will be non-null
//   4. Package is exported to all unnamed modules
//      _export_flags is set to PKG_EXP_ALLUNNAMED
//      _qualified_exports may or may not be null depending on whether the package
//        is also qualifiedly exported to one or more named modules.
#define PKG_EXP_UNQUALIFIED  0x0001
#define PKG_EXP_ALLUNNAMED   0x0002
#define PKG_EXP_UNQUALIFIED_OR_ALL_UNAMED (PKG_EXP_UNQUALIFIED | PKG_EXP_ALLUNNAMED)

class PackageEntry : public HashtableEntry<Symbol*, mtModule> {
private:
  ModuleEntry* _module;
  // Indicates if package is exported unqualifiedly or to all unnamed. Access to
  // this field is protected by the Module_lock.
  int _export_flags;
  // Used to indicate for packages with classes loaded by the boot loader that
  // a class in that package has been loaded.  And, for packages with classes
  // loaded by the boot loader from -Xbootclasspath/a in an unnamed module, it
  // indicates from which class path entry.
  s2 _classpath_index;
  bool _must_walk_exports;
  // Contains list of modules this package is qualifiedly exported to.  Access
  // to this list is protected by the Module_lock.
  GrowableArray<ModuleEntry*>* _qualified_exports;
  JFR_ONLY(DEFINE_TRACE_ID_FIELD;)

  // Initial size of a package entry's list of qualified exports.
  enum {QUAL_EXP_SIZE = 43};

  // a bit map indicating which CDS classpath entries have defined classes in this package.
  volatile int _defined_by_cds_in_class_path;
public:
  void init() {
    _module = NULL;
    _export_flags = 0;
    _classpath_index = -1;
    _must_walk_exports = false;
    _qualified_exports = NULL;
    _defined_by_cds_in_class_path = 0;
  }

  // package name
  Symbol*            name() const               { return literal(); }

  // the module containing the package definition
  ModuleEntry*       module() const             { return _module; }
  void               set_module(ModuleEntry* m) { _module = m; }

  // package's export state
  bool is_exported() const { // qualifiedly or unqualifiedly exported
    assert_locked_or_safepoint(Module_lock);
    return module()->is_open() ||
            ((_export_flags & PKG_EXP_UNQUALIFIED_OR_ALL_UNAMED) != 0) ||
            has_qual_exports_list();
  }
  // Returns true if the package has any explicit qualified exports or is exported to all unnamed
  bool is_qual_exported() const {
    assert_locked_or_safepoint(Module_lock);
    return (has_qual_exports_list() || is_exported_allUnnamed());
  }
  // Returns true if there are any explicit qualified exports.  Note that even
  // if the _qualified_exports list is now empty (because the modules that were
  // on the list got gc-ed and deleted from the list) this method may still
  // return true.
  bool has_qual_exports_list() const {
    assert_locked_or_safepoint(Module_lock);
    return (!is_unqual_exported() && _qualified_exports != NULL);
  }
  bool is_exported_allUnnamed() const {
    assert_locked_or_safepoint(Module_lock);
    return (module()->is_open() || _export_flags == PKG_EXP_ALLUNNAMED);
  }
  bool is_unqual_exported() const {
    assert_locked_or_safepoint(Module_lock);
    return (module()->is_open() || _export_flags == PKG_EXP_UNQUALIFIED);
  }

  // Explicitly set _export_flags to PKG_EXP_UNQUALIFIED and clear
  // PKG_EXP_ALLUNNAMED, if it was set.
  void set_unqual_exported() {
    if (module()->is_open()) {
        // No-op for open modules since all packages are unqualifiedly exported
        return;
    }
    assert(Module_lock->owned_by_self(), "should have the Module_lock");
    _export_flags = PKG_EXP_UNQUALIFIED;
  }

  bool exported_pending_delete() const;

  void set_exported(ModuleEntry* m);

  void set_is_exported_allUnnamed();

  void set_classpath_index(s2 classpath_index) {
    _classpath_index = classpath_index;
  }
  s2 classpath_index() const { return _classpath_index; }

  bool has_loaded_class() const { return _classpath_index != -1; }

  // returns true if the package is defined in the unnamed module
  bool in_unnamed_module() const  { return !_module->is_named(); }

  // returns true if the package specifies m as a qualified export, including through an unnamed export
  bool is_qexported_to(ModuleEntry* m) const;

  // add the module to the package's qualified exports
  void add_qexport(ModuleEntry* m);
  void set_export_walk_required(ClassLoaderData* m_loader_data);

  PackageEntry* next() const {
    return (PackageEntry*)HashtableEntry<Symbol*, mtModule>::next();
  }

  PackageEntry** next_addr() {
    return (PackageEntry**)HashtableEntry<Symbol*, mtModule>::next_addr();
  }

  // iteration of qualified exports
  void package_exports_do(ModuleClosure* f);

  JFR_ONLY(DEFINE_TRACE_ID_METHODS;)

  // Purge dead weak references out of exported list when any given class loader is unloaded.
  void purge_qualified_exports();
  void delete_qualified_exports();

  void print(outputStream* st = tty);
  void verify();

#if INCLUDE_CDS_JAVA_HEAP
  void iterate_symbols(MetaspaceClosure* closure);
  PackageEntry* allocate_archived_entry() const;
  void init_as_archived_entry();
  static PackageEntry* get_archived_entry(PackageEntry* orig_entry);
  void load_from_archive();
#endif

  static int max_index_for_defined_in_class_path() {
    return sizeof(int) * BitsPerByte;
  }

  bool is_defined_by_cds_in_class_path(int idx) const {
    assert(idx < max_index_for_defined_in_class_path(), "sanity");
    return((Atomic::load(&_defined_by_cds_in_class_path) & ((int)1 << idx)) != 0);
  }
  void set_defined_by_cds_in_class_path(int idx) {
    assert(idx < max_index_for_defined_in_class_path(), "sanity");
    int old_val = 0;
    int new_val = 0;
    do {
      old_val = Atomic::load(&_defined_by_cds_in_class_path);
      new_val = old_val | ((int)1 << idx);
    } while (Atomic::cmpxchg(&_defined_by_cds_in_class_path, old_val, new_val) != old_val);
  }
};

// The PackageEntryTable is a Hashtable containing a list of all packages defined
// by a particular class loader.  Each package is represented as a PackageEntry node.
// The PackageEntryTable's lookup is lock free.
//
class PackageEntryTable : public Hashtable<Symbol*, mtModule> {
  friend class VMStructs;
public:
  enum Constants {
    _packagetable_entry_size = 109  // number of entries in package entry table
  };

private:
  PackageEntry* new_entry(unsigned int hash, Symbol* name, ModuleEntry* module);
  void add_entry(int index, PackageEntry* new_entry);

  int entry_size() const { return BasicHashtable<mtModule>::entry_size(); }

  PackageEntry** bucket_addr(int i) {
    return (PackageEntry**)Hashtable<Symbol*, mtModule>::bucket_addr(i);
  }

  static unsigned int compute_hash(Symbol* name) { return (unsigned int)(name->identity_hash()); }
  int index_for(Symbol* name) const { return hash_to_index(compute_hash(name)); }

public:
  PackageEntryTable(int table_size);
  ~PackageEntryTable();

  PackageEntry* bucket(int i) {
    return (PackageEntry*)Hashtable<Symbol*, mtModule>::bucket(i);
  }

  // Create package entry in loader's package entry table.  Assume Module
  // lock was taken by caller.
  void locked_create_entry(Symbol* name, ModuleEntry* module);

  // Create package entry in loader's package entry table if it does not
  // already exist.  Assume Module lock was taken by caller.
  void locked_create_entry_if_not_exist(Symbol* name, ModuleEntry* module);

  // Lookup Package with loader's package entry table, add it if not found.
  // This will acquire the Module lock.
  PackageEntry* lookup(Symbol* name, ModuleEntry* module);

  // Only lookup Package within loader's package entry table.
  // This will acquire the Module lock.
  PackageEntry* lookup_only(Symbol* Package);

  // Only lookup Package within loader's package entry table.  Assume Module lock
  // was taken by caller.
  PackageEntry* locked_lookup_only(Symbol* Package);

  void verify_javabase_packages(GrowableArray<Symbol*> *pkg_list);

  // purge dead weak references out of exported list
  void purge_all_package_exports();

  void print(outputStream* st = tty);
  void verify();

#if INCLUDE_CDS_JAVA_HEAP
  void iterate_symbols(MetaspaceClosure* closure);
  Array<PackageEntry*>* allocate_archived_entries();
  void init_archived_entries(Array<PackageEntry*>* archived_packages);
  void load_archived_entries(Array<PackageEntry*>* archived_packages);
#endif
};

#endif // SHARE_CLASSFILE_PACKAGEENTRY_HPP
