/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoaderDataGraph.inline.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/protectionDomainCache.hpp"
#include "classfile/stringTable.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "gc/shared/oopStorage.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "memory/universe.hpp"
#include "oops/oopHandle.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/serviceThread.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"
#include "prims/jvmtiImpl.hpp"
#include "prims/jvmtiTagMap.hpp"
#include "prims/resolvedMethodTable.hpp"
#include "services/diagnosticArgument.hpp"
#include "services/diagnosticFramework.hpp"
#include "services/gcNotifier.hpp"
#include "services/lowMemoryDetector.hpp"
#include "services/threadIdTable.hpp"

DEBUG_ONLY(JavaThread* ServiceThread::_instance = NULL;)
JvmtiDeferredEvent* ServiceThread::_jvmti_event = NULL;
// The service thread has it's own static deferred event queue.
// Events can be posted before JVMTI vm_start, so it's too early to call JvmtiThreadState::state_for
// to add this field to the per-JavaThread event queue.  TODO: fix this sometime later
JvmtiDeferredEventQueue ServiceThread::_jvmti_service_queue;

// Defer releasing JavaThread OopHandle to the ServiceThread
class OopHandleList : public CHeapObj<mtInternal> {
  OopHandle      _handle;
  OopHandleList* _next;
 public:
   OopHandleList(OopHandle h, OopHandleList* next) : _handle(h), _next(next) {}
   ~OopHandleList() {
     _handle.release(JavaThread::thread_oop_storage());
   }
   OopHandleList* next() const { return _next; }
};

static OopHandleList* _oop_handle_list = NULL;

static void release_oop_handles() {
  OopHandleList* list;
  {
    MutexLocker ml(Service_lock, Mutex::_no_safepoint_check_flag);
    list = _oop_handle_list;
    _oop_handle_list = NULL;
  }
  assert(!SafepointSynchronize::is_at_safepoint(), "cannot be called at a safepoint");

  while (list != NULL) {
    OopHandleList* l = list;
    list = l->next();
    delete l;
  }
}

void ServiceThread::initialize() {
  EXCEPTION_MARK;

  const char* name = "Service Thread";
  Handle thread_oop = JavaThread::create_system_thread_object(name, false /* not visible */, CHECK);

  ServiceThread* thread = new ServiceThread(&service_thread_entry);
  JavaThread::vm_exit_on_osthread_failure(thread);

  JavaThread::start_internal_daemon(THREAD, thread, thread_oop, NearMaxPriority);
  DEBUG_ONLY(_instance = thread;)
}

static void cleanup_oopstorages() {
  for (OopStorage* storage : OopStorageSet::Range<OopStorageSet::Id>()) {
    storage->delete_empty_blocks();
  }
}

void ServiceThread::service_thread_entry(JavaThread* jt, TRAPS) {
  while (true) {
    bool sensors_changed = false;
    bool has_jvmti_events = false;
    bool has_gc_notification_event = false;
    bool has_dcmd_notification_event = false;
    bool stringtable_work = false;
    bool symboltable_work = false;
    bool resolved_method_table_work = false;
    bool thread_id_table_work = false;
    bool protection_domain_table_work = false;
    bool oopstorage_work = false;
    JvmtiDeferredEvent jvmti_event;
    bool oop_handles_to_release = false;
    bool cldg_cleanup_work = false;
    bool jvmti_tagmap_work = false;
    {
      // Need state transition ThreadBlockInVM so that this thread
      // will be handled by safepoint correctly when this thread is
      // notified at a safepoint.

      // This ThreadBlockInVM object is not also considered to be
      // suspend-equivalent because ServiceThread is not visible to
      // external suspension.

      ThreadBlockInVM tbivm(jt);

      MonitorLocker ml(Service_lock, Mutex::_no_safepoint_check_flag);
      // Process all available work on each (outer) iteration, rather than
      // only the first recognized bit of work, to avoid frequently true early
      // tests from potentially starving later work.  Hence the use of
      // arithmetic-or to combine results; we don't want short-circuiting.
      while (((sensors_changed = (!UseNotificationThread && LowMemoryDetector::has_pending_requests())) |
              (has_jvmti_events = _jvmti_service_queue.has_events()) |
              (has_gc_notification_event = (!UseNotificationThread && GCNotifier::has_event())) |
              (has_dcmd_notification_event = (!UseNotificationThread && DCmdFactory::has_pending_jmx_notification())) |
              (stringtable_work = StringTable::has_work()) |
              (symboltable_work = SymbolTable::has_work()) |
              (resolved_method_table_work = ResolvedMethodTable::has_work()) |
              (thread_id_table_work = ThreadIdTable::has_work()) |
              (protection_domain_table_work = SystemDictionary::pd_cache_table()->has_work()) |
              (oopstorage_work = OopStorage::has_cleanup_work_and_reset()) |
              (oop_handles_to_release = (_oop_handle_list != NULL)) |
              (cldg_cleanup_work = ClassLoaderDataGraph::should_clean_metaspaces_and_reset()) |
              (jvmti_tagmap_work = JvmtiTagMap::has_object_free_events_and_reset())
             ) == 0) {
        // Wait until notified that there is some work to do.
        ml.wait();
      }

      if (has_jvmti_events) {
        // Get the event under the Service_lock
        jvmti_event = _jvmti_service_queue.dequeue();
        _jvmti_event = &jvmti_event;
      }
    }

    if (stringtable_work) {
      StringTable::do_concurrent_work(jt);
    }

    if (symboltable_work) {
      SymbolTable::do_concurrent_work(jt);
    }

    if (has_jvmti_events) {
      _jvmti_event->post();
      _jvmti_event = NULL;  // reset
    }

    if (!UseNotificationThread) {
      if (sensors_changed) {
        LowMemoryDetector::process_sensor_changes(jt);
      }

      if(has_gc_notification_event) {
        GCNotifier::sendNotification(CHECK);
      }

      if(has_dcmd_notification_event) {
        DCmdFactory::send_notification(CHECK);
      }
    }

    if (resolved_method_table_work) {
      ResolvedMethodTable::do_concurrent_work(jt);
    }

    if (thread_id_table_work) {
      ThreadIdTable::do_concurrent_work(jt);
    }

    if (protection_domain_table_work) {
      SystemDictionary::pd_cache_table()->unlink();
    }

    if (oopstorage_work) {
      cleanup_oopstorages();
    }

    if (oop_handles_to_release) {
      release_oop_handles();
    }

    if (cldg_cleanup_work) {
      ClassLoaderDataGraph::safepoint_and_clean_metaspaces();
    }

    if (jvmti_tagmap_work) {
      JvmtiTagMap::flush_all_object_free_events();
    }
  }
}

void ServiceThread::enqueue_deferred_event(JvmtiDeferredEvent* event) {
  MutexLocker ml(Service_lock, Mutex::_no_safepoint_check_flag);
  // If you enqueue events before the service thread runs, gc and the sweeper
  // cannot keep the nmethod alive.  This could be restricted to compiled method
  // load and unload events, if we wanted to be picky.
  assert(_instance != NULL, "cannot enqueue events before the service thread runs");
  _jvmti_service_queue.enqueue(*event);
  Service_lock->notify_all();
 }

void ServiceThread::oops_do_no_frames(OopClosure* f, CodeBlobClosure* cf) {
  JavaThread::oops_do_no_frames(f, cf);
  // The ServiceThread "owns" the JVMTI Deferred events, scan them here
  // to keep them alive until they are processed.
  if (_jvmti_event != NULL) {
    _jvmti_event->oops_do(f, cf);
  }
  // Requires a lock, because threads can be adding to this queue.
  MutexLocker ml(Service_lock, Mutex::_no_safepoint_check_flag);
  _jvmti_service_queue.oops_do(f, cf);
}

void ServiceThread::nmethods_do(CodeBlobClosure* cf) {
  JavaThread::nmethods_do(cf);
  if (cf != NULL) {
    if (_jvmti_event != NULL) {
      _jvmti_event->nmethods_do(cf);
    }
    // Requires a lock, because threads can be adding to this queue.
    MutexLocker ml(Service_lock, Mutex::_no_safepoint_check_flag);
    _jvmti_service_queue.nmethods_do(cf);
  }
}

void ServiceThread::add_oop_handle_release(OopHandle handle) {
  MutexLocker ml(Service_lock, Mutex::_no_safepoint_check_flag);
  OopHandleList* new_head = new OopHandleList(handle, _oop_handle_list);
  _oop_handle_list = new_head;
  Service_lock->notify_all();
}
