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
#include "gc/shared/gc_globals.hpp"
#include "memory/universe.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.inline.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vmThread.hpp"

// Mutexes used in the VM (see comment in mutexLocker.hpp):
//
// Note that the following pointers are effectively final -- after having been
// set at JVM startup-time, they should never be subsequently mutated.
// Instead of using pointers to malloc()ed monitors and mutexes we should consider
// eliminating the indirection and using instances instead.
// Consider using GCC's __read_mostly.

Mutex*   Patching_lock                = NULL;
Mutex*   CompiledMethod_lock          = NULL;
Monitor* SystemDictionary_lock        = NULL;
Mutex*   SharedDictionary_lock        = NULL;
Mutex*   Module_lock                  = NULL;
Mutex*   CompiledIC_lock              = NULL;
Mutex*   InlineCacheBuffer_lock       = NULL;
Mutex*   VMStatistic_lock             = NULL;
Mutex*   JNIHandleBlockFreeList_lock  = NULL;
Mutex*   JmethodIdCreation_lock       = NULL;
Mutex*   JfieldIdCreation_lock        = NULL;
Monitor* JNICritical_lock             = NULL;
Mutex*   JvmtiThreadState_lock        = NULL;
Monitor* EscapeBarrier_lock           = NULL;
Monitor* Heap_lock                    = NULL;
Mutex*   ExpandHeap_lock              = NULL;
Mutex*   AdapterHandlerLibrary_lock   = NULL;
Mutex*   SignatureHandlerLibrary_lock = NULL;
Mutex*   VtableStubs_lock             = NULL;
Mutex*   SymbolArena_lock             = NULL;
Monitor* StringDedup_lock             = NULL;
Mutex*   StringDedupIntern_lock       = NULL;
Monitor* CodeCache_lock               = NULL;
Monitor* CodeSweeper_lock             = NULL;
Mutex*   MethodData_lock              = NULL;
Mutex*   TouchedMethodLog_lock        = NULL;
Mutex*   RetData_lock                 = NULL;
Monitor* VMOperation_lock             = NULL;
Monitor* Threads_lock                 = NULL;
Mutex*   NonJavaThreadsList_lock      = NULL;
Mutex*   NonJavaThreadsListSync_lock  = NULL;
Monitor* CGC_lock                     = NULL;
Monitor* STS_lock                     = NULL;
Monitor* G1OldGCCount_lock            = NULL;
Mutex*   Shared_DirtyCardQ_lock       = NULL;
Mutex*   G1DetachedRefinementStats_lock = NULL;
Mutex*   MarkStackFreeList_lock       = NULL;
Mutex*   MarkStackChunkList_lock      = NULL;
Mutex*   MonitoringSupport_lock       = NULL;
Mutex*   ParGCRareEvent_lock          = NULL;
Monitor* ConcurrentGCBreakpoints_lock = NULL;
Mutex*   Compile_lock                 = NULL;
Monitor* MethodCompileQueue_lock      = NULL;
Monitor* CompileThread_lock           = NULL;
Monitor* Compilation_lock             = NULL;
Mutex*   CompileTaskAlloc_lock        = NULL;
Mutex*   CompileStatistics_lock       = NULL;
Mutex*   DirectivesStack_lock         = NULL;
Mutex*   MultiArray_lock              = NULL;
Monitor* Terminator_lock              = NULL;
Monitor* InitCompleted_lock           = NULL;
Monitor* BeforeExit_lock              = NULL;
Monitor* Notify_lock                  = NULL;
Mutex*   ProfilePrint_lock            = NULL;
Mutex*   ExceptionCache_lock          = NULL;
Mutex*   NMethodSweeperStats_lock     = NULL;
#ifndef PRODUCT
Mutex*   FullGCALot_lock              = NULL;
#endif

Mutex*   Debug1_lock                  = NULL;
Mutex*   Debug2_lock                  = NULL;
Mutex*   Debug3_lock                  = NULL;

Mutex*   tty_lock                     = NULL;

Mutex*   RawMonitor_lock              = NULL;
Mutex*   PerfDataMemAlloc_lock        = NULL;
Mutex*   PerfDataManager_lock         = NULL;
Mutex*   OopMapCacheAlloc_lock        = NULL;

Mutex*   FreeList_lock                = NULL;
Mutex*   OldSets_lock                 = NULL;
Mutex*   Uncommit_lock                = NULL;
Monitor* RootRegionScan_lock          = NULL;

Mutex*   Management_lock              = NULL;
Monitor* MonitorDeflation_lock        = NULL;
Monitor* Service_lock                 = NULL;
Monitor* Notification_lock            = NULL;
Monitor* PeriodicTask_lock            = NULL;
Monitor* RedefineClasses_lock         = NULL;
Mutex*   Verify_lock                  = NULL;
Monitor* Zip_lock                     = NULL;

#if INCLUDE_JFR
Mutex*   JfrStacktrace_lock           = NULL;
Monitor* JfrMsg_lock                  = NULL;
Mutex*   JfrBuffer_lock               = NULL;
Mutex*   JfrStream_lock               = NULL;
Monitor* JfrThreadSampler_lock        = NULL;
#endif

#ifndef SUPPORTS_NATIVE_CX8
Mutex*   UnsafeJlong_lock             = NULL;
#endif
Mutex*   CodeHeapStateAnalytics_lock  = NULL;

Mutex*   Metaspace_lock               = NULL;
Mutex*   ClassLoaderDataGraph_lock    = NULL;
Monitor* ThreadsSMRDelete_lock        = NULL;
Mutex*   ThreadIdTableCreate_lock     = NULL;
Mutex*   SharedDecoder_lock           = NULL;
Mutex*   DCmdFactory_lock             = NULL;
#if INCLUDE_NMT
Mutex*   NMTQuery_lock                = NULL;
#endif
#if INCLUDE_CDS
#if INCLUDE_JVMTI
Mutex*   CDSClassFileStream_lock      = NULL;
#endif
Mutex*   DumpTimeTable_lock           = NULL;
Mutex*   CDSLambda_lock               = NULL;
Mutex*   DumpRegion_lock              = NULL;
Mutex*   ClassListFile_lock           = NULL;
Mutex*   UnregisteredClassesTable_lock= NULL;
Mutex*   LambdaFormInvokers_lock      = NULL;
#endif // INCLUDE_CDS
Mutex*   Bootclasspath_lock           = NULL;

#if INCLUDE_JVMCI
Monitor* JVMCI_lock                   = NULL;
#endif


#define MAX_NUM_MUTEX 128
static Mutex* _mutex_array[MAX_NUM_MUTEX];
static int _num_mutex;

#ifdef ASSERT
void assert_locked_or_safepoint(const Mutex* lock) {
  // check if this thread owns the lock (common case)
  assert(lock != NULL, "Need non-NULL lock");
  if (lock->owned_by_self()) return;
  if (SafepointSynchronize::is_at_safepoint()) return;
  if (!Universe::is_fully_initialized()) return;
  fatal("must own lock %s", lock->name());
}

// a weaker assertion than the above
void assert_locked_or_safepoint_weak(const Mutex* lock) {
  assert(lock != NULL, "Need non-NULL lock");
  if (lock->is_locked()) return;
  if (SafepointSynchronize::is_at_safepoint()) return;
  if (!Universe::is_fully_initialized()) return;
  fatal("must own lock %s", lock->name());
}

// a stronger assertion than the above
void assert_lock_strong(const Mutex* lock) {
  assert(lock != NULL, "Need non-NULL lock");
  if (lock->owned_by_self()) return;
  fatal("must own lock %s", lock->name());
}

void assert_locked_or_safepoint_or_handshake(const Mutex* lock, const JavaThread* thread) {
  if (thread->is_handshake_safe_for(Thread::current())) return;
  assert_locked_or_safepoint(lock);
}
#endif

#define def(var, type, pri, vm_block, safepoint_check_allowed ) {      \
  var = new type(Mutex::pri, #var, vm_block, Mutex::safepoint_check_allowed); \
  assert(_num_mutex < MAX_NUM_MUTEX, "increase MAX_NUM_MUTEX");        \
  _mutex_array[_num_mutex++] = var;                                      \
}

// Using Padded subclasses to prevent false sharing of these global monitors and mutexes.
void mutex_init() {
  def(tty_lock                     , PaddedMutex  , tty,         true,  _safepoint_check_never);      // allow to lock in VM

  def(CGC_lock                     , PaddedMonitor, special,     true,  _safepoint_check_never);      // coordinate between fore- and background GC
  def(STS_lock                     , PaddedMonitor, leaf,        true,  _safepoint_check_never);

  if (UseG1GC) {
    def(G1OldGCCount_lock          , PaddedMonitor, leaf,        true,  _safepoint_check_always);

    def(Shared_DirtyCardQ_lock     , PaddedMutex  , access + 1,  true,  _safepoint_check_never);

    def(G1DetachedRefinementStats_lock, PaddedMutex, leaf    ,   true, _safepoint_check_never);

    def(FreeList_lock              , PaddedMutex  , leaf     ,   true,  _safepoint_check_never);
    def(OldSets_lock               , PaddedMutex  , leaf     ,   true,  _safepoint_check_never);
    def(Uncommit_lock              , PaddedMutex  , leaf + 1 ,   true,  _safepoint_check_never);
    def(RootRegionScan_lock        , PaddedMonitor, leaf     ,   true,  _safepoint_check_never);

    def(MarkStackFreeList_lock     , PaddedMutex  , leaf     ,   true,  _safepoint_check_never);
    def(MarkStackChunkList_lock    , PaddedMutex  , leaf     ,   true,  _safepoint_check_never);

    def(MonitoringSupport_lock     , PaddedMutex  , native   ,   true,  _safepoint_check_never);      // used for serviceability monitoring support
  }
  def(StringDedup_lock             , PaddedMonitor, leaf,        true,  _safepoint_check_never);
  def(StringDedupIntern_lock       , PaddedMutex  , leaf,        true,  _safepoint_check_never);
  def(ParGCRareEvent_lock          , PaddedMutex  , leaf,        true,  _safepoint_check_always);
  def(CodeCache_lock               , PaddedMonitor, special,     true,  _safepoint_check_never);
  def(CodeSweeper_lock             , PaddedMonitor, special-2,   true,  _safepoint_check_never);
  def(RawMonitor_lock              , PaddedMutex  , special,     true,  _safepoint_check_never);
  def(OopMapCacheAlloc_lock        , PaddedMutex  , leaf,        true,  _safepoint_check_always); // used for oop_map_cache allocation.

  def(Metaspace_lock               , PaddedMutex  , leaf-1,      true,  _safepoint_check_never);
  def(ClassLoaderDataGraph_lock    , PaddedMutex  , nonleaf,     false, _safepoint_check_always);

  def(Patching_lock                , PaddedMutex  , special,     true,  _safepoint_check_never);      // used for safepointing and code patching.
  def(CompiledMethod_lock          , PaddedMutex  , special-1,   true,  _safepoint_check_never);
  def(MonitorDeflation_lock        , PaddedMonitor, tty-2,       true,  _safepoint_check_never);      // used for monitor deflation thread operations
  def(Service_lock                 , PaddedMonitor, tty-2,       true,  _safepoint_check_never);      // used for service thread operations

  if (UseNotificationThread) {
    def(Notification_lock            , PaddedMonitor, special,     true,  _safepoint_check_never);  // used for notification thread operations
  } else {
    Notification_lock = Service_lock;
  }

  def(JmethodIdCreation_lock       , PaddedMutex  , special-2,   true,  _safepoint_check_never); // used for creating jmethodIDs.

  def(SystemDictionary_lock        , PaddedMonitor, leaf,        true,  _safepoint_check_always);
  def(SharedDictionary_lock        , PaddedMutex  , leaf,        true,  _safepoint_check_always);
  def(Module_lock                  , PaddedMutex  , leaf+2,      false, _safepoint_check_always);
  def(InlineCacheBuffer_lock       , PaddedMutex  , leaf,        true,  _safepoint_check_never);
  def(VMStatistic_lock             , PaddedMutex  , leaf,        false, _safepoint_check_always);
  def(ExpandHeap_lock              , PaddedMutex  , leaf,        true,  _safepoint_check_always); // Used during compilation by VM thread
  def(JNIHandleBlockFreeList_lock  , PaddedMutex  , leaf-1,      true,  _safepoint_check_never);      // handles are used by VM thread
  def(SignatureHandlerLibrary_lock , PaddedMutex  , leaf,        false, _safepoint_check_always);
  def(SymbolArena_lock             , PaddedMutex  , leaf+2,      true,  _safepoint_check_never);
  def(ProfilePrint_lock            , PaddedMutex  , leaf,        false, _safepoint_check_always); // serial profile printing
  def(ExceptionCache_lock          , PaddedMutex  , leaf,        false, _safepoint_check_always); // serial profile printing
  def(Debug1_lock                  , PaddedMutex  , leaf,        true,  _safepoint_check_never);
#ifndef PRODUCT
  def(FullGCALot_lock              , PaddedMutex  , leaf,        false, _safepoint_check_always); // a lock to make FullGCALot MT safe
#endif
  def(BeforeExit_lock              , PaddedMonitor, leaf,        true,  _safepoint_check_always);
  def(PerfDataMemAlloc_lock        , PaddedMutex  , leaf,        true,  _safepoint_check_always); // used for allocating PerfData memory for performance data
  def(PerfDataManager_lock         , PaddedMutex  , leaf,        true,  _safepoint_check_always); // used for synchronized access to PerfDataManager resources

  def(Threads_lock                 , PaddedMonitor, barrier,     true,  _safepoint_check_always);  // Used for safepoint protocol.
  def(NonJavaThreadsList_lock      , PaddedMutex,   barrier,     true,  _safepoint_check_never);
  def(NonJavaThreadsListSync_lock  , PaddedMutex,   leaf,        true,  _safepoint_check_never);

  def(VMOperation_lock             , PaddedMonitor, nonleaf,     true,  _safepoint_check_always);  // VM_thread allowed to block on these
  def(RetData_lock                 , PaddedMutex  , nonleaf,     false, _safepoint_check_always);
  def(Terminator_lock              , PaddedMonitor, nonleaf,     true,  _safepoint_check_always);
  def(InitCompleted_lock           , PaddedMonitor, leaf,        true,  _safepoint_check_never);
  def(VtableStubs_lock             , PaddedMutex  , nonleaf,     true,  _safepoint_check_never);
  def(Notify_lock                  , PaddedMonitor, nonleaf,     true,  _safepoint_check_always);
  def(JNICritical_lock             , PaddedMonitor, nonleaf,     true,  _safepoint_check_always); // used for JNI critical regions
  def(AdapterHandlerLibrary_lock   , PaddedMutex  , nonleaf,     true,  _safepoint_check_always);

  def(Heap_lock                    , PaddedMonitor, nonleaf+1,   false, _safepoint_check_always); // Doesn't safepoint check during termination.
  def(JfieldIdCreation_lock        , PaddedMutex  , nonleaf+1,   true,  _safepoint_check_always); // jfieldID, Used in VM_Operation

  def(CompiledIC_lock              , PaddedMutex  , nonleaf+2,   false, _safepoint_check_never);      // locks VtableStubs_lock, InlineCacheBuffer_lock
  def(CompileTaskAlloc_lock        , PaddedMutex  , nonleaf+2,   true,  _safepoint_check_always);
  def(CompileStatistics_lock       , PaddedMutex  , nonleaf+2,   false, _safepoint_check_always);
  def(DirectivesStack_lock         , PaddedMutex  , special,     true,  _safepoint_check_never);
  def(MultiArray_lock              , PaddedMutex  , nonleaf+2,   false, _safepoint_check_always);

  def(JvmtiThreadState_lock        , PaddedMutex  , nonleaf+2,   false, _safepoint_check_always); // Used by JvmtiThreadState/JvmtiEventController
  def(EscapeBarrier_lock           , PaddedMonitor, leaf,        false, _safepoint_check_never);  // Used to synchronize object reallocation/relocking triggered by JVMTI
  def(Management_lock              , PaddedMutex  , nonleaf+2,   false, _safepoint_check_always); // used for JVM management

  def(ConcurrentGCBreakpoints_lock , PaddedMonitor, nonleaf,     true,  _safepoint_check_always);
  def(Compile_lock                 , PaddedMutex  , nonleaf+3,   false, _safepoint_check_always);
  def(MethodData_lock              , PaddedMutex  , nonleaf+3,   false, _safepoint_check_always);
  def(TouchedMethodLog_lock        , PaddedMutex  , nonleaf+3,   false, _safepoint_check_always);

  def(MethodCompileQueue_lock      , PaddedMonitor, nonleaf+4,   false, _safepoint_check_always);
  def(Debug2_lock                  , PaddedMutex  , nonleaf+4,   true,  _safepoint_check_never);
  def(Debug3_lock                  , PaddedMutex  , nonleaf+4,   true,  _safepoint_check_never);
  def(CompileThread_lock           , PaddedMonitor, nonleaf+5,   false, _safepoint_check_always);
  def(PeriodicTask_lock            , PaddedMonitor, nonleaf+5,   true,  _safepoint_check_always);
  def(RedefineClasses_lock         , PaddedMonitor, nonleaf+5,   true,  _safepoint_check_always);
  def(Verify_lock                  , PaddedMutex,   nonleaf+5,   true,  _safepoint_check_always);
  def(Zip_lock                     , PaddedMonitor, leaf,        true,  _safepoint_check_never);

  if (WhiteBoxAPI) {
    def(Compilation_lock           , PaddedMonitor, leaf,        false, _safepoint_check_never);
  }

#if INCLUDE_JFR
  def(JfrMsg_lock                  , PaddedMonitor, leaf,        true,  _safepoint_check_always);
  def(JfrBuffer_lock               , PaddedMutex  , leaf,        true,  _safepoint_check_never);
  def(JfrStream_lock               , PaddedMutex  , nonleaf + 1, false, _safepoint_check_never);
  def(JfrStacktrace_lock           , PaddedMutex  , tty-2,       true,  _safepoint_check_never);
  def(JfrThreadSampler_lock        , PaddedMonitor, leaf,        true,  _safepoint_check_never);
#endif

#ifndef SUPPORTS_NATIVE_CX8
  def(UnsafeJlong_lock             , PaddedMutex  , special,     false, _safepoint_check_never);
#endif

  def(CodeHeapStateAnalytics_lock  , PaddedMutex  , nonleaf+6,   false, _safepoint_check_always);
  def(NMethodSweeperStats_lock     , PaddedMutex  , special,     true,  _safepoint_check_never);
  def(ThreadsSMRDelete_lock        , PaddedMonitor, special,     true,  _safepoint_check_never);
  def(ThreadIdTableCreate_lock     , PaddedMutex  , leaf,        false, _safepoint_check_always);
  def(SharedDecoder_lock           , PaddedMutex  , native,      true,  _safepoint_check_never);
  def(DCmdFactory_lock             , PaddedMutex  , leaf,        true,  _safepoint_check_never);
#if INCLUDE_NMT
  def(NMTQuery_lock                , PaddedMutex  , max_nonleaf, false, _safepoint_check_always);
#endif
#if INCLUDE_CDS
#if INCLUDE_JVMTI
  def(CDSClassFileStream_lock      , PaddedMutex  , max_nonleaf, false, _safepoint_check_always);
#endif
  def(DumpTimeTable_lock           , PaddedMutex  , leaf - 1,    true,  _safepoint_check_never);
  def(CDSLambda_lock               , PaddedMutex  , leaf,        true,  _safepoint_check_never);
  def(DumpRegion_lock              , PaddedMutex  , leaf,        true,  _safepoint_check_never);
  def(ClassListFile_lock           , PaddedMutex  , leaf,        true,  _safepoint_check_never);
  def(LambdaFormInvokers_lock      , PaddedMutex  , nonleaf+2,   false, _safepoint_check_always);
#endif // INCLUDE_CDS
  def(Bootclasspath_lock           , PaddedMutex  , leaf,        false, _safepoint_check_never);

#if INCLUDE_JVMCI
  def(JVMCI_lock                   , PaddedMonitor, nonleaf+2,   true,  _safepoint_check_always);
#endif
}

GCMutexLocker::GCMutexLocker(Mutex* mutex) {
  if (SafepointSynchronize::is_at_safepoint()) {
    _locked = false;
  } else {
    _mutex = mutex;
    _locked = true;
    _mutex->lock();
  }
}

// Print all mutexes/monitors that are currently owned by a thread; called
// by fatal error handler.
void print_owned_locks_on_error(outputStream* st) {
  st->print("VM Mutex/Monitor currently owned by a thread: ");
  bool none = true;
  for (int i = 0; i < _num_mutex; i++) {
     // see if it has an owner
     if (_mutex_array[i]->owner() != NULL) {
       if (none) {
          // print format used by Mutex::print_on_error()
          st->print_cr(" ([mutex/lock_event])");
          none = false;
       }
       _mutex_array[i]->print_on_error(st);
       st->cr();
     }
  }
  if (none) st->print_cr("None");
}
