/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "code/scopeDesc.hpp"
#include "memory/allocation.hpp"
#include "prims/jvmtiDeferredUpdates.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/escapeBarrier.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.hpp"
#include "runtime/handshake.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/keepStackGCProcessed.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/registerMap.hpp"
#include "runtime/stackFrameStream.inline.hpp"
#include "runtime/stackValue.hpp"
#include "runtime/stackValueCollection.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/vframe.hpp"
#include "runtime/vframe_hp.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

#if COMPILER2_OR_JVMCI

// Returns true iff objects were reallocated and relocked because of access through JVMTI
bool EscapeBarrier::objs_are_deoptimized(JavaThread* thread, intptr_t* fr_id) {
  // first/oldest update holds the flag
  GrowableArray<jvmtiDeferredLocalVariableSet*>* list = JvmtiDeferredUpdates::deferred_locals(thread);
  bool result = false;
  if (list != NULL) {
    for (int i = 0; i < list->length(); i++) {
      if (list->at(i)->matches(fr_id)) {
        result = list->at(i)->objects_are_deoptimized();
        break;
      }
    }
  }
  return result;
}

// Deoptimize objects of frames of the target thread at depth >= d1 and depth <= d2.
// Deoptimize objects of caller frames if they passed references to ArgEscape objects as arguments.
// Return false in the case of a reallocation failure and true otherwise.
bool EscapeBarrier::deoptimize_objects(int d1, int d2) {
  if (!barrier_active()) return true;
  if (d1 < deoptee_thread()->frames_to_pop_failed_realloc()) {
    // The deoptee thread has frames with reallocation failures on top of its stack.
    // These frames are about to be removed. We must not interfere with that and signal failure.
    return false;
  }
  if (deoptee_thread()->has_last_Java_frame()) {
    assert(calling_thread() == Thread::current(), "should be");
    KeepStackGCProcessedMark ksgcpm(deoptee_thread());
    ResourceMark rm(calling_thread());
    HandleMark   hm(calling_thread());
    RegisterMap  reg_map(deoptee_thread(), false /* update_map */, false /* process_frames */);
    vframe* vf = deoptee_thread()->last_java_vframe(&reg_map);
    int cur_depth = 0;

    // Skip frames at depth < d1
    while (vf != NULL && cur_depth < d1) {
      cur_depth++;
      vf = vf->sender();
    }

    while (vf != NULL && ((cur_depth <= d2) || !vf->is_entry_frame())) {
      if (vf->is_compiled_frame()) {
        compiledVFrame* cvf = compiledVFrame::cast(vf);
        // Deoptimize frame and local objects if any exist.
        // If cvf is deeper than depth, then we deoptimize iff local objects are passed as args.
        bool should_deopt = cur_depth <= d2 ? cvf->has_ea_local_in_scope() : cvf->arg_escape();
        if (should_deopt && !deoptimize_objects(cvf->fr().id())) {
          // reallocation of scalar replaced objects failed because heap is exhausted
          return false;
        }

        // move to top frame
        while(!vf->is_top()) {
          cur_depth++;
          vf = vf->sender();
        }
      }

      // move to next physical frame
      cur_depth++;
      vf = vf->sender();
    }
  }
  return true;
}

bool EscapeBarrier::deoptimize_objects_all_threads() {
  if (!barrier_active()) return true;
  ResourceMark rm(calling_thread());
  for (JavaThreadIteratorWithHandle jtiwh; JavaThread *jt = jtiwh.next(); ) {
    if (jt->frames_to_pop_failed_realloc() > 0) {
      // The deoptee thread jt has frames with reallocation failures on top of its stack.
      // These frames are about to be removed. We must not interfere with that and signal failure.
      return false;
    }
    if (jt->has_last_Java_frame()) {
      KeepStackGCProcessedMark ksgcpm(jt);
      RegisterMap reg_map(jt, false /* update_map */, false /* process_frames */);
      vframe* vf = jt->last_java_vframe(&reg_map);
      assert(jt->frame_anchor()->walkable(),
             "The stack of JavaThread " PTR_FORMAT " is not walkable. Thread state is %d",
             p2i(jt), jt->thread_state());
      while (vf != NULL) {
        if (vf->is_compiled_frame()) {
          compiledVFrame* cvf = compiledVFrame::cast(vf);
          if ((cvf->has_ea_local_in_scope() || cvf->arg_escape()) &&
              !deoptimize_objects_internal(jt, cvf->fr().id())) {
            return false; // reallocation failure
          }
          // move to top frame
          while(!vf->is_top()) {
            vf = vf->sender();
          }
        }
        // move to next physical frame
        vf = vf->sender();
      }
    }
  }
  return true; // success
}

bool EscapeBarrier::_deoptimizing_objects_for_all_threads = false;
bool EscapeBarrier::_self_deoptimization_in_progress      = false;

class EscapeBarrierSuspendHandshake : public HandshakeClosure {
 public:
  EscapeBarrierSuspendHandshake(const char* name) :
    HandshakeClosure(name) { }
  void do_thread(Thread* th) { }
};

void EscapeBarrier::sync_and_suspend_one() {
  assert(_calling_thread != NULL, "calling thread must not be NULL");
  assert(_deoptee_thread != NULL, "deoptee thread must not be NULL");
  assert(barrier_active(), "should not call");

  // Sync with other threads that might be doing deoptimizations
  {
    // Need to switch to _thread_blocked for the wait() call
    ThreadBlockInVM tbivm(_calling_thread);
    MonitorLocker ml(_calling_thread, EscapeBarrier_lock, Mutex::_no_safepoint_check_flag);
    while (_self_deoptimization_in_progress || _deoptee_thread->is_obj_deopt_suspend()) {
      ml.wait();
    }

    if (self_deopt()) {
      _self_deoptimization_in_progress = true;
      return;
    }

    // set suspend flag for target thread
    _deoptee_thread->set_obj_deopt_flag();
  }

  // Use a handshake to synchronize with the target thread.
  EscapeBarrierSuspendHandshake sh("EscapeBarrierSuspendOne");
  Handshake::execute(&sh, _deoptee_thread);
  assert(!_deoptee_thread->has_last_Java_frame() || _deoptee_thread->frame_anchor()->walkable(),
         "stack should be walkable now");
}

void EscapeBarrier::sync_and_suspend_all() {
  assert(barrier_active(), "should not call");
  assert(_calling_thread != NULL, "calling thread must not be NULL");
  assert(all_threads(), "sanity");

  // Sync with other threads that might be doing deoptimizations
  {
    // Need to switch to _thread_blocked for the wait() call
    ThreadBlockInVM tbivm(_calling_thread);
    MonitorLocker ml(_calling_thread, EscapeBarrier_lock, Mutex::_no_safepoint_check_flag);

    bool deopt_in_progress;
    do {
      deopt_in_progress = _self_deoptimization_in_progress;
      for (JavaThreadIteratorWithHandle jtiwh; JavaThread *jt = jtiwh.next(); ) {
        deopt_in_progress = (deopt_in_progress || jt->is_obj_deopt_suspend());
        if (deopt_in_progress) {
          break;
        }
      }
      if (deopt_in_progress) {
        ml.wait(); // then check again
      }
    } while(deopt_in_progress);

    _self_deoptimization_in_progress = true;
    _deoptimizing_objects_for_all_threads = true;

    // We set the suspend flags before executing the handshake because then the
    // setting will be visible after leaving the _thread_blocked state in
    // JavaThread::wait_for_object_deoptimization(). If we set the flags in the
    // handshake then the read must happen after the safepoint/handshake poll.
    for (JavaThreadIteratorWithHandle jtiwh; JavaThread *jt = jtiwh.next(); ) {
      if (jt->is_Java_thread() && !jt->is_hidden_from_external_view() && (jt != _calling_thread)) {
        jt->set_obj_deopt_flag();
      }
    }
  }

  // Use a handshake to synchronize with the other threads.
  EscapeBarrierSuspendHandshake sh("EscapeBarrierSuspendAll");
  Handshake::execute(&sh);
#ifdef ASSERT
  for (JavaThreadIteratorWithHandle jtiwh; JavaThread *jt = jtiwh.next(); ) {
    if (jt->is_hidden_from_external_view()) continue;
    assert(!jt->has_last_Java_frame() || jt->frame_anchor()->walkable(),
           "The stack of JavaThread " PTR_FORMAT " is not walkable. Thread state is %d",
           p2i(jt), jt->thread_state());
  }
#endif // ASSERT
}

void EscapeBarrier::resume_one() {
  assert(barrier_active(), "should not call");
  assert(!all_threads(), "use resume_all()");
  MonitorLocker ml(_calling_thread, EscapeBarrier_lock, Mutex::_no_safepoint_check_flag);
  if (self_deopt()) {
    assert(_self_deoptimization_in_progress, "incorrect synchronization");
    _self_deoptimization_in_progress = false;
  } else {
    _deoptee_thread->clear_obj_deopt_flag();
  }
  ml.notify_all();
}

void EscapeBarrier::resume_all() {
  assert(barrier_active(), "should not call");
  assert(all_threads(), "use resume_one()");
  MonitorLocker ml(_calling_thread, EscapeBarrier_lock, Mutex::_no_safepoint_check_flag);
  assert(_self_deoptimization_in_progress, "incorrect synchronization");
  _deoptimizing_objects_for_all_threads = false;
  _self_deoptimization_in_progress = false;
  for (JavaThreadIteratorWithHandle jtiwh; JavaThread *jt = jtiwh.next(); ) {
    jt->clear_obj_deopt_flag();
  }
  ml.notify_all();
}

void EscapeBarrier::thread_added(JavaThread* jt) {
  if (!jt->is_hidden_from_external_view()) {
    MutexLocker ml(EscapeBarrier_lock, Mutex::_no_safepoint_check_flag);
    if (_deoptimizing_objects_for_all_threads) {
      jt->set_obj_deopt_flag();
    }
  }
}

void EscapeBarrier::thread_removed(JavaThread* jt) {
  MonitorLocker ml(EscapeBarrier_lock, Mutex::_no_safepoint_check_flag);
  if (jt->is_obj_deopt_suspend()) {
    // jt terminated before it self suspended.
    // Other threads might be waiting to perform deoptimizations for it.
    jt->clear_obj_deopt_flag();
    ml.notify_all();
  }
}

// Remember that objects were reallocated and relocked for the compiled frame with the given id
static void set_objs_are_deoptimized(JavaThread* thread, intptr_t* fr_id) {
  // set in first/oldest update
  GrowableArray<jvmtiDeferredLocalVariableSet*>* list =
    JvmtiDeferredUpdates::deferred_locals(thread);
  DEBUG_ONLY(bool found = false);
  if (list != NULL) {
    for (int i = 0; i < list->length(); i++) {
      if (list->at(i)->matches(fr_id)) {
        DEBUG_ONLY(found = true);
        list->at(i)->set_objs_are_deoptimized();
        break;
      }
    }
  }
  assert(found, "variable set should exist at least for one vframe");
}

// Deoptimize the given frame and deoptimize objects with optimizations based on
// escape analysis, i.e. reallocate scalar replaced objects on the heap and
// relock objects if locking has been eliminated.
// Deoptimized objects are kept as JVMTI deferred updates until the compiled
// frame is replaced with interpreter frames.  Returns false iff at least one
// reallocation failed.
bool EscapeBarrier::deoptimize_objects_internal(JavaThread* deoptee, intptr_t* fr_id) {
  assert(barrier_active(), "should not call");

  JavaThread* ct = calling_thread();
  bool realloc_failures = false;

  if (!objs_are_deoptimized(deoptee, fr_id)) {
    // Make sure the frame identified by fr_id is deoptimized and fetch its last vframe
    compiledVFrame* last_cvf;
    bool fr_is_deoptimized;
    do {
      StackFrameStream fst(deoptee, true /* update */, false /* process_frames */);
      while (fst.current()->id() != fr_id && !fst.is_done()) {
        fst.next();
      }
      assert(fst.current()->id() == fr_id, "frame not found");
      assert(fst.current()->is_compiled_frame(),
             "only compiled frames can contain stack allocated objects");
      fr_is_deoptimized = fst.current()->is_deoptimized_frame();
      if (!fr_is_deoptimized) {
        // Execution must not continue in the compiled method, so we deoptimize the frame.
        Deoptimization::deoptimize_frame(deoptee, fr_id);
      } else {
        last_cvf = compiledVFrame::cast(vframe::new_vframe(fst.current(), fst.register_map(), deoptee));
      }
    } while(!fr_is_deoptimized);

    // collect inlined frames
    compiledVFrame* cvf = last_cvf;
    GrowableArray<compiledVFrame*>* vfs = new GrowableArray<compiledVFrame*>(10);
    while (!cvf->is_top()) {
      vfs->push(cvf);
      cvf = compiledVFrame::cast(cvf->sender());
    }
    vfs->push(cvf);

    // reallocate and relock optimized objects
    bool deoptimized_objects = Deoptimization::deoptimize_objects_internal(ct, vfs, realloc_failures);
    if (!realloc_failures && deoptimized_objects) {
      // now do the updates
      for (int frame_index = 0; frame_index < vfs->length(); frame_index++) {
        cvf = vfs->at(frame_index);
        cvf->create_deferred_updates_after_object_deoptimization();
      }
      set_objs_are_deoptimized(deoptee, fr_id);
    }
  }
  return !realloc_failures;
}

#endif // COMPILER2_OR_JVMCI
