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
 *
 */

#include "precompiled.hpp"
#include "logging/log.hpp"
#include "runtime/globals.hpp"
#include "runtime/os.hpp"
#include "runtime/safepointMechanism.hpp"
#include "services/memTracker.hpp"
#include <sys/mman.h>

void SafepointMechanism::pd_initialize() {
  // No special code needed if we can use SIGTRAP
  if (USE_POLL_BIT_ONLY) {
    default_initialize();
    return;
  }

  // Poll bit values
  _poll_word_armed_value    = poll_bit();
  _poll_word_disarmed_value = ~_poll_word_armed_value;

  // Allocate one protected page
  char* map_address = (char*)MAP_FAILED;
  const size_t page_size = os::vm_page_size();
  const size_t map_size = 2 * page_size;
  const int prot  = PROT_READ;
  const int flags = MAP_PRIVATE | MAP_ANONYMOUS;

  // Use optimized addresses for the polling page,
  // e.g. map it to a special 32-bit address.
  if (OptimizePollingPageLocation) {
    // architecture-specific list of address wishes:
    char* address_wishes[] = {
        // AIX: addresses lower than 0x30000000 don't seem to work on AIX.
        // PPC64: all address wishes are non-negative 32 bit values where
        // the lower 16 bits are all zero. we can load these addresses
        // with a single ppc_lis instruction.
        (char*) 0x30000000, (char*) 0x31000000,
        (char*) 0x32000000, (char*) 0x33000000,
        (char*) 0x40000000, (char*) 0x41000000,
        (char*) 0x42000000, (char*) 0x43000000,
        (char*) 0x50000000, (char*) 0x51000000,
        (char*) 0x52000000, (char*) 0x53000000,
        (char*) 0x60000000, (char*) 0x61000000,
        (char*) 0x62000000, (char*) 0x63000000
    };
    int address_wishes_length = sizeof(address_wishes)/sizeof(char*);

    // iterate over the list of address wishes:
    for (int i = 0; i < address_wishes_length; i++) {
      // Try to map with current address wish.
      // AIX: AIX needs MAP_FIXED if we provide an address and mmap will
      // fail if the address is already mapped.
      map_address = (char*) ::mmap(address_wishes[i],
                                   map_size, prot,
                                   flags | MAP_FIXED,
                                   -1, 0);
      log_debug(os)("SafePoint Polling Page address: %p (wish) => %p",
                    address_wishes[i], map_address);

      if (map_address == address_wishes[i]) {
        // Map succeeded and map_address is at wished address, exit loop.
        break;
      }

      if (map_address != (char*)MAP_FAILED) {
        // Map succeeded, but polling_page is not at wished address, unmap and continue.
        ::munmap(map_address, map_size);
        map_address = (char*)MAP_FAILED;
      }
      // Map failed, continue loop.
    }
  }
  if (map_address == (char*)MAP_FAILED) {
    map_address = (char*) ::mmap(NULL, map_size, prot, flags, -1, 0);
  }
  guarantee(map_address != (char*)MAP_FAILED && map_address != NULL,
            "SafepointMechanism::pd_initialize: failed to allocate polling page");
  log_info(os)("SafePoint Polling address: " INTPTR_FORMAT, p2i(map_address));
  _polling_page = (address)(map_address);

  // Register polling page with NMT.
  MemTracker::record_virtual_memory_reserve_and_commit(map_address, map_size, CALLER_PC, mtSafepoint);

  // Use same page for thread local handshakes without SIGTRAP
  if (!os::guard_memory((char*)_polling_page, page_size)) {
    fatal("Could not protect polling page");
  }
  uintptr_t bad_page_val  = reinterpret_cast<uintptr_t>(map_address),
            good_page_val = bad_page_val + os::vm_page_size();
  _poll_page_armed_value    = bad_page_val;
  _poll_page_disarmed_value = good_page_val;
}
