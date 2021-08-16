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

#ifndef SHARE_CLASSFILE_PLACEHOLDERS_HPP
#define SHARE_CLASSFILE_PLACEHOLDERS_HPP

#include "utilities/hashtable.hpp"

class PlaceholderEntry;
class Thread;
class ClassLoaderData;
class Symbol;

// Placeholder objects. These represent classes currently
// being loaded, as well as arrays of primitives.
//

class PlaceholderTable : public Hashtable<Symbol*, mtClass> {

public:
  PlaceholderTable(int table_size);

  PlaceholderEntry* new_entry(int hash, Symbol* name, ClassLoaderData* loader_data, Symbol* supername);
  void free_entry(PlaceholderEntry* entry);

  PlaceholderEntry* bucket(int i) const {
    return (PlaceholderEntry*)Hashtable<Symbol*, mtClass>::bucket(i);
  }

  PlaceholderEntry** bucket_addr(int i) {
    return (PlaceholderEntry**)Hashtable<Symbol*, mtClass>::bucket_addr(i);
  }

  PlaceholderEntry* add_entry(unsigned int hash, Symbol* name,
                              ClassLoaderData* loader_data,
                              Symbol* supername);

  // This returns a Symbol* to match type for SystemDictionary
  Symbol* find_entry(unsigned int hash,
                     Symbol* name, ClassLoaderData* loader_data);

  PlaceholderEntry* get_entry(unsigned int hash,
                              Symbol* name, ClassLoaderData* loader_data);

// caller to create a placeholder entry must enumerate an action
// caller claims ownership of that action
// For parallel classloading:
// multiple LOAD_INSTANCE threads can proceed in parallel
// multiple LOAD_SUPER threads can proceed in parallel
// LOAD_SUPER needed to check for class circularity
// DEFINE_CLASS: ultimately define class must be single threaded
// on a class/classloader basis
// so the head of that queue owns the token
// and the rest of the threads return the result the first thread gets
 enum classloadAction {
    LOAD_INSTANCE = 1,             // calling load_instance_class
    LOAD_SUPER = 2,                // loading superclass for this class
    DEFINE_CLASS = 3               // find_or_define class
 };

  // find_and_add returns probe pointer - old or new
  // If no entry exists, add a placeholder entry and push SeenThread for classloadAction
  // If entry exists, reuse entry and push SeenThread for classloadAction
  PlaceholderEntry* find_and_add(unsigned int hash,
                                 Symbol* name, ClassLoaderData* loader_data,
                                 classloadAction action, Symbol* supername,
                                 JavaThread* thread);

  void remove_entry(unsigned int hash,
                    Symbol* name, ClassLoaderData* loader_data);

  // find_and_remove first removes SeenThread for classloadAction
  // If all queues are empty and definer is null, remove the PlacheholderEntry completely
  void find_and_remove(unsigned int hash,
                       Symbol* name, ClassLoaderData* loader_data,
                       classloadAction action, JavaThread* thread);

  void print_on(outputStream* st) const;
  void print() const;
  void verify();
};

class SeenThread;

// Placeholder objects represent classes currently being loaded.
// All threads examining the placeholder table must hold the
// SystemDictionary_lock, so we don't need special precautions
// on store ordering here.
// The system dictionary is the only user of this class.

class PlaceholderEntry : public HashtableEntry<Symbol*, mtClass> {

  friend class PlaceholderTable;

 private:
  ClassLoaderData*  _loader_data;   // initiating loader
  Symbol*           _supername;
  JavaThread*       _definer;       // owner of define token
  InstanceKlass*    _instanceKlass; // InstanceKlass from successful define
  SeenThread*       _superThreadQ;  // doubly-linked queue of Threads loading a superclass for this class
  SeenThread*       _loadInstanceThreadQ;  // loadInstance thread
                                    // This can't be multiple threads since class loading waits for
                                    // this token to be removed.

  SeenThread*       _defineThreadQ; // queue of Threads trying to define this class
                                    // including _definer
                                    // _definer owns token
                                    // queue waits for and returns results from _definer

  SeenThread* actionToQueue(PlaceholderTable::classloadAction action);
  void set_threadQ(SeenThread* seenthread, PlaceholderTable::classloadAction action);
  void add_seen_thread(JavaThread* thread, PlaceholderTable::classloadAction action);
  bool remove_seen_thread(JavaThread* thread, PlaceholderTable::classloadAction action);

 public:
  // Simple accessors, used only by SystemDictionary
  Symbol*            klassname()           const { return literal(); }

  ClassLoaderData*   loader_data()         const { return _loader_data; }
  void               set_loader_data(ClassLoaderData* loader_data) { _loader_data = loader_data; }

  Symbol*            supername()           const { return _supername; }
  void               set_supername(Symbol* supername) {
    _supername = supername;
    if (_supername != NULL) _supername->increment_refcount();
  }

  JavaThread*        definer()             const {return _definer; }
  void               set_definer(JavaThread* definer) { _definer = definer; }

  InstanceKlass*     instance_klass()      const {return _instanceKlass; }
  void               set_instance_klass(InstanceKlass* ik) { _instanceKlass = ik; }

  SeenThread*        superThreadQ()        const { return _superThreadQ; }
  void               set_superThreadQ(SeenThread* SeenThread) { _superThreadQ = SeenThread; }

  SeenThread*        loadInstanceThreadQ() const { return _loadInstanceThreadQ; }
  void               set_loadInstanceThreadQ(SeenThread* SeenThread) { _loadInstanceThreadQ = SeenThread; }

  SeenThread*        defineThreadQ()       const { return _defineThreadQ; }
  void               set_defineThreadQ(SeenThread* SeenThread) { _defineThreadQ = SeenThread; }

  PlaceholderEntry* next() const {
    return (PlaceholderEntry*)HashtableEntry<Symbol*, mtClass>::next();
  }

  PlaceholderEntry** next_addr() {
    return (PlaceholderEntry**)HashtableEntry<Symbol*, mtClass>::next_addr();
  }

  // Test for equality
  // Entries are unique for class/classloader name pair
  bool equals(Symbol* class_name, ClassLoaderData* loader) const {
    return (klassname() == class_name && loader_data() == loader);
  }

  bool super_load_in_progress() {
     return (_superThreadQ != NULL);
  }

  bool instance_load_in_progress() {
    return (_loadInstanceThreadQ != NULL);
  }

  bool define_class_in_progress() {
    return (_defineThreadQ != NULL);
  }

  // Used for ClassCircularityError checking
  bool check_seen_thread(JavaThread* thread, PlaceholderTable::classloadAction action);

  // Print method doesn't append a cr
  void print_entry(outputStream* st) const;
  void verify() const;
};

#endif // SHARE_CLASSFILE_PLACEHOLDERS_HPP
