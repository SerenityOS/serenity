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
 *
 */

#ifndef SHARE_OOPS_ACCESS_INLINE_HPP
#define SHARE_OOPS_ACCESS_INLINE_HPP

#include "oops/access.hpp"

#include "gc/shared/barrierSet.inline.hpp"
#include "gc/shared/barrierSetConfig.inline.hpp"
#include "oops/accessBackend.inline.hpp"

// This file outlines the last 2 steps of the template pipeline of accesses going through
// the Access API.
// * Step 5.a: Barrier resolution. This step is invoked the first time a runtime-dispatch
//             happens for an access. The appropriate BarrierSet::AccessBarrier accessor
//             is resolved, then the function pointer is updated to that accessor for
//             future invocations.
// * Step 5.b: Post-runtime dispatch. This step now casts previously unknown types such
//             as the address type of an oop on the heap (is it oop* or narrowOop*) to
//             the appropriate type. It also splits sufficiently orthogonal accesses into
//             different functions, such as whether the access involves oops or primitives
//             and whether the access is performed on the heap or outside. Then the
//             appropriate BarrierSet::AccessBarrier is called to perform the access.

namespace AccessInternal {
  // Step 5.b: Post-runtime dispatch.
  // This class is the last step before calling the BarrierSet::AccessBarrier.
  // Here we make sure to figure out types that were not known prior to the
  // runtime dispatch, such as whether an oop on the heap is oop or narrowOop.
  // We also split orthogonal barriers such as handling primitives vs oops
  // and on-heap vs off-heap into different calls to the barrier set.
  template <class GCBarrierType, BarrierType type, DecoratorSet decorators>
  struct PostRuntimeDispatch: public AllStatic { };

  template <class GCBarrierType, DecoratorSet decorators>
  struct PostRuntimeDispatch<GCBarrierType, BARRIER_STORE, decorators>: public AllStatic {
    template <typename T>
    static void access_barrier(void* addr, T value) {
      GCBarrierType::store_in_heap(reinterpret_cast<T*>(addr), value);
    }

    static void oop_access_barrier(void* addr, oop value) {
      typedef typename HeapOopType<decorators>::type OopType;
      if (HasDecorator<decorators, IN_HEAP>::value) {
        GCBarrierType::oop_store_in_heap(reinterpret_cast<OopType*>(addr), value);
      } else {
        GCBarrierType::oop_store_not_in_heap(reinterpret_cast<OopType*>(addr), value);
      }
    }
  };

  template <class GCBarrierType, DecoratorSet decorators>
  struct PostRuntimeDispatch<GCBarrierType, BARRIER_LOAD, decorators>: public AllStatic {
    template <typename T>
    static T access_barrier(void* addr) {
      return GCBarrierType::load_in_heap(reinterpret_cast<T*>(addr));
    }

    static oop oop_access_barrier(void* addr) {
      typedef typename HeapOopType<decorators>::type OopType;
      if (HasDecorator<decorators, IN_HEAP>::value) {
        return GCBarrierType::oop_load_in_heap(reinterpret_cast<OopType*>(addr));
      } else {
        return GCBarrierType::oop_load_not_in_heap(reinterpret_cast<OopType*>(addr));
      }
    }
  };

  template <class GCBarrierType, DecoratorSet decorators>
  struct PostRuntimeDispatch<GCBarrierType, BARRIER_ATOMIC_XCHG, decorators>: public AllStatic {
    template <typename T>
    static T access_barrier(void* addr, T new_value) {
      return GCBarrierType::atomic_xchg_in_heap(reinterpret_cast<T*>(addr), new_value);
    }

    static oop oop_access_barrier(void* addr, oop new_value) {
      typedef typename HeapOopType<decorators>::type OopType;
      if (HasDecorator<decorators, IN_HEAP>::value) {
        return GCBarrierType::oop_atomic_xchg_in_heap(reinterpret_cast<OopType*>(addr), new_value);
      } else {
        return GCBarrierType::oop_atomic_xchg_not_in_heap(reinterpret_cast<OopType*>(addr), new_value);
      }
    }
  };

  template <class GCBarrierType, DecoratorSet decorators>
  struct PostRuntimeDispatch<GCBarrierType, BARRIER_ATOMIC_CMPXCHG, decorators>: public AllStatic {
    template <typename T>
    static T access_barrier(void* addr, T compare_value, T new_value) {
      return GCBarrierType::atomic_cmpxchg_in_heap(reinterpret_cast<T*>(addr), compare_value, new_value);
    }

    static oop oop_access_barrier(void* addr, oop compare_value, oop new_value) {
      typedef typename HeapOopType<decorators>::type OopType;
      if (HasDecorator<decorators, IN_HEAP>::value) {
        return GCBarrierType::oop_atomic_cmpxchg_in_heap(reinterpret_cast<OopType*>(addr), compare_value, new_value);
      } else {
        return GCBarrierType::oop_atomic_cmpxchg_not_in_heap(reinterpret_cast<OopType*>(addr), compare_value, new_value);
      }
    }
  };

  template <class GCBarrierType, DecoratorSet decorators>
  struct PostRuntimeDispatch<GCBarrierType, BARRIER_ARRAYCOPY, decorators>: public AllStatic {
    template <typename T>
    static bool access_barrier(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                               arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                               size_t length) {
      GCBarrierType::arraycopy_in_heap(src_obj, src_offset_in_bytes, src_raw,
                                       dst_obj, dst_offset_in_bytes, dst_raw,
                                       length);
      return true;
    }

    template <typename T>
    static bool oop_access_barrier(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                                   arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                                   size_t length) {
      typedef typename HeapOopType<decorators>::type OopType;
      return GCBarrierType::oop_arraycopy_in_heap(src_obj, src_offset_in_bytes, reinterpret_cast<OopType*>(src_raw),
                                                  dst_obj, dst_offset_in_bytes, reinterpret_cast<OopType*>(dst_raw),
                                                  length);
    }
  };

  template <class GCBarrierType, DecoratorSet decorators>
  struct PostRuntimeDispatch<GCBarrierType, BARRIER_STORE_AT, decorators>: public AllStatic {
    template <typename T>
    static void access_barrier(oop base, ptrdiff_t offset, T value) {
      GCBarrierType::store_in_heap_at(base, offset, value);
    }

    static void oop_access_barrier(oop base, ptrdiff_t offset, oop value) {
      GCBarrierType::oop_store_in_heap_at(base, offset, value);
    }
  };

  template <class GCBarrierType, DecoratorSet decorators>
  struct PostRuntimeDispatch<GCBarrierType, BARRIER_LOAD_AT, decorators>: public AllStatic {
    template <typename T>
    static T access_barrier(oop base, ptrdiff_t offset) {
      return GCBarrierType::template load_in_heap_at<T>(base, offset);
    }

    static oop oop_access_barrier(oop base, ptrdiff_t offset) {
      return GCBarrierType::oop_load_in_heap_at(base, offset);
    }
  };

  template <class GCBarrierType, DecoratorSet decorators>
  struct PostRuntimeDispatch<GCBarrierType, BARRIER_ATOMIC_XCHG_AT, decorators>: public AllStatic {
    template <typename T>
    static T access_barrier(oop base, ptrdiff_t offset, T new_value) {
      return GCBarrierType::atomic_xchg_in_heap_at(base, offset, new_value);
    }

    static oop oop_access_barrier(oop base, ptrdiff_t offset, oop new_value) {
      return GCBarrierType::oop_atomic_xchg_in_heap_at(base, offset, new_value);
    }
  };

  template <class GCBarrierType, DecoratorSet decorators>
  struct PostRuntimeDispatch<GCBarrierType, BARRIER_ATOMIC_CMPXCHG_AT, decorators>: public AllStatic {
    template <typename T>
    static T access_barrier(oop base, ptrdiff_t offset, T compare_value, T new_value) {
      return GCBarrierType::atomic_cmpxchg_in_heap_at(base, offset, compare_value, new_value);
    }

    static oop oop_access_barrier(oop base, ptrdiff_t offset, oop compare_value, oop new_value) {
      return GCBarrierType::oop_atomic_cmpxchg_in_heap_at(base, offset, compare_value, new_value);
    }
  };

  template <class GCBarrierType, DecoratorSet decorators>
  struct PostRuntimeDispatch<GCBarrierType, BARRIER_CLONE, decorators>: public AllStatic {
    static void access_barrier(oop src, oop dst, size_t size) {
      GCBarrierType::clone_in_heap(src, dst, size);
    }
  };

  template <class GCBarrierType, DecoratorSet decorators>
  struct PostRuntimeDispatch<GCBarrierType, BARRIER_RESOLVE, decorators>: public AllStatic {
    static oop access_barrier(oop obj) {
      return GCBarrierType::resolve(obj);
    }
  };

  // Resolving accessors with barriers from the barrier set happens in two steps.
  // 1. Expand paths with runtime-decorators, e.g. is UseCompressedOops on or off.
  // 2. Expand paths for each BarrierSet available in the system.
  template <DecoratorSet decorators, typename FunctionPointerT, BarrierType barrier_type>
  struct BarrierResolver: public AllStatic {
    template <DecoratorSet ds>
    static typename EnableIf<
      HasDecorator<ds, INTERNAL_VALUE_IS_OOP>::value,
      FunctionPointerT>::type
    resolve_barrier_gc() {
      BarrierSet* bs = BarrierSet::barrier_set();
      assert(bs != NULL, "GC barriers invoked before BarrierSet is set");
      switch (bs->kind()) {
#define BARRIER_SET_RESOLVE_BARRIER_CLOSURE(bs_name)                    \
        case BarrierSet::bs_name: {                                     \
          return PostRuntimeDispatch<typename BarrierSet::GetType<BarrierSet::bs_name>::type:: \
            AccessBarrier<ds>, barrier_type, ds>::oop_access_barrier; \
        }                                                               \
        break;
        FOR_EACH_CONCRETE_BARRIER_SET_DO(BARRIER_SET_RESOLVE_BARRIER_CLOSURE)
#undef BARRIER_SET_RESOLVE_BARRIER_CLOSURE

      default:
        fatal("BarrierSet AccessBarrier resolving not implemented");
        return NULL;
      };
    }

    template <DecoratorSet ds>
    static typename EnableIf<
      !HasDecorator<ds, INTERNAL_VALUE_IS_OOP>::value,
      FunctionPointerT>::type
    resolve_barrier_gc() {
      BarrierSet* bs = BarrierSet::barrier_set();
      assert(bs != NULL, "GC barriers invoked before BarrierSet is set");
      switch (bs->kind()) {
#define BARRIER_SET_RESOLVE_BARRIER_CLOSURE(bs_name)                    \
        case BarrierSet::bs_name: {                                       \
          return PostRuntimeDispatch<typename BarrierSet::GetType<BarrierSet::bs_name>::type:: \
            AccessBarrier<ds>, barrier_type, ds>::access_barrier; \
        }                                                                 \
        break;
        FOR_EACH_CONCRETE_BARRIER_SET_DO(BARRIER_SET_RESOLVE_BARRIER_CLOSURE)
#undef BARRIER_SET_RESOLVE_BARRIER_CLOSURE

      default:
        fatal("BarrierSet AccessBarrier resolving not implemented");
        return NULL;
      };
    }

    static FunctionPointerT resolve_barrier_rt() {
      if (UseCompressedOops) {
        const DecoratorSet expanded_decorators = decorators | INTERNAL_RT_USE_COMPRESSED_OOPS;
        return resolve_barrier_gc<expanded_decorators>();
      } else {
        return resolve_barrier_gc<decorators>();
      }
    }

    static FunctionPointerT resolve_barrier() {
      return resolve_barrier_rt();
    }
  };

  // Step 5.a: Barrier resolution
  // The RuntimeDispatch class is responsible for performing a runtime dispatch of the
  // accessor. This is required when the access either depends on whether compressed oops
  // is being used, or it depends on which GC implementation was chosen (e.g. requires GC
  // barriers). The way it works is that a function pointer initially pointing to an
  // accessor resolution function gets called for each access. Upon first invocation,
  // it resolves which accessor to be used in future invocations and patches the
  // function pointer to this new accessor.

  template <DecoratorSet decorators, typename T>
  void RuntimeDispatch<decorators, T, BARRIER_STORE>::store_init(void* addr, T value) {
    func_t function = BarrierResolver<decorators, func_t, BARRIER_STORE>::resolve_barrier();
    _store_func = function;
    function(addr, value);
  }

  template <DecoratorSet decorators, typename T>
  void RuntimeDispatch<decorators, T, BARRIER_STORE_AT>::store_at_init(oop base, ptrdiff_t offset, T value) {
    func_t function = BarrierResolver<decorators, func_t, BARRIER_STORE_AT>::resolve_barrier();
    _store_at_func = function;
    function(base, offset, value);
  }

  template <DecoratorSet decorators, typename T>
  T RuntimeDispatch<decorators, T, BARRIER_LOAD>::load_init(void* addr) {
    func_t function = BarrierResolver<decorators, func_t, BARRIER_LOAD>::resolve_barrier();
    _load_func = function;
    return function(addr);
  }

  template <DecoratorSet decorators, typename T>
  T RuntimeDispatch<decorators, T, BARRIER_LOAD_AT>::load_at_init(oop base, ptrdiff_t offset) {
    func_t function = BarrierResolver<decorators, func_t, BARRIER_LOAD_AT>::resolve_barrier();
    _load_at_func = function;
    return function(base, offset);
  }

  template <DecoratorSet decorators, typename T>
  T RuntimeDispatch<decorators, T, BARRIER_ATOMIC_CMPXCHG>::atomic_cmpxchg_init(void* addr, T compare_value, T new_value) {
    func_t function = BarrierResolver<decorators, func_t, BARRIER_ATOMIC_CMPXCHG>::resolve_barrier();
    _atomic_cmpxchg_func = function;
    return function(addr, compare_value, new_value);
  }

  template <DecoratorSet decorators, typename T>
  T RuntimeDispatch<decorators, T, BARRIER_ATOMIC_CMPXCHG_AT>::atomic_cmpxchg_at_init(oop base, ptrdiff_t offset, T compare_value, T new_value) {
    func_t function = BarrierResolver<decorators, func_t, BARRIER_ATOMIC_CMPXCHG_AT>::resolve_barrier();
    _atomic_cmpxchg_at_func = function;
    return function(base, offset, compare_value, new_value);
  }

  template <DecoratorSet decorators, typename T>
  T RuntimeDispatch<decorators, T, BARRIER_ATOMIC_XCHG>::atomic_xchg_init(void* addr, T new_value) {
    func_t function = BarrierResolver<decorators, func_t, BARRIER_ATOMIC_XCHG>::resolve_barrier();
    _atomic_xchg_func = function;
    return function(addr, new_value);
  }

  template <DecoratorSet decorators, typename T>
  T RuntimeDispatch<decorators, T, BARRIER_ATOMIC_XCHG_AT>::atomic_xchg_at_init(oop base, ptrdiff_t offset, T new_value) {
    func_t function = BarrierResolver<decorators, func_t, BARRIER_ATOMIC_XCHG_AT>::resolve_barrier();
    _atomic_xchg_at_func = function;
    return function(base, offset, new_value);
  }

  template <DecoratorSet decorators, typename T>
  bool RuntimeDispatch<decorators, T, BARRIER_ARRAYCOPY>::arraycopy_init(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                                                                         arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                                                                         size_t length) {
    func_t function = BarrierResolver<decorators, func_t, BARRIER_ARRAYCOPY>::resolve_barrier();
    _arraycopy_func = function;
    return function(src_obj, src_offset_in_bytes, src_raw,
                    dst_obj, dst_offset_in_bytes, dst_raw,
                    length);
  }

  template <DecoratorSet decorators, typename T>
  void RuntimeDispatch<decorators, T, BARRIER_CLONE>::clone_init(oop src, oop dst, size_t size) {
    func_t function = BarrierResolver<decorators, func_t, BARRIER_CLONE>::resolve_barrier();
    _clone_func = function;
    function(src, dst, size);
  }

  template <DecoratorSet decorators, typename T>
  oop RuntimeDispatch<decorators, T, BARRIER_RESOLVE>::resolve_init(oop obj) {
    func_t function = BarrierResolver<decorators, func_t, BARRIER_RESOLVE>::resolve_barrier();
    _resolve_func = function;
    return function(obj);
  }
}

#endif // SHARE_OOPS_ACCESS_INLINE_HPP
