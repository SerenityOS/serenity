/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "gc/shared/gc_globals.hpp"
#include "gc/z/zAbort.inline.hpp"
#include "gc/z/zCollectedHeap.hpp"
#include "gc/z/zCPU.inline.hpp"
#include "gc/z/zGlobals.hpp"
#include "gc/z/zNMethodTable.hpp"
#include "gc/z/zPageAllocator.inline.hpp"
#include "gc/z/zRelocationSetSelector.inline.hpp"
#include "gc/z/zStat.hpp"
#include "gc/z/zTracer.inline.hpp"
#include "gc/z/zUtils.hpp"
#include "memory/metaspaceUtils.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/atomic.hpp"
#include "runtime/os.hpp"
#include "runtime/timer.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"
#include "utilities/ticks.hpp"

#define ZSIZE_FMT                       SIZE_FORMAT "M(%.0f%%)"
#define ZSIZE_ARGS_WITH_MAX(size, max)  ((size) / M), (percent_of(size, max))
#define ZSIZE_ARGS(size)                ZSIZE_ARGS_WITH_MAX(size, ZStatHeap::max_capacity())

#define ZTABLE_ARGS_NA                  "%9s", "-"
#define ZTABLE_ARGS(size)               SIZE_FORMAT_W(8) "M (%.0f%%)", \
                                        ((size) / M), (percent_of(size, ZStatHeap::max_capacity()))

//
// Stat sampler/counter data
//
struct ZStatSamplerData {
  uint64_t _nsamples;
  uint64_t _sum;
  uint64_t _max;

  ZStatSamplerData() :
    _nsamples(0),
    _sum(0),
    _max(0) {}

  void add(const ZStatSamplerData& new_sample) {
    _nsamples += new_sample._nsamples;
    _sum += new_sample._sum;
    _max = MAX2(_max, new_sample._max);
  }
};

struct ZStatCounterData {
  uint64_t _counter;

  ZStatCounterData() :
    _counter(0) {}
};

//
// Stat sampler history
//
template <size_t size>
class ZStatSamplerHistoryInterval {
private:
  size_t           _next;
  ZStatSamplerData _samples[size];
  ZStatSamplerData _accumulated;
  ZStatSamplerData _total;

public:
  ZStatSamplerHistoryInterval() :
      _next(0),
      _samples(),
      _accumulated(),
      _total() {}

  bool add(const ZStatSamplerData& new_sample) {
    // Insert sample
    const ZStatSamplerData old_sample = _samples[_next];
    _samples[_next] = new_sample;

    // Adjust accumulated
    _accumulated._nsamples += new_sample._nsamples;
    _accumulated._sum += new_sample._sum;
    _accumulated._max = MAX2(_accumulated._max, new_sample._max);

    // Adjust total
    _total._nsamples -= old_sample._nsamples;
    _total._sum -= old_sample._sum;
    _total._nsamples += new_sample._nsamples;
    _total._sum += new_sample._sum;
    if (_total._max < new_sample._max) {
      // Found new max
      _total._max = new_sample._max;
    } else if (_total._max == old_sample._max) {
      // Removed old max, reset and find new max
      _total._max = 0;
      for (size_t i = 0; i < size; i++) {
        if (_total._max < _samples[i]._max) {
          _total._max = _samples[i]._max;
        }
      }
    }

    // Adjust next
    if (++_next == size) {
      _next = 0;

      // Clear accumulated
      const ZStatSamplerData zero;
      _accumulated = zero;

      // Became full
      return true;
    }

    // Not yet full
    return false;
  }

  const ZStatSamplerData& total() const {
    return _total;
  }

  const ZStatSamplerData& accumulated() const {
    return _accumulated;
  }
};

class ZStatSamplerHistory : public CHeapObj<mtGC> {
private:
  ZStatSamplerHistoryInterval<10> _10seconds;
  ZStatSamplerHistoryInterval<60> _10minutes;
  ZStatSamplerHistoryInterval<60> _10hours;
  ZStatSamplerData                _total;

  uint64_t avg(uint64_t sum, uint64_t nsamples) const {
    return (nsamples > 0) ? sum / nsamples : 0;
  }

public:
  ZStatSamplerHistory() :
      _10seconds(),
      _10minutes(),
      _10hours(),
      _total() {}

  void add(const ZStatSamplerData& new_sample) {
    if (_10seconds.add(new_sample)) {
      if (_10minutes.add(_10seconds.total())) {
        if (_10hours.add(_10minutes.total())) {
          _total.add(_10hours.total());
        }
      }
    }
  }

  uint64_t avg_10_seconds() const {
    const uint64_t sum      = _10seconds.total()._sum;
    const uint64_t nsamples = _10seconds.total()._nsamples;
    return avg(sum, nsamples);
  }

  uint64_t avg_10_minutes() const {
    const uint64_t sum      = _10seconds.accumulated()._sum +
                              _10minutes.total()._sum;
    const uint64_t nsamples = _10seconds.accumulated()._nsamples +
                              _10minutes.total()._nsamples;
    return avg(sum, nsamples);
  }

  uint64_t avg_10_hours() const {
    const uint64_t sum      = _10seconds.accumulated()._sum +
                              _10minutes.accumulated()._sum +
                              _10hours.total()._sum;
    const uint64_t nsamples = _10seconds.accumulated()._nsamples +
                              _10minutes.accumulated()._nsamples +
                              _10hours.total()._nsamples;
    return avg(sum, nsamples);
  }

  uint64_t avg_total() const {
    const uint64_t sum      = _10seconds.accumulated()._sum +
                              _10minutes.accumulated()._sum +
                              _10hours.accumulated()._sum +
                              _total._sum;
    const uint64_t nsamples = _10seconds.accumulated()._nsamples +
                              _10minutes.accumulated()._nsamples +
                              _10hours.accumulated()._nsamples +
                              _total._nsamples;
    return avg(sum, nsamples);
  }

  uint64_t max_10_seconds() const {
    return _10seconds.total()._max;
  }

  uint64_t max_10_minutes() const {
    return MAX2(_10seconds.accumulated()._max,
                _10minutes.total()._max);
  }

  uint64_t max_10_hours() const {
    return MAX3(_10seconds.accumulated()._max,
                _10minutes.accumulated()._max,
                _10hours.total()._max);
  }

  uint64_t max_total() const {
    return MAX4(_10seconds.accumulated()._max,
                _10minutes.accumulated()._max,
                _10hours.accumulated()._max,
                _total._max);
  }
};

//
// Stat unit printers
//
void ZStatUnitTime(LogTargetHandle log, const ZStatSampler& sampler, const ZStatSamplerHistory& history) {
  log.print(" %10s: %-41s "
            "%9.3f / %-9.3f "
            "%9.3f / %-9.3f "
            "%9.3f / %-9.3f "
            "%9.3f / %-9.3f   ms",
            sampler.group(),
            sampler.name(),
            TimeHelper::counter_to_millis(history.avg_10_seconds()),
            TimeHelper::counter_to_millis(history.max_10_seconds()),
            TimeHelper::counter_to_millis(history.avg_10_minutes()),
            TimeHelper::counter_to_millis(history.max_10_minutes()),
            TimeHelper::counter_to_millis(history.avg_10_hours()),
            TimeHelper::counter_to_millis(history.max_10_hours()),
            TimeHelper::counter_to_millis(history.avg_total()),
            TimeHelper::counter_to_millis(history.max_total()));
}

void ZStatUnitBytes(LogTargetHandle log, const ZStatSampler& sampler, const ZStatSamplerHistory& history) {
  log.print(" %10s: %-41s "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) " "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) " "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) " "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) "   MB",
            sampler.group(),
            sampler.name(),
            history.avg_10_seconds() / M,
            history.max_10_seconds() / M,
            history.avg_10_minutes() / M,
            history.max_10_minutes() / M,
            history.avg_10_hours() / M,
            history.max_10_hours() / M,
            history.avg_total() / M,
            history.max_total() / M);
}

void ZStatUnitThreads(LogTargetHandle log, const ZStatSampler& sampler, const ZStatSamplerHistory& history) {
  log.print(" %10s: %-41s "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) " "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) " "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) " "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) "   threads",
            sampler.group(),
            sampler.name(),
            history.avg_10_seconds(),
            history.max_10_seconds(),
            history.avg_10_minutes(),
            history.max_10_minutes(),
            history.avg_10_hours(),
            history.max_10_hours(),
            history.avg_total(),
            history.max_total());
}

void ZStatUnitBytesPerSecond(LogTargetHandle log, const ZStatSampler& sampler, const ZStatSamplerHistory& history) {
  log.print(" %10s: %-41s "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) " "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) " "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) " "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) "   MB/s",
            sampler.group(),
            sampler.name(),
            history.avg_10_seconds() / M,
            history.max_10_seconds() / M,
            history.avg_10_minutes() / M,
            history.max_10_minutes() / M,
            history.avg_10_hours() / M,
            history.max_10_hours() / M,
            history.avg_total() / M,
            history.max_total() / M);
}

void ZStatUnitOpsPerSecond(LogTargetHandle log, const ZStatSampler& sampler, const ZStatSamplerHistory& history) {
  log.print(" %10s: %-41s "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) " "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) " "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) " "
            UINT64_FORMAT_W(9) " / " UINT64_FORMAT_W(-9) "   ops/s",
            sampler.group(),
            sampler.name(),
            history.avg_10_seconds(),
            history.max_10_seconds(),
            history.avg_10_minutes(),
            history.max_10_minutes(),
            history.avg_10_hours(),
            history.max_10_hours(),
            history.avg_total(),
            history.max_total());
}

//
// Stat value
//
uintptr_t ZStatValue::_base = 0;
uint32_t  ZStatValue::_cpu_offset = 0;

ZStatValue::ZStatValue(const char* group,
                          const char* name,
                          uint32_t id,
                          uint32_t size) :
    _group(group),
    _name(name),
    _id(id),
    _offset(_cpu_offset) {
  assert(_base == 0, "Already initialized");
  _cpu_offset += size;
}

template <typename T>
T* ZStatValue::get_cpu_local(uint32_t cpu) const {
  assert(_base != 0, "Not initialized");
  const uintptr_t cpu_base = _base + (_cpu_offset * cpu);
  const uintptr_t value_addr = cpu_base + _offset;
  return (T*)value_addr;
}

void ZStatValue::initialize() {
  // Finalize and align CPU offset
  _cpu_offset = align_up(_cpu_offset, (uint32_t)ZCacheLineSize);

  // Allocation aligned memory
  const size_t size = _cpu_offset * ZCPU::count();
  _base = ZUtils::alloc_aligned(ZCacheLineSize, size);
}

const char* ZStatValue::group() const {
  return _group;
}

const char* ZStatValue::name() const {
  return _name;
}

uint32_t ZStatValue::id() const {
  return _id;
}

//
// Stat iterable value
//
template <typename T> uint32_t ZStatIterableValue<T>::_count = 0;
template <typename T> T*       ZStatIterableValue<T>::_first = NULL;

template <typename T>
ZStatIterableValue<T>::ZStatIterableValue(const char* group,
                                          const char* name,
                                          uint32_t size) :
    ZStatValue(group, name, _count++, size),
    _next(insert()) {}

template <typename T>
T* ZStatIterableValue<T>::insert() const {
  T* const next = _first;
  _first = (T*)this;
  return next;
}

template <typename T>
void ZStatIterableValue<T>::sort() {
  T* first_unsorted = _first;
  _first = NULL;

  while (first_unsorted != NULL) {
    T* const value = first_unsorted;
    first_unsorted = value->_next;
    value->_next = NULL;

    T** current = &_first;

    while (*current != NULL) {
      // First sort by group, then by name
      const int group_cmp = strcmp((*current)->group(), value->group());
      if ((group_cmp > 0) || (group_cmp == 0 && strcmp((*current)->name(), value->name()) > 0)) {
        break;
      }

      current = &(*current)->_next;
    }
    value->_next = *current;
    *current = value;
  }
}

//
// Stat sampler
//
ZStatSampler::ZStatSampler(const char* group, const char* name, ZStatUnitPrinter printer) :
    ZStatIterableValue<ZStatSampler>(group, name, sizeof(ZStatSamplerData)),
    _printer(printer) {}

ZStatSamplerData* ZStatSampler::get() const {
  return get_cpu_local<ZStatSamplerData>(ZCPU::id());
}

ZStatSamplerData ZStatSampler::collect_and_reset() const {
  ZStatSamplerData all;

  const uint32_t ncpus = ZCPU::count();
  for (uint32_t i = 0; i < ncpus; i++) {
    ZStatSamplerData* const cpu_data = get_cpu_local<ZStatSamplerData>(i);
    if (cpu_data->_nsamples > 0) {
      const uint64_t nsamples = Atomic::xchg(&cpu_data->_nsamples, (uint64_t)0);
      const uint64_t sum = Atomic::xchg(&cpu_data->_sum, (uint64_t)0);
      const uint64_t max = Atomic::xchg(&cpu_data->_max, (uint64_t)0);
      all._nsamples += nsamples;
      all._sum += sum;
      if (all._max < max) {
        all._max = max;
      }
    }
  }

  return all;
}

ZStatUnitPrinter ZStatSampler::printer() const {
  return _printer;
}

//
// Stat counter
//
ZStatCounter::ZStatCounter(const char* group, const char* name, ZStatUnitPrinter printer) :
    ZStatIterableValue<ZStatCounter>(group, name, sizeof(ZStatCounterData)),
    _sampler(group, name, printer) {}

ZStatCounterData* ZStatCounter::get() const {
  return get_cpu_local<ZStatCounterData>(ZCPU::id());
}

void ZStatCounter::sample_and_reset() const {
  uint64_t counter = 0;

  const uint32_t ncpus = ZCPU::count();
  for (uint32_t i = 0; i < ncpus; i++) {
    ZStatCounterData* const cpu_data = get_cpu_local<ZStatCounterData>(i);
    counter += Atomic::xchg(&cpu_data->_counter, (uint64_t)0);
  }

  ZStatSample(_sampler, counter);
}

//
// Stat unsampled counter
//
ZStatUnsampledCounter::ZStatUnsampledCounter(const char* name) :
    ZStatIterableValue<ZStatUnsampledCounter>("Unsampled", name, sizeof(ZStatCounterData)) {}

ZStatCounterData* ZStatUnsampledCounter::get() const {
  return get_cpu_local<ZStatCounterData>(ZCPU::id());
}

ZStatCounterData ZStatUnsampledCounter::collect_and_reset() const {
  ZStatCounterData all;

  const uint32_t ncpus = ZCPU::count();
  for (uint32_t i = 0; i < ncpus; i++) {
    ZStatCounterData* const cpu_data = get_cpu_local<ZStatCounterData>(i);
    all._counter += Atomic::xchg(&cpu_data->_counter, (uint64_t)0);
  }

  return all;
}

//
// Stat MMU (Minimum Mutator Utilization)
//
ZStatMMUPause::ZStatMMUPause() :
    _start(0.0),
    _end(0.0) {}

ZStatMMUPause::ZStatMMUPause(const Ticks& start, const Ticks& end) :
    _start(TimeHelper::counter_to_millis(start.value())),
    _end(TimeHelper::counter_to_millis(end.value())) {}

double ZStatMMUPause::end() const {
  return _end;
}

double ZStatMMUPause::overlap(double start, double end) const {
  const double start_max = MAX2(start, _start);
  const double end_min = MIN2(end, _end);

  if (end_min > start_max) {
    // Overlap found
    return end_min - start_max;
  }

  // No overlap
  return 0.0;
}

size_t ZStatMMU::_next = 0;
size_t ZStatMMU::_npauses = 0;
ZStatMMUPause ZStatMMU::_pauses[200];
double ZStatMMU::_mmu_2ms = 100.0;
double ZStatMMU::_mmu_5ms = 100.0;
double ZStatMMU::_mmu_10ms = 100.0;
double ZStatMMU::_mmu_20ms = 100.0;
double ZStatMMU::_mmu_50ms = 100.0;
double ZStatMMU::_mmu_100ms = 100.0;

const ZStatMMUPause& ZStatMMU::pause(size_t index) {
  return _pauses[(_next - index - 1) % ARRAY_SIZE(_pauses)];
}

double ZStatMMU::calculate_mmu(double time_slice) {
  const double end = pause(0).end();
  const double start = end - time_slice;
  double time_paused = 0.0;

  // Find all overlapping pauses
  for (size_t i = 0; i < _npauses; i++) {
    const double overlap = pause(i).overlap(start, end);
    if (overlap == 0.0) {
      // No overlap
      break;
    }

    time_paused += overlap;
  }

  // Calculate MMU
  const double time_mutator = time_slice - time_paused;
  return percent_of(time_mutator, time_slice);
}

void ZStatMMU::register_pause(const Ticks& start, const Ticks& end) {
  // Add pause
  const size_t index = _next++ % ARRAY_SIZE(_pauses);
  _pauses[index] = ZStatMMUPause(start, end);
  _npauses = MIN2(_npauses + 1, ARRAY_SIZE(_pauses));

  // Recalculate MMUs
  _mmu_2ms    = MIN2(_mmu_2ms,   calculate_mmu(2));
  _mmu_5ms    = MIN2(_mmu_5ms,   calculate_mmu(5));
  _mmu_10ms   = MIN2(_mmu_10ms,  calculate_mmu(10));
  _mmu_20ms   = MIN2(_mmu_20ms,  calculate_mmu(20));
  _mmu_50ms   = MIN2(_mmu_50ms,  calculate_mmu(50));
  _mmu_100ms  = MIN2(_mmu_100ms, calculate_mmu(100));
}

void ZStatMMU::print() {
  log_info(gc, mmu)("MMU: 2ms/%.1f%%, 5ms/%.1f%%, 10ms/%.1f%%, 20ms/%.1f%%, 50ms/%.1f%%, 100ms/%.1f%%",
                    _mmu_2ms, _mmu_5ms, _mmu_10ms, _mmu_20ms, _mmu_50ms, _mmu_100ms);
}

//
// Stat phases
//
ConcurrentGCTimer ZStatPhase::_timer;

ZStatPhase::ZStatPhase(const char* group, const char* name) :
    _sampler(group, name, ZStatUnitTime) {}

void ZStatPhase::log_start(LogTargetHandle log, bool thread) const {
  if (!log.is_enabled()) {
    return;
  }

  if (thread) {
    ResourceMark rm;
    log.print("%s (%s)", name(), Thread::current()->name());
  } else {
    log.print("%s", name());
  }
}

void ZStatPhase::log_end(LogTargetHandle log, const Tickspan& duration, bool thread) const {
  if (!log.is_enabled()) {
    return;
  }

  if (thread) {
    ResourceMark rm;
    log.print("%s (%s) %.3fms", name(), Thread::current()->name(), TimeHelper::counter_to_millis(duration.value()));
  } else {
    log.print("%s %.3fms", name(), TimeHelper::counter_to_millis(duration.value()));
  }
}

ConcurrentGCTimer* ZStatPhase::timer() {
  return &_timer;
}

const char* ZStatPhase::name() const {
  return _sampler.name();
}

ZStatPhaseCycle::ZStatPhaseCycle(const char* name) :
    ZStatPhase("Collector", name) {}

void ZStatPhaseCycle::register_start(const Ticks& start) const {
  timer()->register_gc_start(start);

  ZTracer::tracer()->report_gc_start(ZCollectedHeap::heap()->gc_cause(), start);

  ZCollectedHeap::heap()->print_heap_before_gc();
  ZCollectedHeap::heap()->trace_heap_before_gc(ZTracer::tracer());

  log_info(gc, start)("Garbage Collection (%s)",
                       GCCause::to_string(ZCollectedHeap::heap()->gc_cause()));
}

void ZStatPhaseCycle::register_end(const Ticks& start, const Ticks& end) const {
  if (ZAbort::should_abort()) {
    log_info(gc)("Garbage Collection (%s) Aborted",
                 GCCause::to_string(ZCollectedHeap::heap()->gc_cause()));
    return;
  }

  timer()->register_gc_end(end);

  ZCollectedHeap::heap()->print_heap_after_gc();
  ZCollectedHeap::heap()->trace_heap_after_gc(ZTracer::tracer());

  ZTracer::tracer()->report_gc_end(end, timer()->time_partitions());

  const Tickspan duration = end - start;
  ZStatSample(_sampler, duration.value());

  ZStatLoad::print();
  ZStatMMU::print();
  ZStatMark::print();
  ZStatNMethods::print();
  ZStatMetaspace::print();
  ZStatReferences::print();
  ZStatRelocation::print();
  ZStatHeap::print();

  log_info(gc)("Garbage Collection (%s) " ZSIZE_FMT "->" ZSIZE_FMT,
               GCCause::to_string(ZCollectedHeap::heap()->gc_cause()),
               ZSIZE_ARGS(ZStatHeap::used_at_mark_start()),
               ZSIZE_ARGS(ZStatHeap::used_at_relocate_end()));
}

Tickspan ZStatPhasePause::_max;

ZStatPhasePause::ZStatPhasePause(const char* name) :
    ZStatPhase("Phase", name) {}

const Tickspan& ZStatPhasePause::max() {
  return _max;
}

void ZStatPhasePause::register_start(const Ticks& start) const {
  timer()->register_gc_pause_start(name(), start);

  LogTarget(Debug, gc, phases, start) log;
  log_start(log);
}

void ZStatPhasePause::register_end(const Ticks& start, const Ticks& end) const {
  timer()->register_gc_pause_end(end);

  const Tickspan duration = end - start;
  ZStatSample(_sampler, duration.value());

  // Track max pause time
  if (_max < duration) {
    _max = duration;
  }

  // Track minimum mutator utilization
  ZStatMMU::register_pause(start, end);

  LogTarget(Info, gc, phases) log;
  log_end(log, duration);
}

ZStatPhaseConcurrent::ZStatPhaseConcurrent(const char* name) :
    ZStatPhase("Phase", name) {}

void ZStatPhaseConcurrent::register_start(const Ticks& start) const {
  timer()->register_gc_concurrent_start(name(), start);

  LogTarget(Debug, gc, phases, start) log;
  log_start(log);
}

void ZStatPhaseConcurrent::register_end(const Ticks& start, const Ticks& end) const {
  if (ZAbort::should_abort()) {
    return;
  }

  timer()->register_gc_concurrent_end(end);

  const Tickspan duration = end - start;
  ZStatSample(_sampler, duration.value());

  LogTarget(Info, gc, phases) log;
  log_end(log, duration);
}

ZStatSubPhase::ZStatSubPhase(const char* name) :
    ZStatPhase("Subphase", name) {}

void ZStatSubPhase::register_start(const Ticks& start) const {
  LogTarget(Debug, gc, phases, start) log;
  log_start(log, true /* thread */);
}

void ZStatSubPhase::register_end(const Ticks& start, const Ticks& end) const {
  if (ZAbort::should_abort()) {
    return;
  }

  ZTracer::tracer()->report_thread_phase(name(), start, end);

  const Tickspan duration = end - start;
  ZStatSample(_sampler, duration.value());

  LogTarget(Debug, gc, phases) log;
  log_end(log, duration, true /* thread */);
}

ZStatCriticalPhase::ZStatCriticalPhase(const char* name, bool verbose) :
    ZStatPhase("Critical", name),
    _counter("Critical", name, ZStatUnitOpsPerSecond),
    _verbose(verbose) {}

void ZStatCriticalPhase::register_start(const Ticks& start) const {
  // This is called from sensitive contexts, for example before an allocation stall
  // has been resolved. This means we must not access any oops in here since that
  // could lead to infinite recursion. Without access to the thread name we can't
  // really log anything useful here.
}

void ZStatCriticalPhase::register_end(const Ticks& start, const Ticks& end) const {
  ZTracer::tracer()->report_thread_phase(name(), start, end);

  const Tickspan duration = end - start;
  ZStatSample(_sampler, duration.value());
  ZStatInc(_counter);

  if (_verbose) {
    LogTarget(Info, gc) log;
    log_end(log, duration, true /* thread */);
  } else {
    LogTarget(Debug, gc) log;
    log_end(log, duration, true /* thread */);
  }
}

//
// Stat timer
//
THREAD_LOCAL uint32_t ZStatTimerDisable::_active = 0;

//
// Stat sample/inc
//
void ZStatSample(const ZStatSampler& sampler, uint64_t value) {
  ZStatSamplerData* const cpu_data = sampler.get();
  Atomic::add(&cpu_data->_nsamples, 1u);
  Atomic::add(&cpu_data->_sum, value);

  uint64_t max = cpu_data->_max;
  for (;;) {
    if (max >= value) {
      // Not max
      break;
    }

    const uint64_t new_max = value;
    const uint64_t prev_max = Atomic::cmpxchg(&cpu_data->_max, max, new_max);
    if (prev_max == max) {
      // Success
      break;
    }

    // Retry
    max = prev_max;
  }

  ZTracer::tracer()->report_stat_sampler(sampler, value);
}

void ZStatInc(const ZStatCounter& counter, uint64_t increment) {
  ZStatCounterData* const cpu_data = counter.get();
  const uint64_t value = Atomic::add(&cpu_data->_counter, increment);

  ZTracer::tracer()->report_stat_counter(counter, increment, value);
}

void ZStatInc(const ZStatUnsampledCounter& counter, uint64_t increment) {
  ZStatCounterData* const cpu_data = counter.get();
  Atomic::add(&cpu_data->_counter, increment);
}

//
// Stat allocation rate
//
const ZStatUnsampledCounter ZStatAllocRate::_counter("Allocation Rate");
TruncatedSeq                ZStatAllocRate::_samples(ZStatAllocRate::sample_hz);
TruncatedSeq                ZStatAllocRate::_rate(ZStatAllocRate::sample_hz);

const ZStatUnsampledCounter& ZStatAllocRate::counter() {
  return _counter;
}

uint64_t ZStatAllocRate::sample_and_reset() {
  const ZStatCounterData bytes_per_sample = _counter.collect_and_reset();
  _samples.add(bytes_per_sample._counter);

  const uint64_t bytes_per_second = _samples.sum();
  _rate.add(bytes_per_second);

  return bytes_per_second;
}

double ZStatAllocRate::predict() {
  return _rate.predict_next();
}

double ZStatAllocRate::avg() {
  return _rate.avg();
}

double ZStatAllocRate::sd() {
  return _rate.sd();
}

//
// Stat thread
//
ZStat::ZStat() :
    _metronome(sample_hz) {
  set_name("ZStat");
  create_and_start();
}

void ZStat::sample_and_collect(ZStatSamplerHistory* history) const {
  // Sample counters
  for (const ZStatCounter* counter = ZStatCounter::first(); counter != NULL; counter = counter->next()) {
    counter->sample_and_reset();
  }

  // Collect samples
  for (const ZStatSampler* sampler = ZStatSampler::first(); sampler != NULL; sampler = sampler->next()) {
    ZStatSamplerHistory& sampler_history = history[sampler->id()];
    sampler_history.add(sampler->collect_and_reset());
  }
}

bool ZStat::should_print(LogTargetHandle log) const {
  static uint64_t print_at = ZStatisticsInterval;
  const uint64_t now = os::elapsedTime();

  if (now < print_at) {
    return false;
  }

  print_at = ((now / ZStatisticsInterval) * ZStatisticsInterval) + ZStatisticsInterval;

  return log.is_enabled();
}

void ZStat::print(LogTargetHandle log, const ZStatSamplerHistory* history) const {
  // Print
  log.print("=== Garbage Collection Statistics =======================================================================================================================");
  log.print("                                                             Last 10s              Last 10m              Last 10h                Total");
  log.print("                                                             Avg / Max             Avg / Max             Avg / Max             Avg / Max");

  for (const ZStatSampler* sampler = ZStatSampler::first(); sampler != NULL; sampler = sampler->next()) {
    const ZStatSamplerHistory& sampler_history = history[sampler->id()];
    const ZStatUnitPrinter printer = sampler->printer();
    printer(log, *sampler, sampler_history);
  }

  log.print("=========================================================================================================================================================");
}

void ZStat::run_service() {
  ZStatSamplerHistory* const history = new ZStatSamplerHistory[ZStatSampler::count()];
  LogTarget(Info, gc, stats) log;

  ZStatSampler::sort();

  // Main loop
  while (_metronome.wait_for_tick()) {
    sample_and_collect(history);
    if (should_print(log)) {
      print(log, history);
    }
  }

  delete [] history;
}

void ZStat::stop_service() {
  _metronome.stop();
}

//
// Stat table
//
class ZStatTablePrinter {
private:
  static const size_t _buffer_size = 256;

  const size_t _column0_width;
  const size_t _columnN_width;
  char         _buffer[_buffer_size];

public:
  class ZColumn {
  private:
    char* const  _buffer;
    const size_t _position;
    const size_t _width;
    const size_t _width_next;

    ZColumn next() const {
      // Insert space between columns
      _buffer[_position + _width] = ' ';
      return ZColumn(_buffer, _position + _width + 1, _width_next, _width_next);
    }

    size_t print(size_t position, const char* fmt, va_list va) {
      const int res = jio_vsnprintf(_buffer + position, _buffer_size - position, fmt, va);
      if (res < 0) {
        return 0;
      }

      return (size_t)res;
    }

  public:
    ZColumn(char* buffer, size_t position, size_t width, size_t width_next) :
        _buffer(buffer),
        _position(position),
        _width(width),
        _width_next(width_next) {}

    ZColumn left(const char* fmt, ...) ATTRIBUTE_PRINTF(2, 3) {
      va_list va;

      va_start(va, fmt);
      const size_t written = print(_position, fmt, va);
      va_end(va);

      if (written < _width) {
        // Fill empty space
        memset(_buffer + _position + written, ' ', _width - written);
      }

      return next();
    }

    ZColumn right(const char* fmt, ...) ATTRIBUTE_PRINTF(2, 3) {
      va_list va;

      va_start(va, fmt);
      const size_t written = print(_position, fmt, va);
      va_end(va);

      if (written > _width) {
        // Line too long
        return fill('?');
      }

      if (written < _width) {
        // Short line, move all to right
        memmove(_buffer + _position + _width - written, _buffer + _position, written);

        // Fill empty space
        memset(_buffer + _position, ' ', _width - written);
      }

      return next();
    }

    ZColumn center(const char* fmt, ...) ATTRIBUTE_PRINTF(2, 3) {
      va_list va;

      va_start(va, fmt);
      const size_t written = print(_position, fmt, va);
      va_end(va);

      if (written > _width) {
        // Line too long
        return fill('?');
      }

      if (written < _width) {
        // Short line, move all to center
        const size_t start_space = (_width - written) / 2;
        const size_t end_space = _width - written - start_space;
        memmove(_buffer + _position + start_space, _buffer + _position, written);

        // Fill empty spaces
        memset(_buffer + _position, ' ', start_space);
        memset(_buffer + _position + start_space + written, ' ', end_space);
      }

      return next();
    }

    ZColumn fill(char filler = ' ') {
      memset(_buffer + _position, filler, _width);
      return next();
    }

    const char* end() {
      _buffer[_position] = '\0';
      return _buffer;
    }
  };

public:
  ZStatTablePrinter(size_t column0_width, size_t columnN_width) :
      _column0_width(column0_width),
      _columnN_width(columnN_width) {}

  ZColumn operator()() {
    return ZColumn(_buffer, 0, _column0_width, _columnN_width);
  }
};

//
// Stat cycle
//
uint64_t  ZStatCycle::_nwarmup_cycles = 0;
Ticks     ZStatCycle::_start_of_last;
Ticks     ZStatCycle::_end_of_last;
NumberSeq ZStatCycle::_serial_time(0.7 /* alpha */);
NumberSeq ZStatCycle::_parallelizable_time(0.7 /* alpha */);
uint      ZStatCycle::_last_active_workers = 0;

void ZStatCycle::at_start() {
  _start_of_last = Ticks::now();
}

void ZStatCycle::at_end(GCCause::Cause cause, uint active_workers) {
  _end_of_last = Ticks::now();

  if (cause == GCCause::_z_warmup) {
    _nwarmup_cycles++;
  }

  _last_active_workers = active_workers;

  // Calculate serial and parallelizable GC cycle times
  const double duration = (_end_of_last - _start_of_last).seconds();
  const double workers_duration = ZStatWorkers::get_and_reset_duration();
  const double serial_time = duration - workers_duration;
  const double parallelizable_time = workers_duration * active_workers;
  _serial_time.add(serial_time);
  _parallelizable_time.add(parallelizable_time);
}

bool ZStatCycle::is_warm() {
  return _nwarmup_cycles >= 3;
}

uint64_t ZStatCycle::nwarmup_cycles() {
  return _nwarmup_cycles;
}

bool ZStatCycle::is_time_trustable() {
  // The times are considered trustable if we
  // have completed at least one warmup cycle.
  return _nwarmup_cycles > 0;
}

const AbsSeq& ZStatCycle::serial_time() {
  return _serial_time;
}

const AbsSeq& ZStatCycle::parallelizable_time() {
  return _parallelizable_time;
}

uint ZStatCycle::last_active_workers() {
  return _last_active_workers;
}

double ZStatCycle::time_since_last() {
  if (_end_of_last.value() == 0) {
    // No end recorded yet, return time since VM start
    return os::elapsedTime();
  }

  const Ticks now = Ticks::now();
  const Tickspan time_since_last = now - _end_of_last;
  return time_since_last.seconds();
}

//
// Stat workers
//
Ticks ZStatWorkers::_start_of_last;
Tickspan ZStatWorkers::_accumulated_duration;

void ZStatWorkers::at_start() {
  _start_of_last = Ticks::now();
}

void ZStatWorkers::at_end() {
  const Ticks now = Ticks::now();
  const Tickspan duration = now - _start_of_last;
  _accumulated_duration += duration;
}

double ZStatWorkers::get_and_reset_duration() {
  const double duration = _accumulated_duration.seconds();
  const Ticks now = Ticks::now();
  _accumulated_duration = now - now;
  return duration;
}

//
// Stat load
//
void ZStatLoad::print() {
  double loadavg[3] = {};
  os::loadavg(loadavg, ARRAY_SIZE(loadavg));
  log_info(gc, load)("Load: %.2f/%.2f/%.2f", loadavg[0], loadavg[1], loadavg[2]);
}

//
// Stat mark
//
size_t ZStatMark::_nstripes;
size_t ZStatMark::_nproactiveflush;
size_t ZStatMark::_nterminateflush;
size_t ZStatMark::_ntrycomplete;
size_t ZStatMark::_ncontinue;
size_t ZStatMark::_mark_stack_usage;

void ZStatMark::set_at_mark_start(size_t nstripes) {
  _nstripes = nstripes;
}

void ZStatMark::set_at_mark_end(size_t nproactiveflush,
                                size_t nterminateflush,
                                size_t ntrycomplete,
                                size_t ncontinue) {
  _nproactiveflush = nproactiveflush;
  _nterminateflush = nterminateflush;
  _ntrycomplete = ntrycomplete;
  _ncontinue = ncontinue;
}

void ZStatMark::set_at_mark_free(size_t mark_stack_usage) {
  _mark_stack_usage = mark_stack_usage;
}

void ZStatMark::print() {
  log_info(gc, marking)("Mark: "
                        SIZE_FORMAT " stripe(s), "
                        SIZE_FORMAT " proactive flush(es), "
                        SIZE_FORMAT " terminate flush(es), "
                        SIZE_FORMAT " completion(s), "
                        SIZE_FORMAT " continuation(s) ",
                        _nstripes,
                        _nproactiveflush,
                        _nterminateflush,
                        _ntrycomplete,
                        _ncontinue);

  log_info(gc, marking)("Mark Stack Usage: " SIZE_FORMAT "M", _mark_stack_usage / M);
}

//
// Stat relocation
//
ZRelocationSetSelectorStats ZStatRelocation::_selector_stats;
size_t                      ZStatRelocation::_forwarding_usage;
size_t                      ZStatRelocation::_small_in_place_count;
size_t                      ZStatRelocation::_medium_in_place_count;

void ZStatRelocation::set_at_select_relocation_set(const ZRelocationSetSelectorStats& selector_stats) {
  _selector_stats = selector_stats;
}

void ZStatRelocation::set_at_install_relocation_set(size_t forwarding_usage) {
  _forwarding_usage = forwarding_usage;
}

void ZStatRelocation::set_at_relocate_end(size_t small_in_place_count, size_t medium_in_place_count) {
  _small_in_place_count = small_in_place_count;
  _medium_in_place_count = medium_in_place_count;
}

void ZStatRelocation::print(const char* name,
                            const ZRelocationSetSelectorGroupStats& selector_group,
                            size_t in_place_count) {
  log_info(gc, reloc)("%s Pages: " SIZE_FORMAT " / " SIZE_FORMAT "M, Empty: " SIZE_FORMAT "M, "
                      "Relocated: " SIZE_FORMAT "M, In-Place: " SIZE_FORMAT,
                      name,
                      selector_group.npages(),
                      selector_group.total() / M,
                      selector_group.empty() / M,
                      selector_group.relocate() / M,
                      in_place_count);
}

void ZStatRelocation::print() {
  print("Small", _selector_stats.small(), _small_in_place_count);
  if (ZPageSizeMedium != 0) {
    print("Medium", _selector_stats.medium(), _medium_in_place_count);
  }
  print("Large", _selector_stats.large(), 0 /* in_place_count */);

  log_info(gc, reloc)("Forwarding Usage: " SIZE_FORMAT "M", _forwarding_usage / M);
}

//
// Stat nmethods
//
void ZStatNMethods::print() {
  log_info(gc, nmethod)("NMethods: " SIZE_FORMAT " registered, " SIZE_FORMAT " unregistered",
                        ZNMethodTable::registered_nmethods(),
                        ZNMethodTable::unregistered_nmethods());
}

//
// Stat metaspace
//
void ZStatMetaspace::print() {
  MetaspaceCombinedStats stats = MetaspaceUtils::get_combined_statistics();
  log_info(gc, metaspace)("Metaspace: "
                          SIZE_FORMAT "M used, "
                          SIZE_FORMAT "M committed, " SIZE_FORMAT "M reserved",
                          stats.used() / M,
                          stats.committed() / M,
                          stats.reserved() / M);
}

//
// Stat references
//
ZStatReferences::ZCount ZStatReferences::_soft;
ZStatReferences::ZCount ZStatReferences::_weak;
ZStatReferences::ZCount ZStatReferences::_final;
ZStatReferences::ZCount ZStatReferences::_phantom;

void ZStatReferences::set(ZCount* count, size_t encountered, size_t discovered, size_t enqueued) {
  count->encountered = encountered;
  count->discovered = discovered;
  count->enqueued = enqueued;
}

void ZStatReferences::set_soft(size_t encountered, size_t discovered, size_t enqueued) {
  set(&_soft, encountered, discovered, enqueued);
}

void ZStatReferences::set_weak(size_t encountered, size_t discovered, size_t enqueued) {
  set(&_weak, encountered, discovered, enqueued);
}

void ZStatReferences::set_final(size_t encountered, size_t discovered, size_t enqueued) {
  set(&_final, encountered, discovered, enqueued);
}

void ZStatReferences::set_phantom(size_t encountered, size_t discovered, size_t enqueued) {
  set(&_phantom, encountered, discovered, enqueued);
}

void ZStatReferences::print(const char* name, const ZStatReferences::ZCount& ref) {
  log_info(gc, ref)("%s: "
                    SIZE_FORMAT " encountered, "
                    SIZE_FORMAT " discovered, "
                    SIZE_FORMAT " enqueued",
                    name,
                    ref.encountered,
                    ref.discovered,
                    ref.enqueued);
}

void ZStatReferences::print() {
  print("Soft", _soft);
  print("Weak", _weak);
  print("Final", _final);
  print("Phantom", _phantom);
}

//
// Stat heap
//
ZStatHeap::ZAtInitialize ZStatHeap::_at_initialize;
ZStatHeap::ZAtMarkStart ZStatHeap::_at_mark_start;
ZStatHeap::ZAtMarkEnd ZStatHeap::_at_mark_end;
ZStatHeap::ZAtRelocateStart ZStatHeap::_at_relocate_start;
ZStatHeap::ZAtRelocateEnd ZStatHeap::_at_relocate_end;

size_t ZStatHeap::capacity_high() {
  return MAX4(_at_mark_start.capacity,
              _at_mark_end.capacity,
              _at_relocate_start.capacity,
              _at_relocate_end.capacity);
}

size_t ZStatHeap::capacity_low() {
  return MIN4(_at_mark_start.capacity,
              _at_mark_end.capacity,
              _at_relocate_start.capacity,
              _at_relocate_end.capacity);
}

size_t ZStatHeap::free(size_t used) {
  return _at_initialize.max_capacity - used;
}

size_t ZStatHeap::allocated(size_t used, size_t reclaimed) {
  // The amount of allocated memory between point A and B is used(B) - used(A).
  // However, we might also have reclaimed memory between point A and B. This
  // means the current amount of used memory must be incremented by the amount
  // reclaimed, so that used(B) represents the amount of used memory we would
  // have had if we had not reclaimed anything.
  return (used + reclaimed) - _at_mark_start.used;
}

size_t ZStatHeap::garbage(size_t reclaimed) {
  return _at_mark_end.garbage - reclaimed;
}

void ZStatHeap::set_at_initialize(const ZPageAllocatorStats& stats) {
  _at_initialize.min_capacity = stats.min_capacity();
  _at_initialize.max_capacity = stats.max_capacity();
}

void ZStatHeap::set_at_mark_start(const ZPageAllocatorStats& stats) {
  _at_mark_start.soft_max_capacity = stats.soft_max_capacity();
  _at_mark_start.capacity = stats.capacity();
  _at_mark_start.free = free(stats.used());
  _at_mark_start.used = stats.used();
}

void ZStatHeap::set_at_mark_end(const ZPageAllocatorStats& stats) {
  _at_mark_end.capacity = stats.capacity();
  _at_mark_end.free = free(stats.used());
  _at_mark_end.used = stats.used();
  _at_mark_end.allocated = allocated(stats.used(), 0 /* reclaimed */);
}

void ZStatHeap::set_at_select_relocation_set(const ZRelocationSetSelectorStats& stats) {
  const size_t live = stats.small().live() + stats.medium().live() + stats.large().live();
  _at_mark_end.live = live;
  _at_mark_end.garbage = _at_mark_start.used - live;
}

void ZStatHeap::set_at_relocate_start(const ZPageAllocatorStats& stats) {
  _at_relocate_start.capacity = stats.capacity();
  _at_relocate_start.free = free(stats.used());
  _at_relocate_start.used = stats.used();
  _at_relocate_start.allocated = allocated(stats.used(), stats.reclaimed());
  _at_relocate_start.garbage = garbage(stats.reclaimed());
  _at_relocate_start.reclaimed = stats.reclaimed();
}

void ZStatHeap::set_at_relocate_end(const ZPageAllocatorStats& stats, size_t non_worker_relocated) {
  const size_t reclaimed = stats.reclaimed() - MIN2(non_worker_relocated, stats.reclaimed());

  _at_relocate_end.capacity = stats.capacity();
  _at_relocate_end.capacity_high = capacity_high();
  _at_relocate_end.capacity_low = capacity_low();
  _at_relocate_end.free = free(stats.used());
  _at_relocate_end.free_high = free(stats.used_low());
  _at_relocate_end.free_low = free(stats.used_high());
  _at_relocate_end.used = stats.used();
  _at_relocate_end.used_high = stats.used_high();
  _at_relocate_end.used_low = stats.used_low();
  _at_relocate_end.allocated = allocated(stats.used(), reclaimed);
  _at_relocate_end.garbage = garbage(reclaimed);
  _at_relocate_end.reclaimed = reclaimed;
}

size_t ZStatHeap::max_capacity() {
  return _at_initialize.max_capacity;
}

size_t ZStatHeap::used_at_mark_start() {
  return _at_mark_start.used;
}

size_t ZStatHeap::used_at_relocate_end() {
  return _at_relocate_end.used;
}

void ZStatHeap::print() {
  log_info(gc, heap)("Min Capacity: "
                     ZSIZE_FMT, ZSIZE_ARGS(_at_initialize.min_capacity));
  log_info(gc, heap)("Max Capacity: "
                     ZSIZE_FMT, ZSIZE_ARGS(_at_initialize.max_capacity));
  log_info(gc, heap)("Soft Max Capacity: "
                     ZSIZE_FMT, ZSIZE_ARGS(_at_mark_start.soft_max_capacity));

  ZStatTablePrinter table(10, 18);
  log_info(gc, heap)("%s", table()
                     .fill()
                     .center("Mark Start")
                     .center("Mark End")
                     .center("Relocate Start")
                     .center("Relocate End")
                     .center("High")
                     .center("Low")
                     .end());
  log_info(gc, heap)("%s", table()
                     .right("Capacity:")
                     .left(ZTABLE_ARGS(_at_mark_start.capacity))
                     .left(ZTABLE_ARGS(_at_mark_end.capacity))
                     .left(ZTABLE_ARGS(_at_relocate_start.capacity))
                     .left(ZTABLE_ARGS(_at_relocate_end.capacity))
                     .left(ZTABLE_ARGS(_at_relocate_end.capacity_high))
                     .left(ZTABLE_ARGS(_at_relocate_end.capacity_low))
                     .end());
  log_info(gc, heap)("%s", table()
                     .right("Free:")
                     .left(ZTABLE_ARGS(_at_mark_start.free))
                     .left(ZTABLE_ARGS(_at_mark_end.free))
                     .left(ZTABLE_ARGS(_at_relocate_start.free))
                     .left(ZTABLE_ARGS(_at_relocate_end.free))
                     .left(ZTABLE_ARGS(_at_relocate_end.free_high))
                     .left(ZTABLE_ARGS(_at_relocate_end.free_low))
                     .end());
  log_info(gc, heap)("%s", table()
                     .right("Used:")
                     .left(ZTABLE_ARGS(_at_mark_start.used))
                     .left(ZTABLE_ARGS(_at_mark_end.used))
                     .left(ZTABLE_ARGS(_at_relocate_start.used))
                     .left(ZTABLE_ARGS(_at_relocate_end.used))
                     .left(ZTABLE_ARGS(_at_relocate_end.used_high))
                     .left(ZTABLE_ARGS(_at_relocate_end.used_low))
                     .end());
  log_info(gc, heap)("%s", table()
                     .right("Live:")
                     .left(ZTABLE_ARGS_NA)
                     .left(ZTABLE_ARGS(_at_mark_end.live))
                     .left(ZTABLE_ARGS(_at_mark_end.live /* Same as at mark end */))
                     .left(ZTABLE_ARGS(_at_mark_end.live /* Same as at mark end */))
                     .left(ZTABLE_ARGS_NA)
                     .left(ZTABLE_ARGS_NA)
                     .end());
  log_info(gc, heap)("%s", table()
                     .right("Allocated:")
                     .left(ZTABLE_ARGS_NA)
                     .left(ZTABLE_ARGS(_at_mark_end.allocated))
                     .left(ZTABLE_ARGS(_at_relocate_start.allocated))
                     .left(ZTABLE_ARGS(_at_relocate_end.allocated))
                     .left(ZTABLE_ARGS_NA)
                     .left(ZTABLE_ARGS_NA)
                     .end());
  log_info(gc, heap)("%s", table()
                     .right("Garbage:")
                     .left(ZTABLE_ARGS_NA)
                     .left(ZTABLE_ARGS(_at_mark_end.garbage))
                     .left(ZTABLE_ARGS(_at_relocate_start.garbage))
                     .left(ZTABLE_ARGS(_at_relocate_end.garbage))
                     .left(ZTABLE_ARGS_NA)
                     .left(ZTABLE_ARGS_NA)
                     .end());
  log_info(gc, heap)("%s", table()
                     .right("Reclaimed:")
                     .left(ZTABLE_ARGS_NA)
                     .left(ZTABLE_ARGS_NA)
                     .left(ZTABLE_ARGS(_at_relocate_start.reclaimed))
                     .left(ZTABLE_ARGS(_at_relocate_end.reclaimed))
                     .left(ZTABLE_ARGS_NA)
                     .left(ZTABLE_ARGS_NA)
                     .end());
}
