/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_ATOMIC_HPP
#define SHARE_RUNTIME_ATOMIC_HPP

#include "memory/allocation.hpp"
#include "metaprogramming/conditional.hpp"
#include "metaprogramming/enableIf.hpp"
#include "metaprogramming/isIntegral.hpp"
#include "metaprogramming/isPointer.hpp"
#include "metaprogramming/isSame.hpp"
#include "metaprogramming/primitiveConversions.hpp"
#include "metaprogramming/removeCV.hpp"
#include "metaprogramming/removePointer.hpp"
#include "runtime/orderAccess.hpp"
#include "utilities/align.hpp"
#include "utilities/bytes.hpp"
#include "utilities/macros.hpp"
#include <type_traits>

enum atomic_memory_order {
  // The modes that align with C++11 are intended to
  // follow the same semantics.
  memory_order_relaxed = 0,
  memory_order_acquire = 2,
  memory_order_release = 3,
  memory_order_acq_rel = 4,
  memory_order_seq_cst = 5,
  // Strong two-way memory barrier.
  memory_order_conservative = 8
};

enum ScopedFenceType {
    X_ACQUIRE
  , RELEASE_X
  , RELEASE_X_FENCE
};

class Atomic : AllStatic {
public:
  // Atomic operations on int64 types are not available on all 32-bit
  // platforms. If atomic ops on int64 are defined here they must only
  // be used from code that verifies they are available at runtime and
  // can provide an alternative action if not - see supports_cx8() for
  // a means to test availability.

  // The memory operations that are mentioned with each of the atomic
  // function families come from src/share/vm/runtime/orderAccess.hpp,
  // e.g., <fence> is described in that file and is implemented by the
  // OrderAccess::fence() function. See that file for the gory details
  // on the Memory Access Ordering Model.

  // All of the atomic operations that imply a read-modify-write action
  // guarantee a two-way memory barrier across that operation. Historically
  // these semantics reflect the strength of atomic operations that are
  // provided on SPARC/X86. We assume that strength is necessary unless
  // we can prove that a weaker form is sufficiently safe.

  // Atomically store to a location
  // The type T must be either a pointer type convertible to or equal
  // to D, an integral/enum type equal to D, or a type equal to D that
  // is primitive convertible using PrimitiveConversions.
  template<typename D, typename T>
  inline static void store(volatile D* dest, T store_value);

  template <typename D, typename T>
  inline static void release_store(volatile D* dest, T store_value);

  template <typename D, typename T>
  inline static void release_store_fence(volatile D* dest, T store_value);

  // Atomically load from a location
  // The type T must be either a pointer type, an integral/enum type,
  // or a type that is primitive convertible using PrimitiveConversions.
  template<typename T>
  inline static T load(const volatile T* dest);

  template <typename T>
  inline static T load_acquire(const volatile T* dest);

  // Atomically add to a location. *add*() provide:
  // <fence> add-value-to-dest <membar StoreLoad|StoreStore>

  // Returns updated value.
  template<typename D, typename I>
  inline static D add(D volatile* dest, I add_value,
                      atomic_memory_order order = memory_order_conservative);

  // Returns previous value.
  template<typename D, typename I>
  inline static D fetch_and_add(D volatile* dest, I add_value,
                                atomic_memory_order order = memory_order_conservative);

  template<typename D, typename I>
  inline static D sub(D volatile* dest, I sub_value,
                      atomic_memory_order order = memory_order_conservative);

  // Atomically increment location. inc() provide:
  // <fence> increment-dest <membar StoreLoad|StoreStore>
  // The type D may be either a pointer type, or an integral
  // type. If it is a pointer type, then the increment is
  // scaled to the size of the type pointed to by the pointer.
  template<typename D>
  inline static void inc(D volatile* dest,
                         atomic_memory_order order = memory_order_conservative);

  // Atomically decrement a location. dec() provide:
  // <fence> decrement-dest <membar StoreLoad|StoreStore>
  // The type D may be either a pointer type, or an integral
  // type. If it is a pointer type, then the decrement is
  // scaled to the size of the type pointed to by the pointer.
  template<typename D>
  inline static void dec(D volatile* dest,
                         atomic_memory_order order = memory_order_conservative);

  // Performs atomic exchange of *dest with exchange_value. Returns old
  // prior value of *dest. xchg*() provide:
  // <fence> exchange-value-with-dest <membar StoreLoad|StoreStore>
  // The type T must be either a pointer type convertible to or equal
  // to D, an integral/enum type equal to D, or a type equal to D that
  // is primitive convertible using PrimitiveConversions.
  template<typename D, typename T>
  inline static D xchg(volatile D* dest, T exchange_value,
                       atomic_memory_order order = memory_order_conservative);

  // Performs atomic compare of *dest and compare_value, and exchanges
  // *dest with exchange_value if the comparison succeeded. Returns prior
  // value of *dest. cmpxchg*() provide:
  // <fence> compare-and-exchange <membar StoreLoad|StoreStore>

  template<typename D, typename U, typename T>
  inline static D cmpxchg(D volatile* dest,
                          U compare_value,
                          T exchange_value,
                          atomic_memory_order order = memory_order_conservative);

  // Performs atomic compare of *dest and NULL, and replaces *dest
  // with exchange_value if the comparison succeeded.  Returns true if
  // the comparison succeeded and the exchange occurred.  This is
  // often used as part of lazy initialization, as a lock-free
  // alternative to the Double-Checked Locking Pattern.
  template<typename D, typename T>
  inline static bool replace_if_null(D* volatile* dest, T* value,
                                     atomic_memory_order order = memory_order_conservative);

private:
WINDOWS_ONLY(public:) // VS2017 warns (C2027) use of undefined type if IsPointerConvertible is declared private
  // Test whether From is implicitly convertible to To.
  // From and To must be pointer types.
  // Note: Provides the limited subset of C++11 std::is_convertible
  // that is needed here.
  template<typename From, typename To> struct IsPointerConvertible;

protected:
  // Dispatch handler for store.  Provides type-based validity
  // checking and limited conversions around calls to the platform-
  // specific implementation layer provided by PlatformOp.
  template<typename D, typename T, typename PlatformOp, typename Enable = void>
  struct StoreImpl;

  // Platform-specific implementation of store.  Support for sizes
  // of 1, 2, 4, and (if different) pointer size bytes are required.
  // The class is a function object that must be default constructable,
  // with these requirements:
  //
  // either:
  // - dest is of type D*, an integral, enum or pointer type.
  // - new_value are of type T, an integral, enum or pointer type D or
  //   pointer type convertible to D.
  // or:
  // - T and D are the same and are primitive convertible using PrimitiveConversions
  // and either way:
  // - platform_store is an object of type PlatformStore<sizeof(T)>.
  //
  // Then
  //   platform_store(new_value, dest)
  // must be a valid expression.
  //
  // The default implementation is a volatile store. If a platform
  // requires more for e.g. 64 bit stores, a specialization is required
  template<size_t byte_size> struct PlatformStore;

  // Dispatch handler for load.  Provides type-based validity
  // checking and limited conversions around calls to the platform-
  // specific implementation layer provided by PlatformOp.
  template<typename T, typename PlatformOp, typename Enable = void>
  struct LoadImpl;

  // Platform-specific implementation of load. Support for sizes of
  // 1, 2, 4 bytes and (if different) pointer size bytes are required.
  // The class is a function object that must be default
  // constructable, with these requirements:
  //
  // - dest is of type T*, an integral, enum or pointer type, or
  //   T is convertible to a primitive type using PrimitiveConversions
  // - platform_load is an object of type PlatformLoad<sizeof(T)>.
  //
  // Then
  //   platform_load(src)
  // must be a valid expression, returning a result convertible to T.
  //
  // The default implementation is a volatile load. If a platform
  // requires more for e.g. 64 bit loads, a specialization is required
  template<size_t byte_size> struct PlatformLoad;

  // Give platforms a variation point to specialize.
  template<size_t byte_size, ScopedFenceType type> struct PlatformOrderedStore;
  template<size_t byte_size, ScopedFenceType type> struct PlatformOrderedLoad;

private:
  // Dispatch handler for add.  Provides type-based validity checking
  // and limited conversions around calls to the platform-specific
  // implementation layer provided by PlatformAdd.
  template<typename D, typename I, typename Enable = void>
  struct AddImpl;

  // Platform-specific implementation of add.  Support for sizes of 4
  // bytes and (if different) pointer size bytes are required.  The
  // class must be default constructable, with these requirements:
  //
  // - dest is of type D*, an integral or pointer type.
  // - add_value is of type I, an integral type.
  // - sizeof(I) == sizeof(D).
  // - if D is an integral type, I == D.
  // - order is of type atomic_memory_order.
  // - platform_add is an object of type PlatformAdd<sizeof(D)>.
  //
  // Then both
  //   platform_add.add_and_fetch(dest, add_value, order)
  //   platform_add.fetch_and_add(dest, add_value, order)
  // must be valid expressions returning a result convertible to D.
  //
  // add_and_fetch atomically adds add_value to the value of dest,
  // returning the new value.
  //
  // fetch_and_add atomically adds add_value to the value of dest,
  // returning the old value.
  //
  // When D is a pointer type P*, both add_and_fetch and fetch_and_add
  // treat it as if it were an uintptr_t; they do not perform any
  // scaling of add_value, as that has already been done by the caller.
  //
  // No definition is provided; all platforms must explicitly define
  // this class and any needed specializations.
  template<size_t byte_size> struct PlatformAdd;

  // Support for platforms that implement some variants of add using a
  // (typically out of line) non-template helper function.  The
  // generic arguments passed to PlatformAdd need to be translated to
  // the appropriate type for the helper function, the helper function
  // invoked on the translated arguments, and the result translated
  // back.  Type is the parameter / return type of the helper
  // function.  No scaling of add_value is performed when D is a pointer
  // type, so this function can be used to implement the support function
  // required by AddAndFetch.
  template<typename Type, typename Fn, typename D, typename I>
  static D add_using_helper(Fn fn, D volatile* dest, I add_value);

  // Dispatch handler for cmpxchg.  Provides type-based validity
  // checking and limited conversions around calls to the
  // platform-specific implementation layer provided by
  // PlatformCmpxchg.
  template<typename D, typename U, typename T, typename Enable = void>
  struct CmpxchgImpl;

  // Platform-specific implementation of cmpxchg.  Support for sizes
  // of 1, 4, and 8 are required.  The class is a function object that
  // must be default constructable, with these requirements:
  //
  // - dest is of type T*.
  // - exchange_value and compare_value are of type T.
  // - order is of type atomic_memory_order.
  // - platform_cmpxchg is an object of type PlatformCmpxchg<sizeof(T)>.
  //
  // Then
  //   platform_cmpxchg(dest, compare_value, exchange_value, order)
  // must be a valid expression, returning a result convertible to T.
  //
  // A default definition is provided, which declares a function template
  //   T operator()(T volatile*, T, T, atomic_memory_order) const
  //
  // For each required size, a platform must either provide an
  // appropriate definition of that function, or must entirely
  // specialize the class template for that size.
  template<size_t byte_size> struct PlatformCmpxchg;

  // Support for platforms that implement some variants of cmpxchg
  // using a (typically out of line) non-template helper function.
  // The generic arguments passed to PlatformCmpxchg need to be
  // translated to the appropriate type for the helper function, the
  // helper invoked on the translated arguments, and the result
  // translated back.  Type is the parameter / return type of the
  // helper function.
  template<typename Type, typename Fn, typename T>
  static T cmpxchg_using_helper(Fn fn,
                                T volatile* dest,
                                T compare_value,
                                T exchange_value);

  // Support platforms that do not provide Read-Modify-Write
  // byte-level atomic access. To use, derive PlatformCmpxchg<1> from
  // this class.
public: // Temporary, can't be private: C++03 11.4/2. Fixed by C++11.
  struct CmpxchgByteUsingInt;
private:

  // Dispatch handler for xchg.  Provides type-based validity
  // checking and limited conversions around calls to the
  // platform-specific implementation layer provided by
  // PlatformXchg.
  template<typename D, typename T, typename Enable = void>
  struct XchgImpl;

  // Platform-specific implementation of xchg.  Support for sizes
  // of 4, and sizeof(intptr_t) are required.  The class is a function
  // object that must be default constructable, with these requirements:
  //
  // - dest is of type T*.
  // - exchange_value is of type T.
  // - platform_xchg is an object of type PlatformXchg<sizeof(T)>.
  //
  // Then
  //   platform_xchg(dest, exchange_value)
  // must be a valid expression, returning a result convertible to T.
  //
  // A default definition is provided, which declares a function template
  //   T operator()(T volatile*, T, atomic_memory_order) const
  //
  // For each required size, a platform must either provide an
  // appropriate definition of that function, or must entirely
  // specialize the class template for that size.
  template<size_t byte_size> struct PlatformXchg;

  // Support for platforms that implement some variants of xchg
  // using a (typically out of line) non-template helper function.
  // The generic arguments passed to PlatformXchg need to be
  // translated to the appropriate type for the helper function, the
  // helper invoked on the translated arguments, and the result
  // translated back.  Type is the parameter / return type of the
  // helper function.
  template<typename Type, typename Fn, typename T>
  static T xchg_using_helper(Fn fn,
                             T volatile* dest,
                             T exchange_value);
};

template<typename From, typename To>
struct Atomic::IsPointerConvertible<From*, To*> : AllStatic {
  // Determine whether From* is implicitly convertible to To*, using
  // the "sizeof trick".
  typedef char yes;
  typedef char (&no)[2];

  static yes test(To*);
  static no test(...);
  static From* test_value;

  static const bool value = (sizeof(yes) == sizeof(test(test_value)));
};

// Handle load for pointer, integral and enum types.
template<typename T, typename PlatformOp>
struct Atomic::LoadImpl<
  T,
  PlatformOp,
  typename EnableIf<IsIntegral<T>::value || std::is_enum<T>::value || IsPointer<T>::value>::type>
{
  T operator()(T const volatile* dest) const {
    // Forward to the platform handler for the size of T.
    return PlatformOp()(dest);
  }
};

// Handle load for types that have a translator.
//
// All the involved types must be identical.
//
// This translates the original call into a call on the decayed
// arguments, and returns the recovered result of that translated
// call.
template<typename T, typename PlatformOp>
struct Atomic::LoadImpl<
  T,
  PlatformOp,
  typename EnableIf<PrimitiveConversions::Translate<T>::value>::type>
{
  T operator()(T const volatile* dest) const {
    typedef PrimitiveConversions::Translate<T> Translator;
    typedef typename Translator::Decayed Decayed;
    STATIC_ASSERT(sizeof(T) == sizeof(Decayed));
    Decayed result = PlatformOp()(reinterpret_cast<Decayed const volatile*>(dest));
    return Translator::recover(result);
  }
};

// Default implementation of atomic load if a specific platform
// does not provide a specialization for a certain size class.
// For increased safety, the default implementation only allows
// load types that are pointer sized or smaller. If a platform still
// supports wide atomics, then it has to use specialization
// of Atomic::PlatformLoad for that wider size class.
template<size_t byte_size>
struct Atomic::PlatformLoad {
  template<typename T>
  T operator()(T const volatile* dest) const {
    STATIC_ASSERT(sizeof(T) <= sizeof(void*)); // wide atomics need specialization
    return *dest;
  }
};

// Handle store for integral and enum types.
//
// All the involved types must be identical.
template<typename T, typename PlatformOp>
struct Atomic::StoreImpl<
  T, T,
  PlatformOp,
  typename EnableIf<IsIntegral<T>::value || std::is_enum<T>::value>::type>
{
  void operator()(T volatile* dest, T new_value) const {
    // Forward to the platform handler for the size of T.
    PlatformOp()(dest, new_value);
  }
};

// Handle store for pointer types.
//
// The new_value must be implicitly convertible to the
// destination's type; it must be type-correct to store the
// new_value in the destination.
template<typename D, typename T, typename PlatformOp>
struct Atomic::StoreImpl<
  D*, T*,
  PlatformOp,
  typename EnableIf<Atomic::IsPointerConvertible<T*, D*>::value>::type>
{
  void operator()(D* volatile* dest, T* new_value) const {
    // Allow derived to base conversion, and adding cv-qualifiers.
    D* value = new_value;
    PlatformOp()(dest, value);
  }
};

// Handle store for types that have a translator.
//
// All the involved types must be identical.
//
// This translates the original call into a call on the decayed
// arguments.
template<typename T, typename PlatformOp>
struct Atomic::StoreImpl<
  T, T,
  PlatformOp,
  typename EnableIf<PrimitiveConversions::Translate<T>::value>::type>
{
  void operator()(T volatile* dest, T new_value) const {
    typedef PrimitiveConversions::Translate<T> Translator;
    typedef typename Translator::Decayed Decayed;
    STATIC_ASSERT(sizeof(T) == sizeof(Decayed));
    PlatformOp()(reinterpret_cast<Decayed volatile*>(dest),
                 Translator::decay(new_value));
  }
};

// Default implementation of atomic store if a specific platform
// does not provide a specialization for a certain size class.
// For increased safety, the default implementation only allows
// storing types that are pointer sized or smaller. If a platform still
// supports wide atomics, then it has to use specialization
// of Atomic::PlatformStore for that wider size class.
template<size_t byte_size>
struct Atomic::PlatformStore {
  template<typename T>
  void operator()(T volatile* dest,
                  T new_value) const {
    STATIC_ASSERT(sizeof(T) <= sizeof(void*)); // wide atomics need specialization
    (void)const_cast<T&>(*dest = new_value);
  }
};

template<typename D>
inline void Atomic::inc(D volatile* dest, atomic_memory_order order) {
  STATIC_ASSERT(IsPointer<D>::value || IsIntegral<D>::value);
  typedef typename Conditional<IsPointer<D>::value, ptrdiff_t, D>::type I;
  Atomic::add(dest, I(1), order);
}

template<typename D>
inline void Atomic::dec(D volatile* dest, atomic_memory_order order) {
  STATIC_ASSERT(IsPointer<D>::value || IsIntegral<D>::value);
  typedef typename Conditional<IsPointer<D>::value, ptrdiff_t, D>::type I;
  // Assumes two's complement integer representation.
  #pragma warning(suppress: 4146)
  Atomic::add(dest, I(-1), order);
}

template<typename D, typename I>
inline D Atomic::sub(D volatile* dest, I sub_value, atomic_memory_order order) {
  STATIC_ASSERT(IsPointer<D>::value || IsIntegral<D>::value);
  STATIC_ASSERT(IsIntegral<I>::value);
  // If D is a pointer type, use [u]intptr_t as the addend type,
  // matching signedness of I.  Otherwise, use D as the addend type.
  typedef typename Conditional<IsSigned<I>::value, intptr_t, uintptr_t>::type PI;
  typedef typename Conditional<IsPointer<D>::value, PI, D>::type AddendType;
  // Only allow conversions that can't change the value.
  STATIC_ASSERT(IsSigned<I>::value == IsSigned<AddendType>::value);
  STATIC_ASSERT(sizeof(I) <= sizeof(AddendType));
  AddendType addend = sub_value;
  // Assumes two's complement integer representation.
  #pragma warning(suppress: 4146) // In case AddendType is not signed.
  return Atomic::add(dest, -addend, order);
}

// Define the class before including platform file, which may specialize
// the operator definition.  No generic definition of specializations
// of the operator template are provided, nor are there any generic
// specializations of the class.  The platform file is responsible for
// providing those.
template<size_t byte_size>
struct Atomic::PlatformCmpxchg {
  template<typename T>
  T operator()(T volatile* dest,
               T compare_value,
               T exchange_value,
               atomic_memory_order order) const;
};

// Define the class before including platform file, which may use this
// as a base class, requiring it be complete.  The definition is later
// in this file, near the other definitions related to cmpxchg.
struct Atomic::CmpxchgByteUsingInt {
  static uint8_t get_byte_in_int(uint32_t n, uint32_t idx);
  static uint32_t set_byte_in_int(uint32_t n, uint8_t b, uint32_t idx);
  template<typename T>
  T operator()(T volatile* dest,
               T compare_value,
               T exchange_value,
               atomic_memory_order order) const;
};

// Define the class before including platform file, which may specialize
// the operator definition.  No generic definition of specializations
// of the operator template are provided, nor are there any generic
// specializations of the class.  The platform file is responsible for
// providing those.
template<size_t byte_size>
struct Atomic::PlatformXchg {
  template<typename T>
  T operator()(T volatile* dest,
               T exchange_value,
               atomic_memory_order order) const;
};

template <ScopedFenceType T>
class ScopedFenceGeneral: public StackObj {
 public:
  void prefix() {}
  void postfix() {}
};

// The following methods can be specialized using simple template specialization
// in the platform specific files for optimization purposes. Otherwise the
// generalized variant is used.

template<> inline void ScopedFenceGeneral<X_ACQUIRE>::postfix()       { OrderAccess::acquire(); }
template<> inline void ScopedFenceGeneral<RELEASE_X>::prefix()        { OrderAccess::release(); }
template<> inline void ScopedFenceGeneral<RELEASE_X_FENCE>::prefix()  { OrderAccess::release(); }
template<> inline void ScopedFenceGeneral<RELEASE_X_FENCE>::postfix() { OrderAccess::fence();   }

template <ScopedFenceType T>
class ScopedFence : public ScopedFenceGeneral<T> {
  void *const _field;
 public:
  ScopedFence(void *const field) : _field(field) { prefix(); }
  ~ScopedFence() { postfix(); }
  void prefix() { ScopedFenceGeneral<T>::prefix(); }
  void postfix() { ScopedFenceGeneral<T>::postfix(); }
};

// platform specific in-line definitions - must come before shared definitions

#include OS_CPU_HEADER(atomic)

// shared in-line definitions

// size_t casts...
#if (SIZE_MAX != UINTPTR_MAX)
#error size_t is not WORD_SIZE, interesting platform, but missing implementation here
#endif

template<typename T>
inline T Atomic::load(const volatile T* dest) {
  return LoadImpl<T, PlatformLoad<sizeof(T)> >()(dest);
}

template<size_t byte_size, ScopedFenceType type>
struct Atomic::PlatformOrderedLoad {
  template <typename T>
  T operator()(const volatile T* p) const {
    ScopedFence<type> f((void*)p);
    return Atomic::load(p);
  }
};

template <typename T>
inline T Atomic::load_acquire(const volatile T* p) {
  return LoadImpl<T, PlatformOrderedLoad<sizeof(T), X_ACQUIRE> >()(p);
}

template<typename D, typename T>
inline void Atomic::store(volatile D* dest, T store_value) {
  StoreImpl<D, T, PlatformStore<sizeof(D)> >()(dest, store_value);
}

template<size_t byte_size, ScopedFenceType type>
struct Atomic::PlatformOrderedStore {
  template <typename T>
  void operator()(volatile T* p, T v) const {
    ScopedFence<type> f((void*)p);
    Atomic::store(p, v);
  }
};

template <typename D, typename T>
inline void Atomic::release_store(volatile D* p, T v) {
  StoreImpl<D, T, PlatformOrderedStore<sizeof(D), RELEASE_X> >()(p, v);
}

template <typename D, typename T>
inline void Atomic::release_store_fence(volatile D* p, T v) {
  StoreImpl<D, T, PlatformOrderedStore<sizeof(D), RELEASE_X_FENCE> >()(p, v);
}

template<typename D, typename I>
inline D Atomic::add(D volatile* dest, I add_value,
                     atomic_memory_order order) {
  return AddImpl<D, I>::add_and_fetch(dest, add_value, order);
}

template<typename D, typename I>
inline D Atomic::fetch_and_add(D volatile* dest, I add_value,
                               atomic_memory_order order) {
  return AddImpl<D, I>::fetch_and_add(dest, add_value, order);
}

template<typename D, typename I>
struct Atomic::AddImpl<
  D, I,
  typename EnableIf<IsIntegral<I>::value &&
                    IsIntegral<D>::value &&
                    (sizeof(I) <= sizeof(D)) &&
                    (IsSigned<I>::value == IsSigned<D>::value)>::type>
{
  static D add_and_fetch(D volatile* dest, I add_value, atomic_memory_order order) {
    D addend = add_value;
    return PlatformAdd<sizeof(D)>().add_and_fetch(dest, addend, order);
  }
  static D fetch_and_add(D volatile* dest, I add_value, atomic_memory_order order) {
    D addend = add_value;
    return PlatformAdd<sizeof(D)>().fetch_and_add(dest, addend, order);
  }
};

template<typename P, typename I>
struct Atomic::AddImpl<
  P*, I,
  typename EnableIf<IsIntegral<I>::value && (sizeof(I) <= sizeof(P*))>::type>
{
  STATIC_ASSERT(sizeof(intptr_t) == sizeof(P*));
  STATIC_ASSERT(sizeof(uintptr_t) == sizeof(P*));
  typedef typename Conditional<IsSigned<I>::value,
                               intptr_t,
                               uintptr_t>::type CI;

  static CI scale_addend(CI add_value) {
    return add_value * sizeof(P);
  }

  static P* add_and_fetch(P* volatile* dest, I add_value, atomic_memory_order order) {
    CI addend = add_value;
    return PlatformAdd<sizeof(P*)>().add_and_fetch(dest, scale_addend(addend), order);
  }
  static P* fetch_and_add(P* volatile* dest, I add_value, atomic_memory_order order) {
    CI addend = add_value;
    return PlatformAdd<sizeof(P*)>().fetch_and_add(dest, scale_addend(addend), order);
  }
};

template<typename Type, typename Fn, typename D, typename I>
inline D Atomic::add_using_helper(Fn fn, D volatile* dest, I add_value) {
  return PrimitiveConversions::cast<D>(
    fn(PrimitiveConversions::cast<Type>(add_value),
       reinterpret_cast<Type volatile*>(dest)));
}

template<typename D, typename U, typename T>
inline D Atomic::cmpxchg(D volatile* dest,
                         U compare_value,
                         T exchange_value,
                         atomic_memory_order order) {
  return CmpxchgImpl<D, U, T>()(dest, compare_value, exchange_value, order);
}

template<typename D, typename T>
inline bool Atomic::replace_if_null(D* volatile* dest, T* value,
                                    atomic_memory_order order) {
  // Presently using a trivial implementation in terms of cmpxchg.
  // Consider adding platform support, to permit the use of compiler
  // intrinsics like gcc's __sync_bool_compare_and_swap.
  D* expected_null = NULL;
  return expected_null == cmpxchg(dest, expected_null, value, order);
}

// Handle cmpxchg for integral and enum types.
//
// All the involved types must be identical.
template<typename T>
struct Atomic::CmpxchgImpl<
  T, T, T,
  typename EnableIf<IsIntegral<T>::value || std::is_enum<T>::value>::type>
{
  T operator()(T volatile* dest, T compare_value, T exchange_value,
               atomic_memory_order order) const {
    // Forward to the platform handler for the size of T.
    return PlatformCmpxchg<sizeof(T)>()(dest,
                                        compare_value,
                                        exchange_value,
                                        order);
  }
};

// Handle cmpxchg for pointer types.
//
// The destination's type and the compare_value type must be the same,
// ignoring cv-qualifiers; we don't care about the cv-qualifiers of
// the compare_value.
//
// The exchange_value must be implicitly convertible to the
// destination's type; it must be type-correct to store the
// exchange_value in the destination.
template<typename D, typename U, typename T>
struct Atomic::CmpxchgImpl<
  D*, U*, T*,
  typename EnableIf<Atomic::IsPointerConvertible<T*, D*>::value &&
                    IsSame<typename RemoveCV<D>::type,
                           typename RemoveCV<U>::type>::value>::type>
{
  D* operator()(D* volatile* dest, U* compare_value, T* exchange_value,
               atomic_memory_order order) const {
    // Allow derived to base conversion, and adding cv-qualifiers.
    D* new_value = exchange_value;
    // Don't care what the CV qualifiers for compare_value are,
    // but we need to match D* when calling platform support.
    D* old_value = const_cast<D*>(compare_value);
    return PlatformCmpxchg<sizeof(D*)>()(dest, old_value, new_value, order);
  }
};

// Handle cmpxchg for types that have a translator.
//
// All the involved types must be identical.
//
// This translates the original call into a call on the decayed
// arguments, and returns the recovered result of that translated
// call.
template<typename T>
struct Atomic::CmpxchgImpl<
  T, T, T,
  typename EnableIf<PrimitiveConversions::Translate<T>::value>::type>
{
  T operator()(T volatile* dest, T compare_value, T exchange_value,
               atomic_memory_order order) const {
    typedef PrimitiveConversions::Translate<T> Translator;
    typedef typename Translator::Decayed Decayed;
    STATIC_ASSERT(sizeof(T) == sizeof(Decayed));
    return Translator::recover(
      cmpxchg(reinterpret_cast<Decayed volatile*>(dest),
              Translator::decay(compare_value),
              Translator::decay(exchange_value),
              order));
  }
};

template<typename Type, typename Fn, typename T>
inline T Atomic::cmpxchg_using_helper(Fn fn,
                                      T volatile* dest,
                                      T compare_value,
                                      T exchange_value) {
  STATIC_ASSERT(sizeof(Type) == sizeof(T));
  return PrimitiveConversions::cast<T>(
    fn(PrimitiveConversions::cast<Type>(exchange_value),
       reinterpret_cast<Type volatile*>(dest),
       PrimitiveConversions::cast<Type>(compare_value)));
}

inline uint32_t Atomic::CmpxchgByteUsingInt::set_byte_in_int(uint32_t n,
                                                             uint8_t b,
                                                             uint32_t idx) {
  int bitsIdx = BitsPerByte * idx;
  return (n & ~(static_cast<uint32_t>(0xff) << bitsIdx))
          | (static_cast<uint32_t>(b) << bitsIdx);
}

inline uint8_t Atomic::CmpxchgByteUsingInt::get_byte_in_int(uint32_t n,
                                                            uint32_t idx) {
  int bitsIdx = BitsPerByte * idx;
  return (uint8_t)(n >> bitsIdx);
}

template<typename T>
inline T Atomic::CmpxchgByteUsingInt::operator()(T volatile* dest,
                                                 T compare_value,
                                                 T exchange_value,
                                                 atomic_memory_order order) const {
  STATIC_ASSERT(sizeof(T) == sizeof(uint8_t));
  uint8_t canon_exchange_value = exchange_value;
  uint8_t canon_compare_value = compare_value;
  volatile uint32_t* aligned_dest
    = reinterpret_cast<volatile uint32_t*>(align_down(dest, sizeof(uint32_t)));
  size_t offset = pointer_delta(dest, aligned_dest, 1);

  uint32_t idx = (Endian::NATIVE == Endian::BIG)
                   ? (sizeof(uint32_t) - 1 - offset)
                   : offset;

  // current value may not be what we are looking for, so force it
  // to that value so the initial cmpxchg will fail if it is different
  uint32_t cur = set_byte_in_int(Atomic::load(aligned_dest), canon_compare_value, idx);

  // always execute a real cmpxchg so that we get the required memory
  // barriers even on initial failure
  do {
    // value to swap in matches current value
    // except for the one byte we want to update
    uint32_t new_value = set_byte_in_int(cur, canon_exchange_value, idx);

    uint32_t res = cmpxchg(aligned_dest, cur, new_value, order);
    if (res == cur) break;      // success

    // at least one byte in the int changed value, so update
    // our view of the current int
    cur = res;
    // if our byte is still as cur we loop and try again
  } while (get_byte_in_int(cur, idx) == canon_compare_value);

  return PrimitiveConversions::cast<T>(get_byte_in_int(cur, idx));
}

// Handle xchg for integral and enum types.
//
// All the involved types must be identical.
template<typename T>
struct Atomic::XchgImpl<
  T, T,
  typename EnableIf<IsIntegral<T>::value || std::is_enum<T>::value>::type>
{
  T operator()(T volatile* dest, T exchange_value, atomic_memory_order order) const {
    // Forward to the platform handler for the size of T.
    return PlatformXchg<sizeof(T)>()(dest, exchange_value, order);
  }
};

// Handle xchg for pointer types.
//
// The exchange_value must be implicitly convertible to the
// destination's type; it must be type-correct to store the
// exchange_value in the destination.
template<typename D, typename T>
struct Atomic::XchgImpl<
  D*, T*,
  typename EnableIf<Atomic::IsPointerConvertible<T*, D*>::value>::type>
{
  D* operator()(D* volatile* dest, T* exchange_value, atomic_memory_order order) const {
    // Allow derived to base conversion, and adding cv-qualifiers.
    D* new_value = exchange_value;
    return PlatformXchg<sizeof(D*)>()(dest, new_value, order);
  }
};

// Handle xchg for types that have a translator.
//
// All the involved types must be identical.
//
// This translates the original call into a call on the decayed
// arguments, and returns the recovered result of that translated
// call.
template<typename T>
struct Atomic::XchgImpl<
  T, T,
  typename EnableIf<PrimitiveConversions::Translate<T>::value>::type>
{
  T operator()(T volatile* dest, T exchange_value, atomic_memory_order order) const {
    typedef PrimitiveConversions::Translate<T> Translator;
    typedef typename Translator::Decayed Decayed;
    STATIC_ASSERT(sizeof(T) == sizeof(Decayed));
    return Translator::recover(
      xchg(reinterpret_cast<Decayed volatile*>(dest),
           Translator::decay(exchange_value),
           order));
  }
};

template<typename Type, typename Fn, typename T>
inline T Atomic::xchg_using_helper(Fn fn,
                                   T volatile* dest,
                                   T exchange_value) {
  STATIC_ASSERT(sizeof(Type) == sizeof(T));
  // Notice the swapped order of arguments. Change when/if stubs are rewritten.
  return PrimitiveConversions::cast<T>(
    fn(PrimitiveConversions::cast<Type>(exchange_value),
       reinterpret_cast<Type volatile*>(dest)));
}

template<typename D, typename T>
inline D Atomic::xchg(volatile D* dest, T exchange_value, atomic_memory_order order) {
  return XchgImpl<D, T>()(dest, exchange_value, order);
}

#endif // SHARE_RUNTIME_ATOMIC_HPP
