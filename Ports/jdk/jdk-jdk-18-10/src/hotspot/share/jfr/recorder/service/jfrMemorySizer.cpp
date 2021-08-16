/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/recorder/service/jfrMemorySizer.hpp"
#include "logging/log.hpp"
#include "runtime/os.hpp"

const julong MAX_ADJUSTED_GLOBAL_BUFFER_SIZE = 1 * M;
const julong MIN_ADJUSTED_GLOBAL_BUFFER_SIZE_CUTOFF = 512 * K;
const julong MIN_GLOBAL_BUFFER_SIZE = 64 * K;
const julong MAX_GLOBAL_BUFFER_SIZE = 2 * G;
// implies at least 2 * MIN_GLOBAL_BUFFER SIZE
const julong MIN_BUFFER_COUNT = 2;
// MAX global buffer count open ended
const julong DEFAULT_BUFFER_COUNT = 20;
// MAX thread local buffer size == size of a single global buffer (runtime determined)
// DEFAULT thread local buffer size = 2 * os page size (runtime determined)
const julong MIN_THREAD_BUFFER_SIZE = 4 * K;
const julong MAX_THREAD_BUFFER_SIZE = 2 * G;
const julong MIN_MEMORY_SIZE = 1 * M;
const julong DEFAULT_MEMORY_SIZE = 10 * M;

//
// In pages:
//
// units = total_pages / per_unit_pages
//
static julong div_pages(julong& total_pages, julong& per_unit_pages) {
  assert(total_pages > 0, "invariant");
  assert(per_unit_pages > 0, "invariant");
  assert(total_pages >= per_unit_pages, "invariant");

  const julong units = total_pages / per_unit_pages;
  const julong rem = total_pages % per_unit_pages;

  assert(units > 0, "invariant");

  if (rem > 0) {
    total_pages -= rem % units;
    per_unit_pages += rem / units;
  }

  assert(per_unit_pages > 0, "invariant");
  assert(total_pages % units == 0, "invariant");
  assert(units * per_unit_pages == total_pages, "invariant");
  assert(units == total_pages / per_unit_pages, "invariant");

  return units;
}

static void page_size_align_up(julong& value) {
  static const julong alignment = os::vm_page_size() - 1;
  value = (value + alignment) & ~alignment;
}

//
// In bytes:
// units = total_bytes / per_unit_bytes
//
static julong div_total_by_per_unit(julong& total_bytes, julong& per_unit_bytes) {
  assert(total_bytes > 0, "invariant");
  assert(per_unit_bytes > 0, "invariant");
  assert(total_bytes >= per_unit_bytes, "invariant");

  page_size_align_up(total_bytes);
  assert(total_bytes % os::vm_page_size() == 0, "invariant");
  julong total_pages = total_bytes / os::vm_page_size();

  page_size_align_up(per_unit_bytes);
  assert(per_unit_bytes % os::vm_page_size() == 0, "invariant");
  julong per_unit_pages = per_unit_bytes / os::vm_page_size();

  const julong units = div_pages(total_pages, per_unit_pages);
  assert(units > 0, "invariant");

  total_bytes = total_pages * os::vm_page_size();
  per_unit_bytes = per_unit_pages * os::vm_page_size();

  assert(per_unit_bytes > 0, "invariant");
  assert(total_bytes / per_unit_bytes == units, "invariant");

  return units;
}

//
// per_unit_bytes = total_bytes / units
//
static julong div_total_by_units(julong& total_bytes, julong& units) {
  page_size_align_up(total_bytes);
  assert(total_bytes % os::vm_page_size() == 0, "invariant");
  julong total_pages = total_bytes / os::vm_page_size();
  assert(units > 0, "invariant");

  julong per_unit_pages = total_pages <= units ? 1 : total_pages / units;
  units = div_pages(total_pages, per_unit_pages);

  julong per_unit_bytes = per_unit_pages * os::vm_page_size();
  assert(per_unit_bytes % os::vm_page_size() == 0, "invariant");

  total_bytes = total_pages * os::vm_page_size();
  assert(total_bytes % os::vm_page_size() == 0, "invariant");

  assert(total_bytes % units == 0, "invariant");
  assert(total_bytes / units == per_unit_bytes, "invariant");
  assert(units * per_unit_bytes == total_bytes, "invariant");

  return per_unit_bytes;
}

//
// total_bytes = per_unit_bytes * units;
//
static julong multiply(julong& per_unit_bytes, julong& units) {
  page_size_align_up(per_unit_bytes);
  assert(per_unit_bytes % os::vm_page_size() == 0, "invariant");
  assert(units > 0, "invariant");

  julong total_bytes = per_unit_bytes * units;
  assert(total_bytes % os::vm_page_size() == 0, "invariant");

  assert(total_bytes % units == 0, "invariant");
  assert(total_bytes / units == per_unit_bytes, "invariant");
  assert(units * per_unit_bytes == total_bytes, "invariant");

  return total_bytes;
}

// Total_bytes is explicitly set.
//
// Deduce other parameters by delegating to a sizing policy
template <typename SizingPolicy>
static julong adjust(JfrMemoryOptions* options) {
  page_size_align_up(options->memory_size);
  assert(options->memory_size % os::vm_page_size() == 0, "invariant");
  julong total_pages = options->memory_size / os::vm_page_size();
  assert(options->buffer_count > 0, "invariant");
  julong per_unit_pages = total_pages / options->buffer_count;
  page_size_align_up(options->thread_buffer_size);
  assert(options->thread_buffer_size % os::vm_page_size() == 0, "invariant");
  julong thread_buffer_pages = options->thread_buffer_size / os::vm_page_size();

  SizingPolicy::adjust(total_pages, per_unit_pages, options->buffer_count, thread_buffer_pages, options->thread_buffer_size_configured);
  assert(options->buffer_count * per_unit_pages == total_pages, "invariant");

  const julong per_unit_bytes = per_unit_pages * os::vm_page_size();
  options->memory_size = total_pages * os::vm_page_size();
  options->thread_buffer_size = thread_buffer_pages * os::vm_page_size();

  assert(options->memory_size % options->buffer_count == 0, "invariant");
  assert(options->memory_size / options->buffer_count == per_unit_bytes, "invariant");
  assert(options->buffer_count * per_unit_bytes == options->memory_size, "invariant");
  assert(per_unit_bytes >= options->thread_buffer_size, "invariant");
  return per_unit_bytes;
}

static void align_buffer_size(julong& buffer_size_in_pages, julong max_size_pages, julong min_size_pages, bool sizeup = false) {
  buffer_size_in_pages = MIN2(buffer_size_in_pages, max_size_pages);
  buffer_size_in_pages = MAX2(buffer_size_in_pages, min_size_pages);
  size_t multiples = 0;
  if (buffer_size_in_pages < max_size_pages) {
    while (buffer_size_in_pages >=
      (min_size_pages << (multiples + (sizeup ? 0 : 1)))) {
      ++multiples;
    }
    buffer_size_in_pages = min_size_pages << multiples;
  }
  assert(buffer_size_in_pages >= min_size_pages && buffer_size_in_pages <= max_size_pages, "invariant");
}

static void adjust_buffer_size_to_total_memory_size(julong& total_pages, julong& buffer_size_pages) {
  static const julong max_buffer_size_pages = MAX_ADJUSTED_GLOBAL_BUFFER_SIZE / os::vm_page_size();
  // If memory size is less than DEFAULT_MEMORY_SIZE,
  // the adjustment algorithm can decrease the size of the global buffer
  // all the way down to the MIN_GLOBAL_BUFFER_SIZE (taking embedded use case in account).
  // If memory size is larger than DEFAULT_MEMORY_SIZE, the lowest size of
  // a global buffer will be the size of MIN_ADJUSTED_GLOBAL_BUFFER_SIZE_CUTOFF
  static const julong min_buffer_size_pages =
    total_pages * os::vm_page_size() < DEFAULT_MEMORY_SIZE ?
      MIN_GLOBAL_BUFFER_SIZE / os::vm_page_size() :
      MIN_ADJUSTED_GLOBAL_BUFFER_SIZE_CUTOFF / os::vm_page_size();

  align_buffer_size(buffer_size_pages, max_buffer_size_pages, min_buffer_size_pages);
  assert(buffer_size_pages % min_buffer_size_pages == 0, "invariant");

  julong remainder = total_pages % buffer_size_pages;
  while (remainder >= (buffer_size_pages >> 1)) {
    if (buffer_size_pages <= min_buffer_size_pages) {
      break;
    }
    buffer_size_pages >>= 1;
    remainder = total_pages % buffer_size_pages;
  }
}

// Sizing policy class
class ScaleOutAdjuster : public AllStatic {
 public:
  static void adjust(julong& total_pages,
                     julong& buffer_size_pages,
                     julong& buffer_count,
                     julong& thread_buffer_size_pages,
                     bool is_thread_buffer_size_set) {
    assert(buffer_count > 0, "invariant");
    adjust_buffer_size_to_total_memory_size(total_pages, buffer_size_pages);
    assert(buffer_size_pages * os::vm_page_size() >= MIN_GLOBAL_BUFFER_SIZE, "invariant");
    assert((buffer_size_pages * os::vm_page_size()) % MIN_GLOBAL_BUFFER_SIZE == 0, "invariant");
    if (is_thread_buffer_size_set) {
      if (thread_buffer_size_pages > buffer_size_pages) {
        buffer_size_pages = thread_buffer_size_pages;
      }
    }
    // and with this information, calculate what the new buffer count will be
    buffer_count = div_pages(total_pages, buffer_size_pages);
  }
};

static void memory_and_thread_buffer_size(JfrMemoryOptions* options) {
  assert(options->memory_size_configured, "invariant");
  assert(!options->buffer_count_configured, "invariant");
  assert(!options->global_buffer_size_configured, "invariant");
  // here the only thing specified is the overall total memory size
  // we can and will apply some sizing heuristics to derive both
  // the size of an individual global buffer and by implication the number of global
  // buffers to use. Starting values for buffer count and global_buffer_size
  // will be the defaults.
  options->global_buffer_size = adjust<ScaleOutAdjuster>(options);
}

static void memory_size_and_buffer_count(JfrMemoryOptions* options) {
  assert(options->memory_size_configured, "invariant");
  assert(!options->global_buffer_size_configured, "invariant");
  assert(!options->thread_buffer_size_configured, "invariant");
  assert(options->buffer_count_configured, "invariant");
  options->global_buffer_size = div_total_by_units(options->memory_size, options->buffer_count);
}

static void memory_size_and_global_buffer_size(JfrMemoryOptions* options) {
  assert(options->memory_size_configured, "invariant");
  assert(options->global_buffer_size_configured, "invariant");
  assert(!options->buffer_count_configured, "invariant");
  page_size_align_up(options->thread_buffer_size);
  options->buffer_count = div_total_by_per_unit(options->memory_size, options->global_buffer_size);
  if (options->thread_buffer_size > options->global_buffer_size) {
    options->global_buffer_size = options->thread_buffer_size;
    options->buffer_count = div_total_by_per_unit(options->memory_size, options->global_buffer_size);
  }
  assert(options->global_buffer_size >= options->thread_buffer_size, "invariant");
}

static bool is_ambiguous(const JfrMemoryOptions* options) {
  assert(options->memory_size_configured, "invariant");
  assert(options->global_buffer_size_configured, "invariant");
  assert(options->buffer_count_configured, "invariant");
  assert(options->thread_buffer_size <= options->global_buffer_size, "invariant");
  // This can cause an ambiguous situation because all three parameters are explicitly set.
  return options->global_buffer_size * options->buffer_count != options->memory_size;
}

static void all_options_set(JfrMemoryOptions* options) {
  options->buffer_count = div_total_by_per_unit(options->memory_size, options->global_buffer_size);
  page_size_align_up(options->thread_buffer_size);
  if (options->thread_buffer_size > options->global_buffer_size) {
    options->global_buffer_size = options->thread_buffer_size;
    options->buffer_count = div_total_by_per_unit(options->memory_size, options->global_buffer_size);
  }
  assert(options->global_buffer_size >= options->thread_buffer_size, "invariant");
  assert(options->memory_size / options->global_buffer_size == options->buffer_count, "invariant");
  assert(options->memory_size % options->global_buffer_size == 0, "invariant");
}

static void global_buffer_size(JfrMemoryOptions* options) {
  assert(!options->memory_size_configured, "invariant");
  page_size_align_up(options->thread_buffer_size);
  if (options->thread_buffer_size > options->global_buffer_size) {
    options->global_buffer_size = options->thread_buffer_size;
  }
  options->memory_size = multiply(options->global_buffer_size, options->buffer_count);
  assert(options->global_buffer_size >= options->thread_buffer_size, "invariant");
}

static void thread_buffer_size(JfrMemoryOptions* options) {
  assert(!options->global_buffer_size_configured, "invariant");
  assert(options->thread_buffer_size_configured, "invariant");
  page_size_align_up(options->thread_buffer_size);
  options->global_buffer_size = div_total_by_units(options->memory_size, options->buffer_count);
  if (options->thread_buffer_size > options->global_buffer_size) {
    options->global_buffer_size = options->thread_buffer_size;
    if (options->memory_size_configured) {
      options->buffer_count = div_total_by_per_unit(options->memory_size, options->global_buffer_size);
    } else {
      options->memory_size = multiply(options->global_buffer_size, options->buffer_count);
    }
    options->buffer_count = div_total_by_per_unit(options->memory_size, options->global_buffer_size);
  }
  assert(options->global_buffer_size >= options->thread_buffer_size, "invariant");
}

static void default_size(const JfrMemoryOptions* options) {
  // no memory options explicitly set
  // default values already statically adjusted
  assert(!options->thread_buffer_size_configured, "invariant");
  assert(!options->memory_size_configured, "invariant");
  assert(!options->buffer_count_configured, "invarinat");
  assert(!options->global_buffer_size_configured, "invariant");
}

#ifdef ASSERT
static void assert_post_condition(const JfrMemoryOptions* options) {
  assert(options->memory_size % os::vm_page_size() == 0, "invariant");
  assert(options->global_buffer_size % os::vm_page_size() == 0, "invariant");
  assert(options->thread_buffer_size % os::vm_page_size() == 0, "invariant");
  assert(options->buffer_count >= MIN_BUFFER_COUNT, "invariant");
  assert(options->global_buffer_size >= options->thread_buffer_size, "invariant");
}
#endif

// MEMORY SIZING ALGORITHM

bool JfrMemorySizer::adjust_options(JfrMemoryOptions* options) {
  assert(options != NULL, "invariant");

  enum MemoryOptions {
    MEMORY_SIZE = 1,
    GLOBAL_BUFFER_SIZE = 2,
    GLOBAL_BUFFER_COUNT = 4,
    THREAD_BUFFER_SIZE = 8
  };

  // LEGEND
  //
  // M = "memorysize" option
  // G = "globalbuffersize" option
  // C = "numglobalbuffers" option
  // T = "threadbuffersize" option
  //
  // The memory options comprise an n-set (a 4-set) = { M, G, C, T }
  //
  // Number of r-subsets = 5 (0, 1, 2, 3, 4) (including null set)
  //
  // Unordered selection:
  //
  // C(4, 0) = {} = NULL set = 1
  // C(4, 1) = { (M), (G), (C), (T) } = 4
  // C(4, 2) = { (M, G), (M, C), (M, T), (G, C), (G, T), (C, T) } = 6
  // C(4, 3) = { (M, G, C), (M, G, T), (M, C, T), (G, C, T) } = 4
  // C(4, 4) = { (M, G, C, T) } = 1
  //
  // in shorter terms: P({ M, G, C, T}) = 16
  //
#define MG   (MEMORY_SIZE | GLOBAL_BUFFER_SIZE)
#define MC   (MEMORY_SIZE | GLOBAL_BUFFER_COUNT)
#define MT   (MEMORY_SIZE | THREAD_BUFFER_SIZE)
#define MGC  (MG | GLOBAL_BUFFER_COUNT)
#define MGT  (MG | THREAD_BUFFER_SIZE)
#define MCT  (MC | THREAD_BUFFER_SIZE)
#define MGCT (MGC | THREAD_BUFFER_SIZE)
#define GC   (GLOBAL_BUFFER_SIZE | GLOBAL_BUFFER_COUNT)
#define GT   (GLOBAL_BUFFER_SIZE | THREAD_BUFFER_SIZE)
#define GCT  (GC | THREAD_BUFFER_SIZE)
#define CT   (GLOBAL_BUFFER_COUNT | THREAD_BUFFER_SIZE)

  int set_of_options = 0;

  if (options->memory_size_configured) {
    set_of_options |= MEMORY_SIZE;
  }
  if (options->global_buffer_size_configured) {
    set_of_options |= GLOBAL_BUFFER_SIZE;
  }
  if (options->buffer_count_configured) {
    set_of_options |= GLOBAL_BUFFER_COUNT;
  }
  if (options->thread_buffer_size_configured) {
    set_of_options |= THREAD_BUFFER_SIZE;
  }

  switch (set_of_options) {
    case MT:
    case MEMORY_SIZE:
      memory_and_thread_buffer_size(options);
      break;
    case MC:
      memory_size_and_buffer_count(options);
      break;
    case MGT:
      assert(options->thread_buffer_size_configured, "invariant");
    case MG:
      memory_size_and_global_buffer_size(options);
      break;
    case MGC:
    case MGCT:
      if (is_ambiguous(options)) {
        // Let the user resolve the ambiguity by bailing.
        return false;
      }
      all_options_set(options);
      break;
    case GCT:
      assert(options->buffer_count_configured, "invariant");
      assert(options->thread_buffer_size_configured, "invariant");
    case GC:
      assert(options->global_buffer_size_configured, "invariant");
    case GT:
    case GLOBAL_BUFFER_COUNT:
    case GLOBAL_BUFFER_SIZE:
      global_buffer_size(options);
      break;
    case MCT:
      assert(options->memory_size_configured, "invariant");
    case CT:
      assert(options->buffer_count_configured, "invariant");
    case THREAD_BUFFER_SIZE:
      thread_buffer_size(options);
      break;
    default:
      default_size(options);
  }
  if (options->buffer_count < MIN_BUFFER_COUNT ||
      options->global_buffer_size < options->thread_buffer_size) {
    return false;
  }
  DEBUG_ONLY(assert_post_condition(options);)
  return true;
}
