/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_VMTHREAD_HPP
#define SHARE_RUNTIME_VMTHREAD_HPP

#include "runtime/perfDataTypes.hpp"
#include "runtime/nonJavaThread.hpp"
#include "runtime/thread.hpp"
#include "runtime/task.hpp"
#include "runtime/vmOperation.hpp"

// VM operation timeout handling: warn or abort the VM when VM operation takes
// too long. Periodic tasks do not participate in safepoint protocol, and therefore
// can fire when application threads are stopped.

class VMOperationTimeoutTask : public PeriodicTask {
private:
  volatile int _armed;
  jlong _arm_time;
  const char* _vm_op_name;
public:
  VMOperationTimeoutTask(size_t interval_time) :
          PeriodicTask(interval_time), _armed(0), _arm_time(0), _vm_op_name(nullptr) {}

  virtual void task();

  bool is_armed();
  void arm(const char* vm_op_name);
  void disarm();
};

//
// A single VMThread (the primordial thread) spawns all other threads
// and is itself used by other threads to offload heavy vm operations
// like scavenge, garbage_collect etc.
//

class VMThread: public NamedThread {
 private:
  static ThreadPriority _current_priority;

  static bool _should_terminate;
  static bool _terminated;
  static Monitor * _terminate_lock;
  static PerfCounter* _perf_accumulated_vm_operation_time;

  static VMOperationTimeoutTask* _timeout_task;

  static bool handshake_alot();
  static void setup_periodic_safepoint_if_needed();

  void evaluate_operation(VM_Operation* op);
  void inner_execute(VM_Operation* op);
  void wait_for_operation();

 public:
  // Constructor
  VMThread();

  // No destruction allowed
  ~VMThread() {
    guarantee(false, "VMThread deletion must fix the race with VM termination");
  }


  // Tester
  bool is_VM_thread() const                      { return true; }

  // The ever running loop for the VMThread
  void loop();

  // Called to stop the VM thread
  static void wait_for_vm_thread_exit();
  static bool should_terminate()                  { return _should_terminate; }
  static bool is_terminated()                     { return _terminated == true; }

  // Execution of vm operation
  static void execute(VM_Operation* op);

  // Returns the current vm operation if any.
  static VM_Operation* vm_operation()             {
    assert(Thread::current()->is_VM_thread(), "Must be");
    return _cur_vm_operation;
  }

  static VM_Operation::VMOp_Type vm_op_type()     {
    VM_Operation* op = vm_operation();
    assert(op != NULL, "sanity");
    return op->type();
  }

  // Returns the single instance of VMThread.
  static VMThread* vm_thread()                    { return _vm_thread; }

  void verify();

  // Performance measurement
  static PerfCounter* perf_accumulated_vm_operation_time() {
    return _perf_accumulated_vm_operation_time;
  }

  // Entry for starting vm thread
  virtual void run();

  // Creations/Destructions
  static void create();
  static void destroy();

  static void wait_until_executed(VM_Operation* op);

  // Printing
  const char* type_name() const { return "VMThread"; }

 private:
  // VM_Operation support
  static VM_Operation*     _cur_vm_operation;   // Current VM operation
  static VM_Operation*     _next_vm_operation;  // Next VM operation

  bool set_next_operation(VM_Operation *op);    // Set the _next_vm_operation if possible.

  // Pointer to single-instance of VM thread
  static VMThread*     _vm_thread;
};

#endif // SHARE_RUNTIME_VMTHREAD_HPP
