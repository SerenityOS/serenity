/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_JVMTIRAWMONITOR_HPP
#define SHARE_PRIMS_JVMTIRAWMONITOR_HPP

#include "memory/allocation.hpp"
#include "runtime/park.hpp"
#include "utilities/growableArray.hpp"

//
// class JvmtiRawMonitor
//
// Used by JVMTI methods: All RawMonitor methods (CreateRawMonitor, EnterRawMonitor, etc.)
//
// A simplified version of the ObjectMonitor code.
//

// Important note:
// Raw monitors can be used in callbacks which happen during safepoint by the VM
// thread (e.g., heapRootCallback). This means we may not transition/safepoint
// poll in many cases, else the agent JavaThread can deadlock with the VM thread,
// as this old comment says:
// "We can't safepoint block in here because we could deadlock the vmthread. Blech."
// The rules are:
// - We must never safepoint poll if raw monitor is owned.
// - We may safepoint poll before it is owned and after it has been released.
// If this were the only thing we needed to think about we could just stay in
// native for all operations. However we need to honor a suspend request, not
// entering a monitor if suspended, and check for interrupts. Honoring a suspend
// request and reading the interrupt flag must be done from VM state
// (a safepoint unsafe state).

class JvmtiRawMonitor : public CHeapObj<mtSynchronizer>  {

  // Helper class to allow Threads to be linked into queues.
  // This is a stripped down version of ObjectWaiter.
  class QNode : public StackObj {
    friend class JvmtiRawMonitor;
    enum TStates { TS_READY, TS_RUN, TS_WAIT, TS_ENTER };
    QNode* volatile _next;
    QNode* volatile _prev;
    ParkEvent* _event;
    volatile int _notified;
    volatile TStates _t_state;

    QNode(Thread* thread);
  };

  Thread* volatile _owner;      // pointer to owning thread
  volatile int _recursions;     // recursion count, 0 for first entry
  QNode* volatile _entry_list;  // Threads blocked on entry or reentry.
                                // The list is actually composed of nodes,
                                // acting as proxies for Threads.
  QNode* volatile _wait_set;    // Threads wait()ing on the monitor
  int _magic;
  char* _name;
  // JVMTI_RM_MAGIC is set in contructor and unset in destructor.
  enum { JVMTI_RM_MAGIC = (int)(('T' << 24) | ('I' << 16) | ('R' << 8) | 'M') };

  // Helpers for queue management isolation
  void enqueue_waiter(QNode& node);
  void dequeue_waiter(QNode& node);

  // Mostly low-level implementation routines
  void simple_enter(Thread* self);
  void simple_exit(Thread* self);
  int simple_wait(Thread* self, jlong millis);
  void simple_notify(Thread* self, bool all);

  class ExitOnSuspend {
   protected:
    JvmtiRawMonitor* _rm;
    bool _rm_exited;
   public:
    ExitOnSuspend(JvmtiRawMonitor* rm) : _rm(rm), _rm_exited(false) {}
    void operator()(JavaThread* current);
    bool monitor_exited() { return _rm_exited; }
  };

 public:

  // return codes
  enum {
    M_OK,                    // no error
    M_ILLEGAL_MONITOR_STATE, // IllegalMonitorStateException
    M_INTERRUPTED            // Thread.interrupt()
  };

  // Non-aborting operator new
  void* operator new(size_t size) throw() {
    return CHeapObj::operator new(size, std::nothrow);
  }

  JvmtiRawMonitor(const char* name);
  ~JvmtiRawMonitor();

  Thread* owner() const { return _owner; }
  void set_owner(Thread* owner) { _owner = owner; }
  int recursions() const { return _recursions; }
  void raw_enter(Thread* self);
  int raw_exit(Thread* self);
  int raw_wait(jlong millis, Thread* self);
  int raw_notify(Thread* self);
  int raw_notifyAll(Thread* self);
  int magic() const { return _magic; }
  const char* get_name() const { return _name; }
  bool is_valid();
};

// Onload pending raw monitors
// Class is used to cache onload or onstart monitor enter
// which will transition into real monitor when
// VM is fully initialized.
class JvmtiPendingMonitors : public AllStatic {

 private:
  static GrowableArray<JvmtiRawMonitor*>* _monitors; // Cache raw monitor enter

  inline static GrowableArray<JvmtiRawMonitor*>* monitors() { return _monitors; }

  static void dispose() {
    delete monitors();
  }

 public:
  static void enter(JvmtiRawMonitor* monitor) {
    monitors()->append(monitor);
  }

  static int count() {
    return monitors()->length();
  }

  static void destroy(JvmtiRawMonitor* monitor) {
    while (monitors()->contains(monitor)) {
      monitors()->remove(monitor);
    }
  }

  // Return false if monitor is not found in the list.
  static bool exit(JvmtiRawMonitor* monitor) {
    return monitors()->remove_if_existing(monitor);
  }

  static void transition_raw_monitors();
};

#endif // SHARE_PRIMS_JVMTIRAWMONITOR_HPP
