/*
 * Copyright (c) 2017, 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHSHAREDVARIABLES_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHSHAREDVARIABLES_HPP

#include "gc/shenandoah/shenandoahPadding.hpp"
#include "memory/allocation.hpp"
#include "runtime/atomic.hpp"

typedef jbyte ShenandoahSharedValue;

// Needed for cooperation with generated code.
STATIC_ASSERT(sizeof(ShenandoahSharedValue) == 1);

typedef struct ShenandoahSharedFlag {
  enum {
    UNSET = 0,
    SET = 1
  };

  shenandoah_padding(0);
  volatile ShenandoahSharedValue value;
  shenandoah_padding(1);

  ShenandoahSharedFlag() {
    unset();
  }

  void set() {
    Atomic::release_store_fence(&value, (ShenandoahSharedValue)SET);
  }

  void unset() {
    Atomic::release_store_fence(&value, (ShenandoahSharedValue)UNSET);
  }

  bool is_set() const {
    return Atomic::load_acquire(&value) == SET;
  }

  bool is_unset() const {
    return Atomic::load_acquire(&value) == UNSET;
  }

  void set_cond(bool val) {
    if (val) {
      set();
    } else {
      unset();
    }
  }

  bool try_set() {
    if (is_set()) {
      return false;
    }
    ShenandoahSharedValue old = Atomic::cmpxchg(&value, (ShenandoahSharedValue)UNSET, (ShenandoahSharedValue)SET);
    return old == UNSET; // success
  }

  bool try_unset() {
    if (!is_set()) {
      return false;
    }
    ShenandoahSharedValue old = Atomic::cmpxchg(&value, (ShenandoahSharedValue)SET, (ShenandoahSharedValue)UNSET);
    return old == SET; // success
  }

  volatile ShenandoahSharedValue* addr_of() {
    return &value;
  }

private:
  volatile ShenandoahSharedValue* operator&() {
    fatal("Use addr_of() instead");
    return NULL;
  }

  bool operator==(ShenandoahSharedFlag& other) { fatal("Use is_set() instead"); return false; }
  bool operator!=(ShenandoahSharedFlag& other) { fatal("Use is_set() instead"); return false; }
  bool operator> (ShenandoahSharedFlag& other) { fatal("Use is_set() instead"); return false; }
  bool operator>=(ShenandoahSharedFlag& other) { fatal("Use is_set() instead"); return false; }
  bool operator< (ShenandoahSharedFlag& other) { fatal("Use is_set() instead"); return false; }
  bool operator<=(ShenandoahSharedFlag& other) { fatal("Use is_set() instead"); return false; }

} ShenandoahSharedFlag;

typedef struct ShenandoahSharedBitmap {
  shenandoah_padding(0);
  volatile ShenandoahSharedValue value;
  shenandoah_padding(1);

  ShenandoahSharedBitmap() {
    clear();
  }

  void set(uint mask) {
    assert (mask < (sizeof(ShenandoahSharedValue) * CHAR_MAX), "sanity");
    ShenandoahSharedValue mask_val = (ShenandoahSharedValue) mask;
    while (true) {
      ShenandoahSharedValue ov = Atomic::load_acquire(&value);
      if ((ov & mask_val) != 0) {
        // already set
        return;
      }

      ShenandoahSharedValue nv = ov | mask_val;
      if (Atomic::cmpxchg(&value, ov, nv) == ov) {
        // successfully set
        return;
      }
    }
  }

  void unset(uint mask) {
    assert (mask < (sizeof(ShenandoahSharedValue) * CHAR_MAX), "sanity");
    ShenandoahSharedValue mask_val = (ShenandoahSharedValue) mask;
    while (true) {
      ShenandoahSharedValue ov = Atomic::load_acquire(&value);
      if ((ov & mask_val) == 0) {
        // already unset
        return;
      }

      ShenandoahSharedValue nv = ov & ~mask_val;
      if (Atomic::cmpxchg(&value, ov, nv) == ov) {
        // successfully unset
        return;
      }
    }
  }

  void clear() {
    Atomic::release_store_fence(&value, (ShenandoahSharedValue)0);
  }

  bool is_set(uint mask) const {
    return !is_unset(mask);
  }

  bool is_unset(uint mask) const {
    assert (mask < (sizeof(ShenandoahSharedValue) * CHAR_MAX), "sanity");
    return (Atomic::load_acquire(&value) & (ShenandoahSharedValue) mask) == 0;
  }

  bool is_clear() const {
    return (Atomic::load_acquire(&value)) == 0;
  }

  void set_cond(uint mask, bool val) {
    if (val) {
      set(mask);
    } else {
      unset(mask);
    }
  }

  volatile ShenandoahSharedValue* addr_of() {
    return &value;
  }

  ShenandoahSharedValue raw_value() const {
    return value;
  }

private:
  volatile ShenandoahSharedValue* operator&() {
    fatal("Use addr_of() instead");
    return NULL;
  }

  bool operator==(ShenandoahSharedFlag& other) { fatal("Use is_set() instead"); return false; }
  bool operator!=(ShenandoahSharedFlag& other) { fatal("Use is_set() instead"); return false; }
  bool operator> (ShenandoahSharedFlag& other) { fatal("Use is_set() instead"); return false; }
  bool operator>=(ShenandoahSharedFlag& other) { fatal("Use is_set() instead"); return false; }
  bool operator< (ShenandoahSharedFlag& other) { fatal("Use is_set() instead"); return false; }
  bool operator<=(ShenandoahSharedFlag& other) { fatal("Use is_set() instead"); return false; }

} ShenandoahSharedBitmap;

template<class T>
struct ShenandoahSharedEnumFlag {
  shenandoah_padding(0);
  volatile ShenandoahSharedValue value;
  shenandoah_padding(1);

  ShenandoahSharedEnumFlag() {
    value = 0;
  }

  void set(T v) {
    assert (v >= 0, "sanity");
    assert (v < (sizeof(ShenandoahSharedValue) * CHAR_MAX), "sanity");
    Atomic::release_store_fence(&value, (ShenandoahSharedValue)v);
  }

  T get() const {
    return (T)Atomic::load_acquire(&value);
  }

  T cmpxchg(T new_value, T expected) {
    assert (new_value >= 0, "sanity");
    assert (new_value < (sizeof(ShenandoahSharedValue) * CHAR_MAX), "sanity");
    return (T)Atomic::cmpxchg(&value, (ShenandoahSharedValue)expected, (ShenandoahSharedValue)new_value);
  }

  volatile ShenandoahSharedValue* addr_of() {
    return &value;
  }

private:
  volatile T* operator&() {
    fatal("Use addr_of() instead");
    return NULL;
  }

  bool operator==(ShenandoahSharedEnumFlag& other) { fatal("Use get() instead"); return false; }
  bool operator!=(ShenandoahSharedEnumFlag& other) { fatal("Use get() instead"); return false; }
  bool operator> (ShenandoahSharedEnumFlag& other) { fatal("Use get() instead"); return false; }
  bool operator>=(ShenandoahSharedEnumFlag& other) { fatal("Use get() instead"); return false; }
  bool operator< (ShenandoahSharedEnumFlag& other) { fatal("Use get() instead"); return false; }
  bool operator<=(ShenandoahSharedEnumFlag& other) { fatal("Use get() instead"); return false; }

};

typedef struct ShenandoahSharedSemaphore {
  shenandoah_padding(0);
  volatile ShenandoahSharedValue value;
  shenandoah_padding(1);

  static uint max_tokens() {
    return sizeof(ShenandoahSharedValue) * CHAR_MAX;
  }

  ShenandoahSharedSemaphore(uint tokens) {
    assert(tokens <= max_tokens(), "sanity");
    Atomic::release_store_fence(&value, (ShenandoahSharedValue)tokens);
  }

  bool try_acquire() {
    while (true) {
      ShenandoahSharedValue ov = Atomic::load_acquire(&value);
      if (ov == 0) {
        return false;
      }
      ShenandoahSharedValue nv = ov - 1;
      if (Atomic::cmpxchg(&value, ov, nv) == ov) {
        // successfully set
        return true;
      }
    }
  }

  void claim_all() {
    Atomic::release_store_fence(&value, (ShenandoahSharedValue)0);
  }

} ShenandoahSharedSemaphore;

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHSHAREDVARIABLES_HPP
