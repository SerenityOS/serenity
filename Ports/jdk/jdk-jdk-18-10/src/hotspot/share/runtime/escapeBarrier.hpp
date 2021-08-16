/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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

#ifndef SHARE_RUNTIME_ESCAPEBARRIER_HPP
#define SHARE_RUNTIME_ESCAPEBARRIER_HPP

#include "compiler/compiler_globals.hpp"
#include "memory/allocation.hpp"
#include "utilities/macros.hpp"

class JavaThread;

// EscapeBarriers should be put on execution paths where JVMTI agents can access object
// references held by java threads.
// They provide means to revert optimizations based on escape analysis in a well synchronized manner
// just before local references escape through JVMTI.

class EscapeBarrier : StackObj {

#if COMPILER2_OR_JVMCI
  JavaThread* const _calling_thread;
  JavaThread* const _deoptee_thread;
  bool        const _barrier_active;

  static bool _deoptimizing_objects_for_all_threads;
  static bool _self_deoptimization_in_progress;

  // Suspending is necessary because the target thread's stack must be walked and
  // object reallocation is not possible in a handshake or at a safepoint.
  // Suspending is based on handshakes. It is sufficient if the target thread(s)
  // cannot return to executing bytecodes. Acquiring a lock is ok. Leaving a
  // safepoint/handshake safe state is not ok.
  // See also JavaThread::wait_for_object_deoptimization().
  void sync_and_suspend_one();
  void sync_and_suspend_all();
  void resume_one();
  void resume_all();

  // Deoptimize the given frame and deoptimize objects with optimizations based on escape analysis.
  bool deoptimize_objects_internal(JavaThread* deoptee, intptr_t* fr_id);

  // Deoptimize objects, i.e. reallocate and relock them. The target frames are deoptimized.
  // The methods return false iff at least one reallocation failed.
  bool deoptimize_objects(intptr_t* fr_id) {
    return deoptimize_objects_internal(deoptee_thread(), fr_id);
  }

public:
  // Revert ea based optimizations for given deoptee thread
  EscapeBarrier(bool barrier_active, JavaThread* calling_thread, JavaThread* deoptee_thread)
    : _calling_thread(calling_thread), _deoptee_thread(deoptee_thread),
      _barrier_active(barrier_active && (JVMCI_ONLY(UseJVMCICompiler) NOT_JVMCI(false)
                      COMPILER2_PRESENT(|| DoEscapeAnalysis)))
  {
    if (_barrier_active) sync_and_suspend_one();
  }

  // Revert ea based optimizations for all java threads
  EscapeBarrier(bool barrier_active, JavaThread* calling_thread)
    : _calling_thread(calling_thread), _deoptee_thread(NULL),
      _barrier_active(barrier_active && (JVMCI_ONLY(UseJVMCICompiler) NOT_JVMCI(false)
                      COMPILER2_PRESENT(|| DoEscapeAnalysis)))
  {
    if (_barrier_active) sync_and_suspend_all();
  }

#else

public:
  EscapeBarrier(bool barrier_active, JavaThread* calling_thread, JavaThread* deoptee_thread) { }
  EscapeBarrier(bool barrier_active, JavaThread* calling_thread) { }
  static bool deoptimizing_objects_for_all_threads() { return false; }
  bool barrier_active() const                        { return false; }
#endif // COMPILER2_OR_JVMCI

  // Deoptimize objects of frames of the target thread up to the given depth.
  // Deoptimize objects of caller frames if they passed references to ArgEscape objects as arguments.
  // Return false in the case of a reallocation failure and true otherwise.
  bool deoptimize_objects(int depth) {
    return deoptimize_objects(0, depth);
  }

  // Deoptimize objects of frames of the target thread at depth >= d1 and depth <= d2.
  // Deoptimize objects of caller frames if they passed references to ArgEscape objects as arguments.
  // Return false in the case of a reallocation failure and true otherwise.
  bool deoptimize_objects(int d1, int d2)                      NOT_COMPILER2_OR_JVMCI_RETURN_(true);

  // Find and deoptimize non escaping objects and the holding frames on all stacks.
  bool deoptimize_objects_all_threads()                        NOT_COMPILER2_OR_JVMCI_RETURN_(true);

  // A java thread was added to the list of threads.
  static void thread_added(JavaThread* jt)                     NOT_COMPILER2_OR_JVMCI_RETURN;

  // A java thread was removed from the list of threads.
  static void thread_removed(JavaThread* jt)                   NOT_COMPILER2_OR_JVMCI_RETURN;

#if COMPILER2_OR_JVMCI
  // Returns true iff objects were reallocated and relocked because of access through JVMTI.
  static bool objs_are_deoptimized(JavaThread* thread, intptr_t* fr_id);

  static bool deoptimizing_objects_for_all_threads() { return _deoptimizing_objects_for_all_threads; }

  ~EscapeBarrier() {
    if (!barrier_active()) return;
    if (all_threads()) {
      resume_all();
    } else {
      resume_one();
    }
  }

  // Should revert optimizations for all threads.
  bool all_threads()    const { return _deoptee_thread == NULL; }

  // Current thread deoptimizes its own objects.
  bool self_deopt()     const { return _calling_thread == _deoptee_thread; }

  // Inactive barriers are created if no local objects can escape.
  bool barrier_active() const { return _barrier_active; }

  // accessors
  JavaThread* calling_thread() const     { return _calling_thread; }
  JavaThread* deoptee_thread() const     { return _deoptee_thread; }
#endif // COMPILER2_OR_JVMCI
};

#endif // SHARE_RUNTIME_ESCAPEBARRIER_HPP
