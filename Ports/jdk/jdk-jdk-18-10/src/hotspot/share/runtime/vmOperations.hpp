/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_VMOPERATIONS_HPP
#define SHARE_RUNTIME_VMOPERATIONS_HPP

#include "oops/oop.hpp"
#include "runtime/vmOperation.hpp"
#include "runtime/thread.hpp"
#include "runtime/threadSMR.hpp"

// A hodge podge of commonly used VM Operations

class VM_None: public VM_Operation {
  const char* _reason;
 public:
  VM_None(const char* reason) : _reason(reason) {}
  const char* name() const { return _reason; }
  VMOp_Type type() const { return VMOp_None; }
  void doit() {};
};

class VM_Cleanup: public VM_Operation {
 public:
  VMOp_Type type() const { return VMOp_Cleanup; }
  void doit() {};
};

class VM_ClearICs: public VM_Operation {
 private:
  bool _preserve_static_stubs;
 public:
  VM_ClearICs(bool preserve_static_stubs) { _preserve_static_stubs = preserve_static_stubs; }
  void doit();
  VMOp_Type type() const { return VMOp_ClearICs; }
};

// empty vm op, evaluated just to force a safepoint
class VM_ForceSafepoint: public VM_Operation {
 public:
  void doit()         {}
  VMOp_Type type() const { return VMOp_ForceSafepoint; }
};

// empty vm op, when forcing a safepoint to suspend a thread
class VM_ThreadSuspend: public VM_ForceSafepoint {
 public:
  VMOp_Type type() const { return VMOp_ThreadSuspend; }
};

// empty vm op, when forcing a safepoint to suspend threads from jvmti
class VM_ThreadsSuspendJVMTI: public VM_ForceSafepoint {
 public:
  VMOp_Type type() const { return VMOp_ThreadsSuspendJVMTI; }
};

// empty vm op, when forcing a safepoint due to inline cache buffers being full
class VM_ICBufferFull: public VM_ForceSafepoint {
 public:
  VMOp_Type type() const { return VMOp_ICBufferFull; }
  virtual bool skip_thread_oop_barriers() const { return true; }
};

// Base class for invoking parts of a gtest in a safepoint.
// Derived classes provide the doit method.
// Typically also need to transition the gtest thread from native to VM.
class VM_GTestExecuteAtSafepoint: public VM_Operation {
 public:
  VMOp_Type type() const                         { return VMOp_GTestExecuteAtSafepoint; }

 protected:
  VM_GTestExecuteAtSafepoint() {}
};

class VM_CleanClassLoaderDataMetaspaces : public VM_Operation {
 public:
  VM_CleanClassLoaderDataMetaspaces() {}
  VMOp_Type type() const                         { return VMOp_CleanClassLoaderDataMetaspaces; }
  void doit();
};

// Deopt helper that can deoptimize frames in threads other than the
// current thread.  Only used through Deoptimization::deoptimize_frame.
class VM_DeoptimizeFrame: public VM_Operation {
  friend class Deoptimization;

 private:
  JavaThread* _thread;
  intptr_t*   _id;
  int _reason;
  VM_DeoptimizeFrame(JavaThread* thread, intptr_t* id, int reason);

 public:
  VMOp_Type type() const                         { return VMOp_DeoptimizeFrame; }
  void doit();
  bool allow_nested_vm_operations() const        { return true;  }
};

#ifndef PRODUCT
class VM_DeoptimizeAll: public VM_Operation {
 private:
  Klass* _dependee;
 public:
  VM_DeoptimizeAll() {}
  VMOp_Type type() const                         { return VMOp_DeoptimizeAll; }
  void doit();
  bool allow_nested_vm_operations() const        { return true; }
};


class VM_ZombieAll: public VM_Operation {
 public:
  VM_ZombieAll() {}
  VMOp_Type type() const                         { return VMOp_ZombieAll; }
  void doit();
  bool allow_nested_vm_operations() const        { return true; }
};
#endif // PRODUCT

class VM_PrintThreads: public VM_Operation {
 private:
  outputStream* _out;
  bool _print_concurrent_locks;
  bool _print_extended_info;
  bool _print_jni_handle_info;
 public:
  VM_PrintThreads()
    : _out(tty), _print_concurrent_locks(PrintConcurrentLocks), _print_extended_info(false), _print_jni_handle_info(false)
  {}
  VM_PrintThreads(outputStream* out, bool print_concurrent_locks, bool print_extended_info, bool print_jni_handle_info)
    : _out(out), _print_concurrent_locks(print_concurrent_locks), _print_extended_info(print_extended_info),
      _print_jni_handle_info(print_jni_handle_info)
  {}
  VMOp_Type type() const {
    return VMOp_PrintThreads;
  }
  void doit();
  bool doit_prologue();
  void doit_epilogue();
};

class VM_PrintMetadata : public VM_Operation {
 private:
  outputStream* const _out;
  const size_t        _scale;
  const int           _flags;

 public:
  VM_PrintMetadata(outputStream* out, size_t scale, int flags)
    : _out(out), _scale(scale), _flags(flags)
  {};

  VMOp_Type type() const  { return VMOp_PrintMetadata; }
  void doit();
};

class DeadlockCycle;
class VM_FindDeadlocks: public VM_Operation {
 private:
  bool              _concurrent_locks;
  DeadlockCycle*    _deadlocks;
  outputStream*     _out;
  ThreadsListSetter _setter;  // Helper to set hazard ptr in the originating thread
                              // which protects the JavaThreads in _deadlocks.

 public:
  VM_FindDeadlocks(bool concurrent_locks) :  _concurrent_locks(concurrent_locks), _deadlocks(NULL), _out(NULL), _setter() {};
  VM_FindDeadlocks(outputStream* st) : _concurrent_locks(true), _deadlocks(NULL), _out(st) {};
  ~VM_FindDeadlocks();

  DeadlockCycle* result()      { return _deadlocks; };
  VMOp_Type type() const       { return VMOp_FindDeadlocks; }
  void doit();
};

class ThreadDumpResult;
class ThreadSnapshot;
class ThreadConcurrentLocks;

class VM_ThreadDump : public VM_Operation {
 private:
  ThreadDumpResult*              _result;
  int                            _num_threads;
  GrowableArray<instanceHandle>* _threads;
  int                            _max_depth;
  bool                           _with_locked_monitors;
  bool                           _with_locked_synchronizers;

  void snapshot_thread(JavaThread* java_thread, ThreadConcurrentLocks* tcl);

 public:
  VM_ThreadDump(ThreadDumpResult* result,
                int max_depth,  // -1 indicates entire stack
                bool with_locked_monitors,
                bool with_locked_synchronizers);

  VM_ThreadDump(ThreadDumpResult* result,
                GrowableArray<instanceHandle>* threads,
                int num_threads, // -1 indicates entire stack
                int max_depth,
                bool with_locked_monitors,
                bool with_locked_synchronizers);

  VMOp_Type type() const { return VMOp_ThreadDump; }
  void doit();
  bool doit_prologue();
  void doit_epilogue();
};


class VM_Exit: public VM_Operation {
 private:
  int  _exit_code;
  static volatile bool _vm_exited;
  static Thread * volatile _shutdown_thread;
  static void wait_if_vm_exited();
 public:
  VM_Exit(int exit_code) {
    _exit_code = exit_code;
  }
  static int wait_for_threads_in_native_to_block();
  static int set_vm_exited();
  static bool vm_exited()                      { return _vm_exited; }
  static Thread * shutdown_thread()            { return _shutdown_thread; }
  static void block_if_vm_exited() {
    if (_vm_exited) {
      wait_if_vm_exited();
    }
  }
  VMOp_Type type() const { return VMOp_Exit; }
  void doit();
};

class VM_PrintCompileQueue: public VM_Operation {
 private:
  outputStream* _out;

 public:
  VM_PrintCompileQueue(outputStream* st) : _out(st) {}
  VMOp_Type type() const { return VMOp_PrintCompileQueue; }
  void doit();
};

#if INCLUDE_SERVICES
class VM_PrintClassHierarchy: public VM_Operation {
 private:
  outputStream* _out;
  bool _print_interfaces;
  bool _print_subclasses;
  char* _classname;

 public:
  VM_PrintClassHierarchy(outputStream* st, bool print_interfaces, bool print_subclasses, char* classname) :
    _out(st), _print_interfaces(print_interfaces), _print_subclasses(print_subclasses),
    _classname(classname) {}
  VMOp_Type type() const { return VMOp_PrintClassHierarchy; }
  void doit();
};
#endif // INCLUDE_SERVICES

#endif // SHARE_RUNTIME_VMOPERATIONS_HPP
