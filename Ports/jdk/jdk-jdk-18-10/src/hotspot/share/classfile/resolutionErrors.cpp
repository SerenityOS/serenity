/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/resolutionErrors.hpp"
#include "memory/allocation.hpp"
#include "memory/resourceArea.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/safepoint.hpp"
#include "utilities/hashtable.inline.hpp"

// create new error entry
void ResolutionErrorTable::add_entry(int index, unsigned int hash,
                                     const constantPoolHandle& pool, int cp_index,
                                     Symbol* error, Symbol* message,
                                     Symbol* cause, Symbol* cause_msg)
{
  assert_locked_or_safepoint(SystemDictionary_lock);
  assert(!pool.is_null() && error != NULL, "adding NULL obj");

  ResolutionErrorEntry* entry = (ResolutionErrorEntry*)Hashtable<ConstantPool*, mtClass>::new_entry(hash, pool());
  entry->set_cp_index(cp_index);
  entry->set_error(error);
  entry->set_message(message);
  entry->set_nest_host_error(NULL);
  entry->set_cause(cause);
  entry->set_cause_msg(cause_msg);

  add_entry(index, entry);
}

// create new nest host error entry
void ResolutionErrorTable::add_entry(int index, unsigned int hash,
                                     const constantPoolHandle& pool, int cp_index,
                                     const char* message)
{
  assert_locked_or_safepoint(SystemDictionary_lock);
  assert(!pool.is_null() && message != NULL, "adding NULL obj");

  ResolutionErrorEntry* entry = (ResolutionErrorEntry*)Hashtable<ConstantPool*, mtClass>::new_entry(hash, pool());
  entry->set_cp_index(cp_index);
  entry->set_nest_host_error(message);
  entry->set_error(NULL);
  entry->set_message(NULL);
  entry->set_cause(NULL);
  entry->set_cause_msg(NULL);

  add_entry(index, entry);
}

// find entry in the table
ResolutionErrorEntry* ResolutionErrorTable::find_entry(int index, unsigned int hash,
                                                       const constantPoolHandle& pool, int cp_index)
{
  assert_locked_or_safepoint(SystemDictionary_lock);

  for (ResolutionErrorEntry *error_probe = bucket(index);
                         error_probe != NULL;
                         error_probe = error_probe->next()) {
  if (error_probe->hash() == hash && error_probe->pool() == pool()) {
      return error_probe;;
    }
  }
  return NULL;
}

void ResolutionErrorEntry::set_error(Symbol* e) {
  _error = e;
  if (_error != NULL) {
    _error->increment_refcount();
  }
}

void ResolutionErrorEntry::set_message(Symbol* c) {
  _message = c;
  if (_message != NULL) {
    _message->increment_refcount();
  }
}

void ResolutionErrorEntry::set_cause(Symbol* c) {
  _cause = c;
  if (_cause != NULL) {
    _cause->increment_refcount();
  }
}

void ResolutionErrorEntry::set_cause_msg(Symbol* c) {
  _cause_msg = c;
  if (_cause_msg != NULL) {
    _cause_msg->increment_refcount();
  }
}

void ResolutionErrorEntry::set_nest_host_error(const char* message) {
  _nest_host_error = message;
}

void ResolutionErrorTable::free_entry(ResolutionErrorEntry *entry) {
  // decrement error refcount
  if (entry->error() != NULL) {
    entry->error()->decrement_refcount();
  }
  if (entry->message() != NULL) {
    entry->message()->decrement_refcount();
  }
  if (entry->cause() != NULL) {
    entry->cause()->decrement_refcount();
  }
  if (entry->cause_msg() != NULL) {
    entry->cause_msg()->decrement_refcount();
  }
  if (entry->nest_host_error() != NULL) {
    FREE_C_HEAP_ARRAY(char, entry->nest_host_error());
  }
  BasicHashtable<mtClass>::free_entry(entry);
}


// create resolution error table
ResolutionErrorTable::ResolutionErrorTable(int table_size)
    : Hashtable<ConstantPool*, mtClass>(table_size, sizeof(ResolutionErrorEntry)) {
}

// RedefineClasses support - remove matching entry of a
// constant pool that is going away
void ResolutionErrorTable::delete_entry(ConstantPool* c) {
  assert_locked_or_safepoint(SystemDictionary_lock);
  for (int i = 0; i < table_size(); i++) {
    for (ResolutionErrorEntry** p = bucket_addr(i); *p != NULL; ) {
      ResolutionErrorEntry* entry = *p;
      assert(entry->pool() != NULL, "resolution error table is corrupt");
      if (entry->pool() == c) {
        *p = entry->next();
        free_entry(entry);
      } else {
        p = entry->next_addr();
      }
    }
  }
}


// Remove unloaded entries from the table
void ResolutionErrorTable::purge_resolution_errors() {
  assert_locked_or_safepoint(SystemDictionary_lock);
  for (int i = 0; i < table_size(); i++) {
    for (ResolutionErrorEntry** p = bucket_addr(i); *p != NULL; ) {
      ResolutionErrorEntry* entry = *p;
      assert(entry->pool() != (ConstantPool*)NULL, "resolution error table is corrupt");
      ConstantPool* pool = entry->pool();
      assert(pool->pool_holder() != NULL, "Constant pool without a class?");

      if (pool->pool_holder()->is_loader_alive()) {
        p = entry->next_addr();
      } else {
        *p = entry->next();
        free_entry(entry);
      }
    }
  }
}
