/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/gcLogPrecious.hpp"
#include "gc/shared/gc_globals.hpp"
#include "gc/z/zGlobals.hpp"
#include "runtime/globals.hpp"
#include "runtime/os.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/powerOfTwo.hpp"

#ifdef LINUX
#include <sys/mman.h>
#endif // LINUX

//
// The heap can have three different layouts, depending on the max heap size.
//
// Address Space & Pointer Layout 1
// --------------------------------
//
//  +--------------------------------+ 0x00007FFFFFFFFFFF (127TB)
//  .                                .
//  .                                .
//  .                                .
//  +--------------------------------+ 0x0000014000000000 (20TB)
//  |         Remapped View          |
//  +--------------------------------+ 0x0000010000000000 (16TB)
//  .                                .
//  +--------------------------------+ 0x00000c0000000000 (12TB)
//  |         Marked1 View           |
//  +--------------------------------+ 0x0000080000000000 (8TB)
//  |         Marked0 View           |
//  +--------------------------------+ 0x0000040000000000 (4TB)
//  .                                .
//  +--------------------------------+ 0x0000000000000000
//
//   6                  4 4  4 4
//   3                  6 5  2 1                                             0
//  +--------------------+----+-----------------------------------------------+
//  |00000000 00000000 00|1111|11 11111111 11111111 11111111 11111111 11111111|
//  +--------------------+----+-----------------------------------------------+
//  |                    |    |
//  |                    |    * 41-0 Object Offset (42-bits, 4TB address space)
//  |                    |
//  |                    * 45-42 Metadata Bits (4-bits)  0001 = Marked0      (Address view 4-8TB)
//  |                                                    0010 = Marked1      (Address view 8-12TB)
//  |                                                    0100 = Remapped     (Address view 16-20TB)
//  |                                                    1000 = Finalizable  (Address view N/A)
//  |
//  * 63-46 Fixed (18-bits, always zero)
//
//
// Address Space & Pointer Layout 2
// --------------------------------
//
//  +--------------------------------+ 0x00007FFFFFFFFFFF (127TB)
//  .                                .
//  .                                .
//  .                                .
//  +--------------------------------+ 0x0000280000000000 (40TB)
//  |         Remapped View          |
//  +--------------------------------+ 0x0000200000000000 (32TB)
//  .                                .
//  +--------------------------------+ 0x0000180000000000 (24TB)
//  |         Marked1 View           |
//  +--------------------------------+ 0x0000100000000000 (16TB)
//  |         Marked0 View           |
//  +--------------------------------+ 0x0000080000000000 (8TB)
//  .                                .
//  +--------------------------------+ 0x0000000000000000
//
//   6                 4 4  4 4
//   3                 7 6  3 2                                              0
//  +------------------+-----+------------------------------------------------+
//  |00000000 00000000 0|1111|111 11111111 11111111 11111111 11111111 11111111|
//  +-------------------+----+------------------------------------------------+
//  |                   |    |
//  |                   |    * 42-0 Object Offset (43-bits, 8TB address space)
//  |                   |
//  |                   * 46-43 Metadata Bits (4-bits)  0001 = Marked0      (Address view 8-16TB)
//  |                                                   0010 = Marked1      (Address view 16-24TB)
//  |                                                   0100 = Remapped     (Address view 32-40TB)
//  |                                                   1000 = Finalizable  (Address view N/A)
//  |
//  * 63-47 Fixed (17-bits, always zero)
//
//
// Address Space & Pointer Layout 3
// --------------------------------
//
//  +--------------------------------+ 0x00007FFFFFFFFFFF (127TB)
//  .                                .
//  .                                .
//  .                                .
//  +--------------------------------+ 0x0000500000000000 (80TB)
//  |         Remapped View          |
//  +--------------------------------+ 0x0000400000000000 (64TB)
//  .                                .
//  +--------------------------------+ 0x0000300000000000 (48TB)
//  |         Marked1 View           |
//  +--------------------------------+ 0x0000200000000000 (32TB)
//  |         Marked0 View           |
//  +--------------------------------+ 0x0000100000000000 (16TB)
//  .                                .
//  +--------------------------------+ 0x0000000000000000
//
//   6               4  4  4 4
//   3               8  7  4 3                                               0
//  +------------------+----+-------------------------------------------------+
//  |00000000 00000000 |1111|1111 11111111 11111111 11111111 11111111 11111111|
//  +------------------+----+-------------------------------------------------+
//  |                  |    |
//  |                  |    * 43-0 Object Offset (44-bits, 16TB address space)
//  |                  |
//  |                  * 47-44 Metadata Bits (4-bits)  0001 = Marked0      (Address view 16-32TB)
//  |                                                  0010 = Marked1      (Address view 32-48TB)
//  |                                                  0100 = Remapped     (Address view 64-80TB)
//  |                                                  1000 = Finalizable  (Address view N/A)
//  |
//  * 63-48 Fixed (16-bits, always zero)
//

// Default value if probing is not implemented for a certain platform: 128TB
static const size_t DEFAULT_MAX_ADDRESS_BIT = 47;
// Minimum value returned, if probing fails: 64GB
static const size_t MINIMUM_MAX_ADDRESS_BIT = 36;

static size_t probe_valid_max_address_bit() {
#ifdef LINUX
  size_t max_address_bit = 0;
  const size_t page_size = os::vm_page_size();
  for (size_t i = DEFAULT_MAX_ADDRESS_BIT; i > MINIMUM_MAX_ADDRESS_BIT; --i) {
    const uintptr_t base_addr = ((uintptr_t) 1U) << i;
    if (msync((void*)base_addr, page_size, MS_ASYNC) == 0) {
      // msync suceeded, the address is valid, and maybe even already mapped.
      max_address_bit = i;
      break;
    }
    if (errno != ENOMEM) {
      // Some error occured. This should never happen, but msync
      // has some undefined behavior, hence ignore this bit.
#ifdef ASSERT
      fatal("Received '%s' while probing the address space for the highest valid bit", os::errno_name(errno));
#else // ASSERT
      log_warning_p(gc)("Received '%s' while probing the address space for the highest valid bit", os::errno_name(errno));
#endif // ASSERT
      continue;
    }
    // Since msync failed with ENOMEM, the page might not be mapped.
    // Try to map it, to see if the address is valid.
    void* const result_addr = mmap((void*) base_addr, page_size, PROT_NONE, MAP_PRIVATE|MAP_ANONYMOUS|MAP_NORESERVE, -1, 0);
    if (result_addr != MAP_FAILED) {
      munmap(result_addr, page_size);
    }
    if ((uintptr_t) result_addr == base_addr) {
      // address is valid
      max_address_bit = i;
      break;
    }
  }
  if (max_address_bit == 0) {
    // probing failed, allocate a very high page and take that bit as the maximum
    const uintptr_t high_addr = ((uintptr_t) 1U) << DEFAULT_MAX_ADDRESS_BIT;
    void* const result_addr = mmap((void*) high_addr, page_size, PROT_NONE, MAP_PRIVATE|MAP_ANONYMOUS|MAP_NORESERVE, -1, 0);
    if (result_addr != MAP_FAILED) {
      max_address_bit = BitsPerSize_t - count_leading_zeros((size_t) result_addr) - 1;
      munmap(result_addr, page_size);
    }
  }
  log_info_p(gc, init)("Probing address space for the highest valid bit: " SIZE_FORMAT, max_address_bit);
  return MAX2(max_address_bit, MINIMUM_MAX_ADDRESS_BIT);
#else // LINUX
  return DEFAULT_MAX_ADDRESS_BIT;
#endif // LINUX
}

size_t ZPlatformAddressOffsetBits() {
  const static size_t valid_max_address_offset_bits = probe_valid_max_address_bit() + 1;
  const size_t max_address_offset_bits = valid_max_address_offset_bits - 3;
  const size_t min_address_offset_bits = max_address_offset_bits - 2;
  const size_t address_offset = round_up_power_of_2(MaxHeapSize * ZVirtualToPhysicalRatio);
  const size_t address_offset_bits = log2i_exact(address_offset);
  return clamp(address_offset_bits, min_address_offset_bits, max_address_offset_bits);
}

size_t ZPlatformAddressMetadataShift() {
  return ZPlatformAddressOffsetBits();
}
