/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/recorder/checkpoint/jfrCheckpointWriter.hpp"
#include "jfr/recorder/checkpoint/types/jfrThreadGroup.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/semaphore.hpp"
#include "utilities/growableArray.hpp"

static const int initial_array_size = 30;

class ThreadGroupExclusiveAccess : public StackObj {
 private:
  static Semaphore _mutex_semaphore;
 public:
  ThreadGroupExclusiveAccess() { _mutex_semaphore.wait(); }
  ~ThreadGroupExclusiveAccess() { _mutex_semaphore.signal(); }
};

Semaphore ThreadGroupExclusiveAccess::_mutex_semaphore(1);
JfrThreadGroup* JfrThreadGroup::_instance = NULL;

class JfrThreadGroupPointers : public ResourceObj {
 private:
  const Handle _thread_group_handle;
  jweak _thread_group_weak_ref;
 public:
  JfrThreadGroupPointers(Handle thread_group_handle, jweak thread_group_weak_ref);
  Handle thread_group_handle() const;
  jweak thread_group_weak_ref() const;
  oopDesc* const thread_group_oop() const;
  jweak transfer_weak_global_handle_ownership();
  void clear_weak_ref();
};

JfrThreadGroupPointers::JfrThreadGroupPointers(Handle thread_group_handle, jweak thread_group_weak_ref) :
  _thread_group_handle(thread_group_handle),
  _thread_group_weak_ref(thread_group_weak_ref) {}

Handle JfrThreadGroupPointers::thread_group_handle() const {
  return _thread_group_handle;
}

jweak JfrThreadGroupPointers::thread_group_weak_ref() const {
  return _thread_group_weak_ref;
}

oopDesc* const JfrThreadGroupPointers::thread_group_oop() const {
  assert(_thread_group_weak_ref == NULL ||
         JNIHandles::resolve_non_null(_thread_group_weak_ref) == _thread_group_handle(), "invariant");
  return _thread_group_handle();
}

jweak JfrThreadGroupPointers::transfer_weak_global_handle_ownership() {
  jweak temp = _thread_group_weak_ref;
  _thread_group_weak_ref = NULL;
  return temp;
}

void JfrThreadGroupPointers::clear_weak_ref() {
  if (NULL != _thread_group_weak_ref) {
    JNIHandles::destroy_weak_global(_thread_group_weak_ref);
  }
}

class JfrThreadGroupsHelper : public ResourceObj {
 private:
  static const int invalid_iterator_pos = -1;
  GrowableArray<JfrThreadGroupPointers*>* _thread_group_hierarchy;
  int _current_iterator_pos;

  int populate_thread_group_hierarchy(const JavaThread* jt, Thread* current);
  JfrThreadGroupPointers& at(int index);

 public:
  JfrThreadGroupsHelper(const JavaThread* jt, Thread* current);
  ~JfrThreadGroupsHelper();
  JfrThreadGroupPointers& next();
  bool is_valid() const;
  bool has_next() const;
};

JfrThreadGroupsHelper::JfrThreadGroupsHelper(const JavaThread* jt, Thread* current) {
  _thread_group_hierarchy = new GrowableArray<JfrThreadGroupPointers*>(10);
  _current_iterator_pos = populate_thread_group_hierarchy(jt, current) - 1;
}

JfrThreadGroupsHelper::~JfrThreadGroupsHelper() {
  assert(_current_iterator_pos == invalid_iterator_pos, "invariant");
  for (int i = 0; i < _thread_group_hierarchy->length(); ++i) {
    _thread_group_hierarchy->at(i)->clear_weak_ref();
  }
}

JfrThreadGroupPointers& JfrThreadGroupsHelper::at(int index) {
  assert(_thread_group_hierarchy != NULL, "invariant");
  assert(index > invalid_iterator_pos && index < _thread_group_hierarchy->length(), "invariant");
  return *(_thread_group_hierarchy->at(index));
}

bool JfrThreadGroupsHelper::has_next() const {
  return _current_iterator_pos > invalid_iterator_pos;
}

bool JfrThreadGroupsHelper::is_valid() const {
  return (_thread_group_hierarchy != NULL && _thread_group_hierarchy->length() > 0);
}

JfrThreadGroupPointers& JfrThreadGroupsHelper::next() {
  assert(is_valid(), "invariant");
  return at(_current_iterator_pos--);
}

/*
 * If not at a safepoint, we create global weak references for
 * all reachable threadgroups for this thread.
 * If we are at a safepoint, the caller is the VMThread during
 * JFR checkpointing. It can use naked oops, because nothing
 * will move before the list of threadgroups is cleared and
 * mutator threads restarted. The threadgroup list is cleared
 * later by the VMThread as one of the final steps in JFR checkpointing
 * (not here).
 */
int JfrThreadGroupsHelper::populate_thread_group_hierarchy(const JavaThread* jt, Thread* current) {
  assert(jt != NULL && jt->is_Java_thread(), "invariant");
  assert(current != NULL, "invariant");
  assert(_thread_group_hierarchy != NULL, "invariant");

  // immediate thread group
  Handle thread_group_handle(current, java_lang_Thread::threadGroup(jt->threadObj()));
  if (thread_group_handle == NULL) {
    return 0;
  }

  const bool use_weak_handles = !SafepointSynchronize::is_at_safepoint();
  jweak thread_group_weak_ref = use_weak_handles ? JNIHandles::make_weak_global(thread_group_handle) : NULL;

  JfrThreadGroupPointers* thread_group_pointers = new JfrThreadGroupPointers(thread_group_handle, thread_group_weak_ref);
  _thread_group_hierarchy->append(thread_group_pointers);
  // immediate parent thread group
  oop parent_thread_group_obj = java_lang_ThreadGroup::parent(thread_group_handle());
  Handle parent_thread_group_handle(current, parent_thread_group_obj);

  // and check parents parents...
  while (!(parent_thread_group_handle == NULL)) {
    const jweak parent_group_weak_ref = use_weak_handles ? JNIHandles::make_weak_global(parent_thread_group_handle) : NULL;
    thread_group_pointers = new JfrThreadGroupPointers(parent_thread_group_handle, parent_group_weak_ref);
    _thread_group_hierarchy->append(thread_group_pointers);
    parent_thread_group_obj = java_lang_ThreadGroup::parent(parent_thread_group_handle());
    parent_thread_group_handle = Handle(current, parent_thread_group_obj);
  }
  return _thread_group_hierarchy->length();
}

static traceid next_id() {
  static traceid _current_threadgroup_id = 0;
  return ++_current_threadgroup_id;
}

class JfrThreadGroup::JfrThreadGroupEntry : public JfrCHeapObj {
  friend class JfrThreadGroup;
 private:
  traceid _thread_group_id;
  traceid _parent_group_id;
  char* _thread_group_name; // utf8 format
  // If an entry is created during a safepoint, the
  // _thread_group_oop contains a direct oop to
  // the java.lang.ThreadGroup object.
  // If an entry is created on javathread exit time (not at safepoint),
  // _thread_group_weak_ref contains a JNI weak global handle
  // indirection to the java.lang.ThreadGroup object.
  // Note: we cannot use a union here since CHECK_UNHANDLED_OOPS makes oop have
  //       a ctor which isn't allowed in a union by the SunStudio compiler
  oop _thread_group_oop;
  jweak _thread_group_weak_ref;

  JfrThreadGroupEntry(const char* tgstr, JfrThreadGroupPointers& ptrs);
  ~JfrThreadGroupEntry();

  traceid thread_group_id() const { return _thread_group_id; }
  void set_thread_group_id(traceid tgid) { _thread_group_id = tgid; }

  const char* const thread_group_name() const { return _thread_group_name; }
  void set_thread_group_name(const char* tgname);

  traceid parent_group_id() const { return _parent_group_id; }
  void set_parent_group_id(traceid pgid) { _parent_group_id = pgid; }

  void set_thread_group(JfrThreadGroupPointers& ptrs);
  bool is_equal(const JfrThreadGroupPointers& ptrs) const;
  const oop thread_group() const;
};

JfrThreadGroup::JfrThreadGroupEntry::JfrThreadGroupEntry(const char* tgname, JfrThreadGroupPointers& ptrs) :
  _thread_group_id(0),
  _parent_group_id(0),
  _thread_group_name(NULL),
  _thread_group_oop(NULL),
  _thread_group_weak_ref(NULL) {
  set_thread_group_name(tgname);
  set_thread_group(ptrs);
}

JfrThreadGroup::JfrThreadGroupEntry::~JfrThreadGroupEntry() {
  if (_thread_group_name != NULL) {
    JfrCHeapObj::free(_thread_group_name, strlen(_thread_group_name) + 1);
  }
  if (_thread_group_weak_ref != NULL) {
    JNIHandles::destroy_weak_global(_thread_group_weak_ref);
  }
}

void JfrThreadGroup::JfrThreadGroupEntry::set_thread_group_name(const char* tgname) {
  assert(_thread_group_name == NULL, "invariant");
  if (tgname != NULL) {
    size_t len = strlen(tgname);
    _thread_group_name = JfrCHeapObj::new_array<char>(len + 1);
    strncpy(_thread_group_name, tgname, len + 1);
  }
}

const oop JfrThreadGroup::JfrThreadGroupEntry::thread_group() const {
  return _thread_group_weak_ref != NULL ? JNIHandles::resolve(_thread_group_weak_ref) : _thread_group_oop;
}

void JfrThreadGroup::JfrThreadGroupEntry::set_thread_group(JfrThreadGroupPointers& ptrs) {
  _thread_group_weak_ref = ptrs.transfer_weak_global_handle_ownership();
  if (_thread_group_weak_ref == NULL) {
    _thread_group_oop = ptrs.thread_group_oop();
    assert(_thread_group_oop != NULL, "invariant");
  } else {
    _thread_group_oop = NULL;
  }
}

JfrThreadGroup::JfrThreadGroup() :
  _list(new (ResourceObj::C_HEAP, mtTracing) GrowableArray<JfrThreadGroupEntry*>(initial_array_size, mtTracing)) {}

JfrThreadGroup::~JfrThreadGroup() {
  if (_list != NULL) {
    for (int i = 0; i < _list->length(); i++) {
      JfrThreadGroupEntry* e = _list->at(i);
      delete e;
    }
    delete _list;
  }
}

JfrThreadGroup* JfrThreadGroup::instance() {
  return _instance;
}

void JfrThreadGroup::set_instance(JfrThreadGroup* new_instance) {
  _instance = new_instance;
}

traceid JfrThreadGroup::thread_group_id(const JavaThread* jt, Thread* current) {
  JfrThreadGroupsHelper helper(jt, current);
  return helper.is_valid() ? thread_group_id_internal(helper) : 0;
}

traceid JfrThreadGroup::thread_group_id(JavaThread* const jt) {
  return thread_group_id(jt, jt);
}

traceid JfrThreadGroup::thread_group_id_internal(JfrThreadGroupsHelper& helper) {
  ThreadGroupExclusiveAccess lock;
  JfrThreadGroup* tg_instance = instance();
  if (tg_instance == NULL) {
    tg_instance = new JfrThreadGroup();
    if (tg_instance == NULL) {
      return 0;
    }
    set_instance(tg_instance);
  }

  JfrThreadGroupEntry* tge = NULL;
  int parent_thread_group_id = 0;
  while (helper.has_next()) {
    JfrThreadGroupPointers& ptrs = helper.next();
    tge = tg_instance->find_entry(ptrs);
    if (NULL == tge) {
      tge = tg_instance->new_entry(ptrs);
      assert(tge != NULL, "invariant");
      tge->set_parent_group_id(parent_thread_group_id);
    }
    parent_thread_group_id = tge->thread_group_id();
  }
  // the last entry in the hierarchy is the immediate thread group
  return tge->thread_group_id();
}

bool JfrThreadGroup::JfrThreadGroupEntry::is_equal(const JfrThreadGroupPointers& ptrs) const {
  return ptrs.thread_group_oop() == thread_group();
}

JfrThreadGroup::JfrThreadGroupEntry*
JfrThreadGroup::find_entry(const JfrThreadGroupPointers& ptrs) const {
  for (int index = 0; index < _list->length(); ++index) {
    JfrThreadGroupEntry* curtge = _list->at(index);
    if (curtge->is_equal(ptrs)) {
      return curtge;
    }
  }
  return (JfrThreadGroupEntry*) NULL;
}

// Assumes you already searched for the existence
// of a corresponding entry in find_entry().
JfrThreadGroup::JfrThreadGroupEntry*
JfrThreadGroup::new_entry(JfrThreadGroupPointers& ptrs) {
  JfrThreadGroupEntry* const tge = new JfrThreadGroupEntry(java_lang_ThreadGroup::name(ptrs.thread_group_oop()), ptrs);
  add_entry(tge);
  return tge;
}

int JfrThreadGroup::add_entry(JfrThreadGroupEntry* tge) {
  assert(tge != NULL, "attempting to add a null entry!");
  assert(0 == tge->thread_group_id(), "id must be unassigned!");
  tge->set_thread_group_id(next_id());
  return _list->append(tge);
}

void JfrThreadGroup::write_thread_group_entries(JfrCheckpointWriter& writer) const {
  assert(_list != NULL && !_list->is_empty(), "should not need be here!");
  const int number_of_tg_entries = _list->length();
  writer.write_count(number_of_tg_entries);
  for (int index = 0; index < number_of_tg_entries; ++index) {
    const JfrThreadGroupEntry* const curtge = _list->at(index);
    writer.write_key(curtge->thread_group_id());
    writer.write(curtge->parent_group_id());
    writer.write(curtge->thread_group_name());
  }
}

void JfrThreadGroup::write_selective_thread_group(JfrCheckpointWriter* writer, traceid thread_group_id) const {
  assert(writer != NULL, "invariant");
  assert(_list != NULL && !_list->is_empty(), "should not need be here!");
  const int number_of_tg_entries = _list->length();

  // save context
  const JfrCheckpointContext ctx = writer->context();
  writer->write_type(TYPE_THREADGROUP);
  const jlong count_offset = writer->reserve(sizeof(u4)); // Don't know how many yet
  int number_of_entries_written = 0;
  for (int index = number_of_tg_entries - 1; index >= 0; --index) {
    const JfrThreadGroupEntry* const curtge = _list->at(index);
    if (thread_group_id == curtge->thread_group_id()) {
      writer->write_key(curtge->thread_group_id());
      writer->write(curtge->parent_group_id());
      writer->write(curtge->thread_group_name());
      ++number_of_entries_written;
      thread_group_id = curtge->parent_group_id();
    }
  }
  if (number_of_entries_written == 0) {
    // nothing to write, restore context
    writer->set_context(ctx);
    return;
  }
  assert(number_of_entries_written > 0, "invariant");
  writer->write_count(number_of_entries_written, count_offset);
}

// Write out JfrThreadGroup instance and then delete it
void JfrThreadGroup::serialize(JfrCheckpointWriter& writer) {
  ThreadGroupExclusiveAccess lock;
  JfrThreadGroup* tg_instance = instance();
  assert(tg_instance != NULL, "invariant");
  tg_instance->write_thread_group_entries(writer);
}

// for writing a particular thread group
void JfrThreadGroup::serialize(JfrCheckpointWriter* writer, traceid thread_group_id) {
  assert(writer != NULL, "invariant");
  ThreadGroupExclusiveAccess lock;
  JfrThreadGroup* const tg_instance = instance();
  assert(tg_instance != NULL, "invariant");
  tg_instance->write_selective_thread_group(writer, thread_group_id);
}
