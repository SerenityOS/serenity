/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2013 SAP SE. All rights reserved.
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

#ifndef CPU_PPC_ICACHE_PPC_HPP
#define CPU_PPC_ICACHE_PPC_HPP

// Interface for updating the instruction cache.  Whenever the VM modifies
// code, part of the processor instruction cache potentially has to be flushed.

class ICache : public AbstractICache {
  friend class ICacheStubGenerator;
  static int ppc64_flush_icache(address start, int lines, int magic);

 public:
  enum {
    // Actually, cache line size is 64, but keeping it as it is to be
    // on the safe side on ALL PPC64 implementations.
    log2_line_size = 5,
    line_size      = 1 << log2_line_size
  };

  static void ppc64_flush_icache_bytes(address start, int bytes) {
    // Align start address to an icache line boundary and transform
    // nbytes to an icache line count.
    const uint line_offset = mask_address_bits(start, line_size - 1);
    ppc64_flush_icache(start - line_offset, (bytes + line_offset + line_size - 1) >> log2_line_size, 0);
  }
};

#endif // CPU_PPC_ICACHE_PPC_HPP
