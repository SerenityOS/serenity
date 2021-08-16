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
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/placeholders.hpp"
#include "logging/log.hpp"
#include "logging/logTag.hpp"
#include "logging/logStream.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/thread.hpp"
#include "utilities/hashtable.inline.hpp"

// SeenThread objects represent list of threads that are
// currently performing a load action on a class.
// For class circularity, set before loading a superclass.
// For bootclasssearchpath, set before calling load_instance_class.
// Defining must be single threaded on a class/classloader basis
// For DEFINE_CLASS, the head of the queue owns the
// define token and the rest of the threads wait to return the
// result the first thread gets.
class SeenThread: public CHeapObj<mtInternal> {
private:
   JavaThread* _thread;
   SeenThread* _stnext;
   SeenThread* _stprev;
public:
   SeenThread(JavaThread* thread) {
       _thread = thread;
       _stnext = NULL;
       _stprev = NULL;
   }
   JavaThread* thread()          const { return _thread;}
   void set_thread(JavaThread* thread) { _thread = thread; }

   SeenThread* next()        const { return _stnext;}
   void set_next(SeenThread* seen) { _stnext = seen; }
   void set_prev(SeenThread* seen) { _stprev = seen; }

  void print_action_queue(outputStream* st) {
    SeenThread* seen = this;
    while (seen != NULL) {
      seen->thread()->print_value_on(st);
      st->print(", ");
      seen = seen->next();
    }
  }
};

SeenThread* PlaceholderEntry::actionToQueue(PlaceholderTable::classloadAction action) {
  SeenThread* queuehead = NULL;
  switch (action) {
    case PlaceholderTable::LOAD_INSTANCE:
       queuehead = _loadInstanceThreadQ;
       break;
    case PlaceholderTable::LOAD_SUPER:
       queuehead = _superThreadQ;
       break;
    case PlaceholderTable::DEFINE_CLASS:
       queuehead = _defineThreadQ;
       break;
    default: Unimplemented();
  }
  return queuehead;
}

void PlaceholderEntry::set_threadQ(SeenThread* seenthread, PlaceholderTable::classloadAction action) {
  switch (action) {
    case PlaceholderTable::LOAD_INSTANCE:
       _loadInstanceThreadQ = seenthread;
       break;
    case PlaceholderTable::LOAD_SUPER:
       _superThreadQ = seenthread;
       break;
    case PlaceholderTable::DEFINE_CLASS:
       _defineThreadQ = seenthread;
       break;
    default: Unimplemented();
  }
  return;
}

// Doubly-linked list of Threads per action for class/classloader pair
// Class circularity support: links in thread before loading superclass
// bootstrap loader support:  links in a thread before load_instance_class
// definers: use as queue of define requestors, including owner of
// define token. Appends for debugging of requestor order
void PlaceholderEntry::add_seen_thread(JavaThread* thread, PlaceholderTable::classloadAction action) {
  assert_lock_strong(SystemDictionary_lock);
  SeenThread* threadEntry = new SeenThread(thread);
  SeenThread* seen = actionToQueue(action);

  assert(action != PlaceholderTable::LOAD_INSTANCE || seen == NULL,
         "Only one LOAD_INSTANCE allowed at a time");

  if (seen == NULL) {
    set_threadQ(threadEntry, action);
    return;
  }
  SeenThread* next;
  while ((next = seen->next()) != NULL) {
    seen = next;
  }
  seen->set_next(threadEntry);
  threadEntry->set_prev(seen);
  return;
}

bool PlaceholderEntry::check_seen_thread(JavaThread* thread, PlaceholderTable::classloadAction action) {
  assert_lock_strong(SystemDictionary_lock);
  SeenThread* threadQ = actionToQueue(action);
  SeenThread* seen = threadQ;
  while (seen) {
    if (thread == seen->thread()) {
      return true;
    }
    seen = seen->next();
  }
  return false;
}

// returns true if seenthreadQ is now empty
// Note, caller must ensure probe still exists while holding
// SystemDictionary_lock
// ignores if cleanup has already been done
// if found, deletes SeenThread
bool PlaceholderEntry::remove_seen_thread(JavaThread* thread, PlaceholderTable::classloadAction action) {
  assert_lock_strong(SystemDictionary_lock);
  SeenThread* threadQ = actionToQueue(action);
  SeenThread* seen = threadQ;
  SeenThread* prev = NULL;
  while (seen) {
    if (thread == seen->thread()) {
      if (prev) {
        prev->set_next(seen->next());
      } else {
        set_threadQ(seen->next(), action);
      }
      if (seen->next()) {
        seen->next()->set_prev(prev);
      }
      delete seen;
      break;
    }
    prev = seen;
    seen = seen->next();
  }
  return (actionToQueue(action) == NULL);
}


// Placeholder methods

PlaceholderEntry* PlaceholderTable::new_entry(int hash, Symbol* name,
                                              ClassLoaderData* loader_data,
                                              Symbol* supername) {
  PlaceholderEntry* entry = (PlaceholderEntry*)Hashtable<Symbol*, mtClass>::new_entry(hash, name);
  // Hashtable with Symbol* literal must increment and decrement refcount.
  name->increment_refcount();
  entry->set_loader_data(loader_data);
  entry->set_supername(supername);
  entry->set_superThreadQ(NULL);
  entry->set_loadInstanceThreadQ(NULL);
  entry->set_defineThreadQ(NULL);
  entry->set_definer(NULL);
  entry->set_instance_klass(NULL);
  return entry;
}

void PlaceholderTable::free_entry(PlaceholderEntry* entry) {
  // decrement Symbol refcount here because Hashtable doesn't.
  entry->literal()->decrement_refcount();
  if (entry->supername() != NULL) entry->supername()->decrement_refcount();
  BasicHashtable<mtClass>::free_entry(entry);
}


// Placeholder objects represent classes currently being loaded.
// All threads examining the placeholder table must hold the
// SystemDictionary_lock, so we don't need special precautions
// on store ordering here.
PlaceholderEntry* PlaceholderTable::add_entry(unsigned int hash,
                                              Symbol* class_name, ClassLoaderData* loader_data,
                                              Symbol* supername){
  assert_locked_or_safepoint(SystemDictionary_lock);
  assert(class_name != NULL, "adding NULL obj");

  // Both readers and writers are locked so it's safe to just
  // create the placeholder and insert it in the list without a membar.
  PlaceholderEntry* entry = new_entry(hash, class_name, loader_data, supername);
  int index = hash_to_index(hash);
  Hashtable<Symbol*, mtClass>::add_entry(index, entry);
  return entry;
}


// Remove a placeholder object.
void PlaceholderTable::remove_entry(unsigned int hash,
                                    Symbol* class_name,
                                    ClassLoaderData* loader_data) {
  assert_locked_or_safepoint(SystemDictionary_lock);
  int index = hash_to_index(hash);
  PlaceholderEntry** p = bucket_addr(index);
  while (*p != NULL) {
    PlaceholderEntry *probe = *p;
    if (probe->hash() == hash && probe->equals(class_name, loader_data)) {
      // Delete entry
      *p = probe->next();
      free_entry(probe);
      return;
    }
    p = probe->next_addr();
  }
}

PlaceholderEntry* PlaceholderTable::get_entry(unsigned int hash,
                                              Symbol* class_name,
                                              ClassLoaderData* loader_data) {
  assert_locked_or_safepoint(SystemDictionary_lock);

  int index = hash_to_index(hash);
  for (PlaceholderEntry *place_probe = bucket(index);
                         place_probe != NULL;
                         place_probe = place_probe->next()) {
    if (place_probe->hash() == hash &&
        place_probe->equals(class_name, loader_data)) {
      return place_probe;
    }
  }
  return NULL;
}

Symbol* PlaceholderTable::find_entry(unsigned int hash,
                                     Symbol* class_name,
                                     ClassLoaderData* loader_data) {
  PlaceholderEntry* probe = get_entry(hash, class_name, loader_data);
  return (probe != NULL ? probe->klassname() : NULL);
}

static const char* action_to_string(PlaceholderTable::classloadAction action) {
  switch (action) {
  case PlaceholderTable::LOAD_INSTANCE: return "LOAD_INSTANCE";
  case PlaceholderTable::LOAD_SUPER:    return "LOAD_SUPER";
  case PlaceholderTable::DEFINE_CLASS:  return "DEFINE_CLASS";
 }
 return "";
}

inline void log(PlaceholderEntry* entry, const char* function, PlaceholderTable::classloadAction action) {
  if (log_is_enabled(Debug, class, load, placeholders)) {
    LogTarget(Debug, class, load, placeholders) lt;
    ResourceMark rm;
    LogStream ls(lt);
    ls.print("%s %s ", function, action_to_string(action));
    entry->print_entry(&ls);
  }
}

// find_and_add returns probe pointer - old or new
// If no entry exists, add a placeholder entry
// If entry exists, reuse entry
// For both, push SeenThread for classloadAction
// If LOAD_SUPER, this is used for circularity detection for instanceklass loading.
PlaceholderEntry* PlaceholderTable::find_and_add(unsigned int hash,
                                                 Symbol* name,
                                                 ClassLoaderData* loader_data,
                                                 classloadAction action,
                                                 Symbol* supername,
                                                 JavaThread* thread) {
  assert(action != LOAD_SUPER || supername != NULL, "must have a super class name");
  PlaceholderEntry* probe = get_entry(hash, name, loader_data);
  if (probe == NULL) {
    // Nothing found, add place holder
    probe = add_entry(hash, name, loader_data, supername);
  } else {
    if (action == LOAD_SUPER) {
      probe->set_supername(supername);
    }
  }
  probe->add_seen_thread(thread, action);
  log(probe, "find_and_add", action);
  return probe;
}


// placeholder is used to track class loading internal states
// placeholder existence now for loading superclass/superinterface
// superthreadQ tracks class circularity, while loading superclass/superinterface
// loadInstanceThreadQ tracks load_instance_class calls
// definer() tracks the single thread that owns define token
// defineThreadQ tracks waiters on defining thread's results
// 1st claimant creates placeholder
// find_and_add adds SeenThread entry for appropriate queue
// All claimants remove SeenThread after completing action
// On removal: if definer and all queues empty, remove entry
// Note: you can be in both placeholders and systemDictionary
// Therefore - must always check SD first
// Ignores the case where entry is not found
void PlaceholderTable::find_and_remove(unsigned int hash,
                                       Symbol* name, ClassLoaderData* loader_data,
                                       classloadAction action,
                                       JavaThread* thread) {
    assert_locked_or_safepoint(SystemDictionary_lock);
    PlaceholderEntry *probe = get_entry(hash, name, loader_data);
    if (probe != NULL) {
       log(probe, "find_and_remove", action);
       probe->remove_seen_thread(thread, action);
       // If no other threads using this entry, and this thread is not using this entry for other states
       if ((probe->superThreadQ() == NULL) && (probe->loadInstanceThreadQ() == NULL)
          && (probe->defineThreadQ() == NULL) && (probe->definer() == NULL)) {
         remove_entry(hash, name, loader_data);
       }
    }
  }

PlaceholderTable::PlaceholderTable(int table_size)
    : Hashtable<Symbol*, mtClass>(table_size, sizeof(PlaceholderEntry)) {
}

void PlaceholderEntry::verify() const {
  guarantee(loader_data() != NULL, "Must have been setup.");
  guarantee(loader_data()->class_loader() == NULL || loader_data()->class_loader()->is_instance(),
            "checking type of _loader");
  guarantee(instance_klass() == NULL
            || instance_klass()->is_instance_klass(),
            "checking type of instance_klass result");
}

void PlaceholderTable::verify() {
  verify_table<PlaceholderEntry>("Placeholder Table");
}


// Note, doesn't append a cr
// Can't call this print_on because HashtableEntry doesn't initialize its vptr
// and print_on is a virtual function so the vptr call crashes.
void PlaceholderEntry::print_entry(outputStream* st) const {
  klassname()->print_value_on(st);
  if (loader_data() != NULL) {
    st->print(", loader ");
    loader_data()->print_value_on(st);
  }
  if (supername() != NULL) {
    st->print(", supername ");
    supername()->print_value_on(st);
  }
  if (definer() != NULL) {
    st->print(", definer ");
    definer()->print_value_on(st);
  }
  if (instance_klass() != NULL) {
    st->print(", InstanceKlass ");
    instance_klass()->print_value_on(st);
  }
  st->cr();
  st->print("loadInstanceThreadQ threads:");
  loadInstanceThreadQ()->print_action_queue(st);
  st->cr();
  st->print("superThreadQ threads:");
  superThreadQ()->print_action_queue(st);
  st->cr();
  st->print("defineThreadQ threads:");
  defineThreadQ()->print_action_queue(st);
  st->cr();
}

void PlaceholderTable::print_on(outputStream* st) const {
  st->print_cr("Placeholder table (table_size=%d, placeholders=%d)",
                table_size(), number_of_entries());
  for (int pindex = 0; pindex < table_size(); pindex++) {
    for (PlaceholderEntry* probe = bucket(pindex);
                           probe != NULL;
                           probe = probe->next()) {
      st->print("%4d: placeholder ", pindex);
      probe->print_entry(st);
    }
  }
}

void PlaceholderTable::print() const { return print_on(tty); }
