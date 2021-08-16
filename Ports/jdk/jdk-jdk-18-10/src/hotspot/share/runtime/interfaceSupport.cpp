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

#include "precompiled.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "runtime/atomic.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/init.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/stackFrameStream.inline.hpp"
#include "runtime/vframe.hpp"
#include "runtime/vmOperations.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/preserveException.hpp"

// Implementation of InterfaceSupport

#ifdef ASSERT
VMEntryWrapper::VMEntryWrapper() {
  if (VerifyLastFrame) {
    InterfaceSupport::verify_last_frame();
  }
}

VMEntryWrapper::~VMEntryWrapper() {
  InterfaceSupport::check_gc_alot();
  if (WalkStackALot) {
    InterfaceSupport::walk_stack();
  }
  if (DeoptimizeALot || DeoptimizeRandom) {
    InterfaceSupport::deoptimizeAll();
  }
  if (ZombieALot) {
    InterfaceSupport::zombieAll();
  }
  // do verification AFTER potential deoptimization
  if (VerifyStack) {
    InterfaceSupport::verify_stack();
  }
}

VMNativeEntryWrapper::VMNativeEntryWrapper() {
  if (GCALotAtAllSafepoints) InterfaceSupport::check_gc_alot();
}

VMNativeEntryWrapper::~VMNativeEntryWrapper() {
  if (GCALotAtAllSafepoints) InterfaceSupport::check_gc_alot();
}

unsigned int InterfaceSupport::_scavenge_alot_counter = 1;
unsigned int InterfaceSupport::_fullgc_alot_counter   = 1;
int InterfaceSupport::_fullgc_alot_invocation = 0;

void InterfaceSupport::gc_alot() {
  Thread *thread = Thread::current();
  if (!thread->is_Java_thread()) return; // Avoid concurrent calls
  // Check for new, not quite initialized thread. A thread in new mode cannot initiate a GC.
  JavaThread *current_thread = JavaThread::cast(thread);
  if (current_thread->active_handles() == NULL) return;

  // Short-circuit any possible re-entrant gc-a-lot attempt
  if (thread->skip_gcalot()) return;

  if (Threads::is_vm_complete()) {

    if (++_fullgc_alot_invocation < FullGCALotStart) {
      return;
    }

    // Use this line if you want to block at a specific point,
    // e.g. one number_of_calls/scavenge/gc before you got into problems
    if (FullGCALot) _fullgc_alot_counter--;

    // Check if we should force a full gc
    if (_fullgc_alot_counter == 0) {
      // Release dummy so objects are forced to move
      if (!Universe::release_fullgc_alot_dummy()) {
        warning("FullGCALot: Unable to release more dummies at bottom of heap");
      }
      HandleMark hm(thread);
      Universe::heap()->collect(GCCause::_full_gc_alot);
      unsigned int invocations = Universe::heap()->total_full_collections();
      // Compute new interval
      if (FullGCALotInterval > 1) {
        _fullgc_alot_counter = 1+(unsigned int)((double)FullGCALotInterval*os::random()/(max_jint+1.0));
        log_trace(gc)("Full gc no: %u\tInterval: %u", invocations, _fullgc_alot_counter);
      } else {
        _fullgc_alot_counter = 1;
      }
      // Print progress message
      if (invocations % 100 == 0) {
        log_trace(gc)("Full gc no: %u", invocations);
      }
    } else {
      if (ScavengeALot) _scavenge_alot_counter--;
      // Check if we should force a scavenge
      if (_scavenge_alot_counter == 0) {
        HandleMark hm(thread);
        Universe::heap()->collect(GCCause::_scavenge_alot);
        unsigned int invocations = Universe::heap()->total_collections() - Universe::heap()->total_full_collections();
        // Compute new interval
        if (ScavengeALotInterval > 1) {
          _scavenge_alot_counter = 1+(unsigned int)((double)ScavengeALotInterval*os::random()/(max_jint+1.0));
          log_trace(gc)("Scavenge no: %u\tInterval: %u", invocations, _scavenge_alot_counter);
        } else {
          _scavenge_alot_counter = 1;
        }
        // Print progress message
        if (invocations % 1000 == 0) {
          log_trace(gc)("Scavenge no: %u", invocations);
        }
      }
    }
  }
}


vframe* vframe_array[50];
int walk_stack_counter = 0;

void InterfaceSupport::walk_stack_from(vframe* start_vf) {
  // walk
  int i = 0;
  for (vframe* f = start_vf; f; f = f->sender() ) {
    if (i < 50) vframe_array[i++] = f;
  }
}


void InterfaceSupport::walk_stack() {
  JavaThread* thread = JavaThread::current();
  walk_stack_counter++;
  if (!thread->has_last_Java_frame()) return;
  ResourceMark rm(thread);
  RegisterMap reg_map(thread);
  walk_stack_from(thread->last_java_vframe(&reg_map));
}

// invocation counter for InterfaceSupport::deoptimizeAll/zombieAll functions
int deoptimizeAllCounter = 0;
int zombieAllCounter = 0;

void InterfaceSupport::zombieAll() {
  // This method is called by all threads when a thread make
  // transition to VM state (for example, runtime calls).
  // Divide number of calls by number of threads to avoid
  // dependence of ZombieAll events frequency on number of threads.
  int value = zombieAllCounter / Threads::number_of_threads();
  if (is_init_completed() && value > ZombieALotInterval) {
    zombieAllCounter = 0;
    VM_ZombieAll op;
    VMThread::execute(&op);
  }
  zombieAllCounter++;
}

void InterfaceSupport::deoptimizeAll() {
  // This method is called by all threads when a thread make
  // transition to VM state (for example, runtime calls).
  // Divide number of calls by number of threads to avoid
  // dependence of DeoptimizeAll events frequency on number of threads.
  int value = deoptimizeAllCounter / Threads::number_of_threads();
  if (is_init_completed()) {
    if (DeoptimizeALot && value > DeoptimizeALotInterval) {
      deoptimizeAllCounter = 0;
      VM_DeoptimizeAll op;
      VMThread::execute(&op);
    } else if (DeoptimizeRandom && (value & 0x1F) == (os::random() & 0x1F)) {
      VM_DeoptimizeAll op;
      VMThread::execute(&op);
    }
  }
  deoptimizeAllCounter++;
}


void InterfaceSupport::verify_stack() {
  JavaThread* thread = JavaThread::current();
  ResourceMark rm(thread);
  // disabled because it throws warnings that oop maps should only be accessed
  // in VM thread or during debugging

  if (!thread->has_pending_exception()) {
    // verification does not work if there are pending exceptions
    StackFrameStream sfs(thread, true /* update */, true /* process_frames */);
    CodeBlob* cb = sfs.current()->cb();
      // In case of exceptions we might not have a runtime_stub on
      // top of stack, hence, all callee-saved registers are not going
      // to be setup correctly, hence, we cannot do stack verify
    if (cb != NULL && !(cb->is_runtime_stub() || cb->is_uncommon_trap_stub())) return;

    for (; !sfs.is_done(); sfs.next()) {
      sfs.current()->verify(sfs.register_map());
    }
  }
}


void InterfaceSupport::verify_last_frame() {
  JavaThread* thread = JavaThread::current();
  ResourceMark rm(thread);
  RegisterMap reg_map(thread);
  frame fr = thread->last_frame();
  fr.verify(&reg_map);
}


#endif // ASSERT


void InterfaceSupport_init() {
#ifdef ASSERT
  if (ScavengeALot || FullGCALot) {
    srand(ScavengeALotInterval * FullGCALotInterval);
  }
#endif
}
