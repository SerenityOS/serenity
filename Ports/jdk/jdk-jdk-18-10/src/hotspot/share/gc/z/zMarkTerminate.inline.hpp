/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZMARKTERMINATE_INLINE_HPP
#define SHARE_GC_Z_ZMARKTERMINATE_INLINE_HPP

#include "gc/z/zMarkTerminate.hpp"

#include "runtime/atomic.hpp"

inline ZMarkTerminate::ZMarkTerminate() :
    _nworkers(0),
    _nworking_stage0(0),
    _nworking_stage1(0) {}

inline bool ZMarkTerminate::enter_stage(volatile uint* nworking_stage) {
  return Atomic::sub(nworking_stage, 1u) == 0;
}

inline void ZMarkTerminate::exit_stage(volatile uint* nworking_stage) {
  Atomic::add(nworking_stage, 1u);
}

inline bool ZMarkTerminate::try_exit_stage(volatile uint* nworking_stage) {
  uint nworking = Atomic::load(nworking_stage);

  for (;;) {
    if (nworking == 0) {
      return false;
    }

    const uint new_nworking = nworking + 1;
    const uint prev_nworking = Atomic::cmpxchg(nworking_stage, nworking, new_nworking);
    if (prev_nworking == nworking) {
      // Success
      return true;
    }

    // Retry
    nworking = prev_nworking;
  }
}

inline void ZMarkTerminate::reset(uint nworkers) {
  _nworkers = _nworking_stage0 = _nworking_stage1 = nworkers;
}

inline bool ZMarkTerminate::enter_stage0() {
  return enter_stage(&_nworking_stage0);
}

inline void ZMarkTerminate::exit_stage0() {
  exit_stage(&_nworking_stage0);
}

inline bool ZMarkTerminate::try_exit_stage0() {
  return try_exit_stage(&_nworking_stage0);
}

inline bool ZMarkTerminate::enter_stage1() {
  return enter_stage(&_nworking_stage1);
}

inline bool ZMarkTerminate::try_exit_stage1() {
  return try_exit_stage(&_nworking_stage1);
}

#endif // SHARE_GC_Z_ZMARKTERMINATE_INLINE_HPP
