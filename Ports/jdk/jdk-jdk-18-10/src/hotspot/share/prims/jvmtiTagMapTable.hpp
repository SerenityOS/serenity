/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_PRIMS_TAGMAPTABLE_HPP
#define SHARE_VM_PRIMS_TAGMAPTABLE_HPP

#include "oops/weakHandle.hpp"
#include "utilities/hashtable.hpp"

class JvmtiEnv;

// Hashtable to record oops used for JvmtiTagMap
class JvmtiTagMapEntryClosure;

class JvmtiTagMapEntry : public HashtableEntry<WeakHandle, mtServiceability> {
  jlong _tag;                           // the tag
 public:
  JvmtiTagMapEntry* next() const {
    return (JvmtiTagMapEntry*)HashtableEntry<WeakHandle, mtServiceability>::next();
  }

  JvmtiTagMapEntry** next_addr() {
    return (JvmtiTagMapEntry**)HashtableEntry<WeakHandle, mtServiceability>::next_addr();
  }

  oop object();
  oop object_no_keepalive();
  jlong tag() const       { return _tag; }
  void set_tag(jlong tag) { _tag = tag; }
};

class JvmtiTagMapTable : public Hashtable<WeakHandle, mtServiceability> {
  enum Constants {
    _table_size  = 1007
  };

private:
  JvmtiTagMapEntry* bucket(int i) {
    return (JvmtiTagMapEntry*) Hashtable<WeakHandle, mtServiceability>::bucket(i);
  }

  JvmtiTagMapEntry** bucket_addr(int i) {
    return (JvmtiTagMapEntry**) Hashtable<WeakHandle, mtServiceability>::bucket_addr(i);
  }

  JvmtiTagMapEntry* new_entry(unsigned int hash, WeakHandle w, jlong tag);
  void free_entry(JvmtiTagMapEntry* entry);

  unsigned int compute_hash(oop obj);

  JvmtiTagMapEntry* find(int index, unsigned int hash, oop obj);

  void resize_if_needed();

public:
  JvmtiTagMapTable();
  ~JvmtiTagMapTable();

  JvmtiTagMapEntry* find(oop obj);
  JvmtiTagMapEntry* add(oop obj, jlong tag);

  void remove(oop obj);

  // iterate over all entries in the hashmap
  void entry_iterate(JvmtiTagMapEntryClosure* closure);

  bool is_empty() const { return number_of_entries() == 0; }

  // Cleanup cleared entries and post
  void remove_dead_entries(JvmtiEnv* env, bool post_object_free);
  void rehash();
  void clear();
};

// A supporting class for iterating over all entries in Hashmap
class JvmtiTagMapEntryClosure {
 public:
  virtual void do_entry(JvmtiTagMapEntry* entry) = 0;
};

#endif // SHARE_VM_PRIMS_TAGMAPTABLE_HPP
