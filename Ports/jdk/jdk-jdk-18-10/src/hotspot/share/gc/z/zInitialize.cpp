/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zAddress.hpp"
#include "gc/z/zBarrierSet.hpp"
#include "gc/z/zCPU.hpp"
#include "gc/z/zGlobals.hpp"
#include "gc/z/zHeuristics.hpp"
#include "gc/z/zInitialize.hpp"
#include "gc/z/zLargePages.hpp"
#include "gc/z/zNUMA.hpp"
#include "gc/z/zStat.hpp"
#include "gc/z/zThreadLocalAllocBuffer.hpp"
#include "gc/z/zTracer.hpp"
#include "logging/log.hpp"
#include "runtime/vm_version.hpp"

ZInitialize::ZInitialize(ZBarrierSet* barrier_set) {
  log_info(gc, init)("Initializing %s", ZName);
  log_info(gc, init)("Version: %s (%s)",
                     VM_Version::vm_release(),
                     VM_Version::jdk_debug_level());

  // Early initialization
  ZAddress::initialize();
  ZNUMA::initialize();
  ZCPU::initialize();
  ZStatValue::initialize();
  ZThreadLocalAllocBuffer::initialize();
  ZTracer::initialize();
  ZLargePages::initialize();
  ZHeuristics::set_medium_page_size();
  ZBarrierSet::set_barrier_set(barrier_set);

  pd_initialize();
}
