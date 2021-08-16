/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016 SAP SE. All rights reserved.
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

#include "runtime/icache.hpp"

// interface (see ICache::flush_icache_stub_t):
//   address   addr   (Z_R2, ignored)
//   int       lines  (Z_R3, ignored)
//   int       magic  (Z_R4)
//
//   returns: int (Z_R2)
//
//   Note: z/Architecture doesn't need explicit flushing, so this is implemented as a nop.

// Call c function (which just does nothing).
int z_flush_icache(address start, int lines, int magic) { return magic; }

void ICacheStubGenerator::generate_icache_flush(ICache::flush_icache_stub_t* flush_icache_stub) {
  *flush_icache_stub = (ICache::flush_icache_stub_t)z_flush_icache;

  // First call to flush itself.
  ICache::invalidate_range((address)(*flush_icache_stub), 0);
};

