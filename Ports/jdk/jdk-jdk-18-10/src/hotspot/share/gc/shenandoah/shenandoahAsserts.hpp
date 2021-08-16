/*
 * Copyright (c) 2018, 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHASSERTS_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHASSERTS_HPP

#include "memory/iterator.hpp"
#include "runtime/mutex.hpp"
#include "utilities/formatBuffer.hpp"

typedef FormatBuffer<8192> ShenandoahMessageBuffer;

class ShenandoahAsserts {
public:
  enum SafeLevel {
    _safe_unknown,
    _safe_oop,
    _safe_oop_fwd,
    _safe_all
  };

  static void print_obj(ShenandoahMessageBuffer &msg, oop obj);

  static void print_non_obj(ShenandoahMessageBuffer &msg, void *loc);

  static void print_obj_safe(ShenandoahMessageBuffer &msg, void *loc);

  static void print_failure(SafeLevel level, oop obj, void *interior_loc, oop loc,
                            const char *phase, const char *label,
                            const char *file, int line);

  static void print_rp_failure(const char *label, BoolObjectClosure* actual,
                               const char *file, int line);

  static void assert_in_heap(void* interior_loc, oop obj, const char* file, int line);
  static void assert_in_heap_or_null(void* interior_loc, oop obj, const char* file, int line);
  static void assert_in_correct_region(void* interior_loc, oop obj, const char* file, int line);

  static void assert_correct(void* interior_loc, oop obj, const char* file, int line);
  static void assert_forwarded(void* interior_loc, oop obj, const char* file, int line);
  static void assert_not_forwarded(void* interior_loc, oop obj, const char* file, int line);
  static void assert_marked(void* interior_loc, oop obj, const char* file, int line);
  static void assert_marked_weak(void* interior_loc, oop obj, const char* file, int line);
  static void assert_marked_strong(void* interior_loc, oop obj, const char* file, int line);
  static void assert_in_cset(void* interior_loc, oop obj, const char* file, int line);
  static void assert_not_in_cset(void* interior_loc, oop obj, const char* file, int line);
  static void assert_not_in_cset_loc(void* interior_loc, const char* file, int line);

  static void assert_locked_or_shenandoah_safepoint(Mutex* lock, const char* file, int line);

  static void assert_heaplocked(const char* file, int line);
  static void assert_not_heaplocked(const char* file, int line);
  static void assert_heaplocked_or_safepoint(const char* file, int line);

#ifdef ASSERT
#define shenandoah_assert_in_heap(interior_loc, obj) \
                    ShenandoahAsserts::assert_in_heap(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_in_heap_or_null(interior_loc, obj) \
                    ShenandoahAsserts::assert_in_heap_or_null(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_in_correct_region(interior_loc, obj) \
                    ShenandoahAsserts::assert_in_correct_region(interior_loc, obj, __FILE__, __LINE__)

#define shenandoah_assert_correct_if(interior_loc, obj, condition) \
  if (condition)    ShenandoahAsserts::assert_correct(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_correct_except(interior_loc, obj, exception) \
  if (!(exception)) ShenandoahAsserts::assert_correct(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_correct(interior_loc, obj) \
                    ShenandoahAsserts::assert_correct(interior_loc, obj, __FILE__, __LINE__)

#define shenandoah_assert_forwarded_if(interior_loc, obj, condition) \
  if (condition)    ShenandoahAsserts::assert_forwarded(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_forwarded_except(interior_loc, obj, exception) \
  if (!(exception)) ShenandoahAsserts::assert_forwarded(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_forwarded(interior_loc, obj) \
                    ShenandoahAsserts::assert_forwarded(interior_loc, obj, __FILE__, __LINE__)

#define shenandoah_assert_not_forwarded_if(interior_loc, obj, condition) \
  if (condition)    ShenandoahAsserts::assert_not_forwarded(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_not_forwarded_except(interior_loc, obj, exception) \
  if (!(exception)) ShenandoahAsserts::assert_not_forwarded(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_not_forwarded(interior_loc, obj) \
                    ShenandoahAsserts::assert_not_forwarded(interior_loc, obj, __FILE__, __LINE__)

#define shenandoah_assert_marked_if(interior_loc, obj, condition) \
  if (condition)    ShenandoahAsserts::assert_marked(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_marked_except(interior_loc, obj, exception) \
  if (!(exception)) ShenandoahAsserts::assert_marked(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_marked(interior_loc, obj) \
                    ShenandoahAsserts::assert_marked(interior_loc, obj, __FILE__, __LINE__)

#define shenandoah_assert_marked_weak_if(interior_loc, obj, condition) \
  if (condition)    ShenandoahAsserts::assert_marked_weak(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_marked_weak_except(interior_loc, obj, exception) \
  if (!(exception)) ShenandoahAsserts::assert_marked_weak(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_marked_weak(interior_loc, obj) \
                    ShenandoahAsserts::assert_marked_weak(interior_loc, obj, __FILE__, __LINE__)

#define shenandoah_assert_marked_strong_if(interior_loc, obj, condition) \
  if (condition)    ShenandoahAsserts::assert_marked_strong(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_marked_strong_except(interior_loc, obj, exception) \
  if (!(exception)) ShenandoahAsserts::assert_marked_strong(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_marked_strong(interior_loc, obj) \
                    ShenandoahAsserts::assert_marked_strong(interior_loc, obj, __FILE__, __LINE__)

#define shenandoah_assert_in_cset_if(interior_loc, obj, condition) \
  if (condition)    ShenandoahAsserts::assert_in_cset(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_in_cset_except(interior_loc, obj, exception) \
  if (!(exception)) ShenandoahAsserts::assert_in_cset(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_in_cset(interior_loc, obj) \
                    ShenandoahAsserts::assert_in_cset(interior_loc, obj, __FILE__, __LINE__)

#define shenandoah_assert_not_in_cset_if(interior_loc, obj, condition) \
  if (condition)    ShenandoahAsserts::assert_not_in_cset(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_not_in_cset_except(interior_loc, obj, exception) \
  if (!(exception)) ShenandoahAsserts::assert_not_in_cset(interior_loc, obj, __FILE__, __LINE__)
#define shenandoah_assert_not_in_cset(interior_loc, obj) \
                    ShenandoahAsserts::assert_not_in_cset(interior_loc, obj, __FILE__, __LINE__)

#define shenandoah_assert_not_in_cset_loc_if(interior_loc, condition) \
  if (condition)    ShenandoahAsserts::assert_not_in_cset_loc(interior_loc, __FILE__, __LINE__)
#define shenandoah_assert_not_in_cset_loc_except(interior_loc, exception) \
  if (!(exception)) ShenandoahAsserts::assert_not_in_cset_loc(interior_loc, __FILE__, __LINE__)
#define shenandoah_assert_not_in_cset_loc(interior_loc) \
                    ShenandoahAsserts::assert_not_in_cset_loc(interior_loc, __FILE__, __LINE__)

#define shenandoah_assert_rp_isalive_installed() \
                    ShenandoahAsserts::assert_rp_isalive_installed(__FILE__, __LINE__)
#define shenandoah_assert_rp_isalive_not_installed() \
                    ShenandoahAsserts::assert_rp_isalive_not_installed(__FILE__, __LINE__)

#define shenandoah_assert_safepoint() \
                    assert(ShenandoahSafepoint::is_at_shenandoah_safepoint(), "Should be at Shenandoah Safepoints")

#define shenandoah_assert_locked_or_safepoint(lock) \
                    ShenandoahAsserts::assert_locked_or_shenandoah_safepoint(lock, __FILE__, __LINE__)

#define shenandoah_assert_heaplocked() \
                    ShenandoahAsserts::assert_heaplocked(__FILE__, __LINE__)

#define shenandoah_assert_not_heaplocked() \
                    ShenandoahAsserts::assert_not_heaplocked(__FILE__, __LINE__)

#define shenandoah_assert_heaplocked_or_safepoint() \
                    ShenandoahAsserts::assert_heaplocked_or_safepoint(__FILE__, __LINE__)
#else
#define shenandoah_assert_in_heap(interior_loc, obj)
#define shenandoah_assert_in_heap_or_null(interior_loc, obj)
#define shenandoah_assert_in_correct_region(interior_loc, obj)

#define shenandoah_assert_correct_if(interior_loc, obj, condition)
#define shenandoah_assert_correct_except(interior_loc, obj, exception)
#define shenandoah_assert_correct(interior_loc, obj)

#define shenandoah_assert_forwarded_if(interior_loc, obj, condition)
#define shenandoah_assert_forwarded_except(interior_loc, obj, exception)
#define shenandoah_assert_forwarded(interior_loc, obj)

#define shenandoah_assert_not_forwarded_if(interior_loc, obj, condition)
#define shenandoah_assert_not_forwarded_except(interior_loc, obj, exception)
#define shenandoah_assert_not_forwarded(interior_loc, obj)

#define shenandoah_assert_marked_if(interior_loc, obj, condition)
#define shenandoah_assert_marked_except(interior_loc, obj, exception)
#define shenandoah_assert_marked(interior_loc, obj)

#define shenandoah_assert_marked_weak_if(interior_loc, obj, condition)
#define shenandoah_assert_marked_weak_except(interior_loc, obj, exception)
#define shenandoah_assert_marked_weak(interior_loc, obj)

#define shenandoah_assert_marked_strong_if(interior_loc, obj, condition)
#define shenandoah_assert_marked_strong_except(interior_loc, obj, exception)
#define shenandoah_assert_marked_strong(interior_loc, obj)

#define shenandoah_assert_in_cset_if(interior_loc, obj, condition)
#define shenandoah_assert_in_cset_except(interior_loc, obj, exception)
#define shenandoah_assert_in_cset(interior_loc, obj)

#define shenandoah_assert_not_in_cset_if(interior_loc, obj, condition)
#define shenandoah_assert_not_in_cset_except(interior_loc, obj, exception)
#define shenandoah_assert_not_in_cset(interior_loc, obj)

#define shenandoah_assert_not_in_cset_loc_if(interior_loc, condition)
#define shenandoah_assert_not_in_cset_loc_except(interior_loc, exception)
#define shenandoah_assert_not_in_cset_loc(interior_loc)

#define shenandoah_assert_rp_isalive_installed()
#define shenandoah_assert_rp_isalive_not_installed()

#define shenandoah_assert_safepoint()
#define shenandoah_assert_locked_or_safepoint(lock)

#define shenandoah_assert_heaplocked()
#define shenandoah_assert_not_heaplocked()
#define shenandoah_assert_heaplocked_or_safepoint()

#endif

#define shenandoah_not_implemented \
                    { fatal("Deliberately not implemented."); }
#define shenandoah_not_implemented_return(v) \
                    { fatal("Deliberately not implemented."); return v; }

};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHASSERTS_HPP
