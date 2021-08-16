/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_BARRIERSETCONFIG_HPP
#define SHARE_GC_SHARED_BARRIERSETCONFIG_HPP

#include "utilities/macros.hpp"

// Do something for each concrete barrier set part of the build.
#define FOR_EACH_CONCRETE_BARRIER_SET_DO(f)          \
  f(CardTableBarrierSet)                             \
  EPSILONGC_ONLY(f(EpsilonBarrierSet))               \
  G1GC_ONLY(f(G1BarrierSet))                         \
  SHENANDOAHGC_ONLY(f(ShenandoahBarrierSet))         \
  ZGC_ONLY(f(ZBarrierSet))

#define FOR_EACH_ABSTRACT_BARRIER_SET_DO(f)          \
  f(ModRef)

// Do something for each known barrier set.
#define FOR_EACH_BARRIER_SET_DO(f)    \
  FOR_EACH_ABSTRACT_BARRIER_SET_DO(f) \
  FOR_EACH_CONCRETE_BARRIER_SET_DO(f)

#endif // SHARE_GC_SHARED_BARRIERSETCONFIG_HPP
