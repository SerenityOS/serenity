/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/oopStorage.hpp"
#include "jvmtifiles/jvmtiEnv.hpp"
#include "logging/log.hpp"
#include "memory/allocation.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/oop.inline.hpp"
#include "oops/weakHandle.inline.hpp"
#include "prims/jvmtiEventController.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiTagMapTable.hpp"
#include "utilities/hashtable.inline.hpp"
#include "utilities/macros.hpp"

oop JvmtiTagMapEntry::object() {
  return literal().resolve();
}

oop JvmtiTagMapEntry::object_no_keepalive() {
  // Just peek at the object without keeping it alive.
  return literal().peek();
}

JvmtiTagMapTable::JvmtiTagMapTable()
  : Hashtable<WeakHandle, mtServiceability>(_table_size, sizeof(JvmtiTagMapEntry)) {}

void JvmtiTagMapTable::clear() {
  // Clear this table
  log_debug(jvmti, table)("JvmtiTagMapTable cleared");
  for (int i = 0; i < table_size(); ++i) {
    for (JvmtiTagMapEntry* m = bucket(i); m != NULL;) {
      JvmtiTagMapEntry* entry = m;
      // read next before freeing.
      m = m->next();
      free_entry(entry);
    }
    JvmtiTagMapEntry** p = bucket_addr(i);
    *p = NULL; // clear out buckets.
  }
  assert(number_of_entries() == 0, "should have removed all entries");
}

JvmtiTagMapTable::~JvmtiTagMapTable() {
  clear();
  // base class ~BasicHashtable deallocates the buckets.
}

// Entries are C_Heap allocated
JvmtiTagMapEntry* JvmtiTagMapTable::new_entry(unsigned int hash, WeakHandle w, jlong tag) {
  JvmtiTagMapEntry* entry = (JvmtiTagMapEntry*)Hashtable<WeakHandle, mtServiceability>::new_entry(hash, w);
  entry->set_tag(tag);
  return entry;
}

void JvmtiTagMapTable::free_entry(JvmtiTagMapEntry* entry) {
  entry->literal().release(JvmtiExport::weak_tag_storage()); // release to OopStorage
  BasicHashtable<mtServiceability>::free_entry(entry);
}

unsigned int JvmtiTagMapTable::compute_hash(oop obj) {
  assert(obj != NULL, "obj is null");
  return Universe::heap()->hash_oop(obj);
}

JvmtiTagMapEntry* JvmtiTagMapTable::find(int index, unsigned int hash, oop obj) {
  assert(obj != NULL, "Cannot search for a NULL object");

  for (JvmtiTagMapEntry* p = bucket(index); p != NULL; p = p->next()) {
    if (p->hash() == hash) {

      // Peek the object to check if it is the right target.
      oop target = p->object_no_keepalive();

      // The obj is in the table as a target already
      if (target == obj) {
        ResourceMark rm;
        log_trace(jvmti, table)("JvmtiTagMap entry found for %s index %d",
                                obj->print_value_string(), index);
        // The object() accessor makes sure the target object is kept alive before
        // leaking out.
        (void)p->object();
        return p;
      }
    }
  }
  return NULL;
}

JvmtiTagMapEntry* JvmtiTagMapTable::find(oop obj) {
  unsigned int hash = compute_hash(obj);
  int index = hash_to_index(hash);
  return find(index, hash, obj);
}

JvmtiTagMapEntry* JvmtiTagMapTable::add(oop obj, jlong tag) {
  unsigned int hash = compute_hash(obj);
  int index = hash_to_index(hash);
  // One was added while acquiring the lock
  assert(find(index, hash, obj) == NULL, "shouldn't already be present");

  // obj was read with AS_NO_KEEPALIVE, or equivalent.
  // The object needs to be kept alive when it is published.
  Universe::heap()->keep_alive(obj);

  WeakHandle w(JvmtiExport::weak_tag_storage(), obj);
  JvmtiTagMapEntry* p = new_entry(hash, w, tag);
  Hashtable<WeakHandle, mtServiceability>::add_entry(index, p);
  ResourceMark rm;
  log_trace(jvmti, table)("JvmtiTagMap entry added for %s index %d",
                          obj->print_value_string(), index);

  // Resize if the table is getting too big.
  resize_if_needed();

  return p;
}

void JvmtiTagMapTable::remove(oop obj) {
  unsigned int hash = compute_hash(obj);
  int index = hash_to_index(hash);
  JvmtiTagMapEntry** p = bucket_addr(index);
  JvmtiTagMapEntry* entry = bucket(index);
  while (entry != NULL) {
    oop target = entry->object_no_keepalive();
    if (target != NULL && target == obj) {
      log_trace(jvmti, table)("JvmtiTagMap entry removed for index %d", index);
      *p = entry->next();
      free_entry(entry);
      return; // done
    }
    // get next entry and address
    p = entry->next_addr();
    entry = entry->next();
  }
}

void JvmtiTagMapTable::entry_iterate(JvmtiTagMapEntryClosure* closure) {
  for (int i = 0; i < table_size(); ++i) {
    for (JvmtiTagMapEntry* p = bucket(i); p != NULL; p = p->next()) {
      closure->do_entry(p);
    }
  }
}

const int _resize_load_trigger = 5;       // load factor that will trigger the resize
static bool _resizable = true;

void JvmtiTagMapTable::resize_if_needed() {
  if (_resizable && number_of_entries() > (_resize_load_trigger*table_size())) {
    int desired_size = calculate_resize(true);
    if (desired_size == table_size()) {
      _resizable = false; // hit max
    } else {
      if (!resize(desired_size)) {
        // Something went wrong, turn resizing off
        _resizable = false;
      }
      log_info(jvmti, table) ("JvmtiTagMap table resized to %d", table_size());
    }
  }
}

// Serially remove entries for dead oops from the table, and notify jvmti.
void JvmtiTagMapTable::remove_dead_entries(JvmtiEnv* env, bool post_object_free) {
  int oops_removed = 0;
  int oops_counted = 0;
  for (int i = 0; i < table_size(); ++i) {
    JvmtiTagMapEntry** p = bucket_addr(i);
    JvmtiTagMapEntry* entry = bucket(i);
    while (entry != NULL) {
      oops_counted++;
      oop l = entry->object_no_keepalive();
      if (l != NULL) {
        p = entry->next_addr();
      } else {
        // Entry has been removed.
        oops_removed++;
        log_trace(jvmti, table)("JvmtiTagMap entry removed for index %d", i);
        jlong tag = entry->tag();
        *p = entry->next();
        free_entry(entry);

        // post the event to the profiler
        if (post_object_free) {
          JvmtiExport::post_object_free(env, tag);
        }

      }
      // get next entry
      entry = *p;
    }
  }

  log_info(jvmti, table) ("JvmtiTagMap entries counted %d removed %d; %s",
                          oops_counted, oops_removed, post_object_free ? "free object posted" : "no posting");
}

// Rehash oops in the table
void JvmtiTagMapTable::rehash() {
  ResourceMark rm;
  GrowableArray<JvmtiTagMapEntry*> moved_entries;

  int oops_counted = 0;
  for (int i = 0; i < table_size(); ++i) {
    JvmtiTagMapEntry** p = bucket_addr(i);
    JvmtiTagMapEntry* entry = bucket(i);
    while (entry != NULL) {
      oops_counted++;
      oop l = entry->object_no_keepalive();
      if (l != NULL) {
        // Check if oop has moved, ie its hashcode is different
        // than the one entered in the table.
        unsigned int new_hash = compute_hash(l);
        if (entry->hash() != new_hash) {
          *p = entry->next();
          entry->set_hash(new_hash);
          unlink_entry(entry);
          moved_entries.push(entry);
        } else {
          p = entry->next_addr();
        }
      } else {
        // Skip removed oops. They may still have to be posted.
        p = entry->next_addr();
      }
      // get next entry
      entry = *p;
    }
  }

  int rehash_len = moved_entries.length();
  // Now add back in the entries that were removed.
  for (int i = 0; i < rehash_len; i++) {
    JvmtiTagMapEntry* moved_entry = moved_entries.at(i);
    int index = hash_to_index(moved_entry->hash());
    Hashtable<WeakHandle, mtServiceability>::add_entry(index, moved_entry);
  }

  log_info(jvmti, table) ("JvmtiTagMap entries counted %d rehashed %d",
                          oops_counted, rehash_len);
}
