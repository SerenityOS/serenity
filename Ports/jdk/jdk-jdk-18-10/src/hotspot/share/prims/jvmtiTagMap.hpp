/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

// JvmtiTagMap

#ifndef SHARE_PRIMS_JVMTITAGMAP_HPP
#define SHARE_PRIMS_JVMTITAGMAP_HPP

#include "jvmtifiles/jvmti.h"
#include "memory/allocation.hpp"

class JvmtiEnv;
class JvmtiTagMapTable;
class JvmtiTagMapEntryClosure;

class JvmtiTagMap :  public CHeapObj<mtInternal> {
 private:

  JvmtiEnv*             _env;                       // the jvmti environment
  Mutex                 _lock;                      // lock for this tag map
  JvmtiTagMapTable*     _hashmap;                   // the hashmap for tags
  bool                  _needs_rehashing;
  bool                  _needs_cleaning;

  static bool           _has_object_free_events;

  // create a tag map
  JvmtiTagMap(JvmtiEnv* env);

  // accessors
  inline JvmtiEnv* env() const              { return _env; }

  void check_hashmap(bool post_events);

  void entry_iterate(JvmtiTagMapEntryClosure* closure);
  void post_dead_objects_on_vm_thread();

 public:
  // indicates if this tag map is locked
  bool is_locked()                          { return lock()->is_locked(); }
  inline Mutex* lock()                      { return &_lock; }

  JvmtiTagMapTable* hashmap() { return _hashmap; }

  // returns true if the hashmaps are empty
  bool is_empty();

  // return tag for the given environment
  static JvmtiTagMap* tag_map_for(JvmtiEnv* env);

  // destroy tag map
  ~JvmtiTagMap();

  // set/get tag
  void set_tag(jobject obj, jlong tag);
  jlong get_tag(jobject obj);

  // deprecated heap iteration functions
  void iterate_over_heap(jvmtiHeapObjectFilter object_filter,
                         Klass* klass,
                         jvmtiHeapObjectCallback heap_object_callback,
                         const void* user_data);

  void iterate_over_reachable_objects(jvmtiHeapRootCallback heap_root_callback,
                                      jvmtiStackReferenceCallback stack_ref_callback,
                                      jvmtiObjectReferenceCallback object_ref_callback,
                                      const void* user_data);

  void iterate_over_objects_reachable_from_object(jobject object,
                                                  jvmtiObjectReferenceCallback object_reference_callback,
                                                  const void* user_data);


  // advanced (JVMTI 1.1) heap iteration functions
  void iterate_through_heap(jint heap_filter,
                            Klass* klass,
                            const jvmtiHeapCallbacks* callbacks,
                            const void* user_data);

  void follow_references(jint heap_filter,
                         Klass* klass,
                         jobject initial_object,
                         const jvmtiHeapCallbacks* callbacks,
                         const void* user_data);

  // get tagged objects
  jvmtiError get_objects_with_tags(const jlong* tags, jint count,
                                   jint* count_ptr, jobject** object_result_ptr,
                                   jlong** tag_result_ptr);


  void remove_dead_entries(bool post_object_free);
  void remove_dead_entries_locked(bool post_object_free);

  static void check_hashmaps_for_heapwalk();
  static void set_needs_rehashing() NOT_JVMTI_RETURN;
  static void set_needs_cleaning() NOT_JVMTI_RETURN;
  static void gc_notification(size_t num_dead_entries) NOT_JVMTI_RETURN;

  void flush_object_free_events();
  void clear();  // Clear tagmap table after the env is disposed.

  // For ServiceThread
  static void flush_all_object_free_events() NOT_JVMTI_RETURN;
  static bool has_object_free_events_and_reset() NOT_JVMTI_RETURN_(false);
};

#endif // SHARE_PRIMS_JVMTITAGMAP_HPP
