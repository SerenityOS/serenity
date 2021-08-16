/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_HANDSHAKE_HPP
#define SHARE_RUNTIME_HANDSHAKE_HPP

#include "memory/allocation.hpp"
#include "memory/iterator.hpp"
#include "runtime/flags/flagSetting.hpp"
#include "runtime/mutex.hpp"
#include "runtime/orderAccess.hpp"
#include "utilities/filterQueue.hpp"

class HandshakeOperation;
class JavaThread;
class SuspendThreadHandshake;
class ThreadSelfSuspensionHandshake;

// A handshake closure is a callback that is executed for a JavaThread
// while it is in a safepoint/handshake-safe state. Depending on the
// nature of the closure, the callback may be executed by the initiating
// thread, the target thread, or the VMThread. If the callback is not executed
// by the target thread it will remain in a blocked state until the callback completes.
class HandshakeClosure : public ThreadClosure, public CHeapObj<mtThread> {
  const char* const _name;
 public:
  HandshakeClosure(const char* name) : _name(name) {}
  virtual ~HandshakeClosure()                      {}
  const char* name() const                         { return _name; }
  virtual bool is_async()                          { return false; }
  virtual bool is_suspend()                        { return false; }
  virtual void do_thread(Thread* thread) = 0;
};

class AsyncHandshakeClosure : public HandshakeClosure {
 public:
   AsyncHandshakeClosure(const char* name) : HandshakeClosure(name) {}
   virtual ~AsyncHandshakeClosure() {}
   virtual bool is_async()          { return true; }
};

class Handshake : public AllStatic {
 public:
  // Execution of handshake operation
  static void execute(HandshakeClosure*       hs_cl);
  static void execute(HandshakeClosure*       hs_cl, JavaThread* target);
  static void execute(AsyncHandshakeClosure*  hs_cl, JavaThread* target);
};

class JvmtiRawMonitor;

// The HandshakeState keeps track of an ongoing handshake for this JavaThread.
// VMThread/Handshaker and JavaThread are serialized with _lock making sure the
// operation is only done by either VMThread/Handshaker on behalf of the
// JavaThread or by the target JavaThread itself.
class HandshakeState {
  friend ThreadSelfSuspensionHandshake;
  friend SuspendThreadHandshake;
  friend JavaThread;
  // This a back reference to the JavaThread,
  // the target for all operation in the queue.
  JavaThread* _handshakee;
  // The queue containing handshake operations to be performed on _handshakee.
  FilterQueue<HandshakeOperation*> _queue;
  // Provides mutual exclusion to this state and queue. Also used for
  // JavaThread suspend/resume operations.
  Monitor _lock;
  // Set to the thread executing the handshake operation.
  Thread* volatile _active_handshaker;

  bool claim_handshake();
  bool possibly_can_process_handshake();
  bool can_process_handshake();

  bool have_non_self_executable_operation();
  HandshakeOperation* get_op_for_self(bool allow_suspend);
  HandshakeOperation* get_op();
  void remove_op(HandshakeOperation* op);

  void set_active_handshaker(Thread* thread) { Atomic::store(&_active_handshaker, thread); }

  class MatchOp {
    HandshakeOperation* _op;
   public:
    MatchOp(HandshakeOperation* op) : _op(op) {}
    bool operator()(HandshakeOperation* op) {
      return op == _op;
    }
  };

 public:
  HandshakeState(JavaThread* thread);

  void add_operation(HandshakeOperation* op);

  bool has_operation() {
    return !_queue.is_empty();
  }
  bool has_a_non_suspend_operation();

  bool operation_pending(HandshakeOperation* op);

  // If the method returns true we need to check for a possible safepoint.
  // This is due to a suspension handshake which put the JavaThread in blocked
  // state so a safepoint may be in-progress.
  bool process_by_self(bool allow_suspend);

  enum ProcessResult {
    _no_operation = 0,
    _not_safe,
    _claim_failed,
    _processed,
    _succeeded,
    _number_states
  };
  ProcessResult try_process(HandshakeOperation* match_op);

  Thread* active_handshaker() const { return Atomic::load(&_active_handshaker); }

  // Suspend/resume support
 private:
  // This flag is true when the thread owning this
  // HandshakeState (the _handshakee) is suspended.
  volatile bool _suspended;
  // This flag is true while there is async handshake (trap)
  // on queue. Since we do only need one, we can reuse it if
  // thread gets suspended again (after a resume)
  // and we have not yet processed it.
  bool _async_suspend_handshake;

  // Called from the suspend handshake.
  bool suspend_with_handshake();
  // Called from the async handshake (the trap)
  // to stop a thread from continuing execution when suspended.
  void do_self_suspend();

  bool is_suspended()                       { return Atomic::load(&_suspended); }
  void set_suspended(bool to)               { return Atomic::store(&_suspended, to); }
  bool has_async_suspend_handshake()        { return _async_suspend_handshake; }
  void set_async_suspend_handshake(bool to) { _async_suspend_handshake = to; }

  bool suspend();
  bool resume();
};

#endif // SHARE_RUNTIME_HANDSHAKE_HPP
