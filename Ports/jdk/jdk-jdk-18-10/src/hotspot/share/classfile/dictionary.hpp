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

#ifndef SHARE_CLASSFILE_DICTIONARY_HPP
#define SHARE_CLASSFILE_DICTIONARY_HPP

#include "oops/instanceKlass.hpp"
#include "oops/oop.hpp"
#include "oops/oopHandle.hpp"
#include "utilities/hashtable.hpp"
#include "utilities/ostream.hpp"

class DictionaryEntry;
class ProtectionDomainEntry;
template <typename T> class GrowableArray;

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// The data structure for the class loader data dictionaries.

class Dictionary : public Hashtable<InstanceKlass*, mtClass> {
  friend class VMStructs;

  static bool _some_dictionary_needs_resizing;
  bool _resizable;
  bool _needs_resizing;
  void check_if_needs_resize();

  ClassLoaderData* _loader_data;  // backpointer to owning loader
  ClassLoaderData* loader_data() const { return _loader_data; }

  DictionaryEntry* get_entry(int index, unsigned int hash, Symbol* name);

public:
  Dictionary(ClassLoaderData* loader_data, int table_size, bool resizable = false);
  Dictionary(ClassLoaderData* loader_data, int table_size, HashtableBucket<mtClass>* t, int number_of_entries, bool resizable = false);
  ~Dictionary();

  static bool does_any_dictionary_needs_resizing();
  bool resize_if_needed();

  void add_klass(unsigned int hash, Symbol* class_name, InstanceKlass* obj);

  InstanceKlass* find_class(unsigned int hash, Symbol* name);

  void classes_do(void f(InstanceKlass*));
  void classes_do(void f(InstanceKlass*, TRAPS), TRAPS);
  void all_entries_do(KlassClosure* closure);
  void classes_do(MetaspaceClosure* it);

  void clean_cached_protection_domains(GrowableArray<ProtectionDomainEntry*>* delete_list);

  // Protection domains
  InstanceKlass* find(unsigned int hash, Symbol* name, Handle protection_domain);
  void validate_protection_domain(unsigned int name_hash,
                                  InstanceKlass* klass,
                                  Handle class_loader,
                                  Handle protection_domain,
                                  TRAPS);

  void print_on(outputStream* st) const;
  void verify();

 private:
  DictionaryEntry* new_entry(unsigned int hash, InstanceKlass* klass);

  DictionaryEntry* bucket(int i) const {
    return (DictionaryEntry*)Hashtable<InstanceKlass*, mtClass>::bucket(i);
  }

  // The following method is not MT-safe and must be done under lock.
  DictionaryEntry** bucket_addr(int i) {
    return (DictionaryEntry**)Hashtable<InstanceKlass*, mtClass>::bucket_addr(i);
  }

  void free_entry(DictionaryEntry* entry);

  bool is_valid_protection_domain(unsigned int hash,
                                  Symbol* name,
                                  Handle protection_domain);
  void add_protection_domain(int index, unsigned int hash,
                             InstanceKlass* klass,
                             Handle protection_domain);
};

// An entry in the class loader data dictionaries, this describes a class as
// { InstanceKlass*, protection_domain }.

class DictionaryEntry : public HashtableEntry<InstanceKlass*, mtClass> {
  friend class VMStructs;
 private:
  // Contains the set of approved protection domains that can access
  // this dictionary entry.
  //
  // [Note that C.protection_domain(), which is stored in the java.lang.Class
  // mirror of C, is NOT the same as PD]
  //
  // If an entry for PD exists in the list, it means that
  // it is okay for a caller class to reference the class in this dictionary entry.
  //
  // The usage of the PD set can be seen in SystemDictionary::validate_protection_domain()
  // It is essentially a cache to avoid repeated Java up-calls to
  // ClassLoader.checkPackageAccess().
  //
  ProtectionDomainEntry* volatile _pd_set;

 public:
  // Tells whether a protection is in the approved set.
  bool contains_protection_domain(oop protection_domain) const;
  // Adds a protection domain to the approved set.
  void add_protection_domain(ClassLoaderData* loader_data, Handle protection_domain);

  InstanceKlass* instance_klass() const { return literal(); }
  InstanceKlass** klass_addr() { return (InstanceKlass**)literal_addr(); }

  DictionaryEntry* next() const {
    return (DictionaryEntry*)HashtableEntry<InstanceKlass*, mtClass>::next();
  }

  DictionaryEntry** next_addr() {
    return (DictionaryEntry**)HashtableEntry<InstanceKlass*, mtClass>::next_addr();
  }

  ProtectionDomainEntry* pd_set_acquire() const            { return Atomic::load_acquire(&_pd_set); }
  void release_set_pd_set(ProtectionDomainEntry* entry)    { Atomic::release_store(&_pd_set, entry); }

  // Tells whether the initiating class' protection domain can access the klass in this entry
  inline bool is_valid_protection_domain(Handle protection_domain);
  void verify_protection_domain_set();

  void print_count(outputStream *st);
  void verify();
};

// Entry in a SymbolPropertyTable, mapping a single Symbol*
// to a managed and an unmanaged pointer.
class SymbolPropertyEntry : public HashtableEntry<Symbol*, mtSymbol> {
  friend class VMStructs;
 private:
  intptr_t _symbol_mode;  // secondary key
  Method*   _method;
  OopHandle _method_type;

 public:
  Symbol* symbol() const            { return literal(); }

  intptr_t symbol_mode() const      { return _symbol_mode; }
  void set_symbol_mode(intptr_t m)  { _symbol_mode = m; }

  Method*        method() const     { return _method; }
  void set_method(Method* p)        { _method = p; }

  oop      method_type() const;
  void set_method_type(oop p);

  // We need to clear the OopHandle because these hashtable entries are not constructed properly.
  void clear_method_type() { _method_type = OopHandle(); }

  void free_entry();

  SymbolPropertyEntry* next() const {
    return (SymbolPropertyEntry*)HashtableEntry<Symbol*, mtSymbol>::next();
  }

  SymbolPropertyEntry** next_addr() {
    return (SymbolPropertyEntry**)HashtableEntry<Symbol*, mtSymbol>::next_addr();
  }

  void print_entry(outputStream* st) const;
};

// A system-internal mapping of symbols to pointers, both managed
// and unmanaged.  Used to record the auto-generation of each method
// MethodHandle.invoke(S)T, for all signatures (S)T.
class SymbolPropertyTable : public Hashtable<Symbol*, mtSymbol> {
  friend class VMStructs;
private:
  // The following method is not MT-safe and must be done under lock.
  SymbolPropertyEntry** bucket_addr(int i) {
    return (SymbolPropertyEntry**) Hashtable<Symbol*, mtSymbol>::bucket_addr(i);
  }

  void add_entry(int index, SymbolPropertyEntry* new_entry) {
    ShouldNotReachHere();
  }
  void set_entry(int index, SymbolPropertyEntry* new_entry) {
    ShouldNotReachHere();
  }

  SymbolPropertyEntry* new_entry(unsigned int hash, Symbol* symbol, intptr_t symbol_mode) {
    SymbolPropertyEntry* entry = (SymbolPropertyEntry*) Hashtable<Symbol*, mtSymbol>::new_entry(hash, symbol);
    // Hashtable with Symbol* literal must increment and decrement refcount.
    symbol->increment_refcount();
    entry->set_symbol_mode(symbol_mode);
    entry->set_method(NULL);
    entry->clear_method_type();
    return entry;
  }

public:
  SymbolPropertyTable(int table_size);
  SymbolPropertyTable(int table_size, HashtableBucket<mtSymbol>* t, int number_of_entries);

  void free_entry(SymbolPropertyEntry* entry);

  unsigned int compute_hash(Symbol* sym, intptr_t symbol_mode) {
    // Use the regular identity_hash.
    return Hashtable<Symbol*, mtSymbol>::compute_hash(sym) ^ symbol_mode;
  }

  int index_for(Symbol* name, intptr_t symbol_mode) {
    return hash_to_index(compute_hash(name, symbol_mode));
  }

  // need not be locked; no state change
  SymbolPropertyEntry* find_entry(int index, unsigned int hash, Symbol* name, intptr_t name_mode);

  // must be done under SystemDictionary_lock
  SymbolPropertyEntry* add_entry(int index, unsigned int hash, Symbol* name, intptr_t name_mode);

  void methods_do(void f(Method*));

  void verify();

  SymbolPropertyEntry* bucket(int i) {
    return (SymbolPropertyEntry*) Hashtable<Symbol*, mtSymbol>::bucket(i);
  }
};
#endif // SHARE_CLASSFILE_DICTIONARY_HPP
