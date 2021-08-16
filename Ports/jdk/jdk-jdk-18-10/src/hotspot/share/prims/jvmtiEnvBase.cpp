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
#include "classfile/classLoaderDataGraph.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/moduleEntry.hpp"
#include "jvmtifiles/jvmtiEnv.hpp"
#include "memory/iterator.hpp"
#include "memory/resourceArea.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "prims/jvmtiEnvBase.hpp"
#include "prims/jvmtiEventController.inline.hpp"
#include "prims/jvmtiExtensions.hpp"
#include "prims/jvmtiImpl.hpp"
#include "prims/jvmtiManageCapabilities.hpp"
#include "prims/jvmtiTagMap.hpp"
#include "prims/jvmtiThreadState.inline.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/jfieldIDWorkaround.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/objectMonitor.inline.hpp"
#include "runtime/osThread.hpp"
#include "runtime/signature.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/vframe.inline.hpp"
#include "runtime/vframe_hp.hpp"
#include "runtime/vmThread.hpp"
#include "runtime/vmOperations.hpp"


///////////////////////////////////////////////////////////////
//
// JvmtiEnvBase
//

JvmtiEnvBase* JvmtiEnvBase::_head_environment = NULL;

bool JvmtiEnvBase::_globally_initialized = false;
volatile bool JvmtiEnvBase::_needs_clean_up = false;

jvmtiPhase JvmtiEnvBase::_phase = JVMTI_PHASE_PRIMORDIAL;

volatile int JvmtiEnvBase::_dying_thread_env_iteration_count = 0;

extern jvmtiInterface_1_ jvmti_Interface;
extern jvmtiInterface_1_ jvmtiTrace_Interface;


// perform initializations that must occur before any JVMTI environments
// are released but which should only be initialized once (no matter
// how many environments are created).
void
JvmtiEnvBase::globally_initialize() {
  assert(Threads::number_of_threads() == 0 || JvmtiThreadState_lock->is_locked(), "sanity check");
  assert(_globally_initialized == false, "bad call");

  JvmtiManageCapabilities::initialize();

  // register extension functions and events
  JvmtiExtensions::register_extensions();

#ifdef JVMTI_TRACE
  JvmtiTrace::initialize();
#endif

  _globally_initialized = true;
}


void
JvmtiEnvBase::initialize() {
  assert(Threads::number_of_threads() == 0 || JvmtiThreadState_lock->is_locked(), "sanity check");

  // Add this environment to the end of the environment list (order is important)
  {
    // This block of code must not contain any safepoints, as list deallocation
    // (which occurs at a safepoint) cannot occur simultaneously with this list
    // addition.  Note: NoSafepointVerifier cannot, currently, be used before
    // threads exist.
    JvmtiEnvIterator it;
    JvmtiEnvBase *previous_env = NULL;
    for (JvmtiEnvBase* env = it.first(); env != NULL; env = it.next(env)) {
      previous_env = env;
    }
    if (previous_env == NULL) {
      _head_environment = this;
    } else {
      previous_env->set_next_environment(this);
    }
  }

  if (_globally_initialized == false) {
    globally_initialize();
  }
}

jvmtiPhase
JvmtiEnvBase::phase() {
  // For the JVMTI environments possessed the can_generate_early_vmstart:
  //   replace JVMTI_PHASE_PRIMORDIAL with JVMTI_PHASE_START
  if (_phase == JVMTI_PHASE_PRIMORDIAL &&
      JvmtiExport::early_vmstart_recorded() &&
      early_vmstart_env()) {
    return JVMTI_PHASE_START;
  }
  return _phase; // Normal case
}

bool
JvmtiEnvBase::is_valid() {
  jint value = 0;

  // This object might not be a JvmtiEnvBase so we can't assume
  // the _magic field is properly aligned. Get the value in a safe
  // way and then check against JVMTI_MAGIC.

  switch (sizeof(_magic)) {
  case 2:
    value = Bytes::get_native_u2((address)&_magic);
    break;

  case 4:
    value = Bytes::get_native_u4((address)&_magic);
    break;

  case 8:
    value = Bytes::get_native_u8((address)&_magic);
    break;

  default:
    guarantee(false, "_magic field is an unexpected size");
  }

  return value == JVMTI_MAGIC;
}


bool
JvmtiEnvBase::use_version_1_0_semantics() {
  int major, minor, micro;

  JvmtiExport::decode_version_values(_version, &major, &minor, &micro);
  return major == 1 && minor == 0;  // micro version doesn't matter here
}


bool
JvmtiEnvBase::use_version_1_1_semantics() {
  int major, minor, micro;

  JvmtiExport::decode_version_values(_version, &major, &minor, &micro);
  return major == 1 && minor == 1;  // micro version doesn't matter here
}

bool
JvmtiEnvBase::use_version_1_2_semantics() {
  int major, minor, micro;

  JvmtiExport::decode_version_values(_version, &major, &minor, &micro);
  return major == 1 && minor == 2;  // micro version doesn't matter here
}


JvmtiEnvBase::JvmtiEnvBase(jint version) : _env_event_enable() {
  _version = version;
  _env_local_storage = NULL;
  _tag_map = NULL;
  _native_method_prefix_count = 0;
  _native_method_prefixes = NULL;
  _next = NULL;
  _class_file_load_hook_ever_enabled = false;

  // Moot since ClassFileLoadHook not yet enabled.
  // But "true" will give a more predictable ClassFileLoadHook behavior
  // for environment creation during ClassFileLoadHook.
  _is_retransformable = true;

  // all callbacks initially NULL
  memset(&_event_callbacks,0,sizeof(jvmtiEventCallbacks));

  // all capabilities initially off
  memset(&_current_capabilities, 0, sizeof(_current_capabilities));

  // all prohibited capabilities initially off
  memset(&_prohibited_capabilities, 0, sizeof(_prohibited_capabilities));

  _magic = JVMTI_MAGIC;

  JvmtiEventController::env_initialize((JvmtiEnv*)this);

#ifdef JVMTI_TRACE
  _jvmti_external.functions = TraceJVMTI != NULL ? &jvmtiTrace_Interface : &jvmti_Interface;
#else
  _jvmti_external.functions = &jvmti_Interface;
#endif
}


void
JvmtiEnvBase::dispose() {

#ifdef JVMTI_TRACE
  JvmtiTrace::shutdown();
#endif

  // Dispose of event info and let the event controller call us back
  // in a locked state (env_dispose, below)
  JvmtiEventController::env_dispose(this);
}

void
JvmtiEnvBase::env_dispose() {
  assert(Threads::number_of_threads() == 0 || JvmtiThreadState_lock->is_locked(), "sanity check");

  // We have been entered with all events disabled on this environment.
  // A race to re-enable events (by setting callbacks) is prevented by
  // checking for a valid environment when setting callbacks (while
  // holding the JvmtiThreadState_lock).

  // Mark as invalid.
  _magic = DISPOSED_MAGIC;

  // Relinquish all capabilities.
  jvmtiCapabilities *caps = get_capabilities();
  JvmtiManageCapabilities::relinquish_capabilities(caps, caps, caps);

  // Same situation as with events (see above)
  set_native_method_prefixes(0, NULL);

  JvmtiTagMap* tag_map_to_clear = tag_map_acquire();
  // A tag map can be big, clear it now to save memory until
  // the destructor runs.
  if (tag_map_to_clear != NULL) {
    tag_map_to_clear->clear();
  }

  _needs_clean_up = true;
}


JvmtiEnvBase::~JvmtiEnvBase() {
  assert(SafepointSynchronize::is_at_safepoint(), "sanity check");

  // There is a small window of time during which the tag map of a
  // disposed environment could have been reallocated.
  // Make sure it is gone.
  JvmtiTagMap* tag_map_to_deallocate = _tag_map;
  set_tag_map(NULL);
  // A tag map can be big, deallocate it now
  if (tag_map_to_deallocate != NULL) {
    delete tag_map_to_deallocate;
  }

  _magic = BAD_MAGIC;
}


void
JvmtiEnvBase::periodic_clean_up() {
  assert(SafepointSynchronize::is_at_safepoint(), "sanity check");

  // JvmtiEnvBase reference is saved in JvmtiEnvThreadState. So
  // clean up JvmtiThreadState before deleting JvmtiEnv pointer.
  JvmtiThreadState::periodic_clean_up();

  // Unlink all invalid environments from the list of environments
  // and deallocate them
  JvmtiEnvIterator it;
  JvmtiEnvBase* previous_env = NULL;
  JvmtiEnvBase* env = it.first();
  while (env != NULL) {
    if (env->is_valid()) {
      previous_env = env;
      env = it.next(env);
    } else {
      // This one isn't valid, remove it from the list and deallocate it
      JvmtiEnvBase* defunct_env = env;
      env = it.next(env);
      if (previous_env == NULL) {
        _head_environment = env;
      } else {
        previous_env->set_next_environment(env);
      }
      delete defunct_env;
    }
  }

}


void
JvmtiEnvBase::check_for_periodic_clean_up() {
  assert(SafepointSynchronize::is_at_safepoint(), "sanity check");

  class ThreadInsideIterationClosure: public ThreadClosure {
   private:
    bool _inside;
   public:
    ThreadInsideIterationClosure() : _inside(false) {};

    void do_thread(Thread* thread) {
      _inside |= thread->is_inside_jvmti_env_iteration();
    }

    bool is_inside_jvmti_env_iteration() {
      return _inside;
    }
  };

  if (_needs_clean_up) {
    // Check if we are currently iterating environment,
    // deallocation should not occur if we are
    ThreadInsideIterationClosure tiic;
    Threads::threads_do(&tiic);
    if (!tiic.is_inside_jvmti_env_iteration() &&
             !is_inside_dying_thread_env_iteration()) {
      _needs_clean_up = false;
      JvmtiEnvBase::periodic_clean_up();
    }
  }
}


void
JvmtiEnvBase::record_first_time_class_file_load_hook_enabled() {
  assert(Threads::number_of_threads() == 0 || JvmtiThreadState_lock->is_locked(),
         "sanity check");

  if (!_class_file_load_hook_ever_enabled) {
    _class_file_load_hook_ever_enabled = true;

    if (get_capabilities()->can_retransform_classes) {
      _is_retransformable = true;
    } else {
      _is_retransformable = false;

      // cannot add retransform capability after ClassFileLoadHook has been enabled
      get_prohibited_capabilities()->can_retransform_classes = 1;
    }
  }
}


void
JvmtiEnvBase::record_class_file_load_hook_enabled() {
  if (!_class_file_load_hook_ever_enabled) {
    if (Threads::number_of_threads() == 0) {
      record_first_time_class_file_load_hook_enabled();
    } else {
      MutexLocker mu(JvmtiThreadState_lock);
      record_first_time_class_file_load_hook_enabled();
    }
  }
}


jvmtiError
JvmtiEnvBase::set_native_method_prefixes(jint prefix_count, char** prefixes) {
  assert(Threads::number_of_threads() == 0 || JvmtiThreadState_lock->is_locked(),
         "sanity check");

  int old_prefix_count = get_native_method_prefix_count();
  char **old_prefixes = get_native_method_prefixes();

  // allocate and install the new prefixex
  if (prefix_count == 0 || !is_valid()) {
    _native_method_prefix_count = 0;
    _native_method_prefixes = NULL;
  } else {
    // there are prefixes, allocate an array to hold them, and fill it
    char** new_prefixes = (char**)os::malloc((prefix_count) * sizeof(char*), mtInternal);
    if (new_prefixes == NULL) {
      return JVMTI_ERROR_OUT_OF_MEMORY;
    }
    for (int i = 0; i < prefix_count; i++) {
      char* prefix = prefixes[i];
      if (prefix == NULL) {
        for (int j = 0; j < (i-1); j++) {
          os::free(new_prefixes[j]);
        }
        os::free(new_prefixes);
        return JVMTI_ERROR_NULL_POINTER;
      }
      prefix = os::strdup(prefixes[i]);
      if (prefix == NULL) {
        for (int j = 0; j < (i-1); j++) {
          os::free(new_prefixes[j]);
        }
        os::free(new_prefixes);
        return JVMTI_ERROR_OUT_OF_MEMORY;
      }
      new_prefixes[i] = prefix;
    }
    _native_method_prefix_count = prefix_count;
    _native_method_prefixes = new_prefixes;
  }

  // now that we know the new prefixes have been successfully installed we can
  // safely remove the old ones
  if (old_prefix_count != 0) {
    for (int i = 0; i < old_prefix_count; i++) {
      os::free(old_prefixes[i]);
    }
    os::free(old_prefixes);
  }

  return JVMTI_ERROR_NONE;
}


// Collect all the prefixes which have been set in any JVM TI environments
// by the SetNativeMethodPrefix(es) functions.  Be sure to maintain the
// order of environments and the order of prefixes within each environment.
// Return in a resource allocated array.
char**
JvmtiEnvBase::get_all_native_method_prefixes(int* count_ptr) {
  assert(Threads::number_of_threads() == 0 ||
         SafepointSynchronize::is_at_safepoint() ||
         JvmtiThreadState_lock->is_locked(),
         "sanity check");

  int total_count = 0;
  GrowableArray<char*>* prefix_array =new GrowableArray<char*>(5);

  JvmtiEnvIterator it;
  for (JvmtiEnvBase* env = it.first(); env != NULL; env = it.next(env)) {
    int prefix_count = env->get_native_method_prefix_count();
    char** prefixes = env->get_native_method_prefixes();
    for (int j = 0; j < prefix_count; j++) {
      // retrieve a prefix and so that it is safe against asynchronous changes
      // copy it into the resource area
      char* prefix = prefixes[j];
      char* prefix_copy = NEW_RESOURCE_ARRAY(char, strlen(prefix)+1);
      strcpy(prefix_copy, prefix);
      prefix_array->at_put_grow(total_count++, prefix_copy);
    }
  }

  char** all_prefixes = NEW_RESOURCE_ARRAY(char*, total_count);
  char** p = all_prefixes;
  for (int i = 0; i < total_count; ++i) {
    *p++ = prefix_array->at(i);
  }
  *count_ptr = total_count;
  return all_prefixes;
}

void
JvmtiEnvBase::set_event_callbacks(const jvmtiEventCallbacks* callbacks,
                                               jint size_of_callbacks) {
  assert(Threads::number_of_threads() == 0 || JvmtiThreadState_lock->is_locked(), "sanity check");

  size_t byte_cnt = sizeof(jvmtiEventCallbacks);

  // clear in either case to be sure we got any gap between sizes
  memset(&_event_callbacks, 0, byte_cnt);

  // Now that JvmtiThreadState_lock is held, prevent a possible race condition where events
  // are re-enabled by a call to set event callbacks where the DisposeEnvironment
  // occurs after the boiler-plate environment check and before the lock is acquired.
  if (callbacks != NULL && is_valid()) {
    if (size_of_callbacks < (jint)byte_cnt) {
      byte_cnt = size_of_callbacks;
    }
    memcpy(&_event_callbacks, callbacks, byte_cnt);
  }
}


// In the fullness of time, all users of the method should instead
// directly use allocate, besides being cleaner and faster, this will
// mean much better out of memory handling
unsigned char *
JvmtiEnvBase::jvmtiMalloc(jlong size) {
  unsigned char* mem = NULL;
  jvmtiError result = allocate(size, &mem);
  assert(result == JVMTI_ERROR_NONE, "Allocate failed");
  return mem;
}


// Handle management

jobject JvmtiEnvBase::jni_reference(Handle hndl) {
  return JNIHandles::make_local(hndl());
}

jobject JvmtiEnvBase::jni_reference(JavaThread *thread, Handle hndl) {
  return JNIHandles::make_local(thread, hndl());
}

void JvmtiEnvBase::destroy_jni_reference(jobject jobj) {
  JNIHandles::destroy_local(jobj);
}

void JvmtiEnvBase::destroy_jni_reference(JavaThread *thread, jobject jobj) {
  JNIHandles::destroy_local(jobj); // thread is unused.
}

//
// Threads
//

jobject *
JvmtiEnvBase::new_jobjectArray(int length, Handle *handles) {
  if (length == 0) {
    return NULL;
  }

  jobject *objArray = (jobject *) jvmtiMalloc(sizeof(jobject) * length);
  NULL_CHECK(objArray, NULL);

  for (int i=0; i<length; i++) {
    objArray[i] = jni_reference(handles[i]);
  }
  return objArray;
}

jthread *
JvmtiEnvBase::new_jthreadArray(int length, Handle *handles) {
  return (jthread *) new_jobjectArray(length,handles);
}

jthreadGroup *
JvmtiEnvBase::new_jthreadGroupArray(int length, Handle *handles) {
  return (jthreadGroup *) new_jobjectArray(length,handles);
}

// return the vframe on the specified thread and depth, NULL if no such frame
// The thread and the oops in the returned vframe might not have been process.
vframe*
JvmtiEnvBase::vframeForNoProcess(JavaThread* java_thread, jint depth) {
  if (!java_thread->has_last_Java_frame()) {
    return NULL;
  }
  RegisterMap reg_map(java_thread, true /* update_map */, false /* process_frames */);
  vframe *vf = java_thread->last_java_vframe(&reg_map);
  int d = 0;
  while ((vf != NULL) && (d < depth)) {
    vf = vf->java_sender();
    d++;
  }
  return vf;
}


//
// utilities: JNI objects
//


jclass
JvmtiEnvBase::get_jni_class_non_null(Klass* k) {
  assert(k != NULL, "k != NULL");
  Thread *thread = Thread::current();
  return (jclass)jni_reference(Handle(thread, k->java_mirror()));
}

//
// Field Information
//

bool
JvmtiEnvBase::get_field_descriptor(Klass* k, jfieldID field, fieldDescriptor* fd) {
  if (!jfieldIDWorkaround::is_valid_jfieldID(k, field)) {
    return false;
  }
  bool found = false;
  if (jfieldIDWorkaround::is_static_jfieldID(field)) {
    JNIid* id = jfieldIDWorkaround::from_static_jfieldID(field);
    found = id->find_local_field(fd);
  } else {
    // Non-static field. The fieldID is really the offset of the field within the object.
    int offset = jfieldIDWorkaround::from_instance_jfieldID(k, field);
    found = InstanceKlass::cast(k)->find_field_from_offset(offset, false, fd);
  }
  return found;
}

//
// Object Monitor Information
//

//
// Count the number of objects for a lightweight monitor. The hobj
// parameter is object that owns the monitor so this routine will
// count the number of times the same object was locked by frames
// in java_thread.
//
jint
JvmtiEnvBase::count_locked_objects(JavaThread *java_thread, Handle hobj) {
  jint ret = 0;
  if (!java_thread->has_last_Java_frame()) {
    return ret;  // no Java frames so no monitors
  }

  Thread* current_thread = Thread::current();
  ResourceMark rm(current_thread);
  HandleMark   hm(current_thread);
  RegisterMap  reg_map(java_thread);

  for(javaVFrame *jvf=java_thread->last_java_vframe(&reg_map); jvf != NULL;
                                                 jvf = jvf->java_sender()) {
    GrowableArray<MonitorInfo*>* mons = jvf->monitors();
    if (!mons->is_empty()) {
      for (int i = 0; i < mons->length(); i++) {
        MonitorInfo *mi = mons->at(i);
        if (mi->owner_is_scalar_replaced()) continue;

        // see if owner of the monitor is our object
        if (mi->owner() != NULL && mi->owner() == hobj()) {
          ret++;
        }
      }
    }
  }
  return ret;
}



jvmtiError
JvmtiEnvBase::get_current_contended_monitor(JavaThread *calling_thread, JavaThread *java_thread, jobject *monitor_ptr) {
  Thread *current_thread = Thread::current();
  assert(java_thread->is_handshake_safe_for(current_thread),
         "call by myself or at handshake");
  oop obj = NULL;
  // The ObjectMonitor* can't be async deflated since we are either
  // at a safepoint or the calling thread is operating on itself so
  // it cannot leave the underlying wait()/enter() call.
  ObjectMonitor *mon = java_thread->current_waiting_monitor();
  if (mon == NULL) {
    // thread is not doing an Object.wait() call
    mon = java_thread->current_pending_monitor();
    if (mon != NULL) {
      // The thread is trying to enter() an ObjectMonitor.
      obj = mon->object();
      assert(obj != NULL, "ObjectMonitor should have a valid object!");
    }
    // implied else: no contended ObjectMonitor
  } else {
    // thread is doing an Object.wait() call
    obj = mon->object();
    assert(obj != NULL, "Object.wait() should have an object");
  }

  if (obj == NULL) {
    *monitor_ptr = NULL;
  } else {
    HandleMark hm(current_thread);
    Handle     hobj(current_thread, obj);
    *monitor_ptr = jni_reference(calling_thread, hobj);
  }
  return JVMTI_ERROR_NONE;
}


jvmtiError
JvmtiEnvBase::get_owned_monitors(JavaThread *calling_thread, JavaThread* java_thread,
                                 GrowableArray<jvmtiMonitorStackDepthInfo*> *owned_monitors_list) {
  // Note:
  // calling_thread is the thread that requested the list of monitors for java_thread.
  // java_thread is the thread owning the monitors.
  // current_thread is the thread executing this code, can be a non-JavaThread (e.g. VM Thread).
  // And they all may be different threads.
  jvmtiError err = JVMTI_ERROR_NONE;
  Thread *current_thread = Thread::current();
  assert(java_thread->is_handshake_safe_for(current_thread),
         "call by myself or at handshake");

  if (java_thread->has_last_Java_frame()) {
    ResourceMark rm(current_thread);
    HandleMark   hm(current_thread);
    RegisterMap  reg_map(java_thread);

    int depth = 0;
    for (javaVFrame *jvf = java_thread->last_java_vframe(&reg_map); jvf != NULL;
         jvf = jvf->java_sender()) {
      if (MaxJavaStackTraceDepth == 0 || depth++ < MaxJavaStackTraceDepth) {  // check for stack too deep
        // add locked objects for this frame into list
        err = get_locked_objects_in_frame(calling_thread, java_thread, jvf, owned_monitors_list, depth-1);
        if (err != JVMTI_ERROR_NONE) {
          return err;
        }
      }
    }
  }

  // Get off stack monitors. (e.g. acquired via jni MonitorEnter).
  JvmtiMonitorClosure jmc(java_thread, calling_thread, owned_monitors_list, this);
  ObjectSynchronizer::monitors_iterate(&jmc);
  err = jmc.error();

  return err;
}

// Save JNI local handles for any objects that this frame owns.
jvmtiError
JvmtiEnvBase::get_locked_objects_in_frame(JavaThread* calling_thread, JavaThread* java_thread,
                                 javaVFrame *jvf, GrowableArray<jvmtiMonitorStackDepthInfo*>* owned_monitors_list, jint stack_depth) {
  jvmtiError err = JVMTI_ERROR_NONE;
  Thread* current_thread = Thread::current();
  ResourceMark rm(current_thread);
  HandleMark   hm(current_thread);

  GrowableArray<MonitorInfo*>* mons = jvf->monitors();
  if (mons->is_empty()) {
    return err;  // this javaVFrame holds no monitors
  }

  oop wait_obj = NULL;
  {
    // The ObjectMonitor* can't be async deflated since we are either
    // at a safepoint or the calling thread is operating on itself so
    // it cannot leave the underlying wait() call.
    // Save object of current wait() call (if any) for later comparison.
    ObjectMonitor *mon = java_thread->current_waiting_monitor();
    if (mon != NULL) {
      wait_obj = mon->object();
    }
  }
  oop pending_obj = NULL;
  {
    // The ObjectMonitor* can't be async deflated since we are either
    // at a safepoint or the calling thread is operating on itself so
    // it cannot leave the underlying enter() call.
    // Save object of current enter() call (if any) for later comparison.
    ObjectMonitor *mon = java_thread->current_pending_monitor();
    if (mon != NULL) {
      pending_obj = mon->object();
    }
  }

  for (int i = 0; i < mons->length(); i++) {
    MonitorInfo *mi = mons->at(i);

    if (mi->owner_is_scalar_replaced()) continue;

    oop obj = mi->owner();
    if (obj == NULL) {
      // this monitor doesn't have an owning object so skip it
      continue;
    }

    if (wait_obj == obj) {
      // the thread is waiting on this monitor so it isn't really owned
      continue;
    }

    if (pending_obj == obj) {
      // the thread is pending on this monitor so it isn't really owned
      continue;
    }

    if (owned_monitors_list->length() > 0) {
      // Our list has at least one object on it so we have to check
      // for recursive object locking
      bool found = false;
      for (int j = 0; j < owned_monitors_list->length(); j++) {
        jobject jobj = ((jvmtiMonitorStackDepthInfo*)owned_monitors_list->at(j))->monitor;
        oop check = JNIHandles::resolve(jobj);
        if (check == obj) {
          found = true;  // we found the object
          break;
        }
      }

      if (found) {
        // already have this object so don't include it
        continue;
      }
    }

    // add the owning object to our list
    jvmtiMonitorStackDepthInfo *jmsdi;
    err = allocate(sizeof(jvmtiMonitorStackDepthInfo), (unsigned char **)&jmsdi);
    if (err != JVMTI_ERROR_NONE) {
        return err;
    }
    Handle hobj(Thread::current(), obj);
    jmsdi->monitor = jni_reference(calling_thread, hobj);
    jmsdi->stack_depth = stack_depth;
    owned_monitors_list->append(jmsdi);
  }

  return err;
}

jvmtiError
JvmtiEnvBase::get_stack_trace(JavaThread *java_thread,
                              jint start_depth, jint max_count,
                              jvmtiFrameInfo* frame_buffer, jint* count_ptr) {
#ifdef ASSERT
  uint32_t debug_bits = 0;
#endif
  Thread *current_thread = Thread::current();
  assert(SafepointSynchronize::is_at_safepoint() ||
         java_thread->is_handshake_safe_for(current_thread),
         "call by myself / at safepoint / at handshake");
  int count = 0;
  if (java_thread->has_last_Java_frame()) {
    RegisterMap reg_map(java_thread);
    ResourceMark rm(current_thread);
    javaVFrame *jvf = java_thread->last_java_vframe(&reg_map);
    HandleMark hm(current_thread);
    if (start_depth != 0) {
      if (start_depth > 0) {
        for (int j = 0; j < start_depth && jvf != NULL; j++) {
          jvf = jvf->java_sender();
        }
        if (jvf == NULL) {
          // start_depth is deeper than the stack depth
          return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
      } else { // start_depth < 0
        // we are referencing the starting depth based on the oldest
        // part of the stack.
        // optimize to limit the number of times that java_sender() is called
        javaVFrame *jvf_cursor = jvf;
        javaVFrame *jvf_prev = NULL;
        javaVFrame *jvf_prev_prev = NULL;
        int j = 0;
        while (jvf_cursor != NULL) {
          jvf_prev_prev = jvf_prev;
          jvf_prev = jvf_cursor;
          for (j = 0; j > start_depth && jvf_cursor != NULL; j--) {
            jvf_cursor = jvf_cursor->java_sender();
          }
        }
        if (j == start_depth) {
          // previous pointer is exactly where we want to start
          jvf = jvf_prev;
        } else {
          // we need to back up further to get to the right place
          if (jvf_prev_prev == NULL) {
            // the -start_depth is greater than the stack depth
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
          }
          // j now is the number of frames on the stack starting with
          // jvf_prev, we start from jvf_prev_prev and move older on
          // the stack that many, the result is -start_depth frames
          // remaining.
          jvf = jvf_prev_prev;
          for (; j < 0; j++) {
            jvf = jvf->java_sender();
          }
        }
      }
    }
    for (; count < max_count && jvf != NULL; count++) {
      frame_buffer[count].method = jvf->method()->jmethod_id();
      frame_buffer[count].location = (jvf->method()->is_native() ? -1 : jvf->bci());
      jvf = jvf->java_sender();
    }
  } else {
    if (start_depth != 0) {
      // no frames and there is a starting depth
      return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }
  }
  *count_ptr = count;
  return JVMTI_ERROR_NONE;
}

jvmtiError
JvmtiEnvBase::get_frame_count(JvmtiThreadState *state, jint *count_ptr) {
  assert((state != NULL),
         "JavaThread should create JvmtiThreadState before calling this method");
  *count_ptr = state->count_frames();
  return JVMTI_ERROR_NONE;
}

jvmtiError
JvmtiEnvBase::get_frame_location(JavaThread *java_thread, jint depth,
                                 jmethodID* method_ptr, jlocation* location_ptr) {
#ifdef ASSERT
  uint32_t debug_bits = 0;
#endif
  Thread* current_thread = Thread::current();
  assert(java_thread->is_handshake_safe_for(current_thread),
         "call by myself or at handshake");
  ResourceMark rm(current_thread);

  vframe *vf = vframeForNoProcess(java_thread, depth);
  if (vf == NULL) {
    return JVMTI_ERROR_NO_MORE_FRAMES;
  }

  // vframeFor should return a java frame. If it doesn't
  // it means we've got an internal error and we return the
  // error in product mode. In debug mode we will instead
  // attempt to cast the vframe to a javaVFrame and will
  // cause an assertion/crash to allow further diagnosis.
#ifdef PRODUCT
  if (!vf->is_java_frame()) {
    return JVMTI_ERROR_INTERNAL;
  }
#endif

  HandleMark hm(current_thread);
  javaVFrame *jvf = javaVFrame::cast(vf);
  Method* method = jvf->method();
  if (method->is_native()) {
    *location_ptr = -1;
  } else {
    *location_ptr = jvf->bci();
  }
  *method_ptr = method->jmethod_id();

  return JVMTI_ERROR_NONE;
}


jvmtiError
JvmtiEnvBase::get_object_monitor_usage(JavaThread* calling_thread, jobject object, jvmtiMonitorUsage* info_ptr) {
  assert(SafepointSynchronize::is_at_safepoint(), "must be at safepoint");
  Thread* current_thread = VMThread::vm_thread();
  assert(current_thread == Thread::current(), "must be");

  HandleMark hm(current_thread);
  Handle hobj;

  // Check arguments
  {
    oop mirror = JNIHandles::resolve_external_guard(object);
    NULL_CHECK(mirror, JVMTI_ERROR_INVALID_OBJECT);
    NULL_CHECK(info_ptr, JVMTI_ERROR_NULL_POINTER);

    hobj = Handle(current_thread, mirror);
  }

  ThreadsListHandle tlh(current_thread);
  JavaThread *owning_thread = NULL;
  ObjectMonitor *mon = NULL;
  jvmtiMonitorUsage ret = {
      NULL, 0, 0, NULL, 0, NULL
  };

  uint32_t debug_bits = 0;
  // first derive the object's owner and entry_count (if any)
  {
    address owner = NULL;
    {
      markWord mark = hobj()->mark();

      if (!mark.has_monitor()) {
        // this object has a lightweight monitor

        if (mark.has_locker()) {
          owner = (address)mark.locker(); // save the address of the Lock word
        }
        // implied else: no owner
      } else {
        // this object has a heavyweight monitor
        mon = mark.monitor();

        // The owner field of a heavyweight monitor may be NULL for no
        // owner, a JavaThread * or it may still be the address of the
        // Lock word in a JavaThread's stack. A monitor can be inflated
        // by a non-owning JavaThread, but only the owning JavaThread
        // can change the owner field from the Lock word to the
        // JavaThread * and it may not have done that yet.
        owner = (address)mon->owner();
      }
    }

    if (owner != NULL) {
      // This monitor is owned so we have to find the owning JavaThread.
      owning_thread = Threads::owning_thread_from_monitor_owner(tlh.list(), owner);
      assert(owning_thread != NULL, "owning JavaThread must not be NULL");
      Handle     th(current_thread, owning_thread->threadObj());
      ret.owner = (jthread)jni_reference(calling_thread, th);
    }

    if (owning_thread != NULL) {  // monitor is owned
      // The recursions field of a monitor does not reflect recursions
      // as lightweight locks before inflating the monitor are not included.
      // We have to count the number of recursive monitor entries the hard way.
      // We pass a handle to survive any GCs along the way.
      ret.entry_count = count_locked_objects(owning_thread, hobj);
    }
    // implied else: entry_count == 0
  }

  jint nWant = 0, nWait = 0;
  if (mon != NULL) {
    // this object has a heavyweight monitor
    nWant = mon->contentions(); // # of threads contending for monitor
    nWait = mon->waiters();     // # of threads in Object.wait()
    ret.waiter_count = nWant + nWait;
    ret.notify_waiter_count = nWait;
  } else {
    // this object has a lightweight monitor
    ret.waiter_count = 0;
    ret.notify_waiter_count = 0;
  }

  // Allocate memory for heavyweight and lightweight monitor.
  jvmtiError err;
  err = allocate(ret.waiter_count * sizeof(jthread *), (unsigned char**)&ret.waiters);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  err = allocate(ret.notify_waiter_count * sizeof(jthread *),
                 (unsigned char**)&ret.notify_waiters);
  if (err != JVMTI_ERROR_NONE) {
    deallocate((unsigned char*)ret.waiters);
    return err;
  }

  // now derive the rest of the fields
  if (mon != NULL) {
    // this object has a heavyweight monitor

    // Number of waiters may actually be less than the waiter count.
    // So NULL out memory so that unused memory will be NULL.
    memset(ret.waiters, 0, ret.waiter_count * sizeof(jthread *));
    memset(ret.notify_waiters, 0, ret.notify_waiter_count * sizeof(jthread *));

    if (ret.waiter_count > 0) {
      // we have contending and/or waiting threads
      if (nWant > 0) {
        // we have contending threads
        ResourceMark rm(current_thread);
        // get_pending_threads returns only java thread so we do not need to
        // check for non java threads.
        GrowableArray<JavaThread*>* wantList = Threads::get_pending_threads(tlh.list(), nWant, (address)mon);
        if (wantList->length() < nWant) {
          // robustness: the pending list has gotten smaller
          nWant = wantList->length();
        }
        for (int i = 0; i < nWant; i++) {
          JavaThread *pending_thread = wantList->at(i);
          Handle th(current_thread, pending_thread->threadObj());
          ret.waiters[i] = (jthread)jni_reference(calling_thread, th);
        }
      }
      if (nWait > 0) {
        // we have threads in Object.wait()
        int offset = nWant;  // add after any contending threads
        ObjectWaiter *waiter = mon->first_waiter();
        for (int i = 0, j = 0; i < nWait; i++) {
          if (waiter == NULL) {
            // robustness: the waiting list has gotten smaller
            nWait = j;
            break;
          }
          JavaThread *w = mon->thread_of_waiter(waiter);
          if (w != NULL) {
            // If the thread was found on the ObjectWaiter list, then
            // it has not been notified. This thread can't change the
            // state of the monitor so it doesn't need to be suspended.
            Handle th(current_thread, w->threadObj());
            ret.waiters[offset + j] = (jthread)jni_reference(calling_thread, th);
            ret.notify_waiters[j++] = (jthread)jni_reference(calling_thread, th);
          }
          waiter = mon->next_waiter(waiter);
        }
      }
    } // ThreadsListHandle is destroyed here.

    // Adjust count. nWant and nWait count values may be less than original.
    ret.waiter_count = nWant + nWait;
    ret.notify_waiter_count = nWait;
  } else {
    // this object has a lightweight monitor and we have nothing more
    // to do here because the defaults are just fine.
  }

  // we don't update return parameter unless everything worked
  *info_ptr = ret;

  return JVMTI_ERROR_NONE;
}

ResourceTracker::ResourceTracker(JvmtiEnv* env) {
  _env = env;
  _allocations = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<unsigned char*>(20, mtServiceability);
  _failed = false;
}
ResourceTracker::~ResourceTracker() {
  if (_failed) {
    for (int i=0; i<_allocations->length(); i++) {
      _env->deallocate(_allocations->at(i));
    }
  }
  delete _allocations;
}

jvmtiError ResourceTracker::allocate(jlong size, unsigned char** mem_ptr) {
  unsigned char *ptr;
  jvmtiError err = _env->allocate(size, &ptr);
  if (err == JVMTI_ERROR_NONE) {
    _allocations->append(ptr);
    *mem_ptr = ptr;
  } else {
    *mem_ptr = NULL;
    _failed = true;
  }
  return err;
 }

unsigned char* ResourceTracker::allocate(jlong size) {
  unsigned char* ptr;
  allocate(size, &ptr);
  return ptr;
}

char* ResourceTracker::strdup(const char* str) {
  char *dup_str = (char*)allocate(strlen(str)+1);
  if (dup_str != NULL) {
    strcpy(dup_str, str);
  }
  return dup_str;
}

struct StackInfoNode {
  struct StackInfoNode *next;
  jvmtiStackInfo info;
};

// Create a jvmtiStackInfo inside a linked list node and create a
// buffer for the frame information, both allocated as resource objects.
// Fill in both the jvmtiStackInfo and the jvmtiFrameInfo.
// Note that either or both of thr and thread_oop
// may be null if the thread is new or has exited.
void
MultipleStackTracesCollector::fill_frames(jthread jt, JavaThread *thr, oop thread_oop) {
#ifdef ASSERT
  Thread *current_thread = Thread::current();
  assert(SafepointSynchronize::is_at_safepoint() ||
         thr->is_handshake_safe_for(current_thread),
         "call by myself / at safepoint / at handshake");
#endif

  jint state = 0;
  struct StackInfoNode *node = NEW_RESOURCE_OBJ(struct StackInfoNode);
  jvmtiStackInfo *infop = &(node->info);
  node->next = head();
  set_head(node);
  infop->frame_count = 0;
  infop->thread = jt;

  if (thread_oop != NULL) {
    // get most state bits
    state = (jint)java_lang_Thread::get_thread_status(thread_oop);
  }

  if (thr != NULL) {    // add more state bits if there is a JavaThead to query
    if (thr->is_suspended()) {
      state |= JVMTI_THREAD_STATE_SUSPENDED;
    }
    JavaThreadState jts = thr->thread_state();
    if (jts == _thread_in_native) {
      state |= JVMTI_THREAD_STATE_IN_NATIVE;
    }
    if (thr->is_interrupted(false)) {
      state |= JVMTI_THREAD_STATE_INTERRUPTED;
    }
  }
  infop->state = state;

  if (thr != NULL && (state & JVMTI_THREAD_STATE_ALIVE) != 0) {
    infop->frame_buffer = NEW_RESOURCE_ARRAY(jvmtiFrameInfo, max_frame_count());
    env()->get_stack_trace(thr, 0, max_frame_count(),
                           infop->frame_buffer, &(infop->frame_count));
  } else {
    infop->frame_buffer = NULL;
    infop->frame_count = 0;
  }
  _frame_count_total += infop->frame_count;
}

// Based on the stack information in the linked list, allocate memory
// block to return and fill it from the info in the linked list.
void
MultipleStackTracesCollector::allocate_and_fill_stacks(jint thread_count) {
  // do I need to worry about alignment issues?
  jlong alloc_size =  thread_count       * sizeof(jvmtiStackInfo)
                    + _frame_count_total * sizeof(jvmtiFrameInfo);
  env()->allocate(alloc_size, (unsigned char **)&_stack_info);

  // pointers to move through the newly allocated space as it is filled in
  jvmtiStackInfo *si = _stack_info + thread_count;      // bottom of stack info
  jvmtiFrameInfo *fi = (jvmtiFrameInfo *)si;            // is the top of frame info

  // copy information in resource area into allocated buffer
  // insert stack info backwards since linked list is backwards
  // insert frame info forwards
  // walk the StackInfoNodes
  for (struct StackInfoNode *sin = head(); sin != NULL; sin = sin->next) {
    jint frame_count = sin->info.frame_count;
    size_t frames_size = frame_count * sizeof(jvmtiFrameInfo);
    --si;
    memcpy(si, &(sin->info), sizeof(jvmtiStackInfo));
    if (frames_size == 0) {
      si->frame_buffer = NULL;
    } else {
      memcpy(fi, sin->info.frame_buffer, frames_size);
      si->frame_buffer = fi;  // point to the new allocated copy of the frames
      fi += frame_count;
    }
  }
  assert(si == _stack_info, "the last copied stack info must be the first record");
  assert((unsigned char *)fi == ((unsigned char *)_stack_info) + alloc_size,
         "the last copied frame info must be the last record");
}


void
VM_GetThreadListStackTraces::doit() {
  assert(SafepointSynchronize::is_at_safepoint(), "must be at safepoint");

  ResourceMark rm;
  ThreadsListHandle tlh;
  for (int i = 0; i < _thread_count; ++i) {
    jthread jt = _thread_list[i];
    JavaThread* java_thread = NULL;
    oop thread_oop = NULL;
    jvmtiError err = JvmtiExport::cv_external_thread_to_JavaThread(tlh.list(), jt, &java_thread, &thread_oop);
    if (err != JVMTI_ERROR_NONE) {
      // We got an error code so we don't have a JavaThread *, but
      // only return an error from here if we didn't get a valid
      // thread_oop.
      if (thread_oop == NULL) {
        _collector.set_result(err);
        return;
      }
      // We have a valid thread_oop.
    }
    _collector.fill_frames(jt, java_thread, thread_oop);
  }
  _collector.allocate_and_fill_stacks(_thread_count);
}

void
GetSingleStackTraceClosure::do_thread(Thread *target) {
  JavaThread *jt = JavaThread::cast(target);
  oop thread_oop = jt->threadObj();

  if (!jt->is_exiting() && thread_oop != NULL) {
    ResourceMark rm;
    _collector.fill_frames(_jthread, jt, thread_oop);
    _collector.allocate_and_fill_stacks(1);
  }
}

void
VM_GetAllStackTraces::doit() {
  assert(SafepointSynchronize::is_at_safepoint(), "must be at safepoint");

  ResourceMark rm;
  _final_thread_count = 0;
  for (JavaThreadIteratorWithHandle jtiwh; JavaThread *jt = jtiwh.next(); ) {
    oop thread_oop = jt->threadObj();
    if (thread_oop != NULL &&
        !jt->is_exiting() &&
        java_lang_Thread::is_alive(thread_oop) &&
        !jt->is_hidden_from_external_view()) {
      ++_final_thread_count;
      // Handle block of the calling thread is used to create local refs.
      _collector.fill_frames((jthread)JNIHandles::make_local(_calling_thread, thread_oop),
                             jt, thread_oop);
    }
  }
  _collector.allocate_and_fill_stacks(_final_thread_count);
}

// Verifies that the top frame is a java frame in an expected state.
// Deoptimizes frame if needed.
// Checks that the frame method signature matches the return type (tos).
// HandleMark must be defined in the caller only.
// It is to keep a ret_ob_h handle alive after return to the caller.
jvmtiError
JvmtiEnvBase::check_top_frame(Thread* current_thread, JavaThread* java_thread,
                              jvalue value, TosState tos, Handle* ret_ob_h) {
  ResourceMark rm(current_thread);

  vframe *vf = vframeForNoProcess(java_thread, 0);
  NULL_CHECK(vf, JVMTI_ERROR_NO_MORE_FRAMES);

  javaVFrame *jvf = (javaVFrame*) vf;
  if (!vf->is_java_frame() || jvf->method()->is_native()) {
    return JVMTI_ERROR_OPAQUE_FRAME;
  }

  // If the frame is a compiled one, need to deoptimize it.
  if (vf->is_compiled_frame()) {
    if (!vf->fr().can_be_deoptimized()) {
      return JVMTI_ERROR_OPAQUE_FRAME;
    }
    Deoptimization::deoptimize_frame(java_thread, jvf->fr().id());
  }

  // Get information about method return type
  Symbol* signature = jvf->method()->signature();

  ResultTypeFinder rtf(signature);
  TosState fr_tos = as_TosState(rtf.type());
  if (fr_tos != tos) {
    if (tos != itos || (fr_tos != btos && fr_tos != ztos && fr_tos != ctos && fr_tos != stos)) {
      return JVMTI_ERROR_TYPE_MISMATCH;
    }
  }

  // Check that the jobject class matches the return type signature.
  jobject jobj = value.l;
  if (tos == atos && jobj != NULL) { // NULL reference is allowed
    Handle ob_h(current_thread, JNIHandles::resolve_external_guard(jobj));
    NULL_CHECK(ob_h, JVMTI_ERROR_INVALID_OBJECT);
    Klass* ob_k = ob_h()->klass();
    NULL_CHECK(ob_k, JVMTI_ERROR_INVALID_OBJECT);

    // Method return type signature.
    char* ty_sign = 1 + strchr(signature->as_C_string(), JVM_SIGNATURE_ENDFUNC);

    if (!VM_GetOrSetLocal::is_assignable(ty_sign, ob_k, current_thread)) {
      return JVMTI_ERROR_TYPE_MISMATCH;
    }
    *ret_ob_h = ob_h;
  }
  return JVMTI_ERROR_NONE;
} /* end check_top_frame */


// ForceEarlyReturn<type> follows the PopFrame approach in many aspects.
// Main difference is on the last stage in the interpreter.
// The PopFrame stops method execution to continue execution
// from the same method call instruction.
// The ForceEarlyReturn forces return from method so the execution
// continues at the bytecode following the method call.

// java_thread - protected by ThreadsListHandle and pre-checked

jvmtiError
JvmtiEnvBase::force_early_return(JavaThread* java_thread, jvalue value, TosState tos) {
  // retrieve or create the state
  JvmtiThreadState* state = JvmtiThreadState::state_for(java_thread);
  if (state == NULL) {
    return JVMTI_ERROR_THREAD_NOT_ALIVE;
  }

  // Eagerly reallocate scalar replaced objects.
  JavaThread* current_thread = JavaThread::current();
  EscapeBarrier eb(true, current_thread, java_thread);
  if (!eb.deoptimize_objects(0)) {
    // Reallocation of scalar replaced objects failed -> return with error
    return JVMTI_ERROR_OUT_OF_MEMORY;
  }

  SetForceEarlyReturn op(state, value, tos);
  if (java_thread == current_thread) {
    op.doit(java_thread, true /* self */);
  } else {
    Handshake::execute(&op, java_thread);
  }
  return op.result();
}

void
SetForceEarlyReturn::doit(Thread *target, bool self) {
  JavaThread* java_thread = JavaThread::cast(target);
  Thread* current_thread = Thread::current();
  HandleMark   hm(current_thread);

  if (!self) {
    if (!java_thread->is_suspended()) {
      _result = JVMTI_ERROR_THREAD_NOT_SUSPENDED;
      return;
    }
  }

  // Check to see if a ForceEarlyReturn was already in progress
  if (_state->is_earlyret_pending()) {
    // Probably possible for JVMTI clients to trigger this, but the
    // JPDA backend shouldn't allow this to happen
    _result = JVMTI_ERROR_INTERNAL;
    return;
  }
  {
    // The same as for PopFrame. Workaround bug:
    //  4812902: popFrame hangs if the method is waiting at a synchronize
    // Catch this condition and return an error to avoid hanging.
    // Now JVMTI spec allows an implementation to bail out with an opaque
    // frame error.
    OSThread* osThread = java_thread->osthread();
    if (osThread->get_state() == MONITOR_WAIT) {
      _result = JVMTI_ERROR_OPAQUE_FRAME;
      return;
    }
  }

  Handle ret_ob_h;
  _result = JvmtiEnvBase::check_top_frame(current_thread, java_thread, _value, _tos, &ret_ob_h);
  if (_result != JVMTI_ERROR_NONE) {
    return;
  }
  assert(_tos != atos || _value.l == NULL || ret_ob_h() != NULL,
         "return object oop must not be NULL if jobject is not NULL");

  // Update the thread state to reflect that the top frame must be
  // forced to return.
  // The current frame will be returned later when the suspended
  // thread is resumed and right before returning from VM to Java.
  // (see call_VM_base() in assembler_<cpu>.cpp).

  _state->set_earlyret_pending();
  _state->set_earlyret_oop(ret_ob_h());
  _state->set_earlyret_value(_value, _tos);

  // Set pending step flag for this early return.
  // It is cleared when next step event is posted.
  _state->set_pending_step_for_earlyret();
}

void
JvmtiMonitorClosure::do_monitor(ObjectMonitor* mon) {
  if ( _error != JVMTI_ERROR_NONE) {
    // Error occurred in previous iteration so no need to add
    // to the list.
    return;
  }
  if (mon->owner() == _java_thread ) {
    // Filter out on stack monitors collected during stack walk.
    oop obj = mon->object();
    bool found = false;
    for (int j = 0; j < _owned_monitors_list->length(); j++) {
      jobject jobj = ((jvmtiMonitorStackDepthInfo*)_owned_monitors_list->at(j))->monitor;
      oop check = JNIHandles::resolve(jobj);
      if (check == obj) {
        // On stack monitor already collected during the stack walk.
        found = true;
        break;
      }
    }
    if (found == false) {
      // This is off stack monitor (e.g. acquired via jni MonitorEnter).
      jvmtiError err;
      jvmtiMonitorStackDepthInfo *jmsdi;
      err = _env->allocate(sizeof(jvmtiMonitorStackDepthInfo), (unsigned char **)&jmsdi);
      if (err != JVMTI_ERROR_NONE) {
        _error = err;
        return;
      }
      Handle hobj(Thread::current(), obj);
      jmsdi->monitor = _env->jni_reference(_calling_thread, hobj);
      // stack depth is unknown for this monitor.
      jmsdi->stack_depth = -1;
      _owned_monitors_list->append(jmsdi);
    }
  }
}

GrowableArray<OopHandle>* JvmtiModuleClosure::_tbl = NULL;

void JvmtiModuleClosure::do_module(ModuleEntry* entry) {
  assert_locked_or_safepoint(Module_lock);
  OopHandle module = entry->module_handle();
  guarantee(module.resolve() != NULL, "module object is NULL");
  _tbl->push(module);
}

jvmtiError
JvmtiModuleClosure::get_all_modules(JvmtiEnv* env, jint* module_count_ptr, jobject** modules_ptr) {
  ResourceMark rm;
  MutexLocker mcld(ClassLoaderDataGraph_lock);
  MutexLocker ml(Module_lock);

  _tbl = new GrowableArray<OopHandle>(77);
  if (_tbl == NULL) {
    return JVMTI_ERROR_OUT_OF_MEMORY;
  }

  // Iterate over all the modules loaded to the system.
  ClassLoaderDataGraph::modules_do(&do_module);

  jint len = _tbl->length();
  guarantee(len > 0, "at least one module must be present");

  jobject* array = (jobject*)env->jvmtiMalloc((jlong)(len * sizeof(jobject)));
  if (array == NULL) {
    return JVMTI_ERROR_OUT_OF_MEMORY;
  }
  for (jint idx = 0; idx < len; idx++) {
    array[idx] = JNIHandles::make_local(Thread::current(), _tbl->at(idx).resolve());
  }
  _tbl = NULL;
  *modules_ptr = array;
  *module_count_ptr = len;
  return JVMTI_ERROR_NONE;
}

void
UpdateForPopTopFrameClosure::doit(Thread *target, bool self) {
  Thread* current_thread  = Thread::current();
  HandleMark hm(current_thread);
  JavaThread* java_thread = JavaThread::cast(target);
  assert(java_thread == _state->get_thread(), "Must be");

  if (!self && !java_thread->is_suspended()) {
    _result = JVMTI_ERROR_THREAD_NOT_SUSPENDED;
    return;
  }

  // Check to see if a PopFrame was already in progress
  if (java_thread->popframe_condition() != JavaThread::popframe_inactive) {
    // Probably possible for JVMTI clients to trigger this, but the
    // JPDA backend shouldn't allow this to happen
    _result = JVMTI_ERROR_INTERNAL;
    return;
  }

  // Was workaround bug
  //    4812902: popFrame hangs if the method is waiting at a synchronize
  // Catch this condition and return an error to avoid hanging.
  // Now JVMTI spec allows an implementation to bail out with an opaque frame error.
  OSThread* osThread = java_thread->osthread();
  if (osThread->get_state() == MONITOR_WAIT) {
    _result = JVMTI_ERROR_OPAQUE_FRAME;
    return;
  }

  ResourceMark rm(current_thread);
  // Check if there is more than one Java frame in this thread, that the top two frames
  // are Java (not native) frames, and that there is no intervening VM frame
  int frame_count = 0;
  bool is_interpreted[2];
  intptr_t *frame_sp[2];
  // The 2-nd arg of constructor is needed to stop iterating at java entry frame.
  for (vframeStream vfs(java_thread, true, false /* process_frames */); !vfs.at_end(); vfs.next()) {
    methodHandle mh(current_thread, vfs.method());
    if (mh->is_native()) {
      _result = JVMTI_ERROR_OPAQUE_FRAME;
      return;
    }
    is_interpreted[frame_count] = vfs.is_interpreted_frame();
    frame_sp[frame_count] = vfs.frame_id();
    if (++frame_count > 1) break;
  }
  if (frame_count < 2)  {
    // We haven't found two adjacent non-native Java frames on the top.
    // There can be two situations here:
    //  1. There are no more java frames
    //  2. Two top java frames are separated by non-java native frames
    if(JvmtiEnvBase::vframeForNoProcess(java_thread, 1) == NULL) {
      _result = JVMTI_ERROR_NO_MORE_FRAMES;
      return;
    } else {
      // Intervening non-java native or VM frames separate java frames.
      // Current implementation does not support this. See bug #5031735.
      // In theory it is possible to pop frames in such cases.
      _result = JVMTI_ERROR_OPAQUE_FRAME;
      return;
    }
  }

  // If any of the top 2 frames is a compiled one, need to deoptimize it
  for (int i = 0; i < 2; i++) {
    if (!is_interpreted[i]) {
      Deoptimization::deoptimize_frame(java_thread, frame_sp[i]);
    }
  }

  // Update the thread state to reflect that the top frame is popped
  // so that cur_stack_depth is maintained properly and all frameIDs
  // are invalidated.
  // The current frame will be popped later when the suspended thread
  // is resumed and right before returning from VM to Java.
  // (see call_VM_base() in assembler_<cpu>.cpp).

  // It's fine to update the thread state here because no JVMTI events
  // shall be posted for this PopFrame.

  if (!java_thread->is_exiting() && java_thread->threadObj() != NULL) {
    _state->update_for_pop_top_frame();
    java_thread->set_popframe_condition(JavaThread::popframe_pending_bit);
    // Set pending step flag for this popframe and it is cleared when next
    // step event is posted.
    _state->set_pending_step_for_popframe();
    _result = JVMTI_ERROR_NONE;
  }
}

void
SetFramePopClosure::doit(Thread *target, bool self) {
  ResourceMark rm;
  JavaThread* java_thread = JavaThread::cast(target);

  assert(_state->get_thread() == java_thread, "Must be");

  if (!self && !java_thread->is_suspended()) {
    _result = JVMTI_ERROR_THREAD_NOT_SUSPENDED;
    return;
  }

  vframe *vf = JvmtiEnvBase::vframeForNoProcess(java_thread, _depth);
  if (vf == NULL) {
    _result = JVMTI_ERROR_NO_MORE_FRAMES;
    return;
  }

  if (!vf->is_java_frame() || ((javaVFrame*) vf)->method()->is_native()) {
    _result = JVMTI_ERROR_OPAQUE_FRAME;
    return;
  }

  assert(vf->frame_pointer() != NULL, "frame pointer mustn't be NULL");
  if (java_thread->is_exiting() || java_thread->threadObj() == NULL) {
    return; /* JVMTI_ERROR_THREAD_NOT_ALIVE (default) */
  }
  int frame_number = _state->count_frames() - _depth;
  _state->env_thread_state((JvmtiEnvBase*)_env)->set_frame_pop(frame_number);
  _result = JVMTI_ERROR_NONE;
}

void
GetOwnedMonitorInfoClosure::do_thread(Thread *target) {
  JavaThread *jt = JavaThread::cast(target);
  if (!jt->is_exiting() && (jt->threadObj() != NULL)) {
    _result = ((JvmtiEnvBase *)_env)->get_owned_monitors(_calling_thread,
                                                         jt,
                                                         _owned_monitors_list);
  }
}

void
GetCurrentContendedMonitorClosure::do_thread(Thread *target) {
  JavaThread *jt = JavaThread::cast(target);
  if (!jt->is_exiting() && (jt->threadObj() != NULL)) {
    _result = ((JvmtiEnvBase *)_env)->get_current_contended_monitor(_calling_thread,
                                                                    jt,
                                                                    _owned_monitor_ptr);
  }
}

void
GetStackTraceClosure::do_thread(Thread *target) {
  JavaThread *jt = JavaThread::cast(target);
  if (!jt->is_exiting() && jt->threadObj() != NULL) {
    _result = ((JvmtiEnvBase *)_env)->get_stack_trace(jt,
                                                      _start_depth, _max_count,
                                                      _frame_buffer, _count_ptr);
  }
}

void
GetFrameCountClosure::do_thread(Thread *target) {
  JavaThread* jt = _state->get_thread();
  assert(target == jt, "just checking");
  if (!jt->is_exiting() && jt->threadObj() != NULL) {
    _result = ((JvmtiEnvBase*)_env)->get_frame_count(_state, _count_ptr);
  }
}

void
GetFrameLocationClosure::do_thread(Thread *target) {
  JavaThread *jt = JavaThread::cast(target);
  if (!jt->is_exiting() && jt->threadObj() != NULL) {
    _result = ((JvmtiEnvBase*)_env)->get_frame_location(jt, _depth,
                                                        _method_ptr, _location_ptr);
  }
}
