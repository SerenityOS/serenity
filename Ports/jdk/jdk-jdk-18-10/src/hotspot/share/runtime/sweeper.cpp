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
#include "code/codeCache.hpp"
#include "code/compiledIC.hpp"
#include "code/icBuffer.hpp"
#include "code/nmethod.hpp"
#include "compiler/compileBroker.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/workgroup.hpp"
#include "jfr/jfrEvents.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/method.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/handshake.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/os.hpp"
#include "runtime/sweeper.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vmOperations.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/events.hpp"
#include "utilities/xmlstream.hpp"

#ifdef ASSERT

#define SWEEP(nm) record_sweep(nm, __LINE__)
// Sweeper logging code
class SweeperRecord {
 public:
  int traversal;
  int compile_id;
  long traversal_mark;
  int state;
  const char* kind;
  address vep;
  address uep;
  int line;

  void print() {
      tty->print_cr("traversal = %d compile_id = %d %s uep = " PTR_FORMAT " vep = "
                    PTR_FORMAT " state = %d traversal_mark %ld line = %d",
                    traversal,
                    compile_id,
                    kind == NULL ? "" : kind,
                    p2i(uep),
                    p2i(vep),
                    state,
                    traversal_mark,
                    line);
  }
};

static int _sweep_index = 0;
static SweeperRecord* _records = NULL;

void NMethodSweeper::record_sweep(CompiledMethod* nm, int line) {
  if (_records != NULL) {
    _records[_sweep_index].traversal = _traversals;
    _records[_sweep_index].traversal_mark = nm->is_nmethod() ? ((nmethod*)nm)->stack_traversal_mark() : 0;
    _records[_sweep_index].compile_id = nm->compile_id();
    _records[_sweep_index].kind = nm->compile_kind();
    _records[_sweep_index].state = nm->get_state();
    _records[_sweep_index].vep = nm->verified_entry_point();
    _records[_sweep_index].uep = nm->entry_point();
    _records[_sweep_index].line = line;
    _sweep_index = (_sweep_index + 1) % SweeperLogEntries;
  }
}

void NMethodSweeper::init_sweeper_log() {
 if (LogSweeper && _records == NULL) {
   // Create the ring buffer for the logging code
   _records = NEW_C_HEAP_ARRAY(SweeperRecord, SweeperLogEntries, mtGC);
   memset(_records, 0, sizeof(SweeperRecord) * SweeperLogEntries);
  }
}
#else
#define SWEEP(nm)
#endif

CompiledMethodIterator NMethodSweeper::_current(CompiledMethodIterator::all_blobs); // Current compiled method
long     NMethodSweeper::_traversals                   = 0;    // Stack scan count, also sweep ID.
long     NMethodSweeper::_total_nof_code_cache_sweeps  = 0;    // Total number of full sweeps of the code cache
int      NMethodSweeper::_seen                         = 0;    // Nof. nmethod we have currently processed in current pass of CodeCache
size_t   NMethodSweeper::_sweep_threshold_bytes        = 0;    // Threshold for when to sweep. Updated after ergonomics

volatile bool NMethodSweeper::_should_sweep            = false;// Indicates if a normal sweep will be done
volatile bool NMethodSweeper::_force_sweep             = false;// Indicates if a forced sweep will be done
volatile size_t NMethodSweeper::_bytes_changed         = 0;    // Counts the total nmethod size if the nmethod changed from:
                                                               //   1) alive       -> not_entrant
                                                               //   2) not_entrant -> zombie
int    NMethodSweeper::_hotness_counter_reset_val       = 0;

long   NMethodSweeper::_total_nof_methods_reclaimed     = 0;   // Accumulated nof methods flushed
long   NMethodSweeper::_total_nof_c2_methods_reclaimed  = 0;   // Accumulated nof methods flushed
size_t NMethodSweeper::_total_flushed_size              = 0;   // Total number of bytes flushed from the code cache
Tickspan NMethodSweeper::_total_time_sweeping;                 // Accumulated time sweeping
Tickspan NMethodSweeper::_total_time_this_sweep;               // Total time this sweep
Tickspan NMethodSweeper::_peak_sweep_time;                     // Peak time for a full sweep
Tickspan NMethodSweeper::_peak_sweep_fraction_time;            // Peak time sweeping one fraction

class MarkActivationClosure: public CodeBlobClosure {
public:
  virtual void do_code_blob(CodeBlob* cb) {
    assert(cb->is_nmethod(), "CodeBlob should be nmethod");
    nmethod* nm = (nmethod*)cb;
    nm->set_hotness_counter(NMethodSweeper::hotness_counter_reset_val());
    // If we see an activation belonging to a non_entrant nmethod, we mark it.
    if (nm->is_not_entrant()) {
      nm->mark_as_seen_on_stack();
    }
  }
};
static MarkActivationClosure mark_activation_closure;

int NMethodSweeper::hotness_counter_reset_val() {
  if (_hotness_counter_reset_val == 0) {
    _hotness_counter_reset_val = (ReservedCodeCacheSize < M) ? 1 : (ReservedCodeCacheSize / M) * 2;
  }
  return _hotness_counter_reset_val;
}
bool NMethodSweeper::wait_for_stack_scanning() {
  return _current.end();
}

class NMethodMarkingClosure : public HandshakeClosure {
private:
  CodeBlobClosure* _cl;
public:
  NMethodMarkingClosure(CodeBlobClosure* cl) : HandshakeClosure("NMethodMarking"), _cl(cl) {}
  void do_thread(Thread* thread) {
    if (thread->is_Java_thread() && ! thread->is_Code_cache_sweeper_thread()) {
      JavaThread::cast(thread)->nmethods_do(_cl);
    }
  }
};

CodeBlobClosure* NMethodSweeper::prepare_mark_active_nmethods() {
#ifdef ASSERT
  assert(Thread::current()->is_Code_cache_sweeper_thread(), "must be executed under CodeCache_lock and in sweeper thread");
  assert_lock_strong(CodeCache_lock);
#endif

  // If we do not want to reclaim not-entrant or zombie methods there is no need
  // to scan stacks
  if (!MethodFlushing) {
    return NULL;
  }

  // Check for restart
  assert(_current.method() == NULL, "should only happen between sweeper cycles");
  assert(wait_for_stack_scanning(), "should only happen between sweeper cycles");

  _seen = 0;
  _current = CompiledMethodIterator(CompiledMethodIterator::all_blobs);
  // Initialize to first nmethod
  _current.next();
  _traversals += 1;
  _total_time_this_sweep = Tickspan();

  if (PrintMethodFlushing) {
    tty->print_cr("### Sweep: stack traversal %ld", _traversals);
  }
  return &mark_activation_closure;
}

/**
  * This function triggers a VM operation that does stack scanning of active
  * methods. Stack scanning is mandatory for the sweeper to make progress.
  */
void NMethodSweeper::do_stack_scanning() {
  assert(!CodeCache_lock->owned_by_self(), "just checking");
  if (wait_for_stack_scanning()) {
    CodeBlobClosure* code_cl;
    {
      MutexLocker ccl(CodeCache_lock, Mutex::_no_safepoint_check_flag);
      code_cl = prepare_mark_active_nmethods();
    }
    if (code_cl != NULL) {
      NMethodMarkingClosure nm_cl(code_cl);
      Handshake::execute(&nm_cl);
    }
  }
}

void NMethodSweeper::sweeper_loop() {
  bool timeout;
  while (true) {
    {
      ThreadBlockInVM tbivm(JavaThread::current());
      MonitorLocker waiter(CodeSweeper_lock, Mutex::_no_safepoint_check_flag);
      const long wait_time = 60*60*24 * 1000;
      timeout = waiter.wait(wait_time);
    }
    if (!timeout && (_should_sweep || _force_sweep)) {
      sweep();
    }
  }
}

/**
  * Wakes up the sweeper thread to sweep if code cache space runs low
  */
void NMethodSweeper::report_allocation(int code_blob_type) {
  if (should_start_aggressive_sweep(code_blob_type)) {
    MonitorLocker waiter(CodeSweeper_lock, Mutex::_no_safepoint_check_flag);
    _should_sweep = true;
    CodeSweeper_lock->notify();
  }
}

bool NMethodSweeper::should_start_aggressive_sweep(int code_blob_type) {
  // Makes sure that we do not invoke the sweeper too often during startup.
  double start_threshold = 100.0 / (double)StartAggressiveSweepingAt;
  double aggressive_sweep_threshold = MAX2(start_threshold, 1.1);
  return (CodeCache::reverse_free_ratio(code_blob_type) >= aggressive_sweep_threshold);
}

/**
  * Wakes up the sweeper thread and forces a sweep. Blocks until it finished.
  */
void NMethodSweeper::force_sweep() {
  ThreadBlockInVM tbivm(JavaThread::current());
  MonitorLocker waiter(CodeSweeper_lock, Mutex::_no_safepoint_check_flag);
  // Request forced sweep
  _force_sweep = true;
  while (_force_sweep) {
    // Notify sweeper that we want to force a sweep and wait for completion.
    // In case a sweep currently takes place we timeout and try again because
    // we want to enforce a full sweep.
    CodeSweeper_lock->notify();
    waiter.wait(1000);
  }
}

/**
 * Handle a safepoint request
 */
void NMethodSweeper::handle_safepoint_request() {
  JavaThread* thread = JavaThread::current();
  if (SafepointMechanism::should_process(thread)) {
    if (PrintMethodFlushing && Verbose) {
      tty->print_cr("### Sweep at %d out of %d, yielding to safepoint", _seen, CodeCache::nmethod_count());
    }
    MutexUnlocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);

    ThreadBlockInVM tbivm(thread);
  }
}

void NMethodSweeper::sweep() {
  assert(_should_sweep || _force_sweep, "must have been set");
  assert(JavaThread::current()->thread_state() == _thread_in_vm, "must run in vm mode");
  Atomic::store(&_bytes_changed, static_cast<size_t>(0)); // reset regardless of sleep reason
  if (_should_sweep) {
    MutexLocker mu(CodeSweeper_lock, Mutex::_no_safepoint_check_flag);
    _should_sweep = false;
  }

  do_stack_scanning();

  init_sweeper_log();
  sweep_code_cache();

  // We are done with sweeping the code cache once.
  _total_nof_code_cache_sweeps++;

  if (_force_sweep) {
    // Notify requester that forced sweep finished
    MutexLocker mu(CodeSweeper_lock, Mutex::_no_safepoint_check_flag);
    _force_sweep = false;
    CodeSweeper_lock->notify();
  }
}

static void post_sweep_event(EventSweepCodeCache* event,
                             const Ticks& start,
                             const Ticks& end,
                             s4 traversals,
                             int swept,
                             int flushed,
                             int zombified) {
  assert(event != NULL, "invariant");
  assert(event->should_commit(), "invariant");
  event->set_starttime(start);
  event->set_endtime(end);
  event->set_sweepId(traversals);
  event->set_sweptCount(swept);
  event->set_flushedCount(flushed);
  event->set_zombifiedCount(zombified);
  event->commit();
}

void NMethodSweeper::sweep_code_cache() {
  ResourceMark rm;
  Ticks sweep_start_counter = Ticks::now();

  log_debug(codecache, sweep, start)("CodeCache flushing");

  int flushed_count                = 0;
  int zombified_count              = 0;
  int flushed_c2_count     = 0;

  if (PrintMethodFlushing && Verbose) {
    tty->print_cr("### Sweep at %d out of %d", _seen, CodeCache::nmethod_count());
  }

  int swept_count = 0;
  assert(!SafepointSynchronize::is_at_safepoint(), "should not be in safepoint when we get here");
  assert(!CodeCache_lock->owned_by_self(), "just checking");

  int freed_memory = 0;
  {
    MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);

    while (!_current.end()) {
      swept_count++;
      // Since we will give up the CodeCache_lock, always skip ahead
      // to the next nmethod.  Other blobs can be deleted by other
      // threads but nmethods are only reclaimed by the sweeper.
      CompiledMethod* nm = _current.method();
      _current.next();

      // Now ready to process nmethod and give up CodeCache_lock
      {
        MutexUnlocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
        // Save information before potentially flushing the nmethod
        // Only flushing nmethods so size only matters for them.
        int size = nm->is_nmethod() ? ((nmethod*)nm)->total_size() : 0;
        bool is_c2_method = nm->is_compiled_by_c2();
        bool is_osr = nm->is_osr_method();
        int compile_id = nm->compile_id();
        intptr_t address = p2i(nm);
        const char* state_before = nm->state();
        const char* state_after = "";

        MethodStateChange type = process_compiled_method(nm);
        switch (type) {
          case Flushed:
            state_after = "flushed";
            freed_memory += size;
            ++flushed_count;
            if (is_c2_method) {
              ++flushed_c2_count;
            }
            break;
          case MadeZombie:
            state_after = "made zombie";
            ++zombified_count;
            break;
          case None:
            break;
          default:
           ShouldNotReachHere();
        }
        if (PrintMethodFlushing && Verbose && type != None) {
          tty->print_cr("### %s nmethod %3d/" PTR_FORMAT " (%s) %s", is_osr ? "osr" : "", compile_id, address, state_before, state_after);
        }
      }

      _seen++;
      handle_safepoint_request();
    }
  }

  assert(_current.end(), "must have scanned the whole cache");

  const Ticks sweep_end_counter = Ticks::now();
  const Tickspan sweep_time = sweep_end_counter - sweep_start_counter;
  {
    MutexLocker mu(NMethodSweeperStats_lock, Mutex::_no_safepoint_check_flag);
    _total_time_sweeping  += sweep_time;
    _total_time_this_sweep += sweep_time;
    _peak_sweep_fraction_time = MAX2(sweep_time, _peak_sweep_fraction_time);
    _total_flushed_size += freed_memory;
    _total_nof_methods_reclaimed += flushed_count;
    _total_nof_c2_methods_reclaimed += flushed_c2_count;
    _peak_sweep_time = MAX2(_peak_sweep_time, _total_time_this_sweep);
  }

  EventSweepCodeCache event(UNTIMED);
  if (event.should_commit()) {
    post_sweep_event(&event, sweep_start_counter, sweep_end_counter, (s4)_traversals, swept_count, flushed_count, zombified_count);
  }

#ifdef ASSERT
  if(PrintMethodFlushing) {
    tty->print_cr("### sweeper:      sweep time(" JLONG_FORMAT "): ", sweep_time.value());
  }
#endif

  Log(codecache, sweep) log;
  if (log.is_debug()) {
    LogStream ls(log.debug());
    CodeCache::print_summary(&ls, false);
  }
  log_sweep("finished");

  // Sweeper is the only case where memory is released, check here if it
  // is time to restart the compiler. Only checking if there is a certain
  // amount of free memory in the code cache might lead to re-enabling
  // compilation although no memory has been released. For example, there are
  // cases when compilation was disabled although there is 4MB (or more) free
  // memory in the code cache. The reason is code cache fragmentation. Therefore,
  // it only makes sense to re-enable compilation if we have actually freed memory.
  // Note that typically several kB are released for sweeping 16MB of the code
  // cache. As a result, 'freed_memory' > 0 to restart the compiler.
  if (!CompileBroker::should_compile_new_jobs() && (freed_memory > 0)) {
    CompileBroker::set_should_compile_new_jobs(CompileBroker::run_compilation);
    log.debug("restart compiler");
    log_sweep("restart_compiler");
  }
}

 // This function updates the sweeper statistics that keep track of nmethods
 // state changes. If there is 'enough' state change, the sweeper is invoked
 // as soon as possible. Also, we are guaranteed to invoke the sweeper if
 // the code cache gets full.
void NMethodSweeper::report_state_change(nmethod* nm) {
  Atomic::add(&_bytes_changed, (size_t)nm->total_size());
  if (Atomic::load(&_bytes_changed) > _sweep_threshold_bytes) {
    MutexLocker mu(CodeSweeper_lock, Mutex::_no_safepoint_check_flag);
    _should_sweep = true;
    CodeSweeper_lock->notify(); // Wake up sweeper.
  }
}

class CompiledMethodMarker: public StackObj {
 private:
  CodeCacheSweeperThread* _thread;
 public:
  CompiledMethodMarker(CompiledMethod* cm) {
    JavaThread* current = JavaThread::current();
    assert (current->is_Code_cache_sweeper_thread(), "Must be");
    _thread = (CodeCacheSweeperThread*)current;
    if (!cm->is_zombie() && !cm->is_unloading()) {
      // Only expose live nmethods for scanning
      _thread->set_scanned_compiled_method(cm);
    }
  }
  ~CompiledMethodMarker() {
    _thread->set_scanned_compiled_method(NULL);
  }
};

NMethodSweeper::MethodStateChange NMethodSweeper::process_compiled_method(CompiledMethod* cm) {
  assert(cm != NULL, "sanity");
  assert(!CodeCache_lock->owned_by_self(), "just checking");

  MethodStateChange result = None;
  // Make sure this nmethod doesn't get unloaded during the scan,
  // since safepoints may happen during acquired below locks.
  CompiledMethodMarker nmm(cm);
  SWEEP(cm);

  // Skip methods that are currently referenced by the VM
  if (cm->is_locked_by_vm()) {
    // But still remember to clean-up inline caches for alive nmethods
    if (cm->is_alive()) {
      // Clean inline caches that point to zombie/non-entrant/unloaded nmethods
      cm->cleanup_inline_caches(false);
      SWEEP(cm);
    }
    return result;
  }

  if (cm->is_zombie()) {
    // All inline caches that referred to this nmethod were cleaned in the
    // previous sweeper cycle. Now flush the nmethod from the code cache.
    assert(!cm->is_locked_by_vm(), "must not flush locked Compiled Methods");
    cm->flush();
    assert(result == None, "sanity");
    result = Flushed;
  } else if (cm->is_not_entrant()) {
    // If there are no current activations of this method on the
    // stack we can safely convert it to a zombie method
    OrderAccess::loadload(); // _stack_traversal_mark and _state
    if (cm->can_convert_to_zombie()) {
      // Code cache state change is tracked in make_zombie()
      cm->make_zombie();
      SWEEP(cm);
      assert(result == None, "sanity");
      result = MadeZombie;
      assert(cm->is_zombie(), "nmethod must be zombie");
    } else {
      // Still alive, clean up its inline caches
      cm->cleanup_inline_caches(false);
      SWEEP(cm);
    }
  } else if (cm->is_unloaded()) {
    // Code is unloaded, so there are no activations on the stack.
    // Convert the nmethod to zombie.
    // Code cache state change is tracked in make_zombie()
    cm->make_zombie();
    SWEEP(cm);
    assert(result == None, "sanity");
    result = MadeZombie;
  } else {
    if (cm->is_nmethod()) {
      possibly_flush((nmethod*)cm);
    }
    // Clean inline caches that point to zombie/non-entrant/unloaded nmethods
    cm->cleanup_inline_caches(false);
    SWEEP(cm);
  }
  return result;
}


void NMethodSweeper::possibly_flush(nmethod* nm) {
  if (UseCodeCacheFlushing) {
    if (!nm->is_locked_by_vm() && !nm->is_native_method() && !nm->is_not_installed() && !nm->is_unloading()) {
      bool make_not_entrant = false;

      // Do not make native methods not-entrant
      nm->dec_hotness_counter();
      // Get the initial value of the hotness counter. This value depends on the
      // ReservedCodeCacheSize
      int reset_val = hotness_counter_reset_val();
      int time_since_reset = reset_val - nm->hotness_counter();
      int code_blob_type = CodeCache::get_code_blob_type(nm);
      double threshold = -reset_val + (CodeCache::reverse_free_ratio(code_blob_type) * NmethodSweepActivity);
      // The less free space in the code cache we have - the bigger reverse_free_ratio() is.
      // I.e., 'threshold' increases with lower available space in the code cache and a higher
      // NmethodSweepActivity. If the current hotness counter - which decreases from its initial
      // value until it is reset by stack walking - is smaller than the computed threshold, the
      // corresponding nmethod is considered for removal.
      if ((NmethodSweepActivity > 0) && (nm->hotness_counter() < threshold) && (time_since_reset > MinPassesBeforeFlush)) {
        // A method is marked as not-entrant if the method is
        // 1) 'old enough': nm->hotness_counter() < threshold
        // 2) The method was in_use for a minimum amount of time: (time_since_reset > MinPassesBeforeFlush)
        //    The second condition is necessary if we are dealing with very small code cache
        //    sizes (e.g., <10m) and the code cache size is too small to hold all hot methods.
        //    The second condition ensures that methods are not immediately made not-entrant
        //    after compilation.
        make_not_entrant = true;
      }

      // The stack-scanning low-cost detection may not see the method was used (which can happen for
      // flat profiles). Check the age counter for possible data.
      if (UseCodeAging && make_not_entrant && (nm->is_compiled_by_c2() || nm->is_compiled_by_c1())) {
        MethodCounters* mc = nm->method()->get_method_counters(Thread::current());
        if (mc != NULL) {
          // Snapshot the value as it's changed concurrently
          int age = mc->nmethod_age();
          if (MethodCounters::is_nmethod_hot(age)) {
            // The method has gone through flushing, and it became relatively hot that it deopted
            // before we could take a look at it. Give it more time to appear in the stack traces,
            // proportional to the number of deopts.
            MethodData* md = nm->method()->method_data();
            if (md != NULL && time_since_reset > (int)(MinPassesBeforeFlush * (md->tenure_traps() + 1))) {
              // It's been long enough, we still haven't seen it on stack.
              // Try to flush it, but enable counters the next time.
              mc->reset_nmethod_age();
            } else {
              make_not_entrant = false;
            }
          } else if (MethodCounters::is_nmethod_warm(age)) {
            // Method has counters enabled, and the method was used within
            // previous MinPassesBeforeFlush sweeps. Reset the counter. Stay in the existing
            // compiled state.
            mc->reset_nmethod_age();
            // delay the next check
            nm->set_hotness_counter(NMethodSweeper::hotness_counter_reset_val());
            make_not_entrant = false;
          } else if (MethodCounters::is_nmethod_age_unset(age)) {
            // No counters were used before. Set the counters to the detection
            // limit value. If the method is going to be used again it will be compiled
            // with counters that we're going to use for analysis the the next time.
            mc->reset_nmethod_age();
          } else {
            // Method was totally idle for 10 sweeps
            // The counter already has the initial value, flush it and may be recompile
            // later with counters
          }
        }
      }

      if (make_not_entrant) {
        nm->make_not_entrant();

        // Code cache state change is tracked in make_not_entrant()
        if (PrintMethodFlushing && Verbose) {
          tty->print_cr("### Nmethod %d/" PTR_FORMAT "made not-entrant: hotness counter %d/%d threshold %f",
              nm->compile_id(), p2i(nm), nm->hotness_counter(), reset_val, threshold);
        }
      }
    }
  }
}

// Print out some state information about the current sweep and the
// state of the code cache if it's requested.
void NMethodSweeper::log_sweep(const char* msg, const char* format, ...) {
  if (PrintMethodFlushing) {
    ResourceMark rm;
    stringStream s;
    // Dump code cache state into a buffer before locking the tty,
    // because log_state() will use locks causing lock conflicts.
    CodeCache::log_state(&s);

    ttyLocker ttyl;
    tty->print("### sweeper: %s ", msg);
    if (format != NULL) {
      va_list ap;
      va_start(ap, format);
      tty->vprint(format, ap);
      va_end(ap);
    }
    tty->print_cr("%s", s.as_string());
  }

  if (LogCompilation && (xtty != NULL)) {
    ResourceMark rm;
    stringStream s;
    // Dump code cache state into a buffer before locking the tty,
    // because log_state() will use locks causing lock conflicts.
    CodeCache::log_state(&s);

    ttyLocker ttyl;
    xtty->begin_elem("sweeper state='%s' traversals='" INTX_FORMAT "' ", msg, (intx)traversal_count());
    if (format != NULL) {
      va_list ap;
      va_start(ap, format);
      xtty->vprint(format, ap);
      va_end(ap);
    }
    xtty->print("%s", s.as_string());
    xtty->stamp();
    xtty->end_elem();
  }
}

void NMethodSweeper::print(outputStream* out) {
  ttyLocker ttyl;
  out = (out == NULL) ? tty : out;
  out->print_cr("Code cache sweeper statistics:");
  out->print_cr("  Total sweep time:                %1.0lf ms", (double)_total_time_sweeping.value()/1000000);
  out->print_cr("  Total number of full sweeps:     %ld", _total_nof_code_cache_sweeps);
  out->print_cr("  Total number of flushed methods: %ld (thereof %ld C2 methods)", _total_nof_methods_reclaimed,
                                                    _total_nof_c2_methods_reclaimed);
  out->print_cr("  Total size of flushed methods:   " SIZE_FORMAT " kB", _total_flushed_size/K);
}
