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

#include "precompiled.hpp"
#include "classfile/dictionary.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/moduleEntry.hpp"
#include "classfile/packageEntry.hpp"
#include "classfile/placeholders.hpp"
#include "classfile/protectionDomainCache.hpp"
#include "classfile/vmClasses.hpp"
#include "code/nmethod.hpp"
#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/oop.inline.hpp"
#include "oops/symbol.hpp"
#include "oops/weakHandle.inline.hpp"
#include "prims/jvmtiTagMapTable.hpp"
#include "runtime/safepoint.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/hashtable.hpp"
#include "utilities/hashtable.inline.hpp"
#include "utilities/numberSeq.hpp"

// This hashtable is implemented as an open hash table with a fixed number of buckets.

// Hashtable entry allocates in the C heap directly.

template <MEMFLAGS F> BasicHashtableEntry<F>* BasicHashtable<F>::new_entry(unsigned int hashValue) {
  BasicHashtableEntry<F>* entry = ::new (NEW_C_HEAP_ARRAY(char, this->entry_size(), F))
                                        BasicHashtableEntry<F>(hashValue);
  return entry;
}


template <class T, MEMFLAGS F> HashtableEntry<T, F>* Hashtable<T, F>::new_entry(unsigned int hashValue, T obj) {
  HashtableEntry<T, F>* entry = ::new (NEW_C_HEAP_ARRAY(char, this->entry_size(), F))
                                      HashtableEntry<T, F>(hashValue, obj);
  return entry;
}

template <MEMFLAGS F> inline void BasicHashtable<F>::free_entry(BasicHashtableEntry<F>* entry) {
  // Unlink from the Hashtable prior to freeing
  unlink_entry(entry);
  FREE_C_HEAP_ARRAY(char, entry);
  JFR_ONLY(_stats_rate.remove();)
}


template <MEMFLAGS F> void BasicHashtable<F>::free_buckets() {
  FREE_C_HEAP_ARRAY(HashtableBucket, _buckets);
  _buckets = NULL;
}

// Default overload, for types that are uninteresting.
template<typename T> static int literal_size(T) { return 0; }

static int literal_size(Symbol *symbol) {
  return symbol->size() * HeapWordSize;
}

static int literal_size(oop obj) {
  if (obj == NULL) {
    return 0;
  } else if (obj->klass() == vmClasses::String_klass()) {
    // This may overcount if String.value arrays are shared.
    return (obj->size() + java_lang_String::value(obj)->size()) * HeapWordSize;
  } else {
    return obj->size();
  }
}

static int literal_size(WeakHandle v) {
  return literal_size(v.peek());
}

const double _resize_factor    = 2.0;     // by how much we will resize using current number of entries
const int _small_table_sizes[] = { 107, 1009, 2017, 4049, 5051, 10103, 20201, 40423 } ;
const int _small_array_size = sizeof(_small_table_sizes)/sizeof(int);

// possible hashmap sizes - odd primes that roughly double in size.
// To avoid excessive resizing the odd primes from 4801-76831 and
// 76831-307261 have been removed.
const int _large_table_sizes[] =  { 4801, 76831, 307261, 614563, 1228891,
    2457733, 4915219, 9830479, 19660831, 39321619, 78643219 };
const int _large_array_size = sizeof(_large_table_sizes)/sizeof(int);

// Calculate next "good" hashtable size based on requested count
template <MEMFLAGS F> int BasicHashtable<F>::calculate_resize(bool use_large_table_sizes) const {
  int requested = (int)(_resize_factor*number_of_entries());
  const int* primelist = use_large_table_sizes ? _large_table_sizes : _small_table_sizes;
  int arraysize =  use_large_table_sizes ? _large_array_size  : _small_array_size;
  int newsize;
  for (int i = 0; i < arraysize; i++) {
    newsize = primelist[i];
    if (newsize >= requested)
      break;
  }
  return newsize;
}

template <MEMFLAGS F> bool BasicHashtable<F>::resize(int new_size) {

  // Allocate new buckets
  HashtableBucket<F>* buckets_new = NEW_C_HEAP_ARRAY2_RETURN_NULL(HashtableBucket<F>, new_size, F, CURRENT_PC);
  if (buckets_new == NULL) {
    return false;
  }

  // Clear the new buckets
  for (int i = 0; i < new_size; i++) {
    buckets_new[i].clear();
  }

  int table_size_old = _table_size;
  // hash_to_index() uses _table_size, so switch the sizes now
  _table_size = new_size;

  // Move entries from the old table to a new table
  for (int index_old = 0; index_old < table_size_old; index_old++) {
    for (BasicHashtableEntry<F>* p = _buckets[index_old].get_entry(); p != NULL; ) {
      BasicHashtableEntry<F>* next = p->next();
      int index_new = hash_to_index(p->hash());

      p->set_next(buckets_new[index_new].get_entry());
      buckets_new[index_new].set_entry(p);
      p = next;
    }
  }

  // The old backets now can be released
  BasicHashtable<F>::free_buckets();

  // Switch to the new storage
  _buckets = buckets_new;

  return true;
}

template <MEMFLAGS F> bool BasicHashtable<F>::maybe_grow(int max_size, int load_factor) {
  assert(SafepointSynchronize::is_at_safepoint(), "must be at safepoint");

  if (table_size() >= max_size) {
    return false;
  }
  if (number_of_entries() / table_size() > load_factor) {
    resize(MIN2<int>(table_size() * 2, max_size));
    return true;
  } else {
    return false;
  }
}

template <class T, MEMFLAGS F> TableStatistics Hashtable<T, F>::statistics_calculate(T (*literal_load_barrier)(HashtableEntry<T, F>*)) {
  NumberSeq summary;
  int literal_bytes = 0;
  for (int i = 0; i < this->table_size(); ++i) {
    int count = 0;
    for (HashtableEntry<T, F>* e = this->bucket(i);
         e != NULL; e = e->next()) {
      count++;
      T l = (literal_load_barrier != NULL) ? literal_load_barrier(e) : e->literal();
      literal_bytes += literal_size(l);
    }
    summary.add((double)count);
  }
  return TableStatistics(this->_stats_rate, summary, literal_bytes, sizeof(HashtableBucket<F>), sizeof(HashtableEntry<T, F>));
}

// Dump footprint and bucket length statistics
template <class T, MEMFLAGS F> void Hashtable<T, F>::print_table_statistics(outputStream* st,
                                                                            const char *table_name,
                                                                            T (*literal_load_barrier)(HashtableEntry<T, F>*)) {
  TableStatistics ts = statistics_calculate(literal_load_barrier);
  ts.print(st, table_name);
}

#ifndef PRODUCT
template <class T> static void print_literal(T const& l) { l.print(); }
template <class T> static void print_literal(T* l) { print_literal(*l); }

template <class T, MEMFLAGS F> void Hashtable<T, F>::print() {
  ResourceMark rm;

  for (int i = 0; i < BasicHashtable<F>::table_size(); i++) {
    HashtableEntry<T, F>* entry = bucket(i);
    while(entry != NULL) {
      tty->print("%d : ", i);
      print_literal(entry->literal());
      tty->cr();
      entry = entry->next();
    }
  }
}

template <MEMFLAGS F>
template <class T> void BasicHashtable<F>::verify_table(const char* table_name) {
  int element_count = 0;
  int max_bucket_count = 0;
  int max_bucket_number = 0;
  for (int index = 0; index < table_size(); index++) {
    int bucket_count = 0;
    for (T* probe = (T*)bucket(index); probe != NULL; probe = probe->next()) {
      probe->verify();
      bucket_count++;
    }
    element_count += bucket_count;
    if (bucket_count > max_bucket_count) {
      max_bucket_count = bucket_count;
      max_bucket_number = index;
    }
  }
  guarantee(number_of_entries() == element_count,
            "Verify of %s failed", table_name);

  // Log some statistics about the hashtable
  log_info(hashtables)("%s max bucket size %d bucket %d element count %d table size %d", table_name,
                       max_bucket_count, max_bucket_number, _number_of_entries, _table_size);
  if (_number_of_entries > 0 && log_is_enabled(Debug, hashtables)) {
    for (int index = 0; index < table_size(); index++) {
      int bucket_count = 0;
      for (T* probe = (T*)bucket(index); probe != NULL; probe = probe->next()) {
        log_debug(hashtables)("bucket %d hash " INTPTR_FORMAT, index, (intptr_t)probe->hash());
        bucket_count++;
      }
      if (bucket_count > 0) {
        log_debug(hashtables)("bucket %d count %d", index, bucket_count);
      }
    }
  }
}
#endif // PRODUCT

// Explicitly instantiate these types
template class Hashtable<nmethod*, mtGC>;
template class HashtableEntry<nmethod*, mtGC>;
template class BasicHashtable<mtGC>;
template class Hashtable<ConstantPool*, mtClass>;
template class Hashtable<Symbol*, mtSymbol>;
template class Hashtable<Klass*, mtClass>;
template class Hashtable<InstanceKlass*, mtClass>;
template class Hashtable<WeakHandle, mtClass>;
template class Hashtable<WeakHandle, mtServiceability>;
template class Hashtable<Symbol*, mtModule>;
template class Hashtable<Symbol*, mtClass>;
template class HashtableEntry<Symbol*, mtSymbol>;
template class HashtableEntry<Symbol*, mtClass>;
template class HashtableBucket<mtClass>;
template class BasicHashtableEntry<mtSymbol>;
template class BasicHashtableEntry<mtCode>;
template class BasicHashtable<mtClass>;
template class BasicHashtable<mtClassShared>;
template class BasicHashtable<mtSymbol>;
template class BasicHashtable<mtCode>;
template class BasicHashtable<mtInternal>;
template class BasicHashtable<mtModule>;
template class BasicHashtable<mtCompiler>;
template class BasicHashtable<mtTracing>;
template class BasicHashtable<mtServiceability>;
template class BasicHashtable<mtLogging>;

template void BasicHashtable<mtClass>::verify_table<DictionaryEntry>(char const*);
template void BasicHashtable<mtModule>::verify_table<ModuleEntry>(char const*);
template void BasicHashtable<mtModule>::verify_table<PackageEntry>(char const*);
template void BasicHashtable<mtClass>::verify_table<ProtectionDomainCacheEntry>(char const*);
template void BasicHashtable<mtClass>::verify_table<PlaceholderEntry>(char const*);
