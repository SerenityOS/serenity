/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2013 SAP SE. All rights reserved.
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

#ifndef OS_AIX_OSTHREAD_AIX_HPP
#define OS_AIX_OSTHREAD_AIX_HPP

 public:
  typedef pthread_t thread_id_t;

 private:
  int _thread_type;

 public:

  int thread_type() const {
    return _thread_type;
  }
  void set_thread_type(int type) {
    _thread_type = type;
  }

 private:

  // On AIX, we use the pthread id as OSThread::thread_id and keep the kernel thread id
  // separately for diagnostic purposes.
  //
  // Note: this kernel thread id is saved at thread start. Depending on the
  // AIX scheduling mode, this may not be the current thread id (usually not
  // a problem though as we run with AIXTHREAD_SCOPE=S).
  tid_t _kernel_thread_id;

  sigset_t _caller_sigmask; // Caller's signal mask

 public:

  // Methods to save/restore caller's signal mask
  sigset_t  caller_sigmask() const       { return _caller_sigmask; }
  void    set_caller_sigmask(sigset_t sigmask)  { _caller_sigmask = sigmask; }

#ifndef PRODUCT
  // Used for debugging, return a unique integer for each thread.
  int thread_identifier() const   { return _thread_id; }
#endif
#ifdef ASSERT
  // We expect no reposition failures so kill vm if we get one.
  //
  bool valid_reposition_failure() {
    return false;
  }
#endif // ASSERT
  tid_t kernel_thread_id() const {
    return _kernel_thread_id;
  }
  void set_kernel_thread_id(tid_t tid) {
    _kernel_thread_id = tid;
  }

  pthread_t pthread_id() const {
    // Here: same as OSThread::thread_id()
    return _thread_id;
  }

  // ***************************************************************
  // suspension support.
  // ***************************************************************

 public:
  // flags that support signal based suspend/resume on Aix are in a
  // separate class to avoid confusion with many flags in OSThread that
  // are used by VM level suspend/resume.
  os::SuspendResume sr;

  // _ucontext and _siginfo are used by SR_handler() to save thread context,
  // and they will later be used to walk the stack or reposition thread PC.
  // If the thread is not suspended in SR_handler() (e.g. self suspend),
  // the value in _ucontext is meaningless, so we must use the last Java
  // frame information as the frame. This will mean that for threads
  // that are parked on a mutex the profiler (and safepoint mechanism)
  // will see the thread as if it were still in the Java frame. This
  // not a problem for the profiler since the Java frame is a close
  // enough result. For the safepoint mechanism when the give it the
  // Java frame we are not at a point where the safepoint needs the
  // frame to that accurate (like for a compiled safepoint) since we
  // should be in a place where we are native and will block ourselves
  // if we transition.
 private:
  void* _siginfo;
  ucontext_t* _ucontext;
  int _expanding_stack;                 // non zero if manually expanding stack
  address _alt_sig_stack;               // address of base of alternate signal stack

 public:
  void* siginfo() const                   { return _siginfo;  }
  void set_siginfo(void* ptr)             { _siginfo = ptr;   }
  ucontext_t* ucontext() const            { return _ucontext; }
  void set_ucontext(ucontext_t* ptr)      { _ucontext = ptr;  }
  void set_expanding_stack(void)          { _expanding_stack = 1;  }
  void clear_expanding_stack(void)        { _expanding_stack = 0;  }
  int  expanding_stack(void)              { return _expanding_stack;  }

  void set_alt_sig_stack(address val)     { _alt_sig_stack = val; }
  address alt_sig_stack(void)             { return _alt_sig_stack; }

 private:
  Monitor* _startThread_lock;     // sync parent and child in thread creation

 public:

  Monitor* startThread_lock() const {
    return _startThread_lock;
  }

  // ***************************************************************
  // Platform dependent initialization and cleanup
  // ***************************************************************

 private:

  void pd_initialize();
  void pd_destroy();

 public:

  // The last measured values of cpu timing to prevent the "stale
  // value return" bug in thread_cpu_time.
  volatile struct {
    jlong sys;
    jlong user;
  } _last_cpu_times;

#endif // OS_AIX_OSTHREAD_AIX_HPP
