/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/parallel/psVirtualspace.hpp"
#include "memory/virtualspace.hpp"
#include "runtime/os.hpp"

// PSVirtualSpace

PSVirtualSpace::PSVirtualSpace(ReservedSpace rs, size_t alignment) :
  _alignment(alignment)
{
  set_reserved(rs);
  set_committed(reserved_low_addr(), reserved_low_addr());
  DEBUG_ONLY(verify());
}

PSVirtualSpace::PSVirtualSpace(ReservedSpace rs) :
  _alignment(os::vm_page_size())
{
  set_reserved(rs);
  set_committed(reserved_low_addr(), reserved_low_addr());
  DEBUG_ONLY(verify());
}

// Deprecated.
PSVirtualSpace::PSVirtualSpace():
  _alignment(os::vm_page_size()),
  _reserved_low_addr(NULL),
  _reserved_high_addr(NULL),
  _committed_low_addr(NULL),
  _committed_high_addr(NULL),
  _special(false) {
}

// Deprecated.
bool PSVirtualSpace::initialize(ReservedSpace rs,
                                size_t commit_size) {
  set_reserved(rs);
  set_committed(reserved_low_addr(), reserved_low_addr());

  // Commit to initial size.
  assert(commit_size <= rs.size(), "commit_size too big");
  bool result = commit_size > 0 ? expand_by(commit_size) : true;
  DEBUG_ONLY(verify());
  return result;
}

PSVirtualSpace::~PSVirtualSpace() {
  release();
}

bool PSVirtualSpace::contains(void* p) const {
  char* const cp = (char*)p;
  return cp >= committed_low_addr() && cp < committed_high_addr();
}

void PSVirtualSpace::release() {
  DEBUG_ONLY(PSVirtualSpaceVerifier this_verifier(this));
  // This may not release memory it didn't reserve.
  // Use rs.release() to release the underlying memory instead.
  _reserved_low_addr = _reserved_high_addr = NULL;
  _committed_low_addr = _committed_high_addr = NULL;
  _special = false;
}

bool PSVirtualSpace::expand_by(size_t bytes) {
  assert(is_aligned(bytes), "arg not aligned");
  DEBUG_ONLY(PSVirtualSpaceVerifier this_verifier(this));

  if (uncommitted_size() < bytes) {
    return false;
  }

  char* const base_addr = committed_high_addr();
  bool result = special() ||
         os::commit_memory(base_addr, bytes, alignment(), !ExecMem);
  if (result) {
    _committed_high_addr += bytes;
  }

  return result;
}

bool PSVirtualSpace::shrink_by(size_t bytes) {
  assert(is_aligned(bytes), "arg not aligned");
  DEBUG_ONLY(PSVirtualSpaceVerifier this_verifier(this));

  if (committed_size() < bytes) {
    return false;
  }

  char* const base_addr = committed_high_addr() - bytes;
  bool result = special() || os::uncommit_memory(base_addr, bytes);
  if (result) {
    _committed_high_addr -= bytes;
  }

  return result;
}

size_t
PSVirtualSpace::expand_into(PSVirtualSpace* other_space, size_t bytes) {
  assert(is_aligned(bytes), "arg not aligned");
  assert(grows_up(), "this space must grow up");
  assert(other_space->grows_down(), "other space must grow down");
  assert(reserved_high_addr() == other_space->reserved_low_addr(),
         "spaces not contiguous");
  assert(special() == other_space->special(), "one space is special, the other is not");
  DEBUG_ONLY(PSVirtualSpaceVerifier this_verifier(this));
  DEBUG_ONLY(PSVirtualSpaceVerifier other_verifier(other_space));

  size_t bytes_needed = bytes;

  // First use the uncommitted region in this space.
  size_t tmp_bytes = MIN2(uncommitted_size(), bytes_needed);
  if (tmp_bytes > 0) {
    if (expand_by(tmp_bytes)) {
      bytes_needed -= tmp_bytes;
    } else {
      return 0;
    }
  }

  // Next take from the uncommitted region in the other space, and commit it.
  tmp_bytes = MIN2(other_space->uncommitted_size(), bytes_needed);
  if (tmp_bytes > 0) {
    char* const commit_base = committed_high_addr();
    if (other_space->special() ||
        os::commit_memory(commit_base, tmp_bytes, alignment(), !ExecMem)) {
      // Reduce the reserved region in the other space.
      other_space->set_reserved(other_space->reserved_low_addr() + tmp_bytes,
                                other_space->reserved_high_addr(),
                                other_space->special());

      // Grow both reserved and committed in this space.
      _reserved_high_addr += tmp_bytes;
      _committed_high_addr += tmp_bytes;
      bytes_needed -= tmp_bytes;
    } else {
      return bytes - bytes_needed;
    }
  }

  // Finally take from the already committed region in the other space.
  tmp_bytes = bytes_needed;
  if (tmp_bytes > 0) {
    // Reduce both committed and reserved in the other space.
    other_space->set_committed(other_space->committed_low_addr() + tmp_bytes,
                               other_space->committed_high_addr());
    other_space->set_reserved(other_space->reserved_low_addr() + tmp_bytes,
                              other_space->reserved_high_addr(),
                              other_space->special());

    // Grow both reserved and committed in this space.
    _reserved_high_addr += tmp_bytes;
    _committed_high_addr += tmp_bytes;
  }

  return bytes;
}

#ifndef PRODUCT
bool PSVirtualSpace::is_aligned(size_t value, size_t align) {
  const size_t tmp_value = value + align - 1;
  const size_t mask = ~(align - 1);
  return (tmp_value & mask) == value;
}

bool PSVirtualSpace::is_aligned(size_t value) const {
  return is_aligned(value, alignment());
}

bool PSVirtualSpace::is_aligned(char* value) const {
  return is_aligned((size_t)value);
}

void PSVirtualSpace::verify() const {
  assert(is_aligned(alignment(), os::vm_page_size()), "bad alignment");
  assert(is_aligned(reserved_low_addr()), "bad reserved_low_addr");
  assert(is_aligned(reserved_high_addr()), "bad reserved_high_addr");
  assert(is_aligned(committed_low_addr()), "bad committed_low_addr");
  assert(is_aligned(committed_high_addr()), "bad committed_high_addr");

  // Reserved region must be non-empty or both addrs must be 0.
  assert(reserved_low_addr() < reserved_high_addr() ||
         reserved_low_addr() == NULL && reserved_high_addr() == NULL,
         "bad reserved addrs");
  assert(committed_low_addr() <= committed_high_addr(), "bad committed addrs");

  if (grows_up()) {
    assert(reserved_low_addr() == committed_low_addr(), "bad low addrs");
    assert(reserved_high_addr() >= committed_high_addr(), "bad high addrs");
  } else {
    assert(reserved_high_addr() == committed_high_addr(), "bad high addrs");
    assert(reserved_low_addr() <= committed_low_addr(), "bad low addrs");
  }
}

#endif // #ifndef PRODUCT

void PSVirtualSpace::print_space_boundaries_on(outputStream* st) const {
  st->print_cr(" [" PTR_FORMAT ", " PTR_FORMAT ", " PTR_FORMAT ")",
               p2i(low_boundary()), p2i(high()), p2i(high_boundary()));
}
