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

#include "precompiled.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/oopStorage.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "jfr/jfrEvents.hpp"
#include "jfr/support/jfrThreadId.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/markWord.hpp"
#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "oops/weakHandle.inline.hpp"
#include "prims/jvmtiDeferredUpdates.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/atomic.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/objectMonitor.inline.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/osThread.hpp"
#include "runtime/perfData.hpp"
#include "runtime/safefetch.inline.hpp"
#include "runtime/safepointMechanism.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/thread.inline.hpp"
#include "services/threadService.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/macros.hpp"
#include "utilities/preserveException.hpp"
#if INCLUDE_JFR
#include "jfr/support/jfrFlush.hpp"
#endif

#ifdef DTRACE_ENABLED

// Only bother with this argument setup if dtrace is available
// TODO-FIXME: probes should not fire when caller is _blocked.  assert() accordingly.


#define DTRACE_MONITOR_PROBE_COMMON(obj, thread)                           \
  char* bytes = NULL;                                                      \
  int len = 0;                                                             \
  jlong jtid = SharedRuntime::get_java_tid(thread);                        \
  Symbol* klassname = obj->klass()->name();                                \
  if (klassname != NULL) {                                                 \
    bytes = (char*)klassname->bytes();                                     \
    len = klassname->utf8_length();                                        \
  }

#define DTRACE_MONITOR_WAIT_PROBE(monitor, obj, thread, millis)            \
  {                                                                        \
    if (DTraceMonitorProbes) {                                             \
      DTRACE_MONITOR_PROBE_COMMON(obj, thread);                            \
      HOTSPOT_MONITOR_WAIT(jtid,                                           \
                           (monitor), bytes, len, (millis));               \
    }                                                                      \
  }

#define HOTSPOT_MONITOR_contended__enter HOTSPOT_MONITOR_CONTENDED_ENTER
#define HOTSPOT_MONITOR_contended__entered HOTSPOT_MONITOR_CONTENDED_ENTERED
#define HOTSPOT_MONITOR_contended__exit HOTSPOT_MONITOR_CONTENDED_EXIT
#define HOTSPOT_MONITOR_notify HOTSPOT_MONITOR_NOTIFY
#define HOTSPOT_MONITOR_notifyAll HOTSPOT_MONITOR_NOTIFYALL

#define DTRACE_MONITOR_PROBE(probe, monitor, obj, thread)                  \
  {                                                                        \
    if (DTraceMonitorProbes) {                                             \
      DTRACE_MONITOR_PROBE_COMMON(obj, thread);                            \
      HOTSPOT_MONITOR_##probe(jtid,                                        \
                              (uintptr_t)(monitor), bytes, len);           \
    }                                                                      \
  }

#else //  ndef DTRACE_ENABLED

#define DTRACE_MONITOR_WAIT_PROBE(obj, thread, millis, mon)    {;}
#define DTRACE_MONITOR_PROBE(probe, obj, thread, mon)          {;}

#endif // ndef DTRACE_ENABLED

// Tunables ...
// The knob* variables are effectively final.  Once set they should
// never be modified hence.  Consider using __read_mostly with GCC.

int ObjectMonitor::Knob_SpinLimit    = 5000;    // derived by an external tool -

static int Knob_Bonus               = 100;     // spin success bonus
static int Knob_BonusB              = 100;     // spin success bonus
static int Knob_Penalty             = 200;     // spin failure penalty
static int Knob_Poverty             = 1000;
static int Knob_FixedSpin           = 0;
static int Knob_PreSpin             = 10;      // 20-100 likely better

DEBUG_ONLY(static volatile bool InitDone = false;)

OopStorage* ObjectMonitor::_oop_storage = NULL;

// -----------------------------------------------------------------------------
// Theory of operations -- Monitors lists, thread residency, etc:
//
// * A thread acquires ownership of a monitor by successfully
//   CAS()ing the _owner field from null to non-null.
//
// * Invariant: A thread appears on at most one monitor list --
//   cxq, EntryList or WaitSet -- at any one time.
//
// * Contending threads "push" themselves onto the cxq with CAS
//   and then spin/park.
//
// * After a contending thread eventually acquires the lock it must
//   dequeue itself from either the EntryList or the cxq.
//
// * The exiting thread identifies and unparks an "heir presumptive"
//   tentative successor thread on the EntryList.  Critically, the
//   exiting thread doesn't unlink the successor thread from the EntryList.
//   After having been unparked, the wakee will recontend for ownership of
//   the monitor.   The successor (wakee) will either acquire the lock or
//   re-park itself.
//
//   Succession is provided for by a policy of competitive handoff.
//   The exiting thread does _not_ grant or pass ownership to the
//   successor thread.  (This is also referred to as "handoff" succession").
//   Instead the exiting thread releases ownership and possibly wakes
//   a successor, so the successor can (re)compete for ownership of the lock.
//   If the EntryList is empty but the cxq is populated the exiting
//   thread will drain the cxq into the EntryList.  It does so by
//   by detaching the cxq (installing null with CAS) and folding
//   the threads from the cxq into the EntryList.  The EntryList is
//   doubly linked, while the cxq is singly linked because of the
//   CAS-based "push" used to enqueue recently arrived threads (RATs).
//
// * Concurrency invariants:
//
//   -- only the monitor owner may access or mutate the EntryList.
//      The mutex property of the monitor itself protects the EntryList
//      from concurrent interference.
//   -- Only the monitor owner may detach the cxq.
//
// * The monitor entry list operations avoid locks, but strictly speaking
//   they're not lock-free.  Enter is lock-free, exit is not.
//   For a description of 'Methods and apparatus providing non-blocking access
//   to a resource,' see U.S. Pat. No. 7844973.
//
// * The cxq can have multiple concurrent "pushers" but only one concurrent
//   detaching thread.  This mechanism is immune from the ABA corruption.
//   More precisely, the CAS-based "push" onto cxq is ABA-oblivious.
//
// * Taken together, the cxq and the EntryList constitute or form a
//   single logical queue of threads stalled trying to acquire the lock.
//   We use two distinct lists to improve the odds of a constant-time
//   dequeue operation after acquisition (in the ::enter() epilogue) and
//   to reduce heat on the list ends.  (c.f. Michael Scott's "2Q" algorithm).
//   A key desideratum is to minimize queue & monitor metadata manipulation
//   that occurs while holding the monitor lock -- that is, we want to
//   minimize monitor lock holds times.  Note that even a small amount of
//   fixed spinning will greatly reduce the # of enqueue-dequeue operations
//   on EntryList|cxq.  That is, spinning relieves contention on the "inner"
//   locks and monitor metadata.
//
//   Cxq points to the set of Recently Arrived Threads attempting entry.
//   Because we push threads onto _cxq with CAS, the RATs must take the form of
//   a singly-linked LIFO.  We drain _cxq into EntryList  at unlock-time when
//   the unlocking thread notices that EntryList is null but _cxq is != null.
//
//   The EntryList is ordered by the prevailing queue discipline and
//   can be organized in any convenient fashion, such as a doubly-linked list or
//   a circular doubly-linked list.  Critically, we want insert and delete operations
//   to operate in constant-time.  If we need a priority queue then something akin
//   to Solaris' sleepq would work nicely.  Viz.,
//   http://agg.eng/ws/on10_nightly/source/usr/src/uts/common/os/sleepq.c.
//   Queue discipline is enforced at ::exit() time, when the unlocking thread
//   drains the cxq into the EntryList, and orders or reorders the threads on the
//   EntryList accordingly.
//
//   Barring "lock barging", this mechanism provides fair cyclic ordering,
//   somewhat similar to an elevator-scan.
//
// * The monitor synchronization subsystem avoids the use of native
//   synchronization primitives except for the narrow platform-specific
//   park-unpark abstraction.  See the comments in os_solaris.cpp regarding
//   the semantics of park-unpark.  Put another way, this monitor implementation
//   depends only on atomic operations and park-unpark.  The monitor subsystem
//   manages all RUNNING->BLOCKED and BLOCKED->READY transitions while the
//   underlying OS manages the READY<->RUN transitions.
//
// * Waiting threads reside on the WaitSet list -- wait() puts
//   the caller onto the WaitSet.
//
// * notify() or notifyAll() simply transfers threads from the WaitSet to
//   either the EntryList or cxq.  Subsequent exit() operations will
//   unpark the notifyee.  Unparking a notifee in notify() is inefficient -
//   it's likely the notifyee would simply impale itself on the lock held
//   by the notifier.
//
// * An interesting alternative is to encode cxq as (List,LockByte) where
//   the LockByte is 0 iff the monitor is owned.  _owner is simply an auxiliary
//   variable, like _recursions, in the scheme.  The threads or Events that form
//   the list would have to be aligned in 256-byte addresses.  A thread would
//   try to acquire the lock or enqueue itself with CAS, but exiting threads
//   could use a 1-0 protocol and simply STB to set the LockByte to 0.
//   Note that is is *not* word-tearing, but it does presume that full-word
//   CAS operations are coherent with intermix with STB operations.  That's true
//   on most common processors.
//
// * See also http://blogs.sun.com/dave


void* ObjectMonitor::operator new (size_t size) throw() {
  return AllocateHeap(size, mtInternal);
}
void* ObjectMonitor::operator new[] (size_t size) throw() {
  return operator new (size);
}
void ObjectMonitor::operator delete(void* p) {
  FreeHeap(p);
}
void ObjectMonitor::operator delete[] (void *p) {
  operator delete(p);
}

// Check that object() and set_object() are called from the right context:
static void check_object_context() {
#ifdef ASSERT
  Thread* self = Thread::current();
  if (self->is_Java_thread()) {
    // Mostly called from JavaThreads so sanity check the thread state.
    JavaThread* jt = JavaThread::cast(self);
    switch (jt->thread_state()) {
    case _thread_in_vm:    // the usual case
    case _thread_in_Java:  // during deopt
      break;
    default:
      fatal("called from an unsafe thread state");
    }
    assert(jt->is_active_Java_thread(), "must be active JavaThread");
  } else {
    // However, ThreadService::get_current_contended_monitor()
    // can call here via the VMThread so sanity check it.
    assert(self->is_VM_thread(), "must be");
  }
#endif // ASSERT
}

ObjectMonitor::ObjectMonitor(oop object) :
  _header(markWord::zero()),
  _object(_oop_storage, object),
  _owner(NULL),
  _previous_owner_tid(0),
  _next_om(NULL),
  _recursions(0),
  _EntryList(NULL),
  _cxq(NULL),
  _succ(NULL),
  _Responsible(NULL),
  _Spinner(0),
  _SpinDuration(ObjectMonitor::Knob_SpinLimit),
  _contentions(0),
  _WaitSet(NULL),
  _waiters(0),
  _WaitSetLock(0)
{ }

ObjectMonitor::~ObjectMonitor() {
  _object.release(_oop_storage);
}

oop ObjectMonitor::object() const {
  check_object_context();
  if (_object.is_null()) {
    return NULL;
  }
  return _object.resolve();
}

oop ObjectMonitor::object_peek() const {
  if (_object.is_null()) {
    return NULL;
  }
  return _object.peek();
}

void ObjectMonitor::ExitOnSuspend::operator()(JavaThread* current) {
  if (current->is_suspended()) {
    _om->_recursions = 0;
    _om->_succ = NULL;
    // Don't need a full fence after clearing successor here because of the call to exit().
    _om->exit(current, false /* not_suspended */);
    _om_exited = true;

    current->set_current_pending_monitor(_om);
  }
}

void ObjectMonitor::ClearSuccOnSuspend::operator()(JavaThread* current) {
  if (current->is_suspended()) {
    if (_om->_succ == current) {
      _om->_succ = NULL;
      OrderAccess::fence(); // always do a full fence when successor is cleared
    }
  }
}

// -----------------------------------------------------------------------------
// Enter support

bool ObjectMonitor::enter(JavaThread* current) {
  // The following code is ordered to check the most common cases first
  // and to reduce RTS->RTO cache line upgrades on SPARC and IA32 processors.

  void* cur = try_set_owner_from(NULL, current);
  if (cur == NULL) {
    assert(_recursions == 0, "invariant");
    return true;
  }

  if (cur == current) {
    // TODO-FIXME: check for integer overflow!  BUGID 6557169.
    _recursions++;
    return true;
  }

  if (current->is_lock_owned((address)cur)) {
    assert(_recursions == 0, "internal state error");
    _recursions = 1;
    set_owner_from_BasicLock(cur, current);  // Convert from BasicLock* to Thread*.
    return true;
  }

  // We've encountered genuine contention.
  assert(current->_Stalled == 0, "invariant");
  current->_Stalled = intptr_t(this);

  // Try one round of spinning *before* enqueueing current
  // and before going through the awkward and expensive state
  // transitions.  The following spin is strictly optional ...
  // Note that if we acquire the monitor from an initial spin
  // we forgo posting JVMTI events and firing DTRACE probes.
  if (TrySpin(current) > 0) {
    assert(owner_raw() == current, "must be current: owner=" INTPTR_FORMAT, p2i(owner_raw()));
    assert(_recursions == 0, "must be 0: recursions=" INTX_FORMAT, _recursions);
    assert(object()->mark() == markWord::encode(this),
           "object mark must match encoded this: mark=" INTPTR_FORMAT
           ", encoded this=" INTPTR_FORMAT, object()->mark().value(),
           markWord::encode(this).value());
    current->_Stalled = 0;
    return true;
  }

  assert(owner_raw() != current, "invariant");
  assert(_succ != current, "invariant");
  assert(!SafepointSynchronize::is_at_safepoint(), "invariant");
  assert(current->thread_state() != _thread_blocked, "invariant");

  // Keep track of contention for JVM/TI and M&M queries.
  add_to_contentions(1);
  if (is_being_async_deflated()) {
    // Async deflation is in progress and our contentions increment
    // above lost the race to async deflation. Undo the work and
    // force the caller to retry.
    const oop l_object = object();
    if (l_object != NULL) {
      // Attempt to restore the header/dmw to the object's header so that
      // we only retry once if the deflater thread happens to be slow.
      install_displaced_markword_in_object(l_object);
    }
    current->_Stalled = 0;
    add_to_contentions(-1);
    return false;
  }

  JFR_ONLY(JfrConditionalFlushWithStacktrace<EventJavaMonitorEnter> flush(current);)
  EventJavaMonitorEnter event;
  if (event.is_started()) {
    event.set_monitorClass(object()->klass());
    // Set an address that is 'unique enough', such that events close in
    // time and with the same address are likely (but not guaranteed) to
    // belong to the same object.
    event.set_address((uintptr_t)this);
  }

  { // Change java thread status to indicate blocked on monitor enter.
    JavaThreadBlockedOnMonitorEnterState jtbmes(current, this);

    assert(current->current_pending_monitor() == NULL, "invariant");
    current->set_current_pending_monitor(this);

    DTRACE_MONITOR_PROBE(contended__enter, this, object(), current);
    if (JvmtiExport::should_post_monitor_contended_enter()) {
      JvmtiExport::post_monitor_contended_enter(current, this);

      // The current thread does not yet own the monitor and does not
      // yet appear on any queues that would get it made the successor.
      // This means that the JVMTI_EVENT_MONITOR_CONTENDED_ENTER event
      // handler cannot accidentally consume an unpark() meant for the
      // ParkEvent associated with this ObjectMonitor.
    }

    OSThreadContendState osts(current->osthread());

    assert(current->thread_state() == _thread_in_vm, "invariant");

    for (;;) {
      ExitOnSuspend eos(this);
      {
        ThreadBlockInVMPreprocess<ExitOnSuspend> tbivs(current, eos, true /* allow_suspend */);
        EnterI(current);
        current->set_current_pending_monitor(NULL);
        // We can go to a safepoint at the end of this block. If we
        // do a thread dump during that safepoint, then this thread will show
        // as having "-locked" the monitor, but the OS and java.lang.Thread
        // states will still report that the thread is blocked trying to
        // acquire it.
        // If there is a suspend request, ExitOnSuspend will exit the OM
        // and set the OM as pending.
      }
      if (!eos.exited()) {
        // ExitOnSuspend did not exit the OM
        assert(owner_raw() == current, "invariant");
        break;
      }
    }

    // We've just gotten past the enter-check-for-suspend dance and we now own
    // the monitor free and clear.
  }

  add_to_contentions(-1);
  assert(contentions() >= 0, "must not be negative: contentions=%d", contentions());
  current->_Stalled = 0;

  // Must either set _recursions = 0 or ASSERT _recursions == 0.
  assert(_recursions == 0, "invariant");
  assert(owner_raw() == current, "invariant");
  assert(_succ != current, "invariant");
  assert(object()->mark() == markWord::encode(this), "invariant");

  // The thread -- now the owner -- is back in vm mode.
  // Report the glorious news via TI,DTrace and jvmstat.
  // The probe effect is non-trivial.  All the reportage occurs
  // while we hold the monitor, increasing the length of the critical
  // section.  Amdahl's parallel speedup law comes vividly into play.
  //
  // Another option might be to aggregate the events (thread local or
  // per-monitor aggregation) and defer reporting until a more opportune
  // time -- such as next time some thread encounters contention but has
  // yet to acquire the lock.  While spinning that thread could
  // spinning we could increment JVMStat counters, etc.

  DTRACE_MONITOR_PROBE(contended__entered, this, object(), current);
  if (JvmtiExport::should_post_monitor_contended_entered()) {
    JvmtiExport::post_monitor_contended_entered(current, this);

    // The current thread already owns the monitor and is not going to
    // call park() for the remainder of the monitor enter protocol. So
    // it doesn't matter if the JVMTI_EVENT_MONITOR_CONTENDED_ENTERED
    // event handler consumed an unpark() issued by the thread that
    // just exited the monitor.
  }
  if (event.should_commit()) {
    event.set_previousOwner(_previous_owner_tid);
    event.commit();
  }
  OM_PERFDATA_OP(ContendedLockAttempts, inc());
  return true;
}

// Caveat: TryLock() is not necessarily serializing if it returns failure.
// Callers must compensate as needed.

int ObjectMonitor::TryLock(JavaThread* current) {
  void* own = owner_raw();
  if (own != NULL) return 0;
  if (try_set_owner_from(NULL, current) == NULL) {
    assert(_recursions == 0, "invariant");
    return 1;
  }
  // The lock had been free momentarily, but we lost the race to the lock.
  // Interference -- the CAS failed.
  // We can either return -1 or retry.
  // Retry doesn't make as much sense because the lock was just acquired.
  return -1;
}

// Deflate the specified ObjectMonitor if not in-use. Returns true if it
// was deflated and false otherwise.
//
// The async deflation protocol sets owner to DEFLATER_MARKER and
// makes contentions negative as signals to contending threads that
// an async deflation is in progress. There are a number of checks
// as part of the protocol to make sure that the calling thread has
// not lost the race to a contending thread.
//
// The ObjectMonitor has been successfully async deflated when:
//   (contentions < 0)
// Contending threads that see that condition know to retry their operation.
//
bool ObjectMonitor::deflate_monitor() {
  if (is_busy()) {
    // Easy checks are first - the ObjectMonitor is busy so no deflation.
    return false;
  }

  if (ObjectSynchronizer::is_final_audit() && owner_is_DEFLATER_MARKER()) {
    // The final audit can see an already deflated ObjectMonitor on the
    // in-use list because MonitorList::unlink_deflated() might have
    // blocked for the final safepoint before unlinking all the deflated
    // monitors.
    assert(contentions() < 0, "must be negative: contentions=%d", contentions());
    // Already returned 'true' when it was originally deflated.
    return false;
  }

  const oop obj = object_peek();

  if (obj == NULL) {
    // If the object died, we can recycle the monitor without racing with
    // Java threads. The GC already broke the association with the object.
    set_owner_from(NULL, DEFLATER_MARKER);
    assert(contentions() >= 0, "must be non-negative: contentions=%d", contentions());
    _contentions = INT_MIN; // minimum negative int
  } else {
    // Attempt async deflation protocol.

    // Set a NULL owner to DEFLATER_MARKER to force any contending thread
    // through the slow path. This is just the first part of the async
    // deflation dance.
    if (try_set_owner_from(NULL, DEFLATER_MARKER) != NULL) {
      // The owner field is no longer NULL so we lost the race since the
      // ObjectMonitor is now busy.
      return false;
    }

    if (contentions() > 0 || _waiters != 0) {
      // Another thread has raced to enter the ObjectMonitor after
      // is_busy() above or has already entered and waited on
      // it which makes it busy so no deflation. Restore owner to
      // NULL if it is still DEFLATER_MARKER.
      if (try_set_owner_from(DEFLATER_MARKER, NULL) != DEFLATER_MARKER) {
        // Deferred decrement for the JT EnterI() that cancelled the async deflation.
        add_to_contentions(-1);
      }
      return false;
    }

    // Make a zero contentions field negative to force any contending threads
    // to retry. This is the second part of the async deflation dance.
    if (Atomic::cmpxchg(&_contentions, 0, INT_MIN) != 0) {
      // Contentions was no longer 0 so we lost the race since the
      // ObjectMonitor is now busy. Restore owner to NULL if it is
      // still DEFLATER_MARKER:
      if (try_set_owner_from(DEFLATER_MARKER, NULL) != DEFLATER_MARKER) {
        // Deferred decrement for the JT EnterI() that cancelled the async deflation.
        add_to_contentions(-1);
      }
      return false;
    }
  }

  // Sanity checks for the races:
  guarantee(owner_is_DEFLATER_MARKER(), "must be deflater marker");
  guarantee(contentions() < 0, "must be negative: contentions=%d",
            contentions());
  guarantee(_waiters == 0, "must be 0: waiters=%d", _waiters);
  guarantee(_cxq == NULL, "must be no contending threads: cxq="
            INTPTR_FORMAT, p2i(_cxq));
  guarantee(_EntryList == NULL,
            "must be no entering threads: EntryList=" INTPTR_FORMAT,
            p2i(_EntryList));

  if (obj != NULL) {
    if (log_is_enabled(Trace, monitorinflation)) {
      ResourceMark rm;
      log_trace(monitorinflation)("deflate_monitor: object=" INTPTR_FORMAT
                                  ", mark=" INTPTR_FORMAT ", type='%s'",
                                  p2i(obj), obj->mark().value(),
                                  obj->klass()->external_name());
    }

    // Install the old mark word if nobody else has already done it.
    install_displaced_markword_in_object(obj);
  }

  // We leave owner == DEFLATER_MARKER and contentions < 0
  // to force any racing threads to retry.
  return true;  // Success, ObjectMonitor has been deflated.
}

// Install the displaced mark word (dmw) of a deflating ObjectMonitor
// into the header of the object associated with the monitor. This
// idempotent method is called by a thread that is deflating a
// monitor and by other threads that have detected a race with the
// deflation process.
void ObjectMonitor::install_displaced_markword_in_object(const oop obj) {
  // This function must only be called when (owner == DEFLATER_MARKER
  // && contentions <= 0), but we can't guarantee that here because
  // those values could change when the ObjectMonitor gets moved from
  // the global free list to a per-thread free list.

  guarantee(obj != NULL, "must be non-NULL");

  // Separate loads in is_being_async_deflated(), which is almost always
  // called before this function, from the load of dmw/header below.

  // _contentions and dmw/header may get written by different threads.
  // Make sure to observe them in the same order when having several observers.
  OrderAccess::loadload_for_IRIW();

  const oop l_object = object_peek();
  if (l_object == NULL) {
    // ObjectMonitor's object ref has already been cleared by async
    // deflation or GC so we're done here.
    return;
  }
  assert(l_object == obj, "object=" INTPTR_FORMAT " must equal obj="
         INTPTR_FORMAT, p2i(l_object), p2i(obj));

  markWord dmw = header();
  // The dmw has to be neutral (not NULL, not locked and not marked).
  assert(dmw.is_neutral(), "must be neutral: dmw=" INTPTR_FORMAT, dmw.value());

  // Install displaced mark word if the object's header still points
  // to this ObjectMonitor. More than one racing caller to this function
  // can rarely reach this point, but only one can win.
  markWord res = obj->cas_set_mark(dmw, markWord::encode(this));
  if (res != markWord::encode(this)) {
    // This should be rare so log at the Info level when it happens.
    log_info(monitorinflation)("install_displaced_markword_in_object: "
                               "failed cas_set_mark: new_mark=" INTPTR_FORMAT
                               ", old_mark=" INTPTR_FORMAT ", res=" INTPTR_FORMAT,
                               dmw.value(), markWord::encode(this).value(),
                               res.value());
  }

  // Note: It does not matter which thread restored the header/dmw
  // into the object's header. The thread deflating the monitor just
  // wanted the object's header restored and it is. The threads that
  // detected a race with the deflation process also wanted the
  // object's header restored before they retry their operation and
  // because it is restored they will only retry once.
}

// Convert the fields used by is_busy() to a string that can be
// used for diagnostic output.
const char* ObjectMonitor::is_busy_to_string(stringStream* ss) {
  ss->print("is_busy: waiters=%d, ", _waiters);
  if (contentions() > 0) {
    ss->print("contentions=%d, ", contentions());
  } else {
    ss->print("contentions=0");
  }
  if (!owner_is_DEFLATER_MARKER()) {
    ss->print("owner=" INTPTR_FORMAT, p2i(owner_raw()));
  } else {
    // We report NULL instead of DEFLATER_MARKER here because is_busy()
    // ignores DEFLATER_MARKER values.
    ss->print("owner=" INTPTR_FORMAT, NULL);
  }
  ss->print(", cxq=" INTPTR_FORMAT ", EntryList=" INTPTR_FORMAT, p2i(_cxq),
            p2i(_EntryList));
  return ss->base();
}

#define MAX_RECHECK_INTERVAL 1000

void ObjectMonitor::EnterI(JavaThread* current) {
  assert(current->thread_state() == _thread_blocked, "invariant");

  // Try the lock - TATAS
  if (TryLock (current) > 0) {
    assert(_succ != current, "invariant");
    assert(owner_raw() == current, "invariant");
    assert(_Responsible != current, "invariant");
    return;
  }

  if (try_set_owner_from(DEFLATER_MARKER, current) == DEFLATER_MARKER) {
    // Cancelled the in-progress async deflation by changing owner from
    // DEFLATER_MARKER to current. As part of the contended enter protocol,
    // contentions was incremented to a positive value before EnterI()
    // was called and that prevents the deflater thread from winning the
    // last part of the 2-part async deflation protocol. After EnterI()
    // returns to enter(), contentions is decremented because the caller
    // now owns the monitor. We bump contentions an extra time here to
    // prevent the deflater thread from winning the last part of the
    // 2-part async deflation protocol after the regular decrement
    // occurs in enter(). The deflater thread will decrement contentions
    // after it recognizes that the async deflation was cancelled.
    add_to_contentions(1);
    assert(_succ != current, "invariant");
    assert(_Responsible != current, "invariant");
    return;
  }

  assert(InitDone, "Unexpectedly not initialized");

  // We try one round of spinning *before* enqueueing current.
  //
  // If the _owner is ready but OFFPROC we could use a YieldTo()
  // operation to donate the remainder of this thread's quantum
  // to the owner.  This has subtle but beneficial affinity
  // effects.

  if (TrySpin(current) > 0) {
    assert(owner_raw() == current, "invariant");
    assert(_succ != current, "invariant");
    assert(_Responsible != current, "invariant");
    return;
  }

  // The Spin failed -- Enqueue and park the thread ...
  assert(_succ != current, "invariant");
  assert(owner_raw() != current, "invariant");
  assert(_Responsible != current, "invariant");

  // Enqueue "current" on ObjectMonitor's _cxq.
  //
  // Node acts as a proxy for current.
  // As an aside, if were to ever rewrite the synchronization code mostly
  // in Java, WaitNodes, ObjectMonitors, and Events would become 1st-class
  // Java objects.  This would avoid awkward lifecycle and liveness issues,
  // as well as eliminate a subset of ABA issues.
  // TODO: eliminate ObjectWaiter and enqueue either Threads or Events.

  ObjectWaiter node(current);
  current->_ParkEvent->reset();
  node._prev   = (ObjectWaiter*) 0xBAD;
  node.TState  = ObjectWaiter::TS_CXQ;

  // Push "current" onto the front of the _cxq.
  // Once on cxq/EntryList, current stays on-queue until it acquires the lock.
  // Note that spinning tends to reduce the rate at which threads
  // enqueue and dequeue on EntryList|cxq.
  ObjectWaiter* nxt;
  for (;;) {
    node._next = nxt = _cxq;
    if (Atomic::cmpxchg(&_cxq, nxt, &node) == nxt) break;

    // Interference - the CAS failed because _cxq changed.  Just retry.
    // As an optional optimization we retry the lock.
    if (TryLock (current) > 0) {
      assert(_succ != current, "invariant");
      assert(owner_raw() == current, "invariant");
      assert(_Responsible != current, "invariant");
      return;
    }
  }

  // Check for cxq|EntryList edge transition to non-null.  This indicates
  // the onset of contention.  While contention persists exiting threads
  // will use a ST:MEMBAR:LD 1-1 exit protocol.  When contention abates exit
  // operations revert to the faster 1-0 mode.  This enter operation may interleave
  // (race) a concurrent 1-0 exit operation, resulting in stranding, so we
  // arrange for one of the contending thread to use a timed park() operations
  // to detect and recover from the race.  (Stranding is form of progress failure
  // where the monitor is unlocked but all the contending threads remain parked).
  // That is, at least one of the contended threads will periodically poll _owner.
  // One of the contending threads will become the designated "Responsible" thread.
  // The Responsible thread uses a timed park instead of a normal indefinite park
  // operation -- it periodically wakes and checks for and recovers from potential
  // strandings admitted by 1-0 exit operations.   We need at most one Responsible
  // thread per-monitor at any given moment.  Only threads on cxq|EntryList may
  // be responsible for a monitor.
  //
  // Currently, one of the contended threads takes on the added role of "Responsible".
  // A viable alternative would be to use a dedicated "stranding checker" thread
  // that periodically iterated over all the threads (or active monitors) and unparked
  // successors where there was risk of stranding.  This would help eliminate the
  // timer scalability issues we see on some platforms as we'd only have one thread
  // -- the checker -- parked on a timer.

  if (nxt == NULL && _EntryList == NULL) {
    // Try to assume the role of responsible thread for the monitor.
    // CONSIDER:  ST vs CAS vs { if (Responsible==null) Responsible=current }
    Atomic::replace_if_null(&_Responsible, current);
  }

  // The lock might have been released while this thread was occupied queueing
  // itself onto _cxq.  To close the race and avoid "stranding" and
  // progress-liveness failure we must resample-retry _owner before parking.
  // Note the Dekker/Lamport duality: ST cxq; MEMBAR; LD Owner.
  // In this case the ST-MEMBAR is accomplished with CAS().
  //
  // TODO: Defer all thread state transitions until park-time.
  // Since state transitions are heavy and inefficient we'd like
  // to defer the state transitions until absolutely necessary,
  // and in doing so avoid some transitions ...

  int nWakeups = 0;
  int recheckInterval = 1;

  for (;;) {

    if (TryLock(current) > 0) break;
    assert(owner_raw() != current, "invariant");

    // park self
    if (_Responsible == current) {
      current->_ParkEvent->park((jlong) recheckInterval);
      // Increase the recheckInterval, but clamp the value.
      recheckInterval *= 8;
      if (recheckInterval > MAX_RECHECK_INTERVAL) {
        recheckInterval = MAX_RECHECK_INTERVAL;
      }
    } else {
      current->_ParkEvent->park();
    }

    if (TryLock(current) > 0) break;

    if (try_set_owner_from(DEFLATER_MARKER, current) == DEFLATER_MARKER) {
      // Cancelled the in-progress async deflation by changing owner from
      // DEFLATER_MARKER to current. As part of the contended enter protocol,
      // contentions was incremented to a positive value before EnterI()
      // was called and that prevents the deflater thread from winning the
      // last part of the 2-part async deflation protocol. After EnterI()
      // returns to enter(), contentions is decremented because the caller
      // now owns the monitor. We bump contentions an extra time here to
      // prevent the deflater thread from winning the last part of the
      // 2-part async deflation protocol after the regular decrement
      // occurs in enter(). The deflater thread will decrement contentions
      // after it recognizes that the async deflation was cancelled.
      add_to_contentions(1);
      break;
    }

    // The lock is still contested.
    // Keep a tally of the # of futile wakeups.
    // Note that the counter is not protected by a lock or updated by atomics.
    // That is by design - we trade "lossy" counters which are exposed to
    // races during updates for a lower probe effect.

    // This PerfData object can be used in parallel with a safepoint.
    // See the work around in PerfDataManager::destroy().
    OM_PERFDATA_OP(FutileWakeups, inc());
    ++nWakeups;

    // Assuming this is not a spurious wakeup we'll normally find _succ == current.
    // We can defer clearing _succ until after the spin completes
    // TrySpin() must tolerate being called with _succ == current.
    // Try yet another round of adaptive spinning.
    if (TrySpin(current) > 0) break;

    // We can find that we were unpark()ed and redesignated _succ while
    // we were spinning.  That's harmless.  If we iterate and call park(),
    // park() will consume the event and return immediately and we'll
    // just spin again.  This pattern can repeat, leaving _succ to simply
    // spin on a CPU.

    if (_succ == current) _succ = NULL;

    // Invariant: after clearing _succ a thread *must* retry _owner before parking.
    OrderAccess::fence();
  }

  // Egress :
  // current has acquired the lock -- Unlink current from the cxq or EntryList.
  // Normally we'll find current on the EntryList .
  // From the perspective of the lock owner (this thread), the
  // EntryList is stable and cxq is prepend-only.
  // The head of cxq is volatile but the interior is stable.
  // In addition, current.TState is stable.

  assert(owner_raw() == current, "invariant");

  UnlinkAfterAcquire(current, &node);
  if (_succ == current) _succ = NULL;

  assert(_succ != current, "invariant");
  if (_Responsible == current) {
    _Responsible = NULL;
    OrderAccess::fence(); // Dekker pivot-point

    // We may leave threads on cxq|EntryList without a designated
    // "Responsible" thread.  This is benign.  When this thread subsequently
    // exits the monitor it can "see" such preexisting "old" threads --
    // threads that arrived on the cxq|EntryList before the fence, above --
    // by LDing cxq|EntryList.  Newly arrived threads -- that is, threads
    // that arrive on cxq after the ST:MEMBAR, above -- will set Responsible
    // non-null and elect a new "Responsible" timer thread.
    //
    // This thread executes:
    //    ST Responsible=null; MEMBAR    (in enter epilogue - here)
    //    LD cxq|EntryList               (in subsequent exit)
    //
    // Entering threads in the slow/contended path execute:
    //    ST cxq=nonnull; MEMBAR; LD Responsible (in enter prolog)
    //    The (ST cxq; MEMBAR) is accomplished with CAS().
    //
    // The MEMBAR, above, prevents the LD of cxq|EntryList in the subsequent
    // exit operation from floating above the ST Responsible=null.
  }

  // We've acquired ownership with CAS().
  // CAS is serializing -- it has MEMBAR/FENCE-equivalent semantics.
  // But since the CAS() this thread may have also stored into _succ,
  // EntryList, cxq or Responsible.  These meta-data updates must be
  // visible __before this thread subsequently drops the lock.
  // Consider what could occur if we didn't enforce this constraint --
  // STs to monitor meta-data and user-data could reorder with (become
  // visible after) the ST in exit that drops ownership of the lock.
  // Some other thread could then acquire the lock, but observe inconsistent
  // or old monitor meta-data and heap data.  That violates the JMM.
  // To that end, the 1-0 exit() operation must have at least STST|LDST
  // "release" barrier semantics.  Specifically, there must be at least a
  // STST|LDST barrier in exit() before the ST of null into _owner that drops
  // the lock.   The barrier ensures that changes to monitor meta-data and data
  // protected by the lock will be visible before we release the lock, and
  // therefore before some other thread (CPU) has a chance to acquire the lock.
  // See also: http://gee.cs.oswego.edu/dl/jmm/cookbook.html.
  //
  // Critically, any prior STs to _succ or EntryList must be visible before
  // the ST of null into _owner in the *subsequent* (following) corresponding
  // monitorexit.  Recall too, that in 1-0 mode monitorexit does not necessarily
  // execute a serializing instruction.

  return;
}

// ReenterI() is a specialized inline form of the latter half of the
// contended slow-path from EnterI().  We use ReenterI() only for
// monitor reentry in wait().
//
// In the future we should reconcile EnterI() and ReenterI().

void ObjectMonitor::ReenterI(JavaThread* current, ObjectWaiter* currentNode) {
  assert(current != NULL, "invariant");
  assert(currentNode != NULL, "invariant");
  assert(currentNode->_thread == current, "invariant");
  assert(_waiters > 0, "invariant");
  assert(object()->mark() == markWord::encode(this), "invariant");

  assert(current->thread_state() != _thread_blocked, "invariant");

  int nWakeups = 0;
  for (;;) {
    ObjectWaiter::TStates v = currentNode->TState;
    guarantee(v == ObjectWaiter::TS_ENTER || v == ObjectWaiter::TS_CXQ, "invariant");
    assert(owner_raw() != current, "invariant");

    if (TryLock(current) > 0) break;
    if (TrySpin(current) > 0) break;

    {
      OSThreadContendState osts(current->osthread());

      assert(current->thread_state() == _thread_in_vm, "invariant");

      {
        ClearSuccOnSuspend csos(this);
        ThreadBlockInVMPreprocess<ClearSuccOnSuspend> tbivs(current, csos, true /* allow_suspend */);
        current->_ParkEvent->park();
      }
    }

    // Try again, but just so we distinguish between futile wakeups and
    // successful wakeups.  The following test isn't algorithmically
    // necessary, but it helps us maintain sensible statistics.
    if (TryLock(current) > 0) break;

    // The lock is still contested.
    // Keep a tally of the # of futile wakeups.
    // Note that the counter is not protected by a lock or updated by atomics.
    // That is by design - we trade "lossy" counters which are exposed to
    // races during updates for a lower probe effect.
    ++nWakeups;

    // Assuming this is not a spurious wakeup we'll normally
    // find that _succ == current.
    if (_succ == current) _succ = NULL;

    // Invariant: after clearing _succ a contending thread
    // *must* retry  _owner before parking.
    OrderAccess::fence();

    // This PerfData object can be used in parallel with a safepoint.
    // See the work around in PerfDataManager::destroy().
    OM_PERFDATA_OP(FutileWakeups, inc());
  }

  // current has acquired the lock -- Unlink current from the cxq or EntryList .
  // Normally we'll find current on the EntryList.
  // Unlinking from the EntryList is constant-time and atomic-free.
  // From the perspective of the lock owner (this thread), the
  // EntryList is stable and cxq is prepend-only.
  // The head of cxq is volatile but the interior is stable.
  // In addition, current.TState is stable.

  assert(owner_raw() == current, "invariant");
  assert(object()->mark() == markWord::encode(this), "invariant");
  UnlinkAfterAcquire(current, currentNode);
  if (_succ == current) _succ = NULL;
  assert(_succ != current, "invariant");
  currentNode->TState = ObjectWaiter::TS_RUN;
  OrderAccess::fence();      // see comments at the end of EnterI()
}

// By convention we unlink a contending thread from EntryList|cxq immediately
// after the thread acquires the lock in ::enter().  Equally, we could defer
// unlinking the thread until ::exit()-time.

void ObjectMonitor::UnlinkAfterAcquire(JavaThread* current, ObjectWaiter* currentNode) {
  assert(owner_raw() == current, "invariant");
  assert(currentNode->_thread == current, "invariant");

  if (currentNode->TState == ObjectWaiter::TS_ENTER) {
    // Normal case: remove current from the DLL EntryList .
    // This is a constant-time operation.
    ObjectWaiter* nxt = currentNode->_next;
    ObjectWaiter* prv = currentNode->_prev;
    if (nxt != NULL) nxt->_prev = prv;
    if (prv != NULL) prv->_next = nxt;
    if (currentNode == _EntryList) _EntryList = nxt;
    assert(nxt == NULL || nxt->TState == ObjectWaiter::TS_ENTER, "invariant");
    assert(prv == NULL || prv->TState == ObjectWaiter::TS_ENTER, "invariant");
  } else {
    assert(currentNode->TState == ObjectWaiter::TS_CXQ, "invariant");
    // Inopportune interleaving -- current is still on the cxq.
    // This usually means the enqueue of self raced an exiting thread.
    // Normally we'll find current near the front of the cxq, so
    // dequeueing is typically fast.  If needbe we can accelerate
    // this with some MCS/CHL-like bidirectional list hints and advisory
    // back-links so dequeueing from the interior will normally operate
    // in constant-time.
    // Dequeue current from either the head (with CAS) or from the interior
    // with a linear-time scan and normal non-atomic memory operations.
    // CONSIDER: if current is on the cxq then simply drain cxq into EntryList
    // and then unlink current from EntryList.  We have to drain eventually,
    // so it might as well be now.

    ObjectWaiter* v = _cxq;
    assert(v != NULL, "invariant");
    if (v != currentNode || Atomic::cmpxchg(&_cxq, v, currentNode->_next) != v) {
      // The CAS above can fail from interference IFF a "RAT" arrived.
      // In that case current must be in the interior and can no longer be
      // at the head of cxq.
      if (v == currentNode) {
        assert(_cxq != v, "invariant");
        v = _cxq;          // CAS above failed - start scan at head of list
      }
      ObjectWaiter* p;
      ObjectWaiter* q = NULL;
      for (p = v; p != NULL && p != currentNode; p = p->_next) {
        q = p;
        assert(p->TState == ObjectWaiter::TS_CXQ, "invariant");
      }
      assert(v != currentNode, "invariant");
      assert(p == currentNode, "Node not found on cxq");
      assert(p != _cxq, "invariant");
      assert(q != NULL, "invariant");
      assert(q->_next == p, "invariant");
      q->_next = p->_next;
    }
  }

#ifdef ASSERT
  // Diagnostic hygiene ...
  currentNode->_prev  = (ObjectWaiter*) 0xBAD;
  currentNode->_next  = (ObjectWaiter*) 0xBAD;
  currentNode->TState = ObjectWaiter::TS_RUN;
#endif
}

// -----------------------------------------------------------------------------
// Exit support
//
// exit()
// ~~~~~~
// Note that the collector can't reclaim the objectMonitor or deflate
// the object out from underneath the thread calling ::exit() as the
// thread calling ::exit() never transitions to a stable state.
// This inhibits GC, which in turn inhibits asynchronous (and
// inopportune) reclamation of "this".
//
// We'd like to assert that: (THREAD->thread_state() != _thread_blocked) ;
// There's one exception to the claim above, however.  EnterI() can call
// exit() to drop a lock if the acquirer has been externally suspended.
// In that case exit() is called with _thread_state == _thread_blocked,
// but the monitor's _contentions field is > 0, which inhibits reclamation.
//
// 1-0 exit
// ~~~~~~~~
// ::exit() uses a canonical 1-1 idiom with a MEMBAR although some of
// the fast-path operators have been optimized so the common ::exit()
// operation is 1-0, e.g., see macroAssembler_x86.cpp: fast_unlock().
// The code emitted by fast_unlock() elides the usual MEMBAR.  This
// greatly improves latency -- MEMBAR and CAS having considerable local
// latency on modern processors -- but at the cost of "stranding".  Absent the
// MEMBAR, a thread in fast_unlock() can race a thread in the slow
// ::enter() path, resulting in the entering thread being stranding
// and a progress-liveness failure.   Stranding is extremely rare.
// We use timers (timed park operations) & periodic polling to detect
// and recover from stranding.  Potentially stranded threads periodically
// wake up and poll the lock.  See the usage of the _Responsible variable.
//
// The CAS() in enter provides for safety and exclusion, while the CAS or
// MEMBAR in exit provides for progress and avoids stranding.  1-0 locking
// eliminates the CAS/MEMBAR from the exit path, but it admits stranding.
// We detect and recover from stranding with timers.
//
// If a thread transiently strands it'll park until (a) another
// thread acquires the lock and then drops the lock, at which time the
// exiting thread will notice and unpark the stranded thread, or, (b)
// the timer expires.  If the lock is high traffic then the stranding latency
// will be low due to (a).  If the lock is low traffic then the odds of
// stranding are lower, although the worst-case stranding latency
// is longer.  Critically, we don't want to put excessive load in the
// platform's timer subsystem.  We want to minimize both the timer injection
// rate (timers created/sec) as well as the number of timers active at
// any one time.  (more precisely, we want to minimize timer-seconds, which is
// the integral of the # of active timers at any instant over time).
// Both impinge on OS scalability.  Given that, at most one thread parked on
// a monitor will use a timer.
//
// There is also the risk of a futile wake-up. If we drop the lock
// another thread can reacquire the lock immediately, and we can
// then wake a thread unnecessarily. This is benign, and we've
// structured the code so the windows are short and the frequency
// of such futile wakups is low.

void ObjectMonitor::exit(JavaThread* current, bool not_suspended) {
  void* cur = owner_raw();
  if (current != cur) {
    if (current->is_lock_owned((address)cur)) {
      assert(_recursions == 0, "invariant");
      set_owner_from_BasicLock(cur, current);  // Convert from BasicLock* to Thread*.
      _recursions = 0;
    } else {
      // Apparent unbalanced locking ...
      // Naively we'd like to throw IllegalMonitorStateException.
      // As a practical matter we can neither allocate nor throw an
      // exception as ::exit() can be called from leaf routines.
      // see x86_32.ad Fast_Unlock() and the I1 and I2 properties.
      // Upon deeper reflection, however, in a properly run JVM the only
      // way we should encounter this situation is in the presence of
      // unbalanced JNI locking. TODO: CheckJNICalls.
      // See also: CR4414101
#ifdef ASSERT
      LogStreamHandle(Error, monitorinflation) lsh;
      lsh.print_cr("ERROR: ObjectMonitor::exit(): thread=" INTPTR_FORMAT
                    " is exiting an ObjectMonitor it does not own.", p2i(current));
      lsh.print_cr("The imbalance is possibly caused by JNI locking.");
      print_debug_style_on(&lsh);
      assert(false, "Non-balanced monitor enter/exit!");
#endif
      return;
    }
  }

  if (_recursions != 0) {
    _recursions--;        // this is simple recursive enter
    return;
  }

  // Invariant: after setting Responsible=null an thread must execute
  // a MEMBAR or other serializing instruction before fetching EntryList|cxq.
  _Responsible = NULL;

#if INCLUDE_JFR
  // get the owner's thread id for the MonitorEnter event
  // if it is enabled and the thread isn't suspended
  if (not_suspended && EventJavaMonitorEnter::is_enabled()) {
    _previous_owner_tid = JFR_THREAD_ID(current);
  }
#endif

  for (;;) {
    assert(current == owner_raw(), "invariant");

    // Drop the lock.
    // release semantics: prior loads and stores from within the critical section
    // must not float (reorder) past the following store that drops the lock.
    // Uses a storeload to separate release_store(owner) from the
    // successor check. The try_set_owner() below uses cmpxchg() so
    // we get the fence down there.
    release_clear_owner(current);
    OrderAccess::storeload();

    if ((intptr_t(_EntryList)|intptr_t(_cxq)) == 0 || _succ != NULL) {
      return;
    }
    // Other threads are blocked trying to acquire the lock.

    // Normally the exiting thread is responsible for ensuring succession,
    // but if other successors are ready or other entering threads are spinning
    // then this thread can simply store NULL into _owner and exit without
    // waking a successor.  The existence of spinners or ready successors
    // guarantees proper succession (liveness).  Responsibility passes to the
    // ready or running successors.  The exiting thread delegates the duty.
    // More precisely, if a successor already exists this thread is absolved
    // of the responsibility of waking (unparking) one.
    //
    // The _succ variable is critical to reducing futile wakeup frequency.
    // _succ identifies the "heir presumptive" thread that has been made
    // ready (unparked) but that has not yet run.  We need only one such
    // successor thread to guarantee progress.
    // See http://www.usenix.org/events/jvm01/full_papers/dice/dice.pdf
    // section 3.3 "Futile Wakeup Throttling" for details.
    //
    // Note that spinners in Enter() also set _succ non-null.
    // In the current implementation spinners opportunistically set
    // _succ so that exiting threads might avoid waking a successor.
    // Another less appealing alternative would be for the exiting thread
    // to drop the lock and then spin briefly to see if a spinner managed
    // to acquire the lock.  If so, the exiting thread could exit
    // immediately without waking a successor, otherwise the exiting
    // thread would need to dequeue and wake a successor.
    // (Note that we'd need to make the post-drop spin short, but no
    // shorter than the worst-case round-trip cache-line migration time.
    // The dropped lock needs to become visible to the spinner, and then
    // the acquisition of the lock by the spinner must become visible to
    // the exiting thread).

    // It appears that an heir-presumptive (successor) must be made ready.
    // Only the current lock owner can manipulate the EntryList or
    // drain _cxq, so we need to reacquire the lock.  If we fail
    // to reacquire the lock the responsibility for ensuring succession
    // falls to the new owner.
    //
    if (try_set_owner_from(NULL, current) != NULL) {
      return;
    }

    guarantee(owner_raw() == current, "invariant");

    ObjectWaiter* w = NULL;

    w = _EntryList;
    if (w != NULL) {
      // I'd like to write: guarantee (w->_thread != current).
      // But in practice an exiting thread may find itself on the EntryList.
      // Let's say thread T1 calls O.wait().  Wait() enqueues T1 on O's waitset and
      // then calls exit().  Exit release the lock by setting O._owner to NULL.
      // Let's say T1 then stalls.  T2 acquires O and calls O.notify().  The
      // notify() operation moves T1 from O's waitset to O's EntryList. T2 then
      // release the lock "O".  T2 resumes immediately after the ST of null into
      // _owner, above.  T2 notices that the EntryList is populated, so it
      // reacquires the lock and then finds itself on the EntryList.
      // Given all that, we have to tolerate the circumstance where "w" is
      // associated with current.
      assert(w->TState == ObjectWaiter::TS_ENTER, "invariant");
      ExitEpilog(current, w);
      return;
    }

    // If we find that both _cxq and EntryList are null then just
    // re-run the exit protocol from the top.
    w = _cxq;
    if (w == NULL) continue;

    // Drain _cxq into EntryList - bulk transfer.
    // First, detach _cxq.
    // The following loop is tantamount to: w = swap(&cxq, NULL)
    for (;;) {
      assert(w != NULL, "Invariant");
      ObjectWaiter* u = Atomic::cmpxchg(&_cxq, w, (ObjectWaiter*)NULL);
      if (u == w) break;
      w = u;
    }

    assert(w != NULL, "invariant");
    assert(_EntryList == NULL, "invariant");

    // Convert the LIFO SLL anchored by _cxq into a DLL.
    // The list reorganization step operates in O(LENGTH(w)) time.
    // It's critical that this step operate quickly as
    // "current" still holds the outer-lock, restricting parallelism
    // and effectively lengthening the critical section.
    // Invariant: s chases t chases u.
    // TODO-FIXME: consider changing EntryList from a DLL to a CDLL so
    // we have faster access to the tail.

    _EntryList = w;
    ObjectWaiter* q = NULL;
    ObjectWaiter* p;
    for (p = w; p != NULL; p = p->_next) {
      guarantee(p->TState == ObjectWaiter::TS_CXQ, "Invariant");
      p->TState = ObjectWaiter::TS_ENTER;
      p->_prev = q;
      q = p;
    }

    // In 1-0 mode we need: ST EntryList; MEMBAR #storestore; ST _owner = NULL
    // The MEMBAR is satisfied by the release_store() operation in ExitEpilog().

    // See if we can abdicate to a spinner instead of waking a thread.
    // A primary goal of the implementation is to reduce the
    // context-switch rate.
    if (_succ != NULL) continue;

    w = _EntryList;
    if (w != NULL) {
      guarantee(w->TState == ObjectWaiter::TS_ENTER, "invariant");
      ExitEpilog(current, w);
      return;
    }
  }
}

void ObjectMonitor::ExitEpilog(JavaThread* current, ObjectWaiter* Wakee) {
  assert(owner_raw() == current, "invariant");

  // Exit protocol:
  // 1. ST _succ = wakee
  // 2. membar #loadstore|#storestore;
  // 2. ST _owner = NULL
  // 3. unpark(wakee)

  _succ = Wakee->_thread;
  ParkEvent * Trigger = Wakee->_event;

  // Hygiene -- once we've set _owner = NULL we can't safely dereference Wakee again.
  // The thread associated with Wakee may have grabbed the lock and "Wakee" may be
  // out-of-scope (non-extant).
  Wakee  = NULL;

  // Drop the lock.
  // Uses a fence to separate release_store(owner) from the LD in unpark().
  release_clear_owner(current);
  OrderAccess::fence();

  DTRACE_MONITOR_PROBE(contended__exit, this, object(), current);
  Trigger->unpark();

  // Maintain stats and report events to JVMTI
  OM_PERFDATA_OP(Parks, inc());
}


// -----------------------------------------------------------------------------
// Class Loader deadlock handling.
//
// complete_exit exits a lock returning recursion count
// complete_exit/reenter operate as a wait without waiting
// complete_exit requires an inflated monitor
// The _owner field is not always the Thread addr even with an
// inflated monitor, e.g. the monitor can be inflated by a non-owning
// thread due to contention.
intx ObjectMonitor::complete_exit(JavaThread* current) {
  assert(InitDone, "Unexpectedly not initialized");

  void* cur = owner_raw();
  if (current != cur) {
    if (current->is_lock_owned((address)cur)) {
      assert(_recursions == 0, "internal state error");
      set_owner_from_BasicLock(cur, current);  // Convert from BasicLock* to Thread*.
      _recursions = 0;
    }
  }

  guarantee(current == owner_raw(), "complete_exit not owner");
  intx save = _recursions; // record the old recursion count
  _recursions = 0;         // set the recursion level to be 0
  exit(current);           // exit the monitor
  guarantee(owner_raw() != current, "invariant");
  return save;
}

// reenter() enters a lock and sets recursion count
// complete_exit/reenter operate as a wait without waiting
bool ObjectMonitor::reenter(intx recursions, JavaThread* current) {

  guarantee(owner_raw() != current, "reenter already owner");
  if (!enter(current)) {
    return false;
  }
  // Entered the monitor.
  guarantee(_recursions == 0, "reenter recursion");
  _recursions = recursions;
  return true;
}

// Checks that the current THREAD owns this monitor and causes an
// immediate return if it doesn't. We don't use the CHECK macro
// because we want the IMSE to be the only exception that is thrown
// from the call site when false is returned. Any other pending
// exception is ignored.
#define CHECK_OWNER()                                                  \
  do {                                                                 \
    if (!check_owner(THREAD)) {                                        \
       assert(HAS_PENDING_EXCEPTION, "expected a pending IMSE here."); \
       return;                                                         \
     }                                                                 \
  } while (false)

// Returns true if the specified thread owns the ObjectMonitor.
// Otherwise returns false and throws IllegalMonitorStateException
// (IMSE). If there is a pending exception and the specified thread
// is not the owner, that exception will be replaced by the IMSE.
bool ObjectMonitor::check_owner(TRAPS) {
  JavaThread* current = THREAD;
  void* cur = owner_raw();
  if (cur == current) {
    return true;
  }
  if (current->is_lock_owned((address)cur)) {
    set_owner_from_BasicLock(cur, current);  // Convert from BasicLock* to Thread*.
    _recursions = 0;
    return true;
  }
  THROW_MSG_(vmSymbols::java_lang_IllegalMonitorStateException(),
             "current thread is not owner", false);
}

static void post_monitor_wait_event(EventJavaMonitorWait* event,
                                    ObjectMonitor* monitor,
                                    uint64_t notifier_tid,
                                    jlong timeout,
                                    bool timedout) {
  assert(event != NULL, "invariant");
  assert(monitor != NULL, "invariant");
  event->set_monitorClass(monitor->object()->klass());
  event->set_timeout(timeout);
  // Set an address that is 'unique enough', such that events close in
  // time and with the same address are likely (but not guaranteed) to
  // belong to the same object.
  event->set_address((uintptr_t)monitor);
  event->set_notifier(notifier_tid);
  event->set_timedOut(timedout);
  event->commit();
}

// -----------------------------------------------------------------------------
// Wait/Notify/NotifyAll
//
// Note: a subset of changes to ObjectMonitor::wait()
// will need to be replicated in complete_exit
void ObjectMonitor::wait(jlong millis, bool interruptible, TRAPS) {
  JavaThread* current = THREAD;

  assert(InitDone, "Unexpectedly not initialized");

  CHECK_OWNER();  // Throws IMSE if not owner.

  EventJavaMonitorWait event;

  // check for a pending interrupt
  if (interruptible && current->is_interrupted(true) && !HAS_PENDING_EXCEPTION) {
    // post monitor waited event.  Note that this is past-tense, we are done waiting.
    if (JvmtiExport::should_post_monitor_waited()) {
      // Note: 'false' parameter is passed here because the
      // wait was not timed out due to thread interrupt.
      JvmtiExport::post_monitor_waited(current, this, false);

      // In this short circuit of the monitor wait protocol, the
      // current thread never drops ownership of the monitor and
      // never gets added to the wait queue so the current thread
      // cannot be made the successor. This means that the
      // JVMTI_EVENT_MONITOR_WAITED event handler cannot accidentally
      // consume an unpark() meant for the ParkEvent associated with
      // this ObjectMonitor.
    }
    if (event.should_commit()) {
      post_monitor_wait_event(&event, this, 0, millis, false);
    }
    THROW(vmSymbols::java_lang_InterruptedException());
    return;
  }

  assert(current->_Stalled == 0, "invariant");
  current->_Stalled = intptr_t(this);
  current->set_current_waiting_monitor(this);

  // create a node to be put into the queue
  // Critically, after we reset() the event but prior to park(), we must check
  // for a pending interrupt.
  ObjectWaiter node(current);
  node.TState = ObjectWaiter::TS_WAIT;
  current->_ParkEvent->reset();
  OrderAccess::fence();          // ST into Event; membar ; LD interrupted-flag

  // Enter the waiting queue, which is a circular doubly linked list in this case
  // but it could be a priority queue or any data structure.
  // _WaitSetLock protects the wait queue.  Normally the wait queue is accessed only
  // by the the owner of the monitor *except* in the case where park()
  // returns because of a timeout of interrupt.  Contention is exceptionally rare
  // so we use a simple spin-lock instead of a heavier-weight blocking lock.

  Thread::SpinAcquire(&_WaitSetLock, "WaitSet - add");
  AddWaiter(&node);
  Thread::SpinRelease(&_WaitSetLock);

  _Responsible = NULL;

  intx save = _recursions;     // record the old recursion count
  _waiters++;                  // increment the number of waiters
  _recursions = 0;             // set the recursion level to be 1
  exit(current);               // exit the monitor
  guarantee(owner_raw() != current, "invariant");

  // The thread is on the WaitSet list - now park() it.
  // On MP systems it's conceivable that a brief spin before we park
  // could be profitable.
  //
  // TODO-FIXME: change the following logic to a loop of the form
  //   while (!timeout && !interrupted && _notified == 0) park()

  int ret = OS_OK;
  int WasNotified = 0;

  // Need to check interrupt state whilst still _thread_in_vm
  bool interrupted = interruptible && current->is_interrupted(false);

  { // State transition wrappers
    OSThread* osthread = current->osthread();
    OSThreadWaitState osts(osthread, true);

    assert(current->thread_state() == _thread_in_vm, "invariant");

    {
      ClearSuccOnSuspend csos(this);
      ThreadBlockInVMPreprocess<ClearSuccOnSuspend> tbivs(current, csos, true /* allow_suspend */);
      if (interrupted || HAS_PENDING_EXCEPTION) {
        // Intentionally empty
      } else if (node._notified == 0) {
        if (millis <= 0) {
          current->_ParkEvent->park();
        } else {
          ret = current->_ParkEvent->park(millis);
        }
      }
    }

    // Node may be on the WaitSet, the EntryList (or cxq), or in transition
    // from the WaitSet to the EntryList.
    // See if we need to remove Node from the WaitSet.
    // We use double-checked locking to avoid grabbing _WaitSetLock
    // if the thread is not on the wait queue.
    //
    // Note that we don't need a fence before the fetch of TState.
    // In the worst case we'll fetch a old-stale value of TS_WAIT previously
    // written by the is thread. (perhaps the fetch might even be satisfied
    // by a look-aside into the processor's own store buffer, although given
    // the length of the code path between the prior ST and this load that's
    // highly unlikely).  If the following LD fetches a stale TS_WAIT value
    // then we'll acquire the lock and then re-fetch a fresh TState value.
    // That is, we fail toward safety.

    if (node.TState == ObjectWaiter::TS_WAIT) {
      Thread::SpinAcquire(&_WaitSetLock, "WaitSet - unlink");
      if (node.TState == ObjectWaiter::TS_WAIT) {
        DequeueSpecificWaiter(&node);       // unlink from WaitSet
        assert(node._notified == 0, "invariant");
        node.TState = ObjectWaiter::TS_RUN;
      }
      Thread::SpinRelease(&_WaitSetLock);
    }

    // The thread is now either on off-list (TS_RUN),
    // on the EntryList (TS_ENTER), or on the cxq (TS_CXQ).
    // The Node's TState variable is stable from the perspective of this thread.
    // No other threads will asynchronously modify TState.
    guarantee(node.TState != ObjectWaiter::TS_WAIT, "invariant");
    OrderAccess::loadload();
    if (_succ == current) _succ = NULL;
    WasNotified = node._notified;

    // Reentry phase -- reacquire the monitor.
    // re-enter contended monitor after object.wait().
    // retain OBJECT_WAIT state until re-enter successfully completes
    // Thread state is thread_in_vm and oop access is again safe,
    // although the raw address of the object may have changed.
    // (Don't cache naked oops over safepoints, of course).

    // post monitor waited event. Note that this is past-tense, we are done waiting.
    if (JvmtiExport::should_post_monitor_waited()) {
      JvmtiExport::post_monitor_waited(current, this, ret == OS_TIMEOUT);

      if (node._notified != 0 && _succ == current) {
        // In this part of the monitor wait-notify-reenter protocol it
        // is possible (and normal) for another thread to do a fastpath
        // monitor enter-exit while this thread is still trying to get
        // to the reenter portion of the protocol.
        //
        // The ObjectMonitor was notified and the current thread is
        // the successor which also means that an unpark() has already
        // been done. The JVMTI_EVENT_MONITOR_WAITED event handler can
        // consume the unpark() that was done when the successor was
        // set because the same ParkEvent is shared between Java
        // monitors and JVM/TI RawMonitors (for now).
        //
        // We redo the unpark() to ensure forward progress, i.e., we
        // don't want all pending threads hanging (parked) with none
        // entering the unlocked monitor.
        node._event->unpark();
      }
    }

    if (event.should_commit()) {
      post_monitor_wait_event(&event, this, node._notifier_tid, millis, ret == OS_TIMEOUT);
    }

    OrderAccess::fence();

    assert(current->_Stalled != 0, "invariant");
    current->_Stalled = 0;

    assert(owner_raw() != current, "invariant");
    ObjectWaiter::TStates v = node.TState;
    if (v == ObjectWaiter::TS_RUN) {
      enter(current);
    } else {
      guarantee(v == ObjectWaiter::TS_ENTER || v == ObjectWaiter::TS_CXQ, "invariant");
      ReenterI(current, &node);
      node.wait_reenter_end(this);
    }

    // current has reacquired the lock.
    // Lifecycle - the node representing current must not appear on any queues.
    // Node is about to go out-of-scope, but even if it were immortal we wouldn't
    // want residual elements associated with this thread left on any lists.
    guarantee(node.TState == ObjectWaiter::TS_RUN, "invariant");
    assert(owner_raw() == current, "invariant");
    assert(_succ != current, "invariant");
  } // OSThreadWaitState()

  current->set_current_waiting_monitor(NULL);

  guarantee(_recursions == 0, "invariant");
  _recursions = save      // restore the old recursion count
                + JvmtiDeferredUpdates::get_and_reset_relock_count_after_wait(current); //  increased by the deferred relock count
  _waiters--;             // decrement the number of waiters

  // Verify a few postconditions
  assert(owner_raw() == current, "invariant");
  assert(_succ != current, "invariant");
  assert(object()->mark() == markWord::encode(this), "invariant");

  // check if the notification happened
  if (!WasNotified) {
    // no, it could be timeout or Thread.interrupt() or both
    // check for interrupt event, otherwise it is timeout
    if (interruptible && current->is_interrupted(true) && !HAS_PENDING_EXCEPTION) {
      THROW(vmSymbols::java_lang_InterruptedException());
    }
  }

  // NOTE: Spurious wake up will be consider as timeout.
  // Monitor notify has precedence over thread interrupt.
}


// Consider:
// If the lock is cool (cxq == null && succ == null) and we're on an MP system
// then instead of transferring a thread from the WaitSet to the EntryList
// we might just dequeue a thread from the WaitSet and directly unpark() it.

void ObjectMonitor::INotify(JavaThread* current) {
  Thread::SpinAcquire(&_WaitSetLock, "WaitSet - notify");
  ObjectWaiter* iterator = DequeueWaiter();
  if (iterator != NULL) {
    guarantee(iterator->TState == ObjectWaiter::TS_WAIT, "invariant");
    guarantee(iterator->_notified == 0, "invariant");
    // Disposition - what might we do with iterator ?
    // a.  add it directly to the EntryList - either tail (policy == 1)
    //     or head (policy == 0).
    // b.  push it onto the front of the _cxq (policy == 2).
    // For now we use (b).

    iterator->TState = ObjectWaiter::TS_ENTER;

    iterator->_notified = 1;
    iterator->_notifier_tid = JFR_THREAD_ID(current);

    ObjectWaiter* list = _EntryList;
    if (list != NULL) {
      assert(list->_prev == NULL, "invariant");
      assert(list->TState == ObjectWaiter::TS_ENTER, "invariant");
      assert(list != iterator, "invariant");
    }

    // prepend to cxq
    if (list == NULL) {
      iterator->_next = iterator->_prev = NULL;
      _EntryList = iterator;
    } else {
      iterator->TState = ObjectWaiter::TS_CXQ;
      for (;;) {
        ObjectWaiter* front = _cxq;
        iterator->_next = front;
        if (Atomic::cmpxchg(&_cxq, front, iterator) == front) {
          break;
        }
      }
    }

    // _WaitSetLock protects the wait queue, not the EntryList.  We could
    // move the add-to-EntryList operation, above, outside the critical section
    // protected by _WaitSetLock.  In practice that's not useful.  With the
    // exception of  wait() timeouts and interrupts the monitor owner
    // is the only thread that grabs _WaitSetLock.  There's almost no contention
    // on _WaitSetLock so it's not profitable to reduce the length of the
    // critical section.

    iterator->wait_reenter_begin(this);
  }
  Thread::SpinRelease(&_WaitSetLock);
}

// Consider: a not-uncommon synchronization bug is to use notify() when
// notifyAll() is more appropriate, potentially resulting in stranded
// threads; this is one example of a lost wakeup. A useful diagnostic
// option is to force all notify() operations to behave as notifyAll().
//
// Note: We can also detect many such problems with a "minimum wait".
// When the "minimum wait" is set to a small non-zero timeout value
// and the program does not hang whereas it did absent "minimum wait",
// that suggests a lost wakeup bug.

void ObjectMonitor::notify(TRAPS) {
  JavaThread* current = THREAD;
  CHECK_OWNER();  // Throws IMSE if not owner.
  if (_WaitSet == NULL) {
    return;
  }
  DTRACE_MONITOR_PROBE(notify, this, object(), current);
  INotify(current);
  OM_PERFDATA_OP(Notifications, inc(1));
}


// The current implementation of notifyAll() transfers the waiters one-at-a-time
// from the waitset to the EntryList. This could be done more efficiently with a
// single bulk transfer but in practice it's not time-critical. Beware too,
// that in prepend-mode we invert the order of the waiters. Let's say that the
// waitset is "ABCD" and the EntryList is "XYZ". After a notifyAll() in prepend
// mode the waitset will be empty and the EntryList will be "DCBAXYZ".

void ObjectMonitor::notifyAll(TRAPS) {
  JavaThread* current = THREAD;
  CHECK_OWNER();  // Throws IMSE if not owner.
  if (_WaitSet == NULL) {
    return;
  }

  DTRACE_MONITOR_PROBE(notifyAll, this, object(), current);
  int tally = 0;
  while (_WaitSet != NULL) {
    tally++;
    INotify(current);
  }

  OM_PERFDATA_OP(Notifications, inc(tally));
}

// -----------------------------------------------------------------------------
// Adaptive Spinning Support
//
// Adaptive spin-then-block - rational spinning
//
// Note that we spin "globally" on _owner with a classic SMP-polite TATAS
// algorithm.  On high order SMP systems it would be better to start with
// a brief global spin and then revert to spinning locally.  In the spirit of MCS/CLH,
// a contending thread could enqueue itself on the cxq and then spin locally
// on a thread-specific variable such as its ParkEvent._Event flag.
// That's left as an exercise for the reader.  Note that global spinning is
// not problematic on Niagara, as the L2 cache serves the interconnect and
// has both low latency and massive bandwidth.
//
// Broadly, we can fix the spin frequency -- that is, the % of contended lock
// acquisition attempts where we opt to spin --  at 100% and vary the spin count
// (duration) or we can fix the count at approximately the duration of
// a context switch and vary the frequency.   Of course we could also
// vary both satisfying K == Frequency * Duration, where K is adaptive by monitor.
// For a description of 'Adaptive spin-then-block mutual exclusion in
// multi-threaded processing,' see U.S. Pat. No. 8046758.
//
// This implementation varies the duration "D", where D varies with
// the success rate of recent spin attempts. (D is capped at approximately
// length of a round-trip context switch).  The success rate for recent
// spin attempts is a good predictor of the success rate of future spin
// attempts.  The mechanism adapts automatically to varying critical
// section length (lock modality), system load and degree of parallelism.
// D is maintained per-monitor in _SpinDuration and is initialized
// optimistically.  Spin frequency is fixed at 100%.
//
// Note that _SpinDuration is volatile, but we update it without locks
// or atomics.  The code is designed so that _SpinDuration stays within
// a reasonable range even in the presence of races.  The arithmetic
// operations on _SpinDuration are closed over the domain of legal values,
// so at worst a race will install and older but still legal value.
// At the very worst this introduces some apparent non-determinism.
// We might spin when we shouldn't or vice-versa, but since the spin
// count are relatively short, even in the worst case, the effect is harmless.
//
// Care must be taken that a low "D" value does not become an
// an absorbing state.  Transient spinning failures -- when spinning
// is overall profitable -- should not cause the system to converge
// on low "D" values.  We want spinning to be stable and predictable
// and fairly responsive to change and at the same time we don't want
// it to oscillate, become metastable, be "too" non-deterministic,
// or converge on or enter undesirable stable absorbing states.
//
// We implement a feedback-based control system -- using past behavior
// to predict future behavior.  We face two issues: (a) if the
// input signal is random then the spin predictor won't provide optimal
// results, and (b) if the signal frequency is too high then the control
// system, which has some natural response lag, will "chase" the signal.
// (b) can arise from multimodal lock hold times.  Transient preemption
// can also result in apparent bimodal lock hold times.
// Although sub-optimal, neither condition is particularly harmful, as
// in the worst-case we'll spin when we shouldn't or vice-versa.
// The maximum spin duration is rather short so the failure modes aren't bad.
// To be conservative, I've tuned the gain in system to bias toward
// _not spinning.  Relatedly, the system can sometimes enter a mode where it
// "rings" or oscillates between spinning and not spinning.  This happens
// when spinning is just on the cusp of profitability, however, so the
// situation is not dire.  The state is benign -- there's no need to add
// hysteresis control to damp the transition rate between spinning and
// not spinning.

// Spinning: Fixed frequency (100%), vary duration
int ObjectMonitor::TrySpin(JavaThread* current) {
  // Dumb, brutal spin.  Good for comparative measurements against adaptive spinning.
  int ctr = Knob_FixedSpin;
  if (ctr != 0) {
    while (--ctr >= 0) {
      if (TryLock(current) > 0) return 1;
      SpinPause();
    }
    return 0;
  }

  for (ctr = Knob_PreSpin + 1; --ctr >= 0;) {
    if (TryLock(current) > 0) {
      // Increase _SpinDuration ...
      // Note that we don't clamp SpinDuration precisely at SpinLimit.
      // Raising _SpurDuration to the poverty line is key.
      int x = _SpinDuration;
      if (x < Knob_SpinLimit) {
        if (x < Knob_Poverty) x = Knob_Poverty;
        _SpinDuration = x + Knob_BonusB;
      }
      return 1;
    }
    SpinPause();
  }

  // Admission control - verify preconditions for spinning
  //
  // We always spin a little bit, just to prevent _SpinDuration == 0 from
  // becoming an absorbing state.  Put another way, we spin briefly to
  // sample, just in case the system load, parallelism, contention, or lock
  // modality changed.
  //
  // Consider the following alternative:
  // Periodically set _SpinDuration = _SpinLimit and try a long/full
  // spin attempt.  "Periodically" might mean after a tally of
  // the # of failed spin attempts (or iterations) reaches some threshold.
  // This takes us into the realm of 1-out-of-N spinning, where we
  // hold the duration constant but vary the frequency.

  ctr = _SpinDuration;
  if (ctr <= 0) return 0;

  if (NotRunnable(current, (JavaThread*) owner_raw())) {
    return 0;
  }

  // We're good to spin ... spin ingress.
  // CONSIDER: use Prefetch::write() to avoid RTS->RTO upgrades
  // when preparing to LD...CAS _owner, etc and the CAS is likely
  // to succeed.
  if (_succ == NULL) {
    _succ = current;
  }
  Thread* prv = NULL;

  // There are three ways to exit the following loop:
  // 1.  A successful spin where this thread has acquired the lock.
  // 2.  Spin failure with prejudice
  // 3.  Spin failure without prejudice

  while (--ctr >= 0) {

    // Periodic polling -- Check for pending GC
    // Threads may spin while they're unsafe.
    // We don't want spinning threads to delay the JVM from reaching
    // a stop-the-world safepoint or to steal cycles from GC.
    // If we detect a pending safepoint we abort in order that
    // (a) this thread, if unsafe, doesn't delay the safepoint, and (b)
    // this thread, if safe, doesn't steal cycles from GC.
    // This is in keeping with the "no loitering in runtime" rule.
    // We periodically check to see if there's a safepoint pending.
    if ((ctr & 0xFF) == 0) {
      if (SafepointMechanism::should_process(current)) {
        goto Abort;           // abrupt spin egress
      }
      SpinPause();
    }

    // Probe _owner with TATAS
    // If this thread observes the monitor transition or flicker
    // from locked to unlocked to locked, then the odds that this
    // thread will acquire the lock in this spin attempt go down
    // considerably.  The same argument applies if the CAS fails
    // or if we observe _owner change from one non-null value to
    // another non-null value.   In such cases we might abort
    // the spin without prejudice or apply a "penalty" to the
    // spin count-down variable "ctr", reducing it by 100, say.

    JavaThread* ox = (JavaThread*) owner_raw();
    if (ox == NULL) {
      ox = (JavaThread*)try_set_owner_from(NULL, current);
      if (ox == NULL) {
        // The CAS succeeded -- this thread acquired ownership
        // Take care of some bookkeeping to exit spin state.
        if (_succ == current) {
          _succ = NULL;
        }

        // Increase _SpinDuration :
        // The spin was successful (profitable) so we tend toward
        // longer spin attempts in the future.
        // CONSIDER: factor "ctr" into the _SpinDuration adjustment.
        // If we acquired the lock early in the spin cycle it
        // makes sense to increase _SpinDuration proportionally.
        // Note that we don't clamp SpinDuration precisely at SpinLimit.
        int x = _SpinDuration;
        if (x < Knob_SpinLimit) {
          if (x < Knob_Poverty) x = Knob_Poverty;
          _SpinDuration = x + Knob_Bonus;
        }
        return 1;
      }

      // The CAS failed ... we can take any of the following actions:
      // * penalize: ctr -= CASPenalty
      // * exit spin with prejudice -- goto Abort;
      // * exit spin without prejudice.
      // * Since CAS is high-latency, retry again immediately.
      prv = ox;
      goto Abort;
    }

    // Did lock ownership change hands ?
    if (ox != prv && prv != NULL) {
      goto Abort;
    }
    prv = ox;

    // Abort the spin if the owner is not executing.
    // The owner must be executing in order to drop the lock.
    // Spinning while the owner is OFFPROC is idiocy.
    // Consider: ctr -= RunnablePenalty ;
    if (NotRunnable(current, ox)) {
      goto Abort;
    }
    if (_succ == NULL) {
      _succ = current;
    }
  }

  // Spin failed with prejudice -- reduce _SpinDuration.
  // TODO: Use an AIMD-like policy to adjust _SpinDuration.
  // AIMD is globally stable.
  {
    int x = _SpinDuration;
    if (x > 0) {
      // Consider an AIMD scheme like: x -= (x >> 3) + 100
      // This is globally sample and tends to damp the response.
      x -= Knob_Penalty;
      if (x < 0) x = 0;
      _SpinDuration = x;
    }
  }

 Abort:
  if (_succ == current) {
    _succ = NULL;
    // Invariant: after setting succ=null a contending thread
    // must recheck-retry _owner before parking.  This usually happens
    // in the normal usage of TrySpin(), but it's safest
    // to make TrySpin() as foolproof as possible.
    OrderAccess::fence();
    if (TryLock(current) > 0) return 1;
  }
  return 0;
}

// NotRunnable() -- informed spinning
//
// Don't bother spinning if the owner is not eligible to drop the lock.
// Spin only if the owner thread is _thread_in_Java or _thread_in_vm.
// The thread must be runnable in order to drop the lock in timely fashion.
// If the _owner is not runnable then spinning will not likely be
// successful (profitable).
//
// Beware -- the thread referenced by _owner could have died
// so a simply fetch from _owner->_thread_state might trap.
// Instead, we use SafeFetchXX() to safely LD _owner->_thread_state.
// Because of the lifecycle issues, the _thread_state values
// observed by NotRunnable() might be garbage.  NotRunnable must
// tolerate this and consider the observed _thread_state value
// as advisory.
//
// Beware too, that _owner is sometimes a BasicLock address and sometimes
// a thread pointer.
// Alternately, we might tag the type (thread pointer vs basiclock pointer)
// with the LSB of _owner.  Another option would be to probabilistically probe
// the putative _owner->TypeTag value.
//
// Checking _thread_state isn't perfect.  Even if the thread is
// in_java it might be blocked on a page-fault or have been preempted
// and sitting on a ready/dispatch queue.
//
// The return value from NotRunnable() is *advisory* -- the
// result is based on sampling and is not necessarily coherent.
// The caller must tolerate false-negative and false-positive errors.
// Spinning, in general, is probabilistic anyway.


int ObjectMonitor::NotRunnable(JavaThread* current, JavaThread* ox) {
  // Check ox->TypeTag == 2BAD.
  if (ox == NULL) return 0;

  // Avoid transitive spinning ...
  // Say T1 spins or blocks trying to acquire L.  T1._Stalled is set to L.
  // Immediately after T1 acquires L it's possible that T2, also
  // spinning on L, will see L.Owner=T1 and T1._Stalled=L.
  // This occurs transiently after T1 acquired L but before
  // T1 managed to clear T1.Stalled.  T2 does not need to abort
  // its spin in this circumstance.
  intptr_t BlockedOn = SafeFetchN((intptr_t *) &ox->_Stalled, intptr_t(1));

  if (BlockedOn == 1) return 1;
  if (BlockedOn != 0) {
    return BlockedOn != intptr_t(this) && owner_raw() == ox;
  }

  assert(sizeof(ox->_thread_state == sizeof(int)), "invariant");
  int jst = SafeFetch32((int *) &ox->_thread_state, -1);;
  // consider also: jst != _thread_in_Java -- but that's overspecific.
  return jst == _thread_blocked || jst == _thread_in_native;
}


// -----------------------------------------------------------------------------
// WaitSet management ...

ObjectWaiter::ObjectWaiter(JavaThread* current) {
  _next     = NULL;
  _prev     = NULL;
  _notified = 0;
  _notifier_tid = 0;
  TState    = TS_RUN;
  _thread   = current;
  _event    = _thread->_ParkEvent;
  _active   = false;
  assert(_event != NULL, "invariant");
}

void ObjectWaiter::wait_reenter_begin(ObjectMonitor * const mon) {
  _active = JavaThreadBlockedOnMonitorEnterState::wait_reenter_begin(_thread, mon);
}

void ObjectWaiter::wait_reenter_end(ObjectMonitor * const mon) {
  JavaThreadBlockedOnMonitorEnterState::wait_reenter_end(_thread, _active);
}

inline void ObjectMonitor::AddWaiter(ObjectWaiter* node) {
  assert(node != NULL, "should not add NULL node");
  assert(node->_prev == NULL, "node already in list");
  assert(node->_next == NULL, "node already in list");
  // put node at end of queue (circular doubly linked list)
  if (_WaitSet == NULL) {
    _WaitSet = node;
    node->_prev = node;
    node->_next = node;
  } else {
    ObjectWaiter* head = _WaitSet;
    ObjectWaiter* tail = head->_prev;
    assert(tail->_next == head, "invariant check");
    tail->_next = node;
    head->_prev = node;
    node->_next = head;
    node->_prev = tail;
  }
}

inline ObjectWaiter* ObjectMonitor::DequeueWaiter() {
  // dequeue the very first waiter
  ObjectWaiter* waiter = _WaitSet;
  if (waiter) {
    DequeueSpecificWaiter(waiter);
  }
  return waiter;
}

inline void ObjectMonitor::DequeueSpecificWaiter(ObjectWaiter* node) {
  assert(node != NULL, "should not dequeue NULL node");
  assert(node->_prev != NULL, "node already removed from list");
  assert(node->_next != NULL, "node already removed from list");
  // when the waiter has woken up because of interrupt,
  // timeout or other spurious wake-up, dequeue the
  // waiter from waiting list
  ObjectWaiter* next = node->_next;
  if (next == node) {
    assert(node->_prev == node, "invariant check");
    _WaitSet = NULL;
  } else {
    ObjectWaiter* prev = node->_prev;
    assert(prev->_next == node, "invariant check");
    assert(next->_prev == node, "invariant check");
    next->_prev = prev;
    prev->_next = next;
    if (_WaitSet == node) {
      _WaitSet = next;
    }
  }
  node->_next = NULL;
  node->_prev = NULL;
}

// -----------------------------------------------------------------------------
// PerfData support
PerfCounter * ObjectMonitor::_sync_ContendedLockAttempts       = NULL;
PerfCounter * ObjectMonitor::_sync_FutileWakeups               = NULL;
PerfCounter * ObjectMonitor::_sync_Parks                       = NULL;
PerfCounter * ObjectMonitor::_sync_Notifications               = NULL;
PerfCounter * ObjectMonitor::_sync_Inflations                  = NULL;
PerfCounter * ObjectMonitor::_sync_Deflations                  = NULL;
PerfLongVariable * ObjectMonitor::_sync_MonExtant              = NULL;

// One-shot global initialization for the sync subsystem.
// We could also defer initialization and initialize on-demand
// the first time we call ObjectSynchronizer::inflate().
// Initialization would be protected - like so many things - by
// the MonitorCache_lock.

void ObjectMonitor::Initialize() {
  assert(!InitDone, "invariant");

  if (!os::is_MP()) {
    Knob_SpinLimit = 0;
    Knob_PreSpin   = 0;
    Knob_FixedSpin = -1;
  }

  if (UsePerfData) {
    EXCEPTION_MARK;
#define NEWPERFCOUNTER(n)                                                \
  {                                                                      \
    n = PerfDataManager::create_counter(SUN_RT, #n, PerfData::U_Events,  \
                                        CHECK);                          \
  }
#define NEWPERFVARIABLE(n)                                                \
  {                                                                       \
    n = PerfDataManager::create_variable(SUN_RT, #n, PerfData::U_Events,  \
                                         CHECK);                          \
  }
    NEWPERFCOUNTER(_sync_Inflations);
    NEWPERFCOUNTER(_sync_Deflations);
    NEWPERFCOUNTER(_sync_ContendedLockAttempts);
    NEWPERFCOUNTER(_sync_FutileWakeups);
    NEWPERFCOUNTER(_sync_Parks);
    NEWPERFCOUNTER(_sync_Notifications);
    NEWPERFVARIABLE(_sync_MonExtant);
#undef NEWPERFCOUNTER
#undef NEWPERFVARIABLE
  }

  _oop_storage = OopStorageSet::create_weak("ObjectSynchronizer Weak", mtSynchronizer);

  DEBUG_ONLY(InitDone = true;)
}

void ObjectMonitor::print_on(outputStream* st) const {
  // The minimal things to print for markWord printing, more can be added for debugging and logging.
  st->print("{contentions=0x%08x,waiters=0x%08x"
            ",recursions=" INTX_FORMAT ",owner=" INTPTR_FORMAT "}",
            contentions(), waiters(), recursions(),
            p2i(owner()));
}
void ObjectMonitor::print() const { print_on(tty); }

#ifdef ASSERT
// Print the ObjectMonitor like a debugger would:
//
// (ObjectMonitor) 0x00007fdfb6012e40 = {
//   _header = 0x0000000000000001
//   _object = 0x000000070ff45fd0
//   _pad_buf0 = {
//     [0] = '\0'
//     ...
//     [43] = '\0'
//   }
//   _owner = 0x0000000000000000
//   _previous_owner_tid = 0
//   _pad_buf1 = {
//     [0] = '\0'
//     ...
//     [47] = '\0'
//   }
//   _next_om = 0x0000000000000000
//   _recursions = 0
//   _EntryList = 0x0000000000000000
//   _cxq = 0x0000000000000000
//   _succ = 0x0000000000000000
//   _Responsible = 0x0000000000000000
//   _Spinner = 0
//   _SpinDuration = 5000
//   _contentions = 0
//   _WaitSet = 0x0000700009756248
//   _waiters = 1
//   _WaitSetLock = 0
// }
//
void ObjectMonitor::print_debug_style_on(outputStream* st) const {
  st->print_cr("(ObjectMonitor*) " INTPTR_FORMAT " = {", p2i(this));
  st->print_cr("  _header = " INTPTR_FORMAT, header().value());
  st->print_cr("  _object = " INTPTR_FORMAT, p2i(object_peek()));
  st->print_cr("  _pad_buf0 = {");
  st->print_cr("    [0] = '\\0'");
  st->print_cr("    ...");
  st->print_cr("    [%d] = '\\0'", (int)sizeof(_pad_buf0) - 1);
  st->print_cr("  }");
  st->print_cr("  _owner = " INTPTR_FORMAT, p2i(owner_raw()));
  st->print_cr("  _previous_owner_tid = " UINT64_FORMAT, _previous_owner_tid);
  st->print_cr("  _pad_buf1 = {");
  st->print_cr("    [0] = '\\0'");
  st->print_cr("    ...");
  st->print_cr("    [%d] = '\\0'", (int)sizeof(_pad_buf1) - 1);
  st->print_cr("  }");
  st->print_cr("  _next_om = " INTPTR_FORMAT, p2i(next_om()));
  st->print_cr("  _recursions = " INTX_FORMAT, _recursions);
  st->print_cr("  _EntryList = " INTPTR_FORMAT, p2i(_EntryList));
  st->print_cr("  _cxq = " INTPTR_FORMAT, p2i(_cxq));
  st->print_cr("  _succ = " INTPTR_FORMAT, p2i(_succ));
  st->print_cr("  _Responsible = " INTPTR_FORMAT, p2i(_Responsible));
  st->print_cr("  _Spinner = %d", _Spinner);
  st->print_cr("  _SpinDuration = %d", _SpinDuration);
  st->print_cr("  _contentions = %d", contentions());
  st->print_cr("  _WaitSet = " INTPTR_FORMAT, p2i(_WaitSet));
  st->print_cr("  _waiters = %d", _waiters);
  st->print_cr("  _WaitSetLock = %d", _WaitSetLock);
  st->print_cr("}");
}
#endif
