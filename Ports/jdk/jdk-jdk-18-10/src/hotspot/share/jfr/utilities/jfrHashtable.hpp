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

#ifndef SHARE_JFR_UTILITIES_JFRHASHTABLE_HPP
#define SHARE_JFR_UTILITIES_JFRHASHTABLE_HPP

#include "jfr/utilities/jfrAllocation.hpp"
#include "runtime/atomic.hpp"
#include "services/memTracker.hpp"
#include "utilities/debug.hpp"
#include "utilities/macros.hpp"

template <typename T>
class JfrBasicHashtableEntry : public JfrCHeapObj {
 private:
  typedef JfrBasicHashtableEntry<T> Entry;
  Entry* _next;
  T _literal;          // ref to item in table.
  uintptr_t _hash;

 public:
  JfrBasicHashtableEntry(uintptr_t hash, const T& data) : _next(NULL), _literal(data), _hash(hash) {}
  uintptr_t hash() const { return _hash; }
  T literal() const { return _literal; }
  T* literal_addr() { return &_literal; }
  void set_literal(T s) { _literal = s; }
  void set_next(Entry* next) { _next = next; }
  Entry* next() const { return _next; }
  Entry** next_addr() { return &_next; }
};

template <typename T>
class JfrHashtableBucket : public CHeapObj<mtTracing> {
  template <typename>
  friend class JfrBasicHashtable;
 private:
  typedef JfrBasicHashtableEntry<T> TableEntry;
  TableEntry* _entry;

  TableEntry* get_entry() const {
    return (TableEntry*)Atomic::load_acquire(&_entry);
  }
  void set_entry(TableEntry* entry) { Atomic::release_store(&_entry, entry);}
  TableEntry** entry_addr() { return &_entry; }
};

template <typename T>
class JfrBasicHashtable : public CHeapObj<mtTracing> {
 private:
  typedef JfrHashtableBucket<T> Bucket;
  typedef JfrBasicHashtableEntry<T> TableEntry;
  Bucket* _buckets;
  uintptr_t _table_size;
  const size_t _entry_size;
  size_t _number_of_entries;

 protected:
  JfrBasicHashtable(uintptr_t table_size, size_t entry_size) :
    _buckets(NULL), _table_size(table_size), _entry_size(entry_size), _number_of_entries(0) {
    _buckets = NEW_C_HEAP_ARRAY2(Bucket, table_size, mtTracing, CURRENT_PC);
    memset((void*)_buckets, 0, table_size * sizeof(Bucket));
  }

  size_t hash_to_index(uintptr_t full_hash) const {
    const uintptr_t h = full_hash % _table_size;
    assert(h < _table_size, "Illegal hash value");
    return (size_t)h;
  }
  size_t entry_size() const { return _entry_size; }
  void unlink_entry(TableEntry* entry) {
    entry->set_next(NULL);
    --_number_of_entries;
  }
  void free_buckets() {
    FREE_C_HEAP_ARRAY(Bucket, _buckets);
  }
  TableEntry* bucket(size_t i) { return _buckets[i].get_entry();}
  TableEntry** bucket_addr(size_t i) { return _buckets[i].entry_addr(); }
  uintptr_t table_size() const { return _table_size; }
  size_t number_of_entries() const { return _number_of_entries; }
  void add_entry(size_t index, TableEntry* entry) {
    assert(entry != NULL, "invariant");
    entry->set_next(bucket(index));
    _buckets[index].set_entry(entry);
    ++_number_of_entries;
  }
};

template <typename IdType, typename Entry, typename T>
class AscendingId : public JfrCHeapObj  {
 private:
  IdType _id;
 public:
  AscendingId() : _id(0) {}
  // callbacks
  void on_link(Entry* entry) {
    assert(entry != NULL, "invariant");
    assert(entry->id() == 0, "invariant");
    entry->set_id(++_id);
  }
  bool on_equals(uintptr_t hash, const Entry* entry) {
    assert(entry->hash() == hash, "invariant");
    return true;
  }
};

// IdType must be scalar
template <typename T, typename IdType>
class JfrHashtableEntry : public JfrBasicHashtableEntry<T> {
 public:
  JfrHashtableEntry(uintptr_t hash, const T& data) : JfrBasicHashtableEntry<T>(hash, data), _id(0) {}
  typedef IdType ID;
  ID id() const { return _id; }
  void set_id(ID id) const { _id = id; }
  T& value() const { return *const_cast<JfrHashtableEntry*>(this)->literal_addr();}
  const T* value_addr() const { return const_cast<JfrHashtableEntry*>(this)->literal_addr(); }
 private:
  mutable ID _id;
};

template <typename T, typename IdType, template <typename, typename> class Entry,
          typename Callback = AscendingId<IdType, Entry<T, IdType>, T> ,
          size_t TABLE_SIZE = 1009>
class HashTableHost : public JfrBasicHashtable<T> {
 public:
  typedef Entry<T, IdType> HashEntry;
  HashTableHost(size_t size = 0) : JfrBasicHashtable<T>(size == 0 ? TABLE_SIZE : size, sizeof(HashEntry)), _callback(new Callback()) {}
  HashTableHost(Callback* cb, size_t size = 0) : JfrBasicHashtable<T>(size == 0 ? TABLE_SIZE : size, sizeof(HashEntry)), _callback(cb) {}
  ~HashTableHost() {
    this->clear_entries();
    this->free_buckets();
  }

  // direct insert assumes non-existing entry
  HashEntry& put(uintptr_t hash, const T& data);

  // lookup entry, will put if not found
  HashEntry& lookup_put(uintptr_t hash, const T& data) {
    HashEntry* entry = lookup_only(hash);
    return entry == NULL ? put(hash, data) : *entry;
  }

  HashEntry* lookup_only(uintptr_t hash);

  // id retrieval
  IdType id(uintptr_t hash, const T& data) {
    assert(data != NULL, "invariant");
    const HashEntry& entry = lookup_put(hash, data);
    assert(entry.id() > 0, "invariant");
    return entry.id();
  }

  template <typename Functor>
  void iterate_value(Functor& f);

  template <typename Functor>
  void iterate_entry(Functor& f);

  size_t cardinality() const { return this->number_of_entries(); }
  bool has_entries() const { return this->cardinality() > 0; }
  void clear_entries();

  // removal and deallocation
  void free_entry(HashEntry* entry) {
    assert(entry != NULL, "invariant");
    JfrBasicHashtable<T>::unlink_entry(entry);
    _callback->on_unlink(entry);
    delete entry;
  }

 private:
  Callback* _callback;
  size_t index_for(uintptr_t hash) { return this->hash_to_index(hash); }
  HashEntry* new_entry(uintptr_t hash, const T& data);
  void add_entry(size_t index, HashEntry* new_entry) {
    assert(new_entry != NULL, "invariant");
    _callback->on_link(new_entry);
    assert(new_entry->id() > 0, "invariant");
    JfrBasicHashtable<T>::add_entry(index, new_entry);
  }
};

template <typename T, typename IdType, template <typename, typename> class Entry, typename Callback, size_t TABLE_SIZE>
Entry<T, IdType>& HashTableHost<T, IdType, Entry, Callback, TABLE_SIZE>::put(uintptr_t hash, const T& data) {
  assert(lookup_only(hash) == NULL, "use lookup_put()");
  HashEntry* const entry = new_entry(hash, data);
  add_entry(index_for(hash), entry);
  return *entry;
}

template <typename T, typename IdType, template <typename, typename> class Entry, typename Callback, size_t TABLE_SIZE>
Entry<T, IdType>* HashTableHost<T, IdType, Entry, Callback, TABLE_SIZE>::lookup_only(uintptr_t hash) {
  HashEntry* entry = (HashEntry*)this->bucket(index_for(hash));
  while (entry != NULL) {
    if (entry->hash() == hash && _callback->on_equals(hash, entry)) {
      return entry;
    }
    entry = (HashEntry*)entry->next();
  }
  return NULL;
}

template <typename T, typename IdType, template <typename, typename> class Entry, typename Callback, size_t TABLE_SIZE>
template <typename Functor>
void HashTableHost<T, IdType, Entry, Callback, TABLE_SIZE>::iterate_value(Functor& f) {
  for (size_t i = 0; i < this->table_size(); ++i) {
    const HashEntry* entry = (const HashEntry*)this->bucket(i);
    while (entry != NULL) {
      if (!f(entry->value())) {
        break;
      }
      entry = (HashEntry*)entry->next();
    }
  }
}

template <typename T, typename IdType, template <typename, typename> class Entry, typename Callback, size_t TABLE_SIZE>
template <typename Functor>
void HashTableHost<T, IdType, Entry, Callback, TABLE_SIZE>::iterate_entry(Functor& f) {
  for (size_t i = 0; i < this->table_size(); ++i) {
    const HashEntry* entry = (const HashEntry*)this->bucket(i);
    while (entry != NULL) {
      if (!f(entry)) {
        break;
      }
      entry = (const HashEntry*)entry->next();
    }
  }
}

template <typename T, typename IdType, template <typename, typename> class Entry, typename Callback, size_t TABLE_SIZE>
void HashTableHost<T, IdType, Entry, Callback, TABLE_SIZE>::clear_entries() {
  for (size_t i = 0; i < this->table_size(); ++i) {
    HashEntry** bucket = (HashEntry**)this->bucket_addr(i);
    HashEntry* entry = *bucket;
    while (entry != NULL) {
      HashEntry* entry_to_remove = entry;
      entry = (HashEntry*)entry->next();
      this->free_entry(entry_to_remove);
    }
    *bucket = NULL;
  }
  assert(this->number_of_entries() == 0, "should have removed all entries");
}

template <typename T, typename IdType, template <typename, typename> class Entry, typename Callback, size_t TABLE_SIZE>
Entry<T, IdType>* HashTableHost<T, IdType, Entry, Callback, TABLE_SIZE>::new_entry(uintptr_t hash, const T& data) {
  assert(sizeof(HashEntry) == this->entry_size(), "invariant");
  HashEntry* const entry = new HashEntry(hash, data);
  assert(entry != NULL, "invariant");
  assert(0 == entry->id(), "invariant");
  return entry;
}

#endif // SHARE_JFR_UTILITIES_JFRHASHTABLE_HPP
