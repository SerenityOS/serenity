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

#ifndef SHARE_RUNTIME_PARK_HPP
#define SHARE_RUNTIME_PARK_HPP

#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

/*
 * Per-thread blocking support for JSR166. See the Java-level
 * documentation for rationale. Basically, park acts like wait, unpark
 * like notify.
 *
 * Parkers are inherently part of their associated JavaThread and are only
 * accessed when the JavaThread is guaranteed to be alive (e.g. by operating
 * on the current thread, or by having the thread protected by a
 * ThreadsListHandle.
 *
 * Class Parker is declared in shared code and extends the platform-specific
 * os::PlatformParker class, which contains the actual implementation
 * mechanics (condvars/events etc). The implementation for park() and unpark()
 * are also in the platform-specific os_<os>.cpp files.
 *
 * In the future we'll want to think about eliminating Parker and using
 * ParkEvent instead.  There's considerable duplication between the two
 * services.
 *
 */

class Parker : public os::PlatformParker {
 private:
  NONCOPYABLE(Parker);
 public:
  Parker() : PlatformParker() {}

  // For simplicity of interface with Java, all forms of park (indefinite,
  // relative, and absolute) are multiplexed into one call.
  void park(bool isAbsolute, jlong time);
  void unpark();
};

/////////////////////////////////////////////////////////////
//
// ParkEvents are type-stable and immortal.
//
// Lifecycle: Once a ParkEvent is associated with a thread that ParkEvent remains
// associated with the thread for the thread's entire lifetime - the relationship is
// stable. A thread will be associated at most one ParkEvent.  When the thread
// expires, the ParkEvent moves to the EventFreeList.  New threads attempt to allocate from
// the EventFreeList before creating a new Event.  Type-stability frees us from
// worrying about stale Event or Thread references in the objectMonitor subsystem.
// (A reference to ParkEvent is always valid, even though the event may no longer be associated
// with the desired or expected thread.  A key aspect of this design is that the callers of
// park, unpark, etc must tolerate stale references and spurious wakeups).
//
// Only the "associated" thread can block (park) on the ParkEvent, although
// any other thread can unpark a reachable parkevent.  Park() is allowed to
// return spuriously.  In fact park-unpark a really just an optimization to
// avoid unbounded spinning and surrender the CPU to be a polite system citizen.
// A degenerate albeit "impolite" park-unpark implementation could simply return.
// See http://blogs.sun.com/dave for more details.
//
// Eventually I'd like to eliminate Events and ObjectWaiters, both of which serve as
// thread proxies, and simply make the THREAD structure type-stable and persistent.
// Currently, we unpark events associated with threads, but ideally we'd just
// unpark threads.
//
// The base-class, PlatformEvent, is platform-specific while the ParkEvent is
// platform-independent.  PlatformEvent provides park(), unpark(), etc., and
// is abstract -- that is, a PlatformEvent should never be instantiated except
// as part of a ParkEvent.
// Equivalently we could have defined a platform-independent base-class that
// exported Allocate(), Release(), etc.  The platform-specific class would extend
// that base-class, adding park(), unpark(), etc.
//
// A word of caution: The JVM uses 2 very similar constructs:
// 1. ParkEvent are used for Java-level "monitor" synchronization.
// 2. Parkers are used by JSR166-JUC park-unpark.
//
// We'll want to eventually merge these redundant facilities and use ParkEvent.


class ParkEvent : public os::PlatformEvent {
  private:
    ParkEvent * FreeNext ;

    // Current association
    Thread * AssociatedWith ;

  public:
    // MCS-CLH list linkage and Native Mutex/Monitor
    ParkEvent * volatile ListNext ;
    volatile int TState ;
    volatile int Notified ;             // for native monitor construct

  private:
    static ParkEvent * volatile FreeList ;
    static volatile int ListLock ;

    // It's prudent to mark the dtor as "private"
    // ensuring that it's not visible outside the package.
    // Unfortunately gcc warns about such usage, so
    // we revert to the less desirable "protected" visibility.
    // The other compilers accept private dtors.

  protected:        // Ensure dtor is never invoked
    ~ParkEvent() { guarantee (0, "invariant") ; }

    ParkEvent() : PlatformEvent() {
       AssociatedWith = NULL ;
       FreeNext       = NULL ;
       ListNext       = NULL ;
       TState         = 0 ;
       Notified       = 0 ;
    }

    // We use placement-new to force ParkEvent instances to be
    // aligned on 256-byte address boundaries.  This ensures that the least
    // significant byte of a ParkEvent address is always 0.

    void * operator new (size_t sz) throw();
    void operator delete (void * a) ;

  public:
    static ParkEvent * Allocate (Thread * t) ;
    static void Release (ParkEvent * e) ;
} ;

#endif // SHARE_RUNTIME_PARK_HPP
