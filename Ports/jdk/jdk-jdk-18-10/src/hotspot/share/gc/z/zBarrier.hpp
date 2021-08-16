/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZBARRIER_HPP
#define SHARE_GC_Z_ZBARRIER_HPP

#include "memory/allocation.hpp"
#include "memory/iterator.hpp"
#include "oops/oop.hpp"

typedef bool (*ZBarrierFastPath)(uintptr_t);
typedef uintptr_t (*ZBarrierSlowPath)(uintptr_t);

class ZBarrier : public AllStatic {
private:
  static const bool GCThread    = true;
  static const bool AnyThread   = false;

  static const bool Follow      = true;
  static const bool DontFollow  = false;

  static const bool Strong      = false;
  static const bool Finalizable = true;

  static const bool Publish     = true;
  static const bool Overflow    = false;

  template <ZBarrierFastPath fast_path> static void self_heal(volatile oop* p, uintptr_t addr, uintptr_t heal_addr);

  template <ZBarrierFastPath fast_path, ZBarrierSlowPath slow_path> static oop barrier(volatile oop* p, oop o);
  template <ZBarrierFastPath fast_path, ZBarrierSlowPath slow_path> static oop weak_barrier(volatile oop* p, oop o);
  template <ZBarrierFastPath fast_path, ZBarrierSlowPath slow_path> static void root_barrier(oop* p, oop o);

  static bool is_good_or_null_fast_path(uintptr_t addr);
  static bool is_weak_good_or_null_fast_path(uintptr_t addr);
  static bool is_marked_or_null_fast_path(uintptr_t addr);

  static bool during_mark();
  static bool during_relocate();
  template <bool finalizable> static bool should_mark_through(uintptr_t addr);
  template <bool gc_thread, bool follow, bool finalizable, bool publish> static uintptr_t mark(uintptr_t addr);
  static uintptr_t remap(uintptr_t addr);
  static uintptr_t relocate(uintptr_t addr);
  static uintptr_t relocate_or_mark(uintptr_t addr);
  static uintptr_t relocate_or_mark_no_follow(uintptr_t addr);
  static uintptr_t relocate_or_remap(uintptr_t addr);

  static uintptr_t load_barrier_on_oop_slow_path(uintptr_t addr);
  static uintptr_t load_barrier_on_invisible_root_oop_slow_path(uintptr_t addr);

  static uintptr_t weak_load_barrier_on_oop_slow_path(uintptr_t addr);
  static uintptr_t weak_load_barrier_on_weak_oop_slow_path(uintptr_t addr);
  static uintptr_t weak_load_barrier_on_phantom_oop_slow_path(uintptr_t addr);

  static uintptr_t keep_alive_barrier_on_oop_slow_path(uintptr_t addr);
  static uintptr_t keep_alive_barrier_on_weak_oop_slow_path(uintptr_t addr);
  static uintptr_t keep_alive_barrier_on_phantom_oop_slow_path(uintptr_t addr);

  static uintptr_t mark_barrier_on_oop_slow_path(uintptr_t addr);
  static uintptr_t mark_barrier_on_finalizable_oop_slow_path(uintptr_t addr);

  static void verify_on_weak(volatile oop* referent_addr) NOT_DEBUG_RETURN;

public:
  // Load barrier
  static  oop load_barrier_on_oop(oop o);
  static  oop load_barrier_on_oop_field(volatile oop* p);
  static  oop load_barrier_on_oop_field_preloaded(volatile oop* p, oop o);
  static void load_barrier_on_oop_array(volatile oop* p, size_t length);
  static void load_barrier_on_oop_fields(oop o);
  static  oop load_barrier_on_weak_oop_field_preloaded(volatile oop* p, oop o);
  static  oop load_barrier_on_phantom_oop_field_preloaded(volatile oop* p, oop o);
  static void load_barrier_on_root_oop_field(oop* p);
  static void load_barrier_on_invisible_root_oop_field(oop* p);

  // Weak load barrier
  static oop weak_load_barrier_on_oop_field(volatile oop* p);
  static oop weak_load_barrier_on_oop_field_preloaded(volatile oop* p, oop o);
  static oop weak_load_barrier_on_weak_oop(oop o);
  static oop weak_load_barrier_on_weak_oop_field_preloaded(volatile oop* p, oop o);
  static oop weak_load_barrier_on_phantom_oop(oop o);
  static oop weak_load_barrier_on_phantom_oop_field_preloaded(volatile oop* p, oop o);

  // Is alive barrier
  static bool is_alive_barrier_on_weak_oop(oop o);
  static bool is_alive_barrier_on_phantom_oop(oop o);

  // Keep alive barrier
  static void keep_alive_barrier_on_oop(oop o);
  static void keep_alive_barrier_on_weak_oop_field(volatile oop* p);
  static void keep_alive_barrier_on_phantom_oop_field(volatile oop* p);
  static void keep_alive_barrier_on_phantom_root_oop_field(oop* p);

  // Mark barrier
  static void mark_barrier_on_oop_field(volatile oop* p, bool finalizable);
  static void mark_barrier_on_oop_array(volatile oop* p, size_t length, bool finalizable);

  // Narrow oop variants, never used.
  static oop  load_barrier_on_oop_field(volatile narrowOop* p);
  static oop  load_barrier_on_oop_field_preloaded(volatile narrowOop* p, oop o);
  static void load_barrier_on_oop_array(volatile narrowOop* p, size_t length);
  static oop  load_barrier_on_weak_oop_field_preloaded(volatile narrowOop* p, oop o);
  static oop  load_barrier_on_phantom_oop_field_preloaded(volatile narrowOop* p, oop o);
  static oop  weak_load_barrier_on_oop_field_preloaded(volatile narrowOop* p, oop o);
  static oop  weak_load_barrier_on_weak_oop_field_preloaded(volatile narrowOop* p, oop o);
  static oop  weak_load_barrier_on_phantom_oop_field_preloaded(volatile narrowOop* p, oop o);
};

class ZLoadBarrierOopClosure : public BasicOopIterateClosure {
public:
  virtual void do_oop(oop* p);
  virtual void do_oop(narrowOop* p);
};

#endif // SHARE_GC_Z_ZBARRIER_HPP
