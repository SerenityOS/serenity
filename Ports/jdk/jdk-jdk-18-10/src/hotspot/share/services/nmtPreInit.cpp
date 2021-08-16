/*
 * Copyright (c) 2021 SAP SE. All rights reserved.
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/os.hpp"
#include "services/nmtPreInit.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"
#include "utilities/ostream.hpp"
#include "utilities/globalDefinitions.hpp"

#if INCLUDE_NMT

// Obviously we cannot use os::malloc for any dynamic allocation during pre-NMT-init, so we must use
// raw malloc; to make this very clear, wrap them.
static void* raw_malloc(size_t s)               { return ::malloc(s); }
static void* raw_realloc(void* old, size_t s)   { return ::realloc(old, s); }
static void  raw_free(void* p)                  { ::free(p); }

// We must ensure that the start of the payload area of the nmt lookup table nodes is malloc-aligned
static const size_t malloc_alignment = 2 * sizeof(void*); // could we use max_align_t?
STATIC_ASSERT(is_aligned(sizeof(NMTPreInitAllocation), malloc_alignment));

// --------- NMTPreInitAllocation --------------

NMTPreInitAllocation* NMTPreInitAllocation::do_alloc(size_t payload_size) {
  const size_t outer_size = sizeof(NMTPreInitAllocation) + payload_size;
  void* p = raw_malloc(outer_size);
  NMTPreInitAllocation* a = new(p) NMTPreInitAllocation(payload_size);
  return a;
}

NMTPreInitAllocation* NMTPreInitAllocation::do_reallocate(NMTPreInitAllocation* old, size_t new_payload_size) {
  assert(old->next == NULL, "unhang from map first");
  // We just reallocate the old block, header and all.
  const size_t new_outer_size = sizeof(NMTPreInitAllocation) + new_payload_size;
  void* p = raw_realloc(old, new_outer_size);
  // re-stamp header with new size
  NMTPreInitAllocation* a = new(p) NMTPreInitAllocation(new_payload_size);
  return a;
}

void NMTPreInitAllocation::do_free(NMTPreInitAllocation* p) {
  assert(p->next == NULL, "unhang from map first");
  raw_free(p);
}

// --------- NMTPreInitAllocationTable --------------

NMTPreInitAllocationTable::NMTPreInitAllocationTable() {
  ::memset(_entries, 0, sizeof(_entries));
}

// print a string describing the current state
void NMTPreInitAllocationTable::print_state(outputStream* st) const {
  // Collect some statistics and print them
  int num_entries = 0;
  int num_primary_entries = 0;
  int longest_chain = 0;
  size_t sum_bytes = 0;
  for (int i = 0; i < table_size; i++) {
    int chain_len = 0;
    for (NMTPreInitAllocation* a = _entries[i]; a != NULL; a = a->next) {
      chain_len++;
      sum_bytes += a->size;
    }
    if (chain_len > 0) {
      num_primary_entries++;
    }
    num_entries += chain_len;
    longest_chain = MAX2(chain_len, longest_chain);
  }
  st->print("entries: %d (primary: %d, empties: %d), sum bytes: " SIZE_FORMAT
            ", longest chain length: %d",
            num_entries, num_primary_entries, table_size - num_primary_entries,
            sum_bytes, longest_chain);
}

#ifdef ASSERT
void NMTPreInitAllocationTable::print_map(outputStream* st) const {
  for (int i = 0; i < table_size; i++) {
    st->print("[%d]: ", i);
    for (NMTPreInitAllocation* a = _entries[i]; a != NULL; a = a->next) {
      st->print( PTR_FORMAT "(" SIZE_FORMAT ") ", p2i(a->payload()), a->size);
    }
    st->cr();
  }
}

void NMTPreInitAllocationTable::verify() const {
  // This verifies the buildup of the lookup table, including the load and the chain lengths.
  // We should see chain lens of 0-1 under normal conditions. Under artificial conditions
  // (20000 VM args) we should see maybe 6-7. From a certain length on we can be sure something
  // is broken.
  const int longest_acceptable_chain_len = 30;
  int num_chains_too_long = 0;
  for (index_t i = 0; i < table_size; i++) {
    int len = 0;
    for (const NMTPreInitAllocation* a = _entries[i]; a != NULL; a = a->next) {
      index_t i2 = index_for_key(a->payload());
      assert(i2 == i, "wrong hash");
      assert(a->size > 0, "wrong size");
      len++;
      // very paranoid: search for dups
      bool found = false;
      for (const NMTPreInitAllocation* a2 = _entries[i]; a2 != NULL; a2 = a2->next) {
        if (a == a2) {
          assert(!found, "dup!");
          found = true;
        }
      }
    }
    if (len > longest_acceptable_chain_len) {
      num_chains_too_long++;
    }
  }
  if (num_chains_too_long > 0) {
    assert(false, "NMT preinit lookup table degenerated (%d/%d chains longer than %d)",
                  num_chains_too_long, table_size, longest_acceptable_chain_len);
  }
}
#endif // ASSERT

// --------- NMTPreinit --------------

NMTPreInitAllocationTable* NMTPreInit::_table = NULL;
bool NMTPreInit::_nmt_was_initialized = false;

// Some statistics
unsigned NMTPreInit::_num_mallocs_pre = 0;
unsigned NMTPreInit::_num_reallocs_pre = 0;
unsigned NMTPreInit::_num_frees_pre = 0;

void NMTPreInit::create_table() {
  assert(_table == NULL, "just once");
  void* p = raw_malloc(sizeof(NMTPreInitAllocationTable));
  _table = new(p) NMTPreInitAllocationTable();
}

// Allocate with os::malloc (hidden to prevent having to include os.hpp)
void* NMTPreInit::do_os_malloc(size_t size) {
  return os::malloc(size, mtNMT);
}

// Switches from NMT pre-init state to NMT post-init state;
//  in post-init, no modifications to the lookup table are possible.
void NMTPreInit::pre_to_post() {
  assert(_nmt_was_initialized == false, "just once");
  _nmt_was_initialized = true;
  DEBUG_ONLY(verify();)
}

#ifdef ASSERT
void NMTPreInit::verify() {
  if (_table != NULL) {
    _table->verify();
  }
  assert(_num_reallocs_pre <= _num_mallocs_pre &&
         _num_frees_pre <= _num_mallocs_pre, "stats are off");
}
#endif // ASSERT

void NMTPreInit::print_state(outputStream* st) {
  if (_table != NULL) {
    _table->print_state(st);
    st->cr();
  }
  st->print_cr("pre-init mallocs: %u, pre-init reallocs: %u, pre-init frees: %u",
               _num_mallocs_pre, _num_reallocs_pre, _num_frees_pre);
}

#endif // INCLUDE_NMT
