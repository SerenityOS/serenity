/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/parallel/parallelInitLogger.hpp"
#include "gc/shared/genArguments.hpp"
#include "gc/shared/gcLogPrecious.hpp"

void ParallelInitLogger::print_heap() {
  log_info_p(gc, init)("Alignments:"
                       " Space " SIZE_FORMAT "%s,"
                       " Generation " SIZE_FORMAT "%s,"
                       " Heap " SIZE_FORMAT "%s",
                       byte_size_in_exact_unit(SpaceAlignment), exact_unit_for_byte_size(SpaceAlignment),
                       byte_size_in_exact_unit(GenAlignment), exact_unit_for_byte_size(GenAlignment),
                       byte_size_in_exact_unit(HeapAlignment), exact_unit_for_byte_size(HeapAlignment));
  GCInitLogger::print_heap();
}

void ParallelInitLogger::print() {
  ParallelInitLogger init_log;
  init_log.print_all();
}
