/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.hpp"
#include "memory/metaspace.hpp"
#include "memory/metaspaceUtils.hpp"
#include "services/mallocTracker.hpp"
#include "services/memReporter.hpp"
#include "services/threadStackTracker.hpp"
#include "services/virtualMemoryTracker.hpp"
#include "utilities/globalDefinitions.hpp"

size_t MemReporterBase::reserved_total(const MallocMemory* malloc, const VirtualMemory* vm) const {
  return malloc->malloc_size() + malloc->arena_size() + vm->reserved();
}

size_t MemReporterBase::committed_total(const MallocMemory* malloc, const VirtualMemory* vm) const {
  return malloc->malloc_size() + malloc->arena_size() + vm->committed();
}

void MemReporterBase::print_total(size_t reserved, size_t committed) const {
  const char* scale = current_scale();
  output()->print("reserved=" SIZE_FORMAT "%s, committed=" SIZE_FORMAT "%s",
    amount_in_current_scale(reserved), scale, amount_in_current_scale(committed), scale);
}

void MemReporterBase::print_malloc(size_t amount, size_t count, MEMFLAGS flag) const {
  const char* scale = current_scale();
  outputStream* out = output();
  const char* alloc_type = (flag == mtThreadStack) ? "" : "malloc=";

  if (flag != mtNone) {
    out->print("(%s" SIZE_FORMAT "%s type=%s", alloc_type,
      amount_in_current_scale(amount), scale, NMTUtil::flag_to_name(flag));
  } else {
    out->print("(%s" SIZE_FORMAT "%s", alloc_type,
      amount_in_current_scale(amount), scale);
  }

  if (count > 0) {
    out->print(" #" SIZE_FORMAT "", count);
  }

  out->print(")");
}

void MemReporterBase::print_virtual_memory(size_t reserved, size_t committed) const {
  const char* scale = current_scale();
  output()->print("(mmap: reserved=" SIZE_FORMAT "%s, committed=" SIZE_FORMAT "%s)",
    amount_in_current_scale(reserved), scale, amount_in_current_scale(committed), scale);
}

void MemReporterBase::print_malloc_line(size_t amount, size_t count) const {
  output()->print("%28s", " ");
  print_malloc(amount, count);
  output()->print_cr(" ");
}

void MemReporterBase::print_virtual_memory_line(size_t reserved, size_t committed) const {
  output()->print("%28s", " ");
  print_virtual_memory(reserved, committed);
  output()->print_cr(" ");
}

void MemReporterBase::print_arena_line(size_t amount, size_t count) const {
  const char* scale = current_scale();
  output()->print_cr("%27s (arena=" SIZE_FORMAT "%s #" SIZE_FORMAT ")", " ",
    amount_in_current_scale(amount), scale, count);
}

void MemReporterBase::print_virtual_memory_region(const char* type, address base, size_t size) const {
  const char* scale = current_scale();
  output()->print("[" PTR_FORMAT " - " PTR_FORMAT "] %s " SIZE_FORMAT "%s",
    p2i(base), p2i(base + size), type, amount_in_current_scale(size), scale);
}


void MemSummaryReporter::report() {
  outputStream* out = output();
  const size_t total_malloced_bytes = _malloc_snapshot->total();
  const size_t total_mmap_reserved_bytes = _vm_snapshot->total_reserved();
  const size_t total_mmap_committed_bytes = _vm_snapshot->total_committed();

  size_t total_reserved_amount = total_malloced_bytes + total_mmap_reserved_bytes;
  size_t total_committed_amount = total_malloced_bytes + total_mmap_committed_bytes;

  // Overall total
  out->print_cr("\nNative Memory Tracking:\n");

  if (scale() > 1) {
    out->print_cr("(Omitting categories weighting less than 1%s)", current_scale());
    out->cr();
  }

  out->print("Total: ");
  print_total(total_reserved_amount, total_committed_amount);
  out->cr();
  out->print_cr("       malloc: " SIZE_FORMAT "%s #" SIZE_FORMAT,
                amount_in_current_scale(total_malloced_bytes), current_scale(),
                _malloc_snapshot->total_count());
  out->print("       mmap:   ");
  print_total(total_mmap_reserved_bytes, total_mmap_committed_bytes);
  out->cr();
  out->cr();

  // Summary by memory type
  for (int index = 0; index < mt_number_of_types; index ++) {
    MEMFLAGS flag = NMTUtil::index_to_flag(index);
    // thread stack is reported as part of thread category
    if (flag == mtThreadStack) continue;
    MallocMemory* malloc_memory = _malloc_snapshot->by_type(flag);
    VirtualMemory* virtual_memory = _vm_snapshot->by_type(flag);

    report_summary_of_type(flag, malloc_memory, virtual_memory);
  }
}

void MemSummaryReporter::report_summary_of_type(MEMFLAGS flag,
  MallocMemory*  malloc_memory, VirtualMemory* virtual_memory) {

  size_t reserved_amount  = reserved_total (malloc_memory, virtual_memory);
  size_t committed_amount = committed_total(malloc_memory, virtual_memory);

  // Count thread's native stack in "Thread" category
  if (flag == mtThread) {
    if (ThreadStackTracker::track_as_vm()) {
      const VirtualMemory* thread_stack_usage =
        (const VirtualMemory*)_vm_snapshot->by_type(mtThreadStack);
      reserved_amount  += thread_stack_usage->reserved();
      committed_amount += thread_stack_usage->committed();
    } else {
      const MallocMemory* thread_stack_usage =
        (const MallocMemory*)_malloc_snapshot->by_type(mtThreadStack);
      reserved_amount += thread_stack_usage->malloc_size();
      committed_amount += thread_stack_usage->malloc_size();
    }
  } else if (flag == mtNMT) {
    // Count malloc headers in "NMT" category
    reserved_amount  += _malloc_snapshot->malloc_overhead()->size();
    committed_amount += _malloc_snapshot->malloc_overhead()->size();
  }

  if (amount_in_current_scale(reserved_amount) > 0) {
    outputStream* out   = output();
    const char*   scale = current_scale();
    out->print("-%26s (", NMTUtil::flag_to_name(flag));
    print_total(reserved_amount, committed_amount);
    out->print_cr(")");

    if (flag == mtClass) {
      // report class count
      out->print_cr("%27s (classes #" SIZE_FORMAT ")",
        " ", (_instance_class_count + _array_class_count));
      out->print_cr("%27s (  instance classes #" SIZE_FORMAT ", array classes #" SIZE_FORMAT ")",
        " ", _instance_class_count, _array_class_count);
    } else if (flag == mtThread) {
      if (ThreadStackTracker::track_as_vm()) {
        const VirtualMemory* thread_stack_usage =
         _vm_snapshot->by_type(mtThreadStack);
        // report thread count
        out->print_cr("%27s (thread #" SIZE_FORMAT ")", " ", ThreadStackTracker::thread_count());
        out->print("%27s (stack: ", " ");
        print_total(thread_stack_usage->reserved(), thread_stack_usage->committed());
      } else {
        MallocMemory* thread_stack_memory = _malloc_snapshot->by_type(mtThreadStack);
        const char* scale = current_scale();
        // report thread count
        assert(ThreadStackTracker::thread_count() == 0, "Not used");
        out->print_cr("%27s (thread #" SIZE_FORMAT ")", " ", thread_stack_memory->malloc_count());
        out->print("%27s (Stack: " SIZE_FORMAT "%s", " ",
          amount_in_current_scale(thread_stack_memory->malloc_size()), scale);
      }
      out->print_cr(")");
    }

     // report malloc'd memory
    if (amount_in_current_scale(malloc_memory->malloc_size()) > 0) {
      // We don't know how many arena chunks are in used, so don't report the count
      size_t count = (flag == mtChunk) ? 0 : malloc_memory->malloc_count();
      print_malloc_line(malloc_memory->malloc_size(), count);
    }

    if (amount_in_current_scale(virtual_memory->reserved()) > 0) {
      print_virtual_memory_line(virtual_memory->reserved(), virtual_memory->committed());
    }

    if (amount_in_current_scale(malloc_memory->arena_size()) > 0) {
      print_arena_line(malloc_memory->arena_size(), malloc_memory->arena_count());
    }

    if (flag == mtNMT &&
      amount_in_current_scale(_malloc_snapshot->malloc_overhead()->size()) > 0) {
      out->print_cr("%27s (tracking overhead=" SIZE_FORMAT "%s)", " ",
        amount_in_current_scale(_malloc_snapshot->malloc_overhead()->size()), scale);
    } else if (flag == mtClass) {
      // Metadata information
      report_metadata(Metaspace::NonClassType);
      if (Metaspace::using_class_space()) {
        report_metadata(Metaspace::ClassType);
      }
    }
    out->print_cr(" ");
  }
}

void MemSummaryReporter::report_metadata(Metaspace::MetadataType type) const {
  assert(type == Metaspace::NonClassType || type == Metaspace::ClassType,
    "Invalid metadata type");
  const char* name = (type == Metaspace::NonClassType) ?
    "Metadata:   " : "Class space:";

  outputStream* out = output();
  const char* scale = current_scale();
  const MetaspaceStats stats = MetaspaceUtils::get_statistics(type);

  size_t waste = stats.committed() - stats.used();
  float waste_percentage = stats.committed() > 0 ? (((float)waste * 100)/stats.committed()) : 0.0f;

  out->print_cr("%27s (  %s)", " ", name);
  out->print("%27s (    ", " ");
  print_total(stats.reserved(), stats.committed());
  out->print_cr(")");
  out->print_cr("%27s (    used=" SIZE_FORMAT "%s)", " ", amount_in_current_scale(stats.used()), scale);
  out->print_cr("%27s (    waste=" SIZE_FORMAT "%s =%2.2f%%)", " ", amount_in_current_scale(waste),
    scale, waste_percentage);
}

void MemDetailReporter::report_detail() {
  // Start detail report
  outputStream* out = output();
  out->print_cr("Details:\n");

  int num_omitted =
      report_malloc_sites() +
      report_virtual_memory_allocation_sites();
  if (num_omitted > 0) {
    assert(scale() > 1, "sanity");
    out->print_cr("(%d call sites weighting less than 1%s each omitted.)",
                   num_omitted, current_scale());
    out->cr();
  }
}

int MemDetailReporter::report_malloc_sites() {
  MallocSiteIterator         malloc_itr = _baseline.malloc_sites(MemBaseline::by_size);
  if (malloc_itr.is_empty()) return 0;

  outputStream* out = output();

  const MallocSite* malloc_site;
  int num_omitted = 0;
  while ((malloc_site = malloc_itr.next()) != NULL) {
    // Don't report free sites; does not count toward omitted count.
    if (malloc_site->size() == 0) {
      continue;
    }
    // Don't report if site has allocated less than one unit of whatever our scale is
    if (scale() > 1 && amount_in_current_scale(malloc_site->size()) == 0) {
      num_omitted ++;
      continue;
    }
    const NativeCallStack* stack = malloc_site->call_stack();
    stack->print_on(out);
    out->print("%29s", " ");
    MEMFLAGS flag = malloc_site->flag();
    assert(NMTUtil::flag_is_valid(flag) && flag != mtNone,
      "Must have a valid memory type");
    print_malloc(malloc_site->size(), malloc_site->count(),flag);
    out->print_cr("\n");
  }
  return num_omitted;
}

int MemDetailReporter::report_virtual_memory_allocation_sites()  {
  VirtualMemorySiteIterator  virtual_memory_itr =
    _baseline.virtual_memory_sites(MemBaseline::by_size);

  if (virtual_memory_itr.is_empty()) return 0;

  outputStream* out = output();
  const VirtualMemoryAllocationSite*  virtual_memory_site;
  int num_omitted = 0;
  while ((virtual_memory_site = virtual_memory_itr.next()) != NULL) {
    // Don't report free sites; does not count toward omitted count.
    if (virtual_memory_site->reserved() == 0) {
      continue;
    }
    // Don't report if site has reserved less than one unit of whatever our scale is
    if (scale() > 1 && amount_in_current_scale(virtual_memory_site->reserved()) == 0) {
      num_omitted++;
      continue;
    }
    const NativeCallStack* stack = virtual_memory_site->call_stack();
    stack->print_on(out);
    out->print("%28s (", " ");
    print_total(virtual_memory_site->reserved(), virtual_memory_site->committed());
    MEMFLAGS flag = virtual_memory_site->flag();
    if (flag != mtNone) {
      out->print(" Type=%s", NMTUtil::flag_to_name(flag));
    }
    out->print_cr(")\n");
  }
  return num_omitted;
}


void MemDetailReporter::report_virtual_memory_map() {
  // Virtual memory map always in base address order
  VirtualMemoryAllocationIterator itr = _baseline.virtual_memory_allocations();
  const ReservedMemoryRegion* rgn;

  output()->print_cr("Virtual memory map:");
  while ((rgn = itr.next()) != NULL) {
    report_virtual_memory_region(rgn);
  }
}

void MemDetailReporter::report_virtual_memory_region(const ReservedMemoryRegion* reserved_rgn) {
  assert(reserved_rgn != NULL, "NULL pointer");

  // Don't report if size is too small
  if (amount_in_current_scale(reserved_rgn->size()) == 0) return;

  outputStream* out = output();
  const char* scale = current_scale();
  const NativeCallStack*  stack = reserved_rgn->call_stack();
  bool all_committed = reserved_rgn->size() == reserved_rgn->committed_size();
  const char* region_type = (all_committed ? "reserved and committed" : "reserved");
  out->print_cr(" ");
  print_virtual_memory_region(region_type, reserved_rgn->base(), reserved_rgn->size());
  out->print(" for %s", NMTUtil::flag_to_name(reserved_rgn->flag()));
  if (stack->is_empty()) {
    out->print_cr(" ");
  } else {
    out->print_cr(" from");
    stack->print_on(out, 4);
  }

  if (all_committed) {
    CommittedRegionIterator itr = reserved_rgn->iterate_committed_regions();
    const CommittedMemoryRegion* committed_rgn = itr.next();
    if (committed_rgn->size() == reserved_rgn->size() && committed_rgn->call_stack()->equals(*stack)) {
      // One region spanning the entire reserved region, with the same stack trace.
      // Don't print this regions because the "reserved and committed" line above
      // already indicates that the region is comitted.
      assert(itr.next() == NULL, "Unexpectedly more than one regions");
      return;
    }
  }

  CommittedRegionIterator itr = reserved_rgn->iterate_committed_regions();
  const CommittedMemoryRegion* committed_rgn;
  while ((committed_rgn = itr.next()) != NULL) {
    // Don't report if size is too small
    if (amount_in_current_scale(committed_rgn->size()) == 0) continue;
    stack = committed_rgn->call_stack();
    out->print("\n\t");
    print_virtual_memory_region("committed", committed_rgn->base(), committed_rgn->size());
    if (stack->is_empty()) {
      out->print_cr(" ");
    } else {
      out->print_cr(" from");
      stack->print_on(out, 12);
    }
  }
}

void MemSummaryDiffReporter::report_diff() {
  outputStream* out = output();
  out->print_cr("\nNative Memory Tracking:\n");

  if (scale() > 1) {
    out->print_cr("(Omitting categories weighting less than 1%s)", current_scale());
    out->cr();
  }

  // Overall diff
  out->print("Total: ");
  print_virtual_memory_diff(_current_baseline.total_reserved_memory(),
    _current_baseline.total_committed_memory(), _early_baseline.total_reserved_memory(),
    _early_baseline.total_committed_memory());

  out->print_cr("\n");

  // Summary diff by memory type
  for (int index = 0; index < mt_number_of_types; index ++) {
    MEMFLAGS flag = NMTUtil::index_to_flag(index);
    // thread stack is reported as part of thread category
    if (flag == mtThreadStack) continue;
    diff_summary_of_type(flag,
      _early_baseline.malloc_memory(flag),
      _early_baseline.virtual_memory(flag),
      _early_baseline.metaspace_stats(),
      _current_baseline.malloc_memory(flag),
      _current_baseline.virtual_memory(flag),
      _current_baseline.metaspace_stats());
  }
}

void MemSummaryDiffReporter::print_malloc_diff(size_t current_amount, size_t current_count,
    size_t early_amount, size_t early_count, MEMFLAGS flags) const {
  const char* scale = current_scale();
  outputStream* out = output();
  const char* alloc_type = (flags == mtThread) ? "" : "malloc=";

  out->print("%s" SIZE_FORMAT "%s", alloc_type, amount_in_current_scale(current_amount), scale);
  // Report type only if it is valid and not under "thread" category
  if (flags != mtNone && flags != mtThread) {
    out->print(" type=%s", NMTUtil::flag_to_name(flags));
  }

  long amount_diff = diff_in_current_scale(current_amount, early_amount);
  if (amount_diff != 0) {
    out->print(" %+ld%s", amount_diff, scale);
  }
  if (current_count > 0) {
    out->print(" #" SIZE_FORMAT "", current_count);
    if (current_count != early_count) {
      out->print(" %+d", (int)(current_count - early_count));
    }
  }
}

void MemSummaryDiffReporter::print_arena_diff(size_t current_amount, size_t current_count,
  size_t early_amount, size_t early_count) const {
  const char* scale = current_scale();
  outputStream* out = output();
  out->print("arena=" SIZE_FORMAT "%s", amount_in_current_scale(current_amount), scale);
  if (diff_in_current_scale(current_amount, early_amount) != 0) {
    out->print(" %+ld", diff_in_current_scale(current_amount, early_amount));
  }

  out->print(" #" SIZE_FORMAT "", current_count);
  if (current_count != early_count) {
    out->print(" %+d", (int)(current_count - early_count));
  }
}

void MemSummaryDiffReporter::print_virtual_memory_diff(size_t current_reserved, size_t current_committed,
    size_t early_reserved, size_t early_committed) const {
  const char* scale = current_scale();
  outputStream* out = output();
  out->print("reserved=" SIZE_FORMAT "%s", amount_in_current_scale(current_reserved), scale);
  long reserved_diff = diff_in_current_scale(current_reserved, early_reserved);
  if (reserved_diff != 0) {
    out->print(" %+ld%s", reserved_diff, scale);
  }

  out->print(", committed=" SIZE_FORMAT "%s", amount_in_current_scale(current_committed), scale);
  long committed_diff = diff_in_current_scale(current_committed, early_committed);
  if (committed_diff != 0) {
    out->print(" %+ld%s", committed_diff, scale);
  }
}


void MemSummaryDiffReporter::diff_summary_of_type(MEMFLAGS flag,
  const MallocMemory* early_malloc, const VirtualMemory* early_vm,
  const MetaspaceCombinedStats& early_ms,
  const MallocMemory* current_malloc, const VirtualMemory* current_vm,
  const MetaspaceCombinedStats& current_ms) const {

  outputStream* out = output();
  const char* scale = current_scale();

  // Total reserved and committed memory in current baseline
  size_t current_reserved_amount  = reserved_total (current_malloc, current_vm);
  size_t current_committed_amount = committed_total(current_malloc, current_vm);

  // Total reserved and committed memory in early baseline
  size_t early_reserved_amount  = reserved_total(early_malloc, early_vm);
  size_t early_committed_amount = committed_total(early_malloc, early_vm);

  // Adjust virtual memory total
  if (flag == mtThread) {
    const VirtualMemory* early_thread_stack_usage =
      _early_baseline.virtual_memory(mtThreadStack);
    const VirtualMemory* current_thread_stack_usage =
      _current_baseline.virtual_memory(mtThreadStack);

    early_reserved_amount  += early_thread_stack_usage->reserved();
    early_committed_amount += early_thread_stack_usage->committed();

    current_reserved_amount  += current_thread_stack_usage->reserved();
    current_committed_amount += current_thread_stack_usage->committed();
  } else if (flag == mtNMT) {
    early_reserved_amount  += _early_baseline.malloc_tracking_overhead();
    early_committed_amount += _early_baseline.malloc_tracking_overhead();

    current_reserved_amount  += _current_baseline.malloc_tracking_overhead();
    current_committed_amount += _current_baseline.malloc_tracking_overhead();
  }

  if (amount_in_current_scale(current_reserved_amount) > 0 ||
      diff_in_current_scale(current_reserved_amount, early_reserved_amount) != 0) {

    // print summary line
    out->print("-%26s (", NMTUtil::flag_to_name(flag));
    print_virtual_memory_diff(current_reserved_amount, current_committed_amount,
      early_reserved_amount, early_committed_amount);
    out->print_cr(")");

    // detail lines
    if (flag == mtClass) {
      // report class count
      out->print("%27s (classes #" SIZE_FORMAT "", " ", _current_baseline.class_count());
      int class_count_diff = (int)(_current_baseline.class_count() -
        _early_baseline.class_count());
      if (_current_baseline.class_count() != _early_baseline.class_count()) {
        out->print(" %+d", (int)(_current_baseline.class_count() - _early_baseline.class_count()));
      }
      out->print_cr(")");

      out->print("%27s (  instance classes #" SIZE_FORMAT, " ", _current_baseline.instance_class_count());
      if (_current_baseline.instance_class_count() != _early_baseline.instance_class_count()) {
        out->print(" %+d", (int)(_current_baseline.instance_class_count() - _early_baseline.instance_class_count()));
      }
      out->print(", array classes #" SIZE_FORMAT, _current_baseline.array_class_count());
      if (_current_baseline.array_class_count() != _early_baseline.array_class_count()) {
        out->print(" %+d", (int)(_current_baseline.array_class_count() - _early_baseline.array_class_count()));
      }
      out->print_cr(")");

    } else if (flag == mtThread) {
      // report thread count
      out->print("%27s (thread #" SIZE_FORMAT "", " ", _current_baseline.thread_count());
      int thread_count_diff = (int)(_current_baseline.thread_count() -
          _early_baseline.thread_count());
      if (thread_count_diff != 0) {
        out->print(" %+d", thread_count_diff);
      }
      out->print_cr(")");

      out->print("%27s (stack: ", " ");
      if (ThreadStackTracker::track_as_vm()) {
        // report thread stack
        const VirtualMemory* current_thread_stack =
          _current_baseline.virtual_memory(mtThreadStack);
        const VirtualMemory* early_thread_stack =
          _early_baseline.virtual_memory(mtThreadStack);

        print_virtual_memory_diff(current_thread_stack->reserved(), current_thread_stack->committed(),
          early_thread_stack->reserved(), early_thread_stack->committed());
      } else {
        const MallocMemory* current_thread_stack =
          _current_baseline.malloc_memory(mtThreadStack);
        const MallocMemory* early_thread_stack =
          _early_baseline.malloc_memory(mtThreadStack);

        print_malloc_diff(current_thread_stack->malloc_size(), current_thread_stack->malloc_count(),
          early_thread_stack->malloc_size(), early_thread_stack->malloc_count(), flag);
      }
      out->print_cr(")");
    }

    // Report malloc'd memory
    size_t current_malloc_amount = current_malloc->malloc_size();
    size_t early_malloc_amount   = early_malloc->malloc_size();
    if (amount_in_current_scale(current_malloc_amount) > 0 ||
        diff_in_current_scale(current_malloc_amount, early_malloc_amount) != 0) {
      out->print("%28s(", " ");
      print_malloc_diff(current_malloc_amount, (flag == mtChunk) ? 0 : current_malloc->malloc_count(),
        early_malloc_amount, early_malloc->malloc_count(), mtNone);
      out->print_cr(")");
    }

    // Report virtual memory
    if (amount_in_current_scale(current_vm->reserved()) > 0 ||
        diff_in_current_scale(current_vm->reserved(), early_vm->reserved()) != 0) {
      out->print("%27s (mmap: ", " ");
      print_virtual_memory_diff(current_vm->reserved(), current_vm->committed(),
        early_vm->reserved(), early_vm->committed());
      out->print_cr(")");
    }

    // Report arena memory
    if (amount_in_current_scale(current_malloc->arena_size()) > 0 ||
        diff_in_current_scale(current_malloc->arena_size(), early_malloc->arena_size()) != 0) {
      out->print("%28s(", " ");
      print_arena_diff(current_malloc->arena_size(), current_malloc->arena_count(),
        early_malloc->arena_size(), early_malloc->arena_count());
      out->print_cr(")");
    }

    // Report native memory tracking overhead
    if (flag == mtNMT) {
      size_t current_tracking_overhead = amount_in_current_scale(_current_baseline.malloc_tracking_overhead());
      size_t early_tracking_overhead   = amount_in_current_scale(_early_baseline.malloc_tracking_overhead());

      out->print("%27s (tracking overhead=" SIZE_FORMAT "%s", " ",
        amount_in_current_scale(_current_baseline.malloc_tracking_overhead()), scale);

      long overhead_diff = diff_in_current_scale(_current_baseline.malloc_tracking_overhead(),
           _early_baseline.malloc_tracking_overhead());
      if (overhead_diff != 0) {
        out->print(" %+ld%s", overhead_diff, scale);
      }
      out->print_cr(")");
    } else if (flag == mtClass) {
      print_metaspace_diff(current_ms, early_ms);
    }
    out->print_cr(" ");
  }
}

void MemSummaryDiffReporter::print_metaspace_diff(const MetaspaceCombinedStats& current_ms,
                                                  const MetaspaceCombinedStats& early_ms) const {
  print_metaspace_diff("Metadata", current_ms.non_class_space_stats(), early_ms.non_class_space_stats());
  if (Metaspace::using_class_space()) {
    print_metaspace_diff("Class space", current_ms.class_space_stats(), early_ms.class_space_stats());
  }
}

void MemSummaryDiffReporter::print_metaspace_diff(const char* header,
                                                  const MetaspaceStats& current_stats,
                                                  const MetaspaceStats& early_stats) const {
  outputStream* out = output();
  const char* scale = current_scale();

  out->print_cr("%27s: (  %s)", " ", header);
  out->print("%27s (    ", " ");
  print_virtual_memory_diff(current_stats.reserved(),
                            current_stats.committed(),
                            early_stats.reserved(),
                            early_stats.committed());
  out->print_cr(")");

  long diff_used = diff_in_current_scale(current_stats.used(),
                                         early_stats.used());

  size_t current_waste = current_stats.committed() - current_stats.used();
  size_t early_waste = early_stats.committed() - early_stats.used();
  long diff_waste = diff_in_current_scale(current_waste, early_waste);

  // Diff used
  out->print("%27s (    used=" SIZE_FORMAT "%s", " ",
    amount_in_current_scale(current_stats.used()), scale);
  if (diff_used != 0) {
    out->print(" %+ld%s", diff_used, scale);
  }
  out->print_cr(")");

  // Diff waste
  const float waste_percentage = current_stats.committed() == 0 ? 0.0f :
                                 (current_waste * 100.0f) / current_stats.committed();
  out->print("%27s (    waste=" SIZE_FORMAT "%s =%2.2f%%", " ",
    amount_in_current_scale(current_waste), scale, waste_percentage);
  if (diff_waste != 0) {
    out->print(" %+ld%s", diff_waste, scale);
  }
  out->print_cr(")");
}

void MemDetailDiffReporter::report_diff() {
  MemSummaryDiffReporter::report_diff();
  diff_malloc_sites();
  diff_virtual_memory_sites();
}

void MemDetailDiffReporter::diff_malloc_sites() const {
  MallocSiteIterator early_itr = _early_baseline.malloc_sites(MemBaseline::by_site_and_type);
  MallocSiteIterator current_itr = _current_baseline.malloc_sites(MemBaseline::by_site_and_type);

  const MallocSite* early_site   = early_itr.next();
  const MallocSite* current_site = current_itr.next();

  while (early_site != NULL || current_site != NULL) {
    if (early_site == NULL) {
      new_malloc_site(current_site);
      current_site = current_itr.next();
    } else if (current_site == NULL) {
      old_malloc_site(early_site);
      early_site = early_itr.next();
    } else {
      int compVal = current_site->call_stack()->compare(*early_site->call_stack());
      if (compVal < 0) {
        new_malloc_site(current_site);
        current_site = current_itr.next();
      } else if (compVal > 0) {
        old_malloc_site(early_site);
        early_site = early_itr.next();
      } else {
        diff_malloc_site(early_site, current_site);
        early_site   = early_itr.next();
        current_site = current_itr.next();
      }
    }
  }
}

void MemDetailDiffReporter::diff_virtual_memory_sites() const {
  VirtualMemorySiteIterator early_itr = _early_baseline.virtual_memory_sites(MemBaseline::by_site);
  VirtualMemorySiteIterator current_itr = _current_baseline.virtual_memory_sites(MemBaseline::by_site);

  const VirtualMemoryAllocationSite* early_site   = early_itr.next();
  const VirtualMemoryAllocationSite* current_site = current_itr.next();

  while (early_site != NULL || current_site != NULL) {
    if (early_site == NULL) {
      new_virtual_memory_site(current_site);
      current_site = current_itr.next();
    } else if (current_site == NULL) {
      old_virtual_memory_site(early_site);
      early_site = early_itr.next();
    } else {
      int compVal = current_site->call_stack()->compare(*early_site->call_stack());
      if (compVal < 0) {
        new_virtual_memory_site(current_site);
        current_site = current_itr.next();
      } else if (compVal > 0) {
        old_virtual_memory_site(early_site);
        early_site = early_itr.next();
      } else {
        diff_virtual_memory_site(early_site, current_site);
        early_site   = early_itr.next();
        current_site = current_itr.next();
      }
    }
  }
}


void MemDetailDiffReporter::new_malloc_site(const MallocSite* malloc_site) const {
  diff_malloc_site(malloc_site->call_stack(), malloc_site->size(), malloc_site->count(),
    0, 0, malloc_site->flag());
}

void MemDetailDiffReporter::old_malloc_site(const MallocSite* malloc_site) const {
  diff_malloc_site(malloc_site->call_stack(), 0, 0, malloc_site->size(),
    malloc_site->count(), malloc_site->flag());
}

void MemDetailDiffReporter::diff_malloc_site(const MallocSite* early,
  const MallocSite* current)  const {
  if (early->flag() != current->flag()) {
    // If malloc site type changed, treat it as deallocation of old type and
    // allocation of new type.
    old_malloc_site(early);
    new_malloc_site(current);
  } else {
    diff_malloc_site(current->call_stack(), current->size(), current->count(),
      early->size(), early->count(), early->flag());
  }
}

void MemDetailDiffReporter::diff_malloc_site(const NativeCallStack* stack, size_t current_size,
  size_t current_count, size_t early_size, size_t early_count, MEMFLAGS flags) const {
  outputStream* out = output();

  assert(stack != NULL, "NULL stack");

  if (diff_in_current_scale(current_size, early_size) == 0) {
      return;
  }

  stack->print_on(out);
  out->print("%28s (", " ");
  print_malloc_diff(current_size, current_count,
    early_size, early_count, flags);

  out->print_cr(")\n");
}


void MemDetailDiffReporter::new_virtual_memory_site(const VirtualMemoryAllocationSite* site) const {
  diff_virtual_memory_site(site->call_stack(), site->reserved(), site->committed(), 0, 0, site->flag());
}

void MemDetailDiffReporter::old_virtual_memory_site(const VirtualMemoryAllocationSite* site) const {
  diff_virtual_memory_site(site->call_stack(), 0, 0, site->reserved(), site->committed(), site->flag());
}

void MemDetailDiffReporter::diff_virtual_memory_site(const VirtualMemoryAllocationSite* early,
  const VirtualMemoryAllocationSite* current) const {
  assert(early->flag() == current->flag(), "Should be the same");
  diff_virtual_memory_site(current->call_stack(), current->reserved(), current->committed(),
    early->reserved(), early->committed(), current->flag());
}

void MemDetailDiffReporter::diff_virtual_memory_site(const NativeCallStack* stack, size_t current_reserved,
  size_t current_committed, size_t early_reserved, size_t early_committed, MEMFLAGS flag) const  {
  outputStream* out = output();

  // no change
  if (diff_in_current_scale(current_reserved, early_reserved) == 0 &&
      diff_in_current_scale(current_committed, early_committed) == 0) {
    return;
  }

  stack->print_on(out);
  out->print("%28s (mmap: ", " ");
  print_virtual_memory_diff(current_reserved, current_committed,
    early_reserved, early_committed);

  if (flag != mtNone) {
    out->print(" Type=%s", NMTUtil::flag_to_name(flag));
  }

  out->print_cr(")\n");
 }
