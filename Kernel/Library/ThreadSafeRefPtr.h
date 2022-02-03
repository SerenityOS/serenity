/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/Error.h>
#include <AK/Format.h>
#include <AK/NonnullRefPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Traits.h>
#include <AK/Types.h>
#ifdef KERNEL
#    include <Kernel/Arch/Processor.h>
#    include <Kernel/Arch/ScopedCritical.h>
#endif

#define THREADSAFEREFPTR_SCRUB_BYTE 0xa0

namespace AK {

template<typename T>
class OwnPtr;

template<typename T>
struct RefPtrTraits {
    ALWAYS_INLINE static T* as_ptr(FlatPtr bits)
    {
        return (T*)(bits & ~(FlatPtr)1);
    }

    ALWAYS_INLINE static FlatPtr as_bits(T* ptr)
    {
        VERIFY(((FlatPtr)ptr & 1) == 0);
        return (FlatPtr)ptr;
    }

    template<typename U, typename PtrTraits>
    ALWAYS_INLINE static FlatPtr convert_from(FlatPtr bits)
    {
        if (PtrTraits::is_null(bits))
            return default_null_value;
        return as_bits(PtrTraits::as_ptr(bits));
    }

    ALWAYS_INLINE static bool is_null(FlatPtr bits)
    {
        return (bits & ~(FlatPtr)1) == 0;
    }

    ALWAYS_INLINE static FlatPtr exchange(Atomic<FlatPtr>& atomic_var, FlatPtr new_value)
    {
        // Only exchange when lock is not held
        VERIFY((new_value & 1) == 0);
        FlatPtr expected = atomic_var.load(AK::MemoryOrder::memory_order_relaxed);
        for (;;) {
            expected &= ~(FlatPtr)1; // only if lock bit is not set
            if (atomic_var.compare_exchange_strong(expected, new_value, AK::MemoryOrder::memory_order_acq_rel))
                break;
#ifdef KERNEL
            Kernel::Processor::wait_check();
#endif
        }
        return expected;
    }

    ALWAYS_INLINE static bool exchange_if_null(Atomic<FlatPtr>& atomic_var, FlatPtr new_value)
    {
        // Only exchange when lock is not held
        VERIFY((new_value & 1) == 0);
        for (;;) {
            FlatPtr expected = default_null_value; // only if lock bit is not set
            if (atomic_var.compare_exchange_strong(expected, new_value, AK::MemoryOrder::memory_order_acq_rel))
                break;
            if (!is_null(expected))
                return false;
#ifdef KERNEL
            Kernel::Processor::wait_check();
#endif
        }
        return true;
    }

    ALWAYS_INLINE static FlatPtr lock(Atomic<FlatPtr>& atomic_var)
    {
        // This sets the lock bit atomically, preventing further modifications.
        // This is important when e.g. copying a RefPtr where the source
        // might be released and freed too quickly. This allows us
        // to temporarily lock the pointer so we can add a reference, then
        // unlock it
        FlatPtr bits;
        for (;;) {
            bits = atomic_var.fetch_or(1, AK::MemoryOrder::memory_order_acq_rel);
            if ((bits & 1) == 0)
                break;
#ifdef KERNEL
            Kernel::Processor::wait_check();
#endif
        }
        VERIFY((bits & 1) == 0);
        return bits;
    }

    ALWAYS_INLINE static void unlock(Atomic<FlatPtr>& atomic_var, FlatPtr new_value)
    {
        VERIFY((new_value & 1) == 0);
        atomic_var.store(new_value, AK::MemoryOrder::memory_order_release);
    }

    static constexpr FlatPtr default_null_value = 0;

    using NullType = std::nullptr_t;
};

template<typename T, typename PtrTraits>
class [[nodiscard]] RefPtr {
    template<typename U, typename P>
    friend class RefPtr;
    template<typename U>
    friend class WeakPtr;

public:
    enum AdoptTag {
        Adopt
    };

    RefPtr() = default;
    RefPtr(const T* ptr)
        : m_bits(PtrTraits::as_bits(const_cast<T*>(ptr)))
    {
        ref_if_not_null(const_cast<T*>(ptr));
    }
    RefPtr(const T& object)
        : m_bits(PtrTraits::as_bits(const_cast<T*>(&object)))
    {
        T* ptr = const_cast<T*>(&object);
        VERIFY(ptr);
        VERIFY(!is_null());
        ptr->ref();
    }
    RefPtr(AdoptTag, T& object)
        : m_bits(PtrTraits::as_bits(&object))
    {
        VERIFY(!is_null());
    }
    RefPtr(RefPtr&& other)
        : m_bits(other.leak_ref_raw())
    {
    }
    ALWAYS_INLINE RefPtr(const NonnullRefPtr<T>& other)
        : m_bits(PtrTraits::as_bits(const_cast<T*>(other.add_ref())))
    {
    }
    template<typename U>
    ALWAYS_INLINE RefPtr(const NonnullRefPtr<U>& other) requires(IsConvertible<U*, T*>)
        : m_bits(PtrTraits::as_bits(const_cast<U*>(other.add_ref())))
    {
    }
    template<typename U>
    ALWAYS_INLINE RefPtr(NonnullRefPtr<U>&& other) requires(IsConvertible<U*, T*>)
        : m_bits(PtrTraits::as_bits(&other.leak_ref()))
    {
        VERIFY(!is_null());
    }
    template<typename U, typename P = RefPtrTraits<U>>
    RefPtr(RefPtr<U, P>&& other) requires(IsConvertible<U*, T*>)
        : m_bits(PtrTraits::template convert_from<U, P>(other.leak_ref_raw()))
    {
    }
    RefPtr(const RefPtr& other)
        : m_bits(other.add_ref_raw())
    {
    }
    template<typename U, typename P = RefPtrTraits<U>>
    RefPtr(const RefPtr<U, P>& other) requires(IsConvertible<U*, T*>)
        : m_bits(other.add_ref_raw())
    {
    }
    ALWAYS_INLINE ~RefPtr()
    {
        clear();
#ifdef SANITIZE_PTRS
        m_bits.store(explode_byte(THREADSAFEREFPTR_SCRUB_BYTE), AK::MemoryOrder::memory_order_relaxed);
#endif
    }

    template<typename U>
    RefPtr(const OwnPtr<U>&) = delete;
    template<typename U>
    RefPtr& operator=(const OwnPtr<U>&) = delete;

    void swap(RefPtr& other)
    {
        if (this == &other)
            return;

        // NOTE: swap is not atomic!
        FlatPtr other_bits = PtrTraits::exchange(other.m_bits, PtrTraits::default_null_value);
        FlatPtr bits = PtrTraits::exchange(m_bits, other_bits);
        PtrTraits::exchange(other.m_bits, bits);
    }

    template<typename U, typename P = RefPtrTraits<U>>
    void swap(RefPtr<U, P>& other) requires(IsConvertible<U*, T*>)
    {
        // NOTE: swap is not atomic!
        FlatPtr other_bits = P::exchange(other.m_bits, P::default_null_value);
        FlatPtr bits = PtrTraits::exchange(m_bits, PtrTraits::template convert_from<U, P>(other_bits));
        P::exchange(other.m_bits, P::template convert_from<U, P>(bits));
    }

    ALWAYS_INLINE RefPtr& operator=(RefPtr&& other)
    {
        if (this != &other)
            assign_raw(other.leak_ref_raw());
        return *this;
    }

    template<typename U, typename P = RefPtrTraits<U>>
    ALWAYS_INLINE RefPtr& operator=(RefPtr<U, P>&& other) requires(IsConvertible<U*, T*>)
    {
        assign_raw(PtrTraits::template convert_from<U, P>(other.leak_ref_raw()));
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE RefPtr& operator=(NonnullRefPtr<U>&& other) requires(IsConvertible<U*, T*>)
    {
        assign_raw(PtrTraits::as_bits(&other.leak_ref()));
        return *this;
    }

    ALWAYS_INLINE RefPtr& operator=(const NonnullRefPtr<T>& other)
    {
        assign_raw(PtrTraits::as_bits(other.add_ref()));
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE RefPtr& operator=(const NonnullRefPtr<U>& other) requires(IsConvertible<U*, T*>)
    {
        assign_raw(PtrTraits::as_bits(other.add_ref()));
        return *this;
    }

    ALWAYS_INLINE RefPtr& operator=(const RefPtr& other)
    {
        if (this != &other)
            assign_raw(other.add_ref_raw());
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE RefPtr& operator=(const RefPtr<U>& other) requires(IsConvertible<U*, T*>)
    {
        assign_raw(other.add_ref_raw());
        return *this;
    }

    ALWAYS_INLINE RefPtr& operator=(const T* ptr)
    {
        ref_if_not_null(const_cast<T*>(ptr));
        assign_raw(PtrTraits::as_bits(const_cast<T*>(ptr)));
        return *this;
    }

    ALWAYS_INLINE RefPtr& operator=(const T& object)
    {
        const_cast<T&>(object).ref();
        assign_raw(PtrTraits::as_bits(const_cast<T*>(&object)));
        return *this;
    }

    RefPtr& operator=(std::nullptr_t)
    {
        clear();
        return *this;
    }

    ALWAYS_INLINE bool assign_if_null(RefPtr&& other)
    {
        if (this == &other)
            return is_null();
        return PtrTraits::exchange_if_null(m_bits, other.leak_ref_raw());
    }

    template<typename U, typename P = RefPtrTraits<U>>
    ALWAYS_INLINE bool assign_if_null(RefPtr<U, P>&& other)
    {
        if (this == &other)
            return is_null();
        return PtrTraits::exchange_if_null(m_bits, PtrTraits::template convert_from<U, P>(other.leak_ref_raw()));
    }

    ALWAYS_INLINE void clear()
    {
        assign_raw(PtrTraits::default_null_value);
    }

    bool operator!() const { return PtrTraits::is_null(m_bits.load(AK::MemoryOrder::memory_order_relaxed)); }

    [[nodiscard]] T* leak_ref()
    {
        FlatPtr bits = PtrTraits::exchange(m_bits, PtrTraits::default_null_value);
        return PtrTraits::as_ptr(bits);
    }

    NonnullRefPtr<T> release_nonnull()
    {
        FlatPtr bits = PtrTraits::exchange(m_bits, PtrTraits::default_null_value);
        VERIFY(!PtrTraits::is_null(bits));
        return NonnullRefPtr<T>(NonnullRefPtr<T>::Adopt, *PtrTraits::as_ptr(bits));
    }

    ALWAYS_INLINE T* ptr() { return as_ptr(); }
    ALWAYS_INLINE const T* ptr() const { return as_ptr(); }

    ALWAYS_INLINE T* operator->()
    {
        return as_nonnull_ptr();
    }

    ALWAYS_INLINE const T* operator->() const
    {
        return as_nonnull_ptr();
    }

    ALWAYS_INLINE T& operator*()
    {
        return *as_nonnull_ptr();
    }

    ALWAYS_INLINE const T& operator*() const
    {
        return *as_nonnull_ptr();
    }

    ALWAYS_INLINE operator const T*() const { return as_ptr(); }
    ALWAYS_INLINE operator T*() { return as_ptr(); }

    ALWAYS_INLINE operator bool() { return !is_null(); }

    bool operator==(std::nullptr_t) const { return is_null(); }
    bool operator!=(std::nullptr_t) const { return !is_null(); }

    bool operator==(const RefPtr& other) const { return as_ptr() == other.as_ptr(); }
    bool operator!=(const RefPtr& other) const { return as_ptr() != other.as_ptr(); }

    bool operator==(RefPtr& other) { return as_ptr() == other.as_ptr(); }
    bool operator!=(RefPtr& other) { return as_ptr() != other.as_ptr(); }

    bool operator==(const T* other) const { return as_ptr() == other; }
    bool operator!=(const T* other) const { return as_ptr() != other; }

    bool operator==(T* other) { return as_ptr() == other; }
    bool operator!=(T* other) { return as_ptr() != other; }

    ALWAYS_INLINE bool is_null() const { return PtrTraits::is_null(m_bits.load(AK::MemoryOrder::memory_order_relaxed)); }

    template<typename U = T, typename EnableIf<IsSame<U, T> && !IsNullPointer<typename PtrTraits::NullType>>::Type* = nullptr>
    typename PtrTraits::NullType null_value() const
    {
        // make sure we are holding a null value
        FlatPtr bits = m_bits.load(AK::MemoryOrder::memory_order_relaxed);
        VERIFY(PtrTraits::is_null(bits));
        return PtrTraits::to_null_value(bits);
    }
    template<typename U = T, typename EnableIf<IsSame<U, T> && !IsNullPointer<typename PtrTraits::NullType>>::Type* = nullptr>
    void set_null_value(typename PtrTraits::NullType value)
    {
        // make sure that new null value would be interpreted as a null value
        FlatPtr bits = PtrTraits::from_null_value(value);
        VERIFY(PtrTraits::is_null(bits));
        assign_raw(bits);
    }

private:
    template<typename F>
    void do_while_locked(F f) const
    {
#ifdef KERNEL
        // We don't want to be pre-empted while we have the lock bit set
        Kernel::ScopedCritical critical;
#endif
        FlatPtr bits = PtrTraits::lock(m_bits);
        T* ptr = PtrTraits::as_ptr(bits);
        f(ptr);
        PtrTraits::unlock(m_bits, bits);
    }

    [[nodiscard]] ALWAYS_INLINE FlatPtr leak_ref_raw()
    {
        return PtrTraits::exchange(m_bits, PtrTraits::default_null_value);
    }

    [[nodiscard]] ALWAYS_INLINE FlatPtr add_ref_raw() const
    {
#ifdef KERNEL
        // We don't want to be pre-empted while we have the lock bit set
        Kernel::ScopedCritical critical;
#endif
        // This prevents a race condition between thread A and B:
        // 1. Thread A copies RefPtr, e.g. through assignment or copy constructor,
        //    gets the pointer from source, but is pre-empted before adding
        //    another reference
        // 2. Thread B calls clear, leak_ref, or release_nonnull on source, and
        //    then drops the last reference, causing the object to be deleted
        // 3. Thread A finishes step #1 by attempting to add a reference to
        //    the object that was already deleted in step #2
        FlatPtr bits = PtrTraits::lock(m_bits);
        if (T* ptr = PtrTraits::as_ptr(bits))
            ptr->ref();
        PtrTraits::unlock(m_bits, bits);
        return bits;
    }

    ALWAYS_INLINE void assign_raw(FlatPtr bits)
    {
        FlatPtr prev_bits = PtrTraits::exchange(m_bits, bits);
        unref_if_not_null(PtrTraits::as_ptr(prev_bits));
    }

    ALWAYS_INLINE T* as_ptr() const
    {
        return PtrTraits::as_ptr(m_bits.load(AK::MemoryOrder::memory_order_relaxed));
    }

    ALWAYS_INLINE T* as_nonnull_ptr() const
    {
        return as_nonnull_ptr(m_bits.load(AK::MemoryOrder::memory_order_relaxed));
    }

    ALWAYS_INLINE T* as_nonnull_ptr(FlatPtr bits) const
    {
        VERIFY(!PtrTraits::is_null(bits));
        return PtrTraits::as_ptr(bits);
    }

    mutable Atomic<FlatPtr> m_bits { PtrTraits::default_null_value };
};

template<typename T>
struct Formatter<RefPtr<T>> : Formatter<const T*> {
    ErrorOr<void> format(FormatBuilder& builder, RefPtr<T> const& value)
    {
        return Formatter<const T*>::format(builder, value.ptr());
    }
};

template<typename T>
struct Traits<RefPtr<T>> : public GenericTraits<RefPtr<T>> {
    using PeekType = T*;
    using ConstPeekType = const T*;
    static unsigned hash(const RefPtr<T>& p) { return ptr_hash(p.ptr()); }
    static bool equals(const RefPtr<T>& a, const RefPtr<T>& b) { return a.ptr() == b.ptr(); }
};

template<typename T, typename U>
inline NonnullRefPtr<T> static_ptr_cast(const NonnullRefPtr<U>& ptr)
{
    return NonnullRefPtr<T>(static_cast<const T&>(*ptr));
}

template<typename T, typename U, typename PtrTraits = RefPtrTraits<T>>
inline RefPtr<T> static_ptr_cast(const RefPtr<U>& ptr)
{
    return RefPtr<T, PtrTraits>(static_cast<const T*>(ptr.ptr()));
}

template<typename T, typename PtrTraitsT, typename U, typename PtrTraitsU>
inline void swap(RefPtr<T, PtrTraitsT>& a, RefPtr<U, PtrTraitsU>& b) requires(IsConvertible<U*, T*>)
{
    a.swap(b);
}

template<typename T>
inline RefPtr<T> adopt_ref_if_nonnull(T* object)
{
    if (object)
        return RefPtr<T>(RefPtr<T>::Adopt, *object);
    return {};
}

template<typename T, class... Args>
requires(IsConstructible<T, Args...>) inline ErrorOr<NonnullRefPtr<T>> try_make_ref_counted(Args&&... args)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) T(forward<Args>(args)...));
}

// FIXME: Remove once P0960R3 is available in Clang.
template<typename T, class... Args>
inline ErrorOr<NonnullRefPtr<T>> try_make_ref_counted(Args&&... args)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) T { forward<Args>(args)... });
}

template<typename T>
inline ErrorOr<NonnullRefPtr<T>> adopt_nonnull_ref_or_enomem(T* object)
{
    auto result = adopt_ref_if_nonnull(object);
    if (!result)
        return Error::from_errno(ENOMEM);
    return result.release_nonnull();
}

}

using AK::adopt_ref_if_nonnull;
using AK::RefPtr;
using AK::static_ptr_cast;
using AK::try_make_ref_counted;

#ifdef KERNEL
using AK::adopt_nonnull_ref_or_enomem;
#endif
