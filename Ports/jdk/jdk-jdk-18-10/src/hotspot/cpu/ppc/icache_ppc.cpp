/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2018 SAP SE. All rights reserved.
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
#include "runtime/icache.hpp"

// Use inline assembler to implement icache flush.
int ICache::ppc64_flush_icache(address start, int lines, int magic) {
  address end = start + (unsigned int)lines*ICache::line_size;
  assert(start <= end, "flush_icache parms");

  // Store modified cache lines from data cache.
  for (address a = start; a < end; a += ICache::line_size) {
    __asm__ __volatile__(
     "dcbst 0, %0  \n"
     :
     : "r" (a)
     : "memory");
  }

  // sync instruction
  __asm__ __volatile__(
     "sync \n"
     :
     :
     : "memory");

  // Invalidate respective cache lines in instruction cache.
  for (address a = start; a < end; a += ICache::line_size) {
    __asm__ __volatile__(
     "icbi 0, %0   \n"
     :
     : "r" (a)
     : "memory");
  }

  // Discard fetched instructions.
  __asm__ __volatile__(
     "isync \n"
     :
     :
     : "memory");

  return magic;
}

void ICacheStubGenerator::generate_icache_flush(ICache::flush_icache_stub_t* flush_icache_stub) {

  *flush_icache_stub = (ICache::flush_icache_stub_t)ICache::ppc64_flush_icache;

  // First call to flush itself.
  // Pointless since we call C, but it is expected to get
  // executed during VM_Version::determine_features().
  ICache::invalidate_range((address)(*flush_icache_stub), 0);
}
