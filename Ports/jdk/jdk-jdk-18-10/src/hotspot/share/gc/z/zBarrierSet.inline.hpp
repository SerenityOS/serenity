/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZBARRIERSET_INLINE_HPP
#define SHARE_GC_Z_ZBARRIERSET_INLINE_HPP

#include "gc/z/zBarrierSet.hpp"

#include "gc/shared/accessBarrierSupport.inline.hpp"
#include "gc/z/zBarrier.inline.hpp"
#include "utilities/debug.hpp"

template <DecoratorSet decorators, typename BarrierSetT>
template <DecoratorSet expected>
inline void ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::verify_decorators_present() {
  if ((decorators & expected) == 0) {
    fatal("Using unsupported access decorators");
  }
}

template <DecoratorSet decorators, typename BarrierSetT>
template <DecoratorSet expected>
inline void ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::verify_decorators_absent() {
  if ((decorators & expected) != 0) {
    fatal("Using unsupported access decorators");
  }
}

template <DecoratorSet decorators, typename BarrierSetT>
inline oop* ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::field_addr(oop base, ptrdiff_t offset) {
  assert(base != NULL, "Invalid base");
  return reinterpret_cast<oop*>(reinterpret_cast<intptr_t>((void*)base) + offset);
}

template <DecoratorSet decorators, typename BarrierSetT>
template <typename T>
inline oop ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::load_barrier_on_oop_field_preloaded(T* addr, oop o) {
  verify_decorators_absent<ON_UNKNOWN_OOP_REF>();

  if (HasDecorator<decorators, AS_NO_KEEPALIVE>::value) {
    if (HasDecorator<decorators, ON_STRONG_OOP_REF>::value) {
      return ZBarrier::weak_load_barrier_on_oop_field_preloaded(addr, o);
    } else if (HasDecorator<decorators, ON_WEAK_OOP_REF>::value) {
      return ZBarrier::weak_load_barrier_on_weak_oop_field_preloaded(addr, o);
    } else {
      assert((HasDecorator<decorators, ON_PHANTOM_OOP_REF>::value), "Must be");
      return ZBarrier::weak_load_barrier_on_phantom_oop_field_preloaded(addr, o);
    }
  } else {
    if (HasDecorator<decorators, ON_STRONG_OOP_REF>::value) {
      return ZBarrier::load_barrier_on_oop_field_preloaded(addr, o);
    } else if (HasDecorator<decorators, ON_WEAK_OOP_REF>::value) {
      return ZBarrier::load_barrier_on_weak_oop_field_preloaded(addr, o);
    } else {
      assert((HasDecorator<decorators, ON_PHANTOM_OOP_REF>::value), "Must be");
      return ZBarrier::load_barrier_on_phantom_oop_field_preloaded(addr, o);
    }
  }
}

template <DecoratorSet decorators, typename BarrierSetT>
template <typename T>
inline oop ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::load_barrier_on_unknown_oop_field_preloaded(oop base, ptrdiff_t offset, T* addr, oop o) {
  verify_decorators_present<ON_UNKNOWN_OOP_REF>();

  const DecoratorSet decorators_known_strength =
    AccessBarrierSupport::resolve_possibly_unknown_oop_ref_strength<decorators>(base, offset);

  if (HasDecorator<decorators, AS_NO_KEEPALIVE>::value) {
    if (decorators_known_strength & ON_STRONG_OOP_REF) {
      return ZBarrier::weak_load_barrier_on_oop_field_preloaded(addr, o);
    } else if (decorators_known_strength & ON_WEAK_OOP_REF) {
      return ZBarrier::weak_load_barrier_on_weak_oop_field_preloaded(addr, o);
    } else {
      assert(decorators_known_strength & ON_PHANTOM_OOP_REF, "Must be");
      return ZBarrier::weak_load_barrier_on_phantom_oop_field_preloaded(addr, o);
    }
  } else {
    if (decorators_known_strength & ON_STRONG_OOP_REF) {
      return ZBarrier::load_barrier_on_oop_field_preloaded(addr, o);
    } else if (decorators_known_strength & ON_WEAK_OOP_REF) {
      return ZBarrier::load_barrier_on_weak_oop_field_preloaded(addr, o);
    } else {
      assert(decorators_known_strength & ON_PHANTOM_OOP_REF, "Must be");
      return ZBarrier::load_barrier_on_phantom_oop_field_preloaded(addr, o);
    }
  }
}

//
// In heap
//
template <DecoratorSet decorators, typename BarrierSetT>
template <typename T>
inline oop ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::oop_load_in_heap(T* addr) {
  verify_decorators_absent<ON_UNKNOWN_OOP_REF>();

  const oop o = Raw::oop_load_in_heap(addr);
  return load_barrier_on_oop_field_preloaded(addr, o);
}

template <DecoratorSet decorators, typename BarrierSetT>
inline oop ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::oop_load_in_heap_at(oop base, ptrdiff_t offset) {
  oop* const addr = field_addr(base, offset);
  const oop o = Raw::oop_load_in_heap(addr);

  if (HasDecorator<decorators, ON_UNKNOWN_OOP_REF>::value) {
    return load_barrier_on_unknown_oop_field_preloaded(base, offset, addr, o);
  }

  return load_barrier_on_oop_field_preloaded(addr, o);
}

template <DecoratorSet decorators, typename BarrierSetT>
template <typename T>
inline oop ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::oop_atomic_cmpxchg_in_heap(T* addr, oop compare_value, oop new_value) {
  verify_decorators_present<ON_STRONG_OOP_REF>();
  verify_decorators_absent<AS_NO_KEEPALIVE>();

  ZBarrier::load_barrier_on_oop_field(addr);
  return Raw::oop_atomic_cmpxchg_in_heap(addr, compare_value, new_value);
}

template <DecoratorSet decorators, typename BarrierSetT>
inline oop ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::oop_atomic_cmpxchg_in_heap_at(oop base, ptrdiff_t offset, oop compare_value, oop new_value) {
  verify_decorators_present<ON_STRONG_OOP_REF | ON_UNKNOWN_OOP_REF>();
  verify_decorators_absent<AS_NO_KEEPALIVE>();

  // Through Unsafe.CompareAndExchangeObject()/CompareAndSetObject() we can receive
  // calls with ON_UNKNOWN_OOP_REF set. However, we treat these as ON_STRONG_OOP_REF,
  // with the motivation that if you're doing Unsafe operations on a Reference.referent
  // field, then you're on your own anyway.
  ZBarrier::load_barrier_on_oop_field(field_addr(base, offset));
  return Raw::oop_atomic_cmpxchg_in_heap_at(base, offset, compare_value, new_value);
}

template <DecoratorSet decorators, typename BarrierSetT>
template <typename T>
inline oop ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::oop_atomic_xchg_in_heap(T* addr, oop new_value) {
  verify_decorators_present<ON_STRONG_OOP_REF>();
  verify_decorators_absent<AS_NO_KEEPALIVE>();

  const oop o = Raw::oop_atomic_xchg_in_heap(addr, new_value);
  return ZBarrier::load_barrier_on_oop(o);
}

template <DecoratorSet decorators, typename BarrierSetT>
inline oop ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::oop_atomic_xchg_in_heap_at(oop base, ptrdiff_t offset, oop new_value) {
  verify_decorators_present<ON_STRONG_OOP_REF>();
  verify_decorators_absent<AS_NO_KEEPALIVE>();

  const oop o = Raw::oop_atomic_xchg_in_heap_at(base, offset, new_value);
  return ZBarrier::load_barrier_on_oop(o);
}

template <DecoratorSet decorators, typename BarrierSetT>
template <typename T>
inline bool ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::oop_arraycopy_in_heap(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                                                                                       arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                                                                                       size_t length) {
  T* src = arrayOopDesc::obj_offset_to_raw(src_obj, src_offset_in_bytes, src_raw);
  T* dst = arrayOopDesc::obj_offset_to_raw(dst_obj, dst_offset_in_bytes, dst_raw);

  if (!HasDecorator<decorators, ARRAYCOPY_CHECKCAST>::value) {
    // No check cast, bulk barrier and bulk copy
    ZBarrier::load_barrier_on_oop_array(src, length);
    return Raw::oop_arraycopy_in_heap(NULL, 0, src, NULL, 0, dst, length);
  }

  // Check cast and copy each elements
  Klass* const dst_klass = objArrayOop(dst_obj)->element_klass();
  for (const T* const end = src + length; src < end; src++, dst++) {
    const oop elem = ZBarrier::load_barrier_on_oop_field(src);
    if (!oopDesc::is_instanceof_or_null(elem, dst_klass)) {
      // Check cast failed
      return false;
    }

    // Cast is safe, since we know it's never a narrowOop
    *(oop*)dst = elem;
  }

  return true;
}

template <DecoratorSet decorators, typename BarrierSetT>
inline void ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::clone_in_heap(oop src, oop dst, size_t size) {
  ZBarrier::load_barrier_on_oop_fields(src);
  Raw::clone_in_heap(src, dst, size);
}

//
// Not in heap
//
template <DecoratorSet decorators, typename BarrierSetT>
template <typename T>
inline oop ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::oop_load_not_in_heap(T* addr) {
  verify_decorators_absent<ON_UNKNOWN_OOP_REF>();

  const oop o = Raw::oop_load_not_in_heap(addr);
  return load_barrier_on_oop_field_preloaded(addr, o);
}

template <DecoratorSet decorators, typename BarrierSetT>
template <typename T>
inline oop ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::oop_atomic_cmpxchg_not_in_heap(T* addr, oop compare_value, oop new_value) {
  verify_decorators_present<ON_STRONG_OOP_REF>();
  verify_decorators_absent<AS_NO_KEEPALIVE>();

  return Raw::oop_atomic_cmpxchg_not_in_heap(addr, compare_value, new_value);
}

template <DecoratorSet decorators, typename BarrierSetT>
template <typename T>
inline oop ZBarrierSet::AccessBarrier<decorators, BarrierSetT>::oop_atomic_xchg_not_in_heap(T* addr, oop new_value) {
  verify_decorators_present<ON_STRONG_OOP_REF>();
  verify_decorators_absent<AS_NO_KEEPALIVE>();

  return Raw::oop_atomic_xchg_not_in_heap(addr, new_value);
}

#endif // SHARE_GC_Z_ZBARRIERSET_INLINE_HPP
