/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_ACCESSBACKEND_INLINE_HPP
#define SHARE_OOPS_ACCESSBACKEND_INLINE_HPP

#include "oops/accessBackend.hpp"

#include "oops/access.hpp"
#include "oops/arrayOop.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/atomic.hpp"
#include "runtime/orderAccess.hpp"

template <DecoratorSet decorators>
template <DecoratorSet idecorators, typename T>
inline typename EnableIf<
  AccessInternal::MustConvertCompressedOop<idecorators, T>::value, T>::type
RawAccessBarrier<decorators>::decode_internal(typename HeapOopType<idecorators>::type value) {
  if (HasDecorator<decorators, IS_NOT_NULL>::value) {
    return CompressedOops::decode_not_null(value);
  } else {
    return CompressedOops::decode(value);
  }
}

template <DecoratorSet decorators>
template <DecoratorSet idecorators, typename T>
inline typename EnableIf<
  AccessInternal::MustConvertCompressedOop<idecorators, T>::value,
  typename HeapOopType<idecorators>::type>::type
RawAccessBarrier<decorators>::encode_internal(T value) {
  if (HasDecorator<decorators, IS_NOT_NULL>::value) {
    return CompressedOops::encode_not_null(value);
  } else {
    return CompressedOops::encode(value);
  }
}

template <DecoratorSet decorators>
template <typename T>
inline void RawAccessBarrier<decorators>::oop_store(void* addr, T value) {
  typedef typename AccessInternal::EncodedType<decorators, T>::type Encoded;
  Encoded encoded = encode(value);
  store(reinterpret_cast<Encoded*>(addr), encoded);
}

template <DecoratorSet decorators>
template <typename T>
inline void RawAccessBarrier<decorators>::oop_store_at(oop base, ptrdiff_t offset, T value) {
  oop_store(field_addr(base, offset), value);
}

template <DecoratorSet decorators>
template <typename T>
inline T RawAccessBarrier<decorators>::oop_load(void* addr) {
  typedef typename AccessInternal::EncodedType<decorators, T>::type Encoded;
  Encoded encoded = load<Encoded>(reinterpret_cast<Encoded*>(addr));
  return decode<T>(encoded);
}

template <DecoratorSet decorators>
template <typename T>
inline T RawAccessBarrier<decorators>::oop_load_at(oop base, ptrdiff_t offset) {
  return oop_load<T>(field_addr(base, offset));
}

template <DecoratorSet decorators>
template <typename T>
inline T RawAccessBarrier<decorators>::oop_atomic_cmpxchg(void* addr, T compare_value, T new_value) {
  typedef typename AccessInternal::EncodedType<decorators, T>::type Encoded;
  Encoded encoded_new = encode(new_value);
  Encoded encoded_compare = encode(compare_value);
  Encoded encoded_result = atomic_cmpxchg(reinterpret_cast<Encoded*>(addr),
                                          encoded_compare,
                                          encoded_new);
  return decode<T>(encoded_result);
}

template <DecoratorSet decorators>
template <typename T>
inline T RawAccessBarrier<decorators>::oop_atomic_cmpxchg_at(oop base, ptrdiff_t offset, T compare_value, T new_value) {
  return oop_atomic_cmpxchg(field_addr(base, offset), compare_value, new_value);
}

template <DecoratorSet decorators>
template <typename T>
inline T RawAccessBarrier<decorators>::oop_atomic_xchg(void* addr, T new_value) {
  typedef typename AccessInternal::EncodedType<decorators, T>::type Encoded;
  Encoded encoded_new = encode(new_value);
  Encoded encoded_result = atomic_xchg(reinterpret_cast<Encoded*>(addr), encoded_new);
  return decode<T>(encoded_result);
}

template <DecoratorSet decorators>
template <typename T>
inline T RawAccessBarrier<decorators>::oop_atomic_xchg_at(oop base, ptrdiff_t offset, T new_value) {
  return oop_atomic_xchg(field_addr(base, offset), new_value);
}

template <DecoratorSet decorators>
template <typename T>
inline bool RawAccessBarrier<decorators>::oop_arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                                                        arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                                                        size_t length) {
  return arraycopy(src_obj, src_offset_in_bytes, src_raw,
                   dst_obj, dst_offset_in_bytes, dst_raw,
                   length);
}

template <DecoratorSet decorators>
template <DecoratorSet ds, typename T>
inline typename EnableIf<
  HasDecorator<ds, MO_SEQ_CST>::value, T>::type
RawAccessBarrier<decorators>::load_internal(void* addr) {
  if (support_IRIW_for_not_multiple_copy_atomic_cpu) {
    OrderAccess::fence();
  }
  return Atomic::load_acquire(reinterpret_cast<const volatile T*>(addr));
}

template <DecoratorSet decorators>
template <DecoratorSet ds, typename T>
inline typename EnableIf<
  HasDecorator<ds, MO_ACQUIRE>::value, T>::type
RawAccessBarrier<decorators>::load_internal(void* addr) {
  return Atomic::load_acquire(reinterpret_cast<const volatile T*>(addr));
}

template <DecoratorSet decorators>
template <DecoratorSet ds, typename T>
inline typename EnableIf<
  HasDecorator<ds, MO_RELAXED>::value, T>::type
RawAccessBarrier<decorators>::load_internal(void* addr) {
  return Atomic::load(reinterpret_cast<const volatile T*>(addr));
}

template <DecoratorSet decorators>
template <DecoratorSet ds, typename T>
inline typename EnableIf<
  HasDecorator<ds, MO_SEQ_CST>::value>::type
RawAccessBarrier<decorators>::store_internal(void* addr, T value) {
  Atomic::release_store_fence(reinterpret_cast<volatile T*>(addr), value);
}

template <DecoratorSet decorators>
template <DecoratorSet ds, typename T>
inline typename EnableIf<
  HasDecorator<ds, MO_RELEASE>::value>::type
RawAccessBarrier<decorators>::store_internal(void* addr, T value) {
  Atomic::release_store(reinterpret_cast<volatile T*>(addr), value);
}

template <DecoratorSet decorators>
template <DecoratorSet ds, typename T>
inline typename EnableIf<
  HasDecorator<ds, MO_RELAXED>::value>::type
RawAccessBarrier<decorators>::store_internal(void* addr, T value) {
  Atomic::store(reinterpret_cast<volatile T*>(addr), value);
}

template <DecoratorSet decorators>
template <DecoratorSet ds, typename T>
inline typename EnableIf<
  HasDecorator<ds, MO_RELAXED>::value, T>::type
RawAccessBarrier<decorators>::atomic_cmpxchg_internal(void* addr, T compare_value, T new_value) {
  return Atomic::cmpxchg(reinterpret_cast<volatile T*>(addr),
                         compare_value,
                         new_value,
                         memory_order_relaxed);
}

template <DecoratorSet decorators>
template <DecoratorSet ds, typename T>
inline typename EnableIf<
  HasDecorator<ds, MO_SEQ_CST>::value, T>::type
RawAccessBarrier<decorators>::atomic_cmpxchg_internal(void* addr, T compare_value, T new_value) {
  return Atomic::cmpxchg(reinterpret_cast<volatile T*>(addr),
                         compare_value,
                         new_value,
                         memory_order_conservative);
}

template <DecoratorSet decorators>
template <DecoratorSet ds, typename T>
inline typename EnableIf<
  HasDecorator<ds, MO_SEQ_CST>::value, T>::type
RawAccessBarrier<decorators>::atomic_xchg_internal(void* addr, T new_value) {
  return Atomic::xchg(reinterpret_cast<volatile T*>(addr),
                      new_value);
}

// For platforms that do not have native support for wide atomics,
// we can emulate the atomicity using a lock. So here we check
// whether that is necessary or not.

template <DecoratorSet ds>
template <DecoratorSet decorators, typename T>
inline typename EnableIf<
  AccessInternal::PossiblyLockedAccess<T>::value, T>::type
RawAccessBarrier<ds>::atomic_xchg_maybe_locked(void* addr, T new_value) {
  if (!AccessInternal::wide_atomic_needs_locking()) {
    return atomic_xchg_internal<ds>(addr, new_value);
  } else {
    AccessInternal::AccessLocker access_lock;
    volatile T* p = reinterpret_cast<volatile T*>(addr);
    T old_val = RawAccess<>::load(p);
    RawAccess<>::store(p, new_value);
    return old_val;
  }
}

template <DecoratorSet ds>
template <DecoratorSet decorators, typename T>
inline typename EnableIf<
  AccessInternal::PossiblyLockedAccess<T>::value, T>::type
RawAccessBarrier<ds>::atomic_cmpxchg_maybe_locked(void* addr, T compare_value, T new_value) {
  if (!AccessInternal::wide_atomic_needs_locking()) {
    return atomic_cmpxchg_internal<ds>(addr, compare_value, new_value);
  } else {
    AccessInternal::AccessLocker access_lock;
    volatile T* p = reinterpret_cast<volatile T*>(addr);
    T old_val = RawAccess<>::load(p);
    if (old_val == compare_value) {
      RawAccess<>::store(p, new_value);
    }
    return old_val;
  }
}

class RawAccessBarrierArrayCopy: public AllStatic {
  template<typename T> struct IsHeapWordSized: public IntegralConstant<bool, sizeof(T) == HeapWordSize> { };
public:
  template <DecoratorSet decorators, typename T>
  static inline typename EnableIf<
    HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value>::type
  arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
            arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
            size_t length) {
    src_raw = arrayOopDesc::obj_offset_to_raw(src_obj, src_offset_in_bytes, src_raw);
    dst_raw = arrayOopDesc::obj_offset_to_raw(dst_obj, dst_offset_in_bytes, dst_raw);

    // We do not check for ARRAYCOPY_ATOMIC for oops, because they are unconditionally always atomic.
    if (HasDecorator<decorators, ARRAYCOPY_ARRAYOF>::value) {
      AccessInternal::arraycopy_arrayof_conjoint_oops(src_raw, dst_raw, length);
    } else {
      typedef typename HeapOopType<decorators>::type OopType;
      AccessInternal::arraycopy_conjoint_oops(reinterpret_cast<OopType*>(src_raw),
                                              reinterpret_cast<OopType*>(dst_raw), length);
    }
  }

  template <DecoratorSet decorators, typename T>
  static inline typename EnableIf<
    !HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value &&
    HasDecorator<decorators, ARRAYCOPY_ARRAYOF>::value>::type
  arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
            arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
            size_t length) {
    src_raw = arrayOopDesc::obj_offset_to_raw(src_obj, src_offset_in_bytes, src_raw);
    dst_raw = arrayOopDesc::obj_offset_to_raw(dst_obj, dst_offset_in_bytes, dst_raw);

    AccessInternal::arraycopy_arrayof_conjoint(src_raw, dst_raw, length);
  }

  template <DecoratorSet decorators, typename T>
  static inline typename EnableIf<
    !HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value &&
    HasDecorator<decorators, ARRAYCOPY_DISJOINT>::value && IsHeapWordSized<T>::value>::type
  arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
            arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
            size_t length) {
    src_raw = arrayOopDesc::obj_offset_to_raw(src_obj, src_offset_in_bytes, src_raw);
    dst_raw = arrayOopDesc::obj_offset_to_raw(dst_obj, dst_offset_in_bytes, dst_raw);

    // There is only a disjoint optimization for word granularity copying
    if (HasDecorator<decorators, ARRAYCOPY_ATOMIC>::value) {
      AccessInternal::arraycopy_disjoint_words_atomic(src_raw, dst_raw, length);
    } else {
      AccessInternal::arraycopy_disjoint_words(src_raw, dst_raw, length);
    }
  }

  template <DecoratorSet decorators, typename T>
  static inline typename EnableIf<
    !HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value &&
    !(HasDecorator<decorators, ARRAYCOPY_DISJOINT>::value && IsHeapWordSized<T>::value) &&
    !HasDecorator<decorators, ARRAYCOPY_ARRAYOF>::value &&
    !HasDecorator<decorators, ARRAYCOPY_ATOMIC>::value>::type
  arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
            arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
            size_t length) {
    src_raw = arrayOopDesc::obj_offset_to_raw(src_obj, src_offset_in_bytes, src_raw);
    dst_raw = arrayOopDesc::obj_offset_to_raw(dst_obj, dst_offset_in_bytes, dst_raw);

    AccessInternal::arraycopy_conjoint(src_raw, dst_raw, length);
  }

  template <DecoratorSet decorators, typename T>
  static inline typename EnableIf<
    !HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value &&
    !(HasDecorator<decorators, ARRAYCOPY_DISJOINT>::value && IsHeapWordSized<T>::value) &&
    !HasDecorator<decorators, ARRAYCOPY_ARRAYOF>::value &&
    HasDecorator<decorators, ARRAYCOPY_ATOMIC>::value>::type
  arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
            arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
            size_t length) {
    src_raw = arrayOopDesc::obj_offset_to_raw(src_obj, src_offset_in_bytes, src_raw);
    dst_raw = arrayOopDesc::obj_offset_to_raw(dst_obj, dst_offset_in_bytes, dst_raw);

    AccessInternal::arraycopy_conjoint_atomic(src_raw, dst_raw, length);
  }
};

template<> struct RawAccessBarrierArrayCopy::IsHeapWordSized<void>: public IntegralConstant<bool, false> { };

template <DecoratorSet decorators>
template <typename T>
inline bool RawAccessBarrier<decorators>::arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                                                    arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                                                    size_t length) {
  RawAccessBarrierArrayCopy::arraycopy<decorators>(src_obj, src_offset_in_bytes, src_raw,
                                                   dst_obj, dst_offset_in_bytes, dst_raw,
                                                   length);
  return true;
}

template <DecoratorSet decorators>
inline void RawAccessBarrier<decorators>::clone(oop src, oop dst, size_t size) {
  // 4839641 (4840070): We must do an oop-atomic copy, because if another thread
  // is modifying a reference field in the clonee, a non-oop-atomic copy might
  // be suspended in the middle of copying the pointer and end up with parts
  // of two different pointers in the field.  Subsequent dereferences will crash.
  // 4846409: an oop-copy of objects with long or double fields or arrays of same
  // won't copy the longs/doubles atomically in 32-bit vm's, so we copy jlongs instead
  // of oops.  We know objects are aligned on a minimum of an jlong boundary.
  // The same is true of StubRoutines::object_copy and the various oop_copy
  // variants, and of the code generated by the inline_native_clone intrinsic.

  assert(MinObjAlignmentInBytes >= BytesPerLong, "objects misaligned");
  AccessInternal::arraycopy_conjoint_atomic(reinterpret_cast<jlong*>((oopDesc*)src),
                                            reinterpret_cast<jlong*>((oopDesc*)dst),
                                            align_object_size(size) / HeapWordsPerLong);
  // Clear the header
  dst->init_mark();
}

#endif // SHARE_OOPS_ACCESSBACKEND_INLINE_HPP
