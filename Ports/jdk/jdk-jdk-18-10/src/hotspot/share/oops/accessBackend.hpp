/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_ACCESSBACKEND_HPP
#define SHARE_OOPS_ACCESSBACKEND_HPP

#include "gc/shared/barrierSetConfig.hpp"
#include "memory/allocation.hpp"
#include "metaprogramming/conditional.hpp"
#include "metaprogramming/decay.hpp"
#include "metaprogramming/enableIf.hpp"
#include "metaprogramming/integralConstant.hpp"
#include "metaprogramming/isFloatingPoint.hpp"
#include "metaprogramming/isIntegral.hpp"
#include "metaprogramming/isPointer.hpp"
#include "metaprogramming/isSame.hpp"
#include "metaprogramming/isVolatile.hpp"
#include "oops/accessDecorators.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/globals.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"


// This metafunction returns either oop or narrowOop depending on whether
// an access needs to use compressed oops or not.
template <DecoratorSet decorators>
struct HeapOopType: AllStatic {
  static const bool needs_oop_compress = HasDecorator<decorators, INTERNAL_CONVERT_COMPRESSED_OOP>::value &&
                                         HasDecorator<decorators, INTERNAL_RT_USE_COMPRESSED_OOPS>::value;
  typedef typename Conditional<needs_oop_compress, narrowOop, oop>::type type;
};

namespace AccessInternal {
  enum BarrierType {
    BARRIER_STORE,
    BARRIER_STORE_AT,
    BARRIER_LOAD,
    BARRIER_LOAD_AT,
    BARRIER_ATOMIC_CMPXCHG,
    BARRIER_ATOMIC_CMPXCHG_AT,
    BARRIER_ATOMIC_XCHG,
    BARRIER_ATOMIC_XCHG_AT,
    BARRIER_ARRAYCOPY,
    BARRIER_CLONE,
    BARRIER_RESOLVE
  };

  template <DecoratorSet decorators, typename T>
  struct MustConvertCompressedOop: public IntegralConstant<bool,
    HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value &&
    IsSame<typename HeapOopType<decorators>::type, narrowOop>::value &&
    IsSame<T, oop>::value> {};

  // This metafunction returns an appropriate oop type if the value is oop-like
  // and otherwise returns the same type T.
  template <DecoratorSet decorators, typename T>
  struct EncodedType: AllStatic {
    typedef typename Conditional<
      HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value,
      typename HeapOopType<decorators>::type, T>::type type;
  };

  template <DecoratorSet decorators>
  inline typename HeapOopType<decorators>::type*
  oop_field_addr(oop base, ptrdiff_t byte_offset) {
    return reinterpret_cast<typename HeapOopType<decorators>::type*>(
             reinterpret_cast<intptr_t>((void*)base) + byte_offset);
  }

  // This metafunction returns whether it is possible for a type T to require
  // locking to support wide atomics or not.
  template <typename T>
#ifdef SUPPORTS_NATIVE_CX8
  struct PossiblyLockedAccess: public IntegralConstant<bool, false> {};
#else
  struct PossiblyLockedAccess: public IntegralConstant<bool, (sizeof(T) > 4)> {};
#endif

  template <DecoratorSet decorators, typename T>
  struct AccessFunctionTypes {
    typedef T (*load_at_func_t)(oop base, ptrdiff_t offset);
    typedef void (*store_at_func_t)(oop base, ptrdiff_t offset, T value);
    typedef T (*atomic_cmpxchg_at_func_t)(oop base, ptrdiff_t offset, T compare_value, T new_value);
    typedef T (*atomic_xchg_at_func_t)(oop base, ptrdiff_t offset, T new_value);

    typedef T (*load_func_t)(void* addr);
    typedef void (*store_func_t)(void* addr, T value);
    typedef T (*atomic_cmpxchg_func_t)(void* addr, T compare_value, T new_value);
    typedef T (*atomic_xchg_func_t)(void* addr, T new_value);

    typedef bool (*arraycopy_func_t)(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                                     arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                                     size_t length);
    typedef void (*clone_func_t)(oop src, oop dst, size_t size);
    typedef oop (*resolve_func_t)(oop obj);
  };

  template <DecoratorSet decorators>
  struct AccessFunctionTypes<decorators, void> {
    typedef bool (*arraycopy_func_t)(arrayOop src_obj, size_t src_offset_in_bytes, void* src,
                                     arrayOop dst_obj, size_t dst_offset_in_bytes, void* dst,
                                     size_t length);
  };

  template <DecoratorSet decorators, typename T, BarrierType barrier> struct AccessFunction {};

#define ACCESS_GENERATE_ACCESS_FUNCTION(bt, func)                   \
  template <DecoratorSet decorators, typename T>                    \
  struct AccessFunction<decorators, T, bt>: AllStatic{              \
    typedef typename AccessFunctionTypes<decorators, T>::func type; \
  }
  ACCESS_GENERATE_ACCESS_FUNCTION(BARRIER_STORE, store_func_t);
  ACCESS_GENERATE_ACCESS_FUNCTION(BARRIER_STORE_AT, store_at_func_t);
  ACCESS_GENERATE_ACCESS_FUNCTION(BARRIER_LOAD, load_func_t);
  ACCESS_GENERATE_ACCESS_FUNCTION(BARRIER_LOAD_AT, load_at_func_t);
  ACCESS_GENERATE_ACCESS_FUNCTION(BARRIER_ATOMIC_CMPXCHG, atomic_cmpxchg_func_t);
  ACCESS_GENERATE_ACCESS_FUNCTION(BARRIER_ATOMIC_CMPXCHG_AT, atomic_cmpxchg_at_func_t);
  ACCESS_GENERATE_ACCESS_FUNCTION(BARRIER_ATOMIC_XCHG, atomic_xchg_func_t);
  ACCESS_GENERATE_ACCESS_FUNCTION(BARRIER_ATOMIC_XCHG_AT, atomic_xchg_at_func_t);
  ACCESS_GENERATE_ACCESS_FUNCTION(BARRIER_ARRAYCOPY, arraycopy_func_t);
  ACCESS_GENERATE_ACCESS_FUNCTION(BARRIER_CLONE, clone_func_t);
  ACCESS_GENERATE_ACCESS_FUNCTION(BARRIER_RESOLVE, resolve_func_t);
#undef ACCESS_GENERATE_ACCESS_FUNCTION

  template <DecoratorSet decorators, typename T, BarrierType barrier_type>
  typename AccessFunction<decorators, T, barrier_type>::type resolve_barrier();

  template <DecoratorSet decorators, typename T, BarrierType barrier_type>
  typename AccessFunction<decorators, T, barrier_type>::type resolve_oop_barrier();

  class AccessLocker {
  public:
    AccessLocker();
    ~AccessLocker();
  };
  bool wide_atomic_needs_locking();

  void* field_addr(oop base, ptrdiff_t offset);

  // Forward calls to Copy:: in the cpp file to reduce dependencies and allow
  // faster build times, given how frequently included access is.
  void arraycopy_arrayof_conjoint_oops(void* src, void* dst, size_t length);
  void arraycopy_conjoint_oops(oop* src, oop* dst, size_t length);
  void arraycopy_conjoint_oops(narrowOop* src, narrowOop* dst, size_t length);

  void arraycopy_disjoint_words(void* src, void* dst, size_t length);
  void arraycopy_disjoint_words_atomic(void* src, void* dst, size_t length);

  template<typename T>
  void arraycopy_conjoint(T* src, T* dst, size_t length);
  template<typename T>
  void arraycopy_arrayof_conjoint(T* src, T* dst, size_t length);
  template<typename T>
  void arraycopy_conjoint_atomic(T* src, T* dst, size_t length);
}

// This mask specifies what decorators are relevant for raw accesses. When passing
// accesses to the raw layer, irrelevant decorators are removed.
const DecoratorSet RAW_DECORATOR_MASK = INTERNAL_DECORATOR_MASK | MO_DECORATOR_MASK |
                                        ARRAYCOPY_DECORATOR_MASK | IS_NOT_NULL;

// The RawAccessBarrier performs raw accesses with additional knowledge of
// memory ordering, so that OrderAccess/Atomic is called when necessary.
// It additionally handles compressed oops, and hence is not completely "raw"
// strictly speaking.
template <DecoratorSet decorators>
class RawAccessBarrier: public AllStatic {
protected:
  static inline void* field_addr(oop base, ptrdiff_t byte_offset) {
    return AccessInternal::field_addr(base, byte_offset);
  }

protected:
  // Only encode if INTERNAL_VALUE_IS_OOP
  template <DecoratorSet idecorators, typename T>
  static inline typename EnableIf<
    AccessInternal::MustConvertCompressedOop<idecorators, T>::value,
    typename HeapOopType<idecorators>::type>::type
  encode_internal(T value);

  template <DecoratorSet idecorators, typename T>
  static inline typename EnableIf<
    !AccessInternal::MustConvertCompressedOop<idecorators, T>::value, T>::type
  encode_internal(T value) {
    return value;
  }

  template <typename T>
  static inline typename AccessInternal::EncodedType<decorators, T>::type
  encode(T value) {
    return encode_internal<decorators, T>(value);
  }

  // Only decode if INTERNAL_VALUE_IS_OOP
  template <DecoratorSet idecorators, typename T>
  static inline typename EnableIf<
    AccessInternal::MustConvertCompressedOop<idecorators, T>::value, T>::type
  decode_internal(typename HeapOopType<idecorators>::type value);

  template <DecoratorSet idecorators, typename T>
  static inline typename EnableIf<
    !AccessInternal::MustConvertCompressedOop<idecorators, T>::value, T>::type
  decode_internal(T value) {
    return value;
  }

  template <typename T>
  static inline T decode(typename AccessInternal::EncodedType<decorators, T>::type value) {
    return decode_internal<decorators, T>(value);
  }

protected:
  template <DecoratorSet ds, typename T>
  static typename EnableIf<
    HasDecorator<ds, MO_SEQ_CST>::value, T>::type
  load_internal(void* addr);

  template <DecoratorSet ds, typename T>
  static typename EnableIf<
    HasDecorator<ds, MO_ACQUIRE>::value, T>::type
  load_internal(void* addr);

  template <DecoratorSet ds, typename T>
  static typename EnableIf<
    HasDecorator<ds, MO_RELAXED>::value, T>::type
  load_internal(void* addr);

  template <DecoratorSet ds, typename T>
  static inline typename EnableIf<
    HasDecorator<ds, MO_UNORDERED>::value, T>::type
  load_internal(void* addr) {
    return *reinterpret_cast<T*>(addr);
  }

  template <DecoratorSet ds, typename T>
  static typename EnableIf<
    HasDecorator<ds, MO_SEQ_CST>::value>::type
  store_internal(void* addr, T value);

  template <DecoratorSet ds, typename T>
  static typename EnableIf<
    HasDecorator<ds, MO_RELEASE>::value>::type
  store_internal(void* addr, T value);

  template <DecoratorSet ds, typename T>
  static typename EnableIf<
    HasDecorator<ds, MO_RELAXED>::value>::type
  store_internal(void* addr, T value);

  template <DecoratorSet ds, typename T>
  static inline typename EnableIf<
    HasDecorator<ds, MO_UNORDERED>::value>::type
  store_internal(void* addr, T value) {
    *reinterpret_cast<T*>(addr) = value;
  }

  template <DecoratorSet ds, typename T>
  static typename EnableIf<
    HasDecorator<ds, MO_SEQ_CST>::value, T>::type
  atomic_cmpxchg_internal(void* addr, T compare_value, T new_value);

  template <DecoratorSet ds, typename T>
  static typename EnableIf<
    HasDecorator<ds, MO_RELAXED>::value, T>::type
  atomic_cmpxchg_internal(void* addr, T compare_value, T new_value);

  template <DecoratorSet ds, typename T>
  static typename EnableIf<
    HasDecorator<ds, MO_SEQ_CST>::value, T>::type
  atomic_xchg_internal(void* addr, T new_value);

  // The following *_locked mechanisms serve the purpose of handling atomic operations
  // that are larger than a machine can handle, and then possibly opt for using
  // a slower path using a mutex to perform the operation.

  template <DecoratorSet ds, typename T>
  static inline typename EnableIf<
    !AccessInternal::PossiblyLockedAccess<T>::value, T>::type
  atomic_cmpxchg_maybe_locked(void* addr, T compare_value, T new_value) {
    return atomic_cmpxchg_internal<ds>(addr, compare_value, new_value);
  }

  template <DecoratorSet ds, typename T>
  static typename EnableIf<
    AccessInternal::PossiblyLockedAccess<T>::value, T>::type
  atomic_cmpxchg_maybe_locked(void* addr, T compare_value, T new_value);

  template <DecoratorSet ds, typename T>
  static inline typename EnableIf<
    !AccessInternal::PossiblyLockedAccess<T>::value, T>::type
  atomic_xchg_maybe_locked(void* addr, T new_value) {
    return atomic_xchg_internal<ds>(addr, new_value);
  }

  template <DecoratorSet ds, typename T>
  static typename EnableIf<
    AccessInternal::PossiblyLockedAccess<T>::value, T>::type
  atomic_xchg_maybe_locked(void* addr, T new_value);

public:
  template <typename T>
  static inline void store(void* addr, T value) {
    store_internal<decorators>(addr, value);
  }

  template <typename T>
  static inline T load(void* addr) {
    return load_internal<decorators, T>(addr);
  }

  template <typename T>
  static inline T atomic_cmpxchg(void* addr, T compare_value, T new_value) {
    return atomic_cmpxchg_maybe_locked<decorators>(addr, compare_value, new_value);
  }

  template <typename T>
  static inline T atomic_xchg(void* addr, T new_value) {
    return atomic_xchg_maybe_locked<decorators>(addr, new_value);
  }

  template <typename T>
  static bool arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                        arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                        size_t length);

  template <typename T>
  static void oop_store(void* addr, T value);
  template <typename T>
  static void oop_store_at(oop base, ptrdiff_t offset, T value);

  template <typename T>
  static T oop_load(void* addr);
  template <typename T>
  static T oop_load_at(oop base, ptrdiff_t offset);

  template <typename T>
  static T oop_atomic_cmpxchg(void* addr, T compare_value, T new_value);
  template <typename T>
  static T oop_atomic_cmpxchg_at(oop base, ptrdiff_t offset, T compare_value, T new_value);

  template <typename T>
  static T oop_atomic_xchg(void* addr, T new_value);
  template <typename T>
  static T oop_atomic_xchg_at(oop base, ptrdiff_t offset, T new_value);

  template <typename T>
  static void store_at(oop base, ptrdiff_t offset, T value) {
    store(field_addr(base, offset), value);
  }

  template <typename T>
  static T load_at(oop base, ptrdiff_t offset) {
    return load<T>(field_addr(base, offset));
  }

  template <typename T>
  static T atomic_cmpxchg_at(oop base, ptrdiff_t offset, T compare_value, T new_value) {
    return atomic_cmpxchg(field_addr(base, offset), compare_value, new_value);
  }

  template <typename T>
  static T atomic_xchg_at(oop base, ptrdiff_t offset, T new_value) {
    return atomic_xchg(field_addr(base, offset), new_value);
  }

  template <typename T>
  static bool oop_arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                            arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                            size_t length);

  static void clone(oop src, oop dst, size_t size);

  static oop resolve(oop obj) { return obj; }
};

// Below is the implementation of the first 4 steps of the template pipeline:
// * Step 1: Set default decorators and decay types. This step gets rid of CV qualifiers
//           and sets default decorators to sensible values.
// * Step 2: Reduce types. This step makes sure there is only a single T type and not
//           multiple types. The P type of the address and T type of the value must
//           match.
// * Step 3: Pre-runtime dispatch. This step checks whether a runtime call can be
//           avoided, and in that case avoids it (calling raw accesses or
//           primitive accesses in a build that does not require primitive GC barriers)
// * Step 4: Runtime-dispatch. This step performs a runtime dispatch to the corresponding
//           BarrierSet::AccessBarrier accessor that attaches GC-required barriers
//           to the access.

namespace AccessInternal {
  template <typename T>
  struct OopOrNarrowOopInternal: AllStatic {
    typedef oop type;
  };

  template <>
  struct OopOrNarrowOopInternal<narrowOop>: AllStatic {
    typedef narrowOop type;
  };

  // This metafunction returns a canonicalized oop/narrowOop type for a passed
  // in oop-like types passed in from oop_* overloads where the user has sworn
  // that the passed in values should be oop-like (e.g. oop, oopDesc*, arrayOop,
  // narrowOoop, instanceOopDesc*, and random other things).
  // In the oop_* overloads, it must hold that if the passed in type T is not
  // narrowOop, then it by contract has to be one of many oop-like types implicitly
  // convertible to oop, and hence returns oop as the canonical oop type.
  // If it turns out it was not, then the implicit conversion to oop will fail
  // to compile, as desired.
  template <typename T>
  struct OopOrNarrowOop: AllStatic {
    typedef typename OopOrNarrowOopInternal<typename Decay<T>::type>::type type;
  };

  inline void* field_addr(oop base, ptrdiff_t byte_offset) {
    return reinterpret_cast<void*>(reinterpret_cast<intptr_t>((void*)base) + byte_offset);
  }
  // Step 4: Runtime dispatch
  // The RuntimeDispatch class is responsible for performing a runtime dispatch of the
  // accessor. This is required when the access either depends on whether compressed oops
  // is being used, or it depends on which GC implementation was chosen (e.g. requires GC
  // barriers). The way it works is that a function pointer initially pointing to an
  // accessor resolution function gets called for each access. Upon first invocation,
  // it resolves which accessor to be used in future invocations and patches the
  // function pointer to this new accessor.

  template <DecoratorSet decorators, typename T, BarrierType type>
  struct RuntimeDispatch: AllStatic {};

  template <DecoratorSet decorators, typename T>
  struct RuntimeDispatch<decorators, T, BARRIER_STORE>: AllStatic {
    typedef typename AccessFunction<decorators, T, BARRIER_STORE>::type func_t;
    static func_t _store_func;

    static void store_init(void* addr, T value);

    static inline void store(void* addr, T value) {
      _store_func(addr, value);
    }
  };

  template <DecoratorSet decorators, typename T>
  struct RuntimeDispatch<decorators, T, BARRIER_STORE_AT>: AllStatic {
    typedef typename AccessFunction<decorators, T, BARRIER_STORE_AT>::type func_t;
    static func_t _store_at_func;

    static void store_at_init(oop base, ptrdiff_t offset, T value);

    static inline void store_at(oop base, ptrdiff_t offset, T value) {
      _store_at_func(base, offset, value);
    }
  };

  template <DecoratorSet decorators, typename T>
  struct RuntimeDispatch<decorators, T, BARRIER_LOAD>: AllStatic {
    typedef typename AccessFunction<decorators, T, BARRIER_LOAD>::type func_t;
    static func_t _load_func;

    static T load_init(void* addr);

    static inline T load(void* addr) {
      return _load_func(addr);
    }
  };

  template <DecoratorSet decorators, typename T>
  struct RuntimeDispatch<decorators, T, BARRIER_LOAD_AT>: AllStatic {
    typedef typename AccessFunction<decorators, T, BARRIER_LOAD_AT>::type func_t;
    static func_t _load_at_func;

    static T load_at_init(oop base, ptrdiff_t offset);

    static inline T load_at(oop base, ptrdiff_t offset) {
      return _load_at_func(base, offset);
    }
  };

  template <DecoratorSet decorators, typename T>
  struct RuntimeDispatch<decorators, T, BARRIER_ATOMIC_CMPXCHG>: AllStatic {
    typedef typename AccessFunction<decorators, T, BARRIER_ATOMIC_CMPXCHG>::type func_t;
    static func_t _atomic_cmpxchg_func;

    static T atomic_cmpxchg_init(void* addr, T compare_value, T new_value);

    static inline T atomic_cmpxchg(void* addr, T compare_value, T new_value) {
      return _atomic_cmpxchg_func(addr, compare_value, new_value);
    }
  };

  template <DecoratorSet decorators, typename T>
  struct RuntimeDispatch<decorators, T, BARRIER_ATOMIC_CMPXCHG_AT>: AllStatic {
    typedef typename AccessFunction<decorators, T, BARRIER_ATOMIC_CMPXCHG_AT>::type func_t;
    static func_t _atomic_cmpxchg_at_func;

    static T atomic_cmpxchg_at_init(oop base, ptrdiff_t offset, T compare_value, T new_value);

    static inline T atomic_cmpxchg_at(oop base, ptrdiff_t offset, T compare_value, T new_value) {
      return _atomic_cmpxchg_at_func(base, offset, compare_value, new_value);
    }
  };

  template <DecoratorSet decorators, typename T>
  struct RuntimeDispatch<decorators, T, BARRIER_ATOMIC_XCHG>: AllStatic {
    typedef typename AccessFunction<decorators, T, BARRIER_ATOMIC_XCHG>::type func_t;
    static func_t _atomic_xchg_func;

    static T atomic_xchg_init(void* addr, T new_value);

    static inline T atomic_xchg(void* addr, T new_value) {
      return _atomic_xchg_func(addr, new_value);
    }
  };

  template <DecoratorSet decorators, typename T>
  struct RuntimeDispatch<decorators, T, BARRIER_ATOMIC_XCHG_AT>: AllStatic {
    typedef typename AccessFunction<decorators, T, BARRIER_ATOMIC_XCHG_AT>::type func_t;
    static func_t _atomic_xchg_at_func;

    static T atomic_xchg_at_init(oop base, ptrdiff_t offset, T new_value);

    static inline T atomic_xchg_at(oop base, ptrdiff_t offset, T new_value) {
      return _atomic_xchg_at_func(base, offset, new_value);
    }
  };

  template <DecoratorSet decorators, typename T>
  struct RuntimeDispatch<decorators, T, BARRIER_ARRAYCOPY>: AllStatic {
    typedef typename AccessFunction<decorators, T, BARRIER_ARRAYCOPY>::type func_t;
    static func_t _arraycopy_func;

    static bool arraycopy_init(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                               arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                               size_t length);

    static inline bool arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                                 arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                                 size_t length) {
      return _arraycopy_func(src_obj, src_offset_in_bytes, src_raw,
                             dst_obj, dst_offset_in_bytes, dst_raw,
                             length);
    }
  };

  template <DecoratorSet decorators, typename T>
  struct RuntimeDispatch<decorators, T, BARRIER_CLONE>: AllStatic {
    typedef typename AccessFunction<decorators, T, BARRIER_CLONE>::type func_t;
    static func_t _clone_func;

    static void clone_init(oop src, oop dst, size_t size);

    static inline void clone(oop src, oop dst, size_t size) {
      _clone_func(src, dst, size);
    }
  };

  template <DecoratorSet decorators, typename T>
  struct RuntimeDispatch<decorators, T, BARRIER_RESOLVE>: AllStatic {
    typedef typename AccessFunction<decorators, T, BARRIER_RESOLVE>::type func_t;
    static func_t _resolve_func;

    static oop resolve_init(oop obj);

    static inline oop resolve(oop obj) {
      return _resolve_func(obj);
    }
  };

  // Initialize the function pointers to point to the resolving function.
  template <DecoratorSet decorators, typename T>
  typename AccessFunction<decorators, T, BARRIER_STORE>::type
  RuntimeDispatch<decorators, T, BARRIER_STORE>::_store_func = &store_init;

  template <DecoratorSet decorators, typename T>
  typename AccessFunction<decorators, T, BARRIER_STORE_AT>::type
  RuntimeDispatch<decorators, T, BARRIER_STORE_AT>::_store_at_func = &store_at_init;

  template <DecoratorSet decorators, typename T>
  typename AccessFunction<decorators, T, BARRIER_LOAD>::type
  RuntimeDispatch<decorators, T, BARRIER_LOAD>::_load_func = &load_init;

  template <DecoratorSet decorators, typename T>
  typename AccessFunction<decorators, T, BARRIER_LOAD_AT>::type
  RuntimeDispatch<decorators, T, BARRIER_LOAD_AT>::_load_at_func = &load_at_init;

  template <DecoratorSet decorators, typename T>
  typename AccessFunction<decorators, T, BARRIER_ATOMIC_CMPXCHG>::type
  RuntimeDispatch<decorators, T, BARRIER_ATOMIC_CMPXCHG>::_atomic_cmpxchg_func = &atomic_cmpxchg_init;

  template <DecoratorSet decorators, typename T>
  typename AccessFunction<decorators, T, BARRIER_ATOMIC_CMPXCHG_AT>::type
  RuntimeDispatch<decorators, T, BARRIER_ATOMIC_CMPXCHG_AT>::_atomic_cmpxchg_at_func = &atomic_cmpxchg_at_init;

  template <DecoratorSet decorators, typename T>
  typename AccessFunction<decorators, T, BARRIER_ATOMIC_XCHG>::type
  RuntimeDispatch<decorators, T, BARRIER_ATOMIC_XCHG>::_atomic_xchg_func = &atomic_xchg_init;

  template <DecoratorSet decorators, typename T>
  typename AccessFunction<decorators, T, BARRIER_ATOMIC_XCHG_AT>::type
  RuntimeDispatch<decorators, T, BARRIER_ATOMIC_XCHG_AT>::_atomic_xchg_at_func = &atomic_xchg_at_init;

  template <DecoratorSet decorators, typename T>
  typename AccessFunction<decorators, T, BARRIER_ARRAYCOPY>::type
  RuntimeDispatch<decorators, T, BARRIER_ARRAYCOPY>::_arraycopy_func = &arraycopy_init;

  template <DecoratorSet decorators, typename T>
  typename AccessFunction<decorators, T, BARRIER_CLONE>::type
  RuntimeDispatch<decorators, T, BARRIER_CLONE>::_clone_func = &clone_init;

  template <DecoratorSet decorators, typename T>
  typename AccessFunction<decorators, T, BARRIER_RESOLVE>::type
  RuntimeDispatch<decorators, T, BARRIER_RESOLVE>::_resolve_func = &resolve_init;

  // Step 3: Pre-runtime dispatching.
  // The PreRuntimeDispatch class is responsible for filtering the barrier strength
  // decorators. That is, for AS_RAW, it hardwires the accesses without a runtime
  // dispatch point. Otherwise it goes through a runtime check if hardwiring was
  // not possible.
  struct PreRuntimeDispatch: AllStatic {
    template<DecoratorSet decorators>
    struct CanHardwireRaw: public IntegralConstant<
      bool,
      !HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value || // primitive access
      !HasDecorator<decorators, INTERNAL_CONVERT_COMPRESSED_OOP>::value || // don't care about compressed oops (oop* address)
      HasDecorator<decorators, INTERNAL_RT_USE_COMPRESSED_OOPS>::value> // we can infer we use compressed oops (narrowOop* address)
    {};

    static const DecoratorSet convert_compressed_oops = INTERNAL_RT_USE_COMPRESSED_OOPS | INTERNAL_CONVERT_COMPRESSED_OOP;

    template<DecoratorSet decorators>
    static bool is_hardwired_primitive() {
      return !HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value;
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value && CanHardwireRaw<decorators>::value>::type
    store(void* addr, T value) {
      typedef RawAccessBarrier<decorators & RAW_DECORATOR_MASK> Raw;
      if (HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value) {
        Raw::oop_store(addr, value);
      } else {
        Raw::store(addr, value);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value && !CanHardwireRaw<decorators>::value>::type
    store(void* addr, T value) {
      if (UseCompressedOops) {
        const DecoratorSet expanded_decorators = decorators | convert_compressed_oops;
        PreRuntimeDispatch::store<expanded_decorators>(addr, value);
      } else {
        const DecoratorSet expanded_decorators = decorators & ~convert_compressed_oops;
        PreRuntimeDispatch::store<expanded_decorators>(addr, value);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      !HasDecorator<decorators, AS_RAW>::value>::type
    store(void* addr, T value) {
      if (is_hardwired_primitive<decorators>()) {
        const DecoratorSet expanded_decorators = decorators | AS_RAW;
        PreRuntimeDispatch::store<expanded_decorators>(addr, value);
      } else {
        RuntimeDispatch<decorators, T, BARRIER_STORE>::store(addr, value);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value>::type
    store_at(oop base, ptrdiff_t offset, T value) {
      store<decorators>(field_addr(base, offset), value);
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      !HasDecorator<decorators, AS_RAW>::value>::type
    store_at(oop base, ptrdiff_t offset, T value) {
      if (is_hardwired_primitive<decorators>()) {
        const DecoratorSet expanded_decorators = decorators | AS_RAW;
        PreRuntimeDispatch::store_at<expanded_decorators>(base, offset, value);
      } else {
        RuntimeDispatch<decorators, T, BARRIER_STORE_AT>::store_at(base, offset, value);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value && CanHardwireRaw<decorators>::value, T>::type
    load(void* addr) {
      typedef RawAccessBarrier<decorators & RAW_DECORATOR_MASK> Raw;
      if (HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value) {
        return Raw::template oop_load<T>(addr);
      } else {
        return Raw::template load<T>(addr);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value && !CanHardwireRaw<decorators>::value, T>::type
    load(void* addr) {
      if (UseCompressedOops) {
        const DecoratorSet expanded_decorators = decorators | convert_compressed_oops;
        return PreRuntimeDispatch::load<expanded_decorators, T>(addr);
      } else {
        const DecoratorSet expanded_decorators = decorators & ~convert_compressed_oops;
        return PreRuntimeDispatch::load<expanded_decorators, T>(addr);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      !HasDecorator<decorators, AS_RAW>::value, T>::type
    load(void* addr) {
      if (is_hardwired_primitive<decorators>()) {
        const DecoratorSet expanded_decorators = decorators | AS_RAW;
        return PreRuntimeDispatch::load<expanded_decorators, T>(addr);
      } else {
        return RuntimeDispatch<decorators, T, BARRIER_LOAD>::load(addr);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value, T>::type
    load_at(oop base, ptrdiff_t offset) {
      return load<decorators, T>(field_addr(base, offset));
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      !HasDecorator<decorators, AS_RAW>::value, T>::type
    load_at(oop base, ptrdiff_t offset) {
      if (is_hardwired_primitive<decorators>()) {
        const DecoratorSet expanded_decorators = decorators | AS_RAW;
        return PreRuntimeDispatch::load_at<expanded_decorators, T>(base, offset);
      } else {
        return RuntimeDispatch<decorators, T, BARRIER_LOAD_AT>::load_at(base, offset);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value && CanHardwireRaw<decorators>::value, T>::type
    atomic_cmpxchg(void* addr, T compare_value, T new_value) {
      typedef RawAccessBarrier<decorators & RAW_DECORATOR_MASK> Raw;
      if (HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value) {
        return Raw::oop_atomic_cmpxchg(addr, compare_value, new_value);
      } else {
        return Raw::atomic_cmpxchg(addr, compare_value, new_value);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value && !CanHardwireRaw<decorators>::value, T>::type
    atomic_cmpxchg(void* addr, T compare_value, T new_value) {
      if (UseCompressedOops) {
        const DecoratorSet expanded_decorators = decorators | convert_compressed_oops;
        return PreRuntimeDispatch::atomic_cmpxchg<expanded_decorators>(addr, compare_value, new_value);
      } else {
        const DecoratorSet expanded_decorators = decorators & ~convert_compressed_oops;
        return PreRuntimeDispatch::atomic_cmpxchg<expanded_decorators>(addr, compare_value, new_value);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      !HasDecorator<decorators, AS_RAW>::value, T>::type
    atomic_cmpxchg(void* addr, T compare_value, T new_value) {
      if (is_hardwired_primitive<decorators>()) {
        const DecoratorSet expanded_decorators = decorators | AS_RAW;
        return PreRuntimeDispatch::atomic_cmpxchg<expanded_decorators>(addr, compare_value, new_value);
      } else {
        return RuntimeDispatch<decorators, T, BARRIER_ATOMIC_CMPXCHG>::atomic_cmpxchg(addr, compare_value, new_value);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value, T>::type
    atomic_cmpxchg_at(oop base, ptrdiff_t offset, T compare_value, T new_value) {
      return atomic_cmpxchg<decorators>(field_addr(base, offset), compare_value, new_value);
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      !HasDecorator<decorators, AS_RAW>::value, T>::type
    atomic_cmpxchg_at(oop base, ptrdiff_t offset, T compare_value, T new_value) {
      if (is_hardwired_primitive<decorators>()) {
        const DecoratorSet expanded_decorators = decorators | AS_RAW;
        return PreRuntimeDispatch::atomic_cmpxchg_at<expanded_decorators>(base, offset, compare_value, new_value);
      } else {
        return RuntimeDispatch<decorators, T, BARRIER_ATOMIC_CMPXCHG_AT>::atomic_cmpxchg_at(base, offset, compare_value, new_value);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value && CanHardwireRaw<decorators>::value, T>::type
    atomic_xchg(void* addr, T new_value) {
      typedef RawAccessBarrier<decorators & RAW_DECORATOR_MASK> Raw;
      if (HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value) {
        return Raw::oop_atomic_xchg(addr, new_value);
      } else {
        return Raw::atomic_xchg(addr, new_value);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value && !CanHardwireRaw<decorators>::value, T>::type
    atomic_xchg(void* addr, T new_value) {
      if (UseCompressedOops) {
        const DecoratorSet expanded_decorators = decorators | convert_compressed_oops;
        return PreRuntimeDispatch::atomic_xchg<expanded_decorators>(addr, new_value);
      } else {
        const DecoratorSet expanded_decorators = decorators & ~convert_compressed_oops;
        return PreRuntimeDispatch::atomic_xchg<expanded_decorators>(addr, new_value);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      !HasDecorator<decorators, AS_RAW>::value, T>::type
    atomic_xchg(void* addr, T new_value) {
      if (is_hardwired_primitive<decorators>()) {
        const DecoratorSet expanded_decorators = decorators | AS_RAW;
        return PreRuntimeDispatch::atomic_xchg<expanded_decorators>(addr, new_value);
      } else {
        return RuntimeDispatch<decorators, T, BARRIER_ATOMIC_XCHG>::atomic_xchg(addr, new_value);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value, T>::type
    atomic_xchg_at(oop base, ptrdiff_t offset, T new_value) {
      return atomic_xchg<decorators>(field_addr(base, offset), new_value);
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      !HasDecorator<decorators, AS_RAW>::value, T>::type
    atomic_xchg_at(oop base, ptrdiff_t offset, T new_value) {
      if (is_hardwired_primitive<decorators>()) {
        const DecoratorSet expanded_decorators = decorators | AS_RAW;
        return PreRuntimeDispatch::atomic_xchg<expanded_decorators>(base, offset, new_value);
      } else {
        return RuntimeDispatch<decorators, T, BARRIER_ATOMIC_XCHG_AT>::atomic_xchg_at(base, offset, new_value);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value && CanHardwireRaw<decorators>::value, bool>::type
    arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
              arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
              size_t length) {
      typedef RawAccessBarrier<decorators & RAW_DECORATOR_MASK> Raw;
      if (HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value) {
        return Raw::oop_arraycopy(src_obj, src_offset_in_bytes, src_raw,
                                  dst_obj, dst_offset_in_bytes, dst_raw,
                                  length);
      } else {
        return Raw::arraycopy(src_obj, src_offset_in_bytes, src_raw,
                              dst_obj, dst_offset_in_bytes, dst_raw,
                              length);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value && !CanHardwireRaw<decorators>::value, bool>::type
    arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
              arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
              size_t length) {
      if (UseCompressedOops) {
        const DecoratorSet expanded_decorators = decorators | convert_compressed_oops;
        return PreRuntimeDispatch::arraycopy<expanded_decorators>(src_obj, src_offset_in_bytes, src_raw,
                                                                  dst_obj, dst_offset_in_bytes, dst_raw,
                                                                  length);
      } else {
        const DecoratorSet expanded_decorators = decorators & ~convert_compressed_oops;
        return PreRuntimeDispatch::arraycopy<expanded_decorators>(src_obj, src_offset_in_bytes, src_raw,
                                                                  dst_obj, dst_offset_in_bytes, dst_raw,
                                                                  length);
      }
    }

    template <DecoratorSet decorators, typename T>
    inline static typename EnableIf<
      !HasDecorator<decorators, AS_RAW>::value, bool>::type
    arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
              arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
              size_t length) {
      if (is_hardwired_primitive<decorators>()) {
        const DecoratorSet expanded_decorators = decorators | AS_RAW;
        return PreRuntimeDispatch::arraycopy<expanded_decorators>(src_obj, src_offset_in_bytes, src_raw,
                                                                  dst_obj, dst_offset_in_bytes, dst_raw,
                                                                  length);
      } else {
        return RuntimeDispatch<decorators, T, BARRIER_ARRAYCOPY>::arraycopy(src_obj, src_offset_in_bytes, src_raw,
                                                                            dst_obj, dst_offset_in_bytes, dst_raw,
                                                                            length);
      }
    }

    template <DecoratorSet decorators>
    inline static typename EnableIf<
      HasDecorator<decorators, AS_RAW>::value>::type
    clone(oop src, oop dst, size_t size) {
      typedef RawAccessBarrier<decorators & RAW_DECORATOR_MASK> Raw;
      Raw::clone(src, dst, size);
    }

    template <DecoratorSet decorators>
    inline static typename EnableIf<
      !HasDecorator<decorators, AS_RAW>::value>::type
    clone(oop src, oop dst, size_t size) {
      RuntimeDispatch<decorators, oop, BARRIER_CLONE>::clone(src, dst, size);
    }
  };

  // Step 2: Reduce types.
  // Enforce that for non-oop types, T and P have to be strictly the same.
  // P is the type of the address and T is the type of the values.
  // As for oop types, it is allow to send T in {narrowOop, oop} and
  // P in {narrowOop, oop, HeapWord*}. The following rules apply according to
  // the subsequent table. (columns are P, rows are T)
  // |           | HeapWord  |   oop   | narrowOop |
  // |   oop     |  rt-comp  | hw-none |  hw-comp  |
  // | narrowOop |     x     |    x    |  hw-none  |
  //
  // x means not allowed
  // rt-comp means it must be checked at runtime whether the oop is compressed.
  // hw-none means it is statically known the oop will not be compressed.
  // hw-comp means it is statically known the oop will be compressed.

  template <DecoratorSet decorators, typename T>
  inline void store_reduce_types(T* addr, T value) {
    PreRuntimeDispatch::store<decorators>(addr, value);
  }

  template <DecoratorSet decorators>
  inline void store_reduce_types(narrowOop* addr, oop value) {
    const DecoratorSet expanded_decorators = decorators | INTERNAL_CONVERT_COMPRESSED_OOP |
                                             INTERNAL_RT_USE_COMPRESSED_OOPS;
    PreRuntimeDispatch::store<expanded_decorators>(addr, value);
  }

  template <DecoratorSet decorators>
  inline void store_reduce_types(narrowOop* addr, narrowOop value) {
    const DecoratorSet expanded_decorators = decorators | INTERNAL_CONVERT_COMPRESSED_OOP |
                                             INTERNAL_RT_USE_COMPRESSED_OOPS;
    PreRuntimeDispatch::store<expanded_decorators>(addr, value);
  }

  template <DecoratorSet decorators>
  inline void store_reduce_types(HeapWord* addr, oop value) {
    const DecoratorSet expanded_decorators = decorators | INTERNAL_CONVERT_COMPRESSED_OOP;
    PreRuntimeDispatch::store<expanded_decorators>(addr, value);
  }

  template <DecoratorSet decorators, typename T>
  inline T atomic_cmpxchg_reduce_types(T* addr, T compare_value, T new_value) {
    return PreRuntimeDispatch::atomic_cmpxchg<decorators>(addr, compare_value, new_value);
  }

  template <DecoratorSet decorators>
  inline oop atomic_cmpxchg_reduce_types(narrowOop* addr, oop compare_value, oop new_value) {
    const DecoratorSet expanded_decorators = decorators | INTERNAL_CONVERT_COMPRESSED_OOP |
                                             INTERNAL_RT_USE_COMPRESSED_OOPS;
    return PreRuntimeDispatch::atomic_cmpxchg<expanded_decorators>(addr, compare_value, new_value);
  }

  template <DecoratorSet decorators>
  inline narrowOop atomic_cmpxchg_reduce_types(narrowOop* addr, narrowOop compare_value, narrowOop new_value) {
    const DecoratorSet expanded_decorators = decorators | INTERNAL_CONVERT_COMPRESSED_OOP |
                                             INTERNAL_RT_USE_COMPRESSED_OOPS;
    return PreRuntimeDispatch::atomic_cmpxchg<expanded_decorators>(addr, compare_value, new_value);
  }

  template <DecoratorSet decorators>
  inline oop atomic_cmpxchg_reduce_types(HeapWord* addr,
                                         oop compare_value,
                                         oop new_value) {
    const DecoratorSet expanded_decorators = decorators | INTERNAL_CONVERT_COMPRESSED_OOP;
    return PreRuntimeDispatch::atomic_cmpxchg<expanded_decorators>(addr, compare_value, new_value);
  }

  template <DecoratorSet decorators, typename T>
  inline T atomic_xchg_reduce_types(T* addr, T new_value) {
    const DecoratorSet expanded_decorators = decorators;
    return PreRuntimeDispatch::atomic_xchg<expanded_decorators>(addr, new_value);
  }

  template <DecoratorSet decorators>
  inline oop atomic_xchg_reduce_types(narrowOop* addr, oop new_value) {
    const DecoratorSet expanded_decorators = decorators | INTERNAL_CONVERT_COMPRESSED_OOP |
                                             INTERNAL_RT_USE_COMPRESSED_OOPS;
    return PreRuntimeDispatch::atomic_xchg<expanded_decorators>(addr, new_value);
  }

  template <DecoratorSet decorators>
  inline narrowOop atomic_xchg_reduce_types(narrowOop* addr, narrowOop new_value) {
    const DecoratorSet expanded_decorators = decorators | INTERNAL_CONVERT_COMPRESSED_OOP |
                                             INTERNAL_RT_USE_COMPRESSED_OOPS;
    return PreRuntimeDispatch::atomic_xchg<expanded_decorators>(addr, new_value);
  }

  template <DecoratorSet decorators>
  inline oop atomic_xchg_reduce_types(HeapWord* addr, oop new_value) {
    const DecoratorSet expanded_decorators = decorators | INTERNAL_CONVERT_COMPRESSED_OOP;
    return PreRuntimeDispatch::atomic_xchg<expanded_decorators>(addr, new_value);
  }

  template <DecoratorSet decorators, typename T>
  inline T load_reduce_types(T* addr) {
    return PreRuntimeDispatch::load<decorators, T>(addr);
  }

  template <DecoratorSet decorators, typename T>
  inline typename OopOrNarrowOop<T>::type load_reduce_types(narrowOop* addr) {
    const DecoratorSet expanded_decorators = decorators | INTERNAL_CONVERT_COMPRESSED_OOP |
                                             INTERNAL_RT_USE_COMPRESSED_OOPS;
    return PreRuntimeDispatch::load<expanded_decorators, typename OopOrNarrowOop<T>::type>(addr);
  }

  template <DecoratorSet decorators, typename T>
  inline oop load_reduce_types(HeapWord* addr) {
    const DecoratorSet expanded_decorators = decorators | INTERNAL_CONVERT_COMPRESSED_OOP;
    return PreRuntimeDispatch::load<expanded_decorators, oop>(addr);
  }

  template <DecoratorSet decorators, typename T>
  inline bool arraycopy_reduce_types(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                                     arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                                     size_t length) {
    return PreRuntimeDispatch::arraycopy<decorators>(src_obj, src_offset_in_bytes, src_raw,
                                                     dst_obj, dst_offset_in_bytes, dst_raw,
                                                     length);
  }

  template <DecoratorSet decorators>
  inline bool arraycopy_reduce_types(arrayOop src_obj, size_t src_offset_in_bytes, HeapWord* src_raw,
                                     arrayOop dst_obj, size_t dst_offset_in_bytes, HeapWord* dst_raw,
                                     size_t length) {
    const DecoratorSet expanded_decorators = decorators | INTERNAL_CONVERT_COMPRESSED_OOP;
    return PreRuntimeDispatch::arraycopy<expanded_decorators>(src_obj, src_offset_in_bytes, src_raw,
                                                              dst_obj, dst_offset_in_bytes, dst_raw,
                                                              length);
  }

  template <DecoratorSet decorators>
  inline bool arraycopy_reduce_types(arrayOop src_obj, size_t src_offset_in_bytes, narrowOop* src_raw,
                                     arrayOop dst_obj, size_t dst_offset_in_bytes, narrowOop* dst_raw,
                                     size_t length) {
    const DecoratorSet expanded_decorators = decorators | INTERNAL_CONVERT_COMPRESSED_OOP |
                                             INTERNAL_RT_USE_COMPRESSED_OOPS;
    return PreRuntimeDispatch::arraycopy<expanded_decorators>(src_obj, src_offset_in_bytes, src_raw,
                                                              dst_obj, dst_offset_in_bytes, dst_raw,
                                                              length);
  }

  // Step 1: Set default decorators. This step remembers if a type was volatile
  // and then sets the MO_RELAXED decorator by default. Otherwise, a default
  // memory ordering is set for the access, and the implied decorator rules
  // are applied to select sensible defaults for decorators that have not been
  // explicitly set. For example, default object referent strength is set to strong.
  // This step also decays the types passed in (e.g. getting rid of CV qualifiers
  // and references from the types). This step also perform some type verification
  // that the passed in types make sense.

  template <DecoratorSet decorators, typename T>
  static void verify_types(){
    // If this fails to compile, then you have sent in something that is
    // not recognized as a valid primitive type to a primitive Access function.
    STATIC_ASSERT((HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value || // oops have already been validated
                   (IsPointer<T>::value || IsIntegral<T>::value) ||
                    IsFloatingPoint<T>::value)); // not allowed primitive type
  }

  template <DecoratorSet decorators, typename P, typename T>
  inline void store(P* addr, T value) {
    verify_types<decorators, T>();
    typedef typename Decay<P>::type DecayedP;
    typedef typename Decay<T>::type DecayedT;
    DecayedT decayed_value = value;
    // If a volatile address is passed in but no memory ordering decorator,
    // set the memory ordering to MO_RELAXED by default.
    const DecoratorSet expanded_decorators = DecoratorFixup<
      (IsVolatile<P>::value && !HasDecorator<decorators, MO_DECORATOR_MASK>::value) ?
      (MO_RELAXED | decorators) : decorators>::value;
    store_reduce_types<expanded_decorators>(const_cast<DecayedP*>(addr), decayed_value);
  }

  template <DecoratorSet decorators, typename T>
  inline void store_at(oop base, ptrdiff_t offset, T value) {
    verify_types<decorators, T>();
    typedef typename Decay<T>::type DecayedT;
    DecayedT decayed_value = value;
    const DecoratorSet expanded_decorators = DecoratorFixup<decorators |
                                             (HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value ?
                                              INTERNAL_CONVERT_COMPRESSED_OOP : DECORATORS_NONE)>::value;
    PreRuntimeDispatch::store_at<expanded_decorators>(base, offset, decayed_value);
  }

  template <DecoratorSet decorators, typename P, typename T>
  inline T load(P* addr) {
    verify_types<decorators, T>();
    typedef typename Decay<P>::type DecayedP;
    typedef typename Conditional<HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value,
                                 typename OopOrNarrowOop<T>::type,
                                 typename Decay<T>::type>::type DecayedT;
    // If a volatile address is passed in but no memory ordering decorator,
    // set the memory ordering to MO_RELAXED by default.
    const DecoratorSet expanded_decorators = DecoratorFixup<
      (IsVolatile<P>::value && !HasDecorator<decorators, MO_DECORATOR_MASK>::value) ?
      (MO_RELAXED | decorators) : decorators>::value;
    return load_reduce_types<expanded_decorators, DecayedT>(const_cast<DecayedP*>(addr));
  }

  template <DecoratorSet decorators, typename T>
  inline T load_at(oop base, ptrdiff_t offset) {
    verify_types<decorators, T>();
    typedef typename Conditional<HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value,
                                 typename OopOrNarrowOop<T>::type,
                                 typename Decay<T>::type>::type DecayedT;
    // Expand the decorators (figure out sensible defaults)
    // Potentially remember if we need compressed oop awareness
    const DecoratorSet expanded_decorators = DecoratorFixup<decorators |
                                             (HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value ?
                                              INTERNAL_CONVERT_COMPRESSED_OOP : DECORATORS_NONE)>::value;
    return PreRuntimeDispatch::load_at<expanded_decorators, DecayedT>(base, offset);
  }

  template <DecoratorSet decorators, typename P, typename T>
  inline T atomic_cmpxchg(P* addr, T compare_value, T new_value) {
    verify_types<decorators, T>();
    typedef typename Decay<P>::type DecayedP;
    typedef typename Decay<T>::type DecayedT;
    DecayedT new_decayed_value = new_value;
    DecayedT compare_decayed_value = compare_value;
    const DecoratorSet expanded_decorators = DecoratorFixup<
      (!HasDecorator<decorators, MO_DECORATOR_MASK>::value) ?
      (MO_SEQ_CST | decorators) : decorators>::value;
    return atomic_cmpxchg_reduce_types<expanded_decorators>(const_cast<DecayedP*>(addr),
                                                            compare_decayed_value,
                                                            new_decayed_value);
  }

  template <DecoratorSet decorators, typename T>
  inline T atomic_cmpxchg_at(oop base, ptrdiff_t offset, T compare_value, T new_value) {
    verify_types<decorators, T>();
    typedef typename Decay<T>::type DecayedT;
    DecayedT new_decayed_value = new_value;
    DecayedT compare_decayed_value = compare_value;
    // Determine default memory ordering
    const DecoratorSet expanded_decorators = DecoratorFixup<
      (!HasDecorator<decorators, MO_DECORATOR_MASK>::value) ?
      (MO_SEQ_CST | decorators) : decorators>::value;
    // Potentially remember that we need compressed oop awareness
    const DecoratorSet final_decorators = expanded_decorators |
                                          (HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value ?
                                           INTERNAL_CONVERT_COMPRESSED_OOP : DECORATORS_NONE);
    return PreRuntimeDispatch::atomic_cmpxchg_at<final_decorators>(base, offset, compare_decayed_value,
                                                                   new_decayed_value);
  }

  template <DecoratorSet decorators, typename P, typename T>
  inline T atomic_xchg(P* addr, T new_value) {
    verify_types<decorators, T>();
    typedef typename Decay<P>::type DecayedP;
    typedef typename Decay<T>::type DecayedT;
    DecayedT new_decayed_value = new_value;
    // atomic_xchg is only available in SEQ_CST flavour.
    const DecoratorSet expanded_decorators = DecoratorFixup<decorators | MO_SEQ_CST>::value;
    return atomic_xchg_reduce_types<expanded_decorators>(const_cast<DecayedP*>(addr),
                                                         new_decayed_value);
  }

  template <DecoratorSet decorators, typename T>
  inline T atomic_xchg_at(oop base, ptrdiff_t offset, T new_value) {
    verify_types<decorators, T>();
    typedef typename Decay<T>::type DecayedT;
    DecayedT new_decayed_value = new_value;
    // atomic_xchg is only available in SEQ_CST flavour.
    const DecoratorSet expanded_decorators = DecoratorFixup<decorators | MO_SEQ_CST |
                                             (HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value ?
                                              INTERNAL_CONVERT_COMPRESSED_OOP : DECORATORS_NONE)>::value;
    return PreRuntimeDispatch::atomic_xchg_at<expanded_decorators>(base, offset, new_decayed_value);
  }

  template <DecoratorSet decorators, typename T>
  inline bool arraycopy(arrayOop src_obj, size_t src_offset_in_bytes, const T* src_raw,
                        arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                        size_t length) {
    STATIC_ASSERT((HasDecorator<decorators, INTERNAL_VALUE_IS_OOP>::value ||
                   (IsSame<T, void>::value || IsIntegral<T>::value) ||
                    IsFloatingPoint<T>::value)); // arraycopy allows type erased void elements
    typedef typename Decay<T>::type DecayedT;
    const DecoratorSet expanded_decorators = DecoratorFixup<decorators | IS_ARRAY | IN_HEAP>::value;
    return arraycopy_reduce_types<expanded_decorators>(src_obj, src_offset_in_bytes, const_cast<DecayedT*>(src_raw),
                                                       dst_obj, dst_offset_in_bytes, const_cast<DecayedT*>(dst_raw),
                                                       length);
  }

  template <DecoratorSet decorators>
  inline void clone(oop src, oop dst, size_t size) {
    const DecoratorSet expanded_decorators = DecoratorFixup<decorators>::value;
    PreRuntimeDispatch::clone<expanded_decorators>(src, dst, size);
  }

  // Infer the type that should be returned from an Access::oop_load.
  template <typename P, DecoratorSet decorators>
  class OopLoadProxy: public StackObj {
  private:
    P *const _addr;
  public:
    OopLoadProxy(P* addr) : _addr(addr) {}

    inline operator oop() {
      return load<decorators | INTERNAL_VALUE_IS_OOP, P, oop>(_addr);
    }

    inline operator narrowOop() {
      return load<decorators | INTERNAL_VALUE_IS_OOP, P, narrowOop>(_addr);
    }

    template <typename T>
    inline bool operator ==(const T& other) const {
      return load<decorators | INTERNAL_VALUE_IS_OOP, P, T>(_addr) == other;
    }

    template <typename T>
    inline bool operator !=(const T& other) const {
      return load<decorators | INTERNAL_VALUE_IS_OOP, P, T>(_addr) != other;
    }
  };

  // Infer the type that should be returned from an Access::load_at.
  template <DecoratorSet decorators>
  class LoadAtProxy: public StackObj {
  private:
    const oop _base;
    const ptrdiff_t _offset;
  public:
    LoadAtProxy(oop base, ptrdiff_t offset) : _base(base), _offset(offset) {}

    template <typename T>
    inline operator T() const {
      return load_at<decorators, T>(_base, _offset);
    }

    template <typename T>
    inline bool operator ==(const T& other) const { return load_at<decorators, T>(_base, _offset) == other; }

    template <typename T>
    inline bool operator !=(const T& other) const { return load_at<decorators, T>(_base, _offset) != other; }
  };

  // Infer the type that should be returned from an Access::oop_load_at.
  template <DecoratorSet decorators>
  class OopLoadAtProxy: public StackObj {
  private:
    const oop _base;
    const ptrdiff_t _offset;
  public:
    OopLoadAtProxy(oop base, ptrdiff_t offset) : _base(base), _offset(offset) {}

    inline operator oop() const {
      return load_at<decorators | INTERNAL_VALUE_IS_OOP, oop>(_base, _offset);
    }

    inline operator narrowOop() const {
      return load_at<decorators | INTERNAL_VALUE_IS_OOP, narrowOop>(_base, _offset);
    }

    template <typename T>
    inline bool operator ==(const T& other) const {
      return load_at<decorators | INTERNAL_VALUE_IS_OOP, T>(_base, _offset) == other;
    }

    template <typename T>
    inline bool operator !=(const T& other) const {
      return load_at<decorators | INTERNAL_VALUE_IS_OOP, T>(_base, _offset) != other;
    }
  };
}

#endif // SHARE_OOPS_ACCESSBACKEND_HPP
