/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef GTEST_THREADHELPER_INLINE_HPP
#define GTEST_THREADHELPER_INLINE_HPP

#include "runtime/mutex.hpp"
#include "runtime/semaphore.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vmThread.hpp"
#include "runtime/vmOperations.hpp"
#include "unittest.hpp"

static void startTestThread(JavaThread* thread, const char* name) {
  EXCEPTION_MARK;
  HandleMark hm(THREAD);
  Handle thread_oop;

  // This code can be called from the main thread, which is _thread_in_native,
  // or by an existing JavaTestThread, which is _thread_in_vm.
  if (THREAD->thread_state() == _thread_in_native) {
    ThreadInVMfromNative tivfn(THREAD);
    thread_oop = JavaThread::create_system_thread_object(name, false /* not visible */, CHECK);
    JavaThread::start_internal_daemon(THREAD, thread, thread_oop, NoPriority);
  } else {
    thread_oop = JavaThread::create_system_thread_object(name, false /* not visible */, CHECK);
    JavaThread::start_internal_daemon(THREAD, thread, thread_oop, NoPriority);
  }
}

class VM_StopSafepoint : public VM_Operation {
public:
  Semaphore* _running;
  Semaphore* _test_complete;
  VM_StopSafepoint(Semaphore* running, Semaphore* wait_for) :
    _running(running), _test_complete(wait_for) {}
  VMOp_Type type() const          { return VMOp_None; }
  bool evaluate_at_safepoint() const { return false; }
  void doit()                     { _running->signal(); _test_complete->wait(); }
};

// This class and thread keep the non-safepoint op running while we do our testing.
class VMThreadBlocker : public JavaThread {
  Semaphore _ready;
  Semaphore _unblock;

  static void blocker_thread_entry(JavaThread* thread, TRAPS) {
    VMThreadBlocker* t = static_cast<VMThreadBlocker*>(thread);
    VM_StopSafepoint ss(&t->_ready, &t->_unblock);
    VMThread::execute(&ss);
  }

  VMThreadBlocker() : JavaThread(&blocker_thread_entry) {};

  virtual ~VMThreadBlocker() {}

public:
  // Convenience method for client code
  static VMThreadBlocker* start() {
    const char* name = "VMThreadBlocker";
    VMThreadBlocker* thread = new VMThreadBlocker();
    JavaThread::vm_exit_on_osthread_failure(thread);
    startTestThread(thread, name);
    return thread;
  }

  void ready() {
    _ready.wait();
  }
  void release() {
    _unblock.signal();
  }
};

// For testing in a real JavaThread.
class JavaTestThread : public JavaThread {
  Semaphore* _post;

protected:
  JavaTestThread(Semaphore* post)
    : JavaThread(&test_thread_entry), _post(post) {
    JavaThread::vm_exit_on_osthread_failure(this);
  }
  virtual ~JavaTestThread() {}

public:
  // simplified starting for callers and subclasses
  void doit() {
    startTestThread(this, "JavaTestThread");
  }

  virtual void main_run() = 0;

  static void test_thread_entry(JavaThread* thread, TRAPS) {
    JavaTestThread* t = static_cast<JavaTestThread*>(thread);
    t->main_run();
    t->_post->signal();
  }
};

template <typename FUNC>
class SingleTestThread : public JavaTestThread {
  FUNC& _f;
public:
  SingleTestThread(Semaphore* post, FUNC& f)
    : JavaTestThread(post), _f(f) {
  }

  virtual ~SingleTestThread(){}

  virtual void main_run() {
    _f(this);
  }
};

template <typename TESTFUNC>
static void nomt_test_doer(TESTFUNC &f) {
  Semaphore post;

  VMThreadBlocker* blocker = VMThreadBlocker::start();

  blocker->ready();

  SingleTestThread<TESTFUNC>* stt = new SingleTestThread<TESTFUNC>(&post, f);
  stt->doit();
  post.wait();

  blocker->release();
}

template <typename RUNNER>
static void mt_test_doer() {
  Semaphore post;

  VMThreadBlocker* blocker = VMThreadBlocker::start();

  blocker->ready();

  RUNNER* runner = new RUNNER(&post);
  runner->doit();
  post.wait();

  blocker->release();
}

#endif // include guard
