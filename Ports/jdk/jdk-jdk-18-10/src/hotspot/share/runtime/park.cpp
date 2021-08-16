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
#include "memory/allocation.inline.hpp"
#include "runtime/thread.hpp"
#include "services/memTracker.hpp"

// Lifecycle management for TSM ParkEvents.
// ParkEvents are type-stable (TSM).
// In our particular implementation they happen to be immortal.
//
// We manage concurrency on the FreeList with a CAS-based
// detach-modify-reattach idiom that avoids the ABA problems
// that would otherwise be present in a simple CAS-based
// push-pop implementation.   (push-one and pop-all)
//
// Caveat: Allocate() and Release() may be called from threads
// other than the thread associated with the Event!
// If we need to call Allocate() when running as the thread in
// question then look for the PD calls to initialize native TLS.
// Native TLS (Win32/Linux/Solaris) can only be initialized or
// accessed by the associated thread.
// See also pd_initialize().
//
// Note that we could defer associating a ParkEvent with a thread
// until the 1st time the thread calls park().  unpark() calls to
// an unprovisioned thread would be ignored.  The first park() call
// for a thread would allocate and associate a ParkEvent and return
// immediately.

volatile int ParkEvent::ListLock = 0 ;
ParkEvent * volatile ParkEvent::FreeList = NULL ;

ParkEvent * ParkEvent::Allocate (Thread * t) {
  ParkEvent * ev ;

  // Start by trying to recycle an existing but unassociated
  // ParkEvent from the global free list.
  // Using a spin lock since we are part of the mutex impl.
  // 8028280: using concurrent free list without memory management can leak
  // pretty badly it turns out.
  Thread::SpinAcquire(&ListLock, "ParkEventFreeListAllocate");
  {
    ev = FreeList;
    if (ev != NULL) {
      FreeList = ev->FreeNext;
    }
  }
  Thread::SpinRelease(&ListLock);

  if (ev != NULL) {
    guarantee (ev->AssociatedWith == NULL, "invariant") ;
  } else {
    // Do this the hard way -- materialize a new ParkEvent.
    ev = new ParkEvent () ;
    guarantee ((intptr_t(ev) & 0xFF) == 0, "invariant") ;
  }
  ev->reset() ;                     // courtesy to caller
  ev->AssociatedWith = t ;          // Associate ev with t
  ev->FreeNext       = NULL ;
  return ev ;
}

void ParkEvent::Release (ParkEvent * ev) {
  if (ev == NULL) return ;
  guarantee (ev->FreeNext == NULL      , "invariant") ;
  ev->AssociatedWith = NULL ;
  // Note that if we didn't have the TSM/immortal constraint, then
  // when reattaching we could trim the list.
  Thread::SpinAcquire(&ListLock, "ParkEventFreeListRelease");
  {
    ev->FreeNext = FreeList;
    FreeList = ev;
  }
  Thread::SpinRelease(&ListLock);
}

// Override operator new and delete so we can ensure that the
// least significant byte of ParkEvent addresses is 0.
// Beware that excessive address alignment is undesirable
// as it can result in D$ index usage imbalance as
// well as bank access imbalance on Niagara-like platforms,
// although Niagara's hash function should help.

void * ParkEvent::operator new (size_t sz) throw() {
  return (void *) ((intptr_t (AllocateHeap(sz + 256, mtInternal, CALLER_PC)) + 256) & -256) ;
}

void ParkEvent::operator delete (void * a) {
  // ParkEvents are type-stable and immortal ...
  ShouldNotReachHere();
}
